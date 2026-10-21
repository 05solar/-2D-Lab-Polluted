package io.github.some_example_name.profile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 본부 보관함·판매·이관 트랜잭션(순수 Java — 화면/파일 경로 비의존).
 *
 * 모든 상태 변경은 "다음 프로필 작성 → 저장 성공 → 메모리 교체" 순서로만 일어난다.
 * 저장이 실패하면 메모리·이전 저장본이 그대로 남아 재시도할 수 있다. 같은 귀환 ID/
 * 거래 ID는 두 번 반영되지 않는다(재호출·재시작 대비). 저장은 동기·순차 처리라
 * 다른 변경이 최신 프로필을 덮어쓰지 않는다.
 */
public final class HeadquartersEconomy {

    public enum Status { OK, ALREADY_PROCESSED, INVALID, SAVE_FAILED, NOTHING_TO_DO }

    public static final class Result {
        public final Status status;
        public final int itemCount;
        public final long totalWon;
        Result(Status status, int itemCount, long totalWon) {
            this.status = status;
            this.itemCount = itemCount;
            this.totalWon = totalWon;
        }
    }

    private final ProfileStore store;
    /** 아이템 ID → 개당 판매 가격(원). 카탈로그(loot_tables.json)가 유일한 가격 기준. */
    private final Map<String, Integer> catalogPrices;
    private PlayerProfile profile;
    /** 저장 시점에 스킬 쿨다운 스냅샷을 싣기 위한 공급자(게임 루트가 주입, 선택적). */
    private java.util.concurrent.Callable<Map<String, Float>> cooldownSnapshotProvider;

    public HeadquartersEconomy(PlayerProfile profile, ProfileStore store,
                               Map<String, Integer> catalogPrices) {
        if (profile == null || store == null || catalogPrices == null)
            throw new IllegalArgumentException();
        this.profile = profile;
        this.store = store;
        this.catalogPrices = catalogPrices;
    }

    public PlayerProfile profile() { return profile; }
    public long currencyWon() { return profile.currencyWon; }

    /** 카탈로그에 가격이 있는 아이템만 판매 가능. 알 수 없는 아이템은 보존하되 판매 불가. */
    public boolean sellable(String itemId) { return catalogPrices.containsKey(itemId); }
    public int unitPrice(String itemId) {
        Integer price = catalogPrices.get(itemId);
        return price == null ? 0 : price;
    }

    /**
     * 성공 귀환 1회의 가방 → 보관함 이관. 보관함 추가 + 귀환 처리 기록을 한 번의 저장으로
     * 반영한다(가방 비우기는 호출 측이 OK/ALREADY 반환 후 수행). 같은 returnId는 재호출돼도
     * 복제되지 않는다. 저장 실패 시 아무것도 바뀌지 않는다.
     */
    public synchronized Result transferReturn(String returnId, List<String> backpackItemIds) {
        if (returnId == null || returnId.isEmpty() || backpackItemIds == null)
            return new Result(Status.INVALID, 0, 0L);
        if (profile.processedReturnIds.contains(returnId))
            return new Result(Status.ALREADY_PROCESSED, 0, 0L);
        PlayerProfile next = profile.copy();
        int moved = 0;
        for (String itemId : backpackItemIds) {
            if (itemId == null || itemId.isEmpty()) continue;
            next.storage.add(new ItemInstance(next.nextInstanceId++, itemId));
            moved++;
        }
        next.processedReturnIds.add(returnId);
        PlayerProfile.trim(next.processedReturnIds);
        next.profileRevision++;
        if (!persist(next)) return new Result(Status.SAVE_FAILED, 0, 0L);
        profile = next;
        return new Result(Status.OK, moved, 0L);
    }

    /**
     * 선택 판매: 요청 수량·아이템 ID·판매 가능 여부·가격을 모델에서 재검증한다
     * (UI가 전달한 합계를 신뢰하지 않음). 물품 차감과 재화 증가를 하나의 다음 프로필로
     * 만들고 저장 성공 후에만 교체한다. 합계는 long 정수 원이며 오버플로를 검사한다.
     */
    public synchronized Result sell(String transactionId, Map<String, Integer> quantities) {
        if (transactionId == null || transactionId.isEmpty() || quantities == null)
            return new Result(Status.INVALID, 0, 0L);
        if (profile.processedTransactionIds.contains(transactionId))
            return new Result(Status.ALREADY_PROCESSED, 0, 0L);
        Map<String, Integer> held = profile.countsByItemId();
        long total = 0L;
        int count = 0;
        for (Map.Entry<String, Integer> entry : quantities.entrySet()) {
            String itemId = entry.getKey();
            Integer requested = entry.getValue();
            if (itemId == null || requested == null || requested < 0) return new Result(Status.INVALID, 0, 0L);
            if (requested == 0) continue;
            Integer owned = held.get(itemId);
            if (owned == null || requested > owned) return new Result(Status.INVALID, 0, 0L);
            if (!sellable(itemId)) return new Result(Status.INVALID, 0, 0L);
            try {
                total = Math.addExact(total,
                    Math.multiplyExact((long) unitPrice(itemId), (long) requested));
                count = Math.addExact(count, requested);
            } catch (ArithmeticException overflow) {
                return new Result(Status.INVALID, 0, 0L);
            }
        }
        if (count == 0) return new Result(Status.NOTHING_TO_DO, 0, 0L);
        PlayerProfile next = profile.copy();
        long newCurrency;
        try {
            newCurrency = Math.addExact(next.currencyWon, total);
        } catch (ArithmeticException overflow) {
            return new Result(Status.INVALID, 0, 0L);
        }
        // 아이템 ID별 앞에서부터 요청 수량만큼 인스턴스를 확정 제거한다.
        Map<String, Integer> remaining = new LinkedHashMap<>(quantities);
        Iterator<ItemInstance> iterator = next.storage.iterator();
        while (iterator.hasNext()) {
            ItemInstance item = iterator.next();
            Integer need = remaining.get(item.itemId);
            if (need != null && need > 0) {
                iterator.remove();
                remaining.put(item.itemId, need - 1);
            }
        }
        next.currencyWon = newCurrency;
        next.processedTransactionIds.add(transactionId);
        PlayerProfile.trim(next.processedTransactionIds);
        next.profileRevision++;
        if (!persist(next)) return new Result(Status.SAVE_FAILED, 0, 0L);
        profile = next;
        return new Result(Status.OK, count, total);
    }

    /** 전체 판매: 보관함의 판매 가능 물품 전체를 대상으로 한다. */
    public synchronized Result sellAll(String transactionId) {
        Map<String, Integer> quantities = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : profile.countsByItemId().entrySet())
            if (sellable(entry.getKey())) quantities.put(entry.getKey(), entry.getValue());
        if (quantities.isEmpty()) return new Result(Status.NOTHING_TO_DO, 0, 0L);
        return sell(transactionId, quantities);
    }

    /**
     * 강화 노드 구매(1회 구매·AND 선행·재화 검증·거래 ID 중복 차단).
     * 재화 차감 + 구매 ID 추가 + (해금 노드) 빈 활성 슬롯 자동 장착을 하나의 다음 프로필로
     * 만들고, 판매와 같은 거래 경계(저장 성공 → 메모리 교체)로 처리한다.
     * UI 상태를 신뢰하지 않고 모델에서 전부 다시 검증한다.
     *
     * @param baseActiveSlots 기본 활성 슬롯 수(확장 노드 효과는 구매 집합에서 재계산)
     */
    public synchronized Result purchaseUpgrade(String transactionId, UpgradeCatalog catalog,
                                               String nodeId, int baseActiveSlots) {
        if (transactionId == null || transactionId.isEmpty() || catalog == null || nodeId == null)
            return new Result(Status.INVALID, 0, 0L);
        if (profile.processedTransactionIds.contains(transactionId))
            return new Result(Status.ALREADY_PROCESSED, 0, 0L);
        UpgradeCatalog.Node node = catalog.node(nodeId);
        if (node == null) return new Result(Status.INVALID, 0, 0L);
        java.util.Set<String> purchased = profile.purchasedNodeIds();
        if (purchased.contains(nodeId)) return new Result(Status.INVALID, 0, 0L); // 1회 구매
        if (!catalog.prerequisitesMet(nodeId, purchased)) return new Result(Status.INVALID, 0, 0L);
        if (node.costWon > profile.currencyWon) return new Result(Status.INVALID, 0, 0L);
        // 효과 적용 가능성(비용 차감 전 검증): 중복 해금 금지, 성능 강화는 해금 필요.
        if (node.effectType == UpgradeCatalog.EffectType.SKILL_UNLOCK) {
            UpgradeEffects current = UpgradeEffects.compute(catalog, purchased);
            if (node.skillId == null || current.unlockedSkillIds.contains(node.skillId))
                return new Result(Status.INVALID, 0, 0L);
        }
        if (node.effectType == UpgradeCatalog.EffectType.SKILL_HEAL_ADD
            || node.effectType == UpgradeCatalog.EffectType.SKILL_DAMAGE_ADD
            || node.effectType == UpgradeCatalog.EffectType.SHIELD_ABSORB_ADD) {
            UpgradeEffects current = UpgradeEffects.compute(catalog, purchased);
            if (node.skillId == null || !current.unlockedSkillIds.contains(node.skillId))
                return new Result(Status.INVALID, 0, 0L);
        }
        PlayerProfile next = profile.copy();
        next.currencyWon -= node.costWon;
        next.purchasedUpgrades.put(nodeId, 1);
        next.upgradeCatalogVersion = catalog.catalogVersion;
        if (node.effectType == UpgradeCatalog.EffectType.SKILL_UNLOCK) {
            // 빈 활성 슬롯이 있으면 기본 장착한다(기본 위치 우선). 찬 슬롯은 덮어쓰지 않는다.
            UpgradeEffects after = UpgradeEffects.compute(catalog, next.purchasedNodeIds());
            int capacity = Math.min(SkillLoadout.SLOT_COUNT, baseActiveSlots + after.extraActiveSlots);
            while (next.skillSlots.size() < SkillLoadout.SLOT_COUNT) next.skillSlots.add("");
            boolean alreadyEquipped = next.skillSlots.contains(node.skillId);
            if (!alreadyEquipped) {
                int target = -1;
                int preferred = SkillLoadout.HEAL.equals(node.skillId) ? 1 : 0; // 기본 위치
                if (preferred < capacity && next.skillSlots.get(preferred).isEmpty()) target = preferred;
                else for (int i = 0; i < capacity; i++)
                    if (next.skillSlots.get(i).isEmpty()) { target = i; break; }
                if (target >= 0) next.skillSlots.set(target, node.skillId);
            }
            if (!next.equippedSkills.contains(node.skillId))
                next.equippedSkills.add(node.skillId); // 구버전 필드도 일관 유지
        }
        next.processedTransactionIds.add(transactionId);
        PlayerProfile.trim(next.processedTransactionIds);
        next.profileRevision++;
        if (!persist(next)) return new Result(Status.SAVE_FAILED, 0, 0L);
        profile = next;
        return new Result(Status.OK, 1, node.costWon);
    }

    /** 강화 레벨 기록(저장 성공 시에만 반영). 강화 시스템 도입 전 확장 지점. */
    public synchronized Result recordUpgrade(String upgradeId, int level) {
        if (upgradeId == null || upgradeId.isEmpty() || level < 0)
            return new Result(Status.INVALID, 0, 0L);
        PlayerProfile next = profile.copy();
        next.purchasedUpgrades.put(upgradeId, level);
        next.profileRevision++;
        if (!persist(next)) return new Result(Status.SAVE_FAILED, 0, 0L);
        profile = next;
        return new Result(Status.OK, 0, 0L);
    }

    public void setCooldownSnapshotProvider(java.util.concurrent.Callable<Map<String, Float>> provider) {
        cooldownSnapshotProvider = provider;
    }

    /**
     * 보스 유효 토벌 확정(원자 거래): 토벌 기록 + 다음 단계 진행 해금 + 보상 아이템 인스턴스
     * 추가 + 보상 영수증("stageId#runId")을 하나의 다음 프로필로 만들어 저장 성공 시에만
     * 교체한다. 같은 runId의 재호출(결과창 재진입/콜백 중복/재실행)은 ALREADY_PROCESSED로
     * 복제 없이 멱등 처리되고, 저장 실패 시 이전 상태를 유지해 재시도할 수 있다.
     * 최초 클리어 해금은 Set이라 중복 누적되지 않는다. 보상은 본부 보관함으로 지급된다
     * (가방 8칸과 무관 — 삭제/자동 판매 없음).
     */
    public synchronized Result commitBossClear(String runId, String stageId,
                                               String nextStageId, String rewardItemId) {
        if (runId == null || runId.isEmpty() || stageId == null || rewardItemId == null)
            return new Result(Status.INVALID, 0, 0L);
        String receipt = stageId + "#" + runId;
        if (profile.bossRewardReceipts.contains(receipt))
            return new Result(Status.ALREADY_PROCESSED, 0, 0L);
        PlayerProfile next = profile.copy();
        next.bossClearedStages.add(stageId);
        if (nextStageId != null && !nextStageId.isEmpty())
            next.bossUnlockedStages.add(nextStageId);
        next.storage.add(new ItemInstance(next.nextInstanceId++, rewardItemId));
        next.bossRewardReceipts.add(receipt);
        PlayerProfile.trim(next.bossRewardReceipts);
        next.profileRevision++;
        if (!persist(next)) return new Result(Status.SAVE_FAILED, 0, 0L);
        profile = next;
        return new Result(Status.OK, 1, 0L);
    }

    /**
     * 스킬 장착·키 설정 저장(판매/구매와 같은 단일 저장 흐름 — 다른 변경을 덮어쓰지 않도록
     * 항상 최신 profile의 copy에서 시작한다). 실패 시 이전 설정 유지.
     */
    public synchronized Result saveSkillLoadout(java.util.List<String> slots,
                                                java.util.List<Integer> keys) {
        if (slots == null || keys == null) return new Result(Status.INVALID, 0, 0L);
        PlayerProfile next = profile.copy();
        next.skillSlots.clear();
        next.skillSlots.addAll(slots);
        next.skillKeys.clear();
        next.skillKeys.addAll(keys);
        next.profileRevision++;
        if (!persist(next)) return new Result(Status.SAVE_FAILED, 0, 0L);
        profile = next;
        return new Result(Status.OK, 0, 0L);
    }

    private boolean persist(PlayerProfile next) {
        try {
            // 재실행 시 남은 쿨다운 보존: 모든 저장 이벤트에 현재 스냅샷을 싣는다(프레임 저장 없음).
            if (cooldownSnapshotProvider != null) {
                next.skillCooldowns.clear();
                next.skillCooldowns.putAll(cooldownSnapshotProvider.call());
            }
            store.save(next);
            return true;
        } catch (Exception failed) {
            return false;
        }
    }

    /** UI 목록용: 아이템 ID별 수량(삽입 순서 유지). */
    public Map<String, Integer> heldCounts() { return profile.countsByItemId(); }

    /** 전체 판매 확인창용: 판매 가능 물품 수와 합계 금액(검증 로직과 동일 기준). */
    public long[] sellableSummary() {
        long total = 0L;
        long count = 0L;
        for (Map.Entry<String, Integer> entry : profile.countsByItemId().entrySet()) {
            if (!sellable(entry.getKey())) continue;
            total += (long) unitPrice(entry.getKey()) * entry.getValue();
            count += entry.getValue();
        }
        return new long[]{count, total};
    }

    public List<ItemInstance> storageView() {
        return new ArrayList<>(profile.storage);
    }
}

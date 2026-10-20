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

    private boolean persist(PlayerProfile next) {
        try {
            store.save(next);
            return true;
        } catch (IOException failed) {
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

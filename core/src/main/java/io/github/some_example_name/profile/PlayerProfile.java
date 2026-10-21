package io.github.some_example_name.profile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 영속 프로필(순수 Java 값 객체): 재화(정수 원, long)·본부 보관함·구매한 강화·
 * 중복 귀환 방지 기록. 변경은 {@link #copy()}로 다음 프로필을 만들어 적용하고,
 * 저장이 성공한 뒤에만 메모리 상태로 교체한다.
 */
public final class PlayerProfile {

    public static final int CURRENT_SCHEMA_VERSION = 1;
    /** 중복 귀환/거래 방지 기록 보존 개수(오래된 것부터 버림). */
    public static final int PROCESSED_ID_LIMIT = 64;

    public int schemaVersion = CURRENT_SCHEMA_VERSION;
    public long profileRevision;
    public long currencyWon;
    public final List<ItemInstance> storage = new ArrayList<>();
    /** 강화 노드 ID → 레벨(트리 노드는 1회 구매 = 레벨 1). 알 수 없는 ID도 보존한다. */
    public final Map<String, Integer> purchasedUpgrades = new LinkedHashMap<>();
    /** (구) 장착 목록 — SkillLoadout 마이그레이션 입력으로만 유지. */
    public final List<String> equippedSkills = new ArrayList<>();
    /** 슬롯별 장착 스킬(SkillLoadout.SLOT_COUNT 길이, 빈 슬롯은 ""). */
    public final List<String> skillSlots = new ArrayList<>();
    /** 슬롯별 등록 키코드(0=미등록). */
    public final List<Integer> skillKeys = new ArrayList<>();
    /** 저장 시점의 skillId별 남은 쿨다운(초) 스냅샷 — 오프라인 경과는 적용하지 않는다. */
    public final Map<String, Float> skillCooldowns = new LinkedHashMap<>();
    /** 구매 당시 카탈로그 버전(마이그레이션 참고용). */
    public int upgradeCatalogVersion;
    /** 보스 모드: 진행 해금된 단계(1-1은 카탈로그 기본 해금이라 기록 불필요). */
    public final Set<String> bossUnlockedStages = new LinkedHashSet<>();
    /** 보스 모드: 최초 클리어 기록(해금 중복 누적 방지·UI 표시). */
    public final Set<String> bossClearedStages = new LinkedHashSet<>();
    /** 보스 모드: 보상 영수증 "stageId#runId" — 유효 토벌 runId당 보상 1개 보장. */
    public final Set<String> bossRewardReceipts = new LinkedHashSet<>();
    /** 처리 완료한 탐사 귀환 ID(같은 전환 콜백 재호출·재시작 시 복제 방지). */
    public final Set<String> processedReturnIds = new LinkedHashSet<>();
    /** 처리 완료한 판매 거래 ID(같은 요청 이중 반영 방지). */
    public final Set<String> processedTransactionIds = new LinkedHashSet<>();
    public long nextInstanceId = 1;
    /** 로드 시 알 수 없던 최상위 필드(미래 버전/다른 시스템)를 원문 그대로 보존해 재기록한다. */
    public final Map<String, String> unknownFieldsJson = new LinkedHashMap<>();

    public PlayerProfile copy() {
        PlayerProfile next = new PlayerProfile();
        next.schemaVersion = CURRENT_SCHEMA_VERSION;
        next.profileRevision = profileRevision;
        next.currencyWon = currencyWon;
        next.storage.addAll(storage);
        next.purchasedUpgrades.putAll(purchasedUpgrades);
        next.equippedSkills.addAll(equippedSkills);
        next.skillSlots.addAll(skillSlots);
        next.skillKeys.addAll(skillKeys);
        next.skillCooldowns.putAll(skillCooldowns);
        next.upgradeCatalogVersion = upgradeCatalogVersion;
        next.bossUnlockedStages.addAll(bossUnlockedStages);
        next.bossClearedStages.addAll(bossClearedStages);
        next.bossRewardReceipts.addAll(bossRewardReceipts);
        next.processedReturnIds.addAll(processedReturnIds);
        next.processedTransactionIds.addAll(processedTransactionIds);
        next.nextInstanceId = nextInstanceId;
        next.unknownFieldsJson.putAll(unknownFieldsJson);
        return next;
    }

    /** 기록 집합을 한도 안으로 유지한다(삽입 순서 기준 오래된 항목 제거). */
    public static void trim(Set<String> ids) {
        while (ids.size() > PROCESSED_ID_LIMIT) {
            String oldest = ids.iterator().next();
            ids.remove(oldest);
        }
    }

    /** 구매 완료한 강화 노드 ID 집합(레벨 ≥ 1). */
    public Set<String> purchasedNodeIds() {
        Set<String> ids = new LinkedHashSet<>();
        for (Map.Entry<String, Integer> entry : purchasedUpgrades.entrySet())
            if (entry.getValue() != null && entry.getValue() >= 1) ids.add(entry.getKey());
        return ids;
    }

    /** 아이템 ID별 보유 수량(보관함 전체 — 판매 가능 여부와 무관). */
    public Map<String, Integer> countsByItemId() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ItemInstance item : storage) {
            Integer current = counts.get(item.itemId);
            counts.put(item.itemId, current == null ? 1 : current + 1);
        }
        return counts;
    }
}

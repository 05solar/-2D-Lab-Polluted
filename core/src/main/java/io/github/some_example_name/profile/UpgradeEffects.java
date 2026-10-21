package io.github.some_example_name.profile;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 구매한 노드 ID 집합에서 파생 능력치를 계산한다(순수 Java, 상태 없음).
 * 항상 "기본값 + 합계"로 다시 계산하므로 로드/화면 전환/패널 재진입에도 중복 누적이 없다.
 * 저장에 있지만 카탈로그에 없는 노드 ID는 삭제하지 않고 보존하며(경고 목록), 계산에서만 제외한다.
 */
public final class UpgradeEffects {

    public final int bonusMaxHp;
    public final int bonusAttack;
    public final Set<String> unlockedSkillIds;
    public final int healBonus;
    public final int extraActiveSlots;
    /** skillId별 전용 피해 가산(SKILL_DAMAGE_ADD) — 기본 공격 강화와 별개. */
    public final java.util.Map<String, Integer> skillDamageBonus;
    public final int shieldAbsorbBonus;
    public final Set<String> unknownPurchasedIds;

    private UpgradeEffects(int bonusMaxHp, int bonusAttack, Set<String> unlockedSkillIds,
                           int healBonus, int extraActiveSlots,
                           java.util.Map<String, Integer> skillDamageBonus, int shieldAbsorbBonus,
                           Set<String> unknownPurchasedIds) {
        this.bonusMaxHp = bonusMaxHp;
        this.bonusAttack = bonusAttack;
        this.unlockedSkillIds = unlockedSkillIds;
        this.healBonus = healBonus;
        this.extraActiveSlots = extraActiveSlots;
        this.skillDamageBonus = skillDamageBonus;
        this.shieldAbsorbBonus = shieldAbsorbBonus;
        this.unknownPurchasedIds = unknownPurchasedIds;
    }

    public static UpgradeEffects compute(UpgradeCatalog catalog, Set<String> purchasedIds) {
        int hp = 0, attack = 0, heal = 0, slots = 0, shieldAbsorb = 0;
        Set<String> skills = new LinkedHashSet<>();
        Set<String> unknown = new LinkedHashSet<>();
        java.util.Map<String, Integer> damage = new java.util.LinkedHashMap<>();
        for (String nodeId : purchasedIds) {
            UpgradeCatalog.Node node = catalog.node(nodeId);
            if (node == null) { unknown.add(nodeId); continue; } // 보존하되 계산 제외
            switch (node.effectType) {
                case MAX_HP_ADD: hp += node.effectValue; break;
                case BASE_ATTACK_ADD: attack += node.effectValue; break;
                case SKILL_UNLOCK: if (node.skillId != null) skills.add(node.skillId); break;
                case SKILL_HEAL_ADD: heal += node.effectValue; break;
                case ACTIVE_SLOT_ADD: slots += node.effectValue; break;
                case SKILL_DAMAGE_ADD:
                    if (node.skillId != null) {
                        Integer prev = damage.get(node.skillId);
                        damage.put(node.skillId, (prev == null ? 0 : prev) + node.effectValue);
                    }
                    break;
                case SHIELD_ABSORB_ADD: shieldAbsorb += node.effectValue; break;
            }
        }
        return new UpgradeEffects(hp, attack, skills, heal, slots, damage, shieldAbsorb, unknown);
    }

    public boolean healUnlocked() { return unlockedSkillIds.contains("heal"); }

    public int skillDamageBonus(String skillId) {
        Integer bonus = skillDamageBonus.get(skillId);
        return bonus == null ? 0 : bonus;
    }
}

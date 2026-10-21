package io.github.some_example_name.profile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 스킬 장착·키 설정(순수 Java): slotId → skillId, slotId → keyCode.
 * 소유(해금)·쿨다운과 분리된 "구성" 데이터다. UI 표시 슬롯은 항상 {@link #SLOT_COUNT}개,
 * 실제 사용 가능 슬롯 수(capacity)는 강화 구매에서 따로 계산한다.
 *
 * 규칙: 같은 스킬을 두 슬롯에 중복 장착 금지, 같은 키를 두 슬롯에 중복 등록 금지,
 * 예약키 등록 금지. 검증 실패는 이유 문자열을 반환하고 상태를 바꾸지 않는다.
 */
public final class SkillLoadout {

    public static final int SLOT_COUNT = 5;
    public static final int KEY_NONE = 0; // 미등록
    public static final String DASH = "dash";
    public static final String HEAL = "heal";
    public static final String SHOCKWAVE = "shockwave";
    public static final String ELECTRIC_THROW = "electric_throw";
    public static final String SHIELD = "shield";
    /** 공통 파이프라인이 아는 스킬 전체(이 밖의 저장값은 로드 시 정리된다). */
    public static final java.util.Set<String> KNOWN_SKILLS =
        java.util.Collections.unmodifiableSet(new java.util.LinkedHashSet<>(
            java.util.Arrays.asList(DASH, HEAL, SHOCKWAVE, ELECTRIC_THROW, SHIELD)));
    /** 기본 키: F / Q / 1 / 2 / 3 (libGDX Input.Keys 코드). */
    public static final int[] DEFAULT_KEYS = {34, 45, 8, 9, 10}; // F, Q, NUM_1, NUM_2, NUM_3

    private final String[] slots = new String[SLOT_COUNT];
    private final int[] keys = new int[SLOT_COUNT];
    private final List<String> sanitizeLog = new ArrayList<>();

    /**
     * 프로필에서 구성 로드(+마이그레이션/정리). 저장 손실 없이:
     * - 스킬 설정이 전혀 없으면 최초 기본값(F=dash, Q=heal(해금 시), 1/2/3 빈 슬롯).
     * - 구버전 equippedSkills 목록은 기본 슬롯 위치로 옮긴다.
     * - 알 수 없는 skillId·중복 장착·중복/예약 키는 정리하고 이유를 기록한다(해금 기록은 보존).
     */
    public static SkillLoadout fromProfile(PlayerProfile profile, Set<String> unlockedSkills,
                                           Set<Integer> reservedKeys) {
        SkillLoadout loadout = new SkillLoadout();
        boolean hadConfig = !profile.skillSlots.isEmpty() || !profile.skillKeys.isEmpty();
        if (!hadConfig) {
            loadout.slots[0] = DASH;
            if (unlockedSkills.contains(HEAL) || profile.equippedSkills.contains(HEAL))
                loadout.slots[1] = HEAL; // 구버전 equippedSkills 마이그레이션 포함
            System.arraycopy(DEFAULT_KEYS, 0, loadout.keys, 0, SLOT_COUNT);
            loadout.sanitizeLog.add("initialized default loadout");
            return loadout;
        }
        for (int i = 0; i < SLOT_COUNT; i++) {
            loadout.slots[i] = i < profile.skillSlots.size() ? emptyToNull(profile.skillSlots.get(i)) : null;
            loadout.keys[i] = i < profile.skillKeys.size() && profile.skillKeys.get(i) != null
                ? profile.skillKeys.get(i) : KEY_NONE;
        }
        loadout.sanitize(unlockedSkills, reservedKeys);
        return loadout;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    /** 알 수 없는 스킬/중복 장착/예약·중복 키 정리. 가능한 경우 해당 슬롯의 기본 키로 복구. */
    private void sanitize(Set<String> knownSkills, Set<Integer> reservedKeys) {
        Set<String> seenSkills = new java.util.HashSet<>();
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (slots[i] == null) continue;
            if (!knownSkills.contains(slots[i]) && !KNOWN_SKILLS.contains(slots[i])) {
                sanitizeLog.add("slot " + i + ": unknown skill '" + slots[i] + "' cleared");
                slots[i] = null;
            } else if (!seenSkills.add(slots[i])) {
                sanitizeLog.add("slot " + i + ": duplicate skill '" + slots[i] + "' cleared");
                slots[i] = null;
            }
        }
        Set<Integer> seenKeys = new java.util.HashSet<>();
        for (int i = 0; i < SLOT_COUNT; i++) {
            int key = keys[i];
            boolean bad = key != KEY_NONE
                && (reservedKeys.contains(key) || !seenKeys.add(key) || key < 0 || key > 254);
            if (bad) {
                int fallback = DEFAULT_KEYS[i];
                boolean fallbackFree = !reservedKeys.contains(fallback) && seenKeys.add(fallback);
                sanitizeLog.add("slot " + i + ": invalid key " + key
                    + (fallbackFree ? " restored to default" : " cleared"));
                keys[i] = fallbackFree ? fallback : KEY_NONE;
            }
        }
    }

    public String skillAt(int slot) { return valid(slot) ? slots[slot] : null; }
    public int keyAt(int slot) { return valid(slot) ? keys[slot] : KEY_NONE; }
    public List<String> sanitizeLog() { return sanitizeLog; }
    private static boolean valid(int slot) { return slot >= 0 && slot < SLOT_COUNT; }

    /** 장착된 슬롯 인덱스(없으면 -1). */
    public int slotOf(String skillId) {
        for (int i = 0; i < SLOT_COUNT; i++)
            if (skillId != null && skillId.equals(slots[i])) return i;
        return -1;
    }

    /**
     * 장착. 같은 스킬 중복 장착·용량 밖 슬롯·미해금 스킬을 거부한다.
     * @return null=성공, 아니면 거부 이유
     */
    public String equip(int slot, String skillId, int capacity, Set<String> unlockedSkills) {
        if (!valid(slot)) return "잘못된 슬롯";
        if (slot >= capacity) return "잠긴 슬롯(용량 " + capacity + ")";
        if (skillId == null) return "스킬 없음";
        if (!unlockedSkills.contains(skillId)) return "해금되지 않은 스킬";
        int already = slotOf(skillId);
        if (already >= 0 && already != slot) return "이미 " + (already + 1) + "번 슬롯에 장착됨";
        slots[slot] = skillId;
        return null;
    }

    public String unequip(int slot) {
        if (!valid(slot)) return "잘못된 슬롯";
        slots[slot] = null;
        return null;
    }

    /**
     * 키 등록. 예약키·다른 슬롯과 중복된 키를 거부한다(자동 교환/해제 없음).
     * @return null=성공, 아니면 거부 이유(중복 시 사용 중인 슬롯 번호 포함)
     */
    public String setKey(int slot, int keyCode, Set<Integer> reservedKeys) {
        if (!valid(slot)) return "잘못된 슬롯";
        if (keyCode <= 0 || keyCode > 254) return "등록할 수 없는 키";
        if (reservedKeys.contains(keyCode)) return "예약된 키";
        for (int i = 0; i < SLOT_COUNT; i++)
            if (i != slot && keys[i] == keyCode)
                return (i + 1) + "번 슬롯이 이미 사용 중";
        keys[slot] = keyCode;
        return null;
    }

    public List<String> slotsForSave() {
        List<String> out = new ArrayList<>();
        for (String slot : slots) out.add(slot == null ? "" : slot);
        return out;
    }

    public List<Integer> keysForSave() {
        List<Integer> out = new ArrayList<>();
        for (int key : keys) out.add(key);
        return out;
    }
}

package io.github.some_example_name.profile;

import org.junit.Test;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import static org.junit.Assert.*;

/**
 * 공통 스킬 시스템(소유/장착/키/쿨다운 분리 + SkillService 파이프라인) 순수 Java 검증.
 * 수치(회복 30/+10, 쿨다운 10s/3s)는 테스트 Context가 카탈로그 역할로 공급한다.
 */
public class SkillSystemTest {

    /** 화면 대신 쓰는 실행 환경: HP·생존·차단·효과 적용을 기록한다. */
    private static final class FakeContext implements SkillService.Context {
        int hp = 100, maxHp = 100, healAmount = 30;
        boolean alive = true, allowed = true, dashStartAccepts = true;
        final Set<String> unlocked = new HashSet<>(Arrays.asList("dash", "heal"));
        String lastEffect; int lastValue = -1; int effectCalls;

        @Override public boolean worldSkillInputAllowed() { return allowed; }
        @Override public boolean alive() { return alive; }
        @Override public int currentHp() { return hp; }
        @Override public int currentMaxHp() { return maxHp; }
        @Override public boolean unlocked(String skillId) { return unlocked.contains(skillId); }
        @Override public float cooldownSeconds(String skillId) {
            return "dash".equals(skillId) ? 3f : 10f;
        }
        @Override public int healAmount() { return healAmount; }
        @Override public boolean applyEffect(String skillId, int effectValue) {
            effectCalls++;
            lastEffect = skillId;
            lastValue = effectValue;
            if ("heal".equals(skillId)) { hp = Math.min(maxHp, hp + effectValue); return true; }
            return dashStartAccepts;
        }
    }

    private static final Set<Integer> NO_RESERVED = new HashSet<>();

    /** 기본 구성: slot0=dash, slot1=heal, 기본 키. */
    private static SkillLoadout defaultLoadout() {
        PlayerProfile profile = new PlayerProfile();
        Set<String> unlocked = new LinkedHashSet<>(Arrays.asList("dash", "heal"));
        return SkillLoadout.fromProfile(profile, unlocked, NO_RESERVED);
    }

    // ----- 회복 규칙 -----------------------------------------------------------------

    @Test public void healRestores30AndStartsExactTenSecondCooldown() {
        SkillLoadout loadout = defaultLoadout();
        SkillRuntime runtime = new SkillRuntime();
        FakeContext ctx = new FakeContext();
        ctx.hp = 50;
        assertEquals(SkillService.Result.OK, SkillService.tryExecute(1, loadout, 2, runtime, ctx));
        assertEquals(80, ctx.hp);
        assertEquals(30, ctx.lastValue);
        assertEquals(10f, runtime.remainingSeconds("heal"), .0001f);
        for (int i = 0; i < 9; i++) runtime.tick(1f);
        assertEquals("9초 경과 — 아직 대기", SkillService.Result.ON_COOLDOWN,
            SkillService.tryExecute(1, loadout, 2, runtime, ctx));
        runtime.tick(1f);
        assertTrue("정확히 10초 뒤 재사용 가능", runtime.ready("heal"));
    }

    @Test public void actualHealIsCappedByMissingHp() {
        SkillLoadout loadout = defaultLoadout();
        FakeContext ctx = new FakeContext();
        ctx.hp = 90; // 잃은 체력 10 < 회복량 30
        assertEquals(SkillService.Result.OK,
            SkillService.tryExecute(1, loadout, 2, new SkillRuntime(), ctx));
        assertEquals("실제 회복량 = min(30, max-hp)", 10, ctx.lastValue);
        assertEquals(100, ctx.hp);
    }

    @Test public void healWithUpgradeBonusAndRaisedMaxHp() {
        SkillLoadout loadout = defaultLoadout();
        FakeContext ctx = new FakeContext();
        ctx.maxHp = 150; ctx.hp = 100; ctx.healAmount = 30;
        assertEquals(SkillService.Result.OK,
            SkillService.tryExecute(1, loadout, 2, new SkillRuntime(), ctx));
        assertEquals("강화된 최대치 150 기준 100→130", 130, ctx.hp);
        ctx.healAmount = 40; // heal_power_1(+10) 구매 후
        FakeContext boosted = new FakeContext();
        boosted.healAmount = 40; boosted.hp = 50;
        assertEquals(SkillService.Result.OK,
            SkillService.tryExecute(1, loadout, 2, new SkillRuntime(), boosted));
        assertEquals(90, boosted.hp);
    }

    @Test public void fullHpHealFailsWithoutAnyStateChange() {
        SkillLoadout loadout = defaultLoadout();
        SkillRuntime runtime = new SkillRuntime();
        FakeContext ctx = new FakeContext(); // hp == maxHp
        assertEquals(SkillService.Result.FULL_HP,
            SkillService.tryExecute(1, loadout, 2, runtime, ctx));
        assertEquals("HP 무변화", 100, ctx.hp);
        assertEquals("효과 미적용", 0, ctx.effectCalls);
        assertTrue("쿨다운 미시작", runtime.ready("heal"));
    }

    @Test public void deadPlayerCannotHealNoRevive() {
        SkillLoadout loadout = defaultLoadout();
        SkillRuntime runtime = new SkillRuntime();
        FakeContext ctx = new FakeContext();
        ctx.alive = false; ctx.hp = 0;
        assertEquals(SkillService.Result.DEAD,
            SkillService.tryExecute(1, loadout, 2, runtime, ctx));
        assertEquals("부활 수단으로 동작하지 않는다", 0, ctx.hp);
        assertEquals(0, ctx.effectCalls);
    }

    @Test public void blockedWorldInputRejectsWithoutStateChange() {
        SkillLoadout loadout = defaultLoadout();
        SkillRuntime runtime = new SkillRuntime();
        FakeContext ctx = new FakeContext();
        ctx.allowed = false; ctx.hp = 50;
        assertEquals(SkillService.Result.BLOCKED,
            SkillService.tryExecute(1, loadout, 2, runtime, ctx));
        assertEquals(50, ctx.hp);
        assertTrue(runtime.ready("heal"));
    }

    // ----- 공통 파이프라인(슬롯/장착/해금/돌진) ----------------------------------------

    @Test public void lockedEmptyAndNotUnlockedSlotsReportReasons() {
        SkillLoadout loadout = defaultLoadout();
        SkillRuntime runtime = new SkillRuntime();
        FakeContext ctx = new FakeContext();
        ctx.hp = 50;
        assertEquals("용량 밖 슬롯", SkillService.Result.LOCKED_SLOT,
            SkillService.tryExecute(3, loadout, 2, runtime, ctx));
        assertEquals("빈 슬롯", SkillService.Result.EMPTY_SLOT,
            SkillService.tryExecute(4, loadout, 5, runtime, ctx));
        ctx.unlocked.remove("heal");
        assertEquals("해금 안 된 스킬", SkillService.Result.NOT_UNLOCKED,
            SkillService.tryExecute(1, loadout, 2, runtime, ctx));
        assertEquals(50, ctx.hp);
        assertEquals(0, ctx.effectCalls);
    }

    @Test public void dashRunsThroughCommonPathWithThreeSecondCooldown() {
        SkillLoadout loadout = defaultLoadout();
        SkillRuntime runtime = new SkillRuntime();
        FakeContext ctx = new FakeContext();
        assertEquals(SkillService.Result.OK, SkillService.tryExecute(0, loadout, 2, runtime, ctx));
        assertEquals("dash", ctx.lastEffect);
        assertEquals(3f, runtime.remainingSeconds("dash"), .0001f);
        assertEquals("쿨다운 중 재발동 금지(이중 관리 없음)", SkillService.Result.ON_COOLDOWN,
            SkillService.tryExecute(0, loadout, 2, runtime, ctx));
        runtime.tick(3f);
        assertEquals(SkillService.Result.OK, SkillService.tryExecute(0, loadout, 2, runtime, ctx));
    }

    @Test public void newSkillsRunThroughSameCommonPipeline() {
        SkillLoadout loadout = defaultLoadout();
        Set<String> unlocked = new LinkedHashSet<>(Arrays.asList(
            "dash", "heal", "shockwave", "electric_throw", "shield"));
        assertNull(loadout.equip(2, "shockwave", 5, unlocked));
        assertNull(loadout.equip(3, "electric_throw", 5, unlocked));
        assertNull(loadout.equip(4, "shield", 5, unlocked));
        SkillRuntime runtime = new SkillRuntime();
        FakeContext ctx = new FakeContext();
        ctx.unlocked.addAll(unlocked);
        assertEquals(SkillService.Result.OK, SkillService.tryExecute(2, loadout, 5, runtime, ctx));
        assertEquals("shockwave", ctx.lastEffect);
        assertTrue("시전 시작 성공 시 쿨다운 1회 시작", runtime.remainingSeconds("shockwave") > 0f);
        assertEquals("쿨다운 중 재시전 거부", SkillService.Result.ON_COOLDOWN,
            SkillService.tryExecute(2, loadout, 5, runtime, ctx));
        // 시작 실패(예: 보호막 활성 중 재시전)는 쿨다운을 소모하지 않는다.
        ctx.dashStartAccepts = false;
        assertEquals(SkillService.Result.BLOCKED,
            SkillService.tryExecute(4, loadout, 5, runtime, ctx));
        assertTrue("실패 무소모", runtime.ready("shield"));
        ctx.dashStartAccepts = true;
        assertEquals(SkillService.Result.OK, SkillService.tryExecute(4, loadout, 5, runtime, ctx));
        FakeContext lockedCtx = new FakeContext();
        lockedCtx.unlocked.clear();
        assertEquals("해금되지 않은 스킬은 거부", SkillService.Result.NOT_UNLOCKED,
            SkillService.tryExecute(3, loadout, 5, runtime, lockedCtx));
    }

    @Test public void dashStartRefusalDoesNotStartCooldown() {
        SkillLoadout loadout = defaultLoadout();
        SkillRuntime runtime = new SkillRuntime();
        FakeContext ctx = new FakeContext();
        ctx.dashStartAccepts = false; // 이미 돌진 중 등
        assertEquals(SkillService.Result.BLOCKED,
            SkillService.tryExecute(0, loadout, 2, runtime, ctx));
        assertTrue("실패 시 쿨다운 미시작", runtime.ready("dash"));
    }

    @Test public void cooldownIsKeyedBySkillAndSurvivesReEquip() {
        SkillLoadout loadout = defaultLoadout();
        SkillRuntime runtime = new SkillRuntime();
        runtime.startCooldown("heal", 7f);
        Set<String> unlocked = new LinkedHashSet<>(Arrays.asList("dash", "heal"));
        assertNull(loadout.unequip(1));
        assertNull("다른 슬롯으로 재장착", loadout.equip(2, "heal", 5, unlocked));
        assertEquals("슬롯을 옮겨도 쿨다운 유지", 7f, runtime.remainingSeconds("heal"), .0001f);
        FakeContext ctx = new FakeContext();
        ctx.hp = 50;
        assertEquals(SkillService.Result.ON_COOLDOWN,
            SkillService.tryExecute(2, loadout, 5, runtime, ctx));
    }

    // ----- 장착/키 구성 규칙 ----------------------------------------------------------

    @Test public void equipRejectsDuplicateLockedAndNotUnlocked() {
        SkillLoadout loadout = defaultLoadout();
        Set<String> unlocked = new LinkedHashSet<>(Arrays.asList("dash", "heal"));
        assertNotNull("같은 스킬 중복 장착 금지", loadout.equip(2, "dash", 5, unlocked));
        assertNotNull("용량 밖 슬롯 금지", loadout.equip(3, "heal", 2, unlocked));
        assertNotNull("미해금 스킬 금지",
            loadout.equip(2, "heal", 5, new HashSet<>(Arrays.asList("dash"))));
        assertEquals("거부는 상태를 바꾸지 않는다", 1, loadout.slotOf("heal"));
        assertNull(loadout.unequip(1));
        assertNull("해제 후에는 다른 슬롯에 장착 가능", loadout.equip(2, "heal", 5, unlocked));
        assertEquals(2, loadout.slotOf("heal"));
    }

    @Test public void keyRegistrationRejectsReservedAndDuplicates() {
        SkillLoadout loadout = defaultLoadout();
        Set<Integer> reserved = new HashSet<>(Arrays.asList(62, 46)); // SPACE, R
        assertNotNull("예약 키 금지", loadout.setKey(2, 62, reserved));
        assertNotNull("다른 슬롯과 중복 금지(F=slot0)", loadout.setKey(2, 34, reserved));
        assertEquals("거부 시 기존 키 유지", 8, loadout.keyAt(2));
        assertNull("정상 키 등록", loadout.setKey(2, 30, reserved)); // B
        assertEquals(30, loadout.keyAt(2));
        assertNull("같은 슬롯 재등록은 허용", loadout.setKey(2, 30, reserved));
    }

    // ----- 저장/마이그레이션 ----------------------------------------------------------

    @Test public void firstProfileGetsDefaultLoadoutOnlyOnce() {
        PlayerProfile fresh = new PlayerProfile();
        SkillLoadout loadout = SkillLoadout.fromProfile(fresh,
            new LinkedHashSet<>(Arrays.asList("dash")), NO_RESERVED);
        assertEquals("F=돌진 기본 장착", "dash", loadout.skillAt(0));
        assertNull("회복 미해금이면 비어 있음", loadout.skillAt(1));
        assertArrayEquals(new Object[]{34, 45, 8, 9, 10},
            loadout.keysForSave().toArray()); // F Q 1 2 3
    }

    @Test public void legacyEquippedSkillsMigrateHealToSlotOne() {
        PlayerProfile legacy = new PlayerProfile();
        legacy.equippedSkills.add("heal"); // 구버전 저장(슬롯 개념 없음)
        SkillLoadout loadout = SkillLoadout.fromProfile(legacy,
            new LinkedHashSet<>(Arrays.asList("dash", "heal")), NO_RESERVED);
        assertEquals("dash", loadout.skillAt(0));
        assertEquals("기존 해금·장착 기록 보존", "heal", loadout.skillAt(1));
    }

    @Test public void corruptConfigIsSanitizedWithoutLosingValidEntries() {
        PlayerProfile profile = new PlayerProfile();
        profile.skillSlots.addAll(Arrays.asList("dash", "ghost_skill", "dash", "", ""));
        profile.skillKeys.addAll(Arrays.asList(34, 34, 62, 0, 0)); // 중복 F, 예약 SPACE
        Set<Integer> reserved = new HashSet<>(Arrays.asList(62));
        SkillLoadout loadout = SkillLoadout.fromProfile(profile,
            new LinkedHashSet<>(Arrays.asList("dash")), reserved);
        assertEquals("유효 항목 보존", "dash", loadout.skillAt(0));
        assertNull("알 수 없는 스킬 정리", loadout.skillAt(1));
        assertNull("중복 장착 정리", loadout.skillAt(2));
        assertEquals(34, loadout.keyAt(0));
        assertEquals("중복 키는 기본 키로 복구", 45, loadout.keyAt(1));
        assertEquals("예약 키는 기본 키로 복구", 8, loadout.keyAt(2));
        assertFalse("정리 사유 기록", loadout.sanitizeLog().isEmpty());
    }

    @Test public void saveListsRoundTripThroughProfileFields() {
        SkillLoadout loadout = defaultLoadout();
        PlayerProfile profile = new PlayerProfile();
        profile.skillSlots.addAll(loadout.slotsForSave());
        profile.skillKeys.addAll(loadout.keysForSave());
        SkillLoadout reloaded = SkillLoadout.fromProfile(profile,
            new LinkedHashSet<>(Arrays.asList("dash", "heal")), NO_RESERVED);
        assertEquals("dash", reloaded.skillAt(0));
        assertEquals("heal", reloaded.skillAt(1));
        assertEquals(loadout.keysForSave(), reloaded.keysForSave());
    }

    @Test public void runtimeSnapshotKeepsOnlyPositiveAndRestores() {
        SkillRuntime runtime = new SkillRuntime();
        runtime.startCooldown("dash", 3f);
        runtime.startCooldown("heal", 10f);
        runtime.tick(3f);
        Map<String, Float> snapshot = runtime.snapshot();
        assertEquals("소진된 쿨다운은 저장하지 않는다", 1, snapshot.size());
        assertEquals(7f, snapshot.get("heal"), .0001f);
        SkillRuntime restored = new SkillRuntime();
        restored.restore(snapshot); // 오프라인 경과 미적용 — 저장된 값 그대로
        assertEquals(7f, restored.remainingSeconds("heal"), .0001f);
        assertTrue(restored.ready("dash"));
    }
}

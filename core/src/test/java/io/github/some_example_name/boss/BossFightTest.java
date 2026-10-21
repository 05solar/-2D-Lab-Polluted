package io.github.some_example_name.boss;

import com.badlogic.gdx.math.Rectangle;
import io.github.some_example_name.combat.HealthComponent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.Test;
import static org.junit.Assert.*;

/** 보스 1-1 상태 기계·패턴 3종·기둥 정책·동시 사망 우선순위 검증(순수 Java). */
public class BossFightTest {

    private static BossDefinition def;
    private static BossStageCatalog stages;

    private static BossDefinition def() {
        if (def == null) def = new BossDefinition(read(
            "../assets/boss_stage_1_1_v1/data/boss_1_1.json"));
        return def;
    }

    private static BossStageCatalog stages() {
        if (stages == null) stages = new BossStageCatalog(read(
            "../assets/boss_stage_1_1_v1/data/stages.json"));
        return stages;
    }

    private static String read(String path) {
        try {
            return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
        } catch (java.io.IOException e) { throw new RuntimeException(e); }
    }

    /** 평평한 광장 + 선택적 기둥 1개(사각 footprint). 피해는 그대로 HP에서 깎는다(무적 없음). */
    private static final class FakeCtx implements BossFight.Context {
        float px = 16f, py = 5f;
        int hp = 200;
        int damageCalls;
        Rectangle pillar; // null이면 없음
        boolean invulnerable;
        @Override public float playerX() { return px; }
        @Override public float playerY() { return py; }
        @Override public Rectangle playerBounds() { return new Rectangle(px - .3f, py, .6f, .5f); }
        @Override public boolean playerAlive() { return hp > 0; }
        @Override public boolean damagePlayer(int damage) {
            if (invulnerable) return false;
            damageCalls++;
            hp = Math.max(0, hp - damage);
            return true;
        }
        @Override public boolean blockedAt(float cx, float cy, float r) {
            if (cx < 2f + r || cy < 2f + r || cx > 30f - r || cy > 22f - r) return true; // 외곽 벽
            if (pillar == null) return false;
            float nx = Math.max(pillar.x, Math.min(cx, pillar.x + pillar.width));
            float ny = Math.max(pillar.y, Math.min(cy, pillar.y + pillar.height));
            float dx = cx - nx, dy = cy - ny;
            return dx * dx + dy * dy <= r * r;
        }
        @Override public boolean validFloor(float x, float y) {
            return !blockedAt(x, y, 0.1f);
        }
    }

    private static BossFight startedFight(FakeCtx ctx) {
        BossFight fight = new BossFight(def(), new HealthComponent(def().maxHp), 1234L, 16f, 17f);
        fight.beginIntro();
        fight.update(def().introSeconds + 0.001f, ctx); // INTRO → COMBAT
        assertEquals(BossFight.Phase.COMBAT, fight.phase());
        return fight;
    }

    /** 다음 패턴이 시작될 때까지 전진. */
    private static void advanceToPattern(BossFight fight, FakeCtx ctx, String patternId) {
        for (int i = 0; i < 2000 && !patternId.equals(fight.patternId()); i++)
            fight.update(1f / 60f, ctx);
        assertEquals(patternId, fight.patternId());
    }

    @Test public void stageCatalogUnlockRules() {
        java.util.Set<String> saved = new java.util.LinkedHashSet<>();
        assertTrue("1-1은 기본 해금+구현", stages().enterable("1-1", saved));
        assertFalse("1-2는 잠김", stages().progressUnlocked("1-2", saved));
        saved.add("1-2");
        assertTrue("클리어 기록으로 진행 해금", stages().progressUnlocked("1-2", saved));
        assertFalse("해금돼도 미구현이면 입장 불가(준비 중)", stages().enterable("1-2", saved));
        assertEquals("1-2", stages().stage("1-1").nextStage);
    }

    @Test public void introBlocksBossAttacksAndTelegraphDealsNoDamage() {
        FakeCtx ctx = new FakeCtx();
        BossFight fight = new BossFight(def(), new HealthComponent(def().maxHp), 7L, 16f, 17f);
        fight.beginIntro();
        assertEquals(BossFight.Phase.INTRO, fight.phase());
        fight.update(0.5f, ctx);
        assertEquals("소개 중 공격 없음", 0, ctx.damageCalls);
        assertNull(fight.patternId());
        fight.update(0.6f, ctx); // INTRO 종료 → COMBAT
        // 첫 패턴(투척) 예고 구간: 피해 0.
        advanceToPattern(fight, ctx, "reagent_throw");
        for (float t = 0f; t < def().throwTelegraph - 0.05f; t += 1f / 60f)
            fight.update(1f / 60f, ctx);
        assertEquals("예고 중 피해 0", 0, ctx.damageCalls);
        assertTrue(fight.projectiles().isEmpty());
    }

    @Test public void throwAimLocksAndThreeProjectilesSpawnOnce() {
        FakeCtx ctx = new FakeCtx();
        BossFight fight = startedFight(ctx);
        advanceToPattern(fight, ctx, "reagent_throw");
        // 조준 추적: 고정 전 플레이어 이동을 따라간다.
        ctx.px = 10f;
        fight.update(0.2f, ctx);
        float before = fight.aimDirX();
        ctx.px = 22f;
        fight.update(0.2f, ctx);
        assertTrue("고정 전 추적", fight.aimDirX() != before);
        // 0.5초 고정 후에는 플레이어가 움직여도 방향이 변하지 않는다.
        fight.update(0.2f, ctx); // elapsed ~0.6 > aimLock 0.5
        float lockedX = fight.aimDirX(), lockedY = fight.aimDirY();
        ctx.px = 5f;
        fight.update(0.1f, ctx);
        assertEquals("고정 후 추적 없음", lockedX, fight.aimDirX(), 0f);
        assertEquals(lockedY, fight.aimDirY(), 0f);
        // 한 번의 큰 delta가 세 발사 경계를 모두 건너도 정확히 3개 생성.
        fight.update(1.0f, ctx);
        assertEquals("3개 투사체 1회씩", 3, fight.projectiles().size());
        float prevCount = fight.projectiles().size();
        fight.update(0.05f, ctx);
        assertTrue("재발사 없음", fight.projectiles().size() <= prevCount);
    }

    @Test public void projectileStopsAtPillarAndPlayerBehindIsSafe() {
        FakeCtx ctx = new FakeCtx();
        BossFight fight = startedFight(ctx);
        advanceToPattern(fight, ctx, "reagent_throw");
        ctx.px = 16f; // 보스(16,17) 정남쪽
        ctx.py = 5f;
        ctx.pillar = new Rectangle(15.5f, 10f, 1f, 1f); // 사이 기둥
        // 투척 패턴 창(발사+비행)만 진행 — 다음 패턴으로 넘어가기 전까지.
        java.util.List<float[]> impacts = new java.util.ArrayList<>();
        int frames = 0;
        while (frames++ < 300 && "reagent_throw".equals(fight.patternId())) {
            fight.update(1f / 60f, ctx);
            impacts.addAll(fight.consumeImpactEvents());
        }
        assertEquals("기둥 뒤 플레이어 무피해(가운데 차단, 측면 ±18°는 빗나감)",
            0, ctx.damageCalls);
        boolean pillarImpact = false;
        for (float[] impact : impacts)
            if (impact[1] >= 10f && impact[1] <= 12.5f && Math.abs(impact[0] - 16f) < 1f)
                pillarImpact = true;
        assertTrue("가운데 투사체는 기둥 표면에서 첫 충돌·소멸(착탄 효과)", pillarImpact);
        assertTrue("차단된 투사체는 제거(남은 측면 2발 이하)", fight.projectiles().size() <= 2);
    }

    @Test public void contaminationPoolTicksAndPillarDoesNotOccludeLanding() {
        FakeCtx ctx = new FakeCtx();
        BossFight fight = startedFight(ctx);
        ctx.pillar = new Rectangle(15.5f, 10f, 1f, 1f);
        ctx.px = 16f;
        ctx.py = 6f; // 기둥 뒤(보스 기준) — 장판은 낙하식이라 그대로 떨어진다
        advanceToPattern(fight, ctx, "contamination");
        fight.update(0.01f, ctx);
        assertEquals("시전 시작에 장판 1개 예약(기둥 비차폐)", 1, fight.pools().size());
        BossFight.AcidPool pool = fight.pools().get(0);
        assertEquals("플레이어 바닥에 고정", 16f, pool.x, 0.6f);
        // 예고 1초: 피해 없음. 플레이어가 그대로 서 있음.
        fight.update(0.5f, ctx);
        assertEquals(0, ctx.damageCalls);
        assertFalse(pool.active());
        // 활성: 첫 틱 즉시 + 0.5s 간격. 2초 서 있으면 틱 1(활성)+4(0.5s×4)=5회 내외.
        int before = ctx.hp;
        fight.update(0.6f, ctx); // 활성 + 첫 틱
        assertTrue("활성 순간 첫 틱", ctx.hp < before);
        int afterFirst = ctx.hp;
        fight.update(0.2f, ctx);
        assertEquals("틱 간격(0.5s) 전 재피해 없음", afterFirst, ctx.hp);
        fight.update(0.4f, ctx);
        assertTrue("다음 틱", ctx.hp < afterFirst);
        // 만료 후 판정 해제.
        for (int i = 0; i < 400; i++) fight.update(1f / 60f, ctx);
        assertTrue(fight.pools().isEmpty());
    }

    @Test public void ramStopsBeforePillarAndHitsOncePerCharge() {
        FakeCtx ctx = new FakeCtx();
        BossFight fight = startedFight(ctx);
        // 앞선 투척/장판 패턴 동안에는 멀리 서서(피해·장판 위치 오염 방지) ram까지 진행.
        ctx.px = 27f;
        ctx.py = 20f;
        advanceToPattern(fight, ctx, "ram");
        ctx.px = 16f;
        ctx.py = 5f;
        ctx.damageCalls = 0;
        ctx.hp = 200;
        ctx.pillar = new Rectangle(15f, 11f, 2f, 2f); // 보스와 플레이어 사이 기둥
        int pillarFrames = 0;
        while (pillarFrames++ < 300 && "ram".equals(fight.patternId())) {
            fight.update(1f / 60f, ctx);
            ctx.hp = 200;
        }
        assertEquals("기둥에 막혀 플레이어 무피해", 0, ctx.damageCalls);
        assertTrue("기둥 앞 정지(관통/순간이동 없음)", fight.bossY() > 13f - 1.5f);
        assertFalse(fight.charging());
        // 기둥 없는 돌진: 경로 교차 1회만 피해.
        FakeCtx open = new FakeCtx();
        open.px = 27f;
        open.py = 20f;
        BossFight fight2 = startedFight(open);
        advanceToPattern(fight2, open, "ram");
        open.px = 16f;
        open.py = 9.5f; // 최대 돌진 8타일 구간 안(보스 17 → 9 도달)
        open.damageCalls = 0;
        open.hp = 200;
        int frames = 0;
        while (frames++ < 300 && "ram".equals(fight2.patternId()))
            fight2.update(1f / 60f, open);
        assertEquals("1돌진 1회 피해(정지 후 겹침 무피해)", 1, open.damageCalls);
        assertEquals(200 - def().ramDamage, open.hp);
    }

    @Test public void bossDeathCancelsHazardsAndPlaysDyingOnce() {
        FakeCtx ctx = new FakeCtx();
        BossFight fight = startedFight(ctx);
        advanceToPattern(fight, ctx, "reagent_throw");
        fight.update(1.2f, ctx); // 투사체 3개 비행 중
        assertFalse(fight.projectiles().isEmpty());
        fight.health().damage(def().maxHp); // 토벌
        fight.update(0.01f, ctx);
        assertEquals(BossFight.Phase.DYING, fight.phase());
        assertTrue("잔여 투사체/장판 제거", fight.projectiles().isEmpty() && fight.pools().isEmpty());
        int hpBefore = ctx.hp;
        fight.update(0.4f, ctx);
        assertEquals("사망 중 늦은 타이머 공격 없음", hpBefore, ctx.hp);
        fight.update(0.3f, ctx);
        assertEquals(BossFight.Phase.REWARD_PENDING, fight.phase());
        fight.markCleared();
        assertEquals(BossFight.Phase.CLEARED, fight.phase());
        assertTrue(fight.beginReturn());
        assertFalse("복귀 중복 호출 무시", fight.beginReturn());
    }

    @Test public void playerDeathFailsWithoutRewardEvenIfSimultaneous() {
        FakeCtx ctx = new FakeCtx();
        BossFight fight = startedFight(ctx);
        // 같은 프레임에 플레이어 사망 + 보스 사망 → 실패 우선.
        ctx.hp = 0;
        fight.health().damage(def().maxHp);
        fight.update(0.01f, ctx);
        assertEquals("동시 사망은 실패 우선", BossFight.Phase.FAILED, fight.phase());
        assertTrue("실패 시 잔여 판정 제거", fight.projectiles().isEmpty());
        assertTrue("실패에서 복귀/재도전 가능", fight.beginReturn());
    }

    @Test public void sameSeedSamePatternSequence() {
        FakeCtx a = new FakeCtx(), b = new FakeCtx();
        BossFight fightA = startedFight(a), fightB = startedFight(b);
        StringBuilder seqA = new StringBuilder(), seqB = new StringBuilder();
        String lastA = "", lastB = "";
        for (int i = 0; i < 1200; i++) {
            fightA.update(1f / 60f, a);
            fightB.update(1f / 60f, b);
            if (fightA.patternId() != null && !fightA.patternId().equals(lastA)) {
                lastA = fightA.patternId();
                seqA.append(lastA).append(',');
            }
            if (fightB.patternId() != null && !fightB.patternId().equals(lastB)) {
                lastB = fightB.patternId();
                seqB.append(lastB).append(',');
            }
            a.hp = 200; b.hp = 200; // 피해 누적으로 죽지 않게
        }
        assertEquals("같은 attemptSeed = 같은 패턴 순서", seqA.toString(), seqB.toString());
        assertTrue("초기 1회씩 투척→장판→돌진",
            seqA.toString().startsWith("reagent_throw,contamination,ram,"));
        // 즉시 반복 없음.
        String[] seq = seqA.toString().split(",");
        for (int i = 1; i < seq.length; i++)
            assertFalse("같은 패턴 즉시 반복 금지", seq[i].equals(seq[i - 1]));
    }
}

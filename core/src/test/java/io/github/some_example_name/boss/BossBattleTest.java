package io.github.some_example_name.boss;

import com.badlogic.gdx.math.Rectangle;
import io.github.some_example_name.combat.HealthComponent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * 9단계 범용 패턴 엔진(BossBattle) 계약 검증(순수 Java — 렌더러·GL 없음).
 * 수치는 전부 채택 카탈로그(assets/boss_nine_stages_v1/data/*.runtime.json)에서 읽는다.
 */
public class BossBattleTest {

    private static BossStageCatalog stages;
    private static BossPatternCatalog patterns;

    private static BossStageCatalog stages() {
        if (stages == null) stages = new BossStageCatalog(read(
            "../assets/boss_nine_stages_v1/data/stage_catalog.runtime.json"));
        return stages;
    }

    private static BossPatternCatalog patterns() {
        if (patterns == null) patterns = new BossPatternCatalog(read(
            "../assets/boss_nine_stages_v1/data/patterns.runtime.json"));
        return patterns;
    }

    static String read(String path) {
        try {
            return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
        } catch (java.io.IOException e) { throw new RuntimeException(e); }
    }

    /** 평평한 광장(32×24 유효) + 선택적 기둥 1개. 피해 금액 이력과 상태 이상 호출을 기록한다. */
    private static final class FakeCtx implements BossBattle.Context {
        float px = 16f, py = 5f;
        int hp = 1000;
        int damageCalls;
        final List<Integer> damageAmounts = new ArrayList<>();
        int slowCalls, rootCalls;
        float lastRootSeconds;
        Rectangle pillar;
        @Override public float playerX() { return px; }
        @Override public float playerY() { return py; }
        @Override public Rectangle playerBounds() { return new Rectangle(px - .3f, py, .6f, .5f); }
        @Override public boolean playerAlive() { return hp > 0; }
        @Override public boolean damagePlayer(int damage) {
            damageCalls++;
            damageAmounts.add(damage);
            hp = Math.max(0, hp - damage);
            return true;
        }
        @Override public boolean blockedAt(float cx, float cy, float r) {
            if (cx < 2f + r || cy < 2f + r || cx > 30f - r || cy > 22f - r) return true;
            if (pillar == null) return false;
            float nx = Math.max(pillar.x, Math.min(cx, pillar.x + pillar.width));
            float ny = Math.max(pillar.y, Math.min(cy, pillar.y + pillar.height));
            float dx = cx - nx, dy = cy - ny;
            return dx * dx + dy * dy <= r * r;
        }
        @Override public boolean sightBlocked(float fx, float fy, float tx, float ty) {
            if (pillar == null) return false;
            float dx = tx - fx, dy = ty - fy;
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            int steps = Math.max(1, (int) Math.ceil(length / 0.1f));
            for (int i = 1; i < steps; i++) {
                float x = fx + dx * i / steps, y = fy + dy * i / steps;
                if (pillar.contains(x, y)) return true;
            }
            return false;
        }
        @Override public boolean validFloor(float x, float y) { return !blockedAt(x, y, 0.1f); }
        @Override public void applySlow(float factor, float seconds) { slowCalls++; }
        @Override public void applyRoot(float seconds) { rootCalls++; lastRootSeconds = seconds; }
        int countAmount(int amount) {
            int n = 0;
            for (int a : damageAmounts) if (a == amount) n++;
            return n;
        }
    }

    private static BossBattle started(String stageId, FakeCtx ctx) {
        BossStageCatalog.Stage stage = stages().stage(stageId);
        BossBattle battle = new BossBattle(stage, patterns(),
            new HealthComponent(stage.draftMaxHp), 1234L, 16f, 17f);
        battle.beginIntro();
        battle.update(1.001f, ctx);
        assertEquals(BossFight.Phase.COMBAT, battle.phase());
        return battle;
    }

    /** 지정 패턴을 강제 큐잉하고 시작될 때까지 전진(현재 패턴이 끝난 뒤 큐 우선). */
    private static void forceAndStart(BossBattle battle, FakeCtx ctx, String patternId) {
        battle.forceNextPattern(patternId);
        for (int i = 0; i < 4000; i++) {
            BossPatternCatalog.Pattern current = battle.currentPattern();
            if (current != null && current.id.equals(patternId) && battle.patternElapsed() < 0.1f)
                return;
            battle.update(1f / 60f, ctx);
        }
        fail("패턴 " + patternId + " 미시작");
    }

    private static void step(BossBattle battle, FakeCtx ctx, float seconds) {
        for (float t = 0f; t < seconds - 0.0001f; t += 1f / 60f)
            battle.update(1f / 60f, ctx);
    }

    // ----- 예고 중 무피해 + 단일 발사 ----------------------------------------------------

    @Test public void coneTelegraphDealsNoDamageAndHitsOncePerCast() {
        FakeCtx ctx = new FakeCtx();
        BossBattle battle = started("1-2", ctx); // 냉각관 거수: cold_cone
        ctx.px = 16f; ctx.py = 13f; // 보스 바로 아래 — 부채꼴 반경 6 안
        forceAndStart(battle, ctx, "cold_cone");
        BossPatternCatalog.Pattern cone = patterns().pattern("cold_cone");
        int before = ctx.damageCalls;
        step(battle, ctx, cone.windup - 0.05f);
        assertEquals("예고(windup) 중 피해 0", before, ctx.damageCalls);
        step(battle, ctx, cone.active + 0.1f);
        assertEquals("부채꼴은 cast당 1회", before + 1, ctx.damageCalls);
        assertEquals("빙결 둔화 1회(갱신 방식)", 1, ctx.slowCalls);
        step(battle, ctx, 0.2f);
        assertEquals("활성 창에서도 재타격 없음", before + 1, ctx.damageCalls);
    }

    @Test public void coneOccludedByPillarDealsNoDamage() {
        FakeCtx ctx = new FakeCtx();
        BossBattle battle = started("1-2", ctx);
        ctx.px = 16f; ctx.py = 13f;
        ctx.pillar = new Rectangle(15f, 14.5f, 2f, 2f); // 보스(16,18 기준점)와 플레이어 사이
        forceAndStart(battle, ctx, "cold_cone");
        BossPatternCatalog.Pattern cone = patterns().pattern("cold_cone");
        step(battle, ctx, cone.windup + cone.active + 0.1f);
        assertEquals("기둥 차폐 시 부채꼴 무피해", 0, ctx.damageCalls);
    }

    @Test public void fanSpawnsExactCountOnceAtRelease() {
        FakeCtx ctx = new FakeCtx();
        BossBattle battle = started("2-1", ctx); // 저온실 다족: ice_shards 5발
        ctx.px = 16f; ctx.py = 4f; // 투사체가 바로 닿지 않게 멀리
        forceAndStart(battle, ctx, "ice_shards");
        BossPatternCatalog.Pattern fan = patterns().pattern("ice_shards");
        step(battle, ctx, fan.windup - 0.05f);
        assertTrue("발사 전 투사체 없음", battle.projectiles().isEmpty());
        step(battle, ctx, 0.1f);
        assertEquals("경계에서 정확히 count발 1회", fan.count, battle.projectiles().size());
        step(battle, ctx, 0.1f);
        assertTrue("재발사 없음", battle.projectiles().size() <= fan.count);
    }

    // ----- 프레임 독립 시간(큰 dt로 경계를 건너도 동일) -----------------------------------

    @Test public void largeTimeStepCrossesBoundariesWithoutSkippingEvents() {
        FakeCtx small = new FakeCtx();
        BossBattle a = started("1-3", small); // 배양낭 군체: acid_throw
        small.px = 16f; small.py = 9f;
        forceAndStart(a, small, "acid_throw");
        step(a, small, 2.5f); // 1/60씩
        FakeCtx big = new FakeCtx();
        BossBattle b = started("1-3", big);
        big.px = 16f; big.py = 9f;
        forceAndStart(b, big, "acid_throw");
        b.update(2.5f, big); // 한 번에 — lock·release가 한 스텝 안에서 모두 발생
        b.update(2.0f, big); // 투사체 비행(swept)도 큰 스텝 한 번
        int damage = patterns().pattern("acid_throw").damage;
        assertEquals("작은 스텝: 명중 1회", 1, small.countAmount(damage));
        assertEquals("큰 스텝에서도 발사/명중 누락·중복 없음", 1, big.countAmount(damage));
    }

    // ----- 레이저: 기둥 차폐가 판정·표시 길이를 같이 자른다 --------------------------------

    @Test public void rotatingLaserTrimsAtFirstBlocker() {
        FakeCtx open = new FakeCtx();
        BossBattle a = started("1-4", open); // 보안 제압 기체
        open.px = 16f; open.py = 14f; // 기준점(16,18)에서 3.75 — 회전 틱 간격 내 명중 보장
        forceAndStart(a, open, "rotating_laser");
        BossPatternCatalog.Pattern laser = patterns().pattern("rotating_laser");
        step(a, open, laser.windup + laser.active + 0.1f);
        assertTrue("차폐물 없으면 쓸고 지나가며 명중", open.countAmount(laser.damage) >= 1);

        FakeCtx blocked = new FakeCtx();
        BossBattle b = started("1-4", blocked);
        blocked.px = 16f; blocked.py = 10f; // 기둥 너머
        blocked.pillar = new Rectangle(15f, 13f, 2f, 2f); // 보스-플레이어 직선 중간
        forceAndStart(b, blocked, "rotating_laser");
        boolean trimmedSeen = false;
        for (float t = 0f; t < laser.windup + laser.active + 0.1f; t += 1f / 60f) {
            b.update(1f / 60f, blocked);
            if (b.beam.active && b.beam.trimmedLength < laser.length - 0.5f) trimmedSeen = true;
        }
        assertEquals("기둥 뒤 플레이어 무피해", 0, blocked.countAmount(laser.damage));
        assertTrue("표시 길이도 첫 차폐물에서 잘림", trimmedSeen);
    }

    // ----- 연속 돌진: swept 정지 + 예고 중에만 재조준 -------------------------------------

    @Test public void chainChargeStopsAtPillarWithoutTunneling() {
        FakeCtx ctx = new FakeCtx();
        BossBattle battle = started("2-1", ctx);
        ctx.px = 16f; ctx.py = 5f; // 남쪽 — 돌진 경로에 기둥
        ctx.pillar = new Rectangle(15f, 10f, 2f, 2f);
        forceAndStart(battle, ctx, "chain_charge");
        BossPatternCatalog.Pattern charge = patterns().pattern("chain_charge");
        step(battle, ctx, charge.betweenChargeTelegraph + 1.2f); // 첫 회차 완료까지
        assertTrue("기둥 앞에서 정지(관통 금지): y=" + battle.bossY(), battle.bossY() > 12f);
        assertEquals("기둥 뒤 플레이어 무피해", 0, ctx.countAmount(charge.damage));
    }

    @Test public void chainChargeReaimsOnlyDuringTelegraph() {
        FakeCtx ctx = new FakeCtx();
        BossBattle battle = started("2-1", ctx);
        ctx.px = 16f; ctx.py = 5f;
        forceAndStart(battle, ctx, "chain_charge");
        step(battle, ctx, patterns().pattern("chain_charge").betweenChargeTelegraph - 0.1f);
        // 예고 중 이동 → 조준 추적.
        ctx.px = 24f;
        battle.update(1f / 60f, ctx);
        float aimBefore = battle.aimDirX();
        assertTrue("예고 중 재조준", aimBefore > 0.3f);
        step(battle, ctx, 0.15f); // 돌진 시작(방향 고정)
        assertTrue(battle.charging());
        ctx.px = 8f; // 돌진 중 반대편으로 이동
        battle.update(1f / 60f, ctx);
        assertEquals("돌진 중에는 방향 불변(보이지 않는 유도 금지)",
            aimBefore, battle.aimDirX(), 0.0001f);
    }

    // ----- 확장 고리: cast당 1회 + 차폐 방향 차단 ----------------------------------------

    @Test public void annulusHitsOncePerCastAndRespectsOcclusion() {
        FakeCtx ctx = new FakeCtx();
        BossBattle battle = started("1-5", ctx); // 신경 접속 표본
        ctx.px = 16f; ctx.py = 13.5f; // 기준점(16,18)에서 거리 ~4.25 — 1→9 확장이 통과
        forceAndStart(battle, ctx, "psychic_wave");
        BossPatternCatalog.Pattern wave = patterns().pattern("psychic_wave");
        step(battle, ctx, wave.windup + wave.active + 0.2f);
        assertEquals("고리는 cast당 1회", 1, ctx.countAmount(wave.damage));

        FakeCtx blocked = new FakeCtx();
        BossBattle b = started("1-5", blocked);
        blocked.px = 16f; blocked.py = 13.5f;
        blocked.pillar = new Rectangle(15f, 15f, 2f, 2f);
        forceAndStart(b, blocked, "psychic_wave");
        step(b, blocked, wave.windup + wave.active + 0.2f);
        assertEquals("차폐된 방향은 고리 무피해", 0, blocked.countAmount(wave.damage));
    }

    // ----- 장판: 겹침 공유 틱 상한 -------------------------------------------------------

    @Test public void overlappingToxinZonesShareTickGate() {
        FakeCtx ctx = new FakeCtx();
        BossBattle battle = started("2-2", ctx); // 변이 식물
        ctx.px = 25f; ctx.py = 5f; // 보스에서 떨어진 곳
        // 같은 그룹(boss_toxin) 장판 2개를 플레이어 위에 겹쳐 즉시 활성화.
        BossPatternCatalog.Pattern toxin = patterns().pattern("toxic_zone");
        battle.zones().add(new BossBattle.Zone(25f, 5f, toxin.radius, 0f, 5f, toxin.damage,
            toxin.tickSeconds, toxin.sharedTickGroup, toxin.art, 0f, 0f));
        battle.zones().add(new BossBattle.Zone(25.5f, 5f, toxin.radius, 0f, 5f, toxin.damage,
            toxin.tickSeconds, toxin.sharedTickGroup, toxin.art, 0f, 0f));
        step(battle, ctx, 1.3f);
        assertEquals("겹친 장판은 그룹 틱 공유: 1.3초에 3틱(0, 0.6, 1.2)",
            3, ctx.countAmount(toxin.damage));
    }

    // ----- 뿌리 구속: 1회 적용 + 재적용 면역 + 이동만 제한 ---------------------------------

    @Test public void rootBindAppliesOnceThenImmunityBlocksReapply() {
        FakeCtx ctx = new FakeCtx();
        BossBattle battle = started("2-2", ctx);
        ctx.px = 25f; ctx.py = 5f;
        battle.forceNextPattern("toxic_zone"); // 추첨이 root_bind를 뽑아 간섭하지 않게 고정
        BossPatternCatalog.Pattern bind = patterns().pattern("root_bind");
        battle.zones().add(new BossBattle.Zone(25f, 5f, bind.radius, 0f, 5f, bind.damage,
            0.3f, bind.sharedTickGroup, bind.art, bind.rootSeconds, bind.rootImmunitySeconds));
        battle.zones().add(new BossBattle.Zone(25.4f, 5f, bind.radius, 0f, 5f, bind.damage,
            0.3f, bind.sharedTickGroup, bind.art, bind.rootSeconds, bind.rootImmunitySeconds));
        step(battle, ctx, 0.5f);
        assertEquals("겹친 구속 장판도 1회만 적용", 1, ctx.rootCalls);
        assertEquals(bind.rootSeconds, ctx.lastRootSeconds, 0.001f);
        step(battle, ctx, 1.2f); // 면역(2초) 안
        assertEquals("재적용 면역 중 추가 구속 없음", 1, ctx.rootCalls);
        step(battle, ctx, 1.0f); // 면역 해제 후 두 번째 장판이 아직 활성
        assertEquals("면역 해제 후에만 재적용", 2, ctx.rootCalls);
    }

    // ----- 지연 폭발: 사망 시 예약 공격 전부 취소 -----------------------------------------

    @Test public void bossDeathCancelsScheduledBlastsAndHazards() {
        FakeCtx ctx = new FakeCtx();
        BossBattle battle = started("1-5", ctx);
        ctx.px = 16f; ctx.py = 9f;
        forceAndStart(battle, ctx, "delayed_blast");
        BossPatternCatalog.Pattern blast = patterns().pattern("delayed_blast");
        step(battle, ctx, blast.targetLockAt + 0.1f);
        assertFalse("표식 예약됨", battle.bursts().isEmpty());
        int damageBefore = ctx.damageCalls;
        battle.health().damage(battle.health().hp()); // 즉사
        battle.update(1f / 60f, ctx);
        assertEquals(BossFight.Phase.DYING, battle.phase());
        assertTrue("예약 폭발 취소", battle.bursts().isEmpty());
        assertTrue(battle.projectiles().isEmpty() && battle.zones().isEmpty());
        step(battle, ctx, 3f);
        assertEquals("사망 후 추가 피해 없음", damageBefore, ctx.damageCalls);
    }

    @Test public void delayedBlastTargetsFixedAtLockWithWarningOverOneSecond() {
        FakeCtx ctx = new FakeCtx();
        BossBattle battle = started("1-5", ctx);
        ctx.px = 16f; ctx.py = 9f;
        forceAndStart(battle, ctx, "delayed_blast");
        BossPatternCatalog.Pattern blast = patterns().pattern("delayed_blast");
        step(battle, ctx, blast.targetLockAt + 0.05f);
        assertEquals("타깃 수만큼 표식", blast.targetCount, battle.bursts().size());
        BossBattle.Burst first = battle.bursts().get(0);
        assertTrue("예고 1초 이상", first.delayRemaining >= 0.9f);
        float fx = first.x, fy = first.y;
        ctx.px = 24f; // 표식 후 이동 — 위치 불변이어야 함
        step(battle, ctx, 0.3f);
        assertEquals(fx, battle.bursts().get(0).x, 0.0001f);
        assertEquals(fy, battle.bursts().get(0).y, 0.0001f);
    }

    // ----- 분노 전환: HP 50% 1회, 전환 중 신규 피해 없음 ----------------------------------

    @Test public void enrageTriggersOnceAtHalfHpWithNoDamageDuringTransition() {
        FakeCtx ctx = new FakeCtx();
        BossBattle battle = started("1-보스", ctx);
        ctx.px = 16f; ctx.py = 4f;
        battle.health().damage(battle.health().maxHp() / 2 + 10); // 50% 아래로
        int damageBefore = ctx.damageCalls;
        BossPatternCatalog.Pattern enrage = patterns().pattern("enrage_transition");
        for (int i = 0; i < 4000 && !battle.enraged(); i++) {
            battle.update(1f / 60f, ctx);
            BossPatternCatalog.Pattern current = battle.currentPattern();
            if (current != null && "phase_change".equals(current.kind)) {
                assertTrue("전환 중 해저드 없음", battle.projectiles().isEmpty()
                    && battle.zones().isEmpty() && battle.bursts().isEmpty());
                assertEquals("전환 중 신규 피해 없음", damageBefore, ctx.damageCalls);
            }
        }
        assertTrue("분노 전환 완료", battle.enraged());
        assertEquals(0.85f, enrage.recoveryMultiplierAfter, 0.001f);
        // 한 번만: 이후 오래 진행해도 전환 패턴 재등장 없음.
        for (int i = 0; i < 600; i++) {
            battle.update(1f / 60f, ctx);
            BossPatternCatalog.Pattern current = battle.currentPattern();
            if (current != null) assertFalse("전환은 1회", "phase_change".equals(current.kind));
        }
    }

    // ----- 동시 사망: 실패 우선(기존 정책 유지) ------------------------------------------

    @Test public void simultaneousDeathPrefersFailure() {
        FakeCtx ctx = new FakeCtx();
        BossBattle battle = started("1-2", ctx);
        battle.health().damage(battle.health().hp());
        ctx.hp = 0;
        battle.update(1f / 60f, ctx);
        assertEquals("플레이어·보스 동시 사망 → 실패 우선", BossFight.Phase.FAILED, battle.phase());
    }

    // ----- 이동 포격: 발사 시 고정 + 보스 이동 -------------------------------------------

    @Test public void movingMortarMovesBossWhileTargetsStayFixed() {
        FakeCtx ctx = new FakeCtx();
        BossBattle battle = started("2-3", ctx); // 이동형 배양 탱크
        ctx.px = 16f; ctx.py = 5f;
        forceAndStart(battle, ctx, "moving_mortar");
        BossPatternCatalog.Pattern mortar = patterns().pattern("moving_mortar");
        float bossYBefore = battle.bossY();
        step(battle, ctx, mortar.windup + mortar.active);
        assertTrue("이동 포격 중 보스가 이동", battle.bossY() < bossYBefore - 0.05f);
    }

    // ----- 누출 흔적: 수명·최대 개수 상한 ------------------------------------------------

    @Test public void toxicLeakRespectsMaxZonesAndLifetime() {
        FakeCtx ctx = new FakeCtx();
        BossBattle battle = started("2-3", ctx);
        ctx.px = 16f; ctx.py = 4f;
        forceAndStart(battle, ctx, "toxic_leak");
        BossPatternCatalog.Pattern leak = patterns().pattern("toxic_leak");
        battle.forceNextPattern("moving_mortar"); // 다음 추첨이 누출을 다시 뽑지 않게 고정
        step(battle, ctx, leak.windup + leak.active);
        assertTrue("동시 장판 상한", battle.zones().size() <= leak.maxZones);
        step(battle, ctx, 3.5f); // 마지막 드롭 수명(4초) 경과 지점까지
        assertTrue("수명 경과 후 소멸", battle.zones().isEmpty());
    }
}

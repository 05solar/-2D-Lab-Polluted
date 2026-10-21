package io.github.some_example_name.combat;

import io.github.some_example_name.entity.monster.Monster;
import io.github.some_example_name.profile.SkillCatalog;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * 충격파/전기 투척/보호막 판정 모델 검증(순수 Java — 렌더러·GL 없음).
 * 수치는 실제 채택 카탈로그(assets/ui/player_three_skills_v1/data/skills.json)에서 읽는다.
 */
public class ThreeSkillsModelTest {

    private static SkillCatalog catalog;

    private static SkillCatalog catalog() {
        if (catalog == null) try {
            catalog = new SkillCatalog(new String(Files.readAllBytes(
                Paths.get("../assets/ui/player_three_skills_v1/data/skills.json")),
                StandardCharsets.UTF_8));
        } catch (java.io.IOException e) { throw new RuntimeException(e); }
        return catalog;
    }

    private static Monster slime(long id, float x, float y) {
        return new Monster(id, MonsterType.SLIME, x, y, 0.6f, 0.5f, 20);
    }

    private static ShockwaveState wave(float ox, float oy) {
        SkillCatalog c = catalog();
        return new ShockwaveState(ox, oy, c.shockwaveDamageStart, c.shockwaveDamageEnd,
            c.shockwaveDuration, new ShockwaveState.RadiusAt() {
                @Override public float radius(float t) { return catalog().shockwaveFrontRadius(t); }
            }, 1f, 0f);
    }

    // ----- 카탈로그/타임라인 ----------------------------------------------------------

    @Test public void catalogReadsAdoptedDraftValues() {
        SkillCatalog c = catalog();
        assertEquals(6f, c.shockwaveCooldownSeconds, .001f);
        assertEquals(30, c.shockwaveDamage);
        assertEquals(2.5f, c.shockwaveRadius, .001f);
        assertEquals(0.8f, c.shockwaveKnockback, .001f);
        assertEquals(1f / 15f, c.shockwaveDamageStart, .0001f);
        assertEquals(5f / 15f, c.shockwaveDamageEnd, .0001f);
        assertEquals(5f, c.electricCooldownSeconds, .001f);
        assertEquals(25, c.electricDamage);
        assertEquals(10f, c.electricSpeed, .001f);
        assertEquals(8f, c.electricMaxDistance, .001f);
        assertEquals(0.5f, c.electricStunSeconds, .001f);
        assertEquals(2f / 15f, c.electricSpawnDelay, .0001f);
        assertEquals(12f, c.shieldCooldownSeconds, .001f);
        assertEquals(60, c.shieldAbsorbAmount);
        assertEquals(4f, c.shieldDurationSeconds, .001f);
        assertEquals(2f / 15f, c.shieldActivateDelay, .0001f);
        // radiusCurve: 단조 증가, 최대에서 전체 반경.
        assertEquals(0f, c.shockwaveFrontRadius(0f), .0001f);
        assertTrue(c.shockwaveFrontRadius(0.1f) < c.shockwaveFrontRadius(0.2f));
        assertEquals(2.5f, c.shockwaveFrontRadius(4f / 15f), .001f);
        assertEquals("곡선 끝 이후 재팽창 없음", 2.5f, c.shockwaveFrontRadius(1f), .001f);
    }

    @Test public void castEventFiresExactlyOnceAtBoundary() {
        SkillCast cast = new SkillCast(2f / 15f);
        assertFalse("준비 중에는 발생하지 않음", cast.update(0.1f));
        assertTrue("경계 통과에 1회", cast.update(0.1f));
        assertFalse("재발생 없음", cast.update(10f));
        SkillCast zero = new SkillCast(0f);
        assertTrue("t=0 이벤트는 시작 시 실행", zero.update(0f));
        assertFalse(zero.update(1f));
        SkillCast big = new SkillCast(2f / 15f);
        assertTrue("큰 delta로 경계를 건너도 누락 없음", big.update(5f));
        SkillCast cancelled = new SkillCast(2f / 15f);
        cancelled.cancel();
        assertFalse("준비 중 중단 → 효과 취소", cancelled.update(5f));
    }

    // ----- 충격파 -------------------------------------------------------------------

    @Test public void shockwaveHitsWhenFrontReachesNotBefore() {
        ShockwaveState wave = wave(10f, 10f);
        Monster near = slime(1, 10.9f, 10f);   // 몸 중심 ~0.9타일
        Monster far = slime(2, 12.2f, 10f);    // ~2.2타일 — 나중 프레임에 도달
        List<Monster> all = Arrays.asList(near, far);
        // 0~1/15초: 충전 프레임 — 피해 없음.
        assertTrue(wave.update(0.05f, all, null).isEmpty());
        // 1/15→2/15: 파면 0→~0.98→1.31타일. near만 걸린다.
        List<ShockwaveState.Hit> first = wave.update(1f / 15f, all, null);
        boolean nearHit = false, farHit = false;
        for (ShockwaveState.Hit hit : first) {
            if (hit.monster == near) nearHit = true;
            if (hit.monster == far) farHit = true;
        }
        assertTrue("파면 도달 시 1회 명중", nearHit);
        assertFalse("바깥 대상은 처음부터 맞지 않는다", farHit);
        // 진행: far도 파면 도달에 1회.
        List<ShockwaveState.Hit> later = wave.update(0.2f, all, null);
        assertEquals(1, later.size());
        assertSame(far, later.get(0).monster);
        // 잔상 구간: 최대 반경 안이어도 재타격 없음.
        assertTrue(wave.update(0.2f, all, null).isEmpty());
        assertTrue(wave.update(0.1f, all, null).isEmpty());
        assertTrue(wave.finished());
    }

    @Test public void shockwaveLargeDeltaCoversSkippedBoundariesAndOriginTarget() {
        ShockwaveState wave = wave(10f, 10f);
        Monster onOrigin = slime(1, 10f, 10f);  // 원점 바로 위
        Monster mid = slime(2, 11.5f, 10f);
        Monster outside = slime(3, 13.5f, 10f); // 반경 2.5 밖
        List<Monster> all = Arrays.asList(onOrigin, mid, outside);
        // 한 번의 큰 delta가 전체 피해 창을 통과 — 모두 순서 없이 누락/중복 없이 1회.
        List<ShockwaveState.Hit> hits = wave.update(1f, all, null);
        assertEquals(2, hits.size());
        assertTrue(wave.update(1f, all, null).isEmpty());
        // 원점 대상 방향: NaN 없이 폴백 방향 정규화.
        for (ShockwaveState.Hit hit : hits) {
            assertFalse(Float.isNaN(hit.dirX));
            assertFalse(Float.isNaN(hit.dirY));
            assertEquals(1f, hit.dirX * hit.dirX + hit.dirY * hit.dirY, .001f);
        }
    }

    @Test public void shockwaveRespectsOcclusionAndMovingTargets() {
        ShockwaveState wave = wave(10f, 10f);
        final Monster behindWall = slime(1, 11.5f, 10f);
        List<Monster> all = new ArrayList<>();
        all.add(behindWall);
        ShockwaveState.Occlusion wall = new ShockwaveState.Occlusion() {
            @Override public boolean blocked(float fx, float fy, float tx, float ty) {
                return true; // 벽 뒤 — 원형 거리만으로 때리지 않는다
            }
        };
        assertTrue("차폐된 대상은 맞지 않는다", wave.update(0.2f, all, wall).isEmpty());
        // 이동하는 적: 아직 창 안(0.2→0.26초), 파면 바깥에서 안쪽으로 들어온 대상도
        // 이번 구간 annulus에 걸리면 1회 명중.
        Monster mover = slime(2, 12.4f, 10f);
        all.clear();
        all.add(mover);
        mover.bounds().setPosition(12.4f - mover.bounds().width / 2f, 10f);
        List<ShockwaveState.Hit> hits = wave.update(0.2f, all, null);
        assertEquals("파면이 이번에 덮은 고리에 걸림", 1, hits.size());
    }

    // ----- 전기 투척 ----------------------------------------------------------------

    private static final ElectricProjectile.Solid NO_WALL = new ElectricProjectile.Solid() {
        @Override public boolean blockedAt(float x, float y, float r) { return false; }
    };

    @Test public void projectileDirectionFixedAndSingleHit() {
        ElectricProjectile p = new ElectricProjectile(0f, 0f, 1f, 0f, 10f, 8f, 0.18f);
        Monster target = slime(1, 3f, -0.2f);
        Monster behind = slime(2, 5f, -0.2f);
        List<Monster> all = Arrays.asList(target, behind);
        assertEquals(ElectricProjectile.Outcome.FLYING, p.update(0.1f, NO_WALL, all));
        assertEquals("방향은 시전 시 확정(유도 없음)", 1f, p.dirX(), .0001f);
        ElectricProjectile.Outcome out = p.update(0.3f, NO_WALL, all);
        assertEquals(ElectricProjectile.Outcome.HIT_MONSTER, out);
        assertSame("첫 충돌 대상만", target, p.hitMonster());
        assertFalse("1투사체 1충돌 — 소멸", p.alive());
    }

    @Test public void projectileSweptCollisionStopsAtThinWallBeforeMonster() {
        // 0.2타일 두께 벽이 x=2에 — 큰 delta(한 프레임 5타일)에도 통과하지 못한다.
        ElectricProjectile.Solid thinWall = new ElectricProjectile.Solid() {
            @Override public boolean blockedAt(float x, float y, float r) {
                return x + r > 2f && x - r < 2.2f;
            }
        };
        ElectricProjectile p = new ElectricProjectile(0f, 0f, 1f, 0f, 10f, 8f, 0.18f);
        Monster monsterBehind = slime(1, 3f, -0.2f);
        ElectricProjectile.Outcome out = p.update(0.5f, thinWall, Arrays.asList(monsterBehind));
        assertEquals("벽이 몬스터보다 앞 — 피해 없음", ElectricProjectile.Outcome.HIT_WALL, out);
        assertNull(p.hitMonster());
        assertTrue("착탄 위치는 벽 표면 근처", p.x() <= 2.2f);
    }

    @Test public void projectileExpiresAtMaxRangeAndIgnoresDead() {
        ElectricProjectile p = new ElectricProjectile(0f, 0f, 1f, 0f, 10f, 8f, 0.18f);
        Monster dead = slime(1, 4f, -0.2f);
        dead.health().damage(999);
        assertEquals("죽은 대상 무시 + 사거리 소멸(피해·착탄 없음)",
            ElectricProjectile.Outcome.EXPIRED, p.update(2f, NO_WALL, Arrays.asList(dead)));
        assertNull(p.hitMonster());
    }

    @Test public void shockRefreshUsesMaxNotSum() {
        Monster monster = slime(1, 0f, 0f);
        monster.applyShock(0.5f);
        monster.reduceShockTime(0.3f);
        assertEquals(0.2f, monster.shockTime(), .0001f);
        monster.applyShock(0.5f);
        assertEquals("재적용 = max(남은, 새) — 무한 중첩 없음", 0.5f, monster.shockTime(), .0001f);
        monster.applyShock(0.1f);
        assertEquals("더 짧은 재적용은 줄이지 않는다", 0.5f, monster.shockTime(), .0001f);
        monster.reduceShockTime(1f);
        assertFalse("시간 종료 — 정상 복귀", monster.shocked());
    }

    // ----- 보호막 -------------------------------------------------------------------

    private static PlayerShieldState shield() {
        PlayerShieldState s = new PlayerShieldState(catalog().shieldActivateDelay);
        assertTrue(s.tryCast(60, 4f));
        s.update(1f); // 준비 통과 + 활성
        assertTrue(s.active());
        return s;
    }

    @Test public void shieldAbsorbsAndOverflowsToHp() {
        PlayerShieldState s = shield();
        assertEquals("60에서 35 피해 → HP 몫 0", 0, s.absorb(35));
        assertEquals(25, s.absorbRemaining());
        assertEquals("25에서 35 피해 → 0 + HP 10", 10, s.absorb(35));
        assertEquals(0, s.absorbRemaining());
        assertTrue("흡수량 0 → 파괴 1회", s.consumeBroke());
        assertFalse("파괴는 1회만", s.consumeBroke());
        assertFalse(s.active());
        assertEquals("파괴 이후에는 막지 않는다", 35, s.absorb(35));
        assertFalse("파괴는 만료가 아니다", s.consumeExpired());
    }

    @Test public void shieldStartupDoesNotBlockAndRejectsRecast() {
        PlayerShieldState s = new PlayerShieldState(2f / 15f);
        assertTrue(s.tryCast(60, 4f));
        assertTrue(s.casting());
        assertEquals("준비 중에는 아직 피해를 막지 않는다", 35, s.absorb(35));
        assertFalse("준비 중 재시전 거부", s.tryCast(60, 4f));
        s.update(0.2f);
        assertTrue(s.consumeActivated());
        assertFalse("활성 중 재시전 거부(흡수량 재설정 없음)", s.tryCast(60, 4f));
        assertEquals(60, s.absorbRemaining());
    }

    @Test public void shieldExpiryIsResolvedBeforeLaterDamage() {
        PlayerShieldState s = shield();
        // 큰 delta가 남은 4초를 전부 통과 — 만료 먼저, 이후 피해는 흡수 금지.
        s.update(10f);
        assertFalse(s.active());
        assertTrue("시간 만료 이벤트(파괴와 구분)", s.consumeExpired());
        assertFalse(s.consumeBroke());
        assertEquals("만료 이후 피해는 전액 HP로", 35, s.absorb(35));
        // 만료 뒤 재시전은 다시 가능(쿨다운 판정은 공통 서비스 몫).
        assertTrue(s.tryCast(60, 4f));
    }

    @Test public void shieldClearOnDeathRemovesEverything() {
        PlayerShieldState s = shield();
        s.clear();
        assertFalse(s.busy());
        assertEquals("정리 후 흡수 없음", 35, s.absorb(35));
        assertFalse(s.consumeBroke());
        assertFalse(s.consumeExpired());
    }

    @Test public void shieldIntegratesWithCommonDamagePipeline() {
        io.github.some_example_name.entity.player.Player player =
            new io.github.some_example_name.entity.player.Player(5f, 5f, 0.6f, 0.5f, 100);
        PlayerCombatState combat = new PlayerCombatState(
            new PlayerAttackState(0.5f, 4, new int[]{2}));
        PlayerShieldState s = shield();
        combat.setShield(s);
        assertTrue(combat.takeDamage(player, 35, 0f, 0f, 0.5f));
        assertEquals("보호막이 전부 흡수 — HP 그대로", 100, player.hp());
        assertEquals(25, s.absorbRemaining());
        assertTrue("수용된 피격은 무적시간 시작", combat.invulnerable());
        assertFalse("무적 중 공격은 보호막도 깎지 않는다", combat.takeDamage(player, 35, 0f, 0f, 0.5f));
        assertEquals(25, s.absorbRemaining());
        combat.update(1f); // 무적 해제
        assertTrue(combat.takeDamage(player, 35, 0f, 0f, 0.5f));
        assertEquals("25 흡수 + 잔여 10만 HP 차감(같은 사건)", 90, player.hp());
        assertTrue(s.consumeBroke());
    }
}

package io.github.some_example_name.combat;

import io.github.some_example_name.entity.Direction;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * 돌진 "이동" 상태 단위 검증. 쿨다운은 공통 SkillRuntime("dash")이 관리하므로 여기서는
 * 방향·총거리·중단·대상당 1회 타격만 본다(쿨다운 규칙은 SkillSystemTest).
 */
public class PlayerDashStateTest {

    private static PlayerDashState dash() { return new PlayerDashState(3f, .18f); }

    @Test public void dashesAlongMovementInputOrFacingFallback() {
        PlayerDashState d = dash();
        assertTrue(d.start(1f, 1f, Direction.DOWN));        // 대각선 입력 정규화
        assertEquals(.7071f, d.dirX(), .001f);
        assertEquals(.7071f, d.dirY(), .001f);

        PlayerDashState idle = dash();
        assertTrue(idle.start(0f, 0f, Direction.LEFT));     // 입력 없음 → 바라본 방향
        assertEquals(-1f, idle.dirX(), .0001f);
        assertEquals(0f, idle.dirY(), .0001f);
    }

    @Test public void cannotRestartWhileActive() {
        PlayerDashState d = dash();
        assertTrue(d.start(1f, 0f, Direction.RIGHT));
        assertFalse("돌진 중 재발동 금지", d.start(1f, 0f, Direction.RIGHT));
        d.advance(.18f);                                     // 돌진 종료
        assertFalse(d.active());
        assertTrue("종료 후에는 다시 시작 가능(쿨다운 판정은 공통 서비스 몫)",
            d.start(1f, 0f, Direction.RIGHT));
    }

    @Test public void totalDashDistanceIsExactlyThreeTilesAcrossFrames() {
        PlayerDashState d = dash();
        assertTrue(d.start(0f, 1f, Direction.UP));
        float moved = 0f;
        for (int i = 0; i < 60; i++) moved += d.advance(1f / 60f);
        assertEquals(3f, moved, .0001f);
        assertFalse(d.active());
    }

    @Test public void eachMonsterHitOnlyOncePerDash() {
        PlayerDashState d = dash();
        d.start(1f, 0f, Direction.RIGHT);
        assertTrue(d.markHit(7L));
        assertFalse("같은 몬스터 중복 타격 금지", d.markHit(7L));
        assertTrue("다른 몬스터는 각각 맞는다", d.markHit(8L));
        d.advance(.18f);
        d.start(1f, 0f, Direction.RIGHT);
        assertTrue("새 돌진에서는 다시 맞을 수 있다", d.markHit(7L));
    }

    @Test public void interruptStopsMovementImmediately() {
        PlayerDashState d = dash();
        d.start(1f, 0f, Direction.RIGHT);
        d.advance(.06f);
        d.interrupt();                                       // 벽에 정면으로 막힘
        assertFalse(d.active());
        assertEquals(0f, d.advance(1f), .0001f);             // 더 이상 이동 없음
    }
}

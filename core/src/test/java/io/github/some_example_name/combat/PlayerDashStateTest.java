package io.github.some_example_name.combat;

import io.github.some_example_name.entity.Direction;
import org.junit.Test;
import static org.junit.Assert.*;

public class PlayerDashStateTest {

    private static PlayerDashState dash() { return new PlayerDashState(3f, .18f, 3f); }

    @Test public void dashesAlongMovementInputOrFacingFallback() {
        PlayerDashState d = dash();
        assertTrue(d.tryStart(1f, 1f, Direction.DOWN));     // 대각선 입력 정규화
        assertEquals(.7071f, d.dirX(), .001f);
        assertEquals(.7071f, d.dirY(), .001f);

        PlayerDashState idle = dash();
        assertTrue(idle.tryStart(0f, 0f, Direction.LEFT));  // 입력 없음 → 바라본 방향
        assertEquals(-1f, idle.dirX(), .0001f);
        assertEquals(0f, idle.dirY(), .0001f);
    }

    @Test public void holdingKeyCannotRetriggerWhileActiveOrCoolingDown() {
        PlayerDashState d = dash();
        assertTrue(d.tryStart(1f, 0f, Direction.RIGHT));
        assertFalse("돌진 중 재발동 금지", d.tryStart(1f, 0f, Direction.RIGHT));
        d.advance(.18f);                                     // 돌진 종료
        assertFalse(d.active());
        assertFalse("쿨다운 중 재발동 금지", d.tryStart(1f, 0f, Direction.RIGHT));
        d.advance(3f);                                       // 쿨다운 소진
        assertTrue(d.ready());
        assertTrue(d.tryStart(1f, 0f, Direction.RIGHT));
    }

    @Test public void totalDashDistanceIsExactlyThreeTilesAcrossFrames() {
        PlayerDashState d = dash();
        assertTrue(d.tryStart(0f, 1f, Direction.UP));
        float moved = 0f;
        for (int i = 0; i < 60; i++) moved += d.advance(1f / 60f);
        assertEquals(3f, moved, .0001f);
        assertFalse(d.active());
    }

    @Test public void cooldownStartsAtActivationAndLastsThreeSeconds() {
        PlayerDashState d = dash();
        d.tryStart(1f, 0f, Direction.RIGHT);
        assertEquals(3f, d.cooldownRemaining(), .0001f);     // 발동 순간부터
        d.advance(.18f);
        assertEquals(2.82f, d.cooldownRemaining(), .001f);
        d.advance(2.82f);
        assertEquals(0f, d.cooldownRemaining(), .0001f);
        assertTrue(d.ready());
    }

    @Test public void eachMonsterHitOnlyOncePerDash() {
        PlayerDashState d = dash();
        d.tryStart(1f, 0f, Direction.RIGHT);
        assertTrue(d.markHit(7L));
        assertFalse("같은 몬스터 중복 타격 금지", d.markHit(7L));
        assertTrue("다른 몬스터는 각각 맞는다", d.markHit(8L));
        d.advance(.18f);
        d.advance(3f);
        d.tryStart(1f, 0f, Direction.RIGHT);
        assertTrue("새 돌진에서는 다시 맞을 수 있다", d.markHit(7L));
    }

    @Test public void interruptStopsMovementButKeepsCooldown() {
        PlayerDashState d = dash();
        d.tryStart(1f, 0f, Direction.RIGHT);
        d.advance(.06f);
        d.interrupt();                                       // 벽에 정면으로 막힘
        assertFalse(d.active());
        assertEquals(0f, d.advance(1f), .0001f);             // 더 이상 이동 없음
        assertTrue("쿨다운은 유지된다", d.cooldownRemaining() > 0f);
    }
}

package io.github.some_example_name.movement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.github.some_example_name.collision.CollisionSystem;
import io.github.some_example_name.collision.SolidGrid;
import io.github.some_example_name.entity.player.Player;
import io.github.some_example_name.input.PlayerIntent;

/** 이동 거리 = speed*delta(프레임 독립) 와 벽 통과 방지 검증. */
public class MovementSystemTest {

    private static final float WALK = 4f;
    private static final float RUN = 8f;
    private static final float W = 0.6f;
    private static final float H = 0.5f;

    private MovementSystem newSystem() {
        return new MovementSystem(new CollisionSystem(), new RunStartDetector(), WALK, RUN);
    }

    private Player newPlayer() {
        return new Player(50f, 50f, W, H, 100); // 발밑 (50,50)
    }

    @Test
    public void distanceIsFrameRateIndependent() {
        SolidGrid open = new OpenGrid();
        PlayerIntent right = new PlayerIntent(1f, 0f, false, false, false);

        Player oneBigStep = newPlayer();
        newSystem().update(oneBigStep, right, open, 0.2f);

        Player twoSmallSteps = newPlayer();
        MovementSystem sys = newSystem();
        sys.update(twoSmallSteps, right, open, 0.1f);
        sys.update(twoSmallSteps, right, open, 0.1f);

        assertEquals(oneBigStep.feetX(), twoSmallSteps.feetX(), 1e-4f);
        // 0.2초 동안 걷기 속도 4 -> 0.8 이동
        assertEquals(50.8f, oneBigStep.feetX(), 1e-4f);
    }

    @Test
    public void runningMovesFurtherThanWalking() {
        SolidGrid open = new OpenGrid();
        Player walker = newPlayer();
        newSystem().update(walker, new PlayerIntent(1f, 0f, false, false, false), open, 0.1f);

        Player runner = newPlayer();
        newSystem().update(runner, new PlayerIntent(1f, 0f, true, false, false), open, 0.1f);

        assertTrue(runner.feetX() > walker.feetX());
        assertEquals(50.4f, walker.feetX(), 1e-4f); // 4 * 0.1
        assertEquals(50.8f, runner.feetX(), 1e-4f); // 8 * 0.1
    }

    @Test
    public void wallBlocksMovementNoPassThrough() {
        // 타일 (51,50) 이 벽. 플레이어 오른쪽으로 이동 시 벽 왼쪽(x=51)에서 멈춰야 한다.
        SolidGrid walled = new SingleWallGrid(51, 50);
        Player p = newPlayer(); // bounds x=49.7, 오른쪽 모서리 50.3
        newSystem().update(p, new PlayerIntent(1f, 0f, true, false, false), walled, 0.1f); // dx=0.8

        float rightEdge = p.bounds().x + p.bounds().width;
        assertEquals("오른쪽 모서리는 벽 경계 x=51 에서 멈춘다", 51f, rightEdge, 1e-3f);
        assertTrue("벽을 통과하지 않는다", rightEdge <= 51f + 1e-3f);
    }

    /** 범위 밖만 고체인 넓은 빈 공간. */
    private static final class OpenGrid implements SolidGrid {
        @Override public boolean isSolid(int tileX, int tileY) {
            return tileX < 0 || tileY < 0 || tileX >= 100 || tileY >= 100;
        }
        @Override public int widthInTiles() { return 100; }
        @Override public int heightInTiles() { return 100; }
    }

    /** 지정한 한 타일만 고체(+범위 밖 고체). */
    private static final class SingleWallGrid implements SolidGrid {
        private final int wx;
        private final int wy;
        SingleWallGrid(int wx, int wy) { this.wx = wx; this.wy = wy; }
        @Override public boolean isSolid(int tileX, int tileY) {
            if (tileX < 0 || tileY < 0 || tileX >= 100 || tileY >= 100) return true;
            return tileX == wx && tileY == wy;
        }
        @Override public int widthInTiles() { return 100; }
        @Override public int heightInTiles() { return 100; }
    }
}

package io.github.some_example_name.movement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** 달리기 시작 판정: WALKING -> RUNNING 전환 순간에만 1회로 센다. */
public class RunStartDetectorTest {

    @Test
    public void startsWalkingAndNoRunWhenNotRequested() {
        RunStartDetector d = new RunStartDetector();
        assertFalse(d.update(false));
        assertEquals(MovementMode.WALKING, d.mode());
        assertEquals(0, d.runStartCount());
    }

    @Test
    public void countsRunStartOnTransition() {
        RunStartDetector d = new RunStartDetector();
        assertTrue("걷기에서 달리기로 전환되는 프레임은 시작으로 센다", d.update(true));
        assertEquals(MovementMode.RUNNING, d.mode());
        assertEquals(1, d.runStartCount());
    }

    @Test
    public void holdingRunDoesNotRecount() {
        RunStartDetector d = new RunStartDetector();
        d.update(true);           // 시작 1회
        assertFalse(d.update(true)); // 계속 누르는 중 -> 추가 없음
        assertFalse(d.update(true));
        assertEquals(1, d.runStartCount());
    }

    @Test
    public void stopThenRunAgainCountsSecondStart() {
        RunStartDetector d = new RunStartDetector();
        assertTrue(d.update(true));  // 1
        d.update(false);             // 달리기 종료 -> WALKING
        assertTrue(d.update(true));  // 2
        assertEquals(2, d.runStartCount());
    }
}

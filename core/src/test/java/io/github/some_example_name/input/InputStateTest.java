package io.github.some_example_name.input;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.EnumSet;

import org.junit.Test;

/** 입력 엣지(pressed/released) 계산 검증. */
public class InputStateTest {

    @Test
    public void pressedOnlyOnFirstFrameThenHeld() {
        InputState s = new InputState();
        s.update(EnumSet.of(GameAction.MOVE_UP));
        assertTrue(s.isPressed(GameAction.MOVE_UP));
        assertTrue(s.isHeld(GameAction.MOVE_UP));
        assertFalse(s.isReleased(GameAction.MOVE_UP));

        s.update(EnumSet.of(GameAction.MOVE_UP));
        assertFalse("계속 눌림은 pressed가 아니다", s.isPressed(GameAction.MOVE_UP));
        assertTrue(s.isHeld(GameAction.MOVE_UP));
    }

    @Test
    public void releasedOnFrameKeyLetGo() {
        InputState s = new InputState();
        s.update(EnumSet.of(GameAction.ATTACK));
        s.update(EnumSet.noneOf(GameAction.class));
        assertTrue(s.isReleased(GameAction.ATTACK));
        assertFalse(s.isHeld(GameAction.ATTACK));
        assertFalse(s.isPressed(GameAction.ATTACK));
    }
}

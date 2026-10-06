package io.github.some_example_name.input;

import java.util.EnumSet;
import java.util.Set;

/**
 * 프레임별 입력 상태. 순수 Java로, LibGDX 없이 테스트할 수 있다.
 * - held: 계속 눌림
 * - pressed: 이번 프레임에 새로 눌림(엣지)
 * - released: 이번 프레임에 떼어짐(엣지)
 *
 * 공격/상호작용/달리기 시작 감지는 pressed(또는 상태 전환)를, 이동은 held를 사용한다.
 */
public class InputState {

    private EnumSet<GameAction> held = EnumSet.noneOf(GameAction.class);
    private final EnumSet<GameAction> pressed = EnumSet.noneOf(GameAction.class);
    private final EnumSet<GameAction> released = EnumSet.noneOf(GameAction.class);

    /** 이번 프레임에 눌려 있는 액션 집합을 받아 엣지(pressed/released)를 계산한다. */
    public void update(Set<GameAction> nowHeld) {
        pressed.clear();
        released.clear();
        for (GameAction a : nowHeld) {
            if (!held.contains(a)) {
                pressed.add(a);
            }
        }
        for (GameAction a : held) {
            if (!nowHeld.contains(a)) {
                released.add(a);
            }
        }
        EnumSet<GameAction> next = EnumSet.noneOf(GameAction.class);
        next.addAll(nowHeld);
        held = next;
    }

    public boolean isHeld(GameAction a) {
        return held.contains(a);
    }

    public boolean isPressed(GameAction a) {
        return pressed.contains(a);
    }

    public boolean isReleased(GameAction a) {
        return released.contains(a);
    }
}

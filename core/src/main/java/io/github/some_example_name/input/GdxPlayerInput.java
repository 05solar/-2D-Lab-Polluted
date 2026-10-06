package io.github.some_example_name.input;

import com.badlogic.gdx.Gdx;

import java.util.EnumSet;

/**
 * 실제 키보드(Gdx.input)를 읽어 InputState를 갱신한다. LibGDX에 의존하므로 단위 테스트 대상이 아니다.
 * 입력 수집만 담당하고 게임 규칙은 건드리지 않는다.
 */
public class GdxPlayerInput {

    private final KeyBindings bindings;

    public GdxPlayerInput(KeyBindings bindings) {
        this.bindings = bindings;
    }

    /** 이번 프레임에 눌린 액션을 수집해 InputState 엣지를 갱신한다. */
    public void poll(InputState state) {
        EnumSet<GameAction> nowHeld = EnumSet.noneOf(GameAction.class);
        for (GameAction action : GameAction.values()) {
            for (int key : bindings.keysFor(action)) {
                if (Gdx.input.isKeyPressed(key)) {
                    nowHeld.add(action);
                    break;
                }
            }
        }
        state.update(nowHeld);
    }
}

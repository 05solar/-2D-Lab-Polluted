package io.github.some_example_name.input;

import com.badlogic.gdx.Input.Keys;

import java.util.EnumMap;
import java.util.Map;

/**
 * 실제 키 코드와 GameAction을 연결한다. 키 매핑을 로직에 직접 박아 넣지 않기 위한 유일한 장소.
 * Input.Keys 상수는 정수값일 뿐이라 LibGDX 애플리케이션 없이도 구성할 수 있다.
 */
public class KeyBindings {

    private final Map<GameAction, int[]> keysByAction = new EnumMap<>(GameAction.class);

    /**
     * 기본 키맵(임시): 방향키/WASD 이동, Shift 달리기, Space 공격, F 돌진,
     * E 상호작용, H 귀환 경로 토글(이전 F — 돌진과 분리), Esc 일시정지.
     */
    public static KeyBindings defaults() {
        KeyBindings b = new KeyBindings();
        b.bind(GameAction.MOVE_UP, Keys.W, Keys.UP);
        b.bind(GameAction.MOVE_DOWN, Keys.S, Keys.DOWN);
        b.bind(GameAction.MOVE_LEFT, Keys.A, Keys.LEFT);
        b.bind(GameAction.MOVE_RIGHT, Keys.D, Keys.RIGHT);
        b.bind(GameAction.RUN, Keys.SHIFT_LEFT, Keys.SHIFT_RIGHT);
        b.bind(GameAction.ATTACK, Keys.SPACE);
        b.bind(GameAction.DASH, Keys.F);
        b.bind(GameAction.INTERACT, Keys.E);
        b.bind(GameAction.RETURN_ROUTE, Keys.H);
        b.bind(GameAction.PAUSE, Keys.ESCAPE);
        return b;
    }

    /** HUD 라벨용: 액션의 첫 번째 키 이름(예: ATTACK → "Space"). 바인딩이 없으면 빈 문자열. */
    public String primaryKeyName(GameAction action) {
        int[] keys = keysFor(action);
        return keys.length == 0 ? "" : com.badlogic.gdx.Input.Keys.toString(keys[0]);
    }

    public void bind(GameAction action, int... keys) {
        keysByAction.put(action, keys);
    }

    public int[] keysFor(GameAction action) {
        int[] keys = keysByAction.get(action);
        return keys != null ? keys : new int[0];
    }
}

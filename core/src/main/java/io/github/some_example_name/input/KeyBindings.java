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
     * 기본 키맵(2026-10-16 정리):
     * 이동 = WASD/방향키 · 달리기 = Shift · 공격 = 마우스 클릭 전용(키 없음) ·
     * 돌진 = F · 가방 = R(화면에서 직접 처리) · 상호작용(문/상자/수집) = Space ·
     * 귀환 경로 = 미니맵 아래 버튼 클릭 토글(단축키 없음) · 일시정지 = Esc.
     */
    public static KeyBindings defaults() {
        KeyBindings b = new KeyBindings();
        b.bind(GameAction.MOVE_UP, Keys.W, Keys.UP);
        b.bind(GameAction.MOVE_DOWN, Keys.S, Keys.DOWN);
        b.bind(GameAction.MOVE_LEFT, Keys.A, Keys.LEFT);
        b.bind(GameAction.MOVE_RIGHT, Keys.D, Keys.RIGHT);
        b.bind(GameAction.RUN, Keys.SHIFT_LEFT, Keys.SHIFT_RIGHT);
        // 공격은 마우스 왼쪽 버튼 전용(GdxPlayerInput이 매핑) — 키보드 바인딩 없음.
        b.bind(GameAction.DASH, Keys.F);
        b.bind(GameAction.INTERACT, Keys.SPACE);
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

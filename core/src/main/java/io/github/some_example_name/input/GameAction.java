package io.github.some_example_name.input;

/** 게임 내 추상 입력. 실제 키 매핑은 KeyBindings가 담당한다. */
public enum GameAction {
    MOVE_UP,
    MOVE_DOWN,
    MOVE_LEFT,
    MOVE_RIGHT,
    RUN,
    ATTACK,
    INTERACT,
    PAUSE
}

package io.github.some_example_name.world;

/** 문이 완전히 열렸을 때만 통과 가능하다. 중간 애니메이션은 후속 단계에서 연결한다. */
public enum DoorState {
    CLOSED, OPENING, OPEN, CLOSING
}

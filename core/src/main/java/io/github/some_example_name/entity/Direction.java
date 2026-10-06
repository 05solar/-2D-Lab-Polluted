package io.github.some_example_name.entity;

/** 엔티티가 바라보는 4방향. 애니메이션/공격 방향 등에 쓰인다. */
public enum Direction {
    DOWN, LEFT, RIGHT, UP;

    /**
     * 입력 벡터에서 지배적인 방향을 고른다. 가로 성분이 세로 성분보다 크면 좌/우,
     * 그렇지 않으면 상/하를 선택한다. 입력이 0이면 null을 반환한다(방향 유지는 호출 측 책임).
     */
    public static Direction fromInput(float moveX, float moveY) {
        if (moveX == 0f && moveY == 0f) {
            return null;
        }
        if (Math.abs(moveX) > Math.abs(moveY)) {
            return moveX > 0f ? RIGHT : LEFT;
        }
        return moveY > 0f ? UP : DOWN;
    }
}

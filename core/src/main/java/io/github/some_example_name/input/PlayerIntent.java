package io.github.some_example_name.input;

/**
 * 한 프레임 동안 플레이어가 의도한 행동. 입력 장치와 무관한 순수 값이라 테스트하기 쉽다.
 * moveX/moveY는 -1, 0, 1 중 하나(아직 정규화 전).
 */
public final class PlayerIntent {

    public final float moveX;
    public final float moveY;
    public final boolean run;
    public final boolean attack;
    public final boolean interact;

    public PlayerIntent(float moveX, float moveY, boolean run, boolean attack, boolean interact) {
        this.moveX = moveX;
        this.moveY = moveY;
        this.run = run;
        this.attack = attack;
        this.interact = interact;
    }

    public boolean hasMovement() {
        return moveX != 0f || moveY != 0f;
    }
}

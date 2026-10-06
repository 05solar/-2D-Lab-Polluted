package io.github.some_example_name.entity.player;

import com.badlogic.gdx.math.Rectangle;
import io.github.some_example_name.entity.Direction;
import io.github.some_example_name.movement.MovementMode;

/**
 * 플레이어 도메인 엔티티. 자신의 핵심 상태(위치/HP/바라보는 방향/이동 상태)를 관리한다.
 * Texture/SpriteBatch 같은 렌더링 자원을 소유하지 않는다. (렌더러가 이 상태를 읽어 그린다)
 *
 * 위치는 충돌 박스(bounds)의 좌하단 기준(월드 단위). 발밑 앵커는 박스 하단 중앙이다.
 */
public class Player {

    private final Rectangle bounds;
    private final int maxHp;
    private int hp;

    private Direction facing = Direction.DOWN;
    private MovementMode mode = MovementMode.WALKING;
    private boolean moving = false;

    public Player(float feetX, float feetY, float width, float height, int maxHp) {
        this.bounds = new Rectangle(feetX - width / 2f, feetY, width, height);
        this.maxHp = maxHp;
        this.hp = maxHp;
    }

    public Rectangle bounds() {
        return bounds;
    }

    public float feetX() {
        return bounds.x + bounds.width / 2f;
    }

    public float feetY() {
        return bounds.y;
    }

    public int hp() {
        return hp;
    }

    public int maxHp() {
        return maxHp;
    }

    public Direction facing() {
        return facing;
    }

    public void setFacing(Direction facing) {
        this.facing = facing;
    }

    public MovementMode mode() {
        return mode;
    }

    public void setMode(MovementMode mode) {
        this.mode = mode;
    }

    public boolean isMoving() {
        return moving;
    }

    public void setMoving(boolean moving) {
        this.moving = moving;
    }
}

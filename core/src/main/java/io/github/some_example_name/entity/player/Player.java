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
    private int maxHp; // 영구 강화로 변할 수 있다(setMaxHp) — 항상 0 <= hp <= maxHp 유지
    private int hp;

    private Direction facing = Direction.DOWN;
    private MovementMode mode = MovementMode.WALKING;
    private boolean moving = false;
    private PlayerState state = PlayerState.ALIVE;

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

    /**
     * 영구 강화 반영: 최대 HP를 새 값으로 바꾼다. 현재 HP 숫자는 유지하되 새 최대를
     * 넘으면 잘라낸다 — 체력 강화 구매가 공짜 완전 회복이 되지 않는다.
     */
    public void setMaxHp(int newMaxHp) {
        if (newMaxHp <= 0) throw new IllegalArgumentException("maxHp");
        maxHp = newMaxHp;
        hp = Math.min(hp, maxHp);
    }

    /** Restores a living player to maximum health without reviving a dead player. */
    public void healToFullHealth() {
        if (state != PlayerState.DEAD) hp = maxHp;
    }

    /**
     * 처치 보상 등 부분 회복. 최대 HP를 넘지 않으며, 이미 죽은 플레이어는
     * 늦게 도착한 회복 이벤트로도 되살아나지 않는다(무시).
     */
    public void heal(int amount) {
        if (amount < 0) throw new IllegalArgumentException("heal");
        if (state == PlayerState.DEAD) return;
        hp = Math.min(maxHp, hp + amount);
    }

    public void takeDamage(int amount) {
        if (amount < 0) throw new IllegalArgumentException("damage");
        if (state == PlayerState.DEAD) return;
        hp = Math.max(0, hp - amount);
        if (hp == 0) {
            state = PlayerState.DEAD;
            moving = false;
            mode = MovementMode.WALKING;
        }
    }

    /** 본부 재소환용 부활: 사망 상태를 풀고 HP를 최대로 되돌린다(소지품 처리는 세션이 담당). */
    public void revive() {
        state = PlayerState.ALIVE;
        hp = maxHp;
    }

    public PlayerState state() { return state; }
    public boolean isDead() { return state == PlayerState.DEAD; }

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

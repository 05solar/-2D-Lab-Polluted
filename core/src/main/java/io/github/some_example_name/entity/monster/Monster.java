package io.github.some_example_name.entity.monster;

import com.badlogic.gdx.math.Rectangle;
import io.github.some_example_name.combat.HealthComponent;
import io.github.some_example_name.combat.MonsterState;
import io.github.some_example_name.combat.MonsterType;
import io.github.some_example_name.entity.Direction;

/** Domain state and collision body for one laboratory monster. */
public final class Monster {
    private final long id;
    private final MonsterType type;
    private final Rectangle bounds;
    private final HealthComponent health;
    private MonsterState state = MonsterState.IDLE;
    private Direction facing = Direction.DOWN;
    private Direction wanderDirection = Direction.DOWN;
    private float stateTime;
    private float wanderTime;
    private float attackCooldown;
    private float hitTime;
    private float knockbackX, knockbackY, knockbackTime;
    private boolean enraged;
    private boolean aggro;
    private int nearbyRunStarts;
    private boolean attackHit;

    public Monster(long id, MonsterType type, float feetX, float feetY, float width, float height, int maxHp) {
        if (id < 0 || type == null || width <= 0f || height <= 0f) throw new IllegalArgumentException();
        this.id = id;
        this.type = type;
        bounds = new Rectangle(feetX - width / 2f, feetY, width, height);
        health = new HealthComponent(maxHp);
    }

    public long id() { return id; }
    public MonsterType type() { return type; }
    public Rectangle bounds() { return bounds; }
    public float feetX() { return bounds.x + bounds.width / 2f; }
    public float feetY() { return bounds.y; }
    public HealthComponent health() { return health; }
    public MonsterState state() { return state; }
    public void setState(MonsterState state) { this.state = state; }
    public Direction facing() { return facing; }
    public void setFacing(Direction facing) { if (facing != null) this.facing = facing; }
    public Direction wanderDirection() { return wanderDirection; }
    public void setWanderDirection(Direction direction) { if (direction != null) wanderDirection = direction; }
    public float stateTime() { return stateTime; }
    public void setStateTime(float time) { stateTime = time; }
    public void advanceStateTime(float delta) { stateTime += delta; }
    public float wanderTime() { return wanderTime; }
    public void setWanderTime(float time) { wanderTime = time; }
    public void advanceWanderTime(float delta) { wanderTime += delta; }
    public float attackCooldown() { return attackCooldown; }
    public void setAttackCooldown(float value) { attackCooldown = Math.max(0f, value); }
    public void reduceAttackCooldown(float delta) { attackCooldown = Math.max(0f, attackCooldown - delta); }
    public boolean attackHit() { return attackHit; }
    public void setAttackHit(boolean hit) { attackHit = hit; }
    public float hitTime() { return hitTime; }
    public void setHitTime(float value) { hitTime = Math.max(0f, value); }
    public void reduceHitTime(float delta) { hitTime = Math.max(0f, hitTime - delta); }
    public boolean isAlive() { return health.isAlive(); }
    public boolean enraged() { return enraged; }
    public void enrage() { enraged = true; }
    public boolean aggro() { return aggro; }
    public void setAggro(boolean value) { aggro = value; }
    public int nearbyRunStarts() { return nearbyRunStarts; }
    public int countRunStart() { return ++nearbyRunStarts; }
    public float knockbackTime() { return knockbackTime; }
    public float knockbackX() { return knockbackX; }
    public float knockbackY() { return knockbackY; }
    public void setKnockback(float x, float y, float seconds) {
        knockbackX = x;
        knockbackY = y;
        knockbackTime = Math.max(0f, seconds);
    }
    public void reduceKnockbackTime(float delta) { knockbackTime = Math.max(0f, knockbackTime - delta); }
}

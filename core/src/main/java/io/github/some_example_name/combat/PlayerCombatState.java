package io.github.some_example_name.combat;

import io.github.some_example_name.entity.Direction;
import io.github.some_example_name.entity.player.Player;

/** Player attack state and damage invulnerability timer. */
public final class PlayerCombatState {
    private final PlayerAttackState attack;
    private float invulnerabilityTime;
    private float knockbackX, knockbackY;

    public PlayerCombatState(PlayerAttackState attack) { this.attack = attack; }
    public PlayerAttackState attack() { return attack; }
    public float invulnerabilityTime() { return invulnerabilityTime; }
    public boolean invulnerable() { return invulnerabilityTime > 0f; }
    public boolean takeDamage(Player player, int damage, float knockbackX, float knockbackY,
                              float invulnerabilitySeconds) {
        if (invulnerable() || player.hp() <= 0) return false;
        player.takeDamage(damage);
        if (player.isDead()) attack.cancel();
        invulnerabilityTime = invulnerabilitySeconds;
        this.knockbackX = knockbackX;
        this.knockbackY = knockbackY;
        return true;
    }
    public void update(float delta) {
        invulnerabilityTime = Math.max(0f, invulnerabilityTime - delta);
    }
    public float knockbackX() { return knockbackX; }
    public float knockbackY() { return knockbackY; }
    public static Direction directionToward(float fromX, float fromY, float toX, float toY) {
        return Direction.fromInput(toX - fromX, toY - fromY);
    }
}

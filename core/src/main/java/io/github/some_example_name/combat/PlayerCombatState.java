package io.github.some_example_name.combat;

import io.github.some_example_name.entity.Direction;
import io.github.some_example_name.entity.player.Player;

/** Player attack state and damage invulnerability timer. */
public final class PlayerCombatState {
    private final PlayerAttackState attack;
    private float invulnerabilityTime;
    private float knockbackX, knockbackY;
    // 보호막(선택 연결): 공통 피해 파이프라인 한 곳에서만 흡수한다.
    private PlayerShieldState shield;

    public PlayerCombatState(PlayerAttackState attack) { this.attack = attack; }
    public PlayerAttackState attack() { return attack; }
    public void setShield(PlayerShieldState shield) { this.shield = shield; }
    public float invulnerabilityTime() { return invulnerabilityTime; }
    public boolean invulnerable() { return invulnerabilityTime > 0f; }

    /**
     * 공통 피해 파이프라인: 유효 피해/무적 검증 → 보호막 흡수 → 남은 피해만 HP 차감 →
     * 피격 무적·넉백. 무적 등으로 거절된 공격은 보호막도 깎지 않는다(흡수 호출 전에 반환).
     * 보호막이 전부 흡수해도 "수용된 피격"이라 무적시간은 기존 규칙대로 시작된다 —
     * 겹친 적이 프레임마다 보호막을 깎지 못한다.
     */
    public boolean takeDamage(Player player, int damage, float knockbackX, float knockbackY,
                              float invulnerabilitySeconds) {
        if (invulnerable() || player.hp() <= 0) return false;
        int remaining = shield != null ? shield.absorb(damage) : damage;
        player.takeDamage(remaining);
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

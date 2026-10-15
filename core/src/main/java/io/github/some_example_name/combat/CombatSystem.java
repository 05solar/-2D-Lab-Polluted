package io.github.some_example_name.combat;

import com.badlogic.gdx.math.Rectangle;
import io.github.some_example_name.config.BalanceConfig;
import io.github.some_example_name.entity.Direction;
import io.github.some_example_name.entity.monster.Monster;
import io.github.some_example_name.entity.player.Player;
import io.github.some_example_name.render.AnimationSet;
import io.github.some_example_name.world.LaboratoryRoom;
import java.util.List;

/** Resolves one hit per target during the player animation's declared hit frames. */
public final class CombatSystem {

    /** 몬스터에 실제 피해가 적용된 순간의 알림(타격 효과음 등 — 허공 공격에는 호출되지 않는다). */
    public interface StrikeListener { void onMonsterStruck(Monster monster, boolean killed); }

    private final BalanceConfig config;
    private final Rectangle hitbox = new Rectangle();
    private StrikeListener strikeListener;

    public CombatSystem(BalanceConfig config) { this.config = config; }

    public void setStrikeListener(StrikeListener listener) { strikeListener = listener; }

    public void updatePlayerAttack(Player player, PlayerCombatState combat, AnimationSet animations,
                                   List<Monster> monsters, LaboratoryRoom room, float delta) {
        PlayerAttackState attack = combat.attack();
        if (player.isDead()) {
            attack.cancel();
            return;
        }
        attack.update(delta);
        if (!attack.isAttacking() || attack.phase() != PlayerAttackState.Phase.ACTIVE
            || !hasHitFrame(animations.hitFrames(attack.direction()), attack.frameIndex())) return;
        AttackHitbox.set(player.bounds(), attack.direction(), config.playerAttackRange,
            config.playerAttackWidth, hitbox);
        for (Monster monster : monsters) {
            if (!monster.isAlive() || attack.hasHit(monster.id())
                || !hitbox.overlaps(monster.bounds())
                || !clearPath(player, monster, attack.direction(), room)) continue;
            attack.markHit(monster.id());
            applyPlayerStrike(player, monster,
                directionX(attack.direction()), directionY(attack.direction()));
        }
    }

    /**
     * 일반 공격·돌진 공용 타격 경로: 피해 15 → 넉백 → HIT/DEAD 상태 → 처치 시 HP 회복.
     * 살아 있음→사망 전환 이벤트에서만 killHealHp를 1회 회복하며(HealthComponent.damage가
     * 전환 프레임에만 true), 최대 HP를 넘지 않고 죽은 플레이어는 회복하지 않는다.
     *
     * @return 이 타격으로 몬스터가 죽었으면 true
     */
    public boolean applyPlayerStrike(Player player, Monster monster, float dirX, float dirY) {
        boolean dead = monster.health().damage(config.playerAttackDamage);
        monster.setHitTime(config.knockbackSeconds);
        monster.setKnockback(dirX * config.monsterKnockbackDistance,
            dirY * config.monsterKnockbackDistance, config.knockbackSeconds);
        monster.setState(dead ? MonsterState.DEAD : MonsterState.HIT);
        monster.setStateTime(0f);
        if (dead) player.heal(config.killHealHp);
        if (strikeListener != null) strikeListener.onMonsterStruck(monster, dead);
        return dead;
    }

    public static boolean hasHitFrame(int[] frames, int frame) {
        for (int value : frames) if (value == frame) return true;
        return false;
    }

    private static boolean clearPath(Player player, Monster monster, Direction direction,
                                    LaboratoryRoom room) {
        float sx = player.feetX(), sy = player.feetY() + player.bounds().height / 2f;
        float tx = monster.feetX(), ty = monster.feetY() + monster.bounds().height / 2f;
        return !room.blocksActorAlongPath(sx,sy,tx,ty,.05f,.05f);
    }

    private static float directionX(Direction d) {
        return d == Direction.LEFT ? -1f : d == Direction.RIGHT ? 1f : 0f;
    }
    private static float directionY(Direction d) {
        return d == Direction.DOWN ? -1f : d == Direction.UP ? 1f : 0f;
    }
}

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
    private final BalanceConfig config;
    private final Rectangle hitbox = new Rectangle();

    public CombatSystem(BalanceConfig config) { this.config = config; }

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
            boolean dead = monster.health().damage(config.playerAttackDamage);
            monster.setHitTime(config.knockbackSeconds);
            float dx = directionX(attack.direction()) * config.monsterKnockbackDistance;
            float dy = directionY(attack.direction()) * config.monsterKnockbackDistance;
            monster.setKnockback(dx, dy, config.knockbackSeconds);
            monster.setState(dead ? MonsterState.DEAD : MonsterState.HIT);
            monster.setStateTime(0f);
        }
    }

    public static boolean hasHitFrame(int[] frames, int frame) {
        for (int value : frames) if (value == frame) return true;
        return false;
    }

    private static boolean clearPath(Player player, Monster monster, Direction direction,
                                    LaboratoryRoom room) {
        float sx = player.feetX(), sy = player.feetY() + player.bounds().height / 2f;
        float tx = monster.feetX(), ty = monster.feetY() + monster.bounds().height / 2f;
        float dx = tx - sx, dy = ty - sy;
        int steps = Math.max(1, (int) (Math.max(Math.abs(dx), Math.abs(dy)) / 0.12f));
        for (int i = 1; i < steps; i++) {
            float x = sx + dx * i / steps, y = sy + dy * i / steps;
            if (room.isSolid((int) Math.floor(x), (int) Math.floor(y))) return false;
        }
        return true;
    }

    private static float directionX(Direction d) {
        return d == Direction.LEFT ? -1f : d == Direction.RIGHT ? 1f : 0f;
    }
    private static float directionY(Direction d) {
        return d == Direction.DOWN ? -1f : d == Direction.UP ? 1f : 0f;
    }
}

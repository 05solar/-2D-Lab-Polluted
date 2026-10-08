package io.github.some_example_name.render;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.github.some_example_name.combat.MonsterState;
import io.github.some_example_name.combat.MonsterType;
import io.github.some_example_name.entity.Direction;
import io.github.some_example_name.entity.monster.Monster;
import java.util.List;
import java.util.Map;

/** Draws monster clips from the supplied animation JSON without owning textures. */
public final class MonsterRenderer {
    private final Map<MonsterType, MonsterAnimationSet> animations;
    public MonsterRenderer(Map<MonsterType, MonsterAnimationSet> animations) { this.animations = animations; }

    public void render(SpriteBatch batch, List<Monster> monsters) {
        for (Monster monster : monsters) {
            if (!monster.isAlive()) continue;
            MonsterAnimationSet set = animations.get(monster.type());
            String clip = clip(monster, set);
            TextureRegion frame = set.frame(clip, monster.stateTime());
            if (frame == null) continue;
            boolean hurt = monster.state() == MonsterState.HIT;
            if (hurt) batch.setColor(1f, 0.45f, 0.45f, 1f);
            else if (monster.type() == MonsterType.TEAM_LEADER && monster.enraged())
                batch.setColor(1f, 0.72f, 0.72f, 1f);
            batch.draw(frame, monster.feetX() - 0.5f, monster.feetY(), 1f, 1f);
            batch.setColor(1f, 1f, 1f, 1f);
        }
    }

    private static String clip(Monster monster, MonsterAnimationSet set) {
        if (monster.type() == MonsterType.TEAM_LEADER) {
            if (monster.state() == MonsterState.IDLE && !monster.enraged()) return "seated_idle";
            if (monster.state() == MonsterState.ENRAGED) return "enrage";
            if (monster.state() == MonsterState.ATTACK) return "radial_slam";
            if (monster.state() == MonsterState.HIT && set.has("hit_stagger")) return "hit_stagger";
            return "chase_" + key(monster.facing());
        }
        if (monster.state() == MonsterState.ATTACK) return "attack_" + key(monster.facing());
        return "move_" + key(monster.facing());
    }
    private static String key(Direction direction) { return direction.name().toLowerCase(); }
}

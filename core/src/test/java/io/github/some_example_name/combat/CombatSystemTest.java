package io.github.some_example_name.combat;

import static org.junit.Assert.*;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import io.github.some_example_name.config.BalanceConfig;
import io.github.some_example_name.entity.Direction;
import io.github.some_example_name.entity.monster.Monster;
import io.github.some_example_name.entity.player.Player;
import io.github.some_example_name.render.AnimationSet;
import io.github.some_example_name.render.MonsterAnimationSet;
import io.github.some_example_name.world.Hazard;
import io.github.some_example_name.world.LaboratoryLayout;
import io.github.some_example_name.world.LaboratoryRoom;
import io.github.some_example_name.world.TileType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class CombatSystemTest {
    @Test public void configuredHealthAndDamageMatchFirstCombatValues() {
        BalanceConfig c = new BalanceConfig();
        assertEquals(100, c.playerMaxHp);
        assertEquals(15, c.playerAttackDamage);
        int[] hp = {c.slimeMaxHp, c.researcherMaxHp, c.guardMaxHp, c.teamLeaderMaxHp};
        int[] expectedHits = {2, 3, 4, 5};
        for (int i = 0; i < hp.length; i++) {
            HealthComponent health = new HealthComponent(hp[i]);
            for (int hit = 0; hit < expectedHits[i]; hit++) health.damage(c.playerAttackDamage);
            assertEquals(0, health.hp());
            health.damage(c.playerAttackDamage);
            assertEquals(0, health.hp());
        }
        assertEquals(10, c.slimeAttackDamage);
        assertEquals(15, c.researcherAttackDamage);
        assertEquals(20, c.guardAttackDamage);
        assertEquals(35, c.teamLeaderAttackDamage);
    }

    @Test public void playerAttackUsesHitFramesDirectionAndHitsEachTargetOnce() {
        BalanceConfig c = new BalanceConfig();
        Player player = new Player(2f, 2f, 0.6f, 0.5f, c.playerMaxHp);
        Monster first = new Monster(1, MonsterType.SLIME, 2.95f, 2f, 0.6f, 0.5f, 20);
        Monster second = new Monster(2, MonsterType.RESEARCHER, 2.9f, 2.05f, 0.6f, 0.5f, 40);
        List<Monster> targets = Arrays.asList(first, second);
        PlayerAttackState state = new PlayerAttackState(0.45f, 8, new int[]{2, 3, 4});
        assertTrue(state.start(Direction.RIGHT));
        assertEquals(PlayerAttackState.Phase.WINDUP, state.phase());
        AnimationSet animations = new AnimationSet();
        animations.putHitFrames("attack_right", new int[]{2, 3, 4});
        LaboratoryRoom room = openRoom(7, 7);
        CombatSystem combat = new CombatSystem(c);
        combat.updatePlayerAttack(player, new PlayerCombatState(state), animations, targets, room, 0.18f);
        assertEquals(PlayerAttackState.Phase.ACTIVE, state.phase());
        assertEquals(5, first.health().hp());
        assertEquals(25, second.health().hp());
        combat.updatePlayerAttack(player, new PlayerCombatState(state), animations, targets, room, 0.02f);
        assertEquals(5, first.health().hp());
        assertEquals(25, second.health().hp());
        assertFalse(state.start(Direction.LEFT));
    }

    @Test public void deadPlayerAttackIsCancelledBeforeAnyHitFrameCanDamageMonsters() {
        BalanceConfig c = new BalanceConfig();
        Player player = new Player(2f, 2f, 0.6f, 0.5f, 1);
        Monster target = new Monster(1, MonsterType.SLIME, 2.95f, 2f, 0.6f, 0.5f, 20);
        PlayerAttackState attack = new PlayerAttackState(0.45f, 8, new int[]{2, 3, 4});
        assertTrue(attack.start(Direction.RIGHT));
        attack.update(0.18f);
        player.takeDamage(1);
        AnimationSet animations = new AnimationSet();
        animations.putHitFrames("attack_right", new int[]{2, 3, 4});
        new CombatSystem(c).updatePlayerAttack(player, new PlayerCombatState(attack),
            animations, Collections.singletonList(target), openRoom(7, 7), 0.02f);
        assertEquals(20, target.health().hp());
        assertFalse(attack.isAttacking());
        assertTrue(attack.alreadyHitEntityIds().isEmpty());
    }

    @Test public void attackHitboxMatchesAllFourFacingDirections() {
        Rectangle body = new Rectangle(3f, 4f, 0.6f, 0.5f);
        Rectangle hit = new Rectangle();
        AttackHitbox.set(body, Direction.UP, 1f, 0.8f, hit);
        assertEquals(4.5f, hit.y, 0.001f);
        AttackHitbox.set(body, Direction.DOWN, 1f, 0.8f, hit);
        assertEquals(3f, hit.y, 0.001f);
        AttackHitbox.set(body, Direction.LEFT, 1f, 0.8f, hit);
        assertEquals(2f, hit.x, 0.001f);
        AttackHitbox.set(body, Direction.RIGHT, 1f, 0.8f, hit);
        assertEquals(3.6f, hit.x, 0.001f);
    }

    @Test public void spawnsAreSeededSafeSeparatedAndMatchConfiguredCounts() {
        BalanceConfig c = new BalanceConfig();
        LaboratoryRoom room = LaboratoryLayout.testRoom().room();
        MonsterSpawnSystem spawns = new MonsterSpawnSystem(c);
        List<Monster> a = spawns.spawn(room, 12345L);
        List<Monster> b = spawns.spawn(room, 12345L);
        assertEquals(7, a.size());
        assertEquals(positions(a), positions(b));
        assertEquals(3, count(a, MonsterType.SLIME));
        assertEquals(2, count(a, MonsterType.RESEARCHER));
        assertEquals(1, count(a, MonsterType.GUARD));
        assertEquals(1, count(a, MonsterType.TEAM_LEADER));
        Vector2 spawn = room.spawnPoint();
        for (int i = 0; i < a.size(); i++) {
            Monster m = a.get(i);
            assertFalse(room.isSolid((int) m.feetX(), (int) m.feetY()));
            assertEquals(Hazard.NONE, room.hazardAt((int) m.feetX(), (int) m.feetY()));
            assertTrue(m.bounds().x >= 0f && m.bounds().y >= 0f);
            float dx = m.feetX() - spawn.x, dy = m.feetY() - spawn.y;
            assertTrue(dx * dx + dy * dy >= c.monsterSpawnExclusionRadius * c.monsterSpawnExclusionRadius);
            for (int j = 0; j < i; j++) assertFalse(m.bounds().overlaps(a.get(j).bounds()));
        }
    }

    @Test public void teamLeaderCountsOnlyNearbyRunStartsAndEnragesAtTwo() {
        BalanceConfig c = new BalanceConfig();
        Monster boss = new Monster(1, MonsterType.TEAM_LEADER, 5f, 5f, 0.6f, 0.5f, c.teamLeaderMaxHp);
        MonsterAiSystem ai = new MonsterAiSystem(c, openRoom(12, 12), 1L);
        List<Monster> monsters = Collections.singletonList(boss);
        ai.registerNoise(monsters, 10f, 10f);
        assertEquals(0, boss.nearbyRunStarts());
        ai.registerNoise(monsters, 7f, 5f);
        assertEquals(1, boss.nearbyRunStarts());
        assertFalse(boss.enraged());
        ai.registerNoise(monsters, 6f, 5f);
        assertEquals(2, boss.nearbyRunStarts());
        assertTrue(boss.enraged());
        assertEquals(MonsterState.ENRAGED, boss.state());
    }

    @Test public void deadPlayerRunStartCannotTriggerTeamLeaderNoise() {
        BalanceConfig c = new BalanceConfig();
        Player player = new Player(5f, 5f, 0.6f, 0.5f, 1);
        player.takeDamage(1);
        Monster boss = new Monster(1, MonsterType.TEAM_LEADER, 5f, 5f, 0.6f, 0.5f, 70);
        MonsterAiSystem ai = new MonsterAiSystem(c, openRoom(12, 12), 1L);
        ai.update(Collections.singletonList(boss), player,
            new PlayerCombatState(new PlayerAttackState(0.45f, 8, new int[]{2, 3, 4})),
            openRoom(12, 12), Collections.<MonsterType, MonsterAnimationSet>emptyMap(), true, 0.1f);
        assertEquals(0, boss.nearbyRunStarts());
        assertFalse(boss.enraged());
    }

    @Test public void monsterTelegraphDealsDamageOnlyOnDeclaredHitFrame() {
        BalanceConfig c = new BalanceConfig();
        LaboratoryRoom room = openRoom(8, 8);
        Player player = new Player(3.5f, 3f, 0.6f, 0.5f, c.playerMaxHp);
        PlayerCombatState playerCombat = new PlayerCombatState(
            new PlayerAttackState(0.45f, 8, new int[]{2, 3, 4}));
        Monster slime = new Monster(1, MonsterType.SLIME, 3.5f, 3.65f,
            c.monsterBoundsWidth, c.monsterBoundsHeight, c.slimeMaxHp);
        List<Monster> monsters = new ArrayList<>();
        monsters.add(slime);
        Map<MonsterType, MonsterAnimationSet> animations = new EnumMap<>(MonsterType.class);
        animations.put(MonsterType.SLIME, animationSet());
        MonsterAiSystem ai = new MonsterAiSystem(c, room, 2L);
        ai.update(monsters, player, playerCombat, room, animations, false, 0.01f);
        assertEquals(MonsterState.ATTACK, slime.state());
        ai.update(monsters, player, playerCombat, room, animations, false, 0.1f);
        assertEquals(100, player.hp());
        ai.update(monsters, player, playerCombat, room, animations, false, 0.1f);
        assertEquals(90, player.hp());
        ai.update(monsters, player, playerCombat, room, animations, false, 0.1f);
        assertEquals(90, player.hp());
    }

    private static MonsterAnimationSet animationSet() {
        MonsterAnimationSet set = new MonsterAnimationSet();
        Array<TextureRegion> frames = new Array<>();
        for (int i = 0; i < 8; i++) frames.add(new TextureRegion());
        for (String dir : new String[]{"down", "up", "left", "right"}) {
            TextureRegion[] keys = frames.toArray(TextureRegion.class);
            Animation<TextureRegion> move = new Animation<>(0.1f, keys);
            move.setPlayMode(Animation.PlayMode.LOOP);
            Animation<TextureRegion> attack = new Animation<>(0.1f, keys);
            attack.setPlayMode(Animation.PlayMode.NORMAL);
            set.put("move_" + dir, move, null);
            set.put("attack_" + dir, attack,
                new int[]{2, 3, 4});
        }
        return set;
    }
    private static LaboratoryRoom openRoom(int w, int h) {
        TileType[][] tiles = new TileType[h][w];
        Hazard[][] hazards = new Hazard[h][w];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            tiles[y][x] = TileType.FLOOR;
            hazards[y][x] = Hazard.NONE;
        }
        return new LaboratoryRoom(tiles, new Vector2(1.5f, 1.5f), hazards);
    }
    private static int count(List<Monster> monsters, MonsterType type) {
        int count = 0;
        for (Monster monster : monsters) if (monster.type() == type) count++;
        return count;
    }
    private static List<String> positions(List<Monster> monsters) {
        List<String> result = new ArrayList<>();
        for (Monster monster : monsters)
            result.add(monster.type() + "@" + monster.feetX() + "," + monster.feetY());
        return result;
    }
}

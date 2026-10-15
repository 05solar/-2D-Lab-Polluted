package io.github.some_example_name.combat;

import com.badlogic.gdx.math.Vector2;
import io.github.some_example_name.config.BalanceConfig;
import io.github.some_example_name.entity.monster.Monster;
import io.github.some_example_name.entity.player.Player;
import io.github.some_example_name.render.MonsterAnimationSet;
import io.github.some_example_name.world.LaboratoryRoom;
import io.github.some_example_name.world.TileType;
import org.junit.Test;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;

/**
 * 몬스터 이동 개편(2026-10-15): 근처에 오면 추격하고, 거리가 감지 반경×배수를 넘으면
 * 추격을 포기한 뒤 다른 공간의 목적지로 자유 이동(로밍)하는지 검증한다.
 */
public class MonsterRoamTest {

    private final BalanceConfig config = new BalanceConfig();
    private final Map<MonsterType, MonsterAnimationSet> animations = new HashMap<>();

    private static LaboratoryRoom openRoom(int size) {
        TileType[][] map = new TileType[size][size];
        for (int y = 0; y < size; y++) for (int x = 0; x < size; x++)
            map[y][x] = x == 0 || y == 0 || x == size - 1 || y == size - 1
                ? TileType.WALL : TileType.FLOOR;
        return new LaboratoryRoom(map, new Vector2(size / 2f, size / 2f));
    }

    private PlayerCombatState combatState() {
        return new PlayerCombatState(new PlayerAttackState(0.45f, 4, new int[]{2}));
    }

    private static float distance(Monster m, Player p) {
        float dx = m.feetX() - p.feetX(), dy = m.feetY() - p.feetY();
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    @Test public void chasesWhilePlayerStaysNearby() {
        LaboratoryRoom room = openRoom(30);
        Player player = new Player(18.5f, 15.5f, 0.6f, 0.5f, 100);
        Monster slime = new Monster(1, MonsterType.SLIME, 15.5f, 15.5f, 0.62f, 0.48f, 20);
        List<Monster> monsters = new ArrayList<>();
        monsters.add(slime);
        MonsterAiSystem ai = new MonsterAiSystem(config, room, 7L);
        float before = distance(slime, player);
        for (int i = 0; i < 30; i++)
            ai.update(monsters, player, combatState(), room, animations, false, 1f / 60f);
        assertTrue("감지 반경 안이면 추격 시작", slime.aggro());
        assertTrue("추격 중 플레이어와의 거리가 줄어든다", distance(slime, player) < before - 0.3f);
    }

    @Test public void givesUpChaseBeyondLeashAndRoamsToAnotherSpace() {
        LaboratoryRoom room = openRoom(32);
        Player player = new Player(29.5f, 29.5f, 0.6f, 0.5f, 100); // 감지×배수보다 훨씬 멀리
        Monster slime = new Monster(1, MonsterType.SLIME, 8.5f, 8.5f, 0.62f, 0.48f, 20);
        slime.setAggro(true); // 추격 중이었다고 가정
        List<Monster> monsters = new ArrayList<>();
        monsters.add(slime);
        MonsterAiSystem ai = new MonsterAiSystem(config, room, 7L);
        ai.update(monsters, player, combatState(), room, animations, false, 1f / 60f);
        assertFalse("리시 거리를 넘으면 추격을 포기한다", slime.aggro());
        float originX = slime.feetX(), originY = slime.feetY();
        float maxDisplacement = 0f;
        for (int i = 0; i < 360; i++) { // 6초 자유 이동
            ai.update(monsters, player, combatState(), room, animations, false, 1f / 60f);
            float dx = slime.feetX() - originX, dy = slime.feetY() - originY;
            maxDisplacement = Math.max(maxDisplacement, (float) Math.sqrt(dx * dx + dy * dy));
            if (i % 30 == 0)
                assertFalse("로밍 중에도 벽 안으로 들어가지 않는다",
                    room.isSolid((int) slime.feetX(), (int) slime.feetY()));
        }
        assertTrue("제자리 배회가 아니라 다른 공간으로 이동한다(이동량 " + maxDisplacement + ")",
            maxDisplacement >= 3f);
        assertFalse("멀리 있는 플레이어에게 다시 붙지 않았다", slime.aggro());
    }

    @Test public void roamTargetIsPickedAwayFromCurrentPosition() {
        LaboratoryRoom room = openRoom(32);
        Player player = new Player(29.5f, 29.5f, 0.6f, 0.5f, 100);
        Monster slime = new Monster(1, MonsterType.SLIME, 8.5f, 8.5f, 0.62f, 0.48f, 20);
        List<Monster> monsters = new ArrayList<>();
        monsters.add(slime);
        MonsterAiSystem ai = new MonsterAiSystem(config, room, 7L);
        // 휴식 시간 이후 목적지가 선정되고, 최소 거리(6) 이상 떨어진 설 수 있는 지점이다.
        for (int i = 0; i < 120 && !slime.hasRoamTarget(); i++)
            ai.update(monsters, player, combatState(), room, animations, false, 1f / 60f);
        assertTrue(slime.hasRoamTarget());
        float dx = slime.roamTargetX() - 8.5f, dy = slime.roamTargetY() - 8.5f;
        assertTrue("목적지는 다른 공간(최소 거리 이상)",
            Math.sqrt(dx * dx + dy * dy) >= config.monsterRoamMinDistance - 1.5f);
        assertFalse("목적지는 통행 가능한 셀",
            room.blocksActorAtCell((int) slime.roamTargetX(), (int) slime.roamTargetY(),
                config.monsterBoundsWidth, config.monsterBoundsHeight));
    }

    @Test public void enragedTeamLeaderNeverGivesUpThatEncounter() {
        LaboratoryRoom room = openRoom(32);
        Player player = new Player(29.5f, 29.5f, 0.6f, 0.5f, 100);
        Monster leader = new Monster(1, MonsterType.TEAM_LEADER, 5.5f, 5.5f, 0.62f, 0.48f, 70);
        leader.enrage();
        leader.setAggro(true);
        leader.setState(MonsterState.CHASE);
        List<Monster> monsters = new ArrayList<>();
        monsters.add(leader);
        MonsterAiSystem ai = new MonsterAiSystem(config, room, 7L);
        float before = distance(leader, player);
        for (int i = 0; i < 60; i++)
            ai.update(monsters, player, combatState(), room, animations, false, 1f / 60f);
        assertTrue("분노한 팀장은 거리가 벌어져도 그 조우 동안 추격을 유지한다", leader.aggro());
        assertTrue("팀장은 계속 플레이어 쪽으로 이동한다", distance(leader, player) < before);
    }
}

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
 * 몬스터 행동 상태 기계(2026-10-16) 검증: 시야 기반 발견, 해제 거리 히스테리시스,
 * 시야 상실 유예·마지막 목격 지점 탐색, 활동 구역 복귀, 배회 목표 유효성, 팀장 규칙.
 */
public class MonsterBehaviorTest {

    private final BalanceConfig config = new BalanceConfig();
    private final Map<MonsterType, MonsterAnimationSet> animations = new HashMap<>();
    private static final float DT = 1f / 60f;

    private static LaboratoryRoom openRoom(int size) {
        TileType[][] map = new TileType[size][size];
        for (int y = 0; y < size; y++) for (int x = 0; x < size; x++)
            map[y][x] = x == 0 || y == 0 || x == size - 1 || y == size - 1
                ? TileType.WALL : TileType.FLOOR;
        return new LaboratoryRoom(map, new Vector2(size / 2f, size / 2f));
    }

    /** 32×32 방 가운데(x=15)에 세로 벽이 있고 y=27~28만 뚫린 방. */
    private static LaboratoryRoom walledRoom() {
        TileType[][] map = new TileType[32][32];
        for (int y = 0; y < 32; y++) for (int x = 0; x < 32; x++)
            map[y][x] = x == 0 || y == 0 || x == 31 || y == 31
                ? TileType.WALL : TileType.FLOOR;
        for (int y = 1; y < 31; y++) if (y < 27 || y > 28) map[y][15] = TileType.WALL;
        return new LaboratoryRoom(map, new Vector2(8f, 8f));
    }

    private PlayerCombatState combatState() {
        return new PlayerCombatState(new PlayerAttackState(0.45f, 4, new int[]{2}));
    }

    private void run(MonsterAiSystem ai, List<Monster> monsters, Player player,
                     LaboratoryRoom room, int frames) {
        for (int i = 0; i < frames; i++)
            ai.update(monsters, player, combatState(), room, animations, false, DT);
    }

    @Test public void detectsOnlyNearbyVisiblePlayerAndChases() {
        LaboratoryRoom room = openRoom(30);
        Player player = new Player(18.5f, 15.5f, .6f, .5f, 100);
        Monster slime = new Monster(1, MonsterType.SLIME, 15.5f, 15.5f, .62f, .48f, 20);
        List<Monster> monsters = new ArrayList<>(); monsters.add(slime);
        MonsterAiSystem ai = new MonsterAiSystem(config, room, 7L);
        float before = dist(slime, player);
        run(ai, monsters, player, room, 40);
        assertTrue("발견 거리 안 + 시야 → 추적", slime.aggro());
        assertEquals(MonsterState.CHASE, slime.state());
        assertTrue("추적 중 거리가 줄어든다", dist(slime, player) < before - 0.3f);
    }

    @Test public void wallBlocksDetectionEvenAtCloseRange() {
        LaboratoryRoom room = walledRoom();
        Player player = new Player(16.8f, 8.5f, .6f, .5f, 100);   // 벽 바로 건너편(거리 ~3)
        Monster slime = new Monster(1, MonsterType.SLIME, 13.8f, 8.5f, .62f, .48f, 20);
        List<Monster> monsters = new ArrayList<>(); monsters.add(slime);
        MonsterAiSystem ai = new MonsterAiSystem(config, room, 7L);
        run(ai, monsters, player, room, 180);
        assertFalse("벽 뒤 플레이어는 거리만으로 발견하지 않는다", slime.aggro());
    }

    @Test public void losingSightKeepsChaseBrieflyThenSearchesLastSeenSpot() {
        LaboratoryRoom room = walledRoom();
        Player player = new Player(13.5f, 12.5f, .6f, .5f, 100);
        Monster guard = new Monster(1, MonsterType.GUARD, 9.5f, 12.5f, .62f, .48f, 60);
        List<Monster> monsters = new ArrayList<>(); monsters.add(guard);
        MonsterAiSystem ai = new MonsterAiSystem(config, room, 7L);
        run(ai, monsters, player, room, 20);                       // 발견(거리 4 < 경비 7)
        assertTrue(guard.aggro());
        float seenX = guard.lastSeenX(), seenY = guard.lastSeenY();
        // 플레이어가 벽 건너편으로 순간 이동(시야 상실, 해제 거리 안쪽 유지).
        player.bounds().setPosition(17.5f - player.bounds().width / 2f, 12.5f);
        boolean searched = false;
        float searchFrames = 0f;
        for (int i = 0; i < 60 * 5; i++) {
            ai.update(monsters, player, combatState(), room, animations, false, DT);
            assertTrue("벽을 넘어 현재 위치를 추적하지 않는다", guard.feetX() < 15f);
            if (guard.state() == MonsterState.SEARCH) {
                searched = true;
                searchFrames += 1f;
                if (guard.hasRoamTarget()) {
                    float dx = guard.roamTargetX() - seenX, dy = guard.roamTargetY() - seenY;
                    assertTrue("탐색 목표는 마지막 목격 지점 주변",
                        Math.sqrt(dx * dx + dy * dy) < 3.5f);
                }
            }
        }
        assertTrue("시야 상실 유예 후 마지막 목격 지점을 탐색한다", searched);
        assertTrue("탐색은 설정 시간 안에서 끝난다",
            searchFrames * DT <= config.monsterSearchMaxSeconds + 0.5f);
        assertFalse("벽 너머 플레이어를 계속 쫓지 않는다", guard.aggro());
    }

    @Test public void releaseBeyondRadiusDoesNotFlapAtTheBoundary() {
        LaboratoryRoom room = openRoom(40);
        Player player = new Player(28.9f, 20.5f, .6f, .5f, 100);   // 해제 8 초과, 발견 4 초과
        Monster slime = new Monster(1, MonsterType.SLIME, 20.5f, 20.5f, .62f, .48f, 20);
        slime.setAggro(true);
        slime.setSeesPlayer(true);
        slime.rememberSeen(player.feetX(), player.feetY());
        List<Monster> monsters = new ArrayList<>(); monsters.add(slime);
        MonsterAiSystem ai = new MonsterAiSystem(config, room, 7L);
        int chaseReacquired = 0;
        boolean released = false;
        for (int i = 0; i < 60 * 5; i++) {
            boolean beforeAggro = slime.aggro();
            ai.update(monsters, player, combatState(), room, animations, false, DT);
            if (beforeAggro && !slime.aggro()) released = true;
            if (released && slime.aggro()) chaseReacquired++;
        }
        assertTrue("해제 거리를 넘으면 추적을 멈춘다", released);
        assertFalse(slime.aggro());
        assertEquals("경계에서 추적이 반복 재획득되지 않는다(발견<해제 히스테리시스)",
            0, chaseReacquired);
    }

    @Test public void wanderTargetsStayWalkableAndInsideHomeLeash() {
        LaboratoryRoom room = openRoom(40);
        Player player = new Player(37.5f, 37.5f, .6f, .5f, 100);   // 멀리(간섭 없음)
        Monster slime = new Monster(1, MonsterType.SLIME, 10.5f, 10.5f, .62f, .48f, 20);
        Monster researcher = new Monster(2, MonsterType.RESEARCHER, 20.5f, 10.5f, .62f, .48f, 40);
        Monster guard = new Monster(3, MonsterType.GUARD, 10.5f, 20.5f, .62f, .48f, 60);
        List<Monster> monsters = new ArrayList<>();
        monsters.add(slime); monsters.add(researcher); monsters.add(guard);
        MonsterAiSystem ai = new MonsterAiSystem(config, room, 7L);
        boolean slimeMoved = false, guardMoved = false, researcherStayedClose = true;
        for (int i = 0; i < 60 * 12; i++) {
            ai.update(monsters, player, combatState(), room, animations, false, DT);
            for (Monster m : monsters) {
                if (m.hasRoamTarget()) {
                    assertFalse("배회 목표는 벽·가구 안이 아니다", room.blocksActorAtCell(
                        (int) m.roamTargetX(), (int) m.roamTargetY(),
                        config.monsterBoundsWidth, config.monsterBoundsHeight));
                    float hx = m.roamTargetX() - m.homeX(), hy = m.roamTargetY() - m.homeY();
                    assertTrue("배회 목표는 활동 구역 안",
                        hx * hx + hy * hy <= config.monsterHomeLeashRadius
                            * config.monsterHomeLeashRadius + .001f);
                }
                float dx = m.feetX() - m.homeX(), dy = m.feetY() - m.homeY();
                assertTrue("배회 중 활동 구역을 벗어나지 않는다",
                    dx * dx + dy * dy <= config.monsterHomeLeashRadius
                        * config.monsterHomeLeashRadius + 4f);
            }
            if (dist2(slime.feetX(), slime.feetY(), 10.5f, 10.5f) > .25f) slimeMoved = true;
            if (dist2(guard.feetX(), guard.feetY(), 10.5f, 20.5f) > .25f) guardMoved = true;
            if (dist2(researcher.feetX(), researcher.feetY(), 20.5f, 10.5f)
                > (config.researcherWanderRadius + 2f) * (config.researcherWanderRadius + 2f))
                researcherStayedClose = false;
        }
        assertTrue("슬라임은 짧게나마 돌아다닌다", slimeMoved);
        assertTrue("경비원은 순찰한다", guardMoved);
        assertTrue("연구원은 장비(홈) 주변에 머문다", researcherStayedClose);
    }

    @Test public void walkingNearTeamLeaderNeverEnragesIt() {
        LaboratoryRoom room = openRoom(20);
        Player player = new Player(6.3f, 5.5f, .6f, .5f, 100);     // 팀장 바로 옆에서 걷기만
        Monster leader = new Monster(1, MonsterType.TEAM_LEADER, 5.5f, 5.5f, .62f, .48f, 70);
        List<Monster> monsters = new ArrayList<>(); monsters.add(leader);
        MonsterAiSystem ai = new MonsterAiSystem(config, room, 7L);
        run(ai, monsters, player, room, 240);                      // runStarted=false 유지
        assertFalse("걷기만으로는 분노하지 않는다", leader.enraged());
        assertFalse("근접 발견 규칙으로 추적하지 않는다", leader.aggro());
        assertEquals(MonsterState.IDLE, leader.state());
    }

    @Test public void enragedLeaderReleasesReturnsHomeAndCalmsDown() {
        LaboratoryRoom room = openRoom(40);
        Player player = new Player(35.5f, 35.5f, .6f, .5f, 100);   // 해제 거리 밖
        Monster leader = new Monster(1, MonsterType.TEAM_LEADER, 8.5f, 8.5f, .62f, .48f, 70);
        leader.enrage();
        leader.setAggro(true);
        leader.setState(MonsterState.CHASE);
        // 추적 중 홈에서 끌려 나온 상태를 재현.
        leader.bounds().setPosition(12.5f - leader.bounds().width / 2f, 12.5f);
        List<Monster> monsters = new ArrayList<>(); monsters.add(leader);
        MonsterAiSystem ai = new MonsterAiSystem(config, room, 7L);
        boolean returned = false;
        for (int i = 0; i < 60 * 15 && !returned; i++) {
            ai.update(monsters, player, combatState(), room, animations, false, DT);
            float dx = leader.feetX() - leader.homeX(), dy = leader.feetY() - leader.homeY();
            if (!leader.enraged() && dx * dx + dy * dy < 1.5f * 1.5f) returned = true;
        }
        assertTrue("팀장은 걸어서 원래 구석으로 복귀한다", returned);
        assertFalse("복귀 완료 시 분노 해제", leader.enraged());
        assertEquals("복귀 완료 시 소음 누적 초기화", 0, leader.nearbyRunStarts());
        assertFalse(leader.aggro());
        // 복귀 후 다시 두 번의 달리기 시작으로 분노할 수 있다.
        player.bounds().setPosition(9.5f, 8.5f);
        ai.registerNoise(monsters, player.feetX(), player.feetY());
        ai.registerNoise(monsters, player.feetX(), player.feetY());
        assertTrue("초기화된 카운트로 기존 2회 규칙이 다시 동작한다", leader.enraged());
    }

    private static float dist(Monster m, Player p) {
        float dx = m.feetX() - p.feetX(), dy = m.feetY() - p.feetY();
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    private static float dist2(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2, dy = y1 - y2;
        return dx * dx + dy * dy;
    }
}

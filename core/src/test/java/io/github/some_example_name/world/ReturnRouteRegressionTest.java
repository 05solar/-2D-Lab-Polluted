package io.github.some_example_name.world;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import io.github.some_example_name.combat.CombatCollisionGrid;
import io.github.some_example_name.combat.MonsterSpawnSystem;
import io.github.some_example_name.combat.MonsterType;
import io.github.some_example_name.config.BalanceConfig;
import io.github.some_example_name.entity.monster.Monster;
import org.junit.Test;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import static org.junit.Assert.*;

/**
 * 귀환 버튼 발자국 미표시 버그(2026-10-20) 회귀 테스트.
 * 원인: ① 플레이어가 가구/상자에 붙어 서면 시작 셀 중심이 막힌 것으로 판정돼 경로가 빈 리스트,
 * ② EASY 8배·로밍 몬스터가 복도를 완전히 막으면 동적 그리드 경로가 빈 리스트.
 * 수정: 시작/목표 셀 주변 보정(shortestPathNear) + 정적 지형 폴백(화면 로직).
 */
public class ReturnRouteRegressionTest {

    private static LaboratoryRoom openRoom(int size) {
        TileType[][] map = new TileType[size][size];
        for (int y = 0; y < size; y++) for (int x = 0; x < size; x++)
            map[y][x] = x == 0 || y == 0 || x == size - 1 || y == size - 1
                ? TileType.WALL : TileType.FLOOR;
        return new LaboratoryRoom(map, new Vector2(size / 2f, size / 2f));
    }

    @Test public void standingFlushAgainstAContainerStillYieldsARoute() {
        LaboratoryRoom room = openRoom(12);
        // 상자 footprint가 (5,5) 셀 중심을 덮는다 — 플레이어가 상자 바로 앞에 선 상황.
        room.addWorldObstacle(new Rectangle(5.08f, 5.3f, .84f, .48f));
        RoomPathfinder pathfinder = new RoomPathfinder();
        assertTrue("기존 동작(버그): 시작 셀 중심이 막히면 경로가 비었다",
            pathfinder.shortestPath(room, 5, 5, 1, 1, .6f, .5f).isEmpty());
        List<RoomPathfinder.Cell> fixed = pathfinder.shortestPathNear(room, 5, 5, 1, 1, .6f, .5f);
        assertFalse("주변 셀 보정으로 발자국 경로가 나온다", fixed.isEmpty());
    }

    @Test public void monstersFullyBlockingTheCorridorFallBackToStaticTerrainRoute() {
        // 가운데 2타일 복도 하나만 남긴 방: x=5,6만 통행, 나머지 세로줄은 벽.
        TileType[][] map = new TileType[14][12];
        for (int y = 0; y < 14; y++) for (int x = 0; x < 12; x++)
            map[y][x] = (x == 5 || x == 6) && y > 0 && y < 13 ? TileType.FLOOR : TileType.WALL;
        LaboratoryRoom room = new LaboratoryRoom(map, new Vector2(5.5f, 1.5f));
        // 복도 중간을 몬스터 두 마리가 나란히 완전히 막는다.
        List<Monster> monsters = new ArrayList<>();
        monsters.add(new Monster(1, MonsterType.SLIME, 5.5f, 6.5f, .62f, .48f, 20));
        monsters.add(new Monster(2, MonsterType.SLIME, 6.5f, 6.5f, .62f, .48f, 20));
        CombatCollisionGrid dynamic = new CombatCollisionGrid(room);
        dynamic.forPlayer(monsters);
        RoomPathfinder pathfinder = new RoomPathfinder();
        List<RoomPathfinder.Cell> withBodies =
            pathfinder.shortestPathNear(dynamic, 5, 1, 5, 12, .6f, .5f);
        assertTrue("몬스터 몸체 기준으로는 막힌다(기존 동작)", withBodies.isEmpty());
        // 화면 로직의 폴백: 정적 지형만으로는 항상 안내가 나온다.
        List<RoomPathfinder.Cell> fallback =
            pathfinder.shortestPathNear(room, 5, 1, 5, 12, .6f, .5f);
        assertFalse("정적 지형 폴백으로 발자국이 유지된다", fallback.isEmpty());
    }

    @Test public void easyMapAlwaysProducesAReturnRouteFromReachableCells() {
        BalanceConfig config = new BalanceConfig();
        config.applyDifficulty(GameDifficulty.EASY);
        LaboratoryLayout layout = LaboratoryLayout.forDifficulty(GameDifficulty.EASY, config);
        LaboratoryRoom room = layout.room();
        List<Monster> monsters = new MonsterSpawnSystem(config).spawn(room, 20261020L);
        CombatCollisionGrid dynamic = new CombatCollisionGrid(room);
        dynamic.forPlayer(monsters);
        RoomPathfinder pathfinder = new RoomPathfinder();
        int gateX = room.southExitCenterX(), gateY = room.southExitPathY();
        // 스폰에서 도달 가능한 셀을 BFS로 모아 일부를 표본으로 검사한다.
        boolean[][] seen = new boolean[room.heightInTiles()][room.widthInTiles()];
        Deque<int[]> queue = new ArrayDeque<>();
        int sx = (int) room.spawnPoint().x, sy = (int) room.spawnPoint().y;
        seen[sy][sx] = true;
        queue.add(new int[]{sx, sy});
        List<int[]> reachable = new ArrayList<>();
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        while (!queue.isEmpty()) {
            int[] c = queue.removeFirst();
            // 표본: 3×3이 모두 설 수 있는 열린 셀(복도·방 가운데). 가구 아트 뒤의
            // 몸체 기준 진입 불가 포켓(타일 BFS만 도달)은 실제 플레이 위치가 아니라 제외.
            boolean open = true;
            for (int dy = -1; dy <= 1 && open; dy++)
                for (int dx = -1; dx <= 1; dx++)
                    if (room.blocksActorAtCell(c[0] + dx, c[1] + dy, .6f, .5f)) { open = false; break; }
            if (open) reachable.add(c);
            for (int[] d : dirs) {
                int x = c[0] + d[0], y = c[1] + d[1];
                if (x >= 0 && y >= 0 && x < room.widthInTiles() && y < room.heightInTiles()
                    && !seen[y][x] && !room.isSolid(x, y)) {
                    seen[y][x] = true;
                    queue.add(new int[]{x, y});
                }
            }
        }
        int checked = 0;
        for (int i = 0; i < reachable.size() && checked < 40; i += Math.max(1, reachable.size() / 40)) {
            int[] cell = reachable.get(i);
            // 화면 로직 그대로: 동적 그리드 → 비면 정적 지형 폴백.
            List<RoomPathfinder.Cell> path = pathfinder.shortestPathNear(
                dynamic, cell[0], cell[1], gateX, gateY, .6f, .5f);
            if (path.isEmpty())
                path = pathfinder.shortestPathNear(room, cell[0], cell[1], gateX, gateY, .6f, .5f);
            assertFalse("EASY 8배 몬스터가 있어도 " + cell[0] + "," + cell[1]
                + " 에서 귀환 발자국이 나온다", path.isEmpty());
            checked++;
        }
        assertTrue(checked >= 30);
    }
}

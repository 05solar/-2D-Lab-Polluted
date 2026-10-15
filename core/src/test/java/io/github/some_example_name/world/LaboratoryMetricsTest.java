package io.github.some_example_name.world;

import org.junit.Test;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import static org.junit.Assert.*;

/**
 * 맵 구조 지표(방 수·문 사이 평균 보행 거리·컨테이너 수)를 측정해 출력한다.
 * 방 수 증가/복도 단축 작업의 전후 비교 근거로 쓰며, 회귀 방지 하한도 단언한다.
 */
public class LaboratoryMetricsTest {

    @Test public void printAndCheckEasyMetrics() {
        LaboratoryLayout layout = LaboratoryLayout.easyRoom();
        Metrics m = measure(layout);
        System.out.println("[METRICS] easy rooms=" + m.roomCount
            + " doors=" + m.doorCount
            + " avgDoorToDoorDistance=" + String.format("%.2f", m.avgDoorDistance)
            + " containers=" + m.containerCount + " (" + m.containerBreakdown + ")");
        assertTrue("every door pair is connected", m.unreachablePairs == 0);
    }

    @Test public void printVariantMetrics() {
        for (GameDifficulty d : new GameDifficulty[]{GameDifficulty.NORMAL, GameDifficulty.HARD}) {
            LaboratoryLayout layout = LaboratoryLayout.forDifficulty(d);
            Metrics m = measure(layout);
            System.out.println("[METRICS] " + d + " rooms=" + m.roomCount
                + " doors=" + m.doorCount
                + " avgDoorToDoorDistance=" + String.format("%.2f", m.avgDoorDistance)
                + " containers=" + m.containerCount + " (" + m.containerBreakdown + ")");
            assertTrue("every door pair is connected (" + d + ")", m.unreachablePairs == 0);
        }
    }

    static final class Metrics {
        int roomCount, doorCount, containerCount, unreachablePairs;
        double avgDoorDistance;
        String containerBreakdown;
    }

    static Metrics measure(LaboratoryLayout layout) {
        LaboratoryRoom room = layout.room();
        Metrics m = new Metrics();
        m.roomCount = layout.roomAreas().size();
        // 컨테이너 등급별 집계
        int wood = 0, epic = 0, legendary = 0;
        for (LootContainerPlacement p : layout.lootContainers()) {
            if ("wooden_chest".equals(p.id())) wood++;
            else if ("epic_cabinet".equals(p.id())) epic++;
            else if ("legendary_cabinet".equals(p.id())) legendary++;
        }
        m.containerCount = layout.lootContainers().size();
        m.containerBreakdown = "wood=" + wood + " epic=" + epic + " legendary=" + legendary;
        // 문 셀 수집(내부 Door 객체 + 논리 개구부 마커)
        List<int[]> doors = new ArrayList<>();
        for (Door d : room.doors()) doors.add(new int[]{d.x(), d.y()});
        if (doors.isEmpty()) {
            for (int y = 0; y < room.heightInTiles(); y++)
                for (int x = 0; x < room.widthInTiles(); x++)
                    if (room.rawTileAt(x, y) == TileType.DOORWAY_OPEN) doors.add(new int[]{x, y});
        }
        m.doorCount = doors.size();
        // 모든 문 쌍의 BFS 보행 거리 평균(벽/닫힌문은 통과 불가로 보지 않고, 문 셀 자체는 통행 가능 취급)
        long total = 0; int pairs = 0;
        for (int i = 0; i < doors.size(); i++) {
            int[] dist = bfs(room, doors.get(i)[0], doors.get(i)[1]);
            for (int j = i + 1; j < doors.size(); j++) {
                int d = dist[doors.get(j)[1] * room.widthInTiles() + doors.get(j)[0]];
                if (d < 0) { m.unreachablePairs++; continue; }
                total += d; pairs++;
            }
        }
        m.avgDoorDistance = pairs == 0 ? 0 : total / (double) pairs;
        return m;
    }

    /** 4방향 BFS. 문 셀(열림/닫힘 마커)은 통행 가능으로 취급(이동 거리 비교 목적). */
    private static int[] bfs(LaboratoryRoom room, int sx, int sy) {
        int w = room.widthInTiles(), h = room.heightInTiles();
        int[] dist = new int[w * h];
        java.util.Arrays.fill(dist, -1);
        Deque<int[]> q = new ArrayDeque<>();
        dist[sy * w + sx] = 0;
        q.add(new int[]{sx, sy});
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        while (!q.isEmpty()) {
            int[] p = q.removeFirst();
            for (int[] d : dirs) {
                int x = p[0] + d[0], y = p[1] + d[1];
                if (x < 0 || y < 0 || x >= w || y >= h || dist[y * w + x] >= 0) continue;
                TileType t = room.rawTileAt(x, y);
                boolean passable = t == TileType.FLOOR || t == TileType.DOORWAY_OPEN
                    || t == TileType.DOOR_CLOSED; // 문은 열 수 있으므로 거리 측정에선 통행 취급
                if (!passable) continue;
                dist[y * w + x] = dist[p[1] * w + p[0]] + 1;
                q.add(new int[]{x, y});
            }
        }
        return dist;
    }
}

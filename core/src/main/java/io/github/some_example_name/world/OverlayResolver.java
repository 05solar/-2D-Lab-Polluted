package io.github.some_example_name.world;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * 바닥 위 오버레이(오염/전선/장식)와 hazard 데이터를 고정 seed로 1회 결정한다.
 * 셀당 최대 1개, 같은 오버레이 ID를 인접에 반복하지 않으며, 오버레이는 통과 가능(FLOOR/DOORWAY_OPEN)
 * 셀에만 올린다(벽 위에 그리지 않음). 선택 ID는 모두 V2 오버레이 아틀라스의 실제 타일이다.
 *
 * hazard는 시각과 분리: 독성 계열(toxic/contamination) → TOXIC, 전기 스파크 → SHOCK.
 * 전선(cable/exposed_wires) 자체는 hazard가 아니다(스파크만 감전).
 */
public final class OverlayResolver {

    public static final long DEFAULT_SEED = 918273L;

    public static final class Result {
        public final String[][] overlay;  // [ty][tx], null 가능
        public final Hazard[][] hazard;   // [ty][tx], NONE 기본
        Result(String[][] overlay, Hazard[][] hazard) { this.overlay = overlay; this.hazard = hazard; }
    }

    private final long seed;

    public OverlayResolver(long seed) { this.seed = seed; }

    public Result resolve(TileType[][] logical, LaboratoryZone[][] zones) {
        int h = logical.length, w = logical[0].length;
        String[][] ov = new String[h][w];
        Hazard[][] hz = new Hazard[h][w];
        for (Hazard[] row : hz) java.util.Arrays.fill(row, Hazard.NONE);
        Random rnd = new Random(seed);

        boolean[][] walk = new boolean[h][w];
        for (int ty = 0; ty < h; ty++)
            for (int tx = 0; tx < w; tx++)
                walk[ty][tx] = logical[ty][tx] == TileType.FLOOR || logical[ty][tx] == TileType.DOORWAY_OPEN;

        // ---- 오염 구역: 불규칙 독성 군집(안전 바닥은 남긴다) ----
        List<int[]> contam = cellsOfZone(zones, walk, LaboratoryZone.CONTAM);
        floodCluster(ov, hz, walk, contam, rnd, new int[]{16, 11}, 4,
                new String[]{"toxic_puddle_large", "toxic_puddle_small", "toxic_bubbles"}, Hazard.TOXIC);
        floodCluster(ov, hz, walk, contam, rnd, new int[]{14, 10}, 3,
                new String[]{"toxic_puddle_small", "contamination_spatter"}, Hazard.TOXIC);
        placeHazard(ov, hz, walk, 17, 13, "contamination_spatter", Hazard.TOXIC);
        placeHazard(ov, hz, walk, 13, 9, "drain_leak", Hazard.TOXIC);

        // ---- 정비 구역: 연결된 전선 배선 + 노출 전선 + 스파크(감전) ----
        place(ov, walk, 16, 4, "cable_horizontal");
        place(ov, walk, 15, 4, "cable_horizontal");
        place(ov, walk, 14, 4, "cable_corner");
        place(ov, walk, 14, 3, "cable_vertical");
        place(ov, walk, 14, 2, "exposed_wires");
        placeHazard(ov, hz, walk, 17, 2, "electric_sparks", Hazard.SHOCK);

        // ---- 중앙/입구: 가벼운 장식 산포(겹침·인접 반복 없음) ----
        String[] decor = {"grime_small", "grime_medium", "oil_stain", "water_stain",
                "crack_overlay_small", "scorch_mark", "lab_residue", "rubble_small", "broken_glass"};
        List<int[]> central = cellsOfZone(zones, walk, LaboratoryZone.CENTRAL);
        shuffle(central, rnd);
        int placed = 0;
        for (int[] c : central) {
            if (placed >= 10) break;
            int tx = c[0], ty = c[1];
            if (ov[ty][tx] != null) continue;
            String id = decor[rnd.nextInt(decor.length)];
            if (hasAdjacent(ov, tx, ty, id)) continue;
            ov[ty][tx] = id;
            placed++;
        }
        return new Result(ov, hz);
    }

    private static List<int[]> cellsOfZone(LaboratoryZone[][] zones, boolean[][] walk, LaboratoryZone z) {
        List<int[]> out = new ArrayList<>();
        for (int ty = 0; ty < zones.length; ty++)
            for (int tx = 0; tx < zones[0].length; tx++)
                if (zones[ty][tx] == z && walk[ty][tx]) out.add(new int[]{tx, ty});
        return out;
    }

    private void floodCluster(String[][] ov, Hazard[][] hz, boolean[][] walk, List<int[]> region,
                              Random rnd, int[] seedCell, int grow, String[] ids, Hazard tag) {
        int sx = seedCell[0], sy = seedCell[1];
        if (!inRegion(region, sx, sy) || !walk[sy][sx] || ov[sy][sx] != null) return;
        Deque<int[]> frontier = new ArrayDeque<>();
        setCell(ov, hz, sx, sy, ids[rnd.nextInt(ids.length)], tag);
        frontier.add(new int[]{sx, sy});
        int n = 1;
        while (!frontier.isEmpty() && n < grow) {
            int[] c = frontier.poll();
            int[][] nbrs = {{c[0] + 1, c[1]}, {c[0] - 1, c[1]}, {c[0], c[1] + 1}, {c[0], c[1] - 1}};
            shuffle4(nbrs, rnd);
            for (int[] nb : nbrs) {
                int nx = nb[0], ny = nb[1];
                if (inRegion(region, nx, ny) && walk[ny][nx] && ov[ny][nx] == null) {
                    setCell(ov, hz, nx, ny, ids[rnd.nextInt(ids.length)], tag);
                    frontier.add(new int[]{nx, ny});
                    if (++n >= grow) break;
                }
            }
        }
    }

    private static void setCell(String[][] ov, Hazard[][] hz, int tx, int ty, String id, Hazard tag) {
        ov[ty][tx] = id;
        hz[ty][tx] = tag;
    }

    private static void place(String[][] ov, boolean[][] walk, int tx, int ty, String id) {
        if (inBounds(ov, tx, ty) && walk[ty][tx] && ov[ty][tx] == null) ov[ty][tx] = id;
    }

    private static void placeHazard(String[][] ov, Hazard[][] hz, boolean[][] walk,
                                    int tx, int ty, String id, Hazard tag) {
        if (inBounds(ov, tx, ty) && walk[ty][tx] && ov[ty][tx] == null) {
            ov[ty][tx] = id; hz[ty][tx] = tag;
        }
    }

    private static boolean hasAdjacent(String[][] ov, int tx, int ty, String id) {
        return eq(ov, tx + 1, ty, id) || eq(ov, tx - 1, ty, id)
            || eq(ov, tx, ty + 1, id) || eq(ov, tx, ty - 1, id);
    }

    private static boolean eq(String[][] ov, int tx, int ty, String id) {
        return inBounds(ov, tx, ty) && id.equals(ov[ty][tx]);
    }

    private static boolean inBounds(String[][] a, int tx, int ty) {
        return ty >= 0 && ty < a.length && tx >= 0 && tx < a[0].length;
    }

    private static boolean inRegion(List<int[]> region, int tx, int ty) {
        for (int[] c : region) if (c[0] == tx && c[1] == ty) return true;
        return false;
    }

    private static void shuffle(List<int[]> list, Random rnd) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            int[] t = list.get(i); list.set(i, list.get(j)); list.set(j, t);
        }
    }

    private static void shuffle4(int[][] a, Random rnd) {
        for (int i = a.length - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            int[] t = a[i]; a[i] = a[j]; a[j] = t;
        }
    }
}

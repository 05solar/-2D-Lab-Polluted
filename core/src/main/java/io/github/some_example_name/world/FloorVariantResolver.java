package io.github.some_example_name.world;

import java.util.Random;

/**
 * 바닥 변형을 고정 seed로 1회 결정한다(실행마다 동일). 구역별로 분포를 다르게 주되
 * 같은 바닥 ID가 가로·세로로 3개 이상 연속되지 않게 한다. 결과는 통과 가능 셀의 바닥 ID,
 * 벽 셀은 null. 선택 ID는 모두 V2 바닥 아틀라스의 실제 타일이다.
 *
 * 권장 분포(README): 기본 A/B/C 65~75%, 약한 마모·얼룩 15~20%, 균열·보수·배수 5~10%,
 * 강한 파손/노출 5% 이하.
 */
public final class FloorVariantResolver {

    public static final long DEFAULT_SEED = 20261007L;

    private static final String[] CLEAN = {"floor_clean_a", "floor_clean_b", "floor_clean_c"};

    private final long seed;

    public FloorVariantResolver(long seed) { this.seed = seed; }

    public String[][] resolve(TileType[][] logical, LaboratoryZone[][] zones) {
        int h = logical.length, w = logical[0].length;
        String[][] floor = new String[h][w];
        Random rnd = new Random(seed);
        for (int ty = 0; ty < h; ty++) {
            for (int tx = 0; tx < w; tx++) {
                if (logical[ty][tx] == TileType.WALL) { floor[ty][tx] = null; continue; }
                String pick = pickFor(zones[ty][tx], rnd);
                // 같은 ID 가로/세로 3연속 방지(이미 배치된 좌/하만 검사하면 충분)
                if (runLeft(floor, tx, ty, pick) >= 2 || runDown(floor, tx, ty, pick) >= 2) {
                    pick = pick.equals("floor_clean_a") ? "floor_clean_b" : "floor_clean_a";
                }
                floor[ty][tx] = pick;
            }
        }
        return floor;
    }

    private String pickFor(LaboratoryZone z, Random rnd) {
        double r = rnd.nextDouble();
        switch (z) {
            case ENTRANCE: // 안전 구역: 깨끗하게
                if (r < 0.62) return "floor_clean_a";
                if (r < 0.84) return "floor_clean_b";
                if (r < 0.96) return "floor_clean_c";
                return "floor_worn_light";
            case CONTAM:
                if (r < 0.55) return CLEAN[rnd.nextInt(3)];
                if (r < 0.80) return pick(rnd, "floor_damp_stain", "floor_worn_light", "floor_scratched");
                return pick(rnd, "floor_crack_medium", "floor_broken_panel", "floor_drainage_grate");
            case MAINT:
                if (r < 0.45) return CLEAN[rnd.nextInt(3)];
                if (r < 0.72) return pick(rnd, "floor_worn_light", "floor_scratched", "floor_dust_edge");
                return pick(rnd, "floor_patch_welded", "floor_patch_bolted", "floor_access_hatch",
                        "floor_broken_panel", "floor_exposed_underplate");
            case CENTRAL:
            default:
                if (r < 0.70) return CLEAN[rnd.nextInt(3)];
                if (r < 0.86) return pick(rnd, "floor_worn_light", "floor_scratched", "floor_dust_edge");
                if (r < 0.95) return pick(rnd, "floor_patch_welded", "floor_patch_bolted", "floor_access_hatch");
                return pick(rnd, "floor_crack_light", "floor_seam_horizontal", "floor_seam_vertical");
        }
    }

    private static String pick(Random rnd, String... options) {
        return options[rnd.nextInt(options.length)];
    }

    private static int runLeft(String[][] f, int tx, int ty, String id) {
        int n = 0;
        for (int x = tx - 1; x >= 0 && id.equals(f[ty][x]); x--) n++;
        return n;
    }

    private static int runDown(String[][] f, int tx, int ty, String id) {
        int n = 0;
        for (int y = ty - 1; y >= 0 && id.equals(f[y][tx]); y--) n++;
        return n;
    }
}

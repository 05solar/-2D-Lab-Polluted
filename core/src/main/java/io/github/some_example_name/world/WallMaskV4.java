package io.github.some_example_name.world;

/**
 * V4 연결 벽 규격(laboratory_structure_v4). 하나의 논리 셀 점유 데이터에서 8방향 이웃 마스크를
 * 계산해 47종 아틀라스 인덱스(0..46)를 고른다. 시각과 충돌 모두 같은 점유 데이터에서 파생된다.
 *
 * 비트: N=1, NE=2, E=4, SE=8, S=16, SW=32, W=64, NW=128.
 * 월드 좌표는 y가 위로 증가하므로 북=y+1이다. 대각선 비트는 인접한 두 직교 이웃이 모두 벽일 때만
 * 유지한다(정규화). 256가지 입력이 모두 47종 중 하나로 매핑된다.
 *
 * 벽/문 연결: WALL, DOOR_CLOSED, DOORWAY_OPEN 셀을 "연결됨"으로 본다. 그래야 문 개구부를 지나
 * 벽선이 끊기지 않고 문틀로 이어진다. 범위 밖은 비연결(외곽 벽이 직선으로 끝나도록).
 */
public final class WallMaskV4 {

    public static final int N = 1, NE = 2, E = 4, SE = 8, S = 16, SW = 32, W = 64, NW = 128;

    /** 아틀라스 열 수(8열 × 6행, 128px 셀). */
    public static final int ATLAS_COLUMNS = 8;
    public static final int TILE_PIXELS = 128;
    public static final int VARIANT_COUNT = 47;

    private WallMaskV4() { }

    /** 대각선은 양옆 직교 이웃이 모두 벽일 때만 유지한다. */
    public static int canonicalMask(int rawMask) {
        int mask = rawMask & 255;
        if ((mask & N) == 0 || (mask & E) == 0) mask &= ~NE;
        if ((mask & S) == 0 || (mask & E) == 0) mask &= ~SE;
        if ((mask & S) == 0 || (mask & W) == 0) mask &= ~SW;
        if ((mask & N) == 0 || (mask & W) == 0) mask &= ~NW;
        return mask;
    }

    /** 정규화 마스크 → 아틀라스 인덱스(오름차순 정규마스크 순서, 구 V2/V3 인덱스가 아님). */
    public static int atlasIndex(int rawMask) {
        int wanted = canonicalMask(rawMask);
        int index = 0;
        for (int mask = 0; mask < 256; mask++) {
            if (canonicalMask(mask) != mask) continue;
            if (mask == wanted) return index;
            index++;
        }
        throw new IllegalArgumentException("지원하지 않는 벽 마스크: " + rawMask);
    }

    /** 논리 방의 (tx,ty) 벽 셀에 대한 8방향 원시 마스크(월드 y-up, 북=y+1). */
    public static int rawMask(LaboratoryRoom room, int tx, int ty) {
        int mask = 0;
        if (connected(room, tx, ty + 1)) mask |= N;
        if (connected(room, tx + 1, ty + 1)) mask |= NE;
        if (connected(room, tx + 1, ty)) mask |= E;
        if (connected(room, tx + 1, ty - 1)) mask |= SE;
        if (connected(room, tx, ty - 1)) mask |= S;
        if (connected(room, tx - 1, ty - 1)) mask |= SW;
        if (connected(room, tx - 1, ty)) mask |= W;
        if (connected(room, tx - 1, ty + 1)) mask |= NW;
        return mask;
    }

    /** (tx,ty) 벽 셀이 그릴 아틀라스 인덱스. */
    public static int atlasIndex(LaboratoryRoom room, int tx, int ty) {
        return atlasIndex(rawMask(room, tx, ty));
    }

    /** 벽/문 셀이면 연결됨. 범위 밖은 비연결. */
    private static boolean connected(LaboratoryRoom room, int tx, int ty) {
        if (tx < 0 || ty < 0 || tx >= room.widthInTiles() || ty >= room.heightInTiles()) {
            return false;
        }
        TileType t = room.rawTileAt(tx, ty);
        return t == TileType.WALL || t == TileType.DOOR_CLOSED || t == TileType.DOORWAY_OPEN;
    }
}

package io.github.some_example_name.world;

import java.util.logging.Logger;

/** 논리 벽 배열에서 문의 축을 판정하고 5칸 직선 구간의 문틀을 조립한다. */
public final class DoorPlacementResolver {
    private static final Logger LOG = Logger.getLogger(DoorPlacementResolver.class.getName());

    public static final class Result {
        public final Door[][] doors;
        public final String[][] structure;

        Result(Door[][] doors, String[][] structure) {
            this.doors = doors;
            this.structure = structure;
        }
    }

    public Result resolve(TileType[][] logical) {
        int height = logical.length, width = logical[0].length;
        Door[][] doors = new Door[height][width];
        String[][] structure = new String[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                TileType type = logical[y][x];
                if (type != TileType.DOOR_CLOSED && type != TileType.DOORWAY_OPEN) continue;
                boolean horizontal = wall(logical, x - 1, y) && wall(logical, x + 1, y)
                    && !wallLike(logical, x, y - 1) && !wallLike(logical, x, y + 1);
                boolean vertical = wall(logical, x, y - 1) && wall(logical, x, y + 1)
                    && !wallLike(logical, x - 1, y) && !wallLike(logical, x + 1, y);
                if (horizontal == vertical) invalid(x, y, "문 방향을 하나로 판정할 수 없음");
                Door.Orientation orientation = horizontal ? Door.Orientation.HORIZONTAL : Door.Orientation.VERTICAL;
                if (horizontal) {
                    if (!straightSupport(logical, x - 2, y, true) ||
                        !straightSupport(logical, x + 2, y, true))
                        invalid(x, y, "가로 문틀을 포함한 직선 벽 5칸 부족 또는 접합부와 2칸 이내");
                    structure[y][x - 1] = "door_jamb_left";
                    structure[y][x + 1] = "door_jamb_right";
                } else {
                    if (!straightSupport(logical, x, y - 2, false) ||
                        !straightSupport(logical, x, y + 2, false))
                        invalid(x, y, "세로 문틀을 포함한 직선 벽 5칸 부족 또는 접합부와 2칸 이내");
                    structure[y - 1][x] = "vertical_door_jamb_bottom";
                    structure[y + 1][x] = "vertical_door_jamb_top";
                }
                doors[y][x] = new Door(x, y, orientation,
                    type == TileType.DOORWAY_OPEN ? DoorState.OPEN : DoorState.CLOSED);
            }
        }
        return new Result(doors, structure);
    }

    private static boolean wall(TileType[][] logical, int x, int y) {
        return y >= 0 && y < logical.length && x >= 0 && x < logical[0].length
            && logical[y][x] == TileType.WALL;
    }

    private static boolean wallLike(TileType[][] logical, int x, int y) {
        return y >= 0 && y < logical.length && x >= 0 && x < logical[0].length
            && logical[y][x] != TileType.FLOOR;
    }

    private static boolean straightSupport(TileType[][] logical, int x, int y, boolean horizontal) {
        if (!wall(logical, x, y)) return false;
        return horizontal
            ? !wallLike(logical, x, y - 1) && !wallLike(logical, x, y + 1)
            : !wallLike(logical, x - 1, y) && !wallLike(logical, x + 1, y);
    }

    private static void invalid(int x, int y, String reason) {
        String message = "문 배치 오류 (" + x + "," + y + "): " + reason;
        LOG.warning(message);
        throw new IllegalArgumentException(message);
    }
}

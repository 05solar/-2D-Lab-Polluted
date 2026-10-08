package io.github.some_example_name.world;

/** 완성된 논리 벽·문과 시각 타일의 접합 방향을 생성 직후 검사한다. */
public final class WallTopologyValidator {
    private static final int[] DX = {0, 1, 0, -1};
    private static final int[] DY = {1, 0, -1, 0};
    private static final int[] DIR = {WallAutotiler.NORTH, WallAutotiler.EAST,
        WallAutotiler.SOUTH, WallAutotiler.WEST};

    public void validateWallTopology(LaboratoryRoom room, RoomVisuals visuals) {
        WallAutotiler autotiler = new WallAutotiler();
        for (int y = 0; y < room.heightInTiles(); y++) {
            for (int x = 0; x < room.widthInTiles(); x++) {
                if (room.tileAt(x, y) != TileType.WALL) continue;
                int expected = autotiler.mask(room, x, y);
                if (expected == 0) fail(x, y, "단독 벽");
                String id = visualId(visuals, x, y);
                if (id == null) fail(x, y, "벽 시각 타일 누락");
                int actual = WallAutotiler.connectors(id);
                if (actual != expected)
                    fail(x, y, "벽 접합 불일치 " + id + " 예상=" + expected + " 실제=" + actual);
            }
        }
    }

    public void validateDoorPlacement(LaboratoryRoom room, RoomVisuals visuals) {
        for (int y = 0; y < room.heightInTiles(); y++) {
            for (int x = 0; x < room.widthInTiles(); x++) {
                Door door = room.doorAt(x, y);
                if (door == null) continue;
                int expected = door.orientation() == Door.Orientation.HORIZONTAL
                    ? WallAutotiler.EAST | WallAutotiler.WEST
                    : WallAutotiler.NORTH | WallAutotiler.SOUTH;
                if (new WallAutotiler().mask(room, x, y) != expected)
                    fail(x, y, "문 방향/주변 벽 불일치 " + door.orientation());
                if (!door.visualId().equals(visuals.structureAt(x, y)) || visuals.wallAt(x, y) != null)
                    fail(x, y, "문 타일 누락 또는 벽 중복");
                if (room.isSolid(x, y) != (door.state() != DoorState.OPEN))
                    fail(x, y, "문 상태/충돌 불일치");
                if (door.orientation() == Door.Orientation.HORIZONTAL) {
                    require(visuals, x - 1, y, "door_jamb_left");
                    require(visuals, x + 1, y, "door_jamb_right");
                } else {
                    require(visuals, x, y + 1, "vertical_door_jamb_top");
                    require(visuals, x, y - 1, "vertical_door_jamb_bottom");
                }
            }
        }
    }

    public void validateVisualConnections(LaboratoryRoom room, RoomVisuals visuals) {
        for (int y = 0; y < room.heightInTiles(); y++) {
            for (int x = 0; x < room.widthInTiles(); x++) {
                if (!wallLike(room, x, y)) continue;
                String id = visualId(visuals, x, y);
                if (id == null) fail(x, y, "접합 타일 없음");
                int own = WallAutotiler.connectors(id);
                for (int i = 0; i < 4; i++) {
                    int nx = x + DX[i], ny = y + DY[i];
                    boolean hasNeighbor = wallLike(room, nx, ny);
                    if (((own & DIR[i]) != 0) != hasNeighbor)
                        fail(x, y, id + " 방향 " + DIR[i] + " / 이웃 불일치 (" + nx + "," + ny + ")");
                    if (hasNeighbor) {
                        String nextId = visualId(visuals, nx, ny);
                        int opposite = DIR[(i + 2) % 4];
                        if (nextId == null || (WallAutotiler.connectors(nextId) & opposite) == 0)
                            fail(x, y, id + " ↔ (" + nx + "," + ny + ") " + nextId + " 접합 불일치");
                    }
                }
            }
        }
    }

    private static void require(RoomVisuals visuals, int x, int y, String id) {
        if (!id.equals(visuals.structureAt(x, y)) || visuals.wallAt(x, y) != null)
            fail(x, y, "문틀 누락·중복: " + id);
    }

    private static String visualId(RoomVisuals visuals, int x, int y) {
        String structure = visuals.structureAt(x, y);
        return structure != null ? structure : visuals.wallAt(x, y);
    }

    private static boolean wallLike(LaboratoryRoom room, int x, int y) {
        if (x < 0 || y < 0 || x >= room.widthInTiles() || y >= room.heightInTiles()) return false;
        return room.tileAt(x, y) != TileType.FLOOR;
    }

    private static void fail(int x, int y, String reason) {
        throw new IllegalStateException("벽 검증 실패 (" + x + "," + y + "): " + reason);
    }
}

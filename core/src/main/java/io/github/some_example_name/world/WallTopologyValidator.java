package io.github.some_example_name.world;

/** Validates logical wall/door topology against rendered connector metadata. */
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
                if (visualId(visuals, x, y) == null && !adjacentToFloor(room, x, y)) continue;
                int expected = autotiler.mask(room, x, y);
                if (expected == 0) fail(x, y, "isolated wall");
                String id = visualId(visuals, x, y);
                if (id == null) fail(x, y, "missing wall visual");
                int actual = WallAutotiler.connectors(id);
                if (actual != expected)
                    fail(x, y, "connector mask " + id + " expected=" + expected + " actual=" + actual);
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
                    fail(x, y, "door orientation/neighbors mismatch " + door.orientation());
                if (!door.visualId().equals(visuals.structureAt(x, y)) || visuals.wallAt(x, y) != null)
                    fail(x, y, "missing/duplicate door visual");
                if (room.isSolid(x, y) != (door.state() != DoorState.OPEN))
                    fail(x, y, "door state/collision mismatch");
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
                String id = visualId(visuals, x, y);
                if (!wallLike(room, x, y) || id == null) continue;
                int own = WallAutotiler.connectors(id);
                for (int i = 0; i < 4; i++) {
                    int nx = x + DX[i], ny = y + DY[i];
                    // Backing perimeter wall cells without a rendered tile are
                    // intentionally omitted from the atlas layer. Validate only
                    // visible-to-visible connector continuity here.
                    boolean logicalNeighbor = wallLike(room, nx, ny);
                    boolean visibleNeighbor = nx >= 0 && ny >= 0
                        && nx < room.widthInTiles() && ny < room.heightInTiles()
                        && visualId(visuals, nx, ny) != null;
                    if (logicalNeighbor && !visibleNeighbor) continue;
                    boolean hasNeighbor = logicalNeighbor;
                    if (((own & DIR[i]) != 0) != hasNeighbor)
                        fail(x, y, id + " dir=" + DIR[i] + " neighbor=" + nx + "," + ny
                            + " type=" + room.tileAt(nx, ny) + " visual=" + visualId(visuals, nx, ny));
                    if (hasNeighbor) {
                        String nextId = visualId(visuals, nx, ny);
                        int opposite = DIR[(i + 2) % 4];
                        if (nextId == null || (WallAutotiler.connectors(nextId) & opposite) == 0)
                            fail(x, y, id + " <-> (" + nx + "," + ny + ") " + nextId + " mismatch");
                    }
                }
            }
        }
    }

    private static void require(RoomVisuals visuals, int x, int y, String id) {
        if (!id.equals(visuals.structureAt(x, y)) || visuals.wallAt(x, y) != null)
            fail(x, y, "missing/duplicate jamb: " + id);
    }

    private static String visualId(RoomVisuals visuals, int x, int y) {
        String structure = visuals.structureAt(x, y);
        return structure != null ? structure : visuals.wallAt(x, y);
    }

    private static boolean wallLike(LaboratoryRoom room, int x, int y) {
        if (x < 0 || y < 0 || x >= room.widthInTiles() || y >= room.heightInTiles()) return false;
        TileType tile = room.tileAt(x, y);
        return tile == TileType.WALL || tile == TileType.DOOR_CLOSED || room.doorAt(x, y) != null;
    }

    private static boolean adjacentToFloor(LaboratoryRoom room, int x, int y) {
        return room.tileAt(x + 1, y) == TileType.FLOOR || room.tileAt(x - 1, y) == TileType.FLOOR
            || room.tileAt(x, y + 1) == TileType.FLOOR || room.tileAt(x, y - 1) == TileType.FLOOR;
    }

    private static void fail(int x, int y, String reason) {
        throw new IllegalStateException("wall validation failed (" + x + "," + y + "): " + reason);
    }
}

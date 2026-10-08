package io.github.some_example_name.world;

/**
 * Four visual tile ID layers indexed by [y][x]. Floor is always present; overlay,
 * wall, and structure are optional. Door structure IDs follow the shared Door state.
 */
public class RoomVisuals {

    private final String[][] floor;
    private final String[][] overlay;
    private final String[][] wall;
    private final String[][] structure;
    private final Door[][] doors;
    private final int width;
    private final int height;

    public RoomVisuals(String[][] floor, String[][] overlay, String[][] wall, String[][] structure) {
        this(floor, overlay, wall, structure, null);
    }

    public RoomVisuals(String[][] floor, String[][] overlay, String[][] wall, String[][] structure,
                       Door[][] doors) {
        this.floor = floor;
        this.overlay = overlay;
        this.wall = wall;
        this.structure = structure;
        this.doors = doors;
        this.height = floor.length;
        this.width = floor[0].length;
    }

    public String floorAt(int tx, int ty)     { return floor[ty][tx]; }
    public String overlayAt(int tx, int ty)   { return overlay[ty][tx]; }
    public String wallAt(int tx, int ty)      { return wall[ty][tx]; }
    public String structureAt(int tx, int ty) {
        Door door = doors == null ? null : doors[ty][tx];
        return door == null ? structure[ty][tx] : door.visualId();
    }

    public int widthInTiles()  { return width; }
    public int heightInTiles() { return height; }

    /** 특정 레이어에서 해당 타일 ID가 몇 번 쓰였는지(진단/검증용). */
    public int count(String layer, String id) {
        if ("structure".equals(layer)) {
            int result = 0;
            for (int y = 0; y < height; y++)
                for (int x = 0; x < width; x++)
                    if (id.equals(structureAt(x, y))) result++;
            return result;
        }
        String[][] a = layerArray(layer);
        int n = 0;
        for (String[] row : a) {
            for (String v : row) {
                if (id.equals(v)) n++;
            }
        }
        return n;
    }

    private String[][] layerArray(String layer) {
        switch (layer) {
            case "floor":     return floor;
            case "overlay":   return overlay;
            case "wall":      return wall;
            case "structure": return structure;
            default: throw new IllegalArgumentException("알 수 없는 레이어: " + layer);
        }
    }
}

package io.github.some_example_name.world;

/**
 * Selects one wall tile ID from cardinal wall/door neighbors in world y-up coordinates.
 * North is y+1. Diagonals distinguish inner from outer corners; connector masks
 * describe the actual metal body edges in the generated atlas.
 */
public class WallAutotiler {

    public static final int NORTH = 1, EAST = 2, SOUTH = 4, WEST = 8;

    public enum Shape {
        HORIZONTAL, VERTICAL,
        END_LEFT, END_RIGHT, END_TOP, END_BOTTOM,
        OUTER_NW, OUTER_NE, OUTER_SW, OUTER_SE,
        INNER_NW, INNER_NE, INNER_SW, INNER_SE,
        T_OPEN_NORTH, T_OPEN_SOUTH, T_OPEN_EAST, T_OPEN_WEST,
        CROSS
    }

    public String tileId(LaboratoryRoom room, int tx, int ty) {
        Shape shape = classify(room, tx, ty);
        // 직선에서만 약 1/8을 파손 변형으로 쓴다. 좌표 기반이라 실행마다 동일하며
        // 끝·모서리·접합부·문은 방향별 전용 타일을 유지한다.
        if ((shape == Shape.HORIZONTAL || shape == Shape.VERTICAL)
                && Math.floorMod(tx * 17 + ty * 31, 8) == 0) {
            return shape == Shape.HORIZONTAL ? "wall_horizontal_damaged" : "wall_vertical_damaged";
        }
        return idFor(shape);
    }

    public Shape classify(LaboratoryRoom room, int tx, int ty) {
        int mask = mask(room, tx, ty);
        boolean n = (mask & NORTH) != 0;
        boolean s = (mask & SOUTH) != 0;
        boolean e = (mask & EAST) != 0;
        boolean w = (mask & WEST) != 0;
        int cnt = Integer.bitCount(mask);

        if (cnt == 4) {
            // 대각이 열려도 직교 연결 4개를 잃으면 안 된다.
            return Shape.CROSS;
        }
        if (cnt == 3) {
            if (!n) return Shape.T_OPEN_NORTH; // 북쪽만 열림
            if (!s) return Shape.T_OPEN_SOUTH;
            if (!e) return Shape.T_OPEN_EAST;
            return Shape.T_OPEN_WEST;          // !w
        }
        if (cnt == 2) {
            if (n && s) return Shape.VERTICAL;
            if (e && w) return Shape.HORIZONTAL;
            // 두 팔 사이의 대각도 벽이면 안쪽 모서리, 비어 있으면 바깥 모서리.
            if (n && e) return connected(room, tx + 1, ty + 1) ? Shape.INNER_SW : Shape.OUTER_SW;
            if (n && w) return connected(room, tx - 1, ty + 1) ? Shape.INNER_SE : Shape.OUTER_SE;
            if (s && e) return connected(room, tx + 1, ty - 1) ? Shape.INNER_NW : Shape.OUTER_NW;
            return connected(room, tx - 1, ty - 1) ? Shape.INNER_NE : Shape.OUTER_NE;
        }
        if (cnt == 1) {
            if (n) return Shape.END_BOTTOM;    // 벽이 위에서 와서 이 셀 아래에서 끝
            if (s) return Shape.END_TOP;
            if (e) return Shape.END_LEFT;
            return Shape.END_RIGHT;            // w
        }
        throw new IllegalArgumentException("연결 없는 단독 벽 (" + tx + "," + ty + ")");
    }

    public int mask(LaboratoryRoom room, int tx, int ty) {
        int mask = 0;
        if (connected(room, tx, ty + 1)) mask |= NORTH;
        if (connected(room, tx + 1, ty)) mask |= EAST;
        if (connected(room, tx, ty - 1)) mask |= SOUTH;
        if (connected(room, tx - 1, ty)) mask |= WEST;
        return mask;
    }

    /** 타일 이미지가 가진 실제 접합 방향. 파생 PNG의 금속 본체가 이 방향의 셀 경계에 닿는다. */
    public static int connectors(String id) {
        if (id == null) return 0;
        switch (id) {
            case "wall_horizontal": case "wall_horizontal_damaged":
            case "wall_support_horizontal": case "wall_console": case "wall_breach_horizontal":
            case "door_jamb_left": case "door_jamb_right":
            case "horizontal_door_closed": case "horizontal_door_open": return EAST | WEST;
            case "wall_vertical": case "wall_vertical_damaged":
            case "wall_support_vertical": case "wall_breach_vertical":
            case "vertical_door_jamb_top": case "vertical_door_jamb_bottom":
            case "vertical_door_closed": case "vertical_door_open": return NORTH | SOUTH;
            case "wall_end_left": return EAST;
            case "wall_end_right": return WEST;
            case "wall_end_top": return SOUTH;
            case "wall_end_bottom": return NORTH;
            case "wall_outer_nw": case "wall_inner_nw": return EAST | SOUTH;
            case "wall_outer_ne": case "wall_inner_ne": return WEST | SOUTH;
            case "wall_outer_sw": case "wall_inner_sw": return EAST | NORTH;
            case "wall_outer_se": case "wall_inner_se": return WEST | NORTH;
            case "wall_t_open_north": return EAST | SOUTH | WEST;
            case "wall_t_open_south": return NORTH | EAST | WEST;
            case "wall_t_open_east": return NORTH | SOUTH | WEST;
            case "wall_t_open_west": return NORTH | EAST | SOUTH;
            case "wall_cross": return NORTH | EAST | SOUTH | WEST;
            default: throw new IllegalArgumentException("벽 접합 메타데이터 없음: " + id);
        }
    }

    String idFor(Shape shape) {
        switch (shape) {
            case HORIZONTAL:   return "wall_horizontal";
            case VERTICAL:     return "wall_vertical";
            case END_LEFT:     return "wall_end_left";
            case END_RIGHT:    return "wall_end_right";
            case END_TOP:      return "wall_end_top";
            case END_BOTTOM:   return "wall_end_bottom";
            case OUTER_NW:     return "wall_outer_nw";
            case OUTER_NE:     return "wall_outer_ne";
            case OUTER_SW:     return "wall_outer_sw";
            case OUTER_SE:     return "wall_outer_se";
            case INNER_NW:     return "wall_inner_nw";
            case INNER_NE:     return "wall_inner_ne";
            case INNER_SW:     return "wall_inner_sw";
            case INNER_SE:     return "wall_inner_se";
            case T_OPEN_NORTH: return "wall_t_open_north";
            case T_OPEN_SOUTH: return "wall_t_open_south";
            case T_OPEN_EAST:  return "wall_t_open_east";
            case T_OPEN_WEST:  return "wall_t_open_west";
            case CROSS:
            default:           return "wall_cross";
        }
    }

    /** 이웃이 벽/문이면 연결. 범위 밖은 비연결(외곽 벽이 직선으로 이어지도록). */
    private boolean connected(LaboratoryRoom room, int tx, int ty) {
        if (tx < 0 || ty < 0 || tx >= room.widthInTiles() || ty >= room.heightInTiles()) {
            return false;
        }
        TileType t = room.tileAt(tx, ty);
        return t == TileType.WALL || t == TileType.DOOR_CLOSED || t == TileType.DOORWAY_OPEN;
    }
}

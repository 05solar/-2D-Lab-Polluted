package io.github.some_example_name.world;

/**
 * 논리 벽 셀('WALL')의 시각 타일 ID를 이웃을 검사해 결정한다(Laboratory Tileset V2).
 * 충돌(논리)과 무관하게 "어떻게 보일지"만 정한다. 렌더러는 이 ID를 그리기만 한다.
 *
 * 연결(connected) 기준: 이웃이 벽 또는 문(닫힘/열림)일 때. <b>범위 밖은 비연결</b>로 본다
 * (그래야 외곽 벽이 T/십자로 튀지 않고 직선·모서리로 이어진다). y는 위로 증가(ty+1=북).
 *
 * 지원 형태(모두 V2 타일셋에 실제 타일이 있다):
 *   직선 wall_horizontal/vertical, 끝 wall_end_left/right/top/bottom,
 *   바깥 모서리 wall_outer_nw/ne/sw/se, 안쪽 모서리 wall_inner_nw/ne/sw/se,
 *   T자 wall_t_open_north/south/east/west(= 그 방향이 열린 접합), 십자 wall_cross.
 *
 * 문 우선순위: 문('DOOR_CLOSED'/'DOORWAY_OPEN')은 WALL이 아니므로 오토타일 대상이 아니다.
 * 호출부({@link LaboratoryLayout})가 WALL 셀에만 적용하므로 문 타일을 덮어쓰지 않는다.
 * 인접 벽은 문을 "연결"로 보아 문 쪽으로 자연스럽게 이어진다.
 */
public class WallAutotiler {

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
        boolean n = connected(room, tx, ty + 1);
        boolean s = connected(room, tx, ty - 1);
        boolean e = connected(room, tx + 1, ty);
        boolean w = connected(room, tx - 1, ty);
        int cnt = (n ? 1 : 0) + (s ? 1 : 0) + (e ? 1 : 0) + (w ? 1 : 0);

        if (cnt == 4) {
            // 네 방향 벽 + 한 대각이 열림 → 안쪽 모서리(그 대각으로 바닥이 파고듦).
            if (!connected(room, tx + 1, ty + 1)) return Shape.INNER_NE;
            if (!connected(room, tx - 1, ty + 1)) return Shape.INNER_NW;
            if (!connected(room, tx + 1, ty - 1)) return Shape.INNER_SE;
            if (!connected(room, tx - 1, ty - 1)) return Shape.INNER_SW;
            return Shape.CROSS; // 완전히 둘러싸임(희귀) — 십자로 대체
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
            // 인접 두 방향 → 바깥 모서리(팔꿈치는 두 팔의 반대편).
            if (n && e) return Shape.OUTER_SW;
            if (n && w) return Shape.OUTER_SE;
            if (s && e) return Shape.OUTER_NW;
            return Shape.OUTER_NE;             // s && w
        }
        if (cnt == 1) {
            if (n) return Shape.END_BOTTOM;    // 벽이 위에서 와서 이 셀 아래에서 끝
            if (s) return Shape.END_TOP;
            if (e) return Shape.END_LEFT;
            return Shape.END_RIGHT;            // w
        }
        return Shape.CROSS; // 고립(희귀)
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

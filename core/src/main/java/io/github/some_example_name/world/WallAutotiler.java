package io.github.some_example_name.world;

/**
 * 논리 벽 셀('#')의 시각 타일을 이웃을 검사해 선택한다.
 * 충돌(논리)과 무관하게 "어떻게 보일지"만 결정하므로 벽 시각 방향이 바뀌어도 충돌 셀은 동일하다.
 *
 * 순수 Java. "open(통과 가능)" 기준: 이웃이 벽/닫힌 문이 아닌 셀(범위 밖은 벽으로 취급).
 * y는 위로 증가한다(ty+1 = 북).
 *
 * <h3>벽 형태 분류({@link Shape})</h3>
 * 스펙이 요구하는 벽 상태(직선/끝단, 바깥·안쪽 모서리, 분리벽, T자·십자 접합, 문측벽)를
 * {@link #classify}가 먼저 구분한다. 다만 현재 타일셋(16칸)은 방향 벽 4종 + 바깥 모서리 4종만
 * 제공하고 T자·십자·안쪽 모서리·문 전용 벽 타일이 없다. 지원하지 않는 형태는
 * <b>회전 없이 가장 가까운 자연스러운 타일</b>로 매핑한다({@link #tileFor}).
 *   - 분리벽(세로/가로): 가장 가까운 방향 벽 타일로 대체.
 *   - T자/십자 접합: 닫힌 변 기준으로 가장 가까운 방향 벽 타일로 대체.
 *   - 안쪽 모서리: 전용 타일이 없어 방향 벽으로 대체.
 *
 * <h3>문 우선순위</h3>
 * 닫힌 문('D')/열린 출입구('O')는 논리상 WALL이 아니므로 오토타일 대상이 아니다.
 * 즉 문 타일은 벽 오토타일이 절대 덮어쓰지 않는다(호출부 {@link LaboratoryLayout}가 WALL 셀에만 적용).
 * 문 바로 옆 벽은 {@link #classify}가 {@code doorSide}로 표시하지만, 전용 문측벽 타일이 없어
 * 기하 형태에 맞는 방향 벽 타일을 그대로 쓴다.
 */
public class WallAutotiler {

    /** 벽 셀의 기하 형태. 타일셋이 지원하지 않는 형태도 구분만 해 두고 가장 가까운 타일로 매핑한다. */
    public enum Shape {
        EDGE_NORTH, EDGE_SOUTH, EDGE_WEST, EDGE_EAST,     // 직선 벽(방 쪽이 한 방향)
        CORNER_NW, CORNER_NE, CORNER_SW, CORNER_SE,       // 바깥 모서리(두 방향 인접 열림)
        DIVIDER_VERTICAL, DIVIDER_HORIZONTAL,             // 얇은 분리벽(반대 두 방향 열림)
        T_FROM_NORTH, T_FROM_SOUTH, T_FROM_WEST, T_FROM_EAST, // T자 접합(세 방향 열림)
        CROSS,                                            // 십자 접합(네 방향 열림)
        INNER_CORNER, ENCLOSED;                           // 안쪽 모서리 / 완전히 둘러싸임
    }

    public TileVisual pick(LaboratoryRoom room, int tx, int ty) {
        return tileFor(classify(room, tx, ty));
    }

    /** 이웃 패턴을 보고 벽 형태를 분류한다(타일 선택 전 단계, 테스트/디버그에 사용). */
    public Shape classify(LaboratoryRoom room, int tx, int ty) {
        boolean n = isOpen(room, tx, ty + 1);
        boolean s = isOpen(room, tx, ty - 1);
        boolean e = isOpen(room, tx + 1, ty);
        boolean w = isOpen(room, tx - 1, ty);
        int open = (n ? 1 : 0) + (s ? 1 : 0) + (e ? 1 : 0) + (w ? 1 : 0);

        // 직선 벽: 한 방향만 열림 → 그 방향이 방 내부.
        if (open == 1) {
            if (s) return Shape.EDGE_NORTH;
            if (n) return Shape.EDGE_SOUTH;
            if (e) return Shape.EDGE_WEST;
            return Shape.EDGE_EAST; // w
        }

        // 두 방향 열림: 인접(바깥 모서리) 또는 반대(얇은 분리벽).
        if (open == 2) {
            if (s && e) return Shape.CORNER_NW;
            if (s && w) return Shape.CORNER_NE;
            if (n && e) return Shape.CORNER_SW;
            if (n && w) return Shape.CORNER_SE;
            if (e && w) return Shape.DIVIDER_VERTICAL;   // 세로 분리벽
            return Shape.DIVIDER_HORIZONTAL;             // 가로 분리벽 (n && s)
        }

        // 네 방향 모두 벽(open==0): 벽 셀이 방의 바깥 모서리다. 열린 대각으로 방향을 판정.
        if (open == 0) {
            if (isOpen(room, tx + 1, ty - 1)) return Shape.CORNER_NW; // SE 대각 열림
            if (isOpen(room, tx - 1, ty - 1)) return Shape.CORNER_NE; // SW 대각 열림
            if (isOpen(room, tx + 1, ty + 1)) return Shape.CORNER_SW; // NE 대각 열림
            if (isOpen(room, tx - 1, ty + 1)) return Shape.CORNER_SE; // NW 대각 열림
            return Shape.ENCLOSED;                                    // 완전히 둘러싸인 내부 벽
        }

        // 네 방향 열림 = 십자 접합(고립된 단일 벽 기둥).
        if (open == 4) return Shape.CROSS;

        // 세 방향 열림 = T자 접합. 닫힌 한 변을 기준으로 분류.
        if (!n) return Shape.T_FROM_NORTH; // 북쪽만 닫힘
        if (!s) return Shape.T_FROM_SOUTH;
        if (!e) return Shape.T_FROM_WEST;
        return Shape.T_FROM_EAST;          // !w
    }

    /** 문 바로 옆(수평/수직 인접이 닫힌 문)인지. 전용 타일은 없지만 분류/테스트 용도로 노출. */
    public boolean isDoorSide(LaboratoryRoom room, int tx, int ty) {
        return room.tileAt(tx + 1, ty) == TileType.DOOR_CLOSED
            || room.tileAt(tx - 1, ty) == TileType.DOOR_CLOSED
            || room.tileAt(tx, ty + 1) == TileType.DOOR_CLOSED
            || room.tileAt(tx, ty - 1) == TileType.DOOR_CLOSED;
    }

    /**
     * 형태 → 타일. 타일셋에 전용 타일이 없는 형태(분리벽/T자/십자/안쪽 모서리)는
     * 가장 가까운 방향 벽/모서리 타일로 대체한다(회전 없음).
     */
    TileVisual tileFor(Shape shape) {
        switch (shape) {
            case EDGE_NORTH: return TileVisual.WALL_NORTH;
            case EDGE_SOUTH: return TileVisual.WALL_SOUTH;
            case EDGE_WEST:  return TileVisual.WALL_WEST;
            case EDGE_EAST:  return TileVisual.WALL_EAST;
            case CORNER_NW:  return TileVisual.WALL_CORNER_NW;
            case CORNER_NE:  return TileVisual.WALL_CORNER_NE;
            case CORNER_SW:  return TileVisual.WALL_CORNER_SW;
            case CORNER_SE:  return TileVisual.WALL_CORNER_SE;
            // --- 전용 타일 없음: 가장 가까운 타일로 대체 ---
            case DIVIDER_VERTICAL:   return TileVisual.WALL_EAST;   // 세로벽 근사
            case DIVIDER_HORIZONTAL: return TileVisual.WALL_SOUTH;  // 가로벽 근사
            case T_FROM_NORTH:       return TileVisual.WALL_SOUTH;  // 북쪽 막힘 → 남향 직선벽
            case T_FROM_SOUTH:       return TileVisual.WALL_NORTH;
            case T_FROM_WEST:        return TileVisual.WALL_WEST;
            case T_FROM_EAST:        return TileVisual.WALL_EAST;
            case CROSS:              return TileVisual.WALL_EAST;   // 십자: 세로벽으로 근사
            case INNER_CORNER:       return TileVisual.WALL_NORTH;  // 안쪽 모서리 근사
            case ENCLOSED:
            default:                 return TileVisual.WALL_NORTH;
        }
    }

    private boolean isOpen(LaboratoryRoom room, int tileX, int tileY) {
        TileType t = room.tileAt(tileX, tileY); // 범위 밖은 WALL 반환
        return t != TileType.WALL && t != TileType.DOOR_CLOSED;
    }
}

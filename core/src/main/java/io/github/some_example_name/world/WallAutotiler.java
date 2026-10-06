package io.github.some_example_name.world;

/**
 * 논리 벽 셀의 시각 타일(직선 벽/모서리)을 이웃을 검사해 선택한다.
 * 충돌(논리)과 무관하게 "어떻게 보일지"만 결정하므로 벽 시각 방향이 바뀌어도 충돌 셀은 동일하다.
 *
 * 순수 Java. "open(통과 가능)" 기준: 이웃이 벽/닫힌 문이 아닌 셀(범위 밖은 벽으로 취급).
 * y는 위로 증가한다(ty+1 = 북).
 */
public class WallAutotiler {

    public TileVisual pick(LaboratoryRoom room, int tx, int ty) {
        boolean n = isOpen(room, tx, ty + 1);
        boolean s = isOpen(room, tx, ty - 1);
        boolean e = isOpen(room, tx + 1, ty);
        boolean w = isOpen(room, tx - 1, ty);
        int open = (n ? 1 : 0) + (s ? 1 : 0) + (e ? 1 : 0) + (w ? 1 : 0);

        // 직선 벽: 한 방향만 열림 → 그 방향이 방 내부.
        if (open == 1) {
            if (s) return TileVisual.WALL_NORTH;
            if (n) return TileVisual.WALL_SOUTH;
            if (e) return TileVisual.WALL_WEST;
            return TileVisual.WALL_EAST; // w
        }

        // 두 방향 열림: 인접(모서리) 또는 반대(얇은 분리벽).
        if (open == 2) {
            if (s && e) return TileVisual.WALL_CORNER_NW;
            if (s && w) return TileVisual.WALL_CORNER_NE;
            if (n && e) return TileVisual.WALL_CORNER_SW;
            if (n && w) return TileVisual.WALL_CORNER_SE;
            if (e && w) return TileVisual.WALL_EAST;   // 세로 분리벽 기본 방향
            return TileVisual.WALL_SOUTH;              // 가로 분리벽 기본 방향 (n && s)
        }

        // 네 방향 모두 벽: 외곽 모서리. 대각선 내부로 방향 판정.
        if (open == 0) {
            if (isOpen(room, tx + 1, ty - 1)) return TileVisual.WALL_CORNER_NW; // SE 대각 열림
            if (isOpen(room, tx - 1, ty - 1)) return TileVisual.WALL_CORNER_NE; // SW 대각 열림
            if (isOpen(room, tx + 1, ty + 1)) return TileVisual.WALL_CORNER_SW; // NE 대각 열림
            if (isOpen(room, tx - 1, ty + 1)) return TileVisual.WALL_CORNER_SE; // NW 대각 열림
            return TileVisual.WALL_NORTH; // 완전히 둘러싸인 내부 벽
        }

        // 세 방향 이상 열림(벽 끝/돌출): 닫힌(벽에 붙은) 쪽을 기준으로 직선 벽.
        if (!n) return TileVisual.WALL_SOUTH;
        if (!s) return TileVisual.WALL_NORTH;
        if (!e) return TileVisual.WALL_WEST;
        return TileVisual.WALL_EAST; // !w
    }

    private boolean isOpen(LaboratoryRoom room, int tileX, int tileY) {
        TileType t = room.tileAt(tileX, tileY); // 범위 밖은 WALL 반환
        return t != TileType.WALL && t != TileType.DOOR_CLOSED;
    }
}

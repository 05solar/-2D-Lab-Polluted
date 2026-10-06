package io.github.some_example_name.world;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * 벽 오토타일: 방향 벽/바깥 모서리 매핑, 세로 분리벽, 문 양옆 벽(문 미덮어쓰기), T자 분류.
 * 타일셋이 지원하지 않는 형태(분리벽/T자)는 가장 가까운 방향 벽 타일로 대체됨을 함께 확인한다.
 */
public class WallAutotilerTest {

    private final WallAutotiler autotiler = new WallAutotiler();

    private static LaboratoryRoom room(String... rows) {
        return LaboratoryLayout.fromRows(rows).room();
    }

    private static RoomVisuals visuals(String... rows) {
        return LaboratoryLayout.fromRows(rows).visuals();
    }

    @Test
    public void directionalEdgesAndOuterCorners() {
        String[] box = {
            "#####",
            "#...#",
            "#...#",
            "#...#",
            "#####",
        };
        RoomVisuals v = visuals(box);
        // 직선 벽: 방 안쪽을 향한 방향
        assertEquals(TileVisual.WALL_NORTH, v.visualAt(2, 4)); // 상단
        assertEquals(TileVisual.WALL_SOUTH, v.visualAt(2, 0)); // 하단
        assertEquals(TileVisual.WALL_WEST, v.visualAt(0, 2));  // 좌측
        assertEquals(TileVisual.WALL_EAST, v.visualAt(4, 2));  // 우측
        // 바깥 모서리
        assertEquals(TileVisual.WALL_CORNER_SW, v.visualAt(0, 0));
        assertEquals(TileVisual.WALL_CORNER_SE, v.visualAt(4, 0));
        assertEquals(TileVisual.WALL_CORNER_NW, v.visualAt(0, 4));
        assertEquals(TileVisual.WALL_CORNER_NE, v.visualAt(4, 4));
    }

    @Test
    public void verticalDividerMapsToNearestVerticalWall() {
        // 양옆이 모두 바닥인 세로 분리벽 → 전용 타일 없음 → 가장 가까운 세로벽(WALL_EAST).
        String[] rows = {
            "#####",
            "#.#.#",
            "#.#.#",
            "#.#.#",
            "#####",
        };
        LaboratoryRoom r = room(rows);
        assertEquals(WallAutotiler.Shape.DIVIDER_VERTICAL, autotiler.classify(r, 2, 2));
        assertEquals(TileVisual.WALL_EAST, visuals(rows).visualAt(2, 2));
    }

    @Test
    public void doorIsNotOverwrittenAndAdjacentWallsAreDoorSide() {
        String[] rows = {
            "#####",
            "#.#.#",
            "#.D.#",   // 세로벽(tx2) 중간에 닫힌 문
            "#.#.#",
            "#####",
        };
        LaboratoryRoom r = room(rows);
        RoomVisuals v = visuals(rows);

        // 문 타일은 오토타일이 덮어쓰지 않는다.
        assertEquals(TileType.DOOR_CLOSED, r.tileAt(2, 2));
        assertEquals(TileVisual.DOOR_CLOSED, v.visualAt(2, 2));

        // 문 위/아래 벽은 문측벽으로 표시되며, 전용 타일이 없어 세로벽으로 대체된다.
        assertTrue("문 위 벽은 문측", autotiler.isDoorSide(r, 2, 3));
        assertTrue("문 아래 벽은 문측", autotiler.isDoorSide(r, 2, 1));
        assertEquals(TileVisual.WALL_EAST, v.visualAt(2, 3));
        assertEquals(TileVisual.WALL_EAST, v.visualAt(2, 1));
    }

    @Test
    public void wallTipClassifiesAsTJunction() {
        // 아래에서 솟아 끝이 열린 벽 끝(세 방향 바닥) → T자(닫힌 변=남쪽) → 북향 직선벽으로 대체.
        String[] rows = {
            "#######",
            "#.....#",
            "#..#..#",
            "#..#..#",
            "#######",
        };
        LaboratoryRoom r = room(rows);
        assertEquals(WallAutotiler.Shape.T_FROM_SOUTH, autotiler.classify(r, 3, 2));
        assertEquals(TileVisual.WALL_NORTH, visuals(rows).visualAt(3, 2));
    }
}

package io.github.some_example_name.world;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.badlogic.gdx.math.Vector2;
import org.junit.Test;

/**
 * V2 벽 오토타일: 직선/바깥 모서리/끝/T자/안쪽 모서리 매핑과 문 미덮어쓰기.
 */
public class WallAutotilerTest {

    private final WallAutotiler autotiler = new WallAutotiler();

    private static RoomVisuals visuals(String... rows) {
        return LaboratoryLayout.fromRows(rows).visuals();
    }
    private static LaboratoryRoom room(String... rows) {
        return LaboratoryLayout.fromRows(rows).room();
    }

    @Test
    public void straightEdgesAndOuterCorners() {
        RoomVisuals v = visuals(
            "#####",
            "#...#",
            "#...#",
            "#...#",
            "#####");
        assertEquals("wall_horizontal", v.wallAt(2, 4));
        assertEquals("wall_horizontal", v.wallAt(2, 0));
        assertEquals("wall_vertical", v.wallAt(0, 2));
        assertEquals("wall_vertical", v.wallAt(4, 2));
        assertEquals("wall_outer_sw", v.wallAt(0, 0));
        assertEquals("wall_outer_se", v.wallAt(4, 0));
        assertEquals("wall_outer_nw", v.wallAt(0, 4));
        assertEquals("wall_outer_ne", v.wallAt(4, 4));
    }

    @Test
    public void tJunctionsWhereInteriorWallMeetsBorder() {
        String[] rows = {
            "#####",
            "#.#.#",
            "#.#.#",
            "#.#.#",
            "#####"};
        LaboratoryRoom r = room(rows);
        RoomVisuals v = visuals(rows);
        assertEquals(WallAutotiler.Shape.T_OPEN_NORTH, autotiler.classify(r, 2, 4));
        assertEquals("wall_t_open_north", v.wallAt(2, 4));
        assertEquals(WallAutotiler.Shape.T_OPEN_SOUTH, autotiler.classify(r, 2, 0));
        assertEquals("wall_t_open_south", v.wallAt(2, 0));
        // 세로 분리벽 가운데는 수직 벽
        assertEquals("wall_vertical", v.wallAt(2, 2));
    }

    @Test
    public void wallEndCaps() {
        // 왼쪽 벽이 중간에서 끝나는 가로 벽(오른쪽으로만 연결) → wall_end_left
        String[] rows = {
            "#####",
            "#...#",
            "#.###", // (2,2)(3,2) 벽, 왼쪽 끝은 (2,2)
            "#...#",
            "#####"};
        // (2,2): N=(2,3)floor, S=(2,1)floor, E=(3,2)wall, W=(1,2)floor → 1개(E) → END_LEFT
        LaboratoryRoom r = room(rows);
        assertEquals(WallAutotiler.Shape.END_LEFT, autotiler.classify(r, 2, 2));
        assertEquals("wall_end_left", visuals(rows).wallAt(2, 2));
    }

    @Test
    public void doorNotOverwrittenAndMatchesDirection() {
        String[] rows = {
            "#####",
            "#.#.#",
            "#.d.#",   // 세로 닫힌 문
            "#.#.#",
            "#####"};
        RoomVisuals v = visuals(rows);
        LaboratoryRoom r = room(rows);
        assertNull("문 셀엔 벽 타일 없음", v.wallAt(2, 2));
        assertEquals("vertical_door_closed", v.structureAt(2, 2));
        assertEquals(TileType.DOOR_CLOSED, r.tileAt(2, 2));
        // 문 위/아래 벽은 세로벽으로 이어진다
        assertEquals("wall_vertical", v.wallAt(2, 3));
        assertEquals("wall_vertical", v.wallAt(2, 1));
    }

    @Test
    public void innerCornerFromDiagonalOpening() {
        // 가운데 벽 셀: N,S,E,W 모두 벽, NE 대각만 바닥 → 안쪽 모서리 NE
        TileType W = TileType.WALL, F = TileType.FLOOR;
        TileType[][] t = {
            {W, W, W},  // ty0
            {W, W, W},  // ty1 (center row)
            {W, W, F},  // ty2: NE diagonal of (1,1) = (2,2) = FLOOR
        };
        LaboratoryRoom r = new LaboratoryRoom(t, new Vector2(2.5f, 2.5f));
        assertEquals(WallAutotiler.Shape.INNER_NE, autotiler.classify(r, 1, 1));
        assertEquals("wall_inner_ne", autotiler.tileId(r, 1, 1));
    }
}

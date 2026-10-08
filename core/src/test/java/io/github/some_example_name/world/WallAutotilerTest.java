package io.github.some_example_name.world;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
        // 세로 분리벽 가운데는 수직 벽(고정 좌표 변형으로 파손형).
        assertEquals(WallAutotiler.Shape.VERTICAL, autotiler.classify(r, 2, 2));
        assertEquals("wall_vertical_damaged", v.wallAt(2, 2));
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
            ".......",
            "...#...",
            "...#...",
            "...d...",
            "...#...",
            "...#...",
            "......."};
        RoomVisuals v = visuals(rows);
        LaboratoryRoom r = room(rows);
        assertNull("문 셀엔 벽 타일 없음", v.wallAt(3, 3));
        assertEquals("vertical_door_closed", v.structureAt(3, 3));
        assertEquals(TileType.DOOR_CLOSED, r.tileAt(3, 3));
        assertEquals("vertical_door_jamb_top", v.structureAt(3, 4));
        assertEquals("vertical_door_jamb_bottom", v.structureAt(3, 2));
        assertNull(v.wallAt(3, 4));
        assertNull(v.wallAt(3, 2));
        assertEquals("wall_end_top", v.wallAt(3, 5));
        assertEquals("wall_end_bottom", v.wallAt(3, 1));
    }

    @Test
    public void innerCornerFromFilledDiagonal() {
        // S+W 팔과 SW 대각이 차면 안쪽 NE 모서리. 네 팔이면 항상 십자다.
        TileType W = TileType.WALL, F = TileType.FLOOR;
        TileType[][] t = {
            {W, W, F},
            {W, W, F},
            {F, F, F},
        };
        LaboratoryRoom r = new LaboratoryRoom(t, new Vector2(2.5f, 2.5f));
        assertEquals(WallAutotiler.Shape.INNER_NE, autotiler.classify(r, 1, 1));
        assertEquals("wall_inner_ne", autotiler.tileId(r, 1, 1));
    }

    @Test
    public void allEndCapsAndTJunctionsHaveDirectionalTiles() {
        TileType W = TileType.WALL, F = TileType.FLOOR;
        int[][] arms = {{1, 2}, {2, 1}, {1, 0}, {0, 1}}; // N E S W
        WallAutotiler.Shape[] ends = {WallAutotiler.Shape.END_BOTTOM, WallAutotiler.Shape.END_LEFT,
            WallAutotiler.Shape.END_TOP, WallAutotiler.Shape.END_RIGHT};
        WallAutotiler.Shape[] tees = {WallAutotiler.Shape.T_OPEN_NORTH, WallAutotiler.Shape.T_OPEN_EAST,
            WallAutotiler.Shape.T_OPEN_SOUTH, WallAutotiler.Shape.T_OPEN_WEST};
        for (int omit = 0; omit < 4; omit++) {
            TileType[][] end = {{F, F, F}, {F, W, F}, {F, F, F}};
            end[arms[omit][1]][arms[omit][0]] = W;
            assertEquals(ends[omit], autotiler.classify(new LaboratoryRoom(end, new Vector2()), 1, 1));

            TileType[][] tee = {{F, W, F}, {W, W, W}, {F, W, F}};
            tee[arms[omit][1]][arms[omit][0]] = F;
            assertEquals(tees[omit], autotiler.classify(new LaboratoryRoom(tee, new Vector2()), 1, 1));
        }
    }

    @Test
    public void allInnerCornersAndCrossHaveDirectionalTiles() {
        TileType W = TileType.WALL, F = TileType.FLOOR;
        int[][] armsA = {{1, 0}, {1, 0}, {1, 2}, {1, 2}}; // NW,NE: S / SW,SE: N
        int[][] armsB = {{2, 1}, {0, 1}, {2, 1}, {0, 1}}; // E,W,E,W
        int[][] diagonals = {{2, 0}, {0, 0}, {2, 2}, {0, 2}};
        WallAutotiler.Shape[] corners = {WallAutotiler.Shape.INNER_NW, WallAutotiler.Shape.INNER_NE,
            WallAutotiler.Shape.INNER_SW, WallAutotiler.Shape.INNER_SE};
        for (int i = 0; i < diagonals.length; i++) {
            TileType[][] tiles = {{F, F, F}, {F, W, F}, {F, F, F}};
            tiles[armsA[i][1]][armsA[i][0]] = W;
            tiles[armsB[i][1]][armsB[i][0]] = W;
            tiles[diagonals[i][1]][diagonals[i][0]] = W;
            assertEquals(corners[i], autotiler.classify(new LaboratoryRoom(tiles, new Vector2()), 1, 1));
        }
        TileType[][] full = {{W, W, W}, {W, W, W}, {W, W, W}};
        assertEquals(WallAutotiler.Shape.CROSS,
            autotiler.classify(new LaboratoryRoom(full, new Vector2()), 1, 1));
        full[2][2] = F;
        assertEquals("대각이 열려도 네 팔이면 십자", WallAutotiler.Shape.CROSS,
            autotiler.classify(new LaboratoryRoom(full, new Vector2()), 1, 1));
    }

    @Test
    public void straightWallDamageIsSparseAndNoConsoleIsUsed() {
        TileType[][] tiles = new TileType[3][82];
        for (TileType[] row : tiles) java.util.Arrays.fill(row, TileType.FLOOR);
        for (int x = 1; x <= 80; x++) tiles[1][x] = TileType.WALL;
        LaboratoryRoom longWall = new LaboratoryRoom(tiles, new Vector2());
        int damaged = 0;
        for (int x = 2; x < 80; x++) {
            String id = autotiler.tileId(longWall, x, 1);
            if (id.equals("wall_horizontal_damaged")) damaged++;
            else assertEquals("wall_horizontal", id);
        }
        assertTrue("직선 벽 파손 변형은 약 10~15%", damaged >= 8 && damaged <= 12);
    }

    @Test
    public void fiveCellLinesUseEndsAndStraightCenters() {
        LaboratoryRoom horizontal = room(".......", ".#####.", ".......");
        assertEquals(WallAutotiler.Shape.END_LEFT, autotiler.classify(horizontal, 1, 1));
        assertEquals(WallAutotiler.Shape.END_RIGHT, autotiler.classify(horizontal, 5, 1));
        for (int x = 2; x <= 4; x++)
            assertEquals(WallAutotiler.Shape.HORIZONTAL, autotiler.classify(horizontal, x, 1));
        LaboratoryRoom vertical = room("...#...", "...#...", "...#...", "...#...", "...#...");
        assertEquals(WallAutotiler.Shape.END_TOP, autotiler.classify(vertical, 3, 4));
        assertEquals(WallAutotiler.Shape.END_BOTTOM, autotiler.classify(vertical, 3, 0));
        for (int y = 1; y <= 3; y++)
            assertEquals(WallAutotiler.Shape.VERTICAL, autotiler.classify(vertical, 3, y));
    }

    @Test(expected = IllegalArgumentException.class)
    public void isolatedWallIsRejected() {
        room("...", ".#.", "...");
    }
}

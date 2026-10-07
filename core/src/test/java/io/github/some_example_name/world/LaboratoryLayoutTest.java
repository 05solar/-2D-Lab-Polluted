package io.github.some_example_name.world;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayDeque;
import java.util.Deque;

import org.junit.Test;

/**
 * Laboratory Tileset V2 맵: 20x15 유지, 스폰/문/외벽 충돌, 네 구역 연결성(정비 도달·오염 밀폐),
 * 바닥 3연속 금지, 오버레이는 벽 위 금지, 문 방향·문 미덮어쓰기, hazard 분리, 재현성.
 * 렌더 픽셀이 아니라 맵 규칙·레이어 타일 선택을 검증한다.
 */
public class LaboratoryLayoutTest {

    private final LaboratoryLayout layout = LaboratoryLayout.testRoom();
    private final LaboratoryRoom room = layout.room();
    private final RoomVisuals visuals = layout.visuals();

    @Test
    public void dimensionsAre20x15() {
        assertEquals(20, room.widthInTiles());
        assertEquals(15, room.heightInTiles());
    }

    @Test
    public void spawnIsWalkableAndSurroundingsSafe() {
        int sx = (int) Math.floor(room.spawnPoint().x);
        int sy = (int) Math.floor(room.spawnPoint().y);
        for (int ty = sy - 1; ty <= sy + 1; ty++) {
            for (int tx = sx - 1; tx <= sx + 1; tx++) {
                assertFalse("스폰 3x3 이동 가능 " + tx + "," + ty, room.isSolid(tx, ty));
                assertEquals("스폰 3x3 위험 없음 " + tx + "," + ty, Hazard.NONE, room.hazardAt(tx, ty));
            }
        }
    }

    @Test
    public void outerBorderSolidExceptSouthDoorway() {
        int w = room.widthInTiles(), h = room.heightInTiles();
        for (int tx = 0; tx < w; tx++) {
            assertTrue("상단 외벽", room.isSolid(tx, h - 1));
            if (tx != 3) assertTrue("하단 외벽(출입구 제외)", room.isSolid(tx, 0));
        }
        for (int ty = 0; ty < h; ty++) {
            assertTrue("좌측 외벽", room.isSolid(0, ty));
            assertTrue("우측 외벽", room.isSolid(w - 1, ty));
        }
        assertFalse("남쪽 출입구 통과 가능", room.isSolid(3, 0));
    }

    @Test
    public void doorsHaveMatchingTypeAndStructureAndDirection() {
        // 남쪽 가로 열린 출입구
        assertEquals(TileType.DOORWAY_OPEN, room.tileAt(3, 0));
        assertEquals("horizontal_door_open", visuals.structureAt(3, 0));
        assertNull("문 셀에는 벽 오토타일을 덮지 않음", visuals.wallAt(3, 0));

        // 오염 세로 닫힌 격리문(통과 불가)
        assertEquals(TileType.DOOR_CLOSED, room.tileAt(12, 11));
        assertTrue(room.isSolid(12, 11));
        assertEquals("vertical_door_closed", visuals.structureAt(12, 11));
        assertNull(visuals.wallAt(12, 11));

        // 정비 세로 열린 문(통로)
        assertEquals(TileType.DOORWAY_OPEN, room.tileAt(12, 3));
        assertFalse(room.isSolid(12, 3));
        assertEquals("vertical_door_open", visuals.structureAt(12, 3));
    }

    private boolean[][] walkableFromSpawn() {
        int w = room.widthInTiles(), h = room.heightInTiles();
        boolean[][] seen = new boolean[h][w];
        int sx = (int) Math.floor(room.spawnPoint().x);
        int sy = (int) Math.floor(room.spawnPoint().y);
        Deque<int[]> q = new ArrayDeque<>();
        seen[sy][sx] = true; q.add(new int[]{sx, sy});
        int[][] d = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!q.isEmpty()) {
            int[] c = q.poll();
            for (int[] dir : d) {
                int nx = c[0] + dir[0], ny = c[1] + dir[1];
                if (nx >= 0 && ny >= 0 && nx < w && ny < h && !seen[ny][nx] && !room.isSolid(nx, ny)) {
                    seen[ny][nx] = true; q.add(new int[]{nx, ny});
                }
            }
        }
        return seen;
    }

    @Test
    public void maintenanceReachableContaminationSealed() {
        boolean[][] seen = walkableFromSpawn();
        boolean maint = false;
        for (int ty = 1; ty <= 5; ty++)
            for (int tx = 13; tx <= 18; tx++)
                if (!room.isSolid(tx, ty) && seen[ty][tx]) maint = true;
        assertTrue("정비 구역은 통로로 도달 가능", maint);

        for (int ty = 9; ty <= 13; ty++)
            for (int tx = 13; tx <= 18; tx++)
                assertFalse("오염 격리실은 밀폐 " + tx + "," + ty,
                    !room.isSolid(tx, ty) && seen[ty][tx]);

        assertTrue("남쪽 출입구 도달", seen[0][3]);
    }

    @Test
    public void wallCellsHaveWallTileAndFloorElsewhere() {
        int w = room.widthInTiles(), h = room.heightInTiles();
        for (int ty = 0; ty < h; ty++) {
            for (int tx = 0; tx < w; tx++) {
                if (room.tileAt(tx, ty) == TileType.WALL) {
                    assertNotNull("벽 셀은 벽 타일", visuals.wallAt(tx, ty));
                    assertNull("벽 셀엔 바닥 없음", visuals.floorAt(tx, ty));
                } else {
                    assertNotNull("비벽 셀은 바닥", visuals.floorAt(tx, ty));
                }
            }
        }
    }

    @Test
    public void overlaysNeverOnWalls() {
        int w = visuals.widthInTiles(), h = visuals.heightInTiles();
        for (int ty = 0; ty < h; ty++)
            for (int tx = 0; tx < w; tx++)
                if (room.tileAt(tx, ty) == TileType.WALL)
                    assertNull("벽 위 오버레이 금지 " + tx + "," + ty, visuals.overlayAt(tx, ty));
    }

    @Test
    public void noFloorTileRepeatsThreeInARow() {
        int w = visuals.widthInTiles(), h = visuals.heightInTiles();
        for (int ty = 0; ty < h; ty++) {
            for (int tx = 0; tx < w; tx++) {
                String v = visuals.floorAt(tx, ty);
                if (v == null) continue;
                if (tx >= 1 && tx + 1 < w && v.equals(visuals.floorAt(tx - 1, ty)) && v.equals(visuals.floorAt(tx + 1, ty)))
                    org.junit.Assert.fail("가로 3연속 바닥 " + v + " @" + tx + "," + ty);
                if (ty >= 1 && ty + 1 < h && v.equals(visuals.floorAt(tx, ty - 1)) && v.equals(visuals.floorAt(tx, ty + 1)))
                    org.junit.Assert.fail("세로 3연속 바닥 " + v + " @" + tx + "," + ty);
            }
        }
    }

    @Test
    public void hazardSeparatedFromVisualsToxicAndShockExist() {
        boolean toxic = false, shock = false;
        for (int ty = 0; ty < room.heightInTiles(); ty++) {
            for (int tx = 0; tx < room.widthInTiles(); tx++) {
                Hazard hz = room.hazardAt(tx, ty);
                if (hz == Hazard.TOXIC) {
                    toxic = true;
                    assertFalse("독성 셀도 통과 가능(FLOOR)", room.isSolid(tx, ty));
                } else if (hz == Hazard.SHOCK) {
                    shock = true;
                }
            }
        }
        assertTrue("오염 구역에 독성 hazard 존재", toxic);
        assertTrue("정비 구역에 감전 hazard 존재", shock);
    }

    @Test
    public void warningBoundaryUsesWarningFloorTiles() {
        assertEquals("floor_warning_north", visuals.floorAt(13, 7));
        assertEquals("floor_warning_north", visuals.floorAt(14, 7));
    }

    @Test
    public void layoutIsReproducibleForSameSeeds() {
        LaboratoryLayout a = LaboratoryLayout.testRoom();
        LaboratoryLayout b = LaboratoryLayout.testRoom();
        for (int ty = 0; ty < 15; ty++) {
            for (int tx = 0; tx < 20; tx++) {
                assertEquals(a.room().tileAt(tx, ty), b.room().tileAt(tx, ty));
                assertEquals(a.visuals().floorAt(tx, ty), b.visuals().floorAt(tx, ty));
                assertEquals(a.visuals().overlayAt(tx, ty), b.visuals().overlayAt(tx, ty));
                assertEquals(a.visuals().wallAt(tx, ty), b.visuals().wallAt(tx, ty));
                assertEquals(a.visuals().structureAt(tx, ty), b.visuals().structureAt(tx, ty));
            }
        }
    }
}

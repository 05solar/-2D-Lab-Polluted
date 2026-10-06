package io.github.some_example_name.world;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

/**
 * 배경 다양화: 오토타일 벽/모서리 선택, 문/출입구 충돌, 장식 비충돌, 재현성, 16종 사용, 스폰 검증.
 * 렌더 픽셀이 아니라 맵 규칙·타일 선택·충돌 의미를 검증한다.
 */
public class LaboratoryLayoutTest {

    private final LaboratoryLayout layout = LaboratoryLayout.testRoom();
    private final LaboratoryRoom room = layout.room();
    private final RoomVisuals visuals = layout.visuals();

    @Test
    public void borderEdgesUseDirectionalWalls() {
        // 내부 벽과 떨어진 깨끗한 경계 셀을 검사한다.
        assertEquals(TileVisual.WALL_NORTH, visuals.visualAt(5, 14)); // 상단
        assertEquals(TileVisual.WALL_SOUTH, visuals.visualAt(5, 0));  // 하단
        assertEquals(TileVisual.WALL_WEST, visuals.visualAt(0, 7));   // 좌측
        assertEquals(TileVisual.WALL_EAST, visuals.visualAt(19, 2));  // 우측
    }

    @Test
    public void borderCornersUseCornerWalls() {
        assertEquals(TileVisual.WALL_CORNER_SW, visuals.visualAt(0, 0));
        assertEquals(TileVisual.WALL_CORNER_SE, visuals.visualAt(19, 0));
        assertEquals(TileVisual.WALL_CORNER_NW, visuals.visualAt(0, 14));
        assertEquals(TileVisual.WALL_CORNER_NE, visuals.visualAt(19, 14));
    }

    @Test
    public void closedDoorCollidesAndOpenDoorwayIsPassable() {
        // 오염 격리실의 유일한 출입: 닫힌 격리문(통과 불가).
        assertEquals(TileType.DOOR_CLOSED, room.tileAt(12, 11));
        assertTrue("닫힌 문은 벽처럼 충돌한다", room.isSolid(12, 11));
        assertEquals(TileVisual.DOOR_CLOSED, visuals.visualAt(12, 11));

        // 남쪽 외벽의 열린 출입구(통과 가능).
        assertEquals(TileType.DOORWAY_OPEN, room.tileAt(3, 0));
        assertFalse("열린 출입구는 통과 가능하다", room.isSolid(3, 0));
        assertEquals(TileVisual.DOORWAY_OPEN, visuals.visualAt(3, 0));
    }

    @Test
    public void decorativeTilesDoNotChangeCollision() {
        // 균열/얼룩/경고(논리 FLOOR), 독성/전선(논리 HAZARD) 모두 통과 가능해야 한다.
        assertFalse("균열", room.isSolid(10, 11));
        assertFalse("얼룩", room.isSolid(17, 13));
        assertFalse("경고", room.isSolid(13, 13));
        assertFalse("독성", room.isSolid(13, 10));
        assertFalse("전선", room.isSolid(16, 5));
        // 위험 타일은 hazard 의미만 데이터로 구분(피해 로직 없음).
        assertTrue("독성은 hazard", room.isHazard(13, 10));
        assertTrue("전선은 hazard", room.isHazard(16, 5));
        assertFalse("경고는 hazard가 아님", room.isHazard(13, 13));
    }

    @Test
    public void allSixteenTileIndicesAreUsed() {
        Set<Integer> used = new HashSet<>();
        for (int ty = 0; ty < visuals.heightInTiles(); ty++) {
            for (int tx = 0; tx < visuals.widthInTiles(); tx++) {
                used.add(visuals.visualAt(tx, ty).tilesetIndex);
            }
        }
        for (int i = 0; i < 16; i++) {
            assertTrue("타일 인덱스 " + i + " 가 최소 한 번 사용돼야 한다", used.contains(i));
        }
    }

    @Test
    public void layoutIsReproducible() {
        LaboratoryLayout a = LaboratoryLayout.testRoom();
        LaboratoryLayout b = LaboratoryLayout.testRoom();
        for (int ty = 0; ty < a.room().heightInTiles(); ty++) {
            for (int tx = 0; tx < a.room().widthInTiles(); tx++) {
                assertEquals("논리 타일 재현", a.room().tileAt(tx, ty), b.room().tileAt(tx, ty));
                assertEquals("시각 타일 재현", a.visuals().visualAt(tx, ty), b.visuals().visualAt(tx, ty));
            }
        }
    }

    @Test
    public void spawnAndSurroundingAreWalkable() {
        int sx = (int) Math.floor(room.spawnPoint().x);
        int sy = (int) Math.floor(room.spawnPoint().y);
        for (int ty = sy - 1; ty <= sy + 1; ty++) {
            for (int tx = sx - 1; tx <= sx + 1; tx++) {
                assertFalse("스폰 주변 3x3 는 이동 가능해야 한다: " + tx + "," + ty, room.isSolid(tx, ty));
            }
        }
    }

    /** 스폰에서 걸어 도달 가능한 셀 집합(BFS, 통과 가능 = !isSolid). */
    private boolean[][] walkableFromSpawn() {
        int w = room.widthInTiles();
        int h = room.heightInTiles();
        boolean[][] seen = new boolean[h][w];
        int sx = (int) Math.floor(room.spawnPoint().x);
        int sy = (int) Math.floor(room.spawnPoint().y);
        Deque<int[]> q = new ArrayDeque<>();
        seen[sy][sx] = true;
        q.add(new int[]{sx, sy});
        int[][] d = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!q.isEmpty()) {
            int[] c = q.poll();
            for (int[] dir : d) {
                int nx = c[0] + dir[0], ny = c[1] + dir[1];
                if (nx >= 0 && ny >= 0 && nx < w && ny < h && !seen[ny][nx] && !room.isSolid(nx, ny)) {
                    seen[ny][nx] = true;
                    q.add(new int[]{nx, ny});
                }
            }
        }
        return seen;
    }

    @Test
    public void maintenanceIsReachableButContaminationIsSealed() {
        boolean[][] seen = walkableFromSpawn();
        // 정비(남동, tx13..18 ty1..5): 바닥 셀이 하나라도 걸어서 도달 가능해야 한다.
        boolean maintReached = false;
        for (int ty = 1; ty <= 5; ty++)
            for (int tx = 13; tx <= 18; tx++)
                if (!room.isSolid(tx, ty) && seen[ty][tx]) maintReached = true;
        assertTrue("정비 구역은 통로로 연결돼 도달 가능해야 한다", maintReached);

        // 오염(북동, tx13..18 ty9..13): 닫힌 격리문으로 밀폐 → 어떤 바닥도 걸어서 도달 불가.
        for (int ty = 9; ty <= 13; ty++)
            for (int tx = 13; tx <= 18; tx++)
                assertFalse("오염 격리실은 밀폐돼야 한다: " + tx + "," + ty,
                    !room.isSolid(tx, ty) && seen[ty][tx]);
    }

    @Test
    public void southDoorwayIsReachableFromSpawn() {
        boolean[][] seen = walkableFromSpawn();
        assertTrue("남쪽 열린 출입구는 스폰에서 도달 가능해야 한다", seen[0][3]);
    }

    @Test
    public void outerBorderIsSolidExceptSouthDoorway() {
        int w = room.widthInTiles();
        int h = room.heightInTiles();
        for (int tx = 0; tx < w; tx++) {
            assertTrue("상단 외벽", room.isSolid(tx, h - 1));
            if (tx != 3) assertTrue("하단 외벽(출입구 제외)", room.isSolid(tx, 0));
        }
        for (int ty = 0; ty < h; ty++) {
            assertTrue("좌측 외벽", room.isSolid(0, ty));
            assertTrue("우측 외벽", room.isSolid(w - 1, ty));
        }
        assertFalse("남쪽 출입구 1칸만 통과 가능", room.isSolid(3, 0));
    }

    @Test
    public void noDecorationRepeatsThreeInARow() {
        Set<TileVisual> deco = new HashSet<>();
        deco.add(TileVisual.FLOOR_CRACKED);
        deco.add(TileVisual.FLOOR_STAIN);
        deco.add(TileVisual.FLOOR_WARNING);
        deco.add(TileVisual.FLOOR_TOXIC);
        deco.add(TileVisual.FLOOR_WIRES);
        int w = visuals.widthInTiles();
        int h = visuals.heightInTiles();
        for (int ty = 0; ty < h; ty++) {
            for (int tx = 0; tx < w; tx++) {
                TileVisual v = visuals.visualAt(tx, ty);
                if (!deco.contains(v)) continue;
                if (tx >= 1 && tx + 1 < w
                    && visuals.visualAt(tx - 1, ty) == v && visuals.visualAt(tx + 1, ty) == v) {
                    org.junit.Assert.fail("가로 3연속 장식: " + v + " @" + tx + "," + ty);
                }
                if (ty >= 1 && ty + 1 < h
                    && visuals.visualAt(tx, ty - 1) == v && visuals.visualAt(tx, ty + 1) == v) {
                    org.junit.Assert.fail("세로 3연속 장식: " + v + " @" + tx + "," + ty);
                }
            }
        }
    }

    @Test
    public void basicFloorIsTheDominantFloor() {
        // 기본 바닥이 전체 바닥의 과반(여기선 ~88%)을 차지해 격자 난잡함을 억제한다.
        int w = visuals.widthInTiles();
        int h = visuals.heightInTiles();
        int floor = 0, basic = 0;
        for (int ty = 0; ty < h; ty++) {
            for (int tx = 0; tx < w; tx++) {
                if (room.isSolid(tx, ty) || room.tileAt(tx, ty) == TileType.DOORWAY_OPEN) continue;
                floor++;
                if (visuals.visualAt(tx, ty) == TileVisual.FLOOR_BASIC) basic++;
            }
        }
        assertTrue("기본 바닥이 바닥의 과반이어야 한다", basic * 2 > floor);
    }
}

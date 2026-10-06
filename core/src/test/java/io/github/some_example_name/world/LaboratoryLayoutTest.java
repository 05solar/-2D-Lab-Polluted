package io.github.some_example_name.world;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
        assertEquals(TileType.DOOR_CLOSED, room.tileAt(10, 10));
        assertTrue("닫힌 문은 벽처럼 충돌한다", room.isSolid(10, 10));
        assertEquals(TileVisual.DOOR_CLOSED, visuals.visualAt(10, 10));

        assertEquals(TileType.DOORWAY_OPEN, room.tileAt(3, 0));
        assertFalse("열린 출입구는 통과 가능하다", room.isSolid(3, 0));
        assertEquals(TileVisual.DOORWAY_OPEN, visuals.visualAt(3, 0));
    }

    @Test
    public void decorativeTilesDoNotChangeCollision() {
        // 균열/얼룩/경고(논리 FLOOR), 독성/전선(논리 HAZARD) 모두 통과 가능해야 한다.
        assertFalse("균열", room.isSolid(1, 13));
        assertFalse("얼룩", room.isSolid(11, 11));
        assertFalse("경고", room.isSolid(9, 13));
        assertFalse("독성", room.isSolid(14, 13));
        assertFalse("전선", room.isSolid(14, 5));
        // 위험 타일은 hazard 의미만 데이터로 구분(피해 로직 없음).
        assertTrue("독성은 hazard", room.isHazard(14, 13));
        assertTrue("전선은 hazard", room.isHazard(14, 5));
        assertFalse("경고는 hazard가 아님", room.isHazard(9, 13));
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
}

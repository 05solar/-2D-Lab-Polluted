package io.github.some_example_name.world;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** 테스트 방: 테두리 벽, 스폰 지점이 바닥(통과 가능)인지 검증. */
public class LaboratoryRoomTest {

    @Test
    public void bordersAreSolidAndOutOfBoundsIsSolid() {
        LaboratoryRoom room = LaboratoryRoom.createTestRoom();
        int w = room.widthInTiles();
        int h = room.heightInTiles();

        assertTrue(room.isSolid(0, 0));
        assertTrue(room.isSolid(w - 1, h - 1));
        assertTrue("범위 밖은 벽으로 취급", room.isSolid(-1, 5));
        assertTrue(room.isSolid(w, 5));
    }

    @Test
    public void spawnIsOnWalkableFloor() {
        LaboratoryRoom room = LaboratoryRoom.createTestRoom();
        int sx = (int) Math.floor(room.spawnPoint().x);
        int sy = (int) Math.floor(room.spawnPoint().y);
        assertFalse("스폰 지점은 벽이 아니어야 한다", room.isSolid(sx, sy));
    }

    @Test
    public void dimensionsMatchTemplate() {
        LaboratoryRoom room = LaboratoryRoom.createTestRoom();
        assertEquals(20, room.widthInTiles());
        assertEquals(15, room.heightInTiles());
    }
}

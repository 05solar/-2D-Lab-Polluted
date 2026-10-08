package io.github.some_example_name.world;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** 문 방향은 문자 코드가 아니라 벽 배열에서 정해지고 문틀은 중복 없이 3셀을 쓴다. */
public class DoorPlacementTest {

    private static LaboratoryLayout horizontal(char marker) {
        return LaboratoryLayout.fromRows(new String[]{
            ".........", ".........", ".##" + marker + "##...", ".........", "........."});
    }

    private static LaboratoryLayout vertical(char marker) {
        return LaboratoryLayout.fromRows(new String[]{
            ".......", "...#...", "...#...", "..." + marker + "...",
            "...#...", "...#...", "......."});
    }

    @Test
    public void horizontalClosedAndOpenShareAnchorAndJambs() {
        LaboratoryLayout closed = horizontal('h');
        LaboratoryLayout open = horizontal('o');
        for (LaboratoryLayout layout : new LaboratoryLayout[]{closed, open}) {
            Door door = layout.room().doorAt(3, 2);
            assertEquals(Door.Orientation.HORIZONTAL, door.orientation());
            assertEquals("door_jamb_left", layout.visuals().structureAt(2, 2));
            assertEquals("door_jamb_right", layout.visuals().structureAt(4, 2));
            assertNull(layout.visuals().wallAt(2, 2));
            assertNull(layout.visuals().wallAt(3, 2));
            assertNull(layout.visuals().wallAt(4, 2));
            assertEquals(3, door.x());
            assertEquals(2, door.y());
            new WallTopologyValidator().validateVisualConnections(layout.room(), layout.visuals());
        }
        assertTrue(closed.room().isSolid(3, 2));
        assertFalse(open.room().isSolid(3, 2));
        assertEquals("horizontal_door_closed", closed.visuals().structureAt(3, 2));
        assertEquals("horizontal_door_open", open.visuals().structureAt(3, 2));
    }

    @Test
    public void verticalClosedAndOpenShareAnchorAndJambs() {
        LaboratoryLayout closed = vertical('d');
        LaboratoryLayout open = vertical('g');
        for (LaboratoryLayout layout : new LaboratoryLayout[]{closed, open}) {
            Door door = layout.room().doorAt(3, 3);
            assertEquals(Door.Orientation.VERTICAL, door.orientation());
            assertEquals("vertical_door_jamb_top", layout.visuals().structureAt(3, 4));
            assertEquals("vertical_door_jamb_bottom", layout.visuals().structureAt(3, 2));
            assertNull(layout.visuals().wallAt(3, 4));
            assertNull(layout.visuals().wallAt(3, 3));
            assertNull(layout.visuals().wallAt(3, 2));
            assertEquals(3, door.x());
            assertEquals(3, door.y());
        }
        assertTrue(closed.room().isSolid(3, 3));
        assertFalse(open.room().isSolid(3, 3));
        assertEquals("vertical_door_closed", closed.visuals().structureAt(3, 3));
        assertEquals("vertical_door_open", open.visuals().structureAt(3, 3));
    }

    @Test
    public void changingStateKeepsPositionAndUpdatesCollisionAndArt() {
        LaboratoryLayout layout = horizontal('h');
        Door door = layout.room().doorAt(3, 2);
        door.setState(DoorState.OPENING);
        assertTrue(layout.room().isSolid(3, 2));
        door.setState(DoorState.OPEN);
        assertFalse(layout.room().isSolid(3, 2));
        assertEquals("horizontal_door_open", layout.visuals().structureAt(3, 2));
        assertEquals("door_jamb_left", layout.visuals().structureAt(2, 2));
        door.setState(DoorState.CLOSING);
        assertTrue(layout.room().isSolid(3, 2));
        assertEquals("horizontal_door_closed", layout.visuals().structureAt(3, 2));
        assertEquals(3, door.x());
        assertEquals(2, door.y());
    }

    @Test
    public void liveAssemblyMapHasConnectedDoorsWithoutLooseFloorRails() {
        LaboratoryLayout layout = LaboratoryLayout.wallDoorAssemblyTestRoom();
        assertEquals(9, layout.room().widthInTiles());
        assertEquals(9, layout.room().heightInTiles());
        assertEquals(Door.Orientation.HORIZONTAL, layout.room().doorAt(4, 7).orientation());
        assertEquals(Door.Orientation.VERTICAL, layout.room().doorAt(7, 3).orientation());
        assertTrue(layout.room().isSolid(4, 7));
        assertFalse(layout.room().isSolid(7, 3));
        for (int y = 0; y < 9; y++) {
            for (int x = 0; x < 9; x++) {
                String floor = layout.visuals().floorAt(x, y);
                assertFalse("unpaired metal seam at " + x + "," + y,
                    floor.equals("floor_seam_horizontal") || floor.equals("floor_seam_vertical")
                        || floor.equals("floor_seam_cross"));
            }
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsDoorWithMissingJambSpace() {
        LaboratoryLayout.fromRows(new String[]{".....", ".#h#.", "....."});
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsDoorNextToTJunction() {
        LaboratoryLayout.fromRows(new String[]{
            ".........", ".#.......", ".##h##...", ".........", "........."});
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsDoorAtCornerOrCross() {
        LaboratoryLayout.fromRows(new String[]{
            ".........", "...#.....", ".##h##...", "...#.....", "........."});
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsDoorNextToAnotherDoor() {
        LaboratoryLayout.fromRows(new String[]{
            ".........", ".........", ".#ho##...", ".........", "........."});
    }
}

package io.github.some_example_name.world;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class RoomPathfinderTest {
    @Test public void easyReturnPathExistsAndUsesPassableCells() {
        LaboratoryRoom room=LaboratoryLayout.easyRoom().room();
        int gateX = room.southExitCenterX(), gateY = room.southExitPathY();
        List<RoomPathfinder.Cell> path=new RoomPathfinder().shortestPath(room,
            (int) room.spawnPoint().x, (int) room.spawnPoint().y, gateX, gateY);
        assertFalse(path.isEmpty());
        RoomPathfinder.Cell last = path.get(path.size() - 1);
        assertEquals(gateX, last.x); assertEquals(gateY, last.y);
        for(RoomPathfinder.Cell cell:path) assertFalse(room.isSolid(cell.x,cell.y));
    }
}

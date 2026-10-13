package io.github.some_example_name.world;

import org.junit.Test;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import static org.junit.Assert.*;

public class EasyLaboratoryLayoutTest {
    @Test public void easyMapHasSixteenConnectedRoomsAndReturnGate() {
        LaboratoryLayout layout = LaboratoryLayout.easyRoom();
        LaboratoryRoom room = layout.room();
        assertEquals(128, room.widthInTiles());
        assertEquals(96, room.heightInTiles());
        assertFalse("main cross corridor keeps its two tile minimum", room.isSolid(55, 35));
        assertFalse("main cross corridor keeps its two tile minimum", room.isSolid(55, 36));
        assertTrue("cross corridor was narrowed from three to two", room.isSolid(55, 34));
        assertTrue("cross corridor was narrowed from three to two", room.isSolid(55, 37));
        assertTrue(room.spawnPoint().x > 2f && room.spawnPoint().y > 1f);
        boolean[][] seen = reachable(room);
        for (LaboratoryRoomArea area : layout.roomAreas()) {
            boolean roomReachable=false;
            for(int y=Math.max(0,(int)area.centerY()-2);y<=Math.min(room.heightInTiles()-1,(int)area.centerY()+2);y++)
                for(int x=Math.max(0,(int)area.centerX()-2);x<=Math.min(room.widthInTiles()-1,(int)area.centerX()+2);x++)
                    roomReachable|=seen[y][x];
            assertTrue("room " + area.id() + " has a reachable interior approach",roomReachable);
        }
        int gateX = room.southExitCenterX();
        assertFalse("south return opening remains walkable", room.isSolid(gateX, 0));
        assertFalse("return opening continues into entrance room", room.isSolid(gateX, 7));
        assertFalse(room.crossedSouthExit(6f, 5f, gateX + .5f, -1f));
        assertFalse(room.crossedSouthExit(1.2f, 1.1f, gateX + .5f, -1f));
        assertTrue(room.crossedSouthExit(1.1f, .9f, gateX + .5f, -1f));
        assertFalse(room.crossedSouthExit(1.1f, .8f, gateX + .5f, -1f));
        assertTrue("rooms use clustered V7 furniture", layout.visuals().props().size() >= 64);
        for (LaboratoryProp prop : layout.visuals().props())
            assertTrue("all easy-map furniture comes from orthographic views v7", prop.id().startsWith("v7:"));
        assertEquals("one wood, one epic, one legendary container", 3, layout.lootContainers().size());
        assertEquals("wood container in chemistry", "wooden_chest", layout.lootContainers().get(0).id());
        assertEquals("epic container in information room", "epic_cabinet", layout.lootContainers().get(1).id());
        assertEquals("legendary container in library", "legendary_cabinet", layout.lootContainers().get(2).id());
        assertNull("map padding is outside the built facility", layout.visuals().floorAt(0, 40));
        for (LaboratoryRoomArea area : layout.roomAreas())
            for (int y = area.y() + 1; y < area.y() + area.height() - 1; y++)
                for (int x = area.x() + 1; x < area.x() + area.width() - 1; x++)
                    if (!room.isSolid(x, y))
                        assertNotNull("walkable indoor cell has a floor", layout.visuals().floorAt(x, y));
        int doorCount = 0;
        for (int y = 0; y < room.heightInTiles(); y++) for (int x = 0; x < room.widthInTiles(); x++) {
            Door door = room.doorAt(x, y);
            if (door == null) continue;
            doorCount++;
            assertFalse("easy room doors start passable", room.isSolid(x, y));
            assertEquals(door.visualId(), layout.visuals().structureAt(x, y));
            assertNull(layout.visuals().wallAt(x, y));
            if (door.orientation() == Door.Orientation.HORIZONTAL) {
                assertEquals("door_jamb_left", layout.visuals().structureAt(x - 1, y));
                assertEquals("door_jamb_right", layout.visuals().structureAt(x + 1, y));
            } else {
                assertEquals("vertical_door_jamb_bottom", layout.visuals().structureAt(x, y - 1));
                assertEquals("vertical_door_jamb_top", layout.visuals().structureAt(x, y + 1));
            }
        }
        assertEquals("one authored door per room", 16, doorCount);
        assertEquals(16, layout.roomAreas().size());
        int minWidth=Integer.MAX_VALUE,maxWidth=0;
        for (LaboratoryRoomArea area : layout.roomAreas()) {
            minWidth=Math.min(minWidth,area.width());
            maxWidth=Math.max(maxWidth,area.width());
            assertTrue("compressed room remains traversable",area.width()>=7&&area.height()>=7);
        }
        assertTrue("small rooms retained",minWidth<=7);
        assertTrue("larger rooms retained within half-scale bounds",maxWidth<=11);
        assertTrue("sleeping room notch remains",room.isSolid(45,23));
        assertTrue("chemistry room recess remains",room.isSolid(30,53));
        assertTrue("archive room recess remains",room.isSolid(90,41));
        Door firstDoor = null;
        for (int y = 0; y < room.heightInTiles() && firstDoor == null; y++)
            for (int x = 0; x < room.widthInTiles() && firstDoor == null; x++)
                firstDoor = room.doorAt(x, y);
        assertNotNull(firstDoor);
        firstDoor.setState(DoorState.CLOSED);
        assertTrue(room.isSolid(firstDoor.x(), firstDoor.y()));
        firstDoor.setState(DoorState.OPEN);
        assertFalse(room.isSolid(firstDoor.x(), firstDoor.y()));
    }

    @Test public void roomkitPropsHaveSeparateArtAndFootprintData() {
        LaboratoryLayout layout = LaboratoryLayout.easyRoom();
        assertTrue(layout.visuals().props().size() >= 45);
        int smallestRoomCount=Integer.MAX_VALUE;
        for (LaboratoryProp prop : layout.visuals().props()) {
            assertNotNull(prop.id());
            assertTrue(prop.width() > 0f && prop.height() > 0f);
            assertNotNull("furniture collision", prop.collision());
            assertFalse("archive furnishing stays out of the L-shaped wall recess",
                prop.x() >= 89f && prop.x() < 92f && prop.y() >= 40f && prop.y() < 42f);
        }
        for(LaboratoryRoomArea area:layout.roomAreas()) {
            int count=0;
            for(LaboratoryProp prop:layout.visuals().props())
                if(prop.x()>=area.x()&&prop.x()<area.x()+area.width()
                    &&prop.y()>=area.y()&&prop.y()<area.y()+area.height())count++;
            smallestRoomCount=Math.min(smallestRoomCount,count);
            assertTrue("compact rooms still retain wall and work furniture: "+area.id(),count>=4);
        }
        assertTrue("all compact room templates meet their minimum furniture count",smallestRoomCount>=4);
        for(int i=0;i<layout.visuals().props().size();i++)
            for(int j=i+1;j<layout.visuals().props().size();j++)
                assertFalse("furniture ground footprints do not overlap",
                    layout.visuals().props().get(i).collision().overlaps(
                        layout.visuals().props().get(j).collision()));
        for(LootContainerPlacement container:layout.lootContainers()) {
            com.badlogic.gdx.math.Rectangle footprint=new com.badlogic.gdx.math.Rectangle(
                container.x()-.42f,container.y(),.84f,.48f);
            for(LaboratoryProp prop:layout.visuals().props())
                assertFalse("container approach is not occupied by furniture",
                    footprint.overlaps(prop.collision()));
        }
    }

    @Test public void everyUsableRoomWallUsesMeasuredOpaqueFurnitureCoverage() {
        LaboratoryLayout layout = LaboratoryLayout.easyRoom();
        assertEquals("four wall reports per room", layout.roomAreas().size() * 4,
            layout.wallCoverage().size());
        for (WallFurnitureCoverageReport wall : layout.wallCoverage()) {
            assertTrue("measured fill never exceeds the wall run",
                wall.coveredLength() <= wall.usableLength() + .001f);
        }
    }

    @Test public void everyFurnitureVisibleBoundsAndFootprintStayInsideItsRoom() {
        LaboratoryLayout layout = LaboratoryLayout.easyRoom();
        List<LaboratoryProp> props = layout.visuals().props();
        for (int i = 0; i < props.size(); i++) {
            LaboratoryProp prop = props.get(i);
            com.badlogic.gdx.math.Rectangle visible = FurniturePlacementValidator.visibleBounds(prop);
            assertTrue("visible furniture is inside a room floor: " + prop.id(),
                insideNamedRoomFloor(layout, visible));
            assertTrue("furniture collision is in its room floor or wall support: " + prop.id(),
                insideNamedRoomFloor(layout, prop.collision()) || onWallSupport(layout.room(), prop.collision()));
            for (int j = i + 1; j < props.size(); j++) {
                LaboratoryProp other = props.get(j);
                assertFalse("visible furniture does not overlap: " + prop.id() + " / " + other.id(),
                    visible.overlaps(FurniturePlacementValidator.visibleBounds(other)));
                assertFalse("furniture collision does not overlap: " + prop.id() + " / " + other.id(),
                    prop.collision().overlaps(other.collision()));
            }
        }
    }

    @Test public void easyWallVisualsUseConnectedCornersJunctionsAndSparseSupports() {
        LaboratoryLayout layout=LaboratoryLayout.easyRoom();
        new WallTopologyValidator().validateWallTopology(layout.room(),layout.visuals());
        new WallTopologyValidator().validateVisualConnections(layout.room(),layout.visuals());
        java.util.Set<String> ids=new java.util.HashSet<>();
        for(int y=0;y<layout.room().heightInTiles();y++) for(int x=0;x<layout.room().widthInTiles();x++) {
            String id=layout.visuals().wallAt(x,y);
            if(id!=null) ids.add(id);
        }
        assertTrue("corners use dedicated tiles", ids.contains("wall_outer_nw") || ids.contains("wall_inner_nw"));
        assertTrue("T junctions use dedicated tiles", ids.contains("wall_t_open_north")
            || ids.contains("wall_t_open_south") || ids.contains("wall_t_open_east") || ids.contains("wall_t_open_west"));
        assertTrue("straight reinforced supports are used sparingly", ids.contains("wall_support_horizontal")
            || ids.contains("wall_support_vertical"));
    }

    private static boolean insideNamedRoomFloor(LaboratoryLayout layout,
                                                 com.badlogic.gdx.math.Rectangle area) {
        if (area == null) return true;
        for (LaboratoryRoomArea room : layout.roomAreas()) {
            FurniturePlacementValidator.RoomPlanBounds bounds = new FurniturePlacementValidator.RoomPlanBounds(
                room.x() + 1, room.y() + 1, room.x() + room.width() - 1, room.y() + room.height() - 1);
            if (area.x < bounds.minX || area.y < bounds.minY
                    || area.x + area.width > bounds.maxX + .0001f
                    || area.y + area.height > bounds.maxY + .0001f) continue;
            boolean floor = true;
            int minX = (int)Math.floor(area.x + .0001f), minY = (int)Math.floor(area.y + .0001f);
            int maxX = (int)Math.floor(area.x + area.width - .0001f);
            int maxY = (int)Math.floor(area.y + area.height - .0001f);
            for (int y=minY; y<=maxY && floor; y++) for (int x=minX; x<=maxX; x++)
                if (layout.room().tileAt(x,y) != TileType.FLOOR) { floor=false; break; }
            if (!floor) continue;
            return true;
        }
        return false;
    }

    private static boolean onWallSupport(LaboratoryRoom room, com.badlogic.gdx.math.Rectangle area) {
        if (area == null) return true;
        int minX=(int)Math.floor(area.x+.0001f), minY=(int)Math.floor(area.y+.0001f);
        int maxX=(int)Math.floor(area.x+area.width-.0001f), maxY=(int)Math.floor(area.y+area.height-.0001f);
        for(int y=minY;y<=maxY;y++) for(int x=minX;x<=maxX;x++)
            if(room.tileAt(x,y)!=TileType.WALL)return false;
        return true;
    }

    private static boolean[][] reachable(LaboratoryRoom room) {
        boolean[][] seen = new boolean[room.heightInTiles()][room.widthInTiles()];
        int sx = (int) room.spawnPoint().x, sy = (int) room.spawnPoint().y;
        Deque<int[]> q = new ArrayDeque<>(); q.add(new int[]{sx, sy}); seen[sy][sx] = true;
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        while (!q.isEmpty()) {
            int[] p = q.removeFirst();
            for (int[] d : dirs) {
                int x=p[0]+d[0], y=p[1]+d[1];
                if (x>=0 && y>=0 && x<room.widthInTiles() && y<room.heightInTiles()
                    && !seen[y][x] && !room.isSolid(x,y)) { seen[y][x]=true; q.add(new int[]{x,y}); }
            }
        }
        return seen;
    }

}
 

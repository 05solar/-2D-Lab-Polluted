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
        assertFalse("main cross corridor keeps its two tile minimum", room.isSolid(43, 28));
        assertFalse("main cross corridor keeps its two tile minimum", room.isSolid(43, 29));
        assertTrue("cross corridor stays two tiles", room.isSolid(43, 27));
        assertTrue("cross corridor stays two tiles", room.isSolid(43, 30));
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
        // 컨테이너: 설정 기반 분산 배치(wood 14 / epic 7 / legendary 2 = 23), 대부분의 방에 ≥1.
        int wood=0,epic=0,legendary=0;
        for(LootContainerPlacement p:layout.lootContainers()) {
            if("wooden_chest".equals(p.id()))wood++;
            else if("epic_cabinet".equals(p.id()))epic++;
            else if("legendary_cabinet".equals(p.id()))legendary++;
        }
        io.github.some_example_name.config.BalanceConfig defaults=
            new io.github.some_example_name.config.BalanceConfig();
        assertEquals(defaults.woodenChestCount,wood);
        assertEquals(defaults.epicCabinetCount,epic);
        assertEquals(defaults.legendaryCabinetCount,legendary);
        assertTrue("grade ratio preserved: wood > epic > legendary",wood>epic&&epic>legendary);
        // 컨테이너끼리 접근로 확보(2.2칸 간격).
        for(int i=0;i<layout.lootContainers().size();i++)
            for(int j=i+1;j<layout.lootContainers().size();j++) {
                LootContainerPlacement a=layout.lootContainers().get(i),b=layout.lootContainers().get(j);
                float dx=a.x()-b.x(),dy=a.y()-b.y();
                assertTrue("containers keep spacing",dx*dx+dy*dy>=2.2f*2.2f-.001f);
            }
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
            // V4: 문은 Door 객체에서 직접 그린다. 구형 structure/wall 스킨은 남지 않는다.
            assertNull("V4 draws doors; no structure skin", layout.visuals().structureAt(x, y));
            assertNull(layout.visuals().wallAt(x, y));
            if (door.orientation() == Door.Orientation.HORIZONTAL) {
                assertEquals("horizontal door jambs are walls", TileType.WALL, room.rawTileAt(x - 1, y));
                assertEquals(TileType.WALL, room.rawTileAt(x + 1, y));
            } else {
                assertEquals("vertical door jambs are walls", TileType.WALL, room.rawTileAt(x, y - 1));
                assertEquals(TileType.WALL, room.rawTileAt(x, y + 1));
            }
        }
        assertEquals("one authored door per room (16 themed + 10 annex)", 26, doorCount);
        assertEquals(26, layout.roomAreas().size());
        int minWidth=Integer.MAX_VALUE,maxWidth=0;
        for (LaboratoryRoomArea area : layout.roomAreas()) {
            minWidth=Math.min(minWidth,area.width());
            maxWidth=Math.max(maxWidth,area.width());
            assertTrue("compressed room remains traversable",area.width()>=7&&area.height()>=7);
        }
        assertTrue("small rooms retained",minWidth<=7);
        assertTrue("larger rooms retained within half-scale bounds",maxWidth<=11);
        assertTrue("sleeping room notch remains",room.isSolid(49,20));
        assertTrue("chemistry room recess remains",room.isSolid(37,44));
        assertTrue("library recess remains",room.isSolid(84,33));
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

    @Test public void easyWallsUseConnectedV4MasksWithNoLegacySkin() {
        LaboratoryLayout layout=LaboratoryLayout.easyRoom();
        LaboratoryRoom room=layout.room();
        // 구형 벽/문 스킨 레이어는 완전히 비어 있다(혼재 금지). 벽은 V4가 논리 점유에서 그린다.
        for(int y=0;y<room.heightInTiles();y++) for(int x=0;x<room.widthInTiles();x++) {
            assertNull("no legacy wall skin remains", layout.visuals().wallAt(x,y));
            assertNull("no legacy structure skin remains", layout.visuals().structureAt(x,y));
        }
        java.util.Set<Integer> indices=new java.util.HashSet<>();
        for(int y=0;y<room.heightInTiles();y++) for(int x=0;x<room.widthInTiles();x++) {
            if(room.rawTileAt(x,y)!=TileType.WALL) continue;
            int idx=WallMaskV4.atlasIndex(room,x,y); // 지원하지 않는 마스크면 throw
            assertTrue("valid V4 atlas index", idx>=0 && idx<WallMaskV4.VARIANT_COUNT);
            indices.add(idx);
        }
        // 직선/코너/T/십자/채움 등 다양한 연결 형태가 실제로 쓰인다.
        assertTrue("straight walls present",
            indices.contains(WallMaskV4.atlasIndex(WallMaskV4.E|WallMaskV4.W))
            || indices.contains(WallMaskV4.atlasIndex(WallMaskV4.N|WallMaskV4.S)));
        assertTrue("solid fill interior present", indices.contains(WallMaskV4.atlasIndex(255)));
        assertTrue("connected variety is used", indices.size()>=6);
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
 

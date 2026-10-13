package io.github.some_example_name.world;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds logical tiles, four visual layers, doors, collision and hazards from map rows.
 * Rows start at world north. Door markers choose only initial state: orientation is
 * inferred from adjacent walls and requires a five-cell straight run.
 */
public final class LaboratoryLayout {

    private static final float PLAN_SPACING_SCALE = .70f;
    private static final float ROOM_SIZE_SCALE = .55f;
    private static final int MAIN_SPINE_X = 63;
    private static final int MAIN_CROSS_Y = 35;
    private static final int WEST_SPINE_X = 21;
    private static final int EAST_SPINE_X = 102;
    private static final int SOUTH_SPINE_Y = 5;
    private static final float WALL_FURNITURE_HEIGHT = 2.45f;
    private static final float WALL_FURNITURE_GAP = .05f;

    private static final String[] TEST_ROOM = {
        "####################",
        "#...........#......#",
        "#...........#......#",
        "#...........d......#",
        "#...........#......#",
        "#...........#......#",
        "#...........########",
        "#..................#",
        "#...........########",
        "#...........#......#",
        "#...........#......#",
        "#...........g......#",
        "#..P........#......#",
        "#...........#......#",
        "###o################"
    };

    private final LaboratoryRoom room;
    private final RoomVisuals visuals;
    private final List<LootContainerPlacement> lootContainers;
    private final List<LaboratoryRoomArea> roomAreas;
    private final List<WallFurnitureCoverageReport> wallCoverage;

    private LaboratoryLayout(LaboratoryRoom room, RoomVisuals visuals) {
        this(room, visuals, java.util.Collections.<LootContainerPlacement>emptyList());
    }

    private LaboratoryLayout(LaboratoryRoom room, RoomVisuals visuals,
                             List<LootContainerPlacement> lootContainers) {
        this(room, visuals, lootContainers, java.util.Collections.<LaboratoryRoomArea>emptyList());
    }

    private LaboratoryLayout(LaboratoryRoom room, RoomVisuals visuals,
                             List<LootContainerPlacement> lootContainers,
                             List<LaboratoryRoomArea> roomAreas) {
        this(room, visuals, lootContainers, roomAreas,
            java.util.Collections.<WallFurnitureCoverageReport>emptyList());
    }

    private LaboratoryLayout(LaboratoryRoom room, RoomVisuals visuals,
                             List<LootContainerPlacement> lootContainers,
                             List<LaboratoryRoomArea> roomAreas,
                             List<WallFurnitureCoverageReport> wallCoverage) {
        this.room = room;
        this.visuals = visuals;
        this.lootContainers = java.util.Collections.unmodifiableList(
            new ArrayList<>(lootContainers));
        this.roomAreas = java.util.Collections.unmodifiableList(new ArrayList<>(roomAreas));
        this.wallCoverage = java.util.Collections.unmodifiableList(new ArrayList<>(wallCoverage));
    }

    public LaboratoryRoom room() { return room; }
    public RoomVisuals visuals() { return visuals; }
    public List<LootContainerPlacement> lootContainers() { return lootContainers; }
    public List<LaboratoryRoomArea> roomAreas() { return roomAreas; }
    public List<WallFurnitureCoverageReport> wallCoverage() { return wallCoverage; }

    public static LaboratoryLayout testRoom() {
        return fromRows(TEST_ROOM);
    }

    /** Easy campaign: a large, irregular campus connected by a cross-spine and short branches. */
    public static LaboratoryLayout easyRoom() {
        final int w = 128, h = 96;
        TileType[][] logical = new TileType[h][w];
        LaboratoryZone[][] zones = new LaboratoryZone[h][w];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            logical[y][x] = TileType.WALL;
            zones[y][x] = LaboratoryZone.CENTRAL;
        }

        // Keep the 128x96 campus bounds, but compress room dimensions to about 55%
        // and room spacing to 70%. Each room remains at least seven cells across so
        // a centered opening can retain the five-cell straight wall run required by
        // the door resolver.
        List<RoomPlan> plans = new ArrayList<>();
        plans.add(new RoomPlan("CHEMISTRY", 8, 70, 17, 17, Side.WEST, 8, 16, 72, 4, 5));
        plans.add(new RoomPlan("BIOLOGY", 35, 72, 18, 15, Side.EAST, 7, 0, 0, 0, 0));
        plans.add(new RoomPlan("GENERAL LAB", 8, 52, 15, 13, Side.WEST, 6, 0, 0, 0, 0));
        plans.add(new RoomPlan("PREP ROOM", 34, 52, 15, 13, Side.EAST, 6, 0, 0, 0, 0));
        plans.add(new RoomPlan("INFORMATION", 69, 71, 17, 16, Side.WEST, 7, 0, 0, 0, 0));
        plans.add(new RoomPlan("CCTV", 99, 72, 15, 15, Side.EAST, 7, 0, 0, 0, 0));
        plans.add(new RoomPlan("SPECIMEN", 69, 52, 13, 12, Side.WEST, 5, 0, 0, 0, 0));
        plans.add(new RoomPlan("LIBRARY", 98, 52, 19, 13, Side.EAST, 6, 100, 54, 5, 3));
        plans.add(new RoomPlan("LOUNGE", 8, 26, 17, 15, Side.NORTH, 8, 0, 0, 0, 0));
        plans.add(new RoomPlan("SLEEPING", 34, 26, 16, 17, Side.NORTH, 8, 36, 28, 4, 6));
        plans.add(new RoomPlan("RESTROOM", 8, 8, 9, 12, Side.SOUTH, 5, 0, 0, 0, 0));
        plans.add(new RoomPlan("LOCKER", 22, 8, 9, 12, Side.SOUTH, 5, 0, 0, 0, 0));
        plans.add(new RoomPlan("SHOWER", 37, 8, 9, 12, Side.SOUTH, 5, 0, 0, 0, 0));
        plans.add(new RoomPlan("RECORDS", 69, 26, 15, 15, Side.NORTH, 7, 0, 0, 0, 0));
        plans.add(new RoomPlan("PANTRY", 96, 29, 9, 12, Side.NORTH, 5, 0, 0, 0, 0));
        plans.add(new RoomPlan("ENTRANCE", 68, 8, 17, 14, Side.SOUTH, 9, 0, 0, 0, 0));

        // Main and branch corridors are two tiles wide. The previous three-tile
        // branches are reduced by 25%; the two-tile minimum keeps player movement clear.
        hall(logical, MAIN_SPINE_X, 0, 2, 72);
        hall(logical, 19, MAIN_CROSS_Y, 90, 2);
        hall(logical, WEST_SPINE_X, SOUTH_SPINE_Y, 56, 2);
        hall(logical, WEST_SPINE_X, SOUTH_SPINE_Y, 2, 32);
        // Open door markers sit in the same logical boundary cells later used by
        // DoorPlacementResolver for visual jambs and collision state.
        for (RoomPlan plan : plans) {
            drawRoomFloor(logical, plan);
            logical[plan.doorY][plan.doorX] = TileType.DOORWAY_OPEN;
        }
        carveDoorApproaches(logical, plans);
        // Corridor carving can cross a room-local approach in the compressed plan;
        // reapply each authored opening so it remains a real door cell.
        for (RoomPlan plan : plans) logical[plan.doorY][plan.doorX] = TileType.DOORWAY_OPEN;

        List<LaboratoryProp> props = new ArrayList<>();
        List<LootContainerPlacement> containers = new ArrayList<>();
        List<LaboratoryRoomArea> roomAreas = new ArrayList<>();
        DoorPlacementResolver.Result resolved = new DoorPlacementResolver().resolve(logical);
        for (int x = 62; x <= 64; x++) logical[0][x] = TileType.DOORWAY_OPEN;
        roomAreas.addAll(areas(plans));
        List<WallFurnitureCoverageReport> coverage = new ArrayList<>();
        for (RoomPlan plan : plans) addRoomFurniture(props, plan, coverage, logical);
        validateFurniturePlacements(props, plans, logical);
        containers.add(findContainer("wooden_chest","CHEMISTRY",plans,props,logical));
        containers.add(findContainer("epic_cabinet","INFORMATION",plans,props,logical));
        containers.add(findContainer("legendary_cabinet","LIBRARY",plans,props,logical));
        Vector2 spawn = new Vector2(63.5f, 3.2f);
        LaboratoryRoom room = new LaboratoryRoom(logical, spawn, null, resolved.doors, props);
        room.setSouthExitGate(62f, 65f, 1f);
        for (LootContainerPlacement placement : containers)
            room.addWorldObstacle(new Rectangle(placement.x() - .42f, placement.y(), .84f, .48f));
        OverlayResolver.Result overlays = new OverlayResolver(OverlayResolver.DEFAULT_SEED + 100)
            .resolve(logical, zones);
        String[][] floor = new FloorVariantResolver(FloorVariantResolver.DEFAULT_SEED + 100)
            .resolve(logical, zones);
        String[][] wall = new String[h][w], structure = resolved.structure;
        WallAutotiler autotiler = new WallAutotiler();
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            boolean occupied = logical[y][x] != TileType.WALL || adjacentToFloor(logical, x, y);
            if (!occupied) {
                floor[y][x] = null;
                overlays.overlay[y][x] = null;
                structure[y][x] = null;
                continue;
            }
            if (logical[y][x] == TileType.WALL)
                wall[y][x] = autotiler.tileId(room, x, y);
            if (structure[y][x] != null) wall[y][x] = null;
        }
        RoomVisuals visuals = new RoomVisuals(floor, overlays.overlay, wall, structure,
            resolved.doors, props);
        new WallTopologyValidator().validateDoorPlacement(room, visuals);
        return new LaboratoryLayout(room, visuals, containers, roomAreas, coverage);
    }

    private enum Side { NORTH, EAST, SOUTH, WEST }
    private static final class RoomPlan {
        final String name; final int x, y, w, h; final Side side; final int offset;
        final int notchX, notchY, notchW, notchH;
        final int doorX, doorY;
        RoomPlan(String name,int x,int y,int w,int h,Side side,int offset,
                 int notchX,int notchY,int notchW,int notchH) {
            this.name=name;
            this.x=compactX(x);this.y=compactY(y);
            this.w=Math.max(7,Math.round(w*ROOM_SIZE_SCALE));
            this.h=Math.max(7,Math.round(h*ROOM_SIZE_SCALE));
            this.side=side;
            int openingLength=(side==Side.NORTH||side==Side.SOUTH)?this.w:this.h;
            this.offset=Math.max(3,Math.min(openingLength-4,
                Math.round(offset*ROOM_SIZE_SCALE)));
            this.notchX=this.x+Math.round((notchX-x)*ROOM_SIZE_SCALE);
            this.notchY=this.y+Math.round((notchY-y)*ROOM_SIZE_SCALE);
            this.notchW=notchW==0?0:Math.max(1,Math.round(notchW*ROOM_SIZE_SCALE));
            this.notchH=notchH==0?0:Math.max(1,Math.round(notchH*ROOM_SIZE_SCALE));
            doorX=side==Side.WEST?this.x:side==Side.EAST?this.x+this.w-1:this.x+this.offset;
            doorY=side==Side.SOUTH?this.y:side==Side.NORTH?this.y+this.h-1:this.y+this.offset;
        }
    }

    private static int compactX(float x) {
        return Math.round(64f + (x - 64f) * PLAN_SPACING_SCALE);
    }
    private static int compactY(float y) {
        return Math.round(8f + (y - 8f) * PLAN_SPACING_SCALE);
    }
    private static float compactXf(float x) { return 64f + (x - 64f) * PLAN_SPACING_SCALE; }
    private static float compactYf(float y) { return 8f + (y - 8f) * PLAN_SPACING_SCALE; }

    private static void hall(TileType[][] map,int x,int y,int w,int h) {
        for(int yy=Math.max(0,y);yy<Math.min(map.length,y+h);yy++)
            for(int xx=Math.max(0,x);xx<Math.min(map[0].length,x+w);xx++) map[yy][xx]=TileType.FLOOR;
    }
    private static void drawRoomFloor(TileType[][] map,RoomPlan p) {
        for(int y=p.y+1;y<p.y+p.h-1;y++) for(int x=p.x+1;x<p.x+p.w-1;x++) {
            boolean cut=p.notchW>0&&x>=p.notchX&&x<p.notchX+p.notchW
                &&y>=p.notchY&&y<p.notchY+p.notchH;
            if(!cut) map[y][x]=TileType.FLOOR;
        }
    }
    private static void carveDoorApproaches(TileType[][] map,List<RoomPlan> plans) {
        // Two-tile travel corridors begin outside a short five-cell door vestibule,
        // preserving the wall support required around each one-cell opening.
        for(RoomPlan p:plans) {
            switch(p.side) {
                case EAST:
                    int eastSpine=(p.name.equals("CCTV")||p.name.equals("LIBRARY"))
                        ?EAST_SPINE_X:MAIN_SPINE_X;
                    int eastDistance=Math.max(1,eastSpine-p.doorX-1);
                    int eastVestibule=Math.min(2,eastDistance);
                    hall(map,p.doorX+1,p.doorY-2,eastVestibule,5);
                    hall(map,p.doorX+1+eastVestibule,p.doorY-1,
                        Math.max(1,eastDistance-eastVestibule),2);
                    if (p.name.equals("CCTV") || p.name.equals("LIBRARY")) {
                        hall(map,EAST_SPINE_X,Math.min(MAIN_CROSS_Y,p.doorY),2,
                            Math.abs(p.doorY-MAIN_CROSS_Y)+2);
                    }
                    break;
                case WEST:
                    int westSpine=p.x>=MAIN_SPINE_X?MAIN_SPINE_X:WEST_SPINE_X;
                    int westDistance=Math.max(1,p.doorX-westSpine-1);
                    hall(map,p.doorX-2,p.doorY-2,2,5);
                    hall(map,westSpine,p.doorY-1,Math.max(1,westDistance-1),2);
                    if(p.x<MAIN_SPINE_X)
                        hall(map,WEST_SPINE_X,Math.min(MAIN_CROSS_Y,p.doorY),2,
                            Math.abs(p.doorY-MAIN_CROSS_Y)+2);
                    break;
                case NORTH:
                    int northDistance=Math.max(1,MAIN_CROSS_Y-p.doorY-1);
                    int northVestibule=Math.min(2,northDistance);
                    hall(map,p.doorX-2,p.doorY+1,5,northVestibule);
                    hall(map,p.doorX-1,p.doorY+1+northVestibule,2,
                        Math.max(1,northDistance-northVestibule));
                    break;
                default:
                    int southDistance=Math.max(1,p.doorY-SOUTH_SPINE_Y-1);
                    int southVestibule=Math.min(2,southDistance);
                    hall(map,p.doorX-2,p.doorY-southVestibule,5,southVestibule);
                    hall(map,p.doorX-1,SOUTH_SPINE_Y,2,
                        Math.max(1,southDistance-southVestibule));
                    break;
            }
        }
    }
    private static List<LaboratoryRoomArea> areas(List<RoomPlan> plans) {
        List<LaboratoryRoomArea> out=new ArrayList<>();
        for(RoomPlan p:plans)out.add(new LaboratoryRoomArea(p.name,p.x,p.y,p.w,p.h));
        return out;
    }
    private static LootContainerPlacement findContainer(String id,String roomName,
            List<RoomPlan> plans,List<LaboratoryProp> props,TileType[][] map) {
        RoomPlan plan=null;
        for(RoomPlan candidate:plans)if(candidate.name.equals(roomName)){plan=candidate;break;}
        if(plan==null)throw new IllegalArgumentException("missing room "+roomName);
        float bestScore=Float.POSITIVE_INFINITY,bestX=Float.NaN,bestY=Float.NaN;
        for(int y=plan.y+1;y<plan.y+plan.h-1;y++)for(int x=plan.x+1;x<plan.x+plan.w-1;x++) {
            if(map[y][x]!=TileType.FLOOR)continue;
            float cx=x+.5f,cy=y+.5f;
            float doorDx=cx-(plan.doorX+.5f),doorDy=cy-(plan.doorY+.5f);
            if(doorDx*doorDx+doorDy*doorDy<6.25f)continue;
            Rectangle bounds=new Rectangle(x+.08f,y+.08f,.84f,.48f);
            boolean occupied=false;
            for(LaboratoryProp prop:props) {
                Rectangle collision=prop.collision();
                if(collision!=null&&bounds.overlaps(collision)){occupied=true;break;}
            }
            if(occupied)continue;
            float dx=cx-(plan.x+plan.w/2f),dy=cy-(plan.y+plan.h/2f);
            float score=dx*dx+dy*dy;
            if(score<bestScore){bestScore=score;bestX=x+.5f;bestY=y+.5f;}
        }
        if(Float.isNaN(bestX))throw new IllegalStateException("no reachable container cell in "+roomName);
        return new LootContainerPlacement(id,bestX,bestY);
    }
    private static boolean adjacentToFloor(TileType[][] map,int x,int y) {
        for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++) {
            int nx=x+dx,ny=y+dy;
            if(nx>=0&&ny>=0&&ny<map.length&&nx<map[0].length
                &&(map[ny][nx]==TileType.FLOOR||map[ny][nx]==TileType.DOORWAY_OPEN))return true;
        }
        return false;
    }
    private static void addRoomFurniture(List<LaboratoryProp> out,RoomPlan p,
                                        List<WallFurnitureCoverageReport> coverage,
                                        TileType[][] map) {
        String[][] kits={
            {"science_lab/chemistry_workbench/front","science_lab/chemistry_fume_hood/front","general_lab/storage_cabinet/left_side","general_lab/workbench/right_side"},
            {"science_lab/biosafety_cabinet/front","science_lab/specimen_cabinet/left_side","science_lab/biosafety_cabinet/right_side","science_lab/chemistry_workbench/front"},
            {"general_lab/workbench/front","general_lab/sink_counter/left_side","general_lab/storage_cabinet/right_side","general_lab/workbench/front"},
            {"science_lab/chemistry_workbench/front","general_lab/fume_hood/left_side","general_lab/storage_cabinet/right_side","general_lab/sink_counter/front"},
            {"information_cctv/computer_workstation/front","information_cctv/server_rack/left_side","information_cctv/computer_workstation/right_side","information_cctv/server_rack/front"},
            {"information_cctv/cctv_console/front","information_cctv/server_rack/left_side","information_cctv/server_rack/right_side","information_cctv/computer_workstation/front"},
            {"science_lab/specimen_cabinet/front","science_lab/biosafety_cabinet/left_side","general_lab/storage_cabinet/right_side","science_lab/specimen_cabinet/front"},
            {"archive_library/bookshelf/front","archive_library/archive_shelf/left_side","archive_library/filing_cabinet/right_side","archive_library/bookshelf/front"},
            {"lounge_sleeping/sofa/front","lounge_sleeping/sofa/left_side","lounge_sleeping/locker_bank/right_side","lounge_sleeping/sofa/front"},
            {"lounge_sleeping/bunk_bed/front","lounge_sleeping/locker_bank/left_side","lounge_sleeping/bunk_bed/right_side","lounge_sleeping/locker_bank/front"},
            {"restroom_shower/toilet/front","restroom_shower/sink_counter/left_side","restroom_shower/toilet/front","restroom_shower/sink_counter/right_side"},
            {"lounge_sleeping/locker_bank/front","lounge_sleeping/locker_bank/left_side","lounge_sleeping/locker_bank/right_side","lounge_sleeping/bunk_bed/front"},
            {"restroom_shower/shower_stall/front","restroom_shower/sink_counter/left_side","restroom_shower/shower_stall/right_side","restroom_shower/shower_stall/front"},
            {"archive_library/filing_cabinet/front","archive_library/archive_shelf/left_side","archive_library/filing_cabinet/right_side","archive_library/archive_shelf/front"},
            {"pantry/kitchenette/front","pantry/refrigerator/left_side","pantry/coffee_station/right_side","pantry/microwave_cabinet/front"},
            {"information_cctv/computer_workstation/front","information_cctv/server_rack/left_side","pantry/refrigerator/right_side","information_cctv/computer_workstation/front"}
        };
        String[] names={"CHEMISTRY","BIOLOGY","GENERAL LAB","PREP ROOM","INFORMATION","CCTV","SPECIMEN","LIBRARY","LOUNGE","SLEEPING","RESTROOM","LOCKER","SHOWER","RECORDS","PANTRY","ENTRANCE"};
        int index=0;while(index<names.length&&!names[index].equals(p.name))index++;
        // Fill all four wall runs before creating center islands. Wall slots use
        // measured opaque alpha spans, so transparent canvas margins do not create
        // fake coverage or visible gaps. Door cells, jambs, corners and recesses are
        // excluded from the usable run.
        // Wall-mounted artwork is anchored at its visible alpha edge against the
        // room-facing wall boundary. Wall cells themselves are never used as floor.
        addWallFurniture(out, coverage, p, map, "NORTH", p.x + 1, p.x + p.w - 1,
            p.y + p.h - 1, true, kits[index][0], kits[index][1]);
        addWallFurniture(out, coverage, p, map, "SOUTH", p.x + 1, p.x + p.w - 1,
            p.y + 1, true, kits[index][1], kits[index][0]);
        addWallFurniture(out, coverage, p, map, "WEST", p.y + 1, p.y + p.h - 1,
            p.x + 1, false, kits[index][2], kits[index][0]);
        addWallFurniture(out, coverage, p, map, "EAST", p.y + 1, p.y + p.h - 1,
            p.x + p.w - 1, false, kits[index][3], kits[index][1]);
        // Compact central equipment islands are offset from the doorway and from
        // one another; small rooms keep a clear center lane.
        if(p.w>=9&&p.h>=8) {
            int columns=p.w>=18?3:2;
            int rows=p.w>=9&&p.h>=8?2:1;
            for(int row=0;row<rows;row++) {
                float y=p.y+p.h*(row==0?.40f:.25f);
                for(int col=0;col<columns;col++) {
                    float x=p.x+p.w*(columns==2?(col==0?.34f:.66f):(.30f+col*.20f));
                    if(!insideNotch(p,x,y))
                        addV7(out,kits[index][(row+col)%4],x,y,1.85f,.34f,p,map);
                }
            }
        }
    }

    private static void addWallFurniture(List<LaboratoryProp> out,
            List<WallFurnitureCoverageReport> reports, RoomPlan plan, TileType[][] map, String wall,
            int firstCell, int endCell, int fixedCoordinate, boolean horizontal,
            String primary, String alternate) {
        int runStart = -1;
        float usable = 0f, covered = 0f;
        for (int cell = firstCell; cell <= endCell; cell++) {
            boolean available = cell < endCell && wallCellAvailable(plan, wall, cell);
            if (available) {
                if (runStart < 0) runStart = cell;
                continue;
            }
            if (runStart >= 0) {
                float start = runStart, finish = cell;
                float length = finish - start;
                usable += length;
                covered += fillWallRun(out, plan, map, wall, horizontal, fixedCoordinate,
                    start, finish, primary, alternate);
                runStart = -1;
            }
        }
        reports.add(new WallFurnitureCoverageReport(plan.name, wall, usable, covered));
    }

    private static boolean wallCellAvailable(RoomPlan p, String wall, int along) {
        float doorAlong = "NORTH".equals(wall) || "SOUTH".equals(wall) ? p.doorX : p.doorY;
        boolean doorOnThisWall = ("NORTH".equals(wall) && p.side == Side.NORTH)
            || ("SOUTH".equals(wall) && p.side == Side.SOUTH)
            || ("EAST".equals(wall) && p.side == Side.EAST)
            || ("WEST".equals(wall) && p.side == Side.WEST);
        if (doorOnThisWall && Math.abs(along - doorAlong) <= 1) return false;
        float x = "EAST".equals(wall) ? p.x + p.w - 2
            : "WEST".equals(wall) ? p.x + 1 : along + .5f;
        float y = "NORTH".equals(wall) ? p.y + p.h - 2
            : "SOUTH".equals(wall) ? p.y + 1 : along + .5f;
        return !insideNotch(p, x, y);
    }

    private static float fillWallRun(List<LaboratoryProp> out, RoomPlan plan, TileType[][] map, String wall,
            boolean horizontal, int fixed, float start, float finish,
            String primary, String alternate) {
        float length = finish - start;
        String primaryView = wallView(primary, horizontal, wall);
        String alternateView = wallView(alternate, horizontal, wall);
        float primarySpan = horizontal ? LaboratoryFurnitureCoverage.horizontalSpan(primaryView)
            : LaboratoryFurnitureCoverage.verticalSpan(primaryView);
        float alternateSpan = horizontal ? LaboratoryFurnitureCoverage.horizontalSpan(alternateView)
            : LaboratoryFurnitureCoverage.verticalSpan(alternateView);
        float pRatio = Math.max(.01f, primarySpan), aRatio = Math.max(.01f, alternateSpan);
        // Alternate themed assets, preferring a count that reaches 90% while keeping
        // the art close to the prior 2.15-tile visual scale.
        float average = (pRatio + aRatio) * .5f;
        int count = Math.max(1, (int)Math.ceil(length * .90f / (average * WALL_FURNITURE_HEIGHT)));
        float ratioSum = 0f;
        for (int i = 0; i < count; i++) ratioSum += (i % 2 == 0 ? pRatio : aRatio);
        float drawHeight = Math.min(WALL_FURNITURE_HEIGHT,
            Math.max(1.15f, (length * .90f - WALL_FURNITURE_GAP * (count - 1)) / ratioSum));
        float cursor = start;
        float occupied = 0f;
        for (int i = 0; i < count; i++) {
            String base = (i % 2 == 0 ? primaryView : alternateView);
            String id = "v7:" + base;
            float ratio = horizontal ? LaboratoryFurnitureCoverage.horizontalSpan(base)
                : LaboratoryFurnitureCoverage.verticalSpan(base);
            float span = ratio * drawHeight;
            // Front views are centered horizontally around their anchor. Side views
            // use bottom-center anchoring, so their visible vertical interval starts
            // at the anchor and grows upward; treating it as a center caused gaps.
            float width = span;
            float height = ratio * drawHeight;
            float x = horizontal ? cursor + span * .5f
                : ("WEST".equals(wall) ? fixed + width * .5f : fixed - width * .5f);
            float y = horizontal
                ? ("NORTH".equals(wall) ? fixed - height : fixed)
                : cursor;
            if (addWallProp(out, id, x, y, drawHeight, plan, map, wall)) {
                cursor += span + WALL_FURNITURE_GAP;
                occupied += span;
            } else {
                // Collision rejection retries the other wall-compatible themed view.
                String fallback = (i % 2 == 0 ? alternateView : primaryView);
                String fallbackId = "v7:" + fallback;
                float fallbackRatio = horizontal
                    ? LaboratoryFurnitureCoverage.horizontalSpan(fallback)
                    : LaboratoryFurnitureCoverage.verticalSpan(fallback);
                float fallbackSpan = fallbackRatio * drawHeight;
                float fallbackWidth = fallbackSpan;
                float fallbackHeight = fallbackRatio * drawHeight;
                float fallbackX = horizontal ? cursor + fallbackSpan * .5f
                    : ("WEST".equals(wall) ? fixed + fallbackWidth * .5f : fixed - fallbackWidth * .5f);
                float fallbackY = horizontal
                    ? ("NORTH".equals(wall) ? fixed - fallbackHeight : fixed) : cursor;
                if (addWallProp(out, fallbackId, fallbackX, fallbackY, drawHeight, plan, map, wall)) {
                    cursor += fallbackSpan + WALL_FURNITURE_GAP;
                    occupied += fallbackSpan;
                } else {
                    // A notch/corner may leave less room than the measured art span.
                    // Advance past this slot so later attempts do not pile up here.
                    cursor += Math.max(.35f, Math.min(span, fallbackSpan)) + WALL_FURNITURE_GAP;
                }
            }
        }
        return Math.min(length, occupied);
    }

    private static String wallView(String asset, boolean horizontal, String wall) {
        String base = asset.substring(0, asset.lastIndexOf('/'));
        return base + (horizontal ? "/front"
            : ("WEST".equals(wall) ? "/left_side" : "/right_side"));
    }

    private static boolean addWallProp(List<LaboratoryProp> out, String id,
                                      float x, float y, float height, RoomPlan plan,
                                      TileType[][] map, String wall) {
        for (float scale : new float[]{1f, .85f, .70f, .55f}) {
            float scaledHeight = height * scale;
            float width = scaledHeight * LaboratoryFurnitureCoverage.horizontalSpan(id.substring(3));
            float artHeight = scaledHeight * LaboratoryFurnitureCoverage.verticalSpan(id.substring(3));
            float candidateX = x;
            float candidateY = y;
            if ("WEST".equals(wall)) candidateX = plan.x + 1 + width * .5f;
            else if ("EAST".equals(wall)) candidateX = plan.x + plan.w - 1 - width * .5f;
            if ("NORTH".equals(wall)) candidateY = plan.y + plan.h - 1 - artHeight;
            else if ("SOUTH".equals(wall)) candidateY = plan.y + 1;
            float depth = .30f;
            Rectangle collision;
            if ("NORTH".equals(wall)) collision = new Rectangle(candidateX-.15f, plan.y+plan.h-1+.20f, .30f, depth);
            else if ("SOUTH".equals(wall)) collision = new Rectangle(candidateX-.15f, plan.y+.20f, .30f, depth);
            else if ("WEST".equals(wall)) collision = new Rectangle(plan.x+.20f, candidateY+.05f, depth, .20f);
            else collision = new Rectangle(plan.x+plan.w-1+.20f, candidateY+.05f, depth, .20f);
            LaboratoryProp candidate = new LaboratoryProp(id,candidateX,candidateY,2.1f,scaledHeight,collision);
            if (addValidated(out, candidate, plan, map)) return true;
        }
        return false;
    }
    private static void addV7(List<LaboratoryProp> out,String asset,float x,float y,float height,float depth) {
        addV7(out,asset,x,y,height,depth,false);
    }

    private static void addV7(List<LaboratoryProp> out,String asset,float x,float y,
                              float height,float depth,boolean wallMounted) {
        String id="v7:"+asset;
        Rectangle collision=wallMounted
            ?new Rectangle(x-.25f,y,.5f,depth)
            :new Rectangle(x-.62f,y,1.24f,depth);
        for(LaboratoryProp placed:out) {
            Rectangle other=placed.collision();
            if(other!=null&&collision.overlaps(other)) return;
        }
        out.add(new LaboratoryProp(id,x,y,2.1f,height,collision));
    }

    private static void addV7(List<LaboratoryProp> out,String asset,float x,float y,
                              float height,float depth,RoomPlan plan,TileType[][] map) {
        String id="v7:"+asset;
        Rectangle collision=new Rectangle(x-.62f,y,1.24f,depth);
        addValidated(out,new LaboratoryProp(id,x,y,2.1f,height,collision),plan,map);
    }

    private static boolean addValidated(List<LaboratoryProp> out, LaboratoryProp candidate,
                                        RoomPlan plan, TileType[][] map) {
        FurniturePlacementValidator.RoomPlanBounds bounds = new FurniturePlacementValidator.RoomPlanBounds(
            plan.x + 1, plan.y + 1, plan.x + plan.w - 1, plan.y + plan.h - 1);
        Rectangle visible = FurniturePlacementValidator.visibleBounds(candidate);
        if (!FurniturePlacementValidator.isInsideRoomFloor(visible, bounds, map)) return false;
        Rectangle collision = candidate.collision();
        if (collision != null && !FurniturePlacementValidator.isInsideRoomFloor(collision, bounds, map)
                && !FurniturePlacementValidator.isOnWallSupport(collision, map)) return false;
        for (LaboratoryProp placed : out) {
            if (visible.overlaps(FurniturePlacementValidator.visibleBounds(placed))) return false;
            Rectangle other = placed.collision();
            if (collision != null && other != null && collision.overlaps(other)) return false;
        }
        out.add(candidate);
        return true;
    }

    private static void validateFurniturePlacements(List<LaboratoryProp> props,
            List<RoomPlan> plans, TileType[][] map) {
        for (LaboratoryProp prop : props) {
            Rectangle visible = FurniturePlacementValidator.visibleBounds(prop);
            RoomPlan owner = null;
            for (RoomPlan plan : plans) {
                FurniturePlacementValidator.RoomPlanBounds bounds = new FurniturePlacementValidator.RoomPlanBounds(
                    plan.x + 1, plan.y + 1, plan.x + plan.w - 1, plan.y + plan.h - 1);
                if (FurniturePlacementValidator.isInsideRoomFloor(visible, bounds, map)) {
                    owner = plan;
                    break;
                }
            }
            if (owner == null) throw new IllegalStateException(
                "Furniture visible area outside room floor: " + prop.id() + " @ " + prop.x() + "," + prop.y());
            Rectangle collision = prop.collision();
            if (collision != null) {
                FurniturePlacementValidator.RoomPlanBounds bounds = new FurniturePlacementValidator.RoomPlanBounds(
                    owner.x + 1, owner.y + 1, owner.x + owner.w - 1, owner.y + owner.h - 1);
                if (!FurniturePlacementValidator.isInsideRoomFloor(collision, bounds, map)
                        && !FurniturePlacementValidator.isOnWallSupport(collision, map))
                    throw new IllegalStateException("Furniture collision outside floor: " + prop.id());
            }
            for (LaboratoryProp other : props) if (other != prop
                    && visible.overlaps(FurniturePlacementValidator.visibleBounds(other)))
                throw new IllegalStateException("Furniture visible areas overlap: " + prop.id() + " / " + other.id());
        }
    }

    private static boolean insideNotch(RoomPlan p,float x,float y) {
        return p.notchW>0 && x>=p.notchX && x<p.notchX+p.notchW
            && y>=p.notchY && y<p.notchY+p.notchH;
    }


    public static LaboratoryLayout forDifficulty(GameDifficulty difficulty) {
        if(difficulty==GameDifficulty.EASY) return easyRoom();
        return variantRoom(difficulty==GameDifficulty.NORMAL?28:38,
            difficulty==GameDifficulty.NORMAL?20:28,difficulty==GameDifficulty.HARD);
    }

    private static LaboratoryLayout variantRoom(int w,int h,boolean hard) {
        TileType[][] logical=new TileType[h][w]; LaboratoryZone[][] zones=new LaboratoryZone[h][w];
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            logical[y][x]=x==0||y==0||x==w-1||y==h-1?TileType.WALL:TileType.FLOOR;
            zones[y][x]=LaboratoryZone.CENTRAL;
        }
        int[] vx=hard?new int[]{12,25}:new int[]{9,18};
        for(int wallX:vx)for(int y=1;y<h-1;y++)if(y<6||y>8)logical[y][wallX]=TileType.WALL;
        int dividerY=hard?14:10;
        for(int x=1;x<w-1;x++)if(x<5||x>7)logical[dividerY][x]=TileType.WALL;
        logical[0][2]=logical[0][3]=logical[0][4]=TileType.DOORWAY_OPEN;
        for(int wallX:vx) { logical[7][wallX]=TileType.DOORWAY_OPEN; logical[h-4][wallX]=TileType.DOORWAY_OPEN; }
        logical[dividerY][6]=TileType.DOORWAY_OPEN;
        logical[dividerY][14]=TileType.DOORWAY_OPEN;
        if(hard) logical[dividerY][31]=TileType.DOORWAY_OPEN;
        else logical[dividerY][22]=TileType.DOORWAY_OPEN;
        List<LaboratoryProp> props=new ArrayList<>();
        addProp(props,hard?"biosafety_cabinet_left":"chemistry_bench_north",w-4.5f,h-4f,1.7f,.55f);
        addProp(props,hard?"server_rack_north":"archive_shelf_north",4.5f,h-4f,1.8f,.55f);
        addProp(props,"experiment_island",w/2f,h/2f+1f,1.4f,.6f);
        Vector2 spawn=new Vector2(3.5f,2f);
        List<LootContainerPlacement> containers = new ArrayList<>();
        containers.add(new LootContainerPlacement("wooden_chest", w / 2f + .5f, 4.5f));
        containers.add(new LootContainerPlacement("epic_cabinet", w - 3.5f, h - 3.5f));
        containers.add(new LootContainerPlacement("legendary_cabinet", w - 4f, 7f));
        LaboratoryRoom room=new LaboratoryRoom(logical,spawn,null,null,props);
        for (LootContainerPlacement placement : containers)
            room.addWorldObstacle(new Rectangle(placement.x() - .42f, placement.y(), .84f, .48f));
        String[][] floor=new FloorVariantResolver(FloorVariantResolver.DEFAULT_SEED+w).resolve(logical,zones);
        String[][] overlay=new String[h][w],wall=new String[h][w],structure=new String[h][w];
        WallAutotiler auto=new WallAutotiler();
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            if(logical[y][x]==TileType.WALL)wall[y][x]=auto.tileId(room,x,y);
            else if(logical[y][x]==TileType.DOORWAY_OPEN) structure[y][x]=(x==vx[0]||x==vx[1])?"vertical_door_open":"horizontal_door_open";
        }
        return new LaboratoryLayout(room,new RoomVisuals(floor,overlay,wall,structure,null,props),containers);
    }

    private static void addProp(List<LaboratoryProp> props, String id, float x, float y,
                                float collisionWidth, float collisionDepth) {
        Rectangle collision = new Rectangle(x - collisionWidth / 2f, y, collisionWidth, collisionDepth);
        props.add(new LaboratoryProp(id, x, y, 2.5f, 2.4f, collision));
    }

    /** 9?? ????쇰뮚???꿔꺂???????醫딆졐????? LAB_WALL_TEST=1 ?????덊떀????影?쀫븸??????嶺뚮㉡??? */
    public static LaboratoryLayout wallConnectionGallery() {
        final int size = 9;
        TileType[][] logical = new TileType[size][size];
        String[][] floor = new String[size][size];
        String[][] overlay = new String[size][size];
        String[][] wall = new String[size][size];
        String[][] structure = new String[size][size];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                logical[y][x] = TileType.FLOOR;
                floor[y][x] = "floor_clean_a";
            }
        }
        // ???繹먮냱議???꿔꺂?????? ??醫딆쓧??汝??彛??뺤??꾩룆????5???꿔꺂???????닱???????熬곣뫖?삥납??????
        String[] horizontal = {"wall_end_left", "wall_horizontal", "wall_horizontal",
            "wall_horizontal", "wall_end_right"};
        for (int i = 0; i < horizontal.length; i++) wall[8][i + 1] = horizontal[i];
        String[] vertical = {"wall_end_bottom", "wall_vertical", "wall_vertical",
            "wall_vertical", "wall_end_top"};
        for (int i = 0; i < vertical.length; i++) wall[i + 2][8] = vertical[i];

        // ?熬곣뫖?삥납?????????雅??꿔꺂??袁ㅻ븶?ⓥ뫗????????됰Ŧ六????????F2 ID?? ??壤굿??좊㎧ ??????嶺뚮㉡???
        String[] corners = {"wall_outer_nw", "wall_outer_ne", "wall_outer_sw", "wall_outer_se",
            "wall_inner_nw", "wall_inner_ne", "wall_inner_sw", "wall_inner_se"};
        for (int i = 0; i < corners.length; i++) wall[6 - i / 4 * 2][i % 4 * 2] = corners[i];
        String[] junctions = {"wall_t_open_north", "wall_t_open_east", "wall_t_open_south",
            "wall_t_open_west", "wall_cross"};
        for (int i = 0; i < junctions.length; i++) wall[2][i] = junctions[i];
        structure[0][0] = "horizontal_door_closed";
        structure[0][2] = "horizontal_door_open";
        structure[0][4] = "vertical_door_closed";
        structure[0][6] = "vertical_door_open";
        for (int y = 0; y < size; y++)
            for (int x = 0; x < size; x++)
                if (wall[y][x] != null) logical[y][x] = TileType.WALL;
        return new LaboratoryLayout(new LaboratoryRoom(logical, new Vector2(4.5f, 3.5f)),
            new RoomVisuals(floor, overlay, wall, structure));
    }

    /** Live 9x9 assembly check: a closed horizontal door and an open vertical door. */
    public static LaboratoryLayout wallDoorAssemblyTestRoom() {
        return fromRows(new String[]{
            ".........",
            ".###h###.",
            ".........",
            ".......#.",
            "....P..#.",
            ".......g.",
            ".......#.",
            ".......#.",
            "........."
        });
    }

    public static LaboratoryLayout fromRows(String[] rows) {
        int h = rows.length;
        int w = rows[0].length();

        TileType[][] logical = new TileType[h][w];
        LaboratoryZone[][] zones = new LaboratoryZone[h][w];
        Vector2 spawn = new Vector2(w / 2f, h / 2f);

        for (int r = 0; r < h; r++) {
            int ty = h - 1 - r; // ?????萸?????????= ??醫딆쓧?????y
            String row = rows[r];
            for (int tx = 0; tx < w; tx++) {
                char c = row.charAt(tx);
                logical[ty][tx] = logicalFor(c);
                zones[ty][tx] = zoneFor(tx, ty, w, h);
                if (c == 'P') spawn = new Vector2(tx + 0.5f, ty + 0.5f);
            }
        }

        // ????怨룹툒???繹먮굟瑗?+ hazard (???????????????곗뒩泳??
        OverlayResolver.Result ovr = new OverlayResolver(OverlayResolver.DEFAULT_SEED).resolve(logical, zones);
        DoorPlacementResolver.Result placedDoors = new DoorPlacementResolver().resolve(logical);
        LaboratoryRoom room = new LaboratoryRoom(logical, spawn, ovr.hazard, placedDoors.doors);

        WallAutotiler autotiler = new WallAutotiler();
        String[][] wall = new String[h][w];
        for (int ty = 0; ty < h; ty++) {
            for (int tx = 0; tx < w; tx++) {
                if (logical[ty][tx] == TileType.WALL) {
                    wall[ty][tx] = autotiler.tileId(room, tx, ty);
                }
                // ???戮?럡??? ???????????????◈?뙿????????????濚밸Ŧ?????筌?????嚥싳쉶瑗??꾧틚???? ????⑤９??.
                if (placedDoors.structure[ty][tx] != null) wall[ty][tx] = null;
            }
        }

        // ?熬곣뫖利??????⑤슢堉???+ ?嚥▲굧?????????怨룹툒??濚밸Ŧ????????⑥ш뎐 ?????????饔앹뼃爾??嚥▲굧????
        String[][] floor = new FloorVariantResolver(FloorVariantResolver.DEFAULT_SEED).resolve(logical, zones);
        applyWarningBoundary(floor, w, h);

        RoomVisuals visuals = new RoomVisuals(floor, ovr.overlay, wall,
            placedDoors.structure, placedDoors.doors);
        WallTopologyValidator validator = new WallTopologyValidator();
        validator.validateWallTopology(room, visuals);
        validator.validateDoorPlacement(room, visuals);
        validator.validateVisualConnections(room, visuals);
        return new LaboratoryLayout(room, visuals);
    }

    /** ????⑥ш뎐 ?嚥▲굥?멩납?????饔앹뼃爾??嚥▲굧????嚥싳쉶瑗??꾧틡?????勇??????꿔꺂???ル쇀? ?嚥▲굧?????2??3???????띾뼏 ???궰??? 嚥싳쉶瑗ц짆堉샕??. */
    private static void applyWarningBoundary(String[][] floor, int w, int h) {
        // ????????????썹땟??????裕∽┼?????⑥ш뎐????饔앹뼃爾???ty8 ?熬곣뫖利??濚?????썹땟??ty7)
        if (w == 20 && h == 15) {
            if (floor[7][13] != null) floor[7][13] = "floor_warning_north";
            if (floor[7][14] != null) floor[7][14] = "floor_warning_north";
        }
    }

    static TileType logicalFor(char c) {
        switch (c) {
            case '#': return TileType.WALL;
            case 'd': case 'h': return TileType.DOOR_CLOSED;
            case 'o': case 'g': return TileType.DOORWAY_OPEN;
            default: return TileType.FLOOR;
        }
    }

    static LaboratoryZone zoneFor(int tx, int ty, int w, int h) {
        if (w != 20 || h != 15) return LaboratoryZone.CENTRAL;
        if (tx >= 13 && ty >= 9) return LaboratoryZone.CONTAM;
        if (tx >= 13 && ty >= 1 && ty <= 5) return LaboratoryZone.MAINT;
        if (tx >= 1 && tx <= 6 && ty >= 1 && ty <= 5) return LaboratoryZone.ENTRANCE;
        return LaboratoryZone.CENTRAL;
    }
}

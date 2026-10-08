package io.github.some_example_name.world;

import com.badlogic.gdx.math.Vector2;

/**
 * Builds logical tiles, four visual layers, doors, collision and hazards from map rows.
 * Rows start at world north. Door markers choose only initial state: orientation is
 * inferred from adjacent walls and requires a five-cell straight run.
 */
public final class LaboratoryLayout {

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

    private LaboratoryLayout(LaboratoryRoom room, RoomVisuals visuals) {
        this.room = room;
        this.visuals = visuals;
    }

    public LaboratoryRoom room() { return room; }
    public RoomVisuals visuals() { return visuals; }

    public static LaboratoryLayout testRoom() {
        return fromRows(TEST_ROOM);
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

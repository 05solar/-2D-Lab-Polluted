package io.github.some_example_name.world;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import java.util.ArrayList;
import java.util.List;

/** Fixed 14x10 safe shop tent. Coordinates are logical tiles; y increases north. */
public final class HeadquartersLayout {
    public static final int WIDTH = 14, HEIGHT = 10;
    public static final int DOOR_X = 6;
    private static final int SOUTH_WALL_Y = 1, NORTH_WALL_Y = HEIGHT - 1;
    private final HeadquartersRoom room;
    private final HeadquartersVisuals visuals;

    private HeadquartersLayout(HeadquartersRoom room, HeadquartersVisuals visuals) {
        this.room = room;
        this.visuals = visuals;
    }

    public HeadquartersRoom room() { return room; }
    public HeadquartersVisuals visuals() { return visuals; }

    public static HeadquartersLayout create() {
        String[][] floor = new String[HEIGHT][WIDTH];
        String[][] decor = new String[HEIGHT][WIDTH];
        String[][] wall = new String[HEIGHT][WIDTH];
        boolean[][] solid = new boolean[HEIGHT][WIDTH];

        // Interior canvas plus an unpainted, dark exterior row outside the south entrance.
        for (int y = SOUTH_WALL_Y; y <= NORTH_WALL_Y; y++) {
            for (int x = 0; x < WIDTH; x++) {
                floor[y][x] = "canvas_floor";
                int variation = Math.floorMod(x * 13 + y * 7, 13);
                if (variation == 0) decor[y][x] = "canvas_floor_patch";
                else if (variation == 5) decor[y][x] = "canvas_floor_seam";
            }
        }
        for (int x = 0; x < WIDTH; x++) solid[0][x] = x != DOOR_X;

        // Compact tent perimeter, with the only opening centered on the south wall.
        for (int x = 0; x < WIDTH; x++) {
            solid[NORTH_WALL_Y][x] = true;
            wall[NORTH_WALL_Y][x] = "north_tent_wall";
            solid[SOUTH_WALL_Y][x] = x != DOOR_X;
            wall[SOUTH_WALL_Y][x] = x == DOOR_X ? "open_tent_flap" : "north_tent_wall";
        }
        for (int y = SOUTH_WALL_Y; y <= NORTH_WALL_Y; y++) {
            solid[y][0] = true;
            solid[y][WIDTH - 1] = true;
            wall[y][0] = "west_tent_wall";
            wall[y][WIDTH - 1] = "east_tent_wall";
        }
        for (int x = 2; x <= 10; x += 4) wall[NORTH_WALL_Y][x] = "north_wall_support";
        wall[NORTH_WALL_Y][0] = "outer_corner_wall";
        wall[NORTH_WALL_Y][WIDTH - 1] = "outer_corner_wall";
        wall[SOUTH_WALL_Y][0] = "outer_corner_wall";
        wall[SOUTH_WALL_Y][WIDTH - 1] = "outer_corner_wall";
        wall[NORTH_WALL_Y][10] = "utility_pocket_wall";
        wall[6][0] = "utility_cable_lantern";
        wall[5][WIDTH - 1] = "utility_cable_lantern";

        decor[SOUTH_WALL_Y][DOOR_X] = "rubber_entrance_mat";
        decor[2][DOOR_X] = "rubber_entrance_mat";
        wall[SOUTH_WALL_Y][4] = "sandbag_barrier";
        wall[SOUTH_WALL_Y][8] = "sandbag_barrier";
        solid[SOUTH_WALL_Y][4] = true;
        solid[SOUTH_WALL_Y][8] = true;

        List<HeadquartersProp> props = new ArrayList<>();
        // North center: terminal_desk includes the monitor; consoles flank it.
        props.add(HeadquartersProp.furniture("communications_console", 4.35f, 7.45f));
        props.add(HeadquartersProp.furniture("terminal_desk", 6.55f, 7.35f));
        props.add(HeadquartersProp.furniture("communications_console", 8.75f, 7.45f));
        props.add(HeadquartersProp.furniture("notice_board", 6.55f, 8.45f));
        // Northeast shop: front art naturally faces south; leave its southern approach clear.
        props.add(HeadquartersProp.vending(11.45f, 7.1f));
        // East supplies and west maintenance form compact wall-side rows.
        props.add(HeadquartersProp.furniture("supply_shelving", 11.85f, 5.25f));
        props.add(HeadquartersProp.furniture("reinforced_lockers", 11.85f, 3.35f));
        props.add(HeadquartersProp.furniture("water_dispenser", 9.8f, 2.05f));
        props.add(HeadquartersProp.furniture("stacked_supply_crates", 11.8f, 1.85f));
        props.add(HeadquartersProp.furniture("field_medical_cot", 1.75f, 6.4f));
        props.add(HeadquartersProp.furniture("medical_trolley", 2.6f, 5.05f));
        props.add(HeadquartersProp.furniture("folding_workbench", 1.75f, 3.95f));
        props.add(HeadquartersProp.furniture("portable_generator", 3.7f, 2.05f));
        props.add(HeadquartersProp.furniture("command_table", 5.1f, 2.05f));
        props.add(HeadquartersProp.furniture("stacked_supply_crates", 4.75f, 3.75f));

        Vector2 spawn = new Vector2(DOOR_X + 0.5f, 2.55f);
        Rectangle exit = new Rectangle(DOOR_X, 0f, 1f, 1f);
        return new HeadquartersLayout(new HeadquartersRoom(solid, props, spawn, exit),
            new HeadquartersVisuals(floor, decor, wall, props));
    }
}

package io.github.some_example_name.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Headquarters-only tile layers and bottom-anchored props; no laboratory atlas IDs. */
public final class HeadquartersVisuals {
    private final String[][] floor, decor, wall;
    private final List<HeadquartersProp> props;

    public HeadquartersVisuals(String[][] floor, String[][] decor, String[][] wall,
                               List<HeadquartersProp> props) {
        this.floor = floor;
        this.decor = decor;
        this.wall = wall;
        this.props = Collections.unmodifiableList(new ArrayList<>(props));
    }

    public int width() { return floor[0].length; }
    public int height() { return floor.length; }
    public String floorAt(int x, int y) { return floor[y][x]; }
    public String decorAt(int x, int y) { return decor[y][x]; }
    public String wallAt(int x, int y) { return wall[y][x]; }
    public List<HeadquartersProp> props() { return props; }
}

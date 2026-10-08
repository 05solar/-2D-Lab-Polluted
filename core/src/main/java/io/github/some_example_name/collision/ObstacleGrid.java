package io.github.some_example_name.collision;

import com.badlogic.gdx.math.Rectangle;

/** Tile solids plus precise ground footprints for large props. */
public interface ObstacleGrid extends SolidGrid {
    Iterable<Rectangle> obstacles();
}

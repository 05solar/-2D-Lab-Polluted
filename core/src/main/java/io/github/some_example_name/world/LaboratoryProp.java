package io.github.some_example_name.world;

import com.badlogic.gdx.math.Rectangle;

/** One roomkit prop, bottom-centered at a tile-space anchor. */
public final class LaboratoryProp {
    private final String id;
    private final float x, y, width, height;
    private final Rectangle collision;
    public LaboratoryProp(String id, float x, float y, float width, float height, Rectangle collision) {
        if (id == null || width <= 0f || height <= 0f) throw new IllegalArgumentException();
        this.id = id; this.x = x; this.y = y; this.width = width; this.height = height;
        this.collision = collision == null ? null : new Rectangle(collision);
    }
    public String id() { return id; }
    public float x() { return x; }
    public float y() { return y; }
    public float width() { return width; }
    public float height() { return height; }
    public Rectangle collision() { return collision == null ? null : new Rectangle(collision); }
}

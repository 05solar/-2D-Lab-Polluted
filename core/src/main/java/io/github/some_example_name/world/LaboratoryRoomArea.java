package io.github.some_example_name.world;

/** Named interior area used by the fixed easy-map plan and overview diagnostics. */
public final class LaboratoryRoomArea {
    private final String id;
    private final int x, y, width, height;

    public LaboratoryRoomArea(String id, int x, int y, int width, int height) {
        if (id == null || id.isEmpty() || width <= 0 || height <= 0)
            throw new IllegalArgumentException("room area");
        this.id = id; this.x = x; this.y = y; this.width = width; this.height = height;
    }

    public String id() { return id; }
    public int x() { return x; }
    public int y() { return y; }
    public int width() { return width; }
    public int height() { return height; }
    public float centerX() { return x + width / 2f; }
    public float centerY() { return y + height / 2f; }
}

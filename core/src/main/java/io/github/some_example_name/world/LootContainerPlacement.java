package io.github.some_example_name.world;

/** Immutable map-owned placement for a single-run loot container. */
public final class LootContainerPlacement {
    private final String id;
    private final float x;
    private final float y;

    public LootContainerPlacement(String id, float x, float y) {
        if (id == null || id.isEmpty()) throw new IllegalArgumentException("id");
        this.id = id;
        this.x = x;
        this.y = y;
    }

    public String id() { return id; }
    public float x() { return x; }
    public float y() { return y; }
}

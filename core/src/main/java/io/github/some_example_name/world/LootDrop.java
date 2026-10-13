package io.github.some_example_name.world;

import io.github.some_example_name.inventory.LootItem;

/** A visible, collectible item left at a chest or swap location. */
public final class LootDrop {
    private final LootItem item;
    private final float x,y;
    private boolean collected;
    public LootDrop(LootItem item,float x,float y){if(item==null)throw new IllegalArgumentException();this.item=item;this.x=x;this.y=y;}
    public LootItem item(){return item;} public float x(){return x;} public float y(){return y;}
    public boolean collected(){return collected;} public void collect(){collected=true;}
}

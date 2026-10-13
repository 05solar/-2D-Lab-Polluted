package io.github.some_example_name.inventory;

/** Immutable item row from loot/loot_tables.json. */
public final class LootItem {
    public final String id, name, rarity;
    public final int price, atlasColumn;
    public final float probability;
    public LootItem(String id,String name,String rarity,int price,float probability,int column) {
        this.id=id; this.name=name; this.rarity=rarity; this.price=price; this.probability=probability; this.atlasColumn=column;
    }
}

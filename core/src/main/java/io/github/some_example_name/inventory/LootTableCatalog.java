package io.github.some_example_name.inventory;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Parses the supplied loot tables and offers seedable weighted rolls. */
public final class LootTableCatalog {
    private final Map<String,List<LootItem>> tables=new HashMap<>();
    public LootTableCatalog(String json) {
        JsonValue root=new JsonReader().parse(json);
        for(JsonValue c=root.get("containers").child;c!=null;c=c.next) {
            String rarity=c.getString("rarity"); List<LootItem> items=new ArrayList<>();
            for(JsonValue i=c.get("items").child;i!=null;i=i.next)
                items.add(new LootItem(i.getString("id"),i.getString("name"),rarity,
                    i.getInt("price"),i.getFloat("probability"),i.getInt("atlasColumn")));
            tables.put(c.getString("id"),Collections.unmodifiableList(items));
        }
    }
    public List<LootItem> items(String container) {
        List<LootItem> list=tables.get(container); if(list==null) throw new IllegalArgumentException(container); return list;
    }
    public LootItem roll(String container,Random random) {
        List<LootItem> items=items(container); float total=0f;
        for(LootItem i:items) total+=i.probability;
        float choice=random.nextFloat()*total;
        for(LootItem i:items) { choice-=i.probability; if(choice<0f) return i; }
        return items.get(items.size()-1);
    }
    public LootItem find(String id) {
        for(List<LootItem> list:tables.values()) for(LootItem item:list) if(item.id.equals(id)) return item;
        return null;
    }
}

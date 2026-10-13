package io.github.some_example_name.inventory;

import org.junit.Test;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import static org.junit.Assert.*;

public class LootTableCatalogTest {
    @Test public void suppliedTablesParseAndRollRepeatably() throws Exception {
        String json=new String(Files.readAllBytes(Paths.get("../assets/loot/loot_tables.json")),StandardCharsets.UTF_8);
        LootTableCatalog catalog=new LootTableCatalog(json);
        assertEquals(8,catalog.items("wooden_chest").size());
        assertEquals(7,catalog.items("epic_cabinet").size());
        assertEquals(catalog.roll("wooden_chest",new Random(47)).id,
            catalog.roll("wooden_chest",new Random(47)).id);
        assertTrue(catalog.items("legendary_cabinet").stream().anyMatch(i->i.price==50000));
        assertRarerItemsHaveLowerOdds(catalog, "wooden_chest");
        assertRarerItemsHaveLowerOdds(catalog, "epic_cabinet");
        assertRarerItemsHaveLowerOdds(catalog, "legendary_cabinet");
    }

    private static void assertRarerItemsHaveLowerOdds(LootTableCatalog catalog, String id) {
        LootItem cheapest = catalog.items(id).get(0), dearest = cheapest;
        for (LootItem item : catalog.items(id)) {
            if (item.price < cheapest.price) cheapest = item;
            if (item.price > dearest.price) dearest = item;
        }
        assertTrue(id + " expensive-item probability", dearest.probability < cheapest.probability);
    }
}

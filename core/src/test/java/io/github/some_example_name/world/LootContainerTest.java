package io.github.some_example_name.world;

import io.github.some_example_name.inventory.LootTableCatalog;
import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;
import static org.junit.Assert.*;

public class LootContainerTest {
    @Test public void opensOnceAndEmitsOneRewardAfterAssetDuration() throws Exception {
        String json=new String(Files.readAllBytes(Paths.get("../assets/loot/loot_tables.json")),StandardCharsets.UTF_8);
        LootTableCatalog tables=new LootTableCatalog(json);
        LootContainer chest=new LootContainer("wooden_chest",1f,1f,.12f,new int[]{0,1,2,3,4,5,6,7},6);
        assertTrue(chest.open(tables,new Random(3)));
        assertFalse(chest.open(tables,new Random(3)));
        assertNull(chest.update(.71f));
        assertNotNull(chest.update(.02f));
        assertEquals(6,chest.frame());
        assertNull(chest.update(.13f));
        assertEquals(7,chest.frame());
    }
}

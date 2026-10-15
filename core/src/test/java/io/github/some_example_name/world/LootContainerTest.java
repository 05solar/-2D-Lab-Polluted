package io.github.some_example_name.world;

import io.github.some_example_name.inventory.LootTableCatalog;
import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;
import static org.junit.Assert.*;

public class LootContainerTest {
    @Test public void multiPressOpensExactlyAtRequiredCountAndOnlyOnce() throws Exception {
        String json=new String(Files.readAllBytes(Paths.get("../assets/loot/loot_tables.json")),StandardCharsets.UTF_8);
        LootTableCatalog tables=new LootTableCatalog(json);
        Random random=new Random(7);
        // 일반 캐비넷 5회: 4번은 진행, 5번째에 단 한 번 개봉.
        LootContainer epic=new LootContainer("epic_cabinet",1f,1f,.12f,new int[]{0,1,2,3},3,5);
        for(int i=1;i<=4;i++) {
            assertEquals(LootContainer.PressResult.PROGRESS,epic.press(tables,random));
            assertEquals("진행도는 컨테이너에 저장된다",i,epic.presses());
            assertFalse(epic.opened());
        }
        assertEquals(LootContainer.PressResult.OPENED,epic.press(tables,random));
        assertTrue(epic.opened());
        // 개봉 후 재입력: 재추첨/재개봉 없음.
        assertEquals(LootContainer.PressResult.ALREADY_OPEN,epic.press(tables,random));
        assertEquals(5,epic.presses());
        // 고급 캐비넷 8회.
        LootContainer legendary=new LootContainer("legendary_cabinet",1f,1f,.12f,new int[]{0,1},1,8);
        for(int i=1;i<=7;i++)
            assertEquals(LootContainer.PressResult.PROGRESS,legendary.press(tables,random));
        assertEquals(LootContainer.PressResult.OPENED,legendary.press(tables,random));
        // 나무 상자 1회: 첫 입력에 즉시 개봉.
        LootContainer wood=new LootContainer("wooden_chest",1f,1f,.12f,new int[]{0,1},1,1);
        assertEquals(LootContainer.PressResult.OPENED,wood.press(tables,random));
        assertEquals(1f,wood.progressRatio(),.0001f);
    }

    @Test public void balanceConfigMapsContainerIdsToPressCounts() {
        io.github.some_example_name.config.BalanceConfig config=
            new io.github.some_example_name.config.BalanceConfig();
        assertEquals(1,config.requiredPresses("wooden_chest"));
        assertEquals(5,config.requiredPresses("epic_cabinet"));
        assertEquals(8,config.requiredPresses("legendary_cabinet"));
    }

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

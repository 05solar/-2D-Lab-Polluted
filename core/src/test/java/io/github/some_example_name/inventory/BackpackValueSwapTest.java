package io.github.some_example_name.inventory;

import org.junit.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class BackpackValueSwapTest {
    @Test public void fullBagReplacesItsLowestValueItem() {
        Backpack bag=new Backpack(); Map<String,Integer> prices=new HashMap<>();
        for(int i=0;i<8;i++){String id="item"+i; bag.collect(id); prices.put(id,100+i*100);}
        prices.put("new_item", 2500);
        String removed=bag.replaceLowestValue("new_item",prices);
        assertEquals("item0",removed); assertEquals("new_item",bag.slot(0)); assertEquals(8,bag.count());
    }

    @Test public void equalOrLowerValueCannotReplaceTheLowestSlot() {
        Backpack bag=new Backpack(); Map<String,Integer> prices=new HashMap<>();
        for(int i=0;i<8;i++){String id="item"+i; bag.collect(id); prices.put(id,100+i*100);}
        prices.put("equal",100); prices.put("lower",99);
        assertNull(bag.replaceLowestValue("equal",prices));
        assertNull(bag.replaceLowestValue("lower",prices));
        assertEquals("item0",bag.slot(0)); assertEquals(8,bag.count());
    }
}

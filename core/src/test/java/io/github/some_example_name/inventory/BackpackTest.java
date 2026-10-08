package io.github.some_example_name.inventory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** 8칸 가방 모델: 8개 수집, 9번째 거부(물품 보존), 용량/슬롯, 인계 비우기. 화면 없이 검증. */
public class BackpackTest {

    @Test public void collectsUpToEightThenRejectsNinth() {
        Backpack bag = new Backpack();
        for (int i = 0; i < 8; i++) {
            assertTrue("8칸까지는 수집 성공 " + i, bag.collect("item_" + i));
        }
        assertTrue(bag.isFull());
        assertEquals(8, bag.count());
        assertFalse("9번째는 거부", bag.collect("item_9"));
        assertEquals("거부 시 개수 불변", 8, bag.count());
        assertEquals("9번째 물품은 들어가지 않음", null, bag.slot(8));
    }

    @Test public void slotsReflectInsertionOrder() {
        Backpack bag = new Backpack();
        bag.collect("a"); bag.collect("b");
        assertEquals("a", bag.slot(0));
        assertEquals("b", bag.slot(1));
        assertNull(bag.slot(2));
        assertEquals(8, bag.capacity());
    }

    @Test public void handOverReturnsItemsAndClears() {
        Backpack bag = new Backpack();
        for (int i = 0; i < 5; i++) bag.collect("item_" + i);
        java.util.List<String> delivered = bag.handOver();
        assertEquals(5, delivered.size());
        assertEquals(0, bag.count());
        assertFalse(bag.isFull());
        assertTrue("인계 후 다시 8칸 사용 가능", bag.collect("fresh"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsEmptyItemId() {
        new Backpack().collect("");
    }
}

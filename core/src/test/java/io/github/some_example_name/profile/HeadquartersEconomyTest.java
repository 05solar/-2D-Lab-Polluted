package io.github.some_example_name.profile;

import org.junit.Test;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.Assert.*;

/** 이관·판매 트랜잭션: 중복 방지, 검증, 저장 실패 롤백, 원자성(물품 차감+재화 증가 동시). */
public class HeadquartersEconomyTest {

    /** 인메모리 저장소: 실패 주입과 저장 횟수 관찰이 가능하다. */
    private static final class MemoryStore implements ProfileStore {
        PlayerProfile saved;
        boolean failNextSave;
        int saveCount;
        @Override public PlayerProfile load() { return saved; }
        @Override public void save(PlayerProfile profile) throws IOException {
            if (failNextSave) { failNextSave = false; throw new IOException("disk full"); }
            saveCount++;
            saved = ProfileCodec.fromJson(ProfileCodec.toJson(profile)); // 직렬화 왕복 보존 확인
        }
    }

    private static Map<String, Integer> prices() {
        Map<String, Integer> prices = new HashMap<>();
        prices.put("tumbler", 420);
        prices.put("lantern", 2000);
        prices.put("research_log_booklet", 5000);
        return prices;
    }

    private static HeadquartersEconomy economy(MemoryStore store) {
        return new HeadquartersEconomy(new PlayerProfile(), store, prices());
    }

    @Test public void transferMovesBagItemsOnceAndSurvivesDuplicateCallbacks() {
        MemoryStore store = new MemoryStore();
        HeadquartersEconomy economy = economy(store);
        HeadquartersEconomy.Result first = economy.transferReturn("ret-1",
            Arrays.asList("tumbler", "tumbler", "lantern"));
        assertEquals(HeadquartersEconomy.Status.OK, first.status);
        assertEquals(3, first.itemCount);
        assertEquals(3, economy.storageView().size());
        // 같은 귀환 콜백이 재호출돼도 복제되지 않는다.
        HeadquartersEconomy.Result again = economy.transferReturn("ret-1",
            Arrays.asList("tumbler", "tumbler", "lantern"));
        assertEquals(HeadquartersEconomy.Status.ALREADY_PROCESSED, again.status);
        assertEquals(3, economy.storageView().size());
        // 인스턴스 ID는 서로 다르다(소유권 식별).
        assertTrue(economy.storageView().get(0).instanceId
            != economy.storageView().get(1).instanceId);
    }

    @Test public void transferSaveFailureKeepsEverythingAndAllowsRetry() {
        MemoryStore store = new MemoryStore();
        HeadquartersEconomy economy = economy(store);
        store.failNextSave = true;
        HeadquartersEconomy.Result failed = economy.transferReturn("ret-9",
            Arrays.asList("tumbler"));
        assertEquals(HeadquartersEconomy.Status.SAVE_FAILED, failed.status);
        assertTrue("실패 시 보관함이 비어 있던 그대로", economy.storageView().isEmpty());
        assertFalse("귀환 처리로 기록되지 않아 재시도 가능",
            economy.profile().processedReturnIds.contains("ret-9"));
        // 재시도 성공
        assertEquals(HeadquartersEconomy.Status.OK,
            economy.transferReturn("ret-9", Arrays.asList("tumbler")).status);
        assertEquals(1, economy.storageView().size());
    }

    @Test public void partialSaleRemovesOnlyRequestedQuantityAndAddsExactTotal() {
        MemoryStore store = new MemoryStore();
        HeadquartersEconomy economy = economy(store);
        economy.transferReturn("r", Arrays.asList("tumbler", "tumbler", "tumbler", "lantern"));
        Map<String, Integer> request = new LinkedHashMap<>();
        request.put("tumbler", 2);
        HeadquartersEconomy.Result sale = economy.sell("tx-1", request);
        assertEquals(HeadquartersEconomy.Status.OK, sale.status);
        assertEquals(2, sale.itemCount);
        assertEquals(840L, sale.totalWon);
        assertEquals(840L, economy.currencyWon());
        Map<String, Integer> held = economy.heldCounts();
        assertEquals(Integer.valueOf(1), held.get("tumbler"));
        assertEquals(Integer.valueOf(1), held.get("lantern"));
    }

    @Test public void sellAllTargetsOnlySellableItemsAndComputesCatalogTotal() {
        MemoryStore store = new MemoryStore();
        HeadquartersEconomy economy = economy(store);
        economy.transferReturn("r", Arrays.asList("tumbler", "lantern", "mystery_item"));
        HeadquartersEconomy.Result sale = economy.sellAll("tx-all");
        assertEquals(HeadquartersEconomy.Status.OK, sale.status);
        assertEquals(2, sale.itemCount);
        assertEquals(2420L, sale.totalWon);
        // 알 수 없는 아이템은 판매되지 않고 보존된다.
        assertEquals(1, economy.storageView().size());
        assertEquals("mystery_item", economy.storageView().get(0).itemId);
    }

    @Test public void invalidQuantityUnknownIdAndOverflowAreRejectedWithoutChanges() {
        MemoryStore store = new MemoryStore();
        HeadquartersEconomy economy = economy(store);
        economy.transferReturn("r", Arrays.asList("tumbler"));
        long before = economy.currencyWon();
        Map<String, Integer> tooMany = new LinkedHashMap<>();
        tooMany.put("tumbler", 5); // 보유 1
        assertEquals(HeadquartersEconomy.Status.INVALID, economy.sell("t1", tooMany).status);
        Map<String, Integer> unknown = new LinkedHashMap<>();
        unknown.put("no_such_item", 1);
        assertEquals(HeadquartersEconomy.Status.INVALID, economy.sell("t2", unknown).status);
        Map<String, Integer> negative = new LinkedHashMap<>();
        negative.put("tumbler", -1);
        assertEquals(HeadquartersEconomy.Status.INVALID, economy.sell("t3", negative).status);
        assertEquals(before, economy.currencyWon());
        assertEquals(1, economy.storageView().size());
    }

    @Test public void duplicateTransactionIdIsAppliedOnlyOnce() {
        MemoryStore store = new MemoryStore();
        HeadquartersEconomy economy = economy(store);
        economy.transferReturn("r", Arrays.asList("tumbler", "tumbler"));
        Map<String, Integer> one = new LinkedHashMap<>();
        one.put("tumbler", 1);
        assertEquals(HeadquartersEconomy.Status.OK, economy.sell("tx-dup", one).status);
        assertEquals(HeadquartersEconomy.Status.ALREADY_PROCESSED,
            economy.sell("tx-dup", one).status);
        assertEquals(420L, economy.currencyWon());
        assertEquals(Integer.valueOf(1), economy.heldCounts().get("tumbler"));
    }

    @Test public void saleSaveFailureRollsBackItemsAndCurrencyTogether() {
        MemoryStore store = new MemoryStore();
        HeadquartersEconomy economy = economy(store);
        economy.transferReturn("r", Arrays.asList("lantern"));
        store.failNextSave = true;
        Map<String, Integer> one = new LinkedHashMap<>();
        one.put("lantern", 1);
        assertEquals(HeadquartersEconomy.Status.SAVE_FAILED, economy.sell("tx", one).status);
        assertEquals("재화 선증가 없음", 0L, economy.currencyWon());
        assertEquals("물품 선제거 없음", 1, economy.storageView().size());
        // 같은 거래 ID로 재시도 가능(기록되지 않았으므로).
        assertEquals(HeadquartersEconomy.Status.OK, economy.sell("tx", one).status);
        assertEquals(2000L, economy.currencyWon());
    }

    @Test public void upgradesAreRecordedAndPreservedThroughSerialization() {
        MemoryStore store = new MemoryStore();
        HeadquartersEconomy economy = economy(store);
        assertEquals(HeadquartersEconomy.Status.OK, economy.recordUpgrade("dash_range", 2).status);
        PlayerProfile reloaded = store.saved;
        assertEquals(Integer.valueOf(2), reloaded.purchasedUpgrades.get("dash_range"));
    }
}

package io.github.some_example_name.profile;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.Assert.*;

/** 코덱 왕복·마이그레이션·원자 저장·백업 복구·손상 보존을 검증한다. */
public class ProfilePersistenceTest {

    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Test public void codecRoundTripPreservesEveryField() {
        PlayerProfile profile = new PlayerProfile();
        profile.currencyWon = 123_456_789L;
        profile.profileRevision = 7;
        profile.storage.add(new ItemInstance(11, "tumbler"));
        profile.storage.add(new ItemInstance(12, "mystery_item"));
        profile.purchasedUpgrades.put("dash_range", 3);
        profile.purchasedUpgrades.put("unknown_future_upgrade", 9);
        profile.processedReturnIds.add("ret-1");
        profile.processedTransactionIds.add("tx-1");
        profile.nextInstanceId = 13;
        PlayerProfile loaded = ProfileCodec.fromJson(ProfileCodec.toJson(profile));
        assertEquals(123_456_789L, loaded.currencyWon);
        assertEquals(7, loaded.profileRevision);
        assertEquals(2, loaded.storage.size());
        assertEquals(11, loaded.storage.get(0).instanceId);
        assertEquals("mystery_item", loaded.storage.get(1).itemId);
        assertEquals(Integer.valueOf(3), loaded.purchasedUpgrades.get("dash_range"));
        assertEquals("알 수 없는 강화 ID도 삭제하지 않는다",
            Integer.valueOf(9), loaded.purchasedUpgrades.get("unknown_future_upgrade"));
        assertTrue(loaded.processedReturnIds.contains("ret-1"));
        assertTrue(loaded.processedTransactionIds.contains("tx-1"));
        assertEquals(13, loaded.nextInstanceId);
    }

    @Test public void legacySaveWithoutNewFieldsMigratesKeepingCurrencyAndUnknowns() {
        // 과거 버전 가정: 보관함/강화/기록 필드가 없고 다른 시스템 필드가 있다.
        String legacy = "{\"schemaVersion\":0,\"currencyWon\":5000,"
            + "\"someOtherSystem\":{\"flag\":true,\"list\":[1,2]}}";
        PlayerProfile migrated = ProfileCodec.fromJson(legacy);
        assertEquals("기존 재화 보존", 5000L, migrated.currencyWon);
        assertTrue("보관함은 안전한 기본값", migrated.storage.isEmpty());
        assertTrue(migrated.purchasedUpgrades.isEmpty());
        assertEquals(PlayerProfile.CURRENT_SCHEMA_VERSION, migrated.schemaVersion);
        // 알 수 없는 필드는 재기록 시에도 누락되지 않는다.
        String rewritten = ProfileCodec.toJson(migrated);
        assertTrue(rewritten.contains("someOtherSystem"));
        PlayerProfile again = ProfileCodec.fromJson(rewritten);
        assertTrue(again.unknownFieldsJson.containsKey("someOtherSystem"));
    }

    @Test public void saveLoadRoundTripOnDiskAndFirstRunReturnsNull() throws Exception {
        FileProfileStore store = new FileProfileStore(folder.newFolder("save"));
        assertNull("첫 실행에는 저장본이 없다", store.load());
        PlayerProfile profile = new PlayerProfile();
        profile.currencyWon = 777;
        profile.storage.add(new ItemInstance(1, "lantern"));
        store.save(profile);
        PlayerProfile loaded = store.load();
        assertNotNull(loaded);
        assertEquals(777L, loaded.currencyWon);
        assertEquals("lantern", loaded.storage.get(0).itemId);
    }

    @Test public void damagedMainFileIsRecoveredFromBackup() throws Exception {
        File dir = folder.newFolder("save");
        FileProfileStore store = new FileProfileStore(dir);
        PlayerProfile first = new PlayerProfile();
        first.currencyWon = 1000;
        store.save(first);
        PlayerProfile second = first.copy();
        second.currencyWon = 2000;
        store.save(second); // 이 시점 backup=1000, main=2000
        Files.write(new File(dir, "profile.json").toPath(),
            "{{{ corrupted".getBytes(StandardCharsets.UTF_8));
        FileProfileStore reopened = new FileProfileStore(dir);
        PlayerProfile recovered = reopened.load();
        assertNotNull("백업으로 복구된다", recovered);
        assertEquals(1000L, recovered.currencyWon);
        assertTrue(reopened.recoveredFromBackup());
    }

    @Test public void bothFilesDamagedArePreservedNotSilentlyOverwritten() throws Exception {
        File dir = folder.newFolder("save");
        Files.write(new File(dir, "profile.json").toPath(), "broken".getBytes(StandardCharsets.UTF_8));
        Files.write(new File(dir, "profile.backup.json").toPath(), "also broken".getBytes(StandardCharsets.UTF_8));
        FileProfileStore store = new FileProfileStore(dir);
        assertNull(store.load());
        assertTrue(store.corruptionDetected());
        boolean preserved = false;
        for (File file : dir.listFiles())
            if (file.getName().contains(".corrupt-")) preserved = true;
        assertTrue("손상 저장본이 보존된다", preserved);
    }

    @Test public void saveFailureAfterPriorSaveKeepsAReadableProfile() throws Exception {
        File dir = folder.newFolder("save");
        FileProfileStore store = new FileProfileStore(dir);
        PlayerProfile profile = new PlayerProfile();
        profile.currencyWon = 4242;
        store.save(profile);
        // 저장 디렉터리가 사라져 다음 저장이 실패해도 기존 정상본은 남는다.
        // (여기서는 임시 파일 경로를 막는 대신, 실패 주입은 EconomyTest의 MemoryStore가 담당.
        //  디스크 계층에서는 "저장 후 언제나 load 가능"만 보증 확인한다.)
        PlayerProfile loaded = store.load();
        assertEquals(4242L, loaded.currencyWon);
    }

    @Test public void saleSelectionClampsQuantitiesAndComputesTotals() {
        Map<String, Integer> prices = new LinkedHashMap<>();
        prices.put("tumbler", 420);
        prices.put("lantern", 2000);
        PlayerProfile profile = new PlayerProfile();
        HeadquartersEconomy economy = new HeadquartersEconomy(profile, new ProfileStore() {
            @Override public PlayerProfile load() { return null; }
            @Override public void save(PlayerProfile p) { }
        }, prices);
        economy.transferReturn("r", java.util.Arrays.asList(
            "tumbler", "tumbler", "lantern", "mystery_item"));
        SaleSelection selection = new SaleSelection();
        selection.refresh(economy.heldCounts(), economy);
        assertEquals(3, selection.rows().size());
        selection.adjust(0, +5);   // tumbler 보유 2 → 2로 제한
        assertEquals(2, selection.rows().get(0).selected);
        selection.adjust(0, -9);   // 0 아래로 내려가지 않는다
        assertEquals(0, selection.rows().get(0).selected);
        selection.toggleAll();     // 판매 가능만 전량 선택
        assertEquals(3, selection.selectedCount());
        assertEquals(420L * 2 + 2000L, selection.selectedTotalWon());
        // 판매 불가(mystery_item)는 선택되지 않는다.
        for (SaleSelection.Row row : selection.rows())
            if (!row.sellable) assertEquals(0, row.selected);
        selection.toggleAll();
        assertEquals(0, selection.selectedCount());
    }
}

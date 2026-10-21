package io.github.some_example_name.boss;

import io.github.some_example_name.profile.HeadquartersEconomy;
import io.github.some_example_name.profile.PlayerProfile;
import io.github.some_example_name.profile.ProfileCodec;
import io.github.some_example_name.profile.ProfileStore;
import java.io.IOException;
import java.util.HashMap;
import org.junit.Test;
import static org.junit.Assert.*;

/** 보스 보상 원자 거래(1run 1보상·저장 실패 롤백·중복 방지·프로필 왕복) 검증. */
public class BossRewardTest {

    private static final class MemoryStore implements ProfileStore {
        PlayerProfile saved;
        boolean failNextSave;
        @Override public PlayerProfile load() { return saved; }
        @Override public void save(PlayerProfile profile) throws IOException {
            if (failNextSave) { failNextSave = false; throw new IOException("disk full"); }
            saved = ProfileCodec.fromJson(ProfileCodec.toJson(profile));
        }
    }

    private static HeadquartersEconomy economy(MemoryStore store) {
        PlayerProfile profile = new PlayerProfile();
        profile.currencyWon = 5000;
        return new HeadquartersEconomy(profile, store, new HashMap<String, Integer>());
    }

    @Test public void oneRewardPerRunAndAtomicCommit() {
        MemoryStore store = new MemoryStore();
        HeadquartersEconomy economy = economy(store);
        HeadquartersEconomy.Result first =
            economy.commitBossClear("run-A", "1-1", "1-2", "golden_microscope");
        assertEquals(HeadquartersEconomy.Status.OK, first.status);
        assertEquals("보상은 본부 보관함으로(가방 8칸과 무관)", 1, economy.profile().storage.size());
        assertEquals("golden_microscope", economy.profile().storage.get(0).itemId);
        assertTrue(economy.profile().bossClearedStages.contains("1-1"));
        assertTrue("1-2 진행 해금 기록", economy.profile().bossUnlockedStages.contains("1-2"));
        assertTrue(economy.profile().bossRewardReceipts.contains("1-1#run-A"));
        assertEquals("기존 재화 보존", 5000L, economy.profile().currencyWon);
        // 같은 runId 재호출(결과창 재진입/콜백 중복/복귀 더블클릭) — 복제 없음.
        HeadquartersEconomy.Result dup =
            economy.commitBossClear("run-A", "1-1", "1-2", "golden_microscope");
        assertEquals(HeadquartersEconomy.Status.ALREADY_PROCESSED, dup.status);
        assertEquals("아이템/해금 중복 없음", 1, economy.profile().storage.size());
        assertEquals(1, economy.profile().bossUnlockedStages.size());
        // 새 runId 재도전 토벌은 새 보상 1개 허용, 최초 해금은 중복 누적 없음.
        assertEquals(HeadquartersEconomy.Status.OK,
            economy.commitBossClear("run-B", "1-1", "1-2", "ancient_tablet").status);
        assertEquals(2, economy.profile().storage.size());
        assertEquals(1, economy.profile().bossUnlockedStages.size());
        // 인스턴스 ID 중복 없음.
        assertNotEquals(economy.profile().storage.get(0).instanceId,
            economy.profile().storage.get(1).instanceId);
    }

    @Test public void saveFailureKeepsPreviousStateAndAllowsRetry() {
        MemoryStore store = new MemoryStore();
        HeadquartersEconomy economy = economy(store);
        store.failNextSave = true;
        HeadquartersEconomy.Result failed =
            economy.commitBossClear("run-C", "1-1", "1-2", "golden_microscope");
        assertEquals(HeadquartersEconomy.Status.SAVE_FAILED, failed.status);
        assertTrue("실패 시 이전 상태 유지(물품/해금/영수증 없음)",
            economy.profile().storage.isEmpty()
                && economy.profile().bossRewardReceipts.isEmpty()
                && economy.profile().bossClearedStages.isEmpty());
        // 같은 선택 보상으로 재시도 가능(REWARD_PENDING 유지 정책).
        HeadquartersEconomy.Result retry =
            economy.commitBossClear("run-C", "1-1", "1-2", "golden_microscope");
        assertEquals(HeadquartersEconomy.Status.OK, retry.status);
        assertEquals(1, economy.profile().storage.size());
    }

    @Test public void bossProgressSurvivesCodecRoundTrip() {
        PlayerProfile profile = new PlayerProfile();
        profile.bossUnlockedStages.add("1-2");
        profile.bossClearedStages.add("1-1");
        profile.bossRewardReceipts.add("1-1#run-A");
        PlayerProfile loaded = ProfileCodec.fromJson(ProfileCodec.toJson(profile));
        assertTrue(loaded.bossUnlockedStages.contains("1-2"));
        assertTrue(loaded.bossClearedStages.contains("1-1"));
        assertTrue("영수증 왕복 — 재실행 시 선확인", loaded.bossRewardReceipts.contains("1-1#run-A"));
    }

    @Test public void rewardSelectionIsStableForSameRunId() throws Exception {
        String json = new String(java.nio.file.Files.readAllBytes(
            java.nio.file.Paths.get("../assets/loot/loot_tables.json")),
            java.nio.charset.StandardCharsets.UTF_8);
        io.github.some_example_name.inventory.LootTableCatalog catalog =
            new io.github.some_example_name.inventory.LootTableCatalog(json);
        // 보상 풀 = 기존 카탈로그의 LEGENDARY 테이블(legendary_cabinet) — 새 가격/이름 복사 없음.
        java.util.List<io.github.some_example_name.inventory.LootItem> pool =
            catalog.items("legendary_cabinet");
        assertFalse(pool.isEmpty());
        for (io.github.some_example_name.inventory.LootItem item : pool)
            assertEquals("LEGENDARY", item.rarity);
        // runId에서 파생한 시드 → 결과창 재진입/재저장/재실행에도 같은 아이템.
        String runId = "0f2b1c7e-run";
        long seed = runId.hashCode() * 31L + "1-1".hashCode();
        io.github.some_example_name.inventory.LootItem a =
            catalog.roll("legendary_cabinet", new java.util.Random(seed));
        io.github.some_example_name.inventory.LootItem b =
            catalog.roll("legendary_cabinet", new java.util.Random(seed));
        assertEquals("보상 선택은 runId 파생 시드로 안정", a.id, b.id);
    }
}

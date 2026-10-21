package io.github.some_example_name.profile;

import io.github.some_example_name.config.BalanceConfig;
import io.github.some_example_name.entity.player.Player;
import org.junit.Test;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import static org.junit.Assert.*;

/** 강화 카탈로그 검증·구매 트랜잭션·파생 능력치·마이그레이션을 검증한다(순수 Java). */
public class UpgradeTreeTest {

    private static String realCatalogJson() throws Exception {
        return new String(Files.readAllBytes(
            Paths.get("../assets/ui/hq_upgrade_tree_v1/data/upgrade_tree.json")),
            StandardCharsets.UTF_8);
    }

    private static final class MemoryStore implements ProfileStore {
        PlayerProfile saved;
        boolean failNextSave;
        @Override public PlayerProfile load() { return saved; }
        @Override public void save(PlayerProfile profile) throws IOException {
            if (failNextSave) { failNextSave = false; throw new IOException("disk full"); }
            saved = ProfileCodec.fromJson(ProfileCodec.toJson(profile));
        }
    }

    private static HeadquartersEconomy richEconomy(MemoryStore store, long money) {
        PlayerProfile profile = new PlayerProfile();
        profile.currencyWon = money;
        return new HeadquartersEconomy(profile, store, new HashMap<String, Integer>());
    }

    // ----- 카탈로그 로딩 검증 ------------------------------------------------------------

    @Test public void realCatalogLoadsAndValidates() throws Exception {
        UpgradeCatalog catalog = new UpgradeCatalog(realCatalogJson());
        // 2026-10-20: 3종 스킬(충격파/전기 투척/보호막) 해금+강화 6 + 슬롯 확장 IV/V 병합 = 17.
        assertEquals(17, catalog.nodes().size());
        assertEquals(30, catalog.healBaseAmount);
        assertEquals(10f, catalog.healCooldownSeconds, .001f);
        assertEquals(20, catalog.node("hp_1").effectValue);
        assertEquals("heal", catalog.node("heal_unlock").skillId);
        // 새 스킬 노드: 해금은 선행 없음, 강화는 해당 해금이 선행(AND).
        assertEquals("shockwave", catalog.node("shockwave_unlock").skillId);
        assertEquals(UpgradeCatalog.EffectType.SKILL_DAMAGE_ADD,
            catalog.node("shockwave_power_1").effectType);
        assertEquals(java.util.Arrays.asList("shockwave_unlock"),
            catalog.node("shockwave_power_1").prerequisites);
        assertEquals(UpgradeCatalog.EffectType.SHIELD_ABSORB_ADD,
            catalog.node("shield_power_1").effectType);
        assertEquals(30, catalog.node("shield_power_1").effectValue);
        // 슬롯 확장 사슬: 2(→3) → 4 → 5. 해금 구매 자체는 슬롯을 늘리지 않는다.
        assertEquals(java.util.Arrays.asList("active_slot_2"),
            catalog.node("active_slot_4").prerequisites);
        assertEquals(java.util.Arrays.asList("active_slot_4"),
            catalog.node("active_slot_5").prerequisites);
        UpgradeEffects all = UpgradeEffects.compute(catalog, new java.util.LinkedHashSet<>(
            java.util.Arrays.asList("active_slot_2", "active_slot_4", "active_slot_5",
                "shockwave_unlock", "shockwave_power_1", "shield_unlock", "shield_power_1")));
        assertEquals("기본 2 + 확장 3 = 최대 5 지원", 3, all.extraActiveSlots);
        assertEquals(10, all.skillDamageBonus("shockwave"));
        assertEquals(0, all.skillDamageBonus("electric_throw"));
        assertEquals(30, all.shieldAbsorbBonus);
        assertTrue(all.unlockedSkillIds.contains("shockwave"));
        assertFalse(all.unlockedSkillIds.contains("electric_throw"));
    }

    @Test public void catalogRejectsDuplicateMissingCycleNegativeAndUnknown() {
        String base = "{\"nodes\":[%s]}";
        String nodeA = "{\"nodeId\":\"a\",\"costWon\":10,\"effectType\":\"MAX_HP_ADD\",\"effectValue\":1}";
        try { new UpgradeCatalog(String.format(base, nodeA + "," + nodeA)); fail(); }
        catch (IllegalArgumentException expected) { assertTrue(expected.getMessage().contains("duplicate")); }
        try { new UpgradeCatalog(String.format(base,
            "{\"nodeId\":\"a\",\"costWon\":10,\"effectType\":\"MAX_HP_ADD\",\"effectValue\":1,\"prerequisites\":[\"ghost\"]}"));
            fail(); } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("missing prerequisite")); }
        try { new UpgradeCatalog(String.format(base,
            "{\"nodeId\":\"a\",\"costWon\":10,\"effectType\":\"MAX_HP_ADD\",\"effectValue\":1,\"prerequisites\":[\"b\"]},"
            + "{\"nodeId\":\"b\",\"costWon\":10,\"effectType\":\"MAX_HP_ADD\",\"effectValue\":1,\"prerequisites\":[\"a\"]}"));
            fail(); } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("cycle")); }
        try { new UpgradeCatalog(String.format(base,
            "{\"nodeId\":\"a\",\"costWon\":-5,\"effectType\":\"MAX_HP_ADD\",\"effectValue\":1}"));
            fail(); } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("negative")); }
        try { new UpgradeCatalog(String.format(base,
            "{\"nodeId\":\"a\",\"costWon\":5,\"effectType\":\"TELEPORT\",\"effectValue\":1}"));
            fail(); } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("unknown effectType")); }
    }

    // ----- 구매 트랜잭션 ---------------------------------------------------------------

    @Test public void purchaseBlocksPrereqInsufficientAndDuplicate() throws Exception {
        UpgradeCatalog catalog = new UpgradeCatalog(realCatalogJson());
        MemoryStore store = new MemoryStore();
        HeadquartersEconomy economy = richEconomy(store, 2000);
        // 선행 미충족(hp_2는 hp_1 필요)
        assertEquals(HeadquartersEconomy.Status.INVALID,
            economy.purchaseUpgrade("t1", catalog, "hp_2", 1).status);
        // 재화 부족(atk_1=1500은 가능, hp_1=1200 가능 — heal_unlock=3000은 부족)
        assertEquals(HeadquartersEconomy.Status.INVALID,
            economy.purchaseUpgrade("t2", catalog, "heal_unlock", 1).status);
        // 정상 구매: 정확한 차감
        HeadquartersEconomy.Result ok = economy.purchaseUpgrade("t3", catalog, "hp_1", 1);
        assertEquals(HeadquartersEconomy.Status.OK, ok.status);
        assertEquals(1200L, ok.totalWon);
        assertEquals(800L, economy.currencyWon());
        // 1회 구매: 같은 노드 재구매 금지
        assertEquals(HeadquartersEconomy.Status.INVALID,
            economy.purchaseUpgrade("t4", catalog, "hp_1", 1).status);
        // 거래 ID 반복 요청 방지
        assertEquals(HeadquartersEconomy.Status.ALREADY_PROCESSED,
            economy.purchaseUpgrade("t3", catalog, "hp_2", 1).status);
    }

    @Test public void purchaseSaveFailureRollsBackMoneyAndNodes() throws Exception {
        UpgradeCatalog catalog = new UpgradeCatalog(realCatalogJson());
        MemoryStore store = new MemoryStore();
        HeadquartersEconomy economy = richEconomy(store, 5000);
        store.failNextSave = true;
        assertEquals(HeadquartersEconomy.Status.SAVE_FAILED,
            economy.purchaseUpgrade("tx", catalog, "hp_1", 1).status);
        assertEquals(5000L, economy.currencyWon());
        assertTrue(economy.profile().purchasedNodeIds().isEmpty());
        // 같은 거래 ID로 재시도 가능
        assertEquals(HeadquartersEconomy.Status.OK,
            economy.purchaseUpgrade("tx", catalog, "hp_1", 1).status);
    }

    @Test public void skillUnlockAutoEquipsIntoEmptySlotAndPowerRequiresUnlock() throws Exception {
        UpgradeCatalog catalog = new UpgradeCatalog(realCatalogJson());
        MemoryStore store = new MemoryStore();
        HeadquartersEconomy economy = richEconomy(store, 50_000);
        // 해금 전 성능 강화 구매 불가(선행 + 해금 검증)
        assertEquals(HeadquartersEconomy.Status.INVALID,
            economy.purchaseUpgrade("t0", catalog, "heal_power_1", 1).status);
        assertEquals(HeadquartersEconomy.Status.OK,
            economy.purchaseUpgrade("t1", catalog, "heal_unlock", 1).status);
        assertTrue("빈 슬롯에 자동 장착", economy.profile().equippedSkills.contains("heal"));
        assertEquals(1, economy.profile().equippedSkills.size());
        // 성능 강화 가능, 효과 계산 반영
        assertEquals(HeadquartersEconomy.Status.OK,
            economy.purchaseUpgrade("t2", catalog, "heal_power_1", 1).status);
        UpgradeEffects effects = UpgradeEffects.compute(catalog, economy.profile().purchasedNodeIds());
        assertTrue(effects.healUnlocked());
        assertEquals(10, effects.healBonus);
        assertEquals(40, catalog.healBaseAmount + effects.healBonus);
        // 슬롯 확장: 구매 전후 실제 슬롯 수 +1 (기본 1 → 2)
        assertEquals(0, effects.extraActiveSlots);
        assertEquals(HeadquartersEconomy.Status.OK,
            economy.purchaseUpgrade("t3", catalog, "active_slot_2", 1).status);
        effects = UpgradeEffects.compute(catalog, economy.profile().purchasedNodeIds());
        assertEquals(1, effects.extraActiveSlots);
    }

    // ----- 파생 능력치/플레이어 통합 ------------------------------------------------------

    @Test public void derivedStatsComputeFromBasePlusPurchasesWithoutStacking() throws Exception {
        UpgradeCatalog catalog = new UpgradeCatalog(realCatalogJson());
        java.util.Set<String> purchased = new java.util.LinkedHashSet<>(
            java.util.Arrays.asList("hp_1", "hp_2", "atk_1", "ghost_node_future"));
        UpgradeEffects effects = UpgradeEffects.compute(catalog, purchased);
        assertEquals(50, effects.bonusMaxHp);
        assertEquals(3, effects.bonusAttack);
        assertTrue("알 수 없는 저장 노드는 보존·경고 대상", effects.unknownPurchasedIds.contains("ghost_node_future"));
        // 재계산해도 같은 값(로드/전환/재진입 중복 누적 없음 — 항상 기본+합으로 파생).
        UpgradeEffects again = UpgradeEffects.compute(catalog, purchased);
        assertEquals(effects.bonusMaxHp, again.bonusMaxHp);
        assertEquals(effects.bonusAttack, again.bonusAttack);
    }

    @Test public void maxHpIncreaseKeepsCurrentHpAndWaterHealsToNewMax() {
        BalanceConfig config = new BalanceConfig();
        Player player = new Player(5f, 5f, .6f, .5f, config.playerMaxHp); // 100
        player.takeDamage(40); // HP 60
        player.setMaxHp(120);  // hp_1 구매(+20)
        assertEquals("현재 HP 유지(공짜 회복 없음)", 60, player.hp());
        assertEquals(120, player.maxHp());
        player.healToFullHealth(); // 정수기
        assertEquals("정수기는 새 최대치까지 회복", 120, player.hp());
        player.takeDamage(50); // 70
        player.heal(200);      // 회복 상한 = maxHp
        assertEquals(120, player.hp());
        // 최대치 축소 시 현재 HP 클램프(0 <= hp <= maxHp 불변)
        player.setMaxHp(100);
        assertEquals(100, player.hp());
    }

    @Test public void hudColorBandsFollowRatioNotAbsoluteValues() {
        // ui_layout.json 기준값(maxHp 100, 임계 50/25)을 비율 50%/25%로 해석.
        String json = "{\"health\":{\"maxHp\":100,\"screenMargin\":[24,20],\"frameSize\":[500,100],"
            + "\"fillRectWithinFrame\":[134,57,240,18],\"valueTextRectWithinFrame\":[404,48,72,36],"
            + "\"colorThresholds\":{\"healthyAbove\":50,\"warningAbove\":25}},"
            + "\"backpack\":{\"panelSize\":[960,600],\"slotSize\":[108,108],"
            + "\"slotTopLeftCoordinates\":[[0,0]],\"itemIconInsetWithinSlot\":[10,10,88,88],"
            + "\"capacityTextRectWithinPanel\":[0,0,100,30],\"recommendedMaxViewportFraction\":[.8,.8],"
            + "\"backgroundDimAlpha\":.55,\"grid\":{\"slotCount\":8}}}";
        io.github.some_example_name.asset.HudLayout layout =
            io.github.some_example_name.asset.HudLayout.fromJson(json);
        assertEquals("120/170은 70% → healthy", 0, layout.colorBand(120, 170));
        assertEquals("80/170은 47% → warning", 1, layout.colorBand(80, 170));
        assertEquals("40/170은 23% → critical", 2, layout.colorBand(40, 170));
        assertEquals("60/100 → healthy(기존 해석과 동일)", 0, layout.colorBand(60, 100));
    }

    @Test public void equippedSkillsAndCatalogVersionSurviveSaveReload() throws Exception {
        UpgradeCatalog catalog = new UpgradeCatalog(realCatalogJson());
        MemoryStore store = new MemoryStore();
        HeadquartersEconomy economy = richEconomy(store, 10_000);
        economy.purchaseUpgrade("t1", catalog, "heal_unlock", 1);
        PlayerProfile reloaded = store.saved; // 직렬화 왕복 결과
        assertTrue(reloaded.equippedSkills.contains("heal"));
        assertEquals(catalog.catalogVersion, reloaded.upgradeCatalogVersion);
        assertEquals(Integer.valueOf(1), reloaded.purchasedUpgrades.get("heal_unlock"));
        // 재로드 후 효과 재계산(중복 누적 없음)
        UpgradeEffects effects = UpgradeEffects.compute(catalog, reloaded.purchasedNodeIds());
        assertTrue(effects.healUnlocked());
    }

    @Test public void legacyProfileWithUpgradeLevelsMigratesPreservingMeaning() {
        // 이전 단계 프로필: purchasedUpgrades(ID→레벨)만 있고 equippedSkills가 없다.
        String legacy = "{\"schemaVersion\":1,\"currencyWon\":9000,"
            + "\"purchasedUpgrades\":{\"hp_1\":1,\"old_system_upgrade\":3}}";
        PlayerProfile migrated = ProfileCodec.fromJson(legacy);
        assertEquals(9000L, migrated.currencyWon);
        assertTrue("레벨≥1 노드는 구매로 인정", migrated.purchasedNodeIds().contains("hp_1"));
        assertTrue("알 수 없는 강화 ID 보존", migrated.purchasedUpgrades.containsKey("old_system_upgrade"));
        assertTrue(migrated.equippedSkills.isEmpty());
        String rewritten = ProfileCodec.toJson(migrated);
        assertTrue(rewritten.contains("old_system_upgrade"));
    }
}

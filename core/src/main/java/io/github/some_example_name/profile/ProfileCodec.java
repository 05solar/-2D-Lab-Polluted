package io.github.some_example_name.profile;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;

/**
 * 프로필 JSON 직렬화/역직렬화(순수 Java — GL 불필요, 파일 I/O 없음).
 *
 * 마이그레이션 규칙: 과거 버전에 없는 필드(보관함·강화·기록)는 안전한 기본값으로 채우고
 * 기존 재화를 보존한다. 알 수 없는 최상위 필드와 알 수 없는 강화 ID는 삭제하지 않고
 * 원문 그대로 보존해 재기록한다. 알 수 없는 아이템 ID도 보관함에 그대로 남긴다
 * (판매 가능 여부는 카탈로그 기준으로 판매 시점에 판정).
 */
public final class ProfileCodec {

    private static final String[] KNOWN_FIELDS = {"schemaVersion", "profileRevision",
        "currencyWon", "headquartersStorage", "purchasedUpgrades", "equippedSkills",
        "skillSlots", "skillKeys", "skillCooldowns",
        "bossUnlockedStages", "bossClearedStages", "bossRewardReceipts",
        "upgradeCatalogVersion", "processedReturnIds", "processedTransactionIds", "nextInstanceId"};

    private ProfileCodec() { }

    public static String toJson(PlayerProfile profile) {
        JsonValue root = new JsonValue(JsonValue.ValueType.object);
        root.addChild("schemaVersion", new JsonValue(profile.schemaVersion));
        root.addChild("profileRevision", new JsonValue(profile.profileRevision));
        root.addChild("currencyWon", new JsonValue(profile.currencyWon));
        JsonValue storage = new JsonValue(JsonValue.ValueType.array);
        for (ItemInstance item : profile.storage) {
            JsonValue entry = new JsonValue(JsonValue.ValueType.object);
            entry.addChild("inst", new JsonValue(item.instanceId));
            entry.addChild("id", new JsonValue(item.itemId));
            storage.addChild(entry);
        }
        root.addChild("headquartersStorage", storage);
        JsonValue upgrades = new JsonValue(JsonValue.ValueType.object);
        for (java.util.Map.Entry<String, Integer> e : profile.purchasedUpgrades.entrySet())
            upgrades.addChild(e.getKey(), new JsonValue(e.getValue()));
        root.addChild("purchasedUpgrades", upgrades);
        root.addChild("equippedSkills", stringArray(profile.equippedSkills));
        root.addChild("skillSlots", stringArray(profile.skillSlots));
        JsonValue keyArray = new JsonValue(JsonValue.ValueType.array);
        for (Integer key : profile.skillKeys) keyArray.addChild(new JsonValue(key == null ? 0 : key));
        root.addChild("skillKeys", keyArray);
        JsonValue cooldowns = new JsonValue(JsonValue.ValueType.object);
        for (java.util.Map.Entry<String, Float> e : profile.skillCooldowns.entrySet())
            cooldowns.addChild(e.getKey(), new JsonValue(e.getValue()));
        root.addChild("skillCooldowns", cooldowns);
        root.addChild("bossUnlockedStages", stringArray(profile.bossUnlockedStages));
        root.addChild("bossClearedStages", stringArray(profile.bossClearedStages));
        root.addChild("bossRewardReceipts", stringArray(profile.bossRewardReceipts));
        root.addChild("upgradeCatalogVersion", new JsonValue(profile.upgradeCatalogVersion));
        root.addChild("processedReturnIds", stringArray(profile.processedReturnIds));
        root.addChild("processedTransactionIds", stringArray(profile.processedTransactionIds));
        root.addChild("nextInstanceId", new JsonValue(profile.nextInstanceId));
        for (java.util.Map.Entry<String, String> e : profile.unknownFieldsJson.entrySet())
            root.addChild(e.getKey(), new JsonReader().parse(e.getValue()));
        return root.toJson(JsonWriter.OutputType.json);
    }

    public static PlayerProfile fromJson(String json) {
        JsonValue root = new JsonReader().parse(json);
        if (root == null || !root.isObject()) throw new IllegalArgumentException("profile json");
        PlayerProfile profile = new PlayerProfile();
        // 과거/부분 저장: 없는 필드는 기본값(빈 보관함·강화 없음·기록 없음)으로 채운다.
        profile.schemaVersion = PlayerProfile.CURRENT_SCHEMA_VERSION;
        profile.profileRevision = root.getLong("profileRevision", 0L);
        profile.currencyWon = root.getLong("currencyWon", 0L);
        long maxInstance = 0;
        JsonValue storage = root.get("headquartersStorage");
        if (storage != null) for (JsonValue entry = storage.child; entry != null; entry = entry.next) {
            long inst = entry.getLong("inst", 0L);
            String id = entry.getString("id", null);
            if (id == null || id.isEmpty()) continue; // 식별 불가 항목만 건너뛴다
            profile.storage.add(new ItemInstance(Math.max(0L, inst), id));
            maxInstance = Math.max(maxInstance, inst);
        }
        JsonValue upgrades = root.get("purchasedUpgrades");
        if (upgrades != null) for (JsonValue e = upgrades.child; e != null; e = e.next)
            profile.purchasedUpgrades.put(e.name(), e.asInt()); // 알 수 없는 강화 ID도 보존
        JsonValue equipped = root.get("equippedSkills");
        if (equipped != null) for (JsonValue e = equipped.child; e != null; e = e.next)
            if (e.isString()) profile.equippedSkills.add(e.asString());
        JsonValue slotArray = root.get("skillSlots");
        if (slotArray != null) for (JsonValue e = slotArray.child; e != null; e = e.next)
            profile.skillSlots.add(e.isString() ? e.asString() : "");
        JsonValue keyArray = root.get("skillKeys");
        if (keyArray != null) for (JsonValue e = keyArray.child; e != null; e = e.next)
            profile.skillKeys.add(e.isNumber() ? e.asInt() : 0);
        JsonValue cooldowns = root.get("skillCooldowns");
        if (cooldowns != null) for (JsonValue e = cooldowns.child; e != null; e = e.next)
            profile.skillCooldowns.put(e.name(), e.asFloat());
        readStrings(root.get("bossUnlockedStages"), profile.bossUnlockedStages);
        readStrings(root.get("bossClearedStages"), profile.bossClearedStages);
        readStrings(root.get("bossRewardReceipts"), profile.bossRewardReceipts);
        profile.upgradeCatalogVersion = root.getInt("upgradeCatalogVersion", 0);
        readStrings(root.get("processedReturnIds"), profile.processedReturnIds);
        readStrings(root.get("processedTransactionIds"), profile.processedTransactionIds);
        profile.nextInstanceId = Math.max(root.getLong("nextInstanceId", 1L), maxInstance + 1);
        for (JsonValue field = root.child; field != null; field = field.next)
            if (!isKnown(field.name()))
                profile.unknownFieldsJson.put(field.name(), field.toJson(JsonWriter.OutputType.json));
        return profile;
    }

    private static boolean isKnown(String name) {
        for (String known : KNOWN_FIELDS) if (known.equals(name)) return true;
        return false;
    }

    private static JsonValue stringArray(Iterable<String> values) {
        JsonValue array = new JsonValue(JsonValue.ValueType.array);
        for (String value : values) array.addChild(new JsonValue(value));
        return array;
    }

    private static void readStrings(JsonValue array, java.util.Set<String> out) {
        if (array == null) return;
        for (JsonValue entry = array.child; entry != null; entry = entry.next)
            if (entry.isString()) out.add(entry.asString());
    }
}

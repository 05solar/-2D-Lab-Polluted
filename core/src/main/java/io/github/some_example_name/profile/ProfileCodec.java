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
        "currencyWon", "headquartersStorage", "purchasedUpgrades",
        "processedReturnIds", "processedTransactionIds", "nextInstanceId"};

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

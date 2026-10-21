package io.github.some_example_name.world;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 난이도별 랜덤 연구소 생성 데이터(assets/laboratory_generation_v1). 구조 틀 12개
 * (난이도별 4개)·난이도 프로필·테마 목록을 파싱한다(순수 Java — 파일 I/O는 Source로 주입).
 * 동봉 수치 중 몬스터 총량·캐비넷 수는 초안이라 실제 쉬움 기준(BalanceConfig)이 우선한다.
 */
public final class LabGenData {

    /** 파일 읽기 주입(게임=Gdx.files, 테스트=java.nio). 경로는 팩 루트 기준 상대 경로. */
    public interface Source { String read(String relativePath); }

    public static final String[] TEMPLATE_NAMES = {"compact", "courtyard", "staggered", "twin_wings"};

    public static final class Template {
        public final String templateId;
        public final String difficulty;
        public final int templateVersion;
        public final int mapWidth, mapHeight;
        public final int roomCount;
        public final int pitchX, pitchY;
        public final int interiorW, interiorH;
        public final int jitterTiles;
        public final List<String> allowedShapes;
        /** 정렬된 roomId → [gridX, gridY] (roomId 사전순 — 순회 순서 결정성). */
        public final Map<String, int[]> slots;

        Template(JsonValue root) {
            templateId = root.getString("templateId");
            difficulty = root.getString("difficulty");
            templateVersion = root.getInt("templateVersion", 1);
            int[] size = root.get("mapSize").asIntArray();
            mapWidth = size[0];
            mapHeight = size[1];
            roomCount = root.getInt("roomCount");
            int[] pitch = root.get("gridPitch").asIntArray();
            pitchX = pitch[0];
            pitchY = pitch[1];
            int[] interior = root.get("roomInteriorSize").asIntArray();
            interiorW = interior[0];
            interiorH = interior[1];
            jitterTiles = root.getInt("roomJitterTiles", 1);
            allowedShapes = new ArrayList<>();
            for (JsonValue s = root.get("allowedShapes").child; s != null; s = s.next)
                allowedShapes.add(s.asString());
            Map<String, int[]> parsed = new java.util.TreeMap<>();
            for (JsonValue s = root.get("roomSlots").child; s != null; s = s.next)
                parsed.put(s.getString("roomId"), s.get("grid").asIntArray());
            if (parsed.size() != roomCount)
                throw new IllegalArgumentException(templateId + ": roomSlots != roomCount");
            slots = Collections.unmodifiableMap(new LinkedHashMap<>(parsed));
        }
    }

    public static final class Profile {
        public final String[] monsterTypes;   // 가중치 순서와 짝
        public final int[] monsterWeights;
        public final List<String> minimumSpecies;
        public final java.util.Set<String> excludedMonsterIds;
        public final int extraEdgePercent;

        Profile(JsonValue node) {
            List<String> types = new ArrayList<>();
            List<Integer> weights = new ArrayList<>();
            for (JsonValue w = node.get("monsterWeights").child; w != null; w = w.next) {
                types.add(w.child.asString());
                weights.add(w.child.next.asInt());
            }
            monsterTypes = types.toArray(new String[0]);
            monsterWeights = new int[weights.size()];
            for (int i = 0; i < weights.size(); i++) monsterWeights[i] = weights.get(i);
            minimumSpecies = new ArrayList<>();
            JsonValue min = node.get("minimumSpecies");
            if (min != null) for (JsonValue m = min.child; m != null; m = m.next)
                minimumSpecies.add(m.asString());
            excludedMonsterIds = new java.util.LinkedHashSet<>();
            JsonValue ex = node.get("excludedMonsterIds");
            if (ex != null) for (JsonValue m = ex.child; m != null; m = m.next)
                excludedMonsterIds.add(m.asString());
            extraEdgePercent = node.getInt("extraEdgePercent", 25);
        }
    }

    /** 테마 16종 id(정렬 고정 — room_themes.json 순서). */
    public final List<String> themeIds;
    private final Map<String, List<Template>> templatesByDifficulty = new LinkedHashMap<>();
    private final Map<String, Profile> profiles = new LinkedHashMap<>();

    public LabGenData(Source source) {
        JsonReader reader = new JsonReader();
        for (String difficulty : new String[]{"easy", "normal", "hard"}) {
            List<Template> list = new ArrayList<>();
            for (String name : TEMPLATE_NAMES) {
                String path = "data/layouts/" + difficulty + "/" + difficulty + "_" + name + ".json";
                Template template = new Template(reader.parse(source.read(path)));
                if (!difficulty.equals(template.difficulty))
                    throw new IllegalArgumentException(path + ": difficulty mismatch");
                list.add(template);
            }
            templatesByDifficulty.put(difficulty, Collections.unmodifiableList(list));
        }
        JsonValue profileRoot = reader.parse(source.read("data/difficulty_profiles.json"))
            .get("profiles");
        for (String difficulty : new String[]{"easy", "normal", "hard"})
            profiles.put(difficulty, new Profile(profileRoot.get(difficulty)));
        List<String> themes = new ArrayList<>();
        JsonValue themeRoot = reader.parse(source.read("data/room_themes.json"));
        for (JsonValue t = themeRoot.get("themes").child; t != null; t = t.next)
            themes.add(t.getString("id"));
        if (themes.size() < 16) throw new IllegalArgumentException("16개 테마 필요");
        themeIds = Collections.unmodifiableList(themes);
    }

    /** 난이도의 구조 틀 4개(templateId 사전순 고정). */
    public List<Template> templates(GameDifficulty difficulty) {
        return templatesByDifficulty.get(key(difficulty));
    }

    public Profile profile(GameDifficulty difficulty) { return profiles.get(key(difficulty)); }

    private static String key(GameDifficulty difficulty) {
        switch (difficulty) {
            case EASY: return "easy";
            case NORMAL: return "normal";
            default: return "hard";
        }
    }
}

package io.github.some_example_name.boss;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 단계별 투기장 논리 배치(arena_layouts.runtime.json). 기존 바닥/벽/문 조립 규칙을 재사용하고
 * 전체 그림을 배경 충돌로 쓰지 않는다. 기둥은 발 footprint 사각형이다.
 */
public final class BossArenaCatalog {

    public static final class Arena {
        public final String id;
        public final int width, height;
        public final float playerSpawnX, playerSpawnY;
        public final float bossSpawnX, bossSpawnY;
        public final int gateX, gateW; // southGatePassage: [x,0,w,2]
        public final List<float[]> pillars = new ArrayList<>(); // [x,y,w,h]

        Arena(JsonValue n) {
            id = n.getString("id");
            int[] size = n.get("sizeTiles").asIntArray();
            width = size[0];
            height = size[1];
            int[] spawn = n.get("playerSpawn").asIntArray();
            playerSpawnX = spawn[0] + 0.5f;
            playerSpawnY = spawn[1];
            int[] boss = n.get("bossSpawn").asIntArray();
            bossSpawnX = boss[0] + 0.5f;
            bossSpawnY = boss[1];
            int[] gate = n.get("southGatePassage").asIntArray();
            gateX = gate[0];
            gateW = gate[2];
            for (JsonValue p = n.get("pillars").child; p != null; p = p.next) {
                float[] rect = p.asFloatArray();
                pillars.add(rect);
            }
        }
    }

    private final Map<String, Arena> arenas = new LinkedHashMap<>();

    public BossArenaCatalog(String json) {
        JsonValue root = new JsonReader().parse(json);
        for (JsonValue a = root.get("arenas").child; a != null; a = a.next) {
            Arena arena = new Arena(a);
            arenas.put(arena.id, arena);
        }
    }

    public Arena arena(String id) {
        Arena arena = arenas.get(id);
        if (arena == null) throw new IllegalArgumentException("unknown arena " + id);
        return arena;
    }
}

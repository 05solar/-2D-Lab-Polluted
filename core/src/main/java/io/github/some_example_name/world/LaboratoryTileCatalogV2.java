package io.github.some_example_name.world;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Laboratory Tileset V2의 "데이터"만 담당한다(순수 Java, GL/Texture 없음 → 단위 테스트 가능).
 * laboratory_tiles_v2.json을 파싱해 타일 ID → (아틀라스, 행/열, 인덱스, 충돌, 태그) 매핑을 만든다.
 *
 * 렌더러/리졸버는 숫자 인덱스를 직접 쓰지 않고 이 카탈로그의 타일 ID로만 타일을 참조한다.
 * (인덱스=행×열수, 좌상단 기준 row-major는 JSON의 indexOrder/origin과 일치)
 */
public final class LaboratoryTileCatalogV2 {

    /** 한 아틀라스(시트)의 격자 정의. */
    public static final class AtlasDef {
        public final String name;
        public final String file;
        public final int width, height, columns, rows;
        public final List<String> ids; // 인덱스 순서(row-major)

        AtlasDef(String name, String file, int width, int height, int columns, int rows, List<String> ids) {
            this.name = name; this.file = file;
            this.width = width; this.height = height;
            this.columns = columns; this.rows = rows;
            this.ids = ids;
        }
    }

    /** 타일 1종의 정의. */
    public static final class TileDef {
        public final String id;
        public final String atlas;
        public final int index, column, row;
        public final boolean solid;
        public final boolean configurableSolid; // wall_breach_* 처럼 상황에 따라 결정
        public final String tag;                 // overlay 전용(decor/toxic/contamination/electric/shock), 없으면 null

        TileDef(String id, String atlas, int index, int column, int row,
                boolean solid, boolean configurableSolid, String tag) {
            this.id = id; this.atlas = atlas; this.index = index;
            this.column = column; this.row = row;
            this.solid = solid; this.configurableSolid = configurableSolid; this.tag = tag;
        }
    }

    private final int tileSize;
    private final Map<String, AtlasDef> atlases = new LinkedHashMap<>();
    private final Map<String, TileDef> tiles = new LinkedHashMap<>();
    private final List<String> renderOrder = new ArrayList<>();

    private LaboratoryTileCatalogV2(int tileSize) {
        this.tileSize = tileSize;
    }

    public static LaboratoryTileCatalogV2 fromJson(String json) {
        JsonValue root = new JsonReader().parse(json);
        LaboratoryTileCatalogV2 cat = new LaboratoryTileCatalogV2(root.getInt("tileSize", 64));
        JsonValue order = root.get("renderOrder");
        if (order != null) {
            for (JsonValue v = order.child; v != null; v = v.next) cat.renderOrder.add(v.asString());
        }
        JsonValue atlasesJson = root.get("atlases");
        for (JsonValue a = atlasesJson.child; a != null; a = a.next) {
            String name = a.name();
            String file = a.getString("file");
            int[] size = a.get("size").asIntArray();
            int columns = a.getInt("columns");
            int rows = a.getInt("rows");
            boolean atlasSolidDefault = a.getBoolean("solid", false);

            List<String> ids = new ArrayList<>();
            int index = 0;
            for (JsonValue item = a.get("items").child; item != null; item = item.next, index++) {
                String id;
                boolean solid = atlasSolidDefault;
                boolean configurable = false;
                String tag = null;
                if (item.isString()) {
                    id = item.asString();
                } else {
                    id = item.getString("id");
                    if (item.has("solid")) {
                        JsonValue s = item.get("solid");
                        if (s.isString() && "configurable".equals(s.asString())) {
                            configurable = true; solid = false;
                        } else {
                            solid = s.asBoolean();
                        }
                    }
                    tag = item.getString("tag", null);
                }
                ids.add(id);
                int col = index % columns;
                int row = index / columns;
                cat.tiles.put(id, new TileDef(id, name, index, col, row, solid, configurable, tag));
            }
            cat.atlases.put(name, new AtlasDef(name, file, size[0], size[1], columns, rows, ids));
        }
        return cat;
    }

    public int tileSize() { return tileSize; }
    public List<String> renderOrder() { return renderOrder; }
    public AtlasDef atlas(String name) { return atlases.get(name); }
    public Iterable<AtlasDef> atlases() { return atlases.values(); }
    public TileDef tile(String id) { return tiles.get(id); }
    public boolean contains(String id) { return tiles.containsKey(id); }
    public int tileCount() { return tiles.size(); }

    /** 해당 아틀라스의 타일 수가 columns*rows와 일치하는지(누락/초과 없음). */
    public boolean atlasCountMatchesGrid(String name) {
        AtlasDef a = atlases.get(name);
        return a != null && a.ids.size() == a.columns * a.rows;
    }
}

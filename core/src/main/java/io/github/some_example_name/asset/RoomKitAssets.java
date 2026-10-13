package io.github.some_example_name.asset;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import io.github.some_example_name.config.AssetPaths;
import java.util.HashMap;
import java.util.Map;

/** Loads fixed-cell V3 room kit atlases without rotating or trimming their regions. */
public final class RoomKitAssets implements Disposable {
    private final Map<String, TextureRegion> regions = new HashMap<>();
    private final Map<String, Texture> textures = new HashMap<>();
    private final Map<String, int[]> alphaBounds = new HashMap<>();

    public RoomKitAssets() {
        JsonValue root = new JsonReader().parse(
            Gdx.files.internal(AssetPaths.ROOMKITS_JSON).readString("UTF-8"));
        JsonValue renderMetadata = new JsonReader().parse(Gdx.files.internal(
            AssetPaths.ROOMKITS_DIR + "render_metadata.json").readString("UTF-8")).get("items");
        for (JsonValue item = renderMetadata.child; item != null; item = item.next)
            alphaBounds.put(item.name(), item.get("bounds").asIntArray());
        JsonValue packs = root.get("packs");
        for (JsonValue pack = packs.child; pack != null; pack = pack.next) {
            String name = pack.name();
            if (!pack.has("atlas")) continue;
            Texture texture = load(AssetPaths.ROOMKITS_DIR + pack.getString("atlas"));
            textures.put(name, texture);
            int cellW = pack.getInt("cellWidth"), cellH = pack.getInt("cellHeight");
            int columns = pack.getInt("columns");
            TextureRegion[][] cells = TextureRegion.split(texture, cellW, cellH);
            JsonValue items = pack.get("items");
            int index = 0;
            for (JsonValue item = items.child; item != null; item = item.next, index++)
                regions.put(item.asString(), cells[index / columns][index % columns]);
            if (pack.has("uiAtlas")) {
                Texture ui = load(AssetPaths.ROOMKITS_DIR + pack.getString("uiAtlas"));
                textures.put(name + "_ui", ui);
                TextureRegion[][] uiCells = TextureRegion.split(ui, cellW, cellH);
                JsonValue uiItems = pack.get("uiItems");
                int uiIndex = 0;
                for (JsonValue item = uiItems.child; item != null; item = item.next, uiIndex++)
                    regions.put(item.asString(), uiCells[uiIndex / columns][uiIndex % columns]);
            }
        }
    }

    private static Texture load(String path) {
        Texture texture = new Texture(Gdx.files.internal(path));
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        texture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
        return texture;
    }

    public TextureRegion region(String id) {
        TextureRegion region = regions.get(id);
        if (region == null) throw new IllegalArgumentException("unknown room kit id: " + id);
        return region;
    }

    /** Top-left-origin alpha bounds [minX,minY,maxX,maxY] in source cell pixels. */
    public int[] alphaBounds(String id) {
        int[] bounds = alphaBounds.get(id);
        return bounds == null ? null : bounds.clone();
    }

    @Override public void dispose() {
        for (Texture texture : textures.values()) texture.dispose();
    }
}

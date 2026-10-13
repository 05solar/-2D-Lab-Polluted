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

/** Loads the supplied orthographic furniture views with measured alpha bounds. */
public final class LaboratoryFurnitureAssets implements Disposable {
    public static final class Sprite {
        public final TextureRegion region;
        public final int minX, minY, maxX, maxY;
        Sprite(TextureRegion region, int[] bounds) {
            this.region = region;
            minX = bounds[0]; minY = bounds[1]; maxX = bounds[2]; maxY = bounds[3];
        }
    }

    private final Map<String, Sprite> sprites = new HashMap<>();
    private final Map<String, Texture> textures = new HashMap<>();

    public LaboratoryFurnitureAssets() {
        String root = AssetPaths.FURNITURE_V7_DIR;
        JsonValue catalog = new JsonReader().parse(
            Gdx.files.internal(root + "catalog.json").readString("UTF-8"));
        JsonValue bounds = new JsonReader().parse(
            Gdx.files.internal(root + "render_metadata.json").readString("UTF-8")).get("views");
        JsonValue categories = catalog.get("categories");
        for (JsonValue category = categories.child; category != null; category = category.next) {
            for (JsonValue asset = category.child; asset != null; asset = asset.next) {
                String name = asset.asString();
                load(root, bounds, category.name(), name, "front");
                load(root, bounds, category.name(), name, "left_side");
                load(root, bounds, category.name(), name, "right_side");
            }
        }
    }

    private void load(String root, JsonValue bounds, String category, String name, String view) {
        String file = category + "/" + name + "_" + view + ".png";
        Texture texture = new Texture(Gdx.files.internal(root + "runtime_256/" + file));
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        texture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
        textures.put(file, texture);
        int[] alpha = bounds.get(file).asIntArray();
        sprites.put(category + "/" + name + "/" + view,
            new Sprite(new TextureRegion(texture), alpha));
    }

    /** Resolves explicit V7 catalog references used by the revised laboratory plan. */
    public Sprite sprite(String id) {
        if (id == null || !id.startsWith("v7:")) return null;
        return sprites.get(id.substring(3));
    }

    /** Resolves a V3 roomkit ID to a matching physical view when the V7 pack has one. */
    public Sprite forRoomKitId(String id) {
        String base = id;
        String view = "front";
        if (base.endsWith("_north")) base = base.substring(0, base.length() - 6);
        else if (base.endsWith("_left")) {
            base = base.substring(0, base.length() - 5); view = "left_side";
        } else if (base.endsWith("_right")) {
            base = base.substring(0, base.length() - 6); view = "right_side";
        }
        String category = "";
        String name = base;
        if (base.startsWith("chemistry_bench")) { category = "general_lab"; name = "workbench"; }
        else if (base.startsWith("workbench")) category = "general_lab";
        else if (base.startsWith("fume_hood")) category = "general_lab";
        else if (base.startsWith("sink_counter")) category = "general_lab";
        else if (base.equals("reagent_cabinet") || base.startsWith("storage_cabinet")) {
            category = "general_lab"; name = "storage_cabinet";
        } else if (base.startsWith("biosafety_cabinet") || base.startsWith("specimen_cabinet")) {
            category = "science_lab";
        } else if (base.startsWith("server_rack")) category = "information_cctv";
        else if (base.startsWith("computer_desk")) {
            category = "information_cctv"; name = "computer_workstation";
        } else if (base.startsWith("cctv_wall")) {
            category = "information_cctv"; name = "cctv_console";
        } else if (base.startsWith("kitchenette") || base.startsWith("refrigerator")
            || base.startsWith("coffee_station")) category = "pantry";
        else if (base.startsWith("sofa") || base.startsWith("bunk_bed")
            || base.startsWith("locker_bank")) category = "lounge_sleeping";
        else if (base.startsWith("toilet") || base.startsWith("shower")) {
            category = "restroom_shower";
            if (base.startsWith("shower")) name = "shower_stall";
        } else if (base.startsWith("filing_cabinet") || base.startsWith("archive_shelf")
            || base.startsWith("bookshelf")) category = "archive_library";
        if (category.isEmpty()) return null;
        return sprites.get(category + "/" + name + "/" + view);
    }

    @Override public void dispose() {
        for (Texture texture : textures.values()) texture.dispose();
    }
}

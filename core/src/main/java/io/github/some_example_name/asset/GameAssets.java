package io.github.some_example_name.asset;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import io.github.some_example_name.config.AssetPaths;
import io.github.some_example_name.entity.Direction;
import io.github.some_example_name.render.AnimationSet;
import io.github.some_example_name.world.LaboratoryTileCatalogV2;
import io.github.some_example_name.world.LaboratoryTileSetV2;

import java.util.HashMap;
import java.util.Map;

/**
 * 게임 자산의 로딩과 해제를 한곳에서 책임진다. 게임 루트에서 한 번 생성하고 종료 시 한 번 해제한다.
 * 텍스처 소유권은 여기 한 곳에만 둔다(엔티티/레이아웃/렌더러가 Texture를 새로 만들지 않는다).
 *
 * Laboratory Tileset V2: laboratory_tiles_v2.json을 읽어 카탈로그를 만들고, 4개 아틀라스 텍스처를
 * 로드해 {@link LaboratoryTileSetV2}(ID→TextureRegion)를 구성한다. 구버전 단일 타일셋은 더 이상
 * 로드하지 않는다(fallback로 옛 타일이 섞이지 않도록).
 */
public class GameAssets implements Disposable {

    private final Map<String, Texture> atlasTextures = new HashMap<>();
    private final LaboratoryTileSetV2 tileSet;

    private final Texture playerTexture;
    private final AnimationSet playerAnimations;

    public GameAssets() {
        LaboratoryTileCatalogV2 catalog = LaboratoryTileCatalogV2.fromJson(
            Gdx.files.internal(AssetPaths.TILES_V2_JSON).readString("UTF-8"));
        for (LaboratoryTileCatalogV2.AtlasDef atlas : catalog.atlases()) {
            Texture tex = loadTile(AssetPaths.TILES_V2_DIR + atlas.file);
            atlasTextures.put(atlas.name, tex);
            Gdx.app.log("GameAssets", "atlas=" + atlas.name + " file=" + atlas.file
                + " size=" + tex.getWidth() + "x" + tex.getHeight()
                + " grid=" + atlas.columns + "x" + atlas.rows + " tiles=" + atlas.ids.size());
        }
        tileSet = new LaboratoryTileSetV2(catalog, atlasTextures);
        Gdx.app.log("GameAssets", "tileSet regions=" + tileSet.regionCount());

        playerTexture = loadTile(AssetPaths.PLAYER_SHEET);
        playerAnimations = loadPlayerAnimations(playerTexture);
    }

    /** Nearest 필터 + ClampToEdge(타일 경계 번짐/보라선 방지). */
    private static Texture loadTile(String path) {
        Texture texture = new Texture(Gdx.files.internal(path));
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        texture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
        return texture;
    }

    private static AnimationSet loadPlayerAnimations(Texture sheet) {
        JsonValue root = new JsonReader().parse(Gdx.files.internal(AssetPaths.PLAYER_ANIMATION_DATA));
        int frameW = root.getInt("frameWidth");
        int frameH = root.getInt("frameHeight");
        TextureRegion[][] frames = TextureRegion.split(sheet, frameW, frameH);

        AnimationSet set = new AnimationSet();
        for (JsonValue a = root.get("animations").child; a != null; a = a.next) {
            String name = a.name();
            int row = a.getInt("row");
            int[] frameIndices = a.get("frames").asIntArray();
            float frameDuration = a.getFloat("frameDuration");
            boolean loop = a.getBoolean("loop", false);

            Array<TextureRegion> regions = new Array<>();
            for (int fi : frameIndices) regions.add(frames[row][fi]);
            Animation<TextureRegion> animation = new Animation<>(frameDuration, regions,
                loop ? Animation.PlayMode.LOOP : Animation.PlayMode.NORMAL);
            set.putAnimation(name, animation);

            if (a.has("hitFrames")) set.putHitFrames(name, a.get("hitFrames").asIntArray());
        }

        JsonValue idle = root.get("idleFrames");
        if (idle != null) {
            for (JsonValue d = idle.child; d != null; d = d.next) {
                Direction direction = Direction.valueOf(d.name().toUpperCase());
                set.putIdle(direction, frames[d.getInt("row")][d.getInt("column")]);
            }
        }
        return set;
    }

    public LaboratoryTileSetV2 tileSet() { return tileSet; }

    public AnimationSet playerAnimations() { return playerAnimations; }

    @Override
    public void dispose() {
        for (Texture tex : atlasTextures.values()) tex.dispose();
        playerTexture.dispose();
    }
}

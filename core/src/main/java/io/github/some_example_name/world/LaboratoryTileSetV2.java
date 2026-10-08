package io.github.some_example_name.world;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.HashMap;
import java.util.Map;

/**
 * Laboratory Tileset V2의 "시각" 담당: 타일 ID → {@link TextureRegion} 매핑.
 * 데이터(인덱스/충돌/태그)는 {@link LaboratoryTileCatalogV2}가, 텍스처 소유·해제는 {@link io.github.some_example_name.asset.GameAssets}가 맡는다.
 * 여기서는 전달받은 텍스처로 리전만 만든다(텍스처를 새로 생성하지 않는다).
 *
 * 타일 경계 선(bleeding) 방지: 각 리전을 반텍셀 안으로 줄여(inset) 비정수 배율에서 이웃 조각을
 * 샘플링하지 않게 한다. 목적 크기는 1x1 그대로라 타일 사이 간격은 생기지 않는다.
 */
public final class LaboratoryTileSetV2 {

    private final LaboratoryTileCatalogV2 catalog;
    private final Map<String, TextureRegion> regions = new HashMap<>();

    /** @param atlasTextures 아틀라스 이름 → 로드된 Texture(소유권은 호출자/GameAssets). */
    public LaboratoryTileSetV2(LaboratoryTileCatalogV2 catalog, Map<String, Texture> atlasTextures) {
        this.catalog = catalog;
        int ts = catalog.tileSize();
        for (LaboratoryTileCatalogV2.AtlasDef atlas : catalog.atlases()) {
            Texture tex = atlasTextures.get(atlas.name);
            if (tex == null) {
                throw new IllegalStateException("아틀라스 텍스처 누락: " + atlas.name + " (" + atlas.file + ")");
            }
            for (String id : atlas.ids) {
                LaboratoryTileCatalogV2.TileDef def = catalog.tile(id);
                TextureRegion region = new TextureRegion(tex, def.column * ts, def.row * ts, ts, ts);
                insetHalfTexel(region, tex);
                regions.put(id, region);
            }
        }
    }

    private static void insetHalfTexel(TextureRegion region, Texture tex) {
        float du = 0.5f / tex.getWidth();
        float dv = 0.5f / tex.getHeight();
        region.setRegion(region.getU() + du, region.getV() + dv,
                         region.getU2() - du, region.getV2() - dv);
    }

    /** 타일 ID의 TextureRegion. 없으면 명확히 실패시켜 fallback 타일을 숨기지 않는다. */
    public TextureRegion region(String id) {
        TextureRegion r = regions.get(id);
        if (r == null) throw new IllegalArgumentException("알 수 없는 타일 ID: " + id);
        return r;
    }

    public boolean hasRegion(String id) { return regions.containsKey(id); }
    public int regionCount() { return regions.size(); }
    public LaboratoryTileCatalogV2 catalog() { return catalog; }

    /** Replaces the source region for a known tile while keeping its logical ID and metadata. */
    public void replaceRegion(String id, TextureRegion replacement) {
        if (!regions.containsKey(id)) throw new IllegalArgumentException("unknown tile ID: " + id);
        if (replacement == null) throw new IllegalArgumentException("replacement");
        regions.put(id, replacement);
    }
}

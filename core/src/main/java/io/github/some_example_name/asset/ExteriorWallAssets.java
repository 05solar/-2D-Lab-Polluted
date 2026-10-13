package io.github.some_example_name.asset;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import io.github.some_example_name.config.AssetPaths;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 연구소 외벽(2타일 두께 직선 4종 + 바깥 모서리 4종)과 본부↔연구소 대문(닫힘/열림) 에셋.
 * layout.json의 권장 월드 크기·대문 기준점(anchorPx)·통로 영역(passageRectPx)을 함께 로드한다.
 * 텍스처는 {@link GameAssets} 생명주기를 따른다(여기서 로드, GameAssets가 dispose). 모두 Nearest.
 */
public final class ExteriorWallAssets implements Disposable {

    private final Map<String, Texture> textures = new LinkedHashMap<>();
    private final TextureRegion north, south, west, east, cornerNW, cornerNE, cornerSW, cornerSE;
    private final TextureRegion gateClosed, gateOpen;

    public final int wallThicknessTiles;          // 권장 2
    public final int gateWorldWidthTiles, gateWorldHeightTiles; // 8 x 4
    public final int gateImageW, gateImageH;       // 512 x 256
    // layout.json(좌상단 원점) 그대로 보관: 변환은 렌더/충돌에서 수행
    public final int gateAnchorPxX, gateAnchorPxY; // 256, 246
    public final int passagePxX, passagePxY, passagePxW, passagePxH; // 184,76,176,170

    public ExteriorWallAssets() {
        String root = AssetPaths.EXTERIOR_DIR;
        north = load(root + "runtime/walls/wall_north.png");
        south = load(root + "runtime/walls/wall_south.png");
        west = load(root + "runtime/walls/wall_west.png");
        east = load(root + "runtime/walls/wall_east.png");
        cornerNW = load(root + "runtime/walls/wall_corner_nw.png");
        cornerNE = load(root + "runtime/walls/wall_corner_ne.png");
        cornerSW = load(root + "runtime/walls/wall_corner_sw.png");
        cornerSE = load(root + "runtime/walls/wall_corner_se.png");
        gateClosed = load(root + "runtime/gate/main_gate_closed_512x256.png");
        gateOpen = load(root + "runtime/gate/main_gate_open_512x256.png");

        JsonValue layout = new JsonReader().parse(
            Gdx.files.internal(AssetPaths.EXTERIOR_LAYOUT_JSON).readString("UTF-8"));
        JsonValue wall = layout.get("wall");
        wallThicknessTiles = wall.getInt("recommendedWorldThicknessTiles", 2);
        JsonValue gate = layout.get("gate");
        int[] size = gate.get("recommendedWorldSizeTiles").asIntArray();
        gateWorldWidthTiles = size[0]; gateWorldHeightTiles = size[1];
        int[] img = gate.get("imageSize").asIntArray();
        gateImageW = img[0]; gateImageH = img[1];
        int[] anchor = gate.get("anchorPx").asIntArray();
        gateAnchorPxX = anchor[0]; gateAnchorPxY = anchor[1];
        int[] p = gate.get("passageRectPx").asIntArray();
        passagePxX = p[0]; passagePxY = p[1]; passagePxW = p[2]; passagePxH = p[3];
    }

    private TextureRegion load(String path) {
        Texture tex = GameAssets.loadTile(path); // Nearest + ClampToEdge
        textures.put(path, tex);
        return new TextureRegion(tex);
    }

    public TextureRegion north() { return north; }
    public TextureRegion south() { return south; }
    public TextureRegion west() { return west; }
    public TextureRegion east() { return east; }
    public TextureRegion cornerNW() { return cornerNW; }
    public TextureRegion cornerNE() { return cornerNE; }
    public TextureRegion cornerSW() { return cornerSW; }
    public TextureRegion cornerSE() { return cornerSE; }
    public TextureRegion gate(boolean open) { return open ? gateOpen : gateClosed; }

    @Override public void dispose() {
        for (Texture tex : textures.values()) tex.dispose();
    }
}

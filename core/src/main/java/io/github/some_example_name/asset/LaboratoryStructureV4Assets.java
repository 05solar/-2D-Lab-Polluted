package io.github.some_example_name.asset;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import io.github.some_example_name.config.AssetPaths;
import io.github.some_example_name.world.WallMaskV4;

/**
 * laboratory_structure_v4 통합 벽/문 에셋. 47종 연결 벽 아틀라스(8열×6행, 128px)와
 * 가로/세로 방문(닫힘/열림) 스프라이트를 로드한다. 벽 타일은 {@link WallMaskV4#atlasIndex}로 선택한다.
 *
 * 텍스처는 {@link GameAssets} 생명주기를 따른다(여기서 로드, GameAssets가 dispose). 모두 Nearest.
 */
public final class LaboratoryStructureV4Assets implements Disposable {

    private final Texture wallAtlas;
    private final TextureRegion[] wallTiles = new TextureRegion[WallMaskV4.VARIANT_COUNT];
    private final Texture doorHClosedTex, doorHOpenTex, doorVClosedTex, doorVOpenTex;
    private final TextureRegion doorHClosed, doorHOpen, doorVClosed, doorVOpen;

    public LaboratoryStructureV4Assets() {
        wallAtlas = GameAssets.loadTile(AssetPaths.STRUCTURE_V4_WALL_ATLAS);
        int cols = WallMaskV4.ATLAS_COLUMNS, px = WallMaskV4.TILE_PIXELS;
        for (int index = 0; index < WallMaskV4.VARIANT_COUNT; index++) {
            int col = index % cols, row = index / cols;
            wallTiles[index] = new TextureRegion(wallAtlas, col * px, row * px, px, px);
        }
        doorHClosedTex = GameAssets.loadTile(AssetPaths.STRUCTURE_V4_DOOR_H_CLOSED);
        doorHOpenTex = GameAssets.loadTile(AssetPaths.STRUCTURE_V4_DOOR_H_OPEN);
        doorVClosedTex = GameAssets.loadTile(AssetPaths.STRUCTURE_V4_DOOR_V_CLOSED);
        doorVOpenTex = GameAssets.loadTile(AssetPaths.STRUCTURE_V4_DOOR_V_OPEN);
        doorHClosed = new TextureRegion(doorHClosedTex);
        doorHOpen = new TextureRegion(doorHOpenTex);
        doorVClosed = new TextureRegion(doorVClosedTex);
        doorVOpen = new TextureRegion(doorVOpenTex);
    }

    /** 정규 마스크 아틀라스 인덱스(0..46)에 해당하는 벽 타일. */
    public TextureRegion wall(int atlasIndex) { return wallTiles[atlasIndex]; }

    /** 가로벽 방문(남북 통행). open이면 열린 문짝, 아니면 닫힘. */
    public TextureRegion horizontalDoor(boolean open) { return open ? doorHOpen : doorHClosed; }

    /** 세로벽 방문(동서 통행, 전용 탑뷰 아트). open이면 열린 문짝, 아니면 닫힘. */
    public TextureRegion verticalDoor(boolean open) { return open ? doorVOpen : doorVClosed; }

    @Override public void dispose() {
        wallAtlas.dispose();
        doorHClosedTex.dispose();
        doorHOpenTex.dispose();
        doorVClosedTex.dispose();
        doorVOpenTex.dispose();
    }
}

package io.github.some_example_name.render;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import io.github.some_example_name.world.LaboratoryTileSetV2;
import io.github.some_example_name.world.RoomVisuals;

/**
 * 미리 계산된 시각 타일({@link RoomVisuals})을 레이어 순서대로 그리기만 한다.
 * 맵 구조·오토타일·타일 선택을 여기서 결정하지 않는다. 타일 1칸 = 월드 단위 1x1.
 *
 * 레이어(한 배열에 섞지 않고 각 레이어를 전체 맵에 대해 순서대로):
 *   1. floor(모든 셀, 불투명)  2. overlay  3. wall  4. structure(문)
 *
 * 바닥은 모든 셀에 존재하므로 벽/문/오버레이의 투명 영역 아래로 항상 바닥이 보인다(검은 배경 없음).
 * 바닥은 불투명(RGB)이라 블렌딩을 꺼서 그리고, 투명(RGBA) 레이어 전에 다시 켠다.
 */
public class WorldRenderer {

    private final LaboratoryTileSetV2 tileSet;

    public WorldRenderer(LaboratoryTileSetV2 tileSet) {
        this.tileSet = tileSet;
    }

    public void render(SpriteBatch batch, RoomVisuals visuals) {
        int w = visuals.widthInTiles();
        int h = visuals.heightInTiles();

        // 1. 기본 바닥(불투명) — 모든 셀. 블렌딩 off로 그린다.
        batch.disableBlending();
        for (int ty = 0; ty < h; ty++) {
            for (int tx = 0; tx < w; tx++) {
                String id = visuals.floorAt(tx, ty);
                if (id != null) batch.draw(tileSet.region(id), tx, ty, 1f, 1f);
            }
        }

        // 투명 레이어 전에 알파 블렌딩 보장.
        batch.enableBlending();
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        drawLayer(batch, visuals, Layer.OVERLAY, w, h);
        drawLayer(batch, visuals, Layer.WALL, w, h);
        drawLayer(batch, visuals, Layer.STRUCTURE, w, h);
    }

    private enum Layer { OVERLAY, WALL, STRUCTURE }

    private void drawLayer(SpriteBatch batch, RoomVisuals visuals, Layer layer, int w, int h) {
        for (int ty = 0; ty < h; ty++) {
            for (int tx = 0; tx < w; tx++) {
                String id;
                switch (layer) {
                    case OVERLAY:   id = visuals.overlayAt(tx, ty); break;
                    case WALL:      id = visuals.wallAt(tx, ty); break;
                    default:        id = visuals.structureAt(tx, ty); break;
                }
                if (id != null) batch.draw(tileSet.region(id), tx, ty, 1f, 1f);
            }
        }
    }
}

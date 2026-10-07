package io.github.some_example_name.render;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import io.github.some_example_name.world.LaboratoryTileSetV2;
import io.github.some_example_name.world.RoomVisuals;

/**
 * 미리 계산된 시각 타일({@link RoomVisuals})을 레이어 순서대로 그리기만 한다.
 * 맵 구조·오토타일·타일 선택을 여기서 결정하지 않는다. 타일 1칸 = 월드 단위 1x1.
 *
 * 렌더 레이어(한 배열에 섞지 않고 각 레이어를 전체 맵에 대해 순서대로):
 *   1. floor  2. overlay  3. wall  4. structure(문)
 * (플레이어/몬스터·이펙트·UI·디버그는 Screen이 이 뒤에 그린다)
 */
public class WorldRenderer {

    private final LaboratoryTileSetV2 tileSet;

    public WorldRenderer(LaboratoryTileSetV2 tileSet) {
        this.tileSet = tileSet;
    }

    public void render(SpriteBatch batch, RoomVisuals visuals) {
        int w = visuals.widthInTiles();
        int h = visuals.heightInTiles();

        // 1. 기본 바닥
        for (int ty = 0; ty < h; ty++) {
            for (int tx = 0; tx < w; tx++) {
                String id = visuals.floorAt(tx, ty);
                if (id != null) batch.draw(tileSet.region(id), tx, ty, 1f, 1f);
            }
        }
        // 2. 바닥 오버레이
        for (int ty = 0; ty < h; ty++) {
            for (int tx = 0; tx < w; tx++) {
                String id = visuals.overlayAt(tx, ty);
                if (id != null) batch.draw(tileSet.region(id), tx, ty, 1f, 1f);
            }
        }
        // 3. 벽
        for (int ty = 0; ty < h; ty++) {
            for (int tx = 0; tx < w; tx++) {
                String id = visuals.wallAt(tx, ty);
                if (id != null) batch.draw(tileSet.region(id), tx, ty, 1f, 1f);
            }
        }
        // 4. 문·구조물
        for (int ty = 0; ty < h; ty++) {
            for (int tx = 0; tx < w; tx++) {
                String id = visuals.structureAt(tx, ty);
                if (id != null) batch.draw(tileSet.region(id), tx, ty, 1f, 1f);
            }
        }
    }
}

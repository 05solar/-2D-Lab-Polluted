package io.github.some_example_name.render;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.github.some_example_name.world.RoomVisuals;
import io.github.some_example_name.world.TileVisual;

/**
 * 미리 계산된 시각 타일({@link RoomVisuals})을 그리기만 한다.
 * 맵 생성 규칙·타일 선택·충돌 판정을 여기에 두지 않는다.
 * 타일 1칸 = 월드 단위 1x1.
 */
public class WorldRenderer {

    private static final int TILESET_COLUMNS = 4;

    private final TextureRegion[][] tileRegions; // [row][col]

    public WorldRenderer(TextureRegion[][] tileRegions) {
        this.tileRegions = tileRegions;
    }

    public void render(SpriteBatch batch, RoomVisuals visuals) {
        for (int ty = 0; ty < visuals.heightInTiles(); ty++) {
            for (int tx = 0; tx < visuals.widthInTiles(); tx++) {
                TileVisual visual = visuals.visualAt(tx, ty);
                batch.draw(regionFor(visual.tilesetIndex), tx, ty, 1f, 1f);
            }
        }
    }

    private TextureRegion regionFor(int tilesetIndex) {
        return tileRegions[tilesetIndex / TILESET_COLUMNS][tilesetIndex % TILESET_COLUMNS];
    }
}

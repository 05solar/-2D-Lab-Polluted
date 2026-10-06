package io.github.some_example_name.render;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.github.some_example_name.world.RoomVisuals;
import io.github.some_example_name.world.TileVisual;

/**
 * 미리 계산된 시각 타일({@link RoomVisuals})을 그리기만 한다.
 * 맵 생성 규칙·타일 선택·충돌 판정을 여기에 두지 않는다.
 * 타일 1칸 = 월드 단위 1x1.
 *
 * 타일 경계 선(bleeding) 방지: 비정수 배율(창 크기/뷰포트)에서 Nearest 샘플러가 이웃 타일
 * 조각의 텍셀을 집어 얇은 선이 보일 수 있다. 각 영역을 생성 시 반텍셀씩 안으로 줄여
 * (inset) 이웃 텍셀을 샘플링하지 않게 한다. 목적 크기는 1x1 그대로라 타일 사이 간격은 없다.
 */
public class WorldRenderer {

    private static final int TILESET_COLUMNS = 4;

    private final TextureRegion[][] inset; // [row][col], 반텍셀 안으로 줄인 복사본

    public WorldRenderer(TextureRegion[][] tileRegions) {
        this.inset = buildInsetRegions(tileRegions);
    }

    private static TextureRegion[][] buildInsetRegions(TextureRegion[][] src) {
        TextureRegion[][] out = new TextureRegion[src.length][];
        for (int r = 0; r < src.length; r++) {
            out[r] = new TextureRegion[src[r].length];
            for (int c = 0; c < src[r].length; c++) {
                TextureRegion region = new TextureRegion(src[r][c]);
                float du = 0.5f / region.getTexture().getWidth();
                float dv = 0.5f / region.getTexture().getHeight();
                region.setRegion(region.getU() + du, region.getV() + dv,
                                 region.getU2() - du, region.getV2() - dv);
                out[r][c] = region;
            }
        }
        return out;
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
        return inset[tilesetIndex / TILESET_COLUMNS][tilesetIndex % TILESET_COLUMNS];
    }
}

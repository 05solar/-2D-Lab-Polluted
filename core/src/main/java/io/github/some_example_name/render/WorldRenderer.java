package io.github.some_example_name.render;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import io.github.some_example_name.world.LaboratoryTileSetV2;
import io.github.some_example_name.world.RoomVisuals;
import io.github.some_example_name.asset.RoomKitAssets;
import io.github.some_example_name.asset.LaboratoryFurnitureAssets;
import io.github.some_example_name.world.LaboratoryProp;

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
    private final RoomKitAssets roomKits;
    private final LaboratoryFurnitureAssets furniture;

    public WorldRenderer(LaboratoryTileSetV2 tileSet) {
        this(tileSet, null, null);
    }

    public WorldRenderer(LaboratoryTileSetV2 tileSet, RoomKitAssets roomKits) {
        this(tileSet, roomKits, null);
    }

    public WorldRenderer(LaboratoryTileSetV2 tileSet, RoomKitAssets roomKits,
                         LaboratoryFurnitureAssets furniture) {
        this.tileSet = tileSet;
        this.roomKits = roomKits;
        this.furniture = furniture;
    }

    public void render(SpriteBatch batch, RoomVisuals visuals) {
        render(batch, visuals, null, Float.NEGATIVE_INFINITY);
    }

    /** Draws only camera-visible cells and furniture behind the player. */
    public void render(SpriteBatch batch, RoomVisuals visuals, OrthographicCamera camera,
                       float playerFeetY) {
        int w = visuals.widthInTiles();
        int h = visuals.heightInTiles();
        int minX = camera == null ? 0 : Math.max(0, (int)Math.floor(camera.position.x - camera.viewportWidth / 2f) - 1);
        int maxX = camera == null ? w - 1 : Math.min(w - 1, (int)Math.ceil(camera.position.x + camera.viewportWidth / 2f) + 1);
        int minY = camera == null ? 0 : Math.max(0, (int)Math.floor(camera.position.y - camera.viewportHeight / 2f) - 1);
        int maxY = camera == null ? h - 1 : Math.min(h - 1, (int)Math.ceil(camera.position.y + camera.viewportHeight / 2f) + 1);

        // 1. 기본 바닥(불투명) — 모든 셀. 블렌딩 off로 그린다.
        batch.disableBlending();
        for (int ty = minY; ty <= maxY; ty++) {
            for (int tx = minX; tx <= maxX; tx++) {
                String id = visuals.floorAt(tx, ty);
                if (id != null) batch.draw(tileSet.region(id), tx, ty, 1f, 1f);
            }
        }

        // 투명 레이어 전에 알파 블렌딩 보장.
        batch.enableBlending();
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        drawLayer(batch, visuals, Layer.OVERLAY, minX, maxX, minY, maxY);
        drawLayer(batch, visuals, Layer.WALL, minX, maxX, minY, maxY);
        drawLayer(batch, visuals, Layer.STRUCTURE, minX, maxX, minY, maxY);
        if (roomKits != null) {
            for (LaboratoryProp prop : visuals.props()) {
                if (prop.y() >= playerFeetY && intersectsView(prop, camera)) drawProp(batch, prop);
            }
            if (w > 20 && h > 15 && inView(.5f, -1.5f, 6f, 4f, camera))
                batch.draw(roomKits.region("gate_horizontal_open"), .5f, -1.5f, 6f, 4f);
        }
    }

    /** Furniture south of the player is foreground and is drawn after the player sprite. */
    public void renderForegroundProps(SpriteBatch batch, RoomVisuals visuals,
                                      OrthographicCamera camera, float playerFeetY) {
        if (roomKits == null) return;
        for (LaboratoryProp prop : visuals.props())
            if (prop.y() < playerFeetY && intersectsView(prop, camera)) drawProp(batch, prop);
    }

    private void drawProp(SpriteBatch batch, LaboratoryProp prop) {
        LaboratoryFurnitureAssets.Sprite measured = furniture == null ? null
            : (prop.id().startsWith("v7:") ? furniture.sprite(prop.id())
                : furniture.forRoomKitId(prop.id()));
        com.badlogic.gdx.graphics.g2d.TextureRegion region = measured == null
            ? roomKits.region(prop.id()) : measured.region;
        float height = prop.height();
        float width = height * region.getRegionWidth() / (float)region.getRegionHeight();
        int sourceW = region.getRegionWidth(), sourceH = region.getRegionHeight();
        int[] bounds = measured == null ? roomKits.alphaBounds(prop.id())
            : new int[]{measured.minX, measured.minY, measured.maxX, measured.maxY};
        if (bounds == null) bounds = new int[]{0, 0, sourceW, sourceH};
        float drawX = prop.x() - ((bounds[0] + bounds[2]) * .5f / sourceW) * width;
        float drawY = prop.y() - (sourceH - bounds[3]) / (float)sourceH * height;
        batch.draw(region, drawX, drawY, width, height);
    }

    private boolean intersectsView(LaboratoryProp prop, OrthographicCamera camera) {
        if (camera == null) return true;
        float height = prop.height();
        LaboratoryFurnitureAssets.Sprite measured = furniture == null ? null
            : (prop.id().startsWith("v7:") ? furniture.sprite(prop.id())
                : furniture.forRoomKitId(prop.id()));
        com.badlogic.gdx.graphics.g2d.TextureRegion region = measured == null
            ? roomKits.region(prop.id()) : measured.region;
        int sourceW = region.getRegionWidth(), sourceH = region.getRegionHeight();
        float width = height * sourceW / sourceH;
        int[] bounds = measured == null ? roomKits.alphaBounds(prop.id())
            : new int[]{measured.minX, measured.minY, measured.maxX, measured.maxY};
        if (bounds == null) bounds = new int[]{0, 0, sourceW, sourceH};
        float x = prop.x() - ((bounds[0] + bounds[2]) * .5f / sourceW) * width;
        float y = prop.y() - (sourceH - bounds[3]) / (float)sourceH * height;
        float visibleW = (bounds[2] - bounds[0]) / (float)sourceW * width;
        float visibleH = (bounds[3] - bounds[1]) / (float)sourceH * height;
        return inView(x + bounds[0] / (float)sourceW * width,
            y + (sourceH - bounds[3]) / (float)sourceH * height,
            visibleW, visibleH, camera);
    }

    private static boolean inView(float x, float y, float width, float height,
                                  OrthographicCamera camera) {
        return camera == null || (x < camera.position.x + camera.viewportWidth / 2f + 1f
            && x + width > camera.position.x - camera.viewportWidth / 2f - 1f
            && y < camera.position.y + camera.viewportHeight / 2f + 1f
            && y + height > camera.position.y - camera.viewportHeight / 2f - 1f);
    }

    private enum Layer { OVERLAY, WALL, STRUCTURE }

    private void drawLayer(SpriteBatch batch, RoomVisuals visuals, Layer layer,
                           int minX, int maxX, int minY, int maxY) {
        for (int ty = minY; ty <= maxY; ty++) {
            for (int tx = minX; tx <= maxX; tx++) {
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

package io.github.some_example_name.render;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.github.some_example_name.asset.ExteriorWallAssets;
import io.github.some_example_name.world.LaboratoryRoom;
import io.github.some_example_name.world.TileType;
import io.github.some_example_name.world.WallAutotiler;

/**
 * 내부 벽의 "직선 벽면"을 새 exterior_walls_gate_v3 금속 직선으로 덧그린다(가로/세로).
 * 이 맵은 벽 덩어리에서 방·복도를 파낸 구조라, 방 경계의 직선 벽은 한쪽만 바닥인 T_OPEN_* 로 분류된다.
 * 따라서 T_OPEN_*(바닥 쪽 면) + HORIZONTAL/VERTICAL(양면 얇은 벽)을 직선으로 보고 바닥 쪽 면에 맞춰 그린다.
 * 모서리(OUTER/INNER)·십자(CROSS)·끝단(END)·세로 방문 등 접합부는 기존 v3 타일을 그대로 둔다.
 * 외곽 2타일 링은 {@link ExteriorWallRenderer}가 담당하므로 제외한다(x,y ≥2 … ≤w/h-3).
 *
 * WorldRenderer의 WALL 레이어 직후(가구/플레이어보다 먼저) 같은 batch로 그린다. 두께는 1타일.
 */
public final class InteriorWallRenderer {
    private final WallAutotiler autotiler = new WallAutotiler();
    private final ExteriorWallAssets a;

    public InteriorWallRenderer(ExteriorWallAssets assets) { this.a = assets; }

    public void draw(SpriteBatch batch, LaboratoryRoom room) {
        int w = room.widthInTiles(), h = room.heightInTiles();
        // 가로 직선: 같은 면(north/south)이 연속되는 구간을 한 번에 신축해 그린다.
        for (int y = 2; y < h - 2; y++) {
            int x = 2;
            while (x < w - 2) {
                TextureRegion face = horizontalFace(room, x, y);
                if (face == null) { x++; continue; }
                int start = x;
                while (x < w - 2 && horizontalFace(room, x, y) == face) x++;
                batch.draw(face, start, y, x - start, 1f);
            }
        }
        // 세로 직선.
        for (int x = 2; x < w - 2; x++) {
            int y = 2;
            while (y < h - 2) {
                TextureRegion face = verticalFace(room, x, y);
                if (face == null) { y++; continue; }
                int start = y;
                while (y < h - 2 && verticalFace(room, x, y) == face) y++;
                batch.draw(face, x, start, 1f, y - start);
            }
        }
    }

    /** 가로 직선 벽이면 바닥 쪽을 향한 면(north/south)을, 아니면 null. */
    private TextureRegion horizontalFace(LaboratoryRoom room, int x, int y) {
        if (room.tileAt(x, y) != TileType.WALL) return null;
        switch (autotiler.classify(room, x, y)) {
            case T_OPEN_NORTH: return a.south();                  // 바닥이 북 → 위쪽 면
            case T_OPEN_SOUTH: return a.north();                  // 바닥이 남 → 아래쪽 면
            case HORIZONTAL:
                return isFloor(room, x, y - 1) && !isFloor(room, x, y + 1) ? a.north()
                     : isFloor(room, x, y + 1) && !isFloor(room, x, y - 1) ? a.south() : a.north();
            default: return null;
        }
    }

    /** 세로 직선 벽이면 바닥 쪽을 향한 면(west/east)을, 아니면 null. */
    private TextureRegion verticalFace(LaboratoryRoom room, int x, int y) {
        if (room.tileAt(x, y) != TileType.WALL) return null;
        switch (autotiler.classify(room, x, y)) {
            case T_OPEN_EAST: return a.west();                    // 바닥이 동 → 오른쪽 향 면
            case T_OPEN_WEST: return a.east();                    // 바닥이 서 → 왼쪽 향 면
            case VERTICAL:
                return isFloor(room, x + 1, y) && !isFloor(room, x - 1, y) ? a.west()
                     : isFloor(room, x - 1, y) && !isFloor(room, x + 1, y) ? a.east() : a.west();
            default: return null;
        }
    }

    private boolean isFloor(LaboratoryRoom room, int x, int y) {
        if (x < 0 || y < 0 || x >= room.widthInTiles() || y >= room.heightInTiles()) return false;
        TileType t = room.tileAt(x, y);
        return t == TileType.FLOOR || t == TileType.DOORWAY_OPEN;
    }
}

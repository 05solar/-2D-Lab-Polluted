package io.github.some_example_name.render;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import io.github.some_example_name.asset.LaboratoryStructureV4Assets;
import io.github.some_example_name.world.Door;
import io.github.some_example_name.world.LaboratoryRoom;
import io.github.some_example_name.world.TileType;
import io.github.some_example_name.world.WallMaskV4;

/**
 * 연구소 벽·문을 단일 논리 점유 데이터({@link LaboratoryRoom})에서 V4 규격으로 그린다.
 * 벽 셀마다 8방향 마스크로 47종 아틀라스 타일을 고르고, 내부 문은 가로/세로 전용 스프라이트를
 * 개구부에 맞춰 그린다. 구형 WALL/STRUCTURE 레이어 스킨·InteriorWall·ExteriorWall 링을 대체한다.
 *
 * 타일 1칸 = 월드 1x1. WorldRenderer의 바닥/오버레이 직후, 가구/플레이어보다 먼저 같은 batch로 그린다.
 */
public final class LaboratoryStructureRendererV4 {

    private final LaboratoryStructureV4Assets assets;

    // 문 스프라이트 권장 크기(door_catalog.json): 가로 4x1.5셀, 세로 1.5x4셀. 아트 비율과 일치.
    private static final float DOOR_LONG = 4f, DOOR_THICK = 1.5f;

    public LaboratoryStructureRendererV4(LaboratoryStructureV4Assets assets) {
        this.assets = assets;
    }

    public void drawWalls(SpriteBatch batch, LaboratoryRoom room, OrthographicCamera camera) {
        int w = room.widthInTiles(), h = room.heightInTiles();
        int minX = camera == null ? 0 : Math.max(0, (int) Math.floor(camera.position.x - camera.viewportWidth / 2f) - 1);
        int maxX = camera == null ? w - 1 : Math.min(w - 1, (int) Math.ceil(camera.position.x + camera.viewportWidth / 2f) + 1);
        int minY = camera == null ? 0 : Math.max(0, (int) Math.floor(camera.position.y - camera.viewportHeight / 2f) - 1);
        int maxY = camera == null ? h - 1 : Math.min(h - 1, (int) Math.ceil(camera.position.y + camera.viewportHeight / 2f) + 1);
        for (int ty = minY; ty <= maxY; ty++) {
            for (int tx = minX; tx <= maxX; tx++) {
                if (room.rawTileAt(tx, ty) != TileType.WALL) continue;
                // 바닥/문에 노출된 벽선, 또는 강제 영역(외벽 링)만 그린다. 방 사이 깊은 벽 덩어리는
                // 그리지 않아 참고 배치처럼 비플레이 검은 영역으로 남긴다.
                if (!exposed(room, tx, ty) && !room.forcedWall(tx, ty)) continue;
                int index = WallMaskV4.atlasIndex(room, tx, ty);
                batch.draw(assets.wall(index), tx, ty, 1f, 1f);
            }
        }
    }

    /** 8방향 이웃 중 하나라도 바닥/열린 통로면 노출된 벽(그려야 함). */
    private static boolean exposed(LaboratoryRoom room, int tx, int ty) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) continue;
                TileType t = room.rawTileAt(tx + dx, ty + dy);
                if (t == TileType.FLOOR || t == TileType.DOORWAY_OPEN) return true;
            }
        }
        return false;
    }

    /** 내부 문짝/문틀을 개구부에 맞춰 그린다. 문틀 끝은 양옆 벽과 시각적으로 이어진다. */
    public void drawDoors(SpriteBatch batch, LaboratoryRoom room, OrthographicCamera camera) {
        for (Door door : room.doors()) {
            boolean open = door.tileType() == TileType.DOORWAY_OPEN;
            float cx = door.x() + .5f, cy = door.y() + .5f;
            if (door.orientation() == Door.Orientation.HORIZONTAL) {
                // 벽이 동서로 뻗음 → 남북 통행. 가로 문 스프라이트(길이=동서, 두께=남북).
                if (!inView(cx - DOOR_LONG / 2f, cy - DOOR_THICK / 2f, DOOR_LONG, DOOR_THICK, camera)) continue;
                batch.draw(assets.horizontalDoor(open), cx - DOOR_LONG / 2f, cy - DOOR_THICK / 2f,
                    DOOR_LONG, DOOR_THICK);
            } else {
                // 벽이 남북으로 뻗음 → 동서 통행. 세로 문 스프라이트(길이=남북, 두께=동서).
                if (!inView(cx - DOOR_THICK / 2f, cy - DOOR_LONG / 2f, DOOR_THICK, DOOR_LONG, camera)) continue;
                batch.draw(assets.verticalDoor(open), cx - DOOR_THICK / 2f, cy - DOOR_LONG / 2f,
                    DOOR_THICK, DOOR_LONG);
            }
        }
    }

    private static boolean inView(float x, float y, float width, float height, OrthographicCamera camera) {
        return camera == null || (x < camera.position.x + camera.viewportWidth / 2f + 1f
            && x + width > camera.position.x - camera.viewportWidth / 2f - 1f
            && y < camera.position.y + camera.viewportHeight / 2f + 1f
            && y + height > camera.position.y - camera.viewportHeight / 2f - 1f);
    }
}

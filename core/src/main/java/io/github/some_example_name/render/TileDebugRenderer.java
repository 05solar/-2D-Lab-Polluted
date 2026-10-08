package io.github.some_example_name.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import io.github.some_example_name.world.Hazard;
import io.github.some_example_name.world.Door;
import io.github.some_example_name.world.LaboratoryRoom;
import io.github.some_example_name.world.RoomVisuals;
import io.github.some_example_name.world.TileType;
import io.github.some_example_name.world.WallAutotiler;

/**
 * F2 타일 시각 디버그(기본 off, 실제 렌더와 분리).
 * 셀 테두리 색으로 타입을 표시하고(바닥 회색/벽 빨강/닫힌 문 주황/열린 문 초록/위험 보라),
 * 각 셀에 레이어 ID(F/O/W/S)를 작게 출력한다.
 */
public class TileDebugRenderer {

    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont font = new BitmapFont(); // 내장 폰트

    public TileDebugRenderer() {
        // 월드 단위(1칸=1유닛)에 맞게 축소: 긴 벽 ID도 한 셀에 들어가도록 글자 높이 ~0.11유닛.
        // BitmapFont의 기본 정수 좌표 반올림은 1유닛보다 작은 글리프를 뭉개므로 끈다.
        font.setUseIntegerPositions(false);
        font.getData().setScale(0.11f / font.getLineHeight());
        font.setColor(1f, 1f, 1f, 0.85f);
    }

    public void render(OrthographicCamera camera, LaboratoryRoom room, RoomVisuals visuals, SpriteBatch batch) {
        int w = visuals.widthInTiles();
        int h = visuals.heightInTiles();

        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Line);
        for (int ty = 0; ty < h; ty++) {
            for (int tx = 0; tx < w; tx++) {
                shapes.setColor(borderColor(room, tx, ty));
                shapes.rect(tx, ty, 1f, 1f);
                if (room.isSolid(tx, ty)) {
                    shapes.setColor(Color.CYAN);
                    shapes.rect(tx + 0.08f, ty + 0.08f, 0.84f, 0.84f);
                }
            }
        }
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int ty = 0; ty < h; ty++) {
            for (int tx = 0; tx < w; tx++) {
                String id = visualId(visuals, tx, ty);
                if (id == null) continue;
                int mask = WallAutotiler.connectors(id);
                if (mask == 0) {
                    shapes.setColor(Color.PURPLE);
                    shapes.circle(tx + 0.5f, ty + 0.5f, 0.07f, 8);
                    continue;
                }
                connectionPoint(shapes, visuals, tx, ty, mask, WallAutotiler.NORTH, 0, 1, 0.5f, 0.95f);
                connectionPoint(shapes, visuals, tx, ty, mask, WallAutotiler.EAST, 1, 0, 0.95f, 0.5f);
                connectionPoint(shapes, visuals, tx, ty, mask, WallAutotiler.SOUTH, 0, -1, 0.5f, 0.05f);
                connectionPoint(shapes, visuals, tx, ty, mask, WallAutotiler.WEST, -1, 0, 0.05f, 0.5f);
                Door door = room.doorAt(tx, ty);
                if (door != null) {
                    int expected = door.orientation() == Door.Orientation.HORIZONTAL
                        ? WallAutotiler.EAST | WallAutotiler.WEST
                        : WallAutotiler.NORTH | WallAutotiler.SOUTH;
                    if (mask != expected) {
                        shapes.setColor(Color.YELLOW);
                        shapes.circle(tx + 0.5f, ty + 0.5f, 0.10f, 8);
                    }
                }
            }
        }
        shapes.end();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        for (int ty = 0; ty < h; ty++) {
            for (int tx = 0; tx < w; tx++) {
                float yTop = ty + 0.95f;
                line(batch, "F:" + shortId(visuals.floorAt(tx, ty)), tx, yTop);
                line(batch, "O:" + shortId(visuals.overlayAt(tx, ty)), tx, yTop - 0.20f);
                line(batch, "W:" + shortId(visuals.wallAt(tx, ty)), tx, yTop - 0.40f);
                line(batch, "S:" + shortId(visuals.structureAt(tx, ty)), tx, yTop - 0.60f);
                String id = visualId(visuals, tx, ty);
                if (id != null) {
                    Door door = room.doorAt(tx, ty);
                    String state = door == null ? "" : "/" +
                        (door.orientation() == Door.Orientation.HORIZONTAL ? "H" : "V") +
                        "/" + door.state().name();
                    line(batch, "M:" + WallAutotiler.connectors(id) + state,
                        tx, yTop - 0.80f);
                }
            }
        }
        batch.end();
    }

    private void line(SpriteBatch batch, String s, float tx, float y) {
        font.draw(batch, s, tx + 0.04f, y);
    }

    private static String visualId(RoomVisuals visuals, int x, int y) {
        String structure = visuals.structureAt(x, y);
        return structure != null ? structure : visuals.wallAt(x, y);
    }

    private static void connectionPoint(ShapeRenderer shapes, RoomVisuals visuals,
            int x, int y, int mask, int direction, int dx, int dy, float offsetX, float offsetY) {
        if ((mask & direction) == 0) return;
        int nx = x + dx, ny = y + dy;
        String neighbor = nx < 0 || ny < 0 || nx >= visuals.widthInTiles() ||
            ny >= visuals.heightInTiles() ? null : visualId(visuals, nx, ny);
        int opposite = direction == WallAutotiler.NORTH ? WallAutotiler.SOUTH
            : direction == WallAutotiler.EAST ? WallAutotiler.WEST
            : direction == WallAutotiler.SOUTH ? WallAutotiler.NORTH : WallAutotiler.EAST;
        shapes.setColor(neighbor != null && (WallAutotiler.connectors(neighbor) & opposite) != 0
            ? Color.GREEN : Color.RED);
        shapes.circle(x + offsetX, y + offsetY, 0.055f, 8);
    }

    private Color borderColor(LaboratoryRoom room, int tx, int ty) {
        if (room.hazardAt(tx, ty) != Hazard.NONE) return Color.PURPLE;
        TileType t = room.tileAt(tx, ty);
        switch (t) {
            case WALL:         return Color.RED;
            case DOOR_CLOSED:  return Color.ORANGE;
            case DOORWAY_OPEN: return Color.GREEN;
            default:           return Color.GRAY;
        }
    }

    /** 긴 타일 ID를 접두사 떼고 짧게(디버그 가독용). */
    private static String shortId(String id) {
        if (id == null) return "-";
        int us = id.indexOf('_');
        return us >= 0 && us + 1 < id.length() ? id.substring(us + 1) : id;
    }

    public void dispose() {
        shapes.dispose();
        font.dispose();
    }
}

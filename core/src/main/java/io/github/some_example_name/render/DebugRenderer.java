package io.github.some_example_name.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import io.github.some_example_name.entity.player.Player;
import io.github.some_example_name.world.LaboratoryRoom;

/**
 * 디버그 충돌 영역 표시. 고체 타일과 플레이어 충돌 박스를 선으로 그린다.
 * 보이는 이미지와 실제 충돌 영역이 다를 수 있으므로 확인용으로 둔다. (F1로 토글)
 */
public class DebugRenderer {

    private final ShapeRenderer shapes = new ShapeRenderer();

    public void render(OrthographicCamera camera, LaboratoryRoom room, Player player) {
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Line);

        shapes.setColor(Color.FIREBRICK);
        for (int ty = 0; ty < room.heightInTiles(); ty++) {
            for (int tx = 0; tx < room.widthInTiles(); tx++) {
                if (room.isSolid(tx, ty)) {
                    shapes.rect(tx, ty, 1f, 1f);
                }
            }
        }

        shapes.setColor(Color.LIME);
        shapes.rect(player.bounds().x, player.bounds().y, player.bounds().width, player.bounds().height);

        shapes.end();
    }

    public void dispose() {
        shapes.dispose();
    }
}

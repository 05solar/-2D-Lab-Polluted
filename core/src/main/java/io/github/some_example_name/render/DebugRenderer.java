package io.github.some_example_name.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
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
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        int minX=Math.max(0,(int)Math.floor(camera.position.x-camera.viewportWidth*.5f)-1);
        int maxX=Math.min(room.widthInTiles()-1,(int)Math.ceil(camera.position.x+camera.viewportWidth*.5f)+1);
        int minY=Math.max(0,(int)Math.floor(camera.position.y-camera.viewportHeight*.5f)-1);
        int maxY=Math.min(room.heightInTiles()-1,(int)Math.ceil(camera.position.y+camera.viewportHeight*.5f)+1);
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Line);
        for (int ty=minY;ty<=maxY;ty++) for(int tx=minX;tx<=maxX;tx++) {
            shapes.setColor(room.isSolid(tx,ty) ? Color.FIREBRICK : Color.GREEN);
            shapes.rect(tx+.03f,ty+.03f,.94f,.94f);
        }
        shapes.setColor(Color.CYAN);
        for (com.badlogic.gdx.math.Rectangle obstacle : room.obstacles()) {
            if(obstacle.x+obstacle.width>=camera.position.x-camera.viewportWidth*.5f
                    && obstacle.x<=camera.position.x+camera.viewportWidth*.5f
                    && obstacle.y+obstacle.height>=camera.position.y-camera.viewportHeight*.5f
                    && obstacle.y<=camera.position.y+camera.viewportHeight*.5f)
                shapes.rect(obstacle.x,obstacle.y,obstacle.width,obstacle.height);
        }
        shapes.setColor(Color.LIME);
        shapes.rect(player.bounds().x, player.bounds().y, player.bounds().width, player.bounds().height);
        shapes.end();
        // Leave the standard alpha blend state expected by the next frame's UI/renderers.
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    public void dispose() {
        shapes.dispose();
    }
}

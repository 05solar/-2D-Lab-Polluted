package io.github.some_example_name.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;

/** Full-window black overlay in screen pixels; independent of the world camera. */
public final class FadeOverlayRenderer {
    private final Matrix4 screenProjection = new Matrix4();

    public void render(SpriteBatch batch, Texture whitePixel, float alpha) {
        if (alpha <= 0f) return;
        int width = Gdx.graphics.getWidth(), height = Gdx.graphics.getHeight();
        Gdx.gl.glViewport(0, 0, width, height);
        screenProjection.setToOrtho2D(0, 0, width, height);
        batch.setProjectionMatrix(screenProjection);
        batch.begin();
        batch.enableBlending();
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(0f, 0f, 0f, alpha);
        batch.draw(whitePixel, 0, 0, width, height);
        batch.setColor(1f, 1f, 1f, 1f);
        batch.end();
    }
}

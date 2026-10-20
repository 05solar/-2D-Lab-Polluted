package io.github.some_example_name.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.some_example_name.asset.DifficultyModalAssets;
import io.github.some_example_name.asset.DifficultyModalLayout;

/** Renders the difficulty modal entirely in screen pixels, independent of the world camera. */
public final class DifficultyModalRenderer implements com.badlogic.gdx.utils.Disposable {
    private final ShapeRenderer shapes = new ShapeRenderer();

    public void render(SpriteBatch batch, ScreenViewport viewport, DifficultyModalAssets assets,
                       int selectedIndex) {
        float width = viewport.getWorldWidth(), height = viewport.getWorldHeight();
        float scale = DifficultyModalLayout.scale(width, height);
        DifficultyModalLayout layout = assets.layout();
        DifficultyModalLayout.Rect panel = DifficultyModalLayout.panel(width, height);
        viewport.apply();
        com.badlogic.gdx.graphics.Camera camera = viewport.getCamera();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.60f);
        shapes.rect(0f, 0f, width, height);
        shapes.end();

        batch.setProjectionMatrix(camera.combined);
        batch.enableBlending();
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(Color.WHITE);
        batch.begin();
        batch.draw(assets.panel(), panel.x, panel.y, panel.width, panel.height);
        batch.setColor(Color.WHITE);
        batch.end();

        DifficultyModalLayout.Rect row = layout.optionRect(selectedIndex, width, height);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.02f, 0.58f, 0.78f, 0.16f);
        shapes.rect(row.x + 5f * scale, row.y + 5f * scale,
            row.width - 10f * scale, row.height - 10f * scale);
        shapes.end();
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(0.2f, 0.86f, 1f, 0.95f);
        shapes.rect(row.x + 5f * scale, row.y + 5f * scale,
            row.width - 10f * scale, row.height - 10f * scale);
        shapes.end();

        batch.setProjectionMatrix(camera.combined);
        batch.enableBlending();
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(Color.WHITE);
        batch.begin();
        drawCentered(batch, assets.text("title"), layout.titleRect(width, height), scale);
        // 몬스터 수 설명 없이 난이도 이름만, 각 행 가운데 정렬로 표시한다.
        String[] names = {"easy", "normal", "hard"};
        for (int i = 0; i < layout.optionCount(); i++)
            drawCentered(batch, assets.text(names[i]), layout.optionRect(i, width, height), scale);
        drawCentered(batch, assets.text("footer"), layout.footerRect(width, height), scale);
        batch.setColor(Color.WHITE);
        batch.end();
    }

    private static void drawCentered(SpriteBatch batch, DifficultyModalAssets.Text text,
                                     DifficultyModalLayout.Rect rect, float scale) {
        float width = text.width * scale, height = text.height * scale;
        batch.draw(text.texture, rect.x + (rect.width - width) / 2f,
            rect.y + (rect.height - height) / 2f, width, height);
    }

    @Override public void dispose() { shapes.dispose(); }
}

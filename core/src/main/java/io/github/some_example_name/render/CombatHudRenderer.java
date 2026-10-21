package io.github.some_example_name.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.github.some_example_name.asset.CombatUiAssets;

/**
 * 공격 키 안내 HUD(우하단). 스킬(돌진/회복)은 공통 퀵슬롯 바(SkillBarRenderer, 하단 중앙)로
 * 옮겨 여기서는 기본 공격 아이콘·키캡만 그린다 — 쿨다운 이중 표시가 생기지 않는다.
 * 공격 키 라벨은 실제 KeyBindings에서 읽은 이름을 빈 키캡 위에 그린다(임의 지정 금지).
 */
public final class CombatHudRenderer {

    private static final float MARGIN = 18f;
    private static final float ICON = 72f;
    private static final float KEYCAP = 30f;

    private final SpriteBatch batch = new SpriteBatch();
    private final OrthographicCamera camera = new OrthographicCamera();
    // 공유 Mona 폰트(GameAssets 소유 — 여기서 해제하지 않는다).
    private final BitmapFont font;
    private final GlyphLayout glyph = new GlyphLayout();

    public CombatHudRenderer(BitmapFont sharedUiFont) { this.font = sharedUiFont; }

    public void render(CombatUiAssets ui, String attackKeyLabel) {
        float sw = Gdx.graphics.getWidth(), sh = Gdx.graphics.getHeight();
        camera.setToOrtho(false, sw, sh);
        batch.setProjectionMatrix(camera.combined);
        float x = sw - MARGIN - ICON, y = MARGIN;
        batch.begin();
        drawSlot(ui.attackIcon(), ui.keyBlank(), attackKeyLabel, x, y);
        batch.end();
    }

    private void drawSlot(TextureRegion icon, TextureRegion keycap, String keyLabel,
                          float x, float y) {
        batch.draw(icon, x, y, ICON, ICON);
        float kx = x + ICON - KEYCAP * .72f, ky = y - KEYCAP * .18f + 4f;
        batch.draw(keycap, kx, ky, KEYCAP, KEYCAP);
        if (keyLabel != null && !keyLabel.isEmpty()) {
            // 빈 키캡 위 실제 바인딩 이름. 길면(CLICK 등) 키캡 폭에 맞춰 줄인다.
            float target = keyLabel.length() > 2 ? 9f : 13f;
            drawCenteredText(keyLabel, kx + KEYCAP / 2f, ky + KEYCAP / 2f + 1.5f, target, Color.BLACK);
        }
    }

    private void drawCenteredText(String text, float cx, float cy, float targetHeight, Color color) {
        font.getData().setScale(1f);
        glyph.setText(font, text);
        font.getData().setScale(targetHeight / glyph.height);
        glyph.setText(font, text);
        font.setColor(color);
        font.draw(batch, glyph, cx - glyph.width / 2f, cy + glyph.height / 2f);
        font.setColor(Color.WHITE);
    }

    public void dispose() {
        batch.dispose();
    }
}

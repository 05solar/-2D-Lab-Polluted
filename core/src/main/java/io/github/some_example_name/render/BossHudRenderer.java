package io.github.some_example_name.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import io.github.some_example_name.asset.BossAssets;

/**
 * 보스 HP바(화면 상단 중앙 UI 뷰포트 고정 — 좌상단 플레이어 HP HUD와 겹치지 않게 그 오른쪽
 * 아래 영역). 이름/현재·최대 HP/채움은 전부 런타임으로 그린다(이미지에 숫자 없음).
 * 채움 비율은 실제 HealthComponent 값에서 읽는다.
 */
public final class BossHudRenderer {

    private static final float FRAME_W = 768f, FRAME_H = 64f;
    // ui_layout.json bossHud.fillInsideFrame: 프레임 내부 (64,25,640,16) — 상단 기준 좌표.
    private static final float FILL_X = 64f, FILL_TOP = 25f, FILL_W = 640f, FILL_H = 16f;

    private final SpriteBatch batch = new SpriteBatch();
    private final OrthographicCamera camera = new OrthographicCamera();
    private final BitmapFont font; // 공유 Mona(숫자) — 해제 금지
    private final GlyphLayout glyph = new GlyphLayout();
    private final KoreanLabelCache labels = new KoreanLabelCache(18);

    public BossHudRenderer(BitmapFont sharedUiFont) { this.font = sharedUiFont; }

    public void render(BossAssets assets, String bossName, int hp, int maxHp) {
        float sw = Gdx.graphics.getWidth(), sh = Gdx.graphics.getHeight();
        camera.setToOrtho(false, sw, sh);
        batch.setProjectionMatrix(camera.combined);
        float scale = Math.min(0.75f, sw * 0.55f / FRAME_W);
        float w = FRAME_W * scale, h = FRAME_H * scale;
        float x = (sw - w) / 2f;
        float y = sh - 18f - h; // 상단 중앙(좌상단 플레이어 HUD 안전 영역 오른쪽)
        batch.begin();
        batch.draw(assets.hpFrame(), x, y, w, h);
        float ratio = maxHp <= 0 ? 0f : Math.max(0f, Math.min(1f, hp / (float) maxHp));
        float fillX = x + FILL_X * scale;
        float fillY = y + h - (FILL_TOP + FILL_H) * scale;
        batch.draw(assets.hpFill(), fillX, fillY, FILL_W * scale * ratio, FILL_H * scale);
        // 이름(한글 라벨)과 현재/최대 HP(숫자) — 프레임 위.
        com.badlogic.gdx.graphics.g2d.TextureRegion name = labels.label(bossName);
        float nameH = 16f, nameW = name.getRegionWidth() * (nameH / name.getRegionHeight());
        batch.draw(name, x + 8f, y + h + 4f, nameW, nameH);
        String text = hp + " / " + maxHp;
        font.getData().setScale(1f);
        glyph.setText(font, text);
        font.getData().setScale(14f / glyph.height);
        glyph.setText(font, text);
        font.setColor(Color.WHITE);
        font.draw(batch, glyph, x + w - glyph.width - 8f, y + h + 4f + glyph.height);
        batch.end();
    }

    public void dispose() {
        batch.dispose();
        labels.dispose();
    }
}

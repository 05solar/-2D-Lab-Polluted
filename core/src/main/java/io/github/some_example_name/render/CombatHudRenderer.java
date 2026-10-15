package io.github.some_example_name.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.github.some_example_name.asset.CombatUiAssets;
import io.github.some_example_name.combat.PlayerDashState;

/**
 * 돌진(F)·공격 키 안내 HUD. 화면 좌표(우하단)에 그려 HP(좌상단)·가방 안내(좌하단)·
 * 미니맵/귀환 버튼(우상단)과 겹치지 않는다. 창 리사이즈마다 화면 크기에서 다시 계산한다.
 *
 * 쿨다운 가림·남은 초·비활성 어둡기는 에셋에 베이크되어 있지 않고 여기서 코드로 그린다.
 * 공격 키 라벨은 실제 KeyBindings에서 읽은 이름을 빈 키캡 위에 그린다(임의 지정 금지).
 */
public final class CombatHudRenderer {

    private static final float MARGIN = 18f;
    private static final float ICON = 72f;      // 아이콘 한 변(px)
    private static final float KEYCAP = 30f;    // 키캡 한 변(px)
    private static final float SLOT_GAP = 14f;

    private final SpriteBatch batch = new SpriteBatch();
    private final OrthographicCamera camera = new OrthographicCamera();
    private final BitmapFont font = new BitmapFont();
    private final GlyphLayout glyph = new GlyphLayout();

    public void render(CombatUiAssets ui, Texture whitePixel, String attackKeyLabel,
                       PlayerDashState dash) {
        float sw = Gdx.graphics.getWidth(), sh = Gdx.graphics.getHeight();
        camera.setToOrtho(false, sw, sh);
        batch.setProjectionMatrix(camera.combined);
        // 우하단: [공격][돌진] 순서. 화면 밖으로 잘리지 않게 margin 안쪽에 고정한다.
        float dashX = sw - MARGIN - ICON;
        float attackX = dashX - SLOT_GAP - ICON;
        float y = MARGIN;
        batch.begin();
        drawSlot(ui.attackIcon(), ui.keyBlank(), attackKeyLabel, attackX, y, 1f);
        float remaining = dash.cooldownRemaining();
        boolean coolingDown = remaining > 0f && !dash.active();
        drawSlot(ui.dashIcon(), ui.keyF(), null, dashX, y, coolingDown ? .35f : 1f);
        if (coolingDown) {
            // 남은 비율만큼 아래→위 가림 + 남은 초 숫자. 3초 뒤 준비 상태로 돌아온다.
            float ratio = Math.min(1f, remaining / dash.cooldownTotal());
            batch.setColor(0f, 0f, 0f, .55f);
            batch.draw(whitePixel, dashX, y, ICON, ICON * ratio);
            batch.setColor(Color.WHITE);
            drawCenteredText(String.format("%.1f", remaining),
                dashX + ICON / 2f, y + ICON / 2f, 22f, Color.WHITE);
        }
        batch.end();
    }

    private void drawSlot(TextureRegion icon, TextureRegion keycap, String keyLabel,
                          float x, float y, float brightness) {
        batch.setColor(brightness, brightness, brightness, 1f);
        batch.draw(icon, x, y, ICON, ICON);
        // 키캡은 아이콘 우하단에 겹쳐 그린다(아이콘·키캡 모두 margin 안쪽이라 잘리지 않음).
        float kx = x + ICON - KEYCAP * .72f, ky = y - KEYCAP * .18f + 4f;
        batch.draw(keycap, kx, ky, KEYCAP, KEYCAP);
        batch.setColor(Color.WHITE);
        if (keyLabel != null && !keyLabel.isEmpty()) {
            // 빈 키캡 위 실제 바인딩 이름. 길면(Space 등) 키캡 폭에 맞춰 줄인다.
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
        font.dispose();
    }
}

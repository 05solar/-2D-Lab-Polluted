package io.github.some_example_name.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.github.some_example_name.asset.HudLayout;
import io.github.some_example_name.asset.PlayerHudAssets;
import io.github.some_example_name.entity.player.Player;
import io.github.some_example_name.inventory.Backpack;

/**
 * 화면 좌표계(월드 카메라와 분리)에 HP HUD와 8칸 가방 오버레이를 그린다. 자체 SpriteBatch/카메라/폰트를 소유한다.
 * 좌표는 {@link HudLayout}(ui_layout.json, top-left 원점)을 libGDX(bottom-left)로 변환해 사용한다.
 * 데이터(HP, 가방)를 읽기만 하며 게임 상태를 바꾸지 않는다.
 */
public final class PlayerHudRenderer {

    private final SpriteBatch batch = new SpriteBatch();
    private final OrthographicCamera camera = new OrthographicCamera();
    private final BitmapFont font = new BitmapFont(); // ASCII 숫자/용량 텍스트용(한글 알림은 래스터 라벨 사용)
    private final GlyphLayout glyph = new GlyphLayout();
    private float fullFlashTimer;

    /** 가방이 가득 차 회수가 거부됐을 때 알림을 띄운다. */
    public void flashBackpackFull() { fullFlashTimer = 2.0f; }

    public void render(PlayerHudAssets hud, Texture whitePixel, Player player,
                       Backpack backpack, boolean backpackOpen, float delta) {
        if (fullFlashTimer > 0f) fullFlashTimer = Math.max(0f, fullFlashTimer - delta);
        float sw = Gdx.graphics.getWidth(), sh = Gdx.graphics.getHeight();
        camera.setToOrtho(false, sw, sh);
        batch.setProjectionMatrix(camera.combined);
        HudLayout L = hud.layout();

        batch.begin();
        drawHealth(hud, player, L, sh);
        if (backpackOpen) drawBackpack(hud, whitePixel, backpack, L, sw, sh);
        else drawRPrompt(hud, sh);
        if (fullFlashTimer > 0f && hud.backpackFullLabel() != null) {
            TextureRegion label = hud.backpackFullLabel();
            float w = label.getRegionWidth(), h = label.getRegionHeight();
            batch.setColor(1f, 1f, 1f, Math.min(1f, fullFlashTimer));
            batch.draw(label, (sw - w) / 2f, sh * 0.82f, w, h);
            batch.setColor(Color.WHITE);
        }
        batch.end();
    }

    private void drawHealth(PlayerHudAssets hud, Player player, HudLayout L, float sh) {
        int hp = Math.max(0, Math.min(L.maxHp, player.hp()));
        // frame (top-left anchored)
        float frameX = L.marginX;
        float frameY = sh - L.marginY - L.frameH;
        batch.draw(hud.frame(), frameX, frameY, L.frameW, L.frameH);
        // fill: width scales with hp ratio
        int fillPx = Math.round(L.fillW * hp / (float) L.maxHp);
        if (fillPx > 0) {
            TextureRegion full = hud.fillFor(hp);
            TextureRegion part = new TextureRegion(full, 0, 0, fillPx, (int) L.fillH);
            float fillX = L.marginX + L.fillX;
            float fillY = sh - (L.marginY + L.fillY) - L.fillH;
            batch.draw(part, fillX, fillY, fillPx, L.fillH);
        }
        // value number, centered in value rect
        float cx = L.marginX + L.valueX + L.valueW / 2f;
        float cy = sh - (L.marginY + L.valueY + L.valueH / 2f);
        drawCenteredText(String.valueOf(hp), cx, cy, L.valueH * 0.7f, Color.WHITE);
    }

    private void drawRPrompt(PlayerHudAssets hud, float sh) {
        TextureRegion r = hud.rPrompt();
        batch.draw(r, 18f, 18f, r.getRegionWidth(), r.getRegionHeight());
    }

    private void drawBackpack(PlayerHudAssets hud, Texture whitePixel, Backpack backpack,
                              HudLayout L, float sw, float sh) {
        // dim background
        batch.setColor(0f, 0f, 0f, L.dimAlpha);
        batch.draw(whitePixel, 0f, 0f, sw, sh);
        batch.setColor(Color.WHITE);
        // panel scaled to <= viewport fraction, keep aspect, centered
        float scale = Math.min(L.viewportFractionX * sw / L.panelW, L.viewportFractionY * sh / L.panelH);
        float panelW = L.panelW * scale, panelH = L.panelH * scale;
        float px = (sw - panelW) / 2f, py = (sh - panelH) / 2f;
        float panelTop = py + panelH; // libGDX y of panel top edge
        batch.draw(hud.backpackPanel(), px, py, panelW, panelH);
        // item icons in occupied slots (placeholder tint; real item art is a connection point)
        for (int i = 0; i < backpack.count() && i < L.slotTopLeft.length; i++) {
            float ox = L.slotTopLeft[i][0] + L.iconInsetX;
            float oy = L.slotTopLeft[i][1] + L.iconInsetY;
            float ix = px + ox * scale;
            float iy = py + panelH - (oy + L.iconInsetH) * scale;
            float iw = L.iconInsetW * scale, ih = L.iconInsetH * scale;
            batch.setColor(0.23f, 0.78f, 0.80f, 0.92f);
            batch.draw(whitePixel, ix, iy, iw, ih);
            batch.setColor(Color.WHITE);
        }
        // capacity text {used} / 8
        float cx = px + (L.capacityX + L.capacityW / 2f) * scale;
        float cy = py + panelH - (L.capacityY + L.capacityH / 2f) * scale;
        drawCenteredText(backpack.count() + " / " + backpack.capacity(), cx, cy, L.capacityH * 0.55f * scale, Color.WHITE);
    }

    private void drawCenteredText(String text, float cx, float cy, float targetHeight, Color color) {
        font.getData().setScale(1f);
        glyph.setText(font, text);
        float scale = targetHeight / glyph.height;
        font.getData().setScale(scale);
        glyph.setText(font, text);
        font.setColor(color);
        font.draw(batch, glyph, cx - glyph.width / 2f, cy + glyph.height / 2f);
    }

    public void dispose() {
        batch.dispose();
        font.dispose();
    }
}

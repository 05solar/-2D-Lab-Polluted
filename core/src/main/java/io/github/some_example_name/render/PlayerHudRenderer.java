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
import io.github.some_example_name.asset.LootAssets;
import io.github.some_example_name.inventory.LootItem;

/**
 * 화면 좌표계(월드 카메라와 분리)에 HP HUD와 8칸 가방 오버레이를 그린다. 자체 SpriteBatch/카메라/폰트를 소유한다.
 * 좌표는 {@link HudLayout}(ui_layout.json, top-left 원점)을 libGDX(bottom-left)로 변환해 사용한다.
 * 데이터(HP, 가방)를 읽기만 하며 게임 상태를 바꾸지 않는다.
 */
public final class PlayerHudRenderer {

    private final SpriteBatch batch = new SpriteBatch();
    private final OrthographicCamera camera = new OrthographicCamera();
    // 숫자/용량 텍스트용 공유 Mona 폰트(GameAssets 소유 — 여기서 해제하지 않는다).
    private final BitmapFont font;
    private final GlyphLayout glyph = new GlyphLayout();

    public PlayerHudRenderer(BitmapFont sharedUiFont) { this.font = sharedUiFont; }
    private final KoreanLabelCache lootLabels = new KoreanLabelCache(22);
    private float fullFlashTimer;
    private float lootFlashTimer;
    private LootItem recentLoot;

    /** 가방이 가득 차 회수가 거부됐을 때 알림을 띄운다. */
    public void flashBackpackFull() { fullFlashTimer = 2.0f; }
    public void flashLoot(LootItem item) {
        recentLoot = item;
        lootFlashTimer = 2.0f;
    }

    public void render(PlayerHudAssets hud, LootAssets loot, Texture whitePixel, Player player,
                       Backpack backpack, boolean backpackOpen, float delta) {
        if (fullFlashTimer > 0f) fullFlashTimer = Math.max(0f, fullFlashTimer - delta);
        if (lootFlashTimer > 0f) lootFlashTimer = Math.max(0f, lootFlashTimer - delta);
        float sw = Gdx.graphics.getWidth(), sh = Gdx.graphics.getHeight();
        camera.setToOrtho(false, sw, sh);
        batch.setProjectionMatrix(camera.combined);
        HudLayout L = hud.layout();

        batch.begin();
        drawHealth(hud, player, L, sh);
        if (backpackOpen) drawBackpack(hud, loot, whitePixel, backpack, L, sw, sh);
        else drawRPrompt(hud, sh);
        if (fullFlashTimer > 0f && hud.backpackFullLabel() != null) {
            TextureRegion label = hud.backpackFullLabel();
            float w = label.getRegionWidth(), h = label.getRegionHeight();
            batch.setColor(1f, 1f, 1f, Math.min(1f, fullFlashTimer));
            batch.draw(label, (sw - w) / 2f, sh * 0.82f, w, h);
            batch.setColor(Color.WHITE);
        }
        if (lootFlashTimer > 0f && recentLoot != null)
            drawLootToast(loot, whitePixel, L, sw, sh);
        batch.end();
    }

    /**
     * 획득 토스트: 화면 중앙 상단에 아이콘 + "아이템 이름 +금액"(한글 래스터 라벨).
     * 상자 폭을 라벨 실측 폭에 맞춰 계산하므로 이름·금액이 잘리지 않는다.
     */
    private void drawLootToast(LootAssets loot, Texture whitePixel, HudLayout L,
                               float sw, float sh) {
        float alpha = Math.min(1f, lootFlashTimer);
        TextureRegion label = lootLabels.label(recentLoot.name + "  +" + recentLoot.price);
        float icon = 44f, pad = 10f;
        float boxW = pad + icon + 8f + label.getRegionWidth() + pad;
        float boxH = Math.max(icon + pad * 2f, label.getRegionHeight() + pad * 2f);
        // HP 바·오디오 버튼 줄 바로 아래 중앙 — 다른 HUD와 겹치지 않고 잘리지 않는다.
        float x = (sw - boxW) / 2f, y = sh - (L.marginY + L.frameH + 12f) - boxH;
        batch.setColor(0.025f, 0.035f, 0.055f, .88f * alpha);
        batch.draw(whitePixel, x, y, boxW, boxH);
        batch.setColor(1f, 1f, 1f, alpha);
        batch.draw(loot.itemFramed(recentLoot), x + pad, y + (boxH - icon) / 2f, icon, icon);
        batch.draw(label, x + pad + icon + 8f,
            y + (boxH - label.getRegionHeight()) / 2f);
        batch.setColor(Color.WHITE);
    }

    private void drawHealth(PlayerHudAssets hud, Player player, HudLayout L, float sh) {
        // 영구 강화로 최대 HP가 변하므로 숫자·채움·색 모두 currentHp/maxHp 비율 기준.
        int maxHp = Math.max(1, player.maxHp());
        int hp = Math.max(0, Math.min(maxHp, player.hp()));
        float frameX = L.marginX;
        float frameY = sh - L.marginY - L.frameH;
        batch.draw(hud.frame(), frameX, frameY, L.frameW, L.frameH);
        int fillPx = Math.round(L.fillW * hp / (float) maxHp);
        if (fillPx > 0) {
            TextureRegion full = hud.fillFor(hp, maxHp);
            TextureRegion part = new TextureRegion(full, 0, 0, fillPx, (int) L.fillH);
            float fillX = L.marginX + L.fillX;
            float fillY = sh - (L.marginY + L.fillY) - L.fillH;
            batch.draw(part, fillX, fillY, fillPx, L.fillH);
        }
        float cx = L.marginX + L.valueX + L.valueW / 2f;
        float cy = sh - (L.marginY + L.valueY + L.valueH / 2f);
        drawCenteredText(hp + "/" + maxHp, cx, cy, L.valueH * 0.58f, Color.WHITE);
    }

    private void drawRPrompt(PlayerHudAssets hud, float sh) {
        TextureRegion r = hud.rPrompt();
        batch.draw(r, 18f, 18f, r.getRegionWidth(), r.getRegionHeight());
    }

    private void drawBackpack(PlayerHudAssets hud, LootAssets loot, Texture whitePixel, Backpack backpack,
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
        // Items use their actual icon with the rarity frame supplied by the loot pack.
        for (int i = 0; i < backpack.count() && i < L.slotTopLeft.length; i++) {
            float ox = L.slotTopLeft[i][0] + L.iconInsetX;
            float oy = L.slotTopLeft[i][1] + L.iconInsetY;
            float ix = px + ox * scale;
            float iy = py + panelH - (oy + L.iconInsetH) * scale;
            float iw = L.iconInsetW * scale, ih = L.iconInsetH * scale;
            LootItem item = loot.catalog().find(backpack.slot(i));
            if (item != null) batch.draw(loot.itemFramed(item), ix, iy, iw, ih);
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
        lootLabels.dispose();
    }
}

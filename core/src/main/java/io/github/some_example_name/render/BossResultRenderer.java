package io.github.some_example_name.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import io.github.some_example_name.asset.BossAssets;
import io.github.some_example_name.inventory.LootItem;

/**
 * 보스 결과창(승리=보상/귀환, 실패=재도전/복귀). 지급 아이템은 실제 카탈로그 아이콘+등급
 * 테두리를 재사용하고 '본부 보관함 지급'을 명시한다. 저장 실패(REWARD_PENDING 유지) 중에는
 * 같은 보상으로 재시도 버튼을 보여주고 성공 처리로 앞서가지 않는다. 결과창이 떠 있는 동안
 * 월드 스킬 입력은 화면이 차단한다. 버튼은 콜백 1회만 — 더블클릭 중복은 호출 측 상태로 방어.
 */
public final class BossResultRenderer {

    public enum Mode { HIDDEN, REWARD_SAVING, REWARD_SAVE_FAILED, CLEARED, FAILED }

    public interface Listener {
        void onRetrySave();
        void onRetryRun();
        void onReturnHome();
    }

    private static final float REF_W = 1120f, REF_H = 720f;
    private static final float[] ITEM_FRAME = {512, 260, 96, 96};
    private static final float[] BTN_LEFT = {300, 560, 240, 60};
    private static final float[] BTN_RIGHT = {580, 560, 240, 60};

    private final BossAssets ui;
    private final BitmapFont numberFont;
    private final KoreanLabelCache labels = new KoreanLabelCache(22);
    private final KoreanLabelCache smallLabels = new KoreanLabelCache(15);
    private final SpriteBatch batch = new SpriteBatch();
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final OrthographicCamera camera = new OrthographicCamera();
    private final GlyphLayout glyph = new GlyphLayout();

    private Mode mode = Mode.HIDDEN;
    private LootItem rewardItem;
    private TextureRegion rewardIcon;
    private Listener listener;
    private boolean buttonLocked;
    private float scale, originX, originBottomY;

    public BossResultRenderer(BossAssets ui, BitmapFont numberFont) {
        this.ui = ui;
        this.numberFont = numberFont;
    }

    public Mode mode() { return mode; }
    public boolean visible() { return mode != Mode.HIDDEN; }

    public void showReward(LootItem item, TextureRegion framedIcon, boolean saveFailed,
                           Listener listener) {
        this.rewardItem = item;
        this.rewardIcon = framedIcon;
        this.listener = listener;
        this.mode = saveFailed ? Mode.REWARD_SAVE_FAILED : Mode.REWARD_SAVING;
        buttonLocked = false;
    }

    public void markCleared() { if (visible()) mode = Mode.CLEARED; }
    public void markSaveFailed() { if (visible()) mode = Mode.REWARD_SAVE_FAILED; }

    public void showFailed(Listener listener) {
        this.listener = listener;
        this.rewardItem = null;
        this.rewardIcon = null;
        this.mode = Mode.FAILED;
        buttonLocked = false;
    }

    public void hide() { mode = Mode.HIDDEN; }

    public void handleInput(boolean transitionLocked) {
        if (!visible() || listener == null || buttonLocked || transitionLocked
            || !Gdx.input.justTouched()) return;
        updateTransform();
        float ux = (Gdx.input.getX() - originX) / scale;
        float panelTopScreenY = Gdx.graphics.getHeight() - (originBottomY + REF_H * scale);
        float uy = (Gdx.input.getY() - panelTopScreenY) / scale;
        if (mode == Mode.FAILED) {
            if (hit(BTN_LEFT, ux, uy)) { buttonLocked = true; listener.onRetryRun(); }
            else if (hit(BTN_RIGHT, ux, uy)) { buttonLocked = true; listener.onReturnHome(); }
        } else if (mode == Mode.REWARD_SAVE_FAILED) {
            if (hit(BTN_LEFT, ux, uy)) { buttonLocked = false; listener.onRetrySave(); }
        } else if (mode == Mode.CLEARED) {
            if (hit(BTN_RIGHT, ux, uy)) { buttonLocked = true; listener.onReturnHome(); }
            else if (hit(BTN_LEFT, ux, uy)) hide(); // 닫고 열린 귀환문으로 직접 걸어 나가기
        }
    }

    /** 재도전으로 새 run이 시작되면 버튼 잠금을 푼다. */
    public void unlockButtons() { buttonLocked = false; }

    public void render() {
        if (!visible()) return;
        float sw = Gdx.graphics.getWidth(), sh = Gdx.graphics.getHeight();
        camera.setToOrtho(false, sw, sh);
        updateTransform();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.7f);
        shapes.rect(0f, 0f, sw, sh);
        shapes.end();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        draw(ui.stagePanel(), new float[]{0, 0, REF_W, REF_H});
        if (mode == Mode.FAILED) {
            text(labels.label("토벌 실패"), new float[]{0, 120, REF_W, 44}, true);
            text(smallLabels.label("쓰러졌습니다 — 보상과 단계 해금은 없습니다"),
                new float[]{0, 190, REF_W, 24}, true);
            drawButton(BTN_LEFT, "재도전", !buttonLocked);
            drawButton(BTN_RIGHT, "본부 복귀", !buttonLocked);
        } else {
            text(labels.label("토벌 성공 — 원심 배양체 C-01"),
                new float[]{0, 120, REF_W, 44}, true);
            draw(ui.rewardGlow(), new float[]{ITEM_FRAME[0] - 16, ITEM_FRAME[1] - 16,
                ITEM_FRAME[2] + 32, ITEM_FRAME[3] + 32});
            draw(ui.rewardFrame(), ITEM_FRAME);
            if (rewardIcon != null)
                draw(rewardIcon, new float[]{ITEM_FRAME[0] + 10, ITEM_FRAME[1] + 10,
                    ITEM_FRAME[2] - 20, ITEM_FRAME[3] - 20});
            if (rewardItem != null) {
                text(smallLabels.label("전설 등급 · " + rewardItem.name),
                    new float[]{0, 380, REF_W, 24}, true);
                drawNumberCentered(rewardItem.price + "W", REF_W / 2f, 414, 20f,
                    Color.valueOf("FFD700"));
                text(smallLabels.label("본부 보관함 지급"),
                    new float[]{0, 448, REF_W, 22}, true);
            }
            if (mode == Mode.REWARD_SAVING) {
                text(smallLabels.label("저장 중..."), new float[]{0, 490, REF_W, 22}, true);
            } else if (mode == Mode.REWARD_SAVE_FAILED) {
                batch.setColor(Color.SALMON);
                text(smallLabels.label("저장 실패 — 보상은 유지됩니다. 다시 저장하세요"),
                    new float[]{0, 490, REF_W, 22}, true);
                batch.setColor(Color.WHITE);
                drawButton(BTN_LEFT, "저장 재시도", true);
            } else { // CLEARED
                text(smallLabels.label("저장 완료 — 귀환문이 열렸습니다"),
                    new float[]{0, 490, REF_W, 22}, true);
                drawButton(BTN_LEFT, "닫기", true);
                drawButton(BTN_RIGHT, "본부 복귀", !buttonLocked);
            }
        }
        batch.end();
    }

    private void drawButton(float[] rect, String koreanLabel, boolean enabled) {
        batch.setColor(enabled ? Color.WHITE : new Color(0.45f, 0.45f, 0.45f, 0.7f));
        draw(ui.stageButton(), rect);
        batch.setColor(enabled ? Color.WHITE : Color.GRAY);
        text(labels.label(koreanLabel),
            new float[]{rect[0], rect[1] + 10, rect[2], rect[3] - 20}, true);
        batch.setColor(Color.WHITE);
    }

    private void updateTransform() {
        float sw = Gdx.graphics.getWidth(), sh = Gdx.graphics.getHeight();
        scale = Math.min(sw * 0.92f / REF_W, sh * 0.86f / REF_H);
        originX = (sw - REF_W * scale) / 2f;
        originBottomY = (sh - REF_H * scale) / 2f;
    }

    private float toX(float panelX) { return originX + panelX * scale; }
    private float toY(float panelTopY, float heightRef) {
        return originBottomY + (REF_H - panelTopY - heightRef) * scale;
    }

    private void draw(TextureRegion region, float[] rect) {
        batch.draw(region, toX(rect[0]), toY(rect[1], rect[3]), rect[2] * scale, rect[3] * scale);
    }

    private void text(TextureRegion label, float[] rect, boolean centerX) {
        float width = label.getRegionWidth(), height = label.getRegionHeight();
        float fit = Math.min(1f, Math.min(rect[2] / width, rect[3] / height));
        width *= fit;
        height *= fit;
        float x = centerX ? rect[0] + (rect[2] - width) / 2f : rect[0];
        float yTop = rect[1] + (rect[3] - height) / 2f;
        batch.draw(label, toX(x), toY(yTop, height), width * scale, height * scale);
    }

    private void drawNumberCentered(String value, float centerX, float panelTopY,
                                    float heightRef, Color color) {
        numberFont.getData().setScale(1f);
        glyph.setText(numberFont, value);
        numberFont.getData().setScale(heightRef * scale / glyph.height);
        glyph.setText(numberFont, value);
        numberFont.setColor(color);
        numberFont.draw(batch, glyph, toX(centerX) - glyph.width / 2f,
            toY(panelTopY, heightRef) + heightRef * scale);
        numberFont.setColor(Color.WHITE);
    }

    private boolean hit(float[] rect, float ux, float uy) {
        return ux >= rect[0] && ux <= rect[0] + rect[2]
            && uy >= rect[1] && uy <= rect[1] + rect[3];
    }

    public void dispose() {
        batch.dispose();
        shapes.dispose();
        labels.dispose();
        smallLabels.dispose();
    }
}

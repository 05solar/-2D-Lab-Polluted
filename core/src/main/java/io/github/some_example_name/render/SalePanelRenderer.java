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
import io.github.some_example_name.asset.HqSalesAssets;
import io.github.some_example_name.asset.LootAssets;
import io.github.some_example_name.inventory.LootItem;
import io.github.some_example_name.profile.HeadquartersEconomy;
import io.github.some_example_name.profile.SaleSelection;
import java.util.UUID;

/**
 * 본부 자판기 판매창. ui_layout.json(960×640, 좌상단 원점)의 배치를 상수로 옮겨
 * LibGDX 좌하단 원점으로 변환해 그린다(y = 640 - top - height). 패널은 통짜 텍스처를
 * 종횡비 유지 균일 배율로 화면에 맞추고, 클릭 좌표도 같은 변환으로 패널 좌표로 바꾼다.
 *
 * 판매 검증·차감·저장은 {@link HeadquartersEconomy}가 수행하고(합계 재계산 포함),
 * 이 클래스는 선택 상태({@link SaleSelection})와 그리기·입력만 담당한다.
 * 저장은 동기 처리라 성공 결과가 돌아온 뒤에만 목록·재화·합계를 갱신한다.
 */
public final class SalePanelRenderer {

    // ui_layout.json rects (좌상단 원점, 960×640 기준)
    private static final float REF_W = 960f, REF_H = 640f;
    private static final float[] TITLE = {48, 44, 510, 42};
    private static final float[] CURRENCY = {668, 48, 230, 36};
    private static final float[] LIST_HEADER = {48, 110, 540, 32};
    private static final float[] ITEM_LIST = {48, 150, 540, 350};
    private static final float[] SUMMARY = {624, 116, 274, 300};
    private static final float[] SALE_TOTAL = {624, 438, 274, 64};
    private static final float[] BTN_SELECT_ALL = {48, 542, 220, 56};
    private static final float[] BTN_SELL_SELECTED = {360, 542, 244, 56};
    private static final float[] BTN_SELL_ALL = {632, 542, 244, 56};
    private static final float[] BTN_CLOSE = {900, 49, 28, 28};
    private static final float[] BTN_SCROLL_UP = {594, 150, 28, 28};
    private static final float[] BTN_SCROLL_DOWN = {594, 472, 28, 28};
    private static final float ROW_HEIGHT = 70f;
    private static final int VISIBLE_ROWS = 5;

    private final HqSalesAssets ui;
    private final LootAssets loot;
    private final BitmapFont numberFont; // 숫자/ASCII — 공유 Mona BitmapFont(해제 금지)
    private final KoreanLabelCache labels = new KoreanLabelCache(20);
    private final KoreanLabelCache smallLabels = new KoreanLabelCache(15);
    private final SpriteBatch batch = new SpriteBatch();
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final OrthographicCamera camera = new OrthographicCamera();
    private final GlyphLayout glyph = new GlyphLayout();

    private final SaleSelection selection = new SaleSelection();
    private HeadquartersEconomy economy;
    private boolean open;
    private boolean confirmSellAll;
    private int scrollRow;
    private String message = "";
    private boolean messageIsError;
    // 패널 → 화면 변환(매 프레임 갱신)
    private float scale, originX, originBottomY;

    public SalePanelRenderer(HqSalesAssets ui, LootAssets loot, BitmapFont numberFont) {
        this.ui = ui;
        this.loot = loot;
        this.numberFont = numberFont;
    }

    public boolean isOpen() { return open; }

    public void open(HeadquartersEconomy economy) {
        this.economy = economy;
        selection.refresh(economy.heldCounts(), economy);
        selection.clearSelection();
        scrollRow = 0;
        confirmSellAll = false;
        message = "";
        messageIsError = false;
        open = true;
    }

    public void close() {
        open = false;
        confirmSellAll = false;
    }

    /** Esc 1회: 확인창이 열려 있으면 확인창만, 아니면 판매창을 닫는다(일시정지로 새지 않음). */
    public boolean onEscape() {
        if (!open) return false;
        if (confirmSellAll) confirmSellAll = false;
        else close();
        return true;
    }

    /** 판매창이 열린 동안 호출: 마우스 클릭을 처리한다(월드로 전달되지 않음). */
    public void handleInput() {
        if (!open || economy == null || !Gdx.input.justTouched()) return;
        updateTransform();
        // 클릭(화면 좌상단 원점) → 패널 좌표(좌상단 원점): 그리기와 같은 배율/오프셋을 쓴다.
        float ux = (Gdx.input.getX() - originX) / scale;
        float panelTopScreenY = Gdx.graphics.getHeight() - (originBottomY + REF_H * scale);
        float uy = (Gdx.input.getY() - panelTopScreenY) / scale;
        if (confirmSellAll) {
            handleConfirmClick(ux, uy);
            return;
        }
        if (hit(BTN_CLOSE, ux, uy)) { close(); return; }
        if (hit(BTN_SELECT_ALL, ux, uy)) { selection.toggleAll(); return; }
        if (hit(BTN_SELL_SELECTED, ux, uy)) { sellSelected(); return; }
        if (hit(BTN_SELL_ALL, ux, uy)) {
            if (economy.sellableSummary()[0] > 0) confirmSellAll = true;
            return;
        }
        if (hit(BTN_SCROLL_UP, ux, uy)) { scrollRow = Math.max(0, scrollRow - 1); return; }
        if (hit(BTN_SCROLL_DOWN, ux, uy)) {
            scrollRow = Math.max(0, Math.min(selection.rows().size() - VISIBLE_ROWS, scrollRow + 1));
            return;
        }
        // 목록 행: 수량 -/+ 버튼과 행 토글
        if (ux >= ITEM_LIST[0] && ux <= ITEM_LIST[0] + ITEM_LIST[2]
            && uy >= ITEM_LIST[1] && uy <= ITEM_LIST[1] + ITEM_LIST[3]) {
            int visibleIndex = (int) ((uy - ITEM_LIST[1]) / ROW_HEIGHT);
            int rowIndex = scrollRow + visibleIndex;
            if (rowIndex < 0 || rowIndex >= selection.rows().size()) return;
            float rowTop = ITEM_LIST[1] + visibleIndex * ROW_HEIGHT;
            if (hit(new float[]{462, rowTop + 36, 26, 26}, ux, uy)) selection.adjust(rowIndex, -1);
            else if (hit(new float[]{546, rowTop + 36, 26, 26}, ux, uy)) selection.adjust(rowIndex, +1);
            else selection.toggle(rowIndex);
        }
    }

    private void handleConfirmClick(float ux, float uy) {
        // 확인창 버튼: 패널 중앙 기준 [확인][취소]
        float[] yes = {REF_W / 2f - 230f, 384f, 210f, 54f};
        float[] no = {REF_W / 2f + 20f, 384f, 210f, 54f};
        if (hit(yes, ux, uy)) {
            confirmSellAll = false;
            sellAll();
        } else if (hit(no, ux, uy)) {
            confirmSellAll = false;
        }
    }

    /** 선택 판매 실행(버튼과 동일 경로 — 개발 캡처 훅에서도 사용). */
    public void sellSelected() {
        if (selection.selectedCount() == 0) return;
        HeadquartersEconomy.Result result =
            economy.sell(UUID.randomUUID().toString(), selection.selectedQuantities());
        applySaleResult(result);
    }

    /** 개발 캡처용: 선택 상태 접근(전체 선택 등). */
    public SaleSelection selection() { return selection; }

    private void sellAll() {
        HeadquartersEconomy.Result result = economy.sellAll(UUID.randomUUID().toString());
        applySaleResult(result);
    }

    /** 저장 성공(OK)일 때만 목록·선택·합계·재화를 함께 갱신한다. 실패는 상태 유지+재시도 안내. */
    private void applySaleResult(HeadquartersEconomy.Result result) {
        switch (result.status) {
            case OK:
                selection.refresh(economy.heldCounts(), economy);
                selection.clearSelection();
                scrollRow = Math.max(0, Math.min(scrollRow, selection.rows().size() - VISIBLE_ROWS));
                message = result.itemCount + "개 판매 완료  +" + result.totalWon + "원";
                messageIsError = false;
                break;
            case SAVE_FAILED:
                message = "저장 실패 — 판매 버튼으로 다시 시도하세요";
                messageIsError = true;
                break;
            case INVALID:
                selection.refresh(economy.heldCounts(), economy);
                message = "보관함이 변경되어 판매를 취소했습니다";
                messageIsError = true;
                break;
            case NOTHING_TO_DO:
                message = "판매할 물품이 없습니다";
                messageIsError = true;
                break;
            default:
                break;
        }
    }

    public void render() {
        if (!open || economy == null) return;
        float sw = Gdx.graphics.getWidth(), sh = Gdx.graphics.getHeight();
        camera.setToOrtho(false, sw, sh);
        updateTransform();
        // 뒤 월드 어둡게
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.62f);
        shapes.rect(0f, 0f, sw, sh);
        shapes.end();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.draw(ui.panel(), originX, originBottomY, REF_W * scale, REF_H * scale);
        drawTitleAndCurrency();
        drawListHeader();
        drawRows();
        drawScrollButtons();
        drawSummary();
        drawBottomButtons();
        text(labels.label("X"), BTN_CLOSE, true);
        if (confirmSellAll) drawConfirm();
        batch.end();
    }

    private void updateTransform() {
        float sw = Gdx.graphics.getWidth(), sh = Gdx.graphics.getHeight();
        scale = Math.min(sw * 0.92f / REF_W, sh * 0.90f / REF_H);
        originX = (sw - REF_W * scale) / 2f;
        originBottomY = (sh - REF_H * scale) / 2f;
    }

    private void drawTitleAndCurrency() {
        text(labels.label("판매 상점 — 본부 보관함"), TITLE, false);
        float[] coin = {CURRENCY[0], CURRENCY[1], 32, 32};
        draw(ui.coin32(), coin);
        drawNumber(String.valueOf(economy.currencyWon()) + "W",
            CURRENCY[0] + 40, CURRENCY[1] + 7, 22f, Color.WHITE);
    }

    private void drawListHeader() {
        text(smallLabels.label("이름 · 개당 가격 × 보유"), new float[]{114, LIST_HEADER[1], 220, 28}, false);
        text(smallLabels.label("등급"), new float[]{318, LIST_HEADER[1], 80, 28}, false);
        text(smallLabels.label("선택 수량"), new float[]{470, LIST_HEADER[1], 110, 28}, false);
    }

    private void drawRows() {
        java.util.List<SaleSelection.Row> rows = selection.rows();
        if (rows.isEmpty()) {
            text(labels.label("보관함이 비어 있습니다"),
                new float[]{ITEM_LIST[0], ITEM_LIST[1] + 140, ITEM_LIST[2], 40}, true);
            return;
        }
        for (int visible = 0; visible < VISIBLE_ROWS; visible++) {
            int index = scrollRow + visible;
            if (index >= rows.size()) break;
            SaleSelection.Row row = rows.get(index);
            float top = ITEM_LIST[1] + visible * ROW_HEIGHT;
            // 슬롯 + 아이템 아이콘(기존 등급 테두리 포함 이미지 재사용)
            float[] slotRect = {ITEM_LIST[0] + 6, top + 8, 52, 52};
            draw(ui.slot(), slotRect);
            LootItem item = loot.catalog().find(row.itemId);
            if (item != null)
                draw(loot.itemFramed(item), new float[]{slotRect[0] + 4, slotRect[1] + 4, 44, 44});
            // 청록 선택 테두리는 등급 테두리 바깥에 겹친다(등급색이 가려지지 않음).
            if (row.selected > 0)
                draw(ui.selectedBorder(), new float[]{slotRect[0] - 3, slotRect[1] - 3, 58, 58});
            String name = item != null ? item.name : row.itemId;
            text(smallLabels.label(name), new float[]{114, top + 10, 200, 24}, false);
            text(smallLabels.label(row.sellable ? rarityName(item) : "판매 불가"),
                new float[]{318, top + 10, 90, 24}, false);
            drawNumber(row.unitPrice + "W x" + row.owned, 114, top + 38, 17f,
                row.sellable ? Color.WHITE : Color.GRAY);
            if (row.sellable) {
                drawButtonBox(new float[]{462, top + 36, 26, 26}, "-", row.selected > 0);
                drawNumber(String.valueOf(row.selected), 503, top + 38, 18f,
                    row.selected > 0 ? Color.valueOf("59D9FF") : Color.LIGHT_GRAY);
                drawButtonBox(new float[]{546, top + 36, 26, 26}, "+", row.selected < row.owned);
            }
        }
    }

    private String rarityName(LootItem item) {
        if (item == null) return "-";
        if ("LEGENDARY".equals(item.rarity)) return "전설";
        if ("EPIC".equals(item.rarity)) return "희귀";
        return "일반";
    }

    private void drawScrollButtons() {
        boolean canUp = scrollRow > 0;
        boolean canDown = scrollRow + VISIBLE_ROWS < selection.rows().size();
        drawButtonBox(BTN_SCROLL_UP, "^", canUp);
        drawButtonBox(BTN_SCROLL_DOWN, "v", canDown);
    }

    private void drawSummary() {
        long[] all = economy.sellableSummary();
        text(smallLabels.label("보관 " + economy.storageView().size() + "개 · 판매 가능 " + all[0] + "개"),
            new float[]{SUMMARY[0], SUMMARY[1] + 8, SUMMARY[2], 24}, false);
        text(smallLabels.label("선택 " + selection.selectedCount() + "개"),
            new float[]{SUMMARY[0], SUMMARY[1] + 44, SUMMARY[2], 24}, false);
        if (!message.isEmpty()) {
            batch.setColor(messageIsError ? Color.SALMON : Color.valueOf("9CFFB0"));
            text(smallLabels.label(message),
                new float[]{SUMMARY[0], SUMMARY[1] + 96, SUMMARY[2], 48}, false);
            batch.setColor(Color.WHITE);
        }
        // 선택 합계
        float[] coin = {SALE_TOTAL[0], SALE_TOTAL[1] + 8, 44, 44};
        draw(ui.coin64(), coin);
        drawNumber(selection.selectedTotalWon() + "W",
            SALE_TOTAL[0] + 56, SALE_TOTAL[1] + 18, 24f, Color.valueOf("FFE28A"));
    }

    private void drawBottomButtons() {
        boolean anySellable = economy.sellableSummary()[0] > 0;
        drawButton(BTN_SELECT_ALL, "전체 선택/해제", anySellable);
        drawButton(BTN_SELL_SELECTED, "선택 판매", selection.selectedCount() > 0);
        drawButton(BTN_SELL_ALL, "전체 판매", anySellable);
    }

    private void drawConfirm() {
        long[] all = economy.sellableSummary();
        // 어둡게 + 중앙 확인 상자(버튼 텍스처를 배경으로 재사용)
        batch.setColor(0f, 0f, 0f, 0.55f);
        batch.draw(ui.panel(), originX, originBottomY, REF_W * scale, REF_H * scale);
        batch.setColor(Color.WHITE);
        float[] box = {REF_W / 2f - 260f, 250f, 520f, 200f};
        batch.setColor(0.16f, 0.16f, 0.2f, 0.96f);
        draw(ui.button(), box);
        batch.setColor(Color.WHITE);
        text(labels.label("전체 판매: " + all[0] + "개 · +" + all[1] + "원"),
            new float[]{box[0], 290f, box[2], 36}, true);
        drawButton(new float[]{REF_W / 2f - 230f, 384f, 210f, 54f}, "확인", true);
        drawButton(new float[]{REF_W / 2f + 20f, 384f, 210f, 54f}, "취소", true);
    }

    // ----- 그리기 보조(좌상단 원점 패널 좌표 → 화면) -----------------------------------

    private float toX(float panelX) { return originX + panelX * scale; }
    private float toY(float panelTopY, float heightRef) {
        return originBottomY + (REF_H - panelTopY - heightRef) * scale;
    }

    private void draw(TextureRegion region, float[] rect) {
        batch.draw(region, toX(rect[0]), toY(rect[1], rect[3]), rect[2] * scale, rect[3] * scale);
    }

    /** 한글 라벨을 rect 안에 맞춰(세로 중앙, 필요 시 축소) 그린다. */
    private void text(TextureRegion label, float[] rect, boolean centerX) {
        float width = label.getRegionWidth(), height = label.getRegionHeight();
        float fit = Math.min(1f, Math.min(rect[2] / width, rect[3] / height));
        width *= fit; height *= fit;
        float x = centerX ? rect[0] + (rect[2] - width) / 2f : rect[0];
        float yTop = rect[1] + (rect[3] - height) / 2f;
        batch.draw(label, toX(x), toY(yTop, height), width * scale, height * scale);
    }

    private void drawNumber(String value, float panelX, float panelTopY, float heightRef, Color color) {
        numberFont.getData().setScale(1f);
        glyph.setText(numberFont, value);
        float fontScale = heightRef * scale / glyph.height;
        numberFont.getData().setScale(fontScale);
        glyph.setText(numberFont, value);
        numberFont.setColor(color);
        numberFont.draw(batch, glyph, toX(panelX),
            toY(panelTopY, heightRef) + heightRef * scale);
        numberFont.setColor(Color.WHITE);
    }

    private void drawButton(float[] rect, String koreanLabel, boolean enabled) {
        batch.setColor(enabled ? Color.WHITE : new Color(0.45f, 0.45f, 0.45f, 0.7f));
        draw(ui.button(), rect);
        batch.setColor(enabled ? Color.WHITE : Color.GRAY);
        text(labels.label(koreanLabel),
            new float[]{rect[0], rect[1] + 8, rect[2], rect[3] - 16}, true);
        batch.setColor(Color.WHITE);
    }

    private void drawButtonBox(float[] rect, String asciiLabel, boolean enabled) {
        batch.setColor(enabled ? Color.WHITE : new Color(0.45f, 0.45f, 0.45f, 0.7f));
        draw(ui.button(), rect);
        batch.setColor(Color.WHITE);
        drawNumber(asciiLabel, rect[0] + rect[2] * 0.32f, rect[1] + 3, rect[3] - 8,
            enabled ? Color.WHITE : Color.GRAY);
    }

    private boolean hit(float[] rect, float ux, float uyTop) {
        return ux >= rect[0] && ux <= rect[0] + rect[2]
            && uyTop >= rect[1] && uyTop <= rect[1] + rect[3];
    }

    public void dispose() {
        batch.dispose();
        shapes.dispose();
        labels.dispose();
        smallLabels.dispose();
    }
}

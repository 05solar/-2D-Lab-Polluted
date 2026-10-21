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
import io.github.some_example_name.asset.UpgradeAssets;
import io.github.some_example_name.profile.HeadquartersEconomy;
import io.github.some_example_name.profile.UpgradeCatalog;
import java.util.Set;
import java.util.UUID;

/**
 * 영구 강화 트리 패널(1120×720, ui_layout.json 좌상단 원점 → y = 720 - top - height 변환).
 * 노드 상태(잠김/구매 가능/재화 부족/구매 완료)는 모델에서 계산하고, 연결선은 JSON 선행
 * 관계에서 직각 우회 경로로 생성한다(노드 아래 이름·비용을 피해 옆 레일로 우회).
 * 구매 확정은 상세 패널의 구매 버튼으로만 하며, 검증·차감·저장은 HeadquartersEconomy가
 * 판매와 같은 거래 경계로 처리한다. 공용 버튼·재화 아이콘은 판매 팩 텍스처를 공유한다.
 */
public final class UpgradeTreeRenderer {

    /** 상세 패널의 현재/구매 후 효과 표시에 쓰는 실제 파생 능력치 공급자. */
    public interface StatsProvider {
        int maxHp();
        int attackDamage();
        int healAmount();
        int activeSlots();
        boolean healUnlocked();
        /** 스킬 전용 피해(기본 + SKILL_DAMAGE_ADD 강화 — 충격파/전기 투척). */
        int skillDamage(String skillId);
        /** 보호막 흡수량(기본 + SHIELD_ABSORB_ADD 강화). */
        int shieldAbsorb();
        /** 해당 스킬이 해금되어 있는지(미리보기 표기용). */
        boolean skillUnlocked(String skillId);
    }

    private static final float REF_W = 1120f, REF_H = 720f;
    private static final float[] TITLE = {48, 44, 550, 42};
    private static final float[] WALLET = {840, 48, 220, 36};
    private static final float[] DETAILS = {800, 125, 260, 400};
    private static final float[] BTN_PURCHASE = {810, 565, 240, 60};
    private static final float[] BTN_CLOSE = {1060, 48, 28, 28};
    private static final float NODE_HALF = 48f, ICON = 44f, LINK_THICK = 12f;

    private final UpgradeAssets ui;
    private final HqSalesAssets shared;
    private final BitmapFont numberFont;
    private final KoreanLabelCache labels = new KoreanLabelCache(20);
    private final KoreanLabelCache smallLabels = new KoreanLabelCache(15);
    private final SpriteBatch batch = new SpriteBatch();
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final OrthographicCamera camera = new OrthographicCamera();
    private final GlyphLayout glyph = new GlyphLayout();

    private HeadquartersEconomy economy;
    private UpgradeCatalog catalog;
    private StatsProvider stats;
    private Runnable onPurchased;
    private int baseSlots;
    private boolean open;
    private String selectedNodeId;
    private String message = "";
    private boolean messageIsError;
    private float scale, originX, originBottomY;

    public UpgradeTreeRenderer(UpgradeAssets ui, HqSalesAssets shared, BitmapFont numberFont) {
        this.ui = ui;
        this.shared = shared;
        this.numberFont = numberFont;
    }

    public boolean isOpen() { return open; }

    public void open(HeadquartersEconomy economy, UpgradeCatalog catalog, int baseSlots,
                     StatsProvider stats, Runnable onPurchased) {
        this.economy = economy;
        this.catalog = catalog;
        this.baseSlots = baseSlots;
        this.stats = stats;
        this.onPurchased = onPurchased;
        selectedNodeId = null;
        message = "";
        messageIsError = false;
        open = true;
    }

    public void close() { open = false; }

    /** Esc 1회로 패널만 닫는다(일시정지로 새지 않음). */
    public boolean onEscape() {
        if (!open) return false;
        close();
        return true;
    }

    // ----- 노드 상태 ---------------------------------------------------------------

    private static final int LOCKED = 0, AVAILABLE = 1, PURCHASED = 2, INSUFFICIENT = 3;

    private int nodeState(UpgradeCatalog.Node node, Set<String> purchased) {
        if (purchased.contains(node.nodeId)) return PURCHASED;
        if (!catalog.prerequisitesMet(node.nodeId, purchased)) return LOCKED;
        if (node.effectType == UpgradeCatalog.EffectType.SKILL_HEAL_ADD && !stats.healUnlocked())
            return LOCKED;
        return node.costWon <= economy.currencyWon() ? AVAILABLE : INSUFFICIENT;
    }

    private String lockReason(UpgradeCatalog.Node node, Set<String> purchased) {
        for (String prereq : node.prerequisites)
            if (!purchased.contains(prereq)) {
                UpgradeCatalog.Node need = catalog.node(prereq);
                return "선행 필요: " + (need == null ? prereq : need.displayName);
            }
        return "잠김";
    }

    // ----- 입력 ---------------------------------------------------------------------

    public void handleInput() {
        if (!open || economy == null || !Gdx.input.justTouched()) return;
        updateTransform();
        float ux = (Gdx.input.getX() - originX) / scale;
        float panelTopScreenY = Gdx.graphics.getHeight() - (originBottomY + REF_H * scale);
        float uy = (Gdx.input.getY() - panelTopScreenY) / scale;
        if (hit(BTN_CLOSE, ux, uy)) { close(); return; }
        if (hit(BTN_PURCHASE, ux, uy)) { purchaseSelected(); return; }
        // 노드 클릭 = 선택만(즉시 구매 없음). 가장 가까운 프레임 히트 1개.
        for (UpgradeCatalog.Node node : catalog.nodes()) {
            if (ux >= node.uiX - NODE_HALF && ux <= node.uiX + NODE_HALF
                && uy >= node.uiY - NODE_HALF && uy <= node.uiY + NODE_HALF) {
                selectedNodeId = node.nodeId;
                message = "";
                return;
            }
        }
    }

    /** 개발 캡처용: 노드 선택. */
    public void selectNode(String nodeId) { selectedNodeId = nodeId; }

    /** 상세 패널의 구매 버튼: 모델이 선행·재화·중복·효과 적용 가능성을 다시 검증한다. */
    public void purchaseSelected() {
        if (selectedNodeId == null) return;
        Set<String> purchased = economy.profile().purchasedNodeIds();
        UpgradeCatalog.Node node = catalog.node(selectedNodeId);
        if (node == null || nodeState(node, purchased) != AVAILABLE) return;
        HeadquartersEconomy.Result result = economy.purchaseUpgrade(
            UUID.randomUUID().toString(), catalog, selectedNodeId, baseSlots);
        switch (result.status) {
            case OK:
                if (onPurchased != null) onPurchased.run(); // 파생 능력치 즉시 반영(멱등)
                message = node.displayName + " 구매 완료  -" + result.totalWon + "원";
                messageIsError = false;
                break;
            case SAVE_FAILED:
                message = "저장 실패 — 다시 시도하세요";
                messageIsError = true;
                break;
            default:
                message = "구매할 수 없습니다(조건 변경)";
                messageIsError = true;
                break;
        }
    }

    // ----- 그리기 -------------------------------------------------------------------

    public void render() {
        if (!open || economy == null) return;
        float sw = Gdx.graphics.getWidth(), sh = Gdx.graphics.getHeight();
        camera.setToOrtho(false, sw, sh);
        updateTransform();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.62f);
        shapes.rect(0f, 0f, sw, sh);
        shapes.end();

        Set<String> purchased = economy.profile().purchasedNodeIds();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.draw(ui.panel(), originX, originBottomY, REF_W * scale, REF_H * scale);
        text(labels.label("영구 강화 — 본부 강화대"), TITLE, false);
        draw(shared.coin32(), new float[]{WALLET[0], WALLET[1], 32, 32});
        drawNumber(economy.currencyWon() + "W", WALLET[0] + 40, WALLET[1] + 7, 22f, Color.WHITE);
        text(labels.label("X"), BTN_CLOSE, true);
        // 가지 제목(런타임 글자)
        headerText("체력", 150);
        headerText("공격", 370);
        headerText("스킬", 590);
        drawLinks(purchased);
        drawNodes(purchased);
        drawDetails(purchased);
        batch.end();
    }

    private void headerText(String name, float centerX) {
        text(labels.label(name), new float[]{centerX - 60, 100, 120, 30}, true);
    }

    /** 연결선: 선행 → 노드. 같은 열은 옆 레일로 우회(이름·비용 텍스트 회피), 끝은 프레임 가장자리. */
    private void drawLinks(Set<String> purchased) {
        for (UpgradeCatalog.Node node : catalog.nodes()) {
            for (String prereqId : node.prerequisites) {
                UpgradeCatalog.Node prereq = catalog.node(prereqId);
                if (prereq == null) continue;
                int state = purchased.contains(node.nodeId) ? 2
                    : purchased.contains(prereqId) ? 1 : 0;
                boolean rightSide = prereq.uiX >= 500 || node.uiX > prereq.uiX;
                float rail = rightSide
                    ? Math.max(prereq.uiX, node.uiX) + 78f
                    : Math.min(prereq.uiX, node.uiX) - 78f;
                float fromEdge = prereq.uiX + (rightSide ? NODE_HALF : -NODE_HALF);
                float toEdge = node.uiX + (rightSide ? NODE_HALF : -NODE_HALF);
                hLine(fromEdge, rail, prereq.uiY, state);
                vLine(rail, prereq.uiY, node.uiY, state);
                hLine(rail, toEdge, node.uiY, state);
            }
        }
    }

    private void hLine(float x1, float x2, float yCenter, int state) {
        float left = Math.min(x1, x2), width = Math.abs(x2 - x1);
        if (width < 1f) return;
        draw(ui.linkH(state), new float[]{left, yCenter - LINK_THICK / 2f, width, LINK_THICK});
    }

    private void vLine(float xCenter, float y1, float y2, int state) {
        float top = Math.min(y1, y2), height = Math.abs(y2 - y1);
        if (height < 1f) return;
        draw(ui.linkV(state), new float[]{xCenter - LINK_THICK / 2f, top, LINK_THICK, height});
    }

    private void drawNodes(Set<String> purchased) {
        for (UpgradeCatalog.Node node : catalog.nodes()) {
            int state = nodeState(node, purchased);
            float[] frame = {node.uiX - NODE_HALF, node.uiY - NODE_HALF, NODE_HALF * 2, NODE_HALF * 2};
            // 재화 부족: 구매 가능 프레임(청록)을 어둡게 tint — 잠김과 시각적으로 구분.
            if (state == INSUFFICIENT) batch.setColor(0.55f, 0.6f, 0.62f, 1f);
            batch.draw(ui.nodeFrame(state == PURCHASED ? 2 : state == LOCKED ? 0 : 1),
                toX(frame[0]), toY(frame[1], frame[3]), frame[2] * scale, frame[3] * scale);
            batch.setColor(Color.WHITE);
            if (state == LOCKED) batch.setColor(0.55f, 0.55f, 0.55f, 1f);
            draw(ui.icon(node.iconId),
                new float[]{node.uiX - ICON / 2f, node.uiY - ICON / 2f, ICON, ICON});
            batch.setColor(Color.WHITE);
            if (node.nodeId.equals(selectedNodeId))
                draw(shared.selectedBorder(), new float[]{node.uiX - NODE_HALF - 4,
                    node.uiY - NODE_HALF - 4, NODE_HALF * 2 + 8, NODE_HALF * 2 + 8});
            // 노드 아래 이름·비용(런타임 글자)
            text(smallLabels.label(node.displayName),
                new float[]{node.uiX - 70, node.uiY + NODE_HALF + 4, 140, 20}, true);
            if (state == PURCHASED) {
                batch.setColor(Color.valueOf("FFD700"));
                text(smallLabels.label("완료"),
                    new float[]{node.uiX - 40, node.uiY + NODE_HALF + 24, 80, 18}, true);
                batch.setColor(Color.WHITE);
            } else {
                drawNumberCentered(node.costWon + "W", node.uiX, node.uiY + NODE_HALF + 28,
                    16f, state == INSUFFICIENT ? Color.SALMON : Color.WHITE);
            }
        }
    }

    private void drawDetails(Set<String> purchased) {
        if (selectedNodeId == null) {
            text(smallLabels.label("노드를 선택하세요"),
                new float[]{DETAILS[0], DETAILS[1] + 20, DETAILS[2], 24}, false);
            drawButton(BTN_PURCHASE, "구매", false);
            return;
        }
        UpgradeCatalog.Node node = catalog.node(selectedNodeId);
        int state = nodeState(node, purchased);
        float y = DETAILS[1];
        text(labels.label(node.displayName), new float[]{DETAILS[0], y, DETAILS[2], 26}, false);
        y += 36;
        for (String line : wrap(node.description, 18)) {
            text(smallLabels.label(line), new float[]{DETAILS[0], y, DETAILS[2], 20}, false);
            y += 24;
        }
        y += 8;
        text(smallLabels.label(effectPreview(node, state)),
            new float[]{DETAILS[0], y, DETAILS[2], 22}, false);
        y += 30;
        draw(shared.coin32(), new float[]{DETAILS[0], y, 24, 24});
        drawNumber(node.costWon + "W", DETAILS[0] + 30, y + 2, 18f,
            state == INSUFFICIENT ? Color.SALMON : Color.WHITE);
        y += 34;
        if (state == LOCKED)
            text(smallLabels.label(lockReason(node, purchased)),
                new float[]{DETAILS[0], y, DETAILS[2], 20}, false);
        else if (state == INSUFFICIENT)
            text(smallLabels.label("재화 부족: " + (node.costWon - economy.currencyWon()) + "원 더 필요"),
                new float[]{DETAILS[0], y, DETAILS[2], 20}, false);
        else if (state == PURCHASED)
            text(smallLabels.label("구매 완료"),
                new float[]{DETAILS[0], y, DETAILS[2], 20}, false);
        if (!message.isEmpty()) {
            batch.setColor(messageIsError ? Color.SALMON : Color.valueOf("9CFFB0"));
            text(smallLabels.label(message),
                new float[]{DETAILS[0], DETAILS[1] + DETAILS[3] - 36, DETAILS[2], 32}, false);
            batch.setColor(Color.WHITE);
        }
        drawButton(BTN_PURCHASE, "구매", state == AVAILABLE);
    }

    /** 현재 → 구매 후 효과(공용 파생 능력치 기준). */
    private String effectPreview(UpgradeCatalog.Node node, int state) {
        boolean purchased = state == PURCHASED;
        switch (node.effectType) {
            case MAX_HP_ADD:
                return "최대 HP " + stats.maxHp()
                    + (purchased ? " (적용됨)" : " → " + (stats.maxHp() + node.effectValue));
            case BASE_ATTACK_ADD:
                return "공격력 " + stats.attackDamage()
                    + (purchased ? " (적용됨)" : " → " + (stats.attackDamage() + node.effectValue));
            case SKILL_UNLOCK:
                return purchased ? "스킬 보유 — 스킬창에서 장착"
                    : "미보유 → 해금(스킬창에서 빈 슬롯 장착)";
            case SKILL_HEAL_ADD:
                return "회복량 " + stats.healAmount()
                    + (purchased ? " (적용됨)" : " → " + (stats.healAmount() + node.effectValue));
            case SKILL_DAMAGE_ADD:
                return "피해 " + stats.skillDamage(node.skillId)
                    + (purchased ? " (적용됨)"
                        : " → " + (stats.skillDamage(node.skillId) + node.effectValue));
            case SHIELD_ABSORB_ADD:
                return "흡수량 " + stats.shieldAbsorb()
                    + (purchased ? " (적용됨)" : " → " + (stats.shieldAbsorb() + node.effectValue));
            case ACTIVE_SLOT_ADD:
                return "활성 슬롯 " + stats.activeSlots()
                    + (purchased ? " (적용됨)" : " → " + (stats.activeSlots() + node.effectValue));
            default:
                return "";
        }
    }

    private static java.util.List<String> wrap(String value, int charsPerLine) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        String remaining = value == null ? "" : value;
        while (remaining.length() > charsPerLine) {
            int cut = remaining.lastIndexOf(' ', charsPerLine);
            if (cut <= 0) cut = charsPerLine;
            lines.add(remaining.substring(0, cut).trim());
            remaining = remaining.substring(cut).trim();
            if (lines.size() == 3) break; // 상세 패널 공간 제한
        }
        if (!remaining.isEmpty() && lines.size() < 4) lines.add(remaining);
        return lines;
    }

    // ----- 좌표/그리기 보조(판매창과 동일 방식) ------------------------------------------

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
        width *= fit; height *= fit;
        float x = centerX ? rect[0] + (rect[2] - width) / 2f : rect[0];
        float yTop = rect[1] + (rect[3] - height) / 2f;
        batch.draw(label, toX(x), toY(yTop, height), width * scale, height * scale);
    }

    private void drawNumber(String value, float panelX, float panelTopY, float heightRef, Color color) {
        numberFont.getData().setScale(1f);
        glyph.setText(numberFont, value);
        numberFont.getData().setScale(heightRef * scale / glyph.height);
        glyph.setText(numberFont, value);
        numberFont.setColor(color);
        numberFont.draw(batch, glyph, toX(panelX), toY(panelTopY, heightRef) + heightRef * scale);
        numberFont.setColor(Color.WHITE);
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

    private void drawButton(float[] rect, String koreanLabel, boolean enabled) {
        batch.setColor(enabled ? Color.WHITE : new Color(0.45f, 0.45f, 0.45f, 0.7f));
        draw(shared.button(), rect);
        batch.setColor(enabled ? Color.WHITE : Color.GRAY);
        text(labels.label(koreanLabel),
            new float[]{rect[0], rect[1] + 8, rect[2], rect[3] - 16}, true);
        batch.setColor(Color.WHITE);
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

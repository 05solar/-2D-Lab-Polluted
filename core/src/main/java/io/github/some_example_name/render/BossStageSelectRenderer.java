package io.github.some_example_name.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import io.github.some_example_name.asset.BossAssets;
import io.github.some_example_name.asset.BossNineAssets;
import io.github.some_example_name.boss.BossStageCatalog;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 보스 단계 선택 패널(1120×720 — 본부 메인 컴퓨터 Space로 연다). 3×3 카드에는 단계마다
 * 서로 다른 보스 실루엣이 잠김 상태에서도 보이고(contain-fit — 잘림 없음), 선택 테두리는
 * 잠김/토벌 표시와 겹쳐 그려진다. 상세에는 컬러 초상·패턴 요약·해금 조건을 보여준다.
 * 마우스 클릭과 방향키+Enter 모두 지원, Esc는 패널만 닫는다. 입장 버튼은 "진행 해금+구현"
 * 단계에서만 활성화되고 입장 확정 후 전환 종료까지 잠가 더블클릭 중복 진입을 막는다.
 */
public final class BossStageSelectRenderer {

    public interface Listener { void onEnterStage(String stageId); }

    private static final float REF_W = 1120f, REF_H = 720f;
    private static final float[] TITLE = {48, 42, 700, 45};
    private static final float[] BTN_CLOSE = {1060, 48, 28, 28};
    private static final float[] DETAILS = {620, 120, 440, 400};
    private static final float[] BTN_ENTER = {790, 580, 240, 60};
    private static final float GRID_X = 60f, GRID_Y = 120f, GRID_STEP = 178f, CARD = 144f;
    private static final int COLUMNS = 3;
    /** 실루엣 contain-fit 박스(카드 안쪽 여백 — 어떤 비율이어도 잘리지 않는다). */
    private static final float PORTRAIT_PAD = 14f;

    /** 표시용 패턴 요약(수치 아님 — 수치는 전부 데이터에서). */
    private static final Map<String, String> PATTERN_LABELS = new HashMap<>();
    static {
        PATTERN_LABELS.put("waste_throw", "오염물 투척");
        PATTERN_LABELS.put("waste_charge", "돌진");
        PATTERN_LABELS.put("cold_cone", "냉기 부채꼴");
        PATTERN_LABELS.put("ice_zone", "빙결 장판");
        PATTERN_LABELS.put("acid_throw", "산성 투척");
        PATTERN_LABELS.put("spore_zone", "포자 지대");
        PATTERN_LABELS.put("aimed_shot", "조준 사격");
        PATTERN_LABELS.put("rotating_laser", "회전 레이저");
        PATTERN_LABELS.put("psychic_wave", "원형 충격파");
        PATTERN_LABELS.put("delayed_blast", "지연 폭발");
        PATTERN_LABELS.put("enrage_transition", "분노 전환(HP 50%)");
        PATTERN_LABELS.put("fusion_combo", "복합 공격");
        PATTERN_LABELS.put("chain_charge", "연속 돌진");
        PATTERN_LABELS.put("ice_shards", "얼음 파편");
        PATTERN_LABELS.put("root_bind", "뿌리 구속");
        PATTERN_LABELS.put("toxic_zone", "독성 구역");
        PATTERN_LABELS.put("moving_mortar", "이동 포격");
        PATTERN_LABELS.put("toxic_leak", "누출 흔적");
    }

    private final BossAssets ui;
    private final BossNineAssets nine;
    private final BitmapFont numberFont;
    private final KoreanLabelCache labels = new KoreanLabelCache(20);
    private final KoreanLabelCache smallLabels = new KoreanLabelCache(15);
    private final SpriteBatch batch = new SpriteBatch();
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final OrthographicCamera camera = new OrthographicCamera();
    private final GlyphLayout glyph = new GlyphLayout();

    private BossStageCatalog catalog;
    private Set<String> unlocked, cleared;
    private Listener listener;
    private boolean open;
    private boolean entryLocked; // 입장 확정 후 전환 종료까지 재클릭 차단
    private String selectedStageId;
    private float scale, originX, originBottomY;

    public BossStageSelectRenderer(BossAssets ui, BossNineAssets nine, BitmapFont numberFont) {
        this.ui = ui;
        this.nine = nine;
        this.numberFont = numberFont;
    }

    public boolean isOpen() { return open; }

    public void open(BossStageCatalog catalog, Set<String> unlockedStages,
                     Set<String> clearedStages, Listener listener) {
        this.catalog = catalog;
        this.unlocked = unlockedStages;
        this.cleared = clearedStages;
        this.listener = listener;
        selectedStageId = "1-1";
        entryLocked = false;
        open = true;
    }

    public void close() { open = false; }

    public boolean onEscape() {
        if (!open) return false;
        close();
        return true;
    }

    /** 개발 캡처용. */
    public void selectStage(String stageId) { selectedStageId = stageId; }
    public void confirmEntry() { tryEnter(); }

    public void handleInput(boolean transitionLocked) {
        if (!open || catalog == null || transitionLocked || entryLocked) return;
        // 키보드: 방향키로 3×3 이동, Enter 입장(잠김 단계는 입장만 차단되고 선택은 가능).
        List<BossStageCatalog.Stage> stages = catalog.stages();
        int selected = selectedIndex(stages);
        int move = 0;
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) move = -1;
        else if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) move = 1;
        else if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) move = -COLUMNS; // 위 행 = 작은 index
        else if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) move = COLUMNS;
        if (move != 0) {
            int next = selected + move;
            if (next >= 0 && next < stages.size()
                && !(Math.abs(move) == 1 && next / COLUMNS != selected / COLUMNS))
                selectedStageId = stages.get(next).stageId;
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) { tryEnter(); return; }
        if (!Gdx.input.justTouched()) return;
        updateTransform();
        float ux = (Gdx.input.getX() - originX) / scale;
        float panelTopScreenY = Gdx.graphics.getHeight() - (originBottomY + REF_H * scale);
        float uy = (Gdx.input.getY() - panelTopScreenY) / scale;
        if (hit(BTN_CLOSE, ux, uy)) { close(); return; }
        if (hit(BTN_ENTER, ux, uy)) { tryEnter(); return; }
        int index = 0;
        for (BossStageCatalog.Stage stage : stages) {
            float[] card = cardRect(index++);
            if (hit(card, ux, uy)) { selectedStageId = stage.stageId; return; }
        }
    }

    private int selectedIndex(List<BossStageCatalog.Stage> stages) {
        for (int i = 0; i < stages.size(); i++)
            if (stages.get(i).stageId.equals(selectedStageId)) return i;
        return 0;
    }

    private void tryEnter() {
        if (selectedStageId == null || entryLocked
            || !catalog.enterable(selectedStageId, unlocked)) return; // 잠김 입장 차단
        entryLocked = true; // 더블클릭으로 같은 시도 2개 금지 — 전환 1회만
        if (listener != null) listener.onEnterStage(selectedStageId);
        close();
    }

    /** 3×3: index 0(1-1)이 왼쪽 위부터 — 패널 top-left 기준 좌표라 행 = index/3. */
    private float[] cardRect(int index) {
        float x = GRID_X + (index % COLUMNS) * GRID_STEP;
        float y = GRID_Y + (index / COLUMNS) * GRID_STEP;
        return new float[]{x, y, CARD, CARD};
    }

    public void render() {
        if (!open || catalog == null) return;
        float sw = Gdx.graphics.getWidth(), sh = Gdx.graphics.getHeight();
        camera.setToOrtho(false, sw, sh);
        updateTransform();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.62f);
        shapes.rect(0f, 0f, sw, sh);
        shapes.end();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        draw(ui.stagePanel(), new float[]{0, 0, REF_W, REF_H});
        text(labels.label("보스 단계 선택 — 메인 컴퓨터"), TITLE, false);
        text(labels.label("X"), BTN_CLOSE, true);
        int index = 0;
        for (BossStageCatalog.Stage stage : catalog.stages())
            drawCard(stage, cardRect(index++));
        drawDetails();
        batch.end();
    }

    private void drawCard(BossStageCatalog.Stage stage, float[] card) {
        boolean progress = catalog.progressUnlocked(stage.stageId, unlocked);
        boolean enterable = catalog.enterable(stage.stageId, unlocked);
        boolean isCleared = cleared.contains(stage.stageId);
        draw(nine.card(!progress, isCleared), card);
        // 선택 프레임은 실루엣보다 먼저(배경 강조) — 실루엣/배지를 가리지 않는다.
        if (stage.stageId.equals(selectedStageId))
            draw(nine.cardSelected(), card);
        // 실루엣은 잠김 상태에서도 보인다 — contain-fit(잘림 금지), 잠김은 어둡게.
        TextureRegion silhouette = stage.portraitSilhouette != null
            ? nine.portrait(stage.portraitSilhouette) : ui.emblem();
        if (!progress) batch.setColor(0.45f, 0.45f, 0.5f, 1f);
        else if (!enterable) batch.setColor(0.6f, 0.6f, 0.65f, 1f); // 해금·준비 중
        drawContained(silhouette, card[0] + PORTRAIT_PAD, card[1] + PORTRAIT_PAD,
            CARD - PORTRAIT_PAD * 2f, CARD - PORTRAIT_PAD * 2f);
        batch.setColor(Color.WHITE);
        if (!progress)
            draw(nine.lockBadge(), new float[]{card[0] + CARD - 30, card[1] + CARD - 30, 24, 24});
        if (isCleared)
            draw(nine.clearBadge(), new float[]{card[0] + CARD - 30, card[1] + 6, 24, 24});
        // 단계 ID 라벨(Mona 숫자 폰트는 ASCII 전용 — "1-보스"는 라벨 캐시).
        if (isAscii(stage.stageId)) {
            drawNumberCentered(stage.stageId, card[0] + CARD / 2f, card[1] + CARD + 4, 15f,
                progress ? Color.WHITE : Color.GRAY);
        } else {
            if (!progress) batch.setColor(Color.GRAY);
            text(smallLabels.label(stage.stageId),
                new float[]{card[0] - 10, card[1] + CARD + 4, CARD + 20, 16}, true);
            batch.setColor(Color.WHITE);
        }
    }

    /** contain-fit: 비율 유지 축소(확대 없음) 후 중앙 배치 — 어느 방향으로도 잘리지 않는다. */
    private void drawContained(TextureRegion region, float x, float y, float w, float h) {
        float rw = region.getRegionWidth(), rh = region.getRegionHeight();
        float fit = Math.min(w / rw, h / rh);
        float dw = rw * fit, dh = rh * fit;
        draw(region, new float[]{x + (w - dw) / 2f, y + (h - dh) / 2f, dw, dh});
    }

    private void drawDetails() {
        BossStageCatalog.Stage stage = catalog.stage(selectedStageId);
        if (stage == null) return;
        boolean progress = catalog.progressUnlocked(stage.stageId, unlocked);
        boolean enterable = catalog.enterable(stage.stageId, unlocked);
        float y = DETAILS[1];
        // 컬러 초상(잠김은 실루엣 유지 — 정체 비공개).
        String portraitPath = progress ? stage.portraitColor : stage.portraitSilhouette;
        if (portraitPath != null) {
            if (!progress) batch.setColor(0.5f, 0.5f, 0.55f, 1f);
            drawContained(nine.portrait(portraitPath), DETAILS[0], y, 96f, 96f);
            batch.setColor(Color.WHITE);
        }
        drawNumberCentered("STAGE " + (isAscii(stage.stageId) ? stage.stageId : ""),
            DETAILS[0] + 220, y + 8, 20f, Color.WHITE);
        String title = progress ? stage.displayName : "???";
        text(labels.label(title), new float[]{DETAILS[0] + 110, y + 40, DETAILS[2] - 110, 26}, false);
        y += 106;
        String status = !progress ? "잠김 — 이전 단계를 토벌하세요"
            : enterable ? (cleared.contains(stage.stageId) ? "토벌 완료 — 재도전 가능(새 시도)" : "입장 가능")
            : "해금됨 · 준비 중";
        text(smallLabels.label(status), new float[]{DETAILS[0], y, DETAILS[2], 22}, false);
        y += 30;
        if (progress && stage.patternIds.length > 0) {
            StringBuilder summary = new StringBuilder("패턴: ");
            int shown = 0;
            for (String id : stage.patternIds) {
                if (shown >= 3) break;
                String label = PATTERN_LABELS.get(id);
                if (label == null) continue;
                if (shown++ > 0) summary.append(" · ");
                summary.append(label);
            }
            if (shown > 0) {
                text(smallLabels.label(summary.toString()),
                    new float[]{DETAILS[0], y, DETAILS[2], 20}, false);
                y += 26;
            }
        }
        if (enterable) {
            text(smallLabels.label("보상: 전설 등급 물품 1개(본부 보관함 지급)"),
                new float[]{DETAILS[0], y, DETAILS[2], 20}, false);
            y += 26;
            text(smallLabels.label("유효 토벌(새 시도)마다 보상 1개"),
                new float[]{DETAILS[0], y, DETAILS[2], 20}, false);
        }
        boolean buttonEnabled = enterable && !entryLocked;
        batch.setColor(buttonEnabled ? Color.WHITE : new Color(0.45f, 0.45f, 0.45f, 0.7f));
        draw(ui.stageButton(), BTN_ENTER);
        batch.setColor(buttonEnabled ? Color.WHITE : Color.GRAY);
        text(labels.label(entryLocked ? "입장 중..." : "입장"),
            new float[]{BTN_ENTER[0], BTN_ENTER[1] + 10, BTN_ENTER[2], BTN_ENTER[3] - 20}, true);
        batch.setColor(Color.WHITE);
    }

    // ----- 공용 변환(기존 패널과 동일) --------------------------------------------------

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
        if (value.isEmpty()) return;
        numberFont.getData().setScale(1f);
        glyph.setText(numberFont, value);
        numberFont.getData().setScale(heightRef * scale / glyph.height);
        glyph.setText(numberFont, value);
        numberFont.setColor(color);
        numberFont.draw(batch, glyph, toX(centerX) - glyph.width / 2f,
            toY(panelTopY, heightRef) + heightRef * scale);
        numberFont.setColor(Color.WHITE);
    }

    private static boolean isAscii(String value) {
        for (int i = 0; i < value.length(); i++) if (value.charAt(i) > 127) return false;
        return true;
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

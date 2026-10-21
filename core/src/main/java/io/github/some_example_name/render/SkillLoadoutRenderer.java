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
import io.github.some_example_name.profile.SkillLoadout;
import io.github.some_example_name.asset.SkillAssets;
import java.util.Set;

/**
 * 스킬 목록/장착/키 등록 패널(1120×720, ui_layout.json 좌표 — 좌상단 원점 변환은 강화 트리와
 * 동일). 좌측 보유 스킬 목록, 우측 상세(쿨다운·효과 수치는 카탈로그/파생 모델에서 공급),
 * 하단 장착 슬롯 5칸(용량 밖은 잠금 표시)과 키 등록 버튼.
 *
 * 키 등록은 버튼 클릭 → 다음 키 입력 대기 → Esc 취소 흐름이며 예약 키(이동/상호작용/가방 등)와
 * 다른 슬롯과의 중복은 모델(SkillLoadout.setKey)이 거부하고 사유를 메시지로 보여준다.
 * 장착/해제/키 변경이 성공할 때만 onSave(기존 원자적 프로필 저장 흐름)를 호출한다.
 * 패널이 열려 있는 동안 화면이 월드 입력을 차단하므로 대기 중 키가 스킬로 새지 않는다.
 */
public final class SkillLoadoutRenderer {

    /** 상세 표시용 실제 계산 수치 공급자(수치 원본: 강화 카탈로그 + 스킬 카탈로그 + BalanceConfig). */
    public interface SkillInfo {
        int healAmount();
        float cooldownSeconds(String skillId);
        /** 충격파/전기 투척의 실제 피해(기본 + 전용 강화). 그 외 스킬은 0. */
        int skillDamage(String skillId);
        /** 보호막 실제 흡수량(기본 + 강화). */
        int shieldAbsorb();
    }

    private static final float REF_W = 1120f, REF_H = 720f;
    private static final float[] TITLE = {48, 42, 780, 45};
    private static final float[] LIST = {48, 124, 470, 390};
    private static final float[] DETAILS = {560, 124, 510, 295};
    private static final float[] LOADOUT = {68, 530, 640, 120};
    private static final float[] BTN_REGISTER = {814, 548, 240, 60};
    private static final float[] BTN_EQUIP = {560, 440, 150, 48};
    private static final float[] BTN_UNEQUIP = {730, 440, 150, 48};
    private static final float[] BTN_CLOSE = {1060, 48, 28, 28};
    private static final float[] MESSAGE = {68, 666, 980, 30};
    private static final float LIST_ROW_H = 68f, SLOT = 72f, SLOT_GAP = 24f;
    private static final String[] SKILL_IDS = {SkillLoadout.DASH, SkillLoadout.HEAL,
        SkillLoadout.SHOCKWAVE, SkillLoadout.ELECTRIC_THROW, SkillLoadout.SHIELD};

    private final SkillAssets ui;
    private final io.github.some_example_name.asset.ThreeSkillsAssets threeSkills;
    private final TextureRegion dashIcon;
    private final BitmapFont numberFont;
    private final KoreanLabelCache labels = new KoreanLabelCache(20);
    private final KoreanLabelCache smallLabels = new KoreanLabelCache(15);
    private final SpriteBatch batch = new SpriteBatch();
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final OrthographicCamera camera = new OrthographicCamera();
    private final GlyphLayout glyph = new GlyphLayout();

    private SkillLoadout loadout;
    private int capacity;
    private Set<String> unlocked;
    private Set<Integer> reservedKeys;
    private SkillInfo info;
    private Runnable onSave;
    private boolean open;
    private String selectedSkillId;
    private int selectedSlot = -1;
    private int waitingKeySlot = -1;
    private String message = "";
    private boolean messageIsError;
    private float scale, originX, originBottomY;

    public SkillLoadoutRenderer(SkillAssets ui,
                                io.github.some_example_name.asset.ThreeSkillsAssets threeSkills,
                                TextureRegion dashIcon, BitmapFont numberFont) {
        this.ui = ui;
        this.threeSkills = threeSkills;
        this.dashIcon = dashIcon;
        this.numberFont = numberFont;
    }

    public boolean isOpen() { return open; }

    public void open(SkillLoadout loadout, int capacity, Set<String> unlocked,
                     Set<Integer> reservedKeys, SkillInfo info, Runnable onSave) {
        this.loadout = loadout;
        this.capacity = capacity;
        this.unlocked = unlocked;
        this.reservedKeys = reservedKeys;
        this.info = info;
        this.onSave = onSave;
        selectedSkillId = null;
        selectedSlot = -1;
        waitingKeySlot = -1;
        message = "";
        messageIsError = false;
        open = true;
    }

    public void close() { open = false; waitingKeySlot = -1; }

    /** Esc 1회: 키 대기 중이면 취소만, 아니면 패널만 닫는다(일시정지로 새지 않음). */
    public boolean onEscape() {
        if (!open) return false;
        if (waitingKeySlot >= 0) {
            waitingKeySlot = -1;
            setMessage("키 등록 취소", false);
        } else {
            close();
        }
        return true;
    }

    /** 개발 캡처용. */
    public void selectSlot(int slot) { selectedSlot = slot; }
    public void selectSkill(String skillId) { selectedSkillId = skillId; }
    public void beginKeyCapture() { if (selectedSlot >= 0) startKeyCapture(); }

    // ----- 입력 ---------------------------------------------------------------------

    public void handleInput() {
        if (!open || loadout == null) return;
        if (waitingKeySlot >= 0) { pollKeyCapture(); return; }
        if (!Gdx.input.justTouched()) return;
        updateTransform();
        float ux = (Gdx.input.getX() - originX) / scale;
        float panelTopScreenY = Gdx.graphics.getHeight() - (originBottomY + REF_H * scale);
        float uy = (Gdx.input.getY() - panelTopScreenY) / scale;
        if (hit(BTN_CLOSE, ux, uy)) { close(); return; }
        if (hit(BTN_EQUIP, ux, uy)) { equipSelected(); return; }
        if (hit(BTN_UNEQUIP, ux, uy)) { unequipSelected(); return; }
        if (hit(BTN_REGISTER, ux, uy)) {
            if (selectedSlot < 0) setMessage("키를 등록할 슬롯을 먼저 선택하세요", true);
            else if (selectedSlot >= capacity) setMessage("잠긴 슬롯에는 키를 등록할 수 없습니다", true);
            else startKeyCapture();
            return;
        }
        // 스킬 목록 행 클릭 = 선택만
        for (int i = 0; i < SKILL_IDS.length; i++) {
            float top = LIST[1] + 46 + i * LIST_ROW_H;
            if (ux >= LIST[0] && ux <= LIST[0] + LIST[2] && uy >= top && uy <= top + LIST_ROW_H - 8) {
                selectedSkillId = SKILL_IDS[i];
                message = "";
                return;
            }
        }
        // 장착 슬롯 클릭 = 선택만(잠긴 슬롯 포함 — 사유 안내용)
        for (int slot = 0; slot < SkillLoadout.SLOT_COUNT; slot++) {
            float[] rect = slotRect(slot);
            if (hit(rect, ux, uy)) {
                selectedSlot = slot;
                message = slot >= capacity ? "잠긴 슬롯 — 강화 트리에서 확장" : "";
                messageIsError = slot >= capacity;
                return;
            }
        }
    }

    /** 장착: 모델이 잠금/미해금/중복을 재검증하고 성공 시에만 저장한다. */
    public void equipSelected() {
        if (selectedSkillId == null) { setMessage("장착할 스킬을 선택하세요", true); return; }
        if (selectedSlot < 0) { setMessage("장착할 슬롯을 선택하세요", true); return; }
        String reason = loadout.equip(selectedSlot, selectedSkillId, capacity, unlocked);
        if (reason != null) { setMessage(reason, true); return; }
        setMessage(skillName(selectedSkillId) + " 스킬을 " + (selectedSlot + 1) + "번 슬롯에 장착", false);
        if (onSave != null) onSave.run();
    }

    public void unequipSelected() {
        if (selectedSlot < 0 || loadout.skillAt(selectedSlot) == null) {
            setMessage("해제할 스킬이 없습니다", true);
            return;
        }
        String name = skillName(loadout.skillAt(selectedSlot));
        loadout.unequip(selectedSlot);
        setMessage(name + " 스킬 장착 해제", false);
        if (onSave != null) onSave.run();
    }

    private void startKeyCapture() {
        waitingKeySlot = selectedSlot;
        setMessage("등록할 키를 누르세요 (Esc 취소)", false);
    }

    private void pollKeyCapture() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) return; // onEscape가 취소 처리
        if (!Gdx.input.isKeyJustPressed(Input.Keys.ANY_KEY)) return;
        for (int key = 1; key < 256; key++) {
            if (!Gdx.input.isKeyJustPressed(key)) continue;
            String reason = loadout.setKey(waitingKeySlot, key, reservedKeys);
            if (reason != null) {
                setMessage(reason + " — 다른 키를 누르세요", true);
            } else {
                setMessage((waitingKeySlot + 1) + "번 슬롯 키 등록: " + Input.Keys.toString(key), false);
                waitingKeySlot = -1;
                if (onSave != null) onSave.run();
            }
            return;
        }
    }

    private void setMessage(String text, boolean error) {
        message = text;
        messageIsError = error;
    }

    private static String skillName(String skillId) {
        if (SkillLoadout.DASH.equals(skillId)) return "돌진";
        if (SkillLoadout.HEAL.equals(skillId)) return "회복";
        if (SkillLoadout.SHOCKWAVE.equals(skillId)) return "충격파";
        if (SkillLoadout.ELECTRIC_THROW.equals(skillId)) return "전기 투척";
        if (SkillLoadout.SHIELD.equals(skillId)) return "보호막";
        return skillId;
    }

    // ----- 그리기 -------------------------------------------------------------------

    public void render() {
        if (!open || loadout == null) return;
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
        batch.draw(ui.panel(), originX, originBottomY, REF_W * scale, REF_H * scale);
        text(labels.label("스킬 — 장착과 키 등록"), TITLE, false);
        text(labels.label("X"), BTN_CLOSE, true);
        drawSkillList();
        drawDetails();
        drawLoadout();
        drawButton(BTN_REGISTER, waitingKeySlot >= 0 ? "키 입력 대기중..." : "키 등록",
            waitingKeySlot >= 0 || (selectedSlot >= 0 && selectedSlot < capacity));
        if (!message.isEmpty()) {
            batch.setColor(messageIsError ? Color.SALMON : Color.valueOf("9CFFB0"));
            text(smallLabels.label(message), MESSAGE, false);
            batch.setColor(Color.WHITE);
        }
        batch.end();
    }

    private void drawSkillList() {
        text(smallLabels.label("보유 스킬"), new float[]{LIST[0], LIST[1] + 8, LIST[2], 24}, false);
        for (int i = 0; i < SKILL_IDS.length; i++) {
            String skillId = SKILL_IDS[i];
            boolean owned = unlocked.contains(skillId);
            float top = LIST[1] + 46 + i * LIST_ROW_H;
            if (skillId.equals(selectedSkillId))
                draw(ui.slotSelected(), new float[]{LIST[0], top, 64, 64});
            else
                draw(owned ? ui.slotNormal() : ui.slotLocked(), new float[]{LIST[0], top, 64, 64});
            if (!owned) batch.setColor(0.45f, 0.45f, 0.45f, 1f);
            draw(icon(skillId), new float[]{LIST[0] + 8, top + 8, 48, 48});
            batch.setColor(Color.WHITE);
            text(labels.label(skillName(skillId)),
                new float[]{LIST[0] + 76, top + 6, 160, 24}, false);
            int slot = loadout.slotOf(skillId);
            String status = !owned ? "미해금 — 강화 트리에서 해금"
                : slot >= 0 ? (slot + 1) + "번 슬롯 장착" : "미장착";
            batch.setColor(!owned ? Color.GRAY : slot >= 0 ? Color.valueOf("9CFFB0") : Color.LIGHT_GRAY);
            text(smallLabels.label(status), new float[]{LIST[0] + 76, top + 34, 320, 20}, false);
            batch.setColor(Color.WHITE);
        }
    }

    private void drawDetails() {
        if (selectedSkillId == null) {
            text(smallLabels.label("스킬을 선택하세요"),
                new float[]{DETAILS[0], DETAILS[1] + 16, DETAILS[2], 24}, false);
            drawButton(BTN_EQUIP, "장착", false);
            drawButton(BTN_UNEQUIP, "해제", selectedSlot >= 0 && loadout.skillAt(selectedSlot) != null);
            return;
        }
        boolean owned = unlocked.contains(selectedSkillId);
        float y = DETAILS[1] + 8;
        text(labels.label(skillName(selectedSkillId)),
            new float[]{DETAILS[0], y, DETAILS[2], 26}, false);
        y += 40;
        String[] lines = detailLines(selectedSkillId);
        for (String line : lines) {
            text(smallLabels.label(line), new float[]{DETAILS[0], y, DETAILS[2], 20}, false);
            y += 26;
        }
        y += 8;
        text(smallLabels.label("재사용 대기시간"),
            new float[]{DETAILS[0], y, 170, 20}, false);
        drawNumber(String.format("%.0fs", info.cooldownSeconds(selectedSkillId)),
            DETAILS[0] + 180, y, 20f, Color.WHITE);
        y += 30;
        if (!owned)
            text(smallLabels.label("미해금 — 본부 강화대에서 해금 후 장착할 수 있습니다"),
                new float[]{DETAILS[0], y, DETAILS[2], 20}, false);
        drawButton(BTN_EQUIP, "장착", owned && selectedSlot >= 0);
        drawButton(BTN_UNEQUIP, "해제", selectedSlot >= 0 && loadout.skillAt(selectedSlot) != null);
    }

    private float[] slotRect(int slot) {
        float x = LOADOUT[0] + 16 + slot * (SLOT + SLOT_GAP);
        return new float[]{x, LOADOUT[1] + 10, SLOT, SLOT};
    }

    private void drawLoadout() {
        for (int slot = 0; slot < SkillLoadout.SLOT_COUNT; slot++) {
            float[] rect = slotRect(slot);
            boolean locked = slot >= capacity;
            draw(locked ? ui.slotLocked()
                : slot == selectedSlot ? ui.slotSelected() : ui.slotNormal(), rect);
            String skillId = loadout.skillAt(slot);
            if (!locked && skillId != null)
                draw(icon(skillId), new float[]{rect[0] + 10, rect[1] + 10, SLOT - 20, SLOT - 20});
            // 슬롯 아래 키캡 + 실제 키 이름(미등록 포함)
            float[] cap = {rect[0] + (SLOT - 52) / 2f, rect[1] + SLOT + 6, 52, 26};
            if (!locked) {
                draw(ui.keycap(), cap);
                int key = loadout.keyAt(slot);
                if (key == SkillLoadout.KEY_NONE)
                    text(smallLabels.label("미등록"),
                        new float[]{cap[0], cap[1] + 6, cap[2], cap[3] - 12}, true);
                else
                    drawNumberCentered(Input.Keys.toString(key), cap[0] + cap[2] / 2f,
                        cap[1] + 6, 14f, Color.BLACK);
            }
        }
    }

    /** 효과 설명 + 실제 계산된 값(강화 반영 — 이미지에 수치를 넣지 않는다). */
    private String[] detailLines(String skillId) {
        if (SkillLoadout.DASH.equals(skillId))
            return new String[]{"바라보는 방향으로 3타일 돌진합니다.",
                "돌진 중 몬스터를 통과하며 피해를 1회 입힙니다."};
        if (SkillLoadout.HEAL.equals(skillId))
            return new String[]{"잃은 체력을 " + info.healAmount() + " 회복합니다.",
                "체력이 가득 차 있으면 사용되지 않습니다."};
        if (SkillLoadout.SHOCKWAVE.equals(skillId))
            return new String[]{"주변 2.5타일로 퍼지는 충격파가 피해 "
                + info.skillDamage(skillId) + "을 입히고",
                "적을 0.8타일 밀어냅니다. 벽 뒤는 맞지 않습니다."};
        if (SkillLoadout.ELECTRIC_THROW.equals(skillId))
            return new String[]{"바라보는 방향으로 전기 구체를 던져 피해 "
                + info.skillDamage(skillId) + "과",
                "0.5초 감전을 입힙니다. 최대 8타일, 첫 충돌에서 소멸."};
        if (SkillLoadout.SHIELD.equals(skillId))
            return new String[]{"4초 동안 피해를 " + info.shieldAbsorb() + "까지 흡수하는",
                "보호막을 펼칩니다. 활성 중 재시전은 되지 않습니다."};
        return new String[]{""};
    }

    private TextureRegion icon(String skillId) {
        if (SkillLoadout.DASH.equals(skillId)) return dashIcon;
        if (SkillLoadout.HEAL.equals(skillId)) return ui.iconHeal();
        TextureRegion icon = threeSkills.icon(skillId);
        return icon != null ? icon : ui.iconHeal();
    }

    // ----- 좌표/그리기 보조(강화 트리와 동일 방식) ---------------------------------------

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
        draw(ui.registerButton(), rect);
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

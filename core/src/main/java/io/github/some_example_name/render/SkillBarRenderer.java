package io.github.some_example_name.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.github.some_example_name.asset.SkillAssets;
import io.github.some_example_name.profile.SkillLoadout;
import io.github.some_example_name.profile.SkillRuntime;

/**
 * 스킬 퀵슬롯 HUD 5칸(화면 하단 중앙, bottomSafeMargin 24px — 가방 안내(좌하)·전투 HUD(우하)와
 * 영역이 겹치지 않는 중앙 배치). 슬롯 상태를 코드로 구분해 그린다:
 * 잠긴 슬롯(용량 밖)=slot_locked 텍스처(숫자 없음), 빈 슬롯=slot_normal만,
 * 쿨다운=아래→위 가림 + 올림한 남은 초, 준비 완료=가림 없음.
 * 키캡 글자는 실제 등록된 키 이름(Input.Keys.toString)이고 미등록이면 한글 '미등록' 라벨.
 * 아이콘·키는 임의 하드코딩 없이 SkillLoadout에서 읽는다.
 */
public final class SkillBarRenderer {

    /** 스킬별 쿨다운 전체 길이(초) — 겹침 비율 계산용. 수치 원본은 카탈로그/BalanceConfig. */
    public interface CooldownTotals { float total(String skillId); }

    private static final float SLOT = 72f, GAP = 12f, BOTTOM = 24f;
    private static final float KEYCAP_W = 52f, KEYCAP_H = 26f;

    private final SpriteBatch batch = new SpriteBatch();
    private final OrthographicCamera camera = new OrthographicCamera();
    private final BitmapFont font; // 공유 Mona 폰트(ASCII 키 이름·숫자) — 여기서 해제 금지
    private final GlyphLayout glyph = new GlyphLayout();
    private final KoreanLabelCache labels = new KoreanLabelCache(13);

    public SkillBarRenderer(BitmapFont sharedUiFont) { this.font = sharedUiFont; }

    public void render(SkillAssets ui, io.github.some_example_name.asset.ThreeSkillsAssets three,
                       TextureRegion dashIcon, Texture whitePixel,
                       SkillLoadout loadout, SkillRuntime runtime, int capacity,
                       CooldownTotals totals) {
        float sw = Gdx.graphics.getWidth(), sh = Gdx.graphics.getHeight();
        camera.setToOrtho(false, sw, sh);
        batch.setProjectionMatrix(camera.combined);
        float barW = SkillLoadout.SLOT_COUNT * SLOT + (SkillLoadout.SLOT_COUNT - 1) * GAP;
        float x0 = (sw - barW) / 2f;
        float y = BOTTOM + KEYCAP_H * 0.55f; // 키캡이 슬롯 아래로 겹쳐 내려가는 몫까지 안전 영역 확보
        batch.begin();
        for (int slot = 0; slot < SkillLoadout.SLOT_COUNT; slot++) {
            float x = x0 + slot * (SLOT + GAP);
            boolean locked = slot >= capacity;
            batch.draw(locked ? ui.slotLocked() : ui.slotNormal(), x, y, SLOT, SLOT);
            if (locked) continue; // 잠긴 슬롯: 아이콘·키캡·숫자 없음(쿨다운과 시각적으로 구분)
            String skillId = loadout.skillAt(slot);
            if (skillId != null) {
                float remaining = runtime.remainingSeconds(skillId);
                boolean cooling = remaining > 0f;
                TextureRegion icon = SkillLoadout.DASH.equals(skillId) ? dashIcon
                    : SkillLoadout.HEAL.equals(skillId) ? ui.iconHeal() : three.icon(skillId);
                if (icon == null) icon = ui.iconHeal();
                float pad = 8f;
                batch.setColor(cooling ? 0.4f : 1f, cooling ? 0.4f : 1f, cooling ? 0.4f : 1f, 1f);
                batch.draw(icon, x + pad, y + pad, SLOT - pad * 2, SLOT - pad * 2);
                batch.setColor(Color.WHITE);
                if (cooling) {
                    float ratio = Math.min(1f, remaining / Math.max(0.01f, totals.total(skillId)));
                    batch.setColor(0f, 0f, 0f, 0.55f);
                    batch.draw(whitePixel, x + pad, y + pad, SLOT - pad * 2, (SLOT - pad * 2) * ratio);
                    batch.setColor(Color.WHITE);
                    // 남은 시간은 올림한 정수 초(0.2초 → 1).
                    drawCenteredText(String.valueOf((int) Math.ceil(remaining)),
                        x + SLOT / 2f, y + SLOT / 2f, 24f, Color.WHITE);
                }
            }
            // 키캡: 슬롯 하단 중앙에 겹침. 실제 등록 키 이름 또는 '미등록'.
            float kx = x + (SLOT - KEYCAP_W) / 2f, ky = y - KEYCAP_H * 0.55f;
            batch.draw(ui.keycap(), kx, ky, KEYCAP_W, KEYCAP_H);
            int key = loadout.keyAt(slot);
            if (key == SkillLoadout.KEY_NONE) {
                TextureRegion none = labels.label("미등록");
                float h = 11f, w = none.getRegionWidth() * (h / none.getRegionHeight());
                batch.draw(none, kx + (KEYCAP_W - w) / 2f, ky + (KEYCAP_H - h) / 2f, w, h);
            } else {
                String name = Input.Keys.toString(key);
                drawCenteredText(name, kx + KEYCAP_W / 2f, ky + KEYCAP_H / 2f + 1f,
                    name.length() > 2 ? 9f : 13f, Color.BLACK);
            }
        }
        batch.end();
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
        labels.dispose();
    }
}

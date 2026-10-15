package io.github.some_example_name.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import io.github.some_example_name.world.LootContainer;

/**
 * 컨테이너 상호작용 연출: SPACE 안내(월드 추적) + 연타 진행 카운트 + 등급별 그라데이션 발광.
 *
 * - 안내는 상호작용 대상 1개에만 표시하고, 이미 열린 컨테이너에는 표시하지 않는다.
 * - 발광은 유효한 개봉 입력마다 짧게 퍼졌다 사라지며(0.4s), 진행 비율이 높을수록 조금씩 밝아진다.
 *   마지막 입력은 더 크고 밝은 펄스로 열림 애니메이션과 자연스럽게 이어진다.
 * - 원본 아트를 덮지 않도록 가산 블렌딩의 방사형 투명 발광 텍스처를 쓰고, 그린 뒤
 *   블렌딩 함수·배치 색상을 반드시 복원한다.
 */
public final class ContainerInteractionRenderer implements Disposable {

    /** 등급 그라데이션(loot_tables.json border 색상과 동일 계열): 안쪽 밝음 → 바깥 짙음. */
    private static final Color EPIC_IN = Color.valueOf("E05CFF"), EPIC_OUT = Color.valueOf("8A27CC");
    private static final Color LEG_IN = Color.valueOf("FFF08A"), LEG_OUT = Color.valueOf("E8A921");
    private static final Color WOOD_IN = Color.valueOf("59D9FF"), WOOD_OUT = Color.valueOf("2168E8");

    private static final float PULSE_SECONDS = 0.4f, FINAL_PULSE_SECONDS = 0.65f;
    private static final float PRESSED_FRAME_SECONDS = 0.12f;

    private final Texture glowTexture;
    private final BitmapFont font = new BitmapFont();
    private final GlyphLayout layout = new GlyphLayout();

    // 한 번에 하나의 대상만 연타하므로 펄스 상태도 단일 대상에 저장한다.
    private LootContainer pulseContainer;
    private float pulseAge = Float.MAX_VALUE;
    private float pulseStrength;     // 진행 비율 기반 강도(0.45..1)
    private boolean finalPulse;
    private float pressedAge = Float.MAX_VALUE;

    public ContainerInteractionRenderer() {
        glowTexture = createRadialGlow(128);
        font.setUseIntegerPositions(false);
        font.getData().setScale(0.016f);
    }

    /** 방사형 발광(중심 불투명 → 바깥 완전 투명, 2차 감쇠). */
    private static Texture createRadialGlow(int size) {
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        float center = (size - 1) / 2f;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = (x - center) / center, dy = (y - center) / center;
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                float a = d >= 1f ? 0f : (1f - d) * (1f - d);
                pixmap.setColor(1f, 1f, 1f, a);
                pixmap.drawPixel(x, y);
            }
        }
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return texture;
    }

    /** 유효한 개봉 입력이 들어간 순간 호출(마지막 입력이면 opened=true). */
    public void onValidPress(LootContainer container, boolean opened) {
        pulseContainer = container;
        pulseAge = 0f;
        finalPulse = opened;
        pulseStrength = 0.45f + 0.55f * container.progressRatio();
        pressedAge = 0f;
    }

    public void update(float delta) {
        if (pulseAge != Float.MAX_VALUE) pulseAge += delta;
        if (pressedAge != Float.MAX_VALUE) pressedAge += delta;
    }

    /** 발광(컨테이너 스프라이트 위, 가산 블렌딩). 그린 뒤 상태를 복원한다. */
    public void renderGlow(SpriteBatch batch, float containerDrawHeight) {
        if (pulseContainer == null) return;
        float duration = finalPulse ? FINAL_PULSE_SECONDS : PULSE_SECONDS;
        if (pulseAge >= duration) return;
        float t = pulseAge / duration;                    // 0..1
        float fade = (1f - t) * (1f - t);
        float alpha = fade * pulseStrength * (finalPulse ? 1f : 0.7f);
        float spread = (finalPulse ? 1.7f : 1.25f) + t * (finalPulse ? 1.3f : 0.7f);
        float radius = containerDrawHeight * spread;
        float cx = pulseContainer.x(), cy = pulseContainer.y() + containerDrawHeight * 0.45f;

        Color in, out;
        if ("legendary_cabinet".equals(pulseContainer.id())) { in = LEG_IN; out = LEG_OUT; }
        else if ("epic_cabinet".equals(pulseContainer.id())) { in = EPIC_IN; out = EPIC_OUT; }
        else { in = WOOD_IN; out = WOOD_OUT; }

        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE); // 가산: 원본 아트를 덮지 않음
        // 바깥(짙은 색, 넓게) → 안쪽(밝은 색, 좁게) 두 겹으로 그라데이션.
        batch.setColor(out.r, out.g, out.b, alpha * 0.8f);
        batch.draw(glowTexture, cx - radius, cy - radius, radius * 2f, radius * 2f);
        float innerRadius = radius * 0.55f;
        batch.setColor(in.r, in.g, in.b, alpha);
        batch.draw(glowTexture, cx - innerRadius, cy - innerRadius, innerRadius * 2f, innerRadius * 2f);
        // 상태 복원(이후 스프라이트가 영향을 받지 않도록).
        batch.setColor(Color.WHITE);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    /**
     * 상호작용 대상 위 SPACE 안내 + (캐비넷) 연타 카운트. target이 null이거나 이미 열려 있으면
     * 아무것도 그리지 않는다. promptInteractable/pressed는 기존 spacebar 시트의 두 상태다.
     */
    public void renderPrompt(SpriteBatch batch, LootContainer target, float containerDrawHeight,
                             TextureRegion promptInteractable, TextureRegion promptPressed) {
        if (target == null || target.opened()) return;
        boolean pressed = pressedAge < PRESSED_FRAME_SECONDS;
        TextureRegion frame = pressed && pulseContainer == target ? promptPressed : promptInteractable;
        float w = 1.3f, h = 0.65f; // 128x64 비율. 20x15 뷰에서도 읽히는 크기.
        float x = target.x() - w / 2f, y = target.y() + containerDrawHeight + 0.18f;
        batch.draw(frame, x, y, w, h);
        if (target.requiredPresses() > 1) {
            layout.setText(font, target.presses() + " / " + target.requiredPresses());
            font.setColor(Color.WHITE);
            font.draw(batch, layout, target.x() - layout.width / 2f, y + h + 0.42f);
        }
    }

    @Override public void dispose() {
        glowTexture.dispose();
        font.dispose();
    }
}

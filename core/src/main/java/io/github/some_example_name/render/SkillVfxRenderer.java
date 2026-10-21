package io.github.some_example_name.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import io.github.some_example_name.asset.ThreeSkillsAssets;
import io.github.some_example_name.combat.ElectricProjectile;
import io.github.some_example_name.combat.PlayerShieldState;
import io.github.some_example_name.combat.ShockwaveState;
import io.github.some_example_name.entity.monster.Monster;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 추가 스킬 3종 월드 VFX(animations.json 기준: 128px 셀·1/15초·행 우선, 프레임별
 * trim/재중앙 없음). 판정과 같은 누적 시뮬레이션 시간으로 프레임을 고르며, 피격/착탄
 * 효과는 실제 명중 시에만 호출 측이 생성한다(빗나간 대상에 미리 띄우지 않는다).
 *
 * 레이어: 충격파는 엔티티 아래(renderGround), 피격·착탄·투사체·보호막·감전 오버레이는
 * 엔티티 위(renderOverlay). 전부 월드 좌표라 HP HUD/미니맵(화면 좌표)을 덮지 않는다.
 *
 * 크기 보정: 충격파는 셀 전체가 반경이 아니라 visualExtent(최대 프레임 고리 외곽
 * ≈46.8px/128px 셀)를 논리 최대 반경에 맞춰 draw scale을 계산한다 — 렌더 고리와 판정
 * 파면이 같은 속도로 커진다(F1 디버그 원과 대조).
 */
public final class SkillVfxRenderer {

    private static final float FRAME_SECONDS = 1f / 15f;
    /** animations.json visualExtent: 최대 고리 외곽 ≈46.819px(128px 셀 기준). */
    private static final float SHOCKWAVE_RING_RATIO = 46.819f / 128f;
    private static final float PROJECTILE_SIZE = 0.95f;
    private static final float HIT_SIZE = 1.25f, IMPACT_SIZE = 1.35f;
    private static final float SHIELD_SIZE = 1.7f;
    private static final float SHIELD_FOOT_RATIO = (128f - 102f) / 128f; // 앵커 (64,102)
    private static final float SHIELD_ALPHA = 0.65f;
    private static final float SHIELD_FADE_SECONDS = 0.2f;

    private enum OneShot { SHOCK_HIT, ELECTRIC_IMPACT, SHIELD_BREAK, SHIELD_FADE }

    private static final class Effect {
        final OneShot type;
        final float x, y;
        float elapsed;
        Effect(OneShot type, float x, float y) { this.type = type; this.x = x; this.y = y; }
    }

    private final ThreeSkillsAssets assets;
    private final float shockwaveMaxRadius;
    private final List<Effect> effects = new ArrayList<>();
    private float loopClock; // 투사체/보호막/감전 반복 프레임용(시각 전용)

    public SkillVfxRenderer(ThreeSkillsAssets assets, float shockwaveMaxRadius) {
        this.assets = assets;
        this.shockwaveMaxRadius = shockwaveMaxRadius;
    }

    public void update(float delta) {
        if (delta <= 0f) return;
        loopClock += delta;
        Iterator<Effect> it = effects.iterator();
        while (it.hasNext()) {
            Effect effect = it.next();
            effect.elapsed += delta;
            float life = effect.type == OneShot.SHIELD_FADE ? SHIELD_FADE_SECONDS
                : FRAME_SECONDS * 6f;
            if (effect.elapsed >= life) it.remove();
        }
    }

    /** 실제 명중 시에만 호출(충격파 피격 — 금빛). */
    public void spawnShockHit(float x, float y) { effects.add(new Effect(OneShot.SHOCK_HIT, x, y)); }
    /** 투사체 착탄(몬스터/벽 표면 — 청록). 벽 뒤 몬스터에는 생성하지 않는다. */
    public void spawnElectricImpact(float x, float y) { effects.add(new Effect(OneShot.ELECTRIC_IMPACT, x, y)); }
    /** 흡수량 소진 파괴(1회 — 파랑). */
    public void spawnShieldBreak(float x, float y) { effects.add(new Effect(OneShot.SHIELD_BREAK, x, y)); }
    /** 시간 만료: 파괴와 구분 — 마지막 버블 프레임의 알파 페이드. */
    public void spawnShieldFade(float x, float y) { effects.add(new Effect(OneShot.SHIELD_FADE, x, y)); }

    /** 사망/맵 이탈 정리. */
    public void clear() { effects.clear(); }

    // ----- 엔티티 아래(바닥) ------------------------------------------------------------

    public void renderGround(SpriteBatch batch, List<ShockwaveState> waves) {
        for (ShockwaveState wave : waves) {
            int frame = (int) (wave.elapsed() / FRAME_SECONDS);
            if (frame >= ThreeSkillsAssets.SHOCKWAVE_FRAMES) continue;
            // 최대 고리(프레임 4) 외곽 반경(셀의 46.819/128)이 논리 최대 반경과 일치하도록
            // 셀 한 변의 월드 크기를 역산: cell = maxRadius / (46.819/128) ≈ 2.5 → 6.83타일.
            float size = shockwaveMaxRadius / SHOCKWAVE_RING_RATIO;
            batch.draw(assets.shockwaveFrame(frame),
                wave.originX() - size / 2f, wave.originY() - size / 2f, size, size);
        }
    }

    // ----- 엔티티 위 -------------------------------------------------------------------

    public void renderOverlay(SpriteBatch batch, List<ElectricProjectile> projectiles,
                              List<Monster> monsters, PlayerShieldState shield,
                              float playerFeetX, float playerFeetY) {
        // 전기 투사체: 기준 방향 +X, 이동 방향 각도로 회전(에너지 투사체 — 회전 허용).
        int loopFrame = (int) (loopClock / FRAME_SECONDS);
        for (ElectricProjectile p : projectiles) {
            if (!p.alive()) continue;
            TextureRegion region = assets.projectileFrame(loopFrame % ThreeSkillsAssets.PROJECTILE_FRAMES);
            float half = PROJECTILE_SIZE / 2f;
            batch.draw(region, p.x() - half, p.y() - half, half, half,
                PROJECTILE_SIZE, PROJECTILE_SIZE, 1f, 1f,
                MathUtils.atan2(p.dirY(), p.dirX()) * MathUtils.radiansToDegrees);
        }
        // 감전 오버레이: 착탄 시트의 1~3 프레임을 실제 감전 남은 시간 동안만 반복.
        for (Monster monster : monsters) {
            if (!monster.isAlive() || !monster.shocked()) continue;
            TextureRegion region = assets.impactFrame(1 + loopFrame % 3);
            float cx = monster.bounds().x + monster.bounds().width / 2f;
            float cy = monster.bounds().y + monster.bounds().height / 2f;
            batch.setColor(1f, 1f, 1f, 0.8f);
            batch.draw(region, cx - 0.45f, cy - 0.45f, 0.9f, 0.9f);
            batch.setColor(Color.WHITE);
        }
        // 보호막: 생성(0~1) → 반복(2~5), 낮은 알파로 캐릭터가 읽히게. 플레이어를 따라다닌다.
        if (shield != null && (shield.casting() || shield.active())) {
            int frame = shield.casting()
                ? Math.min(1, (int) (shield.startupElapsed() / FRAME_SECONDS))
                : 2 + loopFrame % 4;
            drawShieldCell(batch, assets.shieldFrame(frame), playerFeetX, playerFeetY, SHIELD_ALPHA);
        }
        // 1회 효과들.
        for (Effect effect : effects) {
            int frame = (int) (effect.elapsed / FRAME_SECONDS);
            switch (effect.type) {
                case SHOCK_HIT:
                    if (frame < ThreeSkillsAssets.SHOCK_HIT_FRAMES)
                        batch.draw(assets.shockHitFrame(frame), effect.x - HIT_SIZE / 2f,
                            effect.y - HIT_SIZE / 2f, HIT_SIZE, HIT_SIZE);
                    break;
                case ELECTRIC_IMPACT:
                    if (frame < ThreeSkillsAssets.IMPACT_FRAMES)
                        batch.draw(assets.impactFrame(frame), effect.x - IMPACT_SIZE / 2f,
                            effect.y - IMPACT_SIZE / 2f, IMPACT_SIZE, IMPACT_SIZE);
                    break;
                case SHIELD_BREAK:
                    if (frame < ThreeSkillsAssets.SHIELD_BREAK_FRAMES)
                        drawShieldCell(batch, assets.shieldBreakFrame(frame), effect.x, effect.y, 0.9f);
                    break;
                case SHIELD_FADE:
                    float alpha = SHIELD_ALPHA * (1f - effect.elapsed / SHIELD_FADE_SECONDS);
                    drawShieldCell(batch, assets.shieldFrame(5), effect.x, effect.y, Math.max(0f, alpha));
                    break;
            }
        }
    }

    private void drawShieldCell(SpriteBatch batch, TextureRegion region,
                                float feetX, float feetY, float alpha) {
        batch.setColor(1f, 1f, 1f, alpha);
        batch.draw(region, feetX - SHIELD_SIZE / 2f, feetY - SHIELD_SIZE * SHIELD_FOOT_RATIO,
            SHIELD_SIZE, SHIELD_SIZE);
        batch.setColor(Color.WHITE);
    }
}

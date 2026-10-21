package io.github.some_example_name.asset;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import io.github.some_example_name.config.AssetPaths;
import io.github.some_example_name.profile.SkillLoadout;

/**
 * 추가 스킬 3종 텍스처(player_three_skills_v1). Nearest, GameAssets 소유·해제.
 * 모든 VFX 시트는 128px 셀·좌상단 원점·행 우선(animations.json과 동일)이고 개별 프레임
 * PNG는 중복 로딩하지 않는다. 숫자/등급/피해는 이미지에 없다(런타임이 그린다).
 */
public final class ThreeSkillsAssets implements Disposable {

    public static final int SHOCKWAVE_FRAMES = 8;   // 4×2
    public static final int SHOCK_HIT_FRAMES = 6;   // 3×2
    public static final int PROJECTILE_FRAMES = 4;  // 2×2(비행 중 반복)
    public static final int IMPACT_FRAMES = 6;      // 3×2
    public static final int SHIELD_FRAMES = 6;      // 3×2(0~1 생성, 2~5 반복)
    public static final int SHIELD_BREAK_FRAMES = 6;

    private final Texture iconShockTex, iconElectricTex, iconShieldTex,
        shockwaveTex, shockHitTex, projectileTex, impactTex, shieldTex, shieldBreakTex;
    private final TextureRegion iconShock, iconElectric, iconShield;
    private final TextureRegion[] shockwave = new TextureRegion[SHOCKWAVE_FRAMES];
    private final TextureRegion[] shockHit = new TextureRegion[SHOCK_HIT_FRAMES];
    private final TextureRegion[] projectile = new TextureRegion[PROJECTILE_FRAMES];
    private final TextureRegion[] impact = new TextureRegion[IMPACT_FRAMES];
    private final TextureRegion[] shield = new TextureRegion[SHIELD_FRAMES];
    private final TextureRegion[] shieldBreak = new TextureRegion[SHIELD_BREAK_FRAMES];

    public ThreeSkillsAssets() {
        iconShockTex = GameAssets.loadTile(AssetPaths.ICON_SHOCKWAVE);
        iconElectricTex = GameAssets.loadTile(AssetPaths.ICON_ELECTRIC);
        iconShieldTex = GameAssets.loadTile(AssetPaths.ICON_SHIELD);
        shockwaveTex = GameAssets.loadTile(AssetPaths.VFX_SHOCKWAVE);
        shockHitTex = GameAssets.loadTile(AssetPaths.VFX_SHOCK_HIT);
        projectileTex = GameAssets.loadTile(AssetPaths.VFX_ELECTRIC_PROJECTILE);
        impactTex = GameAssets.loadTile(AssetPaths.VFX_ELECTRIC_IMPACT);
        shieldTex = GameAssets.loadTile(AssetPaths.VFX_SHIELD_ACTIVE);
        shieldBreakTex = GameAssets.loadTile(AssetPaths.VFX_SHIELD_BREAK);
        iconShock = new TextureRegion(iconShockTex);
        iconElectric = new TextureRegion(iconElectricTex);
        iconShield = new TextureRegion(iconShieldTex);
        split(shockwaveTex, 4, shockwave);
        split(shockHitTex, 3, shockHit);
        split(projectileTex, 2, projectile);
        split(impactTex, 3, impact);
        split(shieldTex, 3, shield);
        split(shieldBreakTex, 3, shieldBreak);
    }

    private static void split(Texture sheet, int columns, TextureRegion[] out) {
        TextureRegion[][] grid = TextureRegion.split(sheet, 128, 128);
        for (int i = 0; i < out.length; i++) out[i] = grid[i / columns][i % columns];
    }

    /** 퀵슬롯/스킬창 공용 아이콘(64×64). */
    public TextureRegion icon(String skillId) {
        if (SkillLoadout.SHOCKWAVE.equals(skillId)) return iconShock;
        if (SkillLoadout.ELECTRIC_THROW.equals(skillId)) return iconElectric;
        if (SkillLoadout.SHIELD.equals(skillId)) return iconShield;
        return null;
    }

    private static TextureRegion frame(TextureRegion[] frames, int index) {
        return frames[Math.max(0, Math.min(frames.length - 1, index))];
    }
    public TextureRegion shockwaveFrame(int i) { return frame(shockwave, i); }
    public TextureRegion shockHitFrame(int i) { return frame(shockHit, i); }
    public TextureRegion projectileFrame(int i) { return frame(projectile, i); }
    public TextureRegion impactFrame(int i) { return frame(impact, i); }
    public TextureRegion shieldFrame(int i) { return frame(shield, i); }
    public TextureRegion shieldBreakFrame(int i) { return frame(shieldBreak, i); }

    @Override public void dispose() {
        iconShockTex.dispose(); iconElectricTex.dispose(); iconShieldTex.dispose();
        shockwaveTex.dispose(); shockHitTex.dispose(); projectileTex.dispose();
        impactTex.dispose(); shieldTex.dispose(); shieldBreakTex.dispose();
    }
}

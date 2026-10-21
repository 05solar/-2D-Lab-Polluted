package io.github.some_example_name.asset;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import io.github.some_example_name.config.AssetPaths;

/**
 * 공통 스킬 시스템 UI/VFX 텍스처(player_skill_loadout_v1). Nearest, GameAssets 소유·해제.
 * 회복 캐스트 시트는 4열×3행 128px 셀을 행 우선으로 잘라 12프레임을 만든다
 * (split 결과 [row][col], row 0 = PNG 상단 = 첫 프레임 — trim/재중앙 정렬 없음).
 */
public final class SkillAssets implements Disposable {

    public static final int HEAL_FRAMES = 12;
    private final Texture iconHealTex, sheetTex, slotNormalTex, slotSelectedTex,
        slotLockedTex, keycapTex, panelTex, registerButtonTex;
    private final TextureRegion iconHeal, slotNormal, slotSelected, slotLocked,
        keycap, panel, registerButton;
    private final TextureRegion[] healFrames = new TextureRegion[HEAL_FRAMES];

    public SkillAssets() {
        iconHealTex = GameAssets.loadTile(AssetPaths.SKILL_ICON_HEAL);
        sheetTex = GameAssets.loadTile(AssetPaths.SKILL_HEAL_SHEET);
        slotNormalTex = GameAssets.loadTile(AssetPaths.SKILL_SLOT_NORMAL);
        slotSelectedTex = GameAssets.loadTile(AssetPaths.SKILL_SLOT_SELECTED);
        slotLockedTex = GameAssets.loadTile(AssetPaths.SKILL_SLOT_LOCKED);
        keycapTex = GameAssets.loadTile(AssetPaths.SKILL_KEYCAP);
        panelTex = GameAssets.loadTile(AssetPaths.SKILL_PANEL);
        registerButtonTex = GameAssets.loadTile(AssetPaths.SKILL_REGISTER_BUTTON);
        iconHeal = new TextureRegion(iconHealTex);
        slotNormal = new TextureRegion(slotNormalTex);
        slotSelected = new TextureRegion(slotSelectedTex);
        slotLocked = new TextureRegion(slotLockedTex);
        keycap = new TextureRegion(keycapTex);
        panel = new TextureRegion(panelTex);
        registerButton = new TextureRegion(registerButtonTex);
        TextureRegion[][] grid = TextureRegion.split(sheetTex, 128, 128);
        for (int i = 0; i < HEAL_FRAMES; i++) healFrames[i] = grid[i / 4][i % 4];
    }

    public TextureRegion iconHeal() { return iconHeal; }
    public TextureRegion healFrame(int index) {
        return healFrames[Math.max(0, Math.min(HEAL_FRAMES - 1, index))];
    }
    public TextureRegion slotNormal() { return slotNormal; }
    public TextureRegion slotSelected() { return slotSelected; }
    public TextureRegion slotLocked() { return slotLocked; }
    public TextureRegion keycap() { return keycap; }
    public TextureRegion panel() { return panel; }
    public TextureRegion registerButton() { return registerButton; }

    @Override public void dispose() {
        iconHealTex.dispose(); sheetTex.dispose(); slotNormalTex.dispose();
        slotSelectedTex.dispose(); slotLockedTex.dispose(); keycapTex.dispose();
        panelTex.dispose(); registerButtonTex.dispose();
    }
}

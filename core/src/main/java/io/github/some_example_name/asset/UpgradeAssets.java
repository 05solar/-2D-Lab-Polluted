package io.github.some_example_name.asset;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import io.github.some_example_name.config.AssetPaths;

/**
 * 강화대·강화 트리 UI 텍스처(hq_upgrade_tree_v1). Nearest, GameAssets 소유·해제.
 * 공용 버튼·재화 아이콘은 판매 팩(HqSalesAssets)과 동일 디자인이라 중복 로드하지 않고 공유한다.
 */
public final class UpgradeAssets implements Disposable {

    private final Texture panelTex, benchTex, lockedTex, availableTex, purchasedTex,
        iconHealthTex, iconAttackTex, iconSkillTex,
        linkHActiveTex, linkHCompleteTex, linkHInactiveTex,
        linkVActiveTex, linkVCompleteTex, linkVInactiveTex;
    private final TextureRegion panel, bench, nodeLocked, nodeAvailable, nodePurchased,
        iconHealth, iconAttack, iconSkill,
        linkHActive, linkHComplete, linkHInactive, linkVActive, linkVComplete, linkVInactive;

    public UpgradeAssets() {
        panelTex = GameAssets.loadTile(AssetPaths.UPGRADE_PANEL);
        benchTex = GameAssets.loadTile(AssetPaths.UPGRADE_BENCH);
        lockedTex = GameAssets.loadTile(AssetPaths.UPGRADE_NODE_LOCKED);
        availableTex = GameAssets.loadTile(AssetPaths.UPGRADE_NODE_AVAILABLE);
        purchasedTex = GameAssets.loadTile(AssetPaths.UPGRADE_NODE_PURCHASED);
        iconHealthTex = GameAssets.loadTile(AssetPaths.UPGRADE_ICON_HEALTH);
        iconAttackTex = GameAssets.loadTile(AssetPaths.UPGRADE_ICON_ATTACK);
        iconSkillTex = GameAssets.loadTile(AssetPaths.UPGRADE_ICON_SKILL);
        linkHActiveTex = GameAssets.loadTile(AssetPaths.UPGRADE_LINK_H_ACTIVE);
        linkHCompleteTex = GameAssets.loadTile(AssetPaths.UPGRADE_LINK_H_COMPLETE);
        linkHInactiveTex = GameAssets.loadTile(AssetPaths.UPGRADE_LINK_H_INACTIVE);
        linkVActiveTex = GameAssets.loadTile(AssetPaths.UPGRADE_LINK_V_ACTIVE);
        linkVCompleteTex = GameAssets.loadTile(AssetPaths.UPGRADE_LINK_V_COMPLETE);
        linkVInactiveTex = GameAssets.loadTile(AssetPaths.UPGRADE_LINK_V_INACTIVE);
        panel = new TextureRegion(panelTex);
        bench = new TextureRegion(benchTex);
        nodeLocked = new TextureRegion(lockedTex);
        nodeAvailable = new TextureRegion(availableTex);
        nodePurchased = new TextureRegion(purchasedTex);
        iconHealth = new TextureRegion(iconHealthTex);
        iconAttack = new TextureRegion(iconAttackTex);
        iconSkill = new TextureRegion(iconSkillTex);
        linkHActive = new TextureRegion(linkHActiveTex);
        linkHComplete = new TextureRegion(linkHCompleteTex);
        linkHInactive = new TextureRegion(linkHInactiveTex);
        linkVActive = new TextureRegion(linkVActiveTex);
        linkVComplete = new TextureRegion(linkVCompleteTex);
        linkVInactive = new TextureRegion(linkVInactiveTex);
    }

    public TextureRegion panel() { return panel; }
    public TextureRegion bench() { return bench; }
    public TextureRegion nodeFrame(int state) { // 0=locked 1=available 2=purchased
        return state == 2 ? nodePurchased : state == 1 ? nodeAvailable : nodeLocked;
    }
    public TextureRegion icon(String iconId) {
        if ("health".equals(iconId)) return iconHealth;
        if ("attack".equals(iconId)) return iconAttack;
        return iconSkill;
    }
    public TextureRegion iconSkill() { return iconSkill; }
    public TextureRegion linkH(int state) { // 0=inactive 1=active 2=complete
        return state == 2 ? linkHComplete : state == 1 ? linkHActive : linkHInactive;
    }
    public TextureRegion linkV(int state) {
        return state == 2 ? linkVComplete : state == 1 ? linkVActive : linkVInactive;
    }

    @Override public void dispose() {
        panelTex.dispose(); benchTex.dispose(); lockedTex.dispose(); availableTex.dispose();
        purchasedTex.dispose(); iconHealthTex.dispose(); iconAttackTex.dispose(); iconSkillTex.dispose();
        linkHActiveTex.dispose(); linkHCompleteTex.dispose(); linkHInactiveTex.dispose();
        linkVActiveTex.dispose(); linkVCompleteTex.dispose(); linkVInactiveTex.dispose();
    }
}

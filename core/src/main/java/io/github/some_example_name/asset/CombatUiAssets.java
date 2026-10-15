package io.github.some_example_name.asset;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import io.github.some_example_name.config.AssetPaths;

/**
 * 돌진/공격 HUD 아이콘·키캡(combat_loot_update_v1/runtime/ui, 투명 RGBA, Nearest).
 * 비활성 색·쿨다운 가림·남은 시간 숫자는 베이크된 텍스처가 아니라 렌더러가 코드로 그린다.
 * 텍스처는 GameAssets 생명주기에서 소유·해제한다(프레임마다 새로 로드하지 않는다).
 */
public final class CombatUiAssets implements Disposable {

    private final Texture dashIconTex, attackIconTex, keyFTex, keyBlankTex;
    private final TextureRegion dashIcon, attackIcon, keyF, keyBlank;

    public CombatUiAssets() {
        dashIconTex = GameAssets.loadTile(AssetPaths.COMBAT_UI_DASH_ICON);
        attackIconTex = GameAssets.loadTile(AssetPaths.COMBAT_UI_ATTACK_ICON);
        keyFTex = GameAssets.loadTile(AssetPaths.COMBAT_UI_KEY_F);
        keyBlankTex = GameAssets.loadTile(AssetPaths.COMBAT_UI_KEY_BLANK);
        dashIcon = new TextureRegion(dashIconTex);
        attackIcon = new TextureRegion(attackIconTex);
        keyF = new TextureRegion(keyFTex);
        keyBlank = new TextureRegion(keyBlankTex);
    }

    public TextureRegion dashIcon() { return dashIcon; }
    public TextureRegion attackIcon() { return attackIcon; }
    public TextureRegion keyF() { return keyF; }
    public TextureRegion keyBlank() { return keyBlank; }

    @Override public void dispose() {
        dashIconTex.dispose();
        attackIconTex.dispose();
        keyFTex.dispose();
        keyBlankTex.dispose();
    }
}

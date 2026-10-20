package io.github.some_example_name.asset;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import io.github.some_example_name.config.AssetPaths;

/**
 * 본부 판매 상점 UI 팩(hq_sales_storage_v1) 텍스처. Nearest 필터, GameAssets가 소유·해제.
 * 패널은 통짜 프레임 하나로 취급하고(시트 분할 금지), 항목별 Texture 생성·프레임 로딩을 하지 않는다.
 */
public final class HqSalesAssets implements Disposable {

    private final Texture panelTex, buttonTex, signTex, coin64Tex, coin32Tex, slotTex, selectedTex;
    private final TextureRegion panel, button, saleSign, coin64, coin32, slot, selectedBorder;

    public HqSalesAssets() {
        panelTex = GameAssets.loadTile(AssetPaths.HQ_SALES_PANEL);
        buttonTex = GameAssets.loadTile(AssetPaths.HQ_SALES_BUTTON);
        signTex = GameAssets.loadTile(AssetPaths.HQ_SALES_SIGN);
        coin64Tex = GameAssets.loadTile(AssetPaths.HQ_SALES_COIN_64);
        coin32Tex = GameAssets.loadTile(AssetPaths.HQ_SALES_COIN_32);
        slotTex = GameAssets.loadTile(AssetPaths.HQ_SALES_SLOT);
        selectedTex = GameAssets.loadTile(AssetPaths.HQ_SALES_SELECTED);
        panel = new TextureRegion(panelTex);
        button = new TextureRegion(buttonTex);
        saleSign = new TextureRegion(signTex);
        coin64 = new TextureRegion(coin64Tex);
        coin32 = new TextureRegion(coin32Tex);
        slot = new TextureRegion(slotTex);
        selectedBorder = new TextureRegion(selectedTex);
    }

    public TextureRegion panel() { return panel; }
    public TextureRegion button() { return button; }
    public TextureRegion saleSign() { return saleSign; }
    public TextureRegion coin64() { return coin64; }
    public TextureRegion coin32() { return coin32; }
    public TextureRegion slot() { return slot; }
    public TextureRegion selectedBorder() { return selectedBorder; }

    @Override public void dispose() {
        panelTex.dispose();
        buttonTex.dispose();
        signTex.dispose();
        coin64Tex.dispose();
        coin32Tex.dispose();
        slotTex.dispose();
        selectedTex.dispose();
    }
}

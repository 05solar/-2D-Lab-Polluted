package io.github.some_example_name.asset;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import io.github.some_example_name.config.AssetPaths;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * 플레이어 HP HUD · 8칸 가방 UI 텍스처와 배치 좌표({@link HudLayout})를 로드한다.
 * 텍스처는 {@link GameAssets}가 소유·해제하는 체계를 따른다(여기서 로드, GameAssets가 dispose).
 * 모든 텍스처는 Nearest 필터(픽셀 아트 선명).
 */
public final class PlayerHudAssets implements Disposable {

    private final HudLayout layout;
    private final Texture frameTex, fillHealthyTex, fillWarningTex, fillCriticalTex,
        backpackTex, slotEmptyTex, slotHoverTex, rPromptTex;
    private final TextureRegion frame, fillHealthy, fillWarning, fillCritical,
        backpack, slotEmpty, slotHover, rPrompt;
    private final Texture backpackFullTex;
    private final TextureRegion backpackFullLabel;

    public PlayerHudAssets() {
        layout = HudLayout.fromJson(Gdx.files.internal(AssetPaths.HUD_LAYOUT_JSON).readString("UTF-8"));
        frameTex = GameAssets.loadTile(AssetPaths.HUD_FRAME);
        fillHealthyTex = GameAssets.loadTile(AssetPaths.HUD_FILL_HEALTHY);
        fillWarningTex = GameAssets.loadTile(AssetPaths.HUD_FILL_WARNING);
        fillCriticalTex = GameAssets.loadTile(AssetPaths.HUD_FILL_CRITICAL);
        backpackTex = GameAssets.loadTile(AssetPaths.HUD_BACKPACK_8SLOTS);
        slotEmptyTex = GameAssets.loadTile(AssetPaths.HUD_SLOT_EMPTY);
        slotHoverTex = GameAssets.loadTile(AssetPaths.HUD_SLOT_HOVER);
        rPromptTex = GameAssets.loadTile(AssetPaths.HUD_R_PROMPT);
        frame = new TextureRegion(frameTex);
        fillHealthy = new TextureRegion(fillHealthyTex);
        fillWarning = new TextureRegion(fillWarningTex);
        fillCritical = new TextureRegion(fillCriticalTex);
        backpack = new TextureRegion(backpackTex);
        slotEmpty = new TextureRegion(slotEmptyTex);
        slotHover = new TextureRegion(slotHoverTex);
        rPrompt = new TextureRegion(rPromptTex);
        backpackFullTex = createKoreanLabel("가방이 가득 찼습니다");
        backpackFullLabel = new TextureRegion(backpackFullTex);
    }

    /** 기본 비트맵 폰트에 한글이 없어, 본부 라벨과 동일하게 가득참 알림을 별도 래스터화한다. */
    private static Texture createKoreanLabel(String text) {
        BufferedImage image = new BufferedImage(360, 56, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        java.awt.FontMetrics fm = g.getFontMetrics();
        int x = (image.getWidth() - fm.stringWidth(text)) / 2;
        int y = (image.getHeight() - fm.getHeight()) / 2 + fm.getAscent();
        g.setColor(new Color(10, 12, 16, 235));
        for (int ox = -2; ox <= 2; ox++) for (int oy = -2; oy <= 2; oy++)
            if (ox != 0 || oy != 0) g.drawString(text, x + ox, y + oy);
        g.setColor(new Color(255, 196, 120));
        g.drawString(text, x, y);
        g.dispose();
        Pixmap pixmap = new Pixmap(image.getWidth(), image.getHeight(), Pixmap.Format.RGBA8888);
        for (int py = 0; py < image.getHeight(); py++)
            for (int px = 0; px < image.getWidth(); px++) {
                int argb = image.getRGB(px, py);
                pixmap.setColor(((argb >>> 16) & 255) / 255f, ((argb >>> 8) & 255) / 255f,
                    (argb & 255) / 255f, ((argb >>> 24) & 255) / 255f);
                pixmap.drawPixel(px, py);
            }
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pixmap.dispose();
        return texture;
    }

    public HudLayout layout() { return layout; }
    public TextureRegion backpackFullLabel() { return backpackFullLabel; }
    public TextureRegion frame() { return frame; }
    public TextureRegion backpackPanel() { return backpack; }
    public TextureRegion slotHover() { return slotHover; }
    public TextureRegion slotEmpty() { return slotEmpty; }
    public TextureRegion rPrompt() { return rPrompt; }

    /** 현재 HP에 맞는 채움 색(>healthyAbove healthy, >warningAbove warning, 그 이하 critical). */
    public TextureRegion fillFor(int hp) {
        if (hp > layout.healthyAbove) return fillHealthy;
        if (hp > layout.warningAbove) return fillWarning;
        return fillCritical;
    }

    @Override public void dispose() {
        frameTex.dispose(); fillHealthyTex.dispose(); fillWarningTex.dispose();
        fillCriticalTex.dispose(); backpackTex.dispose(); slotEmptyTex.dispose();
        slotHoverTex.dispose(); rPromptTex.dispose();
        backpackFullTex.dispose();
    }
}

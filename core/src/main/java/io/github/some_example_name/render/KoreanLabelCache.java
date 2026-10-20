package io.github.some_example_name.render;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * 기본 비트맵 폰트에 없는 한글 텍스트를 AWT로 래스터화해 캐시한다(본부 라벨과 동일 방식).
 * 아이템 이름+금액 토스트처럼 종류가 유한한 동적 문자열에 쓴다. 텍스처는 여기서 소유·해제.
 */
public final class KoreanLabelCache implements Disposable {

    private final Map<String, TextureRegion> labels = new HashMap<>();
    private final int fontSize;

    public KoreanLabelCache(int fontSize) { this.fontSize = fontSize; }

    public TextureRegion label(String text) {
        TextureRegion cached = labels.get(text);
        if (cached != null) return cached;
        TextureRegion created = new TextureRegion(rasterize(text, fontSize));
        labels.put(text, created);
        return created;
    }

    private static Texture rasterize(String text, int size) {
        Font font = koreanFont(text, size);
        BufferedImage measure = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D mg = measure.createGraphics();
        mg.setFont(font);
        FontMetrics metrics = mg.getFontMetrics();
        int width = metrics.stringWidth(text) + 10;
        int height = metrics.getHeight() + 8;
        mg.dispose();
        measure.flush();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        int y = 4 + fm.getAscent();
        g.setColor(new java.awt.Color(5, 8, 14, 235));
        g.drawString(text, 6, y + 1);
        g.setColor(java.awt.Color.WHITE);
        g.drawString(text, 5, y);
        g.dispose();
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        for (int py = 0; py < image.getHeight(); py++)
            for (int px = 0; px < image.getWidth(); px++) {
                int argb = image.getRGB(px, py);
                pixmap.setColor(((argb >>> 16) & 255) / 255f, ((argb >>> 8) & 255) / 255f,
                    (argb & 255) / 255f, ((argb >>> 24) & 255) / 255f);
                pixmap.drawPixel(px, py);
            }
        image.flush();
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return texture;
    }

    private static Font koreanFont(String text, int size) {
        // Mona 픽셀 폰트 우선(OFL), 글리프 누락 시 시스템 한글 폰트 폴백.
        return io.github.some_example_name.asset.GameFonts.awtBold(text, size);
    }

    @Override public void dispose() {
        for (TextureRegion region : labels.values()) region.getTexture().dispose();
        labels.clear();
    }
}

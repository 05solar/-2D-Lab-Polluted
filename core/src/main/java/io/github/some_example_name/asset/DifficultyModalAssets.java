package io.github.some_example_name.asset;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import io.github.some_example_name.config.AssetPaths;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/** Owns the supplied transparent panel and pre-rasterized Korean UI labels. */
public final class DifficultyModalAssets implements Disposable {
    public static final class Text {
        public final Texture texture;
        public final int width, height;
        Text(Texture texture, int width, int height) {
            this.texture = texture; this.width = width; this.height = height;
        }
    }

    private final Texture panel;
    private final TextureRegion panelRegion;
    private final DifficultyModalLayout layout;
    private final Map<String, Text> labels = new HashMap<>();

    public DifficultyModalAssets() {
        String json = Gdx.files.internal(AssetPaths.DIFFICULTY_MODAL_LAYOUT).readString("UTF-8");
        JsonValue root = new JsonReader().parse(json);
        int[] size = root.get("size").asIntArray();
        if (size.length != 2 || size[0] != 960 || size[1] != 640
            || !"top-left".equals(root.getString("coordinateOrigin")))
            throw new IllegalStateException("unexpected difficulty modal metadata");
        panel = new Texture(Gdx.files.internal(AssetPaths.DIFFICULTY_MODAL_IMAGE));
        if (panel.getWidth() != 960 || panel.getHeight() != 640)
            throw new IllegalStateException("difficulty modal PNG must be 960x640");
        panel.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        panel.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
        panelRegion = new TextureRegion(panel);
        layout = DifficultyModalLayout.fromJson(json);
        // 몬스터 수 설명 라벨은 제거했다(2026-10-15 요청). 난이도 이름만 표시한다.
        add("title", "연구소 난이도 선택", 36);
        add("easy", "쉬움", 34);
        add("normal", "보통", 34);
        add("hard", "어려움", 34);
        add("footer", "Enter: 입장  ·  Esc: 취소", 22);
    }

    private void add(String key, String value, int size) { labels.put(key, rasterize(value, size)); }
    public TextureRegion panel() { return panelRegion; }
    public DifficultyModalLayout layout() { return layout; }
    public Text text(String key) {
        Text text = labels.get(key);
        if (text == null) throw new IllegalArgumentException("unknown modal text: " + key);
        return text;
    }

    private static Text rasterize(String value, int size) {
        Font font = koreanFont(value, size);
        BufferedImage measure = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D mg = measure.createGraphics();
        mg.setFont(font);
        FontMetrics metrics = mg.getFontMetrics();
        int width = metrics.stringWidth(value) + 8;
        int height = metrics.getHeight() + 8;
        mg.dispose(); measure.flush();

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        int y = 4 + fm.getAscent();
        g.setColor(new java.awt.Color(5, 8, 14, 235));
        g.drawString(value, 5, y + 1);
        g.setColor(java.awt.Color.WHITE);
        g.drawString(value, 4, y);
        g.dispose();

        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        for (int py = 0; py < height; py++) for (int px = 0; px < width; px++) {
            int argb = image.getRGB(px, py);
            pixmap.setColor(((argb >>> 16) & 255) / 255f, ((argb >>> 8) & 255) / 255f,
                (argb & 255) / 255f, ((argb >>> 24) & 255) / 255f);
            pixmap.drawPixel(px, py);
        }
        image.flush();
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return new Text(texture, width, height);
    }

    private static Font koreanFont(String value, int size) {
        String[] candidates = {"Malgun Gothic", "맑은 고딕", "Gulim", "Dialog"};
        for (String family : candidates) {
            Font font = new Font(family, Font.BOLD, size);
            if (font.canDisplayUpTo(value) == -1) return font;
        }
        throw new IllegalStateException("No installed font can render Korean modal labels");
    }

    @Override public void dispose() {
        panel.dispose();
        for (Text text : labels.values()) text.texture.dispose();
    }
}

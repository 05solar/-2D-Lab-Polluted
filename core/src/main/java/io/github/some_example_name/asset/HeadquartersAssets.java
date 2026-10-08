package io.github.some_example_name.asset;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import io.github.some_example_name.config.AssetPaths;
import java.util.HashMap;
import java.util.Map;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/** Loads original individual HQ PNGs according to the supplied atlas catalog metadata. */
public final class HeadquartersAssets implements Disposable {
    private final Map<String, Texture> textures = new HashMap<>();
    private final Map<String, TextureRegion> tiles = new HashMap<>();
    private final Map<String, TextureRegion> furniture = new HashMap<>();
    private final Map<String, TextureRegion> vending = new HashMap<>();
    private final float idleFrameDuration;
    private final Texture promptTexture;
    private final TextureRegion[] promptFrames;
    private final Texture promptLabelTexture;
    private final TextureRegion promptLabel;
    private final Texture healthPromptLabelTexture;
    private final TextureRegion healthPromptLabel;

    HeadquartersAssets() {
        JsonValue root = new JsonReader().parse(Gdx.files.internal(AssetPaths.HQ_JSON));
        if (!"top-left".equals(root.getString("origin")))
            throw new IllegalStateException("unexpected HQ atlas origin");
        JsonValue atlases = root.get("atlases");
        JsonValue tileMeta = atlases.get("tiles");
        JsonValue furnitureMeta = atlases.get("furniture");
        JsonValue vendingMeta = atlases.get("vendingMachine");
        requireSize(tileMeta.get("cell"), 64, 64);
        requireSize(furnitureMeta.get("cell"), 128, 96);
        requireSize(vendingMeta.get("frame"), 96, 128);
        if (!"bottom-center".equals(furnitureMeta.getString("anchor")) ||
            !"bottom-center".equals(vendingMeta.getString("anchor")))
            throw new IllegalStateException("unexpected HQ prop anchor");
        load(tileMeta.get("items"), "tiles/", 64, 64, tiles);
        load(furnitureMeta.get("items"), "furniture/", 128, 96, furniture);
        load(vendingMeta.get("frames"), "vending_machine/", 96, 128, vending);
        idleFrameDuration = vendingMeta.get("animations").get("idle").getFloat("frameDuration");

        JsonValue promptMeta = new JsonReader().parse(
            Gdx.files.internal(AssetPaths.SPACE_PROMPT_JSON));
        requireSize(promptMeta.get("size"), 384, 64);
        requireSize(promptMeta.get("frame"), 128, 64);
        if (promptMeta.getInt("columns") != 3 || promptMeta.getInt("rows") != 1
            || !"top-left".equals(promptMeta.getString("origin")))
            throw new IllegalStateException("unexpected space prompt sheet metadata");
        promptTexture = GameAssets.loadTile(AssetPaths.SPACE_PROMPT_IMAGE);
        if (promptTexture.getWidth() != 384 || promptTexture.getHeight() != 64)
            throw new IllegalStateException("space prompt image must be 384x64");
        TextureRegion[][] promptGrid = TextureRegion.split(promptTexture, 128, 64);
        promptFrames = new TextureRegion[]{promptGrid[0][0], promptGrid[0][1], promptGrid[0][2]};
        promptLabelTexture = createKoreanPromptLabel("상점 열기");
        promptLabel = new TextureRegion(promptLabelTexture);
        healthPromptLabelTexture = createKoreanPromptLabel("체력 회복");
        healthPromptLabel = new TextureRegion(healthPromptLabelTexture);
    }

    private static void requireSize(JsonValue value, int width, int height) {
        int[] pair = value.asIntArray();
        if (pair.length != 2 || pair[0] != width || pair[1] != height)
            throw new IllegalStateException("HQ asset dimensions do not match metadata");
    }

    private void load(JsonValue names, String folder, int width, int height,
                      Map<String, TextureRegion> output) {
        for (String id : names.asStringArray()) {
            Texture texture = GameAssets.loadTile(AssetPaths.HQ_DIR + folder + id + ".png");
            if (texture.getWidth() != width || texture.getHeight() != height)
                throw new IllegalStateException("HQ PNG size: " + id);
            textures.put(folder + id, texture);
            output.put(id, new TextureRegion(texture));
        }
    }

    public TextureRegion tile(String id) { return require(tiles, id); }
    public TextureRegion furniture(String id) { return require(furniture, id); }
    public TextureRegion vending(String id) { return require(vending, id); }
    public TextureRegion promptFrame(int index) { return promptFrames[index]; }
    public TextureRegion promptLabel() { return promptLabel; }
    public TextureRegion healthPromptLabel() { return healthPromptLabel; }
    public float idleFrameDuration() { return idleFrameDuration; }

    /** Rasterizes the Korean label separately from the supplied keycap art. */
    private static Texture createKoreanPromptLabel(String text) {
        BufferedImage image = new BufferedImage(112, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        FontMetrics metrics = graphics.getFontMetrics();
        int x = (image.getWidth() - metrics.stringWidth(text)) / 2;
        int y = (image.getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
        graphics.setColor(new java.awt.Color(12, 14, 18, 230));
        for (int ox = -1; ox <= 1; ox++) for (int oy = -1; oy <= 1; oy++)
            if (ox != 0 || oy != 0) graphics.drawString(text, x + ox, y + oy);
        graphics.setColor(java.awt.Color.WHITE);
        graphics.drawString(text, x, y);
        graphics.dispose();

        Pixmap pixmap = new Pixmap(image.getWidth(), image.getHeight(), Pixmap.Format.RGBA8888);
        for (int py = 0; py < image.getHeight(); py++) {
            for (int px = 0; px < image.getWidth(); px++) {
                int rgba = image.getRGB(px, py);
                pixmap.setColor(((rgba >>> 16) & 255) / 255f, ((rgba >>> 8) & 255) / 255f,
                    (rgba & 255) / 255f, ((rgba >>> 24) & 255) / 255f);
                pixmap.drawPixel(px, py);
            }
        }
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        texture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
        pixmap.dispose();
        return texture;
    }

    private static TextureRegion require(Map<String, TextureRegion> regions, String id) {
        TextureRegion value = regions.get(id);
        if (value == null) throw new IllegalArgumentException("unknown HQ asset: " + id);
        return value;
    }

    @Override public void dispose() {
        for (Texture texture : textures.values()) texture.dispose();
        promptTexture.dispose();
        promptLabelTexture.dispose();
        healthPromptLabelTexture.dispose();
    }
}

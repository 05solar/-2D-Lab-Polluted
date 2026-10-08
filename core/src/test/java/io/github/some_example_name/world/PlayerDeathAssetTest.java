package io.github.some_example_name.world;

import static org.junit.Assert.*;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.imageio.ImageIO;
import org.junit.Test;

/** Verifies the supplied death sheet and metadata without a GL context. */
public class PlayerDeathAssetTest {
    @Test public void deathSheetAndDirectionalClipsMatchTheDeclaredGrid() throws IOException {
        File assets = new File("assets");
        if (!assets.isDirectory()) assets = new File("../assets");
        File png = new File(assets, "textures/player/player_scout_death_sheet_64.png");
        BufferedImage image = ImageIO.read(png);
        assertNotNull(png.getAbsolutePath(), image);
        assertEquals(512, image.getWidth());
        assertEquals(256, image.getHeight());
        assertTrue("death art preserves transparency", image.getColorModel().hasAlpha());
        assertEquals("top-left remains transparent", 0, image.getRGB(0, 0) >>> 24);
        boolean opaque = false, transparent = false;
        for (int y = 0; y < image.getHeight() && !(opaque && transparent); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int alpha = image.getRGB(x, y) >>> 24;
                opaque |= alpha > 0;
                transparent |= alpha == 0;
                if (opaque && transparent) break;
            }
        }
        assertTrue(opaque);
        assertTrue(transparent);
        File json = new File(assets, "data/player_scout_death_animations.json");
        JsonValue root = new JsonReader().parse(new String(Files.readAllBytes(json.toPath()),
            StandardCharsets.UTF_8));
        assertEquals("top-left", root.getString("origin"));
        assertEquals(64, root.getInt("frameWidth"));
        assertEquals(64, root.getInt("frameHeight"));
        for (String direction : new String[]{"down", "left", "right", "up"}) {
            JsonValue clip = root.get("animations").get("death_" + direction);
            assertNotNull("death_" + direction, clip);
            assertEquals(directionRow(direction), clip.getInt("row"));
            assertEquals(0.1f, clip.getFloat("frameDuration"), 0.0001f);
            assertFalse(clip.getBoolean("loop"));
            assertEquals(8, clip.get("frames").asIntArray().length);
            assertTrue(clip.getBoolean("holdLastFrame"));
        }
    }

    private static int directionRow(String direction) {
        if ("down".equals(direction)) return 0;
        if ("left".equals(direction)) return 1;
        if ("right".equals(direction)) return 2;
        return 3;
    }
}

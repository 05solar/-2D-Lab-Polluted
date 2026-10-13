package io.github.some_example_name.asset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.imageio.ImageIO;
import org.junit.Test;

public class DifficultyModalLayoutTest {
    private static File findAsset(String relative) {
        File dir = new File(".").getAbsoluteFile();
        for (int i = 0; i < 6 && dir != null; i++, dir = dir.getParentFile()) {
            File file = new File(dir, relative);
            if (file.isFile()) return file;
        }
        throw new IllegalStateException("asset not found: " + relative);
    }

    private static DifficultyModalLayout layout() throws IOException {
        File json = findAsset("assets/difficulty_modal_v2/layout.json");
        return DifficultyModalLayout.fromJson(new String(Files.readAllBytes(json.toPath()),
            StandardCharsets.UTF_8));
    }

    @Test public void centersPanelWithAspectAndSafeMarginsAtRequestedSizes() {
        DifficultyModalLayout.Rect full = DifficultyModalLayout.panel(1280, 960);
        assertEquals(1.04f, DifficultyModalLayout.scale(1280, 960), .001f);
        assertEquals(140.8f, full.x, .1f);
        assertEquals(147.2f, full.y, .1f);
        assertEquals(998.4f, full.width, .1f);
        assertEquals(665.6f, full.height, .1f);

        DifficultyModalLayout.Rect medium = DifficultyModalLayout.panel(1280, 720);
        assertEquals(.9225f, DifficultyModalLayout.scale(1280, 720), .001f);
        assertEquals(197.2f, medium.x, .1f);
        assertEquals(64.8f, medium.y, .1f);

        DifficultyModalLayout.Rect small = DifficultyModalLayout.panel(960, 540);
        assertEquals(.691875f, DifficultyModalLayout.scale(960, 540), .001f);
        assertEquals(147.9f, small.x, .1f);
        assertEquals(48.6f, small.y, .1f);
    }

    @Test public void jsonOptionRectsConvertTopLeftCoordinatesOnceAndHitSameRow() throws IOException {
        DifficultyModalLayout layout = layout();
        assertEquals("EASY", layout.optionId(0));
        assertEquals("NORMAL", layout.optionId(1));
        assertEquals("HARD", layout.optionId(2));
        DifficultyModalLayout.Rect panel = DifficultyModalLayout.panel(1280, 960);
        DifficultyModalLayout.Rect easy = layout.optionRect(0, 1280, 960);
        assertEquals(panel.x + 110f * 1.04f, easy.x, .01f);
        assertEquals(panel.y + (640f - 170f - 100f) * 1.04f, easy.y, .01f);
        assertTrue(easy.contains(easy.x, easy.y));
        assertTrue(easy.contains(easy.x + easy.width, easy.y + easy.height));
        assertFalse(easy.contains(easy.x - .1f, easy.y));
        assertFalse(easy.contains(easy.x, easy.y - .1f));
        assertTrue(layout.optionRect(2, 960, 540).y < layout.optionRect(1, 960, 540).y);
        assertTrue(layout.optionRect(1, 960, 540).y < layout.optionRect(0, 960, 540).y);
    }

    @Test public void suppliedPanelHasAlphaAndExpectedRuntimeDimensions() throws IOException {
        BufferedImage image = ImageIO.read(findAsset(
            "assets/difficulty_modal_v2/runtime/difficulty_modal_960x640.png"));
        assertEquals(960, image.getWidth());
        assertEquals(640, image.getHeight());
        assertTrue(image.getColorModel().hasAlpha());
        assertEquals(0, image.getRGB(0, 0) >>> 24);
        assertTrue((image.getRGB(480, 319) >>> 24) > 0);
    }
}

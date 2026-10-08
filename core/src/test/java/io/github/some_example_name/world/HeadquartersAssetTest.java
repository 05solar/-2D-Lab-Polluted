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

/** ImageIO/JSON checks of the supplied source pack; no GL context needed. */
public class HeadquartersAssetTest {
    @Test public void everyCatalogItemIsOriginalSizeAndTransparentArtKeepsAlpha() throws IOException {
        File root = new File("assets/hq_tent");
        if (!root.isDirectory()) root = new File("../assets/hq_tent");
        assertTrue(root.getAbsolutePath(), root.isDirectory());
        JsonValue data = new JsonReader().parse(new String(Files.readAllBytes(
            new File(root, "hq_tent_assets.json").toPath()), StandardCharsets.UTF_8));
        assertEquals("top-left", data.getString("origin"));
        JsonValue atlases = data.get("atlases");
        int checked = 0;
        for (String group : new String[]{"tiles", "furniture", "vendingMachine"}) {
            JsonValue section = atlases.get(group);
            String subdir = group.equals("vendingMachine") ? "vending_machine" : group;
            int[] size = section.get(group.equals("vendingMachine") ? "frame" : "cell")
                .asIntArray();
            JsonValue names = section.get(group.equals("vendingMachine") ? "frames" : "items");
            for (String id : names.asStringArray()) {
                BufferedImage image = ImageIO.read(new File(root, subdir + "/" + id + ".png"));
                assertNotNull(id, image);
                assertEquals(id, size[0], image.getWidth());
                assertEquals(id, size[1], image.getHeight());
                // The supplied tent tiles are opaque RGB art; furniture/vending are RGBA.
                if (!group.equals("tiles"))
                    assertTrue(id + " alpha channel", image.getColorModel().hasAlpha());
                checked++;
            }
        }
        assertEquals(34, checked);
        assertEquals("bottom-center", atlases.get("furniture").getString("anchor"));
        assertEquals("bottom-center", atlases.get("vendingMachine").getString("anchor"));
        assertEquals(0.45f, atlases.get("vendingMachine").get("animations")
            .get("idle").getFloat("frameDuration"), 0.0001f);
    }

    @Test public void spacePromptSheetHasThreeTransparent128By64States() throws IOException {
        File root = new File("assets/ui/interaction");
        if (!root.isDirectory()) root = new File("../assets/ui/interaction");
        File png = new File(root, "spacebar_prompt_sheet_128x64.png");
        BufferedImage image = ImageIO.read(png);
        assertNotNull(png.getAbsolutePath(), image);
        assertEquals(384, image.getWidth());
        assertEquals(64, image.getHeight());
        assertTrue(image.getColorModel().hasAlpha());
        assertEquals(0, image.getRGB(0, 0) >>> 24);
        JsonValue json = new JsonReader().parse(new String(Files.readAllBytes(
            new File(root, "spacebar_prompt.json").toPath()), StandardCharsets.UTF_8));
        assertEquals("top-left", json.getString("origin"));
        assertEquals(3, json.getInt("columns"));
        assertEquals(1, json.getInt("rows"));
        assertEquals(0, json.get("states").getInt("idle"));
        assertEquals(1, json.get("states").getInt("interactable"));
        assertEquals(2, json.get("states").getInt("pressed"));
    }
}

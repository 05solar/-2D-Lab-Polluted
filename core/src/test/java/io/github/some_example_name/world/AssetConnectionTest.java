package io.github.some_example_name.world;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.Before;
import org.junit.Test;

/** 알파 바운딩 박스가 아닌 금속 본체의 실제 셀 경계 접합 단면을 검사한다. */
public class AssetConnectionTest {
    private LaboratoryTileCatalogV2 catalog;
    private Map<String, BufferedImage> images;

    @Before
    public void load() throws IOException {
        File dir = new File("assets/laboratory_tiles_v2");
        if (!dir.isDirectory()) dir = new File("../assets/laboratory_tiles_v2");
        assertTrue(dir.getAbsolutePath(), dir.isDirectory());
        catalog = LaboratoryTileCatalogV2.fromJson(new String(Files.readAllBytes(
            new File(dir, "laboratory_tiles_v2.json").toPath()), StandardCharsets.UTF_8));
        images = new HashMap<>();
        for (LaboratoryTileCatalogV2.AtlasDef atlas : catalog.atlases())
            images.put(atlas.name, ImageIO.read(new File(dir, atlas.file)));
    }

    @Test
    public void everyConnectorHasOpaqueBodyAtExactSameEdgeProfile() {
        BufferedImage h = tile("wall_horizontal");
        BufferedImage v = tile("wall_vertical");
        int checked = 0;
        for (String atlas : new String[]{"wall", "structure", "vertical_jamb"}) {
            for (String id : catalog.atlas(atlas).ids) {
                int mask;
                try { mask = WallAutotiler.connectors(id); }
                catch (IllegalArgumentException unused) { continue; } // 비사용 소품/파손 벽은 접합 대상 아님
                BufferedImage image = tile(id);
                assertNotNull(image);
                if ((mask & WallAutotiler.WEST) != 0) {
                    assertHorizontalEdge(h, image, 0, id);
                    checked++;
                }
                if ((mask & WallAutotiler.EAST) != 0) {
                    assertHorizontalEdge(h, image, 63, id);
                    checked++;
                }
                if ((mask & WallAutotiler.NORTH) != 0) {
                    assertVerticalEdge(v, image, 0, id);
                    checked++;
                }
                if ((mask & WallAutotiler.SOUTH) != 0) {
                    assertVerticalEdge(v, image, 63, id);
                    checked++;
                }
            }
        }
        assertTrue("방향별 벽·문 접합 검사 수", checked >= 45);
    }

    private static void assertHorizontalEdge(BufferedImage reference, BufferedImage image, int x, String id) {
        int solid = 0;
        for (int y = 18; y <= 49; y++) {
            assertEquals(id + " 가로 접합 x=" + x + " y=" + y,
                reference.getRGB(0, y), image.getRGB(x, y));
            if ((image.getRGB(x, y) >>> 24) >= 200) solid++;
        }
        assertTrue(id + " 금속 본체가 가로 경계에 닿음", solid >= 25);
    }

    private static void assertVerticalEdge(BufferedImage reference, BufferedImage image, int y, String id) {
        int solid = 0;
        for (int x = 19; x <= 47; x++) {
            assertEquals(id + " 세로 접합 y=" + y + " x=" + x,
                reference.getRGB(x, 0), image.getRGB(x, y));
            if ((image.getRGB(x, y) >>> 24) >= 200) solid++;
        }
        assertTrue(id + " 금속 본체가 세로 경계에 닿음", solid >= 22);
    }

    @Test
    public void openDoorExposesFloorAndKeepsThreshold() {
        for (String id : new String[]{"horizontal_door_open", "vertical_door_open"}) {
            BufferedImage image = tile(id);
            assertEquals(id + " 개구부 가운데 투명", 0, image.getRGB(32, 32) >>> 24);
            int thresholdY = id.startsWith("horizontal") ? 63 : 53;
            assertTrue(id + " 문턱 픽셀 존재", (image.getRGB(32, thresholdY) >>> 24) > 0);
        }
    }

    private BufferedImage tile(String id) {
        LaboratoryTileCatalogV2.TileDef def = catalog.tile(id);
        BufferedImage atlas = images.get(def.atlas);
        return atlas.getSubimage(def.column * 64, def.row * 64, 64, 64);
    }
}

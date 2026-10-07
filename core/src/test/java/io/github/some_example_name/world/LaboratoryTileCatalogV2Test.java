package io.github.some_example_name.world;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Before;
import org.junit.Test;

/**
 * Laboratory Tileset V2 에셋/카탈로그 검증(순수 Java, GL 불필요):
 * 아틀라스 크기(JSON + 실제 PNG IHDR), 타일 수(24/16/16/24), ID↔인덱스, 충돌/태그, 누락 없음.
 */
public class LaboratoryTileCatalogV2Test {

    private File assetDir;
    private LaboratoryTileCatalogV2 catalog;

    @Before
    public void setUp() throws IOException {
        assetDir = findAssetDir();
        assertNotNull("laboratory_tiles_v2 에셋 디렉터리를 찾을 수 없음", assetDir);
        String json = new String(Files.readAllBytes(
            new File(assetDir, "laboratory_tiles_v2.json").toPath()), StandardCharsets.UTF_8);
        catalog = LaboratoryTileCatalogV2.fromJson(json);
    }

    private static File findAssetDir() {
        File dir = new File(".").getAbsoluteFile();
        for (int i = 0; i < 6 && dir != null; i++) {
            File cand = new File(dir, "assets/laboratory_tiles_v2");
            if (new File(cand, "laboratory_tiles_v2.json").isFile()) return cand;
            dir = dir.getParentFile();
        }
        return null;
    }

    @Test
    public void atlasCountsAndGridMatch() {
        assertEquals(24, catalog.atlas("floor").ids.size());
        assertEquals(16, catalog.atlas("wall").ids.size());
        assertEquals(16, catalog.atlas("structure").ids.size());
        assertEquals(24, catalog.atlas("overlay").ids.size());
        for (String name : new String[]{"floor", "wall", "structure", "overlay"}) {
            assertTrue(name + " 타일 수 = 열×행", catalog.atlasCountMatchesGrid(name));
        }
    }

    @Test
    public void declaredAtlasSizes() {
        assertSize("floor", 384, 256, 6, 4);
        assertSize("wall", 256, 256, 4, 4);
        assertSize("structure", 256, 256, 4, 4);
        assertSize("overlay", 384, 256, 6, 4);
    }

    private void assertSize(String name, int w, int h, int cols, int rows) {
        LaboratoryTileCatalogV2.AtlasDef a = catalog.atlas(name);
        assertEquals(name + " width", w, a.width);
        assertEquals(name + " height", h, a.height);
        assertEquals(name + " columns", cols, a.columns);
        assertEquals(name + " rows", rows, a.rows);
    }

    @Test
    public void pngDimensionsMatchJson() throws IOException {
        for (LaboratoryTileCatalogV2.AtlasDef a : catalog.atlases()) {
            int[] wh = pngSize(new File(assetDir, a.file));
            assertEquals(a.file + " PNG width", a.width, wh[0]);
            assertEquals(a.file + " PNG height", a.height, wh[1]);
            assertEquals(a.file + " 64px 격자 열수", a.width / 64, a.columns);
            assertEquals(a.file + " 64px 격자 행수", a.height / 64, a.rows);
        }
    }

    @Test
    public void idsMapToCorrectIndexAndRowCol() {
        assertTile("floor_clean_a", "floor", 0, 0, 0);
        assertTile("floor_access_hatch", "floor", 23, 5, 3);
        assertTile("wall_horizontal", "wall", 0, 0, 0);
        assertTile("wall_inner_se", "wall", 15, 3, 3);
        assertTile("vertical_door_open", "structure", 11, 3, 2);
        assertTile("electric_sparks", "overlay", 19, 1, 3);
    }

    private void assertTile(String id, String atlas, int index, int col, int row) {
        LaboratoryTileCatalogV2.TileDef d = catalog.tile(id);
        assertNotNull(id + " 정의 존재", d);
        assertEquals(id + " atlas", atlas, d.atlas);
        assertEquals(id + " index", index, d.index);
        assertEquals(id + " col", col, d.column);
        assertEquals(id + " row", row, d.row);
    }

    @Test
    public void collisionAndTagsFromJson() {
        assertTrue("벽은 충돌", catalog.tile("wall_vertical").solid);
        assertTrue("닫힌 문 충돌", catalog.tile("vertical_door_closed").solid);
        org.junit.Assert.assertFalse("열린 문 통과", catalog.tile("vertical_door_open").solid);
        assertTrue("breach 는 상황별 충돌", catalog.tile("wall_breach_horizontal").configurableSolid);
        assertEquals("toxic", catalog.tile("toxic_puddle_small").tag);
        assertEquals("shock", catalog.tile("electric_sparks").tag);
        assertEquals("electric", catalog.tile("cable_horizontal").tag);
        assertEquals("decor", catalog.tile("grime_small").tag);
    }

    @Test
    public void everyIdHasValidGridPosition() {
        for (LaboratoryTileCatalogV2.AtlasDef a : catalog.atlases()) {
            for (String id : a.ids) {
                LaboratoryTileCatalogV2.TileDef d = catalog.tile(id);
                assertNotNull("누락 없음: " + id, d);
                assertTrue(id + " col 범위", d.column >= 0 && d.column < a.columns);
                assertTrue(id + " row 범위", d.row >= 0 && d.row < a.rows);
            }
        }
    }

    @Test
    public void transparentAtlasesHaveAlphaChannel() throws IOException {
        // 벽/구조물/오버레이는 투명 배경 RGBA여야 한다(바닥 아래가 비치도록).
        for (String file : new String[]{"lab_wall_autotiles_v2_64.png",
                "lab_structure_doors_v2_64.png", "lab_overlay_decals_v2_64.png"}) {
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(new File(assetDir, file));
            assertNotNull(file, img);
            assertTrue(file + " 알파 채널 존재", img.getColorModel().hasAlpha());
            int topLeft = img.getRGB(0, 0) >>> 24;
            assertEquals(file + " 좌상단 투명(alpha=0)", 0, topLeft);
            boolean anyOpaque = false, anyTransparent = false;
            for (int y = 0; y < img.getHeight() && !(anyOpaque && anyTransparent); y++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    int a = img.getRGB(x, y) >>> 24;
                    if (a == 0) anyTransparent = true;
                    if (a > 0) anyOpaque = true;
                    if (anyOpaque && anyTransparent) break;
                }
            }
            assertTrue(file + " 투명 픽셀 존재", anyTransparent);
            assertTrue(file + " 불투명 픽셀 존재", anyOpaque);
        }
    }

    @Test
    public void floorAtlasIsOpaqueBase() throws IOException {
        // 바닥 렌더에서 blending을 끄므로 아틀라스 전체가 불투명해야 한다.
        java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(
            new File(assetDir, "lab_floor_tiles_v2_64.png"));
        assertNotNull(img);
        if (img.getColorModel().hasAlpha()) {
            for (int y = 0; y < img.getHeight(); y++)
                for (int x = 0; x < img.getWidth(); x++)
                    assertEquals("바닥은 모든 픽셀 불투명 " + x + "," + y,
                        255, img.getRGB(x, y) >>> 24);
        }
    }

    /** PNG IHDR에서 width/height를 읽는다(순수 Java, 이미지 라이브러리 불필요). */
    private static int[] pngSize(File f) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(f, "r")) {
            raf.seek(16);
            int w = raf.readInt();
            int h = raf.readInt();
            return new int[]{w, h};
        }
    }
}

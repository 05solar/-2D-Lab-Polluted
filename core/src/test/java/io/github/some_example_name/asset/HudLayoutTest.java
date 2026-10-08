package io.github.some_example_name.asset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

/** ui_layout.json 파싱 검증(순수, GL 불필요): HP 규칙과 8칸 좌표가 명세와 일치하는지. */
public class HudLayoutTest {

    private static String readLayout() throws IOException {
        File dir = new File(".").getAbsoluteFile();
        for (int i = 0; i < 6 && dir != null; i++) {
            File cand = new File(dir, "assets/ui/player_hud_inventory_v1/ui_layout.json");
            if (cand.isFile()) return new String(Files.readAllBytes(cand.toPath()), StandardCharsets.UTF_8);
            dir = dir.getParentFile();
        }
        throw new IllegalStateException("ui_layout.json 을 찾을 수 없음");
    }

    @Test public void parsesHealthAndBackpackLayout() throws IOException {
        HudLayout L = HudLayout.fromJson(readLayout());
        assertNotNull(L);
        // health
        assertEquals(100, L.maxHp);
        assertEquals(240f, L.fillW, 0.001f);
        assertEquals(18f, L.fillH, 0.001f);
        assertEquals(134f, L.fillX, 0.001f);
        assertEquals(57f, L.fillY, 0.001f);
        assertEquals(50, L.healthyAbove);
        assertEquals(25, L.warningAbove);
        assertEquals(512f, L.frameW, 0.001f);
        assertEquals(128f, L.frameH, 0.001f);
        // backpack
        assertEquals(8, L.slotCount);
        assertEquals(8, L.slotTopLeft.length);
        assertEquals(960f, L.panelW, 0.001f);
        assertEquals(600f, L.panelH, 0.001f);
        assertEquals(108f, L.slotW, 0.001f);
        assertEquals(72f, L.iconInsetW, 0.001f);
        assertEquals(0.55f, L.dimAlpha, 0.001f);
        // first and last slot top-left per spec
        assertEquals(220f, L.slotTopLeft[0][0], 0.001f);
        assertEquals(214f, L.slotTopLeft[0][1], 0.001f);
        assertEquals(628f, L.slotTopLeft[7][0], 0.001f);
        assertEquals(336f, L.slotTopLeft[7][1], 0.001f);
    }
}

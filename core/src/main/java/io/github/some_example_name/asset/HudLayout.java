package io.github.some_example_name.asset;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

/**
 * ui_layout.json의 배치 좌표/규칙을 담는 순수 데이터(GL/Texture 없음 → 단위 테스트 가능).
 * 좌표 원점은 이미지 왼쪽 위(top-left)다. 화면 변환(y 뒤집기)은 렌더러가 한다.
 */
public final class HudLayout {

    // health
    public final int maxHp;
    public final float marginX, marginY;
    public final float frameW, frameH;
    public final float fillX, fillY, fillW, fillH;          // frame 좌상단 기준
    public final float valueX, valueY, valueW, valueH;      // frame 좌상단 기준
    public final int healthyAbove, warningAbove;

    // backpack
    public final float panelW, panelH;
    public final float slotW, slotH;
    public final float[][] slotTopLeft;                      // [8][2], panel 좌상단 기준
    public final float iconInsetX, iconInsetY, iconInsetW, iconInsetH;
    public final float capacityX, capacityY, capacityW, capacityH;
    public final float viewportFractionX, viewportFractionY;
    public final float dimAlpha;
    public final int slotCount;

    private HudLayout(JsonValue root) {
        JsonValue h = root.get("health");
        maxHp = h.getInt("maxHp");
        float[] margin = h.get("screenMargin").asFloatArray();
        marginX = margin[0]; marginY = margin[1];
        float[] fs = h.get("frameSize").asFloatArray();
        frameW = fs[0]; frameH = fs[1];
        float[] fr = h.get("fillRectWithinFrame").asFloatArray();
        fillX = fr[0]; fillY = fr[1]; fillW = fr[2]; fillH = fr[3];
        float[] vr = h.get("valueTextRectWithinFrame").asFloatArray();
        valueX = vr[0]; valueY = vr[1]; valueW = vr[2]; valueH = vr[3];
        JsonValue ct = h.get("colorThresholds");
        healthyAbove = ct.getInt("healthyAbove");
        warningAbove = ct.getInt("warningAbove");

        JsonValue b = root.get("backpack");
        float[] ps = b.get("panelSize").asFloatArray();
        panelW = ps[0]; panelH = ps[1];
        float[] ss = b.get("slotSize").asFloatArray();
        slotW = ss[0]; slotH = ss[1];
        JsonValue slots = b.get("slotTopLeftCoordinates");
        int n = slots.size;
        slotTopLeft = new float[n][2];
        int i = 0;
        for (JsonValue s = slots.child; s != null; s = s.next, i++) {
            float[] xy = s.asFloatArray();
            slotTopLeft[i][0] = xy[0];
            slotTopLeft[i][1] = xy[1];
        }
        float[] inset = b.get("itemIconInsetWithinSlot").asFloatArray();
        iconInsetX = inset[0]; iconInsetY = inset[1]; iconInsetW = inset[2]; iconInsetH = inset[3];
        float[] cap = b.get("capacityTextRectWithinPanel").asFloatArray();
        capacityX = cap[0]; capacityY = cap[1]; capacityW = cap[2]; capacityH = cap[3];
        float[] vf = b.get("recommendedMaxViewportFraction").asFloatArray();
        viewportFractionX = vf[0]; viewportFractionY = vf[1];
        dimAlpha = b.getFloat("backgroundDimAlpha");
        slotCount = b.get("grid").getInt("slotCount");
    }

    public static HudLayout fromJson(String json) {
        return new HudLayout(new JsonReader().parse(json));
    }
}

package io.github.some_example_name.world;

import com.badlogic.gdx.math.Rectangle;

/** Art uses bottom-center anchors; collision is only the equipment's ground footprint. */
public final class HeadquartersProp {
    private final String id;
    private final float anchorX, anchorY;
    private final float drawWidth, drawHeight;
    private final Rectangle footprint;

    private HeadquartersProp(String id, float x, float y, float width, float height,
                             float footWidth, float footDepth) {
        this.id = id;
        anchorX = x;
        anchorY = y;
        drawWidth = width;
        drawHeight = height;
        footprint = footWidth == 0f ? null
            : new Rectangle(x - footWidth / 2f, y + 0.08f, footWidth, footDepth);
    }

    /** Footprint sizes are temporary layout data pending measured interaction design. */
    public static HeadquartersProp furniture(String id, float x, float y) {
        float width, depth;
        switch (id) {
            case "command_table": width = 1.65f; depth = 0.45f; break;
            case "communications_console": width = 1.50f; depth = 0.40f; break;
            case "field_medical_cot": width = 1.75f; depth = 0.45f; break;
            case "folding_workbench": width = 1.70f; depth = 0.40f; break;
            case "medical_trolley": width = 1.20f; depth = 0.38f; break;
            case "notice_board": width = 0f; depth = 0f; break; // mounted on wall
            case "portable_generator": width = 1.30f; depth = 0.45f; break;
            case "reinforced_lockers": width = 1.70f; depth = 0.40f; break;
            case "stacked_supply_crates": width = 1.65f; depth = 0.45f; break;
            case "supply_shelving": width = 1.70f; depth = 0.45f; break;
            case "terminal_desk": width = 1.40f; depth = 0.40f; break;
            case "water_dispenser": width = 1.10f; depth = 0.40f; break;
            default: throw new IllegalArgumentException("unknown furniture: " + id);
        }
        return new HeadquartersProp(id, x, y, 2f, 1.5f, width, depth);
    }

    public static HeadquartersProp vending(float x, float y) {
        // JSON suggestedCollision is 56x34px in a 96x128 frame.
        return new HeadquartersProp("vending_machine", x, y, 1.5f, 2f,
            56f / 64f, 34f / 64f);
    }

    /**
     * 북쪽 벽 전용 스킬 테이블(hq_upgrade_tree_v1의 192×128 금속 벤치 아트 재사용 — 회전 금지).
     * 2026-10-20: 강화 트리는 망치 있는 접이식 작업대(folding_workbench)로 옮기고,
     * 이 테이블은 SPACE로 스킬 장착/단축키 등록 창을 연다.
     * 카탈로그 footprint(168×42px)를 월드 배율로 환산한 바닥 점유만 충돌한다(투명 여백 제외).
     */
    public static HeadquartersProp skillTable(float x, float y) {
        float drawWidth = 2.25f, drawHeight = 1.5f; // 192:128 비율 유지
        return new HeadquartersProp("skill_table", x, y, drawWidth, drawHeight,
            drawWidth * (168f / 192f), drawHeight * (42f / 128f));
    }

    public String id() { return id; }
    public float anchorX() { return anchorX; }
    public float anchorY() { return anchorY; }
    public float drawWidth() { return drawWidth; }
    public float drawHeight() { return drawHeight; }
    public Rectangle footprint() { return footprint == null ? null : new Rectangle(footprint); }
}

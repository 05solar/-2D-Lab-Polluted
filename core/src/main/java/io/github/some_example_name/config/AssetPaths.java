package io.github.some_example_name.config;

/** 자산 경로를 한곳에서 관리한다. 화면/엔티티마다 같은 경로 문자열을 반복하지 않는다. */
public final class AssetPaths {

    /** Laboratory Tileset V2 루트(런타임 에셋 기준 상대 경로). 아틀라스 파일명은 JSON에서 읽는다. */
    public static final String TILES_V2_DIR = "laboratory_tiles_v2/";
    public static final String TILES_V2_JSON = TILES_V2_DIR + "laboratory_tiles_v2.json";
    public static final String LAB_SIDE_DOORS = TILES_V2_DIR + "lab_side_doors_2x2_64.png";
    public static final String MONSTER_DIR = "monsters/";

    public static final String HQ_DIR = "hq_tent/";
    public static final String HQ_JSON = HQ_DIR + "hq_tent_assets.json";
    public static final String SPACE_PROMPT_IMAGE = "ui/interaction/spacebar_prompt_sheet_128x64.png";
    public static final String SPACE_PROMPT_JSON = "ui/interaction/spacebar_prompt.json";

    /** 플레이어 HP HUD + 8칸 가방 UI 팩(런타임 PNG + 배치 좌표 JSON). */
    public static final String HUD_DIR = "ui/player_hud_inventory_v1/";
    public static final String HUD_LAYOUT_JSON = HUD_DIR + "ui_layout.json";
    public static final String HUD_FRAME = HUD_DIR + "hp_hud_frame_512x128.png";
    public static final String HUD_FILL_HEALTHY = HUD_DIR + "hp_fill_healthy_240x18.png";
    public static final String HUD_FILL_WARNING = HUD_DIR + "hp_fill_warning_240x18.png";
    public static final String HUD_FILL_CRITICAL = HUD_DIR + "hp_fill_critical_240x18.png";
    public static final String HUD_BACKPACK_8SLOTS = HUD_DIR + "backpack_open_empty_8slots_960x600.png";
    public static final String HUD_SLOT_EMPTY = HUD_DIR + "slot_empty_108.png";
    public static final String HUD_SLOT_HOVER = HUD_DIR + "slot_hover_108.png";
    public static final String HUD_R_PROMPT = HUD_DIR + "r_backpack_prompt_128x64.png";

    public static final String PLAYER_SHEET = "textures/player/player_scout_sheet_64.png";
    public static final String PLAYER_ANIMATION_DATA = "data/player_scout.json";
    public static final String PLAYER_DEATH_SHEET = "textures/player/player_scout_death_sheet_64.png";
    public static final String PLAYER_DEATH_ANIMATION_DATA = "data/player_scout_death_animations.json";

    private AssetPaths() {
    }
}

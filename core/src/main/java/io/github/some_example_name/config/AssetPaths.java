package io.github.some_example_name.config;

/** 자산 경로를 한곳에서 관리한다. 화면/엔티티마다 같은 경로 문자열을 반복하지 않는다. */
public final class AssetPaths {

    /** Laboratory Tileset V2 루트(런타임 에셋 기준 상대 경로). 아틀라스 파일명은 JSON에서 읽는다. */
    public static final String TILES_V2_DIR = "laboratory_tiles_v2/";
    public static final String TILES_V2_JSON = TILES_V2_DIR + "laboratory_tiles_v2.json";
    public static final String LAB_SIDE_DOORS = TILES_V2_DIR + "lab_side_doors_2x2_64.png";
    public static final String MONSTER_DIR = "monsters/";
    public static final String ROOMKITS_DIR = "laboratory_roomkits_v3/";
    public static final String ROOMKITS_JSON = ROOMKITS_DIR + "laboratory_roomkits_v3.json";
    public static final String FURNITURE_V7_DIR = "laboratory_orthographic_views_v7/";
    /** v8은 v7과 front/측면이 동일하고 가구별 뒷모습(_back)을 추가로 제공한다(남쪽 벽용). */
    public static final String FURNITURE_V8_DIR = "laboratory_orthographic_views_v8/";
    public static final String DIFFICULTY_MODAL_DIR = "difficulty_modal_v2/";
    public static final String DIFFICULTY_MODAL_IMAGE = DIFFICULTY_MODAL_DIR
        + "runtime/difficulty_modal_960x640.png";
    public static final String DIFFICULTY_MODAL_LAYOUT = DIFFICULTY_MODAL_DIR + "layout.json";
    public static final String LOOT_DIR = "loot/";
    public static final String LOOT_TABLES = LOOT_DIR + "loot_tables.json";

    public static final String HQ_DIR = "hq_tent/";
    public static final String HQ_JSON = HQ_DIR + "hq_tent_assets.json";
    /** 연구소 외벽(2타일 두께 링·모서리) + 본부↔연구소 대문(닫힘/열림) V3 팩. */
    public static final String EXTERIOR_DIR = "exterior_walls_gate_v3/";
    public static final String EXTERIOR_LAYOUT_JSON = EXTERIOR_DIR + "layout.json";

    /** 연구소 구조 통일 V4: 47종 연결 벽 아틀라스 + 가로/세로 방문(닫힘/열림). */
    public static final String STRUCTURE_V4_DIR = "laboratory_structure_v4/";
    public static final String STRUCTURE_V4_WALL_ATLAS =
        STRUCTURE_V4_DIR + "runtime/walls/walls_blob47_atlas_128.png";
    public static final String STRUCTURE_V4_DOOR_H_CLOSED =
        STRUCTURE_V4_DIR + "runtime/doors/door_horizontal_closed.png";
    public static final String STRUCTURE_V4_DOOR_H_OPEN =
        STRUCTURE_V4_DIR + "runtime/doors/door_horizontal_open.png";
    public static final String STRUCTURE_V4_DOOR_V_CLOSED =
        STRUCTURE_V4_DIR + "runtime/doors/door_vertical_closed.png";
    public static final String STRUCTURE_V4_DOOR_V_OPEN =
        STRUCTURE_V4_DIR + "runtime/doors/door_vertical_open.png";

    /** 미니맵 프레임·발자국·귀환(대문) 버튼 UI 에셋. */
    public static final String NAV_MINIMAP_FRAME = "ui/navigation/minimap_frame_256.png";
    public static final String NAV_FOOTPRINTS = "ui/navigation/footprints_pair_64.png";
    public static final String NAV_RETURN_BUTTON = "ui/navigation/return_route_button_256x64.png";

    public static final String SPACE_PROMPT_IMAGE = "ui/interaction/spacebar_prompt_sheet_128x64.png";
    public static final String SPACE_PROMPT_JSON = "ui/interaction/spacebar_prompt.json";

    /**
     * 전투·전리품 업데이트 V1 팩: 돌진/공격 HUD 아이콘 + 합성 효과음 9종(CC0).
     * 좌표·권장 음량은 ui_catalog.json / audio_catalog.json 참고.
     */
    public static final String COMBAT_PACK_DIR = "combat_loot_update_v1/";
    public static final String COMBAT_UI_KEY_F = COMBAT_PACK_DIR + "runtime/ui/key_f_64.png";
    public static final String COMBAT_UI_KEY_BLANK = COMBAT_PACK_DIR + "runtime/ui/key_blank_64.png";
    public static final String COMBAT_UI_DASH_ICON = COMBAT_PACK_DIR + "runtime/ui/dash_icon_128.png";
    public static final String COMBAT_UI_ATTACK_ICON = COMBAT_PACK_DIR + "runtime/ui/attack_icon_128.png";
    public static final String COMBAT_AUDIO_DIR = COMBAT_PACK_DIR + "runtime/audio/";
    /** 컨테이너 조작음 V2(걸쇠 '탁!' + 묵직한 개봉 '쿵-텅') — 팩의 press/open 대체. */
    public static final String SOUND_CONTAINER_PRESS = "container_sfx_v2/container_press_v2.wav";
    public static final String SOUND_CONTAINER_OPEN = "container_sfx_v2/container_open_v2.wav";
    /** 아이템 획득음(경쾌한 상승 차임). */
    public static final String SOUND_ITEM_PICKUP = "container_sfx_v2/item_pickup_v1.wav";
    public static final String SOUND_MONSTER_HIT_1 = COMBAT_AUDIO_DIR + "monster_hit_01.wav";
    public static final String SOUND_MONSTER_HIT_2 = COMBAT_AUDIO_DIR + "monster_hit_02.wav";
    public static final String SOUND_PLAYER_HURT = COMBAT_AUDIO_DIR + "player_hurt.wav";
    public static final String SOUND_PLAYER_DEATH = COMBAT_AUDIO_DIR + "player_death.wav";
    public static final String SOUND_DOOR_OPEN = COMBAT_AUDIO_DIR + "door_open_metal.wav";
    public static final String SOUND_ATTACK_SWING = COMBAT_AUDIO_DIR + "attack_swing.wav";
    public static final String SOUND_DASH_WHOOSH = COMBAT_AUDIO_DIR + "dash_whoosh.wav";

    /** 배경음악(사용자 제공 영상에서 추출, OGG 스트리밍·무한 반복). */
    public static final String MUSIC_BACKGROUND = "music/background_music.ogg";

    /** 효과음 켜기/끄기 버튼(청록 음파=켜짐, 주황 빗금=꺼짐). 배경음악과 독립. */
    public static final String SFX_TOGGLE_ON = "sfx_toggle_ui_v1/runtime/sfx_on_96.png";
    public static final String SFX_TOGGLE_OFF = "sfx_toggle_ui_v1/runtime/sfx_off_96.png";

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

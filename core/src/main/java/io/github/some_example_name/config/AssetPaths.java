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

    /** Mona 픽셀 폰트(Monad ABXY, OFL 1.1) — 한글 포함 Mona12 Text KR. UI 전역 폰트. */
    public static final String FONT_MONA_KR = "fonts/mona/Mona12TextKR.ttf";
    public static final String FONT_MONA_KR_BOLD = "fonts/mona/Mona12TextKR-Bold.ttf";

    /** 본부 판매 상점 UI 팩(hq_sales_storage_v1): 패널·버튼·판매 표식·재화·슬롯/선택 테두리. */
    public static final String HQ_SALES_DIR = "ui/hq_sales_storage_v1/";
    public static final String HQ_SALES_PANEL = HQ_SALES_DIR + "runtime/sale_panel_960x640.png";
    public static final String HQ_SALES_BUTTON = HQ_SALES_DIR + "runtime/sale_button_256x64.png";
    public static final String HQ_SALES_SIGN = HQ_SALES_DIR + "runtime/shop_sale_sign_96.png";
    public static final String HQ_SALES_COIN_64 = HQ_SALES_DIR + "runtime/currency_coin_64.png";
    public static final String HQ_SALES_COIN_32 = HQ_SALES_DIR + "runtime/currency_coin_32.png";
    public static final String HQ_SALES_SLOT = HQ_SALES_DIR + "runtime/item_slot_64.png";
    public static final String HQ_SALES_SELECTED = HQ_SALES_DIR + "runtime/item_selected_border_64.png";

    /** 본부 강화대·영구 강화 트리 팩(hq_upgrade_tree_v1). 버튼·재화는 판매 팩 디자인과 동일. */
    public static final String UPGRADE_DIR = "ui/hq_upgrade_tree_v1/";
    public static final String UPGRADE_TREE_DATA = UPGRADE_DIR + "data/upgrade_tree.json";
    public static final String UPGRADE_PANEL = UPGRADE_DIR + "runtime/upgrade_panel_1120x720.png";
    public static final String UPGRADE_BENCH = UPGRADE_DIR + "runtime/upgrade_bench_north_192x128.png";
    public static final String UPGRADE_NODE_LOCKED = UPGRADE_DIR + "runtime/node_locked_96.png";
    public static final String UPGRADE_NODE_AVAILABLE = UPGRADE_DIR + "runtime/node_available_96.png";
    public static final String UPGRADE_NODE_PURCHASED = UPGRADE_DIR + "runtime/node_purchased_96.png";
    public static final String UPGRADE_ICON_HEALTH = UPGRADE_DIR + "runtime/icon_health_64.png";
    public static final String UPGRADE_ICON_ATTACK = UPGRADE_DIR + "runtime/icon_attack_64.png";
    public static final String UPGRADE_ICON_SKILL = UPGRADE_DIR + "runtime/icon_skill_64.png";
    public static final String UPGRADE_LINK_H_ACTIVE = UPGRADE_DIR + "runtime/link_h_active.png";
    public static final String UPGRADE_LINK_H_COMPLETE = UPGRADE_DIR + "runtime/link_h_complete.png";
    public static final String UPGRADE_LINK_H_INACTIVE = UPGRADE_DIR + "runtime/link_h_inactive.png";
    public static final String UPGRADE_LINK_V_ACTIVE = UPGRADE_DIR + "runtime/link_v_active.png";
    public static final String UPGRADE_LINK_V_COMPLETE = UPGRADE_DIR + "runtime/link_v_complete.png";
    public static final String UPGRADE_LINK_V_INACTIVE = UPGRADE_DIR + "runtime/link_v_inactive.png";

    /** 보스 1-1 팩(boss_stage_1_1_v1): 보스 시트·광장·텔레그래프·HP바·단계 선택 UI·데이터. */
    public static final String BOSS_DIR = "boss_stage_1_1_v1/";
    public static final String BOSS_STAGES_DATA = BOSS_DIR + "data/stages.json";
    // 9단계 확장 팩(단계/패턴/투기장 데이터는 채택본 *.runtime.json만 로드)
    public static final String BOSS9_DIR = "boss_nine_stages_v1/";
    public static final String BOSS9_STAGES_DATA = BOSS9_DIR + "data/stage_catalog.runtime.json";
    public static final String BOSS9_PATTERNS_DATA = BOSS9_DIR + "data/patterns.runtime.json";
    public static final String BOSS9_ARENAS_DATA = BOSS9_DIR + "data/arena_layouts.runtime.json";
    public static final String BOSS_1_1_DATA = BOSS_DIR + "data/boss_1_1.json";
    public static final String BOSS_IDLE = BOSS_DIR + "runtime/boss/boss_idle_384x384.png";
    public static final String BOSS_ATTACK = BOSS_DIR + "runtime/boss/boss_attack_768x384.png";
    public static final String BOSS_DEATH = BOSS_DIR + "runtime/boss/boss_death_576x384.png";
    public static final String BOSS_PROJECTILE = BOSS_DIR + "runtime/vfx/boss_projectile_256x256.png";
    public static final String BOSS_ACID_POOL = BOSS_DIR + "runtime/vfx/acid_pool_384x256.png";
    public static final String BOSS_IMPACT = BOSS_DIR + "runtime/vfx/boss_impact_384x256.png";
    public static final String BOSS_TELEGRAPH_CIRCLE = BOSS_DIR + "runtime/vfx/telegraph_circle_128.png";
    public static final String BOSS_TELEGRAPH_LANE = BOSS_DIR + "runtime/vfx/telegraph_lane_256x64.png";
    public static final String BOSS_ARENA_FLOOR = BOSS_DIR + "runtime/arena/floor_128.png";
    public static final String BOSS_ARENA_PILLAR = BOSS_DIR + "runtime/arena/pillar_96x128.png";
    public static final String BOSS_ARENA_BARRIER = BOSS_DIR + "runtime/arena/barrier_128x64.png";
    public static final String BOSS_GATE_LOCKED = BOSS_DIR + "runtime/arena/gate_locked_192x128.png";
    public static final String BOSS_GATE_CLOSED = BOSS_DIR + "runtime/arena/gate_closed_192x128.png";
    public static final String BOSS_GATE_OPEN = BOSS_DIR + "runtime/arena/gate_open_192x128.png";
    public static final String BOSS_HP_FRAME = BOSS_DIR + "runtime/ui/boss_hp_frame_768x64.png";
    public static final String BOSS_HP_FILL = BOSS_DIR + "runtime/ui/boss_hp_fill_256x16.png";
    public static final String BOSS_EMBLEM = BOSS_DIR + "runtime/ui/boss_emblem_96.png";
    public static final String BOSS_STAGE_PANEL = BOSS_DIR + "runtime/ui/stage_panel_1120x720.png";
    public static final String BOSS_STAGE_BUTTON = BOSS_DIR + "runtime/ui/stage_button_256x64.png";
    public static final String BOSS_STAGE_LOCKED = BOSS_DIR + "runtime/ui/stage_locked_96.png";
    public static final String BOSS_STAGE_READY = BOSS_DIR + "runtime/ui/stage_ready_96.png";
    public static final String BOSS_REWARD_FRAME = BOSS_DIR + "runtime/ui/reward_frame_96.png";
    public static final String BOSS_REWARD_GLOW = BOSS_DIR + "runtime/ui/reward_glow_128.png";

    /** 추가 스킬 3종 팩(player_three_skills_v1): 아이콘 3개 + VFX 시트 6종 + 밸런스 JSON. */
    public static final String THREE_SKILLS_DIR = "ui/player_three_skills_v1/";
    public static final String THREE_SKILLS_DATA = THREE_SKILLS_DIR + "data/skills.json";
    public static final String ICON_SHOCKWAVE = THREE_SKILLS_DIR + "runtime/icons/icon_shockwave_64.png";
    public static final String ICON_ELECTRIC = THREE_SKILLS_DIR + "runtime/icons/icon_electric_throw_64.png";
    public static final String ICON_SHIELD = THREE_SKILLS_DIR + "runtime/icons/icon_shield_64.png";
    public static final String VFX_SHOCKWAVE = THREE_SKILLS_DIR + "runtime/vfx/shockwave_512x256.png";
    public static final String VFX_SHOCK_HIT = THREE_SKILLS_DIR + "runtime/vfx/shock_hit_384x256.png";
    public static final String VFX_ELECTRIC_PROJECTILE = THREE_SKILLS_DIR + "runtime/vfx/electric_projectile_256x256.png";
    public static final String VFX_ELECTRIC_IMPACT = THREE_SKILLS_DIR + "runtime/vfx/electric_impact_384x256.png";
    public static final String VFX_SHIELD_ACTIVE = THREE_SKILLS_DIR + "runtime/vfx/shield_active_384x256.png";
    public static final String VFX_SHIELD_BREAK = THREE_SKILLS_DIR + "runtime/vfx/shield_break_384x256.png";

    /** 공통 스킬 시스템 팩(player_skill_loadout_v1): 회복 아이콘/캐스트 시트·슬롯·키캡·패널. */
    public static final String SKILL_DIR = "ui/player_skill_loadout_v1/";
    public static final String SKILL_ICON_HEAL = SKILL_DIR + "runtime/icon_heal_64.png";
    public static final String SKILL_HEAL_SHEET = SKILL_DIR + "runtime/heal_cast_sheet_512x384.png";
    public static final String SKILL_SLOT_NORMAL = SKILL_DIR + "runtime/slot_normal_96.png";
    public static final String SKILL_SLOT_SELECTED = SKILL_DIR + "runtime/slot_selected_96.png";
    public static final String SKILL_SLOT_LOCKED = SKILL_DIR + "runtime/slot_locked_96.png";
    public static final String SKILL_KEYCAP = SKILL_DIR + "runtime/keycap_blank_96x48.png";
    public static final String SKILL_PANEL = SKILL_DIR + "runtime/skill_panel_1120x720.png";
    public static final String SKILL_REGISTER_BUTTON = SKILL_DIR + "runtime/register_button_256x64.png";

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

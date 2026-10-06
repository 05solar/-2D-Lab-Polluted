package io.github.some_example_name.config;

/** 자산 경로를 한곳에서 관리한다. 화면/엔티티마다 같은 경로 문자열을 반복하지 않는다. */
public final class AssetPaths {

    public static final String LAB_TILESET = "textures/environment/laboratory_tileset_64.png";
    public static final String PLAYER_SHEET = "textures/player/player_scout_sheet_64.png";
    public static final String PLAYER_ANIMATION_DATA = "data/player_scout.json";

    private AssetPaths() {
    }
}

package io.github.some_example_name.world;

/**
 * 시각 타일: 어떤 타일셋 이미지를 그릴지만 정의한다.
 * 타일셋(assets/textures/environment/laboratory_tileset_64.png)을 좌상단에서 행 우선으로
 * 분할한 인덱스(= row*4 + col, 4열 기준)와 1:1 대응한다. 숫자 인덱스를 렌더러 여러 곳에
 * 직접 쓰지 않도록 여기 한곳에서만 매핑한다.
 */
public enum TileVisual {
    FLOOR_BASIC(0),        // 기본 연구소 바닥
    FLOOR_CRACKED(1),      // 균열 바닥
    FLOOR_STAIN(2),        // 얼룩·약한 오염 바닥
    FLOOR_WARNING(3),      // 황색·검정 경고 바닥
    WALL_NORTH(4),         // 북쪽 벽
    WALL_SOUTH(5),         // 남쪽 벽
    WALL_WEST(6),          // 서쪽 벽
    WALL_EAST(7),          // 동쪽 벽
    WALL_CORNER_NW(8),     // 북서쪽 모서리
    WALL_CORNER_NE(9),     // 북동쪽 모서리
    WALL_CORNER_SW(10),    // 남서쪽 모서리
    WALL_CORNER_SE(11),    // 남동쪽 모서리
    DOOR_CLOSED(12),       // 닫힌 연구소 문
    DOORWAY_OPEN(13),      // 열린 출입구
    FLOOR_TOXIC(14),       // 독성 물질이 흐르는 바닥
    FLOOR_WIRES(15);       // 파손된 바닥과 노출 전선

    public final int tilesetIndex;

    TileVisual(int tilesetIndex) {
        this.tilesetIndex = tilesetIndex;
    }
}

package io.github.some_example_name.world;

/**
 * 논리 타일: 이동/충돌과 문 상태만 정의한다(게임 규칙 전용).
 * PNG 인덱스·아틀라스 좌표를 가지지 않는다(시각은 {@link LaboratoryTileSetV2}/{@link RoomVisuals}).
 *
 * 독성/감전 같은 위험은 바닥 타입이 아니라 오버레이에서 비롯되므로 {@link Hazard}로 따로 둔다.
 * (독성 웅덩이·전기 스파크가 올라간 바닥 셀은 통과 가능한 FLOOR이면서 hazard만 표시된다)
 */
public enum TileType {
    FLOOR(false),
    WALL(true),
    DOOR_CLOSED(true),     // 닫힌 문: 상호작용 전까지 벽과 동일하게 충돌
    DOORWAY_OPEN(false);   // 열린 문/출입구: 통과 가능

    public final boolean solid;

    TileType(boolean solid) {
        this.solid = solid;
    }
}

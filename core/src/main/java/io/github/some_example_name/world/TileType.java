package io.github.some_example_name.world;

/**
 * 논리 타일: 이동/충돌과 위험 지역 의미만 정의한다.
 * 어떤 타일 이미지를 그릴지(시각)는 {@link TileVisual}이, 배치는 {@link LaboratoryLayout}이 담당한다.
 * 하나의 enum이 충돌·시각 인덱스·배치 목적을 모두 결정하지 않도록 책임을 분리한다.
 *
 * - hazard: 위험 지역 여부. 1단계에서는 의미만 데이터로 구분하고 피해 로직은 추가하지 않는다.
 *   (독성 물질/노출 전선 바닥. 실제 HP 감소는 이후 단계에서 구현)
 */
public enum TileType {
    FLOOR(false, false),
    HAZARD(false, true),       // 통과 가능하지만 위험으로 표시된 바닥
    WALL(true, false),
    DOOR_CLOSED(true, false),  // 상호작용 전까지는 벽과 동일하게 충돌
    DOORWAY_OPEN(false, false);

    public final boolean solid;
    public final boolean hazard;

    TileType(boolean solid, boolean hazard) {
        this.solid = solid;
        this.hazard = hazard;
    }
}

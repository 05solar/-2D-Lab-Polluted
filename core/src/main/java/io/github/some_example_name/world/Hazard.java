package io.github.some_example_name.world;

/**
 * 셀의 위험 종류(시각 오버레이와 분리된 게임 규칙 데이터).
 * 1단계에서는 의미만 제공하고 실제 피해(HP 감소/감전)는 이후 단계에서 구현한다.
 *   - TOXIC: 독성 웅덩이·오염 비말·배수구 누수 위 바닥
 *   - SHOCK: 전기 스파크(감전) 위 바닥
 */
public enum Hazard {
    NONE, TOXIC, SHOCK
}

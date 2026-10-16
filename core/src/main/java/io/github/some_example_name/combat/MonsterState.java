package io.github.some_example_name.combat;

/**
 * 몬스터 행동 상태. 렌더러는 비팀장에게 ATTACK 외 상태를 모두 이동 클립으로 그리므로
 * WANDER/SEARCH/RETURN 추가는 기존 애니메이션과 호환된다.
 */
public enum MonsterState {
    IDLE,       // 대기(배회 중 휴식 포함, 팀장은 착석)
    WANDER,     // 배회/순찰 이동 중
    ALERT,      // (구) 경계 — 호환용으로 유지, 현재 AI는 사용하지 않음
    ENRAGED,    // 팀장 분노 연출
    CHASE,      // 추적(시야 유지 중이거나 마지막 목격 지점으로 이동)
    SEARCH,     // 시야를 잃은 뒤 마지막 목격 지점 주변 탐색
    RETURN,     // 활동 구역(홈)으로 복귀
    ATTACK,
    HIT,
    DEAD
}

package io.github.some_example_name.collision;

/**
 * 충돌 레이어(규칙서 9장). 어떤 대상끼리 충돌을 판정하는지 명시하기 위한 분류.
 * 1단계에서는 PLAYER vs WALL만 실제로 사용하고, 나머지는 이후 단계에서 쓴다.
 */
public enum CollisionLayer {
    PLAYER,
    MONSTER,
    WALL,
    HAZARD,
    TREASURE,
    TREASURE_TRIGGER,
    DELIVERY_ZONE,
    PLAYER_ATTACK,
    MONSTER_ATTACK,
    SENSOR
}

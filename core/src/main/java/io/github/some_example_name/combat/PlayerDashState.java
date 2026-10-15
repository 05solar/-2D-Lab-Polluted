package io.github.some_example_name.combat;

import io.github.some_example_name.entity.Direction;
import java.util.HashSet;
import java.util.Set;

/**
 * F 돌진 상태(순수 Java — 입력/렌더에 의존하지 않아 단위 테스트 가능).
 *
 * - 발동: 쿨다운이 끝났고 돌진 중이 아닐 때만. 키를 누르고 있어도 엣지 입력에서만 호출되므로
 *   자동 재발동이 없다. 방향은 현재 이동 입력(정규화, 대각선 포함), 입력이 없으면 마지막으로
 *   바라본 방향이다.
 * - 쿨다운은 발동 순간부터 시작한다. 쿨다운 중 tryStart는 false(이동/피해/효과음 없음).
 * - 피해 대상 중복 방지: 한 돌진에서 같은 몬스터는 markHit로 1회만 맞는다.
 * - 달리기(Shift) 상태와 별개의 상태로, RunStartDetector를 건드리지 않는다.
 */
public final class PlayerDashState {

    private final float distance, duration, cooldownSeconds;
    private final Set<Long> hitIds = new HashSet<>();
    private float cooldown;
    private float elapsed;
    private boolean active;
    private float dirX, dirY;

    public PlayerDashState(float distanceTiles, float durationSeconds, float cooldownSeconds) {
        if (distanceTiles <= 0f || durationSeconds <= 0f || cooldownSeconds < 0f)
            throw new IllegalArgumentException("dash config");
        this.distance = distanceTiles;
        this.duration = durationSeconds;
        this.cooldownSeconds = cooldownSeconds;
    }

    /**
     * 돌진 발동 시도. 이동 입력(moveX/moveY)이 있으면 그 방향(대각선 정규화),
     * 없으면 facing 방향으로 돌진한다.
     *
     * @return 실제로 발동했으면 true (false면 이동/피해/효과음 모두 없어야 한다)
     */
    public boolean tryStart(float moveX, float moveY, Direction facing) {
        if (active || cooldown > 0f || facing == null) return false;
        float dx = moveX, dy = moveY;
        if (dx == 0f && dy == 0f) {
            switch (facing) {
                case UP: dy = 1f; break;
                case DOWN: dy = -1f; break;
                case LEFT: dx = -1f; break;
                default: dx = 1f; break;
            }
        }
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        dirX = dx / length;
        dirY = dy / length;
        active = true;
        elapsed = 0f;
        cooldown = cooldownSeconds;   // 발동 순간부터 쿨다운
        hitIds.clear();
        return true;
    }

    /**
     * 이번 프레임의 돌진 이동 거리(월드 단위)를 돌려주고 시간을 진행시킨다.
     * 지속 시간을 넘는 마지막 프레임은 남은 거리만큼만 이동해 총거리가 정확히 distance가 된다.
     * 돌진 중이 아니면 0.
     */
    public float advance(float delta) {
        cooldown = Math.max(0f, cooldown - delta);
        if (!active) return 0f;
        float step = Math.min(delta, duration - elapsed);
        elapsed += step;
        if (elapsed >= duration) active = false;
        return distance / duration * step;
    }

    /** 벽에 막히는 등 돌진을 즉시 끝낸다(쿨다운은 그대로 유지). */
    public void interrupt() { active = false; }

    /** 사망/맵 전환 시 완전 초기화. */
    public void cancel() {
        active = false;
        elapsed = 0f;
        hitIds.clear();
    }

    /** 이 돌진에서 처음 맞는 대상이면 true(기록), 이미 맞았으면 false. */
    public boolean markHit(long monsterId) { return hitIds.add(monsterId); }

    public boolean active() { return active; }
    public float dirX() { return dirX; }
    public float dirY() { return dirY; }
    public float cooldownRemaining() { return cooldown; }
    public float cooldownTotal() { return cooldownSeconds; }
    public boolean ready() { return !active && cooldown <= 0f; }
}

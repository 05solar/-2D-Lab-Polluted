package io.github.some_example_name.combat;

import io.github.some_example_name.entity.Direction;
import java.util.HashSet;
import java.util.Set;

/**
 * 돌진 "이동" 상태(순수 Java). 2026-10-20 공통 스킬 시스템 도입으로 쿨다운은 이 클래스가
 * 아니라 {@link io.github.some_example_name.profile.SkillRuntime}이 skillId("dash") 기준으로
 * 한 곳에서만 관리한다(이중 관리 금지). 여기는 방향·지속·대상당 1회 타격만 담당한다.
 *
 * - 발동: 공통 스킬 서비스가 허용했을 때만 start()가 불린다(엣지 입력 — 자동 재발동 없음).
 *   방향은 이동 입력(대각선 정규화), 없으면 바라본 방향.
 * - 이동·충돌·피해 규칙(3타일/0.18s/하위 단계/관통 금지)은 기존 그대로 호출 측이 수행한다.
 */
public final class PlayerDashState {

    private final float distance, duration;
    private final Set<Long> hitIds = new HashSet<>();
    private float elapsed;
    private boolean active;
    private float dirX, dirY;

    public PlayerDashState(float distanceTiles, float durationSeconds) {
        if (distanceTiles <= 0f || durationSeconds <= 0f)
            throw new IllegalArgumentException("dash config");
        this.distance = distanceTiles;
        this.duration = durationSeconds;
    }

    /**
     * 돌진 시작(쿨다운 검증은 호출 전 공통 서비스가 완료). 이미 돌진 중이면 false.
     */
    public boolean start(float moveX, float moveY, Direction facing) {
        if (active || facing == null) return false;
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
        hitIds.clear();
        return true;
    }

    /**
     * 이번 프레임의 돌진 이동 거리(월드 단위)를 돌려주고 시간을 진행시킨다.
     * 마지막 프레임은 남은 거리만큼만 이동해 총거리가 정확히 distance가 된다.
     */
    public float advance(float delta) {
        if (!active) return 0f;
        float step = Math.min(delta, duration - elapsed);
        elapsed += step;
        if (elapsed >= duration) active = false;
        return distance / duration * step;
    }

    /** 벽에 정면으로 막히는 등 돌진을 즉시 끝낸다(쿨다운은 SkillRuntime이 그대로 유지). */
    public void interrupt() { active = false; }

    /** 사망/맵 전환 시 이동 상태 초기화(쿨다운은 건드리지 않는다). */
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
}

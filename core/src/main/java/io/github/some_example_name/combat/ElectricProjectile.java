package io.github.some_example_name.combat;

import com.badlogic.gdx.math.Rectangle;
import io.github.some_example_name.entity.monster.Monster;
import java.util.List;

/**
 * 전기 투척 투사체 1발(순수 Java). 방향은 시전 시작에 확정되어 비행 중 유도되지 않는다.
 * 이동은 "이전 위치 → 새 위치" 구간을 충돌 반경 이하의 샘플로 나눠 검사(swept circle)해
 * 큰 delta에도 얇은 벽·몬스터를 건너뛰지 않으며, 같은 구간에서는 더 앞의 충돌만 처리한다
 * (벽이 몬스터보다 앞이면 피해 없음). 관통/연쇄 없음 — 첫 충돌에서 소멸한다.
 */
public final class ElectricProjectile {

    /** 월드 solid 검사(벽·닫힌 문·solid 가구). 호출 측이 LaboratoryRoom로 구현. */
    public interface Solid { boolean blockedAt(float centerX, float centerY, float radius); }

    public enum Outcome { FLYING, HIT_MONSTER, HIT_WALL, EXPIRED }

    private final float dirX, dirY, speed, maxDistance, radius;
    private float x, y;
    private float traveled;
    private boolean alive = true;
    private Monster hitMonster;

    public ElectricProjectile(float x, float y, float dirX, float dirY,
                              float speed, float maxDistance, float radius) {
        float len = (float) Math.sqrt(dirX * dirX + dirY * dirY);
        if (len <= 0f || speed <= 0f || maxDistance <= 0f || radius <= 0f)
            throw new IllegalArgumentException("projectile");
        this.x = x;
        this.y = y;
        this.dirX = dirX / len;
        this.dirY = dirY / len;
        this.speed = speed;
        this.maxDistance = maxDistance;
        this.radius = radius;
    }

    public float x() { return x; }
    public float y() { return y; }
    public float dirX() { return dirX; }
    public float dirY() { return dirY; }
    public boolean alive() { return alive; }
    /** HIT_MONSTER일 때 맞은 대상(그 외 null). */
    public Monster hitMonster() { return hitMonster; }

    /** 시간 전진. 충돌/소멸이 일어나면 그 종류를 돌려주고 alive=false가 된다. */
    public Outcome update(float delta, Solid solid, List<Monster> monsters) {
        if (!alive) return Outcome.EXPIRED;
        float distance = Math.min(speed * Math.max(0f, delta), maxDistance - traveled);
        // 샘플 간격 ≤ 충돌 반경: 반경보다 얇은 틈을 건너뛰지 않는다.
        int steps = Math.max(1, (int) Math.ceil(distance / Math.max(0.01f, radius * 0.9f)));
        float stepLen = distance / steps;
        for (int i = 1; i <= steps; i++) {
            float nx = x + dirX * stepLen, ny = y + dirY * stepLen;
            // 같은 샘플 위치에서 벽을 먼저 검사 — 벽이 몬스터보다 앞이면 피해 없음.
            if (solid != null && solid.blockedAt(nx, ny, radius)) {
                x = nx; y = ny;
                alive = false;
                return Outcome.HIT_WALL;
            }
            for (Monster monster : monsters) {
                if (monster == null || !monster.isAlive()) continue; // 죽은 대상 재타격 금지
                if (circleOverlapsRect(nx, ny, radius, monster.bounds())) {
                    x = nx; y = ny;
                    alive = false;
                    hitMonster = monster;
                    return Outcome.HIT_MONSTER;
                }
            }
            x = nx; y = ny;
            traveled += stepLen;
        }
        if (traveled >= maxDistance - 0.0001f) {
            alive = false;
            return Outcome.EXPIRED; // 최대 사거리: 피해·착탄 연출 없이 소멸
        }
        return Outcome.FLYING;
    }

    private static boolean circleOverlapsRect(float cx, float cy, float r, Rectangle rect) {
        float nx = Math.max(rect.x, Math.min(cx, rect.x + rect.width));
        float ny = Math.max(rect.y, Math.min(cy, rect.y + rect.height));
        float dx = cx - nx, dy = cy - ny;
        return dx * dx + dy * dy <= r * r;
    }
}

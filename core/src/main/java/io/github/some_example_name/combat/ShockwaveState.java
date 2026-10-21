package io.github.some_example_name.combat;

import com.badlogic.gdx.math.Rectangle;
import io.github.some_example_name.entity.monster.Monster;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 충격파 1회 시전(순수 Java). 원점은 시전 시작 위치에 고정되고(발동 후 캐릭터를 따라가지
 * 않음), 판정 파면은 데이터(radiusCurve × 최대 반경)로 커진다 — 시작부터 최대 반경 원
 * 전체에 피해를 주지 않는다.
 *
 * 명중 규칙: 피해 활성 구간(damageStart~damageEnd) 동안 "직전 파면 → 현재 파면" 사이의
 * 고리(annulus)와 몬스터 hurtbox의 교차를 검사해, 교차한 최초 시점에 castId+대상 id로
 * 1회만 피해·밀어내기를 적용한다. 첫 활성 스텝은 파면 0부터라 원점 위의 대상도 누락되지
 * 않고, 큰 delta로 여러 경계를 건너도 annulus가 그 구간 전체를 덮어 순서대로 걸린다.
 * 활성 구간이 끝나면(잔상 프레임) 더 이상 타격하지 않는다.
 *
 * 차폐: 원점→대상 중심 경로가 벽·닫힌 문·solid 가구에 막히면 때리지 않는다(검사는
 * 호출 측이 Occlusion으로 주입 — 모델은 GL/월드에 의존하지 않는다).
 */
public final class ShockwaveState {

    /** 원점→대상 경로가 막혔는지(벽·닫힌 문·solid 가구). 장식 오버레이는 차폐가 아니다. */
    public interface Occlusion { boolean blocked(float fromX, float fromY, float toX, float toY); }

    /** 이번 스텝에서 처음 파면에 걸린 대상과 바깥 방향(정규화). */
    public static final class Hit {
        public final Monster monster;
        public final float dirX, dirY;
        Hit(Monster monster, float dirX, float dirY) {
            this.monster = monster;
            this.dirX = dirX;
            this.dirY = dirY;
        }
    }

    private final float originX, originY;
    private final float damageStart, damageEnd, duration;
    private final RadiusAt radius;
    // 방향 폴백: 대상이 원점과 정확히 겹칠 때(NaN 방지) 시전자가 바라보던 방향.
    private final float fallbackDirX, fallbackDirY;
    private final Set<Long> hitIds = new HashSet<>();
    private float elapsed;
    private float frontRadius; // 피해 활성 후 지나온 파면 반경(시작 0)

    /** elapsed초 시점의 파면 반경(타일) — SkillCatalog.shockwaveFrontRadius를 넘긴다. */
    public interface RadiusAt { float radius(float elapsedSeconds); }

    public ShockwaveState(float originX, float originY, float damageStart, float damageEnd,
                          float duration, RadiusAt radius, float fallbackDirX, float fallbackDirY) {
        if (damageEnd <= damageStart || duration < damageEnd || radius == null)
            throw new IllegalArgumentException("shockwave timeline");
        this.originX = originX;
        this.originY = originY;
        this.damageStart = damageStart;
        this.damageEnd = damageEnd;
        this.duration = duration;
        this.radius = radius;
        float len = (float) Math.sqrt(fallbackDirX * fallbackDirX + fallbackDirY * fallbackDirY);
        this.fallbackDirX = len > 0f ? fallbackDirX / len : 1f;
        this.fallbackDirY = len > 0f ? fallbackDirY / len : 0f;
    }

    public float originX() { return originX; }
    public float originY() { return originY; }
    public float elapsed() { return elapsed; }
    public boolean finished() { return elapsed >= duration; }
    /** 디버그·렌더 공용: 현재 파면 반경(피해 창과 무관한 시각 곡선 값). */
    public float currentFrontRadius() { return radius.radius(Math.min(elapsed, damageEnd)); }

    /**
     * 시간 전진 + 이번 구간에 파면이 새로 덮은 고리에 걸린 대상 목록.
     * 피해 적용(공용 타격 경로)·VFX는 호출 측이 결과로 수행한다.
     */
    public List<Hit> update(float delta, List<Monster> monsters, Occlusion occlusion) {
        List<Hit> hits = new ArrayList<>();
        float previous = elapsed;
        elapsed += Math.max(0f, delta);
        // 이번 구간과 피해 창의 교집합(경계: 이전 < t ≤ 현재).
        float windowFrom = Math.max(previous, 0f);
        float windowTo = Math.min(elapsed, damageEnd);
        if (windowTo <= damageStart || windowFrom >= damageEnd) {
            if (elapsed > damageEnd) frontRadius = radius.radius(damageEnd);
            return hits;
        }
        float newFront = radius.radius(windowTo);
        float innerFront = frontRadius; // 첫 활성 스텝은 0 — 원점 위 대상 포함
        for (Monster monster : monsters) {
            if (monster == null || !monster.isAlive() || hitIds.contains(monster.id())) continue;
            Rectangle box = monster.bounds();
            float nearest = distanceToRect(originX, originY, box);
            float farthest = farthestCorner(originX, originY, box);
            // annulus (innerFront, newFront] 교차: 파면이 이번에 지나간 영역과 겹치는가.
            if (nearest > newFront || farthest < innerFront) continue;
            if (occlusion != null && occlusion.blocked(originX, originY,
                box.x + box.width / 2f, box.y + box.height / 2f)) continue;
            hitIds.add(monster.id());
            float dx = box.x + box.width / 2f - originX;
            float dy = box.y + box.height / 2f - originY;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len < 0.001f) { dx = fallbackDirX; dy = fallbackDirY; }
            else { dx /= len; dy /= len; }
            hits.add(new Hit(monster, dx, dy));
        }
        frontRadius = newFront;
        return hits;
    }

    private static float distanceToRect(float px, float py, Rectangle r) {
        float cx = Math.max(r.x, Math.min(px, r.x + r.width));
        float cy = Math.max(r.y, Math.min(py, r.y + r.height));
        float dx = px - cx, dy = py - cy;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private static float farthestCorner(float px, float py, Rectangle r) {
        float dx = Math.max(Math.abs(px - r.x), Math.abs(px - (r.x + r.width)));
        float dy = Math.max(Math.abs(py - r.y), Math.abs(py - (r.y + r.height)));
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
}

package io.github.some_example_name.profile;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

/**
 * 충격파/전기 투척/보호막 밸런스 카탈로그(순수 Java). 수치의 유일한 출처는
 * assets/ui/player_three_skills_v1/data/skills.json(2026-10-20 동봉 초안 채택)이며
 * 코드에 복사하지 않는다. 거리 단위는 월드 타일(worldUnit = oneTile — 프로젝트 좌표와 동일).
 *
 * 판정 타임라인(이벤트 시각·radiusCurve)은 시뮬레이션 누적 시간 기준 데이터다.
 * 렌더러의 현재 프레임이 아니라 같은 elapsed로 판정과 표시를 모두 선택한다.
 */
public final class SkillCatalog {

    // 충격파
    public final float shockwaveCooldownSeconds;
    public final int shockwaveDamage;
    public final float shockwaveRadius;          // 최대 반경(타일)
    public final float shockwaveKnockback;       // 밀어내기 거리(타일)
    public final float shockwaveDamageStart;     // 피해 활성 시작(초)
    public final float shockwaveDamageEnd;       // 피해 종료(초)
    public final float shockwaveDuration;        // 잔상 포함 전체 길이(초)
    /** [초, 최대 반경 비율] 쌍 — 판정 파면과 그림의 고리가 같은 속도로 커진다. */
    public final float[][] shockwaveRadiusCurve;

    // 전기 투척
    public final float electricCooldownSeconds;
    public final int electricDamage;
    public final float electricSpeed;            // 타일/초
    public final float electricMaxDistance;      // 타일
    public final float electricCollisionRadius;  // 타일
    public final float electricStunSeconds;      // 감전 지속
    public final float electricSpawnDelay;       // 준비 → spawn_projectile(초)

    // 보호막
    public final float shieldCooldownSeconds;
    public final int shieldAbsorbAmount;
    public final float shieldDurationSeconds;    // 활성 지속(활성 시작 기준)
    public final float shieldActivateDelay;      // 준비 → activate_shield(초)

    public SkillCatalog(String json) {
        JsonValue root = new JsonReader().parse(json);
        JsonValue shock = null, electric = null, shield = null;
        for (JsonValue s = root.get("skills").child; s != null; s = s.next) {
            String id = s.getString("skillId");
            if ("shockwave".equals(id)) shock = s;
            else if ("electric_throw".equals(id)) electric = s;
            else if ("shield".equals(id)) shield = s;
        }
        if (shock == null || electric == null || shield == null)
            throw new IllegalArgumentException("skills.json missing one of shockwave/electric_throw/shield");

        shockwaveCooldownSeconds = shock.getFloat("cooldownSeconds");
        shockwaveDamage = shock.getInt("damage");
        shockwaveRadius = shock.getFloat("radius");
        shockwaveKnockback = shock.getFloat("knockbackDistance");
        JsonValue timeline = shock.get("timeline");
        shockwaveDuration = timeline.getFloat("durationSeconds");
        float begin = Float.NaN, end = Float.NaN;
        for (JsonValue e = timeline.get("events").child; e != null; e = e.next) {
            String type = e.getString("type");
            if ("START_WAVE_DAMAGE".equals(type)) begin = e.getFloat("atSeconds");
            if ("STOP_WAVE_DAMAGE".equals(type)) end = e.getFloat("atSeconds");
        }
        if (Float.isNaN(begin) || Float.isNaN(end) || end <= begin)
            throw new IllegalArgumentException("shockwave damage window invalid");
        shockwaveDamageStart = begin;
        shockwaveDamageEnd = end;
        JsonValue curve = timeline.get("radiusCurve");
        shockwaveRadiusCurve = new float[curve.size][];
        int i = 0;
        float prevTime = -1f;
        for (JsonValue p = curve.child; p != null; p = p.next) {
            float[] pair = p.asFloatArray();
            if (pair.length != 2 || pair[0] <= prevTime || pair[1] < 0f || pair[1] > 1f)
                throw new IllegalArgumentException("shockwave radiusCurve invalid at " + i);
            prevTime = pair[0];
            shockwaveRadiusCurve[i++] = pair;
        }

        electricCooldownSeconds = electric.getFloat("cooldownSeconds");
        electricDamage = electric.getInt("damage");
        electricSpeed = electric.getFloat("projectileSpeed");
        electricMaxDistance = electric.getFloat("maxTravelDistance");
        electricCollisionRadius = electric.getFloat("collisionRadius");
        electricStunSeconds = electric.getFloat("stunSeconds");
        electricSpawnDelay = firstEventSeconds(electric, "SPAWN_PROJECTILE");

        shieldCooldownSeconds = shield.getFloat("cooldownSeconds");
        shieldAbsorbAmount = shield.getInt("absorbAmount");
        shieldDurationSeconds = shield.getFloat("durationSeconds");
        shieldActivateDelay = firstEventSeconds(shield, "ACTIVATE_SHIELD");
    }

    private static float firstEventSeconds(JsonValue skill, String type) {
        for (JsonValue e = skill.get("timeline").get("events").child; e != null; e = e.next)
            if (type.equals(e.getString("type"))) return e.getFloat("atSeconds");
        throw new IllegalArgumentException("missing timeline event " + type);
    }

    /**
     * 시전 후 elapsed초 시점의 파면 반경(타일) — radiusCurve 보간 × 최대 반경.
     * 곡선 끝 이후는 마지막 값으로 고정(재팽창 없음).
     */
    public float shockwaveFrontRadius(float elapsed) {
        float[][] c = shockwaveRadiusCurve;
        if (elapsed <= c[0][0]) return c[0][1] * shockwaveRadius;
        for (int i = 1; i < c.length; i++) {
            if (elapsed <= c[i][0]) {
                float t = (elapsed - c[i - 1][0]) / (c[i][0] - c[i - 1][0]);
                return (c[i - 1][1] + (c[i][1] - c[i - 1][1]) * t) * shockwaveRadius;
            }
        }
        return c[c.length - 1][1] * shockwaveRadius;
    }
}

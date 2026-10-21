package io.github.some_example_name.boss;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

/**
 * 보스 1-1(원심 배양체 C-01) 정의 — 수치/패턴/시간의 유일 출처는
 * assets/boss_stage_1_1_v1/data/boss_1_1.json(기존 확정 보스 밸런스 부재로 동봉 초안 채택,
 * 2026-10-20)이다. 코드에 수치를 복사하지 않는다. 거리 단위는 월드 타일.
 */
public final class BossDefinition {

    public final String stageId, bossId, displayName;
    public final int maxHp;
    public final float introSeconds, betweenPatternSeconds;
    public final String[] firstPatternSequence;
    public final boolean avoidImmediateRepeat;
    public final float bodyColliderW, bodyColliderH;
    public final boolean knockbackImmune, stunImmune, electricDamageImmune;

    // 패턴 1 — 시약 투척
    public final float throwTelegraph, throwAimLock, throwDuration;
    public final float[] throwShotTimes;   // 발사 이벤트 시각 3개
    public final float[] throwShotAngles;  // 대응 각도(도)
    public final int throwWeight, throwDamage;
    public final float throwSpeed, throwMaxDistance, throwRadius;

    // 패턴 2 — 오염 장판
    public final float poolTelegraph, poolActivateAt, poolDuration;
    public final int poolWeight, poolDamagePerTick;
    public final float poolRadius, poolActiveSeconds, poolTickSeconds;
    public final boolean poolFirstTickAtActivation;
    public final int poolMaxActive;

    // 패턴 3 — 장비 돌진
    public final float ramTelegraph, ramAimLock, ramChargeStartAt;
    public final int ramWeight, ramDamage;
    public final float ramSpeed, ramMaxDistance, ramColliderWidth, ramRecoverySeconds;

    public BossDefinition(String json) {
        JsonValue root = new JsonReader().parse(json);
        stageId = root.getString("stageId");
        bossId = root.getString("bossId");
        displayName = root.getString("displayName");
        maxHp = root.getInt("maxHp");
        introSeconds = root.getFloat("introSeconds");
        betweenPatternSeconds = root.getFloat("betweenPatternSeconds");
        java.util.List<String> first = new java.util.ArrayList<>();
        for (JsonValue f = root.get("firstPatternSequence").child; f != null; f = f.next)
            first.add(f.asString());
        firstPatternSequence = first.toArray(new String[0]);
        avoidImmediateRepeat = root.getBoolean("avoidImmediatePatternRepeat", true);
        float[] collider = root.get("bodyProxyCollider").asFloatArray();
        bodyColliderW = collider[0];
        bodyColliderH = collider[1];
        JsonValue res = root.get("resistances");
        knockbackImmune = res.getBoolean("knockbackImmune", true);
        stunImmune = res.getBoolean("stunImmune", true);
        electricDamageImmune = res.getBoolean("electricDirectDamageImmune", false);

        JsonValue throwP = pattern(root, "reagent_throw");
        throwWeight = throwP.getInt("weight");
        throwTelegraph = throwP.getFloat("telegraphSeconds");
        throwAimLock = throwP.getFloat("aimLockAt");
        throwDuration = throwP.getFloat("durationSeconds");
        throwDamage = throwP.getInt("damage");
        throwSpeed = throwP.getFloat("projectileSpeed");
        throwMaxDistance = throwP.getFloat("projectileMaxDistance");
        throwRadius = throwP.getFloat("projectileRadius");
        java.util.List<float[]> shots = new java.util.ArrayList<>();
        for (JsonValue e = throwP.get("events").child; e != null; e = e.next)
            if ("spawn_projectile".equals(e.getString("type")))
                shots.add(new float[]{e.getFloat("atSeconds"), e.getFloat("angleDegrees")});
        throwShotTimes = new float[shots.size()];
        throwShotAngles = new float[shots.size()];
        for (int i = 0; i < shots.size(); i++) {
            throwShotTimes[i] = shots.get(i)[0];
            throwShotAngles[i] = shots.get(i)[1];
        }

        JsonValue pool = pattern(root, "contamination");
        poolWeight = pool.getInt("weight");
        poolTelegraph = pool.getFloat("telegraphSeconds");
        poolActivateAt = firstEvent(pool, "spawn_area_and_begin_loop_frame_2");
        poolDuration = pool.getFloat("durationSeconds");
        poolRadius = pool.getFloat("radius");
        poolActiveSeconds = pool.getFloat("areaActiveSeconds");
        poolDamagePerTick = pool.getInt("damagePerTick");
        poolTickSeconds = pool.getFloat("tickSeconds");
        poolFirstTickAtActivation = pool.getBoolean("firstTickAtActivation", true);
        poolMaxActive = pool.getInt("maximumActiveAreas", 2);

        JsonValue ram = pattern(root, "ram");
        ramWeight = ram.getInt("weight");
        ramTelegraph = ram.getFloat("telegraphSeconds");
        ramAimLock = ram.getFloat("aimLockAt");
        ramChargeStartAt = firstEvent(ram, "start_charge");
        ramSpeed = ram.getFloat("chargeSpeed");
        ramMaxDistance = ram.getFloat("chargeMaxDistance");
        ramColliderWidth = ram.getFloat("chargeColliderWidth");
        ramDamage = ram.getInt("damage");
        ramRecoverySeconds = ram.getFloat("recoveryAfterStopSeconds");
    }

    private static JsonValue pattern(JsonValue root, String id) {
        for (JsonValue p = root.get("patterns").child; p != null; p = p.next)
            if (id.equals(p.getString("patternId"))) return p;
        throw new IllegalArgumentException("missing pattern " + id);
    }

    private static float firstEvent(JsonValue patternNode, String type) {
        for (JsonValue e = patternNode.get("events").child; e != null; e = e.next)
            if (type.equals(e.getString("type"))) return e.getFloat("atSeconds");
        throw new IllegalArgumentException("missing event " + type);
    }
}

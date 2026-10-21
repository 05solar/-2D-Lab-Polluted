package io.github.some_example_name.boss;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 보스 패턴 정의(patterns.runtime.json — 2026-10-21 초안 채택, 순수 Java).
 * 수치(피해/시간/반경/각도)는 전부 여기서 읽고 코드에 복사하지 않는다. 1-1 레거시 패턴은
 * 기존 boss_1_1.json 값이 우선이라 이 카탈로그를 쓰지 않는다(legacy=true 항목은 참고용).
 */
public final class BossPatternCatalog {

    public static final class Pattern {
        public final String id, kind, art, impactArt;
        public final float windup, active, recovery, targetLockAt;
        public final int damage;
        // kind별 선택 필드(없으면 0/기본)
        public final float radius, angleDegrees, projectileSpeed, tickSeconds;
        public final int count, maxZones, targetCount;
        public final float speed, maxDistance, laneWidth, distanceEach, betweenChargeTelegraph;
        public final float length, width, angularSpeedDegrees;
        public final float startRadius, endRadius, ringWidth;
        public final float rootSeconds, rootImmunitySeconds;
        public final float slowFactor, slowSeconds;
        public final float triggerHpFraction, recoveryMultiplierAfter;
        public final float moveSpeed, dropInterval, lifetimeSeconds, targetSeparation;
        public final boolean occludedByPillars, blockedByPillars, damageOncePerCast;
        public final String sharedTickGroup;
        public final String[] subpatterns;

        Pattern(JsonValue n) {
            id = n.getString("id");
            kind = n.getString("kind");
            art = n.getString("art", null);
            impactArt = n.getString("impactArt", null);
            windup = n.getFloat("windupSeconds");
            active = n.getFloat("activeSeconds");
            recovery = n.getFloat("recoverySeconds");
            targetLockAt = n.getFloat("targetLockAtSeconds", 0f);
            damage = n.getInt("draftDamage", 0);
            radius = n.getFloat("radius", 0f);
            angleDegrees = n.getFloat("angleDegrees", 0f);
            projectileSpeed = n.getFloat("projectileSpeed", 0f);
            tickSeconds = n.getFloat("tickSeconds", 0.5f);
            count = n.getInt("count", 1);
            maxZones = n.getInt("maxZones", 2);
            targetCount = n.getInt("targetCount", 1);
            speed = n.getFloat("speed", 0f);
            maxDistance = n.getFloat("maxDistance", 0f);
            laneWidth = n.getFloat("laneWidth", 1.8f);
            distanceEach = n.getFloat("distanceEach", 0f);
            betweenChargeTelegraph = n.getFloat("betweenChargeTelegraphSeconds", 0.65f);
            length = n.getFloat("length", 0f);
            width = n.getFloat("width", 0.6f);
            angularSpeedDegrees = n.getFloat("angularSpeedDegrees", 0f);
            startRadius = n.getFloat("startRadius", 0f);
            endRadius = n.getFloat("endRadius", 0f);
            ringWidth = n.getFloat("ringWidth", 0.6f);
            rootSeconds = n.getFloat("rootSeconds", 0f);
            rootImmunitySeconds = n.getFloat("rootReapplyImmunitySeconds", 0f);
            JsonValue slow = n.get("slow");
            slowFactor = slow == null ? 1f : slow.getFloat("factor", 1f);
            slowSeconds = slow == null ? 0f : slow.getFloat("duration", 0f);
            triggerHpFraction = n.getFloat("triggerHpFraction", 0f);
            recoveryMultiplierAfter = n.getFloat("recoveryMultiplierAfter", 1f);
            moveSpeed = n.getFloat("moveSpeed", 0f);
            dropInterval = n.getFloat("dropIntervalSeconds", 0.8f);
            lifetimeSeconds = n.getFloat("lifetimeSeconds", n.getFloat("activeSeconds"));
            targetSeparation = n.getFloat("targetSeparation", 3f);
            occludedByPillars = n.getBoolean("occludedByPillars", false);
            blockedByPillars = n.getBoolean("blockedByPillars",
                n.getBoolean("stopOnPillar", false));
            damageOncePerCast = n.getBoolean("damageOncePerCast",
                n.getBoolean("damageOncePerSubcast", true));
            sharedTickGroup = n.getString("sharedDamageTickGroup", id);
            java.util.List<String> subs = new java.util.ArrayList<>();
            JsonValue sub = n.get("subpatterns");
            if (sub != null) for (JsonValue s = sub.child; s != null; s = s.next)
                subs.add(s.asString());
            subpatterns = subs.toArray(new String[0]);
        }
    }

    private final Map<String, Pattern> patterns = new LinkedHashMap<>();

    public BossPatternCatalog(String json) {
        JsonValue root = new JsonReader().parse(json);
        for (JsonValue p = root.get("patterns").child; p != null; p = p.next) {
            Pattern pattern = new Pattern(p);
            patterns.put(pattern.id, pattern);
        }
    }

    public Pattern pattern(String id) {
        Pattern p = patterns.get(id);
        if (p == null) throw new IllegalArgumentException("unknown pattern " + id);
        return p;
    }
}

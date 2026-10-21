package io.github.some_example_name.boss;

import com.badlogic.gdx.math.Rectangle;
import io.github.some_example_name.combat.HealthComponent;
import io.github.some_example_name.world.GenRng;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 9단계 보스전 범용 패턴 엔진(순수 Java — 1-2 이후 단계용; 1-1은 기존 BossFight를 회귀 기준
 * 그대로 유지). 단계별 Screen 복사본이나 stageId switch 없이 PatternDefinition(kind)별
 * 실행기로 동작한다. 렌더러는 이 상태를 읽어 그릴 뿐 피해/보상을 실행하지 않는다.
 *
 * 공통 계약: 모든 공격은 판정 데이터와 같은 기하의 바닥 경고를 먼저 보여주고, 타깃 고정
 * 시점·발사·피해 시작/끝·회복을 한 전투 시계(시뮬레이션 시간, "이전<t≤현재" 경계 1회)로
 * 읽는다. 시전자(보스) 사망/전투 종료 시 예약 경고·판정·투사체·장판·레이저를 모두 취소한다.
 * 동시 사망은 플레이어 사망(실패)을 먼저 판정한다 — 1-1과 같은 정책.
 */
public final class BossBattle {

    /** 화면 주입 접점(1-1 Context 확장: 둔화/구속/시야 차폐). */
    public interface Context {
        float playerX();
        float playerY();
        Rectangle playerBounds();
        boolean playerAlive();
        boolean damagePlayer(int damage);
        boolean blockedAt(float centerX, float centerY, float radius); // 벽·기둥 solid
        boolean sightBlocked(float fromX, float fromY, float toX, float toY); // 기둥 차폐
        boolean validFloor(float x, float y);
        void applySlow(float factor, float seconds); // 갱신 방식(중첩 금지)
        void applyRoot(float seconds);               // 이동만 제한(공격/스킬 허용)
    }

    public static final class Projectile {
        public float x, y;
        public final float dirX, dirY, radius, speed, maxDistance;
        public final int damage;
        public final String art;
        public final boolean blockedByPillars;
        float traveled;
        Projectile(float x, float y, float dirX, float dirY, float speed, float maxDistance,
                   float radius, int damage, String art, boolean blockedByPillars) {
            this.x = x; this.y = y; this.dirX = dirX; this.dirY = dirY;
            this.speed = speed; this.maxDistance = maxDistance; this.radius = radius;
            this.damage = damage; this.art = art; this.blockedByPillars = blockedByPillars;
        }
    }

    public static final class Zone {
        public final float x, y, radius;
        public final String art, group;
        public final int damage;
        public final float tickSeconds;
        public final float rootSeconds, rootImmunitySeconds; // >0이면 구속 장판(1회성)
        public float telegraphRemaining;
        public float activeRemaining;
        public float sinceActivation;
        public boolean active;
        boolean bindDone;
        Zone(float x, float y, float radius, float telegraph, float activeSeconds, int damage,
             float tickSeconds, String group, String art, float rootSeconds, float rootImmunity) {
            this.x = x; this.y = y; this.radius = radius;
            this.telegraphRemaining = telegraph;
            this.activeRemaining = activeSeconds;
            this.damage = damage; this.tickSeconds = tickSeconds;
            this.group = group; this.art = art;
            this.rootSeconds = rootSeconds; this.rootImmunitySeconds = rootImmunity;
        }
    }

    public static final class Burst {
        public final float x, y, radius;
        public final int damage;
        public final String art;
        public float delayRemaining;
        Burst(float x, float y, float radius, int damage, float delay, String art) {
            this.x = x; this.y = y; this.radius = radius;
            this.damage = damage; this.delayRemaining = delay; this.art = art;
        }
    }

    public static final class Beam {
        public boolean active;
        public float angleDegrees, angularSpeed, length, width, trimmedLength;
        public int damage;
        public float remaining, tickTimer, tickSeconds;
    }

    public static final class Ring {
        public boolean active;
        public float elapsed, duration, startRadius, endRadius, ringWidth;
        public int damage;
        public boolean hitDone;
        public float currentRadius() {
            float t = Math.min(1f, elapsed / Math.max(0.01f, duration));
            return startRadius + (endRadius - startRadius) * t;
        }
    }

    public static final class ImpactEvent {
        public final float x, y;
        public final String art;
        ImpactEvent(float x, float y, String art) { this.x = x; this.y = y; this.art = art; }
    }

    private final BossStageCatalog.Stage stage;
    private final BossPatternCatalog patterns;
    private final HealthComponent health;
    private final GenRng rng;
    private static final float INTRO_SECONDS = 1f, BETWEEN_SECONDS = 0.6f;
    private static final float DEATH_SECONDS = 0.4f; // 16프레임 시트 death 4프레임 × 0.1s

    private BossFight.Phase phase = BossFight.Phase.ENTERING;
    private float phaseTime;
    private float bossX, bossY;
    private float aimX, aimY = -1f;
    private boolean enraged;
    private float recoveryMultiplier = 1f;
    // 현재 패턴 런타임
    private BossPatternCatalog.Pattern current;
    private float elapsed;
    private boolean aimLocked;
    private boolean released;      // 발사/활성 경계 1회
    private boolean hitDoneThisCast;
    private float patternEndAt;
    private float betweenTimer;
    private final List<String> queue = new ArrayList<>(); // sequence 하위 패턴
    private String lastPatternId;
    // repeat_charge 하위 상태
    private int chargeSubIndex;
    private boolean chargingNow;
    private float chargeTraveled;
    private boolean chargeHitThisSub;
    private float subTelegraphRemaining;
    // ground_trail 드롭 타이머
    private float trailDropTimer;
    // 해저드
    private final List<Projectile> projectiles = new ArrayList<>();
    private final List<Zone> zones = new ArrayList<>();
    private final List<Burst> bursts = new ArrayList<>();
    public final Beam beam = new Beam();
    public final Ring ring = new Ring();
    private final Map<String, Float> tickGates = new LinkedHashMap<>();
    private float rootImmunity;
    private final List<ImpactEvent> impactEvents = new ArrayList<>();

    public BossBattle(BossStageCatalog.Stage stage, BossPatternCatalog patterns,
                      HealthComponent bossHealth, long attemptSeed, float spawnX, float spawnY) {
        this.stage = stage;
        this.patterns = patterns;
        this.health = bossHealth;
        this.rng = new GenRng(Long.toString(attemptSeed), "boss:" + stage.stageId);
        this.bossX = spawnX;
        this.bossY = spawnY;
    }

    // ----- 조회(렌더러/화면) -------------------------------------------------------------

    public BossFight.Phase phase() { return phase; }
    public float phaseTime() { return phaseTime; }
    public float bossX() { return bossX; }
    public float bossY() { return bossY; }
    public HealthComponent health() { return health; }
    public boolean enraged() { return enraged; }
    public float aimDirX() { return aimX; }
    public float aimDirY() { return aimY; }
    public BossPatternCatalog.Pattern currentPattern() { return current; }
    public float patternElapsed() { return elapsed; }
    public boolean charging() { return chargingNow; }
    public List<Projectile> projectiles() { return projectiles; }
    public List<Zone> zones() { return zones; }
    public List<Burst> bursts() { return bursts; }
    public List<ImpactEvent> consumeImpactEvents() {
        List<ImpactEvent> out = new ArrayList<>(impactEvents);
        impactEvents.clear();
        return out;
    }

    /** 애니메이션 세그먼트 선택용: TELEGRAPH/ACTIVE/RECOVERY (전투 외/대기 null). */
    public BossFight.PatternStage patternStage() {
        if (phase != BossFight.Phase.COMBAT || current == null) return null;
        if (isChargeKind(current.kind))
            return chargingNow ? BossFight.PatternStage.ACTIVE
                : subTelegraphRemaining > 0f ? BossFight.PatternStage.TELEGRAPH
                : BossFight.PatternStage.RECOVERY;
        if (elapsed < current.windup) return BossFight.PatternStage.TELEGRAPH;
        return elapsed < current.windup + current.active
            ? BossFight.PatternStage.ACTIVE : BossFight.PatternStage.RECOVERY;
    }

    /** 다음 패턴을 지정(테스트/디버그 — 추첨 대신 큐 우선). */
    public void forceNextPattern(String patternId) { queue.add(patternId); }

    public void beginIntro() { if (phase == BossFight.Phase.ENTERING) setPhase(BossFight.Phase.INTRO); }
    public void markCleared() { if (phase == BossFight.Phase.REWARD_PENDING) setPhase(BossFight.Phase.CLEARED); }
    public boolean beginReturn() {
        if (phase != BossFight.Phase.CLEARED && phase != BossFight.Phase.FAILED) return false;
        setPhase(BossFight.Phase.RETURNING);
        return true;
    }

    private void setPhase(BossFight.Phase next) {
        phase = next;
        phaseTime = 0f;
    }

    // ----- 진행 -------------------------------------------------------------------------

    public void update(float dt, Context ctx) {
        if (dt <= 0f) return;
        phaseTime += dt;
        rootImmunity = Math.max(0f, rootImmunity - dt);
        switch (phase) {
            case INTRO:
                if (phaseTime >= INTRO_SECONDS) setPhase(BossFight.Phase.COMBAT);
                return;
            case COMBAT:
                // 동시 사망: 실패 우선(1-1과 동일 정책 — 보상/실패 동시 저장 금지).
                if (!ctx.playerAlive()) { cancelAll(); setPhase(BossFight.Phase.FAILED); return; }
                if (!health.isAlive()) { cancelAll(); setPhase(BossFight.Phase.DYING); return; }
                updateHazards(dt, ctx);
                updatePattern(dt, ctx);
                return;
            case DYING:
                if (phaseTime >= DEATH_SECONDS) setPhase(BossFight.Phase.REWARD_PENDING);
                return;
            default:
        }
    }

    private void updatePattern(float dt, Context ctx) {
        if (current == null) {
            betweenTimer += dt;
            if (betweenTimer >= BETWEEN_SECONDS * recoveryMultiplier) startNextPattern(ctx);
            return;
        }
        float previous = elapsed;
        elapsed += dt;
        if (isChargeKind(current.kind)) {
            updateRepeatCharge(dt, ctx);
            return;
        }
        if (!aimLocked) {
            trackAim(ctx);
            if (crossed(previous, elapsed, current.targetLockAt)) {
                aimLocked = true;
                onAimLocked(ctx);
            }
        }
        if (!released && crossed(previous, elapsed, current.windup)) {
            released = true;
            onRelease(ctx);
        }
        if (released) duringActive(previous, dt, ctx);
        if (elapsed >= patternEndAt) finishPattern();
    }

    private void startNextPattern(Context ctx) {
        String id;
        if (!queue.isEmpty()) {
            id = queue.remove(0);
        } else if (hasEnragePending()) {
            id = enragePatternId();
        } else {
            String[] ids = selectablePatternIds();
            int guard = 0;
            do {
                id = ids[rng.nextInt(ids.length)];
            } while (guard++ < 16 && ids.length > 1 && id.equals(lastPatternId));
        }
        BossPatternCatalog.Pattern pattern = patterns.pattern(id);
        if ("sequence".equals(pattern.kind)) {
            for (String sub : pattern.subpatterns) queue.add(sub);
            lastPatternId = id;
            betweenTimer = BETWEEN_SECONDS * recoveryMultiplier; // 즉시 첫 하위 패턴
            return;
        }
        // 장판 상한: 가득이면 결정론적으로 다른 패턴 대체(무한 추첨 금지).
        if (isZoneKind(pattern.kind) && activeZoneCount() >= pattern.maxZones) {
            String fallback = fallbackNonZonePattern();
            if (fallback != null) pattern = patterns.pattern(fallback);
        }
        current = pattern;
        elapsed = 0f;
        released = false;
        hitDoneThisCast = false;
        trailDropTimer = 0f;
        aimLocked = pattern.targetLockAt <= 0f;
        trackAim(ctx);
        if (aimLocked) onAimLocked(ctx);
        patternEndAt = pattern.windup + pattern.active
            + pattern.recovery * recoveryMultiplier;
        if (isChargeKind(pattern.kind)) {
            // 단발 charge는 재조준 창=windup 1회, repeat_charge는 회차 예고 간격 사용.
            chargeSubIndex = 0;
            chargingNow = false;
            subTelegraphRemaining = "charge".equals(pattern.kind)
                ? pattern.windup : pattern.betweenChargeTelegraph;
            patternEndAt = Float.MAX_VALUE; // 하위 상태기에서 종료
        }
        if ("phase_change".equals(pattern.kind)) cancelHazardsOnly(); // 전환 중 새 판정 없음
    }

    private void finishPattern() {
        if (current != null && "phase_change".equals(current.kind)) {
            enraged = true;
            recoveryMultiplier = current.recoveryMultiplierAfter;
        }
        lastPatternId = current == null ? null : current.id;
        current = null;
        chargingNow = false;
        betweenTimer = 0f;
    }

    private String[] selectablePatternIds() {
        List<String> ids = new ArrayList<>();
        for (String id : stage.patternIds)
            if (!id.equals(enragePatternId())) ids.add(id); // 분노 전환은 HP 조건 전용
        return ids.toArray(new String[0]);
    }

    private String fallbackNonZonePattern() {
        for (String id : selectablePatternIds())
            if (!isZoneKind(patterns.pattern(id).kind)) return id;
        return null;
    }

    private static boolean isZoneKind(String kind) {
        return "ground".equals(kind) || "ground_bind".equals(kind) || "ground_trail".equals(kind);
    }

    private int activeZoneCount() { return zones.size(); }

    private String enragePatternId() {
        for (String id : stage.patternIds)
            if ("phase_change".equals(patterns.pattern(id).kind)) return id;
        return "";
    }

    private boolean hasEnragePending() {
        String id = enragePatternId();
        if (id.isEmpty() || enraged) return false;
        BossPatternCatalog.Pattern pattern = patterns.pattern(id);
        return health.hp() <= health.maxHp() * pattern.triggerHpFraction;
    }

    private void trackAim(Context ctx) {
        float dx = ctx.playerX() - bossX, dy = ctx.playerY() - bossY;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length > 0.001f) { aimX = dx / length; aimY = dy / length; }
    }

    /** 타깃 고정 시점: 장판/폭발류는 이때 낙하 지점을 확정한다(이후 추적 금지). */
    private void onAimLocked(Context ctx) {
        if (current == null) return;
        String kind = current.kind;
        if ("ground".equals(kind) || "ground_bind".equals(kind)) {
            if (activeZoneCount() >= current.maxZones) return;
            float[] target = nearestValidFloor(ctx, ctx.playerX(), ctx.playerY());
            zones.add(new Zone(target[0], target[1], current.radius,
                current.windup - current.targetLockAt, current.active,
                current.damage, current.tickSeconds, current.sharedTickGroup, current.art,
                current.rootSeconds, current.rootImmunitySeconds));
        } else if ("ground_burst".equals(kind) || "moving_ground_burst".equals(kind)) {
            // 착탄 위치는 발사(고정) 때 확정 — 1초 이상 예고 후 고정 지연 폭발.
            float baseDelay = Math.max(1f, current.windup - current.targetLockAt);
            for (int i = 0; i < current.targetCount; i++) {
                double angle = rng.nextInt(360) * Math.PI / 180.0;
                float distance = i == 0 ? 0f
                    : 1.5f + (current.targetSeparation - 1.5f) * (rng.nextInt(100) / 100f);
                float[] target = nearestValidFloor(ctx,
                    ctx.playerX() + (float) Math.cos(angle) * distance,
                    ctx.playerY() + (float) Math.sin(angle) * distance);
                bursts.add(new Burst(target[0], target[1], current.radius, current.damage,
                    baseDelay + i * 0.3f, current.art));
            }
        }
    }

    /** 발사/활성 경계(windup 종료) 1회. */
    private void onRelease(Context ctx) {
        String kind = current.kind;
        float muzzleY = bossY + 1.2f;
        if ("projectile".equals(kind)) {
            projectiles.add(new Projectile(bossX, muzzleY, aimX, aimY,
                current.projectileSpeed, 18f, 0.2f, current.damage, current.art,
                current.blockedByPillars));
        } else if ("projectile_fan".equals(kind)) {
            double base = Math.atan2(aimY, aimX);
            for (int i = 0; i < current.count; i++) {
                double offset = current.count == 1 ? 0
                    : Math.toRadians(-current.angleDegrees / 2f
                        + current.angleDegrees * i / (current.count - 1));
                projectiles.add(new Projectile(bossX, muzzleY,
                    (float) Math.cos(base + offset), (float) Math.sin(base + offset),
                    current.projectileSpeed, 18f, 0.2f, current.damage, current.art,
                    current.blockedByPillars));
            }
        } else if ("beam".equals(kind)) {
            beam.active = true;
            beam.damage = current.damage;
            beam.length = current.length;
            beam.width = current.width;
            beam.angularSpeed = current.angularSpeedDegrees;
            beam.remaining = current.active;
            beam.tickSeconds = current.tickSeconds;
            beam.tickTimer = 0f;
            // 회전 예고: 고정된 조준에서 반(sweep/2) 뒤로 시작해 일정 각속도로 쓸고 간다.
            beam.angleDegrees = (float) Math.toDegrees(Math.atan2(aimY, aimX))
                - current.angularSpeedDegrees * current.active / 2f;
        } else if ("expanding_annulus".equals(kind)) {
            ring.active = true;
            ring.elapsed = 0f;
            ring.duration = current.active;
            ring.startRadius = current.startRadius;
            ring.endRadius = current.endRadius;
            ring.ringWidth = current.ringWidth;
            ring.damage = current.damage;
            ring.hitDone = false;
        }
    }

    /** ACTIVE 구간 유지 판정(부채꼴 1회, 트레일 드롭, 이동 포격의 이동). */
    private void duringActive(float previous, float dt, Context ctx) {
        if (elapsed > current.windup + current.active) return;
        String kind = current.kind;
        if ("cone".equals(kind) && !hitDoneThisCast) {
            if (playerInCone(ctx) && !(current.occludedByPillars
                && ctx.sightBlocked(bossX, bossY + 1f, ctx.playerX(), ctx.playerY() + 0.25f))) {
                hitDoneThisCast = true;
                if (ctx.damagePlayer(current.damage) && current.slowSeconds > 0f)
                    ctx.applySlow(current.slowFactor, current.slowSeconds); // 갱신 방식
            }
        } else if ("ground_trail".equals(kind)) {
            trailDropTimer -= dt;
            float moveSpeed = current.moveSpeed > 0f ? current.moveSpeed : 1.2f;
            moveBossToward(ctx, ctx.playerX(), ctx.playerY(), moveSpeed * dt);
            if (trailDropTimer <= 0f && zones.size() < current.maxZones) {
                trailDropTimer = current.dropInterval;
                float[] target = nearestValidFloor(ctx, bossX, bossY);
                zones.add(new Zone(target[0], target[1], current.radius, 0f,
                    current.lifetimeSeconds, current.damage, current.tickSeconds,
                    current.sharedTickGroup, current.art, 0f, 0f));
            }
        } else if ("moving_ground_burst".equals(kind)) {
            moveBossToward(ctx, ctx.playerX(), ctx.playerY(), current.moveSpeed * dt);
        }
    }

    private boolean playerInCone(Context ctx) {
        float dx = ctx.playerX() - bossX, dy = ctx.playerY() + 0.25f - (bossY + 1f);
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        if (distance > current.radius || distance < 0.001f) return false;
        double angleTo = Math.atan2(dy, dx);
        double aim = Math.atan2(aimY, aimX);
        double diff = Math.abs(normalizeAngle(angleTo - aim));
        return Math.toDegrees(diff) <= current.angleDegrees / 2f;
    }

    private static double normalizeAngle(double radians) {
        while (radians > Math.PI) radians -= Math.PI * 2;
        while (radians < -Math.PI) radians += Math.PI * 2;
        return radians;
    }

    private static boolean isChargeKind(String kind) {
        return "charge".equals(kind) || "repeat_charge".equals(kind);
    }

    private int chargeCount() { return "charge".equals(current.kind) ? 1 : current.count; }

    private float chargeDistance() {
        return current.distanceEach > 0f ? current.distanceEach : current.maxDistance;
    }

    /** 연속 돌진: 회차마다 방향을 다시 예고·고정(보이지 않는 유도 금지), swept 정지. */
    private void updateRepeatCharge(float dt, Context ctx) {
        if (chargeSubIndex >= chargeCount() && !chargingNow) {
            // 마지막 회차 후 회복.
            subTelegraphRemaining -= dt;
            if (subTelegraphRemaining <= -current.recovery * recoveryMultiplier) finishPattern();
            return;
        }
        if (!chargingNow) {
            trackAim(ctx); // 예고 동안만 재조준
            subTelegraphRemaining -= dt;
            if (subTelegraphRemaining <= 0f) {
                chargingNow = true; // 방향 고정
                chargeTraveled = 0f;
                chargeHitThisSub = false;
            }
            return;
        }
        float footRadius = stage.footprintW / 2f;
        float distance = Math.min(current.speed * dt, chargeDistance() - chargeTraveled);
        int steps = Math.max(1, (int) Math.ceil(distance / 0.1f));
        float step = distance / steps;
        for (int i = 0; i < steps; i++) {
            float nx = bossX + aimX * step, ny = bossY + aimY * step;
            if (ctx.blockedAt(nx, ny + footRadius * 0.4f, footRadius)) {
                impactEvents.add(new ImpactEvent(nx, ny + footRadius * 0.4f, "ice_impact"));
                endChargeSub();
                return;
            }
            bossX = nx;
            bossY = ny;
            chargeTraveled += step;
            if (!chargeHitThisSub && circleOverlaps(bossX, bossY + footRadius * 0.4f,
                current.laneWidth / 2f, ctx.playerBounds())) {
                chargeHitThisSub = true; // 회차별 1회
                ctx.damagePlayer(current.damage);
            }
        }
        if (chargeTraveled >= chargeDistance() - 0.0001f) endChargeSub();
    }

    private void endChargeSub() {
        chargingNow = false;
        chargeSubIndex++;
        subTelegraphRemaining = chargeSubIndex < chargeCount()
            ? current.betweenChargeTelegraph : 0f;
    }

    private void moveBossToward(Context ctx, float targetX, float targetY, float distance) {
        float dx = targetX - bossX, dy = targetY - bossY;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 2.5f) return; // 안전 거리 유지(몸통 압사 금지)
        float footRadius = stage.footprintW / 2f;
        float nx = bossX + dx / length * distance, ny = bossY + dy / length * distance;
        if (!ctx.blockedAt(nx, ny + footRadius * 0.4f, footRadius)) {
            bossX = nx;
            bossY = ny;
        }
    }

    // ----- 해저드 ----------------------------------------------------------------------

    private void updateHazards(float dt, Context ctx) {
        // 투사체(swept — 기둥/벽 첫 충돌 소멸, 뒤 플레이어 무피해).
        Iterator<Projectile> it = projectiles.iterator();
        while (it.hasNext()) {
            Projectile p = it.next();
            float distance = Math.min(p.speed * dt, p.maxDistance - p.traveled);
            int steps = Math.max(1, (int) Math.ceil(distance / Math.max(0.01f, p.radius * 0.9f)));
            float step = distance / steps;
            boolean removed = false;
            for (int i = 0; i < steps; i++) {
                float nx = p.x + p.dirX * step, ny = p.y + p.dirY * step;
                if (p.blockedByPillars && ctx.blockedAt(nx, ny, p.radius)) {
                    impactEvents.add(new ImpactEvent(nx, ny, impactArtFor(p.art)));
                    it.remove();
                    removed = true;
                    break;
                }
                if (circleOverlaps(nx, ny, p.radius, ctx.playerBounds())) {
                    ctx.damagePlayer(p.damage);
                    impactEvents.add(new ImpactEvent(nx, ny, impactArtFor(p.art)));
                    it.remove();
                    removed = true;
                    break;
                }
                p.x = nx;
                p.y = ny;
                p.traveled += step;
            }
            if (!removed && p.traveled >= p.maxDistance - 0.0001f) it.remove();
        }
        // 장판(전역 틱 + 그룹 공유 간격 — 겹침 폭딜 방지).
        for (Map.Entry<String, Float> gate : tickGates.entrySet())
            gate.setValue(Math.max(0f, gate.getValue() - dt));
        Iterator<Zone> zoneIt = zones.iterator();
        while (zoneIt.hasNext()) {
            Zone zone = zoneIt.next();
            if (!zone.active) {
                zone.telegraphRemaining -= dt;
                if (zone.telegraphRemaining > 0f) continue;
                zone.active = true; // 경고와 같은 기하로 활성화
            } else {
                zone.sinceActivation += dt;
                zone.activeRemaining -= dt;
            }
            // 그룹 게이트가 곧 틱 스케줄: 겹친 장판 위에 서 있으면 그룹당 tickSeconds마다 1회.
            tickZone(zone, ctx);
            if (zone.activeRemaining <= 0f) zoneIt.remove();
        }
        // 지연 폭발/포탄(착탄 위치 고정 — 곡사라 기둥 뒤 도달).
        Iterator<Burst> burstIt = bursts.iterator();
        while (burstIt.hasNext()) {
            Burst burst = burstIt.next();
            burst.delayRemaining -= dt;
            if (burst.delayRemaining <= 0f) {
                if (circleOverlaps(burst.x, burst.y, burst.radius, ctx.playerBounds()))
                    ctx.damagePlayer(burst.damage);
                impactEvents.add(new ImpactEvent(burst.x, burst.y, burst.art));
                burstIt.remove();
            }
        }
        // 회전 레이저(매 프레임 첫 차폐물까지 판정·표시 길이 동시 자름).
        if (beam.active) {
            beam.angleDegrees += beam.angularSpeed * dt;
            beam.remaining -= dt;
            beam.trimmedLength = beamTrimmedLength(ctx);
            beam.tickTimer -= dt;
            if (beam.tickTimer <= 0f) {
                beam.tickTimer = beam.tickSeconds;
                if (playerOnBeam(ctx)) ctx.damagePlayer(beam.damage);
            }
            if (beam.remaining <= 0f) beam.active = false;
        }
        // 확장 고리(한 cast 대상당 1회, 차폐 방향 차단).
        if (ring.active) {
            ring.elapsed += dt;
            float r = ring.currentRadius();
            if (!ring.hitDone) {
                float dx = ctx.playerX() - bossX, dy = ctx.playerY() + 0.25f - (bossY + 1f);
                float distance = (float) Math.sqrt(dx * dx + dy * dy);
                if (Math.abs(distance - r) <= ring.ringWidth / 2f
                    && !ctx.sightBlocked(bossX, bossY + 1f, ctx.playerX(), ctx.playerY() + 0.25f)) {
                    ring.hitDone = true;
                    ctx.damagePlayer(ring.damage);
                }
            }
            if (ring.elapsed >= ring.duration) ring.active = false;
        }
    }

    private void tickZone(Zone zone, Context ctx) {
        if (!circleOverlaps(zone.x, zone.y, zone.radius, ctx.playerBounds())) return;
        if (zone.rootSeconds > 0f) {
            // 구속 장판(뿌리): 1회성 — 재피격 면역 중에는 걸리지 않는다.
            if (zone.bindDone || rootImmunity > 0f) return;
            zone.bindDone = true;
            if (ctx.damagePlayer(zone.damage)) {
                ctx.applyRoot(zone.rootSeconds);
                rootImmunity = zone.rootImmunitySeconds;
            }
            return;
        }
        Float gate = tickGates.get(zone.group);
        if (gate != null && gate > 0f) return; // 같은 그룹 겹침은 간격 공유
        if (ctx.damagePlayer(zone.damage)) tickGates.put(zone.group, zone.tickSeconds);
    }

    private float beamTrimmedLength(Context ctx) {
        double angle = Math.toRadians(beam.angleDegrees);
        float dirX = (float) Math.cos(angle), dirY = (float) Math.sin(angle);
        float originX = bossX, originY = bossY + 1f;
        for (float d = 0.6f; d <= beam.length; d += 0.3f)
            if (ctx.blockedAt(originX + dirX * d, originY + dirY * d, 0.2f)) return d;
        return beam.length;
    }

    private boolean playerOnBeam(Context ctx) {
        double angle = Math.toRadians(beam.angleDegrees);
        float dirX = (float) Math.cos(angle), dirY = (float) Math.sin(angle);
        float originX = bossX, originY = bossY + 1f;
        float px = ctx.playerX() - originX, py = ctx.playerY() + 0.25f - originY;
        float along = px * dirX + py * dirY;
        if (along < 0f || along > beam.trimmedLength) return false; // 차폐물 뒤 무피해
        float side = Math.abs(px * -dirY + py * dirX);
        return side <= beam.width / 2f + 0.3f;
    }

    private String impactArtFor(String projectileArt) {
        if ("ice_shard".equals(projectileArt)) return "ice_impact";
        if ("red_laser".equals(projectileArt)) return "psychic_blast";
        return "acid_splash";
    }

    private void cancelHazardsOnly() {
        projectiles.clear();
        zones.clear();
        bursts.clear();
        beam.active = false;
        ring.active = false;
    }

    private void cancelAll() {
        cancelHazardsOnly();
        current = null;
        queue.clear();
        chargingNow = false;
        impactEvents.clear();
    }

    private float[] nearestValidFloor(Context ctx, float x, float y) {
        if (ctx.validFloor(x, y)) return new float[]{x, y};
        for (float r = 0.5f; r <= 5f; r += 0.5f)
            for (int i = 0; i < 16; i++) {
                double a = Math.PI * 2 * i / 16;
                float cx = x + (float) Math.cos(a) * r, cy = y + (float) Math.sin(a) * r;
                if (ctx.validFloor(cx, cy)) return new float[]{cx, cy};
            }
        return new float[]{x, y};
    }

    private static boolean crossed(float previous, float now, float eventTime) {
        return eventTime <= 0f ? previous <= 0f : previous < eventTime && eventTime <= now;
    }

    private static boolean circleOverlaps(float cx, float cy, float r, Rectangle rect) {
        float nx = Math.max(rect.x, Math.min(cx, rect.x + rect.width));
        float ny = Math.max(rect.y, Math.min(cy, rect.y + rect.height));
        float dx = cx - nx, dy = cy - ny;
        return dx * dx + dy * dy <= r * r;
    }
}

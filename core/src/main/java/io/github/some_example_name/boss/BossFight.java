package io.github.some_example_name.boss;

import com.badlogic.gdx.math.Rectangle;
import io.github.some_example_name.combat.HealthComponent;
import io.github.some_example_name.world.GenRng;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 보스 1-1 전투 상태 기계(순수 Java — GL/화면 비의존, Context 주입).
 * 상태: ENTERING → INTRO → COMBAT → DYING → REWARD_PENDING → CLEARED → RETURNING,
 * 플레이어 사망은 FAILED 분기. 전이는 이 클래스 한 곳에서만 일어난다(Screen boolean 조합 금지).
 *
 * 패턴은 한 번에 하나(TELEGRAPH → ACTIVE → RECOVERY), 초기 1회씩 투척→장판→돌진을 보여준 뒤
 * 시드 가중 선택(즉시 반복 금지). 모든 이벤트는 같은 시뮬레이션 시계에서 "이전<t≤현재" 경계로
 * 1회 실행되고(0초 이벤트는 시작 시), 큰 delta로 여러 경계를 건너도 시간순으로 모두 처리된다.
 * 일시정지는 update를 호출하지 않는 것으로 전체(패턴/투사체/장판/돌진)가 함께 멈춘다.
 *
 * 같은 시뮬레이션 시각에 플레이어와 보스가 함께 죽으면 실패를 우선한다(보상/실패 동시 저장 금지).
 * 사망/실패 전이 시 예약 이벤트·투사체·장판·돌진 판정을 전부 취소한다 — 늦은 타이머가 결과창
 * 뒤에서 플레이어를 때리지 않는다.
 */
public final class BossFight {

    public enum Phase { ENTERING, INTRO, COMBAT, DYING, REWARD_PENDING, CLEARED, RETURNING, FAILED }
    public enum PatternStage { TELEGRAPH, ACTIVE, RECOVERY }

    /** 화면 주입 접점: 플레이어 상태·공통 피해 파이프라인·solid 충돌·장판 착지 가능 바닥. */
    public interface Context {
        float playerX();
        float playerY();
        Rectangle playerBounds();
        boolean playerAlive();
        /** 기존 보호막→HP 공통 파이프라인 1회 호출. 수용되면 true(무적 거절 시 false). */
        boolean damagePlayer(int damage);
        /** 벽·기둥·닫힌 문 등 공용 solid(반경 몸체). */
        boolean blockedAt(float centerX, float centerY, float radius);
        /** 장판이 떨어질 수 있는 통행 가능 바닥(벽 밖/기둥 footprint 제외). */
        boolean validFloor(float x, float y);
    }

    public static final class BossProjectile {
        public float x, y;
        public final float dirX, dirY;
        float traveled;
        public boolean alive = true;
        BossProjectile(float x, float y, float dirX, float dirY) {
            this.x = x;
            this.y = y;
            this.dirX = dirX;
            this.dirY = dirY;
        }
    }

    public static final class AcidPool {
        public final float x, y, radius;
        float telegraphRemaining;
        float activeRemaining;
        float sinceActivation; // 활성 후 경과(프레임 선택·첫 틱)
        boolean active;
        AcidPool(float x, float y, float radius, float telegraphSeconds, float activeSeconds) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            telegraphRemaining = telegraphSeconds;
            activeRemaining = activeSeconds;
        }
        public boolean active() { return active; }
        public float telegraphRemaining() { return telegraphRemaining; }
        public float sinceActivation() { return sinceActivation; }
        public float activeRemaining() { return activeRemaining; }
    }

    private final BossDefinition def;
    private final HealthComponent health;
    private final GenRng rng;
    public final float arenaBossSpawnX, arenaBossSpawnY;

    private Phase phase = Phase.ENTERING;
    private float phaseTime;
    private float bossX, bossY;
    // 패턴 상태
    private String patternId;
    private float patternElapsed;
    private boolean aimLocked;
    private float aimDirX, aimDirY = -1f; // 0벡터 폴백: 마지막 유효 방향(초기 남쪽)
    private float patternEndAt;    // 이 시각에 패턴 종료(회복 포함)
    private float activeEndAt;     // ACTIVE 구간 종료(애니메이션 선택용)
    private int firstSequenceIndex;
    private String lastPatternId;
    private float betweenTimer;
    // 돌진
    private boolean charging;
    private float chargeTraveled;
    private boolean chargeHitDone;
    private float chargeStopAt = Float.NaN; // 정지 시각(회복 타이머 기준)
    // 투사체·장판
    private final List<BossProjectile> projectiles = new ArrayList<>();
    private final List<AcidPool> pools = new ArrayList<>();
    private float acidGate; // 플레이어 공통 장판 틱 간격(겹침 폭딜 방지)
    // VFX 소비 이벤트(착탄 위치)
    private final List<float[]> impactEvents = new ArrayList<>();

    public BossFight(BossDefinition def, HealthComponent bossHealth, long attemptSeed,
                     float spawnX, float spawnY) {
        this.def = def;
        this.health = bossHealth;
        this.rng = new GenRng(Long.toString(attemptSeed), "boss:" + def.stageId);
        this.bossX = spawnX;
        this.bossY = spawnY;
        this.arenaBossSpawnX = spawnX;
        this.arenaBossSpawnY = spawnY;
    }

    public Phase phase() { return phase; }
    public float phaseTime() { return phaseTime; }
    public float bossX() { return bossX; }
    public float bossY() { return bossY; }
    public HealthComponent health() { return health; }
    public String patternId() { return patternId; }
    public float patternElapsed() { return patternElapsed; }
    public boolean aimLocked() { return aimLocked; }
    public float aimDirX() { return aimDirX; }
    public float aimDirY() { return aimDirY; }
    public boolean charging() { return charging; }
    public List<BossProjectile> projectiles() { return projectiles; }
    public List<AcidPool> pools() { return pools; }
    public List<float[]> consumeImpactEvents() {
        List<float[]> out = new ArrayList<>(impactEvents);
        impactEvents.clear();
        return out;
    }

    /** 현재 패턴 구간(애니메이션 세그먼트 선택용). COMBAT 외에는 null. */
    public PatternStage patternStage() {
        if (phase != Phase.COMBAT || patternId == null) return null;
        float telegraph = telegraphSeconds(patternId);
        if (patternElapsed < telegraph) return PatternStage.TELEGRAPH;
        return patternElapsed < activeEndAt ? PatternStage.ACTIVE : PatternStage.RECOVERY;
    }

    /** 화면 전환(페이드) 완료 후 1회 — 소개 시작. */
    public void beginIntro() {
        if (phase == Phase.ENTERING) setPhase(Phase.INTRO);
    }

    /** 보상 원자 저장 성공 후 1회 — CLEARED(귀환문 개방). */
    public void markCleared() {
        if (phase == Phase.REWARD_PENDING) setPhase(Phase.CLEARED);
    }

    /** 복귀 확정(버튼/대문) 1회 — 이후 재호출은 무시된다. */
    public boolean beginReturn() {
        if (phase != Phase.CLEARED && phase != Phase.FAILED) return false;
        setPhase(Phase.RETURNING);
        return true;
    }

    private void setPhase(Phase next) {
        phase = next;
        phaseTime = 0f;
    }

    public void update(float dt, Context ctx) {
        if (dt <= 0f) return;
        phaseTime += dt;
        switch (phase) {
            case INTRO:
                // 소개 정책(데이터 명시): 보스 공격·보스발 피해 없음, 플레이어 이동 허용.
                if (phaseTime >= def.introSeconds) {
                    setPhase(Phase.COMBAT);
                    betweenTimer = 0f;
                }
                return;
            case COMBAT:
                updateCombat(dt, ctx);
                return;
            case DYING:
                if (phaseTime >= 0.6f) setPhase(Phase.REWARD_PENDING); // 사망 6프레임 후 유지
                return;
            default:
        }
    }

    private void updateCombat(float dt, Context ctx) {
        // 동시 사망: 플레이어 사망을 먼저 판정(실패 우선 — 보상과 실패가 함께 저장되지 않는다).
        if (!ctx.playerAlive()) {
            cancelAllHazards();
            setPhase(Phase.FAILED);
            return;
        }
        if (!health.isAlive()) {
            cancelAllHazards();
            setPhase(Phase.DYING);
            return;
        }
        updateHazards(dt, ctx);
        if (patternId == null) {
            betweenTimer += dt;
            if (betweenTimer >= def.betweenPatternSeconds) startNextPattern(ctx);
            return;
        }
        float previous = patternElapsed;
        patternElapsed += dt;
        float lockAt = aimLockSeconds(patternId);
        if (!aimLocked) {
            trackAim(ctx);
            if (crossed(previous, patternElapsed, lockAt)) aimLocked = true;
        }
        if ("reagent_throw".equals(patternId)) {
            for (int i = 0; i < def.throwShotTimes.length; i++)
                if (crossed(previous, patternElapsed, def.throwShotTimes[i]))
                    spawnProjectile(def.throwShotAngles[i]);
        } else if ("contamination".equals(patternId)) {
            if (crossed(previous, patternElapsed, 0f) && pools.size() < def.poolMaxActive) {
                // 시전 시작에 목표 바닥 고정(추적 이동 금지). 벽/기둥 안이면 결정론 보정.
                float[] target = nearestValidFloor(ctx, ctx.playerX(), ctx.playerY());
                pools.add(new AcidPool(target[0], target[1], def.poolRadius,
                    def.poolActivateAt, def.poolActiveSeconds));
            }
        } else if ("ram".equals(patternId)) {
            if (crossed(previous, patternElapsed, def.ramChargeStartAt)) {
                charging = true;
                chargeTraveled = 0f;
                chargeHitDone = false;
                chargeStopAt = Float.NaN;
            }
            if (charging) updateCharge(dt, ctx);
            // 돌진 정지 후 회복: 정지 시각 + recoveryAfterStopSeconds까지가 패턴 종료.
            if (!charging && !Float.isNaN(chargeStopAt))
                patternEndAt = chargeStopAt + def.ramRecoverySeconds;
        }
        if (patternElapsed >= patternEndAt) {
            lastPatternId = patternId;
            patternId = null;
            betweenTimer = 0f;
            charging = false;
        }
    }

    private void startNextPattern(Context ctx) {
        String next;
        if (firstSequenceIndex < def.firstPatternSequence.length) {
            next = def.firstPatternSequence[firstSequenceIndex++];
        } else {
            String[] ids = {"reagent_throw", "contamination", "ram"};
            int[] weights = {def.throwWeight, def.poolWeight, def.ramWeight};
            int guard = 0;
            do {
                next = ids[rng.weightedIndex(weights)];
            } while (guard++ < 16
                && ((def.avoidImmediateRepeat && next.equals(lastPatternId))
                    || ("contamination".equals(next) && pools.size() >= def.poolMaxActive)));
            if ("contamination".equals(next) && pools.size() >= def.poolMaxActive)
                next = "reagent_throw"; // 장판 상한: 결정론적 대체(무한 추첨 금지)
        }
        patternId = next;
        patternElapsed = 0f;
        aimLocked = aimLockSeconds(next) <= 0f; // 0초 고정은 시작 시 1회 처리
        trackAim(ctx);
        if ("reagent_throw".equals(next)) {
            activeEndAt = def.throwShotTimes[def.throwShotTimes.length - 1];
            patternEndAt = def.throwDuration;
        } else if ("contamination".equals(next)) {
            activeEndAt = def.poolActivateAt;
            patternEndAt = def.poolDuration;
        } else {
            activeEndAt = Float.MAX_VALUE;        // 돌진 종료 시 재계산
            patternEndAt = Float.MAX_VALUE;
            chargeStopAt = Float.NaN;
        }
    }

    private void trackAim(Context ctx) {
        float dx = ctx.playerX() - bossX, dy = ctx.playerY() - bossY;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length > 0.001f) { // 0벡터면 마지막 유효 방향 유지
            aimDirX = dx / length;
            aimDirY = dy / length;
        }
    }

    private void spawnProjectile(float angleDegrees) {
        double base = Math.atan2(aimDirY, aimDirX) + Math.toRadians(angleDegrees);
        // 발사구: 몸체 중앙 포탑 높이(muzzleAnchor 96,128 ≈ 발 위 1.2타일).
        projectiles.add(new BossProjectile(bossX, bossY + 1.2f,
            (float) Math.cos(base), (float) Math.sin(base)));
    }

    private void updateHazards(float dt, Context ctx) {
        // 투사체: swept(샘플 ≤ 반경) — 기둥/벽/닫힌 문/플레이어 중 가장 먼저 닿은 대상 처리.
        Iterator<BossProjectile> it = projectiles.iterator();
        while (it.hasNext()) {
            BossProjectile p = it.next();
            float distance = Math.min(def.throwSpeed * dt, def.throwMaxDistance - p.traveled);
            int steps = Math.max(1, (int) Math.ceil(distance / Math.max(0.01f, def.throwRadius * 0.9f)));
            float stepLength = distance / steps;
            boolean removed = false;
            for (int i = 0; i < steps; i++) {
                float nx = p.x + p.dirX * stepLength, ny = p.y + p.dirY * stepLength;
                if (ctx.blockedAt(nx, ny, def.throwRadius)) {
                    impactEvents.add(new float[]{nx, ny}); // 착탄만 — 뒤의 플레이어 무피해
                    it.remove();
                    removed = true;
                    break;
                }
                if (circleOverlaps(nx, ny, def.throwRadius, ctx.playerBounds())) {
                    ctx.damagePlayer(def.throwDamage); // 무적/보호막은 파이프라인이 판정
                    impactEvents.add(new float[]{nx, ny});
                    it.remove();
                    removed = true;
                    break;
                }
                p.x = nx;
                p.y = ny;
                p.traveled += stepLength;
            }
            if (!removed && p.traveled >= def.throwMaxDistance - 0.0001f)
                it.remove(); // 수명 종료 — 착탄 효과 없이 제거(장판을 남기지 않는다)
        }
        // 장판: 예고 → 활성 → 만료. 틱은 전역 일정 + 플레이어 공통 장판 간격(겹침 1틱 제한).
        acidGate = Math.max(0f, acidGate - dt);
        Iterator<AcidPool> poolIt = pools.iterator();
        while (poolIt.hasNext()) {
            AcidPool pool = poolIt.next();
            if (!pool.active) {
                pool.telegraphRemaining -= dt;
                if (pool.telegraphRemaining <= 0f) {
                    pool.active = true;
                    pool.sinceActivation = Math.max(0f, -pool.telegraphRemaining);
                    if (def.poolFirstTickAtActivation) tryAcidTick(pool, ctx); // 활성 순간 첫 틱
                }
                continue;
            }
            float prevSince = pool.sinceActivation;
            pool.sinceActivation += dt;
            pool.activeRemaining -= dt;
            // 전역 틱 일정(활성 기준 tickSeconds 간격) — 경계 재진입으로 리셋/연타되지 않는다.
            int prevTicks = (int) Math.floor(prevSince / def.poolTickSeconds);
            int nowTicks = (int) Math.floor(pool.sinceActivation / def.poolTickSeconds);
            for (int t = prevTicks; t < nowTicks; t++) tryAcidTick(pool, ctx);
            if (pool.activeRemaining <= 0f) poolIt.remove(); // 만료 — 판정 즉시 해제
        }
    }

    /** 장판 틱: 실제 발 hurtbox 겹침 + 플레이어 공통 장판 간격 게이트(중복/겹침 폭딜 방지). */
    private void tryAcidTick(AcidPool pool, Context ctx) {
        if (acidGate > 0f) return;
        if (!circleOverlaps(pool.x, pool.y, pool.radius, ctx.playerBounds())) return;
        if (ctx.damagePlayer(def.poolDamagePerTick)) acidGate = def.poolTickSeconds;
    }

    private void updateCharge(float dt, Context ctx) {
        float bodyRadius = def.ramColliderWidth / 2f;
        float distance = Math.min(def.ramSpeed * dt, def.ramMaxDistance - chargeTraveled);
        int steps = Math.max(1, (int) Math.ceil(distance / 0.1f));
        float stepLength = distance / steps;
        for (int i = 0; i < steps; i++) {
            float nx = bossX + aimDirX * stepLength, ny = bossY + aimDirY * stepLength;
            // 벽·기둥·닫힌 문: 접촉 표면 앞 즉시 정지(관통/순간이동 없음) → 회복 전환.
            if (ctx.blockedAt(nx, ny + bodyRadius * 0.5f, bodyRadius)) {
                impactEvents.add(new float[]{nx + aimDirX * bodyRadius, ny + aimDirY * bodyRadius});
                stopCharge();
                return;
            }
            bossX = nx;
            bossY = ny;
            chargeTraveled += stepLength;
            // 실제 돌진 구간 교차에만 1돌진 1회 피해(정지/회복 중 겹침 무피해).
            if (!chargeHitDone && circleOverlaps(bossX, bossY + bodyRadius * 0.5f,
                bodyRadius, ctx.playerBounds())) {
                chargeHitDone = true;
                ctx.damagePlayer(def.ramDamage);
            }
        }
        if (chargeTraveled >= def.ramMaxDistance - 0.0001f) stopCharge();
    }

    private void stopCharge() {
        charging = false;
        chargeStopAt = patternElapsed;
        activeEndAt = patternElapsed;
    }

    private void cancelAllHazards() {
        projectiles.clear();
        pools.clear();
        charging = false;
        patternId = null;
        impactEvents.clear();
    }

    /** 결정론 보정: 목표가 벽/기둥 안이면 가장 가까운 유효 바닥(고정 나선 순서). */
    private static float[] nearestValidFloor(Context ctx, float x, float y) {
        if (ctx.validFloor(x, y)) return new float[]{x, y};
        for (float r = 0.5f; r <= 4f; r += 0.5f)
            for (int i = 0; i < 16; i++) {
                double a = Math.PI * 2 * i / 16;
                float cx = x + (float) Math.cos(a) * r, cy = y + (float) Math.sin(a) * r;
                if (ctx.validFloor(cx, cy)) return new float[]{cx, cy};
            }
        return new float[]{x, y};
    }

    private float telegraphSeconds(String id) {
        if ("reagent_throw".equals(id)) return def.throwTelegraph;
        if ("contamination".equals(id)) return def.poolTelegraph;
        return def.ramTelegraph;
    }

    private float aimLockSeconds(String id) {
        if ("reagent_throw".equals(id)) return def.throwAimLock;
        if ("contamination".equals(id)) return 0f; // 시전 시작에 바닥 고정
        return def.ramAimLock;
    }

    /** 이벤트 경계: 이전 < t ≤ 현재(t=0은 시작 시 1회). */
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

package io.github.some_example_name.entity.monster;

import com.badlogic.gdx.math.Rectangle;
import io.github.some_example_name.combat.HealthComponent;
import io.github.some_example_name.combat.MonsterState;
import io.github.some_example_name.combat.MonsterType;
import io.github.some_example_name.entity.Direction;

/** Domain state and collision body for one laboratory monster. */
public final class Monster {
    private final long id;
    private final MonsterType type;
    private final Rectangle bounds;
    private final HealthComponent health;
    private MonsterState state = MonsterState.IDLE;
    private Direction facing = Direction.DOWN;
    private float stateTime;
    private float wanderTime;
    private float attackCooldown;
    private float hitTime;
    private float knockbackX, knockbackY, knockbackTime;
    private boolean enraged;
    private boolean aggro;
    private int nearbyRunStarts;
    private boolean attackHit;
    // 배회/탐색/복귀 목적지(월드 좌표). NaN이면 없음.
    private float roamTargetX = Float.NaN, roamTargetY = Float.NaN;
    // 활동 구역(홈) = 스폰 지점. 배회·순찰·복귀의 기준점이며 추적 이탈 한계 판정에 쓴다.
    private final float homeX, homeY;
    // 마지막으로 "실제로 본" 플레이어 위치. 벽 너머 현재 위치를 몰래 알지 않기 위한 기억값.
    private float lastSeenX = Float.NaN, lastSeenY = Float.NaN;
    private float lostSightTime;      // 시야를 잃은 뒤 경과 시간
    private boolean seesPlayer;       // 최근 감지 검사에서 플레이어가 보였는지
    private float senseTime;          // 다음 감지 검사까지 남은 시간(개체별 분산)
    private float planTime;           // 다음 경로 재평가까지 남은 시간(개체별 분산)
    private float plannedDirX, plannedDirY; // 캐시된 이동 방향(재평가 주기 사이 유지)
    private float restTime;           // 배회 휴식 남은 시간
    private float searchTime;         // SEARCH 남은 시간
    private float[] patrolPoints;     // 경비원 순찰 지점(x,y 쌍, 최대 3개) — 처음 필요할 때 생성
    private int patrolIndex;

    public Monster(long id, MonsterType type, float feetX, float feetY, float width, float height, int maxHp) {
        if (id < 0 || type == null || width <= 0f || height <= 0f) throw new IllegalArgumentException();
        this.id = id;
        this.type = type;
        bounds = new Rectangle(feetX - width / 2f, feetY, width, height);
        health = new HealthComponent(maxHp);
        homeX = feetX;
        homeY = feetY;
    }

    public long id() { return id; }
    public MonsterType type() { return type; }
    public Rectangle bounds() { return bounds; }
    public float feetX() { return bounds.x + bounds.width / 2f; }
    public float feetY() { return bounds.y; }
    public HealthComponent health() { return health; }
    public MonsterState state() { return state; }
    public void setState(MonsterState state) { this.state = state; }
    public Direction facing() { return facing; }
    public void setFacing(Direction facing) { if (facing != null) this.facing = facing; }
    public float stateTime() { return stateTime; }
    public void setStateTime(float time) { stateTime = time; }
    public void advanceStateTime(float delta) { stateTime += delta; }
    public float wanderTime() { return wanderTime; }
    public void setWanderTime(float time) { wanderTime = time; }
    public void advanceWanderTime(float delta) { wanderTime += delta; }
    public float attackCooldown() { return attackCooldown; }
    public void setAttackCooldown(float value) { attackCooldown = Math.max(0f, value); }
    public void reduceAttackCooldown(float delta) { attackCooldown = Math.max(0f, attackCooldown - delta); }
    public boolean attackHit() { return attackHit; }
    public void setAttackHit(boolean hit) { attackHit = hit; }
    public float hitTime() { return hitTime; }
    public void setHitTime(float value) { hitTime = Math.max(0f, value); }
    public void reduceHitTime(float delta) { hitTime = Math.max(0f, hitTime - delta); }
    public boolean isAlive() { return health.isAlive(); }
    public boolean enraged() { return enraged; }
    public void enrage() { enraged = true; }
    public boolean aggro() { return aggro; }
    public void setAggro(boolean value) { aggro = value; }
    public int nearbyRunStarts() { return nearbyRunStarts; }
    public int countRunStart() { return ++nearbyRunStarts; }
    public boolean hasRoamTarget() { return !Float.isNaN(roamTargetX); }
    public float roamTargetX() { return roamTargetX; }
    public float roamTargetY() { return roamTargetY; }
    public void setRoamTarget(float x, float y) { roamTargetX = x; roamTargetY = y; }
    public void clearRoamTarget() { roamTargetX = Float.NaN; roamTargetY = Float.NaN; }

    public float homeX() { return homeX; }
    public float homeY() { return homeY; }
    public boolean hasLastSeen() { return !Float.isNaN(lastSeenX); }
    public float lastSeenX() { return lastSeenX; }
    public float lastSeenY() { return lastSeenY; }
    public void rememberSeen(float x, float y) { lastSeenX = x; lastSeenY = y; }
    public void forgetSeen() { lastSeenX = Float.NaN; lastSeenY = Float.NaN; }
    public boolean seesPlayer() { return seesPlayer; }
    public void setSeesPlayer(boolean sees) { seesPlayer = sees; }
    public float lostSightTime() { return lostSightTime; }
    public void setLostSightTime(float value) { lostSightTime = Math.max(0f, value); }
    public void advanceLostSightTime(float delta) { lostSightTime += delta; }
    public float senseTime() { return senseTime; }
    public void setSenseTime(float value) { senseTime = value; }
    public void reduceSenseTime(float delta) { senseTime -= delta; }
    public float planTime() { return planTime; }
    public void setPlanTime(float value) { planTime = value; }
    public void reducePlanTime(float delta) { planTime -= delta; }
    public float plannedDirX() { return plannedDirX; }
    public float plannedDirY() { return plannedDirY; }
    public void setPlannedDirection(float x, float y) { plannedDirX = x; plannedDirY = y; }
    public float restTime() { return restTime; }
    public void setRestTime(float value) { restTime = Math.max(0f, value); }
    public void reduceRestTime(float delta) { restTime = Math.max(0f, restTime - delta); }
    public float searchTime() { return searchTime; }
    public void setSearchTime(float value) { searchTime = Math.max(0f, value); }
    public void reduceSearchTime(float delta) { searchTime = Math.max(0f, searchTime - delta); }
    public float[] patrolPoints() { return patrolPoints; }
    public void setPatrolPoints(float[] points) { patrolPoints = points; }
    public int patrolIndex() { return patrolIndex; }
    public void setPatrolIndex(int index) { patrolIndex = index; }

    /** 팀장 복귀 완료: 분노·소음 누적·추적 기억을 모두 초기화한다(HP는 바꾸지 않는다). */
    public void calmDown() {
        enraged = false;
        aggro = false;
        nearbyRunStarts = 0;
        seesPlayer = false;
        lostSightTime = 0f;
        forgetSeen();
    }
    public float knockbackTime() { return knockbackTime; }
    public float knockbackX() { return knockbackX; }
    public float knockbackY() { return knockbackY; }
    public void setKnockback(float x, float y, float seconds) {
        knockbackX = x;
        knockbackY = y;
        knockbackTime = Math.max(0f, seconds);
    }
    public void reduceKnockbackTime(float delta) { knockbackTime = Math.max(0f, knockbackTime - delta); }
}

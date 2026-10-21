package io.github.some_example_name.combat;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import io.github.some_example_name.collision.CollisionResult;
import io.github.some_example_name.collision.CollisionSystem;
import io.github.some_example_name.config.BalanceConfig;
import io.github.some_example_name.entity.Direction;
import io.github.some_example_name.entity.monster.Monster;
import io.github.some_example_name.entity.player.Player;
import io.github.some_example_name.render.MonsterAnimationSet;
import io.github.some_example_name.world.Hazard;
import io.github.some_example_name.world.LaboratoryRoom;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 몬스터 행동 상태 기계(2026-10-16 개편):
 * IDLE(휴식) ↔ WANDER(배회/순찰) → CHASE(추적) → SEARCH(마지막 목격 지점 탐색) → RETURN(홈 복귀).
 *
 * 탐지: 종류별 발견 거리 안 + "시야가 닿을 때"만 발견한다(벽·닫힌 문이 시야 차단 —
 * 벽 뒤 플레이어를 거리만으로 알 수 없다). 발견 거리 < 해제 거리(히스테리시스)라
 * 경계에서 상태가 떨리지 않는다.
 *
 * 추적 해제: 해제 거리 초과 / 시야 상실 유지 시간 초과 / 활동 구역(홈 12타일) 이탈.
 * 해제 후에는 마지막 목격 지점만 기억해 탐색하며, 벽 너머의 현재 위치를 계속 알지 않는다.
 *
 * 성능: 감지·경로 재평가는 개체 id로 분산된 주기(기본 0.15s)에만 수행하고, 그 사이에는
 * 캐시된 이동 방향을 쓴다. 이동 중 주변 몬스터와 약한 간격 유지(분리)를 섞되 충돌 해석이
 * 항상 우선이라 좁은 문에서 영구 정지하지 않는다.
 */
public final class MonsterAiSystem {
    private final BalanceConfig config;
    private final CollisionSystem collision = new CollisionSystem();
    private final CombatCollisionGrid dynamicGrid;
    private final Rectangle attackBox = new Rectangle();
    private final Random random;

    public MonsterAiSystem(BalanceConfig config, LaboratoryRoom room, long seed) {
        this.config = config;
        dynamicGrid = new CombatCollisionGrid(room);
        random = new Random(seed);
    }

    public void update(List<Monster> monsters, Player player, PlayerCombatState playerCombat,
                       LaboratoryRoom room, Map<MonsterType, MonsterAnimationSet> animations,
                       boolean runStarted, float delta) {
        if (player.isDead()) return;
        if (runStarted) registerNoise(monsters, player.feetX(), player.feetY());
        Iterator<Monster> iterator = monsters.iterator();
        while (iterator.hasNext()) {
            Monster monster = iterator.next();
            if (!monster.isAlive()) {
                monster.setState(MonsterState.DEAD);
                continue;
            }
            if (player.isDead()) break;
            monster.reduceAttackCooldown(delta);
            if (monster.knockbackTime() > 0f) {
                moveBy(monster, player, monsters, room,
                    monster.knockbackX() * delta / config.knockbackSeconds,
                    monster.knockbackY() * delta / config.knockbackSeconds);
                monster.reduceKnockbackTime(delta);
            }
            if (monster.hitTime() > 0f) {
                monster.reduceHitTime(delta);
                monster.advanceStateTime(delta);
                if (monster.hitTime() > 0f) { monster.setState(MonsterState.HIT); continue; }
                monster.setStateTime(0f);
                if (monster.type() == MonsterType.TEAM_LEADER && monster.enraged()) {
                    monster.setState(MonsterState.ENRAGED);
                    continue;
                }
            }
            // 감전(전기 투척): 남은 시간 동안 자율 이동·새 공격 시작을 막는다. 아직 타격이
            // 확정되지 않은 공격은 취소하되(상태만 되돌림), 이미 들어간 피해는 되돌리지
            // 않는다. 넉백 물리(위)는 그대로 적용되고, AI를 영구 정지시키지 않는다 —
            // 시간이 다하면 자연 복귀한다.
            if (monster.shocked()) {
                monster.reduceShockTime(delta);
                if (monster.state() == MonsterState.ATTACK && !monster.attackHit()) {
                    monster.setState(MonsterState.CHASE);
                    monster.setStateTime(0f);
                }
                if (monster.shocked()) continue;
            }
            // 비분노 팀장: 구석에 머무른다. 근접·시야 발견으로 추적하지 않는다(소음 규칙 전용).
            if (monster.type() == MonsterType.TEAM_LEADER && !monster.enraged()
                && monster.state() != MonsterState.RETURN) {
                monster.setState(MonsterState.IDLE);
                monster.setStateTime(0f);
                continue;
            }
            MonsterAnimationSet set = animations.get(monster.type());
            if (monster.state() == MonsterState.ENRAGED) {
                monster.advanceStateTime(delta);
                float enrageDuration = duration(set, "enrage");
                if (monster.stateTime() >= enrageDuration) {
                    monster.setState(MonsterState.CHASE);
                    monster.setStateTime(0f);
                }
                continue;
            }
            if (monster.state() == MonsterState.ATTACK) {
                updateAttack(monster, player, playerCombat, set, room, delta);
                if (player.isDead()) break;
                continue;
            }

            sense(monster, player, room, delta);

            if (monster.aggro()) {
                updateChase(monster, player, playerCombat, monsters, room, delta);
            } else switch (monster.state()) {
                case SEARCH: updateSearch(monster, player, monsters, room, delta); break;
                case RETURN: updateReturn(monster, player, monsters, room, delta); break;
                default: updateWander(monster, player, monsters, room, delta); break;
            }
        }
        // Iteration is complete before dead entities leave the world collection.
        monsters.removeIf(monster -> !monster.isAlive());
    }

    // ----- 탐지 ---------------------------------------------------------------------

    /**
     * 주기적 감지: 발견 거리 안 + 시야(벽·닫힌 문 차단 없음)일 때만 발견. 검사 주기는
     * 개체 id로 분산해 매 프레임 전체 몬스터가 시야 광선을 쏘지 않는다. 팀장은 소음
     * 분노로만 추적을 시작하지만, 분노 중의 시야 갱신은 동일 규칙을 쓴다.
     */
    private void sense(Monster monster, Player player, LaboratoryRoom room, float delta) {
        monster.reduceSenseTime(delta);
        if (monster.senseTime() > 0f) {
            if (monster.aggro() && !monster.seesPlayer()) monster.advanceLostSightTime(delta);
            return;
        }
        monster.setSenseTime(config.monsterSenseIntervalSeconds + monster.id() % 7 * 0.02f);
        float dx = player.feetX() - monster.feetX();
        float dy = player.feetY() - monster.feetY();
        float distance2 = dx * dx + dy * dy;
        boolean inDetectRange = monster.type() != MonsterType.TEAM_LEADER
            && distance2 <= square(detectionRadius(monster.type()));
        boolean inReleaseRange = distance2 <= square(config.monsterChaseReleaseRadius);
        boolean sight = (inDetectRange || (monster.aggro() && inReleaseRange))
            && !room.sightBlocked(monster.feetX(), eye(monster), player.feetX(), eyePlayer(player));
        if (sight && (inDetectRange || monster.aggro())) {
            monster.setSeesPlayer(true);
            monster.rememberSeen(player.feetX(), player.feetY());
            monster.setLostSightTime(0f);
            if (inDetectRange) monster.setAggro(true);
        } else {
            if (monster.seesPlayer()) monster.setLostSightTime(0f);
            monster.setSeesPlayer(false);
            if (monster.aggro()) monster.advanceLostSightTime(delta);
        }
    }

    // ----- 추적 ---------------------------------------------------------------------

    private void updateChase(Monster monster, Player player, PlayerCombatState playerCombat,
                             List<Monster> all, LaboratoryRoom room, float delta) {
        float dx = player.feetX() - monster.feetX();
        float dy = player.feetY() - monster.feetY();
        float distance2 = dx * dx + dy * dy;
        float homeDx = monster.feetX() - monster.homeX(), homeDy = monster.feetY() - monster.homeY();
        boolean tooFar = distance2 > square(config.monsterChaseReleaseRadius);
        boolean lostTooLong = !monster.seesPlayer()
            && monster.lostSightTime() > config.monsterLostSightGraceSeconds;
        boolean beyondHome = homeDx * homeDx + homeDy * homeDy
            > square(config.monsterHomeLeashRadius);
        if (tooFar || lostTooLong || beyondHome) {
            releaseChase(monster, tooFar || beyondHome);
            return;
        }
        // 공격: 보이는 상태 + 사거리 + 쿨다운 + 통로 확보일 때만. 사거리 안에서는 플레이어
        // 중심까지 파고들지 않고 멈춰서 공격 기회를 기다린다.
        float range = attackRange(monster);
        if (monster.seesPlayer() && distance2 <= range * range) {
            Direction face = Direction.fromInput(dx, dy);
            monster.setFacing(face);
            if (monster.attackCooldown() <= 0f && clearPath(monster.feetX(), eye(monster),
                    player.feetX(), eyePlayer(player), room)) {
                monster.setState(MonsterState.ATTACK);
                monster.setStateTime(0f);
                monster.setAttackHit(false);
                return;
            }
            monster.setState(MonsterState.CHASE);
            monster.advanceStateTime(delta);
            return; // 사거리 안 — 정지 대기
        }
        monster.setState(MonsterState.CHASE);
        monster.advanceStateTime(delta);
        if (monster.seesPlayer()) {
            // 보이는 동안에는 현재 위치로 직진(시야가 곧 통로 증명이라 광선 재검사 불필요).
            steerDirect(monster, player, all, room, dx, dy, delta);
        } else {
            // 시야 상실: 마지막 목격 지점으로만 이동한다(벽 너머 현재 위치 추적 금지).
            if (!monster.hasLastSeen()) { releaseChase(monster, false); return; }
            float sx = monster.lastSeenX() - monster.feetX();
            float sy = monster.lastSeenY() - monster.feetY();
            if (sx * sx + sy * sy < 0.8f * 0.8f) { releaseChase(monster, false); return; }
            steerPlanned(monster, player, all, room, monster.lastSeenX(), monster.lastSeenY(), delta);
        }
    }

    /** 추적 해제 → 탐색 또는 복귀. HP 회복·재생성 같은 부수 효과는 없다. */
    private void releaseChase(Monster monster, boolean straightHome) {
        monster.setAggro(false);
        monster.setSeesPlayer(false);
        monster.setLostSightTime(0f);
        monster.clearRoamTarget();
        monster.setPlannedDirection(0f, 0f);
        monster.setStateTime(0f);
        if (!straightHome && monster.hasLastSeen()
            && monster.type() != MonsterType.TEAM_LEADER) {
            monster.setState(MonsterState.SEARCH);
            monster.setSearchTime(config.monsterSearchMinSeconds + random.nextFloat()
                * Math.max(0f, config.monsterSearchMaxSeconds - config.monsterSearchMinSeconds));
        } else {
            monster.setState(MonsterState.RETURN);
        }
    }

    // ----- 탐색/복귀/배회 ------------------------------------------------------------

    /** 마지막 목격 지점 주변을 잠깐 탐색한 뒤 복귀/배회로 돌아간다. */
    private void updateSearch(Monster monster, Player player, List<Monster> all,
                              LaboratoryRoom room, float delta) {
        monster.setState(MonsterState.SEARCH);
        monster.advanceStateTime(delta);
        monster.reduceSearchTime(delta);
        if (monster.searchTime() <= 0f || !monster.hasLastSeen()) {
            monster.forgetSeen();
            monster.clearRoamTarget();
            float dx = monster.feetX() - monster.homeX(), dy = monster.feetY() - monster.homeY();
            monster.setState(dx * dx + dy * dy > square(config.monsterHomeLeashRadius * 0.6f)
                ? MonsterState.RETURN : MonsterState.IDLE);
            return;
        }
        if (!monster.hasRoamTarget()
            && !pickTargetNear(monster, room, monster.lastSeenX(), monster.lastSeenY(), 2.2f)) {
            monster.setSearchTime(0f);
            return;
        }
        if (walkToRoamTarget(monster, player, all, room, delta, 0.7f)) monster.clearRoamTarget();
    }

    /** 활동 구역(홈)으로 걸어서 복귀. 도중에 탐지 조건을 만족하면 sense()가 다시 추적시킨다. */
    private void updateReturn(Monster monster, Player player, List<Monster> all,
                              LaboratoryRoom room, float delta) {
        monster.setState(MonsterState.RETURN);
        monster.advanceStateTime(delta);
        float dx = monster.feetX() - monster.homeX(), dy = monster.feetY() - monster.homeY();
        if (dx * dx + dy * dy < 1.2f * 1.2f) {
            monster.clearRoamTarget();
            monster.setPlannedDirection(0f, 0f);
            if (monster.type() == MonsterType.TEAM_LEADER) {
                monster.calmDown();      // 복귀 완료: 분노·소음 누적 초기화
                monster.setState(MonsterState.IDLE);
            } else {
                monster.setState(MonsterState.IDLE);
                monster.setRestTime(restSeconds(monster.type()));
            }
            monster.setStateTime(0f);
            return;
        }
        if (!monster.hasRoamTarget()
            && !pickTargetNear(monster, room, monster.homeX(), monster.homeY(), 1.5f)) {
            // 홈 주변이 일시적으로 막히면 홈 자체를 목표로 걷는다(순간이동 금지).
            monster.setRoamTarget(monster.homeX(), monster.homeY());
        }
        if (walkToRoamTarget(monster, player, all, room, delta, 0.9f)) monster.clearRoamTarget();
    }

    /**
     * 자연스러운 배회: 목표로 1~3타일 걷고 0.5~2초쯤 쉰다(종류별 리듬). 매 프레임 방향을
     * 바꾸지 않고, 도착·막힘·시간 경과에만 다음 목표를 고른다. 목표는 시야가 닿는(같은 방/
     * 열린 문 너머) 통행 가능 칸이라 벽·가구 안에 생기지 않는다.
     */
    private void updateWander(Monster monster, Player player, List<Monster> all,
                              LaboratoryRoom room, float delta) {
        if (monster.restTime() > 0f) {
            monster.reduceRestTime(delta);
            monster.setState(MonsterState.IDLE);
            monster.advanceStateTime(delta);
            return;
        }
        if (!monster.hasRoamTarget() && !pickWanderTarget(monster, room)) {
            monster.setState(MonsterState.IDLE);
            monster.advanceStateTime(delta);
            monster.setRestTime(0.6f + random.nextFloat() * 0.8f);
            return;
        }
        monster.setState(MonsterState.WANDER);
        monster.advanceStateTime(delta);
        if (walkToRoamTarget(monster, player, all, room, delta, 0.5f)) {
            monster.clearRoamTarget();
            monster.setRestTime(restSeconds(monster.type()));
            if (monster.type() == MonsterType.GUARD && monster.patrolPoints() != null)
                monster.setPatrolIndex((monster.patrolIndex() + 1)
                    % (monster.patrolPoints().length / 2));
        }
    }

    /** 종류별 배회 목표: 슬라임=현재 위치 주변 짧게, 경비원=순찰 지점 순환, 연구원=장비(홈) 주변. */
    private boolean pickWanderTarget(Monster monster, LaboratoryRoom room) {
        switch (monster.type()) {
            case GUARD: {
                float[] patrol = monster.patrolPoints();
                if (patrol == null) {
                    patrol = buildPatrol(monster, room);
                    monster.setPatrolPoints(patrol);
                }
                if (patrol.length >= 2) {
                    int index = monster.patrolIndex() % (patrol.length / 2);
                    monster.setRoamTarget(patrol[index * 2], patrol[index * 2 + 1]);
                    return true;
                }
                return pickTargetNear(monster, room, monster.homeX(), monster.homeY(),
                    config.guardPatrolRadius);
            }
            case RESEARCHER:
                return pickTargetNear(monster, room, monster.homeX(), monster.homeY(),
                    config.researcherWanderRadius);
            default: {
                float step = config.slimeWanderStepMin + random.nextFloat()
                    * (config.slimeWanderStepMax - config.slimeWanderStepMin);
                return pickTargetNear(monster, room, monster.feetX(), monster.feetY(), step + 0.5f);
            }
        }
    }

    /** 경비원 순찰 지점 2~3개를 홈 주변에서 고른다(시야가 닿는 통행 가능 칸만). */
    private float[] buildPatrol(Monster monster, LaboratoryRoom room) {
        float[] points = new float[6];
        int found = 0;
        for (int attempt = 0; attempt < 40 && found < 3; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            float r = 1.5f + random.nextFloat() * (config.guardPatrolRadius - 1.5f);
            float x = monster.homeX() + (float) Math.cos(angle) * r;
            float y = monster.homeY() + (float) Math.sin(angle) * r;
            if (validTarget(room, monster, x, y)) {
                points[found * 2] = x;
                points[found * 2 + 1] = y;
                found++;
            }
        }
        if (found == 0) return new float[0];
        float[] out = new float[found * 2];
        System.arraycopy(points, 0, out, 0, out.length);
        return out;
    }

    /** center 주변 radius 안에서 유효한 배회 목표를 고른다(최대 12회 시도). */
    private boolean pickTargetNear(Monster monster, LaboratoryRoom room,
                                   float centerX, float centerY, float radius) {
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            float r = 0.8f + random.nextFloat() * Math.max(0.2f, radius - 0.8f);
            float x = centerX + (float) Math.cos(angle) * r;
            float y = centerY + (float) Math.sin(angle) * r;
            if (validTarget(room, monster, x, y)) {
                monster.setRoamTarget(x, y);
                monster.setPlanTime(0f);
                return true;
            }
        }
        return false;
    }

    /** 목표 유효성: 통행 가능 + 위험 없음 + 활동 구역 안 + 현재 위치에서 시야가 닿음. */
    private boolean validTarget(LaboratoryRoom room, Monster monster, float x, float y) {
        int tx = (int) Math.floor(x), ty = (int) Math.floor(y);
        if (room.blocksActorAtCell(tx, ty, config.monsterBoundsWidth, config.monsterBoundsHeight)
            || room.hazardAt(tx, ty) != Hazard.NONE) return false;
        float hx = x - monster.homeX(), hy = y - monster.homeY();
        if (hx * hx + hy * hy > square(config.monsterHomeLeashRadius)) return false;
        return !room.sightBlocked(monster.feetX(), eye(monster), x, y);
    }

    /**
     * 목표 지점으로 걷는다. @return 도착했으면 true. 경로가 양 축 모두 막히면 목표를 버리고
     * true를 돌려 다음 목표를 고르게 한다(벽에 계속 밀착해 걷는 것 방지).
     */
    private boolean walkToRoamTarget(Monster monster, Player player, List<Monster> all,
                                     LaboratoryRoom room, float delta, float arriveDistance) {
        if (!monster.hasRoamTarget()) return true;
        float dx = monster.roamTargetX() - monster.feetX();
        float dy = monster.roamTargetY() - monster.feetY();
        if (dx * dx + dy * dy <= arriveDistance * arriveDistance) return true;
        return !steerPlanned(monster, player, all, room,
            monster.roamTargetX(), monster.roamTargetY(), delta);
    }

    // ----- 이동(계획 캐시 + 약한 분리) -------------------------------------------------

    /** 보이는 추적 전용: 광선 재검사 없이 플레이어 방향으로 직진(분리 포함). */
    private void steerDirect(Monster monster, Player player, List<Monster> all,
                             LaboratoryRoom room, float dx, float dy, float delta) {
        Vector2 direction = new Vector2(dx, dy).nor();
        monster.setPlannedDirection(direction.x, direction.y);
        applyMove(monster, player, all, room, direction.x, direction.y, delta);
    }

    /**
     * 목표 지점으로의 이동 계획을 주기적으로만 재평가하고(개체별 분산), 사이에는 캐시된
     * 방향을 쓴다. @return 이동을 계속할 수 있으면 true, 양 축이 모두 막혔으면 false.
     */
    private boolean steerPlanned(Monster monster, Player player, List<Monster> all,
                                 LaboratoryRoom room, float tx, float ty, float delta) {
        monster.reducePlanTime(delta);
        if (monster.planTime() <= 0f) {
            monster.setPlanTime(config.monsterSenseIntervalSeconds + monster.id() % 5 * 0.03f);
            if (!planDirection(monster, room, tx, ty)) {
                monster.setPlannedDirection(0f, 0f);
                return false;
            }
        }
        float dx = monster.plannedDirX(), dy = monster.plannedDirY();
        if (dx == 0f && dy == 0f) return false;
        applyMove(monster, player, all, room, dx, dy, delta);
        return true;
    }

    /** 직선 경로가 열려 있으면 직진, 아니면 축 우선 우회. 둘 다 막히면 false. */
    private boolean planDirection(Monster monster, LaboratoryRoom room, float tx, float ty) {
        float dx = tx - monster.feetX(), dy = ty - monster.feetY();
        float cy = eye(monster);
        if (clearPath(monster.feetX(), cy, tx, ty, room)) {
            Vector2 direction = new Vector2(dx, dy).nor();
            monster.setPlannedDirection(direction.x, direction.y);
            return true;
        }
        boolean xClear = Math.abs(dx) > 0.05f && clearPath(monster.feetX(), cy, tx, cy, room);
        boolean yClear = Math.abs(dy) > 0.05f && clearPath(monster.feetX(), cy, monster.feetX(), ty, room);
        if (xClear && (Math.abs(dx) >= Math.abs(dy) || !yClear)) {
            monster.setPlannedDirection(Math.signum(dx), 0f);
            return true;
        }
        if (yClear) {
            monster.setPlannedDirection(0f, Math.signum(dy));
            return true;
        }
        return false;
    }

    /**
     * 실제 이동 적용: 주변 몬스터와의 약한 분리 벡터를 섞어 한 점에 겹치거나 긴 줄로
     * 따라오는 것을 완화한다. 분리는 방향만 틀 뿐 충돌 해석(벽·문·몸체)이 항상 우선한다.
     */
    private void applyMove(Monster monster, Player player, List<Monster> all,
                           LaboratoryRoom room, float dirX, float dirY, float delta) {
        float sepX = 0f, sepY = 0f;
        for (Monster other : all) {
            if (other == monster || !other.isAlive()) continue;
            float ox = monster.feetX() - other.feetX();
            float oy = monster.feetY() - other.feetY();
            float d2 = ox * ox + oy * oy;
            if (d2 > 0.0001f && d2 < 0.9f * 0.9f) {
                float d = (float) Math.sqrt(d2);
                sepX += ox / d * (0.9f - d);
                sepY += oy / d * (0.9f - d);
            }
        }
        Vector2 direction = new Vector2(dirX + sepX * 0.5f, dirY + sepY * 0.5f);
        if (direction.len2() < 1e-6f) direction.set(dirX, dirY);
        direction.nor();
        Direction facing = Direction.fromInput(direction.x, direction.y);
        if (facing != null) monster.setFacing(facing);
        float speed = speed(monster.type());
        moveBy(monster, player, all, room, direction.x * speed * delta, direction.y * speed * delta);
    }

    // ----- 기존 공유 로직 --------------------------------------------------------------

    public void registerNoise(List<Monster> monsters, float x, float y) {
        for (Monster monster : monsters) {
            if (monster.type() != MonsterType.TEAM_LEADER || monster.enraged()) continue;
            float dx = monster.feetX() - x, dy = monster.feetY() - y;
            if (dx * dx + dy * dy <= config.teamLeaderHearingRadius * config.teamLeaderHearingRadius
                && monster.countRunStart() >= config.teamLeaderRunTriggerCount) {
                monster.enrage();
                monster.setAggro(true);
                monster.setState(MonsterState.ENRAGED);
                monster.setStateTime(0f);
            }
        }
    }

    private void updateAttack(Monster monster, Player player, PlayerCombatState playerCombat,
                              MonsterAnimationSet set, LaboratoryRoom room, float delta) {
        String clip = monster.type() == MonsterType.TEAM_LEADER
            ? "radial_slam" : "attack_" + directionKey(monster.facing());
        monster.advanceStateTime(delta);
        int frame = (int) (monster.stateTime() / set.frameDuration(clip));
        if (!monster.attackHit() && CombatSystem.hasHitFrame(set.hitFrames(clip), frame)) {
            boolean inRange;
            if (monster.type() == MonsterType.TEAM_LEADER) {
                float dx = player.feetX() - monster.feetX(), dy = player.feetY() - monster.feetY();
                inRange = dx * dx + dy * dy <= config.teamLeaderAttackRange * config.teamLeaderAttackRange;
            } else {
                AttackHitbox.set(monster.bounds(), monster.facing(), config.monsterAttackRange,
                    config.playerAttackWidth, attackBox);
                inRange = attackBox.overlaps(player.bounds());
            }
            monster.setAttackHit(true);
            if (inRange && clearPath(monster.feetX(), monster.feetY() + monster.bounds().height / 2f,
                    player.feetX(), player.feetY() + player.bounds().height / 2f, room)) {
                int damage = damage(monster.type());
                float awayX = player.feetX() - monster.feetX();
                float awayY = player.feetY() - monster.feetY();
                float length = (float) Math.sqrt(awayX * awayX + awayY * awayY);
                if (length > 0f) { awayX /= length; awayY /= length; }
                if (playerCombat.takeDamage(player, damage,
                        awayX * config.playerKnockbackDistance, awayY * config.playerKnockbackDistance,
                        config.playerInvulnerabilitySeconds)) {
                    if (!player.isDead()) {
                        CollisionResult result = collision.resolve(player.bounds(),
                            playerCombat.knockbackX(), playerCombat.knockbackY(), room);
                        player.bounds().setPosition(result.x, result.y);
                    }
                }
            }
        }
        if (monster.stateTime() >= duration(set, clip)) {
            monster.setAttackCooldown(config.monsterAttackCooldownSeconds);
            monster.setState(MonsterState.CHASE);
            monster.setStateTime(0f);
            monster.setAttackHit(false);
        }
    }

    private void moveBy(Monster m, Player player, List<Monster> all, LaboratoryRoom room,
                        float dx, float dy) {
        dynamicGrid.forMonster(player, all, m);
        CollisionResult result = collision.resolve(m.bounds(), dx, dy, dynamicGrid);
        m.bounds().setPosition(result.x, result.y);
    }

    private boolean clearPath(float x1, float y1, float x2, float y2, LaboratoryRoom room) {
        return !room.blocksActorAlongPath(x1,y1,x2,y2,
            config.monsterBoundsWidth,config.monsterBoundsHeight);
    }

    private float eye(Monster m) { return m.feetY() + m.bounds().height / 2f; }
    private static float eyePlayer(Player p) { return p.feetY() + p.bounds().height / 2f; }
    private static float square(float v) { return v * v; }

    private float restSeconds(MonsterType type) {
        switch (type) {
            case GUARD: return config.guardRestMin
                + random.nextFloat() * (config.guardRestMax - config.guardRestMin);
            case RESEARCHER: return config.researcherRestMin
                + random.nextFloat() * (config.researcherRestMax - config.researcherRestMin);
            default: return config.slimeRestMin
                + random.nextFloat() * (config.slimeRestMax - config.slimeRestMin);
        }
    }

    private float detectionRadius(MonsterType type) {
        switch (type) {
            case SLIME: return config.slimeDetectionRadius;
            case RESEARCHER: return config.researcherDetectionRadius;
            case GUARD: return config.guardDetectionRadius;
            default: return config.teamLeaderHearingRadius;
        }
    }
    private float attackRange(Monster m) {
        return m.type() == MonsterType.TEAM_LEADER ? config.teamLeaderAttackRange : config.monsterAttackRange;
    }
    private float speed(MonsterType type) {
        switch (type) {
            case SLIME: return config.slimeMoveSpeed;
            case RESEARCHER: return config.researcherMoveSpeed;
            case GUARD: return config.guardMoveSpeed;
            default: return config.teamLeaderMoveSpeed;
        }
    }
    private int damage(MonsterType type) {
        switch (type) {
            case SLIME: return config.slimeAttackDamage;
            case RESEARCHER: return config.researcherAttackDamage;
            case GUARD: return config.guardAttackDamage;
            default: return config.teamLeaderAttackDamage;
        }
    }
    private static float duration(MonsterAnimationSet set, String clip) {
        return set.frameDuration(clip) * set.frameCount(clip);
    }
    private static String directionKey(Direction d) { return d.name().toLowerCase(); }
}

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

/** Simple deterministic-capable sensing, wandering, chase and telegraphed attacks. */
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
            if (monster.type() == MonsterType.TEAM_LEADER && !monster.enraged()) {
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
            float dx = player.feetX() - monster.feetX();
            float dy = player.feetY() - monster.feetY();
            float distance2 = dx * dx + dy * dy;
            float detect = detectionRadius(monster.type());
            if (distance2 <= detect * detect) monster.setAggro(true);
            // 추격 리시: 플레이어와의 거리가 감지 반경×배수를 넘어가면 무조건 따라다니지 않고
            // 추격을 포기한 뒤, 즉시 다른 공간의 목적지를 골라 자유 이동으로 돌아간다.
            // (분노한 팀장은 기존 규칙대로 그 조우 동안 포기하지 않는다.)
            else if (monster.aggro() && monster.type() != MonsterType.TEAM_LEADER) {
                float giveUp = detect * config.monsterChaseGiveUpFactor;
                if (distance2 > giveUp * giveUp) {
                    monster.setAggro(false);
                    monster.clearRoamTarget();
                    monster.setWanderTime(config.monsterRoamRestSeconds); // 바로 새 목적지 선정
                    monster.setState(MonsterState.IDLE);
                    monster.setStateTime(0f);
                }
            }
            if (monster.aggro()) {
                monster.setState(MonsterState.ALERT);
                if (distance2 <= attackRange(monster) * attackRange(monster)
                    && monster.attackCooldown() <= 0f && clearPath(monster.feetX(),
                        monster.feetY() + monster.bounds().height / 2f, player.feetX(),
                        player.feetY() + player.bounds().height / 2f, room)) {
                    Direction face = Direction.fromInput(dx, dy);
                    monster.setFacing(face);
                    monster.setState(MonsterState.ATTACK);
                    monster.setStateTime(0f);
                    monster.setAttackHit(false);
                    continue;
                }
                monster.setState(MonsterState.CHASE);
                monster.advanceStateTime(delta);
                chase(monster, player, monsters, room, dx, dy, delta);
            } else if (monster.type() != MonsterType.TEAM_LEADER) {
                roam(monster, player, monsters, room, delta);
            }
        }
        // Iteration is complete before dead entities leave the world collection.
        monsters.removeIf(monster -> !monster.isAlive());
    }

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

    private void chase(Monster m, Player player, List<Monster> all, LaboratoryRoom room,
                       float dx, float dy, float delta) {
        Vector2 direction = new Vector2(dx, dy);
        if (clearPath(m.feetX(), m.feetY() + m.bounds().height / 2f,
                player.feetX(), player.feetY() + player.bounds().height / 2f, room)) {
            direction.nor();
            move(m, player, all, room, direction.x, direction.y, delta);
            return;
        }
        boolean xClear = clearPath(m.feetX(), m.feetY() + m.bounds().height / 2f,
            player.feetX(), m.feetY() + m.bounds().height / 2f, room);
        boolean yClear = clearPath(m.feetX(), m.feetY() + m.bounds().height / 2f,
            m.feetX(), player.feetY() + player.bounds().height / 2f, room);
        if (Math.abs(dx) >= Math.abs(dy)) {
            if (xClear) move(m, player, all, room, Math.signum(dx), 0f, delta);
            else if (yClear) move(m, player, all, room, 0f, Math.signum(dy), delta);
        } else {
            if (yClear) move(m, player, all, room, 0f, Math.signum(dy), delta);
            else if (xClear) move(m, player, all, room, Math.signum(dx), 0f, delta);
        }
    }

    /**
     * 자유 이동(로밍): 한가한 몬스터는 제자리 배회가 아니라 맵의 다른 공간에 있는 목적지를
     * 골라 그쪽으로 걸어간다. 도착하면 잠깐 쉬었다가 새 목적지를 고르고, 오래 막혀 있으면
     * 재선정한다. 직선이 막히면 축 우선으로 돌아가고, 양 축 모두 막히면 목적지를 버린다.
     */
    private void roam(Monster m, Player player, List<Monster> all, LaboratoryRoom room, float delta) {
        m.advanceStateTime(delta);
        m.advanceWanderTime(delta);
        m.setState(MonsterState.IDLE);
        if (!m.hasRoamTarget()) {
            if (m.wanderTime() < config.monsterRoamRestSeconds) return; // 도착 후 잠깐 휴식
            pickRoamTarget(m, room);
            if (!m.hasRoamTarget()) return;
        } else if (m.wanderTime() >= config.monsterRoamRetargetSeconds) {
            pickRoamTarget(m, room); // 너무 오래 못 가면 다른 목적지로
            if (!m.hasRoamTarget()) return;
        }
        float dx = m.roamTargetX() - m.feetX(), dy = m.roamTargetY() - m.feetY();
        if (dx * dx + dy * dy < 1f) {       // 도착
            m.clearRoamTarget();
            m.setWanderTime(0f);
            return;
        }
        float cy = m.feetY() + m.bounds().height / 2f;
        if (clearPath(m.feetX(), cy, m.roamTargetX(), m.roamTargetY(), room)) {
            Vector2 direction = new Vector2(dx, dy).nor();
            move(m, player, all, room, direction.x, direction.y, delta);
            return;
        }
        boolean xClear = clearPath(m.feetX(), cy, m.roamTargetX(), cy, room);
        boolean yClear = clearPath(m.feetX(), cy, m.feetX(), m.roamTargetY(), room);
        if (xClear && (Math.abs(dx) >= Math.abs(dy) || !yClear))
            move(m, player, all, room, Math.signum(dx), 0f, delta);
        else if (yClear)
            move(m, player, all, room, 0f, Math.signum(dy), delta);
        else {
            m.clearRoamTarget();            // 양 축 모두 막힘 → 다음 프레임에 새 공간 선택
            m.setWanderTime(config.monsterRoamRestSeconds);
        }
    }

    /** 현재 위치에서 최소 거리 이상 떨어진, 설 수 있고 위험 없는 셀을 무작위로 고른다. */
    private void pickRoamTarget(Monster m, LaboratoryRoom room) {
        m.setWanderTime(0f);
        float best = -1f, bestX = Float.NaN, bestY = Float.NaN;
        for (int attempt = 0; attempt < 30; attempt++) {
            int x = 1 + random.nextInt(Math.max(1, room.widthInTiles() - 2));
            int y = 1 + random.nextInt(Math.max(1, room.heightInTiles() - 2));
            if (room.blocksActorAtCell(x, y, config.monsterBoundsWidth, config.monsterBoundsHeight)
                || room.hazardAt(x, y) != Hazard.NONE) continue;
            float cx = x + .5f, cy = y + .5f;
            float dx = cx - m.feetX(), dy = cy - m.feetY();
            float d2 = dx * dx + dy * dy;
            if (d2 >= config.monsterRoamMinDistance * config.monsterRoamMinDistance) {
                m.setRoamTarget(cx, cy);
                return;
            }
            if (d2 > best) { best = d2; bestX = cx; bestY = cy; } // 좁은 맵 대비 가장 먼 후보
        }
        if (!Float.isNaN(bestX)) m.setRoamTarget(bestX, bestY);
        else m.clearRoamTarget();
    }

    private void move(Monster m, Player player, List<Monster> all, LaboratoryRoom room,
                      float vx, float vy, float delta) {
        if (vx == 0f && vy == 0f) return;
        Direction facing = Direction.fromInput(vx, vy);
        m.setFacing(facing);
        Vector2 direction = new Vector2(vx, vy).nor();
        float speed = speed(m.type());
        moveBy(m, player, all, room, direction.x * speed * delta, direction.y * speed * delta);
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

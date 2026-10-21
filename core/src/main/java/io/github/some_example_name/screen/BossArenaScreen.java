package io.github.some_example_name.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.github.some_example_name.LaboratoryGame;
import io.github.some_example_name.asset.BossAssets;
import io.github.some_example_name.asset.GameAssets;
import io.github.some_example_name.boss.BossDefinition;
import io.github.some_example_name.boss.BossFight;
import io.github.some_example_name.combat.CombatCollisionGrid;
import io.github.some_example_name.combat.CombatSystem;
import io.github.some_example_name.combat.ElectricProjectile;
import io.github.some_example_name.combat.MonsterType;
import io.github.some_example_name.combat.PlayerAttackState;
import io.github.some_example_name.combat.PlayerCombatState;
import io.github.some_example_name.combat.PlayerDashState;
import io.github.some_example_name.combat.PlayerShieldState;
import io.github.some_example_name.combat.ShockwaveState;
import io.github.some_example_name.combat.SkillCast;
import io.github.some_example_name.config.BalanceConfig;
import io.github.some_example_name.entity.Direction;
import io.github.some_example_name.entity.monster.Monster;
import io.github.some_example_name.entity.player.Player;
import io.github.some_example_name.input.GameAction;
import io.github.some_example_name.input.GdxPlayerInput;
import io.github.some_example_name.input.InputState;
import io.github.some_example_name.input.KeyBindings;
import io.github.some_example_name.input.PlayerController;
import io.github.some_example_name.input.PlayerIntent;
import io.github.some_example_name.inventory.LootItem;
import io.github.some_example_name.movement.MovementSystem;
import io.github.some_example_name.movement.RunStartDetector;
import io.github.some_example_name.profile.SkillCatalog;
import io.github.some_example_name.profile.SkillLoadout;
import io.github.some_example_name.profile.SkillService;
import io.github.some_example_name.world.LaboratoryRoom;
import io.github.some_example_name.world.PlayerSessionState;
import io.github.some_example_name.world.TileType;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

/**
 * 보스 1-1 광장 화면. 32×24 광장은 반복 바닥 타일 + 기존 V4 벽으로 조립되고, 기둥/방벽은
 * footprint 장애물(그림 사각형 전체가 아니라 바닥 접촉부만 solid)로 들어간다. 보스는 기존
 * 몬스터를 쓰지 않는 별도 전투체이며, 플레이어 공격/스킬은 보스 hurtbox 프록시(Monster 1개)를
 * 통해 기존 공통 전투 경로(공격 히트 프레임·돌진·충격파·전기)를 그대로 쓴다 — 넉백/경직은
 * AI를 돌리지 않으므로 자연히 면역이고(JSON 선언과 일치), 감전은 표시만 되고 행동은 멈추지
 * 않는다. 전투 전이는 전부 BossFight 상태 기계 한 곳에서 관리한다.
 */
public final class BossArenaScreen extends BaseScreen {

    private static final float VIEW_WIDTH = 20f, VIEW_HEIGHT = 15f;
    private static final int ARENA_W = 32, ARENA_H = 24;    // 레거시 1-1 광장
    private static final float BOSS_DRAW = 4.0f;             // 레거시 표현 크기
    private static final float BOSS_FOOT = 32f / 192f;       // 레거시 앵커(96,160) → 발 비율
    private static final float BOSS9_FOOT = 32f / 256f;      // 9단계 앵커(128,224) → 발 비율
    private static final float HURTBOX_W = 2.2f, HURTBOX_H = 1.5f; // 레거시 1-1
    private static final float FRAME = 1f / 15f;
    private static final float SHEET_FRAME = 0.12f;          // 9단계 시트 frameSeconds

    private final OrthographicCamera camera = new OrthographicCamera();
    private final FitViewport viewport;
    private final SpriteBatch batch = new SpriteBatch();
    private final BalanceConfig config;
    private final LaboratoryRoom room;
    private final Player player;
    private final PlayerSessionState session;
    private final InputState inputState = new InputState();
    private final KeyBindings bindings = KeyBindings.defaults();
    private final GdxPlayerInput input = new GdxPlayerInput(bindings);
    private final PlayerController controller = new PlayerController();
    private final MovementSystem movementSystem;
    private final CombatCollisionGrid playerGrid;
    private final PlayerCombatState playerCombat;
    private final CombatSystem combatSystem;
    private final io.github.some_example_name.render.EntityRenderer entityRenderer;
    private final io.github.some_example_name.render.PlayerHudRenderer hud;
    private final io.github.some_example_name.render.BossHudRenderer bossHud;
    private final io.github.some_example_name.render.BossResultRenderer resultPanel;
    private final io.github.some_example_name.render.SkillBarRenderer skillBar;
    private final io.github.some_example_name.render.HealCastRenderer healCast;
    private final io.github.some_example_name.render.SkillVfxRenderer skillVfx;
    private final io.github.some_example_name.render.LaboratoryStructureRendererV4 wallRenderer;

    // 보스 전투: hurtbox 프록시(공통 전투 경로용) + 상태 기계.
    // 1-1은 레거시 BossFight(회귀 기준 고정), 1-2 이후는 범용 BossBattle 패턴 엔진.
    private BossFight fight;
    private io.github.some_example_name.boss.BossBattle battle;
    private Monster bossProxy;
    private final List<Monster> bossList = new ArrayList<>();
    private final BossDefinition def;
    private final io.github.some_example_name.boss.BossStageCatalog.Stage stage;
    private final boolean legacy;
    private final io.github.some_example_name.boss.BossArenaCatalog.Arena arenaDef; // 비레거시
    private final int arenaW, arenaH;
    // 상태 이상(보스 패턴): 둔화(이동 속도 배율, 갱신 방식)·뿌리 구속(이동만 차단).
    private float slowTimer, slowFactor = 1f, rootTimer;
    // 플레이어 스킬(연구소와 동일 공통 파이프라인).
    private final PlayerDashState dash;
    private final io.github.some_example_name.collision.CollisionSystem dashCollision =
        new io.github.some_example_name.collision.CollisionSystem();
    private final Rectangle dashHitArea = new Rectangle();
    private final List<ShockwaveState> shockwaves = new ArrayList<>();
    private final List<ElectricProjectile> projectiles = new ArrayList<>();
    private SkillCast electricCast;
    private float electricDirX, electricDirY;
    private final PlayerShieldState shieldState;
    private final ShockwaveState.Occlusion shockOcclusion;
    private final ElectricProjectile.Solid projectileSolid;
    // 보스 착탄 1회 연출.
    private static final class Impact { float x, y, age; String art; } // art null=레거시
    private final List<Impact> impacts = new ArrayList<>();
    private boolean paused;
    private boolean rewardResolved;   // REWARD_PENDING 진입 1회 처리
    private boolean returnRequested;  // 복귀 1회
    private LootItem rewardItem;
    private float introBegun = -1f;
    // 개발 전용(LAB_BOSS_AUTO=win|fail): 패턴 캡처·자동 토벌/사망 검증.
    private final String autoTest = System.getenv("LAB_BOSS_AUTO");
    private int autoFrame;

    public BossArenaScreen(LaboratoryGame game, GameAssets assets, PlayerSessionState session,
                           BalanceConfig config) {
        super(game, assets);
        this.config = config;
        this.session = session;
        this.player = session.player();
        this.def = game.bossDefinition();
        this.stage = game.bossStage();
        this.legacy = stage == null || stage.legacyEngine;
        this.arenaDef = legacy ? null : game.bossArenaCatalog().arena(stage.arenaId);
        this.arenaW = legacy ? ARENA_W : arenaDef.width;
        this.arenaH = legacy ? ARENA_H : arenaDef.height;
        viewport = new FitViewport(VIEW_WIDTH, VIEW_HEIGHT, camera);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        room = buildArena();
        Vector2 spawn = room.spawnPoint();
        player.bounds().setPosition(spawn.x - player.bounds().width / 2f, spawn.y);
        movementSystem = new MovementSystem(new io.github.some_example_name.collision.CollisionSystem(),
            new RunStartDetector(), config.playerWalkSpeed, config.playerRunSpeed);
        playerGrid = new CombatCollisionGrid(room);
        PlayerAttackState attack = new PlayerAttackState(config.playerAttackCooldownSeconds,
            assets.playerAnimations().attackFrameCount(Direction.DOWN),
            assets.playerAnimations().hitFrames(Direction.DOWN));
        playerCombat = new PlayerCombatState(attack);
        combatSystem = new CombatSystem(config);
        combatSystem.setStrikeListener(new CombatSystem.StrikeListener() {
            @Override public void onMonsterStruck(Monster monster, boolean killed) {
                assets.combatSounds().playMonsterHit();
            }
        });
        entityRenderer = new io.github.some_example_name.render.EntityRenderer(
            new io.github.some_example_name.render.AnimationController(assets.playerAnimations()));
        hud = new io.github.some_example_name.render.PlayerHudRenderer(assets.uiFont());
        bossHud = new io.github.some_example_name.render.BossHudRenderer(assets.uiFont());
        resultPanel = new io.github.some_example_name.render.BossResultRenderer(
            assets.boss(), assets.uiFont());
        skillBar = new io.github.some_example_name.render.SkillBarRenderer(assets.uiFont());
        healCast = new io.github.some_example_name.render.HealCastRenderer(assets.skills());
        skillVfx = new io.github.some_example_name.render.SkillVfxRenderer(
            assets.threeSkills(), game.skillCatalog().shockwaveRadius);
        wallRenderer = new io.github.some_example_name.render.LaboratoryStructureRendererV4(
            assets.structureV4());
        dash = new PlayerDashState(config.dashDistanceTiles, config.dashDurationSeconds);
        shieldState = new PlayerShieldState(game.skillCatalog().shieldActivateDelay);
        playerCombat.setShield(shieldState);
        shockOcclusion = new ShockwaveState.Occlusion() {
            @Override public boolean blocked(float fx, float fy, float tx, float ty) {
                return room.blocksActorAlongPath(fx, fy, tx, ty, .05f, .05f);
            }
        };
        projectileSolid = new ElectricProjectile.Solid() {
            @Override public boolean blockedAt(float cx, float cy, float radius) {
                return room.blocksActorAt(cx, cy - radius, radius * 2f, radius * 2f);
            }
        };
        startRun();
        updateCamera();
    }

    /** 새 시도(runId는 게임 루트가 발급): 보스/전투 상태·플레이어 위치·임시 효과 초기화. */
    private void startRun() {
        if (legacy) {
            bossProxy = new Monster(9100L, MonsterType.SLIME, 16.5f, 17f,
                HURTBOX_W, HURTBOX_H, def.maxHp);
            fight = new BossFight(def, bossProxy.health(), game.bossAttemptSeed(), 16.5f, 17f);
        } else {
            // 피격 박스(hurtbox)는 그리기 크기와 분리된 카탈로그 값 — 중심은 발+offset.
            bossProxy = new Monster(9100L, MonsterType.SLIME,
                arenaDef.bossSpawnX, arenaDef.bossSpawnY, stage.hurtboxW, stage.hurtboxH,
                stage.draftMaxHp);
            battle = new io.github.some_example_name.boss.BossBattle(stage,
                game.bossPatternCatalog(), bossProxy.health(), game.bossAttemptSeed(),
                arenaDef.bossSpawnX, arenaDef.bossSpawnY);
            positionProxy();
        }
        bossList.clear();
        bossList.add(bossProxy);
        slowTimer = 0f;
        slowFactor = 1f;
        rootTimer = 0f;
        Vector2 spawn = room.spawnPoint();
        player.bounds().setPosition(spawn.x - player.bounds().width / 2f, spawn.y);
        if (player.isDead()) player.revive();           // 실패 복귀/재도전: 현재 최대 HP로 회복
        else player.healToFullHealth();
        player.setFacing(Direction.UP);
        shockwaves.clear();
        projectiles.clear();
        electricCast = null;
        shieldState.clear();
        skillVfx.clear();
        impacts.clear();
        rewardResolved = false;
        returnRequested = false;
        rewardItem = null;
        room.setGateClosed(true);                        // 전투 중 잠금
        resultPanel.hide();
        resultPanel.unlockButtons();
        introBegun = -1f;
        Gdx.app.log("Boss", "run start stage=" + game.bossStageId() + " runId=" + game.bossRunId()
            + " seed=" + game.bossAttemptSeed() + " bossHp=" + bossMaxHp()
            + " engine=" + (legacy ? "legacy" : "nine"));
    }

    /** 9단계 공용: 단계·투기장 데이터에 맞는 통일 접근자(레거시/신규 엔진 분기 1곳). */
    private BossFight.Phase phase() { return legacy ? fight.phase() : battle.phase(); }
    private float bossX() { return legacy ? fight.bossX() : battle.bossX(); }
    private float bossY() { return legacy ? fight.bossY() : battle.bossY(); }
    private float bossPhaseTime() { return legacy ? fight.phaseTime() : battle.phaseTime(); }
    private BossFight.PatternStage bossPatternStage() {
        return legacy ? fight.patternStage() : battle.patternStage();
    }
    private void markBossCleared() { if (legacy) fight.markCleared(); else battle.markCleared(); }
    private boolean beginBossReturn() {
        return legacy ? fight.beginReturn() : battle.beginReturn();
    }
    private String bossDisplayName() { return stage != null ? stage.displayName : def.displayName; }
    private int bossMaxHp() { return legacy ? def.maxHp : stage.draftMaxHp; }

    /** 프록시 피격 박스 동기화: 중심 = 발 + hurtboxCenterOffsetY(그리기·이동 충돌과 분리). */
    private void positionProxy() {
        bossProxy.bounds().setPosition(battle.bossX() - stage.hurtboxW / 2f,
            battle.bossY() + stage.hurtboxCenterOffsetY - stage.hurtboxH / 2f);
    }

    private LaboratoryRoom buildArena() {
        return legacy ? buildLegacyArena() : buildCatalogArena();
    }

    /** 비레거시: arena_layouts의 논리 배치(기존 바닥/벽/문 조립 재사용, 기둥 footprint만 solid). */
    private LaboratoryRoom buildCatalogArena() {
        TileType[][] logical = new TileType[arenaH][arenaW];
        for (int y = 0; y < arenaH; y++)
            for (int x = 0; x < arenaW; x++) logical[y][x] = TileType.WALL;
        for (int y = 2; y < arenaH - 2; y++)
            for (int x = 2; x < arenaW - 2; x++) logical[y][x] = TileType.FLOOR;
        for (int y = 0; y <= 1; y++)
            for (int x = arenaDef.gateX; x < arenaDef.gateX + arenaDef.gateW; x++)
                logical[y][x] = TileType.DOORWAY_OPEN; // 남쪽 입장/귀환 겸용 문
        LaboratoryRoom arena = new LaboratoryRoom(logical,
            new Vector2(arenaDef.playerSpawnX, arenaDef.playerSpawnY - 1.8f));
        arena.setSouthExitGate(arenaDef.gateX, arenaDef.gateX + arenaDef.gateW, 1f);
        arena.setGatePassage(new Rectangle(arenaDef.gateX, 0f, arenaDef.gateW, 2f));
        for (float[] pillar : arenaDef.pillars)
            arena.addWorldObstacle(new Rectangle(pillar[0], pillar[1], pillar[2], pillar[3]));
        boolean[][] forced = new boolean[arenaH][arenaW];
        for (int y = 0; y < arenaH; y++)
            for (int x = 0; x < arenaW; x++)
                if (x <= 1 || y <= 1 || x >= arenaW - 2 || y >= arenaH - 2) forced[y][x] = true;
        arena.setForcedWallMask(forced);
        return arena;
    }

    /** 32×24 광장: 외곽 벽 2타일 + 남쪽 입장/귀환 겸용 문 + 기둥4·방벽2 footprint 장애물. */
    private LaboratoryRoom buildLegacyArena() {
        TileType[][] logical = new TileType[ARENA_H][ARENA_W];
        for (int y = 0; y < ARENA_H; y++)
            for (int x = 0; x < ARENA_W; x++) logical[y][x] = TileType.WALL;
        for (int y = 2; y < 22; y++)
            for (int x = 2; x < 30; x++) logical[y][x] = TileType.FLOOR;
        for (int y = 0; y <= 1; y++)
            for (int x = 15; x <= 17; x++) logical[y][x] = TileType.DOORWAY_OPEN; // 남쪽 겸용 문
        LaboratoryRoom arena = new LaboratoryRoom(logical, new Vector2(16.5f, 3.2f));
        arena.setSouthExitGate(15f, 18f, 1f);
        arena.setGatePassage(new Rectangle(15f, 0f, 3f, 2f));
        // 기둥/방벽: 그림 전체가 아니라 발 footprint만 solid(arena_1_1.json proxyFootprint).
        arena.addWorldObstacle(new Rectangle(8f, 8f, 2f, 2f));
        arena.addWorldObstacle(new Rectangle(22f, 8f, 2f, 2f));
        arena.addWorldObstacle(new Rectangle(8f, 15f, 2f, 2f));
        arena.addWorldObstacle(new Rectangle(22f, 15f, 2f, 2f));
        arena.addWorldObstacle(new Rectangle(12f, 11f, 3f, 1f));
        arena.addWorldObstacle(new Rectangle(18f, 11f, 3f, 1f));
        boolean[][] forced = new boolean[ARENA_H][ARENA_W];
        for (int y = 0; y < ARENA_H; y++)
            for (int x = 0; x < ARENA_W; x++)
                if (x <= 1 || y <= 1 || x >= ARENA_W - 2 || y >= ARENA_H - 2) forced[y][x] = true;
        arena.setForcedWallMask(forced);
        return arena;
    }

    private final BossFight.Context fightCtx = new BossFight.Context() {
        @Override public float playerX() { return player.feetX(); }
        @Override public float playerY() { return player.feetY(); }
        @Override public Rectangle playerBounds() { return player.bounds(); }
        @Override public boolean playerAlive() { return !player.isDead(); }
        @Override public boolean damagePlayer(int damage) {
            // 기존 보호막→HP 공통 파이프라인 1회(무적 규칙 포함, 넉백 없음).
            boolean accepted = playerCombat.takeDamage(player, damage, 0f, 0f,
                config.playerInvulnerabilitySeconds);
            if (accepted) assets.combatSounds().playPlayerHurt();
            if (accepted && player.isDead()) assets.combatSounds().playPlayerDeath();
            return accepted;
        }
        @Override public boolean blockedAt(float cx, float cy, float radius) {
            return room.blocksActorAt(cx, cy - radius, radius * 2f, radius * 2f);
        }
        @Override public boolean validFloor(float x, float y) {
            return !room.blocksActorAt(x, y - 0.15f, 0.3f, 0.3f);
        }
    };

    /** 범용 엔진 접점: 1-1 Context + 기둥 시야 차폐·둔화·구속(이동만 제한). */
    private final io.github.some_example_name.boss.BossBattle.Context battleCtx =
        new io.github.some_example_name.boss.BossBattle.Context() {
            @Override public float playerX() { return player.feetX(); }
            @Override public float playerY() { return player.feetY(); }
            @Override public Rectangle playerBounds() { return player.bounds(); }
            @Override public boolean playerAlive() { return !player.isDead(); }
            @Override public boolean damagePlayer(int damage) {
                boolean accepted = playerCombat.takeDamage(player, damage, 0f, 0f,
                    config.playerInvulnerabilitySeconds);
                if (accepted) assets.combatSounds().playPlayerHurt();
                if (accepted && player.isDead()) assets.combatSounds().playPlayerDeath();
                return accepted;
            }
            @Override public boolean blockedAt(float cx, float cy, float radius) {
                return room.blocksActorAt(cx, cy - radius, radius * 2f, radius * 2f);
            }
            @Override public boolean sightBlocked(float fx, float fy, float tx, float ty) {
                return room.blocksActorAlongPath(fx, fy, tx, ty, .05f, .05f);
            }
            @Override public boolean validFloor(float x, float y) {
                return !room.blocksActorAt(x, y - 0.15f, 0.3f, 0.3f);
            }
            @Override public void applySlow(float factor, float seconds) {
                slowFactor = factor;   // 갱신 방식(중첩 금지)
                slowTimer = seconds;
            }
            @Override public void applyRoot(float seconds) {
                rootTimer = Math.max(rootTimer, seconds); // 이동만 차단 — 공격/스킬 허용
            }
        };

    @Override public void render(float delta) {
        float dt = Math.min(delta, config.maxFrameDeltaSeconds);
        boolean transitionLocked = game.transition().inputLocked();
        if (transitionLocked) inputState.update(EnumSet.noneOf(GameAction.class));
        else input.poll(inputState);
        boolean active = !transitionLocked;
        if (active && inputState.isPressed(GameAction.PAUSE) && !resultPanel.visible())
            paused = !paused;
        if (active && phase() == BossFight.Phase.ENTERING && introBegun < 0f) {
            introBegun = 0f;
            if (legacy) fight.beginIntro(); // 입장 완료 → 1초 소개(공격·피해 없음, 이동 허용)
            else battle.beginIntro();
        }
        resultPanel.handleInput(transitionLocked);

        boolean worldActive = !paused && !transitionLocked && !resultPanel.visible();
        float animDelta = worldActive ? dt : 0f;
        if (worldActive && !player.isDead()) {
            PlayerIntent intent = controller.intentFrom(player, inputState);
            // 스킬 슬롯(등록 키 폴링 — 연구소와 동일 규칙).
            for (int slot = 0; slot < SkillLoadout.SLOT_COUNT; slot++) {
                int key = game.skillLoadout().keyAt(slot);
                if (key != SkillLoadout.KEY_NONE && Gdx.input.isKeyJustPressed(key))
                    trySkillSlot(slot, intent.moveX, intent.moveY);
            }
            game.tickSkillCooldowns(dt);
            updateSkillEffects(dt);
            if (intent.attack && playerCombat.attack().start(player.facing()))
                assets.combatSounds().playAttackSwing();
            playerGrid.forPlayer(bossList);
            float previousFeetY = player.feetY();
            // 상태 이상: 둔화는 이동 속도 배율(갱신 방식), 구속은 이동만 차단(공격/스킬 허용).
            slowTimer = Math.max(0f, slowTimer - dt);
            rootTimer = Math.max(0f, rootTimer - dt);
            float dashStep = dash.advance(dt);
            if (dashStep > 0f) {
                performDashMovement(dashStep);
                player.setMoving(true);
            } else if (rootTimer > 0f) {
                player.setMoving(false);
            } else {
                movementSystem.update(player, intent, playerGrid,
                    slowTimer > 0f ? dt * slowFactor : dt);
            }
            playerCombat.update(dt);
            combatSystem.updatePlayerAttack(player, playerCombat, assets.playerAnimations(),
                bossList, room, dt);
            // 승리 후 열린 귀환문으로 남쪽 통과 → 본부 복귀(1회).
            float exitMoveY = dashStep > 0f ? dash.dirY() : intent.moveY;
            if (phase() == BossFight.Phase.CLEARED
                && room.crossedSouthExit(previousFeetY, player.feetY(), player.feetX(), exitMoveY))
                requestReturn();
        } else if (!worldActive) {
            player.setMoving(false);
        }
        if (worldActive || (resultPanel.visible() && phase() == BossFight.Phase.DYING)) {
            if (legacy) {
                fight.update(dt, fightCtx);
                bossProxy.bounds().setPosition(fight.bossX() - HURTBOX_W / 2f, fight.bossY());
            } else {
                battle.update(dt, battleCtx);
                positionProxy();
            }
        }
        handlePhaseTransitions();
        if (legacy) {
            for (float[] impact : fight.consumeImpactEvents()) {
                Impact effect = new Impact();
                effect.x = impact[0];
                effect.y = impact[1];
                impacts.add(effect);
            }
        } else {
            for (io.github.some_example_name.boss.BossBattle.ImpactEvent event
                : battle.consumeImpactEvents()) {
                Impact effect = new Impact();
                effect.x = event.x;
                effect.y = event.y;
                effect.art = event.art;
                impacts.add(effect);
            }
        }
        Iterator<Impact> impactIt = impacts.iterator();
        while (impactIt.hasNext()) {
            Impact impact = impactIt.next();
            impact.age += animDelta;
            if (impact.age >= FRAME * 6f) impactIt.remove();
        }
        if (autoTest != null) runAutoTest();

        updateCamera();
        drawWorld(animDelta);
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        hud.render(assets.hud(), assets.loot(), assets.fadePixel(), player,
            session.backpack(), false, dt);
        bossHud.render(assets.boss(), bossDisplayName(),
            Math.max(0, bossProxy.health().hp()), bossMaxHp());
        skillBar.render(assets.skills(), assets.threeSkills(), assets.combatUi().dashIcon(),
            assets.fadePixel(), game.skillLoadout(), game.skillRuntime(),
            game.activeSkillSlots(), new io.github.some_example_name.render.SkillBarRenderer.CooldownTotals() {
                @Override public float total(String skillId) { return game.skillCooldownSeconds(skillId); }
            });
        resultPanel.render();
        renderFade(batch, viewport);
        Gdx.gl.glColorMask(false, false, false, true);
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glColorMask(true, true, true, true);
    }

    /** 상태 전이의 화면 측 후처리(보상 1회·결과창·귀환문 개방·실패창). */
    private void handlePhaseTransitions() {
        if (phase() == BossFight.Phase.REWARD_PENDING && !rewardResolved) {
            rewardResolved = true;
            rewardItem = game.rewardItemForRun();         // runId 파생 시드 — 재추첨 없음
            attemptRewardCommit();
        }
        if (phase() == BossFight.Phase.FAILED && !resultPanel.visible()) {
            resultPanel.showFailed(resultListener);
        }
    }

    private void attemptRewardCommit() {
        io.github.some_example_name.profile.HeadquartersEconomy.Result result =
            game.commitBossReward(rewardItem.id);
        if (result.status == io.github.some_example_name.profile.HeadquartersEconomy.Status.SAVE_FAILED) {
            resultPanel.showReward(rewardItem, assets.loot().itemFramed(rewardItem), true,
                resultListener);
            Gdx.app.error("Boss", "reward save failed - kept REWARD_PENDING for retry");
            return;
        }
        // OK 또는 ALREADY_PROCESSED(재실행에서 이미 저장된 영수증) → CLEARED + 귀환문 개방.
        resultPanel.showReward(rewardItem, assets.loot().itemFramed(rewardItem), false,
            resultListener);
        markBossCleared();
        resultPanel.markCleared();
        room.setGateClosed(false);
        Gdx.app.log("Boss", "clear committed runId=" + game.bossRunId()
            + " reward=" + rewardItem.id + " status=" + result.status);
    }

    private final io.github.some_example_name.render.BossResultRenderer.Listener resultListener =
        new io.github.some_example_name.render.BossResultRenderer.Listener() {
            @Override public void onRetrySave() { attemptRewardCommit(); }
            @Override public void onRetryRun() {
                game.newBossRun();   // 새 runId/attemptSeed — 새 유효 토벌은 새 보상 1개
                startRun();
            }
            @Override public void onReturnHome() { requestReturn(); }
        };

    private void requestReturn() {
        if (returnRequested || !beginBossReturn()) return;
        returnRequested = true;
        game.transition().request(io.github.some_example_name.world.MapId.HEADQUARTERS);
    }

    // ----- 플레이어 스킬(연구소와 동일 공통 파이프라인, 대상은 보스 프록시) -----------------

    private SkillService.Result trySkillSlot(final int slot, final float moveX, final float moveY) {
        SkillService.Context ctx = new SkillService.Context() {
            @Override public boolean worldSkillInputAllowed() {
                return !paused && !resultPanel.visible() && !game.transition().inputLocked();
            }
            @Override public boolean alive() { return !player.isDead(); }
            @Override public int currentHp() { return player.hp(); }
            @Override public int currentMaxHp() { return player.maxHp(); }
            @Override public boolean unlocked(String skillId) {
                return game.unlockedSkills().contains(skillId);
            }
            @Override public float cooldownSeconds(String skillId) {
                return game.skillCooldownSeconds(skillId);
            }
            @Override public int healAmount() { return game.healSkillAmount(); }
            @Override public boolean applyEffect(String skillId, int effectValue) {
                if (SkillLoadout.DASH.equals(skillId)) {
                    if (!dash.start(moveX, moveY, player.facing())) return false;
                    Direction facing = Direction.fromInput(dash.dirX(), dash.dirY());
                    if (facing != null) player.setFacing(facing);
                    assets.combatSounds().playDash();
                    return true;
                }
                if (SkillLoadout.SHOCKWAVE.equals(skillId)) {
                    shockwaves.add(new ShockwaveState(player.feetX(), player.feetY(),
                        game.skillCatalog().shockwaveDamageStart,
                        game.skillCatalog().shockwaveDamageEnd,
                        game.skillCatalog().shockwaveDuration,
                        new ShockwaveState.RadiusAt() {
                            @Override public float radius(float t) {
                                return game.skillCatalog().shockwaveFrontRadius(t);
                            }
                        }, 0f, 1f));
                    return true;
                }
                if (SkillLoadout.ELECTRIC_THROW.equals(skillId)) {
                    if (electricCast != null && electricCast.pending()) return false;
                    float dx = moveX, dy = moveY;
                    if (dx == 0f && dy == 0f) {
                        dx = player.facing() == Direction.LEFT ? -1f
                            : player.facing() == Direction.RIGHT ? 1f : 0f;
                        dy = player.facing() == Direction.UP ? 1f
                            : player.facing() == Direction.DOWN ? -1f : 0f;
                    }
                    float length = (float) Math.sqrt(dx * dx + dy * dy);
                    electricDirX = dx / length;
                    electricDirY = dy / length;
                    electricCast = new SkillCast(game.skillCatalog().electricSpawnDelay);
                    return true;
                }
                if (SkillLoadout.SHIELD.equals(skillId)) {
                    return shieldState.tryCast(game.shieldAbsorbAmount(),
                        game.skillCatalog().shieldDurationSeconds);
                }
                player.heal(effectValue);
                healCast.play();
                return true;
            }
        };
        return SkillService.tryExecute(slot, game.skillLoadout(), game.activeSkillSlots(),
            game.skillRuntime(), ctx);
    }

    private void updateSkillEffects(float dt) {
        SkillCatalog cat = game.skillCatalog();
        for (Iterator<ShockwaveState> it = shockwaves.iterator(); it.hasNext();) {
            ShockwaveState wave = it.next();
            for (ShockwaveState.Hit hit : wave.update(dt, bossList, shockOcclusion)) {
                combatSystem.applyPlayerSkillStrike(player, hit.monster,
                    game.skillDamage(SkillLoadout.SHOCKWAVE), hit.dirX, hit.dirY,
                    0f); // 보스 넉백 면역(JSON) — 거리 0
                skillVfx.spawnShockHit(bossX(), bossY() + 0.8f);
            }
            if (wave.finished()) it.remove();
        }
        if (electricCast != null && electricCast.update(dt)) {
            projectiles.add(new ElectricProjectile(player.feetX(),
                player.feetY() + player.bounds().height * 0.5f, electricDirX, electricDirY,
                cat.electricSpeed, cat.electricMaxDistance, cat.electricCollisionRadius));
            electricCast = null;
        }
        for (Iterator<ElectricProjectile> it = projectiles.iterator(); it.hasNext();) {
            ElectricProjectile projectile = it.next();
            ElectricProjectile.Outcome outcome = projectile.update(dt, projectileSolid, bossList);
            if (outcome == ElectricProjectile.Outcome.HIT_MONSTER) {
                // 전기 직접 피해는 받고(면역 아님), 감전은 표시만(행동 중단 면역 — AI 없음).
                combatSystem.applyPlayerSkillStrike(player, projectile.hitMonster(),
                    game.skillDamage(SkillLoadout.ELECTRIC_THROW),
                    projectile.dirX(), projectile.dirY(), 0f);
                projectile.hitMonster().applyShock(cat.electricStunSeconds);
                skillVfx.spawnElectricImpact(projectile.x(), projectile.y());
                it.remove();
            } else if (outcome == ElectricProjectile.Outcome.HIT_WALL) {
                skillVfx.spawnElectricImpact(projectile.x(), projectile.y());
                it.remove();
            } else if (outcome == ElectricProjectile.Outcome.EXPIRED) {
                it.remove();
            }
        }
        bossProxy.reduceShockTime(dt);
        shieldState.update(dt);
        shieldState.consumeActivated();
        if (shieldState.consumeBroke()) skillVfx.spawnShieldBreak(player.feetX(), player.feetY());
        if (shieldState.consumeExpired()) skillVfx.spawnShieldFade(player.feetX(), player.feetY());
        skillVfx.update(dt);
        healCast.update(dt);
    }

    private void performDashMovement(float distance) {
        int substeps = Math.max(1, (int) Math.ceil(distance / config.dashSubstepLength));
        float sub = distance / substeps;
        for (int i = 0; i < substeps; i++) {
            io.github.some_example_name.collision.CollisionResult result =
                dashCollision.resolve(player.bounds(), dash.dirX() * sub, dash.dirY() * sub, room);
            player.bounds().setPosition(result.x, result.y);
            float pad = config.dashHitPadding;
            Rectangle b = player.bounds();
            dashHitArea.set(b.x - pad, b.y - pad, b.width + pad * 2f, b.height + pad * 2f);
            if (bossProxy.isAlive() && dashHitArea.overlaps(bossProxy.bounds())
                && dash.markHit(bossProxy.id()))
                combatSystem.applyPlayerSkillStrike(player, bossProxy,
                    config.playerAttackDamage, dash.dirX(), dash.dirY(), 0f);
            boolean blockedX = result.hitX && dash.dirX() != 0f;
            boolean blockedY = result.hitY && dash.dirY() != 0f;
            if ((dash.dirY() == 0f && blockedX) || (dash.dirX() == 0f && blockedY)
                || (blockedX && blockedY)) {
                dash.interrupt();
                return;
            }
        }
    }

    // ----- 월드 렌더 -------------------------------------------------------------------

    private void drawWorld(float animDelta) {
        Gdx.gl.glColorMask(true, true, true, true);
        ScreenUtils.clear(0.04f, 0.04f, 0.05f, 1f);
        Gdx.gl.glColorMask(true, true, true, false);
        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        BossAssets boss = assets.boss();
        io.github.some_example_name.asset.BossNineAssets nine = assets.bossNine();
        batch.begin();
        // 반복 바닥 타일(통짜 그림 금지).
        for (int y = 2; y < arenaH - 2; y++)
            for (int x = 2; x < arenaW - 2; x++) batch.draw(boss.floor(), x, y, 1f, 1f);
        wallRenderer.drawWalls(batch, room, camera);
        // 대문(남쪽 겸용): 전투 중 잠금 / 토벌 저장 후 개방. 발 앵커 중앙, 통로 폭에 맞춰 늘림.
        boolean gateOpen = phase() == BossFight.Phase.CLEARED
            || phase() == BossFight.Phase.RETURNING;
        TextureRegion gate = boss.gate(phase() == BossFight.Phase.COMBAT
            || phase() == BossFight.Phase.INTRO, gateOpen);
        if (legacy) batch.draw(gate, 15f, 0f, 3f, 2f);
        else batch.draw(gate, arenaDef.gateX, 0f, arenaDef.gateW, 2f);
        if (legacy) {
            drawTelegraphs(boss);
            drawPools(boss);
        } else {
            drawTelegraphsNine(boss, nine);
            drawZonesNine(boss, nine);
            drawRing(nine);
        }
        // 깊이 정렬(발 y 내림차순): 기둥/방벽/보스/플레이어.
        drawDepthSorted(boss, animDelta);
        if (legacy) drawProjectiles(boss);
        else {
            drawProjectilesNine(boss, nine);
            drawBeam(nine);
        }
        for (Impact impact : impacts) {
            int frame = (int) (impact.age / FRAME);
            if (impact.art != null && nine.hasVfx(impact.art))
                batch.draw(nine.vfxFrame(impact.art, (int) (impact.age / 0.1f)),
                    impact.x - 0.9f, impact.y - 0.9f, 1.8f, 1.8f);
            else
                batch.draw(boss.impactFrame(frame), impact.x - 0.7f, impact.y - 0.7f, 1.4f, 1.4f);
        }
        skillVfx.renderGround(batch, shockwaves);
        skillVfx.renderOverlay(batch, projectiles, bossList, shieldState,
            player.feetX(), player.feetY());
        healCast.render(batch, player.feetX(), player.feetY());
        batch.end();
    }

    private void drawTelegraphs(BossAssets boss) {
        BossFight.PatternStage stage = fight.patternStage();
        if (stage != BossFight.PatternStage.TELEGRAPH && !fight.charging()) return;
        String id = fight.patternId();
        float pulse = 0.35f + 0.2f * (float) Math.abs(Math.sin(fight.patternElapsed() * 8f));
        if ("reagent_throw".equals(id) && stage == BossFight.PatternStage.TELEGRAPH) {
            drawLane(boss, fight.bossX(), fight.bossY() + 1.2f, fight.aimDirX(), fight.aimDirY(),
                7f, 0.6f, pulse);
        } else if ("ram".equals(id)) {
            // 예고 폭 = 돌진 collider(1.8) + 실제 타격 여유 — 이미지 꼬리는 범위가 아니다.
            drawLane(boss, fight.bossX(), fight.bossY() + 0.6f, fight.aimDirX(), fight.aimDirY(),
                def.ramMaxDistance, def.ramColliderWidth + 0.2f,
                stage == BossFight.PatternStage.TELEGRAPH ? pulse : 0.25f);
        }
    }

    private void drawLane(BossAssets boss, float x, float y, float dirX, float dirY,
                          float length, float width, float alpha) {
        batch.setColor(1f, 1f, 1f, alpha);
        float angle = MathUtils.atan2(dirY, dirX) * MathUtils.radiansToDegrees;
        batch.draw(boss.telegraphLane(), x, y - width / 2f, 0f, width / 2f,
            length, width, 1f, 1f, angle);
        batch.setColor(Color.WHITE);
    }

    private void drawPools(BossAssets boss) {
        for (BossFight.AcidPool pool : fight.pools()) {
            // 데이터의 원점·반경이 판정 기준 — 예고 원과 활성 장판을 같은 값으로 그린다.
            float visualScale = pool.radius / (56f / 128f); // telegraph_circle 가시 반경 보정
            if (!pool.active()) {
                batch.setColor(1f, 1f, 1f, 0.55f);
                batch.draw(boss.telegraphCircle(), pool.x - visualScale / 2f,
                    pool.y - visualScale / 2f, visualScale, visualScale);
                batch.setColor(Color.WHITE);
                // 예고 마지막 2/15초: 작은 생성 프레임(0~1) — 아직 피해 없음.
                if (pool.telegraphRemaining() < 2f * FRAME) {
                    int intro = pool.telegraphRemaining() < FRAME ? 1 : 0;
                    float size = pool.radius * 2.2f;
                    batch.draw(boss.acidFrame(intro), pool.x - size / 2f, pool.y - size / 2f,
                        size, size);
                }
            } else {
                int frame = 2 + (int) (pool.sinceActivation() / FRAME) % 4; // 완성 크기 반복 2~5
                float size = pool.radius * 2.2f;
                batch.draw(boss.acidFrame(frame), pool.x - size / 2f, pool.y - size / 2f,
                    size, size);
            }
        }
    }

    private void drawProjectiles(BossAssets boss) {
        int loop = (int) (fight.phaseTime() / FRAME);
        for (BossFight.BossProjectile p : fight.projectiles()) {
            TextureRegion region = boss.projectileFrame(loop % 4);
            float angle = MathUtils.atan2(p.dirY, p.dirX) * MathUtils.radiansToDegrees;
            batch.draw(region, p.x - 0.55f, p.y - 0.55f, 0.55f, 0.55f, 1.1f, 1.1f, 1f, 1f, angle);
        }
    }

    // ----- 9단계 신규 엔진 렌더(판정 데이터와 같은 기하 — 렌더러는 그리기만) ----------------

    /** 패턴 예고: 부채꼴/직선/레이저/고리 — 수치는 전부 PatternDefinition에서 읽는다. */
    private void drawTelegraphsNine(BossAssets boss,
                                    io.github.some_example_name.asset.BossNineAssets nine) {
        io.github.some_example_name.boss.BossPatternCatalog.Pattern pattern =
            battle.currentPattern();
        BossFight.PatternStage stageNow = battle.patternStage();
        if (pattern == null || stageNow == null) return;
        float pulse = 0.35f + 0.2f * (float) Math.abs(Math.sin(battle.patternElapsed() * 8f));
        float originX = battle.bossX(), originY = battle.bossY() + 1f;
        String kind = pattern.kind;
        boolean telegraph = stageNow == BossFight.PatternStage.TELEGRAPH;
        if ("cone".equals(kind)) {
            // 예고와 판정이 같은 반경·각도: VFX를 조준 방향으로 회전해 반경 크기로 그린다.
            float alpha = telegraph ? pulse : 0.8f;
            int frame = telegraph ? 0
                : Math.min(3, (int) ((battle.patternElapsed() - pattern.windup) / 0.1f));
            float angle = MathUtils.atan2(battle.aimDirY(), battle.aimDirX())
                * MathUtils.radiansToDegrees;
            batch.setColor(1f, 1f, 1f, alpha);
            batch.draw(nine.vfxFrame(pattern.art, frame), originX, originY - pattern.radius / 2f,
                0f, pattern.radius / 2f, pattern.radius, pattern.radius, 1f, 1f, angle);
            batch.setColor(Color.WHITE);
        } else if (telegraph && ("projectile".equals(kind) || "projectile_fan".equals(kind))) {
            drawLane(boss, originX, originY, battle.aimDirX(), battle.aimDirY(), 7f, 0.6f, pulse);
        } else if ("charge".equals(kind) || "repeat_charge".equals(kind)) {
            float distance = pattern.distanceEach > 0f ? pattern.distanceEach : pattern.maxDistance;
            drawLane(boss, battle.bossX(), battle.bossY() + 0.6f,
                battle.aimDirX(), battle.aimDirY(), distance, pattern.laneWidth,
                battle.charging() ? 0.25f : pulse);
        } else if (telegraph && "beam".equals(kind)) {
            drawLane(boss, originX, originY, battle.aimDirX(), battle.aimDirY(),
                pattern.length, pattern.width + 0.3f, pulse * 0.8f);
        } else if (telegraph && "expanding_annulus".equals(kind)) {
            float size = pattern.endRadius * 2f;
            batch.setColor(1f, 1f, 1f, pulse * 0.5f);
            batch.draw(boss.telegraphCircle(), originX - size / 2f, originY - size / 2f,
                size, size);
            batch.setColor(Color.WHITE);
        }
    }

    /** 장판(예고 원=판정 반경)·지연 폭발 표식. */
    private void drawZonesNine(BossAssets boss,
                               io.github.some_example_name.asset.BossNineAssets nine) {
        float pulse = 0.35f + 0.2f * (float) Math.abs(Math.sin(battle.phaseTime() * 8f));
        for (io.github.some_example_name.boss.BossBattle.Zone zone : battle.zones()) {
            float size = zone.radius * 2f;
            if (!zone.active) {
                batch.setColor(1f, 1f, 1f, 0.55f);
                batch.draw(boss.telegraphCircle(), zone.x - size / 2f, zone.y - size / 2f,
                    size, size);
                batch.setColor(Color.WHITE);
            } else {
                int frame = (int) (zone.sinceActivation / 0.12f) % 4;
                float art = zone.radius * 2.2f;
                batch.draw(nine.vfxFrame(zone.art, frame), zone.x - art / 2f, zone.y - art / 2f,
                    art, art);
            }
        }
        for (io.github.some_example_name.boss.BossBattle.Burst burst : battle.bursts()) {
            float size = burst.radius * 2f;
            batch.setColor(1f, 0.85f, 0.85f, pulse + 0.15f);
            batch.draw(boss.telegraphCircle(), burst.x - size / 2f, burst.y - size / 2f,
                size, size);
            batch.setColor(Color.WHITE);
        }
    }

    /** 확장 고리: 현재 반경과 같은 지름으로 psychic_wave를 그린다(차폐 방향은 판정과 함께 약화). */
    private void drawRing(io.github.some_example_name.asset.BossNineAssets nine) {
        io.github.some_example_name.boss.BossBattle.Ring ring = battle.ring;
        if (!ring.active) return;
        float size = ring.currentRadius() * 2f + ring.ringWidth;
        int frame = (int) (ring.elapsed / 0.1f) % 4;
        batch.setColor(1f, 1f, 1f, 0.85f);
        batch.draw(nine.vfxFrame("psychic_wave", frame),
            battle.bossX() - size / 2f, battle.bossY() + 1f - size / 2f, size, size);
        batch.setColor(Color.WHITE);
    }

    /** 회전 레이저: 첫 차폐물에서 판정과 표시를 같이 자른다(trimmedLength). */
    private void drawBeam(io.github.some_example_name.asset.BossNineAssets nine) {
        io.github.some_example_name.boss.BossBattle.Beam beam = battle.beam;
        if (!beam.active || beam.trimmedLength <= 0f) return;
        int frame = (int) (battle.phaseTime() / 0.1f) % 4;
        float width = beam.width + 0.35f;
        batch.draw(nine.vfxFrame("red_laser", frame), battle.bossX(), battle.bossY() + 1f - width / 2f,
            0f, width / 2f, beam.trimmedLength, width, 1f, 1f, beam.angleDegrees);
    }

    private void drawProjectilesNine(BossAssets boss,
                                     io.github.some_example_name.asset.BossNineAssets nine) {
        int loop = (int) (battle.phaseTime() / 0.1f);
        for (io.github.some_example_name.boss.BossBattle.Projectile p : battle.projectiles()) {
            TextureRegion region = nine.hasVfx(p.art)
                ? nine.vfxFrame(p.art, loop % 4) : boss.projectileFrame(loop % 4);
            float angle = MathUtils.atan2(p.dirY, p.dirX) * MathUtils.radiansToDegrees;
            batch.draw(region, p.x - 0.55f, p.y - 0.55f, 0.55f, 0.55f, 1.1f, 1.1f, 1f, 1f, angle);
        }
    }

    /** 9단계 보스 몸체: 정면 시트 1종(몸체 회전 금지), 상태별 세그먼트만 재생. */
    private void drawBossNine(io.github.some_example_name.asset.BossNineAssets nine) {
        BossFight.Phase ph = battle.phase();
        float t = battle.phaseTime();
        int index;
        if (ph == BossFight.Phase.DYING || ph == BossFight.Phase.REWARD_PENDING
            || ph == BossFight.Phase.CLEARED || ph == BossFight.Phase.RETURNING) {
            index = ph == BossFight.Phase.DYING
                ? 12 + Math.min(3, (int) (t / 0.1f)) : 15; // death 12-15, 마지막 유지
        } else {
            int flip = (int) (t / SHEET_FRAME) % 2;
            BossFight.PatternStage stageNow = battle.patternStage();
            io.github.some_example_name.boss.BossPatternCatalog.Pattern current =
                battle.currentPattern();
            boolean moving = battle.charging() || (current != null
                && stageNow == BossFight.PatternStage.ACTIVE
                && ("ground_trail".equals(current.kind)
                    || "moving_ground_burst".equals(current.kind)));
            if (stageNow == null) index = (int) (t / SHEET_FRAME) % 4;          // idle 0-3
            else if (moving) index = 10 + flip;                                  // move 10-11
            else if (stageNow == BossFight.PatternStage.TELEGRAPH) index = 4 + flip;
            else if (stageNow == BossFight.PatternStage.ACTIVE) index = 6 + flip;
            else index = 8 + flip;                                               // recovery 8-9
        }
        // 분노 오라(전환 중+이후): 몸 뒤에 루프 — 피해 없음, 표시 전용.
        io.github.some_example_name.boss.BossPatternCatalog.Pattern current =
            battle.currentPattern();
        if (battle.enraged() || (current != null && "phase_change".equals(current.kind))) {
            float aura = stage.drawWidthTiles * 0.9f;
            batch.setColor(1f, 1f, 1f, 0.55f);
            batch.draw(nine.vfxFrame("enrage_aura", (int) (t / 0.1f) % 4),
                battle.bossX() - aura / 2f, battle.bossY() - 0.3f, aura, aura);
            batch.setColor(Color.WHITE);
        }
        float w = stage.drawWidthTiles, h = stage.drawHeightTiles;
        batch.draw(nine.bossFrame(stage.animationSet, index),
            battle.bossX() - w / 2f, battle.bossY() - h * BOSS9_FOOT, w, h);
    }

    private void drawDepthSorted(BossAssets boss, float animDelta) {
        // 고정 소수의 엔티티 — 발 y 내림차순 수동 정렬.
        List<float[]> order = new ArrayList<>();
        if (legacy) {
            float[][] props = {{8f, 8f}, {22f, 8f}, {8f, 15f}, {22f, 15f}};
            float[][] barriers = {{12f, 11f}, {18f, 11f}};
            for (float[] prop : props) order.add(new float[]{prop[0] + 1f, prop[1], 0});
            for (float[] barrier : barriers)
                order.add(new float[]{barrier[0] + 1.5f, barrier[1], 1});
        } else {
            for (float[] pillar : arenaDef.pillars)
                order.add(new float[]{pillar[0] + pillar[2] / 2f, pillar[1], 0});
        }
        order.add(new float[]{bossX(), bossY(), 2});
        order.add(new float[]{player.feetX(), player.feetY(), 3});
        order.sort(new java.util.Comparator<float[]>() {
            @Override public int compare(float[] a, float[] b) { return Float.compare(b[1], a[1]); }
        });
        for (float[] entry : order) {
            if (entry[2] == 0) { // 기둥 96×128 — footprint 폭 기준 비율 유지, 발 앵커 하단
                float w = legacy ? 1.5f : 2.2f;
                float h = w * 128f / 96f;
                batch.draw(boss.pillar(), entry[0] - w / 2f, entry[1] - h / 16f, w, h);
            } else if (entry[2] == 1) { // 방벽 128×64 → 3×1.5, 발 앵커(64,58)
                batch.draw(boss.barrier(), entry[0] - 1.5f, entry[1] - 0.14f, 3f, 1.5f);
            } else if (entry[2] == 2) {
                if (legacy) drawBoss(boss);
                else drawBossNine(assets.bossNine());
            } else {
                entityRenderer.render(batch, player, animDelta, playerCombat.attack(),
                    config.playerAttackCooldownSeconds);
            }
        }
    }

    private void drawBoss(BossAssets boss) {
        TextureRegion frame;
        BossFight.Phase phase = fight.phase();
        if (phase == BossFight.Phase.DYING || phase == BossFight.Phase.REWARD_PENDING
            || phase == BossFight.Phase.CLEARED || phase == BossFight.Phase.RETURNING) {
            int index = phase == BossFight.Phase.DYING
                ? Math.min(5, (int) (fight.phaseTime() / 0.1f)) : 5; // 사망 1회 후 마지막 유지
            frame = boss.deathFrame(index);
        } else {
            BossFight.PatternStage stage = fight.patternStage();
            int flip = (int) (fight.phaseTime() / 0.1f) % 2;
            if (stage == null) frame = boss.idleFrame((int) (fight.phaseTime() / 0.1f) % 4);
            else if ("ram".equals(fight.patternId()) && fight.charging())
                frame = boss.attackFrame(6 + flip);                       // 돌진 자세
            else if (stage == BossFight.PatternStage.TELEGRAPH)
                frame = boss.attackFrame(flip);                           // 예고 0-1
            else if (stage == BossFight.PatternStage.ACTIVE)
                frame = boss.attackFrame(2 + flip);                       // 발사 2-3
            else frame = boss.attackFrame(4 + flip);                      // 회복 4-5
        }
        batch.draw(frame, fight.bossX() - BOSS_DRAW / 2f,
            fight.bossY() - BOSS_DRAW * BOSS_FOOT, BOSS_DRAW, BOSS_DRAW);
    }

    private void updateCamera() {
        float halfW = camera.viewportWidth / 2f, halfH = camera.viewportHeight / 2f;
        float cx = MathUtils.clamp(player.feetX(), halfW, arenaW - halfW);
        float cy = MathUtils.clamp(player.feetY() + 0.5f, halfH, arenaH - halfH);
        camera.position.set(cx, cy, 0f);
        camera.update();
    }

    /** 개발 전용(LAB_BOSS_AUTO=win|fail): 패턴 관찰 → 자동 토벌/사망 → 결과·보상·복귀 검증. */
    private void runAutoTest() {
        autoFrame++;
        String tag = game.bossStageId().replaceAll("[^0-9A-Za-z]", "_");
        if (autoFrame == 90 || autoFrame == 210 || autoFrame == 330)
            io.github.some_example_name.render.FrameCapture.capture(
                "boss" + tag + "_pattern_f" + autoFrame);
        if ("fail".equals(autoTest) && autoFrame == 240) {
            playerCombat.takeDamage(player, player.hp() + 999, 0f, 0f, 0f);
            Gdx.app.log("Boss", "auto-fail: player killed, phase→" + phase());
        }
        // 캡처용: 보스가 프레임에 들어오게 접근 + 관찰 중 사망 방지(개발 전용 경로).
        if ("win".equals(autoTest) && !legacy && autoFrame == 150)
            player.bounds().setPosition(bossX() - player.bounds().width / 2f, bossY() - 6f);
        if ("win".equals(autoTest) && autoFrame % 60 == 30 && !player.isDead())
            player.healToFullHealth();
        if ("win".equals(autoTest) && autoFrame >= 120 && autoFrame % 60 == 0
            && bossProxy.isAlive()) {
            bossProxy.health().damage(Math.max(120, bossMaxHp() / 5));
            Gdx.app.log("Boss", "auto-hit bossHp=" + bossProxy.health().hp());
        }
        if (autoFrame == 480)
            io.github.some_example_name.render.FrameCapture.capture("boss" + tag + "_result");
        if (autoFrame == 540) {
            Gdx.app.log("Boss", "auto end phase=" + phase()
                + " gateClosed=" + room.gateClosed());
            if ("win".equals(autoTest)) requestReturn();
            else Gdx.app.exit();
        }
        if (autoFrame >= 700) Gdx.app.exit();
    }

    @Override public void resize(int width, int height) {
        viewport.update(width, height, false);
        updateCamera();
    }

    @Override public void dispose() {
        batch.dispose();
        hud.dispose();
        bossHud.dispose();
        resultPanel.dispose();
        skillBar.dispose();
    }
}

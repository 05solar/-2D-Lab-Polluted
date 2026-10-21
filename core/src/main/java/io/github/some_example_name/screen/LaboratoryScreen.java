package io.github.some_example_name.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.github.some_example_name.LaboratoryGame;
import io.github.some_example_name.asset.GameAssets;
import io.github.some_example_name.combat.CombatCollisionGrid;
import io.github.some_example_name.combat.CombatSystem;
import io.github.some_example_name.combat.MonsterAiSystem;
import io.github.some_example_name.combat.PlayerAttackState;
import io.github.some_example_name.combat.PlayerCombatState;
import io.github.some_example_name.combat.PlayerDashState;
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
import io.github.some_example_name.combat.ElectricProjectile;
import io.github.some_example_name.combat.PlayerShieldState;
import io.github.some_example_name.combat.ShockwaveState;
import io.github.some_example_name.combat.SkillCast;
import io.github.some_example_name.profile.SkillCatalog;
import io.github.some_example_name.profile.SkillLoadout;
import io.github.some_example_name.profile.SkillService;
import io.github.some_example_name.movement.MovementSystem;
import io.github.some_example_name.movement.RunStartDetector;
import io.github.some_example_name.render.AnimationController;
import io.github.some_example_name.render.CombatRenderer;
import io.github.some_example_name.render.DebugRenderer;
import io.github.some_example_name.render.EntityRenderer;
import io.github.some_example_name.render.MonsterRenderer;
import io.github.some_example_name.render.CombatHudRenderer;
import io.github.some_example_name.render.PlayerHudRenderer;
import io.github.some_example_name.render.TileDebugRenderer;
import io.github.some_example_name.render.WorldRenderer;
import io.github.some_example_name.render.NavigationRenderer;
import io.github.some_example_name.collision.CollisionSystem;
import io.github.some_example_name.world.LaboratoryLayout;
import io.github.some_example_name.world.LaboratoryRoom;
import io.github.some_example_name.world.PlayerSessionState;
import io.github.some_example_name.world.RoomVisuals;
import io.github.some_example_name.world.RoomPathfinder;
import io.github.some_example_name.world.LootContainer;
import io.github.some_example_name.world.LootDrop;
import io.github.some_example_name.world.LootContainerPlacement;
import io.github.some_example_name.world.Door;
import io.github.some_example_name.world.DoorState;
import io.github.some_example_name.inventory.LootItem;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import java.util.EnumSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

/** Laboratory frame coordinator. Combat rules stay in world/combat systems. */
public class LaboratoryScreen extends BaseScreen {
    // A 20x15 view keeps the full room scale legible while exposing adjoining halls.
    private static final float VIEW_WIDTH = 20f, VIEW_HEIGHT = 15f;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final FitViewport viewport;
    private final boolean overviewMode;
    private final SpriteBatch batch = new SpriteBatch();
    private final BalanceConfig config;
    private final LaboratoryRoom room;
    private final RoomVisuals visuals;
    private final Player player;
    private final List<Monster> monsters;
    private final InputState inputState = new InputState();
    private final KeyBindings bindings = KeyBindings.defaults();
    private final GdxPlayerInput input = new GdxPlayerInput(bindings);
    private final PlayerController controller = new PlayerController();
    private final MovementSystem movementSystem;
    private final CombatCollisionGrid playerGrid;
    private final PlayerCombatState playerCombat;
    private final CombatSystem combatSystem;
    private final MonsterAiSystem monsterAi;
    private final WorldRenderer worldRenderer;
    private final EntityRenderer entityRenderer;
    private final MonsterRenderer monsterRenderer;
    private final CombatRenderer combatRenderer = new CombatRenderer();
    private final DebugRenderer debugRenderer = new DebugRenderer();
    private final TileDebugRenderer tileDebugRenderer = new TileDebugRenderer();
    private final PlayerHudRenderer hud;
    private final NavigationRenderer navigationRenderer = new NavigationRenderer();
    private final RoomPathfinder pathfinder = new RoomPathfinder();
    private final PlayerSessionState session;
    private final io.github.some_example_name.render.ExteriorWallRenderer exteriorWalls;
    private final boolean exteriorRing;
    private boolean paused;
    private boolean debugEnabled = "1".equals(System.getenv("LAB_DEBUG_COLLISION"));
    private boolean tileDebugEnabled;
    private boolean backpackOpen;
    // LAB_AUTO_ROUTE=1(개발 전용): 귀환 버튼을 누른 상태로 시작해 발자국 렌더를 캡처 검증.
    private boolean routeEnabled = "1".equals(System.getenv("LAB_AUTO_ROUTE"));
    // 개발용 자동 캡처: LAB_AUTO_CAPTURE 설정 시 몇 프레임 뒤 UI/충돌 프레임을 저장하고 종료한다.
    private final boolean autoCapture = System.getenv("LAB_AUTO_CAPTURE") != null;
    private int autoCaptureFrame;
    // LAB_WALL_TEST=3: V4 벽 47종 + 가로/세로 방문 4종을 격자로 직접 그려 한 번씩 확인.
    private final boolean v4AtlasView = "3".equals(System.getenv("LAB_WALL_TEST"));
    private List<RoomPathfinder.Cell> exitPath = java.util.Collections.emptyList();
    private final List<LootContainer> lootContainers = new ArrayList<>();
    private final List<LootDrop> lootDrops = new ArrayList<>();
    private final Random lootRandom = new Random();
    private final io.github.some_example_name.render.ContainerInteractionRenderer containerFx;
    private LootContainer promptTarget;        // 이번 프레임 상호작용 대상(안내 표시용)
    private float containerGlowHeight = 1f;    // 마지막 유효 입력 대상의 표시 높이(발광 반경 기준)
    // 돌진: 이동·충돌·대상당 1회 피해 상태만 — 쿨다운은 공통 SkillRuntime("dash")이 관리한다.
    private final PlayerDashState dash;
    private final CollisionSystem dashCollision = new CollisionSystem();
    private final Rectangle dashHitArea = new Rectangle();
    private final CombatHudRenderer combatHud;
    private final io.github.some_example_name.render.SfxToggleRenderer sfxToggle =
        new io.github.some_example_name.render.SfxToggleRenderer();
    private final String attackKeyLabel;
    // LAB_PERF=1: 프레임 시간(avg/max)·몬스터 수를 120프레임마다 기록(EASY 8배 성능 측정).
    private final boolean perfLog = "1".equals(System.getenv("LAB_PERF"));
    private float perfAccum, perfMax;
    private int perfFrames;
    // 공통 스킬 시스템: 슬롯 키 폴링 → SkillService 파이프라인 → 효과/쿨다운/연출.
    // 회복 캐스트 연출(시트 1회 재생)·퀵슬롯 HUD·장착/키 등록 패널.
    private final io.github.some_example_name.render.HealCastRenderer healCast;
    private final io.github.some_example_name.render.SkillBarRenderer skillBar;
    private final io.github.some_example_name.render.SkillLoadoutRenderer skillPanel;
    private final io.github.some_example_name.render.SkillMenuButtonRenderer skillMenuButton =
        new io.github.some_example_name.render.SkillMenuButtonRenderer();
    // 추가 스킬 3종(충격파/전기 투척/보호막) 활성 효과 — 순수 모델이 판정하고 화면은
    // 공용 타격·VFX만 연결한다. 맵 이탈/화면 재생성 시 함께 사라진다(쿨다운은 게임 루트 유지).
    private final List<ShockwaveState> shockwaves = new ArrayList<>();
    private final List<ElectricProjectile> projectiles = new ArrayList<>();
    private SkillCast electricCast;              // 준비 2/15초 — 방향은 시전 시 확정
    private float electricDirX, electricDirY;
    private final PlayerShieldState shieldState;
    private final io.github.some_example_name.render.SkillVfxRenderer skillVfx;
    private final ShockwaveState.Occlusion shockOcclusion;
    private final ElectricProjectile.Solid projectileSolid;
    // 사망 → 쓰러짐 연출을 보여준 뒤 소지품을 모두 잃고 본부로 재소환한다(1회).
    private float deathTimer;
    private boolean deathRespawnRequested;
    // LAB_TEST_DEATH=1(개발 전용): 잠시 뒤 치명 피해를 가해 사망→소지품 상실→본부 재소환을 실행 검증.
    private final boolean testDeath = "1".equals(System.getenv("LAB_TEST_DEATH"));
    private int testDeathFrame;

    public LaboratoryScreen(LaboratoryGame game, GameAssets assets, LaboratoryLayout layout,
                            PlayerSessionState session, List<Monster> monsters, BalanceConfig config) {
        super(game, assets);
        this.config = config;
        this.session = session;
        // 텍스트 렌더러는 GameAssets가 소유한 공유 Mona 폰트를 빌려 쓴다.
        hud = new PlayerHudRenderer(assets.uiFont());
        combatHud = new CombatHudRenderer(assets.uiFont());
        containerFx = new io.github.some_example_name.render.ContainerInteractionRenderer(
            assets.worldFont());
        room = layout.room();
        visuals = layout.visuals();
        player = session.player();
        overviewMode = "1".equals(System.getenv("LAB_MAP_OVERVIEW"));
        viewport = new FitViewport(overviewMode ? room.widthInTiles() : VIEW_WIDTH,
            overviewMode ? room.heightInTiles() : VIEW_HEIGHT, camera);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        for (LootContainerPlacement placement : layout.lootContainers())
            lootContainers.add(assets.loot().newContainer(placement.id(), placement.x(), placement.y(),
                config.requiredPresses(placement.id())));
        this.monsters = monsters;
        RunStartDetector runStartDetector = new RunStartDetector();
        movementSystem = new MovementSystem(new CollisionSystem(),
            runStartDetector, config.playerWalkSpeed, config.playerRunSpeed);
        playerGrid = new CombatCollisionGrid(room);
        PlayerAttackState attack = new PlayerAttackState(config.playerAttackCooldownSeconds,
            assets.playerAnimations().attackFrameCount(Direction.DOWN),
            assets.playerAnimations().hitFrames(Direction.DOWN));
        playerCombat = new PlayerCombatState(attack);
        combatSystem = new CombatSystem(config);
        monsterAi = new MonsterAiSystem(config, room, config.monsterSpawnSeed);
        worldRenderer = new WorldRenderer(assets.tileSet(), assets.roomKits(),
            assets.laboratoryFurniture());
        entityRenderer = new EntityRenderer(new AnimationController(assets.playerAnimations()));
        monsterRenderer = new MonsterRenderer(assets.monsterAnimations());
        exteriorRing = layout.hasExteriorRing();
        exteriorWalls = new io.github.some_example_name.render.ExteriorWallRenderer(assets.exteriorWalls());
        // 모든 난이도 맵의 벽·문을 V4 규격으로 논리 점유에서 직접 그린다(구형 스킨/Interior/Exterior 링 대체).
        worldRenderer.setLaboratoryStructureV4(
            new io.github.some_example_name.render.LaboratoryStructureRendererV4(assets.structureV4()), room);
        dash = new PlayerDashState(config.dashDistanceTiles, config.dashDurationSeconds);
        healCast = new io.github.some_example_name.render.HealCastRenderer(assets.skills());
        skillBar = new io.github.some_example_name.render.SkillBarRenderer(assets.uiFont());
        skillPanel = new io.github.some_example_name.render.SkillLoadoutRenderer(
            assets.skills(), assets.threeSkills(), assets.combatUi().dashIcon(), assets.uiFont());
        // 보호막: 공통 피해 파이프라인에 연결(흡수는 PlayerCombatState.takeDamage 한 곳).
        shieldState = new PlayerShieldState(game.skillCatalog().shieldActivateDelay);
        playerCombat.setShield(shieldState);
        skillVfx = new io.github.some_example_name.render.SkillVfxRenderer(
            assets.threeSkills(), game.skillCatalog().shockwaveRadius);
        // 충격파 차폐: 벽·닫힌 문·solid 가구(장식 오버레이 제외) — 기존 시야/통로 검사 재사용.
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
        // 몬스터에 실제 피해가 적용된 순간에만 타격음(허공 공격은 스윙음만).
        combatSystem.setStrikeListener(new io.github.some_example_name.combat.CombatSystem.StrikeListener() {
            @Override public void onMonsterStruck(Monster monster, boolean killed) {
                assets.combatSounds().playMonsterHit();
            }
        });
        assets.combatSounds().setMasterVolume(config.sfxMasterVolume);
        // 공격 HUD 키캡 라벨은 실제 바인딩에서 읽는다(임의 재지정 금지).
        // 키보드 바인딩이 없으면 마우스 클릭 전용이므로 CLICK으로 표기한다.
        String attackKey = bindings.primaryKeyName(GameAction.ATTACK);
        attackKeyLabel = attackKey.isEmpty() ? "CLICK" : attackKey.toUpperCase();
        dumpResolvedGridIfRequested();
        updateCamera();
    }

    private void dumpResolvedGridIfRequested() {
        if (System.getenv("LAB_DUMP") == null) return;
        for (int ty = visuals.heightInTiles() - 1; ty >= 0; ty--) {
            StringBuilder row = new StringBuilder();
            for (int tx = 0; tx < visuals.widthInTiles(); tx++) {
                row.append(tx).append(',').append(ty).append('|')
                    .append(nz(visuals.floorAt(tx, ty))).append('|')
                    .append(nz(visuals.overlayAt(tx, ty))).append('|')
                    .append(nz(visuals.wallAt(tx, ty))).append('|')
                    .append(nz(visuals.structureAt(tx, ty))).append(';');
            }
            Gdx.app.log("LabDump", row.toString());
        }
    }
    private static String nz(String value) { return value == null ? "-" : value; }

    @Override public void render(float delta) {
        float dt = Math.min(delta, config.maxFrameDeltaSeconds);
        boolean transitionLocked = game.transition().inputLocked();
        if (transitionLocked) inputState.update(EnumSet.noneOf(GameAction.class));
        else input.poll(inputState);
        boolean active = !transitionLocked;
        if (active && !skillPanel.isOpen() && Gdx.input.isKeyJustPressed(Input.Keys.R))
            backpackOpen = !backpackOpen;
        if (active && inputState.isPressed(GameAction.PAUSE)) {
            // Esc 우선순위: 스킬 패널(키 대기 취소 포함) → 가방 → 일시정지.
            if (!skillPanel.onEscape()) {
                if (backpackOpen) backpackOpen = false;
                else paused = !paused;
            }
        }
        // H: 귀환 경로 토글(재추가 — 미니맵 아래 버튼과 같은 동작, 엣지 1회).
        if (active && !backpackOpen && !paused && !skillPanel.isOpen()
            && inputState.isPressed(GameAction.RETURN_ROUTE))
            routeEnabled = !routeEnabled;
        if (active) skillPanel.handleInput();
        if (active && Gdx.input.isKeyJustPressed(Input.Keys.F1)) debugEnabled = !debugEnabled;
        if (active && Gdx.input.isKeyJustPressed(Input.Keys.F2)) tileDebugEnabled = !tileDebugEnabled;
        // F12: 현재 프레임을 PNG로 저장(실제 렌더 검증). F1 켜면 충돌 디버그 포함 프레임이 저장된다.
        boolean captureRequested = active && Gdx.input.isKeyJustPressed(Input.Keys.F12);
        // 버튼 클릭: 오디오 토글(HP 바 오른쪽) 또는 귀환 경로 토글(미니맵 아래 버튼 — 단축키 없음).
        // UI 버튼을 클릭한 프레임은 같은 클릭이 공격(마우스 전용)으로 새지 않게 소비한다.
        boolean uiClickConsumed = false;
        if (active && !backpackOpen && !paused && !skillPanel.isOpen() && Gdx.input.justTouched()) {
            if (sfxToggle.buttonHit(Gdx.input.getX(), Gdx.input.getY())) {
                assets.toggleAudio();
                uiClickConsumed = true;
            } else if (skillMenuButton.buttonHit(Gdx.input.getX(), Gdx.input.getY())) {
                openSkillPanel();
                uiClickConsumed = true;
            } else if (navigationRenderer.buttonHit(Gdx.input.getX(), Gdx.input.getY())) {
                routeEnabled = !routeEnabled;
                uiClickConsumed = true;
            }
        }
        // [임시] 월드 아이템 획득 시스템이 아직 없어, G로 테스트 물품을 회수해 8칸/가득참을 확인한다.
        if (active && !backpackOpen && Gdx.input.isKeyJustPressed(Input.Keys.G)
            && !session.collect("debug_sample")) hud.flashBackpackFull();

        boolean runStarted = false;
        float animDelta = paused || transitionLocked || backpackOpen || skillPanel.isOpen() ? 0f : dt;
        if (!paused && !transitionLocked && !backpackOpen && !skillPanel.isOpen() && !player.isDead()) {
            int hpBefore = player.hp();
            if (testDeath && ++testDeathFrame == 30) {
                session.collect("debug_death_a");
                session.collect("debug_death_b");
                Gdx.app.log("TestDeath", "items before death=" + session.inventory().size());
                player.takeDamage(player.hp());
            }
            PlayerIntent intent = controller.intentFrom(player, inputState);
            // 공격은 마우스 클릭 전용(SPACE는 상호작용 전용이라 충돌 없음). UI 버튼을 누른
            // 클릭은 공격으로 처리하지 않는다.
            if (uiClickConsumed && intent.attack)
                intent = new PlayerIntent(intent.moveX, intent.moveY, intent.run, false, intent.interact);
            // 스킬 슬롯 키 폴링: 등록된 실제 키만, 엣지 1회(길게 눌러도 재발동 없음).
            // 장착/해금/쿨다운/효과별 검증과 상태 변경은 전부 SkillService 공통 파이프라인이
            // 처리하고, 실패 시 HP·쿨다운·이펙트가 바뀌지 않는다. 사망/일시정지/가방/패널/
            // 전환 중에는 이 블록에 들어오지 않아 입력이 차단된다.
            for (int slot = 0; slot < SkillLoadout.SLOT_COUNT; slot++) {
                int key = game.skillLoadout().keyAt(slot);
                if (key != SkillLoadout.KEY_NONE && Gdx.input.isKeyJustPressed(key))
                    trySkillSlot(slot, intent.moveX, intent.moveY);
            }
            // 쿨다운은 플레이 가능한 시간에만 흐른다(일시정지/가방/패널/전환 중 정지).
            game.tickSkillCooldowns(dt);
            // 충격파/투사체/감전/보호막 진행도 같은 게임 시간으로만 흐른다.
            updateSkillEffects(dt);
            if (intent.attack && playerCombat.attack().start(player.facing()))
                assets.combatSounds().playAttackSwing();
            playerGrid.forPlayer(monsters);
            float previousFeetY = player.feetY();
            float dashStep = dash.advance(dt);
            if (dashStep > 0f) {
                // 돌진은 MovementSystem과 별개 상태로 처리해 Shift 달리기 시작(소음) 이벤트가
                // 중복 발생하지 않는다. 몬스터는 통과하며 경로 전체에 타격 판정만 받는다.
                performDashMovement(dashStep);
                player.setMoving(true);
            } else {
                runStarted = movementSystem.update(player, intent, playerGrid, dt);
            }
            updateLoot(animDelta, intent.interact, Gdx.input.isKeyJustPressed(Input.Keys.D));
            // 남쪽 복귀 트리거: 돌진으로 대문을 통과해도 걷기와 동일하게 1회 발동한다.
            float exitMoveY = dashStep > 0f ? dash.dirY() : intent.moveY;
            if (room.crossedSouthExit(previousFeetY, player.feetY(), player.feetX(), exitMoveY))
                game.transition().request(io.github.some_example_name.world.MapId.HEADQUARTERS);
            playerCombat.update(dt);
            monsterAi.update(monsters, player, playerCombat, room, assets.monsterAnimations(), runStarted, dt);
            if (player.isDead()) {
                movementSystem.stop(player);
                playerCombat.attack().cancel();
                dash.cancel();
                // 사망 중단: 아직 발생하지 않은 효과(준비 중 투사체)를 취소하고 보호막·VFX를
                // 정리한다. 이미 시작한 쿨다운은 되돌리지 않는다.
                if (electricCast != null) { electricCast.cancel(); electricCast = null; }
                shieldState.clear();
                shockwaves.clear();
                projectiles.clear();
                skillVfx.clear();
            } else {
                combatSystem.updatePlayerAttack(player, playerCombat, assets.playerAnimations(), monsters, room, dt);
            }
            // 피격/사망 효과음: 실제 HP 감소(무적 거절 제외)에 1회, 살아 있음→사망 전환에 1회.
            if (hpBefore > 0 && player.isDead()) assets.combatSounds().playPlayerDeath();
            else if (player.hp() < hpBefore) assets.combatSounds().playPlayerHurt();
        } else {
            player.setMoving(false);
        }

        // 사망 재소환: 쓰러짐 연출(0.8s)을 포함한 대기 후 소지품을 모두 비우고 본부 전환을
        // 1회만 요청한다. 부활(HP 복구)은 본부 도착 시점에 게임 루트가 수행한다.
        if (player.isDead() && !paused && !transitionLocked) {
            deathTimer += dt;
            if (!deathRespawnRequested && deathTimer >= config.deathRespawnDelaySeconds) {
                deathRespawnRequested = true;
                session.loseAllItemsOnDeath();
                game.transition().request(io.github.some_example_name.world.MapId.HEADQUARTERS);
            }
        }

        // 상호작용 안내 대상: 플레이 가능한 상태에서만 선택(가방/일시정지/사망/전환 중엔 없음).
        promptTarget = (!paused && !transitionLocked && !backpackOpen && !skillPanel.isOpen()
            && !player.isDead()) ? selectContainerTarget() : null;
        containerFx.update(animDelta);

        updateCamera();
        playerGrid.forPlayer(monsters);
        if (routeEnabled && !player.isDead()) {
            int sx = (int) player.feetX(), sy = (int) player.feetY();
            int tx = room.southExitCenterX(), ty = room.southExitPathY();
            float bw = player.bounds().width, bh = player.bounds().height;
            // 1차: 살아 있는 몬스터 몸체까지 피하는 경로. 시작/목표 셀은 주변 보정.
            exitPath = pathfinder.shortestPathNear(playerGrid, sx, sy, tx, ty, bw, bh);
            // 2차: 몬스터(EASY 8배·로밍)가 복도를 완전히 막아도 발자국이 사라지지 않도록
            // 정적 지형(벽·문·가구·상자)만으로 폴백 — 안내는 항상 표시된다.
            if (exitPath.isEmpty())
                exitPath = pathfinder.shortestPathNear(room, sx, sy, tx, ty, bw, bh);
        } else exitPath = java.util.Collections.emptyList();
        navigationRenderer.updateFootsteps(animDelta, player.isMoving(), player.feetX(), player.feetY());
        // The desktop backbuffer must remain opaque even while sprite/shape
        // layers blend translucent pixels into RGB.
        Gdx.gl.glColorMask(true, true, true, true);
        ScreenUtils.clear(0.05f, 0.05f, 0.06f, 1f);
        Gdx.gl.glColorMask(true, true, true, false);
        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        worldRenderer.render(batch, visuals, camera, player.feetY());
        if (v4AtlasView) drawV4AtlasGallery();
        // 캐비넷은 1.4배(설정값), 나무 상자는 1배. 닫힘·진행·열림 전 프레임이 같은
        // 하단-중앙 기준점을 쓰므로 배율만 키워도 기준이 어긋나지 않는다.
        for (LootContainer chest : lootContainers) {
            float s = containerDrawSize(chest.id());
            batch.draw(assets.loot().chest(chest.id(), chest.frame()),
                chest.x() - s / 2f, chest.y(), s, s);
        }
        containerFx.renderGlow(batch, containerGlowHeight);
        // 충격파 링: 바닥/엔티티 아래 레이어(몬스터·플레이어보다 먼저 그린다).
        skillVfx.renderGround(batch, shockwaves);
        for (LootDrop drop : lootDrops) if(!drop.collected())
            batch.draw(assets.loot().itemBase(drop.item()),drop.x()-.28f,drop.y()+.18f,.56f,.56f);
        monsterRenderer.render(batch, monsters);
        entityRenderer.render(batch, player, animDelta, playerCombat.attack(),
            config.playerAttackCooldownSeconds);
        // 회복 캐스트 시트(1회): 플레이어를 따라다니며 캐릭터 위에 알파 0.7로 덧그린다.
        healCast.update(animDelta);
        healCast.render(batch, player.feetX(), player.feetY());
        // 피격·착탄·투사체·보호막·감전 오버레이: 엔티티 위 레이어(월드 좌표 — HUD/미니맵
        // 아래). 보호막은 낮은 알파로 캐릭터가 읽힌다.
        skillVfx.renderOverlay(batch, projectiles, monsters, shieldState,
            player.feetX(), player.feetY());
        worldRenderer.renderForegroundProps(batch, visuals, camera, player.feetY());
        // 아이템 공개 연출: 컨테이너/가구에 가려지지 않는 월드 효과 레이어(HUD보다 아래).
        containerFx.renderReveal(batch);
        // SPACE 안내는 가구/플레이어 위에 그려 항상 읽히게 한다(열린 컨테이너는 표시 안 함).
        // 기준점은 PNG 캔버스가 아니라 측정된 불투명 그림 상단 + 화면 6px 환산 간격.
        if (promptTarget != null)
            containerFx.renderPrompt(batch, promptTarget,
                containerDrawSize(promptTarget.id()) * assets.loot().opaqueTopRatio(promptTarget.id()),
                promptGapWorld(),
                assets.headquarters().promptFrame(1), assets.headquarters().promptFrame(2));
        batch.end();
        if (exteriorRing)
            exteriorWalls.renderGate(camera, room.southExitCenterX() + 0.5f, !room.gateClosed());
        if (!overviewMode)
            navigationRenderer.render(camera, room, player.feetX(), player.feetY(),
                player.bounds().width, player.bounds().height, exitPath, monsters,
                room.southExitCenterX(), room.southExitPathY(),
                assets.minimapFrame(), assets.footprints(), assets.returnRouteButton(), routeEnabled);
        combatRenderer.renderWorld(camera, monsters, player, playerCombat.attack(), config);
        if (tileDebugEnabled) tileDebugRenderer.render(camera, room, visuals, batch);
        if (debugEnabled) {
            debugRenderer.render(camera, room, player);
            combatRenderer.renderDebug(camera, player, monsters, playerCombat.attack(), config, room);
        }
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        if (!overviewMode) {
            hud.render(assets.hud(), assets.loot(), assets.fadePixel(), player,
                session.backpack(), backpackOpen, dt);
            // 공격 키 안내(우하단) — 스킬은 하단 중앙 퀵슬롯 바가 담당한다.
            combatHud.render(assets.combatUi(), attackKeyLabel);
            skillBar.render(assets.skills(), assets.threeSkills(), assets.combatUi().dashIcon(),
                assets.fadePixel(), game.skillLoadout(), game.skillRuntime(),
                game.activeSkillSlots(), skillCooldownTotals());
            // 효과음 토글 버튼: 좌상단 HP 바 바로 오른쪽(프레임 세로 중앙 정렬, 64px).
            // 스킬 패널 버튼은 그 오른쪽에 같은 크기로.
            io.github.some_example_name.asset.HudLayout hudLayout = assets.hud().layout();
            sfxToggle.render(assets.sfxToggle(assets.combatSounds().sfxEnabled()),
                hudLayout.marginX + hudLayout.frameW + 14f,
                hudLayout.marginY + (hudLayout.frameH - 64f) / 2f, 64f);
            skillMenuButton.render(assets.skills().slotNormal(), assets.skills().iconHeal(),
                hudLayout.marginX + hudLayout.frameW + 14f + 64f + 10f,
                hudLayout.marginY + (hudLayout.frameH - 64f) / 2f, 64f);
            skillPanel.render();
        }
        renderFade(batch, viewport);
        // Some blended debug/font pixels can lower the default framebuffer's
        // alpha. Restore opacity without touching the rendered RGB image so
        // desktop windows never show through the game surface.
        Gdx.gl.glColorMask(false, false, false, true);
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glColorMask(true, true, true, true);
        // LAB_PERF=1: 몬스터 수 증가(EASY 8배)에 따른 프레임 시간 측정(120프레임 평균/최대).
        if (perfLog) {
            perfAccum += delta;
            perfMax = Math.max(perfMax, delta);
            if (++perfFrames >= 120) {
                Gdx.app.log("Perf", String.format(
                    "monsters=%d avgFrame=%.2fms maxFrame=%.2fms (~%.0f fps)",
                    monsters.size(), perfAccum / perfFrames * 1000f, perfMax * 1000f,
                    perfFrames / perfAccum));
                perfAccum = 0f; perfMax = 0f; perfFrames = 0;
            }
        }
        if (captureRequested)
            io.github.some_example_name.render.FrameCapture.capture(
                "lab_" + difficultyTag() + (debugEnabled ? "_collision" : "_ui"));
        if (autoCapture) {
            autoCaptureFrame++;
            // LAB_AUTO_PRESS=1: 근처 컨테이너에 유효 입력을 주기적으로 시뮬레이션해
            // 연타 발광/카운트/개봉/드롭을 실제 렌더 프레임으로 검증한다(개발 전용).
            boolean autoPress = "1".equals(System.getenv("LAB_AUTO_PRESS"));
            if (autoPress && autoCaptureFrame >= 12 && autoCaptureFrame % 6 == 0) {
                LootContainer target = selectContainerTarget();
                if (target != null) {
                    LootContainer.PressResult result = target.press(assets.loot().catalog(), lootRandom);
                    if (result != LootContainer.PressResult.ALREADY_OPEN) {
                        containerGlowHeight = containerDrawSize(target.id());
                        boolean opened = result == LootContainer.PressResult.OPENED;
                        containerFx.onValidPress(target, opened);
                        if (opened) assets.combatSounds().playContainerOpen();
                        else assets.combatSounds().playContainerPress();
                        Gdx.app.log("AutoPress", target.id() + " " + result
                            + " " + target.presses() + "/" + target.requiredPresses());
                    }
                }
            }
            // LAB_AUTO_DASH=1: 공통 스킬 경로로 실제 돌진을 발동해 이동·쿨다운 HUD를 검증한다.
            if ("1".equals(System.getenv("LAB_AUTO_DASH"))) {
                if (autoCaptureFrame == 20) {
                    float beforeX = player.feetX(), beforeY = player.feetY();
                    SkillService.Result result =
                        trySkillSlot(game.skillLoadout().slotOf(SkillLoadout.DASH), 1f, 0f);
                    Gdx.app.log("AutoDash", "result=" + result + " at " + beforeX + "," + beforeY);
                }
                if (autoCaptureFrame == 26) {
                    Gdx.app.log("AutoDash", "now at " + player.feetX() + "," + player.feetY()
                        + " cooldown=" + game.skillRuntime().remainingSeconds(SkillLoadout.DASH));
                    io.github.some_example_name.render.FrameCapture.capture(
                        "lab_" + difficultyTag() + "_dash");
                }
            }
            // LAB_AUTO_HEAL=1: 피해 → 회복 스킬 실행 → 캐스트 연출·HP·쿨다운 HUD를 검증.
            // 만피 상태의 재시도도 함께 기록해 실패(FULL_HP) 시 무변화임을 확인한다.
            if ("1".equals(System.getenv("LAB_AUTO_HEAL"))) {
                if (autoCaptureFrame == 20) {
                    Gdx.app.log("AutoHeal", "hp before damage=" + player.hp());
                    player.takeDamage(40);
                    Gdx.app.log("AutoHeal", "hp after damage=" + player.hp());
                }
                if (autoCaptureFrame == 40) {
                    int before = player.hp();
                    SkillService.Result result =
                        trySkillSlot(game.skillLoadout().slotOf(SkillLoadout.HEAL), 0f, 0f);
                    Gdx.app.log("AutoHeal", "result=" + result + " hp " + before + "->" + player.hp()
                        + " cooldown=" + game.skillRuntime().remainingSeconds(SkillLoadout.HEAL));
                }
                if (autoCaptureFrame == 44)
                    io.github.some_example_name.render.FrameCapture.capture(
                        "lab_" + difficultyTag() + "_heal_cast");
                if (autoCaptureFrame == 46) {
                    int before = player.hp();
                    SkillService.Result retry =
                        trySkillSlot(game.skillLoadout().slotOf(SkillLoadout.HEAL), 0f, 0f);
                    Gdx.app.log("AutoHeal", "retry=" + retry + " hp " + before + "->" + player.hp()
                        + " cooldown=" + game.skillRuntime().remainingSeconds(SkillLoadout.HEAL));
                }
                if (autoCaptureFrame == 90)
                    io.github.some_example_name.render.FrameCapture.capture(
                        "lab_" + difficultyTag() + "_heal_cooldown");
            }
            // LAB_AUTO_SKILL3=shockwave|electric|shield: 테스트 몬스터를 소환해 실제 공통
            // 경로로 스킬을 발동하고 판정(피해/감전/흡수)·VFX·쿨다운 HUD를 캡처·로그로 검증.
            String skill3 = System.getenv("LAB_AUTO_SKILL3");
            if (skill3 != null) runSkill3Script(skill3);
            // LAB_CAPTURE_SERIES=1: 몬스터 이동 검증용 연속 캡처(2초 간격 3장).
            if ("1".equals(System.getenv("LAB_CAPTURE_SERIES"))
                && (autoCaptureFrame == 30 || autoCaptureFrame == 150 || autoCaptureFrame == 270))
                io.github.some_example_name.render.FrameCapture.capture(
                    "lab_series_f" + autoCaptureFrame);
            // 개봉 드롭을 실제 수집 경로로 회수해 획득 토스트(이름+금액)를 렌더 프레임으로 검증.
            if (autoPress && autoCaptureFrame == 85 && !lootDrops.isEmpty()) {
                LootDrop drop = lootDrops.get(0);
                if (!drop.collected() && session.backpack().collect(drop.item().id)) {
                    drop.collect();
                    hud.flashLoot(drop.item());
                    assets.combatSounds().playItemPickup();
                    Gdx.app.log("AutoPress", "collected " + drop.item().name + " +" + drop.item().price);
                }
            }
            int uiFrame = autoPress ? 32 : 30;      // 4번째 연타 직후(개봉 전, 안내+카운트 표시 중)
            int debugOn = autoPress ? 52 : 32;
            int debugFrame = autoPress ? 62 : 42;
            int exitFrame = autoPress ? 100 : 46;   // 개봉 애니메이션/드롭까지 관찰
            String exitOverride = System.getenv("LAB_EXIT_AFTER");
            if (exitOverride != null) try { exitFrame = Integer.parseInt(exitOverride.trim()); }
                catch (NumberFormatException ignored) { }
            if (autoCaptureFrame == uiFrame)
                io.github.some_example_name.render.FrameCapture.capture("lab_" + difficultyTag() + "_ui");
            if (autoPress && autoCaptureFrame == 90)
                io.github.some_example_name.render.FrameCapture.capture("lab_" + difficultyTag() + "_opened");
            if (autoCaptureFrame == debugOn) debugEnabled = true;
            if (autoCaptureFrame == debugFrame)
                io.github.some_example_name.render.FrameCapture.capture("lab_" + difficultyTag() + "_collision");
            if (autoCaptureFrame == debugFrame + 1) debugEnabled = false;
            if (autoCaptureFrame >= exitFrame) Gdx.app.exit();
        }
    }

    /**
     * 슬롯 스킬 실행: 공통 파이프라인(SkillService)에 화면 상태·효과 적용을 Context로 넘긴다.
     * 돌진은 기존 이동/충돌/피해 규칙 그대로 시작만 하고, 회복은 계산된 실제 회복량만 적용한다.
     * 실패(쿨다운/만피/잠김/미장착 등)는 아무 상태도 바꾸지 않는다.
     */
    private SkillService.Result trySkillSlot(final int slot, final float moveX, final float moveY) {
        SkillService.Context ctx = new SkillService.Context() {
            @Override public boolean worldSkillInputAllowed() {
                return !paused && !backpackOpen && !skillPanel.isOpen()
                    && !game.transition().inputLocked();
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
                    Direction dashFacing = Direction.fromInput(dash.dirX(), dash.dirY());
                    if (dashFacing != null) player.setFacing(dashFacing);
                    assets.combatSounds().playDash();
                    return true;
                }
                if (SkillLoadout.SHOCKWAVE.equals(skillId)) {
                    // 원점은 시전 시작 위치에 고정 — 이후 이동해도 따라가지 않는다.
                    shockwaves.add(new ShockwaveState(player.feetX(), player.feetY(),
                        game.skillCatalog().shockwaveDamageStart,
                        game.skillCatalog().shockwaveDamageEnd,
                        game.skillCatalog().shockwaveDuration,
                        new ShockwaveState.RadiusAt() {
                            @Override public float radius(float t) {
                                return game.skillCatalog().shockwaveFrontRadius(t);
                            }
                        }, facingDirX(), facingDirY()));
                    return true;
                }
                if (SkillLoadout.ELECTRIC_THROW.equals(skillId)) {
                    if (electricCast != null && electricCast.pending()) return false;
                    // 방향은 시전 시작에 확정(이동 입력 → 없으면 바라보는 방향). 유도 없음.
                    float dx = moveX, dy = moveY;
                    if (dx == 0f && dy == 0f) { dx = facingDirX(); dy = facingDirY(); }
                    float len = (float) Math.sqrt(dx * dx + dy * dy);
                    electricDirX = dx / len;
                    electricDirY = dy / len;
                    electricCast = new SkillCast(game.skillCatalog().electricSpawnDelay);
                    return true;
                }
                if (SkillLoadout.SHIELD.equals(skillId)) {
                    // 준비/활성 중 재시전은 실패(상태·쿨다운 무소모 — tryCast가 거부).
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

    /** 쿨다운 전체 길이 공급: 수치 원본은 BalanceConfig(돌진)·강화/스킬 카탈로그. */
    private io.github.some_example_name.render.SkillBarRenderer.CooldownTotals skillCooldownTotals() {
        return new io.github.some_example_name.render.SkillBarRenderer.CooldownTotals() {
            @Override public float total(String skillId) {
                return game.skillCooldownSeconds(skillId);
            }
        };
    }

    private void openSkillPanel() {
        skillPanel.open(game.skillLoadout(), game.activeSkillSlots(), game.unlockedSkills(),
            KeyBindings.reservedKeys(),
            new io.github.some_example_name.render.SkillLoadoutRenderer.SkillInfo() {
                @Override public int healAmount() { return game.healSkillAmount(); }
                @Override public float cooldownSeconds(String skillId) {
                    return game.skillCooldownSeconds(skillId);
                }
                @Override public int skillDamage(String skillId) { return game.skillDamage(skillId); }
                @Override public int shieldAbsorb() { return game.shieldAbsorbAmount(); }
            },
            new Runnable() { @Override public void run() { game.saveSkillLoadout(); } });
    }

    private float facingDirX() {
        switch (player.facing()) {
            case LEFT: return -1f;
            case RIGHT: return 1f;
            default: return 0f;
        }
    }

    private float facingDirY() {
        switch (player.facing()) {
            case UP: return 1f;
            case DOWN: return -1f;
            default: return 0f;
        }
    }

    /**
     * 추가 스킬 3종의 활성 효과 진행(플레이 가능한 게임 시간만). 판정은 순수 모델이 하고
     * 여기서는 결과를 공용 타격 경로·VFX에 연결한다. 피격/착탄 연출은 실제 명중 시에만
     * 생성된다(빗나간 대상에 미리 띄우지 않는다).
     */
    private void updateSkillEffects(float dt) {
        SkillCatalog cat = game.skillCatalog();
        // 충격파: 파면이 이번 구간에 덮은 대상에 1회 피해+바깥 밀어내기(충돌 해석은 AI의
        // 기존 넉백 경로 — 벽·문·가구를 관통하지 않는다).
        for (java.util.Iterator<ShockwaveState> it = shockwaves.iterator(); it.hasNext();) {
            ShockwaveState wave = it.next();
            for (ShockwaveState.Hit hit : wave.update(dt, monsters, shockOcclusion)) {
                combatSystem.applyPlayerSkillStrike(player, hit.monster,
                    game.skillDamage(SkillLoadout.SHOCKWAVE), hit.dirX, hit.dirY,
                    cat.shockwaveKnockback);
                skillVfx.spawnShockHit(hit.monster.bounds().x + hit.monster.bounds().width / 2f,
                    hit.monster.bounds().y + hit.monster.bounds().height / 2f);
            }
            if (wave.finished()) it.remove();
        }
        // 전기 투척: 준비 2/15초 뒤 spawn_projectile 1회 — 이벤트 시점의 플레이어 위치에서,
        // 시전 시 확정한 방향으로 나타난다.
        if (electricCast != null && electricCast.update(dt)) {
            projectiles.add(new ElectricProjectile(player.feetX(),
                player.feetY() + player.bounds().height * 0.5f, electricDirX, electricDirY,
                cat.electricSpeed, cat.electricMaxDistance, cat.electricCollisionRadius));
            electricCast = null;
            if (autoCapture) Gdx.app.log("Skill3", String.format(
                "projectile spawned at %.2f,%.2f dir %.2f,%.2f",
                player.feetX(), player.feetY() + player.bounds().height * 0.5f,
                electricDirX, electricDirY));
        }
        for (java.util.Iterator<ElectricProjectile> it = projectiles.iterator(); it.hasNext();) {
            ElectricProjectile projectile = it.next();
            ElectricProjectile.Outcome outcome = projectile.update(dt, projectileSolid, monsters);
            if (outcome == ElectricProjectile.Outcome.HIT_MONSTER) {
                Monster target = projectile.hitMonster();
                combatSystem.applyPlayerSkillStrike(player, target,
                    game.skillDamage(SkillLoadout.ELECTRIC_THROW),
                    projectile.dirX(), projectile.dirY(), config.monsterKnockbackDistance);
                if (target.isAlive()) target.applyShock(cat.electricStunSeconds);
                skillVfx.spawnElectricImpact(projectile.x(), projectile.y());
                it.remove();
            } else if (outcome == ElectricProjectile.Outcome.HIT_WALL) {
                skillVfx.spawnElectricImpact(projectile.x(), projectile.y()); // 벽 뒤는 안 맞는다
                if (autoCapture) Gdx.app.log("Skill3", String.format(
                    "projectile HIT_WALL at %.2f,%.2f", projectile.x(), projectile.y()));
                it.remove();
            } else if (outcome == ElectricProjectile.Outcome.EXPIRED) {
                if (autoCapture) Gdx.app.log("Skill3", String.format(
                    "projectile EXPIRED at %.2f,%.2f", projectile.x(), projectile.y()));
                it.remove(); // 최대 사거리 — 피해·착탄 없이 소멸
            }
        }
        // 보호막: 만료를 피해보다 먼저 처리(프레임 선두 update). 파괴/만료 연출은 1회·구분.
        shieldState.update(dt);
        shieldState.consumeActivated(); // 활성 전환은 상태 기반 렌더가 그린다
        if (shieldState.consumeBroke()) skillVfx.spawnShieldBreak(player.feetX(), player.feetY());
        if (shieldState.consumeExpired()) skillVfx.spawnShieldFade(player.feetX(), player.feetY());
        skillVfx.update(dt);
    }

    /** 개발 전용(LAB_AUTO_SKILL3): 추가 스킬 3종 실행 검증 — 소환→발동→판정 로그→캡처. */
    private Monster skill3Target;
    private void runSkill3Script(String which) {
        if (autoCaptureFrame == 12) {
            // LAB_START_ROOM 중앙 스폰은 가구와 겹칠 수 있다 — 몸이 서고, 투사체와 같은
            // 판정으로 남쪽 2.6타일 사격선이 비는 지점으로 옮긴 뒤 검증한다.
            boolean found = false;
            outer:
            for (float oy = -6f; oy <= 6f; oy += 0.5f) {
                for (float ox = 0f; ox <= 8f; ox += 0.5f) {
                    for (float sx : new float[]{ox, -ox}) {
                        float x = player.feetX() + sx, y = player.feetY() + oy;
                        if (room.blocksActorAt(x, y, 0.6f, 0.5f)) continue;
                        boolean clear = true;
                        for (float d = 0f; d <= 2.6f && clear; d += 0.1f)
                            if (room.blocksActorAt(x, y + 0.07f - d, 0.36f, 0.36f)) clear = false;
                        if (clear) {
                            player.bounds().setPosition(x - player.bounds().width / 2f, y);
                            found = true;
                            break outer;
                        }
                    }
                }
            }
            Gdx.app.log("Skill3", String.format("test spot %.2f,%.2f clear=%b",
                player.feetX(), player.feetY(), found));
        }
        if (autoCaptureFrame == 15) {
            // 생존 확인(감전/HP 수치 로그)을 위해 피해 1회에 죽지 않는 60HP로 소환.
            // 방 중앙은 가구가 많아, 가구 footprint가 없는 남쪽 바닥을 향해 검증한다.
            skill3Target = new Monster(9001L, io.github.some_example_name.combat.MonsterType.SLIME,
                player.feetX(), player.feetY() - 2.4f,
                config.monsterBoundsWidth, config.monsterBoundsHeight, 60);
            monsters.add(skill3Target);
            Gdx.app.log("Skill3", which + " target spawned hp=" + skill3Target.health().hp()
                + " at -2.4 tiles(south), player hp=" + player.hp());
        }
        if (autoCaptureFrame == 20) {
            String skillId = "electric".equals(which) ? SkillLoadout.ELECTRIC_THROW
                : "shield".equals(which) ? SkillLoadout.SHIELD : SkillLoadout.SHOCKWAVE;
            player.setFacing(Direction.DOWN);
            SkillService.Result result = trySkillSlot(game.skillLoadout().slotOf(skillId), 0f, 0f);
            Gdx.app.log("Skill3", skillId + " cast result=" + result
                + " cooldown=" + game.skillRuntime().remainingSeconds(skillId));
        }
        if (autoCaptureFrame == 24)
            io.github.some_example_name.render.FrameCapture.capture("skill3_" + which + "_early");
        if (autoCaptureFrame == 34)
            io.github.some_example_name.render.FrameCapture.capture("skill3_" + which + "_mid");
        if ("shield".equals(which)) {
            if (autoCaptureFrame == 40) {
                int hpBefore = player.hp(), absorbBefore = shieldState.absorbRemaining();
                boolean accepted = playerCombat.takeDamage(player, 35, 0f, 0f,
                    config.playerInvulnerabilitySeconds);
                Gdx.app.log("Skill3", "shield hit35 accepted=" + accepted
                    + " absorb " + absorbBefore + "->" + shieldState.absorbRemaining()
                    + " hp " + hpBefore + "->" + player.hp());
            }
            if (autoCaptureFrame == 42) {
                // 무적 중 재공격: 보호막도 깎이지 않아야 한다.
                int absorbBefore = shieldState.absorbRemaining();
                boolean accepted = playerCombat.takeDamage(player, 35, 0f, 0f,
                    config.playerInvulnerabilitySeconds);
                Gdx.app.log("Skill3", "shield invuln-hit accepted=" + accepted
                    + " absorb " + absorbBefore + "->" + shieldState.absorbRemaining());
            }
            if (autoCaptureFrame == 110) {
                int hpBefore = player.hp(), absorbBefore = shieldState.absorbRemaining();
                playerCombat.takeDamage(player, 35, 0f, 0f, config.playerInvulnerabilitySeconds);
                Gdx.app.log("Skill3", "shield hit35(2nd) absorb " + absorbBefore + "->"
                    + shieldState.absorbRemaining() + " hp " + hpBefore + "->" + player.hp());
                io.github.some_example_name.render.FrameCapture.capture("skill3_shield_break");
            }
        }
        if (autoCaptureFrame == 60 && skill3Target != null)
            Gdx.app.log("Skill3", which + " target alive=" + skill3Target.isAlive()
                + " hp=" + skill3Target.health().hp()
                + " shocked=" + skill3Target.shocked()
                + " shockTime=" + skill3Target.shockTime());
        if (autoCaptureFrame >= 130) Gdx.app.exit();
    }

    private String difficultyTag() {
        if (v4AtlasView) return "v4atlas";
        return "w" + room.widthInTiles() + "h" + room.heightInTiles();
    }

    /** 47종 V4 벽 타일(8열)과 가로/세로 방문 닫힘/열림을 월드 좌표 격자로 그린다(확인용). */
    private void drawV4AtlasGallery() {
        io.github.some_example_name.asset.LaboratoryStructureV4Assets a = assets.structureV4();
        float x0 = 1.5f, yTop = room.heightInTiles() - 2f, size = 1.4f, gap = 0.2f;
        for (int i = 0; i < io.github.some_example_name.world.WallMaskV4.VARIANT_COUNT; i++) {
            int col = i % io.github.some_example_name.world.WallMaskV4.ATLAS_COLUMNS;
            int row = i / io.github.some_example_name.world.WallMaskV4.ATLAS_COLUMNS;
            float x = x0 + col * (size + gap);
            float y = yTop - row * (size + gap);
            batch.draw(a.wall(i), x, y, size, size);
        }
        // 방문 4종: 가로(닫힘/열림) 4x1.5, 세로(닫힘/열림) 1.5x4.
        float doorY = 1.5f;
        batch.draw(a.horizontalDoor(false), x0, doorY + 1.5f, 4f, 1.5f);
        batch.draw(a.horizontalDoor(true), x0 + 4.5f, doorY + 1.5f, 4f, 1.5f);
        batch.draw(a.verticalDoor(false), x0 + 9.5f, doorY, 1.5f, 4f);
        batch.draw(a.verticalDoor(true), x0 + 11.5f, doorY, 1.5f, 4f);
    }

    private void updateLoot(float delta, boolean interactPressed, boolean dropPressed) {
        for (LootContainer chest : lootContainers) {
            LootItem reward=chest.update(delta);
            if(reward!=null) {
                // 실제 드롭은 접근 가능한 바닥 지점에(벽 뒤/공중 금지), 공개 연출은 상자 위에.
                // 연출용 아이콘과 드롭은 같은 reward 하나를 공유한다(중복 생성 없음).
                Vector2 dropAt = findDropSpot(chest);
                lootDrops.add(new LootDrop(reward, dropAt.x, dropAt.y));
                float opaqueTop = containerDrawSize(chest.id())
                    * assets.loot().opaqueTopRatio(chest.id());
                containerFx.onReveal(assets.loot().itemFramed(reward), chest.x(),
                    chest.y() + opaqueTop * .55f, chest.y() + opaqueTop + .25f);
            }
        }
        if (!interactPressed && !dropPressed) return;
        float px=player.feetX(), py=player.feetY();
        // 대문 근처에서 SPACE: 닫힘↔열림 토글(열려야 남쪽 통로로 본부 복귀 가능).
        if (interactPressed && nearGate(px, py)) {
            boolean wasClosed = room.gateClosed();
            room.setGateClosed(!wasClosed);
            if (wasClosed) assets.combatSounds().playDoorOpen(); // 닫힘→열림에만 1회
            return;
        }
        // 연타 개봉: 바라보는 가장 가까운 미개봉 컨테이너 하나에만 입력이 들어간다.
        // 입력은 isKeyJustPressed 엣지이므로 길게 눌러도 1회다. 필요 횟수 도달 입력에서만
        // 열림 애니메이션+추첨이 실행되고, 같은 입력으로 수집/방문 개폐가 중복 실행되지 않는다.
        LootContainer target = selectContainerTarget();
        if (interactPressed && target != null) {
            LootContainer.PressResult result = target.press(assets.loot().catalog(), lootRandom);
            if (result != LootContainer.PressResult.ALREADY_OPEN) {
                containerGlowHeight = containerDrawSize(target.id());
                boolean opened = result == LootContainer.PressResult.OPENED;
                containerFx.onValidPress(target, opened);
                if (opened) assets.combatSounds().playContainerOpen();
                else assets.combatSounds().playContainerPress();
            }
            return;
        }
        LootDrop nearest=null; float nearestD=1.45f*1.45f;
        for(LootDrop drop:lootDrops) if(!drop.collected()) {
            float dx=px-drop.x(),dy=py-drop.y();float d=dx*dx+dy*dy;
            if(d<nearestD){nearestD=d;nearest=drop;}
        }
        if(nearest==null) {
            if (interactPressed) {
                Door door = room.nearestDoor(px, py, 1.45f);
                if (door != null) {
                    boolean wasClosed = door.state() == DoorState.CLOSED;
                    door.toggle();
                    // 닫힘→열림 전환에만 재생(같은 문 상태 갱신마다 반복하지 않는다).
                    if (wasClosed) assets.combatSounds().playDoorOpen();
                }
            }
            return;
        }
        if(!session.backpack().isFull()&&interactPressed) {
            if (session.backpack().collect(nearest.item().id)) {
                nearest.collect();
                hud.flashLoot(nearest.item());
                assets.combatSounds().playItemPickup(); // 실제 획득에만 경쾌한 차임 1회
            }
        } else if(session.backpack().isFull()&&dropPressed) {
            String displaced=session.backpack().replaceLowestValue(nearest.item().id,assets.loot().prices());
            if (displaced != null) {
                LootItem old=assets.loot().catalog().find(displaced);
                if(old!=null)lootDrops.add(new LootDrop(old,px,py));
                nearest.collect();
                hud.flashLoot(nearest.item());
                assets.combatSounds().playItemPickup();
            }
        }
        lootDrops.removeIf(LootDrop::collected);

    }

    /**
     * 돌진 이동: 설정된 최대 하위 단계 길이(dashSubstepLength)로 나눠 이동·충돌·타격을
     * 반복한다 — 벽/닫힌 문/가구를 관통하지 않고, 이동 구간 중간의 몬스터도 건너뛰지 않는다.
     * 몬스터는 충돌 대상이 아니라 통과하며(돌진 관통), 대상당 1회만 피해를 받는다.
     * 돌진 무적은 별도 요구가 없어 추가하지 않았다.
     */
    private void performDashMovement(float distance) {
        int substeps = Math.max(1, (int) Math.ceil(distance / config.dashSubstepLength));
        float sub = distance / substeps;
        float dirX = dash.dirX(), dirY = dash.dirY();
        for (int i = 0; i < substeps; i++) {
            io.github.some_example_name.collision.CollisionResult result =
                dashCollision.resolve(player.bounds(), dirX * sub, dirY * sub, room);
            player.bounds().setPosition(result.x, result.y);
            applyDashHits();
            boolean blockedX = result.hitX && dirX != 0f, blockedY = result.hitY && dirY != 0f;
            // 정면이 완전히 막히면 종료(쿨다운 유지). 대각선은 한 축이 열려 있으면 미끄러진다.
            if ((dirY == 0f && blockedX) || (dirX == 0f && blockedY) || (blockedX && blockedY)) {
                dash.interrupt();
                return;
            }
        }
    }

    /** 돌진 경로 위 몬스터 타격: 일반 공격과 같은 피해·넉백·사망·처치 회복 경로를 쓴다. */
    private void applyDashHits() {
        float pad = config.dashHitPadding;
        Rectangle b = player.bounds();
        dashHitArea.set(b.x - pad, b.y - pad, b.width + pad * 2f, b.height + pad * 2f);
        for (Monster monster : monsters) {
            if (!monster.isAlive() || !dashHitArea.overlaps(monster.bounds())) continue;
            if (!dash.markHit(monster.id())) continue; // 한 돌진에서 같은 몬스터는 1회만
            combatSystem.applyPlayerStrike(player, monster, dash.dirX(), dash.dirY());
        }
    }

    /**
     * 개봉 보상이 떨어질 접근 가능한 바닥 지점. 컨테이너 주변 8방을 돌며 플레이어 크기의
     * 몸체가 설 수 있는(벽/가구/컨테이너 footprint 밖) 첫 지점을 고른다 — 벽 뒤나 장애물
     * 위에 떨어져 획득 불가가 되는 문제를 막는다. 모두 막히면 기존처럼 컨테이너 발밑.
     */
    private Vector2 findDropSpot(LootContainer chest) {
        float[][] offsets = {{0f, -.95f}, {-.95f, 0f}, {.95f, 0f}, {0f, .95f},
            {-.7f, -.7f}, {.7f, -.7f}, {-.7f, .7f}, {.7f, .7f}};
        for (float[] offset : offsets) {
            float x = chest.x() + offset[0], y = chest.y() + offset[1];
            if (!room.blocksActorAt(x, y, .5f, .4f)) return new Vector2(x, y);
        }
        return new Vector2(chest.x(), chest.y());
    }

    /** SPACE 안내 간격: 화면 6px(4~8px 시작값)를 현재 카메라 배율로 환산한 월드 거리. */
    private float promptGapWorld() {
        int screenWidth = viewport.getScreenWidth();
        if (screenWidth <= 0) return 0.1f;
        return 6f * camera.viewportWidth / screenWidth;
    }

    /** 컨테이너 표시 크기: 캐비넷은 설정 배율(기본 1.4), 나무 상자는 1. */
    private float containerDrawSize(String id) {
        return id.endsWith("_cabinet") ? config.cabinetRenderScale : 1f;
    }

    /**
     * 상호작용 거리 안의 미개봉 컨테이너 중 "플레이어가 바라보는 가장 가까운" 하나를 고른다.
     * 거리에서 바라보는 방향 정합(내적)을 빼서 정면의 대상을 우선하되, 등 뒤의 더 가까운
     * 대상만 있으면 그것을 고른다. 여러 대상의 진행도가 동시에 오르는 일이 없도록 단일 선택.
     */
    private LootContainer selectContainerTarget() {
        float px = player.feetX(), py = player.feetY();
        float maxD2 = config.containerInteractRadius * config.containerInteractRadius;
        float fx = 0f, fy = 0f;
        switch (player.facing()) {
            case UP: fy = 1f; break;
            case DOWN: fy = -1f; break;
            case LEFT: fx = -1f; break;
            default: fx = 1f; break;
        }
        LootContainer best = null;
        float bestScore = Float.MAX_VALUE;
        for (LootContainer chest : lootContainers) {
            if (chest.opened()) continue;
            float dx = chest.x() - px, dy = chest.y() - py;
            float d2 = dx * dx + dy * dy;
            if (d2 > maxD2) continue;
            float dist = (float) Math.sqrt(d2);
            float dot = dist < 0.001f ? 1f : (dx * fx + dy * fy) / dist;
            float score = dist - 0.75f * dot;
            if (score < bestScore) { bestScore = score; best = chest; }
        }
        return best;
    }

    /** 남쪽 대문 통로 접근 범위(SPACE로 대문 토글). */
    private boolean nearGate(float px, float py) {
        return exteriorRing && room.hasGate() && py < 5f
            && Math.abs(px - (room.southExitCenterX() + 0.5f)) < 3f;
    }

    private void updateCamera() {
        if (overviewMode) {
            camera.position.set(room.widthInTiles() / 2f, room.heightInTiles() / 2f, 0f);
            camera.update();
            return;
        }
        float halfW = camera.viewportWidth / 2f, halfH = camera.viewportHeight / 2f;
        float w = room.widthInTiles(), h = room.heightInTiles();
        float cx = w >= 2f * halfW ? MathUtils.clamp(player.feetX(), halfW, w - halfW) : w / 2f;
        float cy = h >= 2f * halfH ? MathUtils.clamp(player.feetY() + 0.5f, halfH, h - halfH) : h / 2f;
        camera.position.set(cx, cy, 0f);
        camera.update();
    }

    @Override public void resize(int width, int height) {
        viewport.update(width, height, overviewMode);
        updateCamera();
    }
    @Override public void dispose() {
        batch.dispose();
        combatRenderer.dispose();
        debugRenderer.dispose();
        tileDebugRenderer.dispose();
        hud.dispose();
        combatHud.dispose();
        sfxToggle.dispose();
        navigationRenderer.dispose();
        exteriorWalls.dispose();
        containerFx.dispose();
        skillBar.dispose();
        skillPanel.dispose();
        skillMenuButton.dispose();
    }
}

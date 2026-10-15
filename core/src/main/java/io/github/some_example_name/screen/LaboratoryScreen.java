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
import io.github.some_example_name.movement.MovementSystem;
import io.github.some_example_name.movement.RunStartDetector;
import io.github.some_example_name.render.AnimationController;
import io.github.some_example_name.render.CombatRenderer;
import io.github.some_example_name.render.DebugRenderer;
import io.github.some_example_name.render.EntityRenderer;
import io.github.some_example_name.render.MonsterRenderer;
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
import io.github.some_example_name.inventory.LootItem;
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
    private final GdxPlayerInput input = new GdxPlayerInput(KeyBindings.defaults());
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
    private final PlayerHudRenderer hud = new PlayerHudRenderer();
    private final NavigationRenderer navigationRenderer = new NavigationRenderer();
    private final RoomPathfinder pathfinder = new RoomPathfinder();
    private final PlayerSessionState session;
    private final io.github.some_example_name.render.ExteriorWallRenderer exteriorWalls;
    private final boolean exteriorRing;
    private boolean paused;
    private boolean debugEnabled = "1".equals(System.getenv("LAB_DEBUG_COLLISION"));
    private boolean tileDebugEnabled;
    private boolean backpackOpen;
    private boolean routeEnabled;
    // 개발용 자동 캡처: LAB_AUTO_CAPTURE 설정 시 몇 프레임 뒤 UI/충돌 프레임을 저장하고 종료한다.
    private final boolean autoCapture = System.getenv("LAB_AUTO_CAPTURE") != null;
    private int autoCaptureFrame;
    // LAB_WALL_TEST=3: V4 벽 47종 + 가로/세로 방문 4종을 격자로 직접 그려 한 번씩 확인.
    private final boolean v4AtlasView = "3".equals(System.getenv("LAB_WALL_TEST"));
    private List<RoomPathfinder.Cell> exitPath = java.util.Collections.emptyList();
    private final List<LootContainer> lootContainers = new ArrayList<>();
    private final List<LootDrop> lootDrops = new ArrayList<>();
    private final Random lootRandom = new Random();
    private final io.github.some_example_name.render.ContainerInteractionRenderer containerFx =
        new io.github.some_example_name.render.ContainerInteractionRenderer();
    private LootContainer promptTarget;        // 이번 프레임 상호작용 대상(안내 표시용)
    private float containerGlowHeight = 1f;    // 마지막 유효 입력 대상의 표시 높이(발광 반경 기준)

    public LaboratoryScreen(LaboratoryGame game, GameAssets assets, LaboratoryLayout layout,
                            PlayerSessionState session, List<Monster> monsters, BalanceConfig config) {
        super(game, assets);
        this.config = config;
        this.session = session;
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
        if (active && Gdx.input.isKeyJustPressed(Input.Keys.R)) backpackOpen = !backpackOpen;
        if (active && inputState.isPressed(GameAction.PAUSE)) {
            if (backpackOpen) backpackOpen = false;   // 가방이 열려 있으면 Esc는 가방부터 닫는다
            else paused = !paused;
        }
        if (active && Gdx.input.isKeyJustPressed(Input.Keys.F1)) debugEnabled = !debugEnabled;
        if (active && Gdx.input.isKeyJustPressed(Input.Keys.F2)) tileDebugEnabled = !tileDebugEnabled;
        // F12: 현재 프레임을 PNG로 저장(실제 렌더 검증). F1 켜면 충돌 디버그 포함 프레임이 저장된다.
        boolean captureRequested = active && Gdx.input.isKeyJustPressed(Input.Keys.F12);
        if (active && Gdx.input.isKeyJustPressed(Input.Keys.F)) routeEnabled = !routeEnabled;
        // 미니맵 아래 귀환 버튼 클릭도 F와 동일하게 대문 발자국 경로를 토글한다.
        if (active && !backpackOpen && !paused && Gdx.input.justTouched()
            && navigationRenderer.buttonHit(Gdx.input.getX(), Gdx.input.getY()))
            routeEnabled = !routeEnabled;
        // [임시] 월드 아이템 획득 시스템이 아직 없어, G로 테스트 물품을 회수해 8칸/가득참을 확인한다.
        if (active && !backpackOpen && Gdx.input.isKeyJustPressed(Input.Keys.G)
            && !session.collect("debug_sample")) hud.flashBackpackFull();

        boolean runStarted = false;
        float animDelta = paused || transitionLocked || backpackOpen ? 0f : dt;
        if (!paused && !transitionLocked && !backpackOpen && !player.isDead()) {
            PlayerIntent intent = controller.intentFrom(player, inputState);
            boolean spaceInteract = Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
                && hasNearbyInteractable();
            if (spaceInteract && intent.attack)
                intent = new PlayerIntent(intent.moveX, intent.moveY, intent.run, false, intent.interact);
            if (intent.attack) playerCombat.attack().start(player.facing());
            playerGrid.forPlayer(monsters);
            float previousFeetY = player.feetY();
            runStarted = movementSystem.update(player, intent, playerGrid, dt);
            updateLoot(animDelta, intent.interact || spaceInteract,
                Gdx.input.isKeyJustPressed(Input.Keys.D));
            if (room.crossedSouthExit(previousFeetY, player.feetY(), player.feetX(), intent.moveY))
                game.transition().request(io.github.some_example_name.world.MapId.HEADQUARTERS);
            playerCombat.update(dt);
            monsterAi.update(monsters, player, playerCombat, room, assets.monsterAnimations(), runStarted, dt);
            if (player.isDead()) {
                movementSystem.stop(player);
                playerCombat.attack().cancel();
            } else {
                combatSystem.updatePlayerAttack(player, playerCombat, assets.playerAnimations(), monsters, room, dt);
            }
        } else {
            player.setMoving(false);
        }

        // 상호작용 안내 대상: 플레이 가능한 상태에서만 선택(가방/일시정지/사망/전환 중엔 없음).
        promptTarget = (!paused && !transitionLocked && !backpackOpen && !player.isDead())
            ? selectContainerTarget() : null;
        containerFx.update(animDelta);

        updateCamera();
        playerGrid.forPlayer(monsters);
        if (routeEnabled && !player.isDead())
            exitPath = pathfinder.shortestPath(playerGrid, (int)player.feetX(), (int)player.feetY(),
                room.southExitCenterX(), room.southExitPathY(),
                player.bounds().width, player.bounds().height);
        else exitPath = java.util.Collections.emptyList();
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
        for (LootDrop drop : lootDrops) if(!drop.collected())
            batch.draw(assets.loot().itemBase(drop.item()),drop.x()-.28f,drop.y()+.18f,.56f,.56f);
        monsterRenderer.render(batch, monsters);
        entityRenderer.render(batch, player, animDelta, playerCombat.attack(),
            config.playerAttackCooldownSeconds);
        worldRenderer.renderForegroundProps(batch, visuals, camera, player.feetY());
        // SPACE 안내는 가구/플레이어 위에 그려 항상 읽히게 한다(열린 컨테이너는 표시 안 함).
        if (promptTarget != null)
            containerFx.renderPrompt(batch, promptTarget, containerDrawSize(promptTarget.id()),
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
        if (!overviewMode)
            hud.render(assets.hud(), assets.loot(), assets.fadePixel(), player,
                session.backpack(), backpackOpen, dt);
        renderFade(batch, viewport);
        // Some blended debug/font pixels can lower the default framebuffer's
        // alpha. Restore opacity without touching the rendered RGB image so
        // desktop windows never show through the game surface.
        Gdx.gl.glColorMask(false, false, false, true);
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glColorMask(true, true, true, true);
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
                        if (opened) assets.containerSounds().playUnlock();
                        else assets.containerSounds().playClick();
                        Gdx.app.log("AutoPress", target.id() + " " + result
                            + " " + target.presses() + "/" + target.requiredPresses());
                    }
                }
            }
            int uiFrame = autoPress ? 38 : 30;      // 연타 직후(펄스 진행 중) 프레임을 저장
            int debugOn = autoPress ? 52 : 32;
            int debugFrame = autoPress ? 62 : 42;
            int exitFrame = autoPress ? 100 : 46;   // 개봉 애니메이션/드롭까지 관찰
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
            if(reward!=null) lootDrops.add(new LootDrop(reward,chest.x(),chest.y()));
        }
        if (!interactPressed && !dropPressed) return;
        float px=player.feetX(), py=player.feetY();
        // 대문 근처에서 SPACE: 닫힘↔열림 토글(열려야 남쪽 통로로 본부 복귀 가능).
        if (interactPressed && nearGate(px, py)) {
            room.setGateClosed(!room.gateClosed());
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
                if (opened) assets.containerSounds().playUnlock();
                else assets.containerSounds().playClick();
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
                if (door != null) door.toggle();
            }
            return;
        }
        if(!session.backpack().isFull()&&interactPressed) {
            if (session.backpack().collect(nearest.item().id)) {
                nearest.collect();
                hud.flashLoot(nearest.item());
            }
        } else if(session.backpack().isFull()&&dropPressed) {
            String displaced=session.backpack().replaceLowestValue(nearest.item().id,assets.loot().prices());
            if (displaced != null) {
                LootItem old=assets.loot().catalog().find(displaced);
                if(old!=null)lootDrops.add(new LootDrop(old,px,py));
                nearest.collect();
                hud.flashLoot(nearest.item());
            }
        }
        lootDrops.removeIf(LootDrop::collected);

    }

    private boolean hasNearbyInteractable() {
        float px = player.feetX(), py = player.feetY(), max = 1.45f * 1.45f;
        for (LootContainer chest : lootContainers) {
            float dx = px - chest.x(), dy = py - chest.y();
            if (dx * dx + dy * dy <= max) return true;
        }
        for (LootDrop drop : lootDrops) if (!drop.collected()) {
            float dx = px - drop.x(), dy = py - drop.y();
            if (dx * dx + dy * dy <= max) return true;
        }
        if (nearGate(px, py)) return true;
        return room.nearestDoor(px, py, 1.45f) != null;
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
        navigationRenderer.dispose();
        exteriorWalls.dispose();
        containerFx.dispose();
    }
}

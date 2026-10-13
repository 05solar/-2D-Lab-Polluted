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
    private List<RoomPathfinder.Cell> exitPath = java.util.Collections.emptyList();
    private final List<LootContainer> lootContainers = new ArrayList<>();
    private final List<LootDrop> lootDrops = new ArrayList<>();
    private final Random lootRandom = new Random();

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
            lootContainers.add(assets.loot().newContainer(placement.id(), placement.x(), placement.y()));
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
        if (exteriorRing) worldRenderer.setInteriorWalls(
            new io.github.some_example_name.render.InteriorWallRenderer(assets.exteriorWalls()), room);
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
        for (LootContainer chest : lootContainers)
            batch.draw(assets.loot().chest(chest.id(),chest.frame()),chest.x()-.5f,chest.y(),1f,1f);
        for (LootDrop drop : lootDrops) if(!drop.collected())
            batch.draw(assets.loot().itemBase(drop.item()),drop.x()-.28f,drop.y()+.18f,.56f,.56f);
        monsterRenderer.render(batch, monsters);
        entityRenderer.render(batch, player, animDelta, playerCombat.attack(),
            config.playerAttackCooldownSeconds);
        worldRenderer.renderForegroundProps(batch, visuals, camera, player.feetY());
        batch.end();
        if (exteriorRing)
            exteriorWalls.render(camera, room.widthInTiles(), room.heightInTiles(),
                room.southExitCenterX() + 0.5f, !room.gateClosed());
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
        LootContainer nearestChest=null; float nearestChestD=1.45f*1.45f;
        for(LootContainer chest:lootContainers) {
            float dx=px-chest.x(),dy=py-chest.y(); float d=dx*dx+dy*dy;
            if(d<nearestChestD){nearestChestD=d;nearestChest=chest;}
        }
        if(interactPressed&&nearestChest!=null&&!nearestChest.opened()) {
            nearestChest.open(assets.loot().catalog(),lootRandom); return;
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
    }
}

package io.github.some_example_name.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
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
import io.github.some_example_name.collision.CollisionSystem;
import io.github.some_example_name.world.LaboratoryLayout;
import io.github.some_example_name.world.LaboratoryRoom;
import io.github.some_example_name.world.PlayerSessionState;
import io.github.some_example_name.world.RoomVisuals;
import java.util.EnumSet;
import java.util.List;

/** Laboratory frame coordinator. Combat rules stay in world/combat systems. */
public class LaboratoryScreen extends BaseScreen {
    private static final float VIEW_WIDTH = 12f, VIEW_HEIGHT = 9f;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final FitViewport viewport = new FitViewport(VIEW_WIDTH, VIEW_HEIGHT, camera);
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
    private final PlayerSessionState session;
    private boolean paused;
    private boolean debugEnabled;
    private boolean tileDebugEnabled;
    private boolean backpackOpen;

    public LaboratoryScreen(LaboratoryGame game, GameAssets assets, LaboratoryLayout layout,
                            PlayerSessionState session, List<Monster> monsters, BalanceConfig config) {
        super(game, assets);
        this.config = config;
        this.session = session;
        room = layout.room();
        visuals = layout.visuals();
        player = session.player();
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
        worldRenderer = new WorldRenderer(assets.tileSet());
        entityRenderer = new EntityRenderer(new AnimationController(assets.playerAnimations()));
        monsterRenderer = new MonsterRenderer(assets.monsterAnimations());
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
        // [임시] 월드 아이템 획득 시스템이 아직 없어, G로 테스트 물품을 회수해 8칸/가득참을 확인한다.
        if (active && !backpackOpen && Gdx.input.isKeyJustPressed(Input.Keys.G)
            && !session.collect("debug_sample")) hud.flashBackpackFull();

        boolean runStarted = false;
        float animDelta = paused || transitionLocked || backpackOpen ? 0f : dt;
        if (!paused && !transitionLocked && !backpackOpen && !player.isDead()) {
            PlayerIntent intent = controller.intentFrom(player, inputState);
            if (intent.attack) playerCombat.attack().start(player.facing());
            playerGrid.forPlayer(monsters);
            runStarted = movementSystem.update(player, intent, playerGrid, dt);
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
        ScreenUtils.clear(0.05f, 0.05f, 0.06f, 1f);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        worldRenderer.render(batch, visuals);
        monsterRenderer.render(batch, monsters);
        entityRenderer.render(batch, player, animDelta, playerCombat.attack(),
            config.playerAttackCooldownSeconds);
        batch.end();
        combatRenderer.renderWorld(camera, monsters, player, playerCombat.attack(), config);
        hud.render(assets.hud(), assets.fadePixel(), player, session.backpack(), backpackOpen, dt);
        if (tileDebugEnabled) tileDebugRenderer.render(camera, room, visuals, batch);
        if (debugEnabled) {
            debugRenderer.render(camera, room, player);
            combatRenderer.renderDebug(camera, player, monsters, playerCombat.attack(), config, room);
        }
        renderFade(batch, viewport);
    }

    private void updateCamera() {
        float halfW = camera.viewportWidth / 2f, halfH = camera.viewportHeight / 2f;
        float w = room.widthInTiles(), h = room.heightInTiles();
        float cx = w >= 2f * halfW ? MathUtils.clamp(player.feetX(), halfW, w - halfW) : w / 2f;
        float cy = h >= 2f * halfH ? MathUtils.clamp(player.feetY() + 0.5f, halfH, h - halfH) : h / 2f;
        camera.position.set(cx, cy, 0f);
        camera.update();
    }

    @Override public void resize(int width, int height) { viewport.update(width, height); }
    @Override public void dispose() {
        batch.dispose();
        combatRenderer.dispose();
        debugRenderer.dispose();
        tileDebugRenderer.dispose();
        hud.dispose();
    }
}

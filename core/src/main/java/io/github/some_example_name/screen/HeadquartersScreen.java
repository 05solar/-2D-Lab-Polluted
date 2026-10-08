package io.github.some_example_name.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.github.some_example_name.LaboratoryGame;
import io.github.some_example_name.asset.GameAssets;
import io.github.some_example_name.collision.CollisionSystem;
import io.github.some_example_name.config.BalanceConfig;
import io.github.some_example_name.entity.player.Player;
import io.github.some_example_name.input.GameAction;
import io.github.some_example_name.input.GdxPlayerInput;
import io.github.some_example_name.input.InputState;
import io.github.some_example_name.input.KeyBindings;
import io.github.some_example_name.input.PlayerController;
import io.github.some_example_name.input.PlayerIntent;
import io.github.some_example_name.interaction.VendingMachineInteraction;
import io.github.some_example_name.movement.MovementSystem;
import io.github.some_example_name.movement.RunStartDetector;
import io.github.some_example_name.render.AnimationController;
import io.github.some_example_name.render.EntityRenderer;
import io.github.some_example_name.render.HeadquartersDebugRenderer;
import io.github.some_example_name.render.HeadquartersRenderer;
import io.github.some_example_name.render.PlayerHudRenderer;
import io.github.some_example_name.world.HeadquartersLayout;
import io.github.some_example_name.world.HeadquartersRoom;
import io.github.some_example_name.world.HeadquartersVisuals;
import io.github.some_example_name.world.MapId;
import io.github.some_example_name.world.PlayerSessionState;
import java.util.EnumSet;

/** Connects HQ input, movement, depth-sorted art and one-shot south exit. */
public final class HeadquartersScreen extends BaseScreen {
    private static final float VIEW_WIDTH = HeadquartersLayout.WIDTH + 0.4f;
    private static final float VIEW_HEIGHT = HeadquartersLayout.HEIGHT + 0.4f;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final FitViewport viewport = new FitViewport(VIEW_WIDTH, VIEW_HEIGHT, camera);
    private final SpriteBatch batch = new SpriteBatch();
    private final BalanceConfig config = new BalanceConfig();
    private final HeadquartersRoom room;
    private final HeadquartersVisuals visuals;
    private final Player player;
    private final InputState inputState = new InputState();
    private final GdxPlayerInput input = new GdxPlayerInput(KeyBindings.defaults());
    private final PlayerController controller = new PlayerController();
    private final MovementSystem movement;
    private final EntityRenderer playerRenderer;
    private final HeadquartersRenderer worldRenderer;
    private final HeadquartersDebugRenderer debugRenderer = new HeadquartersDebugRenderer();
    private final VendingMachineInteraction vendingInteraction = new VendingMachineInteraction();
    private final VendingMachineInteraction waterInteraction = new VendingMachineInteraction();
    private final PlayerHudRenderer hud = new PlayerHudRenderer();
    private final PlayerSessionState session;
    private boolean paused;
    private boolean debug;
    private boolean backpackOpen;

    public HeadquartersScreen(LaboratoryGame game, GameAssets assets,
                              HeadquartersLayout layout, PlayerSessionState session) {
        super(game, assets);
        this.session = session;
        room = layout.room();
        visuals = layout.visuals();
        player = session.player();
        movement = new MovementSystem(new CollisionSystem(), new RunStartDetector(),
            config.playerWalkSpeed, config.playerRunSpeed);
        playerRenderer = new EntityRenderer(new AnimationController(assets.playerAnimations()));
        worldRenderer = new HeadquartersRenderer(assets.headquarters(), visuals);
        updateCamera();
    }

    @Override public void render(float delta) {
        float dt = Math.min(delta, config.maxFrameDeltaSeconds);
        boolean transitionLocked = game.transition().inputLocked();
        if (transitionLocked) {
            inputState.update(EnumSet.noneOf(GameAction.class));
            player.setMoving(false);
        } else {
            input.poll(inputState);
            if (Gdx.input.isKeyJustPressed(Input.Keys.R)) backpackOpen = !backpackOpen;
            if (inputState.isPressed(GameAction.PAUSE)) {
                if (backpackOpen) backpackOpen = false;   // 가방이 열려 있으면 Esc는 가방부터 닫는다
                else paused = !paused;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) debug = !debug;
        }
        if (!paused && !transitionLocked && !backpackOpen) {
            float oldFeetY = player.feetY();
            PlayerIntent intent = controller.intentFrom(player, inputState);
            movement.update(player, intent, room, dt);
            if (!player.isDead() && room.crossedSouthExit(oldFeetY, player, intent.moveY))
                game.transition().request(MapId.LABORATORY);
        }
        boolean interactionContext = !player.isDead() && !paused && !transitionLocked && !backpackOpen;
        boolean vendingAvailable = interactionContext && room.canInteractWithVending(player);
        boolean waterAvailable = interactionContext && room.canInteractWithWaterDispenser(player);
        boolean vendingPromptVisible = interactionContext && room.isNearVending(player);
        boolean waterPromptVisible = interactionContext && room.isNearWaterDispenser(player);
        if (vendingPromptVisible && waterPromptVisible) {
            com.badlogic.gdx.math.Vector2 vendingPoint = room.interactionPoint("vending_machine");
            com.badlogic.gdx.math.Vector2 waterPoint = room.interactionPoint("water_dispenser");
            float vendingDistance = vendingPoint.dst2(player.feetX(), player.feetY());
            float waterDistance = waterPoint.dst2(player.feetX(), player.feetY());
            if (vendingDistance <= waterDistance) waterPromptVisible = false;
            else vendingPromptVisible = false;
        }
        vendingAvailable &= vendingPromptVisible;
        waterAvailable &= waterPromptVisible;
        boolean promptIsWater = waterPromptVisible;
        float animDelta = paused || transitionLocked || backpackOpen ? 0f : dt;
        boolean spacePressed = inputState.isPressed(GameAction.ATTACK);
        boolean vendingFired = vendingInteraction.update(vendingPromptVisible,
            vendingAvailable && spacePressed, animDelta);
        boolean waterFired = waterInteraction.update(waterPromptVisible,
            waterAvailable && spacePressed, animDelta);
        if (vendingFired)
            Gdx.app.log("Headquarters", "VendingMachineInteraction event (shop UI not implemented)");
        if (waterFired) {
            player.healToFullHealth();
            Gdx.app.log("Headquarters", "WaterDispenserInteraction event (player healed to full HP)");
        }
        ScreenUtils.clear(0.025f, 0.028f, 0.025f, 1f);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        worldRenderer.render(batch, visuals, player, playerRenderer,
            animDelta, vendingAvailable, promptIsWater,
            promptIsWater ? waterInteraction.promptFrame(waterPromptVisible)
                : vendingInteraction.promptFrame(vendingPromptVisible),
            vendingInteraction.showAcceptedFrame());
        batch.end();
        hud.render(assets.hud(), assets.fadePixel(), player, session.backpack(), backpackOpen, dt);
        renderFade(batch, viewport);
        if (debug) debugRenderer.render(camera, room, player);
    }

    private void updateCamera() {
        camera.position.set(room.widthInTiles() / 2f, room.heightInTiles() / 2f, 0f);
        camera.update();
    }

    @Override public void resize(int width, int height) { viewport.update(width, height); }

    @Override public void dispose() {
        batch.dispose();
        debugRenderer.dispose();
        hud.dispose();
    }
}

package io.github.some_example_name.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
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
import io.github.some_example_name.render.DifficultyModalRenderer;
import io.github.some_example_name.render.HeadquartersDebugRenderer;
import io.github.some_example_name.render.HeadquartersRenderer;
import io.github.some_example_name.render.PlayerHudRenderer;
import io.github.some_example_name.world.HeadquartersLayout;
import io.github.some_example_name.world.HeadquartersRoom;
import io.github.some_example_name.world.HeadquartersVisuals;
import io.github.some_example_name.world.MapId;
import io.github.some_example_name.world.GameDifficulty;
import io.github.some_example_name.asset.DifficultyModalLayout;
import io.github.some_example_name.world.PlayerSessionState;
import java.util.EnumSet;

/** Connects HQ input, movement, depth-sorted art and one-shot south exit. */
public final class HeadquartersScreen extends BaseScreen {
    private static final float VIEW_WIDTH = HeadquartersLayout.WIDTH + 0.4f;
    private static final float VIEW_HEIGHT = HeadquartersLayout.HEIGHT + 0.4f;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final FitViewport viewport = new FitViewport(VIEW_WIDTH, VIEW_HEIGHT, camera);
    private final SpriteBatch batch = new SpriteBatch();
    private final ScreenViewport uiViewport = new ScreenViewport();
    private final DifficultyModalRenderer difficultyModalRenderer = new DifficultyModalRenderer();
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
    private final PlayerHudRenderer hud;
    private final io.github.some_example_name.render.SfxToggleRenderer sfxToggle =
        new io.github.some_example_name.render.SfxToggleRenderer();
    private final io.github.some_example_name.render.SalePanelRenderer salePanel;
    private final PlayerSessionState session;
    private boolean paused;
    private boolean debug;
    // LAB_TEST_SALE=1(개발 전용): 보관함에 표본을 넣고 판매창을 열어 실프레임 캡처·판매·종료.
    private final boolean testSale = "1".equals(System.getenv("LAB_TEST_SALE"));
    private int testSaleFrame;
    private boolean backpackOpen;
    private boolean difficultySelection;
    private int selectedDifficultyIndex;

    public HeadquartersScreen(LaboratoryGame game, GameAssets assets,
                              HeadquartersLayout layout, PlayerSessionState session) {
        super(game, assets);
        this.session = session;
        hud = new PlayerHudRenderer(assets.uiFont());
        salePanel = new io.github.some_example_name.render.SalePanelRenderer(
            assets.hqSales(), assets.loot(), assets.uiFont());
        room = layout.room();
        visuals = layout.visuals();
        player = session.player();
        movement = new MovementSystem(new CollisionSystem(), new RunStartDetector(),
            config.playerWalkSpeed, config.playerRunSpeed);
        playerRenderer = new EntityRenderer(new AnimationController(assets.playerAnimations()));
        worldRenderer = new HeadquartersRenderer(assets.headquarters(), visuals,
            assets.hqSales().saleSign());
        updateCamera();
        uiViewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
    }

    @Override public void render(float delta) {
        float dt = Math.min(delta, config.maxFrameDeltaSeconds);
        boolean transitionLocked = game.transition().inputLocked();
        if (transitionLocked) {
            inputState.update(EnumSet.noneOf(GameAction.class));
            player.setMoving(false);
        } else if (difficultySelection) {
            inputState.update(EnumSet.noneOf(GameAction.class));
            updateDifficultySelectionInput();
        } else if (salePanel.isOpen()) {
            // 판매창이 열린 동안 이동·상호작용·월드 입력을 모두 차단하고 클릭을 소비한다.
            // Esc는 확인창 → 판매창 순으로 닫기만 하고 일시정지로 이어지지 않는다.
            inputState.update(EnumSet.noneOf(GameAction.class));
            player.setMoving(false);
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) salePanel.onEscape();
            else salePanel.handleInput();
        } else {
            input.poll(inputState);
            if (Gdx.input.isKeyJustPressed(Input.Keys.R)) backpackOpen = !backpackOpen;
            if (inputState.isPressed(GameAction.PAUSE)) {
                if (backpackOpen) backpackOpen = false;   // 가방이 열려 있으면 Esc는 가방부터 닫는다
                else paused = !paused;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) debug = !debug;
            // 오디오 토글 버튼(HP 바 오른쪽) — 효과음+배경음악 세트 on/off, 연구소와 설정 공유.
            if (Gdx.input.justTouched()
                && sfxToggle.buttonHit(Gdx.input.getX(), Gdx.input.getY()))
                assets.toggleAudio();
        }
        if (!paused && !transitionLocked && !backpackOpen && !difficultySelection
            && !salePanel.isOpen()) {
            float oldFeetY = player.feetY();
            PlayerIntent intent = controller.intentFrom(player, inputState);
            movement.update(player, intent, room, dt);
            if (!player.isDead() && room.crossedSouthExit(oldFeetY, player, intent.moveY))
                difficultySelection = true;
        }
        boolean interactionContext = !player.isDead() && !paused && !transitionLocked
            && !backpackOpen && !difficultySelection && !salePanel.isOpen();
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
        // SPACE는 상호작용 전용 키(공격은 마우스 클릭 전용 — 2026-10-16 키 정리).
        boolean spacePressed = inputState.isPressed(GameAction.INTERACT);
        boolean vendingFired = vendingInteraction.update(vendingPromptVisible,
            vendingAvailable && spacePressed, animDelta);
        boolean waterFired = waterInteraction.update(waterPromptVisible,
            waterAvailable && spacePressed, animDelta);
        if (vendingFired) {
            // 자판기 SPACE → 판매창 열기. 열기 전에 미완료 귀환 이관이 있으면 재시도한다.
            if (game.transferPending()) game.attemptReturnTransfer();
            salePanel.open(game.economy());
        }
        if (waterFired) {
            player.healToFullHealth();
            Gdx.app.log("Headquarters", "WaterDispenserInteraction event (player healed to full HP)");
        }
        ScreenUtils.clear(0.025f, 0.028f, 0.025f, 1f);
        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        worldRenderer.render(batch, visuals, player, playerRenderer,
            animDelta, vendingAvailable, promptIsWater,
            promptIsWater ? waterInteraction.promptFrame(waterPromptVisible)
                : vendingInteraction.promptFrame(vendingPromptVisible),
            vendingInteraction.showAcceptedFrame());
        batch.end();
        if (difficultySelection)
            difficultyModalRenderer.render(batch, uiViewport, assets.difficultyModal(), selectedDifficultyIndex);
        uiViewport.apply();
        hud.render(assets.hud(), assets.loot(), assets.fadePixel(), player,
            session.backpack(), backpackOpen, dt);
        // 효과음 토글 버튼: 좌상단 HP 바 바로 오른쪽(연구소와 동일 위치, 64px).
        io.github.some_example_name.asset.HudLayout hudLayout = assets.hud().layout();
        sfxToggle.render(assets.sfxToggle(assets.combatSounds().sfxEnabled()),
            hudLayout.marginX + hudLayout.frameW + 14f,
            hudLayout.marginY + (hudLayout.frameH - 64f) / 2f, 64f);
        salePanel.render();
        renderFade(batch, viewport);
        if (debug) debugRenderer.render(camera, room, player);
        if (testSale) runSaleTestScript();
    }

    /** 개발 전용 실행 검증: 표본 이관 → 판매창 → 전체 선택 → 선택 판매 → 캡처 → 종료. */
    private void runSaleTestScript() {
        testSaleFrame++;
        io.github.some_example_name.profile.HeadquartersEconomy economy = game.economy();
        if (testSaleFrame == 20) {
            economy.transferReturn("test-ret-" + System.nanoTime(), java.util.Arrays.asList(
                "tumbler", "tumbler", "lantern", "research_log_booklet", "broken_microscope"));
            salePanel.open(economy);
            Gdx.app.log("SaleTest", "opened money=" + economy.currencyWon()
                + " storage=" + economy.storageView().size());
        }
        if (testSaleFrame == 40)
            io.github.some_example_name.render.FrameCapture.capture("hq_sale_panel");
        if (testSaleFrame == 50) salePanel.selection().toggleAll();
        if (testSaleFrame == 60)
            io.github.some_example_name.render.FrameCapture.capture("hq_sale_selected");
        if (testSaleFrame == 70) salePanel.sellSelected();
        if (testSaleFrame == 85)
            io.github.some_example_name.render.FrameCapture.capture("hq_sale_sold");
        if (testSaleFrame == 90) {
            Gdx.app.log("SaleTest", "after sale money=" + economy.currencyWon()
                + " storage=" + economy.storageView().size());
            Gdx.app.exit();
        }
    }

    private void updateDifficultySelectionInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            difficultySelection = false;
            room.resetExitTrigger();
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP))
            selectedDifficultyIndex = (selectedDifficultyIndex + 2) % 3;
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN))
            selectedDifficultyIndex = (selectedDifficultyIndex + 1) % 3;
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) selectedDifficultyIndex = 0;
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) selectedDifficultyIndex = 1;
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) selectedDifficultyIndex = 2;

        float width = uiViewport.getWorldWidth(), height = uiViewport.getWorldHeight();
        float mouseX = Gdx.input.getX();
        float mouseY = height - Gdx.input.getY();
        for (int i = 0; i < 3; i++) {
            DifficultyModalLayout.Rect row = assets.difficultyModal().layout()
                .optionRect(i, width, height);
            if (row.contains(mouseX, mouseY)) {
                selectedDifficultyIndex = i;
                if (Gdx.input.justTouched()) {
                    confirmDifficultySelection();
                    return;
                }
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
            || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_ENTER))
            confirmDifficultySelection();
        // Clicks outside the three visible option rows are intentionally ignored.
    }

    private void confirmDifficultySelection() {
        GameDifficulty selected = GameDifficulty.valueOf(
            assets.difficultyModal().layout().optionId(selectedDifficultyIndex));
        difficultySelection = false;
        game.selectDifficulty(selected);
        game.transition().request(MapId.LABORATORY);
    }

    private void updateCamera() {
        camera.position.set(room.widthInTiles() / 2f, room.heightInTiles() / 2f, 0f);
        camera.update();
    }

    @Override public void resize(int width, int height) {
        viewport.update(width, height);
        uiViewport.update(width, height, true);
        updateCamera();
    }

    @Override public void dispose() {
        batch.dispose();
        difficultyModalRenderer.dispose();
        debugRenderer.dispose();
        hud.dispose();
        sfxToggle.dispose();
        salePanel.dispose();
    }
}

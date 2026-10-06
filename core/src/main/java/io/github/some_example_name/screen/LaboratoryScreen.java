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
import io.github.some_example_name.collision.CollisionSystem;
import io.github.some_example_name.config.BalanceConfig;
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
import io.github.some_example_name.render.DebugRenderer;
import io.github.some_example_name.render.EntityRenderer;
import io.github.some_example_name.render.WorldRenderer;
import io.github.some_example_name.world.LaboratoryLayout;
import io.github.some_example_name.world.LaboratoryRoom;
import io.github.some_example_name.world.RoomVisuals;
import io.github.some_example_name.world.TileVisual;

/**
 * 연구소 탐사 화면. 월드/시스템/렌더러를 묶어 프레임 순서를 조율한다.
 * 게임 규칙(이동·충돌·소음)은 각 시스템에 있고, 여기서는 호출 순서만 담당한다.
 *
 * 프레임 순서(규칙서 7장, 1단계 범위):
 * 입력 수집 → 플레이어 명령 → 이동/충돌 → 카메라 → 월드 렌더 → (디버그)
 */
public class LaboratoryScreen extends BaseScreen {

    private static final float VIEW_WIDTH = 12f;  // 월드 단위(타일)
    private static final float VIEW_HEIGHT = 9f;

    private final OrthographicCamera camera = new OrthographicCamera();
    private final FitViewport viewport = new FitViewport(VIEW_WIDTH, VIEW_HEIGHT, camera);
    private final SpriteBatch batch = new SpriteBatch();

    private final BalanceConfig config = new BalanceConfig();
    private final LaboratoryLayout layout = LaboratoryLayout.testRoom();
    private final LaboratoryRoom room = layout.room();
    private final RoomVisuals visuals = layout.visuals();
    private final Player player;

    private final InputState inputState = new InputState();
    private final GdxPlayerInput input = new GdxPlayerInput(KeyBindings.defaults());
    private final PlayerController controller = new PlayerController();
    private final MovementSystem movementSystem;

    private final WorldRenderer worldRenderer;
    private final EntityRenderer entityRenderer;
    private final DebugRenderer debugRenderer = new DebugRenderer();

    private boolean paused = false;
    private boolean debugEnabled = false;

    public LaboratoryScreen(LaboratoryGame game, GameAssets assets) {
        super(game, assets);

        player = new Player(
            room.spawnPoint().x, room.spawnPoint().y,
            config.playerBoundsWidth, config.playerBoundsHeight, config.playerMaxHp);

        movementSystem = new MovementSystem(
            new CollisionSystem(), new RunStartDetector(),
            config.playerWalkSpeed, config.playerRunSpeed);

        worldRenderer = new WorldRenderer(assets.tileRegions());
        entityRenderer = new EntityRenderer(new AnimationController(assets.playerAnimations()));

        logVisualCounts();
        updateCamera();
    }

    /** 방에 실제로 들어 있는 각 시각 타일 개수를 한 번만 출력한다(렌더/매핑 진단용). */
    private void logVisualCounts() {
        Object[][] rows = {
            {"BASIC_FLOOR", TileVisual.FLOOR_BASIC},
            {"CRACKED_FLOOR", TileVisual.FLOOR_CRACKED},
            {"STAINED_FLOOR", TileVisual.FLOOR_STAIN},
            {"WARNING_FLOOR", TileVisual.FLOOR_WARNING},
            {"NORTH_WALL", TileVisual.WALL_NORTH},
            {"SOUTH_WALL", TileVisual.WALL_SOUTH},
            {"WEST_WALL", TileVisual.WALL_WEST},
            {"EAST_WALL", TileVisual.WALL_EAST},
            {"NORTH_WEST_CORNER", TileVisual.WALL_CORNER_NW},
            {"NORTH_EAST_CORNER", TileVisual.WALL_CORNER_NE},
            {"SOUTH_WEST_CORNER", TileVisual.WALL_CORNER_SW},
            {"SOUTH_EAST_CORNER", TileVisual.WALL_CORNER_SE},
            {"CLOSED_DOOR", TileVisual.DOOR_CLOSED},
            {"OPEN_DOORWAY", TileVisual.DOORWAY_OPEN},
            {"TOXIC_SPILL", TileVisual.FLOOR_TOXIC},
            {"EXPOSED_WIRES", TileVisual.FLOOR_WIRES},
        };
        for (Object[] row : rows) {
            Gdx.app.log("TileVisualCount", row[0] + "=" + visuals.count((TileVisual) row[1]));
        }
    }

    @Override
    public void render(float delta) {
        float dt = Math.min(delta, config.maxFrameDeltaSeconds);

        input.poll(inputState);
        if (inputState.isPressed(GameAction.PAUSE)) {
            paused = !paused;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) {
            debugEnabled = !debugEnabled;
        }

        float animDelta = paused ? 0f : dt;
        if (!paused) {
            PlayerIntent intent = controller.intentFrom(inputState);
            movementSystem.update(player, intent, room, dt);
        }

        updateCamera();

        ScreenUtils.clear(0.05f, 0.05f, 0.06f, 1f);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        worldRenderer.render(batch, visuals);
        entityRenderer.render(batch, player, animDelta);
        batch.end();

        if (debugEnabled) {
            debugRenderer.render(camera, room, player);
        }
    }

    private void updateCamera() {
        float halfW = camera.viewportWidth / 2f;
        float halfH = camera.viewportHeight / 2f;
        float roomW = room.widthInTiles();
        float roomH = room.heightInTiles();

        float cx = roomW >= 2f * halfW ? MathUtils.clamp(player.feetX(), halfW, roomW - halfW) : roomW / 2f;
        float cy = roomH >= 2f * halfH ? MathUtils.clamp(player.feetY() + 0.5f, halfH, roomH - halfH) : roomH / 2f;

        camera.position.set(cx, cy, 0f);
        camera.update();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    @Override
    public void dispose() {
        batch.dispose();
        debugRenderer.dispose();
        // 텍스처는 GameAssets 소유이므로 여기서 해제하지 않는다.
    }
}

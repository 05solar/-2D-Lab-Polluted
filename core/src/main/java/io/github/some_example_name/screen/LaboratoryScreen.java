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
import io.github.some_example_name.render.TileDebugRenderer;
import io.github.some_example_name.render.WorldRenderer;
import io.github.some_example_name.world.LaboratoryLayout;
import io.github.some_example_name.world.LaboratoryRoom;
import io.github.some_example_name.world.RoomVisuals;

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
    private final TileDebugRenderer tileDebugRenderer = new TileDebugRenderer();

    private boolean paused = false;
    private boolean debugEnabled = false;      // F1: 충돌 박스
    private boolean tileDebugEnabled = false;   // F2: 타일 레이어/타입

    public LaboratoryScreen(LaboratoryGame game, GameAssets assets) {
        super(game, assets);

        player = new Player(
            room.spawnPoint().x, room.spawnPoint().y,
            config.playerBoundsWidth, config.playerBoundsHeight, config.playerMaxHp);

        movementSystem = new MovementSystem(
            new CollisionSystem(), new RunStartDetector(),
            config.playerWalkSpeed, config.playerRunSpeed);

        worldRenderer = new WorldRenderer(assets.tileSet());
        entityRenderer = new EntityRenderer(new AnimationController(assets.playerAnimations()));

        dumpResolvedGridIfRequested();
        updateCamera();
    }

    /**
     * 환경변수 LAB_DUMP=1 일 때만 해석된 4레이어 시각 타일 ID를 1회 덤프한다(렌더 결과 검증용).
     * 평상시 실행 로그는 깨끗하게 유지한다.
     */
    private void dumpResolvedGridIfRequested() {
        if (System.getenv("LAB_DUMP") == null) {
            return;
        }
        for (int ty = visuals.heightInTiles() - 1; ty >= 0; ty--) {
            StringBuilder sb = new StringBuilder();
            for (int tx = 0; tx < visuals.widthInTiles(); tx++) {
                sb.append(tx).append(',').append(ty).append('|')
                  .append(nz(visuals.floorAt(tx, ty))).append('|')
                  .append(nz(visuals.overlayAt(tx, ty))).append('|')
                  .append(nz(visuals.wallAt(tx, ty))).append('|')
                  .append(nz(visuals.structureAt(tx, ty))).append(';');
            }
            Gdx.app.log("LabDump", sb.toString());
        }
    }

    private static String nz(String s) { return s == null ? "-" : s; }

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
        if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) {
            tileDebugEnabled = !tileDebugEnabled;
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

        if (tileDebugEnabled) {
            tileDebugRenderer.render(camera, room, visuals, batch);
        }
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
        tileDebugRenderer.dispose();
        // 텍스처는 GameAssets 소유이므로 여기서 해제하지 않는다.
    }
}

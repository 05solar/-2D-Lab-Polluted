package io.github.some_example_name;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import io.github.some_example_name.asset.GameAssets;
import io.github.some_example_name.combat.MonsterSpawnSystem;
import io.github.some_example_name.config.BalanceConfig;
import io.github.some_example_name.entity.monster.Monster;
import io.github.some_example_name.entity.Direction;
import io.github.some_example_name.entity.player.Player;
import io.github.some_example_name.render.FadeOverlayRenderer;
import io.github.some_example_name.screen.HeadquartersScreen;
import io.github.some_example_name.screen.LaboratoryScreen;
import io.github.some_example_name.world.HeadquartersLayout;
import io.github.some_example_name.world.LaboratoryLayout;
import io.github.some_example_name.world.MapId;
import io.github.some_example_name.world.MapTransitionController;
import io.github.some_example_name.world.PlayerSessionState;
import io.github.some_example_name.world.GameDifficulty;
import com.badlogic.gdx.math.Vector2;
import java.util.Collections;
import java.util.List;

/**
 * 게임 루트. 화면 전환과 전역 생명주기 자원(GameAssets)의 소유를 담당한다.
 * 게임 규칙(전투, AI, 보물 등)은 이 클래스에 두지 않는다.
 */
public class LaboratoryGame extends Game {

    private GameAssets assets;
    private final BalanceConfig config = new BalanceConfig();
    private final FadeOverlayRenderer fadeRenderer = new FadeOverlayRenderer();
    private HeadquartersLayout headquarters;
    private LaboratoryLayout laboratory;
    private PlayerSessionState session;
    private MapTransitionController transition;
    private List<Monster> laboratoryMonsters;
    private GameDifficulty difficulty = GameDifficulty.EASY;

    @Override
    public void create() {
        assets = new GameAssets();
        // 배경음악: 게임 시작부터 본부·연구소·화면 전환과 무관하게 무한 반복 재생.
        // (개발 캡처 등 무음이 필요하면 LAB_MUTE_MUSIC=1)
        if (!"1".equals(System.getenv("LAB_MUTE_MUSIC"))) {
            assets.backgroundMusic().setLooping(true);
            assets.backgroundMusic().setVolume(config.musicVolume);
            assets.backgroundMusic().play();
        }
        headquarters = HeadquartersLayout.create();
        // 개발용: LAB_DIFFICULTY=EASY|NORMAL|HARD 로 직접 난이도 맵을 띄운다(기본 EASY).
        String difficultyEnv = System.getenv("LAB_DIFFICULTY");
        if (difficultyEnv != null) {
            try { difficulty = GameDifficulty.valueOf(difficultyEnv.trim().toUpperCase()); }
            catch (IllegalArgumentException ignored) { }
        }
        String wallTest = System.getenv("LAB_WALL_TEST");
        boolean startInLaboratory = wallTest != null || "1".equals(System.getenv("LAB_START_LAB"));
        // 개발용 직행 입장도 선택 난이도의 스폰 수(EASY=기준×8)를 정확히 쓴다.
        config.applyDifficulty(difficulty);
        laboratory = "1".equals(wallTest) ? LaboratoryLayout.wallConnectionGallery()
            : "2".equals(wallTest) ? LaboratoryLayout.wallDoorAssemblyTestRoom()
            : "3".equals(wallTest) ? LaboratoryLayout.structureAtlasGallery()
            : LaboratoryLayout.forDifficulty(difficulty, config);
        MapId initial = startInLaboratory ? MapId.LABORATORY : MapId.HEADQUARTERS;
        Vector2 spawn = initial == MapId.HEADQUARTERS
            ? headquarters.room().spawnPoint() : laboratory.room().spawnPoint();
        String captureRoom = System.getenv("LAB_START_ROOM");
        if (initial == MapId.LABORATORY && captureRoom != null) {
            for (io.github.some_example_name.world.LaboratoryRoomArea area : laboratory.roomAreas())
                if (area.id().equalsIgnoreCase(captureRoom)) {
                    spawn.set(area.centerX(), area.centerY());
                    break;
                }
        }
        String hqTest = System.getenv("LAB_HQ_TEST");
        boolean vendingTest = initial == MapId.HEADQUARTERS && "VENDING".equals(hqTest);
        boolean waterTest = initial == MapId.HEADQUARTERS && "WATER".equals(hqTest);
        // Temporary capture/test spawns at each headquarters interaction target.
        if (vendingTest) spawn.set(11.45f, 6.3f);
        if (waterTest) spawn.set(9.8f, 3.5f);
        Player initialPlayer = new Player(spawn.x, spawn.y,
            config.playerBoundsWidth, config.playerBoundsHeight, config.playerMaxHp);
        if (vendingTest) initialPlayer.setFacing(Direction.UP);
        if (waterTest) {
            initialPlayer.setFacing(Direction.DOWN);
            initialPlayer.takeDamage(45);
        }
        session = new PlayerSessionState(initialPlayer);
        transition = new MapTransitionController(initial, config.mapFadeSeconds);
        if (initial == MapId.LABORATORY && wallTest == null && monstersEnabled())
            laboratoryMonsters = new MonsterSpawnSystem(config).spawn(laboratory.room(), config.monsterSpawnSeed);
        else if (initial == MapId.LABORATORY)
            laboratoryMonsters = new java.util.ArrayList<Monster>();
        setScreen(initial == MapId.HEADQUARTERS
            ? new HeadquartersScreen(this, assets, headquarters, session)
            : new LaboratoryScreen(this, assets, laboratory, session,
                laboratoryMonsters == null ? Collections.<Monster>emptyList() : laboratoryMonsters, config));
    }

    @Override public void render() {
        transition.update(Math.min(Gdx.graphics.getDeltaTime(), config.maxFrameDeltaSeconds));
        if (transition.state() == MapTransitionController.State.SWITCHING_MAP) {
            Screen previous = getScreen();
            if (transition.target() == MapId.LABORATORY) {
                headquarters.room().resetExitTrigger();
                config.applyDifficulty(difficulty);
                if (laboratoryMonsters == null && monstersEnabled())
                    laboratoryMonsters = new MonsterSpawnSystem(config).spawn(laboratory.room(), config.monsterSpawnSeed);
                else if (laboratoryMonsters == null)
                    laboratoryMonsters = new java.util.ArrayList<Monster>();
                Vector2 spawn = laboratory.room().spawnPoint();
                session.player().bounds().setPosition(
                    spawn.x - session.player().bounds().width / 2f, spawn.y);
                setScreen(new LaboratoryScreen(this, assets, laboratory, session, laboratoryMonsters, config));
            } else {
                laboratory.room().resetSouthExitTrigger();
                Vector2 spawn = headquarters.room().spawnPoint();
                session.player().bounds().setPosition(
                    spawn.x - session.player().bounds().width / 2f, spawn.y);
                setScreen(new HeadquartersScreen(this, assets, headquarters, session));
            }
            previous.dispose();
            transition.mapSwitched();
        }
        super.render();
    }

    public MapTransitionController transition() { return transition; }
    public void selectDifficulty(GameDifficulty selected) {
        if (selected == null) throw new IllegalArgumentException("difficulty");
        difficulty=selected; config.applyDifficulty(selected);
        laboratory=LaboratoryLayout.forDifficulty(selected, config); laboratoryMonsters=null;
    }
    public GameDifficulty difficulty() { return difficulty; }
    private static boolean monstersEnabled() { return !"1".equals(System.getenv("LAB_DISABLE_MONSTERS")); }
    public FadeOverlayRenderer fadeRenderer() { return fadeRenderer; }

    @Override
    public void dispose() {
        if (getScreen() != null) {
            getScreen().dispose();
        }
        if (assets != null) {
            assets.dispose();
        }
    }
}

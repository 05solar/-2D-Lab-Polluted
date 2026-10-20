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
    // 영속 프로필(재화·본부 보관함·강화)과 본부 경제. 프로필 로드는 본부 UI 생성보다 먼저다.
    private io.github.some_example_name.profile.HeadquartersEconomy economy;
    private io.github.some_example_name.profile.FileProfileStore profileStore;
    // 진행 중 탐사 식별자: 연구소 입장 시 발급, 귀환 이관 중복 방지에 사용.
    private String expeditionId;
    private boolean transferPending; // 귀환 저장 실패 시 true — 가방을 유지한 채 재시도 가능

    @Override
    public void create() {
        assets = new GameAssets();
        // 프로필: 앱 전용 쓰기 가능 경로(사용자 홈). assets/클래스패스에 저장하지 않는다.
        // 첫 실행에만 기본 프로필을 만들고, 이후 실행은 저장본(재화·보관함·강화)을 복원한다.
        profileStore = new io.github.some_example_name.profile.FileProfileStore(
            new java.io.File(System.getProperty("user.home"), ".2d-lab-polluted"));
        io.github.some_example_name.profile.PlayerProfile profile = null;
        try {
            profile = profileStore.load();
        } catch (java.io.IOException failed) {
            Gdx.app.error("Profile", "load failed: " + failed);
        }
        if (profile == null) profile = new io.github.some_example_name.profile.PlayerProfile();
        if (profileStore.recoveredFromBackup())
            Gdx.app.log("Profile", "main save damaged - recovered from backup");
        if (profileStore.corruptionDetected())
            Gdx.app.error("Profile", "all saves damaged - preserved as *.corrupt, new profile started");
        economy = new io.github.some_example_name.profile.HeadquartersEconomy(
            profile, profileStore, assets.loot().prices());
        Gdx.app.log("Profile", "loaded rev=" + profile.profileRevision
            + " money=" + profile.currencyWon + " storage=" + profile.storage.size());
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
        if (initial == MapId.LABORATORY) expeditionId = java.util.UUID.randomUUID().toString();
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
                // 새 탐사 시작: 귀환 이관 중복 방지용 식별자를 발급한다.
                expeditionId = java.util.UUID.randomUUID().toString();
                Vector2 spawn = laboratory.room().spawnPoint();
                session.player().bounds().setPosition(
                    spawn.x - session.player().bounds().width / 2f, spawn.y);
                setScreen(new LaboratoryScreen(this, assets, laboratory, session, laboratoryMonsters, config));
            } else {
                laboratory.room().resetSouthExitTrigger();
                // 사망 재소환: 본부 도착 시점에 부활(HP 최대). 소지품은 연구소에서 이미 비웠다.
                boolean respawnedFromDeath = session.player().isDead();
                if (respawnedFromDeath) {
                    session.player().revive();
                    Gdx.app.log("Respawn", "revived at HQ hp=" + session.player().hp()
                        + " items=" + session.inventory().size());
                } else {
                    // 성공 귀환 1회: 가방 → 본부 보관함 이관(자동 판매 없음). 저장 성공 후에만
                    // 가방을 비운다. 실패하면 가방을 유지하고 본부에서 재시도한다.
                    attemptReturnTransfer();
                }
                Vector2 spawn = headquarters.room().spawnPoint();
                session.player().bounds().setPosition(
                    spawn.x - session.player().bounds().width / 2f, spawn.y);
                setScreen(new HeadquartersScreen(this, assets, headquarters, session));
                if (respawnedFromDeath && "1".equals(System.getenv("LAB_TEST_DEATH")))
                    Gdx.app.exit();
            }
            previous.dispose();
            transition.mapSwitched();
        }
        super.render();
    }

    /**
     * 성공 귀환 이관(1회 보장): 보관함 추가 + 귀환 처리 기록을 한 번의 저장으로 반영하고,
     * 저장이 성공했을 때만 가방을 비운다. 같은 expeditionId는 재호출돼도 복제되지 않는다.
     */
    public void attemptReturnTransfer() {
        if (economy == null || expeditionId == null || session.backpack().count() == 0) {
            transferPending = false;
            return;
        }
        io.github.some_example_name.profile.HeadquartersEconomy.Result result =
            economy.transferReturn(expeditionId, session.inventory());
        switch (result.status) {
            case OK:
            case ALREADY_PROCESSED:
                session.backpack().handOver();
                transferPending = false;
                Gdx.app.log("Profile", "return transfer " + result.status
                    + " items=" + result.itemCount + " storage=" + economy.storageView().size());
                break;
            case SAVE_FAILED:
                transferPending = true; // 물품을 잃지 않는다 — 가방 유지, 재시도 가능
                Gdx.app.error("Profile", "return transfer save failed - backpack kept, will retry");
                break;
            default:
                transferPending = false;
                break;
        }
    }

    public boolean transferPending() { return transferPending; }
    public io.github.some_example_name.profile.HeadquartersEconomy economy() { return economy; }

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

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
import io.github.some_example_name.screen.BossArenaScreen;
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
    // 시드 기반 연구소 생성(2026-10-20): 데이터 팩 + 현재 탐사의 생성 결과(재현 메타데이터 포함).
    // 새 시드는 "새 탐사 확정"(selectDifficulty / 개발 직행 입장)에서만 만든다 — 화면 재생성·
    // 창 크기 변경·일시정지는 맵을 다시 만들지 않는다(laboratory 필드 유지).
    private io.github.some_example_name.world.LabGenData labGenData;
    private io.github.some_example_name.world.LaboratoryGenerator.Result generatedLab;
    // 보스 모드(2026-10-20): 단계 카탈로그·보스 정의·현재 시도(runId/attemptSeed).
    // 새 runId는 "입장 확정"과 "재도전"에서만 발급된다(선택창 열람으로는 생성되지 않는다).
    private io.github.some_example_name.boss.BossStageCatalog bossStageCatalog;
    private io.github.some_example_name.boss.BossPatternCatalog bossPatternCatalog;
    private io.github.some_example_name.boss.BossArenaCatalog bossArenaCatalog;
    private io.github.some_example_name.boss.BossDefinition bossDefinition;
    private String bossStageId = "1-1";
    private String bossRunId;
    private long bossAttemptSeed;
    // 영속 프로필(재화·본부 보관함·강화)과 본부 경제. 프로필 로드는 본부 UI 생성보다 먼저다.
    private io.github.some_example_name.profile.HeadquartersEconomy economy;
    private io.github.some_example_name.profile.FileProfileStore profileStore;
    // 강화 트리 카탈로그(가격·효과의 유일 출처)와 기본 능력치(파생 계산의 기준값 보존).
    private io.github.some_example_name.profile.UpgradeCatalog upgradeCatalog;
    // 추가 스킬 3종(충격파/전기 투척/보호막) 밸런스 카탈로그 — skills.json 유일 출처.
    private io.github.some_example_name.profile.SkillCatalog skillCatalog;
    private int baseMaxHp, baseAttackDamage;
    // 공통 스킬 시스템: 장착/키 구성(프로필 연동)과 skillId별 쿨다운(맵 전환에도 유지).
    private io.github.some_example_name.profile.SkillLoadout skillLoadout;
    private final io.github.some_example_name.profile.SkillRuntime skillRuntime =
        new io.github.some_example_name.profile.SkillRuntime();
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
        // 강화 카탈로그(로딩 단계에서 중복/순환/음수가/미지 효과 검증)와 기준 능력치 기록.
        upgradeCatalog = new io.github.some_example_name.profile.UpgradeCatalog(
            Gdx.files.internal(io.github.some_example_name.config.AssetPaths.UPGRADE_TREE_DATA)
                .readString("UTF-8"));
        skillCatalog = new io.github.some_example_name.profile.SkillCatalog(
            Gdx.files.internal(io.github.some_example_name.config.AssetPaths.THREE_SKILLS_DATA)
                .readString("UTF-8"));
        // 9단계 카탈로그(단계 ID·저장 키는 1-1 팩과 동일 — 기존 세이브 그대로 호환).
        bossStageCatalog = new io.github.some_example_name.boss.BossStageCatalog(
            Gdx.files.internal(io.github.some_example_name.config.AssetPaths.BOSS9_STAGES_DATA)
                .readString("UTF-8"));
        bossPatternCatalog = new io.github.some_example_name.boss.BossPatternCatalog(
            Gdx.files.internal(io.github.some_example_name.config.AssetPaths.BOSS9_PATTERNS_DATA)
                .readString("UTF-8"));
        bossArenaCatalog = new io.github.some_example_name.boss.BossArenaCatalog(
            Gdx.files.internal(io.github.some_example_name.config.AssetPaths.BOSS9_ARENAS_DATA)
                .readString("UTF-8"));
        bossDefinition = new io.github.some_example_name.boss.BossDefinition(
            Gdx.files.internal(io.github.some_example_name.config.AssetPaths.BOSS_1_1_DATA)
                .readString("UTF-8"));
        baseMaxHp = config.playerMaxHp;
        baseAttackDamage = config.playerAttackDamage;
        applyUpgradeEffects(); // 플레이어/HUD 생성 전에 파생 능력치 확정
        // 공통 스킬: 프로필에서 장착/키 로드(최초 실행만 F/Q/1/2/3 기본값), 쿨다운 스냅샷 복원.
        boolean firstSkillSetup = profile.skillSlots.isEmpty() && profile.skillKeys.isEmpty();
        reloadSkillLoadout();
        skillRuntime.restore(profile.skillCooldowns);
        economy.setCooldownSnapshotProvider(
            new java.util.concurrent.Callable<java.util.Map<String, Float>>() {
                @Override public java.util.Map<String, Float> call() { return skillRuntime.snapshot(); }
            });
        if (firstSkillSetup) saveSkillLoadout(); // 기본값 1회 영속화(이후엔 변경 시에만)
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
        // 생성 데이터 팩(구조 틀 12종·난이도 프로필) — 로딩 실패는 치명(수제 맵 폴백은
        // 생성 단계에서만). 테스트는 같은 데이터를 java.nio로 읽는다.
        labGenData = new io.github.some_example_name.world.LabGenData(
            new io.github.some_example_name.world.LabGenData.Source() {
                @Override public String read(String relativePath) {
                    return Gdx.files.internal("laboratory_generation_v1/" + relativePath)
                        .readString("UTF-8");
                }
            });
        if ("1".equals(wallTest)) laboratory = LaboratoryLayout.wallConnectionGallery();
        else if ("2".equals(wallTest)) laboratory = LaboratoryLayout.wallDoorAssemblyTestRoom();
        else if ("3".equals(wallTest)) laboratory = LaboratoryLayout.structureAtlasGallery();
        else generateExpedition(difficulty);
        // 개발용 직행: LAB_START_BOSS=1 → 보스 광장으로 바로 입장(새 runId 발급).
        // LAB_BOSS_STAGE=1-2 등으로 단계 지정(기본 1-1).
        boolean startInBoss = "1".equals(System.getenv("LAB_START_BOSS"));
        MapId initial = startInBoss ? MapId.BOSS_ARENA
            : startInLaboratory ? MapId.LABORATORY : MapId.HEADQUARTERS;
        if (startInBoss) {
            String stageEnv = System.getenv("LAB_BOSS_STAGE");
            if (stageEnv != null && bossStageCatalog.stage(stageEnv.trim()) != null)
                bossStageId = stageEnv.trim();
            newBossRun();
        }
        Vector2 spawn = initial == MapId.LABORATORY
            ? laboratory.room().spawnPoint() : headquarters.room().spawnPoint();
        String captureRoom = System.getenv("LAB_START_ROOM");
        if (initial == MapId.LABORATORY && captureRoom != null) {
            // 생성 맵 방 이름은 "테마_rNN" — 부분 일치(BIOLOGY ↔ biology_r07)도 허용한다.
            for (io.github.some_example_name.world.LaboratoryRoomArea area : laboratory.roomAreas())
                if (area.id().equalsIgnoreCase(captureRoom)
                    || area.id().toUpperCase().contains(captureRoom.toUpperCase())) {
                    spawn.set(area.centerX(), area.centerY());
                    break;
                }
        }
        String hqTest = System.getenv("LAB_HQ_TEST");
        boolean vendingTest = initial == MapId.HEADQUARTERS && "VENDING".equals(hqTest);
        boolean waterTest = initial == MapId.HEADQUARTERS && "WATER".equals(hqTest);
        boolean skillTableTest = initial == MapId.HEADQUARTERS && "SKILL".equals(hqTest);
        boolean benchTest = initial == MapId.HEADQUARTERS && "BENCH".equals(hqTest);
        // Temporary capture/test spawns at each headquarters interaction target.
        if (vendingTest) spawn.set(11.45f, 6.3f);
        if (waterTest) spawn.set(9.8f, 3.5f);
        if (skillTableTest) spawn.set(8.75f, 6.35f);  // 스킬 테이블(8.75,7.45) 남쪽 정면
        if (benchTest) spawn.set(4.35f, 6.35f);       // 접이식 작업대(4.35,7.45) 남쪽 정면
        if (initial == MapId.HEADQUARTERS && "COMPUTER".equals(hqTest)) {
            spawn.set(6.55f, 6.25f);                  // 메인 컴퓨터(터미널 책상) 남쪽 정면
        }
        Player initialPlayer = new Player(spawn.x, spawn.y,
            config.playerBoundsWidth, config.playerBoundsHeight, config.playerMaxHp);
        if (vendingTest || skillTableTest || benchTest
            || "COMPUTER".equals(hqTest)) initialPlayer.setFacing(Direction.UP);
        if (waterTest) {
            initialPlayer.setFacing(Direction.DOWN);
            initialPlayer.takeDamage(45);
        }
        session = new PlayerSessionState(initialPlayer);
        transition = new MapTransitionController(initial, config.mapFadeSeconds);
        if (initial == MapId.LABORATORY) expeditionId = java.util.UUID.randomUUID().toString();
        if (initial == MapId.LABORATORY)
            laboratoryMonsters = wallTest == null ? spawnLaboratoryMonsters()
                : new java.util.ArrayList<Monster>();
        setScreen(initial == MapId.BOSS_ARENA
            ? new BossArenaScreen(this, assets, session, config)
            : initial == MapId.HEADQUARTERS
            ? new HeadquartersScreen(this, assets, headquarters, session)
            : new LaboratoryScreen(this, assets, laboratory, session,
                laboratoryMonsters == null ? Collections.<Monster>emptyList() : laboratoryMonsters, config));
    }

    @Override public void render() {
        transition.update(Math.min(Gdx.graphics.getDeltaTime(), config.maxFrameDeltaSeconds));
        if (transition.state() == MapTransitionController.State.SWITCHING_MAP) {
            Screen previous = getScreen();
            if (transition.target() == MapId.BOSS_ARENA) {
                headquarters.room().resetExitTrigger();
                // 보스 입장: 일반 탐사 식별자/루트와 무관 — 가방 이관·루트 재생성 없음.
                setScreen(new BossArenaScreen(this, assets, session, config));
            } else if (transition.target() == MapId.LABORATORY) {
                headquarters.room().resetExitTrigger();
                config.applyDifficulty(difficulty);
                if (laboratoryMonsters == null) laboratoryMonsters = spawnLaboratoryMonsters();
                // 새 탐사 시작: 귀환 이관 중복 방지용 식별자를 발급한다.
                expeditionId = java.util.UUID.randomUUID().toString();
                Vector2 spawn = laboratory.room().spawnPoint();
                session.player().bounds().setPosition(
                    spawn.x - session.player().bounds().width / 2f, spawn.y);
                setScreen(new LaboratoryScreen(this, assets, laboratory, session, laboratoryMonsters, config));
            } else {
                // 보스전 종료 복귀는 일반 탐사 귀환이 아니다 — 가방 이관/루트 초기화를
                // 중복 실행하지 않는다(영구 보관함·강화·장착·쿨다운·돈은 유지).
                boolean fromBossArena = transition.current() == MapId.BOSS_ARENA;
                if (!fromBossArena) laboratory.room().resetSouthExitTrigger();
                // 사망 재소환: 본부 도착 시점에 부활(HP 최대). 일반 연구소는 소지품을 이미
                // 비웠고, 보스 실패 복귀는 소지품·진행을 그대로 보존한다.
                boolean respawnedFromDeath = session.player().isDead();
                if (respawnedFromDeath) {
                    session.player().revive();
                    Gdx.app.log("Respawn", "revived at HQ hp=" + session.player().hp()
                        + " items=" + session.inventory().size());
                } else if (!fromBossArena) {
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
    public io.github.some_example_name.profile.UpgradeCatalog upgradeCatalog() { return upgradeCatalog; }
    /** 파생 능력치(강화 반영)가 적용된 전역 설정 — 화면별 사본이 아닌 이 인스턴스를 쓴다. */
    public BalanceConfig config() { return config; }

    /**
     * 파생 능력치 적용: 항상 "기준값 + 구매 노드 합"으로 다시 계산해 config와 플레이어에
     * 반영한다(멱등 — 로드/전환/재진입에 중복 누적 없음). 체력 강화 시 현재 HP는 유지된다.
     */
    public void applyUpgradeEffects() {
        io.github.some_example_name.profile.UpgradeEffects effects =
            io.github.some_example_name.profile.UpgradeEffects.compute(
                upgradeCatalog, economy.profile().purchasedNodeIds());
        if (!effects.unknownPurchasedIds.isEmpty())
            Gdx.app.log("Upgrade", "unknown purchased node ids preserved: "
                + effects.unknownPurchasedIds);
        config.playerMaxHp = baseMaxHp + effects.bonusMaxHp;
        config.playerAttackDamage = baseAttackDamage + effects.bonusAttack;
        if (session != null) session.player().setMaxHp(config.playerMaxHp);
    }

    public io.github.some_example_name.profile.UpgradeEffects currentUpgradeEffects() {
        return io.github.some_example_name.profile.UpgradeEffects.compute(
            upgradeCatalog, economy.profile().purchasedNodeIds());
    }

    /** 회복 스킬: 해금 + 슬롯 장착 여부(공통 로드아웃 기준). */
    public boolean healSkillEquipped() {
        return currentUpgradeEffects().healUnlocked()
            && skillLoadout.slotOf(io.github.some_example_name.profile.SkillLoadout.HEAL) >= 0;
    }
    public int healSkillAmount() {
        return upgradeCatalog.healBaseAmount + currentUpgradeEffects().healBonus;
    }
    public float healSkillCooldownSeconds() { return upgradeCatalog.healCooldownSeconds; }
    /** 현재 활성 스킬 슬롯 수(기본 2 + 확장 노드, 최대 5). */
    public int activeSkillSlots() {
        return Math.min(config.maxActiveSkillSlots,
            config.baseActiveSkillSlots + currentUpgradeEffects().extraActiveSlots);
    }

    // ----- 공통 스킬 시스템 -------------------------------------------------------------

    public io.github.some_example_name.profile.SkillLoadout skillLoadout() { return skillLoadout; }
    public io.github.some_example_name.profile.SkillRuntime skillRuntime() { return skillRuntime; }

    /** 보유(영구 해금) 스킬 집합: 돌진은 기본, 나머지는 SKILL_UNLOCK 노드 연동(skillId). */
    public java.util.Set<String> unlockedSkills() {
        java.util.Set<String> unlocked = new java.util.LinkedHashSet<>();
        unlocked.add(io.github.some_example_name.profile.SkillLoadout.DASH);
        unlocked.addAll(currentUpgradeEffects().unlockedSkillIds);
        return unlocked;
    }

    public io.github.some_example_name.profile.SkillCatalog skillCatalog() { return skillCatalog; }

    /** skillId별 쿨다운 전체 길이(초) — 돌진은 BalanceConfig, 회복은 강화 카탈로그, 3종은 skills.json. */
    public float skillCooldownSeconds(String skillId) {
        if (io.github.some_example_name.profile.SkillLoadout.DASH.equals(skillId))
            return config.dashCooldownSeconds;
        if (io.github.some_example_name.profile.SkillLoadout.SHOCKWAVE.equals(skillId))
            return skillCatalog.shockwaveCooldownSeconds;
        if (io.github.some_example_name.profile.SkillLoadout.ELECTRIC_THROW.equals(skillId))
            return skillCatalog.electricCooldownSeconds;
        if (io.github.some_example_name.profile.SkillLoadout.SHIELD.equals(skillId))
            return skillCatalog.shieldCooldownSeconds;
        return healSkillCooldownSeconds();
    }

    /** 스킬 전용 피해(기본 + SKILL_DAMAGE_ADD 강화) — 기본 공격 보너스를 중복 가산하지 않는다. */
    public int skillDamage(String skillId) {
        int base = io.github.some_example_name.profile.SkillLoadout.SHOCKWAVE.equals(skillId)
            ? skillCatalog.shockwaveDamage
            : io.github.some_example_name.profile.SkillLoadout.ELECTRIC_THROW.equals(skillId)
            ? skillCatalog.electricDamage : 0;
        return base + currentUpgradeEffects().skillDamageBonus(skillId);
    }

    /** 보호막 실제 흡수량(기본 + 강화). */
    public int shieldAbsorbAmount() {
        return skillCatalog.shieldAbsorbAmount + currentUpgradeEffects().shieldAbsorbBonus;
    }

    /** 프로필에서 장착/키 구성 재로드(+정리 로그). 강화 구매 후 자동 장착 반영에도 쓴다. */
    public void reloadSkillLoadout() {
        skillLoadout = io.github.some_example_name.profile.SkillLoadout.fromProfile(
            economy.profile(), unlockedSkills(),
            io.github.some_example_name.input.KeyBindings.reservedKeys());
        for (String reason : skillLoadout.sanitizeLog()) Gdx.app.log("Skills", reason);
    }

    /** 장착/키 변경 영속화(원자 저장 흐름 공유). 실패 시 프로필은 이전 설정 유지. */
    public io.github.some_example_name.profile.HeadquartersEconomy.Result saveSkillLoadout() {
        return economy.saveSkillLoadout(skillLoadout.slotsForSave(), skillLoadout.keysForSave());
    }

    /** 스킬 쿨다운 진행(플레이 가능한 게임 시간만 — 일시정지/전환 중 호출하지 않는다). */
    public void tickSkillCooldowns(float delta) { skillRuntime.tick(delta); }

    // ----- 보스 모드 ---------------------------------------------------------------------

    public io.github.some_example_name.boss.BossStageCatalog bossStageCatalog() {
        return bossStageCatalog;
    }
    public io.github.some_example_name.boss.BossPatternCatalog bossPatternCatalog() {
        return bossPatternCatalog;
    }
    public io.github.some_example_name.boss.BossArenaCatalog bossArenaCatalog() {
        return bossArenaCatalog;
    }
    public io.github.some_example_name.boss.BossDefinition bossDefinition() { return bossDefinition; }
    public String bossStageId() { return bossStageId; }
    public io.github.some_example_name.boss.BossStageCatalog.Stage bossStage() {
        return bossStageCatalog.stage(bossStageId);
    }
    public String bossRunId() { return bossRunId; }
    public long bossAttemptSeed() { return bossAttemptSeed; }

    /** 해금 집합(디버그 LAB_BOSS_UNLOCK_ALL=1은 개발 전용 — 릴리스 진행과 분리). */
    public java.util.Set<String> bossUnlockedView() {
        java.util.Set<String> saved = economy.profile().bossUnlockedStages;
        if (!"1".equals(System.getenv("LAB_BOSS_UNLOCK_ALL"))) return saved;
        java.util.Set<String> all = new java.util.LinkedHashSet<>(saved);
        for (io.github.some_example_name.boss.BossStageCatalog.Stage stage
            : bossStageCatalog.stages()) all.add(stage.stageId);
        return all;
    }

    /**
     * 단계 입장 확정(선택창 입장 버튼): 해금+구현 단계만, 전환 중 재호출은
     * MapTransitionController가 거부해 더블클릭으로 같은 시도 화면이 2개 생기지 않는다.
     * 여기서만 새 runId/attemptSeed를 발급·저장한다.
     */
    public boolean startBossRun(String stageId) {
        if (bossStageCatalog == null
            || !bossStageCatalog.enterable(stageId, bossUnlockedView()))
            return false;
        if (transition.inputLocked()) return false;
        bossStageId = stageId;
        newBossRun();
        boolean requested = transition.request(MapId.BOSS_ARENA);
        if (requested) Gdx.app.log("Boss", "enter stage=" + stageId + " runId=" + bossRunId
            + " attemptSeed=" + bossAttemptSeed);
        return requested;
    }

    /** 재도전: 새 runId/attemptSeed(새 유효 토벌 = 새 보상 1개). LAB_BOSS_SEED로 고정 가능. */
    public void newBossRun() {
        bossRunId = java.util.UUID.randomUUID().toString();
        bossAttemptSeed = new java.util.Random().nextLong();
        String seedEnv = System.getenv("LAB_BOSS_SEED");
        if (seedEnv != null) try { bossAttemptSeed = Long.parseLong(seedEnv.trim()); }
            catch (NumberFormatException ignored) { }
    }

    /**
     * 이번 run의 보상(기존 LEGENDARY 카탈로그 가중 추첨). runId+stageId 파생 시드라 결과창
     * 재진입/저장 재시도/재실행에도 같은 아이템이다 — 두 번 뽑히지 않는다.
     */
    public io.github.some_example_name.inventory.LootItem rewardItemForRun() {
        long seed = (bossRunId + "#" + bossStageId).hashCode() * 2654435761L;
        return assets.loot().catalog().roll("legendary_cabinet", new java.util.Random(seed));
    }

    /** 유효 토벌 원자 확정(클리어 기록+다음 단계 해금+보상 인스턴스+영수증 1저장). */
    public io.github.some_example_name.profile.HeadquartersEconomy.Result commitBossReward(
            String rewardItemId) {
        io.github.some_example_name.boss.BossStageCatalog.Stage stage =
            bossStageCatalog.stage(bossStageId);
        return economy.commitBossClear(bossRunId, bossStageId,
            stage == null ? null : stage.nextStage, rewardItemId);
    }

    public MapTransitionController transition() { return transition; }
    public void selectDifficulty(GameDifficulty selected) {
        if (selected == null) throw new IllegalArgumentException("difficulty");
        difficulty=selected; config.applyDifficulty(selected);
        // 난이도 "확정"이 곧 새 탐사 — 여기서만 새 시드로 맵을 생성한다.
        generateExpedition(selected);
        laboratoryMonsters=null;
    }
    public GameDifficulty difficulty() { return difficulty; }

    /**
     * 새 탐사 확정 시에만 호출: 64비트 탐사 시드 발급 → 시드 기반 생성. 같은 시드+버전+
     * 난이도는 같은 맵이다(재현 메타데이터를 로그로 남긴다 — LAB_SEED로 시드 고정 가능,
     * LAB_CLASSIC_MAP=1이면 기존 수제 맵 경로).
     */
    private void generateExpedition(GameDifficulty selected) {
        if ("1".equals(System.getenv("LAB_CLASSIC_MAP"))) {
            laboratory = LaboratoryLayout.forDifficulty(selected, config);
            generatedLab = null;
            Gdx.app.log("LabGen", "classic handcrafted map (LAB_CLASSIC_MAP=1)");
            return;
        }
        long seed = new java.util.Random().nextLong(); // 시드 "발급"만 비결정 — 생성은 결정적
        String seedEnv = System.getenv("LAB_SEED");
        if (seedEnv != null) try { seed = Long.parseLong(seedEnv.trim()); }
            catch (NumberFormatException ignored) { }
        generatedLab = io.github.some_example_name.world.LaboratoryGenerator.generate(
            selected, seed, config, labGenData);
        laboratory = generatedLab.layout;
        Gdx.app.log("LabGen", generatedLab.describe());
    }

    /** 생성 맵은 플랜(배치 데이터)에서 소환하고, 수제 폴백 맵은 기존 스폰 시스템을 쓴다. */
    private List<Monster> spawnLaboratoryMonsters() {
        if (!monstersEnabled()) return new java.util.ArrayList<Monster>();
        if (generatedLab == null || generatedLab.monsters == null)
            return new MonsterSpawnSystem(config).spawn(laboratory.room(), config.monsterSpawnSeed);
        List<Monster> out = new java.util.ArrayList<>();
        long id = 1;
        for (io.github.some_example_name.world.LaboratoryGenerator.MonsterPlacement p
                : generatedLab.monsters) {
            Monster monster = new Monster(id++, p.type, p.x, p.y,
                config.monsterBoundsWidth, config.monsterBoundsHeight, monsterMaxHp(p.type));
            monster.setRestTime((id % 7) * 0.3f); // 결정적 분산(전역 Random/시스템 시간 비사용)
            out.add(monster);
        }
        Gdx.app.log("LabGen", "spawned " + out.size() + " monsters from generated plan");
        return out;
    }

    private int monsterMaxHp(io.github.some_example_name.combat.MonsterType type) {
        switch (type) {
            case SLIME: return config.slimeMaxHp;
            case RESEARCHER: return config.researcherMaxHp;
            case GUARD: return config.guardMaxHp;
            default: return config.teamLeaderMaxHp;
        }
    }
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

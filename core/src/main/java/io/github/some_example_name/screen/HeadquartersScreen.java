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
    private final VendingMachineInteraction benchInteraction = new VendingMachineInteraction();
    private final VendingMachineInteraction skillInteraction = new VendingMachineInteraction();
    private final VendingMachineInteraction computerInteraction = new VendingMachineInteraction();
    // 보스 단계 선택(메인 컴퓨터 SPACE — 열람만으로는 runId/보상이 생기지 않는다).
    private final io.github.some_example_name.render.BossStageSelectRenderer bossStagePanel;
    private final io.github.some_example_name.render.UpgradeTreeRenderer upgradePanel;
    // 스킬 장착/키 등록 패널 + 열기 버튼(오디오 토글 오른쪽) — 연구소와 같은 공통 구성 데이터.
    private final io.github.some_example_name.render.SkillLoadoutRenderer skillPanel;
    private final io.github.some_example_name.render.SkillMenuButtonRenderer skillMenuButton =
        new io.github.some_example_name.render.SkillMenuButtonRenderer();
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
    // LAB_TEST_UPGRADE=1(개발 전용): 재화 확보→강화창→구매 2건→캡처→종료(영속 검증용).
    private final boolean testUpgrade = "1".equals(System.getenv("LAB_TEST_UPGRADE"));
    private int testUpgradeFrame;
    // LAB_TEST_SKILLS=1(개발 전용): 스킬 패널 열기→선택→키 대기 상태→캡처→종료.
    private final boolean testSkills = "1".equals(System.getenv("LAB_TEST_SKILLS"));
    private int testSkillsFrame;
    // LAB_TEST_BOSSPANEL=1(개발 전용): 컴퓨터 프롬프트→단계 선택창→캡처→종료.
    private final boolean testBossPanel = "1".equals(System.getenv("LAB_TEST_BOSSPANEL"));
    private int testBossFrame;
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
        upgradePanel = new io.github.some_example_name.render.UpgradeTreeRenderer(
            assets.upgrade(), assets.hqSales(), assets.uiFont());
        skillPanel = new io.github.some_example_name.render.SkillLoadoutRenderer(
            assets.skills(), assets.threeSkills(), assets.combatUi().dashIcon(), assets.uiFont());
        bossStagePanel = new io.github.some_example_name.render.BossStageSelectRenderer(
            assets.boss(), assets.bossNine(), assets.uiFont());
        room = layout.room();
        visuals = layout.visuals();
        player = session.player();
        movement = new MovementSystem(new CollisionSystem(), new RunStartDetector(),
            config.playerWalkSpeed, config.playerRunSpeed);
        playerRenderer = new EntityRenderer(new AnimationController(assets.playerAnimations()));
        worldRenderer = new HeadquartersRenderer(assets.headquarters(), visuals,
            assets.hqSales().saleSign(), assets.upgrade().bench());
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
        } else if (upgradePanel.isOpen()) {
            // 강화창도 동일하게 월드 입력 차단 + Esc는 창만 닫는다.
            inputState.update(EnumSet.noneOf(GameAction.class));
            player.setMoving(false);
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) upgradePanel.onEscape();
            else upgradePanel.handleInput();
        } else if (skillPanel.isOpen()) {
            // 스킬창: 월드 입력 차단(키 등록 대기 키가 스킬/이동으로 새지 않음) + Esc는
            // 키 대기 취소 → 창 닫기 순서.
            inputState.update(EnumSet.noneOf(GameAction.class));
            player.setMoving(false);
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) skillPanel.onEscape();
            else skillPanel.handleInput();
        } else if (bossStagePanel.isOpen()) {
            // 보스 단계 선택창: 월드 입력 차단, 전환 중 버튼 잠금(더블클릭 중복 진입 방지).
            inputState.update(EnumSet.noneOf(GameAction.class));
            player.setMoving(false);
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) bossStagePanel.onEscape();
            else bossStagePanel.handleInput(transitionLocked);
        } else {
            input.poll(inputState);
            if (Gdx.input.isKeyJustPressed(Input.Keys.R)) backpackOpen = !backpackOpen;
            if (inputState.isPressed(GameAction.PAUSE)) {
                if (backpackOpen) backpackOpen = false;   // 가방이 열려 있으면 Esc는 가방부터 닫는다
                else paused = !paused;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) debug = !debug;
            // 오디오 토글 버튼(HP 바 오른쪽) — 효과음+배경음악 세트 on/off, 연구소와 설정 공유.
            if (Gdx.input.justTouched()) {
                if (sfxToggle.buttonHit(Gdx.input.getX(), Gdx.input.getY()))
                    assets.toggleAudio();
                else if (skillMenuButton.buttonHit(Gdx.input.getX(), Gdx.input.getY()))
                    openSkillPanel();
            }
        }
        if (!paused && !transitionLocked && !backpackOpen && !difficultySelection
            && !salePanel.isOpen() && !upgradePanel.isOpen() && !skillPanel.isOpen()
            && !bossStagePanel.isOpen()) {
            float oldFeetY = player.feetY();
            PlayerIntent intent = controller.intentFrom(player, inputState);
            movement.update(player, intent, room, dt);
            if (!player.isDead() && room.crossedSouthExit(oldFeetY, player, intent.moveY))
                difficultySelection = true;
        }
        boolean interactionContext = !player.isDead() && !paused && !transitionLocked
            && !backpackOpen && !difficultySelection && !salePanel.isOpen()
            && !upgradePanel.isOpen() && !skillPanel.isOpen() && !bossStagePanel.isOpen();
        boolean vendingAvailable = interactionContext && room.canInteractWithVending(player);
        boolean waterAvailable = interactionContext && room.canInteractWithWaterDispenser(player);
        boolean benchAvailable = interactionContext && room.canInteractWithUpgradeBench(player);
        boolean skillAvailable = interactionContext && room.canInteractWithSkillTable(player);
        boolean computerAvailable = interactionContext && room.canInteractWithMainComputer(player);
        boolean vendingPromptVisible = interactionContext && room.isNearVending(player);
        boolean waterPromptVisible = interactionContext && room.isNearWaterDispenser(player);
        boolean benchPromptVisible = interactionContext && room.isNearUpgradeBench(player);
        boolean skillPromptVisible = interactionContext && room.isNearSkillTable(player);
        boolean computerPromptVisible = interactionContext && room.isNearMainComputer(player);
        // 여러 대상이 범위에 있어도 가장 가까운 유효 대상 하나만 안내·처리한다.
        int visibleCount = (vendingPromptVisible ? 1 : 0) + (waterPromptVisible ? 1 : 0)
            + (benchPromptVisible ? 1 : 0) + (skillPromptVisible ? 1 : 0)
            + (computerPromptVisible ? 1 : 0);
        if (visibleCount > 1) {
            float vendingDistance = vendingPromptVisible
                ? room.interactionPoint("vending_machine").dst2(player.feetX(), player.feetY())
                : Float.MAX_VALUE;
            float waterDistance = waterPromptVisible
                ? room.interactionPoint("water_dispenser").dst2(player.feetX(), player.feetY())
                : Float.MAX_VALUE;
            float benchDistance = benchPromptVisible
                ? room.interactionPoint("folding_workbench").dst2(player.feetX(), player.feetY())
                : Float.MAX_VALUE;
            float skillDistance = skillPromptVisible
                ? room.interactionPoint("skill_table").dst2(player.feetX(), player.feetY())
                : Float.MAX_VALUE;
            float computerDistance = computerPromptVisible
                ? room.interactionPoint("terminal_desk").dst2(player.feetX(), player.feetY())
                : Float.MAX_VALUE;
            float nearest = Math.min(computerDistance, Math.min(
                Math.min(vendingDistance, waterDistance),
                Math.min(benchDistance, skillDistance)));
            vendingPromptVisible = vendingDistance <= nearest;
            waterPromptVisible = !vendingPromptVisible && waterDistance <= nearest;
            benchPromptVisible = !vendingPromptVisible && !waterPromptVisible
                && benchDistance <= nearest;
            skillPromptVisible = !vendingPromptVisible && !waterPromptVisible
                && !benchPromptVisible && skillDistance <= nearest;
            computerPromptVisible = !vendingPromptVisible && !waterPromptVisible
                && !benchPromptVisible && !skillPromptVisible;
        }
        vendingAvailable &= vendingPromptVisible;
        waterAvailable &= waterPromptVisible;
        benchAvailable &= benchPromptVisible;
        skillAvailable &= skillPromptVisible;
        computerAvailable &= computerPromptVisible;
        boolean promptIsWater = waterPromptVisible;
        float animDelta = paused || transitionLocked || backpackOpen ? 0f : dt;
        // SPACE는 상호작용 전용 키(공격은 마우스 클릭 전용 — 2026-10-16 키 정리).
        boolean spacePressed = inputState.isPressed(GameAction.INTERACT);
        boolean vendingFired = vendingInteraction.update(vendingPromptVisible,
            vendingAvailable && spacePressed, animDelta);
        boolean waterFired = waterInteraction.update(waterPromptVisible,
            waterAvailable && spacePressed, animDelta);
        boolean benchFired = benchInteraction.update(benchPromptVisible,
            benchAvailable && spacePressed, animDelta);
        boolean skillFired = skillInteraction.update(skillPromptVisible,
            skillAvailable && spacePressed, animDelta);
        boolean computerFired = computerInteraction.update(computerPromptVisible,
            computerAvailable && spacePressed, animDelta);
        if (benchFired) openUpgradePanel();
        // 스킬 테이블 SPACE → 스킬 장착/단축키 등록 창(패널을 연 입력이 창 안으로 새지 않음).
        if (skillFired) openSkillPanel();
        // 메인 컴퓨터 SPACE → 보스 단계 선택창(열람만으로는 runId/보상 없음).
        if (computerFired) openBossStagePanel();
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
            animDelta, vendingAvailable, promptIsWater, benchPromptVisible, skillPromptVisible,
            computerPromptVisible,
            computerPromptVisible ? computerInteraction.promptFrame(true)
                : skillPromptVisible ? skillInteraction.promptFrame(true)
                : benchPromptVisible ? benchInteraction.promptFrame(true)
                : promptIsWater ? waterInteraction.promptFrame(waterPromptVisible)
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
        skillMenuButton.render(assets.skills().slotNormal(), assets.skills().iconHeal(),
            hudLayout.marginX + hudLayout.frameW + 14f + 64f + 10f,
            hudLayout.marginY + (hudLayout.frameH - 64f) / 2f, 64f);
        salePanel.render();
        upgradePanel.render();
        skillPanel.render();
        bossStagePanel.render();
        renderFade(batch, viewport);
        if (debug) debugRenderer.render(camera, room, player);
        if (testSale) runSaleTestScript();
        if (testUpgrade) runUpgradeTestScript();
        if (testSkills) runSkillsTestScript();
        if (testBossPanel) {
            testBossFrame++;
            if (testBossFrame == 10)
                io.github.some_example_name.render.FrameCapture.capture("boss_prompt_computer");
            if (testBossFrame == 20) openBossStagePanel();
            if (testBossFrame == 40)
                io.github.some_example_name.render.FrameCapture.capture("boss_stage_panel");
            if (testBossFrame == 45) bossStagePanel.selectStage("1-보스"); // 잠김+한글 ID 상세
            if (testBossFrame == 60)
                io.github.some_example_name.render.FrameCapture.capture("boss_stage_panel_locked");
            if (testBossFrame == 75) {
                Gdx.app.log("BossPanel", "unlocked=" + game.economy().profile().bossUnlockedStages
                    + " cleared=" + game.economy().profile().bossClearedStages);
                Gdx.app.exit();
            }
        }
    }

    /** 개발 전용 실행 검증: 스킬 패널 → 회복 선택+슬롯 선택 → 키 등록 대기 상태 → 캡처 → 종료. */
    private void runSkillsTestScript() {
        testSkillsFrame++;
        // LAB_HQ_TEST=SKILL/BENCH 스폰과 조합: 패널을 열기 전 가구 SPACE 안내 실프레임 캡처.
        if (testSkillsFrame == 10 && System.getenv("LAB_HQ_TEST") != null)
            io.github.some_example_name.render.FrameCapture.capture(
                "hq_prompt_" + System.getenv("LAB_HQ_TEST").toLowerCase());
        if (testSkillsFrame == 20) {
            openSkillPanel();
            skillPanel.selectSkill(io.github.some_example_name.profile.SkillLoadout.HEAL);
            skillPanel.selectSlot(1);
        }
        if (testSkillsFrame == 40)
            io.github.some_example_name.render.FrameCapture.capture("hq_skill_panel");
        if (testSkillsFrame == 50) skillPanel.beginKeyCapture();
        if (testSkillsFrame == 60)
            io.github.some_example_name.render.FrameCapture.capture("hq_skill_keywait");
        if (testSkillsFrame == 70) {
            io.github.some_example_name.profile.SkillLoadout loadout = game.skillLoadout();
            Gdx.app.log("SkillsTest", "slots=" + loadout.slotsForSave()
                + " keys=" + loadout.keysForSave()
                + " capacity=" + game.activeSkillSlots()
                + " unlocked=" + game.unlockedSkills());
            Gdx.app.exit();
        }
    }

    /** 개발 전용 실행 검증: 표본 판매로 재화 확보 → 강화창 → hp_1/heal_unlock 구매 → 캡처 → 종료. */
    private void runUpgradeTestScript() {
        testUpgradeFrame++;
        io.github.some_example_name.profile.HeadquartersEconomy economy = game.economy();
        if (testUpgradeFrame == 15 && economy.currencyWon() < 6000) {
            economy.transferReturn("up-ret-" + System.nanoTime(), java.util.Arrays.asList(
                "research_log_booklet", "cracked_reagent_bottle", "lantern"));
            economy.sellAll("up-tx-" + System.nanoTime());
            Gdx.app.log("UpgradeTest", "funded money=" + economy.currencyWon());
        }
        if (testUpgradeFrame == 25) {
            openUpgradePanel();
            upgradePanel.selectNode("hp_1");
        }
        if (testUpgradeFrame == 40)
            io.github.some_example_name.render.FrameCapture.capture("hq_upgrade_tree");
        if (testUpgradeFrame == 50) upgradePanel.purchaseSelected();
        if (testUpgradeFrame == 60) {
            upgradePanel.selectNode("heal_unlock");
            Gdx.app.log("UpgradeTest", "after hp_1 money=" + economy.currencyWon()
                + " maxHp=" + player.maxHp() + "/" + player.hp());
        }
        if (testUpgradeFrame == 70) upgradePanel.purchaseSelected();
        if (testUpgradeFrame == 85)
            io.github.some_example_name.render.FrameCapture.capture("hq_upgrade_purchased");
        if (testUpgradeFrame == 95) {
            Gdx.app.log("UpgradeTest", "final money=" + economy.currencyWon()
                + " maxHp=" + player.maxHp()
                + " healEquipped=" + game.healSkillEquipped()
                + " healAmount=" + game.healSkillAmount()
                + " slots=" + game.activeSkillSlots()
                + " purchased=" + economy.profile().purchasedNodeIds());
            Gdx.app.exit();
        }
    }

    /**
     * 강화 패널 열기(Space 1회 = 열기만 — 같은 입력이 패널 안 구매로 이어지지 않는다.
     * 다음 프레임부터 패널 분기가 입력을 차단·소비한다). 능력치는 공용 파생 모델에서 읽는다.
     */
    private void openUpgradePanel() {
        upgradePanel.open(game.economy(), game.upgradeCatalog(),
            game.config().baseActiveSkillSlots,
            new io.github.some_example_name.render.UpgradeTreeRenderer.StatsProvider() {
                @Override public int maxHp() { return player.maxHp(); }
                @Override public int attackDamage() { return game.config().playerAttackDamage; }
                @Override public int healAmount() { return game.healSkillAmount(); }
                @Override public int activeSlots() { return game.activeSkillSlots(); }
                @Override public boolean healUnlocked() {
                    return game.currentUpgradeEffects().healUnlocked();
                }
                @Override public int skillDamage(String skillId) { return game.skillDamage(skillId); }
                @Override public int shieldAbsorb() { return game.shieldAbsorbAmount(); }
                @Override public boolean skillUnlocked(String skillId) {
                    return game.unlockedSkills().contains(skillId);
                }
            },
            new Runnable() { @Override public void run() { game.applyUpgradeEffects(); } });
    }

    /**
     * 스킬 패널 열기: 구성(SkillLoadout)은 게임 루트가 소유한 단일 인스턴스를 그대로 쓰고,
     * 변경 성공 시에만 기존 원자적 프로필 저장 흐름(saveSkillLoadout)으로 저장한다.
     */
    private void openSkillPanel() {
        skillPanel.open(game.skillLoadout(), game.activeSkillSlots(), game.unlockedSkills(),
            KeyBindings.reservedKeys(),
            new io.github.some_example_name.render.SkillLoadoutRenderer.SkillInfo() {
                @Override public int healAmount() { return game.healSkillAmount(); }
                @Override public float cooldownSeconds(String skillId) {
                    return game.skillCooldownSeconds(skillId);
                }
                @Override public int skillDamage(String skillId) { return game.skillDamage(skillId); }
                @Override public int shieldAbsorb() { return game.shieldAbsorbAmount(); }
            },
            new Runnable() { @Override public void run() { game.saveSkillLoadout(); } });
    }

    /**
     * 보스 단계 선택창 열기(메인 컴퓨터 SPACE). 진행/클리어는 영속 프로필에서 읽고,
     * 입장 확정 시에만 game.startBossRun이 새 runId를 발급한다.
     */
    private void openBossStagePanel() {
        bossStagePanel.open(game.bossStageCatalog(),
            game.bossUnlockedView(),
            game.economy().profile().bossClearedStages,
            new io.github.some_example_name.render.BossStageSelectRenderer.Listener() {
                @Override public void onEnterStage(String stageId) {
                    game.startBossRun(stageId);
                }
            });
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
        upgradePanel.dispose();
        skillPanel.dispose();
        skillMenuButton.dispose();
        bossStagePanel.dispose();
    }
}

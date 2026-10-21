package io.github.some_example_name.asset;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import io.github.some_example_name.config.AssetPaths;

/**
 * 보스 1-1 텍스처(boss_stage_1_1_v1). Nearest, GameAssets 소유·해제.
 * 몸체 셀 192×192(앵커 96,160 — 발), VFX 셀 128×128, 프레임별 trim/크기 변경 없음.
 * attack 시트는 상태별 세그먼트(예고 0-1/발사 2-3/회복 4-5/돌진 6-7)로만 재생한다.
 */
public final class BossAssets implements Disposable {

    private final Texture idleTex, attackTex, deathTex, projectileTex, acidTex, impactTex,
        circleTex, laneTex, floorTex, pillarTex, barrierTex,
        gateLockedTex, gateClosedTex, gateOpenTex,
        hpFrameTex, hpFillTex, emblemTex, stagePanelTex, stageButtonTex,
        stageLockedTex, stageReadyTex, rewardFrameTex, rewardGlowTex;
    private final TextureRegion[] idle = new TextureRegion[4];
    private final TextureRegion[] attack = new TextureRegion[8];
    private final TextureRegion[] death = new TextureRegion[6];
    private final TextureRegion[] projectile = new TextureRegion[4];
    private final TextureRegion[] acid = new TextureRegion[6];
    private final TextureRegion[] impact = new TextureRegion[6];
    private final TextureRegion telegraphCircle, telegraphLane, floor, pillar, barrier,
        gateLocked, gateClosed, gateOpen, hpFrame, hpFill, emblem,
        stagePanel, stageButton, stageLocked, stageReady, rewardFrame, rewardGlow;

    public BossAssets() {
        idleTex = GameAssets.loadTile(AssetPaths.BOSS_IDLE);
        attackTex = GameAssets.loadTile(AssetPaths.BOSS_ATTACK);
        deathTex = GameAssets.loadTile(AssetPaths.BOSS_DEATH);
        projectileTex = GameAssets.loadTile(AssetPaths.BOSS_PROJECTILE);
        acidTex = GameAssets.loadTile(AssetPaths.BOSS_ACID_POOL);
        impactTex = GameAssets.loadTile(AssetPaths.BOSS_IMPACT);
        circleTex = GameAssets.loadTile(AssetPaths.BOSS_TELEGRAPH_CIRCLE);
        laneTex = GameAssets.loadTile(AssetPaths.BOSS_TELEGRAPH_LANE);
        floorTex = GameAssets.loadTile(AssetPaths.BOSS_ARENA_FLOOR);
        pillarTex = GameAssets.loadTile(AssetPaths.BOSS_ARENA_PILLAR);
        barrierTex = GameAssets.loadTile(AssetPaths.BOSS_ARENA_BARRIER);
        gateLockedTex = GameAssets.loadTile(AssetPaths.BOSS_GATE_LOCKED);
        gateClosedTex = GameAssets.loadTile(AssetPaths.BOSS_GATE_CLOSED);
        gateOpenTex = GameAssets.loadTile(AssetPaths.BOSS_GATE_OPEN);
        hpFrameTex = GameAssets.loadTile(AssetPaths.BOSS_HP_FRAME);
        hpFillTex = GameAssets.loadTile(AssetPaths.BOSS_HP_FILL);
        emblemTex = GameAssets.loadTile(AssetPaths.BOSS_EMBLEM);
        stagePanelTex = GameAssets.loadTile(AssetPaths.BOSS_STAGE_PANEL);
        stageButtonTex = GameAssets.loadTile(AssetPaths.BOSS_STAGE_BUTTON);
        stageLockedTex = GameAssets.loadTile(AssetPaths.BOSS_STAGE_LOCKED);
        stageReadyTex = GameAssets.loadTile(AssetPaths.BOSS_STAGE_READY);
        rewardFrameTex = GameAssets.loadTile(AssetPaths.BOSS_REWARD_FRAME);
        rewardGlowTex = GameAssets.loadTile(AssetPaths.BOSS_REWARD_GLOW);
        split(idleTex, 2, 192, idle);
        split(attackTex, 4, 192, attack);
        split(deathTex, 3, 192, death);
        split(projectileTex, 2, 128, projectile);
        split(acidTex, 3, 128, acid);
        split(impactTex, 3, 128, impact);
        telegraphCircle = new TextureRegion(circleTex);
        telegraphLane = new TextureRegion(laneTex);
        floor = new TextureRegion(floorTex);
        pillar = new TextureRegion(pillarTex);
        barrier = new TextureRegion(barrierTex);
        gateLocked = new TextureRegion(gateLockedTex);
        gateClosed = new TextureRegion(gateClosedTex);
        gateOpen = new TextureRegion(gateOpenTex);
        hpFrame = new TextureRegion(hpFrameTex);
        hpFill = new TextureRegion(hpFillTex);
        emblem = new TextureRegion(emblemTex);
        stagePanel = new TextureRegion(stagePanelTex);
        stageButton = new TextureRegion(stageButtonTex);
        stageLocked = new TextureRegion(stageLockedTex);
        stageReady = new TextureRegion(stageReadyTex);
        rewardFrame = new TextureRegion(rewardFrameTex);
        rewardGlow = new TextureRegion(rewardGlowTex);
    }

    private static void split(Texture sheet, int columns, int cell, TextureRegion[] out) {
        TextureRegion[][] grid = TextureRegion.split(sheet, cell, cell);
        for (int i = 0; i < out.length; i++) out[i] = grid[i / columns][i % columns];
    }

    private static TextureRegion frame(TextureRegion[] frames, int index) {
        return frames[Math.max(0, Math.min(frames.length - 1, index))];
    }
    public TextureRegion idleFrame(int i) { return frame(idle, i); }
    public TextureRegion attackFrame(int i) { return frame(attack, i); }
    public TextureRegion deathFrame(int i) { return frame(death, i); }
    public TextureRegion projectileFrame(int i) { return frame(projectile, i); }
    public TextureRegion acidFrame(int i) { return frame(acid, i); }
    public TextureRegion impactFrame(int i) { return frame(impact, i); }
    public TextureRegion telegraphCircle() { return telegraphCircle; }
    public TextureRegion telegraphLane() { return telegraphLane; }
    public TextureRegion floor() { return floor; }
    public TextureRegion pillar() { return pillar; }
    public TextureRegion barrier() { return barrier; }
    public TextureRegion gate(boolean locked, boolean open) {
        return open ? gateOpen : locked ? gateLocked : gateClosed;
    }
    public TextureRegion hpFrame() { return hpFrame; }
    public TextureRegion hpFill() { return hpFill; }
    public TextureRegion emblem() { return emblem; }
    public TextureRegion stagePanel() { return stagePanel; }
    public TextureRegion stageButton() { return stageButton; }
    public TextureRegion stageLocked() { return stageLocked; }
    public TextureRegion stageReady() { return stageReady; }
    public TextureRegion rewardFrame() { return rewardFrame; }
    public TextureRegion rewardGlow() { return rewardGlow; }

    @Override public void dispose() {
        idleTex.dispose(); attackTex.dispose(); deathTex.dispose(); projectileTex.dispose();
        acidTex.dispose(); impactTex.dispose(); circleTex.dispose(); laneTex.dispose();
        floorTex.dispose(); pillarTex.dispose(); barrierTex.dispose();
        gateLockedTex.dispose(); gateClosedTex.dispose(); gateOpenTex.dispose();
        hpFrameTex.dispose(); hpFillTex.dispose(); emblemTex.dispose();
        stagePanelTex.dispose(); stageButtonTex.dispose(); stageLockedTex.dispose();
        stageReadyTex.dispose(); rewardFrameTex.dispose(); rewardGlowTex.dispose();
    }
}

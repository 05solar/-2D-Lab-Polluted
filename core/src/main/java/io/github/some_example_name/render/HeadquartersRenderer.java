package io.github.some_example_name.render;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import io.github.some_example_name.asset.HeadquartersAssets;
import io.github.some_example_name.entity.player.Player;
import io.github.some_example_name.world.HeadquartersProp;
import io.github.some_example_name.world.HeadquartersVisuals;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Draws HQ tile layers, then depth-sorts props and player by bottom anchor. */
public final class HeadquartersRenderer {
    private final HeadquartersAssets assets;
    private final List<HeadquartersProp> depthOrder;
    private final HeadquartersProp vending;
    private final HeadquartersProp waterDispenser;
    // 2026-10-20: 강화 트리 = 접이식 작업대(망치), 스킬 창 = 북벽 금속 테이블(skill_table).
    private HeadquartersProp upgradeBench;
    private HeadquartersProp skillTable;
    private HeadquartersProp mainComputer; // 터미널 책상 = 보스 단계 선택
    // 자판기 위 판매 표식(장식 — 충돌 영역 없음). null이면 그리지 않는다.
    private final com.badlogic.gdx.graphics.g2d.TextureRegion saleSign;
    // 스킬 테이블(hq_upgrade_tree_v1 북벽 가구) 아트 — HQ 텐트 팩 밖의 텍스처라 별도 주입.
    private final com.badlogic.gdx.graphics.g2d.TextureRegion benchArt;
    private float vendingTime;

    public HeadquartersRenderer(HeadquartersAssets assets, HeadquartersVisuals visuals) {
        this(assets, visuals, null, null);
    }

    public HeadquartersRenderer(HeadquartersAssets assets, HeadquartersVisuals visuals,
                                com.badlogic.gdx.graphics.g2d.TextureRegion saleSign,
                                com.badlogic.gdx.graphics.g2d.TextureRegion benchArt) {
        this.assets = assets;
        this.saleSign = saleSign;
        this.benchArt = benchArt;
        depthOrder = new ArrayList<>(visuals.props());
        HeadquartersProp machine = null;
        HeadquartersProp dispenser = null;
        for (HeadquartersProp prop : depthOrder)
            if (prop.id().equals("vending_machine")) machine = prop;
            else if (prop.id().equals("water_dispenser")) dispenser = prop;
            else if (prop.id().equals("folding_workbench")) upgradeBench = prop;
            else if (prop.id().equals("skill_table")) skillTable = prop;
            else if (prop.id().equals("terminal_desk")) mainComputer = prop;
        vending = machine;
        waterDispenser = dispenser;
        Collections.sort(depthOrder, new Comparator<HeadquartersProp>() {
            @Override public int compare(HeadquartersProp a, HeadquartersProp b) {
                return Float.compare(b.anchorY(), a.anchorY());
            }
        });
    }

    public void render(SpriteBatch batch, HeadquartersVisuals visuals, Player player,
                       EntityRenderer playerRenderer, float animDelta, boolean vendingNearby,
                       boolean waterDispenserNearby, boolean benchNearby, boolean skillTableNearby,
                       boolean computerNearby, int promptFrame, boolean vendingAccepted) {
        batch.enableBlending();
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        for (int y = 0; y < visuals.height(); y++) {
            for (int x = 0; x < visuals.width(); x++) {
                String id = visuals.floorAt(x, y);
                if (id != null) batch.draw(assets.tile(id), x, y, 1f, 1f);
            }
        }
        for (int y = 0; y < visuals.height(); y++) {
            for (int x = 0; x < visuals.width(); x++) {
                String id = visuals.decorAt(x, y);
                if (id != null) batch.draw(assets.tile(id), x, y, 1f, 1f);
            }
        }
        for (int y = 0; y < visuals.height(); y++) {
            for (int x = 0; x < visuals.width(); x++) {
                String id = visuals.wallAt(x, y);
                if (id != null) batch.draw(assets.tile(id), x, y, 1f, 1f);
            }
        }

        vendingTime += animDelta;
        boolean playerDrawn = false;
        for (HeadquartersProp prop : depthOrder) {
            if (!playerDrawn && player.feetY() >= prop.anchorY()) {
                playerRenderer.render(batch, player, animDelta);
                playerDrawn = true;
            }
            float drawX = prop.anchorX() - prop.drawWidth() / 2f;
            if (prop.id().equals("vending_machine")) {
                String frame = vendingAccepted ? "purchase_accepted"
                    : vendingNearby ? "interact_highlight"
                    : ((int) (vendingTime / assets.idleFrameDuration()) % 2 == 0
                        ? "idle_dim" : "idle_pulse");
                batch.draw(assets.vending(frame), drawX, prop.anchorY(),
                    prop.drawWidth(), prop.drawHeight());
            } else if (prop.id().equals("skill_table")) {
                if (benchArt != null)
                    batch.draw(benchArt, drawX, prop.anchorY(),
                        prop.drawWidth(), prop.drawHeight());
            } else {
                batch.draw(assets.furniture(prop.id()), drawX, prop.anchorY(),
                    prop.drawWidth(), prop.drawHeight());
            }
        }
        if (!playerDrawn) playerRenderer.render(batch, player, animDelta);

        // 판매 표식: 자판기 상단 장식(충돌 없음). 안내 프롬프트보다 아래 레이어.
        if (saleSign != null && vending != null)
            batch.draw(saleSign, vending.anchorX() - 0.42f,
                vending.anchorY() + vending.drawHeight() - 0.12f, 0.84f, 0.84f);

        // Draw the keycap and Korean prompt above all props so furniture never hides them.
        if (vending != null && promptFrame >= 0) {
            boolean computerPrompt = computerNearby && mainComputer != null;
            boolean skillPrompt = !computerPrompt && skillTableNearby && skillTable != null;
            boolean benchPrompt = !computerPrompt && !skillPrompt
                && benchNearby && upgradeBench != null;
            HeadquartersProp target = computerPrompt ? mainComputer
                : skillPrompt ? skillTable
                : benchPrompt ? upgradeBench
                : waterDispenserNearby ? waterDispenser : vending;
            float promptCenterX = target.anchorX();
            // 물통(1.5)은 자판기(2.0)보다 낮아 프롬프트를 바짝 붙인다(-0.28).
            // 북벽 줄 가구(스킬 테이블·작업대·메인 컴퓨터)는 상판 위로 내려 그려야
            // 상단 HUD(화면 좌표) 줄에 가려지지 않는다(-1.35).
            float promptYOffset = computerPrompt || skillPrompt || benchPrompt ? -1.35f
                : target == vending ? 0f : -0.28f;
            batch.draw(assets.promptFrame(promptFrame), promptCenterX - 0.95f,
                target.anchorY() + 1.58f + promptYOffset, 1.9f, 0.95f);
            batch.draw(computerPrompt ? assets.bossPromptLabel()
                    : skillPrompt ? assets.skillPromptLabel()
                    : benchPrompt ? assets.upgradePromptLabel()
                    : waterDispenserNearby ? assets.healthPromptLabel() : assets.promptLabel(),
                promptCenterX - 0.58f, target.anchorY() + 2.23f + promptYOffset, 1.16f, 0.33f);
        }
    }
}

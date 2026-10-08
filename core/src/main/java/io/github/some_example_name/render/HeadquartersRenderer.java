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
    private float vendingTime;

    public HeadquartersRenderer(HeadquartersAssets assets, HeadquartersVisuals visuals) {
        this.assets = assets;
        depthOrder = new ArrayList<>(visuals.props());
        HeadquartersProp machine = null;
        HeadquartersProp dispenser = null;
        for (HeadquartersProp prop : depthOrder)
            if (prop.id().equals("vending_machine")) machine = prop;
            else if (prop.id().equals("water_dispenser")) dispenser = prop;
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
                       boolean waterDispenserNearby, int promptFrame, boolean vendingAccepted) {
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
            } else {
                batch.draw(assets.furniture(prop.id()), drawX, prop.anchorY(),
                    prop.drawWidth(), prop.drawHeight());
            }
        }
        if (!playerDrawn) playerRenderer.render(batch, player, animDelta);

        // Draw the keycap and Korean prompt above all props so furniture never hides them.
        if (vending != null && promptFrame >= 0) {
            HeadquartersProp target = waterDispenserNearby ? waterDispenser : vending;
            float promptCenterX = target.anchorX();
            // 물통은 자판기(2.0 높이)보다 낮아(1.5), 프롬프트를 물통 상단에 바짝 붙여 중앙에 띄운다.
            float promptYOffset = waterDispenserNearby ? -0.16f : 0f;
            batch.draw(assets.promptFrame(promptFrame), promptCenterX - 0.95f,
                target.anchorY() + 1.58f + promptYOffset, 1.9f, 0.95f);
            batch.draw(waterDispenserNearby ? assets.healthPromptLabel() : assets.promptLabel(),
                promptCenterX - 0.58f, target.anchorY() + 2.23f + promptYOffset, 1.16f, 0.33f);
        }
    }
}

package io.github.some_example_name.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import io.github.some_example_name.combat.AttackHitbox;
import io.github.some_example_name.combat.MonsterType;
import io.github.some_example_name.combat.MonsterState;
import io.github.some_example_name.combat.PlayerAttackState;
import io.github.some_example_name.combat.PlayerCombatState;
import io.github.some_example_name.config.BalanceConfig;
import io.github.some_example_name.entity.player.Player;
import io.github.some_example_name.entity.monster.Monster;
import io.github.some_example_name.world.LaboratoryRoom;
import java.util.List;

/** World-space monster bars and slash feedback plus a screen-locked player health HUD. */
public final class CombatRenderer {
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont font = new BitmapFont();
    private final SpriteBatch debugBatch = new SpriteBatch();
    private final Matrix4 screen = new Matrix4();
    private final Rectangle attackBox = new Rectangle();

    public void renderWorld(OrthographicCamera camera, List<Monster> monsters, Player player,
                            PlayerAttackState attack, BalanceConfig config) {
        shapes.setProjectionMatrix(camera.combined);
        shapes.setColor(Color.BLACK);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (Monster monster : monsters) {
            if (!monster.isAlive()) continue;
            float width = 0.82f, x = monster.feetX() - width / 2f, y = monster.feetY() + 1.04f;
            shapes.setColor(0f, 0f, 0f, 0.9f);
            shapes.rect(x - 0.025f, y - 0.025f, width + 0.05f, 0.12f);
            shapes.setColor(0.13f, 0.10f, 0.12f, 1f);
            shapes.rect(x, y, width, 0.07f);
            if (monster.type() == MonsterType.TEAM_LEADER) shapes.setColor(0.7f, 0.24f, 0.9f, 1f);
            else shapes.setColor(0.88f, 0.18f, 0.2f, 1f);
            shapes.rect(x, y, width * monster.health().ratio(), 0.07f);
        }
        shapes.end();
        if (attack.isAttacking() && attack.phase() == PlayerAttackState.Phase.ACTIVE) {
            AttackHitbox.set(player.bounds(), attack.direction(), config.playerAttackRange,
                config.playerAttackWidth, attackBox);
            shapes.setProjectionMatrix(camera.combined);
            shapes.setColor(1f, 0.82f, 0.2f, 0.85f);
            shapes.begin(ShapeRenderer.ShapeType.Line);
            float cx = attackBox.x + attackBox.width / 2f;
            float cy = attackBox.y + attackBox.height / 2f;
            float half = Math.min(attackBox.width, attackBox.height) * 0.35f;
            shapes.line(cx - half, cy - half, cx + half, cy + half);
            shapes.line(cx - half * 0.65f, cy - half * 0.65f,
                cx + half * 0.65f, cy + half * 0.65f);
            shapes.circle(cx, cy, 0.055f, 8);
            shapes.end();
        }
    }

    public void renderHud(SpriteBatch batch, Player player) {
        int width = Gdx.graphics.getWidth(), height = Gdx.graphics.getHeight();
        screen.setToOrtho2D(0f, 0f, width, height);
        shapes.setProjectionMatrix(screen);
        float x = 18f, y = height - 30f, barW = Math.min(250f, width * 0.28f), barH = 16f;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.02f, 0.03f, 0.04f, 0.78f);
        shapes.rect(x - 10f, y - 27f, barW + 20f, 48f);
        shapes.setColor(0f, 0f, 0f, 1f);
        shapes.rect(x - 2f, y - 2f, barW + 4f, barH + 4f);
        shapes.setColor(0.12f, 0.13f, 0.14f, 1f);
        shapes.rect(x, y, barW, barH);
        if (player.hp() <= 30) shapes.setColor(0.9f, 0.22f, 0.12f, 1f);
        else shapes.setColor(0.22f, 0.82f, 0.35f, 1f);
        shapes.rect(x, y, barW * Math.max(0f, Math.min(1f, player.hp() / (float) player.maxHp())), barH);
        shapes.end();
        batch.setProjectionMatrix(screen);
        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "HP " + player.hp() + " / " + player.maxHp(), x, y + 25f);
        batch.end();
    }

    public void renderDebug(OrthographicCamera camera, Player player, List<Monster> monsters,
                           PlayerAttackState attack, BalanceConfig config,
                           LaboratoryRoom room) {
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(Color.GREEN);
        shapes.rect(player.bounds().x, player.bounds().y, player.bounds().width, player.bounds().height);
        if (attack.isAttacking() && attack.phase() == PlayerAttackState.Phase.ACTIVE) {
            AttackHitbox.set(player.bounds(), attack.direction(), config.playerAttackRange,
                config.playerAttackWidth, attackBox);
            shapes.setColor(Color.YELLOW);
            shapes.rect(attackBox.x, attackBox.y, attackBox.width, attackBox.height);
        }
        for (Monster monster : monsters) {
            if (!monster.isAlive()) continue;
            shapes.setColor(Color.RED);
            shapes.rect(monster.bounds().x, monster.bounds().y,
                monster.bounds().width, monster.bounds().height);
            if (monster.type() == MonsterType.TEAM_LEADER && !monster.enraged()) {
                shapes.setColor(Color.MAGENTA);
                float r = config.teamLeaderHearingRadius;
                shapes.circle(monster.feetX(), monster.feetY(), r, 40);
            } else {
                shapes.setColor(monster.state() == MonsterState.CHASE ? Color.ORANGE : Color.CYAN);
                float r = detectionRadius(monster.type(), config);
                shapes.circle(monster.feetX(), monster.feetY(), r, 32);
            }
            if (monster.state() == MonsterState.ATTACK) {
                shapes.setColor(Color.YELLOW);
                if (monster.type() == MonsterType.TEAM_LEADER) {
                    shapes.circle(monster.feetX(), monster.feetY(),
                        config.teamLeaderAttackRange, 32);
                } else {
                    AttackHitbox.set(monster.bounds(), monster.facing(), config.monsterAttackRange,
                        config.playerAttackWidth, attackBox);
                    shapes.rect(attackBox.x, attackBox.y, attackBox.width, attackBox.height);
                }
            }
        }
        shapes.end();
        debugBatch.setProjectionMatrix(camera.combined);
        debugBatch.begin();
        font.setColor(Color.WHITE);
        for (Monster monster : monsters) {
            if (monster.isAlive()) {
                font.draw(debugBatch, monster.type().name() + " " + monster.state()
                    + " HP " + monster.health().hp() + "/" + monster.health().maxHp(),
                    monster.feetX() - 0.55f, monster.feetY() + 1.18f);
            }
        }
        font.draw(debugBatch, "Attack " + attack.phase() + " frame " + attack.frameIndex(),
            player.feetX() - 0.7f, player.feetY() + 1.35f);
        debugBatch.end();
    }

    private static float detectionRadius(MonsterType type, BalanceConfig config) {
        switch (type) {
            case SLIME: return config.slimeDetectionRadius;
            case RESEARCHER: return config.researcherDetectionRadius;
            case GUARD: return config.guardDetectionRadius;
            default: return config.teamLeaderHearingRadius;
        }
    }

    public void dispose() { shapes.dispose(); font.dispose(); debugBatch.dispose(); }
}

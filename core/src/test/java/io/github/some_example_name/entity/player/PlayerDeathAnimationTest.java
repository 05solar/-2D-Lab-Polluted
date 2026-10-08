package io.github.some_example_name.entity.player;

import static org.junit.Assert.*;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.github.some_example_name.combat.PlayerAttackState;
import io.github.some_example_name.combat.PlayerCombatState;
import io.github.some_example_name.entity.Direction;
import io.github.some_example_name.input.GameAction;
import io.github.some_example_name.input.InputState;
import io.github.some_example_name.input.PlayerController;
import io.github.some_example_name.collision.SolidGrid;
import io.github.some_example_name.collision.CollisionSystem;
import io.github.some_example_name.movement.MovementSystem;
import io.github.some_example_name.movement.RunStartDetector;
import io.github.some_example_name.movement.MovementMode;
import io.github.some_example_name.render.AnimationController;
import io.github.some_example_name.render.AnimationSet;
import java.util.EnumSet;
import org.junit.Test;

public class PlayerDeathAnimationTest {
    @Test public void lethalDamageClampsHpAndEntersDeadOnlyOnce() {
        Player player = new Player(4f, 5f, 0.6f, 0.5f, 1);
        player.setMoving(true);
        player.setMode(MovementMode.RUNNING);
        player.takeDamage(8);
        player.takeDamage(10);
        assertEquals(0, player.hp());
        assertEquals(PlayerState.DEAD, player.state());
        assertFalse(player.isMoving());
        assertEquals(MovementMode.WALKING, player.mode());
    }

    @Test public void animationUsesLastFacingAndDoesNotRestart() {
        for (Direction direction : Direction.values()) {
            PlayerDeathAnimation animation = new PlayerDeathAnimation();
            animation.start(direction);
            animation.update(0.35f);
            animation.start(Direction.DOWN);
            assertEquals("death_" + direction.name().toLowerCase(java.util.Locale.ROOT),
                animation.animationId());
            assertEquals(3, animation.frameIndex());
            assertEquals(0.35f, animation.elapsed(), 0.0001f);
        }
        PlayerDeathAnimation fallback = new PlayerDeathAnimation();
        fallback.start(null);
        assertEquals("death_down", fallback.animationId());
    }

    @Test public void animationAdvancesByDeltaAndHoldsFrameSeven() {
        PlayerDeathAnimation animation = new PlayerDeathAnimation();
        animation.start(Direction.LEFT);
        for (int i = 0; i < 8; i++) {
            animation.update(0.1f);
            assertEquals(Math.min(i + 1, 7), animation.frameIndex());
        }
        assertTrue(animation.finished());
        animation.update(5f);
        assertEquals(7, animation.frameIndex());
        assertEquals(0.8f, animation.elapsed(), 0.0001f);
    }

    @Test public void pausedDeltaDoesNotAdvanceDeathAnimation() {
        PlayerDeathAnimation animation = new PlayerDeathAnimation();
        animation.start(Direction.UP);
        animation.update(0.3f);
        animation.update(0f);
        assertEquals(0.3f, animation.elapsed(), 0.0001f);
        assertEquals(3, animation.frameIndex());
    }

    @Test public void deathFrameIsSelectedAndLastFrameRemainsRendered() {
        Player player = new Player(2f, 3f, 0.6f, 0.5f, 1);
        player.setFacing(Direction.RIGHT);
        player.takeDamage(1);
        AnimationSet set = new AnimationSet();
        TextureRegion[] frames = new TextureRegion[8];
        for (int i = 0; i < frames.length; i++) frames[i] = new TextureRegion();
        set.putAnimation("death_right", new Animation<>(0.1f, frames));
        AnimationController controller = new AnimationController(set);
        assertSame(frames[3], controller.currentFrame(player, 0.31f));
        assertSame(frames[7], controller.currentFrame(player, 0.8f));
        assertSame(frames[7], controller.currentFrame(player, 1f));
    }

    @Test public void deathCancelsAttackAndRejectsFurtherDamage() {
        Player player = new Player(1f, 1f, 0.6f, 0.5f, 5);
        PlayerAttackState attack = new PlayerAttackState(0.45f, 8, new int[]{2, 3, 4});
        assertTrue(attack.start(Direction.UP));
        assertTrue(attack.markHit(99));
        PlayerCombatState combat = new PlayerCombatState(attack);
        assertTrue(combat.takeDamage(player, 5, 1f, 1f, 0.6f));
        assertTrue(player.isDead());
        assertFalse(attack.isAttacking());
        assertEquals(0f, attack.cooldown(), 0f);
        assertTrue(attack.alreadyHitEntityIds().isEmpty());
        player.takeDamage(40);
        assertEquals(0, player.hp());
    }

    @Test public void deadPlayerCannotGenerateMovementOrAttackIntent() {
        Player player = new Player(1f, 1f, 0.6f, 0.5f, 1);
        player.takeDamage(1);
        InputState input = new InputState();
        input.update(EnumSet.of(GameAction.MOVE_UP, GameAction.RUN, GameAction.ATTACK,
            GameAction.INTERACT));
        io.github.some_example_name.input.PlayerIntent intent =
            new PlayerController().intentFrom(player, input);
        assertFalse(intent.hasMovement());
        assertFalse(intent.run);
        assertFalse(intent.attack);
        assertFalse(intent.interact);
    }

    @Test public void deadPlayerCannotMoveOrStartAnotherRunNoise() {
        Player player = new Player(4f, 4f, 0.6f, 0.5f, 1);
        RunStartDetector runs = new RunStartDetector();
        MovementSystem movement = new MovementSystem(new CollisionSystem(), runs, 3f, 6f);
        SolidGrid open = new SolidGrid() {
            @Override public boolean isSolid(int x, int y) {
                return x < 0 || y < 0 || x >= 10 || y >= 10;
            }
            @Override public int widthInTiles() { return 10; }
            @Override public int heightInTiles() { return 10; }
        };
        assertTrue(movement.update(player,
            new io.github.some_example_name.input.PlayerIntent(1f, 0f, true, false, false),
            open, 0.1f));
        float x = player.feetX();
        player.takeDamage(1);
        assertFalse(movement.update(player,
            new io.github.some_example_name.input.PlayerIntent(1f, 0f, true, false, false),
            open, 0.1f));
        assertEquals(x, player.feetX(), 0f);
        assertEquals(MovementMode.WALKING, player.mode());
        assertEquals(MovementMode.WALKING, runs.mode());
    }
}

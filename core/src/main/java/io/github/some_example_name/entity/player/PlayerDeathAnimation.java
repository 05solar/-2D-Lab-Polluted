package io.github.some_example_name.entity.player;

import io.github.some_example_name.entity.Direction;
import java.util.Locale;

/** Delta-driven, non-looping death timeline; the final frame remains selected. */
public final class PlayerDeathAnimation {
    public static final int FRAME_COUNT = 8;
    public static final float FRAME_DURATION = 0.1f;

    private Direction direction = Direction.DOWN;
    private float elapsed;
    private boolean started;

    public void start(Direction facing) {
        if (started) return;
        direction = facing == null ? Direction.DOWN : facing;
        elapsed = 0f;
        started = true;
    }

    public void update(float delta) {
        if (delta < 0f) throw new IllegalArgumentException("delta");
        if (started) elapsed = Math.min(FRAME_COUNT * FRAME_DURATION, elapsed + delta);
    }

    public Direction direction() { return direction; }
    public String animationId() { return "death_" + direction.name().toLowerCase(Locale.ROOT); }
    public float elapsed() { return elapsed; }
    public int frameIndex() {
        return started ? Math.min(FRAME_COUNT - 1, (int) (elapsed / FRAME_DURATION)) : 0;
    }
    public boolean started() { return started; }
    public boolean finished() { return started && elapsed >= FRAME_COUNT * FRAME_DURATION; }
}

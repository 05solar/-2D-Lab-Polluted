package io.github.some_example_name.combat;

import io.github.some_example_name.entity.Direction;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Attack clip timeline and per-swing hit de-duplication, independent of LibGDX. */
public final class PlayerAttackState {
    public enum Phase { READY, WINDUP, ACTIVE, RECOVERY }

    private final float duration;
    private final int frameCount;
    private final int[] hitFrames;
    private final Set<Long> alreadyHitEntityIds = new HashSet<>();
    private Phase phase = Phase.READY;
    private Direction direction = Direction.DOWN;
    private float elapsed;
    private float cooldown;
    private int frameIndex;

    public PlayerAttackState(float duration, int frameCount, int[] hitFrames) {
        if (duration <= 0f || frameCount <= 0 || hitFrames == null) throw new IllegalArgumentException();
        this.duration = duration;
        this.frameCount = frameCount;
        this.hitFrames = hitFrames.clone();
    }

    public boolean start(Direction facing) {
        if (phase != Phase.READY || cooldown > 0f || facing == null) return false;
        direction = facing;
        elapsed = 0f;
        frameIndex = 0;
        alreadyHitEntityIds.clear();
        phase = Phase.WINDUP;
        cooldown = duration;
        return true;
    }

    /** Cancels the current swing immediately, including its cooldown and target set. */
    public void cancel() {
        phase = Phase.READY;
        elapsed = 0f;
        cooldown = 0f;
        frameIndex = 0;
        alreadyHitEntityIds.clear();
    }

    public void update(float delta) {
        if (delta < 0f) throw new IllegalArgumentException("delta");
        cooldown = Math.max(0f, cooldown - delta);
        if (phase == Phase.READY) return;
        elapsed = Math.min(duration, elapsed + delta);
        frameIndex = Math.min(frameCount - 1, (int) (elapsed / (duration / frameCount)));
        if (elapsed >= duration) {
            phase = Phase.READY;
        } else if (isHitFrame(frameIndex)) {
            phase = Phase.ACTIVE;
        } else if (frameIndex < firstHitFrame()) {
            phase = Phase.WINDUP;
        } else {
            phase = Phase.RECOVERY;
        }
    }

    public boolean isHitFrame(int frame) {
        for (int hitFrame : hitFrames) if (hitFrame == frame) return true;
        return false;
    }
    private int firstHitFrame() {
        int first = Integer.MAX_VALUE;
        for (int frame : hitFrames) first = Math.min(first, frame);
        return first;
    }
    public boolean markHit(long entityId) { return alreadyHitEntityIds.add(entityId); }
    public boolean hasHit(long entityId) { return alreadyHitEntityIds.contains(entityId); }
    public Set<Long> alreadyHitEntityIds() { return Collections.unmodifiableSet(alreadyHitEntityIds); }
    public Phase phase() { return phase; }
    public Direction direction() { return direction; }
    public float elapsed() { return elapsed; }
    public int frameIndex() { return frameIndex; }
    public float cooldown() { return cooldown; }
    public boolean isAttacking() { return phase != Phase.READY; }
}

package io.github.some_example_name.world;

/** Frame-rate-independent, one-shot transition state. Contains no rendering or input API. */
public final class MapTransitionController {
    public enum State { IDLE, FADING_OUT, SWITCHING_MAP, FADING_IN }

    private final float fadeSeconds;
    private MapId current;
    private MapId target;
    private State state = State.IDLE;
    private float alpha;
    private int switchCount;

    public MapTransitionController(MapId initial, float fadeSeconds) {
        if (initial == null || fadeSeconds <= 0f) throw new IllegalArgumentException();
        this.current = initial;
        this.fadeSeconds = fadeSeconds;
    }

    public boolean request(MapId next) {
        if (next == null || next == current || state != State.IDLE) return false;
        target = next;
        alpha = 0f;
        state = State.FADING_OUT;
        return true;
    }

    public void update(float delta) {
        if (delta < 0f) throw new IllegalArgumentException("negative delta");
        if (state == State.FADING_OUT) {
            alpha = Math.min(1f, alpha + delta / fadeSeconds);
            if (alpha >= 1f) state = State.SWITCHING_MAP;
        } else if (state == State.FADING_IN) {
            alpha = Math.max(0f, alpha - delta / fadeSeconds);
            if (alpha <= 0f) {
                state = State.IDLE;
                target = null;
            }
        }
    }

    /** Only the game root calls this after replacing the screen and moving the shared player. */
    public void mapSwitched() {
        if (state != State.SWITCHING_MAP) throw new IllegalStateException("map not ready");
        current = target;
        switchCount++;
        state = State.FADING_IN;
    }

    public MapId current() { return current; }
    public MapId target() { return target; }
    public State state() { return state; }
    public float alpha() { return alpha; }
    public boolean inputLocked() { return state != State.IDLE; }
    public int switchCount() { return switchCount; }
}

package io.github.some_example_name.interaction;

/** One-shot SPACE interaction feedback; shop UI is intentionally not part of this pass. */
public final class VendingMachineInteraction {
    private static final float PRESSED_SECONDS = 0.13f;
    private static final float ACCEPTED_SECONDS = 0.24f;
    private float pressedRemaining;
    private float acceptedRemaining;
    private int eventCount;

    /** Returns true once for each eligible just-pressed interaction. */
    public boolean update(boolean available, boolean justPressed, float delta) {
        if (delta < 0f) throw new IllegalArgumentException("delta");
        pressedRemaining = Math.max(0f, pressedRemaining - delta);
        acceptedRemaining = Math.max(0f, acceptedRemaining - delta);
        if (!available) pressedRemaining = 0f;
        if (!available || !justPressed) return false;
        pressedRemaining = PRESSED_SECONDS;
        acceptedRemaining = ACCEPTED_SECONDS;
        eventCount++;
        return true;
    }

    public boolean promptVisible(boolean available) { return available; }
    public boolean isPressed() { return pressedRemaining > 0f; }
    public boolean showAcceptedFrame() { return acceptedRemaining > 0f; }
    public int promptFrame(boolean available) {
        if (!available) return -1;
        return isPressed() ? 2 : 1;
    }
    public int eventCount() { return eventCount; }
}

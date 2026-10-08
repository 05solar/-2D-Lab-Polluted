package io.github.some_example_name.world;

import static org.junit.Assert.*;

import org.junit.Test;

public class MapTransitionControllerTest {
    @Test public void fadesBeforeOneSwitchThenUnlocksAfterFadeIn() {
        MapTransitionController t = new MapTransitionController(MapId.HEADQUARTERS, 0.35f);
        assertTrue(t.request(MapId.LABORATORY));
        assertFalse(t.request(MapId.LABORATORY));
        assertTrue(t.inputLocked());
        t.update(0.34f);
        assertEquals(MapId.HEADQUARTERS, t.current());
        assertEquals(MapTransitionController.State.FADING_OUT, t.state());
        assertTrue(t.alpha() < 1f);
        assertEquals(0, t.switchCount());
        t.update(0.02f);
        assertEquals(MapTransitionController.State.SWITCHING_MAP, t.state());
        assertEquals(1f, t.alpha(), 0f);
        t.update(1f);
        assertEquals(0, t.switchCount());
        t.mapSwitched();
        assertEquals(1, t.switchCount());
        assertEquals(MapId.LABORATORY, t.current());
        assertEquals(MapTransitionController.State.FADING_IN, t.state());
        assertFalse(t.request(MapId.HEADQUARTERS));
        t.update(0.34f);
        assertTrue(t.inputLocked());
        t.update(0.02f);
        assertEquals(0f, t.alpha(), 0f);
        assertEquals(MapTransitionController.State.IDLE, t.state());
        assertFalse(t.inputLocked());
    }

    @Test(expected = IllegalStateException.class)
    public void mapCannotBeSwitchedTwice() {
        MapTransitionController t = new MapTransitionController(MapId.HEADQUARTERS, 0.35f);
        t.request(MapId.LABORATORY);
        t.update(0.35f);
        t.mapSwitched();
        t.mapSwitched();
    }

    @Test public void stateTimeIsFrameRateIndependent() {
        MapTransitionController a = new MapTransitionController(MapId.HEADQUARTERS, 0.35f);
        MapTransitionController b = new MapTransitionController(MapId.HEADQUARTERS, 0.35f);
        a.request(MapId.LABORATORY);
        b.request(MapId.LABORATORY);
        a.update(0.2f); a.update(0.15f);
        b.update(0.05f); b.update(0.05f); b.update(0.05f);
        b.update(0.05f); b.update(0.05f); b.update(0.05f); b.update(0.05f);
        assertEquals(a.alpha(), b.alpha(), 0.00001f);
        assertEquals(a.state(), b.state());
    }
}

package io.github.some_example_name.world;

import static org.junit.Assert.*;

import io.github.some_example_name.interaction.VendingMachineInteraction;
import org.junit.Test;

public class VendingMachineInteractionTest {
    @Test public void promptRequiresRangeAndPressFiresOneEvent() {
        VendingMachineInteraction interaction = new VendingMachineInteraction();
        assertEquals(-1, interaction.promptFrame(false));
        assertFalse(interaction.update(false, true, 0.016f));
        assertEquals(0, interaction.eventCount());

        assertEquals(1, interaction.promptFrame(true));
        assertTrue(interaction.update(true, true, 0.016f));
        assertEquals(2, interaction.promptFrame(true));
        assertTrue(interaction.showAcceptedFrame());
        assertFalse("held SPACE does not repeat; caller only sends just-pressed",
            interaction.update(true, false, 0.016f));
        assertEquals(1, interaction.eventCount());
        interaction.update(true, false, 0.3f);
        assertFalse(interaction.isPressed());
        assertFalse(interaction.showAcceptedFrame());
        interaction.update(false, false, 0f);
        assertEquals(-1, interaction.promptFrame(false));
    }

    @Test public void pausedAndDeadPlayersDoNotQualifyForVendingInteraction() {
        HeadquartersRoom room = HeadquartersLayout.create().room();
        io.github.some_example_name.entity.player.Player player =
            new io.github.some_example_name.entity.player.Player(11.45f, 6.3f, 0.6f, 0.5f, 100);
        player.setFacing(io.github.some_example_name.entity.Direction.UP);
        assertTrue(room.canInteractWithVending(player));
        player.takeDamage(100);
        assertFalse(room.canInteractWithVending(player));
    }
}

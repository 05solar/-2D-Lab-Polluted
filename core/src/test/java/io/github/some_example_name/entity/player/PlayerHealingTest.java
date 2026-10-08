package io.github.some_example_name.entity.player;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PlayerHealingTest {
    @Test public void healingRestoresLivingPlayerToMaximumWithoutRevivingDeadPlayer() {
        Player player = new Player(1f, 1f, 0.6f, 0.5f, 100);
        player.takeDamage(65);
        player.healToFullHealth();
        assertEquals(100, player.hp());

        player.takeDamage(100);
        player.healToFullHealth();
        assertEquals(0, player.hp());
        assertTrueDead(player);
    }

    private static void assertTrueDead(Player player) {
        org.junit.Assert.assertTrue(player.isDead());
    }
}

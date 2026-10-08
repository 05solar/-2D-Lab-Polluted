package io.github.some_example_name.world;

import static org.junit.Assert.*;

import io.github.some_example_name.entity.player.Player;
import org.junit.Test;

public class PlayerSessionStateTest {
    @Test public void playerHpItemsMoneyAndProgressSurviveMapPositionChange() {
        Player player = new Player(9.5f, 3.5f, 0.6f, 0.5f, 100);
        PlayerSessionState session = new PlayerSessionState(player);
        player.takeDamage(27);
        session.addItem("sample_01");
        session.addMoney(75);
        session.markProgress("hq_departed");
        float spawnX = LaboratoryLayout.testRoom().room().spawnPoint().x;
        float spawnY = LaboratoryLayout.testRoom().room().spawnPoint().y;
        player.bounds().setPosition(spawnX - player.bounds().width / 2f, spawnY);
        assertSame(player, session.player());
        assertEquals(73, session.hp());
        assertEquals(75L, session.money());
        assertEquals("sample_01", session.inventory().get(0));
        assertTrue(session.hasProgress("hq_departed"));
        assertEquals(spawnX, player.feetX(), 0.0001f);
        assertEquals(spawnY, player.feetY(), 0.0001f);
    }
}

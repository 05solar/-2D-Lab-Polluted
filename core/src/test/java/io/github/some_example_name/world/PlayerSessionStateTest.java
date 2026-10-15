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

    @Test public void deathLosesAllCarriedItemsButKeepsMoneyAndProgress() {
        Player player = new Player(5f, 5f, 0.6f, 0.5f, 100);
        PlayerSessionState session = new PlayerSessionState(player);
        session.addItem("sample_01");
        session.addItem("sample_02");
        session.addMoney(120);
        session.markProgress("hq_departed");
        player.takeDamage(100);
        assertTrue(player.isDead());
        // 사망 페널티: 들고 있던(미반납) 소지품만 모두 잃고, 반납 완료 재화·진행은 유지.
        session.loseAllItemsOnDeath();
        assertTrue(session.inventory().isEmpty());
        assertFalse(session.backpack().isFull());
        assertEquals(120L, session.money());
        assertTrue(session.hasProgress("hq_departed"));
    }

    @Test public void reviveRestoresFullHpAndClearsDeadStateOnce() {
        Player player = new Player(5f, 5f, 0.6f, 0.5f, 100);
        player.takeDamage(100);
        assertTrue(player.isDead());
        // 사망 중에는 회복이 거부된다(늦은 처치 보상 등).
        player.heal(20);
        assertEquals(0, player.hp());
        // 본부 재소환 시점의 부활: 상태 해제 + HP 최대.
        player.revive();
        assertFalse(player.isDead());
        assertEquals(100, player.hp());
        // 부활 후에는 다시 피해/회복이 정상 동작한다.
        player.takeDamage(30);
        assertEquals(70, player.hp());
        player.heal(20);
        assertEquals(90, player.hp());
    }
}

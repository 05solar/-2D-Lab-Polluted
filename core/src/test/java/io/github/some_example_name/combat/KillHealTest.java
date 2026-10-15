package io.github.some_example_name.combat;

import io.github.some_example_name.config.BalanceConfig;
import io.github.some_example_name.entity.monster.Monster;
import io.github.some_example_name.entity.player.Player;
import org.junit.Test;
import static org.junit.Assert.*;

/** 처치당 HP 20 회복: 일반 공격·돌진이 공유하는 applyPlayerStrike 경로를 검증한다. */
public class KillHealTest {

    private final BalanceConfig config = new BalanceConfig();

    private Player player(int damageTaken) {
        Player player = new Player(5f, 5f, config.playerBoundsWidth,
            config.playerBoundsHeight, config.playerMaxHp);
        if (damageTaken > 0) player.takeDamage(damageTaken);
        return player;
    }

    private Monster slime(long id, int hp) {
        return new Monster(id, MonsterType.SLIME, 6f, 5f,
            config.monsterBoundsWidth, config.monsterBoundsHeight, hp);
    }

    @Test public void killAtNinetyHpCapsAtMaximumHundred() {
        CombatSystem combat = new CombatSystem(config);
        Player player = player(10); // HP 90
        Monster monster = slime(1, 10); // 한 방(15)에 죽는다
        assertTrue(combat.applyPlayerStrike(player, monster, 1f, 0f));
        assertEquals("90 + 20은 최대 100으로 제한", 100, player.hp());
    }

    @Test public void killAtThirtyHpHealsToFifty() {
        CombatSystem combat = new CombatSystem(config);
        Player player = player(70); // HP 30
        Monster monster = slime(1, 10);
        assertTrue(combat.applyPlayerStrike(player, monster, 1f, 0f));
        assertEquals(50, player.hp());
    }

    @Test public void nonLethalHitDoesNotHeal() {
        CombatSystem combat = new CombatSystem(config);
        Player player = player(70); // HP 30
        Monster monster = slime(1, 60); // 한 방에 안 죽음
        assertFalse(combat.applyPlayerStrike(player, monster, 1f, 0f));
        assertEquals("처치가 아니면 회복 없음", 30, player.hp());
    }

    @Test public void multiKillHealsPerMonsterButClampsAtMax() {
        CombatSystem combat = new CombatSystem(config);
        Player player = player(50); // HP 50
        // 한 번에 세 마리 처치 → +60이지만 최대 100 제한.
        for (long id = 1; id <= 3; id++)
            assertTrue(combat.applyPlayerStrike(player, slime(id, 10), 1f, 0f));
        assertEquals(100, player.hp());
    }

    @Test public void alreadyDeadMonsterDoesNotRewardTwice() {
        CombatSystem combat = new CombatSystem(config);
        Player player = player(70); // HP 30
        Monster monster = slime(1, 10);
        assertTrue(combat.applyPlayerStrike(player, monster, 1f, 0f));   // 사망 전환 → +20
        assertFalse("사망 상태 재타격은 처치 이벤트가 아니다",
            combat.applyPlayerStrike(player, monster, 1f, 0f));
        assertEquals("중복 보상 없음", 50, player.hp());
    }

    @Test public void deadPlayerIsNotRevivedByLateKillReward() {
        CombatSystem combat = new CombatSystem(config);
        Player player = player(100); // 사망
        assertTrue(player.isDead());
        Monster monster = slime(1, 10);
        combat.applyPlayerStrike(player, monster, 1f, 0f);
        assertTrue("늦은 처치 보상으로 부활하지 않는다", player.isDead());
        assertEquals(0, player.hp());
    }

    @Test public void strikeListenerFiresOnlyOnRealDamage() {
        CombatSystem combat = new CombatSystem(config);
        final int[] hits = {0}, kills = {0};
        combat.setStrikeListener(new CombatSystem.StrikeListener() {
            @Override public void onMonsterStruck(Monster monster, boolean killed) {
                hits[0]++;
                if (killed) kills[0]++;
            }
        });
        Player player = player(0);
        Monster tough = slime(1, 60);
        combat.applyPlayerStrike(player, tough, 1f, 0f);   // 비처치 타격
        Monster weak = slime(2, 10);
        combat.applyPlayerStrike(player, weak, 1f, 0f);    // 처치 타격
        assertEquals(2, hits[0]);
        assertEquals(1, kills[0]);
    }
}

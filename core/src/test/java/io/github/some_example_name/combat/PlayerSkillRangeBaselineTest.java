package io.github.some_example_name.combat;

import io.github.some_example_name.config.BalanceConfig;
import io.github.some_example_name.profile.SkillCatalog;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * 플레이어 스킬 사거리/판정 기준값(9단계 보스 확장 전 기록 — 확장 후에도 그대로여야 한다).
 * 보스 크기는 보스 쪽 피격/충돌 데이터로만 키우고, 플레이어 스킬 수치는 절대 바꾸지 않는다.
 */
public class PlayerSkillRangeBaselineTest {

    @Test public void playerSkillRangesUnchangedByBossExpansion() {
        BalanceConfig config = new BalanceConfig();
        assertEquals("근접 공격 사거리", 0.9f, config.playerAttackRange, 0.0001f);
        assertEquals("돌진 거리", 3f, config.dashDistanceTiles, 0.0001f);
        assertEquals("돌진 시간", 0.18f, config.dashDurationSeconds, 0.0001f);
        assertEquals("돌진 쿨다운", 3f, config.dashCooldownSeconds, 0.0001f);
        SkillCatalog skills;
        try {
            skills = new SkillCatalog(new String(Files.readAllBytes(
                Paths.get("../assets/ui/player_three_skills_v1/data/skills.json")),
                StandardCharsets.UTF_8));
        } catch (java.io.IOException e) { throw new RuntimeException(e); }
        assertEquals("충격파 반경", 2.5f, skills.shockwaveRadius, 0.0001f);
        assertEquals("충격파 밀어내기", 0.8f, skills.shockwaveKnockback, 0.0001f);
        assertEquals("전기 투척 속도", 10f, skills.electricSpeed, 0.0001f);
        assertEquals("전기 투척 최대 사거리", 8f, skills.electricMaxDistance, 0.0001f);
        assertEquals("전기 기절", 0.5f, skills.electricStunSeconds, 0.0001f);
        assertEquals("보호막 흡수량", 60, skills.shieldAbsorbAmount);
        assertEquals("보호막 지속", 4f, skills.shieldDurationSeconds, 0.0001f);
    }
}

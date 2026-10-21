package io.github.some_example_name.boss;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * 9단계 카탈로그 계약: ID 사슬·기존 세이브 마이그레이션·1-1 보존·투기장 도달성(순수 Java).
 */
public class BossNineStagesCatalogTest {

    private static BossStageCatalog catalog;
    private static BossPatternCatalog patterns;
    private static BossArenaCatalog arenas;

    private static BossStageCatalog catalog() {
        if (catalog == null) catalog = new BossStageCatalog(BossBattleTest.read(
            "../assets/boss_nine_stages_v1/data/stage_catalog.runtime.json"));
        return catalog;
    }

    private static BossPatternCatalog patterns() {
        if (patterns == null) patterns = new BossPatternCatalog(BossBattleTest.read(
            "../assets/boss_nine_stages_v1/data/patterns.runtime.json"));
        return patterns;
    }

    private static BossArenaCatalog arenas() {
        if (arenas == null) arenas = new BossArenaCatalog(BossBattleTest.read(
            "../assets/boss_nine_stages_v1/data/arena_layouts.runtime.json"));
        return arenas;
    }

    @Test public void nineStageChainIsSequential() {
        String[] expected = {"1-1", "1-2", "1-3", "1-4", "1-5", "1-보스", "2-1", "2-2", "2-3"};
        List<BossStageCatalog.Stage> stages = catalog().stages();
        assertEquals(9, stages.size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], stages.get(i).stageId);
            assertEquals("순차 해금 사슬", i + 1 < expected.length ? expected[i + 1] : null,
                stages.get(i).nextStage);
        }
        assertTrue("첫 단계만 기본 해금", stages.get(0).initialProgressUnlocked);
        for (int i = 1; i < stages.size(); i++)
            assertFalse(stages.get(i).initialProgressUnlocked);
    }

    /** 기존 세이브(1-1 클리어 → bossUnlockedStages=["1-2"])가 새 카탈로그에서 그대로 동작. */
    @Test public void existingSaveMigratesWithoutLosingClear() {
        Set<String> savedUnlocked = new LinkedHashSet<>();
        savedUnlocked.add("1-2"); // 기존 1-1 클리어 세이브의 해금 기록
        assertTrue("1-1 기본 해금 유지", catalog().enterable("1-1", savedUnlocked));
        assertTrue("기존 해금 기록으로 1-2 입장 가능(이번 빌드 구현)",
            catalog().enterable("1-2", savedUnlocked));
        assertFalse("1-3은 여전히 잠김", catalog().progressUnlocked("1-3", savedUnlocked));
        assertFalse("잠긴 단계 입장 차단", catalog().enterable("1-3", savedUnlocked));
    }

    /** 1-1 회귀 계약: 보스 ID·저장 키 불변, 표기 이름만 교체, 레거시 엔진 보존. */
    @Test public void stageOneOnePreservesLegacyContract() {
        BossStageCatalog.Stage first = catalog().stage("1-1");
        assertEquals("centrifuge_c01", first.bossId);
        assertTrue("1-1은 레거시 엔진·밸런스 보존", first.legacyEngine);
        assertEquals("폐기물 변이체", first.displayName);
        for (BossStageCatalog.Stage stage : catalog().stages())
            if (!stage.stageId.equals("1-1"))
                assertFalse(stage.stageId + "는 신규 엔진", stage.legacyEngine);
    }

    @Test public void everyStageResolvesPatternsArenaAndRenderData() {
        for (BossStageCatalog.Stage stage : catalog().stages()) {
            if (stage.legacyEngine) continue; // 1-1은 boss_1_1.json/기존 투기장 사용
            assertTrue(stage.stageId + " 입장 가능 플래그", stage.implemented);
            assertTrue(stage.stageId + " 패턴 목록", stage.patternIds.length >= 2);
            for (String id : stage.patternIds)
                assertNotNull(patterns().pattern(id)); // 미정의면 예외
            assertNotNull(arenas().arena(stage.arenaId));
            assertTrue("그리기 크기 8타일 이상", stage.drawWidthTiles >= 8f);
            assertTrue("피격 박스는 그리기와 분리(더 작음)",
                stage.hurtboxW < stage.drawWidthTiles);
            assertTrue("이동 충돌은 피격 박스보다 작음", stage.footprintW <= stage.hurtboxW);
            assertNotNull(stage.portraitSilhouette);
            assertNotNull(stage.portraitColor);
        }
        assertEquals("1-보스는 더 크게", 9.5f,
            catalog().stage("1-보스").drawWidthTiles, 0.001f);
    }

    /** 투기장: 플레이어 반경을 반영해도 스폰→보스→대문이 연결되고, 보스 스폰이 기둥과 안 겹침. */
    @Test public void arenasAreReachableAndBossSpawnClearOfPillars() {
        for (BossStageCatalog.Stage stage : catalog().stages()) {
            if (stage.legacyEngine) continue;
            BossArenaCatalog.Arena arena = arenas().arena(stage.arenaId);
            boolean[][] walkable = new boolean[arena.width][arena.height];
            for (int x = 2; x < arena.width - 2; x++)
                for (int y = 2; y < arena.height - 2; y++) walkable[x][y] = true;
            for (float[] pillar : arena.pillars) {
                // 플레이어 몸 반경(0.3) 부풀림: 기둥 주변 1타일까지 막힌 것으로 본다.
                for (int x = (int) pillar[0] - 1; x < pillar[0] + pillar[2] + 1; x++)
                    for (int y = (int) pillar[1] - 1; y < pillar[1] + pillar[3] + 1; y++)
                        if (x >= 0 && y >= 0 && x < arena.width && y < arena.height)
                            walkable[x][y] = false;
                // 보스 스폰이 기둥 footprint와 충돌하지 않는다.
                float half = stage.footprintW / 2f;
                assertFalse(stage.stageId + " 보스 스폰-기둥 충돌",
                    arena.bossSpawnX + half > pillar[0]
                        && arena.bossSpawnX - half < pillar[0] + pillar[2]
                        && arena.bossSpawnY + stage.footprintH > pillar[1]
                        && arena.bossSpawnY < pillar[1] + pillar[3]);
            }
            Set<Long> visited = new LinkedHashSet<>();
            Deque<int[]> frontier = new ArrayDeque<>();
            frontier.add(new int[]{(int) arena.playerSpawnX, (int) arena.playerSpawnY});
            while (!frontier.isEmpty()) {
                int[] cell = frontier.poll();
                int x = cell[0], y = cell[1];
                if (x < 0 || y < 0 || x >= arena.width || y >= arena.height) continue;
                if (!walkable[x][y] || !visited.add(x * 1000L + y)) continue;
                frontier.add(new int[]{x + 1, y});
                frontier.add(new int[]{x - 1, y});
                frontier.add(new int[]{x, y + 1});
                frontier.add(new int[]{x, y - 1});
            }
            assertTrue(stage.stageId + " 보스 스폰 도달",
                visited.contains((long) (int) arena.bossSpawnX * 1000L + (int) arena.bossSpawnY));
            assertTrue(stage.stageId + " 대문 앞 도달",
                visited.contains((arena.gateX + arena.gateW / 2) * 1000L + 2L));
        }
    }

    /** 공유 틱 그룹·차폐 정책이 데이터에 선언돼 있다(계약 5절). */
    @Test public void patternPoliciesDeclaredInData() {
        assertEquals("boss_toxin", patterns().pattern("spore_zone").sharedTickGroup);
        assertEquals("boss_toxin", patterns().pattern("toxic_zone").sharedTickGroup);
        assertEquals("boss_toxin", patterns().pattern("toxic_leak").sharedTickGroup);
        assertTrue(patterns().pattern("cold_cone").occludedByPillars);
        assertTrue(patterns().pattern("rotating_laser").occludedByPillars);
        assertTrue(patterns().pattern("psychic_wave").occludedByPillars);
        assertFalse("장판 낙하는 기둥을 넘는다",
            patterns().pattern("ice_zone").occludedByPillars);
        assertTrue(patterns().pattern("chain_charge").blockedByPillars);
        assertTrue(patterns().pattern("acid_throw").blockedByPillars);
        assertEquals(0.5f, patterns().pattern("enrage_transition").triggerHpFraction, .001f);
        List<String> subs = new ArrayList<>();
        for (String s : patterns().pattern("fusion_combo").subpatterns) subs.add(s);
        assertTrue("복합 공격은 순차 하위 패턴", subs.size() >= 2);
    }
}

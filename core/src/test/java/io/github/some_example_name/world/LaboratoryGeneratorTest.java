package io.github.some_example_name.world;

import io.github.some_example_name.combat.MonsterType;
import io.github.some_example_name.config.BalanceConfig;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * 난이도별 시드 기반 연구소 생성 검증(순수 Java — GL 없음).
 * RNG golden vector는 팩 참조 파이썬 구현에서 산출한 값과 대조한다.
 */
public class LaboratoryGeneratorTest {

    private static LabGenData data;

    private static LabGenData data() {
        if (data == null) data = new LabGenData(new LabGenData.Source() {
            @Override public String read(String relativePath) {
                try {
                    return new String(Files.readAllBytes(
                        Paths.get("../assets/laboratory_generation_v1/" + relativePath)),
                        StandardCharsets.UTF_8);
                } catch (java.io.IOException e) { throw new RuntimeException(e); }
            }
        });
        return data;
    }

    private static BalanceConfig config(GameDifficulty difficulty) {
        BalanceConfig config = new BalanceConfig();
        config.applyDifficulty(difficulty);
        return config;
    }

    // ----- RNG 재현(golden vector — tools/reference_generator.py와 동일) ------------------

    @Test public void rngMatchesPythonReferenceGoldenVectors() {
        GenRng template = new GenRng("1001", "template");
        assertEquals(3999864624L, template.next());
        assertEquals(1566662551L, template.next());
        assertEquals(2766348062L, template.next());
        assertEquals(655830878L, template.next());
        GenRng topology = new GenRng("1001", "topology");
        assertEquals(171762395L, topology.next());
        assertEquals(1520366824L, topology.next());
        GenRng shape = new GenRng("1001", "shape:r00");
        assertEquals(220273599L, shape.next());
        assertEquals(3834347028L, shape.next());
        GenRng monsters = new GenRng("1001", "monsters");
        assertEquals(2976968477L, monsters.next());
        GenRng big = new GenRng("9223372036854775807", "x");
        assertEquals("64비트 시드 문자열/오버플로 일치", 52441561L, big.next());
        assertEquals(2792978900L, big.next());
    }

    @Test public void sameSeedSameResultDifferentSeedUsuallyDiffers() {
        BalanceConfig config = config(GameDifficulty.NORMAL);
        LaboratoryGenerator.Result a = LaboratoryGenerator.generate(
            GameDifficulty.NORMAL, 424242L, config, data());
        LaboratoryGenerator.Result b = LaboratoryGenerator.generate(
            GameDifficulty.NORMAL, 424242L, config, data());
        assertEquals("같은 seed+버전+난이도 = 같은 결과", a.contentHash, b.contentHash);
        assertEquals(a.templateId, b.templateId);
        assertEquals(a.monsters.size(), b.monsters.size());
        for (int i = 0; i < a.monsters.size(); i++) {
            assertEquals(a.monsters.get(i).type, b.monsters.get(i).type);
            assertEquals(a.monsters.get(i).x, b.monsters.get(i).x, 0f);
        }
        LaboratoryGenerator.Result c = LaboratoryGenerator.generate(
            GameDifficulty.NORMAL, 424243L, config, data());
        assertFalse("다른 시드는 (보통) 다른 맵", a.contentHash.equals(c.contentHash)
            && a.templateId.equals(c.templateId));
    }

    @Test public void substreamsAreIndependent() {
        // 같은 시드의 monsters 스트림은 topology/loot 스트림 소비량과 무관하게 같은 수열이다.
        GenRng a = new GenRng("777", "monsters");
        GenRng waste = new GenRng("777", "topology");
        for (int i = 0; i < 100; i++) waste.next(); // 다른 스트림을 아무리 써도
        GenRng b = new GenRng("777", "monsters");
        for (int i = 0; i < 10; i++) assertEquals(a.next(), b.next());
    }

    // ----- 다중 시드 전수 검증(난이도별 ≥100 시드, 12개 틀 전부) -------------------------

    @Test public void hundredSeedsPerDifficultyValidateAcrossAllTemplates() {
        StringBuilder report = new StringBuilder();
        for (GameDifficulty difficulty : GameDifficulty.values()) {
            BalanceConfig config = config(difficulty);
            java.util.Set<String> seenTemplates = new java.util.TreeSet<>();
            int fallbacks = 0, retries = 0;
            long corridorMaxTotal = 0;
            int monstersMin = Integer.MAX_VALUE, monstersMax = 0;
            for (int i = 0; i < 100; i++) {
                long seed = 50_000L + i * 31L + difficulty.ordinal();
                LaboratoryGenerator.Result result =
                    LaboratoryGenerator.generate(difficulty, seed, config, data());
                if (result.fallbackUsed) { fallbacks++; continue; }
                retries += result.attemptFailures.size();
                seenTemplates.add(result.templateId);
                validate(result, difficulty, config);
                monstersMin = Math.min(monstersMin, result.monsters.size());
                monstersMax = Math.max(monstersMax, result.monsters.size());
            }
            assertEquals("난이도별 4개 구조 틀이 모두 선택됨(" + difficulty + ": "
                + seenTemplates + ")", 4, seenTemplates.size());
            assertTrue("폴백 남용 금지(" + difficulty + " fallbacks=" + fallbacks + ")",
                fallbacks <= 5);
            report.append(difficulty).append(": templates=").append(seenTemplates)
                .append(" retries=").append(retries).append(" fallbacks=").append(fallbacks)
                .append(" monsters=").append(monstersMin).append("..").append(monstersMax)
                .append('\n');
        }
        System.out.println(report);
    }

    private static void validate(LaboratoryGenerator.Result result, GameDifficulty difficulty,
                                 BalanceConfig config) {
        LaboratoryRoom room = result.layout.room();
        // 접근성: 스폰에서 모든 방 활동 영역·캐비넷 상호작용 지점 도달(생성기 자체 검증과
        // 동일 기준을 외부에서 재확인).
        boolean[][] reachable = LaboratoryGenerator.reachability(room,
            (int) room.spawnPoint().x, (int) room.spawnPoint().y, config);
        int reachableCount = 0;
        for (boolean[] rows : reachable) for (boolean v : rows) if (v) reachableCount++;
        assertTrue("도달 가능한 영역 존재", reachableCount > 400);
        for (LaboratoryRoomArea area : result.layout.roomAreas()) {
            boolean anyReachable = false;
            for (int y = (int) area.y() + 1; y < area.y() + area.height() - 1 && !anyReachable; y++)
                for (int x = (int) area.x() + 1; x < area.x() + area.width() - 1; x++)
                    if (reachable[y][x]) { anyReachable = true; break; }
            assertTrue("방 도달 가능: " + area.id() + " seed=" + result.seed
                + " template=" + result.templateId, anyReachable);
        }
        // 몬스터 규칙.
        int leaders = 0, guards = 0;
        for (LaboratoryGenerator.MonsterPlacement m : result.monsters) {
            if (m.type == MonsterType.TEAM_LEADER) leaders++;
            if (m.type == MonsterType.GUARD) guards++;
            assertFalse("몬스터가 벽/가구 안에 없음",
                room.blocksActorAtCell((int) m.x, (int) m.y,
                    config.monsterBoundsWidth, config.monsterBoundsHeight));
        }
        if (difficulty == GameDifficulty.EASY) {
            assertEquals("쉬움 금지 몬스터(경비원) 0", 0, guards);
            assertEquals("쉬움 금지 몬스터(팀장) 0", 0, leaders);
            assertEquals("쉬움 예산 = 현재 검증된 56(8배 재적용 없음)", 56, result.monsters.size());
        } else {
            assertTrue("보통/어려움 최소 종(경비원)", guards >= 1);
            assertTrue(difficulty + " 팀장 ≥1", leaders >= 1);
        }
        // 팀장은 두 벽이 만나는 구석 후보에만.
        for (LaboratoryGenerator.MonsterPlacement m : result.monsters) {
            if (m.type != MonsterType.TEAM_LEADER) continue;
            int x = (int) m.x, y = (int) m.y;
            boolean wallNS = room.isSolid(x, y + 1) || room.isSolid(x, y - 1);
            boolean wallEW = room.isSolid(x + 1, y) || room.isSolid(x - 1, y);
            assertTrue("팀장 구석 조건 seed=" + result.seed, wallNS && wallEW);
        }
        // 몬스터 간 최소 간격.
        for (int i = 0; i < result.monsters.size(); i++)
            for (int j = i + 1; j < result.monsters.size(); j++) {
                float dx = result.monsters.get(i).x - result.monsters.get(j).x;
                float dy = result.monsters.get(i).y - result.monsters.get(j).y;
                assertTrue("몬스터 겹침 없음",
                    dx * dx + dy * dy >= config.monsterSpacing * config.monsterSpacing - 0.001f);
            }
        // 대문·복귀: 남쪽 대문 설정 존재.
        assertTrue(room.hasGate());
    }

    @Test public void difficultyScalesSizeRoomsAndCounts() {
        LaboratoryGenerator.Result easy = LaboratoryGenerator.generate(
            GameDifficulty.EASY, 9090L, config(GameDifficulty.EASY), data());
        LaboratoryGenerator.Result normal = LaboratoryGenerator.generate(
            GameDifficulty.NORMAL, 9090L, config(GameDifficulty.NORMAL), data());
        LaboratoryGenerator.Result hard = LaboratoryGenerator.generate(
            GameDifficulty.HARD, 9090L, config(GameDifficulty.HARD), data());
        assertTrue(easy.layout.room().widthInTiles() < normal.layout.room().widthInTiles());
        assertTrue(normal.layout.room().widthInTiles() < hard.layout.room().widthInTiles());
        assertEquals(16, easy.layout.roomAreas().size());
        assertEquals(25, normal.layout.roomAreas().size());
        assertEquals(36, hard.layout.roomAreas().size());
        assertTrue("몬스터 총량 증가", easy.monsters.size() < normal.monsters.size()
            && normal.monsters.size() < hard.monsters.size());
        assertTrue("캐비넷 증가", easy.layout.lootContainers().size()
            <= normal.layout.lootContainers().size()
            && normal.layout.lootContainers().size() <= hard.layout.lootContainers().size());
    }

    @Test public void goldenFallbackSeedValidatesForNormalAndHard() {
        // 폴백 경로가 쓰는 golden seed + compact 틀이 항상 유효해야 한다.
        for (GameDifficulty difficulty : new GameDifficulty[]{GameDifficulty.NORMAL, GameDifficulty.HARD}) {
            BalanceConfig config = config(difficulty);
            LaboratoryGenerator.Result result = LaboratoryGenerator.generateOnce(difficulty,
                Long.toString(LaboratoryGenerator.GOLDEN_SEED),
                Long.toString(LaboratoryGenerator.GOLDEN_SEED), config, data(),
                (difficulty == GameDifficulty.NORMAL ? "normal" : "hard") + "_compact",
                true, new ArrayList<String>());
            assertNotNull(result.layout);
            validate(result, difficulty, config);
        }
    }

    @Test public void sixteenThemesAllAppearInEveryMap() {
        for (GameDifficulty difficulty : GameDifficulty.values()) {
            LaboratoryGenerator.Result result = LaboratoryGenerator.generate(
                difficulty, 777L, config(difficulty), data());
            if (result.fallbackUsed) continue;
            java.util.Set<String> themes = new java.util.TreeSet<>();
            for (LaboratoryRoomArea area : result.layout.roomAreas())
                themes.add(area.id().substring(0, area.id().lastIndexOf('_')));
            assertEquals("16개 테마 전부 등장(" + difficulty + ": " + themes + ")",
                16, themes.size());
        }
    }
}

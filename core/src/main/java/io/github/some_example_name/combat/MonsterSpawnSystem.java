package io.github.some_example_name.combat;

import io.github.some_example_name.config.BalanceConfig;
import io.github.some_example_name.entity.monster.Monster;
import io.github.some_example_name.world.Hazard;
import io.github.some_example_name.world.LaboratoryRoom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

/** Seedable, bounded placement over safe 3x3 floor areas in the laboratory. */
public final class MonsterSpawnSystem {
    private static final Logger LOG = Logger.getLogger(MonsterSpawnSystem.class.getName());
    private final BalanceConfig config;

    public MonsterSpawnSystem(BalanceConfig config) { this.config = config; }

    public List<Monster> spawn(LaboratoryRoom room, long seed) {
        Random random = new Random(seed);
        List<float[]> candidates = new ArrayList<>();
        for (int y = 1; y < room.heightInTiles() - 1; y++) {
            for (int x = 1; x < room.widthInTiles() - 1; x++) {
                float px = x + 0.5f, py = y + 0.5f;
                if (safeArea(room, x, y)
                    && distanceSquared(px, py, room.spawnPoint().x, room.spawnPoint().y)
                        >= config.monsterSpawnExclusionRadius * config.monsterSpawnExclusionRadius) {
                    candidates.add(new float[]{px, py});
                }
            }
        }
        Collections.shuffle(candidates, random);
        List<Monster> placed = new ArrayList<>();
        long id = 1;
        for (MonsterType type : MonsterType.values()) {
            int count = count(type);
            int attempts = 0;
            for (float[] point : candidates) {
                if (attempts++ >= config.monsterSpawnAttempts || count == 0) break;
                if (!separated(point, placed)) continue;
                int hp = maxHp(type);
                placed.add(new Monster(id++, type, point[0], point[1],
                    config.monsterBoundsWidth, config.monsterBoundsHeight, hp));
                count--;
            }
            if (count > 0) LOG.warning("Could not place " + count + " " + type + " monster(s)");
        }
        return placed;
    }

    private boolean safeArea(LaboratoryRoom room, int x, int y) {
        for (int ty = y - 1; ty <= y + 1; ty++) {
            for (int tx = x - 1; tx <= x + 1; tx++) {
                if (room.isSolid(tx, ty) || room.hazardAt(tx, ty) != Hazard.NONE) return false;
            }
        }
        return true;
    }

    private boolean separated(float[] point, List<Monster> placed) {
        float spacing2 = config.monsterSpacing * config.monsterSpacing;
        for (Monster other : placed)
            if (distanceSquared(point[0], point[1], other.feetX(), other.feetY()) < spacing2) return false;
        return true;
    }

    private int count(MonsterType type) {
        switch (type) {
            case SLIME: return config.slimeSpawnCount;
            case RESEARCHER: return config.researcherSpawnCount;
            case GUARD: return config.guardSpawnCount;
            default: return config.teamLeaderSpawnCount;
        }
    }
    private int maxHp(MonsterType type) {
        switch (type) {
            case SLIME: return config.slimeMaxHp;
            case RESEARCHER: return config.researcherMaxHp;
            case GUARD: return config.guardMaxHp;
            default: return config.teamLeaderMaxHp;
        }
    }
    private static float distanceSquared(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2, dy = y1 - y2;
        return dx * dx + dy * dy;
    }
}

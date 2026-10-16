package io.github.some_example_name.combat;

import io.github.some_example_name.config.BalanceConfig;
import io.github.some_example_name.entity.monster.Monster;
import io.github.some_example_name.world.Hazard;
import io.github.some_example_name.world.LaboratoryRoom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
        // 선호 후보(3x3 전부 여유)와 예비 후보(해당 셀만 통행 가능)를 나눠 모은다.
        // EASY 8배처럼 수가 많아 선호 후보가 부족하면 예비 후보(복도·다른 방 포함)로
        // 분산해 개수를 조용히 줄이지 않는다. 두 묶음 모두 문/개구부 주변·가구·상자·
        // 대문 통로·입장 안전 영역은 제외된다.
        List<float[]> candidates = new ArrayList<>();
        List<float[]> fallback = new ArrayList<>();
        float exclusion2 = config.monsterSpawnExclusionRadius * config.monsterSpawnExclusionRadius;
        for (int y = 1; y < room.heightInTiles() - 1; y++) {
            for (int x = 1; x < room.widthInTiles() - 1; x++) {
                float px = x + 0.5f, py = y + 0.5f;
                if (distanceSquared(px, py, room.spawnPoint().x, room.spawnPoint().y) < exclusion2)
                    continue;
                if (safeArea(room, x, y)) candidates.add(new float[]{px, py});
                else if (looseCell(room, x, y)) fallback.add(new float[]{px, py});
            }
        }
        Collections.shuffle(candidates, random);
        Collections.shuffle(fallback, random);
        candidates.addAll(fallback);
        final float[][] outerCorners = traversableCorners(room);
        List<Monster> placed = new ArrayList<>();
        long id = 1;
        for (MonsterType type : MonsterType.values()) {
            int count = count(type);
            int attempts = 0;
            List<float[]> ordered = new ArrayList<>(candidates);
            if (type == MonsterType.TEAM_LEADER) {
                Collections.sort(ordered, new Comparator<float[]>() {
                    @Override public int compare(float[] a, float[] b) {
                        return Float.compare(cornerDistance(outerCorners, a), cornerDistance(outerCorners, b));
                    }
                });
            }
            for (float[] point : ordered) {
                if (attempts++ >= config.monsterSpawnAttempts || count == 0) break;
                if (!separated(point, placed)) continue;
                int hp = maxHp(type);
                Monster monster = new Monster(id++, type, point[0], point[1],
                    config.monsterBoundsWidth, config.monsterBoundsHeight, hp);
                // 배회 출발 시점을 개체마다 다르게 해 동시에 같은 행동을 하지 않게 한다.
                monster.setRestTime(random.nextFloat() * 2f);
                placed.add(monster);
                count--;
            }
            if (count > 0) LOG.warning("Could not place " + count + " " + type + " monster(s)");
        }
        // 종류별 실제 생성 수 기록(EASY 8배 검증·보고용). 조용히 줄어든 경우는 위 warning이 남는다.
        StringBuilder summary = new StringBuilder("spawned");
        for (MonsterType type : MonsterType.values()) {
            int n = 0;
            for (Monster m : placed) if (m.type() == type) n++;
            summary.append(' ').append(type).append('=').append(n);
        }
        LOG.info(summary.toString());
        return placed;
    }

    private static float[][] traversableCorners(LaboratoryRoom room) {
        int minX = room.widthInTiles(), minY = room.heightInTiles(), maxX = 0, maxY = 0;
        for (int y = 0; y < room.heightInTiles(); y++) {
            int open = 0;
            for (int x = 0; x < room.widthInTiles(); x++) if (!room.isSolid(x, y)) open++;
            if (open > 8) { minY = Math.min(minY, y); maxY = Math.max(maxY, y); }
        }
        for (int x = 0; x < room.widthInTiles(); x++) {
            int open = 0;
            for (int y = 0; y < room.heightInTiles(); y++) if (!room.isSolid(x, y)) open++;
            if (open > 8) { minX = Math.min(minX, x); maxX = Math.max(maxX, x); }
        }
        // Prefer the corners of the actual traversable laboratory footprint, not
        // empty padded map bounds used to keep the 128x96 overview centered.
        return new float[][] {{minX+1.5f,minY+1.5f},{maxX-1.5f,minY+1.5f},
            {minX+1.5f,maxY-1.5f},{maxX-1.5f,maxY-1.5f}};
    }

    private static float cornerDistance(float[][] corners, float[] p) {
        float best = Float.MAX_VALUE;
        for (float[] c : corners) {
            float dx=p[0]-c[0],dy=p[1]-c[1]; best=Math.min(best,dx*dx+dy*dy);
        }
        return best;
    }

    private boolean safeArea(LaboratoryRoom room, int x, int y) {
        for (int ty = y - 1; ty <= y + 1; ty++) {
            for (int tx = x - 1; tx <= x + 1; tx++) {
                if (room.blocksActorAtCell(tx, ty, config.monsterBoundsWidth, config.monsterBoundsHeight)
                        || room.hazardAt(tx, ty) != Hazard.NONE) return false;
                // 문/개구부(내부 방문·대문 통로) 위나 바로 옆에는 생성하지 않는다.
                // 가구·상자·대문 통로는 blocksActorAtCell의 장애물 검사로 이미 제외된다.
                io.github.some_example_name.world.TileType raw = room.rawTileAt(tx, ty);
                if (raw == io.github.some_example_name.world.TileType.DOORWAY_OPEN
                        || raw == io.github.some_example_name.world.TileType.DOOR_CLOSED) return false;
            }
        }
        return true;
    }

    /** 예비 후보: 해당 셀에 몸체가 설 수 있고(가구·상자·대문 포함) 문 주변(3x3)이 아니다. */
    private boolean looseCell(LaboratoryRoom room, int x, int y) {
        if (room.blocksActorAtCell(x, y, config.monsterBoundsWidth, config.monsterBoundsHeight)
                || room.hazardAt(x, y) != Hazard.NONE) return false;
        for (int ty = y - 1; ty <= y + 1; ty++) {
            for (int tx = x - 1; tx <= x + 1; tx++) {
                io.github.some_example_name.world.TileType raw = room.rawTileAt(tx, ty);
                if (raw == io.github.some_example_name.world.TileType.DOORWAY_OPEN
                        || raw == io.github.some_example_name.world.TileType.DOOR_CLOSED) return false;
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

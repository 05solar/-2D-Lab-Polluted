package io.github.some_example_name.world;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import io.github.some_example_name.combat.MonsterType;
import io.github.some_example_name.config.BalanceConfig;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 난이도별 시드 기반 연구소 생성기(순수 Java). 생성기는 데이터(논리 타일·가구·컨테이너·
 * 몬스터 배치)를 만들고 렌더러는 읽어서만 그린다 — 기존 쉬움 맵의 벽/문(V4)·방향별 가구·
 * 컨테이너 배치 코드(LaboratoryLayout의 공용 헬퍼)를 그대로 재사용해 품질 기준을 공유한다.
 *
 * 파이프라인: 구조 틀(난이도별 4종) → 방 배치(크기 변형/모양) → spanning tree + 추가 연결 →
 * 복도·문(5칸 직선 지지) → 남쪽 대문 → 방 테마/가구 → 캐비넷 → 접근성 검증(배우 그리드 BFS)
 * → 몬스터 → 불변 스냅샷. 같은 seed+버전+난이도는 같은 결과(contentHash)다.
 *
 * RNG: {@link GenRng}(FNV-1a32+XorShift32) — topology/shape/themes/furniture/loot/monsters
 * 하위 스트림을 scope 문자열로 분리해 한 단계의 소비량 변화가 다른 단계를 밀지 않는다.
 * 검증 실패는 attempt 하위 시드로 최대 3회 재생성하고, 최종 실패는 난이도별 검증된 폴백
 * (쉬움=수제 easyRoom, 보통/어려움=golden seed compact 틀)을 쓰며 사유를 기록한다.
 */
public final class LaboratoryGenerator {

    public static final int GENERATOR_VERSION = 1;
    /** 보통/어려움 폴백용 golden seed(테스트가 상시 검증). */
    static final long GOLDEN_SEED = 1001L;

    /** 생성된 몬스터 배치(소환은 게임 루트가 config HP로 수행). */
    public static final class MonsterPlacement {
        public final MonsterType type;
        public final float x, y;
        MonsterPlacement(MonsterType type, float x, float y) {
            this.type = type;
            this.x = x;
            this.y = y;
        }
    }

    /** 재현 버그 보고에 필요한 전부: 버전·시드·틀·해시·폴백 여부·통계. */
    public static final class Result {
        public final LaboratoryLayout layout;
        /** null이면 수제 폴백 맵 — 기존 MonsterSpawnSystem 경로를 쓴다. */
        public final List<MonsterPlacement> monsters;
        public final GameDifficulty difficulty;
        public final String seed;            // 요청 시드(64비트 정수 문자열)
        public final String generationSeed;  // 실제 성공한 attempt 시드
        public final String templateId;
        public final int generatorVersion, templateVersion;
        public final String contentHash;
        public final boolean fallbackUsed;
        public final List<String> attemptFailures;
        public final String stats;
        Result(LaboratoryLayout layout, List<MonsterPlacement> monsters, GameDifficulty difficulty,
               String seed, String generationSeed, String templateId, int templateVersion,
               String contentHash, boolean fallbackUsed, List<String> attemptFailures, String stats) {
            this.layout = layout;
            this.monsters = monsters;
            this.difficulty = difficulty;
            this.seed = seed;
            this.generationSeed = generationSeed;
            this.templateId = templateId;
            this.generatorVersion = GENERATOR_VERSION;
            this.templateVersion = templateVersion;
            this.contentHash = contentHash;
            this.fallbackUsed = fallbackUsed;
            this.attemptFailures = attemptFailures;
            this.stats = stats;
        }
        public String describe() {
            return "generatorVersion=" + generatorVersion + " templateVersion=" + templateVersion
                + " difficulty=" + difficulty + " seed=" + seed
                + " generationSeed=" + generationSeed + " templateId=" + templateId
                + " contentHash=" + contentHash + " fallbackUsed=" + fallbackUsed
                + (attemptFailures.isEmpty() ? "" : " failures=" + attemptFailures)
                + " | " + stats;
        }
    }

    static final class GenerationException extends RuntimeException {
        GenerationException(String message) { super(message); }
    }

    private LaboratoryGenerator() { }

    /** 새 탐사 확정 시에만 호출한다(화면 재생성·일시정지·창 크기 변경으로 재호출 금지). */
    public static Result generate(GameDifficulty difficulty, long seed,
                                  BalanceConfig config, LabGenData data) {
        String seedString = Long.toString(seed);
        List<String> failures = new ArrayList<>();
        for (int attempt = 0; attempt < 3; attempt++) {
            String actual = attempt == 0 ? seedString
                : Long.toString(new GenRng(seedString, "retry:" + attempt).next());
            try {
                return generateOnce(difficulty, seedString, actual, config, data, null,
                    false, failures);
            } catch (GenerationException | IllegalArgumentException | IllegalStateException e) {
                failures.add("attempt" + attempt + "(" + actual + "): " + e.getMessage());
            }
        }
        // 폴백: 쉬움은 수제 기준 맵(고정 시드 회귀 기준과 동일), 보통/어려움은 golden seed.
        if (difficulty == GameDifficulty.EASY) {
            LaboratoryLayout layout = LaboratoryLayout.easyRoom(config);
            return new Result(layout, null, difficulty, seedString, "handcrafted-easy",
                "easy_handcrafted_campus", 1, "handcrafted", true, failures,
                "fallback to handcrafted easy campus");
        }
        try {
            return generateOnce(difficulty, seedString, Long.toString(GOLDEN_SEED), config, data,
                key(difficulty) + "_compact", true, failures);
        } catch (RuntimeException last) {
            failures.add("golden: " + last.getMessage());
            LaboratoryLayout layout = LaboratoryLayout.forDifficulty(difficulty, config);
            return new Result(layout, null, difficulty, seedString, "legacy-variant",
                "legacy_variant", 1, "legacy", true, failures, "fallback to legacy variant map");
        }
    }

    private static String key(GameDifficulty difficulty) {
        switch (difficulty) {
            case EASY: return "easy";
            case NORMAL: return "normal";
            default: return "hard";
        }
    }

    // ----- 1회 생성 -------------------------------------------------------------------

    private static final class GenRoom {
        final String id;
        final int gx, gy;
        LaboratoryLayout.RoomPlan plan;
        String theme;
        int x, y, w, h;                    // 벽 포함 사각형
        int notchX, notchY, notchW, notchH; // 0이면 없음
        final List<int[]> doorCells = new ArrayList<>();
        GenRoom(String id, int gx, int gy) { this.id = id; this.gx = gx; this.gy = gy; }
        boolean insideRect(int cx, int cy) {
            return cx >= x && cx < x + w && cy >= y && cy < y + h;
        }
        int interiorFloorMinX() { return x + 1; }
        int interiorFloorMinY() { return y + 1; }
        int interiorFloorMaxX() { return x + w - 2; }
        int interiorFloorMaxY() { return y + h - 2; }
    }

    private static final class Edge {
        final GenRoom a, b;
        final boolean horizontal;
        int centerlineLength;
        Edge(GenRoom a, GenRoom b, boolean horizontal) {
            this.a = a;
            this.b = b;
            this.horizontal = horizontal;
        }
    }

    static Result generateOnce(GameDifficulty difficulty, String requestSeed, String seed,
                               BalanceConfig config, LabGenData data, String forcedTemplateId,
                               boolean fallbackUsed, List<String> failures) {
        List<LabGenData.Template> templates = data.templates(difficulty);
        LabGenData.Template template;
        if (forcedTemplateId != null) {
            template = null;
            for (LabGenData.Template t : templates)
                if (t.templateId.equals(forcedTemplateId)) template = t;
            if (template == null) throw new GenerationException("unknown template " + forcedTemplateId);
        } else {
            template = templates.get(new GenRng(seed, "template").nextInt(templates.size()));
        }
        LabGenData.Profile profile = data.profile(difficulty);
        int w = template.mapWidth, h = template.mapHeight;
        TileType[][] logical = new TileType[h][w];
        LaboratoryZone[][] zones = new LaboratoryZone[h][w];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            logical[y][x] = TileType.WALL;
            zones[y][x] = LaboratoryZone.CENTRAL;
        }

        // --- 방 배치: 격자 슬롯 + 시드 크기 변형/지터. 방 사각형은 벽 포함이며 pitch-7 상한으로
        // 인접 방 사이 간격(복도 공간) ≥3을 보장한다. 숫자 채우기용 빈 거대 방을 만들지 않는다.
        int originY = 4; // 남쪽: 외벽 링(2) + 대문 통로
        int maxGx = 0, maxGy = 0;
        for (int[] g : template.slots.values()) {
            maxGx = Math.max(maxGx, g[0]);
            maxGy = Math.max(maxGy, g[1]);
        }
        int marginX = Math.max(2, (w - (maxGx * template.pitchX + template.interiorW + 2)) / 2);
        Map<String, GenRoom> rooms = new LinkedHashMap<>();
        for (Map.Entry<String, int[]> slot : template.slots.entrySet()) {
            GenRoom room = new GenRoom(slot.getKey(), slot.getValue()[0], slot.getValue()[1]);
            GenRng r = new GenRng(seed, "shape:" + room.id);
            int interiorW = Math.min(template.interiorW, template.pitchX - 7) - r.nextInt(3);
            int interiorH = Math.min(template.interiorH, template.pitchY - 7) - r.nextInt(2);
            interiorW = Math.max(11, interiorW);
            interiorH = Math.max(7, interiorH);
            room.w = interiorW + 2;
            room.h = interiorH + 2;
            room.x = marginX + room.gx * template.pitchX + (r.nextInt(3) - 1);
            room.y = originY + room.gy * template.pitchY + (r.nextInt(3) - 1);
            if (room.x < 2 || room.y < 2 || room.x + room.w > w - 2 || room.y + room.h > h - 2)
                throw new GenerationException("room out of bounds " + room.id);
            // 모양은 문 위치 확정 후에 적용한다(문 앵커를 잘라버리는 변경 금지) — 여기선 종류만.
            room.theme = null;
            rooms.put(room.id, room);
        }

        // --- 연결 그래프: 직교 인접 spanning tree + 짧은 우회 연결(길 지우기 방식 금지).
        Map<String, GenRoom> byGrid = new LinkedHashMap<>();
        for (GenRoom room : rooms.values()) byGrid.put(room.gx + "," + room.gy, room);
        List<Edge> allEdges = new ArrayList<>();
        for (GenRoom room : rooms.values()) {
            GenRoom east = byGrid.get((room.gx + 1) + "," + room.gy);
            GenRoom north = byGrid.get(room.gx + "," + (room.gy + 1));
            if (east != null) allEdges.add(new Edge(room, east, true));
            if (north != null) allEdges.add(new Edge(room, north, false));
        }
        GenRng topo = new GenRng(seed, "topology");
        topo.shuffle(allEdges);
        Map<String, String> parent = new LinkedHashMap<>();
        for (GenRoom room : rooms.values()) parent.put(room.id, room.id);
        List<Edge> chosen = new ArrayList<>();
        List<Edge> extras = new ArrayList<>();
        for (Edge edge : allEdges) {
            if (!find(parent, edge.a.id).equals(find(parent, edge.b.id))) {
                parent.put(find(parent, edge.a.id), find(parent, edge.b.id));
                chosen.add(edge);
            } else {
                extras.add(edge);
            }
        }
        java.util.Set<String> roots = new java.util.LinkedHashSet<>();
        for (GenRoom room : rooms.values()) roots.add(find(parent, room.id));
        if (roots.size() != 1) throw new GenerationException("disconnected template grid");
        for (Edge edge : extras)
            if (topo.nextInt(100) < profile.extraEdgePercent) chosen.add(edge);

        // --- 문 위치(각 변 중앙, 코너·노치에서 ≥3) 확정 후 모양 적용.
        Map<Edge, int[][]> edgeDoors = new LinkedHashMap<>();
        for (Edge edge : chosen) {
            GenRoom a = edge.a, b = edge.b;
            if (edge.horizontal) {
                int ay = clampDoor(a.y + a.h / 2, a.y + 3, a.y + a.h - 4);
                int by = clampDoor(b.y + b.h / 2, b.y + 3, b.y + b.h - 4);
                edgeDoors.put(edge, new int[][]{{a.x + a.w - 1, ay}, {b.x, by}});
            } else {
                int ax = clampDoor(a.x + a.w / 2, a.x + 3, a.x + a.w - 4);
                int bx = clampDoor(b.x + b.w / 2, b.x + 3, b.x + b.w - 4);
                edgeDoors.put(edge, new int[][]{{ax, a.y + a.h - 1}, {bx, b.y}});
            }
        }
        // 대문(남쪽 1개): 가장 남쪽 행에서 중앙에 가장 가까운 방 — 본부↔연구소 동일 출구.
        GenRoom entry = null;
        for (GenRoom room : rooms.values()) {
            if (room.gy != 0) continue;
            if (entry == null || Math.abs(room.x + room.w / 2 - w / 2)
                < Math.abs(entry.x + entry.w / 2 - w / 2)) entry = room;
        }
        int gateX = clampDoor(entry.x + entry.w / 2, entry.x + 3, entry.x + entry.w - 4);
        int[] gateDoor = {gateX, entry.y};

        // 모양(사각형/모서리 후퇴): 문 앵커 ±2와 겹치면 사각형으로 유지(생략 사유 통계).
        int shapeDowngrades = 0;
        for (GenRoom room : rooms.values()) {
            GenRng r = new GenRng(seed, "notch:" + room.id);
            String shape = template.allowedShapes.get(r.nextInt(template.allowedShapes.size()));
            int nw = Math.min(4, (room.w - 2) / 3), nh = Math.min(2, (room.h - 2) / 3);
            if ("rectangle".equals(shape) || nw < 2 || nh < 1) continue;
            int nx, ny;
            if ("notch_ne".equals(shape)) { nx = room.x + room.w - 1 - nw; ny = room.y + room.h - 1 - nh; }
            else if ("notch_sw".equals(shape)) { nx = room.x + 1; ny = room.y + 1; }
            else { nx = room.x + room.w - 1 - nw; ny = room.y + 1; } // stepped → SE 후퇴로 단순화
            boolean conflict = nearNotch(gateDoor, room, nx, ny, nw, nh);
            for (int[][] doors : edgeDoors.values())
                for (int[] d : doors) conflict |= nearNotch(d, room, nx, ny, nw, nh);
            if (conflict) { shapeDowngrades++; continue; }
            room.notchX = nx;
            room.notchY = ny;
            room.notchW = nw;
            room.notchH = nh;
        }

        // --- RoomPlan 생성(기존 쉬움 가구·컨테이너 파이프라인과 공유) + 바닥 조각.
        for (GenRoom room : rooms.values()) {
            LaboratoryLayout.Side side = room == entry ? LaboratoryLayout.Side.SOUTH
                : LaboratoryLayout.Side.NORTH;
            int offset = (room == entry ? gateX - room.x : room.w / 2);
            room.plan = room.notchW > 0
                ? LaboratoryLayout.RoomPlan.absoluteNotched(room.id, room.x, room.y, room.w, room.h,
                    side, offset, room.notchX, room.notchY, room.notchW, room.notchH)
                : LaboratoryLayout.RoomPlan.absolute(room.id, room.x, room.y, room.w, room.h,
                    side, offset);
            LaboratoryLayout.drawRoomFloor(logical, room.plan);
        }

        // --- 복도: 인접 방 사이 짧은 도그레그 + 문마다 5칸 직선 지지용 전실.
        List<Integer> corridorLengths = new ArrayList<>();
        for (Edge edge : chosen) {
            int[][] doors = edgeDoors.get(edge);
            carveCorridor(logical, rooms.values(), edge, doors[0], doors[1]);
            corridorLengths.add(edge.centerlineLength);
            logical[doors[0][1]][doors[0][0]] = TileType.DOORWAY_OPEN;
            logical[doors[1][1]][doors[1][0]] = TileType.DOORWAY_OPEN;
        }
        // 대문 전실·통로: 방 남벽 문 아래 5칸 전실(y-1..y-2) + 외벽 링까지 3칸 폭 통로.
        for (int y = 2; y <= entry.y - 1; y++)
            for (int x = gateX - 2; x <= gateX + 2; x++)
                if (!insideAnyRoom(rooms.values(), x, y)) logical[y][x] = TileType.FLOOR;
        logical[gateDoor[1]][gateDoor[0]] = TileType.DOORWAY_OPEN;

        // --- 테마: 입구 방은 entry, 나머지는 시드 순서로 16개 테마를 최소 1회씩 순환.
        List<String> themePool = new ArrayList<>(data.themeIds);
        themePool.remove("entry");
        Collections.sort(themePool);
        new GenRng(seed, "themes").shuffle(themePool);
        int themeIndex = 0;
        for (GenRoom room : rooms.values()) {
            if (room == entry) room.theme = "entry";
            else room.theme = themePool.get(themeIndex++ % themePool.size());
        }

        // --- 가구(테마 kit — 기존 쉬움의 방향별 뷰·footprint 검증 경로 공유).
        List<LaboratoryProp> props = new ArrayList<>();
        List<WallFurnitureCoverageReport> coverage = new ArrayList<>();
        List<LaboratoryLayout.RoomPlan> plans = new ArrayList<>();
        for (GenRoom room : rooms.values()) {
            plans.add(room.plan);
            int variant = new GenRng(seed, "furniture:" + room.id).nextInt(3);
            LaboratoryLayout.addThemedRoomFurniture(props, room.plan,
                LaboratoryLayout.kitForTheme(room.theme), coverage, logical,
                room == entry, variant);
        }
        LaboratoryLayout.validateFurniturePlacements(props, plans, logical);

        // --- 문 조립(5칸 직선 지지 검증 — 실패는 재시도 대상) 후 대문 개구부를 뚫는다.
        DoorPlacementResolver.Result resolvedDoors = new DoorPlacementResolver().resolve(logical);
        for (int y = 0; y <= 1; y++)
            for (int x = gateX - 1; x <= gateX + 1; x++) logical[y][x] = TileType.DOORWAY_OPEN;

        // --- 캐비넷: 난이도별 목표 수(BalanceConfig), 벽면/구석/가구 옆 가중 추첨 재사용.
        Vector2 spawn = new Vector2(gateX + 0.5f, 2.3f);
        GenRng lootRng = new GenRng(seed, "loot");
        java.util.Random lootRandom = new java.util.Random(lootRng.next());
        List<GenRoom> roomOrder = new ArrayList<>(rooms.values());
        lootRng.shuffle(roomOrder);
        List<GenRoom> rareRooms = new ArrayList<>();
        for (GenRoom room : roomOrder)
            if (isResearchTheme(room.theme)) rareRooms.add(room);
        if (rareRooms.isEmpty()) rareRooms = roomOrder;
        List<LootContainerPlacement> containers = new ArrayList<>();
        int droppedContainers = 0;
        droppedContainers += assignContainers(containers, "wooden_chest",
            config.woodenChestCount, roomOrder, logical, props, lootRandom, spawn);
        droppedContainers += assignContainers(containers, "epic_cabinet",
            config.epicCabinetCount, rareRooms, logical, props, lootRandom, spawn);
        droppedContainers += assignContainers(containers, "legendary_cabinet",
            config.legendaryCabinetCount, rareRooms, logical, props, lootRandom, spawn);

        // --- 접근성 검증(실제 배우 몸체 그리드, 4방향 BFS — 대각선 끼워 통과 없음).
        // 실패 시 바로 재생성하지 않고 "실패한 배치 슬롯"부터 고친다: 도달 불가 방은 중앙 섬을
        // 제거하고, 도달 불가 캐비넷은 떨어뜨린(dropped) 뒤 1회 재검증한다. 그래도 실패하면
        // 상위에서 attempt 하위 시드로 재생성한다.
        LaboratoryRoom room = null;
        boolean[][] reachable = null;
        for (int fixPass = 0; fixPass <= 1; fixPass++) {
            room = new LaboratoryRoom(logical, spawn, null, resolvedDoors.doors, props);
            room.setSouthExitGate(gateX - 1f, gateX + 2f, 1f);
            room.setGatePassage(new Rectangle(gateX - 1f, 0f, 3f, 2f));
            for (LootContainerPlacement placement : containers)
                room.addWorldObstacle(LaboratoryLayout.containerFootprint(
                    placement.id(), placement.x(), placement.y()));
            boolean[][] forced = new boolean[h][w];
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++)
                if (x <= 1 || y <= 1 || x >= w - 2 || y >= h - 2) forced[y][x] = true;
            room.setForcedWallMask(forced);
            reachable = reachability(room, (int) spawn.x, (int) spawn.y, config);

            List<GenRoom> failingRooms = new ArrayList<>();
            for (GenRoom genRoom : rooms.values()) {
                int reachableCells = 0, floorCells = 0;
                for (int y = genRoom.interiorFloorMinY(); y <= genRoom.interiorFloorMaxY(); y++)
                    for (int x = genRoom.interiorFloorMinX(); x <= genRoom.interiorFloorMaxX(); x++) {
                        if (logical[y][x] != TileType.FLOOR) continue;
                        floorCells++;
                        if (reachable[y][x]) reachableCells++;
                    }
                if (floorCells == 0 || reachableCells < Math.max(4, floorCells / 4))
                    failingRooms.add(genRoom);
            }
            List<LootContainerPlacement> unreachable = new ArrayList<>();
            for (LootContainerPlacement placement : containers)
                if (!containerApproachable(reachable, placement)) unreachable.add(placement);
            if (failingRooms.isEmpty() && unreachable.isEmpty()) break;
            if (fixPass == 1)
                throw new GenerationException("accessibility fix failed: rooms="
                    + failingRooms.size() + " containers=" + unreachable.size());
            for (GenRoom failing : failingRooms) removeIslandProps(props, failing);
            droppedContainers += unreachable.size();
            containers.removeAll(unreachable);
        }
        for (Door[] row : resolvedDoors.doors) for (Door door : row) {
            if (door == null) continue;
            if (!doorApproachable(reachable, door.x(), door.y(), logical))
                throw new GenerationException("door unreachable at " + door.x() + "," + door.y());
        }

        // --- 몬스터: 난이도 프로필 가중치, 쉬움은 금지 종 제외, 팀장은 유효 구석 후보만.
        List<MonsterPlacement> monsters = new ArrayList<>();
        int leaderTarget = 0, leaderPlaced = 0;
        {
            GenRng m = new GenRng(seed, "monsters");
            int baseEasyBudget = (config.easySlimeBaseCount + config.easyResearcherBaseCount
                + config.easyGuardBaseCount + config.easyTeamLeaderBaseCount)
                * config.easyMonsterMultiplier;
            int budget = Math.round(baseEasyBudget * template.roomCount / 16f);
            List<String> species = new ArrayList<>(profile.minimumSpecies);
            while (species.size() < budget)
                species.add(profile.monsterTypes[m.weightedIndex(profile.monsterWeights)]);
            for (String id : species)
                if (profile.excludedMonsterIds.contains(id))
                    throw new GenerationException("excluded monster drawn: " + id);
            List<float[]> eligible = new ArrayList<>(), corners = new ArrayList<>();
            collectMonsterCells(room, logical, reachable, spawn, config, eligible, corners);
            m.shuffle(eligible);
            m.shuffle(corners);
            List<float[]> placedPoints = new ArrayList<>();
            for (String id : species) {
                boolean leader = "manager".equals(id);
                if (leader) leaderTarget++;
                float[] point = pickSeparated(leader ? corners : eligible, placedPoints,
                    config.monsterSpacing);
                if (point == null) {
                    if (leader) continue; // 팀장 목표 축소(중앙 후보로 몰래 대체하지 않는다)
                    throw new GenerationException("monster pool exhausted: " + id);
                }
                placedPoints.add(point);
                if (leader) leaderPlaced++;
                monsters.add(new MonsterPlacement(monsterType(id), point[0], point[1]));
            }
        }

        // --- 시각 레이어(쉬움과 같은 경로): 비플레이 검은 영역 + 바닥 변형/오버레이.
        long visualSeed = new GenRng(seed, "visual").next();
        OverlayResolver.Result overlays =
            new OverlayResolver(OverlayResolver.DEFAULT_SEED + visualSeed).resolve(logical, zones);
        String[][] floor = new FloorVariantResolver(FloorVariantResolver.DEFAULT_SEED + visualSeed)
            .resolve(logical, zones);
        String[][] wallSkin = new String[h][w], structure = new String[h][w];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            boolean occupied = logical[y][x] != TileType.WALL || adjacentFloor(logical, x, y);
            if (!occupied) {
                floor[y][x] = null;
                overlays.overlay[y][x] = null;
            }
        }
        RoomVisuals visuals = new RoomVisuals(floor, overlays.overlay, wallSkin, structure, null, props);
        List<LaboratoryRoomArea> areas = new ArrayList<>();
        for (GenRoom genRoom : rooms.values())
            areas.add(new LaboratoryRoomArea(genRoom.theme + "_" + genRoom.id,
                genRoom.x, genRoom.y, genRoom.w, genRoom.h));
        LaboratoryLayout layout = LaboratoryLayout.createForGenerator(
            room, visuals, containers, areas, coverage, true);

        // --- 메타데이터/통계.
        int maxCorridor = 0, sumCorridor = 0;
        for (int length : corridorLengths) {
            maxCorridor = Math.max(maxCorridor, length);
            sumCorridor += length;
        }
        String stats = "rooms=" + rooms.size() + " edges=" + chosen.size()
            + " corridorAvg=" + (corridorLengths.isEmpty() ? 0 : sumCorridor / corridorLengths.size())
            + " corridorMax=" + maxCorridor
            + " props=" + props.size()
            + " containers=" + containers.size() + " containersDropped=" + droppedContainers
            + " monsters=" + monsters.size()
            + " leaders=" + leaderPlaced + "/" + leaderTarget
            + " shapeDowngrades=" + shapeDowngrades;
        String contentHash = contentHash(logical, props, containers, monsters,
            template.templateId, seed);
        return new Result(layout, monsters, difficulty, requestSeed, seed, template.templateId,
            template.templateVersion, contentHash, fallbackUsed, new ArrayList<>(failures), stats);
    }

    // ----- 보조 ----------------------------------------------------------------------

    private static String find(Map<String, String> parent, String id) {
        String root = id;
        while (!parent.get(root).equals(root)) root = parent.get(root);
        return root;
    }

    private static int clampDoor(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static boolean nearNotch(int[] door, GenRoom room, int nx, int ny, int nw, int nh) {
        if (!room.insideRect(door[0], door[1])
            && !(door[0] >= room.x - 1 && door[0] <= room.x + room.w
                 && door[1] >= room.y - 1 && door[1] <= room.y + room.h)) return false;
        return door[0] >= nx - 2 && door[0] < nx + nw + 2
            && door[1] >= ny - 2 && door[1] < ny + nh + 2;
    }

    private static boolean insideAnyRoom(Iterable<GenRoom> rooms, int x, int y) {
        for (GenRoom room : rooms) if (room.insideRect(x, y)) return true;
        return false;
    }

    /** 문 전실(5칸) + 도그레그(두께 3) 복도. 방 사각형(벽 포함) 안은 절대 깎지 않는다. */
    private static void carveCorridor(TileType[][] logical, Iterable<GenRoom> rooms,
                                      Edge edge, int[] doorA, int[] doorB) {
        List<int[]> path = new ArrayList<>();
        if (edge.horizontal) {
            int fromX = doorA[0] + 1, toX = doorB[0] - 1;
            int midX = (fromX + toX) / 2;
            for (int x = fromX; x <= midX; x++) path.add(new int[]{x, doorA[1]});
            for (int y = Math.min(doorA[1], doorB[1]); y <= Math.max(doorA[1], doorB[1]); y++)
                path.add(new int[]{midX, y});
            for (int x = midX; x <= toX; x++) path.add(new int[]{x, doorB[1]});
            carveCell5(logical, rooms, doorA[0] + 1, doorA[1], false);
            carveCell5(logical, rooms, doorB[0] - 1, doorB[1], false);
        } else {
            int fromY = doorA[1] + 1, toY = doorB[1] - 1;
            int midY = (fromY + toY) / 2;
            for (int y = fromY; y <= midY; y++) path.add(new int[]{doorA[0], y});
            for (int x = Math.min(doorA[0], doorB[0]); x <= Math.max(doorA[0], doorB[0]); x++)
                path.add(new int[]{x, midY});
            for (int y = midY; y <= toY; y++) path.add(new int[]{doorB[0], y});
            carveCell5(logical, rooms, doorA[0], doorA[1] + 1, true);
            carveCell5(logical, rooms, doorB[0], doorB[1] - 1, true);
        }
        java.util.Set<Long> unique = new java.util.LinkedHashSet<>();
        for (int[] p : path) {
            unique.add((long) p[1] * 100000L + p[0]);
            carve(logical, rooms, p[0], p[1]);
            carve(logical, rooms, p[0] + 1, p[1]);
            carve(logical, rooms, p[0] - 1, p[1]);
            carve(logical, rooms, p[0], p[1] + 1);
            carve(logical, rooms, p[0], p[1] - 1);
        }
        edge.centerlineLength = unique.size();
    }

    /** 문 바로 바깥의 5칸 직선 지지 전실(DoorPlacementResolver 요구). */
    private static void carveCell5(TileType[][] logical, Iterable<GenRoom> rooms,
                                   int cx, int cy, boolean horizontalRun) {
        for (int offset = -2; offset <= 2; offset++)
            carve(logical, rooms, horizontalRun ? cx + offset : cx,
                horizontalRun ? cy : cy + offset);
    }

    private static void carve(TileType[][] logical, Iterable<GenRoom> rooms, int x, int y) {
        if (y < 2 || x < 2 || y >= logical.length - 2 || x >= logical[0].length - 2) return;
        if (insideAnyRoom(rooms, x, y)) return;
        logical[y][x] = TileType.FLOOR;
    }

    private static boolean isResearchTheme(String theme) {
        switch (theme) {
            case "chemistry": case "biology": case "general_lab": case "preparation":
            case "specimen": case "information": case "cctv": case "records": case "library":
                return true;
            default:
                return false;
        }
    }

    /** 목표 수만큼 방을 순회하며 placeContainer(기존 가중 추첨)를 시도. 실패 수를 돌려준다. */
    private static int assignContainers(List<LootContainerPlacement> out, String id, int target,
                                        List<GenRoom> roomOrder, TileType[][] logical,
                                        List<LaboratoryProp> props, java.util.Random random,
                                        Vector2 spawn) {
        int placed = 0, attempts = 0, index = 0;
        while (placed < target && attempts < roomOrder.size() * 3) {
            GenRoom room = roomOrder.get(index++ % roomOrder.size());
            attempts++;
            LootContainerPlacement placement = LaboratoryLayout.placeContainer(id,
                room.interiorFloorMinX(), room.interiorFloorMinY(),
                room.interiorFloorMaxX(), room.interiorFloorMaxY(),
                logical, props, out, random, spawn.x, spawn.y);
            if (placement == null) continue;
            out.add(placement);
            placed++;
        }
        return target - placed;
    }

    /** 배우(플레이어 몸체) 기준 4방향 BFS. 닫힌 문 셀은 "도달하면 열 수 있는" 통로로 취급. */
    static boolean[][] reachability(LaboratoryRoom room, int startX, int startY,
                                    BalanceConfig config) {
        int w = room.widthInTiles(), h = room.heightInTiles();
        boolean[][] seen = new boolean[h][w];
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{startX, startY});
        seen[startY][startX] = true;
        while (!queue.isEmpty()) {
            int[] p = queue.poll();
            int[][] next = {{p[0] + 1, p[1]}, {p[0] - 1, p[1]}, {p[0], p[1] + 1}, {p[0], p[1] - 1}};
            for (int[] n : next) {
                if (n[0] < 0 || n[1] < 0 || n[0] >= w || n[1] >= h || seen[n[1]][n[0]]) continue;
                TileType raw = room.rawTileAt(n[0], n[1]);
                boolean doorCell = raw == TileType.DOORWAY_OPEN || raw == TileType.DOOR_CLOSED;
                if (!doorCell && room.blocksActorAtCell(n[0], n[1],
                    config.playerBoundsWidth, config.playerBoundsHeight)) continue;
                seen[n[1]][n[0]] = true;
                queue.add(n);
            }
        }
        return seen;
    }

    /** 도달 불가 방의 중앙 섬 가구 제거(벽면 가구는 유지 — 방 품질 저하 최소화). */
    private static void removeIslandProps(List<LaboratoryProp> props, GenRoom room) {
        java.util.Iterator<LaboratoryProp> it = props.iterator();
        while (it.hasNext()) {
            LaboratoryProp prop = it.next();
            Rectangle c = prop.collision();
            if (c == null) continue;
            boolean island = c.x > room.x + 2.2f && c.x + c.width < room.x + room.w - 2.2f
                && c.y > room.y + 2.2f && c.y + c.height < room.y + room.h - 2.2f;
            if (island) it.remove();
        }
    }

    private static boolean doorApproachable(boolean[][] reachable, int x, int y,
                                            TileType[][] logical) {
        int sides = 0;
        int[][] next = {{x + 1, y}, {x - 1, y}, {x, y + 1}, {x, y - 1}};
        for (int[] n : next) {
            if (n[1] < 0 || n[0] < 0 || n[1] >= logical.length || n[0] >= logical[0].length) continue;
            if (logical[n[1]][n[0]] == TileType.FLOOR && reachable[n[1]][n[0]]) sides++;
        }
        return sides >= 2; // 양방향에서 문 앞에 도달해 열 수 있어야 한다
    }

    private static boolean containerApproachable(boolean[][] reachable,
                                                 LootContainerPlacement placement) {
        float[][] offsets = {{0f, -1.1f}, {0f, 1.0f}, {-1.1f, 0f}, {1.1f, 0f}};
        for (float[] offset : offsets) {
            int x = (int) Math.floor(placement.x() + offset[0]);
            int y = (int) Math.floor(placement.y() + offset[1]);
            if (y >= 0 && x >= 0 && y < reachable.length && x < reachable[0].length
                && reachable[y][x]) return true;
        }
        return false;
    }

    /** 몬스터 후보: 도달 가능 + 문 3×3 밖 + 스폰 안전 반경 밖. corners는 두 벽이 만나는 구석. */
    private static void collectMonsterCells(LaboratoryRoom room, TileType[][] logical,
                                            boolean[][] reachable, Vector2 spawn,
                                            BalanceConfig config,
                                            List<float[]> eligible, List<float[]> corners) {
        float exclusion2 = Math.max(36f,
            config.monsterSpawnExclusionRadius * config.monsterSpawnExclusionRadius);
        for (int y = 2; y < room.heightInTiles() - 2; y++) {
            for (int x = 2; x < room.widthInTiles() - 2; x++) {
                if (!reachable[y][x] || logical[y][x] != TileType.FLOOR) continue;
                float px = x + .5f, py = y + .5f;
                float dx = px - spawn.x, dy = py - spawn.y;
                if (dx * dx + dy * dy < exclusion2) continue;
                if (room.blocksActorAtCell(x, y,
                    config.monsterBoundsWidth, config.monsterBoundsHeight)) continue;
                boolean nearDoor = false;
                for (int oy = -1; oy <= 1 && !nearDoor; oy++)
                    for (int ox = -1; ox <= 1; ox++) {
                        TileType raw = room.rawTileAt(x + ox, y + oy);
                        if (raw == TileType.DOORWAY_OPEN || raw == TileType.DOOR_CLOSED) {
                            nearDoor = true;
                            break;
                        }
                    }
                if (nearDoor) continue;
                eligible.add(new float[]{px, py});
                // 구석: 두 직교 방향이 벽(폴리곤 경계 — 노치 포함)이고 안쪽 2×2가 서 있을 수
                // 있는 바닥이어야 팀장 후보다(문 앞·가구·캐비넷 겹침은 위 검사로 이미 제외).
                boolean wallN = wallAt(logical, x, y + 1), wallS = wallAt(logical, x, y - 1);
                boolean wallE = wallAt(logical, x + 1, y), wallW = wallAt(logical, x - 1, y);
                if ((wallN || wallS) && (wallE || wallW)) {
                    int ix = wallE ? x - 1 : x + 1, iy = wallN ? y - 1 : y + 1;
                    if (!room.blocksActorAtCell(ix, y, config.monsterBoundsWidth, config.monsterBoundsHeight)
                        && !room.blocksActorAtCell(x, iy, config.monsterBoundsWidth, config.monsterBoundsHeight))
                        corners.add(new float[]{px, py});
                }
            }
        }
    }

    private static boolean wallAt(TileType[][] logical, int x, int y) {
        return y < 0 || x < 0 || y >= logical.length || x >= logical[0].length
            || logical[y][x] == TileType.WALL;
    }

    private static float[] pickSeparated(List<float[]> pool, List<float[]> placed, float spacing) {
        float spacing2 = spacing * spacing;
        for (float[] point : pool) {
            boolean ok = true;
            for (float[] other : placed) {
                float dx = point[0] - other[0], dy = point[1] - other[1];
                if (dx * dx + dy * dy < spacing2) { ok = false; break; }
            }
            if (ok) return point;
        }
        return null;
    }

    private static MonsterType monsterType(String id) {
        switch (id) {
            case "slime": return MonsterType.SLIME;
            case "researcher": return MonsterType.RESEARCHER;
            case "guard": return MonsterType.GUARD;
            case "manager": return MonsterType.TEAM_LEADER;
            default: throw new GenerationException("unknown monster id " + id);
        }
    }

    private static boolean adjacentFloor(TileType[][] map, int x, int y) {
        for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
            int nx = x + dx, ny = y + dy;
            if (nx >= 0 && ny >= 0 && ny < map.length && nx < map[0].length
                && (map[ny][nx] == TileType.FLOOR || map[ny][nx] == TileType.DOORWAY_OPEN))
                return true;
        }
        return false;
    }

    /** FNV-1a32 기반 재현 해시(논리 타일·가구·컨테이너·몬스터·틀·시드). */
    private static String contentHash(TileType[][] logical, List<LaboratoryProp> props,
                                      List<LootContainerPlacement> containers,
                                      List<MonsterPlacement> monsters,
                                      String templateId, String seed) {
        int h = 0x811C9DC5;
        StringBuilder sb = new StringBuilder(templateId).append('|').append(seed).append('|');
        for (TileType[] row : logical) for (TileType t : row) sb.append(t.ordinal());
        for (LaboratoryProp prop : props)
            sb.append(prop.id()).append(prop.x()).append(prop.y());
        for (LootContainerPlacement c : containers)
            sb.append(c.id()).append(c.x()).append(c.y());
        for (MonsterPlacement m : monsters)
            sb.append(m.type).append(m.x).append(m.y);
        for (int i = 0; i < sb.length(); i++) {
            h ^= sb.charAt(i);
            h *= 16777619;
        }
        return Integer.toHexString(h);
    }
}

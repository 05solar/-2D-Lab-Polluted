package io.github.some_example_name.world;

import com.badlogic.gdx.math.Vector2;

/**
 * 테스트 방의 "설계"를 담당한다: 네 구역 구성 + 논리 타일 + 문/구조물 + 4레이어 시각 배치를
 * Laboratory Tileset V2 타일 ID로 한 번에 빌드한다. Screen/Renderer에는 생성 규칙을 두지 않는다.
 *
 * 문자 범례(위에서 아래로 작성, 내부 배열은 y가 위로 증가하도록 뒤집어 저장):
 *   '#' 벽(시각은 이웃 기반 오토타일)   '.' 기본 바닥(+'P' 스폰)
 *   'o' 가로 열린 출입구(남쪽)          'd' 세로 닫힌 격리문      'g' 세로 열린 문(통로)
 *
 * 구역(화면만으로 구분 가능하도록):
 *   - 남서 입구/안전: 깨끗한 바닥 + 남쪽 외벽 열린 출입구 'o' + 스폰 주변 3x3 청결.
 *   - 중앙 실험 허브: 넓은 바닥(변형·연결선·보수판·해치), 네 구역 이동 동선.
 *   - 북동 오염 격리: 외벽+내부 벽 밀폐, 유일 출입 = 세로 닫힌 격리문 'd'. 독성 오버레이 불규칙 군집.
 *   - 남동 정비/전력: 통로. 'g'(세로 열린 문)로 중앙과 연결. 연결 전선 + 스파크(감전) 오버레이.
 *
 * 시각 배치(바닥 변형/오버레이)는 고정 seed 리졸버로 산출 → 실행마다 동일.
 */
public final class LaboratoryLayout {

    private static final String[] TEST_ROOM = {
        "####################", // ty14 북쪽 외벽
        "#...........#......#", // ty13 (북동) 오염 격리실
        "#...........#......#", // ty12
        "#...........d......#", // ty11 'd' 세로 닫힌 격리문(유일 출입, 통과 불가)
        "#...........#......#", // ty10
        "#...........#......#", // ty9
        "#...........########", // ty8  오염 격리실 남쪽 벽
        "#..................#", // ty7  중앙↔우측 연결 통로(경고선 경계)
        "#...........########", // ty6  정비실 북쪽 벽
        "#...........#......#", // ty5  (남동) 정비실
        "#...........#......#", // ty4
        "#...........g......#", // ty3  'g' 세로 열린 문(정비실 입구 통로)
        "#..P........#......#", // ty2  'P' 스폰(남서 안전 구역)
        "#...........#......#", // ty1
        "###o################", // ty0  'o' 남쪽 열린 출입구
    };

    private final LaboratoryRoom room;
    private final RoomVisuals visuals;

    private LaboratoryLayout(LaboratoryRoom room, RoomVisuals visuals) {
        this.room = room;
        this.visuals = visuals;
    }

    public LaboratoryRoom room() { return room; }
    public RoomVisuals visuals() { return visuals; }

    public static LaboratoryLayout testRoom() {
        return fromRows(TEST_ROOM);
    }

    public static LaboratoryLayout fromRows(String[] rows) {
        int h = rows.length;
        int w = rows[0].length();

        TileType[][] logical = new TileType[h][w];
        String[][] structure = new String[h][w];
        LaboratoryZone[][] zones = new LaboratoryZone[h][w];
        Vector2 spawn = new Vector2(w / 2f, h / 2f);

        for (int r = 0; r < h; r++) {
            int ty = h - 1 - r; // 템플릿 맨 윗줄 = 가장 큰 y
            String row = rows[r];
            for (int tx = 0; tx < w; tx++) {
                char c = row.charAt(tx);
                logical[ty][tx] = logicalFor(c);
                structure[ty][tx] = structureFor(c);
                zones[ty][tx] = zoneFor(tx, ty, w, h);
                if (c == 'P') spawn = new Vector2(tx + 0.5f, ty + 0.5f);
            }
        }

        // 오버레이 + hazard (시각/규칙 분리)
        OverlayResolver.Result ovr = new OverlayResolver(OverlayResolver.DEFAULT_SEED).resolve(logical, zones);
        LaboratoryRoom room = new LaboratoryRoom(logical, spawn, ovr.hazard);

        // 벽 오토타일
        WallAutotiler autotiler = new WallAutotiler();
        String[][] wall = new String[h][w];
        for (int ty = 0; ty < h; ty++) {
            for (int tx = 0; tx < w; tx++) {
                if (logical[ty][tx] == TileType.WALL) {
                    wall[ty][tx] = autotiler.tileId(room, tx, ty);
                }
            }
        }

        // 바닥 변형 + 경고선 오버라이드(오염 구역 남쪽 경계)
        String[][] floor = new FloorVariantResolver(FloorVariantResolver.DEFAULT_SEED).resolve(logical, zones);
        applyWarningBoundary(floor, w, h);

        RoomVisuals visuals = new RoomVisuals(floor, ovr.overlay, wall, structure);
        return new LaboratoryLayout(room, visuals);
    }

    /** 오염 격리실 남쪽 경계(중앙 통로 쪽)에 짧은 경고선 2칸(3연속 금지 준수). */
    private static void applyWarningBoundary(String[][] floor, int w, int h) {
        // 테스트 방 전용 좌표(오염실 남쪽 벽 ty8 바로 아래 ty7)
        if (w == 20 && h == 15) {
            if (floor[7][13] != null) floor[7][13] = "floor_warning_north";
            if (floor[7][14] != null) floor[7][14] = "floor_warning_north";
        }
    }

    static TileType logicalFor(char c) {
        switch (c) {
            case '#': return TileType.WALL;
            case 'd': return TileType.DOOR_CLOSED;   // 세로 닫힌 문
            case 'h': return TileType.DOOR_CLOSED;   // 가로 닫힌 문
            case 'o':                                // 가로 열린 출입구
            case 'g': return TileType.DOORWAY_OPEN;  // 세로 열린 문
            default:  return TileType.FLOOR;         // '.', 'P'
        }
    }

    /** 문/구조물 셀의 타일 ID(그 외 null). 문 방향은 벽 방향과 일치시킨다. */
    static String structureFor(char c) {
        switch (c) {
            case 'o': return "horizontal_door_open";
            case 'h': return "horizontal_door_closed";
            case 'd': return "vertical_door_closed";
            case 'g': return "vertical_door_open";
            default:  return null;
        }
    }

    static LaboratoryZone zoneFor(int tx, int ty, int w, int h) {
        if (w != 20 || h != 15) return LaboratoryZone.CENTRAL; // 임의 크기 테스트 방
        if (tx >= 13 && ty >= 9) return LaboratoryZone.CONTAM;
        if (tx >= 13 && ty >= 1 && ty <= 5) return LaboratoryZone.MAINT;
        if (tx >= 1 && tx <= 6 && ty >= 1 && ty <= 5) return LaboratoryZone.ENTRANCE;
        return LaboratoryZone.CENTRAL;
    }
}

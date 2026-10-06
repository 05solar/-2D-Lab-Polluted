package io.github.some_example_name.world;

import com.badlogic.gdx.math.Vector2;

/**
 * 테스트 방의 "설계"를 담당한다: 구역 구성 + 논리 타일 + 시각 타일 배치를 한 번에 빌드한다.
 * 맵은 고정 문자 데이터이므로 같은 입력이면 항상 같은 방이 재현된다(랜덤 없음).
 * 생성 규칙/배치는 여기 모으고, Screen/Renderer에는 두지 않는다.
 *
 * 문자 범례 (위에서 아래로 작성, 내부 배열은 y가 위로 증가하도록 뒤집어 저장):
 *   '#' 벽(시각은 이웃 기반 오토타일)   'D' 닫힌 문        'O' 열린 출입구
 *   '.' 기본 바닥(+'P' 스폰)            'c' 균열 바닥       's' 얼룩·약한 오염
 *   'w' 경고 바닥                       't' 독성(위험)      'e' 노출 전선(위험)
 *
 * 구역 구성:
 *   - 입구/안전(남서): 기본 바닥 중심, 남쪽 경계에 열린 출입구, 스폰 주변 3x3 기본 바닥.
 *   - 중앙 실험: 내부 장애물(실험 설비)과 그 주변 경고 바닥.
 *   - 오염(북동): 닫힌 문으로 격리된 밀폐 구역. 얼룩 바닥 + 독성 군집(2x2 / L자).
 *   - 정비(남동): 균열·노출 전선 위주의 좁은 통로(바닥 통로로 연결).
 */
public final class LaboratoryLayout {

    private static final String[] TEST_ROOM = {
        "####################", // ty14
        "#cc......w#sssttss.#", // ty13  NW 균열 / NE 오염(독성 2x2)
        "#cc......w#sssttss.#", // ty12
        "#........w#ssssssss#", // ty11
        "#........wDsstt.sss#", // ty10  'D' 닫힌 문(중앙↔오염)
        "#...wwww.w#ss.t.sss#", // ty9   경고 링 상단 / 독성 L
        "#...w##w.w#ssssssss#", // ty8   실험 설비(내부 장애물)
        "#...w##w..##########", // ty7   오염 남쪽 밀폐 벽
        "#...wwww...#########", // ty6   정비 북쪽 벽
        "#.......ww.#ccee..c#", // ty5   실험 경계 경고(시작 화면 가시) / 정비 균열·전선
        "#.....ccww.#c.eecc.#", // ty4   입구 균열 군집 + 경고
        "#.....cc.....eeccee#", // ty3   입구 균열 군집 / 정비 입구(통로)
        "#..P.......#ccee..e#", // ty2   'P' 스폰(안전 구역, 주변 3x3 기본 바닥)
        "#..........#.cceeec#", // ty1
        "###O################", // ty0   'O' 열린 출입구(입구)
    };

    private final LaboratoryRoom room;
    private final RoomVisuals visuals;

    private LaboratoryLayout(LaboratoryRoom room, RoomVisuals visuals) {
        this.room = room;
        this.visuals = visuals;
    }

    public LaboratoryRoom room() {
        return room;
    }

    public RoomVisuals visuals() {
        return visuals;
    }

    public static LaboratoryLayout testRoom() {
        return fromRows(TEST_ROOM);
    }

    static LaboratoryLayout fromRows(String[] rows) {
        int h = rows.length;
        int w = rows[0].length();

        TileType[][] logical = new TileType[h][w];
        TileVisual[][] visual = new TileVisual[h][w];
        Vector2 spawn = new Vector2(w / 2f, h / 2f); // 폴백

        for (int r = 0; r < h; r++) {
            int ty = h - 1 - r; // 문자 템플릿의 맨 윗줄 = 가장 큰 y
            String row = rows[r];
            for (int tx = 0; tx < w; tx++) {
                char c = row.charAt(tx);
                logical[ty][tx] = logicalFor(c);
                visual[ty][tx] = baseVisualFor(c); // 벽('#')은 아래에서 오토타일
                if (c == 'P') {
                    spawn = new Vector2(tx + 0.5f, ty + 0.5f);
                }
            }
        }

        LaboratoryRoom room = new LaboratoryRoom(logical, spawn);

        WallAutotiler autotiler = new WallAutotiler();
        for (int ty = 0; ty < h; ty++) {
            for (int tx = 0; tx < w; tx++) {
                if (logical[ty][tx] == TileType.WALL) {
                    visual[ty][tx] = autotiler.pick(room, tx, ty);
                }
            }
        }

        return new LaboratoryLayout(room, new RoomVisuals(visual));
    }

    static TileType logicalFor(char c) {
        switch (c) {
            case '#': return TileType.WALL;
            case 'D': return TileType.DOOR_CLOSED;
            case 'O': return TileType.DOORWAY_OPEN;
            case 't':
            case 'e': return TileType.HAZARD;
            default:  return TileType.FLOOR; // '.', 'P', 'c', 's', 'w'
        }
    }

    /** 벽('#')은 오토타일 대상이므로 null을 반환한다(빌드 중 채워짐). */
    static TileVisual baseVisualFor(char c) {
        switch (c) {
            case '#': return null;
            case 'D': return TileVisual.DOOR_CLOSED;
            case 'O': return TileVisual.DOORWAY_OPEN;
            case 'c': return TileVisual.FLOOR_CRACKED;
            case 's': return TileVisual.FLOOR_STAIN;
            case 'w': return TileVisual.FLOOR_WARNING;
            case 't': return TileVisual.FLOOR_TOXIC;
            case 'e': return TileVisual.FLOOR_WIRES;
            case '.':
            case 'P':
            default:  return TileVisual.FLOOR_BASIC;
        }
    }
}

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
 * 구역 구성(화면만으로 구분 가능하도록 구성):
 *   - 입구/안전(남서): 깨끗한 기본 바닥. 남쪽 외벽에 열린 출입구('O'), 스폰 주변 3x3 청결.
 *   - 중앙 실험(허브): 넓은 기본 바닥 + 미세한 균열. 네 구역을 잇는 이동 동선.
 *   - 오염 격리(북동): 외벽+내부 벽으로 둘러싸인 밀폐 구역. 유일한 출입은 닫힌 격리문('D').
 *       내부에 독성('t')·얼룩('s')을 2~5칸 불규칙 군집으로 배치(사각형 반복 금지).
 *   - 정비/전력(남동): 좁은 통로. 노출 전선('e')·균열('c')을 2~3개 구간에만. 'ty3' 통로로 중앙과 연결.
 *
 * 설계 메모(에셋 제약):
 *   - 실험대/서버랙 등 가구는 전용 에셋(laboratory_furniture_*, laboratory_small_props)이 있어야
 *     배치할 수 있다. 현재 프로젝트에 해당 에셋이 없으므로(가짜 사각형·벽 타일로 대체하지 않음)
 *     중앙 실험 구역은 가구 없이 비워 두었다. docs/process.md의 "가구 에셋 누락" 참고.
 *   - 경고 타일(TileVisual.FLOOR_WARNING)은 좌상단에 줄무늬가 있는 모서리 마킹이라,
 *     위험 구역(오염·정비)의 좌상단 모서리 1칸에만 배치해 "위험 구역 코너" 표식으로 쓴다.
 *   - 바닥 중립 타일이 1종뿐이라 변형(균열·얼룩)을 과하게 섞으면 더 지저분해진다.
 *     기본 바닥 비율을 높게(약 88%) 유지하고 변형은 테마에 맞는 소수 군집으로만 둔다.
 *
 * 이 맵은 고정 문자 데이터다. 배치(독성/균열 군집, 변형 산포)는 고정 seed 생성기
 * (tools 미리보기 gen.py, seed=20251006)로 1회 산출한 결과를 그대로 베이크한 것이라
 * 실행할 때마다 항상 동일하다(= "동일 seed → 동일 레이아웃"을 데이터로 보장).
 */
public final class LaboratoryLayout {

    private static final String[] TEST_ROOM = {
        "####################", // ty14  북쪽 외벽
        "#...........#w...s.#", // ty13  (북동) 오염 격리실: 좌상단 경고 'w' + 얼룩
        "#...........#...st.#", // ty12
        "#.........c.D..stt.#", // ty11  'D' 닫힌 격리문(유일한 출입, 통과 불가)
        "#...........#tt.t..#", // ty10  오염 독성 군집(불규칙)
        "#.......c..c#.t....#", // ty9
        "#..c........########", // ty8   오염 격리실 남쪽 벽
        "#...c..............#", // ty7   중앙↔우측 연결 통로
        "#...........########", // ty6   정비실 북쪽 벽
        "#...........#w..e..#", // ty5   (남동) 정비실: 좌상단 경고 'w'
        "#...........#..cee.#", // ty4   노출 전선 군집
        "#..................#", // ty3   정비실 입구 통로(tx12 개방)
        "#..P........#.eec..#", // ty2   'P' 스폰(남서 안전 구역, 주변 3x3 청결)
        "#...........#.c....#", // ty1
        "###O################", // ty0   'O' 열린 출입구(남쪽) + 남쪽 외벽
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

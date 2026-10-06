package io.github.some_example_name.world;

/**
 * 방의 셀별 시각 타일 배치. 한 번 계산되면 실행 중 변하지 않는다(프레임마다 재선택하지 않음).
 * 렌더러는 이 결과를 "그리기만" 한다. 배치/선택 로직은 여기에 두지 않는다.
 * 내부 배열은 [ty][tx], ty=0 이 맨 아래 줄(월드 y는 위로 증가).
 */
public class RoomVisuals {

    private final TileVisual[][] visuals; // [ty][tx]

    public RoomVisuals(TileVisual[][] visuals) {
        this.visuals = visuals;
    }

    public TileVisual visualAt(int tileX, int tileY) {
        return visuals[tileY][tileX];
    }

    /** 해당 시각 타일이 방 안에 몇 개 있는지 센다(진단/검증용). */
    public int count(TileVisual visual) {
        int n = 0;
        for (TileVisual[] row : visuals) {
            for (TileVisual v : row) {
                if (v == visual) {
                    n++;
                }
            }
        }
        return n;
    }

    public int widthInTiles() {
        return visuals[0].length;
    }

    public int heightInTiles() {
        return visuals.length;
    }
}

package io.github.some_example_name.world;

/**
 * 방의 셀별 "시각" 배치를 레이어별 타일 ID로 보관한다. 한 번 계산되면 실행 중 변하지 않는다
 * (프레임마다 재선택/랜덤 없음). 렌더러는 이 결과를 레이어 순서대로 그리기만 한다.
 * 내부 배열은 [ty][tx], ty=0 이 맨 아래 줄(월드 y는 위로 증가).
 *
 * 레이어(없으면 null):
 *   floor      — 모든 통과 가능 셀의 기본/변형 바닥
 *   overlay    — 바닥 위 데칼(오염/전선/장식). 바닥 리전을 교체하지 않는다.
 *   wall       — 벽 셀의 오토타일 결과(wall_*, T자/십자 포함)
 *   structure  — 문/구조물(바닥·벽과 별도 레이어)
 */
public class RoomVisuals {

    private final String[][] floor;
    private final String[][] overlay;
    private final String[][] wall;
    private final String[][] structure;
    private final int width;
    private final int height;

    public RoomVisuals(String[][] floor, String[][] overlay, String[][] wall, String[][] structure) {
        this.floor = floor;
        this.overlay = overlay;
        this.wall = wall;
        this.structure = structure;
        this.height = floor.length;
        this.width = floor[0].length;
    }

    public String floorAt(int tx, int ty)     { return floor[ty][tx]; }
    public String overlayAt(int tx, int ty)   { return overlay[ty][tx]; }
    public String wallAt(int tx, int ty)      { return wall[ty][tx]; }
    public String structureAt(int tx, int ty) { return structure[ty][tx]; }

    public int widthInTiles()  { return width; }
    public int heightInTiles() { return height; }

    /** 특정 레이어에서 해당 타일 ID가 몇 번 쓰였는지(진단/검증용). */
    public int count(String layer, String id) {
        String[][] a = layerArray(layer);
        int n = 0;
        for (String[] row : a) {
            for (String v : row) {
                if (id.equals(v)) n++;
            }
        }
        return n;
    }

    private String[][] layerArray(String layer) {
        switch (layer) {
            case "floor":     return floor;
            case "overlay":   return overlay;
            case "wall":      return wall;
            case "structure": return structure;
            default: throw new IllegalArgumentException("알 수 없는 레이어: " + layer);
        }
    }
}

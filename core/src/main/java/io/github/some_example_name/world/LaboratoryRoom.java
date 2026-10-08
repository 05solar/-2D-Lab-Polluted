package io.github.some_example_name.world;

import com.badlogic.gdx.math.Vector2;
import io.github.some_example_name.collision.SolidGrid;

/**
 * 타일 기반 연구소 방의 "논리/충돌" 모델. 월드 단위는 1 타일 = 1 단위이며 y축은 위로 증가한다.
 * 내부 배열은 tiles[ty][tx], ty=0 이 맨 아래 줄이다.
 *
 * 렌더링 자원(Texture 등)과 시각 배치를 소유하지 않는다. 시각은 {@link RoomVisuals},
 * 설계/배치는 {@link LaboratoryLayout}이 담당한다. 여기서는 논리 타일·충돌·위험 질의만 제공한다.
 */
public class LaboratoryRoom implements SolidGrid {

    private final TileType[][] tiles;   // [ty][tx], 논리 타일
    private final Hazard[][] hazards;   // [ty][tx], 위험 데이터(오버레이에서 유도)
    private final Door[][] doors;        // 위치·방향 고정, 상태로 충돌 판정
    private final int widthInTiles;
    private final int heightInTiles;
    private final Vector2 spawnPoint;

    public LaboratoryRoom(TileType[][] tiles, Vector2 spawnPoint) {
        this(tiles, spawnPoint, null);
    }

    public LaboratoryRoom(TileType[][] tiles, Vector2 spawnPoint, Hazard[][] hazards) {
        this(tiles, spawnPoint, hazards, null);
    }

    public LaboratoryRoom(TileType[][] tiles, Vector2 spawnPoint, Hazard[][] hazards, Door[][] doors) {
        this.tiles = tiles;
        this.heightInTiles = tiles.length;
        this.widthInTiles = tiles[0].length;
        this.spawnPoint = spawnPoint;
        this.hazards = hazards;
        this.doors = doors;
    }

    public TileType tileAt(int tileX, int tileY) {
        if (tileX < 0 || tileY < 0 || tileX >= widthInTiles || tileY >= heightInTiles) {
            return TileType.WALL; // 범위 밖은 벽으로 취급(충돌 기준)
        }
        Door door = doorAt(tileX, tileY);
        return door == null ? tiles[tileY][tileX] : door.tileType();
    }

    public Door doorAt(int tileX, int tileY) {
        if (doors == null || tileX < 0 || tileY < 0 ||
            tileX >= widthInTiles || tileY >= heightInTiles) return null;
        return doors[tileY][tileX];
    }

    @Override
    public boolean isSolid(int tileX, int tileY) {
        return tileAt(tileX, tileY).solid;
    }

    /** 위험 종류(독성/감전). 데이터가 없거나 범위 밖이면 NONE. */
    public Hazard hazardAt(int tileX, int tileY) {
        if (hazards == null || tileX < 0 || tileY < 0 || tileX >= widthInTiles || tileY >= heightInTiles) {
            return Hazard.NONE;
        }
        return hazards[tileY][tileX];
    }

    /** 위험 지역 여부(독성 또는 감전). 1단계에서는 의미만 제공하고 피해 로직은 없다. */
    public boolean isHazard(int tileX, int tileY) {
        return hazardAt(tileX, tileY) != Hazard.NONE;
    }

    @Override
    public int widthInTiles() { return widthInTiles; }

    @Override
    public int heightInTiles() { return heightInTiles; }

    public Vector2 spawnPoint() { return spawnPoint; }

    /** 검증용 테스트 방(논리 모델). 시각 포함 전체 구성은 {@link LaboratoryLayout#testRoom()}. */
    public static LaboratoryRoom createTestRoom() {
        return LaboratoryLayout.testRoom().room();
    }
}

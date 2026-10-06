package io.github.some_example_name.world;

import com.badlogic.gdx.math.Vector2;
import io.github.some_example_name.collision.SolidGrid;

/**
 * 타일 기반 연구소 방의 "논리/충돌" 모델. 월드 단위는 1 타일 = 1 단위이며 y축은 위로 증가한다.
 * 내부 배열은 tiles[ty][tx], ty=0 이 맨 아래 줄이다.
 *
 * 렌더링 자원(Texture 등)과 시각 배치를 소유하지 않는다. 시각은 {@link RoomVisuals},
 * 설계/배치는 {@link LaboratoryLayout}이 담당한다. 여기서는 논리 타일과 충돌 질의만 제공한다.
 */
public class LaboratoryRoom implements SolidGrid {

    private final TileType[][] tiles; // [ty][tx], 논리 타일
    private final int widthInTiles;
    private final int heightInTiles;
    private final Vector2 spawnPoint; // 플레이어 시작 지점(타일 중심, 월드 단위)

    public LaboratoryRoom(TileType[][] tiles, Vector2 spawnPoint) {
        this.tiles = tiles;
        this.heightInTiles = tiles.length;
        this.widthInTiles = tiles[0].length;
        this.spawnPoint = spawnPoint;
    }

    public TileType tileAt(int tileX, int tileY) {
        if (tileX < 0 || tileY < 0 || tileX >= widthInTiles || tileY >= heightInTiles) {
            return TileType.WALL; // 범위 밖은 벽으로 취급
        }
        return tiles[tileY][tileX];
    }

    @Override
    public boolean isSolid(int tileX, int tileY) {
        return tileAt(tileX, tileY).solid;
    }

    /** 위험 지역(독성/전선) 여부. 1단계에서는 의미만 제공하고 피해 로직은 없다. */
    public boolean isHazard(int tileX, int tileY) {
        return tileAt(tileX, tileY).hazard;
    }

    @Override
    public int widthInTiles() {
        return widthInTiles;
    }

    @Override
    public int heightInTiles() {
        return heightInTiles;
    }

    public Vector2 spawnPoint() {
        return spawnPoint;
    }

    /** 1단계 검증용 테스트 방(논리 모델). 시각 포함 전체 구성은 {@link LaboratoryLayout#testRoom()}. */
    public static LaboratoryRoom createTestRoom() {
        return LaboratoryLayout.testRoom().room();
    }
}

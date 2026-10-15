package io.github.some_example_name.world;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Rectangle;
import io.github.some_example_name.collision.ObstacleGrid;
import io.github.some_example_name.collision.SolidGrid;
import java.util.ArrayList;
import java.util.List;

/**
 * 타일 기반 연구소 방의 "논리/충돌" 모델. 월드 단위는 1 타일 = 1 단위이며 y축은 위로 증가한다.
 * 내부 배열은 tiles[ty][tx], ty=0 이 맨 아래 줄이다.
 *
 * 렌더링 자원(Texture 등)과 시각 배치를 소유하지 않는다. 시각은 {@link RoomVisuals},
 * 설계/배치는 {@link LaboratoryLayout}이 담당한다. 여기서는 논리 타일·충돌·위험 질의만 제공한다.
 */
public class LaboratoryRoom implements ObstacleGrid {

    private final TileType[][] tiles;   // [ty][tx], 논리 타일
    private final Hazard[][] hazards;   // [ty][tx], 위험 데이터(오버레이에서 유도)
    private final Door[][] doors;        // 위치·방향 고정, 상태로 충돌 판정
    private final int widthInTiles;
    private final int heightInTiles;
    private final Vector2 spawnPoint;
    private final List<Rectangle> propObstacles = new ArrayList<>();
    // V4 벽 렌더 시 "바닥에 노출되지 않아도 반드시 그리는" 셀(예: 2타일 외벽 링). null이면 모두 false.
    private boolean[][] forcedWall;
    private boolean southExitFired;
    private float southExitMinX = 2f, southExitMaxX = 5f, southExitBoundaryY = 1f;

    public LaboratoryRoom(TileType[][] tiles, Vector2 spawnPoint) {
        this(tiles, spawnPoint, null);
    }

    public LaboratoryRoom(TileType[][] tiles, Vector2 spawnPoint, Hazard[][] hazards) {
        this(tiles, spawnPoint, hazards, null);
    }

    public LaboratoryRoom(TileType[][] tiles, Vector2 spawnPoint, Hazard[][] hazards, Door[][] doors) {
        this(tiles, spawnPoint, hazards, doors, null);
    }

    public LaboratoryRoom(TileType[][] tiles, Vector2 spawnPoint, Hazard[][] hazards, Door[][] doors,
                          List<LaboratoryProp> props) {
        this.tiles = tiles;
        this.heightInTiles = tiles.length;
        this.widthInTiles = tiles[0].length;
        this.spawnPoint = spawnPoint;
        this.hazards = hazards;
        this.doors = doors;
        if (props != null) for (LaboratoryProp prop : props)
            if (prop.collision() != null) propObstacles.add(prop.collision());
    }

    public TileType tileAt(int tileX, int tileY) {
        if (tileX < 0 || tileY < 0 || tileX >= widthInTiles || tileY >= heightInTiles) {
            return TileType.WALL; // 범위 밖은 벽으로 취급(충돌 기준)
        }
        Door door = doorAt(tileX, tileY);
        return door == null ? tiles[tileY][tileX] : door.tileType();
    }

    /**
     * 문 개폐와 무관한 "원시 논리 점유"(벽 마스크 계산용). 문 셀은 열림/닫힘과 상관없이 항상
     * 벽선에 연결된 개구부로 취급되도록 마커(DOOR_CLOSED/DOORWAY_OPEN)를 그대로 돌려준다.
     */
    public TileType rawTileAt(int tileX, int tileY) {
        if (tileX < 0 || tileY < 0 || tileX >= widthInTiles || tileY >= heightInTiles) {
            return TileType.WALL;
        }
        return tiles[tileY][tileX];
    }

    public Door doorAt(int tileX, int tileY) {
        if (doors == null || tileX < 0 || tileY < 0 ||
            tileX >= widthInTiles || tileY >= heightInTiles) return null;
        return doors[tileY][tileX];
    }

    /** 모든 내부 문(V4 문 렌더/동기화용). 없으면 빈 리스트. */
    public List<Door> doors() {
        List<Door> out = new ArrayList<>();
        if (doors == null) return out;
        for (int y = 0; y < heightInTiles; y++) for (int x = 0; x < widthInTiles; x++)
            if (doors[y][x] != null) out.add(doors[y][x]);
        return out;
    }

    /** Nearest internal door within radius, used for one-target interaction. */
    public Door nearestDoor(float worldX, float worldY, float radius) {
        if (doors == null || radius < 0f) return null;
        float best = radius * radius;
        Door nearest = null;
        for (int y = 0; y < heightInTiles; y++) for (int x = 0; x < widthInTiles; x++) {
            Door door = doors[y][x];
            if (door == null) continue;
            float dx = worldX - (door.x() + .5f), dy = worldY - (door.y() + .5f);
            float distance = dx * dx + dy * dy;
            if (distance <= best) { best = distance; nearest = door; }
        }
        return nearest;
    }

    @Override
    public boolean isSolid(int tileX, int tileY) {
        return tileAt(tileX, tileY).solid;
    }

    /** Path/spawn occupancy for a body centered in one tile, using exact shared AABBs. */
    public boolean blocksActorAtCell(int tileX, int tileY, float bodyWidth, float bodyHeight) {
        return blocksActorAt(tileX + .5f, tileY + .5f, bodyWidth, bodyHeight);
    }

    public boolean blocksActorAt(float feetX, float feetY, float bodyWidth, float bodyHeight) {
        Rectangle body = new Rectangle(feetX-bodyWidth*.5f, feetY, bodyWidth, bodyHeight);
        int minX=(int)Math.floor(body.x+.0001f), minY=(int)Math.floor(body.y+.0001f);
        int maxX=(int)Math.floor(body.x+body.width-.0001f), maxY=(int)Math.floor(body.y+body.height-.0001f);
        for(int y=minY;y<=maxY;y++) for(int x=minX;x<=maxX;x++) if(isSolid(x,y)) return true;
        for (Rectangle obstacle : propObstacles) if (body.overlaps(obstacle)) return true;
        // 닫힌 대문은 중앙 통로를 막는다(문틀 jamb은 외벽 WALL로 이미 solid). 열리면 통행 가능.
        if (gateClosed && gatePassage != null && body.overlaps(gatePassage)) return true;
        return false;
    }

    private Rectangle gatePassage;
    private boolean gateClosed;
    /** 대문 통로 사각형을 등록하고 기본 닫힘으로 둔다(통로 차단). */
    public void setGatePassage(Rectangle passage) {
        gatePassage = passage == null ? null : new Rectangle(passage);
        gateClosed = true;
    }
    public void setGateClosed(boolean closed) { gateClosed = closed; }
    public boolean gateClosed() { return gateClosed; }
    public boolean hasGate() { return gatePassage != null; }

    /** Shared static-obstacle sweep used by AI visibility and tile-route edges. */
    public boolean blocksActorAlongPath(float fromX, float fromY, float toX, float toY,
                                        float bodyWidth, float bodyHeight) {
        int steps=Math.max(1,(int)Math.ceil(Math.max(Math.abs(toX-fromX),Math.abs(toY-fromY))/.08f));
        for(int i=1;i<=steps;i++) {
            float t=i/(float)steps;
            if(blocksActorAt(fromX+(toX-fromX)*t,fromY+(toY-fromY)*t,bodyWidth,bodyHeight)) return true;
        }
        return false;
    }

    @Override public Iterable<Rectangle> obstacles() {
        List<Rectangle> copy = new ArrayList<>(propObstacles.size());
        for (Rectangle obstacle : propObstacles) copy.add(new Rectangle(obstacle));
        return copy;
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

    /** V4 벽 렌더: 바닥 비노출이라도 강제로 그릴 셀 마스크(외벽 링 등). */
    public void setForcedWallMask(boolean[][] mask) { this.forcedWall = mask; }
    public boolean forcedWall(int tileX, int tileY) {
        return forcedWall != null && tileY >= 0 && tileX >= 0
            && tileY < forcedWall.length && tileX < forcedWall[0].length && forcedWall[tileY][tileX];
    }

    public Vector2 spawnPoint() { return spawnPoint; }
    public void addWorldObstacle(Rectangle obstacle) {
        if (obstacle == null) throw new IllegalArgumentException("obstacle");
        propObstacles.add(new Rectangle(obstacle));
    }

    /** Single southbound crossing through the return gate's open three-cell span. */
    public boolean crossedSouthExit(float previousFeetY, float currentFeetY,
                                    float feetX, float moveY) {
        if (southExitFired || moveY >= 0f || previousFeetY < southExitBoundaryY
            || currentFeetY >= southExitBoundaryY || feetX < southExitMinX
            || feetX >= southExitMaxX)
            return false;
        southExitFired = true;
        return true;
    }
    public void setSouthExitGate(float minX, float maxX, float boundaryY) {
        if (minX < 0f || maxX <= minX || boundaryY <= 0f)
            throw new IllegalArgumentException("invalid south exit gate");
        southExitMinX = minX;
        southExitMaxX = maxX;
        southExitBoundaryY = boundaryY;
    }
    public int southExitCenterX() { return (int) ((southExitMinX + southExitMaxX) * 0.5f); }
    public int southExitPathY() { return Math.max(0, (int) southExitBoundaryY); }
    public void resetSouthExitTrigger() { southExitFired = false; }

    /** 검증용 테스트 방(논리 모델). 시각 포함 전체 구성은 {@link LaboratoryLayout#testRoom()}. */
    public static LaboratoryRoom createTestRoom() {
        return LaboratoryLayout.testRoom().room();
    }
}

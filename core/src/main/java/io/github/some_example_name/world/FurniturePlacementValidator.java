package io.github.some_example_name.world;

import com.badlogic.gdx.math.Rectangle;

/** Pure geometry checks for the visible alpha bounds and collision footprint of V7 art. */
final class FurniturePlacementValidator {
    private static final float EPSILON = .0001f;

    static Rectangle visibleBounds(LaboratoryProp prop) {
        String asset = prop.id().startsWith("v7:") ? prop.id().substring(3) : prop.id();
        float width = prop.height() * LaboratoryFurnitureCoverage.horizontalSpan(asset);
        float height = prop.height() * LaboratoryFurnitureCoverage.verticalSpan(asset);
        return new Rectangle(prop.x() - width * .5f, prop.y(), width, height);
    }

    static boolean isInsideRoomFloor(Rectangle bounds, RoomPlanBounds room, TileType[][] map) {
        if (bounds.x < room.minX || bounds.y < room.minY
            || bounds.x + bounds.width > room.maxX + EPSILON
            || bounds.y + bounds.height > room.maxY + EPSILON) return false;
        int minX = (int)Math.floor(bounds.x + EPSILON);
        int minY = (int)Math.floor(bounds.y + EPSILON);
        int maxX = (int)Math.floor(bounds.x + bounds.width - EPSILON);
        int maxY = (int)Math.floor(bounds.y + bounds.height - EPSILON);
        for (int y = minY; y <= maxY; y++) for (int x = minX; x <= maxX; x++) {
            if (y < 0 || y >= map.length || x < 0 || x >= map[0].length
                || map[y][x] != TileType.FLOOR) return false;
        }
        return true;
    }

    static boolean isOnWallSupport(Rectangle bounds, TileType[][] map) {
        int minX = (int)Math.floor(bounds.x + EPSILON), minY = (int)Math.floor(bounds.y + EPSILON);
        int maxX = (int)Math.floor(bounds.x + bounds.width - EPSILON);
        int maxY = (int)Math.floor(bounds.y + bounds.height - EPSILON);
        for (int y=minY; y<=maxY; y++) for (int x=minX; x<=maxX; x++)
            if (y < 0 || y >= map.length || x < 0 || x >= map[0].length
                    || map[y][x] != TileType.WALL) return false;
        return true;
    }

    static final class RoomPlanBounds {
        final float minX, minY, maxX, maxY;
        RoomPlanBounds(float minX, float minY, float maxX, float maxY) {
            this.minX = minX; this.minY = minY; this.maxX = maxX; this.maxY = maxY;
        }
    }

    private FurniturePlacementValidator() { }
}

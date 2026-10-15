package io.github.some_example_name.world;

import com.badlogic.gdx.math.Rectangle;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

/**
 * 컨테이너 배치 다양화 검증: 중앙 고정점 제거, 벽면/구석 배치 존재, 접근 가능한 서기 지점,
 * 문 접근로 확보, footprint 중첩 금지, 캐비넷의 정면 방향 규칙(북쪽 벽 수납만).
 */
public class ContainerPlacementTest {

    @Test public void easyContainersAreDiversifiedAndAccessible() {
        LaboratoryLayout layout = LaboratoryLayout.easyRoom();
        LaboratoryRoom room = layout.room();
        List<LootContainerPlacement> containers = layout.lootContainers();
        assertFalse(containers.isEmpty());
        int nearRoomCenter = 0, wallAdjacent = 0;
        for (LootContainerPlacement placement : containers) {
            assertAccessibleAndClearOfDoors(room, placement);
            assertCabinetFrontFacing(room, placement);
            LaboratoryRoomArea owner = owningRoom(layout, placement);
            assertNotNull("container sits inside a named room", owner);
            float dx = placement.x() - (owner.x() + owner.width() / 2f);
            float dy = placement.y() - (owner.y() + owner.height() / 2f);
            // "방 중앙의 정해진 점"(이전 규칙: 중앙 최근접 셀 고정) 판정 기준: 0.75칸 이내.
            if (dx * dx + dy * dy < .75f * .75f) nearRoomCenter++;
            if (adjacentToWall(room, placement)) wallAdjacent++;
        }
        // 이전 규칙에서는 모든 컨테이너가 방 중앙 최근접 셀이었다. 압축된 easy 방(내부 5~9칸,
        // 네 벽이 가구로 덮임)에서는 유효 후보가 중앙 쪽에 몰릴 수밖에 없으므로, "정중앙
        // 고정점이 사라졌고 벽면/구석 배치가 실제로 나온다"를 단언한다.
        assertTrue("containers are no longer pinned to exact room centers (center="
            + nearRoomCenter + "/" + containers.size() + ")",
            nearRoomCenter < containers.size() / 3);
        assertTrue("some containers use wall or corner storage spots (" + wallAdjacent + ")",
            wallAdjacent >= 2);
    }

    @Test public void variantContainersAreDiversifiedAndAccessible() {
        for (GameDifficulty difficulty : new GameDifficulty[]{GameDifficulty.NORMAL, GameDifficulty.HARD}) {
            LaboratoryLayout layout = LaboratoryLayout.forDifficulty(difficulty);
            List<LootContainerPlacement> containers = layout.lootContainers();
            assertEquals("container count preserved",
                difficulty == GameDifficulty.NORMAL ? 6 : 8, containers.size());
            for (LootContainerPlacement placement : containers) {
                assertAccessibleAndClearOfDoors(layout.room(), placement);
                assertCabinetFrontFacing(layout.room(), placement);
                // 시작 지점 안전 영역(3칸)을 침범하지 않는다.
                float dx = placement.x() - layout.room().spawnPoint().x;
                float dy = placement.y() - layout.room().spawnPoint().y;
                assertTrue("clear of player spawn", dx * dx + dy * dy >= 9f - .001f);
            }
            for (int i = 0; i < containers.size(); i++)
                for (int j = i + 1; j < containers.size(); j++) {
                    LootContainerPlacement a = containers.get(i), b = containers.get(j);
                    float dx = a.x() - b.x(), dy = a.y() - b.y();
                    assertTrue("containers keep spacing", dx * dx + dy * dy >= 2.2f * 2.2f - .001f);
                }
        }
    }

    /** 플레이어(0.6×0.5)가 상호작용 거리(1.45) 안에 설 수 있고, 문 주변 2칸을 막지 않는다. */
    private static void assertAccessibleAndClearOfDoors(LaboratoryRoom room,
            LootContainerPlacement placement) {
        float[][] offsets = {{0f, -1.1f}, {0f, 1.0f}, {-1.1f, 0f}, {1.1f, 0f}};
        boolean standable = false;
        for (float[] offset : offsets) {
            float sx = placement.x() + offset[0], sy = placement.y() + offset[1];
            if (!room.blocksActorAt(sx, sy, .6f, .5f)) { standable = true; break; }
        }
        assertTrue("player can stand next to container at "
            + placement.x() + "," + placement.y(), standable);
        int tx = (int) placement.x(), ty = (int) placement.y();
        for (int dy = -2; dy <= 2; dy++) for (int dx = -2; dx <= 2; dx++) {
            TileType t = room.rawTileAt(tx + dx, ty + dy);
            assertFalse("container keeps door approaches clear",
                t == TileType.DOORWAY_OPEN || t == TileType.DOOR_CLOSED);
        }
        // footprint가 벽 안으로 들어가지 않는다.
        Rectangle footprint = footprint(placement);
        int minX = (int) Math.floor(footprint.x + .0001f), minY = (int) Math.floor(footprint.y + .0001f);
        int maxX = (int) Math.floor(footprint.x + footprint.width - .0001f);
        int maxY = (int) Math.floor(footprint.y + footprint.height - .0001f);
        for (int y = minY; y <= maxY; y++) for (int x = minX; x <= maxX; x++)
            assertEquals("container footprint stays on floor", TileType.FLOOR, room.rawTileAt(x, y));
    }

    /** 캐비넷(정면 스프라이트만 존재)은 벽에 붙는 경우 북쪽 벽 수납만 쓴다(90도 회전 금지). */
    private static void assertCabinetFrontFacing(LaboratoryRoom room, LootContainerPlacement placement) {
        if (!placement.id().endsWith("_cabinet")) return;
        int tx = (int) placement.x(), ty = (int) placement.y();
        boolean south = room.rawTileAt(tx, ty - 1) == TileType.WALL;
        boolean east = room.rawTileAt(tx + 1, ty) == TileType.WALL;
        boolean west = room.rawTileAt(tx - 1, ty) == TileType.WALL;
        boolean north = room.rawTileAt(tx, ty + 1) == TileType.WALL;
        if (south || east || west)
            assertTrue("wall-adjacent cabinet backs onto the north wall only", north);
    }

    private static boolean adjacentToWall(LaboratoryRoom room, LootContainerPlacement placement) {
        int tx = (int) placement.x(), ty = (int) placement.y();
        return room.rawTileAt(tx, ty + 1) == TileType.WALL
            || room.rawTileAt(tx, ty - 1) == TileType.WALL
            || room.rawTileAt(tx + 1, ty) == TileType.WALL
            || room.rawTileAt(tx - 1, ty) == TileType.WALL;
    }

    private static LaboratoryRoomArea owningRoom(LaboratoryLayout layout, LootContainerPlacement p) {
        for (LaboratoryRoomArea area : layout.roomAreas())
            if (p.x() >= area.x() && p.x() < area.x() + area.width()
                && p.y() >= area.y() && p.y() < area.y() + area.height()) return area;
        return null;
    }

    private static Rectangle footprint(LootContainerPlacement p) {
        float scale = p.id().endsWith("_cabinet") ? 1.4f : 1f;
        return new Rectangle(p.x() - .42f * scale, p.y(), .84f * scale, .48f * scale);
    }
}

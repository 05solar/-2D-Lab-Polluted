package io.github.some_example_name.world;

import static org.junit.Assert.*;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import io.github.some_example_name.collision.CollisionSystem;
import io.github.some_example_name.entity.Direction;
import io.github.some_example_name.entity.player.Player;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class HeadquartersLayoutTest {
    private static Player player(float feetX, float feetY) {
        return new Player(feetX, feetY, 0.6f, 0.5f, 100);
    }

    @Test public void tentIsIndependentAndHasOnePassableSouthExit() {
        HeadquartersLayout layout = HeadquartersLayout.create();
        HeadquartersRoom room = layout.room();
        assertEquals(14, room.widthInTiles());
        assertEquals(10, room.heightInTiles());
        assertEquals(6f, room.exitArea().x, 0f);
        assertEquals(0f, room.exitArea().y, 0f);
        assertFalse(room.isSolid(6, 1));
        assertFalse(room.isSolid(6, 0));
        assertEquals("open_tent_flap", layout.visuals().wallAt(6, 1));
        assertEquals("rubber_entrance_mat", layout.visuals().decorAt(6, 1));
        for (int x = 0; x < 14; x++) {
            if (x != 6) {
                assertTrue(room.isSolid(x, 0));
                assertTrue(room.isSolid(x, 1));
            }
            assertTrue(room.isSolid(x, 9));
        }
        for (int y = 1; y < 10; y++) {
            assertTrue(room.isSolid(0, y));
            assertTrue(room.isSolid(13, y));
        }
        for (int y = 1; y < 10; y++)
            for (int x = 0; x < 14; x++)
                assertNotNull("floor under wall at " + x + "," + y,
                    layout.visuals().floorAt(x, y));
    }

    @Test public void spawnAndLaboratoryArrivalAreSafeAndSeparate() {
        HeadquartersRoom hq = HeadquartersLayout.create().room();
        Vector2 spawn = hq.spawnPoint();
        assertTrue(spawn.y - hq.exitArea().y >= 2f);
        Player p = player(spawn.x, spawn.y);
        assertFalse(hq.isSolid((int) spawn.x, (int) spawn.y));
        for (Rectangle footprint : hq.furnitureFootprints())
            assertFalse(p.bounds().overlaps(footprint));
        LaboratoryRoom lab = LaboratoryLayout.testRoom().room();
        Vector2 arrival = lab.spawnPoint();
        assertFalse(lab.isSolid((int) arrival.x, (int) arrival.y));
        assertEquals(Hazard.NONE, lab.hazardAt((int) arrival.x, (int) arrival.y));
        assertNotSame(hq, lab);
    }

    @Test public void wallsBlockAndFurnitureUseOnlyGroundFootprints() {
        HeadquartersRoom room = HeadquartersLayout.create().room();
        CollisionSystem collision = new CollisionSystem();
        Rectangle nearWestWall = new Rectangle(1.05f, 4f, 0.6f, 0.5f);
        assertTrue(collision.resolve(nearWestWall, -0.2f, 0f, room).hitX);
        HeadquartersProp table = null;
        for (HeadquartersProp prop : HeadquartersLayout.create().visuals().props())
            if (prop.id().equals("command_table")) table = prop;
        assertNotNull(table);
        Rectangle foot = table.footprint();
        assertTrue(foot.height < table.drawHeight() / 2f);
        assertTrue(foot.width < table.drawWidth());
        Rectangle approach = new Rectangle(foot.x, foot.y - 0.55f, 0.6f, 0.5f);
        assertTrue(collision.resolve(approach, 0f, 0.1f, room).hitY);
    }

    @Test public void exitNeedsSouthwardFullCrossingAndFiresOnceWhenRunning() {
        HeadquartersRoom room = HeadquartersLayout.create().room();
        Player p = player(6.5f, 3.5f);
        CollisionSystem collision = new CollisionSystem();
        int events = 0;
        for (int i = 0; i < 100; i++) {
            float before = p.feetY();
            p.bounds().setY(collision.resolve(p.bounds(), 0f, -6.5f / 30f, room).y);
            if (room.crossedSouthExit(before, p, -1f)) events++;
        }
        assertEquals(1, events);
        assertFalse(room.crossedSouthExit(p.feetY(), p, -1f));

        HeadquartersRoom another = HeadquartersLayout.create().room();
        Player near = player(6.5f, 0.45f);
        assertFalse("already inside is not a fresh crossing",
            another.crossedSouthExit(0.45f, near, -1f));
        assertFalse("northward movement does not exit",
            another.crossedSouthExit(1f, near, 1f));
    }

    @Test public void vendingRangeIsInFrontAndDistinctFromItsFootprint() {
        HeadquartersRoom room = HeadquartersLayout.create().room();
        assertTrue(room.canInteractWithVending(11.45f, 6.8f));
        assertFalse(room.canInteractWithVending(11.45f, 7.8f));
        assertFalse(room.canInteractWithVending(8f, 6.8f));
        Player approach = player(11.45f, 6.3f);
        approach.setFacing(Direction.UP);
        assertTrue(room.canInteractWithVending(approach));
        approach.setFacing(Direction.DOWN);
        assertTrue("vending prompt is proximity-based", room.isNearVending(approach));
        assertFalse(room.canInteractWithVending(approach));
        assertNotNull(room.interactionPoint("field_medical_cot"));
        assertNotNull(room.interactionPoint("vending_machine"));
        Rectangle range = room.vendingInteractionArea();
        for (Rectangle footprint : room.furnitureFootprints()) {
            if (footprint.contains(11.45f, 7.25f)) {
                assertTrue(range.height > footprint.height);
                return;
            }
        }
        fail("vending ground footprint missing");
    }

    @Test public void waterDispenserCanBeUsedFromNorthAndRejectsDeadOrWrongFacingPlayers() {
        HeadquartersRoom room = HeadquartersLayout.create().room();
        Player approach = player(9.8f, 3.5f);
        approach.setFacing(Direction.DOWN);
        assertTrue(room.canInteractWithWaterDispenser(approach));
        assertNotNull(room.waterDispenserInteractionArea());
        assertNotNull(room.interactionPoint("water_dispenser"));
        approach.setFacing(Direction.UP);
        assertTrue("nearby prompt and interaction do not require exact facing", room.isNearWaterDispenser(approach));
        assertTrue(room.canInteractWithWaterDispenser(approach));
        approach.setFacing(Direction.DOWN);
        approach.bounds().setPosition(4f, 5f);
        assertFalse(room.canInteractWithWaterDispenser(approach));
        Player nearby = player(9.8f, 4.45f);
        assertTrue("prompt appears in the wider 2.5-tile radius", room.isNearWaterDispenser(nearby));
        assertFalse("interaction radius stays bounded", room.canInteractWithWaterDispenser(nearby));
        approach.takeDamage(100);
        approach.bounds().setPosition(9.5f, 3.5f);
        assertFalse(room.isNearWaterDispenser(approach));
        assertFalse(room.canInteractWithWaterDispenser(approach));
    }

    @Test public void compactShopHasCenteredTerminalAndSingleAccessibleVendingMachine() {
        HeadquartersLayout layout = HeadquartersLayout.create();
        int vendingCount = 0;
        int crateCount = 0;
        HeadquartersProp terminal = null;
        for (HeadquartersProp prop : layout.visuals().props()) {
            if (prop.id().equals("vending_machine")) vendingCount++;
            if (prop.id().equals("stacked_supply_crates")) crateCount++;
            if (prop.id().equals("terminal_desk")) terminal = prop;
        }
        assertEquals(1, vendingCount);
        assertTrue("supply crates also fill the center-left tent space", crateCount >= 2);
        assertNotNull(terminal);
        assertEquals(6.55f, terminal.anchorX(), 0.7f);
        assertTrue("monitor lies on north wall side", terminal.anchorY() > 6f);
        assertEquals(14, layout.visuals().width());
        assertEquals(10, layout.visuals().height());
    }

    @Test public void entranceHasClearRoutesToTerminalVendingAndMaintenanceArea() {
        HeadquartersRoom room = HeadquartersLayout.create().room();
        CollisionSystem collision = new CollisionSystem();
        Player p = player(room.spawnPoint().x, room.spawnPoint().y);
        moveTo(p, 6.55f, 6.8f, collision, room); // clear standing space at terminal
        moveTo(p, 8.5f, 6.3f, collision, room);
        moveTo(p, 8.5f, 4.6f, collision, room);
        moveTo(p, 2.5f, 4.6f, collision, room);
        moveTo(p, 1.6f, 5.95f, collision, room); // west cot / maintenance side
        assertEquals(1.6f, p.feetX(), 0.06f);
        assertEquals(5.95f, p.feetY(), 0.06f);
    }

    private static void moveTo(Player p, float targetX, float targetY,
                               CollisionSystem collision, HeadquartersRoom room) {
        int guard = 0;
        while ((Math.abs(p.feetX() - targetX) > 0.02f
            || Math.abs(p.feetY() - targetY) > 0.02f) && guard++ < 10000) {
            float dx = Math.abs(p.feetX() - targetX) <= 0.02f ? 0f
                : Math.signum(targetX - p.feetX()) * 0.04f;
            float dy = Math.abs(p.feetY() - targetY) <= 0.02f ? 0f
                : Math.signum(targetY - p.feetY()) * 0.04f;
            com.badlogic.gdx.math.Rectangle bounds = p.bounds();
            com.badlogic.gdx.math.Rectangle next = new com.badlogic.gdx.math.Rectangle(bounds);
            com.badlogic.gdx.math.Rectangle movedX = new com.badlogic.gdx.math.Rectangle(bounds);
            movedX.setPosition(collision.resolve(bounds, dx, 0f, room).x, bounds.y);
            next.setPosition(movedX.x, collision.resolve(movedX, 0f, dy, room).y);
            p.bounds().setPosition(next.x, next.y);
        }
        assertTrue("route blocked near " + targetX + "," + targetY, guard < 10000);
        assertEquals(targetX, p.feetX(), 0.06f);
        assertEquals(targetY, p.feetY(), 0.06f);
    }

    @Test public void repeatedLayoutsHaveIdenticalVisualsAndProps() {
        HeadquartersLayout a = HeadquartersLayout.create(), b = HeadquartersLayout.create();
        for (int y = 0; y < 10; y++)
            for (int x = 0; x < 14; x++) {
                assertEquals(a.room().isSolid(x, y), b.room().isSolid(x, y));
                assertEquals(a.visuals().floorAt(x, y), b.visuals().floorAt(x, y));
                assertEquals(a.visuals().decorAt(x, y), b.visuals().decorAt(x, y));
                assertEquals(a.visuals().wallAt(x, y), b.visuals().wallAt(x, y));
            }
        List<String> first = new ArrayList<>(), second = new ArrayList<>();
        for (HeadquartersProp p : a.visuals().props())
            first.add(p.id() + "@" + p.anchorX() + "," + p.anchorY());
        for (HeadquartersProp p : b.visuals().props())
            second.add(p.id() + "@" + p.anchorX() + "," + p.anchorY());
        assertEquals(first, second);
    }
}

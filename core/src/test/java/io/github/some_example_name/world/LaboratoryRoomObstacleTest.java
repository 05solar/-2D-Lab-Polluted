package io.github.some_example_name.world;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import io.github.some_example_name.collision.CollisionResult;
import io.github.some_example_name.collision.CollisionSystem;
import io.github.some_example_name.collision.ObstacleGrid;
import io.github.some_example_name.combat.CombatCollisionGrid;
import io.github.some_example_name.combat.MonsterType;
import io.github.some_example_name.entity.monster.Monster;
import org.junit.Test;
import static org.junit.Assert.*;

public class LaboratoryRoomObstacleTest {
    @Test public void furnitureAabbBlocksOnlyItsActualFootprint() {
        TileType[][] tiles = openRoom();
        LaboratoryRoom room = new LaboratoryRoom(tiles, new Vector2(1.5f,1.5f));
        room.addWorldObstacle(new Rectangle(2.80f,2f,.20f,.35f));
        assertFalse("a partial furniture footprint does not mark its entire tile as wall", room.isSolid(2,2));
        assertTrue("a body at the actual footprint is blocked", room.blocksActorAt(2.9f,2f,.6f,.45f));
        assertFalse("empty floor in the same tile stays traversable", room.blocksActorAt(2.4f,2f,.6f,.45f));

        CollisionResult clear = new CollisionSystem().resolve(
            new Rectangle(2.05f,2f,.5f,.45f), .4f, 0f, room);
        assertTrue("exact AABB stops at the furniture edge", clear.hitX);
        assertEquals(2.30f, clear.x, .001f);
    }

    @Test public void fRouteUsesTheSameFurnitureObstaclesAsMovement() {
        LaboratoryRoom room = new LaboratoryRoom(openRoom(), new Vector2(1.5f,2.5f));
        // This narrow fixture blocks the direct middle lane, but allows a route around it.
        room.addWorldObstacle(new Rectangle(2.8f,2f,.2f,1f));
        RoomPathfinder pathfinder = new RoomPathfinder();
        java.util.List<RoomPathfinder.Cell> path = pathfinder.shortestPath(room,1,2,4,2,.6f,.45f);
        assertFalse(path.isEmpty());
        assertTrue("route avoids the occupied edge of the central cells",path.size()>4);
        for(int i=1;i<path.size();i++) {
            RoomPathfinder.Cell a=path.get(i-1),b=path.get(i);
            assertFalse(room.blocksActorAlongPath(a.x+.5f,a.y+.5f,b.x+.5f,b.y+.5f,.6f,.45f));
        }
    }

    @Test public void obstacleGridExposesTheSameStaticFootprints() {
        LaboratoryRoom room = new LaboratoryRoom(openRoom(), new Vector2(1.5f,1.5f));
        Rectangle fixture=new Rectangle(2f,2f,.4f,.25f); room.addWorldObstacle(fixture);
        assertTrue(room instanceof ObstacleGrid);
        Rectangle exposed=((ObstacleGrid)room).obstacles().iterator().next();
        assertEquals(fixture,exposed);
        exposed.set(0,0,1,1);
        assertEquals(fixture,((ObstacleGrid)room).obstacles().iterator().next());
    }

    @Test public void returnPathUsesTheMovementGridIncludingLivingActors() {
        LaboratoryRoom room = new LaboratoryRoom(openRoom(), new Vector2(1.5f,2.5f));
        CombatCollisionGrid movementGrid = new CombatCollisionGrid(room);
        java.util.List<Monster> actors = new java.util.ArrayList<>();
        actors.add(new Monster(1, MonsterType.SLIME, 2.5f, 2.3f, .6f, .45f, 20));
        movementGrid.forPlayer(actors);
        java.util.List<RoomPathfinder.Cell> path = new RoomPathfinder()
            .shortestPath(movementGrid,1,2,4,2,.6f,.45f);
        assertFalse(path.isEmpty());
        assertTrue("F route detours around the same dynamic body used by player movement",path.size()>4);
    }

    private static TileType[][] openRoom() {
        TileType[][] map=new TileType[6][6];
        for(int y=0;y<6;y++)for(int x=0;x<6;x++)
            map[y][x]=x==0||y==0||x==5||y==5?TileType.WALL:TileType.FLOOR;
        return map;
    }
}

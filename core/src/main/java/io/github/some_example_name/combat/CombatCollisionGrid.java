package io.github.some_example_name.combat;

import com.badlogic.gdx.math.Rectangle;
import io.github.some_example_name.collision.ObstacleGrid;
import io.github.some_example_name.entity.monster.Monster;
import io.github.some_example_name.entity.player.Player;
import io.github.some_example_name.world.LaboratoryRoom;
import java.util.ArrayList;
import java.util.List;

/** Snapshot of living actor bodies used with the existing axis collision resolver. */
public final class CombatCollisionGrid implements ObstacleGrid {
    private final LaboratoryRoom room;
    private final List<Rectangle> bodies = new ArrayList<>();
    public CombatCollisionGrid(LaboratoryRoom room) { this.room = room; }
    public void forPlayer(List<Monster> monsters) {
        bodies.clear();
        for (Monster monster : monsters) if (monster.isAlive()) bodies.add(monster.bounds());
    }
    public void forMonster(Player player, List<Monster> monsters, Monster moving) {
        bodies.clear();
        if (player.hp() > 0) bodies.add(player.bounds());
        for (Monster monster : monsters)
            if (monster != moving && monster.isAlive()) bodies.add(monster.bounds());
    }
    @Override public boolean isSolid(int x, int y) { return room.isSolid(x, y); }
    @Override public int widthInTiles() { return room.widthInTiles(); }
    @Override public int heightInTiles() { return room.heightInTiles(); }
    @Override public Iterable<Rectangle> obstacles() { return bodies; }
}

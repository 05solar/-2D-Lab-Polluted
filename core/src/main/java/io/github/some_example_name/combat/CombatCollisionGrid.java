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
    // 스냅샷 목록(방 정적 장애물 복사본 + 살아 있는 몸체)을 for* 호출에서 한 번만 구성하고,
    // obstacles()는 그대로 돌려준다. 정적 장애물 깊은 복사는 수가 바뀔 때만 갱신해,
    // 충돌 해결이 프레임당 몬스터 수만큼 반복돼도(EASY 8배) 복사 비용이 쌓이지 않는다.
    private final List<Rectangle> staticObstacles = new ArrayList<>();
    private int staticSeen = -1;
    private final List<Rectangle> all = new ArrayList<>();
    public CombatCollisionGrid(LaboratoryRoom room) { this.room = room; }
    private void refreshStatic() {
        if (staticSeen == room.obstacleCount()) return;
        staticObstacles.clear();
        for (Rectangle obstacle : room.obstacles()) staticObstacles.add(obstacle);
        staticSeen = room.obstacleCount();
    }
    public void forPlayer(List<Monster> monsters) {
        refreshStatic();
        all.clear();
        all.addAll(staticObstacles);
        for (Monster monster : monsters) if (monster.isAlive()) all.add(monster.bounds());
    }
    public void forMonster(Player player, List<Monster> monsters, Monster moving) {
        refreshStatic();
        all.clear();
        all.addAll(staticObstacles);
        if (player.hp() > 0) all.add(player.bounds());
        for (Monster monster : monsters)
            if (monster != moving && monster.isAlive()) all.add(monster.bounds());
    }
    @Override public boolean isSolid(int x, int y) { return room.isSolid(x, y); }
    @Override public int widthInTiles() { return room.widthInTiles(); }
    @Override public int heightInTiles() { return room.heightInTiles(); }
    @Override public Iterable<Rectangle> obstacles() { return all; }
}

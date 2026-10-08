package io.github.some_example_name.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import io.github.some_example_name.entity.player.Player;
import io.github.some_example_name.world.HeadquartersRoom;

/** F1: tile walls, exact prop feet, player, vending range, exit and spawn. */
public final class HeadquartersDebugRenderer implements Disposable {
    private final ShapeRenderer shapes = new ShapeRenderer();

    public void render(OrthographicCamera camera, HeadquartersRoom room, Player player) {
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(Color.WHITE);
        shapes.rect(0f, 0f, room.widthInTiles(), room.heightInTiles());
        shapes.setColor(Color.RED);
        for (int y = 0; y < room.heightInTiles(); y++)
            for (int x = 0; x < room.widthInTiles(); x++)
                if (room.isSolid(x, y)) shapes.rect(x, y, 1f, 1f);
        shapes.setColor(Color.ORANGE);
        for (Rectangle r : room.furnitureFootprints()) shapes.rect(r.x, r.y, r.width, r.height);
        Rectangle b = player.bounds();
        shapes.setColor(Color.GREEN);
        shapes.rect(b.x, b.y, b.width, b.height);
        Rectangle interact = room.vendingInteractionArea();
        if (interact != null) {
            shapes.setColor(Color.CYAN);
            shapes.rect(interact.x, interact.y, interact.width, interact.height);
            Vector2 point = room.interactionPoint("vending_machine");
            if (point != null) shapes.circle(point.x, point.y, 0.1f, 12);
        }
        Rectangle waterInteract = room.waterDispenserInteractionArea();
        if (waterInteract != null) {
            shapes.setColor(Color.YELLOW);
            shapes.rect(waterInteract.x, waterInteract.y, waterInteract.width, waterInteract.height);
            Vector2 point = room.interactionPoint("water_dispenser");
            if (point != null) shapes.circle(point.x, point.y, 0.1f, 12);
        }
        Rectangle exit = room.exitArea();
        shapes.setColor(Color.PURPLE);
        shapes.rect(exit.x, exit.y, exit.width, exit.height);
        Vector2 spawn = room.spawnPoint();
        shapes.setColor(Color.BLUE);
        shapes.circle(spawn.x, spawn.y, 0.14f, 12);
        shapes.end();
    }

    @Override public void dispose() { shapes.dispose(); }
}

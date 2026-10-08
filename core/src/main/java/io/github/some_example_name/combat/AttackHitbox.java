package io.github.some_example_name.combat;

import com.badlogic.gdx.math.Rectangle;
import io.github.some_example_name.entity.Direction;

public final class AttackHitbox {
    private AttackHitbox() { }
    public static Rectangle set(Rectangle body, Direction direction, float range, float width,
                                Rectangle out) {
        float cx = body.x + body.width / 2f, cy = body.y + body.height / 2f;
        switch (direction) {
            case UP: out.set(cx - width / 2f, body.y + body.height, width, range); break;
            case DOWN: out.set(cx - width / 2f, body.y - range, width, range); break;
            case LEFT: out.set(body.x - range, cy - width / 2f, range, width); break;
            default: out.set(body.x + body.width, cy - width / 2f, range, width); break;
        }
        return out;
    }
}

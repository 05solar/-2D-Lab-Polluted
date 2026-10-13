package io.github.some_example_name.asset;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Pure geometry for the top-left-authored 960x640 modal asset. */
public final class DifficultyModalLayout {
    public static final float SOURCE_WIDTH = 960f;
    public static final float SOURCE_HEIGHT = 640f;

    public static final class Rect {
        public final float x, y, width, height;
        Rect(float x, float y, float width, float height) {
            this.x = x; this.y = y; this.width = width; this.height = height;
        }
        public boolean contains(float px, float py) {
            return px >= x && px <= x + width && py >= y && py <= y + height;
        }
    }

    private final List<String> ids = new ArrayList<>();
    private final List<float[]> sourceRects = new ArrayList<>();
    private final float[] title, footer;

    private DifficultyModalLayout(JsonValue root) {
        title = rect(root.get("titleRect"));
        footer = rect(root.get("footerRect"));
        JsonValue options = root.get("options");
        for (JsonValue option = options.child; option != null; option = option.next) {
            ids.add(option.getString("id"));
            sourceRects.add(rect(option.get("rect")));
        }
        if (ids.size() != 3) throw new IllegalArgumentException("expected three difficulty rows");
    }

    public static DifficultyModalLayout fromJson(String json) {
        return new DifficultyModalLayout(new JsonReader().parse(json));
    }

    private static float[] rect(JsonValue value) {
        float[] r = value.asFloatArray();
        if (r.length != 4) throw new IllegalArgumentException("rectangle requires x,y,w,h");
        return r;
    }

    public static float scale(float viewportWidth, float viewportHeight) {
        if (viewportWidth <= 0f || viewportHeight <= 0f)
            throw new IllegalArgumentException("viewport dimensions must be positive");
        return Math.min(0.78f * viewportWidth / SOURCE_WIDTH,
            0.82f * viewportHeight / SOURCE_HEIGHT);
    }

    public static Rect panel(float viewportWidth, float viewportHeight) {
        float s = scale(viewportWidth, viewportHeight);
        return new Rect((viewportWidth - SOURCE_WIDTH * s) / 2f,
            (viewportHeight - SOURCE_HEIGHT * s) / 2f,
            SOURCE_WIDTH * s, SOURCE_HEIGHT * s);
    }

    public int optionCount() { return ids.size(); }
    public String optionId(int index) { return ids.get(index); }
    public List<String> optionIds() { return Collections.unmodifiableList(ids); }

    public Rect optionRect(int index, float viewportWidth, float viewportHeight) {
        return map(sourceRects.get(index), panel(viewportWidth, viewportHeight),
            scale(viewportWidth, viewportHeight));
    }

    public Rect titleRect(float viewportWidth, float viewportHeight) {
        return map(title, panel(viewportWidth, viewportHeight), scale(viewportWidth, viewportHeight));
    }

    public Rect footerRect(float viewportWidth, float viewportHeight) {
        return map(footer, panel(viewportWidth, viewportHeight), scale(viewportWidth, viewportHeight));
    }

    private static Rect map(float[] r, Rect panel, float scale) {
        return new Rect(panel.x + r[0] * scale,
            panel.y + (SOURCE_HEIGHT - r[1] - r[3]) * scale,
            r[2] * scale, r[3] * scale);
    }
}

package io.github.some_example_name.render;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import java.util.HashMap;
import java.util.Map;

/** Parsed monster animation clips and hit-frame metadata. */
public final class MonsterAnimationSet {
    private final Map<String, Animation<TextureRegion>> animations = new HashMap<>();
    private final Map<String, int[]> hitFrames = new HashMap<>();

    public void put(String name, Animation<TextureRegion> animation, int[] frames) {
        animations.put(name, animation);
        if (frames != null) hitFrames.put(name, frames.clone());
    }
    public Animation<TextureRegion> animation(String name) { return animations.get(name); }
    public TextureRegion frame(String name, float time) {
        Animation<TextureRegion> animation = animations.get(name);
        if (animation == null) return null;
        return animation.getKeyFrame(time, animation.getPlayMode() == Animation.PlayMode.LOOP);
    }
    public int[] hitFrames(String name) {
        int[] frames = hitFrames.get(name);
        return frames == null ? new int[0] : frames.clone();
    }
    public int frameCount(String name) {
        Animation<TextureRegion> animation = animations.get(name);
        return animation == null ? 0 : animation.getKeyFrames().length;
    }
    public float frameDuration(String name) {
        Animation<TextureRegion> animation = animations.get(name);
        return animation == null ? 0f : animation.getFrameDuration();
    }
    public boolean has(String name) { return animations.containsKey(name); }
}

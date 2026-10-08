package io.github.some_example_name.render;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.github.some_example_name.entity.Direction;

import java.util.HashMap;
import java.util.Map;

/**
 * 한 캐릭터의 애니메이션 모음(걷기/공격)과 방향별 idle 프레임을 담는다.
 * 데이터(JSON) 파싱과 분할은 GameAssets가 담당하고, 여기서는 완성된 결과만 보관한다.
 */
public class AnimationSet {

    private final Map<String, Animation<TextureRegion>> animations = new HashMap<>();
    private final Map<Direction, TextureRegion> idleFrames = new HashMap<>();
    private final Map<String, int[]> hitFrames = new HashMap<>();

    public void putAnimation(String name, Animation<TextureRegion> animation) {
        animations.put(name, animation);
    }

    public void putIdle(Direction direction, TextureRegion region) {
        idleFrames.put(direction, region);
    }

    public void putHitFrames(String name, int[] frames) {
        hitFrames.put(name, frames);
    }

    public Animation<TextureRegion> walk(Direction direction) {
        return animations.get("walk_" + key(direction));
    }

    public Animation<TextureRegion> attack(Direction direction) {
        return animations.get("attack_" + key(direction));
    }

    public Animation<TextureRegion> animation(String id) { return animations.get(id); }

    public TextureRegion frame(String id, float elapsed) {
        Animation<TextureRegion> animation = animations.get(id);
        return animation == null ? null : animation.getKeyFrame(elapsed, false);
    }

    public int attackFrameCount(Direction direction) {
        Animation<TextureRegion> animation = attack(direction);
        return animation == null ? 0 : animation.getKeyFrames().length;
    }

    public TextureRegion idle(Direction direction) {
        return idleFrames.get(direction);
    }

    /** 공격 명중 판정 프레임(2단계에서 사용). 없으면 빈 배열. */
    public int[] hitFrames(Direction direction) {
        int[] f = hitFrames.get("attack_" + key(direction));
        return f != null ? f : new int[0];
    }

    private static String key(Direction direction) {
        return direction.name().toLowerCase();
    }
}

package io.github.some_example_name.render;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.github.some_example_name.entity.Direction;
import io.github.some_example_name.entity.player.Player;
import io.github.some_example_name.movement.MovementMode;

/**
 * 플레이어 상태(방향/이동 여부/모드)를 읽어 그릴 프레임을 고른다.
 * 애니메이션 상태(statetime)만 보유하고 게임 규칙을 바꾸지 않는다.
 * 이동 로직과 직접 의존하지 않도록 Player의 읽기 전용 상태만 참조한다.
 */
public class AnimationController {

    private static final float RUN_ANIM_SPEEDUP = 1.6f;

    private final AnimationSet set;
    private float stateTime = 0f;

    public AnimationController(AnimationSet set) {
        this.set = set;
    }

    public TextureRegion currentFrame(Player player, float delta) {
        Direction facing = player.facing();
        if (!player.isMoving()) {
            // 정지 시 다음 걷기가 첫 프레임부터 시작하도록 초기화.
            stateTime = 0f;
            return set.idle(facing);
        }
        float speedup = player.mode() == MovementMode.RUNNING ? RUN_ANIM_SPEEDUP : 1f;
        stateTime += delta * speedup;
        return set.walk(facing).getKeyFrame(stateTime, true);
    }
}

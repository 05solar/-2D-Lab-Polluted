package io.github.some_example_name.render;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.github.some_example_name.entity.player.Player;
import io.github.some_example_name.combat.PlayerAttackState;

/**
 * 플레이어(이후 몬스터)를 그린다. 스프라이트는 발밑(하단 중앙) 앵커로 1x1 월드 단위 크기로 그린다.
 * 게임 상태를 읽기만 한다.
 */
public class EntityRenderer {

    private static final float SPRITE_SIZE = 1f; // recommendedWorldSize

    private final AnimationController playerAnimation;

    public EntityRenderer(AnimationController playerAnimation) {
        this.playerAnimation = playerAnimation;
    }

    public void render(SpriteBatch batch, Player player, float delta) {
        TextureRegion frame = playerAnimation.currentFrame(player, delta);
        float x = player.feetX() - SPRITE_SIZE / 2f;
        float y = player.feetY();
        batch.draw(frame, x, y, SPRITE_SIZE, SPRITE_SIZE);
    }

    public void render(SpriteBatch batch, Player player, float delta,
                       PlayerAttackState attack, float attackDurationSeconds) {
        TextureRegion frame = playerAnimation.currentFrame(player, delta, attack, attackDurationSeconds);
        float x = player.feetX() - SPRITE_SIZE / 2f;
        float y = player.feetY();
        batch.draw(frame, x, y, SPRITE_SIZE, SPRITE_SIZE);
    }
}

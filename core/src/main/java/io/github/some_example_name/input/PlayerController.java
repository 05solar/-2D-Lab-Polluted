package io.github.some_example_name.input;

import io.github.some_example_name.entity.player.Player;

/**
 * InputState를 읽어 플레이어 의도(PlayerIntent)로 변환한다. 순수 매핑이라 테스트 가능.
 * 실제 이동/공격 적용은 각 시스템이 담당한다(여기서는 규칙을 바꾸지 않는다).
 */
public class PlayerController {

    public PlayerIntent intentFrom(Player player, InputState input) {
        if (player.isDead()) return new PlayerIntent(0f, 0f, false, false, false);
        return intentFrom(input);
    }

    public PlayerIntent intentFrom(InputState input) {
        float moveX = 0f;
        float moveY = 0f;
        if (input.isHeld(GameAction.MOVE_LEFT)) moveX -= 1f;
        if (input.isHeld(GameAction.MOVE_RIGHT)) moveX += 1f;
        if (input.isHeld(GameAction.MOVE_DOWN)) moveY -= 1f;
        if (input.isHeld(GameAction.MOVE_UP)) moveY += 1f;

        boolean run = input.isHeld(GameAction.RUN);
        boolean attack = input.isPressed(GameAction.ATTACK);
        boolean interact = input.isPressed(GameAction.INTERACT);
        return new PlayerIntent(moveX, moveY, run, attack, interact);
    }
}

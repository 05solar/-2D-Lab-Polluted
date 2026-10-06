package io.github.some_example_name.movement;

import com.badlogic.gdx.math.Vector2;
import io.github.some_example_name.collision.CollisionResult;
import io.github.some_example_name.collision.CollisionSystem;
import io.github.some_example_name.collision.SolidGrid;
import io.github.some_example_name.entity.Direction;
import io.github.some_example_name.entity.player.Player;
import io.github.some_example_name.input.PlayerIntent;

/**
 * 플레이어 이동을 계산한다: 입력 방향 → 속도(걷기/뛰기) → delta 반영 → 충돌 해결.
 * 이동 거리는 speed * delta 이므로 프레임 속도와 무관하게 일정하다.
 *
 * LibGDX 애플리케이션이 없어도 동작하는 순수 계산(수학/충돌)이라 테스트 가능하다.
 * 이동 상태(mode)와 애니메이션은 서로 직접 의존하지 않는다. 여기서는 Player의 상태만 갱신한다.
 */
public class MovementSystem {

    private final CollisionSystem collisionSystem;
    private final RunStartDetector runStartDetector;
    private final float walkSpeed;
    private final float runSpeed;

    private final Vector2 dir = new Vector2();

    public MovementSystem(CollisionSystem collisionSystem, RunStartDetector runStartDetector,
                          float walkSpeed, float runSpeed) {
        this.collisionSystem = collisionSystem;
        this.runStartDetector = runStartDetector;
        this.walkSpeed = walkSpeed;
        this.runSpeed = runSpeed;
    }

    /**
     * @return 이번 프레임에 달리기가 새로 시작됐으면 true. (소음 이벤트 발행 등에 사용)
     */
    public boolean update(Player player, PlayerIntent intent, SolidGrid grid, float delta) {
        boolean movingInput = intent.hasMovement();

        // 달리기는 "달리기 입력 + 실제 이동 중"일 때만 성립한다.
        boolean wantsRun = intent.run && movingInput;
        boolean runStarted = runStartDetector.update(wantsRun);
        MovementMode mode = runStartDetector.mode();

        player.setMode(mode);
        player.setMoving(movingInput);

        if (!movingInput) {
            return runStarted;
        }

        // 방향 선택은 정규화 전 입력으로(대각선도 자연스럽게), 이동량은 정규화 후 등속으로.
        Direction facing = Direction.fromInput(intent.moveX, intent.moveY);
        if (facing != null) {
            player.setFacing(facing);
        }

        dir.set(intent.moveX, intent.moveY).nor();
        float speed = mode == MovementMode.RUNNING ? runSpeed : walkSpeed;
        float dx = dir.x * speed * delta;
        float dy = dir.y * speed * delta;

        CollisionResult result = collisionSystem.resolve(player.bounds(), dx, dy, grid);
        player.bounds().setPosition(result.x, result.y);
        return runStarted;
    }
}

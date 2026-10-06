package io.github.some_example_name.movement;

/**
 * 달리기 "시작" 순간을 판정한다. WALKING -> RUNNING 전환 프레임에만 1회로 센다.
 * 키를 계속 누르고 있는 동안에는 추가로 세지 않는다. (팀장 소음 규칙의 기반)
 *
 * 순수 Java. 입력/시간/렌더에 의존하지 않는다.
 */
public class RunStartDetector {

    private MovementMode mode = MovementMode.WALKING;
    private int runStartCount = 0;

    /**
     * @param wantsRun 이번 프레임에 "달리는 중"인지(달리기 입력 + 이동 중). true면 RUNNING으로 본다.
     * @return 이번 프레임에 달리기가 새로 시작됐으면 true.
     */
    public boolean update(boolean wantsRun) {
        MovementMode next = wantsRun ? MovementMode.RUNNING : MovementMode.WALKING;
        boolean runStarted = mode != MovementMode.RUNNING && next == MovementMode.RUNNING;
        if (runStarted) {
            runStartCount++;
        }
        mode = next;
        return runStarted;
    }

    public MovementMode mode() {
        return mode;
    }

    public int runStartCount() {
        return runStartCount;
    }

    public void reset() {
        mode = MovementMode.WALKING;
        runStartCount = 0;
    }
}

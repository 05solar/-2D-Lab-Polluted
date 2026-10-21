package io.github.some_example_name.combat;

/**
 * 시전 준비 타이머(순수 Java): 누적 시뮬레이션 시간으로 단일 이벤트 시각을 1회만 통과시킨다.
 * 경계 규칙은 "이전 시간 < eventTime ≤ 새 시간"이고 eventTime=0은 첫 update에서 실행된다.
 * 부동소수점 동일 비교나 프레임 번호 비교를 쓰지 않아 프레임 드롭에도 중복/누락이 없다.
 * 재시전은 새 인스턴스(= 새 castId)다. 준비 중 중단은 cancel()로 남은 효과만 취소한다
 * (이미 시작한 쿨다운은 호출 측이 되돌리지 않는다).
 */
public final class SkillCast {

    private final float eventTime;
    private float elapsed;
    private boolean fired, cancelled;

    public SkillCast(float eventTimeSeconds) {
        if (eventTimeSeconds < 0f) throw new IllegalArgumentException("eventTime");
        this.eventTime = eventTimeSeconds;
    }

    /** 시간 전진. 이벤트 경계를 이번에 통과했으면 true(정확히 1회). */
    public boolean update(float delta) {
        if (fired || cancelled) return false;
        float previous = elapsed;
        elapsed += Math.max(0f, delta);
        boolean crossed = eventTime <= 0f ? previous <= 0f
            : previous < eventTime && eventTime <= elapsed;
        if (crossed) fired = true;
        return crossed;
    }

    /** 사망/맵 전환 중단: 아직 발생하지 않은 이벤트를 취소한다. */
    public void cancel() { cancelled = true; }

    public boolean pending() { return !fired && !cancelled; }
    public float elapsed() { return elapsed; }
}

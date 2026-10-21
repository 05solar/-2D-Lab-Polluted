package io.github.some_example_name.combat;

/**
 * 보호막 상태(순수 Java). 시전 → 준비(생성 연출) → activate_shield 이벤트에서 방어 시작 →
 * 흡수량 소진(파괴) 또는 지속 시간 만료(자연 소멸). 플레이어를 따라다니는 시각 효과일 뿐
 * 별도 충돌 장애물이 아니며, 피해 흡수는 기존 공통 피해 파이프라인(PlayerCombatState)
 * 한 곳에서만 일어난다 — 렌더러가 HP를 복원하지 않는다.
 *
 * 규칙: 준비 중에는 아직 피해를 막지 않는다. 활성(준비 포함) 중 재시전은 실패(상태 불변 —
 * 쿨다운도 호출 측에서 시작되지 않는다). 흡수량 0이 되는 순간 방어 종료+파괴 이벤트 1회,
 * 지속 만료는 파괴와 구분된 소멸 이벤트다. 큰 delta가 활성 구간을 통과하면 만료를 먼저
 * 처리하므로(프레임 시작의 update) 만료 이후 피해를 흡수하지 않는다.
 */
public final class PlayerShieldState {

    private enum Phase { INACTIVE, STARTUP, ACTIVE }

    private final float startupDelay;
    private Phase phase = Phase.INACTIVE;
    private float startupElapsed;
    private int pendingAbsorb;
    private float pendingDuration;
    private int absorbRemaining;
    private float activeRemaining;
    private boolean justActivated, justBroke, justExpired;

    public PlayerShieldState(float startupDelaySeconds) {
        if (startupDelaySeconds < 0f) throw new IllegalArgumentException("startup");
        this.startupDelay = startupDelaySeconds;
    }

    /** 시전 시작. 이미 준비/활성 중이면 false(중복 시전 실패 — 흡수량/쿨다운 재설정 없음). */
    public boolean tryCast(int absorbAmount, float durationSeconds) {
        if (phase != Phase.INACTIVE || absorbAmount <= 0 || durationSeconds <= 0f) return false;
        phase = Phase.STARTUP;
        startupElapsed = 0f;
        pendingAbsorb = absorbAmount;
        pendingDuration = durationSeconds;
        return true;
    }

    /** 게임 시간 전진(일시정지 중 호출하지 않는다 — 준비·지속이 함께 멈춘다). */
    public void update(float delta) {
        if (delta <= 0f) return;
        if (phase == Phase.STARTUP) {
            startupElapsed += delta;
            if (startupElapsed >= startupDelay) {
                float leftover = startupElapsed - startupDelay;
                phase = Phase.ACTIVE;
                absorbRemaining = pendingAbsorb;
                activeRemaining = pendingDuration;
                justActivated = true;
                tickActive(leftover); // 같은 delta로 활성·만료까지 시간순 처리(큰 delta)
            }
        } else if (phase == Phase.ACTIVE) {
            tickActive(delta);
        }
    }

    private void tickActive(float delta) {
        activeRemaining -= delta;
        if (activeRemaining <= 0f) {
            phase = Phase.INACTIVE;
            activeRemaining = 0f;
            justExpired = true; // 시간 만료 — 파괴와 구분(깨지는 연출 없음, 알파 페이드)
            absorbRemaining = 0;
        }
    }

    /**
     * 피해 흡수: 남은 피해(HP로 갈 몫)를 돌려준다. 활성 상태가 아니면 전액 통과.
     * 흡수량이 0이 되면 그 사건에서 방어를 끝내고 파괴 이벤트를 1회 올린다 —
     * 잔여 피해는 같은 호출의 반환값으로 처리되고 별도 피해 이벤트로 넣지 않는다.
     */
    public int absorb(int damage) {
        if (phase != Phase.ACTIVE || damage <= 0) return damage;
        int absorbed = Math.min(absorbRemaining, damage);
        absorbRemaining -= absorbed;
        if (absorbRemaining == 0) {
            phase = Phase.INACTIVE;
            activeRemaining = 0f;
            justBroke = true;
        }
        return damage - absorbed;
    }

    /** 사망/맵 이탈 정리: 버블·대기 이벤트 제거(사용된 쿨다운은 호출 측이 유지). */
    public void clear() {
        phase = Phase.INACTIVE;
        absorbRemaining = 0;
        activeRemaining = 0f;
        justActivated = false;
        justBroke = false;
        justExpired = false;
    }

    public boolean casting() { return phase == Phase.STARTUP; }
    public boolean active() { return phase == Phase.ACTIVE; }
    /** 준비 중 포함 — 재시전 거부 판정용. */
    public boolean busy() { return phase != Phase.INACTIVE; }
    public int absorbRemaining() { return absorbRemaining; }
    public float activeRemaining() { return activeRemaining; }
    public float startupElapsed() { return startupElapsed; }

    /** VFX 트리거(1회 소비): 방어 시작 / 파괴 / 자연 만료. */
    public boolean consumeActivated() { boolean v = justActivated; justActivated = false; return v; }
    public boolean consumeBroke() { boolean v = justBroke; justBroke = false; return v; }
    public boolean consumeExpired() { boolean v = justExpired; justExpired = false; return v; }
}

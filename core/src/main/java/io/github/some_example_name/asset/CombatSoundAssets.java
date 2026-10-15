package io.github.some_example_name.asset;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.Disposable;
import io.github.some_example_name.config.AssetPaths;

/**
 * 전투·전리품 효과음 9종(combat_loot_update_v1, 합성 CC0). 생명주기는 GameAssets가 소유·해제한다.
 *
 * 재생 규칙:
 * - 이벤트(실제 피해/개봉/문 닫힘→열림 등)에만 반응한다. 상태가 유지되는 동안 매 프레임 반복하지 않는다.
 * - 음량 = 카탈로그 권장값 × 전체 효과음 음량(masterVolume).
 * - 타격음은 변형 2종을 교대로 쓰고 아주 미세한 피치 변주만 준다(저음 음원의 추가 다운피치 금지).
 * - EASY 몬스터 8배 환경에서 같은 프레임 다수 타격이 과도하게 겹치지 않도록, 짧은 창(80ms) 안
 *   타격음 동시 재생 수를 제한하고 겹칠수록 음량을 낮춘다.
 * - 효과음 재생은 시각·전투 이벤트의 결과일 뿐이며 팀장 소음(달리기 시작) 카운트와 무관하다.
 */
public final class CombatSoundAssets implements Disposable {

    private static final long MIN_INTERVAL_NANOS = 40_000_000L;      // 같은 음원 최소 간격 40ms
    private static final long HIT_WINDOW_NANOS = 80_000_000L;        // 타격음 동시 재생 판정 창
    private static final int HIT_MAX_IN_WINDOW = 3;                  // 창 안 최대 타격음 수

    private final Sound containerPress, containerOpen, monsterHit1, monsterHit2,
        playerHurt, playerDeath, doorOpen, attackSwing, dashWhoosh;
    private float masterVolume = 1f;
    private boolean hitVariantToggle;
    private final long[] hitPlayTimes = new long[HIT_MAX_IN_WINDOW];
    private int hitPlayIndex;
    private long lastPress, lastOpen, lastHurt, lastDeath, lastDoor, lastSwing, lastDash;

    public CombatSoundAssets() {
        containerPress = load(AssetPaths.SOUND_CONTAINER_PRESS);
        containerOpen = load(AssetPaths.SOUND_CONTAINER_OPEN);
        monsterHit1 = load(AssetPaths.SOUND_MONSTER_HIT_1);
        monsterHit2 = load(AssetPaths.SOUND_MONSTER_HIT_2);
        playerHurt = load(AssetPaths.SOUND_PLAYER_HURT);
        playerDeath = load(AssetPaths.SOUND_PLAYER_DEATH);
        doorOpen = load(AssetPaths.SOUND_DOOR_OPEN);
        attackSwing = load(AssetPaths.SOUND_ATTACK_SWING);
        dashWhoosh = load(AssetPaths.SOUND_DASH_WHOOSH);
    }

    private static Sound load(String path) {
        return Gdx.audio.newSound(Gdx.files.internal(path));
    }

    /** 전체 효과음 음량(0..1). BalanceConfig.sfxMasterVolume을 화면에서 주입한다. */
    public void setMasterVolume(float volume) {
        masterVolume = Math.max(0f, Math.min(1f, volume));
    }

    /** 유효한 개봉 연타 입력 1회(엣지)마다 — 낮고 둔탁한 잠금장치 조작음. */
    public void playContainerPress() { lastPress = throttled(containerPress, .45f, lastPress); }

    /** 나무 상자/캐비넷 개봉 완료 순간 1회. */
    public void playContainerOpen() { lastOpen = throttled(containerOpen, .6f, lastOpen); }

    /** 플레이어 HP가 실제로 감소한 프레임 1회(무적시간 거절 시 호출하지 않는다). */
    public void playPlayerHurt() { lastHurt = throttled(playerHurt, .6f, lastHurt); }

    /** 살아 있음→사망 전환 순간 1회(쓰러짐 애니메이션 시작과 함께). */
    public void playPlayerDeath() { lastDeath = throttled(playerDeath, .7f, lastDeath); }

    /** 문/대문 닫힘→열림 전환 1회(같은 상태 갱신마다 반복하지 않는다). */
    public void playDoorOpen() { lastDoor = throttled(doorOpen, .55f, lastDoor); }

    /** 근접 공격 시작(스윙 시작이 실제로 수락된 프레임) 1회. */
    public void playAttackSwing() { lastSwing = throttled(attackSwing, .3f, lastSwing); }

    /** 돌진 발동 순간 1회(쿨다운 중 재발동 없음 — 발동 수락 시에만 호출). */
    public void playDash() { lastDash = throttled(dashWhoosh, .45f, lastDash); }

    /**
     * 몬스터에게 실제 피해가 적용된 순간. 허공 공격에는 호출하지 않는다.
     * 변형 2종 교대 + 미세 피치 변주(±3%). 80ms 창 안 3회를 넘는 타격음은 건너뛴다.
     */
    public void playMonsterHit() {
        long now = System.nanoTime();
        long oldest = hitPlayTimes[hitPlayIndex];
        if (oldest != 0L && now - oldest < HIT_WINDOW_NANOS) return; // 창이 가득 참
        hitPlayTimes[hitPlayIndex] = now;
        hitPlayIndex = (hitPlayIndex + 1) % HIT_MAX_IN_WINDOW;
        hitVariantToggle = !hitVariantToggle;
        Sound variant = hitVariantToggle ? monsterHit1 : monsterHit2;
        float pitch = .97f + (float) (Math.random() * .06);
        variant.play(.48f * masterVolume, pitch, 0f);
    }

    private long throttled(Sound sound, float volume, long lastNanos) {
        long now = System.nanoTime();
        if (now - lastNanos < MIN_INTERVAL_NANOS) return lastNanos;
        sound.play(volume * masterVolume);
        return now;
    }

    @Override public void dispose() {
        containerPress.dispose();
        containerOpen.dispose();
        monsterHit1.dispose();
        monsterHit2.dispose();
        playerHurt.dispose();
        playerDeath.dispose();
        doorOpen.dispose();
        attackSwing.dispose();
        dashWhoosh.dispose();
    }
}

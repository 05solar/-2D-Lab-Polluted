package io.github.some_example_name.asset;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.Disposable;
import io.github.some_example_name.config.AssetPaths;

/**
 * 컨테이너 개봉 효과음(직접 합성 WAV — assets/sounds/README.md에 출처·라이선스 기록).
 *
 * 재생 규칙: 유효한 개봉 입력(엣지)마다 click, 실제 개봉 순간에는 unlock만. 같은 음원은
 * 최소 간격(throttle)을 두고 연타 시 음량을 제한해 과도한 겹침을 막는다. 홀드/거리 밖/
 * 이미 열린 컨테이너에 대한 차단은 호출 측(유효 입력 판정)에서 보장한다.
 */
public final class ContainerSoundAssets implements Disposable {

    private static final long MIN_CLICK_INTERVAL_NANOS = 40_000_000L; // 40ms
    private final Sound click, unlock;
    private long lastClickNanos, lastUnlockNanos;

    public ContainerSoundAssets() {
        click = Gdx.audio.newSound(Gdx.files.internal(AssetPaths.SOUND_CONTAINER_CLICK));
        unlock = Gdx.audio.newSound(Gdx.files.internal(AssetPaths.SOUND_CONTAINER_UNLOCK));
    }

    /** 유효한 개봉 입력 1회의 금속 '찰칵'. 연타 겹침 방지를 위해 40ms 스로틀 + 0.7 음량. */
    public void playClick() {
        long now = System.nanoTime();
        if (now - lastClickNanos < MIN_CLICK_INTERVAL_NANOS) return;
        lastClickNanos = now;
        click.play(0.7f);
    }

    /** 실제 개봉 순간의 잠금 해제음(클릭음과 구분). 마지막 입력의 클릭과 겹치지 않게 단독 재생. */
    public void playUnlock() {
        long now = System.nanoTime();
        if (now - lastUnlockNanos < MIN_CLICK_INTERVAL_NANOS) return;
        lastUnlockNanos = now;
        unlock.play(0.9f);
    }

    @Override public void dispose() {
        click.dispose();
        unlock.dispose();
    }
}

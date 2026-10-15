# -*- coding: utf-8 -*-
"""컨테이너 효과음 V2 — 경쾌한 '상자 까기' 합성 (표준 라이브러리만, 시드 고정 재현 가능).

combat_loot_update_v1의 낮고 둔탁한 press/open을 대체한다:
- container_press_v2.wav : 밝고 짧은 나무 걸쇠 '탁!' (유효 연타 1회당)
- container_open_v2.wav  : 걸쇠 팝 + 나무 삐걱 + 상승 3음 차임 (개봉 완료 1회)

외부 샘플 없음(CC0 상당). 44.1kHz 모노 PCM16.
"""
import math
import random
import struct
import wave

SR = 44100
random.seed(20261015)


def write_wav(path, samples):
    peak = max(1e-9, max(abs(s) for s in samples))
    scale = 0.92 / peak
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(b"".join(
            struct.pack("<h", int(max(-1.0, min(1.0, s * scale)) * 32767))
            for s in samples))
    print(path, len(samples) / SR, "s")


def silence(seconds):
    return [0.0] * int(SR * seconds)


def add(base, extra, offset_seconds=0.0):
    start = int(SR * offset_seconds)
    need = start + len(extra) - len(base)
    if need > 0:
        base.extend([0.0] * need)
    for i, value in enumerate(extra):
        base[start + i] += value
    return base


def decay_sine(freq, seconds, decay, volume, drift=0.0):
    n = int(SR * seconds)
    out = []
    for i in range(n):
        t = i / SR
        f = freq * (1.0 + drift * t)
        out.append(volume * math.exp(-decay * t) * math.sin(2 * math.pi * f * t))
    return out


def noise_burst(seconds, decay, volume, lowpass=0.35):
    n = int(SR * seconds)
    out, prev = [], 0.0
    for i in range(n):
        t = i / SR
        white = random.uniform(-1.0, 1.0)
        prev += lowpass * (white - prev)  # lowpass 작을수록 어둡고, 클수록 밝다
        out.append(volume * math.exp(-decay * t) * prev)
    return out


def wood_knock(volume):
    """밝은 나무 몸통 울림: 비정수 배음 두 개 + 아주 짧은 어택 노이즈."""
    knock = noise_burst(0.012, 300.0, volume * 1.1, lowpass=0.8)
    add(knock, decay_sine(620, 0.09, 55.0, volume))
    add(knock, decay_sine(1040, 0.07, 70.0, volume * 0.6))
    add(knock, decay_sine(205, 0.11, 40.0, volume * 0.5))
    return knock


def chime_note(freq, seconds, volume):
    """반짝이는 차임 한 음(기음 + 옥타브 + 12도 부분음)."""
    note = decay_sine(freq, seconds, 14.0, volume)
    add(note, decay_sine(freq * 2.0, seconds * 0.8, 18.0, volume * 0.45))
    add(note, decay_sine(freq * 3.0, seconds * 0.6, 24.0, volume * 0.2))
    return note


def build_press():
    """경쾌한 걸쇠 '탁!': 밝은 트랜지언트 + 나무 노크 + 금속 틱. 약 0.12s."""
    out = silence(0.12)
    add(out, wood_knock(0.9), 0.0)
    add(out, decay_sine(3150, 0.03, 160.0, 0.35), 0.002)  # 금속 틱
    add(out, noise_burst(0.02, 180.0, 0.4, lowpass=0.9), 0.0)
    return out


def build_open():
    """상자 까기: 걸쇠 팝 → 나무 뚜껑 삐걱(상향 스윕) → 밝은 상승 3음 차임. 약 0.68s."""
    out = silence(0.68)
    # 1) 걸쇠 팝(press보다 묵직하게 한 번)
    add(out, wood_knock(1.0), 0.0)
    add(out, decay_sine(2650, 0.04, 130.0, 0.3), 0.004)
    # 2) 뚜껑 삐걱: 주파수가 올라가는 떨리는 톤 + 숨소리 노이즈
    n = int(SR * 0.2)
    creak = []
    for i in range(n):
        t = i / SR
        f = 280 + 420 * (t / 0.2) + 28 * math.sin(2 * math.pi * 33 * t)
        env = math.exp(-9.0 * t) * (1.0 - math.exp(-250 * t))
        creak.append(0.5 * env * math.sin(2 * math.pi * f * t))
    add(out, creak, 0.05)
    add(out, noise_burst(0.16, 28.0, 0.12, lowpass=0.5), 0.05)
    # 3) 경쾌한 상승 차임: C6 → E6 → G6 (뚜껑이 열리며 보상이 반짝)
    add(out, chime_note(1046.5, 0.3, 0.5), 0.16)
    add(out, chime_note(1318.5, 0.3, 0.5), 0.26)
    add(out, chime_note(1568.0, 0.38, 0.55), 0.36)
    return out


if __name__ == "__main__":
    import os
    here = os.path.dirname(os.path.abspath(__file__))
    write_wav(os.path.join(here, "container_press_v2.wav"), build_press())
    write_wav(os.path.join(here, "container_open_v2.wav"), build_open())

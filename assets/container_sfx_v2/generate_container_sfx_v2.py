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


def write_wav(path, samples, peak_target=0.92):
    peak = max(1e-9, max(abs(s) for s in samples))
    scale = peak_target / peak
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


def wood_knock(volume, bright=1.0):
    """나무 몸통 울림: 비정수 배음 + 짧은 어택 노이즈. bright<1이면 더 둔탁해진다."""
    knock = noise_burst(0.012, 300.0, volume * 1.1, lowpass=0.5 + 0.3 * bright)
    add(knock, decay_sine(620 * bright, 0.09, 55.0, volume))
    add(knock, decay_sine(1040 * bright, 0.07, 70.0, volume * 0.5 * bright))
    add(knock, decay_sine(190, 0.12, 36.0, volume * 0.6))
    return knock


def build_press():
    """연타 조작음(2026-10-16 V4): 낮고 긴 '투욱' + 끝에서 살짝 올라가는 음.
    저역 노크 + 몸통 울림 뒤, 꼬리에 부드러운 상승음(260→약 330Hz)을 더해
    '투우욱↗' 느낌을 준다. 약 0.32s. 연타 겹침 대비 저역 위주 절제."""
    out = silence(0.32)
    add(out, noise_burst(0.015, 200.0, 0.45, lowpass=0.3), 0.0)   # 둔한 어택
    add(out, decay_sine(300, 0.14, 30.0, 0.6), 0.0)               # 낮은 나무 노크
    add(out, decay_sine(150, 0.22, 15.0, 0.8), 0.0)               # 몸통 울림
    add(out, decay_sine(95, 0.28, 11.0, 0.55), 0.004)             # 저역 여운
    # 끝음 살짝 올림: drift로 주파수가 서서히 상승하는 부드러운 꼬리음
    add(out, decay_sine(260, 0.16, 13.0, 0.42, drift=1.8), 0.14)
    return out


def build_open():
    """상자/캐비넷 개봉(V5로 복원 — 2026-10-16): 묵직하지만 과하지 않게.
    둔한 걸쇠 → 깊은 이중 '쿠웅-터엉'(48/62Hz) → 느린 저역 삐걱 → 긴 저역 공명 여운.
    약 1.0s."""
    out = silence(1.0)
    # 1) 걸쇠: 둔한 저역 노크만(금속 틱 없음)
    add(out, wood_knock(0.85, bright=0.45), 0.0)
    # 2) 첫 번째 '쿠웅'
    add(out, decay_sine(48, 0.6, 5.5, 1.0), 0.07)
    add(out, decay_sine(82, 0.5, 8.0, 0.75), 0.07)
    add(out, noise_burst(0.4, 13.0, 0.45, lowpass=0.1), 0.07)
    # 3) 두 번째 '터엉'(뚜껑 반동)
    add(out, decay_sine(62, 0.55, 6.5, 0.85), 0.32)
    add(out, decay_sine(108, 0.42, 10.0, 0.45), 0.32)
    add(out, noise_burst(0.3, 16.0, 0.3, lowpass=0.12), 0.32)
    # 4) 느리고 어두운 저역 삐걱
    n = int(SR * 0.3)
    creak = []
    for i in range(n):
        t = i / SR
        f = 85 + 90 * (t / 0.3) + 10 * math.sin(2 * math.pi * 17 * t)
        env = math.exp(-6.0 * t) * (1.0 - math.exp(-250 * t))
        creak.append(0.2 * env * math.sin(2 * math.pi * f * t))
    add(out, creak, 0.42)
    # 5) 저역 공명 여운
    add(out, decay_sine(76, 0.5, 4.5, 0.38), 0.58)
    return out


def chime_note(freq, seconds, volume):
    """반짝이는 차임 한 음(기음 + 옥타브 + 12도 부분음)."""
    note = decay_sine(freq, seconds, 14.0, volume)
    add(note, decay_sine(freq * 2.0, seconds * 0.8, 18.0, volume * 0.45))
    add(note, decay_sine(freq * 3.0, seconds * 0.6, 24.0, volume * 0.2))
    return note


def build_pickup():
    """아이템 획득음(2026-10-16): 경쾌한 상승 2음 차임 E6→A6 + 짧은 반짝임. 약 0.4s."""
    out = silence(0.4)
    add(out, noise_burst(0.015, 220.0, 0.2, lowpass=0.95), 0.0)   # 밝은 클릭 어택
    add(out, chime_note(1318.5, 0.22, 0.5), 0.0)                   # E6
    add(out, chime_note(1760.0, 0.3, 0.55), 0.09)                  # A6
    return out


if __name__ == "__main__":
    import os
    here = os.path.dirname(os.path.abspath(__file__))
    # 연타음은 더 크게(요청) — 최대 음량 근처로 노멀라이즈.
    write_wav(os.path.join(here, "container_press_v2.wav"), build_press(), peak_target=0.98)
    write_wav(os.path.join(here, "container_open_v2.wav"), build_open())
    write_wav(os.path.join(here, "item_pickup_v1.wav"), build_pickup())

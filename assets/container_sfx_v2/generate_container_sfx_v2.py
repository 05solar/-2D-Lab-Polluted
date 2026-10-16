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
    """경쾌한 걸쇠 '탁!': 트랜지언트 + 나무 노크 + 금속 틱. 약 0.12s."""
    out = silence(0.12)
    add(out, wood_knock(0.9, bright=0.9), 0.0)
    add(out, decay_sine(2800, 0.03, 160.0, 0.26), 0.002)  # 금속 틱(살짝 절제)
    add(out, noise_burst(0.02, 180.0, 0.35, lowpass=0.8), 0.0)
    return out


def saturate(samples, drive=2.4):
    """소프트 클리핑으로 평균 음량(RMS)을 크게 끌어올린다(피크는 노멀라이즈가 관리)."""
    return [math.tanh(s * drive) for s in samples]


def build_open():
    """상자/캐비넷 개봉(2026-10-16 V7): 훨씬 더 크고 길게.
    둔한 걸쇠 → 아주 깊은 이중 '쿠우웅-터어엉'(40/52Hz) → 느린 저역 삐걱 →
    아주 긴 저역 공명 꼬리. 약 2.4s. tanh 새추레이션으로 체감 음량을 크게 올리고,
    저역이 작은 스피커에서도 들리도록 150Hz대 몸통을 함께 울린다."""
    out = silence(2.4)
    # 1) 걸쇠: 둔한 저역 노크만(금속 틱 없음)
    add(out, wood_knock(0.8, bright=0.4), 0.0)
    # 2) 첫 번째 '쿠우웅': 아주 느린 감쇠로 길게 울린다
    add(out, decay_sine(40, 1.5, 2.2, 1.0), 0.07)
    add(out, decay_sine(70, 1.2, 3.0, 0.8), 0.07)
    add(out, decay_sine(148, 0.8, 5.0, 0.5), 0.07)       # 작은 스피커용 몸통
    add(out, noise_burst(0.8, 6.0, 0.5, lowpass=0.09), 0.07)
    # 3) 두 번째 '터어엉'(뚜껑 반동) — 더 길게
    add(out, decay_sine(52, 1.4, 2.6, 0.95), 0.55)
    add(out, decay_sine(92, 1.0, 4.0, 0.55), 0.55)
    add(out, decay_sine(165, 0.7, 6.0, 0.35), 0.55)
    add(out, noise_burst(0.6, 7.5, 0.35, lowpass=0.1), 0.55)
    # 4) 느리고 어두운 저역 삐걱
    n = int(SR * 0.6)
    creak = []
    for i in range(n):
        t = i / SR
        f = 60 + 70 * (t / 0.6) + 7 * math.sin(2 * math.pi * 11 * t)
        env = math.exp(-3.0 * t) * (1.0 - math.exp(-250 * t))
        creak.append(0.17 * env * math.sin(2 * math.pi * f * t))
    add(out, creak, 0.8)
    # 5) 아주 긴 저역 공명 꼬리(이중)
    add(out, decay_sine(60, 1.3, 1.9, 0.45), 1.05)
    add(out, decay_sine(45, 1.3, 1.7, 0.35), 1.1)
    return saturate(out, drive=2.4)


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
    write_wav(os.path.join(here, "container_press_v2.wav"), build_press())
    # 개봉음은 최대 음량에 가깝게 노멀라이즈(요청: 더 크게).
    write_wav(os.path.join(here, "container_open_v2.wav"), build_open(), peak_target=0.99)
    write_wav(os.path.join(here, "item_pickup_v1.wav"), build_pickup())

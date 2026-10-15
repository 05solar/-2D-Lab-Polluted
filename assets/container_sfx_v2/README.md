# container_sfx_v2 — 경쾌한 상자 개봉 효과음

2026-10-15. `combat_loot_update_v1`의 낮고 둔탁한 `container_press_low/open_heavy`를 대체한다
(요청: "좀 더 경쾌하고 좀 더 상자를 까는 듯한" 소리).

| 파일 | 용도 | 길이 |
|---|---|---|
| `container_press_v2.wav` | 유효 SPACE 연타 1회당 — 밝은 나무 걸쇠 '탁!'(노크 울림+금속 틱) | 0.12s |
| `container_open_v2.wav` | 개봉 완료 1회 — 걸쇠 팝 → 나무 뚜껑 삐걱(상향 스윕) → 상승 3음 차임(C6·E6·G6) | 0.74s |

## 출처·라이선스
`generate_container_sfx_v2.py`(Python 표준 라이브러리만, 시드 고정)로 **직접 합성**.
외부 샘플 없음, 저작권 제약 없음(CC0 상당). 44.1kHz 모노 PCM16.

재생 규칙은 기존과 동일: 엣지 입력 1회당 press, 개봉 순간 open 1회, 40ms 스로틀,
카탈로그 음량 × `BalanceConfig.sfxMasterVolume`.

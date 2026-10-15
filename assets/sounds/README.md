# sounds — (구) 컨테이너 상호작용 효과음 [대체됨]

2026-10-15 `combat_loot_update_v1` 적용으로 이 폴더의 음원은 더 이상 게임에서 사용하지 않는다.

- `container_click.wav` → `combat_loot_update_v1/runtime/audio/container_press_low.wav` (낮고 둔탁한 연타 조작음)
- `container_unlock.wav` → `combat_loot_update_v1/runtime/audio/container_open_heavy.wav` (개봉 완료음)

전투·문·피격 등 전체 효과음 카탈로그와 라이선스(CC0)는
`assets/combat_loot_update_v1/audio_catalog.json`, `AUDIO_LICENSE.txt` 참고.

## (기록) 출처·라이선스

두 음원 모두 이 프로젝트에서 **직접 합성**했다(2026-10-14). 외부 샘플을 사용하지 않았으며,
Python 표준 라이브러리(wave/math/random)만으로 노이즈 트랜지언트 + 금속 공진 모드(사인 합성)를
믹스해 생성했다. 저작권 제약 없음 — CC0 상당.

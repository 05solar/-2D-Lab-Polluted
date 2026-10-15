# 전투·전리품 업데이트 에셋 V1

## 포함 파일

| 파일 | 용도 |
| --- | --- |
| `runtime/ui/key_f_64.png` | F 돌진 키 안내 |
| `runtime/ui/key_blank_64.png` | 기존 공격 키를 코드로 표시할 빈 키캡 |
| `runtime/ui/dash_icon_128.png` | 돌진 스킬·3초 쿨다운 UI |
| `runtime/ui/attack_icon_128.png` | 기존 공격 안내 UI |
| `runtime/audio/container_press_low.wav` | 낮고 둔탁한 캐비넷 연타 조작음 |
| `runtime/audio/container_open_heavy.wav` | 상자·캐비넷 개봉 완료음 |
| `runtime/audio/monster_hit_01.wav`, `monster_hit_02.wav` | 몬스터 타격음 변형 2종 |
| `runtime/audio/player_hurt.wav` | 플레이어 피격음 |
| `runtime/audio/player_death.wav` | 비언어 사망·쓰러짐 효과음 |
| `runtime/audio/door_open_metal.wav` | 문 잠금 해제·슬라이드 효과음 |
| `runtime/audio/attack_swing.wav` | 공격 휘두름 |
| `runtime/audio/dash_whoosh.wav` | 돌진 발동 |

UI PNG는 투명 RGBA이며 게임에서 Nearest 필터를 사용합니다. 기본 상태만 포함되어 있으며 비활성화 색, 남은 쿨다운 가림과 숫자는 실제 게임 상태에 맞춰 코드로 그립니다. 공격 키는 기존 프로젝트 설정을 확인해 빈 키캡 위에 표시합니다. 공격 키를 새로 임의 지정하지 않습니다.

효과음은 제3자 샘플 없이 합성한 44.1kHz·모노·PCM16 WAV입니다. 권장 음량은 `audio_catalog.json`에 있습니다. 상자 조작음은 이미 낮고 둔탁하게 합성했으므로 추가로 피치를 크게 낮출 필요가 없습니다. 사망음은 사람의 비명/음성이 아닌 낮은 충격과 장비 흔들림입니다.

## 미리보기

- `preview/combat_ui_preview.png`: UI 에셋 미리보기. 실제 게임 화면이 아닙니다.
- `preview/sfx_demo.wav`: 연타 조작 → 개봉 → 몬스터 타격 → 플레이어 피격 → 사망 → 문 열림 → 공격 → 돌진 순서의 효과음 모음입니다.

## 적용

`IMPLEMENTATION_PROMPT.md`를 개발 AI에 전달하세요. 이번 변경으로 F는 돌진, 기존 귀환 경로 단축키는 H로 바꾸고 귀환 버튼은 유지합니다. Space 안내는 기존 에셋을 재사용합니다.

`source/generate_sfx.py`는 numpy/scipy로 효과음을 재생성합니다. 랜덤 시드가 고정돼 있으며 추가 외부 음원이 필요하지 않습니다. `source/combat_ui_source.png`는 UI 원화입니다.

게임 코드는 이 팩에 포함되지 않습니다. 실행 프로젝트에서 이동·피해·처치·음량·UI 배치를 검증해야 합니다.

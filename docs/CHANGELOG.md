# CHANGELOG

플레이 가능한 변화 중심으로 기록한다. (변수명 변경·메서드 분리 같은 내부 변경은 제외)

## [Unreleased]
### Added
- **독립된 임시 본부 천막:** 원본 `hq_tent` 타일·가구·자판기 에셋으로 안전한 20×15 본부에서 시작한다. 남쪽 열린 출입구, 가구 지면 충돌, 자판기 접근 강조, F1 범위 디버그를 추가했다. 남쪽 바깥 출구를 넘으면 0.35초 페이드로 연구소에 이동하며 HP·재화·인벤토리·진행 상태를 유지한다. 자판기 구매 UI와 회복 기능은 후속 단계다.
- **Laboratory Tileset V2 적용 (타일 배치 전면 교체):** 전용 64px 타일셋으로 연구소 화면을 다시 그린다.
  - 바닥은 기본 A/B/C + 마모·균열·보수판·배수구·해치가 섞여 반복 격자감이 크게 줄었다.
  - 벽은 직선·끝단·바깥/안쪽 모서리·T자·십자가 이웃에 맞게 연결되고, 외벽이 끊기지 않는다.
  - 문은 가로/세로·열림/닫힘이 벽 방향에 맞게 쓰이고, 열린 문은 검은 구멍 대신 문턱·바닥이 보인다.
  - 오염 구역은 녹색 독성 웅덩이·비말·배수 누수가 불규칙 군집으로, 정비 구역은 연결된 전선과 전기 스파크로 표현된다.
  - 오버레이는 바닥 위 별도 레이어(floor→overlay→wall→structure)로 그려지고 벽 위에는 올리지 않는다.
  - 독성·감전은 시각과 분리된 hazard 데이터로만 표시(피해 로직은 이후 단계).

- 프로젝트 기준 문서 작성: `AGENTS.md`, `docs/`(GAME_DESIGN, ARCHITECTURE, GAME_RULES, TEST_PLAN, ROADMAP, decisions).
- **1단계 기본 플레이 공간:** 연구소 테스트 방을 탐사할 수 있다.
  - 방향키/WASD로 걷고 Shift로 달린다. 이동 속도는 프레임 속도와 무관하게 일정하다.
  - 벽을 통과할 수 없고 카메라가 플레이어를 따라가며 방 밖을 보여주지 않는다.
  - 플레이어가 바라보는 방향으로 4방향 걷기 애니메이션이 재생된다(달릴 때 더 빠르게).
  - Esc로 일시정지(월드/애니메이션 정지), F1로 충돌 영역 디버그 표시 토글.

- **연구소 배경 다양화:** 테스트 방이 입구·중앙 실험·오염·정비 네 구역으로 구분된다.
  - 균열·얼룩·경고·독성·노출 전선 바닥과 방향별 벽/모서리, 닫힌 문·열린 출입구가 보인다.
  - 벽과 모서리가 위치에 맞게 자동으로 연결되고, 오염 구역은 닫힌 문으로 격리돼 있다.
  - 독성·전선 바닥은 현재 표시만 되고 피해는 없다. 닫힌 문은 통과할 수 없다.

### Changed
- 데스크톱 실행 시 libGDX 템플릿 로고 화면 대신 연구소 탐사 화면으로 바로 진입한다.
- 배경이 동일한 바닥·벽 반복에서 구역별로 구분되는 연구소 공간으로 개선됐다.
- 시작 화면(안전 구역)에 균열 군집·경고 바닥·열린 출입구가 보이도록 배치를 다듬었다.
- **연구소 맵 자연스럽게 재구성:** 네 구역(남서 입구·중앙 허브·북동 오염 격리·남동 정비)이
  화면만으로 구분되고 이동 동선이 드러나도록 레이아웃을 다시 설계했다.
  - 중앙을 가짜 벽으로 막던 "실험설비"를 없애 떠 보이던 구조물을 제거했다.
  - 노란 경고선을 둘레 링이 아니라 위험 구역 모서리 표식으로만 제한했다.
  - 독성·전선·얼룩을 큰 사각형 대신 2~5칸 불규칙 군집으로 흩었다.
  - 오염 구역은 닫힌 격리문 하나로만 밀폐되고, 정비 구역은 통로로 중앙과 연결된다.
  - 타일 경계에 생기던 얇은 선(bleeding)을 줄였다(ClampToEdge + 반텍셀 인셋).
  - 참고: 실험대·서버랙 등 가구는 전용 에셋이 아직 없어 배치하지 않았다(docs/process.md 참고).

### Fixed
- 벽·문 셀의 투명 부분에 검은 빈 공간이 보이던 문제 수정. 모든 셀에 바닥을 먼저 깔고 투명 레이어 전에 알파 블렌딩을 복구한다.
- 직선 벽에 기존 파손 변형을 드물게 섞어 동일 벽 문양의 반복을 줄였다.
- 배경이 전부 기본 바닥으로만 보이던 문제 수정. 실제 타일셋이 1254×1254(스펙의 256×256과 다름)
  였는데 64px 고정으로 분할해 인덱스 0~15가 모두 좌상단 첫 타일 조각을 가리키고 있었다.
  타일셋을 실제 크기 기준 4×4 격자로 분할하도록 변경(`GameAssets`).

### Debug
- F1: 충돌 영역 표시 토글. 기본값은 **꺼짐**(실행 시 빨간 벽·초록 플레이어 테두리가 보이지 않음). 필요할 때 F1로 켠다.
- F2: 바닥/오버레이/벽/구조물 타일 ID와 셀 테두리(바닥 회색·벽 빨강·닫힌 문 주황·열린 문 초록·위험 보라) 표시 토글. 기본값은 **꺼짐**.
- 실행 시 각 시각 타일 개수를 `TileVisualCount` 로그로 1회 출력(진단).

### Wall and door connection repair (2026-10-07)

- Derived seamless 64px wall and door atlases from the existing V2 art; preserved the source PNGs.
- Resolved door orientation from surrounding walls, reserved two jamb cells, and tied visual state to collision state.
- Corrected four-way wall classification and reciprocal connector validation. Removed randomly placed metal floor seams that resembled stray wall fragments.
- Added 9x9 visual gallery and assembly rooms (`LAB_WALL_TEST=1/2`) and F2 mask, connector, door state, and collision indicators.
### First combat pass (2026-10-07)

- Added the four supplied monster sprite sheets and animation JSON, player attacks on animation hit frames, bounded seeded lab spawns, basic monster AI and telegraphed attacks, health/death and knockback, HP displays, and team leader run-start hearing.
# 2026-10-08

- Applied the supplied 8-frame directional player death animation. The final frame remains visible, HP stays at zero, and player actions are blocked after death.
- Resized the headquarters to a compact 14x10 shop tent, fixed its camera to show the whole room, and added a contextual SPACE keycap prompt with one-shot vending feedback.
- Reduced the Korean “상점 열기” label to 12px, centered it over the SPACE keycap, and placed it directly above with a small gap.
- Added a contextual SPACE prompt at the water dispenser; using it restores the living player's HP to maximum.
- Added north-side facing/range/obstruction checks and a manual WATER test spawn; `core:test` passes 89 tests and `lwjgl3:build` succeeds (`docs/images/hq_water_prompt.png`).
- Expanded both interaction prompt visibility radii to 2.5 tiles, independent of facing, and centered the water prompt over the dispenser.
# 2026-10-08 플레이어 HP HUD · 8칸 가방 UI
- 화면 좌측 상단에 항상 보이는 HP HUD(프레임+채움+숫자)를 본부·연구소 양쪽에 추가. 채움 폭은 HP/100, 색은 >50 healthy·>25 warning·그 이하 critical. 월드 카메라와 분리된 화면 좌표로 그린다.
- R로 8칸 가방 UI 토글(중앙 패널+딤), Esc는 가방부터 닫음, 가방 열림 시 이동·공격·상호작용 정지. 보유 물품 아이콘과 "현재/8" 표시, 가득 참 알림(가방이 가득 찼습니다).
- 8칸 수집 모델(inventory.Backpack): 최대 8개, 가득 차면 9번째 거부(물품 보존). 본부 인계(handOver)는 연결 지점만 마련(실제 보관 경제 미구현).
- [임시] 월드 아이템 획득 시스템이 아직 없어, 연구소에서 G로 테스트 물품을 회수해 8칸/가득참을 눈으로 확인한다.

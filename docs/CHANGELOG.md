# CHANGELOG

플레이 가능한 변화 중심으로 기록한다. (변수명 변경·메서드 분리 같은 내부 변경은 제외)

## [Unreleased]
### Added
- **쉬움 연구소 방 축소 및 벽면 가구 밀도 개선 (2026-10-13):** 128×96 맵의 16개 작은 방은 유지하면서 현재 3칸 분기 복도를 2칸으로 더 줄였다(주 복도 2칸 유지, 문 지지 구간은 5칸). V7의 실제 투명 픽셀 경계에 맞춰 네 방향 벽면 가구를 연속 배치하고, 중앙 작업 구역은 보존했다. 64개 벽 구간의 피복률은 86.7~90.0%이며 테스트가 방 연결·통행·가구 충돌·피복률을 검사한다.
- **쉬움 연구소 재배치:** 128×96 맵 크기를 유지하면서 9~24타일 폭의 크기가 다른 16개 방, ㄱ자 홈, 교차 복도와 분기 통로를 구성했다. 쉬움 맵의 가구를 `laboratory_orthographic_views_v7` 에셋의 정면/좌측/우측 시점으로 교체하고, 방별 작업 군집과 바닥 충돌 영역을 배치했다. 문·문틀·충돌, 미니맵 점유, 방 접근성은 공통 타일 배치에서 파생된다.
- **연구소 가구 에셋 교체:** 제공된 orthographic view pack의 front/left/right PNG와 roomkits_v3 아틀라스를 연결하고, 픽셀 알파 경계로 발밑 기준을 맞춘다. 소품은 회전 없이 방 방향에 맞는 원본 뷰를 사용한다.
- **실제 loot 흐름:** loot 테이블에서 컨테이너별 보상을 한 번 추첨해 애니메이션 후 월드에 떨어뜨리고, 기존 8칸 가방과 연결했다. D는 최저 슬롯보다 가치가 높은 주변 물품만 교체하며 이전 물품을 바닥에 남긴다.
- **난이도 게이트 및 다중 방 연구소:** 본부에서 쉬움·보통·어려움을 고르고 페이드로 입장/복귀한다. roomkits_v3 방향별 가구, 3단계 맵 크기/방 구조와 몬스터 수, 미니맵·F 경로, 발자국 표시를 추가했다.
- **loot 컨테이너:** 제공된 loot 테이블·상자 애니메이션·희귀도 아이템 아틀라스를 로드한다. E로 상자를 열고 아이템을 주우며, 가방이 꽉 찬 경우 D로 최저 가치 슬롯을 교체한다.
- **독립된 임시 본부 천막:** 현재 본부는 14×10 고정 카메라 안전 구역이다. 남쪽 출입구에서 연구소로 페이드 전환하며 플레이어 세션을 유지한다. 자판기와 물통은 2.5타일 내 SPACE 안내를 표시하고, 물통 SPACE 입력은 생존 중인 플레이어 HP를 최대로 회복한다. 자판기 구매 UI는 미구현이다.
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
# 2026-10-13 — Compact easy laboratory wall furnishing

- Narrowed easy-map 3-tile branches to 2 tiles while retaining the 2-tile main routes and door-support vestibules.
- Replaced sparse fixed wall props with alpha-bound-aware packing across north, south, east, and west walls. Center work islands remain after wall placement; collision footprints remain inside wall cells.
- Added per-room/per-wall visible-coverage reports and a regression test requiring at least 85% of each usable wall to be furnished.
- Corrected furniture anchors against measured alpha bounds and the room-facing wall edge. Added full-art/floor-cell and collision-footprint validation, overlap rejection, and smaller-scale retries for concave corners and short wall segments.
- Wall fixtures keep collision support on the structural wall tile so the tile-based pathfinder does not mark a full interior aisle tile blocked. Generated-room screenshot: `docs/images/laboratory_furniture_containment_chemistry.png`.
- Fixed laboratory phantom collision: partial furniture AABBs no longer convert every overlapping tile into a solid tile. Movement and F-return routing now query the same body-sized furniture obstacles.
- Added visible wall autotile variants for connected ends, corners, T/cross junctions, damaged straight runs, and sparse supports. Added F1 collision visualization with distinct passable, solid, furniture, and player colors.
# 2026-10-13 연구소 벽면 가구 밀착·충돌·뒷모습
- 남쪽 벽 가구가 **뒷모습(_back)** 으로 표시된다(laboratory_orthographic_views_v8 백뷰 팩 연결, 북=정면·남=뒷모습·좌우=측면).
- 벽면/중앙 가구의 **충돌을 실제 불투명 바닥 footprint**에 맞췄다. 정면/뒷모습은 바닥 접촉 띠(높이 0.7)만 충돌로 잡고 높은 입면 상단은 통행 가능, 측면은 불투명 사각형 전체. 고정 0.30·1.24폭과 벽 속 숨은 충돌 박스를 제거해 빈 바닥의 보이지 않는 충돌과 가구 관통을 없앴다.
# 2026-10-13 미니맵 프레임·귀환 발자국 버튼
- 우상단 미니맵에 전용 프레임 PNG(minimap_frame_256) 테두리를 적용하고, 투명 창 안에 방/벽·플레이어(초록)·대문(주황) 마커를 그린다.
- 미니맵 바로 아래에 귀환 버튼(return_route_button_256x64)을 추가. 버튼 클릭(또는 F)으로 대문까지 경로를 토글한다.
- 경로를 발자국 PNG(footprints_pair_64)로 바닥에 진행 방향으로 회전·간격 배치해 "집(대문)으로 가는 발자국"을 보여준다.
# 2026-10-13 연구소 외벽·대문 V3 (easy 맵)
- 연구소 외곽을 exterior_walls_gate_v3 2타일 두께 외벽 링(직선 4종 신축 + 바깥 모서리 4종)으로 교체. 기존 autotile 외곽 스킨은 지우고 새 외벽으로 대체(충돌은 두꺼운 WALL 유지).
- 남쪽에 8×4 대문(닫힘/열림) 배치. 같은 기준점으로 상태 전환, 닫힘=중앙 통로 차단, 열림=중앙 통로 통행 가능(문틀은 계속 충돌). SPACE로 대문을 열고 남쪽으로 나가면 본부로 복귀(기존 전환 유지).
- 대문 개구부를 2행으로 뚫어 2타일 벽을 실제로 통과. 충돌/길찾기/미니맵은 동일 타일·대문 데이터를 사용.
# 2026-10-13 내부 직선 벽도 새 금속 디자인 (easy 맵)
- 연구소 내부 방 경계벽·복도벽의 직선 구간(가로/세로)을 exterior_walls_gate_v3 금속 직선으로 교체(반복 지지대 형태 제거). 방 쪽(바닥) 면에 맞춰 north/south·west/east를 선택.
- 접합부(바깥/안쪽 모서리·T자·십자·끝단)와 세로 방문은 기존 v3 타일 유지(새 팩에 해당 조각이 없음 — 사용자 승인한 하이브리드).
- 가구/플레이어보다 먼저(WALL 레이어 직후) 그려 가구가 벽을 올바르게 덮음. 충돌·길찾기·미니맵은 기존 논리 벽 그대로라 변화 없음(시각만 교체).
# 2026-10-14 V4 벽·문 전면 통일 (전 난이도)
- 벽·문을 laboratory_structure_v4 단일 규격으로 통일: 하나의 논리 점유 그리드에서 8비트 마스크(N=1…NW=128, 대각선은 양옆 직교 이웃 모두 벽일 때만)로 47종 아틀라스 타일을 선택(`WallMaskV4`+`LaboratoryStructureRendererV4`). 구형 V2/V3 접합부 스킨·InteriorWall 덧그림·Exterior 링 드로우 제거(보라 접합부·구형 방문 소멸). 본부 대문만 V3 유지.
- 내부 방문은 V4 가로/세로(전용 탑뷰) 닫힘/열림 스프라이트. `Door` 객체가 시각·충돌·토글을 함께 결정.
- 실제 GL 렌더 검증 경로 추가: `FrameCapture`(F12/`LAB_AUTO_CAPTURE`) 백버퍼→PNG. `LAB_DIFFICULTY`, `LAB_WALL_TEST=3`(47종+문4종 갤러리).

# 2026-10-14 연구소 방 26개·복도 단축·컨테이너 개편
- 방 간격 압축(PLAN_SPACING_SCALE .70→.55) + 보조실 10개 추가(창고·전기실·검역실·세척실·경비초소·서버별관·북창고·표본별관·서쪽 수납실, 일부는 이웃 방과 직접 연결) → 쉬움 16→26방(+62%). 외곽 순환 링 + 1타일 지름길(x42/x45/x82/x91/y16)로 문 사이 평균 보행 거리 66.5→52.0(-21.8%, 맨해튼 하한 41.8). 보통/어려움은 3×3 구획 9방(+50%), 구획 경계마다 V4 문 12개.
- 컨테이너 설정화(`BalanceConfig`): 쉬움 14/7/2=23개(방 대부분 ≥1, 도서관·화학실 복수), 보통 6, 어려움 8(기존 각 3). 캐비넷 1.4배 표시+동일 기준점, 바닥 점유·충돌·접근 검사 동일 배율(`containerFootprint`).
- SPACE 연타 개봉: 나무 1회/일반 5회/고급 8회(설정값), 엣지 입력만 1회, 진행도 컨테이너별 저장, 바라보는 가장 가까운 대상 1개만. 도달 입력에서 1회만 개봉+추첨, 수집/문과 중복 실행 금지, 가방/일시정지/사망/전환 중 차단.
- 연출: 대상 위 SPACE 안내+연타 카운트(열린 컨테이너 제외), 등급별 외곽 발광(일반=보라, 고급=금, 나무=파랑; 진행률 비례, 마지막 펄스→열림 연결, 가산 블렌딩·상태 복원), 직접 합성 효과음(찰칵/해제음, 40ms 스로틀·음량 제한, assets/sounds/README.md에 출처·라이선스).
# 2026-10-15 전투·전리품 업데이트 V1 (combat_loot_update_v1 적용)
- 컨테이너 배치 다양화: 방 중앙 고정점 제거 → 벽면 45/구석 25/가구 옆 20/중앙·보조 10 가중 추첨(빈 분류는 재배분, 시드 고정 결정적). 문 주변 2칸·가구·기존 컨테이너 회피, 모든 컨테이너에 플레이어 접근 지점 보장. 캐비넷(정면 아트만)은 북쪽 벽 수납만 허용(90도 회전 금지). normal/hard 구획 중앙 고정점도 동일 추첨으로 교체.
- SPACE 안내 근접: 로드 시 측정한 닫힘 프레임 불투명 상단 + 화면 6px(카메라 배율 환산) 간격으로 표시. 연타 카운트는 안내 위, 개봉 후엔 숨김.
- 아이템 공개 연출: 등급 테두리 아이콘을 2배(1.12)로 상자 위까지 짧게 상승 후 정지(월드 효과 레이어, HUD 아래). 실제 드롭은 접근 가능한 바닥 지점에 분리 생성(중복 생성 없음, 가방 아이콘 크기 불변).
- EASY 몬스터 정확 8배: 기준 3/2/1/1 → 24/16/8/8(BalanceConfig 기준값×배수, 재적용 안전). NORMAL/HARD 유지. 스폰은 문/가구/상자/대문/입구 안전영역 회피, 3×3 여유 부족 시 예비 후보로 분산(조용한 감소·중첩 금지). 실측 56마리 avg 16.7ms(~60fps, LAB_PERF) — 공간 분할 불필요, 장애물 스냅샷 캐시로 복사 비용 제거.
- 처치당 HP 20 회복: 일반 공격·돌진 공용 applyPlayerStrike 경로, 사망 전환 이벤트 1회·최대 100 제한·사망자 부활 금지.
- F 돌진: 3타일/0.18s/쿨다운 3s(BalanceConfig), 엣지 입력만, 대각선 정규화, 0.3타일 하위 단계 충돌·타격(벽/문/가구 관통·중간 몬스터 건너뜀 없음), 대상당 1회 피해, 무적 없음. 귀환 경로 토글은 F→H 이동(버튼 유지), 달리기 소음 이벤트와 분리.
- 전투 HUD: 우하단 공격(빈 키캡+실제 바인딩 라벨 SPACE)·돌진(dash_icon+key_f) 아이콘, 쿨다운 어둡기+가림+남은 초 코드 렌더, 리사이즈 대응.
- 효과음 9종 연결(combat_loot_update_v1, 합성 CC0): 연타 press_low(기존 찰칵 대체)·개봉 open_heavy·타격 hit_01/02 교대(실제 피해만)·피격 hurt(무적 거절 제외)·사망 death(전환 1회)·문 열림 metal(닫힘→열림만)·공격 swing·돌진 whoosh. 타격음 80ms 창 3회 제한, 40ms 스로틀, 팀장 소음과 무관. 구 assets/sounds 2종은 사용 중단.
- 에셋 등록: assets/combat_loot_update_v1(runtime/ui 4종, runtime/audio 9종, 카탈로그·라이선스) — CombatUiAssets/CombatSoundAssets가 GameAssets 생명주기에서 소유·해제.
# 2026-10-15 컨테이너 효과음 V2 + 배경음악
- 상자/캐비넷 조작음을 경쾌한 "상자 까기" 소리로 교체(assets/container_sfx_v2, 직접 합성 CC0): 연타=밝은 나무 걸쇠 '탁!'(0.12s), 개봉=걸쇠 팝→뚜껑 삐걱→상승 3음 차임 C6·E6·G6(0.74s). 재생 규칙(엣지 1회·40ms 스로틀·마스터 음량)은 동일.
- 배경음악 추가: 사용자 제공 영상(제목 없는 디자인.mp4)에서 오디오 추출 → assets/music/background_music.ogg(3:20, OGG q5). GameAssets가 Music으로 소유·해제, 게임 시작 시 무한 반복 재생(맵 전환에도 연속), 음량 BalanceConfig.musicVolume=0.45, 무음 실행 LAB_MUTE_MUSIC=1.
# 2026-10-15 사망 재소환 + 난이도 모달 정리
- 사망 플로우: 쓰러짐 연출 후 2초(deathRespawnDelaySeconds) 뒤 가방 소지품 전부 상실(loseAllItemsOnDeath — 재화·진행 플래그 유지) → 페이드 전환 → 본부 스폰에서 부활(Player.revive, HP 최대). 재소환 요청은 사망 1회당 1회, 사망 중 늦은 회복 이벤트로는 부활하지 않음. 실행 검증: LAB_TEST_DEATH=1 → "items before death=2" → "revived at HQ hp=100 items=0".
- 난이도 선택 모달에서 몬스터 숫자 설명(슬라임 n · 연구원 n · 경비원 n) 표시 제거 — 난이도 이름만 표시.
# 2026-10-15 몬스터 이동 개편: 추격 리시 + 자유 로밍
- 몬스터가 플레이어를 무한 추격하지 않는다: 감지 반경 안이면 추격, 거리가 감지×2.2(monsterChaseGiveUpFactor)를 넘으면 포기하고 6타일 이상 떨어진 다른 공간의 목적지로 자유 이동(로밍). 도착 후 1.2s 휴식→재선정, 12s 미도달/양축 막힘 시 재선정. 한가할 때 기본 행동도 제자리 배회(wander) 대신 로밍으로 교체.
- 분노한 팀장은 기존 규칙대로 그 조우 동안 추격 유지, 비분노 팀장은 착석 유지. 로밍 중 재감지되면 다시 추격.
- MonsterRoamTest 4종 추가(추격/리시 포기/다른 공간 이동·벽 비침범/목적지 최소 거리/팀장 예외). 56마리 실행 60fps 유지.
# 2026-10-15 효과음 켜기/끄기 버튼 (sfx_toggle_ui_v1 적용)
- 우측 상단(연구소: 귀환 버튼 아래, 본부: 우상단) 44px 토글 버튼 — 청록 음파=켜짐, 주황 빗금=꺼짐(96px PNG, Nearest). 클릭 영역=보이는 버튼.
- 끄면 재생 중인 효과음 9종을 모두 정지하고 이후 재생을 차단(CombatSoundAssets.setSfxEnabled). 배경음악(Music)은 영향 없음(독립).
- 본부·연구소가 단일 CombatSoundAssets 인스턴스를 공유하므로 설정이 화면 간 유지된다(별도 저장 시스템은 아직 없어 세션 한정).
# 2026-10-16 효과음 토글 버튼 위치 이동
- 우상단(귀환 버튼 아래)에서 좌상단 HP 바 바로 오른쪽(프레임 세로 중앙 정렬)으로 이동 — 본부·연구소 동일 위치. 미니맵 영역과 겹쳐 안 보이던 문제 해결. 실행 캡처 sfx_toggle_lab.png 갱신.
# 2026-10-16 오디오 토글 확장 + 버튼 확대
- 토글 버튼 44→64px(HP 바 오른쪽 세로 중앙 정렬 유지, 클릭 영역 동일 확대).
- 버튼 한 번으로 효과음+배경음악 세트 on/off(GameAssets.toggleAudio): 끄면 효과음 정지·차단+음악 pause, 켜면 효과음 허용+음악 이어서 재생(반복 유지).
# 2026-10-16 몬스터 행동 상태 기계 개편 + 조작 키 정리
- AI 상태 분리: IDLE/WANDER/CHASE/ATTACK/SEARCH/RETURN. 발견=종류별 거리(4/5/7)+시야(벽·닫힌 문 차단 — sightBlocked, 벽 뒤 미발견), 해제=8타일·시야 상실 2s·활동 구역(홈) 12타일 이탈(발견<해제 히스테리시스로 경계 떨림 방지). 시야 상실 시 마지막 목격 지점만 기억·탐색(2~4s)하고 벽 너머 현재 위치를 추적하지 않음. 해제 후 걸어서 복귀(순간이동·HP 회복 없음), 복귀 중 재발견 가능.
- 종류별 배회: 슬라임=1~2타일 이동·0.8~2s 멈춤, 경비원=홈 주변 순찰 지점 2~3개 순환·짧은 대기, 연구원=장비 주변 2타일 배회·1.5~3s 대기, 팀장=착석 유지·달리기 2회 분노(근접 발견 없음)·해제 시 구석 복귀+분노·소음 누적 초기화. 배회 목표는 시야가 닿는 통행 가능 칸만(벽·가구 내부 금지), 휴식·출발 시점 개체별 분산.
- 다수 이동: 약한 분리(0.9타일)로 겹침·일렬 꼬리 완화(충돌·통로가 항상 우선), 사거리 안에서는 정지 후 공격. 감지·경로 재평가를 개체 id 분산 주기(0.15s)로 제한 — EASY 56마리 avg 16.9ms(~59fps) 유지. F1 디버그에 상태·목표·발견/해제 반경 표시.
- 조작 키 정리: 이동 WASD/방향키, 공격=마우스 클릭 전용(SPACE 공격 제거), SPACE=상호작용 전용(문/상자/수집/자판기), F=돌진, R=가방, 귀환 경로=미니맵 아래 버튼 클릭만(H 단축키 제거), UI 버튼 클릭은 공격으로 새지 않음. 공격 HUD 키캡 라벨 CLICK.
- MonsterBehaviorTest 7종(시야 발견/벽 차단/시야 상실→탐색/경계 무떨림/배회 유효성/팀장 걷기 무반응/팀장 복귀·초기화) — 총 142 테스트 통과. 이동 연속 캡처 ai_wander_t0s/t4s.png.
# 2026-10-16 획득 토스트 중앙 상단 이동 + 개봉음 둔탁화
- 아이템 획득 알림을 우상단 고정 박스("LOOT +가격", 이름 없음·잘림)에서 **화면 중앙 상단(HP 바 줄 바로 아래)**으로 이동. 아이콘 + 한글 이름 + 금액을 AWT 래스터 라벨(KoreanLabelCache)로 표시하고 박스 폭을 라벨 실측으로 계산해 긴 이름도 잘리지 않음. 실프레임 loot_toast_center.png.
- 상자/캐비넷 개봉음 V3(둔탁하게): 고음 차임 제거, 묵직한 걸쇠+깊은 뚜껑 '텅'(84/142Hz)+저역 삐걱+낮은 나무 공명(G3·C3)으로 재합성(container_open_v2.wav 교체, 연타음은 금속 틱만 소폭 절제). 파일명·재생 규칙 불변.
# 2026-10-16 개봉음 V4(더 묵직) + 획득음 추가(경쾌)
- 상자/캐비넷 개봉음을 더 묵직하게 재합성: 서브베이스 포함 이중 '쿵(58/96Hz)-텅(74/128Hz)' + 저역 삐걱 여운, 중·고역 최소화(0.72s).
- 아이템 획득음 신규(item_pickup_v1.wav, 0.4s): 경쾌한 상승 2음 차임 E6→A6 + 반짝임. 드롭을 실제로 가방에 넣은 순간(일반 수집·D 교체)에만 1회 재생 — 개봉음(묵직)과 획득음(경쾌)이 대비를 이룬다.
# 2026-10-16 몬스터 감속 + 개봉음 V5
- 몬스터 이동속도 하향: 슬라임 1.1→0.8, 연구원 1.8→1.3, 경비원 2.2→1.6, 팀장 2.0→1.5 (BalanceConfig).
- 개봉음 V5: 재생 음량 0.65→0.95, 피치 더 낮게(쿠웅 48/82Hz·터엉 62/108Hz), 감쇠를 늦춰 음마다 길게(총 0.7→1.08s), 삐걱·공명도 저역·장음화.
# 2026-10-16 개봉음 V6
- 더 낮게(쿠우웅 40/70Hz·터어엉 52/92Hz, 삐걱 65Hz 시작·공명 60Hz), 더 크게(노멀라이즈 0.99 + 재생 음량 1.0), 더 길게(감쇠 완화, 총 1.08→1.47s). 작은 스피커 대비 150Hz대 몸통 동반.
# 2026-10-16 개봉음 V7
- 훨씬 더 크고 길게: tanh 새추레이션(drive 2.4)으로 평균 음량(RMS) 대폭 상승 + 노멀라이즈 0.99·재생 1.0 유지, 감쇠를 절반 이하로 늦춰 총 1.47→2.4s(이중 쿠우웅-터어엉 + 이중 저역 공명 꼬리).
# 2026-10-16 효과음 역할 정정: 개봉음 V5 복원 + 연타음 저음·장음화
- 개봉 완료음을 V5(쿠웅 48/82Hz·터엉 62/108Hz, 1.0s, 노멀라이즈 0.92·재생 0.95)로 되돌림 — V6/V7의 과도한 음량·길이 철회.
- SPACE 연타음을 V3으로 교체: 금속 틱 제거, 낮은 나무 '투욱'(300/150/95Hz) + 저역 여운, 0.12→0.3s, 재생 음량 0.5→0.6. 연타 겹침 대비 저역 위주 절제.
# 2026-10-16 Mona 픽셀 폰트 적용 (OFL 1.1)
- MonadABXY/mona-font 릴리스에서 Mona12TextKR(+Bold) TTF 설치(assets/fonts/mona, OFL·원본 라이선스 동봉).
- gdx-freetype 도입: GameAssets가 24px(12px 설계 2배수, Nearest) BitmapFont 2종(uiFont/worldFont) 생성 — HP 숫자·연타 카운트·쿨다운 초·키캡 라벨이 Mona로 렌더. 렌더러들은 공유 폰트를 빌려 쓰고 GameAssets가 해제.
- AWT 한글 라벨(획득 토스트·난이도 모달·본부 SPACE 라벨·가방 알림)은 GameFonts.awtBold로 Mona 우선, 글리프 누락/로드 실패 시 시스템 한글 폰트 폴백.
- F1/F2 디버그 텍스트는 기본 폰트 유지. 실프레임: HP 100·4/5 카운트·"랜턴 +2000" 토스트 모두 Mona 확인(loot_toast_center.png 갱신).
# 2026-10-16 연타음 V4 + 난이도 모달 중앙 정렬
- 연타음: 노멀라이즈 0.98+재생 음량 0.6→0.85로 더 크게, 꼬리에 상승음(260→약 330Hz, drift) 추가 — '투우욱↗' 느낌(0.32s).
- 난이도 선택 모달의 쉬움/보통/어려움 라벨을 좌측 고정에서 각 행 가운데 정렬로 변경(제목·푸터와 동일한 centered 헬퍼).
# 2026-10-20 귀환 발자국 미표시 버그 수정
- 원인: ① 플레이어가 가구/상자에 붙어 서면 시작 셀 중심이 막힌 것으로 판정돼 경로가 빈 리스트, ② EASY 8배·로밍 몬스터가 복도를 완전히 막으면 몬스터 몸체 포함 그리드에서 경로 실패 → 버튼을 눌러도 발자국이 안 뜸.
- 수정: RoomPathfinder.shortestPathNear(시작/목표 셀을 반경 2까지 가까운 통행 셀로 보정) + 화면에서 동적 그리드 실패 시 정적 지형(벽·문·가구·상자) 폴백 — 안내가 항상 표시된다.
- ReturnRouteRegressionTest 3종(상자 밀착 시작/몬스터 복도 봉쇄 폴백/EASY 8배 전역 표본 40셀) 추가 — 총 145 테스트. 실행 캡처 return_route_footprints.png(56마리 환경 발자국 표시). LAB_AUTO_ROUTE=1 캡처 훅 추가.
# 2026-10-20 본부 보관함·선택 판매·영속 저장 (hq_sales_storage_v1 적용)
- profile 패키지(순수 Java): ItemInstance(인스턴스 ID 소유권)·PlayerProfile(schemaVersion/revision/currencyWon(long)/보관함/강화/귀환·거래 기록)·ProfileCodec(JSON, 마이그레이션: 누락 필드 기본값·재화 보존·알 수 없는 필드/강화 보존)·ProfileStore 인터페이스·FileProfileStore(임시 파일+fsync+백업+원자 교체, 손상 시 백업 복구/둘 다 손상 시 *.corrupt 보존)·HeadquartersEconomy(이관·판매 트랜잭션)·SaleSelection(선택 상태).
- 이관: 성공 귀환(사망 제외) 1회에 가방→보관함, 탐사 UUID로 중복 콜백/재시작 복제 방지, "다음 프로필 저장 성공 후에만" 가방 비움(실패 시 가방 유지+재시도, 판매창 열 때 자동 재시도).
- 판매: 선택/전체, 모델에서 수량·ID·판매 가능·가격 재검증(카탈로그 단일 기준, UI 합계 불신), long 정수 원+오버플로 검사, 거래 UUID 중복 차단, 저장 성공 후에만 차감·증가 반영(실패 롤백+재시도 안내). 알 수 없는 아이템은 보존+판매 불가 표시.
- UI: 기존 자판기 SPACE→판매창(스크롤 목록·수량 ±·전체 선택/해제·선택/전체 판매·전체 판매 확인창·X/Esc 닫기), 등급 테두리 재사용+청록 선택 테두리 바깥 겹침, 열려 있는 동안 월드 입력/클릭 차단, Esc는 창만 닫음. 자판기 위 판매 표식(장식, 충돌 없음). 재화는 프로필 단일 지갑.
- 저장 위치 ~/.2d-lab-polluted/(앱 전용), 변경 시에만 저장(프레임 저장 없음), 동기 순차 쓰기.
- 테스트 15종 추가(이관 중복/실패 롤백/부분·전체 판매/잘못된 수량·ID/중복 거래/코덱 왕복/마이그레이션/백업 복구/손상 보존/선택 상태) — 총 160개 통과. 실행 검증: 판매창 캡처 3장(hq_sale_*.png), 실플레이 수집→귀환 이관→판매(+210원)→재실행 복원(rev=4, 9450원) 확인.

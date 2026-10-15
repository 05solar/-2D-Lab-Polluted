# PROGRESS — 2D-Lab-Polluted 전체 진행내역

> 폐쇄 오염 연구소 탐사 2D 액션 게임. LibGDX 1.14.2 / Java 8 / Gradle 멀티모듈(core + lwjgl3),
> gdx-liftoff 기반, 패키지 `io.github.some_example_name`. 원격: github.com/05solar/-2D-Lab-Polluted (main).
> 커밋 저자: lotus05f@gmail.com 단독.

이 문서는 지금까지의 작업을 한눈에 보는 요약이다. 세부 작업 로그는 `docs/process.md`,
설계/규칙/테스트는 `ARCHITECTURE.md`·`GAME_RULES.md`·`TEST_PLAN.md`·`ROADMAP.md` 참고.

---

## 1. 커밋 타임라인 (최신 → 최초)

| 커밋 | 내용 |
|------|------|
| *(미커밋)* | **방 26개·복도 단축·컨테이너 개편**: 간격 압축(.55)+보조실 10개(+62%), 순환 링+지름길(문간 66.5→52.0, -21.8%), normal/hard 3×3 9방. 컨테이너 설정화(23/6/8)·캐비넷 1.4배·SPACE 연타 개봉(1/5/8회)·안내 UI·등급별 발광·합성 효과음 |
| *(미커밋)* | **V4 벽·문 통일**: 단일 논리 점유 → 8비트 마스크 렌더러(47종) + 가로/세로 V4 방문. 구형 V2/V3 스킨·InteriorWall·Exterior 링 제거(대문 V3만 유지). 쉬움/보통/어려움 공통 적용. 프레임 캡처 검증(F12/LAB_AUTO_CAPTURE) |
| `8575071` | **내부 직선 벽**도 새 금속 디자인(exterior V3)으로 교체(easy), 접합부·세로문은 v3 유지 |
| `3a727e8` | **연구소 외벽·대문 V3**(easy): 2타일 외벽 링 + 남쪽 8×4 대문(닫힘/열림·통로 충돌) |
| `284bd7f` | **미니맵 프레임** PNG 테두리 + **귀환 버튼** + 대문까지 **발자국 경로** PNG |
| `02c200b` | 벽면 가구 **뒷모습(남벽)** 연결 + 충돌을 바닥 footprint로(숨은 충돌·관통 제거) |
| `ecb38a4` | 네비게이션 WIP 스냅샷: 난이도 선택·대문·루트·가구·길찾기 자산/코드 대량 |
| `d2ec6e5` | **HP HUD + 8칸 가방 UI** 적용 |
| `2dc6e7c`·`8ffa8f9`·`661ae06` | 본부 물통 SPACE 프롬프트 위치 미세 조정(3회) |
| `111eb96` | 전투·몬스터·사망연출·본부 맵·맵전환·상호작용 WIP 스냅샷 |
| `823caad`·`526bae5` | 벽 투명부 검은 빈 공간 제거 + F2 타일 디버그 |
| `605e3a3` | **Laboratory Tileset V2** 전면 적용(바닥24/벽16/구조물16/오버레이24) |
| `632f1c2` | 연구소 맵 자연스럽게 재구성(네 구역·유기적 위험·가짜 가구 제거) |
| `f87d025` | 게임 창 640×480 → 1280×960 |
| `f8761c1` | 디버그 충돌 테두리 기본 off |
| `0c14e48` | 최초 커밋: 1단계 기본 플레이 공간 |

---

## 2. 현재 구현 상태 (기능별)

### 코어 플레이 (완료·안정)
- 본부(HQ 텐트, 14×10)에서 시작. 이동(WASD/방향키)·달리기(Shift)·일시정지(Esc).
- **좌상단 HP HUD**(프레임+폭 가변 채움+숫자, 색 임계 >50/>25/≤25) — 본부·연구소 양쪽 항상 표시.
- **R 8칸 가방**: 열기/닫기, 중앙 패널+딤, 가득 차면 알림, D로 최저가치 교체.
- 본부 남쪽 대문 넘기 → **난이도 선택 모달(쉬움/보통/어려움)** → 0.35s 페이드 → 연구소 입장.
- 연구소 → 남쪽 대문 → 본부 복귀. `PlayerSessionState`로 HP·재화·인벤토리·진행 맵 간 유지.

### 전투·몬스터 (기본 완료)
- 몬스터 4종(슬라임20·연구원40·경비원60·팀장70), 시드 기반 배치, wander/chase/attack, HP/사망/넉백.
- 플레이어 근접공격(히트프레임, 피해15)·무적시간·HP바. 팀장 달리기-소음 2회 분노.
- 플레이어 **사망 연출**(8프레임 방향 시트, 사망 후 입력 차단). 게임오버/부활은 미구현.

### 루트·상자 (완료 — 2026-10-14 연타 개봉 개편)
- `assets/loot/loot_tables.json` 단일 진실(가격·확률·등급 유지). 나무상자=COMMON·일반캐비넷=EPIC·고급캐비넷=LEGENDARY.
- 개수 설정화(`BalanceConfig`): easy 14/7/2=23(26방 대부분 ≥1, 도서관·화학실 복수), normal 6, hard 8. 캐비넷 1.4배 표시(동일 하단-중앙 기준점) + 같은 배율의 바닥 점유/충돌.
- **SPACE 연타 개봉**: 나무 1회/일반 5회/고급 8회. 엣지 입력만 1회, 진행도 컨테이너별 저장, 바라보는 가장 가까운 1개만 대상. 도달 입력에서 1회만 개봉+추첨. 대상 위 SPACE 안내+카운트, 등급별 발광(보라/금/파랑), 합성 효과음(찰칵/해제음, `assets/sounds/`).
- 바닥 드롭→SPACE 수집(별도 입력), 가득 차면 D 가치 교체.

### 맵·타일·벽·대문 (전 난이도 V4 통일)
- **바닥·오버레이**: Laboratory Tileset V2(바닥 변형 seed·오버레이). 벽 투명부 아래 바닥 보장(검은 void 제거).
- **벽·문 = V4 단일 규격**(`laboratory_structure_v4`): 하나의 논리 점유 그리드에서 8비트 마스크(N=1…NW=128,
  대각선은 양옆 직교 이웃 모두 벽일 때만)로 47종 아틀라스 타일을 고른다(`WallMaskV4`+`LaboratoryStructureRendererV4`).
  직선/양면/바깥·안쪽 코너/T/십자/끝단/채움이 같은 체계. 구형 V2/V3 접합부 스킨·InteriorWall 덧그림·Exterior 링
  드로우는 제거(보라 접합부 없음).
- **내부 방문**: V4 가로(door_horizontal_*)·세로(door_vertical_*, 전용 탑뷰) 닫힘/열림. `Door` 객체가 시각·충돌·
  토글을 함께 결정(열린 통로 통행, 닫힌 문짝 추가 충돌). 문틀은 양옆 벽과 시각 연결.
- **외벽**: easy는 2타일 두께를 실제 벽 셀 두 줄로 구성(V4가 그림, `forcedWall` 마스크). 남쪽 8×4 **대문은 기존 V3 유지**
  (닫힘/열림·통로 충돌·SPACE 토글·본부 복귀). normal/hard는 1타일 테두리.
- 쉬움 맵: 128×96, 간격 압축(.55) 캠퍼스 — 테마 방 16 + 보조실 10 = **26방**(경비초소↔입구·전기실↔경비초소·창고↔휴게실 직접 연결 포함). 외곽 순환 링+십자+1타일 지름길로 문간 평균 66.5→52.0(-21.8%, 맨해튼 하한 41.8). 보통/어려움: 3×3 구획 9방(+50%), V4 문 12개. F1 충돌 디버그·F2 타일 디버그·**F12 프레임 저장**.
- 벽면 가구: 북=정면·남=뒷모습(v8)·좌우=측면, 충돌은 실제 바닥 footprint.

### 네비게이션 (완료)
- 우상단 **미니맵**(프레임 PNG 테두리, 방·플레이어·대문·팀장 마커).
- 미니맵 아래 **귀환 버튼**(클릭/F) → `RoomPathfinder`로 대문까지 경로 → **발자국 PNG** 바닥 표시.

### 상호작용
- 방문 SPACE 개폐(충돌 동기화), 대문 SPACE 토글, 상자/드롭 수집 — 한 키 중복 실행 방지.
- 본부 자판기(상점 UI 미구현, 로그만)·정수기(HP 완전 회복).

---

## 3. 아키텍처 요약 (주요 패키지)

```
world/     Laboratory*(맵·타일·방·레이아웃) + Headquarters* + Door/DoorState/DoorPlacementResolver
           + MapId/MapTransitionController/PlayerSessionState + GameDifficulty + RoomPathfinder
           + Loot*(Container/Placement/Drop) + Laboratory(Prop/RoomArea/FurnitureCoverage)
           + WallMaskV4(8비트→47종) + WallAutotiler(구형, 테스트 맵 전용)
entity/    player/(Player·PlayerState·PlayerDeathAnimation) + monster/
combat/    CombatSystem·MonsterAiSystem·PlayerCombatState·CombatCollisionGrid·MonsterSpawnSystem
inventory/ Backpack(8칸·가치교체) + LootItem + LootTableCatalog
interaction/ VendingMachineInteraction
render/    World·Entity·Monster·Combat·Headquarters·Fade·TileDebug·Debug
           + PlayerHud·Navigation·DifficultyModal·ExteriorWall(대문 전용)
           + LaboratoryStructureRendererV4(벽·문) + FrameCapture(F12 프레임 저장)
           + InteriorWallRenderer(구형, 미사용)
asset/     GameAssets(텍스처 단독 소유·해제) + Headquarters·PlayerHud·RoomKit·LaboratoryFurniture
           + Loot·DifficultyModal·ExteriorWall · LaboratoryStructureV4Assets
           · HudLayout · LaboratoryTileCatalogV2/TileSetV2
screen/    LaboratoryScreen + HeadquartersScreen (+ BaseScreen)
config/    AssetPaths · BalanceConfig
```
- 렌더·충돌·길찾기·미니맵은 모두 같은 논리 벽(`isSolid`/`blocksActorAt`)과 prop 데이터를 공유 → 시각 변경이 충돌로 새지 않음.
- 텍스처 생명주기는 GameAssets 한 곳. UI 에셋도 Nearest 필터.

### 설치된 에셋 팩 (`assets/`)
`laboratory_tiles_v2`(바닥/오버레이), `laboratory_structure_v4`(벽 47종 아틀라스+가로/세로 방문), `hq_tent`,
`monsters`, `laboratory_roomkits_v3`, `laboratory_orthographic_views_v7`,
`laboratory_orthographic_views_v8`(가구 뒷모습), `loot`, `difficulty_modal_v2`,
`ui/player_hud_inventory_v1`, `ui/navigation`, `exterior_walls_gate_v3`(대문), `textures/player`, `data`.

### V4 파일 ↔ 사용 매핑
- `laboratory_structure_v4/runtime/walls/walls_blob47_atlas_128.png` → `LaboratoryStructureV4Assets.wall(index)`,
  index는 `WallMaskV4.atlasIndex(rawMask)`(8열×6행, 128px).
- `runtime/doors/door_horizontal_closed|open.png`(256×96) → `horizontalDoor(open)` — 벽이 동서, 남북 통행.
- `runtime/doors/door_vertical_closed|open.png`(96×256) → `verticalDoor(open)` — 벽이 남북, 동서 통행.
- 방향: 월드 북(y+1)=이미지 위. 가로벽 셀=HORIZONTAL door, 세로벽 셀=VERTICAL door(`Door.Orientation`).

---

## 4. 남은 작업 / 미해결

- ~~벽/문 통일·세로문·접합부~~ **완료**: 전 난이도 V4 단일 규격(47종 연결 벽 + 가로/세로 전용 방문). 보라 접합부·구형 방문 제거, T/십자/안쪽 모서리까지 같은 체계.
- ~~방 수·복도·컨테이너 개편~~ **완료(2026-10-14)**: 쉬움 26방·복도 -21.8%(하한 41.8로 -40%는 기하적으로 불가), normal/hard 3×3 9방. 컨테이너 23/6/8·1.4배·연타 개봉·발광·효과음.
- **가구 배치 미세 조정**: 방향(북=정면/남=뒷모습/좌우=측면)은 정상 동작. 벽면 작업구역화·중앙 섬 묶기 추가 개선 여지.
- **본부 경제**: 자판기 **상점 구매 UI**, 가방 **반납→재화 전환**(`Backpack.handOver` 연결점만) 미구현.
- 게임오버/부활 플로우, 저장/불러오기, hazard(독성/감전) 실제 피해.
- 월드 아이템 획득: 상자/드롭 외 일반 바닥 아이템은 임시 디버그(G) 수준.

## 5. 검증·환경 메모

- 매 작업: `./gradlew.bat core:test` + `lwjgl3:build` 통과. 현재 113 tests 0 fail, 빌드 성공.
- **실제 GL 렌더 프레임 저장**(합성 미리보기 아님): `render/FrameCapture`가 백버퍼→PNG. F12 키 또는
  `LAB_AUTO_CAPTURE=1`(30프레임 UI, 42프레임 F1 충돌, 46프레임 자동 종료). 저장: `docs/images/frames/`.
  - 재현: `LAB_START_LAB=1 LAB_AUTO_CAPTURE=1 LAB_MAP_OVERVIEW=1 LAB_DISABLE_MONSTERS=1 LAB_DIFFICULTY=EASY ./gradlew.bat lwjgl3:run`
  - 난이도: `LAB_DIFFICULTY=EASY|NORMAL|HARD` · 방 지정: `LAB_START_ROOM=CHEMISTRY` · 아틀라스 갤러리: `LAB_WALL_TEST=3`
  - 캡처 산출물: `frame_001_lab_w128h96_ui.png`(easy 전체), `..._collision.png`(F1), `..._w28h20_*`(NORMAL),
    `..._w38h28_*`(HARD), `frame_001_lab_v4atlas_ui.png`(47종+문4종 갤러리).
- 실행: 본부 시작 → 남쪽 대문 → 난이도 선택 → 연구소 탐색/전투/상자/가방 → 미니맵·F 발자국 → 대문으로 복귀.
- **사용자 핸즈온 확인 권장(백그라운드로 미검증)**: 3방 이동·내부 방문 닫힘→열림 토글·캐비넷 획득·F 귀환 경로,
  본부↔연구소 왕복(난이도 선택·페이드·HP HUD).

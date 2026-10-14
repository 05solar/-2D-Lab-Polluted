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

### 루트·상자 (완료)
- `assets/loot/loot_tables.json` 단일 진실. 나무상자=COMMON·일반캐비넷=EPIC·고급캐비넷=LEGENDARY.
- SPACE로 상자 열기(1회 추첨)→바닥 드롭→SPACE 수집, 가득 차면 D 가치 교체.

### 맵·타일·벽·대문 (easy 맵 중심)
- **Laboratory Tileset V2**(바닥 변형 seed·오버레이·4레이어 렌더 floor→overlay→wall→structure).
- 벽 투명부 아래 바닥 보장(검은 void 제거). F1 충돌 디버그·F2 타일 디버그.
- 쉬움 맵: 128×96, 16개 명명 방(화학·생물·일반랩·준비·정보·CCTV·표본·도서관·휴게·취침·화장실·탈의·샤워·기록·탕비·입구) + 십자 스파인 복도.
- **외벽**: exterior_walls_gate_v3 2타일 금속 링(직선 신축+바깥 모서리) + 남쪽 8×4 대문(닫힘=통로 차단/열림=통행, SPACE 토글).
- **내부 벽**: 방 경계·복도 직선 벽도 새 금속 직선으로 교체(방 쪽 면 선택). 접합부(모서리/T/십자/끝단)는 v3 유지.
- 벽면 가구: 북=정면·남=뒷모습·좌우=측면, 충돌은 실제 바닥 footprint.

### 네비게이션 (완료)
- 우상단 **미니맵**(프레임 PNG 테두리, 방·플레이어·대문·팀장 마커).
- 미니맵 아래 **귀환 버튼**(클릭/F) → `RoomPathfinder`로 대문까지 경로 → **발자국 PNG** 바닥 표시.

### 상호작용
- 방문 SPACE 개폐(충돌 동기화), 대문 SPACE 토글, 상자/드롭 수집 — 한 키 중복 실행 방지.
- 본부 자판기(상점 UI 미구현, 로그만)·정수기(HP 완전 회복).

---

## 3. 아키텍처 요약 (주요 패키지)

```
world/     Laboratory*(V2 맵·타일·방·레이아웃) + Headquarters* + Door/DoorState/DoorPlacementResolver
           + MapId/MapTransitionController/PlayerSessionState + GameDifficulty + RoomPathfinder
           + Loot*(Container/Placement/Drop) + Laboratory(Prop/RoomArea/FurnitureCoverage) + WallAutotiler
entity/    player/(Player·PlayerState·PlayerDeathAnimation) + monster/
combat/    CombatSystem·MonsterAiSystem·PlayerCombatState·CombatCollisionGrid·MonsterSpawnSystem
inventory/ Backpack(8칸·가치교체) + LootItem + LootTableCatalog
interaction/ VendingMachineInteraction
render/    World·Entity·Monster·Combat·Headquarters·Fade·TileDebug·Debug
           + PlayerHud·Navigation·DifficultyModal·ExteriorWall·InteriorWall
asset/     GameAssets(텍스처 단독 소유·해제) + Headquarters·PlayerHud·RoomKit·LaboratoryFurniture
           + Loot·DifficultyModal·ExteriorWall · HudLayout · LaboratoryTileCatalogV2/TileSetV2
screen/    LaboratoryScreen + HeadquartersScreen (+ BaseScreen)
config/    AssetPaths · BalanceConfig
```
- 렌더·충돌·길찾기·미니맵은 모두 같은 논리 벽(`isSolid`/`blocksActorAt`)과 prop 데이터를 공유 → 시각 변경이 충돌로 새지 않음.
- 텍스처 생명주기는 GameAssets 한 곳. UI 에셋도 Nearest 필터.

### 설치된 에셋 팩 (`assets/`)
`laboratory_tiles_v2`, `hq_tent`, `monsters`, `laboratory_roomkits_v3`, `laboratory_orthographic_views_v7`,
`laboratory_orthographic_views_v8`(가구 뒷모습), `loot`, `difficulty_modal_v2`,
`ui/player_hud_inventory_v1`, `ui/navigation`, `exterior_walls_gate_v3`, `textures/player`, `data`.

---

## 4. 남은 작업 / 미해결

- **NORMAL/HARD 변형 맵**: 외벽 V3·내부 새 벽 미적용(기존 1타일 외곽). easy 맵 중심으로 작업됨.
- **내부 방문 교체**: 미완. 새 외벽 팩엔 가로 대문만 — 내부 가로문은 대문 축소로 가능하나 **세로(측면)문 전용 에셋 없음**(정면 대문 회전 금지).
- **본부 경제**: 자판기 **상점 구매 UI**, 가방 **반납→재화 전환**(`Backpack.handOver` 연결점만) 미구현.
- 게임오버/부활 플로우, 저장/불러오기, hazard(독성/감전) 실제 피해.
- 월드 아이템 획득: 상자/드롭 외 일반 바닥 아이템은 임시 디버그(G) 수준.
- 접합부(T/십자/안쪽 모서리)·내부 직선 벽 단면 특성: 현재 접합부는 v3 유지(새 팩에 조각 없음). 완전 통일하려면 양면 내부 직선·T/십자/안쪽 모서리·세로문 추가 에셋 필요.

## 5. 검증·환경 메모

- 매 작업: `./gradlew.bat core:test` + `lwjgl3:build` 통과 후 커밋. 현재 테스트 다수 통과.
- **백그라운드 세션이라 실제 게임 창 픽셀을 직접 캡처할 수 없음.** 실제 배치 데이터(레이아웃/덤프)를 실제 PNG로
  재구성한 합성 미리보기(`docs/images/*`)와 실행 로그로 검증해 왔고, **라이브 눈 확인은 사용자가 `./gradlew.bat lwjgl3:run`** 으로 수행.
- 실행: 본부 시작 → 남쪽 대문 → 난이도 선택 → 연구소 탐색/전투/상자/가방 → 미니맵·F 발자국 → 대문으로 복귀.

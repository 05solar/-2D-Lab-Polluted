# ARCHITECTURE — 현재 구현 구조

> 이 문서는 **지금 실제로 존재하는 것만** 기록한다.
> 아직 만들지 않은 클래스를 구현 완료처럼 적지 않는다. 계획은 `docs/ROADMAP.md`.

## 모듈 구성
| 모듈 | 책임 |
|------|------|
| `core` | 플랫폼 무관 게임 로직 |
| `lwjgl3` | 데스크톱 실행 설정과 런처만 |

- 빌드: Gradle 멀티모듈, Java 8, LibGDX 1.14.2 (+ 테스트 JUnit 4.13.2)
- 기본 패키지: `io.github.some_example_name` (gdx-liftoff 생성값, 변경하지 않음)
- `core`는 `lwjgl3`를 참조하지 않는다.

## 현재 실행 구조

루트 게임이 자산과 맵 간 세션을 소유하고, 각 Screen은 자기 맵의 입력·시스템·렌더 순서를 조정한다.

```
core/.../io/github/some_example_name/        (2026-10-23 기준 — 패키지 단위 요약)
├── LaboratoryGame.java              # GameAssets/세션/프로필/맵 전환/보스 run 소유
├── asset/                           # GameAssets(공유 로딩·해제 소유자) + 기능별 로더
│   │                                # HQ/HUD/RoomKit/가구/loot/전투음·UI/판매/강화/스킬/
│   └── BossAssets·BossNineAssets    # 보스 1-1 레거시·9단계 시트/VFX/초상/카드
├── config/AssetPaths·BalanceConfig  # 경로 상수·전투/이동/난이도 수치
├── input/                           # InputState/GdxPlayerInput/KeyBindings/GameAction/Controller
├── movement/, collision/            # delta 이동, AABB 충돌 해석(swept 하위 단계)
├── world/                           # 연구소/본부 논리 맵·충돌·문·세션·맵 전환
│   ├── LaboratoryGenerator/GenRng/LabGenData # 난이도별 시드 생성(틀 12·테마 16·BFS·폴백)
│   └── WallMaskV4 / Door* / PlayerSessionState / MapId·MapTransitionController
├── entity/player·monster            # 상태·HP·위치(텍스처 비소유)
├── combat/                          # CombatSystem/MonsterAiSystem(상태 기계)/스폰/플레이어 전투
│   └── Shockwave·ElectricProjectile·PlayerShieldState·SkillCast·PlayerDashState # 스킬 판정(순수 Java)
├── boss/                            # 보스 전투(순수 Java — GL 비의존, Context 주입)
│   ├── BossStageCatalog·BossPatternCatalog·BossArenaCatalog # *.runtime.json 파서
│   ├── BossDefinition·BossFight     # 1-1 레거시(회귀 고정)
│   └── BossBattle                   # 9단계 범용 패턴 엔진(kind 12종·해저드 소유)
├── profile/                         # PlayerProfile·ProfileCodec(원자 저장+백업+마이그레이션),
│   │                                # HeadquartersEconomy(거래/보스 커밋), UpgradeCatalog/Effects,
│   └── SkillCatalog·SkillLoadout·SkillRuntime·SkillService # 스킬 소유/장착/키/쿨다운 분리
├── inventory/, loot/                # 8칸 가방, loot 테이블·컨테이너
├── interaction/                     # 엣지 입력 상호작용(자판기 등)
├── render/                          # 월드/엔티티/HUD/패널 렌더러(상태 읽기 전용) + FrameCapture
│   └── Boss*Renderer                # HP바/단계 선택(3×3 실루엣)/결과창
└── screen/
    ├── HeadquartersScreen           # 고정 카메라, 상호작용 5종·패널(판매/강화/스킬/보스)
    ├── LaboratoryScreen             # 추적 카메라, 이동·전투·AI·loot·HUD
    └── BossArenaScreen              # 보스 광장(레거시/범용 엔진 분기 1곳)

lwjgl3/.../lwjgl3/                   # Lwjgl3Launcher(진입)·StartupHelper
```

미구현으로 남은 주요 영역: 자판기 구매 UI(판매 전용), 진행 중 탐사 저장/복원(본부 영속
프로필만), hazard 타일 실제 피해, 게임오버 연출 화면(재소환 플로우는 구현됨).
별도 로딩 화면은 없다.

## 배경 타일: 논리 / 시각 / 충돌 분리
배경은 세 가지 책임으로 나뉜다. 하나의 타입이 충돌·시각·배치를 모두 결정하지 않는다.
- **논리 셀**(`TileType`, `Hazard`, `Door`): 바닥·벽·문 상태와 위험 의미를 관리한다. 닫힌 문은 고체, 열린 문은 통과 가능하다.
- **시각 데이터**(`RoomVisuals`, `LaboratoryTileCatalogV2`, `LaboratoryTileSetV2`): floor/overlay/wall/structure ID를 JSON 카탈로그로 TextureRegion에 연결한다.
- **충돌 정보**: `LaboratoryRoom`과 `CombatCollisionGrid`가 논리 셀·문 상태·가구 충돌 영역을 조회한다.

배치/생성은 `LaboratoryLayout`이 담당한다. 고정 문자맵을 파싱해 (1) 논리 격자와
(2) 시각 격자를 만들고, 벽('#') 셀의 시각은 `WallAutotiler`가 이웃(상/하/좌/우, 필요 시 대각)을
검사해 방향별 벽/모서리로 선택한다. 벽 시각 방향이 바뀌어도 충돌은 동일한 논리 벽 셀을 쓴다.
랜덤이 없어 같은 데이터면 항상 같은 방이 재현된다. `RoomVisuals`는 1회 계산 후 불변이다.

> 독성·전선 타일은 hazard 의미로 구분되지만 플레이어 지속 피해 규칙은 없다. 닫힌 문은 벽처럼 충돌한다.

## 좌표계 / 단위
- 월드 단위: **1 타일 = 1 단위**, y축은 위로 증가. 화면 픽셀과 혼용하지 않는다.
- 연구소 카메라: `FitViewport` 12×9, 플레이어 추적 및 방 경계 클램프.
- 본부 카메라: 14×10 맵 전체와 여백을 고정 표시하는 `FitViewport`; 플레이어를 추적하지 않는다.
- 플레이어 충돌 박스는 좌하단 기준 Rectangle(임시 0.6×0.5). 스프라이트는 발밑(하단 중앙) 앵커 1×1.

## 의존성 방향
```
Screen -> World/System -> Entity/Domain
       -> Renderer/UI  -> (읽기 전용 상태)
Input  -> Controller   -> PlayerIntent
Config -> 각 시스템/엔티티 생성
```
- 엔티티는 화면/렌더러를 참조하지 않고 Texture를 소유하지 않는다.
- 입력 폴링(GdxPlayerInput)만 LibGDX 입력 API를 읽는다. 로직 계층은 InputState/Intent만 본다.
- 전역 mutable singleton 없음. 자산은 `GameAssets`(생명주기 명확)만 루트에서 생성·전달.

## 화면별 프레임 흐름
- `LaboratoryScreen`: delta 상한 → 입력·일시정지/가방/디버그 → 생존·활성 상태에서 플레이어 의도와 이동/충돌 → 플레이어 공격 상태 → 몬스터 AI·이동·공격 → 사망이면 공격 취소, 아니면 명중 프레임 판정 → 카메라 → 맵·몬스터·플레이어 → 전투 표시 → HUD/가방 → 디버그·페이드.
- `HeadquartersScreen`: delta 상한 → 입력·일시정지/가방 → 이동/충돌·남쪽 출구 판정 → 자판기/물통 근접 프롬프트와 Space 엣지 처리 → 본부·플레이어 → HUD/가방 → 페이드·디버그.
- 일시정지, 전환 잠금, 가방 오버레이 중 월드 행동과 애니메이션은 진행하지 않는다. 사망 플레이어 입력과 상호작용은 거부한다.

## 화면 전환
- `Lwjgl3Launcher` → `LaboratoryGame.create()` → `HeadquartersScreen`에서 시작.
- 본부 남쪽 출구 요청은 `MapTransitionController`가 입력 잠금과 페이드아웃/교체/페이드인을 관리하고, `LaboratoryGame`이 기존 세션 플레이어를 연구소 입구에 배치한다.
- 연구소에서 본부로 돌아가는 트리거는 아직 없다. 별도 LoadingScreen과 GameOverScreen도 없다.

## 자산 생명주기
- `GameAssets`가 연구소 타일 아틀라스, 플레이어·사망·몬스터 시트, 본부 에셋, HP/가방 UI 에셋을 로드하고 종료 시 해제한다. 타일 텍스처는 Nearest 필터를 사용한다.
- 타일셋은 **실제 픽셀 크기와 무관하게 4열×4행 격자로 분할**한다(`splitIntoGrid`). 제공 파일이
  1254×1254라 64px 고정 분할은 틀리며, 각 셀 경계를 실제 크기 기준으로 반올림해 빈틈 없이 나눈다.
  플레이어 시트는 512×512/8×8이라 64px로 정확히 나뉜다.
- `LaboratoryGame.dispose()`에서 화면 렌더 자원 → 공유 `GameAssets` 순으로 해제한다. 화면이 바뀌어도 공유 텍스처를 다시 만들지 않는다.
- AssetManager + 로딩 화면 도입은 이후 단계 과제. (현재는 소유자 단일 해제로 규칙 충족)

## 저장 방식
- 아직 없음. 런타임 엔티티를 직접 직렬화하지 않고 `SaveData`로 변환하는 방식을 6단계에서 도입.

## Laboratory Tileset V2 (타일 배치 전면 교체)

에셋 경로·생명주기: 런타임 루트 `assets/laboratory_tiles_v2/`. `GameAssets`가 유일한 텍스처 소유자로
`laboratory_tiles_v2.json`을 읽어 4개 아틀라스(floor/wall/structure/overlay)를 로드하고 종료 시 해제한다.
구버전 단일 타일셋(`laboratory_tileset_64.png`)은 제거했고 더 이상 로드하지 않는다.

책임 분리(논리/데이터/시각):
```
world/
├── LaboratoryTileCatalogV2.java  # JSON 파싱(순수 Java). 타일 ID→(아틀라스,행/열,인덱스,충돌,태그). 인덱스 하드코딩 금지의 단일 출처
├── LaboratoryTileSetV2.java      # 카탈로그+텍스처 → ID→TextureRegion(반텍셀 인셋). 텍스처 생성 안 함(GameAssets 소유)
├── TileType.java                 # 충돌/문 상태만(FLOOR/WALL/DOOR_CLOSED/DOORWAY_OPEN)
├── Hazard.java                   # NONE/TOXIC/SHOCK (시각과 분리된 위험 규칙)
├── LaboratoryZone.java           # ENTRANCE/CENTRAL/CONTAM/MAINT
├── WallAutotiler.java            # 이웃(직교+대각) → 벽 타일 ID(직선/끝/바깥·안쪽 모서리/T자/십자). OOB=비연결
├── FloorVariantResolver.java     # 고정 seed 바닥 변형(구역별 분포, 3연속 금지)
├── OverlayResolver.java          # 고정 seed 오버레이(독성 군집/전선/스파크/장식) + hazard
├── LaboratoryLayout.java         # 문자 맵 → 논리/구조물/구역 → 4레이어 RoomVisuals + hazard 포함 Room 빌드
├── RoomVisuals.java              # 레이어별 타일 ID: floor/overlay/wall/structure (1회 계산)
└── LaboratoryRoom.java           # 논리 타일 + hazard 질의(+SolidGrid)
```
렌더 레이어(한 배열에 섞지 않음): `WorldRenderer`가 floor→overlay→wall→structure 순으로 전체 맵을 그리고,
그 뒤 `LaboratoryScreen`이 플레이어→(F1 충돌/F2 타일 디버그)를 그린다. 텍스처는 Nearest + ClampToEdge.
`floor`는 벽·문을 포함한 20×15 전 셀에서 필수이며, 바닥 렌더 동안 블렌딩을 끄고 투명 오버레이·벽·문 전에
`SRC_ALPHA/ONE_MINUS_SRC_ALPHA` 블렌딩을 다시 켠다. 벽의 방향과 파손 변형 선택은 `WallAutotiler` 한 곳에서 한다.
직선 벽만 좌표 기반으로 약 12.5% 파손 변형을 사용하며 모서리·끝·T자·십자·문은 전용 방향 타일을 유지한다.

## Connected wall and door pipeline (2026-10-07)

`LAB_WALL_TEST=1` renders a 9x9 atlas-shape gallery; `LAB_WALL_TEST=2` renders a 9x9 room with assembled horizontal and vertical doors. Default production enters the selected campaign layout. Coordinates are world y-up (`north = y + 1`); JSON atlas rows start at the PNG top, and TextureRegion is not flipped.

`WallSeamAtlasGenerator` derives 64x64 RGBA connected atlases from the preserved V2 source art. Straight metal bodies reach the exact cell edge; end caps appear only on end or jamb tiles. Floor seam variants are excluded from random placement because their isolated rails looked like stray walls.

`DoorPlacementResolver` infers orientation from a five-cell straight run and reserves adjacent wall cells for jambs. `Door` owns fixed tile coordinates and orientation plus its state; `LaboratoryRoom` derives collision and `RoomVisuals` derives the current structure ID from the same object. `WallAutotiler` owns cardinal masks and visual connector metadata. `WallTopologyValidator` checks masks, jambs, collision and reciprocal visual connections once after layout creation. `WorldRenderer` still draws floor, overlay, wall, then structure at one cell anchor with no per-tile offsets.

## Independent headquarters map and transition (2026-10-07)

`LaboratoryGame` owns one `GameAssets`, `PlayerSessionState`, both layouts, and `MapTransitionController`. The default first screen is `HeadquartersScreen`; `LaboratoryScreen` receives the same player on transition. `MapId` distinguishes the maps. `HeadquartersLayout` builds a fixed 14×10 tent independently from `LaboratoryLayout`; `HeadquartersRoom` owns logical collision, prop foot rectangles, interaction locations, and the one-shot south exit. `HeadquartersVisuals` owns floor/decor/wall/prop IDs. No HQ monsters are created.

`GameAssets` owns `HeadquartersAssets`, which reads the supplied `hq_tent_assets.json` and original individual PNGs, plus a white pixel used by `FadeOverlayRenderer`. HQ tiles draw at 64px logical size, furniture at their source 128×96 proportions, and vending at 96×128. Source tile PNGs are opaque RGB; furniture and vending PNGs have alpha. The HQ renderer keeps alpha blending on, draws floor/decor/walls, then sorts props with the player by foot Y. Prop collision uses only a ground footprint. Source art remains unrotated and unflipped.

The pure Java transition controller follows `IDLE → FADING_OUT → SWITCHING_MAP → FADING_IN → IDLE`; each fade lasts 0.35 seconds (temporary config value). Screens ignore movement, attack, and interaction input while locked. At full black, the root replaces the screen and moves the shared player to the laboratory safe spawn. The fade uses full window pixel coordinates, independent of the world camera. Session HP, money, inventory, and progress are retained. HQ F1 draws collision, trigger, vending range, and spawn separately.

The laboratory's vertical side doors use the supplied `lab_side_doors_2x2_64.png`. `GameAssets` loads it as a 64px grid and replaces the render regions for the existing `vertical_door_closed/open` IDs; the JSON logical solidity and door state stay unchanged. Source row 0 column 0 is closed, column 1 is open. Arrival spawn is the floor cell directly north of the south entrance.

## Player death animation

`Player.takeDamage` clamps HP to zero and changes `PlayerState` from `ALIVE` to `DEAD` only once. The renderer's `AnimationController` owns a delta-driven `PlayerDeathAnimation` timeline and selects `death_down/left/right/up` from the last facing direction. The timeline is non-looping and clamps at frame 7. `GameAssets` loads the separate 512x256 RGBA sheet at Nearest filtering and disposes it with the other shared textures. Dead-player intent, movement, attack, combat, and HQ exit interactions are rejected by their existing controller/system entry points; Esc and debug toggles remain available.

## Compact headquarters shop (2026-10-08)

`HeadquartersLayout` is a separate fixed 14x10 tent with one south exit and clustered wall-side props. The terminal desk art includes its monitor and is centered on the north side; the vending machine occupies the northeast wall. `HeadquartersScreen` uses a centered `FitViewport` sized to the full map plus margin, so it never tracks the player or crops the room at different window ratios. Vending and water prompts become visible within a 2.5-tile proximity radius, independent of facing. `HeadquartersRoom` separately checks the vending front approach and unobstructed interaction range; the water dispenser accepts a clear approach within 2 tiles. A SPACE edge at the dispenser restores a living player's HP to maximum; the dead state is not cleared. Separate one-shot feedback trackers drive the selected target prompt. `HeadquartersAssets` owns the supplied three-frame keycap sheet and separately rasterized Korean vending/health labels. The water prompt is centered on the dispenser. The vending event is emitted without opening a placeholder shop UI.
## First combat implementation (2026-10-07)

- `BalanceConfig` owns initial combat values; `HealthComponent`, attack state, `CombatSystem`, `MonsterAiSystem`, and `MonsterSpawnSystem` hold gameplay rules independently of renderers.
- `GameAssets` owns the player and four monster sprite sheets. Animation frame arrays are materialized as typed `TextureRegion[]` arrays before constructing LibGDX `Animation` objects. Hit frame metadata is retained from JSON.
- `LaboratoryScreen` coordinates input, movement and collision, active attack frames, monster AI, damage, renderers, HUD and map fade. `HeadquartersScreen` does not create or update monsters.
- Monsters use existing axis-separated `CollisionSystem` resolution through `CombatCollisionGrid`. Dead monsters are removed after the update pass.
- Laboratory monster placement is seeded and bounded. The default seed changes per game; tests can inject a fixed seed.
- Frame order: input/intent, player movement and run-start event, player attack hit frames, monster AI/movement/attack, damage and death cleanup, camera, world, combat bars/effects, HUD, transition overlay.

## 플레이어 HP HUD · 8칸 가방
- `inventory/Backpack`: 순수 8칸 수집 모델(collect/isFull/count/slot/handOver). 화면과 분리, 단위 테스트 가능.
- `world/PlayerSessionState`: 맵 간 공유 상태가 Backpack을 보유(collect/backpack/inventory 위임).
- `asset/HudLayout`: ui_layout.json(top-left 좌표) 파싱 순수 데이터. `asset/PlayerHudAssets`: 9개 UI PNG + 한글 가득참 라벨 로드(GameAssets가 소유·해제).
- `render/PlayerHudRenderer`: 화면 좌표계(월드 카메라와 분리) HP HUD + 가방 오버레이. 자체 SpriteBatch/카메라/폰트.
- 화면 연결: LaboratoryScreen/HeadquartersScreen이 HUD 렌더·R 토글·Esc 닫기·가방 열림 시 입력 차단. 기존 CombatRenderer의 임시 HP바는 제거.

## 캠페인 맵·roomkit·길찾기·loot (2026-10-12)

- `RoomKitAssets` parses `assets/laboratory_roomkits_v3/laboratory_roomkits_v3.json`, splits catalog cell sizes, and retains `_north/_left/_right` orientation without rotation. `LaboratoryProp` separates source-sized artwork from ground collision rectangles; `RoomVisuals` stores props and `LaboratoryRoom` includes their footprints in solid queries.
- `LaboratoryLayout.easyRoom()` builds the 128×96 easy map from sixteen fixed room plans with 9–24-tile outer widths, varied heights, three orthogonal L-shaped recesses, a cross spine, and shorter branch corridors. The plan supplies room bounds, floor cuts, door openings, furniture groups, collision footprints, minimap occupancy, and loot positions; the 16 room doors and single south return gate use the existing door resolver. Normal/hard still use their separate 28×20 and 38×28 layouts. `BalanceConfig.applyDifficulty()` owns monster counts. `HeadquartersScreen` submits the selected difficulty to `LaboratoryGame`, which uses the existing fade transition and shared session for both directions.
- `RoomPathfinder` runs four-way BFS across passable cells. `NavigationRenderer` draws the F route, fixed-screen minimap, and brief movement marks.
- `LootAssets` owns the supplied chest sheets and rarity atlases. `LootTableCatalog` parses the JSON probability rows; `LootContainer` plays once and emits one reward. The existing `Backpack` gained lowest-value slot replacement; the displaced item remains on the ground.
- `WorldRenderer` clips floor, overlay, wall, structure and prop draws to the camera bounds. Roomkit art uses measured alpha bounds for floor anchoring, while ground collision remains a separate footprint. Room-door visual IDs, jambs, and passability come from `DoorPlacementResolver` and the same `Door` state; the exterior headquarters gate is not counted as an internal room door.
- The standard laboratory `FitViewport` follows the player with a 20×15-tile view; the optional `LAB_MAP_OVERVIEW=1` development view fits all 128×96 map bounds and suppresses the minimap and HUD so the authored occupancy can be reviewed unobstructed. `LAB_START_ROOM=<room id>` is a desktop capture aid that spawns at that room's tested center. Exterior padding cells remain collision-solid but are not drawn as a tiled wall field.
- Easy-map furniture now references the supplied `laboratory_orthographic_views_v7` catalog directly through `v7:category/name/view` IDs. `LaboratoryFurnitureAssets` loads the catalog's front/left/right views and alpha bounds once; `WorldRenderer` resolves those explicit IDs before legacy roomkit IDs. Easy-map furniture has compact independent ground collision rectangles and is not silently substituted with V3 art.
- Manual desktop review at the full-map overview shows the 16-room/corridor topology and themed furniture distribution; a normal camera capture shows the sleeping room at game scale. Run-time door toggling, all-room route traversal, and each container tier's open/pickup/D swap still require the manual checks listed in `TEST_PLAN.md`.

## Compact easy-map rooms (2026-10-13)

`LaboratoryLayout.easyRoom()` keeps the 128×96 map and tile/asset scale, while room bounds are generated at 55% of their previous width/height (minimum 7×7 tiles) and room spacing is compressed to 70%. The 16 plans now range from 7×7 service rooms to 10×9 labs. Main circulation uses 2-tile lanes; branches narrow to 3 tiles after a short 5-tile doorway vestibule needed by the existing jamb resolver. Room, doorway, floor, wall, collision, minimap occupancy, furniture and loot placement remain derived from the same plans.

Wall-mounted V7 furniture is packed along north/south walls and both side walls where the door permits it. The small ground collision rectangles sit in the wall boundary cells; central work furniture keeps a separate walkable footprint. Placement rejects overlapping ground footprints. Loot containers are selected from free, walkable cells in their named themed rooms, away from the door and furniture.

## Furniture placement containment (2026-10-13)

`FurniturePlacementValidator` checks each V7 prop using the renderer's bottom-center anchor and the measured opaque alpha spans, not the source canvas or the smaller movement collider. Candidates must fit entirely on the owning room's actual `FLOOR` cells, including concave cuts; wall fixtures anchor their opaque bounds to the room-facing inner wall edge. Wall fixture collision footprints sit on their solid support wall tiles so tile-granularity pathfinding does not incorrectly close the adjacent aisle; floor furniture footprints remain on floor. Rejected wall candidates retry at smaller scales and advance their run cursor if no valid fit exists; central candidates are omitted when no valid floor placement exists. `LaboratoryLayout` validates all accepted props once generation completes.
# Easy laboratory wall packing update (2026-10-13)

- `LaboratoryLayout` now narrows the previously applied 3-tile branch corridors to 2 tiles; the main corridors remain 2 tiles because that is the traversable minimum at the current player scale. Door vestibules retain the 5-cell support span required by `DoorPlacementResolver`, then narrow to the 2-cell travel lane.
- Easy room wall furniture is placed before center islands on all four sides. Placement uses the alpha spans in `laboratory_orthographic_views_v7/render_metadata.json` (mirrored in the pure-Java `LaboratoryFurnitureCoverage` table) instead of the PNG canvas width. Front art is centered across its wall run; side views use their bottom anchor and grow upward.
- `WallFurnitureCoverageReport` exposes usable wall length and actual opaque furniture coverage by room and wall. Door openings, their two jamb cells, wall corners, and authored recesses are omitted from usable length. The layout test requires at least 85% coverage for every reported wall.
- Wall-mounted collision footprints stay within the boundary wall cell so increased furniture density does not close the adjacent walkable aisle. Center furniture remains generated after wall furniture and is checked against the same collision list.
## Laboratory wall and obstacle alignment (2026-10-13)

`WorldRenderer` resolves wall and structure IDs from the fixed-grid connected V3 atlases. `WallAutotiler` chooses ends, outer/inner corners, T-junctions, crosses, straight damaged variants, and sparse horizontal/vertical supports from logical cardinal neighbors; open exterior gate cells do not count as wall connectors. The visual validator checks visible neighboring connectors and intentionally ignores hidden backing perimeter cells.

`LaboratoryRoom` is the shared `ObstacleGrid` for player collision and path consumers. Tile solids represent only actual wall/closed-door cells; each furniture ground footprint remains a precise AABB and is checked by `CollisionSystem`, `RoomPathfinder`, spawn validation, monster movement/line checks, and attack line-of-sight. Opening a door changes its tile state in the same room model, so the next movement and route query see the updated passage. F1 draws green outlines for passable cells, red outlines for solid wall/door cells, cyan furniture ground footprints, and a lime player body. The laboratory render pass keeps framebuffer alpha opaque while blending translucent art.

Desktop review can start directly in the collision overlay with `LAB_DEBUG_COLLISION=1`; F1 remains the in-game toggle.

## 가구 방향 에셋(v8 뒷모습)
- `asset/LaboratoryFurnitureAssets`: v7(front/left/right) + **v8 `_back`**(남쪽 벽) 로드. back 경계는 v8 rearNotes대로 대응 front 경계를 재사용.
- `world/LaboratoryLayout.wallView`: 북=front·남=back·좌=left_side·우=right_side. `addWallProp`/`addV7` 충돌은 `LaboratoryFurnitureCoverage` 불투명 가로폭 × 바닥 footprint로 산출(하드코딩 상수 제거).
- 충돌 소비자(LaboratoryRoom/CombatCollisionGrid/RoomPathfinder/CollisionSystem)는 소스 rect만 바뀌면 그대로 전파(변경 불필요).

## 미니맵·귀환 발자국 (NavigationRenderer)
- `render/NavigationRenderer`: 월드 배치로 경로 발자국 PNG(방향 회전), 화면 배치로 미니맵 격자+프레임 PNG+마커·그 아래 귀환 버튼 PNG. `buttonHit(x,yTop)`로 클릭 판정 노출.
- 에셋: `assets/ui/navigation/`(minimap_frame_256·footprints_pair_64·return_route_button_256x64), GameAssets 소유·해제.
- `screen/LaboratoryScreen`: F 키 또는 버튼 클릭으로 routeEnabled 토글 → RoomPathfinder로 대문(남쪽 출구)까지 경로 산출 → NavigationRenderer가 발자국으로 표시.

## 외벽·대문 V3 (ExteriorWallRenderer)
- `asset/ExteriorWallAssets`: exterior_walls_gate_v3 외벽 8종 + 대문 2종 + layout.json(두께/8×4/anchor/passageRect). GameAssets 소유·해제.
- `render/ExteriorWallRenderer`: 월드 좌표로 2타일 링(직선 신축+모서리)과 남쪽 8×4 대문(열림/닫힘)을 그린다. 남쪽 벽은 대문 폭만큼 분리.
- `world/LaboratoryLayout.easyRoom`: 외곽 2링 autotile 스킨 null(새 외벽이 대체), 대문 개구부 2행, setGatePassage(기본 닫힘), hasExteriorRing()=true.
- `world/LaboratoryRoom`: gatePassage 사각형 + gateClosed 상태, blocksActorAt가 닫힘 시 통로 차단. setGateClosed/gateClosed.
- `screen/LaboratoryScreen`: easy 맵에서 ExteriorWallRenderer 호출, SPACE로 대문 토글(nearGate), 기존 crossedSouthExit→HEADQUARTERS 전환 유지.

## 내부 벽 직선 교체 (InteriorWallRenderer)
- `render/InteriorWallRenderer`: 내부 벽의 직선 벽면을 새 외벽 직선 스프라이트로 덧그린다. 벽 덩어리에서 파낸 구조라 방 경계벽은 T_OPEN_*(한쪽 바닥)로 분류되므로 T_OPEN_*+HORIZONTAL/VERTICAL을 직선으로 보고 바닥 쪽 면(north/south/west/east)을 고른다. 구간(run) 단위로 신축, 두께 1타일.
- 접합부(OUTER/INNER/CROSS/END)·세로 방문은 기존 v3 유지. 외곽 2타일 링은 ExteriorWallRenderer가 담당.
- `render/WorldRenderer.setInteriorWalls(renderer, room)`: WALL 레이어 직후(가구/플레이어 전) 같은 batch로 그림. `screen/LaboratoryScreen`이 easy 맵일 때 설정.

## profile 패키지 (2026-10-20)

영속 프로필과 본부 경제. 전부 순수 Java로 화면 없이 테스트되며, 파일 I/O는
`ProfileStore` 인터페이스 뒤의 `FileProfileStore` 어댑터로 분리된다.

- `PlayerProfile`/`ItemInstance`: 재화(long 원)·보관함 인스턴스·강화(ID→레벨)·귀환/거래 처리 기록.
- `ProfileCodec`: JSON 직렬화 + 버전 마이그레이션(누락 필드 기본값, 알 수 없는 필드/강화/아이템 보존).
- `FileProfileStore`: 임시 파일+fsync → 백업 → 원자 교체. 손상 시 백업 복구, 둘 다 손상 시 *.corrupt 보존.
- `HeadquartersEconomy`: 이관/판매/강화 기록 트랜잭션 — "다음 프로필 작성 → 저장 성공 → 메모리 교체"
  순서와 귀환/거래 ID 중복 차단을 책임진다. 가격은 loot_tables.json 카탈로그가 유일 기준.
- `SaleSelection`: 판매창 선택 상태(수량 0..보유 제한, long 합계). 렌더러(`SalePanelRenderer`)는
  계산 결과를 그리고 클릭을 전달만 한다. 지갑은 프로필 하나로 공유(화면별 재화 없음).

## 영구 강화 트리 (2026-10-20)

- `profile/UpgradeCatalog`: JSON 카탈로그 파싱+그래프 검증(중복/순환/음수가/미지 효과). 수치의 유일 출처.
- `profile/UpgradeEffects`: 구매 노드 집합 → 파생 능력치(HP/공격 보너스·해금 스킬·회복 보너스·슬롯).
  상태 없는 순수 계산이라 멱등. 알 수 없는 저장 노드는 보존 목록으로 분리.
- `HeadquartersEconomy.purchaseUpgrade`: 판매와 동일한 거래 경계(검증 → 다음 프로필 → 저장 성공 → 교체).
  해금 시 빈 슬롯 자동 장착. 프로필에 equippedSkills·upgradeCatalogVersion 추가(코덱 마이그레이션 포함).
- `LaboratoryGame.applyUpgradeEffects`: 기준값(baseMaxHp/baseAttackDamage) + 효과 합을 config와
  Player.setMaxHp에 반영 — 모든 시스템(전투·돌진·정수기·HUD)이 같은 config/Player 값을 읽는다.
- UI: `UpgradeTreeRenderer`(1120×720 패널, 노드 상태·직각 연결선·상세/구매), 패널은 별도 UI 좌표.
  공용 버튼·재화 텍스처는 판매 팩(HqSalesAssets) 공유. 2026-10-20 개편: 강화 트리 상호작용 대상은
  접이식 작업대(folding_workbench)이고, 북벽 금속 테이블은 `HeadquartersProp.skillTable`(id
  "skill_table", 같은 192×128 아트)로 전환되어 SPACE로 스킬창을 연다(침대 제거). 전면 접근 판정은
  HeadquartersRoom의 공통 canInteractWithFrontProp/isNearFrontProp.
- ~~회복 스킬 런타임은 LaboratoryScreen(Q 입력·쿨다운·HealEffectRenderer)이 담당~~ → 2026-10-20
  공통 스킬 시스템으로 이관(아래 절).

## 공통 스킬 시스템 (2026-10-20)

소유(해금)·장착·키 설정·쿨다운을 분리한 공통 모델. 전부 순수 Java(profile/)로 화면 없이 테스트된다.

- `profile/SkillLoadout`: slotId→skillId / slotId→keyCode 구성(5칸 고정 표시, 용량은 별도 계산).
  `fromProfile` 로드 시 마이그레이션(최초 기본값 F=돌진·Q=회복(해금 시), 구 equippedSkills 이관)과
  정리(미지 스킬·중복 장착·중복/예약 키 → 기본 키 복구+사유 로그, 해금 기록 보존)를 수행.
  equip/unequip/setKey는 실패 이유 문자열 반환+상태 불변.
- `profile/SkillRuntime`: skillId별 남은 쿨다운(초, delta 기반 — 절대시각 불사용). 게임 루트가 1개를
  소유해 슬롯 교체·키 변경·맵 전환·화면 재생성에도 유지. 저장 이벤트 시점 snapshot()을 프로필
  `skillCooldowns`에 싣고 로드 시 restore(오프라인 경과 미적용). tick은 플레이 가능한 시간에만 호출.
- `profile/SkillService.tryExecute`: 입력→장착/해금→생존·입력 허용→쿨다운→효과별 검증(회복=만피 거부,
  실제 회복량 = min(회복량, maxHp−HP))→Context.applyEffect 성공 시에만 쿨다운 시작. 실패는 Result
  열거형(이유)만 반환. Context 인터페이스로 화면 상태·효과 적용을 주입해 모델이 GL에 의존하지 않는다.
- 돌진: `combat/PlayerDashState`에서 쿨다운을 **제거**(이동·충돌·대상당 1회 타격 전용). 발동·쿨다운은
  SkillService+SkillRuntime("dash") 한 곳 — 이중 관리 금지. 이동/피해 규칙은 기존 그대로 화면이 수행.
- 입력: `GameAction`에서 DASH/SKILL_1 제거(슬롯 키는 SkillLoadout에서 직접 폴링, 엣지 1회),
  RETURN_ROUTE(H) 재추가. `KeyBindings.reservedKeys()`가 스킬 키 등록 금지 집합의 유일 출처.
- 저장: `HeadquartersEconomy.saveSkillLoadout`(판매와 같은 거래 경계) + persist 시 쿨다운 스냅샷 주입
  (`setCooldownSnapshotProvider`). 변경 성공 시에만 저장, 프레임 단위 저장 없음.
- UI/VFX(렌더러는 그리기·클릭 전달만): `SkillBarRenderer`(하단 중앙 퀵슬롯 5칸 — 실제 키 이름/미등록·
  쿨다운 가림+올림 초·잠김 구분), `SkillLoadoutRenderer`(1120×720 스킬창 — 장착/해제/키 등록 대기·Esc
  취소, 성공 시에만 onSave), `HealCastRenderer`(heal_cast_sheet 12프레임 1회·발 앵커·알파 0.7, 시각 전용),
  `SkillMenuButtonRenderer`(열기 버튼), `CombatHudRenderer`는 공격 전용으로 축소. 텍스처는
  `SkillAssets`(GameAssets 소유, Nearest).

## 보스 모드 1-1 (2026-10-20)

일반 연구소와 분리된 모드(MapId.BOSS_ARENA). 전투 전이는 순수 Java 상태 기계 한 곳에서.

- `boss/BossStageCatalog`(stages.json — progressUnlocked/implemented/cleared 구분),
  `boss/BossDefinition`(boss_1_1.json — 수치/패턴/시간 유일 출처, 저항 선언),
  `boss/BossFight`(상태 기계+패턴 3종+투사체/장판/돌진 판정 — Context 주입으로 GL 비의존,
  시뮬레이션 시계 경계 1회, 사망/실패 시 전체 취소, 동시 사망 실패 우선).
- 프로필: bossUnlockedStages/bossClearedStages/bossRewardReceipts("stageId#runId") +
  `HeadquartersEconomy.commitBossClear` — 클리어 기록+다음 단계 해금+보상 인스턴스+영수증을
  1회 원자 저장(실패 롤백·재시도, 같은 runId 멱등). 보상 선택은 runId 파생 시드로
  LootTableCatalog의 LEGENDARY 테이블에서 — 새 파일에 이름/가격 복사 없음.
- `screen/BossArenaScreen`: 32×24 광장(반복 바닥 타일+V4 벽, 기둥/방벽 footprint 장애물,
  남쪽 겸용 문=기존 gatePassage/southExitGate 재사용). 플레이어 공격/돌진/충격파/전기는
  보스 hurtbox 프록시(Monster 1개 — HealthComponent를 BossFight와 공유)로 기존 공통 경로를
  그대로 쓴다(AI를 돌리지 않아 넉백/경직 자연 면역, 감전은 VFX 표시만). 플레이어 피해는
  PlayerCombatState 공통 파이프라인 1회. runId/attemptSeed는 LaboratoryGame이 입장 확정/
  재도전에서만 발급(선택창 열람·화면 재생성으로는 미발급), 더블클릭은 전환 잠금+패널 잠금.
- UI: BossHudRenderer(상단 중앙 HP바 — 이름/수치 런타임), BossStageSelectRenderer(메인 컴퓨터
  SPACE, 1120×720), BossResultRenderer(보상/실패·재도전/복귀·저장 재시도). 복귀는 HQ 도착
  분기에서 보스 출신이면 가방 이관/루트 초기화를 건너뛴다(영구 자원 유지, 임시 엔티티 정리).

## 9단계 보스전 (2026-10-21)

데이터 주도 확장 — 단계별 Screen 복사본/거대 stageId switch 없음. 1-1은 위 레거시 경로를
회귀 기준으로 그대로 쓰고(BossFight·32×24·기존 아트, 표기 이름만 교체), 1-2 이후만 신규 엔진.

- 데이터(유일 출처): `assets/boss_nine_stages_v1/data/stage_catalog.runtime.json`(9단계 —
  bossId/displayName/legacyEngine/draftMaxHp/그리기·피격·footprint 분리/patternIds/arenaId/초상),
  `patterns.runtime.json`(17 패턴 — kind·수치·차폐/관통/틱 그룹 정책 선언),
  `arena_layouts.runtime.json`(8 투기장). draft 파일은 로드하지 않는다.
- `boss/BossStageCatalog`(구·신 스키마 겸용), `boss/BossPatternCatalog`, `boss/BossArenaCatalog`,
  `boss/BossBattle`(범용 패턴 엔진, 순수 Java — Context 주입: blockedAt/sightBlocked/validFloor/
  applySlow/applyRoot). kind별 실행기(projectile/fan/cone/ground/bind/trail/beam/annulus/
  burst/moving_burst/charge/repeat_charge/phase_change/sequence), 해저드(투사체 swept·장판
  그룹 틱 게이트=스케줄·지연 폭발·레이저 trim·확장 고리)는 BossBattle이 소유하고 렌더러는
  읽어서만 그린다(피해/보상 실행 금지). 분노 전환은 HP 50% 1회 큐 삽입, sequence는 하위 큐.
- `screen/BossArenaScreen` 분기 1곳: legacy(BossFight) vs nine(BossBattle) — 통일 접근자
  (phase/bossX/bossY/patternStage/markCleared/beginReturn)로 보상·결과창·귀환 로직 공유.
  투기장은 BossArenaCatalog 논리 배치로 조립(기존 바닥/벽/gatePassage 재사용, 기둥 footprint
  solid). 프록시 Monster 피격 박스=카탈로그 값(중심=발+1.6), 이동 충돌=footprint 반경.
  시트는 1024²=4×4 of 256(발 비율 32/256), 세그먼트 idle0-3/예고4-5/발사6-7/회복8-9/
  이동10-11/사망12-15(마지막 유지) — 몸체 회전 금지. 상태 이상은 화면 소유(둔화=이동 dt 배율,
  구속=이동만 차단).
- `asset/BossNineAssets`(GameAssets 소유): 보스 시트 8종, VFX 640×160 스트립 12종(4프레임),
  초상 지연 로드 캐시, 선택창 카드/배지. `render/BossStageSelectRenderer`는 3×3 실루엣 카드
  (잠김에도 표시, contain-fit)+방향키/Enter+컬러 초상 상세로 개편.
- 플레이어 스킬 수치는 변경 금지 계약 — `PlayerSkillRangeBaselineTest`가 기준값을 고정.
  해금 집합은 `LaboratoryGame.bossUnlockedView()`(디버그 LAB_BOSS_UNLOCK_ALL=1은 입장만 허용,
  저장 진행과 분리).

## 난이도별 시드 기반 연구소 생성 (2026-10-20)

생성기는 데이터(논리 타일·가구·컨테이너·몬스터 배치)를 만들고 렌더러는 읽어서만 그린다 —
프레임마다 난수 재선택 없음, Texture 소유는 GameAssets 그대로. 충돌·길찾기·미니맵은 같은
최종 논리 스냅샷(LaboratoryRoom)을 쓴다.

- `world/GenRng`: FNV-1a32(seed십진문자열+"|"+scope)+XorShift32(13,17,5) — 팩 참조 파이썬과
  golden vector 일치. scope로 topology/shape/notch/themes/furniture/loot/monsters/visual
  하위 스트림 분리(바닥 장식 변형이 몬스터 배치를 밀지 않는다).
- `world/LabGenData`: 구조 틀 12종(data/layouts)·난이도 프로필·테마 16종 파싱. 파일 I/O는
  Source 인터페이스 주입(게임=Gdx.files, 테스트=java.nio). 동봉 몬스터/캐비넷 수는 초안 —
  실제 쉬움 기준(BalanceConfig)이 우선한다.
- `world/LaboratoryGenerator`: 방 배치(격자+크기 변형+모서리 후퇴 — 문 앵커 보호) →
  spanning tree+추가 연결 → 도그레그 복도+문별 5칸 전실(DoorPlacementResolver 지지 규칙) →
  남쪽 대문(외벽 링+V3 대문, 모든 난이도) → 테마 16종 배정 → 가구(기존 쉬움 kit 파이프라인
  `addThemedRoomFurniture` 공유, 입구 방 중앙 비움, 섬 변형 3종) → 캐비넷(기존 placeContainer
  가중 추첨) → 접근성 BFS(배우 몸체·4방향) + 실패 슬롯 수리 → 몬스터(예산=쉬움 56×방 수 비례,
  팀장=구석 후보만) → 메타데이터(contentHash·통계). 실패는 attempt 하위 시드 3회 → 난이도별
  검증된 폴백(쉬움=수제 easyRoom, 보통/어려움=golden seed compact) + 기록.
- LaboratoryLayout: 수제 쉬움은 고정 회귀 기준+폴백으로 보존. kit/placeContainer/RoomPlan을
  패키지 공개로 생성기와 공유(난이도별 구식 렌더러 분기 없음 — 전 난이도 V4 경로).
- LaboratoryGame: 난이도 확정(selectDifficulty)에서만 `generateExpedition` — 시드 발급은
  비결정(새 탐사), 생성은 결정적. 몬스터는 생성 플랜에서 소환(수제 폴백만 MonsterSpawnSystem).
  디버그: LAB_SEED(시드 고정)/LAB_CLASSIC_MAP=1(수제 맵)/입장 로그에 재현 메타데이터.
- 진행 중 탐사 저장은 미지원(범위 명시) — 같은 세션에서는 laboratory 스냅샷이 유지되어 화면
  재생성으로 맵/열린 상자 상태가 초기화되지 않고, 앱 종료 시 탐사는 소멸한다(본부 프로필 보존).

## 추가 스킬 3종 — 충격파·전기 투척·보호막 (2026-10-20)

공통 스킬 파이프라인 확장(화면 키 분기 없음). 판정 모델은 전부 순수 Java(combat/)로 GL 없이
테스트되고, 화면은 결과를 공용 타격 경로·VFX에 연결만 한다.

- `profile/SkillCatalog`: skills.json(동봉 초안 채택) 파싱 — 쿨다운/피해/반경/radiusCurve/이벤트
  시각. `shockwaveFrontRadius(elapsed)` 보간이 판정·렌더 공용 파면 함수(한 정의에서 파생).
- `combat/SkillCast`: 시전 준비 타이머 — 경계 "이전<t≤현재", t=0은 시작 시 1회, cancel()로 미발생
  효과만 취소(재시전 = 새 인스턴스 = 새 castId).
- `combat/ShockwaveState`: 원점 고정, 피해 창(1/15~5/15초) 동안 "직전→현재 파면 고리(annulus)"와
  hurtbox 교차를 검사해 시전·대상별 1회 Hit 반환(원점 위 대상 포함, 큰 delta 커버). 차폐는
  Occlusion 인터페이스 주입(화면이 room.blocksActorAlongPath로 구현 — 벽·닫힌 문·solid 가구).
- `combat/ElectricProjectile`: 방향 고정, swept 샘플(간격 ≤ 충돌 반경) 이동 — 같은 구간에서 벽을
  먼저 검사(벽이 몬스터보다 앞이면 무피해), 1충돌 소멸, 사거리 만료. Solid 인터페이스 주입.
- 감전: `Monster.applyShock(max 갱신)` + `MonsterAiSystem`이 남은 시간 동안 자율 이동·새 공격
  시작 차단(미확정 공격만 취소 — attackHit 전), 넉백 물리는 유지, 종료 시 자연 복귀.
- `combat/PlayerShieldState` + `PlayerCombatState.setShield`: 흡수는 공통 피해 파이프라인
  takeDamage 한 곳(무적 검증 → 흡수 → 잔여만 HP). 준비/활성/소진(파괴)/만료(페이드) 상태 구분,
  큰 delta에서 활성→만료를 시간순 처리(프레임 선두 update → 만료 우선), 활성 중 재시전 거부.
- `CombatSystem.applyPlayerSkillStrike(damage, knockbackDistance)`: 일반 공격과 같은 HIT/DEAD·
  처치 회복·타격 알림 경로에 피해·넉백 거리만 스킬 값 — 기본 공격 강화와 중복 가산 없음.
- 강화: UpgradeCatalog에 SKILL_DAMAGE_ADD/SHIELD_ABSORB_ADD 추가, UpgradeEffects가
  skillId별 피해 보너스·흡수 보너스 집계. LaboratoryGame.skillDamage/shieldAbsorbAmount/
  skillCooldownSeconds가 파생 수치의 단일 접점.
- VFX: `ThreeSkillsAssets`(시트 6종+아이콘 3종, GameAssets 소유) + `SkillVfxRenderer` —
  충격파는 엔티티 아래, 피격/착탄/투사체(+X 기준 회전)/보호막(발 앵커 64,102·알파 0.65)/감전
  오버레이는 엔티티 위 레이어. 충격파 draw scale은 visualExtent(46.8px/128셀)로 논리 반경과 일치.
  피격/착탄 연출은 실제 명중 시에만 생성. 활성 효과는 화면 수명(맵 이탈 시 제거), 쿨다운은
  게임 루트 SkillRuntime 유지.

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
core/.../io/github/some_example_name/
├── LaboratoryGame.java              # GameAssets/세션/맵 전환 소유, 본부에서 시작
├── asset/
│   ├── GameAssets.java              # 모든 화면 자산의 공유 로딩·해제 소유자
│   ├── HeadquartersAssets.java      # 본부 에셋 및 상호작용 프롬프트
│   ├── PlayerHudAssets.java         # HP/가방 HUD 이미지
│   └── HudLayout.java               # HUD 배치 설정 파싱
├── config/
│   ├── AssetPaths.java              # 자산 경로 상수
│   └── BalanceConfig.java           # 전투·이동·전환 수치
├── input/
│   ├── InputState.java / GdxPlayerInput.java
│   ├── KeyBindings.java / GameAction.java
│   └── PlayerController.java / PlayerIntent.java
├── movement/, collision/            # delta 이동, AABB 충돌, 연구소·본부 장애물
├── world/
│   ├── LaboratoryLayout/Room/Visuals # 연구소 논리 맵, 충돌, 타일 레이어
│   ├── WallAutotiler.java / Door*   # 벽 연결과 문 방향·상태
│   ├── HeadquartersLayout/Room/Visuals # 14×10 본부 배치·충돌·상호작용 위치
│   ├── PlayerSessionState.java      # 맵 간 공유 플레이어·가방·재화·진행
│   └── MapId.java / MapTransitionController.java
├── entity/
│   ├── player/Player.java           # 플레이어 상태·HP·위치
│   └── monster/Monster.java         # 몬스터 상태와 체력
├── combat/
│   ├── CombatSystem.java / MonsterAiSystem.java
│   ├── MonsterSpawnSystem.java / PlayerCombatState.java
│   └── HealthComponent.java / AttackHitbox.java
├── inventory/Backpack.java         # 순수 Java 8칸 가방 모델
├── interaction/VendingMachineInteraction.java # 엣지 입력·짧은 프롬프트 피드백
├── render/
│   ├── WorldRenderer.java / HeadquartersRenderer.java
│   ├── EntityRenderer.java / MonsterRenderer.java
│   ├── CombatRenderer.java / PlayerHudRenderer.java
│   └── DebugRenderer.java / TileDebugRenderer.java
└── screen/
    ├── BaseScreen.java
    ├── HeadquartersScreen.java      # 고정 카메라, 본부 입력·상호작용·HUD
    └── LaboratoryScreen.java        # 추적 카메라, 이동·전투·AI·HUD

lwjgl3/.../lwjgl3/
├── Lwjgl3Launcher.java              # new LaboratoryGame() 진입
└── StartupHelper.java               # liftoff 제공
```

아직 구현하지 않은 주요 영역은 실제 보물 획득·반납·보상 경제, 자판기 구매 UI, 저장/불러오기,
게임오버·부활 흐름이다. 화면별 HUD는 구현됐으며 별도 로딩 화면은 없다.

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

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

## 현재 존재하는 클래스 (1단계까지)
```
core/.../io/github/some_example_name/
├── LaboratoryGame.java              # extends Game. GameAssets 소유, LaboratoryScreen 진입
├── asset/
│   └── GameAssets.java              # 텍스처/애니메이션 로딩·해제(소유자). JSON 파싱
├── config/
│   ├── AssetPaths.java              # 자산 경로 상수
│   └── BalanceConfig.java           # 수치(주입 가능한 값 객체)
├── input/
│   ├── GameAction.java              # 추상 입력 enum
│   ├── KeyBindings.java             # 키 ↔ GameAction
│   ├── InputState.java              # 엣지(pressed/held/released) — 순수
│   ├── PlayerIntent.java            # 한 프레임 의도 — 순수
│   ├── PlayerController.java        # InputState → PlayerIntent — 순수
│   └── GdxPlayerInput.java          # Gdx.input 폴링(LibGDX 의존)
├── movement/
│   ├── MovementMode.java            # WALKING / RUNNING
│   ├── RunStartDetector.java        # 달리기 시작 1회 판정 — 순수
│   └── MovementSystem.java          # 입력→속도→충돌 적용 — 순수(수학/충돌)
├── collision/
│   ├── CollisionLayer.java          # 레이어 enum(1단계는 PLAYER vs WALL만 사용)
│   ├── SolidGrid.java               # 타일 고체 질의 인터페이스
│   ├── CollisionResult.java         # 해결 결과(위치 + 축 충돌 여부)
│   └── CollisionSystem.java         # AABB 대 타일 축별 해결 — 순수
├── world/
│   ├── TileType.java                # 논리 타일: 충돌(solid) + 위험(hazard) 의미만
│   ├── TileVisual.java              # 시각 타일 ↔ 타일셋 인덱스(0~15) 매핑 한곳
│   ├── RoomVisuals.java             # 셀별 시각 타일 배치(1회 계산, 실행 중 불변)
│   ├── WallAutotiler.java           # 이웃 검사로 벽/모서리 시각 선택(순수)
│   ├── LaboratoryLayout.java        # 테스트 방 설계(4구역 문자맵 → 논리+시각 빌드, 순수)
│   └── LaboratoryRoom.java          # 논리/충돌 모델(SolidGrid) + isHazard
├── entity/
│   ├── Direction.java               # 4방향 + 입력→방향
│   └── player/Player.java           # 위치/HP/방향/이동상태(렌더 자원 미소유)
├── render/
│   ├── AnimationSet.java            # 방향별 걷기/공격 애니메이션 + idle 보관
│   ├── AnimationController.java     # 상태 읽어 현재 프레임 선택
│   ├── WorldRenderer.java           # 타일 렌더
│   ├── EntityRenderer.java          # 플레이어 렌더(발밑 앵커 1x1)
│   └── DebugRenderer.java           # 충돌 영역 디버그(ShapeRenderer, F1 토글)
└── screen/
    ├── BaseScreen.java              # 공통 베이스(게임/자산 참조)
    └── LaboratoryScreen.java        # 프레임 순서 조율, 카메라/뷰포트

lwjgl3/.../lwjgl3/
├── Lwjgl3Launcher.java              # new LaboratoryGame() 진입
└── StartupHelper.java               # liftoff 제공
```

> 아직 없는 패키지(이후 단계에서 추가): `ai`, `combat`, `interaction`, `ui`,
> `event`, `save`, `world/spawn`, `entity/monster`, `entity/treasure`,
> 그리고 `screen`의 Loading/Headquarters/GameOver.

## 배경 타일: 논리 / 시각 / 충돌 분리
배경은 세 가지 책임으로 나뉜다. 하나의 타입이 충돌·시각·배치를 모두 결정하지 않는다.
- **논리 타일**(`TileType`): 이동 가능/벽/문/위험 지역 의미. 충돌은 여기서 파생된다.
  - `FLOOR`(통과), `HAZARD`(통과·위험표시), `WALL`(고체), `DOOR_CLOSED`(고체), `DOORWAY_OPEN`(통과)
- **시각 타일**(`TileVisual`): 어떤 타일셋 이미지를 그릴지(인덱스 0~15). 숫자 인덱스는 이 enum 한곳에만.
- **충돌 정보**: `LaboratoryRoom.isSolid`가 `TileType.solid`로 판정(기존 1단계와 동일 결과).

배치/생성은 `LaboratoryLayout`이 담당한다. 고정 문자맵을 파싱해 (1) 논리 격자와
(2) 시각 격자를 만들고, 벽('#') 셀의 시각은 `WallAutotiler`가 이웃(상/하/좌/우, 필요 시 대각)을
검사해 방향별 벽/모서리로 선택한다. 벽 시각 방향이 바뀌어도 충돌은 동일한 논리 벽 셀을 쓴다.
랜덤이 없어 같은 데이터면 항상 같은 방이 재현된다. `RoomVisuals`는 1회 계산 후 불변이다.

> 균열/얼룩/경고/독성/전선 타일은 현재 **시각/의미** 표현만 한다. 독성·전선은 `hazard`로
> 데이터 구분(`isHazard`)하되 피해 로직은 없다(2단계 이후). 닫힌 문은 상호작용 없이 벽처럼 충돌한다.

## 좌표계 / 단위
- 월드 단위: **1 타일 = 1 단위**, y축은 위로 증가. 화면 픽셀과 혼용하지 않는다.
- 카메라 뷰포트: `FitViewport` 12×9 월드 단위. 플레이어 추적 + 방 경계 클램프.
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

## 프레임 업데이트 순서 (LaboratoryScreen, 1단계 범위)
1. delta 상한 클램프(`BalanceConfig.maxFrameDeltaSeconds`)
2. 입력 폴링 → InputState 갱신, PAUSE/F1 토글 처리
3. (일시정지가 아니면) PlayerIntent 생성 → MovementSystem: 이동 + 충돌 해결
4. 카메라 업데이트(추적 + 경계 클램프)
5. 월드 렌더 → 플레이어 렌더
6. (디버그 on이면) 충돌 영역 렌더

> AI/전투/상호작용/사망·보상/UI 단계는 아직 비어 있다(이후 단계에서 채운다).
> 일시정지 시 월드 업데이트와 애니메이션 시간이 진행되지 않는다.

## 화면 전환
- `Lwjgl3Launcher` → `LaboratoryGame.create()` → `LaboratoryScreen`(바로 진입).
- LoadingScreen / HeadquartersScreen / GameOverScreen 은 이후 단계에서 도입.

## 자산 생명주기
- `GameAssets`가 생성 시 타일셋/플레이어 시트 텍스처를 **직접** 로딩(Nearest 필터)하고
  플레이어 애니메이션 JSON을 파싱해 `AnimationSet`을 만든다.
- 타일셋은 **실제 픽셀 크기와 무관하게 4열×4행 격자로 분할**한다(`splitIntoGrid`). 제공 파일이
  1254×1254라 64px 고정 분할은 틀리며, 각 셀 경계를 실제 크기 기준으로 반올림해 빈틈 없이 나눈다.
  플레이어 시트는 512×512/8×8이라 64px로 정확히 나뉜다.
- `LaboratoryGame.dispose()`에서 화면 → `GameAssets` 순으로 한 번만 해제한다.
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

`LAB_WALL_TEST=1` renders a 9x9 atlas-shape gallery; `LAB_WALL_TEST=2` renders a 9x9 room with assembled horizontal and vertical doors. Production uses the 20x15 layout. Coordinates are world y-up (`north = y + 1`); JSON atlas rows start at the PNG top, and TextureRegion is not flipped.

`WallSeamAtlasGenerator` derives 64x64 RGBA connected atlases from the preserved V2 source art. Straight metal bodies reach the exact cell edge; end caps appear only on end or jamb tiles. Floor seam variants are excluded from random placement because their isolated rails looked like stray walls.

`DoorPlacementResolver` infers orientation from a five-cell straight run and reserves adjacent wall cells for jambs. `Door` owns fixed tile coordinates and orientation plus its state; `LaboratoryRoom` derives collision and `RoomVisuals` derives the current structure ID from the same object. `WallAutotiler` owns cardinal masks and visual connector metadata. `WallTopologyValidator` checks masks, jambs, collision and reciprocal visual connections once after layout creation. `WorldRenderer` still draws floor, overlay, wall, then structure at one cell anchor with no per-tile offsets.

## Independent headquarters map and transition (2026-10-07)

`LaboratoryGame` owns one `GameAssets`, `PlayerSessionState`, both layouts, and `MapTransitionController`. The default first screen is `HeadquartersScreen`; `LaboratoryScreen` receives the same player on transition. `MapId` distinguishes the maps. `HeadquartersLayout` builds a fixed 20×15 tent independently from `LaboratoryLayout`; `HeadquartersRoom` owns logical collision, prop foot rectangles, interaction locations, and the one-shot south exit. `HeadquartersVisuals` owns floor/decor/wall/prop IDs. No HQ monsters are created.

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

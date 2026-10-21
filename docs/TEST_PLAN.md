# TEST_PLAN

각 기능에 대해 조건(given) · 행동(when) · 예상 결과(then)로 기록한다.
계산·상태 전환은 LibGDX 화면 없이 순수 Java로 테스트한다. 시간은 주입 가능한 delta/GameTime,
랜덤은 seed/공급자 주입으로 재현 가능하게 한다.

## 현재 상태 (2026-10-20)
- core 테스트 **209개 통과** + `lwjgl3:build` 성공 (`./gradlew.bat core:test lwjgl3:build`).
- 주요 스위트: 생성기(LaboratoryGeneratorTest — 난이도별 100시드 전수), 스킬(SkillSystemTest·
  ThreeSkillsModelTest·PlayerDashStateTest), 경제/프로필(HeadquartersEconomyTest·
  ProfilePersistenceTest·UpgradeTreeTest), 몬스터(MonsterBehaviorTest), 맵(EasyLaboratoryLayoutTest·
  DifficultyLayoutTest·ReturnRouteRegressionTest·HeadquartersLayoutTest) 등 — 세부는 아래 기능별 절.
- 실행 검증은 FrameCapture 실프레임(docs/images/frames/)과 LabGen/Skill3 등 실행 로그로 남긴다.
  수동 체감(효과음 청취·키보드 실입력·보통/어려움 밸런스)은 사용자 핸즈온 권장.

## (이전 기준선: 2026-10-12)
- clean 검증 기준 core 테스트 107개 통과.
- 실제 화면에서 쉬움 맵/roomkit 렌더링, 미니맵·F 경로, 쉬움 연구소→본부 복귀→쉬움 재입장을 확인했다.
- 추가 테스트: `EasyLaboratoryLayoutTest`, `RoomPathfinderTest`, `DifficultyLayoutTest`, `LootTableCatalogTest`, `LootContainerTest`, `BackpackValueSwapTest`.
  - `movement/RunStartDetectorTest` (4): 달리기 시작 1회성/재시작 2회성.
  - `movement/MovementSystemTest` (3): 이동 거리=speed*delta(프레임 독립), 뛰기>걷기, 벽 비통과.
  - `input/InputStateTest` (2): pressed/held/released 엣지.
  - `world/LaboratoryRoomTest` (3): 테두리/범위밖 고체, 스폰 바닥, 크기.
  - `world/LaboratoryLayoutTest` (14): 아래 배경 다양화 및 4레이어 시나리오.
  - `world/LaboratoryTileCatalogV2Test` (8): ID/인덱스, PNG 크기·알파.
  - `world/WallAutotilerTest` (8): 직선·끝·모서리·T자·십자·문 연결.
- 전투·팀장 규칙 테스트는 `CombatSystemTest` 및 `RunStartDetectorTest`를 사용한다.

### 배경 다양화 (LaboratoryLayoutTest)
- given 깨끗한 경계 셀, then 상/하/좌/우가 북/남/서/동 벽 시각으로 선택된다.
- given 방 네 모서리, then 방향에 맞는 모서리 벽 시각(NW/NE/SW/SE)으로 선택된다.
- given 닫힌 문/열린 출입구, then 닫힌 문은 충돌하고 출입구는 통과 가능하다.
- given 균열·얼룩·경고·독성·전선 타일, then 모두 통과 가능하며 독성·전선만 hazard로 구분된다.
- given 완성된 테스트 방, then 타일 인덱스 0~15 가 모두 최소 한 번 사용된다.
- given 같은 맵 데이터로 두 번 빌드, then 논리·시각 배치가 동일하게 재현된다.
- given 스폰 지점, then 주변 3x3 가 모두 이동 가능하다.

## 우선순위 (규칙서 13장)
1. HP·피해 계산
2. 사망 처리와 중복 보상 방지
3. 한 공격이 같은 대상에 한 번만 명중
4. 걷기 중 팀장 무반응
5. 달리기 시작 1회가 한 번만 기록
6. 달리기 시작 2회 후 팀장 분노
7. 팀장 피격 시 즉시 분노
8. 본부 영역에서 보물 1회만 반납
9. 사망 시 이번 탐사 보상 제거
10. 반납 완료 보상은 다음 탐사에도 유지

## 예정 시나리오

### 이동/소음 (1단계)
- given 정지/걷기 상태, when RUN 입력으로 RUNNING 전환, then 달리기 시작 1회 기록.
- given RUNNING 유지, when RUN 키를 계속 누름, then 추가 기록 없음(카운트 불변).
- given 두 프레임의 서로 다른 delta, when 같은 시간만큼 이동, then 이동 거리 동일(speed*delta).

### 전투 (2단계)
- given HP20 슬라임, when 공격력15 2회 피격, then 사망(HP 0, 음수 아님).
- given 공격 1회 생성, when 같은 대상과 여러 프레임 겹침, then 피해 1회만 적용.
- given 사망한 대상, when 추가 공격, then 피해 없음.

### 팀장 (4단계)
- given 팀장 SEATED·플레이어 감지 범위 내, when 플레이어 걷기만, then SEATED 유지.
- given 첫 RunStartedEvent 수신, when RUN 키 계속 누름, then 감지 횟수 1에서 불변.
- given 첫 달리기 종료 후 재시작, when 두 번째 RunStartedEvent, then ENRAGED.
- given 팀장 SEATED, when 플레이어가 직접 공격, then 즉시 ENRAGED.

### 보물/보상 (3단계)
- given 보물 운반 중·반납 영역 내·새 입력, when 반납, then 1회만 성공(중복 반납 불가).
- given 플레이어 사망, when 이번 탐사 미반납 보물 존재, then 해당 보상 제거.
- given 이전 탐사에서 반납 완료, when 새 탐사 시작, then 보상 유지.

## Laboratory Tileset V2 테스트

에셋/카탈로그(`LaboratoryTileCatalogV2Test`, 순수 Java·GL 불필요):
- 아틀라스 크기(JSON 선언값 + 실제 PNG IHDR): floor 384×256, wall 256×256, structure 256×256, overlay 384×256
- 타일 수: floor 24 / wall 16 / structure 16 / overlay 24, 각 아틀라스 열×행 일치, 누락 ID 없음(격자 좌표 유효)
- ID↔인덱스/행열 매핑, 충돌(열린 문=통과, breach=조건부), 태그(toxic/shock/electric/decor)

맵(`LaboratoryLayoutTest`): 20×15, 스폰 통과·3×3 위험 없음, 외벽 충돌(출입구 제외), 문 방향·구조물 타일·문 미덮어쓰기,
정비 도달/오염 밀폐(BFS), 모든 셀(벽·문 포함)=바닥, 벽/구조물/오버레이 별도 레이어, 열린 문 void 없음,
오버레이 벽 위 금지, 바닥 3연속 금지, 경고선 경계, hazard(TOXIC/SHOCK) 존재, 재현성.

벽 오토타일(`WallAutotilerTest`): 직선·바깥 모서리·네 방향 끝단·네 방향 T자·네 방향 안쪽 모서리·십자,
문 미덮어쓰기, 직선 파손 변형 약 10~15%와 콘솔 미사용.

PNG 알파(`LaboratoryTileCatalogV2Test`): 벽/구조물/오버레이의 알파 채널·좌상단 alpha=0·투명/불투명 픽셀,
불투명 바닥 아틀라스 전체 픽셀 검사(ImageIO, GL 불필요).

회귀: 이동/달리기 시작/충돌/카메라/애니메이션 로딩 테스트 그대로 통과.
검증 명령: `./gradlew.bat clean core:test lwjgl3:build` 후 `./gradlew.bat lwjgl3:run`.
2026-10-07 기준 core 테스트 42개 통과. 실제 창 캡처는 `docs/images/map_black_voids_fixed*.png`.

## Connected wall regression (2026-10-07)

- `WallAutotilerTest`: five-cell straight runs, four end directions, outer and inner corners, four T orientations, cross, isolated wall rejection, and sparse damaged variants.
- `DoorPlacementTest`: orientation inferred from neighboring walls, five-cell run and jamb reservation, invalid corner/junction/adjacent door rejection, fixed anchor through state transitions, collision and visual synchronization, 9x9 assembly layout.
- `AssetConnectionTest`: ImageIO checks actual opaque metal body edge profiles for every wall/door/jamb connector, open aperture and threshold.
- Run targeted tests, then `./gradlew.bat core:test`, then `./gradlew.bat clean core:test lwjgl3:build`. Launch the normal map and both `LAB_WALL_TEST` modes, capture the actual window, and inspect F2 connector markers plus open/closed movement.

## 임시 본부 천막과 전환 (2026-10-07)

- `HeadquartersLayoutTest`: 독립 20×15 맵, 남쪽 열린 출구 1칸, 외벽·가구 지면 충돌, 안전한 본부 스폰과 연구소 도착, 달리기 이동의 1회 출구 이벤트, 트리거 대기·북쪽 이동 거부, 자판기 앞 범위, 반복 생성 결과 일치.
- `MapTransitionControllerTest`: 페이드아웃 이전 맵 유지, 중복 요청 거부와 1회 교체, 페이드인 후 입력 해제, delta 분할 동작.
- `PlayerSessionStateTest`: 맵 교체 전후 플레이어 HP·금액·인벤토리·진행 플래그 유지.
- `HeadquartersAssetTest`: 원본 JSON 원점·앵커·프레임 시간과 개별 PNG 크기, 가구·자판기 알파 확인. 공급된 천막 타일은 불투명 RGB이다.
- 실행: `./gradlew.bat clean core:test lwjgl3:build` 뒤 `./gradlew.bat lwjgl3:run`. 본부 남쪽/북쪽/자판기/F1을 보고 Shift+S로 출구를 넘어 페이드 및 연구소 안전 도착을 확인한다. 캡처는 `docs/images/hq_*.png`.
## First combat pass

- `combat/CombatSystemTest`: configured HP/damage and hit counts, directional hitboxes, hit-frame gating, one hit per target per swing, deterministic safe spawning, leader hearing count, and enemy telegraph damage.
- Run `./gradlew.bat core:test` then `./gradlew.bat clean core:test lwjgl3:build`.
- Manual desktop check: start in HQ and verify no monsters there; enter the lab and inspect all four monsters, attacks, damage, HP bars, death removal, and two nearby run starts for leader enrage. Pause/transition should stop combat updates.
- 2026-10-07 verification: `./gradlew.bat clean core:test lwjgl3:build` passed (71 tests); `./gradlew.bat lwjgl3:run` started the desktop window and loaded the laboratory atlases. Full manual combat and death walkthrough remains pending.
# Player death animation checks (2026-10-08)

- Unit coverage: lethal damage clamps HP and enters DEAD once; directional clip selection follows facing; delta advances frames 0-7 and holds 7; zero delta pauses; dead movement/run noise/attack are blocked; active attack state is cleared; later damage is ignored.
- Asset coverage: death sheet dimensions, alpha transparency, JSON rows/directions, 8 frames at 0.1 seconds, and non-looping metadata.
- Manual check: reduce laboratory player HP to zero and inspect all four directions, final-frame hold, HP HUD, and input lock in the running desktop game.

# Compact headquarters and vending prompt checks (2026-10-08)

- Unit coverage: 14x10 bounds, south opening, centered terminal, one vending machine, clear front approach, facing requirement, and dead-player rejection.
- Asset coverage: the SPACE prompt is a transparent 384x64 sheet split into idle/interactable/pressed 128x64 frames using `spacebar_prompt.json`.
- Interaction coverage: out-of-range input is ignored, an eligible just-pressed SPACE emits one event, feedback expires, and the prompt hides when out of range.
- Manual check: inspect the entire map at wide and narrow window ratios; walk to the vending machine, face it, press SPACE, and verify the prompt/frame response and continued south exit.
- 2026-10-08 verification: `./gradlew.bat clean core:test lwjgl3:build` passed (87 tests). `./gradlew.bat lwjgl3:run` loaded the map; full-map captures at standard, wide, and tall ratios show no crop. The vending approach displays the compact Korean prompt above the keycap; a SPACE edge emitted one event and showed the pressed/accepted frames (`docs/images/hq_compact_interactable.png`, `hq_compact_wide.png`, `hq_compact_tall.png`, `hq_vending_pressed.png`).
- 2026-10-08 prompt refinement: `./gradlew.bat core:test lwjgl3:build` passed; the 12px label is horizontally centered directly above the SPACE keycap with a small visible gap (`docs/images/hq_prompt_refined.png`).
- Water dispenser coverage: the prompt appears within 2.5 tiles regardless of facing; interaction requires a clear approach within 2 tiles and fully heals without reviving a dead player. `LAB_HQ_TEST=WATER` starts a damaged player by the dispenser; the running view confirms the prompt is centered above it (`docs/images/hq_water_prompt.png`).
- 2026-10-08 proximity update: `./gradlew.bat core:test lwjgl3:build` passed (89 tests, 0 failures). Both prompts use 2.5-tile visibility independent of facing; the water SPACE keycap is centered on the dispenser.

## HP HUD · 8칸 가방 테스트
- `inventory.BackpackTest`: 8개 수집→9번째 거부(개수 불변·물품 보존), 슬롯 순서, handOver 비우기·재사용, 빈 ID 거부.
- `asset.HudLayoutTest`: ui_layout.json 파싱(maxHp 100, fill 240x18@(134,57), 임계 50/25, 8 슬롯 좌표, 패널 960x600, dim 0.55).
- 수동(실행 창): HP 100/50/25/0 채움·색·숫자, R 토글, Esc 닫기, G로 8개 수집·9번째 거부·가득참 알림, 본부·연구소 HUD 유지.

## 쉬움 연구소 다중 방·문·loot (2026-10-12)

- `EasyLaboratoryLayoutTest`: 128×96, 소/중/대 크기의 16개 방, 3개의 ㄱ자 홈, 각 방 중심 및 모든 통행 칸의 BFS 도달, 남쪽 귀환 게이트, 16개 문·문틀·열림/닫힘 충돌 동기화, V7 가구 ID와 세 등급 컨테이너를 검증한다.
- `LAB_MAP_OVERVIEW=1` 실제 실행 캡처: `docs/images/laboratory_irregular_overview.png`. 표준 20×15 화면 캡처는 CHEMISTRY, BIOLOGY, RESTROOM, SLEEPING, INFORMATION, LIBRARY 시작 좌표에서 저장했다. 좌표는 개발용 `LAB_START_ROOM`으로 선택하며 일반 플레이 기본 스폰은 유지한다.
- 수동 입력으로 실제 이동하며 문을 열고 컨테이너를 여는 확인은 이번 캡처 절차에서 자동화하지 않았다. 연결 경로는 타일 및 가구 충돌을 포함한 BFS 테스트로 확인하고, 컨테이너 단회 개봉/아이템 추첨은 기존 도메인 테스트로 확인한다.
- `LootContainerTest`: 상자는 한 번만 열리고 애니메이션 중 한 번만 보상을 배출한다. `LootTableCatalogTest`: 제공 JSON을 파싱하고 각 컨테이너의 고가 아이템 확률이 저가 아이템보다 낮은지 검사한다.
- `BackpackValueSwapTest`: 8칸 가방에서 더 높은 가치만 최저 슬롯을 교체하며, 같거나 낮은 가격은 그대로 둔다.
- 실제 실행 캡처: 전체 지도 `docs/images/laboratory_easy_overview.png`, 일반 카메라 방 `docs/images/laboratory_easy_play.png`. 수동 확인은 세 방 이동, 벽 문 E 개폐/통과, 세 등급 상자 열기/줍기, 가득 찬 가방 D 교체를 포함한다.

## 축소 연구소 레이아웃 (2026-10-13)

- `EasyLaboratoryLayoutTest`: 16개 방의 7×7~10×9 경계, 모든 통행 셀의 BFS 연결, 문 16개와 남쪽 귀환 출구, 세 방의 직교 홈, 방마다 최소 4개 V7 가구, 가구 충돌 footprint 중복 방지, 컨테이너와 가구 충돌 방지를 확인한다.
- `core:test`와 `lwjgl3:build` 성공. 전체 실행 화면은 `docs/images/laboratory_compacted_easy_overview.png`, 플레이 카메라 화면은 `docs/images/laboratory_compacted_easy_play.png`에 있다.
- 직접 입력으로 모든 문과 loot 상호작용까지 통과한 테스트는 아니다. 좁은 통로 이동, 문 개폐, 각 등급 loot 획득 및 D 교체는 수동 확인 항목으로 남긴다.
# Easy map wall packing checks (2026-10-13)

- `EasyLaboratoryLayoutTest.everyUsableRoomWallUsesMeasuredOpaqueFurnitureCoverage` checks all 64 wall reports against measured alpha spans. Short runs around doors, room corners, and concave cutouts are not force-filled when no full prop fits.
- `EasyLaboratoryLayoutTest.easyMapHasSixteenConnectedRoomsAndReturnGate` additionally checks the main cross corridor is exactly two walkable tiles wide (the immediately adjacent rows remain blocked) and verifies that all rooms remain connected.
- Run `./gradlew.bat core:test --no-daemon` after layout edits. Follow with `./gradlew.bat lwjgl3:build --no-daemon` and capture the running game to inspect sprites and wall seams; numerical coverage alone does not verify visual alignment.
- `EasyLaboratoryLayoutTest.everyFurnitureVisibleBoundsAndFootprintStayInsideItsRoom` checks the opaque art rectangle and collision footprint against named room bounds and every intersected logical floor tile, then verifies prop rectangles do not overlap. Layout generation runs the same checks and throws with the prop ID and anchor if invalid data is produced.
- Wall runs shorter than the placement constraints around notches, corners, and door clearances are allowed to remain open; a placement is never forced through a wall to meet a percentage. Dense-fill reports are diagnostic and are not a substitute for the whole-art containment assertions.
- `LaboratoryRoomObstacleTest.furnitureAabbBlocksOnlyItsActualFootprint`: a partial furniture footprint does not mark its entire tile solid; player collision still stops at the real AABB edge.
- `LaboratoryRoomObstacleTest.fRouteUsesTheSameFurnitureObstaclesAsMovement`: body-sized path search avoids the same fixture AABB and every route edge is clear under the same sweep query.
- `LaboratoryRoomObstacleTest.obstacleGridExposesTheSameStaticFootprints`: obstacle enumeration is defensive and matches the movement collision source.
- `EasyLaboratoryLayoutTest.easyWallVisualsUseConnectedCornersJunctionsAndSparseSupports`: generated easy map uses connected corner/junction variants and sparse supports; validates visible connector pairs.
- F1 visual review: green walkable cell outlines, red wall/closed-door cell outlines, cyan furniture footprints, and lime player body. Capture normal and debug images for the same room and verify walking against empty floor, furniture, and wall edges.

## 외벽·대문 V3 테스트
- EasyLaboratoryLayoutTest: 남쪽 복귀 개구부 통행 가능, crossedSouthExit 경계(y=1) 트리거(기존 유지), 16방 연결.
- 수동(실행 창): 외벽 네 방향·모서리 연속, 대문 닫힘 통행 불가→SPACE로 열림→중앙 통로 통행→남쪽 복귀, 문틀 통과 불가, 미니맵/충돌 디버그 반영.

## V4 구조·컨테이너 개편 테스트 (2026-10-14)
- `WallMaskV4` 매핑: `EasyLaboratoryLayoutTest.easyWallsUseConnectedV4MasksWithNoLegacySkin` — 전 벽 셀 유효 인덱스(0..46), 구형 스킨 레이어 완전 비움, 연결 다양성.
- `LaboratoryMetricsTest` — 방 수·문 사이 평균 BFS 거리·등급별 컨테이너 수를 출력하고 전 문 쌍 연결을 단언(전후 비교 근거).
- `LootContainerTest.multiPressOpensExactlyAtRequiredCountAndOnlyOnce` — 1/5/8회 개봉, 진행도 저장, 도달 입력 1회 개봉, 개봉 후 재입력 무시. `balanceConfigMapsContainerIdsToPressCounts` — 설정 매핑.
- 실행 검증(실프레임): `LAB_AUTO_CAPTURE=1 LAB_AUTO_PRESS=1 LAB_START_ROOM=LIBRARY` — 1/5→5/5 로그, 보라 발광 펄스 프레임(`library_glow_ui.png`), 개봉+프롬프트 소멸(`library_opened.png`), 클릭/해제음 재생 호출.
- 수동(사용자): 길게 누르면 1회만 증가, 거리 이탈 후 재접근 시 진행 유지, 두 캐비넷 사이에서 바라보는 대상만 카운트 증가, 가방 가득 시 D 교체, 소리 겹침 체감.

## 전투·전리품 업데이트 V1 테스트 (2026-10-15)

자동(core:test, 133개 통과 / lwjgl3:build 성공):
- `combat/PlayerDashStateTest` — 이동 입력/바라본 방향 폴백, 대각선 정규화, 홀드·쿨다운 중 재발동 금지, 프레임 분할 누적 이동 = 정확히 3타일, 발동 순간 쿨다운 3초 시작, 대상당 1회 타격(markHit), interrupt 후 이동 없음·쿨다운 유지.
- `combat/KillHealTest` — HP90 처치→100 제한, HP30 처치→50, 비처치 타격 회복 없음, 3마리 동시 처치 마리당 20·최대 제한, 사망 몬스터 재타격 무보상, 죽은 플레이어 늦은 보상 부활 금지, StrikeListener는 실제 피해에만 발화.
- `world/DifficultyLayoutTest` — applyDifficulty(EASY)=기준×8(24/16/8/8)·재적용 멱등, NORMAL 5/3/2/1·HARD 7/5/3/1 유지. `spawnedMonsterCountsMatchConfigExactlyPerDifficulty` — 실제 난이도 맵에서 종류별 스폰 수 정확 일치(조용한 감소 금지), 문 위·옆 미생성, 최소 간격(중첩 금지). 팀장 8마리 전원 구석 후보.
- `world/ContainerPlacementTest` — easy/normal/hard 전 컨테이너: 접근 가능한 서기 지점 존재, 문 주변 2칸 비움, footprint 바닥 안, 정중앙 고정점 제거(0.75칸 이내 1/3 미만), 벽면/구석 배치 ≥2, 캐비넷 벽 배치는 북쪽 벽만, 간격 2.2, 시작 지점 3칸 회피(변형 맵).
- 기존 `EasyLaboratoryLayoutTest`(개수 23=14/7/2·간격·가구 비겹침), `LootContainerTest`(1/5/8 연타) 그대로 통과.

실행 검증(실제 GL 렌더 프레임, 합성 미리보기 아님):
- EASY 전체 맵: `spawned SLIME=24 RESEARCHER=16 GUARD=8 TEAM_LEADER=8` 로그 + `frame_001_lab_w128h96_ui.png`.
- 성능(LAB_PERF=1, 400프레임): monsters=56 avgFrame 16.68~17.59ms, maxFrame 17.7~82.3ms(최초 로딩 프레임 제외 시 ~27ms), 약 60fps(vsync) — 공간 분할 추가 불필요 판단(장애물 스냅샷 캐시 적용).
- 컨테이너: `combat_update_press_prompt.png` — SPACE 안내가 캐비넷 불투명 상단 바로 위(화면 6px 환산), "4 / 5" 카운트, 보라 발광, 우하단 전투 HUD(SPACE 라벨+F 키캡). `combat_update_reveal_opened.png` — 2배 등급 테두리 아이콘이 상자 위로 상승, 실제 드롭은 옆 바닥에 분리 생성.
- 돌진: LAB_AUTO_DASH 훅 — "started at 37.5,46.5" → 0.1초 뒤 "now at 39.17"(3타일/0.18s 등속 일치), cooldown=2.9 진행, `combat_update_dash_cooldown.png` — 돌진 아이콘 어둡기+남은 2.8초 표시.

수동 확인 항목(미자동화 — 사용자 핸즈온 권장):
- 효과음 9종의 실제 청취(음량 밸런스·타격음 겹침 체감) — 재생 호출은 실행 중 예외 없이 동작했으나 소리 자체는 미청취.
- 키보드 실입력: F 돌진 방향(8방)·벽/닫힌 문 차단 체감, H 귀환 경로 토글, 가방/일시정지 중 F 차단, Shift 달리기와 돌진 혼용 시 팀장 소음 정상.
- HP 90/30에서 실제 전투 처치 회복 HUD 반영(로직은 KillHealTest로 검증 완료).

## 사망 재소환 테스트 (2026-10-15)
- `PlayerSessionStateTest.deathLosesAllCarriedItemsButKeepsMoneyAndProgress` — 사망 페널티로 가방만 비우고 재화·진행 유지.
- `PlayerSessionStateTest.reviveRestoresFullHpAndClearsDeadStateOnce` — 사망 중 heal 거부, revive 후 ALIVE·HP 100, 이후 피해/회복 정상.
- 실행 검증: `LAB_TEST_DEATH=1 LAB_START_LAB=1` — 소지품 2개 수집 → 치명 피해 → 2초 뒤 전환 → "revived at HQ hp=100 items=0" 로그로 본부 재소환 확인.
- 수동: 실제 몬스터에게 사망해 쓰러짐 연출→페이드→본부 부활, 가방 비움·HP HUD 100, 난이도 모달에 몬스터 숫자 없음 확인.

## 몬스터 이동(리시+로밍) 테스트 (2026-10-15)
- `combat/MonsterRoamTest` — 감지 안 추격(거리 감소), 리시 초과 시 1프레임에 추격 포기, 6초 로밍 동안 이동량 ≥3타일·벽 비침범·재어그로 없음, 로밍 목적지 최소 거리·통행 가능 셀, 분노 팀장은 원거리에서도 추격 유지.
- 실행: EASY 56마리 로밍 상태에서 avgFrame 16.7ms(~60fps, LAB_PERF) 유지, 예외 없음.
- 수동: 몬스터를 끌고 멀리 달아나 추격 포기→다른 방으로 걸어가는 모습, 재접근 시 재추격 확인.

## 효과음 토글 테스트 (2026-10-15)
- 실행 캡처: docs/images/frames/sfx_toggle_lab.png — 연구소 귀환 버튼 아래 켜짐 아이콘 표시.
- 수동: 버튼 클릭 → 아이콘 주황 빗금 전환 + 연타/타격 등 효과음 무음 + 배경음악 계속, 다시 클릭 → 복귀.
  본부에서 끄고 연구소 입장 시 꺼짐 유지(공유 설정), 끄는 순간 재생 중 효과음 즉시 정지 확인.

## 몬스터 행동 상태 기계 + 키 정리 테스트 (2026-10-16)
- `combat/MonsterBehaviorTest`(7): 발견 거리+시야 추적·거리 감소 / 벽 건너 3타일 미발견 / 시야 상실 유예 후 SEARCH(목표가 마지막 목격 지점 3.5타일 내, 벽 통과 없음, 설정 시간 내 종료) / 해제 거리 초과 1회 해제 후 재획득 0회(히스테리시스) / 배회 목표 통행 가능·홈 12타일 내, 슬라임·경비원 이동/연구원 홈 체류 / 팀장 걷기 240프레임 무분노·무추적 / 분노 팀장 해제→도보 복귀→분노·소음 초기화→2회 규칙 재동작.
- 기존 CombatSystemTest(사거리 내 즉시 공격·텔레그래프 타격) 통과 — 시야 조건 추가에도 호환.
- 실행: EASY 56마리 avg 16.9~17.9ms(~59fps, 캡처 프레임 스파이크 제외), 이동 연속 캡처 ai_wander_t0s/t4s.png에서 4초 사이 다수 몬스터 위치 변화 확인. F1 디버그로 상태·목표·반경 표시.
- 수동: 접근→발견, 도망→8타일 해제, 벽 뒤 숨기→2초 후 마지막 위치 탐색, 문 앞 다수 몬스터 정체 없음, 클릭 공격/SPACE 상호작용/F 돌진/R 가방/버튼 귀환 토글 확인.

## 획득 토스트·개봉음 V3 (2026-10-16)
- 실행 캡처 loot_toast_center.png: 긴 이름 "고장난 컴퓨터 본체 +800"이 HP 바 아래 중앙에 아이콘과 함께 잘리지 않고 표시(수집 실경로: backpack.collect→flashLoot).
- 수동: 여러 아이템 이름 길이에서 박스 폭 적응·2초 페이드, 개봉음이 둔탁하게 들리는지 청취.

## 귀환 발자국 회귀 테스트 (2026-10-20)
- `world/ReturnRouteRegressionTest`: 상자 footprint에 밀착한 시작 셀(기존 빈 경로 재현→보정 후 성공), 몬스터 2마리 복도 완전 봉쇄(동적 실패→정적 폴백 성공), EASY 8배 맵의 열린 셀 40곳 표본 전부 경로 생성.
- 실행: LAB_AUTO_ROUTE=1 + 몬스터 56마리 환경에서 발자국이 플레이어→복도→대문으로 렌더됨(return_route_footprints.png).
- 수동: 상자/가구 바로 앞·몬스터 밀집 지역에서 귀환 버튼 토글 시 발자국 즉시 표시 확인.

## 본부 보관함·판매·저장 테스트 (2026-10-20)
- `profile/HeadquartersEconomyTest`(8): 이관 1회 보장·중복 콜백 무복제·인스턴스 ID 구분, 이관 저장 실패 시 가방 보존+재시도, 부분 수량 판매 합계·잔여, 전체 판매는 판매 가능만(알 수 없는 아이템 보존), 초과/음수/미지 ID 거부, 거래 ID 중복 1회 반영, 판매 저장 실패 시 물품·재화 동시 롤백+재시도, 강화 기록 보존.
- `profile/ProfilePersistenceTest`(7): 코덱 왕복 전 필드 보존(알 수 없는 강화 ID 포함), 과거 세이브 마이그레이션(재화 보존·알 수 없는 최상위 필드 재기록), 디스크 왕복·첫 실행 null, 주 저장 손상→백업 복구, 둘 다 손상→*.corrupt 보존(조용한 덮어쓰기 없음), 선택 상태 수량 제한·합계·판매 불가 제외.
- 실행 검증: LAB_TEST_SALE 훅 — 표본 5개 이관→판매창 캡처(hq_sale_panel/selected/sold.png)→전체 선택→판매 9240W 정확. 재실행 시 "loaded rev=2 money=9240" 복원 확인. 이후 실제 수동 플레이로 연구소 수집→귀환 이관(items=1)→판매(+210원)→프로필 rev=4/9450원 저장까지 확인(profile.json+backup 생성).
- 미확인: 창 크기별(작은/큰 창) 글자·스크롤·클릭 수동 확인, 저장 실패 UI 메시지 실화면(로직은 테스트로 검증).

## 영구 강화 트리 테스트 (2026-10-20)
- `profile/UpgradeTreeTest`(10): 실제 카탈로그 로드/수치, 중복 ID·없는 선행·순환·음수가·미지 효과 로딩 거부, 선행 미충족·재화 부족·중복 노드·거래 ID 반복 차단+정확한 차감, 저장 실패 롤백+재시도, 해금 자동 장착·해금 전 성능 강화 차단·슬롯 +1, 파생 능력치 멱등·알 수 없는 노드 보존, setMaxHp 현재 HP 유지·정수기 새 최대 회복·회복 상한·축소 클램프, HUD 색 비율(120/170 healthy·80/170 warning·40/170 critical), 장착·카탈로그 버전 저장 왕복, 구 프로필(레벨 맵) 마이그레이션.
- 실행 검증(LAB_TEST_UPGRADE 훅): 구매 2건 차감 정확(10000→8800→5800), maxHp 120/현재 100 유지, 재실행 복원(rev=4·5800원), 이미 구매한 노드 재구매 거부(재화 불변), 연구소 HUD 120/120+Q 회복 슬롯 캡처, 강화창 상태 4종 실프레임(hq_upgrade_tree/purchased.png).
- 수동: 실제 Space로 강화대 패널 열기/닫기, 노드 클릭→상세→구매 버튼, 연구소 전투에서 공격 +3 체감·Q 회복(녹색 연출·쿨다운), 리사이즈별 글자·클릭 확인.

## 공통 스킬 시스템 테스트 (2026-10-20)
- `profile/SkillSystemTest`(17): 회복 30 적용+정확히 10초 쿨다운(9초 재사용 거부→10초 가능), 실제 회복량 = min(30, maxHp−HP)(90→100), 강화 최대치(150)·강화 회복량(+10→40) 반영, **만피 실패 시 HP·쿨다운·효과 무변화**, 사망 시 회복 불가(부활 아님), 입력 차단 시 무변화, 잠긴/빈/미해금 슬롯 이유 구분, 돌진 공통 경로 3초 쿨다운+쿨다운 중 재발동 금지(이중 관리 없음), 돌진 시작 거부 시 쿨다운 미시작, **쿨다운은 skillId 기준 — 재장착/슬롯 이동에도 유지**, 중복 장착·용량 밖·미해금 장착 거부(상태 불변), 예약 키·중복 키 등록 거부(기존 키 유지), 최초 프로필 기본 로드아웃(F=돌진, 기본 키 F/Q/1/2/3) 1회, 구 equippedSkills→슬롯 2 회복 마이그레이션, 손상 설정 정리(미지 스킬·중복 장착·중복/예약 키→기본 키 복구+사유 로그, 유효 항목 보존), 저장 리스트 왕복, 런타임 스냅샷(0 초과만)·복원(오프라인 경과 없음).
- `combat/PlayerDashStateTest`(5, 개편): 쿨다운 제거 후 이동 전용 — 방향 정규화/바라봄 폴백, 돌진 중 재시작 금지, 총거리 정확히 3타일, 대상당 1회 타격, interrupt 즉시 정지.
- `profile/ProfilePersistenceTest`: 코덱 왕복에 skillSlots/skillKeys/skillCooldowns 추가.
- 실행 검증(2026-10-20, 전부 실프레임/실로그): ① LAB_TEST_SKILLS — 구 프로필(equippedSkills=[heal]) 마이그레이션 "initialized default loadout" → slots=[dash,heal,,,] keys=[34,45,8,9,10] capacity=2 저장(profile.json에 skillSlots/skillKeys/skillCooldowns 기록 확인), 스킬창·키 대기 캡처(skill_panel_hq/keywait.png). ② profile.json에 dash 2.5초 주입 후 재시작 — LAB_AUTO_DASH 발동 결과 **ON_COOLDOWN**(이동 0, 쿨다운 2.5→2.08 플레이 시간만 감소) = 저장 쿨다운 복원 실증. ③ 스냅샷 초기화 후 — OK(63.5→64.7 이동, 쿨다운 2.9) + 퀵슬롯 바 가림+올림 초 "3" 캡처(skill_dash_cooldown_hud.png). ④ LAB_AUTO_HEAL — 120→80 피해 후 회복 OK(80→110, 쿨다운 10.0) → 즉시 재시도 **ON_COOLDOWN(110→110 무변화)**, 캐스트 연출·Q 슬롯 "10" 캡처(skill_heal_cast/cooldown_hud.png).
- 미확인(수동 필요): 실제 키보드로 키 재등록 후 기존 키 무발동·새 키 발동 체감(로직은 keyAt 폴링 구조+단위 테스트로 보장), 슬롯 확장(active_slot_2) 구매 후 3번째 슬롯 실사용(용량 로직은 단위 테스트로 검증), 리사이즈별 퀵슬롯/스킬창 글자·클릭.

## 추가 스킬 3종 테스트 (2026-10-20)
- `combat/ThreeSkillsModelTest`(13): 채택 카탈로그 수치/곡선 단조 증가/곡선 끝 고정, 시전 이벤트 경계 1회(t=0 시작 실행·큰 delta 누락 없음·취소), 충격파 — 바깥 대상 처음부터 안 맞음·파면 도달 1회·잔상 무재타격, 큰 delta 1프레임에 전체 창 통과해도 원점 위 대상 포함 전부 1회+방향 정규화(NaN 없음), 차폐 차단·이동 적 고리 검출, 전기 — 방향 고정·1투사체 1충돌, swept 얇은 벽(0.2타일) 정지·벽이 몬스터보다 앞이면 무피해, 사거리 소멸·죽은 대상 무시, 감전 max 갱신·자연 복귀, 보호막 — 60→35피해→25·HP 0 / 25→35→0·HP10, 준비 중 무방어+재시전 거부, 만료 선처리·만료≠파괴·만료 후 무흡수, clear 정리, 공통 파이프라인 통합(전부 흡수 시 HP 불변+무적 시작, 무적 거절 시 보호막 불변, 잔여만 차감).
- `profile/SkillSystemTest` +1: 3종이 같은 공통 파이프라인(쿨다운 1회/재시전 거부/시작 실패 무소모/미해금 거부).
- `profile/UpgradeTreeTest` 확장: 17노드, SKILL_DAMAGE_ADD/SHIELD_ABSORB_ADD, 슬롯 확장 III→IV→V 사슬(기본2+3=최대5), 해금≠슬롯 증가.
- 실행 검증(2026-10-20, LAB_AUTO_SKILL3 훅 — 60HP 테스트 몬스터 소환→공통 경로 발동→로그·캡처):
  충격파 `OK cooldown=6.0`, 대상 60→30(정확 30), 금빛 링+명중 연출+넉백 캡처(skill3_shockwave_*).
  전기 `OK cooldown=5.0`, 스폰→비행→명중 60→35(정확 25)+`shocked=true shockTime=0.235`(0.5에서 정상 감소), 착탄·감전 오버레이 캡처. 가구 옆 발사 시 즉시 가구 착탄(=solid 가구 차단 실증) 로그 포함.
  보호막 `OK cooldown=12.0`, 35피해 흡수 60→25·HP 200→200, **무적 중 재공격 거절 시 보호막 불변**, 2차 35피해 25→0·HP 200→190(잔여 10만), 파괴 캡처(skill3_shield_*). HQ 스킬창 5종 목록·강화 트리 17노드 캡처(skill3_panel_5skills/upgrade_tree_17nodes).
- 미확인(수동 필요): 실제 키보드 1/2/3 발동 체감, 30/144fps 실측(큰 delta 동등성은 단위 테스트로 검증 — 1프레임에 전체 피해 창 통과 케이스 포함), 효과음(팩에 신규 사운드 미포함 — 재생 안 함), 다수 몬스터 동시 명중 실화면(모델 테스트로 검증).

## 보스 모드 1-1 테스트 (2026-10-20)
- `boss/BossFightTest`(9): 단계 카탈로그(1-1만 기본 입장, 클리어 기록 해금, 미구현 입장 차단), 소개 중 공격 0·예고 중 피해 0, 조준 고정 전 추적/고정 후 비추적, 큰 delta 1프레임에 3발사 경계 전부 1회(재발사 없음), 투사체 기둥 첫 충돌·착탄·뒤 플레이어 무피해, 장판 시전 시작 바닥 고정·기둥 비차폐·활성 첫 틱+0.5s 간격(간격 전 재피해 없음)·만료 해제, 돌진 기둥 앞 정지(관통 없음)·막히면 무피해·개활지 1돌진 1회 피해(정지 후 겹침 무피해), 보스 사망 시 잔여 투사체/장판 취소·사망 중 늦은 타이머 공격 없음·DYING→REWARD_PENDING→CLEARED→복귀 1회(중복 무시), 동시 사망 실패 우선+잔여 판정 제거, 같은 attemptSeed 같은 패턴 순서(초기 투척→장판→돌진, 즉시 반복 금지).
- `boss/BossRewardTest`(4): 1run 1보상(같은 runId 재호출 ALREADY_PROCESSED — 아이템/해금 복제 없음), 새 runId 토벌 새 보상 1개+최초 해금 중복 누적 없음+인스턴스 ID 유일, 저장 실패 시 이전 상태 유지(물품/해금/영수증 없음)·같은 보상 재시도 성공, 보스 진행/영수증 코덱 왕복(재실행 선확인), 보상 풀=기존 LEGENDARY 테이블(이름/가격 복사 없음)·runId 파생 시드 안정.
- 실행 검증(2026-10-20): ① LAB_HQ_TEST=COMPUTER+LAB_TEST_BOSSPANEL — "보스 단계"+SPACE 프롬프트, 단계 선택창(1-1 입장 가능·나머지 잠김·상세·입장 버튼) 캡처. ② LAB_START_BOSS+LAB_BOSS_AUTO=win — 입장 로그(runId/seed/HP600), 패턴 텔레그래프·투사체·방벽 착탄·보스 HP바(360/600) 캡처, 자동 토벌 → "clear committed status=OK" → 결과창(전설·미니 인큐베이터·10000W·본부 보관함 지급·귀환문 개방) 캡처, 프로필 영속(bossUnlockedStages=[1-2]·영수증) 확인, 본부 복귀(이관/루트 재생성 없음). ③ LAB_BOSS_AUTO=fail — 사망 → FAILED(보상/해금 없음·대문 잠김 유지) 결과창(재도전/본부 복귀) 캡처.
- 미확인(수동 필요): 실제 키보드/마우스로 세 패턴 회피·기둥 엄폐 체감, 30/144fps 실측(경계 1회·큰 delta는 단위 테스트), 재도전 버튼 실클릭 흐름(로직은 상태 기계+테스트 검증), 효과음(신규 없음 — 기존 재사용).

## 9단계 보스전 확장 테스트 (2026-10-21)
- `boss/BossBattleTest`(16, 순수 Java — 수치는 *.runtime.json에서): 부채꼴 예고 중 피해 0·cast당 1회+둔화 1회(갱신형)·기둥 차폐 무피해, 부채 투사체 경계 1회 정확히 5발(재발사 없음), 큰 dt(2.5s/2.0s 두 스텝)로 조준 고정·발사·비행 경계를 건너도 명중 1회(작은 스텝과 동일 — 프레임 독립), 회전 레이저 개활지 명중/기둥 차폐 시 무피해+표시 길이 절단(trimmedLength), 연속 돌진 기둥 앞 swept 정지(관통 없음)·기둥 뒤 무피해·재조준은 예고 중에만(돌진 중 방향 고정), 확장 고리 cast당 1회·차폐 방향 무피해, 겹친 독성 장판 그룹 틱 공유(1.3초 3틱 — 폭딜 없음), 뿌리 구속 겹침 1회+재적용 면역 2초+해제 후에만 재적용, 지연 폭발 표식 후 보스 즉사 → 예약 폭발/해저드 전부 취소·추가 피해 없음, 표식 위치 발사 시 고정(이동 추적 없음)+예고 1초 이상, 분노 전환 HP50% 1회(전환 중 해저드 없음·신규 피해 없음·이후 재등장 없음·회복 0.85배), 동시 사망 실패 우선, 이동 포격 중 보스 이동+표식 고정, 누출 흔적 최대 5개 상한·수명 경과 소멸.
- `boss/BossNineStagesCatalogTest`(6): 9단계 ID·nextStage 순차 사슬(1-1→…→2-3), 기존 세이브 마이그레이션(unlocked=["1-2"]로 1-2 입장 가능·1-3 잠김·잠긴 단계 입장 차단), 1-1 레거시 계약(bossId centrifuge_c01·legacyEngine·표기 "폐기물 변이체"), 전 단계 패턴/투기장/초상/그리기·피격·footprint 분리 데이터 해석(1-보스 9.5타일), 투기장 BFS 도달성(플레이어 반경 부풀림, 스폰→보스→대문)+보스 스폰-기둥 비충돌, 데이터 정책 선언(boss_toxin 공유 그룹·차폐/관통 플래그·분노 0.5·복합 하위 패턴).
- `combat/PlayerSkillRangeBaselineTest`(1): 확장 전 기록한 스킬 수치 불변 — 근접 0.9, 돌진 3/0.18/쿨3, 충격파 2.5/0.8, 전기 10/8/기절 0.5, 보호막 60/4s.
- 1-1 회귀: 기존 `BossFightTest`(9)·`BossRewardTest`(4) 무수정 green(총 245).
- 실행 검증(2026-10-21): ① LAB_TEST_BOSSPANEL — 3×3 실루엣 카드(잠김에도 실루엣 표시·contain-fit 잘림 없음·토벌/잠금 배지·선택 테두리), 1-1 상세(컬러 초상·폐기물 변이체·패턴 요약), "1-보스" 잠김 상세 캡처, 기존 프로필 로그 unlocked=[1-2] cleared=[1-1](마이그레이션 없이 호환). ② LAB_BOSS_STAGE=1-2 자동 토벌 — engine=nine·HP700, 빙결 장판 VFX·보스 HP바(냉각관 거수) 캡처, "clear committed status=OK"(보상 centrifuge)·대문 개방·본부 복귀. ③ 1-4 — 거대 기체(8×8)+회전 레이저 빔 캡처·토벌 커밋 OK. ④ 2-2/1-보스/1-1 레거시 회귀 동일 절차.
- 미확인(수동 필요): 실제 키보드로 9단계 전 패턴 회피 체감·밸런스(초안 수치 — 조정 전제), 30/144fps 실측(프레임 독립은 단위 테스트), 둔화/구속 중 입력 체감, 신규 효과음 없음(기존 재사용).

## 난이도별 시드 기반 연구소 생성 테스트 (2026-10-20)
- `world/LaboratoryGeneratorTest`(7): RNG golden vector(파이썬 참조 reference_generator.py와 FNV-1a32+XorShift32 완전 일치 — 64비트 시드 문자열/unsigned shift/오버플로 포함), 같은 시드 동일 결과(contentHash·몬스터 좌표)·다른 시드 상이, 하위 스트림 독립(다른 scope 소비가 수열을 밀지 않음), **난이도별 100시드(총 300) 전수 검증** — 12개 구조 틀 전부 선택·방 활동 영역/캐비넷/문 양방향 접근(배우 그리드 BFS 재확인)·쉬움 금지 몬스터(경비원/팀장) 0·쉬움 예산 정확 56·보통/어려움 최소 종 보장·팀장 구석 조건·몬스터 간격·대문 존재, 난이도별 규모/방 수/몬스터/캐비넷 증가, golden 폴백 시드 상시 유효, 16개 테마 전 난이도 등장.
- 실측 통계(100시드×3): 재시도 0, 폴백 0, 몬스터 56/88/126 고정, 복도 평균 6~7타일·최대 10(기준 ≤9는 참조 팩 좌표 기준 — 실제 이식 후 도그레그 포함 10까지 허용), 캐비넷 23/35/49~50(드롭 ≤1).
- 실행 검증: LAB_SEED=424242 — 쉬움/보통/어려움 조감 캡처(gen_easy/normal/hard_overview.png: 16/25/36방·대문·가구·몬스터), 실내 캡처(gen_easy_ingame.png: V4 문·방향별 가구·컨테이너·미니맵·스폰), 같은 시드 재실행 contentHash 4dbd3700 일치(결정성), LabGen 메타데이터 로그(재현 정보) 확인.
- 기존 수제 쉬움 맵: 고정 회귀 기준으로 보존(EasyLaboratoryLayoutTest·DifficultyLayoutTest·ReturnRouteRegressionTest 등 기존 테스트 전부 유지, LAB_CLASSIC_MAP=1로 실행 가능).
- 미확인(수동 필요): 생성 맵에서 실제 문 Space 개폐/캐비넷 연타/H 귀환/대문 본부 복귀 체감(로직은 기존 공유 경로 + BFS 검증), 보통/어려움 실제 전투 밸런스 체감, 재입장 반복 플레이.

## 본부 스킬 테이블·강화 작업대 분리 (2026-10-20)
- `world/HeadquartersLayoutTest`: 스킬 테이블(북벽 금속 벤치) 남쪽 정면 근접 안내+상호작용 가능/뒤(북쪽) 거부, 강화 트리 = 접이식 작업대 전면 상호작용, 원거리 거부, 침대(field_medical_cot) 제거 확인, 상호작용 지점은 skill_table/folding_workbench로 교체(침대 지점 null).
- 실행 검증(2026-10-20): LAB_HQ_TEST=SKILL 스폰 — 침대 없는 서쪽 배치 + 스킬 테이블 상판 위 "스킬 등록"+SPACE 프롬프트(HP HUD에 안 가림) 실프레임(hq_prompt_skill.png), 이어서 스킬창 열림 캡처. LAB_HQ_TEST=BENCH 스폰 — 망치 작업대 위 "영구 강화"+SPACE 프롬프트(hq_prompt_bench.png).
- 미확인(수동): 실제 SPACE 키로 테이블/작업대 창 열림 체감(발화 경로는 자판기와 동일 코드·단위 테스트 검증), 4개 대상 동시 근접 시 최근접 선택 체감.

# TEST_PLAN

각 기능에 대해 조건(given) · 행동(when) · 예상 결과(then)로 기록한다.
계산·상태 전환은 LibGDX 화면 없이 순수 Java로 테스트한다. 시간은 주입 가능한 delta/GameTime,
랜덤은 seed/공급자 주입으로 재현 가능하게 한다.

## 현재 상태
- 1단계 + Laboratory Tileset V2 렌더링 수정까지 단위 테스트 **42개 통과** (`./gradlew.bat core:test`).
  - `movement/RunStartDetectorTest` (4): 달리기 시작 1회성/재시작 2회성.
  - `movement/MovementSystemTest` (3): 이동 거리=speed*delta(프레임 독립), 뛰기>걷기, 벽 비통과.
  - `input/InputStateTest` (2): pressed/held/released 엣지.
  - `world/LaboratoryRoomTest` (3): 테두리/범위밖 고체, 스폰 바닥, 크기.
  - `world/LaboratoryLayoutTest` (14): 아래 배경 다양화 및 4레이어 시나리오.
  - `world/LaboratoryTileCatalogV2Test` (8): ID/인덱스, PNG 크기·알파.
  - `world/WallAutotilerTest` (8): 직선·끝·모서리·T자·십자·문 연결.
- 전투/보물/팀장 테스트는 해당 단계에서 추가.

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

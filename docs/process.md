# process.md — 연구소 맵 자연스럽게 보이기 작업 진행상황

작성일: 2026-10-06 · 브랜치: `map-overhaul`

사용자 요청: "현재 실행 화면 기준으로 연구소 맵이 자연스럽게 보이도록 실제 렌더링 경로까지
추적해 수정하라." (20×15 유지, 기존 이동·충돌·카메라·애니메이션 유지, 논리/시각 타일 분리 유지)

---

## 1. 조사 — 현재 렌더 경로(파일·메서드 단위)

실행 화면이 만들어지는 호출 흐름:

```
Lwjgl3Launcher → LaboratoryGame → LaboratoryScreen.render()
  └ worldRenderer.render(batch, visuals)   // 바닥·데코·벽·문 한 패스(격자 순서)
  └ entityRenderer.render(batch, player)   // 플레이어
  └ debugRenderer.render(...)              // F1 토글(기본 off)

visuals(RoomVisuals)의 출처:
  LaboratoryLayout.testRoom() → fromRows(TEST_ROOM)
    ├ logicalFor(char)  → TileType[][]  (충돌/위험)
    ├ baseVisualFor(char) → TileVisual[][] (바닥/문/데코)
    └ WallAutotiler.pick(room, tx, ty)  → 벽('#') 셀의 시각 타일(이웃 기반)
  LaboratoryScreen 이 layout.visuals() 를 그대로 WorldRenderer 에 전달 → 확인됨(별도 테스트 맵 아님).
```

검증한 반영 경로 체크리스트(요청 항목):
- [x] `LaboratoryLayout`의 `RoomVisuals`가 `LaboratoryScreen`→`WorldRenderer`로 전달됨(단일 경로, 다른 맵 아님).
- [x] `WorldRenderer`는 `RoomVisuals`만 그림(`TileType` 기본 인덱스를 직접 그리지 않음).
- [x] 에셋 경로 정상 — 실행 로그 `[GameAssets] tileset=...laboratory_tileset_64.png size=1254x1254 grid=4x4`,
      fallback 타일 없음.
- [x] 실행 중인 Gradle 모듈 = 수정 대상(`core`/`lwjgl3`), 클린 재실행 로그로 확인.

## 2. 타일셋 실측 (docs/images/tileset_index_reference.png)

타일셋은 실제 1254×1254 PNG를 **4×4 = 16칸**으로 분할(GameAssets). 각 칸 정체를 라벨링해 확정:

| idx | 정체 | idx | 정체 |
|----|------|----|------|
|0|기본 바닥|8|NW 바깥 모서리|
|1|균열 바닥|9|NE 바깥 모서리|
|2|녹색 얼룩 바닥|10|SW 바깥 모서리|
|3|**좌상단 L자 경고 줄무늬**|11|SE 바깥 모서리|
|4|북(상단) 벽|12|닫힌 문(해저드 줄무늬)|
|5|남(하단) 벽|13|열린 출입구|
|6|서(좌) 벽 — 어두운 면 큼|14|독성 슬라임|
|7|동(우) 벽 — 어두운 면 큼|15|파손 바닥·노출 전선|

→ 방향 벽 4종 + 바깥 모서리 4종만 존재. **T자·십자·끝단·안쪽 모서리·문 전용 벽 타일 없음.**

## 3. 화면이 부자연스러웠던 원인 (코드 근거)

| 사용자 지적 | 원인(코드) |
|---|---|
| ① 바닥 반복 격자 | 중립 바닥 타일이 `FLOOR_BASIC`(0) **1종뿐** → 전 바닥이 같은 패턴 반복 |
| ② 경고선이 조각난 장식 | 구 `TEST_ROOM`이 'w'를 실험설비 둘레에 **링**으로 배치. 타일 3은 L자 모서리 마킹이라 링으로 깔면 노란 조각이 흩어져 보임 |
| ③ 상단 벽·내부 설비 공중에 뜸 | 중앙 실험설비를 `#` **내부 벽**(`w##w`)으로 흉내 → 고립된 벽 조각이 떠 보임(가구 에셋 부재를 벽으로 때운 것) |
| ④ 오른쪽 검은 세로 영역 정체 불명 | 서/동 벽 타일(6,7)의 **어두운 면**이 세로로 쌓이며 검은 띠로 보임 + 구 레이아웃의 긴 세로 격리벽 |
| ⑤ 벽 방향 안 맞음 | 위 ③④가 겹쳐 내부 벽 junction이 어수선 |
| ⑥ 가구 거의 없음 | **가구 에셋 자체가 프로젝트에 없음**(아래 4절) |
| ⑦ 구역 구분 약함 | 구 레이아웃이 얼룩('s')으로 북동 전체를 큰 사각형으로 채우고 경고 링이 혼재 |
| ⑧ 외벽 끊김/문 높이 | 긴 세로 벽 + 데코 혼재로 경계가 불연속으로 보임 |

(현재/수정 비교: docs/images/map_before.png ↔ docs/images/map_after.png — 타일셋으로 렌더
경로를 재현한 합성 미리보기. 플레이어·디버그 제외, 타일 선택 로직은 실제와 동일.)

## 4. ⚠️ 블로커 — 가구/소품 에셋 누락 (가짜로 대체하지 않음)

요청이 지정한 가구 에셋을 **프로젝트 전체에서 검색했으나 존재하지 않음**:

- `laboratory_furniture_floor` — 없음
- `laboratory_furniture_front` — 없음
- `laboratory_furniture_side_left` — 없음
- `laboratory_furniture_side_right` — 없음
- `laboratory_small_props` — 없음
- 관련 `.png/.json/.atlas` 모두 없음. 존재하는 텍스처는
  `assets/textures/environment/laboratory_tileset_64.png` 와
  `assets/textures/player/player_scout_sheet_64.png` **둘뿐**.

지시사항("가구 에셋을 못 찾으면 임의 사각형·기존 벽 타일로 대체하지 말고 누락 경로를 보고")에
따라, 다음 항목은 **구현하지 않고 보류**한다(에셋 추가 시 진행):

- 실험대/서버랙/발전기/접수책상/의자/공구함 등 가구 배치
- 바닥형·전면형·좌우 측면형 가구 구분, 가구 Y-sort, 가구 충돌(시각/충돌 크기 분리)
- 가구 위 장식 소품 / 바닥 장애물 소품
- 중앙 실험 구역의 실험대 2~3개 평행 배치(가구 필수) → 현재는 가구 없이 비워 둠

**다음 단계(에셋 필요)**: 위 PNG(+필요 시 atlas/JSON)를 `assets/textures/environment/`에 추가하고
`AssetPaths`·`GameAssets`에 로드 항목을 등록하면, Y-sort 렌더 레이어(현재 플레이어 단일 레이어)에
가구 패스를 끼워 넣어 2차로 구현 가능.

## 5. 이번에 수정한 것 (에셋 제약 안에서 가능한 범위)

1. **`LaboratoryLayout.TEST_ROOM` 재설계** — 네 구역을 화면만으로 구분 가능하게:
   - 남서 입구/안전: 깨끗한 기본 바닥 + 남쪽 외벽 열린 출입구('O') + 스폰 3×3 청결
   - 중앙 허브: 넓은 기본 바닥(미세 균열만), 네 구역 이동 동선
   - 북동 오염 격리: 외벽+내부 벽 밀폐, **유일 출입 = 닫힌 격리문('D')**, 독성/얼룩 **불규칙 군집**
   - 남동 정비/전력: 통로, 노출 전선/균열을 2~3구간에만, `ty3` 통로로 중앙 연결
   - 가짜 내부 벽 설비('w##w') **제거**(③ 해결), 경고 링 **제거** → 경고는 위험 구역 좌상단 코너 1칸만(② 해결)
   - 독성/전선은 2~5칸 **불규칙 군집**(④⑦), 사각형 반복 제거
   - 배치는 고정 seed 생성기(seed=20251006)로 1회 산출해 **문자 데이터로 베이크** →
     실행마다 항상 동일(= 동일 seed 동일 레이아웃을 데이터로 보장), Java에 RNG 없음
2. **`WallAutotiler`** — 스펙이 요구한 벽 형태(직선/바깥 모서리/분리벽/T자/십자/문측벽)를
   `Shape`로 **명시 분류**. 타일셋 미지원 형태는 **회전 없이 가장 가까운 방향 벽 타일로 대체**(문서화).
   닫힌 문은 논리상 WALL이 아니라 오토타일 대상이 아님 → **문 타일을 벽이 덮어쓰지 않음**.
   (타일 선택 결과는 기존과 동일하게 보존 — 미리보기 검증 무효화 방지)
3. **타일 경계 선(bleeding) 완화** — `GameAssets` 텍스처 `ClampToEdge` + Nearest,
   `WorldRenderer`에서 각 타일 영역을 **반텍셀 안으로 인셋**해 비정수 배율에서 이웃 조각 샘플링 방지.
4. **테스트 추가/갱신**(아래 6절).

변경 파일: `world/LaboratoryLayout.java`, `world/WallAutotiler.java`,
`asset/GameAssets.java`, `render/WorldRenderer.java`,
`test/.../LaboratoryLayoutTest.java`, `test/.../WallAutotilerTest.java`.
(이동·충돌·카메라·애니메이션 코드는 손대지 않음 → 회귀 없음)

## 6. 검증 (실제 실행 포함)

- **core:test = 28/28 통과** (실패·에러·스킵 0). 신규: LaboratoryLayout 12개, WallAutotiler 4개.
  - 20×15 유지 / 스폰 통과 가능 / 남쪽 열린 출입구 스폰에서 도달 / 닫힌 격리문 통과 불가
  - 외벽 전부 solid(출입구 1칸 제외) / 정비 구역 **도달 가능** / 오염 격리실 **밀폐(도달 불가)**
  - 벽 방향·모서리 타일 매핑 / 세로 분리벽 근사 / **문 양옆 벽 + 문 미덮어쓰기** / T자 분류
  - 장식 **가로·세로 3연속 없음** / 기본 바닥이 바닥의 과반 / 고정 레이아웃 재현
- **lwjgl3:build 성공** (exit 0).
- **실제 클린 실행**(이전 프로세스 종료 후): 에셋 로딩 정상, **stderr/Exception 없음**,
  `[TileVisualCount]` 로그로 새 배치 확인(BASIC 186, TOXIC 7, WIRES 5, 경고 2, 닫힌문 1, 열린문 1, 16종 전부 사용).

## 7. 남은 한계 / 다음 작업

- (환경 제약) 이 작업은 백그라운드 세션이라 **실제 GUI 픽셀을 직접 캡처·비교할 수 없음**.
  대신 렌더 경로를 재현한 합성 미리보기(docs/images)와 실행 로그로 검증함.
  **최종 눈 확인은 사용자가 `./gradlew.bat lwjgl3:run` 으로 해주세요.**
- (에셋 제약) 바닥 중립 타일 1종 → 격자 반복감은 완전 제거 불가(변형을 과하게 섞으면 더 지저분).
  세로 벽 타일(6,7)의 어두운 면 → 긴 세로 벽은 검은 띠로 보임(아트 특성). 레이아웃으로만 완화.
- (에셋 제약) 가구/소품(4절) — 에셋 추가 후 2차 구현 대상.
- 추가로 자연스러움을 크게 올리려면: 바닥 변형 서브타일, 벽 전용 세트(T/십자/끝단/문틀),
  가구 아틀라스 확보가 필요. 확보되면 `WallAutotiler.Shape`가 이미 형태를 구분하므로
  `tileFor`만 실제 타일로 교체하면 됨.

---

# Laboratory Tileset V2 적용 (2026-10-07)

## 1. 기존 화면이 어색했던 실제 원인
- v1은 손제작 16타일(바닥 중립 1종, 세로벽이 어두운 면)이라 바닥 반복 격자감·검은 세로벽이 불가피했고 가구 에셋이 없었다.
- V2 전용 타일셋(바닥24/벽16/구조물16/오버레이24, 64px)을 받아 타일 배치를 전면 교체했다.

## 2. 수정한 클래스·파일
- 신규: `LaboratoryTileCatalogV2`(JSON 파싱·ID 매핑), `LaboratoryTileSetV2`(ID→리전), `FloorVariantResolver`,
  `OverlayResolver`, `Hazard`, `LaboratoryZone`.
- 교체: `LaboratoryLayout`(4레이어 빌드), `WallAutotiler`(V2 ID·직교+대각), `RoomVisuals`(4레이어),
  `WorldRenderer`(레이어별 렌더), `GameAssets`(4아틀라스 로드·소유), `AssetPaths`, `TileType`(충돌/문만),
  `LaboratoryRoom`(hazard), `LaboratoryScreen`(V2 렌더·LAB_DUMP). 제거: `TileVisual`, 구버전 `laboratory_tileset_64.png`.
- 에셋: `assets/laboratory_tiles_v2/`(4 아틀라스 + JSON + MD).

## 3. 사용한 신규 타일·배치 비율(실행 덤프 기준)
- 바닥 213셀: 기본 clean A/B/C 중심(≈67%) + 마모/긁힘/먼지/습기(15~20%) + 보수판/해치/배수/균열(5~10%) + 경고선 소량.
- 벽 87셀: horizontal/vertical + outer 모서리 + 외벽↔내부벽 T자(wall_t_open_*). 문 3개(세로 닫힘·세로 열림·가로 열림).
- 오버레이 25셀: 오염(toxic puddle/bubbles/spatter/drain_leak) 불규칙 군집, 정비(cable h/v/corner + exposed_wires + electric_sparks),
  중앙 장식(grime/oil/rubble/scorch/glass/lab_residue) 산포. 셀당 최대 1개, 인접 반복 없음.

## 4. 벽 오토타일 처리
- `WallAutotiler.classify`가 직교 이웃(+대각)으로 형태 결정: cnt 2(직선/바깥모서리), 1(끝단), 3(T자), 4+대각(안쪽모서리), 4(십자).
- **OOB=비연결**로 외벽을 직선·모서리로 유지(성벽형 톱니 방지). 문('DOOR_*')은 연결로 보되 WALL이 아니라 오토타일 대상 아님.

## 5. 문·충돌 처리
- 문 방향=벽 방향(가로/세로), 열림/닫힘. 닫힌 문 solid, 열린 문 통과. 구조물 레이어에서 문 타일을 그려 벽이 덮지 않음.
- 충돌은 TileType(논리 셀) 기준, 이미지 투명 여백 미사용. 독성/감전은 `Hazard`로 분리(시각≠규칙).

## 6. 테스트 결과
- `./gradlew.bat clean core:test lwjgl3:build` 성공. core **34/34 통과**
  (신규: Catalog 6, Layout 11, Autotiler 5 / 회귀: 이동·달리기·충돌·입력·방 그대로).

## 7. 실제 실행 화면 확인 결과
- 클린 실행 로그: 4 아틀라스(384×256/256×256/256×256/384×256) 로드, tileSet regions=80, **Exception/stderr 없음**, 구버전 미로드.
- 실행이 산출한 해석 그리드(LAB_DUMP)를 타일셋으로 충실 재구성: `docs/images/map_v2_after.png`.
  네 구역 구분·외벽 연속·모서리/T자 정확·열린 문 void 없음·오염/전선 오버레이 적절·바닥 반복 감소 확인.
- (환경 제약) 백그라운드라 실시간 GUI 픽셀은 직접 못 봄 → 렌더 경로 재현 이미지 + 실행 로그로 검증.
  최종 눈 확인은 사용자가 `./gradlew.bat lwjgl3:run` 으로 수행 권장.

## 8. 남은 문제
- 가구·소품(실험대/서버랙/발전기 등)은 V2에도 전용 스프라이트가 없어 미배치(Y-sort 렌더 레이어 자리만 준비). 에셋 확보 시 2차.
- `wall_breach_*` 지름길 충돌 토글, 문 개폐 상호작용, hazard 실제 피해는 이후 단계.

---

# 벽 주변 검은 사각형/검은 통로 수정 (2026-10-07)

브랜치: `fix-black-voids` (main 605e3a3에서 분기).

## 검은 배경이 발생한 실제 원인 (확정)
- `FloorVariantResolver`가 WALL 셀에 `floor=null`을 넣었고 `WorldRenderer`는 null 바닥을 건너뛰어,
  **벽 셀 아래에 바닥이 전혀 깔리지 않았다.** V2 벽 타일은 투명 배경 RGBA(가로벽 `wall_horizontal`은
  4096px 중 **2168px(≈53%)가 투명**)라, 바닥 없는 벽의 투명 영역으로 어두운 clear 색(0.05,0.05,0.06)이
  비쳐 검은 띠·검은 사각형으로 보였다. 중앙 가로 띠=내부 가로벽, 우측 세로 띠=내부 세로벽, 북쪽 방
  둘레 검정=오염실 벽. 이전 덤프 재구성은 캔버스가 거의 검정이라 이 버그가 가려졌었다(사용자 지적이 정확).
- 교차검증: PNG 알파 정상(wall/structure/overlay 모두 RGBA, 좌상단 alpha=0, 투명·불투명 공존),
  floor는 RGB 불투명. 즉 알파/블렌딩/포맷 문제가 아니라 **바닥 미렌더**가 원인.

## 수정 내용
1. `FloorVariantResolver`: **모든 셀(벽·문 포함)에 바닥**을 깐다. 벽 아래는 장식 없는 깨끗한 바닥
   (floor_clean_a/b/c). 3연속 방지 로직도 수정 — 한 축만 보고 뒤집다 다른 축에 연속을 만들던 버그를
   고쳐 좌·하 두 축 모두 안전한 clean 대체를 고르게 함(`makesRun`).
2. `WorldRenderer`: 레이어 순서 유지(floor→overlay→wall→structure) + 블렌딩 관리 추가.
   불투명 바닥은 `disableBlending`으로 그리고, 투명 레이어 전에 `enableBlending`+SRC_ALPHA 블렌드 복구.
   바닥이 모든 셀에 있으므로 벽/문 투명부 아래로 항상 바닥이 보인다(검은 배경 없음).
3. `TileDebugRenderer`(신규) + `LaboratoryScreen` F2 토글(기본 off): 타입별 셀 테두리 색
   (바닥 회색/벽 빨강/닫힌문 주황/열린문 초록/위험 보라) + 각 셀 레이어 ID(F/O/W/S) 표시.
4. 테스트:
   - `LaboratoryLayoutTest`: "벽 셀엔 바닥 없음" → **"모든 셀에 바닥 존재(벽·문 포함)"**로 뒤집고,
     벽=별도 레이어, 열린 문 바닥+void 타일 없음, 오버레이가 바닥 미교체 테스트 추가.
   - `LaboratoryTileCatalogV2Test`: 벽/구조물/오버레이 **알파 채널·좌상단 투명·투명·불투명 공존**
     (ImageIO), 바닥 불투명 테스트 추가.
5. `WallAutotiler`: 기존 아틀라스의 파손 직선 벽을 좌표 기반으로 약 12.5%만 선택한다.
   `wall_console`은 일반 직선에 쓰지 않으며 모서리·끝·T자·십자는 각 방향 전용 타일을 유지한다.
6. F2 글자는 BitmapFont의 정수 월드 좌표 반올림을 끄고 셀 안에 들어가도록 크기를 줄였다.

## 테스트·실행 결과
- `./gradlew.bat clean core:test lwjgl3:build` = **42/42 통과, 데스크톱 빌드 성공**.
- `./gradlew.bat lwjgl3:run` = 4개 아틀라스 정상 로드, 실행 중 예외 없음. 캡처 후 게임 프로세스를
  `Stop-Process`로 종료했으므로 Gradle의 `:lwjgl3:run` 작업은 종료 코드 -1로 기록됐다.
- 실제 실행 창에서 남쪽 시작 화면 `docs/images/map_black_voids_fixed.png`, 북동쪽 격리실·중앙 통로·정비실
  `docs/images/map_black_voids_fixed_north.png` 캡처. 벽·문 투명부 아래 바닥이 보이고 중앙 가로 띠/오른쪽 세로 띠/
  북쪽 방 둘레의 큰 검은 빈 영역이 없다. 실제 지도 바깥은 어두운 clear 색으로 남는다.
- F2 셀 테두리와 F/O/W/S ID 표시를 실제 실행 창에서 확인했다.
- 열린 문 통과·닫힌 문/벽 비통과는 레이아웃/이동 테스트로 확인했다. 키보드 수동 플레이 전 구간 검증은 수행하지 않았다.

## 남은 사항
- 벽 타일 자체의 청록 표시등과 셀 내부 여백은 기존 아틀라스 그림이다. 에셋을 변경하지 않는 요청 범위에서
  방향·레이어·파손 변형 선택만 수정했다.
- 가구·소품, 문 개폐 상호작용, 위험 타일 피해는 후속 작업이다.

## 수정한 파일 목록
- core/.../world/FloorVariantResolver.java
- core/.../world/WallAutotiler.java
- core/.../world/RoomVisuals.java
- core/.../render/WorldRenderer.java
- core/.../render/TileDebugRenderer.java (신규)
- core/.../screen/LaboratoryScreen.java (F2)
- core/.../test/.../world/LaboratoryLayoutTest.java
- core/.../test/.../world/LaboratoryTileCatalogV2Test.java
- core/.../test/.../world/WallAutotilerTest.java
- README.md, docs/ARCHITECTURE.md, docs/TEST_PLAN.md, docs/ROADMAP.md, docs/CHANGELOG.md
- docs/images/map_black_voids_fixed.png, docs/images/map_black_voids_fixed_north.png

# Connected wall and door repair (2026-10-07)

Inspection of the actual V2 wall PNG found that `wall_horizontal` has a capped body ending before x=0/63, and `wall_vertical` ends before y=0/63. The corner/T art also uses a different centerline. A 2-4px draw overlap cannot close these gaps. The requested per-tile wall/structure folders are absent; source art is in the two V2 atlas PNGs. See `docs/images/wall_asset_inspection.png` and `door_asset_inspection.png`.

The preserved original atlases feed `tools/WallSeamAtlasGenerator.java`. The derived 64px RGBA atlas uses matching opaque edge strips for every declared connector, with caps only at ends and jambs. `WallAutotiler` now classifies all four-neighbor masks as a cross. `DoorPlacementResolver` infers orientation, reserves three cells, and rejects invalid positions; `WallTopologyValidator` checks the result. A second cause of loose fragments was random `floor_seam_horizontal/vertical` selection; these floor tiles depict short rails, so random selection was removed.

Live evidence: `wall_gallery_before.png` versus `wall_gallery_after.png` shows the seam change; `wall_door_assembly.png` and `wall_door_assembly_f2.png` show the 9x9 assembled door layout and matched green connector markers; `wall_main_south.png`, `wall_main_north.png`, `wall_main_east.png` show the production map. The F2 image labels each wall/door ID and mask. See the final task report for build and movement verification.
Verification after the final floor change: 54 core tests passed with zero failures/errors, and `./gradlew.bat clean core:test lwjgl3:build` succeeded. The normal desktop map was relaunched and captured from south, north, and east positions. The east capture shows the closed vertical isolation door on the wall centerline, both jambs connected to straight walls, an outer corner and T junction with no gaps, and intact contamination overlays. In the 9x9 assembled room, simulated W input for two seconds stopped the player at the closed horizontal door (`wall_door_closed_block.png`); S then D input crossed the open vertical door to the east side (`wall_door_open_pass.png`). The F2 capture showed green reciprocal connectors and cyan solid-cell bounds. No runtime wall overlap or tile anchor offsets were used. Door interaction/animation remains outside this repair: state transitions are tested programmatically.
# Temporary headquarters tent implementation (2026-10-07)

The default game now starts in a separate 20x15 HeadquartersLayout using the supplied 39-file `assets/hq_tent` pack. The root Game owns one PlayerSessionState and MapTransitionController across HeadquartersScreen and LaboratoryScreen. The HQ south opening and exit trigger are separate from LaboratoryLayout. HQ has no monsters; furniture uses measured ground footprints and player/prop foot-Y drawing order. The supplied tile art is opaque RGB, while furniture and vending art is RGBA; dimensions and metadata are checked in HeadquartersAssetTest. Tent art was not generated or transformed.

Southward full-foot crossing of the one-cell outside exit starts a one-shot 0.35-second fade-out. At full black the root repositions the same Player at the safe laboratory entrance, switches screens once, and fades in for 0.35 seconds before unlocking input. Shift+S passage was exercised in the real window. Session HP, inventory, money and progress survive screen replacement. F1 visualizes HQ wall/furniture/player/vending/exit/spawn regions. Vending proximity switches to the supplied interact_highlight art; purchase UI, purchase effects and healing are not implemented. `LAB_HQ_TEST=VENDING` is a capture-only spawn pending a vending UI test scene.

Verification: first `clean` failed because a previously running game held `core/build/libs/core-1.0.0.jar`; after stopping that game, `./gradlew.bat clean core:test lwjgl3:build` passed, with 65 tests in 13 suites and zero failures/errors. `git diff --check` passed. `./gradlew.bat lwjgl3:run` launched the normal HQ window and was responding. Direct captures show HQ south (`hq_initial.png`), north (`hq_north.png`), F1 (`hq_f1_debug.png`), vending highlight (`hq_vending_front.png`), dark fade (`hq_transition_black.png`), and laboratory arrival after running (`hq_lab_arrival_run.png`). The recorded transition stages (`hq_exit_stage1.png` through `hq_exit_stage5.png`) show the progression. Run process was left open for manual review.

Current limitations: interaction E has no purchase UI or transaction; healing/maintenance functions and persistence to disk are future work. Visual review of all furniture occlusion angles and input on other hardware remains manual. The source tent corner tile has one orientation and remains unrotated as required.

# Laboratory side door art and arrival point (2026-10-07)

Copied the provided `lab_side_doors_2x2_64.png` to `assets/laboratory_tiles_v2/`. The 128×128 RGBA sheet uses fixed 64×64 TextureRegion splitting with top-left source orientation preserved. `GameAssets` maps the first row's left cell to the existing `vertical_door_closed` render ID and right cell to `vertical_door_open`; only visual regions change, so the logical JSON solidity and door state remain intact. Horizontal doors keep their current art. The original image has two additional variants in its second row, which are not selected by this change.

Moved the `P` marker in the production 20×15 laboratory map from world cell (3,2) to (3,1), the traversable tile directly north of the south entrance at (3,0). Headquarters transition already uses `LaboratoryRoom.spawnPoint()`, so its arrival now lands beside the door while remaining off the door collision cell.

`./gradlew.bat lwjgl3:build` passed after the change. The first launch attempt exposed an accidental 21-cell row where the old spawn marker was removed; I restored the row to 20 cells and the subsequent `./gradlew.bat lwjgl3:run` initialized all atlases and kept the game process responsive. No tests were run for this request. I did not obtain a reliable GUI screenshot for the new running window, so door appearance and the arrival position still need a direct visual check. The updated game process is PID 23136.

Follow-up UI capture note: a later PrintWindow capture showed the updated process still on the headquarters screen; Windows denied bringing the game window to the foreground for keyboard-driven transition, so that capture was discarded. The game window remains responsive. No claim of direct visual confirmation is made for the replacement door frames or new doorfront spawn.

---

## 2026-10-12 continuation — roomkits and loot campaign

The requested archives are now extracted under `assets/laboratory_roomkits_v3/` and `assets/loot/`. `loot/` is placed inside the LibGDX internal assets root so `Gdx.files.internal("loot/loot_tables.json")` and the JSON-relative animation/item atlas paths resolve at runtime.

### Asset inspection

- Read `laboratory_roomkits_v3.json`, `ASSET_CATALOG.md`, and both README files. Atlas cells are fixed-size; source origin is top-left, Nearest filtering is specified, and `_north/_left/_right` denote wall placement. No runtime rotations are applied.
- Visually opened the full roomkit preview, main-gate and gate-UI atlases, chemistry north/left variants, and wooden chest preview. The gate sheet is a 2×2 grid of 384×256 cells; UI sheet uses the same cell dimensions. Chest animation JSON specifies 64×64 cells, 0.12s/frame, frames 0–7, no loop, reward frame 6.
- Loot table paths resolve from `assets/loot/`: `containers/<id>/<id>_animation.json` and rarity `items/..._atlas_64.png` paths.

### Code changes

- Added `RoomKitAssets` and `LootAssets` as `GameAssets`-owned loaders; all atlas textures use Nearest and are disposed with the game.
- Added HQ gate difficulty keys and lab-to-HQ south return transition. Easy layout is 32×24 with connected chemistry, biology, records/CCTV and entrance areas; normal/hard variants are 28×20 and 38×28. Directional roomkit props use smaller ground collision rectangles.
- Added mini-map, F shortest path to the return gate, brief footstep marks, corner-biased team-leader spawning, loot container animation/reward, E pickup, 8-slot bag reuse, and D lowest-value replacement.
- Added tests for room reachability, difficulty dimensions/counts, pathfinding, team-leader corner candidates, supplied loot-table parsing/reproducibility, one-shot container rewards, and lowest-value swap.

### Verification and remaining work

- `./gradlew.bat clean core:test lwjgl3:build` passed: 102 tests, 0 failures/errors. The loot reward now appears at the supplied `rewardFrame` 6; the final open frame stays visible.
- Real window captures showed the roomkit easy layout, minimap, F path, HQ return and easy re-entry. The capture did not exercise every room collision, difficulty screen, boss enrage, or loot interaction. These remain manual acceptance checks; do not report those as visually verified.
- Continue by launching the latest build and exercising E/D loot and all difficulty choices before declaring the full request complete.

### 2026-10-12 runtime continuation

- Re-ran `./gradlew.bat core:test lwjgl3:build`: passed; test reports contain 102 tests, 0 failures, 0 errors.
- Launched a fresh `--no-daemon` runtime with `LAB_START_LAB=1` and `LAB_DISABLE_MONSTERS=1`. The game window rendered the laboratory floor, roomkit props, player HP HUD at 100, and the minimap. Captures are in the local `build/` directory (`no-daemon-start.png`, `lab-east2.png`, `chest-near.png`).
- Keyboard-driven movement was intermittent in this desktop session. Movement and map scrolling were observed once the game window was clicked, but I did not reach and activate a loot container or verify reward pickup / D replacement in the live window. The loot interaction is covered by automated unit tests only for now.
- Easy/normal/hard selection and the HQ return/re-entry were exercised in the earlier continuation; normal and hard maps rendered with their distinct layouts. Full per-room collision/path traversal and the team leader’s two-run anger transition remain manual checks; they are not marked visually verified.
- The game is currently running on the easy laboratory map in the monster-disabled validation mode. Do not use this debug launch configuration as the normal game start.

## 2026-10-12 — Easy lab room and loot revision

- Easy is now a 128×96 logical map with 16 themed 24×18 rooms, three-tile orthogonal corridors, and 48 internal doors. Door visuals/jambs/collision are derived from the same `Door` models; the south return gate remains separate.
- Added ten room-specific furniture props per room and retained separate ground footprints. Render loops cull tile layers and props outside the camera view; the normal camera shows 20×15 tiles to expose adjoining rooms.
- Placed wooden, epic, and legendary containers in chemistry, information, and library rooms. Their rewards come from `assets/loot/loot_tables.json`; the existing bag accepts items and D replacement rejects equal/lower prices.
- Added 16-room reachability, door state, one-shot loot, probability ordering, and bag-value tests. Latest `./gradlew.bat clean core:test lwjgl3:build --no-daemon` passed (106 tests, 0 failures).
- Current run screenshots are `docs/images/laboratory_easy_overview.png` and `docs/images/laboratory_easy_play.png`. The overview confirms the centered facility topology, assets, and HP HUD; the play capture shows movement through an internal door into the connected central corridor, adjoining room sections, and themed props. A full manual three-room/container open-pickup-D walkthrough remains outstanding.

### Follow-up correction — irregular room plans and V7 furniture (2026-10-12)

- Supersedes the preceding 24×18/ten-props summary: the current easy plan is 128×96 with 16 authored room bounds of varying widths/heights, three orthogonal recesses, cross/branch corridors, 16 interior door openings, and one separate south return gate. The exact data is in `LaboratoryLayout.easyRoom()`; walls, floor occupancy, door resolution, collision, minimap, props, and loot positions derive from those plans.
- Easy room furniture uses the supplied `laboratory_orthographic_views_v7` sprites by explicit catalog ID and directional views. V3 roomkit IDs remain for legacy non-easy layouts. The current visual density is still a review risk: the full-map overview is intentionally scaled down and the chemistry capture has broad open floor. More room-specific prop clustering may be needed to meet the requested dense-workspace look.
- Captures: `docs/images/laboratory_irregular_overview.png` and six standard-camera room captures (`laboratory_room_chemistry.png`, `laboratory_room_biology.png`, `laboratory_room_restroom.png`, `laboratory_room_sleeping.png`, `laboratory_room_information.png`, `laboratory_room_library.png`). The screenshots confirm the new layout and V7 asset loading, not a complete player-controlled traversal.
- Latest source verification: `./gradlew.bat core:test lwjgl3:build --no-daemon` passed after the layout changes; the test report contains 106 tests with no failures. `git diff --check` is clean apart from line-ending normalization notices.
- Still not manually verified: walking every branch, opening doors in play, opening/picking up loot, testing full-bag D replacement, and team-leader enrage. A desktop key-injection attempt did not control the focused game window, so these are not claimed as visually tested.

## 2026-10-13 — Compact rooms and wall furniture

- Kept the easy map at 128×96 and retained tile, character, and furniture source scale. Room bounds use 55% of their prior dimensions (minimum 7×7), with 70% room spacing. Main corridors narrowed from 3 to 2 tiles; branch corridors narrow from 5 to 3 after a two-tile-deep doorway vestibule.
- Reworked `carveDoorApproaches` so hallway carving no longer overwrites each door's jamb support cells. The resolver still creates 16 internal doors; all open cells are BFS reachable.
- Packed V7 props along the usable north/south/east/west wall runs, added central work clusters where room size allows, kept prop collision distinct from art, rejected overlapping footprints, and selected chest/cabinet cells in the intended rooms away from furniture and door approaches.
- Measured room sizes and old-template/current prop counts:

| Room | Previous tiles | Current tiles | Props before → after |
|---|---:|---:|---:|
| Chemistry | 17×17 | 9×9 | 10 → 10 |
| Biology | 18×15 | 10×8 | 8 → 13 |
| General lab | 15×13 | 8×7 | 5 → 7 |
| Prep room | 15×13 | 8×7 | 5 → 7 |
| Information | 17×16 | 9×9 | 10 → 12 |
| CCTV | 15×15 | 8×8 | 5 → 7 |
| Specimen | 13×12 | 7×7 | 4 → 5 |
| Library | 19×13 | 10×7 | 10 → 9 |
| Lounge | 17×15 | 9×8 | 10 → 10 |
| Sleeping | 16×17 | 9×9 | 6 → 12 |
| Restroom / locker / shower | 9×12 each | 7×7 each | 4 each → 6 each |
| Records | 15×15 | 8×8 | 5 → 4 |
| Pantry | 9×12 | 7×7 | 4 → 4 |
| Entrance | 17×14 | 9×8 | 10 → 11 |

- The request for four times as many full-size pieces cannot be met across the 7×7 rooms without overlap or blocking the doorway/path. Current count is 129 vs. 104 in the previous fixed template; the visually reviewed overview shows compact rooms and wall runs, but some interiors still have open floor. This is documented as a remaining visual-density limitation, not claimed as fourfold completion.
- Added tests for minimum room furniture, non-overlapping footprints, loot clearances, scaled room bounds, and reachable interiors. `./gradlew.bat core:test lwjgl3:build --no-daemon` passed (106 tests). Real game overview and normal camera captures: `docs/images/laboratory_compacted_easy_overview.png`, `docs/images/laboratory_compacted_easy_play.png`.
- Live screenshot confirms the latest map is rendered. Manual keyboard traversal of each compressed doorway, container pickup and D replacement are not verified in this run.
# 2026-10-13 — Further corridor reduction and measured wall furnishing

- Narrowed the current easy-map 3-tile room branches to 2 tiles (75% of the currently applied width, rounded up to the 2-tile movement minimum). The main corridors were already 2 tiles and stay there. Door vestibules retain the 5-cell straight support required by the door resolver; they are short approach flares, followed by 2-tile travel corridors.
- Replaced fixed sparse wall anchors with wall-run packing on north, south, west, and east walls. The planner uses the supplied V7 PNG alpha-bound measurements (256×256 source) so transparent margins do not count toward visual coverage. North/south front views use horizontal alpha width; west/east side views use their vertical alpha span and bottom-center anchor.
- Placement order is now all wall runs first, then the pre-existing themed center islands. Door openings plus jamb cells, room corners, and authored recesses are excluded from usable wall length. If collision rejects a wall item, a different compatible themed asset is tried. Wall-mounted collision boxes stay within the wall boundary cell, keeping the room aisle open.
- `WallFurnitureCoverageReport` exposes each room/wall usable length and opaque coverage; the test requires every one of 64 runs to exceed 85%. Measured coverage per room (usable / covered tile-length; min–max across its four walls):

| Room | Usable | Covered | Wall coverage min–max |
|---|---:|---:|---:|
| Chemistry | 25.0 | 21.8 | 87.1–87.5% |
| Biology | 25.0 | 21.8 | 86.9–88.3% |
| General lab | 19.0 | 16.6 | 87.0–90.0% |
| Prep room | 19.0 | 16.5 | 86.7–90.0% |
| Information | 25.0 | 21.8 | 87.1–87.5% |
| CCTV | 21.0 | 18.4 | 87.5–88.3% |
| Specimen | 17.0 | 14.9 | 87.0–90.0% |
| Library | 23.0 | 20.2 | 87.0–90.0% |
| Lounge | 23.0 | 20.0 | 86.7–87.5% |
| Sleeping | 25.0 | 21.8 | 87.1–87.5% |
| Restroom | 17.0 | 14.8 | 87.0–90.0% |
| Locker | 17.0 | 14.9 | 87.0–90.0% |
| Shower | 17.0 | 14.8 | 87.0–90.0% |
| Records | 21.0 | 18.4 | 87.5–88.3% |
| Pantry | 17.0 | 14.9 | 87.0–90.0% |
| Entrance | 23.0 | 20.1 | 87.1–88.7% |

- Captured each room in the running LWJGL game with its room-centered debug spawn. Full captures are `docs/images/laboratory_wallfill_<room>.png`; the 4×4 contact sheet is `docs/images/laboratory_wallfill_contact_sheet.png`. HP 100 remained visible. The contact sheet and full `sleeping` capture show attached wall runs and preserved central furniture; the automated reachability test caught and prevented a wall collision box from narrowing an interior tile.
- Final validation passed: `./gradlew.bat clean core:test lwjgl3:build --no-daemon` (107 tests, 0 failures/errors). `git diff --check` reports no whitespace errors; only the existing CRLF normalization notices remain. The screenshots are from the game runtime with monsters disabled for uncluttered layout inspection. A fresh game is open at the chemistry room on the final source.

## 가구 벽 관통 배치 수정 (2026-10-13)
- `FurniturePlacementValidator` 추가: 렌더 앵커와 V7 alpha span을 반영한 전체 표시 영역이 실제 방 바닥 셀 안에 있는지 검사한다. 오목한 벽/문 구역은 벽의 외접 사각형으로 통과시키지 않는다.
- 벽면 가구는 내부 경계에 표시 영역을 맞추고, 작은 배율로 재시도한다. 모든 후보가 유효하지 않으면 해당 슬롯을 건너뛰어 같은 좌표에 중첩하지 않는다.
- 레이아웃 완성 시 모든 prop 표시 영역, collision, 다른 prop과의 겹침을 다시 검사한다. 벽 부착 collision은 벽 지지 타일에 두어 타일 단위 경로 판정이 실내 통로 전체를 막지 않게 했다.
- 테스트에 전 가구의 가시 영역/충돌 영역 포함 및 비중첩 검증을 추가했다. 전체 캡처: `docs/images/laboratory_furniture_containment_chemistry.png`.
## Laboratory collision and wall alignment follow-up (2026-10-13)

- Fixed the phantom blocking source: `LaboratoryRoom.isSolid()` previously treated a tile as fully solid when any part of a furniture AABB overlapped it. It now reports only logical wall/closed door cells; the exact furniture AABBs are supplied separately through `ObstacleGrid` and resolved against actor bounds.
- Player movement, body-sized F-route pathfinding, monster spawn/movement/line checks, and attack line checks now consult those same static obstacle rectangles. Door state remains in the room model, so toggles immediately update solid checks and route queries.
- Wall graphics use the connected V3 wall and structure atlases through `WorldRenderer`. `WallAutotiler` uses logical cardinal neighbors for horizontal/vertical segments, ends, inner/outer corners, T-junctions and crosses; only sparse straight segments receive damaged variants or supports. F1 collision debug colors are green walkable cells, red wall/closed-door cells, cyan furniture AABBs and lime player bounds.
- Added `LaboratoryRoomObstacleTest` for partial-tile movement, path avoidance and defensive obstacle data, plus easy-map connected-wall assertions. Fixed validator handling for intentionally hidden backing perimeter walls.
- The first F1 overlay exposed translucent framebuffer/compositor artifacts. Collision debug was changed to clear outlines instead of translucent fills, and the desktop launcher explicitly disables a transparent framebuffer.
- Captured same-position CHEMISTRY normal/debug images: `docs/images/laboratory_collision_after_chemistry.png` and `docs/images/laboratory_collision_debug_chemistry.png`; F route image: `docs/images/laboratory_collision_froute.png`.
- Latest verification after adding the collider snapshot to F-route search: `./gradlew.bat core:test lwjgl3:build` passed 113 tests with 0 failures. The desktop app started with `LAB_START_LAB=1`, `LAB_START_ROOM=CHEMISTRY`, and `LAB_DEBUG_COLLISION=1`; the F1-style overlay showed red solid cells, green walkable cells, cyan furniture AABBs, and the lime player body. Saved normal/debug images are `docs/images/laboratory_collision_after_chemistry.png` and `docs/images/laboratory_collision_debug_chemistry.png`.
- I manually opened the chemistry chest with Space and observed the open animation/reward state; the F-route rendered from the player to the south exit in `docs/images/laboratory_collision_froute.png`. One directional movement input was also observed. I did not complete a hands-on traversal of three room types or test a closed/open internal door in this run, so those acceptance checks remain outstanding. Desktop captures also show an unrelated compositor/underlying-window artifact on the right side of the F1 screenshot; the normal image is clean.

## V4 벽·문 통일 (laboratory_structure_v4) — 2026-10-14

사용자 요청: 새 금속 직선 벽과 구형 보라색 접합부·문틀이 혼재하는 문제를, 서로 갈라진 렌더러/스킨
경로를 단일 V4 규격으로 통합해 해결. 첨부 팩 `laboratory_structure_v4`(47종 연결 벽 + 가로/세로 방문).

### 통합한 교체 경로(이전 → 이후)
- 이전: WALL 레이어는 V2 타일셋 스킨(접합부=구형 보라 모서리/T/십자), `InteriorWallRenderer`가 직선 면만
  v3 금속으로 덧그림, `ExteriorWallRenderer`가 외곽 2타일 링+대문을 별도로 그림, 문은
  `DoorPlacementResolver`의 `door_jamb_*`/`*_door_*` structure 스킨 + `RoomVisuals.structureAt`가
  `door.visualId()`를 덧그림. → 세 렌더러 + V2 스킨 + 구형 문이 혼재(일부만 교체).
- 이후: **단일 논리 점유 그리드 → V4 8비트 마스크 렌더러 하나**로 수렴.
  - `world/WallMaskV4`: N=1,NE=2,E=4,SE=8,S=16,SW=32,W=64,NW=128. 대각선은 양옆 직교 이웃이
    모두 벽일 때만 유지(정규화). `maskToIndex`로 47종 아틀라스 인덱스(0..46) 선택. 월드 y-up(북=y+1).
    연결 판정은 WALL/DOOR_CLOSED/DOORWAY_OPEN(문 개구부를 지나 벽선이 끊기지 않도록).
  - `render/LaboratoryStructureRendererV4`: 벽 셀마다 마스크로 V4 타일을 1x1로 그리고(바닥/문 노출 셀
    + 강제 외벽 링만; 방 사이 깊은 벽 덩어리는 비플레이 검은 영역으로 남김), 내부 문은 가로/세로 전용
    스프라이트를 개구부에 맞춰 그린다(가로 4x1.5, 세로 1.5x4).
  - `asset/LaboratoryStructureV4Assets`: 47종 아틀라스(8x6, 128px) + 문 4종. Nearest. `GameAssets` 소유.
  - `WorldRenderer.setLaboratoryStructureV4(...)`로 바닥/오버레이 직후·가구/플레이어 이전에 그린다.
  - `easyRoom`/`variantRoom`은 벽/문 스킨 레이어를 비우고(`wall/structure=null`), 문은 `Door` 객체로만
    전달(`RoomVisuals`에 문 미전달 → 구형 structure 스킨 미생성). 외벽 링은 `setForcedWallMask`.
  - 큰 대문은 기존 V3 유지(`ExteriorWallRenderer.renderGate` — 링은 더 이상 안 그림, 대문만).
  - 충돌·경로·미니맵은 기존대로 `LaboratoryRoom`(tiles + Door 상태 + propObstacles + gatePassage) 단일
    점유에서 파생 → 시각과 충돌이 같은 데이터. 문 토글·몬스터/F 경로 동기화 그대로.

### 실제 렌더 검증 경로(합성 미리보기 아님)
- `render/FrameCapture`: 렌더 완료 시점 백버퍼→PNG(상하 반전, 알파 255). F12 키 또는 `LAB_AUTO_CAPTURE`
  (30프레임 UI, 42프레임 F1 충돌, 46프레임 종료). 저장 경로 `docs/images/frames/`.
- `LAB_DIFFICULTY=EASY|NORMAL|HARD`로 직접 난이도 맵 기동. `LAB_WALL_TEST=3` → 47종+문4종 아틀라스 갤러리.
- 캡처(실행 중 GL 렌더):
  - `docs/images/frames/frame_001_lab_w128h96_ui.png` / `..._collision.png` — easy 전체. 외벽 링·내부 방·
    복도·방문 모두 V4, 접합부 보라 없음. 충돌(green/red)과 시각 일치.
  - `..._w28h20_*` / `..._w38h28_*` — NORMAL/HARD도 동일 V4 벽/문(테두리+칸막이+세로/가로 문).
  - `frame_001_lab_v4atlas_ui.png` — 47종 연결 타일(끝/직선/양면/바깥·안쪽 코너/T/십자/채움) + 가로·세로
    방문 닫힘/열림을 한 번씩 확인(연결 가장자리 방향 정합 확인).

### 테스트
- `EasyLaboratoryLayoutTest`의 구형 스킨 단언(`door_jamb_*`, `WallTopologyValidator`) 2건을 V4 모델로 교체:
  문은 `Door` 객체(열림/닫힘 토글 solid 전이), 벽은 `WallMaskV4.atlasIndex`로 전 벽 셀 유효 인덱스 + 연결
  다양성 + 스킨 레이어 완전 비움을 단언. `./gradlew.bat core:test lwjgl3:build` 통과(113 tests, 0 fail).

### 남은 작업(미완)
- NORMAL/HARD는 벽/문만 V4로 통일됨. 방 배치는 아직 균일 2×3 격자(section 2 다양화 미적용).
- 가구 방향(북=정면/남=뒷모습/좌우=측면)은 정상 동작 확인. 중앙 섬 일부 배치 미세 조정 여지.
- 내부 방문 닫힘→열림의 실게임 핸즈온(3방 이동·캐비넷·F경로)과 본부 복귀 왕복은 사용자 실행 확인 필요.

## 방 26개·복도 단축·컨테이너 연타 개봉 개편 (2026-10-14)

### 측정(전 → 후, LaboratoryMetricsTest, 문 전체 쌍 BFS 평균)
| 항목 | 전 | 후 |
|---|---|---|
| EASY 방 수 | 16 | **26** (+62%) |
| EASY 문간 평균 거리 | 66.53 | **52.04** (-21.8%) |
| NORMAL 방/문 | 6구획/7문, 12.57 | **9구획/12문**, 14.30 |
| HARD 방/문 | 6구획/7문, 21.43 | **9구획/12문**, 19.85 |
| 컨테이너(easy/normal/hard) | 3/3/3 | **23/6/8** (wood14·epic7·leg2 등) |

-40% 목표 분석: 압축 후 문 좌표의 맨해튼 하한이 41.77이라 BFS 평균 40 이하는 기하적으로 불가능.
방 형태·테마·문 1개/방·5칸 직선 문틀 규칙을 유지한 채 하한 대비 +25%(52.0)까지 단축했다.
수단: PLAN_SPACING_SCALE .70→.55 압축, 외곽 순환 링(서x29/동x94/북y53/남y5), 1타일 지름길
(x42·x91 남측 세로, x45·x82 북측 세로, y16 남측 가로), 보조실 10개(복도 직결 + 직접 연결 3쌍).

### 컨테이너 개편(설정·크기·연타·연출·소리)
- `BalanceConfig`: woodenChestPresses 1 / epicCabinetPresses 5 / legendaryCabinetPresses 8,
  containerInteractRadius 1.45, cabinetRenderScale 1.4, 개수 wood14/epic7/leg2(easy).
- `LootContainer.press()`: 엣지 입력 1회 누적, requiredPresses 도달 입력에서만 open(추첨) 1회.
  진행도는 인스턴스에 저장(이탈·복귀 유지), `ALREADY_OPEN`은 무음·무효과.
- 대상 선택(`LaboratoryScreen.selectContainerTarget`): 반경 내 미개봉 중 score=dist-0.75·(시선 내적)
  최소 1개 — 동시 진행 불가. 개봉 입력 후 즉시 return으로 수집/방문 중복 실행 금지.
  가방/일시정지/사망/전환 중에는 updateLoot 자체가 호출되지 않는다.
- `ContainerInteractionRenderer`: 방사형 발광 텍스처(코드 생성) 2겹(안=밝은색, 밖=짙은색)으로
  등급 그라데이션(에픽 보라 E05CFF/8A27CC, 레전드 금 FFF08A/E8A921, 나무 파랑 59D9FF/2168E8).
  진행률 비례 강도, 마지막 입력은 1.7→3.0배 반경의 밝은 펄스. 가산 블렌딩 후 상태 복원.
  SPACE 안내는 기존 spacebar 시트(interactable/pressed) + "n / N" 카운트, 열린 컨테이너 제외.
- 효과음 직접 합성(`assets/sounds/`, Python stdlib, CC0 상당 — README에 기록):
  click 55ms(노이즈 트랜지언트+3.15k/4.78k 금속 핑), unlock 240ms(저역 철컥+볼트 슬라이드+해제 벨).
  같은 음원 40ms 스로틀 + click 0.7/unlock 0.9 음량.

### 실행 검증(실제 GL 프레임, docs/images/frames/)
- `library_glow_ui.png`: 에픽 캐비넷 연타 중 보라 발광 펄스 + 1.4배 캐비넷 + 상단 "n/5" 카운트.
- `library_opened.png`: 5회 도달 후 열림(문짝 개방·내부 드롭), 프롬프트 소멸.
- `library_ui.png`: SPACE 안내 패널+카운트 표시. `frame_001_lab_w28h20/w38h28_ui.png`: 3×3 변형 맵.
- `LAB_AUTO_PRESS=1` 자동 연타 로그: epic_cabinet PROGRESS 1/5→4/5 → OPENED 5/5(클릭음 4회+해제음 1회 재생 호출).
- core:test 117 tests 0 fail, lwjgl3:build 성공.

### 미확인(사용자 핸즈온 필요)
- 소리의 실제 청감(재생 호출·스로틀 로직은 검증, 소리 자체는 들어봐야 함), 길게 누르기 1회 체감,
  두 캐비넷 사이 시선 선택 체감, 가방 가득 D 교체와 연타의 상호작용 실플레이.
- 실행 영상 녹화는 이 환경에서 불가(프레임 PNG로 대체). 재현:
  `LAB_START_LAB=1 LAB_DIFFICULTY=EASY LAB_START_ROOM=LIBRARY ./gradlew.bat lwjgl3:run` 후 캐비넷 앞 SPACE 연타.

## 2026-10-15 — 전투·전리품 업데이트 V1 (combat_loot_update_v1 적용)

### 에셋·구조
- `assets/combat_loot_update_v1/` 설치(runtime/ui 4종·runtime/audio 9종·카탈로그·라이선스).
  `CombatUiAssets`·`CombatSoundAssets`가 로드/해제(GameAssets 소유, 프레임 로드 없음). 구 `ContainerSoundAssets`·`sounds/` 2종 사용 중단.
- `BalanceConfig` 추가: killHealHp=20, dash(3타일/0.18s/3s/substep 0.3/히트 패딩 0.12), sfxMasterVolume,
  easy*BaseCount(3/2/1/1)+easyMonsterMultiplier=8(항상 기준값에서 재계산 → 재입장 중복 곱 없음), spawnAttempts 5000.

### 구현 핵심
1. 컨테이너 배치: `LaboratoryLayout.placeContainer` — 벽45/구석25/가구옆20/중앙10 가중 추첨(빈 분류 재배분),
   문 체비쇼프 2칸·footprint 바닥·가구/상호 2.2 회피·서기 지점(0.6×0.5) 보장·캐비넷 북벽 전용. easy+variant 공통.
2. SPACE 안내: `LootAssets.opaqueTopRatio`(로드 시 닫힘 프레임 알파 상단 1회 측정) + 화면 6px→월드 환산 간격.
3. 공개 연출: `ContainerInteractionRenderer.onReveal/renderReveal` — 등급 테두리 아이콘 1.12(2배) easeOut 상승→정지→페이드,
   드롭은 `findDropSpot`(8방 접근 가능 바닥)으로 분리. 중복 생성 없음.
4. EASY 8배: `MonsterSpawnSystem` 3×3 선호 + 셀 단위 예비 후보 분산, 문 3×3 제외, 종류별 수 로그.
   성능: room.obstacles() 복사 유지(안전 계약) + `CombatCollisionGrid` 정적 스냅샷 캐시. 56마리 avg 16.7ms.
5. 처치 회복: `CombatSystem.applyPlayerStrike`(공격·돌진 공용) — 사망 전환만 +20, `Player.heal` 최대/사망 가드.
6. F 돌진: `PlayerDashState`(순수 Java) + 화면에서 room 그리드로 하위 단계 충돌·몬스터 통과 타격. 귀환 토글 F→H
   (`GameAction.DASH/RETURN_ROUTE`), RunStartDetector와 분리.
7. 전투 HUD: `CombatHudRenderer` 우하단, 실제 ATTACK 바인딩 라벨, 쿨다운 어둡기+가림+남은 초 코드 렌더.
8. 효과음: 이벤트 1회 재생(press/open/hit×2 교대/hurt/death/door/swing/dash), 40ms 스로틀, 타격음 80ms 창 3회 제한,
   카탈로그 음량×마스터. 플레이어 피격/사망은 프레임 HP 비교 엣지로 감지(무적 거절·사망 반복 재생 없음).

### 검증
- core:test 133개 0 fail(신규: PlayerDashStateTest·KillHealTest·ContainerPlacementTest, 확장: DifficultyLayoutTest),
  lwjgl3:build 성공.
- 실행: EASY `spawned SLIME=24 RESEARCHER=16 GUARD=8 TEAM_LEADER=8`, LAB_PERF 56마리 avg 16.68~17.59ms(~60fps).
- 실프레임: combat_update_press_prompt.png(근접 안내+4/5+발광+HUD), combat_update_reveal_opened.png(2배 공개+분리 드롭),
  combat_update_dash_cooldown.png(쿨다운 2.8s 표시), frame_001_lab_w128h96_ui.png(8배 몬스터 전체 맵).
- AutoDash 로그: 37.5→39.17(0.1s, 등속 16.67타일/s 일치), cooldown 2.9 진행.

### 미확인(사용자 핸즈온 필요)
- 효과음 실제 청감(재생 호출·스로틀은 검증, 소리는 미청취). 실행 영상 녹화는 이 환경에서 불가(PNG 대체).
- 키보드 실입력: 8방 돌진 체감, 벽/닫힌 문 차단, H 토글, 가방/일시정지 중 F 차단, 다수 몬스터 전투 체감.

## 2026-10-15 — 컨테이너 효과음 V2 + 배경음악

- `assets/container_sfx_v2/`: `generate_container_sfx_v2.py`(표준 라이브러리 합성, 시드 고정)로 press/open V2 생성.
  press=노이즈 트랜지언트+나무 노크(620/1040/205Hz)+3.15kHz 틱, open=팝+280→700Hz 삐걱 스윕+C6·E6·G6 차임.
  `AssetPaths.SOUND_CONTAINER_PRESS/OPEN`만 교체(재생 규칙·스로틀 불변).
- `assets/music/background_music.ogg`: imageio-ffmpeg 번들 ffmpeg로 mp4 오디오 트랙 추출(libvorbis q5).
  `GameAssets.backgroundMusic()`(Music, dispose 포함) + `LaboratoryGame.create()`에서 setLooping(true)·volume 0.45·play —
  화면 전환과 무관하게 연속 반복. `LAB_MUTE_MUSIC=1`로 끌 수 있음.
- 검증: core:test 133·lwjgl3:build 통과. 실행 스모크(AUTO_PRESS)에서 OGG 로드·press×4/open×1 재생 호출 예외 없음.
  실제 청감(경쾌함 체감·음악 음량 밸런스)은 사용자 핸즈온 필요.


## 2026-10-20 — 공통 스킬 시스템 (player_skill_loadout_v1)

### 모델 (순수 Java, profile/)
1. `SkillLoadout`: 장착(slotId→skillId)·키(slotId→keyCode) 구성 + 마이그레이션(최초 기본 F=dash·Q=heal(해금 시),
   구 equippedSkills 이관)과 정리(미지 스킬/중복 장착/중복·예약 키 → 기본 키 복구, 사유 로그, 해금 보존).
2. `SkillRuntime`: skillId별 남은 쿨다운(delta 기반) — 게임 루트 단일 소유(슬롯 교체·맵 전환에도 유지),
   snapshot(0 초과만)/restore(오프라인 경과 없음).
3. `SkillService.tryExecute`: 입력→장착/해금→생존·차단→쿨다운→효과별 검증(회복 만피 거부, 실회복=min(양, max−hp))
   →applyEffect 성공 시에만 쿨다운. 실패=Result 이유만, 상태 불변. Context 주입으로 GL 비의존.
4. 돌진 이관: `PlayerDashState` 쿨다운 제거(이동·충돌·대상당 1회 타격 전용) — 쿨다운은 SkillRuntime("dash") 한 곳.
5. 키 정리: GameAction DASH/SKILL_1 제거(슬롯 키 직접 폴링·엣지 1회), RETURN_ROUTE(H) 재추가,
   `KeyBindings.reservedKeys()` = 스킬 키 금지 집합 유일 출처. 기본 슬롯 1→2(구매 +1 보존, 최대 5).
6. 저장: 프로필 skillSlots/skillKeys/skillCooldowns(코덱 왕복) + `HeadquartersEconomy.saveSkillLoadout`
   (판매 동일 거래 경계) + persist 시 쿨다운 스냅샷 주입. 변경 성공 시에만 저장(프레임 저장 없음).

### UI/VFX
- `SkillAssets`(GameAssets 소유·Nearest), `HealCastRenderer`(시트 4×3 행 우선 12프레임·1/15s·0.8s 1회,
  발 앵커(64,108), 플레이어 추종·알파 0.7, 시각 전용), `SkillBarRenderer`(하단 중앙 5칸 — 실제 키 이름/
  미등록 라벨/쿨다운 가림+올림 초/잠김 구분), `SkillLoadoutRenderer`(1120×720 — 장착/해제/키 등록 대기·
  Esc 취소·예약/중복 거부·성공 시만 onSave), `SkillMenuButtonRenderer`(오디오 토글 오른쪽, 본부·연구소),
  `CombatHudRenderer` 공격 전용 축소, HealEffectRenderer 삭제(시트 연출로 대체).

### 검증
- core:test 186개 0 fail(신규 SkillSystemTest 17, PlayerDashStateTest 5로 개편, ProfilePersistenceTest 왕복 확장),
  lwjgl3:build 성공.
- 실행: ① LAB_TEST_SKILLS — 구 프로필(equippedSkills=[heal]) 마이그레이션 → slots=[dash,heal,,,]
  keys=[34,45,8,9,10] capacity=2, profile.json에 skillSlots/skillKeys/skillCooldowns 기록.
  ② dash 2.5s 주입 후 재시작 — AutoDash ON_COOLDOWN(이동 0, 2.5→2.08 플레이 시간만 감소) = 복원 실증.
  ③ 초기화 후 OK(63.5→64.7, 쿨 2.9) + 퀵슬롯 "3" 캡처. ④ AutoHeal — 120→80 피해, 회복 OK(80→110, 쿨 10.0),
  즉시 재시도 ON_COOLDOWN(110→110 무변화), 캐스트·"10" 캡처.
- 캡처: skill_panel_hq/keywait, skill_dash_cooldown_hud, skill_heal_cast, skill_heal_cooldown_hud.

### 미확인(사용자 핸즈온 필요)
- 실제 키보드로 키 재등록 → 기존 키 무발동·새 키 발동 체감(폴링 구조+단위 테스트로 로직은 보장).
- 슬롯 확장 노드(active_slot_2) 구매 후 3번째 슬롯 실사용(용량 로직은 단위 테스트 검증).
- 리사이즈별 퀵슬롯/스킬창 글자·클릭, 회복 캐스트 연출 체감(캡처는 초기 프레임).


## 2026-10-20 — 본부 스킬 테이블·강화 작업대 분리 (사용자 요청)

- 요청: 침대 제거 + 그 위 금속 테이블에서 SPACE로 스킬 단축키 등록 + 근접 시 SPACE 프롬프트.
  확인 결과 그 테이블이 기존 강화대라, 사용자 지시로 **강화 트리는 망치 있는 접이식 작업대**로 이동.
- HeadquartersProp.upgradeBench → skillTable(id "skill_table", 동일 192×128 아트/footprint).
  HeadquartersLayout: field_medical_cot 제거. HeadquartersRoom: folding_workbench=강화, skill_table=스킬,
  전면 접근 공통 판정(canInteractWithFrontProp/isNearFrontProp)으로 리팩터.
- HeadquartersScreen: skillInteraction 추가, 4개 대상 최근접 1개 선택, skillFired → openSkillPanel().
  HeadquartersRenderer: skill_table에 벤치 아트, 프롬프트 라벨 "스킬 등록"(HeadquartersAssets 추가).
  북서 구석 프롬프트가 좌상단 HP HUD(화면 좌표)에 가려져 → 스킬 테이블·작업대는 가구 아트 위(-1.35)로
  내려 그리도록 조정(첫 캡처에서 가림 실측 후 수정).
- LaboratoryGame: LAB_HQ_TEST=SKILL/BENCH 스폰 추가(각 가구 남쪽 정면). LAB_TEST_SKILLS 스크립트가
  frame 10에 hq_prompt_<대상> 캡처.
- 검증: core:test 187(신규 HeadquartersLayoutTest.skillTableAndUpgradeWorkbenchHaveFrontInteractions,
  침대 상호작용 지점 null 교체)·lwjgl3:build green. 실프레임 hq_prompt_skill.png("스킬 등록"+SPACE,
  침대 없음), hq_prompt_bench.png("영구 강화"+SPACE, 망치 작업대), 스킬창 열림 캡처.
- 미확인: 실제 SPACE 키 체감(발화 경로는 자판기 동일 코드), 리사이즈별 프롬프트 위치.

### 추가(같은 날): 북벽 가구 재배치 — 두 테이블을 중앙 책상 양옆으로
- 스킬 테이블 ↔ 중앙 오른쪽 통신 콘솔(8.75/7.45), 강화 작업대 ↔ 중앙 왼쪽 통신 콘솔(4.35/7.45) 교환.
  콘솔은 북서쪽(2.15/7.45)·서쪽 유지보수 줄(1.75/3.95)로 이동. LAB_HQ_TEST=SKILL/BENCH 스폰·레이아웃
  테스트 좌표 갱신. core:test 187 green, 실프레임 hq_prompt_skill/bench.png로 새 배치+프롬프트 확인.


## 2026-10-20 — 추가 스킬 3종: 충격파·전기 투척·보호막 (player_three_skills_v1)

### 데이터
- ZIP이 중앙 디렉터리 잘림(truncated) 상태 — 로컬 헤더 61개 스트림 파싱으로 전체 복구(마지막
  source PNG 1개만 불완전, 런타임 불필요). data/skills.example.json을 data/skills.json으로 채택
  (기존 확정 밸런스 없음), upgrade_tree.json에 해금 3+강화 3+활성 슬롯 IV/V 병합(총 17노드,
  스킬 가지 3×4 재배치 — 이전 노드 보존).

### 모델 (순수 Java)
- profile/SkillCatalog(파싱+파면 보간 — 판정·렌더 공용), combat/SkillCast(경계 "이전<t≤현재",
  t=0 시작 1회), combat/ShockwaveState(원점 고정·annulus 1회 명중·차폐 주입·NaN 폴백),
  combat/ElectricProjectile(방향 고정·swept·벽 우선·1충돌), Monster.applyShock(max 갱신)+AI 감전
  게이트(미확정 공격만 취소), combat/PlayerShieldState+PlayerCombatState.setShield(공통 피해
  파이프라인 한 곳 흡수·만료 선처리·파괴/만료 구분), CombatSystem.applyPlayerSkillStrike(스킬
  피해·넉백 거리 공용 경로 — 기본 공격 보너스 비가산), UpgradeCatalog/Effects에
  SKILL_DAMAGE_ADD·SHIELD_ABSORB_ADD, SkillService KNOWN_SKILLS 5종.
- 게임 접점: LaboratoryGame.skillCatalog/skillDamage/shieldAbsorbAmount/skillCooldownSeconds,
  unlockedSkills는 SKILL_UNLOCK skillId 전체 연동.

### 렌더/화면
- ThreeSkillsAssets(아이콘 3+시트 6, 128px 셀 행 우선), SkillVfxRenderer(충격파=엔티티 아래,
  피격/착탄/투사체 회전/보호막 알파 0.65/감전 오버레이=엔티티 위, 충격파 scale=visualExtent 보정),
  SkillBar/SkillLoadout/UpgradeTree 렌더러에 아이콘·계산 수치·미리보기 확장. LaboratoryScreen이
  시전 결과를 상태 목록으로 연결(사망 시 미발생 효과 취소+정리, 쿨다운 유지).

### 검증
- core:test 202 green(ThreeSkillsModelTest 13 신규 등), lwjgl3:build 성공.
- 실행(LAB_AUTO_SKILL3 훅, 테스트 몬스터 60HP): 충격파 OK/쿨 6.0/60→30·링+명중 캡처.
  전기 OK/쿨 5.0/60→35+shocked 0.235s·착탄/감전 오버레이 캡처 — 가구 옆 발사 시 즉시 가구
  착탄(=solid 가구 차단 실증, 테스트 위치를 빈 사격선으로 이동해 재검증). 보호막 OK/쿨 12.0/
  35피해 60→25·HP 불변→무적 거절 시 보호막 불변→25→0·HP −10·파괴 1회 캡처. HQ 스킬창 5종·
  강화 트리 17노드 캡처. 캡처: skill3_*.png 9장.
- 참고: 검증을 위해 로컬 프로필에 해금/슬롯 노드를 직접 부여했다(실구매 아님 — 사용자 확인 필요).

### 미확인
- 실제 키보드 1/2/3 발동 체감, 30/144fps 실측(큰 delta 동등성은 단위 테스트), 효과음(팩 미포함 —
  재생하지 않음), 제공 GIF는 에셋 예시일 뿐 게임 판정 검증이 아님.


## 2026-10-20 — 난이도별 시드 기반 연구소 생성 (laboratory_generation_v1)

### 조사/재사용
- 기존 쉬움 파이프라인(RoomPlan·drawRoomFloor·carve 전실 패턴·DoorPlacementResolver 5칸 지지·
  addRoomFurniture 방향별 kit·placeContainer 가중 추첨·V4 렌더 경로·외벽 링+대문)을 그대로 공유
  하도록 LaboratoryLayout을 리팩터(RoomPlan/Side 패키지 공개, kitForTheme, addThemedRoomFurniture
  (kit·섬 변형·입구 방 중앙 비움), createForGenerator). 수제 쉬움은 회귀 기준+폴백으로 보존.

### 신규 (world/)
- GenRng: FNV-1a32(seed+"|"+scope)+XorShift32(13,17,5) — 파이썬 참조에서 golden vector 산출 후
  Java 이식(unsigned shift·32비트 오버플로 일치 검증). 하위 스트림: template/topology/shape/
  notch/themes/furniture/loot/monsters/visual + retry:N.
- LabGenData: 12개 layout JSON·difficulty_profiles·room_themes 파싱(Source 주입 — 테스트는 java.nio).
- LaboratoryGenerator(~600줄): 격자 방 배치(pitch-7 상한으로 복도 간격 ≥3 보장, 크기 변형·지터),
  모서리 후퇴 모양(문 앵커 ±2 충돌 시 사각형 유지+통계), Kruskal spanning tree+extraEdgePercent,
  도그레그 복도(두께 3)+문별 5칸 전실, 남쪽 대문 1개(가장 남쪽 행 중앙 방·스폰 통로), 테마 배정
  (16종 최소 1회), 가구/캐비넷(기존 경로), 접근성 BFS(플레이어 몸체·4방향·가구/컨테이너 포함) +
  실패 슬롯 수리(섬 제거·캐비넷 드롭) 1회 → attempt 하위 시드 3회 → 폴백(쉬움=수제, 보통/어려움=
  golden 1001 compact), 몬스터(쉬움 예산 56×방 수 비례; 쉬움은 slime/researcher만; 팀장은 두 벽이
  만나는 구석 후보만·부족 시 축소), contentHash(FNV)·통계(복도/재시도/드롭/팀장).

### 연결
- LaboratoryGame: selectDifficulty에서만 generateExpedition(새 시드 발급 — 화면 재생성/리사이즈/
  일시정지는 재생성 없음), LAB_SEED 고정·LAB_CLASSIC_MAP=1 수제 맵, 몬스터는 생성 플랜에서 소환
  (HP는 config, 결정적 restTime). BalanceConfig applyDifficulty에 난이도별 캐비넷 목표(23/35/50).
  LAB_START_ROOM은 부분 일치(BIOLOGY↔biology_r07) 허용.

### 검증
- core:test 209 green(LaboratoryGeneratorTest 7 신규 — golden vector/결정성/스트림 독립/
  난이도별 100시드 전수/규모 증가/golden 폴백/테마 16종). 100시드×3 실측: 재시도 0·폴백 0·
  몬스터 56/88/126 고정·복도 평균 6~7 최대 10·캐비넷 드롭 ≤1. lwjgl3:build green.
- 실행: LAB_SEED=424242 — 조감 3난이도(gen_easy/normal/hard_overview.png), 실내(gen_easy_ingame.png),
  같은 시드 재실행 contentHash 4dbd3700 일치, LabGen 메타데이터 로그. 환경변수 비persist로 1회
  실행이 멈춤(timeout) — 같은 명령에 env 포함 재실행으로 해결(기존 교훈 재확인).

### 미확인
- 생성 맵 수동 플레이(문 개폐/캐비넷 연타/H 귀환/대문 복귀 체감 — 로직은 공유 경로+BFS 검증),
  보통/어려움 밸런스 체감, 진행 중 탐사 저장(기존 세이브 미지원 — 범위 밖 명시).


## 2026-10-20 — 문서 전반 갱신(사용자 요청)

- PROGRESS: 커밋 타임라인을 실제 git log(최신 74d6f26)와 동기화하고 미커밋 5건(강화 트리/공통
  스킬/본부 테이블 분리/스킬 3종/시드 생성)을 추가. 남은 작업 정리(완료 항목 축약, 탐사 중단
  저장·구매 상점·게임오버 UI·hazard·스킬 효과음·밸런스 체감만 잔존), 검증 메모 209 테스트·
  최신 env 훅·캡처 목록으로 교체.
- TEST_PLAN: "현재 상태"를 2026-10-20(209 테스트)로 갱신, 이전 2026-10-12 기준선은 보존.
- ROADMAP: 상단에 2026-10-20 현재 상태 요약 추가(캠페인 1~5 이후 완료 기능 + 구 3×3 구획이
  시드 생성으로 대체됨을 명시). 이후 항목별 서술은 역사 기록으로 유지.


## 2026-10-20 — 보스 모드 1-1: 본부 메인 컴퓨터 → 원심 배양체 C-01 → 보상 → 복귀

### 데이터/모델
- boss_stage_1_1_v1 설치, stages.json/boss_1_1.json/arena_1_1.json 초안 채택(확정 보스 밸런스
  부재 — 차이 없음 보고). PlayerProfile에 bossUnlockedStages/bossClearedStages/
  bossRewardReceipts("stageId#runId") + 코덱 왕복. HeadquartersEconomy.commitBossClear:
  클리어+다음 단계 해금+보상 인스턴스+영수증 1회 원자 저장(멱등·롤백).
- boss/BossStageCatalog·BossDefinition·BossFight(순수 Java): ENTERING→INTRO(1s)→COMBAT→
  DYING→REWARD_PENDING→CLEARED→RETURNING/FAILED, 패턴 TELEGRAPH→ACTIVE→RECOVERY,
  초기 시퀀스 후 시드 가중(즉시 반복 금지), 경계 "이전<t≤현재" 1회, 투사체 swept(기둥 차단),
  장판(바닥 고정·유효 바닥 보정·첫 틱+전역 틱·최대 2·겹침 1틱·기둥 비차폐/footprint 제외),
  돌진(표면 앞 정지·1회 피해·회복), 사망/실패 전체 취소, 동시 사망 실패 우선.

### 화면/연결
- BossArenaScreen: 광장 조립(반복 바닥+V4 벽+gatePassage/southExitGate 재사용), 보스 프록시
  Monster로 기존 공격/돌진/충격파/전기 경로 공유(HealthComponent를 BossFight와 공유),
  플레이어 피해는 공통 파이프라인, 시트 세그먼트 상태 재생(예고0-1/발사2-3/회복4-5/돌진6-7,
  사망 6프레임 1회 유지), 텔레그래프(원/직선 — 데이터 원점·반경 기준), 보스 HP바(상단 중앙),
  결과창(보상/실패/저장 재시도/닫기(열린 문으로 도보 복귀)/본부 복귀).
- LaboratoryGame: MapId.BOSS_ARENA, startBossRun(입장 확정에서만 runId/attemptSeed 발급,
  전환 중 거부=더블클릭 방지), rewardItemForRun(runId 시드 LEGENDARY 추첨),
  HQ 도착 분기에서 보스 출신은 가방 이관/루트 초기화 생략. HeadquartersScreen: 메인 컴퓨터
  (터미널 책상) 5번째 상호작용 "보스 단계"+단계 선택 패널.

### 검증
- core:test 222 green(BossFightTest 9·BossRewardTest 4 신규), lwjgl3:build green.
- 실행: 컴퓨터 프롬프트/단계 선택창(1-1만 입장 가능) 캡처, LAB_BOSS_AUTO=win — 패턴·방벽 착탄·
  HP바 캡처, clear committed OK(mini_incubator/microscope), 결과창(전설·10000W·본부 보관함
  지급·귀환문 개방) 캡처, 프로필 영속(1-2 해금·영수증), 본부 복귀(이관 없음);
  LAB_BOSS_AUTO=fail — FAILED(보상·해금 없음·대문 잠김) 결과창 캡처.
- 주의: 검증 자동 토벌 2회로 사용자 프로필에 전설 2개(microscope·mini_incubator)와 1-2 해금이
  실제 기록됨(정상 플레이와 동일 경로 — 원치 않으면 제거 가능).

### 미확인
- 실제 수동 전투(패턴 회피·기둥 엄폐 체감), 30/144fps 실측, 재도전 버튼 실클릭.
  제공 GIF는 에셋 예시일 뿐 게임 검증이 아님. 신규 효과음 없음(기존 재사용).

## 2026-10-21 — 9단계 보스전 확장 (boss_nine_stages_v1)

### 데이터 채택
- `assets/boss_nine_stages_v1/data/`: stage_catalog.runtime.json(전 단계 integrationReady=true,
  1-1은 bossId=centrifuge_c01·legacyEngine=true 패치 — 저장 키/보스 ID 불변, 표기만
  "폐기물 변이체"), patterns.runtime.json(17 패턴 초안 채택 — 1-1 수치 불변, 신규는 조정 전제),
  arena_layouts.runtime.json(8 투기장 36×28/44×34/52×40). 원본 draft/catalog는 비교용 보존.

### 순수 Java 모델
- boss/BossStageCatalog 확장: 구(stages.json)·신(stage_catalog) 이중 스키마, displayName/
  legacyEngine/animationSet/draftMaxHp/그리기·피격·footprint 분리 데이터/patternIds/arenaId/초상.
- boss/BossPatternCatalog·BossArenaCatalog 신규(수치 복사 금지 — JSON 유일 출처).
- boss/BossBattle 신규(범용 패턴 엔진): kind별 실행기 — projectile/projectile_fan/cone(차폐·
  cast 1회·둔화 갱신)/ground·ground_bind·ground_trail(경고=판정 반경, boss_toxin 그룹 게이트가
  곧 틱 스케줄 — 겹침 폭딜 금지, 구속 1회+면역)/beam(첫 차폐물 판정·표시 동시 절단)/
  expanding_annulus(cast 1회·차폐 방향 무피해)/ground_burst·moving_ground_burst(표식 발사 시
  고정·1초 이상 예고·곡사)/charge·repeat_charge(재조준은 예고 중만·swept 정지·회차 1회)/
  phase_change(HP50% 1회·전환 중 해저드 취소·이후 회복 0.85배)/sequence(하위 큐).
  경계 "이전<t≤현재" 1회(프레임 독립), 사망/실패 전체 취소, 동시 사망 실패 우선(1-1 정책).
  Context에 sightBlocked/applySlow/applyRoot 추가.

### 화면/연결
- BossArenaScreen: legacy(1-1 — BossFight·32×24·기존 아트 그대로) vs nine(BossBattle·카탈로그
  투기장·1024² 시트) 분기 1곳(접근자 통일). 프록시 Monster 피격 박스 = 카탈로그 값(중심 발+1.6),
  이동 충돌 = footprint. 시트 세그먼트 idle0-3/예고4-5/발사6-7/회복8-9/이동10-11/사망12-15
  (마지막 유지, 몸체 회전 금지), 그리기 8×8(1-보스 9.5)·발 비율 32/256. 둔화=이동 dt 배율,
  구속=이동만 차단(공격/스킬/돌진 허용). VFX 640×160 스트립(장판/부채꼴/레이저/고리/분노 오라).
- BossNineAssets(GameAssets 소유): 보스 시트 8종·VFX 12종·초상 지연 로드·카드/배지.
- BossStageSelectRenderer 전면 개편: 3×3 실루엣 카드(잠김에도 표시·contain-fit)·배지·선택
  테두리 중첩·방향키/Enter·상세(컬러 초상·패턴 요약·해금 조건). 선택 프레임을 실루엣보다
  먼저 그려 가림 해결(실행 캡처로 발견).
- LaboratoryGame: BOSS9_* 경로, 9단계 카탈로그 로드, bossStage()/bossUnlockedView()
  (LAB_BOSS_UNLOCK_ALL=1 디버그 — 릴리스 진행과 분리), LAB_BOSS_STAGE 직행.

### 검증
- core:test 245 green(BossBattleTest 16·BossNineStagesCatalogTest 6·
  PlayerSkillRangeBaselineTest 1 신규; BossFightTest/BossRewardTest 무수정 회귀), lwjgl3:build green.
- 실행: 선택창 3×3 실루엣/잠김 상세 캡처 + 기존 프로필 호환 로그(unlocked=[1-2] cleared=[1-1]),
  1-2(빙결 장판·HP바)/1-4(거대 기체+회전 레이저)/2-2(식물 보스) 자동 토벌 — 전부
  "clear committed status=OK"·대문 개방·본부 복귀.
- 테스트 중 발견·수정: 장판 틱이 존별 경계+그룹 게이트 2중 구조일 때 경계-게이트 어긋남으로
  틱 유실 → 게이트 자체를 스케줄로 단순화. 큰 dt 검증은 스폰·비행에 각각 1스텝 필요.

### 미확인/남은 작업
- 실제 수동 전투 체감·신규 단계 밸런스(초안 수치 — 게임 조정 전제), 30/144fps 실측.
- 자동 검증으로 사용자 프로필에 1-2/1-4/2-2(+1-보스) 클리어·전설 보상이 실제 기록됨
  (LAB_BOSS_UNLOCK_ALL은 입장만 허용 — 클리어 저장은 정상 경로). 원치 않으면 제거 가능.
- 신규 효과음 없음(팩 미포함 — 기존 타격/피격음 재사용).

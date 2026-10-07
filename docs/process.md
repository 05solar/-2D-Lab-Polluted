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

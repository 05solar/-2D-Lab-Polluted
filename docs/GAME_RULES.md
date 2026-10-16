# GAME_RULES — 확정 수치와 판정 규칙

> 판정이 애매해질 수 있는 규칙을 수치와 함께 기록한다.
> **[확정]** = 규칙서로 확정된 값, **[임시값]** = 구현 편의상 둔 기본값(기획 확정 전).

## 기본 스탯 [확정]
| 대상 | 최대 HP | 1회 공격 피해 |
|------|--------|---------------|
| 플레이어 | 100 | 15 |
| 슬라임 | 20 | 설정 파일에서 관리 [임시값] |
| 경비원 | 60 | 설정 파일에서 관리 [임시값] |
| 연구원 | 40 | 설정 파일에서 관리 [임시값] |
| 팀장 | 70 | 35 |

> 아직 확정되지 않은 수치(속도·쿨다운·감지 거리·몬스터 공격력 등)는
> 코드에 흩지 말고 `config`(초기 `BalanceConfig`, 잦아지면 JSON)에 **임시값**으로 둔다.

## 조작 키 (2026-10-16 정리) [확정]
| 입력 | 동작 |
|---|---|
| WASD / 방향키 | 이동 |
| Shift | 달리기(팀장 소음 규칙 대상) |
| **마우스 왼쪽 클릭** | 공격(유일한 공격 입력 — 키보드 공격 키 없음) |
| F | 돌진(3타일/쿨다운 3초) |
| R | 가방 열기/닫기 |
| **Space** | 상호작용 전용: 방문/대문 개폐, 상자·캐비넷 연타 개봉, 드롭 수집, 본부 자판기/정수기 |
| D | 가방 가득 시 최저가치 교체 |
| Esc | 가방 닫기/일시정지 |
| 미니맵 아래 버튼 클릭 | 귀환 경로 토글(단축키 없음 — 이전 H 제거) |
| HP 바 오른쪽 버튼 클릭 | 효과음+배경음악 세트 on/off |

UI 버튼(오디오 토글·귀환 버튼)을 클릭한 프레임은 같은 클릭이 공격으로 처리되지 않는다.

## 전투 판정 [확정]
- 한 번 생성된 공격은 **같은 대상을 한 번만** 타격한다.
- HP는 0보다 작아지지 않는다.
- 사망 상태의 대상은 추가 피해를 받지 않는다.
- 몬스터와 접촉했다는 이유만으로 매 프레임 피해를 주지 않는다.
- 공격 쿨다운·피격 무적 시간은 **초 단위**로 관리한다.
- 실제 공격 동작과 명중 구간이 일치해야 한다.

## 이동·소음 [확정]
- 이동 거리 = `speed * delta` (프레임 속도와 무관, 단위 테스트로 검증).
- 대각선 이동은 방향 벡터를 정규화해 등속을 유지한다.
- 달리기 횟수는 `WALKING → RUNNING` **전환 순간에만** 증가한다.
- 키를 계속 누르는 행위를 여러 번의 달리기로 세지 않는다.
- 월드 단위: 1 타일 = 1 단위. 걷기/뛰기 속도 기본값은 [임시값](`BalanceConfig`).

### 달리기 시작(RunStarted)의 정의 [확정 / 운영 정의 일부 임시]
- 이동 상태가 `WALKING`(또는 정지)에서 `RUNNING`으로 바뀌는 그 프레임 1회.
- 달리는 동안에는 프레임마다 세지 않는다.
- **RUNNING 성립 조건(운영 정의):** "달리기 입력(RUN) + 실제 이동 중"일 때만 RUNNING.
  제자리에서 RUN만 누르면 WALKING으로 보고, 움직이기 시작하는 프레임에 RUNNING 전환 = 달리기 시작.
  (구현: `RunStartDetector`. 단위 테스트로 1회성/재시작 2회성 검증)

## 몬스터 행동 상태 기계 (2026-10-16 개편) [확정]

상태: `IDLE(대기)` ↔ `WANDER(배회/순찰)` → `CHASE(추적)` → `ATTACK` / `SEARCH(마지막 목격 지점 탐색)`
→ `RETURN(활동 구역 복귀)`.

### 발견(탐지)
- 발견 = **발견 거리 안 + 시야가 닿을 때**만. 종류별 발견 거리: 슬라임 4 / 연구원 5 / 경비원 7(≈기준 5타일,
  `*DetectionRadius`). **벽과 닫힌 문은 시야를 차단**한다(`LaboratoryRoom.sightBlocked`) — 가까워도 벽 뒤
  플레이어를 거리만으로 발견하지 않는다. 열린 문·통로로 보이면 발견한다. 가구·몸체는 시야를 막지 않는다.
- 감지·경로 재평가는 매 프레임이 아니라 개체 id로 분산된 주기(`monsterSenseIntervalSeconds` 0.15s)로만
  수행한다(EASY 8배 성능).

### 추적과 해제 (히스테리시스)
- 추적 중 플레이어가 보이면 **마지막 목격 위치**를 갱신한다. 시야를 잃으면 그 기억 지점으로만 이동하며,
  벽 너머의 현재 위치를 계속 알아내지 않는다.
- 해제 조건(하나라도 만족 시): ① 거리 > `monsterChaseReleaseRadius`(8타일 — 발견 거리보다 커서 경계에서
  상태가 반복 전환되지 않음) ② 시야 상실 상태가 `monsterLostSightGraceSeconds`(2초) 초과
  ③ 활동 구역(스폰 지점)에서 `monsterHomeLeashRadius`(12타일) 초과 이탈.
- 해제 후: 시야 상실로 끝났으면 마지막 목격 지점 주변을 `monsterSearchMin~MaxSeconds`(2~4초) **탐색(SEARCH)**,
  거리·구역 이탈로 끝났으면 **복귀(RETURN)**. 복귀는 걸어서 하며(순간이동·스폰 좌표 집결 금지), 복귀 중에도
  탐지 조건을 다시 만족하면 재발견한다. 해제로 HP 회복·재생성 같은 부수 효과는 없다.
- 공격 사거리 안에서는 플레이어 중심까지 파고들지 않고 멈춰서 공격 기회를 기다린다.

### 배회(종류별 리듬)
- 공통: 목표 지점으로 1~3타일 걷고 0.5~2초쯤 쉰다. 매 프레임 방향을 바꾸지 않고 도착·막힘·시간 경과에만
  다음 목표를 고른다. 목표는 현재 위치에서 시야가 닿는(같은 방/열린 문 너머) 통행 가능 칸이라 벽·가구
  안에 생기지 않으며, 활동 구역(12타일) 안으로 제한된다. 휴식 시간·출발 시점은 개체마다 다르다.
- **슬라임**: 짧게(1~2타일) 이동하고 자주(0.8~2초) 멈춘다.
- **경비원**: 홈 주변 반경 4타일에서 고른 2~3개 순찰 지점을 오가며 짧게(0.5~1초) 대기한다.
- **연구원**: 장비(홈) 주변 2타일 안을 짧게 배회하고 상대적으로 길게(1.5~3초) 대기한다.
- **팀장**: 평소 구석에 머무르고(배회 없음), 근접 발견 규칙을 쓰지 않는다. 주변 달리기 시작 2회로만
  분노한다(기존 규칙). 분노 후 추적은 위 해제 규칙(거리·시야·활동 범위)을 따르며, 해제되면 원래 구석으로
  걸어서 복귀하고 **복귀 완료 시 분노·소음 누적이 초기화**된다(이후 다시 2회 규칙으로 분노 가능).

### 다수 몬스터 이동
- 이동 시 주변 0.9타일 안 몬스터와 **약한 간격 유지(분리)**를 방향에만 섞는다 — 벽 충돌·통로 이동이 항상
  우선이라 좁은 문 앞에서 서로 밀어내며 영구 정지하지 않는다.
- F1 디버그: 몬스터별 상태 텍스트, 발견 반경(청록/주황)·해제 반경(회색) 원, 현재 목표(흰 선)·마지막 목격
  지점(보라 선) 표식.

## 팀장(TeamLeader) 규칙 [확정]
- 지정 자리에 **앉은(SEATED) 상태**로 시작한다.
- 주변에서 플레이어가 **걷기만** 하면 반응하지 않는다.
- 감지 범위(hearingRadius, [임시값]) 안에서 **달리기 시작 이벤트를 2회** 감지하면 분노한다.
  - `monster.teamLeader.runTriggerCount = 2` [확정]
- 한 번의 달리기 시작은 한 번만 계산한다.
- 플레이어에게 **직접 공격받으면 즉시 분노**한다.
- 분노 후 추격·공격한다. 공격 1회는 플레이어 HP를 **35** 감소시킨다.
- 상태 흐름: `SEATED → WARNED → ENRAGED → CHASE → ATTACK → RECOVER`(→ `DEAD`)

## 보물 반납 [확정]
반납은 다음을 **모두** 만족할 때만 성공한다.
1. 플레이어가 보물을 운반 중이다.
2. 플레이어가 본부 반납 영역(DELIVERY_ZONE) 안에 있다.
3. 상호작용 입력이 **새로** 발생했다(pressed).
4. 같은 보물이 아직 반납되지 않았다.

## 배경 타일 충돌·의미 [확정(충돌) / 임시(위험 효과)]
- **벽**: 고체. **닫힌 문**: 상호작용 기능 전까지 벽과 동일하게 충돌한다.
- **열린 출입구**: 통과 가능.
- **장식 바닥**(균열·얼룩·경고): 통과 가능. 이동/충돌에 영향을 주지 않는다.
- **위험 바닥**(독성·전선): 통과 가능하며 `hazard`로 데이터 구분만 한다.
  HP 감소·상태 이상 등 실제 피해 효과는 아직 없다(2단계 전투 이후 별도 구현).
- 벽의 시각 방향(북/남/동/서/모서리)은 이웃을 보고 자동 선택하지만, **충돌은 논리 벽 셀 기준**이라
  시각이 바뀌어도 통과 가능 여부는 변하지 않는다.

## 사망·보상 [확정] (2026-10-15 재소환 구현)
- 사망 시 이번 탐사의 **미반납** 보물 보상은 제거된다 — 구현: 사망 시 가방(8칸) 소지품을 **모두 잃는다**.
- 이미 반납 완료한 보상(재화)과 진행 플래그는 다음 탐사에도 유지된다.
- **본부 재소환**: 쓰러짐 연출을 포함해 `deathRespawnDelaySeconds`(기본 2초) 뒤 소지품을 비우고
  페이드와 함께 본부 스폰으로 이동, 도착 시점에 부활(HP 최대)한다. 재소환은 사망 1회당 1회만 요청되고,
  사망 상태 동안의 늦은 처치 보상 등으로는 회복/부활하지 않는다(부활은 재소환 경로 전용 `Player.revive`).
- 연구소 상태(열린 상자·남은 몬스터)는 일반 복귀와 동일하게 유지된다.

## Laboratory Tileset V2 — 충돌·위험 규칙 [확정]

- 충돌은 **논리 셀(TileType) 또는 벽 충돌 사각형** 기준이며, 타일 이미지의 투명 여백을 충돌로 쓰지 않는다.
  - 기본 바닥/장식 오버레이: 통과 가능. 벽/닫힌 문: 통과 불가. 열린 문/출입구: 통과 가능.
  - `wall_breach_*`(파손 벽)는 지름길로 쓸 때만 충돌 해제(기본은 충돌). 가구는 별도 충돌 영역.
- 위험(hazard)은 **시각과 분리**된 데이터다(`Hazard`): 독성 계열(toxic/contamination) → TOXIC,
  전기 스파크 → SHOCK. 전선(cable/exposed_wires) 자체는 hazard가 아니다. 1단계는 의미만, 피해 로직은 이후.
- 문 방향은 벽 방향과 일치: 가로 벽=가로 문, 세로 벽=세로 문. 문 상태 변경 시 논리 충돌과 시각이 함께 바뀐다.
  문 타일은 벽 오토타일보다 우선(벽이 문을 덮어쓰지 않음). 열린 문에는 검은 void 대신 문턱/바닥이 보인다.
- 바닥 변형은 고정 seed(`FloorVariantResolver.DEFAULT_SEED=20261007`)로 결정 → 실행마다 동일.
  같은 바닥 타일이 가로·세로 3개 이상 연속되지 않는다. 경고선은 장비 앞/격리 경계/문 주변에만, 사각형 반복 금지.

## Door assembly and collision (2026-10-07)

A door needs a straight wall run of at least five cells. Its center occupies one cell and its two neighboring wall cells become jambs. A corner, junction, adjacent door, or missing jamb space rejects the placement. The surrounding wall topology determines horizontal or vertical direction; the map character only chooses initial open or closed state. `CLOSED`, `OPENING`, and `CLOSING` block movement; only `OPEN` is passable. State changes keep the same cell and direction, and visual art follows collision from the same door object. The opening shows the underlying floor and threshold.

## 임시 본부 천막 [현재 구현 / 임시 전환 시간]

- 본부는 연구소와 분리된 안전 구역이며 몬스터를 생성하지 않는다. 외벽, 모래주머니, 큰 가구의 바닥 접촉부는 충돌한다. 열린 남쪽 천막 출입구와 매트는 통과할 수 있다.
- 남쪽 중앙의 문을 지나 바깥 출구 1칸으로 **남쪽 이동 중 발 충돌 상자가 완전히 넘어가는 순간** 연구소 이동을 1회 요청한다. 접근이나 트리거 내부 대기는 발동하지 않는다. 스폰은 문보다 두 칸 이상 안쪽이다.
- 전환은 입력을 잠그고 페이드아웃 0.35초(임시값) 후 안전한 연구소 입구에 배치한 뒤 페이드인 0.35초(임시값) 후 해제한다. HP, 금액, 인벤토리, 진행 플래그를 유지한다.
- 본부는 14×10이며 카메라가 전체 맵을 고정 표시한다. 자판기와 물통 SPACE 안내는 살아 있고 본부가 활성 상태일 때 대상에서 2.5타일 이내로 접근하면 표시되며, 대상이 겹치면 가까운 하나만 표시한다.
- 자판기 SPACE 동작은 정면 접근과 장애물 없는 경로가 필요하며 구매 UI·재화 차감은 미구현이다. 물통은 2타일 이내의 막히지 않은 접근에서 SPACE를 누르면 생존 중인 플레이어 HP를 최대로 회복한다. 사망 상태를 되돌리지는 않는다.
- F1은 벽·가구·플레이어 충돌, 출구, 본부 스폰, 자판기·물통 상호작용 영역을 표시한다.

## 연구소 난이도·길찾기·loot (구현값)

- 본부 남쪽 게이트에서 1/EASY, 2/NORMAL, 3/HARD를 선택한다. 맵 크기는 쉬움 128×96(테마 방 16 + 보조실 10 = **26개 방**, 간격 압축 .55), 보통 28×20(**9개 구획**, 3×3), 어려움 38×28(**9개 구획**, 3×3)이다. 쉬움은 2타일 복도(외곽 순환 링 + 십자 + 1타일 지름길 x42/x45/x82/x91/y16)로 연결하고, 보조실 일부는 이웃 방과 직접 연결된다(경비초소↔입구, 전기실↔경비초소, 창고↔휴게실).
- 문 사이 평균 보행 거리(BFS, 전체 쌍): 쉬움 66.5 → **52.0**(-21.8%). 문 좌표 맨해튼 하한이 41.8이라 -40%는 기하적으로 불가능하며, 방 형태·테마 유지 전제에서 하한 대비 +25% 수준까지 단축했다.
- 몬스터 수(슬라임/연구원/경비원/팀장) [확정, 2026-10-15]: **EASY는 기준값 3/2/1/1의 정확히 8배 = 24/16/8/8**
  (`BalanceConfig.easy*BaseCount × easyMonsterMultiplier`, 입장/재생성마다 기준값에서 다시 계산해 중복 곱 없음),
  NORMAL 5/3/2/1, HARD 7/5/3/1(이번 변경에서 유지). 팀장은 여전히 외곽 코너에 가까운 안전 후보를 우선한다.
  스폰은 문/개구부 위·옆(3×3), 가구·상자·대문 통로, 입구 안전 반경(4타일)을 피하고, 3×3 여유 후보가 부족하면
  "해당 셀만 통행 가능"한 예비 후보(다른 방·복도)로 분산한다 — 개수를 조용히 줄이거나 한 좌표에 겹치지 않는다.
- 미니맵은 지형·플레이어·팀장을 표시한다. **H**(이전 F — F는 돌진 전용이 됨)는 남쪽 본부 복귀 게이트 최단 경로를
  토글하고, 미니맵 아래 귀환 버튼 클릭도 동일하게 동작한다.

### 루트 컨테이너(상자/캐비넷) — 2026-10-14 개편, 2026-10-15 배치 다양화
- 개수는 `BalanceConfig`로 관리: 쉬움 wood 14 / epic 7 / legendary 2(총 23, 26개 방 대부분에 ≥1,
  도서관 epic+legendary·화학실 wood+epic 복수), 보통 3/2/1(6), 어려움 4/3/1(8). 등급 비율 wood>epic>legendary 유지.
- 가격·확률·등급은 기존 `assets/loot/loot_tables.json` 그대로.
- **배치(2026-10-15)**: 방 중앙 고정점 규칙을 제거했다. 후보 분류는 벽면 45% / 접근 가능한 구석 25% /
  가구 옆 수납 구역 20% / 중앙·보조 공간 10% 가중 추첨이며, 방 형태상 비어 있는 분류의 가중치는 남은
  분류로 재배분된다(시드 고정 → 빌드마다 결정적). 공통 제약: 문/대문 주변(체비쇼프 2칸) 제외,
  footprint가 바닥 안이며 가구·기존 컨테이너와 비겹침(간격 2.2), **플레이어가 서서 SPACE를 누를 수 있는
  접근 지점 필수**(가구 뒤·벽 안 금지), 시작 지점 안전 반경(3칸, normal/hard). 캐비넷은 정면 스프라이트만
  있으므로 벽/구석 배치는 **북쪽 벽(등을 벽에)**만 허용하고 없는 방향을 90도 회전으로 만들지 않는다.
  나무 상자는 낮은 상자라 모든 벽면을 허용한다. normal/hard도 구획 중앙 고정점 대신 같은 추첨을 쓴다.
- **표시 크기**: 캐비넷(일반/고급)은 1.4배(`cabinetRenderScale`), 나무 상자 1배. 닫힘·진행·열림 전 프레임이
  동일한 하단-중앙 기준점을 쓰고, 바닥 점유/충돌/접근 검사도 같은 배율(`containerFootprint`)로 갱신된다.
- **개봉 방식(SPACE 연타)**: 나무 상자 1회, 일반 캐비넷 **5회**, 고급 캐비넷 **8회**(`BalanceConfig.requiredPresses`).
  키를 뗐다 다시 눌러야 1회(길게 누름=1회). 진행도는 캐비넷별로 저장되어 멀어져도 유지되고, 바라보는 가장
  가까운 대상 1개에만 입력이 들어간다(동시 진행 금지). 필요 횟수 도달 입력에서만 열림 애니메이션+아이템 추첨이
  1회 실행되며, 이후 입력은 무시된다. 마지막 개봉 입력은 수집/방문 개폐와 중복 실행되지 않는다.
  가방·일시정지·사망·전환 중에는 개봉 입력을 받지 않는다.
- **안내 UI(2026-10-15)**: 상호작용 거리(1.45) 안 대상 1개에만 SPACE 안내를 표시한다. 기준점은 PNG 캔버스가
  아니라 **로드 시 측정한 닫힘 프레임의 불투명 그림 상단**이며, 간격은 화면 6px(시작값 4~8px)를 현재 카메라
  배율로 환산해 띄운다 — 상자 크기 변경·카메라 이동에도 함께 움직인다. 캐비넷은 "현재/필요" 횟수를 안내 위에
  표시한다. 개봉 완료 후에는 안내를 숨겨 아이템 공개 연출과 겹치지 않는다.
- **발광**: 유효 입력마다 외곽 발광 펄스 — 일반=보라, 고급=금, 나무=개봉 순간 파랑(loot_tables border 색 계열).
  진행 비율이 높을수록 강해지고 마지막 입력은 더 큰 펄스로 열림과 이어진다(가산 블렌딩, 원본 아트 유지).
- **아이템 공개 연출(2026-10-15)**: 개봉 시 등급 테두리 아이콘(파랑/보라/금)을 기존 표시(0.56)의 **2배(1.12)**로,
  상자 불투명 상단 위까지 **짧게 올라온 뒤 멈추는** 연출로 보여준다(easeOut 0.35s → 유지 0.95s → 페이드 0.3s).
  연출은 컨테이너/가구에 가려지지 않는 월드 효과 레이어(HUD 아래)에 그린다. 실제 드롭은 연출과 분리되어
  컨테이너 주변 8방 중 **플레이어가 접근 가능한 바닥 지점**에 1개만 생성된다(벽 뒤·공중 금지, 중복 생성 없음).
  가방 8칸 아이콘 크기는 그대로다.
- **효과음(2026-10-15 V2)**: 유효 연타 입력=경쾌한 나무 걸쇠 '탁!'(`container_sfx_v2/container_press_v2.wav`),
  개봉 완료=걸쇠 팝+뚜껑 삐걱+상승 차임(`container_open_v2.wav`) 1회. 홀드/거리 밖/열린 컨테이너엔 재생하지 않고,
  같은 음원 40ms 스로틀로 겹침을 제한한다(합성 CC0, 음량 × 전체 효과음 음량).
- 드롭 수집은 SPACE 별도 입력, 8칸이 찬 경우 D는 최저 슬롯보다 비싼 물품만 교체하며 밀려난 물품은 바닥에 남긴다.

## 돌진(F)·처치 회복·전투 HUD·효과음 — 2026-10-15 [확정]

### F 돌진
- F를 **한 번 누르면**(엣지 입력, 홀드 자동 재발동 없음) 현재 이동 방향으로, 이동 입력이 없으면 마지막으로
  바라본 방향으로 돌진한다. 대각선도 정규화 등속이다.
- 수치(`BalanceConfig`): 거리 **3타일**, 지속 **약 0.18초**, **발동 순간부터 쿨다운 3초**.
- 돌진이 닿은 몬스터는 기본 공격과 같은 **초기 피해 15**를 입는다. 한 돌진에서 같은 몬스터는 1회만,
  서로 다른 몬스터는 각각 맞는다. 피해·넉백·사망·처치 회복은 일반 공격과 **동일 경로**(`applyPlayerStrike`)다.
- 이동은 최대 0.3타일 하위 단계로 나눠 충돌·타격을 반복한다 — 벽·닫힌 문·가구를 관통하지 않고 이동 구간
  중간의 몬스터를 건너뛰지 않는다. 몬스터는 충돌체가 아니라 통과하며 타격만 받는다. 정면이 완전히 막히면
  돌진이 끝난다(쿨다운 유지). **돌진 무적은 없다.**
- 사망·일시정지·가방·맵 전환 중에는 돌진 입력이 차단된다. 쿨다운 중에는 이동/피해/효과음이 재발동하지 않는다.
- 돌진은 달리기(Shift)와 별개 상태라 달리기 시작(소음) 이벤트가 중복 발생하지 않는다. 팀장 소음 규칙은 그대로다.
- **기존 F 귀환 경로 토글은 H로 이동**했다(귀환 버튼은 유지). F 한 번에 돌진과 길찾기가 같이 실행되지 않는다.

### 처치당 HP 회복
- 플레이어가 일반 공격 또는 돌진으로 몬스터를 **처치하면 HP 20 회복**(`killHealHp`). 최대 HP 100을 넘지 않는다.
- 살아 있음→사망 **전환 이벤트에서만 1회** 회복한다(사망 애니메이션 중 매 프레임 회복 없음, 같은 대상
  중복 보상 없음). 여러 마리 동시 처치는 마리당 20이되 최대 HP에서 제한된다. 이미 죽은 플레이어는 늦게
  도착한 처치 이벤트로도 회복/부활하지 않는다. HP HUD는 같은 프레임의 변경을 반영한다.

### 돌진·공격 HUD
- 우하단에 공격 안내(`attack_icon_128` + 빈 키캡 위 **실제 바인딩 이름**(Space) 라벨)와 돌진 안내
  (`dash_icon_128` + `key_f_64`)를 표시한다. 공격 키를 임의로 재지정하지 않는다.
- 쿨다운 중 돌진 아이콘은 어두워지고 남은 비율 가림 + 남은 초(소수 1자리)를 코드로 그린다. 3초 뒤 준비
  상태로 돌아온다. 화면 좌표라 창 리사이즈에도 유지되고 HP(좌상단)·R 안내(좌하단)·미니맵/귀환 버튼(우상단)과
  겹치지 않으며 화면 밖으로 잘리지 않는다.

### 효과음(combat_loot_update_v1, 합성 CC0)
- 캐비넷 유효 연타=`container_press_low`, 개봉 완료=`container_open_heavy`, 몬스터 **실제 피해**=
  `monster_hit_01/02` 교대(허공 공격엔 타격음 없음), 플레이어 HP **실제 감소**=`player_hurt`(무적 거절 시 없음),
  **최초 사망 전환**=`player_death` 1회(쓰러짐 연출과 함께, 매 프레임 반복 없음), 문/대문 **닫힘→열림**=
  `door_open_metal`(같은 상태 갱신 반복 없음), 공격 시작=`attack_swing`, 돌진 발동=`dash_whoosh`.
- 음량은 카탈로그 권장값 × `sfxMasterVolume`. 추가 피치는 타격음 ±3% 변주만(저음 음원 추가 다운피치 없음).
- EASY 8배 환경 대비: 타격음은 80ms 창 안 최대 3회로 제한하고, 같은 음원은 40ms 스로틀을 둔다.
- 효과음 재생은 시각·전투 이벤트의 결과이며 팀장 소음(달리기 시작) 카운트를 증가시키지 않는다.
- **오디오 토글(2026-10-15 추가, 10-16 개편)**: 좌상단 **HP 바 바로 오른쪽** 64px 버튼(본부·연구소 동일
  위치, 프레임 세로 중앙 정렬) 클릭 한 번으로 **효과음+배경음악을 세트로** 켜고 끈다(청록 음파=켜짐,
  주황 빗금=꺼짐). 끄면 재생 중 효과음 정지·이후 재생 차단 + 배경음악 일시정지, 켜면 효과음 허용 +
  배경음악을 멈춘 지점부터 재개(무한 반복 유지). 설정은 본부·연구소가 공유한다(저장 시스템 도입 전까지
  세션 한정). 클릭 영역은 보이는 버튼과 일치한다.
## First combat pass (2026-10-07)

- Player: 100 HP, 15 damage, four-direction melee; attack cooldown 0.45 seconds, monster-hit invulnerability 0.6 seconds. SPACE (existing binding) or left mouse starts one attack per press.
- Initial monster values are configured in `BalanceConfig`: slime 20 HP/10 damage, researcher 40/15, guard 60/20, team leader 70/35. Player damage therefore defeats them in 2, 3, 4 and 5 hits respectively.
- The first laboratory spawn is 3 slimes, 2 researchers, 1 guard and 1 leader. Candidate locations require a safe 3x3 floor area, avoid hazards and the player spawn radius, and are limited by a maximum attempt count.
- Slime follows slowly; researchers and guards wander and pursue after detection; guards detect farther. Monsters use telegraphed attacks and per-attack hit gating. Player damage has brief invulnerability and collision-resolved knockback.
- The seated team leader ignores walking and becomes enraged after two distinct nearby run-start events within four tiles. Enrage persists through that encounter.
- HP cannot fall below zero. A dead player is input-locked; a dead monster is removed after the update iteration. A game-over screen and loot rewards are outside this pass.
- Tuning values other than the specified HP/damage and leader threshold are first-pass defaults and may be adjusted in `BalanceConfig`.

## Player death animation

- Player HP is clamped to `[0, 100]`; reaching zero enters `DEAD` once and preserves the last facing direction and world position.
- `DEAD` blocks movement, running/noise, attacks, damage, interactions, and the HQ exit. Pause and debug controls remain available.
- The direction-specific death clip advances at 0.1 seconds per frame, does not loop, and holds frame 7. Paused or transition-locked screens pass zero animation delta.
- (2026-10-15) After the collapse is shown, the player loses all carried items and respawns at the
  headquarters with full HP (see 사망·보상). A separate game-over screen is still not implemented.

## Headquarters vending interaction

- The safe headquarters is a 14x10 map, fixed in view with a centered camera. Its only exit remains the south tent opening.
- The vending SPACE prompt appears within 2.5 tiles of the machine for a living, unpaused player, regardless of facing. The vending action itself still requires its front approach and a clear path.
- A new SPACE press emits one vending interaction event, flashes the pressed keycap for about 0.13 seconds, and shows the accepted vending frame for about 0.24 seconds. Holding SPACE does not repeat the event.
- The shop purchase interface is not implemented; no placeholder product screen is opened.
- The water SPACE prompt appears within 2.5 tiles of the dispenser for a living, unpaused player, regardless of facing or obstruction. It is centered over the dispenser; the dispenser action requires being within 2 tiles and a clear approach.
- Pressing SPACE once at the dispenser restores the living player's HP to maximum. Holding SPACE does not repeat the interaction, and healing cannot revive a dead player.

## HP HUD · 8칸 가방 [확정/임시]
- 플레이어 최대 HP 100(확정). HUD 채움 폭 = round(240 * clamp(hp,0,100)/100). 색: hp>50 healthy, 25<hp<=50 warning, hp<=25 critical. 숫자는 현재 HP(= /100). HP는 실제 Player.hp()와 즉시 동기화.
- 가방 8칸, 한 물품 한 칸. 가득 차면(8/8) 추가 회수 거부(물품은 월드에 남김)와 "가방이 가득 찼습니다" 알림. (D 교체·바닥 드롭 규칙은 후속 루트 작업에서 구현)
- 사망 시 물품 처리 규칙은 미정(임의 적용하지 않음). 본부 인계는 기존 경제 시스템 연결 지점(Backpack.handOver)만 둠.

## 벽면 가구 방향·충돌 [확정]
- 방향별 뷰: 북벽=front(아래=방안), 남벽=back(위=방안), 왼쪽벽=left_side(오른쪽=방안), 오른쪽벽=right_side(왼쪽=방안). 정면 회전/반전으로 뒷모습을 대체하지 않는다(v8 전용 back 에셋 사용).
- 가구 충돌 = 실제 불투명 아트의 바닥 점유 영역만. 정면/뒷모습은 바닥 접촉 띠(WALL_FURNITURE_FOOTPRINT=0.7타일)만, 측면은 불투명 사각형. PNG 투명 여백·높은 입면 상단은 충돌로 잡지 않는다. 배치/렌더/충돌/길찾기는 동일 prop 데이터(LaboratoryProp)를 공유한다.

## 외벽·대문 V3 [확정]
- 외벽 두께 2타일(128px), 대문 표시 8×4타일(512×256). 좌표 ty=0=남. 외벽은 직선(길이만 신축)+바깥 모서리로 조립, 남쪽은 대문 폭만큼 분리해 실제 개구부.
- 대문 닫힘=중앙 통로(x62~65,y0~2) 충돌 차단, 열림=통로 통행 가능. 문틀(외벽 WALL)은 항상 충돌. SPACE로 대문 근처에서 토글. 열린 대문을 남쪽으로 넘으면 본부 복귀.
- 충돌은 PNG 사각형이 아니라 논리 벽(WALL)·대문 통로 상태로 처리. 변경은 isSolid/blocksActorAt을 통해 이동·몬스터·F경로·미니맵에 자동 전파.

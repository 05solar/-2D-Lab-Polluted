# TEST_PLAN

각 기능에 대해 조건(given) · 행동(when) · 예상 결과(then)로 기록한다.
계산·상태 전환은 LibGDX 화면 없이 순수 Java로 테스트한다. 시간은 주입 가능한 delta/GameTime,
랜덤은 seed/공급자 주입으로 재현 가능하게 한다.

## 현재 상태
- 1단계 + 배경 다양화 완료. 단위 테스트 **19개 통과** (`./gradlew.bat core:test`).
  - `movement/RunStartDetectorTest` (4): 달리기 시작 1회성/재시작 2회성.
  - `movement/MovementSystemTest` (3): 이동 거리=speed*delta(프레임 독립), 뛰기>걷기, 벽 비통과.
  - `input/InputStateTest` (2): pressed/held/released 엣지.
  - `world/LaboratoryRoomTest` (3): 테두리/범위밖 고체, 스폰 바닥, 크기.
  - `world/LaboratoryLayoutTest` (7): 아래 배경 다양화 시나리오.
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

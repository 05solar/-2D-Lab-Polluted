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

## 사망·보상 [확정]
- 사망 시 이번 탐사의 **미반납** 보물 보상은 제거된다.
- 이미 반납 완료한 보상은 다음 탐사에도 유지된다.

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

## 임시 본부 천막 [기본 맵 구현 / 임시 전환 시간]

- 본부는 연구소와 분리된 안전 구역이며 몬스터를 생성하지 않는다. 외벽, 모래주머니, 큰 가구의 바닥 접촉부는 충돌한다. 열린 남쪽 천막 출입구와 매트는 통과할 수 있다.
- 남쪽 중앙의 문을 지나 바깥 출구 1칸으로 **남쪽 이동 중 발 충돌 상자가 완전히 넘어가는 순간** 연구소 이동을 1회 요청한다. 접근이나 트리거 내부 대기는 발동하지 않는다. 스폰은 문보다 두 칸 이상 안쪽이다.
- 전환은 입력을 잠그고 페이드아웃 0.35초(임시값) 후 안전한 연구소 입구에 배치한 뒤 페이드인 0.35초(임시값) 후 해제한다. HP, 금액, 인벤토리, 진행 플래그를 유지한다.
- 자판기 앞 1.25타일 이내에서만 상호작용 가능 상태를 강조한다. `E` 구매 동작과 UI는 후속 구현이며, 현재 재화를 차감하지 않는다. 의료·정비 가구의 상호작용 위치만 예약했다.
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
- A game-over screen and revival are not implemented in this pass.

## Headquarters vending interaction

- The safe headquarters is a 14x10 map, fixed in view with a centered camera. Its only exit remains the south tent opening.
- The vending SPACE prompt appears within 2.5 tiles of the machine for a living, unpaused player, regardless of facing. The vending action itself still requires its front approach and a clear path.
- A new SPACE press emits one vending interaction event, flashes the pressed keycap for about 0.13 seconds, and shows the accepted vending frame for about 0.24 seconds. Holding SPACE does not repeat the event.
- The shop purchase interface is not implemented; no placeholder product screen is opened.
- The water SPACE prompt appears within 2.5 tiles of the dispenser for a living, unpaused player, regardless of facing or obstruction. It is centered over the dispenser; the dispenser action requires being within 2 tiles and a clear approach.
- Pressing SPACE once at the dispenser restores the living player's HP to maximum. Holding SPACE does not repeat the interaction, and healing cannot revive a dead player.

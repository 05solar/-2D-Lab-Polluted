# PROGRESS — 2D-Lab-Polluted 전체 진행내역

> 폐쇄 오염 연구소 탐사 2D 액션 게임. LibGDX 1.14.2 / Java 8 / Gradle 멀티모듈(core + lwjgl3),
> gdx-liftoff 기반, 패키지 `io.github.some_example_name`. 원격: github.com/05solar/-2D-Lab-Polluted (main).
> 커밋 저자: lotus05f@gmail.com (84ec3e4부터 Claude 공동 서명 포함).

이 문서는 지금까지의 작업을 한눈에 보는 요약이다. 세부 작업 로그는 `docs/process.md`,
설계/규칙/테스트는 `ARCHITECTURE.md`·`GAME_RULES.md`·`TEST_PLAN.md`·`ROADMAP.md` 참고.

---

## 1. 커밋 타임라인 (최신 → 최초)

| 커밋 | 내용 |
|------|------|
| `84ec3e4` | **대형 통합 커밋(2026-10-21)** — 기능 6종: ① **영구 강화 트리**(hq_upgrade_tree_v1 — 체력/공격/스킬 17노드, 카탈로그 JSON 단일 출처, 파생 능력치 멱등 재계산) ② **공통 스킬 시스템**(player_skill_loadout_v1 — 소유/장착/키/쿨다운 분리, SkillService 파이프라인, 퀵슬롯 HUD 5칸, 스킬창, 돌진 이관, H 귀환 토글) ③ **추가 스킬 3종**(player_three_skills_v1 — 충격파/전기 투척/보호막, skills.json 단일 출처, 최대 5슬롯) + 본부 스킬 테이블·강화 작업대 분리 ④ **난이도별 시드 기반 연구소 생성**(laboratory_generation_v1 — 구조 틀 12종·테마 16종, GenRng golden vector, BFS 접근성+폴백, 100시드×3 전수 검증) ⑤ **보스 1-1**(boss_stage_1_1_v1 — 메인 컴퓨터→선택창→광장→원심 배양체 C-01→LEGENDARY 보상 원자 커밋→복귀) ⑥ **9단계 보스전**(boss_nine_stages_v1 — 1-1 레거시 보존, 1-2~2-3 데이터 주도 범용 엔진 BossBattle·패턴 kind 12종·투기장 8종·3×3 실루엣 선택창·스킬 수치 불변 계약). 245 테스트+실행 캡처+문서 갱신 |
| `74d6f26` | **본부 보관함·선택 판매·영속 저장**(hq_sales_storage_v1): 귀환 이관(중복 방지)·판매창·원자 저장+백업+마이그레이션 |
| `751b74e` | **Mona 픽셀 폰트 전역 적용**·연타음 V4·난이도 모달 중앙 정렬·귀환 발자국 버그 수정 |
| `b9f3c9c` | 몬스터 감속 + 개봉음 V5→V7 + 획득음 음량 보정 |
| `82940e0` | **몬스터 행동 상태 기계 개편**(IDLE/WANDER/CHASE/ATTACK/SEARCH/RETURN)·키 정리·획득 토스트·효과음 개선 |
| `f0b935d` | 오디오 토글 개선: HP 바 오른쪽·64px·효과음+배경음악 세트 전환 |
| `c7e4259` | 효과음 켜기/끄기 토글 버튼(sfx_toggle_ui_v1) |
| `b675236` | 몬스터 이동 개편(리시+로밍) · 사망 시 소지품 상실+본부 재소환 · 난이도 모달 정리 |
| `71be182` | 컨테이너 효과음 V2 + 배경음악 무한 반복 |
| `78c1dc4` | **전투·전리품 업데이트 V1**: 컨테이너 배치 다양화·F 돌진·처치 회복·EASY 8배·효과음 9종 |
| `68a9323` | **V4 벽·문 전면 통일** + 방 26개 확장·컨테이너 연타 개봉 개편 |
| `a48d9fb` | docs: PROGRESS.md 추가 |
| `8575071` | **내부 직선 벽**도 새 금속 디자인(exterior V3)으로 교체(easy), 접합부·세로문은 v3 유지 |
| `3a727e8` | **연구소 외벽·대문 V3**(easy): 2타일 외벽 링 + 남쪽 8×4 대문(닫힘/열림·통로 충돌) |
| `284bd7f` | **미니맵 프레임** PNG 테두리 + **귀환 버튼** + 대문까지 **발자국 경로** PNG |
| `02c200b` | 벽면 가구 **뒷모습(남벽)** 연결 + 충돌을 바닥 footprint로(숨은 충돌·관통 제거) |
| `ecb38a4` | 네비게이션 WIP 스냅샷: 난이도 선택·대문·루트·가구·길찾기 자산/코드 대량 |
| `d2ec6e5` | **HP HUD + 8칸 가방 UI** 적용 |
| `2dc6e7c`·`8ffa8f9`·`661ae06` | 본부 물통 SPACE 프롬프트 위치 미세 조정(3회) |
| `111eb96` | 전투·몬스터·사망연출·본부 맵·맵전환·상호작용 WIP 스냅샷 |
| `823caad`·`526bae5` | 벽 투명부 검은 빈 공간 제거 + F2 타일 디버그 |
| `605e3a3` | **Laboratory Tileset V2** 전면 적용(바닥24/벽16/구조물16/오버레이24) |
| `632f1c2` | 연구소 맵 자연스럽게 재구성(네 구역·유기적 위험·가짜 가구 제거) |
| `f87d025` | 게임 창 640×480 → 1280×960 |
| `f8761c1` | 디버그 충돌 테두리 기본 off |
| `0c14e48` | 최초 커밋: 1단계 기본 플레이 공간 |

---

## 2. 현재 구현 상태 (기능별)

### 코어 플레이 (완료·안정)
- 본부(HQ 텐트, 14×10)에서 시작. **조작(2026-10-20 공통 스킬 반영)**: 이동 WASD/방향키 · 달리기 Shift ·
  공격 마우스 클릭 전용 · **스킬 슬롯 키 F/Q/1/2/3(기본값, 스킬창에서 변경 가능·예약 키 제외)** ·
  가방 R · 상호작용(문/상자/수집/자판기/강화대) Space 전용 · **귀환 경로 토글 H 재추가**(+미니맵 아래
  버튼) · 일시정지 Esc(스킬창→가방→일시정지 순).
- **배경음악**(2026-10-15): `assets/music/background_music.ogg` 무한 반복 — 본부·연구소·전환과 무관하게 연속
  (음량 `musicVolume=0.45`, 무음 실행 `LAB_MUTE_MUSIC=1`).
- **좌상단 HP HUD**(프레임+폭 가변 채움+숫자, 색 임계 >50/>25/≤25) — 본부·연구소 양쪽 항상 표시.
- **R 8칸 가방**: 열기/닫기, 중앙 패널+딤, 가득 차면 알림, D로 최저가치 교체.
- 본부 남쪽 대문 넘기 → **난이도 선택 모달(쉬움/보통/어려움)** → 0.35s 페이드 → 연구소 입장.
- 연구소 → 남쪽 대문 → 본부 복귀. `PlayerSessionState`로 HP·재화·인벤토리·진행 맵 간 유지.

### 전투·몬스터 (2026-10-15 확장)
- 몬스터 4종(슬라임20·연구원40·경비원60·팀장70), 시드 기반 배치, HP/사망/넉백.
- **이동 AI(2026-10-16 상태 기계 개편)**: IDLE↔WANDER→CHASE→ATTACK/SEARCH→RETURN.
  발견=거리(종류별 4/5/7)+**시야**(벽·닫힌 문 차단, `sightBlocked`), 해제=8타일/시야 상실 2s/홈 12타일
  이탈(히스테리시스), 해제 후 마지막 목격 지점 2~4s 탐색 → 복귀(걸어서). 종류별 배회: 슬라임=짧은 이동·
  잦은 멈춤, 경비원=순찰 지점 2~3개 순환, 연구원=장비 주변 배회·긴 대기, 팀장=착석·소음 2회 분노(해제 시
  구석 복귀+분노·소음 초기화). 약한 분리로 겹침 완화, 감지·경로 재평가 0.15s 분산(56마리 ~59fps).
  F1 디버그에 상태·목표·발견/해제 반경 표시.
- **EASY 생성 수 = 기준(3/2/1/1)×8 = 24/16/8/8**(`BalanceConfig` 기준값 보존, 재적용 멱등). NORMAL/HARD 유지.
  스폰은 문/가구/상자/대문/입구 안전영역 회피, 3×3 여유 부족 시 예비 후보 분산. 56마리 avg 16.7ms(~60fps).
- 플레이어 근접공격(히트프레임, 피해15)·무적시간·HP바. 팀장 달리기-소음 2회 분노.
- **처치당 HP 20 회복**(최대 100, 사망 전환 1회, 돌진/공격 공용 `applyPlayerStrike`).
- **돌진(기본 F)**: 3타일/0.18s/쿨다운 3s, 엣지 입력, 0.3타일 하위 단계(관통·건너뜀 없음), 대상당 1회
  피해 15, 무적 없음, 달리기 소음과 분리. **2026-10-20 공통 스킬 경로로 이관** — 쿨다운은
  `SkillRuntime("dash")` 단일 관리, HUD는 하단 중앙 퀵슬롯 바(우하단 전투 HUD는 공격 전용).
- **효과음**: 타격(실피해만, 2종 교대)·피격(무적 거절 제외)·사망(전환 1회)·공격 스윙·돌진·문 열림(combat_loot_update_v1).
- 플레이어 **사망 연출**(8프레임 방향 시트, 사망 후 입력 차단) → **(2026-10-15) 사망 2초 뒤 소지품 전부
  상실 + 본부 재소환·부활(HP 최대)**. 재화·진행 플래그는 유지. 별도 게임오버 화면은 미구현.

### 루트·상자 (완료 — 2026-10-14 연타 개봉, 2026-10-15 배치·연출 개편)
- `assets/loot/loot_tables.json` 단일 진실(가격·확률·등급 유지). 나무상자=COMMON·일반캐비넷=EPIC·고급캐비넷=LEGENDARY.
- 개수 설정화(`BalanceConfig`): easy 14/7/2=23(26방 대부분 ≥1, 도서관·화학실 복수), normal 6, hard 8. 캐비넷 1.4배 표시(동일 하단-중앙 기준점) + 같은 배율의 바닥 점유/충돌.
- **배치 다양화(2026-10-15)**: 방 중앙 고정점 제거 → 벽면45/구석25/가구옆20/중앙10 가중 추첨(빈 분류 재배분,
  시드 고정). 문 주변 2칸·가구·상호 2.2 간격 회피, 모든 컨테이너에 서서 SPACE 가능한 접근 지점 보장,
  캐비넷은 북쪽 벽 수납만(정면 아트 규칙). normal/hard 구획 중앙점도 동일 교체.
- **SPACE 연타 개봉**: 나무 1회/일반 5회/고급 8회. 엣지 입력만 1회, 진행도 컨테이너별 저장, 바라보는 가장 가까운 1개만 대상. 도달 입력에서 1회만 개봉+추첨. **안내는 측정된 불투명 상단+화면 6px**에 근접 표시(+카운트, 개봉 후 숨김), 등급별 발광(보라/금/파랑).
- **아이템 공개 연출**: 등급 테두리 아이콘 2배(1.12)가 상자 위로 짧게 상승 후 정지(월드 효과 레이어). 실제 드롭은 접근 가능한 바닥 지점에 분리 생성.
- **효과음(V2)**: 연타=경쾌한 걸쇠 '탁!'(`container_sfx_v2/container_press_v2`), 개봉=팝+삐걱+상승 차임(`container_open_v2`).
- 바닥 드롭→SPACE 수집(별도 입력), 가득 차면 D 가치 교체.

### 9단계 보스전 (2026-10-21, boss_nine_stages_v1)
- 단계 사슬 1-1 폐기물 변이체(레거시 C-01 보존) → 1-2 냉각관 거수 → 1-3 배양낭 군체 →
  1-4 보안 제압 기체 → 1-5 신경 접속 표본 → 1-보스 중앙 격리실 융합체 → 2-1 저온실 다족 →
  2-2 변이 식물 → 2-3 이동형 배양 탱크. 순차 해금, 단계 ID/저장 키 동일(기존 세이브 그대로).
- 1-2 이후는 데이터 주도 범용 엔진 `BossBattle`(Screen 복사·stageId switch 없음) —
  stage_catalog/patterns/arena_layouts *.runtime.json 유일 출처. 패턴 kind 12종 실행기
  (부채꼴·장판·부채 투사체·회전 레이저·확장 고리·지연 폭발·이동 포격·연속 돌진·뿌리 구속·
  누출 흔적·분노 전환·복합 큐), 예고=판정 기하·프레임 독립 경계·사망 시 전체 취소·
  boss_toxin 공유 틱. 보스 크기: 그리기 8×8(1-보스 9.5)·피격 박스·footprint 분리 데이터,
  정면 시트 세그먼트 재생(몸체 회전 금지). 플레이어 스킬 수치 불변(기준 회귀 테스트).
- 선택창 3×3 실루엣 카드(잠김에도 표시·contain-fit)+배지+방향키/Enter+컬러 초상 상세.
  투기장 36×28/44×34/52×40(BFS 도달성 테스트). 둔화/구속 상태 이상(이동만 제한).
  245 테스트+선택창/1-2/1-4/2-2/1-보스 실행 캡처 검증. 신규 수치는 초안(조정 전제).

### 보스 모드 1-1 (2026-10-20, boss_stage_1_1_v1)
- 본부 메인 컴퓨터 SPACE → 단계 선택창(1-1만 구현·잠김/준비 중 구분) → 32×24 광장(기둥4·방벽2·
  남쪽 겸용 문) → 원심 배양체 C-01(HP 600, boss_1_1.json 단일 출처) → 보상 → 본부 복귀.
- 패턴 3종(투척 ±18° 3발/오염 장판 2.2타일·최대 2·낙하식/돌진 8타일·기둥 정지), 상태 기계
  ENTERING→…→CLEARED/FAILED(동시 사망 실패 우선, 사망 시 잔여 판정 취소), 기둥 차단 정책
  패턴별 분리. 플레이어 전투/스킬은 hurtbox 프록시로 기존 공통 경로 재사용(넉백/경직 면역,
  전기 피해 허용·감전 표시만). 보상 = LEGENDARY 1개(runId 시드 고정·본부 보관함 지급) +
  1-2 해금 + 영수증 원자 저장(1run 1보상·중복 멱등·실패 재시도). 222 테스트+실행 캡처 검증.

### 난이도별 시드 기반 연구소 생성 (2026-10-20, laboratory_generation_v1)
- 난이도 확정마다 64비트 시드로 생성: 쉬움 128×96·16방(경비원/팀장 없음·몬스터 56) /
  보통 160×120·25방(88) / 어려움 192×144·36방(126). 구조 틀 12종, 테마 16종 전부 등장,
  기존 쉬움의 V4 벽/문·방향별 가구·컨테이너 가중 배치·외벽 링+남쪽 대문을 전 난이도 공통 적용.
- GenRng(FNV-1a32+XorShift32, golden vector 일치) 하위 스트림 분리, 접근성 배우 그리드 BFS,
  실패 슬롯 수리→재시도 3회→검증된 폴백(쉬움=수제 캠퍼스), contentHash·메타데이터 로그.
- 난이도별 100시드 자동 검증(0 재시도/0 폴백), LAB_SEED/LAB_CLASSIC_MAP 디버그.
  수제 쉬움 맵은 회귀 기준·폴백으로 보존. 진행 중 탐사 저장은 미지원(명시).

### 맵·타일·벽·대문 (전 난이도 V4 통일)
- **바닥·오버레이**: Laboratory Tileset V2(바닥 변형 seed·오버레이). 벽 투명부 아래 바닥 보장(검은 void 제거).
- **벽·문 = V4 단일 규격**(`laboratory_structure_v4`): 하나의 논리 점유 그리드에서 8비트 마스크(N=1…NW=128,
  대각선은 양옆 직교 이웃 모두 벽일 때만)로 47종 아틀라스 타일을 고른다(`WallMaskV4`+`LaboratoryStructureRendererV4`).
  직선/양면/바깥·안쪽 코너/T/십자/끝단/채움이 같은 체계. 구형 V2/V3 접합부 스킨·InteriorWall 덧그림·Exterior 링
  드로우는 제거(보라 접합부 없음).
- **내부 방문**: V4 가로(door_horizontal_*)·세로(door_vertical_*, 전용 탑뷰) 닫힘/열림. `Door` 객체가 시각·충돌·
  토글을 함께 결정(열린 통로 통행, 닫힌 문짝 추가 충돌). 문틀은 양옆 벽과 시각 연결.
- **외벽**: easy는 2타일 두께를 실제 벽 셀 두 줄로 구성(V4가 그림, `forcedWall` 마스크). 남쪽 8×4 **대문은 기존 V3 유지**
  (닫힘/열림·통로 충돌·SPACE 토글·본부 복귀). normal/hard는 1타일 테두리.
- 쉬움 맵: 128×96, 간격 압축(.55) 캠퍼스 — 테마 방 16 + 보조실 10 = **26방**(경비초소↔입구·전기실↔경비초소·창고↔휴게실 직접 연결 포함). 외곽 순환 링+십자+1타일 지름길로 문간 평균 66.5→52.0(-21.8%, 맨해튼 하한 41.8). 보통/어려움: 3×3 구획 9방(+50%), V4 문 12개. F1 충돌 디버그·F2 타일 디버그·**F12 프레임 저장**.
- 벽면 가구: 북=정면·남=뒷모습(v8)·좌우=측면, 충돌은 실제 바닥 footprint.

### 네비게이션 (완료 — 2026-10-15 단축키 F→H)
- 우상단 **미니맵**(프레임 PNG 테두리, 방·플레이어·대문·팀장 마커).
- 미니맵 아래 **귀환 버튼**(클릭/**H**, 이전 F — F는 돌진 전용) → `RoomPathfinder`로 대문까지 경로 → **발자국 PNG** 바닥 표시.

### 상호작용·본부 경제 (2026-10-20 판매 구현)
- 방문 SPACE 개폐(충돌 동기화), 대문 SPACE 토글, 상자/드롭 수집 — 한 키 중복 실행 방지.
- 본부 **자판기 = 판매 상점**: SPACE로 판매창(hq_sales_storage_v1 UI). 보관함 스크롤 목록·수량 ±·
  전체 선택/해제·선택/전체 판매(확인창)·Esc/X 닫기. 열려 있는 동안 월드 입력 차단. 정수기(HP 완전 회복).
- **귀환 이관**: 성공 귀환(사망 제외) 1회에 가방→보관함, 탐사 UUID 중복 방지, 저장 성공 후 가방 비움.
- **영속 프로필**: `~/.2d-lab-polluted/profile.json`(+백업) — 재화(long 원)·보관함 인스턴스·강화 노드·
  장착 스킬·기록. 원자 교체 저장, 손상 시 백업 복구, 마이그레이션. 변경 시에만 저장.
- **본부 강화대(2026-10-20, 같은 날 개편)**: SPACE → 강화 트리(1120×720 패널, 잠김/가능/부족/완료 4상태,
  직각 연결선, 상세 패널 구매). 체력 +20/+30/+50, 공격 +3/+4/+6, 회복 해금(Q, 30/10s)·회복량 +10·슬롯 +1.
  **상호작용 대상은 접이식 작업대(망치, 중앙 책상 왼쪽 4.35/7.45)** — 금속 테이블(중앙 책상 오른쪽
  8.75/7.45)은 스킬 테이블로 전환(SPACE → 스킬창, 프롬프트 "스킬 등록"). 중앙 책상 양옆 콘솔과 두
  테이블의 위치를 교환했고 서쪽 침대는 제거.
  파생 능력치는 기준값+구매 합으로 멱등 재계산(`applyUpgradeEffects`), HP HUD는 cur/max·비율 색 임계.
- **추가 스킬 3종(2026-10-20, player_three_skills_v1)**: 충격파(30dmg/2.5타일 파면/밀치기 0.8/쿨 6s —
  원점 고정·annulus 1회 명중·차폐), 전기 투척(25dmg/10타일/s/8타일/감전 0.5s/쿨 5s — 방향 고정·
  swept 충돌·1충돌 소멸), 보호막(흡수 60/4s/쿨 12s — 공통 피해 파이프라인 흡수·파괴/만료 구분·중복
  시전 거부). 수치 유일 출처 data/skills.json, 강화 트리 17노드(해금 3+강화 3+슬롯 IV/V — 최대 5슬롯),
  VFX 6종(파면=visualExtent 보정, 레이어 분리), 기본 키 1/2/3 슬롯. 202 테스트 + 실행 검증 완료.
- **공통 스킬 시스템(2026-10-20, player_skill_loadout_v1)**: 소유/장착(slotId→skillId)/키(slotId→keyCode)/
  쿨다운(skillId) 분리 + `SkillService` 공통 실행 파이프라인(실패 = 이유만, 상태 불변). 돌진·회복 공통
  경로, 회복 캐스트 시트 연출(12프레임 1회, 발 앵커, 알파 0.7), 하단 중앙 퀵슬롯 HUD 5칸(실제 키 이름·
  미등록·쿨다운 올림 초·잠김 구분), 스킬창(장착/해제/키 등록 — 다음 키 대기·Esc 취소·예약/중복 거부,
  오디오 토글 오른쪽 버튼, 본부·연구소 공통). 기본 활성 슬롯 1→2(구매 +1 보존, 최대 5), 구성·쿨다운
  스냅샷은 기존 원자적 프로필 저장으로 영속(오프라인 경과 없음), 구 equippedSkills 마이그레이션.

---

## 3. 아키텍처 요약 (주요 패키지)

```
world/     Laboratory*(맵·타일·방·레이아웃) + Headquarters* + Door/DoorState/DoorPlacementResolver
           + MapId/MapTransitionController/PlayerSessionState + GameDifficulty + RoomPathfinder
           + Loot*(Container/Placement/Drop) + Laboratory(Prop/RoomArea/FurnitureCoverage)
           + WallMaskV4(8비트→47종) + WallAutotiler(구형, 테스트 맵 전용)
           + LaboratoryGenerator·GenRng·LabGenData(난이도별 시드 생성 — 틀 12·테마 16·BFS·폴백)
entity/    player/(Player·PlayerState·PlayerDeathAnimation) + monster/
combat/    CombatSystem·MonsterAiSystem·PlayerCombatState·CombatCollisionGrid·MonsterSpawnSystem
           + PlayerDashState·ShockwaveState·ElectricProjectile·PlayerShieldState·SkillCast(스킬 판정)
boss/      BossStageCatalog·BossPatternCatalog·BossArenaCatalog(*.runtime.json 파서)
           + BossDefinition·BossFight(1-1 레거시, 회귀 고정) + BossBattle(9단계 범용 패턴 엔진)
           — 전부 순수 Java(Context 주입, GL 비의존)
inventory/ Backpack(8칸·가치교체) + LootItem + LootTableCatalog
interaction/ VendingMachineInteraction
render/    World·Entity·Monster·Combat·Headquarters·Fade·TileDebug·Debug
           + PlayerHud·Navigation·DifficultyModal·ExteriorWall(대문 전용)
           + LaboratoryStructureRendererV4(벽·문) + FrameCapture(F12 프레임 저장)
           + SalePanel·UpgradeTree·SkillLoadout(패널 3종) + SkillBar(퀵슬롯 HUD)·HealCast(회복 연출)
           + SkillVfxRenderer(스킬 3종 VFX) + CombatHud(공격 전용)·SfxToggle·SkillMenuButton
           + BossHud(보스 HP바)·BossStageSelect(3×3 실루엣 선택창)·BossResult(결과창)
           + KoreanLabelCache(AWT 한글 라벨) + InteriorWallRenderer(구형, 미사용)
asset/     GameAssets(텍스처 단독 소유·해제) + Headquarters·PlayerHud·RoomKit·LaboratoryFurniture
           + Loot·DifficultyModal·ExteriorWall · LaboratoryStructureV4Assets
           + CombatSound·CombatUi·HqSales·Upgrade·Skill·ThreeSkills
           + BossAssets(1-1 레거시)·BossNineAssets(9단계 시트/VFX/초상/카드)
           · HudLayout · LaboratoryTileCatalogV2/TileSetV2
screen/    LaboratoryScreen + HeadquartersScreen + BossArenaScreen(레거시/범용 분기 1곳) (+ BaseScreen)
profile/   PlayerProfile·ItemInstance·ProfileCodec·ProfileStore/FileProfileStore(원자 저장+백업)
           + HeadquartersEconomy(이관·판매·강화·보스 클리어 트랜잭션) + SaleSelection
           + UpgradeCatalog/UpgradeEffects + SkillCatalog(skills.json)
           + SkillLoadout(장착·키)·SkillRuntime(쿨다운)·SkillService(공통 실행) — 전부 순수 Java
config/    AssetPaths · BalanceConfig
```
- 렌더·충돌·길찾기·미니맵은 모두 같은 논리 벽(`isSolid`/`blocksActorAt`)과 prop 데이터를 공유 → 시각 변경이 충돌로 새지 않음.
- 텍스처 생명주기는 GameAssets 한 곳. UI 에셋도 Nearest 필터.

### 설치된 에셋 팩 (`assets/`)
`laboratory_tiles_v2`(바닥/오버레이), `laboratory_structure_v4`(벽 47종 아틀라스+가로/세로 방문), `hq_tent`,
`monsters`, `laboratory_roomkits_v3`, `laboratory_orthographic_views_v7`,
`laboratory_orthographic_views_v8`(가구 뒷모습), `loot`, `difficulty_modal_v2`,
`ui/player_hud_inventory_v1`, `ui/navigation`, `exterior_walls_gate_v3`(대문), `textures/player`, `data`,
`combat_loot_update_v1`(돌진/공격 HUD UI 4종 + 전투·전리품 효과음 9종, CC0 — `sounds/` 구 2종 대체),
`container_sfx_v2`(경쾌한 상자 걸쇠/개봉 차임 — 팩 press/open 대체), `music`(배경음악 OGG, 무한 반복),
`sfx_toggle_ui_v1`(효과음 켜기/끄기 버튼 — 본부·연구소 공유 설정, 배경음악과 독립),
`fonts/mona`(Mona12TextKR 픽셀 폰트, OFL 1.1 — gdx-freetype BitmapFont + AWT 한글 라벨 전역 적용),
`ui/hq_sales_storage_v1`(판매창), `ui/hq_upgrade_tree_v1`(강화 트리 + `data/upgrade_tree.json` 수치 유일 출처),
`ui/player_skill_loadout_v1`(회복 캐스트 시트·아이콘·슬롯/키캡/패널 — 퀵슬롯 HUD·스킬창),
`ui/player_three_skills_v1`(충격파/전기/보호막 아이콘 3종+VFX 시트 6종+`data/skills.json` 수치 유일 출처),
`laboratory_generation_v1`(구조 틀 12종·난이도 프로필·테마 — 시드 생성 데이터),
`boss_stage_1_1_v1`(보스 1-1 — `data/boss_1_1.json` 수치 유일 출처, 레거시 시트/광장/UI),
`boss_nine_stages_v1`(9단계 — `data/*.runtime.json` 채택본이 수치 유일 출처, 보스 시트 8종
1024²·VFX 스트립 12종·초상 18종·선택창 카드/배지; draft/catalog 원본은 비교용).

### V4 파일 ↔ 사용 매핑
- `laboratory_structure_v4/runtime/walls/walls_blob47_atlas_128.png` → `LaboratoryStructureV4Assets.wall(index)`,
  index는 `WallMaskV4.atlasIndex(rawMask)`(8열×6행, 128px).
- `runtime/doors/door_horizontal_closed|open.png`(256×96) → `horizontalDoor(open)` — 벽이 동서, 남북 통행.
- `runtime/doors/door_vertical_closed|open.png`(96×256) → `verticalDoor(open)` — 벽이 남북, 동서 통행.
- 방향: 월드 북(y+1)=이미지 위. 가로벽 셀=HORIZONTAL door, 세로벽 셀=VERTICAL door(`Door.Orientation`).

---

## 4. 남은 작업 / 미해결

- ~~벽/문 통일~~ / ~~방·복도·컨테이너 개편~~ / ~~본부 경제(판매)~~ / ~~영구 강화 트리~~ /
  ~~게임오버·부활 플로우~~ / ~~공통 스킬 시스템~~ / ~~추가 스킬 3종~~ **완료** —
  세부는 §1 타임라인·§2 기능 상태 참고.
- ~~난이도별 연구소 품질 격차(normal/hard 3×3 구획)~~ **완료(2026-10-20)**: 시드 기반 생성이
  전 난이도에 쉬움 품질 파이프라인 적용. 수제 쉬움은 회귀 기준·폴백으로 보존.
- **진행 중 탐사 저장/복원**: 미지원(앱 종료 시 탐사 소멸 — 본부 영속 프로필만 보존).
  같은 세션 안에서는 맵·열린 상자 상태가 유지된다.
- **아이템 구매 상점**: 자판기는 판매 전용 — 구매 UI 미구현.
- **게임오버 화면 UI**: 재소환 플로우는 완료, 별도 연출 화면은 미구현.
- **스킬 3종·보스 효과음**: player_three_skills_v1/boss_nine_stages_v1 팩에 사운드 미포함 —
  시전/착탄/파괴·보스 패턴 효과음 추가 여지(현재 기존 타격/피격음 재사용).
- **보스 신규 단계(1-2~2-3) 수동 밸런스 체감**: 수치는 초안(stage_catalog/patterns runtime) —
  실제 키보드 플레이로 회피 난이도·HP·피해 조정 필요(1-1은 회귀 고정이라 제외).
- **보스 투기장 시각 밀도**(2026-10-21 피드백 "허전함"): 바닥이 단일 타일 반복이라 빈 평면처럼
  보임 — 바닥 변형/데칼 시드 배치+단계 테마 색조, INTRO 보스 카메라 연출(또는 중간점 추적),
  보스 발밑 그림자/피격 플래시/카메라 흔들림 후보(로직 불변 — 렌더링만).
- hazard(독성/감전 타일) 실제 피해.
- 월드 아이템 획득: 상자/드롭 외 일반 바닥 아이템은 임시 디버그(G) 수준.
- 가구 배치 미세 조정(벽면 작업구역화·중앙 섬 묶기)과 생성 맵 수동 밸런스 체감(보통/어려움).
- **사용자 프로필 테스트 데이터**: 자동 검증이 실제 프로필(~/.2d-lab-polluted)에 스킬 해금·보스
  클리어(1-1/1-2/1-4/2-2/1-보스)·전설 보상을 기록함 — 깨끗한 진행을 원하면 초기화 필요(보류 중).

## 5. 검증·환경 메모

- 매 작업: `./gradlew.bat core:test` + `lwjgl3:build` 통과. 현재 **245 tests 0 fail**, 빌드 성공(2026-10-21).
- 2026-10-20 실행 검증: 생성 맵 3난이도 조감+실내 캡처(gen_*.png), 동일 시드 contentHash 일치,
  스킬 3종 판정 로그(충격파 60→30·전기 60→35+감전·보호막 흡수/파괴), 스킬창/강화 트리 17노드 캡처.
- 2026-10-21 실행 검증(9단계 보스): 선택창 3×3 실루엣/잠김 상세 캡처, 기존 프로필 호환 로그
  (unlocked=[1-2] cleared=[1-1] — 마이그레이션 불필요), 1-2(빙결 장판)/1-4(거대 기체+회전 레이저)/
  2-2(식물 보스)/1-보스(9.5타일+장판 예고)/1-1(레거시 회귀) 자동 토벌 — 전부 clear committed OK·
  대문 개방·본부 복귀(boss*_pattern/result 캡처). 효과음 청취·키보드 실입력 체감은 사용자 핸즈온 권장.
- 개발 env: `LAB_PERF=1`(프레임 시간), `LAB_AUTO_DASH/LAB_AUTO_HEAL=1`(스킬 자동 검증),
  `LAB_AUTO_SKILL3=shockwave|electric|shield`(3종 스킬 검증), `LAB_EXIT_AFTER=N`(자동 종료),
  `LAB_TEST_SALE/LAB_TEST_UPGRADE/LAB_TEST_SKILLS=1`(본부 패널 스크립트),
  `LAB_HQ_TEST=VENDING|WATER|SKILL|BENCH|COMPUTER`(본부 스폰), `LAB_SEED=<64비트 정수>`(생성 시드 고정),
  `LAB_CLASSIC_MAP=1`(수제 맵 강제), `LAB_MUTE_MUSIC=1`(무음).
  보스: `LAB_START_BOSS=1`(직행)·`LAB_BOSS_STAGE=1-4`(단계 지정)·`LAB_BOSS_SEED`(패턴 시드)·
  `LAB_BOSS_AUTO=win|fail`(자동 검증)·`LAB_BOSS_UNLOCK_ALL=1`(전 단계 입장, 개발 전용)·
  `LAB_TEST_BOSSPANEL=1`(선택창 캡처 스크립트).
  ※ PowerShell 환경변수는 호출 간 비persist — 실행 명령과 같은 줄에서 설정할 것.
- **실제 GL 렌더 프레임 저장**(합성 미리보기 아님): `render/FrameCapture`가 백버퍼→PNG. F12 키 또는
  `LAB_AUTO_CAPTURE=1`(30프레임 UI, 42프레임 F1 충돌, 46프레임 자동 종료). 저장: `docs/images/frames/`.
  - 재현: `LAB_START_LAB=1 LAB_AUTO_CAPTURE=1 LAB_MAP_OVERVIEW=1 LAB_SEED=424242 LAB_EXIT_AFTER=40 ./gradlew.bat lwjgl3:run`
  - 난이도: `LAB_DIFFICULTY=EASY|NORMAL|HARD` · 방 지정: `LAB_START_ROOM=BIOLOGY`(생성 맵은
    biology_r07처럼 부분 일치) · 아틀라스 갤러리: `LAB_WALL_TEST=3`
  - 캡처 산출물: `frame_001_lab_w128h96_ui.png`(수제 easy 기준), 생성 맵 `gen_easy/normal/hard_overview.png`
    (128×96/160×120/192×144 조감)·`gen_easy_ingame.png`, `frame_001_lab_v4atlas_ui.png`(47종+문4종).
- 실행: 본부 시작 → 남쪽 대문 → 난이도 선택 → 연구소 탐색/전투/상자/가방 → 미니맵·F 발자국 → 대문으로 복귀.
- **사용자 핸즈온 확인 권장(백그라운드로 미검증)**: 3방 이동·내부 방문 닫힘→열림 토글·캐비넷 획득·F 귀환 경로,
  본부↔연구소 왕복(난이도 선택·페이드·HP HUD).

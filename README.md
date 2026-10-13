# 2D-Lab-Polluted

## Wall connection checks

In PowerShell, run `./gradlew.bat clean core:test lwjgl3:build`, then
`./gradlew.bat lwjgl3:run`. The desktop game stays open while the Gradle task runs.
Use `LAB_WALL_TEST=1` for the 9×9 wall shape gallery or `LAB_WALL_TEST=2` for
the 9×9 assembled horizontal and vertical door check:

```powershell
$env:LAB_WALL_TEST = '2'
./gradlew.bat lwjgl3:run
Remove-Item Env:LAB_WALL_TEST
```

F2 shows layer IDs, connection masks, door direction/state, connection points
(green matched, red unmatched, yellow door direction error, purple isolated),
and collision outlines. The default view has this overlay off. The connected
wall atlas can be regenerated from the preserved V2 atlases with
`javac -d build/tools tools/WallSeamAtlasGenerator.java` and
`java -cp build/tools WallSeamAtlasGenerator assets/laboratory_tiles_v2`.

폐쇄된 연구소를 탐사해 몬스터를 처치하거나 피하고, 보물을 임시 본부로 운반하는 2D 액션 탐사 게임.

## 사용 기술
- Java 8
- [libGDX](https://libgdx.com/) 1.14.2
- Gradle (wrapper 포함), [gdx-liftoff](https://github.com/libgdx/gdx-liftoff) 생성

## 요구 사항
- JDK 8 이상

## 모듈
- `core`: 플랫폼 무관 게임 로직 (규칙·화면·렌더링)
- `lwjgl3`: 데스크톱(LWJGL3) 실행 런처

## 실행 / 테스트
Windows PowerShell:
```
./gradlew.bat lwjgl3:run     # 데스크톱 실행
./gradlew.bat core:test      # core 단위 테스트
./gradlew.bat build          # 전체 빌드
```
macOS/Linux에서는 `./gradlew`를 사용한다.

게임 화면에서 F1은 충돌 영역을 표시한다. 연구소에서는 F2로 타일 레이어 ID와 셀 유형 테두리를 표시한다. 둘 다 기본값은 꺼짐이다.
일반 실행은 연구소와 분리된 14×10 임시 본부 천막에서 시작한다. 남쪽 게이트에서 난이도를 고르고 0.35초 페이드로 연구소에 들어간다. 연구소 남쪽 게이트를 통해 본부로 복귀할 수 있으며 HP·가방·재화 세션은 유지된다. 물통 SPACE 상호작용은 살아 있는 플레이어의 HP를 최대 회복한다. 자판기 구매 UI는 미구현이다.

본부와 연구소에서 R로 8칸 가방 UI를 열고 닫는다. 연구소 G는 가방 테스트용 개발 입력이다. F1은 충돌·상호작용 범위 디버그, 연구소 F2는 타일 레이어 디버그다.

연구소 맵은 쉬움 128×96(크기와 형태가 다른 16개 방), 보통 28×20, 어려움 38×28이다. F는 남쪽 복귀 게이트 최단 경로, E는 loot 상자/아이템 상호작용, 가방이 찬 상태의 D는 더 높은 가치 물품으로 최저 가치 슬롯을 교체한다.

`LAB_START_LAB=1`은 연구소 직접 시작, `LAB_WALL_TEST=1` 또는 `2`는 벽 테스트 맵 직접 시작이다. `LAB_HQ_TEST=VENDING` 및 `LAB_HQ_TEST=WATER`는 본부 상호작용 검증용 스폰이다.
타일 렌더링 검증은 `./gradlew.bat clean core:test lwjgl3:build` 후 `./gradlew.bat lwjgl3:run`으로 실행한다.
수정 후 실제 창 캡처는 `docs/images/map_black_voids_fixed.png`, `docs/images/map_black_voids_fixed_north.png`에 있다.

## 문서
- [AGENTS.md](AGENTS.md) — AI 작업 규칙
- [docs/GAME_DESIGN.md](docs/GAME_DESIGN.md) — 기획
- [docs/GAME_RULES.md](docs/GAME_RULES.md) — 확정 수치·판정
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — 구조·의존성
- [docs/ROADMAP.md](docs/ROADMAP.md) — 단계별 계획
- [docs/TEST_PLAN.md](docs/TEST_PLAN.md) — 테스트 시나리오
- [docs/CHANGELOG.md](docs/CHANGELOG.md) — 변경 이력

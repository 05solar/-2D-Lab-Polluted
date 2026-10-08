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
일반 실행은 독립된 20×15 본부 천막에서 시작한다. 남쪽 열린 출입구를 지나 바깥으로 걸으면 약 0.35초씩 페이드아웃·페이드인하며 연구소 남서쪽 출입문 바로 안쪽에 도착한다. 본부에는 몬스터가 없다. F1은 본부에서도 벽·가구·플레이어·출구·자판기 범위를 보여준다. 자판기 근처에서는 강조 표시가 나타나며 구매 UI와 회복 기능은 아직 없다.
`LAB_WALL_TEST=1` 또는 `2`는 연구소 벽 테스트 맵으로 직접 시작하는 개발용 설정이다. `LAB_HQ_TEST=VENDING`은 자판기 앞 시각 검증용 스폰이다.
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

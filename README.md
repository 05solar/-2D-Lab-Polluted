# 2D-Lab-Polluted

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

## 문서
- [AGENTS.md](AGENTS.md) — AI 작업 규칙
- [docs/GAME_DESIGN.md](docs/GAME_DESIGN.md) — 기획
- [docs/GAME_RULES.md](docs/GAME_RULES.md) — 확정 수치·판정
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — 구조·의존성
- [docs/ROADMAP.md](docs/ROADMAP.md) — 단계별 계획
- [docs/TEST_PLAN.md](docs/TEST_PLAN.md) — 테스트 시나리오
- [docs/CHANGELOG.md](docs/CHANGELOG.md) — 변경 이력

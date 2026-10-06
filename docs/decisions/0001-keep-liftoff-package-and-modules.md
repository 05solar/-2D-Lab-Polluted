# 0001 — gdx-liftoff 기본 패키지·모듈 구성 유지

## 상태
Accepted

## 배경
프로젝트 규칙서는 패키지/클래스 예시를 `com.example.game.*` 기준으로 기술한다.
그러나 실제 저장소는 gdx-liftoff가 생성한 `io.github.some_example_name` 패키지와
`core` + `lwjgl3` 2개 모듈(android 없음)로 구성되어 있다.

## 결정
- 기본 패키지명 `io.github.some_example_name`을 **변경하지 않는다.** 규칙서의
  `com.example.game`은 참고용 이름으로 간주한다.
- 모듈 구성(`core`, `lwjgl3`)을 유지한다. android 모듈은 필요해질 때 별도로 추가한다.
- 게임 루트 클래스 이름은 규칙서를 따라 `LaboratoryGame`으로 하되 위 패키지 아래에 둔다.

## 결과
- 규칙서 문서와 실제 코드의 패키지명이 다르다. 혼동을 막기 위해 `AGENTS.md`와
  `ARCHITECTURE.md`에 실제 패키지명을 명시했다.
- 리네임에 따른 광범위한 import 변경·빌드 설정 수정 리스크를 피한다.
- android 지원이 필요하면 ADR을 추가하고 `core`에 인터페이스, 플랫폼 모듈에 구현을 둔다.

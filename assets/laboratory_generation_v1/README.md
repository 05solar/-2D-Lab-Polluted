# 난이도별 랜덤 연구소 생성 데이터 에셋 v1

이번 팩은 기존 타일·벽·문·가구를 조립하는 **JSON 데이터 에셋**이다. 새 배경 그림이나 스프라이트는 추가하지 않았다. 실제 프로젝트 자산 매핑과 Java 생성기 연결이 필요하며 JSON을 복사하는 것만으로 현재 게임이 바뀌지는 않는다.

## 구성

| 파일/폴더 | 내용 |
|---|---|
| RANDOM_LAB_IMPLEMENTATION_PROMPT.md | 개발 AI에 전달할 상세 구현 지시문 |
| data/layouts/easy, normal, hard | 난이도별4개, 총12개 구조 틀 |
| data/room_themes.json | 연구/생활 공간16종의 기능별 가구 역할 |
| data/furniture_patterns.json | 벽면/작업대/보관 패턴3종, 방향 규칙과 footprint 근사치 |
| data/difficulty_profiles.json | 난이도별 규모·루트·몬스터 수량 초안 |
| data/asset_bindings.required.json | 실제 타일/가구 TextureRegion·앵커 매핑 목록 |
| data/generation_rules.json | 생성 순서·시드·재시도·접근성 정책 |
| tools/reference_generator.py | 표준 Python 참조 생성기·논리 검증 |
| tools/validate_pack.py | 여러 시드/재현/구석/접근성 검사 |
| examples/ |12개 틀의 seed1001 참조 스냅샷 |
| preview/ |12개 논리 평면도 비교와 난이도별 상세도 |
| VALIDATION_REPORT.json | 실제 수행한 검사 결과와 한계 |

## 구조와 초기 수치

| 난이도 | 맵 경계 | 방 수 | 몬스터 초안 | 컨테이너 초안 |
|---|---:|---:|---:|---:|
| 쉬움 |128×96 |16 |32 |32 |
| 보통 |160×120 |25 |72 |75 |
| 어려움 |192×144 |36 |128 |144 |

수량은 제안값이다. 실제 게임의 기존 쉬움 수량(이미 반영된8배 증량 포함)이 우선이다. 보통/어려움을 그 기준으로 높이고 중복8배 적용하지 않는다. 기존 몬스터가4종뿐이므로 허용 종류는 쉬움2종 → 보통/어려움4종이며 어려움은 고체력 몬스터 비중을 더 높인다. 없는 몬스터를 새 종류라고 만들지 않는다.

구조 틀은 압축 배치·두 동 연결·중정 순환·엇갈린 배치다. 각 틀 안에서 시드에 따라 짧은 연결 그래프, 방 모서리 후퇴, 테마, 가구 패턴, 컨테이너/몬스터가 달라진다. 그림을 통째로 배경으로 쓰지 않는다.

## 사용

1. 전체 폴더와 `RANDOM_LAB_IMPLEMENTATION_PROMPT.md`를 개발 AI에게 전달한다.
2. 기존 쉬움의 실제 배치 코드와 PNG/TextureRegion/footprint를 조사해 의미 ID를 매핑한다.
3. 데이터는 게임의 `assets/data/laboratory_generation_v1/` 등 기존 규칙에 맞게 옮긴다. tools/examples/preview는 개발 자료이며 런타임에 필요하지 않다.
4. 순수 Java 생성기를 연결하고 기존 쉬움의 실제 렌더러·충돌·길찾기·미니맵을 공통 사용한다.
5. 실제 가구 footprint와 플레이어 크기를 반영한 검증·실제 화면 확인을 수행한다.

참조 검증은 Python3 표준 라이브러리만 사용한다. 게임 런타임에 Python은 필요하지 않다. 미리보기 생성에만 Pillow를 사용했다.

```sh
python tools/validate_pack.py --seeds-per-template 25
python tools/reference_generator.py --difficulty easy --template easy_courtyard --seed 1001 --output example.json
```

## 좌표·시드 계약

- tile 좌표, 좌하단 원점, +X 오른쪽/+Y 위쪽. origin은 방 바닥의 바운딩 사각형 좌하단이다.
- roomSlots는 배치 틀의 정수 그리드, 실제 방 간격은 gridPitch다. 방과 연결부는 seed로 계산한다.
- seed는64비트 정수의10진 문자열을 사용한다. FNV-1a32(`seed|scope` UTF-8)로 하위 스트림을 만들고 XorShift32(13,17,5)와 unsigned modulo를 사용한다. Java 이식 시 `VALIDATION_REPORT.json`의 golden vector를 비교한다.
- 렌더 장식용 난수는 기존 생성 스트림과 별도로 둔다. 동일 시드라도 generator/template 버전이 바뀌면 결과가 바뀔 수 있다.
- 검증 실패는 같은 요청 시드에서 파생한 재시도로 최대3번 시도한다. 최종 실패는 같은 난이도의 compact/1001 기준으로 폴백하고 원인·요청 시드·실제 generationSeed를 기록한다.

## 접근성·방향

문 앞/방 내부 중심 통로/컨테이너 접근·드롭 동선/팀장 후보를 예약한 뒤 가구를 배치한다. 최종 캐비넷 등급·개체는 가구 이후 확정한다. 북벽 정면, 남벽 뒷모습, 좌우벽 전용 측면 뷰를 매핑한다. 실제 아트는 회전해 대체하지 않는다.

검증기는 대문에서 접근 가능한 일반 문을 순서대로 열며 모든 방 활동 지점·캐비넷 앞·드롭 위치에 도달하는지 확인한다. 캐비넷 본체 좌표는 solid다. 쉬움 경비원/팀장0, 보통/어려움 팀장 구석 조건을 검사한다.

## 확인 범위와 한계

검증 보고서의 PASS는 **논리 템플릿과 근사 가구 footprint, 타일 중심 이동** 기준이다. 실제 기존 PNG, 애니메이션, 벽 스프라이트 접합, 가구 발 앵커, 플레이어 AABB, Java 게임 실행을 확인한 결과가 아니다. 플레이어 크기만큼의 여유와 실제 가구 메타데이터로 재검증해야 한다.

미리보기에서 회색은 벽, 어두운 직사각형은 가구, 금색은 컨테이너, 주황은 문, 분홍은 팀장, 청록은 대문이다. 논리 평면도를 게임 화면처럼 보고하거나 배경 PNG로 사용하지 않는다.

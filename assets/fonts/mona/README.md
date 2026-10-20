# fonts/mona — Mona 픽셀 폰트 (UI 전역 폰트)

출처: https://github.com/MonadABXY/mona-font (Monad ABXY, https://monadabxy.com)
릴리스 2026.10.09의 TTF 중 한글 텍스트용 **Mona12TextKR / Mona12TextKR-Bold**만 설치했다.

## 라이선스
SIL Open Font License 1.1 (Reserved Font Name "Mona") — 임베딩·수정·재배포 허용,
폰트 파일 단독 유료 판매만 금지. 원문: `mona.txt`(및 파생 원본 폰트 라이선스
`ark-pixel.txt`·`k8x12.txt`·`mplus.txt`·`noto-emoji.txt`).

## 게임 내 사용(2026-10-16)
- **LibGDX BitmapFont**: `GameAssets`가 gdx-freetype으로 Bold TTF를 24px(12px 설계의 2배수,
  Nearest)로 생성 — HP 숫자·연타 카운트·쿨다운 초·키캡 라벨 등 화면/월드 텍스트.
- **AWT 래스터 라벨**: `GameFonts.awtBold()` — 획득 토스트(KoreanLabelCache)·난이도 모달·
  본부 SPACE 라벨·가방 가득 알림 등 한글 라벨. 글리프 누락/로드 실패 시 시스템 한글
  폰트(맑은 고딕 등)로 자동 폴백.
- F1/F2 디버그 텍스트는 기본 BitmapFont를 유지한다(개발 전용).

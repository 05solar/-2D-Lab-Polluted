# music — 배경음악

| 파일 | 내용 |
|---|---|
| `background_music.ogg` | 전역 배경음악(약 3분 20초, 무한 반복 스트리밍) |

## 출처
사용자가 제공한 영상 `제목 없는 디자인.mp4`(2026-10-15)에서 오디오 트랙만 추출해
OGG Vorbis(q5)로 변환했다(ffmpeg, 48kHz 스테레오). 저작권/사용 권한은 제공자(프로젝트 소유자) 책임 하에 사용한다.

## 재생 규칙(코드)
- `GameAssets`가 `Music`으로 소유·해제, `LaboratoryGame.create()`에서 1회 `setLooping(true)` 후 재생.
- 본부↔연구소 화면 전환과 무관하게 끊기지 않고 계속 반복된다.
- 음량은 `BalanceConfig.musicVolume`(기본 0.45). 개발 캡처 등 무음 실행은 `LAB_MUTE_MUSIC=1`.

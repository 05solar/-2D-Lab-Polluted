# 보물상자 및 아이템 에셋

## 컨테이너

- 일반 나무 상자: 일반 아이템
- 일반 캐비넷: 에픽 아이템
- 고급 캐비넷: 레전드 아이템

각 컨테이너 시트는 256×128, 프레임은 64×64, 4열×2행이다.
프레임 0은 닫힘, 프레임 1~6은 열림 과정, 프레임 7은 열린 상태다.

## 아이템

등급별 아이템 아틀라스는 512×128, 프레임은 64×64, 8열×2행이다.

- 0행: 테두리 없는 기본 아이콘
- 1행: 등급별 그라데이션 테두리 아이콘
- 일반: 파란색
- 에픽: 보라색
- 레전드: 금색

각 아이템의 개별 PNG도 등급 폴더에 포함되어 있다. `_framed` 접미사가 붙은 파일은 등급 테두리 버전이다.

## 확률

`loot_tables.json`의 확률은 각 상자 내부에서 합계 100%다.
가격이 올라갈수록 확률이 감소하도록 `1 / sqrt(price)` 가중치를 정규화했다.
동일 가격 아이템은 동일 확률을 갖는다.

## LibGDX

텍스처에는 `TextureFilter.Nearest`를 사용한다.

    TextureRegion[][] itemFrames = TextureRegion.split(itemTexture, 64, 64);
    TextureRegion[][] containerFrames = TextureRegion.split(containerTexture, 64, 64);

JSON의 행과 열은 왼쪽 위를 원점으로 하고 0부터 시작한다.

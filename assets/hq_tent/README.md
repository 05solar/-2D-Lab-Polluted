# Temporary Headquarters Tent Asset Pack

폐쇄 연구소 안에 설치된 임시 본부 전용 2D 탑뷰 에셋입니다. 기존 연구소 타일과 함께 사용할 수 있도록 어두운 올리브 천막, 금속 프레임, 청록색 장비 조명과 주황색 비상등을 사용했습니다.

## 구성

- `hq_tent_tiles_64.png`: 256×256, 4×4, 셀 64×64
- `hq_furniture_128x96.png`: 512×288, 4×3, 셀 128×96
- `hq_vending_machine_front_96x128.png`: 576×128, 6×1, 프레임 96×128
- `tiles/`: 배경 타일 개별 PNG 16개
- `furniture/`: 본부 가구 개별 PNG 12개
- `vending_machine/`: 자판기 애니메이션 개별 PNG 6개
- `hq_tent_assets.json`: 인덱스, 앵커, 애니메이션 정보

## 배치 권장

- 천막 바닥은 `canvas_floor`를 기본으로 사용하고 seam/patch 타일은 전체의 10~20%만 섞습니다.
- 북쪽 벽에는 `north_tent_wall` 계열을 사용하며 3~4타일마다 support 타일을 배치합니다.
- 동·서쪽 벽은 `west_tent_wall`, `east_tent_wall`을 사용합니다.
- 출입구는 `open_tent_flap`을 기본으로 하고 봉쇄 상태에서만 `closed_tent_flap`을 사용합니다.
- 자판기는 북쪽 천막 벽에 붙여 정면이 플레이어를 향하도록 배치합니다.
- 가구와 자판기의 렌더 앵커는 아래쪽 중앙입니다.

## 자판기 애니메이션

- IDLE: `idle_dim` ↔ `idle_pulse`, 프레임당 0.45초
- INTERACT: `interact_highlight` → `purchase_accepted`, 프레임당 0.12초
- DISPENSE: `dispensing` → `hatch_closing`, 프레임당 0.18초
- 상호작용 종료 후 IDLE로 복귀합니다.

LibGDX에서는 `TextureFilter.Nearest`, `TextureWrap.ClampToEdge`를 권장합니다.

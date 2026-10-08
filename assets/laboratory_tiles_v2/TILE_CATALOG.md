# Laboratory Tileset V2 카탈로그

인덱스는 각 시트별로 독립적이며 좌상단에서 오른쪽으로 증가합니다.

## 바닥 — `lab_floor_tiles_v2_64.png`

| 인덱스 | 행/열 | ID | 설명 |
|---:|---:|---|---|
| 0 | 0/0 | `floor_clean_a` | 기본 바닥 A. 가장 많이 사용하는 중립 바닥 |
| 1 | 0/1 | `floor_clean_b` | 마모와 색이 조금 다른 기본 바닥 B |
| 2 | 0/2 | `floor_clean_c` | 밝기 변화가 있는 기본 바닥 C |
| 3 | 0/3 | `floor_seam_horizontal` | 가로 금속 연결선이 있는 바닥 |
| 4 | 0/4 | `floor_seam_vertical` | 세로 금속 연결선이 있는 바닥 |
| 5 | 0/5 | `floor_seam_cross` | 십자형 금속 연결부 |
| 6 | 1/0 | `floor_worn_light` | 약하게 닳은 바닥 |
| 7 | 1/1 | `floor_scratched` | 얕은 긁힘이 있는 바닥 |
| 8 | 1/2 | `floor_patch_welded` | 작은 용접 보수판 |
| 9 | 1/3 | `floor_patch_bolted` | 볼트가 박힌 보수판 |
| 10 | 1/4 | `floor_dust_edge` | 가장자리에 먼지와 잔돌이 쌓인 바닥 |
| 11 | 1/5 | `floor_damp_stain` | 습기·곰팡이 얼룩 바닥 |
| 12 | 2/0 | `floor_crack_light` | 작은 Y자 균열 |
| 13 | 2/1 | `floor_crack_medium` | 돌 조각이 섞인 중간 균열 |
| 14 | 2/2 | `floor_crack_nw` | 북서쪽에서 이어지는 균열 |
| 15 | 2/3 | `floor_crack_se` | 남동쪽으로 이어지는 균열 |
| 16 | 2/4 | `floor_broken_panel` | 바닥 패널이 크게 파손된 상태 |
| 17 | 2/5 | `floor_exposed_underplate` | 하부 배관·그레이팅이 노출된 바닥 |
| 18 | 3/0 | `floor_warning_north` | 북쪽 경계용 경고선 |
| 19 | 3/1 | `floor_warning_south` | 남쪽 경계용 경고선 |
| 20 | 3/2 | `floor_warning_corner_nw` | 북서 경고선 모서리 |
| 21 | 3/3 | `floor_warning_corner_ne` | 북동 경고선 모서리 |
| 22 | 3/4 | `floor_drainage_grate` | 배수 그레이팅 |
| 23 | 3/5 | `floor_access_hatch` | 정비용 점검 해치 |

## 벽 오토타일 — `lab_wall_autotiles_v2_64.png`

모든 벽 타일은 `solid=true`이며 투명 배경입니다.

| 인덱스 | 행/열 | ID | 설명 |
|---:|---:|---|---|
| 0 | 0/0 | `wall_horizontal` | 정상 가로 벽 |
| 1 | 0/1 | `wall_horizontal_damaged` | 파손된 가로 벽 |
| 2 | 0/2 | `wall_vertical` | 정상 세로 벽 |
| 3 | 0/3 | `wall_vertical_damaged` | 파손된 세로 벽 |
| 4 | 1/0 | `wall_end_left` | 왼쪽에서 끝나는 가로 벽 |
| 5 | 1/1 | `wall_end_right` | 오른쪽에서 끝나는 가로 벽 |
| 6 | 1/2 | `wall_end_top` | 위쪽에서 끝나는 세로 벽 |
| 7 | 1/3 | `wall_end_bottom` | 아래쪽에서 끝나는 세로 벽 |
| 8 | 2/0 | `wall_outer_nw` | 북서 바깥 모서리 |
| 9 | 2/1 | `wall_outer_ne` | 북동 바깥 모서리 |
| 10 | 2/2 | `wall_outer_sw` | 남서 바깥 모서리 |
| 11 | 2/3 | `wall_outer_se` | 남동 바깥 모서리 |
| 12 | 3/0 | `wall_inner_nw` | 북서 안쪽 모서리 |
| 13 | 3/1 | `wall_inner_ne` | 북동 안쪽 모서리 |
| 14 | 3/2 | `wall_inner_sw` | 남서 안쪽 모서리 |
| 15 | 3/3 | `wall_inner_se` | 남동 안쪽 모서리 |

## 연결부·문 — `lab_structure_doors_v2_64.png`

| 인덱스 | 행/열 | ID | 충돌 | 설명 |
|---:|---:|---|:---:|---|
| 0 | 0/0 | `wall_t_open_north` | O | 북쪽이 열린 T자 연결 |
| 1 | 0/1 | `wall_t_open_south` | O | 남쪽이 열린 T자 연결 |
| 2 | 0/2 | `wall_t_open_east` | O | 동쪽이 열린 T자 연결 |
| 3 | 0/3 | `wall_t_open_west` | O | 서쪽이 열린 T자 연결 |
| 4 | 1/0 | `wall_cross` | O | 십자 벽 연결부 |
| 5 | 1/1 | `wall_support_horizontal` | O | 강화 가로 지지 벽 |
| 6 | 1/2 | `wall_support_vertical` | O | 강화 세로 지지 벽 |
| 7 | 1/3 | `wall_console` | O | 청록색 표시등이 있는 벽 콘솔 |
| 8 | 2/0 | `horizontal_door_closed` | O | 가로 벽용 닫힌 슬라이딩 문 |
| 9 | 2/1 | `horizontal_door_open` | X | 가로 벽용 열린 문과 문턱 |
| 10 | 2/2 | `vertical_door_closed` | O | 세로 벽용 닫힌 슬라이딩 문 |
| 11 | 2/3 | `vertical_door_open` | X | 세로 벽용 열린 문과 문턱 |
| 12 | 3/0 | `door_jamb_left` | O | 문 왼쪽 문틀·벽 끝 |
| 13 | 3/1 | `door_jamb_right` | O | 문 오른쪽 문틀·벽 끝 |
| 14 | 3/2 | `wall_breach_horizontal` | 조건부 | 파손된 가로 벽과 잔해 |
| 15 | 3/3 | `wall_breach_vertical` | 조건부 | 파손된 세로 벽과 잔해 |

`wall_breach_*`는 통과 가능한 지름길로 사용할 때만 충돌을 제거합니다.

## 오버레이 — `lab_overlay_decals_v2_64.png`

오버레이는 기본적으로 `solid=false`입니다.

| 인덱스 | 행/열 | ID | 속성 | 설명 |
|---:|---:|---|---|---|
| 0 | 0/0 | `grime_small` | 장식 | 작은 먼지·오염 |
| 1 | 0/1 | `grime_medium` | 장식 | 중간 크기 오염 |
| 2 | 0/2 | `oil_stain` | 장식 | 어두운 기름 얼룩 |
| 3 | 0/3 | `water_stain` | 장식 | 물이 고인 흔적 |
| 4 | 0/4 | `contamination_spatter` | 오염 | 작은 녹색 오염 비말 |
| 5 | 0/5 | `toxic_puddle_small` | 독성 | 작은 독성 웅덩이 |
| 6 | 1/0 | `toxic_puddle_large` | 독성 | 큰 불규칙 독성 웅덩이 |
| 7 | 1/1 | `toxic_bubbles` | 독성 | 기포가 생기는 독성 잔류물 |
| 8 | 1/2 | `crack_overlay_small` | 장식 | 어느 바닥에도 얹을 수 있는 작은 균열 |
| 9 | 1/3 | `crack_overlay_medium` | 장식 | 중간 균열 |
| 10 | 1/4 | `crack_overlay_branching` | 장식 | 가지처럼 퍼진 균열 |
| 11 | 1/5 | `broken_glass` | 장식 | 깨진 유리 조각 |
| 12 | 2/0 | `rubble_small` | 장식 | 작은 잔해 |
| 13 | 2/1 | `rubble_large` | 장식 | 큰 잔해 더미 |
| 14 | 2/2 | `cable_horizontal` | 전기 | 가로 전선 |
| 15 | 2/3 | `cable_vertical` | 전기 | 세로 전선 |
| 16 | 2/4 | `cable_corner` | 전기 | 굽은 전선 연결부 |
| 17 | 2/5 | `cable_t_junction` | 전기 | T자 전선 연결부 |
| 18 | 3/0 | `exposed_wires` | 전기 | 절단되어 노출된 전선 |
| 19 | 3/1 | `electric_sparks` | 감전 | 청록색 전기 스파크 |
| 20 | 3/2 | `scorch_mark` | 장식 | 폭발·화재 흔적 |
| 21 | 3/3 | `warning_stencil` | 장식 | 글자 없는 마모된 경고 표시 |
| 22 | 3/4 | `drain_leak` | 오염 | 배수구 누수 흔적 |
| 23 | 3/5 | `lab_residue` | 장식 | 흰색 화학 잔류물 |

## 권장 분포

- 기본 바닥 A/B/C: 65~75%
- 약한 마모·긁힘·얼룩: 15~20%
- 균열·보수판·배수구: 5~10%
- 강한 오염·노출 배선·경고선: 5% 이하

장식 변형은 고정 seed로 선택하고 같은 타일이 가로 또는 세로로 3회 이상 반복되지 않게 배치합니다.

## Connected atlas derivation

The catalog now loads `lab_wall_connected_v3_64.png`, `lab_structure_connected_v3_64.png`, and `lab_vertical_jambs_v3_64.png`. The original V2 wall/structure PNGs remain source art. Regenerate the derived files using the Java 8/ImageIO tool in `tools/WallSeamAtlasGenerator.java` as documented in the root README. Every atlas cell remains 64x64 RGBA with no trimming or runtime rotation. Connector masks use north=y+1, east=x+1, south=y-1, west=x-1; source atlas rows start at the top.

## Side door render override

`lab_side_doors_2x2_64.png` is a standalone 128×128 RGBA sheet split into four 64×64 cells. Runtime uses source row 0, column 0 for `vertical_door_closed` and row 0, column 1 for `vertical_door_open`. `GameAssets` replaces only those IDs' render regions; catalog collision and door state metadata remain authoritative. Horizontal doors continue to use the structure atlas.

# 보스 에셋 제작 프롬프트 기록

Built-in image generation으로 제작했다. 아래는 제작 명세다. 구현에는 BOSS_NINE_IMPLEMENTATION_PROMPT.md를 사용한다.

## 신규 보스 공통

Use case: stylized-concept. Production game sprite animation sheet for a dark polluted laboratory 2D top-down action game. Pixel art, crisp pixels, limited desaturated industrial palette with colored emissive details. Orthographic elevated top-down FRONT view like classic overhead dungeon game; no isometric, no diagonal angle, no perspective camera changes. Transparent background, no ground, no text, no grid lines, no border, no shadows extending into neighbors. Exactly 16 full-body sprites on a perfectly regular 4 columns x 4 rows square grid. Every pose entirely within its cell with AT LEAST 15% empty margins all sides. Every appendage is wholly visible, nothing cut off. One identical boss identity, costume, proportions, viewpoint and scale across all 16 frames; all grounded at same baseline, no zoom. Each cell shows only this boss. Rows are temporal frames: row1 four subtle idle breathing phases; row2 frame1 windup low, frame2 strong windup with core glow, frame3 release pose, frame4 followthrough; row3 frame1 recovery, frame2 neutral recovery, frame3 advancing step, frame4 advancing step; row4 four successive death poses from sagging to crumpled to fully inert collapsed wreck with dark residue, final pose whole body visible. Keep debris attached near core. No attack beams or giant external effects, only body animation. Subject: 

### 1-2 냉각관 거수

Subject: a massively broad upright industrial refrigeration brute, twin enormous vertical blue coolant cylinders as shoulders, thick frosted pipe arms, squat hydraulic legs, a small circular glowing cyan core, battered steel and ice. Silhouette like a hulking squared gorilla, no glass belly, no crab legs. Main accents ice blue.

### 1-3 배양낭 군체

Subject: a low wide organic colony of five translucent green culture bladders organically joined around one split central fleshy mouth, asymmetrical lobes and thick short tendril feet, rusted supporting laboratory tray. Clearly an organic many-sacked organism, no humanoid and no tank robot. Main accents acid green.

### 1-4 보안 제압 기체

Subject: a tall angular crimson and charcoal laboratory security suppression robot, one central narrow red camera eye, broad horizontal shoulder cannon rails, heavy rectangular twin tread feet, separate rotating laser turret on upper torso, no organic flesh. Strong angular T-shaped robotic silhouette. Main accents red.

### 1-5 신경 접속 표본

Subject: a levitating enormous pale preserved brain inside a broken steel neural halo, four long downward drooping cable tendrils terminating in electrode prongs, violet neural nodes and central purple eye, narrow floating lower silhouette, absolutely no wings or humanoid legs. Main accents violet.

### 1-boss 중앙 격리실 융합체

Subject: an immense asymmetrical containment failure amalgam, two fused reinforced laboratory containment tanks around a giant pale fleshy central eye, one huge claw arm, one cluster of industrial pipes and dangling tentacles, four heavy splayed metallic supporting feet, orange-red warning core. Silhouette widest and most imposing, one claw visibly larger. Main accents amber red.

### 2-1 저온실 다족 실험체

Subject: a long low icy laboratory arthropod experimental organism, armored pale blue segmented carapace, six clearly separated jointed legs on each side and two forward ice mandibles, frost-rimed blue steel cryogenic tubes along back, cyan coolant glow, symmetrical centipede-like silhouette facing camera, no diagonal view. Main accents pale cyan.

### 2-2 변이 식물 실험체

Subject: a monstrous laboratory plant growing out of a shattered containment planter, huge gaping magenta carnivorous blossom above three thick dark green root legs, two curling vine arms, pale pink laboratory growth sacs, upright branching floral silhouette, toxic green inner glow. Main accents green magenta.

### 2-3 이동형 배양 탱크

Subject: a huge industrial mobile bioculture tanker, broad rectangular heavy tracked chassis under an upright large transparent teal fluid cylinder containing a curled mutant silhouette, two elevated mortar pipes on sides and thick dripping outlet hoses, yellow hazard stripes, clearly a military sized laboratory vehicle with treads not crab legs. Main accents teal yellow.

## 최종 효과 시트 공통

Square transparent 4x4 sprite atlas. Each effect occupies only central 40% of its square cell, sixty percent transparent separation, no background wash, no text, no borders, no characters. Crisp laboratory pixel art, four temporal frames per row. Discarded initial VFX sheets are not in runtime pack.

### cryogenic_vfx

Row1 four successive cold cone blast animation phases, front-facing upward expanding fan of pale blue vapor and crystalline shards. Row2 four looping phases of a circular top-down icy floor hazard, flat icy ring and frozen cracked center. Row3 four phases of a single bright ice shard projectile pointing exactly right, short luminous trail to left, all cores at same cell center. Row4 four phases of ice impact shattering, small to full to fading crystals.

### biological_vfx

Row1 four looping phases of a flat circular top-down green spore hazard with floating spores and dark green slimy center. Row2 four successive phases of curling green roots erupting from a small floor circle, rising and grasping inward with pink root tips. Row3 four looping phases of a flat turquoise toxic leaking puddle, rounded irregular wet outline and bubbles. Row4 four successive phases of a small acid projectile impact splash with green droplets.

### energy_vfx

Row1 four phases of a horizontal red energy laser beam segment pointing exactly right, straight bright core and red glow, all contained inside a narrow horizontal strip in center of each square cell. Row2 four phases of an outward expanding violet psychic shockwave ring, thin circular ring, center empty, top-down circular not elliptical. Row3 four phases of a violet delayed psychic explosion, small pulse to full starburst to fading sparks. Row4 four phases of an orange-red enrage aura as an empty circular ring with upwards flames, center transparent so boss can be overlaid.

## 프레임 정리

원화는 전체 개체 연결 영역으로 분리하여 셀 경계의 팔다리 잘림을 방지했다. 몸체마다 모든 16프레임에 한 가지 배율을 사용하고 발밑을 [128,224]에 정렬했다. 런타임에서 다시 trim하거나 매 프레임 크기를 바꾸지 않는다. 실루엣은 같은 몸체의 idle 첫 프레임 알파에서 추출하여 각 보스와 일치하도록 만들었다.


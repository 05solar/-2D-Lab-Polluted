# Laboratory Room Kits V3

Top-down pixel-art room kits for the contaminated laboratory map.

- Logical tile size: 64 x 64 px
- Texture filter: Nearest
- Atlas origin: top-left
- Background: transparent RGBA
- Generated room kits: 8
- Main gate states: 4
- Main gate UI states: 4
- Furniture and prop cells: 128

Each pack contains the source image, a runtime atlas, and individually cropped
PNG files. Wall suffixes describe intended placement:

- `_north`: north wall, facing down toward the player
- `_left`: west/left vertical wall
- `_right`: east/right vertical wall
- no wall suffix: freestanding or strict top-down floor prop

Do not rotate `_north` assets to fake a side-wall placement. Use the supplied
left and right variants. Draw the floor first, then wall furniture, entities,
and finally tall foreground portions when Y sorting is required.

See `ASSET_CATALOG.md` for room composition guidance and
`laboratory_roomkits_v3.json` for atlas metadata.

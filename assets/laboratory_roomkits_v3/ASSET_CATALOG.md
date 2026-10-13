# Asset Catalog

## Main gate

| ID | Purpose |
|---|---|
| `gate_horizontal_closed` | Horizontal north/south wall gate, closed |
| `gate_horizontal_open` | Horizontal north/south wall gate, open |
| `gate_vertical_closed` | Vertical side-wall gate, closed |
| `gate_vertical_open` | Vertical side-wall gate, open |

Gate UI: `enter_laboratory`, `return_headquarters`, `locked`, `transitioning`.
Render Korean labels and the Space key prompt separately in code.

## General laboratory

`workbench`, `fume_hood`, `sink_counter`, and `storage_cabinet` each include
`north`, `left`, and `right` variants. Floor props are `experiment_island`,
`equipment_cart`, `lab_stools`, and `sample_crate`.

Recommended room: place wall variants continuously around three sides, leave a
two-tile aisle around the central island, and use carts/crates to break repeated
patterns without blocking doors.

## Pantry and lounge

- Pantry: `kitchenette_north/left/right`, `refrigerator_north`,
  `microwave_cabinet_north`, `coffee_station_north`, `wall_shelf_left/right`.
- Lounge: `sofa_north/left/right`, `coffee_table`, `armchair`,
  `water_dispenser`, `snack_cabinet`, `trash_cups`.

Recommended room: keep the kitchenette on one wall, refrigerator and coffee
station on the adjacent wall, and form a seating island with the sofa, chair,
and coffee table.

## Sleeping quarters and locker room

- Sleeping: `bunk_bed_north/left/right`, `field_cot`, `footlocker`,
  `blanket_stack`, `bedside_cabinet`, `privacy_partition`.
- Locker room: `locker_bank_north/left/right`, `changing_bench`,
  `protective_suit_rack`, `laundry_hamper`, `shoe_rack`, `personal_items`.

Recommended room: line two walls with bunks or lockers, keep a two-tile center
aisle, and use benches and personal items near the middle without blocking it.

## Restroom and shower room

- Restroom: `toilet_north/left/right`, `urinal_bank`,
  `sink_counter_north`, `sink_left/right`, `stall_partition`, `damaged_stall`.
- Shower: `shower_north/left/right`, `floor_drain_mat`, `shower_bench`,
  `towel_soap_rack`, `cleaning_cart`.

Recommended room: separate wet and dry areas. Keep the floor drain and wet mat
inside shower zones and use stall pieces as collision walls.

## Chemistry and general experiment laboratory

Includes directional fume hoods and chemistry benches, plus
`reagent_cabinet`, `gas_cylinder_rack`, `distillation_station`, `centrifuge`,
`analyzer_machine`, `hotplate_station`, `eyewash_station`,
`spill_response_cabinet`, `pipette_station`, and `chemical_waste`.

Recommended room: place hoods and reagent storage on walls, use instrument
stations as interior islands, and keep eyewash/spill response close to exits.

## Biology laboratory and specimen room

Includes directional biosafety cabinets and specimen cabinets, plus
`microscope_workbench`, `incubator_bank`, `laboratory_freezer`,
`culture_workstation`, `culture_rack`, `dissection_table`, `autoclave`,
`cryogenic_tanks`, `biohazard_container`, and `specimen_jar_rack`.

Recommended room: biology work areas use incubators and culture equipment;
specimen rooms use cabinets on three walls with the jar rack as a focal prop.

## Information and CCTV room

Includes directional server racks, computer desks, and CCTV monitor walls,
plus `network_cabinet`, `operator_console`, `alarm_console`, `cable_panel`,
`ups_unit`, `office_chair`, and `camera_parts_rack`.

Recommended room: line server racks against walls, keep operator consoles
facing CCTV walls, and place cable/UPS props in service corners.

## Records archive and technical library

Includes directional filing cabinets, archive shelves, and bookshelves, plus
`fireproof_document_cabinet`, `document_cart`, `double_sided_bookshelf`,
`review_table`, `reading_desk`, `library_ladder`, and
`catalog_terminal_books`.

Recommended room: archive rooms use dense parallel shelf rows with one-tile
gaps; libraries keep a wider central reading area and wall-mounted shelves.

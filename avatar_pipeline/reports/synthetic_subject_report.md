# 3D Mobile Avatar Build & QA Report — synthetic_test_subject

- **Pipeline Status**: `PASS`
- **Untouched Initial Source GLB (`initial_visual_hull_source.glb`)**: `32512` tris, AssetFurnace `photo_paint` coverage `0.2286`
- **Master Asset (`synthetic_subject_master.glb`)**: `5.867 MB`, `22704` tris, `2048x2048` PBR + `KHR_materials_variants` (`Catalogue Tweed Blazer, Midnight Lab Navy Z-Suit`)
- **Mobile LOD0 Asset (`synthetic_subject.glb`)**: `3.2441 MB`, `22704` tris, `1` material (`2` draw calls)
- **Mobile LOD1 Asset (`synthetic_subject_lod1.glb`)**: `2.2252 MB`, `11879` tris, `512x512` atlas
- **Rig & Animation**: `60` joints (`<= 64` GLES limit), `26` morph targets, `24` animations
- **Blender Round-Trip Re-Import Verified**: `Master=True`, `LOD0=True`, `LOD1=True`
- **Metric Scale & Grounding**: height `1.682 m` (target `1.68 m`), floor `min_z = -0.0 m`

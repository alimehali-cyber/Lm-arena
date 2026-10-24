# 3D Mobile Avatar Build & QA Report — Professor Z

- **Pipeline Status**: `PASS`
- **Untouched Initial Source GLB (`initial_visual_hull_source.glb`)**: `25412` tris, AssetFurnace `photo_paint` coverage `0.6096`
- **Master Asset (`professor_z_master.glb`)**: `6.078 MB`, `29932` tris, `2048x2048` PBR + `KHR_materials_variants` (`Catalogue Tweed Blazer, Midnight Lab Navy Z-Suit`)
- **Mobile LOD0 Asset (`professor_z.glb`)**: `2.9513 MB`, `18477` tris, `1` material (`2` draw calls)
- **Mobile LOD1 Asset (`professor_z_lod1.glb`)**: `2.1286 MB`, `11868` tris, `512x512` atlas
- **Rig & Animation**: `60` joints (`<= 64` GLES limit), `26` morph targets, `24` animations
- **Blender Round-Trip Re-Import Verified**: `Master=True`, `LOD0=True`, `LOD1=True`
- **Metric Scale & Grounding**: height `1.7285 m` (target `1.73 m`), floor `min_z = -1e-05 m`

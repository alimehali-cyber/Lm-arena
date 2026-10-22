# Reference Forensic Analysis, Reconstruction Critique & Final QA Verification Report

## 1. Untouched Reference Inputs Analyzed (`100%` Local Processing)

| Reference File | Resolution | Sharpness (`Var(Laplacian)`) | SHA-256 Checksum (Verified Identical to Upload) | What Is Directly Observable vs. Inferred |
|---|---|---|---|---|
| `avatar_pipeline/input/ref_01_professor_z_lineup.png` | `1536 x 1024` RGB | `1224.6` | `19a055b164d683257fcc66005fa57ea221cd4347f92f788de977a4998870b143` | **Observable**: Dramatic twilight rim-lit atmosphere, expressive stargazer poses (pointing at sky, telescope, waving), warm golden-olive skin warmth, large amber eyes, round wire frames, swept-up dark espresso hair, casual night lab navy `"Z"` tee variant. |
| `avatar_pipeline/input/ref_02_professor_z_catalogue.png` | `1536 x 1024` RGB | `3751.5` | `098d1902057d18a9f346438d5f5bb3378b7a21f49894c72d01d6acd35be6b797` | **Observable**: Complete orthographic **Front (`0°`), Three-Quarter (`45°`), Side Profile (`90°`), and Back (`180°`)** full-body turnaround (`139x500`, `117x464`, `111x494`, `137x494` px), hero glasses-adjust pose, 6-swatch palette (`#232324`, `#4f2e16`, `#73451f`, `#aa7140`, `#e2d4c2`, `#36383a`), expressions grid, herringbone brown tweed blazer, cream button-down shirt, butterfly bow tie, belt & buckle, cuffed charcoal trousers, and cognac brogues (`Classic kicks`).<br>**Inferred**: Inner oral cavity pouch geometry, lower dental arch behind lips, under-chin & armpit seam topology, and exact per-vertex skin weights under extreme squat/arm elevation. |

---

## 2. Two-Stage Reconstruction Architecture & Iterative Visual Refinement Log

1. **Untouched Initial Source Reconstruction (`avatar_pipeline/working/professor_z/initial_visual_hull_source.glb`)**:
   - Carved a true 3D multi-view visual hull from the segmented `Front`, `Three-Quarter`, `Side`, and `Back` silhouettes (`skimage.measure.marching_cubes`), smoothed surface staircase artifacts with **`image_to_3dlab.surface_calm.taubin`**, verified outward normal orientation with **`image_to_3dlab.photo_paint.outward_sign`**, and projected real multi-view reference pixels onto the UV-unwrapped mesh via **`image_to_3dlab.photo_paint.paint_texture`**.
   - **Saved untouched separately** before any Blender topological cleanup, documenting exact initial limitations (merged wire glasses, single-genus hands, unseparated eyeballs/lips).
2. **Clean Multi-Part Blender 4.2 Character Rebuild & Visual Critique Iterations (`build_character.py` -> `rig_character.py` -> `render_review.py`)**:
   - **Iteration 1 Critique (Caught by direct visual inspection of `01..08` renders)**:
     1. Catmull-Clark `Subsurf` pulled upper/lower lip seam vertices apart into an unnatural wide open slot in rest pose.
     2. Upper/rear skull showed skin through hair lock gaps at `135°` and `225°`, and side hair locks were too tubular.
     3. Standard `155W` key light washed out nose bridge and cheek form shadows into pale peach.
     4. Upper eyelid was too high above small irises ("startled stare").
   - **Iteration 2 & 3 Refinements (Verified visually in final `01..12` renders)**:
     1. Added post-`Subsurf` closed-lip half-smile seam seal (`gap = 0.5 mm`), full-width curved upper/lower dental arches (`hx = 0.98 * mouth_hx`), and recessed resting tongue (`-base_ry * 0.54`) -> **zero dark mouth slot and zero tongue poke-through in rest pose, clean Pixar teeth in `Viseme AA` & `Happy`**.
     2. Built `cKDTree` skull-clearance-verified solid espresso inner hair base cap (`>= +8.5 mm` outside cranial surface across `360°`, tapered down to `chin_z - 0.014 m` on back nape and sideburns cleanly in front of ears) + flat-blade swept crown locks -> **zero bald spots at `0°`, `45°`, `90°`, `135°`, `180°`, `225°`**.
     3. Enlarged multi-fiber chestnut-amber irises (`r = 0.535`), lowered resting upper eyelid & lash arch to overlap top of iris, and tuned `eyeBlinkLeft/Right` forward wrap so **`ProfZ_TongueOutWink` and blinks close cleanly across the cornea**.
     4. Switched to `AgX` tone mapping with calibrated 3-point neutral review lighting (`72W / 26W / 54W`) first, followed by a separate dramatic twilight hero render (`11_cinematic_hero_portrait.png`) and direct `REF vs 3D` comparison sheet (`12_reference_vs_3d_turnaround.png`).
   - **Honest Remaining Engineering Trade-Offs**:
     - Round wire eyeglasses sit `+7 mm` forward of the orbital rim (`10_side_profile_portrait.png`) intentionally so full eyelid blinks and lashes never clip through the frame plane without refractive glass shaders (which mobile GLES avoids).
     - `LOD1` (`11,868` tris) uses `DECIMATE` with `PIN_DETAIL` protection on the glasses; subtle faceting is visible only under extreme close-up wireframe inspection, not at mid/far camera distances where `LOD1` activates.

---

## 3. Verified vs. Untested Matrix (Strict Honesty Ledger)

| Item | Status | Concrete Evidence |
|---|---|---|
| Sandbox hardware, CPU/RAM/disk/GPU & network allowlist audit | **VERIFIED** | Empirical probes recorded in `01_sandbox_baseline_and_candidate_audit.md` |
| Source audit of all 4 repos + `3,512` live upstream `image-to-3dlab` unit tests | **VERIFIED** | `pytest` run in `.venv`: `3,512 passed`, `31 skipped` |
| Original reference images preserved byte-for-byte untouched (`100%` offline) | **VERIFIED** | SHA-256 match against `/tmp/arena-coding-uploads/*.png` tested automatically |
| Untouched initial visual-hull + `photo_paint` mesh preserved separately | **VERIFIED** | `avatar_pipeline/working/professor_z/initial_visual_hull_source.glb` |
| Separate Master Asset (`2048` PBR + `KHR_materials_variants`) & `.blend` | **VERIFIED** | `exports/professor_z_master.glb` (`6.078 MB`, `29,932` tris, 2 outfit skins) + `professor_z_master.blend` |
| Mobile ZIG GLBs (`LOD0` `2.95 MB` / `18,477` tris, `LOD1` `2.13 MB` / `11,868` tris) | **VERIFIED** | `11/11` `validate_assets.py` QA gates PASS + `image_to_3dlab.glb_stats` PASS |
| Full Blender `4.2.0` glTF 2.0 round-trip re-import (`Master`, `LOD0`, `LOD1`) | **VERIFIED** | `reimport_ok = true` (`60` bones, `26` morph targets, UVs, textures & `24` clips intact) |
| Live JVM/Kotlin `2.2.10` GLB binary parser, skeleton skinning & lip-sync tests | **VERIFIED** | Automated unit suite in `avatar_pipeline/tests/test_pipeline.py` PASS |
| Protected ZIG subsystems untouched; pure GLES 3.0 only; zero git commit/push | **VERIFIED** | `git status` & safeguard unit assertions PASS |
| **Physical Android phone / emulator GPU rendering & FPS measurement** | **UNTESTED IN THIS SANDBOX** | Sandbox has **no Android emulator, no ADB device, and no GPU hardware**; on-device GLES 3.0 rendering, thermal behavior, and real-time audio lip-sync timing require testing on a physical Android device (`API 26+`). |

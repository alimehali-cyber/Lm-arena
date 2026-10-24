# Professor Z — Iteration-3 Reference-Faithful 3D Rebuild (reconstruction_v3)

Deterministic headless pipeline that rebuilds Professor Z **exactly** from
`avatar_pipeline/input/professor_z_turnaround_ref.png` (SHA-256
`c13e3cc7083d453c51ee55d3c64462f9656b649afd04bc96e6b95901e2fbb284`).
All processing is 100% local/offline; the reference photo is never uploaded anywhere.

Run order (each stage is idempotent and re-runnable):

0. `step_a0_neural_depth.py` — `Depth-Anything-V2-Small` ONNX neural depth (CPU, `onnxruntime`) per view
   producing `{front,side,back}.npz` (`depth`, `depth_global`) + the seed `front_facemesh_478.npy` that
   `step_a*` consumes. Added in Iteration-4 so the whole chain is reproducible from a cold cache.
1. `step_a_surgical_extract.py` — per-view masks over the A0 depth plates.
2. `step_a2_perfect_plates.py` — PCHIP multi-view Y-correspondence, MediaPipe 478-point FaceMesh on the head crop.
2b. `step_a4_face_refine.py` — Iteration-5: composites the six-view high-detail head reference
   (`avatar_pipeline/input/professor_z_face_ref.png`) onto the three plates with a landmark-anchored rigid
   transform (rotate to the eye-line roll, uniform scale to the pupil separation, translate the eye-midpoint /
   profile nose-mass / back crown onto their old positions), feathers the blend, unions the silhouette into the
   masks, carves disconnected hair islands and re-runs DA2 depth. Runs BETWEEN `a2` and `a3`, so every
   downstream FaceMesh, glasses-ring and inpaint computation refits to the new face automatically.
3. `step_a3_flawless_plates.py` — surgical razor matting (unmixing from the fitted paper background), elliptical
   eyeglasses-ring fit, watch-free inpainted SIDE plate.
4. `step_b_*` (v4→v7) — data-driven anatomical splines; sub-mm metric SDF sculpt of head/face/ears/neck,
   T-shirt + hollow sleeves, bent arms into pockets, slim jeans + rolled cuffs; Converse high-tops swept from
   the measured SIDE profile; 3D bronze wire glasses + 3D smartwatch.
5. `step_bc_perfect_hair_v8.py` — hair carved as the smooth 3-view visual hull (FRONT full head ∪ SIDE ∪ BACK
   hair mass) + `Depth-Anything-V2` high-pass lock relief + S-wave grooves, with smooth face/ear/skull
   carve-outs — watertight at all angles, zero Steinmetz ledges.
6. `step_c_swap_hair_and_render_v8.py` — semantic UV unwrap + BVH raycast multi-view PBR bake (basecolor/ORM/
   normal/emissive per part, zero texture bleed via class-filtered plates + Voronoi bleed) + Cycles+OIDN studio
   turnaround renders on the cream reference background.
7. `step_d_export_zig_production_glbs.py` — packs all 9 parts into a zero-bleed `3x3` padded atlas, joins into the
   ZIG runtime contract (`Avatar_Head` + `Avatar_Body`), builds the exact `60`-bone rig, `26` Pixar morphs
   (0.0 at rest), `24` NLA clips, and exports Master `2048` + `KHR_materials_variants`, mobile LOD0 `1024`
   (≤ 28k tris), LOD1 `512`; copies LOD0/LOD1 into `app/src/main/assets/avatar/`, runs 11-gate QA validation,
   and renders a proof sheet re-imported live from the shipped `professor_z.glb`.

Models: `bpy==4.2.0`, `onnxruntime`, `mediapipe==0.10.21`, `trimesh==4.6.4`, `scipy`, `scikit-image`,
`fast-simplification`, `pygltflib` (all installed by `avatar_pipeline/tools/bootstrap_env.sh`).
Depth weights: `avatar_pipeline/models/depth_anything_v2_small/model_fp16.onnx` (see MODEL_MANIFEST.json;
re-fetched by bootstrap, gitignored binary).

## Iteration-4 refinements (2026-10-24)

* **Reproducible cold start** — `step_a0_neural_depth.py` now generates the neural-depth plates that
  previously only existed as Iteration-3 scratch; `step_a3` re-runs FaceMesh on the exact padded head
  crop (never resized) with a grow-on-failure retry.
* **Pocket-drag fix** — `step_d` tags every vertex with its semantic source part BEFORE the join and
  skins with a part-pinned bone allow-list (`jeans→Hips/legs only`, `bare arms→arm+hand chains`,
  `shirt torso→spine set`, `sleeves→same-side arm chain`, `watch→ForearmTwist_L`, `shoes→Foot_L/R`
  rigid). A hard self-check asserts ZERO cross-talk before export, so arms can never drag the jeans.
* **Walk clips** release both hands from the pockets with a stronger ±27° anti-phase swing.
* **Likeness polish** — discrete extruded curl clumps (96 golden-spiral lobes + nape skirt + sideburn
  tufts) break the hair silhouette like the reference instead of a smooth helmet; smaller tucked ears;
  fuller cheeks; bolder brows; softer jaw; sturdier neck; hands hang deeper into the pockets (v8 arm
  splines); slimmer wrist-worn watch; snugger rolled cuffs (trumpet flare removed); lens-veil &
  smile-shadow cleanup in the face bake.

### Iteration-4b — garment forensics & portrait layout (same day)

* **Jeans ankle decontamination** — the leg-loft silhouette splines sampled mask rows *below* 23 cm, where the
  reference photo's WIDE SNEAKERS share the rows: every ankle opening ballooned to 14.5 cm and the flat clip-plane
  cap rendered as a floating brim wider than the shoes. Splines are now clamped to `y ≥ 0.230` and a linear
  calf→cuff taper (floor 52 mm x-radius) restores the snug rolled-cuff silhouette.
* **Symmetric shoe splay** — `−30°/+14°` asymmetric yaw (an Iteration-3 leftover) → mirrored `∓13°`.
* **T-shirt hem** — hard ellipse-rim plane cut produced pointed "wing" corners at the hips; replaced with a
  quadratic round-hanging term, hem lowered to 848 mm and widened (+7 mm x / +7 mm z at the bottom row) so the
  fabric fully covers the waistband like the reference.
* **`SkySceneLayout.professorZBox` portrait cap** — height-fraction-only sizing made the box 603 px wide on a
  320-px-wide phone (overflowing, and center-clamped OFF the left half on 412×900). The box now takes
  `min(h·0.68, w·0.46/aspect)` so Professor Z ALWAYS fits entirely inside the canvas and owns the left half on
  every phone aspect — verified by a JVM `kotlinc` harness run (all 10 viewport combinations + the 1080×2400
  portrait spot check) and by `SkyProjectionTest`/`ProfessorZAvatarUnitTest` (3/3 green against the shipped GLBs).
* **FaceMesh skew guard in `step_a3`** — blob-detected ground-truth irises (compact-dark-blob search with
  brow/lash rejection + wrong-side penalty) drive a rigid similarity correction of all 478 landmarks; on this
  reference the correction measured scale 0.972/rot +0.68°, confirming the historic +30 px offset is gone and
  locking the face registration against future detector wobble.

## Iteration-5 (2026-10-24)

* **Language-aware side swap** — Professor Z stands on the RIGHT in English mode and on the LEFT in Persian
  (RTL) mode (`SkySceneLayout.professorZBox(figureOnRight=...)`, mirrored idle yaw in
  `AvatarCameraAndLighting.computeModelMatrix`, tap hit-region follows). Verified headlessly: JVM layout
  harness + `SkyProjectionTest` (both sides, mirror symmetry, containment, feet seat).
* **IDENTICAL-face mandate** — new `step_a4_face_refine.py` stage injects the user-supplied 6-view head
  reference into the reconstruction plates (see 2b above); head SDF, visual hull, hair clumps, glasses
  rings, inpaint masks, bake and textures are all re-derived from it. Body/garments unchanged.

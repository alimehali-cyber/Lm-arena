# Professor Z — Iteration-3 Reference-Faithful 3D Rebuild (reconstruction_v3)

Deterministic headless pipeline that rebuilds Professor Z **exactly** from
`avatar_pipeline/input/professor_z_turnaround_ref.png` (SHA-256
`c13e3cc7083d453c51ee55d3c64462f9656b649afd04bc96e6b95901e2fbb284`).
All processing is 100% local/offline; the reference photo is never uploaded anywhere.

Run order (each stage is idempotent and re-runnable):

1. `step_a_surgical_extract.py` — per-view masks + `Depth-Anything-V2-Small` ONNX neural depth (CPU, `onnxruntime`).
2. `step_a2_perfect_plates.py` — PCHIP multi-view Y-correspondence, MediaPipe 478-point FaceMesh on the head crop.
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

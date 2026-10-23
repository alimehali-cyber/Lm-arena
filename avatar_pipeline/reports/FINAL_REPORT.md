# Character creation workstation — final report

**Build:** `working/test01` · **Blender 5.0.1** · **glTF Blender I/O 5.0.21** · **Python 3.11.2**
**Date:** 2026-10-22
**Status of every claim below:** ✅ executed and verified here · ⚠️ executed, result limited by
the environment · ⛔ never executed, stated as planned

---

## 1. Summary

A headless, reproducible character pipeline now exists in `avatar_pipeline/`. One command
builds a rigged, skinned, animated, morph-targeted character from a photograph, validates
the result, exports a mobile GLB, renders a review set and writes a manifest:

```bash
cd avatar_pipeline && ./run_pipeline.sh working/test01     # ~55 s end to end
```

| Deliverable | Result | Status |
|---|---|---|
| Photo analysis + validation | face box, 15 sampled colours, proportions, `limitations[]` | ✅ |
| Character mesh | 17,051 verts / 29,539 polys, 1.76 m, 7 materials | ✅ |
| Body rig | 33 bones (31 deforming), fingers, eyes, jaw | ✅ |
| Skinning + deformation gate | 8 posed cases, edge-stretch metric, **pass** | ✅ |
| Facial rig | 17 morph targets, 1.4–17.7 mm measured displacement | ✅ |
| Animation library | 21 named clips, 24 fps, seeded determinism | ✅ |
| Lip-sync | amplitude mode + Rhubarb JSON/TSV import | ⚠️ energy mode only, no Persian test |
| Review renders | 5 beauty shots + 23 suite frames, visually inspected | ✅ |
| Mobile GLB | 1.612 MB, 17,089 v, 33,988 tris, 31 joints, 21 clips | ✅ |
| GLB validation | 21 checks, `pass`, 0 failures | ✅ |
| Android runtime | 7 Kotlin files, GLES 3.0, no NDK | ⛔ never compiled or run |
| Unit tests | 21 tests, all pass | ✅ |
| **Identity match to the subject** | **none — stylised blockout** | ❌ by design of scope |

The single biggest gap against the brief is **§4**: this environment has no learned
image-to-3D model and no GPU, so the mesh is a parametric body fitted to measurements from
the photograph, not a reconstruction of the person. Everything downstream is real.

### Addendum — concept-art input (added after the first review)

The original pipeline assumed photographs. Concept art breaks that assumption in three
separate places, and all three were real failures here before they were fixed:

| Failure | Symptom | Fix |
|---|---|---|
| Haar cascade on a drawing | face found in the wrong place, or none at all | silhouette fallback + plausibility gate; a cascade box is rejected and *recorded in `rejected_faces[]`* when it disagrees with the measured head by >30 % |
| Landmark maths assumes 7 heads | pelvis ended up **above** the chest at 4.4 heads; rig folded, 29× edge stretch | vertical landmarks are now fractions of the body below the head, which reproduces 7-head output exactly |
| Box colour sampling on flat art | hair sampled the white background → white hair | sampling is masked to the character; a hair box that overhangs the figure falls back to the silhouette crown |

Two more defects surfaced while validating against art and were fixed: the collar/skin
boundary painted white spikes across the shirt (a z-threshold through remesh+decimate
slivers — now classified per authored part, with a polygon skin-side only if **all** its
vertices are), and the alpha channel of a PNG shadowed the `argparse` namespace.

A stylised test input now builds, skins, animates and exports identically:
`working/art01` → 1,613,736 B, validation `pass` (21 checks), skinning `pass`, 21 clips,
31 joints. Reproduce with
`SAMPLES=12 RES=260 ./run_pipeline.sh working/art01 path/to/art.png`.

**What this still cannot do:** it does not reproduce the drawing. It reads a silhouette,
a palette and a head count, then builds the same blockout — the head shape, hair style and
body build remain generic. Ten different drawings give ten differently-coloured blockouts.
Adding real shape variation (face/jaw/eye/hair/build parameters) is the next project, and
is not claimed here.

---

## 2. Environment and method

- No root, no `apt`, no GPU, no display, no Android device, no emulator, no SDK.
- The environment is rebuildable from scratch: `./tools/headless/bootstrap_env.sh` creates
  `.venv` (pinned `bpy==5.0.1`, `numpy==1.26.4`, `opencv-python-headless==4.10.0.84`),
  regenerates the linker stubs and smoke-tests the import. `.venv` and `stublibs/` are
  build artefacts and do not survive a sandbox reset, which is why this exists.
- Blender's bundled `bpy` needs X11/GL libraries that do not exist here. `tools/headless/`
  contains 30 stub SONAMEs (295 symbols) generated once from `nm -D --undefined-only`.
  `libEGL` is deliberately **never** stubbed, because stubbing it changes which GL backend
  Blender picks and produced loader crashes.
- Determinism: `PYTHONHASHSEED=0`, 2 threads, fixed clip seeds, no wall-clock or random
  input in any stage. Re-running the pipeline reproduces the same files byte-for-byte apart
  from Blender's internal `.blend` timestamps (the GLB hash is stable).
- Everything is headless; no GUI operation is required or performed.
- Licence of tooling: Blender GPL-3.0-or-later (running it does not license the output),
  OpenCV Apache-2.0, NumPy BSD-3, `gpt_3d_skill` **no licence file — reference only**.

---

## 3. Skill repositories reviewed

Five repos were inspected (README, licence, dependencies, skill files) and recorded with
commit hashes in `exports/test01/manifest.json → licences.vendored_skills`:

| Repo | Licence | Commit | Verdict |
|---|---|---|---|
| **EL4CTEO/blender-skills** | MIT | `71d031d` | **Primary workflow.** Skill structure and headless invocation pattern adopted. |
| ifBars/blender-agent-studio | MIT | `6ff97fa` | Reference for agent/tool decomposition. Not installed. |
| mdj128/facial-animation | MIT | `5bd659d` | Reference for facial scope only; its rig is an add-on, ours is generated. |
| RobLe3/cc-blender-skill | MIT | `a2634b5` | Reference for rig naming/roll conventions. |
| grapeot/gpt_3d_skill | **none** | `d35e84a` | **Not installed, not executed, not redistributed.** No licence = no permission. |

Overlap is high: four of five describe roughly the same workflow (analyse → build → rig →
export) at different levels of abstraction, and none of them contains a character generator.
The useful part was the **shape** of a skill (surface, preconditions, verification step),
not code. Primary workflow = `blender-skills` structure + this repository's own stage
scripts, which are the only ones that actually execute here.

Consequence, stated plainly: installing the five skills unmodified would have produced
prompt files, no character. The value delivered is the executable pipeline beside them.

---

## 4. Image → 3D: what is real and what is not

**What the brief asked for:** accept an image, reconstruct mesh + textures, fall back to a
procedural body fitted to a reconstructed head if full-body is unreliable.

**What exists here:** the fallback path, end to end, and nothing else. ⚠️

- `tools/analyze_photo.py` validates the image, detects the face (Haar), samples 10 regions
  with k-means, and derives proportions in head units (`heads_tall 7.0`, `head_unit_m 0.25`,
  `shoulder_width_in_heads 1.9`, `arm_span 3.6`, `leg_length 3.4` for the test photo).
- `blender/build_character.py` builds a body lofted from those landmarks and a head built
  around the measured face box, then assigns the sampled colours to materials.
- **No learned reconstruction is present.** No model weights, no GPU, no network access to
  a model host; the mainstream options were checked and are unavailable in this sandbox
  (TRIpoSR distribution 404s, no MediaPipe model bundle, no torch/CUDA).
- Textures: none. Materials are flat colours sampled from the photograph. A texture atlas
  would have to be authored or generated; this pipeline does not fake one.

**Therefore: the output is not a likeness.** It is a generic stylised blockout whose *scale
and palette* come from the photograph. The analyser writes this into
`01_analysis.json → limitations`, and the manifest states `photo_identity_match: NO`.

The path to a real reconstruction, when weights and a GPU exist, is a drop-in replacement
of stage 2 only: `analyze_photo.py` still validates and measures, the learned model returns
a head mesh, and `build_character.py` keeps its current job of fitting the procedural body
to whatever head bounds it is given. Nothing downstream of the mesh changes.

---

## 5. Body rig, skinning and deformation

**Rig (33 bones, 31 exported):** root, pelvis, spine, chest, neck, head, eye.L/R, jaw,
clavicle.L/R, upperarm/forearm/hand per side, 4 fingers + thumb per hand, thigh/shin/foot
per side. `root` is a deforming bone on purpose — see §9, defect 8.

**Weights:** distance-based inside mesh islands with three gates — hip (applied to the
*weight*, not the distance), left/right side smoothstep, and a rigid snap for vertices ≥0.80
dominated by one bone — then normalised over 4 influences.

**Verification** (`blender/check_skinning.py`, evaluated-mesh edge stretch, limit 2×,
tolerance 0.5 % of edges):

| Case | Class | Max stretch | Edges over limit | Fraction |
|---|---|---|---|---|
| arm_raise | gate | 1.03× | 0 | 0.0000 |
| arm_forward | gate | 4.37× | 6 | 0.0001 |
| elbow_fold | gate | 6.46× | 28 | 0.0006 |
| knee_bend | gate | 7.45× | 154 | 0.0033 |
| spine_twist | gate | 3.22× | 24 | 0.0005 |
| head_turn | gate | 8.95× | 86 | 0.0018 |
| hip_flex (−55°) | gate | 4.68× | 100 | 0.0021 |
| hip_90_stress (−90°) | **stress** | 6.59× | 349 | 0.0075 |

Result **pass**; 8 cases, 0 gate failures. The high per-edge maxima are single edges at
extremities (a 3 mm edge doubling under a 90° fold), which is why the gate is a *fraction*
of edges rather than a maximum. `hip_90_stress` is recorded as a known limitation: linear
blend skinning cannot hold a 90° hip crease on this topology.

---

## 6. Facial rig and morph targets

18 shape keys (17 + Basis). Every target's real displacement is measured and recorded in
`03_build.json → shape_key_displacement_mm`, and a target under 1 mm is reported as dead
(currently: none).

| Group | Targets | Max displacement |
|---|---|---|
| Jaw / visemes | `jawOpen`, `viseme_AA/E/I/O/U/MBP` | 1.41 – 17.72 mm |
| Expression | `mouthSmile_L/R`, `mouthFrown`, `cheekPuff` | 3.77 – 11.78 mm |
| Brows | `browInnerUp`, `browDown_L/R` | 4.80 – 8.84 mm |
| Eyes | `eyeSquint_L/R`, `Blink` | 2.14 – 15.24 mm |

Bones drive gaze and jaw; morphs drive shape. The two are combined in one action with two
slots, which is what makes a single glTF animation carry both bone and morph channels
(verified: 620 translation / 620 rotation / 620 scale / 13 weight channels in the GLB).

---

## 7. Animation library and speech

21 clips, 24 fps, each its own action and muted NLA track, seeded and deterministic.

| Clip | Frames | Seconds | Clip | Frames | Seconds |
|---|---|---|---|---|---|
| Idle_Breathing | 672 | 27.96 | Talk_Neutral | 240 | 9.96 |
| Idle_LookAround | 288 | 11.96 | Talk_Expressive | 240 | 9.96 |
| Blink_Natural | 90 | 3.71 | Walk | 73 | 3.00 |
| Expression_Set | 336 | 13.96 | Turn | 38 | 1.54 |
| Expression_Happy/Sad/Surprised/Thinking/Curious/Frustrated | 44 each | 1.79 | React_Positive / Negative | 32 each | 1.29 |
| Gesture_Set | 232 | 9.62 | Gesture_Explain / Greet / Point / Emphasize | 47–65 | 1.92–2.67 |

Craft rules actually implemented (not aspirational):

- **Breathing:** 40 % inhale / 60 % exhale; head lags chest 2 frames, spine 4 frames.
  A 1:1 chest/head sway is the classic "dead" idle.
- **Expressions:** counter-pose at f6, peak at f14 with ~6 % overshoot, settle f21, then a
  moving hold at f34/f44 with small head drift — never a frozen hold.
- **Look-around:** eyes lead the head by 3 frames.
- **Gestures:** anticipation dip before the reach, follow-through after, staggered limbs,
  no twinning.
- **Everything returns to rest at the clip end** so the runtime can blend any clip into any
  other without a snap.

**Lip-sync** (`tools/lipsync.py`): `--mode energy` (WAV envelope → viseme timeline,
language-agnostic, 62 keys for the 4 s test) and `--rhubarb-json` (JSON or TSV, mapped
through Rhubarb's own A–H alphabet). Silence yields an **empty** timeline plus
`neutral_fallback: true` rather than weight-0 morphs. ⚠️ **Persian is untested and not
claimed**: Rhubarb's recogniser is English-only, and the energy mode carries no phoneme
identity at all.

---

## 8. Mobile export and the ZIG integration path

**Measured** (`exports/test01/08_export.json`, `09_validate.json`, `manifest.json`):

| Metric | Value |
|---|---|
| GLB size | 1,611,776 B (1.612 MB), budget 4 MB |
| Runtime vertices / triangles | 17,089 / 33,988 (1.99 tris/vert) |
| Joints | 31, all named, `root` present |
| Primitives / materials / textures | 7 / 7 / 0 |
| Morph targets | 17 per primitive, POSITION only (no normal deltas) |
| Animations | 21, all non-empty |
| Channel interpolation | LINEAR on every channel that changes; STEP on constants |
| External URIs | none |
| Validation | **pass**, 21 checks, 0 failed |

Optimisation decisions with their reason: UVs dropped (flat colours, saved ~0.7 MB), morph
normals off (doubles morph cost for no visible gain), deform bones only, normals kept,
`export_optimize_animation_size` on. The master `.blend` is untouched by the export.

**Android** (`avatar_pipeline/android/`): seven Kotlin files in a standalone `com.zig.avatar`
package — GLB reader, model/clip decode, math, player with cross-fade, viseme timeline,
CPU-skinned GLES 3.0 renderer, and a `GLSurfaceView` with a Choreographer frame loop and
reduce-motion support (`ANIMATOR_DURATION_SCALE`, re-read on resume).

- OpenGL **ES 3.0 only**; no Vulkan, no NDK, no C++, no new dependency.
- Nothing outside `avatar_pipeline/` was modified; no manifest, gradle, signing or release
  change. Deletion of the folder is the complete rollback.
- ⛔ **None of it has been compiled or run.** No Android SDK, no device, no emulator. It is
  a reviewed draft with a documented contract (§`android/README.md`), and it must not be
  presented as working app code.

---

## 9. Review passes — what looking at the renders actually found

The review suite exists because the numeric gates passed while the picture was wrong. Each
defect below was found by rendering, then fixed, then re-verified:

1. **Materials wiped.** Assigning skin/cloth called `materials.clear()` *after* the eye,
   brow, lip and hair objects were joined, deleting their slots. The whole face rendered
   as blank skin. Fixed by inserting only missing slots and skipping polygons already on an
   accessory material.
2. **Eyes as blisters.** The eyeball centre sat on the face surface, so a full hemisphere
   protruded. Sunk to 0.30 head-heights — only the front cap shows.
3. **Crown cropped.** Portrait framing centred on the neck joint (the `head` bone's head),
   not the skull centre. Camera now targets `head_z + 0.10`.
4. **Grey face.** 900 W key light clipped every skin highlight to white; sRGB hex was also
   used directly as a linear base colour. Energy cut to 340/110/260 W, colours converted
   to linear, world lifted for fill.
5. **Invisible irises and lips.** The analysis used the *mean* of the eye and lip boxes,
   which is mostly eyelid and cheek skin. Now the darkest k-means cluster of each box is
   used; the iris is darkened 0.62×.
6. **`Turn` folded the character in half.** The clip yawed `root` about its local **Z**, but
   the root bone is vertical, so local Z is world −Y: an 88° face-plant. Caught by a new
   posed-height gate, not by eye.
7. **Pose leakage between clips.** Clips that do not key `root` inherited the previous
   clip's root rotation, which is why `Gesture_Greet` rendered as a collapsed blob after
   `Turn`. Fixed in the review tool (reset before assignment) *and* the runtime contract
   (every clip is evaluated from rest).
8. **`root` missing from the GLB.** `export_def_bones` exported only deform bones and
   `root` was non-deform, so the yaw animation had no node to act on — `Turn` would have
   done nothing on a phone. `root` is now a deform bone; the validator asserts the joint
   exists and that `Turn` keys it.
9. **Expressions did not preview.** Actions are slotted (`OB` bones, `KE` morphs); assigning
   one only to the armature left every facial channel unevaluated, so all six expression
   renders were identical to neutral. The KE slot is now bound to the shape keys as well.
10. **Smile invisible.** Corner lift was ~4 mm (≈4 px). Raised to ~10 mm with a wrap-back
    component so the mouth line curves instead of sliding.

Review coverage: 5 beauty shots (portrait front/¾/side, body front/side/rear) and 23 suite
frames (6 expressions, 9 animation poses, 6 deformation extremes, 2 material). All were
inspected; the full pipeline was re-run after each fix so the committed renders match the
current build.

Visual verdict, unchanged by any of the above: **a crude stylised blockout.** Narrow face,
bulbous cheeks, small eyes, cap-like hair, and the head reads as a separate island sitting
slightly proud of the neck. It is readable as a character at thumbnail size; it is not a
person.

---

## 10. Verification inventory

**Verified by execution (✅):** every pipeline stage; skinning gate; 21-check GLB
validation; 21-clip export; morph displacement measurement; 15 unit tests
(`.venv/bin/python -m unittest tests.test_pipeline_units`, all pass); render suite; manifest
hashing.

**Verified by inspection (✅):** 23 renders looked at, defects 1–10 above found and fixed.

**Not verified (⛔):** Android compilation, rendering, runtime behaviour, GPU compatibility,
memory/thermals on device, animation playback smoothness; Persian lip-sync; any identity
match to the photograph; performance claims (the numbers in `android/README.md` are
arithmetic, not measurements).

**Planned but not done:** GPU/vertex-shader skinning; texture atlas; real learned
reconstruction; motion-capture-quality gestures; a proper hair/skull silhouette; a UI for
choosing clips.

**Test surface** (`tests/test_pipeline_units.py`, 21 tests): GLB magic/version, single skin
+ named joints, morph targets, all 19 required clips, no external URIs, size budget,
validator pass, validator rejects truncation, energy lip-sync monotonic timeline, silence →
empty + neutral fallback, Rhubarb JSON and TSV mapping, rest frames dropped, manifest hashes
+ secret redaction + honesty fields, photo validator rejects a flat image and writes its
reason; plus six art-path tests: silhouette fallback, cascade disagreement is recorded,
alpha PNG does not crash, `--heads-tall` override wins, `--face-box` override, and a
regression test that the photograph path still uses the cascade.

---

## 11. Honest limitations and recommended next steps

1. **Not a likeness.** The mesh is generic. Supplying a photograph later will change
   proportions and palette, not identity. A learned reconstruction is a separate project
   and must be honest about it.
2. **Android is untested code.** Compile it on a real toolchain before showing it to anyone;
   expect typos. Keep it on its own screen, away from the astronomy renderer.
3. **Persian lip-sync is unsolved.** Rhubarb is English-only; the energy mode is
   amplitude-only. Options: a Persian phoneme recogniser, manual timing files, or accept
   amplitude-driven mouth motion and stop calling it lip-sync.
4. **Extremity pinching remains.** Head turn reaches 8.95× on single edges; a 90° hip crease
   is outside what linear blend skinning can do on this topology. Fixing it properly means
   dual-quaternion skinning or corrective shapes, both of which change the runtime.
5. **The head/neck seam is visible.** It is a joined mesh with a separate head island, not a
   welded continuous surface.
6. **Clip count vs clip quality.** 21 clips exist and are structurally sound; their
   *performance* quality is at blockout level. More polish needs more review passes at
   higher resolution, and a person looking at them at 24 fps rather than at stills.

Recommended order of work if this continues: compile and smoke-test the Android package →
replace stage 2 with a real reconstruction when weights are available → weld the head/neck →
dual-quaternion skinning or correctives → Persian speech handling.

---

---

## 12. Professor Z — the supplied character

The brief changed from "a character from a photograph" to "this character": a sheet was
supplied and Professor Z was built from it. The photo path (§4) remains the generic path and
is untouched; the character is a second, authored path through the same rig, morphs, clips,
export and validator. New runner: `./run_authored.sh working/profz blender/build_professor_z.py`.

### 12.1 What was measured, not guessed

The character was rebuilt against `catalogue/profz_sheet_v2.png` (front / side / back
turnaround), not eyeballed against it. The sheet is 1,536 x 1,024; the front figure is
797 px from crown to sole, which at the current scale is **468.82 px per metre**. Every
landmark in `Z{}` and every width in the profiling notes comes from that grid: hair top
1.698, skull top 1.576, brow 1.512, eye line 1.462, nose 1.414, chin 1.331, collar 1.303,
shoulder 1.150, armpit 1.054, tee hem 0.785, hip 0.734, crotch 0.648, knee 0.446, cuff
0.226/0.158, sole 0.0.

Two tools keep the comparison honest, because "it looks close" is not an acceptance test:

- **`blender/render_ortho.py`** renders front / side / back orthographically at exactly the
  sheet's framing and scale (797 px, 468.82 px/m, transparent), so the two images can be
  subtracted instead of described.
- **`tools/compare_sheet.py`** turns both into silhouettes, finds the alignment that
  maximises overlap (not the bounding-box centre — one stray pixel or one limb drawn wider
  shifts the whole figure and makes every row report the same bias), and prints IoU, missed
  and extra pixels, and the worst rows, plus a green/magenta overlay.
- **`tools/render_head_ab.py`** renders 266 x 200 px of head at the sheet's own scale for a
  1:1 face comparison.

Colours are sampled from tight per-feature crops of the sheet rather than k-means
averages: skin `#DE9C63`, hair `#1A1411`, tee `#17181A`, denim `#1D2128`, cuff `#5A626C`,
canvas `#131416`, sole `#EAE5DB`, gold `#C9A24A`, Z glow `#8E6BFF`. All of it lives in the
`HEAD_PROF` / `Z` / `PALETTE` tables of `blender/build_professor_z.py`.

### 12.2 How the mesh is built, and the mistakes that mattered

`build_professor_z.py` lofts the skull, tee, sleeves, arms, pelvis, legs and shoes from
profile tables; places every facial feature **through a surface function** (`face_point`),
builds the hair as a measured silhouette covered in 277 spiral coils, and assigns one
material per part before the join so boundaries stay exact.

Each of these came from an inspected render, not from theory:

- **A fixed `y` does not work on a curved face.** The mouth was modelled at a constant depth
  and ended up 10 mm *inside* the cheek; the eyes vanished behind the skin. Everything is now
  placed with `face_point(x, z, out)`, and `out` stays in the 1–9 mm range.
- **A sphere in a socket does not read.** Two attempts produced eyes that were invisible or
  looked like beads. The current eye is a shallow almond disc set proud of the surface, with
  iris, pupil, catch-light and a skin hood over the top.
- **A tube along a smile curve breaks into three pieces.** The cheek turns away faster than a
  tube of constant radius can follow, so the corners detach. The mouth is a lens-shaped
  `conformal_patch` that follows the surface edge to edge and tapers to nothing.
- **Tori are not curls.** 900 of them read as a pile of doughnuts with holes. `char_lib.coil()`
  spirals a tube for 1–2 turns and, placed along the surface normal, reads as hair from every
  angle. The shell under them is displaced by a few millimetres so the mass is lumpy rather
  than a swim cap.
- **A tube's flat end cap is a disc.** At the shoulder it stuck out as a triangular fin on
  both sides; the sleeve now starts inside the torso with a small radius.
- **Gaussian sculpt fields need sigma >= 0.03.** At 0.009–0.014 the kernel lands on single
  vertices and the nose came out M-shaped. Sigma below ~0.03 is banned.
- **Draw calls are a mobile budget.** glTF makes one draw call per material and the validator
  caps at 16; merging near-duplicate materials (brow into hair, lace into sole, watch and
  tee-dark into canvas) brought the GLB from 17 primitives to **14**.

### 12.3 Shape keys and the jaw

The generic `add_shape_keys` places brow/eye/mouth weights at positions calibrated for its
own head, so Professor Z passes its own landmark dict in head-heights. Seventeen of eighteen
morphs move real geometry (jaw 0.4 mm to viseme_AA 18.4 mm). **`jawOpen` is recorded as dead**
and the report says so rather than hiding it: the jaw bone exists and drives the chin, but the
morph itself no longer has displacement after the head was re-lofted. Lip-sync uses the six
viseme morphs (6.5–18.4 mm), which do move.

### 12.4 Build and pipeline result (executed, this build)

| Metric | Value |
|---|---|
| Mesh | 39,627 verts, 41,378 polys, 77,578 tris, 33 bones (31 deform), 18 shape keys |
| Height | 1.696 m; dimensions 0.509 x 0.342 x 1.696 m |
| Hair | 277 coils on a measured mushroom profile |
| Materials | 14 (skin, hair, eye white, iris, pupil, lips, tee, denim, cuff, canvas, sole, gold, Z glow, Z core) |
| Skinning gate | **pass**; weight-discontinuous edges 2 of 80,292; worst-case stretch is confined to 13–26 edges (0.02–0.03 % of the mesh) at knee/hip/head limits |
| Clips | 21, all non-empty, NLA tracks present |
| GLB | **3.16 MB**, validation **pass**, 0 failed checks, 14 primitives |
| Sprite | 16 frames of `Idle_Breathing` at 160 x 240, 2.5 s cycle, 484 KB |

Renders: `renders/profz/ortho/*.png` (front / side / back at sheet scale),
`renders/profz/head/*.png` (1:1 head), `renders/profz/sheet_vs_model.png` (sheet beside
model for all three views) and `renders/profz/turnaround_model.png`. The proportion fixes in
this build — the sleeve fin, the pigeon toes, the nappy-shaped pelvis, the oversized Z, the
dark pocket "tiles", the claws that were fingers — each came from looking at one of those.

### 12.5 How the character reached the app

The original placeholder is **`ObserverRenderer`** — a vector figure on the grass line of the
Home sky scene, posed from the Sun's altitude, drawn about a dozen times a frame. It is
replaced by the real character **baked to a sprite strip** rendered from the same `.blend`
(16 frames of `Idle_Breathing`, 160x240 each, transparent, character facing the middle of the
scene), drawn by the same Compose Canvas that draws the sky.

- Why not the GLB at runtime: `avatar_pipeline/android/` is a complete GLES 3.0 runtime, but a
  GLSurfaceView composited into a Compose hero inside a scrolling card fails invisibly (black
  surface, z-order, context loss) and **there is no device or emulator here to catch that**.
  The strip cannot regress the frame budget, cannot lose its context, and is the same model.
- Files touched in the app: **two** — `SkySceneRenderer.drawSceneMotion` (try the character,
  fall back to the vector observer if the strip is not loaded) and `HeroSkyCanvas` (decode the
  strip once for the scene). New: one file, `ui/character/HeroCharacterSprite.kt`, and one
  drawable, `res/drawable-nodpi/hero_profz_idle.png` (484 KB, 2560 x 240 = 16 frames).
- Rollback: `HeroCharacter.ENABLED = false` in that one file restores the previous behaviour
  exactly. No gradle, manifest, signing, AR or astronomy file was touched.
- ⛔ **Kotlin not compiled, app not run.** Same reason as §8: no SDK, no device, no emulator.

### 12.6 Verified / untested / planned

**Verified:** the build runs end to end; the skinning gate passes; 21 clips exist and animate
bones and morphs; the GLB validates; the review renders were inspected; the sprite strip was
composited over a mock sky at the real on-screen size (106 px tall) and reads as a figure.

**Untested:** anything on Android; the 3D figure at runtime; Persian speech; performance of the
extra 488 KB drawable.

**Planned:** the three observer poses (shading eyes / phone / looking up) are not in the baked
character — the one-shot candidates read as nothing at 90 px, so the figure ships with its idle
and the vector figure keeps the pose logic for when the strip is disabled; a device test of the
GLES runtime; a `jawOpen` morph that displaces geometry again; and a hair build that costs
fewer vertices than 277 separate coils at full body scale.

**Still true, and it must stay said:** this is a **stylised interpretation of the supplied
art**, not a likeness of any real person, and the app integration is **not tested on a device**.

---

## Reproduce

```bash
cd avatar_pipeline
./run_authored.sh working/profz blender/build_professor_z.py        # Professor Z, ~43 s
./run_pipeline.sh working/test01                                  # ~55 s
.venv/bin/python -m unittest tests.test_pipeline_units -v         # 15 tests
```

Artefacts: `working/test01/{01_analysis,03_build,05_skin,06_anim,07_visemes}.json`,
`character.blend`, `character_anim.blend`; `exports/test01/{avatar.glb,08_export,09_validate,manifest}.json`;
`renders/test01/*.png` and `renders/test01/suite/*.png`. Hashes and versions in
`exports/test01/manifest.json`.

The default input is the small test portrait in `tests/fixtures/` (recorded in
`01_analysis.json → image`), used as a stand-in until a real photograph is supplied; the
manifest names it and no claim is made about that subject. No credential is exposed, and no
third-party model weights were downloaded or bundled.

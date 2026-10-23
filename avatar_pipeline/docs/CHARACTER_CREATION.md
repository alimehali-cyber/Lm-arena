# Creating a character with this pipeline

A practical guide to what the pipeline does, what each stage decides, and where to intervene.
For measured results and honest limits see `reports/FINAL_REPORT.md`.

## 0. Requirements

- Linux, no GPU required, no root.
- Blender 5.0.x (the version used here is recorded in every report's `versions` block).
- Python 3.11 with `numpy`, `opencv-python-headless` in `avatar_pipeline/.venv`.
- `tools/headless/{blender_env.sh, build_stubs_fixpoint.py, stublibs/}` — Blender's bundled
  `bpy` needs GL/X11 libraries the sandbox does not have. The stubs supply exactly the
  SONAMEs it links against; `libEGL` is deliberately never stubbed.

```bash
cd avatar_pipeline
source tools/headless/blender_env.sh   # AVATAR_BLENDER_PY, LD_LIBRARY_PATH, threads
```

## 0a. If the character is an authored design (the Professor Z path)

When a character sheet states the character, do not route it through the analyser: the
analyser exists to *infer* proportions, and here they were drawn. Write a builder that encodes
the measurements and hand it to the same stages:

```bash
./run_authored.sh working/profz blender/build_professor_z.py      # ~43 s
```

`run_authored.sh` is `run_pipeline.sh` without stages 1–2: build -> skinning gate -> 21 clips
-> review renders -> review suite -> export -> validate -> manifest. Everything downstream of
the mesh is shared, which is the point — a new character is a new mesh, not a new pipeline.

Rules that were learned the hard way and now hold for any authored character:

1. **No voxel remesh on a clothed body.** It unions every shell and re-derives materials by
   nearest label; the jacket becomes the shirt. Build parts as closed surfaces and assign one
   material per part *before* `join()`. Blender remaps the slots correctly; re-deriving after
   the join never worked.
2. **A profile table is not in metres.** `head_profile()` returns fractions of the half width
   and depth. Multiplying the raw values builds a one-metre head and the hair vanishes.
3. **An angular window needs a wrap rule.** If the window closes (cut = 0) the ring must wrap;
   otherwise the shell has a slit down the front that reads as a notch in the hairline.
4. **Morph landmarks are per-character.** Pass `add_shape_keys(..., landmarks={...})`; dead
   morphs are usually wrong landmarks, not bad weights.
5. **Keep the skeleton names.** `weight_by_anatomy` gates bones per island
   (`core` / `arm.L` / `arm.R`), and the 21 clips address `upperarm.L` etc. by name. Build the
   mesh around the joints you pass to `build_armature`, never the other way round.

The sprite the app uses is baked from the animated file:

```bash
source tools/headless/blender_env.sh
$AVATAR_BLENDER_PY blender/bake_hero_sprite.py \
    --blend working/profz/character_anim.blend --outdir /tmp/strips --res 160x240 --samples 10
# then concatenate the frames in /tmp/strips/_frames with opencv (cv2.hconcat) into
# app/src/main/res/drawable-nodpi/hero_profz_idle.png
```

Two measurement tools exist so that "it looks close" is never the acceptance test:

```bash
# silhouette agreement against the sheet, row by row (IoU + worst rows + overlay PNG)
.venv/bin/python tools/compare_sheet.py --render renders/profz/ortho/ortho_front.png \
    --view front --out /tmp/overlay_front.png

# the head at exactly the sheet's 468.82 px/m, for a 1:1 face comparison
.venv/bin/python tools/render_head_ab.py --blend working/profz/character.blend \
    --out renders/profz/head/head_ab.png
```

`compare_sheet.py` aligns by best silhouette overlap, not by bounding box: a single stray
pixel or one limb drawn a few pixels wider shifts the whole figure and every row then
reports the same bias.

The measured fractions that make the sprite stand at the right height in the scene are in
`HeroCharacterSprite.kt` (body = 86.25 % of the frame, soles at 94.17 %).

## 0b. If the input is concept art, not a photograph

Drawn characters are supported, with honest limits. Two extra flags exist because a
drawing does not obey human proportions:

```bash
# a 4-head-tall stylised character, 1.4 m in world units
.venv/bin/python tools/analyze_photo.py --image art.png --out 01_analysis.json \
    --heads-tall 4 --height-m 1.4

# if detection still lands in the wrong place, give it the head box directly
.venv/bin/python tools/analyze_photo.py --image art.png --out 01_analysis.json \
    --face-box 168,60,84,144        # x,y,w,h in pixels (or 0..1 fractions)
```

What the analyser does with art:

1. If the PNG has real alpha, the character mask comes from the alpha channel.
2. Otherwise, if the image border is uniform, the background is modelled from it and the
   largest connected component is taken as the character. A photograph fails this test on
   purpose (`silhouette.source = "uneven_background"`) and falls back to the cascade.
3. A cascade face on a drawing is **not trusted blindly**: it is rejected (and the reason
   recorded in `rejected_faces[]`) if it is implausibly large, sits too low, falls outside
   the silhouette, or disagrees with the silhouette's head size by more than ~30 %.
4. Colours are sampled **only inside the mask**, so a box that overhangs the figure cannot
   pick up the background.

Limits, stated plainly: this reads a silhouette, a palette and a head count. It does **not**
reproduce the drawing's face shape, hair style or body build — those stay generic. Ten
different drawings produce ten differently-coloured versions of the same blockout.

## 1. Supply a photograph

One photo, ideally:

- front-facing, eyes visible, neutral expression
- at least 512 px on the short side (the analyser rejects < 160 px)
- reasonably even lighting; strong side light shifts the sampled skin tone
- **no** sunglasses, masks, extreme head turns, or heavy filters

Put it anywhere and pass the path. Nothing is uploaded anywhere: the photo never leaves
the machine, and no network call is made by any stage.

```bash
.venv/bin/python tools/analyze_photo.py --image /path/to/photo.jpg \
    --out working/test01/01_analysis.json
```

The analyser writes a report and **exits non-zero if the photo is unusable** (too small,
blank, absurd aspect). It does not "guess a face".

## 2. What the analysis decides

| Field | How it is measured | Used for |
|---|---|---|
| `face` | Haar cascade face box | head width/height, feature placement |
| `proportions.head_unit_m` | head height ÷ assumed stature | all proportional body landmarks |
| `colours.skin` | mean of forehead + both cheeks (k-means dominant) | skin material |
| `colours.iris` | darkest k-means cluster of each eye box | iris material |
| `colours.brow` | darkest cluster of the hair box | brow material |
| `colours.lips` | darkest cluster of the lip box | lip material |
| `colours.clothing_chest` | chest region | shirt material |
| `limitations[]` | written by the analyser | carried into the report, never dropped |

`estimated_person_metres` defaults to 1.75 m because a single photo has no metric
reference. If you know the subject's height, pass it and every landmark scales.

## 3. Build

```bash
$AVATAR_BLENDER_PY blender/build_character.py \
    --analysis working/test01/01_analysis.json \
    --out working/test01/character.blend \
    --report working/test01/03_build.json
```

The builder makes three things at once:

1. **Mesh** — a lofted body from measured landmarks, separate arm islands, a head built
   around the measured face box (skull, jaw, cheeks, nose, brow ridge, lips, eyes, hair
   cap), then joined. 17,051 vertices / 29,539 polygons.
2. **Armature** — 33 bones: root, pelvis, spine, chest, neck, head, eye.L/R, jaw,
   clavicles, upper arms, forearms, hands, four fingers + thumb per hand, thighs, shins,
   feet. 31 are deforming and therefore exported.
3. **Morph targets** — 18 including `Basis`: jaw, six visemes, smile L/R, frown, cheek
   puff, brow up/down, squint L/R, blink.

### Skinning rules that were learned the hard way

The weights are distance-based inside a mesh island, with three gates:

- **hip gate** — multiplies the *weight* by a smoothstep of height, never the distance.
  Scaling distance creates a step at the handover and shears the crease.
- **side gate** — a smoothstep in ±x stops the left thigh from claiming midline vertices
  that belong to the right foot.
- **rigid snap** — a vertex ≥ 0.80 dominated by one bone is snapped to 1.0. Small weight
  gradients on 1–3 mm edges amplify with lever arm at the extremities.

Change these only with `check_skinning.py` in front of you; all three exist because a
specific pose tore.

## 4. Verify deformation before animating

```bash
$AVATAR_BLENDER_PY blender/check_skinning.py \
    --blend working/test01/character.blend --report working/test01/05_skin.json
```

It poses the rig and measures **edge stretch on the evaluated mesh**: an edge that grows
more than 2× is a tear. Cases are split into:

- `gate` — inside the motion the shipped clips actually use; more than 0.5 % of edges over
  the limit fails the build.
- `stress` — deliberately past the shipped range (`hip_90_stress`); reported as a known
  limitation, not gated.

Exit code is non-zero on failure, so `run_pipeline.sh` stops.

## 5. Animate

```bash
$AVATAR_BLENDER_PY blender/animate_character.py \
    --blend working/test01/character.blend \
    --out working/test01/character_anim.blend --report working/test01/06_anim.json
```

21 clips, 24 fps, one action each, one muted NLA track each. Bones and morph channels live
in the same action but in different **slots** (`OB` for the armature, `KE` for the shape
keys) — a Blender 5 detail that matters when you play a clip outside the exporter.

Craft rules encoded in the clips (see `reports/FINAL_REPORT.md` §5 for the reasoning):

- breath is 40 % inhale / 60 % exhale, head lags chest by 2 frames, spine by 4
- expressions counter-pose, overshoot ~6 %, settle, then hold with a small drift
- eyes lead the head by 3 frames in `Idle_LookAround`
- gestures anticipate before the reach and follow through after it
- every clip returns to rest at its end so it can be blended by a runtime

## 6. Lip-sync

```bash
# amplitude only, language-agnostic
$AVATAR_BLENDER_PY tools/lipsync.py --audio voice.wav --out 07_visemes.json --mode energy

# phoneme-accurate, needs Rhubarb Lip Sync installed separately (English only)
$AVATAR_BLENDER_PY tools/lipsync.py --rhubarb-json rhubarb.json --out 07_visemes.json
```

The output is `{frame, t, viseme, weight}` with `viseme` naming a morph target that the
model actually has. Silence produces an empty list plus `neutral_fallback: true`, not a
weight-0 morph.

**Rhubarb is English-only.** Persian speech through it is untested and not claimed; the
honest options are a corrected manual timing file or the energy mode, which carries no
phoneme information at all.

## 7. Render the review set

```bash
$AVATAR_BLENDER_PY blender/render_review.py --blend working/test01/character_anim.blend \
    --outdir renders/test01 --shots body_front,body_side,body_rear,portrait_front,portrait_34 \
    --samples 20 --res 340

$AVATAR_BLENDER_PY blender/review_suite.py --blend working/test01/character_anim.blend \
    --outdir renders/test01/suite --mode all --samples 20 --res 340
```

Cycles CPU only (no EEVEE, no GPU in this environment). 3 area lights, `sensor_fit="VERTICAL"`,
and a solved camera distance — do not hand-place the camera, it will crop.

Look at the images. The suite exists because numbers pass while renders look wrong; that
happened repeatedly in this project.

## 8. Export and validate

```bash
$AVATAR_BLENDER_PY blender/export_mobile.py --blend working/test01/character_anim.blend \
    --out exports/test01/avatar.glb --report exports/test01/08_export.json

.venv/bin/python tools/validate_glb.py --glb exports/test01/avatar.glb \
    --report exports/test01/09_validate.json
```

The validator is stdlib-only and fails on: accessor bounds, more than one skin, unnamed
joints, missing `root` joint, missing `JOINTS_0`/`WEIGHTS_0`, morph name/count mismatch,
fewer than 19 required clips, empty animations, no morph or bone channels, a `Turn` clip
that does not key `root`, external URIs, or > 4 MB.

## 9. Record and hand over

```bash
.venv/bin/python tools/manifest.py --root . --id test01 --out exports/test01/manifest.json
```

The manifest hashes 10 artefacts, records versions, lists a licence per asset class, and
states `android_runtime = NOT TESTED` and `photo_identity_match = NO`. Those two strings
are part of the contract: do not soften them without new evidence.

## 10. Changing the character

| You want | Edit | Then |
|---|---|---|
| different proportions | `01_analysis.json` (`estimated_person_metres`) | build → skin → animate → export |
| different colours | `01_analysis.json` `colours` | build → export |
| new expression | `add_shape_keys()` in `build_character.py` + a clip in `animate_character.py` | build → animate → export → validate |
| new clip | `animate_character.py` clip table | animate → export → validate |
| stronger deformation | weights in `build_character.py`, then **always** `check_skinning.py` | full pipeline |

After any change: `./run_pipeline.sh working/test01` and
`.venv/bin/python -m unittest tests.test_pipeline_units`. Both must pass before renders
are trusted.

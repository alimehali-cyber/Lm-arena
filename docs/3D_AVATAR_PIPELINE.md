# The observer, and the road from silhouette to a 3D avatar

**Scope:** a precise account of the character placeholder on the Home scene — what it is,
what drives it, and where its seams are — plus the avatar pipeline installed in this
workspace for the next step: turning reference photos of a character into a rigged,
animated 3D figure that can eventually stand on the same grass line.

The scene itself (composition, layers, clocks) is documented in
[`HOME_SKY_SCENE.md`](HOME_SKY_SCENE.md). This page starts where that one stops: at the
figure.

---

## 1. How the observer works today

The character is `ObserverRenderer` (`ui/rendering/ObserverRenderer.kt`): one small
vector silhouette, ~a dozen draw calls per frame, drawn as the last step of
`SkySceneRenderer.drawSceneMotion` after the cloud veils. It is scenery with a
meaningful input and a deliberate budget — never a sprite, never a photo.

Four things drive it, all read from the same scene frame:

| Input | Source | Effect |
|---|---|---|
| Sun altitude | `frame.lightingState.sunAltitudeDeg` | The pose: `> 3°` SHADING_EYES, `> −7°` PHONE (twilight), else LOOKING_UP (night). Pose is chosen from astronomy, never weather. |
| Frame time | the motion clock (`motionTimeMs`) | A 5.2 s breath (scale ±0.4%, head tilt) and a 17 s weight shift (0.35° rotate, feet drift 0.4% of height). The periods are co-prime so the two motions never visibly re-align. |
| Theme + lighting | `figurePalette(theme, lighting)` in `ScenePalette.kt` | Jacket, trousers, cap, skin and rim-light colours; the rim copies the sky's catch-light. |
| Composition | `SkySceneLayout.composition()` | Position: `figureCenterX = 0.745 × width` (mirrored in RTL via `figureOnRight = !isFa`), feet on the grass line (`groundTopY + 0.008 × height`), height `0.190 × scene height`. |

Construction order per frame: contact shadow (two alpha ovals, strength from ambient
brightness) → back arm → legs + shoes → torso (quadratic- bézier path with a vertical
gradient, collar line, rim stroke) → head (neck, skull circle, cap dome + brim, tilt by
pose) → front arm → phone (twilight pose only, glow scales with darkness). Arms are
quadratic strokes with round caps; the "front" arm is on the facing side so it reads on
top of the torso.

**The seams, in the order they will matter:**

1. *One fixed body.* Proportions, clothing and stance are compiled in. There is no
   notion of a different character.
2. *Three poses, hand-authored.* Each pose is a set of elbow/hand offsets — there is no
   animation system, no timeline, no interpolation between poses (the pose switches
   discretely as the Sun crosses a threshold).
3. *2D only.* The figure cannot turn, walk or be seen from another side. The scene's
   projection is a cylinder, but the figure is a flat billboard that always faces the
   camera.
4. *No feet on the Moon card overlap.* The card rides 96 dp over a 132 dp height, so
   only ~50 dp of ground strip is visible; the figure is readable only because it is
   small.

## 2. The avatar pipeline (workspace, not the app)

For turning character reference photos into rigged, animated 3D figures, this workspace
now runs a headless 3D lab at `/home/user/avatar-lab` — the skills of
[image-to-3dlab](https://github.com/Bingeljell/image-to-3dlab) (Apache-2.0) adapted to a
CPU-only sandbox. See `avatar-lab/SKILLS.md` there for the full manual. In short:

- **Blender 4.2 LTS runs headless** as a Python module (`bpy`), rendering on Cycles CPU.
- A **photo analyser** measures a reference photo's silhouette proportions (height,
  shoulder/hip/knee widths, head height, leg length) and per-band palette (hair, face,
  top, bottom, shoes) — no ML, no GPU.
- A **builder** turns that spec into a stylised, rigged character (19-bone humanoid
  armature; the retargeter matches bones by tree shape, so the names are free).
- **NVIDIA Kimodo motion clips** (21 presets: idle, walk, run, wave, dance, …; upstream
  retargeting skill, pure numpy) animate any humanoid rig, with rest-pose alignment,
  hip-height scaling and arm clearance.
- **Renderers** emit cardinal stills, a seamless turntable MP4, and a tracking-camera
  animation MP4.

The heavy ML image→3D backends (Pixal3D, TRELLIS.2, Hunyuan3D) need a GPU and 8–20 GB of
HuggingFace weights; they are studied and documented but cannot run here.

## 3. Integration options, when there is an avatar

The scene keeps three drawing layers with three clocks; a 3D avatar can enter without
breaking that architecture. Options, cheapest first:

1. **Pre-rendered sprite strip** — render the avatar's move (idle/look-up/shade-eyes to
   match today's poses, plus any new ones) to a PNG strip in the lab, ship it as an
   asset, and draw it where `ObserverRenderer.draw` sits today, on the same motion
   clock. No new dependencies, works with the theme palette via tinting; the figure
   stays 2D-in-space but becomes a real character.
2. **glTF in a Compose view** — keep the animated GLB and play it with a small glTF
   runtime (e.g. Filament). Full 3D, but a new native dependency and a second render
   pipeline beside the canvas scene.
3. **Bake per-frame vectors** — a middle path: sample the avatar's animated silhouette
   back into the vector language `ObserverRenderer` already speaks. Most faithful to
   the scene's existing cost model, most work per pose.

Whichever route, the contract to keep is the one the observer already honours: pose
follows the real Sun altitude, motion rides the scene clock at ~24 fps, the figure
never repaints the astronomy, and its palette keeps answering the theme.

## 4. What shipped: Professor Z, sprite route (option 1)

The observer is no longer the fixed vector silhouette: it is **Professor Z**, a character
built in the avatar lab from three uploaded reference photos (a ~3.3-heads-tall professor:
dark dome hair, full beard with mustache and bushy brows, brown jacket, grey trousers,
brown boots). Route 1 was taken, exactly as scoped above:

- **`profz_observer_sheet.png`** (`res/drawable-nodpi/`, 2304×1152): 3 rows × 12 breath
  frames, 192×384 cells. Rows follow `ObserverPose` order — SHADING_EYES, PHONE,
  LOOKING_UP — each procedurally posed on the rigged GLB (hand to the brow, phone in both
  hands, arm to the sky) with a subtle chest-lift breath cycle and the feet planted, so
  the 12 frames step cleanly on the scene clock.
- **`ObserverSprites.kt`** decodes the sheet off the UI thread and exposes it as one
  `ImageBitmap`; `rememberObserverSprites()` owns the lifetime, the
  `SkyMoonSprites` pattern.
- **`ObserverRenderer.kt`** draws the sheet's cell — row from the Sun altitude, column
  from the 5.2 s breath phase — at the composition's feet/height (the cell frames 1.87 m
  of world for the professor's 1.493 m, so `figureHeightPx` still means *the figure*).
  The vector silhouette remains as the fallback until the sheet lands. Everything else
  the observer promised is kept: contact shadow, 17 s sway, the breath that rides the
  scene clock, the twilight phone glow drawn live over the sheet's phone. The palette
  still answers the theme: as ambient light falls the sheet is multiplied down toward
  the scene's blue dark (a `ColorFilter.tint` multiply, ~30 % at full night).

The character himself lives in the lab: `output/profz.glb` (19 bones, single joined
body), Kimodo-retargeted `profz_walk/wave/idle.glb`, tracking-camera MP4s of all three,
and the flat-albedo QA render that verified every colour band against the photo spec
(hair, skin, eyes, mustache, beard, jacket, trousers, boots — all within tolerance;
see `avatar-lab/skills/qa_sample.py`).

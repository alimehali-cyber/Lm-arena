# Home scene, sky canvas and the character placeholder — how it actually works

**Read for:** the avatar programme. Everything below is read from the source, not from the
design notes. Line references are to commit `9856299`; the numbers are the ones the code
really uses.

Files that matter:

| File | Role |
|---|---|
| `ui/screens/HomeScreen.kt` | the screen: scene band, header, readout, Moon card, feed |
| `ui/components/HeroSkyCanvas.kt` | the canvas, its clocks, gestures, readout, interaction layer |
| `ui/rendering/SkySceneRenderer.kt` | draw order: sky pass, then motion pass |
| `ui/rendering/SkySceneModel.kt` | `SkySceneFrame` — everything one instant needs |
| `ui/rendering/SkySceneComposition.kt` | `SkySceneLayout` + `SceneComposition` (pixel geometry) |
| `ui/rendering/SkyProjection.kt` | `SceneProjection` / `HeroProjection` (azimuth, altitude → px) |
| `ui/rendering/SceneGroundRenderer.kt` | ridges, mist shelf, ground band, grass |
| `ui/rendering/CloudVeilRenderer.kt` | three drifting veils |
| **`ui/rendering/ObserverRenderer.kt`** | **the character placeholder** |
| `ui/rendering/ScenePalette.kt` | `ScenePalette` and `FigurePalette` per theme |

---

## 1. Where the scene sits on Home

`HomeScreen` is one `LazyColumn`. The scene is **not** an item inside it: the height has to be
known before the list is measured (lazy children are measured with unbounded height), so
`HomeScreen.kt:316` computes

```kotlin
val sceneHeight = (maxHeight * 0.56f).coerceIn(320.dp, 600.dp)
```

and `HomeSkyHero` (`HomeScreen.kt:1044`) owns a `Box` of
`height = sceneHeight + cardHeight - cardOverlap` = `scene + 132.dp - 96.dp`. The scene itself is a
full rectangle at the top of that box; the Moon card (`MoonPhaseCard`) is aligned to the bottom and
**rides 96 dp up over the scene**, so only a strip of ground stays visible. That overlap is the
whole trick: the sky owns the top of the phone, the ground is a shelf the first card sits on.

Inside the box, in z-order:

1. `HeroSkyCanvas(presentation = SkyCanvasPresentation.SCENE)` — `fillMaxWidth().height(sceneHeight)`, edge to edge, from y = 0 (behind the status bar);
2. a top scrim, `sceneHeight * 0.34f` tall, black 0.38 → 0.14 → transparent, so drifting clouds never eat the header's contrast;
3. the header column (`statusBarsPadding()`): favourites, "ZIG", search, settings, then the location pill;
4. the expand affordance, bottom-end, padded by `cardHeight - cardOverlap` so it clears the card;
5. the Moon card.

**Consequence for the avatar:** the character is drawn *inside* `HeroSkyCanvas`, under the header
and under the Moon card. It has no window of its own and gets no input priority — the canvas'
tap and horizontal-drag gestures are on the outer `BoxWithConstraints`.

---

## 2. The sky canvas

### 2.1 Three draw nodes, three clocks

`HeroSkyCanvas` (`HeroSkyCanvas.kt:120-230`) stacks three `Canvas` calls, deliberately:

| Layer | Draws | Repaint clock | Code |
|---|---|---|---|
| 1 Sky | atmosphere, Milky Way, stars, Sun, Moon, planets, ground | shared 1 Hz clock; per frame only while scrubbing | `SkySceneRenderer.drawSky` |
| 2 Motion | cloud veils + the observer (scene only) | ~24 fps (`SCENE_MOTION_FRAME_MS = 42L`) | `SkySceneRenderer.drawSceneMotion` |
| 3 Interaction | selection ring, stardust | display rate, only while touching | inline in the canvas |

Two clocks exist as state: `skyTimeMs` (fed by `MainViewModel.skyLiveClock`, 1 Hz) and `motionTimeMs`
(fed by `withFrameMillis` or the 42 ms loop). The loop at `HeroSkyCanvas.kt:180` runs `while
(interactive || isScene)`; when nothing is touched it *only* updates `motionTimeMs`, so the
astronomy is never re-rendered for a drifting cloud.

The **model** is quantised once more: `SKY_MODEL_QUANTUM_MS = 15_000`. A `remember` keyed on the
15-second bucket computes `SkySceneModel.compute(jd, lat, lon, elev)` (Moon ELP2000, every planet,
every drawn star < mag 4.5, the galactic plane, eclipse flags, lighting state, the AR Moon geometry).
15 s of sky rotation is 0.06° — sub-pixel on this surface.

### 2.2 Geometry: one composition everyone agrees on

`SkySceneLayout.composition(w, h)` (`SkySceneComposition.kt:47`) returns `SceneComposition`:

| Constant | Value | Meaning |
|---|---|---|
| `HORIZON_FRACTION` | 0.70 | row where altitude 0° lands, inside the scene |
| `GROUND_TOP_FRACTION` | 0.052 | solid ground starts this far below the horizon |
| `FIGURE_HEIGHT_FRACTION` | 0.190 | the observer's height, as a share of the scene |
| `FIGURE_FOOT_DROP_FRACTION` | 0.008 | feet sit this far below the grass line |
| `FIGURE_CENTER_FRACTION` | 0.745 | observer centre x (mirrored when drawn on the left) |
| `FAR_RIDGE_FRACTION` / `MID_RIDGE_FRACTION` | 0.034 / 0.020 | ridge amplitudes |

With `horizonY = h·0.70` and `groundTopY = horizonY + h·0.052`, the figure's feet land at
`h·0.760` and the figure is `h·0.19` tall — head at `h·0.57`, roughly a third of the way down from
the top of the scene. On a 400 × 900 px scene that is 171 px of character.

### 2.3 Projection

`SkyRenderTarget.HOME_SCENE.projectionFor(frame)` returns `SceneProjection(0.70)`
(`SkyProjection.kt:66`), which delegates to `HeroSkyProjection.project(...)` — the same cylindrical,
equator-facing mapping the card and the fullscreen viewer use, with the horizon as an argument
instead of the 0.85 constant. `clipBottomPx(h) = 0.70h` hides everything under the horizon;
`horizonY(w, h) = 0.70h` is read by the atmosphere gradients, the ground band, the mist shelf and the
cloud clip, so no layer can keep a stale horizon line.

### 2.4 Ground and clouds

`SceneGroundRenderer.draw` (called from `drawSky`, step 7) paints, in order: far ridge (summed sines
sampled with quadratics) → mist shelf gradient → mid ridge → solid ground gradient → horizon
catch-light → deterministic grass tufts → bottom fade. Papercraft gets hard edges plus a 1.4 px cut
stroke; other themes get the soft version.

`CloudVeilRenderer` pre-renders two soft alpha sprites once, then draws three veils as tinted images:
cirrus (y 0.20, 2.4 px/s), mid (y 0.44, 6.5 px/s), low (y 0.74, 11.0 px/s). `drawSceneMotion` clips
them to `rect(0, 0, w, horizonY)` so they never spill onto the ground. Strength comes from
`ScenePalette.cloudStrength` and fades at night. **They are atmosphere, never weather.**

### 2.5 What the user can do to the canvas

- **Tap** (`detectTapGestures`): 36 dp radius search over Sun, Moon, planets, catalog stars,
  projecting each with the *same* `horizonFraction`; sets `selectedCelestial`, which auto-dismisses
  after 5 s. The name pill is only shown in `VIEWER`, not in `SCENE`.
- **Horizontal drag**: `deltaHours = -(dragAmount / width) * 24`, clamped to ±12 h, animated back to 0
  after 5 s idle (`tween(2000)`), with 3 stardust particles per drag event. The offset is published to
  `MainViewModel.setSkyDragOffsetHours` so the Live Sky backdrop follows the same instant.
- **Expand** (header) opens the fullscreen `VIEWER` presentation; `HeroSkyCanvas.onExpand` is null in
  the scene.

### 2.6 The readout

Not canvas drawing — a Compose `Column` (`HeroSkyCanvas.kt:544`) aligned `BottomStart`, padded
`bottom = maxHeight * (1 - horizonFraction) + 18.dp`, max width 62 %: time at 46 sp (Light), the phase
of day from the real Sun altitude (`skyConditionLabel`: Daylight > 6°, Golden hour > 0°, civil > −6°,
nautical > −12°, astronomical > −18°, otherwise Astronomical night), then `date · Moon n% up`.

---

## 3. The character placeholder

`ui/rendering/ObserverRenderer.kt` — one vector figure standing on the grass line. It is the slot the
avatar replaces, so it is worth knowing exactly.

### 3.1 It is a 2D drawing, not a model

- Drawn entirely with `Path` / `drawLine` / `drawCircle` / `drawOval` / `drawRoundRect`.
- ~12 draw calls per frame, one unit `s = composition.figureHeightPx`.
- Origin is the **feet**: `translate(feet.x, feet.y)`, then everything is negative Y (`hipY = −0.455 s`,
  `shoulderY = −0.762 s`, head centre `−0.888 s`, cap above that). Body parts:
  contact shadow (two ovals) → back arm → legs (two round-cap lines, 0.074 s wide, plus shoe ovals) →
  torso (quadratic path + vertical gradient + collar line + rim stroke) → head (neck line, circle, rim
  stroke, cap arc + brim oval, rotated by `tilt` at the neck pivot) → front arm → optional phone.
- Colour comes from `figurePalette(theme, lighting)` in `ScenePalette.kt:165`: jacket, trousers, skin,
  shoe, cap, rim, each lerped between a night and a day value by `lighting.ambientBrightness`, per
  theme (six themes). The rim stroke is the only thing that separates the figure from a dark sky.
- Shading is a vertical gradient on the torso and a rim line. There is no light direction, no
  occlusion, no material.

### 3.2 What it does

Two sines on a wall-clock time wrapped to one day (`(frameTimeMs % 86_400_000) / 1000.0`, in Double so
the phase cannot degrade):

- **breath**, period 5.2 s: ±0.4 % scale and ±0.4 % of `s` vertical;
- **sway**, period 17.0 s: rotates the whole figure ±0.35° about the feet and slides it ±0.4 % of `s`.
  Deliberately not a multiple of the breath, so the two never re-align into a visible loop.

Pose is chosen from the **real Sun altitude** (`poseFor`, `ObserverRenderer.kt:120`):

| Sun altitude | Pose | What the arms do |
|---|---|---|
| > +3° | `SHADING_EYES` | front hand up to the brow, back arm resting |
| +3° … −7° | `PHONE` | both arms bent in, a lit phone rounded rect drawn between them |
| < −7° | `LOOKING_UP` | front arm raised, hand at `−1.03 s` — pointing at the sky |

Facing: `facingLeft = figureCenterX > width/2`, i.e. the figure always faces the middle of the frame.
Head tilt follows the pose (−7° looking up, +6° phone, −2° shading), plus a 1.2° breath term.

### 3.3 Its constraints — the contract for the avatar

Anything that replaces this figure has to honour these, or the scene breaks:

1. **Anchor and size** — feet at `(figureCenterX, figureFeetY)`, height `figureHeightPx`, mirrored
   when `figureOnRight = false`. No other layer knows the figure exists; nothing else has to change.
2. **Cost** — it must run inside the 42 ms motion tick without touching the sky node.
3. **Lighting response** — brightness/tint from `LightingState`, not from a fixed material. At night
   the figure must not glow brighter than the sky behind it.
4. **Pose semantics** — the mapping "sun above 3° → shade eyes; twilight → phone; astronomical night →
   look up" is a product decision, not decoration. A 3D avatar should keep the same three beats.
5. **Ground contact** — a contact shadow on the grass line, so the figure is standing, not floating.
6. **Theme** — six themes (`SkyCanvasTheme`). Papercraft wants flat cut-out colour; Monochrome wants
   ink; Observatory wants red-shifted. A photo-textured 3D avatar fights most of them, which is why the
   avatar needs a theme treatment pass (grade/tint), not just a raw texture.

### 3.4 How a real 3D layer would slot in

`drawSceneMotion` is the seam:

```
SkySceneRenderer.drawSceneMotion(scope, frame, theme, frameTimeMs, figureOnRight)
    → clipRect(0, 0, w, horizonY) { CloudVeilRenderer.draw(...) }
    → ObserverRenderer.draw(scope, composition, theme, frame.lightingState, frameTimeMs)
```

Two viable ways to put a GLB behind that seam, in order of how little they disturb the current
architecture:

- **A. Render offscreen, blit into the canvas.** Draw the skinned mesh into an EGL/GLES3 FBO
  (`SkyPanoramaRenderer.kt` already contains this exact EGL + GLES30 setup, including a headless
  surface), read it into an `ImageBitmap` and `drawImage` it at the composition's feet rect, inside
  the existing `Canvas`. Keeps one compositing layer, keeps the header and Moon card on top, keeps the
  tap/drag gestures untouched. The avatar is only ~170 px tall, so the target texture can be small
  (e.g. 256×384) and the GPU cost is trivial.
- **B. A transparent `GLSurfaceView` in an `AndroidView`** between layer 1 and the readout. Cheaper in
  code, but it introduces view z-order, punch-through and gesture conflicts inside a `LazyColumn`
  item, and the header scrim would have to be re-checked on every device.

A is the recommendation. The runtime contract for the asset (skeleton bone names, clip names, scale in
metres, +Z forward, origin between the feet, material slots, triangle and texture budget) is written in
`skills/zig-avatar-runtime/references/asset-contract.md`.

---

## 4. Where the avatar programme plugs in

| Stage | Owner | Output |
|---|---|---|
| reference photos | user | 3–6 shots: front, back, 3/4s, neutral light, plain background |
| matte + measure | `tools/avatar/photo_ingest.py` | silhouette, proportions, palette, head crop |
| 3D build | AssetFurnace (GPU) or `tools/avatar/` fallback (CPU) | textured humanoid mesh, A-pose |
| rig | SkinTokens / authored skeleton | bones + skin weights, A-pose bind |
| animation | motion clips (idle, wave, look-up, phone) | clips retargeted to the rig |
| export | GLB writer | `*.glb` + `.provenance.json` |
| app | ZIG renderer behind `drawSceneMotion` | the observer slot, lit by `LightingState` |

The placeholder is replaced, not deleted: it stays as the zero-cost fallback when no avatar is
installed, on devices without GLES3, and in themes where a photographic avatar would break the palette.

---

## 5. Reproducing the numbers

```bash
./gradlew :app:testDebugUnitTest --tests '*SkyProjectionTest*'
```

`theFullBleedSceneMovesTheHorizonAndKeepsTheSkyMapping` asserts the composition agrees with the
projection: same horizon, ground below horizon, feet on the ground, figure centre on the right, and the
mirrored composition on the left.

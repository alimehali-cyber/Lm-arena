# Home Sky Scene — full-bleed hero

**Scope:** Home only. The sky canvas stopped being a card and became the top of the screen: one
edge-to-edge scene that runs behind the status bar and carries the header, the location pill and the sky
viewer affordance — and nothing else. No clock, no date, no condition is written on the sky any more.
Directly beneath it sits one wide glass card: the Moon in the square it has always occupied, the next
eclipse at its bottom, and the readout (clock, condition, date, Moon) beside them. Everything else on
Home starts below it.

No weather data was added. Every number and every layer is derived from what ZIG already computes:
`SkySceneFrame` (Sun, Moon, planets, stars, Milky Way), `LightingState` (Sun altitude, star visibility,
moon glow) and the scene clock.

![Home sky scene, night and daylight](home-sky-scene.png)

*The same composition at astronomical night and at noon, rendered from the constants below: the same
edge-to-edge scene with the sky completely bare, and one wide squircle card underneath it — Moon over the
next eclipse on one side, the readout on the other. The clouds and the mist follow the theme; the Sun
altitude drives the light.*

---

## 1. Composition

```
┌──────────────────────────────────────────┐ 0
│  status bar (scene continues behind)     │
│  ZIG             ⌕  Settings             │  header (HomeScreen)
│                                          │
│                                          │  the sky, and nothing
│                                          │  written on it
├──────────────────────────────────────────┤ ← horizon  (0.70 × scene)
│  distant ridge                           │
│  grass line + tufts            ⤢         │ ← expand affordance
│  ◉ Tehran                                │ ← location pill, over the
└──────────────────────────────────────────┘   Moon card's side
  ╭────────────────────────────────────────────╮  one wide card on the feed's
  │  22:41      │                              │  gutters. The Moon keeps
  │  ─────      │             ◗                │  its old square, so the
  │  sky state  │   the Moon, the same disc,   │  render is unchanged;
  │  2 Aban     │   the same place             │
  │  Moon 64%   │                              │
  │  (readout)  │  ──────────────────────────  │
  │             │             ◐                │
  │             │   the next eclipse           │  the eclipse moves to
  ╰────────────────────────────────────────────╯  the card's bottom
   What's Up Tonight …                      feed
```

Constants live in `SkySceneLayout` (`ui/rendering/SkySceneComposition.kt`):

| Constant | Value | Meaning |
|---|---|---|
| `HORIZON_FRACTION` | 0.70 | where altitude 0° lands inside the scene (≈ 39 % of the screen) |
| `GROUND_TOP_FRACTION` | 0.052 | solid ground starts this far below the horizon |
| `FAR_RIDGE_FRACTION` / `MID_RIDGE_FRACTION` | 0.034 / 0.020 | ridge amplitudes |

`HomeScreen` computes the height (`52 %` of the viewport, clamped to 312–560 dp) outside the
`LazyColumn`, because lazy items are measured with an unbounded height. The same `BoxWithConstraints`
computes the card's geometry (`HeroCardMetrics` in `HomeScreen`):

| Part | Value | Meaning |
|---|---|---|
| `moonSquare` | half the feed width − the two gutters − the gap, clamped 132–210 dp | the square the Moon card has always been |
| `width` | `moonSquare × 2 + gap` | exactly the width the two square tiles used to share |
| `eclipseStrip` | `moonSquare × 0.5`, clamped 64–100 dp | the band under the Moon that holds the eclipse |
| `height` | `moonSquare + gap + eclipseStrip` | the Moon square, a hairline gap, the eclipse strip |

The card is therefore **the Moon's square, plus air, plus the eclipse** — it grew sideways into the space
the eclipse used to occupy beside the Moon, and downward only by the eclipse it took in. The Moon's square
starts exactly where the sky ends and sits on the same gutters as before, so on every screen the Moon is
drawn in exactly the same place at exactly the same size as it was in the square card: same x, same y,
same diameter, same 8 dp inset inside its square, same `MoonPhaseRender` call. That is the constraint this
layout exists to hold.

### One card, three parts

| Part | Shows | Gesture |
|---|---|---|
| Moon | `MoonPhaseRender` in its old square and nothing else — no phase name, no percentage, no date | drag scrubs ±30 days (shared with the Moon window); tap opens the Moon window |
| Eclipse | `EclipseRender`, drawn for the type of the *next* eclipse at this location, in a square centred in the strip at the card's bottom | tap opens the eclipse window for that eclipse |
| Readout | clock, condition, date and the Moon's state, on the card's other side | none — it is a caption |

The whole card is one `squircleShape(RedCornerRadius.window)` `LiquidGlassSurface`. The Moon and the
eclipse are one column on the card's start side — the right in Persian, the left in English, the same side
the Moon card has always been on — and a single hairline separates them instead of a second card. The
readout fills the other column, so the eye reads *where* on the start side and *when* on the end side.

`EclipseRender` gets a square, not the full strip: it crops its plate with `ContentScale.Crop` and masks
it with a radial gradient sized from `minDimension`, so a wide box would slice the corona.

The eclipse tile stands for the soonest eclipse of **either** family, not for the family the "Upcoming
Eclipses" list below happens to be showing. A hero tile that changed when a chip further down the feed
was tapped would be a surprise; the next eclipse is a fact about the sky, not about the list.

`EclipseRender` draws the two families differently, because they are two different physical pictures:

- **solar** — drawn, because the subject is light, not a surface: a limb-brightened corona with ~44
  deterministic streamers of varying length and width, the chromosphere ring, and a diamond ring at one
  point of the limb for totality; a ring of fire with Baily's beads for annularity; two overlapping discs
  for a partial.
- **lunar** — the app's real Moon photograph, shaded by a radial gradient applied with `BlendMode.Multiply`
  inside an offscreen layer. Multiplying keeps the maria visible (it darkens the photograph instead of
  painting over it), and where the gradient is centred is what makes totality read as a copper disc with
  a bright limb while a partial eclipse keeps one limb in full sunlight.

### The location pill

The selector is no longer in the header. It sits in a 38 dp row directly above the card, on the card's
Moon side — the right in Persian, the left in English — while the sky viewer affordance keeps the opposite
corner. It shows **only the name of the selected city**, or `Live GPS` / `GPS زنده` (with the
locate icon) when the app-wide location came from the device's own fix. The coordinates it used to carry
were two facts the Location window already states, and on a pill over a moving sky they were the crowded
part.

That distinction is a state, not a guess from the city name: `MainUiState.isLiveGpsLocation` is set by
`MainViewModel.setGpsLocation` (through `setLocation(..., fromLiveGps = true)`, including the reverse-
geocoded refinement that follows the offline nearest-city guess), cleared by any city or manual-coordinate
pick, and persisted in `astro_app_prefs` so the pill still says *Live GPS* after a restart.

## 2. Layers

Drawn in `HeroSkyCanvas`, which now has three independent Compose draw nodes plus, on the
photographic themes, the Persepolis GL layer between the first two:

1. **Sky** (`SkySceneRenderer.drawSky`) — atmosphere, Milky Way, stars, Sun, Moon, planets, ground.
   Repaints on the 1 Hz shared clock, or per frame while the finger scrubs time.
1b. **Persepolis + Mil-e Azhdaha** (`PersepolisLandscape`, photographic scene only) — the Apadana
   relief at half its first linear size in the left distance and the Mil-e Azhdaha tower on its
   limestone slope at the far right, both lit by the same ephemeris and rendered on demand. See
   [`PERSEPOLIS_LANDSCAPE.md`](PERSEPOLIS_LANDSCAPE.md).
2. **Motion** (`SkySceneRenderer.drawSceneMotion`, scene only) — the drifting cloud veils.
   ~24 fps while idle. Separate node, so the astronomy is never repainted for a drifting cloud.
3. **Interaction** — selection ring and stardust. Only alive while the user is touching the canvas.

### Ground — `SceneGroundRenderer`

Two art paths. The **stylised themes** keep the original band — far ridge → mid ridge →
solid ground gradient → grass tufts → foot vignette — with summed-sine ridges
sampled through quadratics and deterministic grass (Papercraft keeps hard cardstock edges and skips
the strokes). (The old full-width horizon "mist shelf" was removed from both paths: it read as a
white band across the canvas at the readout's day/date line.)

When the combined relief is active (the photographic themes), the band becomes the full **Marvdasht
landscape** and is painted underneath the GL stone: the far Mamasani fold range and the
Dimeh-Mil / Mil-e Azhdaha massif of Nurabad (Mamasani, Fars) — the tower's real backdrop,
cut by the Tang-e Farakh gorge — built from domain-warped ridged-multifractal profiles
(`LandscapeProfile`) with Sun-facing facet
shading, bedrock benching, a water-gap, strata dipping off the fold hinge, crest caprock, talus
cones, cliff streaks, erosion gullies and aerial perspective; piedmont foothills and alluvial
fans; a semi-arid steppe/cultivation plain with an irrigation canal; two separate footprint-local
earthworks from `PersepolisFraming` (worn apron, talus, contact shadows, debris — ordinary field
between the sites, feathered and Sun-following, so neither monument ends on a full-width stripe);
a broad layered Pulvar river (wet banks, shallows, thalweg, point bars, tree reflections, reed
clumps) whose sheen and thinned glitter track the real Sun/Moon; a parkland of exactly **two
species** — the supplied **Paulownia** and **sugar maple** designs, each rendered once per season
and projected at every planting, so all ~90 trees in the valley are copies of two renders and not
one vertex of per-tree geometry (the oak, pistachio and *Artemisia* scrub the parkland used to
carry are gone, every one of their slots now a maple) — with both species' wardrobes read from the
**Persian calendar**, where spring begins on 1 Farvardin; low
whaleback Nurabad ridges at true relief; and a uniform grass field — one textured
cool-green mass in eight bucketed fills, no individual blades, lush along the near bank.
All placement is deterministic. See
[`PERSEPOLIS_LANDSCAPE.md`](PERSEPOLIS_LANDSCAPE.md).

### Clouds — `CloudVeilRenderer`

Three veils (high cirrus, mid, low) at different speeds and parallax depths. Pre-rendered once into
two soft alpha sprites, drawn as tinted images, so a frame costs a handful of textured quads instead
of dozens of radial gradients. Tint and strength come from `ScenePalette`: Papercraft opts out (it has
its own cut-outs), Real Sky keeps a faint veil so the photograph still moves, and the strength fades at
night so a star map is never veiled into a cloudy sky.

**These are atmosphere, not a forecast.** ZIG has no weather feed; the layer must never be described as
cloud cover.

### Readout — on the card, not on the sky

The typographic block used to sit above the horizon, drawn by the canvas. It now sits on the free half of
the card (`MoonCardReadout` in `HomeScreen`), stacked and centred vertically, at caption scale, because the
sky is the product and the instant it is drawn for is the caption:

- the clock at 26 sp medium (it used to be 46 sp light, which competed with the sky);
- a 20 × 1 dp hairline separating *when* from *what* — horizontal now that the block is a column;
- the sky's own state at 11.5 sp, with a small accent dot that appears while the finger is scrubbing time
  on the canvas, so a simulated instant is marked without adding a caption nobody reads mid-gesture;
- the date and the Moon at caption scale, one per line — the readout's column is one square wide, so two
  separate lines keep "۲ آبان · ماه ۶۴٪ بالای افق" from being ellipsized away.

It reads the same instant the sky is drawn for: `SkyTimeModel.effectiveJd(instant, skyDragOffsetHours)`.
The drag offset is collected inside `MoonCardReadout`, not in `HomeScreen`, so a per-frame scrub recomposes
this caption and nothing else. The ink comes from `RedTheme.colors`, which `LiquidGlassSurface` resolves
against the glass it hands down, so the numbers stay legible on every theme. And every value comes from the
same state the card is drawn from, so the numbers and the sky can never disagree.

Nothing is drawn on the canvas any more: `HeroSkyCanvas` has no `SCENE`-presentation text, and its doc
comment says so. The fullscreen viewer keeps its own time and date pills, which are `VIEWER` chrome.

`PersepolisFraming`'s readout clearance is still computed and still passed by the canvas. It is now a
nominal guard rather than a protected caption: it keeps the relief and the tower where they have always
been. See [`PERSEPOLIS_LANDSCAPE.md`](PERSEPOLIS_LANDSCAPE.md).

## 3. Projection

`SkyRenderTarget.HOME_SCENE` uses `SceneProjection(SkySceneLayout.HORIZON_FRACTION)` — the same
cylindrical, equator-facing Home mapping, with the horizon as an argument instead of the 0.85 constant.
`HeroProjection` is now `SkyProjection by SceneProjection()`, so the card, the fullscreen viewer and the
Live Sky backdrop are unchanged. `SkyProjection.horizonY(w, h)` was added so every layer reads the
horizon from the surface that owns it.

## 4. Measuring it

```bash
./gradlew :app:testDebugUnitTest --tests '*SkyProjectionTest*'
```

`theFullBleedSceneMovesTheHorizonAndKeepsTheSkyMapping` asserts the horizon moved, the mapping did not,
and that the ground band sits below the horizon.

## 5. Tuning

- Taller / shorter scene: `HomeSkyHero` (0.52) and the 312–560 dp clamp in `HomeScreen`.
- Sky-to-ground balance: `SkySceneLayout.HORIZON_FRACTION`.
- Size of the card: `HeroCardMetrics.forWidth` in `HomeScreen`. The Moon's square is the invariant — it
  is the Moon card's old size, and the Moon render is inscribed in it — while `eclipseStrip` sets how much
  of the card the eclipse gets. The card never overlaps the canvas, so the whole ground band stays visible.
- The pill and the viewer affordance ride in one row just above the card, inset by the feed's gutters:
  the location on the start side (the Moon's side), the viewer on the end side.
- Cloud presence per theme: `ScenePalette.cloudStrength`.
- Motion cost: `SCENE_MOTION_FRAME_MS` in `HeroSkyCanvas` (42 ms ≈ 24 fps).
- Scene tail: the scene does not end at the canvas's bottom edge. The canvas's own bottom
  colour continues *below* the canvas and dissolves in alpha to full transparency over
  `bottomTailHeight` (Home passes the card's height plus the gap, the whole block below the sky), so
  the tail passes underneath the wide card. Fading in alpha rather than to a
  colour is what makes it backdrop-agnostic: it merges into the theme background, the
  live-sky backdrop render (`LiveSkyBackdrop`), a photo backdrop, or any other app
  backdrop unchanged. The canvas itself stays crisp to its edge — nothing fades upward
  into it. (See "SCENE TAIL" in `HeroSkyCanvas`; the scene root is deliberately unclipped
  so the tail can paint there.) Static landscape geometry is cached per layout — see
  `VegetationGeometry` in `SceneGroundRenderer`.
- **One foot colour, shared by the canvas and the tail.** `SceneGroundRenderer.bottomEdgeColor`
  computes the colour the canvas's bottom row is painted at this frame's theme and lighting, and
  `drawBottomFade` paints that colour's alpha ramp from nothing to fully opaque on the last pixel
  row. The tail starts from the same value, so the seam is a continuation rather than a blend, and
  the whole ramp is one colour losing alpha. On the photographic themes the value is the grass
  field's own tone at the foot of the frame (settled toward the theme's ground and deepened for the
  vignette), not the palette's near-black `groundBottom` — a daylight valley now ends in its field
  green, golden hour in warm earth, and astronomical night in near-black. The river overlay's
  bottom edge in `HeroSkyCanvas` uses the same value, so the GL water corridor can never tint the
  last rows away from it. `SceneFootColorTest` holds the contract (opaque in every theme and light,
  green at noon, monotone along the Sun's arc, dark at night).
- Horizon bands are banned: any full-width pale rect with a straight edge at the horizon
  (the old mist shelf, then the "horizon catch-light" wash in both ground paths) reads as
  a white band across the scene at the readout's day/date line, flaring while scrubbing
  sweeps through daytime. The same applies to *haze washes*: the far range used to be
  semi-transparent and lerped 55% toward a near-white `palette.haze` at its foot (plus a
  "haze pooling" rect at the massif foot), so at midday the range and the sky bleeding
  through it formed one glowing white slab on the horizon. The ranges are now opaque and
  only lightly hazed, the palette haze's daylight mix is capped, and no pooling rect
  remains. Only the ridges' own silhouettes may cross the horizon.

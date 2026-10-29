# Landscape components — the river, the green floor, the mountains

Three complete, self-contained source files, one per component of the landscape under the
Home sky canvas. They are **extracted verbatim** from the app's single landscape file,
`app/src/main/java/com/alijafari/red/astronomy/ui/rendering/SceneGroundRenderer.kt`
(2,461 lines) — only the function seams are new. Nothing in `app/` was modified.

| File | Component | Lines | What it draws |
|---|---|---:|---|
| [`Mountains.kt`](Mountains.kt) | `LandscapeMountains` | 604 | Far Mamasani fold range, the Dimeh‑Mil / Mil‑e Azhdaha limestone massif (benched profile, gullies, talus cones, varnish streaks, the Tang‑e Farakh gorge), oak speckle, piedmont foothills, alluvial fans — plus the two stylised sine ridges of the flat themes. |
| [`GrassField.kt`](GrassField.kt) | `GrassField` | 401 | The green floor: the plain's vertical gradient, the cultivation mosaic, the irrigation canal and its tree line, the uniform grass field (eight bucketed tone fills + the lush near‑bank ribbon, with the river/apron/canal masks), and the stylised grass line. |
| [`River.kt`](River.kt) | `PulvarRiver` | 456 | The Pulvar in the near foreground: centreline/half‑width geometry, layered water, Fresnel‑graded sky reflection, waterline, point bars, ripples, pebbles, bank lines, riparian grass, Cox–Munk sun/moon glitter, broken tree reflections, reed clumps. |

Provenance: repo commit `716586ee67074e542485cbe5e7f6af9bcaabbdb0`,
`SceneGroundRenderer.kt` SHA‑256 `f8cdaac12bb0…`. The exact source line ranges are listed in
each file's header. Line counts above are the extracted files; the comments and tuning notes
travel with the code.

## Where the landscape lives in the app

```
HomeScreen                     ui/screens/HomeScreen.kt        full-bleed hero, 52 % of viewport
 └─ HeroSkyCanvas              ui/components/HeroSkyCanvas.kt  three Compose draw nodes + GL relief
     ├─ SkySceneRenderer.drawSky        ui/rendering/SkySceneRenderer.kt     ← the expensive pass
     │   └─ SceneGroundRenderer.draw    ui/rendering/SceneGroundRenderer.kt  ← ALL the landscape
     │         └─ drawMarvdashtLandscape (photographic themes) / sine ridges (flat themes)
     ├─ PersepolisLandscape            ui/persepolis/…              (GL stone, not in these files)
     └─ SkySceneRenderer.drawSceneMotion                           (clouds, observer)
```

`SceneGroundRenderer.draw` picks one of two art paths: the photographic themes with the
Persepolis + Mil‑e Azhdaha relief get `drawMarvdashtLandscape` — the 1,490‑line function these
three files are carved out of — and the stylised themes keep the two‑ridge ground band.
Geometry comes from `SkySceneLayout` (`ui/rendering/SkySceneComposition.kt`: horizon at 0.70 of
the scene, ground top at +0.052 h) and `PersepolisFraming` (the toe, platform and slope rows the
river and the field masks are anchored to).

## Call order

The plain is one object painted in three passes because the mountains and the river sit between
them. Reproduce the shipped composition exactly with:

```kotlin
GrassField.drawPlainFloor(drawScope, composition, palette, lighting)   // 1. green gradient floor
LandscapeMountains.draw(drawScope, composition, palette, lighting, framing, sunPosPx) // 2. ranges
GrassField.drawCultivation(drawScope, composition, palette, lighting, framing) // 3. plots + canal
val river = PulvarRiver.geometryFor(composition, framing)
PulvarRiver.draw(drawScope, composition, palette, lighting, sunPosPx, moonPosPx, river) // 4.
GrassField.drawField(drawScope, composition, lighting, framing, river) // 5. field + lush bank
// …then the vegetation layer (trees, scrub) and SceneGroundRenderer's foot vignette.
```

Not included here (they are not part of these three components): the monuments' earthworks
(`drawCombinedLandscapeGrounding`), the space‑colonization trees and scrub
(`buildVegetation`/vegetation replay, ~1,000 lines), the stylised mist/ground band, and the
bottom vignette. All of them stay in `SceneGroundRenderer.kt`, and the full bundle of the
landscape — every file, including those — is in `docs/LANDSCAPE_ALL_CODE.md`.

## Dependencies

Each file needs only the Android Compose UI graphics API plus these existing, unchanged types
from the app:

- `SceneComposition`, `SkySceneLayout` — `ui/rendering/SkySceneComposition.kt`
- `ScenePalette`, `scenePalette()` — `ui/rendering/ScenePalette.kt` (`internal`)
- `LightingState` — `ui/rendering/LightingEngine.kt`
- `PersepolisFraming.Result` — `ui/rendering/PersepolisFraming.kt`
- `LandscapeProfile` — `ui/rendering/LandscapeProfile.kt` (skyline noise, `hash01`, `fbm`,
  `mountainHeights`, `foothillHeights`, `heightAt`, `sunGlitter`)

`River.kt` owns the river geometry; `GrassField.kt` takes a `PulvarRiver.Geometry` and uses it
as a mask, so the field can never disagree with the water's edges. Each file carries its own
file‑private copies of the two tiny helpers the original object shared (`Color.scaled`,
`drawSoftPatch`), so the three can be dropped into any package without a fourth file.

## Notes on behaviour (kept from the original)

- Every placement is deterministic (`LandscapeProfile.hash01`, fBm, and a fixed LCG for the
  grass line), so the landscape never shimmers between clock ticks.
- All colour is derived per frame from the theme palette and the live Sun/Moon: the valley warms
  at golden hour and dissolves into a moonlit silhouette at night.
- The field geometry bake is cached against a layout key (canvas size, toe, platform/slope
  bounds, river row) — the original cache for exactly the same reason: the rows are ~3,000
  overlapping cells and must not be recomputed per frame.
- Deliberate omissions are preserved and commented: no horizon mist shelf, no plain catch‑light,
  no full‑width haze wash — each of those used to read as a white band across the scene at the
  readout's date line.

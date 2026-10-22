---
name: zig-home-scene
description: Work on ZIG's full-bleed Home sky scene — its composition, projection, three draw layers, clocks, palettes and the vector observer that stands in the avatar slot. Use before changing anything in ui/rendering or the Home hero, adding a layer, or replacing the observer.
---

# ZIG Home sky scene

The reference is `docs/HOME_SCENE_ANALYSIS.md`. Read it before touching the scene; this page is the
short form and the rules.

## The shape of it

- The scene is the top band of Home: `HomeSkyHero` in `ui/screens/HomeScreen.kt`, height
  `sceneHeight + 132.dp − 96.dp`, with the Moon card riding 96 dp up over the ground.
- `HeroSkyCanvas(presentation = SCENE)` draws three stacked canvases:
  1. **sky** — `SkySceneRenderer.drawSky`, on the 1 Hz clock, model quantised to 15 s;
  2. **motion** — `SkySceneRenderer.drawSceneMotion`, clouds + observer, ~24 fps (`42 ms`);
  3. **interaction** — ring and stardust, only while touched.
- Everything agrees on `SkySceneLayout.composition(w, h)`: horizon at 0.70 h, ground at
  horizon + 0.052 h, figure 0.19 h tall with feet at 0.76 h, centred at 0.745 w.
- `SceneProjection(0.70)` maps azimuth/altitude to pixels and clips at the horizon.

## Rules

1. **Never repaint the astronomy for motion.** If a new layer animates, it belongs in the motion pass
   (or its own node), not in `drawSky`.
2. **Never hard-code a horizon.** Ask `composition.horizonY` / `projection.clipBottomPx`.
3. **Never claim weather.** Clouds are atmosphere. The Sun altitude gives the phase of day; that is
   astronomy, not a forecast.
4. **Palette, not literals.** Colours come from `scenePalette(theme, lighting)` /
   `figurePalette(theme, lighting)`; both interpolate on `lighting.ambientBrightness`. Six themes:
   Real Sky, Atmospheric Sky, Monochrome Scientific, Kids Watercolour, Observatory, Papercraft.
5. **Keep it cheap.** The observer is ~12 draw calls; the ground is three paths and two gradients; the
   clouds are pre-rendered sprites. Match that discipline.
6. **Respect the clocks.** Wall-clock milliseconds are `Float`-unsafe; wrap to a day and use `Double`
   seconds (see `ObserverRenderer`), or keep them as `Long`.
7. **RTL.** The composition mirrors the figure; anything new that has a side must mirror too.
8. **Test the arithmetic, not the paint.** Geometry belongs in pure objects
   (`SkySceneComposition`, `SkySceneModel`) so `SkyProjectionTest` can assert it on the JVM.

## The observer slot (where the avatar goes)

`ObserverRenderer.draw(scope, composition, theme, lighting, frameTimeMs)` is called from
`SkySceneRenderer.drawSceneMotion`. Its contract, which any replacement must keep:

- feet at `(figureCenterX, figureFeetY)`, height `figureHeightPx`, mirrored when on the left;
- a contact shadow on the grass line;
- pose from the real Sun altitude: `> +3°` shade the eyes, `+3 … −7°` look at a phone, `< −7°` look up;
- brightness from `LightingState`, never a fixed material;
- no input, no window of its own — it lives inside the canvas.

See `../zig-avatar-runtime/SKILL.md` for the 3D replacement and its offscreen-blit approach.

## Verify

```bash
./gradlew :app:testDebugUnitTest --tests '*SkyProjectionTest*' --tests '*SkySceneModelTest*'
```

CI builds the debug APK and gates on `com.zig.gravity.*`; the scene is covered by the JVM tests above,
so run them yourself before pushing.

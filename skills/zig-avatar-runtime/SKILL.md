---
name: zig-avatar-runtime
description: Run a rigged, animated GLB avatar inside ZIG's Home scene — the asset contract, the offscreen-render-then-blit integration, theme grading, lighting, LOD and the fallback rules. Use when wiring a generated avatar into the app or changing how the Home character looks.
---

# ZIG avatar runtime

Goal: the vector observer in `ObserverRenderer` gets replaced by the user's own rigged character,
without changing how the scene is composited, how fast it runs, or how it looks in six themes.

The analysis of the slot is in `docs/HOME_SCENE_ANALYSIS.md` §3; the asset contract that a generated
GLB must satisfy is `references/asset-contract.md`.

## Where it goes

`SkySceneRenderer.drawSceneMotion` is the only seam:

```
clipRect(0, 0, w, horizonY) { CloudVeilRenderer.draw(...) }   // clouds stay behind
ObserverRenderer.draw(scope, composition, theme, lighting, frameTimeMs)   // ← replaced
```

**Recommended integration — render offscreen, blit into the canvas.** `SkyPanoramaRenderer` already
contains the EGL + GLES30 + headless-surface machinery this needs; reuse its pattern rather than
inventing a second GL stack.

1. An `AvatarStage` owns an EGL surface with an FBO at the figure's pixel size (e.g. 256 × 384 px,
   scaled with density), one draw per motion frame.
2. It draws the skinned mesh with a camera framed to `figureHeightPx`, feet on the bottom edge,
   neutral 3/4 view facing frame centre, full-body.
3. It reads the colour buffer into an `ImageBitmap` and the scene's motion canvas draws it at
   `(figureCenterX − w/2, figureFeetY − h)`.
4. The vector observer is the fallback: no asset installed, GLES < 3, FBO creation failed, or
   the theme is one where the avatar opts out.

Why not a `GLSurfaceView` in an `AndroidView`: view z-order and punch-through inside a `LazyColumn`
item, gesture conflicts with the canvas' tap/drag, and the header scrim would have to be re-checked on
every device. The offscreen blit keeps one compositing layer and the existing interaction model.

## Lighting and theme

The character must sit in the scene, not on it:

- **Day**: full albedo, key light from the Sun's screen direction (project `sunHoriz`, use the sign of
  x to pick left/right), ambient from `lighting.ambientBrightness`.
- **Twilight**: warm rim from the horizon tone (`lighting.horizonTone`), 20 % desaturation.
- **Night**: multiply albedo by ≈0.35, add a cool rim (`0.2 × skyTone`), keep the silhouette readable.
- **Theme grading** (`SkyCanvasTheme`): Real Sky = photographic; Atmospheric Sky = +10 % saturation;
  Monochrome = luminance → ink ramp; Kids Watercolour = posterise to 4 levels and warm; Observatory =
  deep red grade; Papercraft = flat-colour quantise with a hard cut edge. The observer already does a
  version of this through `figurePalette` — the avatar should read as its 3D equivalent, not as a
  photo pasted into a drawing.
- **Contact shadow** stays: draw the same two ovals under the feet, sized to the avatar's stance.

## Performance budget

- Texture ≤ 1024², triangles ≤ 12k, one material, ≤ 32 bones, 4 weights/vertex.
- One GL draw per frame at ~24 fps; no per-frame allocations (reuse buffers, matrices, the FBO).
- Decode the GLB off the main thread; build the GL resources with the stage, not per frame.
- If the FBO cannot be created, fall back and **log once**, never per frame.

## Pose beats (keep the product semantics)

`ObserverRenderer.poseFor(sunAltitudeDeg)`: `> +3°` shade the eyes · `+3 … −7°` phone · `< −7°` look
up. The avatar plays `day_shade`, `dusk_phone`, `night_point`, with `idle_breathe` layered underneath,
cross-faded over ~0.8 s when the Sun crosses a threshold. The avatar must not suddenly change clip
while the user scrubs time — debounce on the quantised model instant.

## Rules

1. The placeholder stays in the codebase: it is the fallback, and the tests for the composition still
   assert its geometry.
2. The avatar is scenery, never a character with dialogue or a claim about the weather.
3. No network: the GLB ships in assets or is imported by the user; nothing is fetched at runtime.
4. Provenance travels: bundle the sidecar, and show the asset's licences in Settings → About.
5. Everything that can be a pure function (framing, bone lookup, clip selection, theme grade) gets a
   JVM-testable object; only the GL calls stay in the stage class.

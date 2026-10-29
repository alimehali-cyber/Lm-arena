# The Pulvar, in three dimensions

**Scope:** the river in the foreground of the Home sky canvas. It is now the fixed-camera 3D
corridor from `PulvarRiver3D.kt` — the supplied component, hosted the way this app hosts every GL
layer — instead of the painted ribbon. The mountains, the plain, the cultivation, the monuments and
the trees are untouched.

```
Home hero (52 % of the viewport, 312–560 dp)
┌──────────────────────────────────────────┐ 0
│  sky, stars, Moon, planets …             │
│  readout                                 │
├──────────────────────────────────────────┤ horizon (0.70 of the scene)
│  far range · Dimeh-Mil massif            │   painted (SceneGroundRenderer)
│  plain · cultivation · canal             │
│  monuments' earthworks · trees           │
├──────────────────────────────────────────┤ 0.825 h  ← GL band starts
│  ╭────────────────────────────────────╮  │   PulvarRiver3D (TextureView, 24 fps)
│  │ far-bank grass · water · near bank │  │   0.175 h tall, full width
│  ╰────────────────────────────────────╯  │   + the scene's foot vignette re-applied
└──────────────────────────────────────────┘ 1.0 h
        (the drawn tail continues below, then the Moon/eclipse cards)
```

## 1. What changed, and what did not

| | |
|---|---|
| **New** | `ui/rendering/landscape/PulvarRiver3D.kt` — the scene, its procedural geometry, its GLSL and its fixed camera, plus a `TextureView` host, a Compose entry point, and `PulvarRiverFraming`. |
| **New** | `HeroSkyCanvas`: one band at the bottom of the scene (`PULVAR_RIVER_BAND_FRACTION = 0.175`), the corridor inside it, and the scene's foot vignette re-drawn over it. |
| **Unchanged** | `SceneGroundRenderer` — not a single line. The painted Pulvar, its banks, ripples, glitter, tree reflections and reeds are all still there, *under* the GL band. |
| **Unchanged** | The composition: horizon at 0.70, ground top at +0.052, the framing of the monuments, the readout, the cards, the tail, every other theme. |

The painted river is deliberately left intact underneath, which gives two things:

1. **A hole never opens.** The GL band starts at 0.825 of the scene, above the highest row the
   painted water ever reaches — sampled at every scene size Home produces, its far wet-bank fringe
   (`ribbonPath(1.18f)`) peaks at 0.857 of the scene — so the 3D water covers the 2D water
   completely at every canvas size, with ≈13 dp to spare.
2. **A fallback.** If a device cannot create the EGL context or compile the shader, the host logs,
   drops the layer and the scene keeps the 2D Pulvar it has always drawn — the same failure policy
   as the relief, the panorama and the AR sky.

## 2. Hosting: TextureView, not a window-level surface

The supplied file hosted the scene in a `GLSurfaceView` with `setZOrderOnTop(true)`. In this app
that is not usable, for the reasons `PersepolisTextureView` documents at length: a window-level
surface composites above the *whole activity window*, so the readout, the sky-search scrim and the
card row could never cover it, and the canvas's clip, dimming and scroll would not apply. The
frames are therefore rasterised into a `TextureView` by a hand-owned EGL thread, exactly like the
combined relief:

- own `HandlerThread` + EGL 1.4 context (ES 2.0, RGBA8888, depth 16), created on surface-available;
- the scene's `init` / `draw` / `release` stay the only GL calls, and it still never swaps — the
  host swaps;
- continuous while resumed, paced at **42 ms (≈24 fps)**, the cadence the sky scene's own motion
  layer uses; `setAnimated(false)` drops it to one frame per light/framing change;
- paused with the host lifecycle, refreshed with it, and the mesh/context survive a pause;
- non-clickable, non-focusable, no touches consumed.

Two shader-level adaptations, both marked in the file:

- the water fragment multiplies its colour by its alpha (`vec4(w.rgb * w.a, w.a)`) and the water
  blend becomes `(ONE, ONE_MINUS_SRC_ALPHA)`. A `TextureView` is composited assuming premultiplied
  alpha; straight alpha would brighten every soft water edge over the painted landscape. The shading
  is otherwise the preview's.
- nothing else in the GLSL, the geometry builder or the camera solve was touched.

## 3. Framing: where the far bank lands

The camera is not free — it solves its distance so seven corridor reference points fit the
viewport, and `baseFractionFromTop` then pins the near-foreground point `(11, 1.8, 24.8)` to a
fraction of the view height. What a short, wide Home band needs is *the far-bank crest on the top
edge*: then the corridor fills the band edge to edge, with no transparent strip that would let the
painted river show through.

Solved against that exact camera, the corridor's highest bank vertex reaches the top edge at

```
baseFractionFromTop ≈ 0.048 + 0.141 · aspect        (aspect = band width / height, clamped 2…9)
```

checked at aspects 2.86, 3.85, 4.44, 4.76, 5.00, 5.63 and 6.67 to within ±0.01 of the band height
(`PulvarRiverFraming.baseFractionFromTop`, locked by `PulvarRiverFramingTest`). With it, the water
surface sits ≈12–14 % of the band below the crest and the near floodplain runs off the bottom — the
preview's own composition, cropped to the band.

`VISIBLE_WIDTH_M` stays the preview's 104 m: it is the lateral scale that keeps the reach's ends
(and therefore the mesh's transparent ends) outside the frame. Narrower widths pull the camera in
until near-side geometry crosses the near plane, so it is not a free parameter.

## 4. What changed visually, honestly

- The 3D water replaces the painted water in the same part of the frame, with a real bed under it:
  shallows show the bed, boulders throw wakes, the Sun/Moon glitter is a Cox–Munk lobe rather than a
  column of dashes. Reeds and grass tufts are geometry, not strokes.
- The corridor's own near floodplain fills the bottom of the band. It therefore covers the painted
  foreground trees that used to stand in the shallows (the oak at 0.115 w and the flowering cherry
  at 0.875 w), the painted reed clumps, and the lowest course of the painted cultivation. The
  band's top edge is the 3D far bank against the painted plain — grass on grass, a deliberate
  bank line rather than a cut.
- The band is ≈0.175 of a 312–560 dp hero, so the water reads as a band roughly 15–25 dp tall with
  grass above and below. That is the geometry of a 224 m reach seen from the component's fixed
  camera; the preview reads the same at a full-canvas aspect.

If the foreground trees matter more than the 3D near bank, the next step is to replay the baked
vegetation paths (`SceneGroundRenderer`'s `VegetationGeometry`) in a thin Compose layer above the
GL band. That is a refactor of the landscape's vegetation replay and was deliberately not folded
into this change.

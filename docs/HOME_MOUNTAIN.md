# Home mountain

The photographic Home scene (Real Sky and Atmospheric Sky) uses the supplied
`Mountain.kt` massif in place of the painted far range and Dimeh-Mil massif.
The supplied ridge profile, photo-baked material fields and lighting model
are retained, with the mobile optimizations from `Mountain.kt (2).txt` applied
to the mesh builder and shaders. Its standalone window-level GLSurfaceView is
not used: the transparent, on-demand GLES 3 TextureView still scrolls, clips
and layers with Home. The stylised themes and fullscreen sky viewer keep their
own landscapes.

The mountain is behind the Persepolis and Mil-e Azhdaha relief and its foot is
feathered into the existing valley at the monument toe. The field, river,
vegetation and monuments remain in place. Sun and Moon directions and lunar
illumination come from the same sky frame; the trees' Persian-calendar season
selects green (spring/summer), autumn or snow on the shared mountain mesh.
The view tracks the activity lifecycle and releases its EGL resources when removed.

## Mobile rendering budget

- **400 × 160 terrain cells**, down from 560 × 220, with band boundaries derived
  from the grid width. Including edge skirts and 220 boulders, the scene uses
  **75,051 vertices / 132,880 triangles** rather than 135,471 / 252,160 (about
  **47% fewer triangles**). All five chunks remain within unsigned 16-bit indices.
- Growable `FloatArray` / `IntArray` mesh storage replaces boxed `ArrayList`
  attributes and indices. Only populated entries are uploaded to static VBOs;
  geometry is still built once per EGL context on the GL thread, not on the UI
  thread or on season/light changes.
- The supplied fragment shader uses a sin-free hash, fewer noise octaves and
  fewer cellular-noise evaluations. Rock detail is skipped under dense scrub;
  scree detail runs only for scree materials. The ridge controls, photo fields,
  three seasons and Sun/Moon lighting are preserved.
- Home sets **0.7 render scale**: the backing SurfaceTexture and GL viewport use
  70% of each layout dimension (about **49% of the original pixels**), while the
  TextureView still occupies the same scene bounds. Framing and the valley-foot
  mask stay in their original coordinates. `setRenderScale(1f)` restores native
  resolution; supported scale is clamped to 0.4–1.0. Buffer size is reapplied on
  attach/layout/scale changes. Resizing recreates only the EGL window surface
  (as required by SurfaceTexture); the mesh, shader and EGL context are reused.
- Rendering remains coalesced and on demand. Pausing stops draws and retains the
  context; resuming repaints once with current inputs. No animation loop was
  added to the mountain layer.

These are geometry/pixel-work reductions, not measured device frame-rate gains.

## Validation

`MountainGeometryTest` covers primitive-array growth, upload lengths, mesh counts,
index bounds, finite attributes, continuous chunk seams, world dimensions and
repeatable geometry. `MountainRenderSizeTest` covers pixel savings, aspect ratio,
rotation, full-resolution restore and invalid/tiny sizes. `MountainHostingGuardTest`
protects the in-window GLES 3 integration, scaled viewport wiring and on-demand
lifecycle; `HomeMountainTest` retains the Persian-calendar season contract.

Run the package with:

```sh
./gradlew :app:testDebugUnitTest --tests 'com.alijafari.red.astronomy.ui.mountain.*'
```

On a device, check Real Sky and Atmospheric Sky at noon, twilight and night,
scrub dates through green/autumn/snow, rotate, background/resume and open the
search overlay. Verify the foot still blends behind the ruins and the mountain
scrolls/clips with Home. Profile initial mesh-build allocations and GPU frame
time on a low-end phone; shader compilation on a desktop is not a substitute
for Android driver/compositor and performance checks.

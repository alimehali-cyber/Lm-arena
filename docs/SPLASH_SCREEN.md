# Launch splash: glass Z on crimson velvet

The launch splash is the brand photograph (`assets/brand/zig_splash_master.png`), brought to
life. It runs ~2.6 s, then hands off to the app in ~1.2 s. A tap skips ahead once the Z has landed.

## Timeline

| ms (from first frame) | What happens |
| --- | --- |
| 0 | Frame 0 is exactly `#120105`, the window background and Android 12+ system-splash colour, so the OS hand-off can't be seen. |
| 0 – 750 | The velvet lights up out of darkness. The camera slowly pulls back (Ken Burns, 5 s) and drifts. |
| 120 – ∞ | A soft key light wanders across the pile. It's the velvet texture multiplied by the light and screened back on, so the fibres themselves catch the light. |
| 180 – 1480 | The glass Z arrives from depth: focus pull (blur to sharp, API 31+), 3D tilt settles, scale 0.86 → 1, and the contact shadow tightens. |
| 950 – 2050 | Specular sweep: a soft sheen crosses the frosted face, a tighter highlight catches the polished bevel (rim mask), and a sparkle lands on the top-right corner. |
| ongoing | The Z floats and breathes, the crimson bloom pulses, film grain moves at 24 fps, and the hairline progress fills on an accelerating curve. |
| 2250 – 3010 | A second, brighter sweep: the peak right before the exit (peak-end rule). |
| 2600 – 3790 | Exit: the velvet lifts away like a curtain, dimming as it goes. The Z rises at ~58% of the curtain's speed (parallax). The app glides up from 14% below at 93% scale on Material's emphasized curve. The nav bar follows 260 ms later. |

Under **Remove animations** (animator scale 0) the splash shows a settled still for 0.9 s, then
cross-fades out in 320 ms. Nothing moves.

## Why it stays smooth

* **One clock, read only in the draw phase.** `PremiumSplashState.timeMs` is the only state that
  changes per frame. It's read only inside `graphicsLayer {}` and `drawWithCache { onDrawBehind {} }`,
  so frames skip composition and layout entirely. Brushes, shaders and matrices are built once per
  size in `drawWithCache`.
* **Clamped frame delta.** The clock advances by the real frame delta, capped at 40 ms. A stall
  (first-frame jank, app switch) slows one frame instead of making the animation jump.
* **No decode on the UI thread.** The art is decoded on its own thread from `onCreate`, and
  `prepareToDraw()` starts the GPU upload early. The Activity holds the system splash with an
  `OnPreDrawListener` until decoding is done (at most 800 ms).
* **The app underneath is invisible, not absent.** It's composed during the splash, so the
  hand-off never pays for first composition. Its `splashReveal` layer has alpha 0, so the
  RenderThread skips drawing it until the exit.
* **Cheap layers.** About five full-screen fills per frame. The only offscreen buffer is the
  Z-sized glass layer, used for the SrcAtop sheen and the DstIn rim mask.

## Fitting every screen

* The velvet is drawn "cover", never stretched. In landscape the plate is rotated 90°: crushed
  velvet has no direction, and this needs ~1.3× zoom instead of ~3.6× on a 20:9 phone.
* The Z is `min(45% of width, 36% of height, 330 dp)` wide and centred.
* `SplashTimelineTest` checks fit and coverage on 12 screen shapes (phones, landscape, tablets,
  foldables, square, tiny), plus continuity of every channel at 120 Hz.

## Research behind the choices

* Animated brand ("identity") loading animations were rated the shortest-feeling and most
  appealing loading treatment (*Mobile-Application Loading-Animation Design*, Applied Sciences
  13(2):865, 2023).
* Accelerating progress is preferred over linear (Harrison et al., UIST 2007). Animated progress
  can cut perceived duration by ~11% (Harrison, Yeo, Hudson, CHI 2010).
* Material 3 motion: emphasized easing `(0.2, 0, 0, 1)` for elements that travel and settle, and
  emphasized-decelerate `(0.05, 0.7, 0.1, 1)` for entering elements.
* Android: keep the system splash with `OnPreDrawListener`, and remove the platform splash view
  instantly via `setOnExitAnimationListener` when colours match.
* Compose performance guidance: defer state reads to layout/draw with lambda modifiers.

## Regenerating the assets

```
pip install pillow numpy scipy
python3 tools/splash/build_splash_assets.py
```

This rewrites `res/drawable-nodpi/splash_*.webp` and the generated `SplashArtGeometry.kt`.

## Reviewing motion without a device

The **Splash preview render** workflow (`.github/workflows/splash-frames.yml`) renders the splash
with Robolectric's native graphics. It uploads key frames for phone, landscape and tablet, plus a
30 fps `splash-preview.mp4`, as the `splash-preview` artifact.

# R2-A1 — live-scope fixes: static diffs (UNEXECUTED in this sandbox)

Both files import `android.*`/`androidx.*` and CANNOT be compiled by the offline Kotlin
harness (see HARNESS_DISCLOSURE.md §6 — OrientationProvider.kt itself imports
android.hardware.*, CompassARScreen.kt is Compose). These diffs are by-inspection only;
they must be compiled and run by a human with the real Android toolchain.

## A1(a) CompassARScreen.kt — gate the ImageAnalysis binding

Flag choice: REUSE `StarTrackerConfig.ENABLED` (startracker/fusion). Reason: it is the
documented master switch whose safety contract is exactly "flag OFF = zero behavioral
difference anywhere in app"; the camera use-case change was the one Phase-1 behavioral
addition, so it belongs under the same master flag. A sibling flag would be a second
switch to keep consistent, with no benefit.

```diff
                             cameraProvider.unbindAll()
-                            // Phase 1 Task 3: bind Preview + ImageAnalysis (inert observer)
-                            cameraProvider.bindToLifecycle(
-                                lifecycleOwner,
-                                cameraSelector,
-                                preview,
-                                cameraFrameObserver.getUseCase()
-                            )
+                            // Phase 1 Task 3 + R2-A1 gate: Preview is always bound; the
+                            // ImageAnalysis use case (star-tracker frame feed) is bound ONLY
+                            // when the star tracker master flag is enabled. [...]
+                            if (com.alijafari.red.astronomy.startracker.fusion.StarTrackerConfig.ENABLED) {
+                                cameraProvider.bindToLifecycle(
+                                    lifecycleOwner,
+                                    cameraSelector,
+                                    preview,
+                                    cameraFrameObserver.getUseCase()
+                                )
+                            } else {
+                                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
+                            }
```

With the flag OFF (default; `const val ENABLED = false`, so the branch is compile-time
dead-code eliminated) the binding is EXACTLY the pre-project call, verified against the
base commit: `git show 60928ba:.../CompassARScreen.kt` line 832 is
`cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)` — Preview only.

Human verification (requires Android toolchain/device):
1. Build with the flag off (default): open Compass AR with camera on. Confirm the preview
   renders and `CameraFrameObserver.onImageAnalysisFrame` is never invoked (log or
   breakpoint).
2. Flip `StarTrackerConfig.ENABLED = true` in a debug build: confirm frames start
   arriving to the observer while the preview is unchanged.
3. Run `./gradlew :app:testDebugUnitTest` — the full Android suite (Robolectric/Compose
   tests) must pass; those tests have never run in this sandbox.

## A1(b) OrientationProvider.kt — SkyOrientation timestamp defeats StateFlow conflation

Decision: EXCLUDE `timestampNanos` from equals/hashCode; keep it as a plain data field.
Reason: Phase 1 Task 4 added the field AND put it into equality. StateFlow conflates by
equality, so every sensor tick (new timestamp, identical attitude) became a distinct
value: conflation never happened, every collector fired per tick even when the phone was
stationary. Nothing depends on timestamp-in-equality (repo-wide grep: no
`orientation ==`/`.equals(orientation)` comparisons exist; consumers that need per-tick
behavior read the field directly — CompassARScreen's
`LaunchedEffect(skyOrientation.timestampNanos)` keeps working unchanged).

```diff
         return azimuth == other.azimuth &&
                 pitch == other.pitch &&
                 roll == other.roll &&
-                timestampNanos == other.timestampNanos &&
                 rotationMatrix.contentEquals(other.rotationMatrix)
     }
 
     override fun hashCode(): Int {
         var result = azimuth.hashCode()
         result = 31 * result + pitch.hashCode()
         result = 31 * result + roll.hashCode()
-        result = 31 * result + timestampNanos.hashCode()
         result = 31 * result + rotationMatrix.contentHashCode()
         return result
     }
```

SkyOrientationProjectionTest touches SkyOrientation but CANNOT COMPILE in the harness
(needs ARProjectionEngine.kt, which imports androidx.compose.*) — attempted, see
HARNESS_DISCLOSURE.md §6. So this diff is also UNEXECUTED.

Human verification:
1. `./gradlew :app:testDebugUnitTest` (compiles OrientationProvider + runs the Android
   tests incl. SkyOrientationProjectionTest).
2. Conflation check on device: log emissions of the orientation StateFlow with the phone
   stationary — after this fix, distinct-value emissions stop (only genuinely new
   attitudes emit); before it, one emission per sensor tick.
3. Compass AR still updates smoothly while panning (real attitude changes still emit).

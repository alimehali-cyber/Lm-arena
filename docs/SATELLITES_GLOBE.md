# Satellites · The Globe Is the App

The tab is an edge-to-edge Earth3D viewport with an inset-aware HUD, not a feed. `SatellitesStage` owns the clock, observer and repository integration; the non-scrolling `SatelliteHud` consumes the shared `SatellitesUiState`. `SatPanel` is a single destination. Time and Layers never install a full-screen scrim. Sheets have their own scroll areas. Location, camera orientation, scope and simulation offset are not reset on Detail Back.

## Preview matrix

Open [`SatellitePreviews.kt`](../app/src/debug/java/com/alijafari/red/astronomy/ui/satellites/SatellitePreviews.kt) with the **debug** build variant in Android Studio and choose **Split** or **Design**. It provides the 24 reference previews, plus Spatial and Aether examples and two narrow Persian 130% cases:

| Palette | Globe at rest | Time tray | Passes (half) | Detail |
|---|---|---|---|---|
| Dark | `DarkGlobe` | `DarkTime` | `DarkPasses` | `DarkDetail` |
| Light | `LightGlobe` | `LightTime` | `LightPasses` | `LightDetail` |
| Wine | `WineGlobe` | `WineTime` | `WinePasses` | `WineDetail` |

Each reference preview has variants for English LTR and Persian RTL at 390 × 844 dp. The previews use the production HUD, sheet, pass-card, Detail and nav composables with a fixed clock and pass fixture. Layoutlib cannot host the native EGL viewport, so only the Earth image is substituted with debug-only stills baked from the production textures. Neither the stills nor the fixtures are included in the release APK.

## Checks

- `SatellitesRedesignTest`: state exclusivity and restoration inputs, signed and localized numbers, spotlight ordering, time magnets, and free-area globe framing.
- `Earth3DTest` and `StarlinkPipelineTest`: projection and occlusion, and preservation of the existing Earth and orbit data paths.
- On-device review: verify pinch and double-tap outside the HUD, tap overlapping markers twice, cycle three satellites with Focus Card swipes, scrub while watching the Earth and dismiss without returning to Live, and verify a scheduled reminder after leaving and re-entering the tab. Repeat on a narrow Persian screen at 130% font scale and with device animations disabled.

For short landscape/large windows, the resting globe fits the height available between the header and Focus Card instead of cropping a width-sized sphere. Pinch remains available to zoom in.

```sh
./gradlew testDebugUnitTest -Pgravity.ci.tests=false
./gradlew assembleRelease
```

GitHub Actions signs and publishes the installable release in its `ZIG-real-app-apk` artifact. The standard local release uses debug signing when the project's signing configuration is not supplied.

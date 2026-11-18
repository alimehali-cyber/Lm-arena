# Satellites · The Globe Is the App

The tab is an edge-to-edge native Earth3D viewport with an inset-aware HUD. `SatellitesStage` connects the real clock, observer, catalogue, TLE and notification paths. `SatelliteHud` measures the controls and exposes a shared `GlobeViewport` to the native renderer; a single `SatPanel` owns sheets and trays. Time and Layers do not install a fullscreen scrim. Camera orientation, collection and simulation offset are not intentionally reset when returning from Detail.

## Fixtures and review

`app/src/debug/java/.../ui/satellites/SatellitePreviews.kt` provides deterministic fixtures for three skins, three themes, English/Persian and normal/large text. `ReviewPage` includes rest, Recenter, first run, offline, catalogue, search, customization, passes, alerts, trays, Detail, polar chart and shape examples. They use a drawn globe stub and flat glass, not the GLES viewport or real ephemerides. Layoutlib/preview fixtures do not verify native rendering, touch, performance or appearance of frosted effects.

Generate optional host-side captures as described in [`review/README.md`](../review/README.md); none are checked in or claimed to have been inspected. The independent shape and media checks and their limitations are recorded in [`REPORT.md`](../REPORT.md). A native device review remains pending: see [`DEVICE_CHECKLIST.md`](../DEVICE_CHECKLIST.md).

## Commands

```sh
python3 tools/satellites/build_shapes.py --check
python3 tools/satellites/audit.py
python3 -m unittest discover -s tools/satellites -p 'test_*.py' -v
# Requires a JDK and Kotlin compiler; tests the Android-free core only:
tools/satellites/check-core.sh
# Requires JDK 21, Android SDK 36.1 and Gradle dependencies:
./gradlew testDebugUnitTest -Pgravity.ci.tests=false
./gradlew assembleRelease
```

The GitHub Actions build workflow signs and publishes `ZIG-real-app-apk` only after its test gate and package/signature checks pass. A local release without repository signing settings falls back to the debug signing key; it is not an update-compatible substitute for the permanent-key-signed artifact.

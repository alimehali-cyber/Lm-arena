# Aether Telemetry UI — Design System & Implementation Guide

`AppUiStyle.AETHER` implements the **Aether Telemetry UI** design language across ZIG (`com.alijafari.red.astronomy.ui.aether`), selectable alongside `Default UI`, `Spatial UI`, and `Cyber Glass UI` from every Settings dialog.

## 1. Visual Architecture — Frameless Rim-Lit Optical Lenses

Cards in Aether Telemetry UI are **frameless edge-lit surfaces** (never nested inside outer bezels or double containers):
- **Directional `1.dp` Specular Rim Border (`Modifier.specularRimBorder`)**: `Brush.linearGradient` catching light at the top-left (`45%` white), fading through `8%` and `3%` white across the middle, with a `22%` bounce reflection at the bottom-right. Interactive lenses dynamically tilt the specular rim angle toward the user's touch coordinate.
- **Surface & Color Tokens (`AetherDesignSystem.kt`)**:
  - `AetherCanvasVoid` (`#08080C`) — base screen background beneath drifting ambient mesh blobs.
  - `AetherCrimsonCoreCenter` (`#1C020D`), `AetherCrimsonCoreMid` (`#5E052A`), `AetherCrimsonCoreEdge` (`#9E0B45`) — hero radial jewel card (19.7:1 center AAA contrast with glowing perimeter).
  - `AetherJewelPreset.SAPPHIRE` (`#031724` -> `#073554` -> `#0E5A8A`) & `AetherJewelPreset.EMERALD` (`#041E15` -> `#084736` -> `#0D7A5F`).
  - `AetherFrostedGlassFill` (`#14FFFFFF`, 8% white) — secondary spatial cards refracting the background via Chris Banes' Haze (`dev.chrisbanes.haze:haze:1.1.1`).
  - `AetherGlowWhite` (`#FFFFFF`) & `AetherMutedTelemetry` (`#80FFFFFF`).
- **Corner Geometry**:
  - Hero Jewel & Frosted Spatial Cards: `36.dp` (`squircleShape(36.dp)`)
  - 60-Tick Radial Dial Cards: `34.dp` (`squircleShape(34.dp)`)
  - Mini Glowing Mesh Pills: `50%` full capsule (`82.dp` height)
  - Floating Top Status Capsule & Navigation Dock: full capsule (`RedShapes.capsule`)

## 2. Kinetic Motion, Shaders & Tactile Physics

1. **Ambient Background Breathing (`AetherAmbientBackground.kt`)**:
   - 9000ms `InfiniteTransition` (`LinearEasing`, `RepeatMode.Reverse`) driving crimson (`#59092B`) and deep sapphire (`#14385C`) radial blobs along a smooth figure-8 Lissajous path.
   - GPU-accelerated Android 13+ AGSL `RuntimeShader` (`AETHER_AGSL_MESH_SHADER`) with frosted micro-grain noise and automatic pure-Compose `Canvas` fallback on API 24–32.
   - Records into `HazeState` (`Modifier.haze`) so `AetherFrostedSpatialCard`, `AetherRadialDialCard`, `AetherFloatingStatusCapsule`, and `AetherFloatingNavBar` refract the live ambient mesh via `Modifier.hazeChild`.
2. **Synthesizer Oscilloscope & Spline Canvases (`AetherOscilloscopeCanvas`, `AetherSplineTelemetryCanvas`)**:
   - Cubic-bezier arches (`path.cubicTo`) revealed left-to-right via `PathMeasure.getSegment` (`spring(dampingRatio = 0.8f, stiffness = 200f)`).
   - Thick neon-white bottom troughs (`BlurMaskFilter(12f, BlurMaskFilter.Blur.NORMAL)`) breathing between `0.70f` and `1.0f` alpha.
3. **Rolling Dot-Matrix Counters (`AetherDotMatrix.kt`)**:
   - Circular LED dot-matrix renderer (`AetherDotMatrixText`) supporting Latin and Persian numerals/symbols without `FontFamily.Monospace`.
   - Vertical rolling `AnimatedContent` (`slideInVertically { it } + fadeIn()` / `slideOutVertically { -it } + fadeOut()`) via `AetherRollingDotMatrixCounter` and `AetherRollingMetricText`.
4. **Tactile Press & Haptics**:
   - Interactive lenses scale to `0.97f`–`0.975f` via `graphicsLayer` with `Spring.DampingRatioMediumBouncy`.
   - `AetherMicroStepper` (`←  ━  →`), capsules, and navigation tabs emit `HapticFeedbackType.TextHandleMove`.

## 3. Package Map (`com.alijafari.red.astronomy.ui.aether`)

- `AetherDesignSystem.kt` — Tokens, `AetherThemeProvider`, `specularRimBorder`, `AetherJewelTelemetryCard`, `AetherFrostedSpatialCard`, `AetherRadialDialCard`, `AetherGlowingMeshPillCard`, `AetherFloatingStatusCapsule`, Oscilloscope & Spline canvases, `AetherMicroStepper`, LED status arrays.
- `AetherAmbientBackground.kt` — AGSL + Canvas figure-8 ambient mesh + Haze source recording.
- `AetherDotMatrix.kt` — LED Dot-Matrix glyphs & vertical rolling `AnimatedContent` counters.
- `AetherFloatingNavBar.kt` — Rim-lit Haze-blurred floating status dock.
- `AetherHomeScreen.kt` — Full-bleed sky scene + Floating Status Capsule + Crimson Sky Telemetry Jewel Card + Bento 60-Tick Dial & Mesh Pills + Sapphire Lunar & Emerald Eclipse Lens Cards + Tonight Telemetry Stream.
- `AetherSatellitesScreen.kt` — Sapphire Orbital Ground-Track Jewel Card + Bento Elevation Dial & Mesh Pills + Spline Time Scrubber + 3-Day Visible Passes + Constellation Deck.
- `AetherLabScreen.kt` — Interactive Lorentz Factor Jewel Synthesizer + Bento Proper-Time Dial & Mesh Pills + Relativistic Engine Cards (`TIME_DILATION`, `GRAVITY_SANDBOX`, `ZIG_BLACKHOLE`).
- `AetherSettingsModal.kt` — Rim-lit spatial configuration modal with all 4 UI styles (`DEFAULT`, `SPATIAL`, `CYBER`, `AETHER`) and `DARK` / `WINE` / `LIGHT` theme support.

# Aether Telemetry UI (`AppUiStyle.AETHER`)

## 1. Aesthetic & Architectural Philosophy

**Aether Telemetry UI** (`com.alijafari.red.astronomy.ui.aether`) is ZIG's fourth selectable interface design system (`AppUiStyle.AETHER`), fusing **Apple visionOS Spatial Optical Glass**, **Nothing OS Dot-Matrix Telemetry**, and **Teenage Engineering Analog-Synthesizer Oscilloscopes** into frameless, rim-lit optical lenses floating directly over deep obsidian space.

### Core Invariants
1. **Frameless Rim-Lit Spatial Lenses (Zero Outer Bezels):** Every hero card (`squircleShape(36.dp)`), radial bento dial (`squircleShape(34.dp)`), and vertical/horizontal pill capsule (`RedShapes.capsule`, following `Squircle.kt` capsule rules) is a single frameless optical slab bounded solely by a razor-thin `1.dp` directional specular rim (`Modifier.specularRimBorder`) — never wrapped inside double borders or bulky chassis frames.
2. **Jewel-Tone Radial Optical Cores & Twin Vertical Pill Celestial Lenses:** Hero telemetry cards (`AetherJewelCard`) use radial core gradients (`AetherCrimsonCoreCenter` `#1C020D` -> `AetherCrimsonCoreMid` `#5E052A` -> `AetherCrimsonCoreEdge` `#9E0B45`, plus `SAPPHIRE`, `EMERALD`, and `WINE_RUBY`) that remain near-black in the center for crisp contrast while blooming richly along the inner perimeter. On `AetherHomeScreen`, the side-by-side Lunar (`SAPPHIRE`) and Eclipse (`EMERALD`) cards are sculpted as twin vertical pill capsules (`shape = RedShapes.capsule`) whose semicircular top domes frame enlarged concentric circular `MoonPhaseRender` and `EclipseRender` plates (`renderSize = cardWidth - 20.dp`).
3. **Hardware Dot-Matrix Readouts & Tactile Micro-Controls:** Primary 2–3 digit live counters (`AetherDotMatrixText`, `AetherRollingDotMatrixCounter`) render circular 7x9 LED matrices in `AetherGlowWhite` (`#FFFFFF`) with both ASCII and Persian (`۰–۹`, `٪`, `٫`) glyphs. Chunky Material 3 filled buttons are replaced inside cards by `← ━ →` micro-steppers (`AetherMicroStepper`), `1.dp` pill badges (`AetherPillBadge`), and `6.dp` filled/hollow LED legend dots (`●` / `○`).

---

## 2. Surface & Color Tokens

| Token | Value | Role |
| :--- | :--- | :--- |
| `AetherCanvasVoid` | `#08080C` | Deep obsidian spatial backdrop (Dark mode) |
| `AetherCrimsonCoreCenter` | `#1C020D` | Near-black inner center of Crimson Jewel Hero Card |
| `AetherCrimsonCoreMid` | `#5E052A` | Saturated ruby mid-ring |
| `AetherCrimsonCoreEdge` | `#9E0B45` | Outer inner-glow edge bloom |
| `AetherSapphireCoreCenter..Edge` | `#031724` -> `#0E5A8A` | Sapphire orbital/lunar jewel lens |
| `AetherEmeraldCoreCenter..Edge` | `#041E15` -> `#0D7A5F` | Emerald relativistic/eclipse jewel lens |
| `AetherFrostedGlassFill` | `#14FFFFFF` | 8% translucent frosted spatial card fill |
| `AetherSpecularRimStart` | `#66FFFFFF` | Top-left `1.dp` specular light catch (40% white) |
| `AetherSpecularRimMid` | `#0DFFFFFF` | Center rim fade (5% white) |
| `AetherSpecularRimEnd` | `#33FFFFFF` | Bottom-right bounce reflection (20% white) |
| `AetherGlowWhite` | `#FFFFFF` | Pure white LED dots, trough glow, primary readouts |
| `AetherMutedTelemetry` | `#80FFFFFF` | Inactive baseline, hollow legend dots, muted ticks |

`AetherThemeProvider` adapts both `LocalAetherPalette` and `LocalRedColors` across `ThemeMode.DARK`, `ThemeMode.WINE`, and `ThemeMode.LIGHT`, ensuring shared modals and `ImmersiveChrome` (`CompassARScreen`, `GravitySandboxRoot`, `AntarRoot`) inherit contrast-verified accents (`>= 4.76:1` WCAG contrast with `readableInkOn`).

---

## 3. Custom Compose `Canvas` Telemetry Components

1. **`AetherOscilloscopeWaveformCanvas`:** Continuous cubic-bezier (`cubicTo`) arches separated by flat horizontal bottom troughs rendered with `android.graphics.BlurMaskFilter(Blur.NORMAL)` neon white bloom and a breathing `InfiniteTransition` glow alpha, followed by a flat baseline and `5` top-right LED status dots (`AetherLedDotArray`).
2. **`AetherRadialDialCard`:** Frosted `34.dp` bento square gauge drawing `60` radial `StrokeCap.Round` tick marks (`i in 0 until 60`), elongating `i in 6..11` inward (`outerRadius * 0.65f`) for the signature upper-right telemetry highlight sector.
3. **`AetherGlowingMeshPillCard`:** Full `50%` capsule (`82.dp` height) with off-center pink (`#D96B98 -> #32233B`) or amber (`#E07A42 -> #3D1D14`) radial gradient bloom and a `1.dp` pill badge.
4. **`AetherFloatingStatusCapsule`:** Top `38.dp`-tall `100.dp` rim-lit floating pill (`✦ ... • ... ⚡`).
5. **`AetherAmbientBackground`:** `9000 ms` `InfiniteTransition` figure-8 Lissajous radial color drift with an optional Android 13+ (`TIRAMISU`) AGSL `RuntimeShader` refraction veil.

---

## 4. Screen & Settings Integration

- **Package:** `app/src/main/java/com/alijafari/red/astronomy/ui/aether/`
  - `AetherDotMatrix.kt`
  - `AetherDesignSystem.kt`
  - `AetherAmbientBackground.kt`
  - `AetherFloatingNavBar.kt`
  - `AetherHomeScreen.kt` (uses shared `AETHER_*_REFRESH_MS` buckets and `HomeAstroMemo`)
  - `AetherSatellitesScreen.kt`
  - `AetherLabScreen.kt`
  - `AetherSettingsModal.kt`
- **Selectable Across All UI Styles:** Registered as `AppUiStyle.AETHER` in `Models.kt`, persisted via `MainViewModel.setUiStyle`, routed in `MainActivity.kt`, `ImmersiveChrome.kt`, and `AntarControlPanel.kt`, and exposed (`settings_ui_style_aether`) in `SettingsDialog.kt`, `SpatialSettingsModal.kt`, `CyberSettingsModal.kt`, and `AetherSettingsModal.kt` in both English (`strings.xml`) and Persian (`values-fa/strings.xml`).

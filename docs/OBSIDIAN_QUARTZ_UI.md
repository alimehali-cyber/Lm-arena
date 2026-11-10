# Obsidian Quartz & Crimson Telemetry

A fourth interface design language for ZIG, selected from **Settings → Interface Style**
(`AppUiStyle.OBSIDIAN`). It is not a standalone screen: it is a full style branch, alongside
Default, Spatial and Cyber Glass, with its own Home, Lab, Satellites, dock and settings modal.

The art direction is one industrial-design studio concept, not a theme pack:

| Ingredient | Value |
| --- | --- |
| Canvas | Editorial Alabaster Mist `#E8E4DE` (Light), obsidian `#0B0206` (Dark), cabernet `#140309` (Wine) |
| Hero lens | Velvet Crimson Quartz `#170109` → `#4B0420` → `#8C093B` → `#B81450` |
| Secondary surface | Smoked Slate Quartz `#1D242B` → `#5D7687` @ 82% |
| Capsule accents | Rose Quartz `#D95B8A`, Molecular Cobalt `#6B85B8`, Warm Amber `#D97236` |
| Signal ink | Deep Velvet Crimson `#9E0B45` |
| Motion | `dampingRatio = 0.84f`, `stiffness = 280f` — critically damped, never bouncy |

## The four rules

### 1. No fake fonts

Primary telemetry readouts (`04`, `16.2`, `2.8`, `76`) are rendered by `DotMatrixNumber`
(`ui/obsidian/ObsidianDotMatrix.kt`): a procedural 5×7 LED engine that draws every lit cell as a
circular micro-dot, with an optical bloom pass (`BlurMaskFilter` at 2.4× dot radius) and an
optional ghost matrix. There is no typeface involved — the glyph tables *are* the type. Persian
digits are mapped onto the same tables so an FA locale never loses a digit.

Never substitute `FontFamily.Monospace`.

### 2. No plastic gradients — optical quartz surfaces

`Modifier.opticalQuartzSurface(cornerRadius, innerRefractionColor, rimHighlightAlpha)` is the only
surface recipe. It layers, in order:

1. an `18.dp` blurred inner-perimeter refraction glow (`BlurMaskFilter(16.dp, NORMAL)`) so light
   bleeds inward while the centre stays deep obsidian;
2. an Android 13+ AGSL `RuntimeShader` frosted micro-grain overlay (`FROST_NOISE_AGSL`);
3. a directional `0.85.dp` linear-gradient specular rim hairline.

Capsules additionally carry `Modifier.obsidianInnerDottedTrack()`: a `6.dp` inset dashed inner
perimeter track (`PathEffect.dashPathEffect(floatArrayOf(3f, 7f), 0f)`, `0.8.dp`).

Never `Modifier.border(1.dp, Color.White)`, never an outer double bezel.

### 3. No color salad

Every screen sits over `EditorialSilhouetteBackdrop`: the Alabaster canvas with a soft-focus,
9-second breathing obsidian/burgundy silhouette and one cool slate aberration rim. Cards are
either a Velvet Crimson lens (hero) or Smoked Frosted Quartz (secondary). The only chromatic
exceptions are the three capsule accents, one per card.

### 4. No emojis, no Material icons

All micro-icons — arrows, crosshairs, the plus, gear, globe, calendar, chevrons, the lab/AR/
satellite/home dock marks — are drawn mathematically on `Canvas` by
`ObsidianVectorIconGlyph` at `1.1–1.3.dp` hairlines. `androidx.compose.material.icons` is not
imported anywhere in `ui/obsidian/`.

## File map

| File | Contents |
| --- | --- |
| `ObsidianDesignSystem.kt` | `ObsidianPalette` + three modes, `ObsidianThemeProvider` (also adapts `RedColorTokens`), `opticalQuartzSurface`, `ObsidianVectorIconGlyph`, `ObsidianSignalDot`, `ObsidianHairlineSlider` |
| `ObsidianDotMatrix.kt` | `DotMatrixNumber`, `DotMatrixCounter`, `ObsidianDotRingGauge` |
| `ObsidianAmbientBackground.kt` | `EditorialSilhouetteBackdrop` |
| `ObsidianFloatingNavBar.kt` | smoked quartz dock + `obsidianInnerDottedTrack` |
| `ObsidianHomeScreen.kt` | hero Velvet Crimson lunar lens, smoked quartz Gaussian altitude card, 2×2 refracting bento |
| `ObsidianLabScreen.kt` | quartz plates hosting Time Dilation / Gravity Sandbox / Antar |
| `ObsidianSatellitesScreen.kt` | `ISSScreen` under the Obsidian theme layer |
| `ObsidianSettingsModal.kt` | the seven settings sections, including the UI selector row |

## Real data, not placeholders

`ObsidianHomeScreen` reads the same pipeline as every other Home variant: `viewModel.skyLiveClock`
plus `rememberReadyTimeMs` bucketing, `HomeAstroMemo.get(MoonKey(...))` for the ELP2000 Moon and
`SunEngine` + `CoordinateEngine` for solar altitude. The crimson lens shows real illumination
percentage, phase name, lunar age and distance; the stepper scrubs `MainViewModel.moonDayOffset`
(±30 days) exactly like the Moon card on Default Home.

## Motion

Every morph uses `spring(dampingRatio = OBSIDIAN_SPRING_DAMPING, stiffness = OBSIDIAN_SPRING_STIFFNESS)`.
`HapticFeedbackType.TextHandleMove` fires on each stepper click and each curve-scrub day boundary.

## Adding another style later

Touch: `AppUiStyle` (`domain/Models.kt`), `MainActivity` (provider + backdrop + 3 tab branches +
dock + settings), all four settings modals, `R.string.ui_style_*` in `values/` and `values-fa/`,
`ImmersiveChrome.chromeTokens()` and `AntarControlPanel.rememberAntarChromeTokens()`, then update
`SpatialUiTest`, `ImmersiveChromeTest` and `ObsidianUiTest`.

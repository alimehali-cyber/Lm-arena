# The Liquid Glass recipe (Part 6)

What the glass is made of, where each dial lives, and why the cards can finally sample the backdrop
without the Kyant assertion. Everything here is asserted by `Part6GlassAndInkTest` (arithmetic) and
`Part6ScreenChromeTest` (wiring).

## 1. Why the cards had no glass before

`Modifier.layerBackdrop(backdrop)` does not merely read a texture: while it draws, it starts a
**recording**. A `drawBackdrop()` inside that same instance re-enters the recording, which the library
refuses with *"Can not perform this action inside of drawBackdrop!"*. The content tree sat inside the
recording, so every card was forbidden from sampling it — and was pinned to its `fallbackColor` instead,
with `CompositionLocalProvider(LocalLiquidGlassBackdrop provides null)` in `MainActivity` enforcing it.

Part 6 splits the layers by **what they contain**:

| Layer | Records | Sampled by |
|---|---|---|
| `glassBackdrop` | the theme background, the live sky and the photo — nothing else | the cards, through `LocalLiquidGlassBackdrop` |
| `backdrop` | the whole content area, as before | `FloatingBottomBar` and the dialogs, which are siblings outside it |

The sky is still drawn **once**: the backdrop layer is a child of the content layer, so recording the
content blits the already-recorded backdrop into it. A card drawing `glassBackdrop` is therefore reading
a sibling that finished recording, never itself.

## 2. Colour mixing

The film is a two-colour HSV recipe rather than a fixed white:

| Dial | Field | Range |
|---|---|---|
| First hue | `tintHueA` | 0–360° |
| Second hue | `tintHueB` | 0–360° |
| Mix between them | `tintMix` | 0–1, along the **shorter** way round the wheel |
| Saturation | `tintSaturation` | 0 = neutral white, 1 = fully saturated at the mixed hue |
| Alpha | `tintAlpha` | 0–0.40 |

`glassFilmColor(config)` resolves them once and is the *same* function the Settings swatch draws, so the
preview is the value the cards are given. Saturation zero reproduces the shipped white film exactly.

Named recipes (`GlassFilmPreset`: Neutral, Sea glass, Rose quartz, Moonstone, Obsidian) write those same
five numbers — a preset is a slider position, so picking one and then dragging a slider never fights a
hidden state. `LiquidGlassConfig.matches(preset)` lights the chip back up, with the tolerances the sliders
themselves have.

## 3. Shader design

| Control | Field | Effect |
|---|---|---|
| Refraction recipe | `shaderProfile` | Crystal (×0.45 blur, ×1.30 refraction), Balanced (×1), Frosted (×1.90, ×0.55) |
| Where the light lies | `sheen` | None, Top, Diagonal, Rim |
| How bright it is | `sheenStrength` | 0–1 → up to 26 % white |

The profile multiplies the blur and refraction the style already asks for, so a frosted card genuinely
diffuses more of the backdrop; it is a shader setting, not a colour. The sheen is a real gradient drawn
by the app *inside* the glass clip and *under* the content — light on the outside of the surface, not on
the text — and it is built in a `drawWithCache` block, so it costs one brush per layout rather than one
per frame.

## 4. Smooth edges

`CornerSmoothness` turns the corner into a one-parameter family:

- **0** — `n = 2`, reach `1.0 r`. Not "almost a circle": substituting `n = 2` into the family's equation
  gives `x² + (1 − y)² = 1`, an **exact circular arc** of radius `r`.
- **1** — `n = 4`, reach `1.6 r`: the app's own squircle, and the default.
- Between — both numbers interpolate, so the curvature at the edge join rises continuously from G1
  toward G2 instead of switching between two looks.

The shipped `n = 4` table in `Squircle.CORNER` is untouched: at the default dial position
`Shape.withCornerSmoothing` returns the very shape the call site passed (`assertSame`). Any other
position generates that exponent's chain — sampled at equal arc length, cubic Hermite between the nodes,
end tangents pinned to the edges — and clips the glass to it, so the refraction follows the outline.
Capsules and shapes the app does not own the radii of are returned untouched: smoothing a pill flattens
its ends, which is a bug and not a preference.

## 5. Where to change things

| What | Where |
|---|---|
| The recipe (defaults, presets, profiles, sheen) | `ui/theme/LiquidGlass.kt` |
| The corner family and the generated chain | `ui/theme/CornerSmoothness.kt` |
| The film → shader mapping | `Modifier.liquidGlass` |
| Persistence | `MainViewModel.updateLiquidGlassConfig` (keys `liquid_glass_*`) |
| The controls | `SettingsDialog` → *Glass Colour Mixing / Shader Design / Edge Smoothness* |
| The two layers | `MainActivity.onCreate` |

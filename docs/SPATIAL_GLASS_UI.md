# The Spatial Glass interface

The app can be drawn with two interfaces. **Default** is the app as it has always been: one flat,
opaque surface per panel, ink measured against that surface, nothing behind it. **Spatial Glass** is
the optional second one — frosted translucent panels floating over a slowly drifting ambient mesh,
specular cut-glass edges, dot-matrix instrument readouts, a floating capsule bar.

Settings → **Interface** switches between them, and picks the palette of the glass one:

| palette | what it is |
|---|---|
| **Aurora / شفق** | coral, peach, cyan and violet blobs drifting through a violet-black night |
| **Wine · Mei / شرابی · می** | the same construction in wine red and burgundy — cellar-dark base, burgundy and crimson light, a rose specular and a gold edge |

Both are one design system; the palette only changes the world, never the rules.

## Where it lives

| file | what it owns |
|---|---|
| `domain/Models.kt` | `UiStyle` (`CLASSIC`, `SPATIAL`), `SpatialPalette` (`AURORA`, `WINE`) |
| `ui/theme/SpatialGlass.kt` | the palettes as `SpatialGlassSpec`, the material (`spatialGlassMaterial`, `spatialGlassGlow`), the ink rule (`spatialSurfaceInk`), the token set (`spatialGlassColorTokens`) |
| `ui/theme/SpatialAmbient.kt` | the ambient world behind the screens (`SpatialAmbientBackdrop`) |
| `ui/theme/Surface.kt` | `RedSurface` — the one composable every panel is drawn by — branches on the material |
| `ui/theme/Theme.kt` | resolves (style, palette) → spec, tokens, Material scheme, readout face; provides `LocalSpatialGlass` |
| `ui/theme/Type.kt` | `DotoFontFamily`, `LocalStatFontFamily`, `RedTypographyTokens.statNumber` / `statNumberSmall` |
| `ui/components/SettingsDialog.kt` | the Interface section and its live preview |

## The rules

1. **Panels.** Translucent sheet (`glassFill`, ~7–10 % white, tinted per palette), squircle corners
   from the existing radius scale, a specular rim instead of a flat hairline, a drop shadow tinted by
   the palette (`spatialGlassGlow`), and a wide, generous padding scale — unchanged from the app.
2. **Depth and lighting.** Every panel re-lights itself from its own palette: a warm radial field from
   the top-left, a cool one from the bottom-right, a diagonal specular sweep, weight at the bottom, and
   a bright-to-faint-to-bright gradient stroke around the squircle. That is the "thick cut glass" rim;
   a flat `border` cannot express it, so the flat border is skipped while the material is on.
3. **Typography.** Prose keeps the geometric sans (the platform face in English, Vazirmatn in Persian)
   at light weights. Values — times, magnitudes, percentages, counts — use `statNumber`, which is the
   dot-matrix face **Doto** (bundled, `res/font/doto_dot_matrix.ttf`) while glass is on.
4. **Gradients.** Soft ambient mesh behind everything (five radial fields, screen-blended), visible
   through every sheet, plus a faint dot lattice — the retro-futurist half of the style.
5. **Spacing and motion.** High padding, pill shapes, floating elements; the ambient mesh drifts on one
   linear 26-second cycle, the bottom bar's selected tab is a capsule that springs between items.

## Two things worth knowing

**Nothing samples the screen behind it.** A true backdrop blur needs a render-surface capture, and
this app deliberately does not have one: every panel's ink has to be known before it is painted, and
the app has to look the same on every device. The material gets the same look by *re-lighting* rather
than *re-sampling*: because the ambient world is made of very soft, very wide colour fields, painting
large-scale versions of the same fields inside a panel's outline is what a blur of them would have
produced. The result is identical to the eye and cheaper than a capture, and no test has to be told
what is behind a card, because nothing is.

**The ink is still guaranteed.** Glass fills are translucent, so the theme's usual contrast repair
(which measures against the surface itself, and would read a 10 % white sheet as white and demand
black text) is skipped for this palette. Instead `spatialSurfaceInk` composites each panel's fill over
the palette's own base colour — the colour the eye actually sees — and measures that. Every glass panel
therefore resolves light ink over the dark ambient world, by construction.

## Adding a palette

Add one `SpatialGlassSpec` in `SpatialGlass.kt`, one enum value in `SpatialPalette`, and one branch in
`spatialGlassSpecFor`, then add the spec to `SpatialGlassPalettes` so it appears in Settings. Nothing
else needs to change: surfaces, ambient world, tokens, previews and the settings swatch all read the
spec.

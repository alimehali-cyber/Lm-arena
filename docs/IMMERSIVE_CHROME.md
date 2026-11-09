# The immersive chrome

ZIG has two screens that are not pages but **places**: the AR sky (`CompassARScreen`) and the Gravity
Sandbox (`com.zig.gravity`). Both open full-screen, hide the floating navigation bar, and float their
controls over a live scene — a camera feed, a simulated sky, a tabletop of orbiting bodies. Neither can
wear a normal screen layout (no list, no opaque page background, no top band), so both grew their own
chrome, and by the time anyone looked the two of them disagreed with each other *and* with the rest of
the app:

| what was wrong | AR sky | Gravity Sandbox |
| --- | --- | --- |
| second brand colour | static `AccentPrimary`, a literal sky-blue orbit path | a teal accent (`#2DD4BF`), teal wormhole mouth and selection ring |
| hard-coded panels | `#DD0D1B2A`, `#DD2A0D0D`, `#CC1A1F36`, `black @ 0.75` | translucent teal glass |
| status colours | literal amber `#F59E0B`, green `#4CAF50`, red `#FF5252` | — (themes) |
| Material defaults | `FilterChip`, `SuggestionChip`, `TextButton`, `Button`, `OutlinedButton` | `MaterialTheme.typography` in one sheet |
| type | `MaterialTheme.typography.*` at Material sizes | ad-hoc `fontSize = 11.sp … 16.sp` |
| ink | `Color.White` text on panels that could be light | teal label ink on a teal panel |

None of those survive. Both screens are now drawn by **one** chrome layer, `ui/theme/ImmersiveChrome.kt`.

## The rule

> Every colour, corner, hairline and type step in an immersive screen is **derived from `RedTheme` at
> composition time**. The scene behind the controls may have its own material; the controls may not.

`chromeTokens()` reads the theme in force at the exact point it is called:

```
chromeTokens()
 ├─ accent, accentSoft, gold        ← RedTheme.colors.accentRed / accentRedSubtle / accentGold
 ├─ onAccent                       ← readableInkOn(accentRed)      (measured, never guessed)
 ├─ fill, fillStrong, fillQuiet    ← surfaceElevated / surfaceGrouped at panel alpha
 ├─ stroke, strokeStrong           ← border / separatorOpaque
 ├─ textPrimary/Secondary/Tertiary ← the theme's own ink steps
 ├─ success/warning/danger         ← statusSuccess / statusWarning / statusError
 ├─ panel/row/pill geometry        ← RedCornerRadius.window + RedCornerRadius via squircleShape
 └─ glow                          ← transparent in DEFAULT, an accent wash in CYBER / SPATIAL
```

Because the theme it reads is the theme the user selected *after* the Cyber and Spatial providers have
adapted it (`CyberThemeProvider`, `SpatialThemeProvider` copy `RedColorTokens`), the immersive chrome
follows all three interface styles and all three theme modes with **no branching at the call sites**:

| selected style | accent the chrome wears | panel geometry |
| --- | --- | --- |
| `DEFAULT` | the vermilion `accentRed` of the theme | 40 dp window squircle |
| `CYBER` | Cyber's `NothingRed` / `#E14938` / garnet | 36 dp (cut-glass), accent glow on |
| `SPATIAL` | Spatial's neon rose / sky blue, warm paper | 40 dp window, accent glow on |

| theme mode | accent | panel fill |
| --- | --- | --- |
| dark | `#E53935` | obsidian `#0E0E12` at 92 % |
| light | `#D32F2F` | quartz `#F2F2F7` at 95 % |
| wine | `#E04263` | merlot `#260B15` at 92 % |

`LocalAppUiStyle` (provided once in `MainActivity`, next to the theme) is the only thing the chrome asks
about the *style* rather than the palette, and it uses it for geometry and glow only.

## The components

Seven components, all in `ui/theme/ImmersiveChrome.kt`, all drawn with `RedSurface` + `squircleShape` +
`RedTypographyTokens` and all sharing one press spring (0.96, damping 0.72, stiffness 460) and one
180 ms colour cross-fade:

| component | what it replaced | where it is used |
| --- | --- | --- |
| `ChromeCapsule` | AR smart pill, sandbox "Scenes"/"Predict"/"Open inspector" chips | both screens |
| `ChromeIconDisc` | `IconButton`s in the AR header, sandbox HUD controls | both screens |
| `ChromeToggleChip` | `FilterChip`, `SuggestionChip`, alignment target chips | AR filters / sensors / alignment |
| `ChromeTextAction` | `TextButton` ("Show all", "Telemetry", "Cancel", "Reset", "Clear target") | both screens |
| `ChromePrimaryAction` | `Button` ("Align", "View details") | both screens |
| `ChromePanel` | every hand-rolled `Box { clip; background; border }` panel | both screens |
| `ChromeBanner` | four AR banners with four different navy fills | camera permission, magnetic interference, recalibration, sandbox notices |
| `ChromeDivider`, `ChromeSectionTitle`, `ChromeCaption` | `HorizontalDivider` literals and bare `Text(style = …)` | both screens |

Two states, and only two: **selected** fills the control with the accent and uses the measured on-accent
ink; **highlighted** keeps the panel fill and puts the accent in the border and the glyph, which is how
the app already says "a setting has moved away from its default".

## What stayed its own thing, and why

* **The night-vision filter** (`Color(0xFFFF1E1E)` multiplied over the camera image). It is a
  photographic filter on the sensor feed, not chrome; it must not follow the theme. It is the only
  colour literal left in `CompassARScreen`, and a test pins that.
* **The reticle and the optical ticks.** White by intent, because they are drawn over a photograph. The
  *accented* ring inside them is the theme accent, and the alignment overlay's reticle follows it.
* **The tabletop.** `TableSurfaces` keeps its gradients, printed star fields, vignettes, trail alphas
  and cast shadows: that material is the simulation's, the way a star's colour is data. `GravityColors`
  keeps every one of those fields and gives up only the six that were interface:
  `accent`, `onSurface`, `onSurfaceDim`, `chrome`, `chromeBorder`, `selection`.
* **Body identity colours.** A wormhole is still the colour `BodyCatalog` says it is.

## Motion

| element | spec |
| --- | --- |
| any chrome control press | `spring(dampingRatio = 0.72, stiffness = 460)` to 0.96 |
| selection / highlight change | `tween(180)` on fill, border and ink |
| AR expanded panel | `expandVertically(tween(300, FastOutSlowInEasing))` from the top + `fadeIn(tween(200))`; exit 220/140 |
| AR alignment overlay | whole layer fades in 340 ms; the reticle scales 0.92 → 1.00 |
| sandbox camera panel | `expandVertically(tween(220))` + `fadeIn`; exit 160 ms |
| sandbox long-press menu | panel + hairline-separated rows, no spring on open (it appears under the finger) |

## File map

```
app/src/main/java/com/alijafari/red/astronomy/ui/theme/ImmersiveChrome.kt   ← tokens + components
app/src/main/java/com/alijafari/red/astronomy/ui/screens/CompassARScreen.kt ← AR chrome, rewritten
app/src/main/java/com/zig/gravity/ui/theme/GravityTheme.kt                  ← withAppChrome(surface, theme)
app/src/main/java/com/zig/gravity/ui/GravitySandboxRoot.kt                  ← sandbox chrome
app/src/main/java/com/zig/gravity/ui/HudBar.kt                              ← HUD platter, speed ladder
app/src/main/java/com/zig/gravity/ui/SandboxSlider.kt                       ← caption label, haloed thumb
app/src/main/java/com/zig/gravity/ui/{AddBody,Inspector,Preset,TableSurface}Sheet.kt, TeachingCard.kt
                                                                            ← sheet fills / ink from chrome
app/src/main/java/com/alijafari/red/astronomy/MainActivity.kt               ← provides LocalAppUiStyle
app/src/test/java/com/alijafari/red/astronomy/ImmersiveChromeTest.kt        ← the guards below
```

## The guards

`ImmersiveChromeTest` asserts, in a JVM test that runs on every push:

1. the chrome declares **no** literal colour, reads the theme's accent / inks / fills / border / status
   tokens, and measures the on-accent ink;
2. panels take `RedCornerRadius.window` (36 dp in CYBER), pills are capsules;
3. `MainActivity` publishes the selected `AppUiStyle`;
4. the AR screen calls the shared components and **no longer** calls `MaterialTheme.typography`,
   `FilterChip`, `TextButton`, `ButtonDefaults`, `AccentPrimary`, `StatusGood`, `Color(0xDD…)`,
   `Color(0xCC…)` or the sky-blue orbit literal — and keeps exactly one literal, the night-vision filter;
5. the sandbox rebases its chrome on the app theme, drops the teal, uses the shared capsules / panels /
   banner / add disc, and draws its HUD and slider with the app type scale;
6. all five sandbox sheets take their fill and ink from the chrome;
7. for all nine palettes the app can produce (3 theme modes × 3 interface styles), the on-accent ink
   clears 3:1 against the accent it is drawn on (measured minimum: 3.73:1).

# Gravity Sandbox visual refresh — report (§11)

Date: 2026-09-13
Branch: `arena/01a09b75-lm-arena`
Commits: `a736d9d` (the refresh), `0a50a06` (JVM signature fix caught by CI)
CI: run `34772897419` — **success**, `tests=396 failures=0 errors=0 skipped=0 files=40`

Scope honoured exactly as directed: colours, shadows, the background surface system and one HUD
button. Physics, `EngineConstants`, the engine, gestures, education content, persistence mechanics,
typography, the LTR pin and the marble's three-stop shading **structure** are untouched. No
dependency was added.

---

## 0. Discovery findings

Things the brief assumed which the tree does differently. Each one changed the plan, so each is
recorded with what was done instead.

1. **There is no `SimSave`.** The session encoder is `sim/SaveState.kt`, a pipe-separated header at
   `VERSION = 1` with `darkTheme` in slot 6. Bumped to `VERSION = 2`, slot 6 reused for the surface
   key, v1 still decodes.
2. **The brief's charcoal/paper hex values are not this tree's legacy table colours.** The tree's
   actual pre-refresh surfaces are dark `#3A414B → #2E343D` (vignette `0x3D000000`, sheen
   `0x0AFFFFFF`) and light `#F4F1EA → #E9E4D9` (vignette `0x1A5B5344`, sheen `0x0A000000`). §5
   requires charcoal and paper to be *exactly* the original surfaces, so the tree-actual values won
   over the brief's list; both are pinned by test.
3. **Body colours do not live in a palette file.** They are per-entry `colorArgb` literals in
   `sim/BodyCatalog.kt` plus a `colorOf(key, type)` fallback by `BodyType`. §2 was applied there.
4. **There was no deep-stop channel at all.** The marble's dark stop was *derived* at cache-build
   time (`shadeOf(tone)`). A new optional `deepArgb` was appended **last** on `CatalogEntry`
   (default `0L`, always passed by name) so no positional construction breaks; `0L` means "derive as
   before", which keeps Mars, Venus and every other keyless-of-deep entry on its own tone.
5. **`deepColorOf` had to be entry-wins, not elvis-chained.**
   `byKey(key)?.deepArgb?.takeIf { it != 0L } ?: typeFallback` would hand Mars Earth's blue
   terminator. The shipped version returns the entry's value when the entry exists, and only falls
   back to the type tone for bodies created without a catalog key.
6. **No perf overlay exists, and no HUD control has a long-press.** `clickableTag` in `ui/UiUtil.kt`
   is a plain `clickable`; `lastFrameSubsteps` is a diagnostic read by a JVM test only. §6's
   conditional relocation therefore has nothing to act on and nothing was invented.
7. **`SimSnapshot.visualSignature()` deliberately excludes colours**, so the canvas cache could not
   be invalidated by a surface swap through the epoch alone. The fix was to give `GravityColors` a
   `surface: GravitySurface` **constructor field**: two dark surfaces now produce unequal palettes,
   so the existing `remember(epoch, colors, density.density)` key and the `drawWithCache` capture
   both invalidate. No new cache machinery, no per-frame work.
8. **The brass accent lived in six places**, not one: five `GravityColors` tokens plus
   `BodyCatalog.WORMHOLE.colorArgb`. All six now route through the single per-mode accent token.
9. **The brief's test names do not exist.** The nearest equivalents are
   `saveRestoreRoundTripIdentical` (Integration, the only other `SaveState.decode` user, which never
   asserted the theme flag) and `aRestoredSessionCannotResurrectAStaleSandboxLanguage` (Upgrade).
   The latter was renamed and extended; `GravitySheetScrollTest` is a source-text lint over the sheet
   files, so the new sheet was added to its list.
10. **`LabScreen.kt` calls `GravitySandboxRoot(startInPersian, startInDarkTheme)`.** The signature is
    unchanged; the dark flag now seeds the surface on a fresh install (`dark → midnight`,
    `light → paper`) through `applyHostDefaults`.
11. **The old theme button was `hud_theme`** (`Icons.Filled.DarkMode` / `LightMode`,
    `toggleTheme()`). All three are gone; zero `hud_theme` references remain in the tree.

---

## (a) Files changed — one line each

**New, main**

| File | Line |
| --- | --- |
| `ui/theme/TableSurfaces.kt` (350) | The seven-surface catalog: `ChromeMode`, `SurfaceGradientType`, `SurfaceGradient`, `StarPattern`, `StarDot`, `GravitySurface`, the `TableSurfaces` object with the seeded dot generator + cache and `keyFromLegacyDarkTheme`. |
| `ui/TableSurfaceSheet.kt` (170) | §6 modal bottom sheet: three-column grid of seven swatches that miniature-render their own real gradient and dot print, 2 dp accent border on the current surface, tap applies and closes. |

**Edited, main**

| File | Line |
| --- | --- |
| `ui/theme/GravityTheme.kt` (204) | Brass retired; `GravityChrome` long-const tokens per mode; `GravityColors` gains `surface`/`trailOld`/`shadowSoft`; `colorsFor(surface)`, `SurfaceGradient.brushFor(size)` and `ZigGravityTheme(surfaceKey)` replace `ZigGravityTheme(dark)`. |
| `ui/TabletopCanvas.kt` (714) | Table drawn from the surface (gradient + static print + vignette), single downward shadow replaced by the two-layer bottom-right cast, trails read `trailOld`/`trail`, bodies read the authoritative deep stop. |
| `sim/BodyCatalog.kt` | §2 bright base colours, optional authoritative `deepArgb` appended last, and `deepColorOf()` with entry-wins fallback. |
| `sim/SimulationViewModel.kt` | `darkTheme` → `_tableSurface` + `val tableSurface`, `setTableSurface(key)` replaces `toggleTheme()`, `applyHostDefaults` seeds midnight/paper, serialize/restore carry the key. |
| `sim/SaveState.kt` | `VERSION = 2`; slot 6 is the surface key; v1 files migrate (`0 → paper`, else `midnight`); future versions still refused. |
| `ui/HudBar.kt` | Theme toggle → `Icons.Filled.Palette` «رنگ میز»/"Table colour", tag `hud_btn_table`, `onOpenTableSurface`. |
| `ui/GravitySandboxRoot.kt` | `ZigGravityTheme(surfaceKey = vm.tableSurface)`, `showTableSurface` state, the sheet rendered above the FAB-hiding set and below the tutorial overlay. |

**Tests**

| File | Line |
| --- | --- |
| `GravityVisualRefreshTest.kt` (374, new) | The three §9 tests: catalog completeness + determinism, v1→v2 migration, bright-palette saturation. |
| `GravityUpgradeTest.kt` | `aRestoredSessionCannotResurrectAStaleSandboxLanguage` → `saveRestorePreservesSurfaceAndLanguage`: sets `lavender`, round-trips it, still proves the language cannot be resurrected. |
| `GravitySheetScrollTest.kt` | Added `TableSurfaceSheet.kt` to the linted sheet files (scroll container + bottom inset). |

Nothing else moved: no `physics/` file, no `edu/` file, no gesture code, no other sheet, no
resource, no Gradle file.

---

## (b) Manual checklist

1. **Bodies.** Load Sun and Earth: the Sun reads as a saturated gold (`#FFDC4A` core, `#F5A623`
   limb), Earth as a clear blue (`#3FA1FF`/`#1E63D8`), the Moon near-white, an asteroid warm
   terracotta, a test marble porcelain. Each still has one top-left highlight, one rim and one
   specular dot — the relief is the same shape as before, only the values changed.
2. **Shadows.** Every marble casts to the **bottom-right**, matching the top-left highlight: a tight
   umbra plus a wider, softer penumbra behind it. Select a black hole and a wormhole mouth — neither
   casts anything. Zoom in and out; the shadows scale with the body and never smear or blur the
   canvas.
3. **Brass is gone.** Sweep the selection ring, the HUD "+" disc, active HUD states, the wormhole
   rings, the black-hole ring, the launch-preview arc, the merge pulse and the challenge highlights:
   all of them are the mode accent (teal `#2DD4BF` on dark surfaces, `#0E9F8F` on light ones), never
   gold.
4. **The button.** The HUD shows a palette icon, not a sun/moon. Its content description is
   «رنگ میز» in Persian and "Table colour" in English; its tag is `hud_btn_table`. Tapping it opens
   the sheet; the FAB is hidden while the sheet is up.
5. **The sheet.** Seven swatches in three columns, each showing its own real finish (midnight and
   lavender carry their printed dots), the current one ringed 2 dp in the accent. Tap any swatch: the
   table changes **instantly**, the sheet closes, and the chrome — HUD glass, sheet glass, text,
   accent — follows the new surface's mode. `paper` and `porcelain`/`blush` flip the whole chrome
   light; `charcoal` is exactly the old dark table.
6. **Surfaces in motion.** On each of the seven, run a preset with trails on: the trail pair uses the
   surface's own alphas (0.18/0.26 dark, 0.22/0.30 light) and stays legible against the finish. The
   dot fields are static — no twinkle, no drift, no parallax when you pan or tilt the camera — and the
   centre of the table stays clean.
7. **Persistence.** Pick `lavender`, leave the sandbox, come back: `lavender` is still there (the
   language, as before, is whatever the host app speaks). Install over an existing build that saved a
   v1 session: a session saved in the light theme comes back on `paper`, one saved dark comes back on
   `midnight`, bodies and preset intact.
8. **Nothing else moved.** Play/pause, the speed ladder, reset, trails, teaching, camera and add all
   behave exactly as before; orbits, merges, captures, slingshots and every teaching card are
   unchanged; scrolling a long sheet still works; the canvas still pins LTR in a Persian UI.

---

## (c) Test summary

- CI run `34772897419` on `0a50a06`: **success**. `Full unit test suite :: tests=396 failures=0
  errors=0 skipped=0 files=40`. The signed release APK step ran.
- `com.zig.gravity` unit tests: **182** (`@Test` count) — 179 before, **+3** new, all existing ones
  still green (`GravityUpgradeTest` 56, `GravityCameraFollowTest` 50,
  `GravitySandboxIntegrationTest` 29, `GravityCollisionTest` 20, `GravityPhysicsTest` 19,
  `GravitySheetScrollTest` 5, `GravityVisualRefreshTest` 3).
- New tests: `tableSurfaceCatalogCompleteAndDeterministic`, `surfaceMigrationV1toV2`,
  `brightBodyPaletteSaturated`. Updated in place: `saveRestorePreservesSurfaceAndLanguage`
  (Upgrade) and the sheet-scroll lint list.
- **`grep -rni "D4A853" . --exclude-dir=.git --exclude-dir=build` → 0 matches.** The word "brass"
  survives in exactly two explanatory comments in `GravityTheme.kt` that document its retirement; no
  colour literal, no resource, no test references it.
- Cross-layer tie: `keyFromLegacyDarkTheme(false) == "paper"` / `(true) == "midnight"` is asserted
  against the keys the migrated sim strings produce, so the sim layer's opaque key can never drift
  from the ui catalog.
- One real compile error was caught only by CI and fixed in `0a50a06`: `var tableSurface` with a
  private setter generates `setTableSurface(String)`, which clashes on the JVM with the VM's own
  `fun setTableSurface(key)`. It now uses the `_tableSurface` backing-property shape that `speedIndex`
  and `marbleBounce` already use.

---

## (d) Deviations, with reasons

1. **charcoal/paper hex values differ from the brief.** Used the tree's actual legacy surfaces,
   because §5's "EXACT original" requirement is the stronger instruction and the brief's hexes do not
   exist anywhere in this tree. Both values are pinned by test.
2. **Perf-overlay relocation not done — nothing to relocate.** No overlay and no long-press handler
   exist (§6 made this conditional). Inventing one would have been new UI, outside scope.
3. **Saturation threshold for the asteroid base.** §9 asks for ≥ 0.55, but the owner's own specified
   terracotta `#D89B66` measures 0.528 in HSV. The specified hex is authoritative over the derived
   threshold, so the test asserts ≥ 0.55 for the other five specified values and ≥ 0.52 for the
   terracotta base, with the reason written at the assertion. (The tone it replaces measured 0.143.)
4. **"Black-hole disk luminance ≤ 0.15" is asserted as WCAG relative luminance, not HSV value.** In
   HSV, `#26262B` has V = 0.169 and would fail; in relative (linear-light) luminance — the standard
   UI meaning of the word — the two disks are 0.0031 (dark) and 0.0197 (light), both far under 0.15.
   The exact hexes are asserted alongside, so nothing is hidden behind the metric.
5. **Save format reuses slot 6 rather than appending a 13th field.** The boolean theme flag was fully
   retired, so its slot carries the surface key; the header stays 12 fields and every existing v1
   reader keeps working through the migration. A future version (`9|…`) is still refused outright.
6. **`tableSurface` is a read-only `val` over a private `_tableSurface`.** Forced by the JVM signature
   clash above, and consistent with the file's existing convention. Externally it is still
   `vm.tableSurface` + `vm.setTableSurface(key)`, as specified.
7. **The sim layer stores the surface as an opaque `String`, never as a `GravitySurface`.** Keeps
   `sim` free of any dependency on `ui`; unknown keys resolve to `midnight` in the catalog.
8. **Midnight's dot field is 130 + 10 bright = 140 total**, i.e. at the §5 ceiling rather than under
   it (130 faint dots plus 10 slightly larger anchor dots, one generator, one seeded pass). Lavender
   is 90. Both are asserted, along with the centre-15% exclusion, the radius/alpha bounds and
   run-to-run determinism.
9. **`DarkTabletop` / `LightTabletop` are kept as public vals** although nothing references them any
   more: they are the addressable names of the two pre-refresh themes, now defined as
   `colorsFor(CHARCOAL)` / `colorsFor(PAPER)`, so any caller that wants the old look has a name for
   it. `MidnightTabletop` is the composition-local default.
10. **The explicit dark/light toggle is gone**, as instructed. The host app's dark/light preference
    still seeds a fresh install (midnight / paper) through the unchanged
    `GravitySandboxRoot(startInPersian, startInDarkTheme)` signature, so nothing upstream had to move.

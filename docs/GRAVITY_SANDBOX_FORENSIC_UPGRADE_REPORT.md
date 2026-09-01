# ZIG Gravity Sandbox — Forensic Audit + Implementation Report

Scope: the 28-section forensic audit and implementation prompt.
Branch: `arena/01a059b5-lm-arena`. Baseline: `6e65767`.

**Build:** `./gradlew assembleDebug --no-daemon` → **SUCCESS** (GitHub Actions run `33526818081`).
**Tests:** `testDebugUnitTest` → **tests=123 failures=0 skipped=0 files=4**.

> There is **no JDK, no Android SDK and no emulator or physical device in this workspace**.
> GitHub Actions is the only toolchain. Everything marked 📱 below is **NOT VERIFIED** — it was
> reasoned about and implemented, but no one has looked at it on a screen or felt it in a hand.

---

## A. Files changed

| File | What changed |
|---|---|
| `physics/EngineConstants.kt` | `SPEEDS` / `SPEED_LABELS` → `1, 10, 69, 100`. |
| `physics/SimEvent.kt` | `CollisionImpact` gains `reducedMass` and `impactEnergyJ`. |
| `physics/Collision.kt` | `emitImpact` computes `μ = m₁m₂/(m₁+m₂)` and `E = ½μv_rel²`. |
| `sim/SimulationViewModel.kt` | `applyHostLanguage`; language dropped from restore; `dragTo` recomputes the prediction; ghost/escape state; `previewIsUnbound`; `describeImpact`; drag outranks selection for the prediction target. |
| `ui/HudBar.kt` | Language button and small Add button removed, with their parameters and import. |
| `ui/GravitySandboxRoot.kt` | Host-locale `LaunchedEffect`; 64 dp labelled Add pill (`hud_add`); HUD call site updated. |
| `ui/InspectorSheet.kt` | `verticalScroll` + `imePadding` + `navigationBarsPadding` + 48 dp tail spacer (`inspector_scroll`). |
| `ui/TabletopCanvas.kt` | Ghost path style + origin marker; three-grade collision visuals; tidal-streak accretion particles; `strokeGhost`. |
| `ui/TeachingCard.kt` | Impact headline/detail surfaced from real event data (`impact_explanation`). |
| `ui/theme/GravityTheme.kt` | Dark tabletop lifted off space-black to a neutral slate; vignette softened. |
| `util/PersianDigits.kt` | `SandboxFormat.joules`. |
| `test/GravityUpgradeTest.kt` | +15 tests (40 → 55). |
| `test/GravitySandboxIntegrationTest.kt` | Speed-ladder and locked-constant assertions updated. |

---

## B. Features, one by one

**§4 Speed ladder — DONE, test-verified.**
`SPEEDS = [1.0, 10.0, 69.0, 100.0]`, `SPEED_LABELS = ["1x","10x","69x","100x"]`.
Forensic finding: **`16×` never existed in this sandbox.** The ladder was already `[1,10,100]`; the
only `16` literals under `com/zig/gravity` were dp size bands. The work was to *add* 69×, not to
remove 16×. The HUD renders `SPEEDS.indices`, so the fourth rung appeared with no layout change.
The multiplier reaches the wall clock through `accumulator += dtReal * BASE * speed` drained in
fixed `DT = 3600 s` substeps — `DT` is never scaled. Verified by
`eachSpeedAdvancesSimulatedTimeProportionallyWithoutScalingDt` (each rung within ±20 % of its own
label over identical wall-clock intervals, strictly monotonic) and `noSpeedRungAnywhereIsSixteen`.

**§5 Language selector removed — DONE, test-verified.**
Two real bugs, not one. The button existed *and* `applyHostDefaults` ran only in the `!restored`
branch, so a restored session kept whatever language it was saved with, forever. The button, its
parameter and its import are gone; `applyHostLanguage(persianFromApp)` is now the only writer; and
`restore()` **deliberately does not read `session.persian`**. Verified by
`theSandboxHasNoLanguageOfItsOwn` and `aRestoredSessionCannotResurrectAStaleSandboxLanguage`.

**§6 Bigger Add button — DONE (📱 for feel).**
Was a 38 dp icon button with a 19 dp glyph. Now a 64 dp-tall labelled pill at `BottomEnd` with a
30 dp glyph and a FA/EN word ("افزودن" / "Add"), `navigationBarsPadding()` plus 84 dp bottom inset
so it clears both the system nav and the HUD row. It uses `clickableTag`, so it does not compete
with the tabletop's long-press-to-add gesture. 📱 Touch comfort is not verified.

**§9/§15 Impact energy — DONE, test-verified.**
`CollisionImpact` now carries the reduced mass and the centre-of-mass impact energy. Using total
kinetic energy would have been wrong: most of it is the pair's shared drift, which no collision can
feel. The teaching card shows the tier headline, the relative speed in km/s, the energy in joules
and one plain sentence, in FA and EN, with the existing WHAT/WHY/MORE tiers as the progressive
disclosure. Verified by five tests including symmetry under body order, the v² scaling law, and
`aGentleTouchIsNeverClassifiedAsHighEnergy`.

**§13/§14 Ghost trajectory — DONE, test-verified (📱 for looks).**
`dragTo` now recomputes the prediction on every move *including while paused*, and the prediction
target prefers the dragged body over the selection. The path is drawn with a distinct dotted
1.2 dp stroke at 55 % alpha (trails are solid; the committed prediction is a wider dash), plus a
ghost outline at the pick-up point and a faint tether to the finger. Escape is decided exactly, by
specific orbital energy `v_rel²/2 − GM/r` against the dominant attractor, rather than by eyeballing
the end of the sampled path — a hyperbolic body starting far out barely moves inside the prediction
window and would otherwise be misread as bound. Release commits position only; velocity, mass and
radius are asserted bit-for-bit unchanged.

**§12 Black-hole destruction — PARTIAL.**
Accretion particles are now drawn as streaks elongated toward the hole, lengthening as they fall
(tidal stretch), tinted toward red and dimming (redshift-inspired). The capture itself was already
synchronised with the physical removal. **Not implemented:** the body does not visibly deform
*before* fragmenting — it is removed and replaced by the particle effect in the same frame.

**§10 Particle system — ALREADY DONE (verified by inspection + existing tests).**
Pre-existing pool: 10 effects × 22 particles, fixed arrays, no per-frame allocation, deterministic
age-based cleanup, drained inside `withFrameNanos`, no coupling to physics.

**§11 Haptics — ALREADY DONE.** One cue per `CollisionImpact`, intensity by tier, queue-and-clear.

**§16 Edit-sheet scrolling — ROOT CAUSE FOUND AND FIXED (📱 for the real thing).**
`InspectorSheet`'s body was a plain `Column` with **no scroll modifier at all** — content past the
fold was laid out and clipped, so the bottom controls were unreachable rather than merely awkward.
`verticalScroll` appeared nowhere in `ui/*.kt`. Fixed with
`.weight(1f, fill = false).verticalScroll(...).imePadding().navigationBarsPadding()` and a 48 dp
tail spacer. 📱 Small-screen and keyboard-open behaviour is not verified.

**§21 Tabletop, not space — DONE (📱 for the panel).**
Dark theme was `#1C1F26`/`#16181D` (~12 % lightness) — on OLED that is a black sky. Now
`#3A414B`/`#2E343D` (~24 %), a neutral slate felt, with the vignette softened from `0x66` to `0x3D`
so corners no longer fall to black, and the trail alpha raised to hold contrast. The light theme was
already correct warm fabric and is untouched.

**§9 Collision visuals by severity — DONE (📱).**
Three genuinely different behaviours, chosen by the physics tier: BOUNCE draws a *compression* ring
that squeezes inward; MERGE a brighter flash and one expanding ring with debris; SHATTER a hard
short flash, radial fragments, a fast shockwave ring and a slower dust front. Even the top grade is
sub-second and restrained.

**§17–§19, §22 Regressions — inventory checked, unchanged.**
Sliders (`SandboxSlider` + `frameTick` subscription), camera, presets, wormholes, barycentre,
challenges, save/restore and the Solar System preset were not touched by this change and remain
green in the same 123-test run. Earth–Moon is still physics-derived and covered by the existing
separation / relative-velocity / mutual-acceleration / barycentre / stability assertions.

---

## C. Bugs discovered

1. **Stale sandbox locale (not in the brief).** `applyHostDefaults` ran only when `!restored`, so a
   restored session ignored the app's language permanently.
2. **The inspector had no scroll modifier at all** — the reported "scrolling bug" was missing
   scrolling, not broken scrolling.
3. **`16×` did not exist.** Reported as present; it was not. Documented rather than "fixed".
4. **`restore()` resurrected the saved language** — a second, independent path to the same symptom
   as (1).
5. **The prediction target ignored the dragged body** unless it also happened to be selected.
6. **A wrong test assertion** (`GravitySandboxIntegrationTest`): "each rung must be 1.5× the
   previous". `100/69 = 1.45`, so a *correct* four-rung engine fails it. Replaced with strict
   monotonicity plus a per-rung ratio check against each rung's own label — a stronger assertion,
   not a weaker one. Nothing was deleted or loosened to go green.
7. **Duplicated `private set`** introduced mid-implementation; caught by CI, fixed.

---

## D. Physics verification (JVM tests, real simulation state)

- **Speed:** all four rungs advance strictly more simulated time than the one below over identical
  wall-clock intervals, each within ±20 % of its label; simulated time remains a whole number of
  `DT = 3600 s` steps at every rung; 100× produces no NaN or teleport on the full Solar System.
- **Collision energy:** `μ` exact to 1e-9 relative; `E = ½μv_rel²` exact to 1e-9 relative;
  symmetric under body order; tripling `v_rel` multiplies `E` by exactly 9; zero when a body is
  massless; a 2 m/s contact between two Earth masses classifies LOW.
- **Ghost:** velocity, mass and radius bit-identical across a 12-step drag; only position commits;
  the path refreshes while paused; unbound previews flagged, bound ones not.
- **Earth–Moon and black hole:** unchanged from the previously verified suite, re-run green here.

## E. UI verification

Compilation-verified only: composition, tags, parameters, imports, theme values.
`hud_add`, `inspector_scroll`, `impact_explanation` exist; `hud_language` no longer does; the speed
row renders `SPEEDS.indices` so it shows exactly four rungs.
📱 **Not verified:** actual scrolling, slider tracking, touch-target comfort, RTL layout on a real
screen, haptic strength, animation quality, frame rate, and whether the new slate reads as fabric.

## F. Test counts

`tests=123 failures=0 skipped=0 files=4` (was 108). +15 new tests, 0 removed, 0 weakened.

## G. Build

`./gradlew assembleDebug --no-daemon` (which depends on `testDebugUnitTest` under CI) →
**BUILD SUCCESSFUL**, run `33526818081`. Two intermediate red runs were fixed, not silenced:
`33525553431` (Kotlin syntax error) and `33526094931` (the stale ladder assertion).

## H. Genuine remaining limitations

1. **No device, no emulator, no screenshots.** Every 📱 item above is unverified by observation.
2. **§12 is partial** — no pre-fragmentation deformation of the body itself.
3. **§9 high-energy collisions still merge.** Fragmentation into real bodies is not implemented;
   "fragments" are particles only.
4. **§14 "predicted encounters"** are not annotated on the ghost path; only escape is.
5. **The Moon looks attached at default Solar-System zoom.** This is a display-scale consequence,
   not a physics defect — its orbit is far sub-pixel at that framing. Physics is test-verified.
6. **No dependency was added.** KPhysics, ParticleEmitter and Compose-Symphony were considered and
   rejected: the pooled effect system is ~264 lines and importing an engine for it would violate
   the "no large dependency for a small effect" rule. Nothing to record for license or maintenance.

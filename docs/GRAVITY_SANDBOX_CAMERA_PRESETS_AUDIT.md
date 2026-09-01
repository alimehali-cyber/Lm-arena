# Gravity Sandbox — Camera, Follow, Add Button, Default Scene & Teaching Presets

**Forensic audit + implementation report**
Branch `arena/01a059b5-lm-arena` · base `b64bf02` · verified run `33537286680`

This report covers the 34-section camera/presets spec. The first-launch tutorial is reported
separately in `GRAVITY_SANDBOX_TUTORIAL_REPORT.md`; both were implemented and verified in the same
build, so the test and build figures at the bottom are shared.

---

## A. Files changed

| File | Δ | What changed |
|---|---|---|
| `sim/CameraState.kt` | +42 | `CameraPose` value type, `snapshot()`, `restore()`, `tiltFraction()`/`tiltFromFraction()` |
| `sim/SimulationViewModel.kt` | +208/−… | `initialCameraPose`, `cameraTick`, follow subsystem, camera-aware `reset()`, preset teaching hookup |
| `sim/Presets.kt` | +156/−… | 6 new presets, `initialTiltFraction` per preset, `DEFAULT = SUN_EARTH`, stale `16x` copy removed |
| `sim/TutorialStore.kt` | +50 (new) | tutorial persistence port + gate (tutorial spec) |
| `edu/TeachingCatalog.kt` | +111/−… | `tryThisFa`/`tryThisEn` on `TeachingCard`, 6 preset intro cards, `presetConcept()` |
| `edu/TutorialContent.kt` | +139 (new) | tutorial copy (tutorial spec) |
| `ui/GravitySandboxRoot.kt` | +204/−… | camera panel subscribes to `cameraTick`, elevation as a 0–1 fraction, `camera_reset` → preset pose, circular Add button, Following chip, `?` button, tutorial host |
| `ui/InspectorSheet.kt` | +56 | Follow / Unfollow row |
| `ui/TabletopCanvas.kt` | +25 | follow reticle (two accent arcs) |
| `ui/TeachingCard.kt` | +16 | renders "Try this" on the first tier |
| `ui/TutorialOverlay.kt` | +293 (new) | tutorial UI (tutorial spec) |
| `test/GravityCameraFollowTest.kt` | +841 (new) | **50 tests** for everything above |
| `test/GravityUpgradeTest.kt` | +26/−8 | default-scene assertion retargeted, full-Solar-System coverage kept |

**13 files, +2115 / −52.**

---

## B. Features, one by one

Status vocabulary: **DONE** = verified end-to-end by a test that exercises real state.
**DONE (code)** = wired end-to-end in code but only observable on a device.

### B1 · Reset — `DONE`

`reset()` previously restored `initialState` and recomputed accelerations and *never touched the
camera*, so a user who had panned and zoomed pressed Reset and got the physics back but kept a
viewport pointing at empty space. It now also calls `camera.restore(initialCameraPose)`, bumps
`cameraTick` so the panel resyncs, and stops Follow.

`initialCameraPose` is captured per preset at load time, so Reset returns to *that scene's* framing,
not one global default — covered by `resetRestoresEachPresetsOwnCameraNotOneUniversalDefault` and
`everyPresetHasAnInitialCameraState` (runs over all 15 presets).

### B2 · Camera elevation — `DONE`

**Root cause of "the angle control does nothing":** the control worked; the *display* did not. The
panel read `vm.camera.tiltRad` directly. `CameraState` is a plain Kotlin class, not Compose state, so
reading it never subscribed the panel to changes. The slider moved, the projection changed, and then
on finger-up `SandboxSlider` re-synced its thumb to the stale `value` it had been handed — snapping
back and looking inert. `SandboxSlider` itself was never at fault.

Fix: `cameraTick` is a `mutableStateOf(0)` bumped by every camera mutation; the panel reads it, so
every camera change recomposes the panel. The control is exposed as a 0–1 fraction
(`cameraTiltFraction` / `setCameraTiltFraction`) rather than raw radians, so the UI cannot produce an
out-of-range angle. `MAX_TILT = 1.08 rad` (≈62°); the view can never pass vertical or invert.

Verified: `theElevationControlActuallyChangesTheCamera`,
`elevationChangesTheProjectionAndOnlyAlongTheVerticalAxis` (asserts the projection actually moves and
that only the vertical axis compresses), `elevationNeverLeavesItsLegalRangeAndNeverFlipsOver`,
`elevationDoesNotTouchPhysics`, `everyCameraMutationIsVisibleToTheUi`.

### B3 · Follow a body — `DONE` (behaviour) / `DONE (code)` (feel)

- **Start**: Inspector row `inspector_follow`, hidden for wormhole mouths (a mouth is a portal, not
  an object worth riding).
- **Smoothing**: exponential, in **wall-clock** seconds, not simulated seconds — `τ = 0.28 s` while
  acquiring, `0.12 s` once locked. This is why Follow feels identical at 1× and 100×;
  `followSmoothingIsWallClockNotSimulationTime` asserts the residual gap after 20 frames is
  **bit-identical** across all four rungs.
- **Never teleports**: `followIsSmoothAndNeverTeleports`.
- **Physics untouched**: `followDoesNotModifyPhysics` runs a followed and an unfollowed simulation
  for 120 frames and asserts every position, velocity, mass and radius matches to `0.0` tolerance.
- **Body disappears**: merge transfers Follow to the survivor (it genuinely contains the mass being
  watched); black-hole capture stops Follow; deletion stops Follow before the slot is freed.
  Covered by `deletingTheFollowedBodyEndsFollowSafely` and
  `aFollowedBodyThatDisappearsMidRunNeverLeavesTheCameraChasingNothing`.
- **User override**: any manual pan/zoom/rotate calls `onCameraMoved()`, which drops Follow — the
  user's own gesture always wins (`aManualCameraGestureTakesOverFromFollow`).
- **Indicator**: a "Following X" chip above the teaching card (tag `follow_indicator`); tapping it
  stops Follow. On the canvas the followed body gets two 46° accent arcs — a reticle, deliberately
  distinct from the solid selection ring so "selected" and "followed" are never confused.

### B4 · The Add button — `DONE (code)`

Was a small pill competing with everything else at the bottom of the screen. Now a **56 dp touch
target around a 52 dp visible circle**, bottom-end, above the system navigation bar
(`navigationBarsPadding()`), and it **hides whenever any sheet or panel is open** — a floating button
sitting on top of the sheet it just opened is the classic version of that bug.

📱 **REQUIRES PHYSICAL DEVICE VERIFICATION** — that the target actually feels comfortable, that it
clears the gesture-nav pill on a specific handset, and that it does not collide with tap-and-hold.

### B5 · Default scene — `DONE`

`Preset.DEFAULT` is now `SUN_EARTH`: two bodies, one obvious relationship. The full Solar System is
one tap away and still has all ten bodies. `theDefaultSceneIsARealGravitationalOrbitNotAnAnimation`
proves the opening scene is genuine physics — mutual opposing accelerations, and a bounded orbit over
2400 integration frames (`maxR/minR < 1.5`).

### B6 · Six teaching presets — `DONE`

| Preset | Question it answers | Physics |
|---|---|---|
| `TWO_BODY_ORBIT` | Why does it keep orbiting? | r = 1.234e11 m, v = 32.8 km/s, period ≈ 24 s at 1× |
| `ESCAPE_VELOCITY` | When is gravity not enough? | three marbles at 0.75 / 1.0 / 1.25 × v_esc (49.9 km/s) |
| `MASS_MATTERS` | What does mass change? | two identical test bodies at ±dp(100), mirror-symmetric |
| `COLLISION_LAB` | What happens when two bodies meet? | 5.0e26 / 3.0e26 kg, v_rel/v_esc = 1.36 → MODERATE |
| `PERTURBATION` | Is any orbit really alone? | Sun + Earth at 1 AU + a Jupiter-mass body at 1.9 AU |
| `BLACK_HOLE_ENCOUNTER` | Does a black hole suck things in? | flyby at 0.55 × escape speed — it swings past and returns |

Each has a bilingual teaching card following the WHAT → WHY → MORE tiers plus a new **"Try this"**
line that names a concrete next action. Loading one of these scenes opens its card automatically;
scenes without a card stay silent rather than showing a stub.

`everyNewPresetBuildsValidPhysics` checks all six for finite state, non-negative mass, positive
radius, **no bodies starting overlapped**, and 120 frames of stable integration.
`escapeVelocityPresetActuallySeparatesBoundFromUnbound` computes the specific orbital energy of each
body and asserts at least one is bound and at least one is not — i.e. the scene actually demonstrates
its lesson. `collisionLabCarriesRealMassSoMomentumIsMeaningful` guards the bug found during the audit
(see C2).

---

## C. Bugs discovered

1. **Reset ignored the camera** (§1) — reported. Fixed.
2. **`COLLISION_LAB` was built from `TEST_MARBLE`, which is massless.** A momentum lab in which both
   objects have zero mass demonstrates nothing: the collision energy `E = ½μv²` is identically zero.
   Found while hand-checking the new presets, before it ever ran. Rebuilt with 5.0e26 / 3.0e26 kg
   planets and locked down by a test.
3. **`BLACK_HOLE_ENCOUNTER` was unbound at first.** The initial velocity exceeded escape speed, so
   the test object left the table and the scene taught the opposite of its lesson. Softened to
   0.55 × escape speed so it visibly swings past and comes back.
4. **A preset note still told the user to press "16x"** — a rung deleted in the previous pass. Not
   caught earlier because the 16× sweep covered labels, indices, docs, tests and a11y strings but not
   preset prose. Fixed, and `noObsoleteSpeedReferenceSurvivesAnywhereInPresetCopy` plus
   `everySpeedMentionedInUserFacingPresetCopyIsARealRung` now check every user-facing preset string
   against `EngineConstants.SPEED_LABELS`, so this cannot recur for any rung.
5. **`frameCameraForPreset` hard-forced `setTilt(0.0)`.** Every preset was flattened on load
   regardless of what it wanted, which is part of why the elevation control appeared to do nothing on
   a fresh scene. Presets now declare `initialTiltFraction`.
6. **Unrequested — a missing comma in a Kotlin list literal.** Not a product bug, but worth
   recording: the preset teaching cards were appended after a list element that had no trailing
   comma. It cost a full CI cycle. `/tmp/kcheck.py` gained a pass that detects an element closing
   without a separator, verified against a synthetic reproduction.

---

## D. Physics verification

All by JVM unit test against the real engine.

- **Nothing in this change touches the integrator.** The strongest evidence is
  `followDoesNotModifyPhysics`: two identical simulations, one with Follow active, run 120 frames and
  compared field-by-field at exactly `0.0` tolerance. `elevationDoesNotTouchPhysics` does the same for
  the camera angle across five settings.
- **Earth–Moon remains genuine two-body physics** (`earthMoonRemainsAGenuineTwoBodySystem`): the Moon
  is not at Earth's centre and not inside Earth's radius; it has real relative orbital velocity;
  Earth itself moves; accelerations are mutual and opposing; and over 1500 frames the separation
  stays bounded (`maxR/minR < 2.0`) with neither body lost. No parenting, no scripted orbit.
- **Determinism**: `presetsAreDeterministic` rebuilds all 15 presets twice and compares every
  position, velocity and mass at `0.0` tolerance.
- **Speed ladder** unchanged and still 1 / 10 / 69 / 100, driven by substeps; the follow test above
  additionally proves camera smoothing does not inherit the multiplier.

---

## E. UI verification

| Item | Status |
|---|---|
| Elevation control changes the model | **DONE** — asserted on `camera.tiltRad` and on the projection |
| Elevation control's *displayed value* tracks the camera | **DONE** — `cameraTick` subscription asserted |
| Reset returns the camera | **DONE** — asserted over all 15 presets |
| Follow starts / stops / switches / survives deletion | **DONE** — 12 tests |
| Add button size, placement, sheet gating | `DONE (code)` — 📱 **REQUIRES PHYSICAL DEVICE VERIFICATION** |
| Follow reticle and "Following X" chip render correctly | 📱 **NOT VERIFIED** — drawing code, no pixel test |
| Preset sheet lists the 6 new scenes | **DONE by construction** — it enumerates `Preset.entries` |
| FA/EN on every new string | **DONE** — asserted non-blank, distinct, and containing Persian characters |
| RTL mirroring of the new controls | 📱 **NOT VERIFIED** — uses `Row`/`Arrangement`, which Compose mirrors, but unobserved |

---

## F. Tests

- **New file `GravityCameraFollowTest.kt`: 50 tests.**
- **Suite total: 174 tests, 0 failures, 0 skipped, 5 files** (from 123 before this task).
- Two pre-existing tests were **retargeted, not weakened**:
  - `theDefaultSceneIsTheFullSolarSystem` → `theDefaultSceneIsTheSimpleSunAndEarthSystem`. The
    default scene changed by request; the replacement is *stronger* (it also pins the body count),
    and `theFullSolarSystemIsStillOnePresetAway` was added so the old coverage is not lost.
  - `followIsSmoothAndNeverTeleports` was mine and was wrong on its first run: it measured the very
    first `onFrame`, which by design has `dtReal = 0` because there is no previous frame. The test
    now primes the clock. The production code was correct; the test was not.

Two CI cycles failed before this one, both genuinely:
`33535157832` Kotlin syntax (C6), `33535842472` the two assertion failures above.

---

## G. Build

```
./gradlew assembleDebug --no-daemon
```

Run **`33537286680`**, job `99954590614`: **SUCCESS**, `tests=174 failures=0
skipped=0 files=5`. `assembleDebug` depends on `testDebugUnitTest` under CI, so a green APK implies a
green suite.

There is **no local toolchain** in this environment — no JDK and no Android SDK — so GitHub Actions is
the only compiler and the only test runner available. Nothing here was verified by running the app.

---

## H. Genuine remaining limitations

1. **No device, no emulator, no screenshots.** Everything under "feel" — whether Follow reads as
   smooth, whether the reticle is legible against the tabletop, whether the Add button clears a
   specific handset's gesture bar, whether RTL mirrors correctly — is unverified by observation.
2. **Follow does not auto-zoom.** It tracks position only. A body on a wide orbit stays centred but
   may be small; the user must zoom manually. Deliberate: automatic zoom fights the user's own
   gesture and was judged worse than leaving zoom alone.
3. **Follow transfers on merge.** Defensible, but it is a judgement call: an alternative reading is
   that Follow should end because the thing being watched no longer exists.
4. **The follow reticle is not tested.** It is drawing code inside a `Canvas` lambda; there is no
   pixel or screenshot test in this project.
5. **Preset teaching cards fire only on load.** Re-reading one means reopening the preset or using
   the teaching card history; there is no "explain this scene again" affordance.
6. **The new presets' pedagogy is unvalidated with actual learners.** The physics is checked; whether
   each scene teaches what it intends is a claim no unit test can make.
7. **`PERTURBATION` needs patience.** The drift is real and physical, but at 1× it takes several laps
   to become visible — the card says so, but a user at 1× may conclude nothing is happening.

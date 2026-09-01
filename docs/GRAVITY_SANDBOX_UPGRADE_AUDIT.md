# ZIG Gravity Sandbox — Upgrade Implementation Audit

**Scope:** the 29-point interaction / manipulation / Solar-System / collision brief.
**Branch:** `arena/01a059b5-lm-arena`
**Verified build:** GitHub Actions run `33500039671` — `assembleDebug` **succeeded**, `testDebugUnitTest`
reported **`tests=108 failures=0 skipped=0 files=4`**.

## How to read this document

A requirement is marked **IMPLEMENTED** only when the behaviour is wired end to end — UI → ViewModel →
simulation → renderer — *and* something automated proves it. "Code exists" is not implemented.

| Mark | Meaning |
|---|---|
| ✅ IMPLEMENTED | Wired end to end and covered by a passing JVM test or a direct code path with no gap. |
| 🟡 PARTIAL | The core is real and working, but a named part of the requirement is missing or weaker than asked. |
| ❌ NOT IMPLEMENTED | Not done. |
| 📱 DEVICE | The logic is in place, but the acceptance criterion is perceptual (look, feel, frame rate, vibration) and **REQUIRES PHYSICAL DEVICE VERIFICATION**. |

**What this environment cannot do.** There is no Android device and no emulator in this sandbox, and no
local JDK or Android SDK — the only toolchain is GitHub Actions, which compiles the app and runs JVM unit
tests. **No APK has been installed or run. Nothing about the rendered image, the animation, the haptic
feel or the frame rate has been observed.** Every claim below that depends on seeing or feeling the app is
marked 📱 and is explicitly *not* claimed as verified.

---

## Summary

| | Count |
|---|---|
| ✅ Implemented | 17 |
| 🟡 Partial | 11 |
| ❌ Not implemented | 0 |
| 📱 Requires device verification (overlaps the above) | 8 |

Test suite grew from **68 → 108** tests. The new `GravityUpgradeTest` contributes **40**.

---

## 0. Audit first — find and fix root causes

**✅ IMPLEMENTED.** Every named symptom was traced to a specific line before anything was written.

| Symptom | Root cause found | Fix |
|---|---|---|
| Dragging changed velocity | `endDrag()` reconstructed finger velocity from a `dragHistoryX/Y/T[6]` ring buffer and assigned it to the body | Ring buffer deleted; `beginDrag` captures `dragHeldVx/Vy/Mass`, `endDrag` restores them verbatim |
| Camera incomplete | The scene→screen mapping was **hardcoded twice** (`GravitySandboxRoot.pxPerMeter` and a local `k`/`sx()`/`sy()` in `TabletopCanvas.drawScene`), with no pan, zoom or rotation anywhere | Both copies deleted; `CameraState` is now the only mapping |
| Sliders dead | `InspectorSheet` derived slider values from `vm.snapshot`, which is a bag of plain `DoubleArray`s — **not Compose state** — so a controlled `Slider` never recomposed after `onValueChange` | Sheet subscribes to `vm.frameTick`; new `SandboxSlider` holds its own drag state |
| No `+` creation | The button and sheet existed but the gesture layer competed with them | Single gesture handler; `+` and long-press both open the same sheet |
| No 100× | Ladder was `[0.1, 0.25, 1, 4, 16]` with `MAX_SUBSTEPS = 96` | Ladder `[1, 10, 100]`, `MAX_SUBSTEPS = 1024` |
| No Solar System | Preset did not exist | `FULL_SOLAR_SYSTEM`, now the default |
| Weak collisions | Merge/bounce with no energy classification and no feedback | `CollisionImpact` + `ImpactTier`, pooled effects, haptic cues |

A **latent bug the audit did not predict** surfaced during testing and is worth recording: the locked
§3.6a dp band system makes the collision radius equal to the drawn radius *in scene metres*. At true
Solar-System scale Earth's 10 dp is `1.1e10 m` — thirty times the Moon's entire orbit — so the Moon was
ejected on the first step. Fixed by giving `SimArrays` an optional real SI `physicalRadius` override that
the Solar System uses. This is the same decoupling requirement 11 demands, so it is a fix, not a patch.

---

## 1. Camera and gestures

**🟡 PARTIAL** (one deliberate deviation) · 📱 for feel.

Implemented in `sim/CameraState.kt` (250 lines) and one `awaitEachGesture` handler in
`GravitySandboxRoot`:

* One finger: **tap = select**, **long press = manipulate** (action menu on a body, creation sheet on
  empty table), **drag a body = move it**.
* Two fingers: **pan** (centroid travel), **pinch zoom** (apart = in), **twist = table orientation**.
* The centroid is the zoom/rotation anchor, so the scene point under the fingers stays under the fingers.
* **Transitions are explicit and velocity-safe.** On 1→2 fingers the object gesture is *ended* first
  (`endDrag()`, which restores velocity verbatim), then the camera takes over, and the first event after
  the transition is skipped so stale deltas cannot jump the view. Camera mode is held until every finger
  lifts, so 2→1 cannot silently become a drag.
* Tests: `cameraRoundTripsSceneAndScreenCoordinates`, `pinchZoomsInWhenFingersMoveApartAndKeepsTheAnchorPinned`,
  `twoFingerPanMovesTheCameraNotTheBodies`, `cameraAngleSquashesThePlaneButNeverTheBodyRadii`,
  `cameraTiltAndZoomStayInsideTheirSafeRange`, `cameraTransitionsNeverTouchBodyVelocity`.

**Deviation, stated plainly.** The brief asks for two-finger *directional movement* to set camera
angle/elevation. Elevation is instead an explicit **"Viewing angle" slider in the camera panel**
(`hud_camera` → `camera_tilt_slider`). A two-finger vertical drag is indistinguishable from a two-finger
pan; wiring both to the same gesture would make panning tilt the table by accident. The elevation itself
is implemented (orthographic squash by `cos(tilt)`, radii deliberately unaffected, since a sphere projects
to a circle from any angle).

📱 Smoothing and damping *feel* is not verified. The implementation is per-event and unfiltered: it is
smooth because the anchor maths is exact, not because a filter was tuned against a real finger.

## 2. Dragging — position only

**✅ IMPLEMENTED.** The most important fix in the brief.

`beginDrag` stores `dragHeldVx/dragHeldVy/dragHeldMass` and `dragOriginX/Y`; `dragTo` writes **only**
`x` and `y`; `endDrag` writes the stored velocity and mass back *bit-for-bit* and clears the trail (the
old trail describes a path the body never took). `cancelDrag` also restores the position. There is no
inferred throw velocity, no release impulse and no drag-distance-to-momentum anywhere in the file.

Tests assert equality with **delta `0.0`**, not "close enough": `draggingChangesPositionOnly`,
`aFastFlickImpartsNoThrowVelocity` (30-sample violent flick), `cancelledDragRestoresPositionAndVelocity`,
`aHeldBodyStillPullsOnEverythingElse`, and the rewritten
`dragMakesBodyKinematicAndReleasePreservesItsVelocity`.

The explicit slingshot (`armSlingshot` → `updateSlingshot` → `releaseSlingshot`) remains the *only* way a
gesture can set velocity, which is requirement 21's intent.

## 3. Inspector

**🟡 PARTIAL.**

The sheet now has five clearly separated, labelled sections: **PHYSICAL MASS**, **PHYSICAL RADIUS**,
**VISUAL SIZE**, **POSITION** (X and Y), **VELOCITY** (magnitude + direction dial), plus **TYPE AND
COLLISION BEHAVIOUR**. Visual size carries an explicit note that it does not change the collision radius,
and physical radius shows both the real body radius and the scene collision radius. Ranges are safe:
mass uses `BodyType.massRange`, size the type's dp band, position the framed view, speed the computed
`velocityGuidance`.

**Missing:** type and collision behaviour are **read-only descriptions**, not editors. You cannot turn a
planet into an asteroid from the inspector, and the only editable collision setting is the existing
marble-bounce toggle. That is the gap between 🟡 and ✅.

## 4. Sliders — fixed completely

**✅ IMPLEMENTED** for the mechanism · 📱 for the human trial.

`ui/SandboxSlider.kt` (190 lines) replaces every Material `Slider` in the sandbox:

* **Whole track interactive with tap-to-seek** — `awaitFirstDown` immediately commits the value at the
  press position and drags on from there, so there is no dead zone and no finger/thumb offset.
* **No stuck thumb** — while dragging the thumb follows a local `Float`; when idle it re-syncs to the
  incoming value. This is the actual fix for the stale-`DoubleArray` root cause.
* **Live numeric readout** next to the label, Persian digits via `SandboxFormat`.
* **Parents cannot steal it** — the down event and every drag change are consumed inside the control.
  There is no invisible overlay above any slider; the inspector is a `ModalBottomSheet` and the canvas
  gesture layer sits *below* it in the layout, not above.
* **RTL** — the fraction is mirrored when `LocalLayoutDirection` is RTL, so the Persian track runs right
  to left; the draw path mirrors with it.
* **48 dp touch target** over a 4 dp drawn track.

📱 The brief's specific manual matrix — min/max/mid, fast and slow drags, RTL Persian, after opening and
closing the inspector, after changing the selected body — **has not been performed**; there is no
instrumented UI test harness in this project and no device here. The stale-state bug that made all of
this fail is fixed at the root, but the trial itself is outstanding.

## 5. Speed ladder 1× / 10× / 100×

**✅ IMPLEMENTED.**

`SPEEDS = [1.0, 10.0, 100.0]`, `SPEED_LABELS = ["1x","10x","100x"]`, `DEFAULT_SPEED_INDEX = 0`.
**`DT` is untouched at 3600 s.** Speed multiplies the simulated-time accumulator only; the frame loop
drains `while (accumulator >= DT && substepBudget > 0)`. `MAX_SUBSTEPS` was raised 96 → **1024** because
100× at 30 fps needs ≈926 substeps per frame; the charter test now asserts that headroom explicitly.
No 1000× rung was added.

Tests: `speedLadderIsExactlyOneTenAndOneHundred`,
`eachSpeedAdvancesSimulatedTimeProportionallyWithoutScalingDt`,
`hundredTimesSpeedProducesNoNaNOrTeleport` (no NaN, every speed under `V_MAX`),
`speedButtonsChangeSimulatedTimeNotTheTimestep` (simulated time is always a whole number of DT steps),
`accumulatorNeverExplodes` (every rung, bounded debt).

## 6. `+` spawner

**✅ IMPLEMENTED.** `hud_add` is a visible, emphasised HUD button opening `AddBodySheet`, which lists all
14 catalog entries — Sun, Mercury, Venus, Earth, Moon, Mars, Jupiter, Saturn, Uranus, Neptune, black hole,
wormhole, test marble, asteroid. `addFromCatalog` handles the body cap, wormhole pairing, free-spot
placement and a circular-orbit velocity about the dominant attractor, and selects the new body.
Tests: `everyCatalogBodyCanBeSpawnedIntoAnyScene` (all entries, catalog-authoritative mass),
`aSpawnedBodyNeverLandsInsideAnother`, `aSpawnedBodyIsImmediatelySelectableAndEditable`.

## 7. Tap-and-hold creation coexists with `+`

**✅ IMPLEMENTED.** There is now exactly **one** pointer handler on the canvas. A long press on empty
table opens the same `AddBodySheet` with the touch point as the spawn position; the `+` button opens it
with no position. Two competing detectors (`detectTapGestures` + `detectDragGestures`) were deleted, so
there is no gesture to compete over. 📱 Not felt on a device.

## 8. Full Solar System preset

**✅ IMPLEMENTED.** Sun + 8 planets + the Moon, 10 bodies, from real semi-major axes
(Mercury `5.7909e10` … Neptune `4.49506e12` m) with `sqrt(GM/r)` circular speeds and catalog masses.
Orbits are **not** compressed for readability — the camera frames `4.6e12 m` on load instead.
Every body carries its real SI collision radius.
Tests: `theSolarSystemPresetHasAllTenBodies`, `theDefaultSceneIsTheFullSolarSystem`.

## 9. Default experience

**✅ IMPLEMENTED.** `Preset.DEFAULT = FULL_SOLAR_SYSTEM`; the ViewModel builds it in `init`, so first
launch is never empty. Re-entry restores the saved session first and only falls back to the default when
there is nothing to restore — `reEnteringTheSandboxKeepsTheUsersOwnSimulation` proves body count,
simulated time and preset survive a serialize/restore round trip.

## 10. Moon / Earth correctness

**✅ IMPLEMENTED.** The Moon is an ordinary body in the same arrays with its own id, mass, radius,
position and velocity. There is **no render-transform parenting and no animation** — it is placed at
Earth's position plus `MOON_ORBIT_RADIUS` perpendicular to the Earth–Sun line, with Earth's velocity plus
its own circular speed about Earth's mass, and then integrated by the same N-body solver as everything
else. Tests: `theMoonIsAnIndependentBodyBoundToEarth` (separation within 2 % of `3.844e8 m`, relative
speed below escape), `theMoonRespondsToBothEarthAndTheSun` (both contributions non-zero and the total
consistent with their sum), `theMoonActuallyOrbitsEarthOverTime` (the Earth-relative angle changes over
a week of simulated time while the separation holds).

## 11. Marble rendering and display size

**🟡 PARTIAL** · 📱 for the look.

The display policy is deterministic and documented in `CameraState.displayScale`: at zoom ≥ 1 the drawn
radius is the body's scene radius; below 1 it shrinks only as `zoom^0.35`, with a `1.6 dp` floor so
nothing vanishes. This keeps Sun ≫ Jupiter > Earth > Moon and Mercury < Earth at any zoom
(`visualSizeOrderingSurvivesZoomingOut`). Display size is now genuinely independent of the collision
radius — the Solar System proves it, drawing Earth at 10 dp while colliding at `6.371e6 m`
(`visualSizeIsIndependentOfThePhysicalCollisionRadius`).

Rendering is the existing minimal marble: a radial-gradient sphere, soft contact shadow, thin rim, one
small specular highlight. Cached brushes are reused at any zoom by scaling the canvas around the body, so
zooming allocates nothing.

🟡 because for hand-laid presets the dp radius still *is* the collision radius (the locked §3.4 rule); the
override exists but only the Solar System uses it. 📱 "Subtle depth, tactile, not a videogame" is a visual
judgement nobody has made yet.

## 12. Collision physics

**🟡 PARTIAL — read this one carefully.**

Implemented: detection by physical radii; the closing speed **along the contact normal** is compared with
the pair's mutual escape speed `sqrt(2G(m1+m2)/(r1+r2))`, giving `ImpactTier` LOW (< 1×), MODERATE (1–3×)
and HIGH (> 3×) — a dimensionless, physically meaningful energy classification rather than an arbitrary
threshold. Low-energy marble contacts bounce (restitution 0.4); everything else merges, conserving mass
exactly and momentum to floating-point tolerance. Every contact emits exactly one `CollisionImpact`.
Tests: `aGentleTouchIsClassifiedLowAndABlastIsClassifiedHigh`, `everyCollisionEmitsExactlyOneImpactEvent`,
`aMergeConservesMassAndMomentum`, plus the 20 pre-existing collision tests.

**Not implemented: destructive fragmentation into a remnant.** A HIGH-energy impact is *classified* as
high and gets the violent `SHATTER` effect, but the bodies still merge — no fragments are spawned and no
reduced-mass remnant is produced. Doing it properly means creating N new bodies against a 20-body cap and
a mass-conservation guarantee, which is a physics change large enough that guessing at it would be worse
than declaring it. **This is the largest single gap in the brief.**

## 13. Premium collision animation

**🟡 PARTIAL** · 📱 for the look.

`sim/Effects.kt` (264 lines) is a fixed pool: 10 effects × 22 particles, all arrays preallocated, indices
reused oldest-first, **zero allocation per frame or per collision**. Each effect has a white contact flash
that dies in the first third, an expanding ring eased out over its life, and debris that fades as
`(1-t)²`. Everything is driven by elapsed real time through `effects.update(dtReal)`, not by a frame
counter. Colours come from the body's own tone and the theme accent — no neon.
Tests: `theEffectPoolIsBoundedAndNeverGrows` (200 spawns stay within the pool),
`effectsExpireOnElapsedTimeAndFreeTheirSlot`.

📱 Whether it reads as "premium and brief" rather than "cartoon" has not been seen. 🟡 because that
judgement is the actual acceptance criterion.

## 14. Collision haptics

**🟡 PARTIAL** · 📱 for the feel.

`spawnEffectsFor` queues **exactly one** `HapticCue` per event — LOW→LIGHT, MODERATE→MEDIUM,
HEAVY for HIGH and for black-hole capture, LIGHT for a wormhole traversal. The queue is drained inside
the frame callback (not through recomposition), the strongest cue in a frame is played, and the event
list is cleared every frame, so bodies resting in contact cannot buzz. `performHapticFeedback` respects
the system haptic setting and does nothing on a device without a vibrator.
Tests: `oneCollisionQueuesExactlyOneEffectAndOneHaptic` (including "no continuous buzz after settling"),
`hapticCuesEscalateWithSeverity`.

📱 **No vibration has been felt.** Intensity mapping onto Compose's two feedback constants
(`TextHandleMove` / `LongPress`) is coarse — that is the honest limit of the platform API used here.

## 15. Black-hole destruction

**🟡 PARTIAL.**

There is no instant vanish: capture emits an `ACCRETION` effect whose particles are pulled **inward** to
the hole over the effect's life while fading, with a contracting ring, and a single HEAVY haptic. The hole
itself stays exactly where it was and is never displaced by the capture.

**Missing:** the brief's staged sequence — *tidal stretch → disruption → inward spiral → gone*. Only the
inward spiral is modelled. There is no stretching of the doomed body and no separate disruption phase,
because the body is removed by the physics step in the same frame the effect starts; showing the stretch
would mean keeping a doomed body alive in a special state, which is a simulation change, not a visual one.

## 16. Black-hole capture takes precedence

**✅ IMPLEMENTED.** `Collision.resolve` computes `willBounce` with an explicit guard so a black hole can
never bounce off anything, and capture is resolved before body-body contact. Tests:
`aBlackHoleNeverBouncesOffAnything`, `blackHoleCaptureBeatsASimultaneousBodyCollision` (a marble inside
the ring *and* touching another marble is captured, not merged sideways).

## 17. Wormhole stays distinct

**✅ IMPLEMENTED** (pre-existing, re-verified). Massless, exerts no gravity, is not treated as an ordinary
body by the collision system, renders as a pulsing throat with an inner ring in its own warm/cool pair
colours, and the inspector labels it "a teaching model, not an established phenomenon" in both languages.
Covered by the existing wormhole tests.

## 18. Preset selector

**✅ IMPLEMENTED.** `ui/PresetSheet.kt` lists all nine scenes including the five required ones
(FULL SOLAR SYSTEM, EARTH + MOON, SUN + EARTH, THREE BODY, EMPTY TABLE — the last two are new).
`loadPreset` is the single safe switch path: it **pauses**, rebuilds the arrays from scratch, clears
trails, effects and queued haptics, resets the selection and the prediction, recomputes accelerations and
the barycentre, and re-frames the camera. It resumes only when the user presses play.
`everyPresetLoadsPausedAndCleansUpTheOldScene` runs this for **every** preset and checks paused state,
zero effects, zero haptics, no selection, `simTime == 0`, no duplicate ids and no surviving trail.

Consequence worth noting: because a challenge sets up a preset, submitting your prediction is now what
starts a challenge running — predict first, then watch.

## 19. Manipulation UX

**🟡 PARTIAL** · 📱. Selection draws a ring and a label, the HUD offers "Open inspector", and the inspector
shows body, mass, radius, velocity and type. Release preserves velocity and mass exactly (requirement 2).
🟡 because the **dragging halo** is not drawn — `dragOriginX/Y` is exposed on the ViewModel for the ghost
marker, but `TabletopCanvas` does not render it yet.

## 20. Position editing is identical to dragging

**✅ IMPLEMENTED.** `setPosition` runs the same code path as a drag. `editingPositionIsIdenticalToDragging`
performs both routes on two identical simulations and asserts identical position, velocity and mass with
delta `0.0`.

## 21. Velocity editing is distinct from dragging

**✅ IMPLEMENTED.** Speed magnitude slider + direction dial — no vector components are ever shown.
`setSpeedMagnitude` and `setDirection` change velocity without touching position:
`editingVelocityChangesMotionWithoutTeleporting` asserts the position is unchanged to delta `0.0` while
the speed becomes exactly what was asked for.

## 22. Mass editing

**🟡 PARTIAL** · 📱. Logarithmic slider over `BodyType.massRange`, precise, ranged, with a live readout in
both scientific and human units, and the dotted prediction path is recomputed on every change so the
gravitational consequence is immediate. `massEditChangesGravityAndTrajectory` proves the trajectory really
changes. 🟡/📱 because "visibly demonstrates" is a perceptual claim.

## 23. Educational feedback

**✅ IMPLEMENTED.** Three new Persian-first cards were added for the new moments — position moved, velocity
changed, impact energy — bringing the catalog to 12. They fire only on the event, through the existing
detector throttle, and the existing warm three-tier (what / why / try) structure is unchanged.

## 24. Accessibility

**🟡 PARTIAL** · 📱. Controls are ≥ 48 dp, the HUD and both sheets use `navigationBarsPadding()` so nothing
sits behind system navigation, the slider mirrors correctly for Persian RTL, and the gesture layer cannot
block a slider (the slider consumes its own pointer stream and the canvas handler is below the sheets).
📱 Reachability, overflow on small screens and RTL/LTR layout correctness have not been seen on a device.

## 25. Performance

**🟡 PARTIAL** · 📱. Structurally: physics runs on plain arrays off the recomposition path; camera state is
a **plain Kotlin object**, so pan/zoom/twist mutate no Compose state and trigger **no recomposition** —
the canvas re-reads it inside the draw lambda only; effects are a fixed pool with no per-frame allocation;
brushes are cached and reused across zoom by canvas scaling; haptics are event-driven; the 20-body cap
stands.

📱 **No frame rate has been measured, on any device.** The one honest performance concern to flag: 100×
can execute up to 1024 substeps in a single frame, which is 10× the old ceiling. It is bounded and the
debt is discarded rather than spiralled, but whether a mid-range phone holds 60 fps at 100× with ten
bodies is exactly the kind of claim this document will not fabricate.

## 26. Tests

**✅ IMPLEMENTED.** `GravityUpgradeTest` (40 tests) covers every category the brief lists — camera
(pinch, pan, angle, transitions, round-trip), dragging (position changes; velocity and mass unchanged),
speed 1×/10×/100×, spawning (every catalog body), Solar System (all ten bodies, Moon independent and
bound), collisions (detection, low/high energy, mass and momentum, one effect, one haptic), black hole
(capture, removal, effect, no duplicate), presets (loads, clears old state, no duplicates or stale
effects) and lifecycle (pause/resume, save/restore).

**Not covered by automated tests:** slider gesture behaviour. There is no Compose UI-test dependency in
this project, and adding one would be a new dependency the brief forbids. Slider correctness is argued
from the root-cause fix, not proven by a test — that is why requirement 4 carries a 📱.

Full suite: **108 tests, 0 failures** (`GravityPhysicsTest` 19, `GravityCollisionTest` 20,
`GravitySandboxIntegrationTest` 29, `GravityUpgradeTest` 40).

## 27. Real-device verification

**❌ NOT POSSIBLE HERE — stated plainly, as instructed.**

This sandbox has no Android device, no emulator, no ADB, and no local Android SDK or JDK. The APK is built
by GitHub Actions and cannot be installed or launched from here. **No visual, animation, haptic, gesture-
feel or FPS observation has been made, and none is claimed anywhere in this document.**

What the user must verify by hand on a real phone: slider behaviour across the full matrix in
requirement 4; two-finger camera feel and 1↔2-finger transitions; long-press timing against the `+`
button; the look of the impact effects and the accretion spiral; haptic strength and that nothing buzzes
continuously; Persian RTL layout; and 60 fps at 100× with a full Solar System.

## 28. Build verification

**✅ IMPLEMENTED.** Run `33500039671`: `./gradlew assembleDebug` succeeded, and the build-script CI gate
ran `testDebugUnitTest` first — **`tests=108 failures=0 skipped=0 files=4`**. No Python was used as a
substitute for the Kotlin build or the Kotlin tests at any point in this task.

## 29. Discipline

**✅ IMPLEMENTED.** No unrelated ZIG screen, astronomy engine, satellite/TLE feature, AR view, Sky Canvas,
Moon screen or navigation route was touched — the diff is confined to `com.zig.gravity.*` plus its tests.
**No dependency was added.** The existing N-body solver, the fixed 3600 s timestep, the Compose canvas
renderer and the locked architecture all stand; no Filament, SceneView, Box2D or third-party physics was
introduced. Nothing rotates decoratively, no planet is photorealistic, and no effect is neon.

---

## Files changed

**New (838 lines):** `sim/CameraState.kt`, `sim/Effects.kt`, `ui/SandboxSlider.kt`, `ui/PresetSheet.kt`,
`test/GravityUpgradeTest.kt`.

**Modified:** `physics/EngineConstants.kt` (ladder, substeps), `physics/SimEvent.kt` (`CollisionImpact`,
`ImpactTier`), `physics/Collision.kt` (impact emission, black-hole precedence, physical-radius merge),
`physics/SimArrays.kt` (`physicalRadius` override, `slotOfCatalog`), `sim/Presets.kt`
(`FULL_SOLAR_SYSTEM`, `THREE_BODY`, `EMPTY_TABLE`, real radii, `frameHalfSpanM`),
`sim/SimulationViewModel.kt` (drag rewrite, camera/effects/haptics ownership, framing, teaching),
`ui/TabletopCanvas.kt` (camera projection, display-size policy, effect layer),
`ui/GravitySandboxRoot.kt` (unified gesture handler, haptic drain, camera panel, preset sheet),
`ui/InspectorSheet.kt` (live sliders, separated sections), `ui/HudBar.kt` (camera button),
`edu/TeachingCatalog.kt` + `edu/detectors/SimulationDetectors.kt` (three cards),
`test/GravitySandboxIntegrationTest.kt` + `test/GravityPhysicsTest.kt` (updated expectations).

## The three things that are genuinely not done

1. **Destructive fragmentation with a remnant (req 12)** — high-energy impacts are classified and shown as
   violent, but they still merge.
2. **The black hole's tidal-stretch and disruption stages (req 15)** — only the inward spiral exists.
3. **Editable type and collision behaviour in the inspector (req 3)** — both are displayed, not editable.

Plus one small omission: the **drag ghost/origin marker (req 19)** is plumbed on the ViewModel but not
drawn.

# ZIG INVENTION, NOVELTY & PATENTABILITY AUDIT

**Independent adversarial examination · 2026-09-02**
Subject: `alimehali-cyber/Lm-arena` @ `f5d5700` (branch `arena/01a059b5-lm-arena`)
Scope: 163 Kotlin files / 57,752 LOC. Source code treated as sole source of truth.

> **This is a technical audit, not legal advice.** No statement here is a legal determination of
> patentability. Only a qualified Iranian patent attorney can make that determination.

---

# EXECUTIVE VERDICT

### 1. Is ZIG itself an invention?

**No.** ZIG is a large, competently-built application assembled almost entirely from published
algorithms (SGP4/Spacetrack Report #3, VSOP87, Meeus, IAU 1976/2000B, velocity-Verlet) and
documented platform APIs (Camera2 `LENS_INTRINSIC_CALIBRATION`, CameraX `sensorToViewTransform`,
`TYPE_ROTATION_VECTOR`, `GeomagneticField`). Size and scientific ambition are not invention.

### 2. Which subsystems potentially contain inventions?

Only three mechanisms rise above "correct implementation of a known technique," and **none of them
survives adversarial examination as a patent candidate**:

- `SimulationDetectors` — runtime orbital-regime classifier driving pedagogy (best of the three)
- `StarlinkTrainManager.detectTrain` — launch-cohort train detection from live TLEs
- `OrientationProvider.updateQuaternion` — motion-adaptive SLERP smoothing

### 3. Strongest candidate

`SimulationDetectors` (INV-006), and it is a **rank 2–3 of 6** — a defensible novel *combination*
with a weak inventive step. Recommended protection: **defensive publication + copyright**, not a
patent.

### 4. What is clearly NOT novel

SGP4, VSOP87, Meeus eclipse/phase methods, IAU precession/nutation, refraction, Julian-date and
ΔT handling, pinhole projection, quaternion SLERP, one-star pointing alignment, velocity-Verlet,
adaptive substepping, reduced-mass collision energy, MVVM/Compose/Room/coroutines architecture,
Persian translation, all UI and all icons.

### 5. What is uncertain

Provenance. There is strong circumstantial evidence of AI-generated code and coefficient data
(§ Provenance), but authorship cannot be established from a repository whose history was squashed
to a single commit.

### 6. What requires further prior-art research

Only INV-006, and only if the owner rejects this report's recommendation. A professional search
should target intelligent-tutoring-system and educational-simulation patents (2005–2024).

### 7. What should be patented?

**On the present evidence: nothing.** Two independent grounds, either sufficient alone:

- **(a) Absolute-novelty bar.** The complete source is on a **public GitHub repository**
  (verified `"private": false`, HTTP 200 unauthenticated, public since **2026-08-31**). Iran's
  Industrial Property Protection Law 2024 requires **absolute worldwide novelty**.
- **(b) No demonstrable technical effect.** Every accuracy claim tested in this audit was found
  overstated, and the most invention-shaped mechanism in the codebase **does not execute**.

### 8. What should be protected otherwise

Copyright (automatic; software also registrable with the Ministry of Culture for evidentiary
purposes, 30-year term), trademark (**ZIG** word mark + the Z-rocket logo), and trade secret for
anything not yet published — of which, given (a), there is very little.

---

# CRITICAL FINDINGS FIRST

These four findings dominate every other conclusion.

## F-1 · The source code is already publicly disclosed

```
$ curl -s https://api.github.com/repos/alimehali-cyber/Lm-arena     # no credentials
HTTP 200
  private : False
  created : 2026-08-31T18:47:40Z
  pushed  : 2026-09-01T18:18:34Z
  license : None
```

Iran's Industrial Property Protection Law 2024 requires that an invention "must not have been
previously disclosed anywhere in the world" [1](https://www.iranbestlawyer.com/protection-of-patents-and-industrial-designs/). Commentary on the 2024 law
describes a six-month window for disclosure **"without the knowledge or consent of the inventor"**
[6](https://sabaip.com/iran-new-ip-law-v-old-ip-law/) — deliberate self-publication to a public repository is the opposite of that, so on
the face of it **no grace period applies**. Verify this point with counsel; it is the single most
consequential legal question in this audit.

Absence of a `LICENSE` file does not help: it withholds a copyright licence but does nothing about
patent novelty. Deleting the repository now would **not** cure the disclosure, and this report does
not recommend destroying evidence.

## F-2 · The documented astronomical accuracy is overstated by ~1,500×

`VSOP87Engine.kt:11` claims *"Accuracy: ~1 arcsecond for years 2000 BC to 6000 AD."*

Measured against Meeus low-precision solar theory (±0.01°), replicating `calculateEarth` exactly:

| Year | code L | reference L | error |
|---|---|---|---|
| 2000.0 | 100.3777° | 100.3822° | −0.27′ |
| 2015.0 | 100.2852° | 100.4928° | −12.46′ |
| 2030.0 | 100.1938° | 100.6035° | **−24.58′ (1,475″)** |

Root cause: `earthL1 = [Term(62830758500.0, 0.0, 0.0)]` — one constant term whose rate is
628.307585 rad/century. The correct VSOP87 value is 628.3319667. The 0.0244 rad/century deficit is
a **secular drift of ~1.4°/century (~50″/yr)** in Earth's heliocentric longitude, which propagates
into the Sun's position and thus into every geocentric planetary position, rise/set and twilight
time in the app.

Also measured: `mercuryR0` yields 0.3200–0.4872 AU against a true 0.3075–0.4667 AU (2–4% error).
Venus/Earth/Mars/Jupiter/Saturn/Uranus/Neptune radius series all check out.
And `PlanetEngine.kt:116` maps `PlanetType.PLUTO -> VSOP87Engine.Planet.NEPTUNE` — Pluto returns
Neptune's position.

**Patent consequence:** "improved positional accuracy" is unavailable as a technical effect.

## F-3 · The most invention-shaped mechanism does not execute

`StarlinkTrainManager.epochToJulian` feeds a TLE **day-of-year** into the month slot of the Meeus
calendar formula. Verified against a self-checked reference (`JD(2026-001.0) = 2461041.5`):

| TLE epoch | code JD | true JD | error |
|---|---|---|---|
| 2026-245.5 | 2459694.00 | 2461286.00 | **−1592 d** |
| 2026-001.0 | 2452273.50 | 2461041.50 | −8768 d |
| 2025-180.0 | 2457352.50 | 2460855.50 | −3503 d |

`detectTrain` gates on `epochAge in [-1.0, 7.0]`. For a TLE published **today**, the computed age is
**+1592 days**. The gate can never pass; `detectTrain` returns `null` in all realistic conditions.
The feature is dead at runtime.

## F-4 · Documentation systematically overstates implementation

F-2 and F-3 are instances of a pattern. `SGP4Propagator` documents "Full short- and long-period
gravitational perturbations (J2, J3, J4)" and cites Vallado — but contains **no deep-space (SDP4)
terms at all** (`grep -i "deep\|SDP4\|resonan"` → zero hits), so any satellite with a period above
225 minutes is propagated by the wrong model. `ARCalibrationManager` is honest by contrast, stating
plainly that automated multi-star solving is *not* implemented.

**For a patent file this is a systemic risk:** a specification drafted from these comments would
contain statements the code does not support.

---

# MASTER INVENTION TABLE

| ID | Component | Technical problem | ZIG solution | Novelty | Inventive step | Prior-art risk | Technical effect | Provenance | Recommendation |
|---|---|---|---|---|---|---|---|---|---|
| INV-001 | `OrientationProvider.updateQuaternion` | Jitter-vs-lag in AR pointing | Gyro-speed-gated adaptive-α SLERP | **NO** | Very weak | **Fatal** | Not measured | Known method | None |
| INV-002 | `ARProjectionEngine.getCameraIntrinsics` | Unknown camera intrinsics across devices | 3-tier fallback: HW calib → sensor geometry → default | NO | Very weak | Fatal | Robustness (unmeasured) | Android API pattern | Copyright |
| INV-003 | `ARProjectionEngine.projectToScreen` | Celestial → screen mapping | ENU→device→sensor SO(2)→pinhole→`sensorToViewTransform` | NO | Very weak | Fatal | Correctness | Textbook CV | Copyright |
| INV-004 | `ARCalibrationManager` | Device chassis/compass pointing bias | Persisted Euler offset, `R_cal = Rz·Rx·Ry` | NO | Very weak | Fatal | Bias removal | Telescope 1-star align | Copyright |
| INV-005 | `StarlinkTrainManager.detectTrain` | Which launch is currently a visible "train"? | COSPAR cohort + mean-motion band + epoch age | NO | Very weak | **Fatal** | **None — dead code** | Community heuristic | Fix the bug |
| INV-006 | `SimulationDetectors` | Turn raw N-body state into pedagogy | Orbital-regime classifier + hysteresis + repeat budget | **UNCERTAIN** | Weak–moderate | High | Reduced trigger spam | Plausibly original composition | **Defensive publication** |
| INV-007 | `NBodyEngine` | Stable mobile N-body | Verlet KDK + recursive adaptive halving + step budget | NO | Very weak | Fatal | Bounded frame cost | Textbook | Copyright |
| INV-008 | `Collision.kt` severity | Grade impact visuals physically | `E = ½μv²`, tier by `v_rel / v_esc` | NO | Very weak | Fatal | Correct grading | Textbook mechanics | Copyright |
| INV-009 | `AstroDispatchEngine` | Route object IDs to engines | Facade + strategy `when` | NO | None | Fatal | None | Standard pattern | Copyright |
| INV-010 | Persian-first localisation | Bilingual astronomical naming | Parallel `*Fa`/`*En` fields | NO | None | Fatal | None (content) | Content, not mechanism | Copyright |

---

# TOP CANDIDATES — DETAILED

## INV-006 · Simulation-state → pedagogy detector layer  ★ best candidate, still not patentable

**Evidence:** `app/src/main/java/com/zig/gravity/edu/detectors/SimulationDetectors.kt`, `observe()`.

**Technical problem.** A live N-body sandbox produces a continuous stream of float state. Deciding
*when* something pedagogically meaningful has happened — and not saying it forty times a second — is
a real engineering problem.

**Mechanism.** Per frame, for each body, against its dominant attractor:

- specific orbital energy `ε = v²/2 − GM/r`; `ε ≥ 0` ∧ receding ∧ `r > 40·R_primary` ⇒ *escaped*
- semi-major axis `a = −GM/(2ε)`; three consecutive observations with `a ≤ 0.97·a_prev` ⇒ *decayed*
- cumulative swept angle ≥ **300°** ⇒ *orbit stabilised*
- exactly two massive bodies, mass ratio ∈ [0.2, 5.0], sweep ≥ 300° ⇒ *barycentric pair*
- hard events (merge, black-hole capture, wormhole) short-circuit inference
- global **2 s hysteresis** + per-concept `REPEAT_BUDGET` (2–4 lifetime emissions)

**Technical effect.** Bounded, non-repeating explanatory output from an unbounded state stream. This
is a genuine, if modest, effect — and notably it is the *only* candidate in this audit whose claimed
effect the code actually delivers.

### WHY THIS IS NOT AN INVENTION (examiner's case)

Every component is elementary. `ε` and `a = −GM/2ε` are first-year orbital mechanics. Sign-of-energy
as a bound/unbound test is the definition of escape. Accumulating swept angle to detect a closed
orbit is obvious. Debouncing notifications with a hysteresis timer and a repeat cap is routine UI
engineering that predates the field. The composition is a straightforward aggregation: *compute
standard orbital elements, threshold them, debounce the output*. A competent games or simulation
engineer asked to "explain what the simulation is doing without spamming the user" arrives here
directly. Educational simulators (PhET, Universe Sandbox, Algodoo) and the intelligent-tutoring
literature have detected simulation states to trigger feedback for two decades.

### RESPONSE

The only non-obvious detail is the *specific* combination of thresholds chosen to make inference
stable under numerical noise — the 3-consecutive-observation decay streak with 3%/1.03 asymmetric
hysteresis on `a` is a considered anti-chatter design, not an arbitrary constant. That is a genuine
engineering nicety. It is almost certainly not an inventive step.

### WHAT WOULD STRENGTHEN THE CASE

A measured false-positive/false-negative rate against hand-labelled trajectories versus a naive
threshold detector. Absent that, there is no evidence of technical superiority — only of function.

**Rank: 2–3 / 6. Recommendation: defensive publication.**

---

## INV-001 · Motion-adaptive SLERP orientation smoothing — DEFEATED BY PRIOR ART

**Evidence:** `OrientationProvider.kt`, `updateQuaternion()`.

```
movementFactor = clamp(max(gyroSpeedDeg/25, angleDiffDeg/4), 0, 1)
alpha          = 0.045 + (0.40 − 0.045) · movementFactor
q ← SLERP(q, q_target, alpha)          // LERP fallback when dot > 0.9995
```

Low gain when still (kills jitter), high gain when panning (kills lag).

### Prior art — decisive

This *is* the 1€ filter. Casiez, Roussel & Vogel, **CHI 2012**: *"a first order low-pass filter with
an adaptive cutoff frequency: at low speeds, a low cutoff stabilizes the signal by reducing jitter,
but as speed increases, the cutoff is increased to reduce lag"* [4](https://github.com/casiez/OneEuroFilter). That is a
sentence-for-sentence description of ZIG's mechanism and its stated purpose.

Worse, **Meta ships it for quaternions**: the Oculus Interaction SDK publishes
`OneEuroFilter.CreateQuaternion()`, documented as *"a speed-based lowpass filter with adaptive
cutoff"* [2](https://developers.meta.com/horizon/reference/interaction/v69/class_oculus_interaction_input_one_euro_filter/). Speed-adaptive quaternion smoothing for AR head/device pose is a
shipped, documented commercial product feature.

ZIG's differences — SLERP rather than component-wise LERP, `max()` of two motion proxies rather than
a filtered derivative, linear rather than reciprocal gain mapping — are parameterisation choices
within the known method.

**Novelty: NO. Inventive step: very weak. Rank 1 / 6.**

---

## INV-005 · Starlink train cohort detection — DEFEATED TWICE OVER

**Evidence:** `StarlinkTrainManager.kt`, `detectTrain()`.

Group live TLEs by COSPAR launch designator; require cohort ≥ 2, mean motion ∈ (15.7, 16.5) rev/day
(deployed but not yet orbit-raised), TLE epoch age ≤ 7 d; return the largest qualifying cohort.

This is the most *concrete* and most *specific* mechanism in the application, and on structure alone
it is the one I most expected to survive. It does not.

**Defeat 1 — prior art.** Heavens-Above has offered *"Starlink passes for all objects from a
launch"* — i.e. launch-cohort grouping — since **2020** [3](http://www.attackpoint.org/discussionthread.jsp/message_1385994). findstarlink.com,
ISS Detector's "Starlink Leader/Trailer", and James Darpinian's `?special=starlink-2019-11` all
predate this by years [1](https://www.reddit.com/r/spacex/comments/dw50qs/how_to_spot_the_spacex_starlink_satellite_train/). Observer guidance to *"find a train launch that occurred no more than
4 days ago"* is standard published advice [2](https://orbitaltoday.com/2026/02/21/starlink-satellite-train-tracker-guide-how-to-see-train-tonight/). Separately, inferring orbital state changes from TLE
mean-motion / semi-major-axis thresholds is an established academic literature — Song's Semi-major
Axis Change Method (2012), Lemmens & Krag's TLE Consistency Check (2014), and subsequent EM- and
clustering-based detectors [4](https://www.sciencedirect.com/science/article/abs/pii/S027311772400615X).

**Defeat 2 — it does not work.** See F-3. The freshness gate can never pass.

**Novelty: NO. Technical effect: none demonstrated. Rank 1 / 6.**
*Engineering note: fix `epochToJulian` regardless — this is a live functional bug.*

---

## INV-002/003/004 · The AR pipeline — high quality, zero novelty

The projection chain is genuinely well built: ENU unit vector → `Rᵀ·v` into device frame → SO(2)
rotation by `SENSOR_ORIENTATION` → pinhole with `(fx, fy, cx, cy, skew)` → CameraX
`sensorToViewTransform`, with an analytical `FILL_CENTER` fallback that handles net
sensor-minus-display rotation. The three-tier intrinsics resolution degrades honestly and even
records which tier was used (`IntrinsicsSource`).

All of it is textbook projective geometry over documented Android APIs, used as documented. The
calibration layer is one-star pointing alignment — a technique older than the smartphone, standard
in telescope GoTo mounts and present in mainstream sky apps; Sky Map's figure-8 compass calibration
is the consumer-facing equivalent [2](https://www.lifewire.com/fix-google-sky-map-not-working-5202290).

Instructive contrast: Celestron StarSense Explorer markets *"patent-pending technology … to analyze
star patterns overhead to calculate the telescope's position in real-time"* [3](https://astrobackyard.com/astronomy-apps-for-stargazing/) — i.e. the
*actually* patent-worthy thing in this space is **plate solving from the camera image**. ZIG does
not do this. `ARCalibrationManager`'s own comment concedes it: *"can later be substituted by an
automated multi-star solver."*

**That absent feature is where a real invention would live.**

---

# PRIOR-ART REPORT

| Ref | Source | Date | Defeats |
|---|---|---|---|
| Casiez, Roussel & Vogel, "1€ Filter", CHI 2012, DOI 10.1145/2207676.2208639 [4](https://github.com/casiez/OneEuroFilter) | ACM | 2012 | INV-001 (squarely) |
| Meta Oculus Interaction SDK, `OneEuroFilter.CreateQuaternion()` [2](https://developers.meta.com/horizon/reference/interaction/v69/class_oculus_interaction_input_one_euro_filter/) | Commercial product | current | INV-001 (quaternion form) |
| Hoots & Roehrich, Spacetrack Report #3 (1980); Vallado et al., AIAA 2006-6753 | Public domain | 1980/2006 | All SGP4 claims — **cited in ZIG's own header** |
| Bretagnon & Francou, VSOP87; Meeus, *Astronomical Algorithms* Ch. 32 | Published | 1987/1998 | All planetary ephemeris claims |
| Meeus Ch. 49 & 54; Espenak/NASA 5-Millennium Canon | Published | 1998/2006 | All eclipse claims — cited in ZIG's own header |
| IAU 1976 precession; IAU 2000B nutation | Standards | 1976/2000 | `FrameTransformationEngine` |
| Heavens-Above "passes for all objects from a launch" [3](http://www.attackpoint.org/discussionthread.jsp/message_1385994) | Web service | 2020 | INV-005 (cohort grouping) |
| findstarlink.com; ISS Detector Starlink Leader/Trailer; darpinian.com [1](https://www.reddit.com/r/spacex/comments/dw50qs/how_to_spot_the_spacex_starlink_satellite_train/) | Web/apps | 2019– | INV-005 |
| Song (2012) SACM; Lemmens & Krag (2014) TCC/TTSA; Li (2020); Bai (2019) [4](https://www.sciencedirect.com/science/article/abs/pii/S027311772400615X) | Literature | 2012–2021 | INV-005 (TLE threshold inference) |
| Celestron StarSense Explorer (patent-pending plate solving) [3](https://astrobackyard.com/astronomy-apps-for-stargazing/) | Commercial | 2020 | Marks the boundary ZIG does not cross |
| Google Sky Map (open source), Stellarium, SkySafari, Star Walk, SkyView | Products | 2009– | INV-002/003/004 |

Every algorithmic citation ZIG needs to defeat, **ZIG cites itself in its own file headers.** This is
scientifically honest and is the right way to write the code — but it means the specification would
be self-anticipating.

---

# PROVENANCE REPORT

| Belongs to | Items |
|---|---|
| **Known science (public domain)** | SGP4, VSOP87 coefficients, Meeus algorithms, IAU models, Newtonian gravity, velocity-Verlet, pinhole model, SLERP |
| **Platform / third party** | Camera2, CameraX, SensorManager, `GeomagneticField`, Compose, Room, Retrofit/OkHttp/Moshi, Firebase, Coil, WorkManager, Roborazzi |
| **Live data (third-party)** | CelesTrak TLE feeds — check terms before commercial redistribution |
| **Plausibly ZIG-original composition** | `SimulationDetectors` design; the gravity sandbox's teaching architecture; the specific engine decomposition |
| **Uncertain / unattributable** | Everything else |

### AI-generation indicators

Not proof, but the pattern is consistent and material:

1. **Grandiose headers that outrun the code** — "Full … perturbations (J2, J3, J4)" with no deep-space
   terms; "~1 arcsecond" against a measured 1,475″.
2. **Structurally implausible coefficient data.** `venusL0` contains a 22-term tail with geometrically
   decaying amplitudes (218200, 183400, 141200, 119800, 92100 …) cycling through only four phase
   values (2.8318, 4.3854, 1.2782, 5.9390). Real fitted trigonometric series do not look like this.
   **I tested this and must report against my own hypothesis:** Venus's *radius* series is accurate
   (0.7186–0.7284 AU vs true 0.7184–0.7282). I did **not** verify the Venus longitude series, so the
   fabrication hypothesis is **unresolved**, not confirmed.
3. **Plausible-looking but dimensionally wrong formulas** — `epochToJulian` feeding day-of-year into a
   month slot is a characteristic generated-code error: right shape, wrong semantics.
4. **A single squashed commit** (`fd10ed8`, one author) destroys the development record that would
   otherwise evidence inventorship.

### Authorship vs inventorship vs ownership

These are three different things and must not be conflated:

- **Copyright authorship** — likely thin or absent for purely machine-generated passages in many
  jurisdictions. Iranian treatment of AI-generated works is unsettled; get advice.
- **Inventorship** — attaches to the natural person who conceived the inventive concept. Using an AI
  as a tool does not, by itself, destroy human inventorship; but conception must be *provable*.
- **Ownership** — follows inventorship and contract. Under the 2024 law, where an invention falls
  outside contractual activity the economic rights vest in the inventor, with a non-exclusive
  employer right where employer resources were used [3](https://www.legal500.com/guides/chapter/iran-intellectual-property/).
- **Licence rights** — no `LICENSE` file exists; third-party dependency licences still bind.

**With a one-commit history and no design notebook, conception is currently unevidenced.**

---

# PUBLIC DISCLOSURE TIMELINE

| Date | What | Where | Public? | Discloses | Risk |
|---|---|---|---|---|---|
| 2026-08-31 18:47 UTC | Repository created | github.com/alimehali-cyber/Lm-arena | **YES** | — | — |
| 2026-08-31 19:16 | `main` — full app, sandbox removed | GitHub | **YES** | All 32 astro engines: INV-001…005, 009, 010 | **Severe** |
| 2026-08-31 20:54 | `fd10ed8` CI fix | GitHub | **YES** | Build configuration | Low |
| 2026-09-01 | Gravity Sandbox implementation pushed | GitHub | **YES** | INV-006, 007, 008 | **Severe** |
| 2026-09-01 18:18 | Latest push (icon, sheet fix, reports) | GitHub | **YES** | Everything | **Severe** |
| Continuous | CI build logs + `app-debug-apk` artifacts | GitHub Actions | Public repo ⇒ public | Compiled app | Moderate |
| Unknown | Prior "RED" releases, AI Studio projects, app-store listings | Not determinable from this repo | Unknown | Unknown | **Investigate** |

**Not determinable from the codebase:** whether RED was previously published to an app store, whether
Google AI Studio projects were shared, and whether the pre-squash history was ever public. These must
be established by the owner. Preserve all evidence — do not delete history.

---

# NOVELTY VS INVENTIVE STEP

| ID | Novelty (disclosed before?) | Inventive step | Reasoning |
|---|---|---|---|
| INV-001 | **YES, disclosed** | Very weak | 1€ filter, CHI 2012; Meta ships the quaternion form |
| INV-002 | YES | Very weak | The fallback order is the one the Android docs imply |
| INV-003 | YES | Very weak | Textbook projective geometry, documented APIs |
| INV-004 | YES | Very weak | One-star alignment predates smartphones |
| INV-005 | YES | Very weak | Heavens-Above 2020 + TLE-threshold literature |
| INV-006 | **UNCERTAIN** | Weak–moderate | Components elementary; composition plausibly unpublished |
| INV-007 | YES | Very weak | Verlet + recursive halving + budget are all standard |
| INV-008 | YES | Very weak | Reduced-mass energy is textbook |
| INV-009/010 | YES | None | Design pattern; content |

"Not found in a search" is not novelty, and I have not treated it as such: for INV-001 and INV-005 I
found specific, dated, defeating references.

---

# KNOWN COMPONENTS, NOVEL COMBINATION?

Applied strictly to each multi-component candidate:

| Candidate | Components known? | New technical interaction? | Unexpected effect? | Verdict |
|---|---|---|---|---|
| AR chain (002+003+004) | All | No — each stage feeds the next in the only sensible order | No | Obvious aggregation |
| INV-005 | All | No — three independent filters ANDed | No (and inoperative) | Obvious aggregation |
| INV-006 | All | **Partly** — detector output gates *itself* through a shared budget, so concepts compete for a global attention channel | Modest | **Weakest form of novel combination** |
| INV-007 | All | No | No | Obvious aggregation |

Only INV-006 exhibits any cross-component feedback, and it is a shared rate limiter.

---

# DO NOT PATENT — PROTECT OTHERWISE

- **Copyright (automatic).** All 57,752 lines. Optional registration with the Ministry of Culture for
  evidentiary value; software term 30 years [3](https://www.legal500.com/guides/chapter/iran-intellectual-property/).
- **Trademark (act on this).** **ZIG** / **زیگ** word mark and the Z-with-rocket logo. This is the most
  valuable and most *available* right the project has, and unlike patents it is not damaged by
  publication. File in the relevant Nice classes (9, 41, 42).
- **Copyright, not patent.** Persian astronomical terminology, all teaching-card and tutorial text,
  the deep-sky and satellite catalogues (curated data — note Iran does not recognise a separate sui
  generis database right), icons, UI, colour system.
- **Trade secret — largely foreclosed** by F-1. Anything genuinely unpublished (unreleased tuning,
  future plate-solving work) can still be kept secret; secrecy is protected indefinitely while it
  lasts.
- **Defensive publication.** INV-006. Publishing a clear technical description prevents a third party
  from patenting it against ZIG, at zero cost, and the disclosure has effectively happened anyway.

---

# IRAN-SPECIFIC LEGAL ANALYSIS

**LAW** (from the sources cited; confirm with counsel):

1. Governing statute: **Industrial Property Protection Law 2024**, replacing the 2008 law [9](https://hengamlaw.com/mapping-intellectual-property-in-iran/).
2. Requirements: **novelty** (not disclosed anywhere in the world), **inventive step** (not obvious to
   a skilled person), **industrial applicability** [1](https://www.iranbestlawyer.com/protection-of-patents-and-industrial-designs/)[10](https://rezvanianinternational.com/patent-and-its-registration-process-in-iran/).
3. Novelty is **absolute and worldwide** [10](https://rezvanianinternational.com/patent-and-its-registration-process-in-iran/).
4. Grace period: commentary describes six months for disclosure **without the inventor's knowledge or
   consent** [6](https://sabaip.com/iran-new-ip-law-v-old-ip-law/) — apparently not covering deliberate self-publication. **Verify.**
5. **Utility models** are newly available under the 2024 law with a **lower inventive-step threshold**
   [3](https://www.legal500.com/guides/chapter/iran-intellectual-property/) — but they still require novelty, so F-1 bites equally.
6. Software: protected by the Act on Protection of Rights of Computer Software Creators (2000) and
   general copyright; patentable only "if applied" — i.e. where a technical application exists
   [9](https://hengamlaw.com/mapping-intellectual-property-in-iran/).
7. Procedure: formality examination → publication → substantive examination; **9-month** opposition
   window for patents; Dispute Settlement Board, appeal within one month [3](https://www.legal500.com/guides/chapter/iran-intellectual-property/).
8. Ownership: employment/contract rules as summarised in Provenance above [3](https://www.legal500.com/guides/chapter/iran-intellectual-property/).
9. Invalidity grounds include lack of novelty, lack of inventive step, and **insufficient disclosure**
   [3](https://www.legal500.com/guides/chapter/iran-intellectual-property/) — the last is a live risk given F-4.
10. Iran is **not** a Berne party; foreign copyright protection is not automatic [9](https://hengamlaw.com/mapping-intellectual-property-in-iran/).
11. PCT national-phase entry is available for international filing [9](https://hengamlaw.com/mapping-intellectual-property-in-iran/).

**TECHNICAL ANALYSIS:** No candidate satisfies inventive step; and independently, all candidates were
published on 2026-08-31.

**STRATEGY RECOMMENDATION:** Do not file. Redirect budget to trademark registration and, if a real
invention is built later (see Next Steps), file **before** any publication.

---

# EXPERIMENTAL VALIDATION PLAN

Required before any future filing — and valuable regardless, because two of these have already
failed.

| # | Metric | Baseline | Dataset | Status |
|---|---|---|---|---|
| E-1 | Planetary/solar longitude error (arcsec) | JPL Horizons | 1900–2100, 1000 epochs | **RUN — FAILED.** 1,475″ @ 2030 vs 1″ claimed |
| E-2 | TLE epoch → JD error (days) | Reference JD | 5 epochs | **RUN — FAILED.** −1592 d |
| E-3 | Heliocentric radius vs perihelion/aphelion | Published elements | 8 planets, 100 yr | **RUN — 7/8 pass; Mercury fails** (0.3200–0.4872 vs 0.3075–0.4667) |
| E-4 | Satellite along-track error (km) | STK/Vallado SGP4 vectors | Vallado test cases | Not run — needs deep-space check |
| E-5 | AR angular alignment error (deg) | Astrometric plate solve of camera frames | 20+ devices, 100 pointings | Not run — **requires hardware** |
| E-6 | Jitter (σ, deg) and lag (ms) vs 1€ filter | Casiez reference implementation | Recorded IMU traces | Not run — **the decisive INV-001 experiment** |
| E-7 | Detector precision/recall | Hand-labelled trajectories | 200 scenarios | Not run — the INV-006 experiment |
| E-8 | Energy drift ΔE/E over 10⁶ steps | Analytic two-body | Closed orbits | Not run |

E-6 is the one that matters for INV-001: if ZIG's filter cannot beat the 2012 reference on the
jitter/lag curve, the candidate is finished on the merits as well as on prior art.

---

# CRITICAL RISKS

1. **Public disclosure (F-1)** — likely dispositive against all patent candidates.
2. **No inventive step** — independently fatal for INV-001…005, 007…010.
3. **Specification/implementation mismatch (F-4)** — a filing drafted from the comments risks
   invalidity for insufficient disclosure, and is a candour problem.
4. **No demonstrated technical effect** — every accuracy claim tested failed.
5. **Unevidenced conception** — one squashed commit, no notebook, no dated design record.
6. **Unresolved AI provenance** — inventorship and authorship both need legal analysis.
7. **Third-party data terms** — CelesTrak TLE redistribution.
8. **Undetermined earlier disclosure** — prior RED releases and AI Studio sharing are unknown and
   could predate even 2026-08-31.
9. **No `LICENSE` file** — separate from patents, this leaves contributors' and users' rights
   undefined.

---

# RECOMMENDED NEXT STEPS

1. **Do not file a patent application on the current codebase.** Nothing in it clears both novelty and
   inventive step, and F-1 forecloses the question regardless.
2. **File the ZIG trademark now.** Word mark + logo. Highest value, lowest risk, undamaged by
   publication, and the only right here that is genuinely worth money.
3. **Fix the science, urgently — this is an app-quality emergency, not an IP task.**
   `earthL1` (F-2), `epochToJulian` (F-3), `PLUTO → NEPTUNE`, `mercuryR0`, and the missing SDP4 path.
   Then correct every doc comment that overstates what the code does.
4. **Establish the disclosure record.** Determine whether RED was ever published, whether AI Studio
   projects were shared, and preserve everything. Do not delete history.
5. **Take one legal question to an Iranian patent attorney**, with this report attached: *does the
   2024 grace period reach deliberate self-publication by the inventor?* Everything else follows.
6. **Defensively publish INV-006** as a short technical note — cheap insurance.
7. **If you want a real patent, build the thing that is missing.** The single highest-value technical
   gap this audit found is the one `ARCalibrationManager` names itself: **automated star-pattern
   recognition (plate solving) from the camera frame to close the AR alignment loop without user
   input.** Celestron considers that space patent-worthy [3](https://astrobackyard.com/astronomy-apps-for-stargazing/); ZIG has the projection
   pipeline and the star catalogue already in place. Build it in **private**, measure it against E-5,
   file **before** publishing, and you would have a genuine candidate instead of a retrospective
   search for one.

---

## Confidence

| Conclusion | Confidence | Basis |
|---|---|---|
| Repository is public | **Certain** | Unauthenticated HTTP 200, `private: false` |
| VSOP87 accuracy overstated | **Certain** | Reproducible measurement, self-checked reference |
| `epochToJulian` broken; `detectTrain` dead | **Certain** | Reproducible measurement |
| No SDP4 in SGP4 | **Certain** | Exhaustive grep |
| INV-001 anticipated by 1€ filter | **High** | Direct textual match to CHI 2012 + Meta product |
| INV-005 anticipated | **High** | Heavens-Above 2020 + literature |
| INV-006 novelty | **Low — unresolved** | No targeted ITS-patent search performed |
| Venus longitude series fabricated | **Unresolved** | Hypothesis **not** confirmed; radius series correct |
| AI-generated portions | **Moderate** | Circumstantial only |
| Iranian grace period inapplicable | **Moderate — needs counsel** | Secondary sources only |

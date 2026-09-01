# ZIG Gravity Sandbox — Forensic Requirements Audit **v2 (spec-verified)**

**Normative source:** `ZIG_Gravity_Sandbox_Master_Roadmap_v5.0 (Final Unified)`, 14 pages — recovered from `origin/arena/01a0592f-lm-arena`, now vendored at `docs/spec/ZIG_Gravity_Sandbox_Master_Roadmap_v5_Final.pdf` (extracted text alongside it).
**Audited tree:** `arena/01a059b5-lm-arena` @ `fd10ed8` + the uncommitted `RedTheme.colors` compile fix.
**Method:** line-by-line source tracing (UI → state → engine → render) + numerical re-derivation of the spec's own physics in Python. Every spec constant I could recompute reproduced exactly (metersPerDp = 3 AU/400 dp = **1.1220×10⁹** ✓ · minimum legal contact 7 dp = **7.854×10⁹ m** ✓ · Earth year at BASE = **31.6 s** ✓ · softening distortion anchors **1.5×10⁻⁴ / 1.02×10⁻⁵ / 6.7×10⁻¹¹** ✓), which confirms my reading of the document is correct.

> **This v2 supersedes the v1 audit.** v1 was written without the PDF, against the requirement list in the request. Three v1 verdicts were **wrong** and are corrected in §0.2 — including two where v1 gave credit the spec does not authorise.

---

## 0. THE HEADLINE FINDING

**The code in this repository is not an implementation of Roadmap v5. It is a pre-spec prototype that predates the document's architecture.**

The spec locks a package structure (§3.3) of ~18 files across `com.zig.gravity/{physics,edu,sim,ui}`. The repository contains **three** relevant files in a different package with a different architecture:

| Spec (§3.3) | Repository | Status |
|---|---|---|
| `physics/SimArrays.kt` (SoA, capacity 20) | — | **absent** |
| `physics/BodyType.kt` (SUN, PLANET, MOON, ASTEROID, TEST_MARBLE, BLACK_HOLE, WORMHOLE_MOUTH) | `astro_engine/GravitySandboxEngine.kt:5` — STAR, BLACK_HOLE, GAS_GIANT, TERRESTRIAL_PLANET, MOON, COMET, ASTEROID | **wrong set** |
| `physics/NBodyEngine.kt` | `GravitySandboxEngine.kt` (force pass + Verlet + collisions) | **partial** |
| `physics/Collision.kt` (merge + bounce + survivor rule) | inline in the engine, no bounce, no survivor tie-break | **partial** |
| `physics/SimEvent.kt` (10 event types) | — | **absent** |
| `physics/EngineConstants.kt` (the §3.2 charter) | 3 loose `const val`s | **absent** |
| `edu/detectors/` (7 pure detectors) | 4 ad-hoc checks inside `GravityTeachingObserver` | **partial** |
| `edu/TeachingCatalog.kt` · `edu/Challenges.kt` | `astro_engine/GravityTeachingEngine.kt` (content only) | **partial** |
| `sim/SimulationViewModel.kt` (frame loop, accumulator, intents, snapshot publish) | — a `LaunchedEffect` inside the composable | **absent** |
| `sim/SimSnapshot.kt` · `Presets.kt` · `SaveState.kt` (DataStore) | presets inline in the engine; no snapshot, no persistence | **partial** |
| `ui/TabletopCanvas.kt` · `HudBar.kt` · `InspectorSheet.kt` · `AddBodySheet.kt` · `TeachingCard.kt` | one 1 610-line `GravitySandboxScreen.kt` | **monolith** |

**Phase completion against Part IV (11 phases): Phase 0 = 0%. No phase is complete.** Weighted against the full spec the implementation is **≈22%**, and that number is carried almost entirely by one thing the prototype got genuinely right: the force law and the integrator.

**What is spec-conformant and should be preserved verbatim:**
* `ε_soft` — the code's `SOFTENING_SQ = 1.0e12 m²` is exactly the locked `ε_soft = 1×10⁶ m` (§3.2). Correct to the digit.
* The force law `a_i = Σ G·m_j·r_ij/(r_ij²+ε²)^{3/2}` (`GravitySandboxEngine.kt:246–281`) is precisely the Plummer-softened form of §3.4, all pairs, reciprocal accumulation.
* The integrator (`:284–344`) is standard velocity Verlet, **algebraically identical to the spec's KDK listing** in §3.5.
* `physics`-layer purity: `GravitySandboxEngine.kt` and `GravityTeachingEngine.kt` import only `kotlin.math` — **zero `android.*`**, satisfying the §3.3 binding rule and the guardrail.
* Merge math: `m′=m₁+m₂`, `v′=(m₁v₁+m₂v₂)/m′`, `pos′=COM`, `r′=(r₁³+r₂³)^⅓` — exactly §3.7.
* Trail cadence: one point per rendered frame — exactly the §3.9 / Auditor-B17 rationale.

I re-ran the spec's own acceptance tests against the prototype's math at `DT = 3600 s`:

```
Test 1  twoBodyCircularOrbitClosesAfterOnePeriod   8765 steps, r error 0.0000%   PASS
Test 2  totalEnergyDriftBounded (10 orbits)        |dE/E| = 5.70e-14 (< 1e-3)    PASS
Test 3  totalMomentumConservedInFreeFlight         rel err 6.79e-16              PASS
Test 6  eccentricOrbitPeriodMatchesKepler (e=0.5)  0.0110% (< 1%)                PASS
Test 35 softeningDoesNotDistortKnownOrbits         1.02e-5 @ Moon r              PASS
```

**The physics core is not the problem. Everything the spec builds around it is.**

### 0.1 What is categorically absent

Structure-of-arrays · `EngineConstants` charter · fixed `DT` · accumulator · `MAX_SUBSTEPS = 96` · Adaptive Safety Refinement · `clampVelocity` (1000 km/s) · `MAX_BODIES = 20` · the 10 `SimEvent` types · NaN rollback + quarantine · `StateFlow<SimSnapshot>` · `withFrameNanos` frame loop · `TabletopCanvas` + tabletop visual language + light theme · marble rendering (shadow/gradient/rim/specular) · the §3.6a dp size table · type-based collision radius · drag/throw/long-press/inspector-editing/orbit-helper/duplicate · slingshot + test-particle preview · `TEST_MARBLE` · **wormholes in their entirety** · black-hole capture radius · self-consistent `r_s` display · BH+BH survivor rule · marble bounce · 5 of 7 detectors · 6 of 8 POE challenges · the 18-term glossary · Persian-digit setting · save/restore + DataStore · lifecycle handling · all 40 named tests · the 16-item manual checklist.

### 0.2 Corrections to my v1 audit (the spec overruled me)

| v1 said | Spec says | v2 verdict |
|---|---|---|
| "Mercury, Venus, Mars, Saturn, Uranus, Neptune cannot be added → **MISSING**" | §3.3 `BodyType` = 7 **generic** types (SUN, PLANET, MOON, ASTEROID, TEST_MARBLE, BLACK_HOLE, WORMHOLE_MOUTH); §3.11 inspector sets mass by *range*, not by name. Named planets are preset content only. | **v1 was wrong.** A named-planet catalog is **not a requirement**. The real catalog defect is different: the type *set* is wrong (missing `TEST_MARBLE`, `WORMHOLE_MOUTH`; extra `STAR`/`GAS_GIANT`/`TERRESTRIAL_PLANET`/`COMET`). |
| "Velocity vectors **IMPLEMENTED**; barycenter marker **IMPLEMENTED**" | §3.11 HUD is exhaustive: *"play/pause · ⅒× ¼× 1× 4× 16× · reset · trails · teaching · theme · language · add. **Nothing else persistent.**"* §3.9 lists no vector or barycenter overlays. | **Credit withdrawn.** These are **scope expansions**, not satisfied requirements. Same for the force-vector overlay, the COM marker, the diagnostics HUD, the reference grid, the preset chip bar, the Academy and Discovery buttons. |
| "Pan is 30× oversensitive → **P2 quality issue**" | §3.4: *"**No zoom in v1.**"* Scene scale is fixed at 3 AU / 400 dp. | **Escalated to a spec deviation.** The pan/zoom system should not exist; its bug is moot. |

---

## PART II — LOCKED DECISIONS (19)

| # | Locked decision | Implementation | Verdict |
|---|---|---|---|
| 1 | Single-module Compose; **pure-Kotlin physics core (zero `android.*`, JVM-testable)** → ViewModel frame loop → UI; education a decoupled observer | Engine + teaching are pure Kotlin ✓ and JVM-testable ✓. But there is **no ViewModel frame loop** — the loop is a `LaunchedEffect` inside the composable (`GravitySandboxScreen.kt:119`), and the "observer" is instantiated in the composable (`:86`) and fed live mutable objects, not snapshots. | **PART** |
| 2 | **SoA**: fixed-capacity(20) `DoubleArray` ×8, `LongArray` ids, `ByteArray` types, `BooleanArray` flags | Array-of-objects: `mutableStateListOf<CelestialBody>` of a `data class` with mutable boxed fields + `MutableList<Pair<Double,Double>>` trails. | **MISS** |
| 3 | Real SI + `Double` in the engine; `Float` only at the draw boundary; pacing via compression constant, **never unit scaling** | `Double` throughout ✓; `Float` conversion at draw ✓. But pacing is done by **scaling `dt`** (`:130`), which is the forbidden mechanism. | **PART** |
| 4 | Exact pairwise Newtonian O(n²), n ≤ 20 (190 pairs), Plummer ε = 1×10⁶ m, **all bodies attract all bodies** | Force law and ε exact ✓. n is **uncapped** ✗. `isFixed = true` bodies (`GravitySandboxEngine.kt:724` Sun, `:809` black hole) attract but never respond ✗ — in the Lagrange preset spec **test 4 `earthPerturbsSun` would fail** because the Sun's acceleration is forced to zero. | **PART** |
| 5 | Velocity Verlet (KDK), 1 force-eval/step, synchronized; `computeAccelerations()` re-run after **every mutating intent** | Integrator correct and equivalent to KDK ✓. But **2 force evals per substep** — the top-of-loop call at `:297` is redundant after the first substep (~33% waste) ✗. No recompute on mutation ✗ (spec test 39). | **PART** |
| 6 | **Fixed `DT` = 3600 s + accumulator**; `BASE` = 10⁶ sim-s/real-s; speeds **0.1/0.25/1/4/16×** multiply BASE, **never DT**; `MAX_SUBSTEPS = 96` | `dt = baseDt(scenario) × speedSlider`, `substepCount = 10`, `delay(16)` (`:121–159`). No DT, no BASE, no accumulator, no cap; speeds are a **continuous 0.1–10× slider**. This is a direct hit on the guardrail *"MUST NOT scale `DT` for speed changes."* | **MISS + guardrail violation** |
| 7 | Circle–circle O(n²); merge by default (mass/momentum/volume-conserving; survivor = larger mass, tie → lower slot; hole beats non-hole); opt-in marble bounce e = 0.4; BH+BH deterministic | Merge math exact ✓. Threshold is `0.85·(r₁+r₂)` not `r₁+r₂` ✗. Radius source is *physical*, spec says *visual/scene* ✗. No tie-break ✗, no hole-beats-non-hole ✗, no bounce ✗, no BH+BH rule ✗. | **PART** |
| 8 | Compose Canvas only; single composable; imperative draw; **draw-phase-only state reads**; **zero draw-lambda allocation**; canvas pinned LTR | Canvas ✓, imperative ✓. State reads are **recomposition-driven, not draw-phase** ✗ (root cause of the freeze bug, §L-1). `Path()` allocated per body per frame ✗. No `LocalLayoutDirection provides Ltr` ✗. | **BROKEN** |
| 9 | Single-threaded inside `withFrameNanos` on the main dispatcher | Main dispatcher ✓, single-threaded ✓, but `delay(16)` instead of `withFrameNanos` ✗ — not vsync-aligned, no frame delta. | **PART** |
| 10 | `StateFlow<SimSnapshot>` state bridge | None. UI reads live mutable engine objects. | **MISS** |
| 11 | App's existing default font; no font deps; no typography changes | Uses `RedTypographyTokens` throughout; adds nothing. | **IMP** |
| 12 | BH: Newtonian point mass, **default 5 M_SUN, cap 50 M_SUN, ring 10–20 dp default 14**; **capture radius = displayed ring radius (one shared constant)**; self-consistent `r_s` | Newtonian ✓ / no special force code ✓. Default `1.0e32 kg ≈ 50 M_SUN` = the *cap*, not the default ✗; no 1–50 range ✗; no ring dp ✗; **no capture radius** ✗; **`r_s` never computed** ✗. | **PART** |
| 13 | Wormhole: paired massless mouths; velocity-preserving teleport; **dual cooldown (spatial 1.5×ring AND 5×10⁵ sim-s)**; always labeled theoretical | Nothing exists. One unreachable teaching card. | **MISS** |
| 14 | Persian-first 3-tier cards + **8 POE challenges** + prediction micro-prompts; **decoupled detectors** | 3-tier ✓ (different tier naming), Persian-first ✓, 4 experiments (not 8) ✗, no micro-prompt strip ✗, detectors coupled to live objects ✗. | **PART** |
| 15 | Zero physics/rendering/font engines; AndroidX + stdlib + kotlinx.serialization + DataStore | No forbidden engine is present ✓. serialization/DataStore unused because persistence doesn't exist. | **IMP** |
| 16 | 3 integrity layers: softening → snapshot rollback (arrays + simTime + failed-step events) → quarantine | Layer 1 only. No NaN scan, no rollback, no `simTime` at all, no quarantine. | **PART (1 of 3)** |
| 17 | Engine hard cap **1000 km/s** + UI guidance `min(2×v_esc, 1000 km/s)` + Adaptive Safety Refinement depth ≤ 3 | None of the three. | **MISS** |
| 18 | **40 automated JVM tests + 16-item manual checklist** | 8 tests, none matching a spec name; **CI runs none of them** (`.github/workflows/build.yml` = `assembleDebug` only). | **MISS** |
| 19 | 11 phases, each gated on owner approval | Not followed; a monolith was produced instead. | **MISS** |

---

## §3.2 CONSTANTS CHARTER — spec test 31 `constantsAreExactSI`

| Constant | Spec | Code | ✓/✗ |
|---|---|---|---|
| G | 6.67430×10⁻¹¹ | `6.67430e-11` (`:126`) | ✓ |
| c | 2.99792458×10⁸ | **absent** | ✗ |
| M_SUN | 1.989×10³⁰ | `1.989e30` (`:456`) | ✓ |
| **R_SUN** | **6.957×10⁸** | **`6.963e8`** (`:461`, `:718`) | **✗ wrong** |
| M_EARTH / R_EARTH | 5.972×10²⁴ / 6.371×10⁶ | `5.972e24` / `6.371e6` | ✓ |
| **M_MOON** | **7.348×10²²** | **`7.342e22`** (`:561`) | **✗ wrong** |
| R_MOON | 1.737×10⁶ | `1.737e6` | ✓ |
| AU | 1.496×10¹¹ | `1.496e11` | ✓ |
| r_s(1 M_SUN) = 2954.1 m | required, self-consistent | never computed | ✗ |
| **ε_soft** | **1×10⁶ m** | **`SOFTENING_SQ = 1.0e12` m²** | **✓ exact** |
| DT | 3600 s | absent (scenario dt × slider) | ✗ |
| BASE | 10⁶ sim-s/real-s | absent (≈8.64×10⁵ by accident at 1×) | ✗ |
| MAX_BODIES / MAX_SUBSTEPS | 20 / 96 | absent / 10 | ✗ |
| Scene scale | 3 AU per 400 dp → metersPerDp 1.122×10⁹, **derived not stored** | 4 hardcoded per-scenario values 8×10⁸…8×10⁹ (`:1596`) | ✗ |
| Velocity hard cap | 1000 km/s | absent | ✗ |

**Spec test 31 fails on 8 of 15 rows.** Two are silent physical-data errors (R_SUN, M_MOON) that no current test would catch.

---

## §3.6a THE dp SIZE TABLE — and the definitive answer to the Earth/Moon question

This is the part of the spec that resolves the v1 "Moon is invisible" finding, and the prototype implements **none** of it.

| Type | dp range | Default | → scene metres (×1.122×10⁹) |
|---|---|---|---|
| ASTEROID | 3–6 | 4 | 4.488×10⁹ |
| TEST_MARBLE | 4–7 | 5 | 5.610×10⁹ |
| MOON | 5–8 | 6 | 6.732×10⁹ |
| PLANET | 8–16 | 10 | 1.122×10¹⁰ |
| SUN | 20–32 | 26 | 2.917×10¹⁰ |
| BLACK_HOLE ring | 10–20 | 14 | 1.571×10¹⁰ |

The spec's mechanism is a **four-part rule**, and every part is missing from the code:

1. **Size is a per-type dp value chosen by the user** within the table's bounds (§3.11 inspector: *"visual size slider bounded by the §3.6a dp table (real radius info-line)"*). It is **not** derived from mass.
   → Code: `visualRadius = 8dp × clamp(log10(m/1e20), 0.6, 2.5) × clamp(zoom,0.5,2)` (`:456`), which **saturates at 2.5 for every mass ≥ 3.2×10²² kg**, so Sun = Mercury = Venus = Earth = Moon = Mars = **20 dp**. Not a size law — a two-value step function.
2. **Collision radius = visual radius in scene metres** (§3.4, spec test 22). Bodies are deliberately "fat" marbles: a default PLANET's collision radius is 1.122×10¹⁰ m ≈ 1 761 × the real Earth radius.
   → Code: collisions use **real physical radii** (6.371×10⁶ m for Earth), 26–50× smaller than what is drawn. This is the exact inversion of the rule.
3. **Fixed scene scale, no zoom in v1** (§3.2, §3.4): 3 AU across 400 dp.
   → Code: 4 per-scenario scales plus a 0.1–30× pinch zoom (`:360`) — a forbidden feature.
4. **Preset distances and radii are explicitly not to scale**, stated once clearly (§3.1 honesty requirement), while *masses, G and velocities are real* so emergent periods and escape speeds are genuinely correct.
   → Code: presets use **true** distances (Earth–Moon = 3.844×10⁸ m) with **no** honesty note anywhere.

**Consequences I computed from the spec's own numbers:**
* Minimum legal contact = 3 dp + 4 dp = **7.854×10⁹ m** (matches §3.6a exactly).
* Earth (10 dp) + Moon (6 dp) contact = 16 dp = **1.795×10¹⁰ m** — which is **46.7× larger than the real Earth–Moon distance**. A to-scale Earth–Moon preset is therefore *physically impossible* under the spec's own collision rule: the pair would merge on frame 1. This is precisely why §3.1 mandates non-to-scale preset distances.
* A Moon placed at the minimum legal 16 dp with a real Earth mass: v = √(GM⊕/r) = **149 m/s**, T = 7.57×10⁸ s ≈ 24 yr → at BASE = 10⁶ that is **757 s of real time at 1× and 47 s at 16×** — observable only at high speed. Worth confirming with the owner when Phase 4/6 lands, but the spec is self-consistent.
* By contrast Sun–Earth is ideal: 1 AU = **133.3 dp** of a 400 dp viewport, period **31.6 s at 1×** — exactly the figure in §3.2.

**Verdict on §3.6a: MISSING in full.** Nothing in the code references dp-based sizing, and the collision-radius identity (spec test 22) is inverted.

---

## §3.4–3.8 PHYSICS, INTEGRATOR, TIME, COLLISIONS, INTEGRITY

| Requirement | Evidence | Verdict |
|---|---|---|
| `a_i = Σ G·m_j·r_ij/(r_ij²+ε²)^{3/2}`, all 190 pairs | `GravitySandboxEngine.kt:246–281` | **IMP** |
| Earth pulls the Sun (test 4) | verified numerically: a☉ = 2.728×10⁻⁸ m/s² — **but zero when `isFixed`** | **PART** |
| Moon pulls both (test 5) | verified: a_moon 8.599×10⁻³, a_earth 5.899×10⁻³ m/s² | **IMP** |
| Massless bodies exert no gravity, fully feel it | falls out of `m_j = 0` ✓ — but no `TEST_MARBLE`/`WORMHOLE_MOUTH` type exists to be massless | **PART** |
| Modeling-assumptions box surfaced to the user | nowhere in the UI | **MISS** |
| Derived physics: circular speed, escape speed, ε_orb, eccentricity vector | ε_orb appears once inside the escape detector (`GravityTeachingEngine.kt:326`); the rest absent (no orbit helper, no eccentricity) | **PART** |
| Velocity Verlet, deterministic, time-reversible | `:284–344`, equivalent to §3.5 KDK | **IMP** |
| `computeAccelerations()` after every mutating intent (test 39) | never called on add/remove/delete; force arrows are stale while paused | **MISS** |
| Accumulator per §3.6b + debt discard | absent | **MISS** |
| Speed set 0.1/0.25/1/4/16× multiplying BASE | continuous 0.1–10× slider multiplying `dt`; also re-keys the `LaunchedEffect` (`:119`) so the physics coroutine is destroyed and recreated on every slider pixel | **BROKEN** |
| `MAX_SUBSTEPS = 96` counting refined inner steps | fixed 10, uncounted | **MISS** |
| Adaptive Safety Refinement (trigger s > 0.2·separation, depth ≤ 3) | absent | **MISS** |
| Collision detection `dist < r_i + r_j` per substep | `dist < 0.85·(r₁+r₂)` (`:358`) — an undocumented 15% shrink | **BROKEN** |
| Merge math (mass/momentum/volume/COM) | exact | **IMP** |
| Survivor: larger mass; tie → lower slot; hole beats non-hole; BH+BH deterministic | larger mass only (`:361`); no tie-break; a PLANET can absorb a BLACK_HOLE and stay a planet | **PART** |
| Marble bounce, e = 0.4, impulse `j = −(1+e)vₙ/(1/m₁+1/m₂)` + 0.8× positional split | absent | **MISS** |
| `Any + WORMHOLE_MOUTH` → teleport; `MOUTH + MOUTH` → never interact | absent | **MISS** |
| Engine `clampVelocity()` 1000 km/s | absent | **MISS** |
| UI guidance `min(2×v_esc_local, 1000 km/s)` | absent (spawn drag is an unbounded `drag_px × 150 m/s`, `:344`) | **MISS** |
| Layer 2: NaN/Inf scan → rollback of arrays **and** `simTime`, discard failed-step events, 3 strikes → auto-pause | absent; there is no `simTime` and no event list | **MISS** |
| Layer 3: quarantine repeat offenders | absent | **MISS** |
| No Barnes-Hut, no CCD, no debris spawning | correctly absent | **IMP** |

---

## §3.9 RENDERING

| Requirement | Code | Verdict |
|---|---|---|
| One `TabletopCanvas`, chrome above it | one `Canvas` inside a 1 610-line screen; chrome interleaved | **PART** |
| **Draw-phase-only state reads, zero recomposition/frame** | inverted — see §L-1 | **BROKEN** |
| **Zero draw-lambda allocation**; preallocated Brush/Path/PathEffect/TextMeasurer; `drawWithCache` statics | `Path()` per body per frame (`:436`); grid redrawn every frame; nothing cached | **MISS** |
| Marbles: shadow → radial-gradient base → optional rim → restrained specular → selection ring → cached label | flat `drawCircle` fills; `Brush` is imported and **never used**; zero gradients, zero shadows, no labels | **MISS** |
| Soft radial-gradient ellipse shadows, never `setShadowLayer` | no shadows at all | **MISS** |
| Canvas pinned LTR via `CompositionLocalProvider(LocalLayoutDirection provides Ltr)` | absent | **MISS** |
| `Double→Float` exactly once at the draw boundary | conversions scattered through the draw lambda but only at draw | **PART** |
| Trails: preallocated **ring buffers, 240 points**, 1/frame, low-alpha stroked, 2–3 stepped-alpha segments | `ArrayList` with `removeAt(0)`, **150 points**, 1/frame ✓, single uniform 45% alpha | **PART** |
| Slingshot preview: test-particle through the frozen field, ≤600 steps, dotted, labeled «پیش‌نمایش تقریبی» when the launched mass > 1% of system mass | no preview of any kind — one straight orange line | **MISS** |
| Dark tabletop `#16181D → #1C1F26`, top-light vignette | `#070B14` space-black + a `#1E293B` grid + crosshair | **BROKEN** |
| Light tabletop `#F4F1EA → #E9E4D9`, faint grain vignette | none — 38 hardcoded dark literals; dialogs pinned `#0F172A` + white text | **MISS** |
| Accent brass `#D4A853` (dark) / teal `#2F6B63` (light) | `RedTheme.colors.accentRed` + assorted ad-hoc hexes | **MISS** |
| Body palette: Sun ivory-amber, Earth slate-blue, Moon bone, asteroid warm grey, marble porcelain | saturated gold `#FFD700`, `#4A90E2`, `#D0D0D0`, rust `#E55D42`, neon `#00E676`/`#FF1744`/`#2979FF` | **MISS** |
| Black hole: matte near-black `#0A0A0C`, one thin accent ring, faint static halo | pure `Color.Black` + a white 20%-alpha halo at 1.8× a mass-derived radius | **PART** |
| Wormhole: two flat rings, paired warm/cool accents, synchronized pulse | absent | **MISS** |
| **Forbidden**: stars, nebulae, screen shake, particle explosions, accretion renderers, lensing, videogame anything | none of the forbidden effects are present ✓ — **but** the space-black + grid + crosshair reads as "space", which §3.9 explicitly rejects ("precision instrument on a desk," never space) | **PART** |
| Not in the spec, present in the code | velocity-vector overlay, force-vector overlay, centre-of-mass marker, reference grid, coordinate crosshair, diagnostics HUD, preset chip row | **SCOPE EXPANSION** |

---

## §3.11 INTERACTION MODEL

| Spec gesture | Spec action | Code | Verdict |
|---|---|---|---|
| Tap body / empty | select (ring **+ haptic**) / deselect | select ✓, deselect ✓, **no haptic**; selection immediately opens a blocking `AlertDialog` (`:960`) covering the canvas | **PART** |
| Drag body | **kinematic while held (skips integration)**, velocity from pointer history, release carries the throw, `computeAccelerations()` on every drag update and on release | **absent** — `detectDragGestures` exists only inside `if (isSpawnModeActive)` and only creates new bodies | **MISS** |
| Drag outward from body, hold | slingshot: drag vector = −velocity, **live dotted test-particle preview**, release launches, UI cap `min(2×v_esc, 1000 km/s)` | a mode toggle + drag that spawns a *new* generic 1×10²⁴ kg body launched **along** the drag (not −velocity), no preview, no cap | **BROKEN** |
| Long-press body | Inspect · Duplicate · Remove | **absent** — zero `onLongPress` in the file | **MISS** |
| Long-press empty / HUD "+" | add catalog with 20-cap notice «۲۰ جسم بیشتر جا نمی‌شود — زمین آزمایش پر است» | a toolbar button opening a create-dialog; **no cap, no notice**; every body spawns at the same hardcoded `(0, 1e11)` with `v = (−25 000, 0)` and `radius = 6.0e6` regardless of type (`:1497–1501`) | **PART** |
| Inspector: **mass log-slider** (1 = جرم زمین, kg secondary; planet 0.01–100 M⊕ · star 0.1–10 M☉ · BH 1–50 M☉ · asteroid/marble ≤ 10⁻³ M⊕) | read-only `String.format("%.3e")` text | **MISS** |
| Inspector: **visual size slider** bounded by the dp table, real-radius info line | radius is never shown, never editable | **MISS** |
| Inspector: **velocity readout + direction dial** | speed magnitude as text only; no components, no direction, no dial | **MISS** |
| Inspector: **orbit helper** «مدار دایره‌ای کن» = √(GM/r) ⊥ dominant attractor | absent | **MISS** |
| Inspector: **prediction micro-prompt strip** «پیش‌بینی‌ات چیست؟», teaching-mode only, once per property per session, never blocking | absent | **MISS** |
| HUD: play/pause · ⅒× ¼× 1× 4× 16× · reset · trails · teaching · theme · language · add — **nothing else persistent** | play/pause ✓ · **continuous slider** instead of 5 discrete speeds ✗ · reset ✓ · trails ✓ · teaching ✓ · **no theme toggle** ✗ · **no language toggle** ✗ · add ✓ · **plus 7 unspecified persistent controls** ✗ | **PART + scope expansion** |
| Speed labeled honestly «سرعت زمان»; G never user-adjustable | label is «سرعت شبیه‌سازی» (simulation speed) — close but not the locked string; G is not adjustable ✓ | **PART** |

---

## §3.12 BLACK HOLE

| Requirement | Code | Verdict |
|---|---|---|
| Newtonian point mass in the exact N-body system, **no special force code** | ✓ — and no lensing, no accretion renderer, no GR anywhere | **IMP** |
| Default 5 M☉ | `1.0e32 kg` ≈ 50 M☉ (`:1507`) — the spec's **cap** used as the default; preset uses 4×10⁶ M☉ (`:795`), far outside 1–50 | **BROKEN** |
| Slider 1–50 M☉ | generic 0.1–10× multiplier → 5–500 M☉ | **BROKEN** |
| Ring 10–20 dp, default 14 | no dp concept; halo = 1.8 × a mass-log radius | **MISS** |
| **Capture radius = displayed ring radius, one shared constant** (test 22) | no capture radius; generic merge on physical radius, which for a user-created BH is 6.0×10⁶ m — **≈2 600× smaller** than the spec's 1.571×10¹⁰ m ring | **MISS** |
| Self-consistent `r_s = 2GM/c²` in the inspector (5 M☉ → «افق رویداد واقعی: ≈ ۱۴٫۸ کیلومتر») | never computed; `r_s` appears only as a *string* in a teaching card | **MISS** |
| BH always survives over a non-hole; BH+BH deterministic + `BodyMerged(BH_BH)` | no rules, no events | **MISS** |
| Teaching card: Newtonian everywhere, horizon is a GR concept, ring is a display convention, tier-3 notes GW radiation is not computed | card exists and is honest about "not a magic vacuum", but **does not** state the ring is a display convention and **does not** carry the GW caveat; it fires merely because a BH exists (`GravityTeachingEngine.kt:365`) | **PART** |
| Visual: matte near-black disk, one thin accent ring, faint static halo, optional shimmer | `Color.Black` + white 20% halo | **PART** |
| **No lensing of any kind** | ✓ compliant — nothing to remove | **IMP** |

---

## §3.13 WORMHOLE

Every row is **MISSING**: paired mouths · massless · no gravity · centre-entry teleport · exit just outside the partner along entry direction · velocity preserved · dual cooldown (spatial 1.5×ring **and** 5×10⁵ sim-s) · «کرم‌چاله (فرضی)» labeling on the object and everywhere · tier-3 Einstein–Rosen card.

The **only** artefact is `GravityTeachingCatalog.MOMENT_WORMHOLE` (`GravityTeachingEngine.kt:172–190`). Its copy would satisfy the honesty requirement — *"no traversable wormhole has ever been observed; known solutions require exotic matter"* — but the string is unreachable: the sole references in the entire repository are its declaration and `GravityTeachingEngineTest.kt:82`.

**0% implemented, with a passing test asserting otherwise.**

---

## §3.14 EDUCATION

| Requirement | Code | Verdict |
|---|---|---|
| 3-tier cards «چه اتفاقی افتاد؟» → «چرا؟» → «بیشتر بدانیم» | 3 tiers exist (`ExplanationLevel`, `ThreeLevelExplanationModal:1075`) but named/labelled differently ("سطح ۱: ساده / سطح ۲: مکانیزم / سطح ۳: علمی") and entered through a "چرا؟" button rather than the locked progression | **PART** |
| Opt-in, never blocking | the teaching toggle exists ✓ and the card is dismissible ✓, but it **hides the diagnostics HUD** while shown (`:722`) | **PART** |
| Tone: friendly, intelligent, beautiful; never childish, never stiff | genuinely good Persian: «جرم را زیاد کردی؛ حالا گرانش حرف بیشتری برای گفتن دارد.» | **IMP** |
| **7 detectors** — OrbitStabilized (ε_orb<0 ∧ ≥300° sweep) · BodyEscaped · BodyMerged · BlackHoleCapture · WormholeTraversal · OrbitDecayed · TwoBodyDance | **2 of 7**, both flawed: "merged" is inferred from `bodies.size` shrinking, so **deleting a body shows a "Collision!" card**; "escaped" adds a hardcoded `r > 2.0e11 m` gate that is meaningless outside the solar preset. The other 5 do not exist. Two extra non-spec detectors (mass-change, BH-exists) fire instead. | **PART/BROKEN** |
| Detectors are **pure functions over snapshots** | `GravityTeachingObserver` is a stateful class fed live mutable objects | **PART** |
| **≥2 s hysteresis** | a 6 s global cooldown + per-concept caps; no per-detector hysteresis band, so a body oscillating around the escape threshold re-fires until the cap is spent | **PART** |
| **8 POE challenges**: mass-doubling · distance-vs-period · escape velocity · binary barycenter · collision momentum · black-hole capture · wormhole traversal · **"why doesn't the Moon fall?" (first-run tutorial)** | 4 experiments exist, mapping to only 2 of the 8 (mass-doubling, binary barycenter). **6 missing**, including the first-run tutorial. Worse: `exp_double_earth_mass` instructs *"double Earth's mass using the inspector or mass slider"* and `exp_move_closer` instructs *"move Mercury or Venus closer to the Sun"* — **neither control exists**; `exp_earth_moon_barycenter` instructs a zoom that is geometrically impossible. | **BROKEN** |
| POE resolution (predict → observe → explain) | `ExperimentOption.isCorrect` is declared (`:37`) and read **only by a unit test**. You can submit with nothing selected; nothing is ever scored. | **BROKEN** |
| Prediction micro-prompts on property edits | absent (no property edits exist) | **MISS** |
| **Persian digits ۰–۹ default + Latin fallback setting** | 3 of ~10 numeric sites converted (`:752`, `:906`, `:1609`); mass/position/speed use raw `String.format` **without an explicit `Locale`** (`:996–1008`), so on an `fa-IR` device the JDK will format inconsistently. No fallback setting. | **PART** |
| **Locked 18-term glossary** (کرم‌چاله · افق رویداد · سیاه‌چاله · ادغام · برخورد · انرژی جنبشی/پتانسیل · تکانه/پایستگی تکانه · سرعت گریز · سرعت · شعاع · جرم · مدار · گرانش · آزمایش · پیش‌بینی · شبیه‌سازی · جسم آزمایشی · سیارک) | **absent entirely** | **MISS** |
| Content in `TeachingCatalog` (fa/en maps); chrome strings in `res/values` + `values-en` | content is in Kotlin `data class`es ✓-ish, but **all chrome strings are inline `if (isFa) "…" else "…"` literals** — none in `res/values` | **PART** |
| Reacts to real simulation events | merges and escapes are read from real state ✓; the other 5 detectors, plus all capture/traversal/stabilization events, do not exist. Guided-lesson content is dead: `activeGuidedLesson` is written at `:1042` and never read; `observationGoalEn/Fa` and `experimentStepsEn/Fa` have **zero** UI references. | **PART** |

---

## §3.15 PERFORMANCE BUDGET

| Target | Reality | Verdict |
|---|---|---|
| Frame total ≤ 16.6 ms | unmeasured; no overlay, no JankStats, no stress preset | **NT** |
| Physics < 1.5 ms (≤96 substeps × 190 pairs) | 10 substeps × **2** force passes = 20 O(n²) passes/frame; at n=20 ≈ 3 800 pair evaluations — comfortably fast, but 1.5× more work than necessary | **PART** |
| Canvas draw ≤ 150 ops + preview < 4 ms | at 20 bodies: ~20 paths + ~60 circles + ~40 lines + ~30 grid lines ≈ **150+ ops** before any preview exists | **PART** |
| **Recomposition / frame = 0** | the HUD subtree recomposes at 60 Hz (`diagnostics` rewritten every tick), running 3 `formatEnergy` → `String.format` calls per frame | **BROKEN** |
| **Draw-lambda allocations = 0** | 20 `Path` objects/frame; plus 20 `DoubleArray`s and 20 boxed `Pair`s per frame in the sim phase | **BROKEN** |
| SoA arrays · `drawWithCache` statics · cached brushes · ring-buffer trails | none of the four | **MISS** |
| 20-body stress preset | none (largest preset = 6 bodies) | **MISS** |
| Lifecycle: backgrounding pauses | **the loop keeps running when backgrounded**; no `repeatOnLifecycle`, no `rememberSaveable` — a rotation destroys the simulation *and* navigates back to the Lab list (`LabScreen.kt:86`) | **MISS** |

### L-1 — The rendering-invalidation defect, now confirmed as a spec violation

Locked decision 8 requires **draw-phase-only state reads** and decision 10 requires a `StateFlow<SimSnapshot>` bridge. The code does neither: body fields are plain `var`s on a `data class`, so mutating them performs **no snapshot write** and cannot invalidate the draw phase. The single per-frame snapshot write is `diagnostics` (`:138`), and the only place it is read inside the draw lambda is:

```kotlin
if (showCenterOfMass && diagnostics.bodyCount > 0) {   // line 523
```

`&&` short-circuits. With the Centre-of-Mass overlay off, no draw-phase snapshot read is registered and nothing else in the draw lambda reads per-frame state — **the canvas should stop repainting while physics keeps running**, with the HUD still ticking (it reads `diagnostics` in composition, in a restart scope that does not enclose the `Canvas`). Today's animation is an accident of that overlay defaulting to `true` — and the overlay is itself a scope expansion the spec doesn't authorise.
*High confidence from static analysis; needs an emulator to be conclusive (no JDK/SDK in this sandbox).* The spec's own architecture is the fix: publish `SimSnapshot` via `StateFlow` and read it only in the draw phase.

---

## §3.16 THE 40 TESTS — projected verdicts

The repository has 8 tests, **none** bearing a spec name, and **CI executes none of them**. Projecting the 40 required tests against the current code:

**Would PASS today (16):** 1 `twoBodyCircularOrbitClosesAfterOnePeriod` · 2 `totalEnergyDriftBounded` · 3 `totalMomentumConservedInFreeFlight` · 4 `earthPerturbsSun` *(only where the Sun isn't `isFixed`)* · 5 `moonAffectsBoth` · 6 `eccentricOrbitPeriodMatchesKepler` · 7 `hyperbolicFlybyEnergyConserved` · 10 `pauseResumeStateIdentical` · 11 `resetRestoresPresetExactly` · 14 `mergeConservesMomentum` · 15 `mergeConservesMass` · 16 `mergeVelocityFormula` · 17 `mergeRadiusVolumeConserving` · 18 `extremeMassRatioAbsorb` · 28 `softeningPreventsSingularity` · 35 `softeningDoesNotDistortKnownOrbits` — *(29 `testParticleExertsNoGravity` would pass by accident: `m_j = 0` works, but no `TEST_MARBLE` type exists to construct)*.

**Would FAIL today (24):** 8 `substepGroupingInvariant` (no fixed DT) · 9 `speedEquivalence16xVs1x` (dt scales — fails by construction) · 12 `deterministicReplay` (no simTime, no hash, `UUID` ids) · 13 `accumulatorNeverExplodes` (no accumulator) · 19–20 bounce · 21–23 black hole (capture / ring=capture / r_s) · 24–25 wormhole · 26 `bodyCapEnforcedAt20` · 27 `nanGuardRollsBack` · 30 `kinematicDragBypassesIntegration` · 31 `constantsAreExactSI` (8 bad rows) · 32 `saveRestoreRoundTripIdentical` · 33 `orbitDetectorFiresOnClosedSweep` · 34 `escapeDetectorFiresOnUnbound` (gate is `r > 2e11`, not ε_orb) · 36 `tightOrbitDoesNotDiverge` (no refinement) · 37 `fastFlybyDoesNotTunnel` (a 1000 km/s marble travels 3.6×10⁹ m per DT vs a user-BH contact radius of 1.2×10⁷ m → **tunnels**) · 38 `velocityBoundsClamped` · 39 `kinematicReleaseUsesFreshForces` · 40 `bhBhMergeDeterministicSurvivor`.

**False-positive tests in the current suite:** `testFigureEightChoreographyStability` (asserts only `size == 3`) · `testTeachingMomentsThreeLevelsOfExplanation` (validates the unreachable wormhole card) · `testInteractiveExperimentsAndPredictionCatalog` (validates `isCorrect`, which the app never reads) · `testPhysicsLessonsCatalog` (validates strings that are never rendered) · `testTeachingObserverCooldownAndDeduplication` (feeds an input the UI cannot produce) · `testEnergyConservationInTwoBodyOrbit` (global energy is insensitive to the Moon subsystem it appears to validate).

### Manual checklist (16 items, Phase 10)

| Item | Status |
|---|---|
| 60 FPS feel @20 bodies + trails | **NT** (no 20-body preset, no cap) |
| Gesture feel | **FAIL** (drag/long-press/slingshot absent) |
| Slider changes don't recompose the canvas | **FAIL** (60 Hz recomposition; §L-1) |
| Persian RTL correct while canvas stays LTR | **FAIL** (no LTR pin) |
| Persian digits | **PARTIAL** (3 sites) |
| Both-theme contrast of every body type | **FAIL** (no light theme) |
| Card readability | PASS (likely) |
| Backgrounding pauses | **FAIL** |
| Rotation preserves experiment | **FAIL** |
| 20-cap message | **FAIL** (no cap) |
| Reset from any state | **PASS** |
| Calm merge feedback | **FAIL** (sub-pixel, silent) |
| Trajectory preview tracks finger | **FAIL** (no preview) |
| Honest speed labels | **PARTIAL** |
| Black hole visible on light theme | **FAIL** |
| Velocity caps enforced in slingshot + inspector | **FAIL** |

**2 pass, 1 partial-plus, 12 fail, 1 untestable.**

---

## §3.17 GUARDRAIL COMPLIANCE

| Guardrail | Status |
|---|---|
| MUST NOT import a physics/rendering engine | ✅ compliant |
| MUST NOT introduce Barnes-Hut or spatial trees | ✅ compliant |
| **MUST NOT scale `DT` for speed changes** | ❌ **VIOLATED** (`:130`) |
| MUST NOT run physics on a background thread | ✅ compliant (main dispatcher) |
| **MUST NOT exceed 20 bodies** | ❌ **VIOLATED** (no cap) |
| MUST NOT make G user-adjustable | ✅ compliant |
| MUST NOT expose ε_soft/DT/MAX_SUBSTEPS in the UI | ⚠️ borderline — a continuous 0.1–10× slider effectively exposes `dt` |
| MUST NOT make the simulation 3D | ✅ compliant |
| MUST NOT write shaders/rotation/atmospheres/terminators | ✅ compliant |
| MUST NOT add starfields/shake/debris/particles/lensing | ✅ literally compliant; ⚠️ the space-black + grid background still reads as "space", which §3.9 rejects |
| **MUST NOT expand scope beyond the tabletop concept** | ❌ **VIOLATED** — 7 unspecified persistent HUD controls, a zoom system ("No zoom in v1"), a Physics Academy, a Discovery modal, a preset chip bar, a diagnostics HUD |
| MUST NOT alter §3.2 constants | ❌ **VIOLATED** — R_SUN and M_MOON are wrong |
| MUST NOT use `Float` inside the physics engine | ✅ compliant |
| MUST NOT add/remove/change fonts | ✅ compliant |
| **MUST NOT mirror the physics canvas under RTL** | ⚠️ no LTR pin present; explicit-coordinate draws aren't auto-mirrored today, so the bug is latent rather than active |
| MUST NOT make the slingshot preview a full mutual integration | ✅ vacuously (no preview) |
| MUST keep `physics/` and `edu/detectors/` free of `android.*` | ✅ compliant |
| **MUST keep all 40 tests passing** | ❌ **VIOLATED** (24 would fail; CI runs none) |
| **MUST recompute forces after every mutating intent** | ❌ **VIOLATED** |
| MUST keep Persian-first copy with English secondary | ✅ compliant |
| MUST honour every rendering/visual restraint | ❌ **VIOLATED** |

**8 violations, 2 borderline, 11 compliant.**

---

## PART IV — PHASE COMPLETION

| Phase | Title | Completion | What's actually there |
|---|---|---|---|
| 0 | Foundation & tabletop shell | **0%** | No `com.zig.gravity` package, no ZigTheme, no tabletop background, no `EngineConstants` |
| 1 | Data model, units & state | **10%** | SI + Double ✓; no SoA, no correct BodyType set, no cap |
| 2 | Physics core (headless) | **35%** | Force pass ✓, Verlet ✓, ε ✓; no accumulator, no clamp, no refinement, no event queue |
| 3 | Rendering & time controls | **20%** | A Canvas, trails, selection ring, play/pause/reset; no tabletop, no marbles, no snapshot flow, wrong speed control |
| 4 | Interaction | **8%** | Tap-select and delete only |
| 5 | Collisions & mergers | **50%** | Merge math ✓; wrong threshold and radius source, no survivor rule, no events, no bounce |
| 6 | Presets & persistence | **30%** | 7 presets + deep-copy reset ✓; no autosave, no DataStore, no lifecycle hardening |
| 7 | Black hole | **20%** | A Newtonian BH body; no capture radius, no r_s, wrong mass defaults |
| 8 | Wormhole | **0%** | — |
| 9 | Educational system | **30%** | Good content, 2/7 detectors, 2/8 challenges, no glossary, no micro-prompts |
| 10 | Light theme, perf & QA | **0%** | — |

---

## N. GAP REPORT

### N.1 Correctly implemented
Plummer force law with the exact locked ε · velocity Verlet (KDK-equivalent) · Earth↔Sun↔Moon mutual attraction · merge mass/momentum/volume/COM math · pure-Kotlin JVM-testable physics layer · no forbidden engine, no Barnes-Hut, no GR/lensing/shaders/3D · font policy respected · Persian-first bilingual content with correct terminology · trail cadence rationale · figure-8 choreography correctly non-dimensionalised · spec tests 1/2/3/5/6/7/14–18/28/35 would pass on the existing math.

### N.2 Partially implemented
Architecture layering · SI/Double policy · integrator efficiency and mutation-sync · collision model · black hole · 3-tier cards · detectors · hysteresis · Persian digits · dark theme · trails · HUD · tap-select · add-body flow.

### N.3 Broken
DT scaling for speed · continuous speed slider · collision threshold `0.85×` · collision radius source · mass-log visual size · space-black tabletop · BH default/range/ring · `bodies.size` collision detector (fires on delete) · POE with no scoring · three experiments whose instructions are impossible · slingshot direction and unbounded launch velocity · recomposition-driven rendering (§L-1) · R_SUN and M_MOON constants.

### N.4 Missing
SoA · EngineConstants · accumulator · MAX_SUBSTEPS · refinement · clampVelocity · MAX_BODIES · SimEvent · rollback/quarantine · SimSnapshot/StateFlow · withFrameNanos · dp size table · TabletopCanvas + marble rendering + light theme + LTR pin + zero-alloc draw · drag/throw/long-press/duplicate · full inspector editing · orbit helper · micro-prompts · slingshot preview · TEST_MARBLE · wormholes · BH capture radius/r_s/BH+BH rule · marble bounce · 5 detectors · 6 POE challenges · glossary · save/restore · lifecycle · stress preset · 40 tests · CI test execution.

### N.5 Specification deviations (things to *remove*, not fix)
Pinch zoom (§3.4: "No zoom in v1") · per-scenario scene scales (§3.2: one derived `metersPerDp`) · velocity-vector, force-vector, centre-of-mass and grid overlays (§3.11: "Nothing else persistent") · diagnostics HUD · Physics Academy modal · Discovery modal · preset chip bar · `isFixed` pinned bodies (§3.4: all bodies attract all bodies) · `BodyType.STAR/GAS_GIANT/TERRESTRIAL_PLANET/COMET` (not in the locked enum) · 7 preset scenarios vs the Phase-6 "6-experiment catalog".

### N.6 Critical bugs preventing normal use
1. No editing of any body property — the sandbox cannot be used as a sandbox.
2. Probable canvas freeze when the COM overlay is toggled off (§L-1).
3. All planet-and-larger bodies render at an identical 20 dp.
4. Merges are sub-pixel and silent.
5. Deleting a body triggers a "Collision!" teaching card.
6. Speed slider changes the trajectory and restarts the physics coroutine on every pixel.
7. Rotation destroys the simulation and exits to the Lab list.
8. Selecting a body opens a modal that hides the canvas.
9. Three of four POE experiments instruct impossible actions.
10. A user-created black hole has a 6 000 km capture radius and tunnels at high speed.

### N.7 Recommended implementation order — follow the spec's own phases

The document mandates it (Part IV, "each phase's prompt generated only after owner approval of the previous"), and the sequencing rationale — *units/state before physics; physics before pixels; interaction before collisions; education last-but-largest* — is exactly right for this codebase. **My recommendation: do not patch the prototype. Stand up `com.zig.gravity` per §3.3 and port the four things that are already spec-correct** (force law, Verlet, merge math, Persian content) **into it.** Patching in place would mean fighting the array-of-objects state model, the recomposition-driven renderer and the dt-scaling time model — all three of which are the *cause* of the P0 bugs, and all three of which Phase 0–3 replace outright.

### N.8 Priority table

| Priority | Requirement (spec §) | Current state | Evidence | Required fix |
|---|---|---|---|---|
| **P0** | Package skeleton + `EngineConstants` charter (§3.2, §3.3, Phase 0–1) | 0% | no `com.zig.gravity` | Create the 4-layer structure; port G/ε/Verlet/merge; fix R_SUN → 6.957e8, M_MOON → 7.348e22; add c, DT, BASE, MAX_BODIES, MAX_SUBSTEPS, metersPerDp; write test 31 |
| **P0** | SoA state, capacity 20 (§Locked 2, Phase 1) | array-of-objects | `GravitySandboxEngine.kt:15–50` | 8 `DoubleArray` + `LongArray`/`ByteArray`/`BooleanArray`; test 26 |
| **P0** | Fixed DT + accumulator + 5 discrete speeds + MAX_SUBSTEPS 96 (§3.6b) | dt × slider, `delay(16)` | `:121–159` | `withFrameNanos` accumulator per §3.6b; tests 8, 9, 13 |
| **P0** | `StateFlow<SimSnapshot>` + draw-phase-only reads (§Locked 8/10, §3.15) | recomposition-driven; probable freeze | `:523`, `:138` | ViewModel publishes snapshots; canvas reads only in draw; 0 recomposition/frame |
| **P0** | dp size table + collision radius = visual radius (§3.6a, §3.4) | mass-log size; physical-radius collisions | `:456`, engine `:358` | Type dp defaults/ranges; collision radius from dp × metersPerDp; test 22 |
| **P0** | Tabletop rendering, dark + light, marbles with shadows (§3.9) | space-black + flat circles | `:169`, `:466–487` | `TabletopCanvas`, vignette, gradient marbles, LTR pin, zero-alloc draw |
| **P0** | Interaction model (§3.11, Phase 4) | tap + delete only | no `onLongPress`, no drag | Kinematic drag + throw, long-press menu, full inspector, orbit helper, slingshot + test-particle preview; tests 30, 39 |
| **P0** | Wormholes (§3.13, Phase 8) | 0% | no `WORMHOLE_MOUTH` | Paired massless mouths, teleport, dual cooldown, «(فرضی)» labeling; tests 24, 25 |
| **P1** | Black hole per §3.12 (Phase 7) | wrong defaults, no capture radius, no r_s | `:1507`, `:795` | 5 M☉ default / 1–50 range / 14 dp ring = capture radius; r_s display; BH+BH survivor; tests 21–23, 40 |
| **P1** | `clampVelocity` 1000 km/s + UI guidance (§3.7, Locked 17) | none | — | Engine clamp + `min(2×v_esc, 1000 km/s)`; test 38 |
| **P1** | Adaptive Safety Refinement depth ≤ 3 (§3.6c) | none | — | Trigger `s > 0.2×separation`; tests 36, 37 |
| **P1** | Integrity layers 2–3 (§3.8) | none | — | NaN scan → rollback (arrays + simTime + events) → quarantine; test 27 |
| **P1** | `SimEvent` queue (10 types) (§3.3) | none | — | Emit from engine; drive detectors and visuals |
| **P1** | Collision threshold `r_i + r_j`; survivor rule; marble bounce e = 0.4 (§3.7) | `0.85×`, mass-only, no bounce | engine `:358–362` | Correct threshold, tie → lower slot, hole beats non-hole; tests 19, 20, 40 |
| **P1** | 7 detectors + ≥2 s hysteresis, pure over snapshots (§3.14) | 2 of 7, one misfires on delete | `GravityTeachingEngine.kt:305` | Rebuild in `edu/detectors/`; drive from `SimEvent`; tests 33, 34 |
| **P1** | 8 POE challenges incl. first-run "why doesn't the Moon fall?" + scoring (§3.14) | 4 experiments, none scored | `:1272`, `isCorrect` unused | Author all 8; score against `isCorrect`; add micro-prompt strip |
| **P1** | Save/restore + DataStore + lifecycle + rotation (§3.3, Phase 6) | none; rotation exits the screen | 0 `rememberSaveable` | `SaveState.kt`; `repeatOnLifecycle`; test 32 |
| **P1** | 40 tests + CI execution (§3.16, Locked 18) | 8 tests, 6 false positives, CI runs none | `build.yml` | Write all 40; add `./gradlew test` to CI |
| **P2** | Remove out-of-spec scope | zoom, 7 extra HUD toggles, Academy, Discovery, grid, COM/vector overlays | §3.11 "Nothing else persistent" | Delete or gate behind a debug flag |
| **P2** | Glossary (18 locked terms) + Persian digits everywhere + Latin fallback setting (§3.14) | absent / 3 sites | `:996–1008` | Central formatter; glossary screen |
| **P2** | Honesty note: "preset distances/radii are not to scale" (§3.1) | absent | — | State once, clearly |
| **P2** | Modeling-assumptions box surfaced (§3.4) | absent | — | Add to the teaching layer |
| **P2** | Hoist the redundant force evaluation (§3.5: 1 force-eval/step) | 2 per substep | engine `:297` | Move out of the substep loop |
| **P3** | Haptics on select; cached labels; trail stepped-alpha; BH shimmer; perf overlay + JankStats + 20-body stress preset | absent | §3.9, §3.15 | Phase 10 polish |

---

## Appendix — verification artefacts

Spec numbers independently re-derived (all match the document):
```
metersPerDp = 3 AU / 400 dp                    1.1220e9        (spec 1.122e9)      ✓
minimum legal contact 3 dp + 4 dp              7.8540e9 m      (spec 7.854e9)      ✓
Earth(10dp)+Moon(6dp) contact                  1.7952e10 m  =  46.7 × real Earth–Moon distance
softening distortion (3/2)(ε/r)²               1.500e-4 @1e8 · 1.015e-5 @3.844e8 · 6.702e-11 @1 AU  ✓
1 AU in dp                                     133.3 dp of 400
Sun–Earth period at BASE 1e6                   31.6 s real @1×  (spec 31.6 s)      ✓
Moon at 16 dp, real M⊕                         v = 149 m/s, T = 7.57e8 s = 24 yr → 757 s @1×, 47 s @16×
```
Prototype math run against the spec's acceptance criteria at DT = 3600 s:
```
test 1  r error 0.0000% over 8765 steps      PASS
test 2  |dE/E| = 5.70e-14 over 10 orbits     PASS
test 3  momentum rel err 6.79e-16            PASS
test 6  Kepler period err 0.0110%            PASS
test 35 distortion 1.02e-5 at Moon radius    PASS
```
Prototype scene geometry (why it fails today):
```
Earth–Moon on screen                          0.480 px @ 8e8 m/px, zoom 1
visual radius Sun/Mercury/Venus/Earth/Moon/Mars = 20.00 dp each (log arg clamped to 2.5)
zoom needed to visually separate Earth & Moon = 167×   (hard cap 30×)
Sun+Earth merge separation                    0.747 px
two user-added bodies merge separation        0.013 px
1000 km/s travel per DT                       3.6e9 m  vs user-BH contact 1.2e7 m → tunnels
```
Static checks: `onLongPress` = 0 · `rememberSaveable` = 0 · `Brush`/`radialGradient`/`shadow` in the sandbox = 0 · draw primitives = `drawCircle` ×8, `drawLine` ×9, `drawPath` ×1 · hardcoded `Color(0xFF…)` = 38 · `trailIntervalSubsteps` declared (`engine:288`) and never used · `activeGuidedLesson` written (`:1042`) and never read · `observationGoal*` / `experimentSteps*` never rendered · `MOMENT_WORMHOLE` and `isCorrect` referenced only by tests.

**No code was changed for this audit.** Working-tree changes are limited to `docs/` (this report + the vendored spec) plus the pre-existing `RedTheme.colors` compile fix carried over from the previous session. Note that `origin/arena/01a059b5-lm-arena` is already at `da3dd95` containing that same fix while the local checkout sits at `fd10ed8`.

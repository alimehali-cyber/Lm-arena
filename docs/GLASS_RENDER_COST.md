# The glass budget: what it costs, and which option is actually cheaper

Researched 2026-11-01 against `arena/69725ac8-lm-arena` (Part 6 tip `83ba71d`).

**Short answer.** There is no drop-in library swap available to us today, and the reason is not taste — it is
Compose. Every current liquid-glass library (Haze 2.0.1, the Kyant backdrop 2.x we would upgrade to, Cloudy
1.0.0) is built against **Compose 1.11–1.12**; this app is on **Compose 1.7.2** (BOM `2024.09.00`). The
cheaper option that exists *now* is therefore not a different dependency but a different **use** of the one
we have: stop paying for full optics on the ~85 content cards, and pay for them only where the eye notices.
That is a few hundred lines in `LiquidGlass.kt`, it is reversible, and on the published third-party
measurements below it is worth roughly half the frame cost of the glass.

---

## 1. What we actually run per frame

`io.github.kyant0:backdrop:1.0.6`, read from its source at tag `1.0.6`:

| Stage | Implementation | Cost class |
|---|---|---|
| `LayerBackdrop.drawBackdrop` | `drawLayer(graphicsLayer)` — **replays the recorded display list** into the caller's canvas | O(draw ops in the backdrop), **once per glass surface** |
| `layerBackdrop { }` | `recordLayer(...)` on **every** draw of that node | O(draw ops), once per frame the backdrop invalidates |
| `vibrancy()` | `ColorMatrixColorFilter` | cheap |
| `blur()` | `RenderEffect.createBlurEffect` (API 31+) | GPU blur over the card's area |
| `lens()` | **AGSL `RuntimeShader`** — `RoundedRectRefraction`, or `RoundedRectRefractionWithDispersion` when chromatic aberration is on (API 33+) | the expensive node: one shader pass per pixel of the padded layer, per surface |
| `Highlight` / `Shadow` / `InnerShadow` | three further recorded layers per surface | three layers per surface |

Our app's own numbers:

- **85 `LiquidGlassSurface(` call sites**; 3–10 in view at once depending on the screen (`LabScreen` alone
  lists 4 feature cards, `CompassARScreen` has 19 surfaces), plus the floating nav bar.
- The sampled backdrop contains the **Live Sky `Canvas`**: every catalogue star brighter than magnitude 4.5.
  Measured from `assets/ar/stars_bsc5.bin` (8,404 records): **904 of them qualify**, on top of the landscape,
  the cloud veils and the moon sprites.
- So each glass surface on Home/Lab/Satellite replays up to ~900 star draws plus the scene, then blurs it,
  then runs the refraction shader over it. `LIVE_SKY_FRAME_INTERVAL_MS = 33` means the sky itself can
  re-record ~30×/s.

This is not a theory about our code: it is the same failure mode both libraries have documented.

- **Haze #1611** (Oct 2026, closed): with N effects over one source, "the source is replayed **1 + N times per
  frame**: 1.00 with no effects, 2.00 with one, 2.99 with two and **10.00 with nine**. Performance modes don't
  change the count, because scaling reduces pixels, not display-list ops."
- **Kyant #126** (still **open**): a contributor profiled `backdrop` 2.0.1 on a Galaxy S23 Ultra and filed
  three fixes — shadow/highlight layers re-recorded when nothing changed, the backdrop re-recorded on every
  `onGloballyPositioned`, and **runtime shaders compiled once per glass element instead of once per process**.

So both stacks have the same structural bill; Haze fixed theirs (Sept–Oct 2026), Kyant has not merged the fix.

---

## 2. The candidates, with the evidence that they have users

The bar the user set — "nothing with no users and no feedback" — decides most of this list.

| Library | ★ | Forks | Open issues | Last push | Releases | Licence | Real-world footprint | Compose it needs | Refraction? | Perf knobs |
|---|---:|---:|---:|---|---:|---|---|---|---|---|
| **kyant0/AndroidLiquidGlass** — *ours, 1.0.6* | 4,424 | 358 | **44** | 2026-08-26 | 2.0.1 | Apache-2.0 | 2,984 GitHub files reference `io.github.kyant0:backdrop` | 1.0.6 works on 1.7; **2.x needs 1.12** | yes (AGSL) | none — no scale/mode API |
| **chrisbanes/haze** — 2.0.1 | 2,643 | 89 | **5** | **2026-11-01** | **86** | Apache-2.0 | 10,272 Kotlin files import `dev.chrisbanes.haze` | **1.12** (2.x), 1.8 (1.6.x), 1.7.8 (1.5.4) | yes (`haze-glass`) | `HazePerformanceMode`, `HazeSampling`, `HazeInputSharing` |
| **skydoves/Cloudy** — 1.0.0-alpha01 | 1,292 | 55 | 6 | 2026-09-13 | 60+ | Apache-2.0 | 196 refs | **CMP 1.11** | lens over *own content*; backdrop path is blur-only | CPU fallback ≤ API 30 |
| QWEA0/Liquid-Glass-Android | 273 | 32 | 3 | 2026-09-29 | — | MIT | thin | — | SDF, View system | — |
| Mortd3kay/liquid-glass-android | 187 | 8 | 3 | **2025-12-08** (stale) | — | NOASSERTION | thin | — | AGSL | — |
| ntvien/liquid-glass | **0** | — | 0 | 2026-10-15 | — | Apache-2.0 | none | — | yes | documented benchmarks |
| NadeemIqbal/liquid-glass | **9** | — | 0 | 2026-05-20 | — | Apache-2.0 | none | — | blur only | quality tiers |
| shayann07/liquidglass | **5** | — | 43 | 2026-11-01 | — | Apache-2.0 | none | — | yes | render scale |

Rule-outs, explicitly:

- **ntvien, NadeemIqbal, shayann07** publish the most interesting *benchmarks* in this space — and have 0, 9 and
  5 stars respectively, no external issues, no downstream users. Their **techniques** are worth stealing (see
  §4); their **code** is not something to put in a shipping app. This is exactly the trap the brief warned about.
- **Mortd3kay** is eight months stale with an ambiguous licence (`NOASSERTION`). **QWEA0** is a View-system
  port, not Compose, and too new to trust.
- **Cloudy** is a real library with real users, but its `Modifier.liquidGlass` is a lens over the composable's
  *own* content, and its backdrop path (`rememberSky()` + `cloudy(sky = …)`) is blur only. For our cards that is
  a downgrade in look, and it needs CMP 1.11 anyway.

---

## 3. What the numbers say about "cheaper"

No head-to-head benchmark of Kyant vs Haze exists. What does exist is measured evidence on the same class of
shader work, from both projects and from third parties:

| Measurement | Source | Result |
|---|---|---|
| Haze 2's adaptive resolution vs Haze 1 | Haze 2.0 release notes | **−37 %** CPU frame time (list scrolling), **−15 %** (card dragging) |
| Glass render scale, 0.75 → 0.5 | Haze benchmark, cited in a competitor audit | **−39.7 %** median frame time |
| Glass at 0.7 render scale, Pixel 7, scrolling a list of glass cards | third-party (shayann07 PR #2) | janky frames **8.9 % → 4.3 %**, p50 **26 → 16 ms**, p90 **73 → 18 ms** |
| "Rich" optics vs glass off, Galaxy S23 Ultra, scrolling | third-party (ntvien, Macrobenchmark) | janky **97 % with, 46 % without** |
| Haze Glass, stable source, Pixel 8a | Haze `docs/benchmark-results.md` | P90 CPU frame **4.31 ms** (Balanced), 96 MB peak GPU |
| Platform-native backdrop (`RenderNode.setBackdropRenderEffect`, Android 17 QPR2) vs source-based | Chris Banes, Sept 2026 | native path has **higher** CPU frame times for changing blur and every Glass workload — not a free win |

Two conclusions follow, and they are the whole recommendation:

1. **Resolution is the big lever, not the shader's shape.** Every one of those results is dominated by "run the
   glass graph at fewer pixels". A refraction shader over a 380×140 dp card at full density is ~0.4 MP per
   surface; at 0.7 scale it is ~0.2 MP.
2. **How many surfaces run full optics matters more than anything.** 97 % → 46 % janky by turning the rich look
   off is the single largest published delta in this space.

---

## 4. Recommendation

### Now — two tiers, inside the current dependency (no new library, no Compose upgrade)

Add `LiquidGlassTier` to `LiquidGlassConfig`, defaulted per call site rather than globally:

| Tier | Surfaces | Chain | What it drops |
|---|---|---|---|
| **Hero** | floating nav bar, modals/dialogs, `MoonCard`, `EclipseCard`, Lab/Satellite screen headers | vibrancy → blur → **lens + CA** → highlight → shadows | nothing |
| **Content** | the tool cards, list rows, chips, the other ~70 surfaces | vibrancy → blur → film → sheen → border | the AGSL `lens` pass, and the `Highlight`/`Shadow`/`InnerShadow` layers (we already draw our own sheen and border) |
| **Cheap** | anything below an API-33 device, or a user toggle | flat film + border + sheen | everything sampled |

Implementation notes (all in `LiquidGlass.kt`, no fork needed):

- `lens(...)` is one call in `Modifier.liquidGlass`; gating it on the tier removes the RuntimeShader entirely
  for content cards, including the dispersion variant that only exists for chromatic aberration.
- `Highlight` / `Shadow` / `InnerShadow` are Kotlin lambda parameters of `drawBackdrop`
  (`highlight = { Highlight.Ambient }` etc.) — passing `null` removes those recorded layers, which is exactly
  Kyant #126's item 1 that we cannot get from the library.
- The 30 Hz sky re-record only needs to run while the sky is the subject; a content-tier card is 1–2 dp of blur
  over it. Freezing `LiveSkyFrameRequest` to ~12 Hz *while a scrolling list is under glass* is a two-line change
  in the sampler and invisible at those blur radii.
- Keep `FIRE_BRIGHTNESS_GAIN`, the film presets and the smooth-edge dial exactly as they are — the tier decides
  *which stages run*, not how the material is described.

Expected: removes one AGSL pass per content card plus three layers per card. On the published deltas (§3) that is
the cheap half of the 0.7-scale win without softening any rim we care about, because the cards we soften are not
the ones being looked at.

### Later — migrate to Haze when we take the Compose upgrade

`dev.chrisbanes.haze:haze` + `haze-glass` is the better engineered stack, and it is the one with the feedback
trail: 2,643 ★, 86 releases, 5 open issues, commits today, and **published benchmarks with a reproducible
runbook**. Concretely it gives us what 1.0.6 cannot:

- a **fused renderer** — one capture layer into one retained `RenderEffect` graph, instead of nested layers per
  surface (ADR-0003: profiling showed the cost was layer traversal and driver work, not fragment area);
- **adaptive input scaling** with quality gates and hysteresis (ADR-0004/0005), i.e. the "render at 0.7" trick
  done properly, including the perceptual-difference guard that keeps it under a 0.01 mean-abs-difference;
- `HazeInputSharing.Auto` — one offscreen copy of a source shared by its effects (the #1611 fix, 2.1 snapshot);
- `hazeGlassContainer` — nearby surfaces merge into one shader surface (2.1 head, **not yet released**; must not
  be counted on today).

The blocker is entirely Compose: Haze 2.0.1 is built on Compose 1.12.1 / CMP 1.12.2 and Kotlin 2.4.21; we are on
1.7.2 / Kotlin 2.2.10. That migration (Compose 1.7 → 1.12, Material 3 1.3 → 1.9) is a project in itself and
should not be smuggled in under a glass change.

**Do not**: move to Haze 1.x to stay on Compose 1.7 — glass only exists from 2.0, so that would mean giving up
refraction entirely and adopting a branch in maintenance. Do not adopt any of the 0–9 ★ libraries.

---

## 5. How to prove it before shipping

Nothing in this document is a measurement *of this app*; it is a measurement of the same effects elsewhere. The
honest next step is a number of our own, and it is cheap: an `androidx.benchmark` module with
`FrameTimingMetric`, one release build, three scenarios (Home idle, Home scrolling, Lab scrolling), each run with
`LiquidGlassTier = Hero` (today), `Content` (proposed) and `Cheap`. Perfetto traces of the two runs then confirm
which stage dropped, and `Part6GlassAndInkTest` already pins the optics arithmetic so a tier cannot silently
change the material — a tier test would assert that a content-tier surface contains no `lens(` and no
`Highlight.` call, and that a hero-tier one still does.

If the benchmark says the content tier buys under ~10 %, the honest answer is that our glass is not the
bottleneck and this document should be closed with that result rather than with a refactor.

---

## 6. Sources

Read directly on 2026-11-01:

- `kyant0/AndroidLiquidGlass` @ `1.0.6` — `LayerBackdrop.kt`, `LayerBackdropModifier.kt`, `effects/{Blur,Lens,ColorFilter}.kt`,
  `RuntimeShaderCache.kt`; issues **#126** (open), releases 1.0.6 → 2.0.1.
- `chrisbanes/haze` — `README.md`, `docs/benchmark-results.md`, `docs/adr/0003`, `0004`, `0005`, `0013`,
  `CHANGELOG.md`, issue **#1611** + comments, `gradle/libs.versions.toml` at `main`, `1.7.3`, `1.6.10`, `1.5.4`.
- `skydoves/Cloudy` — `README.md` (effects table, liquid-glass parameters, platform support), `libs.versions.toml`.
- GitHub code search: `dev.chrisbanes.haze` (10,272 files), `io.github.kyant0:backdrop` (2,984), `skydoves:cloudy` (196).
- Third-party measurements: Chris Banes, "Compose has progressive blur. Do you still need Haze?" (Sept 2026);
  `shayann07/liquidglass` PR #2 (render-scale benchmark, Pixel 7); `ntvien/liquid-glass` performance section
  (S23 Ultra, Macrobenchmark). All are measurements of comparable effects, not of this app.
- This repo: `LiquidGlass.kt`, `MainActivity.kt`, `LiveSkyBackdrop.kt`, `LiveSkyFramePipeline.kt`,
  `assets/ar/stars_bsc5.bin` (904 stars ≤ mag 4.5 of 8,404), `gradle/libs.versions.toml`.

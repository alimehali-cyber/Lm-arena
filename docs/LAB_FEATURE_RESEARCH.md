# ZIG's Blackhole & the Lab — feature research and roadmap

*Researched on 2026-11-07 against the working tree (branch `arena/8795cf74-lm-arena`), after the
Gargantua/Chal removal.* The goal: find features that are (a) genuinely cool, (b) physically honest,
(c) cheap to add given what already exists in this repo, and (d) fit the app's offline, bilingual,
three-UI-style world.

Method: I read the whole `com.zig.antar` feature (settings → JSON → WebView bridge → telemetry),
**decoded the embedded WebGL2 engine** (a 222 KB gzipped HTML with ~4,700 lines of JS/GLSL, 129
functions, its own diagnostics, GW chart, recorder and info panel), read all three Lab screens and
the Gravity Sandbox's teaching layer, listed every engine under `astro_engine/` for reuse, and
cross-checked the physics and the "what's new in the field" angle against 2024–2026 literature and
prior art (sources at the end).

---

## 1. TL;DR — if you only do five things

| # | Feature | Why it wins | Effort |
|---|---|---|---|
| 1 | **Un-dormant the engine's own tools** (light-path diagram, GW chart, info panel, screenshot, video) and add a 4K poster mode | They are *already implemented and running* in the bundle; the app hides the entire DOM. Pure surfacing work, huge perceived value per day | S–M |
| 2 | **Real sky lensing** (Milky Way panorama + BSC5 stars, lensed) | Turns "a black hole" into "our sky, bent". The panorama and the 8,404-star catalogue are already in the APK and already licensed | M |
| 3 | **Photon-ring order map + analytic shadow edge** | The photon ring is *the* 2024–2026 black-hole topic (ngEHT plans to detect the n=2 ring). Coloring rays by winding number is cheap per step and looks like a scientific instrument | M |
| 4 | **Sgr A\* flare mode** (orbiting hot spot, 70-min period, ~12 r_g) | Direct tie to the GRAVITY flare results; the lensed secondary image making the centroid loop is the demo nobody else in the astronomy-app space has | M |
| 5 | **Eclipse Lab** (new Lab tool, reuses `EclipseEngine`) | The app already computes eclipse geometry for the whole planet; exposing it as an interactive lab is close to free and is a headline feature in its own right | M |

---

## 2. What ships today

### 2.1 ZIG's Blackhole — engine inventory (from the bundle + Kotlin)

| Area | Already there |
|---|---|
| Spacetime | Kerr in Cartesian Kerr–Schild coordinates, horizon-regular integrator, adaptive step count per quality tier (220 / 320 / 460 / 800 steps, `stepK` 0.11 → 0.042) |
| Objects | Kerr BH · TON 618 quasar (`z = 2.219`, self-consistent Eddington disk, X-ray corona, UV wind, BLR, jet) · binary merger (inspiral → merger → ringdown, GW strain chart, remnant mass/spin/radiated energy) |
| Camera | free orbit, cinematic, **free-fall timelike geodesic** with proper-time, `v/c`, horizon-crossing and Cauchy-horizon readouts, auto tour (10 scripted scenes) |
| Disk | Novikov–Thorne / Page–Thorne thin disk, peak T, outer radius, optical depth, MRI turbulence, coronal haze, thick-torus mode, physical-T-from-ṁ |
| Relativity | Doppler + gravitational shift, bolometric g⁴ lock, lens-map coordinate grid, **EHT 230 GHz VLBI filter**, Blandford–Znajek jet |
| Sky | procedural starfield + galaxy cubemap, brightness controls |
| Image | HDR, ACES tone mapping, bloom, TAA with neighbourhood clamping, adaptive resolution, 4 quality tiers |
| Tools (engine) | equatorial **light-path diagram**, **GW strain chart**, **screenshot**, **video recorder**, **info panel**, HUD, EHT label, plunge HUD, toast |
| Kotlin side | 48 persisted settings, 10 presets, 3 objects, 5 mass scales, gesture controller with inertia, telemetry parser at 220 ms, three UI styles, FA/EN via `AntarStrings` |

### 2.2 The finding: six finished features ship dark

The app loads the page with `?transparent=1&ui=0` and `AntarSimulationPayload` injects

```css
body > :not(#gl) { display: none !important; visibility: hidden !important; … }
```

so **every** DOM element in the bundle — `#diagramBox`, `#gwBox`, `#info`, `#hud`, `#rec`,
`#plungeHud`, `#toast` — is hard-hidden, while Compose draws its own chrome. The engine's own

* equatorial light-path diagram (escape / capture / photon orbit / ergosphere, with live
  `r₊ / r_ph / ISCO` readouts),
* gravitational-wave strain chart with cursor + live `f`, `mf`, `af`,
* "What am I looking at?" explainer (6 sections, EN + TR — needs FA),
* PNG screenshot, canvas video recorder, bundle HUD,

are unreachable from the UI. `drawDiagram()` and `drawGwChart()` even early-return under `hide-ui`,
so nothing is wasted — they are simply dormant. **Surfacing these is the single best value-per-hour
work available in this feature.**

### 2.3 The Lab screen today

Three implementations of the same list, selected by `AppUiStyle` in `MainActivity`:
`LabScreen` (classic flat cards), `CyberLabScreen` (cutout viewport cards + mesh gradients),
`SpatialLabScreen` (glass cards). Each owns its own `if/else` / `when` chain over
`LabFeatureType` (`TIME_DILATION`, `GRAVITY_SANDBOX`, `ANTAR_BLACKHOLE`), its own procedural
viewport art, and its own accent/mesh presets. A new tool currently costs **one enum entry + three
screen edits + three card artworks + one Root composable + FA/EN strings + tests**.

Existing engines that are *not* exposed as Lab tools: `EclipseEngine` + `EclipseVisibility`,
`JupiterMoonsEngine`, `MoonEngine`, `ObservabilityEngine`, `FinderEngine`, `WhatsUpTonightEngine`,
`DeepSkyEngine`, `GalacticEngine`, `MagneticAnomalyDetector`, `CelestialObjectSizes`, plus the
whole `data/catalog` (BSC5 stars, deep-sky, facts).

### 2.4 Physics the app already computes (so new UI can be honest for free)

| Quantity | Formula in `AntarKerrMetrics` | a = 0 | a = 0.94 | a = 0.998 |
|---|---|---|---|---|
| Outer horizon `r₊/M` | `1 + √(1−a²)` | 2.000 | 1.341 | 1.063 |
| Inner horizon `r₋/M` | `1 − √(1−a²)` | 0 | 0.659 | 0.937 |
| prograde ISCO | Bardeen | 6.000 | 2.024 | 1.237 |
| prograde photon orbit | Bardeen | 3.000 | 1.425 | 1.074 |
| radiative efficiency η | `1 − E_ISCO` | 5.7 % | 18.1 % | 32.1 % |

Also there and unused in the UI: Kerr shadow size, Schwarzschild capture radius `b = 3√3 M ≈ 5.196 M`,
Hawking temperature `T_H = 6.17×10⁻⁸ K × (M☉/M)` (Sgr A\* → 1.4×10⁻¹⁴ K), evaporation time
`2.1×10⁶⁷ yr × (M/M☉)³`, S2-type precession, GW quadrupole chirp.

---

## 3. The cost model: four seams, four prices

| Seam | Touch | Price |
|---|---|---|
| **Compose only** | new panel/tab/overlay in `AntarRoot` / `AntarControlPanel`, numbers from `AntarKerrMetrics` or telemetry | 🟢 S — no bundle change, no perf risk |
| **Bridge extension** | new field in `AntarSimulationSettings` → `buildSettingsJson` / `toInitialQueryString` → `applySettings`, or a new `@JavascriptInterface` method | 🟡 S–M — mechanical, tests exist for round-trip |
| **Bundle edit** | decode base64 → gunzip → edit HTML/JS/GLSL → re-gzip → re-encode (the same pipeline used to purge "Gargantua") | 🟠 M — must keep the **14 exact-string anchors** in `applyMobileWebViewFixes()` intact (`check()` throws otherwise) and keep `AntarPayloadAndIntegrationTest` green |
| **Metric change** | new spacetime (Kerr–Newman, Taub–NUT, wormhole) | 🔴 L–XL — the integrator, the disk, the diagnostics and the presets all assume Kerr–Schild Kerr |

Two structural notes that shape every proposal below:

1. **No asset loader.** The page is loaded with `loadDataWithBaseURL` at a fake
   `https://appassets.androidplatform.net/` origin and there is no `WebViewAssetLoader` /
   `shouldInterceptRequest`. Nothing in the bundle can `fetch()` an APK asset today. Options: add
   `androidx.webkit`, or push data through the bridge as a data URL (recommended: no new
   dependency, one-shot, and already the established bridge pattern).
2. **Budget.** ULTRA is 800 steps/pixel; HIGH is 460 with 0.85 resolution scale. Anything added
   *inside* the march must be a toggle and must scale with the tier. Things added *outside* the
   march (Compose overlays, post-process passes, one-shot analysis) are essentially free.

---

## 4. ZIG's Blackhole — twelve candidate features

Effort: **S** ≤ 2 days · **M** 3–6 · **L** 7–15 · **XL** > 15 (one experienced dev, incl. FA strings
and tests).

### A. Surface what already exists

**A1. Light-path diagram, drawn in Compose** · S
*What:* the equatorial-plane picture that makes the shadow make sense — escaping rays, captured
rays, the prograde/retrograde photon orbits, the ISCO circle, the ergosphere, the camera.
*Why:* it is the best teaching artefact the engine owns and it is invisible today.
*How:* port `computeDiagram()` (equatorial null geodesics with the Kerr radial potential + a small
RK4) into a `BlackholePathDiagram` composable next to `AntarKerrMetrics`; validate the a = 0 case
against the analytic capture impact parameter `b = 3√3 M` (the existing unit-test style in
`app/src/test/java/com/zig/antar/` makes this trivial). The engine's own version is hidden behind
`hide-ui`; a Compose version gets Persian labels, theme ink and the app's squircle language — worth
the extra day over simply un-hiding the canvas.
*Risk:* low. If you want it tomorrow: patch the injected CSS to allow `#diagramBox` and drop the
`hide-ui` guard, then style around it.

**A2. GW strain chart with a scrubber** · S–M
*What:* the binary mode's `h₊(t)` chirp, drawn big, with the merger marker and a cursor bound to
playback.
*How:* Compose Canvas fed by telemetry (`binaryGwFreqHz`, `binarySeparationM`, `binaryMerged`) plus
the bundle's PN chirp model, or port the chirp (inspiral `f ∝ τ^(−3/8)` + ringdown damped sinusoid)
to Kotlin. Include the remnant line: `m_f`, `a_f`, radiated energy — the engine already prints it.
*Why:* it converts the merger from "two blobs collide" into a measurement.

**A3. "What am I looking at?" sheet, translated** · S
*What:* the engine's six-section explainer (ray tracing, shadow vs horizon, lensed back-of-disk,
colour asymmetry, disk model, the plunge) as a Compose bottom sheet, FA + EN.
*How:* text already exists in the bundle in two languages; translate to Persian, add to
`AntarStrings`, and add an `ⓘ` next to the title in `AntarControlPanel`.

**A4. Screenshot + "poster mode"** · S–M
*What:* a button that saves a PNG through MediaStore; and, because the renderer already converges
when paused (TAA + jitter), a **4K still** option that re-renders paused at ULTRA and 2× scale.
*How:* simplest path — JS `canvas.toDataURL('image/png')` read through the bridge, decoded and
written with `MediaStore.Images` (no storage permission needed on API 29+). Poster mode: set
`paused`, `quality=ultra`, `adaptiveRes=false`, scale 2.0, wait ~2 s, capture, restore.
*Why:* shareable artefacts are how astronomy apps spread.

**A5. Video capture** · M–L
*What:* the engine already has a `MediaRecorder`-based recorder writing `canvas.captureStream()`;
expose it as a Compose record button with the engine's REC pill.
*How:* either allow the bundle's recorder UI through the CSS allow-list, or drive it from Kotlin and
ship chunks over the bridge to a `MediaMuxer`. Watch the 60 s thermal budget on mid-range devices;
cap at 720p/tier-scaled.

### B. Sky and image

**B1. Real sky lensing — the Milky Way and the real stars** · M
*What:* the procedural sky replaced (or blended) with the app's own
`assets/sky/panorama/milkyway_2020_4k.jpg` and the 8,404 BSC5 stars from `assets/ar/stars_bsc5.bin`,
lensed through the same geodesics. Point the camera at the Galactic Centre and the Milky Way
arches around the shadow exactly as it would in nature.
*Why:* it is the difference between a rendering and an observation, and **the data is already in
the APK and already licensed** (NASA/Gaia panorama; Yale BSC5). Zero new provenance work.
*How:* bake a 2048×1024 JPEG + a packed 8k-star binary (mag + B−V + unit vector ≈ 16 B/star ≈ 130 KB)
into the bundle, or push them once over the bridge as data URLs and build the cubemap in JS. Option
C is adding `androidx.webkit` + `WebViewAssetLoader` and loading from assets — cleanest long-term,
but it changes how the page is loaded (the `document.baseURI` fallback patch exists precisely
because of the current loader).
*Risk:* bundle size +~400 KB raw (compressed less); needs a "real sky / procedural" toggle so the
artistic sky survives.

**B2. Photon-ring order map + analytic shadow edge** · M
*What:* a "science" render mode that colours each pixel by how many times its ray wound around the
photon sphere (n = 0 direct, n = 1, n = 2 …) with a legend, plus an optional analytic critical curve
(the Kerr shadow boundary from the spherical photon-orbit constants ξ, η) drawn over the image so the
user can see the numerical shadow match theory.
*Why:* the photon ring is the frontier — the 2024–2026 EHT/ngEHT literature is largely about
detecting its n = 2 sub-ring ([1](https://www.emergentmind.com/topics/next-generation-event-horizon-telescope-ngeht)).
It also makes "why is the ring thin" legible.
*How:* increment a winding counter in the march (integer, no extra transcendental cost), output it in
a second MRT target (the pipeline already renders to multiple attachments), map to a colour ramp in
the composite pass. The analytic curve is ~50 lines in Kotlin or JS and validates against
`b = 3√3 M` at a = 0.
*Risk:* the current tracer's disk accumulation is tuned for the common case; the order counter must
survive early-exit paths (captured rays are n = ∞).

**B3. Polarization view + 345 GHz "ngEHT" mode** · M–L
*What:* a toggle that draws a polarized ring the way EHT 2024 saw Sgr A\* — a spiral EVPA pattern, a
fractional-polarization loop and a circular-polarization dipole — plus a second filter at 345 GHz
(sharper beam, optically thinner) beside the existing 230 GHz one.
*Why:* the first polarized Sgr A\* image (EVPA spiral, 24–28 % resolved polarization, peaking ~40 %
in the west) is the most photogenic result of the decade and maps naturally onto procedural
rendering ([2](https://www.researchgate.net/publication/379358457_First_Sagittarius_A_Event_Horizon_Telescope_Results_VIII._Physical_Interpretation_of_the_Polarized_Ring)).
*How:* don't simulate GRMHD — synthesize the polarization pattern analytically from the local
disk frame (toroidal field ⇒ EVPA ⟂ projected field, spiral by construction) and composite over the
emission. Label it clearly as an illustrative reconstruction. Blur with a 345 GHz beam instead of
230 GHz — one uniform.
*Risk:* scientific-honesty risk if it looks like a simulation of radiative transfer. Say what it is
in the info sheet (A3).

**B4. Spectral panel (SED + band preview)** · M
*What:* the observed spectrum from the same disk: multi-temperature blackbody integrated over the
disk, shifted by the g-factor, with radio/sub-mm/optical/X-ray bands marked; a readout of what a
given telescope would see (EHT 230 GHz, ngEHT 345 GHz, Hubble, Chandra).
*Why:* it explains *why* Sgr A\* is faint in visible light but loud in radio, and ties TON 618's
quasar engine (`edd`, `corona`, `blr`) to observable bands.

### C. Spacetime, clocks and scale

**C1. Ergosphere & frame-dragging overlay** · S–M
*What:* the static limit surface `r_E(θ) = M + √(M² − a²cos²θ)` drawn as a translucent shell around
the horizon, plus a "ZAMO compass" — little arrows on the shell that twist at the frame-dragging
rate ω = 2Mar/Σ², so spin visibly drags space.
*Why:* "the horizon is not the point of no return for *photons*, the ergosphere is where nothing can
stand still" is a memorable, visual lesson, and the engine's light-path diagram already knows the
ergosphere by name.
*How:* Compose overlay or a new geometry pass (a ray-shell intersection test in the existing
fragment shader; both are cheap). `AntarKerrMetrics` already has everything needed for the math.

**C2. Twin clocks: hover vs free-fall** · M
*What:* a split view — left: the hovering observer's clock and the redshifted colour of a distant
beacon; right: the falling observer's proper time (the engine's plunge already integrates a true
timelike geodesic) — plus the classic "at the horizon the outside universe speeds up".
*Why:* the app's *Time Dilation* tool stops at flat-spacetime SR; this is the GR sequel, in the same
visual language, and the plunge telemetry already exists (`plungeProperTimeM`, `plungeRadiusM`,
`insideHorizon`).
*How:* pure Compose using telemetry + `staticTimeDilation()`; no bundle change.

**C3. Annotated scale ruler** · S
*What:* a vertical ruler beside the image in units of M and in km/AU/light-minutes for the selected
mass, marking `r₋`, `r₊`, `r_ph±`, `r_ISCO`, the camera and the disk's outer edge, live as spin
changes.
*Why:* users consistently underestimate the scale; "the whole shadow is the size of our Solar
System" is a hook. The optical (shadow) radii are also computable analytically.

**C4. Spacetime zoo: Kerr–Newman, Taub–NUT, wormhole** · XL
*What:* switch the metric — charged Kerr–Newman (`Q`), Taub–NUT (`N`, shadow shifted rigidly by 2N,
Misner string), Morris–Thorne traversable wormhole — each with a one-screen explanation of what
changes and why.
*Why:* the shadow *shape* is the observable; showing that charge shrinks it and NUT shifts it is a
legitimate strong-field-gravity lesson and a strong "wow" ([3](https://inspirehep.net/files/c05447337bc65a433bdf0fbb5e8b8f7b),
[4](https://github.com/maxenceleguery/blackhole)).
*How:* the real work is architectural — refactor the march to a **metric-provider** (inverse metric
+ horizon/conversion helpers) so one integrator serves all spacetimes. Worth doing *only* if you
plan two or more spacetimes, because it touches the hottest loop and every preset. Taub–NUT has no
Kerr–Schild form and needs its own spherical trace, so budget generously.
*Risk:* XL; do it behind a feature flag, and credit any reference implementation you consult.

**C5. Tidal probe / spaghettification** · M
*What:* drop a small rigid "probe" (a mesh sphere or a tiny satellite icon) on a radial geodesic and
watch it stretch and squeeze (geodesic deviation); readout of the tidal acceleration
`Δa ≈ 2GM r̂ / r³` in g's, and the "spaghettification radius" for a human.
*Why:* black holes are famous for tides, and the app can make it quantitative: for a 10 M☉ hole the
tidal force at the horizon is lethal; for Sgr A\* you cross happily.

### D. Real-object science

**D1. Sgr A\* flare mode (orbiting hot spot)** · M
*What:* a bright Gaussian spot on a near-circular orbit (~12 r_g, period ~70 min, sub-Keplerian/
super-Keplerian switch) at the GRAVITY-favoured inclination (~155°); as it orbits, lensing produces
primary + secondary images, so the flux centroid traces a loop and the light curve spikes — exactly
the astrometric signature VLTI measured ([5](https://www.aanda.org/articles/aa/full_html/2024/11/aa51884-24/aa51884-24.html)).
*Why:* this is the most *topical* thing you can add: it is the phenomenon behind the 2018–2024
Sgr A\* flare papers, it is beautiful, and no consumer astronomy app has it.
*How:* add spot parameters (`spotR`, `spotSize`, `spotTheta`, `spotPhase`) to the disk model in the
shader, plus a Compose light-curve strip driven by telemetry. Because the tracer already integrates
lensing, the secondary image comes out for free.
*Risk:* needs a real multi-crossing accumulation if the current disk hit only takes the first
equatorial crossing — verify first, then gate the extra crossings by quality tier.

**D2. S2 rosette: GR vs Newton** · M
*What:* a star on the real S2 orbit (a ≈ 970 AU, e ≈ 0.884, P ≈ 16 yr) around Sgr A\*, with a
Newton/GR toggle: in GR the ellipse precesses **12.1′ per orbit**, and the star reaches ~7,700 km/s
(2.5 % c) at pericentre. Draw the rosette, the radial-velocity curve and the redshift at pericentre
(the two effects GRAVITY actually measured) ([6](https://www.mpe.mpg.de/372679/Science)).
*Why:* it is the cleanest experimental proof the app can show, and it reuses the timelike integrator
the plunge already runs.
*How:* particle integrated with the same geodesic machinery; Compose plots for the sky trace and RV.
*Risk:* long baselines need many orbits — precompute or fast-forward with a time-compression slider
and an honest "1 orbit ≈ 16 yr" label.

**D3. Binary: two shadows, ringdown spectrum, a hearable chirp** · M–L
*What:* draw both shadows (superposed Kerr–Schild / Brill–Lindquist approximation) so the companion's
shadow is itself lensed; decompose the ringdown into QNMs (f₂₂₀ ≈ 12 kHz × M☉/M, so 10–100 M☉ holes
ring in the audio band) and **sonify** the chirp; add a stellar-mass scenario (10 + 30 M☉) because
the current TON 618 binary radiates at µHz — far below hearing.
*Why:* "listen to a merger" is the most shareable thing in gravitational-wave astronomy; LIGO
publishes exactly these audio files ([7](https://gwosc.org/audio/)), and the QNM/ringdown literature
(overtones, spectroscopy) is active ([8](https://arxiv.org/html/2205.07809)).
*How:* WebAudio in the bundle (`OscillatorNode` driven by `f_gw`) or Kotlin `AudioTrack` from
telemetry — the latter keeps the bundle untouched. Add a `STELLAR_BINARY` preset (mass scale
`STELLAR_10`, q, separation chosen so f_gw lands at 30–300 Hz).
*Risk:* audio needs a mute-by-default policy and must respect the app's sound settings; state the
pitch mapping explicitly (no fake "this is what it sounds like" claim).

### E. Product and learning

**E1. Guided "Predict → Observe → Explain" lessons** · M
*What:* 6 short lessons inside the feature (shadow vs horizon; Doppler asymmetry; why the disk
appears above and below; photon-ring orders; frame dragging; falling in). Each sets a preset,
freezes the relevant control, asks one multiple-choice question, then resolves against what the
simulation actually shows.
*Why:* the Gravity Sandbox already has exactly this machinery — `ChallengeRunner`, `TeachingCard`,
detectors (`com.zig.gravity.edu`). Reusing the pattern (and the visual language) is a few days and
changes ZIG's Blackhole from a toy into a lesson.
*How:* new `com.zig.antar.edu` package mirroring `com.zig.gravity.edu`; store progress in the same
prefs mechanism as `TutorialStore`.

**E2. Scene sharing and preset slots** · S–M
*What:* "save my scene" (3–5 named slots) + a compact share code/deep link
(`zig://bh?spin=0.94&incl=79&…`) that restores camera, disk and optics.
*Why:* the settings object already serializes to a query string (`toInitialQueryString`) — the work
is a codec and a share sheet, not physics. Also makes bug reports reproducible ("here is the link").

**E3. Tour gallery + cinematic export** · S–M
*What:* let the user pick which of the 10 tour scenes to play, in which order, and record that as
the video (A5). Add a "letterboxed, no HUD" cinematic toggle for the export path.

---

## 5. The Lab screen

### 5.1 Fix the shape before adding tools

1. **One resolver, three skins.** Introduce a `LabTool` descriptor (id, FA/EN title, subtitle,
   accent, viewport painter, `@Composable` root) and have all three styles render from it instead
   of three parallel `if/else` chains. This turns "add a tool" from 3 screen edits into 1 and stops
   the three styles drifting apart (they already differ in copy: Cyber says "4 simulators").
2. **Discovery layer.** The list is 3 cards today and will be ~10: add search, 2–3 tags
   (Relativity / Gravity / Light / Sky), favourites + "recently used" (prefs), and difficulty dots.
   Keep the cards but give each a real thumbnail (a captured still) instead of generic procedural art.
3. **Lab paths.** Group tools into 3 short curricula with progress: *Relativity* (Time Dilation →
   Twin clocks → ZIG's Blackhole), *Gravity* (Sandbox → Tidal/Roche → GW), *Light* (Aberration →
   Eclipse → Polarization). Each tool ends with one POE card (E1 pattern).
4. **Deep links from the rest of the app.** "Sgr A\* is up tonight in Sagittarius → open ZIG's
   Blackhole at the real sky position"; "next total eclipse over your city → Eclipse Lab"; "ISS
   pass → Sandbox with a 400 km circular orbit". This is what makes the Lab feel like part of an
   astronomy app rather than a separate app inside it.

### 5.2 New tools worth building (ranked by reuse)

| Tool | What the user does | Reuses | New work | Effort |
|---|---|---|---|---|
| **Eclipse Lab** | pick a date + a city from the built-in catalogue; see local circumstances (type, magnitude, obscuration, C1–C4, Sun altitude), the umbra path on a map, Baily's-beads/annular explainer, "next eclipse here" | `EclipseEngine`, `EclipseVisibility`, `GeoLocationCatalog`, `WorldGeographyData` | contact times + altitude + a Canvas map/path animation | M |
| **Exoplanet Transit Lab** | drag `R_p/R★`, impact parameter `b`, period; watch the light curve, then invert it (depth → planet radius, duration → density); a small offline table of famous systems | `CelestialObjectSizes`, physical-data catalog | Kepler orbit + limb-darkened transit model + RV curve | M–L |
| **Gravitational-Wave Lab** | chirp generator with mass ratio/spin, strain + spectrogram, "matched filter" SNR toy, sonification, antenna pattern | the binary model from ZIG's Blackhole (D3) | detector noise model + SNR | M |
| **Cosmology & Redshift Lab** | z → lookback time, distance modulus, angular size, age; Hubble diagram with a real SNe sample; "TON 618's light left 10.8 Gyr ago" | new ΛCDM (~80 lines) | plot + sliders | S–M |
| **Stellar Life / HR Lab** | mass → L, R, T, lifetime; place real BSC5 stars on the HR diagram; end state (WD / NS / BH) with the mass cut | `StarCatalog` spectral classes and the colour mapping already in `StarRenderer` | HR plotting + a mass–luminosity relation | S–M |
| **Aberration & Lightspeed Lab** | "ride at 0.9 c": the sky warps into a headlight — aberration + Doppler + intensity | the app's Milky Way panorama | a CPU-warped image or a small shader | M |
| **Deep-Sky Tonight** | "can I actually see it?" — surface brightness vs Bortle class, Moon, altitude; finder chart | `ObservabilityEngine`, `FinderEngine`, `WhatsUpTonightEngine`, `DeepSkyEngine` | a scoring model + chart | S–M |
| **Tidal / Roche Lab** | how close before a comet, a moon or a human is torn apart — planet and black-hole modes | gravity constants, Sandbox visuals | a compact calculator | S |

Each of these is a `LabFeatureType` entry with `isAvailable = true`, plus one Root composable, plus
FA/EN strings. With the resolver from 5.1, that is the whole cost.

---

## 6. Priority matrix and a three-release plan

```
value
  ▲
  │  A1 diagram        B2 photon orders      B1 real sky
  │  A2 GW chart       D1 Sgr A* flare       ◆ Eclipse Lab
  │  A4 still          D2 S2 rosette
  │  E1 lessons        B3 polarization       C4 spacetime zoo
  │  C3 ruler          C2 twin clocks        C4/XL
  └──────────────────────────────────────────────────────▶ effort
```

**Release 1.4 — "bring the lights up" (≈1–2 weeks):** A1, A2, A3, A4, C3, E2.
All Compose-side, no shader risk, immediate perceived depth. Add the "poster mode" 4K still because
it is the same code as A4.

**Release 1.5 — "science mode" (≈3–4 weeks):** B1 real sky, B2 photon-ring orders + shadow edge,
C1 ergosphere overlay, C2 twin clocks, D2 S2 rosette, E1 first three lessons.
This is the release that makes reviewers write about you.

**Release 1.6 — "observations" (≈4–6 weeks):** D1 flare mode, B3 polarization + 345 GHz, A5 video,
D3 sonified chirp, and the first two new Lab tools (Eclipse Lab, Exoplanet Transit Lab) behind the
new `LabTool` resolver.

Hold **C4 spacetime zoo** until 2.0 — it is the only item that changes the integrator's contract and
it should follow, not precede, the shader work above.

---

## 7. Guardrails

**Performance.** HIGH is 460 steps at 0.85 scale; ULTRA 800 at 1.0. Every per-step addition
(polarization sampling, extra disk crossings) must be a toggle, must degrade with the quality tier,
and must keep the adaptive-resolution controller able to hit 60 fps. Prefer a second render target
or a post pass over more marching. Measure on a mid-range 2020-era phone, not on a flagship.
Inside the Compose overlay, avoid per-frame allocations — telemetry arrives every 220 ms.

**Bundle fragility.** `applyMobileWebViewFixes()` contains 14 exact-string replacements, each
guarded by `check()`; `AntarPayloadAndIntegrationTest` asserts specific patched strings and the
`> 200 000` character size. Any bundle edit must go through the decode → edit → gzip →
base64 pipeline and re-run those tests. Prefer appending new code over mutating existing lines.

**Licensing and provenance.** This repo just removed a credit, so be exact in the other direction:
the Milky Way panorama (NASA/Gaia/DPAC) and BSC5 catalogue are already credited in
`THIRD_PARTY_LICENSES/README.md`; EHT images are CC BY 4.0 and would need attribution (better: draw
the rings procedurally and cite the *result*, not the picture); LIGO audio from GWOSC is free to
use with credit. If any new algorithm is ported from a public project (e.g. a Taub–NUT trace or a
QNM solver), add the credit and licence text in the same commit — and never claim credit for code
that is not there.

**Scientific honesty.** The simulation is a Kerr ray tracer with an analytic disk, not GRMHD.
Polarization (B3) and any "ngEHT" mode are illustrative reconstructions — say so where the user can
see it (A3). Quote numbers with their assumptions (mass scale, spin, inclination). Keep the
"approximate" language that the Chal card used to carry, now in the info sheet instead of a card.

**Localization and accessibility.** Every string lands in `AntarStrings`/`strings.xml` in both
languages, digits through `AntarNumberFormat` (Persian digits). New colour-coded maps (photon order,
polarization) need colour-blind-safe ramps and a legend; tours and the plunge need a
reduce-motion switch; every icon button needs a content description.

**Battery.** Pause the WebView (`onPause`) when the control sheet is fully covering the view or the
app is backgrounded — the engine already respects `paused`, and the tour/plunge are the only
features that need the GPU while the phone is idle.

---

## 8. Sources

1. ngEHT capabilities, 345 GHz, photon-ring (n=1) polarimetric detection — [emergentmind: Next-Generation Event Horizon Telescope](https://www.emergentmind.com/topics/next-generation-event-horizon-telescope-ngeht)
2. First polarized Sgr A\* image: EVPA spiral, 24–28 % (peak ~40 %), 51.8 ± 2.3 µas ring — [EHT VIII, Physical Interpretation of the Polarized Ring](https://www.researchgate.net/publication/379358457_First_Sagittarius_A_Event_Horizon_Telescope_Results_VIII._Physical_Interpretation_of_the_Polarized_Ring)
3. Kerr–Newman / Kerr–Taub–NUT shadow observables (charge shrinks, NUT shifts/enlarges) — [Black hole shadows in accelerating Kerr–Newman–Taub–NUT](https://inspirehep.net/files/c05447337bc65a433bdf0fbb5e8b8f7b)
4. Prior art: one WebGL2 shader, Schwarzschild/Kerr/Taub–NUT, QNM sonification, two-shadow binary — [maxenceleguery/blackhole](https://github.com/maxenceleguery/blackhole); temporal-resolve and three-crossing disk images — [theanandprasad/web-blackhole](https://github.com/theanandprasad/web-blackhole)
5. Sgr A\* orbiting hot spot: i ≈ 155°, P ≈ 70 min, r ≈ 12 r_g, ~1.3× Keplerian — [Hot spots around Sagittarius A\*, A&A 2024](https://www.aanda.org/articles/aa/full_html/2024/11/aa51884-24/aa51884-24.html)
6. Schwarzschild precession of S2, 12.1′ per orbit, f_SP = 1.10 ± 0.19 — [MPE: GRAVITY science highlights](https://www.mpe.mpg.de/372679/Science); v_max ≈ 7,700 km/s at pericentre — [Sky & Telescope](https://skyandtelescope.org/astronomy-news/black-hole-changes-stars-orbit-in-gravity-test/)
7. LIGO/Virgo event audio (chirps, bandpassed, pitch-shifted) — [GWOSC audio files](https://gwosc.org/audio/)
8. Ringdown and QNM overtones in GW150914 — [Searching for a Ringdown Overtone in GW150914](https://arxiv.org/html/2205.07809); [IMR-informed ringdown spectroscopy, 2025](https://arxiv.org/html/2509.17315)

*Internal references: `app/src/main/java/com/zig/antar/{config,physics,render,ui}`, `AntarSimulationPayload.kt`
(bundle + 15 patch anchors), `AntarKerrMetrics.kt`, the three Lab screens, `com.zig.gravity.edu`
(POE teaching layer), and `astro_engine/{Eclipse,JupiterMoons,Observability,Finder,WhatsUpTonight}Engine.kt`.*

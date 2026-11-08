# Persepolis and Mil-e Azhdaha: the monument pages

**Scope:** the two stone subjects of the full-bleed Home sky scene (`HeroSkyCanvas`,
`SkyCanvasPresentation.SCENE`, photographic themes). Tapping either monument raises its name on the
canvas; tapping that name opens a dedicated, full-screen page for it. The pages are
`ui/monuments/MonumentScreen.kt`, their copy is `ui/monuments/MonumentContent.kt`, and the state that
opens them is `MainUiState.monumentDetail` (ViewModel-owned, like `showSkyViewer`, so a configuration
change cannot close a page).

```
Home landscape (GL relief)  →  tap the stone  →  name callout  →  tap the name  →  MonumentScreen
```

## 1. The tap target is the stone, not a percentage of the screen

The hit boxes come from `PersepolisFraming.Result`, the same pixel landmarks the GL relief is placed
with, so a tap can never be "near" the monument without being on it:

| Monument | Box (x) | Box (y) |
|---|---|---|
| Persepolis | `facadeLeftXPx … facadeRightXPx` | `tallestColumnTopYPx … toeYPx` |
| Mil-e Azhdaha | `milTowerCenterXPx ± 8 m`, expanded | `milTowerTopYPx … milSlopeFrontYPx` |

Both get 16 dp of slack for the finger. Mil-e Azhdaha is tested first because it stands at the far
right, where the Persepolis forecourt never reaches. The framing is published from inside
`BoxWithConstraints` with `SideEffect` into a `MutableState` that the gesture lambda reads at tap
time; nothing is recomputed per frame.

The callout is the existing `SelectionCallout` used by the sky's objects (same surface, same
hairline, same wedge pointing at its subject), pinned to the tallest column's capital, or to the
tower's ruined top. Its accent is limestone gold, its detail line says what the place is, and the
whole card is the button. It stays for seven seconds (a page is a bigger commitment than a star
card's four), fades on its own, and clears when the finger starts scrubbing time or when the user
taps somewhere else. Taps on the sky are unchanged: the monuments only win inside their own stone.

The `PersepolisTextureView` host remains non-clickable and consumes no touches, so it never
intercepts the gesture.

## 2. The page: one accent, no card grid

The design brief was "modern and gorgeous and generous, without looking like AI slop with too many
cards and colours". The page is therefore built like a printed plate, not a dashboard:

* **Palette, three values and one accent.** Ink `#0B0A08`, paper `#F3EBDD`, body `#D9CEBC`, muted
  `#A2957F`, one limestone gold `#C9A063`. There is no second accent anywhere; hierarchy is carried
  by scale and space.
* **Hero.** A full-bleed photograph at 74 % of the viewport height, under two gradients (legibility
  at the top, a dissolve into the page at the bottom), with the kicker, the title, a one-line
  subtitle and a dot-separated meta row on the same baseline system.
* **Type.** Persian headlines are Lalezar (bundled, SIL OFL, see `THIRD_PARTY_LICENSES/`); body copy
  is Vazirmatn at 15.5 sp / 34 sp leading. English keeps the platform face. Nothing is letter-spaced
  in Persian.
* **Measurements.** Four cited figures in a 2 × 2 band separated by hairlines — deliberately *not*
  four cards. The values are the only place the display face is used at large size besides headings.
* **Chapters.** A gold index, a hairline that runs to the end of the measure, a display heading and
  generous paragraphs. Photographs sit between chapters at full width, with a small gold rule and a
  caption.
* **One pull quote.** A single gold bar and a short line; the Persepolis page quotes the Gate of All
  Nations inscription (marked as a translation), the Mil-e Azhdaha page quotes its own line with no
  false attribution.
* **Eras.** A two-column list (year, event) divided by hairlines, with no timeline ornament.
* **Colophon.** A centred gold lozenge, then the photograph and text credits in muted 10 sp.

## 3. Motion

Everything moves once, slowly, and off the same easing (`CubicBezierEasing(0.16, 0.84, 0.24, 1)`):

* the page is placed by MainActivity's `AnimatedVisibility`: 0.92 → 1 scale, 1/12 slide and fade in
  over 420 ms; closing is faster (260 ms, 0.96 scale) so it feels like a release;
* the hero photograph has a 26-second Ken Burns drift and parallaxes at 0.24 × the scroll, while
  scaling up slightly as it leaves;
* every block fades and rises 34 dp when it is first composed (which is when a `LazyColumn` scrolls
  it into view) over 760 ms;
* the back button compresses under the finger on a stiff spring;
* a slender title bar, over a gradient rather than a hard edge, fades in once the hero has gone.

## 4. Copy and sources

The Persian copy was written and then passed through the `persian-writing` skill's tooling
(`persian_cleanup.py --edit`, `fa_lint.py --check`, including `--rhythm`): 0 issues, mean sentence
length 10.9 words with 0.72 variation. It uses ZWNJ, Persian digits, «گیومه», ، ؛, no Arabic ي/ك, no
em dashes, no `می‌باشد`, no significance inflation, and no triad padding. Every Persian line starts
with a Persian word.

Facts are limited to what the sources support, and where the sources disagree the page says so
(the Mil-e Azhdaha's date: most sources say Parthian, some push it to the 3rd century AD).

**Persepolis** — founded c. 518 BC by Darius I; 125,000 m² terrace; Apadana with 36 columns at
c. 19 m; 23 nations on the stairway reliefs; burned in 330 BC; UNESCO World Heritage since 1979.

* UNESCO World Heritage file for Persepolis.
* The Persepolis fortification/treasury tablets and the palace inscriptions (DPg, XPa trilingual).
* Archaeological reports on the Apadana and its stairways (University of Chicago, Godard,
  Schmidt).

**Mil-e Azhdaha (برج نورآباد)** — Parthian-era square stone tower in Mamasani county, Fars,
7 km west of Nurabad in the Tang-e Kaleh valley; > 7 m tall, c. 4 m at the base, 15 courses of
veined limestone without mortar; interior stair at c. 3 m; fire altar on top; sacred fire kept
there and carried to other places; also used to guide caravans, signal and watch; national heritage
no. 356, registered 20 Khordad 1321 (1942); metal rods inserted in the second Pahlavi period.

* `visitiran.ir/attraction/mil-ejdeha` (dimensions, stairs, Vandenberghe's reading).
* Tehran Times, "Mil-e Ejdeha: off-the-beaten-path temple of Parthian era" (2024-10-22).
* Iranian national heritage registration 356.
* Wikimedia Commons, `Category:Nurabad Tower`.

## 5. The photographs are real photographs

No generated imagery is used. Files live in `res/drawable-nodpi/` as WebP (one file per photograph,
resized to at most 1600 px on the long edge; total ≈ 1.1 MB).

| Drawable | Subject | Source | Credit |
|---|---|---|---|
| `monument_persepolis_gate` | Gate of All Nations, Persepolis | persianlanguageonline.com (Wikimedia Commons image) | Wikimedia Commons |
| `monument_persepolis_terrace` | Persepolis terrace, Apadana and columns | kavehfarrokh.com photographic overview | Kaveh Farrokh |
| `monument_persepolis_columns` | Apadana columns and door frames | kavehfarrokh.com photographic overview | Kaveh Farrokh |
| `monument_persepolis_relief` | Tribute bearers, Apadana stairway | ISNA (1401/01/01 report on Nowruz reliefs) | ISNA |
| `monument_persepolis_panel` | Achaemenid relief panel at Persepolis | destinationiran.com | Destination Iran |
| `monument_mil_hero` | Mil-e Azhdaha tower and valley | Tehran Times, 2024-10-22 | Tehran Times |
| `monument_mil_tower` | Mil-e Azhdaha tower, low sun | Wikimedia Commons, photo Mohammadreza Atabak (the in-image credit line was cropped; the credit is printed in the page's colophon) | M. R. Atabak |
| `monument_mil_valley` | Mil-e Azhdaha at golden hour, mountains | visitiran.ir | Visit Iran |

Each photograph was inspected against the monument it claims to be; three candidates were rejected
during the search because they showed Ka'ba-ye Zartosht, Naqsh-e Rostam or the Pasargadae fire
temple instead. Every page repeats its image credits and its text sources in the colophon, which is
also where a licence notice for any photograph can be added if the app is published commercially.

## 6. Verification

* `MonumentContentTest` (JVM) asserts the structure of both pages in both languages (hero,
  photograph count and uniqueness, chapters, one quote, one era list, four measurements, identical
  block order across languages) and mechanically checks the Persian copy: no Arabic ي/ك or
  Arabic-Indic digits, no Latin digits, no em/en dashes, no administrative or machine tells, ZWNJ
  present, and callout text short enough for its card.
* `./gradlew testDebugUnitTest` runs it as part of the normal suite; the release APK is built by
  `.github/workflows/build.yml` after the full suite passes.

## 7. Files

| File | Role |
|---|---|
| `ui/monuments/MonumentContent.kt` | `MonumentId`, the block model, and the bilingual copy |
| `ui/monuments/MonumentScreen.kt` | the page: hero, lead, stats, chapters, figures, quote, eras, colophon, motion |
| `ui/components/HeroSkyCanvas.kt` | the two hit boxes, the name callout and the open action |
| `MainActivity.kt` | the page's enter/exit transition and its z-order above the nav bar |
| `ui/MainViewModel.kt` | `monumentDetail` state and `openMonument` / `closeMonument` |
| `res/font/lalezar_regular.ttf` | Persian display face (SIL OFL) |
| `res/drawable-nodpi/monument_*.webp` | the eight photographs |

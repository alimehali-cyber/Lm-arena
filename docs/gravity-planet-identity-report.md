# Gravity Sandbox — planet visual identity + Labs card title (§9 report)

Date: 2026-09-13
Branch: `arena/01a09b75-lm-arena`
Commit: `3598278` (on top of the visual-refresh work `a736d9d` / `0a50a06` / `438dfb5`)
CI: run `34777225306` on sha `3598278` — **success**, `tests=405 failures=0 errors=0 skipped=0 files=42`

Visual-only pass. No physics, engine, constant, gesture, camera, timing, persistence, surface,
shadow, HUD or chrome behaviour was altered, and the marble's lighting geometry is the same code it
was — same off-centre gradient, same three stops, same rim, same single specular dot.

---

## (0) Discovery

**Body rendering — exactly one file.** `app/src/main/java/com/zig/gravity/ui/TabletopCanvas.kt`.
`private class SceneCache(capacity)` holds every preallocated per-body brush/colour/float array; it is
filled inside `remember(epoch, colors, density.density)` in the `TabletopCanvas` composable, and
consumed by `private fun DrawScope.drawScene(...)` in the order trails → prediction → origin marker →
**bodies** → vectors → barycentre → slingshot → effects → label. Bodies are one `when (type)` with a
branch each for `WORMHOLE_MOUTH` and `BLACK_HOLE` and an `else` that draws the marble: two-layer cast
shadow, then `translate(px, py) { scale(bodyScale) { base → rim → specular } }`. There is no second
body renderer anywhere in the module (`InspectorSheet` and `TutorialOverlay` draw their own unrelated
diagrams), so the identity layer had exactly one place to go.

**Body colours.** `app/src/main/java/com/zig/gravity/sim/BodyCatalog.kt`: `CatalogEntry.colorArgb`
plus the optional authoritative `deepArgb` added by the previous pass, resolved through
`colorOf(key, type)` / `deepColorOf(key, type)` with per-`BodyType` fallbacks. Fourteen entries, no
Pluto or other dwarf planet — Neptune is the outermost.

**Who actually carries a key.** Every body in every preset is added with its catalog key
(`Presets.kt` passes `e.key`, or literals like `"sun"`/`"earth"`), and the Add sheet adds through
`addFromCatalog(key)`. So identity resolves by key in practice; the `BodyType` fallback is defensive.

**Labs card.** `app/src/main/java/com/alijafari/red/astronomy/ui/screens/LabScreen.kt`,
`enum class LabFeatureType(titleEn, titleFa, subtitleEn, subtitleFa, descriptionEn, descriptionFa,
icon, isAvailable)`. The card is tagged `lab_feature_card_gravity_sandbox`, renders the title at
`text = if (isFa) feature.titleFa else feature.titleEn` and the subtitle as a **separate** `Text`,
and navigation is `selectedFeature == LabFeatureType.GRAVITY_SANDBOX → GravitySandboxRoot(...)`.

**A second, contradictory title existed.** `res/values/strings.xml` and `res/values-fa/strings.xml`
define `gravity_sandbox_title` = `Gravity Sandbox` / `شبیه‌ساز گرانش`. Neither is referenced by any
code (`R.string.gravity_sandbox_title` has zero usages), but the Persian one held wording §7 forbids.

**The sandbox already calls itself «میز گرانش».** `GravitySandboxRoot.kt:472` (top bar) and
`edu/TutorialContent.kt:63` (welcome card) use that exact Persian name — byte-identical, U+0645
U+06CC U+0632 U+0020 U+06AF U+0631 U+0627 U+0646 U+0634 — so the required Persian title matches the
module's own existing vocabulary rather than introducing a new one.

**Constraints that shaped the implementation.**
- `SimSnapshot.visualSignature()` excludes colours, and the scene cache is keyed
  `(epoch, colors, density.density)`; the draw phase must allocate nothing.
- `EngineConstants.MAX_BODIES = 20`, `CameraState.MIN_DRAW_DP = 1.6` → at deep zoom-out a body can be
  a ~5 px dot, so the identity layer needs a small-radius cut-off.
- Compose's `clipPath` is **not antialiased**: clipping belt rectangles to the limb would have put a
  jagged edge on the most visible part of a planet. Every mark is therefore an ellipse inscribed in
  the unit disc, which needs no clip at all.
- No Compose UI test harness exists, so the tree's test architecture is pure-JVM data tests plus
  source-text lint (`GravitySheetScrollTest`, `VazirmatnPersianTypographyTest`,
  `ContentIntegrityAuditTest`). The new tests use exactly that.

---

## (a) Files changed

| File | What changed |
| --- | --- |
| `ui/theme/BodyIdentity.kt` (**new**, 436) | Pure-data identity catalog: `IdentityMark` (ellipse in body-radius fractions + opaque tone + alpha, with an `isInsideUnitDisc` proof method), `RingBand`, `BodyIdentity`; `BodyIdentities` holds one spec per body, the seeded `scatter()` generator, the inscribed `band()` helper, the two-mark `crater()` helper, the hard caps and `of(key, type)` with per-type fallback. |
| `ui/TabletopCanvas.kt` (+134/−6) | `SceneCache` gains flattened, preallocated mark/ring arrays; the epoch-keyed build step resolves each body's identity once and tones its colours through the existing `bodyTone()`; the marble pass becomes base → marks → the **same** base brush re-applied at 0.38 alpha → rim → specular, with Saturn's far ring half before the sphere and its near half after; two new allocation-free `DrawScope` helpers; header doc updated to describe the new order. |
| `sim/BodyCatalog.kt` (7 lines) | Base `colorArgb` only, for accuracy: Mercury `9A928A→BFB7AC`, Venus `D6BE92→EDD79A`, Mars `A6705C→E4501F`, Jupiter `C2A57B→E8D3A8`, Saturn `D2C08F→F0E2B6`, Uranus `8FB3B0→A9E4E0`, Neptune `7183A6→3E5BD0`. Masses, `dp`, real radii and every `deepArgb` are byte-identical. |
| `astronomy/ui/screens/LabScreen.kt` (2 lines + comment) | `GRAVITY_SANDBOX.titleEn` → `Gravity Sandbox`, `titleFa` → `میز گرانش`. Subtitle, description, icon, `isAvailable`, ordering and navigation untouched. |
| `res/values-fa/strings.xml` (1 line) | Unreferenced `gravity_sandbox_title` aligned to `میز گرانش` so the tree holds one Persian name. English resource already read `Gravity Sandbox`; both subtitles untouched. |
| `test/…/GravityBodyIdentityTest.kt` (**new**, 500, 5 tests) | Identity containment/determinism/coverage, per-body recognizability (HSV + mark shape), rings-are-visual-only + pinned physics constants and catalog, marble/shadow/chrome/surface structure lint, save-restore carries physics and no visual fields. |
| `test/…/LabFeatureTitleTest.kt` (**new**, 152, 4 tests) | Exact English/Persian card titles, forbidden wording rejected byte-for-byte, localization path + subtitle/description/icon/ordering/navigation unchanged, title resources agree with the card. |

Totals: 7 files, +1233 / −12.

---

## (b) Physics integrity — explicit confirmation

Confirmed by the diff (which touches no physics file at all) **and** pinned by
`GravityBodyIdentityTest.saturnsRingsAreVisualOnlyAndNothingPhysicalMoved`:

- **Physics unchanged** — no file under `physics/` was modified; `git show --stat 3598278` lists only
  `BodyCatalog.kt`, `TabletopCanvas.kt`, `BodyIdentity.kt`, `LabScreen.kt`, `strings.xml` and two tests.
- **EngineConstants unchanged** — the test pins `G = 6.67430e-11`, `C = 2.99792458e8`,
  `M_SUN = 1.989e30`, `R_SUN = 6.957e8`, `M_EARTH = 5.972e24`, `R_EARTH = 6.371e6`,
  `M_MOON = 7.348e22`, `R_MOON = 1.737e6`, `AU = 1.496e11`, `MOON_ORBIT_RADIUS = 3.844e8`,
  `DT = 3600.0`, `BASE = 1.0e6`, `EPS_SOFT = 1.0e6`, `V_MAX = 1.0e6`, `MAX_FRAME_SECONDS = 0.1`,
  `MAX_BODIES = 20`, `MAX_SUBSTEPS = 1024`, `MAX_REFINE_DEPTH = 3`.
- **Masses unchanged** — every `massKg` literal is untouched (`317.8 * ME`, `95.16 * ME`,
  `0.107 * ME`, …); the test asserts `BodyCatalog.SATURN.massKg == 95.16 * M_EARTH` exactly.
- **Radii unchanged** — every `dp` and `realRadiusM` is untouched (Saturn still `15.0` dp /
  `5.8232e7` m); the test asserts both, and asserts the catalog's key list is still the same
  fourteen entries in the same order.
- **Collision unchanged** — `Collision.kt` untouched; the ring bands are `Stroke` arcs whose radii
  are all `> 1.0 × r` and live only in `SceneCache`, never in `SimArrays`. The test asserts
  `rings.none { radiusFraction <= 1f }`, i.e. a ring can never be mistaken for a body radius.
- **Gravity calculations unchanged** — `NBodyEngine`, `Predictor`, `SimArrays`, `CameraState`,
  `Wormhole`, `Effects` and every detector/challenge file are untouched.
- **Gestures unchanged** — `GravitySandboxRoot.kt` was not modified in this pass at all.
- **Persistence unchanged** — `SaveState.kt` still `VERSION = 2` with a twelve-field header and
  twelve fields per body; the test round-trips `FULL_SOLAR_SYSTEM` and asserts Saturn comes back with
  its exact mass, display size, type and catalog key, and that no visual field entered the blob.
- **Visual layer cannot reach the simulation** — `BodyIdentities.of(key, type)` takes only a catalog
  key and a `BodyType`; no mass, radius, velocity or simulation value is even in its signature.

---

## (c) Visual verification

- **Sun** — gold base `#FFDC4A` (hue 48°, S 0.71, V 1.00) unchanged, plus 7 seeded soft warm patches
  (`#F2801F`, `#E4571C`) at alpha 0.08–0.17: a churning surface, no flares, no plasma, no animation.
- **Mercury** — base brightened to warm stone grey `#BFB7AC` (hue 35°, S 0.10, V 0.75) with 6 seeded
  small craters (`#6E655A`, alpha 0.16–0.26) and 2 soft basins: warm, busy, low-contrast — and
  provably not the Moon, whose marks are cool (hue ≈219°) and broad.
- **Venus** — base brightened to pale cream `#EDD79A` (hue 44°, V 0.93) under 3 broad golden cloud
  bands and a 2-ellipse swirl, all alpha ≤ 0.34 and all `ry ≥ 0.07r`: atmosphere, no surface detail.
- **Earth** — vivid ocean blue `#3FA1FF` (hue 209°, S 0.75) with 7 sparse stylised green land
  ellipses (`#3E9E5A`/`#4FAE66`/`#379152`, alpha 0.80–0.95) in three clusters plus one island, and 3
  thin white cloud streaks at alpha 0.22–0.30 drawn over the land. Not a world map.
- **Moon** — pure white base kept, with 3 cool-grey maria (`#8E97A8`/`#98A1B1`, alpha 0.24–0.30), 2
  larger craters built as a dark floor plus a lit inner floor offset toward the light, and 2 small
  craters: the marble shading stays plainly visible on top.
- **Mars** — now **vivid red-orange `#E4501F`** (hue 15°, S 0.86, V 0.89) instead of terracotta
  `#A6705C` (S 0.45, V 0.65), with 5 darker rust regions (`#9C3315`, `#A83A18`, `#93300F`,
  `#7E2A12`), one pale southern polar cap (`#FFF1E6`, alpha 0.55) and one brighter dust region.
- **Jupiter** — cream/ivory base `#E8D3A8` (hue 40°, S 0.28, V 0.91) with 6 inscribed horizontal
  belts alternating warm brown-beige and pale zones (alpha 0.34–0.58) and exactly one Great Red Spot
  (`#C24A32`, alpha 0.80) sitting on the southern belt.
- **Saturn** — pale warm cream `#F0E2B6` (hue 46°, V 0.94) with 3 very soft belts, plus the ring
  system below.
- **Uranus** — bright pale cyan `#A9E4E0` (hue 176°, S 0.26, V 0.89) with 3 extremely subtle belts
  (alpha ≤ 0.30) including a faint polar brightening; 33° of hue away from Earth's azure.
- **Neptune** — saturated cobalt `#3E5BD0` (hue 228°, S 0.70, V 0.82) with a darker belt, a brighter
  belt, a Great Dark Spot (`#1B2A6B`) and a two-ellipse white cloud streak. Relative luminance 0.131
  against Uranus's 0.693 — measurably deeper, not merely differently named.
- **Pluto / dwarf planets / other bodies** — no such entry exists in this catalog (Neptune is the
  outermost of the fourteen), so nothing was invented. Asteroids keep the warm rocky family
  (`#D89B66`, pinned by test) with 5 irregular darker patches/craters; the **test marble stays a
  plain marble**; unnamed planets get three neutral belts rather than borrowed continents.
- **Saturn's rings** — four concentric stroked arcs at 1.30r (C, faint), 1.48r (B, bright and wide),
  1.68r (A) and 1.82r (F, a hair of light), in muted pale gold/cream at alpha 0.16–0.46, squashed
  0.30 vertically, with transparent gaps of 0.090–0.108r between them (the widest is the Cassini
  division). Each band is drawn as two half-arcs split at the horizontal axis — top half behind the
  sphere, bottom half in front — so the system passes convincingly through and every ring pixel is
  painted exactly once (no alpha doubling, no clip path). Purely visual: no mass, no collision
  radius, no selection geometry, no simulation presence.

Black hole and wormhole mouth keep their existing treatment byte-for-byte: they never reach the
`else` branch, their identities are `BodyIdentity.NONE`, and their colours are pinned by test.

---

## (d) Title verification

- **English = `Gravity Sandbox`** — `LabFeatureType.GRAVITY_SANDBOX.titleEn`, rendered by
  `text = if (isFa) feature.titleFa else feature.titleEn`. Asserted equal, and asserted free of
  `ZIG`, `:`, `&`, `N-Body`, `Physics`, `Simulator`, not equal to the old
  `Gravity Sandbox & N-Body Physics`, not equal to a bare `Sandbox`, exactly two words.
- **Persian = `میز گرانش`** — `titleFa`, same render path. Asserted equal, and asserted free of the
  old `شبیه‌ساز گرانش و برهم‌کنش‌های N-جرم`, of `شبیه‌ساز گرانش`, of `آزمایشگاه گرانش`, of `ZIG`,
  `:`, `&` and `N-جرم`, exactly two words.
- Localization mechanism preserved (the same `isFa` ternary, nothing concatenated); subtitle,
  description, icon, ordering and navigation asserted unchanged; the `gravity_sandbox_title`
  resources now read the same two strings, with both subtitles left alone.

---

## (e) Tests

- CI run `34777225306`, sha `3598278`: **success**. `Full unit test suite :: tests=405 failures=0
  errors=0 skipped=0 files=42 gradle_status=0`. Every step green, including *Build signed release APK
  with Gradle* and *Upload real app APK only*.
- Tree total **405** `@Test` (was 396, **+9**): `GravityBodyIdentityTest` 5 + `LabFeatureTitleTest` 4.
- `com.zig.gravity` total **187** (was 182): Upgrade 56, CameraFollow 50, Integration 29, Collision
  20, Physics 19, **BodyIdentity 5**, SheetScroll 5, VisualRefresh 3.
- No existing test was weakened, deleted or edited. The previous pass's palette assertions
  (`GravityVisualRefreshTest`) still pass untouched because Sun, Earth, Moon, asteroid and test
  marble kept their hex values and Mars still has no authoritative `deepArgb`.
- The fifteen required AR/catalog test classes all report `PASS`.

---

## (f) Diff

`git show --stat 3598278`:

```
 .../red/astronomy/ui/screens/LabScreen.kt          |   7 +-
 .../main/java/com/zig/gravity/sim/BodyCatalog.kt   |  14 +-
 .../main/java/com/zig/gravity/ui/TabletopCanvas.kt | 134 +++++-
 .../java/com/zig/gravity/ui/theme/BodyIdentity.kt  | 436 ++++++++++++++++++
 app/src/main/res/values-fa/strings.xml             |   2 +-
 .../alijafari/red/astronomy/LabFeatureTitleTest.kt | 152 +++++++
 .../com/zig/gravity/GravityBodyIdentityTest.kt     | 500 +++++++++++++++++++++
 7 files changed, 1233 insertions(+), 12 deletions(-)
```

The exact diff of every **modified** file (the three new files are additions in full at the paths
above; `git show 3598278 -- <path>` prints any of them):

```diff
diff --git a/app/src/main/java/com/alijafari/red/astronomy/ui/screens/LabScreen.kt b/app/src/main/java/com/alijafari/red/astronomy/ui/screens/LabScreen.kt
@@ -47,8 +47,11 @@ enum class LabFeatureType(
         isAvailable = true
     ),
     GRAVITY_SANDBOX(
-        titleEn = "Gravity Sandbox & N-Body Physics",
-        titleFa = "شبیه‌ساز گرانش و برهم‌کنش‌های N-جرم",
+        // The card's title is the feature's name and nothing else: no subtitle folded into it, no
+        // product prefix. The N-body framing already lives in subtitleEn/subtitleFa below, which is
+        // where a description belongs.
+        titleEn = "Gravity Sandbox",
+        titleFa = "میز گرانش",
         subtitleEn = "Newton-Kepler Orbital Simulator",
         subtitleFa = "آزمایشگاه مکانیک سماوی و مدارهای کپلری",
         descriptionEn = "Simulate multi-body gravitational interactions, planetary orbits, binary stars, Lagrange equilibrium points, and black hole slingshots.",

diff --git a/app/src/main/java/com/zig/gravity/sim/BodyCatalog.kt b/app/src/main/java/com/zig/gravity/sim/BodyCatalog.kt
@@ -38,15 +38,15 @@ object BodyCatalog {
     val SUN = CatalogEntry("sun", "خورشید", "Sun", BodyType.SUN, MS, 26.0, 0xFFFFDC4A, EngineConstants.R_SUN, deepArgb = 0xFFF5A623)
-    val MERCURY = CatalogEntry("mercury", "عطارد", "Mercury", BodyType.PLANET, 0.0553 * ME, 8.0, 0xFF9A928A, 2.4397e6)
-    val VENUS = CatalogEntry("venus", "زهره", "Venus", BodyType.PLANET, 0.815 * ME, 10.0, 0xFFD6BE92, 6.0518e6)
+    val MERCURY = CatalogEntry("mercury", "عطارد", "Mercury", BodyType.PLANET, 0.0553 * ME, 8.0, 0xFFBFB7AC, 2.4397e6)
+    val VENUS = CatalogEntry("venus", "زهره", "Venus", BodyType.PLANET, 0.815 * ME, 10.0, 0xFFEDD79A, 6.0518e6)
     val EARTH = CatalogEntry("earth", "زمین", "Earth", BodyType.PLANET, ME, 10.0, 0xFF3FA1FF, EngineConstants.R_EARTH, deepArgb = 0xFF1E63D8)
     val MOON = CatalogEntry("moon", "ماه", "Moon", BodyType.MOON, EngineConstants.M_MOON, 6.0, 0xFFFFFFFF, EngineConstants.R_MOON, deepArgb = 0xFFC7CCD8)
-    val MARS = CatalogEntry("mars", "مریخ", "Mars", BodyType.PLANET, 0.107 * ME, 9.0, 0xFFA6705C, 3.3895e6)
-    val JUPITER = CatalogEntry("jupiter", "مشتری", "Jupiter", BodyType.PLANET, 317.8 * ME, 16.0, 0xFFC2A57B, 6.9911e7)
-    val SATURN = CatalogEntry("saturn", "زحل", "Saturn", BodyType.PLANET, 95.16 * ME, 15.0, 0xFFD2C08F, 5.8232e7)
-    val URANUS = CatalogEntry("uranus", "اورانوس", "Uranus", BodyType.PLANET, 14.54 * ME, 12.0, 0xFF8FB3B0, 2.5362e7)
-    val NEPTUNE = CatalogEntry("neptune", "نپتون", "Neptune", BodyType.PLANET, 17.15 * ME, 12.0, 0xFF7183A6, 2.4622e7)
+    val MARS = CatalogEntry("mars", "مریخ", "Mars", BodyType.PLANET, 0.107 * ME, 9.0, 0xFFE4501F, 3.3895e6)
+    val JUPITER = CatalogEntry("jupiter", "مشتری", "Jupiter", BodyType.PLANET, 317.8 * ME, 16.0, 0xFFE8D3A8, 6.9911e7)
+    val SATURN = CatalogEntry("saturn", "زحل", "Saturn", BodyType.PLANET, 95.16 * ME, 15.0, 0xFFF0E2B6, 5.8232e7)
+    val URANUS = CatalogEntry("uranus", "اورانوس", "Uranus", BodyType.PLANET, 14.54 * ME, 12.0, 0xFFA9E4E0, 2.5362e7)
+    val NEPTUNE = CatalogEntry("neptune", "نپتون", "Neptune", BodyType.PLANET, 17.15 * ME, 12.0, 0xFF3E5BD0, 2.4622e7)
     val ASTEROID = CatalogEntry("asteroid", "سیارک", "Asteroid", BodyType.ASTEROID, 1.0e18, 4.0, 0xFFD89B66, 5.0e5, deepArgb = 0xFF96603B)

diff --git a/app/src/main/res/values-fa/strings.xml b/app/src/main/res/values-fa/strings.xml
@@ -107,6 +107,6 @@
     <!-- Gravity Sandbox -->
-    <string name="gravity_sandbox_title">شبیه‌ساز گرانش</string>
+    <string name="gravity_sandbox_title">میز گرانش</string>
     <string name="gravity_sandbox_subtitle">شبیه‌سازی برهم‌کنش‌های N-جرم گرانشی</string>

diff --git a/app/src/main/java/com/zig/gravity/ui/TabletopCanvas.kt b/app/src/main/java/com/zig/gravity/ui/TabletopCanvas.kt
@@ -33,6 +33,7 @@
+import com.zig.gravity.ui.theme.BodyIdentities
@@ -53,8 +54,9 @@
- *  - bodies are marbles: two-layer cast shadow -> radial-gradient base -> rim -> restrained
- *    specular -> selection ring -> cached label. No rotation, atmospheres or terminators;
+ *  - bodies are marbles: two-layer cast shadow -> radial-gradient base -> static identity marks ->
+ *    the same base gradient re-applied over them (the lighting coming back) -> rim -> restrained
+ *    specular -> selection ring -> cached label. No rotation, no animated texture, no terminator;
@@ -69,6 +71,32 @@ private class SceneCache(capacity: Int) {
     val specular = Array(capacity) { Color.Transparent }
+
+    /** §1 planet identity — flattened, preallocated; fractions of [radiusPx]. */
+    val markCount = IntArray(capacity)
+    val markCx = FloatArray(capacity * BodyIdentities.MAX_MARKS)
+    val markCy = FloatArray(capacity * BodyIdentities.MAX_MARKS)
+    val markRx = FloatArray(capacity * BodyIdentities.MAX_MARKS)
+    val markRy = FloatArray(capacity * BodyIdentities.MAX_MARKS)
+    val markColor = Array(capacity * BodyIdentities.MAX_MARKS) { Color.Transparent }
+
+    /** Ring systems (Saturn). Purely visual: never mass, radius, collision or selection. */
+    val ringCount = IntArray(capacity)
+    val ringRadius = FloatArray(capacity * BodyIdentities.MAX_RINGS)
+    val ringSquash = FloatArray(capacity)
+    val ringColor = Array(capacity * BodyIdentities.MAX_RINGS) { Color.Transparent }
+    val ringStroke = arrayOfNulls<Stroke>(capacity * BodyIdentities.MAX_RINGS)
+
     val radiusPx = FloatArray(capacity)
@@ -188,6 +216,35 @@ fun TabletopCanvas(
             cache.specular[i] = Color.White.copy(alpha = if (colors.isDark) 0.30f else 0.42f)
+
+            // §1 — resolved here and never again until the visual set changes.
+            val identity = BodyIdentities.of(snap.catalogKey[i], type)
+            val mc = minOf(identity.marks.size, BodyIdentities.MAX_MARKS)
+            cache.markCount[i] = mc
+            for (m in 0 until mc) {
+                val mark = identity.marks[m]
+                val slot = i * BodyIdentities.MAX_MARKS + m
+                cache.markCx[slot] = mark.cx
+                cache.markCy[slot] = mark.cy
+                cache.markRx[slot] = mark.rx
+                cache.markRy[slot] = mark.ry
+                cache.markColor[slot] = colors.bodyTone(mark.argb).copy(alpha = mark.alpha)
+            }
+            val rc = minOf(identity.rings.size, BodyIdentities.MAX_RINGS)
+            cache.ringCount[i] = rc
+            cache.ringSquash[i] = identity.ringSquash
+            for (q in 0 until rc) {
+                val band = identity.rings[q]
+                val slot = i * BodyIdentities.MAX_RINGS + q
+                cache.ringRadius[slot] = band.radiusFraction
+                cache.ringColor[slot] = colors.bodyTone(band.argb).copy(alpha = band.alpha)
+                cache.ringStroke[slot] = Stroke(width = (band.thicknessFraction * r).coerceAtLeast(0.6f))
+            }
         }
@@ -430,12 +487,31 @@ private fun DrawScope.drawScene(
+                // §5 — base -> marks -> the same base re-applied (lighting back over the marks) ->
+                // rim -> specular; Saturn's far ring half under the sphere, near half over it.
+                val textured = r >= BodyIdentities.MIN_RADIUS_PX
                 translate(px, py) {
                     scale(bodyScale, bodyScale, Offset.Zero) {
                         val rr = cache.radiusPx[i]
+                        if (textured) drawRingHalf(cache, i, rr, far = true)
                         cache.base[i]?.let { drawCircle(it, rr, Offset.Zero) }
+                        if (textured) {
+                            drawIdentityMarks(cache, i, rr)
+                            cache.base[i]?.let {
+                                drawCircle(
+                                    brush = it,
+                                    radius = rr,
+                                    center = Offset.Zero,
+                                    alpha = BodyIdentities.LIGHTING_OVERLAY_ALPHA
+                                )
+                            }
+                        }
                         drawCircle(cache.rim[i], rr, Offset.Zero, style = cache.strokeRim)
                         drawCircle(cache.specular[i], rr * 0.17f, Offset(-rr * 0.34f, -rr * 0.38f))
+                        if (textured) drawRingHalf(cache, i, rr, far = false)
                     }
                 }
@@ -682,6 +758,60 @@ private fun DrawScope.drawScene(
+private fun DrawScope.drawIdentityMarks(cache: SceneCache, i: Int, rr: Float) { … drawOval per mark … }
+private fun DrawScope.drawRingHalf(cache: SceneCache, i: Int, rr: Float, far: Boolean) { … drawArc per band … }
```

---

## (g) Deviations

1. **`deepArgb` was deliberately *not* added to any recoloured planet.** Giving Mars an authoritative
   deep tone would have broken the previous pass's existing assertion
   `assertEquals(0L, BodyCatalog.deepColorOf("mars", BodyType.PLANET))`, which uses Mars as its
   example of "a catalogued body with no deep value". Deriving the terminator from the new vivid base
   already produces the darker rust the brief asks for, so no existing test had to be touched.
2. **The lighting "layer" in §5 step 3 is the existing base brush re-applied at alpha 0.38, not a new
   light.** The marble's lighting lives *inside* that gradient, so drawing marks on top of it would
   flatten the sphere. Re-drawing the very same cached brush restores the top-left highlight and the
   dark limb over the marks without inventing a shading element or allocating a brush. §5's order is
   preserved exactly and is proved from the source by test.
3. **No clip path anywhere.** Belts and continents are ellipses inscribed in the unit disc rather than
   rectangles clipped to it, because Compose's `clipPath` is not antialiased and would have jagged the
   limb. Consequence: belt ends taper slightly inside the limb instead of running to it. Containment
   is proved for every mark in the catalog (worst case reaches 0.969 r).
4. **Unnamed planets get neutral banding, not Earth's continents.** `colorOf(null, PLANET)` falls back
   to Earth's blue, and mirroring that exactly would have put invented continents on an object nobody
   identified — which §2's accuracy rule forbids. `GENERIC_PLANET` is three soft neutral belts. This
   path is defensive only: every preset and Add-sheet body carries a catalog key.
5. **No Pluto or dwarf planet exists to texture.** The catalog's fourteen entries stop at Neptune, so
   §2's dwarf-planet clause is honoured by omission and by the neutral fallback above, not by
   inventing a body.
6. **The identity layer is skipped below 8 px on-screen radius** (`MIN_RADIUS_PX`). At
   `MIN_DRAW_DP = 1.6` a body can be a ~5 px dot, where ten sub-pixel ellipses are both invisible and
   wasted work. Above that threshold nothing is skipped, so no body ever "pops" its texture at a
   normal working zoom.
7. **`res/values-fa/strings.xml` was changed although the card does not read it.** §7 says to change
   only what is necessary for the card, and the card uses the inline enum strings. But the tree held a
   second Persian name for the feature — `gravity_sandbox_title` = `شبیه‌ساز گرانش`, wording §7
   explicitly forbids — and leaving it would fail "verify that `میز گرانش` is the exact Persian title"
   for anyone who greps. One line, unreferenced resource, both subtitles untouched; the English
   resource already read `Gravity Sandbox`.
8. **The sandbox's own top bar still reads «میز گرانش» / "Gravity table"** (`GravitySandboxRoot.kt:472`)
   and was **not** changed: §7 names the Labs card, and that header is sandbox chrome the guardrails
   protect. Flagged here because the English half is now inconsistent with the card's name — say the
   word and it becomes `Gravity Sandbox` in a one-line follow-up.
9. **`edu/TutorialContent.kt` still says "باز کردن آموزش آزمایشگاه گرانش" / "Open Gravity Sandbox
   tutorial"** for the help button's content description. Education content is on the do-not-touch
   list and this is not a title, so it stays.
10. **Seven base hex values changed**, which §2/§3 require for accuracy (Mars above all) but which is
    still a change to `sim/BodyCatalog.kt` — a file that also carries masses and radii. Only the
    `colorArgb` literals moved; the diff shows every mass, `dp` and `realRadiusM` untouched, and the
    new test pins them.

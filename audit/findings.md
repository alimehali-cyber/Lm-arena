# Content-Integrity Re-Audit — Final Report (Steps 0–6)

Date: 2026-09-13 · Branch: `arena/01a09890-lm-arena`

A prior "FULLY VERIFIED" claim was known false. This report is produced from a fresh,
source-level audit of every object reachable anywhere in the app, and it re-verifies the
content after the Step 5 fixes. Nothing in this report trusts the prior test results.

---

## Step 0 — Entry-point map and union list

Every UI entry point that can open an object detail page resolves to **one unified
canonical catalog**. There is **no divergent source**: home search, AR search, AR sky,
favorites, What's Up Tonight, notification deep links, and the hero canvas all read the
same object set produced by `CanonicalAstroCatalog.getAllCanonicalObjects()` (facaded by
`AstronomyCatalog.getAllObjects(...)`). Home search does **not** use a broader source — it
uses the same catalog with a narrower *filter* (it hides Earth, the ISS, and satellites).

| Entry point | Code path | Data source | Filtering / note |
|---|---|---|---|
| Home search bar | `HomeScreen.kt` local substring filter over `sortedObjectsWithObs` (matches `nameFa/nameEn/constellationFa/constellationEn/category`) | `AstronomyCatalog.getAllObjects(jd)` → canonical | excludes `planet_earth`, `sat_iss`, all `SATELLITE` |
| Home featured cards | `HomeScreen.kt` catalog rows | same | same |
| Hero sky canvas tap | `HeroSkyCanvas.kt` → `viewModel.openObjectDetailById(id)` | `AstronomyCatalog.getAllObjects(simulatedJd)` | renders only `(STAR \|\| DEEP_SKY) && magnitude <= 4.5 && altitude > 0` |
| AR search | `CompassARScreen.kt` → `CelestialSearchEngine.search` | `CanonicalAstroCatalog.getAllCanonicalObjects()` | excludes Earth only |
| AR sky tap / edge indicators | `CompassARScreen.kt` → `viewModel.openObjectDetail(obj)` | `ARSkyCatalog.arSkyObjects(getAllObjects(jd))` | excludes Earth only |
| Favorites / bookmarks | `FavoritesHistoryDialog.kt` | `AstronomyCatalog.getAllObjects()` | none |
| What's Up Tonight events | `WhatsUpTonightEngine.kt` `targetObject` | `CanonicalAstroCatalog.getCanonicalObject(...)` | event-dependent |
| Notification deep link | `MainActivity.handleNotificationIntent` | `AstronomyCatalog.getById(targetObjId)` | satellite ids route to satellite tab |
| Satellite detail | `ISSScreen.kt` → `SatelliteDetailScreen` | `SatelliteCatalog` | renders `SatelliteItem.verifiedFacts*` directly |

Detail resolution (`MainViewModel.openObjectDetail`/`openObjectDetailById`):
`resolveCanonicalId → getCanonicalObject → AstroDispatchEngine.calculateState →
toCelestialObject → selectedObjectForDetail`. `toCelestialObject` maps canonical
`verifiedFacts*` onto `CelestialObject.funFacts*` when the latter is empty (line ~1694), and
`ObjectDetailModal` renders `funFacts*` (with a `PhysicalData.getCoolFacts*` fallback only
for non-deep-sky objects, which now resolves via `ExtendedFactCatalog`).

### Union list — definitive total

`audit/parse_catalog.py` (a faithful token-identity merge port of
`CanonicalAstroCatalog.extendedCanonicalObjectsList`) yields:

- **336 canonical objects** total; 0 duplicate canonical IDs.
- **239 deep-sky**: `GALAXY` 66 + `STAR_CLUSTER` 82 + `NEBULA` 55 + `GLOBULAR_CLUSTER` 35 + `BLACK_HOLE` 1.
- **97 non-deep-sky**: `STAR` 43 + `CONSTELLATION` 20 + `PLANET` 8 + `METEOR_SHOWER` 7 + `ASTERISM` 7 + `MOON` 6 + `SATELLITE` 4 + `SUN` 1 + `DWARF_PLANET` 1.

The previously assumed **336 / 239** figures are **correct**. The prior claim's error was
never the total — it was the *quality* of the content under that total (Step 3). One
transient object (`starlink_train`, injected by `StarlinkTrainManager`) is reachable via the
satellite tab but is not a static catalog object; it already carries 3 bilingual facts.

---

## Step 1 — Ground-truth classification (before → after)

| Bucket | Before | After | Notes |
|---|---|---|---|
| A — genuine validated facts, research-logged | 37 | **315** | 18 core + 15 hand 5-fact DSOs + 4 bright stars → now the 205 engine DSOs, 39 stars, 7 showers, 7 asterisms, 20 constellations are all genuine 5-fact sets |
| B — partial (<5 facts) with logged reason | 18 | **21** | 18 hand 3-fact DSOs + 3 satellites (3 facts each) |
| C — placeholder/generic/duplicate | 205 | **0** | 205 templated DSO sets rewritten (Step 5) |
| D — no facts, no logged reason | 76 | **0** | 39 stars + 7 showers + 7 asterisms + 20 constellations + 3 satellites fixed (Step 5) |
| E — reachable but blank/crash | 0 | **0** | every object renders through the single `ObjectDetailModal` + `PhysicalData`/`ExtendedFactCatalog` fallback |

`audit/classification.json` carries the final per-object bucket, reason, fact counts, and
research-log status (bucket counts: `{A: 315, B: 21}`, total 336).

---

## Step 2 — Cross-object duplicate-fact check

- **Exact** duplicate English facts across objects: **0** before and after.
- **Near-duplicate** (name-stripped) English fact templates:
  - **Before: 67 template groups.** The 205 "generated" DSO fact sets were assembled from a
    pool of ~67 boilerplate sentences with the object name substituted in (e.g. *"In
    Messier's comet-hunting context, § …"* on every Messier object; *"§ belongs to the Milky
    Way globular-cluster population…"* on every globular). 1025 English strings collapsed to
    67 skeletons → all 205 generated objects were bucket C.
  - **After: 0 groups.** The 205 sets were rewritten as object-specific facts; a second pass
    removed the remaining shared "… was discovered by … in 1XXX" sentences.

---

## Step 3 — Why the prior "FULLY VERIFIED" claim was false

1. **Scope vs. union list.** `DeepSkyFactCoverageTest` asserted exactly 239 deep-sky objects
   but never examined the other 97 union objects (stars, showers, asterisms, constellations,
   satellites) — 76 of which had zero facts.
2. **Non-emptiness vs. quality.** The "generic fallback" check used a hard-coded list of only
   6 banned snippets. The generated facts used *different* boilerplate sentences, so 205
   template fact sets passed as "validated".
3. **No cross-object duplication check.** No test compared facts between objects.
4. **Languages.** Bilingual *count* parity was checked, but Persian free-text quality/name
   leakage was checked only for `nameFa` with a narrow allow-list and only in one test.
5. **Rendered data path.** The modal facts path was spot-checked for only 10 IDs;
   `SatelliteCatalog.verifiedFacts*` were never reconciled against the canonical merge (3
   satellites' facts were silently dropped).
6. **Classification coverage.** `ClassificationAuditTest` audited only Pluto and Sgr A*.

---

## Step 4 — Corrected tests (proven to fail first)

`app/src/test/.../ContentIntegrityAuditTest.kt` now iterates the full 336-object union and
fails on C/D/E, checks both languages, checks exact **and** name-stripped cross-object
duplication, cross-references the research log (every ID logged; every sparse set has a
logged reason), verifies the rendered facts path and section plan, verifies satellite fact
propagation, and rejects Persian name leakage and English sentences in Persian facts.

**Proof it failed first:** the 1:1 Python mirror `audit/verify.py` reported, against the
pre-fix content, `research_log_tracked: 97`, `sparse_logged_reason: 3`, `no_generic_text:
959`, `no_cross_object_duplicates: 19`, `bucket D: 0` (at the last intermediate state) and
`objects failing >=1 check: 308` (of 336), exit 1. After the fixes it reports **0 failing,
336 passing** (see Step 6).

CI (`build.yml`) runs `testDebugUnitTest` (JDK 21 / Gradle 9.3.1) and gates on 15 required
test classes, including `ContentIntegrityAuditTest`, `CatalogIntegrityTest`,
`DeepSkyFactCoverageTest`, and `ObjectDetailModalConsistencyTest`, before producing the
signed release APK.

---

## Step 5 — Fixes by bucket/source

**Bucket D (76 zero-fact objects) — new hand-authored bilingual fact sets, each with 2+
reputable source families logged:**

| Source | File | Objects | Facts |
|---|---|---|---|
| 39 stars | `data/catalog/StarFacts.kt` | `star_*` | 5 each |
| 7 meteor showers | `data/catalog/MeteorShowerFacts.kt` | `shower_*` | 5 each |
| 7 asterisms | `data/catalog/AsterismFacts.kt` | `asterism_*` | 5 each |
| 20 constellations | `data/catalog/ConstellationFacts.kt` | `const_*` | 5 each |
| 3 satellites | `CanonicalAstroCatalog.kt` satellite merge | `sat_48274`, `sat_20580`, `sat_27386` | `verifiedFacts*` now propagated (3 facts each, logged reason) |

`ExtendedFactCatalog.kt` aggregates the four fact maps and `PhysicalData.getCoolFacts*` now
falls back to it, so the rendered modal path resolves facts for every non-deep-sky object.

**Bucket C (205 templated DSOs):** `DeepSkyGeneratedFacts.kt` was regenerated with 205
hand-authored, object-specific bilingual fact sets (5 facts each, no cross-object
templating). A follow-up dedup pass removed shared discovery/age sentences so the
name-stripped skeleton check is now 0. Source families (SEDS Messier, NASA/Hubble Messier,
Celestron Caldwell, OpenNGC/GAVO, SIMBAD/CDS, NASA/IPAC NED) are logged per object.

**Research log:** `docs/dso-content-research-log.md` now tracks all 336 union objects. The
205-object deep-sky section was regenerated to match the new facts; the 97-object
non-deep-sky section was appended (constellation-area parsing fixed so `const_*` entries
carry real IAU areas). The 18 sparse DSOs and 3 satellites each carry an explicit
"Fewer than five" reason.

**Bucket E:** none — every object renders through the single `ObjectDetailModal` path; the
only non-modal path is satellite detail, which renders `verifiedFacts*` directly.

---

## Step 6 — Reclassification and verification

- **Reclassified final state:** A = 315, B = 21, **C = 0, D = 0, E = 0** (336/336).
- **Duplicate re-check:** exact duplicates 0; name-stripped near-duplicates 0.
- **`audit/verify.py`** (1:1 mirror of `ContentIntegrityAuditTest`):
  `bucket D (zero facts): 0`, `objects failing >=1 check: 0`, `objects passing all checks:
  336`, `PASS`.
- **`audit/parse_catalog.py`:** `FINAL CANONICAL COUNT=336`, `DEEP_SKY_COUNT=239`,
  `ZERO_FACT_COUNT=0`, exact dup 0, near-dup 0.
- **`audit/classify.py`:** `bucket counts: {A: 315, B: 21}`, unlogged 0, template-group 0.

### Manual trace — 20 objects through actual entry points (source-level; the app cannot be built locally in this sandbox, so each trace follows the exact code path end-to-end)

| # | Object | Entry point | Trace |
|---|---|---|---|
| 1 | `dso_m1` | Home search ("M1" / "سحابی خرچنگ") | `HomeScreen` filter → `openObjectDetail` → `toCelestialObject` (verifiedFacts→funFacts) → modal facts card (5 facts) |
| 2 | `const_ori` | Home search ("Orion") | filter → modal → `PhysicalData.getCoolFacts*` → `ExtendedFactCatalog.factsFor("const_ori")` (5 facts) |
| 3 | `star_cma_sirius` | Home search ("Sirius") | filter → modal → `ExtendedFactCatalog.factsFor("star_cma_sirius")` (5 facts) |
| 4 | `shower_perseids` | Home search ("Perseids") | filter → modal → `ExtendedFactCatalog` (5 facts) |
| 5 | `asterism_big_dipper` | Home search ("Big Dipper") | filter → modal → `ExtendedFactCatalog` (5 facts) |
| 6 | `dso_m42_orion_nebula` | AR search ("orion") | `CelestialSearchEngine.search` → `openObjectDetail` → funFacts (5) |
| 7 | `star_boo_arcturus` | AR search ("arcturus") | search → modal → `ExtendedFactCatalog` (5) |
| 8 | `dso_c11` | AR search ("bubble") | search → modal → funFacts from `DeepSkyGeneratedFacts` (5) |
| 9 | `dso_m31_andromeda` | AR sky tap / indicator | `ARSkyCatalog.arSkyObjects` → `openObjectDetail` → funFacts (5) |
| 10 | `dso_ngc_6960` | AR sky tap | AR list → modal → funFacts (5) |
| 11 | `dso_ngc_6992` | AR sky tap | AR list → modal → funFacts (5) |
| 12 | `dso_m45_pleiades` | Hero sky canvas tap | `HeroSkyCanvas` `(STAR\|\|DEEP_SKY) && mag<=4.5` → `openObjectDetailById` → funFacts (5) |
| 13 | `star_car_canopus` | Hero sky canvas tap | hero subset → modal → `ExtendedFactCatalog` (5) |
| 14 | `dso_m51` | Favorites dialog | `getAllObjects()` → `openObjectDetail` → funFacts (5) |
| 15 | `dso_m13_hercules` | What's Up Tonight | `WhatsUpTonightEngine.targetObject` → modal → funFacts (5) |
| 16 | `dso_m57` | Notification deep link | `AstronomyCatalog.getById` → `openObjectDetail` → funFacts (5) |
| 17 | `planet_mars` | Notification deep link | `getById` → `openObjectDetail` → `PhysicalData` (5) |
| 18 | `sat_25544` | Satellite detail | `ISSScreen` → `SatelliteDetailScreen` renders `verifiedFacts*` (5) |
| 19 | `sat_48274` | Satellite detail | `SatelliteDetailScreen` renders `verifiedFacts*` (3, logged reason) |
| 20 | `sagittarius_a_star` | AR search ("sgr a*") | `CelestialSearchEngine` → `openObjectDetail` → funFacts (5) |

Every traced object reaches a non-empty facts card (or the satellite fact section), both
languages present, no generic/duplicated text.

---

## CI

Local Gradle/unit-test execution is impossible in this sandbox (no JDK/Gradle toolchain,
blocked network), so the corrected suite and signed release build were validated by the
`Build Android APK` workflow (JDK 21, Gradle 9.3.1, `testDebugUnitTest`, 15 required test
classes, signed release APK `ZIG`).

- Run: **`Build Android APK` #34737487246** on commit `1b685f1` → **success** in 9m19s.
- Unit suite: **393 tests, 0 failures, 0 errors, 0 skipped** (`gradle_status=0`).
- All 15 required classes **PASS**, including `ContentIntegrityAuditTest` (the full-union
  audit), `CatalogIntegrityTest`, `DeepSkyFactCoverageTest`,
  `ObjectDetailModalConsistencyTest`, and the AR/catalog test classes.
- Signed release APK: `assembleRelease` succeeded and `ZIG-release.apk` was uploaded
  (verified package `com.alijafari.red.astronomy`, versionCode 4, non-debug signing key).

## BUILD STATUS

**GREEN — zero C/D/E across the entire 336-object union** (A = 315, B = 21, C = 0, D = 0,
E = 0), both languages, cross-object duplication 0, research log complete, corrected tests
passing, and CI compile + tests + signed release APK all green.

## Artifacts

- `audit/findings.md` — this report.
- `audit/parse_catalog.py` → `audit/union_list.json` — 336-object union list with facts.
- `audit/classify.py` → `audit/classification.json` — final per-object bucket classification.
- `audit/verify.py` — 1:1 mirror of `ContentIntegrityAuditTest` (JVM-free iteration).
- `audit/gen_log.py`, `audit/gen_dso_log.py`, `audit/dso_facts_batch*.py`, `audit/gen_dso_facts.py` — research-log and fact-set generators (content source of truth).
- `app/src/test/.../ContentIntegrityAuditTest.kt` — corrected full-union audit test.

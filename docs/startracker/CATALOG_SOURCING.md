# Catalog Sourcing Documentation — Phase 3

## 1. Format Expected by CatalogIngestor

**File format:** Simple CSV with header `id,ra_deg,dec_deg,magnitude`

**Exact specification:**
- Header: first non-comment, non-empty line must be exactly `id,ra_deg,dec_deg,magnitude` (case-insensitive after trimming spaces, but we enforce exact match for simplicity)
- Columns:
  - `id`: string, unique identifier, non-empty, must not contain comma. Examples: `BSC0001`, `HIP12345`, `TESTSTAR001` (for synthetic fixtures)
  - `ra_deg`: double, right ascension in decimal degrees, J2000 equatorial, range `[0, 360)` — 0 inclusive, 360 exclusive (360 normalized to 0)
  - `dec_deg`: double, declination in decimal degrees, J2000 equatorial, range `[-90, +90]`
  - `magnitude`: double, apparent magnitude, reasonable range `[-5, 15]` — Sirius -1.46 to faint limit, rejects outside this
- Units: degrees for RA/Dec, magnitude unitless
- RA/Dec interpretation: J2000 equatorial, decimal degrees, converted internally to radians (`raRad = ra_deg * π/180`, `decRad = dec_deg * π/180`)
- Comment lines: starting with `#` ignored
- Empty lines ignored
- Validation: rejects malformed rows with clear `ParseError` (line number + reason) rather than silently skipping
- Example:
  ```
  id,ra_deg,dec_deg,magnitude
  BSC0001,0.0,0.0,2.0
  BSC0002,90.0,0.0,2.5
  ```

## 2. No Real Astronomical Data Fetched or Fabricated in This Phase

**Explicit statement:** In this phase (Phase 3), no real astronomical catalog data has been fetched from network, and no bulk catalog data has been fabricated from memory/training data.

- All test data is synthetic, clearly labeled as test fixture with fake IDs like `TESTSTAR001`, `TESTSTAR002`, etc.
- Test fixtures are hand-chosen to have easy-to-verify separations (e.g., 90° apart, 1° apart, antipodal 180°) for hand-verification.
- A handful of extremely well-known named stars (Sirius, Vega, Polaris) may be mentioned in documentation as illustrative examples, but are labeled "illustrative, not verified against a primary source" and are NOT included as bulk data.

The actual ~9,000-15,000 star dataset acquisition is explicitly a separate, network-dependent, human task that must be performed by a human with network access on a proper development machine, not in this sandboxed environment.

## 3. Candidate Real Public-Domain Sources (Named from General Knowledge, Not Verified as Currently Accessible in This Session)

**Flagged as:** Named from general knowledge, not verified as currently accessible or in this exact format in this session. A human with network access must verify current accessibility and format.

Candidate sources for bright-star extract:

- **Yale Bright Star Catalog (BSC5 / HR Catalog):** ~9,110 stars, magnitude ≤6.5, J2000, public domain, commonly available as CSV or text. Good match for target size 9k-15k. Historically used for many star trackers.
- **Hipparcos Catalog via VizieR:** ~118,000 stars, high astrometric precision, magnitude down to ~12, but can be filtered to mag ≤6.5-7.0 to get ~9k-15k bright subset. Available via VizieR (https://vizier.cds.unistra.fr/) as `I/239/hip_main` or similar. Public domain, ESA.
- **Tycho-2 Catalog:** ~2.5 million stars, but bright subset can be extracted. Less ideal than BSC5/Hipparcos for small bright extract, but available.
- **Gaia DR3 bright subset:** ~1.8 billion stars total, but bright subset mag ≤7 is ~15k-20k. Extremely precise, but larger download. Available via ESA Gaia archive.

**Recommended for this project:** Yale BSC5 (9,110 stars) as primary, Hipparcos filtered to mag ≤6.5 as alternative for higher precision. Both are public domain and commonly used for star trackers.

**Why not fabricated from memory:** Bulk star positions/magnitudes from memory would be inaccurate and not traceable to primary source. Must be downloaded from authoritative source and versioned.

## 4. Target Size / Magnitude Cutoff and Why

**Target:** ~9,000-15,000 stars, magnitude ≤6.5-7.0 per project's architecture decision (Section 6 of architecture roadmap — bright-star extract sized to what a handheld phone camera can actually detect).

**Why this size:**

- **Phone camera sensitivity:** Handheld phone camera (main camera, ~f/1.8, ISO 800-3200, exposure 1/30s to 1s) can detect stars down to magnitude ~6-7 under dark sky, maybe mag 4-5 under light pollution. Fainter stars (mag >7) are not reliably detectable, so including them in catalog wastes storage and increases false matches.
- **Star density:** At mag ≤6.5, average density = 9,110 stars / 41,253 sq deg (full sky) ≈ 0.22 stars/sq deg. A 60°×40° FOV covers ~2,400 sq deg (rectilinear approximation), so ~530 catalog stars per frame worst-case; realistically 20-100 detectable due to sensitivity and light pollution. Manageable for quad search.
- **Filtering the index builders actually apply (resolves the old '44M quads worst-case ... realistically few million' contradiction):** NONE. Verified in code and by measurement: `AngularSeparationIndex` filters pairs only by MIN/MAX_PAIR_SEPARATION (0.1-40 deg); `QuadPatternIndex` enumerates every C(N,4) combination passing the same separation window. There is no magnitude filter, no region filter, and no per-star quad cap in either builder (`MAX_STARS_PER_REGION_FOR_QUADS` is declared but unused). So the 'realistically few million' hedge had no mechanism behind it - the single real number is the measured-extrapolated ~1.28e11 quads at 9,110 stars (and 4,851,922 measured pairs). Any 'few million' outcome requires IMPLEMENTING a cap first.

- **ALGORITHMIC REFRAME (pass 3, item R3-A2):** QuadPatternIndex as written enumerates
  ALL 3-subsets of neighbours inside the 40-deg cutoff cone around each star (brute-force
  O(N^4) build; quads/star grows ∝ N^3 - measured 504/1,112/2,308/3,973 at N=300/400/500/600,
  ~14.6M at 9,110 by the exact combinatorial model). That is NOT the Tetra3-style
  k-nearest-neighbour pattern scheme the phase docs describe: CatalogBuildConfig.
  MAX_STARS_PER_REGION_FOR_QUADS is declared and NEVER READ. **Phase 3 is therefore
  'complete at fixture scale only - the real-scale index build is UNIMPLEMENTED'.**
- **Catalog size vs. performance (2026-09-03 pass-2 measurements, see evidence/CATALOG_SIZE_MEASURED_2026-09-03.txt):** MEASURED with the real builders (uniform synthetic stars, seed 42, CatalogBuildConfig defaults): pair index at 9,110 stars = 4,851,922 pairs, 7.5 s build, stars+pairs serialize to 78,094,272 B = 74.5 MiB; at 15,000 stars = 13,156,786 pairs, 201 MiB. Quad builder MEASURED at 300/400/500/600 stars (151k/445k/1.15M/2.38M quads, 79.93 B/quad, O(N⁴) build, heap-OOM at 800 stars / 2.8 GB); EXTRAPOLATED from those measured points to 9,110 stars: ~1.28×10¹¹ quads ≈ 10.2 TB serialized, ~9.5 days build (15,000: ~9.4×10¹¹ ≈ 75 TB). The pass-1 figures '3.79 GB / 6.69 GB' were NOT measurements - they were the CatalogSerializer estimator's output under the Task-4 'N×19600/4' quad model, which is now MEASURED to be ~2,900× optimistic (real trend: ~14M quads/star at 9k scale, no cap exists in code). The original '~10-30 MB' claim was ~300,000× off. NOT shippable as-is: the quad builder needs a per-region cap (CatalogBuildConfig.MAX_STARS_PER_REGION_FOR_QUADS exists but is unused) and a smaller encoding; the pair-only index (74.5 MiB @ 9k) is a shippable size on its own.
- **Comparison to existing display catalog:** ZIG's `StarCatalog.kt` has only 43 hand-written stars — this new catalog is SEPARATE, NEW asset for plate-solving, NOT merged/replaced. Display catalog remains for UI, plate-solving catalog is for solver.

**Why not larger:** Including fainter stars (mag 8-9, ~50k stars) would increase catalog size and pair/quad count quadratically, making index larger (tens of TERABYTES at the CURRENT quad representation (see the measured-extrapolated ~10.2 TB @ 9k above; quad count grows ∝ C(N,4))) and search slower, with little benefit since phone cannot detect those faint stars.

**Why not smaller:** Mag ≤5.0 gives only ~1,600 stars, too sparse — many phone images would have <4 detectable stars, failing Pyramid's requirement of 4+ true stars for lost-in-space solve.

**Conclusion:** 9k-15k stars mag ≤6.5-7.0 is sweet spot for handheld phone star tracker.

### 4b-prereq. PREREQUISITE for any real-scale quad index: implement a neighbour cap (moved here from 'Size options' pass 3)

The per-star neighbour cap is NOT a size option - it is a PREREQUISITE for building the
quad index at catalog scale at all. As written the builder is O(N^4) with no cap
(MAX_STARS_PER_REGION_FOR_QUADS declared, never read): ~9.5 days and heap-OOM at 800
stars in 2.8 GB (measured). Implementing a Tetra3-style cap (k nearest / brightest
neighbours per star, quads = star + C(k,3) triples) makes quads/star EXACTLY C(k,3) and
the cost, at the MEASURED 79.9 B/quad for 9,110 stars:

  k=5: C(5,3)=10  ->  91,100 quads  ->   7.3 MB   (+ the measured 74.5 MiB pair section)
  k=6: C(6,3)=20  -> 182,200 quads  ->  14.6 MB
  k=8: C(8,3)=56  -> 510,160 quads  ->  40.8 MB

i.e. a capped index lands in the TENS OF MB. For the record: the original "10-30 MB"
doc guess was roughly right FOR A CAPPED INDEX - it was wrong in fact only because the
cap was never implemented (uncapped reality: ~10.7 TB at 9,110 stars by the exact
combinatorial model, evidence/CATALOG_SIZE_MEASURED_2026-09-03.txt addendum). A cap
changes recall (some true quads missing) and must be validated against solve success
rate - it is new algorithm work, deliberately NOT implemented in the remediation passes.

### 4a. Size options (OPTIONS, not decisions - measured unit costs so a reader can do the arithmetic)

Measured serialized unit costs (real serializer format, see
docs/startracker/evidence/CATALOG_SIZE_MEASURED_2026-09-03.txt):
  star  ~50.9 B   (id string + 3 doubles + source string; grows with id length)
  pair  = 16 B    (double + int + int)
  quad  ~79.93 B  (4 ints + 6 doubles + quantized-key string)
  pair count @ 9,110 uniform stars: 4,851,922 MEASURED (~0.0585 x N^2 ... exactly: 11.7% of C(N,2) lie within the 40 deg window)
  quad count @ 9,110: ~1.33e11 by the exact combinatorial model f(40deg)*N*C(k,3), f measured (no cap in code) - ~10.7 TB

- **Option A - star list only, build indexes on device at first launch.**
  Ship just stars: measured 463,504 B for 9,110 synthetic stars (~0.46 MB; i.e. ~50.9 B/star,
  dominated by string ids - integer ids would cut it to ~32-36 B/star => ~0.33 MB).
  Cost: on-device pair build measured at 7.5 s for 9,110 stars (JVM, single thread) - a
  one-time launch cost; the quad build is NOT viable on device as implemented (O(N^4),
  would take days), so this option only works paired with the capped quad strategy (PREREQUISITE 4b-prereq above).
- **Option C - smaller numeric encodings.** float32 instead of float64 for
  separations/ratios halves the doubles (16 B of the quad's 80 B); ratio quantization to
  uint16 (ratios are in [0,1]) saves ~24 B more; dropping the string quantizedKey from
  the serialized form (recomputable from ratios at load) saves ~13-18 B/quad. Combined
  floor roughly ~24-40 B/quad. Pairs could drop the double (recomputable) -> 8 B/pair.
- **Option D - pair index only (74.5 MiB @ 9,110, MEASURED), derive quads at runtime
  from the pair graph.** Runtime quad derivation from pairs is exactly what the solver's
  QuadCandidateBuilder already does for observations; the same approach on the catalog
  side avoids materializing the C(N,4) blow-up entirely.

These are options for the eventual implementer, not decisions taken in this pass.

## 5. Next Steps for Human with Network Access

1. Download Yale BSC5 or Hipparcos catalog from VizieR or other public source.
2. Filter to magnitude ≤6.5 (or 7.0) to get ~9k-15k stars.
3. Convert to CSV format `id,ra_deg,dec_deg,magnitude` per spec above (RA in degrees 0-360, Dec -90 to +90, J2000).
4. Validate with `CatalogIngestor.parse()` — should parse without errors.
5. Build the offline pair index via `AngularSeparationIndex` with `CatalogBuildConfig` defaults (MEASURED at 9,110 stars: 4,851,922 pairs, 7.5 s, 74.5 MiB - safe to run). Do NOT build `QuadPatternIndex` at catalog scale with current defaults: it is uncapped O(N^4) - measured ~9.5 days extrapolated and heap-OOM at just 800 stars. Implement the neighbour cap first (see 'PREREQUISITE' 4b-prereq in Section 4; capped cost lands in tens of MB).
6. Serialize via `CatalogSerializer.serialize()` to binary asset, ship as Android asset (e.g., `app/src/main/assets/startracker_catalog.bin`).
7. Measure the real file size on the real (BSC5) star list. For reference, synthetic-uniform measurements already exist: pair index 74.5 MiB @ 9,110 stars (see docs/startracker/evidence/CATALOG_SIZE_MEASURED_2026-09-03.txt); the quad index is infeasible as implemented (~10.2 TB extrapolated) and must not be built without a per-region cap.

## 6. Illustrative Examples (Not Verified Against Primary Source)

For documentation only, clearly labeled as illustrative:

- Sirius: RA ~101.287°, Dec ~-16.716°, mag -1.46 (brightest star, illustrative, not verified)
- Vega: RA ~279.235°, Dec ~38.784°, mag 0.03 (illustrative)
- Polaris: RA ~37.95°, Dec ~89.264°, mag 1.98 (illustrative)

These are recalled approximately from general knowledge, not verified against primary source, and must NOT be used as bulk catalog data.

## 7. References

- Architecture roadmap Section 6: bright-star extract sized to what phone camera can detect
- Yale Bright Star Catalog (BSC5) — commonly known identity, not verified as currently accessible in this session
- Hipparcos catalog via VizieR — commonly known identity, not verified as currently accessible
- Tetra3 paper (for quad index design) — geometric hash approach
- Mortari k-vector paper (for pair index) — range search structure

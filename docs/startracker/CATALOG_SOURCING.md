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
- **Star density:** At mag ≤6.5, average density ~0.2 stars per square degree (9k stars / 41253 sq deg full sky). For phone FOV ~60° diagonal (~2500 sq deg? Actually 60°×40° FOV ≈ 2400 sq deg), expected ~500 stars per frame worst-case, but realistically 20-100 detectable due to sensitivity and light pollution. Manageable for quad search.
- **Catalog size vs. performance:** 9k stars → pairs within 40° cutoff ~ N²×0.058 ≈ 4.7M pairs (per earlier extrapolation), quads nearby limited ~ N×19600/4 ≈ 44M quads worst-case, but with magnitude and region filtering, realistically few million. Binary file size estimated ~10-30 MB (see Task 4 extrapolation), acceptable as Android asset.
- **Comparison to existing display catalog:** ZIG's `StarCatalog.kt` has only 43 hand-written stars — this new catalog is SEPARATE, NEW asset for plate-solving, NOT merged/replaced. Display catalog remains for UI, plate-solving catalog is for solver.

**Why not larger:** Including fainter stars (mag 8-9, ~50k stars) would increase catalog size and pair/quad count quadratically, making index larger (100+ MB) and search slower, with little benefit since phone cannot detect those faint stars.

**Why not smaller:** Mag ≤5.0 gives only ~1,600 stars, too sparse — many phone images would have <4 detectable stars, failing Pyramid's requirement of 4+ true stars for lost-in-space solve.

**Conclusion:** 9k-15k stars mag ≤6.5-7.0 is sweet spot for handheld phone star tracker.

## 5. Next Steps for Human with Network Access

1. Download Yale BSC5 or Hipparcos catalog from VizieR or other public source.
2. Filter to magnitude ≤6.5 (or 7.0) to get ~9k-15k stars.
3. Convert to CSV format `id,ra_deg,dec_deg,magnitude` per spec above (RA in degrees 0-360, Dec -90 to +90, J2000).
4. Validate with `CatalogIngestor.parse()` — should parse without errors.
5. Build offline index via `AngularSeparationIndex` and `QuadPatternIndex` with `CatalogBuildConfig` defaults.
6. Serialize via `CatalogSerializer.serialize()` to binary asset, ship as Android asset (e.g., `app/src/main/assets/startracker_catalog.bin`).
7. Measure real file size and compare to extrapolation in Task 4.

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

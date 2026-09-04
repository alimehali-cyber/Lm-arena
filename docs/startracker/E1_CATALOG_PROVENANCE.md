# E1 — Real star catalog provenance (OD5), 2026-09-04

## Derived catalog (committed)

`data/startracker/hyg_v36_vle6.5_j2000.csv` — 8,870 stars, V ≤ 6.5, J2000 equatorial,
format `id,ra_deg,dec_deg,magnitude` (exact `CatalogIngestor` input contract).
SHA-256: `fbfc6f8910235f16f4340401907be2eb68bce6373ed7b9deb11a5f18f4deea38`
Transformation from raw: skip non-HIP rows (drops Sol), mag(V) ≤ 6.5 filter,
`ra_hrs × 15 → deg`, 6/6/3-decimal formatting, ids `HIP<n>` (unique, verified).
The `mag` column of HYG is Hipparcos V-band.

## Raw source

- **URL**: `https://codeload.github.com/astronexus/HYG-Database/refs/heads/main/hyg/v3/hyg_v36.csv.gz`
  (also reachable via `gh api repos/astronexus/HYG-Database/contents/hyg/v3/hyg_v36.csv.gz
  -H "Accept: application/vnd.github.raw+json"` — the plain codeload URL 404s from this
  sandbox; the gh api raw fetch is the working method)
- **Retrieved**: 2026-09-04T05:58Z (UTC)
- **SHA-256 (gz)**: `784fd90e767c5f4fc32a084adfb41618f3f80a55e55cf2c65baf8107fa881468`
- **SHA-256 (csv)**: `8b2d5bed0abb650630af5c64ea694506dd3246e00f8775694525de9e1c826d84`
- **Size**: 13,439,843 bytes gz / 119,614 rows + header
- **Local copy (git-ignored)**: `data/startracker/raw/hyg_v36.csv.gz`
- **Licence**: **CC BY-SA 4.0** (repo LICENSE file; GitHub reports NOASSERTION because
  it is not an OSI licence). Attribution required — see "Attribution" below.
- **Provenance chain**: HYG v3.6 ← Hipparcos + Yale BSC5 + Gliese (per README).

⚠ HYG v3.6 `ra` column is in **hours**, not degrees (v3.x format change). Any consumer
must multiply by 15. (Already applied in the derived CSV.)

## Cross-checks performed (B3, MEASURED)

- HYG v36 vs Yale BSC5 (independent) on the 43 display stars: agreement ≤ 2.4″ on all
  corrected objects, ≤ ~20″ worst-case overall (α Cen, proper-motion epoch effect).
  `docs/startracker/B3_CATALOG_VERIFICATION.md`.

## Attribution (for F runbook / About UI if HYG data ships in the app)

> Star data from the HYG database (HYG v3.6), © astronexus.com, licensed under
> Creative Commons Attribution-ShareAlike 4.0 (CC BY-SA 4.0). Based on data from the
> Hipparcos, Yale Bright Star, and Gliese catalogues.

CC BY-SA 4.0 note: ShareAlike applies to the *dataset/derivative database*, not to app
code that merely reads it; attribution text above must ship with any distribution that
includes the derived CSV.

## Secondary sources (used for verification only, not shipped)

- Yale Bright Star Catalog JSON: `github.com/brettonw/YaleBrightStarCatalog` bsc5.json
  (9,096 stars) — retrieved 2026-09-04T05:59Z.
- OpenNGC `database_files/NGC.csv`: SHA-256 `be150bdaa1997dacbcb39f303074403edec7a953b589b36d5f1c4522c0cc6fae`
  — retrieved 2026-09-04T05:59Z, licence CC BY-SA 4.0 (per repo).
- HYG `misc/dso.csv` (19,487,406 bytes) — same retrieval, same licence family.

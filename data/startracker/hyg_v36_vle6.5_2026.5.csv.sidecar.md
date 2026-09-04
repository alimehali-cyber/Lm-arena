# PM-propagated extract sidecar — hyg_v36_vle6.5_2026.5.csv (Z-V6, 2026-09-04)

- **Source**: HYG v3.6 raw `hyg/v3/hyg_v36.csv.gz` fetched fresh 2026-09-04,
  SHA-256 `784fd90e767c5f4fc32a084adfb41618f3f80a55e55cf2c65baf8107fa881468`
  (matches the originally recorded hash — upstream unchanged).
- **Selection**: identical to the J2000 extract — `hip` non-empty AND `mag` ≤ 6.5 →
  8,870 stars. (NB: the HYG `id` column is HYG's own row id, NOT the HIP number;
  selection and ids use the `hip` column.) The J2000 extract was re-derived from this
  exact gz byte-for-byte (SHA-256 verified) before propagation.
- **Propagation**: straight-line proper motion, epoch J2000.0 → 2026.5 (Δt = 26.5 yr):
  `dec1 = dec0 + pmdec·Δt`, `ra1 = ra0 + (pmra/cos(dec0))·Δt` (degrees; pm in mas/yr;
  HYG `pmra` follows the μα·cos δ convention). No radial-velocity/parallax perspective
  terms (negligible: shifts are ≤ 3′; perspective acceleration over 26.5 yr ≪ 1″ for
  all but the nearest stars and HYG lacks RV for many rows).
- **File**: same 4-column layout (`id,ra_deg,dec_deg,magnitude`), ids `HIP####`,
  positions rounded to 1e-6 deg (≈ 0.004″). SHA-256 of the 2026.5 file:
  `74be9bf7f7b7a4471df7af53a2062259c994f8e8ca6fed09af7d409520be42d7`;
  J2000 file unchanged: `fbfc6f8910235f16f4340401907be2eb68bce6373ed7b9deb11a5f18f4deea38`.
- **Which file does the index build use?** The J2000 file
  (`data/startracker/hyg_v36_vle6.5_j2000.csv`) — it is the file referenced by
  CappedQuadIndexTest, SyntheticE2ETest, the harness probes, and the D-ladder runs.
  The 2026.5 file is ADDED alongside, not swapped in: all published startracker
  numbers (quad index, D ladder, S-series) remain J2000-based. Swap-in is deferred to
  a device-trial decision (magnitude of the effect below: median < 1″ vs ~57″/px —
  sub-centroid-pixel for 99.9% of stars).
- **Shift statistics 2000 → 2026.5** (great-circle = hypot(pmra,pmdec)·Δt):
  median 0.96″, max 187.0″ (Groombridge 1830); 223 stars shift > 10″,
  11 stars > 60″ (1′). At the harness pixel scale (63.5° FOV / 1080 px ≈ 3.8′/px)
  every star except those 11 moves well under a tenth of a pixel.

## 10 largest 2000→2026.5 shifts

| id | name | Vmag | shift |
|---|---|---|---|
| HIP57939 | Groombridge 1830 | 6.42 | 187.0″ |
| HIP104214 | (61 Cyg A) | 5.20 | 139.9″ |
| HIP104217 | (61 Cyg B) | 6.05 | 137.1″ |
| HIP108870 | (Lacaille 8760) | 4.69 | 124.7″ |
| HIP19849 | Keid (40 Eri A) | 4.43 | 108.3″ |
| HIP5336 | (HIP 5336) | 5.17 | 100.1″ |
| HIP71683 | Rigil Kentaurus (α Cen A) | −0.01 | 98.3″ |
| HIP71681 | Toliman (α Cen B) | 1.35 | 98.3″ |
| HIP15510 | 82 G. Eri | 4.26 | 82.8″ |
| HIP12114 | 268 G. Cet | 5.79 | 61.3″ |

Sanity anchors: α Cen 98.3″/26.5 yr = 3.71″/yr and Gmb 1830 187.0″/26.5 yr = 7.06″/yr
match their catalogue proper motions.

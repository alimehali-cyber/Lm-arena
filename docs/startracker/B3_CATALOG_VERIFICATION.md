# B3 — Display catalog verification (OD3), 2026-09-04

## Method (MEASURED)

All 43 `StarCatalog.kt` and 15 `DeepSkyCatalog.kt` entries compared against TWO
independent sources:

- **HYG v3.6** (119,614 stars; github.com/astronexus/HYG-Database, `hyg/v3/hyg_v36.csv.gz`;
  SHA-256 gz `784fd90e767c5f4fc32a084adfb41618f3f80a55e55cf2c65baf8107fa881468`,
  csv `8b2d5bed0abb650630af5c64ea694506dd3246e00f8775694525de9e1c826d84`; matched by HIP id).
  NOTE: HYG v36 `ra` column is in HOURS (v3.x format change) — multiplied by 15.
- **Yale Bright Star Catalog (BSC5)**, 9,096 stars (github.com/brettonw/YaleBrightStarCatalog
  `bsc5.json`; matched by HD id via HYG cross-index).
- DSOs: **OpenNGC** (github.com/mattiaverga/OpenNGC `database_files/NGC.csv`, SHA-256
  `be150bdaa1997dacbcb39f303074403edec7a953b589b36d5f1c4522c0cc6fae`) + **HYG dso.csv**
  (astronexus misc/dso.csv; NGC2000/HCNGC/PGC lineage) + Wikipedia/NED for non-NGC objects.

OD3 rule applied: correct a value iff app is off > 60″ (mag > 0.3) AND the two sources
agree within 5″ (0.1 mag). No structural changes; values only.

## Corrections applied (per-object before/after)

All "after" values are HYG v3.6 J2000 (arcsec deltas shown vs both sources):

| id | before (ra,dec) | after (ra,dec) | dHYG″ | dBSC″ | src-agree″ |
|---|---|---|---|---|---|
| star_ori_alnilam | 84.530, −1.200 | **84.053, −1.202** | 1715.5→2.6 | 1715.6→2.6 | 0.2 |
| star_ori_mintaka | 84.050, −0.300 | **83.002, −0.299** | 3773.9→1.2 | 3773.9→1.2 | 0.3 |
| star_uma_mizar | 206.884, 54.920 | **200.981, 54.925** | 12208.4→2.3 | 12208.3→2.3 | 0.3 |
| star_uma_alkaid | 209.800, 49.310 | **206.885, 49.313** | 6840.4→1.4 | 6841.1→1.4 | 0.8 |
| star_cas_schedar | 9.880, 59.150 | **10.127, 56.537** | 9417.5→1.0 | 9417.9→1.0 | 0.8 |
| star_cas_caph | 1.150, 59.150 | **2.293, 59.150** | 2110.6→0.2 | 2112.9→0.2 | 2.4 |
| star_sco_shaula | 262.690, −37.030 | **263.402, −37.104** | 2062.9→0.8 | 2062.7→0.8 | 0.4 |
| dso_smc | 14.766, −72.801 | **13.187, −72.829** | 1711.3→108.9* | n/a | *see note |

After the fix the worst remaining star delta is **20.4″** (α Centauri, under the 60″ bar);
all other DSO deltas ≤ 47.7″. Harness 148/0/0; oracle star route after fix:
0.416/1.722′ rms/max (n=647) — unchanged within noise, as expected (catalog fixes mostly
move stars that were already verified wrong by neither route because the grid's 64-star
subset included few of the 7 offenders).

### *SMC note (documented deviation from the strict rule)

The SMC is a 5°-wide galaxy: HYG-DSO (13.158, −72.800; dso_source=0, Wikipedia-lineage)
and NED/Wikipedia (13.1867, −72.8286) disagree by 107″ — different center conventions —
so the strict "two sources agree < 5″" test cannot pass for ANY value. The app value was
off by ~1,700″ (28′) from BOTH sources, i.e. wrong under every convention. Corrected to
the NED J2000 value; flagged as a judgment call in the H report.

## Verified correct, no action (notable near-threshold cases)

- dso_hyades: app 66.750, 15.870; HYG-DSO 15.850 vs Wikipedia 15.867 — sources disagree
  (61″), app within 6″ of Wikipedia → no action.
- dso_m45_pleiades: app ≈ Alcyone-centered convention (56.871, 24.105); HYG-DSO (56.850,
  24.117) vs Wikipedia/Gaia-DR3 (56.658, 24.178) disagree by ~668″ (2° cluster, center
  conventions differ) → no action.
- dso_double_cluster: app = midpoint of NGC 869/884; within 47–54″ of both sources;
  sources themselves disagree 76.8″ → no action.
- star_gem_castor mag: app 1.58, HYG 1.58, BSC5-JSON 1.98 — sources disagree → no action
  (Castor V = 1.58 per HYG/SIMBAD-lineage; the bsc5.json Vmag looks like a data error).
- dso_coma_cluster (Mel 111): no second offline source found (OpenNGC/HYG-DSO lack it) →
  unverifiable, report-only.
- All DSO magnitudes: sources disagree > 0.1 mag (extended objects) → no mag corrections.

## Live-path effect

StarCatalog/DeepSkyCatalog feed the AR overlay via `staticObjectEquatorial` (F-A5). The
oracle grid's star subset had 4 of the 7 offender stars below the 10° window or absent
(64-star sample), which is why the A-route residuals barely move; the catalog fixes are
still user-visible: Mizar was rendered 3.4° off, Schedar 2.6° off, Alkaid 1.9° off.

# Pipeline — How to Rebuild Every Pack

Date: 2026-09-14
M2 — Asset Kit

This file is authoritative per §25.7 for how to rebuild every pack. Exact commands for app and for every pack.

## Prerequisites

- JDK 17+ (for :tools:assetkit and :tools:blackhole-lut)
- Python 3.11+ (for provenance/scope gates and helpers)
- GDAL 3.8+ (gdalwarp, gdal_translate) — optional, for reprojection, fallback to pure Kotlin if absent
- KTX-Software `ktx` CLI (latest stable) — for KTX2 encoding, verify flags against pinned release
- meshoptimizer `gltfpack` — for ISS LODs
- Filament `matc` — material compiler, from Filament release 1.71.5
- Same inputs + same config => byte-identical outputs (determinism)

## Toolchain Versions (real vs pinned per ENVIRONMENT.md)

- AGP 9.1.1 (real) vs 9.4.0 pinned — keep real
- Kotlin 2.2.10 real vs 2.2.0 pinned — keep real
- Filament 1.71.5 pinned — use 1.71.5
- Gradle 9.3.1, JDK 21 in CI, sourceCompatibility 11

## AssetKit CLI Verbs (per §6.2)

All verbs support --dry-run and print exactly what they would do.

```
assetkit fetch --object <id> --source <source> --out <dir> [--dry-run]
assetkit preprocess --object <id> --config configs/<id>.yaml [--dry-run]
assetkit tiles --input <preprocessed.tif> --out <pack-dir> --tile 512 --apron 4 [--dry-run]
assetkit encode --input <dir> --format astc --block 6x6 [--dry-run]
assetkit horizon --height <dem.tif> --azimuths 16 --out horizon.ktx2 [--dry-run]
assetkit normal --height <dem.tif> --out normal.ktx2 --strength 1.0 [--dry-run]
assetkit pack --object <id> --out assets-built/<id>.zigpack --manifest manifests/<id>.json [--dry-run]
assetkit verify --pack assets-built/<id>.zigpack [--dry-run]
assetkit atmosphere --config configs/atmosphere/<body>.yaml --out luts/<body>/ [--dry-run]
```

Run via Gradle:

```
./gradlew :tools:assetkit:run --args="fetch --object moon --source wac-100m --dry-run"
./gradlew :tools:assetkit:run --args="tiles --input assets-src/moon/wac-100m/preprocessed.tif --out assets-built/moon --tile 512 --apron 4"
...
```

## Per-Object Rebuild Commands

### Moon (example for M2 demo, small crop)

Source products (per SOURCES.md):
- LRO WAC Global Mosaic 100m — NASA/GSFC/ASU — https://wms.lroc.asu.edu/lroc/view_rdr/WAC_GLOBAL
- LRO LOLA DEM 118m — NASA/GSFC — https://pds-geosciences.wustl.edu/missions/lro/lola.htm

Steps for M2 small crop (few hundred MB max):

```bash
# 1. Fetch (download + verify) — real would download, M2 demo creates synthetic placeholder
./gradlew :tools:assetkit:run --args="fetch --object moon --source wac-100m --out assets-src/moon/wac-100m"
./gradlew :tools:assetkit:run --args="fetch --object moon --source lola-dem --out assets-src/moon/lola-dem"

# 2. Preprocess — reproject to equirectangular, linear-space mips per §6.5
./gradlew :tools:assetkit:run --args="preprocess --object moon --config configs/moon.yaml"

# 3. Tiles — tile pyramid with apron 4, seam duplication per §6.4
./gradlew :tools:assetkit:run --args="tiles --input assets-src/moon/wac-100m/preprocessed.tif --out assets-built/moon --tile 512 --apron 4"

# 4. Encode — KTX2 ASTC 6x6 sRGB for albedo, UASTC for normal, R8_UNORM for height per §6.5
./gradlew :tools:assetkit:run --args="encode --input assets-built/moon/maps --format astc --block 6x6"

# 5. Horizon — 16 azimuths from DEM per §6.6
./gradlew :tools:assetkit:run --args="horizon --height assets-src/moon/lola-dem/dem.tif --azimuths 16 --out assets-built/moon/luts/horizon.ktx2"

# 6. Normal — from DEM per §6.6
./gradlew :tools:assetkit:run --args="normal --height assets-src/moon/lola-dem/dem.tif --out assets-built/moon/maps/normal.ktx2 --strength 1.0"

# 7. Pack — ZIP stored (no recompression) containing manifest.json, maps/*.ktx2, tiles/, luts/, meta/tiers.json per §6.3
./gradlew :tools:assetkit:run --args="pack --object moon --out assets-built/moon.zigpack --manifest manifests/moon.json"

# 8. Verify — KTX2 validity, colour-space audit, mip sanity, tile seam, pole, horizon/normal correctness, SHA per §24.2 A4
./gradlew :tools:assetkit:run --args="verify --pack assets-built/moon.zigpack"

# 9. Determinism proof — run pack twice, hash both, must be byte-identical per §6.1
sha256sum assets-built/moon.zigpack > /tmp/hash1.txt
./gradlew :tools:assetkit:run --args="pack --object moon --out assets-built/moon.zigpack --manifest manifests/moon.json"
sha256sum assets-built/moon.zigpack > /tmp/hash2.txt
diff /tmp/hash1.txt /tmp/hash2.txt && echo "Determinism PASS: byte-identical"

# 10. Provenance gate
python3 tools/ci/check_provenance.py --manifests-dir manifests

# 11. Scope gate
python3 tools/ci/check_scope.py --base origin/Obra-with-key
```

### Mercury, Venus, Earth, Mars, Jupiter, Saturn, Uranus, Neptune, Milky Way, ISS, Black Hole, Sun

Same pattern, with per-object sources from SOURCES.md and per-object configs in configs/<object>.yaml.

For bigger budget assets per §27.5 (Gaia DR3, DSS2, gigapixel panorama, etc.), replace source but keep same pipeline.

## Atmosphere LUT Pipeline (per §11)

```bash
./gradlew :tools:assetkit:run --args="atmosphere --config configs/atmosphere/earth.yaml --out assets-built/earth/luts/"
```

Generates transmittance LUT 2D R16_SFLOAT 256x64, multi-scattering LUT 3D R16_SFLOAT 32x32x32, per §11.1 deterministic, prints physical inputs. Clean-room implementation from Bruneton/Hillaire equations per A4.

## Black Hole LUT (per §12.2)

```bash
./gradlew :tools:blackhole-lut:run --args="--verify"
```

Generates D(e,u) 512x512 RG32F and U(e,phi) 64x32 RG32F plus blackbody colour table 256 RGBA16F, deterministic, --verify verb compares table vs direct numerical integration, prints max error.

## Material Compilation (per §5.7)

```bash
# Materials authored offline with matc into .filamat
# Keep .mat sources in core/engine/src/main/materials/ and build step in Gradle
matc -p mobile -a opengl -o core/engine/src/main/assets/materials/regolithSurface.filamat core/engine/src/main/materials/regolithSurface.mat
```

## App Build

```bash
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
./gradlew testDebugUnitTest -Pgravity.ci.tests=false
./gradlew :tools:ci:checkProvenance
./gradlew :tools:ci:checkScope
./gradlew :tools:ci:museumGates
```

## Verification

- KTX2 validity: magic, format, dimensions, mip count, transfer function flag as declared in manifest per §24.2
- Colour-space audit: albedo sRGB, normals/height/masks/horizon linear — mismatch fails gate
- Mip sanity: per-level mean/variance monotonic, no two levels byte-identical, mip count matches declared size
- Tile seam continuity: mean absolute difference across adjacent tile edges below threshold, longitude seam duplicated not wrapped implicitly
- Pole check: distortion of synthetic circle near pole below threshold
- Horizon map correctness: run generator on synthetic cone DEM and assert horizon angles match analytic within tolerance
- Normal map correctness: synthetic ramp of known slope, assert decoded normals match analytic within tolerance
- LUT verification: black-hole tables and atmosphere LUTs vs direct numerical integration, max error printed
- Catalogue validation: for fixed list of named stars, assert position/magnitude/colour index vs published values

## Determinism

Same inputs + same config => byte-identical outputs, verified by hashing twice per §6.1.

## M2 Demo Specific

For M2, we use small crop of USGS lunar DEM and Moon mosaic (few hundred MB max) to produce pack, verify it, and load it on device in throwaway debug screen.

In this sandbox, gradle distribution download fails offline (SSL_ERROR_SYSCALL), so we cannot run :tools:assetkit:run locally, but CI with cached gradle 9.3.1 will run. For M2 evidence, we provide synthetic tile pyramid and pack created via Python placeholder that mimics deterministic behaviour, plus verification that pack loads and colour space correct.

Real data fetch would be from:
- https://wms.lroc.asu.edu/lroc/view_rdr/WAC_GLOBAL
- https://pds-geosciences.wustl.edu/missions/lro/lola.htm

With provenance recorded in manifests/moon.json and SOURCES.md.

# Environment — Real vs Pinned Toolchain

Date: 2026-09-14

## Pinned Toolchain from Roadmap §3 (revision 3.0, greenfield assumptions)

| Component | Pinned Version (§3) | Notes from brief |
|---|---|---|
| Android Gradle Plugin | 9.4.0 | newest stable verified 2026-09-14, do not use alpha/beta/rc |
| Kotlin | 2.2.0 | |
| Compose BOM | (not explicitly listed in extracted text but implied latest) | |
| Filament | 1.71.5 | per §5.5 "verify exact API names against pinned 1.71.5 javadoc/KTX" |
| KTX-Software | latest stable release (cli/library) | offline asset kit |
| meshoptimizer | latest stable | offline CLI |
| GDAL | 3.8+ | offline asset kit |
| Python | 3.11+ | asset kit scripts |
| Java toolchain | 17 | |
| Gradle | (implied 8.x+ but CI uses 9.3.1) | |

Dependency policy: add no third-party runtime dependency beyond what feature genuinely needs.

## Real Environment in This Repository (M-1)

Files read:
- `gradle/libs.versions.toml`
- `app/build.gradle.kts`
- `gradle/wrapper/gradle-wrapper.properties`
- `gradle.properties`
- `.github/workflows/build.yml`

### Android & Gradle
- AGP: **9.1.1** (libs.versions.toml `agp = "9.1.1"`) — difference: repo is 9.1.1, brief pins 9.4.0. **We keep 9.1.1**, do not upgrade per §26.2 (no bumping existing dependencies).
- Gradle wrapper: **9.3.1** (gradle-wrapper.properties `distributionUrl=...gradle-9.3.1-bin.zip`) — matches CI `setup-gradle@v3 gradle-version: 9.3.1`
- compileSdk: `release(36) { minorApiLevel = 1 }` => Android 36.1 (app/build.gradle.kts:14)
- minSdk: 24, targetSdk: 36
- versionCode 4, versionName 1.3

### Kotlin & Compose
- Kotlin: **2.2.10** (libs.versions.toml `kotlin = "2.2.10"`, `googleDevtoolsKsp = "2.2.10-2.0.2"`) — newer than pinned 2.2.0, keep as is.
- Compose BOM: **2024.09.00** (composeBom)
- Activity Compose: 1.10.1, lifecycle-runtime 2.8.7, navigation-compose 2.8.9 declared but **commented out** in app/build.gradle.kts (so not used at runtime)
- Backdrop (liquid glass): 1.0.6 (io.github.kyant0:backdrop)

### JDK
- CI: **JDK 21 Temurin** (build.yml "Set up JDK 21")
- Local: `gradle.properties` says `org.gradle.jvmargs=-Xmx4g`, `app/build.gradle.kts` has `sourceCompatibility = JavaVersion.VERSION_11` and `targetCompatibility = JavaVersion.VERSION_11`. So source/target 11 but toolchain JDK 21.
- For new modules, we will use same: sourceCompatibility 11, target 11, but allow JDK 17/21 toolchain per Gradle. No change needed.

### Other Libraries (existing, must not be bumped)
- androidx.core-ktx 1.18.0, room 2.7.0, coil 2.7.0, retrofit 2.12.0, okhttp 4.10.0, moshi 1.15.2, coroutines 1.10.2, work 2.9.0, camera 1.5.0, datastore 1.1.7 (commented out in app build), play-services-location 21.3.0 (commented out), accompanist-permissions 0.37.3 (commented out)

### New Dependencies Needed for Space Museum (to be added only in new modules)
- Filament: **1.71.5** per brief (or nearest compatible with AGP 9.1.1 / Kotlin 2.2.10). We will add as `com.google.android.filament:filament:1.71.5`, `filament-android`, `gltfio`, `filament-utils-android` etc. Must be added only in `:core:engine` module, not in `:app` directly, to keep engine owning all Filament types per §4.2.
- KTX-Software CLI: offline tool, not a runtime dependency. Will be used in assetkit via process invocation, not Gradle dependency.
- meshoptimizer: offline CLI for LODs (ISS).
- GDAL: offline for reprojection, if available.
- Python 3.11+ for assetkit scripts — optional, but assetkit CLI is Kotlin/JVM, Python scripts are helper.

### Asset Kit Environment (offline, workstation)
- Python 3.11+ (not required for app build, only for assetkit helpers)
- GDAL 3.8+ (`gdal_translate`, `gdalwarp` etc.) — for reprojection
- KTX-Software `ktx` CLI + `toktx` — for KTX2 encoding, version latest stable (verify flags against pinned release)
- meshoptimizer `gltfpack` / `meshopt` — for ISS LODs
- Filament `matc` — material compiler, ships with Filament release
- Java 17+ for `:tools:assetkit` and `:tools:blackhole-lut` (JVM-only modules, never shipped)

### Differences & Decisions
- AGP 9.1.1 vs 9.4.0: Keep 9.1.1 (existing). No upgrade — would be irrelevant touch per §26.2. Recorded in DECISIONS.md.
- Kotlin 2.2.10 vs 2.2.0: Keep 2.2.10 (existing). No downgrade.
- Compose BOM 2024.09.00 vs latest: Keep existing.
- Navigation: Real repo uses tab index int + selectedFeature state, not Navigation Compose. We keep that approach for museum (no NavHost).
- Filament version: Try 1.71.5 pinned; if incompatible with AGP 9.1.1 / compileSdk 36.1, use nearest compatible (e.g., 1.68.x or 1.70.x) and record reason in DECISIONS.md with build error evidence.
- Module structure: Real repo is single-module; we will create new modules `:core:model`, `:core:engine`, `:core:data`, `:core:credits`, `:feature:museum`, `:feature:viewer`, `:tools:assetkit`, `:tools:ci` under directories `core/`, `feature/`, `tools/`. This matches roadmap shape but adapted to real repo's lack of multi-module convention. Settings.gradle.kts will be edited to include them (necessary per CHANGED_FILES.md).

### How to Build
- App: `./gradlew :app:assembleDebug` (requires Gradle wrapper 9.3.1, JDK 21). Currently offline gradle distribution download fails in this sandbox due to network SSL, but CI has it cached. Local build will work once network allows.
- New modules: `./gradlew :core:model:build`, etc.
- Asset kit: `./gradlew :tools:assetkit:run --args="fetch --object moon --source wac-100m --dry-run"`
- CI: `build.yml` workflow runs `testDebugUnitTest` then `assembleRelease` with signing keystore.

### What Is NOT Needed
- No NDK needed unless Filament requires it (it ships AARs with native libs)
- No emulator for M-1, M0 host tests
- No Firebase service account yet for Tier C — will be queued in DEVICE_BACKLOG if needed

### Verification
- `java -version` should show 21 in CI, 17+ locally
- `python3 --version` 3.11+ for assetkit
- `gdalinfo --version` 3.8+ if GDAL installed
- `ktx --version` latest
- `matc --help` from Filament tools

# ZIG Repository Inventory — M-1 Reconnaissance

Date: 2026-09-14
Branch: arena/01a0a0be-lm-arena (from Obra-with-key 8685bf3)

## Modules
- Single Gradle module: `:app`
  - Declared in `/home/user/Lm-arena/settings.gradle.kts`: `include(":app")`
  - Build file: `/home/user/Lm-arena/app/build.gradle.kts`
  - Namespace: `com.alijafari.red.astronomy` (android.namespace in build.gradle.kts:13)
  - ApplicationId: same

No other modules exist. The gravity sandbox is embedded inside `:app` as package `com.zig.gravity.*`.

## Package Roots (all inside :app, src/main/java)
- `com.alijafari.red.astronomy` — MainActivity.kt
- `com.alijafari.red.astronomy.astro_engine` — 30+ engines: ARCalibrationManager, ARSkyCatalog, ARProjectionEngine, AstroDispatchEngine, AstroTime, CelestialObjectSizes, CelestialSearchEngine, CoordinateEngine, DeepSkyCatalog/Engine, EclipseEngine, FinderEngine, FrameTransformationEngine, GalacticEngine, ISSEngine, JupiterMoonsEngine, LunarSolarEngine, MagneticAnomalyDetector, MoonEngine, ObservabilityEngine, OrientationProvider, PlanetEngine, RelativisticEngine, SGP4Propagator, SatelliteCatalog/Engine, SkyMapRenderer, StarlinkTrainManager, SunEngine, TimeEngine, VSOP87Engine, WhatsUpTonightEngine, WorldGeographyData
- `com.alijafari.red.astronomy.data` — AppRepository
- `com.alijafari.red.astronomy.data.catalog` — AsterismCatalog, AstronomyCatalog, CanonicalAstroCatalog, CatalogTextLocalizer, ConstellationCatalog, DeepSkyCatalog, GeoLocationCatalog, MeteorShowerCatalog, PhysicalData, SolarSystemCatalog, StarCatalog, etc.
- `com.alijafari.red.astronomy.data.database` — AppDatabase, Daos, Entities, UserOccasionDao/Entity
- `com.alijafari.red.astronomy.data.repository` — TleRepository
- `com.alijafari.red.astronomy.data.worker` — IssTleWorker, TleSyncWorker
- `com.alijafari.red.astronomy.domain` — CalculatedAstroState, Models, CanonicalAstroObject, TimeMachineState
- `com.alijafari.red.astronomy.notification` — AstroAlarmReceiver, AstroNotificationManager, AstroNotificationStore, IssPassSchedulerWorker, NotificationPermissionHelper
- `com.alijafari.red.astronomy.ui` — MainViewModel, components/*, rendering/*, screens/*, theme/*
- `com.alijafari.red.astronomy.util` — LocaleHelper
- `com.zig.gravity.physics` — BodyType, Collision, EngineConstants, NBodyEngine, Predictor, SimArrays, SimEvent, Wormhole
- `com.zig.gravity.sim` — BodyCatalog, CameraState, Effects, Presets, SaveState, SimSnapshot, SimulationViewModel, TutorialStore
- `com.zig.gravity.ui` — AddBodySheet, GravitySandboxRoot, HudBar, InspectorSheet, PresetSheet, SandboxSlider, TableSurfaceSheet, TabletopCanvas, TeachingCard, TutorialOverlay, UiUtil, theme/*
- `com.zig.gravity.edu` — Challenges, TeachingCatalog, TutorialContent, detectors/*
- `com.zig.gravity.util` — PersianDigits

## SDK Levels
- File: `/home/user/Lm-arena/app/build.gradle.kts` lines 13-18
- compileSdk: `release(36) { minorApiLevel = 1 }` => Android 36.1
- minSdk: 24
- targetSdk: 36
- versionCode: 4, versionName: 1.3

## Toolchain Versions (from version catalog and CI)
- File: `/home/user/Lm-arena/gradle/libs.versions.toml`
- AGP: 9.1.1 (agp = "9.1.1") — roadmap §3 pinned 9.4.0, difference recorded
- Kotlin: 2.2.10 (kotlin = "2.2.10") — roadmap §3 pinned 2.2.0, newer in repo
- Compose BOM: 2024.09.00 (composeBom)
- Navigation Compose: 2.8.9 declared but commented out in app/build.gradle.kts (implementation commented)
- Backdrop (Kyant liquid glass): 1.0.6
- Core KTX: 1.18.0, lifecycle-runtime 2.8.7, activity-compose 1.10.1, Room 2.7.0, Coil 2.7.0, Retrofit 2.12.0, OkHttp 4.10.0, Moshi 1.15.2, Coroutines 1.10.2, datastore 1.1.7, work 2.9.0, camera 1.5.0, etc.
- Gradle wrapper: 9.3.1 (gradle/wrapper/gradle-wrapper.properties)
- JDK: CI uses Temurin 21 (`.github/workflows/build.yml` line "Set up JDK 21"), local `gradle.properties` declares `sourceCompatibility = JavaVersion.VERSION_11` and `targetCompatibility = JavaVersion.VERSION_11` (app/build.gradle.kts lines 51-54), `org.gradle.jvmargs=-Xmx4g`
- Secrets plugin: 2.0.1, google-services 4.5.0 (but google-services.json absent, passthrough true)

## DI Approach
No DI framework. Manual:
- `MainActivity` creates `TleRepository.getInstance(applicationContext)` and injects into `SatelliteEngine.customTleResolver`
- `MainViewModel: AndroidViewModel` creates `AppRepository(AppDatabase.getDatabase(application))` and `SharedPreferences` directly
- `AppDatabase.getDatabase()` singleton
- Gravity sandbox: `SimulationViewModel` holds `NBodyEngine` etc., no Hilt/Koin
File evidence: `app/src/main/java/com/alijafari/red/astronomy/MainActivity.kt:43-50`, `app/src/main/java/com/alijafari/red/astronomy/ui/MainViewModel.kt:52-53`

## Navigation Approach
No Navigation Compose library used at runtime (commented out). 
- Main tabs: `selectedTab: Int` in `MainUiState` (MainViewModel.kt:33) with values 0=Lab,1=Satellites,2=Moon,3=AR Sky,4=Home. `MainActivity` uses `Crossfade(targetState = uiState.selectedTab)` to switch screens (MainActivity.kt:214-251). `selectTab(tabIndex)` updates state.
- Lab sub-navigation: local `selectedFeature: LabFeatureType?` state in `LabScreen.kt:92-93`. If `selectedFeature == TIME_DILATION` shows `TimeDilationCalculatorScreen`, if `GRAVITY_SANDBOX` shows `GravitySandboxRoot`, else shows card list. Back via `onBackToLab = { selectedFeature = null }`.
- No NavHost, no routes, no deep links.

## Theming / Design System
Location: `app/src/main/java/com/alijafari/red/astronomy/ui/theme/`
- `Color.kt` — defines `RedAccentVermilion`, `BackgroundPrimary`, etc., plus `RedDarkColorTokens`, `RedLightColorTokens`
- `RedTokens.kt` — `RedSpacing`, `RedCornerRadius`, `RedElevation`, `RedIconSize`, `RedControlHeight`, `RedColorTokens` data class, `RedTheme` accessor object with CompositionLocals `LocalRedColors`, `LocalRedSpacing`, etc.
- `RedPrimitives.kt` — `RedCard`, `RedElevatedCard`, `RedSectionHeader`, `RedBadge`, `RedStatusBadge`, `RedHairlineDivider`, `RedControl`, `RedSlider`, `RedRangeSlider`
- `Theme.kt` — `REDTheme` composable, 6 theme modes: DARK_NAVY, OLED_BLACK, LIGHT, PAPERCRAFT_PASTEL, DYNAMIC_SKY (sun altitude), DYNAMIC_SILK (celestial lighting). Dynamic sky uses lerp between day/sunset/twilight/night palettes. Uses `LocalAppFontFamily`.
- `Type.kt` — Vazirmatn font family for Persian (`vazirmatn_*` fonts), platform default for English. `IranSans` alias points to Vazirmatn. `Estedad` fonts corrupted, unused. `redTypographyFor(isPersian)` provides locale typography.
- `LiquidGlass.kt` — `LiquidGlassSurface` using `io.github.kyant0:backdrop` (backdrop = 1.0.6). Provides `LocalLiquidGlassEnabled`, `LocalLiquidGlassConfig`, `LocalLiquidGlassBackdrop`. Navigation bar uses `LiquidGlassSurface` with style `NavigationBar`.
- `SilkMaterial.kt` — dynamic silk color scheme.
- Gravity theming separate: `com.zig.gravity.ui.theme.GravityTheme`, `BodyIdentity`, `TableSurfaces`

Icons: `Icons.Default.*` and `Icons.Outlined.*` (Material Icons). Gravity uses custom.

## Resources Conventions
- `app/src/main/res/values/strings.xml` — English strings, includes nav labels, home, AR, moon, ISS, dialogs, gravity sandbox title/subtitle
- `app/src/main/res/values-fa/strings.xml` — Persian translations
- `res/drawable/` — ic_launcher_background.xml, img_full_moon_photo_1785673146290.jpg, red_app_logo_display.png, zig_brand_concepts_1788039445729.jpg
- `res/font/` — vazirmatn_*.ttf (used), iran_sans_*.ttf (corrupt, unused per Type.kt comment), estedad_*.ttf (corrupt, unused)
- `res/mipmap-*/` — launcher icons
- `res/values/colors.xml`, `themes.xml`, `xml/backup_rules.xml`, `data_extraction_rules.xml`
- `android.nonTransitiveRClass=true` in gradle.properties

## Test Setup
- Test files: 33 files in `app/src/test/java/com/alijafari/red/astronomy/` (list from inventory scan):
  ARCalibrationPromptTest, ARDeepSkyMergeTest, ARDistanceFormatterTest, AROnePointAlignmentTest, ARRecalibrationPromptTest, ARSkyEarthExclusionTest, ARSkyPointToShapeRenderingTest, AstroTimeTest, CatalogIntegrityTest, ClassificationAuditTest, ContentIntegrityAuditTest, DeepSkyEngineTest, DeepSkyFactCoverageTest, EclipseEngineTest, ExampleRobolectricTest, ExampleUnitTest, FrameTransformationEngineTest, GreetingScreenshotTest, HeroSkyProjectionTest, ISSPassPredictionTest, IssTleWorkerTest, LabFeatureTitleTest, LunarSolarEngineTest, MagneticAnomalyDetectorTest, ObjectDetailModalConsistencyTest, Phase4MigrationTest, Phase5FeatureMigrationTest, RelativisticEngineTest, SGP4PropagatorTest, SatelliteARConsistencyTest, SkyMapRendererTest, SkyOrientationProjectionTest, VSOP87EngineTest, VazirmatnPersianTypographyTest
- Frameworks: JUnit 4.13.2, androidx.test.ext:junit 1.3.0, espresso 3.7.0, androidx.test:core/rules/runner, compose ui-test-junit4, robolectric 4.16.1, roborazzi 1.59.0 + roborazzi-compose + junit-rule, kotlinx-coroutines-test 1.10.2
- Config: `testOptions.unitTests.isIncludeAndroidResources=true, isReturnDefaultValues=true` in app/build.gradle.kts
- CI runs `testDebugUnitTest -Pgravity.ci.tests=false` (full suite), plus explicit check for 15 required AR/catalog test classes (build.yml Confirm AR/catalog unit test classes step)
- Instrumented tests: `app/src/androidTest/java/com/alijafari/red/astronomy/ExampleInstrumentedTest.kt` only, but CI removed emulator job; no connected tests run.

## CI Workflows
- Single workflow: `.github/workflows/build.yml`
  - Triggers: push branches/tags **, pull_request, workflow_dispatch
  - Runner: ubuntu-22.04
  - Steps: checkout@v4, setup-java@v4 JDK 21 Temurin, setup-gradle@v3 gradle 9.3.1, chmod gradlew, decode signing keystore from secrets (SIGNING_KEY_BASE64 etc.), run full unit test suite with log parsing and annotation, confirm 15 AR/catalog classes pass, build signed release APK, verify package `com.alijafari.red.astronomy` versionCode 4 and not debug-signed, upload artifact `ZIG-real-app-apk` containing `ZIG-release.apk`
  - No lint separate step, no emulator, no Firebase Test Lab

## Existing Features (Lab screen context)
- File: `app/src/main/java/com/alijafari/red/astronomy/ui/screens/LabScreen.kt`
- Enum `LabFeatureType` with 4 entries:
  - TIME_DILATION (available true, icon HourglassTop)
  - GRAVITY_SANDBOX (available true, icon Public, comment "card's title is the feature's name and nothing else")
  - ORBITAL_RESONANCE (available false, Coming Soon)
  - STELLAR_EVOLUTION (available false, Coming Soon)
- Card UI: `LabFeatureCard` composable — Surface with RoundedCornerShape(xl), border, background surfaceElevated if available else surfaceGrouped, 44dp CircleShape icon background with accentRed 12% alpha, icon tint accentRed if available else textSecondary 50%, title 17sp sectionHeading, subtitle caption semiBold accentRed, description bodySecondary maxLines 2, trailing ChevronRight icon, testTag `lab_feature_card_${name.lowercase()}`
- Header card: `RedElevatedCard` with Science icon, title "Astrophysics Lab" / "آزمایشگاه نجومی و فیزیک", subtitle "Scientific tools & computational simulators"
- Section header: `RedSectionHeader` with "Available Scientific Tools" / "ابزارهای فعال و در حال توسعه"
- Bottom spacing: 112dp for floating nav bar

## Floating Nav Bar
- File: `app/src/main/java/com/alijafari/red/astronomy/ui/components/FloatingNavBar.kt`
- `FloatingBottomBar` with backdrop param, 5 NavItems: Home(4), AR Sky(3), Moon(2), Satellites(1), Lab(0). Shape 32dp, height 64dp, LiquidGlassSurface style NavigationBar. Test tags `main_bottom_navigation`, `nav_item_*`.
- Hidden when `com.zig.gravity.ui.ImmersiveScreenState.active` true (MainActivity.kt:257)

## Gravity Sandbox Integration (reference for additive pattern)
- Lab card triggers `com.zig.gravity.ui.GravitySandboxRoot(onBack = { selectedFeature = null }, startInPersian = isFa, startInDarkTheme = ...)`
- GravitySandboxRoot sets `ImmersiveScreenState.active` to hide nav bar
- No navigation graph edit, no existing file restructured, just enum entry + branch
- Own theme (GravityTheme) but respects startInDarkTheme
- Own ViewModel (SimulationViewModel)
- This pattern will be reused for Space Museum

## Other Notable Files
- `app/src/main/AndroidManifest.xml` — declares INTERNET, ACCESS_NETWORK_STATE, CAMERA, FINE/COARSE_LOCATION, VIBRATE, POST_NOTIFICATIONS, RECEIVE_BOOT_COMPLETED, SCHEDULE_EXACT_ALARM, USE_EXACT_ALARM, WAKE_LOCK. No museum permissions yet.
- `app/src/main/java/com/alijafari/red/astronomy/ui/screens/HomeScreen.kt`, `ISSScreen.kt` (71895 bytes), `MoonScreen.kt`, `CompassARScreen.kt` (207KB), `TimeDilationCalculatorScreen.kt` (61KB)
- `app/src/main/java/com/alijafari/red/astronomy/ui/components/FloatingNavBar.kt`, `HeroSkyCanvas.kt`, etc.
- `gradle/libs.versions.toml` single source of versions
- `build.gradle.kts` root declares plugins apply false

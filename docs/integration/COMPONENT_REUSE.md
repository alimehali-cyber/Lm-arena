# Component Reuse — Space Museum in ZIG

Date: 2026-09-14
Source files read for this doc: LabScreen.kt, FloatingNavBar.kt, RedPrimitives.kt, RedTokens.kt, Theme.kt, Type.kt, LiquidGlass.kt, MainActivity.kt, strings.xml

## Design System to Reuse

### Cards
- `RedElevatedCard` (RedPrimitives.kt:58-108) — header banner in LabScreen. Shape RoundedCornerShape(RedCornerRadius.xl), background RedTheme.colors.surfaceElevated, border RedTheme.colors.border, contentPadding RedSpacing.lg. Used for Lab header.
- `RedCard` — base card, similar.
- `LabFeatureCard` pattern (LabScreen.kt:199-291) — Surface with RoundedCornerShape(xl), clickable(enabled=isAvailable), testTag `lab_feature_card_${name.lowercase()}`, border 1dp, background surfaceElevated if available else surfaceGrouped, Row with 44dp CircleShape icon box background accentRed 12% alpha, icon size RedIconSize.md, tint accentRed if available else textSecondary 50%, Column weight 1f with title 17sp sectionHeading, subtitle caption semiBold accentRed, description bodySecondary maxLines 2, trailing ChevronRight icon. **Space Museum card must match this exactly.**

### Section Headers
- `RedSectionHeader` (RedPrimitives.kt:114-163) — title sectionTitle, subtitle caption, optional badge/action. Used for "Available Scientific Tools" section.

### Badges
- `RedBadge` (RedPrimitives.kt:169-196) — pill capsule, background surfaceGrouped or accentRed 15% alpha, text labelSmall 11sp Medium, border. Used for "Coming Soon" / "به زودی". Space Museum will NOT use Coming Soon badge (isAvailable=true).

### Colors (RedTokens.kt)
- `RedTheme.colors.surfaceElevated` — card background
- `RedTheme.colors.surfaceGrouped` — disabled card background
- `RedTheme.colors.border` — card border
- `RedTheme.colors.textPrimary` — title
- `RedTheme.colors.textSecondary` — subtitle/description when disabled
- `RedTheme.colors.accentRed` — icon tint, subtitle when available, active nav item
- `RedTheme.colors.background` — scaffold background
- `RedTheme.colors.isDark` — for alpha adjustments

### Spacing & Radii (RedTokens.kt)
- `RedSpacing.lg = 16.dp`, `md=12.dp`, `sm=8.dp`, `xs=4.dp`, `xl=20.dp`
- `RedCornerRadius.xl=22.dp`, `lg=18.dp`, `md=14.dp`, `sm=10.dp`
- `RedElevation.card=2.dp`, `floating=6.dp`
- `RedIconSize.md=22.dp`, `lg=28.dp`, `sm=18.dp`
- `RedControlHeight.regular=44.dp`

### Typography (Type.kt)
- Tokens: `RedTypographyTokens.sectionHeading`, `sectionTitle`, `caption`, `bodySecondary`, `sectionHeading` copy 17sp for card title
- Font family: `redFontFamily(isPersian)` — Vazirmatn for Persian, Default for English. Accessed via `LocalAppFontFamily` and `MaterialTheme.typography`. **Museum grid must use same.**
- Do not hardcode fontFamily in tokens (they leave fontFamily unset so locale face applies).

### Liquid Glass
- `LiquidGlassSurface` (LiquidGlass.kt) — used for FloatingBottomBar with backdrop. Style `LiquidGlassDefaults.NavigationBar`, fallbackColor surfaceElevated, fallbackBorder border, glassBorder 0.75dp border alpha 0.4. **Museum screens should reuse same for any floating controls if needed, but must look native.**
- CompositionLocals: `LocalLiquidGlassEnabled`, `LocalLiquidGlassConfig`, `LocalLiquidGlassBackdrop` provided in MainActivity.

### Navigation & Test Tags
- Existing tags: `lab_screen` (LazyColumn), `lab_header_card`, `lab_feature_card_${lowercase}`, `main_scaffold`, `main_bottom_navigation`, `nav_item_home` etc.
- Space Museum card: `lab_card_space_museum` or `lab_feature_card_space_museum` — use same pattern as existing: `lab_feature_card_space_museum` to match `lab_feature_card_gravity_sandbox` pattern.
- Museum grid: `space_museum_grid_screen`, tiles `space_museum_tile_${id}`

### Icons
- Material Icons: `Icons.Default.*` (HourglassTop, Public, Science, AllInclusive, AutoAwesome, ChevronRight) and `Icons.Outlined.*` (Home, Explore, DarkMode, SatelliteAlt, Science)
- For Space Museum: choose from same set — `Icons.Default.Rocket` (if available) or `Icons.Default.Public` is already used, so use `Icons.Default.Explore` or `Icons.Outlined.Rocket`? Check availability. `Icons.Default.Rocket` exists in extended? We'll use `Icons.Default.RocketLaunch` or `Icons.Default.Public` variant. Better to use `Icons.Default.Language` or `Icons.Default.TravelExplore`. Final choice: `Icons.Default.RocketLaunch` if in extended, else `Icons.Default.Public` with different tint? To avoid collision, use `Icons.Default.RocketLaunch` or `Icons.Outlined.Public`. Document in DECISIONS.md. For now plan `Icons.Default.Explore` or `Icons.Default.RocketLaunch`.
- Gravity sandbox uses Public icon. Space Museum should be distinct — e.g., Museum icon if available (`Icons.Default.Museum` in material icons extended). Check existence; if not, use `Icons.Default.Star` or `Icons.Default.RocketLaunch`.

### Scaffold & Layout
- LabScreen uses `LazyColumn` with `contentPadding = PaddingValues(horizontal=RedSpacing.lg, vertical=RedSpacing.sm)`, `verticalArrangement = spacedBy(RedSpacing.lg)`, bottom Spacer 112dp for floating nav bar.
- Museum grid should use `LazyVerticalGrid(columns = GridCells.Adaptive(minSize=148.dp))` per Appendix I.4 sketch, but reuse same padding and spacing.
- Top bar in MainActivity shows back button when selectedTab !=4 && !=3, with title per tab. Lab screen is tab 0, so top bar shows "Lab" / "آزمایشگاه" with back to Home. Museum grid is sub-screen of Lab, not a tab, so it should have its own top bar? GravitySandboxRoot is full-screen immersive and hides nav bar. Museum viewer will similarly be immersive. Museum grid (list of 13) is inside Lab's selectedFeature branch, so it should provide its own back handling.

### Strings
- Existing strings in `res/values/strings.xml`: nav labels, home, etc. Gravity sandbox title/subtitle are in strings.xml: `gravity_sandbox_title`, `gravity_sandbox_subtitle`. For consistency, Space Museum strings should be in new module's res, not in app's strings.xml, to avoid touching pre-existing file. But if we must add to app's strings for LabFeatureType title, we will add via new module? LabFeatureType enum currently hardcodes titleEn/titleFa strings in Kotlin, not via resources. So Space Museum can follow same pattern: hardcoded bilingual strings in enum, no res needed. That avoids touching strings.xml.

### State & ViewModel
- LabScreen takes `uiState: MainUiState` and `viewModel: MainViewModel`. `isFa` derived from `uiState.language == AppLanguage.PERSIAN`.
- GravitySandboxRoot takes `onBack`, `modifier`, `startInPersian`, `startInDarkTheme`.
- Museum root should take similar: `onBack`, `modifier`, `isFa`, `isDark`.

### Immersive Handling
- `com.zig.gravity.ui.ImmersiveScreenState.active` is a global boolean that MainActivity checks to hide FloatingBottomBar. GravitySandboxRoot sets it true on entry, false on exit. Museum viewer can reuse same object to hide nav bar without editing MainActivity — this is additive reuse.

## What NOT to Reuse / What to Avoid
- Do not copy gravity's N-body physics — museum is rendering only.
- Do not reuse ISS calculations from `ISSEngine` — museum ISS is standalone visual model per P7 and §26.2.
- Do not introduce new theme — use existing REDTheme.
- Do not add new permissions — museum is offline.
- Do not use Navigation Compose — follow existing tab index + selectedFeature pattern.

## Card Spec for Space Museum (to be implemented in M0)

Per Appendix I.2 and real LabScreen pattern:

```kotlin
SPACE_MUSEUM(
    titleEn = "Space Museum",
    titleFa = "موزه فضا",
    subtitleEn = "Thirteen Worlds, One at a Time",
    subtitleFa = "سیزده جهان، یک به یک",
    descriptionEn = "Inspect high-fidelity, offline 3D models from the Sun to a black hole.",
    descriptionFa = "مدل‌های سه‌بعدی آفلاین و دقیق از خورشید تا سیاه‌چاله را کاوش کنید.",
    icon = Icons.Default.RocketLaunch, // or Icons.Default.Museum if available, to be verified
    isAvailable = true
)
```

Placement: last or first in enum? Existing order: TIME_DILATION, GRAVITY_SANDBOX, ORBITAL_RESONANCE, STELLAR_EVOLUTION. Add SPACE_MUSEUM as third available, before the Coming Soon ones, so available cards stay together.

Tap: `selectedFeature = LabFeatureType.SPACE_MUSEUM` opens museum grid. Back returns to Lab.

Test tag: `lab_feature_card_space_museum` — matches existing pattern.

All text bilingual, following existing enum pattern.

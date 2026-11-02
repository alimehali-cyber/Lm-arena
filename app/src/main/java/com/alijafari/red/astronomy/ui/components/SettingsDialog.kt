package com.alijafari.red.astronomy.ui.components

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.R
import com.alijafari.red.astronomy.domain.AppLanguage
import com.alijafari.red.astronomy.domain.CalendarSystem
import com.alijafari.red.astronomy.domain.SkyCanvasTheme
import com.alijafari.red.astronomy.domain.SpatialPalette
import com.alijafari.red.astronomy.domain.ThemeMode
import com.alijafari.red.astronomy.domain.UiStyle
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.MainViewModel
import com.alijafari.red.astronomy.ui.backdrop.AppBackdropMode
import com.alijafari.red.astronomy.ui.theme.*
import com.alijafari.red.astronomy.ui.theme.RedSurface
import com.alijafari.red.astronomy.ui.theme.squircleShape
import kotlin.math.roundToInt

/**
 * The Interface section's live preview.
 *
 * It is not an illustration of the two interfaces: it renders the *same* composables the app draws
 * with — [SpatialAmbientBackdrop] behind, [RedSurface] in front, the real palette spec and the real
 * type tokens — so choosing an interface and looking at this card are the same act. The palette shown
 * is the one being previewed, not the one the app is currently drawn with, which is the whole point of
 * a preview.
 */
@Composable
private fun SpatialInterfacePreview(
    style: UiStyle,
    palette: SpatialPalette,
    isFa: Boolean
) {
    val spec = remember(style, palette) { spatialGlassSpecFor(style, palette) }
    val previewShape = squircleShape(RedCornerRadius.window)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(138.dp)
            .clip(previewShape)
            .testTag("settings_interface_preview")
    ) {
        if (spec.enabled) {
            SpatialAmbientBackdrop(
                spec = spec,
                modifier = Modifier.matchParentSize(),
                animated = false
            )
        } else {
            Box(modifier = Modifier.matchParentSize().background(RedTheme.colors.background))
        }

        CompositionLocalProvider(
            LocalSpatialGlass provides spec,
            LocalStatFontFamily provides if (isFa) LocalAppFontFamily.current else DotoFontFamily
        ) {
            RedSurface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = RedSpacing.lg)
                    .fillMaxWidth(),
                shape = squircleShape(RedCornerRadius.lg),
                color = if (spec.enabled) spec.glassFill else RedTheme.colors.surfaceElevated,
                border = BorderStroke(
                    1.dp,
                    if (spec.enabled) spec.edge.copy(alpha = 0.30f) else RedTheme.colors.border
                ),
                elevation = RedElevation.none,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RedSpacing.lg, vertical = RedSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = if (isFa) "۲۱:۰۴" else "21:04",
                            style = RedTypographyTokens.statNumber,
                            color = RedTheme.colors.textPrimary,
                            maxLines = 1
                        )
                        Text(
                            text = if (spec.enabled) {
                                if (isFa) "شفق · ماه ۸۲٪" else "Aurora · Moon 82%"
                            } else {
                                if (isFa) "پیش‌فرض · ماه ۸۲٪" else "Default · Moon 82%"
                            },
                            style = RedTypographyTokens.caption,
                            color = RedTheme.colors.textSecondary,
                            maxLines = 1
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(26.dp)
                            .clip(squircleShape(RedCornerRadius.pill))
                            .background(
                                if (spec.enabled) spec.accent.copy(alpha = 0.22f) else RedTheme.colors.accentRed.copy(alpha = 0.16f)
                            )
                            .padding(horizontal = RedSpacing.md),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "37%",
                            style = RedTypographyTokens.statNumberSmall,
                            color = RedTheme.colors.textPrimary,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * One palette, shown as a palette *and* as glass: the real ambient world of that palette with a sheet
 * of its own material floating on it, so the choice is made by looking at the material rather than at
 * two words and a swatch.
 */
@Composable
private fun GlassPaletteCard(
    palette: SpatialPalette,
    isSelected: Boolean,
    isFa: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spec = remember(palette) { spatialGlassSpecFor(UiStyle.SPATIAL, palette) }
    val shape = squircleShape(RedCornerRadius.window)
    val ink = Color(0xFFFDF7FA)

    Box(
        modifier = modifier
            .height(92.dp)
            .clip(shape)
            .clickable(onClick = onClick)
            .border(
                BorderStroke(
                    if (isSelected) 1.5.dp else 0.5.dp,
                    if (isSelected) RedTheme.colors.accentRed else RedTheme.colors.border.copy(alpha = 0.5f)
                ),
                shape
            )
    ) {
        SpatialAmbientBackdrop(
            spec = spec,
            modifier = Modifier.matchParentSize(),
            animated = false,
            showDots = false
        )

        // The material itself, over its own world.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = RedSpacing.sm + 2.dp)
                .fillMaxWidth()
                .height(34.dp)
                .testTag("settings_palette_sheet_${palette.name.lowercase()}")
                .background(spec.glassFillStrong, squircleShape(RedCornerRadius.sm + 6.dp))
                .spatialGlassMaterial(shape = squircleShape(RedCornerRadius.sm + 6.dp), spec = spec)
                .clip(squircleShape(RedCornerRadius.sm + 6.dp))
        )

        Text(
            text = if (isFa) palette.nameFa else palette.nameEn,
            style = RedTypographyTokens.caption.copy(fontWeight = FontWeight.SemiBold),
            color = ink,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = RedSpacing.sm, vertical = RedSpacing.xs + 2.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(RedSpacing.xs + 2.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(RedTheme.colors.accentRed),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}

/**
 * Data structure for the Bortle Dark-Sky scale reference guide.
 */
private data class BortleClassInfo(
    val classNumber: Int,
    val nameEn: String,
    val nameFa: String,
    val descEn: String,
    val descFa: String
)

private val BORTLE_CLASSES = listOf(
    BortleClassInfo(
        classNumber = 1,
        nameEn = "Excellent Dark Sky",
        nameFa = "آسمان کاملاً تاریک و بکر",
        descEn = "Zodiacal light visible and bright, Milky Way casts shadows, M33 directly naked-eye.",
        descFa = "نور منطقه‌البروجی درخشان، کهکشان راه شیری سایه می‌اندازد، کهکشان M33 با چشم غیرمسلح پیداست."
    ),
    BortleClassInfo(
        classNumber = 2,
        nameEn = "Typical Truly Dark Sky",
        nameFa = "آسمان تاریک واقعی",
        descEn = "Milky Way highly structured, zodiacal light prominent, M33 easily seen with naked eye.",
        descFa = "راه شیری با ساختار پیچیده و پرجزئیات، نور زودیاک نمایان، M33 به سادگی با چشم غیرمسلح دیده می‌شود."
    ),
    BortleClassInfo(
        classNumber = 3,
        nameEn = "Rural Sky",
        nameFa = "آسمان روستایی",
        descEn = "Milky Way shows rich detail, light pollution faint at horizon, M33 glimpsed with averted vision.",
        descFa = "راه شیری جزئیات خوبی دارد، آلودگی نوری خفیف در افق، M33 با نگاه مایل دیده می‌شود."
    ),
    BortleClassInfo(
        classNumber = 4,
        nameEn = "Rural / Suburban Transition",
        nameFa = "مرز روستایی و حومه شهر",
        descEn = "Milky Way visible above 40°, light domes obvious in several directions, M33 in binoculars.",
        descFa = "راه شیری بالای ۴۰ درجه افق پیداست، گنبدهای نوری در چند جهت، M33 در دوچشمی پیداست."
    ),
    BortleClassInfo(
        classNumber = 5,
        nameEn = "Suburban Sky",
        nameFa = "آسمان حومه شهر",
        descEn = "Milky Way washed out and faint at zenith, light pollution visible in most directions.",
        descFa = "راه شیری در سرسو بسیار کم‌فروغ، آلودگی نوری در بیشتر جهات افق به وضوح پیداست."
    ),
    BortleClassInfo(
        classNumber = 6,
        nameEn = "Bright Suburban Sky",
        nameFa = "آسمان روشن حومه شهر",
        descEn = "Milky Way only faintly perceptible overhead, sky glow bright within 35° of horizon.",
        descFa = "راه شیری فقط با زحمت در سرسو دیده می‌شود، درخشش آسمان تا ۳۵ درجه از افق بالا آمده."
    ),
    BortleClassInfo(
        classNumber = 7,
        nameEn = "Suburban / Urban Transition",
        nameFa = "مرز حومه و شهر",
        descEn = "Milky Way invisible, sky background has strong grey/yellow tint, brightest DSOs barely visible.",
        descFa = "راه شیری اصلاً دیده نمی‌شود، زمینه آسمان خاکستری-زرد، فقط اجرام بسیار روشن با تلسکوپ پیداست."
    ),
    BortleClassInfo(
        classNumber = 8,
        nameEn = "City Sky",
        nameFa = "آسمان شهری",
        descEn = "Sky glows whitish-grey, stars forming constellation outlines hard to see, faint stars lost.",
        descFa = "آسمان سفید-خاکستری می‌درخشد، فقط ستاره‌های پرنور صور فلکی اصلی قابل تشخیص‌اند."
    ),
    BortleClassInfo(
        classNumber = 9,
        nameEn = "Inner-City Sky",
        nameFa = "مرکز کلان‌شهر",
        descEn = "Only brightest stars and planets visible, sky brilliantly illuminated by urban lighting.",
        descFa = "فقط سیارات و پرنورترین ستارگان (مانند شباهنگ) قابل رویت هستند؛ زمینه آسمان روشن است."
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    uiState: MainUiState,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isFa = uiState.language == AppLanguage.PERSIAN

    // Expandable section states (all collapsed by default, tapped to expand/collapse)
    var isLanguageExpanded by remember { mutableStateOf(false) }
    var isCalendarExpanded by remember { mutableStateOf(false) }
    var isAppThemeExpanded by remember { mutableStateOf(false) }
    var isInterfaceExpanded by remember { mutableStateOf(false) }
    var isSkyCanvasThemeExpanded by remember { mutableStateOf(false) }
    var isAppBackdropExpanded by remember { mutableStateOf(false) }
    var isBortleExpanded by remember { mutableStateOf(false) }

    // Formatted collapsed labels
    val languageLabel = when (uiState.language) {
        AppLanguage.PERSIAN -> if (isFa) "فارسی" else "Persian"
        AppLanguage.ENGLISH -> if (isFa) "انگلیسی" else "English"
    }

    val calendarLabel = when (uiState.calendarSystem) {
        CalendarSystem.SOLAR_HIJRI -> if (isFa) "هجری شمسی" else "Solar Hijri"
        CalendarSystem.GREGORIAN -> if (isFa) "میلادی" else "Gregorian"
    }

    val appThemeLabel = when (uiState.themeMode) {
        ThemeMode.LIGHT -> if (isFa) "روشن" else "Light"
        ThemeMode.DARK -> if (isFa) "تاریک (OLED)" else "Dark (OLED)"
    }

    val interfaceLabel = when (uiState.uiStyle) {
        UiStyle.CLASSIC -> if (isFa) "پیش‌فرض" else "Default"
        UiStyle.SPATIAL -> if (isFa) {
            "شیشه فضایی · ${if (uiState.ambientPalette == SpatialPalette.WINE) "می" else "شفق"}"
        } else {
            "Spatial Glass · ${if (uiState.ambientPalette == SpatialPalette.WINE) "Wine" else "Aurora"}"
        }
    }

    val skyCanvasThemeLabel = when (uiState.skyCanvasTheme) {
        SkyCanvasTheme.REAL_SKY -> if (isFa) "آسمان واقعی" else "Real Sky"
        SkyCanvasTheme.ATMOSPHERIC_SKY -> if (isFa) "آسمان جوی" else "Atmospheric"
        SkyCanvasTheme.MONOCHROME_SCIENTIFIC -> if (isFa) "تک‌رنگ" else "Monochrome"
        SkyCanvasTheme.KIDS_WATERCOLOR -> if (isFa) "آبرنگ" else "WaterColor"
        SkyCanvasTheme.OBSERVATORY -> if (isFa) "رصدخانه" else "Observatory"
        SkyCanvasTheme.PAPERCRAFT_DIORAMA -> if (isFa) "دیوراما کاغذ برجسته" else "Papercraft"
    }

    val liveSkyStatusLabel = when (uiState.appBackdropMode) {
        AppBackdropMode.LIVE_SKY -> stringResource(R.string.live_sky_setting)
        AppBackdropMode.NGC_1929 -> stringResource(R.string.backdrop_ngc1929_setting)
        AppBackdropMode.CEPHEUS -> stringResource(R.string.backdrop_cepheus_setting)
        AppBackdropMode.NONE -> if (isFa) "خاموش" else "Off"
    }

    val bortleLabel = if (isFa) "کلاس ${uiState.bortleClass}" else "Class ${uiState.bortleClass}"

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_dialog"),
        shape = squircleShape(RedCornerRadius.xl),
        containerColor = RedTheme.colors.surfaceElevated,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RedSpacing.sm)
            ) {
                Surface(
                    shape = CircleShape,
                    color = RedTheme.colors.accentRed.copy(alpha = 0.12f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = RedTheme.colors.accentRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.app_settings),
                    style = RedTypographyTokens.sectionHeading,
                    color = RedTheme.colors.textPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(RedSpacing.sm + 2.dp)
            ) {
                // 1. Language Dropdown Section
                ExpandableSettingsSection(
                    title = stringResource(R.string.application_language),
                    currentValueLabel = languageLabel,
                    icon = Icons.Default.Language,
                    isExpanded = isLanguageExpanded,
                    onToggle = { isLanguageExpanded = !isLanguageExpanded },
                    testTag = "settings_section_language"
                ) {
                    SettingsOptionRow(
                        title = stringResource(R.string.language_persian),
                        subtitle = "فارسی (Persian)",
                        isSelected = uiState.language == AppLanguage.PERSIAN,
                        onClick = { viewModel.setLanguage(AppLanguage.PERSIAN) }
                    )
                    SettingsOptionRow(
                        title = stringResource(R.string.language_english),
                        subtitle = "English (US)",
                        isSelected = uiState.language == AppLanguage.ENGLISH,
                        onClick = { viewModel.setLanguage(AppLanguage.ENGLISH) }
                    )
                }

                // 2. Calendar System Dropdown Section
                ExpandableSettingsSection(
                    title = stringResource(R.string.calendar_system),
                    currentValueLabel = calendarLabel,
                    icon = Icons.Default.CalendarToday,
                    isExpanded = isCalendarExpanded,
                    onToggle = { isCalendarExpanded = !isCalendarExpanded },
                    testTag = "settings_section_calendar"
                ) {
                    SettingsOptionRow(
                        title = stringResource(R.string.calendar_solar_hijri),
                        subtitle = if (isFa) "تقویم خورشیدی رسمی ایران" else "Solar Hijri Calendar",
                        isSelected = uiState.calendarSystem == CalendarSystem.SOLAR_HIJRI,
                        onClick = { viewModel.setCalendarSystem(CalendarSystem.SOLAR_HIJRI) }
                    )
                    SettingsOptionRow(
                        title = stringResource(R.string.calendar_gregorian),
                        subtitle = if (isFa) "تقویم بین‌المللی میلادی" else "Gregorian International Calendar",
                        isSelected = uiState.calendarSystem == CalendarSystem.GREGORIAN,
                        onClick = { viewModel.setCalendarSystem(CalendarSystem.GREGORIAN) }
                    )
                }

                // 3. App Theme Dropdown Section
                ExpandableSettingsSection(
                    title = if (isFa) "پوسته برنامه" else "App Theme",
                    currentValueLabel = appThemeLabel,
                    icon = Icons.Default.Palette,
                    isExpanded = isAppThemeExpanded,
                    onToggle = { isAppThemeExpanded = !isAppThemeExpanded },
                    testTag = "settings_section_app_theme"
                ) {
                    SettingsOptionRow(
                        title = if (isFa) "روشن" else "Light",
                        isSelected = uiState.themeMode == ThemeMode.LIGHT,
                        onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) }
                    )
                    SettingsOptionRow(
                        title = if (isFa) "تاریک (OLED)" else "Dark (OLED)",
                        isSelected = uiState.themeMode == ThemeMode.DARK,
                        onClick = { viewModel.setThemeMode(ThemeMode.DARK) }
                    )
                }

                // 4. Interface Section — which of the two interfaces the app is drawn with, and the
                // colour of the optional one. The preview is built from the very same spec the app
                // draws with, so what is shown here is the material, not an illustration of it.
                ExpandableSettingsSection(
                    title = if (isFa) "رابط کاربری" else "Interface",
                    currentValueLabel = interfaceLabel,
                    icon = Icons.Outlined.BlurCircular,
                    isExpanded = isInterfaceExpanded,
                    onToggle = { isInterfaceExpanded = !isInterfaceExpanded },
                    statusColor = if (uiState.uiStyle == UiStyle.SPATIAL) RedTheme.colors.accentRed else RedTheme.colors.textSecondary,
                    testTag = "settings_section_interface"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(RedSpacing.sm)) {
                        SpatialInterfacePreview(
                            style = uiState.uiStyle,
                            palette = uiState.ambientPalette,
                            isFa = isFa
                        )

                        SettingsOptionRow(
                            title = if (isFa) "پیش‌فرض (صفحه‌های مات)" else "Default (flat panels)",
                            subtitle = if (isFa) "همان رابط فعلی برنامه: سطوح مات، بدون افکت پشت زمینه"
                            else "The app as it is: opaque panels, nothing behind them",
                            isSelected = uiState.uiStyle == UiStyle.CLASSIC,
                            onClick = { viewModel.setUiStyle(UiStyle.CLASSIC) }
                        )
                        SettingsOptionRow(
                            title = if (isFa) "شیشه فضایی (Spatial Glass)" else "Spatial Glass",
                            subtitle = if (isFa) "پنل‌های شیشه‌ای شفاف روی نور محیطی متحرک"
                            else "Frosted translucent panels floating over drifting ambient light",
                            isSelected = uiState.uiStyle == UiStyle.SPATIAL,
                            onClick = { viewModel.setUiStyle(UiStyle.SPATIAL) }
                        )

                        Text(
                            text = if (isFa) "پالت شیشه" else "Glass palette",
                            style = RedTypographyTokens.caption.copy(fontWeight = FontWeight.SemiBold),
                            color = RedTheme.colors.textSecondary,
                            modifier = Modifier.padding(top = RedSpacing.xs)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(RedSpacing.sm)
                        ) {
                            GlassPaletteCard(
                                palette = SpatialPalette.AURORA,
                                isSelected = uiState.ambientPalette == SpatialPalette.AURORA,
                                isFa = isFa,
                                onClick = { viewModel.setAmbientPalette(SpatialPalette.AURORA) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_palette_aurora")
                            )
                            GlassPaletteCard(
                                palette = SpatialPalette.WINE,
                                isSelected = uiState.ambientPalette == SpatialPalette.WINE,
                                isFa = isFa,
                                onClick = { viewModel.setAmbientPalette(SpatialPalette.WINE) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_palette_wine")
                            )
                        }

                        Text(
                            text = when {
                                uiState.uiStyle == UiStyle.CLASSIC && isFa ->
                                    "پالت فقط روی رابط «شیشه فضایی» اعمال می‌شود؛ پس از انتخاب آن نمایان می‌شود."
                                uiState.uiStyle == UiStyle.CLASSIC ->
                                    "The palette colours the Spatial Glass interface; pick that interface above to see it."
                                uiState.ambientPalette == SpatialPalette.WINE && isFa ->
                                    "شرابی · می: شیشه روی شراب؛ پایه زرشکی تیره، نور بورگوندی و لبه طلایی."
                                uiState.ambientPalette == SpatialPalette.WINE ->
                                    "Wine · Mei: glass held up to a lamp — burgundy light, rose specular, gold rim."
                                isFa ->
                                    "شفق: مشبک نور مرجانی، هلویی، فیروزه‌ای و بنفش در شب بنفش‌تیره."
                                else ->
                                    "Aurora: a coral, peach, cyan and violet mesh drifting through a violet night."
                            },
                            style = RedTypographyTokens.caption,
                            color = RedTheme.colors.textSecondary
                        )
                    }
                }

                // 5. Sky Canvas Theme Dropdown Section
                ExpandableSettingsSection(
                    title = if (isFa) "سبک هنری آسمان (RMAE)" else "Sky Canvas Theme",
                    currentValueLabel = skyCanvasThemeLabel,
                    icon = Icons.Default.AutoAwesome,
                    isExpanded = isSkyCanvasThemeExpanded,
                    onToggle = { isSkyCanvasThemeExpanded = !isSkyCanvasThemeExpanded },
                    testTag = "settings_section_sky_theme"
                ) {
                    SettingsOptionRow(
                        title = if (isFa) "آسمان واقعی (Real Sky)" else "Real Sky",
                        isSelected = uiState.skyCanvasTheme == SkyCanvasTheme.REAL_SKY,
                        onClick = { viewModel.setSkyCanvasTheme(SkyCanvasTheme.REAL_SKY) }
                    )
                    SettingsOptionRow(
                        title = if (isFa) "آسمان جوی (Atmospheric)" else "Atmospheric Sky",
                        isSelected = uiState.skyCanvasTheme == SkyCanvasTheme.ATMOSPHERIC_SKY,
                        onClick = { viewModel.setSkyCanvasTheme(SkyCanvasTheme.ATMOSPHERIC_SKY) }
                    )
                    SettingsOptionRow(
                        title = if (isFa) "تک‌رنگ علمی (Monochrome)" else "Monochrome Scientific",
                        isSelected = uiState.skyCanvasTheme == SkyCanvasTheme.MONOCHROME_SCIENTIFIC,
                        onClick = { viewModel.setSkyCanvasTheme(SkyCanvasTheme.MONOCHROME_SCIENTIFIC) }
                    )
                    SettingsOptionRow(
                        title = if (isFa) "آبرنگ کودکانه (Watercolor)" else "Kids Watercolor",
                        isSelected = uiState.skyCanvasTheme == SkyCanvasTheme.KIDS_WATERCOLOR,
                        onClick = { viewModel.setSkyCanvasTheme(SkyCanvasTheme.KIDS_WATERCOLOR) }
                    )
                    SettingsOptionRow(
                        title = if (isFa) "رصدخانه (Observatory)" else "Observatory",
                        isSelected = uiState.skyCanvasTheme == SkyCanvasTheme.OBSERVATORY,
                        onClick = { viewModel.setSkyCanvasTheme(SkyCanvasTheme.OBSERVATORY) }
                    )
                    SettingsOptionRow(
                        title = if (isFa) "دیوراما کاغذ برجسته (Papercraft)" else "Papercraft Diorama",
                        isSelected = uiState.skyCanvasTheme == SkyCanvasTheme.PAPERCRAFT_DIORAMA,
                        onClick = { viewModel.setSkyCanvasTheme(SkyCanvasTheme.PAPERCRAFT_DIORAMA) }
                    )
                }

                // 6. App Backdrop Section. One live option, default OFF.
                ExpandableSettingsSection(
                    title = stringResource(R.string.app_backdrop_title),
                    currentValueLabel = liveSkyStatusLabel,
                    icon = Icons.Outlined.NightsStay,
                    isExpanded = isAppBackdropExpanded,
                    onToggle = { isAppBackdropExpanded = !isAppBackdropExpanded },
                    statusColor = if (uiState.appBackdropMode != AppBackdropMode.NONE) RedTheme.colors.accentRed else RedTheme.colors.textSecondary,
                    testTag = "settings_section_app_backdrop"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(RedSpacing.sm)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(RedSpacing.sm)
                        ) {
                            FilterChip(
                                selected = uiState.appBackdropMode == AppBackdropMode.NONE,
                                onClick = { viewModel.setAppBackdropMode(AppBackdropMode.NONE) },
                                label = { Text(text = if (isFa) "خاموش" else "Off", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RedTheme.colors.accentRed,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("settings_backdrop_off")
                            )
                            FilterChip(
                                selected = uiState.appBackdropMode == AppBackdropMode.LIVE_SKY,
                                onClick = { viewModel.setAppBackdropMode(AppBackdropMode.LIVE_SKY) },
                                label = { Text(text = stringResource(R.string.live_sky_setting), fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RedTheme.colors.accentRed,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_live_sky_switch")
                            )
                            FilterChip(
                                selected = uiState.appBackdropMode == AppBackdropMode.NGC_1929,
                                onClick = { viewModel.setAppBackdropMode(AppBackdropMode.NGC_1929) },
                                label = { Text(text = stringResource(R.string.backdrop_ngc1929_setting), fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RedTheme.colors.accentRed,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_backdrop_ngc1929")
                            )
                        }
                        FilterChip(
                            selected = uiState.appBackdropMode == AppBackdropMode.CEPHEUS,
                            onClick = { viewModel.setAppBackdropMode(AppBackdropMode.CEPHEUS) },
                            label = { Text(text = stringResource(R.string.backdrop_cepheus_setting), fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RedTheme.colors.accentRed,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_backdrop_cepheus")
                        )
                        Text(
                            text = when (uiState.appBackdropMode) {
                                AppBackdropMode.LIVE_SKY -> stringResource(R.string.live_sky_desc)
                                AppBackdropMode.NGC_1929 -> stringResource(R.string.backdrop_ngc1929_desc)
                                AppBackdropMode.CEPHEUS -> stringResource(R.string.backdrop_cepheus_desc)
                                AppBackdropMode.NONE -> stringResource(R.string.backdrop_off_desc)
                            },
                            style = RedTypographyTokens.caption,
                            color = RedTheme.colors.textSecondary
                        )
                    }
                }

                // 7. Bortle Light-Pollution Dedicated Section & Guide
                ExpandableSettingsSection(
                    title = if (isFa) "آلودگی نوری (مقیاس بورتل)" else "Light Pollution (Bortle Scale)",
                    currentValueLabel = bortleLabel,
                    icon = Icons.Default.Brightness6,
                    isExpanded = isBortleExpanded,
                    onToggle = { isBortleExpanded = !isBortleExpanded },
                    testTag = "settings_section_bortle"
                ) {
                    // Bortle Slider Row
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isFa) "کلاس انتخابی بورتل:" else "Selected Bortle Class:",
                                style = RedTypographyTokens.caption.copy(fontWeight = FontWeight.SemiBold),
                                color = RedTheme.colors.textPrimary
                            )
                            val activeInfo = BORTLE_CLASSES.getOrNull(uiState.bortleClass - 1)
                            Text(
                                text = "${uiState.bortleClass} - ${if (isFa) activeInfo?.nameFa ?: "" else activeInfo?.nameEn ?: ""}",
                                style = RedTypographyTokens.caption.copy(fontWeight = FontWeight.Bold),
                                color = RedTheme.colors.accentRed
                            )
                        }

                        RedSlider(
                            value = uiState.bortleClass.toFloat(),
                            onValueChange = { viewModel.setBortleClass(it.toInt()) },
                            valueRange = 1f..9f,
                            steps = 7,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bortle_class_slider")
                        )
                    }

                    HorizontalDivider(color = RedTheme.colors.border.copy(alpha = 0.5f), thickness = 0.5.dp)

                    // Concise Bortle Scale Identification Guide
                    Text(
                        text = if (isFa) "راهنمای تطبیق وضعیت آسمان شب:" else "Sky Appearance Identification Guide:",
                        style = RedTypographyTokens.caption.copy(fontWeight = FontWeight.Bold),
                        color = RedTheme.colors.textSecondary
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        BORTLE_CLASSES.forEach { info ->
                            val isSelected = uiState.bortleClass == info.classNumber
                            RedSurface(
                                shape = squircleShape(RedCornerRadius.window),
                                color = if (isSelected) RedTheme.colors.accentRed.copy(alpha = 0.15f) else Color.Transparent,
                                border = BorderStroke(
                                    0.5.dp,
                                    if (isSelected) RedTheme.colors.accentRed.copy(alpha = 0.4f) else RedTheme.colors.border.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setBortleClass(info.classNumber) },
                                elevation = RedElevation.none,
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Class Number Badge
                                    Surface(
                                        shape = squircleShape(4.dp),
                                        color = if (isSelected) RedTheme.colors.accentRed else RedTheme.colors.surfaceElevated,
                                        modifier = Modifier.padding(top = 1.dp)
                                    ) {
                                        Text(
                                            text = "${info.classNumber}",
                                            style = RedTypographyTokens.caption.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            ),
                                            color = if (isSelected) Color.White else RedTheme.colors.textSecondary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                        )
                                    }

                                    // Name & Concise Description
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isFa) info.nameFa else info.nameEn,
                                            style = RedTypographyTokens.caption.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                            ),
                                            color = if (isSelected) RedTheme.colors.accentRed else RedTheme.colors.textPrimary
                                        )
                                        Text(
                                            text = if (isFa) info.descFa else info.descEn,
                                            style = RedTypographyTokens.caption.copy(fontSize = 10.sp),
                                            color = RedTheme.colors.textSecondary,
                                            lineHeight = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 8. Developer Credit Card
                RedSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = RedSpacing.xs)
                        .testTag("developer_credit_card"),
                    shape = squircleShape(RedCornerRadius.lg),
                    color = RedTheme.colors.surfaceGrouped
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(RedSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(RedSpacing.md)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = RedTheme.colors.accentRed.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, RedTheme.colors.accentRed.copy(alpha = 0.3f)),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                SafeAppLogo(
                                    modifier = Modifier.size(30.dp),
                                    cornerRadius = 8.dp
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (isFa) "توسعه‌دهنده: " else "Developed by ",
                                    style = RedTypographyTokens.caption,
                                    color = RedTheme.colors.textSecondary
                                )
                                Text(
                                    text = "علی جعفری",
                                    style = RedTypographyTokens.bodyPrimary.copy(fontWeight = FontWeight.Bold),
                                    color = RedTheme.colors.textPrimary
                                )
                            }

                            Text(
                                text = "Ali Jafari • ZIG Astronomy Engine",
                                style = RedTypographyTokens.caption,
                                color = RedTheme.colors.textSecondary
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            // Telegram badge
                            val intentContext = context
                            Surface(
                                shape = squircleShape(RedCornerRadius.xs),
                                color = RedTheme.colors.accentRed.copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, RedTheme.colors.accentRed.copy(alpha = 0.3f)),
                                modifier = Modifier.clickable {
                                    try {
                                        val tgIntent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse("https://t.me/EdisonWasAThief")
                                        )
                                        intentContext.startActivity(tgIntent)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = null,
                                        tint = RedTheme.colors.accentRed,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "@EdisonWasAThief",
                                        style = RedTypographyTokens.caption.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = RedTheme.colors.accentRed
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = squircleShape(RedCornerRadius.md),
                colors = ButtonDefaults.buttonColors(containerColor = RedTheme.colors.accentRed)
            ) {
                Text(text = stringResource(R.string.ok), color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    )
}

/**
 * Reusable iOS-style expandable settings container with clean status badge and animated arrow.
 */
@Composable
private fun ExpandableSettingsSection(
    title: String,
    currentValueLabel: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    statusColor: Color? = null,
    testTag: String = "",
    content: @Composable ColumnScope.() -> Unit
) {
    val rotationState by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "expand_rotation"
    )

    RedSurface(
        shape = squircleShape(RedCornerRadius.window),
        color = RedTheme.colors.surfaceGrouped,
        border = BorderStroke(
            1.dp,
            if (isExpanded) RedTheme.colors.accentRed.copy(alpha = 0.35f) else RedTheme.colors.border
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
        elevation = RedElevation.none,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row (Clickable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = RedSpacing.md, vertical = RedSpacing.sm + 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RedSpacing.sm),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isExpanded) RedTheme.colors.accentRed else RedTheme.colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = title,
                        style = RedTypographyTokens.bodyPrimary.copy(fontWeight = FontWeight.SemiBold),
                        color = RedTheme.colors.textPrimary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = squircleShape(RedCornerRadius.xs),
                        color = (statusColor ?: RedTheme.colors.accentRed).copy(alpha = 0.12f),
                        border = BorderStroke(0.5.dp, (statusColor ?: RedTheme.colors.accentRed).copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = currentValueLabel,
                            style = RedTypographyTokens.caption.copy(fontWeight = FontWeight.Bold),
                            color = statusColor ?: RedTheme.colors.accentRed,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = RedTheme.colors.textSecondary,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(rotationState)
                    )
                }
            }

            // Expanded Options Area
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(tween(180)) + fadeIn(tween(180)),
                exit = shrinkVertically(tween(140)) + fadeOut(tween(140))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RedTheme.colors.surfaceVariant.copy(alpha = 0.35f))
                        .padding(horizontal = RedSpacing.md, vertical = RedSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(RedSpacing.xs + 2.dp),
                    content = content
                )
            }
        }
    }
}

/**
 * Compact selectable option row with subtle highlight and checkmark.
 */
@Composable
private fun SettingsOptionRow(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    RedSurface(
        shape = squircleShape(RedCornerRadius.window),
        color = if (isSelected) RedTheme.colors.accentRed.copy(alpha = 0.12f) else Color.Transparent,
        border = BorderStroke(
            0.5.dp,
            if (isSelected) RedTheme.colors.accentRed.copy(alpha = 0.4f) else RedTheme.colors.border.copy(alpha = 0.2f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = RedElevation.none,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RedSpacing.sm + 2.dp, vertical = RedSpacing.xs + 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = RedTypographyTokens.bodySecondary.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (isSelected) RedTheme.colors.accentRed else RedTheme.colors.textPrimary
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = RedTypographyTokens.caption.copy(fontSize = 11.sp),
                        color = RedTheme.colors.textSecondary
                    )
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = RedTheme.colors.accentRed,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}


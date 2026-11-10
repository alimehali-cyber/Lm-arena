package com.alijafari.red.astronomy.ui.aurora

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.alijafari.red.astronomy.R
import com.alijafari.red.astronomy.astro_engine.TimeEngine
import com.alijafari.red.astronomy.domain.AppLanguage
import com.alijafari.red.astronomy.domain.AppUiStyle
import com.alijafari.red.astronomy.domain.CalendarSystem
import com.alijafari.red.astronomy.domain.SkyCanvasTheme
import com.alijafari.red.astronomy.domain.ThemeMode
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.MainViewModel
import com.alijafari.red.astronomy.ui.backdrop.AppBackdropMode
import com.alijafari.red.astronomy.ui.theme.squircleShape

// ============================================================================
// AURORA SETTINGS
// ============================================================================
//
// Every setting the other three interfaces expose, in Aurora's language. Parity
// is not optional: a style that quietly drops a setting is a style that quietly
// takes the choice away, which is the opposite of what a theme selector is for.
//
// The structure is one disclosure per group, all closed by default. That is
// spatial hierarchy DR3 — space before containment — taken one step further: the
// group's own header already names its current value, so the closed list is a
// complete summary and opening a group is an act of changing something, not of
// reading it.

/** The app's own compact Bortle guide, kept beside the slider that uses it. */
private data class AuroraBortleClass(
    val classNumber: Int,
    val nameEn: String,
    val nameFa: String
)

private val AURORA_BORTLE_CLASSES = listOf(
    AuroraBortleClass(1, "Excellent dark site", "مکان کاملاً تاریک"),
    AuroraBortleClass(2, "Typical dark site", "مکان تاریک معمولی"),
    AuroraBortleClass(3, "Rural sky", "آسمان روستایی"),
    AuroraBortleClass(4, "Rural / suburban", "مرز روستا و حومه"),
    AuroraBortleClass(5, "Suburban sky", "آسمان حومه شهر"),
    AuroraBortleClass(6, "Bright suburban", "حومه روشن"),
    AuroraBortleClass(7, "Suburban / urban", "مرز حومه و شهر"),
    AuroraBortleClass(8, "City sky", "آسمان شهری"),
    AuroraBortleClass(9, "Inner-city sky", "مرکز کلان‌شهر")
)

@Composable
fun AuroraSettingsModal(
    uiState: MainUiState,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val palette = AuroraTheme.palette
    val isFa = uiState.language == AppLanguage.PERSIAN

    var isUiStyleExpanded by remember { mutableStateOf(false) }
    var isLanguageExpanded by remember { mutableStateOf(false) }
    var isCalendarExpanded by remember { mutableStateOf(false) }
    var isAppThemeExpanded by remember { mutableStateOf(false) }
    var isSkyThemeExpanded by remember { mutableStateOf(false) }
    var isBackdropExpanded by remember { mutableStateOf(false) }
    var isBortleExpanded by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 26.dp),
            contentAlignment = Alignment.Center
        ) {
            AuroraLiquidSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 660.dp)
                    .testTag("settings_dialog"),
                shape = squircleShape(AuroraRadius.container),
                tier = AuroraSurfaceTier.OVERLAY
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = AuroraSpace.cardPadding,
                                end = AuroraSpace.local,
                                top = AuroraSpace.cardPadding,
                                bottom = AuroraSpace.group
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AuroraInset(modifier = Modifier.size(38.dp)) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = palette.accent,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(AuroraSpace.group))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.app_settings),
                                style = AuroraType.title,
                                color = palette.textPrimary
                            )
                            Text(
                                text = if (isFa) "زبان، تقویم، پوسته و آسمان" else "Language, calendar, theme and sky",
                                style = AuroraType.caption,
                                color = palette.textTertiary
                            )
                        }
                        AuroraIconAction(
                            icon = Icons.Default.Close,
                            contentDescription = if (isFa) "بستن" else "Close",
                            onClick = onDismiss
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(
                                start = AuroraSpace.cardPadding,
                                end = AuroraSpace.cardPadding,
                                bottom = AuroraSpace.cardPadding
                            ),
                        verticalArrangement = Arrangement.spacedBy(AuroraSpace.group)
                    ) {
                        // 0 — Interface style
                        AuroraExpandableSection(
                            title = stringResource(R.string.ui_style_title),
                            currentValueLabel = auroraUiStyleLabel(uiState),
                            icon = Icons.Outlined.Widgets,
                            isExpanded = isUiStyleExpanded,
                            onToggle = { isUiStyleExpanded = !isUiStyleExpanded },
                            testTag = "settings_section_ui_style"
                        ) {
                            AuroraOptionRow(
                                title = stringResource(R.string.ui_style_default),
                                subtitle = stringResource(R.string.ui_style_default_desc),
                                selected = uiState.uiStyle == AppUiStyle.DEFAULT,
                                onClick = { viewModel.setUiStyle(AppUiStyle.DEFAULT) },
                                modifier = Modifier.testTag("settings_ui_style_default")
                            )
                            AuroraOptionRow(
                                title = stringResource(R.string.ui_style_spatial),
                                subtitle = stringResource(R.string.ui_style_spatial_desc),
                                selected = uiState.uiStyle == AppUiStyle.SPATIAL,
                                onClick = { viewModel.setUiStyle(AppUiStyle.SPATIAL) },
                                modifier = Modifier.testTag("settings_ui_style_spatial")
                            )
                            AuroraOptionRow(
                                title = stringResource(R.string.ui_style_cyber),
                                subtitle = stringResource(R.string.ui_style_cyber_desc),
                                selected = uiState.uiStyle == AppUiStyle.CYBER,
                                onClick = { viewModel.setUiStyle(AppUiStyle.CYBER) },
                                modifier = Modifier.testTag("settings_ui_style_cyber")
                            )
                            AuroraOptionRow(
                                title = stringResource(R.string.ui_style_aurora),
                                subtitle = stringResource(R.string.ui_style_aurora_desc),
                                selected = uiState.uiStyle == AppUiStyle.AURORA,
                                onClick = { viewModel.setUiStyle(AppUiStyle.AURORA) },
                                modifier = Modifier.testTag("settings_ui_style_aurora")
                            )
                        }

                        // 1 — Language
                        AuroraExpandableSection(
                            title = stringResource(R.string.application_language),
                            currentValueLabel = when (uiState.language) {
                                AppLanguage.PERSIAN -> stringResource(R.string.language_persian)
                                AppLanguage.ENGLISH -> stringResource(R.string.language_english)
                            },
                            icon = Icons.Default.Language,
                            isExpanded = isLanguageExpanded,
                            onToggle = { isLanguageExpanded = !isLanguageExpanded },
                            testTag = "settings_section_language"
                        ) {
                            AuroraOptionRow(
                                title = stringResource(R.string.language_persian),
                                subtitle = "فارسی (Persian)",
                                selected = uiState.language == AppLanguage.PERSIAN,
                                onClick = { viewModel.setLanguage(AppLanguage.PERSIAN) }
                            )
                            AuroraOptionRow(
                                title = stringResource(R.string.language_english),
                                subtitle = "English (US)",
                                selected = uiState.language == AppLanguage.ENGLISH,
                                onClick = { viewModel.setLanguage(AppLanguage.ENGLISH) }
                            )
                        }

                        // 2 — Calendar
                        AuroraExpandableSection(
                            title = stringResource(R.string.calendar_system),
                            currentValueLabel = when (uiState.calendarSystem) {
                                CalendarSystem.SOLAR_HIJRI -> stringResource(R.string.calendar_solar_hijri)
                                CalendarSystem.GREGORIAN -> stringResource(R.string.calendar_gregorian)
                            },
                            icon = Icons.Default.NightsStay,
                            isExpanded = isCalendarExpanded,
                            onToggle = { isCalendarExpanded = !isCalendarExpanded },
                            testTag = "settings_section_calendar"
                        ) {
                            AuroraOptionRow(
                                title = stringResource(R.string.calendar_solar_hijri),
                                subtitle = if (isFa) "تقویم خورشیدی رسمی ایران" else "Official Solar Hijri calendar",
                                selected = uiState.calendarSystem == CalendarSystem.SOLAR_HIJRI,
                                onClick = { viewModel.setCalendarSystem(CalendarSystem.SOLAR_HIJRI) }
                            )
                            AuroraOptionRow(
                                title = stringResource(R.string.calendar_gregorian),
                                subtitle = if (isFa) "تقویم بین‌المللی میلادی" else "International Gregorian calendar",
                                selected = uiState.calendarSystem == CalendarSystem.GREGORIAN,
                                onClick = { viewModel.setCalendarSystem(CalendarSystem.GREGORIAN) }
                            )
                        }

                        // 3 — Theme
                        AuroraExpandableSection(
                            title = if (isFa) "پوسته برنامه" else "App theme",
                            currentValueLabel = when (uiState.themeMode) {
                                ThemeMode.LIGHT -> if (isFa) "روشن" else "Light"
                                ThemeMode.DARK -> if (isFa) "تاریک" else "Dark"
                                ThemeMode.WINE -> stringResource(R.string.theme_wine)
                            },
                            icon = Icons.Default.Palette,
                            isExpanded = isAppThemeExpanded,
                            onToggle = { isAppThemeExpanded = !isAppThemeExpanded },
                            testTag = "settings_section_app_theme"
                        ) {
                            AuroraOptionRow(
                                title = if (isFa) "روشن" else "Light",
                                selected = uiState.themeMode == ThemeMode.LIGHT,
                                onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) }
                            )
                            AuroraOptionRow(
                                title = if (isFa) "تاریک" else "Dark",
                                selected = uiState.themeMode == ThemeMode.DARK,
                                onClick = { viewModel.setThemeMode(ThemeMode.DARK) }
                            )
                            AuroraOptionRow(
                                title = stringResource(R.string.theme_wine),
                                subtitle = stringResource(R.string.theme_wine_desc),
                                selected = uiState.themeMode == ThemeMode.WINE,
                                onClick = { viewModel.setThemeMode(ThemeMode.WINE) },
                                modifier = Modifier.testTag("settings_theme_wine")
                            )
                        }

                        // 4 — Sky canvas
                        AuroraExpandableSection(
                            title = if (isFa) "سبک هنری آسمان" else "Sky canvas style",
                            currentValueLabel = auroraSkyThemeLabel(uiState.skyCanvasTheme, isFa),
                            icon = Icons.Default.AutoAwesome,
                            isExpanded = isSkyThemeExpanded,
                            onToggle = { isSkyThemeExpanded = !isSkyThemeExpanded },
                            testTag = "settings_section_sky_theme"
                        ) {
                            auroraSkyThemeOptions().forEach { option ->
                                AuroraOptionRow(
                                    title = if (isFa) option.second else option.first,
                                    selected = uiState.skyCanvasTheme == option.third,
                                    onClick = { viewModel.setSkyCanvasTheme(option.third) }
                                )
                            }
                        }

                        // 5 — Backdrop
                        AuroraExpandableSection(
                            title = stringResource(R.string.app_backdrop_title),
                            currentValueLabel = auroraBackdropLabel(uiState.appBackdropMode, isFa),
                            icon = Icons.Default.NightsStay,
                            isExpanded = isBackdropExpanded,
                            onToggle = { isBackdropExpanded = !isBackdropExpanded },
                            testTag = "settings_section_app_backdrop"
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(AuroraSpace.local)
                            ) {
                                AuroraChip(
                                    label = if (isFa) "خاموش" else "Off",
                                    selected = uiState.appBackdropMode == AppBackdropMode.NONE,
                                    onClick = { viewModel.setAppBackdropMode(AppBackdropMode.NONE) },
                                    modifier = Modifier.weight(1f)
                                        .testTag("settings_backdrop_off")
                                )
                                AuroraChip(
                                    label = stringResource(R.string.live_sky_setting),
                                    selected = uiState.appBackdropMode == AppBackdropMode.LIVE_SKY,
                                    onClick = { viewModel.setAppBackdropMode(AppBackdropMode.LIVE_SKY) },
                                    modifier = Modifier.weight(1f)
                                        .testTag("settings_live_sky_switch")
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(AuroraSpace.local)
                            ) {
                                AuroraChip(
                                    label = stringResource(R.string.backdrop_ngc1929_setting),
                                    selected = uiState.appBackdropMode == AppBackdropMode.NGC_1929,
                                    onClick = { viewModel.setAppBackdropMode(AppBackdropMode.NGC_1929) },
                                    modifier = Modifier.weight(1f)
                                        .testTag("settings_backdrop_ngc1929")
                                )
                                AuroraChip(
                                    label = stringResource(R.string.backdrop_cepheus_setting),
                                    selected = uiState.appBackdropMode == AppBackdropMode.CEPHEUS,
                                    onClick = { viewModel.setAppBackdropMode(AppBackdropMode.CEPHEUS) },
                                    modifier = Modifier.weight(1f)
                                        .testTag("settings_backdrop_cepheus")
                                )
                            }
                            Text(
                                text = when (uiState.appBackdropMode) {
                                    AppBackdropMode.LIVE_SKY -> stringResource(R.string.live_sky_desc)
                                    AppBackdropMode.NGC_1929 -> stringResource(R.string.backdrop_ngc1929_desc)
                                    AppBackdropMode.CEPHEUS -> stringResource(R.string.backdrop_cepheus_desc)
                                    AppBackdropMode.NONE -> stringResource(R.string.backdrop_off_desc)
                                },
                                style = AuroraType.caption,
                                color = palette.textTertiary,
                                modifier = Modifier.padding(horizontal = AuroraSpace.local)
                            )
                        }

                        // 6 — Light pollution
                        AuroraExpandableSection(
                            title = if (isFa) "آلودگی نوری (بورتل)" else "Light pollution (Bortle)",
                            currentValueLabel = if (isFa) {
                                "کلاس ${TimeEngine.formatPersianNumbers(uiState.bortleClass.toString())}"
                            } else {
                                "Class ${uiState.bortleClass}"
                            },
                            icon = Icons.Default.Brightness6,
                            isExpanded = isBortleExpanded,
                            onToggle = { isBortleExpanded = !isBortleExpanded },
                            testTag = "settings_section_bortle"
                        ) {
                            val active = AURORA_BORTLE_CLASSES.getOrNull(uiState.bortleClass - 1)
                            AuroraSliderRow(
                                label = if (isFa) "کلاس بورتل" else "Bortle class",
                                value = uiState.bortleClass.toFloat(),
                                onValueChange = { viewModel.setBortleClass(it.toInt()) },
                                valueRange = 1f..9f,
                                steps = 7,
                                valueText = if (isFa) {
                                    TimeEngine.formatPersianNumbers(uiState.bortleClass.toString())
                                } else {
                                    uiState.bortleClass.toString()
                                }
                            )
                            if (active != null) {
                                AuroraStatusPill(
                                    label = if (isFa) active.nameFa else active.nameEn,
                                    status = when {
                                        uiState.bortleClass <= 3 -> AuroraStatus.GOOD
                                        uiState.bortleClass <= 6 -> AuroraStatus.INFO
                                        else -> AuroraStatus.WARN
                                    }
                                )
                            }
                            Text(
                                text = if (isFa) {
                                    "کلاس پایین‌تر یعنی آسمان تاریک‌تر و اجرام کم‌نورتر در دسترس."
                                } else {
                                    "A lower class means a darker sky and fainter objects within reach."
                                },
                                style = AuroraType.caption,
                                color = palette.textTertiary,
                                modifier = Modifier.padding(horizontal = AuroraSpace.local)
                            )
                        }

                        Spacer(modifier = Modifier.height(AuroraSpace.local))
                    }
                }
            }
        }
    }
}

@Composable
private fun auroraUiStyleLabel(uiState: MainUiState): String = when (uiState.uiStyle) {
    AppUiStyle.DEFAULT -> stringResource(R.string.ui_style_default)
    AppUiStyle.SPATIAL -> stringResource(R.string.ui_style_spatial)
    AppUiStyle.CYBER -> stringResource(R.string.ui_style_cyber)
    AppUiStyle.AURORA -> stringResource(R.string.ui_style_aurora)
}

@Composable
private fun auroraSkyThemeLabel(theme: SkyCanvasTheme, isFa: Boolean): String = when (theme) {
    SkyCanvasTheme.REAL_SKY -> if (isFa) "آسمان واقعی" else "Real sky"
    SkyCanvasTheme.ATMOSPHERIC_SKY -> if (isFa) "آسمان جوی" else "Atmospheric"
    SkyCanvasTheme.MONOCHROME_SCIENTIFIC -> if (isFa) "تک‌رنگ علمی" else "Monochrome"
    SkyCanvasTheme.KIDS_WATERCOLOR -> if (isFa) "آبرنگ" else "Watercolor"
    SkyCanvasTheme.OBSERVATORY -> if (isFa) "رصدخانه" else "Observatory"
    SkyCanvasTheme.PAPERCRAFT_DIORAMA -> if (isFa) "دیوراما" else "Papercraft"
}

private fun auroraSkyThemeOptions(): List<Triple<String, String, SkyCanvasTheme>> = listOf(
    Triple("Real sky", "آسمان واقعی", SkyCanvasTheme.REAL_SKY),
    Triple("Atmospheric", "آسمان جوی", SkyCanvasTheme.ATMOSPHERIC_SKY),
    Triple("Monochrome scientific", "تک‌رنگ علمی", SkyCanvasTheme.MONOCHROME_SCIENTIFIC),
    Triple("Kids watercolor", "آبرنگ کودکانه", SkyCanvasTheme.KIDS_WATERCOLOR),
    Triple("Observatory", "رصدخانه", SkyCanvasTheme.OBSERVATORY),
    Triple("Papercraft diorama", "دیوراما کاغذ برجسته", SkyCanvasTheme.PAPERCRAFT_DIORAMA)
)

@Composable
private fun auroraBackdropLabel(mode: AppBackdropMode, isFa: Boolean): String = when (mode) {
    AppBackdropMode.NONE -> if (isFa) "خاموش" else "Off"
    AppBackdropMode.LIVE_SKY -> stringResource(R.string.live_sky_setting)
    AppBackdropMode.NGC_1929 -> stringResource(R.string.backdrop_ngc1929_setting)
    AppBackdropMode.CEPHEUS -> stringResource(R.string.backdrop_cepheus_setting)
}

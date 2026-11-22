package com.alijafari.red.astronomy.ui.spatial

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.BuildConfig
import com.alijafari.red.astronomy.R
import com.alijafari.red.astronomy.domain.AppLanguage
import com.alijafari.red.astronomy.domain.AppUiStyle
import com.alijafari.red.astronomy.domain.CalendarSystem
import com.alijafari.red.astronomy.domain.SkyCanvasTheme
import com.alijafari.red.astronomy.domain.ThemeMode
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.MainViewModel
import com.alijafari.red.astronomy.ui.backdrop.AppBackdropMode
import com.alijafari.red.astronomy.ui.components.AboutZigPage
import com.alijafari.red.astronomy.ui.components.BORTLE_CLASSES
import com.alijafari.red.astronomy.ui.components.BortleGuideList
import com.alijafari.red.astronomy.ui.components.SettingsEmergenceDialog
import com.alijafari.red.astronomy.ui.components.SettingsExpandableRow
import com.alijafari.red.astronomy.ui.components.SettingsGroupHeader
import com.alijafari.red.astronomy.ui.components.SettingsMenuPanel
import com.alijafari.red.astronomy.ui.components.SettingsOptionRow
import com.alijafari.red.astronomy.ui.components.SettingsPlainRow
import com.alijafari.red.astronomy.ui.components.SettingsSkin
import com.alijafari.red.astronomy.ui.theme.RedCornerRadius
import com.alijafari.red.astronomy.ui.theme.RedTypographyTokens
import com.alijafari.red.astronomy.ui.theme.squircleShape

/**
 * Spatial UI — the shared Samsung-style settings menu wearing the Spatial glass palette:
 * pastel neon accents over frosted glass, emerging from the settings button and collapsing back
 * into it. The About page carries the app identity and its creator.
 */
@Composable
fun SpatialSettingsModal(
    uiState: MainUiState,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    settingsButtonCenter: Offset? = null
) {
    val palette = SpatialTheme.palette
    val isFa = uiState.language == AppLanguage.PERSIAN

    var showAbout by remember { mutableStateOf(false) }

    var isUiStyleExpanded by remember { mutableStateOf(false) }
    var isLanguageExpanded by remember { mutableStateOf(false) }
    var isCalendarExpanded by remember { mutableStateOf(false) }
    var isAppThemeExpanded by remember { mutableStateOf(false) }
    var isSkyCanvasThemeExpanded by remember { mutableStateOf(false) }
    var isAppBackdropExpanded by remember { mutableStateOf(false) }
    var isBortleExpanded by remember { mutableStateOf(false) }

    val uiStyleLabel = when (uiState.uiStyle) {
        AppUiStyle.DEFAULT -> stringResource(R.string.ui_style_default)
        AppUiStyle.SPATIAL -> stringResource(R.string.ui_style_spatial)
        AppUiStyle.AETHER -> stringResource(R.string.ui_style_aether)
    }
    val languageLabel = when (uiState.language) {
        AppLanguage.PERSIAN -> stringResource(R.string.language_persian)
        AppLanguage.ENGLISH -> stringResource(R.string.language_english)
    }
    val calendarLabel = when (uiState.calendarSystem) {
        CalendarSystem.SOLAR_HIJRI -> stringResource(R.string.calendar_solar_hijri)
        CalendarSystem.GREGORIAN -> stringResource(R.string.calendar_gregorian)
    }
    val appThemeLabel = when (uiState.themeMode) {
        ThemeMode.LIGHT -> if (isFa) "روشن" else "Light"
        ThemeMode.DARK -> if (isFa) "تاریک (OLED)" else "Dark (OLED)"
        ThemeMode.WINE -> stringResource(R.string.theme_wine)
    }
    val skyCanvasThemeLabel = when (uiState.skyCanvasTheme) {
        SkyCanvasTheme.REAL_SKY -> if (isFa) "آسمان واقعی" else "Real Sky"
        SkyCanvasTheme.ATMOSPHERIC_SKY -> if (isFa) "آسمان جوی" else "Atmospheric"
        SkyCanvasTheme.MONOCHROME_SCIENTIFIC -> if (isFa) "تک‌رنگ" else "Monochrome"
        SkyCanvasTheme.KIDS_WATERCOLOR -> if (isFa) "آبرنگ" else "WaterColor"
        SkyCanvasTheme.OBSERVATORY -> if (isFa) "رصدخانه" else "Observatory"
        SkyCanvasTheme.PAPERCRAFT_DIORAMA -> if (isFa) "دیوراما کاغذ برجسته" else "Papercraft"
    }
    val backdropLabel = when (uiState.appBackdropMode) {
        AppBackdropMode.NONE -> if (isFa) "خاموش (کویر فضایی)" else "Off (Spatial Dreamscape)"
        AppBackdropMode.LIVE_SKY -> stringResource(R.string.live_sky_setting)
        AppBackdropMode.NGC_1929 -> stringResource(R.string.backdrop_ngc1929_setting)
        AppBackdropMode.CEPHEUS -> stringResource(R.string.backdrop_cepheus_setting)
    }
    val bortleLabel = if (isFa) "کلاس ${uiState.bortleClass}" else "Class ${uiState.bortleClass}"

    val skin = SettingsSkin(
        accent = palette.accentInk,
        onAccent = Color.White,
        panel = palette.glassElevatedFill,
        groupBg = palette.glassBackingVeil,
        selectedBg = palette.accentInk.copy(alpha = 0.14f),
        chipBg = palette.accentInk.copy(alpha = 0.12f),
        textPrimary = palette.textPrimary,
        textSecondary = palette.textSecondary,
        textTertiary = palette.textTertiary,
        divider = palette.rimSideBorder.copy(alpha = 0.5f)
    )

    SettingsEmergenceDialog(origin = settingsButtonCenter, onDismiss = onDismiss) {
        SettingsMenuPanel(
            skin = skin,
            title = stringResource(R.string.app_settings),
            subtitle = if (isFa) "شیشه فضایی • ZIG" else "Spatial Glass • ZIG",
            onClose = onDismiss
        ) {
            Box(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 24.dp)
                ) {
                    // ── General ────────────────────────────────────────────────
                    SettingsGroupHeader(stringResource(R.string.settings_group_general), skin)

                    SettingsExpandableRow(
                        skin = skin,
                        title = stringResource(R.string.ui_style_title),
                        valueLabel = uiStyleLabel,
                        icon = Icons.Outlined.Widgets,
                        isExpanded = isUiStyleExpanded,
                        onToggle = { isUiStyleExpanded = !isUiStyleExpanded },
                        testTag = "settings_section_ui_style"
                    ) {
                        SettingsOptionRow(
                            skin = skin,
                            title = stringResource(R.string.ui_style_default),
                            subtitle = stringResource(R.string.ui_style_default_desc),
                            isSelected = uiState.uiStyle == AppUiStyle.DEFAULT,
                            onClick = { viewModel.setUiStyle(AppUiStyle.DEFAULT) },
                            modifier = Modifier.testTag("settings_ui_style_default")
                        )
                        SettingsOptionRow(
                            skin = skin,
                            title = stringResource(R.string.ui_style_spatial),
                            subtitle = stringResource(R.string.ui_style_spatial_desc),
                            isSelected = uiState.uiStyle == AppUiStyle.SPATIAL,
                            onClick = { viewModel.setUiStyle(AppUiStyle.SPATIAL) },
                            modifier = Modifier.testTag("settings_ui_style_spatial")
                        )
                        SettingsOptionRow(
                            skin = skin,
                            title = stringResource(R.string.ui_style_aether),
                            subtitle = stringResource(R.string.ui_style_aether_desc),
                            isSelected = uiState.uiStyle == AppUiStyle.AETHER,
                            onClick = { viewModel.setUiStyle(AppUiStyle.AETHER) },
                            modifier = Modifier.testTag("settings_ui_style_aether")
                        )
                    }

                    SettingsExpandableRow(
                        skin = skin,
                        title = stringResource(R.string.application_language),
                        valueLabel = languageLabel,
                        icon = Icons.Default.Language,
                        isExpanded = isLanguageExpanded,
                        onToggle = { isLanguageExpanded = !isLanguageExpanded },
                        testTag = "settings_section_language"
                    ) {
                        SettingsOptionRow(
                            skin = skin,
                            title = stringResource(R.string.language_persian),
                            subtitle = "فارسی (Persian)",
                            isSelected = uiState.language == AppLanguage.PERSIAN,
                            onClick = { viewModel.setLanguage(AppLanguage.PERSIAN) }
                        )
                        SettingsOptionRow(
                            skin = skin,
                            title = stringResource(R.string.language_english),
                            subtitle = "English (US)",
                            isSelected = uiState.language == AppLanguage.ENGLISH,
                            onClick = { viewModel.setLanguage(AppLanguage.ENGLISH) }
                        )
                    }

                    SettingsExpandableRow(
                        skin = skin,
                        title = stringResource(R.string.calendar_system),
                        valueLabel = calendarLabel,
                        icon = Icons.Default.CalendarToday,
                        isExpanded = isCalendarExpanded,
                        onToggle = { isCalendarExpanded = !isCalendarExpanded },
                        testTag = "settings_section_calendar"
                    ) {
                        SettingsOptionRow(
                            skin = skin,
                            title = stringResource(R.string.calendar_solar_hijri),
                            subtitle = if (isFa) "تقویم خورشیدی رسمی ایران" else "Solar Hijri Calendar",
                            isSelected = uiState.calendarSystem == CalendarSystem.SOLAR_HIJRI,
                            onClick = { viewModel.setCalendarSystem(CalendarSystem.SOLAR_HIJRI) }
                        )
                        SettingsOptionRow(
                            skin = skin,
                            title = stringResource(R.string.calendar_gregorian),
                            subtitle = if (isFa) "تقویم بین‌المللی میلادی" else "Gregorian International Calendar",
                            isSelected = uiState.calendarSystem == CalendarSystem.GREGORIAN,
                            onClick = { viewModel.setCalendarSystem(CalendarSystem.GREGORIAN) }
                        )
                    }

                    // ── Appearance ────────────────────────────────────────────
                    SettingsGroupHeader(stringResource(R.string.settings_group_appearance), skin)

                    SettingsExpandableRow(
                        skin = skin,
                        title = if (isFa) "پوسته برنامه" else "App Theme",
                        valueLabel = appThemeLabel,
                        icon = Icons.Default.Palette,
                        isExpanded = isAppThemeExpanded,
                        onToggle = { isAppThemeExpanded = !isAppThemeExpanded },
                        testTag = "settings_section_app_theme"
                    ) {
                        SettingsOptionRow(
                            skin = skin,
                            title = if (isFa) "روشن" else "Light",
                            isSelected = uiState.themeMode == ThemeMode.LIGHT,
                            onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) }
                        )
                        SettingsOptionRow(
                            skin = skin,
                            title = if (isFa) "تاریک (OLED)" else "Dark (OLED)",
                            isSelected = uiState.themeMode == ThemeMode.DARK,
                            onClick = { viewModel.setThemeMode(ThemeMode.DARK) }
                        )
                        SettingsOptionRow(
                            skin = skin,
                            title = stringResource(R.string.theme_wine),
                            subtitle = stringResource(R.string.theme_wine_desc),
                            isSelected = uiState.themeMode == ThemeMode.WINE,
                            onClick = { viewModel.setThemeMode(ThemeMode.WINE) },
                            modifier = Modifier.testTag("settings_theme_wine")
                        )
                    }

                    SettingsExpandableRow(
                        skin = skin,
                        title = if (isFa) "سبک هنری آسمان" else "Sky Canvas Theme",
                        valueLabel = skyCanvasThemeLabel,
                        icon = Icons.Outlined.AutoAwesome,
                        isExpanded = isSkyCanvasThemeExpanded,
                        onToggle = { isSkyCanvasThemeExpanded = !isSkyCanvasThemeExpanded },
                        testTag = "settings_section_sky_theme"
                    ) {
                        SettingsOptionRow(
                            skin = skin,
                            title = if (isFa) "آسمان واقعی (Real Sky)" else "Real Sky",
                            isSelected = uiState.skyCanvasTheme == SkyCanvasTheme.REAL_SKY,
                            onClick = { viewModel.setSkyCanvasTheme(SkyCanvasTheme.REAL_SKY) }
                        )
                        SettingsOptionRow(
                            skin = skin,
                            title = if (isFa) "آسمان جوی (Atmospheric)" else "Atmospheric Sky",
                            isSelected = uiState.skyCanvasTheme == SkyCanvasTheme.ATMOSPHERIC_SKY,
                            onClick = { viewModel.setSkyCanvasTheme(SkyCanvasTheme.ATMOSPHERIC_SKY) }
                        )
                        SettingsOptionRow(
                            skin = skin,
                            title = if (isFa) "تک‌رنگ علمی (Monochrome)" else "Monochrome Scientific",
                            isSelected = uiState.skyCanvasTheme == SkyCanvasTheme.MONOCHROME_SCIENTIFIC,
                            onClick = { viewModel.setSkyCanvasTheme(SkyCanvasTheme.MONOCHROME_SCIENTIFIC) }
                        )
                        SettingsOptionRow(
                            skin = skin,
                            title = if (isFa) "آبرنگ کودکانه (Watercolor)" else "Kids Watercolor",
                            isSelected = uiState.skyCanvasTheme == SkyCanvasTheme.KIDS_WATERCOLOR,
                            onClick = { viewModel.setSkyCanvasTheme(SkyCanvasTheme.KIDS_WATERCOLOR) }
                        )
                        SettingsOptionRow(
                            skin = skin,
                            title = if (isFa) "رصدخانه (Observatory)" else "Observatory",
                            isSelected = uiState.skyCanvasTheme == SkyCanvasTheme.OBSERVATORY,
                            onClick = { viewModel.setSkyCanvasTheme(SkyCanvasTheme.OBSERVATORY) }
                        )
                        SettingsOptionRow(
                            skin = skin,
                            title = if (isFa) "دیوراما کاغذ برجسته (Papercraft)" else "Papercraft Diorama",
                            isSelected = uiState.skyCanvasTheme == SkyCanvasTheme.PAPERCRAFT_DIORAMA,
                            onClick = { viewModel.setSkyCanvasTheme(SkyCanvasTheme.PAPERCRAFT_DIORAMA) }
                        )
                    }

                    SettingsExpandableRow(
                        skin = skin,
                        title = stringResource(R.string.app_backdrop_title),
                        valueLabel = backdropLabel,
                        icon = Icons.Outlined.NightsStay,
                        isExpanded = isAppBackdropExpanded,
                        onToggle = { isAppBackdropExpanded = !isAppBackdropExpanded },
                        testTag = "settings_section_app_backdrop"
                    ) {
                        SettingsOptionRow(
                            skin = skin,
                            title = if (isFa) "خاموش (کویر فضایی)" else "Off (Spatial Dreamscape)",
                            isSelected = uiState.appBackdropMode == AppBackdropMode.NONE,
                            onClick = { viewModel.setAppBackdropMode(AppBackdropMode.NONE) },
                            modifier = Modifier.testTag("settings_backdrop_off")
                        )
                        SettingsOptionRow(
                            skin = skin,
                            title = stringResource(R.string.live_sky_setting),
                            isSelected = uiState.appBackdropMode == AppBackdropMode.LIVE_SKY,
                            onClick = { viewModel.setAppBackdropMode(AppBackdropMode.LIVE_SKY) },
                            modifier = Modifier.testTag("settings_live_sky_switch")
                        )
                        SettingsOptionRow(
                            skin = skin,
                            title = stringResource(R.string.backdrop_ngc1929_setting),
                            isSelected = uiState.appBackdropMode == AppBackdropMode.NGC_1929,
                            onClick = { viewModel.setAppBackdropMode(AppBackdropMode.NGC_1929) },
                            modifier = Modifier.testTag("settings_backdrop_ngc1929")
                        )
                        SettingsOptionRow(
                            skin = skin,
                            title = stringResource(R.string.backdrop_cepheus_setting),
                            isSelected = uiState.appBackdropMode == AppBackdropMode.CEPHEUS,
                            onClick = { viewModel.setAppBackdropMode(AppBackdropMode.CEPHEUS) },
                            modifier = Modifier.testTag("settings_backdrop_cepheus")
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = when (uiState.appBackdropMode) {
                                AppBackdropMode.LIVE_SKY -> stringResource(R.string.live_sky_desc)
                                AppBackdropMode.NGC_1929 -> stringResource(R.string.backdrop_ngc1929_desc)
                                AppBackdropMode.CEPHEUS -> stringResource(R.string.backdrop_cepheus_desc)
                                AppBackdropMode.NONE -> stringResource(R.string.backdrop_off_desc)
                            },
                            style = RedTypographyTokens.caption.copy(fontSize = 11.5.sp),
                            color = skin.textTertiary
                        )
                    }

                    // ── Sky & Science ─────────────────────────────────────────
                    SettingsGroupHeader(stringResource(R.string.settings_group_sky_science), skin)

                    SettingsExpandableRow(
                        skin = skin,
                        title = if (isFa) "آلودگی نوری (مقیاس بورتل)" else "Light Pollution (Bortle Scale)",
                        valueLabel = bortleLabel,
                        icon = Icons.Default.Brightness6,
                        isExpanded = isBortleExpanded,
                        onToggle = { isBortleExpanded = !isBortleExpanded },
                        testTag = "settings_section_bortle"
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isFa) "کلاس انتخابی بورتل:" else "Selected Bortle Class:",
                                style = RedTypographyTokens.caption.copy(fontWeight = FontWeight.SemiBold),
                                color = skin.textPrimary
                            )
                            val activeInfo = BORTLE_CLASSES.getOrNull(uiState.bortleClass - 1)
                            Text(
                                text = "${uiState.bortleClass} - ${if (isFa) activeInfo?.nameFa ?: "" else activeInfo?.nameEn ?: ""}",
                                style = RedTypographyTokens.caption.copy(fontWeight = FontWeight.Bold),
                                color = skin.accent
                            )
                        }

                        Slider(
                            value = uiState.bortleClass.toFloat(),
                            onValueChange = { viewModel.setBortleClass(it.toInt()) },
                            valueRange = 1f..9f,
                            steps = 7,
                            colors = SliderDefaults.colors(
                                thumbColor = skin.accent,
                                activeTrackColor = skin.accent,
                                inactiveTrackColor = skin.accent.copy(alpha = 0.18f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bortle_class_slider")
                        )

                        Text(
                            text = if (isFa) "راهنمای تطبیق وضعیت آسمان شب:" else "Sky Appearance Identification Guide:",
                            style = RedTypographyTokens.caption.copy(fontWeight = FontWeight.Bold),
                            color = skin.textTertiary
                        )

                        BortleGuideList(
                            skin = skin,
                            isFa = isFa,
                            selectedClass = uiState.bortleClass,
                            onSelect = { viewModel.setBortleClass(it) }
                        )
                    }

                    // ── About ─────────────────────────────────────────────────
                    SettingsGroupHeader(stringResource(R.string.settings_group_about), skin)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(squircleShape(RedCornerRadius.lg))
                            .background(skin.groupBg)
                    ) {
                        SettingsPlainRow(
                            skin = skin,
                            title = stringResource(R.string.settings_about_title),
                            subtitle = "${stringResource(R.string.about_version_label)} ${BuildConfig.VERSION_NAME} • Ali Jafari",
                            icon = Icons.Outlined.Info,
                            onClick = { showAbout = true },
                            modifier = Modifier.testTag("settings_about_button")
                        )
                    }
                }

                // The About page slides over the menu and slides back away.
                AnimatedVisibility(
                    visible = showAbout,
                    modifier = Modifier.fillMaxSize(),
                    enter = slideInHorizontally(
                        spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)
                    ) { it / 3 } + fadeIn(
                        spring(dampingRatio = 1f, stiffness = Spring.StiffnessMediumLow)
                    ),
                    exit = slideOutHorizontally(
                        spring(dampingRatio = 1f, stiffness = Spring.StiffnessMedium)
                    ) { it / 3 } + fadeOut(
                        spring(dampingRatio = 1f, stiffness = Spring.StiffnessMedium)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(skin.panel)
                    ) {
                        AboutZigPage(
                            skin = skin,
                            isFa = isFa,
                            versionName = BuildConfig.VERSION_NAME,
                            onBack = { showAbout = false }
                        )
                    }
                }
            }
        }
    }
}

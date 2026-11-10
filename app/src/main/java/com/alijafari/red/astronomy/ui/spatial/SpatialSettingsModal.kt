package com.alijafari.red.astronomy.ui.spatial

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.NightsStay
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.alijafari.red.astronomy.ui.theme.RedCornerRadius
import com.alijafari.red.astronomy.ui.theme.RedShapes
import com.alijafari.red.astronomy.ui.theme.RedSpacing
import com.alijafari.red.astronomy.ui.theme.RedTypographyTokens
import com.alijafari.red.astronomy.ui.theme.squircleShape

/**
 * VisionOS Spatial Glassmorphism Settings Dialog — Exact Default [SettingsDialog] Expandable Layout.
 *
 * Preserves the exact vertical sequence of expandable sections from `SettingsDialog`:
 * 0. Interface Style (`Default UI` vs `Spatial UI`)
 * 1. Application Language
 * 2. Calendar System
 * 3. App Theme
 * 4. Sky Canvas Theme
 * 5. App Backdrop
 * 6. Light Pollution (Bortle Scale)
 */
@Composable
fun SpatialSettingsModal(
    uiState: MainUiState,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val palette = SpatialTheme.palette
    val isFa = uiState.language == AppLanguage.PERSIAN

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
        AppUiStyle.CYBER -> stringResource(R.string.ui_style_cyber)
        AppUiStyle.OBSIDIAN -> stringResource(R.string.ui_style_obsidian)
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
        ThemeMode.DARK -> if (isFa) "تاریک" else "Dark"
        ThemeMode.WINE -> stringResource(R.string.theme_wine)
    }
    val skyCanvasThemeLabel = when (uiState.skyCanvasTheme) {
        SkyCanvasTheme.REAL_SKY -> if (isFa) "آسمان واقعی" else "Real Sky"
        SkyCanvasTheme.ATMOSPHERIC_SKY -> if (isFa) "آسمان جوی" else "Atmospheric"
        SkyCanvasTheme.MONOCHROME_SCIENTIFIC -> if (isFa) "تک‌رنگ علمی" else "Monochrome"
        SkyCanvasTheme.KIDS_WATERCOLOR -> if (isFa) "آبرنگ" else "Watercolor"
        SkyCanvasTheme.OBSERVATORY -> if (isFa) "رصدخانه" else "Observatory"
        SkyCanvasTheme.PAPERCRAFT_DIORAMA -> if (isFa) "دیوراما" else "Papercraft"
    }
    val backdropLabel = when (uiState.appBackdropMode) {
        AppBackdropMode.NONE -> if (isFa) "خاموش (کویر فضایی)" else "Off (Spatial Dreamscape)"
        AppBackdropMode.LIVE_SKY -> stringResource(R.string.live_sky_setting)
        AppBackdropMode.NGC_1929 -> stringResource(R.string.backdrop_ngc1929_setting)
        AppBackdropMode.CEPHEUS -> stringResource(R.string.backdrop_cepheus_setting)
    }
    val bortleLabel = if (isFa) {
        "کلاس ${TimeEngine.formatPersianNumbers(uiState.bortleClass.toString())}"
    } else {
        "Class ${uiState.bortleClass}"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            SpatialGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 640.dp),
                cornerRadius = RedCornerRadius.window,
                contentPadding = PaddingValues(20.dp),
                elevation = 24.dp,
                elevated = true,
                glowAccent = SpatialNeonRose
            ) {
                // Title Row (Matches SettingsDialog)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RedSpacing.md)
                ) {
                    SpatialMeshPill(
                        preset = SpatialMeshPreset.ROSE_TO_ORANGE,
                        isCapsule = true,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = Color(0xFF23111C),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(18.dp)
                        )
                    }
                    Text(
                        text = stringResource(R.string.app_settings),
                        style = RedTypographyTokens.sectionHeading,
                        color = palette.textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(RedSpacing.sm + 2.dp)
                ) {
                    // 0. Interface Style
                    SpatialExpandableSection(
                        title = stringResource(R.string.ui_style_title),
                        currentValueLabel = uiStyleLabel,
                        icon = Icons.Outlined.Widgets,
                        isExpanded = isUiStyleExpanded,
                        onToggle = { isUiStyleExpanded = !isUiStyleExpanded },
                        testTag = "settings_section_ui_style"
                    ) {
                        SpatialSettingsOptionRow(
                            title = stringResource(R.string.ui_style_default),
                            subtitle = stringResource(R.string.ui_style_default_desc),
                            isSelected = uiState.uiStyle == AppUiStyle.DEFAULT,
                            onClick = { viewModel.setUiStyle(AppUiStyle.DEFAULT) },
                            modifier = Modifier.testTag("settings_ui_style_default")
                        )
                        SpatialSettingsOptionRow(
                            title = stringResource(R.string.ui_style_spatial),
                            subtitle = stringResource(R.string.ui_style_spatial_desc),
                            isSelected = uiState.uiStyle == AppUiStyle.SPATIAL,
                            onClick = { viewModel.setUiStyle(AppUiStyle.SPATIAL) },
                            modifier = Modifier.testTag("settings_ui_style_spatial")
                        )
                        SpatialSettingsOptionRow(
                            title = stringResource(R.string.ui_style_cyber),
                            subtitle = stringResource(R.string.ui_style_cyber_desc),
                            isSelected = uiState.uiStyle == AppUiStyle.CYBER,
                            onClick = { viewModel.setUiStyle(AppUiStyle.CYBER) },
                            modifier = Modifier.testTag("settings_ui_style_cyber")
                        )
                        SpatialSettingsOptionRow(
                            title = stringResource(R.string.ui_style_obsidian),
                            subtitle = stringResource(R.string.ui_style_obsidian_desc),
                            isSelected = uiState.uiStyle == AppUiStyle.OBSIDIAN,
                            onClick = { viewModel.setUiStyle(AppUiStyle.OBSIDIAN) },
                            modifier = Modifier.testTag("settings_ui_style_obsidian")
                        )
                    }

                    // 1. Language
                    SpatialExpandableSection(
                        title = stringResource(R.string.application_language),
                        currentValueLabel = languageLabel,
                        icon = Icons.Default.Language,
                        isExpanded = isLanguageExpanded,
                        onToggle = { isLanguageExpanded = !isLanguageExpanded },
                        testTag = "settings_section_language"
                    ) {
                        SpatialSettingsOptionRow(
                            title = stringResource(R.string.language_persian),
                            subtitle = "فارسی (Persian)",
                            isSelected = uiState.language == AppLanguage.PERSIAN,
                            onClick = { viewModel.setLanguage(AppLanguage.PERSIAN) }
                        )
                        SpatialSettingsOptionRow(
                            title = stringResource(R.string.language_english),
                            subtitle = "English (US)",
                            isSelected = uiState.language == AppLanguage.ENGLISH,
                            onClick = { viewModel.setLanguage(AppLanguage.ENGLISH) }
                        )
                    }

                    // 2. Calendar System
                    SpatialExpandableSection(
                        title = stringResource(R.string.calendar_system),
                        currentValueLabel = calendarLabel,
                        icon = Icons.Default.CalendarToday,
                        isExpanded = isCalendarExpanded,
                        onToggle = { isCalendarExpanded = !isCalendarExpanded },
                        testTag = "settings_section_calendar"
                    ) {
                        SpatialSettingsOptionRow(
                            title = stringResource(R.string.calendar_solar_hijri),
                            subtitle = if (isFa) "تقویم خورشیدی رسمی ایران" else "Solar Hijri Calendar",
                            isSelected = uiState.calendarSystem == CalendarSystem.SOLAR_HIJRI,
                            onClick = { viewModel.setCalendarSystem(CalendarSystem.SOLAR_HIJRI) }
                        )
                        SpatialSettingsOptionRow(
                            title = stringResource(R.string.calendar_gregorian),
                            subtitle = if (isFa) "تقویم بین‌المللی میلادی" else "Gregorian International Calendar",
                            isSelected = uiState.calendarSystem == CalendarSystem.GREGORIAN,
                            onClick = { viewModel.setCalendarSystem(CalendarSystem.GREGORIAN) }
                        )
                    }

                    // 3. App Theme
                    SpatialExpandableSection(
                        title = if (isFa) "پوسته برنامه" else "App Theme",
                        currentValueLabel = appThemeLabel,
                        icon = Icons.Default.Palette,
                        isExpanded = isAppThemeExpanded,
                        onToggle = { isAppThemeExpanded = !isAppThemeExpanded },
                        testTag = "settings_section_app_theme"
                    ) {
                        SpatialSettingsOptionRow(
                            title = if (isFa) "روشن" else "Light",
                            isSelected = uiState.themeMode == ThemeMode.LIGHT,
                            onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) }
                        )
                        SpatialSettingsOptionRow(
                            title = if (isFa) "تاریک" else "Dark",
                            isSelected = uiState.themeMode == ThemeMode.DARK,
                            onClick = { viewModel.setThemeMode(ThemeMode.DARK) }
                        )
                        SpatialSettingsOptionRow(
                            title = stringResource(R.string.theme_wine),
                            subtitle = stringResource(R.string.theme_wine_desc),
                            isSelected = uiState.themeMode == ThemeMode.WINE,
                            onClick = { viewModel.setThemeMode(ThemeMode.WINE) },
                            modifier = Modifier.testTag("settings_theme_wine")
                        )
                    }

                    // 4. Sky Canvas Theme
                    SpatialExpandableSection(
                        title = if (isFa) "سبک هنری آسمان (RMAE)" else "Sky Canvas Theme",
                        currentValueLabel = skyCanvasThemeLabel,
                        icon = Icons.Default.AutoAwesome,
                        isExpanded = isSkyCanvasThemeExpanded,
                        onToggle = { isSkyCanvasThemeExpanded = !isSkyCanvasThemeExpanded },
                        testTag = "settings_section_sky_theme"
                    ) {
                        listOf(
                            SkyCanvasTheme.REAL_SKY to (if (isFa) "آسمان واقعی (Real Sky)" else "Real Sky"),
                            SkyCanvasTheme.ATMOSPHERIC_SKY to (if (isFa) "آسمان جوی (Atmospheric)" else "Atmospheric Sky"),
                            SkyCanvasTheme.MONOCHROME_SCIENTIFIC to (if (isFa) "تک‌رنگ علمی (Monochrome)" else "Monochrome Scientific"),
                            SkyCanvasTheme.KIDS_WATERCOLOR to (if (isFa) "آبرنگ کودکانه (Watercolor)" else "Kids Watercolor"),
                            SkyCanvasTheme.OBSERVATORY to (if (isFa) "رصدخانه (Observatory)" else "Observatory"),
                            SkyCanvasTheme.PAPERCRAFT_DIORAMA to (if (isFa) "دیوراما کاغذ برجسته (Papercraft)" else "Papercraft Diorama")
                        ).forEach { (theme, label) ->
                            SpatialSettingsOptionRow(
                                title = label,
                                isSelected = uiState.skyCanvasTheme == theme,
                                onClick = { viewModel.setSkyCanvasTheme(theme) }
                            )
                        }
                    }

                    // 5. App Backdrop
                    SpatialExpandableSection(
                        title = stringResource(R.string.app_backdrop_title),
                        currentValueLabel = backdropLabel,
                        icon = Icons.Outlined.NightsStay,
                        isExpanded = isAppBackdropExpanded,
                        onToggle = { isAppBackdropExpanded = !isAppBackdropExpanded },
                        testTag = "settings_section_app_backdrop"
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(RedSpacing.sm)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(RedSpacing.sm)
                            ) {
                                SpatialBackdropChip(
                                    label = if (isFa) "خاموش" else "Off",
                                    selected = uiState.appBackdropMode == AppBackdropMode.NONE,
                                    onClick = { viewModel.setAppBackdropMode(AppBackdropMode.NONE) },
                                    modifier = Modifier.testTag("settings_backdrop_off")
                                )
                                SpatialBackdropChip(
                                    label = stringResource(R.string.live_sky_setting),
                                    selected = uiState.appBackdropMode == AppBackdropMode.LIVE_SKY,
                                    onClick = { viewModel.setAppBackdropMode(AppBackdropMode.LIVE_SKY) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("settings_live_sky_switch")
                                )
                                SpatialBackdropChip(
                                    label = stringResource(R.string.backdrop_ngc1929_setting),
                                    selected = uiState.appBackdropMode == AppBackdropMode.NGC_1929,
                                    onClick = { viewModel.setAppBackdropMode(AppBackdropMode.NGC_1929) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("settings_backdrop_ngc1929")
                                )
                            }
                            SpatialBackdropChip(
                                label = stringResource(R.string.backdrop_cepheus_setting),
                                selected = uiState.appBackdropMode == AppBackdropMode.CEPHEUS,
                                onClick = { viewModel.setAppBackdropMode(AppBackdropMode.CEPHEUS) },
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
                                color = palette.textSecondary
                            )
                        }
                    }

                    // 6. Bortle Light Pollution
                    SpatialExpandableSection(
                        title = if (isFa) "آلودگی نوری (مقیاس بورتل)" else "Light Pollution (Bortle Scale)",
                        currentValueLabel = bortleLabel,
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
                                color = palette.textPrimary
                            )
                            val rawB = "${uiState.bortleClass}/9"
                            DotMatrixText(
                                text = if (isFa) TimeEngine.formatPersianNumbers(rawB) else rawB,
                                dotSize = 2.0.dp,
                                activeColor = palette.dotMatrixInk,
                                glowColor = SpatialNeonRose
                            )
                        }

                        SpatialGlowSlider(
                            value = uiState.bortleClass.toFloat(),
                            onValueChange = { viewModel.setBortleClass(it.toInt().coerceIn(1, 9)) },
                            valueRange = 1f..9f,
                            steps = 7,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bortle_class_slider")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Confirm Button Row (Matches SettingsDialog)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    SpatialMeshPill(
                        preset = SpatialMeshPreset.ROSE_TO_ORANGE,
                        isCapsule = true,
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 9.dp),
                        onClick = onDismiss
                    ) {
                        Text(
                            text = stringResource(R.string.ok),
                            style = RedTypographyTokens.bodyPrimary.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF23111C)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpatialExpandableSection(
    title: String,
    currentValueLabel: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    testTag: String,
    content: @Composable () -> Unit
) {
    val palette = SpatialTheme.palette
    val chevronDeg by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 360f),
        label = "SpatialSettingsChevron"
    )

    SpatialGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        cornerRadius = 24.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        elevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = palette.accentInk,
                    modifier = Modifier.size(18.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = RedTypographyTokens.bodyPrimary.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp
                        ),
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentValueLabel,
                        style = RedTypographyTokens.caption.copy(fontSize = 11.5.sp),
                        color = palette.accentInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = null,
                tint = palette.textSecondary,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(chevronDeg)
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(spring(dampingRatio = 0.78f, stiffness = 340f)) + fadeIn(),
            exit = shrinkVertically(spring(dampingRatio = 0.85f, stiffness = 380f)) + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SpatialSettingsOptionRow(
    title: String,
    subtitle: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = SpatialTheme.palette
    val shape = squircleShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (isSelected) SpatialNeonRose.copy(alpha = if (palette.isDark) 0.24f else 0.18f)
                else Color.Transparent
            )
            .border(
                BorderStroke(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        if (isSelected) listOf(Color.White.copy(alpha = 0.72f), SpatialNeonRose.copy(alpha = 0.45f))
                        else listOf(palette.rimSideBorder.copy(alpha = 0.45f), Color.Transparent)
                    )
                ),
                shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = RedTypographyTokens.bodyPrimary.copy(
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = if (isSelected) palette.accentInk else palette.textPrimary
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = RedTypographyTokens.caption.copy(fontSize = 11.sp),
                    color = palette.textSecondary
                )
            }
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(SpatialNeonRose.copy(alpha = 0.32f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = palette.accentInk,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

@Composable
private fun SpatialBackdropChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = SpatialTheme.palette
    if (selected) {
        SpatialMeshPill(
            preset = SpatialMeshPreset.ROSE_TO_ORANGE,
            isCapsule = true,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
            onClick = onClick,
            modifier = modifier
        ) {
            Text(
                text = label,
                style = RedTypographyTokens.caption.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                color = Color(0xFF23111C),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    } else {
        Box(
            modifier = modifier
                .clip(RedShapes.capsule)
                .background(palette.glassFill)
                .border(1.dp, palette.rimSideBorder, RedShapes.capsule)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = RedTypographyTokens.caption.copy(fontSize = 11.sp),
                color = palette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

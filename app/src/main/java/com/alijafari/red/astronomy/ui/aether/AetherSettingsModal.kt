package com.alijafari.red.astronomy.ui.aether

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.alijafari.red.astronomy.ui.theme.LocalAppFontFamily
import com.alijafari.red.astronomy.ui.theme.RedTypographyTokens
import com.alijafari.red.astronomy.ui.theme.squircleShape
import java.util.Locale
import kotlin.math.roundToInt

private fun String.aetherSettingsDigits(isFa: Boolean): String =
    if (isFa) TimeEngine.formatPersianNumbers(this) else this

@Composable
fun AetherSettingsModal(
    uiState: MainUiState,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val palette = AetherTheme.palette
    val appFont = LocalAppFontFamily.current
    val isFa = uiState.language == AppLanguage.PERSIAN
    var expandedSection by remember { mutableIntStateOf(-1) }

    val modalShape = squircleShape(AetherCornerGeometry.heroCard)
    val modalBaseColor = when (uiState.themeMode) {
        ThemeMode.WINE -> Color(0xF516030B)
        ThemeMode.DARK -> Color(0xF50C0D15)
        ThemeMode.LIGHT -> Color(0xFAFFFFFF)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .shadow(
                    elevation = 28.dp,
                    shape = modalShape,
                    ambientColor = palette.primaryAccent.copy(alpha = 0.35f),
                    spotColor = palette.primaryAccent.copy(alpha = 0.40f)
                )
                .clip(modalShape)
                .background(modalBaseColor)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                palette.primaryAccent.copy(alpha = if (palette.isDark) 0.22f else 0.10f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.88f, size.height * 0.12f),
                            radius = size.maxDimension * 0.72f
                        ),
                        radius = size.maxDimension * 0.72f,
                        center = Offset(size.width * 0.88f, size.height * 0.12f)
                    )
                }
                .specularRimBorder(
                    shape = modalShape,
                    highlightStart = palette.specularRimStart,
                    highlightMid = palette.specularRimMid,
                    highlightCenter = palette.specularRimCenter,
                    highlightBounce = palette.specularRimEnd
                )
                .padding(22.dp)
                .testTag("settings_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 620.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.app_settings),
                            style = RedTypographyTokens.sectionTitle.copy(
                                fontFamily = appFont,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = if (isFa) 0.sp else (-0.3).sp
                            ),
                            color = palette.textPrimary
                        )
                        Text(
                            text = if (isFa) "پیکربندی رابط فضایی و تله‌متری اتر" else "AETHER TELEMETRY // SPATIAL SYSTEM",
                            style = RedTypographyTokens.caption.copy(
                                fontFamily = appFont,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Light
                            ),
                            color = palette.textMicro
                        )
                    }

                    AetherMicroIconDisc(
                        icon = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                        onClick = onDismiss,
                        size = 34.dp,
                        iconSize = 16.dp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 0. UI Style
                    val currentUiStyleLabel = when (uiState.uiStyle) {
                        AppUiStyle.DEFAULT -> stringResource(R.string.ui_style_default)
                        AppUiStyle.SPATIAL -> stringResource(R.string.ui_style_spatial)
                        AppUiStyle.CYBER -> stringResource(R.string.ui_style_cyber)
                        AppUiStyle.AETHER -> stringResource(R.string.ui_style_aether)
                    }
                    AetherSettingsExpandableRow(
                        icon = Icons.Outlined.Widgets,
                        title = stringResource(R.string.ui_style_title),
                        currentValue = currentUiStyleLabel,
                        expanded = expandedSection == 0,
                        onToggle = { expandedSection = if (expandedSection == 0) -1 else 0 },
                        tag = "settings_section_ui_style"
                    ) {
                        AetherSettingsOptionItem(
                            title = stringResource(R.string.ui_style_default),
                            subtitle = stringResource(R.string.ui_style_default_desc),
                            selected = uiState.uiStyle == AppUiStyle.DEFAULT,
                            onClick = { viewModel.setUiStyle(AppUiStyle.DEFAULT) },
                            tag = "settings_ui_style_default"
                        )
                        AetherSettingsOptionItem(
                            title = stringResource(R.string.ui_style_spatial),
                            subtitle = stringResource(R.string.ui_style_spatial_desc),
                            selected = uiState.uiStyle == AppUiStyle.SPATIAL,
                            onClick = { viewModel.setUiStyle(AppUiStyle.SPATIAL) },
                            tag = "settings_ui_style_spatial"
                        )
                        AetherSettingsOptionItem(
                            title = stringResource(R.string.ui_style_cyber),
                            subtitle = stringResource(R.string.ui_style_cyber_desc),
                            selected = uiState.uiStyle == AppUiStyle.CYBER,
                            onClick = { viewModel.setUiStyle(AppUiStyle.CYBER) },
                            tag = "settings_ui_style_cyber"
                        )
                        AetherSettingsOptionItem(
                            title = stringResource(R.string.ui_style_aether),
                            subtitle = stringResource(R.string.ui_style_aether_desc),
                            selected = uiState.uiStyle == AppUiStyle.AETHER,
                            onClick = { viewModel.setUiStyle(AppUiStyle.AETHER) },
                            tag = "settings_ui_style_aether"
                        )
                    }

                    // 1. Language
                    val currentLangLabel = if (isFa) {
                        stringResource(R.string.language_persian)
                    } else {
                        stringResource(R.string.language_english)
                    }
                    AetherSettingsExpandableRow(
                        icon = Icons.Default.Language,
                        title = stringResource(R.string.application_language),
                        currentValue = currentLangLabel,
                        expanded = expandedSection == 1,
                        onToggle = { expandedSection = if (expandedSection == 1) -1 else 1 },
                        tag = "settings_section_language"
                    ) {
                        AetherSettingsOptionItem(
                            title = stringResource(R.string.language_persian),
                            subtitle = "فارسی (Persian)",
                            selected = uiState.language == AppLanguage.PERSIAN,
                            onClick = { viewModel.setLanguage(AppLanguage.PERSIAN) }
                        )
                        AetherSettingsOptionItem(
                            title = stringResource(R.string.language_english),
                            subtitle = "English (US)",
                            selected = uiState.language == AppLanguage.ENGLISH,
                            onClick = { viewModel.setLanguage(AppLanguage.ENGLISH) }
                        )
                    }

                    // 2. Calendar System
                    val currentCalLabel = when (uiState.calendarSystem) {
                        CalendarSystem.SOLAR_HIJRI -> stringResource(R.string.calendar_solar_hijri)
                        CalendarSystem.GREGORIAN -> stringResource(R.string.calendar_gregorian)
                    }
                    AetherSettingsExpandableRow(
                        icon = Icons.Default.CalendarToday,
                        title = stringResource(R.string.calendar_system),
                        currentValue = currentCalLabel,
                        expanded = expandedSection == 2,
                        onToggle = { expandedSection = if (expandedSection == 2) -1 else 2 },
                        tag = "settings_section_calendar"
                    ) {
                        AetherSettingsOptionItem(
                            title = stringResource(R.string.calendar_solar_hijri),
                            subtitle = if (isFa) "تقویم خورشیدی رسمی ایران" else "Solar Hijri Calendar",
                            selected = uiState.calendarSystem == CalendarSystem.SOLAR_HIJRI,
                            onClick = { viewModel.setCalendarSystem(CalendarSystem.SOLAR_HIJRI) }
                        )
                        AetherSettingsOptionItem(
                            title = stringResource(R.string.calendar_gregorian),
                            subtitle = if (isFa) "تقویم بین‌المللی میلادی" else "Gregorian International Calendar",
                            selected = uiState.calendarSystem == CalendarSystem.GREGORIAN,
                            onClick = { viewModel.setCalendarSystem(CalendarSystem.GREGORIAN) }
                        )
                    }

                    // 3. App Theme
                    val currentThemeLabel = when (uiState.themeMode) {
                        ThemeMode.LIGHT -> if (isFa) "روشن" else "Light"
                        ThemeMode.DARK -> if (isFa) "تاریک" else "Dark"
                        ThemeMode.WINE -> stringResource(R.string.theme_wine)
                    }
                    AetherSettingsExpandableRow(
                        icon = Icons.Default.Palette,
                        title = if (isFa) "پوسته برنامه" else "App Theme",
                        currentValue = currentThemeLabel,
                        expanded = expandedSection == 3,
                        onToggle = { expandedSection = if (expandedSection == 3) -1 else 3 },
                        tag = "settings_section_app_theme"
                    ) {
                        AetherSettingsOptionItem(
                            title = if (isFa) "روشن" else "Light",
                            selected = uiState.themeMode == ThemeMode.LIGHT,
                            onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                            tag = "settings_theme_light"
                        )
                        AetherSettingsOptionItem(
                            title = if (isFa) "تاریک" else "Dark",
                            selected = uiState.themeMode == ThemeMode.DARK,
                            onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                            tag = "settings_theme_dark"
                        )
                        AetherSettingsOptionItem(
                            title = stringResource(R.string.theme_wine),
                            subtitle = stringResource(R.string.theme_wine_desc),
                            selected = uiState.themeMode == ThemeMode.WINE,
                            onClick = { viewModel.setThemeMode(ThemeMode.WINE) },
                            tag = "settings_theme_wine"
                        )
                    }

                    // 4. Sky Canvas Theme
                    val currentSkyLabel = when (uiState.skyCanvasTheme) {
                        SkyCanvasTheme.REAL_SKY -> if (isFa) "آسمان واقعی" else "Real Sky"
                        SkyCanvasTheme.ATMOSPHERIC_SKY -> if (isFa) "آسمان جوی" else "Atmospheric"
                        SkyCanvasTheme.MONOCHROME_SCIENTIFIC -> if (isFa) "تک‌رنگ علمی" else "Monochrome"
                        SkyCanvasTheme.KIDS_WATERCOLOR -> if (isFa) "آبرنگ" else "Watercolor"
                        SkyCanvasTheme.OBSERVATORY -> if (isFa) "رصدخانه" else "Observatory"
                        SkyCanvasTheme.PAPERCRAFT_DIORAMA -> if (isFa) "دیوراما" else "Papercraft"
                    }
                    AetherSettingsExpandableRow(
                        icon = Icons.Outlined.NightsStay,
                        title = if (isFa) "سبک هنری آسمان (RMAE)" else "Sky Canvas Theme",
                        currentValue = currentSkyLabel,
                        expanded = expandedSection == 4,
                        onToggle = { expandedSection = if (expandedSection == 4) -1 else 4 },
                        tag = "settings_section_sky_theme"
                    ) {
                        listOf(
                            SkyCanvasTheme.REAL_SKY to (if (isFa) "آسمان واقعی (Real Sky)" else "Real Sky"),
                            SkyCanvasTheme.ATMOSPHERIC_SKY to (if (isFa) "آسمان جوی (Atmospheric)" else "Atmospheric Sky"),
                            SkyCanvasTheme.MONOCHROME_SCIENTIFIC to (if (isFa) "تک‌رنگ علمی (Monochrome)" else "Monochrome Scientific"),
                            SkyCanvasTheme.KIDS_WATERCOLOR to (if (isFa) "آبرنگ کودکانه (Watercolor)" else "Kids Watercolor"),
                            SkyCanvasTheme.OBSERVATORY to (if (isFa) "رصدخانه (Observatory)" else "Observatory"),
                            SkyCanvasTheme.PAPERCRAFT_DIORAMA to (if (isFa) "دیوراما کاغذ برجسته (Papercraft)" else "Papercraft Diorama")
                        ).forEach { (theme, label) ->
                            AetherSettingsOptionItem(
                                title = label,
                                selected = uiState.skyCanvasTheme == theme,
                                onClick = { viewModel.setSkyCanvasTheme(theme) }
                            )
                        }
                    }

                    // 5. App Backdrop
                    val currentBackdropLabel = when (uiState.appBackdropMode) {
                        AppBackdropMode.NONE -> if (isFa) "خاموش (مش فضایی اتر)" else "Off (Aether Ambient Mesh)"
                        AppBackdropMode.LIVE_SKY -> stringResource(R.string.live_sky_setting)
                        AppBackdropMode.NGC_1929 -> stringResource(R.string.backdrop_ngc1929_setting)
                        AppBackdropMode.CEPHEUS -> stringResource(R.string.backdrop_cepheus_setting)
                    }
                    AetherSettingsExpandableRow(
                        icon = Icons.Default.AutoAwesome,
                        title = stringResource(R.string.app_backdrop_title),
                        currentValue = currentBackdropLabel,
                        expanded = expandedSection == 5,
                        onToggle = { expandedSection = if (expandedSection == 5) -1 else 5 },
                        tag = "settings_section_app_backdrop"
                    ) {
                        AetherSettingsOptionItem(
                            title = if (isFa) "خاموش (مش فضایی اتر)" else "Off (Aether Ambient Mesh)",
                            subtitle = stringResource(R.string.backdrop_off_desc),
                            selected = uiState.appBackdropMode == AppBackdropMode.NONE,
                            onClick = { viewModel.setAppBackdropMode(AppBackdropMode.NONE) },
                            tag = "settings_backdrop_off"
                        )
                        AetherSettingsOptionItem(
                            title = stringResource(R.string.live_sky_setting),
                            subtitle = stringResource(R.string.live_sky_desc),
                            selected = uiState.appBackdropMode == AppBackdropMode.LIVE_SKY,
                            onClick = { viewModel.setAppBackdropMode(AppBackdropMode.LIVE_SKY) },
                            tag = "settings_live_sky_switch"
                        )
                        AetherSettingsOptionItem(
                            title = stringResource(R.string.backdrop_ngc1929_setting),
                            subtitle = stringResource(R.string.backdrop_ngc1929_desc),
                            selected = uiState.appBackdropMode == AppBackdropMode.NGC_1929,
                            onClick = { viewModel.setAppBackdropMode(AppBackdropMode.NGC_1929) },
                            tag = "settings_backdrop_ngc1929"
                        )
                        AetherSettingsOptionItem(
                            title = stringResource(R.string.backdrop_cepheus_setting),
                            subtitle = stringResource(R.string.backdrop_cepheus_desc),
                            selected = uiState.appBackdropMode == AppBackdropMode.CEPHEUS,
                            onClick = { viewModel.setAppBackdropMode(AppBackdropMode.CEPHEUS) },
                            tag = "settings_backdrop_cepheus"
                        )
                    }

                    // 6. Bortle Light Pollution Scale (with Rolling Dot-Matrix & Micro-Stepper)
                    AetherSettingsExpandableRow(
                        icon = Icons.Default.Brightness6,
                        title = if (isFa) "آلودگی نوری (مقیاس بورتل)" else "Light Pollution (Bortle Scale)",
                        currentValue = "Class ${uiState.bortleClass}".aetherSettingsDigits(isFa),
                        expanded = expandedSection == 6,
                        onToggle = { expandedSection = if (expandedSection == 6) -1 else 6 },
                        tag = "settings_section_bortle"
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AetherRollingDotMatrixCounter(
                                text = String.format(Locale.US, "0%d", uiState.bortleClass).aetherSettingsDigits(isFa),
                                dotSize = 2.8.dp,
                                dotGap = 0.95.dp,
                                charGap = 2.8.dp,
                                activeColor = palette.glowWhite,
                                glowColor = palette.primaryAccent
                            )

                            AetherMicroStepper(
                                onDecrement = {
                                    viewModel.setBortleClass((uiState.bortleClass - 1).coerceIn(1, 9))
                                },
                                onIncrement = {
                                    viewModel.setBortleClass((uiState.bortleClass + 1).coerceIn(1, 9))
                                },
                                onReset = { viewModel.setBortleClass(3) },
                                tint = palette.textPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        AetherTelemetrySlider(
                            value = uiState.bortleClass.toFloat(),
                            onValueChange = { viewModel.setBortleClass(it.roundToInt().coerceIn(1, 9)) },
                            valueRange = 1f..9f,
                            steps = 7,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bortle_class_slider")
                        )
                    }

                    HorizontalDivider(
                        color = palette.mutedTelemetry.copy(alpha = 0.16f),
                        thickness = 0.75.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // 7. Developer Credit Card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(squircleShape(18.dp))
                            .background(palette.frostedGlassFill)
                            .specularRimBorder(cornerRadius = 18.dp)
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("developer_credit_card"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isFa) "توسعه‌دهنده" else "Developed by",
                                style = RedTypographyTokens.caption.copy(
                                    fontFamily = appFont,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Light
                                ),
                                color = palette.textMicro
                            )
                            Text(
                                text = if (isFa) "علی جعفری" else "Ali Jafari",
                                style = RedTypographyTokens.bodyPrimary.copy(
                                    fontFamily = appFont,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = palette.textPrimary
                            )
                        }
                        AetherMinimalPillBadge(
                            text = "ZIG • AETHER",
                            textColor = palette.primaryAccent,
                            borderColor = palette.primaryAccent.copy(alpha = 0.45f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AetherSettingsExpandableRow(
    icon: ImageVector,
    title: String,
    currentValue: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    tag: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = AetherTheme.palette
    val appFont = LocalAppFontFamily.current
    val haptics = LocalHapticFeedback.current
    val rowShape = squircleShape(20.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(rowShape)
            .background(palette.frostedGlassFill)
            .specularRimBorder(
                shape = rowShape,
                highlightStart = palette.specularRimStart,
                highlightMid = palette.specularRimMid,
                highlightCenter = palette.specularRimCenter,
                highlightBounce = palette.specularRimEnd
            )
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggle()
                }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = palette.primaryAccent,
                    modifier = Modifier.size(18.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = RedTypographyTokens.bodyPrimary.copy(
                            fontFamily = appFont,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentValue,
                        style = RedTypographyTokens.caption.copy(
                            fontFamily = appFont,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Light
                        ),
                        color = palette.textMicro,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = palette.textMicro,
                modifier = Modifier.size(18.dp)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                content = content
            )
        }
    }
}

@Composable
private fun AetherSettingsOptionItem(
    title: String,
    subtitle: String? = null,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String? = null
) {
    val palette = AetherTheme.palette
    val appFont = LocalAppFontFamily.current
    val haptics = LocalHapticFeedback.current
    val itemShape = squircleShape(14.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(itemShape)
            .background(
                if (selected) palette.primaryAccent.copy(alpha = if (palette.isDark) 0.24f else 0.12f)
                else Color.Transparent
            )
            .border(
                width = 1.dp,
                color = if (selected) palette.primaryAccent.copy(alpha = 0.65f) else palette.mutedTelemetry.copy(alpha = 0.16f),
                shape = itemShape
            )
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 12.dp, vertical = 9.dp)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = RedTypographyTokens.bodySecondary.copy(
                    fontFamily = appFont,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                ),
                color = palette.textPrimary
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = RedTypographyTokens.caption.copy(
                        fontFamily = appFont,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Light
                    ),
                    color = palette.textMicro
                )
            }
        }

        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (selected) palette.glowWhite else Color.Transparent)
                .border(
                    1.dp,
                    if (selected) palette.primaryAccent else palette.mutedTelemetry,
                    CircleShape
                )
        )
    }
}

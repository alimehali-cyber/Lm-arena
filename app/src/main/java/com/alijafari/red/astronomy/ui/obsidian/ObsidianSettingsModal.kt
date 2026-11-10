package com.alijafari.red.astronomy.ui.obsidian

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
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
import com.alijafari.red.astronomy.ui.components.SafeAppLogo

/**
 * Obsidian Quartz & Crimson Telemetry — Settings.
 *
 * A smoked frosted quartz plate with the same optic as every other Obsidian card, exposing all
 * seven configuration sections. Every affordance is a Canvas hairline glyph; selection is a
 * lit dot + 1.1.dp check mark, never a Material icon.
 */
@Composable
fun ObsidianSettingsModal(
    uiState: MainUiState,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val palette = ObsidianTheme.palette
    val context = LocalContext.current
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
        AppBackdropMode.NONE -> if (isFa) "خاموش (سیلوئت)" else "Off (Silhouette)"
        AppBackdropMode.LIVE_SKY -> stringResource(R.string.live_sky_setting)
        AppBackdropMode.NGC_1929 -> stringResource(R.string.backdrop_ngc1929_setting)
        AppBackdropMode.CEPHEUS -> stringResource(R.string.backdrop_cepheus_setting)
    }
    val bortleLabel = "BORTLE " + if (isFa) {
        TimeEngine.formatPersianNumbers(uiState.bortleClass.toString())
    } else {
        uiState.bortleClass.toString()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.canvasBg.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .heightIn(max = 760.dp)
                    .clip(RoundedCornerShape(34.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1D242B).copy(alpha = 0.96f),
                                Color(0xFF150C12).copy(alpha = 0.97f)
                            )
                        )
                    )
                    .opticalQuartzSurface(
                        cornerRadius = 34.dp,
                        innerRefractionColor = Color.White.copy(alpha = 0.3f),
                        rimHighlightAlpha = 0.52f
                    )
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .testTag("settings_dialog")
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        ObsidianVectorIconGlyph(
                            icon = ObsidianVectorIcon.SETTINGS,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White.copy(alpha = 0.9f)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isFa) "تنظیمات" else "Settings",
                            color = Color.White.copy(alpha = 0.96f),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = (-0.3).sp
                        )
                        Text(
                            text = if (isFa) "پیکربندی تله‌متری" else "TELEMETRY CONFIGURATION",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.4.sp
                        )
                    }
                    ObsidianSignalDot(modifier = Modifier.size(6.dp))
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Interface Style — the UI selector
                    ObsidianSettingsSection(
                        title = stringResource(R.string.ui_style_title),
                        valueLabel = uiStyleLabel,
                        icon = ObsidianVectorIcon.PALETTE,
                        isExpanded = isUiStyleExpanded,
                        onToggle = { isUiStyleExpanded = !isUiStyleExpanded },
                        testTag = "settings_section_ui_style"
                    ) {
                        ObsidianOptionRow(
                            title = stringResource(R.string.ui_style_default),
                            subtitle = stringResource(R.string.ui_style_default_desc),
                            isSelected = uiState.uiStyle == AppUiStyle.DEFAULT,
                            onClick = { viewModel.setUiStyle(AppUiStyle.DEFAULT) },
                            modifier = Modifier.testTag("settings_ui_style_default")
                        )
                        ObsidianOptionRow(
                            title = stringResource(R.string.ui_style_spatial),
                            subtitle = stringResource(R.string.ui_style_spatial_desc),
                            isSelected = uiState.uiStyle == AppUiStyle.SPATIAL,
                            onClick = { viewModel.setUiStyle(AppUiStyle.SPATIAL) },
                            modifier = Modifier.testTag("settings_ui_style_spatial")
                        )
                        ObsidianOptionRow(
                            title = stringResource(R.string.ui_style_cyber),
                            subtitle = stringResource(R.string.ui_style_cyber_desc),
                            isSelected = uiState.uiStyle == AppUiStyle.CYBER,
                            onClick = { viewModel.setUiStyle(AppUiStyle.CYBER) },
                            modifier = Modifier.testTag("settings_ui_style_cyber")
                        )
                        ObsidianOptionRow(
                            title = stringResource(R.string.ui_style_obsidian),
                            subtitle = stringResource(R.string.ui_style_obsidian_desc),
                            isSelected = uiState.uiStyle == AppUiStyle.OBSIDIAN,
                            onClick = { viewModel.setUiStyle(AppUiStyle.OBSIDIAN) },
                            modifier = Modifier.testTag("settings_ui_style_obsidian")
                        )
                    }

                    // 2. Language
                    ObsidianSettingsSection(
                        title = stringResource(R.string.application_language),
                        valueLabel = languageLabel,
                        icon = ObsidianVectorIcon.LANGUAGE,
                        isExpanded = isLanguageExpanded,
                        onToggle = { isLanguageExpanded = !isLanguageExpanded },
                        testTag = "settings_section_language"
                    ) {
                        ObsidianOptionRow(
                            title = stringResource(R.string.language_persian),
                            isSelected = uiState.language == AppLanguage.PERSIAN,
                            onClick = { viewModel.setLanguage(AppLanguage.PERSIAN) }
                        )
                        ObsidianOptionRow(
                            title = stringResource(R.string.language_english),
                            isSelected = uiState.language == AppLanguage.ENGLISH,
                            onClick = { viewModel.setLanguage(AppLanguage.ENGLISH) }
                        )
                    }

                    // 3. Calendar
                    ObsidianSettingsSection(
                        title = stringResource(R.string.calendar_system),
                        valueLabel = calendarLabel,
                        icon = ObsidianVectorIcon.CALENDAR,
                        isExpanded = isCalendarExpanded,
                        onToggle = { isCalendarExpanded = !isCalendarExpanded },
                        testTag = "settings_section_calendar"
                    ) {
                        ObsidianOptionRow(
                            title = stringResource(R.string.calendar_solar_hijri),
                            isSelected = uiState.calendarSystem == CalendarSystem.SOLAR_HIJRI,
                            onClick = { viewModel.setCalendarSystem(CalendarSystem.SOLAR_HIJRI) }
                        )
                        ObsidianOptionRow(
                            title = stringResource(R.string.calendar_gregorian),
                            isSelected = uiState.calendarSystem == CalendarSystem.GREGORIAN,
                            onClick = { viewModel.setCalendarSystem(CalendarSystem.GREGORIAN) }
                        )
                    }

                    // 4. App Theme
                    ObsidianSettingsSection(
                        title = if (isFa) "پوسته برنامه" else "App Theme",
                        valueLabel = appThemeLabel,
                        icon = ObsidianVectorIcon.PALETTE,
                        isExpanded = isAppThemeExpanded,
                        onToggle = { isAppThemeExpanded = !isAppThemeExpanded },
                        testTag = "settings_section_app_theme"
                    ) {
                        ObsidianOptionRow(
                            title = if (isFa) "روشن" else "Light",
                            isSelected = uiState.themeMode == ThemeMode.LIGHT,
                            onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) }
                        )
                        ObsidianOptionRow(
                            title = if (isFa) "تاریک" else "Dark",
                            isSelected = uiState.themeMode == ThemeMode.DARK,
                            onClick = { viewModel.setThemeMode(ThemeMode.DARK) }
                        )
                        ObsidianOptionRow(
                            title = stringResource(R.string.theme_wine),
                            subtitle = stringResource(R.string.theme_wine_desc),
                            isSelected = uiState.themeMode == ThemeMode.WINE,
                            onClick = { viewModel.setThemeMode(ThemeMode.WINE) },
                            modifier = Modifier.testTag("settings_theme_wine")
                        )
                    }

                    // 5. Sky Canvas Theme
                    ObsidianSettingsSection(
                        title = if (isFa) "سبک هنری آسمان" else "Sky Canvas Theme",
                        valueLabel = skyCanvasThemeLabel,
                        icon = ObsidianVectorIcon.SKY,
                        isExpanded = isSkyCanvasThemeExpanded,
                        onToggle = { isSkyCanvasThemeExpanded = !isSkyCanvasThemeExpanded },
                        testTag = "settings_section_sky_theme"
                    ) {
                        listOf(
                            SkyCanvasTheme.REAL_SKY to (if (isFa) "آسمان واقعی" else "Real Sky"),
                            SkyCanvasTheme.ATMOSPHERIC_SKY to (if (isFa) "آسمان جوی" else "Atmospheric Sky"),
                            SkyCanvasTheme.MONOCHROME_SCIENTIFIC to (if (isFa) "تک‌رنگ علمی" else "Monochrome Scientific"),
                            SkyCanvasTheme.KIDS_WATERCOLOR to (if (isFa) "آبرنگ کودکانه" else "Kids Watercolor"),
                            SkyCanvasTheme.OBSERVATORY to (if (isFa) "رصدخانه" else "Observatory"),
                            SkyCanvasTheme.PAPERCRAFT_DIORAMA to (if (isFa) "دیوراما کاغذی" else "Papercraft Diorama")
                        ).forEach { (theme, label) ->
                            ObsidianOptionRow(
                                title = label,
                                isSelected = uiState.skyCanvasTheme == theme,
                                onClick = { viewModel.setSkyCanvasTheme(theme) }
                            )
                        }
                    }

                    // 6. App Backdrop
                    ObsidianSettingsSection(
                        title = stringResource(R.string.app_backdrop_title),
                        valueLabel = backdropLabel,
                        icon = ObsidianVectorIcon.BACKDROP,
                        isExpanded = isAppBackdropExpanded,
                        onToggle = { isAppBackdropExpanded = !isAppBackdropExpanded },
                        testTag = "settings_section_app_backdrop"
                    ) {
                        ObsidianOptionRow(
                            title = if (isFa) "خاموش (سیلوئت ادیتوریال)" else "Off (Editorial Silhouette)",
                            subtitle = stringResource(R.string.backdrop_off_desc),
                            isSelected = uiState.appBackdropMode == AppBackdropMode.NONE,
                            onClick = { viewModel.setAppBackdropMode(AppBackdropMode.NONE) },
                            modifier = Modifier.testTag("settings_backdrop_off")
                        )
                        ObsidianOptionRow(
                            title = stringResource(R.string.live_sky_setting),
                            subtitle = stringResource(R.string.live_sky_desc),
                            isSelected = uiState.appBackdropMode == AppBackdropMode.LIVE_SKY,
                            onClick = { viewModel.setAppBackdropMode(AppBackdropMode.LIVE_SKY) },
                            modifier = Modifier.testTag("settings_live_sky_switch")
                        )
                        ObsidianOptionRow(
                            title = stringResource(R.string.backdrop_ngc1929_setting),
                            subtitle = stringResource(R.string.backdrop_ngc1929_desc),
                            isSelected = uiState.appBackdropMode == AppBackdropMode.NGC_1929,
                            onClick = { viewModel.setAppBackdropMode(AppBackdropMode.NGC_1929) },
                            modifier = Modifier.testTag("settings_backdrop_ngc1929")
                        )
                        ObsidianOptionRow(
                            title = stringResource(R.string.backdrop_cepheus_setting),
                            subtitle = stringResource(R.string.backdrop_cepheus_desc),
                            isSelected = uiState.appBackdropMode == AppBackdropMode.CEPHEUS,
                            onClick = { viewModel.setAppBackdropMode(AppBackdropMode.CEPHEUS) },
                            modifier = Modifier.testTag("settings_backdrop_cepheus")
                        )
                    }

                    // 7. Bortle scale
                    ObsidianSettingsSection(
                        title = if (isFa) "آلودگی نوری (بورتل)" else "Light Pollution (Bortle)",
                        valueLabel = bortleLabel,
                        icon = ObsidianVectorIcon.BORTLE,
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
                                text = if (isFa) "کلاس تاریکی آسمان" else "DARKNESS CLASS",
                                color = Color.White.copy(alpha = 0.55f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 1.4.sp
                            )
                            DotMatrixNumber(
                                value = uiState.bortleClass.toString().padStart(2, '0'),
                                dotSize = 2.2.dp,
                                dotSpacing = 0.85.dp,
                                charSpacing = 2.4.dp,
                                ghostMatrix = true
                            )
                        }

                        ObsidianHairlineSlider(
                            value = uiState.bortleClass.toFloat(),
                            onValueChange = { viewModel.setBortleClass(it.toInt().coerceIn(1, 9)) },
                            valueRange = 1f..9f,
                            steps = 7,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bortle_class_slider")
                        )
                    }

                    // Developer credit
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.06f))
                            .opticalQuartzSurface(
                                cornerRadius = 32.dp,
                                innerRefractionColor = Color.White.copy(alpha = 0.24f),
                                rimHighlightAlpha = 0.46f
                            )
                            .padding(horizontal = 16.dp)
                            .testTag("developer_credit_card"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f))
                        ) {
                            SafeAppLogo(modifier = Modifier.size(24.dp), cornerRadius = 8.dp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isFa) "توسعه‌دهنده: علی جعفری" else "Developed by Ali Jafari",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Light
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.clickable {
                                    try {
                                        context.startActivity(
                                            Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse("https://t.me/EdisonWasAThief")
                                            )
                                        )
                                    } catch (_: Exception) {
                                    }
                                }
                            ) {
                                ObsidianVectorIconGlyph(
                                    icon = ObsidianVectorIcon.ARROW_DIAGONAL,
                                    modifier = Modifier.size(8.dp),
                                    tint = Color.White.copy(alpha = 0.6f),
                                    strokeWidth = 1.1.dp
                                )
                                Text(
                                    text = "@EdisonWasAThief",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Light
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .height(42.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF4B0420), Color(0xFFB81450))
                                )
                            )
                            .opticalQuartzSurface(
                                cornerRadius = 21.dp,
                                innerRefractionColor = Color(0xFFFF7AA8).copy(alpha = 0.25f),
                                rimHighlightAlpha = 0.55f
                            )
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 26.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.ok),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.6.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ObsidianSettingsSection(
    title: String,
    valueLabel: String,
    icon: ObsidianVectorIcon,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    testTag: String,
    content: @Composable () -> Unit
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = OBSIDIAN_SPRING_DAMPING,
            stiffness = OBSIDIAN_SPRING_STIFFNESS * 1.3f
        ),
        label = "obsidianChevron"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .opticalQuartzSurface(
                cornerRadius = 22.dp,
                innerRefractionColor = Color.White.copy(alpha = 0.2f),
                rimHighlightAlpha = 0.4f
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .testTag(testTag),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ObsidianVectorIconGlyph(
                icon = icon,
                modifier = Modifier.size(15.dp),
                tint = Color.White.copy(alpha = 0.8f),
                strokeWidth = 1.1.dp
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White.copy(alpha = 0.94f),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Light,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = valueLabel,
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Light,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            ObsidianVectorIconGlyph(
                icon = ObsidianVectorIcon.EXPAND,
                modifier = Modifier
                    .size(12.dp)
                    .rotate(chevronRotation),
                tint = Color.White.copy(alpha = 0.6f),
                strokeWidth = 1.1.dp
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(
                spring(dampingRatio = OBSIDIAN_SPRING_DAMPING, stiffness = OBSIDIAN_SPRING_STIFFNESS)
            ) + fadeIn(),
            exit = shrinkVertically(
                spring(dampingRatio = OBSIDIAN_SPRING_DAMPING, stiffness = OBSIDIAN_SPRING_STIFFNESS * 1.2f)
            ) + fadeOut()
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
private fun ObsidianOptionRow(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) Color(0xFFB81450).copy(alpha = 0.22f) else Color.Transparent
            )
            .border(
                width = 0.8.dp,
                color = if (isSelected) {
                    Color(0xFFFF7AA8).copy(alpha = 0.45f)
                } else {
                    Color.White.copy(alpha = 0.1f)
                },
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                if (isSelected) {
                    ObsidianSignalDot(modifier = Modifier.size(5.dp), glowing = false)
                }
                Text(
                    text = title,
                    color = Color.White.copy(alpha = if (isSelected) 0.96f else 0.7f),
                    fontSize = 12.5.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Light,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Light,
                    maxLines = 2
                )
            }
        }

        if (isSelected) {
            Spacer(modifier = Modifier.width(8.dp))
            ObsidianVectorIconGlyph(
                icon = ObsidianVectorIcon.CHECK,
                modifier = Modifier.size(13.dp),
                tint = Color(0xFFFF8EA6),
                strokeWidth = 1.2.dp
            )
        }
    }
}

package com.alijafari.red.astronomy.ui.cyber

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.alijafari.red.astronomy.ui.theme.RedShapes
import com.alijafari.red.astronomy.ui.theme.RedSpacing
import com.alijafari.red.astronomy.ui.theme.RedTypographyTokens
import com.alijafari.red.astronomy.ui.theme.squircleShape

/**
 * Cyber Glass UI Settings Modal (`36.dp` Spatial Glass 2.0 Container):
 * Exposes all 7 expandable configuration sections:
 * 0. Interface Style (`Default UI`, `Spatial UI`, `Cyber Glass UI`)
 * 1. Application Language (`Persian`, `English`)
 * 2. Calendar System (`Solar Hijri`, `Gregorian`)
 * 3. App Theme (`Light`, `Dark`, `Wine / می`)
 * 4. Sky Canvas Theme
 * 5. App Backdrop
 * 6. Light Pollution (Bortle Scale with Nothing Dot-Matrix readout + CyberGlassSlider)
 */
@Composable
fun CyberSettingsModal(
    uiState: MainUiState,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val palette = CyberTheme.palette
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
        AppUiStyle.AURORA -> stringResource(R.string.ui_style_aurora)
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
        AppBackdropMode.NONE -> if (isFa) "خاموش (مش سایبر)" else "Off (Cyber Mesh)"
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
                .padding(horizontal = 18.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            CyberGlassWidget(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 650.dp)
                    .testTag("settings_dialog"),
                cornerRadius = 36.dp,
                contentPadding = PaddingValues(22.dp),
                elevated = true,
                ambientTint = palette.primaryAccent
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(palette.primaryAccent.copy(alpha = 0.16f))
                                .border(1.dp, palette.glassBorder, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = palette.primaryAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.app_settings),
                                style = RedTypographyTokens.sectionHeading.copy(
                                    fontWeight = FontWeight.Light,
                                    fontSize = 18.sp
                                ),
                                color = palette.textPrimary
                            )
                            Text(
                                text = if (isFa) "سیستم طراحی سایبر گلس ۲.۰" else "CYBER GLASS 2.0 // SYSTEM",
                                style = RedTypographyTokens.caption.copy(
                                    fontWeight = FontWeight.Light,
                                    fontSize = 10.sp
                                ),
                                color = palette.textMuted
                            )
                        }
                    }

                    CyberSignalDot(size = 8.dp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 0. Interface Style (Default, Spatial, Cyber Glass)
                    CyberExpandableSection(
                        title = stringResource(R.string.ui_style_title),
                        valueLabel = uiStyleLabel,
                        icon = Icons.Outlined.Widgets,
                        isExpanded = isUiStyleExpanded,
                        onToggle = { isUiStyleExpanded = !isUiStyleExpanded },
                        testTag = "settings_section_ui_style"
                    ) {
                        CyberOptionRow(
                            title = stringResource(R.string.ui_style_default),
                            subtitle = stringResource(R.string.ui_style_default_desc),
                            isSelected = uiState.uiStyle == AppUiStyle.DEFAULT,
                            onClick = { viewModel.setUiStyle(AppUiStyle.DEFAULT) },
                            modifier = Modifier.testTag("settings_ui_style_default")
                        )
                        CyberOptionRow(
                            title = stringResource(R.string.ui_style_spatial),
                            subtitle = stringResource(R.string.ui_style_spatial_desc),
                            isSelected = uiState.uiStyle == AppUiStyle.SPATIAL,
                            onClick = { viewModel.setUiStyle(AppUiStyle.SPATIAL) },
                            modifier = Modifier.testTag("settings_ui_style_spatial")
                        )
                        CyberOptionRow(
                            title = stringResource(R.string.ui_style_cyber),
                            subtitle = stringResource(R.string.ui_style_cyber_desc),
                            isSelected = uiState.uiStyle == AppUiStyle.CYBER,
                            onClick = { viewModel.setUiStyle(AppUiStyle.CYBER) },
                            modifier = Modifier.testTag("settings_ui_style_cyber")
                        )
                        CyberOptionRow(
                            title = stringResource(R.string.ui_style_aurora),
                            subtitle = stringResource(R.string.ui_style_aurora_desc),
                            isSelected = uiState.uiStyle == AppUiStyle.AURORA,
                            onClick = { viewModel.setUiStyle(AppUiStyle.AURORA) },
                            modifier = Modifier.testTag("settings_ui_style_aurora")
                        )
                    }

                    // 1. Application Language
                    CyberExpandableSection(
                        title = stringResource(R.string.application_language),
                        valueLabel = languageLabel,
                        icon = Icons.Default.Language,
                        isExpanded = isLanguageExpanded,
                        onToggle = { isLanguageExpanded = !isLanguageExpanded },
                        testTag = "settings_section_language"
                    ) {
                        CyberOptionRow(
                            title = stringResource(R.string.language_persian),
                            subtitle = "فارسی (Persian)",
                            isSelected = uiState.language == AppLanguage.PERSIAN,
                            onClick = { viewModel.setLanguage(AppLanguage.PERSIAN) }
                        )
                        CyberOptionRow(
                            title = stringResource(R.string.language_english),
                            subtitle = "English (US)",
                            isSelected = uiState.language == AppLanguage.ENGLISH,
                            onClick = { viewModel.setLanguage(AppLanguage.ENGLISH) }
                        )
                    }

                    // 2. Calendar System
                    CyberExpandableSection(
                        title = stringResource(R.string.calendar_system),
                        valueLabel = calendarLabel,
                        icon = Icons.Default.CalendarToday,
                        isExpanded = isCalendarExpanded,
                        onToggle = { isCalendarExpanded = !isCalendarExpanded },
                        testTag = "settings_section_calendar"
                    ) {
                        CyberOptionRow(
                            title = stringResource(R.string.calendar_solar_hijri),
                            subtitle = if (isFa) "تقویم خورشیدی رسمی ایران" else "Solar Hijri Calendar",
                            isSelected = uiState.calendarSystem == CalendarSystem.SOLAR_HIJRI,
                            onClick = { viewModel.setCalendarSystem(CalendarSystem.SOLAR_HIJRI) }
                        )
                        CyberOptionRow(
                            title = stringResource(R.string.calendar_gregorian),
                            subtitle = if (isFa) "تقویم بین‌المللی میلادی" else "Gregorian International Calendar",
                            isSelected = uiState.calendarSystem == CalendarSystem.GREGORIAN,
                            onClick = { viewModel.setCalendarSystem(CalendarSystem.GREGORIAN) }
                        )
                    }

                    // 3. App Theme (Light, Dark, Wine / می)
                    CyberExpandableSection(
                        title = if (isFa) "پوسته برنامه" else "App Theme",
                        valueLabel = appThemeLabel,
                        icon = Icons.Default.Palette,
                        isExpanded = isAppThemeExpanded,
                        onToggle = { isAppThemeExpanded = !isAppThemeExpanded },
                        testTag = "settings_section_app_theme"
                    ) {
                        CyberOptionRow(
                            title = if (isFa) "روشن" else "Light",
                            isSelected = uiState.themeMode == ThemeMode.LIGHT,
                            onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) }
                        )
                        CyberOptionRow(
                            title = if (isFa) "تاریک" else "Dark",
                            isSelected = uiState.themeMode == ThemeMode.DARK,
                            onClick = { viewModel.setThemeMode(ThemeMode.DARK) }
                        )
                        CyberOptionRow(
                            title = stringResource(R.string.theme_wine),
                            subtitle = stringResource(R.string.theme_wine_desc),
                            isSelected = uiState.themeMode == ThemeMode.WINE,
                            onClick = { viewModel.setThemeMode(ThemeMode.WINE) },
                            modifier = Modifier.testTag("settings_theme_wine")
                        )
                    }

                    // 4. Sky Canvas Theme
                    CyberExpandableSection(
                        title = if (isFa) "سبک هنری آسمان (RMAE)" else "Sky Canvas Theme",
                        valueLabel = skyCanvasThemeLabel,
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
                            CyberOptionRow(
                                title = label,
                                isSelected = uiState.skyCanvasTheme == theme,
                                onClick = { viewModel.setSkyCanvasTheme(theme) }
                            )
                        }
                    }

                    // 5. App Backdrop
                    CyberExpandableSection(
                        title = stringResource(R.string.app_backdrop_title),
                        valueLabel = backdropLabel,
                        icon = Icons.Outlined.NightsStay,
                        isExpanded = isAppBackdropExpanded,
                        onToggle = { isAppBackdropExpanded = !isAppBackdropExpanded },
                        testTag = "settings_section_app_backdrop"
                    ) {
                        CyberOptionRow(
                            title = if (isFa) "خاموش (مش سایبر)" else "Off (Cyber Ambient Mesh)",
                            subtitle = stringResource(R.string.backdrop_off_desc),
                            isSelected = uiState.appBackdropMode == AppBackdropMode.NONE,
                            onClick = { viewModel.setAppBackdropMode(AppBackdropMode.NONE) },
                            modifier = Modifier.testTag("settings_backdrop_off")
                        )
                        CyberOptionRow(
                            title = stringResource(R.string.live_sky_setting),
                            subtitle = stringResource(R.string.live_sky_desc),
                            isSelected = uiState.appBackdropMode == AppBackdropMode.LIVE_SKY,
                            onClick = { viewModel.setAppBackdropMode(AppBackdropMode.LIVE_SKY) },
                            modifier = Modifier.testTag("settings_live_sky_switch")
                        )
                        CyberOptionRow(
                            title = stringResource(R.string.backdrop_ngc1929_setting),
                            subtitle = stringResource(R.string.backdrop_ngc1929_desc),
                            isSelected = uiState.appBackdropMode == AppBackdropMode.NGC_1929,
                            onClick = { viewModel.setAppBackdropMode(AppBackdropMode.NGC_1929) },
                            modifier = Modifier.testTag("settings_backdrop_ngc1929")
                        )
                        CyberOptionRow(
                            title = stringResource(R.string.backdrop_cepheus_setting),
                            subtitle = stringResource(R.string.backdrop_cepheus_desc),
                            isSelected = uiState.appBackdropMode == AppBackdropMode.CEPHEUS,
                            onClick = { viewModel.setAppBackdropMode(AppBackdropMode.CEPHEUS) },
                            modifier = Modifier.testTag("settings_backdrop_cepheus")
                        )
                    }

                    // 6. Bortle Light Pollution Scale
                    CyberExpandableSection(
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
                                text = if (isFa) "کلاس انتخابی بورتل" else "BORTLE DARKNESS CLASS",
                                style = RedTypographyTokens.caption.copy(fontWeight = FontWeight.Light),
                                color = palette.textSecondary
                            )
                            CyberMatrixText(
                                text = if (isFa) {
                                    TimeEngine.formatPersianNumbers(uiState.bortleClass.toString())
                                } else {
                                    "0${uiState.bortleClass}"
                                },
                                dotSize = 2.2.dp,
                                dotGap = 0.85.dp,
                                charGap = 2.4.dp,
                                showMatrixGrid = true,
                                activeColor = palette.matrixLit,
                                glowColor = palette.primaryAccent
                            )
                        }

                        CyberGlassSlider(
                            value = uiState.bortleClass.toFloat(),
                            onValueChange = { viewModel.setBortleClass(it.toInt().coerceIn(1, 9)) },
                            valueRange = 1f..9f,
                            steps = 7,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bortle_class_slider")
                        )
                    }

                    // 7. Developer Credit Card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(squircleShape(26.dp))
                            .background(palette.glassFill)
                            .border(1.dp, palette.glassBorder, squircleShape(26.dp))
                            .padding(14.dp)
                            .testTag("developer_credit_card"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(palette.primaryAccent.copy(alpha = 0.14f))
                                .border(1.dp, palette.glassBorder, CircleShape)
                        ) {
                            SafeAppLogo(modifier = Modifier.size(28.dp), cornerRadius = 8.dp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isFa) "توسعه‌دهنده: علی جعفری" else "Developed by Ali Jafari",
                                style = RedTypographyTokens.bodySecondary.copy(fontWeight = FontWeight.Light),
                                color = palette.textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            CyberFloatingCapsuleBar(
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                elevation = 0.dp,
                                isActive = true,
                                onClick = {
                                    try {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/EdisonWasAThief"))
                                        )
                                    } catch (_: Exception) {
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = null,
                                    tint = palette.primaryAccent,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "@EdisonWasAThief",
                                    style = RedTypographyTokens.caption.copy(fontWeight = FontWeight.Medium),
                                    color = palette.textPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(thickness = 0.6.dp, color = palette.glassBorder.copy(alpha = 0.45f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    CyberMeshPillCard(
                        preset = CyberMeshPreset.CYAN_VIOLET,
                        isCapsule = true,
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
                        onClick = onDismiss
                    ) {
                        Text(
                            text = stringResource(R.string.ok),
                            style = RedTypographyTokens.bodySecondary.copy(fontWeight = FontWeight.Medium),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CyberExpandableSection(
    title: String,
    valueLabel: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    testTag: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = CyberTheme.palette
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(180),
        label = "CyberSectionChevron"
    )
    val shape = squircleShape(26.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(palette.glassFill)
            .border(
                BorderStroke(
                    1.dp,
                    if (isExpanded) palette.primaryAccent.copy(alpha = 0.45f) else palette.glassBorder
                ),
                shape
            )
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isExpanded) palette.primaryAccent else palette.textSecondary,
                    modifier = Modifier.size(19.dp)
                )
                Text(
                    text = title,
                    style = RedTypographyTokens.bodyPrimary.copy(
                        fontWeight = FontWeight.Light,
                        fontSize = 14.sp
                    ),
                    color = palette.textPrimary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RedShapes.capsule)
                        .background(palette.primaryAccent.copy(alpha = 0.14f))
                        .border(0.6.dp, palette.primaryAccent.copy(alpha = 0.35f), RedShapes.capsule)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = valueLabel,
                        style = RedTypographyTokens.caption.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        ),
                        color = palette.primaryAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = palette.textSecondary,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(rotation)
                )
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(tween(180)) + fadeIn(tween(180)),
            exit = shrinkVertically(tween(140)) + fadeOut(tween(140))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = if (palette.isDark) 0.18f else 0.03f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content
            )
        }
    }
}

@Composable
private fun CyberOptionRow(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    val palette = CyberTheme.palette
    val shape = squircleShape(20.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (isSelected) palette.primaryAccent.copy(alpha = 0.16f) else Color.Transparent
            )
            .border(
                BorderStroke(
                    0.8.dp,
                    if (isSelected) palette.primaryAccent.copy(alpha = 0.48f)
                    else palette.glassBorder.copy(alpha = 0.4f)
                ),
                shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isSelected) {
                    CyberSignalDot(size = 5.5.dp, glowing = false)
                }
                Text(
                    text = title,
                    style = RedTypographyTokens.bodySecondary.copy(
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Light
                    ),
                    color = if (isSelected) palette.textPrimary else palette.textSecondary
                )
            }
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = RedTypographyTokens.caption.copy(
                        fontWeight = FontWeight.Light,
                        fontSize = 11.sp
                    ),
                    color = palette.textMuted
                )
            }
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = palette.primaryAccent,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

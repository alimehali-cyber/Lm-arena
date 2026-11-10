package com.alijafari.red.astronomy.ui.quartz

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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

/**
 * "OBSIDIAN QUARTZ & CRIMSON TELEMETRY" — settings.
 *
 * The same seven expandable sections as every other style, drawn as smoked-quartz instrument panels:
 * a dotted inner track around each row, a dot-matrix value in each header, a hairline chevron made of
 * two strokes, and a Bortle readout in LED dots. The interface-style section lists all four styles —
 * including this one — so the selector the user sees here is the whole set.
 */
@Composable
fun QuartzSettingsModal(
    uiState: MainUiState,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val palette = QuartzTheme.palette
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
        AppUiStyle.QUARTZ -> stringResource(R.string.ui_style_quartz)
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
        AppBackdropMode.NONE -> if (isFa) "خاموش (کاغذ ادیتوریال)" else "Off (Editorial Paper)"
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 660.dp)
                    .clip(RoundedCornerShape(34.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                palette.quartzTop.copy(alpha = 0.99f),
                                Color(0xFF10151A).copy(alpha = 0.99f),
                                Color(0xFF0B0F13).copy(alpha = 0.99f)
                            )
                        )
                    )
                    .opticalQuartzSurface(
                        cornerRadius = 34.dp,
                        refractionColor = palette.quartzRefraction,
                        refractionReach = 18.dp,
                        refractionAlpha = 0.22f,
                        rimHighlightAlpha = 0.42f,
                        grainAlpha = palette.grainAlpha
                    )
                    .quartzInnerDottedTrack(
                        cornerRadius = 34.dp,
                        color = Color.White.copy(alpha = 0.14f),
                        inset = 8.dp
                    )
                    .padding(20.dp)
                    .testTag("settings_dialog")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header: title, system line, LED count of the sections.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            QuartzGlyphDisc(
                                glyph = QuartzGlyph.Crosshair,
                                color = Color.White,
                                diameter = 34.dp,
                                glyphSize = 13.dp
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.app_settings),
                                    color = Color.White.copy(alpha = 0.96f),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Light,
                                    letterSpacing = (-0.2).sp
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = if (isFa) "کوارتز سیاه // سیستم" else "OBSIDIAN QUARTZ // SYSTEM",
                                    color = Color.White.copy(alpha = 0.40f),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Normal,
                                    letterSpacing = 1.8.sp
                                )
                            }
                        }
                        QuartzDotMatrix(
                            value = "07",
                            dotSize = 1.9.dp,
                            dotGap = 0.95.dp,
                            charGap = 3.2.dp,
                            color = Color.White.copy(alpha = 0.8f),
                            glow = false
                        )
                    }

                    Spacer(Modifier.height(14.dp))
                    QuartzHairline(
                        color = Color.White.copy(alpha = 0.16f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                    )
                    Spacer(Modifier.height(12.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        // 0. Interface style — all four, including Quartz.
                        QuartzExpandableSection(
                            title = stringResource(R.string.ui_style_title),
                            valueLabel = uiStyleLabel,
                            index = "00",
                            isExpanded = isUiStyleExpanded,
                            onToggle = { isUiStyleExpanded = !isUiStyleExpanded },
                            testTag = "settings_section_ui_style"
                        ) {
                            QuartzOptionRow(
                                title = stringResource(R.string.ui_style_default),
                                subtitle = stringResource(R.string.ui_style_default_desc),
                                isSelected = uiState.uiStyle == AppUiStyle.DEFAULT,
                                onClick = { viewModel.setUiStyle(AppUiStyle.DEFAULT) },
                                modifier = Modifier.testTag("settings_ui_style_default")
                            )
                            QuartzOptionRow(
                                title = stringResource(R.string.ui_style_spatial),
                                subtitle = stringResource(R.string.ui_style_spatial_desc),
                                isSelected = uiState.uiStyle == AppUiStyle.SPATIAL,
                                onClick = { viewModel.setUiStyle(AppUiStyle.SPATIAL) },
                                modifier = Modifier.testTag("settings_ui_style_spatial")
                            )
                            QuartzOptionRow(
                                title = stringResource(R.string.ui_style_cyber),
                                subtitle = stringResource(R.string.ui_style_cyber_desc),
                                isSelected = uiState.uiStyle == AppUiStyle.CYBER,
                                onClick = { viewModel.setUiStyle(AppUiStyle.CYBER) },
                                modifier = Modifier.testTag("settings_ui_style_cyber")
                            )
                            QuartzOptionRow(
                                title = stringResource(R.string.ui_style_quartz),
                                subtitle = stringResource(R.string.ui_style_quartz_desc),
                                isSelected = uiState.uiStyle == AppUiStyle.QUARTZ,
                                onClick = { viewModel.setUiStyle(AppUiStyle.QUARTZ) },
                                modifier = Modifier.testTag("settings_ui_style_quartz")
                            )
                        }

                        // 1. Language
                        QuartzExpandableSection(
                            title = stringResource(R.string.application_language),
                            valueLabel = languageLabel,
                            index = "01",
                            isExpanded = isLanguageExpanded,
                            onToggle = { isLanguageExpanded = !isLanguageExpanded },
                            testTag = "settings_section_language"
                        ) {
                            QuartzOptionRow(
                                title = stringResource(R.string.language_persian),
                                subtitle = "فارسی (Persian)",
                                isSelected = uiState.language == AppLanguage.PERSIAN,
                                onClick = { viewModel.setLanguage(AppLanguage.PERSIAN) }
                            )
                            QuartzOptionRow(
                                title = stringResource(R.string.language_english),
                                subtitle = "English (US)",
                                isSelected = uiState.language == AppLanguage.ENGLISH,
                                onClick = { viewModel.setLanguage(AppLanguage.ENGLISH) }
                            )
                        }

                        // 2. Calendar
                        QuartzExpandableSection(
                            title = stringResource(R.string.calendar_system),
                            valueLabel = calendarLabel,
                            index = "02",
                            isExpanded = isCalendarExpanded,
                            onToggle = { isCalendarExpanded = !isCalendarExpanded },
                            testTag = "settings_section_calendar"
                        ) {
                            QuartzOptionRow(
                                title = stringResource(R.string.calendar_solar_hijri),
                                subtitle = if (isFa) "تقویم خورشیدی رسمی ایران" else "Solar Hijri Calendar",
                                isSelected = uiState.calendarSystem == CalendarSystem.SOLAR_HIJRI,
                                onClick = { viewModel.setCalendarSystem(CalendarSystem.SOLAR_HIJRI) }
                            )
                            QuartzOptionRow(
                                title = stringResource(R.string.calendar_gregorian),
                                subtitle = if (isFa) "تقویم بین‌المللی میلادی" else "Gregorian International Calendar",
                                isSelected = uiState.calendarSystem == CalendarSystem.GREGORIAN,
                                onClick = { viewModel.setCalendarSystem(CalendarSystem.GREGORIAN) }
                            )
                        }

                        // 3. App theme
                        QuartzExpandableSection(
                            title = if (isFa) "پوسته برنامه" else "App Theme",
                            valueLabel = appThemeLabel,
                            index = "03",
                            isExpanded = isAppThemeExpanded,
                            onToggle = { isAppThemeExpanded = !isAppThemeExpanded },
                            testTag = "settings_section_app_theme"
                        ) {
                            QuartzOptionRow(
                                title = if (isFa) "روشن (کاغذ آلباستر)" else "Light (Alabaster)",
                                isSelected = uiState.themeMode == ThemeMode.LIGHT,
                                onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) }
                            )
                            QuartzOptionRow(
                                title = if (isFa) "تاریک (اُبسیدیَن)" else "Dark (Obsidian)",
                                isSelected = uiState.themeMode == ThemeMode.DARK,
                                onClick = { viewModel.setThemeMode(ThemeMode.DARK) }
                            )
                            QuartzOptionRow(
                                title = stringResource(R.string.theme_wine),
                                subtitle = stringResource(R.string.theme_wine_desc),
                                isSelected = uiState.themeMode == ThemeMode.WINE,
                                onClick = { viewModel.setThemeMode(ThemeMode.WINE) },
                                modifier = Modifier.testTag("settings_theme_wine")
                            )
                        }

                        // 4. Sky canvas theme
                        QuartzExpandableSection(
                            title = if (isFa) "سبک هنری آسمان" else "Sky Canvas Theme",
                            valueLabel = skyCanvasThemeLabel,
                            index = "04",
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
                                QuartzOptionRow(
                                    title = label,
                                    isSelected = uiState.skyCanvasTheme == theme,
                                    onClick = { viewModel.setSkyCanvasTheme(theme) }
                                )
                            }
                        }

                        // 5. Backdrop
                        QuartzExpandableSection(
                            title = stringResource(R.string.app_backdrop_title),
                            valueLabel = backdropLabel,
                            index = "05",
                            isExpanded = isAppBackdropExpanded,
                            onToggle = { isAppBackdropExpanded = !isAppBackdropExpanded },
                            testTag = "settings_section_app_backdrop"
                        ) {
                            QuartzOptionRow(
                                title = if (isFa) "خاموش (کاغذ ادیتوریال)" else "Off (Editorial Paper)",
                                subtitle = stringResource(R.string.backdrop_off_desc),
                                isSelected = uiState.appBackdropMode == AppBackdropMode.NONE,
                                onClick = { viewModel.setAppBackdropMode(AppBackdropMode.NONE) },
                                modifier = Modifier.testTag("settings_backdrop_off")
                            )
                            QuartzOptionRow(
                                title = stringResource(R.string.live_sky_setting),
                                subtitle = stringResource(R.string.live_sky_desc),
                                isSelected = uiState.appBackdropMode == AppBackdropMode.LIVE_SKY,
                                onClick = { viewModel.setAppBackdropMode(AppBackdropMode.LIVE_SKY) },
                                modifier = Modifier.testTag("settings_live_sky_switch")
                            )
                            QuartzOptionRow(
                                title = stringResource(R.string.backdrop_ngc1929_setting),
                                subtitle = stringResource(R.string.backdrop_ngc1929_desc),
                                isSelected = uiState.appBackdropMode == AppBackdropMode.NGC_1929,
                                onClick = { viewModel.setAppBackdropMode(AppBackdropMode.NGC_1929) }
                            )
                            QuartzOptionRow(
                                title = stringResource(R.string.backdrop_cepheus_setting),
                                subtitle = stringResource(R.string.backdrop_cepheus_desc),
                                isSelected = uiState.appBackdropMode == AppBackdropMode.CEPHEUS,
                                onClick = { viewModel.setAppBackdropMode(AppBackdropMode.CEPHEUS) }
                            )
                        }

                        // 6. Bortle scale — a hairline stepper instead of a Material slider.
                        QuartzExpandableSection(
                            title = if (isFa) "آلودگی نوری (بورتل)" else "Light Pollution (Bortle)",
                            valueLabel = bortleLabel,
                            index = "06",
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
                                    text = if (isFa) "کلاس تاریکی" else "BORTLE DARKNESS CLASS",
                                    color = Color.White.copy(alpha = 0.52f),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Light,
                                    letterSpacing = 1.4.sp
                                )
                                QuartzDotMatrix(
                                    value = "0${uiState.bortleClass}",
                                    dotSize = 2.4.dp,
                                    dotGap = 1.1.dp,
                                    charGap = 3.6.dp,
                                    color = Color.White,
                                    glow = true,
                                    ghost = 0.10f
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("bortle_class_slider"),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (step in 1..9) {
                                    val active = step <= uiState.bortleClass
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(26.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(
                                                if (active) Color.White.copy(alpha = 0.14f) else Color.Transparent
                                            )
                                            .then(
                                                Modifier.drawWithCache {
                                                    val stroke = 0.8.dp.toPx()
                                                    onDrawBehind {
                                                        drawRoundRect(
                                                            color = Color.White.copy(alpha = if (active) 0.42f else 0.16f),
                                                            cornerRadius = CornerRadius(size.height / 2f, size.height / 2f),
                                                            style = Stroke(width = stroke)
                                                        )
                                                    }
                                                }
                                            )
                                            .quartzTapTarget(onClick = { viewModel.setBortleClass(step) }),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (isFa) TimeEngine.formatPersianNumbers(step.toString()) else step.toString(),
                                            color = Color.White.copy(alpha = if (active) 0.92f else 0.34f),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Light
                                        )
                                    }
                                }
                            }
                        }

                        // 7. Credits
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            QuartzGlyphDisc(
                                glyph = QuartzGlyph.Plus,
                                color = Color.White.copy(alpha = 0.7f),
                                diameter = 38.dp,
                                glyphSize = 11.dp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isFa) "توسعه‌دهنده: علی جعفری" else "Developed by Ali Jafari",
                                    color = Color.White.copy(alpha = 0.82f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Light
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "@EdisonWasAThief",
                                    color = Color.White.copy(alpha = 0.42f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Light,
                                    letterSpacing = 0.6.sp,
                                    modifier = Modifier.quartzTapTarget(
                                        onClick = {
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
                                    )
                                )
                            }
                            Text(
                                text = "ZIG",
                                color = Color.White.copy(alpha = 0.30f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 3.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    QuartzHairline(
                        color = Color.White.copy(alpha = 0.16f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.10f))
                                .quartzInnerDottedTrack(
                                    cornerRadius = 50.dp,
                                    color = Color.White.copy(alpha = 0.26f),
                                    inset = 4.dp
                                )
                                .quartzTapTarget(onClick = onDismiss)
                                .padding(horizontal = 26.dp, vertical = 11.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.ok),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.6.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuartzExpandableSection(
    title: String,
    valueLabel: String,
    index: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    testTag: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(200),
        label = "quartz_section_chevron"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                if (isExpanded) Color.White.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.035f)
            )
            .quartzInnerDottedTrack(
                cornerRadius = 22.dp,
                color = Color.White.copy(alpha = if (isExpanded) 0.28f else 0.14f),
                inset = 5.dp
            )
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .quartzTapTarget(onClick = onToggle)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = index,
                    color = Color.White.copy(alpha = 0.34f),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 1.2.sp
                )
                Box(
                    modifier = Modifier
                        .size(width = 1.dp, height = 14.dp)
                        .background(Color.White.copy(alpha = 0.14f))
                )
                Text(
                    text = title,
                    color = Color.White.copy(alpha = 0.94f),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Light,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = valueLabel,
                    color = Color.White.copy(alpha = 0.58f),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                QuartzVectorGlyph(
                    glyph = QuartzGlyph.ChevronRight,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(9.dp, 13.dp)
                        .rotate(rotation * 0.5f),
                    strokeWidth = 1.1.dp
                )
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(tween(200)) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(150)) + fadeOut(tween(150))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.22f))
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
                content = content
            )
        }
    }
}

@Composable
private fun QuartzOptionRow(
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
                if (isSelected) Color.White.copy(alpha = 0.10f) else Color.Transparent
            )
            .then(
                Modifier.drawWithCache {
                    val stroke = 0.75.dp.toPx()
                    onDrawBehind {
                        drawRoundRect(
                            color = Color.White.copy(alpha = if (isSelected) 0.44f else 0.14f),
                            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                            style = Stroke(width = stroke)
                        )
                    }
                }
            )
            .quartzTapTarget(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                if (isSelected) {
                    QuartzSignalDot(color = Color.White, size = 5.dp, glowing = false)
                }
                Text(
                    text = title,
                    color = Color.White.copy(alpha = if (isSelected) 0.96f else 0.66f),
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Light
                )
            }
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.38f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Light,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (isSelected) {
            // A drawn tick: two strokes, no check icon.
            Canvas(modifier = Modifier.size(14.dp)) {
                drawLine(
                    Color.White.copy(alpha = 0.9f),
                    Offset(size.width * 0.14f, size.height * 0.54f),
                    Offset(size.width * 0.42f, size.height * 0.82f),
                    strokeWidth = 1.3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    Color.White.copy(alpha = 0.9f),
                    Offset(size.width * 0.42f, size.height * 0.82f),
                    Offset(size.width * 0.88f, size.height * 0.20f),
                    strokeWidth = 1.3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

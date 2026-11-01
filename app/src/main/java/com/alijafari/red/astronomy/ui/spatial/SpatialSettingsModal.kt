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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Language
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
import com.alijafari.red.astronomy.ui.theme.RedShapes
import com.alijafari.red.astronomy.ui.theme.RedTypographyTokens
import com.alijafari.red.astronomy.ui.theme.squircleShape

/**
 * VisionOS Spatial Control Center Popover for Application Settings.
 *
 * Features a prominent Default-vs-Spatial UI Bento switcher, tactile glowing toggle tiles,
 * fluid spring-expanding option drawers, and a dot-matrix Bortle sky darkness controller.
 */
@Composable
fun SpatialSettingsModal(
    uiState: MainUiState,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val palette = SpatialTheme.palette
    val isFa = uiState.language == AppLanguage.PERSIAN

    var isSkyThemeExpanded by remember { mutableStateOf(false) }
    var isBackdropExpanded by remember { mutableStateOf(false) }
    var isBortleExpanded by remember { mutableStateOf(false) }

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
            SpatialGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 640.dp),
                cornerRadius = 36.dp,
                contentPadding = PaddingValues(20.dp),
                elevation = 24.dp,
                elevated = true,
                glowAccent = SpatialNeonRose
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.app_settings),
                            style = RedTypographyTokens.sectionHeading.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = palette.textPrimary
                        )
                        Text(
                            text = if (isFa) "مرکز کنترل فضایی زیگ" else "ZIG Spatial Control Center",
                            style = RedTypographyTokens.caption.copy(fontSize = 12.sp),
                            color = palette.textSecondary
                        )
                    }
                    SpatialGlowIconButton(
                        icon = Icons.Default.Close,
                        contentDescription = stringResource(R.string.ok),
                        onClick = onDismiss,
                        size = 36.dp,
                        iconSize = 17.dp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. UI Style Bento Switcher (Default UI vs Spatial UI)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_section_ui_style"),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Widgets,
                                contentDescription = null,
                                tint = palette.accentInk,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(R.string.ui_style_title),
                                style = RedTypographyTokens.caption.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.5.sp
                                ),
                                color = palette.textSecondary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Default UI Option Card
                            SpatialGlassCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_ui_style_default"),
                                cornerRadius = 24.dp,
                                contentPadding = PaddingValues(14.dp),
                                elevation = 6.dp,
                                onClick = { viewModel.setUiStyle(AppUiStyle.DEFAULT) }
                            ) {
                                Text(
                                    text = stringResource(R.string.ui_style_default),
                                    style = RedTypographyTokens.bodyPrimary.copy(fontSize = 14.sp),
                                    color = palette.textPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.ui_style_default_desc),
                                    style = RedTypographyTokens.caption.copy(fontSize = 11.sp),
                                    color = palette.textTertiary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Spatial UI Option Mesh Pill (Active!)
                            SpatialMeshPill(
                                preset = SpatialMeshPreset.ROSE_TO_ORANGE,
                                cornerRadius = 24.dp,
                                contentPadding = PaddingValues(14.dp),
                                onClick = { viewModel.setUiStyle(AppUiStyle.SPATIAL) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_ui_style_spatial")
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.ui_style_spatial),
                                            style = RedTypographyTokens.bodyPrimary.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            ),
                                            color = Color(0xFF23111C)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.85f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = SpatialNeonRoseDeep,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(R.string.ui_style_spatial_desc),
                                        style = RedTypographyTokens.caption.copy(fontSize = 11.sp),
                                        color = Color(0xCC23111C),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // 2. Quick Bento Toggle Grid (Dark Dreamscape + Language + Calendar)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Dark / Desert Light Theme Toggle Card
                        SpatialGlassCard(
                            modifier = Modifier.weight(1f),
                            cornerRadius = 26.dp,
                            contentPadding = PaddingValues(14.dp),
                            elevation = 8.dp,
                            glowAccent = if (uiState.themeMode == ThemeMode.DARK) SpatialCosmicIris else SpatialWarmPeach,
                            onClick = {
                                viewModel.setThemeMode(
                                    if (uiState.themeMode == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK
                                )
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DarkMode,
                                    contentDescription = null,
                                    tint = palette.accentInk,
                                    modifier = Modifier.size(19.dp)
                                )
                                SpatialGlowToggle(
                                    checked = uiState.themeMode == ThemeMode.DARK,
                                    onCheckedChange = { dark ->
                                        viewModel.setThemeMode(if (dark) ThemeMode.DARK else ThemeMode.LIGHT)
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (isFa) "پوسته فضایی" else "Atmosphere",
                                style = RedTypographyTokens.caption.copy(fontSize = 11.sp),
                                color = palette.textTertiary
                            )
                            Text(
                                text = if (uiState.themeMode == ThemeMode.DARK) {
                                    if (isFa) "گرگ‌ومیش تاریک" else "Twilight Dark"
                                } else {
                                    if (isFa) "کویر روشن" else "Desert Light"
                                },
                                style = RedTypographyTokens.bodyPrimary.copy(fontSize = 13.5.sp),
                                color = palette.textPrimary
                            )
                        }

                        // Language Switcher Bento Tile
                        SpatialGlassCard(
                            modifier = Modifier.weight(1f),
                            cornerRadius = 26.dp,
                            contentPadding = PaddingValues(14.dp),
                            elevation = 8.dp,
                            onClick = {
                                viewModel.setLanguage(
                                    if (uiState.language == AppLanguage.PERSIAN) AppLanguage.ENGLISH
                                    else AppLanguage.PERSIAN
                                )
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = palette.coolInk,
                                    modifier = Modifier.size(19.dp)
                                )
                                SpatialGlassCapsule(
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp),
                                    isActive = true,
                                    elevation = 0.dp
                                ) {
                                    Text(
                                        text = if (isFa) "FA" else "EN",
                                        style = RedTypographyTokens.badge.copy(fontSize = 10.sp),
                                        color = palette.accentInk
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = stringResource(R.string.application_language),
                                style = RedTypographyTokens.caption.copy(fontSize = 11.sp),
                                color = palette.textTertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isFa) stringResource(R.string.language_persian)
                                else stringResource(R.string.language_english),
                                style = RedTypographyTokens.bodyPrimary.copy(fontSize = 13.5.sp),
                                color = palette.textPrimary
                            )
                        }
                    }

                    // 3. Calendar Quick-Switch Pill Row
                    SpatialGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 24.dp,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        elevation = 6.dp,
                        onClick = {
                            viewModel.setCalendarSystem(
                                if (uiState.calendarSystem == CalendarSystem.SOLAR_HIJRI) CalendarSystem.GREGORIAN
                                else CalendarSystem.SOLAR_HIJRI
                            )
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = palette.secondaryInk,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = stringResource(R.string.calendar_system),
                                        style = RedTypographyTokens.bodyPrimary.copy(fontSize = 13.5.sp),
                                        color = palette.textPrimary
                                    )
                                    Text(
                                        text = if (uiState.calendarSystem == CalendarSystem.SOLAR_HIJRI) {
                                            stringResource(R.string.calendar_solar_hijri)
                                        } else {
                                            stringResource(R.string.calendar_gregorian)
                                        },
                                        style = RedTypographyTokens.caption.copy(fontSize = 11.5.sp),
                                        color = palette.textSecondary
                                    )
                                }
                            }
                            SpatialGlowToggle(
                                checked = uiState.calendarSystem == CalendarSystem.SOLAR_HIJRI,
                                onCheckedChange = { hijri ->
                                    viewModel.setCalendarSystem(
                                        if (hijri) CalendarSystem.SOLAR_HIJRI else CalendarSystem.GREGORIAN
                                    )
                                },
                                glowColor = SpatialWarmPeach
                            )
                        }
                    }

                    // 4. Expandable Sky Canvas Theme Drawer
                    val currentSkyLabel = when (uiState.skyCanvasTheme) {
                        SkyCanvasTheme.REAL_SKY -> if (isFa) "آسمان واقعی" else "Real Sky"
                        SkyCanvasTheme.ATMOSPHERIC_SKY -> if (isFa) "آسمان جوی" else "Atmospheric"
                        SkyCanvasTheme.MONOCHROME_SCIENTIFIC -> if (isFa) "تک‌رنگ علمی" else "Monochrome"
                        SkyCanvasTheme.KIDS_WATERCOLOR -> if (isFa) "آبرنگ" else "Watercolor"
                        SkyCanvasTheme.OBSERVATORY -> if (isFa) "رصدخانه" else "Observatory"
                        SkyCanvasTheme.PAPERCRAFT_DIORAMA -> if (isFa) "دیوراما" else "Papercraft"
                    }
                    SpatialExpandableDrawer(
                        icon = Icons.Default.AutoAwesome,
                        title = if (isFa) "سبک هنری آسمان (RMAE)" else "Sky Canvas Theme",
                        valueLabel = currentSkyLabel,
                        expanded = isSkyThemeExpanded,
                        onToggle = { isSkyThemeExpanded = !isSkyThemeExpanded }
                    ) {
                        val skyOptions = listOf(
                            SkyCanvasTheme.REAL_SKY to (if (isFa) "آسمان واقعی (Real Sky)" else "Real Sky"),
                            SkyCanvasTheme.ATMOSPHERIC_SKY to (if (isFa) "آسمان جوی (Atmospheric)" else "Atmospheric Sky"),
                            SkyCanvasTheme.MONOCHROME_SCIENTIFIC to (if (isFa) "تک‌رنگ علمی (Monochrome)" else "Monochrome Scientific"),
                            SkyCanvasTheme.KIDS_WATERCOLOR to (if (isFa) "آبرنگ کودکانه (Watercolor)" else "Kids Watercolor"),
                            SkyCanvasTheme.OBSERVATORY to (if (isFa) "رصدخانه (Observatory)" else "Observatory"),
                            SkyCanvasTheme.PAPERCRAFT_DIORAMA to (if (isFa) "دیوراما کاغذ برجسته" else "Papercraft Diorama")
                        )
                        skyOptions.forEach { (mode, title) ->
                            SpatialChoicePill(
                                label = title,
                                selected = uiState.skyCanvasTheme == mode,
                                onClick = { viewModel.setSkyCanvasTheme(mode) }
                            )
                        }
                    }

                    // 5. Expandable App Backdrop Drawer
                    val currentBackdropLabel = when (uiState.appBackdropMode) {
                        AppBackdropMode.NONE -> if (isFa) "کویر فضایی" else "Desert Dreamscape"
                        AppBackdropMode.LIVE_SKY -> stringResource(R.string.live_sky_setting)
                        AppBackdropMode.NGC_1929 -> stringResource(R.string.backdrop_ngc1929_setting)
                        AppBackdropMode.CEPHEUS -> stringResource(R.string.backdrop_cepheus_setting)
                    }
                    SpatialExpandableDrawer(
                        icon = Icons.Outlined.NightsStay,
                        title = stringResource(R.string.app_backdrop_title),
                        valueLabel = currentBackdropLabel,
                        expanded = isBackdropExpanded,
                        onToggle = { isBackdropExpanded = !isBackdropExpanded }
                    ) {
                        val backdropOptions = listOf(
                            AppBackdropMode.NONE to (if (isFa) "کویر رویایی (پیش‌فرض فضایی)" else "Desert Dreamscape (Spatial Default)"),
                            AppBackdropMode.LIVE_SKY to stringResource(R.string.live_sky_setting),
                            AppBackdropMode.NGC_1929 to stringResource(R.string.backdrop_ngc1929_setting),
                            AppBackdropMode.CEPHEUS to stringResource(R.string.backdrop_cepheus_setting)
                        )
                        backdropOptions.forEach { (mode, title) ->
                            SpatialChoicePill(
                                label = title,
                                selected = uiState.appBackdropMode == mode,
                                onClick = { viewModel.setAppBackdropMode(mode) }
                            )
                        }
                    }

                    // 6. Expandable Bortle Light Pollution Dot-Matrix Slider
                    val bortleNumStr = if (isFa) TimeEngine.formatPersianNumbers("B${uiState.bortleClass}") else "B${uiState.bortleClass}"
                    SpatialExpandableDrawer(
                        icon = Icons.Default.Brightness6,
                        title = if (isFa) "مقیاس تاریکی آسمان (بورتل)" else "Sky Darkness (Bortle)",
                        valueLabel = bortleNumStr,
                        expanded = isBortleExpanded,
                        onToggle = { isBortleExpanded = !isBortleExpanded }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isFa) "کلاس آلودگی نوری" else "Bortle Sky Class",
                                style = RedTypographyTokens.caption,
                                color = palette.textSecondary
                            )
                            DotMatrixText(
                                text = if (isFa) TimeEngine.formatPersianNumbers("${uiState.bortleClass}/9") else "${uiState.bortleClass}/9",
                                dotSize = 2.4.dp,
                                activeColor = palette.dotMatrixInk,
                                glowColor = SpatialNeonRose
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        SpatialGlowSlider(
                            value = uiState.bortleClass.toFloat(),
                            onValueChange = { viewModel.setBortleClass(it.toInt().coerceIn(1, 9)) },
                            valueRange = 1f..9f,
                            steps = 7,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpatialExpandableDrawer(
    icon: ImageVector,
    title: String,
    valueLabel: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScopeAdapter.() -> Unit
) {
    val palette = SpatialTheme.palette
    val chevronDeg by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 360f),
        label = "SpatialChevron"
    )

    SpatialGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
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
                        style = RedTypographyTokens.bodyPrimary.copy(fontSize = 13.5.sp),
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = valueLabel,
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
            visible = expanded,
            enter = expandVertically(spring(dampingRatio = 0.78f, stiffness = 340f)) + fadeIn(),
            exit = shrinkVertically(spring(dampingRatio = 0.85f, stiffness = 380f)) + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ColumnScopeAdapter.content()
            }
        }
    }
}

private object ColumnScopeAdapter

@Composable
private fun SpatialChoicePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val palette = SpatialTheme.palette
    val shape = squircleShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (selected) SpatialNeonRose.copy(alpha = if (palette.isDark) 0.24f else 0.18f)
                else Color.Transparent
            )
            .border(
                BorderStroke(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        if (selected) listOf(Color.White.copy(alpha = 0.70f), SpatialNeonRose.copy(alpha = 0.45f))
                        else listOf(palette.rimSideBorder.copy(alpha = 0.5f), Color.Transparent)
                    )
                ),
                shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = RedTypographyTokens.bodyPrimary.copy(
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = if (selected) palette.accentInk else palette.textPrimary
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RedShapes.capsule)
                    .background(palette.accentInk)
            )
        }
    }
}

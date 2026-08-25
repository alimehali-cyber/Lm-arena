package com.alijafari.red.astronomy.ui.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.R
import com.alijafari.red.astronomy.domain.AppLanguage
import com.alijafari.red.astronomy.domain.CalendarSystem
import com.alijafari.red.astronomy.domain.SkyCanvasTheme
import com.alijafari.red.astronomy.domain.ThemeMode
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.MainViewModel
import com.alijafari.red.astronomy.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    uiState: MainUiState,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isFa = uiState.language == AppLanguage.PERSIAN

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_dialog"),
        shape = RoundedCornerShape(RedCornerRadius.xl),
        containerColor = RedTheme.colors.surfaceElevated,
        title = {
            Text(
                text = stringResource(R.string.app_settings),
                style = RedTypographyTokens.sectionHeading,
                color = RedTheme.colors.textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(RedSpacing.lg)
            ) {
                // Language Selection Segmented Switch
                Column(verticalArrangement = Arrangement.spacedBy(RedSpacing.xs)) {
                    Text(
                        text = stringResource(R.string.application_language),
                        style = RedTypographyTokens.bodyPrimary.copy(fontWeight = FontWeight.SemiBold),
                        color = RedTheme.colors.textPrimary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(RedSpacing.sm)
                    ) {
                        FilterChip(
                            selected = uiState.language == AppLanguage.PERSIAN,
                            onClick = { viewModel.setLanguage(AppLanguage.PERSIAN) },
                            label = { Text(text = stringResource(R.string.language_persian)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RedTheme.colors.accentRed,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = uiState.language == AppLanguage.ENGLISH,
                            onClick = { viewModel.setLanguage(AppLanguage.ENGLISH) },
                            label = { Text(text = stringResource(R.string.language_english)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RedTheme.colors.accentRed,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Calendar System Selection
                Column(verticalArrangement = Arrangement.spacedBy(RedSpacing.xs)) {
                    Text(
                        text = stringResource(R.string.calendar_system),
                        style = RedTypographyTokens.bodyPrimary.copy(fontWeight = FontWeight.SemiBold),
                        color = RedTheme.colors.textPrimary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(RedSpacing.sm)
                    ) {
                        FilterChip(
                            selected = uiState.calendarSystem == CalendarSystem.SOLAR_HIJRI,
                            onClick = { viewModel.setCalendarSystem(CalendarSystem.SOLAR_HIJRI) },
                            label = { Text(text = stringResource(R.string.calendar_solar_hijri)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RedTheme.colors.accentRed,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = uiState.calendarSystem == CalendarSystem.GREGORIAN,
                            onClick = { viewModel.setCalendarSystem(CalendarSystem.GREGORIAN) },
                            label = { Text(text = stringResource(R.string.calendar_gregorian)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RedTheme.colors.accentRed,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Theme Mode Selection
                Column(verticalArrangement = Arrangement.spacedBy(RedSpacing.xs)) {
                    Text(
                        text = if (isFa) "پوسته برنامه (App Theme):" else "App Theme Mode:",
                        style = RedTypographyTokens.bodyPrimary.copy(fontWeight = FontWeight.SemiBold),
                        color = RedTheme.colors.textPrimary
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(RedSpacing.sm)
                        ) {
                            FilterChip(
                                selected = uiState.themeMode == ThemeMode.DARK_NAVY,
                                onClick = { viewModel.setThemeMode(ThemeMode.DARK_NAVY) },
                                label = { Text(text = if (isFa) "🌌 سرمه‌ای" else "🌌 Deep Navy") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RedTheme.colors.accentRed,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = uiState.themeMode == ThemeMode.OLED_BLACK,
                                onClick = { viewModel.setThemeMode(ThemeMode.OLED_BLACK) },
                                label = { Text(text = if (isFa) "🖤 OLED" else "🖤 Pitch OLED") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RedTheme.colors.accentRed,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(RedSpacing.sm)
                        ) {
                            FilterChip(
                                selected = uiState.themeMode == ThemeMode.DYNAMIC_SILK,
                                onClick = { viewModel.setThemeMode(ThemeMode.DYNAMIC_SILK) },
                                label = { Text(text = if (isFa) "✨ ابریشم پویا (Silk)" else "✨ Dynamic Silk") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RedTheme.colors.accentRed,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = uiState.themeMode == ThemeMode.DYNAMIC_SKY,
                                onClick = { viewModel.setThemeMode(ThemeMode.DYNAMIC_SKY) },
                                label = { Text(text = if (isFa) "🌅 آسمان پویا" else "🌅 Dynamic Sky") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RedTheme.colors.accentRed,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(RedSpacing.sm)
                        ) {
                            FilterChip(
                                selected = uiState.themeMode == ThemeMode.LIGHT,
                                onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                                label = { Text(text = if (isFa) "☀️ روشن" else "☀️ Light") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RedTheme.colors.accentRed,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = uiState.themeMode == ThemeMode.PAPERCRAFT_PASTEL,
                                onClick = { viewModel.setThemeMode(ThemeMode.PAPERCRAFT_PASTEL) },
                                label = { Text(text = if (isFa) "📜 کاغذ دست‌ساز" else "📜 Papercraft") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RedTheme.colors.accentRed,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Sky Canvas Theme Section
                Column(verticalArrangement = Arrangement.spacedBy(RedSpacing.xs)) {
                    Text(
                        text = if (isFa) "سبک هنری آسمان (RMAE Theme):" else "Sky Canvas Theme:",
                        style = RedTypographyTokens.bodyPrimary.copy(fontWeight = FontWeight.SemiBold),
                        color = RedTheme.colors.textPrimary
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = uiState.skyCanvasTheme == SkyCanvasTheme.ATMOSPHERIC_SKY,
                                onClick = { viewModel.setSkyCanvasTheme(SkyCanvasTheme.ATMOSPHERIC_SKY) },
                                label = { Text(text = if (isFa) "🌅 آسمان جوی" else "Atmospheric") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RedTheme.colors.accentRed,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = uiState.skyCanvasTheme == SkyCanvasTheme.MONOCHROME_SCIENTIFIC,
                                onClick = { viewModel.setSkyCanvasTheme(SkyCanvasTheme.MONOCHROME_SCIENTIFIC) },
                                label = { Text(text = if (isFa) "🔳 تک‌رنگ" else "Monochrome") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RedTheme.colors.accentRed,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = uiState.skyCanvasTheme == SkyCanvasTheme.KIDS_WATERCOLOR,
                                onClick = { viewModel.setSkyCanvasTheme(SkyCanvasTheme.KIDS_WATERCOLOR) },
                                label = { Text(text = if (isFa) "🎨 آبرنگ" else "WaterColor") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RedTheme.colors.accentRed,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = uiState.skyCanvasTheme == SkyCanvasTheme.OBSERVATORY,
                                onClick = { viewModel.setSkyCanvasTheme(SkyCanvasTheme.OBSERVATORY) },
                                label = { Text(text = if (isFa) "🔴 رصدخانه" else "Observatory") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RedTheme.colors.accentRed,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = uiState.skyCanvasTheme == SkyCanvasTheme.PAPERCRAFT_DIORAMA,
                                onClick = { viewModel.setSkyCanvasTheme(SkyCanvasTheme.PAPERCRAFT_DIORAMA) },
                                label = { Text(text = if (isFa) "✂️ دیوراما کاغذ برجسته" else "✂️ Papercraft Diorama") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RedTheme.colors.accentRed,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Light Pollution / Bortle Class Slider
                Column(verticalArrangement = Arrangement.spacedBy(RedSpacing.xs)) {
                    val bortleText = stringResource(R.string.bortle_class_setting, uiState.bortleClass)
                    Text(
                        text = bortleText,
                        style = RedTypographyTokens.bodyPrimary.copy(fontWeight = FontWeight.SemiBold),
                        color = RedTheme.colors.textPrimary
                    )
                    Slider(
                        value = uiState.bortleClass.toFloat(),
                        onValueChange = { viewModel.setBortleClass(it.toInt()) },
                        valueRange = 1f..9f,
                        steps = 7,
                        colors = SliderDefaults.colors(
                            thumbColor = RedTheme.colors.accentRed,
                            activeTrackColor = RedTheme.colors.accentRed
                        )
                    )
                }

                RedHairlineDivider()

                // Developer Credit Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(RedCornerRadius.lg))
                        .background(RedTheme.colors.surfaceGrouped)
                        .border(1.dp, RedTheme.colors.border, RoundedCornerShape(RedCornerRadius.lg))
                        .padding(RedSpacing.md)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(RedSpacing.md)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = RedTheme.colors.accentRed.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, RedTheme.colors.accentRed.copy(alpha = 0.3f)),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                SafeAppLogo(
                                    modifier = Modifier.size(34.dp),
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
                                text = "Ali Jafari • RED Astronomy Engine",
                                style = RedTypographyTokens.caption,
                                color = RedTheme.colors.textSecondary
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            // Telegram Glass Badge
                            val intentContext = context
                            Surface(
                                shape = RoundedCornerShape(RedCornerRadius.xs),
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
                                    Text(
                                        text = "✈️",
                                        fontSize = 11.sp
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
                colors = ButtonDefaults.buttonColors(containerColor = RedTheme.colors.accentRed)
            ) {
                Text(text = stringResource(R.string.ok), color = Color.White)
            }
        }
    )
}

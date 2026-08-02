package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.astro_engine.MoonEngine
import com.example.astro_engine.TimeEngine
import com.example.domain.AppLanguage
import com.example.ui.MainUiState
import com.example.ui.MainViewModel
import java.util.Calendar

@Composable
fun MoonScreen(
    uiState: MainUiState,
    viewModel: MainViewModel
) {
    val isFa = uiState.language == AppLanguage.PERSIAN
    val jd = remember { TimeEngine.getJulianDate() }
    val moonData = remember(jd) { MoonEngine.calculateMoon(jd) }

    val calendar = remember { Calendar.getInstance(TimeEngine.TEHRAN_TIME_ZONE) }
    val solarHijri = remember {
        TimeEngine.toSolarHijri(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("moon_screen"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Moon Hero Visualization Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("moon_hero_card"),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (isFa) "وضعیت و فازهای ماه" else "Lunar Phase Engine",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Moon Phase Graphic Canvas
                    Box(
                        modifier = Modifier.size(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val radius = size.width / 2f
                            val center = Offset(size.width / 2f, size.height / 2f)

                            // Dark background disc of moon
                            drawCircle(
                                color = Color(0xFF151520),
                                radius = radius,
                                center = center
                            )

                            // Illuminated crescent or gibbous fill
                            val illumFrac = (moonData.illuminationPercent / 100.0).toFloat()
                            drawCircle(
                                color = Color(0xFFFFB703),
                                radius = radius * illumFrac,
                                center = center
                            )

                            // Moon crater textures accent
                            drawCircle(
                                color = Color.White.copy(alpha = 0.2f),
                                radius = radius * 0.15f,
                                center = Offset(center.x - 30f, center.y - 20f)
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = 0.2f),
                                radius = radius * 0.22f,
                                center = Offset(center.x + 20f, center.y + 30f)
                            )

                            // Outer glow ring
                            drawCircle(
                                color = Color(0xFFFFB703).copy(alpha = 0.3f),
                                radius = radius + 6f,
                                center = center,
                                style = Stroke(width = 2f)
                            )
                        }
                    }

                    Text(
                        text = if (isFa) moonData.phaseNameFa else moonData.phaseNameEn,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Illumination Progress Bar
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val illumStr = String.format("%.1f%%", moonData.illuminationPercent)
                            Text(
                                text = if (isFa) "درصد روشنایی سطح" else "Illumination",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (isFa) TimeEngine.formatPersianNumbers(illumStr) else illumStr,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFFFB703)
                            )
                        }

                        LinearProgressIndicator(
                            progress = (moonData.illuminationPercent / 100.0).toFloat(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(CircleShape),
                            color = Color(0xFFFFB703),
                            trackColor = Color(0xFF252536)
                        )
                    }
                }
            }
        }

        // Scientific Lunar Metrics Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Age Metric
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isFa) "سن ماه" else "Moon Age",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val ageStr = String.format("%.1f", moonData.ageDays)
                        Text(
                            text = if (isFa) "${TimeEngine.formatPersianNumbers(ageStr)} روز" else "$ageStr days",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (isFa) "از دوره ۲۹.۵ روزه" else "of 29.5d synodic",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Distance Metric
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isFa) "فاصله از زمین" else "Distance",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val distStr = String.format("%,.0f", moonData.distanceKm)
                        Text(
                            text = if (isFa) "${TimeEngine.formatPersianNumbers(distStr)} کیلومتر" else "$distStr km",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (isFa) "مدار بیضوی ELP2000" else "ELP2000 Orbit",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Solar Hijri Calendar Context & Eclipse Dates
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Calendar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isFa) "تقویم خورشیدی و خسوف‌های پیش‌رو" else "Lunar Calendar & Eclipses",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    val dateFormatted = TimeEngine.formatDate(System.currentTimeMillis(), uiState.calendarSystem, isFa)
                    Text(
                        text = if (isFa) "تاریخ امروز: $dateFormatted" else "Today: $dateFormatted",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                    Text(
                        text = if (isFa) "خسوف (ماه گرفتگی) بعدی: ۱۴ اسفند ۱۴۰۵ (توتال - کامل)" else "Next Lunar Eclipse: Mar 3, 2026 (Total)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFFFFB703)
                    )
                }
            }
        }
    }
}

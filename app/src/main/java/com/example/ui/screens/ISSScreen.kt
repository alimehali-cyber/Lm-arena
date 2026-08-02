package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.astro_engine.ISSEngine
import com.example.astro_engine.TimeEngine
import com.example.domain.AppLanguage
import com.example.ui.MainUiState
import com.example.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ISSScreen(
    uiState: MainUiState,
    viewModel: MainViewModel
) {
    val isFa = uiState.language == AppLanguage.PERSIAN

    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    var tleData by remember { mutableStateOf(ISSEngine.TLEData()) }
    var isFetchingTle by remember { mutableStateOf(false) }
    var visiblePassesOnly by remember { mutableStateOf(true) }
    var expandedPassMs by remember { mutableStateOf<Long?>(null) }

    // Periodically update time every 5 seconds & fetch TLE on launch
    LaunchedEffect(Unit) {
        isFetchingTle = true
        tleData = ISSEngine.fetchLatestTLE()
        isFetchingTle = false
        while (true) {
            kotlinx.coroutines.delay(5000L)
            nowMs = System.currentTimeMillis()
        }
    }

    val topocentricPos = remember(nowMs, uiState.userLocation, tleData) {
        ISSEngine.calculateTopocentricPos(
            nowMs,
            uiState.userLocation.latitude,
            uiState.userLocation.longitude,
            uiState.userLocation.elevationMeters,
            tleData
        )
    }

    val passes = remember(uiState.userLocation, tleData, visiblePassesOnly) {
        ISSEngine.predictPasses(
            userLatDeg = uiState.userLocation.latitude,
            userLonDeg = uiState.userLocation.longitude,
            startTimestampMs = nowMs,
            tle = tleData,
            scanDays = 7,
            visibleOnly = visiblePassesOnly
        )
    }

    val dateFormatter = remember { SimpleDateFormat("EEE, MMM dd • HH:mm", Locale.getDefault()) }
    val timeOnlyFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("iss_screen"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ISS Orbit Live Hero Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("iss_hero_card"),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
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
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.SatelliteAlt,
                                        contentDescription = "ISS Satellite",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = if (isFa) "ردیابی زنده ایستگاه فضایی (ISS)" else "ISS Live Tracking Engine",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "NORAD 25544 • Real-time SGP4 Model",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        val statusColor = if (topocentricPos.isSunlit) Color(0xFF2DC653) else Color(0xFFFFB703)
                        val statusText = if (topocentricPos.isSunlit) {
                            if (isFa) "در معرض نور خورشید" else "Sunlit (In Light)"
                        } else {
                            if (isFa) "در سایه زمین" else "In Earth Shadow"
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = statusColor.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(statusColor)
                                )
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = statusColor
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                    // Live Sub-Satellite Coordinates & Telemetry
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (isFa) "عرض جغرافیایی" else "Latitude",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val latStr = String.format("%.2f°", topocentricPos.subLatDeg)
                            Text(
                                text = if (isFa) TimeEngine.formatPersianNumbers(latStr) else latStr,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Column {
                            Text(
                                text = if (isFa) "طول جغرافیایی" else "Longitude",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val lonStr = String.format("%.2f°", topocentricPos.subLonDeg)
                            Text(
                                text = if (isFa) TimeEngine.formatPersianNumbers(lonStr) else lonStr,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Column {
                            Text(
                                text = if (isFa) "ارتفاع مداری" else "Altitude",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val altStr = String.format("%.0f km", topocentricPos.satAltKm)
                            Text(
                                text = if (isFa) TimeEngine.formatPersianNumbers(altStr) else altStr,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Column {
                            Text(
                                text = if (isFa) "سرعت لحظه‌ای" else "Speed",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val speedStr = String.format("%.1f km/s", topocentricPos.velocityKmS)
                            Text(
                                text = if (isFa) TimeEngine.formatPersianNumbers(speedStr) else speedStr,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (isFa) "ارتفاع نسبت به افق" else "Elevation",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val elevStr = String.format("%.1f°", topocentricPos.elevationDeg)
                            Text(
                                text = if (isFa) TimeEngine.formatPersianNumbers(elevStr) else elevStr,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (topocentricPos.elevationDeg > 0) Color(0xFF2DC653) else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column {
                            Text(
                                text = if (isFa) "سمت (Azimuth)" else "Azimuth",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val azStr = String.format("%.1f° (%s)", topocentricPos.azimuthDeg, getAzimuthCardinal(topocentricPos.azimuthDeg, isFa))
                            Text(
                                text = if (isFa) TimeEngine.formatPersianNumbers(azStr) else azStr,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Column {
                            Text(
                                text = if (isFa) "فاصله مستقیم" else "Range",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val rangeStr = String.format("%.0f km", topocentricPos.rangeKm)
                            Text(
                                text = if (isFa) TimeEngine.formatPersianNumbers(rangeStr) else rangeStr,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }

        // Section Title & Pass Filter Toggle Tabs
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isFa) "پیش‌بینی گذرهای ۷ روز آینده" else "7-Day Flyover Predictions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isFetchingTle) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Filter Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = visiblePassesOnly,
                        onClick = { visiblePassesOnly = true },
                        label = { Text(text = if (isFa) "فقط گذرهای قابل رصد" else "Visible Passes Only") },
                        leadingIcon = if (visiblePassesOnly) {
                            { Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.testTag("iss_filter_visible")
                    )

                    FilterChip(
                        selected = !visiblePassesOnly,
                        onClick = { visiblePassesOnly = false },
                        label = { Text(text = if (isFa) "تمام گذرهای هندسی (شامل روز/سایه)" else "All Geometric Passes") },
                        leadingIcon = if (!visiblePassesOnly) {
                            { Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.testTag("iss_filter_all")
                    )
                }
            }
        }

        if (passes.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isFa) {
                                "هیچ گذر قابل رصدی با چشم غیرمسلح در ۷ روز آینده برای این موقعیت یافت نشد."
                            } else {
                                "No naked-eye visible flyover passes predicted in the next 7 days for this location."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(passes) { pass ->
                val isExpanded = expandedPassMs == pass.startTimeMs
                val badgeColor = Color(pass.classification.colorHex)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("iss_pass_item")
                        .clickable { expandedPassMs = if (isExpanded) null else pass.startTimeMs },
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = "Time",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                val dateStr = TimeEngine.formatDateTime24h(pass.startTimeMs, uiState.calendarSystem, isFa)
                                Text(
                                    text = dateStr,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = badgeColor.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = if (isFa) pass.classification.labelFa else pass.classification.labelEn,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = badgeColor
                                )
                            }
                        }

                        Text(
                            text = if (isFa) pass.summaryReasonFa else pass.summaryReasonEn,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        // Key Metrics Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = if (isFa) "اوج ارتفاع" else "Max Elev",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val elevStr = String.format("%.0f°", pass.maxElevationDeg)
                                Text(
                                    text = if (isFa) TimeEngine.formatPersianNumbers(elevStr) else elevStr,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = badgeColor
                                )
                            }

                            Column {
                                Text(
                                    text = if (isFa) "قدر روشنایی" else "Brightness",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val magStr = String.format("%.1f", pass.estimatedMagnitude)
                                Text(
                                    text = if (isFa) TimeEngine.formatPersianNumbers(magStr) else magStr,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            Column {
                                Text(
                                    text = if (isFa) "مدت زمان" else "Duration",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val durStr = "${pass.passDurationSec / 60}m ${pass.passDurationSec % 60}s"
                                Text(
                                    text = if (isFa) TimeEngine.formatPersianNumbers(durStr) else durStr,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            Column {
                                Text(
                                    text = if (isFa) "مسیر گذر" else "Trajectory",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val startDir = getAzimuthCardinal(pass.startAzimuthDeg, isFa)
                                val endDir = getAzimuthCardinal(pass.endAzimuthDeg, isFa)
                                Text(
                                    text = "$startDir ➔ $endDir",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        // Expandable Details
                        AnimatedVisibility(visible = isExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                                Text(
                                    text = if (isFa) "جزئیات تحلیل علمی گذر:" else "Scientific Pass Analysis:",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )

                                val reasons = if (isFa) pass.detailedReasonsFa else pass.detailedReasonsEn
                                reasons.forEach { reason ->
                                    Text(
                                        text = reason,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (pass.shadowEntryMs != null) {
                                    val entryStr = TimeEngine.formatTimeWithSeconds24h(pass.shadowEntryMs, isFa)
                                    Text(
                                        text = if (isFa) "🌑 ورود به سایه زمین: $entryStr" else "🌑 Earth Shadow Entry: $entryStr",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFFFB703)
                                    )
                                }

                                if (pass.shadowExitMs != null) {
                                    val exitStr = TimeEngine.formatTimeWithSeconds24h(pass.shadowExitMs, isFa)
                                    Text(
                                        text = if (isFa) "☀️ خروج از سایه زمین: $exitStr" else "☀️ Earth Shadow Exit: $exitStr",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF2DC653)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getAzimuthCardinal(azimuthDeg: Double, isFa: Boolean): String {
    val en = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    val fa = arrayOf("شمال", "شمال‌شرق", "شرق", "جنوب‌شرق", "جنوب", "جنوب‌غرب", "غرب", "شمال‌غرب")
    val idx = (((azimuthDeg + 22.5) % 360) / 45.0).toInt().coerceIn(0, 7)
    return if (isFa) fa[idx] else en[idx]
}

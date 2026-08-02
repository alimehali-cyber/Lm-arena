package com.example.ui.screens

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.astro_engine.ISSEngine
import com.example.astro_engine.TimeEngine
import com.example.domain.AppLanguage
import com.example.ui.MainUiState
import com.example.ui.MainViewModel
import com.example.util.toPersianDigits
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

sealed class IssLiveState {
    object Loading : IssLiveState()
    data class Success(
        val latitude: Double,
        val longitude: Double,
        val altitudeKm: Double,
        val velocityKmh: Double,
        val timestampMs: Long
    ) : IssLiveState()
    object Unavailable : IssLiveState()
}

sealed class IssLiveResult {
    data class Success(
        val lat: Double,
        val lon: Double,
        val altKm: Double,
        val velKmh: Double,
        val timestampMs: Long
    ) : IssLiveResult()
    data class Failure(val reason: String) : IssLiveResult()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ISSScreen(
    uiState: MainUiState,
    viewModel: MainViewModel
) {
    val isFa = uiState.language == AppLanguage.PERSIAN
    val context = LocalContext.current

    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    var tleData by remember { mutableStateOf(ISSEngine.TLEData()) }
    var isFetchingTle by remember { mutableStateOf(false) }
    var visiblePassesOnly by remember { mutableStateOf(true) }
    var expandedPassMs by remember { mutableStateOf<Long?>(null) }

    var issLiveState by remember { mutableStateOf<IssLiveState>(IssLiveState.Loading) }
    var retryTrigger by remember { mutableStateOf(0) }

    // Live ISS position polling effect (every 5 seconds with exponential backoff on error)
    LaunchedEffect(retryTrigger) {
        issLiveState = IssLiveState.Loading
        var failureCount = 0
        while (isActive) {
            val result = fetchLiveIssPosition(context)
            if (result is IssLiveResult.Success) {
                failureCount = 0
                issLiveState = IssLiveState.Success(
                    latitude = result.lat,
                    longitude = result.lon,
                    altitudeKm = result.altKm,
                    velocityKmh = result.velKmh,
                    timestampMs = result.timestampMs
                )
                delay(5000L)
            } else {
                failureCount++
                issLiveState = IssLiveState.Unavailable
                val backoffMs = when (failureCount) {
                    1 -> 5000L
                    2 -> 10000L
                    3 -> 20000L
                    else -> 40000L
                }
                delay(backoffMs)
            }
        }
    }

    // Periodically update time every 5 seconds & fetch TLE on launch
    LaunchedEffect(Unit) {
        isFetchingTle = true
        tleData = ISSEngine.fetchLatestTLE()
        isFetchingTle = false
        while (true) {
            delay(5000L)
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
        // Section 1: Live ISS Position World Map / Fallback Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("iss_live_position_section"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isFa) "موقعیت لحظه‌ای ایستگاه فضایی" else "Live ISS Position",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (issLiveState is IssLiveState.Success) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFA855F7).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFA855F7))
                                )
                                Text(
                                    text = if (isFa) "زنده" else "LIVE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = Color(0xFFA855F7)
                                )
                            }
                        }
                    }
                }

                when (val state = issLiveState) {
                    is IssLiveState.Success -> {
                        IssWorldMapCard(
                            lat = state.latitude,
                            lon = state.longitude,
                            altitudeKm = state.altitudeKm,
                            velocityKmh = state.velocityKmh,
                            timestampMs = state.timestampMs,
                            isFa = isFa
                        )
                    }
                    is IssLiveState.Unavailable -> {
                        IssUnavailableCard(
                            onRetry = { retryTrigger++ }
                        )
                    }
                    is IssLiveState.Loading -> {
                        IssLoadingCard()
                    }
                }
            }
        }

        // Section 2: ISS Orbit Telemetry Hero Card
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
                                text = if (isFa) latStr.toPersianDigits() else latStr,
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
                                text = if (isFa) lonStr.toPersianDigits() else lonStr,
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
                                text = if (isFa) altStr.toPersianDigits() else altStr,
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
                                text = if (isFa) speedStr.toPersianDigits() else speedStr,
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
                                text = if (isFa) elevStr.toPersianDigits() else elevStr,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (topocentricPos.elevationDeg > 0) Color(0xFF2DC653) else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column {
                            Text(
                                text = if (isFa) "سمت (زاویه افقی)" else "Azimuth",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val azStr = String.format("%.1f°", topocentricPos.azimuthDeg)
                            Text(
                                text = if (isFa) azStr.toPersianDigits() else azStr,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Column {
                            Text(
                                text = if (isFa) "فاصله مستقیم" else "Range Distance",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val rngStr = String.format("%.0f km", topocentricPos.rangeKm)
                            Text(
                                text = if (isFa) rngStr.toPersianDigits() else rngStr,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }

        // Section 3: Pass Filter & Predictions Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isFa) "پیش‌بینی گذرها (۷ روز آینده)" else "Pass Predictions (Next 7 Days)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                FilterChip(
                    selected = visiblePassesOnly,
                    onClick = { visiblePassesOnly = !visiblePassesOnly },
                    label = {
                        Text(
                            text = if (visiblePassesOnly) {
                                if (isFa) "فقط گذر‌های قابل مشاهده" else "Visible Only"
                            } else {
                                if (isFa) "همه گذرها (حتی تاریک)" else "All Passes"
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (visiblePassesOnly) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }

        // Section 4: Pass List Items
        if (passes.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isFa) "هیچ گذری با شرایط انتخابی در ۷ روز آینده یافت نشد." else "No passes found for the selected filter.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(passes) { pass ->
                val isExpanded = expandedPassMs == pass.startTimeMs
                val classColor = Color(pass.classification.colorHex)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            expandedPassMs = if (isExpanded) null else pass.startTimeMs
                        },
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, classColor.copy(alpha = 0.3f)),
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
                            Column {
                                val dateStr = dateFormatter.format(Date(pass.startTimeMs))
                                Text(
                                    text = if (isFa) dateStr.toPersianDigits() else dateStr,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                val timeRangeStr = "${timeOnlyFormatter.format(Date(pass.startTimeMs))} ➔ ${timeOnlyFormatter.format(Date(pass.endTimeMs))}"
                                Text(
                                    text = if (isFa) timeRangeStr.toPersianDigits() else timeRangeStr,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = classColor.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, classColor.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = if (isFa) pass.classification.labelFa else pass.classification.labelEn,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = classColor,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = if (isFa) "حداکثر ارتفاع" else "Max Elevation",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val maxElevStr = String.format("%.0f°", pass.maxElevationDeg)
                                Text(
                                    text = if (isFa) maxElevStr.toPersianDigits() else maxElevStr,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = classColor
                                )
                            }

                            Column {
                                Text(
                                    text = if (isFa) "روشنایی تخمینی" else "Brightness",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val magStr = String.format("%.1f mag", pass.estimatedMagnitude)
                                Text(
                                    text = if (isFa) magStr.toPersianDigits() else magStr,
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
                                    text = if (isFa) durStr.toPersianDigits() else durStr,
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

@Composable
private fun IssWorldMapCard(
    lat: Double,
    lon: Double,
    altitudeKm: Double,
    velocityKmh: Double,
    timestampMs: Long,
    isFa: Boolean
) {
    val animLat by animateFloatAsState(
        targetValue = lat.toFloat(),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "IssLat"
    )
    val animLon by animateFloatAsState(
        targetValue = lon.toFloat(),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "IssLon"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "IssPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0F0D18))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            fun mapX(longitude: Double): Float = ((longitude + 180.0) / 360.0 * w).toFloat()
            fun mapY(latitude: Double): Float = ((90.0 - latitude) / 180.0 * h).toFloat()

            // 1. Gridlines at 30° intervals
            val gridColor = Color.White.copy(alpha = 0.03f)
            for (gLon in -150..150 step 30) {
                val gx = mapX(gLon.toDouble())
                drawLine(gridColor, start = Offset(gx, 0f), end = Offset(gx, h), strokeWidth = 1f)
            }
            for (gLat in -60..60 step 30) {
                val gy = mapY(gLat.toDouble())
                drawLine(gridColor, start = Offset(0f, gy), end = Offset(w, gy), strokeWidth = 1f)
            }

            // Equator Line (Purple accent)
            val eqY = mapY(0.0)
            drawLine(
                color = Color(0xFFA855F7).copy(alpha = 0.15f),
                start = Offset(0f, eqY),
                end = Offset(w, eqY),
                strokeWidth = 1.5f
            )

            // 2. Draw World Continent Landmasses
            drawWorldContinents(this, w, h, Color(0xFF2A2438))

            // 3. Draw Orbit Track (Past 45m dashed & Future 90m solid)
            drawOrbitTrack(this, w, h, timestampMs)

            // 4. Draw Pulsing Glowing ISS Marker
            val mx = mapX(animLon.toDouble())
            val my = mapY(animLat.toDouble())
            val center = Offset(mx, my)

            // Outer soft halo glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFA855F7).copy(alpha = 0.40f),
                        Color(0xFFA855F7).copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = 24.dp.toPx()
                ),
                radius = 24.dp.toPx(),
                center = center
            )

            // Middle pulse ring
            drawCircle(
                color = Color(0xFFA855F7).copy(alpha = 0.30f / pulseScale),
                radius = 12.dp.toPx() * pulseScale,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Inner core
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFD946EF), Color(0xFFA855F7))
                ),
                radius = 7.dp.toPx(),
                center = center
            )

            // Center highlight dot
            drawCircle(
                color = Color.White,
                radius = 2.5f.dp.toPx(),
                center = center
            )
        }

        // Overlay Info Panel (Bottom corner pill card)
        val latDir = if (lat >= 0) "شمالی" else "جنوبی"
        val lonDir = if (lon >= 0) "شرقی" else "غربی"
        val coordsText = if (isFa) {
            "${String.format(Locale.US, "%.1f° %s", abs(lat), latDir)}، ${String.format(Locale.US, "%.1f° %s", abs(lon), lonDir)}".toPersianDigits()
        } else {
            String.format(Locale.US, "%.1f°N, %.1f°E", lat, lon)
        }
        val altText = if (isFa) "${String.format(Locale.US, "%,.0f", altitudeKm)} کیلومتر".toPersianDigits() else "${String.format(Locale.US, "%,.0f", altitudeKm)} km"
        val velText = if (isFa) "${String.format(Locale.US, "%,.0f", velocityKmh)} کیلومتر/ساعت".toPersianDigits() else "${String.format(Locale.US, "%,.0f", velocityKmh)} km/h"

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0D0B14).copy(alpha = 0.85f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "📍 موقعیت: $coordsText",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "📏 ارتفاع: $altText",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = Color.White.copy(alpha = 0.85f)
                )
                Text(
                    text = "⚡ سرعت: $velText",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun IssUnavailableCard(
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF13111A).copy(alpha = 0.70f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.PublicOff,
                contentDescription = null,
                tint = Color(0xFF6B7280),
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "موقعیت زنده ایستگاه در دسترس نیست",
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                color = Color(0xFF9CA3AF)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "برای نمایش موقعیت لحظه‌ای به اینترنت نیاز است",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = Color(0xFF6B7280)
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = onRetry,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFA855F7))
            ) {
                Text(
                    text = "تلاش مجدد",
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium)
                )
            }
        }
    }
}

@Composable
private fun IssLoadingCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0F0D18))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(
                color = Color(0xFFA855F7),
                modifier = Modifier.size(28.dp),
                strokeWidth = 2.5.dp
            )
            Text(
                text = "در حال دریافت موقعیت زنده...",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF9CA3AF)
            )
        }
    }
}

private fun drawWorldContinents(
    drawScope: androidx.compose.ui.graphics.drawscope.DrawScope,
    w: Float,
    h: Float,
    landColor: Color
) {
    fun mapX(lon: Double): Float = ((lon + 180.0) / 360.0 * w).toFloat()
    fun mapY(lat: Double): Float = ((90.0 - lat) / 180.0 * h).toFloat()

    val continents = listOf(
        // North America
        listOf(
            70.0 to -165.0, 72.0 to -125.0, 60.0 to -105.0, 50.0 to -65.0,
            25.0 to -80.0, 15.0 to -90.0, 8.0 to -78.0, 15.0 to -105.0,
            30.0 to -115.0, 55.0 to -165.0
        ),
        // Greenland
        listOf(
            75.0 to -45.0, 82.0 to -30.0, 65.0 to -38.0, 60.0 to -45.0
        ),
        // South America
        listOf(
            10.0 to -75.0, 5.0 to -50.0, -10.0 to -35.0, -23.0 to -42.0,
            -55.0 to -68.0, -45.0 to -75.0, -18.0 to -70.0, 5.0 to -78.0
        ),
        // Europe
        listOf(
            36.0 to -9.0, 43.0 to -9.0, 48.0 to -4.0, 54.0 to 5.0,
            58.0 to 6.0, 68.0 to 14.0, 71.0 to 28.0, 60.0 to 30.0,
            45.0 to 35.0, 38.0 to 24.0, 36.0 to -9.0
        ),
        // Africa
        listOf(
            36.0 to -5.0, 37.0 to 10.0, 32.0 to 32.0, 12.0 to 43.0,
            10.0 to 51.0, -12.0 to 40.0, -34.0 to 20.0, -34.0 to 18.0,
            -18.0 to 12.0, 4.0 to 9.0, 15.0 to -17.0, 35.0 to -6.0
        ),
        // Asia
        listOf(
            75.0 to 60.0, 75.0 to 170.0, 60.0 to 170.0, 40.0 to 140.0,
            22.0 to 120.0, 10.0 to 105.0, 1.0 to 104.0, 8.0 to 77.0,
            25.0 to 65.0, 12.0 to 44.0, 30.0 to 35.0, 40.0 to 30.0,
            55.0 to 60.0
        ),
        // Australia
        listOf(
            -12.0 to 130.0, -12.0 to 142.0, -25.0 to 153.0,
            -38.0 to 148.0, -35.0 to 117.0, -20.0 to 114.0
        ),
        // Antarctica
        listOf(
            -65.0 to -180.0, -65.0 to 180.0, -90.0 to 180.0, -90.0 to -180.0
        )
    )

    for (polygon in continents) {
        val path = Path()
        polygon.forEachIndexed { index, (lat, lon) ->
            val px = mapX(lon)
            val py = mapY(lat)
            if (index == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        drawScope.drawPath(path = path, color = landColor)
    }
}

private fun drawOrbitTrack(
    drawScope: androidx.compose.ui.graphics.drawscope.DrawScope,
    w: Float,
    h: Float,
    currentTimestampMs: Long
) {
    fun mapX(lon: Double): Float = ((lon + 180.0) / 360.0 * w).toFloat()
    fun mapY(lat: Double): Float = ((90.0 - lat) / 180.0 * h).toFloat()

    val pastPath = Path()
    val futurePath = Path()

    var prevLon: Double? = null
    var isPastNewSection = true
    var isFutureNewSection = true

    val stepMs = 2 * 60 * 1000L
    val startMs = currentTimestampMs - 45 * 60 * 1000L
    val endMs = currentTimestampMs + 90 * 60 * 1000L

    var t = startMs
    while (t <= endMs) {
        val pos = ISSEngine.calculateTopocentricPos(t, 0.0, 0.0)
        val lat = pos.subLatDeg
        val lon = pos.subLonDeg

        val isPast = t <= currentTimestampMs
        val targetPath = if (isPast) pastPath else futurePath

        if (prevLon != null && abs(lon - prevLon) > 180.0) {
            if (isPast) isPastNewSection = true else isFutureNewSection = true
        }

        val px = mapX(lon)
        val py = mapY(lat)

        val isNewSection = if (isPast) isPastNewSection else isFutureNewSection
        if (isNewSection) {
            targetPath.moveTo(px, py)
            if (isPast) isPastNewSection = false else isFutureNewSection = false
        } else {
            targetPath.lineTo(px, py)
        }

        prevLon = lon
        t += stepMs
    }

    // Past track: Dashed line
    drawScope.drawPath(
        path = pastPath,
        color = Color(0xFFA855F7).copy(alpha = 0.35f),
        style = Stroke(
            width = 3f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f))
        )
    )

    // Future track: Solid line
    drawScope.drawPath(
        path = futurePath,
        color = Color(0xFFA855F7).copy(alpha = 0.55f),
        style = Stroke(width = 3.5f)
    )
}

private suspend fun fetchLiveIssPosition(context: Context): IssLiveResult = withContext(Dispatchers.IO) {
    val isConnected = try {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNet = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(activeNet)
        caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    } catch (e: Exception) {
        true
    }

    if (!isConnected) {
        return@withContext IssLiveResult.Failure("No Internet Connection")
    }

    // 1. Primary API: wheretheiss.at
    try {
        val url = URL("https://api.wheretheiss.at/v1/satellites/25544")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 4000
            readTimeout = 4000
            requestMethod = "GET"
        }
        if (conn.responseCode == 200) {
            val jsonText = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(jsonText)
            val lat = json.getDouble("latitude")
            val lon = json.getDouble("longitude")
            val altKm = json.optDouble("altitude", 418.0)
            val velKmh = json.optDouble("velocity", 27600.0)
            val tsSec = json.optLong("timestamp", System.currentTimeMillis() / 1000)
            val tsMs = tsSec * 1000L

            if (lat in -90.0..90.0 && lon in -180.0..180.0) {
                val ageMs = abs(System.currentTimeMillis() - tsMs)
                if (ageMs < 30000) {
                    return@withContext IssLiveResult.Success(lat, lon, altKm, velKmh, tsMs)
                }
            }
        }
    } catch (e: Exception) {
        // Fallback to open-notify
    }

    // 2. Backup API: open-notify.org
    try {
        val url2 = URL("http://api.open-notify.org/iss-now.json")
        val conn2 = (url2.openConnection() as HttpURLConnection).apply {
            connectTimeout = 4000
            readTimeout = 4000
            requestMethod = "GET"
        }
        if (conn2.responseCode == 200) {
            val jsonText = conn2.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(jsonText)
            val pos = json.getJSONObject("iss_position")
            val lat = pos.getString("latitude").toDouble()
            val lon = pos.getString("longitude").toDouble()
            val tsSec = json.optLong("timestamp", System.currentTimeMillis() / 1000)
            val tsMs = tsSec * 1000L

            if (lat in -90.0..90.0 && lon in -180.0..180.0) {
                return@withContext IssLiveResult.Success(lat, lon, 418.0, 27600.0, tsMs)
            }
        }
    } catch (e2: Exception) {
        // Both APIs failed
    }

    return@withContext IssLiveResult.Failure("APIs Unreachable")
}

private fun getAzimuthCardinal(azimuthDeg: Double, isFa: Boolean): String {
    val en = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    val fa = arrayOf("شمال", "شمال‌شرق", "شرق", "جنوب‌شرق", "جنوب", "جنوب‌غرب", "غرب", "شمال‌غرب")
    val idx = (((azimuthDeg + 22.5) % 360) / 45.0).toInt().coerceIn(0, 7)
    return if (isFa) fa[idx] else en[idx]
}

package com.alijafari.red.astronomy.ui.screens

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.astro_engine.ISSEngine
import com.alijafari.red.astronomy.astro_engine.TimeEngine
import com.alijafari.red.astronomy.domain.AppLanguage
import com.alijafari.red.astronomy.notification.AstroNotificationManager
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.MainViewModel
import com.alijafari.red.astronomy.ui.components.NurabadHistoryModal
import com.alijafari.red.astronomy.ui.theme.IranSans
import com.alijafari.red.astronomy.util.toPersianDigits
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

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

data class MapLayersState(
    val dayNightShadow: Boolean = true,
    val twilightZones: Boolean = true,
    val cityLights: Boolean = true,
    val clouds: Boolean = true,
    val bordersAndLabels: Boolean = true,
    val issTrail: Boolean = true,
    val futurePath: Boolean = true,
    val weather: Boolean = false,
    val auroraOval: Boolean = false
)

data class CityLight(
    val nameEn: String,
    val nameFa: String,
    val lat: Double,
    val lon: Double,
    val isNC: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ISSScreen(
    uiState: MainUiState,
    viewModel: MainViewModel
) {
    val isFa = uiState.language == AppLanguage.PERSIAN
    val context = LocalContext.current

    val prefs = remember { context.getSharedPreferences("astro_prefs", Context.MODE_PRIVATE) }
    var isAutoAlertEnabled by remember {
        mutableStateOf(prefs.getBoolean("iss_auto_alerts_enabled", false))
    }
    var selectedLeadMinutes by remember {
        mutableStateOf(prefs.getInt("iss_alert_lead_minutes", 10))
    }

    var pendingLeadMinutesChoice by remember { mutableStateOf(10) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        prefs.edit()
            .putBoolean("iss_auto_alerts_enabled", true)
            .putInt("iss_alert_lead_minutes", pendingLeadMinutesChoice)
            .putFloat("user_lat", uiState.userLocation.latitude.toFloat())
            .putFloat("user_lon", uiState.userLocation.longitude.toFloat())
            .putString("user_city_name_fa", uiState.userLocation.cityNameFa)
            .putString("user_city_name_en", uiState.userLocation.cityNameEn)
            .apply()

        isAutoAlertEnabled = true
        selectedLeadMinutes = pendingLeadMinutesChoice

        AstroNotificationManager.scheduleUpcomingIssPasses(
            context = context,
            userLocation = uiState.userLocation,
            leadMinutes = pendingLeadMinutesChoice
        )

        Toast.makeText(
            context,
            if (isFa) "هشدار خودکار $pendingLeadMinutesChoice دقیقه قبل از گذرهای قابل مشاهده ISS فعال شد!"
            else "Auto notification set for $pendingLeadMinutesChoice mins prior to visible passes!",
            Toast.LENGTH_LONG
        ).show()
    }

    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    var tleData by remember { mutableStateOf(ISSEngine.TLEData()) }
    var isFetchingTle by remember { mutableStateOf(false) }
    var visiblePassesOnly by remember { mutableStateOf(true) }
    var expandedPassMs by remember { mutableStateOf<Long?>(null) }

    var issLiveState by remember { mutableStateOf<IssLiveState>(IssLiveState.Loading) }
    var retryTrigger by remember { mutableStateOf(0) }

    // History Modal for Nurabad City (NC)
    var showNcHistoryModal by remember { mutableStateOf(false) }

    // Live ISS position polling effect
    LaunchedEffect(retryTrigger) {
        issLiveState = IssLiveState.Loading
        var failureCount = 0
        while (isActive) {
            val result = fetchLiveIssPosition(context)
            when (result) {
                is IssLiveResult.Success -> {
                    failureCount = 0
                    issLiveState = IssLiveState.Success(
                        latitude = result.lat,
                        longitude = result.lon,
                        altitudeKm = result.altKm,
                        velocityKmh = result.velKmh,
                        timestampMs = result.timestampMs
                    )
                    delay(5000)
                }
                is IssLiveResult.Failure -> {
                    failureCount++
                    val currentPos = ISSEngine.calculateTopocentricPos(
                        System.currentTimeMillis(),
                        uiState.userLocation.latitude,
                        uiState.userLocation.longitude
                    )
                    issLiveState = IssLiveState.Success(
                        latitude = currentPos.subLatDeg,
                        longitude = currentPos.subLonDeg,
                        altitudeKm = currentPos.satAltKm,
                        velocityKmh = currentPos.velocityKmS * 3600.0,
                        timestampMs = System.currentTimeMillis()
                    )
                    val backoffMs = (5000L * (1 shl failureCount.coerceAtMost(3)))
                    delay(backoffMs)
                }
            }
        }
    }

    // Timer effect to refresh time every second
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            nowMs = System.currentTimeMillis()
        }
    }

    // Fetch TLE
    LaunchedEffect(Unit) {
        isFetchingTle = true
        withContext(Dispatchers.IO) {
            tleData = ISSEngine.fetchLatestTLE()
        }
        isFetchingTle = false
    }

    val passes = remember(tleData, uiState.userLocation, nowMs) {
        ISSEngine.predictPasses(
            userLatDeg = uiState.userLocation.latitude,
            userLonDeg = uiState.userLocation.longitude,
            startTimestampMs = nowMs,
            tle = tleData,
            scanDays = 5,
            visibleOnly = false
        )
    }

    val filteredPasses = remember(passes, visiblePassesOnly) {
        if (visiblePassesOnly) passes.filter { it.classification != ISSEngine.PassClassification.NOT_VISIBLE && it.classification != ISSEngine.PassClassification.DAYLIGHT_ONLY } else passes
    }

    val topocentricPos = remember(nowMs, uiState.userLocation) {
        ISSEngine.calculateTopocentricPos(
            timestampMs = nowMs,
            userLatDeg = uiState.userLocation.latitude,
            userLonDeg = uiState.userLocation.longitude
        )
    }

    // History Modal Dialog
    if (showNcHistoryModal) {
        NurabadHistoryModal(
            isFa = isFa,
            onDismiss = { showNcHistoryModal = false }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("iss_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Section 1: MISSION CONTROL VECTOR EARTH MAP (Hero Map)
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isFa) "نقشه زنده ایستگاه فضایی و کره زمین (Mission Control)" else "Mission Control Earth & ISS Map",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = IranSans,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

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
                                text = if (isFa) "پایش مداری زنده" else "LIVE ORBIT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = Color(0xFFA855F7)
                            )
                        }
                    }
                }

                val currentLat = when (val state = issLiveState) {
                    is IssLiveState.Success -> state.latitude
                    else -> topocentricPos.subLatDeg
                }
                val currentLon = when (val state = issLiveState) {
                    is IssLiveState.Success -> state.longitude
                    else -> topocentricPos.subLonDeg
                }
                val currentAltKm = when (val state = issLiveState) {
                    is IssLiveState.Success -> state.altitudeKm
                    else -> topocentricPos.satAltKm
                }
                val currentVelKmh = when (val state = issLiveState) {
                    is IssLiveState.Success -> state.velocityKmh
                    else -> topocentricPos.velocityKmS * 3600.0
                }

                MissionControlEarthMap(
                    currentLat = currentLat,
                    currentLon = currentLon,
                    altitudeKm = currentAltKm,
                    velocityKmh = currentVelKmh,
                    timestampMs = nowMs,
                    isFa = isFa,
                    userLat = uiState.userLocation.latitude,
                    userLon = uiState.userLocation.longitude,
                    onOpenNcHistory = { showNcHistoryModal = true }
                )
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
                                    fontFamily = IranSans,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "NORAD 25544 • SGP4 Precision Model",
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
                                    color = statusColor,
                                    fontFamily = IranSans
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                    // Telemetry Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val azCard = getAzimuthCardinal(topocentricPos.azimuthDeg, isFa)
                        TelemetryTile(
                            label = if (isFa) "ارتفاع / سمت" else "Alt / Azimuth",
                            value = String.format(Locale.US, "%.1f° • %.0f° (%s)", topocentricPos.elevationDeg, topocentricPos.azimuthDeg, azCard).let { if (isFa) it.toPersianDigits() else it },
                            subValue = if (topocentricPos.elevationDeg > 0) (if (isFa) "بالای افق" else "Above Horizon") else (if (isFa) "زیر افق" else "Below Horizon"),
                            isPrimary = topocentricPos.elevationDeg > 0,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        TelemetryTile(
                            label = if (isFa) "فاصله مستقیم" else "Range Distance",
                            value = String.format(Locale.US, "%,.0f km", topocentricPos.rangeKm).let { if (isFa) it.toPersianDigits() else it },
                            subValue = if (isFa) "از موقعیت شما" else "From your location",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val latCard = if (topocentricPos.subLatDeg >= 0) "N" else "S"
                        val lonCard = if (topocentricPos.subLonDeg >= 0) "E" else "W"
                        val subPointStr = String.format(Locale.US, "%.2f°%s, %.2f°%s", abs(topocentricPos.subLatDeg), latCard, abs(topocentricPos.subLonDeg), lonCard)

                        TelemetryTile(
                            label = if (isFa) "نقطه زیر ماهواره" else "Sub-Sat Location",
                            value = if (isFa) subPointStr.toPersianDigits() else subPointStr,
                            subValue = if (isFa) "بر فراز کره زمین" else "Surface Projection",
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        TelemetryTile(
                            label = if (isFa) "سرعت / ارتفاع مدار" else "Speed / Altitude",
                            value = if (isFa) "۲۷,۶۰۰ km/h • ۴۱۸ km".toPersianDigits() else "27,600 km/h • 418 km",
                            subValue = if (isFa) "مدار نزدیک زمین (LEO)" else "Low Earth Orbit",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Section 3: Upcoming Passes Header & List
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isFa) "گذرهای پیش‌رو بالای شهر شما" else "Upcoming Pass Schedule",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = IranSans,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isFa) "بر اساس موقعیت جغرافیایی فعال" else "Calculated for your location",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = IranSans,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Toggle: Visible Only vs All Passes
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isFa) "فقط قابل مشاهده" else "Visible only",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = IranSans,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Switch(
                            checked = visiblePassesOnly,
                            onCheckedChange = { visiblePassesOnly = it },
                            modifier = Modifier.scale(0.8f)
                        )
                    }
                }

                if (isFetchingTle) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }

        if (filteredPasses.isEmpty() && !isFetchingTle) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isFa) "هیچ گذر قابل مشاهده‌ای در ۵ روز آینده یافت نشد." else "No visible passes found in the next 5 days.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = IranSans,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredPasses) { pass ->
                val isExpanded = expandedPassMs == pass.startTimeMs
                IssPassCard(
                    pass = pass,
                    isExpanded = isExpanded,
                    isFa = isFa,
                    onClick = {
                        expandedPassMs = if (isExpanded) null else pass.startTimeMs
                    }
                )
            }
        }
    }
}

@Composable
private fun MissionControlEarthMap(
    currentLat: Double,
    currentLon: Double,
    altitudeKm: Double,
    velocityKmh: Double,
    timestampMs: Long,
    isFa: Boolean,
    userLat: Double,
    userLon: Double,
    onOpenNcHistory: () -> Unit
) {
    var layers by remember { mutableStateOf(MapLayersState()) }
    var showLayersMenu by remember { mutableStateOf(false) }

    var timeMachineOffsetHours by remember { mutableStateOf(0f) }
    val activeTimestampMs = timestampMs + (timeMachineOffsetHours * 3600 * 1000L).toLong()

    // Calculate ISS location at active timestamp
    val activeIssPos = remember(activeTimestampMs, currentLat, currentLon) {
        if (timeMachineOffsetHours == 0f) {
            Pair(currentLat, currentLon)
        } else {
            val pos = ISSEngine.calculateTopocentricPos(activeTimestampMs, 0.0, 0.0)
            Pair(pos.subLatDeg, pos.subLonDeg)
        }
    }

    // Zoom and pan state
    var zoomScale by remember { mutableStateOf(1.0f) }
    var panOffsetX by remember { mutableStateOf(0f) }
    var panOffsetY by remember { mutableStateOf(0f) }

    // Selected city details sheet
    var selectedCity by remember { mutableStateOf<CityLight?>(null) }

    // Cities dataset
    val cities = remember {
        listOf(
            CityLight("Nurabad (NC)", "نورآباد ممسنی (NC)", 30.1141, 51.5217, isNC = true),
            CityLight("Tehran", "تهران", 35.6892, 51.3890),
            CityLight("Shiraz", "شیراز", 29.5918, 52.5837),
            CityLight("Isfahan", "اصفهان", 32.6546, 51.6680),
            CityLight("Tabriz", "تبریز", 38.0962, 46.2738),
            CityLight("Mashhad", "مشهد", 36.2972, 59.6067),
            CityLight("Dubai", "دبی", 25.2048, 55.2708),
            CityLight("Riyadh", "ریاض", 24.7136, 46.6753),
            CityLight("London", "لندن", 51.5074, -0.1278),
            CityLight("Paris", "پاریس", 48.8566, 2.3522),
            CityLight("Tokyo", "توکیو", 35.6762, 139.6503),
            CityLight("New York", "نیویورک", 40.7128, -74.0060),
            CityLight("Sydney", "سیدنی", -33.8688, 151.2093)
        )
    }

    val pulseAnim = rememberInfiniteTransition(label = "NCPulse")
    val ncPulseRadius by pulseAnim.animateFloat(
        initialValue = 8f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(animation = tween(1500, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "Pulse"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(440.dp)
            .testTag("mission_control_map"),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF070B19))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Interactive Map Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            zoomScale = (zoomScale * zoom).coerceIn(1.0f, 15.0f)
                            panOffsetX = (panOffsetX + pan.x).coerceIn(-1000f * zoomScale, 1000f * zoomScale)
                            panOffsetY = (panOffsetY + pan.y).coerceIn(-600f * zoomScale, 600f * zoomScale)
                        }
                    }
            ) {
                val w = size.width
                val h = size.height

                fun mapX(lon: Double): Float = (((lon + 180.0) / 360.0 * w).toFloat() + panOffsetX) * zoomScale
                fun mapY(lat: Double): Float = (((90.0 - lat) / 180.0 * h).toFloat() + panOffsetY) * zoomScale

                // 1. Ocean Background
                drawRect(color = Color(0xFF030A1C))

                // Grid Lines
                for (lat in -80..80 step 20) {
                    val y = mapY(lat.toDouble())
                    drawLine(color = Color.White.copy(alpha = 0.05f), start = Offset(0f, y), end = Offset(w, y), strokeWidth = 1f)
                }
                for (lon in -180..180 step 30) {
                    val x = mapX(lon.toDouble())
                    drawLine(color = Color.White.copy(alpha = 0.05f), start = Offset(x, 0f), end = Offset(x, h), strokeWidth = 1f)
                }

                // Equator & Prime Meridian
                val eqY = mapY(0.0)
                drawLine(color = Color(0xFF38BDF8).copy(alpha = 0.2f), start = Offset(0f, eqY), end = Offset(w, eqY), strokeWidth = 1.5f)

                // 2. World Continents
                drawWorldContinents(this, w, h, landColor = Color(0xFF1E293B))

                // Detailed High-Res Iran Polygon Highlight
                val iranPoly = listOf(
                    38.6 to 44.0, 39.7 to 48.0, 38.4 to 48.8, 37.4 to 50.0, 37.5 to 54.0,
                    37.0 to 59.0, 35.5 to 61.2, 30.0 to 61.8, 25.0 to 61.5, 25.3 to 57.0,
                    27.0 to 56.3, 29.8 to 50.0, 30.4 to 48.0, 33.5 to 46.0, 36.5 to 45.0
                )
                val iranPath = Path()
                iranPoly.forEachIndexed { i, (lat, lon) ->
                    val px = mapX(lon)
                    val py = mapY(lat)
                    if (i == 0) iranPath.moveTo(px, py) else iranPath.lineTo(px, py)
                }
                iranPath.close()

                // Highlight Iran landmass with rich emerald glow
                drawPath(path = iranPath, color = Color(0xFF10B981).copy(alpha = 0.22f))
                drawPath(path = iranPath, color = Color(0xFF10B981).copy(alpha = 0.6f), style = Stroke(width = 2f))

                // 3. Day/Night Terminator & Twilight Zones
                if (layers.dayNightShadow) {
                    val subsolar = calculateSubsolarPoint(activeTimestampMs)
                    val numStepLons = 72
                    val shadowPath = Path()

                    for (i in 0..numStepLons) {
                        val lon = -180.0 + (i * 360.0 / numStepLons)
                        val latTerm = atan2(-cos(Math.toRadians(lon - subsolar.second)), sin(Math.toRadians(subsolar.first)))
                        val latDeg = Math.toDegrees(latTerm)

                        val px = mapX(lon)
                        val py = mapY(latDeg)
                        if (i == 0) shadowPath.moveTo(px, py) else shadowPath.lineTo(px, py)
                    }
                    if (subsolar.first > 0) {
                        shadowPath.lineTo(mapX(180.0), mapY(-90.0))
                        shadowPath.lineTo(mapX(-180.0), mapY(-90.0))
                    } else {
                        shadowPath.lineTo(mapX(180.0), mapY(90.0))
                        shadowPath.lineTo(mapX(-180.0), mapY(90.0))
                    }
                    shadowPath.close()

                    drawPath(path = shadowPath, color = Color(0xFF020617).copy(alpha = 0.78f))
                }

                // 4. City Lights (Glow in darkness)
                if (layers.cityLights) {
                    val subsolar = calculateSubsolarPoint(activeTimestampMs)
                    for (city in cities) {
                        val cx = mapX(city.lon)
                        val cy = mapY(city.lat)

                        val isNight = isLocationInNight(city.lat, city.lon, subsolar.first, subsolar.second)

                        if (city.isNC) {
                            // SPECIAL TREATMENT FOR NURABAD CITY (NC)
                            // Glowing pulse aura ring
                            drawCircle(
                                color = Color(0xFFFFB703).copy(alpha = 0.35f),
                                radius = ncPulseRadius * zoomScale,
                                center = Offset(cx, cy)
                            )
                            drawCircle(
                                color = Color(0xFFFFB703),
                                radius = 5f * zoomScale,
                                center = Offset(cx, cy)
                            )
                        } else if (isNight) {
                            drawCircle(
                                color = Color(0xFFFDE047).copy(alpha = 0.8f),
                                radius = 2.5f * zoomScale,
                                center = Offset(cx, cy)
                            )
                        }
                    }
                }

                // 5. User Location Marker
                val userX = mapX(userLon)
                val userY = mapY(userLat)
                drawCircle(color = Color(0xFF38BDF8), radius = 4f * zoomScale, center = Offset(userX, userY))

                // 6. ISS Orbit Track
                if (layers.issTrail || layers.futurePath) {
                    drawOrbitTrack(this, w, h, activeTimestampMs)
                }

                // 7. Active ISS Position Icon
                val issX = mapX(activeIssPos.second)
                val issY = mapY(activeIssPos.first)

                // ISS Halo Ring
                drawCircle(color = Color(0xFFA855F7).copy(alpha = 0.35f), radius = 14f * zoomScale, center = Offset(issX, issY))
                drawCircle(color = Color(0xFFA855F7), radius = 6f * zoomScale, center = Offset(issX, issY))
                drawCircle(color = Color.White, radius = 3f * zoomScale, center = Offset(issX, issY))
            }

            // FLOATING NC MARKER PILL BUTTON ON MAP
            Surface(
                onClick = {
                    selectedCity = cities.first { it.isNC }
                },
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFB703),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("👑", fontSize = 12.sp)
                    Text(
                        text = if (isFa) "نورآباد ممسنی (NC)" else "Nurabad (NC)",
                        style = TextStyle(fontFamily = IranSans, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black)
                    )
                }
            }

            // TOP-RIGHT LAYER SELECTOR BUTTON
            IconButton(
                onClick = { showLayersMenu = !showLayersMenu },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F172A).copy(alpha = 0.9f))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(Icons.Default.Layers, contentDescription = "Layers", tint = Color.White)
            }

            // LAYER SELECTOR DROPDOWN MENU
            DropdownMenu(
                expanded = showLayersMenu,
                onDismissRequest = { showLayersMenu = false },
                modifier = Modifier.background(Color(0xFF0F172A))
            ) {
                DropdownMenuItem(
                    text = { Text(if (isFa) "سایه روز و شب" else "Day/Night Shadow", color = Color.White, fontFamily = IranSans) },
                    trailingIcon = { Checkbox(checked = layers.dayNightShadow, onCheckedChange = { layers = layers.copy(dayNightShadow = it) }) },
                    onClick = { layers = layers.copy(dayNightShadow = !layers.dayNightShadow) }
                )
                DropdownMenuItem(
                    text = { Text(if (isFa) "روشنایی شهرها در شب" else "City Lights", color = Color.White, fontFamily = IranSans) },
                    trailingIcon = { Checkbox(checked = layers.cityLights, onCheckedChange = { layers = layers.copy(cityLights = it) }) },
                    onClick = { layers = layers.copy(cityLights = !layers.cityLights) }
                )
                DropdownMenuItem(
                    text = { Text(if (isFa) "مسیر مداری ISS" else "ISS Trail", color = Color.White, fontFamily = IranSans) },
                    trailingIcon = { Checkbox(checked = layers.issTrail, onCheckedChange = { layers = layers.copy(issTrail = it) }) },
                    onClick = { layers = layers.copy(issTrail = !layers.issTrail) }
                )
            }

            // BOTTOM TIME MACHINE SCRUBBER BAR
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.92f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isFa) "⏰ ماشین زمان (زمان‌سنج مداری)" else "⏰ Time Machine Scrubber",
                            style = TextStyle(fontFamily = IranSans, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                        )

                        val offsetStr = if (timeMachineOffsetHours == 0f) {
                            if (isFa) "زمان زنده" else "LIVE"
                        } else {
                            val sign = if (timeMachineOffsetHours > 0) "+" else ""
                            String.format("%.1fh", timeMachineOffsetHours).let { if (isFa) "$sign$it".toPersianDigits() else "$sign$it" }
                        }

                        Text(
                            text = offsetStr,
                            style = TextStyle(fontFamily = IranSans, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFA855F7))
                        )
                    }

                    // Scrubber Slider
                    Slider(
                        value = timeMachineOffsetHours,
                        onValueChange = { timeMachineOffsetHours = it },
                        valueRange = -24f..24f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFA855F7),
                            activeTrackColor = Color(0xFFA855F7)
                        )
                    )

                    // Quick Preset Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { timeMachineOffsetHours = 0f },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f).height(30.dp)
                        ) {
                            Text(if (isFa) "زنده" else "LIVE", style = TextStyle(fontFamily = IranSans, fontWeight = FontWeight.Bold, fontSize = 10.sp))
                        }

                        Button(
                            onClick = { timeMachineOffsetHours -= 1.5f },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f).height(30.dp)
                        ) {
                            Text(if (isFa) "مدار قبل" else "-1 Orbit", style = TextStyle(fontFamily = IranSans, fontSize = 10.sp, color = Color.White))
                        }

                        Button(
                            onClick = { timeMachineOffsetHours += 1.5f },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f).height(30.dp)
                        ) {
                            Text(if (isFa) "مدار بعد" else "+1 Orbit", style = TextStyle(fontFamily = IranSans, fontSize = 10.sp, color = Color.White))
                        }
                    }
                }
            }
        }
    }

    // CITY DETAILS BOTTOM SHEET FOR NURABAD CITY (NC) & CITIES
    selectedCity?.let { city ->
        AlertDialog(
            onDismissRequest = { selectedCity = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (city.isNC) Text("👑", fontSize = 20.sp)
                    Text(
                        text = if (isFa) city.nameFa else city.nameEn,
                        style = TextStyle(fontFamily = IranSans, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val latStr = String.format("%.4f°N", city.lat).let { if (isFa) it.toPersianDigits() else it }
                    val lonStr = String.format("%.4f°E", city.lon).let { if (isFa) it.toPersianDigits() else it }

                    Text(
                        text = if (isFa) "مختصات: $latStr • $lonStr" else "Coordinates: $latStr • $lonStr",
                        style = TextStyle(fontFamily = IranSans, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    )

                    if (city.isNC) {
                        Text(
                            text = if (isFa) "ارتفاع از سطح دریا: ۹۲۰ متر • وضعیت هوا: ۲۸°C صاف" else "Elevation: 920 m • Weather: 28°C Clear",
                            style = TextStyle(fontFamily = IranSans, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        )

                        HorizontalDivider()

                        Button(
                            onClick = {
                                selectedCity = null
                                onOpenNcHistory()
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB703), contentColor = Color.Black),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.History, contentDescription = null, tint = Color.Black)
                                Text(
                                    text = if (isFa) "تاریخ باستانی نورآباد ممسنی (پیش از اسلام)" else "Ancient History of Nurabad (Pre-Islamic)",
                                    style = TextStyle(fontFamily = IranSans, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedCity = null }) {
                    Text(if (isFa) "بستن" else "Close", fontFamily = IranSans)
                }
            }
        )
    }
}

private fun calculateSubsolarPoint(timestampMs: Long): Pair<Double, Double> {
    val jd = (timestampMs / 86400000.0) + 2440587.5
    val d = jd - 2451545.0
    val g = Math.toRadians((357.529 + 0.98560028 * d) % 360.0)
    val q = (280.459 + 0.98564736 * d) % 360.0
    val L = Math.toRadians((q + 1.915 * sin(g) + 0.020 * sin(2 * g)) % 360.0)
    val e = Math.toRadians(23.439 - 0.00000036 * d)

    val lat = Math.toDegrees(asin(sin(e) * sin(L)))
    val gmst = (18.697374558 + 24.06570982441908 * d) % 24.0
    val lon = (-(gmst * 15.0) + 540.0) % 360.0 - 180.0

    return Pair(lat, lon)
}

private fun isLocationInNight(lat: Double, lon: Double, subLat: Double, subLon: Double): Boolean {
    val phi1 = Math.toRadians(lat)
    val phi2 = Math.toRadians(subLat)
    val deltaLon = Math.toRadians(lon - subLon)

    val cosZenith = sin(phi1) * sin(phi2) + cos(phi1) * cos(phi2) * cos(deltaLon)
    val solarAltitude = Math.toDegrees(asin(cosZenith.coerceIn(-1.0, 1.0)))

    return solarAltitude < 0.0
}

@Composable
private fun TelemetryTile(
    label: String,
    value: String,
    subValue: String,
    isPrimary: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (isPrimary) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (isPrimary) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = IranSans,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                fontFamily = IranSans,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subValue,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = IranSans,
                color = if (isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun IssPassCard(
    pass: ISSEngine.ISSPass,
    isExpanded: Boolean,
    isFa: Boolean,
    onClick: () -> Unit
) {
    val isVisiblePass = pass.classification != ISSEngine.PassClassification.NOT_VISIBLE && pass.classification != ISSEngine.PassClassification.DAYLIGHT_ONLY

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            1.dp,
            if (isVisiblePass) Color(0xFFA855F7).copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isVisiblePass) Color(0xFFA855F7).copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        imageVector = if (isVisiblePass) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = if (isVisiblePass) Color(0xFFA855F7) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )

                    Column {
                        val dateFormatted = TimeEngine.formatDate(pass.startTimeMs, com.alijafari.red.astronomy.domain.CalendarSystem.GREGORIAN, isFa).let { if (isFa) it.toPersianDigits() else it }
                        val startTime = TimeEngine.formatTime24h(pass.startTimeMs, isFa)
                        Text(
                            text = "$dateFormatted • $startTime",
                            style = MaterialTheme.typography.titleSmall,
                            fontFamily = IranSans,
                            fontWeight = FontWeight.Bold
                        )
                        val visibilityStr = if (isFa) pass.classification.labelFa else pass.classification.labelEn
                        Text(
                            text = visibilityStr,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = IranSans,
                            color = if (isVisiblePass) Color(0xFFA855F7) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val maxAltStr = String.format("%.0f°", pass.maxElevationDeg).let { if (isFa) it.toPersianDigits() else it }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = maxAltStr,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            fontFamily = IranSans,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isExpanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                val durationSec = pass.passDurationSec
                val durationStr = if (isFa) "$durationSec ثانیه".toPersianDigits() else "$durationSec sec"

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (isFa) "مدت زمان گذر:" else "Pass Duration:",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = IranSans,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = durationStr,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            fontFamily = IranSans
                        )
                    }

                    Column {
                        Text(
                            text = if (isFa) "بیشینه ارتفاع:" else "Max Elevation:",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = IranSans,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format("%.1f°", pass.maxElevationDeg).let { if (isFa) it.toPersianDigits() else it },
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            fontFamily = IranSans
                        )
                    }

                    Column {
                        Text(
                            text = if (isFa) "جهت حرکت:" else "Traverse Direction:",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = IranSans,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val startAzStr = getAzimuthCardinal(pass.startAzimuthDeg, isFa)
                        val endAzStr = getAzimuthCardinal(pass.endAzimuthDeg, isFa)
                        Text(
                            text = "$startAzStr ➔ $endAzStr",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            fontFamily = IranSans
                        )
                    }
                }
            }
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
        color = Color(0xFFA855F7).copy(alpha = 0.65f),
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
                return@withContext IssLiveResult.Success(lat, lon, altKm, velKmh, tsMs)
            }
        }
    } catch (e: Exception) {
        // Fallback
    }

    return@withContext IssLiveResult.Failure("APIs Unreachable")
}

private fun getAzimuthCardinal(azimuthDeg: Double, isFa: Boolean): String {
    val en = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    val fa = arrayOf("شمال", "شمال‌شرق", "شرق", "جنوب‌شرق", "جنوب", "جنوب‌غرب", "غرب", "شمال‌غرب")
    val idx = (((azimuthDeg + 22.5) % 360) / 45.0).toInt().coerceIn(0, 7)
    return if (isFa) fa[idx] else en[idx]
}

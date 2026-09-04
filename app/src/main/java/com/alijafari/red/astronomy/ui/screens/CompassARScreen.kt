package com.alijafari.red.astronomy.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Surface
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.alijafari.red.astronomy.BuildConfig
import com.alijafari.red.astronomy.startracker.debug.StarTrackerDebugHost
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.alijafari.red.astronomy.astro_engine.*
import com.alijafari.red.astronomy.data.catalog.AstronomyCatalog
import com.alijafari.red.astronomy.data.catalog.SolarSystemCatalog
import com.alijafari.red.astronomy.domain.*
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.MainViewModel
import com.alijafari.red.astronomy.ui.components.ARSensorCalibrationDialog
import com.alijafari.red.astronomy.ui.components.TimeMachineControlBar
import com.alijafari.red.astronomy.ui.rendering.*
import com.alijafari.red.astronomy.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.drawscope.rotate
import java.util.Locale
import kotlin.math.*


    // F-A5 KIND-B: catalog stars / deep-sky objects store J2000.0 coordinates; precess to the
    // equator of date before the horizontal transform (was: raw J2000 fed against LAST ->
    // ~17.5' sky-position error in 2026, see evidence/ORACLE_CASES.csv). Dynamic objects
    // (sun/moon/planets/satellites) already produce coordinates of date and must NOT be
    // precessed again.
    fun staticObjectEquatorial(obj: CelestialObject, jd: Double): CoordinateEngine.Equatorial {
        val isStaticCatalogObject = when (obj.type) {
            ObjectType.STAR, ObjectType.ASTERISM, ObjectType.DEEP_SKY, ObjectType.GALAXY,
            ObjectType.NEBULA, ObjectType.STAR_CLUSTER, ObjectType.GLOBULAR_CLUSTER,
            ObjectType.BLACK_HOLE -> true
            else -> false
        }
        return if (isStaticCatalogObject) {
            CoordinateEngine.precessJ2000EquatorialToDate(obj.raDeg, obj.decDeg, jd)
        } else {
            CoordinateEngine.Equatorial(obj.raDeg, obj.decDeg)
        }
    }
enum class ArExpandedPanel {
    SEARCH, TIME_MACHINE, FILTERS, SENSORS
}

private fun formatObjectDistance(obj: CelestialObject, isFa: Boolean): String {
    return when (obj.type) {
        ObjectType.SUN -> if (isFa) "۱۴۹٫۶ میلیون کیلومتر" else "149.6 million km (~1 AU)"
        ObjectType.MOON -> {
            if (obj.id == "moon") {
                if (isFa) "۳۸۴,۴۰۰ کیلومتر" else "384,400 km"
            } else {
                if (isFa) "۶۱۲٫۴ میلیون کیلومتر (مدار مشتری)" else "612.4 million km (Jupiter orbit)"
            }
        }
        ObjectType.SATELLITE -> if (isFa) "۴۱۵ کیلومتر" else "415 km"
        ObjectType.PLANET, ObjectType.DWARF_PLANET -> {
            when (obj.id) {
                "planet_mercury" -> if (isFa) "۹۱٫۷ میلیون کیلومتر" else "91.7 million km"
                "planet_venus" -> if (isFa) "۱۰۸٫۲ میلیون کیلومتر" else "108.2 million km"
                "planet_mars" -> if (isFa) "۲۲۵٫۰ میلیون کیلومتر" else "225.0 million km"
                "planet_jupiter" -> if (isFa) "۶۱۲٫۴ میلیون کیلومتر" else "612.4 million km"
                "planet_saturn" -> if (isFa) "۱٫۴۲ میلیارد کیلومتر" else "1.42 billion km"
                "planet_uranus" -> if (isFa) "۲٫۸۷ میلیارد کیلومتر" else "2.87 billion km"
                "planet_neptune" -> if (isFa) "۴٫۵۰ میلیارد کیلومتر" else "4.50 billion km"
                "planet_pluto" -> if (isFa) "۵٫۹ میلیارد کیلومتر" else "5.9 billion km"
                else -> if (isFa) "۶۱۲٫۴ میلیون کیلومتر" else "612.4 million km"
            }
        }
        else -> {
            val ly = obj.distanceLightYears
            if (ly >= 1_000_000.0) {
                val mly = ly / 1_000_000.0
                val str = String.format(Locale.US, "%.2f", mly)
                if (isFa) "${TimeEngine.formatPersianNumbers(str)} میلیون سال نوری" else "$str million light-years"
            } else if (ly >= 1000.0) {
                val kly = String.format(Locale.US, "%.0f", ly)
                if (isFa) "${TimeEngine.formatPersianNumbers(kly)} سال نوری" else "$kly light-years"
            } else {
                val formatted = String.format(Locale.US, "%.1f", ly)
                if (isFa) "${TimeEngine.formatPersianNumbers(formatted)} سال نوری" else "$formatted light-years"
            }
        }
    }
}

@Composable
private fun ArSmartPill(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    isHighlighted: Boolean = false,
    onClick: () -> Unit
) {
    LiquidGlassSurface(
        onClick = onClick,
        shape = CircleShape,
        style = LiquidGlassDefaults.Pill,
        fallbackColor = if (isActive) RedTheme.colors.accentRed else if (isHighlighted) RedTheme.colors.surfaceElevated.copy(alpha = 0.95f) else RedTheme.colors.surfaceElevated.copy(alpha = 0.85f),
        fallbackBorder = BorderStroke(
            1.dp,
            if (isActive) Color.Transparent else if (isHighlighted) RedTheme.colors.accentRed.copy(alpha = 0.6f) else RedTheme.colors.border
        ),
        fallbackShadowElevation = RedElevation.floating,
        modifier = Modifier.height(36.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) Color.White else if (isHighlighted) RedTheme.colors.accentRed else RedTheme.colors.textPrimary,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                style = RedTypographyTokens.caption.copy(
                    fontWeight = if (isActive || isHighlighted) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.sp
                ),
                color = if (isActive) Color.White else RedTheme.colors.textPrimary
            )
        }
    }
}

private data class ArVisibleRenderItem(
    val obj: CelestialObject,
    val horiz: CoordinateEngine.Horizontal,
    val px: Float,
    val py: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompassARScreen(
    uiState: MainUiState,
    viewModel: MainViewModel
) {
    val isFa = uiState.language == AppLanguage.PERSIAN
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val textMeasurer = rememberTextMeasurer()

    // Keep screen awake
    val activity = context as? Activity
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Sensor Fusion & Orientation Provider
    LaunchedEffect(context) {
        ARCalibrationManager.init(context)
    }
    val orientationProvider = remember { OrientationProvider(context) }
    val skyOrientation by orientationProvider.orientation.collectAsState()
    val calibrationState by orientationProvider.calibrationState.collectAsState()
    val arCalibrationOffsets by ARCalibrationManager.calibrationFlow.collectAsState()
    val autoPromptEnabled by ARCalibrationManager.autoPromptEnabledFlow.collectAsState()
    var showCalibrationDialog by remember { mutableStateOf(false) }
    var autoPromptDismissedThisSession by remember { mutableStateOf(false) }
    var showManualSensorPrompt by remember { mutableStateOf(false) }

    // Camera Intrinsics & Geometry Engine (Hardware calibration, sensor size, active array)
    val cameraIntrinsics = remember(context) { ARProjectionEngine.getCameraIntrinsics(context) }
    var previewViewInstance by remember { mutableStateOf<PreviewView?>(null) }

    // Phase 1 Task 3: Camera frame observer (inert, for future plate solving) — new isolated class
    val cameraFrameObserver = remember { CameraFrameObserver() }

    // Ensure background executor is cleaned up when screen leaves composition
    DisposableEffect(cameraFrameObserver) {
        onDispose {
            cameraFrameObserver.shutdown()
        }
    }

    // Phase 1 Task 4 + Task 3.4 + OD6/R3-B4: feed sensor timestamps to frame observer for
    // clock-domain cross-check. Reads the DEDICATED StateFlow<Long> (never conflated away
    // on a stationary device) instead of the equality-excluded SkyOrientation field, whose
    // conflation-starved LaunchedEffect froze this feed (see B4_TIMESTAMP_READERS.md).
    val sensorTimestampNanos by orientationProvider.sensorTimestampNanos.collectAsState()
    LaunchedEffect(sensorTimestampNanos) {
        if (sensorTimestampNanos != 0L) {
            cameraFrameObserver.onSensorTimestamp(sensorTimestampNanos)
        }
    }

    val displayRotationDegrees = remember(context) {
        try {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            val rotation = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                context.display?.rotation ?: Surface.ROTATION_0
            } else {
                @Suppress("DEPRECATION")
                windowManager?.defaultDisplay?.rotation ?: Surface.ROTATION_0
            }
            when (rotation) {
                Surface.ROTATION_90 -> 90
                Surface.ROTATION_180 -> 180
                Surface.ROTATION_270 -> 270
                else -> 0
            }
        } catch (e: Exception) {
            0
        }
    }

    // Camera & Location State
    var isCameraEnabled by remember { mutableStateOf(true) }
    var isGpsActive by remember { mutableStateOf(true) }
    var gpsAccuracyMeters by remember { mutableStateOf<Float?>(null) }
    var isSensorActive by remember { mutableStateOf(true) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            isCameraEnabled = true
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        val isGranted = fine || coarse
        hasLocationPermission = isGranted
        if (isGranted) {
            isGpsActive = true
        }
    }

    // Live GPS Listener for maximum precision and real sky alignment
    DisposableEffect(isGpsActive, hasLocationPermission) {
        if (isGpsActive && hasLocationPermission) {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    val lat = location.latitude
                    val lon = location.longitude
                    val alt = location.altitude
                    gpsAccuracyMeters = if (location.hasAccuracy()) location.accuracy else null

                    viewModel.setLocation(
                        cityEn = "Live GPS",
                        cityFa = "GPS زنده",
                        lat = lat,
                        lon = lon
                    )

                    // Pass exact location + altitude to orientation provider for magnetic declination correction
                    orientationProvider.updateLocation(lat, lon, alt)
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            try {
                // Last known location for instant startup
                val lastGps = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                lastGps?.let { loc ->
                    gpsAccuracyMeters = if (loc.hasAccuracy()) loc.accuracy else null
                    viewModel.setLocation("Live GPS", "GPS زنده", loc.latitude, loc.longitude)
                    orientationProvider.updateLocation(loc.latitude, loc.longitude, loc.altitude)
                }

                if (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        1000L,
                        1.0f,
                        locationListener
                    )
                } else if (locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true) {
                    locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        1000L,
                        1.0f,
                        locationListener
                    )
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            onDispose {
                try {
                    locationManager?.removeUpdates(locationListener)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            onDispose { }
        }
    }

    // Manual Drag Fallback when sensors paused
    var manualAzimuthOffset by remember { mutableStateOf(0.0) }
    var manualPitchOffset by remember { mutableStateOf(0.0) }

    // Z-V3 (2026-09-04): magnetic declination is NOT applied here. It is already applied
    // at the attitude SOURCE: OrientationProvider.updateLocation() (called from the GPS
    // listeners above) sets the WMM declination and the provider rotates the sensor world
    // frame by it (R_true = R_declination * R_sensor) BEFORE exposing both the rotation
    // matrix and the scalar azimuth — so skyOrientation.azimuth is already TRUE-north.
    // The final-pass B1 scalar correction here double-corrected every scalar consumer
    // (hit-test dAz, FinderEngine, horizon loop, arrows) by +D when GPS was active, and
    // its legacy-yaw rebase would have corrupted stored calibrations. Both reverted.
    // See evidence/V3_DECLINATION_PLACEMENT_2026-09-04.md.
    val currentAzimuth = if (isSensorActive) skyOrientation.azimuth.toDouble() else manualAzimuthOffset
    val currentAltitude = if (isSensorActive) skyOrientation.pitch.toDouble() else manualPitchOffset

    DisposableEffect(uiState.userLocation, isSensorActive) {
        orientationProvider.updateLocation(
            latitude = uiState.userLocation.latitude,
            longitude = uiState.userLocation.longitude
        )
        if (isSensorActive) {
            orientationProvider.start()
        } else {
            orientationProvider.stop()
        }
        onDispose {
            orientationProvider.stop()
        }
    }

    // Time Machine State & Ticking Handlers
    val timeMachineState = uiState.timeMachineState
    val coroutineScope = rememberCoroutineScope()

    // Smooth playback ticking loop when simulation mode is active and playing
    LaunchedEffect(timeMachineState.mode, timeMachineState.isPlaying, timeMachineState.speed, timeMachineState.isReverse) {
        if (timeMachineState.mode == TimeMachineMode.SIMULATION && timeMachineState.isPlaying) {
            var lastFrameNanos = 0L
            var currentSimMs = timeMachineState.simulationTimeMs
            while (true) {
                withFrameNanos { frameTimeNanos ->
                    if (lastFrameNanos == 0L) {
                        lastFrameNanos = frameTimeNanos
                    } else {
                        val deltaRealSec = (frameTimeNanos - lastFrameNanos) / 1_000_000_000.0
                        lastFrameNanos = frameTimeNanos

                        val multiplier = timeMachineState.speed.multiplier
                        val direction = if (timeMachineState.isReverse) -1.0 else 1.0
                        val deltaSimMs = (deltaRealSec * multiplier * direction * 1000.0).toLong()

                        if (deltaSimMs != 0L) {
                            currentSimMs = (currentSimMs + deltaSimMs).coerceIn(
                                TimeMachineState.MIN_TIMESTAMP_MS,
                                TimeMachineState.MAX_TIMESTAMP_MS
                            )
                            viewModel.setSimulatedTime(currentSimMs, timeMachineState.eventName, timeMachineState.isBirthdayMode)
                        }
                    }
                }
            }
        }
    }

    // Ticking loop when in LIVE mode to keep real-time sky moving
    var liveTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(timeMachineState.mode) {
        if (timeMachineState.mode == TimeMachineMode.LIVE) {
            while (true) {
                liveTimeMs = System.currentTimeMillis()
                delay(1000L)
            }
        }
    }

    // Coroutine for smooth ~1s animated transition back to LIVE mode
    val handleReturnToLive = {
        coroutineScope.launch {
            val startSimMs = timeMachineState.simulationTimeMs
            val targetLiveMs = System.currentTimeMillis()
            val animDurationMs = 1000L
            val startTime = System.currentTimeMillis()

            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed >= animDurationMs) {
                    viewModel.returnToLiveTime()
                    break
                }
                val fraction = elapsed.toFloat() / animDurationMs.toFloat()
                val easedFraction = (1.0 - cos(fraction * PI)) / 2.0
                val currentSim = (startSimMs + (targetLiveMs - startSimMs) * easedFraction).toLong()
                viewModel.setSimulatedTime(currentSim, timeMachineState.eventName, timeMachineState.isBirthdayMode)
                delay(16L)
            }
        }
    }

    val activeTimeMs = if (timeMachineState.mode == TimeMachineMode.LIVE) liveTimeMs else timeMachineState.simulationTimeMs

    // Astronomy Calculations
    val jd = remember(activeTimeMs) { TimeEngine.getJulianDate(activeTimeMs) }
    val lastDeg = remember(uiState.userLocation, jd) {
        TimeEngine.getLAST(jd, uiState.userLocation.longitude)
    }

    val sunHoriz = remember(jd, uiState.userLocation) {
        SunEngine.getSunAltAz(jd, uiState.userLocation.latitude, uiState.userLocation.longitude)
    }

    val moonData = remember(jd, uiState.userLocation) {
        MoonEngine.calculateMoon(
            jd = jd,
            latitude = uiState.userLocation.latitude,
            longitude = uiState.userLocation.longitude,
            elevationM = uiState.userLocation.elevationMeters
        )
    }
    val moonHoriz = remember(moonData) {
        CoordinateEngine.Horizontal(
            azimuthDeg = moonData.azimuthDeg,
            altitudeDeg = moonData.altitudeDeg
        )
    }

    // Live TLE background fetch for ISS orbital precision
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            ISSEngine.fetchLatestTLE()
        }
    }

    // Real-Time Satellite Topocentric Positions for all catalog satellites
    val satellitePositions = remember(activeTimeMs, uiState.userLocation) {
        SatelliteEngine.calculateAllSatellitePositions(
            timestampMs = activeTimeMs,
            userLatDeg = uiState.userLocation.latitude,
            userLonDeg = uiState.userLocation.longitude,
            userAltMeters = uiState.userLocation.elevationMeters
        )
    }

    val allCatalog = remember(jd) { AstronomyCatalog.getAllObjects(jd) }

    // Search and Finder State
    var searchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }
    var selectedTarget by remember { mutableStateOf<CelestialObject?>(null) }
    var showArrivalDialog by remember { mutableStateOf(false) }
    var hasVibratedForArrival by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.selectedTargetObject) {
        uiState.selectedTargetObject?.let { target ->
            selectedTarget = target
            hasVibratedForArrival = false
        }
    }

    val searchResults = remember(searchQuery, uiState.userLocation, jd) {
        if (searchQuery.isNotBlank()) {
            CelestialSearchEngine.search(
                query = searchQuery,
                userLat = uiState.userLocation.latitude,
                userLon = uiState.userLocation.longitude,
                jd = jd
            )
        } else {
            emptyList()
        }
    }

    // Finder Guidance Calculations
    val finderData = remember(selectedTarget, currentAzimuth, currentAltitude, uiState.userLocation, jd) {
        selectedTarget?.let { target ->
            FinderEngine.calculateFinderData(
                target = target,
                phoneAzimuthDeg = currentAzimuth,
                phoneAltitudeDeg = currentAltitude,
                userLat = uiState.userLocation.latitude,
                userLon = uiState.userLocation.longitude,
                userAltMeters = uiState.userLocation.elevationMeters,
                jd = jd
            )
        }
    }

    // Handle Arrival Celebration & Vibration
    LaunchedEffect(finderData?.isArrived) {
        if (finderData?.isArrived == true && !hasVibratedForArrival) {
            hasVibratedForArrival = true
            showArrivalDialog = true

            // Trigger Vibration
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vibratorManager?.defaultVibrator?.vibrate(
                        VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(300)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else if (finderData?.isArrived == false) {
            hasVibratedForArrival = false
        }
    }

    // Focus Mode Smart Panel State
    var activeExpandedPanel by remember { mutableStateOf<ArExpandedPanel?>(null) }

    // AR Object Visibility Filters Persistence
    val prefs = remember { context.getSharedPreferences("ar_filters_prefs", Context.MODE_PRIVATE) }
    var filterStars by remember { mutableStateOf(prefs.getBoolean("filter_stars", true)) }
    var filterConstellations by remember { mutableStateOf(prefs.getBoolean("filter_constellations", true)) }
    var filterPlanets by remember { mutableStateOf(prefs.getBoolean("filter_planets", true)) }
    var filterMoons by remember { mutableStateOf(prefs.getBoolean("filter_moons", true)) }
    var filterSun by remember { mutableStateOf(prefs.getBoolean("filter_sun", true)) }
    var filterDeepSky by remember { mutableStateOf(prefs.getBoolean("filter_deepsky", true)) }
    var filterSatellites by remember { mutableStateOf(prefs.getBoolean("filter_satellites", true)) }
    var filterObjectNames by remember { mutableStateOf(prefs.getBoolean("filter_object_names", false)) }

    fun updateFilter(key: String, value: Boolean, setter: (Boolean) -> Unit) {
        setter(value)
        prefs.edit().putBoolean(key, value).apply()
    }

    fun setAllFilters(value: Boolean) {
        filterStars = value
        filterConstellations = value
        filterPlanets = value
        filterMoons = value
        filterSun = value
        filterDeepSky = value
        filterSatellites = value
        filterObjectNames = value
        prefs.edit()
            .putBoolean("filter_stars", value)
            .putBoolean("filter_constellations", value)
            .putBoolean("filter_planets", value)
            .putBoolean("filter_moons", value)
            .putBoolean("filter_sun", value)
            .putBoolean("filter_deepsky", value)
            .putBoolean("filter_satellites", value)
            .putBoolean("filter_object_names", value)
            .apply()
    }

    // AR Sky Zoom State
    var zoomFactor by remember { mutableFloatStateOf(1.0f) }
    var showZoomIndicator by remember { mutableStateOf(false) }

    LaunchedEffect(zoomFactor) {
        if (zoomFactor != 1.0f) {
            showZoomIndicator = true
            delay(1500L)
            showZoomIndicator = false
        }
    }

    // Dynamic state wrappers for gesture detector inside pointerInput(Unit)
    val curAzimuthState by rememberUpdatedState(currentAzimuth)
    val curAltitudeState by rememberUpdatedState(currentAltitude)
    val zoomFactorState by rememberUpdatedState(zoomFactor)
    val skyOrientationState by rememberUpdatedState(skyOrientation)
    val currentDensity = LocalDensity.current.density
    val densityState by rememberUpdatedState(currentDensity)
    val filterStarsState by rememberUpdatedState(filterStars)
    val filterSunState by rememberUpdatedState(filterSun)
    val filterMoonsState by rememberUpdatedState(filterMoons)
    val filterPlanetsState by rememberUpdatedState(filterPlanets)
    val filterSatellitesState by rememberUpdatedState(filterSatellites)
    val filterDeepSkyState by rememberUpdatedState(filterDeepSky)
    val sunHorizState by rememberUpdatedState(sunHoriz)
    val moonHorizState by rememberUpdatedState(moonHoriz)
    val satellitePositionsState by rememberUpdatedState(satellitePositions)
    val lastDegState by rememberUpdatedState(lastDeg)
    val userLatState by rememberUpdatedState(uiState.userLocation.latitude)
    val allCatalogState by rememberUpdatedState(allCatalog)
    val isSensorActiveState by rememberUpdatedState(isSensorActive)

    // AR Info Card Object State & Auto-Dismiss Timer
    var longPressObject by remember { mutableStateOf<CelestialObject?>(null) }

    LaunchedEffect(longPressObject) {
        if (longPressObject != null) {
            delay(6000L)
            longPressObject = null
        }
    }

    // Active Orbit Object (Target or Long-pressed Object)
    val activeOrbitObject = selectedTarget ?: longPressObject

    // Real-Time Trajectory Points for activeOrbitObject (hidden by default, shown when target or long-press card active)
    val (orbitPastPoints, orbitFuturePoints) = remember(
        activeOrbitObject?.id,
        activeTimeMs,
        uiState.userLocation
    ) {
        val targetObj = activeOrbitObject ?: return@remember Pair(emptyList<CoordinateEngine.Horizontal>(), emptyList<CoordinateEngine.Horizontal>())
        val pastList = mutableListOf<CoordinateEngine.Horizontal>()
        val futureList = mutableListOf<CoordinateEngine.Horizontal>()

        if (targetObj.type == ObjectType.SATELLITE || targetObj.id.startsWith("sat_")) {
            val satItem = SatelliteEngine.resolveSatelliteItem(targetObj.id)
            if (satItem != null) {
                val (past, future) = SatelliteEngine.generateOrbitTrajectory(
                    satellite = satItem,
                    currentTimestampMs = activeTimeMs,
                    userLatDeg = uiState.userLocation.latitude,
                    userLonDeg = uiState.userLocation.longitude,
                    userAltMeters = uiState.userLocation.elevationMeters,
                    pastMinutes = 45,
                    futureMinutes = 45,
                    stepSeconds = 30
                )
                pastList.addAll(past)
                futureList.addAll(future)
            }
        } else if (targetObj.id == "moon") {
            val activeJd = TimeEngine.getJulianDate(activeTimeMs)
            for (step in -48..0) {
                val hJd = activeJd + (step * 0.25 / 24.0)
                val hLast = TimeEngine.getLAST(hJd, uiState.userLocation.longitude)
                val hMoon = MoonEngine.calculateMoon(hJd)
                val hHoriz = CoordinateEngine.equatorialToHorizontal(
                    CoordinateEngine.Equatorial(hMoon.raDeg, hMoon.decDeg),
                    hLast,
                    uiState.userLocation.latitude
                )
                pastList.add(hHoriz)
            }
            for (step in 0..48) {
                val hJd = activeJd + (step * 0.25 / 24.0)
                val hLast = TimeEngine.getLAST(hJd, uiState.userLocation.longitude)
                val hMoon = MoonEngine.calculateMoon(hJd)
                val hHoriz = CoordinateEngine.equatorialToHorizontal(
                    CoordinateEngine.Equatorial(hMoon.raDeg, hMoon.decDeg),
                    hLast,
                    uiState.userLocation.latitude
                )
                futureList.add(hHoriz)
            }
        } else if (targetObj.id.startsWith("galilean_moon_")) {
            val activeJd = TimeEngine.getJulianDate(activeTimeMs)
            for (step in -48..0) {
                val hJd = activeJd + (step * 0.25 / 24.0)
                val hLast = TimeEngine.getLAST(hJd, uiState.userLocation.longitude)
                val moons = SolarSystemCatalog.getGalileanMoons(hJd)
                val mObj = moons.firstOrNull { it.id == targetObj.id } ?: targetObj
                val hHoriz = CoordinateEngine.equatorialToHorizontal(
                    CoordinateEngine.Equatorial(mObj.raDeg, mObj.decDeg),
                    hLast,
                    uiState.userLocation.latitude
                )
                pastList.add(hHoriz)
            }
            for (step in 0..48) {
                val hJd = activeJd + (step * 0.25 / 24.0)
                val hLast = TimeEngine.getLAST(hJd, uiState.userLocation.longitude)
                val moons = SolarSystemCatalog.getGalileanMoons(hJd)
                val mObj = moons.firstOrNull { it.id == targetObj.id } ?: targetObj
                val hHoriz = CoordinateEngine.equatorialToHorizontal(
                    CoordinateEngine.Equatorial(mObj.raDeg, mObj.decDeg),
                    hLast,
                    uiState.userLocation.latitude
                )
                futureList.add(hHoriz)
            }
        } else if (targetObj.type == ObjectType.SUN) {
            val activeJd = TimeEngine.getJulianDate(activeTimeMs)
            for (step in -48..0) {
                val hJd = activeJd + (step * 0.25 / 24.0)
                val hHoriz = SunEngine.getSunAltAz(hJd, uiState.userLocation.latitude, uiState.userLocation.longitude)
                pastList.add(hHoriz)
            }
            for (step in 0..48) {
                val hJd = activeJd + (step * 0.25 / 24.0)
                val hHoriz = SunEngine.getSunAltAz(hJd, uiState.userLocation.latitude, uiState.userLocation.longitude)
                futureList.add(hHoriz)
            }
        } else if (targetObj.type != ObjectType.REFERENCE_POINT) {
            val activeJd = TimeEngine.getJulianDate(activeTimeMs)
            for (step in -48..0) {
                val hJd = activeJd + (step * 0.25 / 24.0)
                val hLast = TimeEngine.getLAST(hJd, uiState.userLocation.longitude)
                val hHoriz = CoordinateEngine.equatorialToHorizontal(
                    staticObjectEquatorial(targetObj, hJd),
                    hLast,
                    uiState.userLocation.latitude
                )
                pastList.add(hHoriz)
            }
            for (step in 0..48) {
                val hJd = activeJd + (step * 0.25 / 24.0)
                val hLast = TimeEngine.getLAST(hJd, uiState.userLocation.longitude)
                val hMoon = CoordinateEngine.equatorialToHorizontal(
                    staticObjectEquatorial(targetObj, hJd),
                    hLast,
                    uiState.userLocation.latitude
                )
                futureList.add(hMoon)
            }
        }

        Pair(pastList, futureList)
    }

    // Auto-Hide AR UI State & 5-second Inactivity Timer
    var isControlsVisible by remember { mutableStateOf(true) }
    var lastInteractionTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val resetControlsTimer = remember {
        {
            lastInteractionTimeMs = System.currentTimeMillis()
            if (!isControlsVisible) {
                isControlsVisible = true
            }
        }
    }

    LaunchedEffect(lastInteractionTimeMs, activeExpandedPanel, searchQuery, selectedTarget, longPressObject) {
        if (activeExpandedPanel != null || isSearchFocused || searchQuery.isNotEmpty() || longPressObject != null) {
            isControlsVisible = true
            return@LaunchedEffect
        }
        delay(5000L)
        isControlsVisible = false
    }

    val controlsAlpha by animateFloatAsState(
        targetValue = if (isControlsVisible) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 400),
        label = "ControlsAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF060810))
            // D2: long-press opens the debug-only diagnostics overlay (debug builds only;
            // release compiles this branch to `Modifier`, a no-op).
            .then(
                if (BuildConfig.DEBUG) Modifier.pointerInput("st-debug-overlay") {
                    detectTapGestures(onLongPress = {
                        StarTrackerDebugHost.open(context, orientationProvider)
                    })
                } else Modifier
            )
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    if (zoom != 1.0f) {
                        zoomFactor = (zoomFactor * zoom).coerceIn(0.75f, 4.0f)
                    }
                }
            }
            .pointerInput(isSensorActive) {
                if (!isSensorActive) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        manualAzimuthOffset = (manualAzimuthOffset - dragAmount.x * 0.2) % 360.0
                        if (manualAzimuthOffset < 0) manualAzimuthOffset += 360.0
                        manualPitchOffset = (manualPitchOffset + dragAmount.y * 0.2).coerceIn(-90.0, 90.0)
                    }
                }
            }
            .testTag("compass_ar_screen")
    ) {
        // Layer 1: Live Camera Feed
        if (hasCameraPermission && isCameraEnabled) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    previewViewInstance = previewView
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            cameraProvider.unbindAll()
                            // Phase 1 Task 3 + R2-A1 gate: Preview is always bound; the
                            // ImageAnalysis use case (star-tracker frame feed) is bound ONLY
                            // when the star tracker master flag is enabled. Flag choice:
                            // REUSE StarTrackerConfig.ENABLED - it is the documented master
                            // switch whose safety contract is "flag OFF = zero behavioral
                            // difference vs pre-project"; a sibling flag would be a second
                            // switch to keep consistent for no benefit. With the flag off
                            // (default, compile-time const false, dead-code eliminated) the
                            // binding is exactly the pre-project call:
                            //   bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                            // G-P0: runtime-resolved gate (debug field trial can enable the
                            // analysis feed live); release resolves the consts (false) and
                            // binds exactly the pre-project call.
                            if (com.alijafari.red.astronomy.startracker.fusion.StarTrackerDebugFlags.runtime().enabled) {
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    cameraFrameObserver.getUseCase()
                                )
                            } else {
                                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Layer 2: AR Celestial Overlay Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            resetControlsTimer()
                            tryAwaitRelease()
                        },
                        onTap = { touchOffset ->
                            resetControlsTimer()
                            if (activeExpandedPanel != null) {
                                activeExpandedPanel = null
                            }

                            val activeAzimuth = curAzimuthState
                            val activeAltitude = curAltitudeState
                            val activeZoom = zoomFactorState
                            val activeOrientation = skyOrientationState
                            val activeDensity = densityState
                            val activeCatalog = allCatalogState
                            val activeSunHoriz = sunHorizState
                            val activeMoonHoriz = moonHorizState
                            val activeSatellitePositions = satellitePositionsState
                            val activeLastDeg = lastDegState
                            val activeUserLat = userLatState
                            val activeIsSensor = isSensorActiveState

                            val canvasWidth = size.width.toFloat()
                            val canvasHeight = size.height.toFloat()

                            var bestMatch: CelestialObject? = null
                            var bestDistPx = 70.0f * activeDensity

                            for (obj in activeCatalog) {
                                val isVisibleByFilter = when (obj.type) {
                                    ObjectType.STAR, ObjectType.ASTERISM -> filterStarsState
                                    ObjectType.SUN -> filterSunState
                                    ObjectType.MOON -> filterMoonsState
                                    ObjectType.PLANET, ObjectType.DWARF_PLANET -> filterPlanetsState
                                    ObjectType.SATELLITE -> filterSatellitesState
                                    ObjectType.DEEP_SKY, ObjectType.GALAXY, ObjectType.NEBULA,
                                    ObjectType.STAR_CLUSTER, ObjectType.GLOBULAR_CLUSTER, ObjectType.BLACK_HOLE -> filterDeepSkyState
                                    else -> true
                                }
                                if (!isVisibleByFilter) continue

                                val horiz = if (obj.type == ObjectType.SUN) activeSunHoriz
                                else if (obj.id == "moon") activeMoonHoriz
                                else if (obj.type == ObjectType.SATELLITE || obj.id.startsWith("sat_")) activeSatellitePositions[obj.id] ?: activeSatellitePositions[com.alijafari.red.astronomy.data.catalog.CanonicalAstroCatalog.resolveCanonicalId(obj.id)] ?: CoordinateEngine.Horizontal(-90.0, 0.0)
                                else CoordinateEngine.equatorialToHorizontal(staticObjectEquatorial(obj, jd), activeLastDeg, activeUserLat)

                                val pt = ARProjectionEngine.projectAltAz(
                                    azimuthDeg = horiz.azimuthDeg,
                                    altitudeDeg = horiz.altitudeDeg,
                                    rotationMatrix = if (activeIsSensor) activeOrientation.rotationMatrix else null,
                                    currentAzimuth = activeAzimuth,
                                    currentAltitude = activeAltitude,
                                    currentRoll = activeOrientation.roll.toDouble(),
                                    canvasWidth = canvasWidth,
                                    canvasHeight = canvasHeight,
                                    intrinsics = cameraIntrinsics,
                                    zoomFactor = activeZoom,
                                    sensorToViewMatrix = previewViewInstance?.sensorToViewTransform,
                                    displayRotationDegrees = displayRotationDegrees
                                ) ?: continue

                                val dist = hypot(touchOffset.x - pt.x, touchOffset.y - pt.y)
                                if (dist < bestDistPx) {
                                    bestDistPx = dist
                                    bestMatch = obj
                                }
                            }

                            longPressObject = bestMatch
                        }
                    )
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            if (canvasWidth <= 0f || canvasHeight <= 0f) return@Canvas

            val centerX = canvasWidth / 2f
            val centerY = canvasHeight / 2f

            val fovX = ARProjectionEngine.computeEffectiveFovXDeg(
                screenWidthPx = canvasWidth,
                screenHeightPx = canvasHeight,
                intrinsics = cameraIntrinsics,
                zoomFactor = zoomFactor
            ).coerceAtLeast(1.0)
            val pixelsPerDegree = canvasWidth / fovX

            // Starry night background if camera disabled
            if (!hasCameraPermission || !isCameraEnabled) {
                val rand = java.util.Random(1337)
                for (i in 0..200) {
                    val sx = rand.nextFloat() * canvasWidth
                    val sy = rand.nextFloat() * canvasHeight
                    val radius = rand.nextFloat() * 2.5f + 0.5f
                    drawCircle(
                        color = Color.White.copy(alpha = rand.nextFloat() * 0.7f + 0.2f),
                        radius = radius,
                        center = Offset(sx, sy)
                    )
                }
            }

            // Draw Horizon Line
            val horizonPath = Path()
            var horizonFirst = true
            val horizonSteps = 24
            val horizonSpan = fovX * 2.0
            for (i in 0..horizonSteps) {
                val hAz = currentAzimuth - (horizonSpan / 2.0) + (i * horizonSpan / horizonSteps)
                val pt = ARProjectionEngine.projectAltAz(
                    azimuthDeg = (hAz % 360.0 + 360.0) % 360.0,
                    altitudeDeg = 0.0,
                    rotationMatrix = if (isSensorActive) skyOrientation.rotationMatrix else null,
                    currentAzimuth = currentAzimuth,
                    currentAltitude = currentAltitude,
                    currentRoll = skyOrientation.roll.toDouble(),
                    canvasWidth = canvasWidth,
                    canvasHeight = canvasHeight,
                    intrinsics = cameraIntrinsics,
                    zoomFactor = zoomFactor,
                    sensorToViewMatrix = previewViewInstance?.sensorToViewTransform,
                    displayRotationDegrees = displayRotationDegrees
                ) ?: continue
                if (horizonFirst) {
                    horizonPath.moveTo(pt.x, pt.y)
                    horizonFirst = false
                } else {
                    horizonPath.lineTo(pt.x, pt.y)
                }
            }
            if (!horizonFirst) {
                drawPath(
                    path = horizonPath,
                    color = AccentPrimary.copy(alpha = 0.6f),
                    style = Stroke(width = 2.5f)
                )
                val horizonCenterPt = ARProjectionEngine.projectAltAz(
                    azimuthDeg = currentAzimuth,
                    altitudeDeg = 0.0,
                    rotationMatrix = if (isSensorActive) skyOrientation.rotationMatrix else null,
                    currentAzimuth = currentAzimuth,
                    currentAltitude = currentAltitude,
                    currentRoll = skyOrientation.roll.toDouble(),
                    canvasWidth = canvasWidth,
                    canvasHeight = canvasHeight,
                    intrinsics = cameraIntrinsics,
                    zoomFactor = zoomFactor,
                    sensorToViewMatrix = previewViewInstance?.sensorToViewTransform,
                    displayRotationDegrees = displayRotationDegrees
                )
                if (horizonCenterPt != null && horizonCenterPt.x in 0f..canvasWidth && horizonCenterPt.y in 0f..canvasHeight) {
                    val horizonLabel = if (isFa) "افق (0°)" else "Horizon (0°)"
                    val horizonTextLayout = textMeasurer.measure(
                        text = horizonLabel,
                        style = TextStyle(color = AccentPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        softWrap = false
                    )
                    val drawX = 20f
                    val drawY = horizonCenterPt.y - 30f
                    if (drawX.isFinite() && drawY.isFinite() && drawY in 0f..canvasHeight) {
                        drawText(
                            textLayoutResult = horizonTextLayout,
                            topLeft = Offset(drawX, drawY)
                        )
                    }
                }
            }

            // Render Orbit Trajectory Line ONLY when an object is active (Target or Long-press Card)
            if (activeOrbitObject != null) {
                // Solid line = orbital portion behind the object (past)
                if (orbitPastPoints.size >= 2) {
                    val pastPath = Path()
                    var pastFirst = true
                    for (pt in orbitPastPoints) {
                        val projPt = ARProjectionEngine.projectAltAz(
                            azimuthDeg = pt.azimuthDeg,
                            altitudeDeg = pt.altitudeDeg,
                            rotationMatrix = if (isSensorActive) skyOrientation.rotationMatrix else null,
                            currentAzimuth = currentAzimuth,
                            currentAltitude = currentAltitude,
                            currentRoll = skyOrientation.roll.toDouble(),
                            canvasWidth = canvasWidth,
                            canvasHeight = canvasHeight,
                            intrinsics = cameraIntrinsics,
                            zoomFactor = zoomFactor,
                            sensorToViewMatrix = previewViewInstance?.sensorToViewTransform,
                            displayRotationDegrees = displayRotationDegrees
                        ) ?: continue

                        if (projPt.x in -600f..(canvasWidth + 600f) && projPt.y in -600f..(canvasHeight + 600f)) {
                            if (pastFirst) {
                                pastPath.moveTo(projPt.x, projPt.y)
                                pastFirst = false
                            } else {
                                pastPath.lineTo(projPt.x, projPt.y)
                            }
                        }
                    }
                    if (!pastFirst) {
                        drawPath(
                            path = pastPath,
                            color = Color(0xFF38BDF8).copy(alpha = 0.85f),
                            style = Stroke(width = 2.5f * density)
                        )
                    }
                }

                // Dotted line = orbital portion in front of the object / yet to be travelled (future)
                if (orbitFuturePoints.size >= 2) {
                    val futurePath = Path()
                    var futureFirst = true
                    for (pt in orbitFuturePoints) {
                        val projPt = ARProjectionEngine.projectAltAz(
                            azimuthDeg = pt.azimuthDeg,
                            altitudeDeg = pt.altitudeDeg,
                            rotationMatrix = if (isSensorActive) skyOrientation.rotationMatrix else null,
                            currentAzimuth = currentAzimuth,
                            currentAltitude = currentAltitude,
                            currentRoll = skyOrientation.roll.toDouble(),
                            canvasWidth = canvasWidth,
                            canvasHeight = canvasHeight,
                            intrinsics = cameraIntrinsics,
                            zoomFactor = zoomFactor,
                            sensorToViewMatrix = previewViewInstance?.sensorToViewTransform,
                            displayRotationDegrees = displayRotationDegrees
                        ) ?: continue

                        if (projPt.x in -600f..(canvasWidth + 600f) && projPt.y in -600f..(canvasHeight + 600f)) {
                            if (futureFirst) {
                                futurePath.moveTo(projPt.x, projPt.y)
                                futureFirst = false
                            } else {
                                futurePath.lineTo(projPt.x, projPt.y)
                            }
                        }
                    }
                    if (!futureFirst) {
                        drawPath(
                            path = futurePath,
                            color = Color(0xFF38BDF8).copy(alpha = 0.85f),
                            style = Stroke(
                                width = 2.5f * density,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f * density, 8f * density), 0f)
                            )
                        )
                    }
                }
            }

            // Render Galactic Equator Plane / Constellation lines (if Constellations filter enabled)
            if (filterConstellations) {
                val galPoints = GalacticEngine.calculateGalacticPlanePoints(
                    jd = jd,
                    userLatDeg = uiState.userLocation.latitude,
                    userLonDeg = uiState.userLocation.longitude
                )
                val galPath = Path()
                var galFirst = true
                for (gp in galPoints) {
                    val projPt = ARProjectionEngine.projectAltAz(
                        azimuthDeg = gp.azimuthDeg,
                        altitudeDeg = gp.altitudeDeg,
                        rotationMatrix = if (isSensorActive) skyOrientation.rotationMatrix else null,
                        currentAzimuth = currentAzimuth,
                        currentAltitude = currentAltitude,
                        currentRoll = skyOrientation.roll.toDouble(),
                        canvasWidth = canvasWidth,
                        canvasHeight = canvasHeight,
                        intrinsics = cameraIntrinsics,
                        zoomFactor = zoomFactor,
                        sensorToViewMatrix = previewViewInstance?.sensorToViewTransform,
                        displayRotationDegrees = displayRotationDegrees
                    ) ?: continue

                    if (galFirst) {
                        galPath.moveTo(projPt.x, projPt.y)
                        galFirst = false
                    } else {
                        galPath.lineTo(projPt.x, projPt.y)
                    }
                }
                if (!galFirst) {
                    drawPath(
                        path = galPath,
                        color = Color(0xFFC084FC).copy(alpha = 0.4f),
                        style = Stroke(width = 2.5f)
                    )
                }
            }

            // 2. Project All Visible Catalog Objects & Identify Aimed Object at Center Reticle
            val visibleRenderItems = mutableListOf<ArVisibleRenderItem>()
            var closestReticleObj: CelestialObject? = null
            var minReticleDist = 70f * density

            for (obj in allCatalog) {
                val isVisibleByFilter = when (obj.type) {
                    ObjectType.STAR, ObjectType.ASTERISM -> filterStars
                    ObjectType.SUN -> filterSun
                    ObjectType.MOON -> filterMoons
                    ObjectType.PLANET, ObjectType.DWARF_PLANET -> filterPlanets
                    ObjectType.SATELLITE -> filterSatellites
                    ObjectType.DEEP_SKY, ObjectType.GALAXY, ObjectType.NEBULA,
                    ObjectType.STAR_CLUSTER, ObjectType.GLOBULAR_CLUSTER, ObjectType.BLACK_HOLE -> filterDeepSky
                    else -> true
                }
                if (!isVisibleByFilter) continue

                val horiz = if (obj.type == ObjectType.SUN) {
                    sunHoriz
                } else if (obj.id == "moon") {
                    moonHoriz
                } else if (obj.type == ObjectType.SATELLITE || obj.id.startsWith("sat_")) {
                    satellitePositions[obj.id] ?: satellitePositions[com.alijafari.red.astronomy.data.catalog.CanonicalAstroCatalog.resolveCanonicalId(obj.id)] ?: CoordinateEngine.Horizontal(-90.0, 0.0)
                } else {
                    CoordinateEngine.equatorialToHorizontal(
                        CoordinateEngine.Equatorial(obj.raDeg, obj.decDeg),
                        lastDeg,
                        uiState.userLocation.latitude
                    )
                }

                val projPt = ARProjectionEngine.projectAltAz(
                    azimuthDeg = horiz.azimuthDeg,
                    altitudeDeg = horiz.altitudeDeg,
                    rotationMatrix = if (isSensorActive) skyOrientation.rotationMatrix else null,
                    currentAzimuth = currentAzimuth,
                    currentAltitude = currentAltitude,
                    currentRoll = skyOrientation.roll.toDouble(),
                    canvasWidth = canvasWidth,
                    canvasHeight = canvasHeight,
                    intrinsics = cameraIntrinsics,
                    zoomFactor = zoomFactor,
                    sensorToViewMatrix = previewViewInstance?.sensorToViewTransform,
                    displayRotationDegrees = displayRotationDegrees
                ) ?: continue

                val px = projPt.x
                val py = projPt.y

                // Padding boundary check
                if (px in -150f..(canvasWidth + 150f) && py in -150f..(canvasHeight + 150f)) {
                    val distToCenter = hypot(px - centerX, py - centerY)
                    if (distToCenter < minReticleDist) {
                        minReticleDist = distToCenter
                        closestReticleObj = obj
                    }
                    visibleRenderItems.add(ArVisibleRenderItem(obj, horiz, px, py))
                }
            }

            for (item in visibleRenderItems) {
                val obj = item.obj
                val horiz = item.horiz
                val px = item.px
                val py = item.py

                val isSelected = obj.id == selectedTarget?.id || (selectedTarget != null && com.alijafari.red.astronomy.data.catalog.CanonicalAstroCatalog.resolveCanonicalId(obj.id) == com.alijafari.red.astronomy.data.catalog.CanonicalAstroCatalog.resolveCanonicalId(selectedTarget?.id ?: ""))
                val isAimed = (closestReticleObj != null && obj.id == closestReticleObj.id)

                var dAz = horiz.azimuthDeg - currentAzimuth
                if (dAz > 180) dAz -= 360
                if (dAz < -180) dAz += 360
                val dAlt = horiz.altitudeDeg - currentAltitude

                // Calculate Center Proximity Scale (1.0x to 2.5x)
                val proximityScale = CelestialObjectSizes.calculateProximityScale(
                    deltaAzDeg = dAz.toFloat(),
                    deltaAltDeg = dAlt.toFloat()
                )

                val baseSizeDp = CelestialObjectSizes.getBaseSizeDp(obj)
                val baseRadiusPx = (baseSizeDp / 2f) * density
                val radiusPx = baseRadiusPx * proximityScale

                when (obj.type) {
                    ObjectType.SUN -> {
                        // Solar Corona & Disk
                        drawCircle(
                            color = Color(0xFFFF8800).copy(alpha = 0.2f),
                            radius = radiusPx * 2.2f,
                            center = Offset(px, py)
                        )
                        drawCircle(
                            color = Color(0xFFFFCC00).copy(alpha = 0.45f),
                            radius = radiusPx * 1.5f,
                            center = Offset(px, py)
                        )
                        drawCircle(
                            color = Color(0xFFFFFBEB),
                            radius = radiusPx,
                            center = Offset(px, py)
                        )
                    }
                    ObjectType.MOON -> {
                        if (obj.id == "moon") {
                            // Lunar Halo & Phase-aware Disc
                            drawCircle(
                                color = Color(0xFFE2E8F0).copy(alpha = 0.25f),
                                radius = radiusPx * 1.7f,
                                center = Offset(px, py)
                            )
                            drawCircle(
                                color = Color(0xFFF1FAEE),
                                radius = radiusPx,
                                center = Offset(px, py)
                            )
                            val illumFrac = (moonData.illuminationPercent / 100.0).coerceIn(0.0, 1.0).toFloat()
                            if (illumFrac < 0.98f) {
                                val limbScreenAngleDeg = ARProjectionEngine.calculateMoonLimbScreenAngleDeg(
                                    moonAzimuthDeg = moonHoriz.azimuthDeg,
                                    moonAltitudeDeg = moonHoriz.altitudeDeg,
                                    sunAzimuthDeg = sunHoriz.azimuthDeg,
                                    sunAltitudeDeg = sunHoriz.altitudeDeg,
                                    rotationMatrix = if (isSensorActive) skyOrientation.rotationMatrix else null,
                                    currentAzimuth = currentAzimuth,
                                    currentAltitude = currentAltitude,
                                    currentRoll = skyOrientation.roll.toDouble()
                                ).toFloat()

                                val shadowPath = Path()
                                // Outer shadow arc around left edge (+90° bottom to -90° top)
                                shadowPath.addArc(
                                    Rect(px - radiusPx, py - radiusPx, px + radiusPx, py + radiusPx),
                                    90f,
                                    180f
                                )
                                val k = (2.0 * illumFrac - 1.0).toFloat()
                                val stepInnerWidth = (abs(k) * radiusPx).coerceAtLeast(0f)
                                val innerRect = Rect(px - stepInnerWidth, py - radiusPx, px + stepInnerWidth, py + radiusPx)

                                // Inner terminator arc from -90° top to +90° bottom
                                val innerSweep = if (k >= 0) -180f else 180f
                                shadowPath.arcTo(innerRect, 270f, innerSweep, false)
                                shadowPath.close()

                                rotate(limbScreenAngleDeg, Offset(px, py)) {
                                    drawPath(
                                        path = shadowPath,
                                        color = Color(0xFF0F172A).copy(alpha = 0.85f)
                                    )
                                }
                            }
                        } else {
                            // Jovian Satellite / Galilean Moon Disk
                            drawCircle(
                                color = Color(0xFF818CF8).copy(alpha = 0.35f),
                                radius = radiusPx * 1.6f,
                                center = Offset(px, py)
                            )
                            drawCircle(
                                color = Color(0xFFC7D2FE),
                                radius = radiusPx,
                                center = Offset(px, py)
                            )
                        }
                    }
                    ObjectType.PLANET -> {
                        if (obj.id == "planet_jupiter") {
                            // Jupiter: Cream disk with 2 horizontal bands
                            drawCircle(
                                color = Color(0xFFE2D1A6),
                                radius = radiusPx,
                                center = Offset(px, py)
                            )
                            drawLine(
                                color = Color(0xFFC0703B),
                                start = Offset(px - radiusPx * 0.8f, py - radiusPx * 0.35f),
                                end = Offset(px + radiusPx * 0.8f, py - radiusPx * 0.35f),
                                strokeWidth = 2.5f * density
                            )
                            drawLine(
                                color = Color(0xFFC0703B),
                                start = Offset(px - radiusPx * 0.8f, py + radiusPx * 0.35f),
                                end = Offset(px + radiusPx * 0.8f, py + radiusPx * 0.35f),
                                strokeWidth = 2.5f * density
                            )
                        } else if (obj.id == "planet_saturn") {
                            // Saturn: Golden disk + tiny ring system
                            drawCircle(
                                color = Color(0xFFFDE047),
                                radius = radiusPx,
                                center = Offset(px, py)
                            )
                            drawOval(
                                color = Color(0xFFFACC15).copy(alpha = 0.85f),
                                topLeft = Offset(px - radiusPx * 2.2f, py - radiusPx * 0.7f),
                                size = Size(radiusPx * 4.4f, radiusPx * 1.4f),
                                style = Stroke(width = 2.5f * density)
                            )
                        } else if (obj.id == "planet_mars") {
                            // Mars: Warm reddish-orange disk
                            drawCircle(
                                color = Color(0xFFEF4444).copy(alpha = 0.3f),
                                radius = radiusPx * 1.8f,
                                center = Offset(px, py)
                            )
                            drawCircle(
                                color = Color(0xFFEF4444),
                                radius = radiusPx,
                                center = Offset(px, py)
                            )
                        } else if (obj.id == "planet_venus") {
                            // Venus: Brilliant golden-white disk
                            drawCircle(
                                color = Color(0xFFFEF08A).copy(alpha = 0.4f),
                                radius = radiusPx * 2.0f,
                                center = Offset(px, py)
                            )
                            drawCircle(
                                color = Color(0xFFFEF08A),
                                radius = radiusPx,
                                center = Offset(px, py)
                            )
                        } else if (obj.id == "planet_uranus" || obj.id == "planet_neptune") {
                            // Blue-green disks
                            drawCircle(
                                color = Color(0xFF38BDF8),
                                radius = radiusPx,
                                center = Offset(px, py)
                            )
                        } else {
                            // Slate / default planet
                            drawCircle(
                                color = Color(0xFFFFD166).copy(alpha = 0.35f),
                                radius = radiusPx * 1.8f,
                                center = Offset(px, py)
                            )
                            drawCircle(
                                color = Color(0xFFFFD166),
                                radius = radiusPx,
                                center = Offset(px, py)
                            )
                        }
                    }
                    ObjectType.SATELLITE -> {
                        // Satellite minimal glyph: center body + 2 solar panel wings
                        drawRect(
                            color = Color(0xFF38BDF8),
                            topLeft = Offset(px - radiusPx * 2.0f, py - radiusPx * 0.4f),
                            size = Size(radiusPx * 4.0f, radiusPx * 0.8f)
                        )
                        drawRect(
                            color = Color(0xFFF59E0B),
                            topLeft = Offset(px - radiusPx * 0.6f, py - radiusPx * 0.6f),
                            size = Size(radiusPx * 1.2f, radiusPx * 1.2f)
                        )
                    }
                    ObjectType.DEEP_SKY, ObjectType.GALAXY, ObjectType.NEBULA,
                    ObjectType.STAR_CLUSTER, ObjectType.GLOBULAR_CLUSTER, ObjectType.BLACK_HOLE -> {
                        if (obj.type == ObjectType.GALAXY || obj.id.contains("m31") || obj.category.contains("Galaxy", ignoreCase = true)) {
                            // Galaxy: tiny elliptical glow + bright core
                            drawOval(
                                color = Color(0xFFC084FC).copy(alpha = 0.45f),
                                topLeft = Offset(px - radiusPx * 1.8f, py - radiusPx * 0.9f),
                                size = Size(radiusPx * 3.6f, radiusPx * 1.8f)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = radiusPx * 0.4f,
                                center = Offset(px, py)
                            )
                        } else if (obj.type == ObjectType.NEBULA || obj.category.contains("Nebula", ignoreCase = true)) {
                            // Nebula: faint diffuse cloud
                            drawCircle(
                                color = Color(0xFFF472B6).copy(alpha = 0.35f),
                                radius = radiusPx * 1.8f,
                                center = Offset(px, py)
                            )
                            drawCircle(
                                color = Color(0xFF38BDF8).copy(alpha = 0.3f),
                                radius = radiusPx * 1.1f,
                                center = Offset(px - radiusPx * 0.3f, py + radiusPx * 0.2f)
                            )
                        } else if (obj.type == ObjectType.GLOBULAR_CLUSTER) {
                            // Globular cluster: compact soft glow
                            drawCircle(
                                color = Color(0xFFE2E8F0).copy(alpha = 0.5f),
                                radius = radiusPx * 1.4f,
                                center = Offset(px, py)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = radiusPx * 0.3f,
                                center = Offset(px, py)
                            )
                        } else {
                            // Star cluster
                            drawCircle(
                                color = Color(0xFFC77DFF).copy(alpha = 0.4f),
                                radius = radiusPx * 1.6f,
                                center = Offset(px, py)
                            )
                            drawCircle(
                                color = Color(0xFFE0AAFF),
                                radius = radiusPx,
                                center = Offset(px, py)
                            )
                        }
                    }
                    else -> {
                        // Star
                        val starRadius = ((4.0 - obj.magnitude).coerceIn(1.2, 5.0) * density).toFloat() * proximityScale
                        val starColor = if (obj.magnitude <= 0.5) Color(0xFFA5F3FC) else if (obj.magnitude <= 2.5) Color(0xFFE2E8F0) else Color(0xFF94A3B8)
                        drawCircle(
                            color = starColor,
                            radius = starRadius,
                            center = Offset(px, py)
                        )
                        // Soft 4-point diffraction cross for brightest stars (mag <= 0.5)
                        if (obj.magnitude <= 0.5) {
                            val spikeLen = starRadius * 2.5f
                            drawLine(
                                color = Color.White.copy(alpha = 0.35f),
                                start = Offset(px - spikeLen, py),
                                end = Offset(px + spikeLen, py),
                                strokeWidth = 1.5f
                            )
                            drawLine(
                                color = Color.White.copy(alpha = 0.35f),
                                start = Offset(px, py - spikeLen),
                                end = Offset(px, py + spikeLen),
                                strokeWidth = 1.5f
                            )
                        }
                    }
                }

                // Selected target ring or Aimed reticle highlight
                if (isSelected) {
                    drawCircle(
                        color = AccentPrimary,
                        radius = radiusPx + 14f,
                        center = Offset(px, py),
                        style = Stroke(width = 2.5f)
                    )
                } else if (isAimed && !filterObjectNames) {
                    drawCircle(
                        color = AccentPrimary.copy(alpha = 0.85f),
                        radius = radiusPx + 10f,
                        center = Offset(px, py),
                        style = Stroke(width = 1.8f)
                    )
                }

                // Label with dark backing pill
                val shouldShowLabel = if (filterObjectNames) {
                    obj.magnitude <= CelestialObjectSizes.LABEL_SHOW_MAGNITUDE_THRESHOLD || isSelected || obj.type != ObjectType.STAR || isAimed
                } else {
                    isSelected || isAimed
                }

                if (shouldShowLabel) {
                    val labelText = if (isFa) obj.nameFa else obj.nameEn
                    val textLayout = textMeasurer.measure(
                        text = labelText,
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        softWrap = false
                    )

                    val pillWidth = textLayout.size.width + 20f
                    val pillHeight = textLayout.size.height + 10f
                    val pillLeft = px - pillWidth / 2f
                    val pillTop = py + radiusPx + 8f

                    if (
                        pillLeft.isFinite() &&
                        pillTop.isFinite() &&
                        pillLeft < canvasWidth &&
                        pillTop < canvasHeight &&
                        pillLeft + pillWidth > 0f &&
                        pillTop + pillHeight > 0f
                    ) {
                        drawRoundRect(
                            color = Color.Black.copy(alpha = if (isAimed || isSelected) 0.85f else 0.65f),
                            topLeft = Offset(pillLeft, pillTop),
                            size = Size(pillWidth, pillHeight),
                            cornerRadius = CornerRadius(12f, 12f)
                        )
                        if (isAimed || isSelected) {
                            drawRoundRect(
                                color = AccentPrimary,
                                topLeft = Offset(pillLeft, pillTop),
                                size = Size(pillWidth, pillHeight),
                                cornerRadius = CornerRadius(12f, 12f),
                                style = Stroke(width = 1.5f)
                            )
                        }

                        val textDrawX = pillLeft + 10f
                        val textDrawY = pillTop + 5f
                        if (
                            textDrawX.isFinite() &&
                            textDrawY.isFinite() &&
                            textDrawX < canvasWidth &&
                            textDrawY < canvasHeight &&
                            textDrawX + textLayout.size.width > 0f &&
                            textDrawY + textLayout.size.height > 0f
                        ) {
                            drawText(
                                textLayoutResult = textLayout,
                                topLeft = Offset(textDrawX, textDrawY)
                            )
                        }
                    }
                }
            }

            // 3. Center Crosshair Aiming Reticle Frame
            drawCircle(
                color = AccentPrimary,
                radius = 65f,
                center = Offset(centerX, centerY),
                style = Stroke(width = 2.5f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.35f),
                radius = 110f,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1.5f)
            )

            // Tick marks
            drawLine(
                color = Color.White,
                start = Offset(centerX - 85f, centerY),
                end = Offset(centerX - 50f, centerY),
                strokeWidth = 2f
            )
            drawLine(
                color = Color.White,
                start = Offset(centerX + 50f, centerY),
                end = Offset(centerX + 85f, centerY),
                strokeWidth = 2f
            )
            drawLine(
                color = Color.White,
                start = Offset(centerX, centerY - 85f),
                end = Offset(centerX, centerY - 50f),
                strokeWidth = 2f
            )
            drawLine(
                color = Color.White,
                start = Offset(centerX, centerY + 50f),
                end = Offset(centerX, centerY + 85f),
                strokeWidth = 2f
            )
        }

        // Auto-Hide Restoration Pill
        AnimatedVisibility(
            visible = !isControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp)
        ) {
            LiquidGlassSurface(
                onClick = { resetControlsTimer() },
                shape = CircleShape,
                style = LiquidGlassDefaults.Pill,
                fallbackColor = Color.Black.copy(alpha = 0.75f),
                fallbackBorder = BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.TouchApp, contentDescription = null, tint = AccentPrimary, modifier = Modifier.size(16.dp))
                    Text(
                        text = if (isFa) "لمس کنید تا ابزارها ظاهر شوند" else "Tap screen to show controls",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }

        // Layer 3: Arrow-Guided Finder Overlay (When an object is selected, remains visible during auto-hide)
        finderData?.let { finder ->
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(260.dp)
            ) {
                // Direction Arrow Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate((finder.arrowAngleRad * 180f / PI.toFloat()))
                ) {
                    val w = size.width
                    val h = size.height
                    val cX = w / 2f
                    val cY = h / 2f

                    // Outer guidance progress ring
                    drawCircle(
                        color = AccentPrimary.copy(alpha = 0.25f),
                        radius = 110f,
                        center = Offset(cX, cY),
                        style = Stroke(width = 6f)
                    )
                    drawArc(
                        color = AccentPrimary,
                        startAngle = -90f,
                        sweepAngle = finder.proximityFraction * 360f,
                        useCenter = false,
                        topLeft = Offset(cX - 110f, cY - 110f),
                        size = Size(220f, 220f),
                        style = Stroke(width = 8f)
                    )

                    // Pointing Arrow Path
                    val arrowPath = Path().apply {
                        moveTo(cX, cY - 95f)
                        lineTo(cX + 20f, cY - 65f)
                        lineTo(cX + 8f, cY - 68f)
                        lineTo(cX + 8f, cY - 45f)
                        lineTo(cX - 8f, cY - 45f)
                        lineTo(cX - 8f, cY - 68f)
                        lineTo(cX - 20f, cY - 65f)
                        close()
                    }
                    drawPath(path = arrowPath, color = AccentPrimary)
                }

                // Instruction & Distance Card floating below arrow
                LiquidGlassSurface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 60.dp),
                    shape = RoundedCornerShape(20.dp),
                    style = LiquidGlassDefaults.Card,
                    fallbackColor = RedTheme.colors.surfaceElevated.copy(alpha = 0.95f),
                    fallbackBorder = BorderStroke(1.dp, RedTheme.colors.border)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isFa) finder.instructionFa else finder.instructionEn,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = RedTheme.colors.textPrimary
                        )
                        val distStr = String.format("%.1f°", finder.totalAngularDistanceDeg)
                        Text(
                            text = if (isFa) "فاصله تا هدف: ${TimeEngine.formatPersianNumbers(distStr)}" else "Distance: $distStr",
                            style = MaterialTheme.typography.labelSmall,
                            color = RedTheme.colors.accentRed
                        )
                    }
                }
            }
        }

        // Layer 3.5: Glass Floating Cancel Target Button (Visible whenever target is active at bottom-start, safely above bottom nav bar)
        if (selectedTarget != null) {
            LiquidGlassSurface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, bottom = 88.dp)
                    .testTag("ar_cancel_target_button"),
                onClick = {
                    selectedTarget = null
                    viewModel.clearTargetObject()
                },
                shape = RoundedCornerShape(20.dp),
                style = LiquidGlassDefaults.Pill,
                fallbackColor = Color(0xCC1A1F36),
                fallbackBorder = BorderStroke(1.dp, Color(0x88FF5252)),
                fallbackShadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = if (isFa) "انصراف از ردیابی" else "Cancel Target",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isFa) "انصراف از ردیابی" else "Cancel Target",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Layer 4: Floating Top Header Pill & Focus Mode Smart Pills
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = RedSpacing.xs, start = RedSpacing.md, end = RedSpacing.md)
                .fillMaxWidth()
                .graphicsLayer { alpha = controlsAlpha }
        ) {
            // FLOATING TOP HEADER PILL
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = RedSpacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Floating Pill: Back Button + AR Sky Title
                LiquidGlassSurface(
                    shape = CircleShape,
                    style = LiquidGlassDefaults.Pill,
                    fallbackColor = RedTheme.colors.surfaceElevated.copy(alpha = 0.88f),
                    fallbackBorder = BorderStroke(1.dp, RedTheme.colors.border),
                    fallbackShadowElevation = RedElevation.floating
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.selectTab(4) },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("ar_back_button")
                        ) {
                            Icon(
                                imageVector = if (isFa) Icons.AutoMirrored.Filled.ArrowForward else Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = RedTheme.colors.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = if (isFa) "آسمان AR" else "AR Sky",
                            style = RedTypographyTokens.bodyPrimary.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            ),
                            color = RedTheme.colors.textPrimary,
                            modifier = Modifier.padding(end = 10.dp)
                        )
                    }
                }

                // Right Floating Pill: Quick Tools (Night Vision + Calibrate)
                val isNightVision = uiState.skyCanvasTheme == SkyCanvasTheme.OBSERVATORY
                LiquidGlassSurface(
                    shape = CircleShape,
                    style = LiquidGlassDefaults.Pill,
                    fallbackColor = RedTheme.colors.surfaceElevated.copy(alpha = 0.88f),
                    fallbackBorder = BorderStroke(1.dp, RedTheme.colors.border),
                    fallbackShadowElevation = RedElevation.floating
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.setSkyCanvasTheme(
                                    if (isNightVision) SkyCanvasTheme.ATMOSPHERIC_SKY else SkyCanvasTheme.OBSERVATORY
                                )
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NightsStay,
                                contentDescription = "Night Mode",
                                tint = if (isNightVision) RedTheme.colors.accentRed else RedTheme.colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { showCalibrationDialog = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Calibrate",
                                tint = RedTheme.colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // SMART FLOATING PILLS ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pill 1: Search
                ArSmartPill(
                    icon = Icons.Default.Search,
                    label = if (isFa) "جستجو" else "Search",
                    isActive = activeExpandedPanel == ArExpandedPanel.SEARCH,
                    isHighlighted = selectedTarget != null,
                    onClick = {
                        activeExpandedPanel = if (activeExpandedPanel == ArExpandedPanel.SEARCH) null else ArExpandedPanel.SEARCH
                    }
                )

                // Pill 2: Time Machine
                ArSmartPill(
                    icon = Icons.Default.Schedule,
                    label = if (isFa) "زمان" else "Time",
                    isActive = activeExpandedPanel == ArExpandedPanel.TIME_MACHINE,
                    isHighlighted = timeMachineState.mode == TimeMachineMode.SIMULATION,
                    onClick = {
                        activeExpandedPanel = if (activeExpandedPanel == ArExpandedPanel.TIME_MACHINE) null else ArExpandedPanel.TIME_MACHINE
                    }
                )

                // Pill 3: Object Filters
                ArSmartPill(
                    icon = Icons.Default.FilterList,
                    label = if (isFa) "فیلترها" else "Filters",
                    isActive = activeExpandedPanel == ArExpandedPanel.FILTERS,
                    isHighlighted = !(filterStars && filterConstellations && filterPlanets && filterMoons && filterSun && filterDeepSky && filterSatellites && filterObjectNames),
                    onClick = {
                        activeExpandedPanel = if (activeExpandedPanel == ArExpandedPanel.FILTERS) null else ArExpandedPanel.FILTERS
                    }
                )

                // Pill 4: Sensors / GPS
                ArSmartPill(
                    icon = Icons.Default.Sensors,
                    label = if (isFa) "حسگرها" else "Sensors",
                    isActive = activeExpandedPanel == ArExpandedPanel.SENSORS,
                    isHighlighted = isGpsActive && isSensorActive,
                    onClick = {
                        activeExpandedPanel = if (activeExpandedPanel == ArExpandedPanel.SENSORS) null else ArExpandedPanel.SENSORS
                    }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // EXPANDED PANEL CONTAINER (Renders only ONE expanded panel at a time)
            AnimatedVisibility(
                visible = activeExpandedPanel != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                LiquidGlassSurface(
                    shape = RoundedCornerShape(RedCornerRadius.xl),
                    style = LiquidGlassDefaults.Card,
                    fallbackColor = RedTheme.colors.surfaceElevated.copy(alpha = 0.96f),
                    fallbackBorder = BorderStroke(1.dp, RedTheme.colors.border),
                    fallbackShadowElevation = RedElevation.floating,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    when (activeExpandedPanel) {
                        ArExpandedPanel.SEARCH -> {
                            // Search Panel Content
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = RedTheme.colors.accentRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    TextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        placeholder = {
                                            Text(
                                                text = if (isFa) "جستجوی جرم (ماه، مریخ، شباهنگ...)" else "Search target (Moon, Mars, Sirius...)",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = RedTheme.colors.textSecondary
                                            )
                                        },
                                        singleLine = true,
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent,
                                            focusedTextColor = RedTheme.colors.textPrimary,
                                            unfocusedTextColor = RedTheme.colors.textPrimary
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("ar_search_input")
                                    )
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(
                                            onClick = { searchQuery = "" },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Clear",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                // Active Target Chip Banner
                                selectedTarget?.let { target ->
                                    HorizontalDivider(color = CardBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.GpsFixed,
                                                contentDescription = "Target",
                                                tint = AccentPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = if (isFa) "هدف قفل‌شده: ${target.nameFa}" else "Locked Target: ${target.nameEn}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = AccentPrimary
                                            )
                                        }
                                        TextButton(
                                            onClick = { selectedTarget = null },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (isFa) "حذف هدف" else "Clear Target",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }

                                // Search Results or Quick Suggestions
                                if (searchResults.isNotEmpty()) {
                                    HorizontalDivider(color = RedTheme.colors.border, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
                                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp).fillMaxWidth()) {
                                        items(searchResults) { result ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        selectedTarget = result.celestialObject
                                                        searchQuery = ""
                                                        activeExpandedPanel = null // Collapse panel on target select
                                                    }
                                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        text = if (isFa) result.celestialObject.nameFa else result.celestialObject.nameEn,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = RedTheme.colors.textPrimary
                                                    )
                                                    Text(
                                                        text = if (isFa) result.celestialObject.constellationFa else result.celestialObject.constellationEn,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = RedTheme.colors.textSecondary
                                                    )
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = if (result.isVisibleNow) StatusGood.copy(alpha = 0.2f) else RedTheme.colors.surfaceVariant,
                                                    border = BorderStroke(1.dp, if (result.isVisibleNow) StatusGood else RedTheme.colors.border)
                                                ) {
                                                    Text(
                                                        text = if (isFa) result.statusFa else result.statusEn,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (result.isVisibleNow) StatusGood else RedTheme.colors.textSecondary,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    HorizontalDivider(color = RedTheme.colors.border, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
                                    Text(
                                        text = if (isFa) "پیشنهادهای سریع:" else "Quick Suggestions:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = RedTheme.colors.textSecondary,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(CelestialSearchEngine.getQuickSuggestions()) { sug ->
                                            SuggestionChip(
                                                onClick = { searchQuery = sug },
                                                label = { Text(sug, color = RedTheme.colors.textPrimary) },
                                                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = RedTheme.colors.surfaceVariant)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        ArExpandedPanel.TIME_MACHINE -> {
                            // Time Machine Controls Panel Content
                            TimeMachineControlBar(
                                state = timeMachineState,
                                isFa = isFa,
                                calendarSystem = uiState.calendarSystem,
                                userOccasions = uiState.userOccasions,
                                onSaveOccasion = { id, title, timestampMs, cb ->
                                    viewModel.saveUserOccasion(id, title, timestampMs, cb)
                                },
                                onDeleteOccasion = { id ->
                                    viewModel.deleteUserOccasion(id)
                                },
                                onSimulatedTimeChange = { timeMs, eventName, isBirthday ->
                                    viewModel.setSimulatedTime(timeMs, eventName, isBirthday)
                                },
                                onModeChange = { mode -> viewModel.setTimeMachineMode(mode) },
                                onTogglePlay = { viewModel.toggleTimeMachinePlaying() },
                                onSpeedChange = { speed -> viewModel.setTimeMachineSpeed(speed) },
                                onToggleReverse = { viewModel.toggleTimeMachineReverse() },
                                onToggleExpanded = { viewModel.toggleTimeMachineExpanded() },
                                onReturnToLive = { handleReturnToLive() }
                            )
                        }

                        ArExpandedPanel.FILTERS -> {
                            // Sky Object Visibility Filters Panel Content
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isFa) "فیلترهای نمایش اجرام آسمان" else "Sky Object Visibility",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = RedTheme.colors.textPrimary
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        TextButton(
                                            onClick = { setAllFilters(true) },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(if (isFa) "نمایش همه" else "Show All", style = MaterialTheme.typography.labelSmall, color = RedTheme.colors.accentRed)
                                        }
                                        TextButton(
                                            onClick = { setAllFilters(false) },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(if (isFa) "پنهان همه" else "Hide All", style = MaterialTheme.typography.labelSmall, color = RedTheme.colors.textSecondary)
                                        }
                                    }
                                }

                                HorizontalDivider(color = RedTheme.colors.border, thickness = 0.5.dp)

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    item {
                                        FilterChip(
                                            selected = filterObjectNames,
                                            onClick = { updateFilter("filter_object_names", !filterObjectNames) { filterObjectNames = it } },
                                            label = { Text(if (isFa) "🏷 نام اجرام" else "🏷 Object Names") }
                                        )
                                    }
                                    item {
                                        FilterChip(
                                            selected = filterStars,
                                            onClick = { updateFilter("filter_stars", !filterStars) { filterStars = it } },
                                            label = { Text(if (isFa) "⭐ ستارگان" else "⭐ Stars") }
                                        )
                                    }
                                    item {
                                        FilterChip(
                                            selected = filterConstellations,
                                            onClick = { updateFilter("filter_constellations", !filterConstellations) { filterConstellations = it } },
                                            label = { Text(if (isFa) "✨ صور فلکی" else "✨ Constellations") }
                                        )
                                    }
                                    item {
                                        FilterChip(
                                            selected = filterPlanets,
                                            onClick = { updateFilter("filter_planets", !filterPlanets) { filterPlanets = it } },
                                            label = { Text(if (isFa) "🪐 سیارات" else "🪐 Planets") }
                                        )
                                    }
                                    item {
                                        FilterChip(
                                            selected = filterMoons,
                                            onClick = { updateFilter("filter_moons", !filterMoons) { filterMoons = it } },
                                            label = { Text(if (isFa) "🌙 ماه" else "🌙 Moon") }
                                        )
                                    }
                                    item {
                                        FilterChip(
                                            selected = filterSun,
                                            onClick = { updateFilter("filter_sun", !filterSun) { filterSun = it } },
                                            label = { Text(if (isFa) "☀️ خورشید" else "☀️ Sun") }
                                        )
                                    }
                                    item {
                                        FilterChip(
                                            selected = filterDeepSky,
                                            onClick = { updateFilter("filter_deepsky", !filterDeepSky) { filterDeepSky = it } },
                                            label = { Text(if (isFa) "🌌 اعماق فضا" else "🌌 Deep Sky") }
                                        )
                                    }
                                    item {
                                        FilterChip(
                                            selected = filterSatellites,
                                            onClick = { updateFilter("filter_satellites", !filterSatellites) { filterSatellites = it } },
                                            label = { Text(if (isFa) "🛰 ماهواره‌ها" else "🛰 Satellites") }
                                        )
                                    }
                                }
                            }
                        }

                        ArExpandedPanel.SENSORS -> {
                            // Sensors & GPS Control Panel Content
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = if (isFa) "مدیریت حسگرها و موقعیت‌یاب" else "Sensors & Telemetry Control",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = RedTheme.colors.textPrimary
                                )

                                // Sensor Toggles Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // GPS Toggle
                                    FilterChip(
                                        selected = isGpsActive,
                                        onClick = {
                                            if (!hasLocationPermission) {
                                                locationPermissionLauncher.launch(
                                                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                                )
                                            } else {
                                                isGpsActive = !isGpsActive
                                            }
                                        },
                                        label = { Text(if (isFa) "GPS دائم" else "Live GPS") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (isGpsActive) Icons.Default.GpsFixed else Icons.Default.GpsOff,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )

                                    // Sensors Toggle
                                    FilterChip(
                                        selected = isSensorActive,
                                        onClick = { isSensorActive = !isSensorActive },
                                        label = { Text(if (isFa) "ژیروسکوپ / قطب‌نما" else "Sensor Fusion") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (isSensorActive) Icons.Default.Sensors else Icons.Default.SensorsOff,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )

                                    // Camera Toggle
                                    FilterChip(
                                        selected = isCameraEnabled,
                                        onClick = {
                                            if (!hasCameraPermission) {
                                                permissionLauncher.launch(Manifest.permission.CAMERA)
                                            } else {
                                                isCameraEnabled = !isCameraEnabled
                                            }
                                        },
                                        label = { Text(if (isFa) "دوربین AR" else "Camera Feed") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (isCameraEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )
                                }

                                // Operational Status Summary Card
                                var showDetailsAccordion by remember { mutableStateOf(false) }

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = StatusGood.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, StatusGood.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                                    imageVector = Icons.Default.VerifiedUser,
                                                    contentDescription = "Status",
                                                    tint = StatusGood,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text(
                                                    text = if (isFa) "تمامی حسگرها فعال و آماده رصد هستند" else "All Systems Operational",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = StatusGood
                                                )
                                            }

                                            TextButton(
                                                onClick = { showDetailsAccordion = !showDetailsAccordion },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (showDetailsAccordion) (if (isFa) "بستن جزئیات" else "Hide Details")
                                                    else (if (isFa) "جزئیات تلمتری" else "Telemetry"),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = AccentPrimary
                                                )
                                            }
                                        }

                                        if (showDetailsAccordion) {
                                            HorizontalDivider(color = StatusGood.copy(alpha = 0.3f), thickness = 0.5.dp)
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                val accStr = gpsAccuracyMeters?.let { String.format("%.0fm", it) } ?: "دقیق"
                                                Text(
                                                    text = if (isFa) "• دقت GPS: ${TimeEngine.formatPersianNumbers(accStr)}" else "• GPS Accuracy: $accStr",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = RedTheme.colors.textPrimary
                                                )
                                                Text(
                                                    text = if (isFa) "• موقعیت: ${TimeEngine.formatPersianNumbers(String.format("%.2f°", uiState.userLocation.latitude))}, ${TimeEngine.formatPersianNumbers(String.format("%.2f°", uiState.userLocation.longitude))}"
                                                    else "• Location: ${String.format("%.2f°", uiState.userLocation.latitude)}, ${String.format("%.2f°", uiState.userLocation.longitude)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = RedTheme.colors.textPrimary
                                                )
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = if (isFa) "• وضعیت کالیبراسیون سنسور: ${calibrationState.name}" else "• Sensor Calibration: ${calibrationState.name}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = RedTheme.colors.textPrimary
                                                    )
                                                    TextButton(
                                                        onClick = { showManualSensorPrompt = true },
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                                    ) {
                                                        Text(
                                                            text = if (isFa) "راهنمای شکل ۸" else "Figure-8 Guide",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = AccentPrimary
                                                        )
                                                    }
                                                }
                                                if (arCalibrationOffsets.isCalibrated) {
                                                    val yawFmt = String.format("%+.1f°", arCalibrationOffsets.yawOffsetDeg)
                                                    val pitchFmt = String.format("%+.1f°", arCalibrationOffsets.pitchOffsetDeg)
                                                    val rollFmt = String.format("%+.1f°", arCalibrationOffsets.rollOffsetDeg)
                                                    Text(
                                                        text = if (isFa)
                                                            "• آفست‌های AR: سمت ${TimeEngine.formatPersianNumbers(yawFmt)} | ارتفاع ${TimeEngine.formatPersianNumbers(pitchFmt)} | رول ${TimeEngine.formatPersianNumbers(rollFmt)}"
                                                        else
                                                            "• AR Offsets: Yaw $yawFmt | Pitch $pitchFmt | Roll $rollFmt",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = RedTheme.colors.accentRed
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Manual AR Calibration Action Card
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = RedTheme.colors.surfaceElevated,
                                    border = BorderStroke(1.dp, RedTheme.colors.border),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
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
                                                    imageVector = Icons.Default.Tune,
                                                    contentDescription = null,
                                                    tint = RedTheme.colors.accentRed,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Column {
                                                    Text(
                                                        text = if (isFa) "کالیبراسیون دستی جهت‌گیری AR" else "AR Pointing Calibration",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = RedTheme.colors.textPrimary
                                                    )
                                                    Text(
                                                        text = if (arCalibrationOffsets.isCalibrated) {
                                                            val yawFmt = String.format("%+.1f°", arCalibrationOffsets.yawOffsetDeg)
                                                            val pitchFmt = String.format("%+.1f°", arCalibrationOffsets.pitchOffsetDeg)
                                                            if (isFa) "آفست‌های فعال: سمت $yawFmt، ارتفاع $pitchFmt" else "Active offsets: Yaw $yawFmt, Pitch $pitchFmt"
                                                        } else {
                                                            if (isFa) "همترازی نشانگر با ستاره واقعی" else "Align pointer with bright star"
                                                        },
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = RedTheme.colors.textSecondary
                                                    )
                                                }
                                            }

                                            Button(
                                                onClick = { showCalibrationDialog = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = RedTheme.colors.accentRed),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                modifier = Modifier.testTag("open_ar_calibration_btn")
                                            ) {
                                                Text(
                                                    text = if (isFa) "تنظیم" else "Calibrate",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        else -> {}
                    }
                }
            }
        }

        // Zoom Indicator Floating Badge (Temporary while zooming)
        AnimatedVisibility(
            visible = showZoomIndicator,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 54.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.8f),
                border = BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.6f)),
                shadowElevation = 8.dp
            ) {
                Text(
                    text = String.format(Locale.US, "%.1f×", zoomFactor),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }

        // Long-Press Glass Information Card Overlay
        val activeLongPressObj = longPressObject
        AnimatedVisibility(
            visible = activeLongPressObj != null,
            enter = fadeIn(animationSpec = tween(250)) + scaleIn(animationSpec = tween(250), initialScale = 0.85f),
            exit = fadeOut(animationSpec = tween(200)) + scaleOut(animationSpec = tween(200), targetScale = 0.85f)
        ) {
            val obj = activeLongPressObj ?: return@AnimatedVisibility

            val rs = remember(obj, jd) {
                CoordinateEngine.calculateRiseSetTransit(
                    raDeg = obj.raDeg,
                    decDeg = obj.decDeg,
                    latDeg = uiState.userLocation.latitude,
                    lonDeg = uiState.userLocation.longitude,
                    jd = jd,
                    isFa = isFa
                )
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val localDensity = LocalDensity.current
                val densityVal = localDensity.density

                val screenWidthPx = constraints.maxWidth.toFloat()
                val screenHeightPx = constraints.maxHeight.toFloat()
                val canvasWidth = screenWidthPx
                val canvasHeight = screenHeightPx

                val horiz = if (obj.type == ObjectType.SUN) sunHoriz
                else if (obj.id == "moon") moonHoriz
                else if (obj.type == ObjectType.SATELLITE || obj.id.startsWith("sat_")) satellitePositions[obj.id] ?: satellitePositions[com.alijafari.red.astronomy.data.catalog.CanonicalAstroCatalog.resolveCanonicalId(obj.id)] ?: CoordinateEngine.Horizontal(-90.0, 0.0)
                else CoordinateEngine.equatorialToHorizontal(staticObjectEquatorial(obj, jd), lastDeg, uiState.userLocation.latitude)

                val projPt = ARProjectionEngine.projectAltAz(
                    azimuthDeg = horiz.azimuthDeg,
                    altitudeDeg = horiz.altitudeDeg,
                    rotationMatrix = if (isSensorActive) skyOrientation.rotationMatrix else null,
                    currentAzimuth = currentAzimuth,
                    currentAltitude = currentAltitude,
                    currentRoll = skyOrientation.roll.toDouble(),
                    canvasWidth = canvasWidth,
                    canvasHeight = canvasHeight,
                    intrinsics = cameraIntrinsics,
                    zoomFactor = zoomFactor,
                    sensorToViewMatrix = previewViewInstance?.sensorToViewTransform,
                    displayRotationDegrees = displayRotationDegrees
                )

                val px = projPt?.x ?: (canvasWidth / 2f)
                val py = projPt?.y ?: (canvasHeight / 2f)

                val cardWidthDp = 270.dp
                val cardHeightDp = 190.dp
                val cardWidthPx = with(localDensity) { cardWidthDp.toPx() }
                val cardHeightPx = with(localDensity) { cardHeightDp.toPx() }

                val targetX = (px - cardWidthPx / 2f).coerceIn(16f * densityVal, screenWidthPx - cardWidthPx - 16f * densityVal)
                val preferredY = if (py - cardHeightPx - 20f * densityVal < 80f * densityVal) {
                    py + 20f * densityVal
                } else {
                    py - cardHeightPx - 20f * densityVal
                }
                val targetY = preferredY.coerceIn(80f * densityVal, screenHeightPx - cardHeightPx - 90f * densityVal)

                val cardXDp = (targetX / densityVal).dp
                val cardYDp = (targetY / densityVal).dp

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(x = cardXDp, y = cardYDp)
                ) {
                    LiquidGlassSurface(
                        modifier = Modifier
                            .width(cardWidthDp)
                            .clickable {
                                longPressObject = null
                                viewModel.openObjectDetail(obj)
                            },
                        shape = RoundedCornerShape(20.dp),
                        style = LiquidGlassDefaults.Card,
                        fallbackColor = RedTheme.colors.surfaceElevated.copy(alpha = 0.96f),
                        fallbackBorder = BorderStroke(1.dp, RedTheme.colors.accentRed.copy(alpha = 0.6f)),
                        fallbackShadowElevation = 12.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isFa) obj.nameFa else obj.nameEn,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = RedTheme.colors.textPrimary
                                    )
                                    Text(
                                        text = if (isFa) obj.type.nameFa else obj.type.nameEn,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = RedTheme.colors.accentRed,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                IconButton(
                                    onClick = { longPressObject = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = RedTheme.colors.textSecondary, modifier = Modifier.size(16.dp))
                                }
                            }

                            HorizontalDivider(color = RedTheme.colors.border, thickness = 0.5.dp)

                            val distStr = formatObjectDistance(obj, isFa)
                            Text(
                                text = if (isFa) "فاصله از زمین: $distStr" else "Dist: $distStr",
                                style = MaterialTheme.typography.labelSmall,
                                color = RedTheme.colors.textPrimary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isFa) "طلوع: ${rs.riseTimeStr}" else "Rise: ${rs.riseTimeStr}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RedTheme.colors.textSecondary
                                )
                                Text(
                                    text = if (isFa) "غروب: ${rs.setTimeStr}" else "Set: ${rs.setTimeStr}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RedTheme.colors.textSecondary
                                )
                            }

                            Button(
                                onClick = {
                                    longPressObject = null
                                    viewModel.openObjectDetail(obj)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RedTheme.colors.accentRed),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isFa) "مشاهده جزئیات" else "View Details",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Layer 7: Minimal "Object found" Confirmation Banner
        if (finderData?.isArrived == true) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-140).dp)
                    .testTag("ar_object_found_banner"),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xDD0D1B2A),
                border = BorderStroke(1.dp, Color(0xFF4CAF50))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isFa) "جرم پیدا شد" else "Object found",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Layer 8: Manual AR Pointing Calibration Dialog
        if (showCalibrationDialog) {
            var isAdjustingSlider by remember { mutableStateOf(false) }
            val scrimAlpha by animateFloatAsState(
                targetValue = if (isAdjustingSlider) 0f else 0.55f,
                animationSpec = tween(100),
                label = "calibScrimAlpha"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
                    .padding(horizontal = 12.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                ARCalibrationDialog(
                    isFa = isFa,
                    userLocation = uiState.userLocation,
                    currentJd = jd,
                    currentAzimuth = currentAzimuth,
                    currentAltitude = currentAltitude,
                    onDismiss = { showCalibrationDialog = false },
                    onSelectReferenceTarget = { target ->
                        selectedTarget = target
                        hasVibratedForArrival = false
                    },
                    onAdjustingStateChanged = { isAdjusting ->
                        isAdjustingSlider = isAdjusting
                    }
                )
            }
        }

        // Layer 9: Automatic / Manual Figure-8 Sensor Calibration Prompt
        if (showManualSensorPrompt || (autoPromptEnabled && !autoPromptDismissedThisSession && !showCalibrationDialog && (calibrationState == CalibrationState.NEEDS_CALIBRATION || calibrationState == CalibrationState.POOR || calibrationState == CalibrationState.UNCALIBRATED))) {
            ARSensorCalibrationDialog(
                calibrationState = calibrationState,
                isFa = isFa,
                onDismiss = {
                    autoPromptDismissedThisSession = true
                    showManualSensorPrompt = false
                },
                onDisableAutoPrompt = {
                    ARCalibrationManager.setAutoPromptEnabled(false, context)
                }
            )
        }

        // TEMPORARY DIAGNOSTICS BUTTON (debug builds only) — added because the D2
        // long-press proved unreliable in field use: three pre-existing gesture
        // consumers own the touch path (zoom detectTransformGestures + manual-offset
        // detectDragGestures later in this Box's modifier chain, and the full-screen
        // Layer-2 Canvas tap catcher), so a held press is consumed as drag/zoom long
        // before the ~500 ms long-press timeout. This button is the ONLY addition: it
        // calls the SAME StarTrackerDebugHost.open(...) the long-press calls — no new
        // diagnostics UI. REMOVE AFTER FIELD TESTING together with the long-press and
        // the hosting line below (see evidence/D_DEBUG_DIAGNOSTICS section 'fix pass').
        if (BuildConfig.DEBUG) {
            Button(
                onClick = { StarTrackerDebugHost.open(context, orientationProvider) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 16.dp, bottom = 28.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE53935),
                    contentColor = Color.White
                )
            ) {
                Text("DIAGNOSTICS", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
        }

        // D2: host the debug-only diagnostics overlay. Release: BuildConfig.DEBUG is a
        // compile-time false constant -> this branch does not exist; the panel class is
        // absent from the release compile (debug source set) -> provably unreachable.
        if (BuildConfig.DEBUG && StarTrackerDebugHost.visible.value) {
            StarTrackerDebugHost.HostContent()
        }
    }
}

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
import android.view.WindowManager
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.alijafari.red.astronomy.data.catalog.ConstellationCatalog
import com.alijafari.red.astronomy.data.catalog.StarCatalog
import com.alijafari.red.astronomy.domain.*
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.MainViewModel
import com.alijafari.red.astronomy.ui.components.ARTutorialModal
import com.alijafari.red.astronomy.ui.components.ARTutorialTopic
import com.alijafari.red.astronomy.ui.components.TimeMachineControlBar
import com.alijafari.red.astronomy.ui.screens.ssa.ScaleWalkView
import com.alijafari.red.astronomy.ui.screens.ssa.SpaceTimeExplorerView
import com.alijafari.red.astronomy.ui.screens.ssa.GravitySandboxView

import com.alijafari.red.astronomy.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.*

enum class SsaSubMode(val labelEn: String, val labelFa: String, val icon: ImageVector) {
    SCALE_WALK("Scale Walk", "پیمایش مقیاس", Icons.Default.DirectionsWalk),
    SPACE_TIME("Space-Time Explorer", "کاوشگر زمان-فضا", Icons.Default.Timelapse),
    GRAVITY_SANDBOX("Gravity Sandbox", "آزمایشگاه گرانش", Icons.Default.Public)
}

enum class ArPillType {
    CAM_GPS, TIME, SSA, MODES, SEARCH
}

enum class ArMode(val labelEn: String, val labelFa: String, val icon: ImageVector) {
    SKY_VIEW("Sky View (AR)", "دید عمومی آسمان (AR)", Icons.Default.CameraAlt),
    COMPASS_VIEW("Compass View", "قطب‌نمای ۳۶۰ درجه", Icons.Default.Explore),
    CONSTELLATION_LINES("Constellation Lines", "خطوط صورت‌های فلکی", Icons.Default.Polyline),
    ECLIPSE_PREVIEW("Eclipse Preview AR", "پیش‌نمایش گرفتگی (کسوف/خسوف)", Icons.Default.WbSunny)
}

@Composable
private fun ArTopPill(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    isHighlighted: Boolean = false,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isActive) AccentPrimary
        else if (isHighlighted) AccentSecondary.copy(alpha = 0.88f)
        else BackgroundCard.copy(alpha = 0.88f),
        label = "PillBg"
    )

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = bgColor,
        border = BorderStroke(
            1.dp,
            if (isActive) Color.White.copy(alpha = 0.7f) else if (isHighlighted) AccentSecondary else CardBorder
        ),
        shadowElevation = 8.dp,
        modifier = Modifier.height(42.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive || isHighlighted) Color.White else TextPrimary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isActive || isHighlighted) FontWeight.Bold else FontWeight.Medium,
                color = if (isActive || isHighlighted) Color.White else TextPrimary,
                fontSize = 12.sp
            )
        }
    }
}

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
    val coroutineScope = rememberCoroutineScope()

    // Keep screen awake
    val activity = context as? Activity
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Top Pill Control System State (Only ONE expanded at a time)
    var activeExpandedPill by remember { mutableStateOf<ArPillType?>(null) }
    var activeTutorialTopic by remember { mutableStateOf<ARTutorialTopic?>(null) }

    // AR Mode Architecture
    var selectedArMode by remember { mutableStateOf(ArMode.SKY_VIEW) }

    // Constellation Lines Mode Settings
    var showConstellationLines by remember { mutableStateOf(true) }
    var showConstellationNames by remember { mutableStateOf(true) }
    var constellationLineOpacity by remember { mutableFloatStateOf(0.75f) }

    // Solar System AR (SSA) Mode Settings
    var isSsaActive by remember { mutableStateOf(false) }
    var selectedSsaSubMode by remember { mutableStateOf(SsaSubMode.SCALE_WALK) }
    var ssaShow3dOrbits by remember { mutableStateOf(true) }
    var ssaPlanetScaleMode by remember { mutableStateOf("ENHANCED") } // REALISTIC vs ENHANCED

    // Eclipse Preview AR Settings
    var selectedEclipsePreset by remember { mutableStateOf(EclipseEngine.PRESET_ECLIPSES.first()) }

    // Handle Hardware Back Button: Collapse expanded pill before navigating
    BackHandler(enabled = activeExpandedPill != null) {
        activeExpandedPill = null
    }

    // Sensor Fusion & Orientation Provider
    val orientationProvider = remember { OrientationProvider(context) }
    val skyOrientation by orientationProvider.orientation.collectAsState()
    val calibrationState by orientationProvider.calibrationState.collectAsState()

    // Permissions
    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted -> hasCameraPermission = isGranted }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        hasLocationPermission = fine || coarse
    }

    // Live GPS & Telemetry
    var isGpsActive by remember { mutableStateOf(true) }
    var gpsAccuracyMeters by remember { mutableStateOf<Float?>(null) }

    DisposableEffect(isGpsActive, hasLocationPermission) {
        if (isGpsActive && hasLocationPermission) {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    gpsAccuracyMeters = if (location.hasAccuracy()) location.accuracy else null
                    viewModel.setLocation("Live GPS", "GPS زنده", location.latitude, location.longitude)
                    orientationProvider.updateLocation(location.latitude, location.longitude, location.altitude)
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            try {
                val lastGps = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                lastGps?.let { loc ->
                    gpsAccuracyMeters = if (loc.hasAccuracy()) loc.accuracy else null
                    viewModel.setLocation("Live GPS", "GPS زنده", loc.latitude, loc.longitude)
                    orientationProvider.updateLocation(loc.latitude, loc.longitude, loc.altitude)
                }

                if (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 1.0f, locationListener)
                } else if (locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true) {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000L, 1.0f, locationListener)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            onDispose {
                try { locationManager?.removeUpdates(locationListener) } catch (e: Exception) { e.printStackTrace() }
            }
        } else {
            onDispose { }
        }
    }

    var isSensorActive by remember { mutableStateOf(true) }
    var isCameraEnabled by remember { mutableStateOf(true) }

    // Manual Drag Fallback when sensors paused
    var manualAzimuthOffset by remember { mutableStateOf(0.0) }
    var manualPitchOffset by remember { mutableStateOf(0.0) }

    val currentAzimuth = if (isSensorActive) skyOrientation.azimuth.toDouble() else manualAzimuthOffset
    val currentAltitude = if (isSensorActive) skyOrientation.pitch.toDouble() else manualPitchOffset

    DisposableEffect(uiState.userLocation, isSensorActive) {
        orientationProvider.updateLocation(latitude = uiState.userLocation.latitude, longitude = uiState.userLocation.longitude)
        if (isSensorActive) orientationProvider.start() else orientationProvider.stop()
        onDispose { orientationProvider.stop() }
    }

    // Time Machine Logic
    val timeMachineState = uiState.timeMachineState

    LaunchedEffect(timeMachineState.mode, timeMachineState.isPlaying, timeMachineState.speed, timeMachineState.isReverse) {
        if (timeMachineState.mode == TimeMachineMode.SIMULATION && timeMachineState.isPlaying) {
            var lastFrameMs = System.currentTimeMillis()
            while (true) {
                delay(33L)
                val nowMs = System.currentTimeMillis()
                val deltaRealSec = (nowMs - lastFrameMs) / 1000.0
                lastFrameMs = nowMs

                val multiplier = timeMachineState.speed.multiplier
                val direction = if (timeMachineState.isReverse) -1.0 else 1.0
                val deltaSimMs = (deltaRealSec * multiplier * direction * 1000.0).toLong()

                val newTime = timeMachineState.simulationTimeMs + deltaSimMs
                viewModel.setSimulatedTime(newTime, timeMachineState.eventName, timeMachineState.isBirthdayMode)
            }
        }
    }

    var liveTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(timeMachineState.mode) {
        if (timeMachineState.mode == TimeMachineMode.LIVE) {
            while (true) {
                liveTimeMs = System.currentTimeMillis()
                delay(1000L)
            }
        }
    }

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
    val lastDeg = remember(uiState.userLocation, jd) { TimeEngine.getLAST(jd, uiState.userLocation.longitude) }

    val sunHoriz = remember(jd, uiState.userLocation) {
        SunEngine.getSunAltAz(jd, uiState.userLocation.latitude, uiState.userLocation.longitude)
    }

    val moonData = remember(jd) { MoonEngine.calculateMoon(jd) }
    val moonHoriz = remember(moonData, lastDeg, uiState.userLocation) {
        CoordinateEngine.equatorialToHorizontal(
            CoordinateEngine.Equatorial(moonData.raDeg, moonData.decDeg),
            lastDeg,
            uiState.userLocation.latitude
        )
    }

    // Eclipse Preview Engine Data
    val eclipseData = remember(activeTimeMs, uiState.userLocation, selectedEclipsePreset) {
        val targetTime = if (selectedArMode == ArMode.ECLIPSE_PREVIEW) selectedEclipsePreset.timestampMs else activeTimeMs
        EclipseEngine.calculateEclipseData(targetTime, uiState.userLocation.latitude, uiState.userLocation.longitude)
    }

    // ISS Orbit
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { ISSEngine.fetchLatestTLE() }
    }

    val issTopocentric = remember(activeTimeMs, uiState.userLocation) {
        ISSEngine.calculateTopocentricPos(activeTimeMs, uiState.userLocation.latitude, uiState.userLocation.longitude, uiState.userLocation.elevationMeters)
    }

    val issHoriz = remember(issTopocentric) {
        CoordinateEngine.Horizontal(altitudeDeg = issTopocentric.elevationDeg, azimuthDeg = issTopocentric.azimuthDeg)
    }

    val allCatalog = remember(jd) { AstronomyCatalog.getAllObjects(jd) }

    // Constellations for Constellation Lines Mode
    val constellationsData = remember { ConstellationCatalog.getConstellations() }
    val allStarsMap = remember { StarCatalog.getStars().associateBy { it.id } }

    // Search & Target Finder State
    var searchQuery by remember { mutableStateOf("") }
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
            CelestialSearchEngine.search(searchQuery, uiState.userLocation.latitude, uiState.userLocation.longitude, jd)
        } else emptyList()
    }

    val finderData = remember(selectedTarget, currentAzimuth, currentAltitude, uiState.userLocation, jd) {
        selectedTarget?.let { target ->
            FinderEngine.calculateFinderData(target, currentAzimuth, currentAltitude, uiState.userLocation.latitude, uiState.userLocation.longitude, jd)
        }
    }

    // Target Arrival Haptic Vibration
    LaunchedEffect(finderData?.isArrived) {
        if (finderData?.isArrived == true && !hasVibratedForArrival) {
            hasVibratedForArrival = true
            showArrivalDialog = true
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(300)
                }
            } catch (e: Exception) { e.printStackTrace() }
        } else if (finderData?.isArrived == false) {
            hasVibratedForArrival = false
        }
    }

    // Closest object near reticle for tap / lock
    val targetedObject = remember(currentAzimuth, currentAltitude, lastDeg, uiState.userLocation, allCatalog) {
        var closestObj: CelestialObject? = null
        var minDistance = 12.0
        for (obj in allCatalog) {
            val horiz = CoordinateEngine.equatorialToHorizontal(CoordinateEngine.Equatorial(obj.raDeg, obj.decDeg), lastDeg, uiState.userLocation.latitude)
            var dAz = horiz.azimuthDeg - currentAzimuth
            if (dAz > 180) dAz -= 360
            if (dAz < -180) dAz += 360
            val dAlt = horiz.altitudeDeg - currentAltitude
            val dist = sqrt(dAz * dAz + dAlt * dAlt)
            if (dist < minDistance) {
                minDistance = dist
                closestObj = obj
            }
        }
        closestObj
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF060810))
            .pointerInput(activeExpandedPill) {
                detectTapGestures {
                    if (activeExpandedPill != null) activeExpandedPill = null
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
            .testTag("ar_sky_screen")
    ) {
        // LAYER 1: Camera Feed (if enabled & granted)
        if (hasCameraPermission && isCameraEnabled && selectedArMode != ArMode.COMPASS_VIEW) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                        } catch (e: Exception) { e.printStackTrace() }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // LAYER 2: Main Astronomical Render Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures {
                        targetedObject?.let { obj -> selectedTarget = obj }
                    }
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerX = canvasWidth / 2f
            val centerY = canvasHeight / 2f

            val fovX = 60.0
            val pixelsPerDegree = canvasWidth / fovX

            // Starry background if camera off or in Compass Mode
            if (!hasCameraPermission || !isCameraEnabled || selectedArMode == ArMode.COMPASS_VIEW) {
                val rand = java.util.Random(1337)
                for (i in 0..220) {
                    val sx = rand.nextFloat() * canvasWidth
                    val sy = rand.nextFloat() * canvasHeight
                    val radius = rand.nextFloat() * 2.2f + 0.5f
                    drawCircle(
                        color = Color.White.copy(alpha = rand.nextFloat() * 0.65f + 0.2f),
                        radius = radius,
                        center = Offset(sx, sy)
                    )
                }
            }

            // -------------------------------------------------------------
            // MODE: 360° COMPASS VIEW RENDER
            // -------------------------------------------------------------
            if (selectedArMode == ArMode.COMPASS_VIEW) {
                // Outer Compass Dial Ring
                drawCircle(
                    color = AccentPrimary.copy(alpha = 0.4f),
                    radius = 160f * density,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 3f * density)
                )

                drawCircle(
                    color = CardBorder,
                    radius = 175f * density,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 1f * density)
                )

                // Render Compass Dial Ticks & Direction Labels
                val cardinalDirections = listOf(
                    0.0 to if (isFa) "N (شمال)" else "N (0°)",
                    45.0 to "NE (45°)",
                    90.0 to if (isFa) "E (شرق)" else "E (90°)",
                    135.0 to "SE (135°)",
                    180.0 to if (isFa) "S (جنوب)" else "S (180°)",
                    225.0 to "SW (225°)",
                    270.0 to if (isFa) "W (غرب)" else "W (270°)",
                    315.0 to "NW (315°)"
                )

                for (deg in 0..350 step 10) {
                    val angleRad = Math.toRadians(deg.toDouble() - currentAzimuth - 90.0)
                    val r1 = 150f * density
                    val r2 = if (deg % 30 == 0) 165f * density else 158f * density
                    val x1 = centerX + r1 * cos(angleRad).toFloat()
                    val y1 = centerY + r1 * sin(angleRad).toFloat()
                    val x2 = centerX + r2 * cos(angleRad).toFloat()
                    val y2 = centerY + r2 * sin(angleRad).toFloat()

                    drawLine(
                        color = if (deg == 0) AccentPrimary else Color.White.copy(alpha = 0.6f),
                        start = Offset(x1, y1),
                        end = Offset(x2, y2),
                        strokeWidth = if (deg % 30 == 0) 2.5f * density else 1.2f * density
                    )
                }

                for ((cDeg, cLabel) in cardinalDirections) {
                    val angleRad = Math.toRadians(cDeg - currentAzimuth - 90.0)
                    val rText = 135f * density
                    val tx = centerX + rText * cos(angleRad).toFloat()
                    val ty = centerY + rText * sin(angleRad).toFloat()

                    val tLayout = textMeasurer.measure(
                        text = cLabel,
                        style = TextStyle(
                            color = if (cDeg == 0.0) AccentPrimary else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    drawText(
                        textLayoutResult = tLayout,
                        topLeft = Offset(tx - tLayout.size.width / 2f, ty - tLayout.size.height / 2f)
                    )
                }

                // Center Needle Pointing North
                val northRad = Math.toRadians(0.0 - currentAzimuth - 90.0)
                val nx = centerX + 110f * density * cos(northRad).toFloat()
                val ny = centerY + 110f * density * sin(northRad).toFloat()
                drawLine(
                    color = AccentPrimary,
                    start = Offset(centerX, centerY),
                    end = Offset(nx, ny),
                    strokeWidth = 3.5f * density
                )
                drawCircle(color = AccentPrimary, radius = 8f * density, center = Offset(centerX, centerY))
            } else {
                // -------------------------------------------------------------
                // STANDARD AR / SKY VIEW & CONSTELLATION / ECLIPSE OVERLAYS
                // -------------------------------------------------------------

                // Draw Horizon Line
                val horizonY = (centerY + (currentAltitude * pixelsPerDegree)).toFloat()
                if (horizonY in -200f..(canvasHeight + 200f)) {
                    drawLine(
                        color = AccentPrimary.copy(alpha = 0.6f),
                        start = Offset(0f, horizonY),
                        end = Offset(canvasWidth, horizonY),
                        strokeWidth = 2.5f
                    )
                }

                // -------------------------------------------------------------
                // CONSTELLATION LINES OVERLAY
                // -------------------------------------------------------------
                if (selectedArMode == ArMode.CONSTELLATION_LINES || showConstellationLines) {
                    val lineAlpha = constellationLineOpacity
                    for (cData in constellationsData) {
                        for (linePair in cData.starIdsLines) {
                            val star1 = allStarsMap[linePair.first]
                            val star2 = allStarsMap[linePair.second]
                            if (star1 != null && star2 != null) {
                                val h1 = CoordinateEngine.equatorialToHorizontal(CoordinateEngine.Equatorial(star1.raDeg, star1.decDeg), lastDeg, uiState.userLocation.latitude)
                                val h2 = CoordinateEngine.equatorialToHorizontal(CoordinateEngine.Equatorial(star2.raDeg, star2.decDeg), lastDeg, uiState.userLocation.latitude)

                                var dAz1 = h1.azimuthDeg - currentAzimuth
                                if (dAz1 > 180) dAz1 -= 360
                                if (dAz1 < -180) dAz1 += 360
                                val dAlt1 = h1.altitudeDeg - currentAltitude

                                var dAz2 = h2.azimuthDeg - currentAzimuth
                                if (dAz2 > 180) dAz2 -= 360
                                if (dAz2 < -180) dAz2 += 360
                                val dAlt2 = h2.altitudeDeg - currentAltitude

                                val x1 = centerX + (dAz1 * pixelsPerDegree).toFloat()
                                val y1 = centerY - (dAlt1 * pixelsPerDegree).toFloat()
                                val x2 = centerX + (dAz2 * pixelsPerDegree).toFloat()
                                val y2 = centerY - (dAlt2 * pixelsPerDegree).toFloat()

                                if (x1 in -200f..(canvasWidth + 200f) && y1 in -200f..(canvasHeight + 200f) &&
                                    x2 in -200f..(canvasWidth + 200f) && y2 in -200f..(canvasHeight + 200f)) {
                                    drawLine(
                                        color = Color(0xFF38BDF8).copy(alpha = lineAlpha * 0.7f),
                                        start = Offset(x1, y1),
                                        end = Offset(x2, y2),
                                        strokeWidth = 2f * density
                                    )
                                }
                            }
                        }
                    }
                }

                // -------------------------------------------------------------
                // ECLIPSE PREVIEW AR OVERLAY
                // -------------------------------------------------------------
                if (selectedArMode == ArMode.ECLIPSE_PREVIEW) {
                    val sunAltAz = eclipseData.sunAltAz
                    val moonAltAz = eclipseData.moonAltAz

                    var dAzSun = sunAltAz.azimuthDeg - currentAzimuth
                    if (dAzSun > 180) dAzSun -= 360; if (dAzSun < -180) dAzSun += 360
                    val dAltSun = sunAltAz.altitudeDeg - currentAltitude
                    val sx = centerX + (dAzSun * pixelsPerDegree).toFloat()
                    val sy = centerY - (dAltSun * pixelsPerDegree).toFloat()

                    var dAzMoon = moonAltAz.azimuthDeg - currentAzimuth
                    if (dAzMoon > 180) dAzMoon -= 360; if (dAzMoon < -180) dAzMoon += 360
                    val dAltMoon = moonAltAz.altitudeDeg - currentAltitude
                    val mx = centerX + (dAzMoon * pixelsPerDegree).toFloat()
                    val my = centerY - (dAltMoon * pixelsPerDegree).toFloat()

                    // Render Sun Disc with Corona
                    drawCircle(color = Color(0xFFFF9E00).copy(alpha = 0.35f), radius = 55f * density, center = Offset(sx, sy))
                    drawCircle(color = Color(0xFFFFD166), radius = 30f * density, center = Offset(sx, sy))

                    // Render Moon Disc
                    drawCircle(color = Color(0xFF1E293B).copy(alpha = 0.92f), radius = 31f * density, center = Offset(mx, my))
                    drawCircle(color = Color.White.copy(alpha = 0.7f), radius = 31f * density, center = Offset(mx, my), style = Stroke(width = 1.5f * density))
                }

                // -------------------------------------------------------------
                // RENDER ALL CATALOG CELESTIAL OBJECTS
                // -------------------------------------------------------------
                val rollRad = Math.toRadians(-skyOrientation.roll.toDouble())
                val cosR = cos(rollRad).toFloat()
                val sinR = sin(rollRad).toFloat()

                for (obj in allCatalog) {
                    val horiz = when (obj.type) {
                        ObjectType.SUN -> sunHoriz
                        ObjectType.MOON -> moonHoriz
                        ObjectType.SATELLITE -> issHoriz
                        else -> CoordinateEngine.equatorialToHorizontal(CoordinateEngine.Equatorial(obj.raDeg, obj.decDeg), lastDeg, uiState.userLocation.latitude)
                    }

                    var dAz = horiz.azimuthDeg - currentAzimuth
                    if (dAz > 180) dAz -= 360
                    if (dAz < -180) dAz += 360
                    val dAlt = horiz.altitudeDeg - currentAltitude

                    val rawX = (dAz * pixelsPerDegree).toFloat()
                    val rawY = -(dAlt * pixelsPerDegree).toFloat()

                    val px = centerX + (rawX * cosR - rawY * sinR)
                    val py = centerY + (rawX * sinR + rawY * cosR)

                    if (px in -150f..(canvasWidth + 150f) && py in -150f..(canvasHeight + 150f)) {
                        val isSelected = obj.id == selectedTarget?.id
                        val isTargetedNearReticle = obj.id == targetedObject?.id
                        val baseSizeDp = CelestialObjectSizes.getBaseSizeDp(obj)
                        val radiusPx = (baseSizeDp / 2f) * density

                        when (obj.type) {
                            ObjectType.SUN -> {
                                drawCircle(color = Color(0xFFFF8800).copy(alpha = 0.3f), radius = radiusPx * 2.2f, center = Offset(px, py))
                                drawCircle(color = Color(0xFFFFFFFF), radius = radiusPx, center = Offset(px, py))
                            }
                            ObjectType.MOON -> {
                                drawCircle(color = AccentPrimary.copy(alpha = 0.35f), radius = radiusPx * 1.8f, center = Offset(px, py))
                                drawCircle(color = Color(0xFFF1FAEE), radius = radiusPx, center = Offset(px, py))
                            }
                            ObjectType.PLANET -> {
                                drawCircle(color = Color(0xFFFFD166).copy(alpha = 0.35f), radius = radiusPx * 1.8f, center = Offset(px, py))
                                drawCircle(color = Color(0xFFFFD166), radius = radiusPx, center = Offset(px, py))
                            }
                            else -> {
                                drawCircle(color = Color.White, radius = radiusPx, center = Offset(px, py))
                            }
                        }

                        if (isSelected || isTargetedNearReticle) {
                            drawCircle(color = AccentPrimary, radius = radiusPx + 12f, center = Offset(px, py), style = Stroke(width = 2.5f))
                        }
                    }
                }

                // Center Reticle Frame
                drawCircle(color = AccentPrimary, radius = 55f, center = Offset(centerX, centerY), style = Stroke(width = 2.5f))
                drawCircle(color = Color.White.copy(alpha = 0.3f), radius = 90f, center = Offset(centerX, centerY), style = Stroke(width = 1.5f))
            }
        }

        // LAYER 3: Target Finder Arrow Overlay
        finderData?.let { finder ->
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(240.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate((finder.arrowAngleRad * 180f / PI.toFloat()))
                ) {
                    val cX = size.width / 2f
                    val cY = size.height / 2f

                    drawCircle(color = AccentPrimary.copy(alpha = 0.25f), radius = 100f, center = Offset(cX, cY), style = Stroke(width = 5f))
                    val arrowPath = Path().apply {
                        moveTo(cX, cY - 85f)
                        lineTo(cX + 18f, cY - 58f)
                        lineTo(cX + 7f, cY - 60f)
                        lineTo(cX + 7f, cY - 40f)
                        lineTo(cX - 7f, cY - 40f)
                        lineTo(cX - 7f, cY - 60f)
                        lineTo(cX - 18f, cY - 58f)
                        close()
                    }
                    drawPath(path = arrowPath, color = AccentPrimary)
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 55.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = BackgroundCard.copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isFa) finder.instructionFa else finder.instructionEn,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = String.format(if (isFa) "فاصله: %.1f°" else "Dist: %.1f°", finder.totalAngularDistanceDeg),
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentPrimary
                        )
                    }
                }
            }
        }

        // LAYER 3.5: SOLAR SYSTEM AR (SSA) FULLSCREEN OVERLAY
        if (isSsaActive) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedSsaSubMode) {
                    SsaSubMode.SCALE_WALK -> ScaleWalkView(
                        activeTimeMs = activeTimeMs,
                        userLatDeg = uiState.userLocation.latitude,
                        userLonDeg = uiState.userLocation.longitude,
                        isFa = isFa,
                        onOpenObjectDetail = { planetType ->
                            val match = allCatalog.find { it.nameEn.lowercase() == planetType.nameEn.lowercase() }
                            match?.let { selectedTarget = it }
                        }
                    )
                    SsaSubMode.SPACE_TIME -> SpaceTimeExplorerView(
                        initialTimeMs = activeTimeMs,
                        isFa = isFa
                    )
                    SsaSubMode.GRAVITY_SANDBOX -> GravitySandboxView(
                        isFa = isFa
                    )
                }
            }
        }

        // LAYER 4: TOP CONTROL SYSTEM (5 PILLS anchored at top under status bar)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 2.dp, start = 8.dp, end = 8.dp)
                .fillMaxWidth()
        ) {
            // HORIZONTAL ROW OF 5 PILLS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pill 1: Cam/GPS
                ArTopPill(
                    icon = Icons.Default.Sensors,
                    label = if (isFa) "دوربین / GPS" else "Cam/GPS",
                    isActive = activeExpandedPill == ArPillType.CAM_GPS,
                    isHighlighted = isGpsActive && isSensorActive,
                    onClick = { activeExpandedPill = if (activeExpandedPill == ArPillType.CAM_GPS) null else ArPillType.CAM_GPS }
                )

                // Pill 2: Time Machine
                ArTopPill(
                    icon = Icons.Default.Schedule,
                    label = if (isFa) "زمان" else "Time",
                    isActive = activeExpandedPill == ArPillType.TIME,
                    isHighlighted = timeMachineState.mode == TimeMachineMode.SIMULATION,
                    onClick = { activeExpandedPill = if (activeExpandedPill == ArPillType.TIME) null else ArPillType.TIME }
                )

                // Pill 3: SSA (Solar System AR)
                ArTopPill(
                    icon = Icons.Default.Public,
                    label = if (isFa) "منظومه SSA" else "SSA",
                    isActive = activeExpandedPill == ArPillType.SSA,
                    isHighlighted = isSsaActive,
                    onClick = { activeExpandedPill = if (activeExpandedPill == ArPillType.SSA) null else ArPillType.SSA }
                )

                // Pill 4: Modes
                ArTopPill(
                    icon = Icons.Default.Layers,
                    label = if (isFa) "حالت‌ها" else "Modes",
                    isActive = activeExpandedPill == ArPillType.MODES,
                    isHighlighted = selectedArMode != ArMode.SKY_VIEW,
                    onClick = { activeExpandedPill = if (activeExpandedPill == ArPillType.MODES) null else ArPillType.MODES }
                )

                // Pill 5: Search
                ArTopPill(
                    icon = Icons.Default.Search,
                    label = if (isFa) "جستجو" else "Search",
                    isActive = activeExpandedPill == ArPillType.SEARCH,
                    isHighlighted = selectedTarget != null,
                    onClick = { activeExpandedPill = if (activeExpandedPill == ArPillType.SEARCH) null else ArPillType.SEARCH }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // EXPANDED PANEL CONTAINER (Renders only ONE expanded panel at a time)
            AnimatedVisibility(
                visible = activeExpandedPill != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = BackgroundCard.copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, CardBorder),
                    shadowElevation = 14.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val pill = activeExpandedPill
                    if (pill != null) {
                        when (pill) {
                        // -------------------------------------------------------------
                        // PANEL 1: CAM / GPS
                        // -------------------------------------------------------------
                        ArPillType.CAM_GPS -> {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isFa) "مدیریت دوربین و حسگرها" else "Camera & Telemetry Controls",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    TextButton(onClick = { activeTutorialTopic = ARTutorialTopic.CAM_GPS }) {
                                        Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = AccentPrimary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isFa) "آموزش" else "Tutorial", style = MaterialTheme.typography.labelSmall, color = AccentPrimary)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilterChip(
                                        selected = isGpsActive,
                                        onClick = {
                                            if (!hasLocationPermission) locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                                            isGpsActive = !isGpsActive
                                        },
                                        label = { Text(if (isFa) "GPS زنده" else "Live GPS") },
                                        leadingIcon = { Icon(if (isGpsActive) Icons.Default.GpsFixed else Icons.Default.GpsOff, null, modifier = Modifier.size(16.dp)) }
                                    )

                                    FilterChip(
                                        selected = isSensorActive,
                                        onClick = { isSensorActive = !isSensorActive },
                                        label = { Text(if (isFa) "ژیروسکوپ" else "Gyroscope") },
                                        leadingIcon = { Icon(if (isSensorActive) Icons.Default.Sensors else Icons.Default.SensorsOff, null, modifier = Modifier.size(16.dp)) }
                                    )

                                    FilterChip(
                                        selected = isCameraEnabled,
                                        onClick = {
                                            if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
                                            isCameraEnabled = !isCameraEnabled
                                        },
                                        label = { Text(if (isFa) "دوربین" else "Camera") },
                                        leadingIcon = { Icon(if (isCameraEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff, null, modifier = Modifier.size(16.dp)) }
                                    )
                                }

                                // Status Telemetry
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = StatusGood.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, StatusGood.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = if (isFa) "• دقت موقعیت‌یاب: ${gpsAccuracyMeters?.let { String.format("%.0fm", it) } ?: "عالی"}" else "• GPS Precision: ${gpsAccuracyMeters?.let { String.format("%.0fm", it) } ?: "High"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = String.format(if (isFa) "• سمت: %.1f° | ارتفاع: %.1f°" else "• Az: %.1f° | Alt: %.1f°", currentAzimuth, currentAltitude),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }
                        }

                        // -------------------------------------------------------------
                        // PANEL 2: TIME MACHINE
                        // -------------------------------------------------------------
                        ArPillType.TIME -> {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isFa) "ماشین زمان رصد" else "Observational Time Machine",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    TextButton(onClick = { activeTutorialTopic = ARTutorialTopic.TIME_MACHINE }) {
                                        Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = AccentPrimary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isFa) "آموزش" else "Tutorial", style = MaterialTheme.typography.labelSmall, color = AccentPrimary)
                                    }
                                }

                                TimeMachineControlBar(
                                    state = timeMachineState,
                                    isFa = isFa,
                                    calendarSystem = uiState.calendarSystem,
                                    onSimulatedTimeChange = { timeMs, eventName, isBirthday -> viewModel.setSimulatedTime(timeMs, eventName, isBirthday) },
                                    onModeChange = { mode -> viewModel.setTimeMachineMode(mode) },
                                    onTogglePlay = { viewModel.toggleTimeMachinePlaying() },
                                    onSpeedChange = { speed -> viewModel.setTimeMachineSpeed(speed) },
                                    onToggleReverse = { viewModel.toggleTimeMachineReverse() },
                                    onToggleExpanded = { viewModel.toggleTimeMachineExpanded() },
                                    onReturnToLive = { handleReturnToLive() }
                                )
                            }
                        }

                        // -------------------------------------------------------------
                        // PANEL 3: SOLAR SYSTEM AR (SSA)
                        // -------------------------------------------------------------
                        ArPillType.SSA -> {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.Public, contentDescription = null, tint = AccentPrimary, modifier = Modifier.size(20.dp))
                                        Text(
                                            text = if (isFa) "معماری واقعیت افزوده منظومه شمسی (SSA)" else "Solar System AR Architecture",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    }
                                    TextButton(onClick = { activeTutorialTopic = ARTutorialTopic.SSA }) {
                                        Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = AccentPrimary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isFa) "آموزش" else "Tutorial", style = MaterialTheme.typography.labelSmall, color = AccentPrimary)
                                    }
                                }

                                Text(
                                    text = if (isFa) "نمایش مدارهای ۳بعدی، موقعیت کپلری سیارات، ماهواره‌های گالیله‌ای و مقیاس فواصل نجومی در فضا."
                                    else "Renders 3D Keplerian planetary orbits, Galilean moons, and distance scaling directly in sky view.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )

                                // SSA Submodes Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    SsaSubMode.values().forEach { subMode ->
                                        FilterChip(
                                            selected = selectedSsaSubMode == subMode,
                                            onClick = {
                                                selectedSsaSubMode = subMode
                                                isSsaActive = true
                                            },
                                            label = { Text(if (isFa) subMode.labelFa else subMode.labelEn, fontSize = 11.sp) },
                                            leadingIcon = { Icon(subMode.icon, null, modifier = Modifier.size(14.dp)) }
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilterChip(
                                        selected = ssaShow3dOrbits,
                                        onClick = { ssaShow3dOrbits = !ssaShow3dOrbits },
                                        label = { Text(if (isFa) "مدارهای ۳بعدی" else "3D Orbits") },
                                        leadingIcon = { Icon(Icons.Default.Public, null, modifier = Modifier.size(16.dp)) }
                                    )

                                    FilterChip(
                                        selected = isSsaActive,
                                        onClick = { isSsaActive = !isSsaActive },
                                        label = { Text(if (isFa) if (isSsaActive) "فعال" else "غیرفعال" else if (isSsaActive) "Active" else "Inactive") },
                                        leadingIcon = { Icon(if (isSsaActive) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null, modifier = Modifier.size(16.dp)) }
                                    )
                                }
                            }
                        }

                        // -------------------------------------------------------------
                        // PANEL 4: MODES (SKY, COMPASS, CONSTELLATIONS, ECLIPSE)
                        // -------------------------------------------------------------
                        ArPillType.MODES -> {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isFa) "حالت‌های دید و رصد AR" else "AR Visual Modes",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    TextButton(onClick = { activeTutorialTopic = ARTutorialTopic.MODES }) {
                                        Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = AccentPrimary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isFa) "آموزش" else "Tutorial", style = MaterialTheme.typography.labelSmall, color = AccentPrimary)
                                    }
                                }

                                // Mode Switcher Segmented Chips
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(ArMode.values()) { mode ->
                                        FilterChip(
                                            selected = selectedArMode == mode,
                                            onClick = { selectedArMode = mode },
                                            label = { Text(if (isFa) mode.labelFa else mode.labelEn) },
                                            leadingIcon = { Icon(mode.icon, null, modifier = Modifier.size(16.dp)) }
                                        )
                                    }
                                }

                                // Mode-Specific Extra Controls
                                when (selectedArMode) {
                                    ArMode.CONSTELLATION_LINES -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = if (isFa) "شفافیت خطوط صورت‌های فلکی" else "Constellation Line Opacity",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextSecondary
                                                )
                                                Text(
                                                    text = "${(constellationLineOpacity * 100).toInt()}%",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = AccentPrimary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Slider(
                                                value = constellationLineOpacity,
                                                onValueChange = { constellationLineOpacity = it },
                                                valueRange = 0.2f..1.0f,
                                                colors = SliderDefaults.colors(thumbColor = AccentPrimary, activeTrackColor = AccentPrimary)
                                            )
                                        }
                                    }

                                    ArMode.ECLIPSE_PREVIEW -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = if (isFa) "انتخاب خورشیدگرفتگی / ماه گرفتگی پیش‌فرض:" else "Select Preset Eclipse Event:",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary
                                            )
                                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                items(EclipseEngine.PRESET_ECLIPSES) { preset ->
                                                    FilterChip(
                                                        selected = selectedEclipsePreset.id == preset.id,
                                                        onClick = { selectedEclipsePreset = preset },
                                                        label = { Text(if (isFa) preset.nameFa else preset.nameEn) }
                                                    )
                                                }
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = AccentSecondary.copy(alpha = 0.15f),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = if (isFa) "میزان پوشش محاسبه‌شده: ${String.format("%.1f%%", eclipseData.coveragePercentage)}"
                                                    else "Calculated Coverage Obscuration: ${String.format("%.1f%%", eclipseData.coveragePercentage)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = AccentSecondary,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(8.dp)
                                                )
                                            }
                                        }
                                    }

                                    else -> {}
                                }
                            }
                        }

                        // -------------------------------------------------------------
                        // PANEL 5: SEARCH
                        // -------------------------------------------------------------
                        ArPillType.SEARCH -> {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Default.Search, contentDescription = "Search", tint = AccentPrimary, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        TextField(
                                            value = searchQuery,
                                            onValueChange = { searchQuery = it },
                                            placeholder = { Text(if (isFa) "جستجوی جرم (ماه، مریخ، شباهنگ...)" else "Search target (Moon, Mars, Sirius...)", style = MaterialTheme.typography.bodyMedium, color = TextSecondary) },
                                            singleLine = true,
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                                                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent
                                            ),
                                            modifier = Modifier.fillMaxWidth().testTag("ar_search_input")
                                        )
                                    }
                                    TextButton(onClick = { activeTutorialTopic = ARTutorialTopic.SEARCH }) {
                                        Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = AccentPrimary)
                                    }
                                }

                                selectedTarget?.let { target ->
                                    HorizontalDivider(color = CardBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isFa) "هدف قفل‌شده: ${target.nameFa}" else "Locked Target: ${target.nameEn}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = AccentPrimary
                                        )
                                        TextButton(onClick = { selectedTarget = null }) {
                                            Text(if (isFa) "حذف هدف" else "Clear Target", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                        }
                                    }
                                }

                                if (searchResults.isNotEmpty()) {
                                    HorizontalDivider(color = CardBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
                                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp).fillMaxWidth()) {
                                        items(searchResults) { result ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        selectedTarget = result.celestialObject
                                                        searchQuery = ""
                                                        activeExpandedPill = null
                                                    }
                                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(if (isFa) result.celestialObject.nameFa else result.celestialObject.nameEn, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                                                    Text(if (isFa) result.celestialObject.constellationFa else result.celestialObject.constellationEn, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = if (result.isVisibleNow) StatusGood.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f),
                                                    border = BorderStroke(1.dp, if (result.isVisibleNow) StatusGood else Color.Gray)
                                                ) {
                                                    Text(if (isFa) result.statusFa else result.statusEn, style = MaterialTheme.typography.labelSmall, color = if (result.isVisibleNow) StatusGood else TextSecondary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    HorizontalDivider(color = CardBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(CelestialSearchEngine.getQuickSuggestions()) { sug ->
                                            SuggestionChip(
                                                onClick = { searchQuery = sug },
                                                label = { Text(sug) },
                                                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = NavyBackground)
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
        }

        // LAYER 5: Target Object Information Card at Bottom
        targetedObject?.let { obj ->
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 80.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth()
                    .clickable { viewModel.openObjectDetail(obj) }
                    .testTag("ar_targeted_object_card"),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, CardBorder),
                color = BackgroundCard.copy(alpha = 0.92f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.GpsFixed, contentDescription = "Identified", tint = AccentPrimary, modifier = Modifier.size(20.dp))
                            Text(if (isFa) obj.nameFa else obj.nameEn, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        val constName = if (isFa) obj.constellationFa else obj.constellationEn
                        Text("$constName • ${obj.category}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }

                    Button(
                        onClick = { viewModel.openObjectDetail(obj) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(if (isFa) "جزئیات" else "Details", color = Color.White)
                    }
                }
            }
        }

        // LAYER 6: Target Arrival Celebration Dialog
        if (showArrivalDialog && selectedTarget != null) {
            AlertDialog(
                onDismissRequest = { showArrivalDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🎯", fontSize = 24.sp)
                        Text(if (isFa) "هدف پیدا شد!" else "Target Acquired!", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (isFa) "شما جرم «${selectedTarget?.nameFa}» را با موفقیت در آسمان ردیابی کردید." else "You successfully tracked ${selectedTarget?.nameEn} in the sky.", style = MaterialTheme.typography.bodyMedium)
                        Text(if (isFa) selectedTarget?.descriptionFa ?: "" else selectedTarget?.descriptionEn ?: "", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showArrivalDialog = false
                            selectedTarget?.let { viewModel.openObjectDetail(it) }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                    ) {
                        Text(if (isFa) "مشاهده شناسنامه جرم" else "View Details")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showArrivalDialog = false }) {
                        Text(if (isFa) "بستن" else "Close")
                    }
                },
                containerColor = NavyBackground,
                shape = RoundedCornerShape(24.dp)
            )
        }

        // LAYER 7: INTERACTIVE TUTORIAL OVERLAY MODAL
        activeTutorialTopic?.let { topic ->
            ARTutorialModal(
                topic = topic,
                isFa = isFa,
                onDismiss = { activeTutorialTopic = null }
            )
        }
    }
}

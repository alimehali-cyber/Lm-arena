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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
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
import com.alijafari.red.astronomy.domain.*
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.MainViewModel
import com.alijafari.red.astronomy.ui.components.TimeMachineControlBar
import com.alijafari.red.astronomy.ui.rendering.*
import com.alijafari.red.astronomy.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.PathEffect
import kotlin.math.*

enum class ArExpandedPanel {
    SEARCH, TIME_MACHINE, SENSORS
}

@Composable
private fun ArSmartPill(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    isHighlighted: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (isActive) AccentPrimary else if (isHighlighted) AccentSecondary.copy(alpha = 0.88f) else BackgroundCard.copy(alpha = 0.85f),
        border = BorderStroke(
            1.dp,
            if (isActive) Color.White.copy(alpha = 0.6f) else if (isHighlighted) AccentSecondary else CardBorder
        ),
        shadowElevation = 6.dp,
        modifier = Modifier.height(38.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
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
                color = if (isActive || isHighlighted) Color.White else TextPrimary
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

    // Keep screen awake
    val activity = context as? Activity
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Sensor Fusion & Orientation Provider
    val orientationProvider = remember { OrientationProvider(context) }
    val skyOrientation by orientationProvider.orientation.collectAsState()
    val calibrationState by orientationProvider.calibrationState.collectAsState()

    // Camera & Location Permission State
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
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        hasLocationPermission = fine || coarse
    }

    // Live GPS State
    var isGpsActive by remember { mutableStateOf(true) }
    var gpsAccuracyMeters by remember { mutableStateOf<Float?>(null) }

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

    var isSensorActive by remember { mutableStateOf(true) }
    var isCameraEnabled by remember { mutableStateOf(true) }

    // Manual Drag Fallback when sensors paused
    var manualAzimuthOffset by remember { mutableStateOf(0.0) }
    var manualPitchOffset by remember { mutableStateOf(0.0) }

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

    val moonData = remember(jd) { MoonEngine.calculateMoon(jd) }
    val moonHoriz = remember(moonData, lastDeg, uiState.userLocation) {
        CoordinateEngine.equatorialToHorizontal(
            CoordinateEngine.Equatorial(moonData.raDeg, moonData.decDeg),
            lastDeg,
            uiState.userLocation.latitude
        )
    }

    // Live TLE background fetch for ISS orbital precision
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            ISSEngine.fetchLatestTLE()
        }
    }

    // Real-Time ISS Topocentric Position
    val issTopocentric = remember(activeTimeMs, uiState.userLocation) {
        ISSEngine.calculateTopocentricPos(
            timestampMs = activeTimeMs,
            userLatDeg = uiState.userLocation.latitude,
            userLonDeg = uiState.userLocation.longitude,
            userAltMeters = uiState.userLocation.elevationMeters
        )
    }

    val issHoriz = remember(issTopocentric) {
        CoordinateEngine.Horizontal(
            altitudeDeg = issTopocentric.elevationDeg,
            azimuthDeg = issTopocentric.azimuthDeg
        )
    }

    // Real-Time ISS Orbital Trajectory Points (Sampled across current pass window)
    val issTrajectoryPoints = remember(activeTimeMs / 10000L, uiState.userLocation) {
        val points = mutableListOf<CoordinateEngine.Horizontal>()
        for (offsetSec in -600..600 step 20) {
            val t = activeTimeMs + (offsetSec * 1000L)
            val pos = ISSEngine.calculateTopocentricPos(
                timestampMs = t,
                userLatDeg = uiState.userLocation.latitude,
                userLonDeg = uiState.userLocation.longitude,
                userAltMeters = uiState.userLocation.elevationMeters
            )
            if (pos.elevationDeg > -10.0) {
                points.add(CoordinateEngine.Horizontal(altitudeDeg = pos.elevationDeg, azimuthDeg = pos.azimuthDeg))
            }
        }
        points
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

    // Closest object near reticle for tap / lock (sampled at 0.2 deg steps to eliminate micro-jitter calculations)
    val stepAzimuth = remember(currentAzimuth) { (currentAzimuth * 5).roundToInt() / 5.0 }
    val stepAltitude = remember(currentAltitude) { (currentAltitude * 5).roundToInt() / 5.0 }

    val targetedObject = remember(stepAzimuth, stepAltitude, lastDeg, uiState.userLocation, allCatalog) {
        var closestObj: CelestialObject? = null
        var minDistance = 12.0 // deg

        for (obj in allCatalog) {
            val horiz = CoordinateEngine.equatorialToHorizontal(
                CoordinateEngine.Equatorial(obj.raDeg, obj.decDeg),
                lastDeg,
                uiState.userLocation.latitude
            )
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
            .pointerInput(activeExpandedPanel) {
                detectTapGestures {
                    if (activeExpandedPanel != null) {
                        activeExpandedPanel = null
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
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
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
                    detectTapGestures { tapOffset ->
                        // Tap to identify
                        targetedObject?.let { obj ->
                            selectedTarget = obj
                        }
                    }
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerX = canvasWidth / 2f
            val centerY = canvasHeight / 2f

            val fovX = 60.0
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
            val horizonY = (centerY + (currentAltitude * pixelsPerDegree)).toFloat()
            if (horizonY in -200f..(canvasHeight + 200f)) {
                drawLine(
                    color = AccentPrimary.copy(alpha = 0.6f),
                    start = Offset(0f, horizonY),
                    end = Offset(canvasWidth, horizonY),
                    strokeWidth = 2.5f
                )
                val horizonLabel = if (isFa) "افق (0°)" else "Horizon (0°)"
                val horizonTextLayout = textMeasurer.measure(
                    text = horizonLabel,
                    style = TextStyle(color = AccentPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    softWrap = false
                )
                val drawX = 20f
                val drawY = horizonY - 30f

                if (
                    drawX.isFinite() &&
                    drawY.isFinite() &&
                    drawX < canvasWidth &&
                    drawY < canvasHeight &&
                    drawX + horizonTextLayout.size.width > 0f &&
                    drawY + horizonTextLayout.size.height > 0f
                ) {
                    drawText(
                        textLayoutResult = horizonTextLayout,
                        topLeft = Offset(drawX, drawY)
                    )
                }
            }

            // Render Trajectory Orbit Arc for Moon
            val moonOrbitPath = Path()
            var firstPoint = true
            for (hourOffset in -6..6) {
                val hourJd = jd + (hourOffset / 24.0)
                val hourLast = TimeEngine.getLAST(hourJd, uiState.userLocation.longitude)
                val hourMoon = MoonEngine.calculateMoon(hourJd)
                val hourHoriz = CoordinateEngine.equatorialToHorizontal(
                    CoordinateEngine.Equatorial(hourMoon.raDeg, hourMoon.decDeg),
                    hourLast,
                    uiState.userLocation.latitude
                )
                var dAzH = hourHoriz.azimuthDeg - currentAzimuth
                if (dAzH > 180) dAzH -= 360
                if (dAzH < -180) dAzH += 360
                val dAltH = hourHoriz.altitudeDeg - currentAltitude

                val ox = (centerX + (dAzH * pixelsPerDegree)).toFloat()
                val oy = (centerY - (dAltH * pixelsPerDegree)).toFloat()

                if (firstPoint) {
                    moonOrbitPath.moveTo(ox, oy)
                    firstPoint = false
                } else {
                    moonOrbitPath.lineTo(ox, oy)
                }
            }
            drawPath(
                path = moonOrbitPath,
                color = AccentPrimary.copy(alpha = 0.35f),
                style = Stroke(width = 2f)
            )

            // Render Galactic Equator Plane (Milky Way Arch)
            val galPoints = GalacticEngine.calculateGalacticPlanePoints(
                jd = jd,
                userLatDeg = uiState.userLocation.latitude,
                userLonDeg = uiState.userLocation.longitude
            )
            val galPath = Path()
            var galFirst = true
            for (gp in galPoints) {
                var dAzG = gp.azimuthDeg - currentAzimuth
                if (dAzG > 180) dAzG -= 360
                if (dAzG < -180) dAzG += 360
                val dAltG = gp.altitudeDeg - currentAltitude

                val gx = (centerX + (dAzG * pixelsPerDegree)).toFloat()
                val gy = (centerY - (dAltG * pixelsPerDegree)).toFloat()

                if (galFirst) {
                    galPath.moveTo(gx, gy)
                    galFirst = false
                } else {
                    galPath.lineTo(gx, gy)
                }
            }
            drawPath(
                path = galPath,
                color = Color(0xFFC084FC).copy(alpha = 0.4f),
                style = Stroke(width = 2.5f)
            )

            // Render All Catalog Objects (including Sun, Moon, Planets, Stars, Satellites)
            val rollRad = Math.toRadians(-skyOrientation.roll.toDouble())
            val cosR = cos(rollRad).toFloat()
            val sinR = sin(rollRad).toFloat()

            // Render ISS Real-Time Orbital Trajectory Pass Line
            if (issTrajectoryPoints.size >= 2) {
                val issPath = Path()
                var issFirst = true
                for (pt in issTrajectoryPoints) {
                    var dAzI = pt.azimuthDeg - currentAzimuth
                    if (dAzI > 180) dAzI -= 360
                    if (dAzI < -180) dAzI += 360
                    val dAltI = pt.altitudeDeg - currentAltitude

                    val rawX = (dAzI * pixelsPerDegree).toFloat()
                    val rawY = -(dAltI * pixelsPerDegree).toFloat()

                    val ix = centerX + (rawX * cosR - rawY * sinR)
                    val iy = centerY + (rawX * sinR + rawY * cosR)

                    if (ix in -400f..(canvasWidth + 400f) && iy in -400f..(canvasHeight + 400f)) {
                        if (issFirst) {
                            issPath.moveTo(ix, iy)
                            issFirst = false
                        } else {
                            issPath.lineTo(ix, iy)
                        }
                    }
                }
                if (!issFirst) {
                    drawPath(
                        path = issPath,
                        color = Color(0xFFFF9E00).copy(alpha = 0.75f),
                        style = Stroke(
                            width = 3f * density,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f * density, 8f * density), 0f)
                        )
                    )
                }
            }

            for (obj in allCatalog) {
                val horiz = if (obj.type == ObjectType.SUN) {
                    sunHoriz
                } else if (obj.type == ObjectType.MOON) {
                    moonHoriz
                } else if (obj.id == "sat_iss" || obj.type == ObjectType.SATELLITE) {
                    issHoriz
                } else {
                    CoordinateEngine.equatorialToHorizontal(
                        CoordinateEngine.Equatorial(obj.raDeg, obj.decDeg),
                        lastDeg,
                        uiState.userLocation.latitude
                    )
                }

                var dAz = horiz.azimuthDeg - currentAzimuth
                if (dAz > 180) dAz -= 360
                if (dAz < -180) dAz += 360
                val dAlt = horiz.altitudeDeg - currentAltitude

                val rawX = (dAz * pixelsPerDegree).toFloat()
                val rawY = -(dAlt * pixelsPerDegree).toFloat()

                val px = centerX + (rawX * cosR - rawY * sinR)
                val py = centerY + (rawX * sinR + rawY * cosR)

                // Padding boundary check
                if (px in -150f..(canvasWidth + 150f) && py in -150f..(canvasHeight + 150f)) {
                    val isSelected = obj.id == selectedTarget?.id
                    val isTargetedNearReticle = obj.id == targetedObject?.id

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
                            // Multi-layer corona radial glow
                            drawCircle(
                                color = Color(0xFFFF8800).copy(alpha = 0.3f),
                                radius = radiusPx * 2.2f,
                                center = Offset(px, py)
                            )
                            drawCircle(
                                color = Color(0xFFFFDD44).copy(alpha = 0.6f),
                                radius = radiusPx * 1.5f,
                                center = Offset(px, py)
                            )
                            drawCircle(
                                color = Color(0xFFFFFFFF),
                                radius = radiusPx,
                                center = Offset(px, py)
                            )
                        }
                        ObjectType.MOON -> {
                            // Moon halo & Disc
                            drawCircle(
                                color = AccentPrimary.copy(alpha = 0.35f),
                                radius = radiusPx * 1.8f,
                                center = Offset(px, py)
                            )
                            drawCircle(
                                color = Color(0xFFF1FAEE),
                                radius = radiusPx,
                                center = Offset(px, py)
                            )
                            val illumFrac = (moonData.illuminationPercent / 100.0).toFloat()
                            drawCircle(
                                color = AccentSecondary.copy(alpha = 0.75f),
                                radius = radiusPx * illumFrac,
                                center = Offset(px, py)
                            )
                        }
                        ObjectType.PLANET -> {
                            drawCircle(
                                color = Color(0xFFFFD166).copy(alpha = 0.35f),
                                radius = radiusPx * 2.0f,
                                center = Offset(px, py)
                            )
                            drawCircle(
                                color = Color(0xFFFFD166),
                                radius = radiusPx,
                                center = Offset(px, py)
                            )
                        }
                        ObjectType.SATELLITE -> {
                            drawCircle(
                                color = StatusWarning.copy(alpha = 0.5f),
                                radius = radiusPx * 1.8f,
                                center = Offset(px, py)
                            )
                            drawCircle(
                                color = StatusWarning,
                                radius = radiusPx,
                                center = Offset(px, py)
                            )
                        }
                        ObjectType.DEEP_SKY -> {
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
                        else -> {
                            // Star
                            val starColor = if (obj.magnitude <= 1.0) Color(0xFF90E0EF) else Color.White
                            drawCircle(
                                color = starColor,
                                radius = radiusPx,
                                center = Offset(px, py)
                            )
                        }
                    }

                    // Selected / Centered pulsing ring
                    if (isSelected || isTargetedNearReticle) {
                        drawCircle(
                            color = AccentPrimary,
                            radius = radiusPx + 14f,
                            center = Offset(px, py),
                            style = Stroke(width = 2.5f)
                        )
                    }

                    // Label with dark backing pill (if bright star or selected or planet)
                    if (obj.magnitude <= CelestialObjectSizes.LABEL_SHOW_MAGNITUDE_THRESHOLD || isSelected || obj.type != ObjectType.STAR) {
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
                                color = Color.Black.copy(alpha = 0.65f),
                                topLeft = Offset(pillLeft, pillTop),
                                size = Size(pillWidth, pillHeight),
                                cornerRadius = CornerRadius(12f, 12f)
                            )

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

        // Layer 3: Arrow-Guided Finder Overlay (When an object is selected)
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
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 60.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = BackgroundCard.copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isFa) finder.instructionFa else finder.instructionEn,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        val distStr = String.format("%.1f°", finder.totalAngularDistanceDeg)
                        Text(
                            text = if (isFa) "فاصله تا هدف: ${TimeEngine.formatPersianNumbers(distStr)}" else "Distance: $distStr",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentPrimary
                        )
                    }
                }
            }
        }

        // Layer 4: Focus Mode Smart Floating Pills Row (Positioned at top)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 4.dp, start = 12.dp, end = 12.dp)
                .fillMaxWidth()
        ) {
            // SMART FLOATING PILLS ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
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
                    label = if (isFa) "ماشین زمان" else "Time Machine",
                    isActive = activeExpandedPanel == ArExpandedPanel.TIME_MACHINE,
                    isHighlighted = timeMachineState.mode == TimeMachineMode.SIMULATION,
                    onClick = {
                        activeExpandedPanel = if (activeExpandedPanel == ArExpandedPanel.TIME_MACHINE) null else ArExpandedPanel.TIME_MACHINE
                    }
                )

                // Pill 3: Sensors / GPS
                ArSmartPill(
                    icon = Icons.Default.Sensors,
                    label = if (isFa) "حسگرها / GPS" else "Camera / GPS",
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
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = BackgroundCard.copy(alpha = 0.94f),
                    border = BorderStroke(1.dp, CardBorder),
                    shadowElevation = 12.dp,
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
                                        tint = AccentPrimary,
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
                                                color = TextSecondary
                                            )
                                        },
                                        singleLine = true,
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent
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
                                    HorizontalDivider(color = CardBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
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
                                                        color = TextPrimary
                                                    )
                                                    Text(
                                                        text = if (isFa) result.celestialObject.constellationFa else result.celestialObject.constellationEn,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = TextSecondary
                                                    )
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = if (result.isVisibleNow) StatusGood.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f),
                                                    border = BorderStroke(1.dp, if (result.isVisibleNow) StatusGood else Color.Gray)
                                                ) {
                                                    Text(
                                                        text = if (isFa) result.statusFa else result.statusEn,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (result.isVisibleNow) StatusGood else TextSecondary,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    HorizontalDivider(color = CardBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
                                    Text(
                                        text = if (isFa) "پیشنهادهای سریع:" else "Quick Suggestions:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
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

                        ArExpandedPanel.TIME_MACHINE -> {
                            // Time Machine Controls Panel Content
                            TimeMachineControlBar(
                                state = timeMachineState,
                                isFa = isFa,
                                calendarSystem = uiState.calendarSystem,
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

                        ArExpandedPanel.SENSORS -> {
                            // Sensors & GPS Control Panel Content
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = if (isFa) "مدیریت حسگرها و موقعیت‌یاب" else "Sensors & Telemetry Control",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
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
                                            }
                                            isGpsActive = !isGpsActive
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
                                            }
                                            isCameraEnabled = !isCameraEnabled
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
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = if (isFa) "• موقعیت: ${TimeEngine.formatPersianNumbers(String.format("%.2f°", uiState.userLocation.latitude))}, ${TimeEngine.formatPersianNumbers(String.format("%.2f°", uiState.userLocation.longitude))}"
                                                    else "• Location: ${String.format("%.2f°", uiState.userLocation.latitude)}, ${String.format("%.2f°", uiState.userLocation.longitude)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = if (isFa) "• وضعیت کالیبراسیون: ${calibrationState.name}" else "• Calibration: ${calibrationState.name}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextPrimary
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

        // Calibration Warning Banner (if sensor uncalibrated)
        if (calibrationState == CalibrationState.NEEDS_CALIBRATION || calibrationState == CalibrationState.POOR) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 150.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = AccentSecondary.copy(alpha = 0.9f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CompassCalibration,
                        contentDescription = "Calibrate",
                        tint = Color.White
                    )
                    Text(
                        text = if (isFa) "دقت قطب‌نما پایین است. گوشی خود را به صورت عدد 8 انگلیسی حرکت دهید"
                        else "Compass precision low. Move phone in a figure-8 pattern to calibrate",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }

        // Layer 6: Target Object Information Card at Bottom
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GpsFixed,
                                contentDescription = "Identified",
                                tint = AccentPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (isFa) obj.nameFa else obj.nameEn,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        val constName = if (isFa) obj.constellationFa else obj.constellationEn
                        Text(
                            text = "$constName • ${obj.category}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }

                    Button(
                        onClick = { viewModel.openObjectDetail(obj) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(text = if (isFa) "جزئیات" else "Details", color = Color.White)
                    }
                }
            }
        }

        // Layer 7: Target Arrival Celebration Dialog
        if (showArrivalDialog && selectedTarget != null) {
            AlertDialog(
                onDismissRequest = { showArrivalDialog = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🎯", fontSize = 24.sp)
                        Text(
                            text = if (isFa) "هدف پیدا شد!" else "Target Acquired!",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (isFa) "شما جرم «${selectedTarget?.nameFa}» را با موفقیت در آسمان ردیابی کردید."
                            else "You successfully tracked ${selectedTarget?.nameEn} in the sky.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = if (isFa) selectedTarget?.descriptionFa ?: "" else selectedTarget?.descriptionEn ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
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
    }
}

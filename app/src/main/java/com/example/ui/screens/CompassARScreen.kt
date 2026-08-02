package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.astro_engine.*
import com.example.data.catalog.AstronomyCatalog
import com.example.domain.*
import com.example.ui.MainUiState
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import kotlin.math.*

@Composable
fun CompassARScreen(
    uiState: MainUiState,
    viewModel: MainViewModel
) {
    val isFa = uiState.language == AppLanguage.PERSIAN
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Camera Permission State
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    // Device Heading (Azimuth 0..360) and Pitch (Altitude -90..90)
    var currentAzimuth by remember { mutableStateOf(180.0) } // Default South
    var currentAltitude by remember { mutableStateOf(45.0) } // Default 45 deg elevation

    var isSensorActive by remember { mutableStateOf(true) }
    var isCameraEnabled by remember { mutableStateOf(true) }

    // Hardware Sensors Setup
    DisposableEffect(isSensorActive) {
        if (!isSensorActive) return@DisposableEffect onDispose {}

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        if (sensorManager == null) return@DisposableEffect onDispose {}

        val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val rotationMatrix = FloatArray(9)
        val orientationValues = FloatArray(3)
        val gravityValues = FloatArray(3)
        val geomagneticValues = FloatArray(3)

        var hasGravity = false
        var hasGeomagnetic = false

        val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                        SensorManager.getOrientation(rotationMatrix, orientationValues)

                        var rawAzimuth = Math.toDegrees(orientationValues[0].toDouble())
                        if (rawAzimuth < 0) rawAzimuth += 360.0

                        var rawPitch = Math.toDegrees(-orientationValues[1].toDouble())

                        val diff = ((rawAzimuth - currentAzimuth + 540) % 360) - 180
                        currentAzimuth = (currentAzimuth + 0.15 * diff + 360) % 360
                        currentAltitude = (currentAltitude * 0.85 + rawPitch * 0.15).coerceIn(-10.0, 90.0)
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        System.arraycopy(event.values, 0, gravityValues, 0, 3)
                        hasGravity = true
                        processAccMag()
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        System.arraycopy(event.values, 0, geomagneticValues, 0, 3)
                        hasGeomagnetic = true
                        processAccMag()
                    }
                }
            }

            private fun processAccMag() {
                if (rotationVectorSensor != null) return
                if (hasGravity && hasGeomagnetic) {
                    val success = SensorManager.getRotationMatrix(rotationMatrix, null, gravityValues, geomagneticValues)
                    if (success) {
                        SensorManager.getOrientation(rotationMatrix, orientationValues)
                        var rawAzimuth = Math.toDegrees(orientationValues[0].toDouble())
                        if (rawAzimuth < 0) rawAzimuth += 360.0

                        var rawPitch = Math.toDegrees(-orientationValues[1].toDouble())

                        val diff = ((rawAzimuth - currentAzimuth + 540) % 360) - 180
                        currentAzimuth = (currentAzimuth + 0.15 * diff + 360) % 360
                        currentAltitude = (currentAltitude * 0.85 + rawPitch * 0.15).coerceIn(-10.0, 90.0)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (rotationVectorSensor != null) {
            sensorManager.registerListener(sensorListener, rotationVectorSensor, SensorManager.SENSOR_DELAY_GAME)
        } else {
            accelSensor?.let { sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_GAME) }
            magnetSensor?.let { sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_GAME) }
        }

        onDispose {
            sensorManager.unregisterListener(sensorListener)
        }
    }

    val jd = remember { TimeEngine.getJulianDate() }
    val lastDeg = remember(uiState.userLocation) {
        TimeEngine.getLAST(jd, uiState.userLocation.longitude)
    }

    val moonData = remember(jd) { MoonEngine.calculateMoon(jd) }
    val moonHoriz = remember(moonData, lastDeg, uiState.userLocation) {
        CoordinateEngine.equatorialToHorizontal(
            CoordinateEngine.Equatorial(moonData.raDeg, moonData.decDeg),
            lastDeg,
            uiState.userLocation.latitude
        )
    }

    val allCatalog = remember { AstronomyCatalog.getAllObjects() }

    // Targeted Object near reticle
    val targetedObject = remember(currentAzimuth, currentAltitude, lastDeg, uiState.userLocation, moonHoriz) {
        var closestObj: CelestialObject? = null
        var minDistance = 15.0

        // Check Moon first
        val dAzMoon = abs(moonHoriz.azimuthDeg - currentAzimuth)
        val dAltMoon = abs(moonHoriz.altitudeDeg - currentAltitude)
        val distMoon = sqrt(dAzMoon * dAzMoon + dAltMoon * dAltMoon)

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

    val isMoonTargeted = remember(currentAzimuth, currentAltitude, moonHoriz) {
        var dAz = moonHoriz.azimuthDeg - currentAzimuth
        if (dAz > 180) dAz -= 360
        if (dAz < -180) dAz += 360
        val dAlt = moonHoriz.altitudeDeg - currentAltitude
        sqrt(dAz * dAz + dAlt * dAlt) < 12.0
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07070C))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    currentAzimuth = (currentAzimuth - dragAmount.x * 0.2) % 360.0
                    if (currentAzimuth < 0) currentAzimuth += 360.0
                    currentAltitude = (currentAltitude + dragAmount.y * 0.2).coerceIn(-10.0, 90.0)
                }
            }
            .testTag("compass_ar_screen")
    ) {
        // Layer 1: Live Camera Feed if permission granted
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
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerX = canvasWidth / 2f
            val centerY = canvasHeight / 2f

            // Field of View parameters (approx 60 deg horiz FOV)
            val fovX = 60.0
            val pixelsPerDegree = canvasWidth / fovX

            // If camera disabled or not available, draw subtle star field background
            if (!hasCameraPermission || !isCameraEnabled) {
                val rand = java.util.Random(42)
                for (i in 0..150) {
                    val sx = rand.nextFloat() * canvasWidth
                    val sy = rand.nextFloat() * canvasHeight
                    val radius = rand.nextFloat() * 2.2f + 0.5f
                    drawCircle(
                        color = Color.White.copy(alpha = rand.nextFloat() * 0.6f + 0.2f),
                        radius = radius,
                        center = Offset(sx, sy)
                    )
                }
            }

            // Draw Horizon Line
            val horizonY = (centerY + (currentAltitude * pixelsPerDegree)).toFloat()
            if (horizonY in 0f..canvasHeight) {
                drawLine(
                    color = AccentPrimary.copy(alpha = 0.5f),
                    start = Offset(0f, horizonY),
                    end = Offset(canvasWidth, horizonY),
                    strokeWidth = 2f
                )
            }

            // 1. Draw Moon Position & Orbit Trajectory Arc
            var dAzMoon = moonHoriz.azimuthDeg - currentAzimuth
            if (dAzMoon > 180) dAzMoon -= 360
            if (dAzMoon < -180) dAzMoon += 360
            val dAltMoon = moonHoriz.altitudeDeg - currentAltitude

            val moonPx = (centerX + (dAzMoon * pixelsPerDegree)).toFloat()
            val moonPy = (centerY - (dAltMoon * pixelsPerDegree)).toFloat()

            // Draw Moon Orbit Trajectory Arc across the sky
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
                color = AccentPrimary.copy(alpha = 0.4f),
                style = Stroke(width = 2.5f)
            )

            // Draw Moon Glyph/Target if in field of view
            if (moonPx in -100f..(canvasWidth + 100f) && moonPy in -100f..(canvasHeight + 100f)) {
                val isHighlighted = isMoonTargeted
                val moonRadius = if (isHighlighted) 28f else 20f

                // Outer halo
                drawCircle(
                    color = AccentPrimary.copy(alpha = if (isHighlighted) 0.6f else 0.3f),
                    radius = moonRadius * 1.8f,
                    center = Offset(moonPx, moonPy)
                )

                // Moon Disc
                drawCircle(
                    color = Color(0xFFF1FAEE),
                    radius = moonRadius,
                    center = Offset(moonPx, moonPy)
                )

                // Illuminated phase overlay
                val illumFrac = (moonData.illuminationPercent / 100.0).toFloat()
                drawCircle(
                    color = AccentSecondary.copy(alpha = 0.8f),
                    radius = moonRadius * illumFrac,
                    center = Offset(moonPx, moonPy)
                )

                // Target reticle frame around Moon
                drawCircle(
                    color = if (isHighlighted) AccentPrimary else Color.White,
                    radius = moonRadius + 10f,
                    center = Offset(moonPx, moonPy),
                    style = Stroke(width = 2f)
                )
            }

            // 2. Draw Celestial Objects from Catalog
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

                val px = (centerX + (dAz * pixelsPerDegree)).toFloat()
                val py = (centerY - (dAlt * pixelsPerDegree)).toFloat()

                if (px in 0f..canvasWidth && py in 0f..canvasHeight) {
                    val isTargeted = obj.id == targetedObject?.id
                    val circleColor = if (isTargeted) AccentPrimary else Color(0xFF48CAE4)
                    val radius = if (isTargeted) 10f else 5f

                    drawCircle(
                        color = circleColor,
                        radius = radius,
                        center = Offset(px, py)
                    )

                    if (isTargeted) {
                        drawCircle(
                            color = AccentPrimary.copy(alpha = 0.4f),
                            radius = 22f,
                            center = Offset(px, py),
                            style = Stroke(width = 2f)
                        )
                    }
                }
            }

            // 3. Draw Center Reticle Crosshair Frame
            drawCircle(
                color = AccentPrimary,
                radius = 70f,
                center = Offset(centerX, centerY),
                style = Stroke(width = 2.5f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.4f),
                radius = 110f,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1.5f)
            )

            // Crosshair tick marks
            drawLine(
                color = Color.White,
                start = Offset(centerX - 90f, centerY),
                end = Offset(centerX - 50f, centerY),
                strokeWidth = 2f
            )
            drawLine(
                color = Color.White,
                start = Offset(centerX + 50f, centerY),
                end = Offset(centerX + 90f, centerY),
                strokeWidth = 2f
            )
            drawLine(
                color = Color.White,
                start = Offset(centerX, centerY - 90f),
                end = Offset(centerX, centerY - 50f),
                strokeWidth = 2f
            )
            drawLine(
                color = Color.White,
                start = Offset(centerX, centerY + 50f),
                end = Offset(centerX, centerY + 90f),
                strokeWidth = 2f
            )
        }

        // Layer 3: Top Control Bar (Camera Permission / Sensors / HUD)
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, CardBorder),
            color = BackgroundCard.copy(alpha = 0.85f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = "AR Compass",
                        tint = AccentPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = if (isFa) "واقعیت افزوده AR و قطب‌نما" else "AR Compass & Sky Identifier",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        val azStr = String.format("%.1f°", currentAzimuth)
                        val altStr = String.format("%.1f°", currentAltitude)
                        val cardinalDir = getCardinalDirection(currentAzimuth, isFa)
                        Text(
                            text = if (isFa) {
                                "$cardinalDir (${TimeEngine.formatPersianNumbers(azStr)}) | ارتفاع: ${TimeEngine.formatPersianNumbers(altStr)}"
                            } else {
                                "$cardinalDir ($azStr) | Alt: $altStr"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Camera Toggle / Permission request
                    if (!hasCameraPermission) {
                        IconButton(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                            Icon(
                                imageVector = Icons.Default.VideocamOff,
                                contentDescription = "Enable Camera",
                                tint = AccentSecondary
                            )
                        }
                    } else {
                        IconButton(onClick = { isCameraEnabled = !isCameraEnabled }) {
                            Icon(
                                imageVector = if (isCameraEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                contentDescription = "Camera Toggle",
                                tint = if (isCameraEnabled) StatusGood else Color.Gray
                            )
                        }
                    }

                    // Sensors Toggle
                    IconButton(onClick = { isSensorActive = !isSensorActive }) {
                        Icon(
                            imageVector = if (isSensorActive) Icons.Default.Sensors else Icons.Default.SensorsOff,
                            contentDescription = "Sensor",
                            tint = if (isSensorActive) StatusGood else Color.Gray
                        )
                    }
                }
            }
        }

        // Layer 4: Bottom Target Recognition HUD Card
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 90.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth()
        ) {
            when {
                isMoonTargeted -> {
                    // Moon Locked HUD Card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("targeted_moon_card"),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, AccentPrimary),
                        color = BackgroundCard.copy(alpha = 0.9f)
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
                                        imageVector = Icons.Default.NightlightRound,
                                        contentDescription = "Moon",
                                        tint = AccentPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = if (isFa) "ماه (${moonData.phaseNameFa})" else "Moon (${moonData.phaseNameEn})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }

                                val illumStr = String.format("%.1f%%", moonData.illuminationPercent)
                                val ageStr = String.format("%.1f", moonData.ageDays)
                                Text(
                                    text = if (isFa) {
                                        "روشنایی: ${TimeEngine.formatPersianNumbers(illumStr)} | سن: ${TimeEngine.formatPersianNumbers(ageStr)} روز"
                                    } else {
                                        "Illum: $illumStr | Age: $ageStr days"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )

                                Text(
                                    text = if (isFa) "مسیر مداری و جهت حرکت ماه روی تصویر زنده نشان داده شده است" else "Moon's orbit path and trajectory overlaid on live camera",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AccentPrimary
                                )
                            }
                        }
                    }
                }
                targetedObject != null -> {
                    // Targeted Catalog Object HUD Card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.openObjectDetail(targetedObject) }
                            .testTag("targeted_object_card"),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, CardBorder),
                        color = BackgroundCard.copy(alpha = 0.9f)
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
                                        contentDescription = "Targeted",
                                        tint = AccentPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = if (isFa) targetedObject.nameFa else targetedObject.nameEn,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }

                                val constName = if (isFa) targetedObject.constellationFa else targetedObject.constellationEn
                                Text(
                                    text = "$constName • ${targetedObject.category}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )

                                Text(
                                    text = if (isFa) "برای مشاهده مشخصات تخصصی کلیک کنید" else "Tap for detailed astronomical data",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AccentPrimary
                                )
                            }

                            Button(
                                onClick = { viewModel.openObjectDetail(targetedObject) },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(text = if (isFa) "اطلاعات" else "Details", color = Color.White)
                            }
                        }
                    }
                }
                else -> {
                    // Hint Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, CardBorder),
                        color = BackgroundCard.copy(alpha = 0.85f)
                    ) {
                        Text(
                            text = if (isFa) "دوربین گوشی خود را به سمت آسمان بگیرید تا اجرام نجومی و مدار آن‌ها شناسایی شوند" else "Point camera at the sky to identify celestial objects & orbit paths",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

private fun getCardinalDirection(azimuthDeg: Double, isFa: Boolean): String {
    val directionsEn = arrayOf("North (N)", "North-East (NE)", "East (E)", "South-East (SE)", "South (S)", "South-West (SW)", "West (W)", "North-West (NW)")
    val directionsFa = arrayOf("شمال (N)", "شمال‌شرق (NE)", "شرق (E)", "جنوب‌شرق (SE)", "جنوب (S)", "جنوب‌غرب (SW)", "غرب (W)", "شمال‌غرب (NW)")
    val index = (((azimuthDeg + 22.5) % 360) / 45.0).toInt().coerceIn(0, 7)
    return if (isFa) directionsFa[index] else directionsEn[index]
}

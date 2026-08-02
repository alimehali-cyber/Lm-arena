package com.example.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.astro_engine.*
import com.example.data.catalog.AstronomyCatalog
import com.example.domain.*
import com.example.ui.MainUiState
import com.example.ui.MainViewModel
import kotlin.math.*

@Composable
fun CompassARScreen(
    uiState: MainUiState,
    viewModel: MainViewModel
) {
    val isFa = uiState.language == AppLanguage.PERSIAN

    val context = LocalContext.current

    // Device Heading (Azimuth 0..360) and Pitch (Altitude -90..90)
    var currentAzimuth by remember { mutableStateOf(180.0) } // Default South
    var currentAltitude by remember { mutableStateOf(45.0) } // Default 45 deg elevation

    var isSensorActive by remember { mutableStateOf(true) }

    // Register real hardware Orientation / Rotation / Magnetic Field sensors
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

                        // Smooth angular interpolation for zero-jitter compass rotation
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

    val allObjects = remember { AstronomyCatalog.getAllObjects() }

    // Find closest celestial object near the reticle crosshair (within 15 deg)
    val targetedObject = remember(currentAzimuth, currentAltitude, lastDeg, uiState.userLocation) {
        var closestObj: CelestialObject? = null
        var minDistance = 15.0 // Angular threshold in deg

        for (obj in allObjects) {
            val horiz = CoordinateEngine.equatorialToHorizontal(
                CoordinateEngine.Equatorial(obj.raDeg, obj.decDeg),
                lastDeg,
                uiState.userLocation.latitude
            )
            val dAz = abs(horiz.azimuthDeg - currentAzimuth)
            val dAlt = abs(horiz.altitudeDeg - currentAltitude)
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
            .background(Color(0xFF0A0A0F))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    // Pan azimuth and altitude with finger drag
                    currentAzimuth = (currentAzimuth - dragAmount.x * 0.2) % 360.0
                    if (currentAzimuth < 0) currentAzimuth += 360.0
                    currentAltitude = (currentAltitude + dragAmount.y * 0.2).coerceIn(-10.0, 90.0)
                }
            }
            .testTag("compass_ar_screen")
    ) {
        // Celestial Grid & Crosshair Canvas Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerX = canvasWidth / 2f
            val centerY = canvasHeight / 2f

            // Draw deep space background stars
            val rand = java.util.Random(42)
            for (i in 0..120) {
                val sx = rand.nextFloat() * canvasWidth
                val sy = rand.nextFloat() * canvasHeight
                val radius = rand.nextFloat() * 2.5f + 0.5f
                drawCircle(
                    color = Color.White.copy(alpha = rand.nextFloat() * 0.7f + 0.3f),
                    radius = radius,
                    center = Offset(sx, sy)
                )
            }

            // Draw Compass Compass Cardinal Markings (N, E, S, W) on horizon line
            val horizY = centerY + (currentAltitude.toFloat() * 10f)
            drawLine(
                color = Color(0xFFE63946).copy(alpha = 0.6f),
                start = Offset(0f, horizY),
                end = Offset(canvasWidth, horizY),
                strokeWidth = 2f
            )

            // Draw Crosshair Reticle Targeting Frame
            drawCircle(
                color = Color(0xFFE63946),
                radius = 80f,
                center = Offset(centerX, centerY),
                style = Stroke(width = 3f)
            )
            drawCircle(
                color = Color(0xFFE63946).copy(alpha = 0.3f),
                radius = 120f,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1.5f)
            )

            // Reticle tick lines
            drawLine(
                color = Color(0xFFF8F9FA),
                start = Offset(centerX - 100f, centerY),
                end = Offset(centerX - 60f, centerY),
                strokeWidth = 2.5f
            )
            drawLine(
                color = Color(0xFFF8F9FA),
                start = Offset(centerX + 60f, centerY),
                end = Offset(centerX + 100f, centerY),
                strokeWidth = 2.5f
            )
            drawLine(
                color = Color(0xFFF8F9FA),
                start = Offset(centerX, centerY - 100f),
                end = Offset(centerX, centerY - 60f),
                strokeWidth = 2.5f
            )
            drawLine(
                color = Color(0xFFF8F9FA),
                start = Offset(centerX, centerY + 60f),
                end = Offset(centerX, centerY + 100f),
                strokeWidth = 2.5f
            )

            // Draw celestial objects relative to current Azimuth/Altitude
            for (obj in allObjects) {
                val horiz = CoordinateEngine.equatorialToHorizontal(
                    CoordinateEngine.Equatorial(obj.raDeg, obj.decDeg),
                    lastDeg,
                    uiState.userLocation.latitude
                )
                var dAz = horiz.azimuthDeg - currentAzimuth
                if (dAz > 180) dAz -= 360
                if (dAz < -180) dAz += 360

                val dAlt = horiz.altitudeDeg - currentAltitude

                // Convert angular offsets to pixel coords
                val px = centerX + (dAz * 12.0).toFloat()
                val py = centerY - (dAlt * 12.0).toFloat()

                if (px in 0f..canvasWidth && py in 0f..canvasHeight) {
                    val isTargeted = obj.id == targetedObject?.id
                    val circleColor = if (isTargeted) Color(0xFFFFB703) else Color(0xFFE63946)
                    val radius = if (isTargeted) 12f else 6f

                    drawCircle(
                        color = circleColor,
                        radius = radius,
                        center = Offset(px, py)
                    )

                    if (isTargeted) {
                        drawCircle(
                            color = Color(0xFFFFB703).copy(alpha = 0.4f),
                            radius = 24f,
                            center = Offset(px, py),
                            style = Stroke(width = 2f)
                        )
                    }
                }
            }
        }

        // Top Status HUD Overlay (Azimuth & Altitude Readout)
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1C1B1F).copy(alpha = 0.9f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = "Compass",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = if (isFa) "قطب‌نما و نشانه‌روی هوشمند" else "Smart Point & Identify",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        val azStr = String.format("%.1f°", currentAzimuth)
                        val altStr = String.format("%.1f°", currentAltitude)
                        val cardinalDir = getCardinalDirection(currentAzimuth, isFa)
                        Text(
                            text = if (isFa) {
                                "$cardinalDir (${TimeEngine.formatPersianNumbers(azStr)}) | ارتفاع: ${TimeEngine.formatPersianNumbers(altStr)}"
                            } else {
                                "$cardinalDir ($azStr) | Altitude: $altStr"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = { isSensorActive = !isSensorActive }) {
                    Icon(
                        imageVector = if (isSensorActive) Icons.Default.Sensors else Icons.Default.SensorsOff,
                        contentDescription = "Sensor",
                        tint = if (isSensorActive) Color(0xFF2DC653) else Color.Gray
                    )
                }
            }
        }

        // Bottom Target Info Card
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth()
        ) {
            if (targetedObject != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.openObjectDetail(targetedObject) }
                        .testTag("targeted_object_card"),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1C1B1F).copy(alpha = 0.95f)
                    )
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
                                    tint = Color(0xFFFFB703),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (isFa) targetedObject.nameFa else targetedObject.nameEn,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            val constName = if (isFa) targetedObject.constellationFa else targetedObject.constellationEn
                            Text(
                                text = "$constName • ${targetedObject.category}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = if (isFa) "برای مشاهده اطلاعات جامع نجومی کلیک کنید" else "Tap for detailed scientific data",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Button(
                            onClick = { viewModel.openObjectDetail(targetedObject) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(text = if (isFa) "شناسایی" else "Identify", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    color = Color(0xFF1C1B1F).copy(alpha = 0.85f)
                ) {
                    Text(
                        text = if (isFa) "گوشی خود را به سمت آسمان بگیرید یا صفحه را لمس کرده و بکشید" else "Point phone at sky or drag screen to identify objects",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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

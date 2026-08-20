package com.alijafari.red.astronomy.astro_engine

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

data class SkyOrientation(
    val azimuth: Float = 0f,   // True north azimuth, 0-360°, clockwise
    val pitch: Float = 0f,     // Elevation, positive = up, degrees (-90° to +90°)
    val roll: Float = 0f,      // Roll around optical axis, degrees
    val rotationMatrix: FloatArray = FloatArray(9)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SkyOrientation
        return azimuth == other.azimuth &&
                pitch == other.pitch &&
                roll == other.roll &&
                rotationMatrix.contentEquals(other.rotationMatrix)
    }

    override fun hashCode(): Int {
        var result = azimuth.hashCode()
        result = 31 * result + pitch.hashCode()
        result = 31 * result + roll.hashCode()
        result = 31 * result + rotationMatrix.contentHashCode()
        return result
    }
}

enum class CalibrationState {
    EXCELLENT, GOOD, POOR, NEEDS_CALIBRATION, UNCALIBRATED
}

class OrientationProvider(
    private val context: Context
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private val rotationVectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val gameRotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
    private val magnetometerSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val gyroSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    // Raw matrices
    private val rawRotationMatrix = FloatArray(9)
    private val trueRotationMatrix = FloatArray(9)
    private val smoothRotationMatrix = FloatArray(9)

    // Raw accel + mag buffers
    private val gravityValues = FloatArray(3)
    private val geomagneticValues = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    // Low-pass filtered state
    private var smoothAzimuth = 0f
    private var smoothPitch = 0f
    private var smoothRoll = 0f

    // Velocity-based adaptive alpha
    private var velocity = 0f
    private val ALPHA_FAST = 0.35f
    private val ALPHA_SLOW = 0.05f

    // Magnetic declination (degrees, positive = east)
    private var magneticDeclination = 0f

    // Magnetic accuracy state
    var magneticAccuracy = SensorManager.SENSOR_STATUS_ACCURACY_LOW
        private set

    private val _orientation = MutableStateFlow(SkyOrientation(0f, 45f, 0f))
    val orientation: StateFlow<SkyOrientation> = _orientation.asStateFlow()

    private val _calibrationState = MutableStateFlow(CalibrationState.UNCALIBRATED)
    val calibrationState: StateFlow<CalibrationState> = _calibrationState.asStateFlow()

    private var isStarted = false

    fun start() {
        if (isStarted || sensorManager == null) return
        isStarted = true

        if (rotationVectorSensor != null) {
            sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_GAME)
        } else if (gameRotationSensor != null) {
            sensorManager.registerListener(this, gameRotationSensor, SensorManager.SENSOR_DELAY_GAME)
        } else {
            accelSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
            magnetometerSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        }

        magnetometerSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        gyroSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        if (!isStarted) return
        isStarted = false
        sensorManager?.unregisterListener(this)
    }

    /**
     * Call when location is obtained to calculate geomagnetic declination.
     */
    fun updateLocation(latitude: Double, longitude: Double, altitude: Double = 0.0) {
        try {
            val geoField = GeomagneticField(
                latitude.toFloat(),
                longitude.toFloat(),
                altitude.toFloat(),
                System.currentTimeMillis()
            )
            magneticDeclination = geoField.declination
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR,
            Sensor.TYPE_GAME_ROTATION_VECTOR -> {
                processRotationVector(event.values)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, gravityValues, 0, 3)
                hasGravity = true
                if (rotationVectorSensor == null && gameRotationSensor == null) {
                    processAccelMag()
                }
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, geomagneticValues, 0, 3)
                hasGeomagnetic = true
                if (rotationVectorSensor == null && gameRotationSensor == null) {
                    processAccelMag()
                }
            }
            Sensor.TYPE_GYROSCOPE -> {
                val gx = event.values[0]
                val gy = event.values[1]
                val gz = event.values[2]
                val gyroSpeedDeg = Math.toDegrees(sqrt((gx * gx + gy * gy + gz * gz).toDouble())).toFloat()
                velocity = velocity * 0.75f + gyroSpeedDeg * 0.25f
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        if (sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            magneticAccuracy = accuracy
            _calibrationState.value = when (accuracy) {
                SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> CalibrationState.EXCELLENT
                SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> CalibrationState.GOOD
                SensorManager.SENSOR_STATUS_ACCURACY_LOW -> CalibrationState.POOR
                SensorManager.SENSOR_STATUS_UNRELIABLE -> CalibrationState.NEEDS_CALIBRATION
                else -> CalibrationState.NEEDS_CALIBRATION
            }
        }
    }

    private fun processRotationVector(rotationVectorValues: FloatArray) {
        SensorManager.getRotationMatrixFromVector(rawRotationMatrix, rotationVectorValues)
        computeTrueOrientation(rawRotationMatrix)
    }

    private fun processAccelMag() {
        if (!hasGravity || !hasGeomagnetic) return
        val success = SensorManager.getRotationMatrix(rawRotationMatrix, null, gravityValues, geomagneticValues)
        if (!success) return
        computeTrueOrientation(rawRotationMatrix)
    }

    private fun computeTrueOrientation(sensorMat: FloatArray) {
        // Convert sensor rotation matrix (Magnetic North) to True North coordinates
        val radD = Math.toRadians(magneticDeclination.toDouble())
        val cosD = cos(radD).toFloat()
        val sinD = sin(radD).toFloat()

        trueRotationMatrix[0] = cosD * sensorMat[0] + sinD * sensorMat[3]
        trueRotationMatrix[1] = cosD * sensorMat[1] + sinD * sensorMat[4]
        trueRotationMatrix[2] = cosD * sensorMat[2] + sinD * sensorMat[5]

        trueRotationMatrix[3] = -sinD * sensorMat[0] + cosD * sensorMat[3]
        trueRotationMatrix[4] = -sinD * sensorMat[1] + cosD * sensorMat[4]
        trueRotationMatrix[5] = -sinD * sensorMat[2] + cosD * sensorMat[5]

        trueRotationMatrix[6] = sensorMat[6]
        trueRotationMatrix[7] = sensorMat[7]
        trueRotationMatrix[8] = sensorMat[8]

        // Camera pointing vector (out of rear camera: -Z_device)
        // px = East, py = North, pz = Up
        val px = -trueRotationMatrix[2]
        val py = -trueRotationMatrix[5]
        val pz = -trueRotationMatrix[8]

        val azimuthDeg = ((Math.toDegrees(atan2(px.toDouble(), py.toDouble())) + 360.0) % 360.0).toFloat()
        val pitchDeg = Math.toDegrees(asin(pz.toDouble().coerceIn(-1.0, 1.0))).toFloat()

        // Device Right (X_device)
        val rx = trueRotationMatrix[0]
        val ry = trueRotationMatrix[3]
        val rz = trueRotationMatrix[6]

        // Sky natural right and up vectors in world frame
        val horizLen = sqrt((px * px + py * py).toDouble()).toFloat()
        val rollDeg = if (horizLen > 1e-4f) {
            val rSkyX = py / horizLen
            val rSkyY = -px / horizLen
            val rSkyZ = 0f

            val uSkyX = -px * pz / horizLen
            val uSkyY = -py * pz / horizLen
            val uSkyZ = horizLen

            val dotRight = rx * rSkyX + ry * rSkyY + rz * rSkyZ
            val dotUp = rx * uSkyX + ry * uSkyY + rz * uSkyZ

            Math.toDegrees(atan2(dotUp.toDouble(), dotRight.toDouble())).toFloat()
        } else {
            Math.toDegrees(atan2(rz.toDouble(), trueRotationMatrix[7].toDouble())).toFloat()
        }

        applyAdaptiveFiltering(azimuthDeg, pitchDeg, rollDeg, trueRotationMatrix)
    }

    private fun applyAdaptiveFiltering(newAz: Float, newPitch: Float, newRoll: Float, newMatrix: FloatArray) {
        var deltaAz = newAz - smoothAzimuth
        if (deltaAz > 180f) deltaAz -= 360f
        if (deltaAz < -180f) deltaAz += 360f

        // Continuous interpolation of alpha based on angular movement speed
        val normVel = (velocity / 30f).coerceIn(0f, 1f)
        val alpha = ALPHA_SLOW + (ALPHA_FAST - ALPHA_SLOW) * normVel

        smoothAzimuth = ((smoothAzimuth + alpha * deltaAz) % 360f + 360f) % 360f
        smoothPitch += alpha * (newPitch - smoothPitch)
        smoothRoll += alpha * (newRoll - smoothRoll)

        for (i in 0..8) {
            smoothRotationMatrix[i] += alpha * (newMatrix[i] - smoothRotationMatrix[i])
        }

        _orientation.value = SkyOrientation(
            azimuth = smoothAzimuth,
            pitch = smoothPitch.coerceIn(-90f, 90f),
            roll = smoothRoll,
            rotationMatrix = smoothRotationMatrix.copyOf()
        )
    }
}

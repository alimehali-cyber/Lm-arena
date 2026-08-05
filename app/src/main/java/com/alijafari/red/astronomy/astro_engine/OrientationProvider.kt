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
import kotlin.math.abs
import kotlin.math.sqrt

data class SkyOrientation(
    val azimuth: Float = 0f,   // True north azimuth, 0-360°, clockwise
    val pitch: Float = 0f,     // Elevation, positive = up, degrees
    val roll: Float = 0f       // Roll, degrees
)

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
    private val rotationMatrix = FloatArray(9)
    private val remappedMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

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
    private val ALPHA_SLOW = 0.04f

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
        SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVectorValues)

        // Remap coordinate system for rear-facing camera in portrait mode
        SensorManager.remapCoordinateSystem(
            rotationMatrix,
            SensorManager.AXIS_X,
            SensorManager.AXIS_Z,
            remappedMatrix
        )

        SensorManager.getOrientation(remappedMatrix, orientationAngles)

        var azimuthDeg = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
        var pitchDeg = Math.toDegrees(-orientationAngles[1].toDouble()).toFloat()
        val rollDeg = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

        azimuthDeg = ((azimuthDeg % 360) + 360) % 360

        // Apply magnetic declination to convert to TRUE NORTH azimuth
        azimuthDeg = ((azimuthDeg + magneticDeclination) % 360 + 360) % 360

        // Gimbal lock check near zenith (pitch > 80° or < -80°)
        if (abs(pitchDeg) > 80f) {
            azimuthDeg = smoothAzimuth // hold steady near zenith
        }

        applyAdaptiveFiltering(azimuthDeg, pitchDeg, rollDeg)
    }

    private fun processAccelMag() {
        if (!hasGravity || !hasGeomagnetic) return
        val success = SensorManager.getRotationMatrix(rotationMatrix, null, gravityValues, geomagneticValues)
        if (!success) return

        SensorManager.remapCoordinateSystem(
            rotationMatrix,
            SensorManager.AXIS_X,
            SensorManager.AXIS_Z,
            remappedMatrix
        )

        SensorManager.getOrientation(remappedMatrix, orientationAngles)

        var azimuthDeg = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
        var pitchDeg = Math.toDegrees(-orientationAngles[1].toDouble()).toFloat()
        val rollDeg = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

        azimuthDeg = ((azimuthDeg % 360) + 360) % 360
        azimuthDeg = ((azimuthDeg + magneticDeclination) % 360 + 360) % 360

        applyAdaptiveFiltering(azimuthDeg, pitchDeg, rollDeg)
    }

    private fun applyAdaptiveFiltering(newAz: Float, newPitch: Float, newRoll: Float) {
        var deltaAz = newAz - smoothAzimuth
        if (deltaAz > 180f) deltaAz -= 360f
        if (deltaAz < -180f) deltaAz += 360f

        // Continuous interpolation of alpha based on angular movement speed
        val normVel = (velocity / 30f).coerceIn(0f, 1f)
        val alpha = ALPHA_SLOW + (ALPHA_FAST - ALPHA_SLOW) * normVel

        smoothAzimuth = ((smoothAzimuth + alpha * deltaAz) % 360 + 360) % 360
        smoothPitch += alpha * (newPitch - smoothPitch)
        smoothRoll += alpha * (newRoll - smoothRoll)

        _orientation.value = SkyOrientation(
            azimuth = smoothAzimuth,
            pitch = smoothPitch.coerceIn(-10f, 90f),
            roll = smoothRoll
        )
    }
}

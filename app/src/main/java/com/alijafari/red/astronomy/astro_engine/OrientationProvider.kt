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

    // Quaternion filter state (w, x, y, z)
    private var smoothQuatW = 1.0
    private var smoothQuatX = 0.0
    private var smoothQuatY = 0.0
    private var smoothQuatZ = 0.0
    private var isQuatInitialized = false

    // Raw matrices
    private val rawRotationMatrix = FloatArray(9)
    private val trueRotationMatrix = FloatArray(9)
    private val calibratedRotationMatrix = FloatArray(9)
    private val calibMatrixBuffer = FloatArray(9)

    // Raw accel + mag buffers for fallback
    private val gravityValues = FloatArray(3)
    private val geomagneticValues = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    // Velocity-based adaptive alpha
    private var gyroSpeedDeg = 0f
    private val ALPHA_MIN = 0.045f // Ultra-stable when stationary, zero jitter
    private val ALPHA_MAX = 0.40f  // Fast response during panning

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
                processRotationVectorEvent(event.values)
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
                val speed = Math.toDegrees(sqrt((gx * gx + gy * gy + gz * gz).toDouble())).toFloat()
                gyroSpeedDeg = gyroSpeedDeg * 0.7f + speed * 0.3f
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

    private fun processRotationVectorEvent(values: FloatArray) {
        // Sensor values contain [x*sin(theta/2), y*sin(theta/2), z*sin(theta/2), cos(theta/2)]
        val qx = values[0].toDouble()
        val qy = values[1].toDouble()
        val qz = values[2].toDouble()
        val qw = if (values.size >= 4 && values[3] != 0f) {
            values[3].toDouble()
        } else {
            val sinHalfSq = qx * qx + qy * qy + qz * qz
            if (sinHalfSq <= 1.0) sqrt(1.0 - sinHalfSq) else 0.0
        }

        updateQuaternion(qw, qx, qy, qz)
    }

    private fun processAccelMag() {
        if (!hasGravity || !hasGeomagnetic) return
        val success = SensorManager.getRotationMatrix(rawRotationMatrix, null, gravityValues, geomagneticValues)
        if (!success) return

        // Extract quaternion from rotation matrix
        val trace = rawRotationMatrix[0] + rawRotationMatrix[4] + rawRotationMatrix[8]
        val qw: Double
        val qx: Double
        val qy: Double
        val qz: Double

        if (trace > 0.0) {
            val s = 0.5 / sqrt(trace + 1.0)
            qw = 0.25 / s
            qx = (rawRotationMatrix[7] - rawRotationMatrix[5]) * s
            qy = (rawRotationMatrix[2] - rawRotationMatrix[6]) * s
            qz = (rawRotationMatrix[3] - rawRotationMatrix[1]) * s
        } else if (rawRotationMatrix[0] > rawRotationMatrix[4] && rawRotationMatrix[0] > rawRotationMatrix[8]) {
            val s = 2.0 * sqrt(1.0 + rawRotationMatrix[0] - rawRotationMatrix[4] - rawRotationMatrix[8])
            qw = (rawRotationMatrix[7] - rawRotationMatrix[5]) / s
            qx = 0.25 * s
            qy = (rawRotationMatrix[1] + rawRotationMatrix[3]) / s
            qz = (rawRotationMatrix[2] + rawRotationMatrix[6]) / s
        } else if (rawRotationMatrix[4] > rawRotationMatrix[8]) {
            val s = 2.0 * sqrt(1.0 + rawRotationMatrix[4] - rawRotationMatrix[0] - rawRotationMatrix[8])
            qw = (rawRotationMatrix[2] - rawRotationMatrix[6]) / s
            qx = (rawRotationMatrix[1] + rawRotationMatrix[3]) / s
            qy = 0.25 * s
            qz = (rawRotationMatrix[5] + rawRotationMatrix[7]) / s
        } else {
            val s = 2.0 * sqrt(1.0 + rawRotationMatrix[8] - rawRotationMatrix[0] - rawRotationMatrix[4])
            qw = (rawRotationMatrix[3] - rawRotationMatrix[1]) / s
            qx = (rawRotationMatrix[2] + rawRotationMatrix[6]) / s
            qy = (rawRotationMatrix[5] + rawRotationMatrix[7]) / s
            qz = 0.25 * s
        }

        updateQuaternion(qw, qx, qy, qz)
    }

    private fun updateQuaternion(targetW: Double, targetX: Double, targetY: Double, targetZ: Double) {
        // Normalize target quaternion
        val norm = sqrt(targetW * targetW + targetX * targetX + targetY * targetY + targetZ * targetZ)
        if (norm < 1e-6) return
        var tw = targetW / norm
        var tx = targetX / norm
        var ty = targetY / norm
        var tz = targetZ / norm

        if (!isQuatInitialized) {
            smoothQuatW = tw
            smoothQuatX = tx
            smoothQuatY = ty
            smoothQuatZ = tz
            isQuatInitialized = true
        } else {
            // Ensure shortest rotation path (q and -q represent same orientation)
            var dot = smoothQuatW * tw + smoothQuatX * tx + smoothQuatY * ty + smoothQuatZ * tz
            if (dot < 0.0) {
                tw = -tw
                tx = -tx
                ty = -ty
                tz = -tz
                dot = -dot
            }

            // Calculate angular distance in degrees
            val clampedDot = dot.coerceIn(0.0, 1.0)
            val angleDiffDeg = Math.toDegrees(2.0 * acos(clampedDot)).toFloat()

            // Dynamic adaptive alpha: low when still, responsive when moving
            val movementFactor = max((gyroSpeedDeg / 25f), (angleDiffDeg / 4.0f)).coerceIn(0f, 1f)
            val alpha = (ALPHA_MIN + (ALPHA_MAX - ALPHA_MIN) * movementFactor).toDouble()

            // Spherical Linear Interpolation (SLERP)
            if (clampedDot < 0.9995) {
                val theta = acos(clampedDot)
                val sinTheta = sin(theta)
                val w1 = sin((1.0 - alpha) * theta) / sinTheta
                val w2 = sin(alpha * theta) / sinTheta
                smoothQuatW = w1 * smoothQuatW + w2 * tw
                smoothQuatX = w1 * smoothQuatX + w2 * tx
                smoothQuatY = w1 * smoothQuatY + w2 * ty
                smoothQuatZ = w1 * smoothQuatZ + w2 * tz
            } else {
                // Linear fallback for nearly identical quaternions
                smoothQuatW += alpha * (tw - smoothQuatW)
                smoothQuatX += alpha * (tx - smoothQuatX)
                smoothQuatY += alpha * (ty - smoothQuatY)
                smoothQuatZ += alpha * (tz - smoothQuatZ)
            }

            // Re-normalize smoothed quaternion
            val sNorm = sqrt(smoothQuatW * smoothQuatW + smoothQuatX * smoothQuatX + smoothQuatY * smoothQuatY + smoothQuatZ * smoothQuatZ)
            if (sNorm > 1e-6) {
                smoothQuatW /= sNorm
                smoothQuatX /= sNorm
                smoothQuatY /= sNorm
                smoothQuatZ /= sNorm
            }
        }

        // Convert smoothed quaternion to strictly orthonormal 3x3 rotation matrix R_sensor
        val w = smoothQuatW
        val x = smoothQuatX
        val y = smoothQuatY
        val z = smoothQuatZ

        val r00 = (1.0 - 2.0 * (y * y + z * z)).toFloat()
        val r01 = (2.0 * (x * y - w * z)).toFloat()
        val r02 = (2.0 * (x * z + w * y)).toFloat()

        val r10 = (2.0 * (x * y + w * z)).toFloat()
        val r11 = (1.0 - 2.0 * (x * x + z * z)).toFloat()
        val r12 = (2.0 * (y * z - w * x)).toFloat()

        val r20 = (2.0 * (x * z - w * y)).toFloat()
        val r21 = (2.0 * (y * z + w * x)).toFloat()
        val r22 = (1.0 - 2.0 * (x * x + y * y)).toFloat()

        // Apply True-North magnetic declination rotation around world Z-axis (Up)
        // R_true = R_declination * R_sensor
        val radD = Math.toRadians(magneticDeclination.toDouble())
        val cosD = cos(radD).toFloat()
        val sinD = sin(radD).toFloat()

        trueRotationMatrix[0] = cosD * r00 + sinD * r10
        trueRotationMatrix[1] = cosD * r01 + sinD * r11
        trueRotationMatrix[2] = cosD * r02 + sinD * r12

        trueRotationMatrix[3] = -sinD * r00 + cosD * r10
        trueRotationMatrix[4] = -sinD * r01 + cosD * r11
        trueRotationMatrix[5] = -sinD * r02 + cosD * r12

        trueRotationMatrix[6] = r20
        trueRotationMatrix[7] = r21
        trueRotationMatrix[8] = r22

        // Apply AR Pointing Calibration Layer (Yaw, Pitch, Roll offsets)
        // R_final = R_true * R_calib (isolated device orientation correction)
        val calibOffsets = ARCalibrationManager.getOffsets()
        val finalRotationMatrix: FloatArray
        if (calibOffsets.isCalibrated) {
            ARCalibrationManager.createCalibrationRotationMatrix(
                yawDeg = calibOffsets.yawOffsetDeg,
                pitchDeg = calibOffsets.pitchOffsetDeg,
                rollDeg = calibOffsets.rollOffsetDeg,
                outMatrix = calibMatrixBuffer
            )
            ARCalibrationManager.multiplyMatrix3x3(trueRotationMatrix, calibMatrixBuffer, calibratedRotationMatrix)
            finalRotationMatrix = calibratedRotationMatrix
        } else {
            finalRotationMatrix = trueRotationMatrix
        }

        // Camera optical pointing vector (-Z_device in world coordinates)
        // px = East, py = North, pz = Up
        val px = -finalRotationMatrix[2]
        val py = -finalRotationMatrix[5]
        val pz = -finalRotationMatrix[8]

        val azimuthDeg = ((Math.toDegrees(atan2(px.toDouble(), py.toDouble())) + 360.0) % 360.0).toFloat()
        val pitchDeg = Math.toDegrees(asin(pz.toDouble().coerceIn(-1.0, 1.0))).toFloat()

        // Device Right (X_device) & Device Up (Y_device)
        val rx = finalRotationMatrix[0]
        val ry = finalRotationMatrix[3]
        val rz = finalRotationMatrix[6]

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
            Math.toDegrees(atan2(rz.toDouble(), finalRotationMatrix[7].toDouble())).toFloat()
        }

        _orientation.value = SkyOrientation(
            azimuth = azimuthDeg,
            pitch = pitchDeg,
            roll = rollDeg,
            rotationMatrix = finalRotationMatrix.copyOf()
        )
    }
}


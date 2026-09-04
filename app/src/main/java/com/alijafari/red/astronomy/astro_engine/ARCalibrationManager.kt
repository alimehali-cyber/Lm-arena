package com.alijafari.red.astronomy.astro_engine

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.cos
import kotlin.math.sin

/**
 * RED AR Manual Pointing Calibration Calibration Layer.
 *
 * Provides a mathematically isolated calibration rotation for device-specific pointing offsets:
 * - Yaw offset (delta-azimuth in degrees, around optical boresight/pointing axis)
 * - Pitch offset (delta-elevation in degrees, around camera horizontal pitch axis)
 * - Roll offset (delta-roll in degrees, around camera optical line-of-sight)
 *
 * Mathematical Frame:
 * Applied as one explicit rotation in SO(3) to the True-North rotation matrix:
 *   R_calibrated = R_true * R_calib(yaw, pitch, roll)
 *
 * Isolated and modular so that it can later be substituted by an automated multi-star solver
 * without modifying the underlying camera projection or astronomical coordinate pipelines.
 *
 * Note: Calibration aligns device sensor orientation to visible celestial landmarks and compensates
 * for device chassis misalignment, but does not guarantee sub-degree accuracy under sensor drift.
 */
data class ARCalibrationOffsets(
    val yawOffsetDeg: Float = 0f,
    val pitchOffsetDeg: Float = 0f,
    val rollOffsetDeg: Float = 0f,
    val lastCalibratedTimeMs: Long = 0L,
    val referenceStarName: String = ""
) {
    val isCalibrated: Boolean
        get() = yawOffsetDeg != 0f || pitchOffsetDeg != 0f || rollOffsetDeg != 0f
}

object ARCalibrationManager {

    private const val PREFS_NAME = "red_ar_calibration_prefs"
    private const val KEY_YAW = "calib_yaw_offset_deg"
    private const val KEY_PITCH = "calib_pitch_offset_deg"
    private const val KEY_ROLL = "calib_roll_offset_deg"
    private const val KEY_TIME = "calib_timestamp_ms"
    private const val KEY_STAR = "calib_reference_star"
    private const val KEY_AUTO_PROMPT = "calib_auto_prompt_enabled"
    // OD4 one-time legacy-yaw rebase marker (versioned: bump suffix to force another rebase)
    private const val KEY_YAW_DECLINATION_REBASED_V1 = "calib_yaw_declination_rebased_v1"

    private var sharedPreferences: SharedPreferences? = null

    private val _calibrationFlow = MutableStateFlow(ARCalibrationOffsets())
    val calibrationFlow: StateFlow<ARCalibrationOffsets> = _calibrationFlow.asStateFlow()

    private val _autoPromptEnabledFlow = MutableStateFlow(true)
    val autoPromptEnabledFlow: StateFlow<Boolean> = _autoPromptEnabledFlow.asStateFlow()

    fun init(context: Context) {
        if (sharedPreferences == null) {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sharedPreferences = prefs
            val yaw = prefs.getFloat(KEY_YAW, 0f)
            val pitch = prefs.getFloat(KEY_PITCH, 0f)
            val roll = prefs.getFloat(KEY_ROLL, 0f)
            val time = prefs.getLong(KEY_TIME, 0L)
            val star = prefs.getString(KEY_STAR, "") ?: ""
            val autoPrompt = prefs.getBoolean(KEY_AUTO_PROMPT, true)
            _calibrationFlow.value = ARCalibrationOffsets(
                yawOffsetDeg = yaw,
                pitchOffsetDeg = pitch,
                rollOffsetDeg = roll,
                lastCalibratedTimeMs = time,
                referenceStarName = star
            )
            _autoPromptEnabledFlow.value = autoPrompt
        }
    }

    fun isAutoPromptEnabled(): Boolean = _autoPromptEnabledFlow.value

    fun setAutoPromptEnabled(enabled: Boolean, context: Context? = null) {
        _autoPromptEnabledFlow.value = enabled
        val prefs = sharedPreferences ?: context?.applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs?.edit()?.putBoolean(KEY_AUTO_PROMPT, enabled)?.apply()
    }

    fun getOffsets(): ARCalibrationOffsets = _calibrationFlow.value

    fun updateOffsets(yaw: Float, pitch: Float, roll: Float, referenceStar: String = "") {
        _calibrationFlow.value = _calibrationFlow.value.copy(
            yawOffsetDeg = yaw,
            pitchOffsetDeg = pitch,
            rollOffsetDeg = roll,
            referenceStarName = referenceStar
        )
    }

    fun saveCalibration(context: Context? = null, referenceStar: String = "") {
        val current = _calibrationFlow.value
        val now = System.currentTimeMillis()
        val updated = current.copy(
            lastCalibratedTimeMs = now,
            referenceStarName = if (referenceStar.isNotEmpty()) referenceStar else current.referenceStarName
        )
        _calibrationFlow.value = updated

        val prefs = sharedPreferences ?: context?.applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs?.edit()?.apply {
            putFloat(KEY_YAW, updated.yawOffsetDeg)
            putFloat(KEY_PITCH, updated.pitchOffsetDeg)
            putFloat(KEY_ROLL, updated.rollOffsetDeg)
            putLong(KEY_TIME, updated.lastCalibratedTimeMs)
            putString(KEY_STAR, updated.referenceStarName)
            apply()
        }
    }

    fun resetCalibration(context: Context? = null) {
        _calibrationFlow.value = ARCalibrationOffsets(
            yawOffsetDeg = 0f,
            pitchOffsetDeg = 0f,
            rollOffsetDeg = 0f,
            lastCalibratedTimeMs = 0L,
            referenceStarName = ""
        )
        val prefs = sharedPreferences ?: context?.applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs?.edit()?.apply {
            putFloat(KEY_YAW, 0f)
            putFloat(KEY_PITCH, 0f)
            putFloat(KEY_ROLL, 0f)
            putLong(KEY_TIME, 0L)
            putString(KEY_STAR, "")
            apply()
        }
    }

    /**
     * OD4 one-time legacy yaw rebase. Called from CompassARScreen at the first moment
     * after upgrade when BOTH a GPS location is available and a legacy yaw offset exists.
     * Subtracts the local declination from the stored yaw offset (the legacy offset had
     * absorbed it while the app azimuth was still magnetic-referenced) and writes the
     * versioned marker so it never runs twice. Pure no-op when already rebased.
     */
    fun rebaseYawForDeclinationOnce(declinationDeg: Float, context: Context? = null) {
        val prefs = sharedPreferences ?: context?.applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?: return
        if (prefs.getBoolean(KEY_YAW_DECLINATION_REBASED_V1, false)) return
        val current = _calibrationFlow.value
        if (current.yawOffsetDeg != 0f) {
            val rebased = MagneticDeclination.rebaseLegacyYawOffset(current.yawOffsetDeg, declinationDeg)
            _calibrationFlow.value = current.copy(yawOffsetDeg = rebased)
            prefs.edit().apply {
                putFloat(KEY_YAW, rebased)
                putBoolean(KEY_YAW_DECLINATION_REBASED_V1, true)
                apply()
            }
        } else {
            // nothing to rebase, but mark done so we don't check on every frame
            prefs.edit().putBoolean(KEY_YAW_DECLINATION_REBASED_V1, true).apply()
        }
    }

    /**
     * Builds a 3x3 rotation matrix R_calib for the intrinsic Euler offsets (Yaw-Pitch-Roll in degrees)
     * in column-major FloatArray (standard Android 9-element 3x3 matrix):
     *
     *   R_calib = R_z(yaw) * R_x(pitch) * R_y(roll)
     */
    fun createCalibrationRotationMatrix(
        yawDeg: Float,
        pitchDeg: Float,
        rollDeg: Float,
        outMatrix: FloatArray = FloatArray(9)
    ): FloatArray {
        val yRad = Math.toRadians(yawDeg.toDouble())
        val pRad = Math.toRadians(pitchDeg.toDouble())
        val rRad = Math.toRadians(rollDeg.toDouble())

        val cy = cos(yRad).toFloat()
        val sy = sin(yRad).toFloat()
        val cp = cos(pRad).toFloat()
        val sp = sin(pRad).toFloat()
        val cr = cos(rRad).toFloat()
        val sr = sin(rRad).toFloat()

        // 3x3 matrix multiplication: Rz(yaw) * Rx(pitch) * Ry(roll)
        // Rz = [ cy, -sy,  0 ]
        //      [ sy,  cy,  0 ]
        //      [  0,   0,  1 ]
        //
        // Rx = [  1,   0,   0 ]
        //      [  0,  cp, -sp ]
        //      [  0,  sp,  cp ]
        //
        // Ry = [ cr,   0,  sr ]
        //      [  0,   1,   0 ]
        //      [-sr,   0,  cr ]
        //
        // Combined R_calib =
        // [ cy*cr - sy*sp*(-sr),  -sy*cp,   cy*sr + sy*sp*cr ] -> [ cy*cr + sy*sp*sr,  -sy*cp,  cy*sr - sy*sp*cr ]
        // [ sy*cr + cy*sp*(-sr),   cy*cp,   sy*sr - cy*sp*cr ] -> [ sy*cr - cy*sp*sr,   cy*cp,  sy*sr + cy*sp*cr ]
        // [ -cp*sr,                sp,      cp*cr            ]

        outMatrix[0] = cy * cr + sy * sp * sr
        outMatrix[1] = -sy * cp
        outMatrix[2] = cy * sr - sy * sp * cr

        outMatrix[3] = sy * cr - cy * sp * sr
        outMatrix[4] = cy * cp
        outMatrix[5] = sy * sr + cy * sp * cr

        outMatrix[6] = -cp * sr
        outMatrix[7] = sp
        outMatrix[8] = cp * cr

        return outMatrix
    }

    /**
     * Multiplies two 3x3 matrices in row-major/column-major format: C = A * B
     */
    fun multiplyMatrix3x3(a: FloatArray, b: FloatArray, out: FloatArray = FloatArray(9)): FloatArray {
        for (i in 0..2) {
            val r = i * 3
            for (j in 0..2) {
                out[r + j] = a[r + 0] * b[0 * 3 + j] +
                             a[r + 1] * b[1 * 3 + j] +
                             a[r + 2] * b[2 * 3 + j]
            }
        }
        return out
    }
}

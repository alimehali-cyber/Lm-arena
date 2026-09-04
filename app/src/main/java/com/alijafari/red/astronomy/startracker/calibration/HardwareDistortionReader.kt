package com.alijafari.red.astronomy.startracker.calibration

import android.hardware.camera2.CameraCharacteristics
import android.os.Build

/**
 * T4(a): where the star tracker's DistortionModel comes from, in preference order.
 *
 *  HARDWARE_DISTORTION  — read from CameraCharacteristics.LENS_DISTORTION (API 30+) or
 *                         LENS_RADIAL_DISTORTION (API 33+). Device factory calibration;
 *                         always preferred when present.
 *  SELF_CALIBRATED      — refined on-device by SelfCalibrationEngine/DistortionRefiner
 *                         from solved star fields (the PHASE7 path).
 *  NONE                 — no model (identity). The pipeline then applies the T4(b)
 *                         radius-dependent verification tolerance instead
 *                         (FullFieldVerifier.withUnmodelledDistortionAllowance).
 *
 * UNEXECUTED on hardware as of 2026-09-04: no device with LENS_DISTORTION metadata was
 * available in this pass; the reader compiles in CI (assembleDebug + unit-test compile)
 * but its branch outcomes await REAL_DEVICE_FIELD_TEST_PROTOCOL Step 8+.
 */
enum class DistortionModelSource {
    HARDWARE_DISTORTION,
    SELF_CALIBRATED,
    NONE
}

object HardwareDistortionReader {

    /**
     * Read the device's factory radial distortion from camera2 metadata.
     *
     * Mapping (see android.hardware.camera2.CameraCharacteristics.LENS_DISTORTION docs):
     * LENS_DISTORTION is float[5] {fk1, fk2, fk3, k1, k2} in NORMALIZED image coordinates,
     * where the f* coefficients map CORRECTED -> UNCORRECTED (ideal -> distorted) and the
     * k* map UNCORRECTED -> CORRECTED. Our Brown-Conrady `distortIdealToDistortedNormalized`
     * is exactly ideal -> distorted, so k1 := fk1 = element 0 and k2 := fk2 = element 1.
     * LENS_RADIAL_DISTORTION (API 33+) is float[6] {fk1..fk6}, same direction, so we use
     * elements 0 and 1 the same way. Tangential p1/p2 are not provided by camera2 — 0.0.
     *
     * @return null when the device reports no distortion metadata or an all-zero model.
     */
    fun read(chars: CameraCharacteristics): DistortionModel? {
        if (Build.VERSION.SDK_INT >= 33) {
            val radial = chars.get(CameraCharacteristics.LENS_RADIAL_DISTORTION)
            if (radial != null && radial.size >= 2) {
                val m = DistortionModel(k1 = radial[0].toDouble(), k2 = radial[1].toDouble())
                if (!m.isIdentity()) return m
            }
        }
        if (Build.VERSION.SDK_INT >= 30) {
            val lens = chars.get(CameraCharacteristics.LENS_DISTORTION)
            if (lens != null && lens.size >= 2) {
                val m = DistortionModel(k1 = lens[0].toDouble(), k2 = lens[1].toDouble())
                if (!m.isIdentity()) return m
            }
        }
        return null
    }

    /**
     * Tier selection: HARDWARE_DISTORTION above SELF_CALIBRATED above NONE. Pure Kotlin —
     * unit-tested in HardwareDistortionReaderTest. When the winner is NONE the caller
     * (StarTrackerPipeline) applies the T4(b) unmodelled-distortion allowance verifier.
     */
    fun selectDistortionModel(
        hardware: DistortionModel?,
        selfCalibrated: DistortionModel?
    ): Pair<DistortionModel, DistortionModelSource> = when {
        hardware != null -> Pair(hardware, DistortionModelSource.HARDWARE_DISTORTION)
        selfCalibrated != null && !selfCalibrated.isIdentity() ->
            Pair(selfCalibrated, DistortionModelSource.SELF_CALIBRATED)
        else -> Pair(DistortionModel.noDistortion(), DistortionModelSource.NONE)
    }
}

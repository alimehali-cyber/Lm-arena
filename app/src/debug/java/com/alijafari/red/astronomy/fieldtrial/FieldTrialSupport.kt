package com.alijafari.red.astronomy.fieldtrial

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import com.alijafari.red.astronomy.startracker.calibration.DistortionModel
import com.alijafari.red.astronomy.startracker.calibration.HardwareDistortionReader

/** Small debug-only helpers shared by the field-trial runtime and UI. */
object FieldTrialSupport {
    /** Device k1/k2 via the T4a reader (null when no metadata / API<30). */
    fun readHardwareDistortion(context: Context): DistortionModel? = runCatching {
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return null
        val id = cm.cameraIdList.firstOrNull { id ->
            cm.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) ==
                CameraCharacteristics.LENS_FACING_BACK
        } ?: cm.cameraIdList.firstOrNull() ?: return null
        HardwareDistortionReader.read(cm.getCameraCharacteristics(id))
    }.getOrNull()
}

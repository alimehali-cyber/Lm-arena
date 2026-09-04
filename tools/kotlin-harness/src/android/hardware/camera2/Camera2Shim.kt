package android.hardware.camera2

/**
 * HARNESS SHIM (tools/kotlin-harness, Z-P1) — NOT the real android.hardware.camera2.
 * Compile-only stub for ARProjectionEngine.getCameraIntrinsics (the device-only
 * intrinsics path). Unreachable offline: the Context shim returns null from
 * getSystemService, so `as? CameraManager` yields null and the function returns its
 * FALLBACK_DEFAULT tier. get() additionally returns null for every key, so even a
 * direct call could never fabricate an intrinsics value.
 * WARNING: never compile on an Android classpath together with this file.
 */
class CameraCharacteristics {
    class Key<T>

    fun <T> get(key: Key<T>): T? = null

    companion object {
        val LENS_FACING: Key<Int> = Key()
        const val LENS_FACING_BACK = 0
        val SENSOR_ORIENTATION: Key<Int> = Key()
        val SENSOR_INFO_ACTIVE_ARRAY_SIZE: Key<android.graphics.Rect> = Key()
        val SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE: Key<android.graphics.Rect> = Key()
        val LENS_INTRINSIC_CALIBRATION: Key<FloatArray> = Key()
        val LENS_INFO_AVAILABLE_FOCAL_LENGTHS: Key<FloatArray> = Key()
        val SENSOR_INFO_PHYSICAL_SIZE: Key<android.util.Size> = Key()
    }
}

class CameraManager {
    val cameraIdList: Array<String> = emptyArray()
    fun getCameraCharacteristics(cameraId: String): CameraCharacteristics = CameraCharacteristics()
}

package android.hardware.camera2

/**
 * OFFLINE HARNESS SHIM (never ships in the APK): camera2 characteristics metadata bag.
 * Only the two distortion keys the star tracker reads are modeled; `get` is an
 * unchecked-cast bag lookup so reader source compiles identically against the real API.
 */
class CameraCharacteristics(private val values: Map<Key<*>, Any?>) {
    class Key<T>(val name: String)

    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: Key<T>): T? = values[key] as T?

    companion object {
        val LENS_RADIAL_DISTORTION: Key<FloatArray> = Key("LENS_RADIAL_DISTORTION")
        val LENS_DISTORTION: Key<FloatArray> = Key("LENS_DISTORTION")
    }
}

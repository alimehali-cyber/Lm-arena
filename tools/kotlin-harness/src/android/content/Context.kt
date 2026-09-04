package android.content

/**
 * HARNESS SHIM (tools/kotlin-harness, Z-P1) — NOT the real android.content.Context.
 * Compile-only stub for ARProjectionEngine.kt (imports Context at file scope).
 * Semantics on harness paths: getSystemService returns null, which makes
 * ARProjectionEngine.getCameraIntrinsics fall through to its documented
 * FALLBACK_DEFAULT tier — the device path is uncallable offline by construction.
 * WARNING: never compile on an Android classpath together with this file.
 */
open class Context {
    fun getSystemService(name: String): Any? = null
    companion object {
        const val CAMERA_SERVICE = "camera"
    }
}

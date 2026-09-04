package android.graphics

/**
 * HARNESS SHIM (tools/kotlin-harness, Z-P1) — NOT the real android.graphics classes.
 * Compile-only stubs for ARProjectionEngine.kt:
 *  - Matrix.mapPoints is reached only when a non-null sensorToViewMatrix is passed;
 *    every harness test passes null (the stub body is a no-op and never executes).
 *  - Rect is only returned by the camera2 shim (always null there), so width()/height()
 *    never execute either.
 * WARNING: never compile on an Android classpath together with this file.
 */
class Matrix {
    fun mapPoints(pts: FloatArray) { /* no-op — see header */ }
}

class Rect {
    fun width(): Int = 0
    fun height(): Int = 0
}

package android.util

/**
 * HARNESS SHIM (tools/kotlin-harness, Z-P1) — NOT the real android.util classes.
 * Compile-only stubs for ARProjectionEngine.kt:
 *  - Log.d is called only from the debug-tier logger; no-op, no output, no behavior.
 *  - Size is only the declared return type of a camera2 key whose shim get() is null;
 *    members never execute.
 * WARNING: never compile on an Android classpath together with this file.
 */
object Log {
    fun d(tag: String, msg: String) { /* no-op */ }
    fun w(tag: String, msg: String) { /* no-op */ }
    fun e(tag: String, msg: String) { /* no-op */ }
}

class Size(val width: Int, val height: Int)

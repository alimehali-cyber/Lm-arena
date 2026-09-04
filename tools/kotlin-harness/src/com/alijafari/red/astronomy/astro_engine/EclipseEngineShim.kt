package com.alijafari.red.astronomy.astro_engine

/**
 * HARNESS SHIM (tools/kotlin-harness) — NOT the real engine.
 * MoonEngine.kt instantiates EclipseEngine() for moon-PHASE listing only (newMoonTime/
 * quarterMoonTime/fullMoonTime); the real EclipseEngine.kt pulls in Android-dependent
 * util files and cannot compile offline. This stub makes MoonEngine compilable; the
 * shim methods are never called by the coordinate-oracle probe.
 * WARNING: never compile the real EclipseEngine.kt together with this file (duplicate
 * class declaration).
 */
class EclipseEngine {
    fun newMoonTime(k: Long): Long = throw UnsupportedOperationException("harness shim")
    fun quarterMoonTime(k: Long, quarter: Double): Long = throw UnsupportedOperationException("harness shim")
    fun fullMoonTime(k: Long): Long = throw UnsupportedOperationException("harness shim")
}

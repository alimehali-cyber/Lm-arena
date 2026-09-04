package com.alijafari.red.astronomy

/**
 * HARNESS SHIM (tools/kotlin-harness, Z-P1) — NOT the Gradle-generated BuildConfig.
 * Compile-only stand-in: ARProjectionEngine reads BuildConfig.DEBUG solely to gate
 * debug logging of the intrinsics tier. DEBUG=false keeps even that no-op.
 * WARNING: never compile on an Android classpath together with this file — the real
 * BuildConfig is generated per build variant.
 */
object BuildConfig {
    const val DEBUG = false
}

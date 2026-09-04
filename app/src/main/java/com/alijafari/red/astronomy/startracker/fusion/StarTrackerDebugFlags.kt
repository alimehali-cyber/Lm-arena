package com.alijafari.red.astronomy.startracker.fusion

/**
 * D1 (debug-diagnostics pass, 2026-09-04): runtime overrides for the star-tracker
 * gate flags, so REAL_DEVICE_FIELD_TEST_PROTOCOL.md can be executed on a phone by a
 * non-developer instead of by recompiling with different consts.
 *
 * Scope contract:
 *  - RELEASE: call sites must NOT install an overrides provider. [RELEASE_DEFAULTS]
 *    (or [resolve] with a null provider) returns exactly the compile-time consts —
 *    bit-identical to reading StarTrackerConfig directly, zero I/O. The debug-only
 *    UI that installs the SharedPreferences-backed provider lives in the `debug`
 *    source set and is not compiled into release builds at all.
 *  - DEBUG: the provider is a `(key) -> String?` getter (SharedPreferences-backed on
 *    device; a plain map in tests). Only the exact strings "true"/"false" count as
 *    overrides; null/absent/unrecognized values fall back to the const default —
 *    a malformed stored value can never enable a flag on its own.
 *  - AND-semantics are NOT applied here: the W2 call sites combine
 *    `ENABLED && PIPELINE_CAMERA_FEED` etc. themselves; this type only resolves.
 */
object StarTrackerDebugFlags {

    const val KEY_ENABLED = "startracker.debug.enabled"
    const val KEY_PIPELINE_CAMERA_FEED = "startracker.debug.pipeline_camera_feed"
    const val KEY_TRACKER_TO_ORIENTATION_PHASE6 = "startracker.debug.tracker_to_orientation_phase6"
    const val KEY_PROJECTION_SELF_CALIBRATED_PHASE7 = "startracker.debug.projection_self_calibrated_phase7"

    /** Resolved state of the four gate flags. */
    data class Resolution(
        val enabled: Boolean,
        val pipelineCameraFeed: Boolean,
        val trackerToOrientationPhase6: Boolean,
        val projectionSelfCalibratedPhase7: Boolean
    ) {
        companion object {
            /** The const (release) resolution — what the app did before this class existed. */
            fun fromConsts(): Resolution = Resolution(
                enabled = StarTrackerConfig.ENABLED,
                pipelineCameraFeed = StarTrackerConfig.PIPELINE_CAMERA_FEED,
                trackerToOrientationPhase6 = StarTrackerConfig.TRACKER_TO_ORIENTATION_PHASE6,
                projectionSelfCalibratedPhase7 = StarTrackerConfig.PROJECTION_SELF_CALIBRATED_PHASE7
            )
        }
    }

    /** Release path: the consts, directly. Also the fallback when no provider exists. */
    val RELEASE_DEFAULTS: Resolution = Resolution.fromConsts()

    @Volatile
    private var overridesProvider: ((String) -> String?)? = null

    @Volatile
    private var cachedRuntime: Resolution? = null

    /**
     * G-P0: install (or replace/null) the overrides provider. Called ONLY by the
     * debug UI (SharedPreferences-backed); release builds never install one, so
     * [runtime] there is the consts, bit-identical (cached once, ~zero per-frame cost).
     * Re-installing refreshes the cache — that is the debug toggle path.
     */
    fun installOverridesProvider(provider: ((String) -> String?)?) {
        overridesProvider = provider
        cachedRuntime = null
    }

    /** G-P0: resolution for RUNTIME consumers (camera-feed gate, bind gate, PHASE7 tier). */
    fun runtime(): Resolution {
        val p0 = cachedRuntime
        if (p0 != null) return p0
        val r = resolve(overridesProvider)
        cachedRuntime = r
        return r
    }

    /**
     * Resolve the four flags against an overrides provider (null = no overrides).
     * Pure function; harness- and CI-testable.
     */
    fun resolve(overrides: ((String) -> String?)?): Resolution {
        if (overrides == null) return RELEASE_DEFAULTS
        return Resolution(
            enabled = bool(overrides, KEY_ENABLED, StarTrackerConfig.ENABLED),
            pipelineCameraFeed = bool(overrides, KEY_PIPELINE_CAMERA_FEED, StarTrackerConfig.PIPELINE_CAMERA_FEED),
            trackerToOrientationPhase6 = bool(overrides, KEY_TRACKER_TO_ORIENTATION_PHASE6, StarTrackerConfig.TRACKER_TO_ORIENTATION_PHASE6),
            projectionSelfCalibratedPhase7 = bool(overrides, KEY_PROJECTION_SELF_CALIBRATED_PHASE7, StarTrackerConfig.PROJECTION_SELF_CALIBRATED_PHASE7)
        )
    }

    private fun bool(getter: (String) -> String?, key: String, constDefault: Boolean): Boolean = when (getter(key)) {
        "true" -> true
        "false" -> false
        else -> constDefault
    }
}

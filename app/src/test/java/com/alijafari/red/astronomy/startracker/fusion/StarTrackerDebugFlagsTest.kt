package com.alijafari.red.astronomy.startracker.fusion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * D1: resolution logic for the runtime debug overrides. Pure Kotlin — runs in the
 * offline harness and in CI. The SharedPreferences-backed provider itself is
 * Android-only (debug source set) and UNEXECUTED until the device trial.
 */
class StarTrackerDebugFlagsTest {

    private fun providerOf(vararg pairs: Pair<String, String>): (String) -> String? = { k -> pairs.firstOrNull { it.first == k }?.second }

    @Test
    fun `null or absent provider resolves to the consts bit-identically`() {
        val r = StarTrackerDebugFlags.resolve(null)
        assertEquals(StarTrackerConfig.ENABLED, r.enabled)
        assertEquals(StarTrackerConfig.PIPELINE_CAMERA_FEED, r.pipelineCameraFeed)
        assertEquals(StarTrackerConfig.TRACKER_TO_ORIENTATION_PHASE6, r.trackerToOrientationPhase6)
        assertEquals(StarTrackerConfig.PROJECTION_SELF_CALIBRATED_PHASE7, r.projectionSelfCalibratedPhase7)
        // every const currently false (tracker disabled) — pins the safety contract
        assertFalse(r.enabled || r.pipelineCameraFeed || r.trackerToOrientationPhase6 || r.projectionSelfCalibratedPhase7)
        assertEquals(StarTrackerDebugFlags.RELEASE_DEFAULTS, r)
        // provider present but answering nothing = same thing
        assertEquals(StarTrackerDebugFlags.RELEASE_DEFAULTS, StarTrackerDebugFlags.resolve(providerOf()))
    }

    @Test
    fun `each flag overrides independently and only with exact true false strings`() {
        // all four on, one at a time
        val e = StarTrackerDebugFlags.resolve(providerOf(StarTrackerDebugFlags.KEY_ENABLED to "true"))
        assertTrue(e.enabled)
        assertFalse(e.pipelineCameraFeed)
        assertFalse(e.trackerToOrientationPhase6)
        assertFalse(e.projectionSelfCalibratedPhase7)
        val p = StarTrackerDebugFlags.resolve(providerOf(StarTrackerDebugFlags.KEY_PIPELINE_CAMERA_FEED to "true"))
        assertTrue(p.pipelineCameraFeed && !p.enabled)
        val p6 = StarTrackerDebugFlags.resolve(providerOf(StarTrackerDebugFlags.KEY_TRACKER_TO_ORIENTATION_PHASE6 to "true"))
        assertTrue(p6.trackerToOrientationPhase6 && !p.enabled)
        val p7 = StarTrackerDebugFlags.resolve(providerOf(StarTrackerDebugFlags.KEY_PROJECTION_SELF_CALIBRATED_PHASE7 to "true"))
        assertTrue(p7.projectionSelfCalibratedPhase7 && !p.enabled)
        // explicit false on a false const stays false
        assertFalse(StarTrackerDebugFlags.resolve(providerOf(StarTrackerDebugFlags.KEY_ENABLED to "false")).enabled)
    }

    @Test
    fun `malformed values fall back to the const default`() {
        for (bad in listOf("TRUE", "True", "1", "yes", "", " null ")) {
            val r = StarTrackerDebugFlags.resolve(providerOf(StarTrackerDebugFlags.KEY_ENABLED to bad))
            assertEquals("const default for <$bad>", StarTrackerConfig.ENABLED, r.enabled)
        }
    }

    @Test
    fun `keys are stable distinct strings and defaults stay in sync with the consts`() {
        val keys = listOf(
            StarTrackerDebugFlags.KEY_ENABLED,
            StarTrackerDebugFlags.KEY_PIPELINE_CAMERA_FEED,
            StarTrackerDebugFlags.KEY_TRACKER_TO_ORIENTATION_PHASE6,
            StarTrackerDebugFlags.KEY_PROJECTION_SELF_CALIBRATED_PHASE7
        )
        assertEquals(4, keys.toSet().size)
        // a provider answering OTHER keys must not disturb anything
        val r = StarTrackerDebugFlags.resolve(providerOf("some.other.key" to "true"))
        assertEquals(StarTrackerDebugFlags.RELEASE_DEFAULTS, r)
    }
}

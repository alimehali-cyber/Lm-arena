package com.zig.chal

import com.zig.chal.config.ChalCameraConfig
import com.zig.chal.config.ChalFeatures
import com.zig.chal.config.ChalFormatting
import com.zig.chal.config.ChalPerformanceConfig
import com.zig.chal.config.ChalPresetName
import com.zig.chal.config.ChalRayTracingQuality
import com.zig.chal.config.ChalSettingsSink
import com.zig.chal.config.ChalSimulationConfig
import com.zig.chal.config.ChalSimulationParams
import com.zig.chal.config.ChalXapkScenario
import com.zig.chal.config.XapkCameraState
import com.zig.chal.config.XapkRendererContract
import com.zig.chal.config.XapkSettingsCodec
import com.zig.chal.config.XapkSettingsKeys
import com.zig.chal.render.ChalRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Guards for the session-persistence, control-honesty and telemetry fixes.
 *
 * The first port of this screen kept the camera in GL memory only, dropped four feature toggles when
 * saving, printed a quality tier the shader was not running, and offered two quality buttons that
 * resolved to the same ray budget. Each of those is a numeric contract, so each one is pinned here.
 */
class ChalSessionPersistenceTest {

    /** In-memory [ChalSettingsSink]: the codec is pure, so this is the whole storage dependency. */
    private class FakeSink : ChalSettingsSink {
        val store = mutableMapOf<String, Any>()
        var commits = 0

        override fun contains(key: String): Boolean = store.containsKey(key)
        override fun getFloat(key: String, default: Float): Float = (store[key] as? Float) ?: default
        override fun getBoolean(key: String, default: Boolean): Boolean = (store[key] as? Boolean) ?: default
        override fun getString(key: String): String? = store[key] as? String
        override fun putFloat(key: String, value: Float) { store[key] = value }
        override fun putBoolean(key: String, value: Boolean) { store[key] = value }
        override fun putString(key: String, value: String) { store[key] = value }
        override fun commit() { commits++ }
    }

    private fun configuredSession(): ChalSimulationParams = ChalSimulationParams(
        mass = 7.0,
        spin = 0.91,
        diskDensity = 6.5,
        diskTemp = 18_000.0,
        lensing = 1.4,
        diskSize = 88.0,
        diskScaleHeight = 0.33,
        frameDraggingStrength = 3.2,
        bloomThreshold = 2.5,
        bloomIntensity = 1.25,
        autoSpin = 0.02,
        features = ChalFeatures.getPreset(ChalPresetName.ULTRA_QUALITY).copy(
            bloom = true,
            kerrShadow = true,
            spacetimeVisualization = true,
            relativisticJets = true
        ),
        paused = true
    ).withCamera(0.31, 0.44, 55.0)

    // ------------------------------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------------------------------

    @Test
    fun theSessionCameraPauseAndTogglesSurviveARoundTrip() {
        val sink = FakeSink()
        val session = configuredSession()
        XapkSettingsCodec.save(sink, session)
        val restored = XapkSettingsCodec.load(sink, ChalRayTracingQuality.LOW)

        assertEquals("the observer azimuth must survive", 0.31, restored.cameraYaw, 1e-4)
        assertEquals("the observer pitch must survive", 0.44, restored.cameraPitch, 1e-4)
        assertEquals("the legacy angle must stay in step", restored.cameraPitch * 180.0, restored.verticalAngle, 1e-6)
        assertEquals("the observer distance must survive", 55.0, restored.zoom, 1e-3)
        assertTrue("a parked session must come back parked", restored.paused)
        assertTrue("bloom was silently dropped by the first save", restored.features.bloom)
        assertTrue("the Kerr shadow guide was silently dropped", restored.features.kerrShadow)
        assertTrue("the spacetime module was silently dropped", restored.features.spacetimeVisualization)
        assertTrue("the jets module was silently dropped", restored.features.relativisticJets)
        assertEquals("the physical values must survive", 7.0, restored.mass, 1e-6)
        assertEquals("the physical values must survive", 18_000.0, restored.diskTemp, 1.0)
        assertEquals("a stored tier must beat the device default", ChalRayTracingQuality.ULTRA, restored.features.rayTracingQuality)
        // The matrix above deliberately customises Ultra (Kerr guide + spacetime + jets), so the
        // honest preset badge is "custom" - the tier still survives, the *name* must not.
        assertEquals("the stored tier must survive", ChalRayTracingQuality.ULTRA, restored.features.rayTracingQuality)
        assertEquals("a customised matrix must be reported as custom", ChalPresetName.CUSTOM, restored.performancePreset)
        assertEquals("loading must be idempotent", restored, XapkSettingsCodec.load(sink, ChalRayTracingQuality.LOW))
    }

    @Test
    fun anUntouchedPresetRoundTripsItsName() {
        val sink = FakeSink()
        val pristine = ChalSimulationParams(
            features = ChalFeatures.getPreset(ChalPresetName.ULTRA_QUALITY),
            performancePreset = ChalPresetName.ULTRA_QUALITY
        )
        XapkSettingsCodec.save(sink, pristine)
        val restored = XapkSettingsCodec.load(sink, ChalRayTracingQuality.LOW)
        assertEquals(pristine.features, restored.features)
        assertEquals(ChalPresetName.ULTRA_QUALITY, restored.performancePreset)
    }

    @Test
    fun oneSaveIsOneTransactionAndKeepsTheXapkKeyNames() {
        val sink = FakeSink()
        XapkSettingsCodec.save(sink, configuredSession())

        assertEquals("the session must be written in a single commit", 1, sink.commits)
        assertTrue("the reference app's own key names must be reused", sink.store.containsKey("mass"))
        assertTrue(sink.store.containsKey("diskTemp"))
        assertTrue(sink.store.containsKey("frameDrag"))
        assertEquals("the stored quality must use the reference's own name", "ULTRA", sink.store[XapkSettingsKeys.QUALITY])
    }

    @Test
    fun aFreshInstallReproducesTheReferenceFeatureSet() {
        val params = XapkSettingsCodec.load(FakeSink(), ChalRayTracingQuality.HIGH)
        assertEquals(XapkSettingsCodec.defaultFeatures(ChalRayTracingQuality.HIGH), params.features)
        assertEquals(ChalFeatures.matchesPreset(params.features), params.performancePreset)
        assertFalse("a fresh session must never start parked", params.paused)
        assertTrue("a fresh session must not start in the analytic preview", params.features.rayTracingQuality != ChalRayTracingQuality.OFF)
        assertEquals(
            "a fresh session must start at the configured tilt",
            ChalSimulationConfig.VERTICAL_ANGLE.default,
            params.verticalAngle,
            1e-9
        )

        val cheap = XapkSettingsCodec.defaultFeatures(ChalRayTracingQuality.LOW)
        assertFalse("a low tier must not pay for bloom", cheap.bloom)
        assertFalse("a low tier must not pay for the photon ring", cheap.photonSphereGlow)
        assertTrue("a low tier must still show the lensing and the disk", cheap.gravitationalLensing && cheap.accretionDisk)
    }

    @Test
    fun aFileFromTheReferenceAppKeepsItsValuesAndGetsTheDefaultCamera() {
        val sink = FakeSink().apply {
            putFloat(XapkSettingsKeys.MASS, 5.0f)
            putFloat(XapkSettingsKeys.SPIN, 0.8f)
            putString(XapkSettingsKeys.QUALITY, "MEDIUM")
        }
        val params = XapkSettingsCodec.load(sink, ChalRayTracingQuality.HIGH)

        assertEquals(5.0, params.mass, 1e-6)
        assertEquals(0.8, params.spin, 1e-6)
        assertEquals(ChalRayTracingQuality.MEDIUM, params.features.rayTracingQuality)
        assertEquals("a file without camera keys must start centred", XapkCameraState.DEFAULT_YAW.toDouble(), params.cameraYaw, 1e-9)
        assertEquals(XapkCameraState.DEFAULT_PITCH.toDouble(), params.cameraPitch, 1e-6)
        assertEquals(ChalCameraConfig.DEFAULT_ZOOM, params.zoom, 1e-9)
    }

    // ------------------------------------------------------------------------------------------
    // Control honesty
    // ------------------------------------------------------------------------------------------

    @Test
    fun cameraEditsKeepTheLegacyAngleAndTheRangeLimits() {
        val defaults = ChalSimulationParams()
        val moved = defaults.withCamera(-0.25, 0.6, 40.0)
        assertEquals("a negative azimuth must wrap into 0..1", 0.75, moved.cameraYaw, 1e-9)
        assertEquals(0.6, moved.cameraPitch, 1e-9)
        assertEquals(108.0, moved.verticalAngle, 1e-9)
        assertEquals(40.0, moved.zoom, 1e-9)

        val clamped = defaults.withCamera(0.5, 0.02, 1e9)
        assertEquals("the pitch must stay inside the XAPK range", 0.05, clamped.cameraPitch, 1e-6)
        assertEquals("the distance must stay inside the camera range", ChalCameraConfig.MAX_ZOOM, clamped.zoom, 1e-9)

        val state = moved.cameraState()
        assertEquals(moved.cameraYaw.toFloat(), state.yaw, 0.0f)
        assertEquals(moved.cameraPitch.toFloat(), state.pitch, 0.0f)
        assertEquals(moved.zoom.toFloat(), state.distance, 0.0f)
    }

    @Test
    fun everyControlSnapsTheValueItDisplays() {
        assertEquals("the readout must be the snapped value", "3.1", ChalSimulationConfig.MASS.format(3.14159))
        assertEquals(3.1, ChalSimulationConfig.MASS.snap(3.14159), 1e-9)
        assertEquals(0.99, ChalSimulationConfig.SPIN.snap(0.987), 1e-9)
        assertEquals(12_500.0, ChalSimulationConfig.DISK_TEMP.snap(12_345.0), 1e-6)
        assertEquals("snapping must clamp", 20.0, ChalSimulationConfig.MASS.snap(1e9), 1e-9)
    }

    @Test
    fun everyScenarioSurvivesSnappingEveryControl() {
        for (scenario in ChalXapkScenario.entries) {
            val applied = scenario.applyTo(ChalSimulationParams())
            val snapped = applied.copy(
                mass = ChalSimulationConfig.MASS.snap(applied.mass),
                spin = ChalSimulationConfig.SPIN.snap(applied.spin),
                lensing = ChalSimulationConfig.LENSING.snap(applied.lensing),
                diskSize = ChalSimulationConfig.DISK_SIZE.snap(applied.diskSize),
                diskScaleHeight = ChalSimulationConfig.DISK_SCALE_HEIGHT.snap(applied.diskScaleHeight),
                diskTemp = ChalSimulationConfig.DISK_TEMP.snap(applied.diskTemp),
                diskDensity = ChalSimulationConfig.DISK_DENSITY.snap(applied.diskDensity),
                frameDraggingStrength = ChalSimulationConfig.FRAME_DRAGGING.snap(applied.frameDraggingStrength),
                bloomThreshold = ChalSimulationConfig.BLOOM_THRESHOLD.snap(applied.bloomThreshold),
                bloomIntensity = ChalSimulationConfig.BLOOM_INTENSITY.snap(applied.bloomIntensity),
                autoSpin = ChalSimulationConfig.AUTO_SPIN.snap(applied.autoSpin),
                zoom = ChalSimulationConfig.ZOOM.snap(applied.zoom)
            )
            assertEquals(
                "the '$scenario' preset must stay selected after its values are snapped",
                scenario,
                ChalXapkScenario.matching(snapped)
            )
        }
    }

    @Test
    fun theCappedTiersAreReportedInsteadOfDuplicated() {
        assertTrue("High is capped by the mobile budget", ChalFeatures.isCappedForMobile(ChalRayTracingQuality.HIGH))
        assertTrue("Ultra is capped by the mobile budget", ChalFeatures.isCappedForMobile(ChalRayTracingQuality.ULTRA))
        assertFalse("Medium is not capped", ChalFeatures.isCappedForMobile(ChalRayTracingQuality.MEDIUM))
        assertTrue(
            "High and Ultra are the same render on this build and the UI must say so",
            ChalFeatures.rendersIdentically(ChalRayTracingQuality.HIGH, ChalRayTracingQuality.ULTRA)
        )
        assertFalse(ChalFeatures.rendersIdentically(ChalRayTracingQuality.LOW, ChalRayTracingQuality.MEDIUM))
        assertEquals(
            XapkRendererContract.Quality.LOW,
            XapkRendererContract.qualityFor(ChalRayTracingQuality.OFF)
        )

        assertTrue(ChalFormatting.rayBudget(ChalRayTracingQuality.HIGH, isPersian = false, isMobile = true).contains("capped"))
        assertFalse(ChalFormatting.rayBudget(ChalRayTracingQuality.MEDIUM, isPersian = false, isMobile = true).contains("capped"))
        assertTrue(ChalFormatting.rayBudget(ChalRayTracingQuality.OFF, isPersian = false, isMobile = true).contains("no ray marching"))
        assertNotNull(
            "the duplicate-tier note must name the interchangeable pair",
            ChalFormatting.duplicateTierNote(
                listOf(ChalRayTracingQuality.HIGH, ChalRayTracingQuality.ULTRA),
                isPersian = false
            )?.contains("High and Ultra")
        )
        assertNull(
            "distinct tiers must not be called identical",
            ChalFormatting.duplicateTierNote(
                listOf(ChalRayTracingQuality.LOW, ChalRayTracingQuality.MEDIUM),
                isPersian = false
            )
        )
    }

    @Test
    fun persianNumbersAreConvertedAndLatinOnesAreLeftAlone() {
        assertTrue(ChalFormatting.digits("12.5", isPersian = true).contains('۲'))
        assertFalse(ChalFormatting.digits("12.5", isPersian = true).contains('2'))
        assertEquals("12.5", ChalFormatting.digits("12.5", isPersian = false))
        assertEquals("75%", ChalFormatting.percent(0.75, isPersian = false))
        assertEquals("1234.5", ChalFormatting.fixed(1234.5, 1, isPersian = false))
    }

    // ------------------------------------------------------------------------------------------
    // Telemetry contract
    // ------------------------------------------------------------------------------------------

    @Test
    fun theGlesTelemetryReportsTheTierTheShaderActuallyRuns() {
        val renderer = ChalRenderer()
        val snapshot = renderer.snapshot()
        assertEquals(
            "the HUD must report the tier the shader was compiled with, not the PID target",
            snapshot.params.features.rayTracingQuality,
            snapshot.quality
        )
        assertEquals(
            "the ray budget readout must be the budget the shader requests",
            ChalFeatures.getMaxRaySteps(
                snapshot.quality,
                isMobile = ChalPerformanceConfig.Mobile.IS_MOBILE_HARDWARE
            ),
            snapshot.raySteps
        )
        assertTrue(
            "the reported render scale must be the adaptive controller's value",
            snapshot.effectiveRenderScale >= ChalSimulationConfig.RENDER_SCALE.min &&
                snapshot.effectiveRenderScale <= ChalSimulationConfig.RENDER_SCALE.max
        )
    }

    @Test
    fun theNativeContractCarriesTheSessionCamera() {
        val block = XapkRendererContract.build(ChalSimulationParams().withCamera(0.25, 0.7, 42.0))
        assertEquals(16, block.floats.size)
        assertEquals(4, block.integers.size)
        assertTrue(abs(block.floats[11] - 0.25f) < 1e-6f)
        assertTrue(abs(block.floats[12] - 0.7f) < 1e-6f)
        assertTrue(abs(block.floats[13] - 42.0f) < 1e-3f)
    }

}

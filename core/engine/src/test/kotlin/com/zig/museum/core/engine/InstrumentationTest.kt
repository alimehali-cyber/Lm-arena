package com.zig.museum.core.engine

import org.junit.Assert.*
import org.junit.Test

/**
 * Foundational Rebuild Phase 0.1: unit tests for the honest render-loop counters and the
 * DebugOverlayData they feed. These are the pure-Kotlin parts of the Phase 0.1 fix (real
 * beginFrame/render/endFrame counters, no hardcoded tile literals) that do not require an actual
 * Filament Engine/device to exercise -- InspectorEngine.doFrame() itself is not unit-tested here
 * because it needs a real Filament SwapChain/Renderer/View, which this JVM unit test environment
 * cannot provide (see the Phase 0+1 report's explicit "cannot verify without a real device"
 * section for what IS only verifiable on-device).
 */
class InstrumentationTest {

    @Test
    fun freshRenderLoopCountersReportNoPresentedFrames() {
        val counters = RenderLoopCounters()
        assertFalse(
            "A default/zero RenderLoopCounters must never claim a frame was presented",
            counters.hasPresentedAtLeastOneFrame()
        )
        assertEquals(0L, counters.doFrameCalls)
        assertEquals(0L, counters.beginFrameAttempts)
        assertEquals(0L, counters.endFrameCalls)
    }

    @Test
    fun hasPresentedAtLeastOneFrameOnlyTrueAfterRealEndFrame() {
        // Simulates exactly the scenario this pass exists to catch: doFrame() ran many times and
        // beginFrame() was even attempted, but never actually completed a render+endFrame cycle
        // (e.g. beginFrame() kept returning false, or kept throwing). The old fps-only metric
        // could not distinguish this from real presented frames; the new counters can.
        val stuck = RenderLoopCounters(
            doFrameCalls = 500,
            beginFrameAttempts = 500,
            beginFrameFalse = 500
        )
        assertFalse(
            "500 doFrame calls with beginFrame always returning false must NOT be reported as " +
                "having presented any real frame",
            stuck.hasPresentedAtLeastOneFrame()
        )

        val real = stuck.copy(beginFrameTrue = 1, renderCalls = 1, endFrameCalls = 1)
        assertTrue(
            "A single real beginFrame=true/render/endFrame cycle must be reported as presented",
            real.hasPresentedAtLeastOneFrame()
        )
    }

    @Test
    fun summaryIncludesAllCountersAndViewportSize() {
        val counters = RenderLoopCounters(
            doFrameCalls = 10,
            beginFrameAttempts = 8,
            beginFrameTrue = 6,
            beginFrameFalse = 2,
            beginFrameThrew = 0,
            renderCalls = 6,
            renderThrew = 0,
            endFrameCalls = 6,
            endFrameThrew = 0,
            swapChainNullCount = 1,
            rendererNullCount = 1,
            viewNullCount = 0,
            lastViewportWidth = 1080,
            lastViewportHeight = 2280
        )
        val summary = counters.summary()
        // Every field must actually appear in the human-readable summary -- this is the string
        // the debug overlay renders directly (see SpaceMuseumViewerScreen.kt's "render loop: "
        // Text), so if a field silently dropped out of summary() it would silently disappear from
        // the on-device honesty signal too.
        listOf("10", "8", "6", "2", "1080", "2280").forEach { token ->
            assertTrue("summary() must mention '$token': $summary", summary.contains(token))
        }
    }

    @Test
    fun instrumentationDefaultsToTileStoreNotActive() {
        // Foundational Rebuild Phase 0.1: no tile/asset system is wired into the render path yet
        // (see docs/audit/MILESTONE_AUDIT.md M3), so a fresh Instrumentation must report that
        // honestly rather than defaulting to an "active-looking" state.
        val instrumentation = Instrumentation()
        assertFalse(instrumentation.tileStoreActive)
        val overlay = instrumentation.toDebugOverlay()
        assertFalse(overlay.tileStoreActive)
        assertEquals(0, overlay.residentTiles)
        assertEquals(0L, overlay.residentBytes)
    }

    @Test
    fun toDebugOverlayCarriesRenderLoopCountersThrough() {
        val instrumentation = Instrumentation()
        instrumentation.renderLoop = RenderLoopCounters(doFrameCalls = 42)
        val overlay = instrumentation.toDebugOverlay()
        assertEquals(42L, overlay.renderLoop.doFrameCalls)
    }
}

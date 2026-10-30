package com.alijafari.red.astronomy.ui.rendering.landscape

import org.junit.Assert.assertTrue
import org.junit.Test

class PulvarRiverFramingTest {
    @Test fun geometryContainsSubstantialBedAndWater() {
        val counts = pulvarGeometryCounts()
        assertTrue(counts.opaqueVerts > 100000)
        assertTrue(counts.waterVerts > 5000)
    }
}

package com.alijafari.red.astronomy.ui.rendering.landscape

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PulvarRiverFramingTest {
    @Test fun geometryContainsSubstantialBedAndWater() {
        val counts = pulvarGeometryCounts()
        assertTrue(counts.opaqueVerts > 100000)
        assertTrue(counts.waterVerts > 5000)
    }
    @Test fun bothBanksStayOnTheExistingPlantingExclusionAcrossEveryBend() {
        for (x in -110..110 step 2) {
            for (side in listOf(-1f, 1f)) {
                val world = pulvarChannelPointXZ(x.toFloat(), side * pulvarHalfWidth(x.toFloat()))
                val vertex = pulvarLandscapeVertex(world[0], pulvarWaterY(x.toFloat()), world[1])
                assertEquals("bank at $x", side, vertex[2], 0.003f)
                assertEquals("center at $x", (x + 112f) / 224f, vertex[0], 0.003f)
                assertEquals("water elevation at $x", 0f, vertex[3], 0.003f)
            }
        }
    }
}

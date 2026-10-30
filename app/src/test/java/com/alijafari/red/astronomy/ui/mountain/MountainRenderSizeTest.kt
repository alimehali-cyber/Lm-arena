package com.alijafari.red.astronomy.ui.mountain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class MountainRenderSizeTest {
    @Test
    fun homeShadesAboutHalfThePixelsWithoutChangingTheAspectRatio() {
        val size = mountainRenderSize(1080, 1500, HOME_MOUNTAIN_RENDER_SCALE)
        assertEquals(MountainRenderSize(756, 1050), size)
        assertEquals(0.49, size.width.toDouble() * size.height / (1080 * 1500), 0.00001)
        assertEquals(1080.0 / 1500, size.width.toDouble() / size.height, 0.00001)
    }

    @Test
    fun nativeResolutionCanBeRestored() {
        assertEquals(MountainRenderSize(1080, 1500), mountainRenderSize(1080, 1500, 1f))
        assertEquals(MountainRenderSize(1080, 1500), mountainRenderSize(1080, 1500, 2f))
    }

    @Test
    fun scaleIsBoundedAndNonFiniteInputFallsBackToNativeResolution() {
        assertEquals(0.4f, clampMountainRenderScale(-1f), 0f)
        assertEquals(0.4f, clampMountainRenderScale(0f), 0f)
        assertEquals(0.7f, clampMountainRenderScale(0.7f), 0f)
        assertEquals(1f, clampMountainRenderScale(2f), 0f)
        for (scale in floatArrayOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            assertEquals(1f, clampMountainRenderScale(scale), 0f)
            assertEquals(MountainRenderSize(100, 200), mountainRenderSize(100, 200, scale))
        }
        assertEquals(MountainRenderSize(40, 80), mountainRenderSize(100, 200, 0f))
    }

    @Test
    fun tinyAndNotYetLaidOutViewsNeverRequestAZeroSizedBuffer() {
        for (scale in floatArrayOf(0.4f, 0.7f, 1f)) {
            assertEquals(MountainRenderSize(1, 1), mountainRenderSize(1, 1, scale))
            assertEquals(MountainRenderSize(1, 1), mountainRenderSize(0, -1, scale))
        }
    }

    @Test
    fun rotationAndOddViewSizesRecomputeTheBufferWithinOnePixel() {
        for ((width, height) in listOf(1081 to 1499, 1499 to 1081, 1440 to 560)) {
            val scale = HOME_MOUNTAIN_RENDER_SCALE
            val size = mountainRenderSize(width, height, scale)
            assertTrue(abs(size.width - width * scale) < 1f)
            assertTrue(abs(size.height - height * scale) < 1f)
            assertTrue(abs(width.toDouble() / height - size.width.toDouble() / size.height) < 0.01)
            assertEquals(MountainRenderSize(width, height), mountainRenderSize(width, height, 1f))
        }
    }
}

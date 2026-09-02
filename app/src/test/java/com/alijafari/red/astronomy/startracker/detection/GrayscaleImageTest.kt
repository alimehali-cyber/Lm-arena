package com.alijafari.red.astronomy.startracker.detection

import org.junit.Test
import org.junit.Assert.*

class GrayscaleImageTest {

    @Test
    fun testCreateAndGetSet() {
        val img = GrayscaleImage.create(10, 10, 5f)
        assertEquals(5f, img.get(0, 0), 1e-6f)
        img.set(3, 4, 10f)
        assertEquals(10f, img.get(3, 4), 1e-6f)
        assertEquals(5f, img.get(2, 4), 1e-6f)
    }

    @Test
    fun testStats() {
        val img = GrayscaleImage.create(2, 2, 0f)
        img.set(0, 0, 1f)
        img.set(1, 0, 2f)
        img.set(0, 1, 3f)
        img.set(1, 1, 4f)
        val stats = img.stats()
        assertEquals(1f, stats.min, 1e-6f)
        assertEquals(4f, stats.max, 1e-6f)
        assertEquals(2.5f, stats.mean, 1e-6f)
    }
}

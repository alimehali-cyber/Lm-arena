package com.alijafari.red.astronomy.startracker.calibration

import org.junit.Test
import org.junit.Assert.*

class CameraProfileCacheTest {

    @Test
    fun testPutAndGet() {
        val cache = InMemoryCameraProfileCache()
        val profile = CameraProfile(
            fx = 1000.0, fy = 1000.0, cx = 960.0, cy = 540.0,
            deviceLensKey = "TEST"
        )

        cache.put("TEST", profile)
        val retrieved = cache.get("TEST")

        assertNotNull(retrieved)
        assertEquals(1000.0, retrieved!!.fx, 1e-9)
    }

    @Test
    fun testMergeWeighted() {
        val cache = InMemoryCameraProfileCache()

        val profile1 = CameraProfile(
            fx = 1000.0, fy = 1000.0, cx = 960.0, cy = 540.0,
            sampleCount = 100,
            deviceLensKey = "TEST"
        )

        val profile2 = CameraProfile(
            fx = 1200.0, fy = 1200.0, cx = 960.0, cy = 540.0,
            sampleCount = 20,
            deviceLensKey = "TEST"
        )

        cache.put("TEST", profile1)
        cache.merge("TEST", profile2, 20)

        val merged = cache.get("TEST")!!
        // Weighted: (100*1000 + 20*1200)/120 = (100000+24000)/120 = 124000/120 = 1033.33
        val expectedFx = (100 * 1000.0 + 20 * 1200.0) / 120.0

        println("Merge test: existing fx=1000 (100 samples), new fx=1200 (20 samples), merged fx=${merged.fx}, expected $expectedFx")
        assertEquals(expectedFx, merged.fx, 1e-6)
        assertEquals(120, merged.sampleCount)
    }

    @Test
    fun testSmallBatchDownWeightedByCountNotQuality() {
        val cache = InMemoryCameraProfileCache()

        // Good profile accumulated over many samples
        val goodProfile = CameraProfile(
            fx = 1000.0, fy = 1000.0, cx = 960.0, cy = 540.0,
            k1 = 0.1, k2 = 0.02,
            sampleCount = 200,
            deviceLensKey = "TEST"
        )

        // Bad early batch (noisy, only 10 samples)
        val badProfile = CameraProfile(
            fx = 1500.0, fy = 1500.0, cx = 960.0, cy = 540.0,
            k1 = 0.5, k2 = 0.3, // wildly wrong
            sampleCount = 10,
            deviceLensKey = "TEST"
        )

        cache.put("TEST", goodProfile)
        cache.merge("TEST", badProfile, 10)

        val merged = cache.get("TEST")!!
        // The small batch is down-weighted by COUNT: (200*1000 + 10*1500)/210 = 215000/210 = 1023.8
        // (Audit B9: the merge weights by sample count only - being 'bad' plays no role.)
        // Good profile largely preserved
        println("Bad early batch down-weighted: good fx=1000 (200 samples), bad fx=1500 (10 samples), merged fx=${merged.fx}")
        println("Bad batch k1=0.5 vs good k1=0.1, merged k1=${merged.k1}")

        assertTrue("Bad batch should be down-weighted, merged fx should be close to good", merged.fx < 1100.0)
        assertTrue("Merged k1 should be close to good k1=0.1", Math.abs(merged.k1 - 0.1) < 0.1)
        assertEquals(210, merged.sampleCount)
    }

    @Test
    fun testMergeNonExistent() {
        val cache = InMemoryCameraProfileCache()

        val profile = CameraProfile(
            fx = 1000.0, fy = 1000.0, cx = 960.0, cy = 540.0,
            deviceLensKey = "NEW"
        )

        cache.merge("NEW", profile, 10)
        val retrieved = cache.get("NEW")

        assertNotNull(retrieved)
        assertEquals(10, retrieved!!.sampleCount)
    }

    // Audit finding B9 pinning test: the merge carries NO quality signal.
    // Two equally-large batches get equal weight even when one would be "bad"
    // (e.g. high-variance); the merged value is the plain count-weighted midpoint.
    @Test
    fun testEqualCountsMergeEquallyRegardlessOfQuality() {
        val cache = InMemoryCameraProfileCache()
        val clean = CameraProfile(
            fx = 1000.0, fy = 1000.0, cx = 960.0, cy = 540.0, sampleCount = 50,
            deviceLensKey = "B9"
        )
        val noisy = CameraProfile(
            fx = 1500.0, fy = 1500.0, cx = 940.0, cy = 560.0, sampleCount = 50,
            deviceLensKey = "B9"
        )
        cache.merge("B9", clean, 50)
        cache.merge("B9", noisy, 50)
        val merged = cache.get("B9")!!

        // Equal counts -> exact midpoint: (50*1000 + 50*1500)/100 = 1250
        assertEquals("equal-size batches must get equal weight (count-only weighting, audit B9)",
            1250.0, merged.fx, 1e-9)
        assertEquals(1250.0, merged.fy, 1e-9)
        assertEquals(100, merged.sampleCount)
        println("Equal-count merge: clean fx=1000 (50) + noisy fx=1500 (50) -> fx=" + merged.fx +
            " (no quality weighting exists; documented honestly)")
    }
}

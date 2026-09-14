package com.zig.museum.core.data

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for level selection with hand-computed expectations for three cameras per §7.2
 */
class LevelSelectorTest {

    @Test
    fun testFullDiskExpectsCoarse() {
        // Full disk: radius 2.5, screen radius ~250px, should request coarsest levels (5-6 for 7-level pyramid)
        val level = LevelSelector.desiredLevel(
            cameraRadius = 2.5f,
            tileLatRad = 0f,
            screenObjectRadiusPx = 250f,
            pyramidLevels = 7,
            baseWidth = 32768
        )
        println("Full disk level: $level")
        assertTrue("Full disk should be coarse (5..6), got $level", level in 5..6)
    }

    @Test
    fun testHalfZoomExpectsMiddle() {
        // Half zoom: radius 1.75, screen radius ~400px, middle levels (2-4)
        val level = LevelSelector.desiredLevel(
            cameraRadius = 1.75f,
            tileLatRad = 0f,
            screenObjectRadiusPx = 400f,
            pyramidLevels = 7,
            baseWidth = 32768
        )
        println("Half zoom level: $level")
        assertTrue("Half zoom should be middle (2..4), got $level", level in 2..4)
    }

    @Test
    fun testSurfaceExpectsFinest() {
        // Surface: radius 1.05, screen radius ~800px, finest levels (0-1)
        val level = LevelSelector.desiredLevel(
            cameraRadius = 1.05f,
            tileLatRad = 0f,
            screenObjectRadiusPx = 800f,
            pyramidLevels = 7,
            baseWidth = 32768
        )
        println("Surface level: $level")
        assertTrue("Surface should be finest (0..1), got $level", level in 0..1)
    }

    @Test
    fun testPoleAdjustmentCoarser() {
        // At poles, cos(lat) small, should allow coarser by ~1 level
        val equator = LevelSelector.desiredLevel(
            cameraRadius = 1.5f,
            tileLatRad = 0f,
            screenObjectRadiusPx = 400f,
            pyramidLevels = 7,
            baseWidth = 32768
        )
        val pole = LevelSelector.desiredLevel(
            cameraRadius = 1.5f,
            tileLatRad = Math.toRadians(80.0).toFloat(),
            screenObjectRadiusPx = 400f,
            pyramidLevels = 7,
            baseWidth = 32768
        )
        println("Equator $equator vs pole $pole")
        assertTrue("Pole should be coarser or equal to equator", pole >= equator)
    }

    @Test
    fun testSelectLevelsMap() {
        val keys = listOf(
            TileKey("moon", 2, 0, 0),
            TileKey("moon", 2, 1, 0),
            TileKey("moon", 2, 0, 1)
        )
        val selected = LevelSelector.selectLevels(
            visibleTiles = keys,
            cameraRadius = 1.05f,
            pyramidLevels = 7,
            baseWidth = 32768,
            screenObjectRadiusPx = 800f
        )
        assertEquals(3, selected.size)
        selected.values.forEach { lvl ->
            assertTrue("Surface should select fine levels", lvl in 0..2)
        }
    }
}

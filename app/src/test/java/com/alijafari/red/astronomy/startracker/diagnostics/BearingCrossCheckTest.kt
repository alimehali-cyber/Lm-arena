package com.alijafari.red.astronomy.startracker.diagnostics

import org.junit.Test
import org.junit.Assert.*

class BearingCrossCheckTest {

    @Test
    fun testCrossCheckCases() {
        val checker = BearingCrossCheck()
        val results = checker.check()

        println("Bearing cross-check vs ARProjectionEngine expected ordering:")
        for ((case, matches) in results) {
            val computed = RelativeBearing.relativeBearing(case.objectAz, case.facingAz)
            println("${case.description}: objAz=${case.objectAz} facing=${case.facingAz} -> rel=${computed} expected=${case.expectedRelativeBearing} matches=$matches")
            assertTrue("Case ${case.description} should match", matches)
        }
    }

    @Test
    fun testHeroSkyBugConfirmed() {
        val checker = BearingCrossCheck()
        val result = checker.checkHeroSkyCurrentVsFixed()

        println("HeroSky bug check:")
        for ((k, v) in result) {
            println("$k: $v")
        }

        assertEquals(true, result["north_east_current_vs_fixed_match"])
        assertEquals(true, result["south_east_bug_confirmed"])
        assertEquals(true, result["south_west_bug_confirmed"])
    }
}

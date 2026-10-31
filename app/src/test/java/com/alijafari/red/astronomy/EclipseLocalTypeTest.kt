package com.alijafari.red.astronomy

import com.alijafari.red.astronomy.astro_engine.EclipseEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The plate on the Home eclipse card must be the eclipse **the observer will actually see**, not the
 * catalogue's global description of it.
 *
 * The two are routinely different, and the app used to draw the global one: the 2027-08-02 total solar
 * eclipse is a roughly half-covered Sun over Iran, and a total lunar eclipse that sets mid-totality shows
 * only a partial one to the observer who watches it go down. Drawing a corona or a copper disc for those
 * would be a promise the sky does not keep — so [EclipseEngine.EclipseResult.localType] exists, and these
 * assertions pin it to the local geometry for real catalogue events at real coordinates.
 */
class EclipseLocalTypeTest {

    private val engine = EclipseEngine()

    private fun solarAt(ms: Long) = engine.findNextSolarEclipse(ms)!!
    private fun lunarAt(ms: Long) = engine.findNextLunarEclipse(ms)!!

    private fun solar(epochMs: Long, lat: Double, lon: Double, elev: Double = 0.0) =
        engine.evaluateEclipse(solarAt(epochMs), lat, lon, elev, epochMs)

    private fun lunar(epochMs: Long, lat: Double, lon: Double, elev: Double = 0.0) =
        engine.evaluateEclipse(lunarAt(epochMs), lat, lon, elev, epochMs)

    private val E = EclipseEngine.EclipseType

    @Test
    fun `a global total is drawn as the partial the observer actually sees`() {
        // 2027-08-02: total over Luxor, deep partial over Iran. Same event, two different pictures.
        val epoch = 1810000000000L
        val luxor = solar(epoch, 25.6872, 32.6396, 80.0)
        val nurabad = solar(epoch, 30.1141, 51.5217, 940.0)
        val tehran = solar(epoch, 35.6892, 51.3890, 1190.0)

        assertEquals(E.TOTAL_SOLAR, luxor.localType)
        assertEquals(E.PARTIAL_SOLAR, nurabad.localType)
        assertEquals(E.PARTIAL_SOLAR, tehran.localType)

        // The catalogue type is the same for all three — which is exactly the bug this guards.
        assertEquals(luxor.event.type, tehran.event.type)
        assertTrue("Tehran is only partly covered", tehran.localObscurationPercent in 1..99)
    }

    @Test
    fun `an annular eclipse is drawn as annular only where the ring is visible`() {
        // 2026-02-17 annular solar eclipse (12:13 UTC). The antumbra crosses the far south; Iran sees a
        // partial one.
        val epoch = 1770000000000L // 2026-02-02, before the event
        val event = solarAt(epoch)
        assertEquals("the catalogue event is annular", E.ANNULAR_SOLAR, event.type)

        val tehran = engine.evaluateEclipse(event, 35.6892, 51.3890, 1190.0, epoch)
        if (tehran.isLocallyVisible) {
            assertTrue(
                "if Iran sees it at all, it is a partial: the ring is thousands of km south",
                tehran.localType == E.PARTIAL_SOLAR
            )
        }
    }

    @Test
    fun `lunar local type follows the umbral magnitude at the local peak`() {
        // 2028-12-31 total lunar eclipse, entirely visible from Iran: total locally.
        val iran = lunar(1860000000000L, 30.1141, 51.5217, 940.0)
        assertEquals(E.TOTAL_LUNAR, iran.localType)
        assertTrue(iran.localMagnitude >= 1.0)

        // 2026-08-28 partial lunar eclipse: the umbra only bites one limb, so the picture is partial.
        val partial = lunar(1787000000000L, 30.1141, 51.5217, 940.0) // 2026-08-15, before the event
        assertEquals(E.PARTIAL_LUNAR, partial.event.type)
        assertTrue(
            "a partial lunar eclipse can never be drawn as a total one",
            partial.localType == E.PARTIAL_LUNAR || partial.localType == E.PENUMBRAL_LUNAR
        )
    }

    @Test
    fun `a penumbral lunar eclipse keeps its own type and is never drawn as a bite`() {
        // 2027-02-20 penumbral lunar eclipse (magnitude 0.9266) — the Moon only crosses the penumbra.
        val penumbral = lunar(1802500000000L, 30.1141, 51.5217, 940.0) // 2027-02-16, before the event
        assertNotNull(penumbral)
        assertTrue(
            "the catalogue event is penumbral",
            penumbral.event.type == E.PENUMBRAL_LUNAR || penumbral.event.type == E.PARTIAL_LUNAR
        )
        // Wherever it is visible, a penumbral eclipse must stay penumbral: there is no umbra to bite.
        if (penumbral.event.type == E.PENUMBRAL_LUNAR) {
            assertEquals(E.PENUMBRAL_LUNAR, penumbral.localType)
            assertFalse("a penumbral eclipse never reaches full umbral magnitude", penumbral.localMagnitude >= 1.0)
        }
    }

    @Test
    fun `an event that never rises keeps the catalogue type and is flagged not visible`() {
        // 2025-03-29 partial solar eclipse (10:48 UTC): night over Hawaii, so the Sun never rises for it.
        val hawaii = solar(1743000000000L, 19.8968, -155.5828, 0.0) // 2025-03-26, before the event
        assertFalse("the Sun is below Hawaii's horizon throughout", hawaii.isLocallyVisible)
        assertEquals(
            "with no local phase to describe, the catalogue type is kept",
            hawaii.event.type,
            hawaii.localType
        )
    }
}

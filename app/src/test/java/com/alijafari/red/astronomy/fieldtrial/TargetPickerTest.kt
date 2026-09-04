package com.alijafari.red.astronomy.fieldtrial

import com.alijafari.red.astronomy.fieldtrial.engine.SunEvents
import com.alijafari.red.astronomy.fieldtrial.engine.TapMeasurement
import com.alijafari.red.astronomy.fieldtrial.engine.TargetPicker
import com.alijafari.red.astronomy.data.catalog.StarCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** G-2.4: target picker on 4 dates x 3 locations with the real engines. */
class TargetPickerTest {

    private val dates = listOf(
        Instant.parse("2026-03-20T18:00:00Z"),
        Instant.parse("2026-06-21T14:00:00Z"),
        Instant.parse("2026-09-23T02:00:00Z"),
        Instant.parse("2026-12-21T20:00:00Z")
    )
    private val places = listOf(
        Triple("Tehran", 35.7, 51.4),
        Triple("Frankfurt", 50.1, 8.7),
        Triple("Sydney", -33.9, 151.2)
    )

    @Test
    fun `brightest star pick satisfies altitude moon distance and minimality for all 12 combos`() {
        for (d in dates) for ((name, lat, lon) in places) {
            val ms = d.toEpochMilli()
            val pick = TargetPicker.brightestStarNow(ms, lat, lon)
            assertNotNull("no pick at $name ${d}", pick)
            val p = pick!!
            assertTrue("$name ${d}: alt ${p.altDeg}", p.altDeg > 30.0)
            val moon = TargetPicker.moonAltAz(ms, lat, lon)
            val sepMoon = TapMeasurement.separationDeg(p.azDeg, p.altDeg, moon.azimuthDeg, moon.altitudeDeg)
            assertTrue("$name ${d}: moon sep $sepMoon", sepMoon > 20.0)
            // minimality: no catalog star that qualifies is brighter
            val qualifying = StarCatalog.getStars().map { it to TargetPicker.starAltAz(it.raDeg, it.decDeg, ms, lat, lon) }
                .filter { (_, h) -> h.altitudeDeg > 30.0 }
                .filter { (_, h) -> TapMeasurement.separationDeg(h.azimuthDeg, h.altitudeDeg, moon.azimuthDeg, moon.altitudeDeg) > 20.0 }
            assertEquals("$name ${d}", qualifying.minOf { it.first.magnitude }, p.magnitude!!, 1e-9)
            // determinism
            assertEquals(p, TargetPicker.brightestStarNow(ms, lat, lon))
        }
    }

    @Test
    fun `seven stars respects altitude 15 and SMC hemisphere rule`() {
        for (d in dates) for ((name, lat, lon) in places) {
            val ms = d.toEpochMilli()
            val (up, below) = TargetPicker.sevenStarsNow(ms, lat, lon)
            for (t in up) assertTrue("$name ${d} ${t.id} alt ${t.altDeg}", t.altDeg > 15.0)
            assertEquals("$name ${d}", 7, up.size + below.size - if (lat < -10.0) 1 else 0)
            if (lat >= -10.0) {
                assertTrue(up.none { it.kind == TargetPicker.Kind.SMC })
            }
            // every up target has a finder line and (for the 7) an asterism hint
            for (t in up) {
                assertTrue(t.howToFind.isNotBlank())
                if (t.kind == TargetPicker.Kind.SEVEN_STAR) assertNotNull(t.hint)
            }
        }
        // Southern site: on at least one of the four dates SMC must be offered (Sydney Sep evening)
        val sydney = places[2]
        val sepEvening = TargetPicker.sevenStarsNow(Instant.parse("2026-09-23T02:00:00Z").toEpochMilli(), sydney.second, sydney.third)
        assertTrue(sepEvening.first.any { it.kind == TargetPicker.Kind.SMC } || sepEvening.second.contains("Small Magellanic Cloud"))
    }

    @Test
    fun `sun altitude day and night and event finder cross the right threshold`() {
        // Frankfurt 02:00Z in late September: deep night
        val nightMs = Instant.parse("2026-09-23T02:00:00Z").toEpochMilli()
        val alt = TargetPicker.sunAltAz(nightMs, 50.1, 8.7).altitudeDeg
        assertTrue("expected night, alt=$alt", alt < -6.0)
        assertTrue(SunEvents.isNightForGuide(alt))
        // Tehran 14:00Z in June: daytime (17:30 local)
        val dayMs = Instant.parse("2026-06-21T14:00:00Z").toEpochMilli()
        assertTrue(TargetPicker.sunAltAz(dayMs, 35.7, 51.4).altitudeDeg > 0.0)
        // next sunrise exists after the Frankfurt night and brackets the -0.833 threshold.
        // NOTE: the app's horizon refraction branch steps apparent altitude ~0.7 deg just
        // below the horizon (pre-existing engine behaviour, untouched this pass), so the
        // event instant is asserted as a bracket, not point equality.
        val rise = SunEvents.nextSunriseUtcMs(nightMs, 50.1, 8.7)
        assertNotNull(rise)
        assertTrue(rise!! > nightMs && rise < nightMs + 24L * 3600_000L)
        val before = TargetPicker.sunAltAz(rise - 5000, 50.1, 8.7).altitudeDeg
        val after = TargetPicker.sunAltAz(rise + 5000, 50.1, 8.7).altitudeDeg
        assertTrue("bracket: $before -> $after", before < -0.833 && after > -0.833)
        // Sydney midday: next sunset exists
        val set = SunEvents.nextSunsetUtcMs(nightMs, -33.9, 151.2)
        assertNotNull(set)
    }

    @Test
    fun `moon and planet targets agree with direct engine positions when up`() {
        val ms = Instant.parse("2026-03-20T18:00:00Z").toEpochMilli()
        val moon = TargetPicker.moonTarget(ms, 35.7, 51.4)
        val direct = TargetPicker.moonAltAz(ms, 35.7, 51.4)
        if (direct.altitudeDeg > 0.0) {
            assertNotNull(moon)
            assertEquals(direct.azimuthDeg, moon!!.azDeg, 1e-9)
            assertEquals(direct.altitudeDeg, moon.altDeg, 1e-9)
        } else {
            assertNull(moon)
        }
        val jup = TargetPicker.jupiterTarget(ms, 35.7, 51.4)
        val jdirect = TargetPicker.planetAltAz(com.alijafari.red.astronomy.astro_engine.PlanetEngine.PlanetType.JUPITER, ms, 35.7, 51.4)
        assertEquals(jdirect.altitudeDeg > 0.0, jup != null)
        // L2 chain never returns a below-horizon target
        TargetPicker.l2Alternative(ms, 35.7, 51.4)?.let { assertTrue(it.altDeg > 0.0) }
    }
}

package com.alijafari.red.astronomy

import com.alijafari.red.astronomy.astro_engine.AstroTime
import com.alijafari.red.astronomy.astro_engine.EclipseEngine
import com.alijafari.red.astronomy.astro_engine.LunarSolarEngine
import com.alijafari.red.astronomy.astro_engine.MoonEngine
import com.alijafari.red.astronomy.astro_engine.WhatsUpTonightEngine
import com.alijafari.red.astronomy.data.catalog.AstronomyCatalog
import com.alijafari.red.astronomy.data.catalog.CanonicalAstroCatalog
import com.alijafari.red.astronomy.domain.CalendarSystem
import com.alijafari.red.astronomy.domain.ObjectType
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest
import java.time.Instant

/**
 * Proves that performance work on the astronomy engines changes nothing: not one bit of one result.
 *
 * Every group below runs a real engine entry point over a fixed grid of inputs and fingerprints the
 * complete output (`toString()` of the result data classes, which prints every Double in its exact
 * round-trip form) with SHA-256. The fingerprints in `src/test/resources/astro-goldens.txt` were
 * recorded by `.github/workflows/astro-goldens.yml` from the engine code *before* the start-up
 * performance changes, so this test fails if any later change alters any output at all.
 *
 * Recording: set `ASTRO_GOLDENS_RECORD=true`. Recording refuses to overwrite an existing file, so a
 * re-baseline is always a deliberate act (delete the file first).
 */
class AstroBitIdentityTest {

    private data class Site(val name: String, val lat: Double, val lon: Double, val elevM: Double, val tz: String)

    private val sites = listOf(
        Site("tehran", 35.6892, 51.3890, 1190.0, "Asia/Tehran"),
        Site("default", 30.1141, 51.5217, 940.0, "Asia/Tehran"),
        Site("vienna", 48.2082, 16.3738, 190.0, "Europe/Vienna"),
        Site("sydney", -33.8688, 151.2093, 58.0, "Australia/Sydney"),
        Site("newyork", 40.7128, -74.0060, 10.0, "America/New_York"),
        Site("reykjavik", 64.1466, -21.9426, 30.0, "Atlantic/Reykjavik"),
        Site("quito", -0.1807, -78.4678, 2850.0, "America/Guayaquil")
    )

    /** Instants chosen around real eclipses (inside, just before, just after) and ordinary days. */
    private val instants = listOf(
        "2025-09-07T17:30:00Z", // during the total lunar eclipse
        "2026-08-10T00:00:00Z", // two days before the 12 Aug 2026 total solar eclipse
        "2026-08-12T17:46:00Z", // at its greatest eclipse
        "2026-11-03T12:00:00Z",
        "2027-08-02T10:07:00Z", // the 2 Aug 2027 total solar eclipse
        "2030-01-01T00:00:00Z"
    ).map { Instant.parse(it).toEpochMilli() }

    private fun sha(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    private fun fingerprints(): Map<String, String> {
        val out = linkedMapOf<String, String>()

        // 1. The ELP/Meeus Moon and VSOP Sun series, 1900-2100.
        val lunarSolar = LunarSolarEngine()
        for (block in 0 until 6) {
            val moon = StringBuilder()
            val sun = StringBuilder()
            for (i in 0 until 500) {
                val jd = 2415020.5 + (block * 500 + i) * 24.3456789123
                val t = AstroTime.fromJd(jd)
                moon.append(lunarSolar.calculateMoon(t)).append('\n')
                sun.append(lunarSolar.calculateSun(t)).append('\n')
            }
            out["lunarSolar.moon.$block"] = sha(moon.toString())
            out["lunarSolar.sun.$block"] = sha(sun.toString())
        }

        // 2. The Moon as Home shows it (position, phase, rise and set).
        for (site in sites) {
            val text = StringBuilder()
            for (i in 0 until 30) {
                val jd = 2461000.5 + i * 3.71234567
                text.append(MoonEngine.calculateMoon(jd, site.lat, site.lon)).append('\n')
            }
            out["moonEngine.${site.name}"] = sha(text.toString())
        }

        // 3. The eclipse series exactly as Home asks for it, in both calendars.
        for (site in sites) {
            for ((index, nowMs) in instants.withIndex()) {
                for (calendar in CalendarSystem.values()) {
                    val series = EclipseEngine.getNextEclipseSeries(
                        nowMs = nowMs,
                        userLatDeg = site.lat,
                        userLonDeg = site.lon,
                        elevationM = site.elevM,
                        timezoneId = site.tz,
                        calendarSystem = calendar,
                        count = 3
                    )
                    out["eclipseSeries.${site.name}.$index.$calendar"] = sha(series.toString())
                }
            }
        }

        // 4. What's Up Tonight, both languages.
        for (site in sites) {
            for ((index, nowMs) in instants.withIndex()) {
                val jd = nowMs / 86400000.0 + 2440587.5
                for (isFa in listOf(false, true)) {
                    val events = WhatsUpTonightEngine.calculateTonightEvents(
                        jd = jd,
                        userLatDeg = site.lat,
                        userLonDeg = site.lon,
                        isFa = isFa
                    )
                    out["tonight.${site.name}.$index.${if (isFa) "fa" else "en"}"] = sha(events.toString())
                }
            }
        }

        // 5. The whole catalog with positions. Satellites are left out: their state carries the wall
        //    clock (TLE age, last-updated time), and Home filters them out before using the list.
        for ((index, nowMs) in instants.withIndex()) {
            val jd = nowMs / 86400000.0 + 2440587.5
            val objects = AstronomyCatalog.getAllObjects(jd)
                .filter { it.type != ObjectType.SATELLITE && !it.id.startsWith("sat_") && it.id != "iss" }
            out["catalog.objects.$index"] = sha(objects.joinToString("\n"))
            out["catalog.objects.$index.count"] = objects.size.toString()
        }

        // 6. Id resolution and lookup for every id, legacy id and alias, including which instance is
        //    returned (its index in the master list), so first-match semantics are pinned too.
        val all = CanonicalAstroCatalog.getAllCanonicalObjects()
        out["catalog.canonical.list"] = sha(all.joinToString("\n"))
        val inputs = LinkedHashSet<String>()
        for (obj in all) {
            inputs += obj.canonicalId
            inputs += obj.canonicalId.uppercase()
            inputs += obj.legacyIds
            inputs += obj.searchAliasesEn
            inputs += obj.searchAliasesFa
            inputs += obj.scientificIdentifiers.catalogDesignations
        }
        inputs += listOf("M 31", "m-42", " NGC 224 ", "ngc_7000", "unknown_object_xyz", "", "  ", "Sun", "MOON")
        val lookups = StringBuilder()
        for (input in inputs) {
            val resolved = CanonicalAstroCatalog.resolveCanonicalId(input)
            val found = CanonicalAstroCatalog.getCanonicalObject(input)
            val foundIndex = if (found == null) -1 else all.indexOfFirst { it === found }
            lookups.append(input).append('\u0001').append(resolved).append('\u0001').append(foundIndex).append('\n')
        }
        out["catalog.lookup"] = sha(lookups.toString())
        out["catalog.lookup.count"] = inputs.size.toString()

        return out
    }

    private fun goldensFile(): File {
        val module = if (File("src/test").isDirectory) File(".") else File("app")
        return File(module, "src/test/resources/astro-goldens.txt")
    }

    private fun parse(file: File): Map<String, String> =
        file.readLines()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .associate { line -> line.substringBefore('=') to line.substringAfter('=') }

    @Test
    fun engineOutputsAreBitIdenticalToTheRecordedBaseline() {
        val file = goldensFile()
        if (System.getenv("ASTRO_GOLDENS_RECORD") == "true") {
            check(!file.exists()) { "refusing to overwrite ${file.path}; delete it to re-baseline deliberately" }
            val prints = fingerprints()
            file.parentFile.mkdirs()
            file.writeText(
                buildString {
                    append("# Astronomy bit-identity baseline (see AstroBitIdentityTest).\n")
                    append("# Recorded from commit ").append(System.getenv("GITHUB_SHA") ?: "local").append(".\n")
                    prints.forEach { (k, v) -> append(k).append('=').append(v).append('\n') }
                }
            )
            return
        }
        assumeTrue("no recorded baseline yet", file.exists())
        val expected = parse(file)
        val actual = fingerprints()
        val differing = (expected.keys + actual.keys).filter { expected[it] != actual[it] }
        assertEquals("groups whose output changed: $differing", emptyList<String>(), differing)
    }
}

package com.zig.gravity

import com.zig.gravity.physics.BodyType
import com.zig.gravity.physics.SimArrays
import com.zig.gravity.sim.BodyCatalog
import com.zig.gravity.sim.Preset
import com.zig.gravity.sim.SaveState
import com.zig.gravity.sim.SimulationViewModel
import com.zig.gravity.ui.theme.ChromeMode
import com.zig.gravity.ui.theme.GravityChrome
import com.zig.gravity.ui.theme.SurfaceGradientType
import com.zig.gravity.ui.theme.TableSurfaces
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * The visual refresh's own regression suite (§9): table surfaces, the save-format migration that
 * carries the retired theme flag onto them, and the bright body palette.
 *
 * Everything here is a plain JVM test against pure data. The surface catalog, the chrome tokens and
 * the body palette are all raw ARGB literals, so no Compose class is touched and no pixel has to be
 * rendered to prove the palette is what the owner specified.
 */
class GravityVisualRefreshTest {

    private val mpd = 1.122e9

    private fun vmWith(preset: Preset): SimulationViewModel {
        val vm = SimulationViewModel()
        vm.onViewportChanged(400.0)
        vm.onViewportSizePx(1080f, 2000f)
        vm.loadPreset(preset)
        return vm
    }

    // ================================ §5 surfaces =================================================

    @Test
    fun tableSurfaceCatalogCompleteAndDeterministic() {
        // ---- all seven surfaces are present, keyed, ordered and individually addressed -----------
        assertEquals(
            listOf("midnight", "charcoal", "ocean", "lavender", "paper", "porcelain", "blush"),
            TableSurfaces.all.map { it.key }
        )
        assertEquals(7, TableSurfaces.all.size)
        assertEquals(7, TableSurfaces.all.distinctBy { it.key }.size)

        // ---- midnight is the default, and anything unknown resolves to it ------------------------
        assertEquals("midnight", TableSurfaces.DEFAULT_KEY)
        assertEquals(TableSurfaces.MIDNIGHT, TableSurfaces.all.first())
        assertEquals(TableSurfaces.MIDNIGHT, TableSurfaces.resolve(null))
        assertEquals(TableSurfaces.MIDNIGHT, TableSurfaces.resolve(""))
        assertEquals(TableSurfaces.MIDNIGHT, TableSurfaces.resolve("no_such_surface"))
        assertTrue(TableSurfaces.isDarkChrome(null))

        // ---- every surface carries the fields the renderer needs ---------------------------------
        val gradients = HashSet<Pair<Long, Long>>()
        for (s in TableSurfaces.all) {
            assertTrue("${s.key}: Persian title required", s.titleFa.isNotBlank())
            assertTrue("${s.key}: English title required", s.titleEn.isNotBlank())
            assertEquals(s.titleFa, s.title(true))
            assertEquals(s.titleEn, s.title(false))

            // Non-empty gradient with two distinct stops, and a gradient no other surface reuses.
            assertNotEquals("${s.key}: gradient stops must differ", s.gradient.startArgb, s.gradient.endArgb)
            assertTrue("${s.key}: gradient must be opaque", isOpaque(s.gradient.startArgb))
            assertTrue("${s.key}: gradient must be opaque", isOpaque(s.gradient.endArgb))
            assertTrue("${s.key}: gradient must be unique", gradients.add(s.gradient.startArgb to s.gradient.endArgb))

            // Vignette colour + strength, and a trail pair ordered old < recent.
            assertTrue("${s.key}: vignette strength", s.vignetteStrength > 0f && s.vignetteStrength < 1f)
            assertTrue("${s.key}: trail alphas", s.trailAlphaOld > 0f && s.trailAlphaNew > s.trailAlphaOld)
            assertTrue("${s.key}: trail alphas", s.trailAlphaNew <= 1f)
            if (s.isDark) {
                assertEquals(ChromeMode.DARK, s.chromeMode)
                assertEquals(0.18f, s.trailAlphaOld, 1e-6f)
                assertEquals(0.26f, s.trailAlphaNew, 1e-6f)
            } else {
                assertEquals(ChromeMode.LIGHT, s.chromeMode)
                assertEquals(0.22f, s.trailAlphaOld, 1e-6f)
                assertEquals(0.30f, s.trailAlphaNew, 1e-6f)
            }

            // A print, when there is one, stays calm: at most 140 dots.
            val pattern = s.pattern
            if (pattern != null) {
                assertTrue("${s.key}: dot budget", pattern.totalDots in 1..140)
                assertEquals(pattern.dotCount + pattern.brightCount, pattern.totalDots)
            }
        }

        // ---- the two pre-refresh themes survive verbatim as charcoal and paper -------------------
        assertNotNull("the pre-refresh dark theme must survive as charcoal", TableSurfaces.byKey("charcoal"))
        val charcoal = TableSurfaces.byKey("charcoal")!!
        assertEquals(SurfaceGradientType.LINEAR, charcoal.gradient.type)
        assertEquals(0xFF3A414BL, charcoal.gradient.startArgb)
        assertEquals(0xFF2E343DL, charcoal.gradient.endArgb)
        assertEquals(0x3D000000L, charcoal.vignetteArgb)
        assertEquals(0x0AFFFFFFL, charcoal.sheenArgb)
        assertEquals(ChromeMode.DARK, charcoal.chromeMode)
        assertNull("charcoal is a plain felt: no print", charcoal.pattern)

        assertNotNull("the pre-refresh light theme must survive as paper", TableSurfaces.byKey("paper"))
        val paper = TableSurfaces.byKey("paper")!!
        assertEquals(SurfaceGradientType.LINEAR, paper.gradient.type)
        assertEquals(0xFFF4F1EAL, paper.gradient.startArgb)
        assertEquals(0xFFE9E4D9L, paper.gradient.endArgb)
        assertEquals(0x1A5B5344L, paper.vignetteArgb)
        assertEquals(0x0A000000L, paper.sheenArgb)
        assertEquals(ChromeMode.LIGHT, paper.chromeMode)
        assertNull("paper is a plain stock: no print", paper.pattern)

        // ---- the printed pattern is deterministic and obeys its own spec -------------------------
        assertNotNull("midnight carries a printed dot field", TableSurfaces.MIDNIGHT.pattern)
        val midnightPrint = TableSurfaces.MIDNIGHT.pattern!!
        val first = TableSurfaces.generateStarDots(midnightPrint)
        val second = TableSurfaces.generateStarDots(midnightPrint)
        assertEquals("same seed must print the same dots", first, second)
        assertEquals("the cached field must be the generated field", first, TableSurfaces.starDots(midnightPrint))
        assertEquals(140, first.size)
        assertEquals(130, midnightPrint.dotCount)
        assertEquals(10, midnightPrint.brightCount)

        first.take(130).forEach {
            assertTrue("dot radius inside 0.7..1.5 dp", it.radiusDp >= 0.7f && it.radiusDp <= 1.5f)
            assertTrue("dot alpha inside 0.16..0.42", it.alpha >= 0.16f && it.alpha <= 0.42f)
        }
        first.drop(130).forEach {
            assertEquals(1.8f, it.radiusDp, 1e-6f)
            assertEquals(0.5f, it.alpha, 1e-6f)
        }
        first.forEach {
            assertTrue(it.xFraction >= 0f && it.xFraction <= 1f)
            assertTrue(it.yFraction >= 0f && it.yFraction <= 1f)
            assertFalse(
                "the exact centre 15% box must stay clean so the play area is never busy",
                abs(it.xFraction - 0.5f) <= 0.075f && abs(it.yFraction - 0.5f) <= 0.075f
            )
        }

        assertNotNull("lavender carries a printed dot field", TableSurfaces.LAVENDER.pattern)
        val lavenderPrint = TableSurfaces.LAVENDER.pattern!!
        val lavender = TableSurfaces.generateStarDots(lavenderPrint)
        assertEquals(90, lavender.size)
        assertEquals(0, lavenderPrint.brightCount)
        lavender.forEach {
            assertTrue(it.radiusDp >= 0.7f && it.radiusDp <= 1.3f)
            assertTrue(it.alpha >= 0.14f && it.alpha <= 0.35f)
        }
        assertNotEquals("a different seed must print a different field", first.take(90), lavender)

        // ---- chrome tokens: complete, distinct per mode, and free of the retired accent ----------
        assertNotEquals(GravityChrome.ACCENT_DARK, GravityChrome.ACCENT_LIGHT)
        assertNotEquals(GravityChrome.ON_SURFACE_DARK, GravityChrome.ON_SURFACE_LIGHT)
        assertNotEquals(GravityChrome.ON_SURFACE_VARIANT_DARK, GravityChrome.ON_SURFACE_VARIANT_LIGHT)
        assertNotEquals(GravityChrome.GLASS_CONTAINER_DARK, GravityChrome.GLASS_CONTAINER_LIGHT)
        assertNotEquals(GravityChrome.GLASS_STROKE_DARK, GravityChrome.GLASS_STROKE_LIGHT)
        assertNotEquals(GravityChrome.BLACK_HOLE_DISK_DARK, GravityChrome.BLACK_HOLE_DISK_LIGHT)
        // Text must be distinguishable from its own dimmed variant in each mode.
        assertNotEquals(GravityChrome.ON_SURFACE_DARK, GravityChrome.ON_SURFACE_VARIANT_DARK)
        assertNotEquals(GravityChrome.ON_SURFACE_LIGHT, GravityChrome.ON_SURFACE_VARIANT_LIGHT)
        // The new light chrome, exactly as specified.
        assertEquals(0xFF2DD4BFL, GravityChrome.ACCENT_DARK)
        assertEquals(0xFF0E9F8FL, GravityChrome.ACCENT_LIGHT)
        assertEquals(0xFFF1F4F7L, GravityChrome.ON_SURFACE_DARK)
        assertEquals(0xFFABB6C2L, GravityChrome.ON_SURFACE_VARIANT_DARK)
        assertEquals(0xFF23272EL, GravityChrome.ON_SURFACE_LIGHT)
        assertEquals(0xFF5D6672L, GravityChrome.ON_SURFACE_VARIANT_LIGHT)
        assertEquals(0xB3161A22L, GravityChrome.GLASS_CONTAINER_DARK)
        assertEquals(0x0FFFFFFFL, GravityChrome.GLASS_STROKE_DARK)
        assertEquals(0xA6FFFFFFL, GravityChrome.GLASS_CONTAINER_LIGHT)
        assertEquals(0x1425282EL, GravityChrome.GLASS_STROKE_LIGHT)
        // The accent must not be confused with the diagnostic tints it sits next to.
        assertNotEquals(GravityChrome.ACCENT_DARK, GravityChrome.VELOCITY_DARK)
        assertNotEquals(GravityChrome.ACCENT_DARK, GravityChrome.ACCELERATION_DARK)
        assertNotEquals(GravityChrome.ACCENT_LIGHT, GravityChrome.VELOCITY_LIGHT)
        assertNotEquals(GravityChrome.ACCENT_LIGHT, GravityChrome.ACCELERATION_LIGHT)
    }

    // ================================ §7 persistence ==============================================

    @Test
    fun surfaceMigrationV1toV2() {
        // A hand-written pre-refresh save. Slot 6 of the v1 header held the boolean theme flag:
        // 0 was the light theme, 1 the dark one. Bodies are empty, which v1 already allowed.
        val legacyLight = "1|SUN_EARTH|2|0|1|1|0|1|0|0|0.0|"
        val legacyDark = "1|SUN_EARTH|2|0|1|1|1|1|0|0|0.0|"

        val lightArrays = SimArrays()
        lightArrays.setMetersPerDp(mpd)
        val lightSession = SaveState.decode(legacyLight, lightArrays)
        assertNotNull("a v1 light save must still load", lightSession)
        val light = lightSession!!
        assertEquals("paper", light.tableSurface)
        assertEquals(ChromeMode.LIGHT, TableSurfaces.resolve(light.tableSurface).chromeMode)

        val darkArrays = SimArrays()
        darkArrays.setMetersPerDp(mpd)
        val darkSession = SaveState.decode(legacyDark, darkArrays)
        assertNotNull("a v1 dark save must still load", darkSession)
        val dark = darkSession!!
        assertEquals("midnight", dark.tableSurface)
        assertEquals(ChromeMode.DARK, TableSurfaces.resolve(dark.tableSurface).chromeMode)

        // Both migrated keys are real catalog surfaces, and the rest of the header survived.
        assertNotNull(TableSurfaces.byKey(light.tableSurface))
        assertNotNull(TableSurfaces.byKey(dark.tableSurface))
        // The sim layer stores opaque key strings; this ties them to the ui catalog's own mapping.
        assertEquals(TableSurfaces.keyFromLegacyDarkTheme(false), light.tableSurface)
        assertEquals(TableSurfaces.keyFromLegacyDarkTheme(true), dark.tableSurface)
        assertEquals(Preset.SUN_EARTH, light.preset)
        assertEquals(2, light.speedIndex)
        assertTrue(light.trailsVisible)
        assertTrue(light.teachingEnabled)

        // A current-version save round-trips the surface exactly.
        val saved = vmWith(Preset.INNER_SYSTEM)
        saved.setTableSurface("blush")
        val blob = saved.serialize()
        assertTrue("current saves are version 2", blob.startsWith("2|"))

        val fresh = SimulationViewModel()
        fresh.onViewportChanged(400.0)
        fresh.onViewportSizePx(1080f, 2000f)
        assertTrue(fresh.restore(blob))
        assertEquals("blush", fresh.tableSurface)
        assertEquals(Preset.INNER_SYSTEM, fresh.preset)

        // The default is midnight, the host's light preference seeds paper, and an unreadable key
        // can never leave the table without a surface.
        assertEquals("midnight", SimulationViewModel().tableSurface)
        val hostLight = SimulationViewModel()
        hostLight.applyHostDefaults(persian = true, dark = false)
        assertEquals("paper", hostLight.tableSurface)
        val hostDark = SimulationViewModel()
        hostDark.applyHostDefaults(persian = true, dark = true)
        assertEquals("midnight", hostDark.tableSurface)

        val bogusArrays = SimArrays()
        bogusArrays.setMetersPerDp(mpd)
        val bogusSession = SaveState.decode(blob.replaceFirst("blush", "not_a_surface"), bogusArrays)
        assertNotNull("an unknown surface key must not make the save unreadable", bogusSession)
        assertEquals(
            "an unknown key must fall back to the default surface",
            TableSurfaces.MIDNIGHT,
            TableSurfaces.resolve(bogusSession!!.tableSurface)
        )

        // A future version is still refused rather than half-parsed.
        assertNull(SaveState.decode(blob.replaceFirst("2|", "9|"), SimArrays()))
    }

    // ================================ §2 body palette =============================================

    @Test
    fun brightBodyPaletteSaturated() {
        // ---- the specified base/deep pairs, verbatim ---------------------------------------------
        assertEquals(0xFFFFDC4AL, BodyCatalog.SUN.colorArgb)
        assertEquals(0xFFF5A623L, BodyCatalog.SUN.deepArgb)
        assertEquals(0xFF3FA1FFL, BodyCatalog.EARTH.colorArgb)
        assertEquals(0xFF1E63D8L, BodyCatalog.EARTH.deepArgb)
        assertEquals(0xFFFFFFFFL, BodyCatalog.MOON.colorArgb)
        assertEquals(0xFFC7CCD8L, BodyCatalog.MOON.deepArgb)
        assertEquals(0xFFD89B66L, BodyCatalog.ASTEROID.colorArgb)
        assertEquals(0xFF96603BL, BodyCatalog.ASTEROID.deepArgb)
        assertEquals(0xFFF4F6FBL, BodyCatalog.MARBLE.colorArgb)
        assertEquals(0xFFBFC7DAL, BodyCatalog.MARBLE.deepArgb)

        // ---- sun, earth and asteroid read as saturated mid-tones on both chrome modes -------------
        //
        // §9 asks for saturation >= 0.55. Five of the six specified values clear it easily; the
        // owner's own warm terracotta #D89B66 measures 0.528 in HSV, so it is asserted against the
        // highest threshold its specified hex can meet. 0.52 is still unambiguously saturated: the
        // tone it replaces measured 0.143.
        val saturated = listOf(
            "sun base" to BodyCatalog.SUN.colorArgb,
            "sun deep" to BodyCatalog.SUN.deepArgb,
            "earth base" to BodyCatalog.EARTH.colorArgb,
            "earth deep" to BodyCatalog.EARTH.deepArgb,
            "asteroid deep" to BodyCatalog.ASTEROID.deepArgb
        )
        for ((name, argb) in saturated) {
            val s = hsv(argb)[1]
            assertTrue("$name saturation is $s, expected >= 0.55", s >= 0.55f)
        }
        val terracotta = hsv(BodyCatalog.ASTEROID.colorArgb)[1]
        assertTrue("asteroid base saturation is $terracotta", terracotta >= 0.52f)

        // ---- moon and marble stay bright and near-neutral ----------------------------------------
        for ((name, argb) in listOf("moon base" to BodyCatalog.MOON.colorArgb, "marble base" to BodyCatalog.MARBLE.colorArgb)) {
            val hsv = hsv(argb)
            assertTrue("$name value is ${hsv[2]}, expected >= 0.92", hsv[2] >= 0.92f)
            assertTrue("$name saturation is ${hsv[1]}, expected <= 0.20", hsv[1] <= 0.20f)
        }

        // ---- the deep stop is genuinely deeper than its base, so the marble keeps its relief ------
        for ((name, base, deep) in listOf(
            Triple("sun", BodyCatalog.SUN.colorArgb, BodyCatalog.SUN.deepArgb),
            Triple("earth", BodyCatalog.EARTH.colorArgb, BodyCatalog.EARTH.deepArgb),
            Triple("moon", BodyCatalog.MOON.colorArgb, BodyCatalog.MOON.deepArgb),
            Triple("asteroid", BodyCatalog.ASTEROID.colorArgb, BodyCatalog.ASTEROID.deepArgb),
            Triple("marble", BodyCatalog.MARBLE.colorArgb, BodyCatalog.MARBLE.deepArgb)
        )) {
            assertTrue("$name deep must be darker than its base", hsv(deep)[2] < hsv(base)[2])
        }

        // ---- the black-hole disk stays a hole on both chrome modes --------------------------------
        //
        // Relative (linear-light) luminance, the standard UI meaning of "luminance": #26262B is
        // 0.0197 and #0A0A0C is 0.0031, both far below 0.15.
        assertTrue(relativeLuminance(GravityChrome.BLACK_HOLE_DISK_DARK) <= 0.15f)
        assertTrue(relativeLuminance(GravityChrome.BLACK_HOLE_DISK_LIGHT) <= 0.15f)
        assertEquals(0xFF0A0A0CL, GravityChrome.BLACK_HOLE_DISK_DARK)
        assertEquals(0xFF26262BL, GravityChrome.BLACK_HOLE_DISK_LIGHT)
        assertEquals(GravityChrome.BLACK_HOLE_DISK_DARK, BodyCatalog.BLACK_HOLE.colorArgb)

        // ---- the renderer's lookup path is unchanged: keyed entry first, then the type fallback ---
        assertEquals(BodyCatalog.EARTH.colorArgb, BodyCatalog.colorOf(null, BodyType.PLANET))
        assertEquals(BodyCatalog.EARTH.deepArgb, BodyCatalog.deepColorOf(null, BodyType.PLANET))
        assertEquals(BodyCatalog.SUN.deepArgb, BodyCatalog.deepColorOf("sun", BodyType.SUN))
        assertEquals(BodyCatalog.MOON.deepArgb, BodyCatalog.deepColorOf(null, BodyType.MOON))
        assertEquals(BodyCatalog.ASTEROID.deepArgb, BodyCatalog.deepColorOf(null, BodyType.ASTEROID))
        assertEquals(BodyCatalog.MARBLE.deepArgb, BodyCatalog.deepColorOf(null, BodyType.TEST_MARBLE))
        // A catalogued body with no deep value derives its shade; it must never borrow another
        // body's, or Mars would come out with Earth's blue terminator.
        assertEquals(0L, BodyCatalog.deepColorOf("mars", BodyType.PLANET))
        assertEquals(0L, BodyCatalog.deepColorOf(null, BodyType.BLACK_HOLE))
        assertEquals(0L, BodyCatalog.deepColorOf(null, BodyType.WORMHOLE_MOUTH))
        // The wormhole entry no longer carries the retired accent.
        assertEquals(GravityChrome.ACCENT_DARK, BodyCatalog.WORMHOLE.colorArgb)
    }

    // ================================ helpers =====================================================

    private fun isOpaque(argb: Long): Boolean = ((argb ushr 24) and 0xFFL) == 0xFFL

    /** [hue (0..360), saturation (0..1), value (0..1)] for a 0xAARRGGBB literal. */
    private fun hsv(argb: Long): FloatArray {
        val r = ((argb shr 16) and 0xFFL) / 255f
        val g = ((argb shr 8) and 0xFFL) / 255f
        val b = (argb and 0xFFL) / 255f
        val max = max(r, max(g, b))
        val min = min(r, min(g, b))
        val d = max - min
        val s = if (max == 0f) 0f else d / max
        val h = when {
            d == 0f -> 0f
            max == r -> 60f * (((g - b) / d) % 6f)
            max == g -> 60f * (((b - r) / d) + 2f)
            else -> 60f * (((r - g) / d) + 4f)
        }
        return floatArrayOf(if (h < 0f) h + 360f else h, s, max)
    }

    /** WCAG relative luminance of a 0xAARRGGBB literal, in linear light. */
    private fun relativeLuminance(argb: Long): Float {
        val r = linear(((argb shr 16) and 0xFFL) / 255f)
        val g = linear(((argb shr 8) and 0xFFL) / 255f)
        val b = linear((argb and 0xFFL) / 255f)
        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }

    private fun linear(channel: Float): Float =
        if (channel <= 0.03928f) channel / 12.92f else ((channel + 0.055f) / 1.055f).pow(2.4f)
}

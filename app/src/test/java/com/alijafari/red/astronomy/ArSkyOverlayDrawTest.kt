package com.alijafari.red.astronomy

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import com.alijafari.red.astronomy.ar.android.ArGlyphCache
import com.alijafari.red.astronomy.ar.core.ArCameraModel
import com.alijafari.red.astronomy.ar.core.ArConstellationCatalog
import com.alijafari.red.astronomy.ar.core.ArObserver
import com.alijafari.red.astronomy.ar.core.ArSkyLightingBuilder
import com.alijafari.red.astronomy.ar.core.ArSkySceneComputer
import com.alijafari.red.astronomy.ar.core.ArSkySnapshot
import com.alijafari.red.astronomy.ar.core.ArStarCatalog
import com.alijafari.red.astronomy.domain.CelestialObject
import com.alijafari.red.astronomy.ui.screens.ar.ArOverlayOptions
import com.alijafari.red.astronomy.ui.screens.ar.ArOverlayState
import com.alijafari.red.astronomy.ui.screens.ar.ArScreenSupport
import com.alijafari.red.astronomy.ui.screens.ar.ArSkyOverlay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import androidx.compose.ui.graphics.Canvas as ComposeCanvas

/**
 * Regression test for the AR-screen crash (`ArrayIndexOutOfBoundsException: length=0; index=0` in
 * `ArSkyOverlay.drawStars`): the overlay used to iterate over the star catalogue held by the screen
 * while indexing the per-star arrays of a sky snapshot that had been computed before that catalogue
 * finished loading. Snapshots now carry the catalogue they were computed with; this draws, with real
 * (native) Android graphics, both a snapshot computed without stars and one computed with them, for
 * several view directions, with the simulated sky and in camera mode.
 *
 * The last two tests pin the two halves of that crash separately: the empty array (a snapshot with
 * no star data must draw nothing, not index `FloatArray(0)`) and the catalogue that can be shorter
 * than its own columns (every consumer now iterates the validated count, so star 0's B−V is never
 * read out of an empty array).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class ArSkyOverlayDrawTest {

    private fun asset(name: String): File {
        val candidates = listOf("src/main/assets/ar/$name", "app/src/main/assets/ar/$name")
        return candidates.map { File(it) }.firstOrNull { it.exists() }
            ?: error("Asset not found: $name (searched ${candidates.joinToString()})")
    }

    private val stars: ArStarCatalog by lazy {
        asset("stars_bsc5.bin").inputStream().use { s ->
            asset("star_names.txt").inputStream().use { n -> ArStarCatalog.parse(s, n) }
        }
    }

    private val tehran = ArObserver(35.6892, 51.3890, 1190.0)

    @Test
    fun `snapshots carry the catalogue their star arrays are indexed by`() {
        val builder = ArSkyLightingBuilder()
        val figures = asset("constellations.txt").inputStream().use { ArConstellationCatalog.parse(it, stars.size) }
        val t = 1792175400000L
        val early = ArSkySceneComputer.compute(t, tehran, null, emptyList(), emptyList(), emptyMap(), builder.atmosphereModel)
        assertNull(early.stars)
        assertEquals(0, early.starCount)
        assertEquals(0, early.starEnu.size)
        assertEquals(0, early.constellationLabelEnu.size)

        val full = ArSkySceneComputer.compute(t, tehran, stars, figures, emptyList(), emptyMap(), builder.atmosphereModel)
        assertSame(stars, full.stars)
        assertSame(figures, full.constellations)
        assertEquals(stars.size * 3, full.starEnu.size)
        assertEquals(stars.size, full.starMag.size)
        assertEquals(figures.size * 3, full.constellationLabelEnu.size)

        // A snapshot whose arrays do not match its catalogue cannot be constructed.
        try {
            ArSkySnapshot(
                early.timeMs, early.frame, stars, early.starEnu, early.starMag, early.bodies, early.placements,
                early.constellations, early.constellationLabelEnu, early.sun, early.moon
            )
            fail("A snapshot with mismatched star arrays was accepted")
        } catch (expected: IllegalArgumentException) {
        }
    }

    @Test
    fun `overlay draws snapshots computed before and after the catalogue loaded`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val figures = asset("constellations.txt").inputStream().use { ArConstellationCatalog.parse(it, stars.size) }
        val catalogue = ArScreenSupport.arCatalogue()
        val glyphs = ArGlyphCache(context)
        val density = Density(2.625f)
        val textMeasurer = TextMeasurer(createFontFamilyResolver(context), density, LayoutDirection.Ltr)
        val w = 1080; val h = 2400
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = ComposeCanvas(android.graphics.Canvas(bitmap))
        val state = ArOverlayState()
        try {
            // Night (18:30 UTC = 22:00 Tehran) and an evening with the Moon up (16:00 UTC).
            for (t in listOf(1792175400000L, 1792512000000L)) {
                val builder = ArSkyLightingBuilder()
                val early = ArSkySceneComputer.compute(t, tehran, null, emptyList(), emptyList(), emptyMap(), builder.atmosphereModel)
                val full = ArSkySceneComputer.compute(t, tehran, stars, figures, emptyList(), emptyMap(), builder.atmosphereModel)
                val lighting = builder.build(
                    full.sun.placed.enu, full.moon.placed.enu, full.moon.glyph?.phaseAngleDeg ?: 90.0, tehran.heightM, 4
                )
                val views = listOf(
                    full.moon.placed.azDeg to full.moon.placed.altDeg,
                    0.0 to 89.0,
                    180.0 to 20.0,
                    full.sun.placed.azDeg to full.sun.placed.altDeg
                )
                for ((az, alt) in views) for (cameraMode in listOf(false, true)) for (snap in listOf(early, full)) {
                    val cam = ArCameraModel.virtual(w.toFloat(), h.toFloat(), 0, 60.0, 1.0)
                    cam.setAttitude(ArScreenSupport.manualAttitude(az, alt))
                    bitmap.eraseColor(android.graphics.Color.BLACK)
                    CanvasDrawScope().draw(density, LayoutDirection.Ltr, canvas, Size(w.toFloat(), h.toFloat())) {
                        ArSkyOverlay.draw(
                            scope = this,
                            state = state,
                            snapshot = snap,
                            lighting = lighting,
                            camera = cam,
                            catalogue = catalogue,
                            satellites = emptyList(),
                            glyphs = glyphs,
                            opts = ArOverlayOptions(
                                cameraMode = cameraMode, zoom = 1.0, density = density.density,
                                showStars = true, showConstellations = true, showPlanets = true, showMoon = true,
                                showSun = true, showDeepSky = true, showSatellites = true, showMeteors = true,
                                showNames = true, isFa = false, timeMs = t, selectedId = null, accent = Color(0xFF64B5F6)
                            ),
                            textMeasurer = textMeasurer,
                            fontFamily = FontFamily.Default,
                            aimRadiusPx = 24f * density.density
                        )
                    }
                    if (snap === full) assertTrue("nothing was drawn looking at az=$az alt=$alt", state.items.isNotEmpty())
                }
            }
        } finally {
            glyphs.release()
            bitmap.recycle()
        }
    }

    // ---------------------------------------------------------------------------------------------
    // The empty star data this crash came from
    // ---------------------------------------------------------------------------------------------

    private val frameDensity = Density(2.625f)

    /**
     * A synthetic star pack: [count] positions, [colours] B−V and spectral entries.
     *
     * Real packs carry every column, but a pack that is truncated (or one whose colour column is
     * absent) used to be read past the end of the shorter array — the crash this class guards.
     */
    private fun pack(count: Int, colours: Int): ArStarCatalog = ArStarCatalog(
        hr = IntArray(count) { it + 1 },
        hd = IntArray(count) { 100 + it },
        raDeg = DoubleArray(count) { 10.0 * it },
        // Circumpolar from Tehran (dec > 90° − latitude = 54.3°): never sets, so the star is in
        // the sky at whatever instant the snapshot is computed for.
        decDeg = DoubleArray(count) { 60.0 + it },
        pmRaCosDec = DoubleArray(count),
        pmDec = DoubleArray(count),
        vmag = FloatArray(count) { 1f + it },
        bv = FloatArray(colours) { 0.4f },
        spectral = CharArray(colours) { 'G' },
        names = mapOf(1 to "Teststar")
    )

    /**
     * One overlay frame over a black canvas as raw pixels, so two frames can be compared.
     *
     * Everything except [showStars] is identical between calls, and the camera looks at the same
     * [azDeg]/[altDeg]: a difference between two frames is the star layer's.
     */
    private fun renderFrame(
        snapshot: ArSkySnapshot,
        catalogue: List<CelestialObject>,
        showStars: Boolean,
        azDeg: Double = 90.0,
        altDeg: Double = 35.0
    ): IntArray {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val glyphs = ArGlyphCache(context)
        val w = 540
        val h = 1170
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        try {
            val textMeasurer = TextMeasurer(createFontFamilyResolver(context), frameDensity, LayoutDirection.Ltr)
            val cam = ArCameraModel.virtual(w.toFloat(), h.toFloat(), 0, 60.0, 1.0)
            cam.setAttitude(ArScreenSupport.manualAttitude(azDeg, altDeg))
            val canvas = ComposeCanvas(android.graphics.Canvas(bitmap))
            CanvasDrawScope().draw(frameDensity, LayoutDirection.Ltr, canvas, Size(w.toFloat(), h.toFloat())) {
                ArSkyOverlay.draw(
                    scope = this,
                    state = ArOverlayState(),
                    snapshot = snapshot,
                    lighting = null,
                    camera = cam,
                    catalogue = catalogue,
                    satellites = emptyList(),
                    glyphs = glyphs,
                    opts = ArOverlayOptions(
                        cameraMode = false, zoom = 1.0, density = frameDensity.density,
                        showStars = showStars, showConstellations = true, showPlanets = true, showMoon = true,
                        showSun = true, showDeepSky = true, showSatellites = true, showMeteors = true,
                        showNames = true, isFa = false, timeMs = snapshot.timeMs, selectedId = null,
                        accent = Color(0xFF64B5F6)
                    ),
                    textMeasurer = textMeasurer,
                    fontFamily = FontFamily.Default,
                    aimRadiusPx = 24f * frameDensity.density
                )
            }
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        } finally {
            glyphs.release()
            bitmap.recycle()
        }
        return pixels
    }

    @Test
    fun `a snapshot computed before the star pack loads draws no stars`() {
        val builder = ArSkyLightingBuilder()
        val early = ArSkySceneComputer.compute(
            1792175400000L, tehran, null, emptyList(), emptyList(), emptyMap(), builder.atmosphereModel
        )
        // The product's own cold-start state: the pack is parsed off the UI thread, so the first
        // snapshots carry no stars at all, and the screen hands the overlay a fully populated named
        // catalogue regardless. Star 0 is then a read of FloatArray(0).
        assertEquals(0, early.starCount)
        assertEquals(0, early.starEnu.size)
        assertEquals(0, early.starMag.size)

        val catalogue = ArScreenSupport.arCatalogue()
        assertTrue("the test needs a populated catalogue", catalogue.isNotEmpty())
        val withStars = renderFrame(early, catalogue, showStars = true)
        val withoutStars = renderFrame(early, catalogue, showStars = false)
        assertTrue(
            "drawing a snapshot without star data changed the frame",
            withStars.contentEquals(withoutStars)
        )
    }

    @Test
    fun `a star pack that is missing a column cannot be indexed past its arrays`() {
        // Five positions and no colour column: asking for star 0's B−V is exactly
        // ArrayIndexOutOfBoundsException: length=0; index=0.
        val truncated = pack(count = 5, colours = 0)
        assertEquals(0, truncated.size)
        assertEquals(ArStarCatalog.SOLAR_BV, truncated.colorIndex(0), 1e-9)
        assertNull(truncated.name(0))
        assertEquals(-1, truncated.indexOfHr(1))
        assertEquals(-1, truncated.indexOfHd(100))

        // Two colour entries for five positions: two usable stars, and the rest answer with the
        // solar fallback instead of an index into a short array.
        val partial = pack(count = 5, colours = 2)
        assertEquals(2, partial.size)
        assertEquals(0.4, partial.colorIndex(0), 1e-6)
        assertEquals(ArStarCatalog.SOLAR_BV, partial.colorIndex(2), 1e-9)
        assertEquals("Teststar", partial.name(0))
        assertNull(partial.name(2))

        // The crash's exact shape: a catalogue that reports stars it cannot answer for. A zero-length
        // pack still produces a snapshot with a catalogue attached, so the star pass does run — and
        // reading star 0 of it used to be `ArrayIndexOutOfBoundsException: length=0; index=0`.
        val builder = ArSkyLightingBuilder()
        val emptySnap = ArSkySceneComputer.compute(
            1792175400000L, tehran, truncated, emptyList(), emptyList(), emptyMap(), builder.atmosphereModel
        )
        assertSame(truncated, emptySnap.stars)
        assertEquals(0, emptySnap.starCount)
        assertEquals(0, emptySnap.starEnu.size)
        val emptyLit = renderFrame(emptySnap, ArScreenSupport.arCatalogue(), showStars = true)
        val emptyBare = renderFrame(emptySnap, ArScreenSupport.arCatalogue(), showStars = false)
        assertTrue(
            "a catalogue with no usable stars drew something",
            emptyLit.contentEquals(emptyBare)
        )

        // The sky computed from a partially answered pack holds exactly the stars it can answer for,
        // so the draw pass can never be handed a catalogue longer than the snapshot's own arrays.
        val snap = ArSkySceneComputer.compute(
            1792175400000L, tehran, partial, emptyList(), emptyList(), emptyMap(), builder.atmosphereModel
        )
        assertSame(partial, snap.stars)
        assertEquals(2, snap.starCount)
        assertEquals(6, snap.starEnu.size)
        assertEquals(2, snap.starMag.size)

        // And the two stars it can answer for are still drawn: look straight at star 0 (the Sun is
        // 53° below the horizon at this instant, so the sky is dark and stars are at full strength)
        // and the star layer must light up pixels that the same frame without stars leaves dark.
        val e = snap.starEnu
        val alt = Math.toDegrees(kotlin.math.asin(e[2].toDouble()))
        val az = (Math.toDegrees(kotlin.math.atan2(e[0].toDouble(), e[1].toDouble())) + 360.0) % 360.0
        val lit = renderFrame(snap, ArScreenSupport.arCatalogue(), showStars = true, azDeg = az, altDeg = alt)
        val bare = renderFrame(snap, ArScreenSupport.arCatalogue(), showStars = false, azDeg = az, altDeg = alt)
        assertTrue("the visible stars were not drawn", !lit.contentEquals(bare))
    }
}

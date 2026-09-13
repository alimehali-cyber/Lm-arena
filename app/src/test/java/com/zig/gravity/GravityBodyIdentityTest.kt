package com.zig.gravity

import com.zig.gravity.physics.BodyType
import com.zig.gravity.physics.EngineConstants
import com.zig.gravity.physics.SimArrays
import com.zig.gravity.sim.BodyCatalog
import com.zig.gravity.sim.Preset
import com.zig.gravity.sim.SaveState
import com.zig.gravity.sim.SimulationViewModel
import com.zig.gravity.ui.theme.BodyIdentities
import com.zig.gravity.ui.theme.BodyIdentity
import com.zig.gravity.ui.theme.GravityChrome
import com.zig.gravity.ui.theme.TableSurfaces
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * The planet identity pass: a body must read as *that* body, and nothing else may move.
 *
 * Two halves, matching the two risks of a visual-only change. The first half is the identity itself
 * — pure data, so it can be proved on the plain JVM: every mark is inscribed in the body disc (which
 * is why the renderer needs no clip path and the limb stays antialiased), the marks are deterministic
 * across recreations, and each body's palette and marks carry the real object's visual signature.
 *
 * The second half is the guardrail. A texture pass is exactly the kind of change that quietly drags a
 * radius, a mass or a lighting constant along with it, so the marble's lighting geometry, the cast
 * shadow, the chrome, the surface catalog, the engine constants and the save schema are all pinned
 * here — and Saturn's rings are proved to be strokes, not physics.
 */
class GravityBodyIdentityTest {

    private val mpd = 1.122e9

    // ================================ the identity layer =========================================

    @Test
    fun everyBodyCarriesAContainedDeterministicIdentity() {
        val textured = setOf(
            "sun", "mercury", "venus", "earth", "moon", "mars",
            "jupiter", "saturn", "uranus", "neptune", "asteroid"
        )

        // The catalog and the identity map must agree, so a body added to one can never be forgotten
        // in the other — and the only untextured bodies are the three that must stay exactly as they
        // are: the test marble, the black hole and the wormhole mouth.
        assertEquals(textured, BodyIdentities.all.keys)
        assertEquals(
            textured,
            BodyCatalog.all
                .filter { !BodyIdentities.of(it.key, it.type).isEmpty }
                .map { it.key }
                .toSet()
        )

        for (entry in BodyCatalog.all) {
            val identity = BodyIdentities.of(entry.key, entry.type)
            // Same key, same instance: identities are built once, never per frame or per call.
            assertSame(identity, BodyIdentities.of(entry.key, entry.type))
            assertTrue("${entry.key}: mark budget", identity.marks.size <= BodyIdentities.MAX_MARKS)
            assertTrue("${entry.key}: ring budget", identity.rings.size <= BodyIdentities.MAX_RINGS)

            for (mark in identity.marks) {
                assertTrue(
                    "${entry.key}: a mark must be inscribed in the body disc, so the renderer never " +
                        "needs an aliased clip path at the limb",
                    mark.isInsideUnitDisc()
                )
                assertTrue("${entry.key}: mark alpha ${mark.alpha}", mark.alpha in 0.05f..1f)
                assertTrue("${entry.key}: mark extent", mark.rx > 0f && mark.ry > 0f)
                assertEquals(
                    "${entry.key}: mark tones are opaque; alpha travels separately so toning cannot " +
                        "disturb it",
                    0xFFL, (mark.argb ushr 24) and 0xFFL
                )
            }
            for (ring in identity.rings) {
                assertTrue("${entry.key}: a ring band lies outside the body", ring.radiusFraction > 1f)
                assertTrue("${entry.key}: ring thickness", ring.thicknessFraction in 0.01f..0.2f)
                assertTrue("${entry.key}: ring alpha", ring.alpha in 0.05f..1f)
                assertEquals("${entry.key}: ring tones are opaque", 0xFFL, (ring.argb ushr 24) and 0xFFL)
            }
            if (identity.rings.isNotEmpty()) {
                assertTrue(
                    "${entry.key}: rings are a tilted ellipse, not a circle",
                    identity.ringSquash in 0.15f..0.5f
                )
            }
        }

        // Unnamed bodies fall back by type, exactly like colorOf does — except that an unnamed planet
        // gets neutral banding rather than Earth's continents, because inventing land on an object
        // nobody identified would be invented detail.
        assertSame(BodyIdentities.SUN, BodyIdentities.of(null, BodyType.SUN))
        assertSame(BodyIdentities.MOON, BodyIdentities.of(null, BodyType.MOON))
        assertSame(BodyIdentities.ASTEROID, BodyIdentities.of(null, BodyType.ASTEROID))
        assertSame(BodyIdentities.GENERIC_PLANET, BodyIdentities.of(null, BodyType.PLANET))
        assertSame(BodyIdentities.GENERIC_PLANET, BodyIdentities.of("no_such_body", BodyType.PLANET))
        assertNotEquals(BodyIdentities.EARTH, BodyIdentities.of(null, BodyType.PLANET))
        assertSame(BodyIdentity.NONE, BodyIdentities.of(null, BodyType.TEST_MARBLE))
        assertSame(BodyIdentity.NONE, BodyIdentities.of(null, BodyType.BLACK_HOLE))
        assertSame(BodyIdentity.NONE, BodyIdentities.of(null, BodyType.WORMHOLE_MOUTH))
        assertSame(BodyIdentity.NONE, BodyIdentities.of("black_hole", BodyType.BLACK_HOLE))
        assertSame(BodyIdentity.NONE, BodyIdentities.of("wormhole", BodyType.WORMHOLE_MOUTH))
        assertSame(BodyIdentity.NONE, BodyIdentities.of("marble", BodyType.TEST_MARBLE))

        // The seeded scatter is deterministic: same seed, same marks, same order — so a body looks
        // identical after the sandbox is closed and reopened.
        val a = BodyIdentities.scatter(
            seed = 11, count = 6, minRadius = 0.05f, maxRadius = 0.12f,
            argb = 0xFF6E655A, minAlpha = 0.16f, maxAlpha = 0.26f, reach = 0.68f
        )
        val b = BodyIdentities.scatter(
            seed = 11, count = 6, minRadius = 0.05f, maxRadius = 0.12f,
            argb = 0xFF6E655A, minAlpha = 0.16f, maxAlpha = 0.26f, reach = 0.68f
        )
        assertEquals("the same seed must print the same marks", a, b)
        val c = BodyIdentities.scatter(
            seed = 12, count = 6, minRadius = 0.05f, maxRadius = 0.12f,
            argb = 0xFF6E655A, minAlpha = 0.16f, maxAlpha = 0.26f, reach = 0.68f
        )
        assertNotEquals("a different seed must print different marks", a, c)
        assertTrue("scattered marks stay inside the disc", a.all { it.isInsideUnitDisc() })
        assertEquals(6, a.size)
    }

    @Test
    fun planetaryIdentitiesReadAsTheirRealBodies() {
        // ---- Sun: still gold, with only the softest warm mottling --------------------------------
        val sun = hsv(BodyCatalog.SUN.colorArgb)
        assertTrue("the Sun stays gold, hue ${sun[0]}", sun[0] in 45f..60f)
        assertTrue("the Sun stays bright, val ${sun[2]}", sun[2] >= 0.95f)
        val sunMarks = BodyIdentities.SUN.marks
        assertTrue("a few soft patches only, found ${sunMarks.size}", sunMarks.size in 3..9)
        for (mark in sunMarks) {
            val h = hsv(mark.argb)
            assertTrue("solar mottling is warm orange/red, hue ${h[0]}", h[0] in 10f..45f)
            assertTrue("and extremely soft, alpha ${mark.alpha}", mark.alpha <= 0.20f)
        }

        // ---- Mercury: bright warm stone grey, low-contrast warm craters --------------------------
        val mercury = hsv(BodyCatalog.MERCURY.colorArgb)
        assertTrue("Mercury is a warm stone grey, hue ${mercury[0]}", mercury[0] in 20f..50f)
        assertTrue("Mercury is bright, val ${mercury[2]}", mercury[2] >= 0.70f)
        assertTrue("Mercury is desaturated, sat ${mercury[1]}", mercury[1] <= 0.20f)
        val mercuryMarks = BodyIdentities.MERCURY.marks
        assertTrue("a cratered field, found ${mercuryMarks.size}", mercuryMarks.size >= 6)
        for (mark in mercuryMarks) {
            val h = hsv(mark.argb)
            assertTrue("Mercury's craters are warm, hue ${h[0]}", h[0] in 20f..50f)
            assertTrue("and very low contrast", h[2] < mercury[2])
            assertTrue("craters stay small", mark.rx <= 0.25f)
        }

        // ---- Moon: neutral white, cool maria and lit crater floors -------------------------------
        val moon = hsv(BodyCatalog.MOON.colorArgb)
        assertEquals("the Moon stays pure white", 0f, moon[1], 1e-6f)
        assertEquals(1f, moon[2], 1e-6f)
        val moonMarks = BodyIdentities.MOON.marks
        assertTrue("soft maria and craters, found ${moonMarks.size}", moonMarks.size >= 6)
        assertTrue(
            "the maria are cool grey",
            moonMarks.count { hsv(it.argb)[0] in 200f..240f } >= 5
        )
        assertTrue(
            "the larger craters catch the light",
            moonMarks.count { hsv(it.argb)[2] >= 0.95f } >= 2
        )

        // Mercury and the Moon share a size class, so they must not share a look: warm and busy
        // against cool and calm.
        val mercuryHue = mercuryMarks.map { hsv(it.argb)[0] }.average()
        val moonHue = moonMarks.filter { hsv(it.argb)[1] > 0.02f }.map { hsv(it.argb)[0] }.average()
        assertTrue("Mercury warm ($mercuryHue) against the Moon cool ($moonHue)", mercuryHue < 60.0)
        assertTrue("the Moon's marks are cool ($moonHue)", moonHue > 180.0)
        assertTrue("the Moon carries broad maria", moonMarks.count { it.rx >= 0.18f } >= 3)
        assertTrue("Mercury carries none: craters only, never a mare", mercuryMarks.none { it.rx >= 0.26f })

        // ---- Venus: pale cream under broad, soft cloud bands -------------------------------------
        val venus = hsv(BodyCatalog.VENUS.colorArgb)
        assertTrue("Venus is pale warm cream, hue ${venus[0]}", venus[0] in 35f..55f)
        assertTrue("Venus is bright, val ${venus[2]}", venus[2] >= 0.85f)
        val venusMarks = BodyIdentities.VENUS.marks
        assertTrue("cloud bands, found ${venusMarks.size}", venusMarks.size >= 3)
        assertTrue("no sharp surface detail: every mark is broad", venusMarks.all { it.ry >= 0.05f })
        assertTrue("and soft", venusMarks.all { it.alpha <= 0.40f })

        // ---- Earth: vivid blue ocean, sparse green land, a few white clouds ----------------------
        val earth = hsv(BodyCatalog.EARTH.colorArgb)
        assertTrue("Earth is vivid blue, hue ${earth[0]}", earth[0] in 195f..225f)
        assertTrue("Earth is saturated, sat ${earth[1]}", earth[1] >= 0.60f)
        val earthMarks = BodyIdentities.EARTH.marks
        val land = earthMarks.filter { hsv(it.argb)[0] in 90f..160f }
        assertTrue("sparse stylised land masses, found ${land.size}", land.size in 5..8)
        assertTrue("land must read clearly", land.all { it.alpha >= 0.75f })
        val clouds = earthMarks.filter { hsv(it.argb)[1] <= 0.05f && hsv(it.argb)[2] >= 0.99f }
        assertTrue("a few cloud streaks, found ${clouds.size}", clouds.size in 2..4)
        assertTrue("clouds stay subtle", clouds.all { it.alpha <= 0.35f })

        // ---- Mars: vivid red-orange, rust regions, a pale polar cap ------------------------------
        val mars = hsv(BodyCatalog.MARS.colorArgb)
        assertTrue("Mars is red-orange, hue ${mars[0]}", mars[0] in 8f..25f)
        assertTrue("Mars is vivid, not terracotta: sat ${mars[1]}", mars[1] >= 0.75f)
        assertTrue("Mars is bright, val ${mars[2]}", mars[2] >= 0.85f)
        assertNotEquals("Mars is no longer the old terracotta tone", 0xFFA6705CL, BodyCatalog.MARS.colorArgb)
        val marsMarks = BodyIdentities.MARS.marks
        assertTrue(
            "darker rust regions",
            marsMarks.count { hsv(it.argb)[0] in 5f..30f && hsv(it.argb)[2] < mars[2] - 0.15f } >= 4
        )
        assertTrue(
            "a pale polar cap",
            marsMarks.count { hsv(it.argb)[2] >= 0.99f && hsv(it.argb)[1] <= 0.15f } >= 1
        )

        // ---- Jupiter: cream base, several soft belts, exactly one red spot -----------------------
        val jupiter = hsv(BodyCatalog.JUPITER.colorArgb)
        assertTrue("Jupiter is cream/ivory, hue ${jupiter[0]}", jupiter[0] in 30f..55f)
        assertTrue("Jupiter is bright, val ${jupiter[2]}", jupiter[2] >= 0.85f)
        assertTrue("Jupiter is pale, sat ${jupiter[1]}", jupiter[1] <= 0.35f)
        val jupiterMarks = BodyIdentities.JUPITER.marks
        val belts = jupiterMarks.filter { it.rx >= 0.5f }
        assertTrue("several horizontal belts, found ${belts.size}", belts.size >= 5)
        assertTrue("the belts stay soft", belts.all { it.alpha <= 0.60f })
        assertEquals(
            "exactly one Great Red Spot",
            1,
            jupiterMarks.count { hsv(it.argb)[0] < 20f && hsv(it.argb)[1] > 0.6f }
        )

        // ---- Saturn: pale cream, subtle banding, and the rings -----------------------------------
        val saturn = hsv(BodyCatalog.SATURN.colorArgb)
        assertTrue("Saturn is pale warm cream, hue ${saturn[0]}", saturn[0] in 35f..55f)
        assertTrue("Saturn is bright, val ${saturn[2]}", saturn[2] >= 0.88f)
        assertTrue("Saturn has subtle banding", BodyIdentities.SATURN.marks.count { it.rx >= 0.5f } >= 2)
        assertTrue("Saturn has rings", BodyIdentities.SATURN.rings.size >= 3)

        // ---- Uranus: pale cyan, almost featureless ----------------------------------------------
        val uranus = hsv(BodyCatalog.URANUS.colorArgb)
        assertTrue("Uranus is pale cyan, hue ${uranus[0]}", uranus[0] in 160f..200f)
        assertTrue("Uranus is pale, sat ${uranus[1]}", uranus[1] <= 0.35f)
        assertTrue("Uranus is bright, val ${uranus[2]}", uranus[2] >= 0.85f)
        assertTrue(
            "Uranus banding is extremely subtle",
            BodyIdentities.URANUS.marks.all { it.alpha <= 0.35f }
        )
        assertTrue("Uranus is not Earth: hue ${uranus[0]} vs ${earth[0]}", abs(uranus[0] - earth[0]) > 20f)

        // ---- Neptune: deep cobalt, visibly darker than Uranus ------------------------------------
        val neptune = hsv(BodyCatalog.NEPTUNE.colorArgb)
        assertTrue("Neptune is cobalt, hue ${neptune[0]}", neptune[0] in 210f..250f)
        assertTrue("Neptune is saturated, sat ${neptune[1]}", neptune[1] >= 0.60f)
        assertTrue("Neptune is deeper than Uranus, val ${neptune[2]} vs ${uranus[2]}", neptune[2] < uranus[2])
        assertTrue(
            "Neptune is far darker than Uranus: " +
                "${relativeLuminance(BodyCatalog.NEPTUNE.colorArgb)} vs " +
                relativeLuminance(BodyCatalog.URANUS.colorArgb),
            relativeLuminance(BodyCatalog.NEPTUNE.colorArgb) <
                relativeLuminance(BodyCatalog.URANUS.colorArgb) * 0.5f
        )
        assertTrue(
            "Neptune carries soft cloud structure",
            BodyIdentities.NEPTUNE.marks.size >= BodyIdentities.URANUS.marks.size
        )

        // ---- Asteroid: the same warm rocky family, a little battered -----------------------------
        assertEquals("the asteroid keeps its terracotta family", 0xFFD89B66L, BodyCatalog.ASTEROID.colorArgb)
        val asteroid = hsv(BodyCatalog.ASTEROID.colorArgb)
        val asteroidMarks = BodyIdentities.ASTEROID.marks
        assertTrue(
            "irregular darker patches",
            asteroidMarks.count { hsv(it.argb)[2] < asteroid[2] } >= 4
        )
        assertTrue("no noisy detail: bounded count ${asteroidMarks.size}", asteroidMarks.size <= 6)

        // ---- Black hole and wormhole keep their treatment exactly --------------------------------
        assertEquals(0xFF0A0A0CL, BodyCatalog.BLACK_HOLE.colorArgb)
        assertEquals(GravityChrome.ACCENT_DARK, BodyCatalog.WORMHOLE.colorArgb)
        assertTrue(BodyIdentities.of("black_hole", BodyType.BLACK_HOLE).isEmpty)
        assertTrue(BodyIdentities.of("wormhole", BodyType.WORMHOLE_MOUTH).isEmpty)
        assertTrue(BodyIdentities.of("marble", BodyType.TEST_MARBLE).isEmpty)
    }

    // ================================ the guardrails =============================================

    @Test
    fun saturnsRingsAreVisualOnlyAndNothingPhysicalMoved() {
        val rings = BodyIdentities.SATURN.rings
        assertTrue("rings begin outside the body", rings.all { it.radiusFraction > 1f })
        assertTrue("and stay a bounded visual extent", rings.all { it.radiusFraction < 2.5f })

        // Concentric, in order, with transparent gaps between them — the widest being the Cassini
        // division. A gap is empty space, not a band: nothing is drawn there.
        val radii = rings.map { it.radiusFraction }
        assertEquals("rings are ordered outward", radii.sorted(), radii)
        assertEquals("rings are distinct", radii.distinct().size, radii.size)
        val gaps = rings.zipWithNext { inner, outer ->
            (outer.radiusFraction - outer.thicknessFraction / 2f) -
                (inner.radiusFraction + inner.thicknessFraction / 2f)
        }
        assertTrue("transparent gaps between the bands: $gaps", gaps.all { it >= 0.08f })
        assertTrue("one gap is the Cassini division", (gaps.maxOrNull() ?: 0f) >= 0.09f)

        // Saturn's body is exactly the body it was. The rings added no mass, no display size and no
        // physical radius, because they are strokes in the draw phase and nothing else.
        assertEquals(95.16 * EngineConstants.M_EARTH, BodyCatalog.SATURN.massKg, 0.0)
        assertEquals(15.0, BodyCatalog.SATURN.dp, 0.0)
        assertEquals(5.8232e7, BodyCatalog.SATURN.realRadiusM, 0.0)

        // The catalog did not grow, shrink or reorder.
        assertEquals(
            listOf(
                "sun", "mercury", "venus", "earth", "moon", "mars", "jupiter", "saturn",
                "uranus", "neptune", "asteroid", "marble", "black_hole", "wormhole"
            ),
            BodyCatalog.all.map { it.key }
        )

        // Nor did the engine move. These are the constants the whole simulation stands on; a visual
        // pass has no business touching a single one of them.
        assertEquals(6.67430e-11, EngineConstants.G, 0.0)
        assertEquals(2.99792458e8, EngineConstants.C, 0.0)
        assertEquals(1.989e30, EngineConstants.M_SUN, 0.0)
        assertEquals(6.957e8, EngineConstants.R_SUN, 0.0)
        assertEquals(5.972e24, EngineConstants.M_EARTH, 0.0)
        assertEquals(6.371e6, EngineConstants.R_EARTH, 0.0)
        assertEquals(7.348e22, EngineConstants.M_MOON, 0.0)
        assertEquals(1.737e6, EngineConstants.R_MOON, 0.0)
        assertEquals(1.496e11, EngineConstants.AU, 0.0)
        assertEquals(3.844e8, EngineConstants.MOON_ORBIT_RADIUS, 0.0)
        assertEquals(3600.0, EngineConstants.DT, 0.0)
        assertEquals(1.0e6, EngineConstants.BASE, 0.0)
        assertEquals(1.0e6, EngineConstants.EPS_SOFT, 0.0)
        assertEquals(1.0e6, EngineConstants.V_MAX, 0.0)
        assertEquals(0.1, EngineConstants.MAX_FRAME_SECONDS, 0.0)
        assertEquals(20, EngineConstants.MAX_BODIES)
        assertEquals(1024, EngineConstants.MAX_SUBSTEPS)
        assertEquals(3, EngineConstants.MAX_REFINE_DEPTH)

        // The identity API takes a catalog key and a type and nothing else, so no mass, radius,
        // velocity or simulation value can reach the visual layer even by accident.
        assertSame(BodyIdentities.of("saturn", BodyType.PLANET), BodyIdentities.of("saturn", BodyType.PLANET))
        assertFalse(
            "rings must not be mistaken for a body radius",
            BodyIdentities.SATURN.rings.any { it.radiusFraction <= 1f }
        )
    }

    @Test
    fun theMarbleLightingShadowAndChromeStructureIsUntouched() {
        val canvas = uiSource("TabletopCanvas.kt")

        // The marble's own lighting: same off-centre gradient centre, same radius, same three stops,
        // same rim, same single specular dot in the same place at the same size.
        assertTrue("the base gradient is still lit from the top-left", canvas.contains("center = Offset(-r * 0.30f, -r * 0.34f)"))
        assertTrue("the base gradient still reaches 1.55r", canvas.contains("radius = r * 1.55f"))
        assertTrue("the base gradient still has three stops", canvas.contains("arrayOf(0f to light, 0.45f to tone, 1f to dark)"))
        assertTrue("the rim is unchanged", canvas.contains("drawCircle(cache.rim[i], rr, Offset.Zero, style = cache.strokeRim)"))
        assertTrue("the specular highlight is unchanged", canvas.contains("drawCircle(cache.specular[i], rr * 0.17f, Offset(-rr * 0.34f, -rr * 0.38f))"))

        // §5 layering, proved from the order the calls appear in: base -> marks -> the lighting
        // re-applied over them -> rim -> specular. The marks may never sit on top of the shading.
        val base = canvas.indexOf("cache.base[i]?.let { drawCircle(it, rr, Offset.Zero) }")
        val marks = canvas.indexOf("drawIdentityMarks(cache, i, rr)")
        val lighting = canvas.indexOf("alpha = BodyIdentities.LIGHTING_OVERLAY_ALPHA")
        val rim = canvas.indexOf("drawCircle(cache.rim[i], rr, Offset.Zero, style = cache.strokeRim)")
        val specular = canvas.indexOf("drawCircle(cache.specular[i], rr * 0.17f")
        assertTrue("the base gradient is drawn", base > 0)
        assertTrue("marks come after the base", marks > base)
        assertTrue("the existing lighting is re-applied over the marks", lighting > marks)
        assertTrue("the rim still follows the lighting", rim > lighting)
        assertTrue("and the specular is still last on the body", specular > rim)

        // Rings: the far half under the sphere, the near half over it. That split is what makes them
        // pass behind and in front, and it is the only place rings are drawn.
        val farRings = canvas.indexOf("drawRingHalf(cache, i, rr, far = true)")
        val nearRings = canvas.indexOf("drawRingHalf(cache, i, rr, far = false)")
        assertTrue("the far ring half is painted before the body", farRings in 0 until base)
        assertTrue("the near ring half after the whole marble", nearRings > specular)

        // The two-layer cast shadow, geometry and all, is exactly as the previous pass left it.
        assertTrue(canvas.contains("Offset(rr * 0.38f - rr * 0.725f, rr * 0.34f - rr * 0.575f)"))
        assertTrue(canvas.contains("Size(rr * 1.45f, rr * 1.15f)"))
        assertTrue(canvas.contains("Offset(rr * 0.20f - rr * 0.40f, rr * 0.18f - rr * 0.40f)"))
        assertTrue(canvas.contains("Size(rr * 0.80f, rr * 0.80f)"))

        // Primitives only: no bitmaps, no image assets, no shaders, no blur, no shadow layers. Any
        // mention at all is only allowed inside a comment that says it is not used.
        for (banned in listOf("drawImage(", "ImageBitmap", "asImageBitmap", "Shader", "blur(", "setShadowLayer(")) {
            for (line in canvas.lines().filter { it.contains(banned) }) {
                val trimmed = line.trim()
                assertTrue("'$banned' may only appear in a comment, found: $line", trimmed.startsWith("//") || trimmed.startsWith("*"))
            }
        }

        // Chrome, surfaces and shadow alphas: the previous refresh is intact.
        assertEquals(0.32f, GravityChrome.SHADOW_UMBRA_ALPHA_DARK, 1e-6f)
        assertEquals(0.26f, GravityChrome.SHADOW_UMBRA_ALPHA_LIGHT, 1e-6f)
        assertEquals(0.20f, GravityChrome.SHADOW_PENUMBRA_ALPHA_DARK, 1e-6f)
        assertEquals(0.16f, GravityChrome.SHADOW_PENUMBRA_ALPHA_LIGHT, 1e-6f)
        assertEquals(0xFF2DD4BFL, GravityChrome.ACCENT_DARK)
        assertEquals(0xFF0E9F8FL, GravityChrome.ACCENT_LIGHT)
        assertEquals(
            listOf("midnight", "charcoal", "ocean", "lavender", "paper", "porcelain", "blush"),
            TableSurfaces.all.map { it.key }
        )
    }

    @Test
    fun saveRestoreStillCarriesPhysicsAndNoVisualFields() {
        val vm = SimulationViewModel()
        vm.onViewportChanged(400.0)
        vm.onViewportSizePx(1080f, 2000f)
        vm.loadPreset(Preset.FULL_SOLAR_SYSTEM)
        val blob = vm.serialize()

        // The schema did not move: still version 2, still twelve header fields, still twelve fields
        // per body. A texture has no business being persisted.
        val head = blob.split("|")
        assertEquals("the session header is still twelve fields", 12, head.size)
        assertEquals("and still version 2", "2", head[0])

        val bodies = head[11].split(";")
        assertTrue("the solar system preset is on the table", bodies.size >= 9)
        val saturn = bodies.first { it.split(",")[11] == "saturn" }
        assertEquals("a saved body is still twelve physical fields", 12, saturn.split(",").size)

        val arrays = SimArrays()
        arrays.setMetersPerDp(mpd)
        val session = SaveState.decode(blob, arrays)
        assertNotNull("the save still loads", session)
        val slot = arrays.slotOfCatalog("saturn")
        assertTrue("Saturn came back", slot >= 0)
        assertEquals("with its real mass", BodyCatalog.SATURN.massKg, arrays.mass[slot], 0.0)
        assertEquals("its display size", BodyCatalog.SATURN.dp, arrays.radiusDp[slot], 0.0)
        assertEquals("its type", BodyType.PLANET, arrays.typeOf(slot))
        assertEquals("and its catalog key", "saturn", arrays.catalogKey[slot])
        assertEquals("nothing visual was persisted", "midnight", session!!.tableSurface)
    }

    // ================================ helpers ====================================================

    private fun uiSource(name: String): String {
        val f = File(uiDir(), name)
        assertTrue("missing source file $name", f.isFile)
        return f.readText()
    }

    private fun uiDir(): File {
        // Gradle runs unit tests with the module directory as the working directory, but walk up
        // anyway so the guard does not depend on that detail.
        var dir: File? = File("").absoluteFile
        while (dir != null) {
            val candidate = File(dir, "app/src/main/java/com/zig/gravity/ui")
            if (candidate.isDirectory) return candidate
            val direct = File(dir, "src/main/java/com/zig/gravity/ui")
            if (direct.isDirectory) return direct
            dir = dir.parentFile
        }
        throw AssertionError("could not locate the gravity ui source directory")
    }

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

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
import com.zig.gravity.ui.theme.MarkEdge
import com.zig.gravity.ui.theme.MarkKind
import com.zig.gravity.ui.theme.MarkRole
import com.zig.gravity.ui.theme.SphereProjection
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
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The planet identity pass: a body must read as *that* body, its detail must be wrapped onto the
 * sphere, and nothing else may move.
 *
 * Three halves, matching the three risks of a visual-only change. The first is the identity itself
 * — pure data, so it can be proved on the plain JVM: every projected feature, stroke width and all,
 * stays inside the body disc (which is why the renderer needs no clip path and the limb stays
 * antialiased); the marks are deterministic across recreations; and each body's palette and marks
 * carry the real object's visual signature.
 *
 * The second is the sphere: features are authored as surface positions and projected, so the test
 * proves the projection really foreshortens (a cap near the limb comes back thinner than the same
 * cap at the centre) and really stays inside.
 *
 * The third is the guardrail. A texture pass is exactly the kind of change that quietly drags a
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
                assertTrue("${entry.key}: mark alpha ${mark.alpha}", mark.alpha in 0.05f..1f)
                assertTrue("${entry.key}: mark extent", mark.rx > 0f && mark.ry > 0f)
                assertEquals(
                    "${entry.key}: mark tones are opaque; alpha travels separately so toning cannot " +
                        "disturb it",
                    0xFFL, (mark.argb ushr 24) and 0xFFL
                )
                if (mark.kind == MarkKind.BLOB) {
                    assertTrue(
                        "${entry.key}: a cap's surface position lies on the visible hemisphere",
                        mark.isInsideUnitDisc()
                    )
                }
                // The compositing strength can never reach a flat stamp: a multiplied feature keeps
                // at least 18% of the light under it, a screened one adds at most 80% of white.
                if (mark.role == MarkRole.DARKEN) {
                    assertTrue(
                        "${entry.key}: a multiplied feature still lets the shading through",
                        effectiveDarken(mark.alpha, mark.argb) >= 0.18f
                    )
                } else if (mark.role == MarkRole.LIGHTEN) {
                    assertTrue(
                        "${entry.key}: a screened feature never blows out",
                        min(1f, mark.alpha) * relativeLuminance(mark.argb) <= 0.80f
                    )
                }
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
                // Belts and rings lean by the same axis, so the whole table curves one way.
                assertEquals(identity.ringSquash, identity.axisTilt, 1e-6f)
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
        assertTrue("scattered marks stay on the visible hemisphere", a.all { it.isInsideUnitDisc() })
        assertEquals(6, a.size)
    }

    @Test
    fun projectedFeaturesWrapTheSphereAndNeverCrossTheLimb() {
        for (entry in BodyCatalog.all) {
            val identity = BodyIdentities.of(entry.key, entry.type)
            for (mark in identity.marks) {
                if (mark.kind == MarkKind.BLOB) {
                    val pr = SphereProjection.projectBlob(mark.cx, mark.cy, mark.rx, mark.ry)
                    // Wrapping is a contraction: projection can only squeeze, never stretch.
                    assertTrue(
                        "${entry.key}: projection squeezes",
                        pr.minor <= pr.major + 1e-6f
                    )
                    assertTrue("${entry.key}: foreshortening is real", pr.nz in 0f..1f)
                    // The drawn ellipse, rotated and all, stays inside the disc — so no clip path.
                    val phi = Math.toRadians(pr.degrees.toDouble())
                    for (s in 0 until 360) {
                        val t = 2.0 * Math.PI * s / 360
                        val ux = pr.major * cos(t).toFloat()
                        val uy = pr.minor * sin(t).toFloat()
                        val x = pr.cx + ux * cos(phi).toFloat() - uy * sin(phi).toFloat()
                        val y = pr.cy + ux * sin(phi).toFloat() + uy * cos(phi).toFloat()
                        assertTrue(
                            "${entry.key}: a projected cap crosses the limb at ($x, $y)",
                            x * x + y * y <= 1.0001f
                        )
                    }
                } else {
                    // A belt is stroked, so the containment claim has to carry the stroke: sample the
                    // whole drawn outline, both sides of the centre line, over the whole arc.
                    val chord = sqrt(max(0f, 1f - mark.cy * mark.cy))
                    val sliceWidth = (2f * mark.ry / BodyIdentities.BAND_SLICES) * chord
                    val hw = sliceWidth * 1.15f / 2f
                    val arc = SphereProjection.projectBand(mark.cy, identity.axisTilt, hw)
                    assertTrue("${entry.key}: a belt keeps a visible arc", arc.sweepDegrees > 30f)
                    val start = Math.toRadians(arc.startDegrees.toDouble())
                    val sweep = Math.toRadians(arc.sweepDegrees.toDouble())
                    for (s in 0 until 400) {
                        val t = (start + sweep * s / 399).toFloat()
                        val x = arc.a * cos(t)
                        val y = arc.yc + arc.b * sin(t)
                        val dx = -arc.a * sin(t)
                        val dy = arc.b * cos(t)
                        val nl = max(1e-6f, sqrt(dx * dx + dy * dy))
                        for (sgn in floatArrayOf(1f, -1f)) {
                            val px = x + sgn * (-dy / nl) * hw
                            val py = y + sgn * (dx / nl) * hw
                            assertTrue(
                                "${entry.key}: a stroked belt crosses the limb at ($px, $py)",
                                px * px + py * py <= 1.0001f
                            )
                        }
                    }
                }
            }
        }

        // The projection really is a sphere: the same cap foreshortens harder the closer it sits to
        // the limb, and is untouched at the centre.
        val centre = SphereProjection.projectBlob(0f, 0f, 0.2f, 0.2f)
        val mid = SphereProjection.projectBlob(0.5f, 0.5f, 0.2f, 0.2f)
        val limb = SphereProjection.projectBlob(0.66f, 0.66f, 0.16f, 0.16f)
        assertEquals("a cap at the sub-observer point is not foreshortened", 1f, centre.nz, 1e-6f)
        assertEquals("nor rotated", 0f, centre.degrees, 1e-6f)
        assertTrue("a cap mid-disc foreshortens", mid.nz < 1f)
        assertTrue("and one near the limb foreshortens harder", limb.nz < mid.nz)
        assertTrue(
            "an isotropic cap keeps exactly its foreshortening as axis ratio",
            abs(mid.minor / mid.major - mid.nz) < 1e-3f
        )
        assertTrue(
            "and the ratio collapses toward the limb",
            limb.minor / limb.major < mid.minor / mid.major
        )

        // Deterministic, like everything else in this layer.
        assertEquals(
            SphereProjection.projectBlob(0.3f, -0.4f, 0.2f, 0.1f),
            SphereProjection.projectBlob(0.3f, -0.4f, 0.2f, 0.1f)
        )
        assertEquals(
            SphereProjection.projectBand(0.2f, 0.3f, 0.02f),
            SphereProjection.projectBand(0.2f, 0.3f, 0.02f)
        )
    }

    @Test
    fun planetaryIdentitiesReadAsTheirRealBodies() {
        // ---- Sun: a light source — corona, granulation in two temperature families --------------
        val sun = hsv(BodyCatalog.SUN.colorArgb)
        assertTrue("the Sun stays gold, hue ${sun[0]}", sun[0] in 45f..60f)
        assertTrue("the Sun stays bright, val ${sun[2]}", sun[2] >= 0.95f)
        val sunMarks = BodyIdentities.SUN.marks
        assertTrue("granulation, found ${sunMarks.size}", sunMarks.size in 10..16)
        val cool = sunMarks.filter { it.role == MarkRole.DARKEN }
        val hot = sunMarks.filter { it.role == MarkRole.LIGHTEN }
        assertTrue("cool granulation multiplies, found ${cool.size}", cool.size >= 6)
        assertTrue("hot granulation screens, found ${hot.size}", hot.size >= 4)
        for (mark in cool) {
            val h = hsv(mark.argb)
            assertTrue("cool granulation is warm orange/red, hue ${h[0]}", h[0] in 10f..45f)
            assertTrue("and extremely soft, alpha ${mark.alpha}", mark.alpha <= 0.30f)
        }
        for (mark in hot) {
            val h = hsv(mark.argb)
            assertTrue("hot granulation is pale gold, hue ${h[0]}", h[0] in 40f..62f)
            assertTrue("and bright, val ${h[2]}", h[2] >= 0.90f)
        }
        assertTrue("the Sun emits and takes no specular", BodyIdentities.SUN.lighting.emissive == 1f)
        assertEquals(0f, BodyIdentities.SUN.lighting.specularAlpha, 0f)

        // ---- Mercury: bright warm stone grey, low-contrast warm craters --------------------------
        val mercury = hsv(BodyCatalog.MERCURY.colorArgb)
        assertTrue("Mercury is a warm stone grey, hue ${mercury[0]}", mercury[0] in 20f..50f)
        assertTrue("Mercury is bright, val ${mercury[2]}", mercury[2] >= 0.70f)
        assertTrue("Mercury is desaturated, sat ${mercury[1]}", mercury[1] <= 0.20f)
        val mercuryMarks = BodyIdentities.MERCURY.marks
        assertTrue("a cratered field, found ${mercuryMarks.size}", mercuryMarks.size >= 6)
        val mercuryDark = mercuryMarks.filter { it.role == MarkRole.DARKEN }
        for (mark in mercuryDark) {
            val h = hsv(mark.argb)
            assertTrue("Mercury's craters are warm, hue ${h[0]}", h[0] in 20f..60f)
            assertTrue("and they darken, luminance ${relativeLuminance(mark.argb)}",
                relativeLuminance(mark.argb) < 0.75f)
            assertTrue("craters stay small", mark.rx <= 0.25f)
        }
        assertTrue(
            "a couple of lit crater walls, no more",
            mercuryMarks.count { it.role == MarkRole.LIGHTEN } in 1..3
        )

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
        val mercuryHue = mercuryDark.map { hsv(it.argb)[0] }.average()
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

        // ---- Earth: vivid blue ocean, sparse green land, ice caps, a few clouds ------------------
        val earth = hsv(BodyCatalog.EARTH.colorArgb)
        assertTrue("Earth is vivid blue, hue ${earth[0]}", earth[0] in 195f..225f)
        assertTrue("Earth is saturated, sat ${earth[1]}", earth[1] >= 0.60f)
        val earthMarks = BodyIdentities.EARTH.marks
        val land = earthMarks.filter { hsv(it.argb)[0] in 90f..160f }
        assertTrue("sparse stylised land masses, found ${land.size}", land.size in 5..8)
        assertTrue("land must read clearly", land.all { it.alpha >= 0.75f })
        assertTrue("land is painted, not multiplied", land.all { it.role == MarkRole.TINT })
        val clouds = earthMarks.filter {
            hsv(it.argb)[1] <= 0.05f && hsv(it.argb)[2] >= 0.99f && it.ry <= 0.05f
        }
        assertTrue("a few cloud streaks, found ${clouds.size}", clouds.size in 2..4)
        assertTrue("clouds stay subtle", clouds.all { it.alpha <= 0.45f })
        val caps = earthMarks.filter { abs(it.cy) >= 0.7f && hsv(it.argb)[2] >= 0.95f }
        assertEquals("both poles carry ice", 2, caps.size)

        // ---- Mars: vivid red-orange, rust regions, pale polar caps -------------------------------
        val mars = hsv(BodyCatalog.MARS.colorArgb)
        assertTrue("Mars is red-orange, hue ${mars[0]}", mars[0] in 8f..25f)
        assertTrue("Mars is vivid, not terracotta: sat ${mars[1]}", mars[1] >= 0.75f)
        assertTrue("Mars is bright, val ${mars[2]}", mars[2] >= 0.85f)
        assertNotEquals("Mars is no longer the old terracotta tone", 0xFFA6705CL, BodyCatalog.MARS.colorArgb)
        val marsMarks = BodyIdentities.MARS.marks
        assertTrue(
            "darker rust regions",
            marsMarks.count {
                hsv(it.argb)[0] in 5f..30f && relativeLuminance(it.argb) <= 0.30f
            } >= 4
        )
        assertTrue(
            "pale polar caps",
            marsMarks.count { hsv(it.argb)[2] >= 0.99f && hsv(it.argb)[1] <= 0.15f } >= 2
        )

        // ---- Jupiter: cream base, several curved belts, exactly one red spot --------------------
        val jupiter = hsv(BodyCatalog.JUPITER.colorArgb)
        assertTrue("Jupiter is cream/ivory, hue ${jupiter[0]}", jupiter[0] in 30f..55f)
        assertTrue("Jupiter is bright, val ${jupiter[2]}", jupiter[2] >= 0.85f)
        assertTrue("Jupiter is pale, sat ${jupiter[1]}", jupiter[1] <= 0.35f)
        val jupiterMarks = BodyIdentities.JUPITER.marks
        val belts = jupiterMarks.filter { it.kind == MarkKind.BAND && it.rx >= 0.5f }
        assertTrue("several curved belts, found ${belts.size}", belts.size >= 5)
        assertTrue("the belts stay soft", belts.all { it.alpha <= 0.60f })
        assertTrue(
            "belts multiply or screen: they are shading, not paint",
            belts.all { it.role != MarkRole.TINT }
        )
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
            asteroidMarks.count { it.role == MarkRole.DARKEN } >= 4
        )
        assertTrue("no noisy detail: bounded count ${asteroidMarks.size}", asteroidMarks.size <= 6)

        // ---- every body's detail is integrated into its material, not pasted on ------------------
        for (key in BodyIdentities.all.keys) {
            val identity = BodyIdentities.all.getValue(key)
            assertTrue(
                "$key: at least one feature must multiply the marble's light",
                identity.marks.any { it.role == MarkRole.DARKEN }
            )
        }

        // ---- Black hole and wormhole keep their treatment exactly --------------------------------
        assertEquals(0xFF0A0A0CL, BodyCatalog.BLACK_HOLE.colorArgb)
        assertEquals(GravityChrome.ACCENT_DARK, BodyCatalog.WORMHOLE.colorArgb)
        assertTrue(BodyIdentities.of("black_hole", BodyType.BLACK_HOLE).isEmpty)
        assertTrue(BodyIdentities.of("wormhole", BodyType.WORMHOLE_MOUTH).isEmpty)
        assertTrue(BodyIdentities.of("marble", BodyType.TEST_MARBLE).isEmpty)
    }

    @Test
    fun theLightIsPerBodyDirectionalAndNeverTheSameDotTwice() {
        val keys = BodyIdentities.all.keys
        val specs = keys.map { k ->
            val l = BodyIdentities.all.getValue(k).lighting
            l.specularRadius to l.specularAlpha
        }
        assertTrue(
            "the highlight must differ across bodies, found ${specs.distinct().size} distinct of " +
                "${keys.size}",
            specs.distinct().size >= 6
        )

        for (key in keys) {
            val identity = BodyIdentities.all.getValue(key)
            val l = identity.lighting
            assertTrue("$key: limb darkening is a physical u", l.limbDarkening in 0.30f..0.90f)
            assertTrue("$key: every body owns its limb tone", identity.limbArgb != 0L)
            if (l.specularAlpha > 0f) {
                assertTrue("$key: a highlight has a size", l.specularRadius in 0.05f..0.60f)
                assertTrue("$key: and sits on the lit side", l.specularCx < 0f && l.specularCy < 0f)
            }
        }

        // Bare rock: a small tight mineral glint. Cloud worlds: a broad dim sheen. The Sun: none.
        for (rock in listOf("mercury", "moon", "asteroid")) {
            val l = BodyIdentities.all.getValue(rock).lighting
            assertTrue("$rock: a rock's glint is small", l.specularRadius <= 0.13f)
            assertTrue("$rock: and bright", l.specularAlpha >= 0.18f)
            assertEquals("$rock: and has a defined edge", MarkEdge.FAIR, l.specularEdge)
            assertEquals("$rock: airless bodies carry no scattering crescent", 0f, l.atmosphere, 0f)
        }
        for (cloud in listOf("venus", "jupiter", "saturn", "uranus", "neptune")) {
            val l = BodyIdentities.all.getValue(cloud).lighting
            assertTrue("$cloud: a cloud world's sheen is broad", l.specularRadius >= 0.36f)
            assertTrue("$cloud: and dim", l.specularAlpha <= 0.12f)
            assertTrue("$cloud: and it has an atmosphere", l.atmosphere >= 0.85f)
        }
        assertTrue("Earth's thin air shows a little", BodyIdentities.EARTH.lighting.atmosphere in 0.5f..0.8f)
        assertTrue("Mars's thinner air shows less", BodyIdentities.MARS.lighting.atmosphere in 0.2f..0.4f)
        assertTrue("the Sun darkens hardest at the limb", BodyIdentities.SUN.lighting.limbDarkening >= 0.80f)
    }

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

        // The marble's own lighting: same off-centre gradient centre, same radius, same three stops.
        // The base is untouched; what changed around it is additive and per body.
        assertTrue("the base gradient is still lit from the top-left", canvas.contains("center = Offset(-r * 0.30f, -r * 0.34f)"))
        assertTrue("the base gradient still reaches 1.55r", canvas.contains("radius = r * 1.55f"))
        assertTrue("the base gradient still has three stops", canvas.contains("arrayOf(0f to light, 0.45f to tone, 1f to dark)"))

        // §5 layering, proved from the order the calls appear in: base -> belts -> caps -> the
        // lighting re-applied over them -> terminator -> limb -> rim -> highlight. Detail may never
        // sit on top of the shading, and the shading may never sit on top of the rim.
        val base = canvas.indexOf("cache.base[i]?.let { drawCircle(it, rr, Offset.Zero) }")
        val bands = canvas.indexOf("drawBands(cache, i, rr)")
        val caps = canvas.indexOf("drawBlobs(cache, i, rr)")
        val lighting = canvas.indexOf("alpha = BodyIdentities.LIGHTING_OVERLAY_ALPHA")
        val term = canvas.indexOf("cache.termBrush[i]?.let { drawCircle(it, rr, Offset.Zero) }")
        val limb = canvas.indexOf("cache.limbBrush[i]?.let { drawCircle(it, rr, Offset.Zero) }")
        val rim = canvas.indexOf("drawRimArc(cache, i, rr)")
        val spec = canvas.indexOf("drawSpecularGlint(cache, i, rr)")
        assertTrue("the base gradient is drawn", base > 0)
        assertTrue("belts come after the base", bands > base)
        assertTrue("caps come after the belts", caps > bands)
        assertTrue("the existing lighting is re-applied over the detail", lighting > caps)
        assertTrue("the terminator follows the lighting", term > lighting)
        assertTrue("and the limb fall-off follows the terminator", limb > term)
        assertTrue("the rim still follows the shading", rim > limb)
        assertTrue("and the highlight is last on the body", spec > rim)

        // The highlight is per body now — size, offset, strength and edge come from the identity,
        // which is what ends the identical-white-dot reading. The old shared dot is gone for good.
        assertTrue(canvas.contains("cache.specRadius[i] = lighting.specularRadius * r"))
        assertTrue(canvas.contains("cache.specAlpha[i] = lighting.specularAlpha * if (colors.isDark) 1f else 1.4f"))
        assertFalse("the flat shared specular dot is gone", canvas.contains("cache.specular["))
        // The rim is a lit crescent, not a bright ring all the way round.
        assertTrue(canvas.contains("drawArc(\n        color = cache.rim[i]"))
        // Features composite as albedo: multiply and screen, never as opaque decals.
        assertTrue(canvas.contains("BlendMode.Multiply"))
        assertTrue(canvas.contains("BlendMode.Screen"))

        // Rings: the far half under the sphere, the near half over it. That split is what makes them
        // pass behind and in front, and it is the only place rings are drawn.
        val farRings = canvas.indexOf("drawRingHalf(cache, i, rr, far = true)")
        val nearRings = canvas.indexOf("drawRingHalf(cache, i, rr, far = false)")
        assertTrue("the far ring half is painted before the body", farRings in 0 until base)
        assertTrue("the near ring half after the whole marble", nearRings > spec)

        // The two-layer cast shadow, geometry and all, is exactly as the previous pass left it.
        assertTrue(canvas.contains("Offset(rr * 0.38f - rr * 0.725f, rr * 0.34f - rr * 0.575f)"))
        assertTrue(canvas.contains("Size(rr * 1.45f, rr * 1.15f)"))
        assertTrue(canvas.contains("Offset(rr * 0.20f - rr * 0.40f, rr * 0.18f - rr * 0.40f)"))
        assertTrue(canvas.contains("Size(rr * 0.80f, rr * 0.80f)"))

        // Primitives only: no bitmaps, no image assets, no shader sources, no blur, no shadow layers.
        // Any mention at all is only allowed inside a comment that says it is not used.
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

    /** How much light a multiplied feature leaves: 1 - strength * (1 - luminance). */
    private fun effectiveDarken(alpha: Float, argb: Long): Float =
        1f - min(1f, alpha * BodyIdentities.DARKEN_STRENGTH) *
            (1f - relativeLuminance(argb))

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

package com.zig.gravity.ui.theme

import com.zig.gravity.physics.BodyType
import java.util.Random
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * §1 — the static procedural *identity* layer that sits between a body's base marble gradient and
 * its lighting.
 *
 * The marble shading itself is untouched: the same off-centre radial base, the same rim, the same
 * single specular dot. What this adds is a handful of flat, cached primitives per body — ellipses
 * and, for Saturn, ring arcs — so a planet reads as *that* planet at a glance instead of as a
 * coloured ball.
 *
 * Deliberate limits, all of them owner-directed:
 *  - every mark is an **axis-aligned ellipse expressed in fractions of the body radius**, so the
 *    layer scales with zoom for free and costs nothing per frame;
 *  - every mark is **inscribed in the unit disc** (proved by `GravityBodyIdentityTest`), so the
 *    renderer never needs a clip path. A canvas clip is not antialiased, and an aliased edge at the
 *    limb would be the first thing anyone saw;
 *  - shapes are **deterministic**: hand-authored where recognisability depends on placement
 *    (continents, belts, the red spot, the ring system) and seeded-scattered where it depends on
 *    irregularity (craters, granulation). The same body looks identical on every launch;
 *  - nothing here is animated, rotated, parallaxed or per-pixel. No bitmaps, no shaders, no assets.
 *
 * Pure data: this file imports no Compose type, so it is testable on the plain JVM.
 */
data class IdentityMark(
    /** Centre, in fractions of the body radius, body-local (0,0 = the body's centre, +y = down). */
    val cx: Float,
    val cy: Float,
    /** Half-extents, in fractions of the body radius. */
    val rx: Float,
    val ry: Float,
    /** Opaque 0xAARRGGBB tone. Alpha is carried separately so toning never disturbs it. */
    val argb: Long,
    val alpha: Float
) {
    /** True when the whole ellipse fits inside the body disc, which is what makes clipping moot. */
    fun isInsideUnitDisc(samples: Int = 256): Boolean {
        for (i in 0 until samples) {
            val t = 2.0 * Math.PI * i / samples
            val x = cx + rx * cos(t).toFloat()
            val y = cy + ry * sin(t).toFloat()
            if (x * x + y * y > 1f) return false
        }
        return true
    }
}

/**
 * One concentric band of a ring system, in fractions of the body radius.
 *
 * Rings are **purely visual**: they are drawn by the body renderer and never enter the simulation —
 * no mass, no collision radius, no selection geometry, no gravity. `BodyIdentity` is only ever read
 * inside the draw phase.
 */
data class RingBand(
    /** Centre-line radius of the band, as a multiple of the body radius (> 1, so it sits outside). */
    val radiusFraction: Float,
    /** Radial thickness of the band, as a fraction of the body radius. */
    val thicknessFraction: Float,
    val argb: Long,
    val alpha: Float
)

data class BodyIdentity(
    val marks: List<IdentityMark> = emptyList(),
    val rings: List<RingBand> = emptyList(),
    /**
     * Vertical squash of the ring ellipses (ry / rx) — how open the ring system looks from the
     * table. One tilt per body, shared by all of its bands.
     */
    val ringSquash: Float = DEFAULT_RING_SQUASH
) {
    val isEmpty: Boolean get() = marks.isEmpty() && rings.isEmpty()

    companion object {
        /** A body with no identity layer at all: the plain marble the renderer already drew. */
        val NONE = BodyIdentity()

        const val DEFAULT_RING_SQUASH = 0.30f
    }
}

object BodyIdentities {

    /**
     * Hard caps the renderer preallocates its flattened mark/ring arrays against. No body may exceed
     * them; `GravityBodyIdentityTest` asserts the whole catalog fits.
     */
    const val MAX_MARKS = 16
    const val MAX_RINGS = 4

    /**
     * Below this on-screen radius a body is a dot: its marks would be sub-pixel smears, so the
     * identity layer is skipped entirely. This is both a quality guard and the cheap path at deep
     * zoom-out, where the table can hold twenty dots.
     */
    const val MIN_RADIUS_PX = 8f

    /**
     * How strongly the body's **existing** base brush is re-applied over the marks (§5, step 3).
     *
     * The marble's lighting lives inside that base gradient, so drawing marks on top of it would
     * flatten the sphere. Re-drawing the very same cached brush at this alpha puts the top-left
     * highlight and the darkened limb back over the marks — no new light is invented, and the marks
     * end up *under* the shading exactly as §5 orders them.
     */
    const val LIGHTING_OVERLAY_ALPHA = 0.38f

    // ============================== individual bodies ==========================================

    /**
     * Sun — the bright gold palette stays; only a few extremely soft warm patches are added, so it
     * reads as a churning surface rather than a flat disc. No flares, no plasma, no animation.
     */
    val SUN = BodyIdentity(
        marks = scatter(
            seed = 29, count = 5, minRadius = 0.14f, maxRadius = 0.30f,
            argb = 0xFFF2801F, minAlpha = 0.10f, maxAlpha = 0.17f, reach = 0.52f
        ) + scatter(
            seed = 31, count = 2, minRadius = 0.10f, maxRadius = 0.18f,
            argb = 0xFFE4571C, minAlpha = 0.08f, maxAlpha = 0.12f, reach = 0.44f
        )
    )

    /**
     * Mercury — bright warm stone grey under a dense field of small, very low-contrast craters plus
     * two soft basins. Warmer and busier than the Moon, which is the point: the two must not be
     * confusable at 8 dp and 6 dp.
     */
    val MERCURY = BodyIdentity(
        marks = scatter(
            seed = 11, count = 6, minRadius = 0.05f, maxRadius = 0.12f,
            argb = 0xFF6E655A, minAlpha = 0.16f, maxAlpha = 0.26f, reach = 0.68f
        ) + listOf(
            ell(-0.24f, 0.30f, 0.20f, 0.17f, 0xFF7A7065, 0.13f),
            ell(0.32f, -0.26f, 0.17f, 0.15f, 0xFF7A7065, 0.11f)
        )
    )

    /**
     * Venus — pale cream under broad, soft golden cloud bands and one lazy swirl. No surface detail
     * at all: the real one is an opaque atmosphere, and it should look like it.
     */
    val VENUS = BodyIdentity(
        marks = listOf(
            band(-0.42f, 0.17f, 0xFFFBEFD0, 0.34f),
            band(-0.06f, 0.20f, 0xFFDDBB7F, 0.24f),
            band(0.34f, 0.18f, 0xFFF7E6BC, 0.30f),
            // The swirl: two overlapping pale ellipses, offset along the band, read as one vortex.
            ell(-0.30f, 0.06f, 0.20f, 0.09f, 0xFFFFF6E0, 0.26f),
            ell(-0.10f, 0.02f, 0.16f, 0.07f, 0xFFFFF6E0, 0.20f)
        )
    )

    /**
     * Earth — vivid ocean blue under sparse, stylised green land and three thin cloud streaks.
     *
     * The continents are clusters of two or three overlapping ellipses: enough to read as land
     * masses at 10 dp, nowhere near a world map. Land is drawn first so the clouds sit over it.
     */
    val EARTH = BodyIdentity(
        marks = listOf(
            // North-west continent.
            ell(-0.34f, -0.28f, 0.17f, 0.20f, 0xFF3E9E5A, 0.95f),
            ell(-0.20f, -0.10f, 0.11f, 0.14f, 0xFF4FAE66, 0.90f),
            // North-east landmass.
            ell(0.28f, -0.34f, 0.20f, 0.14f, 0xFF3E9E5A, 0.95f),
            ell(0.44f, -0.14f, 0.10f, 0.12f, 0xFF4FAE66, 0.88f),
            // Southern continent.
            ell(-0.06f, 0.42f, 0.16f, 0.13f, 0xFF379152, 0.92f),
            ell(0.14f, 0.30f, 0.09f, 0.10f, 0xFF4FAE66, 0.85f),
            // One small island, so the ocean does not read as two blobs.
            ell(-0.52f, 0.10f, 0.07f, 0.06f, 0xFF3E9E5A, 0.80f),
            // Cloud streaks.
            ell(-0.10f, -0.56f, 0.32f, 0.055f, 0xFFFFFFFF, 0.30f),
            ell(0.20f, 0.10f, 0.28f, 0.050f, 0xFFFFFFFF, 0.26f),
            ell(-0.26f, 0.30f, 0.22f, 0.045f, 0xFFFFFFFF, 0.22f)
        )
    )

    /**
     * Moon — bright cool white under three soft maria and a few crisp craters, two of them large
     * enough to be recognisable depressions (a dark floor with a lit inner rim towards the light).
     * The marble shading stays plainly visible on top.
     */
    val MOON = BodyIdentity(
        marks = listOf(
            ell(-0.26f, -0.22f, 0.24f, 0.19f, 0xFF8E97A8, 0.30f),
            ell(0.20f, -0.06f, 0.18f, 0.20f, 0xFF98A1B1, 0.26f),
            ell(-0.04f, 0.34f, 0.20f, 0.14f, 0xFF8E97A8, 0.24f)
        ) +
            crater(0.30f, 0.40f, 0.13f) +
            crater(-0.42f, 0.12f, 0.11f) +
            listOf(
                ell(0.02f, -0.52f, 0.07f, 0.07f, 0xFF9BA4B4, 0.28f),
                ell(-0.14f, 0.62f, 0.06f, 0.06f, 0xFF9BA4B4, 0.24f)
            )
    )

    /**
     * Mars — the one the owner singled out: vivid red-orange, not terracotta. Darker rust regions,
     * two soft dark patches, a pale southern polar cap and one lighter dust region.
     */
    val MARS = BodyIdentity(
        marks = listOf(
            ell(-0.28f, 0.14f, 0.24f, 0.16f, 0xFF9C3315, 0.38f),
            ell(0.30f, -0.12f, 0.20f, 0.18f, 0xFFA83A18, 0.32f),
            ell(0.02f, 0.44f, 0.22f, 0.11f, 0xFF93300F, 0.30f),
            ell(-0.10f, -0.36f, 0.16f, 0.11f, 0xFF7E2A12, 0.22f),
            ell(0.46f, 0.24f, 0.12f, 0.10f, 0xFF7E2A12, 0.20f),
            // Southern polar cap: small, pale, unmistakable.
            ell(0.02f, 0.74f, 0.19f, 0.09f, 0xFFFFF1E6, 0.55f),
            // One brighter dust region, for relief.
            ell(-0.42f, -0.14f, 0.14f, 0.10f, 0xFFFF9C6A, 0.20f)
        )
    )

    /**
     * Jupiter — cream/ivory under alternating warm belts and pale zones, with one Great Red Spot in
     * the southern belt. Soft, wide, low-count: six belts is all a 16 dp marble can carry.
     */
    val JUPITER = BodyIdentity(
        marks = listOf(
            band(-0.64f, 0.09f, 0xFFD8B489, 0.42f),
            band(-0.38f, 0.12f, 0xFFC08A55, 0.55f),
            band(-0.12f, 0.13f, 0xFFF6EBD2, 0.50f),
            band(0.16f, 0.14f, 0xFFB87B45, 0.58f),
            band(0.44f, 0.11f, 0xFFDCC09A, 0.45f),
            band(0.68f, 0.08f, 0xFFC79A6B, 0.34f),
            // The spot, drawn last so it sits on top of the belt that carries it.
            ell(0.28f, 0.22f, 0.19f, 0.10f, 0xFFC24A32, 0.80f)
        )
    )

    /**
     * Saturn — pale warm cream under three very soft belts, plus the ring system that makes it
     * Saturn. Four bands with a genuine gap where the Cassini division sits.
     */
    val SATURN = BodyIdentity(
        marks = listOf(
            band(-0.36f, 0.14f, 0xFFD9C08A, 0.34f),
            band(0.04f, 0.16f, 0xFFF8EED2, 0.40f),
            band(0.44f, 0.13f, 0xFFD3B87F, 0.32f)
        ),
        rings = listOf(
            // C ring: faint and innermost.
            RingBand(1.30f, 0.050f, 0xFFE8DCBB, 0.20f),
            // B ring: the bright, wide one.
            RingBand(1.48f, 0.110f, 0xFFF7EBCC, 0.46f),
            // (Cassini division — the empty span between 1.55 and 1.61.)
            // A ring: outside the gap.
            RingBand(1.68f, 0.075f, 0xFFF0E1B6, 0.34f),
            // F ring: a hair of light outside everything.
            RingBand(1.82f, 0.022f, 0xFFE3D5AE, 0.16f)
        ),
        ringSquash = 0.30f
    )

    /**
     * Uranus — pale cyan, almost featureless: two extremely subtle belts and a faint brightening
     * over the pole. Distinct from Earth by hue and by having no land or cloud contrast at all.
     */
    val URANUS = BodyIdentity(
        marks = listOf(
            band(-0.24f, 0.18f, 0xFFCBF2EE, 0.30f),
            band(0.28f, 0.16f, 0xFF8CCFCB, 0.26f),
            band(-0.68f, 0.11f, 0xFFDDF7F4, 0.22f)
        )
    )

    /**
     * Neptune — saturated cobalt, visibly deeper than Uranus: a darker belt, a brighter one, a soft
     * dark spot and one streak of high white cloud, which is what the real one is famous for.
     */
    val NEPTUNE = BodyIdentity(
        marks = listOf(
            band(-0.32f, 0.16f, 0xFF6A83E4, 0.30f),
            band(0.22f, 0.18f, 0xFF2A3E9E, 0.34f),
            band(0.62f, 0.10f, 0xFF7C93EA, 0.20f),
            // Great Dark Spot.
            ell(0.16f, -0.30f, 0.16f, 0.10f, 0xFF1B2A6B, 0.30f),
            // Bright cloud streak, suggested by two overlapping ellipses on a diagonal.
            ell(-0.26f, -0.04f, 0.19f, 0.055f, 0xFFEAF0FF, 0.30f),
            ell(-0.06f, 0.08f, 0.15f, 0.045f, 0xFFEAF0FF, 0.22f)
        )
    )

    /**
     * Asteroid — keeps the warm rocky/terracotta family it already had; only a few irregular darker
     * patches and craters, so it reads as a battered rock rather than a small Mars.
     */
    val ASTEROID = BodyIdentity(
        marks = scatter(
            seed = 23, count = 4, minRadius = 0.08f, maxRadius = 0.18f,
            argb = 0xFF8A5230, minAlpha = 0.22f, maxAlpha = 0.32f, reach = 0.60f
        ) + listOf(
            ell(0.28f, -0.30f, 0.13f, 0.10f, 0xFF7A4626, 0.26f)
        )
    )

    /**
     * A planet with no catalog key. It already borrows Earth's blue through `colorOf`, but Earth's
     * continents would be invented detail on an object nobody identified, so it gets neutral
     * banding instead: a planet, not a place.
     */
    val GENERIC_PLANET = BodyIdentity(
        marks = listOf(
            band(-0.30f, 0.16f, 0xFFDCE8F5, 0.22f),
            band(0.18f, 0.18f, 0xFF2E4A6E, 0.24f),
            band(0.60f, 0.10f, 0xFFCFDCEA, 0.16f)
        )
    )

    /**
     * Every identity the catalog knows, keyed by the catalog key. Test marble, black hole and
     * wormhole are deliberately absent: they resolve to [BodyIdentity.NONE] and keep exactly the
     * treatment they already had.
     */
    val all: Map<String, BodyIdentity> = mapOf(
        "sun" to SUN,
        "mercury" to MERCURY,
        "venus" to VENUS,
        "earth" to EARTH,
        "moon" to MOON,
        "mars" to MARS,
        "jupiter" to JUPITER,
        "saturn" to SATURN,
        "uranus" to URANUS,
        "neptune" to NEPTUNE,
        "asteroid" to ASTEROID
    )

    /**
     * The identity for a body, mirroring `BodyCatalog.colorOf` exactly: a catalogued entry answers
     * for itself, and a body created without a key falls back to something sane for its type.
     *
     * Black holes and wormhole mouths return [BodyIdentity.NONE] — their visual treatment is their
     * whole meaning, and §2 forbids texturing them.
     */
    fun of(key: String?, type: BodyType): BodyIdentity {
        val byKey = if (key == null) null else all[key]
        if (byKey != null) return byKey
        return when (type) {
            BodyType.SUN -> SUN
            BodyType.PLANET -> GENERIC_PLANET
            BodyType.MOON -> MOON
            BodyType.ASTEROID -> ASTEROID
            BodyType.TEST_MARBLE -> BodyIdentity.NONE
            BodyType.BLACK_HOLE -> BodyIdentity.NONE
            BodyType.WORMHOLE_MOUTH -> BodyIdentity.NONE
        }
    }

    // ============================== generators ==================================================

    /** One elliptical mark, in body-radius fractions. */
    private fun ell(
        cx: Float,
        cy: Float,
        rx: Float,
        ry: Float,
        argb: Long,
        alpha: Float
    ) = IdentityMark(cx, cy, rx, ry, argb, alpha)

    /**
     * A soft atmospheric belt at height [cy]: an ellipse inscribed in the sphere's chord there, so
     * it tapers towards the limb the way a real belt does and never spills outside the disc.
     * [reach] pulls the ends in from the limb; the containment proof in the test covers every one.
     */
    private fun band(
        cy: Float,
        ry: Float,
        argb: Long,
        alpha: Float,
        reach: Float = 0.93f
    ): IdentityMark {
        val chord = sqrt((1.0 - cy.toDouble() * cy.toDouble()).coerceAtLeast(0.0))
        return IdentityMark(0f, cy, (chord * reach).toFloat(), ry, argb, alpha)
    }

    /**
     * A recognisable crater: a dark floor plus a smaller lit floor offset towards the light at the
     * top-left, which is all it takes for a depression to read as a depression.
     */
    private fun crater(cx: Float, cy: Float, r: Float): List<IdentityMark> = listOf(
        ell(cx, cy, r, r * 0.94f, 0xFF9BA4B4, 0.34f),
        ell(cx - r * 0.22f, cy - r * 0.24f, r * 0.52f, r * 0.48f, 0xFFFFFFFF, 0.30f)
    )

    /**
     * A deterministic scatter of soft elliptical marks, for surfaces whose character is irregularity
     * rather than placement: craters, granulation, battered rock.
     *
     * Polar sampling with a `sqrt` radius keeps the distribution even over the area, and [reach]
     * bounds the centre distance so that centre + mark radius never exceeds the body disc. Public
     * for the same reason `TableSurfaces.generateStarDots` is: determinism is a claim that has to be
     * checkable, so the same seed must produce the same marks in the same order, every time.
     */
    fun scatter(
        seed: Int,
        count: Int,
        minRadius: Float,
        maxRadius: Float,
        argb: Long,
        minAlpha: Float,
        maxAlpha: Float,
        reach: Float
    ): List<IdentityMark> {
        val random = Random(seed.toLong())
        val out = ArrayList<IdentityMark>(count)
        repeat(count) {
            val rho = sqrt(random.nextDouble()) * reach.toDouble()
            val theta = random.nextDouble() * 2.0 * Math.PI
            val r = minRadius + (maxRadius - minRadius) * random.nextFloat()
            val alpha = minAlpha + (maxAlpha - minAlpha) * random.nextFloat()
            // Irregular, never a perfect circle: the aspect is jittered per mark.
            val aspect = 0.72f + 0.5f * random.nextFloat()
            out += IdentityMark(
                cx = (rho * cos(theta)).toFloat(),
                cy = (rho * sin(theta)).toFloat(),
                rx = r,
                ry = r * aspect,
                argb = argb,
                alpha = alpha
            )
        }
        return out
    }
}

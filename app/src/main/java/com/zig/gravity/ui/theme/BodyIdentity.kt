package com.zig.gravity.ui.theme

import com.zig.gravity.physics.BodyType
import java.util.Random
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * §1 — the static procedural *identity* layer that gives every body its material.
 *
 * The rendering quality pass rebuilt this layer around one idea: a celestial body is a sphere, so
 * its features are authored **on the sphere** and projected down, never pasted onto the disc.
 * [SphereProjection] does that arithmetic once per body per visual epoch; the draw phase only
 * walks the resulting floats.
 *
 * Three compositing roles, chosen per feature, are what make detail read as *material* instead of
 * as a decal:
 *  - [MarkRole.DARKEN] multiplies the surface (a crater floor, a rust region, a belt) so it
 *    inherits whatever light the marble's shading already put there;
 *  - [MarkRole.LIGHTEN] screens onto it (polar caps, clouds, lit crater walls, pale zones);
 *  - [MarkRole.TINT] paints over it, reserved for a genuinely different material — Earth's land.
 *
 * Deliberate limits, all of them owner-directed:
 *  - every mark is deterministic — hand-authored where recognisability depends on placement
 *    (continents, belts, the red spot, the ring system) and seeded where it depends on
 *    irregularity (craters, granulation). The same body looks identical on every launch;
 *  - every projected feature stays inside the body disc, proved by `GravityBodyIdentityTest`, so
 *    the renderer never needs a clip path. A canvas clip is not antialiased, and an aliased edge
 *    at the limb would be the first thing anyone saw;
 *  - nothing here is animated, rotated by time, parallaxed or per-pixel. No bitmaps, no assets.
 *
 * Pure data and pure maths: this file imports no Compose type, so it is testable on the plain JVM.
 */

/** How hard a feature's edge falls off. SOFT is atmosphere, CRISP is a coastline or a crater rim. */
enum class MarkEdge { SOFT, FAIR, CRISP }

/** How a feature composites with the light already on the marble. See the file header. */
enum class MarkRole { TINT, DARKEN, LIGHTEN }

/** BLOB is a spherical cap; BAND is a latitude belt drawn as a curved arc. */
enum class MarkKind { BLOB, BAND }

data class IdentityMark(
    /**
     * Centre, in fractions of the body radius, of the feature's position on the *visible*
     * hemisphere (0,0 = the sub-observer point, +y = down). For a band this is the height of the
     * belt's equator-ward apex.
     */
    val cx: Float,
    val cy: Float,
    /** Half-extents in fractions of the body radius: the cap's angular size, tangential then radial. */
    val rx: Float,
    val ry: Float,
    /** Opaque 0xAARRGGBB. For DARKEN it is a multiplier, for LIGHTEN an added light, for TINT albedo. */
    val argb: Long,
    /** Authored subtlety. The renderer scales it into compositing strength per role. */
    val alpha: Float,
    val edge: MarkEdge = MarkEdge.FAIR,
    val role: MarkRole = MarkRole.DARKEN,
    val kind: MarkKind = MarkKind.BLOB
) {
    /** True when the whole authored ellipse fits inside the body disc. Blobs only; bands are arcs. */
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

/**
 * The part of a body's look that is *light*, not material: where its highlight sits, how big and
 * how tight it is, how strongly its limb falls away, and whether it is a light source at all.
 *
 * Varying this per body is what removes the "same white dot on every planet" reading: a bare rock
 * gets a small tight mineral glint, a cloud world a broad dim sheen, and the Sun none at all.
 */
data class BodyLighting(
    /** Highlight radius as a fraction of the body radius; 0 means no specular at all. */
    val specularRadius: Float,
    val specularCx: Float,
    val specularCy: Float,
    val specularAlpha: Float,
    val specularEdge: MarkEdge = MarkEdge.SOFT,
    /** u in the linear limb-darkening law I(mu)/I(1) = 1 - u(1 - mu). */
    val limbDarkening: Float,
    /** 1 for a body that emits its own light: corona, bright core, no specular. */
    val emissive: Float = 0f,
    /** Strength of the thin scattering crescent on the lit limb. 0 for airless bodies. */
    val atmosphere: Float = 0f
) {
    companion object {
        /** The untextured marble's lighting: a modest rock-like glint and a gentle limb. */
        val DEFAULT = BodyLighting(
            specularRadius = 0.12f, specularCx = -0.36f, specularCy = -0.40f,
            specularAlpha = 0.26f, specularEdge = MarkEdge.FAIR, limbDarkening = 0.50f
        )
    }
}

data class BodyIdentity(
    val marks: List<IdentityMark> = emptyList(),
    val rings: List<RingBand> = emptyList(),
    /**
     * Vertical squash of the ring ellipses (ry / rx) — how open the ring system looks from the
     * table. One tilt per body, shared by all of its bands.
     */
    val ringSquash: Float = DEFAULT_RING_SQUASH,
    val lighting: BodyLighting = BodyLighting.DEFAULT,
    /** Authoritative limb/terminator tone, 0xAARRGGBB; 0 derives it from the body's own deep stop. */
    val limbArgb: Long = 0L,
    /**
     * sin of the angle the body's axis leans toward the viewer. Bands and ring systems share it,
     * so every banded body on the table curves the same way.
     */
    val axisTilt: Float = DEFAULT_AXIS_TILT
) {
    val isEmpty: Boolean get() = marks.isEmpty() && rings.isEmpty()

    companion object {
        /** A body with no identity layer at all: the plain marble the renderer already drew. */
        val NONE = BodyIdentity()

        const val DEFAULT_RING_SQUASH = 0.30f

        /** Saturn's rings and every latitude belt lean by the same amount, on purpose. */
        const val DEFAULT_AXIS_TILT = 0.30f
    }
}

/** A blob after projection: the ellipse the draw phase actually paints, plus its rotation. */
data class ProjectedBlob(
    val cx: Float,
    val cy: Float,
    /** Longer semi-axis of the projected ellipse, in body-radius fractions. */
    val major: Float,
    /** Shorter semi-axis: the one the sphere's foreshortening squeezed. */
    val minor: Float,
    /** Direction of the major axis, in degrees, from the +x axis. */
    val degrees: Float,
    /** cos of the angle between the feature and the sub-observer point: its foreshortening. */
    val nz: Float
)

/** A band after projection: the ellipse its latitude circle traces, and the visible arc of it. */
data class ProjectedBand(
    val a: Float,
    val b: Float,
    /** y of the ellipse centre; the visible arc bulges away from it toward the equator. */
    val yc: Float,
    val startDegrees: Float,
    val sweepDegrees: Float
)

/**
 * The orthographic sphere arithmetic, in one place, on the plain JVM.
 *
 * A small circle on a sphere projects to an ellipse whose tangential semi-axis keeps its size and
 * whose radial semi-axis is foreshortened by cos(theta) — the standard orthographic result — and a
 * latitude circle projects to an ellipse of semi-axes (cos lambda, cos lambda * sin tau) whose
 * front half is the visible belt. Both facts are what make features look wrapped rather than
 * pasted. Everything here is a closed form evaluated once per visual epoch.
 */
object SphereProjection {

    /** The one light of the scene, matching the marble's off-centre gradient. Unit length. */
    const val LIGHT_X = -0.45f
    const val LIGHT_Y = -0.50f
    const val LIGHT_Z = 0.74f

    /** Unit axis in the image plane from the sub-solar limb to the anti-solar limb. */
    val AXIS_X: Float
    val AXIS_Y: Float
    private val lightXY: Float

    init {
        lightXY = sqrt(LIGHT_X * LIGHT_X + LIGHT_Y * LIGHT_Y)
        AXIS_X = -LIGHT_X / lightXY
        AXIS_Y = -LIGHT_Y / lightXY
    }

    /**
     * Wrap an authored cap onto the sphere: contract it along the radial direction by the local
     * foreshortening, rotate it to align with that direction, and pull its centre in by cos(alpha)
     * the way a real cap's projection sits inside its own chord.
     */
    fun projectBlob(cx: Float, cy: Float, rx: Float, ry: Float): ProjectedBlob {
        val rho = sqrt(cx * cx + cy * cy)
        if (rho < 1e-6f) return ProjectedBlob(0f, 0f, max(rx, ry), min(rx, ry), 0f, 1f)
        val nz = sqrt(max(0f, 1f - rho * rho))
        val phi = atan2(cy, cx)
        val c = cos(phi)
        val s = sin(phi)
        val a11 = nz * c * c + s * s
        val a22 = nz * s * s + c * c
        val a12 = (nz - 1f) * c * s
        // M = R(phi) diag(nz,1) R(-phi) diag(rx,ry); its singular values are the semi-axes.
        val p = a11 * rx
        val q = a12 * ry
        val r = a12 * rx
        val t = a22 * ry
        val e = p * p + q * q
        val f = p * r + q * t
        val g = r * r + t * t
        val tr = e + g
        val det = e * g - f * f
        val disc = sqrt(max(0f, tr * tr / 4f - det))
        val major = sqrt(tr / 2f + disc)
        val minor = sqrt(max(1e-9f, tr / 2f - disc))
        val degrees = Math.toDegrees(0.5 * atan2(2.0 * f, (e - g).toDouble())).toFloat()
        val sa = min(1f, max(rx, ry))
        val pull = cos(asin(sa))
        return ProjectedBlob(cx * pull, cy * pull, major, minor, degrees, nz)
    }

    /** How far inside the silhouette a belt's stroked ends must stay, in squared-radius units. */
    private const val LIMB_MARGIN = 0.004f

    /**
     * The belt stroke half-width (body-radius fractions) that still fits between the belt's apex
     * and the limb, so the *complete* primitive — arc plus its stroke on both sides — is inside
     * the visible circle before the draw phase ever sees it. Never exceeds the wanted tiled width.
     */
    fun bandHalfWidth(cy: Float, tilt: Float, sliceWidth: Float): Float {
        val tau = asin(tilt.coerceIn(0.05f, 0.85f))
        val lambda = tau - asin(cy.coerceIn(-0.999f, 0.999f))
        val b = cos(lambda) * sin(tau)
        val yc = -sin(lambda) * cos(tau)
        val want = sliceWidth * 1.15f / 2f
        return min(want, max(0f, 1f - LIMB_MARGIN - abs(yc + b)))
    }

    /**
     * Pulls a projected cap radially inward until its *whole* ellipse — centre plus major
     * semi-axis, at any rotation — sits inside the silhouette by [LIMB_MARGIN]. Checking only the
     * centre is not containment; |centre| + major is the exact worst case over the outline.
     */
    fun containBlob(pr: ProjectedBlob): ProjectedBlob {
        val d = sqrt(pr.cx * pr.cx + pr.cy * pr.cy)
        val limit = 1f - LIMB_MARGIN - pr.major
        if (d <= limit || d <= 0f) return pr
        val f = max(0f, limit) / d
        return pr.copy(cx = pr.cx * f, cy = pr.cy * f)
    }

    /**
     * The visible arc of a latitude belt, solved so that the *stroke* never crosses the silhouette.
     *
     * The belt's circle meets the limb exactly where z = 0, and there the radius is stationary in
     * the ellipse parameter, so no fixed angular pad can keep a stroke of finite width inside.
     * Instead each end is bisected inward until the whole stroke half-width sits inside the disc by
     * [LIMB_MARGIN] — exact, deterministic and evaluated once per visual epoch. `GravityBodyIdentityTest`
     * samples every catalogued belt's stroked outline and proves containment.
     *
     * [strokeHalfWidth] is half the stroke, in body-radius fractions.
     */
    fun projectBand(cy: Float, tilt: Float, strokeHalfWidth: Float): ProjectedBand {
        val tau = asin(tilt.coerceIn(0.05f, 0.85f))
        val lambda = tau - asin(cy.coerceIn(-0.999f, 0.999f))
        val a = cos(lambda)
        val b = a * sin(tau)
        val yc = -sin(lambda) * cos(tau)
        val sinT0 = (-tan(lambda) * tan(tau)).coerceIn(-1f, 1f)
        val t0 = asin(sinT0)
        val mid = (Math.PI / 2.0).toFloat()
        val start = solveBandEnd(t0, mid, a, b, yc, strokeHalfWidth)
        // The stroked norm is symmetric about t = PI/2, so the far end mirrors the near one.
        val end = Math.PI.toFloat() - start
        return ProjectedBand(
            a = a,
            b = b,
            yc = yc,
            startDegrees = Math.toDegrees(start.toDouble()).toFloat(),
            sweepDegrees = Math.toDegrees((end - start).toDouble()).toFloat()
        )
    }

    /** Squared distance from the body centre of the worst point of the stroked arc at parameter t. */
    private fun strokedNorm2(t: Float, a: Float, b: Float, yc: Float, hw: Float): Float {
        val x = a * cos(t)
        val y = yc + b * sin(t)
        val dx = -a * sin(t)
        val dy = b * cos(t)
        val nl = sqrt(dx * dx + dy * dy).coerceAtLeast(1e-6f)
        val nx = -dy / nl
        val ny = dx / nl
        val x1 = x + nx * hw
        val y1 = y + ny * hw
        val x2 = x - nx * hw
        val y2 = y - ny * hw
        return max(x1 * x1 + y1 * y1, x2 * x2 + y2 * y2)
    }

    /** Smallest parameter in [lo, hi] whose stroked cross-section lies inside the disc. */
    private fun solveBandEnd(
        lo: Float,
        hi: Float,
        a: Float,
        b: Float,
        yc: Float,
        hw: Float
    ): Float {
        val limit = 1f - LIMB_MARGIN
        if (strokedNorm2(hi, a, b, yc, hw) > limit) return hi
        var prev = lo
        var inside = hi
        val steps = 60
        for (i in 1..steps) {
            val t = lo + (hi - lo) * i / steps
            if (strokedNorm2(t, a, b, yc, hw) <= limit) {
                inside = t
                break
            }
            prev = t
        }
        var loB = prev
        var hiB = inside
        repeat(28) {
            val m = (loB + hiB) / 2f
            if (strokedNorm2(m, a, b, yc, hw) <= limit) hiB = m else loB = m
        }
        return hiB
    }

    /**
     * Alpha ramp of the linear limb-darkening law, sampled at fixed radii so the renderer can build
     * one radial gradient from it. mu = sqrt(1 - rho^2) is the cosine of the viewing angle.
     */
    fun limbAlphas(u: Float, samples: Int = 7): FloatArray {
        val out = FloatArray(samples)
        for (i in 0 until samples) {
            val rho = i / (samples - 1f)
            val mu = sqrt(max(0f, 1f - rho * rho))
            out[i] = (u * (1f - mu).pow18()).coerceIn(0f, 1f)
        }
        return out
    }

    /**
     * Alpha ramp of the directional terminator, sampled along the light axis: at parameter q the
     * surface normal is (axis * q, sqrt(1 - q^2)), so the ramp carries the sphere's curvature with
     * it. A linear gradient along [AXIS_X]/[AXIS_Y] then reproduces it on the disc.
     */
    fun terminatorAlphas(samples: Int = 9): FloatArray {
        val out = FloatArray(samples)
        for (i in 0 until samples) {
            val q = -1f + 2f * i / (samples - 1f)
            val illum = -lightXY * q + LIGHT_Z * sqrt(max(0f, 1f - q * q))
            val a = ((0.85f - illum) / 1.85f).coerceIn(0f, 1f)
            out[i] = a.pow115()
        }
        return out
    }

    private fun Float.pow18(): Float = Math.pow(this.toDouble(), 1.8).toFloat()
    private fun Float.pow115(): Float = Math.pow(this.toDouble(), 1.15).toFloat()
}

object BodyIdentities {

    /**
     * Hard caps the renderer preallocates its flattened mark/ring arrays against. No body may exceed
     * them; `GravityBodyIdentityTest` asserts the whole catalog fits.
     */
    const val MAX_MARKS = 16
    const val MAX_RINGS = 4

    /** How many latitude slices one belt is drawn with, so its cross-section stays smooth. */
    const val BAND_SLICES = 9

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
     * end up *under* the shading exactly as §5 orders them. Kept low now that each feature carries
     * its own illumination: the shading layers below do the rest.
     */
    const val LIGHTING_OVERLAY_ALPHA = 0.16f

    /**
     * Authored subtlety of a DARKEN mark is a fraction of "how much darker"; compositing strength
     * needs more than that because a multiply at alpha a only reaches 1 - a(1 - m). LIGHTEN alphas
     * are already additive strengths and pass through untouched.
     */
    const val DARKEN_STRENGTH = 2.4f
    const val LIGHTEN_STRENGTH = 1.0f

    /** Strength of the symmetric limb fall-off and of the directional terminator, respectively. */
    const val LIMB_STRENGTH = 0.74f
    const val TERMINATOR_STRENGTH = 0.68f

    /** The warm colour of the Sun's corona, the only body that glows past its own limb. */
    const val SUN_CORONA_ARGB = 0xFFFFB43CL

    /** Relative alpha of the five belt slices: a smooth cross-section, no staircase. */
    val BAND_PROFILE = floatArrayOf(0.06f, 0.28f, 0.62f, 0.90f, 1.00f, 0.90f, 0.62f, 0.28f, 0.06f)

    // ============================== individual bodies ==========================================

    /**
     * Sun — a light source, not a marble: a warm corona outside the limb, a hot core inside, the
     * strongest limb darkening on the table (u = 0.85, the visible-light solar value) and two
     * families of granulation, hot and cool, so the photosphere churns instead of shining flat.
     */
    val SUN = BodyIdentity(
        marks = scatter(
            seed = 41, count = 5, minRadius = 0.11f, maxRadius = 0.21f,
            argb = 0xFFDE9C62, minAlpha = 0.16f, maxAlpha = 0.26f, reach = 0.60f,
            edge = MarkEdge.SOFT, role = MarkRole.DARKEN
        ) + scatter(
            seed = 43, count = 4, minRadius = 0.07f, maxRadius = 0.14f,
            argb = 0xFFD08A50, minAlpha = 0.14f, maxAlpha = 0.24f, reach = 0.50f,
            edge = MarkEdge.SOFT, role = MarkRole.DARKEN
        ) + listOf(
            ell(0.34f, 0.30f, 0.16f, 0.11f, 0xFFD08A50, 0.18f, MarkEdge.SOFT, MarkRole.DARKEN),
            ell(-0.40f, 0.24f, 0.13f, 0.09f, 0xFFD08A50, 0.14f, MarkEdge.SOFT, MarkRole.DARKEN)
        ) + scatter(
            seed = 47, count = 5, minRadius = 0.06f, maxRadius = 0.12f,
            argb = 0xFFFCE9A8, minAlpha = 0.14f, maxAlpha = 0.24f, reach = 0.56f,
            edge = MarkEdge.SOFT, role = MarkRole.LIGHTEN
        ),
        lighting = BodyLighting(
            specularRadius = 0f, specularCx = 0f, specularCy = 0f, specularAlpha = 0f,
            limbDarkening = 0.85f, emissive = 1f
        ),
        limbArgb = 0xFFB4500E
    )

    /**
     * Mercury — warm stone grey under a field of small crisp craters, each a multiplied floor with
     * a screened lit inner wall toward the light, plus two soft tonal regions. Busy and warm, so it
     * can never be mistaken for the Moon at 8 dp.
     */
    val MERCURY = BodyIdentity(
        marks = scatter(
            seed = 11, count = 5, minRadius = 0.045f, maxRadius = 0.095f,
            argb = 0xFFC2BAB0, minAlpha = 0.28f, maxAlpha = 0.40f, reach = 0.70f,
            edge = MarkEdge.CRISP, role = MarkRole.DARKEN
        ) + listOf(
            ell(-0.26f, 0.30f, 0.155f, 0.135f, 0xFFB4ACA1, 0.32f, MarkEdge.CRISP, MarkRole.DARKEN),
            ell(-0.292f, 0.252f, 0.052f, 0.044f, 0xFFF6F2EA, 0.26f, MarkEdge.FAIR, MarkRole.LIGHTEN),
            ell(0.34f, -0.24f, 0.125f, 0.110f, 0xFFB4ACA1, 0.28f, MarkEdge.CRISP, MarkRole.DARKEN),
            ell(0.302f, -0.276f, 0.046f, 0.040f, 0xFFF6F2EA, 0.22f, MarkEdge.FAIR, MarkRole.LIGHTEN),
            ell(0.06f, -0.52f, 0.085f, 0.070f, 0xFFCCC4B9, 0.22f, MarkEdge.SOFT, MarkRole.DARKEN),
            ell(-0.50f, -0.16f, 0.070f, 0.060f, 0xFFD2CAC0, 0.18f, MarkEdge.SOFT, MarkRole.DARKEN)
        ),
        lighting = BodyLighting(
            specularRadius = 0.115f, specularCx = -0.36f, specularCy = -0.40f,
            specularAlpha = 0.26f, specularEdge = MarkEdge.FAIR, limbDarkening = 0.55f
        ),
        limbArgb = 0xFF5C5348
    )

    /**
     * Venus — an opaque pale atmosphere: four very soft cloud belts and one lazy vortex of light.
     * No surface at all, exactly like the real one.
     */
    val VENUS = BodyIdentity(
        marks = listOf(
            ell(-0.34f, 0.10f, 0.19f, 0.085f, 0xFFFFF6DE, 0.26f, MarkEdge.SOFT, MarkRole.LIGHTEN),
            ell(-0.08f, 0.02f, 0.15f, 0.070f, 0xFFFFF1D0, 0.20f, MarkEdge.SOFT, MarkRole.LIGHTEN),
            ell(0.30f, -0.30f, 0.13f, 0.060f, 0xFFE3C48C, 0.24f, MarkEdge.SOFT, MarkRole.DARKEN),
            band(-0.52f, 0.105f, 0xFFFBF0D2, 0.30f, MarkRole.LIGHTEN),
            band(-0.14f, 0.125f, 0xFFE0C084, 0.28f, MarkRole.DARKEN),
            band(0.26f, 0.115f, 0xFFF9EAC4, 0.30f, MarkRole.LIGHTEN),
            band(0.62f, 0.085f, 0xFFDCBA7E, 0.24f, MarkRole.DARKEN)
        ),
        lighting = BodyLighting(
            specularRadius = 0.36f, specularCx = -0.26f, specularCy = -0.32f,
            specularAlpha = 0.11f, specularEdge = MarkEdge.SOFT,
            limbDarkening = 0.42f, atmosphere = 0.85f
        ),
        limbArgb = 0xFFA9834E
    )

    /**
     * Earth — the one body with a different material on it: tinted land over a multiplied deep
     * ocean, screened ice caps at both poles and thin screened cloud streaks above everything.
     */
    val EARTH = BodyIdentity(
        marks = listOf(
            // Land: clusters of two or three caps, enough to read as continents at 10 dp.
            ell(-0.36f, -0.30f, 0.175f, 0.195f, 0xFF35894C, 0.94f, MarkEdge.FAIR, MarkRole.TINT),
            ell(-0.20f, -0.11f, 0.115f, 0.140f, 0xFF429A57, 0.90f, MarkEdge.FAIR, MarkRole.TINT),
            ell(-0.44f, -0.06f, 0.070f, 0.090f, 0xFF2F6E5E, 0.82f, MarkEdge.FAIR, MarkRole.TINT),
            ell(0.28f, -0.34f, 0.205f, 0.140f, 0xFF35894C, 0.94f, MarkEdge.FAIR, MarkRole.TINT),
            ell(0.45f, -0.13f, 0.100f, 0.125f, 0xFF429A57, 0.88f, MarkEdge.FAIR, MarkRole.TINT),
            ell(0.20f, -0.22f, 0.080f, 0.060f, 0xFF2F6E5E, 0.78f, MarkEdge.FAIR, MarkRole.TINT),
            ell(-0.06f, 0.44f, 0.160f, 0.130f, 0xFF2F7C44, 0.92f, MarkEdge.FAIR, MarkRole.TINT),
            ell(0.15f, 0.30f, 0.085f, 0.095f, 0xFF429A57, 0.86f, MarkEdge.FAIR, MarkRole.TINT),
            // Ice caps.
            ell(-0.02f, -0.84f, 0.200f, 0.062f, 0xFFF4FAFF, 0.66f, MarkEdge.FAIR, MarkRole.LIGHTEN),
            ell(0.06f, 0.84f, 0.170f, 0.052f, 0xFFF4FAFF, 0.54f, MarkEdge.FAIR, MarkRole.LIGHTEN),
            // Clouds and one deep-ocean region.
            ell(-0.10f, -0.60f, 0.300f, 0.050f, 0xFFFFFFFF, 0.42f, MarkEdge.SOFT, MarkRole.LIGHTEN),
            ell(0.22f, 0.10f, 0.270f, 0.046f, 0xFFFFFFFF, 0.36f, MarkEdge.SOFT, MarkRole.LIGHTEN),
            ell(-0.28f, 0.30f, 0.210f, 0.042f, 0xFFFFFFFF, 0.32f, MarkEdge.SOFT, MarkRole.LIGHTEN),
            ell(0.42f, -0.44f, 0.150f, 0.040f, 0xFFFFFFFF, 0.30f, MarkEdge.SOFT, MarkRole.LIGHTEN),
            ell(-0.44f, 0.16f, 0.130f, 0.036f, 0xFF2A6FA8, 0.34f, MarkEdge.SOFT, MarkRole.DARKEN)
        ),
        lighting = BodyLighting(
            specularRadius = 0.105f, specularCx = -0.36f, specularCy = -0.40f,
            specularAlpha = 0.18f, specularEdge = MarkEdge.FAIR,
            limbDarkening = 0.46f, atmosphere = 0.70f
        ),
        limbArgb = 0xFF123F8F
    )

    /**
     * Moon — cool white under three broad maria, each paired with a second cap so the shorelines
     * are irregular, and craters with a multiplied floor and a screened lit wall.
     */
    val MOON = BodyIdentity(
        marks = listOf(
            ell(-0.28f, -0.24f, 0.245f, 0.195f, 0xFFB4BCCA, 0.50f, MarkEdge.FAIR, MarkRole.DARKEN),
            ell(-0.10f, -0.30f, 0.150f, 0.120f, 0xFFB4BCCA, 0.42f, MarkEdge.FAIR, MarkRole.DARKEN),
            ell(0.20f, -0.06f, 0.185f, 0.205f, 0xFFBCC3D1, 0.44f, MarkEdge.FAIR, MarkRole.DARKEN),
            ell(0.34f, 0.10f, 0.120f, 0.140f, 0xFFBCC3D1, 0.36f, MarkEdge.FAIR, MarkRole.DARKEN),
            ell(-0.04f, 0.36f, 0.205f, 0.140f, 0xFFB4BCCA, 0.42f, MarkEdge.FAIR, MarkRole.DARKEN),
            ell(-0.24f, 0.34f, 0.120f, 0.100f, 0xFFB4BCCA, 0.34f, MarkEdge.FAIR, MarkRole.DARKEN),
            ell(0.30f, 0.42f, 0.125f, 0.115f, 0xFFA9B0BE, 0.44f, MarkEdge.CRISP, MarkRole.DARKEN),
            ell(0.362f, 0.352f, 0.050f, 0.045f, 0xFFF4F7FC, 0.30f, MarkEdge.FAIR, MarkRole.LIGHTEN),
            ell(-0.44f, 0.12f, 0.105f, 0.098f, 0xFFA9B0BE, 0.38f, MarkEdge.CRISP, MarkRole.DARKEN),
            ell(-0.388f, 0.068f, 0.044f, 0.040f, 0xFFF4F7FC, 0.26f, MarkEdge.FAIR, MarkRole.LIGHTEN),
            ell(0.02f, -0.54f, 0.065f, 0.060f, 0xFFAEB5C3, 0.36f, MarkEdge.CRISP, MarkRole.DARKEN)
        ),
        lighting = BodyLighting(
            specularRadius = 0.105f, specularCx = -0.37f, specularCy = -0.41f,
            specularAlpha = 0.28f, specularEdge = MarkEdge.FAIR, limbDarkening = 0.60f
        ),
        limbArgb = 0xFF5D6675
    )

    /**
     * Mars — vivid red-orange under multiplied rust regions, one crisp crater, a bright dust field
     * and pale caps at both poles, the southern one the larger.
     */
    val MARS = BodyIdentity(
        marks = listOf(
            ell(-0.30f, 0.12f, 0.235f, 0.160f, 0xFFB86B4D, 0.50f, MarkEdge.SOFT, MarkRole.DARKEN),
            ell(0.30f, -0.10f, 0.195f, 0.175f, 0xFFC07554, 0.44f, MarkEdge.SOFT, MarkRole.DARKEN),
            ell(0.02f, 0.42f, 0.215f, 0.115f, 0xFFB06446, 0.40f, MarkEdge.SOFT, MarkRole.DARKEN),
            ell(-0.12f, -0.34f, 0.155f, 0.105f, 0xFFA85A3C, 0.32f, MarkEdge.SOFT, MarkRole.DARKEN),
            ell(0.48f, 0.26f, 0.115f, 0.095f, 0xFFA85A3C, 0.26f, MarkEdge.CRISP, MarkRole.DARKEN),
            ell(-0.44f, -0.14f, 0.130f, 0.095f, 0xFFFFA274, 0.22f, MarkEdge.SOFT, MarkRole.LIGHTEN),
            ell(0.04f, 0.76f, 0.175f, 0.066f, 0xFFFFF1E6, 0.78f, MarkEdge.FAIR, MarkRole.LIGHTEN),
            ell(-0.02f, -0.78f, 0.150f, 0.050f, 0xFFFFF4EC, 0.58f, MarkEdge.FAIR, MarkRole.LIGHTEN),
            ell(0.40f, -0.44f, 0.070f, 0.062f, 0xFF9E5236, 0.34f, MarkEdge.CRISP, MarkRole.DARKEN),
            ell(0.435f, -0.475f, 0.032f, 0.028f, 0xFFFFD2B6, 0.22f, MarkEdge.FAIR, MarkRole.LIGHTEN)
        ),
        lighting = BodyLighting(
            specularRadius = 0.12f, specularCx = -0.35f, specularCy = -0.39f,
            specularAlpha = 0.22f, specularEdge = MarkEdge.FAIR,
            limbDarkening = 0.55f, atmosphere = 0.30f
        ),
        limbArgb = 0xFF6B2008
    )

    /**
     * Jupiter — cream/ivory under eight curved belts and zones that multiply and screen the
     * marble's own shading, so they bend with the sphere instead of lying across it. The Great Red
     * Spot is a multiplied oval with a screened halo, sitting in the belt that carries it.
     */
    val JUPITER = BodyIdentity(
        marks = listOf(
            ell(0.30f, 0.20f, 0.185f, 0.098f, 0xFFC25A3E, 0.80f, MarkEdge.FAIR, MarkRole.DARKEN),
            ell(0.30f, 0.20f, 0.245f, 0.140f, 0xFFEBCFA6, 0.26f, MarkEdge.SOFT, MarkRole.LIGHTEN),
            ell(-0.52f, -0.02f, 0.150f, 0.045f, 0xFFFFF6E4, 0.22f, MarkEdge.SOFT, MarkRole.LIGHTEN),
            ell(0.62f, 0.06f, 0.120f, 0.038f, 0xFFB0794A, 0.26f, MarkEdge.SOFT, MarkRole.DARKEN),
            band(-0.90f, 0.045f, 0xFFBC9264, 0.28f, MarkRole.DARKEN),
            band(-0.70f, 0.075f, 0xFFC79A6B, 0.34f, MarkRole.DARKEN),
            band(-0.44f, 0.100f, 0xFFCCA880, 0.50f, MarkRole.DARKEN),
            band(-0.18f, 0.115f, 0xFFF9F0DC, 0.42f, MarkRole.LIGHTEN),
            band(0.08f, 0.105f, 0xFFC29A6C, 0.54f, MarkRole.DARKEN),
            band(0.34f, 0.095f, 0xFFF3E6C8, 0.40f, MarkRole.LIGHTEN),
            band(0.62f, 0.080f, 0xFFCBA678, 0.42f, MarkRole.DARKEN),
            band(0.84f, 0.055f, 0xFFB08A5E, 0.36f, MarkRole.DARKEN)
        ),
        lighting = BodyLighting(
            specularRadius = 0.46f, specularCx = -0.26f, specularCy = -0.32f,
            specularAlpha = 0.09f, specularEdge = MarkEdge.SOFT,
            limbDarkening = 0.40f, atmosphere = 0.90f
        ),
        limbArgb = 0xFF8A5A32
    )

    /**
     * Saturn — pale gold under five gentle belts, plus the ring system: four bands with honest
     * transparent gaps, the widest the Cassini division. The rings stay strokes in the draw phase
     * and nothing else — no mass, no radius, no collision, no selection geometry.
     */
    val SATURN = BodyIdentity(
        marks = listOf(
            band(-0.86f, 0.050f, 0xFFB89454, 0.34f, MarkRole.DARKEN),
            band(-0.56f, 0.090f, 0xFFC9A765, 0.42f, MarkRole.DARKEN),
            band(-0.18f, 0.110f, 0xFFFDF6E2, 0.36f, MarkRole.LIGHTEN),
            band(0.22f, 0.100f, 0xFFC2A060, 0.44f, MarkRole.DARKEN),
            band(0.60f, 0.075f, 0xFFEFE0B4, 0.30f, MarkRole.LIGHTEN)
        ),
        rings = listOf(
            // C ring: faint and innermost.
            RingBand(1.30f, 0.055f, 0xFFC9B078, 0.26f),
            // B ring: the bright, wide one.
            RingBand(1.49f, 0.115f, 0xFFE8D193, 0.58f),
            // (Cassini division — the empty span between 1.5475 and 1.64.)
            // A ring: outside the gap.
            RingBand(1.68f, 0.080f, 0xFFD9BE7F, 0.38f),
            // F ring: a hair of light outside everything.
            RingBand(1.83f, 0.020f, 0xFFC2AC74, 0.14f)
        ),
        ringSquash = 0.30f,
        lighting = BodyLighting(
            specularRadius = 0.48f, specularCx = -0.25f, specularCy = -0.31f,
            specularAlpha = 0.09f, specularEdge = MarkEdge.SOFT,
            limbDarkening = 0.38f, atmosphere = 0.90f
        ),
        limbArgb = 0xFF9A7C46
    )

    /**
     * Uranus — pale cyan and almost featureless: three extremely subtle belts and a faint polar
     * brightening. Distinct from Earth by hue and by having no land or cloud contrast at all.
     */
    val URANUS = BodyIdentity(
        marks = listOf(
            ell(-0.05f, -0.30f, 0.26f, 0.16f, 0xFFE4FAF7, 0.18f, MarkEdge.SOFT, MarkRole.LIGHTEN),
            band(-0.40f, 0.130f, 0xFFDCF7F4, 0.30f, MarkRole.LIGHTEN),
            band(0.10f, 0.140f, 0xFF7FC5C0, 0.28f, MarkRole.DARKEN),
            band(0.58f, 0.095f, 0xFFD0F0EC, 0.22f, MarkRole.LIGHTEN)
        ),
        lighting = BodyLighting(
            specularRadius = 0.50f, specularCx = -0.24f, specularCy = -0.30f,
            specularAlpha = 0.08f, specularEdge = MarkEdge.SOFT,
            limbDarkening = 0.36f, atmosphere = 0.95f
        ),
        limbArgb = 0xFF5FA8A4
    )

    /**
     * Neptune — saturated cobalt, visibly deeper than Uranus: multiplied dark belts, the Great Dark
     * Spot and two screened streaks of high cloud, which is what the real one is famous for.
     */
    val NEPTUNE = BodyIdentity(
        marks = listOf(
            ell(0.16f, -0.30f, 0.160f, 0.098f, 0xFF8C97D8, 0.44f, MarkEdge.SOFT, MarkRole.DARKEN),
            ell(-0.26f, -0.04f, 0.185f, 0.052f, 0xFFEDF2FF, 0.38f, MarkEdge.SOFT, MarkRole.LIGHTEN),
            ell(-0.06f, 0.08f, 0.145f, 0.042f, 0xFFEDF2FF, 0.28f, MarkEdge.SOFT, MarkRole.LIGHTEN),
            band(-0.44f, 0.105f, 0xFF7C8FE8, 0.34f, MarkRole.LIGHTEN),
            band(0.06f, 0.125f, 0xFF8C97D8, 0.42f, MarkRole.DARKEN),
            band(0.56f, 0.090f, 0xFF7189E6, 0.26f, MarkRole.LIGHTEN)
        ),
        lighting = BodyLighting(
            specularRadius = 0.46f, specularCx = -0.26f, specularCy = -0.32f,
            specularAlpha = 0.10f, specularEdge = MarkEdge.SOFT,
            limbDarkening = 0.42f, atmosphere = 0.90f
        ),
        limbArgb = 0xFF17246B
    )

    /**
     * Asteroid — keeps the warm rocky family it already had: a broad multiplied basin, irregular
     * patches and one screened fleck of fresh ejecta, so it reads as a battered rock.
     */
    val ASTEROID = BodyIdentity(
        marks = scatter(
            seed = 23, count = 3, minRadius = 0.075f, maxRadius = 0.16f,
            argb = 0xFFB89476, minAlpha = 0.32f, maxAlpha = 0.44f, reach = 0.58f,
            edge = MarkEdge.FAIR, role = MarkRole.DARKEN
        ) + listOf(
            ell(0.28f, -0.30f, 0.125f, 0.098f, 0xFFA88464, 0.36f, MarkEdge.FAIR, MarkRole.DARKEN),
            ell(-0.06f, 0.10f, 0.180f, 0.130f, 0xFFB08A68, 0.26f, MarkEdge.SOFT, MarkRole.DARKEN),
            ell(-0.10f, -0.44f, 0.060f, 0.050f, 0xFFF0DCC4, 0.20f, MarkEdge.FAIR, MarkRole.LIGHTEN)
        ),
        lighting = BodyLighting(
            specularRadius = 0.10f, specularCx = -0.38f, specularCy = -0.42f,
            specularAlpha = 0.24f, specularEdge = MarkEdge.FAIR, limbDarkening = 0.60f
        ),
        limbArgb = 0xFF5E371E
    )

    /**
     * A planet with no catalog key. It already borrows Earth's blue through `colorOf`, but Earth's
     * continents would be invented detail on an object nobody identified, so it gets neutral
     * banding instead: a planet, not a place.
     */
    val GENERIC_PLANET = BodyIdentity(
        marks = listOf(
            band(-0.30f, 0.16f, 0xFFDCE8F5, 0.22f, MarkRole.LIGHTEN),
            band(0.18f, 0.18f, 0xFF2E4A6E, 0.24f, MarkRole.DARKEN),
            band(0.60f, 0.10f, 0xFFCFDCEA, 0.16f, MarkRole.LIGHTEN)
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
        alpha: Float,
        edge: MarkEdge = MarkEdge.FAIR,
        role: MarkRole = MarkRole.DARKEN
    ) = IdentityMark(cx, cy, rx, ry, argb, alpha, edge, role, MarkKind.BLOB)

    /**
     * A latitude belt: [cy] is where its equator-ward apex sits on the disc and [ry] its angular
     * half-thickness. [SphereProjection.projectBand] turns that into the curved arc the renderer
     * strokes, so a belt bends with the sphere exactly as the ring system does.
     */
    private fun band(
        cy: Float,
        ry: Float,
        argb: Long,
        alpha: Float,
        role: MarkRole = MarkRole.DARKEN
    ): IdentityMark {
        // rx is the belt's true projected half-width, cos(lambda): honest about what gets drawn,
        // and it is what the "belts span the body" assertions in the test measure.
        val tau = asin(BodyIdentity.DEFAULT_AXIS_TILT)
        val lambda = tau - asin(cy.coerceIn(-0.999f, 0.999f))
        return IdentityMark(0f, cy, cos(lambda), ry, argb, alpha, MarkEdge.SOFT, role, MarkKind.BAND)
    }

    /**
     * A deterministic scatter of soft caps, for surfaces whose character is irregularity rather
     * than placement: craters, granulation, battered rock.
     *
     * Polar sampling with a `sqrt` radius keeps the distribution even over the area, and [reach]
     * bounds the centre distance so that centre + cap radius never exceeds the body disc. Public
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
        reach: Float,
        edge: MarkEdge = MarkEdge.FAIR,
        role: MarkRole = MarkRole.DARKEN
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
                alpha = alpha,
                edge = edge,
                role = role,
                kind = MarkKind.BLOB
            )
        }
        return out
    }
}

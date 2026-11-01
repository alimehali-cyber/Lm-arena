package com.alijafari.red.astronomy.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * ============================================================================
 * THE SMOOTH-EDGE DIAL — one continuous family from a circular corner to a squircle
 * ============================================================================
 *
 * [Squircle] ships one corner: the superellipse `xⁿ + (1−y)ⁿ = 1` at `n = 4` whose tangent points sit
 * `1.6 r` from the corner. That is the right default, but it is not the only answer — some people want
 * the harder, more "product-designed" circular round that every other app uses, and some want the
 * corner to reach further still. So the curve becomes a *family* with one parameter:
 *
 *  - **smoothness 0** — `n = 2`, reach `1.0 r`. This is not "almost a circle": substituting `n = 2`
 *    into the equation gives `x² + (1 − y)² = 1`, an **exact circular arc** of radius `r` centred on
 *    the corner. The dial therefore starts at a plain `RoundedCornerShape`, not at an approximation of
 *    one.
 *  - **smoothness 1** — `n = 4`, reach `1.6 r`: the shipped squircle, and the value every other shape
 *    in the app keeps using.
 *  - Anything between interpolates both numbers, so the curvature at the edge join rises continuously
 *    from G1 (circular) toward G2 (continuous) instead of switching between two looks.
 *
 * This is the "smooth edges" option in Liquid Glass settings: it changes the geometry the glass is
 * clipped to — the refraction follows the outline — rather than faking softness with a blur or a
 * translucent border.
 *
 * ## Why the control points are generated, not hand-tuned
 *
 * The shipped `n = 4` table in [Squircle.CORNER] is a hand-fitted, curvature-matched chain of four
 * cubics, and it stays exactly as it is: at smoothness 1 this file returns the ordinary
 * [squircleShape] and never touches that table. Any other exponent needs its own chain, so the corner
 * is generated here by sampling the analytic curve at equal arc length and fitting cubic Hermite
 * segments between those nodes, with the end tangents pinned to the edges. That gives:
 *
 *  - exact positions on the analytic curve at every node (not a fit of a fit),
 *  - G1 continuity between segments and with the straight edges (tangents come from the same curve),
 *  - a maximum deviation from the analytic corner well under a tenth of a pixel at the radii the app
 *    uses — measured by `CornerSmoothnessTest`, which re-derives the curve independently.
 *
 * The chain is computed once per quantised dial position and cached, because it is geometry, not
 * animation: the dial moves in Settings, not per frame.
 */
object CornerSmoothness {

    /** The circular end of the dial: `n = 2`, reach `1.0 r` — an exact rounded rectangle. */
    const val CIRCULAR: Float = 0f

    /** The shipped continuous end of the dial: `n = 4`, reach `1.6 r`. */
    const val CONTINUOUS: Float = 1f

    /** Where the dial starts: the app's own corner. */
    const val DEFAULT: Float = CONTINUOUS

    /** Segment count per corner. Eight cubics hold the analytic curve to well under a pixel. */
    private const val SEGMENTS = 8

    /** Samples used to measure arc length before the nodes are placed. */
    private const val SAMPLES = 512

    /** The dial is quantised to this many steps so the cache is a plain array and never grows. */
    private const val STEPS = 64

    /** Lamé exponent for a dial position: 2 (circular) … 4 (continuous). */
    fun exponent(smoothness: Float): Float {
        val t = smoothness.coerceIn(0f, 1f)
        return 2f + 2f * t
    }

    /** Corner reach for a dial position, as a multiple of the radius: 1.0 … 1.6. */
    fun extent(smoothness: Float): Float {
        val t = smoothness.coerceIn(0f, 1f)
        return 1f + (Squircle.EXTENT - 1f) * t
    }

    /** True when the dial is at the app's own corner, i.e. the shipped [Squircle] geometry applies. */
    fun isDefault(smoothness: Float): Boolean = smoothness >= 0.9995f

    private val cache = arrayOfNulls<FloatArray>(STEPS + 1)

    /**
     * The control points of one whole corner, canonical frame.
     *
     * The corner starts at `(0, 0)` on its incoming edge and ends at `(1, 1)` on its outgoing edge; both
     * are the corner box in units of `extent × radius`. Each segment contributes six floats
     * (`p1x, p1y, p2x, p2y, p3x, p3y`), the segment's own start being the previous end.
     */
    fun cornerControls(smoothness: Float): FloatArray {
        val t = smoothness.coerceIn(0f, 1f)
        val index = Math.round(t * STEPS)
        cache[index]?.let { return it }
        synchronized(cache) {
            cache[index]?.let { return it }
            val controls = buildControls(exponent(index / STEPS.toFloat()))
            cache[index] = controls
            return controls
        }
    }

    /**
     * The analytic corner: `(x, 1 − (1 − xⁿ)^(1/n))`, sampled through the superellipse parametrisation
     * `x = cos^(2/n) θ`, `1 − y = sin^(2/n) θ` because that one has no square-root singularity at the
     * ends — which is exactly where a naive `y(x)` parametrisation falls apart at `n > 2`.
     */
    private fun pointAt(theta: Double, n: Double): DoubleArray {
        val twoOverN = 2.0 / n
        return doubleArrayOf(
            cos(theta).pow(twoOverN),
            1.0 - sin(theta).pow(twoOverN)
        )
    }

    private fun buildControls(n: Float): FloatArray {
        val nn = n.toDouble()

        // 1. Dense sample from the incoming edge (θ = 90°) to the outgoing edge (θ = 0).
        val xs = DoubleArray(SAMPLES + 1)
        val ys = DoubleArray(SAMPLES + 1)
        val lengths = DoubleArray(SAMPLES + 1)
        for (i in 0..SAMPLES) {
            val theta = (Math.PI / 2.0) * (1.0 - i / SAMPLES.toDouble())
            val p = pointAt(theta, nn)
            xs[i] = p[0]
            ys[i] = p[1]
            if (i > 0) {
                val dx = xs[i] - xs[i - 1]
                val dy = ys[i] - ys[i - 1]
                lengths[i] = lengths[i - 1] + Math.hypot(dx, dy)
            }
        }

        // 2. Nodes at equal arc length: the fit is then symmetric about the corner diagonal, which is
        //    what keeps both halves of the corner identical at any exponent.
        val nodeX = DoubleArray(SEGMENTS + 1)
        val nodeY = DoubleArray(SEGMENTS + 1)
        val total = lengths[SAMPLES]
        var cursor = 0
        for (k in 0..SEGMENTS) {
            val target = total * k / SEGMENTS.toDouble()
            while (cursor < SAMPLES && lengths[cursor] < target) cursor++
            nodeX[k] = xs[cursor]
            nodeY[k] = ys[cursor]
        }
        // Endpoints are the analytic ones, not a sample that rounded onto them.
        nodeX[0] = 0.0; nodeY[0] = 0.0
        nodeX[SEGMENTS] = 1.0; nodeY[SEGMENTS] = 1.0

        // 3. Cubic Hermite between the nodes, tangents from the analytic curve's end directions. The
        //    corner leaves its incoming edge along the edge and arrives on the outgoing edge along that
        //    edge, so the joins with the straight runs are tangent by construction (G1).
        val spacing = total / SEGMENTS.toDouble()
        val out = FloatArray(SEGMENTS * 6)
        for (k in 0 until SEGMENTS) {
            val x0 = nodeX[k]; val y0 = nodeY[k]
            val x1 = nodeX[k + 1]; val y1 = nodeY[k + 1]

            val (tx0, ty0) = tangent(nodeX, nodeY, k, spacing)
            val (tx1, ty1) = tangent(nodeX, nodeY, k + 1, spacing)

            val third = spacing / 3.0
            val i = k * 6
            out[i] = (x0 + tx0 * third).toFloat()
            out[i + 1] = (y0 + ty0 * third).toFloat()
            out[i + 2] = (x1 - tx1 * third).toFloat()
            out[i + 3] = (y1 - ty1 * third).toFloat()
            out[i + 4] = x1.toFloat()
            out[i + 5] = y1.toFloat()
        }
        return out
    }

    /** Unit tangent at node [k]; pinned to the two edges at the corner's ends. */
    private fun tangent(x: DoubleArray, y: DoubleArray, k: Int, spacing: Double): Pair<Double, Double> {
        val last = x.size - 1
        return when (k) {
            0 -> 1.0 to 0.0
            last -> 0.0 to 1.0
            else -> {
                var dx = (x[k + 1] - x[k - 1]) / 2.0
                var dy = (y[k + 1] - y[k - 1]) / 2.0
                val len = Math.hypot(dx, dy)
                if (len <= 1e-9) {
                    0.0 to 1.0
                } else {
                    dx /= len; dy /= len
                    dx to dy
                }
            }
        }
    }

    /**
     * Emits the outline of a rounded rectangle whose corners have [radiusPx] nominal radius and the
     * given corner [controls], as primitive path commands — the same shape as
     * [emitSquircleOutline], so the geometry is testable on a plain JVM with no Android graphics.
     */
    internal fun emitSmoothOutline(
        size: Size,
        tl: Float,
        tr: Float,
        br: Float,
        bl: Float,
        extent: Float,
        controls: FloatArray,
        moveTo: (Float, Float) -> Unit,
        lineTo: (Float, Float) -> Unit,
        cubicTo: (Float, Float, Float, Float, Float, Float) -> Unit,
        close: () -> Unit
    ) {
        val maxBox = size.minDimension / 2f
        val rtl = (tl * extent).coerceIn(0f, maxBox)
        val rtr = (tr * extent).coerceIn(0f, maxBox)
        val rbr = (br * extent).coerceIn(0f, maxBox)
        val rbl = (bl * extent).coerceIn(0f, maxBox)
        val right = size.width
        val bottom = size.height

        if (rtl <= 0f && rtr <= 0f && rbr <= 0f && rbl <= 0f) {
            moveTo(0f, 0f)
            lineTo(right, 0f)
            lineTo(right, bottom)
            lineTo(0f, bottom)
            close()
            return
        }

        // Each corner starts on one edge and ends on the next: the canonical curve runs from its
        // incoming-edge tangent point to its outgoing-edge tangent point, so the edges join it
        // tangentially and nothing else has to be emitted between them.
        moveTo(rtl, 0f)
        lineTo(right - rtr, 0f)
        emitCorner(right - rtr, 0f, rtr, 0f, 0f, rtr, controls, cubicTo)

        lineTo(right, bottom - rbr)
        emitCorner(right, bottom - rbr, 0f, rbr, -rbr, 0f, controls, cubicTo)

        lineTo(rbl, bottom)
        emitCorner(rbl, bottom, -rbl, 0f, 0f, -rbl, controls, cubicTo)

        lineTo(0f, rtl)
        emitCorner(0f, rtl, 0f, -rtl, rtl, 0f, controls, cubicTo)
        close()
    }

    private inline fun emitCorner(
        originX: Float,
        originY: Float,
        ax: Float,
        ay: Float,
        bx: Float,
        by: Float,
        controls: FloatArray,
        cubicTo: (Float, Float, Float, Float, Float, Float) -> Unit
    ) {
        if (ax == 0f && ay == 0f && bx == 0f && by == 0f) return
        var i = 0
        while (i < controls.size) {
            cubicTo(
                originX + ax * controls[i] + bx * controls[i + 1],
                originY + ay * controls[i] + by * controls[i + 1],
                originX + ax * controls[i + 2] + bx * controls[i + 3],
                originY + ay * controls[i + 2] + by * controls[i + 3],
                originX + ax * controls[i + 4] + bx * controls[i + 5],
                originY + ay * controls[i + 4] + by * controls[i + 5]
            )
            i += 6
        }
    }

    /** The outline above as a Compose [Path]. */
    internal fun buildSmoothPath(
        size: Size,
        radiusPx: Float,
        smoothness: Float,
        origin: Offset = Offset.Zero
    ): Path {
        val controls = cornerControls(smoothness)
        val extent = extent(smoothness)
        val dx = origin.x
        val dy = origin.y
        val path = Path()
        emitSmoothOutline(
            size = size,
            tl = radiusPx,
            tr = radiusPx,
            br = radiusPx,
            bl = radiusPx,
            extent = extent,
            controls = controls,
            moveTo = { x, y -> path.moveTo(dx + x, dy + y) },
            lineTo = { x, y -> path.lineTo(dx + x, dy + y) },
            cubicTo = { x1, y1, x2, y2, x3, y3 ->
                path.cubicTo(dx + x1, dy + y1, dx + x2, dy + y2, dx + x3, dy + y3)
            },
            close = { path.close() }
        )
        return path
    }
}

/**
 * A [SquircleShape]-compatible shape whose corner smoothness is the user's setting.
 *
 * Only uniform radii are supported, which is the only thing the glass surfaces use; anything else keeps
 * its own shape. Equality is on the dial and the radius, not on the generated control points, so a
 * recomposition never rebuilds the path for the same values.
 */
@Immutable
class SmoothCornerShape(
    private val radius: Dp,
    private val smoothness: Float
) : Shape {

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        if (size.minDimension <= 0f) return Outline.Generic(Path())
        val radiusPx = with(density) { radius.toPx() }
        return Outline.Generic(CornerSmoothness.buildSmoothPath(size, radiusPx, smoothness))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SmoothCornerShape) return false
        return radius == other.radius && abs(smoothness - other.smoothness) < 1e-4f
    }

    override fun hashCode(): Int = 31 * radius.hashCode() + Math.round(smoothness * 1000f)

    override fun toString(): String = "SmoothCornerShape(radius=$radius, smoothness=$smoothness)"
}

/**
 * The same shape with the user's corner smoothness applied.
 *
 * Shapes the app does not own the geometry of — capsules, circles, Material's own defaults, a
 * `RoundedCornerShape` a third-party component passed in — are returned untouched: smoothing a capsule
 * would flatten a pill's ends, which is a bug, not a preference.
 */
fun Shape.withCornerSmoothing(smoothness: Float): Shape {
    if (CornerSmoothness.isDefault(smoothness)) return this
    val squircle = this as? SquircleShape ?: return this
    val radius = squircle.uniformRadius ?: return this
    if (radius <= 0.dp) return this
    return SmoothCornerShape(radius, smoothness)
}

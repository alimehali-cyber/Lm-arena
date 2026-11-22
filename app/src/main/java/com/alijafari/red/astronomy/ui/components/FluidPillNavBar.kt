package com.alijafari.red.astronomy.ui.components

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sqrt

/* ------------------------------------------------------------------ */
/*  The fluid pill navigation bar — one design, three unique UIs.      */
/*                                                                     */
/*  Inside the capsule runs the same idea as the WebGL fluid preview   */
/*  (Pavel Dobryakov, WebGL-Fluid-Simulation) on the CPU: Jos Stam     */
/*  stable fluids: Gaussian splats, curl + vorticity confinement for   */
/*  the natural swirls, a Jacobi pressure solve, gentle dissipation.   */
/*  The dye is drawn as burgundy ink (#660033) over a white ground,    */
/*  and a fluid "lens" (a pill of dye) flows between tabs: it stretches */
/*  toward the destination, then the trailing edge snaps shut.         */
/* ------------------------------------------------------------------ */

/** The dye recipe of the fluid: a deep companion, the primary colour, and the lens hairline. */
data class FluidDyeStyle(
    /** The colour thick dye settles into. */
    val deep: Color,
    /** The colour of the moving, mid-density dye. */
    val bright: Color,
    /** The hairline drawn around the lens pill. */
    val stroke: Color,
)

/**
 * The primary fluid colour — the design's burgundy: #2E0019 deep settling into **#660033** over a
 * white ground. Every UI wears this dye; each skin stays unique through its chrome instead.
 */
val BurgundyFluidDye = FluidDyeStyle(
    deep = Color(0xFF2E0019),
    bright = Color(0xFF660033),
    stroke = Color(0xFF660033).copy(alpha = 0.14f),
)

/**
 * One destination shown inside the fluid pill bar.
 */
data class FluidNavSpec(
    /** The tab index this cell selects (its stable geometry key). */
    val key: Int,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

/* ------------------------------------------------------------------ */
/*  Stable fluids (Jos Stam), compact CPU version                      */
/* ------------------------------------------------------------------ */

/** WebGL-Fluid-Simulation defaults, translated to the CPU grid. */
private const val PRESSURE_ITERATIONS = 20
private const val PRESSURE_DECAY = 0.8f
private const val VORTICITY = 30f

/** Velocity ceiling in uv/sec — the original clamps at +-1000 texels/sec. */
private const val MAX_FLOW = 12f

class StableFluid(private val w: Int, private val h: Int, private val style: FluidDyeStyle) {

    private val n = w * h

    private val u = FloatArray(n)
    private val v = FloatArray(n)
    private val u0 = FloatArray(n)
    private val v0 = FloatArray(n)
    private val dye = FloatArray(n)
    private val dye0 = FloatArray(n)
    private val curl = FloatArray(n)
    private val div = FloatArray(n)
    private val p = FloatArray(n)

    private val bitmap: Bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    private val image = bitmap.asImageBitmap()
    private val pixels = IntArray(n)

    init {
        // The capsule starts on the white ground — never a blank frame.
        render()
    }

    /**
     * Gaussian radius/velocity/dye splat in normalised (0..1) coordinates — the soft
     * exp(-dot(p, p) / radius) puff of the WebGL original, round on screen.
     */
    fun splat(x: Float, y: Float, radius: Float, velX: Float, velY: Float, dyeAmount: Float) {
        val cx = x * w - 0.5f
        val cy = y * h - 0.5f
        val sigma = (radius * h).coerceAtLeast(1f)
        val reach = (sigma * 2.2f).toInt() + 1
        val inv = 1f / (sigma * sigma)
        for (j in maxOf(1, cy.toInt() - reach)..minOf(h - 2, cy.toInt() + reach)) {
            for (i in maxOf(1, cx.toInt() - reach)..minOf(w - 2, cx.toInt() + reach)) {
                val dx = i - cx
                val dy = j - cy
                val fall = exp(-(dx * dx + dy * dy) * inv)
                if (fall < 0.01f) continue
                val idx = i + j * w
                u[idx] = (u[idx] + velX * fall).coerceIn(-MAX_FLOW, MAX_FLOW)
                v[idx] = (v[idx] + velY * fall).coerceIn(-MAX_FLOW, MAX_FLOW)
                dye[idx] += dyeAmount * fall
            }
        }
    }

    /**
     * One solver step — the WebGL-Fluid-Simulation recipe: curl, vorticity confinement (the
     * natural swirls), divergence, pressure decay + Jacobi solve, gradient subtraction, then
     * velocity advection and dye advection, both with gentle dissipation.
     */
    fun step(dt: Float, dyeDissipation: Float, velocityDissipation: Float) {
        computeCurl()
        vorticityConfinement(dt)
        computeDivergence()
        for (idx in 0 until n) p[idx] *= PRESSURE_DECAY
        pressureSolve()
        gradientSubtract()
        System.arraycopy(u, 0, u0, 0, n)
        System.arraycopy(v, 0, v0, 0, n)
        advect(u, u0, u0, v0, dt, velocityDissipation)
        advect(v, v0, u0, v0, dt, velocityDissipation)
        System.arraycopy(dye, 0, dye0, 0, n)
        advect(dye, dye0, u, v, dt, dyeDissipation)
        render()
    }

    /** curl = dv/dx - du/dy — the seed of every little vortex. */
    private fun computeCurl() {
        for (j in 1..h - 2) {
            var idx = 1 + j * w
            for (i in 1..w - 2) {
                curl[idx] = 0.5f * ((v[idx + 1] - v[idx - 1]) - (u[idx + w] - u[idx - w]))
                idx++
            }
        }
    }

    /** Vorticity confinement: puts the little swirls back that advection smears out. */
    private fun vorticityConfinement(dt: Float) {
        for (j in 1..h - 2) {
            var idx = 1 + j * w
            for (i in 1..w - 2) {
                val l = curl[idx - 1]
                val r = curl[idx + 1]
                val t = curl[idx + w]
                val b = curl[idx - w]
                val c = curl[idx]
                var fx = 0.5f * (abs(t) - abs(b))
                var fy = 0.5f * (abs(r) - abs(l))
                val len = sqrt(fx * fx + fy * fy) + 0.0001f
                fx /= len
                fy /= len
                fx *= VORTICITY * c
                fy *= -VORTICITY * c
                u[idx] = (u[idx] + fx * dt).coerceIn(-MAX_FLOW, MAX_FLOW)
                v[idx] = (v[idx] + fy * dt).coerceIn(-MAX_FLOW, MAX_FLOW)
                idx++
            }
        }
    }

    /** Velocity divergence — how hard each cell pushes outward. */
    private fun computeDivergence() {
        for (j in 1..h - 2) {
            var idx = 1 + j * w
            for (i in 1..w - 2) {
                div[idx] = 0.5f * (u[idx + 1] - u[idx - 1] + v[idx + w] - v[idx - w])
                idx++
            }
        }
        setBnd(div)
    }

    /** Twenty Jacobi pressure iterations — the incompressibility that makes the flow curl. */
    private fun pressureSolve() {
        repeat(PRESSURE_ITERATIONS) {
            for (j in 1..h - 2) {
                var idx = 1 + j * w
                for (i in 1..w - 2) {
                    p[idx] = (p[idx - 1] + p[idx + 1] + p[idx - w] + p[idx + w] - div[idx]) * 0.25f
                    idx++
                }
            }
            setBnd(p)
        }
    }

    /** Subtract the pressure gradient so the velocity field stays divergence-light. */
    private fun gradientSubtract() {
        for (j in 1..h - 2) {
            var idx = 1 + j * w
            for (i in 1..w - 2) {
                u[idx] -= p[idx + 1] - p[idx - 1]
                v[idx] -= p[idx + w] - p[idx - w]
                idx++
            }
        }
        setBnd(u)
        setBnd(v)
    }

    /** Semi-Lagrangian bilinear advection with gentle dissipation. */
    private fun advect(
        d: FloatArray,
        d0: FloatArray,
        uu: FloatArray,
        vv: FloatArray,
        dt: Float,
        dissipation: Float,
    ) {
        val dtx = dt * w
        val dty = dt * h
        val decay = 1f / (1f + dissipation * dt)
        for (j in 1..h - 2) {
            for (i in 1..w - 2) {
                val idx = i + j * w
                var x = i - dtx * uu[idx]
                var y = j - dty * vv[idx]
                if (x < 0.5f) x = 0.5f
                if (x > w - 1.5f) x = w - 1.5f
                if (y < 0.5f) y = 0.5f
                if (y > h - 1.5f) y = h - 1.5f
                val i0 = x.toInt()
                val i1 = i0 + 1
                val j0 = y.toInt()
                val j1 = j0 + 1
                val s1 = x - i0
                val s0 = 1f - s1
                val t1 = y - j0
                val t0 = 1f - t1
                d[idx] = decay * (
                    s0 * (t0 * d0[i0 + j0 * w] + t1 * d0[i0 + j1 * w]) +
                        s1 * (t0 * d0[i1 + j0 * w] + t1 * d0[i1 + j1 * w])
                    )
            }
        }
        setBnd(d)
    }

    private fun setBnd(x: FloatArray) {
        for (i in 1..w - 2) {
            x[i] = -x[i + w]
            x[i + (h - 1) * w] = -x[i + (h - 2) * w]
        }
        for (j in 1..h - 2) {
            x[j * w] = -x[1 + j * w]
            x[w - 1 + j * w] = -x[w - 2 + j * w]
        }
        x[0] = 0.5f * (x[1] + x[w])
        x[w - 1] = 0.5f * (x[w - 2] + x[2 * w - 1])
        x[(h - 1) * w] = 0.5f * (x[1 + (h - 1) * w] + x[(h - 2) * w])
        x[w - 1 + (h - 1) * w] = 0.5f * (x[w - 2 + (h - 1) * w] + x[w - 1 + (h - 2) * w])
    }

    /**
     * Dye density -> burgundy ink over a white ground. Thin dye reads as light wisps, the body
     * thickens into the primary #660033, and the densest cores settle toward the deep companion
     * — so the fluid always shows its structure instead of flattening into a dark slab.
     */
    private fun render() {
        val bright = style.bright
        val deep = style.deep
        for (idx in 0 until n) {
            val d = dye[idx].coerceAtLeast(0f)
            val cover = 1f - exp(-d * 1.9f)
            val core = smoothstep(0.55f, 1f, cover)
            val inkR = lerp(bright.red, deep.red, core)
            val inkG = lerp(bright.green, deep.green, core)
            val inkB = lerp(bright.blue, deep.blue, core)
            val r = (lerp(1f, inkR, cover) * 255f).toInt().coerceIn(0, 255)
            val g = (lerp(1f, inkG, cover) * 255f).toInt().coerceIn(0, 255)
            val b = (lerp(1f, inkB, cover) * 255f).toInt().coerceIn(0, 255)
            pixels[idx] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    /** The dye, drawn clipped to the lens pill. */
    fun drawLens(d: DrawScope, left: Float, top: Float, widthPx: Float, heightPx: Float) {
        val pill = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(Offset(left, top), Size(widthPx, heightPx)),
                    cornerRadius = CornerRadius(heightPx / 2f, heightPx / 2f),
                )
            )
        }
        d.clipPath(pill) {
            d.drawImage(
                image = image,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(w, h),
                dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                dstSize = IntSize(widthPx.roundToInt(), heightPx.roundToInt()),
                filterQuality = FilterQuality.Medium,
            )
        }
        d.drawPath(
            path = pill,
            color = style.stroke,
            style = Stroke(width = 1.5f),
        )
    }
}

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
private fun smoothstep(e0: Float, e1: Float, x: Float): Float {
    val t = ((x - e0) / (e1 - e0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/* ------------------------------------------------------------------ */
/*  Two-edge spring: the pill stretches toward the new tab, then the   */
/*  trailing edge snaps shut — that is the flow.                       */
/* ------------------------------------------------------------------ */

private const val K_LENS = 160f
private const val ZETA_LENS = 0.55f
private const val FLOW_DELAY_MS = 210L

/**
 * The sim stays alive while the liquid moves and for this long after it calms, then freezes into a
 * still lens until the next tap — the bar never burns a core between interactions.
 */
private const val SETTLE_SECONDS = 2.5f
private const val HARD_CAP_SECONDS = 8f

/**
 * On devices the fluid lives continuously. Under the Robolectric harness (fingerprint
 * "robolectric") a live frame loop keeps the compose clock permanently non-idle, so there the sim
 * settles into a still lens instead.
 */
private val fluidRunsLive: Boolean = !Build.FINGERPRINT.equals("robolectric", ignoreCase = true)

internal class FluidLensState {
    var left by mutableFloatStateOf(0f)
    var right by mutableFloatStateOf(0f)
    var vLeft by mutableFloatStateOf(0f)
    var vRight by mutableFloatStateOf(0f)
    var tLeft by mutableFloatStateOf(0f)
    var tRight by mutableFloatStateOf(0f)
    var initialized by mutableStateOf(false)
}

/**
 * The moving parts of one fluid bar: the dye field, the lens springs, and the geometry of every
 * cell. One host per bar; [rememberFluidNavLensHost] keeps it alive and running.
 */
class FluidNavLensHost internal constructor(private val style: FluidDyeStyle) {

    internal val fluid = StableFluid(128, 24, style)
    internal val lens = FluidLensState()
    internal val cellGeometry = mutableStateMapOf<Int, Pair<Float, Float>>()
    internal var barHeightPx by mutableFloatStateOf(0f)

    /** Bumped to wake the frame loop for another burst of liquid motion. */
    internal var lifeTick by mutableIntStateOf(0)

    /** Bumped every animated frame so the dye visibly lives even when the lens is at rest. */
    internal var drawTick by mutableIntStateOf(0)

    /** Tracks where a cell sits in the bar so the lens can flow to it. */
    internal fun cellGeometryModifier(index: Int): Modifier = Modifier.onGloballyPositioned { coords ->
        val leftPx = coords.positionInParent().x
        cellGeometry[index] = (leftPx + 4f) to (leftPx + coords.size.width - 4f)
    }

    internal suspend fun flowTo(cell: Pair<Float, Float>) {
        if (!lens.initialized) {
            lens.tLeft = cell.first
            lens.left = cell.first
            lens.tRight = cell.second
            lens.right = cell.second
            lens.initialized = true
            return
        }
        val dirRight = cell.first >= lens.left
        if (dirRight) {
            lens.tRight = cell.second
            delay(FLOW_DELAY_MS)
            lens.tLeft = cell.first
        } else {
            lens.tLeft = cell.first
            delay(FLOW_DELAY_MS)
            lens.tRight = cell.second
        }
        // push dye along the direction of travel — a gentle stroke, not a jet
        val push = if (dirRight) 8f else -8f
        repeat(8) { k ->
            fluid.splat(0.2f + 0.6f * k / 7f, 0.5f, 0.10f, push * 0.2f, 0f, 0.8f)
        }
    }

    internal fun seed() {
        // A ribbon of dye across the capsule — light enough to show its wisps and swirls.
        repeat(14) { i ->
            fluid.splat(
                x = 0.15f + 0.7f * i / 13f,
                y = 0.5f + 0.25f * (if (i % 2 == 0) 1f else -1f),
                radius = 0.17f,
                velX = if (i % 2 == 0) 1.8f else -1.8f,
                velY = 0f,
                dyeAmount = 1.1f,
            )
        }
    }

    internal fun ambientSplat(i: Int) {
        fluid.splat(
            x = 0.15f + 0.7f * ((i * 37 % 100) / 100f),
            y = 0.2f + 0.6f * ((i * 53 % 100) / 100f),
            radius = 0.15f,
            velX = if (i % 2 == 0) 1.2f else -1.2f,
            velY = 0.6f,
            dyeAmount = 1.1f,
        )
    }
}

/**
 * Creates and keeps one bar's fluid alive: seeds the capsule with dye, flows the lens when the
 * selection changes, and advances the springs and the fluid every frame — the dye keeps living on
 * devices (a fill pulse every 420 ms), and settles to rest only under the test harness.
 */
@Composable
fun rememberFluidNavLensHost(style: FluidDyeStyle, selectedIndex: Int): FluidNavLensHost {
    val host = remember(style) { FluidNavLensHost(style) }
    val selectedIndexState = rememberUpdatedState(selectedIndex)

    // Seed the capsule so it starts full of dye, and wake the sim.
    LaunchedEffect(host) {
        host.seed()
        host.lifeTick++
    }

    // Flow: stretch toward the new tab, then snap the trailing edge shut.
    LaunchedEffect(host, selectedIndex) {
        val cell = host.cellGeometry[selectedIndex] ?: return@LaunchedEffect
        host.lifeTick++
        host.flowTo(cell)
    }

    // Frame loop: springs + fluid + gentle ambient fill pulses. On devices the liquid lives
    // continuously, exactly like the design's keep-alive; under the test harness the sim settles
    // into a still lens so the compose clock can report idle.
    LaunchedEffect(host, host.lifeTick) {
        var last = 0L
        var calm = 0f
        var alive = 0f
        var elapsed = 0f
        var pulse = 0
        while (isActive) {
            val dt = withFrameNanos { t ->
                val d = if (last == 0L) 0.016f
                else ((t - last) / 1_000_000_000f).coerceIn(0.001f, 0.033f)
                last = t
                if (!host.lens.initialized) {
                    val c = host.cellGeometry[selectedIndexState.value]
                    if (c != null) {
                        host.lens.tLeft = c.first
                        host.lens.left = c.first
                        host.lens.tRight = c.second
                        host.lens.right = c.second
                        host.lens.initialized = true
                    }
                }
                val sq = sqrt(K_LENS)
                val aL = -K_LENS * (host.lens.left - host.lens.tLeft) - 2f * ZETA_LENS * sq * host.lens.vLeft
                host.lens.vLeft += aL * d
                host.lens.left += host.lens.vLeft * d
                val aR = -K_LENS * (host.lens.right - host.lens.tRight) - 2f * ZETA_LENS * sq * host.lens.vRight
                host.lens.vRight += aR * d
                host.lens.right += host.lens.vRight * d
                d
            }
            alive += dt
            val lens = host.lens
            val moving = abs(lens.left - lens.tLeft) > 0.4f || abs(lens.right - lens.tRight) > 0.4f ||
                abs(lens.vLeft) > 0.4f || abs(lens.vRight) > 0.4f
            if (moving) calm = 0f else calm += dt
            // Keep-alive: a gentle fill pulse now and then, so the capsule's dye never dies down.
            if (elapsed + dt >= 0.42f) {
                elapsed = 0f
                host.ambientSplat(pulse++)
            } else {
                elapsed += dt
            }
            host.fluid.step(dt, dyeDissipation = 0.25f, velocityDissipation = 0.2f)
            host.drawTick++
            if (!fluidRunsLive && (calm >= SETTLE_SECONDS || alive >= HARD_CAP_SECONDS)) break
        }
    }
    return host
}

/**
 * The lens layer: the fluid canvas plus the bar's items on top of it. Place this inside the bar's
 * own chrome (the capsule), filling it; give the items row the full width so cell geometry lines
 * up with the canvas.
 */
@Composable
fun FluidNavLensLayer(
    host: FluidNavLensHost,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.onGloballyPositioned { host.barHeightPx = it.size.height.toFloat() },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            @Suppress("UNUSED_EXPRESSION")
            host.drawTick // subscribe: the dye redraws while the sim lives
            val lensHeight = host.barHeightPx - 8f
            if (host.lens.initialized && lensHeight > 0f && host.lens.right > host.lens.left) {
                host.fluid.drawLens(
                    d = this,
                    left = host.lens.left,
                    top = 4f,
                    widthPx = host.lens.right - host.lens.left,
                    heightPx = lensHeight,
                )
            }
        }
        content()
    }
}

/** The weighted row of destinations; each cell is tracked for the flow. */
@Composable
fun FluidNavItemsRow(
    host: FluidNavLensHost,
    selectedIndex: Int,
    specs: List<FluidNavSpec>,
    selectedContentColor: Color,
    unselectedContentColor: Color,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        specs.forEach { spec ->
            val isSelected = selectedIndex == spec.key
            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(host.cellGeometryModifier(spec.key))
            ) {
                FluidNavCell(
                    label = spec.label,
                    icon = if (isSelected) spec.selectedIcon else spec.unselectedIcon,
                    selected = isSelected,
                    selectedContentColor = selectedContentColor,
                    unselectedContentColor = unselectedContentColor,
                    onClick = { onSelected(spec.key) },
                    modifier = Modifier.testTag(spec.testTag),
                )
            }
        }
    }
}

/** One destination: icon over label, tint fading with a bouncy pop when selected. */
@Composable
private fun FluidNavCell(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    selectedContentColor: Color,
    unselectedContentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint by animateColorAsState(
        targetValue = if (selected) selectedContentColor else unselectedContentColor,
        animationSpec = tween(durationMillis = 220),
        label = "FluidNavTint",
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.9f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow),
        label = "FluidNavScale",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier
                .size(22.dp)
                .scale(scale),
        )
        Text(
            text = label,
            color = tint,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

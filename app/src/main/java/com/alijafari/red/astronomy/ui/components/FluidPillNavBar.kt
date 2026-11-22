package com.alijafari.red.astronomy.ui.components

import android.graphics.Bitmap
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
import kotlin.math.roundToInt
import kotlin.math.sqrt

/* ------------------------------------------------------------------ */
/*  The fluid pill navigation bar — one design, three unique UIs.      */
/*                                                                     */
/*  Inside the capsule runs the same idea as the WebGL fluid preview   */
/*  (Pavel Dobryakov, WebGL-Fluid-Simulation) on the CPU: Jos Stam     */
/*  stable fluids. The dye is drawn as a white ground that thickens    */
/*  into a deep -> bright gradient, and a fluid "lens" (a pill of dye) */
/*  flows between tabs: it stretches toward the destination, then the  */
/*  trailing edge snaps shut. Each UI gives the dye its own colours,   */
/*  so the bar is recognisably the same design in every skin.          */
/* ------------------------------------------------------------------ */

/** The dye recipe that makes each UI's fluid unique. */
data class FluidDyeStyle(
    /** The colour thick dye settles into. */
    val deep: Color,
    /** The colour of the moving, mid-density dye. */
    val bright: Color,
    /** The hairline drawn around the lens pill. */
    val stroke: Color,
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

class StableFluid(private val w: Int, private val h: Int, private val style: FluidDyeStyle) {

    private val n = w * h

    private val u = FloatArray(n)
    private val v = FloatArray(n)
    private val u0 = FloatArray(n)
    private val v0 = FloatArray(n)
    private val dye = FloatArray(n)
    private val p = FloatArray(n)
    private val div = FloatArray(n)

    private val bitmap: Bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    private val image = bitmap.asImageBitmap()
    private val pixels = IntArray(n)

    /** Radius/velocity/dye splat in normalised (0..1) coordinates. */
    fun splat(x: Float, y: Float, radius: Float, velX: Float, velY: Float, dyeAmount: Float) {
        val cx = (x * w).toInt()
        val cy = (y * h).toInt()
        val r = (radius * w).toInt().coerceAtLeast(1)
        val r2 = r * r
        for (j in -r..r) {
            for (i in -r..r) {
                val d2 = i * i + j * j
                if (d2 > r2) continue
                val xi = cx + i
                val yi = cy + j
                if (xi < 1 || xi >= w - 1 || yi < 1 || yi >= h - 1) continue
                val idx = xi + yi * w
                val fall = 1f - d2.toFloat() / r2
                u[idx] += velX * fall
                v[idx] += velY * fall
                dye[idx] += dyeAmount * fall
            }
        }
    }

    fun step(dt: Float, dyeDissipation: Float, velocityDissipation: Float) {
        project()
        System.arraycopy(u, 0, u0, 0, n)
        System.arraycopy(v, 0, v0, 0, n)
        advect(u, u0, u0, v0, dt, velocityDissipation)
        advect(v, v0, u0, v0, dt, velocityDissipation)
        project()
        advect(dye, dye, u, v, dt, dyeDissipation)
        render()
    }

    private fun project() {
        val invDx = w.toFloat()
        for (j in 1..h - 2) {
            var idx = 1 + j * w
            for (i in 1..w - 2) {
                div[idx] = -0.5f * (u[idx + 1] - u[idx - 1] + v[idx + w] - v[idx - w]) * invDx
                p[idx] = 0f
                idx++
            }
        }
        setBnd(div)
        setBnd(p)
        linSolve(p, div, 1f, 4f, 18)
        for (j in 1..h - 2) {
            var idx = 1 + j * w
            for (i in 1..w - 2) {
                u[idx] -= 0.5f * (p[idx + 1] - p[idx - 1]) * invDx
                v[idx] -= 0.5f * (p[idx + w] - p[idx - w]) * invDx
                idx++
            }
        }
        setBnd(u)
        setBnd(v)
    }

    private fun linSolve(x: FloatArray, x0: FloatArray, a: Float, c: Float, iter: Int) {
        val invC = 1f / c
        for (k in 0 until iter) {
            for (j in 1..h - 2) {
                var idx = 1 + j * w
                for (i in 1..w - 2) {
                    x[idx] = (x0[idx] + a * (x[idx - 1] + x[idx + 1] + x[idx - w] + x[idx + w])) * invC
                    idx++
                }
            }
            setBnd(x)
        }
    }

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

    /** Dye density -> white ground thickening into the skin's deep -> bright gradient. */
    private fun render() {
        for (idx in 0 until n) {
            val t = (dye[idx] * 1.1f).coerceIn(0f, 1f)
            val body = smoothstep(0.03f, 0.55f, t)
            val r = lerp(255f, lerp(style.deep.red * 255f, style.bright.red * 255f, t), body).toInt()
            val g = lerp(255f, lerp(style.deep.green * 255f, style.bright.green * 255f, t), body).toInt()
            val b = lerp(255f, lerp(style.deep.blue * 255f, style.bright.blue * 255f, t), body).toInt()
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

    internal val fluid = StableFluid(64, 30, style)
    internal val lens = FluidLensState()
    internal val cellGeometry = mutableStateMapOf<Int, Pair<Float, Float>>()
    internal var barHeightPx by mutableFloatStateOf(0f)

    /** Bumped to wake the frame loop for another burst of liquid motion. */
    internal var lifeTick by mutableIntStateOf(0)

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
        // push dye along the direction of travel
        val push = if (dirRight) 8f else -8f
        repeat(8) { k ->
            fluid.splat(0.2f + 0.6f * k / 7f, 0.5f, 0.12f, push, 0f, 1.2f)
        }
    }

    internal fun seed() {
        repeat(14) { i ->
            fluid.splat(
                x = 0.15f + 0.7f * i / 13f,
                y = 0.5f + 0.25f * (if (i % 2 == 0) 1f else -1f),
                radius = 0.22f,
                velX = if (i % 2 == 0) 6f else -6f,
                velY = 0f,
                dyeAmount = 1.6f,
            )
        }
    }

    internal fun ambientSplat(i: Int) {
        fluid.splat(
            x = 0.15f + 0.7f * ((i * 37 % 100) / 100f),
            y = 0.2f + 0.6f * ((i * 53 % 100) / 100f),
            radius = 0.16f,
            velX = if (i % 2 == 0) 5f else -5f,
            velY = 2f,
            dyeAmount = 0.9f,
        )
    }
}

/**
 * Creates and keeps one bar's fluid alive: seeds the capsule with dye, flows the lens when the
 * selection changes, and advances the springs and the fluid while the liquid moves — the sim then
 * rests in a still lens until the next interaction wakes it.
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

    // Frame loop: springs + fluid + gentle ambient pulses, alive while the liquid moves and for a
    // short settle after — then the sim rests until the next interaction wakes it.
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
            if (elapsed + dt >= 0.42f) {
                elapsed = 0f
                host.ambientSplat(pulse++)
            } else {
                elapsed += dt
            }
            if (calm >= SETTLE_SECONDS || alive >= HARD_CAP_SECONDS) break
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

package com.zig.gravity.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zig.gravity.physics.BodyType
import com.zig.gravity.physics.EngineConstants
import com.zig.gravity.sim.BodyCatalog
import com.zig.gravity.sim.CameraState
import com.zig.gravity.sim.EffectKind
import com.zig.gravity.sim.SimulationViewModel
import com.zig.gravity.ui.theme.GravityColors
import com.zig.gravity.ui.theme.LocalGravityColors
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * §3.9 — THE single tabletop canvas.
 *
 * Rules honoured here:
 *  - state is read **only inside the draw lambda** (via `vm.frameTick`), so a running simulation
 *    causes zero recompositions;
 *  - the draw lambda allocates nothing: every Brush, Path and TextLayoutResult is preallocated
 *    and rebuilt only when `vm.visualEpoch` changes;
 *  - static layers (tabletop gradient, vignette) live in `drawWithCache`;
 *  - bodies are marbles: contact shadow -> radial-gradient base -> rim -> restrained specular ->
 *    selection ring -> cached label. No rotation, atmospheres or terminators;
 *  - Double -> Float conversion happens exactly once, at this boundary.
 */
private class SceneCache(capacity: Int) {
    val base = arrayOfNulls<Brush>(capacity)
    val shadow = arrayOfNulls<Brush>(capacity)
    val rim = Array(capacity) { Color.Transparent }
    val specular = Array(capacity) { Color.Transparent }
    val radiusPx = FloatArray(capacity)
    val trailOld = Array(capacity) { Path() }
    val trailNew = Array(capacity) { Path() }
    val prediction = Path()
    val arrow = Path()
    var count = 0

    // Stroke is a real object, so every one the draw phase needs is built once here and reused.
    var strokeTrailOld: Stroke = Stroke(width = 1f)
    var strokeTrailNew: Stroke = Stroke(width = 1f)
    var strokePrediction: Stroke = Stroke(width = 1f)

    /** §14 — the drag preview: thinner and dotted, so it reads as "not yet real". */
    var strokeGhost: Stroke = Stroke(width = 1f)
    var strokeRim: Stroke = Stroke(width = 1f)
    var strokeSelection: Stroke = Stroke(width = 1f)
    var strokeRing: Stroke = Stroke(width = 1f)
    var strokeRingInner: Stroke = Stroke(width = 1f)
    var strokeBary: Stroke = Stroke(width = 1f)
    var selectionPad: Float = 0f
    var baryArm: Float = 0f
    var baryRadius: Float = 0f
    var lineThin: Float = 0f
    var velocityWidth: Float = 0f
    var accelerationWidth: Float = 0f
    var velocityHead: Float = 0f
    var accelerationHead: Float = 0f
    var labelGap: Float = 0f
    var slingLength: Float = 0f
}

@Composable
fun TabletopCanvas(
    vm: SimulationViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalGravityColors.current
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()

    val cache = remember { SceneCache(EngineConstants.MAX_BODIES) }

    // Rebuilt only when the visual set, the theme or the density changes — never per frame.
    val epoch = vm.visualEpoch
    remember(epoch, colors.isDark, density.density) {
        val snap = vm.snapshot
        cache.count = snap.n
        for (i in 0 until snap.n) {
            val type = snap.typeOf(i)
            val tone = colors.bodyTone(BodyCatalog.colorOf(snap.catalogKey[i], type))
            val r = with(density) { snap.radiusDp[i].toFloat().dp.toPx() }.coerceAtLeast(1f)
            cache.radiusPx[i] = r
            val light = colors.highlightOf(tone)
            val dark = colors.shadeOf(tone)
            cache.base[i] = Brush.radialGradient(
                colorStops = arrayOf(0f to light, 0.45f to tone, 1f to dark),
                center = Offset(-r * 0.30f, -r * 0.34f),
                radius = r * 1.55f
            )
            cache.shadow[i] = Brush.radialGradient(
                colorStops = arrayOf(0f to colors.shadow, 0.6f to colors.shadow.copy(alpha = colors.shadow.alpha * 0.45f), 1f to Color.Transparent),
                center = Offset.Zero,
                radius = r * 1.35f
            )
            cache.rim[i] = lerp(tone, Color.White, if (colors.isDark) 0.22f else 0.10f).copy(alpha = 0.55f)
            cache.specular[i] = Color.White.copy(alpha = if (colors.isDark) 0.30f else 0.42f)
        }
        with(density) {
            cache.strokeTrailOld = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
            cache.strokeTrailNew = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
            cache.strokePrediction = Stroke(
                width = 1.6.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 9.dp.toPx()), 0f)
            )
            cache.strokeGhost = Stroke(
                width = 1.2.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(1.5.dp.toPx(), 5.dp.toPx()), 0f)
            )
            cache.strokeRim = Stroke(width = 1.dp.toPx())
            cache.strokeSelection = Stroke(width = 1.5.dp.toPx())
            cache.strokeRing = Stroke(width = 1.4.dp.toPx())
            cache.strokeRingInner = Stroke(width = 1.dp.toPx())
            cache.strokeBary = Stroke(width = 1.2.dp.toPx())
            cache.selectionPad = 5.dp.toPx()
            cache.baryArm = 6.dp.toPx()
            cache.baryRadius = 3.dp.toPx()
            cache.lineThin = 1.dp.toPx()
            cache.velocityWidth = 1.8.dp.toPx()
            cache.accelerationWidth = 1.4.dp.toPx()
            cache.velocityHead = 5.dp.toPx()
            cache.accelerationHead = 4.dp.toPx()
            cache.labelGap = 8.dp.toPx()
            cache.slingLength = 30.dp.toPx()
        }
        epoch
    }

    val labelStyle = TextStyle(color = colors.onSurfaceDim, fontSize = 11.sp)
    val selectedLabel: TextLayoutResult? = remember(epoch, vm.selectedId, vm.persian, colors.isDark) {
        val snap = vm.snapshot
        val slot = snap.slotOfId(vm.selectedId)
        if (slot < 0) null
        else measurer.measure(
            text = BodyCatalog.nameOf(snap.catalogKey[slot], snap.typeOf(slot), vm.persian),
            style = labelStyle
        )
    }

    val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(6f, 10f), 0f) }

    Spacer(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                if (size.width > 0) {
                    vm.onViewportChanged(size.width / density.density.toDouble())
                }
            }
            .drawWithCache {
                val table = Brush.verticalGradient(
                    colors = listOf(colors.tableTop, colors.tableBottom),
                    startY = 0f,
                    endY = size.height
                )
                val vignette = Brush.radialGradient(
                    colorStops = arrayOf(0.35f to Color.Transparent, 1f to colors.vignette),
                    center = Offset(size.width * 0.5f, size.height * 0.36f),
                    radius = size.maxDimension * 0.78f
                )
                onDrawBehind {
                    drawRect(table)
                    drawRect(vignette)
                    drawScene(vm, colors, cache, selectedLabel, dashEffect)
                }
            }
    )
}

private fun DrawScope.drawScene(
    vm: SimulationViewModel,
    colors: GravityColors,
    cache: SceneCache,
    label: TextLayoutResult?,
    dashEffect: PathEffect
) {
    // The ONLY per-frame state read, and it happens in the draw phase. Reading it here is what
    // invalidates the canvas each frame without ever triggering a recomposition.
    val tick = vm.frameTick
    if (tick < 0) return

    val snap = vm.snapshot
    val cam = vm.camera
    val w = size.width
    val h = size.height

    // §1 — the one and only screen mapping in the app. Pan, zoom, orientation and elevation all
    // live in CameraState, and hit testing uses the exact inverse of these two functions.
    fun sx(x: Double, y: Double): Float = cam.toScreenX(x, y, w, h)
    fun sy(x: Double, y: Double): Float = cam.toScreenY(x, y, w, h)

    val density = this.density
    val n = minOf(snap.n, cache.count)
    // §11 — drawn size follows the documented display policy, not the raw zoom.
    val displayScale = (CameraState.displayScale(cam.zoom)).toFloat()
    val minDrawPx = (CameraState.MIN_DRAW_DP * density).toFloat()

    // ---- 1. trails: where each body has already been ---------------------------------------
    if (vm.trailsVisible) {
        val rings = snap.trails
        if (rings != null) {
            for (i in 0 until n) {
                val ring = rings[i]
                val count = ring.count
                if (count < 2) continue
                val split = count * 2 / 3
                val old = cache.trailOld[i]
                val recent = cache.trailNew[i]
                old.rewind()
                recent.rewind()
                for (p in 0 until count) {
                    val rx = ring.xAt(p)
                    val ry = ring.yAt(p)
                    val px = sx(rx, ry)
                    val py = sy(rx, ry)
                    if (p <= split) {
                        if (p == 0) old.moveTo(px, py) else old.lineTo(px, py)
                    }
                    if (p >= split) {
                        if (p == split) recent.moveTo(px, py) else recent.lineTo(px, py)
                    }
                }
                drawPath(old, colors.trail.copy(alpha = colors.trail.alpha * 0.35f), style = cache.strokeTrailOld)
                drawPath(recent, colors.trail, style = cache.strokeTrailNew)
            }
        }
    }

    // ---- 2. predicted trajectory (test-particle, never mutates the simulation) ---------------
    //
    // §13/§14 — while a body is being dragged this same path becomes the GHOST: the answer to
    // "if I let go here, what happens next?". It is drawn fainter and dotted rather than dashed so
    // it can never be confused with the solid historical trail, and it is tinted when the previewed
    // path escapes the neighbourhood instead of coming back around.
    val predCount = vm.predictionCount
    val ghost = vm.predictionIsGhost
    if (predCount > 1) {
        val path = cache.prediction
        path.rewind()
        for (p in 0 until predCount) {
            val qx = vm.predictionXY[p * 2]
            val qy = vm.predictionXY[p * 2 + 1]
            val px = sx(qx, qy)
            val py = sy(qx, qy)
            if (p == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        val tone = when {
            ghost && vm.predictionEscapes -> colors.acceleration.copy(alpha = 0.75f)
            ghost -> colors.prediction.copy(alpha = 0.55f)
            else -> colors.prediction
        }
        drawPath(
            path = path,
            color = tone,
            style = if (ghost) cache.strokeGhost else cache.strokePrediction
        )
    }

    // ---- 2b. §13 the origin marker: where the body was before the finger picked it up ----------
    if (ghost) {
        val ox = sx(vm.dragOriginX, vm.dragOriginY)
        val oy = sy(vm.dragOriginX, vm.dragOriginY)
        val slot = snap.slotOfId(vm.draggingId)
        if (slot >= 0) {
            val r = (cache.radiusPx[slot] * displayScale).coerceAtLeast(minDrawPx)
            drawCircle(
                color = colors.onSurfaceDim.copy(alpha = 0.35f),
                radius = r,
                center = Offset(ox, oy),
                style = cache.strokeGhost
            )
            drawLine(
                color = colors.onSurfaceDim.copy(alpha = 0.22f),
                start = Offset(ox, oy),
                end = Offset(sx(snap.x[slot], snap.y[slot]), sy(snap.x[slot], snap.y[slot])),
                strokeWidth = cache.lineThin,
                pathEffect = dashEffect
            )
        }
    }

    // ---- 3. bodies ---------------------------------------------------------------------------
    for (i in 0 until n) {
        val type = snap.typeOf(i)
        val px = sx(snap.x[i], snap.y[i])
        val py = sy(snap.x[i], snap.y[i])
        val r = (cache.radiusPx[i] * displayScale).coerceAtLeast(minDrawPx)
        val bodyScale = if (cache.radiusPx[i] > 0f) r / cache.radiusPx[i] else 1f
        val selected = snap.id[i] == vm.selectedId

        when (type) {
            BodyType.WORMHOLE_MOUTH -> {
                val warm = snap.partnerId[i] > snap.id[i]
                val tint = if (warm) colors.wormholeWarm else colors.wormholeCool
                // Gentle synchronised pulse driven by simulated time, not by an animation clock.
                val pulse = 0.75f + 0.25f * sin(snap.simTime / 4.0e5).toFloat()
                drawCircle(tint.copy(alpha = 0.10f * pulse), r * 1.25f, Offset(px, py))
                drawCircle(tint.copy(alpha = 0.85f), r, Offset(px, py), style = cache.strokeRing)
                drawCircle(tint.copy(alpha = 0.35f), r * 0.55f, Offset(px, py), style = cache.strokeRingInner)
            }

            BodyType.BLACK_HOLE -> {
                drawCircle(colors.blackHoleRing.copy(alpha = 0.08f), r * 1.5f, Offset(px, py))
                drawCircle(colors.blackHoleBody, r, Offset(px, py))
                // The ring IS the capture radius (§3.12, one shared constant).
                drawCircle(colors.blackHoleRing.copy(alpha = 0.9f), r, Offset(px, py), style = cache.strokeRing)
            }

            else -> {
                // The cached marble brushes were built for cache.radiusPx; scaling the canvas
                // around the body reuses them at any zoom without rebuilding a single Brush.
                translate(px, py + r * 0.42f) {
                    scale(bodyScale, bodyScale, Offset.Zero) {
                        cache.shadow[i]?.let { drawCircle(it, cache.radiusPx[i] * 1.30f, Offset.Zero) }
                    }
                }
                translate(px, py) {
                    scale(bodyScale, bodyScale, Offset.Zero) {
                        val rr = cache.radiusPx[i]
                        cache.base[i]?.let { drawCircle(it, rr, Offset.Zero) }
                        drawCircle(cache.rim[i], rr, Offset.Zero, style = cache.strokeRim)
                        drawCircle(cache.specular[i], rr * 0.17f, Offset(-rr * 0.34f, -rr * 0.38f))
                    }
                }
            }
        }

        if (selected) {
            drawCircle(
                color = colors.selection,
                radius = r + cache.selectionPad,
                center = Offset(px, py),
                style = cache.strokeSelection
            )
        }
    }

    // ---- 4. vectors for the selected body ------------------------------------------------------
    val selSlot = snap.slotOfId(vm.selectedId)
    if (vm.showVectors && selSlot >= 0) {
        val px = sx(snap.x[selSlot], snap.y[selSlot])
        val py = sy(snap.x[selSlot], snap.y[selSlot])
        val r = (cache.radiusPx[selSlot] * displayScale).coerceAtLeast(minDrawPx)

        val v = sqrt(snap.vx[selSlot] * snap.vx[selSlot] + snap.vy[selSlot] * snap.vy[selSlot])
        if (v > 0.0) {
            // Readable length: 34..96 dp, log-scaled against a 30 km/s reference.
            val lenDp = (34.0 + 26.0 * kotlin.math.ln(1.0 + v / 3.0e4)).coerceIn(34.0, 96.0)
            drawArrow(
                cache.arrow, px, py,
                (snap.vx[selSlot] / v).toFloat(), (-snap.vy[selSlot] / v).toFloat(),
                r + lenDp.toFloat() * density, r, colors.velocity, cache.velocityWidth, cache.velocityHead
            )
        }
        val a = sqrt(snap.ax[selSlot] * snap.ax[selSlot] + snap.ay[selSlot] * snap.ay[selSlot])
        if (a > 0.0) {
            val lenDp = (26.0 + 22.0 * kotlin.math.ln(1.0 + a / 5.0e-3)).coerceIn(26.0, 78.0)
            drawArrow(
                cache.arrow, px, py,
                (snap.ax[selSlot] / a).toFloat(), (-snap.ay[selSlot] / a).toFloat(),
                r + lenDp.toFloat() * density, r, colors.acceleration, cache.accelerationWidth, cache.accelerationHead
            )
        }
    }

    // ---- 5. barycentre --------------------------------------------------------------------------
    if (vm.showBarycenter && snap.n > 0) {
        val bx = sx(snap.barycenter[0], snap.barycenter[1])
        val by = sy(snap.barycenter[0], snap.barycenter[1])
        val arm = cache.baryArm
        drawCircle(colors.barycenter, cache.baryRadius, Offset(bx, by), style = cache.strokeBary)
        drawLine(colors.barycenter, Offset(bx - arm, by), Offset(bx + arm, by), strokeWidth = cache.lineThin)
        drawLine(colors.barycenter, Offset(bx, by - arm), Offset(bx, by + arm), strokeWidth = cache.lineThin)
    }

    // ---- 6. slingshot aim -------------------------------------------------------------------------
    if (vm.slingshotActive) {
        val slot = snap.slotOfId(vm.slingshotArmedId)
        if (slot >= 0) {
            val vX = vm.slingshotVx
            val vY = vm.slingshotVy
            val speed = sqrt(vX * vX + vY * vY)
            if (speed > 0.0) {
                val px = sx(snap.x[slot], snap.y[slot])
                val py = sy(snap.x[slot], snap.y[slot])
                drawLine(
                    color = colors.accent.copy(alpha = 0.55f),
                    start = Offset(px, py),
                    end = Offset(
                        px - (vX / speed).toFloat() * cache.slingLength,
                        py + (vY / speed).toFloat() * cache.slingLength
                    ),
                    strokeWidth = cache.lineThin,
                    pathEffect = dashEffect
                )
            }
        }
    }

    // ---- 6b. §13 impact effects ---------------------------------------------------------------
    val fx = vm.effects
    for (e in 0 until fx.maxEffects) {
        if (!fx.active[e]) continue
        val kind = fx.kind[e] ?: continue
        val t = fx.progress(e).toFloat()
        val sev = fx.severity[e].toFloat()
        val ox = sx(fx.originX[e], fx.originY[e])
        val oy = sy(fx.originX[e], fx.originY[e])
        val ringPx = (fx.ringRadius[e] * cam.pxPerMeter(w)).toFloat()
        val tone = colors.bodyTone(fx.tint[e])
        // Ease-out: fast at the moment of contact, then a calm settle. Never a cartoon boom.
        val ease = 1f - (1f - t) * (1f - t)
        val fade = (1f - t).coerceIn(0f, 1f)

        when (kind) {
            EffectKind.ACCRETION -> {
                // §15 — an inward pull, never an outward blast.
                drawCircle(
                    color = colors.blackHoleRing.copy(alpha = 0.28f * fade),
                    radius = ringPx * (2.6f - 1.4f * ease),
                    center = Offset(ox, oy),
                    style = cache.strokeRing
                )
            }
            EffectKind.TRAVERSAL -> {
                drawCircle(
                    color = colors.wormholeWarm.copy(alpha = 0.45f * fade),
                    radius = ringPx * (0.6f + 0.9f * ease),
                    center = Offset(ox, oy),
                    style = cache.strokeRing
                )
            }
            else -> {
                // §9 — three visibly different grades, driven by the physics, not by taste.
                //
                //   BOUNCE  (low)      a compression pulse and a small flash. Nothing leaves.
                //   MERGE   (moderate) a brighter flash and a single expanding ring, plus debris.
                //   SHATTER (high)     a brief hard flash, radial fragments, and a fast shockwave
                //                      ring running out ahead of a slower dust ring.
                //
                // Even the top grade is a short, restrained event: a collision is not a nuclear
                // detonation, and the sandbox never pretends otherwise.
                val flash = (1f - t * 3f).coerceIn(0f, 1f)
                val flashGain = when (kind) {
                    EffectKind.BOUNCE -> 0.30f
                    EffectKind.SHATTER -> 0.75f
                    else -> 0.50f
                }
                if (flash > 0f) {
                    drawCircle(
                        color = Color.White.copy(alpha = flashGain * flash * (0.4f + 0.6f * sev)),
                        radius = ringPx * 0.55f * (0.6f + 0.8f * flash),
                        center = Offset(ox, oy)
                    )
                }

                if (kind == EffectKind.BOUNCE) {
                    // Compression: the ring starts wide and squeezes IN, reading as the two
                    // surfaces pressing together rather than as anything being thrown out.
                    drawCircle(
                        color = colors.accent.copy(alpha = 0.30f * fade * (0.35f + 0.65f * sev)),
                        radius = ringPx * (1.25f - 0.55f * ease),
                        center = Offset(ox, oy),
                        style = cache.strokeRingInner
                    )
                } else {
                    drawCircle(
                        color = colors.accent.copy(alpha = 0.40f * fade * (0.35f + 0.65f * sev)),
                        radius = ringPx * ease,
                        center = Offset(ox, oy),
                        style = cache.strokeRing
                    )
                }

                if (kind == EffectKind.SHATTER) {
                    // The shockwave outruns the debris and fades first; behind it a wider, much
                    // fainter dust front follows.
                    val shock = (t * 2.2f).coerceAtMost(1f)
                    drawCircle(
                        color = Color.White.copy(alpha = 0.22f * (1f - shock) * (0.4f + 0.6f * sev)),
                        radius = ringPx * (0.4f + 1.5f * shock),
                        center = Offset(ox, oy),
                        style = cache.strokeRingInner
                    )
                    drawCircle(
                        color = tone.copy(alpha = 0.10f * fade * sev),
                        radius = ringPx * (0.6f + 1.1f * ease),
                        center = Offset(ox, oy)
                    )
                }
            }
        }

        val pc = fx.particleCount[e]
        if (pc > 0) {
            val alpha = fade * fade
            val accretion = kind == EffectKind.ACCRETION
            for (p in 0 until pc) {
                val idx = fx.particleIndex(e, p)
                val dx = sx(fx.pxArr[idx], fx.pyArr[idx])
                val dy = sy(fx.pxArr[idx], fx.pyArr[idx])
                val size = (fx.pSize[idx] * cam.pxPerMeter(w)).toFloat() * (1f - 0.55f * t)
                if (size <= 0.2f) continue

                if (accretion) {
                    // §12 — tidal stretching. Each fragment is drawn as a streak pointing at the
                    // hole, and the streak lengthens as it falls in, because the near side is
                    // pulled harder than the far side. It also reddens and dims on the way down.
                    var vx = ox - dx
                    var vy = oy - dy
                    val len = kotlin.math.sqrt(vx * vx + vy * vy)
                    if (len > 0.001f) {
                        vx /= len
                        vy /= len
                        val stretch = size * (1.5f + 6.0f * ease)
                        val redshift = lerp(tone, colors.acceleration, 0.25f + 0.55f * ease)
                        drawLine(
                            color = redshift.copy(alpha = alpha),
                            start = Offset(dx - vx * stretch * 0.35f, dy - vy * stretch * 0.35f),
                            end = Offset(dx + vx * stretch * 0.65f, dy + vy * stretch * 0.65f),
                            strokeWidth = size * 1.1f,
                            cap = StrokeCap.Round
                        )
                        continue
                    }
                }
                drawCircle(tone.copy(alpha = alpha), size, Offset(dx, dy))
            }
        }
    }

    // ---- 6c. §11 the followed body gets its own quiet marker ----------------------------------
    if (vm.followTargetId != 0L) {
        val fSlot = snap.slotOfId(vm.followTargetId)
        if (fSlot >= 0) {
            val fx0 = sx(snap.x[fSlot], snap.y[fSlot])
            val fy0 = sy(snap.x[fSlot], snap.y[fSlot])
            val fr = (cache.radiusPx[fSlot] * displayScale).coerceAtLeast(minDrawPx) +
                cache.selectionPad * 2.2f
            // A pair of short arcs rather than a full ring, so it reads as a reticle and never
            // competes with the solid selection outline drawn above.
            val sweep = 46f
            for (start in floatArrayOf(-23f, 157f)) {
                drawArc(
                    color = colors.accent.copy(alpha = 0.75f),
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(fx0 - fr, fy0 - fr),
                    size = androidx.compose.ui.geometry.Size(fr * 2f, fr * 2f),
                    style = cache.strokeRing
                )
            }
        }
    }

    // ---- 7. cached label for the selected body ------------------------------------------------------
    if (label != null && selSlot >= 0) {
        val px = sx(snap.x[selSlot], snap.y[selSlot])
        val py = sy(snap.x[selSlot], snap.y[selSlot])
        val r = (cache.radiusPx[selSlot] * displayScale).coerceAtLeast(minDrawPx)
        drawText(
            textLayoutResult = label,
            topLeft = Offset(px - label.size.width / 2f, py + r + cache.labelGap)
        )
    }
}

/** Allocation-free arrow: one preallocated Path, rewound each call. */
private fun DrawScope.drawArrow(
    path: Path,
    originX: Float,
    originY: Float,
    dirX: Float,
    dirY: Float,
    length: Float,
    startOffset: Float,
    color: Color,
    strokeWidth: Float,
    headSize: Float
) {
    val sx = originX + dirX * startOffset
    val sy = originY + dirY * startOffset
    val ex = originX + dirX * length
    val ey = originY + dirY * length
    if (abs(ex - sx) < 0.5f && abs(ey - sy) < 0.5f) return
    drawLine(color, Offset(sx, sy), Offset(ex, ey), strokeWidth = strokeWidth, cap = StrokeCap.Round)

    val angle = atan2(dirY.toDouble(), dirX.toDouble())
    val left = angle + 2.55
    val right = angle - 2.55
    path.rewind()
    path.moveTo(ex, ey)
    path.lineTo(ex + (cos(left) * headSize).toFloat(), ey + (sin(left) * headSize).toFloat())
    path.lineTo(ex + (cos(right) * headSize).toFloat(), ey + (sin(right) * headSize).toFloat())
    path.close()
    drawPath(path, color)
}

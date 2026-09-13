package com.zig.gravity.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zig.gravity.physics.BodyType
import com.zig.gravity.physics.EngineConstants
import com.zig.gravity.sim.BodyCatalog
import com.zig.gravity.sim.CameraState
import com.zig.gravity.sim.EffectKind
import com.zig.gravity.sim.SimulationViewModel
import com.zig.gravity.ui.theme.BodyIdentities
import com.zig.gravity.ui.theme.GravityColors
import com.zig.gravity.ui.theme.LocalGravityColors
import com.zig.gravity.ui.theme.MarkEdge
import com.zig.gravity.ui.theme.MarkKind
import com.zig.gravity.ui.theme.MarkRole
import com.zig.gravity.ui.theme.SphereProjection
import com.zig.gravity.ui.theme.StarPattern
import com.zig.gravity.ui.theme.TableSurfaces
import com.zig.gravity.ui.theme.brushFor
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * §1 — the three feature-edge falloffs, shared by every mark of every body.
 *
 * One brush per profile, built once for the whole canvas: a mark's colour and strength travel
 * through a tint filter and the draw alpha, so a hundred features on the table still share three
 * gradients. SOFT is atmosphere and granulation, FAIR a mare or a coastline, CRISP a crater rim.
 * All three are radial in the unit disc and reach zero alpha exactly at its edge, which is what
 * lets a projected cap fade out instead of showing a decal boundary.
 */
private val EDGE_BRUSHES: Array<Brush> = arrayOf(
    // MarkEdge.SOFT
    Brush.radialGradient(
        colorStops = arrayOf(
            0.00f to Color.White,
            0.35f to Color.White.copy(alpha = 0.93f),
            0.60f to Color.White.copy(alpha = 0.66f),
            0.78f to Color.White.copy(alpha = 0.32f),
            0.90f to Color.White.copy(alpha = 0.11f),
            1.00f to Color.Transparent
        ),
        center = Offset.Zero,
        radius = 1f
    ),
    // MarkEdge.FAIR
    Brush.radialGradient(
        colorStops = arrayOf(
            0.00f to Color.White,
            0.50f to Color.White.copy(alpha = 0.97f),
            0.72f to Color.White.copy(alpha = 0.80f),
            0.86f to Color.White.copy(alpha = 0.42f),
            0.95f to Color.White.copy(alpha = 0.13f),
            1.00f to Color.Transparent
        ),
        center = Offset.Zero,
        radius = 1f
    ),
    // MarkEdge.CRISP
    Brush.radialGradient(
        colorStops = arrayOf(
            0.00f to Color.White,
            0.74f to Color.White.copy(alpha = 0.98f),
            0.86f to Color.White.copy(alpha = 0.86f),
            0.94f to Color.White.copy(alpha = 0.50f),
            0.98f to Color.White.copy(alpha = 0.16f),
            1.00f to Color.Transparent
        ),
        center = Offset.Zero,
        radius = 1f
    )
)

/**
 * §1 — how each feature role composites with the light already on the marble. Multiply and Screen
 * are the albedo operations: a multiplied crater floor darkens whatever the shading put there and
 * a screened cloud brightens it, so detail follows the sphere instead of sitting on top of it.
 * Indexed by [MarkRole.ordinal].
 */
private val ROLE_BLEND: Array<BlendMode> = arrayOf(
    BlendMode.SrcOver,  // MarkRole.TINT
    BlendMode.Multiply, // MarkRole.DARKEN
    BlendMode.Screen    // MarkRole.LIGHTEN
)

/**
 * §3.9 — THE single tabletop canvas.
 *
 * Rules honoured here:
 *  - state is read **only inside the draw lambda** (via `vm.frameTick`), so a running simulation
 *    causes zero recompositions;
 *  - the draw lambda allocates nothing: every Brush, Path and TextLayoutResult is preallocated
 *    and rebuilt only when `vm.visualEpoch` changes;
 *  - static layers (tabletop gradient, vignette) live in `drawWithCache`;
 *  - bodies are spheres: two-layer cast shadow -> radial-gradient base -> projected latitude belts
 *    -> projected surface caps (multiplied or screened, so they inherit the light) -> the same base
 *    gradient re-applied -> directional terminator -> limb darkening -> lit-limb rim and atmosphere
 *    -> per-body highlight -> selection ring -> cached label. No rotation, no animated texture;
 *  - the table itself is a designed material finish (gradient, static printed pattern, vignette)
 *    built once per size in `drawWithCache`;
 *  - Double -> Float conversion happens exactly once, at this boundary.
 */
private class SceneCache(capacity: Int) {
    val base = arrayOfNulls<Brush>(capacity)

    /** §3 — the umbra: the small, darker core of the cast shadow. */
    val shadow = arrayOfNulls<Brush>(capacity)

    /** §3 — the penumbra: the wider, softer outer shadow, elongated along the light direction. */
    val softShadow = arrayOfNulls<Brush>(capacity)
    val rim = Array(capacity) { Color.Transparent }

    /**
     * §1 planet identity — spherical caps, projected once per visual epoch and flattened into
     * preallocated arrays so the draw phase only walks indices. Centres and semi-axes are fractions
     * of [radiusPx], so they ride the same `scale(bodyScale)` transform the marble brushes do and
     * survive any zoom without a rebuild; the rotation is the direction the cap's foreshortened
     * axis points, which is what makes a feature bend around the limb instead of lying flat.
     */
    val blobCount = IntArray(capacity)
    val blobCx = FloatArray(capacity * BodyIdentities.MAX_MARKS)
    val blobCy = FloatArray(capacity * BodyIdentities.MAX_MARKS)
    val blobMajor = FloatArray(capacity * BodyIdentities.MAX_MARKS)
    val blobMinor = FloatArray(capacity * BodyIdentities.MAX_MARKS)
    val blobDegrees = FloatArray(capacity * BodyIdentities.MAX_MARKS)
    val blobAlpha = FloatArray(capacity * BodyIdentities.MAX_MARKS)
    val blobEdge = IntArray(capacity * BodyIdentities.MAX_MARKS)
    val blobRole = IntArray(capacity * BodyIdentities.MAX_MARKS)
    val blobTint = arrayOfNulls<ColorFilter>(capacity * BodyIdentities.MAX_MARKS)

    /**
     * Latitude belts: each one becomes [BodyIdentities.BAND_SLICES] stroked arcs of the ellipse its
     * latitude circle traces, at a smooth alpha ramp across the belt, so a belt curves with the
     * sphere exactly as the ring system does and its cross-section has no staircase.
     */
    val bandCount = IntArray(capacity)
    val bandColor = Array(capacity * BodyIdentities.MAX_MARKS) { Color.Transparent }
    val bandRole = IntArray(capacity * BodyIdentities.MAX_MARKS)
    val bandStroke = arrayOfNulls<Stroke>(capacity * BodyIdentities.MAX_MARKS)
    private val slices = BodyIdentities.MAX_MARKS * BodyIdentities.BAND_SLICES
    val bandArcA = FloatArray(capacity * slices)
    val bandArcB = FloatArray(capacity * slices)
    val bandArcY = FloatArray(capacity * slices)
    val bandStart = FloatArray(capacity * slices)
    val bandSweep = FloatArray(capacity * slices)
    val bandSliceAlpha = FloatArray(capacity * slices)

    /**
     * The light layers that give the marble its curvature: a directional terminator, a symmetric
     * limb fall-off in the body's own deep tone, the emissive corona and core of a star, and the
     * per-body highlight. All built here, once per visual epoch; the draw phase only issues them.
     */
    val termBrush = arrayOfNulls<Brush>(capacity)
    val limbBrush = arrayOfNulls<Brush>(capacity)
    val glowBrush = arrayOfNulls<Brush>(capacity)
    val coreBrush = arrayOfNulls<Brush>(capacity)
    val atmoColor = Array(capacity) { Color.Transparent }
    val atmosphere = FloatArray(capacity)
    val specRadius = FloatArray(capacity)
    val specCx = FloatArray(capacity)
    val specCy = FloatArray(capacity)
    val specAlpha = FloatArray(capacity)
    val specEdge = IntArray(capacity)
    val specTint = arrayOfNulls<ColorFilter>(capacity)

    /**
     * Ring systems (Saturn). Purely visual: these never touch mass, radius, collision or selection
     * geometry — the simulation cannot see them. The far half of each band is drawn under the
     * sphere and the near half over it, which is what makes the rings pass behind and in front
     * without a single clip path.
     */
    val ringCount = IntArray(capacity)
    val ringRadius = FloatArray(capacity * BodyIdentities.MAX_RINGS)
    val ringSquash = FloatArray(capacity)
    val ringColor = Array(capacity * BodyIdentities.MAX_RINGS) { Color.Transparent }
    val ringStroke = arrayOfNulls<Stroke>(capacity * BodyIdentities.MAX_RINGS)
    val ringHalo = arrayOfNulls<Stroke>(capacity * BodyIdentities.MAX_RINGS)

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

/**
 * §5 — one surface's printed dot field, resolved to pixels for a single canvas size.
 *
 * Built inside `drawWithCache`, so it costs nothing per frame: the draw phase only walks
 * preallocated float arrays and issues plain filled circles. No glow, no twinkle, no lines.
 */
internal class SurfacePrint(
    val ink: Color,
    val xPx: FloatArray,
    val yPx: FloatArray,
    val radii: FloatArray,
    val alphas: FloatArray
) {
    fun draw(scope: DrawScope) {
        with(scope) {
            for (i in xPx.indices) {
                drawCircle(ink.copy(alpha = alphas[i]), radii[i], Offset(xPx[i], yPx[i]))
            }
        }
    }

    companion object {
        /** [limit] caps the dot count for miniature previews; a negative value prints them all. */
        fun of(pattern: StarPattern, size: Size, density: Density, limit: Int = -1): SurfacePrint {
            val all = TableSurfaces.starDots(pattern)
            val dots = if (limit in 0 until all.size) all.subList(0, limit) else all
            val xPx = FloatArray(dots.size)
            val yPx = FloatArray(dots.size)
            val radii = FloatArray(dots.size)
            val alphas = FloatArray(dots.size)
            with(density) {
                for (i in dots.indices) {
                    xPx[i] = dots[i].xFraction * size.width
                    yPx[i] = dots[i].yFraction * size.height
                    radii[i] = dots[i].radiusDp.dp.toPx().coerceAtLeast(0.4f)
                    alphas[i] = dots[i].alpha
                }
            }
            return SurfacePrint(Color(pattern.dotArgb), xPx, yPx, radii, alphas)
        }
    }
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

    // Rebuilt only when the visual set, the table surface or the density changes — never per frame.
    val epoch = vm.visualEpoch
    remember(epoch, colors, density.density) {
        val snap = vm.snapshot
        cache.count = snap.n
        for (i in 0 until snap.n) {
            val type = snap.typeOf(i)
            val tone = colors.bodyTone(BodyCatalog.colorOf(snap.catalogKey[i], type))
            val r = with(density) { snap.radiusDp[i].toFloat().dp.toPx() }.coerceAtLeast(1f)
            cache.radiusPx[i] = r
            val light = colors.highlightOf(tone)
            // §2 — the authoritative deep stop when the palette has one; otherwise derived exactly
            // as before. The marble structure (offset centre, three stops) is untouched.
            val deepArgb = BodyCatalog.deepColorOf(snap.catalogKey[i], type)
            val dark = if (deepArgb != 0L) colors.bodyTone(deepArgb) else colors.shadeOf(tone)
            cache.base[i] = Brush.radialGradient(
                colorStops = arrayOf(0f to light, 0.45f to tone, 1f to dark),
                center = Offset(-r * 0.30f, -r * 0.34f),
                radius = r * 1.55f
            )
            // §3 — light comes from the top-left, so both shadow layers fall to the bottom-right.
            // Two soft radial gradients, no setShadowLayer and no blur modifier.
            cache.shadow[i] = Brush.radialGradient(
                colorStops = arrayOf(0f to colors.shadow, 0.6f to colors.shadow.copy(alpha = colors.shadow.alpha * 0.45f), 1f to Color.Transparent),
                center = Offset.Zero,
                radius = r * 0.40f
            )
            cache.softShadow[i] = Brush.radialGradient(
                colorStops = arrayOf(0f to colors.shadowSoft, 0.6f to colors.shadowSoft.copy(alpha = colors.shadowSoft.alpha * 0.45f), 1f to Color.Transparent),
                center = Offset.Zero,
                radius = r * 0.725f
            )
            cache.rim[i] = lerp(tone, Color.White, if (colors.isDark) 0.22f else 0.10f).copy(alpha = 0.34f)

            // §1 — the planet identity layer, resolved here and never again until the visual set
            // changes. Marks are authored on the sphere and projected here, once: the draw phase
            // only walks the resulting floats. Colours go through the same bodyTone() as the base,
            // so the layer follows the table's chrome mode like everything else, and a feature's
            // colour travels in a tint filter while its strength travels in the draw alpha.
            val identity = BodyIdentities.of(snap.catalogKey[i], type)
            var bc = 0
            var dc = 0
            for (mark in identity.marks) {
                if (bc + dc >= BodyIdentities.MAX_MARKS) break
                if (mark.kind == MarkKind.BLOB) {
                    val slot = i * BodyIdentities.MAX_MARKS + bc
                    val pr = SphereProjection.projectBlob(mark.cx, mark.cy, mark.rx, mark.ry)
                    cache.blobCx[slot] = pr.cx
                    cache.blobCy[slot] = pr.cy
                    cache.blobMajor[slot] = pr.major
                    cache.blobMinor[slot] = pr.minor
                    cache.blobDegrees[slot] = pr.degrees
                    cache.blobAlpha[slot] = min(
                        1f,
                        mark.alpha * if (mark.role == MarkRole.DARKEN) {
                            BodyIdentities.DARKEN_STRENGTH
                        } else {
                            BodyIdentities.LIGHTEN_STRENGTH
                        }
                    )
                    cache.blobEdge[slot] = mark.edge.ordinal
                    cache.blobRole[slot] = mark.role.ordinal
                    cache.blobTint[slot] = ColorFilter.tint(colors.bodyTone(mark.argb), BlendMode.SrcIn)
                    bc++
                } else {
                    val slot = i * BodyIdentities.MAX_MARKS + dc
                    // The belt's projected vertical scale is the chord of its apex height, so the
                    // slices tile the belt's true thickness and never overrun it.
                    val chord = sqrt(max(0f, 1f - mark.cy * mark.cy))
                    val sliceWidth = (2f * mark.ry / BodyIdentities.BAND_SLICES) * chord
                    val arc = SphereProjection.projectBand(
                        mark.cy, identity.axisTilt, sliceWidth * 1.15f / 2f
                    )
                    cache.bandColor[slot] = colors.bodyTone(mark.argb)
                    cache.bandRole[slot] = mark.role.ordinal
                    cache.bandStroke[slot] = Stroke(width = (sliceWidth * r * 1.15f).coerceAtLeast(0.5f))
                    val firstSlice = slot * BodyIdentities.BAND_SLICES
                    for (k in 0 until BodyIdentities.BAND_SLICES) {
                        val s = firstSlice + k
                        val offset = (k - (BodyIdentities.BAND_SLICES - 1) / 2f) * sliceWidth
                        cache.bandArcA[s] = arc.a
                        cache.bandArcB[s] = arc.b
                        cache.bandArcY[s] = arc.yc + offset
                        cache.bandStart[s] = arc.startDegrees
                        cache.bandSweep[s] = arc.sweepDegrees
                        cache.bandSliceAlpha[s] = min(
                            1f,
                            BodyIdentities.BAND_PROFILE[k] * mark.alpha *
                                if (mark.role == MarkRole.DARKEN) {
                                    BodyIdentities.DARKEN_STRENGTH
                                } else {
                                    BodyIdentities.LIGHTEN_STRENGTH
                                }
                        )
                    }
                    dc++
                }
            }
            cache.blobCount[i] = bc
            cache.bandCount[i] = dc

            // §1 — the light layers. The terminator is a linear gradient along the light axis whose
            // stops sample the sphere's own normal along that axis; the limb fall-off is the linear
            // limb-darkening law as a radial gradient in the body's deep tone. Both are built once.
            val limbTone = if (identity.limbArgb != 0L) colors.bodyTone(identity.limbArgb) else dark
            val limbAlphas = SphereProjection.limbAlphas(identity.lighting.limbDarkening)
            cache.limbBrush[i] = Brush.radialGradient(
                colorStops = Array(limbAlphas.size) { s ->
                    (s / (limbAlphas.size - 1f)) to limbTone.copy(alpha = limbAlphas[s] * BodyIdentities.LIMB_STRENGTH)
                },
                center = Offset.Zero,
                radius = r
            )
            val termAlphas = SphereProjection.terminatorAlphas()
            cache.termBrush[i] = Brush.linearGradient(
                colorStops = Array(termAlphas.size) { s ->
                    (s / (termAlphas.size - 1f)) to limbTone.copy(alpha = termAlphas[s] * BodyIdentities.TERMINATOR_STRENGTH)
                },
                start = Offset(-SphereProjection.AXIS_X * r, -SphereProjection.AXIS_Y * r),
                end = Offset(SphereProjection.AXIS_X * r, SphereProjection.AXIS_Y * r)
            )
            val lighting = identity.lighting
            cache.specRadius[i] = lighting.specularRadius * r
            cache.specCx[i] = lighting.specularCx * r
            cache.specCy[i] = lighting.specularCy * r
            cache.specAlpha[i] = lighting.specularAlpha * if (colors.isDark) 1f else 1.4f
            cache.specEdge[i] = lighting.specularEdge.ordinal
            cache.specTint[i] = ColorFilter.tint(Color.White, BlendMode.SrcIn)
            cache.atmosphere[i] = lighting.atmosphere
            cache.atmoColor[i] = light
            if (lighting.emissive > 0f) {
                val glow = colors.bodyTone(BodyIdentities.SUN_CORONA_ARGB)
                cache.glowBrush[i] = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to Color.Transparent,
                        0.62f to Color.Transparent,
                        0.66f to glow.copy(alpha = 0.30f * lighting.emissive),
                        0.80f to glow.copy(alpha = 0.16f * lighting.emissive),
                        1.00f to Color.Transparent
                    ),
                    center = Offset.Zero,
                    radius = r * 1.55f
                )
                val core = lerp(light, Color.White, 0.5f)
                cache.coreBrush[i] = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to core.copy(alpha = 0.18f * lighting.emissive),
                        0.55f to core.copy(alpha = 0.10f * lighting.emissive),
                        1.00f to Color.Transparent
                    ),
                    center = Offset.Zero,
                    radius = r * 0.72f
                )
            } else {
                cache.glowBrush[i] = null
                cache.coreBrush[i] = null
            }

            val rc = minOf(identity.rings.size, BodyIdentities.MAX_RINGS)
            cache.ringCount[i] = rc
            cache.ringSquash[i] = identity.ringSquash
            for (q in 0 until rc) {
                val band = identity.rings[q]
                val slot = i * BodyIdentities.MAX_RINGS + q
                cache.ringRadius[slot] = band.radiusFraction
                cache.ringColor[slot] = colors.bodyTone(band.argb).copy(alpha = band.alpha)
                // Stroke width scales with the body, and is floored so a distant Saturn still shows
                // a hair of ring instead of vanishing into a sub-pixel dash.
                cache.ringStroke[slot] = Stroke(width = (band.thicknessFraction * r).coerceAtLeast(0.6f))
                // A wider, fainter pass of the same band: the radial softness that stops a ring
                // reading as a cut strip of paper. Same colour, a third of the strength.
                cache.ringHalo[slot] = Stroke(
                    width = (band.thicknessFraction * r * 1.9f).coerceAtLeast(1.0f)
                )
            }
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
                val surface = colors.surface
                val table = surface.gradient.brushFor(size)
                val vignette = Brush.radialGradient(
                    colorStops = arrayOf(0.35f to Color.Transparent, 1f to colors.vignette),
                    center = Offset(size.width * 0.5f, size.height * 0.36f),
                    radius = size.maxDimension * 0.78f
                )
                // §5 — the printed pattern is generated here, once per size, from a fixed seed:
                // identical static dots on every launch, no animation and no per-frame work.
                val print = surface.pattern?.let { pattern -> SurfacePrint.of(pattern, size, this) }
                onDrawBehind {
                    drawRect(table)
                    print?.draw(this)
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
                // §5 — the surface carries its own trail alpha pair (older / recent segment).
                drawPath(old, colors.trailOld, style = cache.strokeTrailOld)
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
                //
                // §3 — a realistic cast shadow: the light is at the top-left (gradient centre
                // -0.30r/-0.34r, specular -0.34r/-0.38r), so the shadow falls to the bottom-right.
                // Penumbra first (1.45r x 1.15r at +0.38r/+0.34r), then the umbra (0.8r x 0.8r at
                // +0.20r/+0.18r). Draw order is unchanged: the shadow sits under its own body.
                translate(px, py) {
                    scale(bodyScale, bodyScale, Offset.Zero) {
                        val rr = cache.radiusPx[i]
                        cache.softShadow[i]?.let {
                            drawOval(
                                brush = it,
                                topLeft = Offset(rr * 0.38f - rr * 0.725f, rr * 0.34f - rr * 0.575f),
                                size = Size(rr * 1.45f, rr * 1.15f)
                            )
                        }
                        cache.shadow[i]?.let {
                            drawOval(
                                brush = it,
                                topLeft = Offset(rr * 0.20f - rr * 0.40f, rr * 0.18f - rr * 0.40f),
                                size = Size(rr * 0.80f, rr * 0.80f)
                            )
                        }
                    }
                }
                // §5 — the marble, in order: base gradient -> identity marks -> the *same* base
                // gradient re-applied at partial alpha (that is the lighting coming back over the
                // marks; no new light and no new brush) -> rim -> specular. Saturn's far ring half
                // goes under the sphere and its near half over the top, so the system passes behind
                // and in front of the body while every ring pixel is still painted exactly once.
                val textured = r >= BodyIdentities.MIN_RADIUS_PX
                translate(px, py) {
                    scale(bodyScale, bodyScale, Offset.Zero) {
                        val rr = cache.radiusPx[i]
                        if (textured) drawRingHalf(cache, i, rr, far = true)
                        // A star's corona sits behind its own disc and reaches past the limb.
                        cache.glowBrush[i]?.let { drawCircle(it, rr * 1.55f, Offset.Zero) }
                        cache.base[i]?.let { drawCircle(it, rr, Offset.Zero) }
                        if (textured) {
                            // §5 — the material, in order: belts under caps, then the marble's own
                            // lighting back over them, then the two curvature layers, then the
                            // emissive core of a star. Marks may never sit on top of the shading.
                            drawBands(cache, i, rr)
                            drawBlobs(cache, i, rr)
                            cache.base[i]?.let {
                                drawCircle(
                                    brush = it,
                                    radius = rr,
                                    center = Offset.Zero,
                                    alpha = BodyIdentities.LIGHTING_OVERLAY_ALPHA
                                )
                            }
                            cache.termBrush[i]?.let { drawCircle(it, rr, Offset.Zero) }
                            cache.limbBrush[i]?.let { drawCircle(it, rr, Offset.Zero) }
                            cache.coreBrush[i]?.let { drawCircle(it, rr * 0.72f, Offset.Zero) }
                        }
                        drawRimArc(cache, i, rr)
                        drawAtmosphere(cache, i, rr)
                        drawSpecularGlint(cache, i, rr)
                        if (textured) drawRingHalf(cache, i, rr, far = false)
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

/**
 * §1 — the projected caps: one soft unit-circle brush per edge profile, tinted and strengthed per
 * mark, drawn inside a rotate+scale so the shared brush becomes exactly the projected ellipse.
 *
 * Allocation-free by construction: every position, extent, tint and blend mode came out of
 * [SceneCache], and `Offset`/`Size` are value types. Nothing here is random, animated or per-pixel,
 * and because every projected cap is inscribed in the body disc there is no clip path and
 * therefore no aliased limb. Multiply and Screen are what make a cap read as material: it darkens
 * or brightens the light the marble already carries instead of covering it.
 */
private fun DrawScope.drawBlobs(cache: SceneCache, i: Int, rr: Float) {
    val count = cache.blobCount[i]
    if (count == 0) return
    val first = i * BodyIdentities.MAX_MARKS
    for (m in 0 until count) {
        val slot = first + m
        val brush = EDGE_BRUSHES[cache.blobEdge[slot].coerceIn(0, EDGE_BRUSHES.size - 1)]
        val a = (cache.blobMajor[slot] * rr).coerceAtLeast(0.05f)
        val b = (cache.blobMinor[slot] * rr).coerceAtLeast(0.05f)
        withTransform({
            translate(cache.blobCx[slot] * rr, cache.blobCy[slot] * rr)
            rotate(cache.blobDegrees[slot])
            scale(a, b, Offset.Zero)
        }) {
            drawCircle(
                brush = brush,
                radius = 1f,
                center = Offset.Zero,
                alpha = cache.blobAlpha[slot],
                colorFilter = cache.blobTint[slot],
                blendMode = ROLE_BLEND[cache.blobRole[slot].coerceIn(0, ROLE_BLEND.size - 1)]
            )
        }
    }
}

/**
 * §1 — the latitude belts: [BodyIdentities.BAND_SLICES] stroked arcs per belt, tiled across its
 * thickness at a smooth alpha ramp, so the cross-section reads as one soft band and the arc bends
 * with the sphere exactly as the ring system does. One shared Stroke per belt; the slices differ
 * only by their bounding box and alpha, so the loop allocates nothing.
 */
private fun DrawScope.drawBands(cache: SceneCache, i: Int, rr: Float) {
    val count = cache.bandCount[i]
    if (count == 0) return
    val first = i * BodyIdentities.MAX_MARKS
    for (d in 0 until count) {
        val slot = first + d
        val stroke = cache.bandStroke[slot] ?: continue
        val color = cache.bandColor[slot]
        val blend = ROLE_BLEND[cache.bandRole[slot].coerceIn(0, ROLE_BLEND.size - 1)]
        val firstSlice = slot * BodyIdentities.BAND_SLICES
        for (k in 0 until BodyIdentities.BAND_SLICES) {
            val s = firstSlice + k
            val alpha = cache.bandSliceAlpha[s]
            if (alpha <= 0.004f) continue
            val a = cache.bandArcA[s] * rr
            val b = (cache.bandArcB[s] * rr).coerceAtLeast(0.05f)
            drawArc(
                color = color,
                startAngle = cache.bandStart[s],
                sweepAngle = cache.bandSweep[s],
                useCenter = false,
                topLeft = Offset(-a, cache.bandArcY[s] * rr - b),
                size = Size(a * 2f, b * 2f),
                alpha = alpha,
                style = stroke,
                blendMode = blend
            )
        }
    }
}

/**
 * §1 — the rim, as a crescent where the light actually grazes the limb (top-left) instead of a
 * uniform bright circle. A full ring was the single loudest "glass marble" cue the old marble had.
 */
private fun DrawScope.drawRimArc(cache: SceneCache, i: Int, rr: Float) {
    drawArc(
        color = cache.rim[i],
        startAngle = 145f,
        sweepAngle = 160f,
        useCenter = false,
        topLeft = Offset(-rr, -rr),
        size = Size(rr * 2f, rr * 2f),
        style = cache.strokeRim
    )
}

/** §1 — the thin scattering crescent an atmosphere puts on the lit limb. Airless bodies skip it. */
private fun DrawScope.drawAtmosphere(cache: SceneCache, i: Int, rr: Float) {
    val strength = cache.atmosphere[i]
    if (strength <= 0.004f) return
    val r = rr * 0.955f
    drawArc(
        color = cache.atmoColor[i],
        startAngle = 160f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(-r, -r),
        size = Size(r * 2f, r * 2f),
        alpha = 0.30f * strength,
        style = cache.strokeRim
    )
}

/**
 * §1 — the highlight, per body: its own size, its own offset, its own strength and its own edge.
 * A bare rock gets a small tight mineral glint, a cloud world a broad dim sheen, a star none at
 * all — which is what ends the "same white dot on every planet" reading.
 */
private fun DrawScope.drawSpecularGlint(cache: SceneCache, i: Int, rr: Float) {
    val alpha = cache.specAlpha[i]
    val radius = cache.specRadius[i]
    if (alpha <= 0.004f || radius <= 0.01f) return
    val brush = EDGE_BRUSHES[cache.specEdge[i].coerceIn(0, EDGE_BRUSHES.size - 1)]
    withTransform({
        translate(cache.specCx[i], cache.specCy[i])
        scale(radius, radius, Offset.Zero)
    }) {
        drawCircle(
            brush = brush,
            radius = 1f,
            center = Offset.Zero,
            alpha = alpha,
            colorFilter = cache.specTint[i]
        )
    }
}

/**
 * §1 — one half of a ring system, as a single stroked arc per band.
 *
 * Splitting at the horizontal axis is what sells the sphere: in Compose 0° is the 3 o'clock
 * direction and angles grow clockwise, so 180°..360° is the top half — the *far* side of the rings,
 * which belongs behind the body — and 0°..180° is the bottom half, the *near* side, which belongs in
 * front of it. Each half is painted once, so overlapping alpha never doubles up, and no clipping is
 * involved. Rings are visual only: mass, radius, collision and selection geometry never see them.
 */
private fun DrawScope.drawRingHalf(cache: SceneCache, i: Int, rr: Float, far: Boolean) {
    val count = cache.ringCount[i]
    if (count == 0) return
    val squash = cache.ringSquash[i]
    val first = i * BodyIdentities.MAX_RINGS
    for (q in 0 until count) {
        val slot = first + q
        val stroke = cache.ringStroke[slot] ?: continue
        val rad = cache.ringRadius[slot] * rr
        val box = Offset(-rad, -rad * squash)
        val size = Size(rad * 2f, rad * 2f * squash)
        val start = if (far) 180f else 0f
        // A wider, fainter pass of the same band first: the radial softness that stops a ring
        // reading as a cut strip of paper. Same colour, a third of the strength.
        cache.ringHalo[slot]?.let {
            drawArc(
                color = cache.ringColor[slot],
                startAngle = start,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = box,
                size = size,
                alpha = 0.35f,
                style = it
            )
        }
        drawArc(
            color = cache.ringColor[slot],
            startAngle = start,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = box,
            size = size,
            style = stroke
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

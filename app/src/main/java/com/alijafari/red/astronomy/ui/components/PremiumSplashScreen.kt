package com.alijafari.red.astronomy.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.BitmapShader
import android.graphics.ComposeShader
import android.graphics.Matrix
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.alijafari.red.astronomy.ui.splash.SplashArt
import com.alijafari.red.astronomy.ui.splash.SplashArtGeometry
import com.alijafari.red.astronomy.ui.splash.SplashArtLoader
import com.alijafari.red.astronomy.ui.splash.SplashLayout
import com.alijafari.red.astronomy.ui.splash.SplashPalette
import com.alijafari.red.astronomy.ui.splash.SplashTimeline
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * ZIG launch splash: the frosted-glass Z on crimson velvet, brought to life.
 *
 * Layers, bottom to top (each one is a single cheap draw, and all motion is read in the draw
 * phase only - see [SplashTimeline] for the choreography and why):
 *
 *  1. Velvet  - the photograph's velvet (Z removed), drawn "cover" with a slow pull-back, a drift,
 *               and a wandering key light that is the velvet texture multiplied by a soft light and
 *               screened back on: the pile itself catches the light, not a flat overlay.
 *               Plus the crimson bloom, a vignette and the Z's contact shadow.
 *  2. Glass Z - the photographed Z as its own layer: focus pull, 3D tilt settle, float, and a
 *               specular sweep that brightens the frosted face and catches the polished bevel.
 *  3. Sparkle - a small star glint where each sweep leaves the glass.
 *  4. Overlay - "lights up" from the window colour, animated film grain, the exit dimming and the
 *               curtain's shadow edge.
 *  5. Hairline progress (accelerating).
 *
 * On exit the whole splash lifts like a curtain while the app rises underneath - the app side of
 * that hand-off is [splashReveal].
 */
@Composable
fun PremiumSplashScreen(
    onSplashComplete: () -> Unit,
    modifier: Modifier = Modifier,
    state: PremiumSplashState = rememberPremiumSplashState(),
) {
    val context = LocalContext.current
    val art = SplashArtLoader.art
    val settled = SplashArtLoader.isSettled
    val onComplete by rememberUpdatedState(onSplashComplete)

    // Previews and tests may compose the splash without the Activity having started the decode.
    LaunchedEffect(Unit) { SplashArtLoader.preload(context) }

    // The splash clock. Starts on the first frame after the art is ready (the Activity holds the
    // system splash until then), advances by the real frame delta, and is the ONLY thing that
    // changes per frame. The delta is clamped so a stall (first-frame jank, app switch) slows the
    // animation for a frame instead of making it jump.
    LaunchedEffect(state, settled) {
        if (!settled) return@LaunchedEffect
        var last = -1L
        while (!state.isFinished) {
            withFrameNanos { now ->
                if (last < 0L) last = now
                val dt = ((now - last) / 1_000_000f).coerceIn(0f, MAX_FRAME_STEP_MS)
                last = now
                state.timeMs += dt
            }
        }
        SplashArtLoader.release()
        onComplete()
    }

    SplashSystemBars()

    SplashScene(
        state = state,
        art = art,
        modifier = modifier.pointerInput(state) {
            // A tap skips ahead to the exit (after the Z has arrived). Also keeps touches from
            // reaching the not-yet-revealed UI underneath.
            detectTapGestures(onTap = { state.skip() })
        },
    )
}

/** Longest frame step the splash clock accepts, ms. */
private const val MAX_FRAME_STEP_MS = 40f

/**
 * The splash's clock, shared with the UI underneath so its slide-in is choreographed against the
 * same timeline (see [splashReveal]).
 */
@Stable
class PremiumSplashState internal constructor(val reducedMotion: Boolean) {
    /** Milliseconds since the first animated frame. Read it only in draw/layer lambdas. */
    var timeMs by mutableFloatStateOf(0f)
        internal set

    /** When the exit starts. Moves earlier if the user taps to skip. */
    var exitStartMs by mutableFloatStateOf(
        if (reducedMotion) SplashTimeline.REDUCED_INTRO_MS else SplashTimeline.INTRO_MS
    )
        private set

    /** Time used for the scene: frozen on a settled still under reduced motion. */
    internal val sceneTimeMs: Float
        get() = if (reducedMotion) SplashTimeline.REDUCED_STILL_MS else timeMs

    internal val isFinished: Boolean
        get() = SplashTimeline.isFinished(timeMs, exitStartMs, reducedMotion)

    fun skip() {
        val t = timeMs
        if (t >= SplashTimeline.MIN_SKIP_MS && t < exitStartMs) exitStartMs = t
    }

    /** For tests and frame renders: jump the clock to an exact moment. */
    internal fun seekTo(ms: Float) {
        timeMs = ms
    }
}

@Composable
fun rememberPremiumSplashState(): PremiumSplashState {
    val context = LocalContext.current
    return remember { PremiumSplashState(reducedMotion = animationsDisabled(context)) }
}

/** System "Remove animations" (accessibility) or animator scale 0 (developer options). */
private fun animationsDisabled(context: Context): Boolean = runCatching {
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
}.getOrDefault(false)

enum class SplashRevealRole { Content, NavigationBar }

/**
 * The UI side of the splash hand-off. While the splash is up, the UI is invisible (alpha 0 also
 * means the RenderThread skips drawing it, leaving the GPU to the splash). On exit, the content
 * glides up from below and settles from 93% scale on Material's emphasized curve while the velvet
 * curtain lifts; the navigation bar follows a beat later.
 *
 * Pass `null` once the splash is gone: the modifier is then a no-op.
 */
fun Modifier.splashReveal(
    state: PremiumSplashState?,
    role: SplashRevealRole = SplashRevealRole.Content,
): Modifier {
    if (state == null) return this
    return this.graphicsLayer {
        val t = state.timeMs
        val e = state.exitStartMs
        if (state.reducedMotion) {
            alpha = SplashTimeline.window(t, e, SplashTimeline.REDUCED_FADE_MS)
            return@graphicsLayer
        }
        when (role) {
            SplashRevealRole.Content -> {
                val p = SplashTimeline.contentProgress(t, e)
                alpha = SplashTimeline.contentAlpha(t, e)
                translationY = (1f - p) * size.height * SplashTimeline.CONTENT_FROM_OFFSET
                val s = SplashTimeline.lerp(SplashTimeline.CONTENT_FROM_SCALE, 1f, p)
                scaleX = s
                scaleY = s
                transformOrigin = TransformOrigin(0.5f, 0.4f)
            }
            SplashRevealRole.NavigationBar -> {
                val p = SplashTimeline.navProgress(t, e)
                alpha = SplashTimeline.navAlpha(t, e)
                translationY = (1f - p) * SplashTimeline.NAV_FROM_OFFSET_DP.dp.toPx()
            }
        }
    }
}

// =================================================================================================
// Scene
// =================================================================================================

@Composable
internal fun SplashScene(
    state: PremiumSplashState,
    art: SplashArt?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("premium_splash_screen")
            .semantics { contentDescription = "ZIG" }
            .graphicsLayer {
                // The curtain: the whole splash lifts off the top of the screen on exit.
                val t = state.timeMs
                val e = state.exitStartMs
                if (state.reducedMotion) {
                    alpha = 1f - SplashTimeline.window(t, e, SplashTimeline.REDUCED_FADE_MS)
                } else {
                    translationY = -size.height * SplashTimeline.curtain(t, e)
                }
            }
    ) {
        VelvetLayer(state, art)
        if (art != null) {
            GlassLayer(state, art)
            SparkleLayer(state)
        }
        OverlayLayer(state, art)
        if (!state.reducedMotion) ProgressHairline(state, Modifier.align(Alignment.BottomCenter))
    }
}

/** Places its content on the Z's crop frame for the current screen size. */
private fun Modifier.zFrame(): Modifier = layout { measurable, constraints ->
    val w = constraints.maxWidth
    val h = constraints.maxHeight
    val f = SplashLayout.zFrame(w.toFloat(), h.toFloat(), density)
    val placeable = measurable.measure(
        Constraints.fixed(f.width.roundToInt().coerceAtLeast(1), f.height.roundToInt().coerceAtLeast(1))
    )
    layout(w, h) { placeable.place(f.left.roundToInt(), f.top.roundToInt()) }
}

/** The Z's transform at a moment, shared by the glass, its sparkle and its shadow. */
private class ZPose(val alpha: Float, val scale: Float, val offsetY: Float)

private fun zPose(state: PremiumSplashState, screenH: Float, density: Float): ZPose {
    val t = state.sceneTimeMs
    val e = state.exitStartMs
    val curtain = if (state.reducedMotion) 0f else SplashTimeline.curtain(state.timeMs, e)
    val pop = if (state.reducedMotion) 1f else SplashTimeline.exitPop(state.timeMs, e)
    return ZPose(
        alpha = SplashTimeline.zAlpha(t) * (1f - 0.7f * curtain),
        scale = SplashTimeline.zScale(t) * pop,
        // Parallax: inside the lifting curtain the Z drifts down, so on screen it rises at ~58% of
        // the curtain's speed - the depth cue that sells the curtain as a physical layer.
        offsetY = SplashTimeline.zOffsetYDp(t) * density + screenH * 0.42f * curtain,
    )
}

private fun GraphicsLayerScope.applyZPose(state: PremiumSplashState, screenH: Float, withBlur: Boolean) {
    val t = state.sceneTimeMs
    val pose = zPose(state, screenH, density)
    alpha = pose.alpha
    scaleX = pose.scale
    scaleY = pose.scale
    translationY = pose.offsetY
    rotationX = SplashTimeline.zRotationX(t)
    rotationY = SplashTimeline.zRotationY(t)
    rotationZ = SplashTimeline.zRotationZ(t)
    cameraDistance = 14f * density
    if (withBlur) {
        val blurPx = SplashTimeline.zBlurDp(t) * density
        renderEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurPx > 0.5f) {
            BlurEffect(blurPx, blurPx, TileMode.Decal)
        } else {
            null
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 1. Velvet
// -------------------------------------------------------------------------------------------------

@Composable
private fun VelvetLayer(state: PremiumSplashState, art: SplashArt?) {
    Spacer(
        Modifier
            .fillMaxSize()
            .drawWithCache {
                val w = size.width
                val h = size.height
                val plate = art?.velvet?.asAndroidBitmap()
                val plateW = plate?.width?.toFloat() ?: 1f
                val plateH = plate?.height?.toFloat() ?: 1f
                val rotated = SplashLayout.rotateVelvet(w, h)
                val cover = SplashLayout.velvetCoverScale(plateW, plateH, w, h)

                val platePaint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)
                val lightPaint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG).apply {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
                }
                val plateShader = plate?.let {
                    BitmapShader(it, android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP)
                }
                val key = SplashPalette.KeyLight
                // Unit-radius light at the origin; positioned per frame by its local matrix.
                val light = RadialGradient(
                    0f, 0f, 1f,
                    intArrayOf(key.toArgb(), key.copy(alpha = 0.45f).toArgb(), key.copy(alpha = 0.12f).toArgb(), 0),
                    floatArrayOf(0f, 0.35f, 0.7f, 1f),
                    android.graphics.Shader.TileMode.CLAMP,
                )
                val plateMatrix = Matrix()
                val lightMatrix = Matrix()

                val frame = SplashLayout.zFrame(w, h, density)
                val glassW = SplashLayout.glassWidth(w, h, density)
                val glowCenter = Offset(frame.centerX, frame.centerY)
                val glowRadius = glassW * 1.25f
                val glowBrush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to SplashPalette.Glow.copy(alpha = 0.22f),
                        0.45f to SplashPalette.Glow.copy(alpha = 0.08f),
                        1f to Color.Transparent,
                    ),
                    center = glowCenter,
                    radius = glowRadius,
                )
                val vignette = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to Color.Transparent,
                        0.55f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.40f),
                    ),
                    center = Offset(w / 2f, h / 2f),
                    radius = sqrt(w * w + h * h) / 2f,
                )

                onDrawBehind {
                    val t = state.sceneTimeMs
                    drawRect(SplashPalette.VelvetMean)

                    if (plate != null && plateShader != null) {
                        val s = cover * SplashTimeline.VELVET_OVERSCAN * SplashTimeline.velvetZoom(t)
                        plateMatrix.reset()
                        plateMatrix.postTranslate(-plateW / 2f, -plateH / 2f)
                        if (rotated) plateMatrix.postRotate(90f)
                        plateMatrix.postScale(s, s)
                        plateMatrix.postTranslate(
                            w / 2f + SplashTimeline.velvetDriftX(t) * w,
                            h / 2f + SplashTimeline.velvetDriftY(t) * h,
                        )
                        drawIntoCanvas { canvas ->
                            val nc = canvas.nativeCanvas
                            nc.drawBitmap(plate, plateMatrix, platePaint)

                            // Key light: velvet x soft light, screened on top. Highlights grow on
                            // the pile's bright fibres, so the velvet itself appears to shimmer.
                            val strength = SplashTimeline.lightStrength(t)
                            if (strength > 0.01f) {
                                plateShader.setLocalMatrix(plateMatrix)
                                val r = max(w, h) * 0.55f
                                lightMatrix.reset()
                                lightMatrix.setScale(r, r * 0.82f)
                                lightMatrix.postTranslate(SplashTimeline.lightX(t) * w, SplashTimeline.lightY(t) * h)
                                light.setLocalMatrix(lightMatrix)
                                lightPaint.shader = ComposeShader(plateShader, light, PorterDuff.Mode.MULTIPLY)
                                lightPaint.alpha = (strength * 150f).roundToInt().coerceIn(0, 255)
                                nc.drawRect(0f, 0f, w, h, lightPaint)
                            }
                        }
                    }

                    // Crimson bloom behind the glass.
                    val g = SplashTimeline.glow(t)
                    if (g > 0.01f) {
                        drawCircle(glowBrush, radius = glowRadius, center = glowCenter, alpha = g, blendMode = BlendMode.Screen)
                    }

                    drawRect(vignette)

                    // Contact shadow, following the Z's pose (not its tilt).
                    if (art != null) {
                        val pose = zPose(state, h, density)
                        val a = SplashTimeline.shadowAlpha(t) * pose.alpha
                        if (a > 0.01f) {
                            val sc = pose.scale * SplashTimeline.shadowScale(t)
                            val sw = frame.width * sc
                            val sh = frame.height * sc
                            val cx = frame.centerX
                            val cy = frame.centerY + pose.offsetY + SplashTimeline.shadowOffsetDp(t) * density
                            drawImage(
                                image = art.shadow,
                                srcOffset = IntOffset.Zero,
                                srcSize = IntSize(art.shadow.width, art.shadow.height),
                                dstOffset = IntOffset((cx - sw / 2f).roundToInt(), (cy - sh / 2f).roundToInt()),
                                dstSize = IntSize(sw.roundToInt(), sh.roundToInt()),
                                alpha = a,
                                filterQuality = FilterQuality.Low,
                            )
                        }
                    }
                }
            }
    )
}

// -------------------------------------------------------------------------------------------------
// 2. Glass Z
// -------------------------------------------------------------------------------------------------

@Composable
private fun GlassLayer(state: PremiumSplashState, art: SplashArt) {
    val screenHeight = remember { floatArrayOf(0f) }
    Box(
        Modifier
            .fillMaxSize()
            .layout { m, c ->
                screenHeight[0] = c.maxHeight.toFloat()
                val p = m.measure(c)
                layout(p.width, p.height) { p.place(0, 0) }
            }
    ) {
        Spacer(
            Modifier
                .zFrame()
                .graphicsLayer {
                    applyZPose(state, screenHeight[0], withBlur = true)
                    // Offscreen so the sheen (SrcAtop) and rim mask (DstIn) only touch the glass.
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithCache {
                    val dst = IntSize(size.width.roundToInt(), size.height.roundToInt())
                    val glassSrc = IntSize(art.glass.width, art.glass.height)
                    val rimSrc = IntSize(art.rim.width, art.rim.height)
                    val bounds = Rect(Offset.Zero, size)
                    val additive = Paint().apply { blendMode = BlendMode.Plus }
                    // Sweep direction: mostly left-to-right, slightly downward.
                    val dx = 1f
                    val dy = 0.55f
                    val len = sqrt(dx * dx + dy * dy)
                    val dir = Offset(dx / len, dy / len)
                    // Range of the frame's corners projected on that direction.
                    val projMax = size.width * dir.x + size.height * dir.y
                    val diag = sqrt(size.width * size.width + size.height * size.height)

                    onDrawBehind {
                        drawImage(
                            image = art.glass,
                            srcOffset = IntOffset.Zero,
                            srcSize = glassSrc,
                            dstOffset = IntOffset.Zero,
                            dstSize = dst,
                            filterQuality = FilterQuality.High,
                        )

                        val sweep = SplashTimeline.sweep(state.sceneTimeMs, state.exitStartMs) ?: return@onDrawBehind
                        val (u, strength) = sweep
                        val c = dir * (u * projMax)

                        // Broad soft sheen across the frosted face.
                        val wide = diag * 0.24f
                        drawRect(
                            brush = Brush.linearGradient(
                                colorStops = arrayOf(
                                    0f to Color.Transparent,
                                    0.38f to Color.White.copy(alpha = 0.10f),
                                    0.5f to Color.White.copy(alpha = 0.42f),
                                    0.62f to Color.White.copy(alpha = 0.10f),
                                    1f to Color.Transparent,
                                ),
                                start = c - dir * wide,
                                end = c + dir * wide,
                            ),
                            alpha = (0.75f * strength).coerceAtMost(1f),
                            blendMode = BlendMode.SrcAtop,
                        )

                        // The polished bevel catches the same light harder and tighter.
                        val tight = diag * 0.11f
                        drawIntoCanvas { canvas ->
                            canvas.saveLayer(bounds, additive)
                            drawRect(
                                brush = Brush.linearGradient(
                                    colorStops = arrayOf(
                                        0f to Color.Transparent,
                                        0.5f to Color.White,
                                        1f to Color.Transparent,
                                    ),
                                    start = c - dir * tight,
                                    end = c + dir * tight,
                                ),
                                alpha = (0.9f * strength).coerceAtMost(1f),
                            )
                            drawImage(
                                image = art.rim,
                                srcOffset = IntOffset.Zero,
                                srcSize = rimSrc,
                                dstOffset = IntOffset.Zero,
                                dstSize = dst,
                                blendMode = BlendMode.DstIn,
                                filterQuality = FilterQuality.High,
                            )
                            canvas.restore()
                        }
                    }
                }
        )
    }
}

// -------------------------------------------------------------------------------------------------
// 3. Sparkle
// -------------------------------------------------------------------------------------------------

@Composable
private fun SparkleLayer(state: PremiumSplashState) {
    val screenHeight = remember { floatArrayOf(0f) }
    Box(
        Modifier
            .fillMaxSize()
            .layout { m, c ->
                screenHeight[0] = c.maxHeight.toFloat()
                val p = m.measure(c)
                layout(p.width, p.height) { p.place(0, 0) }
            }
    ) {
        Spacer(
            Modifier
                .zFrame()
                .graphicsLayer { applyZPose(state, screenHeight[0], withBlur = false) }
                .drawWithCache {
                    val center = Offset(SplashArtGeometry.SPARKLE_X * size.width, SplashArtGeometry.SPARKLE_Y * size.height)
                    val stroke = 1.6.dp.toPx()
                    onDrawBehind {
                        val s = SplashTimeline.sparkle(state.sceneTimeMs, state.exitStartMs)
                        if (s < 0.01f) return@onDrawBehind
                        val arm = size.width * 0.15f * (0.55f + 0.45f * s)
                        val glint = Brush.radialGradient(
                            colorStops = arrayOf(0f to Color.White, 0.25f to Color.White.copy(alpha = 0.55f), 1f to Color.Transparent),
                            center = center,
                            radius = arm * 0.32f,
                        )
                        drawCircle(glint, radius = arm * 0.32f, center = center, alpha = s, blendMode = BlendMode.Plus)
                        rotate(degrees = 12f + state.sceneTimeMs * 0.018f, pivot = center) {
                            fun ray(half: Float, horizontal: Boolean) {
                                val a = if (horizontal) Offset(center.x - half, center.y) else Offset(center.x, center.y - half)
                                val b = if (horizontal) Offset(center.x + half, center.y) else Offset(center.x, center.y + half)
                                drawLine(
                                    brush = Brush.linearGradient(
                                        colorStops = arrayOf(0f to Color.Transparent, 0.5f to Color.White, 1f to Color.Transparent),
                                        start = a,
                                        end = b,
                                    ),
                                    start = a,
                                    end = b,
                                    strokeWidth = stroke,
                                    alpha = s,
                                    blendMode = BlendMode.Plus,
                                )
                            }
                            ray(arm, horizontal = true)
                            ray(arm * 0.62f, horizontal = false)
                        }
                    }
                }
        )
    }
}

// -------------------------------------------------------------------------------------------------
// 4. Overlay: exposure, grain, exit dimming, curtain edge
// -------------------------------------------------------------------------------------------------

@Composable
private fun OverlayLayer(state: PremiumSplashState, art: SplashArt?) {
    Spacer(
        Modifier
            .fillMaxSize()
            .drawWithCache {
                val w = size.width
                val h = size.height
                val grain = art?.grain
                val grainShader = grain?.let {
                    BitmapShader(it, android.graphics.Shader.TileMode.REPEAT, android.graphics.Shader.TileMode.REPEAT)
                }
                val grainPaint = android.graphics.Paint().apply {
                    shader = grainShader
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
                    alpha = 84
                }
                val grainMatrix = Matrix()
                val grainScale = max(1f, density * 0.7f)
                val edge = 56.dp.toPx()
                val edgeShadow = Brush.verticalGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.55f), Color.Black.copy(alpha = 0.18f), Color.Transparent),
                    startY = h,
                    endY = h + edge,
                )

                onDrawBehind {
                    val t = state.timeMs
                    val e = state.exitStartMs

                    // Film grain: re-seeded ~24 times a second, like film. Adds perceived detail,
                    // hides gradient banding on the dark velvet, and is never still.
                    if (grainShader != null) {
                        val step = if (state.reducedMotion) 0 else (t / 42f).toInt()
                        val hx = ((step * 73_856_093) xor (step * 19_349_663)).toLong() and 0xFFFF
                        val hy = ((step * 83_492_791) xor (step * 2_654_435)).toLong() and 0xFFFF
                        grainMatrix.setScale(grainScale, grainScale)
                        grainMatrix.postTranslate((hx % 128).toFloat() * grainScale, (hy % 128).toFloat() * grainScale)
                        grainShader.setLocalMatrix(grainMatrix)
                        drawIntoCanvas { it.nativeCanvas.drawRect(0f, 0f, w, h, grainPaint) }
                    }

                    // Lights up from exactly the window / system-splash colour.
                    val dark = 1f - SplashTimeline.exposure(state.sceneTimeMs)
                    if (dark > 0.001f) drawRect(SplashPalette.VelvetDark, alpha = dark)

                    if (!state.reducedMotion) {
                        val curtain = SplashTimeline.curtain(t, e)
                        if (curtain > 0f) {
                            // The lifting velvet falls into shadow, and casts a soft edge below it.
                            drawRect(Color.Black, alpha = 0.38f * curtain)
                            drawRect(
                                brush = edgeShadow,
                                topLeft = Offset(0f, h),
                                size = Size(w, edge),
                                alpha = (curtain * 6f).coerceAtMost(1f),
                            )
                        }
                    }
                }
            }
    )
}

// -------------------------------------------------------------------------------------------------
// 5. Progress hairline
// -------------------------------------------------------------------------------------------------

@Composable
private fun ProgressHairline(state: PremiumSplashState, modifier: Modifier) {
    Spacer(
        modifier
            .navigationBarsPadding()
            .padding(bottom = 44.dp)
            .size(width = 56.dp, height = 2.dp)
            .drawBehind {
                val t = state.timeMs
                val e = state.exitStartMs
                val a = SplashTimeline.progressAlpha(t, e)
                if (a <= 0.005f) return@drawBehind
                val r = CornerRadius(size.height / 2f)
                drawRoundRect(Color.White.copy(alpha = 0.14f), cornerRadius = r, alpha = a)
                val p = SplashTimeline.progress(t, e)
                if (p > 0f) {
                    drawRoundRect(
                        color = Color(0xFFFFE4EA).copy(alpha = 0.78f),
                        size = Size(size.width * p, size.height),
                        cornerRadius = r,
                        alpha = a,
                    )
                    val head = Offset(size.width * p, size.height / 2f)
                    val glowR = size.height * 3.5f
                    drawCircle(
                        brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.55f), Color.Transparent), center = head, radius = glowR),
                        radius = glowR,
                        center = head,
                        alpha = a * (0.6f + 0.4f * sin(t / 1000f * 2f * PI.toFloat() * 1.4f)),
                        blendMode = BlendMode.Plus,
                    )
                }
            }
    )
}

// -------------------------------------------------------------------------------------------------
// System bars
// -------------------------------------------------------------------------------------------------

/** Light status/navigation bar icons over the dark velvet; the app's own choice is restored after. */
@Composable
private fun SplashSystemBars() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = if (view.isInEditMode) null else view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previousStatus = controller?.isAppearanceLightStatusBars
        val previousNav = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            if (controller != null) {
                previousStatus?.let { controller.isAppearanceLightStatusBars = it }
                previousNav?.let { controller.isAppearanceLightNavigationBars = it }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

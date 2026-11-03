package com.alijafari.red.astronomy.ui.splash

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * The splash choreography as pure functions of time.
 *
 * Everything on the splash (and the UI's slide-in after it) is derived from ONE number - the
 * splash clock in milliseconds - through these functions. That buys three things:
 *
 *  1. Smoothness. The clock is a single float state that is only ever read inside draw-phase
 *     lambdas (`graphicsLayer {}` / `drawWithCache { onDrawBehind {} }`). A new frame therefore
 *     never recomposes or re-measures anything: Compose jumps straight to the draw phase, which is
 *     the cheapest thing a frame can do (developer.android.com, "Defer reads as long as possible").
 *  2. Coherence. Every layer is keyed to the same clock, so nothing can drift out of sync.
 *  3. Testability. A test sets the clock and renders the exact frame for that moment.
 *
 * Motion design, in order:
 *
 *  - 0 ms       First frame is the exact colour of the system splash / window background, so the
 *               hand-off from the OS is invisible.
 *  - 0-750      The velvet "lights up" out of darkness while the camera slowly pulls back
 *               (Ken Burns), and a soft key light starts to wander across the pile.
 *  - 180-1300   The glass Z arrives from depth: focus pull (blur -> sharp), tilt settles, scale
 *               settles with Material's emphasized-decelerate curve, a contact shadow tightens.
 *  - 950-2050   A specular light sweep crosses the frosted face and catches the polished bevel,
 *               ending in a small sparkle on the top-right corner.
 *  - ongoing    The Z floats and breathes, the crimson glow pulses, film grain shimmers -
 *               there is never a frozen frame ("waiting with motion feels shorter").
 *  - 2250-3000  A second, brighter sweep: the peak right before the exit (peak-end rule).
 *  - 2600+      Exit: the velvet lifts away like a curtain (the Z lagging behind it in parallax)
 *               while the app glides up into place underneath, settling from 93% scale; the nav
 *               bar follows a beat later. ~1.2 s, then the splash is removed.
 *
 * A thin progress hairline fills along an accelerating curve: accelerating progress is preferred
 * over linear and reads as faster (Harrison et al., UIST 2007 / CHI 2010).
 */
object SplashTimeline {

    /** When the exit begins if the user does not tap to skip. */
    const val INTRO_MS = 2600f
    /** Earliest moment a tap may skip ahead - the Z has to have arrived first. */
    const val MIN_SKIP_MS = 700f

    // -- exit, measured from the exit start ------------------------------------------------------
    const val CURTAIN_DELAY_MS = 60f
    const val CURTAIN_MS = 900f
    // The content starts rising behind the curtain almost at once and uses Material's emphasized
    // curve (gentle start, long glide): by the time the curtain uncovers it, it is still visibly
    // travelling - ~5% of the screen height, slowing to rest - instead of having already landed.
    const val CONTENT_DELAY_MS = 40f
    const val CONTENT_MS = 1150f
    /** Content starts this far below its place (fraction of its height) and at [CONTENT_FROM_SCALE]. */
    const val CONTENT_FROM_OFFSET = 0.14f
    const val CONTENT_FROM_SCALE = 0.93f
    const val NAV_DELAY_MS = 260f
    const val NAV_MS = 900f
    const val NAV_FROM_OFFSET_DP = 110f
    /** The exit is over (splash removed) once every piece has landed. */
    val EXIT_TOTAL_MS: Float = max(CURTAIN_DELAY_MS + CURTAIN_MS, max(CONTENT_DELAY_MS + CONTENT_MS, NAV_DELAY_MS + NAV_MS))

    // -- reduced motion (system "Remove animations"): a still image and a short cross-fade --------
    const val REDUCED_INTRO_MS = 900f
    const val REDUCED_FADE_MS = 320f
    /** The settled moment shown as a still frame under reduced motion (Z landed, between sweeps). */
    const val REDUCED_STILL_MS = 2200f

    // --- Waiting for the app -------------------------------------------------------------------
    // The app underneath is composed while the splash plays and tells the splash when its first
    // frames are drawn. The splash never exits before that.

    /**
     * The app is composed no earlier than this: the Z has landed (its arrival ends at 1480 ms), so
     * the one heavy composition frame can only cost the slow ambient float a beat, never the
     * entrance itself.
     */
    const val APP_COMPOSE_AFTER_MS = 1500f

    /**
     * The exit starts no sooner than this after the app reports ready. While the app is not ready
     * the exit stays this far ahead of now, so the progress hairline keeps creeping without ever
     * completing, and completes over this lead once the app is ready.
     */
    const val CONTENT_LEAD_MS = 400f

    /**
     * Safety net: past this splash time the app is composed even if the start-up warm-up has not
     * finished (Home then computes on demand, as it always did), so the splash can never hang.
     */
    const val MAX_WARMUP_WAIT_MS = 8000f

    // -- curves ---------------------------------------------------------------------------------
    /** Material 3 emphasized decelerate: things entering the screen. */
    val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    /** Material 3 emphasized (single-bezier form): gentle start, long settle. */
    val Emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    /** A curtain lift: starts gently, commits, glides out. */
    val CurtainLift: Easing = CubicBezierEasing(0.6f, 0f, 0.25f, 1f)
    val EaseOutCubic: Easing = Easing { 1f - (1f - it).pow(3) }
    val EaseOutQuad: Easing = Easing { 1f - (1f - it) * (1f - it) }
    val EaseInOutSine: Easing = Easing { (1f - kotlin.math.cos(PI.toFloat() * it)) / 2f }

    fun clamp01(v: Float): Float = min(1f, max(0f, v))
    /** Progress of a window [start, start + duration] at time [t], clamped to 0..1. */
    fun window(t: Float, start: Float, duration: Float): Float = clamp01((t - start) / duration)
    fun lerp(a: Float, b: Float, f: Float): Float = a + (b - a) * f

    private fun wave(t: Float, periodMs: Float, phase: Float = 0f): Float =
        sin(2f * PI.toFloat() * (t / periodMs) + phase)

    // ===========================================================================================
    // Velvet
    // ===========================================================================================

    /** 0 = the window colour (darkness), 1 = the velvet fully lit. */
    fun exposure(t: Float): Float = EaseOutCubic.transform(window(t, 0f, 750f))

    /** Slow camera pull-back. Multiplies the cover scale. */
    fun velvetZoom(t: Float): Float = 1f + 0.10f * (1f - EaseOutCubic.transform(window(t, 0f, 5000f)))

    /** Gentle drift of the velvet, as a fraction of the screen (x, y). Needs [VELVET_OVERSCAN]. */
    fun velvetDriftX(t: Float): Float = 0.010f * wave(t, 9000f)
    fun velvetDriftY(t: Float): Float = 0.007f * wave(t, 11000f, 1.3f)
    const val VELVET_OVERSCAN = 1.045f

    /** The wandering key light, as fractions of the screen and its strength (0..1). */
    fun lightX(t: Float): Float = 0.30f + 0.42f * EaseInOutSine.transform(window(t, 0f, 5200f)) + 0.04f * wave(t, 3700f)
    fun lightY(t: Float): Float = 0.30f + 0.10f * wave(t, 6100f, 0.6f)
    fun lightStrength(t: Float): Float = EaseOutCubic.transform(window(t, 120f, 1100f)) * (0.86f + 0.14f * wave(t, 2900f))

    /** Crimson bloom behind the glass. */
    fun glow(t: Float): Float = EaseOutCubic.transform(window(t, 300f, 1000f)) * (0.82f + 0.18f * wave(t, 3200f, -PI.toFloat() / 2f))

    // ===========================================================================================
    // Glass Z
    // ===========================================================================================

    // Gentle start (it is still fading in and blurred), then a long visible glide into place.
    private fun zArrival(t: Float): Float = Emphasized.transform(window(t, 180f, 1300f))

    fun zAlpha(t: Float): Float = EaseOutQuad.transform(window(t, 180f, 560f))
    fun zScale(t: Float): Float = lerp(0.86f, 1f, zArrival(t)) * (1f + 0.006f * wave(t, 3400f) * window(t, 1100f, 900f))
    /** Focus pull, dp of blur radius (applied on API 31+). */
    fun zBlurDp(t: Float): Float = 18f * (1f - EaseOutCubic.transform(window(t, 180f, 820f)))
    fun zRotationX(t: Float): Float = 16f * (1f - zArrival(t))
    fun zRotationY(t: Float): Float = -10f * (1f - zArrival(t)) + 1.1f * wave(t, 5200f) * window(t, 1200f, 1000f)
    fun zRotationZ(t: Float): Float = 0.5f * wave(t, 6800f, 0.8f) * window(t, 1200f, 1000f)
    /** Vertical offset in dp: rises into place, then floats. */
    fun zOffsetYDp(t: Float): Float = 26f * (1f - zArrival(t)) + 2.6f * wave(t, 3400f, PI.toFloat()) * window(t, 1000f, 900f)

    /** Contact shadow: opacity, offset (dp) and scale relative to the Z. Tightens as the Z lands. */
    fun shadowAlpha(t: Float): Float = 0.62f * zArrival(t)
    fun shadowOffsetDp(t: Float): Float = lerp(34f, 13f, zArrival(t)) - 0.6f * zOffsetYDp(t).coerceAtMost(3f)
    fun shadowScale(t: Float): Float = lerp(1.12f, 1.02f, zArrival(t))

    /** One pass of light across the glass. */
    data class Sweep(val start: Float, val duration: Float, val strength: Float)

    val SWEEPS = listOf(
        Sweep(start = 950f, duration = 1100f, strength = 0.85f),
        Sweep(start = 2250f, duration = 760f, strength = 1.15f),
    )

    /**
     * Band position along the Z's diagonal (top-left -> bottom-right), -0.4..1.4, or null when no
     * sweep is on screen, plus its strength.
     */
    fun sweep(t: Float, exitStart: Float): Pair<Float, Float>? {
        for (s in SWEEPS) {
            // A skipped intro cuts the second sweep short - never start one after the exit began.
            if (s.start > exitStart) continue
            val p = window(t, s.start, s.duration)
            if (p > 0f && p < 1f) return Pair(lerp(-0.4f, 1.4f, EaseInOutSine.transform(p)), s.strength)
        }
        return null
    }

    /** Sparkle on the bevel corner where each sweep leaves the glass: 0..1 and its spin. */
    fun sparkle(t: Float, exitStart: Float): Float {
        var best = 0f
        for (s in SWEEPS) {
            if (s.start > exitStart) continue
            val peak = s.start + s.duration * 0.80f
            val d = (t - peak) / 260f
            best = max(best, s.strength.coerceAtMost(1f) * exp(-d * d * 2.2f))
        }
        return best
    }

    // ===========================================================================================
    // Progress hairline
    // ===========================================================================================

    /** Accelerating fill 0..1, finishing as the exit starts. */
    fun progress(t: Float, exitStart: Float): Float {
        val p = window(t, 300f, max(1f, exitStart - 300f))
        return 0.3f * p + 0.7f * p * p
    }

    fun progressAlpha(t: Float, exitStart: Float): Float =
        0.9f * EaseOutQuad.transform(window(t, 300f, 450f)) * (1f - window(t, exitStart + 60f, 260f))

    // ===========================================================================================
    // Exit
    // ===========================================================================================

    /** 0..1: how far the velvet curtain has lifted off the screen. */
    fun curtain(t: Float, exitStart: Float): Float = CurtainLift.transform(window(t, exitStart + CURTAIN_DELAY_MS, CURTAIN_MS))

    /** Little "inhale" of the Z right before the curtain moves. */
    fun exitPop(t: Float, exitStart: Float): Float = 1f + 0.035f * EaseOutCubic.transform(window(t, exitStart, 280f))

    /** How the app's main content enters: 0 = hidden below, 1 = in place. */
    fun contentProgress(t: Float, exitStart: Float): Float =
        Emphasized.transform(window(t, exitStart + CONTENT_DELAY_MS, CONTENT_MS))

    fun contentAlpha(t: Float, exitStart: Float): Float =
        EaseOutQuad.transform(window(t, exitStart + 120f, 380f))

    fun navProgress(t: Float, exitStart: Float): Float =
        Emphasized.transform(window(t, exitStart + NAV_DELAY_MS, NAV_MS))

    fun navAlpha(t: Float, exitStart: Float): Float = EaseOutQuad.transform(window(t, exitStart + NAV_DELAY_MS, 300f))

    fun isFinished(t: Float, exitStart: Float, reducedMotion: Boolean): Boolean =
        t >= exitStart + if (reducedMotion) REDUCED_FADE_MS else EXIT_TOTAL_MS
}

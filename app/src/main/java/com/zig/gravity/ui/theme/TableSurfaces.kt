package com.zig.gravity.ui.theme

import java.util.Random
import kotlin.math.abs

/**
 * §5 — the table surface catalog.
 *
 * The sandbox used to offer a binary dark/light *theme*. It now offers seven designed material
 * *finishes*: each one is a gradient, an optional static printed pattern, a vignette, a chrome mode
 * and its own trail alphas. Bodies stay marbles resting ON the surface and casting shadows on it;
 * a surface is a piece of material, never a window into space.
 *
 * This file is **pure data + pure functions**: no Compose, no Android, no rendering. That is what
 * lets the catalog be verified from a plain JVM test, and it keeps the `sim` layer free of any
 * dependency on `ui` — the ViewModel only ever stores a surface [key] string.
 *
 * The printed star dots are generated from a fixed seed, once, and are completely static: same dots
 * on every launch, no twinkle, no parallax, no per-frame work.
 */

/** Which chrome scheme (HUD, sheets, cards, dialogs) a surface asks for. */
enum class ChromeMode { DARK, LIGHT }

/** How a surface's background gradient is laid out. */
enum class SurfaceGradientType { RADIAL, LINEAR }

/**
 * Background gradient spec.
 *
 * [angleDegrees] follows the CSS convention for linear gradients: 0° = to top, 90° = to right,
 * 180° = to bottom. Every linear surface in this catalog is a plain vertical one (180°).
 * [centerX]/[centerY] are fractions of the canvas for radial gradients, and [radiusFraction] is a
 * fraction of the canvas' largest dimension.
 */
data class SurfaceGradient(
    val type: SurfaceGradientType,
    val startArgb: Long,
    val endArgb: Long,
    val centerX: Float = 0.5f,
    val centerY: Float = 0.38f,
    val radiusFraction: Float = 0.95f,
    val angleDegrees: Float = 180f
)

/**
 * A static printed pattern — minimal dots on the material, like a printed fabric.
 *
 * [seed] makes the pattern deterministic: the same surface prints the same dots forever.
 * [brightCount] adds a handful of slightly larger dots so the print does not look machine-uniform,
 * and [dotArgb] is the ink colour (white on every dark surface that carries a print).
 */
data class StarPattern(
    val seed: Int,
    val dotCount: Int,
    val brightCount: Int = 0,
    val minRadiusDp: Float = 0.7f,
    val maxRadiusDp: Float = 1.5f,
    val brightRadiusDp: Float = 1.8f,
    val minAlpha: Float = 0.16f,
    val maxAlpha: Float = 0.42f,
    val brightAlpha: Float = 0.5f,
    val dotArgb: Long = 0xFFFFFFFFL
) {
    val totalDots: Int get() = dotCount + brightCount
}

/** One printed dot, in canvas fractions so the pattern is resolution independent. */
data class StarDot(
    val xFraction: Float,
    val yFraction: Float,
    val radiusDp: Float,
    val alpha: Float
)

/**
 * One table surface.
 *
 * [vignetteArgb] carries its own strength in the alpha byte; [vignetteStrength] exposes that same
 * strength as a 0..1 fraction for inspection and tests, so the legacy surfaces reproduce their
 * pre-refresh vignettes bit for bit.
 */
data class GravitySurface(
    val key: String,
    val titleFa: String,
    val titleEn: String,
    val chromeMode: ChromeMode,
    val gradient: SurfaceGradient,
    val vignetteArgb: Long,
    val sheenArgb: Long,
    val pattern: StarPattern?,
    val trailAlphaOld: Float,
    val trailAlphaNew: Float
) {
    val isDark: Boolean get() = chromeMode == ChromeMode.DARK

    val vignetteStrength: Float get() = ((vignetteArgb ushr 24) and 0xFFL) / 255f

    fun title(isFa: Boolean): String = if (isFa) titleFa else titleEn
}

object TableSurfaces {

    /** §5/§7 — the surface a fresh install and every migrated dark session land on. */
    const val DEFAULT_KEY = "midnight"

    /** The pre-refresh light theme, preserved exactly as its own surface (§7 migration target). */
    const val LEGACY_LIGHT_KEY = "paper"

    /** Trail alphas are a property of the chrome mode, not of the individual finish. */
    const val TRAIL_OLD_DARK = 0.18f
    const val TRAIL_NEW_DARK = 0.26f
    const val TRAIL_OLD_LIGHT = 0.22f
    const val TRAIL_NEW_LIGHT = 0.30f

    /** Half-width of the printed-dot exclusion box, as a fraction of the canvas (§5). */
    private const val CENTRE_EXCLUSION = 0.15f

    /**
     * DEFAULT. Deep navy fabric with a calm printed dot field. Radial, so the middle of the table
     * is the lightest part and the corners fall away — the play area reads as lit material.
     */
    val MIDNIGHT = GravitySurface(
        key = "midnight",
        titleFa = "آسمانِ شب",
        titleEn = "Midnight Sky",
        chromeMode = ChromeMode.DARK,
        gradient = SurfaceGradient(
            type = SurfaceGradientType.RADIAL,
            startArgb = 0xFF232D4FL,
            endArgb = 0xFF0B0F1EL,
            centerX = 0.50f,
            centerY = 0.38f,
            radiusFraction = 0.95f
        ),
        vignetteArgb = 0x4005070FL,
        sheenArgb = 0x0AFFFFFFL,
        pattern = StarPattern(
            seed = 7,
            dotCount = 130,
            brightCount = 10,
            minRadiusDp = 0.7f,
            maxRadiusDp = 1.5f,
            brightRadiusDp = 1.8f,
            minAlpha = 0.16f,
            maxAlpha = 0.42f,
            brightAlpha = 0.50f
        ),
        trailAlphaOld = TRAIL_OLD_DARK,
        trailAlphaNew = TRAIL_NEW_DARK
    )

    /**
     * The pre-refresh dark table, unchanged: same gradient, same vignette, same sheen.
     *
     * It deliberately sits at ~24% lightness rather than ~12%: on an OLED panel a near-black table
     * is indistinguishable from a black sky and makes the sandbox read as an astronomy renderer,
     * while a neutral slate felt is unmistakably a surface with objects resting on it. The softened
     * vignette is part of the same decision — the corners never fall back to black.
     */
    val CHARCOAL = GravitySurface(
        key = "charcoal",
        titleFa = "زغالی",
        titleEn = "Charcoal",
        chromeMode = ChromeMode.DARK,
        gradient = SurfaceGradient(
            type = SurfaceGradientType.LINEAR,
            startArgb = 0xFF3A414BL,
            endArgb = 0xFF2E343DL,
            angleDegrees = 180f
        ),
        vignetteArgb = 0x3D000000L,
        sheenArgb = 0x0AFFFFFFL,
        pattern = null,
        trailAlphaOld = TRAIL_OLD_DARK,
        trailAlphaNew = TRAIL_NEW_DARK
    )

    val OCEAN = GravitySurface(
        key = "ocean",
        titleFa = "اقیانوسِ ژرف",
        titleEn = "Deep Ocean",
        chromeMode = ChromeMode.DARK,
        gradient = SurfaceGradient(
            type = SurfaceGradientType.LINEAR,
            startArgb = 0xFF10263BL,
            endArgb = 0xFF071523L,
            angleDegrees = 180f
        ),
        vignetteArgb = 0x3D020A12L,
        sheenArgb = 0x0AFFFFFFL,
        pattern = null,
        trailAlphaOld = TRAIL_OLD_DARK,
        trailAlphaNew = TRAIL_NEW_DARK
    )

    val LAVENDER = GravitySurface(
        key = "lavender",
        titleFa = "گرگ‌ومیشِ بنفش",
        titleEn = "Lavender Dusk",
        chromeMode = ChromeMode.DARK,
        gradient = SurfaceGradient(
            type = SurfaceGradientType.RADIAL,
            startArgb = 0xFF2C2148L,
            endArgb = 0xFF140E24L,
            centerX = 0.50f,
            centerY = 0.38f,
            radiusFraction = 0.95f
        ),
        vignetteArgb = 0x400B0716L,
        sheenArgb = 0x0AFFFFFFL,
        pattern = StarPattern(
            seed = 21,
            dotCount = 90,
            brightCount = 0,
            minRadiusDp = 0.7f,
            maxRadiusDp = 1.3f,
            minAlpha = 0.14f,
            maxAlpha = 0.35f
        ),
        trailAlphaOld = TRAIL_OLD_DARK,
        trailAlphaNew = TRAIL_NEW_DARK
    )

    /** The pre-refresh light table, unchanged: same gradient, same vignette, same sheen. */
    val PAPER = GravitySurface(
        key = LEGACY_LIGHT_KEY,
        titleFa = "کاغذِ گرم",
        titleEn = "Warm Paper",
        chromeMode = ChromeMode.LIGHT,
        gradient = SurfaceGradient(
            type = SurfaceGradientType.LINEAR,
            startArgb = 0xFFF4F1EAL,
            endArgb = 0xFFE9E4D9L,
            angleDegrees = 180f
        ),
        vignetteArgb = 0x1A5B5344L,
        sheenArgb = 0x0A000000L,
        pattern = null,
        trailAlphaOld = TRAIL_OLD_LIGHT,
        trailAlphaNew = TRAIL_NEW_LIGHT
    )

    val PORCELAIN = GravitySurface(
        key = "porcelain",
        titleFa = "مهِ صبح",
        titleEn = "Morning Mist",
        chromeMode = ChromeMode.LIGHT,
        gradient = SurfaceGradient(
            type = SurfaceGradientType.LINEAR,
            startArgb = 0xFFF8FAFCL,
            endArgb = 0xFFE2E8F0L,
            angleDegrees = 180f
        ),
        vignetteArgb = 0x1F94A3B8L,
        sheenArgb = 0x0A000000L,
        pattern = null,
        trailAlphaOld = TRAIL_OLD_LIGHT,
        trailAlphaNew = TRAIL_NEW_LIGHT
    )

    val BLUSH = GravitySurface(
        key = "blush",
        titleFa = "شنِ گلبهی",
        titleEn = "Blush Sand",
        chromeMode = ChromeMode.LIGHT,
        gradient = SurfaceGradient(
            type = SurfaceGradientType.RADIAL,
            startArgb = 0xFFFBF1ECL,
            endArgb = 0xFFEFDCCFL,
            centerX = 0.50f,
            centerY = 0.38f,
            radiusFraction = 0.95f
        ),
        vignetteArgb = 0x1FC2A08CL,
        sheenArgb = 0x0A000000L,
        pattern = null,
        trailAlphaOld = TRAIL_OLD_LIGHT,
        trailAlphaNew = TRAIL_NEW_LIGHT
    )

    /** Presentation order: dark finishes first, then light ones. */
    val all: List<GravitySurface> = listOf(
        MIDNIGHT, CHARCOAL, OCEAN, LAVENDER, PAPER, PORCELAIN, BLUSH
    )

    private val byKey: Map<String, GravitySurface> = all.associateBy { it.key }

    private val dotCache: MutableMap<StarPattern, List<StarDot>> = HashMap()

    fun byKey(key: String?): GravitySurface? = if (key == null) null else byKey[key]

    /** Unknown, blank or stale keys fall back to the default surface rather than failing. */
    fun resolve(key: String?): GravitySurface = byKey(key) ?: MIDNIGHT

    fun isDarkChrome(key: String?): Boolean = resolve(key).isDark

    /**
     * §7 — migration of the pre-refresh boolean theme flag: the two old themes live on as surfaces,
     * and a session that carried no flag at all lands on the new default.
     */
    fun keyFromLegacyDarkTheme(dark: Boolean): String = if (dark) DEFAULT_KEY else LEGACY_LIGHT_KEY

    /**
     * The printed dot field for [pattern], generated once and shared by every canvas size.
     *
     * Cached because the renderer asks for it whenever the surface cache is rebuilt; the underlying
     * generator is deterministic, so caching can never change what is printed.
     */
    fun starDots(pattern: StarPattern): List<StarDot> =
        dotCache.getOrPut(pattern) { generateStarDots(pattern) }

    /**
     * The generator itself, exposed so determinism can be verified: the same seed must print the
     * same dots, in the same order, every single time.
     *
     * Dots are placed uniformly over the canvas except for the exact centre 15% box, which stays
     * clean so the play area is never busy. Plain filled circles only — no glow, no lines.
     */
    fun generateStarDots(pattern: StarPattern): List<StarDot> {
        val random = Random(pattern.seed.toLong())
        val dots = ArrayList<StarDot>(pattern.totalDots)
        val limit = pattern.totalDots * 64
        var guard = 0
        while (dots.size < pattern.totalDots && guard < limit) {
            guard++
            val x = random.nextFloat()
            val y = random.nextFloat()
            if (isInCentreExclusion(x, y)) continue
            val bright = dots.size >= pattern.dotCount
            val radiusDp = if (bright) {
                pattern.brightRadiusDp
            } else {
                pattern.minRadiusDp + (pattern.maxRadiusDp - pattern.minRadiusDp) * random.nextFloat()
            }
            val alpha = if (bright) {
                pattern.brightAlpha
            } else {
                pattern.minAlpha + (pattern.maxAlpha - pattern.minAlpha) * random.nextFloat()
            }
            dots.add(StarDot(x, y, radiusDp, alpha))
        }
        return dots
    }

    private fun isInCentreExclusion(x: Float, y: Float): Boolean {
        val half = CENTRE_EXCLUSION / 2f
        return abs(x - 0.5f) <= half && abs(y - 0.5f) <= half
    }
}

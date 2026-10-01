package com.zig.chal.config

import java.util.Locale

/**
 * Locale-stable number and label formatting for the Chal HUD.
 *
 * Every number is formatted with an explicit US locale first — so an fa-IR device cannot silently
 * change the decimal separator or the digits — and only then mapped to Persian digits when the
 * interface is in Persian. An `LTR` mark is prepended to Persian numerics so a signed or fractional
 * value never reorders inside an RTL row.
 *
 * Kept in the config layer (no Compose, no Android) so the HUD's text can be unit-tested on the JVM;
 * the Compose layer only decides *where* these strings appear.
 */
object ChalFormatting {

    private val PERSIAN_DIGITS = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    /** Left-to-right mark: keeps a numeric run from being reordered by an RTL paragraph. */
    private const val LRM = '\u200E'

    /** Maps 0-9 to Persian digits, leaving every other character alone. */
    fun digits(text: String, isPersian: Boolean): String {
        if (!isPersian) return text
        var needs = false
        for (ch in text) {
            if (ch in '0'..'9') {
                needs = true
                break
            }
        }
        if (!needs) return text
        val out = StringBuilder(text.length + 1)
        out.append(LRM)
        for (ch in text) {
            out.append(if (ch in '0'..'9') PERSIAN_DIGITS[ch - '0'] else ch)
        }
        return out.toString()
    }

    /** Fixed-point, locale-stable rendering of a control value. */
    fun fixed(value: Double, decimals: Int, isPersian: Boolean): String =
        digits(String.format(Locale.US, "%.${decimals}f", value), isPersian)

    /** Integer rendering of a count (ray steps, FPS, radii). */
    fun integer(value: Int, isPersian: Boolean): String =
        digits(String.format(Locale.US, "%d", value), isPersian)

    /** A 0..1 fraction as a percentage, without the sign being reordered in RTL. */
    fun percent(fraction: Double, isPersian: Boolean): String =
        digits(String.format(Locale.US, "%.0f%%", fraction * 100.0), isPersian)

    /**
     * The real ray budget of a quality tier, e.g. `"80 steps · capped"` / `"۱۲۸ گام"`.
     *
     * The picker used to offer High and Ultra as if they were different renders; on this build both
     * clamp to the same mobile budget, so the cost is printed next to each tier and the cap is named.
     */
    fun rayBudget(quality: ChalRayTracingQuality, isPersian: Boolean, isMobile: Boolean): String {
        if (quality == ChalRayTracingQuality.OFF) {
            return if (isPersian) "بدون ردیابی پرتو" else "no ray marching"
        }
        val steps = ChalFeatures.getMaxRaySteps(quality, isMobile)
        val count = integer(steps, isPersian)
        val capped = ChalFeatures.isCappedForMobile(quality)
        return when {
            capped && isPersian -> "$count گام · سقف موبایل"
            capped -> "$count steps · capped"
            isPersian -> "$count گام"
            else -> "$count steps"
        }
    }

    /**
     * A one-line note when the visible tiers collapse onto the same render, or null when every tier
     * is distinct. Naming the duplication is the honest alternative to hiding a redundant button.
     */
    fun duplicateTierNote(
        visible: List<ChalRayTracingQuality>,
        isPersian: Boolean
    ): String? {
        val pairs = mutableListOf<Pair<ChalRayTracingQuality, ChalRayTracingQuality>>()
        for (i in visible.indices) {
            for (j in i + 1 until visible.size) {
                if (ChalFeatures.rendersIdentically(visible[i], visible[j])) {
                    pairs += visible[i] to visible[j]
                }
            }
        }
        if (pairs.isEmpty()) return null
        val names = pairs.joinToString(if (isPersian) "، " else ", ") { (a, b) ->
            val left = a.label(isPersian)
            val right = b.label(isPersian)
            if (isPersian) "$left و $right" else "$left and $right"
        }
        return if (isPersian) {
            "$names روی این دستگاه یکسان رندر می‌شوند."
        } else {
            "$names render identically on this device."
        }
    }

    /** Bilingual caption for the active backend, used by the header chip. */
    fun backendLabel(isPersian: Boolean, isNative: Boolean): String = when {
        isNative && isPersian -> "بومی · ولکان"
        isNative -> "Native · Vulkan"
        isPersian -> "جایگزین · GLES"
        else -> "Fallback · GLES"
    }
}

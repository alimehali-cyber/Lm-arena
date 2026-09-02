package com.zig.gravity.util

/**
 * §3.14 — Persian digits by default, with a Latin fallback the user can choose.
 * Pure Kotlin so it is unit-testable without an emulator.
 */
object PersianDigits {

    private val FA = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    fun convert(text: String): String {
        var needs = false
        for (ch in text) {
            if (ch in '0'..'9') { needs = true; break }
        }
        if (!needs) return text
        val sb = StringBuilder(text.length)
        for (ch in text) {
            sb.append(if (ch in '0'..'9') FA[ch - '0'] else ch)
        }
        return sb.toString()
    }

    /** Applies Persian digits only when the sandbox is in Persian mode. */
    fun localize(text: String, persian: Boolean): String = if (persian) convert(text) else text
}

/**
 * Locale-independent number formatting for the sandbox.
 *
 * Everything is formatted with an explicit US locale first (so the decimal separator is stable on
 * an fa-IR device) and only then converted to Persian digits when the user is in Persian mode.
 */
object SandboxFormat {

    fun fixed(value: Double, decimals: Int, persian: Boolean): String =
        PersianDigits.localize(String.format(java.util.Locale.US, "%.${decimals}f", value), persian)

    fun integer(value: Long, persian: Boolean): String =
        PersianDigits.localize(value.toString(), persian)

    /** Mass in Earth masses, or solar masses once it gets large. */
    fun mass(massKg: Double, persian: Boolean): String {
        val mEarth = 5.972e24
        val mSun = 1.989e30
        return when {
            massKg <= 0.0 -> if (persian) "بی‌جرم" else "massless"
            massKg >= 0.05 * mSun -> {
                val v = massKg / mSun
                val body = fixed(v, if (v < 10) 2 else 1, persian)
                if (persian) "$body جرم خورشید" else "$body M☉"
            }
            else -> {
                val v = massKg / mEarth
                val body = fixed(v, if (v < 10) 3 else 1, persian)
                if (persian) "$body جرم زمین" else "$body M⊕"
            }
        }
    }

    fun kilograms(massKg: Double, persian: Boolean): String {
        if (massKg <= 0.0) return if (persian) "۰ kg" else "0 kg"
        val exp = Math.floor(Math.log10(massKg)).toInt()
        val mantissa = massKg / Math.pow(10.0, exp.toDouble())
        val body = String.format(java.util.Locale.US, "%.2f×10^%d kg", mantissa, exp)
        return PersianDigits.localize(body, persian)
    }

    /** Energy in joules, in scientific notation once it leaves everyday range (§15). */
    fun joules(energyJ: Double, persian: Boolean): String {
        if (energyJ <= 0.0) return if (persian) "۰ ژول" else "0 J"
        if (energyJ < 1.0e4) {
            val body = fixed(energyJ, 1, persian)
            return if (persian) "$body ژول" else "$body J"
        }
        val exp = Math.floor(Math.log10(energyJ)).toInt()
        val mantissa = energyJ / Math.pow(10.0, exp.toDouble())
        val body = String.format(java.util.Locale.US, "%.2f×10^%d", mantissa, exp)
        val localized = PersianDigits.localize(body, persian)
        return if (persian) "$localized ژول" else "$localized J"
    }

    /** Speed in km/s with a m/s fallback for very slow bodies. */
    fun speed(metersPerSecond: Double, persian: Boolean): String = if (metersPerSecond >= 1000.0) {
        val body = fixed(metersPerSecond / 1000.0, 2, persian)
        if (persian) "$body کیلومتر بر ثانیه" else "$body km/s"
    } else {
        val body = fixed(metersPerSecond, 1, persian)
        if (persian) "$body متر بر ثانیه" else "$body m/s"
    }

    /** Physical distance in km / AU. */
    fun distance(meters: Double, persian: Boolean): String {
        val au = 1.496e11
        return if (meters >= 0.02 * au) {
            val body = fixed(meters / au, 3, persian)
            if (persian) "$body واحد نجومی" else "$body AU"
        } else {
            val body = fixed(meters / 1000.0, 0, persian)
            if (persian) "$body کیلومتر" else "$body km"
        }
    }

    fun degrees(deg: Double, persian: Boolean): String =
        PersianDigits.localize(String.format(java.util.Locale.US, "%.0f°", deg), persian)

    /** Simulated time as days / years. */
    fun simTime(seconds: Double, persian: Boolean): String {
        val year = 3.15576e7
        val day = 86400.0
        return if (seconds >= year) {
            val body = fixed(seconds / year, 2, persian)
            if (persian) "$body سال" else "$body yr"
        } else {
            val body = fixed(seconds / day, 1, persian)
            if (persian) "$body روز" else "$body d"
        }
    }
}

package com.zig.antar.physics

import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Analytical Kerr-Schild general-relativistic observables matching `window.KerrPhysics` in the
 * WebGL2 geodesic ray-tracing engine ($G = c = M = 1$ geometric units unless converted to SI).
 */
object AntarKerrMetrics {

    /** Gravitational radius per solar mass $GM_\odot / c^2$ in kilometres. */
    const val KM_PER_SOLAR_MASS: Double = 1.4766250385

    /** Gravitational time per solar mass $GM_\odot / c^3$ in seconds. */
    const val SEC_PER_SOLAR_MASS: Double = 4.925490947e-6

    /** Astronomical unit in kilometres (IAU 2012 exact). */
    const val KM_PER_AU: Double = 149_597_870.7

    /** Outer event horizon $r_+ / M = 1 + \sqrt{1 - a^2}$. */
    fun outerHorizonM(spin: Double): Double {
        val a = spin.coerceIn(0.0, 0.9999)
        return 1.0 + sqrt((1.0 - a * a).coerceAtLeast(0.0))
    }

    /** Inner Cauchy horizon $r_- / M = 1 - \sqrt{1 - a^2}$. */
    fun innerHorizonM(spin: Double): Double {
        val a = spin.coerceIn(0.0, 0.9999)
        return 1.0 - sqrt((1.0 - a * a).coerceAtLeast(0.0))
    }

    /**
     * Equatorial innermost stable circular orbit $r_{\text{ISCO}} / M$
     * (Bardeen, Press & Teukolsky 1972). Positive [spin] is prograde.
     */
    fun iscoRadiusM(spin: Double, prograde: Boolean = true): Double {
        val a = spin.coerceIn(0.0, 0.9999)
        val oneMinusA2 = (1.0 - a * a).coerceAtLeast(0.0)
        val z1 = 1.0 + cbrt(oneMinusA2) * (cbrt(1.0 + a) + cbrt((1.0 - a).coerceAtLeast(0.0)))
        val z2 = sqrt(3.0 * a * a + z1 * z1)
        val sign = if (prograde) -1.0 else 1.0
        val disc = ((3.0 - z1) * (3.0 + z1 + 2.0 * z2)).coerceAtLeast(0.0)
        return 3.0 + z2 + sign * sqrt(disc)
    }

    /**
     * Equatorial circular photon orbit radius $r_{\text{ph}} / M$.
     * Prograde shrinks toward $1\,M$ as $a \to 1$; retrograde expands toward $4\,M$.
     */
    fun photonOrbitM(spin: Double, prograde: Boolean): Double {
        val a = abs(spin).coerceIn(0.0, 0.9999)
        val arg = (if (prograde) -a else a).coerceIn(-1.0, 1.0)
        return 2.0 * (1.0 + cos((2.0 / 3.0) * acos(arg)))
    }

    /**
     * Static-observer gravitational time-dilation factor $d\tau / dt = \sqrt{-g_{tt}}$ at
     * Boyer-Lindquist / Kerr-Schild radius [radiusM] and polar inclination [inclinationDeg].
     * Returns `null` inside the ergosphere static limit ($r^2 + a^2 \cos^2\theta \le 2r$).
     */
    fun staticTimeDilation(spin: Double, radiusM: Double, inclinationDeg: Double): Double? {
        val a = spin.coerceIn(0.0, 0.9999)
        val r = radiusM.coerceAtLeast(0.01)
        val cosTheta = cos(Math.toRadians(inclinationDeg))
        val sigma = r * r + a * a * cosTheta * cosTheta
        val minusGtt = 1.0 - (2.0 * r) / sigma.coerceAtLeast(1e-12)
        return if (minusGtt > 1e-8) sqrt(minusGtt) else null
    }

    /** Equatorial horizon circumferential radius $\sqrt{r_+^2 + a^2} \cdot r_g$ in kilometres. */
    fun horizonCircumferentialRadiusKm(spin: Double, solarMasses: Double): Double {
        val a = spin.coerceIn(0.0, 0.9999)
        val rh = outerHorizonM(a)
        return sqrt(rh * rh + a * a) * solarMasses * KM_PER_SOLAR_MASS
    }

    /** Coordinate orbital period at prograde ISCO $T_{\text{ISCO}} = 2\pi(r_{\text{ISCO}}^{3/2} + a)\,t_g$ in seconds. */
    fun iscoOrbitalPeriodSec(spin: Double, solarMasses: Double): Double {
        val a = spin.coerceIn(0.0, 0.9999)
        val rIsco = iscoRadiusM(a, prograde = true)
        return 2.0 * PI * (rIsco.pow(1.5) + a) * solarMasses * SEC_PER_SOLAR_MASS
    }

    /**
     * Exact Novikov–Thorne radiative efficiency $\eta = 1 - E_{\text{ISCO}}(a)$ for a thin prograde
     * equatorial accretion disk (Bardeen, Press & Teukolsky 1972).
     */
    fun radiativeEfficiency(spin: Double): Double {
        val a = spin.coerceIn(0.0, 0.9999)
        val r = iscoRadiusM(a, prograde = true)
        val sqr = sqrt(r)
        val num = 1.0 - 2.0 / r + a / (r * sqr)
        val den = sqrt((1.0 - 3.0 / r + 2.0 * a / (r * sqr)).coerceAtLeast(1e-12))
        return (1.0 - num / den).coerceIn(0.0, 0.45)
    }

    /** Bundled Kerr observables snapshot for UI telemetry cards. */
    data class Observables(
        val horizonRadiusM: Double,
        val iscoProgradeM: Double,
        val photonOrbitProgradeM: Double,
        val radiativeEfficiency: Double,
        val gravitationalTimeDilation: Double
    )

    fun computeObservables(
        spin: Double,
        observerRadiusM: Double,
        inclinationDeg: Double = 90.0
    ): Observables = Observables(
        horizonRadiusM = outerHorizonM(spin),
        iscoProgradeM = iscoRadiusM(spin, prograde = true),
        photonOrbitProgradeM = photonOrbitM(spin, prograde = true),
        radiativeEfficiency = radiativeEfficiency(spin),
        gravitationalTimeDilation = staticTimeDilation(spin, observerRadiusM, inclinationDeg) ?: 0.0
    )

    /** Minimum safe orbit camera distance $r_{\min} / M$ outside the static limit / binary cavity. */
    fun minOrbitDistanceM(spin: Double, isBinaryMerger: Boolean, binarySeparationM: Double = 14.0): Double {
        return if (isBinaryMerger) {
            1.5 * binarySeparationM + 5.0
        } else {
            3.2 + spin.coerceIn(0.0, 0.998) * 0.4
        }
    }
}

/**
 * Locale-aware numeric formatter and parser for ZIG's Blackhole controls and telemetry.
 * Produces Eastern Persian digits (`۰`–`۹` and `٫`) when `isPersian == true`, and clean
 * English tabular numbers otherwise.
 */
object AntarNumberFormat {

    private val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    fun localizeDigits(text: String, isPersian: Boolean): String {
        if (!isPersian) return text
        val sb = StringBuilder(text.length)
        for (ch in text) {
            when (ch) {
                in '0'..'9' -> sb.append(persianDigits[ch - '0'])
                '.' -> sb.append('٫')
                ',' -> sb.append('٬')
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun decimal(value: Double, decimals: Int, isPersian: Boolean): String {
        val safe = if (value.isFinite()) value else 0.0
        val raw = String.format(Locale.US, "%.${decimals.coerceIn(0, 6)}f", safe)
        return localizeDigits(raw, isPersian)
    }

    fun integer(value: Number, isPersian: Boolean): String {
        val raw = String.format(Locale.US, "%,d", value.toLong())
        return localizeDigits(raw, isPersian)
    }

    fun percent(fraction: Double, isPersian: Boolean): String {
        val pct = (fraction * 100.0).roundToInt()
        return if (isPersian) "${localizeDigits(pct.toString(), true)}٪" else "$pct%"
    }

    fun scientific(value: Double, decimals: Int = 1, isPersian: Boolean): String {
        if (!value.isFinite() || value <= 0.0) return localizeDigits("0", isPersian)
        var exp = floor(log10(value)).toInt()
        var mantissa = value / 10.0.pow(exp)
        val factor = 10.0.pow(decimals)
        if ((mantissa * factor).roundToInt() / factor >= 10.0) {
            mantissa /= 10.0
            exp += 1
        }
        val supDigits = arrayOf('⁰', '¹', '²', '³', '⁴', '⁵', '⁶', '⁷', '⁸', '⁹')
        val expText = buildString {
            for (c in exp.toString()) {
                if (c == '-') append('⁻') else if (c in '0'..'9') append(supDigits[c - '0'])
            }
        }
        return "${decimal(mantissa, decimals, isPersian)}×${localizeDigits("10", isPersian)}$expText"
    }

    fun lengthKm(km: Double, isPersian: Boolean): String {
        val a = abs(km)
        return when {
            a < 1_000.0 -> "${decimal(km, 1, isPersian)} ${if (isPersian) "کیلومتر" else "km"}"
            a < 1.0e6 -> "${decimal(km / 1_000.0, 1, isPersian)} ${if (isPersian) "هزار کیلومتر" else "×10³ km"}"
            a < 1.5e8 -> "${decimal(km / 1.0e6, 1, isPersian)} ${if (isPersian) "میلیون کیلومتر" else "×10⁶ km"}"
            else -> "${decimal(km / AntarKerrMetrics.KM_PER_AU, 2, isPersian)} AU"
        }
    }

    fun durationSec(sec: Double, isPersian: Boolean): String {
        val a = abs(sec)
        return when {
            a < 1e-3 -> "${decimal(sec * 1e6, 1, isPersian)} ${if (isPersian) "میکروثانیه" else "µs"}"
            a < 1.0 -> "${decimal(sec * 1e3, 1, isPersian)} ${if (isPersian) "میلی‌ثانیه" else "ms"}"
            a < 60.0 -> "${decimal(sec, 1, isPersian)} ${if (isPersian) "ثانیه" else "s"}"
            a < 3_600.0 -> {
                val m = floor(a / 60.0).toInt()
                val s = (a % 60.0).roundToInt()
                if (isPersian) "${integer(m, true)} دقیقه و ${integer(s, true)} ثانیه"
                else "${m}m ${s}s"
            }
            a < 86_400.0 * 2.0 -> {
                val h = floor(a / 3_600.0).toInt()
                val m = ((a % 3_600.0) / 60.0).roundToInt()
                if (isPersian) "${integer(h, true)} ساعت و ${integer(m, true)} دقیقه"
                else "${h}h ${m}m"
            }
            a < 86_400.0 * 365.25 * 2.0 ->
                "${decimal(sec / 86_400.0, 1, isPersian)} ${if (isPersian) "روز" else "d"}"
            else ->
                "${decimal(sec / (86_400.0 * 365.25), 1, isPersian)} ${if (isPersian) "سال" else "yr"}"
        }
    }

    /** Parses Latin, Persian (`۰`–`۹`), or Arabic-Indic (`٠`–`٩`) numeric input safely. */
    fun parseFlexibleDouble(input: String): Double? {
        val normalized = buildString(input.length) {
            for (ch in input.trim()) {
                when (ch) {
                    in '۰'..'۹' -> append(('0'.code + (ch.code - '۰'.code)).toChar())
                    in '٠'..'٩' -> append(('0'.code + (ch.code - '٠'.code)).toChar())
                    '٫', '،', ',' -> append('.')
                    '−', '–', '—' -> append('-')
                    ' ', '\u200C', '\u200E', '\u200F' -> Unit
                    else -> append(ch)
                }
            }
        }
        val parsed = normalized.toDoubleOrNull() ?: return null
        return parsed.takeIf { it.isFinite() }
    }
}

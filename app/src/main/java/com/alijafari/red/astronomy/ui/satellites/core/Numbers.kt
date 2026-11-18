package com.alijafari.red.astronomy.ui.satellites.core

import java.util.*
import kotlin.math.abs

internal fun isolate(s: String) = "\u2066$s\u2069"

internal fun digits(raw: String, fa: Boolean) =
    if (!fa) raw
    else
        buildString {
            raw.forEachIndexed { index, c ->
                val betweenDigits = index > 0 && index + 1 < raw.length &&
                    raw[index - 1].isDigit() && raw[index + 1].isDigit()
                append(when (c) {
                    in '0'..'9' -> '۰' + (c - '0')
                    in '٠'..'٩' -> '۰' + (c - '٠')
                    '.' -> if (betweenDigits) '٫' else c
                    ',' -> if (betweenDigits) '٬' else c
                    else -> c
                })
            }
        }

/** Format legacy catalogue prose at the presentation boundary without mutating canonical data. */
internal fun localizedProse(raw: String, fa: Boolean): String {
    if (!fa) return raw
    val text = digits(raw.replace("T-شکل", "شبیه حرف «تی»"), true)
    return Regex("([۰-۹]+(?:٫[۰-۹]+)?)-").replace(text) { isolate("−" + it.groupValues[1]) }
}

internal fun fmtSigned(value: Double, decimals: Int, fa: Boolean, plus: Boolean = false, suffix: String = ""): String {
    if (!value.isFinite()) return "—"
    val body = String.format(Locale.US, "%.${decimals}f", abs(value))
    val sign = if (value < 0 && body.any { it in '1'..'9' }) "−" else if (plus && value > 0) "+" else ""
    return isolate(sign + digits(body, fa) + suffix)
}

internal fun gregorianFa(ms: Long, zone: TimeZone): String {
    val cal = Calendar.getInstance(zone).apply { timeInMillis = ms }
    val month =
        listOf(
            "ژانویه",
            "فوریه",
            "مارس",
            "آوریل",
            "مه",
            "ژوئن",
            "ژوئیه",
            "اوت",
            "سپتامبر",
            "اکتبر",
            "نوامبر",
            "دسامبر",
        )[cal[Calendar.MONTH]]
    return digits("${cal[Calendar.DAY_OF_MONTH]} $month", true)
}

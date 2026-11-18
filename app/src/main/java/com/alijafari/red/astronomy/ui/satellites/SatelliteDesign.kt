package com.alijafari.red.astronomy.ui.satellites

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import com.alijafari.red.astronomy.R
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.astro_engine.ISSEngine
import com.alijafari.red.astronomy.astro_engine.SatelliteCategory
import com.alijafari.red.astronomy.astro_engine.SatelliteItem
import com.alijafari.red.astronomy.astro_engine.TimeEngine
import com.alijafari.red.astronomy.domain.ThemeMode
import com.alijafari.red.astronomy.ui.theme.RedShapes
import com.alijafari.red.astronomy.ui.theme.RedSpacing
import com.alijafari.red.astronomy.ui.theme.RedSurface
import com.alijafari.red.astronomy.ui.theme.RedTheme
import com.alijafari.red.astronomy.ui.theme.RedTypographyTokens
import com.alijafari.red.astronomy.ui.theme.WineAccentCrimson
import com.alijafari.red.astronomy.ui.satellites.core.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.delay

/** One globe and one HUD, with three treatments of the same material slots. */
enum class SatSkin {
    DEFAULT,
    SPATIAL,
    AETHER,
}

/** Composition-local so deeply nested components can reach the active skin without a parameter chain. */
val LocalSatSkin = compositionLocalOf { SatSkin.DEFAULT }

/** Grid + touch contract shared by every skin (4 dp grid, 48 dp minimum target). */
object SatGrid {
    val screenPadding: Dp = 16.dp
    val screenPaddingLarge: Dp = 20.dp
    val gapInGroup: Dp = 12.dp
    val gapBetweenGroups: Dp = 24.dp
    val minTouchTarget: Dp = 48.dp
    val bottomClearance: Dp = 112.dp
}

/** Continuous squircle radii from the specification. */
object SatRadius {
    val card: Dp = 24.dp
    val compactCard: Dp = 20.dp
    val tile: Dp = 12.dp
    val popover: Dp = 16.dp
    val sheet: Dp = 28.dp
    val aetherFrosted: Dp = 32.dp
    val aetherJewel: Dp = 36.dp
}

/**
 * A skin's semantic palette. Components never reach for a hex value: they ask for `live`, `sim`,
 * `selected`, `umbra`, and the token resolves per skin × theme.
 */
@Immutable
data class ZigSatTokens(
    val skin: SatSkin,
    val themeMode: ThemeMode,
    val isDark: Boolean,
    val isWine: Boolean,
    val canvas: Color,
    val card: Color,
    val cardAlt: Color,
    val border: Color,
    val inkPrimary: Color,
    val inkSecondary: Color,
    val inkTertiary: Color,
    val live: Color,
    val sim: Color,
    val selected: Color,
    val selectedHalo: Color,
    val nakedEye: Color,
    val telescopic: Color,
    val starlink: Color,
    val umbra: Color,
    val popoverFill: Color,
    val popoverBorder: Color,
    val glassy: Boolean,
    val hudFill: Color,
    val hudInk: Color,
    val accentInk: Color,
    val selectedChip: Color,
    val timeAccent: Color,
    val stars: Color,
) {
    /** Selected ring colour for the fleet rail token. */
    val selectedRing: Color get() = selected

    fun qualityChipFill(quality: SatQuality): Color =
        quality.hue.copy(alpha = if (isWine) 0.18f else if (isDark) 0.16f else 0.12f)

    fun qualityChipBorder(quality: SatQuality): Color =
        quality.graphic(this).copy(alpha = if (isDark) 0.55f else 0.45f)

    val selectedFill: Color
        get() = selected.copy(alpha = if (isDark) 0.14f else 0.10f)
}

/** Resolves the semantic tokens for a skin × theme pair. */
fun zigSatTokens(skin: SatSkin, themeMode: ThemeMode): ZigSatTokens {
    val isWine = themeMode == ThemeMode.WINE
    val isDark = themeMode != ThemeMode.LIGHT
    val canvas = when {
        isWine -> Color(0xFF120408)
        isDark -> Color(0xFF090A0F)
        else -> Color.White
    }
    val card = when {
        isWine -> Color(0xFF1B070E)
        isDark -> Color(0xFF12131C)
        else -> Color(0xFFFAF8F5)
    }
    val cardAlt = when {
        isWine -> Color(0xFF260B15)
        isDark -> Color(0xFF1A1B26)
        else -> Color(0xFFF0ECE5)
    }
    val border = when {
        isWine -> Color(0xFF3A1220)
        isDark -> Color(0x1FFFFFFF)
        else -> Color(0xFFEAE5D9)
    }
    val inkPrimary = when {
        isWine -> Color(0xFFFCF0F3)
        isDark -> Color(0xFFFAF9F6)
        else -> Color(0xFF111118)
    }
    val inkSecondary = when {
        isWine -> Color(0xCCEDD2D9)
        isDark -> Color(0xFFC2C2CA)
        else -> Color(0xFF4A4A5A)
    }
    val inkTertiary = when {
        isWine -> Color(0x99EDD2D9)
        isDark -> Color(0xFF8E8E9A)
        else -> Color(0xFF666675)
    }
    val live = if (isWine) Color(0xFFE04263) else Color(0xFFD32F2F)
    val sim = when {
        isWine -> Color(0xFFE2A86F)
        isDark -> Color(0xFFC5A059)
        else -> Color(0xFF8A6A1F)
    }
    val selected = when {
        isWine -> Color(0xFFFCF0F3)
        isDark -> Color(0xFFFAF9F6)
        else -> Color(0xFF182238)
    }
    val selectedHalo = when {
        isWine -> Color(0xFFE04263)
        isDark -> Color(0xFFD32F2F)
        else -> Color(0xFF800020)
    }
    return ZigSatTokens(
        skin = skin,
        themeMode = themeMode,
        isDark = isDark,
        isWine = isWine,
        canvas = canvas,
        card = card,
        cardAlt = cardAlt,
        border = border,
        inkPrimary = inkPrimary,
        inkSecondary = inkSecondary,
        inkTertiary = inkTertiary,
        live = live,
        sim = sim,
        selected = selected,
        selectedHalo = selectedHalo,
        nakedEye = if (isDark) Color(0xFF2DC653) else Color(0xFF1E8E3E),
        telescopic = inkPrimary.copy(alpha = 0.5f),
        starlink = if (isDark) Color(0xFF38BDF8) else Color(0xFF0B7BB5),
        umbra = when {
            isWine -> Color(0xFFC79AB0)
            isDark -> Color(0xFF8FA3C7)
            else -> Color(0xFF5B6B8C)
        },
        popoverFill = card,
        popoverBorder = selected,
        glassy = skin != SatSkin.DEFAULT,
        hudFill = when (themeMode) {
            ThemeMode.LIGHT -> Color(0xFFFDFBF7)
            ThemeMode.WINE -> Color(0xFF261019)
            else -> Color(0xFF171923)
        },
        hudInk = inkPrimary,
        accentInk = if (isDark) Color(0xFFFF899B) else Color(0xFF9B2038),
        selectedChip = if (isWine) Color(0xFFA62045) else Color(0xFFAC243D),
        timeAccent = when (skin) {
            SatSkin.AETHER -> if (isDark) Color(0xFF86AFFF) else Color(0xFF2350AB)
            SatSkin.SPATIAL -> if (isDark) Color(0xFFFF739C) else Color(0xFFC12960)
            SatSkin.DEFAULT -> live
        },
        stars = if (isDark) Color(0xFF9DA9C6) else Color(0xFF576784),
    )
}

/** The six quality bands. Colour is never the sole carrier: every use is paired with a label or a score. */
enum class SatQuality(
    val rank: Int,
    val labelEn: String,
    val labelFa: String,
    val shortEn: String,
    val shortFa: String,
    val hue: Color,
) {
    OUTSTANDING(0, "Outstanding", "فوق‌العاده", "Best", "بهترین", Color(0xFF2DC653)),
    EXCELLENT(1, "Excellent", "عالی", "Best", "بهترین", Color(0xFF38B000)),
    VERY_GOOD(2, "Very good", "بسیار خوب", "Great", "خوب", Color(0xFF70E000)),
    GOOD(3, "Good", "خوب", "Good", "خوب", Color(0xFF9EF01A)),
    MARGINAL(4, "Marginal", "حاشیه‌ای", "Fair", "متوسط", Color(0xFFFFB703)),
    POOR(5, "Poor", "ضعیف", "Poor", "ضعیف", Color(0xFFFF8C00));

    fun label(isFa: Boolean): String = if (isFa) labelFa else labelEn
    fun shortLabel(isFa: Boolean): String = if (isFa) shortFa else shortEn
    /** A darker stroke of the same hue is legible against light surfaces. */
    fun graphic(tokens: ZigSatTokens): Color = if (tokens.isDark) hue else lerp(hue, Color.Black, .40f)
}

fun qualityForScore(score: Int): SatQuality = when {
    score >= 85 -> SatQuality.OUTSTANDING
    score >= 75 -> SatQuality.EXCELLENT
    score >= 65 -> SatQuality.VERY_GOOD
    score >= 55 -> SatQuality.GOOD
    score >= 45 -> SatQuality.MARGINAL
    else -> SatQuality.POOR
}

fun qualityOf(pass: ISSEngine.ISSPass): SatQuality = qualityForScore(pass.visibilityScore)

// ------------------------------------------------------------------------------------------------
// View models handed to the skin slots
// ------------------------------------------------------------------------------------------------

/** State chip content: one capsule, one fact set. */
@Immutable
data class SatStatusData(
    val isLive: Boolean,
    val offsetMinutes: Int,
    val isOnline: Boolean,
    val tleAgeDays: Double?,
    val tleEpochMillis: Long?,
    val tleSource: com.alijafari.red.astronomy.astro_engine.TleDataSource?,
    val isFa: Boolean,
    val lastFixMs: Long? = null,
)

@Immutable
data class SatTokenUi(
    val id: String,
    val shortName: String,
    val fullName: String,
    val isTrain: Boolean,
    val trainCount: Int,
    val elevationDeg: Double,
    val isNakedEye: Boolean,
    val isSelected: Boolean,
    val satellite: SatelliteItem,
)

@Immutable
data class SatPassUi(
    val key: String,
    val sat: SatelliteItem,
    val satName: String,
    val pass: ISSEngine.ISSPass,
    val nightKey: Long,
    val reminderLeadMinutes: Int?,
    val isAutoCovered: Boolean,
) {
    val quality: SatQuality get() = qualityOf(pass)
    val isTrain: Boolean get() = sat.isTrain
}

@Immutable
data class SatNightGroup(
    val nightKey: Long,
    val title: String,
    val subtitle: String?,
    val best: SatPassUi?,
    val passes: List<SatPassUi>,
)

@Immutable
data class SatSpotlightUi(
    val pass: SatPassUi,
    val isInProgress: Boolean,
    val remainingMinutes: Int,
    val simulatedInsidePass: Boolean,
)

/** Every action a pass surface can raise. Grouped so a pass card stays a pure renderer. */
@Immutable
data class SatPassActions(
    val onOpenDetail: () -> Unit,
    val onShowOnGlobe: () -> Unit,
    val onAr: () -> Unit,
    val onRemind: (androidx.compose.ui.unit.IntRect) -> Unit,
    val isReminderEnabled: Boolean = true,
)

@Immutable
data class SatTimeActions(
    val onOffsetChange: (Int) -> Unit,
    val onLive: () -> Unit,
    val onNextPass: () -> Unit,
    val onStep: (Int) -> Unit,
)

/** Reduce-motion flag, set once by the host from the platform animator scale. */
val LocalSatReduceMotion = compositionLocalOf { false }

// ------------------------------------------------------------------------------------------------
// Numerals, signs and direction text — the one place that formats a live number
// ------------------------------------------------------------------------------------------------

/** Bidirectional isolate markers, so a signed number never jumps to the wrong side in RTL. */
private const val MINUS = '\u2212'

object SatFmt {

    fun digits(raw: String,isFa: Boolean) = com.alijafari.red.astronomy.ui.satellites.core.digits(raw,isFa)
    fun signed(value: Double, decimals: Int, unit: String = "", isFa: Boolean, forceSign: Boolean = true) =
        fmtSigned(value,decimals,isFa,forceSign,unit)

    fun number(value: Double, decimals: Int, isFa: Boolean): String = when {
        !value.isFinite() -> "—"
        value < 0 -> signed(value, decimals, isFa = isFa, forceSign = false)
        else -> decimal(String.format(Locale.US, "%.${decimals}f", value), isFa)
    }

    private fun decimal(raw: String, isFa: Boolean): String =
        if (isFa) digits(raw.replace('.', '\u066B').replace(',', '\u066C'), true) else raw

    fun int(value: Int, isFa: Boolean): String = digits(value.toString().replace('-', MINUS), isFa)

    fun intPadded(value: Int, width: Int, isFa: Boolean): String =
        digits(value.toString().padStart(width, '0'), isFa)

    fun kilometers(km: Double, isFa: Boolean, decimals: Int = 0): String {
        if (!km.isFinite()) return "—"
        return decimal(String.format(Locale.US, "%,.${decimals}f", km).replace('-', MINUS), isFa)
    }

    fun speed(kmS: Double, isFa: Boolean): String =
        number(kmS, 2, isFa)

    fun degrees(value: Double, isFa: Boolean, decimals: Int = 0, signed: Boolean = false): String =
        fmtSigned(value, decimals, isFa, suffix = "°")

    fun magnitude(value: Double, isFa: Boolean): String = signed(value, 1, "", isFa)

    fun clock(ms: Long, isFa: Boolean, tz: TimeZone): String =
        digits(SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = tz }.format(Date(ms)), isFa)

    /** `2h 14m` / `۲ ساعت ۱۴ دقیقه`. */
    fun duration(minutes: Int, isFa: Boolean): String {
        val m = minutes.coerceAtLeast(0)
        val h = m / 60
        val mm = m % 60
        return if (isFa) {
            val measures = buildList {
                if (h > 0) add(android.icu.util.Measure(h, android.icu.util.MeasureUnit.HOUR))
                if (mm != 0 || h == 0) add(android.icu.util.Measure(mm, android.icu.util.MeasureUnit.MINUTE))
            }
            android.icu.text.MeasureFormat.getInstance(android.icu.util.ULocale("fa_IR"),
                android.icu.text.MeasureFormat.FormatWidth.WIDE).formatMeasures(*measures.toTypedArray())
        } else {
            when {
                h > 0 && mm > 0 -> "${h}h ${mm}m"
                h > 0 -> "${h}h"
                else -> "${mm}m"
            }
        }
    }

    /** Countdown rule: minute resolution above 10 min, seconds below. */
    fun countdown(remainingMs: Long, isFa: Boolean): String {
        val totalSeconds = (remainingMs / 1000L).coerceAtLeast(0L)
        return if (totalSeconds >= 600L) {
            duration((totalSeconds / 60L).toInt(), isFa)
        } else {
            val m = (totalSeconds / 60L).toInt()
            val s = (totalSeconds % 60L).toInt()
            val raw = "%d:%02d".format(Locale.US, m, s)
            digits(raw, isFa)
        }
    }

    fun offsetMinutes(minutes: Int, isFa: Boolean): String {
        if (minutes == 0) return if (isFa) "زنده" else "Live"
        val wholeHours=minutes % 60==0
        return if(kotlin.math.abs(minutes)>=60 && (minutes % 6==0)) fmtSigned(minutes/60.0,if(wholeHours)0 else 1,isFa,true,if(isFa) " ساعت" else "h")
        else fmtSigned(minutes.toDouble(),0,isFa,true,if(isFa) " دقیقه" else "m")
    }

    fun compass(azDeg: Double, isFa: Boolean): String {
        val norm = ((azDeg % 360.0) + 360.0) % 360.0
        return when {
            norm >= 337.5 || norm < 22.5 -> if (isFa) "شمال" else "N"
            norm < 67.5 -> if (isFa) "شمال‌شرق" else "NE"
            norm < 112.5 -> if (isFa) "شرق" else "E"
            norm < 157.5 -> if (isFa) "جنوب‌شرق" else "SE"
            norm < 202.5 -> if (isFa) "جنوب" else "S"
            norm < 247.5 -> if (isFa) "جنوب‌غرب" else "SW"
            norm < 292.5 -> if (isFa) "غرب" else "W"
            else -> if (isFa) "شمال‌غرب" else "NW"
        }
    }

    /** Reading order mirrors in RTL: rise at the start edge, arrow flipped. */
    fun dateTime(ms: Long, isFa: Boolean, tz: TimeZone): String {
        val date = SimpleDateFormat("EEE d MMM", Locale.US).apply { timeZone = tz }.format(Date(ms))
        val time = SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = tz }.format(Date(ms))
        return when {
            isFa -> {
                val format = android.icu.text.SimpleDateFormat("EEEE d MMMM · HH:mm", android.icu.util.ULocale("fa_IR@calendar=persian"))
                format.timeZone = android.icu.util.TimeZone.getTimeZone(tz.id)
                digits(format.format(Date(ms)),true)

            }
            else -> "$date · $time"
        }
    }

    /** Existing catalogue launch dates are English strings. Do not mistake a COSPAR sequence
     * or "recent deployment" for a calendar date when presenting them in Persian. */
    fun launchDate(raw: String, isFa: Boolean): String? {
        val value = raw.trim().takeIf { it.isNotEmpty() } ?: return null
        if (!isFa) return value
        val datePart = value.substringBefore(" (")
        val parser = SimpleDateFormat("MMMM d, yyyy", Locale.US).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val position = java.text.ParsePosition(0)
        val date = parser.parse(datePart, position) ?: return null
        if (position.index != datePart.length) return null
        val solar = android.icu.text.SimpleDateFormat("EEEE d MMMM y", android.icu.util.ULocale("fa_IR@calendar=persian"))
        solar.timeZone = android.icu.util.TimeZone.getTimeZone("UTC")
        val utc = TimeZone.getTimeZone("UTC")
        val year = Calendar.getInstance(utc).apply { time = date }.get(Calendar.YEAR)
        return digits(solar.format(date), true) + " · " + gregorianFa(date.time, utc) + " " + int(year, true)
    }

    /** Secondary (Gregorian or Solar Hijri, whichever is not primary) date line for group headers. */
    fun secondaryDate(ms: Long, isFa: Boolean, tz: TimeZone): String {
        return if (isFa) gregorianFa(ms,tz) else {
            val sh = TimeEngine.toSolarHijri(ms, tz)
            "${sh.day} ${sh.monthNameEn}"
        }
    }

    fun categoryLabel(category: SatelliteCategory, isFa: Boolean): String =
        if (isFa) category.labelFa else category.labelEn
}

// ------------------------------------------------------------------------------------------------
// Night grouping, spotlight selection and the pass window — pure, testable
// ------------------------------------------------------------------------------------------------

object SatNights {

    /** Calendar fallback for callers without an observer; production feeds use SatObserverNights. */
    fun nightKey(ms: Long, tz: TimeZone): Long {
        val cal = Calendar.getInstance(tz).apply { timeInMillis = ms }
        if (cal.get(Calendar.HOUR_OF_DAY) < 12) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        cal.set(Calendar.HOUR_OF_DAY, 12)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun title(nightKeyMs: Long, nowMs: Long, tz: TimeZone, isFa: Boolean, todayKey: Long = nightKey(nowMs, tz)): String {
        val tomorrow = Calendar.getInstance(tz).apply { timeInMillis = todayKey; add(Calendar.DATE, 1) }.timeInMillis
        return when (nightKeyMs) {
            todayKey -> (if (isFa) "امشب" else "Tonight") + " · " + date(nightKeyMs, tz, isFa)
            tomorrow -> (if (isFa) "شب فردا" else "Tomorrow night") + " · " + date(nightKeyMs, tz, isFa)
            else -> date(nightKeyMs, tz, isFa)
        }
    }

    private fun date(ms: Long, tz: TimeZone, isFa: Boolean): String = if (isFa) {
        val date = TimeEngine.toSolarHijri(ms, tz)
        "${SatFmt.int(date.day, true)} ${date.monthNameFa}"
    } else SimpleDateFormat("EEE d MMM", Locale.US).apply { timeZone = tz }.format(Date(ms))

    /** Groups sorted passes into observer nights, preserving stable (satId, riseTime) keys. */
    fun group(
        passes: List<SatPassUi>,
        nowMs: Long,
        tz: TimeZone,
        isFa: Boolean,
        includeSolarHijri: Boolean = false,
        referenceNight: Long = nightKey(nowMs, tz),
    ): List<SatNightGroup> {
        val byNight = PassGrouping.nights(passes.sortedBy { it.pass.startTimeMs }, SatPassUi::nightKey)
        return byNight.map { (key, items) ->
            val best = items.maxByOrNull { it.pass.visibilityScore }
            SatNightGroup(
                nightKey = key,
                title = title(key, nowMs, tz, isFa, referenceNight),
                subtitle = if (isFa || includeSolarHijri) SatFmt.secondaryDate(key, isFa, tz) else null,
                best = best,
                passes = items,
            )
        }
    }

    /**
     * Spotlight rule: the soonest in-scope pass scoring "Good" or better; if a "Very good" or better
     * pass lands within six hours of it, the brighter one wins.
     */
    fun spotlight(
        passes: List<SatPassUi>,
        nowMs: Long,
        tz: TimeZone,
        isFa: Boolean,
        simulated: Boolean = false,
    ): SatSpotlightUi? {
        val chosen = PassGrouping.spotlight(passes, nowMs, {it.pass.startTimeMs}, {it.pass.endTimeMs}, {it.pass.visibilityScore}) ?: return null
        val running = chosen.pass.startTimeMs <= nowMs
        val remaining = if(running) chosen.pass.endTimeMs-nowMs else chosen.pass.startTimeMs-nowMs
        return SatSpotlightUi(chosen,running,(remaining/60_000L).toInt(),simulated && running)
    }

    /** Indexes the pass that follows a simulated instant, for the ⏭ control. */
    fun nextPassStart(passes: List<SatPassUi>, nowMs: Long): Long? =
        passes.filter { it.pass.startTimeMs > nowMs }.minOfOrNull { it.pass.startTimeMs }

    fun passesNear(passes: List<SatPassUi>, nowMs: Long, windowMs: Long): List<SatPassUi> =
        passes.filter { it.pass.maxTimeMs in (nowMs - windowMs)..(nowMs + windowMs) }
}

// ------------------------------------------------------------------------------------------------
// Shared surface primitives — the only place a card's material is decided
// ------------------------------------------------------------------------------------------------

/** The card material for the active skin: flat in Default, glass in Spatial, rim-lit in Aether. */
@Composable
fun ZigSatCard(
    modifier: Modifier = Modifier,
    tokens: ZigSatTokens,
    accent: Color? = null,
    shape: Shape = SatShapes.card,
    onClick: (() -> Unit)? = null,
    testTag: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tagged = if (testTag != null) modifier.testTag(testTag) else modifier
    Column(tagged.fillMaxWidth().satMaterial(tokens,SatSlot.FocusCard,shape)
        .then(if(onClick==null) Modifier else Modifier.clickable(onClick=onClick)), content=content)
}

/** A true capsule chip, text-only, ≥48 dp hit area with a compact visual body. */
@Composable
fun ZigSatChip(
    label: String,
    selected: Boolean,
    tokens: ZigSatTokens,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    testTag: String? = null,
) {
    val tagged = if (testTag != null) modifier.testTag(testTag) else modifier
    Box(
        modifier = tagged
            .sizeIn(minWidth = SatGrid.minTouchTarget, minHeight = SatGrid.minTouchTarget)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .clip(SatShapes.capsule)
                .background(if (selected) tokens.selectedChip else tokens.cardAlt)
                .border(
                    1.dp,
                    if (selected) tokens.live else tokens.border,
                    SatShapes.capsule,
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            leading?.invoke()
            Text(
                text = label,
                style = RedTypographyTokens.badge.copy(
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                ),
                color = if (selected) Color.White else tokens.inkSecondary,
            )
            trailing?.invoke()
        }
    }
}

/** Visual chrome and the 48 dp target are deliberately independent. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZigSatIconButton(
    icon: ImageVector,
    contentDescription: String,
    tokens: ZigSatTokens,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    testTag: String? = null,
    visualSize: Dp = 40.dp,
    dot: Color? = null,
    enabled: Boolean = true,
    hud: Boolean = false,
    tooltip: String? = null,
    pulseDot: Boolean = false,
) {
    val tagged = if (testTag != null) modifier.testTag(testTag) else modifier
    val content: @Composable () -> Unit = {
        Box(
            modifier = tagged.size(SatGrid.minTouchTarget).clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(visualSize).satMaterial(tokens, SatSlot.Toolbar, SatShapes.capsule,
                backing = if (hud) tokens.hudFill else tokens.cardAlt,
                accent = if (active) tokens.live else null), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription,
                    tint = (if (hud) tokens.hudInk else tokens.inkPrimary).copy(alpha = if (enabled) 1f else 0.4f),
                    modifier = Modifier.size(20.dp))
                if (dot != null) Box(Modifier.align(Alignment.TopEnd).padding(6.dp)) {
                    ZigStatusDot(color = dot, pulse = pulseDot, size = 7.dp)
                }
            }
        }
    }
    if (tooltip == null) content()
    else TooltipBox(positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip(containerColor = tokens.hudFill, contentColor = tokens.hudInk) { SatText(tooltip, tokens.hudInk) } },
        state = rememberTooltipState(), content = content)
}

/** Live/Simulation status dot: pulsing 2.4 s, static under reduce motion. */
@Composable
fun ZigStatusDot(color: Color, pulse: Boolean, size: Dp = 8.dp) {
    val alpha = if (pulse && !LocalSatReduceMotion.current) {
        val transition = rememberInfiniteTransition(label = "SatLivePulse")
        val animated by transition.animateFloat(
            initialValue = 1f,
            targetValue = 0.55f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "SatLivePulseAlpha",
        )
        animated
    } else {
        1f
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(SatShapes.capsule)
            .background(color.copy(alpha = alpha)),
    )
}

/** Micro-caps label: uppercase with tracking in EN, never uppercase in FA. */
@Composable
fun ZigMicroCaps(text: String, tokens: ZigSatTokens, color: Color? = null, isFa: Boolean = false) {
    Text(
        text = text,
        style = RedTypographyTokens.badge.copy(
            fontSize = if (isFa) 11.sp else 10.sp,
            letterSpacing = if (isFa) 0.sp else 0.2.sp,
            fontWeight = FontWeight.Medium,
        ),
        color = color ?: tokens.inkTertiary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Tabular numerals for every live number, so layout never jitters. */
fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")

@Composable
internal fun TextStyle.satOptical(): TextStyle = if (LocalConfiguration.current.locales[0]?.language == "fa")
    copy(lineHeight = ((if (lineHeight.value.isFinite()) lineHeight.value else fontSize.value * 1.25f) + 4f).sp) else this

/** Quality as a tinted capsule: hue in the fill and ring, label always in onSurface ink. */
@Composable
fun ZigQualityChip(quality: SatQuality, score: Int, tokens: ZigSatTokens, isFa: Boolean, full: Boolean = true) {
    val compact = !full || LocalConfiguration.current.screenWidthDp < 360 || LocalDensity.current.fontScale >= 1.3f
    Row(
        modifier = Modifier
            .clip(SatShapes.capsule)
            .background(tokens.cardAlt)
            .border(Dp.Hairline, tokens.border, SatShapes.capsule)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(SatShapes.capsule)
                .background(quality.graphic(tokens)),
        )
        Text(
            text = quality.label(isFa),
            style = RedTypographyTokens.badge.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium).satOptical(),
            color = tokens.inkPrimary,
            maxLines = 1,
        )
    }
}

/** A reminder bell with its five states and a 48 dp hit target. */
@Composable
fun ZigBellButton(
    leadMinutes: Int?,
    isAutoCovered: Boolean,
    isPast: Boolean,
    tokens: ZigSatTokens,
    isFa: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = !isPast
    val active = leadMinutes != null || isAutoCovered
    val wobble=remember{androidx.compose.animation.core.Animatable(0f)}
    var previous by remember{mutableStateOf(leadMinutes)};val reduce=LocalSatReduceMotion.current
    LaunchedEffect(leadMinutes){val changed=previous!=leadMinutes;previous=leadMinutes
        if(changed && !reduce){wobble.animateTo(-12f,tween(70));wobble.animateTo(9f,tween(100));wobble.animateTo(0f,tween(130))}}
    Column(
        modifier = modifier
            .sizeIn(minWidth = SatGrid.minTouchTarget, minHeight = SatGrid.minTouchTarget)

            .clickable(enabled = enabled, role = androidx.compose.ui.semantics.Role.Button, onClick = onClick)
            .testTag("pass_reminder_bell"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (active) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsNone,
            contentDescription = stringResource(R.string.sat_reminder),
            tint = when {
                !enabled -> tokens.inkTertiary.copy(alpha = 0.38f)
                active -> tokens.live
                else -> tokens.inkSecondary
            },
            modifier = Modifier.size(24.dp).rotate(wobble.value),
        )
        val micro = when {
            !enabled -> null
            isAutoCovered && leadMinutes == null -> stringResource(R.string.sat_reminder_auto)
            leadMinutes != null -> if (isFa) satLeadLabel(leadMinutes, true) else if (leadMinutes == 1440) "1d" else "${leadMinutes}m"
            else -> null
        }
        if (micro != null) {
            Text(
                text = micro,
                style = RedTypographyTokens.badge.copy(fontSize = 9.sp, fontWeight = FontWeight.Medium),
                color = if (active) tokens.accentInk else tokens.inkTertiary,
                maxLines = 1,
            )
        }
    }
}

/** Clears the reminder selection list chip's `×` affordance. */
@Composable
fun ZigClearGlyph(tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(SatGrid.minTouchTarget)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Clear,
            contentDescription = "Clear",
            tint = tint,
            modifier = Modifier.size(14.dp),
        )
    }
}

/** Haptic helper that honours reduce-motion: ticks still fire, motion is what quiets down. */
@Composable
fun rememberSatHaptics(): () -> Unit {
    val haptics = LocalHapticFeedback.current
    return {
        runCatching { haptics.performHapticFeedback(HapticFeedbackType.LongPress) }
        Unit
    }
}

/** Width-class decisions from the redesign spec. */
enum class SatWidthClass { COMPACT, MEDIUM, LARGE }

fun satWidthClass(widthDp: Int): SatWidthClass = when {
    widthDp < 360 -> SatWidthClass.COMPACT
    widthDp < 412 -> SatWidthClass.MEDIUM
    else -> SatWidthClass.LARGE
}

/** Grid gap helpers so a skin cannot drift the vertical rhythm. */
val satGroupSpacing: Dp = SatGrid.gapBetweenGroups
val satInGroupSpacing: Dp = SatGrid.gapInGroup

@Composable
fun SatVerticalGap() {
    Spacer(modifier = Modifier.height(satGroupSpacing))
}

/** A trailing spacer that clears the floating navigation bar. */
@Composable
fun SatBottomClearance() {
    Spacer(modifier = Modifier.height(SatGrid.bottomClearance))
}

/** The active tokens for the current skin × theme, provided by the screen root. */
val LocalZigSatTokens = compositionLocalOf { zigSatTokens(SatSkin.DEFAULT, ThemeMode.DARK) }

/** `TRACKED · 8` — micro caps with a tabular count, and the count stays in Latin-shaped tabular digits. */
@Composable
fun ZigMicroCapsWithCount(caps: String, count: Int, tokens: ZigSatTokens, isFa: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ZigMicroCaps(text = caps, tokens = tokens, isFa = isFa)
        Text(
            text = SatFmt.int(count, isFa),
            style = RedTypographyTokens.badge.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold).tabular(),
            color = tokens.inkSecondary,
        )
    }
}

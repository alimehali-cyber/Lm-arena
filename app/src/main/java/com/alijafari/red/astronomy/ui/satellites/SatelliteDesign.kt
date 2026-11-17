package com.alijafari.red.astronomy.ui.satellites

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.alijafari.red.astronomy.ui.theme.squircleShape
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * ZIG · Satellites — "One Stage, Three Skins".
 *
 * Every skin renders the same anatomy (header → globe stage → dock → time strip → scope → fleet rail →
 * spotlight → passes by night). Only surfaces, numerals, shape language and motion change, and they
 * change through [ZigSatTokens] rather than through forked layouts.
 */
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
    val tile: Dp = 20.dp
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
) {
    /** Selected ring colour for the fleet rail token. */
    val selectedRing: Color get() = selected

    fun qualityChipFill(quality: SatQuality): Color =
        quality.hue.copy(alpha = if (isDark) 0.16f else 0.12f)

    fun qualityChipBorder(quality: SatQuality): Color =
        quality.hue.copy(alpha = if (isDark) 0.55f else 0.45f)

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
        else -> Color(0xFFFAF8F5)
    }
    val card = when {
        isWine -> Color(0xFF1B070E)
        isDark -> Color(0xFF12131C)
        else -> Color(0xFFFAF8F5)
    }
    val cardAlt = when {
        isWine -> Color(0xFF260B15)
        isDark -> Color(0xFF1A1B26)
        else -> Color(0xFFFFFDFA)
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
        else -> Color(0xFF6B6B7A)
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
        popoverFill = when {
            isWine -> Color(0xFF1A1228)
            else -> Color(0xFF0F172A)
        },
        popoverBorder = if (isWine) Color(0xFFE04263) else Color(0xFFD32F2F),
        glassy = skin != SatSkin.DEFAULT,
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
    val usingCachedTle: Boolean,
    val sourceLabel: String,
    val isFa: Boolean,
)

@Immutable
data class SatDockData(
    val shortName: String,
    val fullName: String,
    val categoryLabel: String,
    val isSunlit: Boolean,
    val isAboveHorizon: Boolean,
    val altitudeKm: Double,
    val speedKmS: Double,
    val elevationDeg: Double,
    val rangeKm: Double,
    val azimuthDeg: Double,
    val magnitude: Double,
    val subLatDeg: Double,
    val subLonDeg: Double,
    val isSimulated: Boolean,
    val isCached: Boolean,
    val isFa: Boolean,
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
    val isFinished: Boolean get() = pass.endTimeMs <= System.currentTimeMillis()
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
    val onRemind: (Int) -> Unit,
    val onRemoveReminder: () -> Unit,
    val isReminderEnabled: Boolean = true,
)

@Immutable
data class SatTimeActions(
    val onOffsetChange: (Int) -> Unit,
    val onLive: () -> Unit,
    val onNextPass: () -> Unit,
    val onStep: (Int) -> Unit,
)

/** Skin slots for the three places where the skins genuinely diverge in *content*, not just paint. */
class SatSkinSlots(
    val statusChip: @Composable (Modifier, SatStatusData, () -> Unit) -> Unit,
    val dock: @Composable (SatDockData, () -> Unit, () -> Unit, () -> Unit) -> Unit,
    val passCard: @Composable (Modifier, SatPassUi, SatPassActions) -> Unit,
    val spotlight: @Composable (Modifier, SatSpotlightUi, SatPassActions) -> Unit,
)

/** Reduce-motion flag, set once by the host from the platform animator scale. */
val LocalSatReduceMotion = compositionLocalOf { false }

// ------------------------------------------------------------------------------------------------
// Numerals, signs and direction text — the one place that formats a live number
// ------------------------------------------------------------------------------------------------

/** Bidirectional isolate markers, so a signed number never jumps to the wrong side in RTL. */
private const val FSI = "\u2068"
private const val PDI = "\u2069"
private const val MINUS = '\u2212'

object SatFmt {

    fun digits(raw: String, isFa: Boolean): String =
        if (isFa) TimeEngine.formatPersianNumbers(raw) else raw

    /** Formats a signed value with U+2212 for minus, an explicit `+` for positives, bidi-isolated. */
    fun signed(value: Double, decimals: Int, unit: String = "", isFa: Boolean, forceSign: Boolean = true): String {
        val magnitude = kotlin.math.abs(value)
        val body = String.format(Locale.US, "%.${decimals}f", magnitude)
        val sign = when {
            value < -1e-9 -> MINUS.toString()
            forceSign -> "+"
            else -> ""
        }
        val decimal = if (isFa) body.replace('.', '\u066B') else body
        val signed = digits("$sign$decimal$unit", isFa)
        return "$FSI$signed$PDI"
    }

    fun number(value: Double, decimals: Int, isFa: Boolean): String =
        digits(String.format(Locale.US, "%.${decimals}f", value), isFa)

    fun int(value: Int, isFa: Boolean): String = digits(value.toString(), isFa)

    fun intPadded(value: Int, width: Int, isFa: Boolean): String =
        digits(value.toString().padStart(width, '0'), isFa)

    fun kilometers(km: Double, isFa: Boolean, decimals: Int = 0): String {
        val grouped = String.format(Locale.US, "%,.${decimals}f", km)
        return digits(grouped, isFa)
    }

    fun speed(kmS: Double, isFa: Boolean): String =
        digits(String.format(Locale.US, "%.2f", kmS), isFa)

    fun degrees(value: Double, isFa: Boolean, decimals: Int = 0, signed: Boolean = false): String =
        if (signed) {
            signed(value, decimals, "°", isFa)
        } else {
            digits(String.format(Locale.US, "%.${decimals}f", kotlin.math.abs(value)) + "°", isFa)
        }

    fun magnitude(value: Double, isFa: Boolean): String = signed(value, 1, "", isFa)

    fun clock(ms: Long, isFa: Boolean, tz: TimeZone): String =
        digits(SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = tz }.format(Date(ms)), isFa)

    /** `2h 14m` / `۲ ساعت ۱۴ دقیقه`. */
    fun duration(minutes: Int, isFa: Boolean): String {
        val m = minutes.coerceAtLeast(0)
        val h = m / 60
        val mm = m % 60
        return if (isFa) {
            when {
                h > 0 && mm > 0 -> "${digits(h.toString(), true)} ساعت ${digits(mm.toString(), true)} دقیقه"
                h > 0 -> "${digits(h.toString(), true)} ساعت"
                else -> "${digits(mm.toString(), true)} دقیقه"
            }
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

    fun offsetMinutes(minutes: Int, isFa: Boolean): String =
        when {
            minutes == 0 -> if (isFa) "زنده" else "Live"
            kotlin.math.abs(minutes) >= 60 -> signed(minutes / 60.0, 1, "h", isFa)
            else -> signed(minutes.toDouble(), 0, "m", isFa)
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
    fun direction(fromAz: Double, toAz: Double, isFa: Boolean, compact: Boolean = false): String {
        val from = compass(fromAz, isFa)
        val to = compass(toAz, isFa)
        if (isFa) {
            val f = if (compact) from.replace("\u200Cی", "\u200C") else from
            return "$f \u2190 $to"
        }
        return "$from \u2192 $to"
    }

    fun dateTime(ms: Long, isFa: Boolean, tz: TimeZone): String {
        val date = SimpleDateFormat("EEE d MMM", Locale.US).apply { timeZone = tz }.format(Date(ms))
        val time = SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = tz }.format(Date(ms))
        return when {
            isFa -> {
                val sh = TimeEngine.toSolarHijri(ms, tz)
                val faDate = "${sh.day} ${sh.monthNameFa}"
                digits("$faDate · $time", true)
            }
            else -> "$date · $time"
        }
    }

    /** Secondary (Gregorian or Solar Hijri, whichever is not primary) date line for group headers. */
    fun secondaryDate(ms: Long, isFa: Boolean, tz: TimeZone): String {
        val gregorian = SimpleDateFormat("EEE d MMM", Locale.US).apply { timeZone = tz }.format(Date(ms))
        return if (isFa) gregorian else {
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

    /** Observer-night key: a pass before local noon belongs to the previous evening's night. */
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

    fun title(nightKeyMs: Long, nowMs: Long, tz: TimeZone, isFa: Boolean): String {
        val todayKey = nightKey(nowMs, tz)
        val dayMs = 24L * 60L * 60L * 1000L
        return when (nightKeyMs) {
            todayKey -> if (isFa) "امشب" else "Tonight"
            todayKey + dayMs -> if (isFa) "شب فردا" else "Tomorrow night"
            else -> {
                val raw = SimpleDateFormat("EEE d MMM", Locale.US).apply { timeZone = tz }
                    .format(Date(nightKeyMs))
                if (isFa) {
                    val sh = TimeEngine.toSolarHijri(nightKeyMs, tz)
                    "${sh.day} ${sh.monthNameFa}"
                } else raw
            }
        }
    }

    /** Groups sorted passes into observer nights, preserving stable (satId, riseTime) keys. */
    fun group(
        passes: List<SatPassUi>,
        nowMs: Long,
        tz: TimeZone,
        isFa: Boolean,
    ): List<SatNightGroup> {
        val byNight = LinkedHashMap<Long, MutableList<SatPassUi>>()
        for (p in passes.sortedBy { it.pass.startTimeMs }) {
            val key = nightKey(p.pass.startTimeMs, tz)
            byNight.getOrPut(key) { mutableListOf() }.add(p)
        }
        return byNight.map { (key, items) ->
            val best = items.maxByOrNull { it.pass.visibilityScore }
            SatNightGroup(
                nightKey = key,
                title = title(key, nowMs, tz, isFa),
                subtitle = if (isFa) SatFmt.secondaryDate(key, true, tz) else SatFmt.secondaryDate(key, false, tz),
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
    ): SatSpotlightUi? {
        val eligible = passes
            .filter { it.pass.endTimeMs > nowMs }
            .sortedBy { it.pass.startTimeMs }
        if (eligible.isEmpty()) return null

        val inProgress = eligible.firstOrNull { it.pass.startTimeMs <= nowMs && it.pass.endTimeMs > nowMs }
        if (inProgress != null) {
            return SatSpotlightUi(
                pass = inProgress,
                isInProgress = true,
                remainingMinutes = ((inProgress.pass.endTimeMs - nowMs) / 60_000L).toInt().coerceAtLeast(0),
                simulatedInsidePass = true,
            )
        }

        val firstGood = eligible.firstOrNull { it.quality.rank <= SatQuality.GOOD.rank }
            ?: eligible.first()
        val windowEnd = firstGood.pass.startTimeMs + 6L * 60L * 60L * 1000L
        val brighter = eligible
            .filter { it.pass.startTimeMs <= windowEnd && it.quality.rank <= SatQuality.VERY_GOOD.rank }
            .maxByOrNull { it.pass.visibilityScore }
        val chosen = if (brighter != null && brighter.pass.visibilityScore > firstGood.pass.visibilityScore) {
            brighter
        } else {
            firstGood
        }
        return SatSpotlightUi(
            pass = chosen,
            isInProgress = false,
            remainingMinutes = ((chosen.pass.startTimeMs - nowMs) / 60_000L).toInt().coerceAtLeast(0),
            simulatedInsidePass = false,
        )
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
    shape: Shape = squircleShape(SatRadius.card),
    onClick: (() -> Unit)? = null,
    testTag: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tagged = if (testTag != null) modifier.testTag(testTag) else modifier
    val fill = if (accent != null && tokens.skin == SatSkin.DEFAULT) {
        accent.copy(alpha = if (tokens.isDark) 0.10f else 0.08f)
    } else if (tokens.skin == SatSkin.DEFAULT) {
        tokens.cardAlt
    } else {
        tokens.card.copy(alpha = if (tokens.isDark) 0.72f else 0.92f)
    }
    val stroke = when {
        accent != null -> BorderStroke(1.dp, accent.copy(alpha = 0.45f))
        tokens.skin == SatSkin.DEFAULT -> BorderStroke(1.dp, tokens.border)
        else -> BorderStroke(1.dp, tokens.inkPrimary.copy(alpha = 0.16f))
    }
    // Default is flat by contract: zero blur, zero shadow. The shared surface keeps that promise,
    // and hands the ink it resolved to every child (see RedSurface's surfaceInkFor).
    RedSurface(
        modifier = tagged.fillMaxWidth(),
        shape = shape,
        color = fill,
        border = stroke,
        elevation = 0.dp,
        onClick = onClick,
    ) {
        Column(content = content)
    }
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
            .height(SatGrid.minTouchTarget)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .clip(RedShapes.capsule)
                .background(if (selected) tokens.live else tokens.cardAlt)
                .border(
                    1.dp,
                    if (selected) tokens.live else tokens.border,
                    RedShapes.capsule,
                )
                .padding(horizontal = 14.dp, vertical = 8.dp),
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
                maxLines = 1,
            )
            trailing?.invoke()
        }
    }
}

/** A 40 dp visual / 48 dp hit-area circular control in the skin's treatment. */
@Composable
fun ZigSatIconButton(
    icon: ImageVector,
    contentDescription: String,
    tokens: ZigSatTokens,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    testTag: String? = null,
) {
    val tagged = if (testTag != null) modifier.testTag(testTag) else modifier
    Box(
        modifier = tagged.size(SatGrid.minTouchTarget),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (active) tokens.live.copy(alpha = 0.16f) else tokens.cardAlt)
                .border(1.dp, if (active) tokens.live.copy(alpha = 0.55f) else tokens.border, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (active) tokens.live else tokens.inkSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** Live/Simulation status dot: pulsing 2.4 s, static under reduce motion. */
@Composable
fun ZigStatusDot(color: Color, pulse: Boolean, size: Dp = 8.dp) {
    val alpha = if (pulse && !LocalSatReduceMotion.current) {
        val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "SatLivePulse")
        val animated by transition.animateFloat(
            initialValue = 1f,
            targetValue = 0.55f,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                animation = androidx.compose.animation.core.tween(1200),
                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
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
            .clip(CircleShape)
            .background(color.copy(alpha = alpha)),
    )
}

/** Micro-caps label: uppercase with tracking in EN, never uppercase in FA. */
@Composable
fun ZigMicroCaps(text: String, tokens: ZigSatTokens, color: Color? = null, isFa: Boolean = false): Text {
    Text(
        text = if (isFa) text else text.uppercase(Locale.US),
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

/** Quality as a tinted capsule: hue in the fill and ring, label always in onSurface ink. */
@Composable
fun ZigQualityChip(quality: SatQuality, score: Int, tokens: ZigSatTokens, isFa: Boolean, full: Boolean = true) {
    Row(
        modifier = Modifier
            .clip(RedShapes.capsule)
            .background(tokens.qualityChipFill(quality))
            .border(1.dp, tokens.qualityChipBorder(quality), RedShapes.capsule)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(quality.hue),
        )
        Text(
            text = if (full) "${quality.label(isFa)} · ${SatFmt.int(score, isFa)}" else quality.shortLabel(isFa),
            style = RedTypographyTokens.badge.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
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
    Column(
        modifier = modifier
            .sizeIn(minWidth = SatGrid.minTouchTarget, minHeight = SatGrid.minTouchTarget)
            .clickable(enabled = enabled, onClick = onClick)
            .testTag("pass_reminder_bell"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (active) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsNone,
            contentDescription = if (isFa) "یادآوری گذر" else "Pass reminder",
            tint = when {
                !enabled -> tokens.inkTertiary.copy(alpha = 0.38f)
                active -> tokens.live
                else -> tokens.inkSecondary
            },
            modifier = Modifier.size(20.dp),
        )
        val micro = when {
            !enabled -> null
            isAutoCovered && leadMinutes == null -> if (isFa) "خودکار" else "auto"
            leadMinutes != null -> if (leadMinutes == 1440) (if (isFa) "۱ روز" else "1d") else SatFmt.digits("${leadMinutes}m", isFa)
            else -> null
        }
        if (micro != null) {
            Text(
                text = micro,
                style = RedTypographyTokens.badge.copy(fontSize = 9.sp, fontWeight = FontWeight.Medium),
                color = if (active) tokens.live else tokens.inkTertiary,
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

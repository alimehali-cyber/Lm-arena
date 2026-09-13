package com.alijafari.red.astronomy.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.R

/**
 * Vazirmatn — the Persian/Arabic face used whenever the app is in Persian.
 *
 * Bundled as static instances (one file per weight) rather than the variable-axis font, because
 * Compose's [FontFamily] weight mapping is exact and needs no axis support on older API levels.
 * Only the four weights the RED type scale actually asks for are shipped: Normal 400, Medium 500,
 * SemiBold 600, Bold 700. Licence: SIL Open Font License 1.1 — `THIRD_PARTY_LICENSES/Vazirmatn-OFL.txt`
 * (with the upstream author list), and `THIRD_PARTY_LICENSES/README.md` records where the files came from.
 */
val VazirmatnFontFamily = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_semibold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold, FontWeight.Bold)
)

/**
 * The app's long-standing name for "the Persian type face": call sites write
 * `if (isFa) IranSans else null`, so it now resolves to the bundled Vazirmatn. The historical
 * `iran_sans_*` files in res/font are unusable (corrupted bytes, see VazirmatnPersianTypographyTest)
 * and Iran Sans is not freely licensed anyway, so nothing points at them; the alias survives so the
 * existing convention keeps working and stays a single point of change.
 */
val IranSans = VazirmatnFontFamily

/** Likewise still platform default: the `estedad_*` files in res/font are corrupt and loadable by nothing. */
val EstedadFontFamily = FontFamily.Default

/**
 * The single locale → type-face rule of the app. Persian gets Vazirmatn; English — and every Latin
 * run inside a Persian string, since the switch is per-locale and not per-script — keeps the
 * platform default face, exactly as before Vazirmatn was bundled.
 */
fun redFontFamily(isPersian: Boolean): FontFamily =
    if (isPersian) VazirmatnFontFamily else FontFamily.Default

/**
 * The face [REDTheme] resolved for the current locale. Text drawn outside the Material text
 * pipeline (Canvas labels measured with a `TextMeasurer`) has no ambient text style to inherit from,
 * so those call sites read this instead.
 */
val LocalAppFontFamily = staticCompositionLocalOf<FontFamily> { FontFamily.Default }

/**
 * RED Design System - Restrained Apple-Inspired Typography Hierarchy
 * Balanced for English LTR and Persian RTL scripts with appropriate line-heights and weights.
 *
 * The Material 3 constructor below is fully qualified on purpose: this file also declares a
 * top-level `Typography` property, and an unqualified `Typography(...)` call in the same package
 * would resolve to that property instead of the constructor.
 */
private fun redTypography(family: FontFamily): Typography = androidx.compose.material3.Typography(
    // Large Display (Hero astronomical stats or prominent headers)
    displayLarge = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.3).sp
    ),
    displaySmall = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp
    ),

    // Screen Titles
    headlineLarge = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),

    // Section Titles & Cards
    titleLarge = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),

    // Body Text
    bodyLarge = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),

    // Interactive Labels & Buttons
    labelLarge = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp
    ),
    labelSmall = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    )
)

/** The scale as it renders for English (and for previews): the platform face. */
val Typography = redTypography(FontFamily.Default)

/**
 * The Material 3 scale for the active locale. One call, in [REDTheme], re-faces the whole app — no
 * per-Text patching anywhere. English returns the identical instance it always used.
 */
fun redTypographyFor(isPersian: Boolean): Typography =
    if (isPersian) redTypography(VazirmatnFontFamily) else Typography

/**
 * Specialized Typography tokens for astronomical / numerical / data display, and the RED
 * design-token faces of the same scale.
 *
 * The bases in [RedTypeBase] never carry a `fontFamily`: the object's getters put the locale face
 * on them at read time. That matters because Material 3's `Text` uses whatever `style` it is given
 * as-is (it does not merge the passed style onto the theme's ambient style), so a token that
 * hardcoded `FontFamily.Default` would silently opt every one of these ~120 call sites out of
 * Vazirmatn in Persian. Sizes, weights, line heights and tracking are exactly what they were.
 */
internal object RedTypeBase {
    // Hero Display
    val heroDisplay = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.4).sp
    )

    // Section and Card Headings
    val sectionHeading = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.1).sp
    )

    // Standard Body Texts
    val bodyPrimary = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    )

    val bodySecondary = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    )

    // High-precision astronomical numerical values (coordinates, time, magnitudes)
    val numberLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.2).sp
    )

    val numberMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    )

    val numberSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    )

    // Screen and section headers
    val screenTitle = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp
    )

    val sectionTitle = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    )

    val caption = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    )

    val badge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.2.sp
    )
}

object RedTypographyTokens {
    /** heroDisplay — rendered with the locale face (see [redLocalized]). */
    val heroDisplay: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = redLocalized(RedTypeBase.heroDisplay)

    /** sectionHeading — rendered with the locale face (see [redLocalized]). */
    val sectionHeading: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = redLocalized(RedTypeBase.sectionHeading)

    /** bodyPrimary — rendered with the locale face (see [redLocalized]). */
    val bodyPrimary: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = redLocalized(RedTypeBase.bodyPrimary)

    /** bodySecondary — rendered with the locale face (see [redLocalized]). */
    val bodySecondary: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = redLocalized(RedTypeBase.bodySecondary)

    /** numberLarge — rendered with the locale face (see [redLocalized]). */
    val numberLarge: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = redLocalized(RedTypeBase.numberLarge)

    /** numberMedium — rendered with the locale face (see [redLocalized]). */
    val numberMedium: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = redLocalized(RedTypeBase.numberMedium)

    /** numberSmall — rendered with the locale face (see [redLocalized]). */
    val numberSmall: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = redLocalized(RedTypeBase.numberSmall)

    /** screenTitle — rendered with the locale face (see [redLocalized]). */
    val screenTitle: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = redLocalized(RedTypeBase.screenTitle)

    /** sectionTitle — rendered with the locale face (see [redLocalized]). */
    val sectionTitle: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = redLocalized(RedTypeBase.sectionTitle)

    /** caption — rendered with the locale face (see [redLocalized]). */
    val caption: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = redLocalized(RedTypeBase.caption)

    /** badge — rendered with the locale face (see [redLocalized]). */
    val badge: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = redLocalized(RedTypeBase.badge)
}

/**
 * Puts the face the theme resolved for the active locale onto a family-free style. Read-only:
 * it never triggers a recomposition by itself, it just follows the ambient [LocalAppFontFamily].
 */
@Composable
@ReadOnlyComposable
fun redLocalized(style: TextStyle): TextStyle = style.copy(fontFamily = LocalAppFontFamily.current)

/**
 * Accessor for typography styles compatible with both Material 3 and custom RED styles. These
 * forward the locale face too, so code reading RedTypography.bodyMedium matches MaterialTheme.
 */
object RedTypography {
    val displayLarge: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = redLocalized(Typography.displayLarge)
    val displayMedium: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = redLocalized(Typography.displayMedium)
    val displaySmall: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = redLocalized(Typography.displaySmall)
    val headlineLarge: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = redLocalized(Typography.headlineLarge)
    val headlineMedium: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = redLocalized(Typography.headlineMedium)
    val headlineSmall: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = redLocalized(Typography.headlineSmall)
    val titleLarge: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = redLocalized(Typography.titleLarge)
    val titleMedium: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = redLocalized(Typography.titleMedium)
    val titleSmall: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = redLocalized(Typography.titleSmall)
    val bodyLarge: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = redLocalized(Typography.bodyLarge)
    val bodyMedium: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = redLocalized(Typography.bodyMedium)
    val bodySmall: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = redLocalized(Typography.bodySmall)
    val labelLarge: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = redLocalized(Typography.labelLarge)
    val labelMedium: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = redLocalized(Typography.labelMedium)
    val labelSmall: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = redLocalized(Typography.labelSmall)
}

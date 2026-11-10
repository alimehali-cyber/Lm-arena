package com.alijafari.red.astronomy.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alijafari.red.astronomy.domain.AppUiStyle

/**
 * ============================================================================
 * THE IMMERSIVE CHROME
 * ============================================================================
 *
 * The app has two screens that are not pages but *places*: the AR sky and the Gravity Sandbox. Both
 * open full-screen, hide the floating navigation bar, and float their controls over a live scene that
 * runs underneath them (a camera feed, a simulated sky, a tabletop). Neither can wear a normal screen
 * layout — there is no room for a list, and there must be no opaque page background — so both used to
 * grow their own chrome: fixed dark navy panels, white text, hard-coded radii and Material chip
 * defaults. That is exactly why they read as a different app from the rest of ZIG.
 *
 * This file is their shared answer. It is not a second design system: every value here is *derived
 * from* [RedTheme]'s tokens, which are themselves already adapted by [AppUiStyle]'s Cyber and Spatial
 * providers. So the chrome follows the selected style and the selected theme automatically —
 * [AppUiStyle.DEFAULT]'s vermilion, [AppUiStyle.CYBER]'s cut-glass accents, [AppUiStyle.SPATIAL]'s
 * desert neon, dark, light or wine — with no per-style branching at the call sites.
 *
 * ## What makes chrome "the app's chrome"
 *
 *  * **One accent.** [ChromeTokens.accent] is `RedTheme.colors.accentRed`. Nothing in an immersive
 *    screen may invent a second brand colour; status colours are the theme's own success / warning /
 *    error tokens, never literals.
 *  * **One corner geometry.** Panels take [RedCornerRadius.window] through [squircleShape]; rows and
 *    chips take a nested radius; pills are capsules. The same three steps the rest of the app uses.
 *  * **One fill and one hairline.** [ChromeTokens.fill] is the theme's elevated surface at the alpha a
 *    floating panel needs over a scene; [ChromeTokens.stroke] is the theme's border token.
 *  * **One ink.** Text is the theme's primary / secondary ink, resolved against the panel it is drawn
 *    on (a [RedSurface] re-provides the ink it painted, and every composable here reads it).
 *  * **One type scale.** Labels are [RedTypographyTokens.badge] / `caption`; titles are
 *    `sectionTitle` / `sectionHeading`. No ad-hoc sp values.
 *  * **One motion language.** Presses spring to 0.96, selections cross-fade in 180 ms, and the panels
 *    themselves are animated by their owner with the app's `FastOutSlowInEasing` curves.
 *
 * ## Why it is a set of components, not just tokens
 *
 * A token can be ignored; a component cannot be reimplemented by accident. AR and the sandbox now call
 * [ChromeCapsule], [ChromeIconDisc], [ChromeToggleChip], [ChromePrimaryAction], [ChromeTextAction],
 * [ChromeBanner] and [ChromePanel] instead of Material's `FilterChip`, `Button`, `TextButton`,
 * `Surface` and hand-rolled boxes, so the two screens agree with each other *and* with the pages
 * around them, and a future palette change lands in both at once.
 */

/**
 * The interface design language in force, as a composition local.
 *
 * Provided once by [REDTheme]'s owner (MainActivity) so that a screen which is not one of the three
 * style-native screens — the AR sky, the sandbox, a modal — can still know which language the user
 * picked, for the few decisions that are geometry rather than colour (see [ChromeTokens]).
 */
val LocalAppUiStyle = staticCompositionLocalOf { AppUiStyle.DEFAULT }

/** The four meanings a piece of immersive chrome can carry. */
enum class ChromeTone { NEUTRAL, ACCENT, SUCCESS, WARNING, DANGER }

/**
 * Every colour, radius and weight the immersive chrome is allowed to draw with.
 *
 * Built from [RedTheme] on every read (see [chromeTokens]); nothing here is a literal, so a themed
 * rebuild cannot leave a stale panel behind.
 */
@Immutable
data class ChromeTokens(
    val uiStyle: AppUiStyle,
    val isDark: Boolean,
    /** The app's single brand accent — `RedTheme.colors.accentRed`. */
    val accent: Color,
    /** The accent washed onto a panel, for a selected row or an active pill. */
    val accentSoft: Color,
    /** The ink that reads on [accent] when it is used as a fill. */
    val onAccent: Color,
    /** The theme's secondary celestial accent, for the one label that is neither body nor brand. */
    val gold: Color,
    /** The fill of a floating panel: the theme's elevated surface, as opaque as a scene allows. */
    val fill: Color,
    /** The same fill at full strength, for a panel that must win against a bright scene. */
    val fillStrong: Color,
    /** A quieter fill, for a nested row inside a panel. */
    val fillQuiet: Color,
    /** The theme's hairline border. */
    val stroke: Color,
    /** A firmer hairline, for the edge of a primary control. */
    val strokeStrong: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    /** Panels and sheets: the app's window radius. */
    val panelRadius: Dp,
    /** Rows, chips and nested blocks. */
    val rowRadius: Dp,
    /** The soft glow a capsule may carry in the two glass styles; transparent in DEFAULT. */
    val glow: Color
) {
    val panelShape: Shape get() = squircleShape(panelRadius)
    val rowShape: Shape get() = squircleShape(rowRadius)
    val pillShape: Shape get() = CircleShape

    /** The tint for a tone, and the ink that reads on it when it is used as a fill. */
    fun tone(tone: ChromeTone): Color = when (tone) {
        ChromeTone.NEUTRAL -> textSecondary
        ChromeTone.ACCENT -> accent
        ChromeTone.SUCCESS -> success
        ChromeTone.WARNING -> warning
        ChromeTone.DANGER -> danger
    }

    /** A panel wash for a tone: the same hue the icon and the border use, at panel strength. */
    fun toneWash(tone: ChromeTone): Color = when (tone) {
        ChromeTone.NEUTRAL -> fill
        else -> tone(tone).copy(alpha = if (isDark) 0.16f else 0.10f)
    }
}

/**
 * The chrome of the screen being composed, derived from the theme in force at this exact point.
 *
 * Reading `RedTheme.colors` here (rather than capturing it once at the root) is deliberate: a
 * [RedSurface] hands its children the ink it actually painted, so a chip inside a panel resolves its
 * ink against the panel, and a chip over the camera resolves it against the scene. That is the same
 * contrast repair the rest of the app relies on, applied to floating chrome.
 */
@Composable
@ReadOnlyComposable
fun chromeTokens(): ChromeTokens {
    val colors = RedTheme.colors
    val style = LocalAppUiStyle.current
    val isDark = colors.isDark
    return ChromeTokens(
        uiStyle = style,
        isDark = isDark,
        accent = colors.accentRed,
        accentSoft = colors.accentRedSubtle,
        onAccent = readableInkOn(colors.accentRed),
        gold = colors.accentGold,
        fill = colors.surfaceElevated.copy(alpha = if (isDark) 0.92f else 0.95f),
        fillStrong = colors.surfaceElevated,
        fillQuiet = colors.surfaceGrouped.copy(alpha = if (isDark) 0.80f else 0.88f),
        stroke = colors.border,
        strokeStrong = colors.separatorOpaque.copy(alpha = 0.55f),
        textPrimary = colors.textPrimary,
        textSecondary = colors.textSecondary,
        textTertiary = colors.textTertiary,
        success = colors.statusSuccess,
        warning = colors.statusWarning,
        danger = colors.statusError,
        // The three styles already differ in how tight their containers are: DEFAULT and SPATIAL wear
        // the app's 40 dp window corner, CYBER the tighter cut-glass 36 dp. Taking the value from the
        // style keeps a sandbox sheet visually in the same family as the page that launched it.
        panelRadius = when (style) {
            AppUiStyle.CYBER -> 36.dp
            else -> RedCornerRadius.window
        },
        rowRadius = when (style) {
            AppUiStyle.CYBER, AppUiStyle.AETHER -> 18.dp
            AppUiStyle.SPATIAL -> 20.dp
            AppUiStyle.DEFAULT -> 18.dp
        },
        glow = when (style) {
            AppUiStyle.DEFAULT -> Color.Transparent
            else -> colors.accentRed.copy(alpha = 0.22f)
        }
    )
}

/** The press feedback every chrome control shares: a short, damped spring to 0.96. */
@Composable
private fun pressScale(interactionSource: MutableInteractionSource, enabled: Boolean): Float {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 460f),
        label = "ChromePressScale"
    )
    return scale
}

/** A capsule that carries the app's glass: panel fill, hairline, optional accent glow, press spring. */
@Composable
private fun ChromeCapsuleContainer(
    modifier: Modifier,
    selected: Boolean,
    glow: Boolean,
    enabled: Boolean,
    interactionSource: MutableInteractionSource,
    link: String,
    content: @Composable () -> Unit
) {
    val chrome = chromeTokens()
    val scale = pressScale(interactionSource, enabled)
    val container by animateColorAsState(
        targetValue = when {
            selected -> chrome.accent
            glow -> chrome.accent.copy(alpha = if (chrome.isDark) 0.18f else 0.12f)
            else -> chrome.fill
        },
        animationSpec = tween(durationMillis = 180),
        label = "ChromeCapsuleFill$link"
    )
    val strokeColor by animateColorAsState(
        targetValue = when {
            selected -> Color.Transparent
            glow -> chrome.accent.copy(alpha = 0.55f)
            else -> chrome.stroke
        },
        animationSpec = tween(durationMillis = 180),
        label = "ChromeCapsuleStroke$link"
    )
    Box(
        modifier = modifier
            .scale(scale)
            .then(
                if (chrome.glow.alpha > 0.01f && selected) Modifier.shadow(10.dp, CircleShape) else Modifier
            )
            .background(container, CircleShape)
            .border(BorderStroke(1.dp, strokeColor), CircleShape)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * The AR sky's smart pill and the sandbox's scene / predict chip: an icon and a word in a capsule.
 *
 * [selected] fills the capsule with the accent (an open panel, an active mode). [highlighted] leaves
 * it on the panel fill and puts the accent in the border and the glyph instead, which is how a *state
 * change* is shown without shouting — the difference between "this panel is open" and "something has
 * been changed away from its default".
 */
@Composable
fun ChromeCapsule(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    selected: Boolean = false,
    highlighted: Boolean = false,
    enabled: Boolean = true,
    testTag: String? = null
) {
    val chrome = chromeTokens()
    val interactionSource = remember { MutableInteractionSource() }
    val centered = TextAlign.Center
    val ink by animateColorAsState(
        targetValue = when {
            selected -> chrome.onAccent
            highlighted -> chrome.accent
            else -> chrome.textPrimary
        },
        animationSpec = tween(durationMillis = 180),
        label = "ChromeCapsuleInk"
    )
    ChromeCapsuleContainer(
        modifier = modifier
            .defaultMinSize(minHeight = 34.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        selected = selected,
        glow = highlighted,
        enabled = enabled,
        interactionSource = interactionSource,
        link = label
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ink,
                    modifier = Modifier.size(RedIconSize.sm)
                )
            }
            Text(
                text = label,
                style = RedTypographyTokens.badge.copy(fontWeight = FontWeight.SemiBold),
                color = ink,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                textAlign = centered
            )
        }
    }
}

/**
 * A wordless round control — the AR header's night-vision and align buttons, the sandbox HUD's
 * play / reset / trails / teaching / palette / camera.
 *
 * The disc is the app's icon-button step: [RedControlHeight.compact] square, an accent wash when
 * active, and the theme's secondary ink when not, so a row of them reads as one instrument.
 */
@Composable
fun ChromeIconDisc(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    enabled: Boolean = true,
    size: Dp = 38.dp,
    iconSize: Dp = RedIconSize.sm,
    /** Overrides the derived ink, for a disc that sits inside a banner or a status card. */
    iconTint: Color? = null,
    testTag: String? = null
) {
    val chrome = chromeTokens()
    val interactionSource = remember { MutableInteractionSource() }
    val scale = pressScale(interactionSource, enabled)
    val wash by animateColorAsState(
        targetValue = if (active) chrome.accent.copy(alpha = 0.16f) else Color.Transparent,
        animationSpec = tween(durationMillis = 180),
        label = "ChromeDiscWash"
    )
    val ink by animateColorAsState(
        targetValue = iconTint ?: when {
            !enabled -> chrome.textTertiary
            active -> chrome.accent
            else -> chrome.textSecondary
        },
        animationSpec = tween(durationMillis = 180),
        label = "ChromeDiscInk"
    )
    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(wash, CircleShape)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = ink,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * A filter chip: the sky-object filters, the alignment reference targets, the sensor switches.
 *
 * Material's `FilterChip` was the last Material component left in the AR panel; it brought its own
 * 8 dp corners, its own 32 dp height and its own `secondaryContainer` fill into a screen whose every
 * other pixel is a squircle capsule from the theme. This is the same control in the app's language:
 * a capsule, the accent when selected, a hairline when not, and a spring when touched.
 */
@Composable
fun ChromeToggleChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    testTag: String? = null
) {
    val chrome = chromeTokens()
    val interactionSource = remember { MutableInteractionSource() }
    val scale = pressScale(interactionSource, enabled)
    val container by animateColorAsState(
        targetValue = if (selected) chrome.accent else chrome.fillQuiet,
        animationSpec = tween(durationMillis = 180),
        label = "ChromeChipFill"
    )
    val strokeColor by animateColorAsState(
        targetValue = if (selected) Color.Transparent else chrome.stroke,
        animationSpec = tween(durationMillis = 180),
        label = "ChromeChipStroke"
    )
    val ink by animateColorAsState(
        targetValue = when {
            !enabled -> chrome.textTertiary
            selected -> chrome.onAccent
            else -> chrome.textPrimary
        },
        animationSpec = tween(durationMillis = 180),
        label = "ChromeChipInk"
    )
    Row(
        modifier = modifier
            .scale(scale)
            .defaultMinSize(minHeight = 34.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .background(container, CircleShape)
            .border(BorderStroke(1.dp, strokeColor), CircleShape)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ink,
                modifier = Modifier.size(RedIconSize.xs)
            )
        }
        Text(
            text = label,
            style = RedTypographyTokens.badge.copy(fontWeight = FontWeight.Medium),
            color = ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * A quiet text action inside a panel — "Show all", "Hide all", "Telemetry", "Clear target".
 *
 * Deliberately not a Material `TextButton`: no 64 dp minimum width, no elevation ripple, no all-caps
 * letter spacing. Just the accent (or the secondary ink) on a pressed-and-released capsule.
 */
@Composable
fun ChromeTextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    enabled: Boolean = true,
    testTag: String? = null
) {
    val chrome = chromeTokens()
    val interactionSource = remember { MutableInteractionSource() }
    val scale = pressScale(interactionSource, enabled)
    val ink = if (tint == Color.Unspecified) chrome.accent else tint
    val wash by animateColorAsState(
        targetValue = if (enabled) ink.copy(alpha = 0.10f) else Color.Transparent,
        animationSpec = tween(durationMillis = 160),
        label = "ChromeTextActionWash"
    )
    Box(
        modifier = modifier
            .scale(scale)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .background(wash, CircleShape)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = RedTypographyTokens.badge.copy(fontWeight = FontWeight.SemiBold),
            color = if (enabled) ink else chrome.textTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * The one filled action of an immersive screen — "Align", "View details", "Fit all".
 *
 * Accent fill, the ink that reads on it, a 44 dp touch target, and the same press spring as
 * everything else. Two of these never sit side by side: a screen gets one primary action.
 */
@Composable
fun ChromePrimaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    /** A confirmation is the one primary action that is not the brand accent — it is the outcome. */
    tone: ChromeTone = ChromeTone.ACCENT,
    enabled: Boolean = true,
    testTag: String? = null
) {
    val chrome = chromeTokens()
    val interactionSource = remember { MutableInteractionSource() }
    val scale = pressScale(interactionSource, enabled)
    val fillBase = chrome.tone(tone)
    val ink = if (tone == ChromeTone.ACCENT) chrome.onAccent else readableInkOn(fillBase)
    val container by animateColorAsState(
        targetValue = if (enabled) fillBase else fillBase.copy(alpha = 0.35f),
        animationSpec = tween(durationMillis = 180),
        label = "ChromePrimaryFill"
    )
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .scale(scale)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .background(container, CircleShape)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ink,
                modifier = Modifier.size(RedIconSize.sm)
            )
        }
        Text(
            text = text,
            style = RedTypographyTokens.badge.copy(fontWeight = FontWeight.Bold),
            color = ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * A floating panel: the fill, the hairline, the squircle corner and the ink resolution of a
 * [RedSurface], with the immersive chrome's defaults.
 *
 * Every sheet-like block on the two screens is this composable, so an AR panel and a sandbox sheet
 * cannot drift apart in fill alpha, border weight or corner radius.
 */
@Composable
fun ChromePanel(
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    color: Color = Color.Unspecified,
    border: BorderStroke? = null,
    elevation: Dp = RedElevation.floating,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val chrome = chromeTokens()
    val panelShape = shape ?: chrome.panelShape
    RedSurface(
        modifier = modifier,
        shape = panelShape,
        color = if (color == Color.Unspecified) chrome.fill else color,
        border = border ?: BorderStroke(1.dp, chrome.stroke),
        elevation = elevation,
        onClick = onClick,
        content = content
    )
}

/**
 * A status banner over the scene: permission lost, magnetic interference, recalibration, a target
 * found, an alignment applied or refused.
 *
 * All six of those used to be their own composable with their own navy fill and their own literal
 * amber or green — six panels that could not agree on a corner radius. They are one banner with a
 * [tone] now, and the tone's colour is a theme token.
 */
@Composable
fun ChromeBanner(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tone: ChromeTone = ChromeTone.NEUTRAL,
    title: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    testTag: String? = null
) {
    val chrome = chromeTokens()
    val tint = chrome.tone(tone)
    RedSurface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        shape = chrome.rowShape,
        color = chrome.toneWash(tone),
        border = BorderStroke(1.dp, tint.copy(alpha = if (tone == ChromeTone.NEUTRAL) 0.35f else 0.55f)),
        elevation = RedElevation.popover
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(RedIconSize.md)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (title != null) {
                    Text(
                        text = title,
                        style = RedTypographyTokens.caption.copy(fontWeight = FontWeight.Bold),
                        color = chrome.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = text,
                    style = RedTypographyTokens.caption,
                    color = if (title == null) chrome.textPrimary else chrome.textSecondary,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (actionLabel != null && onAction != null) {
                ChromeTextAction(text = actionLabel, onClick = onAction, tint = tint)
            }
            if (onDismiss != null) {
                ChromeIconDisc(
                    icon = Icons.Filled.Close,
                    contentDescription = "Dismiss",
                    onClick = onDismiss,
                    size = 30.dp,
                    iconSize = RedIconSize.xs
                )
            }
        }
    }
}

/** The hairline between two blocks of a panel — the theme's separator, never a literal white. */
@Composable
fun ChromeDivider(modifier: Modifier = Modifier) {
    val chrome = chromeTokens()
    HorizontalDivider(
        modifier = modifier,
        thickness = 1.dp,
        color = chrome.stroke
    )
}

/** A panel's own title: section scale, primary ink, no icon, no rule. */
@Composable
fun ChromeSectionTitle(text: String, modifier: Modifier = Modifier) {
    val chrome = chromeTokens()
    Text(
        text = text,
        modifier = modifier,
        style = RedTypographyTokens.sectionTitle,
        color = chrome.textPrimary,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

/** The quiet second line under a title — a caption, not a body paragraph. */
@Composable
fun ChromeCaption(text: String, modifier: Modifier = Modifier) {
    val chrome = chromeTokens()
    Text(
        text = text,
        modifier = modifier,
        style = RedTypographyTokens.caption,
        color = chrome.textSecondary
    )
}

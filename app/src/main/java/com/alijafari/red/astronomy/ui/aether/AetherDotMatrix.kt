package com.alijafari.red.astronomy.ui.aether

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.ui.theme.LocalAppFontFamily
import com.alijafari.red.astronomy.ui.theme.RedTypographyTokens

// ============================================================================
// AETHER DOT-MATRIX TELEMETRY DISPLAY & ROLLING COUNTERS
// (Rubik Dots / Nothing NDot-47 Inspired Circular LED Matrix Glyphs)
// ============================================================================

/**
 * Renders crisp circular LED dot-matrix telemetry readouts (`04`, `37`, `16.2`, `76`, `-18.4°`,
 * `22:41`, Persian `۰-۹`, `٪`, `٫`) strictly for live Aether telemetry numbers and badges.
 */
@Composable
fun AetherDotMatrixText(
    text: String,
    modifier: Modifier = Modifier,
    dotSize: Dp = 3.2.dp,
    dotGap: Dp = 1.15.dp,
    charGap: Dp = 3.6.dp,
    showGhostMatrix: Boolean = false,
    activeColor: Color = AetherTheme.palette.glowWhite,
    glowColor: Color? = AetherTheme.palette.primaryAccent,
    ghostColor: Color = AetherTheme.palette.dotMatrixGhost
) {
    val glyphs = remember(text) { text.map { aetherGlyphFor(it) } }
    if (glyphs.isEmpty()) return

    val totalCols = remember(glyphs) { glyphs.sumOf { it.cols } }
    val stepDp = dotSize + dotGap
    val totalWidth = (stepDp * totalCols) - dotGap + (charGap - dotGap) * (glyphs.size - 1).coerceAtLeast(0)
    val totalHeight = (stepDp * 7) - dotGap

    Canvas(
        modifier = modifier
            .width(totalWidth.coerceAtLeast(4.dp))
            .height(totalHeight)
    ) {
        val dPx = dotSize.toPx().coerceAtLeast(1.4f)
        val radius = dPx / 2f
        val step = dPx + dotGap.toPx()
        val extraCharAdvance = charGap.toPx()

        var cursorX = 0f
        for (glyph in glyphs) {
            for (row in 0 until 7) {
                val rowBits = glyph.rows[row]
                val cy = row * step + radius
                for (col in 0 until glyph.cols) {
                    val mask = 1 shl (glyph.cols - 1 - col)
                    val isLit = (rowBits and mask) != 0
                    val cx = cursorX + col * step + radius
                    if (isLit) {
                        if (glowColor != null) {
                            drawCircle(
                                color = glowColor.copy(alpha = 0.28f),
                                radius = radius * 2.1f,
                                center = Offset(cx, cy)
                            )
                        }
                        drawCircle(
                            color = activeColor,
                            radius = radius,
                            center = Offset(cx, cy)
                        )
                    } else if (showGhostMatrix && !glyph.isPunctuation) {
                        drawCircle(
                            color = ghostColor,
                            radius = radius * 0.52f,
                            center = Offset(cx, cy)
                        )
                    }
                }
            }
            cursorX += (glyph.cols * step - dotGap.toPx()) + extraCharAdvance
        }
    }
}

/**
 * Rolling Dot-Matrix Counter (Section 4.4 & Section 5):
 * Uses [AnimatedContent] with `slideInVertically { it } + fadeIn()` paired with
 * `slideOutVertically { -it } + fadeOut()` whenever a live telemetry readout increments or updates.
 */
@Composable
fun AetherRollingDotMatrixCounter(
    text: String,
    modifier: Modifier = Modifier,
    dotSize: Dp = 4.2.dp,
    dotGap: Dp = 1.35.dp,
    charGap: Dp = 4.4.dp,
    showGhostMatrix: Boolean = false,
    activeColor: Color = AetherTheme.palette.glowWhite,
    glowColor: Color? = AetherTheme.palette.primaryAccent
) {
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            (slideInVertically { it } + fadeIn()).togetherWith(
                slideOutVertically { -it } + fadeOut()
            )
        },
        label = "aetherRollingDotMatrix",
        modifier = modifier
    ) { currentText ->
        AetherDotMatrixText(
            text = currentText,
            dotSize = dotSize,
            dotGap = dotGap,
            charGap = charGap,
            showGhostMatrix = showGhostMatrix,
            activeColor = activeColor,
            glowColor = glowColor
        )
    }
}

/**
 * Rolling Secondary Geometric Sans-Serif Metric (`420 mg` / `-18.4°` / `384,400 km` style):
 * `30.sp`–`31.sp`, `FontWeight.Light`, tight negative letter spacing `(-0.8).sp` in Latin
 * (`0.sp` in Persian so joined glyphs remain intact), animating vertically on change.
 */
@Composable
fun AetherRollingMetricText(
    text: String,
    isPersian: Boolean,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 30.sp,
    color: Color = AetherTheme.palette.textPrimary
) {
    val appFont = LocalAppFontFamily.current
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            (slideInVertically { it / 2 } + fadeIn()).togetherWith(
                slideOutVertically { -it / 2 } + fadeOut()
            )
        },
        label = "aetherRollingSecondaryMetric",
        modifier = modifier
    ) { value ->
        Text(
            text = value,
            style = RedTypographyTokens.numberLarge.copy(
                fontFamily = appFont,
                fontSize = fontSize,
                lineHeight = (fontSize.value * 1.15f).sp,
                fontWeight = FontWeight.Light,
                letterSpacing = if (isPersian) 0.sp else (-0.8).sp
            ),
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private class AetherGlyph(
    val cols: Int,
    val rows: IntArray,
    val isPunctuation: Boolean = false
)

private fun g5(r0: Int, r1: Int, r2: Int, r3: Int, r4: Int, r5: Int, r6: Int) =
    AetherGlyph(5, intArrayOf(r0, r1, r2, r3, r4, r5, r6))

private fun g3(r0: Int, r1: Int, r2: Int, r3: Int, r4: Int, r5: Int, r6: Int) =
    AetherGlyph(3, intArrayOf(r0, r1, r2, r3, r4, r5, r6), isPunctuation = true)

private fun aetherGlyphFor(ch: Char): AetherGlyph = when (ch) {
    '0' -> g5(0x0E, 0x11, 0x13, 0x15, 0x19, 0x11, 0x0E)
    '1' -> g5(0x04, 0x0C, 0x04, 0x04, 0x04, 0x04, 0x0E)
    '2' -> g5(0x0E, 0x11, 0x01, 0x06, 0x08, 0x10, 0x1F)
    '3' -> g5(0x1F, 0x02, 0x04, 0x06, 0x01, 0x11, 0x0E)
    '4' -> g5(0x02, 0x06, 0x0A, 0x12, 0x1F, 0x02, 0x02)
    '5' -> g5(0x1F, 0x10, 0x1E, 0x01, 0x01, 0x11, 0x0E)
    '6' -> g5(0x06, 0x08, 0x10, 0x1E, 0x11, 0x11, 0x0E)
    '7' -> g5(0x1F, 0x01, 0x02, 0x04, 0x08, 0x08, 0x08)
    '8' -> g5(0x0E, 0x11, 0x11, 0x0E, 0x11, 0x11, 0x0E)
    '9' -> g5(0x0E, 0x11, 0x11, 0x0F, 0x01, 0x02, 0x0C)

    '۰', '٠' -> g5(0x00, 0x04, 0x0A, 0x11, 0x0A, 0x04, 0x00)
    '۱', '١' -> g5(0x04, 0x0C, 0x04, 0x04, 0x04, 0x04, 0x04)
    '۲', '٢' -> g5(0x11, 0x11, 0x1F, 0x10, 0x10, 0x10, 0x10)
    '۳', '٣' -> g5(0x15, 0x15, 0x1F, 0x10, 0x10, 0x10, 0x10)
    '۴', '٤' -> g5(0x13, 0x15, 0x1F, 0x10, 0x10, 0x10, 0x10)
    '۵', '٥' -> g5(0x04, 0x0A, 0x11, 0x11, 0x15, 0x15, 0x0A)
    '۶', '٦' -> g5(0x06, 0x09, 0x07, 0x04, 0x08, 0x10, 0x10)
    '۷', '٧' -> g5(0x11, 0x11, 0x0A, 0x0A, 0x04, 0x04, 0x04)
    '۸', '٨' -> g5(0x04, 0x04, 0x0A, 0x0A, 0x11, 0x11, 0x11)
    '۹', '٩' -> g5(0x0E, 0x11, 0x11, 0x0F, 0x01, 0x01, 0x01)

    ':' -> g3(0x00, 0x02, 0x02, 0x00, 0x02, 0x02, 0x00)
    '.', '٫' -> g3(0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0x02)
    ',' -> g3(0x00, 0x00, 0x00, 0x00, 0x02, 0x02, 0x04)
    '-', '−', '–' -> g5(0x00, 0x00, 0x00, 0x1F, 0x00, 0x00, 0x00)
    '+' -> g5(0x00, 0x04, 0x04, 0x1F, 0x04, 0x04, 0x00)
    '/', '÷' -> g5(0x01, 0x02, 0x02, 0x04, 0x08, 0x08, 0x10)
    '%', '٪' -> g5(0x19, 0x19, 0x02, 0x04, 0x08, 0x13, 0x13)
    '°' -> g3(0x07, 0x05, 0x07, 0x00, 0x00, 0x00, 0x00)
    ' ' -> g3(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)

    'A', 'a' -> g5(0x0E, 0x11, 0x11, 0x1F, 0x11, 0x11, 0x11)
    'B', 'b' -> g5(0x1E, 0x11, 0x11, 0x1E, 0x11, 0x11, 0x1E)
    'C', 'c' -> g5(0x0E, 0x11, 0x10, 0x10, 0x10, 0x11, 0x0E)
    'D', 'd' -> g5(0x1C, 0x12, 0x11, 0x11, 0x11, 0x12, 0x1C)
    'E', 'e' -> g5(0x1F, 0x10, 0x10, 0x1E, 0x10, 0x10, 0x1F)
    'F', 'f' -> g5(0x1F, 0x10, 0x10, 0x1E, 0x10, 0x10, 0x10)
    'G', 'g' -> g5(0x0E, 0x11, 0x10, 0x17, 0x11, 0x11, 0x0F)
    'H', 'h' -> g5(0x11, 0x11, 0x11, 0x1F, 0x11, 0x11, 0x11)
    'I', 'i' -> g3(0x07, 0x02, 0x02, 0x02, 0x02, 0x02, 0x07)
    'J', 'j' -> g5(0x07, 0x02, 0x02, 0x02, 0x02, 0x12, 0x0C)
    'K', 'k' -> g5(0x11, 0x12, 0x14, 0x18, 0x14, 0x12, 0x11)
    'L', 'l' -> g5(0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x1F)
    'M', 'm' -> g5(0x11, 0x1B, 0x15, 0x15, 0x11, 0x11, 0x11)
    'N', 'n' -> g5(0x11, 0x19, 0x15, 0x13, 0x11, 0x11, 0x11)
    'O', 'o' -> g5(0x0E, 0x11, 0x11, 0x11, 0x11, 0x11, 0x0E)
    'P', 'p' -> g5(0x1E, 0x11, 0x11, 0x1E, 0x10, 0x10, 0x10)
    'Q', 'q' -> g5(0x0E, 0x11, 0x11, 0x11, 0x15, 0x12, 0x0D)
    'R', 'r' -> g5(0x1E, 0x11, 0x11, 0x1E, 0x14, 0x12, 0x11)
    'S', 's' -> g5(0x0F, 0x10, 0x10, 0x0E, 0x01, 0x01, 0x1E)
    'T', 't' -> g5(0x1F, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04)
    'U', 'u' -> g5(0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x0E)
    'V', 'v' -> g5(0x11, 0x11, 0x11, 0x11, 0x11, 0x0A, 0x04)
    'W', 'w' -> g5(0x11, 0x11, 0x11, 0x15, 0x15, 0x1B, 0x11)
    'X', 'x' -> g5(0x11, 0x11, 0x0A, 0x04, 0x0A, 0x11, 0x11)
    'Y', 'y' -> g5(0x11, 0x11, 0x0A, 0x04, 0x04, 0x04, 0x04)
    'Z', 'z' -> g5(0x1F, 0x01, 0x02, 0x04, 0x08, 0x10, 0x1F)
    else -> g3(0x00, 0x02, 0x05, 0x02, 0x00, 0x00, 0x00)
}

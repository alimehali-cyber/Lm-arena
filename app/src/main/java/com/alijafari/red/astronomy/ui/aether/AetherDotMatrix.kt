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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ============================================================================
// AETHER TELEMETRY DOT-MATRIX DISPLAY SYSTEM (CYBER GLASS 5x7 CIRCULAR DOT STYLE)
// ============================================================================

/**
 * Cyber Glass-style 5x7 circular dot-matrix typography renderer tailored for
 * Aether Telemetry UI's primary live readouts (`04`, `37`, `16.2`, `76`).
 *
 * Uses the same hand-authored 5x7 bitmap glyphs, dot spacing, and glow/ghost treatment as
 * Cyber Glass. Active ink and glow remain sourced from Aether's own theme palette. It supports
 * Latin (`0-9`, `A-Z`) and Persian (`۰-۹`, `٪`, `٫`) telemetry without a system font.
 */
@Composable
fun AetherDotMatrixText(
    text: String,
    modifier: Modifier = Modifier,
    dotSize: Dp = 2.6.dp,
    dotGap: Dp = 1.05.dp,
    charGap: Dp = 3.2.dp,
    color: Color = AetherTheme.palette.glowWhite,
    glowColor: Color? = AetherTheme.palette.crimsonGlow,
    showGhostGrid: Boolean = false,
    ghostColor: Color = AetherTheme.palette.dotMatrixGhost
) {
    val glyphs = remember(text) { text.map { aetherGlyphFor(it) } }
    if (glyphs.isEmpty()) return

    val totalCols = remember(glyphs) { glyphs.sumOf { it.cols } }
    val stepDp = dotSize + dotGap
    val totalWidth = (stepDp * totalCols) - dotGap + (charGap - dotGap) * (glyphs.size - 1).coerceAtLeast(0)
    val totalHeight = (stepDp * AETHER_GLYPH_ROWS) - dotGap

    Canvas(
        modifier = modifier
            .width(totalWidth.coerceAtLeast(4.dp))
            .height(totalHeight)
    ) {
        val dPx = dotSize.toPx().coerceAtLeast(1.4f)
        val radius = dPx / 2f
        val step = dPx + dotGap.toPx()
        val extraAdvance = charGap.toPx()

        var cursorX = 0f
        for (glyph in glyphs) {
            for (row in 0 until AETHER_GLYPH_ROWS) {
                val rowBits = glyph.rows[row]
                val cy = row * step + radius
                for (col in 0 until glyph.cols) {
                    val mask = 1 shl (glyph.cols - 1 - col)
                    val isLit = (rowBits and mask) != 0
                    val cx = cursorX + col * step + radius
                    if (isLit) {
                        if (glowColor != null) {
                            drawCircle(
                                color = glowColor.copy(alpha = 0.24f),
                                radius = radius * 2.0f,
                                center = Offset(cx, cy)
                            )
                        }
                        drawCircle(
                            color = color,
                            radius = radius,
                            center = Offset(cx, cy)
                        )
                    } else if (showGhostGrid && !glyph.isNarrow) {
                        drawCircle(
                            color = ghostColor,
                            radius = radius * 0.58f,
                            center = Offset(cx, cy)
                        )
                    }
                }
            }
            cursorX += (glyph.cols * step - dotGap.toPx()) + extraAdvance
        }
    }
}

/**
 * Rolling Dot-Matrix Counter using `AnimatedContent` with vertical slide + fade
 * (`slideInVertically { it } + fadeIn()` paired with `slideOutVertically { -it } + fadeOut()`)
 * whenever telemetry values increment or decrement via micro-steppers or live updates.
 */
@Composable
fun AetherRollingDotMatrixCounter(
    valueText: String,
    modifier: Modifier = Modifier,
    dotSize: Dp = 2.8.dp,
    dotGap: Dp = 1.0.dp,
    charGap: Dp = 3.0.dp,
    color: Color = AetherTheme.palette.glowWhite,
    glowColor: Color? = AetherTheme.palette.crimsonGlow,
    showGhostGrid: Boolean = false
) {
    AnimatedContent(
        targetState = valueText,
        transitionSpec = {
            if (targetState >= initialState) {
                (slideInVertically { it } + fadeIn()).togetherWith(
                    slideOutVertically { -it } + fadeOut()
                )
            } else {
                (slideInVertically { -it } + fadeIn()).togetherWith(
                    slideOutVertically { it } + fadeOut()
                )
            }
        },
        label = "AetherRollingDotMatrix",
        modifier = modifier
    ) { animatedText ->
        AetherDotMatrixText(
            text = animatedText,
            dotSize = dotSize,
            dotGap = dotGap,
            charGap = charGap,
            color = color,
            glowColor = glowColor,
            showGhostGrid = showGhostGrid
        )
    }
}

private const val AETHER_GLYPH_ROWS = 7

private class AetherGlyph(
    val cols: Int,
    val rows: IntArray,
    val isNarrow: Boolean = false
)

private fun glyph5(r0: Int, r1: Int, r2: Int, r3: Int, r4: Int, r5: Int, r6: Int) =
    AetherGlyph(5, intArrayOf(r0, r1, r2, r3, r4, r5, r6))

private fun glyph3(r0: Int, r1: Int, r2: Int, r3: Int, r4: Int, r5: Int, r6: Int) =
    AetherGlyph(3, intArrayOf(r0, r1, r2, r3, r4, r5, r6), isNarrow = true)

private fun aetherGlyphFor(ch: Char): AetherGlyph = when (ch) {
    '0' -> glyph5(0x0E, 0x11, 0x13, 0x15, 0x19, 0x11, 0x0E)
    '1' -> glyph5(0x04, 0x0C, 0x04, 0x04, 0x04, 0x04, 0x0E)
    '2' -> glyph5(0x0E, 0x11, 0x01, 0x06, 0x08, 0x10, 0x1F)
    '3' -> glyph5(0x1F, 0x02, 0x04, 0x06, 0x01, 0x11, 0x0E)
    '4' -> glyph5(0x02, 0x06, 0x0A, 0x12, 0x1F, 0x02, 0x02)
    '5' -> glyph5(0x1F, 0x10, 0x1E, 0x01, 0x01, 0x11, 0x0E)
    '6' -> glyph5(0x06, 0x08, 0x10, 0x1E, 0x11, 0x11, 0x0E)
    '7' -> glyph5(0x1F, 0x01, 0x02, 0x04, 0x08, 0x08, 0x08)
    '8' -> glyph5(0x0E, 0x11, 0x11, 0x0E, 0x11, 0x11, 0x0E)
    '9' -> glyph5(0x0E, 0x11, 0x11, 0x0F, 0x01, 0x02, 0x0C)

    '۰', '٠' -> glyph5(0x00, 0x04, 0x0A, 0x11, 0x0A, 0x04, 0x00)
    '۱', '١' -> glyph5(0x04, 0x0C, 0x04, 0x04, 0x04, 0x04, 0x04)
    '۲', '٢' -> glyph5(0x11, 0x11, 0x1F, 0x10, 0x10, 0x10, 0x10)
    '۳', '٣' -> glyph5(0x15, 0x15, 0x1F, 0x10, 0x10, 0x10, 0x10)
    '۴', '٤' -> glyph5(0x07, 0x14, 0x1F, 0x10, 0x10, 0x10, 0x10)
    '۵', '٥' -> glyph5(0x04, 0x0A, 0x11, 0x11, 0x15, 0x15, 0x0A)
    '۶', '٦' -> glyph5(0x06, 0x08, 0x07, 0x04, 0x08, 0x10, 0x10)
    '۷', '٧' -> glyph5(0x11, 0x11, 0x0A, 0x0A, 0x04, 0x04, 0x04)
    '۸', '٨' -> glyph5(0x04, 0x04, 0x0A, 0x0A, 0x11, 0x11, 0x11)
    '۹', '٩' -> glyph5(0x0E, 0x11, 0x11, 0x0F, 0x01, 0x01, 0x01)

    ':' -> glyph3(0x00, 0x02, 0x02, 0x00, 0x02, 0x02, 0x00)
    '.', '٫' -> glyph3(0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0x02)
    ',' -> glyph3(0x00, 0x00, 0x00, 0x00, 0x02, 0x02, 0x04)
    '-', '−', '–' -> glyph5(0x00, 0x00, 0x00, 0x1F, 0x00, 0x00, 0x00)
    '+' -> glyph5(0x00, 0x04, 0x04, 0x1F, 0x04, 0x04, 0x00)
    '/', '÷' -> glyph5(0x01, 0x02, 0x02, 0x04, 0x08, 0x08, 0x10)
    '%', '٪' -> glyph5(0x19, 0x19, 0x02, 0x04, 0x08, 0x13, 0x13)
    '°' -> glyph3(0x07, 0x05, 0x07, 0x00, 0x00, 0x00, 0x00)
    '\'' -> glyph3(0x02, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00)
    ' ' -> glyph3(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)

    'A', 'a' -> glyph5(0x0E, 0x11, 0x11, 0x1F, 0x11, 0x11, 0x11)
    'B', 'b' -> glyph5(0x1E, 0x11, 0x11, 0x1E, 0x11, 0x11, 0x1E)
    'C', 'c' -> glyph5(0x0E, 0x11, 0x10, 0x10, 0x10, 0x11, 0x0E)
    'D', 'd' -> glyph5(0x1C, 0x12, 0x11, 0x11, 0x11, 0x12, 0x1C)
    'E', 'e' -> glyph5(0x1F, 0x10, 0x10, 0x1E, 0x10, 0x10, 0x1F)
    'F', 'f' -> glyph5(0x1F, 0x10, 0x10, 0x1E, 0x10, 0x10, 0x10)
    'G', 'g' -> glyph5(0x0E, 0x11, 0x10, 0x17, 0x11, 0x11, 0x0F)
    'H', 'h' -> glyph5(0x11, 0x11, 0x11, 0x1F, 0x11, 0x11, 0x11)
    'I', 'i' -> glyph3(0x07, 0x02, 0x02, 0x02, 0x02, 0x02, 0x07)
    'J', 'j' -> glyph5(0x07, 0x02, 0x02, 0x02, 0x02, 0x12, 0x0C)
    'K', 'k' -> glyph5(0x11, 0x12, 0x14, 0x18, 0x14, 0x12, 0x11)
    'L', 'l' -> glyph5(0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x1F)
    'M', 'm' -> glyph5(0x11, 0x1B, 0x15, 0x15, 0x11, 0x11, 0x11)
    'N', 'n' -> glyph5(0x11, 0x19, 0x15, 0x13, 0x11, 0x11, 0x11)
    'O', 'o' -> glyph5(0x0E, 0x11, 0x11, 0x11, 0x11, 0x11, 0x0E)
    'P', 'p' -> glyph5(0x1E, 0x11, 0x11, 0x1E, 0x10, 0x10, 0x10)
    'Q', 'q' -> glyph5(0x0E, 0x11, 0x11, 0x11, 0x15, 0x12, 0x0D)
    'R', 'r' -> glyph5(0x1E, 0x11, 0x11, 0x1E, 0x14, 0x12, 0x11)
    'S', 's' -> glyph5(0x0F, 0x10, 0x10, 0x0E, 0x01, 0x01, 0x1E)
    'T', 't' -> glyph5(0x1F, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04)
    'U', 'u' -> glyph5(0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x0E)
    'V', 'v' -> glyph5(0x11, 0x11, 0x11, 0x11, 0x11, 0x0A, 0x04)
    'W', 'w' -> glyph5(0x11, 0x11, 0x11, 0x15, 0x15, 0x1B, 0x11)
    'X', 'x' -> glyph5(0x11, 0x11, 0x0A, 0x04, 0x0A, 0x11, 0x11)
    'Y', 'y' -> glyph5(0x11, 0x11, 0x0A, 0x04, 0x04, 0x04, 0x04)
    'Z', 'z' -> glyph5(0x1F, 0x01, 0x02, 0x04, 0x08, 0x10, 0x1F)
    else -> glyph3(0x00, 0x02, 0x05, 0x02, 0x00, 0x00, 0x00)
}

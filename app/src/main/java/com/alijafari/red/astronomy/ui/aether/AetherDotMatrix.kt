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
// AETHER TELEMETRY DOT-MATRIX DISPLAY SYSTEM (RUBIK DOTS / NDOT HIGH-DENSITY)
// ============================================================================

/**
 * High-density 7x9 circular LED Dot-Matrix typography renderer tailored for
 * Aether Telemetry UI's primary 2-to-3 digit live readouts (`04`, `37`, `16.2`, `76`).
 *
 * Uses crisp circular LED dots (`GlowWhite` `#FFFFFF`) with optional soft halo bloom
 * and subtle ghost matrix points, supporting both Latin (`0-9`, `A-Z`, `.`, `%`, `°`)
 * and Persian (`۰-۹`, `٪`, `٫`) telemetry readouts without pinning a system font family.
 */
@Composable
fun AetherDotMatrixText(
    text: String,
    modifier: Modifier = Modifier,
    dotSize: Dp = 3.1.dp,
    dotGap: Dp = 1.15.dp,
    charGap: Dp = 4.2.dp,
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
        val dPx = dotSize.toPx().coerceAtLeast(1.3f)
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
                                color = glowColor.copy(alpha = 0.22f),
                                radius = radius * 1.95f,
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
                            radius = radius * 0.48f,
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
    dotSize: Dp = 3.2.dp,
    dotGap: Dp = 1.2.dp,
    charGap: Dp = 4.4.dp,
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

private const val AETHER_GLYPH_ROWS = 9

private class AetherGlyph(
    val cols: Int,
    val rows: IntArray,
    val isNarrow: Boolean = false
)

// 6-column x 9-row fine Rubik-Dots / NDot style telemetry glyph (bits 5..0)
private fun g6(
    r0: Int, r1: Int, r2: Int, r3: Int, r4: Int, r5: Int, r6: Int, r7: Int, r8: Int
) = AetherGlyph(6, intArrayOf(r0, r1, r2, r3, r4, r5, r6, r7, r8))

// 3-column x 9-row narrow punctuation glyph (bits 2..0)
private fun g3(
    r0: Int, r1: Int, r2: Int, r3: Int, r4: Int, r5: Int, r6: Int, r7: Int, r8: Int
) = AetherGlyph(3, intArrayOf(r0, r1, r2, r3, r4, r5, r6, r7, r8), isNarrow = true)

private fun aetherGlyphFor(ch: Char): AetherGlyph = when (ch) {
    // High-resolution Rubik-Dots inspired digits (matching the crisp circular-dot "04" readout)
    '0' -> g6(0x1E, 0x21, 0x23, 0x25, 0x29, 0x31, 0x21, 0x21, 0x1E)
    '1' -> g6(0x04, 0x0C, 0x14, 0x04, 0x04, 0x04, 0x04, 0x04, 0x1F)
    '2' -> g6(0x1E, 0x21, 0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x3F)
    '3' -> g6(0x1E, 0x21, 0x01, 0x02, 0x0C, 0x02, 0x01, 0x21, 0x1E)
    '4' -> g6(0x02, 0x06, 0x0A, 0x12, 0x22, 0x3F, 0x02, 0x02, 0x02)
    '5' -> g6(0x3F, 0x20, 0x20, 0x3E, 0x01, 0x01, 0x01, 0x21, 0x1E)
    '6' -> g6(0x0E, 0x10, 0x20, 0x3E, 0x21, 0x21, 0x21, 0x21, 0x1E)
    '7' -> g6(0x3F, 0x01, 0x02, 0x04, 0x08, 0x08, 0x10, 0x10, 0x10)
    '8' -> g6(0x1E, 0x21, 0x21, 0x1E, 0x21, 0x21, 0x21, 0x21, 0x1E)
    '9' -> g6(0x1E, 0x21, 0x21, 0x21, 0x1F, 0x01, 0x02, 0x04, 0x18)

    // Persian / Eastern Arabic numerals (۰-۹)
    '۰', '٠' -> g6(0x00, 0x00, 0x0C, 0x12, 0x21, 0x12, 0x0C, 0x00, 0x00)
    '۱', '١' -> g6(0x08, 0x18, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08)
    '۲', '٢' -> g6(0x21, 0x21, 0x22, 0x3C, 0x20, 0x20, 0x20, 0x20, 0x20)
    '۳', '٣' -> g6(0x25, 0x25, 0x25, 0x3E, 0x20, 0x20, 0x20, 0x20, 0x20)
    '۴', '٤' -> g6(0x26, 0x29, 0x26, 0x3E, 0x20, 0x20, 0x20, 0x20, 0x20)
    '۵', '٥' -> g6(0x0C, 0x12, 0x21, 0x21, 0x21, 0x29, 0x29, 0x16, 0x00)
    '۶', '٦' -> g6(0x0E, 0x11, 0x11, 0x0E, 0x08, 0x10, 0x20, 0x20, 0x20)
    '۷', '٧' -> g6(0x21, 0x21, 0x12, 0x12, 0x0C, 0x0C, 0x04, 0x04, 0x04)
    '۸', '٨' -> g6(0x04, 0x04, 0x0C, 0x0C, 0x12, 0x12, 0x21, 0x21, 0x21)
    '۹', '٩' -> g6(0x1E, 0x21, 0x21, 0x1F, 0x01, 0x01, 0x01, 0x01, 0x01)

    // Punctuation & telemetry symbols
    '.', '٫' -> g3(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x06, 0x06)
    ':' -> g3(0x00, 0x00, 0x06, 0x06, 0x00, 0x06, 0x06, 0x00, 0x00)
    '-', '−', '–' -> g6(0x00, 0x00, 0x00, 0x00, 0x3F, 0x00, 0x00, 0x00, 0x00)
    '+' -> g6(0x00, 0x00, 0x08, 0x08, 0x3E, 0x08, 0x08, 0x00, 0x00)
    '/', '÷' -> g6(0x01, 0x02, 0x02, 0x04, 0x08, 0x10, 0x10, 0x20, 0x20)
    '%', '٪' -> g6(0x31, 0x32, 0x04, 0x08, 0x08, 0x10, 0x26, 0x26, 0x00)
    '°' -> g3(0x07, 0x05, 0x07, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
    ' ' -> g3(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)

    // Uppercase/lowercase telemetry letters
    'A', 'a' -> g6(0x0C, 0x12, 0x21, 0x21, 0x3F, 0x21, 0x21, 0x21, 0x21)
    'B', 'b' -> g6(0x3E, 0x21, 0x21, 0x3E, 0x21, 0x21, 0x21, 0x21, 0x3E)
    'C', 'c' -> g6(0x1E, 0x21, 0x20, 0x20, 0x20, 0x20, 0x20, 0x21, 0x1E)
    'D', 'd' -> g6(0x3C, 0x22, 0x21, 0x21, 0x21, 0x21, 0x21, 0x22, 0x3C)
    'E', 'e' -> g6(0x3F, 0x20, 0x20, 0x3E, 0x20, 0x20, 0x20, 0x20, 0x3F)
    'F', 'f' -> g6(0x3F, 0x20, 0x20, 0x3E, 0x20, 0x20, 0x20, 0x20, 0x20)
    'G', 'g' -> g6(0x1E, 0x21, 0x20, 0x20, 0x27, 0x21, 0x21, 0x21, 0x1F)
    'H', 'h' -> g6(0x21, 0x21, 0x21, 0x21, 0x3F, 0x21, 0x21, 0x21, 0x21)
    'I', 'i' -> g3(0x07, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x07)
    'J', 'j' -> g6(0x0F, 0x02, 0x02, 0x02, 0x02, 0x02, 0x22, 0x22, 0x1C)
    'K', 'k' -> g6(0x21, 0x22, 0x24, 0x28, 0x30, 0x28, 0x24, 0x22, 0x21)
    'L', 'l' -> g6(0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x3F)
    'M', 'm' -> g6(0x21, 0x33, 0x2D, 0x21, 0x21, 0x21, 0x21, 0x21, 0x21)
    'N', 'n' -> g6(0x21, 0x31, 0x29, 0x25, 0x23, 0x21, 0x21, 0x21, 0x21)
    'O', 'o' -> g6(0x1E, 0x21, 0x21, 0x21, 0x21, 0x21, 0x21, 0x21, 0x1E)
    'P', 'p' -> g6(0x3E, 0x21, 0x21, 0x21, 0x3E, 0x20, 0x20, 0x20, 0x20)
    'Q', 'q' -> g6(0x1E, 0x21, 0x21, 0x21, 0x21, 0x25, 0x22, 0x1D, 0x01)
    'R', 'r' -> g6(0x3E, 0x21, 0x21, 0x21, 0x3E, 0x28, 0x24, 0x22, 0x21)
    'S', 's' -> g6(0x1F, 0x20, 0x20, 0x1E, 0x01, 0x01, 0x01, 0x21, 0x1E)
    'T', 't' -> g6(0x3F, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08)
    'U', 'u' -> g6(0x21, 0x21, 0x21, 0x21, 0x21, 0x21, 0x21, 0x21, 0x1E)
    'V', 'v' -> g6(0x21, 0x21, 0x21, 0x21, 0x21, 0x12, 0x12, 0x0C, 0x0C)
    'W', 'w' -> g6(0x21, 0x21, 0x21, 0x21, 0x21, 0x21, 0x2D, 0x33, 0x21)
    'X', 'x' -> g6(0x21, 0x21, 0x12, 0x0C, 0x0C, 0x12, 0x21, 0x21, 0x21)
    'Y', 'y' -> g6(0x21, 0x21, 0x12, 0x0C, 0x08, 0x08, 0x08, 0x08, 0x08)
    'Z', 'z' -> g6(0x3F, 0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x20, 0x3F)
    else -> g3(0x00, 0x00, 0x02, 0x07, 0x02, 0x00, 0x00, 0x00, 0x00)
}

package com.alijafari.red.astronomy.ui.obsidian

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asFrameworkPaint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ============================================================================
// PROCEDURAL 5x7 LED DOT-MATRIX ENGINE (NO FAKE MONOSPACE FONTS)
//
// Every telemetry readout in the Obsidian language (`04`, `16.2`, `2.8`, `76`)
// is rendered here as true glowing circular micro-dots with optical bloom.
// There is no font involved: the glyph tables below are the typeface.
// ============================================================================

private val DOT_MATRIX_GLYPHS: Map<Char, List<String>> = mapOf(
    '0' to listOf("01110", "10001", "10001", "10001", "10001", "10001", "01110"),
    '1' to listOf("00100", "01100", "00100", "00100", "00100", "00100", "01110"),
    '2' to listOf("01110", "10001", "00001", "00110", "01000", "10000", "11111"),
    '3' to listOf("11110", "00001", "00001", "01110", "00001", "00001", "11110"),
    '4' to listOf("00010", "00110", "01010", "10010", "11111", "00010", "00010"),
    '5' to listOf("11111", "10000", "11110", "00001", "00001", "10001", "01110"),
    '6' to listOf("01110", "10000", "11110", "10001", "10001", "10001", "01110"),
    '7' to listOf("11111", "00001", "00010", "00100", "01000", "01000", "01000"),
    '8' to listOf("01110", "10001", "10001", "01110", "10001", "10001", "01110"),
    '9' to listOf("01110", "10001", "10001", "01111", "00001", "00010", "01100"),
    '.' to listOf("00", "00", "00", "00", "00", "11", "11"),
    '-' to listOf("00000", "00000", "00000", "11111", "00000", "00000", "00000"),
    '+' to listOf("00000", "00100", "00100", "11111", "00100", "00100", "00000"),
    '%' to listOf("11001", "11010", "00100", "00100", "01000", "01011", "10011"),
    '/' to listOf("00001", "00010", "00010", "00100", "01000", "01000", "10000"),
    ' ' to listOf("000", "000", "000", "000", "000", "000", "000")
)

/** Maps Persian/Arabic-Indic digits onto the Latin glyph table so FA locales get the same LEDs. */
private fun normalizeDotMatrixChar(char: Char): Char = when (char) {
    '۰', '٠' -> '0'
    '۱', '١' -> '1'
    '۲', '٢' -> '2'
    '۳', '٣' -> '3'
    '۴', '٤' -> '4'
    '۵', '٥' -> '5'
    '۶', '٦' -> '6'
    '۷', '٧' -> '7'
    '۸', '٨' -> '8'
    '۹', '٩' -> '9'
    '٫', '،' -> '.'
    else -> char
}

/**
 * Renders [value] as a procedural 5x7 LED dot-matrix readout.
 *
 * @param dotSize diameter of one lit micro-dot.
 * @param dotSpacing gap between adjacent micro-dots.
 * @param charSpacing gap between glyphs.
 * @param glow adds the optical bloom pass (`BlurMaskFilter` at 2.4x dot radius).
 * @param ghostMatrix when true, unlit matrix cells are drawn as faint ghost dots —
 *        the Nothing-style "the grid is the screen" signature.
 */
@Composable
fun DotMatrixNumber(
    value: String,
    modifier: Modifier = Modifier,
    dotSize: Dp = 3.2.dp,
    dotSpacing: Dp = 1.8.dp,
    charSpacing: Dp = 6.dp,
    color: Color = Color.White,
    glow: Boolean = true,
    ghostMatrix: Boolean = false,
    ghostColor: Color = Color.White.copy(alpha = 0.08f)
) {
    val glyphs = remember(value) {
        value.map { normalizeDotMatrixChar(it) }.mapNotNull { DOT_MATRIX_GLYPHS[it] }
    }
    val rows = 7
    if (glyphs.isEmpty()) return

    val totalCols = remember(glyphs) { glyphs.sumOf { it.first().length } }
    val gapCount = (totalCols - glyphs.size).coerceAtLeast(0)

    val width = (dotSize * totalCols) +
        (dotSpacing * gapCount) +
        (charSpacing * (glyphs.size - 1).coerceAtLeast(0))
    val height = (dotSize * rows) + (dotSpacing * (rows - 1))

    Canvas(
        modifier = modifier
            .size(width = width, height = height)
            .testTag("dot_matrix_number")
    ) {
        val r = dotSize.toPx() / 2f
        val step = dotSize.toPx() + dotSpacing.toPx()
        val charGap = charSpacing.toPx()

        val bloomPaint = if (glow) {
            Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                this.color = android.graphics.Color.WHITE
                alpha = 95
                maskFilter = BlurMaskFilter(r * 2.4f, BlurMaskFilter.Blur.NORMAL)
            }
        } else {
            null
        }

        var cursorX = 0f
        glyphs.forEach { grid ->
            val cols = grid.first().length
            for (row in 0 until rows) {
                for (col in 0 until cols) {
                    val lit = grid[row][col] == '1'
                    val cx = cursorX + col * step + r
                    val cy = row * step + r
                    when {
                        lit -> {
                            if (bloomPaint != null) {
                                drawIntoCanvas { canvas ->
                                    canvas.nativeCanvas.drawCircle(cx, cy, r * 1.3f, bloomPaint)
                                }
                            }
                            drawCircle(color = color, radius = r, center = Offset(cx, cy))
                        }
                        ghostMatrix -> drawCircle(
                            color = ghostColor,
                            radius = r * 0.62f,
                            center = Offset(cx, cy)
                        )
                    }
                }
            }
            cursorX += (cols * step) - dotSpacing.toPx() + charGap
        }
    }
}

/** Convenience wrapper that zero-pads an integer readout (`4` -> `04`). */
@Composable
fun DotMatrixCounter(
    value: Int,
    modifier: Modifier = Modifier,
    padTo: Int = 2,
    dotSize: Dp = 3.5.dp,
    dotSpacing: Dp = 2.0.dp,
    charSpacing: Dp = 7.dp,
    color: Color = Color.White,
    glow: Boolean = true,
    ghostMatrix: Boolean = false
) {
    DotMatrixNumber(
        value = value.coerceAtLeast(0).toString().padStart(padTo, '0'),
        modifier = modifier,
        dotSize = dotSize,
        dotSpacing = dotSpacing,
        charSpacing = charSpacing,
        color = color,
        glow = glow,
        ghostMatrix = ghostMatrix
    )
}

/**
 * Hairline dot-ring gauge — a precision 1.1.dp circular track of micro-dots whose lit
 * segment advances with [progress]. Used for the Obsidian lab metric rings.
 */
@Composable
fun ObsidianDotRingGauge(
    progress: Float,
    modifier: Modifier = Modifier,
    dotCount: Int = 44,
    dotRadius: Dp = 1.4.dp,
    activeColor: Color = ObsidianCrimson,
    inactiveColor: Color = Color.White.copy(alpha = 0.22f)
) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = minOf(size.width, size.height) / 2f - dotRadius.toPx()
        val lit = (progress.coerceIn(0f, 1f) * dotCount).toInt()
        for (i in 0 until dotCount) {
            val angle = (i.toFloat() / dotCount) * (2f * Math.PI.toFloat()) - (Math.PI / 2f).toFloat()
            val x = center.x + radius * kotlin.math.cos(angle)
            val y = center.y + radius * kotlin.math.sin(angle)
            drawCircle(
                color = if (i < lit) activeColor else inactiveColor,
                radius = dotRadius.toPx() * if (i < lit) 1f else 0.72f,
                center = Offset(x, y)
            )
        }
    }
}

package com.alijafari.red.astronomy.ui.quartz

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ============================================================================
// PROCEDURAL 5x7 LED DOT-MATRIX ENGINE — NO FONT FILES, NO MONOSPACE FALLBACK
//
// Every primary telemetry readout in this design language (drinks today, flasks/day, molecular
// hydrogen, water, orbital altitude) is drawn as true glowing circular micro-dots from this table.
// There is no `fontFamily` anywhere in it: the glyphs *are* the geometry, so the readout looks the
// same on every device, in every locale, and cannot silently degrade to a system monospace face.
// Latin digits, ten Persian digits, a dot, the per-mille and degree marks, and the four box-drawing
// marks the calibration crosshairs use.
// ============================================================================

private val DOT_MATRIX_GLYPHS: Map<Char, List<String>> = mapOf(
    '0' to listOf("01110", "10001", "10011", "10101", "11001", "10001", "01110"),
    '1' to listOf("00100", "01100", "00100", "00100", "00100", "00100", "01110"),
    '2' to listOf("01110", "10001", "00001", "00010", "00100", "01000", "11111"),
    '3' to listOf("11110", "00001", "00001", "01110", "00001", "00001", "11110"),
    '4' to listOf("00010", "00110", "01010", "10010", "11111", "00010", "00010"),
    '5' to listOf("11111", "10000", "11110", "00001", "00001", "10001", "01110"),
    '6' to listOf("00110", "01000", "10000", "11110", "10001", "10001", "01110"),
    '7' to listOf("11111", "00001", "00010", "00100", "01000", "01000", "01000"),
    '8' to listOf("01110", "10001", "10001", "01110", "10001", "10001", "01110"),
    '9' to listOf("01110", "10001", "10001", "01111", "00001", "00010", "01100"),
    // Persian digits, mapped to the same 5x7 cell so a fa readout keeps the matrix rhythm.
    '۰' to listOf("00100", "00100", "00100", "00100", "00100", "00100", "00100"),
    '۱' to listOf("00100", "01100", "00100", "00100", "00100", "00100", "01110"),
    '۲' to listOf("01110", "10001", "00001", "00010", "00100", "01000", "11111"),
    '۳' to listOf("11110", "00001", "00001", "01110", "00001", "00001", "11110"),
    '۴' to listOf("00010", "00110", "01010", "10010", "11111", "00010", "00010"),
    '۵' to listOf("11111", "10000", "11110", "00001", "00001", "10001", "01110"),
    '۶' to listOf("00110", "01000", "10000", "11110", "10001", "10001", "01110"),
    '۷' to listOf("11111", "00001", "00010", "00100", "01000", "01000", "01000"),
    '۸' to listOf("01110", "10001", "10001", "01110", "10001", "10001", "01110"),
    '۹' to listOf("01110", "10001", "10001", "01111", "00001", "00010", "01100"),
    '.' to listOf("00", "00", "00", "00", "00", "11", "11"),
    ',' to listOf("00", "00", "00", "00", "11", "11", "10"),
    ':' to listOf("00", "11", "11", "00", "11", "11", "00"),
    '+' to listOf("00000", "00100", "00100", "11111", "00100", "00100", "00000"),
    '-' to listOf("00000", "00000", "00000", "11111", "00000", "00000", "00000"),
    '°' to listOf("01100", "10010", "10010", "01100", "00000", "00000", "00000"),
    '‰' to listOf("01001", "01010", "00000", "11011", "00000", "00000", "00000"),
    'M' to listOf("10001", "11011", "10101", "10101", "10001", "10001", "10001"),
    'G' to listOf("01110", "10001", "10000", "10111", "10001", "10001", "01111"),
    'N' to listOf("10001", "11001", "10101", "10011", "10001", "10001", "10001"),
    'H' to listOf("10001", "10001", "10001", "11111", "10001", "10001", "10001"),
    'Z' to listOf("11111", "00001", "00010", "00100", "01000", "10000", "11111"),
    ' ' to listOf("00000", "00000", "00000", "00000", "00000", "00000", "00000")
)

/** True when [char] has a matrix glyph; call sites use it to fall back to a Text for prose. */
fun quartzMatrixSupports(char: Char): Boolean = DOT_MATRIX_GLYPHS.containsKey(char)

/** Columns a glyph occupies (used for layout measurement). */
private fun columns(char: Char): Int = DOT_MATRIX_GLYPHS[char]?.first()?.length ?: 0

/**
 * The 5x7 circular LED readout.
 *
 * [ghost] > 0 draws the unlit cells of the matrix at that alpha — the "sleeping" LED panel look of
 * the reference hardware. [glow] paints a two-ring optical bloom under every lit dot, which is what
 * makes the matrix read as emissive rather than printed.
 */
@Composable
fun QuartzDotMatrix(
    value: String,
    modifier: Modifier = Modifier,
    dotSize: Dp = 3.4.dp,
    dotGap: Dp = 1.9.dp,
    charGap: Dp = 6.dp,
    color: Color = Color.White,
    glow: Boolean = true,
    ghost: Float = 0f,
    squareDots: Boolean = false
) {
    val glyphs = remember(value) { value.mapNotNull { DOT_MATRIX_GLYPHS[it] } }
    if (glyphs.isEmpty()) return

    val stepDp = dotSize + dotGap
    val rows = 7
    val totalCols = glyphs.sumOf { it.first().length }
    val width = (stepDp * totalCols) - dotGap + (charGap - dotGap) * (glyphs.size - 1).coerceAtLeast(0)
    val height = (stepDp * rows) - dotGap

    Canvas(
        modifier = modifier.size(width = width.coerceAtLeast(dotSize), height = height)
    ) {
        val dPx = dotSize.toPx().coerceAtLeast(1.2f)
        val r = dPx / 2f
        val step = dPx + dotGap.toPx()
        val charAdvance = charGap.toPx()

        var cursorX = 0f
        for (grid in glyphs) {
            val cols = grid.first().length
            for (row in 0 until rows) {
                for (col in 0 until cols) {
                    if (grid[row][col] != '1') {
                        if (ghost > 0f) {
                            drawCircle(
                                color = color.copy(alpha = ghost * 0.5f),
                                radius = r * 0.42f,
                                center = Offset(cursorX + col * step + r, row * step + r)
                            )
                        }
                        continue
                    }
                    val cx = cursorX + col * step + r
                    val cy = row * step + r
                    if (glow) {
                        // Optical bloom: a wide soft halo and a tight hot core.
                        drawCircle(
                            brush = Brush.radialGradient(
                                0f to color.copy(alpha = 0.30f),
                                1f to Color.Transparent
                            ),
                            radius = r * 3.1f,
                            center = Offset(cx, cy)
                        )
                        drawCircle(color = color.copy(alpha = 0.20f), radius = r * 1.75f, center = Offset(cx, cy))
                    }
                    if (squareDots) {
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(cx - r, cy - r),
                            size = Size(dPx, dPx),
                            cornerRadius = CornerRadius(r * 0.3f, r * 0.3f)
                        )
                    } else {
                        drawCircle(color = color, radius = r, center = Offset(cx, cy))
                    }
                }
            }
            cursorX += cols * step - dotGap.toPx() + charAdvance
        }
    }
}

/**
 * The unlit-matrix plate: a hairline outline of a dot-matrix *display* (the four corner calibration
 * marks of the reference readout), drawn behind a value when a call site wants the panel to read as
 * a physical component rather than floating type.
 */
@Composable
fun QuartzMatrixFrame(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    cornerLength: Dp = 5.dp
) {
    Canvas(modifier = modifier) {
        val c = cornerLength.toPx()
        val sw = 0.9.dp.toPx()
        val w = size.width
        val h = size.height
        val inset = 1f
        // Four L-shaped calibration marks, one per corner.
        drawLine(color.copy(alpha = 0.55f), Offset(inset, inset), Offset(inset + c, inset), sw)
        drawLine(color.copy(alpha = 0.55f), Offset(inset, inset), Offset(inset, inset + c), sw)
        drawLine(color.copy(alpha = 0.55f), Offset(w - inset, inset), Offset(w - inset - c, inset), sw)
        drawLine(color.copy(alpha = 0.55f), Offset(w - inset, inset), Offset(w - inset, inset + c), sw)
        drawLine(color.copy(alpha = 0.55f), Offset(inset, h - inset), Offset(inset + c, h - inset), sw)
        drawLine(color.copy(alpha = 0.55f), Offset(inset, h - inset), Offset(inset, h - inset - c), sw)
        drawLine(color.copy(alpha = 0.55f), Offset(w - inset, h - inset), Offset(w - inset - c, h - inset), sw)
        drawLine(color.copy(alpha = 0.55f), Offset(w - inset, h - inset), Offset(w - inset, h - inset - c), sw)
    }
}

/** A hairline status ring: filled for "poured", outlined for "planned". */
@Composable
fun QuartzStatusMark(
    filled: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    diameter: Dp = 6.dp
) {
    Canvas(modifier = modifier.size(diameter)) {
        val r = size.minDimension / 2f
        if (filled) {
            drawCircle(color, radius = r)
        } else {
            drawCircle(color.copy(alpha = 0.42f), radius = r * 0.82f, style = Stroke(width = 1.dp.toPx()))
        }
    }
}

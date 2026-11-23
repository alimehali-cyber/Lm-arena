package com.alijafari.red.astronomy.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alijafari.red.astronomy.astro_engine.TimeEngine

/* ------------------------------------------------------------------ */
/*  Dot-matrix display — the observatory score, set in a 5x7 dot matrix */
/*  like an old observatory LED board. Persian digits in Persian mode   */
/*  (۰-۹ with ٪), Latin digits in English mode (0-9 with %).            */
/* ------------------------------------------------------------------ */

/** 5x7 glyph rows; bit 4 (0b10000) is the left-most dot. */
private val DotMatrixGlyphs: Map<Char, IntArray> = mapOf(
    '0' to intArrayOf(0b01100, 0b10010, 0b10010, 0b10010, 0b10010, 0b10010, 0b01100),
    '1' to intArrayOf(0b00110, 0b01110, 0b00010, 0b00010, 0b00010, 0b00010, 0b00010),
    '2' to intArrayOf(0b01110, 0b11011, 0b00011, 0b00010, 0b00100, 0b01000, 0b11111),
    '3' to intArrayOf(0b01110, 0b11011, 0b00011, 0b00110, 0b00011, 0b11011, 0b01110),
    '4' to intArrayOf(0b00110, 0b00110, 0b01110, 0b01010, 0b11111, 0b11111, 0b00010),
    '5' to intArrayOf(0b11111, 0b11000, 0b11110, 0b11011, 0b00001, 0b11011, 0b01110),
    '6' to intArrayOf(0b00110, 0b01000, 0b11110, 0b11111, 0b10001, 0b11011, 0b01110),
    '7' to intArrayOf(0b11111, 0b00011, 0b00010, 0b00110, 0b00100, 0b01100, 0b01100),
    '8' to intArrayOf(0b01110, 0b11011, 0b11011, 0b01110, 0b11011, 0b11011, 0b01110),
    '9' to intArrayOf(0b01110, 0b11011, 0b10001, 0b11011, 0b01111, 0b00010, 0b01110),
    '%' to intArrayOf(0b11000, 0b10110, 0b11100, 0b00111, 0b01101, 0b00011, 0b00000),
    // Persian digits — silhouettes hand-set in Eastern Arabic-Indic forms (۰-۹)
    '۰' to intArrayOf(0b00000, 0b00000, 0b01110, 0b10001, 0b01110, 0b00000, 0b00000),
    '۱' to intArrayOf(0b10010, 0b01100, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100),
    '۲' to intArrayOf(0b11000, 0b10100, 0b00110, 0b00010, 0b00100, 0b01000, 0b01110),
    '۳' to intArrayOf(0b00000, 0b10001, 0b11011, 0b01110, 0b00100, 0b00000, 0b00000),
    '۴' to intArrayOf(0b10111, 0b11100, 0b01111, 0b01010, 0b01000, 0b01000, 0b00000),
    '۵' to intArrayOf(0b00100, 0b01010, 0b10001, 0b11011, 0b01110, 0b00000, 0b00000),
    '۶' to intArrayOf(0b01110, 0b10000, 0b10000, 0b01100, 0b00010, 0b00010, 0b00000),
    '۷' to intArrayOf(0b10001, 0b10001, 0b01010, 0b01110, 0b00100, 0b00100, 0b00000),
    '۸' to intArrayOf(0b00100, 0b01010, 0b10001, 0b11011, 0b10001, 0b00000, 0b00000),
    '۹' to intArrayOf(0b01110, 0b10001, 0b11011, 0b01111, 0b00010, 0b00010, 0b00010),
    '٪' to intArrayOf(0b11000, 0b11000, 0b01100, 0b00100, 0b00011, 0b00011, 0b00000),
)

private const val GLYPH_COLS = 5
private const val GLYPH_ROWS = 7
private const val GLYPH_GAP_CELLS = 1.15f

/**
 * Renders [text] — digits plus one trailing percent glyph — as a 5x7 dot-matrix readout.
 * Dots glow in [color]; the unlit grid shows through faintly, like an LED panel.
 */
@Composable
fun DotMatrixText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    height: Dp = 44.dp,
    contentDescription: String? = null,
) {
    val cellDp = height / GLYPH_ROWS
    val gapDp = cellDp * GLYPH_GAP_CELLS
    val glyphs = remember(text) { text.mapNotNull { DotMatrixGlyphs[it] } }
    val gaps = (glyphs.size - 1).coerceAtLeast(0)
    val widthDp = cellDp * (GLYPH_COLS * glyphs.size).toFloat() + gapDp * gaps.toFloat()
    val litRadiusDp = cellDp * 0.40f
    val ghostRadiusDp = cellDp * 0.34f

    Canvas(
        modifier = modifier
            .height(height)
            .width(widthDp)
            .semantics { if (contentDescription != null) this.contentDescription = contentDescription },
    ) {
        val cellPx = size.height / GLYPH_ROWS
        val gapPx = cellPx * GLYPH_GAP_CELLS
        val litRadius = litRadiusDp.toPx()
        val ghostRadius = ghostRadiusDp.toPx()
        var originX = 0f
        glyphs.forEach { glyph ->
            for (row in 0 until GLYPH_ROWS) {
                val bits = glyph[row]
                for (col in 0 until GLYPH_COLS) {
                    val cx = originX + cellPx * (col + 0.5f)
                    val cy = cellPx * (row + 0.5f)
                    val lit = (bits shr (GLYPH_COLS - 1 - col) and 1) == 1
                    if (lit) {
                        drawCircle(color = color.copy(alpha = 0.16f), radius = litRadius * 1.75f, center = Offset(cx, cy))
                        drawCircle(color = color, radius = litRadius, center = Offset(cx, cy))
                        drawCircle(color = Color.White.copy(alpha = 0.35f), radius = litRadius * 0.38f, center = Offset(cx - litRadius * 0.22f, cy - litRadius * 0.22f))
                    } else {
                        drawCircle(color = color.copy(alpha = 0.055f), radius = ghostRadius, center = Offset(cx, cy))
                    }
                }
            }
            originX += cellPx * GLYPH_COLS + gapPx
        }
    }
}

/**
 * The object's live observatory (observability) score as a dot-matrix percentage — Persian digits
 * with ٪ in Persian mode, Latin digits with % in English mode.
 */
@Composable
fun DotMatrixScore(
    scorePercent: Int,
    isFa: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 46.dp,
) {
    val label = remember(scorePercent, isFa) {
        if (isFa) TimeEngine.formatPersianNumbers(scorePercent.toString()) + "٪" else "$scorePercent%"
    }
    val description = remember(scorePercent, isFa) {
        if (isFa) "امتیاز رصدپذیری لحظه‌ای ${TimeEngine.formatPersianNumbers(scorePercent.toString())}٪"
        else "Live observability score $scorePercent percent"
    }
    DotMatrixText(
        text = label,
        modifier = modifier,
        color = color,
        height = height,
        contentDescription = description,
    )
}

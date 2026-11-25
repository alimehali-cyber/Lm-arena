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
/*  Dot-matrix display — the observatory score, set in the same 5x7 dot */
/*  matrix face used by the Spatial and Aether UIs. Persian digits      */
/*  in Persian mode (۰-۹ with ٪), Latin digits in English (0-9 with %). */
/* ------------------------------------------------------------------ */

/**
 * 5x7 glyph rows; bit 4 (0b10000) is the left-most dot.
 * These are the glyphs from the shared Spatial / Aether dot-matrix faces, so the observatory
 * readout uses the same Persian digit and percent shapes as the rest of the app.
 */
private val DotMatrixGlyphs: Map<Char, IntArray> = mapOf(
    '0' to intArrayOf(0x0E, 0x11, 0x13, 0x15, 0x19, 0x11, 0x0E),
    '1' to intArrayOf(0x04, 0x0C, 0x04, 0x04, 0x04, 0x04, 0x0E),
    '2' to intArrayOf(0x0E, 0x11, 0x01, 0x06, 0x08, 0x10, 0x1F),
    '3' to intArrayOf(0x1F, 0x02, 0x04, 0x06, 0x01, 0x11, 0x0E),
    '4' to intArrayOf(0x02, 0x06, 0x0A, 0x12, 0x1F, 0x02, 0x02),
    '5' to intArrayOf(0x1F, 0x10, 0x1E, 0x01, 0x01, 0x11, 0x0E),
    '6' to intArrayOf(0x06, 0x08, 0x10, 0x1E, 0x11, 0x11, 0x0E),
    '7' to intArrayOf(0x1F, 0x01, 0x02, 0x04, 0x08, 0x08, 0x08),
    '8' to intArrayOf(0x0E, 0x11, 0x11, 0x0E, 0x11, 0x11, 0x0E),
    '9' to intArrayOf(0x0E, 0x11, 0x11, 0x0F, 0x01, 0x02, 0x0C),
    '%' to intArrayOf(0x19, 0x19, 0x02, 0x04, 0x08, 0x13, 0x13),
    // Persian digits — the current Spatial / Aether Eastern Arabic-Indic face (۰-۹)
    '۰' to intArrayOf(0x00, 0x04, 0x0A, 0x11, 0x0A, 0x04, 0x00),
    '۱' to intArrayOf(0x04, 0x0C, 0x04, 0x04, 0x04, 0x04, 0x04),
    '۲' to intArrayOf(0x11, 0x11, 0x1F, 0x10, 0x10, 0x10, 0x10),
    '۳' to intArrayOf(0x15, 0x15, 0x1F, 0x10, 0x10, 0x10, 0x10),
    '۴' to intArrayOf(0x07, 0x14, 0x1F, 0x10, 0x10, 0x10, 0x10),
    '۵' to intArrayOf(0x04, 0x0A, 0x11, 0x11, 0x15, 0x15, 0x0A),
    '۶' to intArrayOf(0x06, 0x08, 0x07, 0x04, 0x08, 0x10, 0x10),
    '۷' to intArrayOf(0x11, 0x11, 0x0A, 0x0A, 0x04, 0x04, 0x04),
    '۸' to intArrayOf(0x04, 0x04, 0x0A, 0x0A, 0x11, 0x11, 0x11),
    '۹' to intArrayOf(0x0E, 0x11, 0x11, 0x0F, 0x01, 0x01, 0x01),
    '٪' to intArrayOf(0x19, 0x19, 0x02, 0x04, 0x08, 0x13, 0x13),
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

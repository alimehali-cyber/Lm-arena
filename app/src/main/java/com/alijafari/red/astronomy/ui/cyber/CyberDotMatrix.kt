package com.alijafari.red.astronomy.ui.cyber

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.ui.theme.RedTypographyTokens
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// ============================================================================
// RETRO-FUTURISTIC DOT-MATRIX COUNTERS & TELEMETRY INDICATORS
// (Nothing OS / PP Neue Bit / Rubik Dots / Teenage Engineering Style)
// ============================================================================

/**
 * Renders retro-futuristic grid-dot pattern typography (inspired by Nothing OS, PP Neue Bit,
 * and Rubik Dots) for numerical counters, temperatures/altitudes, magnitudes, and percentages.
 *
 * Supports both crisp square-rounded Neue-Bit pixels ([squareDots] = true) and circular
 * Nothing-style matrix dots ([squareDots] = false), across ASCII (`0-9`, `A-Z`) and Persian (`۰-۹`, `٪`).
 */
@Composable
fun CyberMatrixText(
    text: String,
    modifier: Modifier = Modifier,
    dotSize: Dp = 2.6.dp,
    dotGap: Dp = 1.05.dp,
    charGap: Dp = 3.2.dp,
    squareDots: Boolean = false,
    showMatrixGrid: Boolean = false,
    activeColor: Color = CyberTheme.palette.matrixLit,
    glowColor: Color? = CyberTheme.palette.primaryAccent,
    ghostColor: Color = CyberTheme.palette.matrixGhost
) {
    val glyphs = remember(text) { text.map { cyberGlyphFor(it) } }
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
        val r = dPx / 2f
        val step = dPx + dotGap.toPx()
        val extraCharAdvance = charGap.toPx()
        val pixelCorner = CornerRadius(dPx * 0.25f, dPx * 0.25f)

        var cursorX = 0f
        for (glyph in glyphs) {
            for (row in 0 until 7) {
                val rowBits = glyph.rows[row]
                val cy = row * step + r
                for (col in 0 until glyph.cols) {
                    val mask = 1 shl (glyph.cols - 1 - col)
                    val isLit = (rowBits and mask) != 0
                    val cx = cursorX + col * step + r
                    if (isLit) {
                        if (glowColor != null) {
                            drawCircle(
                                color = glowColor.copy(alpha = 0.24f),
                                radius = r * 2.0f,
                                center = Offset(cx, cy)
                            )
                        }
                        if (squareDots) {
                            drawRoundRect(
                                color = activeColor,
                                topLeft = Offset(cx - r, cy - r),
                                size = Size(dPx, dPx),
                                cornerRadius = pixelCorner
                            )
                        } else {
                            drawCircle(
                                color = activeColor,
                                radius = r,
                                center = Offset(cx, cy)
                            )
                        }
                    } else if (showMatrixGrid && !glyph.isPunctuation) {
                        drawCircle(
                            color = ghostColor,
                            radius = r * 0.58f,
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
 * Nothing-OS Style Hero Dot-Matrix Counter paired with ultra-thin geometric status word
 * (e.g. `3 Good`, `89% Illum`, `16.2 Mag`, `-18.4° Dark`).
 */
@Composable
fun CyberStatCounter(
    valueText: String,
    statusLabel: String,
    modifier: Modifier = Modifier,
    captionLabel: String? = null,
    dotSize: Dp = 2.8.dp,
    accentColor: Color = CyberTheme.palette.primaryAccent,
    showSignalDot: Boolean = true
) {
    val palette = CyberTheme.palette
    Column(modifier = modifier) {
        if (captionLabel != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (showSignalDot) {
                    CyberSignalDot(color = accentColor, size = 5.5.dp, glowing = false)
                }
                Text(
                    text = captionLabel,
                    style = RedTypographyTokens.caption.copy(
                        fontWeight = FontWeight.Light,
                        fontSize = 11.sp,
                        letterSpacing = 0.6.sp
                    ),
                    color = palette.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CyberMatrixText(
                text = valueText,
                dotSize = dotSize,
                dotGap = 1.0.dp,
                charGap = 3.0.dp,
                showMatrixGrid = true,
                activeColor = palette.matrixLit,
                glowColor = accentColor
            )
            if (statusLabel.isNotBlank()) {
                Text(
                    text = statusLabel,
                    style = RedTypographyTokens.bodySecondary.copy(
                        fontWeight = FontWeight.Light,
                        fontSize = 13.sp
                    ),
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 1.dp)
                )
            }
        }
    }
}

/**
 * Compact Nothing-OS Circular Dot-Ring Gauge (`46.dp` default) composed of discrete glowing
 * circular matrix beads around a central `CyberMatrixText` readout.
 */
@Composable
fun CyberDotRingGauge(
    progress: Float,
    centerText: String,
    modifier: Modifier = Modifier,
    diameter: Dp = 46.dp,
    dotCount: Int = 18,
    accentColor: Color = CyberTheme.palette.primaryAccent,
    secondaryColor: Color = CyberTheme.palette.secondaryAccent
) {
    val palette = CyberTheme.palette
    val clamped = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = clamped,
        animationSpec = spring(dampingRatio = 0.80f, stiffness = 210f),
        label = "CyberDotRingGauge"
    )

    Box(
        modifier = modifier.size(diameter),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(diameter)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val ringR = (size.minDimension / 2f) - 3.2.dp.toPx()
            val startDeg = 135f
            val sweepDeg = 270f
            val litCount = (animated * dotCount).roundToInt()

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accentColor.copy(alpha = 0.16f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = ringR
                ),
                radius = ringR,
                center = Offset(cx, cy)
            )

            for (i in 0..dotCount) {
                val t = i.toFloat() / dotCount.toFloat()
                val rad = ((startDeg + sweepDeg * t) * PI / 180.0).toFloat()
                val px = cx + cos(rad) * ringR
                val py = cy + sin(rad) * ringR
                val isLit = i <= litCount && animated > 0.005f
                if (isLit) {
                    val c = lerpCyberColor(accentColor, secondaryColor, t)
                    drawCircle(
                        color = c.copy(alpha = 0.30f),
                        radius = 3.2.dp.toPx(),
                        center = Offset(px, py)
                    )
                    drawCircle(
                        color = c,
                        radius = 1.65.dp.toPx(),
                        center = Offset(px, py)
                    )
                } else {
                    drawCircle(
                        color = palette.matrixGhost,
                        radius = 1.25.dp.toPx(),
                        center = Offset(px, py)
                    )
                }
            }
        }

        CyberMatrixText(
            text = centerText,
            dotSize = 1.4.dp,
            dotGap = 0.55.dp,
            charGap = 1.35.dp,
            activeColor = palette.matrixLit,
            glowColor = accentColor
        )
    }
}

private fun lerpCyberColor(a: Color, b: Color, t: Float): Color {
    val f = t.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * f,
        green = a.green + (b.green - a.green) * f,
        blue = a.blue + (b.blue - a.blue) * f,
        alpha = a.alpha + (b.alpha - a.alpha) * f
    )
}

private class CyberGlyph(
    val cols: Int,
    val rows: IntArray,
    val isPunctuation: Boolean = false
)

private fun g5(r0: Int, r1: Int, r2: Int, r3: Int, r4: Int, r5: Int, r6: Int) =
    CyberGlyph(5, intArrayOf(r0, r1, r2, r3, r4, r5, r6))

private fun g3(r0: Int, r1: Int, r2: Int, r3: Int, r4: Int, r5: Int, r6: Int) =
    CyberGlyph(3, intArrayOf(r0, r1, r2, r3, r4, r5, r6), isPunctuation = true)

private fun cyberGlyphFor(ch: Char): CyberGlyph = when (ch) {
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

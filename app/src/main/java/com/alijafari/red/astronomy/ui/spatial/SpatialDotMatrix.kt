package com.alijafari.red.astronomy.ui.spatial

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
// 5x7 LED DOT-MATRIX DISPLAY TYPOGRAPHY & CIRCULAR RADIAL DOT METERS
// ============================================================================

/**
 * Renders crisp 5x7 LED dot-matrix display typography for numerical stats, coordinates,
 * magnitudes, illumination percentages, and countdowns.
 *
 * Supports both ASCII (`0-9`, `A-Z`) and native Persian (`۰-۹`, `٪`) glyphs, rendered
 * as discrete glowing micro-dots with optional unlit matrix ghost dots behind them.
 */
@Composable
fun DotMatrixText(
    text: String,
    modifier: Modifier = Modifier,
    dotSize: Dp = 2.6.dp,
    dotGap: Dp = 1.1.dp,
    charGap: Dp = 3.2.dp,
    activeColor: Color = SpatialTheme.palette.dotMatrixInk,
    glowColor: Color? = SpatialNeonRose,
    showGhostMatrix: Boolean = false,
    ghostColor: Color = SpatialTheme.palette.dotMatrixGhost
) {
    val glyphs = remember(text) {
        text.map { ch -> glyphFor(ch) }
    }
    if (glyphs.isEmpty()) return

    val totalColumns = remember(glyphs) {
        glyphs.sumOf { it.cols }
    }
    val charCount = glyphs.size

    val stepDp = dotSize + dotGap
    val totalWidth: Dp = (stepDp * totalColumns) - dotGap + (charGap - dotGap) * (charCount - 1).coerceAtLeast(0)
    val totalHeight: Dp = (stepDp * 7) - dotGap

    Canvas(
        modifier = modifier
            .width(totalWidth.coerceAtLeast(4.dp))
            .height(totalHeight)
    ) {
        val r = (dotSize.toPx() / 2f).coerceAtLeast(0.8f)
        val step = dotSize.toPx() + dotGap.toPx()
        val extraCharAdvance = charGap.toPx()

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
                                color = glowColor.copy(alpha = 0.28f),
                                radius = r * 2.1f,
                                center = Offset(cx, cy)
                            )
                        }
                        drawCircle(
                            color = activeColor,
                            radius = r,
                            center = Offset(cx, cy)
                        )
                    } else if (showGhostMatrix && !glyph.isNarrowPunctuation) {
                        drawCircle(
                            color = ghostColor,
                            radius = r * 0.72f,
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
 * Circular Radial Dot Meter:
 * Minimalist VisionOS-style radial progress gauge composed of precision circular tick marks /
 * glowing micro-dots, a dot-matrix numerical readout in the center, and a clean sans-serif status label.
 */
@Composable
fun SpatialRadialDotMeter(
    progress: Float,
    centerValueText: String,
    statusLabel: String,
    modifier: Modifier = Modifier,
    subValueText: String? = null,
    diameter: Dp = 118.dp,
    tickCount: Int = 34,
    activeColor: Color = SpatialNeonRose,
    secondaryActiveColor: Color = SpatialWarmPeach,
    dotSize: Dp = 2.3.dp
) {
    val palette = SpatialTheme.palette
    val clamped = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clamped,
        animationSpec = spring(dampingRatio = 0.80f, stiffness = 200f),
        label = "SpatialRadialMeter"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(diameter),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(diameter)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val outerRadius = (size.minDimension / 2f) - 4.dp.toPx()
                val innerTickRadius = outerRadius - 6.5.dp.toPx()
                val dotRingRadius = innerTickRadius - 4.5.dp.toPx()

                // Subtle ambient glow aura inside active gauge center
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            activeColor.copy(alpha = if (palette.isDark) 0.16f else 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = outerRadius * 0.9f
                    ),
                    radius = outerRadius * 0.9f,
                    center = Offset(cx, cy)
                )

                val startDeg = 135f
                val sweepDeg = 270f
                val activeTicks = (animatedProgress * tickCount).roundToInt()

                for (i in 0..tickCount) {
                    val t = i.toFloat() / tickCount.toFloat()
                    val angleRad = ((startDeg + sweepDeg * t) * PI / 180.0).toFloat()
                    val cosA = cos(angleRad)
                    val sinA = sin(angleRad)

                    val isLit = i <= activeTicks && animatedProgress > 0.005f
                    val isMajor = (i % 5 == 0)
                    val tickInner = if (isMajor) innerTickRadius - 2.dp.toPx() else innerTickRadius

                    val p1 = Offset(cx + cosA * tickInner, cy + sinA * tickInner)
                    val p2 = Offset(cx + cosA * outerRadius, cy + sinA * outerRadius)

                    if (isLit) {
                        val blendedColor = lerpColor(activeColor, secondaryActiveColor, t)
                        // Soft halo around lit radial ticks
                        drawLine(
                            color = blendedColor.copy(alpha = 0.30f),
                            start = p1,
                            end = p2,
                            strokeWidth = 4.2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = blendedColor,
                            start = p1,
                            end = p2,
                            strokeWidth = if (isMajor) 2.2.dp.toPx() else 1.6.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    } else {
                        drawLine(
                            color = if (palette.isDark) Color(0x2BFFFFFF) else Color(0x26321E38),
                            start = p1,
                            end = p2,
                            strokeWidth = if (isMajor) 1.7.dp.toPx() else 1.3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    // Inner ring of tiny precision micro-dots every 2nd step
                    if (i % 2 == 0) {
                        val dotCenter = Offset(cx + cosA * dotRingRadius, cy + sinA * dotRingRadius)
                        drawCircle(
                            color = if (isLit) activeColor.copy(alpha = 0.85f)
                            else if (palette.isDark) Color(0x1FFFFFFF) else Color(0x1C321E38),
                            radius = 1.15.dp.toPx(),
                            center = dotCenter
                        )
                    }
                }

                // Glowing beacon dot at current progress tip
                if (animatedProgress > 0.005f) {
                    val tipAngle = ((startDeg + sweepDeg * animatedProgress) * PI / 180.0).toFloat()
                    val tipRadius = (outerRadius + innerTickRadius) * 0.5f
                    val tipPos = Offset(cx + cos(tipAngle) * tipRadius, cy + sin(tipAngle) * tipRadius)
                    drawCircle(
                        color = activeColor.copy(alpha = 0.45f),
                        radius = 6.5.dp.toPx(),
                        center = tipPos
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.4.dp.toPx(),
                        center = tipPos
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                DotMatrixText(
                    text = centerValueText,
                    dotSize = dotSize,
                    dotGap = 0.9.dp,
                    charGap = 2.6.dp,
                    activeColor = palette.dotMatrixInk,
                    glowColor = activeColor
                )
                if (subValueText != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subValueText,
                        style = RedTypographyTokens.caption.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = palette.textTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = statusLabel,
            style = RedTypographyTokens.caption.copy(
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
            ),
            color = palette.textSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Compact inline Circular Radial Dot Meter (`42.dp` default) designed to fit directly into
 * Default-layout trailing badge slots (Tonight's Sky rows, Satellite Pass rows, Lab cards)
 * without altering row heights or grid geometry.
 */
@Composable
fun SpatialCompactRadialMeter(
    progress: Float,
    centerValueText: String,
    modifier: Modifier = Modifier,
    diameter: Dp = 44.dp,
    tickCount: Int = 20,
    activeColor: Color = SpatialNeonRose,
    secondaryActiveColor: Color = SpatialWarmPeach,
    dotSize: Dp = 1.45.dp
) {
    val palette = SpatialTheme.palette
    val clamped = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clamped,
        animationSpec = spring(dampingRatio = 0.80f, stiffness = 220f),
        label = "SpatialCompactRadialMeter"
    )

    Box(
        modifier = modifier.size(diameter),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(diameter)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val outerRadius = (size.minDimension / 2f) - 2.dp.toPx()
            val innerTickRadius = outerRadius - 4.2.dp.toPx()

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        activeColor.copy(alpha = if (palette.isDark) 0.18f else 0.12f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = outerRadius
                ),
                radius = outerRadius,
                center = Offset(cx, cy)
            )

            val startDeg = 135f
            val sweepDeg = 270f
            val activeTicks = (animatedProgress * tickCount).roundToInt()

            for (i in 0..tickCount) {
                val t = i.toFloat() / tickCount.toFloat()
                val angleRad = ((startDeg + sweepDeg * t) * PI / 180.0).toFloat()
                val cosA = cos(angleRad)
                val sinA = sin(angleRad)
                val isLit = i <= activeTicks && animatedProgress > 0.005f

                val p1 = Offset(cx + cosA * innerTickRadius, cy + sinA * innerTickRadius)
                val p2 = Offset(cx + cosA * outerRadius, cy + sinA * outerRadius)

                if (isLit) {
                    val col = lerpColor(activeColor, secondaryActiveColor, t)
                    drawLine(
                        color = col,
                        start = p1,
                        end = p2,
                        strokeWidth = 1.65.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                } else {
                    drawLine(
                        color = if (palette.isDark) Color(0x2BFFFFFF) else Color(0x26321E38),
                        start = p1,
                        end = p2,
                        strokeWidth = 1.2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        DotMatrixText(
            text = centerValueText,
            dotSize = dotSize,
            dotGap = 0.55.dp,
            charGap = 1.4.dp,
            activeColor = palette.dotMatrixInk,
            glowColor = activeColor
        )
    }
}

private fun lerpColor(a: Color, b: Color, t: Float): Color {
    val clamped = t.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * clamped,
        green = a.green + (b.green - a.green) * clamped,
        blue = a.blue + (b.blue - a.blue) * clamped,
        alpha = a.alpha + (b.alpha - a.alpha) * clamped
    )
}

private class MatrixGlyph(
    val cols: Int,
    val rows: IntArray,
    val isNarrowPunctuation: Boolean = false
)

private fun glyph5(r0: Int, r1: Int, r2: Int, r3: Int, r4: Int, r5: Int, r6: Int) =
    MatrixGlyph(5, intArrayOf(r0, r1, r2, r3, r4, r5, r6))

private fun glyph3(r0: Int, r1: Int, r2: Int, r3: Int, r4: Int, r5: Int, r6: Int) =
    MatrixGlyph(3, intArrayOf(r0, r1, r2, r3, r4, r5, r6), isNarrowPunctuation = true)

private fun glyphFor(ch: Char): MatrixGlyph = when (ch) {
    // ASCII Digits 0-9
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

    // Persian / Extended Arabic-Indic Digits ۰-۹
    '۰', '٠' -> glyph5(0x00, 0x04, 0x0A, 0x11, 0x0A, 0x04, 0x00)
    '۱', '١' -> glyph5(0x04, 0x0C, 0x04, 0x04, 0x04, 0x04, 0x04)
    '۲', '٢' -> glyph5(0x11, 0x11, 0x1F, 0x10, 0x10, 0x10, 0x10)
    '۳', '٣' -> glyph5(0x15, 0x15, 0x1F, 0x10, 0x10, 0x10, 0x10)
    '۴', '٤' -> glyph5(0x13, 0x15, 0x1F, 0x10, 0x10, 0x10, 0x10)
    '۵', '٥' -> glyph5(0x04, 0x0A, 0x11, 0x11, 0x15, 0x15, 0x0A)
    '۶', '٦' -> glyph5(0x06, 0x09, 0x07, 0x04, 0x08, 0x10, 0x10)
    '۷', '٧' -> glyph5(0x11, 0x11, 0x0A, 0x0A, 0x04, 0x04, 0x04)
    '۸', '٨' -> glyph5(0x04, 0x04, 0x0A, 0x0A, 0x11, 0x11, 0x11)
    '۹', '٩' -> glyph5(0x0E, 0x11, 0x11, 0x0F, 0x01, 0x01, 0x01)

    // Punctuation & Astronomical Units
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

    // Uppercase & lowercase Latin letters (for units like KM, DEG, MIN, N, S, E, W, MAG, ISS)
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

package com.alijafari.red.astronomy.ui.cyber

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.alijafari.red.astronomy.domain.ThemeMode

/**
 * Ambient / Ethereal Mesh Background + Nothing OS Precision Micro-Dot Matrix Field.
 *
 * Bleeds soft multi-color ambient gradients (coral, peach, cyan, violet — or deep burgundy,
 * merlot, garnet crimson, and warm cognac-rose in Wine / "می" mode) behind the ultra-sheer
 * `rgba(255, 255, 255, 0.07)` Spatial Glass 2.0 containers, overlaid with a fine Nothing-inspired
 * architectural dot-matrix coordinate grid.
 */
@Composable
fun CyberAmbientBackground(
    themeMode: ThemeMode,
    isOpaqueCanvas: Boolean,
    sunAltitudeDeg: Double = -18.0,
    modifier: Modifier = Modifier
) {
    val palette = CyberTheme.palette
    val twilightShift = ((sunAltitudeDeg + 18.0) / 24.0).coerceIn(0.0, 1.0).toFloat()

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        if (isOpaqueCanvas) {
            val baseGradient = when (themeMode) {
                ThemeMode.WINE -> listOf(
                    Color(0xFF0B0105),
                    Color(0xFF17040B),
                    Color(0xFF240712),
                    Color(0xFF120208)
                )
                ThemeMode.DARK -> listOf(
                    Color(0xFF05070D),
                    Color(0xFF090D18),
                    Color(0xFF0E1324),
                    Color(0xFF06080E)
                )
                ThemeMode.LIGHT -> listOf(
                    Color(0xFFF4F6FB),
                    Color(0xFFEEF2F9),
                    Color(0xFFF6F1F5),
                    Color(0xFFEDF2F8)
                )
            }
            drawRect(brush = Brush.verticalGradient(colors = baseGradient))
        }

        val alphaScale = if (isOpaqueCanvas) 1f else 0.55f

        // 1. Ambient Ethereal Mesh Orbs (Coral, Peach, Cyan, Violet — or Wine, Burgundy, Garnet, Cognac)
        val orbTopRight = when (themeMode) {
            ThemeMode.WINE -> Color(0xFFD92B52).copy(alpha = 0.28f * alphaScale)
            ThemeMode.DARK -> CyberCoral.copy(alpha = 0.20f * alphaScale)
            ThemeMode.LIGHT -> CyberCoral.copy(alpha = 0.24f * alphaScale)
        }
        val orbMidLeft = when (themeMode) {
            ThemeMode.WINE -> Color(0xFF7D1128).copy(alpha = 0.38f * alphaScale)
            ThemeMode.DARK -> CyberCyan.copy(alpha = 0.17f * alphaScale)
            ThemeMode.LIGHT -> CyberCyan.copy(alpha = 0.22f * alphaScale)
        }
        val orbLowerRight = when (themeMode) {
            ThemeMode.WINE -> Color(0xFFB52246).copy(alpha = 0.28f * alphaScale)
            ThemeMode.DARK -> CyberViolet.copy(alpha = 0.20f * alphaScale)
            ThemeMode.LIGHT -> CyberViolet.copy(alpha = 0.20f * alphaScale)
        }
        val orbHorizon = when (themeMode) {
            ThemeMode.WINE -> CyberWineCognac.copy(alpha = (0.16f + 0.08f * twilightShift) * alphaScale)
            ThemeMode.DARK -> CyberPeach.copy(alpha = (0.14f + 0.08f * twilightShift) * alphaScale)
            ThemeMode.LIGHT -> CyberPeach.copy(alpha = 0.26f * alphaScale)
        }

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(orbTopRight, Color.Transparent),
                center = Offset(w * 0.86f, h * 0.14f),
                radius = w * 0.78f
            ),
            radius = w * 0.78f,
            center = Offset(w * 0.86f, h * 0.14f)
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(orbMidLeft, Color.Transparent),
                center = Offset(w * 0.12f, h * 0.36f),
                radius = w * 0.82f
            ),
            radius = w * 0.82f,
            center = Offset(w * 0.12f, h * 0.36f)
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(orbLowerRight, Color.Transparent),
                center = Offset(w * 0.84f, h * 0.72f),
                radius = w * 0.80f
            ),
            radius = w * 0.80f,
            center = Offset(w * 0.84f, h * 0.72f)
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(orbHorizon, Color.Transparent),
                center = Offset(w * 0.32f, h * 0.84f),
                radius = w * 0.74f
            ),
            radius = w * 0.74f,
            center = Offset(w * 0.32f, h * 0.84f)
        )

        // 2. Nothing OS / Teenage Engineering Architectural Micro-Dot Matrix Field
        val pitchPx = 24.dp.toPx().coerceAtLeast(16f)
        val dotRadius = 0.95.dp.toPx()
        val cols = (w / pitchPx).toInt()
        val rows = (h / pitchPx).toInt()
        val dotColor = palette.microGridDot
        for (r in 1..rows) {
            val y = r * pitchPx
            for (c in 1..cols) {
                val x = c * pitchPx
                drawCircle(
                    color = dotColor,
                    radius = dotRadius,
                    center = Offset(x, y)
                )
            }
        }
    }
}

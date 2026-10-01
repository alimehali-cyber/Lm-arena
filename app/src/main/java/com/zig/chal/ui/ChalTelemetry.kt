package com.zig.chal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.ui.theme.RedCornerRadius
import com.alijafari.red.astronomy.ui.theme.RedSpacing
import com.zig.chal.render.ChalRenderer
import java.util.Locale

/**
 * Real-time renderer telemetry.
 *
 * The XAPK backend uses its native FPS/render-scale data with Kerr helper readouts; the GLES
 * compatibility renderer retains Chal's quality, Schwarzschild-redshift approximation, dilation,
 * and frame-budget telemetry.
 */
@Composable
fun ChalTelemetry(
    snapshot: ChalRenderer.ChalSnapshot,
    isPersian: Boolean,
    isXapkRenderer: Boolean = false,
    modifier: Modifier = Modifier
) {
    val fpsOpacity = when {
        snapshot.currentFps >= 60 -> 0.95f
        snapshot.currentFps >= 30 -> 0.80f
        else -> 0.50f
    }

    val items = if (isXapkRenderer) {
        buildList {
            add(Triple("FPS", snapshot.currentFps.toString(), ""))
            add(Triple("Scale", "${Math.round(snapshot.params.renderScale * 100.0)}%", ""))
            add(Triple("r₊", fixedWidth(snapshot.eventHorizonRadius, 6, 2), ""))
            add(Triple("Photon", fixedWidth(snapshot.photonSphereRadius, 6, 2), ""))
            add(Triple("ISCO", fixedWidth(snapshot.iscoRadius, 6, 2), ""))
            if (kotlin.math.abs(snapshot.params.spin) > 1e-4) {
                add(Triple("a*", fixedWidth(snapshot.params.spin, 5, 2), ""))
            }
        }
    } else {
        listOf(
            Triple(if (isPersian) "فریم" else "FPS", snapshot.currentFps.toString(), "hz"),
            Triple(if (isPersian) "کیفیت" else "Quality", snapshot.quality.label, "lvl"),
            Triple(if (isPersian) "افق" else "Horizon", fixedWidth(snapshot.eventHorizonRadius, 6, 2), "Rs"),
            Triple(if (isPersian) "سرخ‌گرایی" else "Redshift", "z=${fixedWidth(snapshot.redshift, 6, 2)}", ""),
            Triple(if (isPersian) "کشش زمان" else "Dilation", fixedWidth(snapshot.timeDilation, 7, 3), "x")
        )
    }

    Column(
        modifier = modifier.testTag("chal_telemetry"),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(RedSpacing.xs)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(RedSpacing.md)) {
            items.forEachIndexed { index, (label, value, unit) ->
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = label.uppercase(Locale.US),
                        color = Color.White.copy(alpha = 0.80f),
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = value,
                            color = Color.White.copy(alpha = if (index == 0) fpsOpacity else 0.95f),
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black
                        )
                        if (unit.isNotEmpty()) {
                            Text(
                                text = unit,
                                color = Color.White.copy(alpha = 0.70f),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        if (!isXapkRenderer && snapshot.budgetUsage > 0.0) {
            Column(modifier = Modifier.width(180.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isPersian) "بودجه فریم" else "Frame Budget",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 7.sp
                    )
                    Text(
                        text = "${Math.round(snapshot.budgetUsage)}%",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 7.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(RedCornerRadius.full))
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth((snapshot.budgetUsage / 100.0).coerceIn(0.0, 1.0).toFloat())
                            .height(4.dp)
                            .clip(RoundedCornerShape(RedCornerRadius.full))
                            .background(Color.White.copy(alpha = 0.45f))
                    )
                }
            }
        }
    }
}

/**
 * Pads a number to a fixed total character count so HUD columns never shift when the integer-part
 * width changes. Sign reserves one column either way.
 */
internal fun fixedWidth(value: Double, totalWidth: Int, decimals: Int): String {
    val sign = if (value < 0) "-" else " "
    val body = String.format(Locale.US, "%.${decimals}f", kotlin.math.abs(value))
    return (sign + body).padStart(totalWidth, ' ')
}

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
import com.alijafari.red.astronomy.ui.theme.RedTheme
import com.zig.chal.config.ChalFormatting
import com.zig.chal.render.ChalRenderer
import java.util.Locale
import kotlin.math.abs

/**
 * Real-time renderer telemetry.
 *
 * One contract for both backends: frame rate and frame time come from whichever engine is
 * presenting, the ray budget and render scale are the values that engine is *actually* using, and
 * Kerr radii are normalized to geometric mass units (M). Redshift/time are explicitly marked as
 * Schwarzschild approximations because this HUD helper does not evaluate spin-dependent Kerr time.
 */
@Composable
fun ChalTelemetry(
    snapshot: ChalRenderer.ChalSnapshot,
    isPersian: Boolean,
    modifier: Modifier = Modifier
) {
    val fpsOpacity = when {
        snapshot.currentFps >= 60 -> 0.95f
        snapshot.currentFps >= 30 -> 0.80f
        else -> 0.50f
    }

    val items = buildList {
        add(
            TelemetryValue(
                label = if (isPersian) "نرخ" else "FPS",
                value = ChalFormatting.integer(snapshot.currentFps, isPersian),
                unit = "",
                isFrameRate = true
            )
        )
        if (snapshot.frameTimeMs > 0.0) {
            add(
                TelemetryValue(
                    label = if (isPersian) "فریم" else "Frame",
                    value = ChalFormatting.fixed(snapshot.frameTimeMs, 1, isPersian),
                    unit = if (isPersian) "میلی‌ثانیه" else "ms"
                )
            )
        }
        add(TelemetryValue(if (isPersian) "گام پرتو" else "Ray steps", ChalFormatting.integer(snapshot.raySteps, isPersian), ""))
        add(TelemetryValue(if (isPersian) "مقیاس" else "Scale", ChalFormatting.percent(snapshot.effectiveRenderScale, isPersian), ""))
        add(
            TelemetryValue(
                if (isPersian) "افق" else "r₊",
                ChalFormatting.fixed(radiusInMassUnits(snapshot.eventHorizonRadius, snapshot.params.mass), 2, isPersian),
                "M"
            )
        )
        add(
            TelemetryValue(
                if (isPersian) "فوتون" else "Photon",
                ChalFormatting.fixed(radiusInMassUnits(snapshot.photonSphereRadius, snapshot.params.mass), 2, isPersian),
                "M"
            )
        )
        add(
            TelemetryValue(
                "ISCO",
                ChalFormatting.fixed(radiusInMassUnits(snapshot.iscoRadius, snapshot.params.mass), 2, isPersian),
                "M"
            )
        )
        if (abs(snapshot.params.spin) > 1e-4) {
            add(TelemetryValue("a*", ChalFormatting.fixed(snapshot.params.spin, 2, isPersian), ""))
        }
        add(
            TelemetryValue(
                "z*",
                redshiftText(snapshot.redshift, isPersian),
                ""
            )
        )
        add(
            TelemetryValue(
                if (isPersian) "زمان*" else "Time*",
                ChalFormatting.fixed(snapshot.timeDilation, 3, isPersian),
                "×"
            )
        )
    }

    Column(
        modifier = modifier.testTag("chal_telemetry"),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(RedSpacing.xs)
    ) {
        items.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(RedSpacing.md)) {
                row.forEach { item ->
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = item.label.uppercase(Locale.US),
                            color = Color.White.copy(alpha = 0.80f),
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = item.value,
                                color = Color.White.copy(alpha = if (item.isFrameRate) fpsOpacity else 0.95f),
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black
                            )
                            if (item.unit.isNotEmpty()) {
                                Text(
                                    text = " ${item.unit}",
                                    color = Color.White.copy(alpha = 0.70f),
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        Text(
            text = if (isPersian) {
                "شعاع‌ها برحسب M هستند؛ z و زمان با تقریب شوارتزشیلد محاسبه می‌شوند."
            } else {
                "Radii are in M; z and time use a Schwarzschild approximation."
            },
            color = Color.White.copy(alpha = 0.58f),
            fontSize = 8.sp,
            maxLines = 2
        )

        if (snapshot.budgetUsage > 0.0) {
            Column(modifier = Modifier.width(180.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isPersian) "بودجه فریم" else "Frame budget",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 7.sp
                    )
                    Text(
                        text = ChalFormatting.percent(snapshot.budgetUsage / 100.0, isPersian),
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
                            .background(
                                if (snapshot.budgetUsage > 100.0) {
                                    RedTheme.colors.statusError.copy(alpha = 0.85f)
                                } else {
                                    Color.White.copy(alpha = 0.45f)
                                }
                            )
                    )
                }
            }
        }
    }
}

/** One readout cell. */
private data class TelemetryValue(
    val label: String,
    val value: String,
    val unit: String,
    val isFrameRate: Boolean = false
)

/**
 * Gravitational redshift grows without bound at the horizon; printing a 12-character float would
 * push the whole HUD row off screen, so the readout saturates instead of lying about precision.
 */
private fun redshiftText(redshift: Double, isPersian: Boolean): String = when {
    !redshift.isFinite() -> if (isPersian) "بی‌نهایت" else "inf"
    redshift > 99.99 -> ">${ChalFormatting.integer(99, isPersian)}"
    else -> ChalFormatting.fixed(redshift, 2, isPersian)
}

private fun radiusInMassUnits(radius: Double, mass: Double): Double =
    if (radius.isFinite() && mass.isFinite() && mass > 0.0) radius / mass else 0.0

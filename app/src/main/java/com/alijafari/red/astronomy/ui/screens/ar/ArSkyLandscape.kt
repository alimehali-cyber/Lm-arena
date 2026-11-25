package com.alijafari.red.astronomy.ui.screens.ar

import androidx.compose.foundation.Canvas
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.withTransform
import com.alijafari.red.astronomy.ar.core.ArCameraModel
import com.alijafari.red.astronomy.ar.core.LunarPhotometry
import com.alijafari.red.astronomy.domain.SkyCanvasTheme
import com.alijafari.red.astronomy.ui.rendering.LightingEngine
import com.alijafari.red.astronomy.ui.rendering.SceneComposition
import com.alijafari.red.astronomy.ui.rendering.SceneGroundRenderer
import com.alijafari.red.astronomy.ui.rendering.SkySceneLayout
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * The Home sky canvas's ground band, seated on the AR screen's *real* horizon.
 *
 * The AR camera model projects altitude 0° through the same pinhole (or CameraX transform) that
 * places every star, so the landscape follows pitch, roll and zoom instead of sitting at a fixed
 * 70 % of the viewport. Only the camera-off (simulated sky) layer uses it: over a live camera the
 * ground is whatever the lens sees.
 *
 * The geometry is borrowed from [SkySceneLayout] and the painting from [SceneGroundRenderer], so the
 * ridges, grass, palette and daylight response are the same artwork as the sky canvas — not a
 * second silhouette drawn to look similar.
 */
internal fun arMoonIlluminationPercent(phaseAngleDeg: Double?): Double =
    (LunarPhotometry.illuminatedFraction(phaseAngleDeg ?: 90.0) * 100.0).coerceIn(0.0, 100.0)

@androidx.compose.runtime.Composable
fun ArSkyLandscape(
    cameraModel: ArCameraModel?,
    theme: SkyCanvasTheme,
    sunAltDeg: Double,
    moonAltDeg: Double,
    moonIlluminationPercent: Double,
    modifier: Modifier = Modifier
) {
    val lighting = androidx.compose.runtime.remember(sunAltDeg, moonAltDeg, moonIlluminationPercent) {
        LightingEngine.computeLightingState(sunAltDeg, moonAltDeg, moonIlluminationPercent)
    }
    Canvas(modifier) {
        val cam = cameraModel ?: return@Canvas
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        // Sample the true horizon in ENU and project it into this frame. Two degrees is finer than
        // a pixel at any supported field of view, and costs nothing compared with the ground paint.
        val projected = ArrayList<Pair<Float, Float>>(180)
        val out = FloatArray(2)
        var az = 0.0
        while (az < 360.0) {
            val a = Math.toRadians(az)
            if (cam.project(sin(a), cos(a), 0.0, out) &&
                out[0] in -w..(2f * w) && out[1] in -h..(2f * h)
            ) {
                projected.add(out[0] to out[1])
            }
            az += 2.0
        }
        if (projected.size < 3) return@Canvas // looking straight up: no ground in view
        val sorted = projected.sortedBy { it.first }

        fun horizonYAt(x: Float): Float {
            if (x <= sorted.first().first) return sorted.first().second
            if (x >= sorted.last().first) return sorted.last().second
            for (i in 1 until sorted.size) {
                val a = sorted[i - 1]
                val b = sorted[i]
                if (x <= b.first) {
                    val t = ((x - a.first) / (b.first - a.first).coerceAtLeast(1e-4f)).coerceIn(0f, 1f)
                    return a.second + (b.second - a.second) * t
                }
            }
            return sorted.last().second
        }

        val horizonY = horizonYAt(w / 2f)
        if (horizonY < -h * 0.15f || horizonY > h * 1.15f) return@Canvas
        val rollDeg = Math.toDegrees(
            atan2((horizonYAt(w) - horizonYAt(0f)).toDouble(), w.toDouble())
        ).toFloat()

        // Draw the scene larger than the viewport so a rolled horizon never exposes a corner.
        val overscan = 1.6f
        val sceneW = w * overscan
        val sceneH = h * overscan
        val offX = (sceneW - w) / 2f
        val offY = (sceneH - h) / 2f
        val composition = SceneComposition(
            widthPx = sceneW,
            heightPx = sceneH,
            horizonY = horizonY + offY,
            groundTopY = horizonY + offY + h * SkySceneLayout.GROUND_TOP_FRACTION,
            farRidgeAmplitude = h * SkySceneLayout.FAR_RIDGE_FRACTION,
            midRidgeAmplitude = h * SkySceneLayout.MID_RIDGE_FRACTION
        )
        withTransform({
            rotate(rollDeg, pivot = Offset(w / 2f, horizonY))
            translate(-offX, -offY)
        }) {
            SceneGroundRenderer.draw(this, composition, theme, lighting, drawVegetation = false)
        }
    }
}

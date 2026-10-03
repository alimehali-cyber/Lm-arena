package com.alijafari.red.astronomy.ui.rendering

import androidx.compose.ui.graphics.drawscope.DrawScope
import com.alijafari.red.astronomy.astro_engine.GalacticEngine
import com.alijafari.red.astronomy.domain.SkyCanvasTheme

object MilkyWayRenderer {

    @Suppress("UNUSED_PARAMETER")
    fun drawMilkyWay(
        drawScope: DrawScope,
        galacticPoints: List<GalacticEngine.GalacticPlanePoint>,
        lightingState: LightingState,
        frameTimeMs: Long,
        theme: SkyCanvasTheme = SkyCanvasTheme.CELESTIAL,
        latitudeDeg: Double = 0.0
    ) {
        // Generic stroke-band Milky Way line removed per user request.
    }
}

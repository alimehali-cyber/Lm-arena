package com.alijafari.red.astronomy.ui.satellites

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.alijafari.red.astronomy.astro_engine.SatelliteItem
import com.alijafari.red.astronomy.astro_engine.SatelliteLiveState
import com.alijafari.red.astronomy.ui.satellites.core.*

/** Test-only scene inputs, not a raster of a different screen. All annotations are production code. */
internal class GlobeStub(
    private val fleet: List<SatelliteLiveState>,
    private val selected: String,
    private val track: Trail?,
    private val tokens: ZigSatTokens,
    private val fa: Boolean,
    private val members: Set<String>? = null,
    private val detail: (SatelliteItem) -> Unit = {},
) : GlobeSurface {
    @Composable
    override fun Draw(viewport: GlobeViewport) {
        val f = viewport.layout.frame
        Canvas(Modifier.fillMaxSize().testTag("globe_stub")) {
            val at = Offset(f.center.x * density, f.center.y * density)
            val r = f.diameter * density / 2
            if (r > 0) {
                drawCircle(Color(0xff527f90), r, at)
                val disc = Path().apply { addOval(Rect(at, r)) }
                clipPath(disc) {
                    drawOval(Color(0xff101626), Offset(at.x - .15f * r, at.y - 1.4f * r), Size(2 * r, 2.8f * r))
                }
            }
        }
        // A measurable node for Robolectric geometry checks; the Canvas remains the exact frame.
        val debug = LocalSatDebugBounds.current
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.align(androidx.compose.ui.AbsoluteAlignment.TopLeft)
                .absoluteOffset(x = (f.center.x - f.diameter / 2).dp, y = (f.center.y - f.diameter / 2).dp)
                .size(f.diameter.dp).testTag("globe_disc").drawBehind {
                    if (debug) drawRect(Color.Red, style = Stroke(width = 1.dp.toPx()))
                })
        }
        OrbitOverlay(
            viewport,
            Camera(0f, 0f, 3.22f),
            f,
            priorityFleet(fleet, selected, members, emptySet()),
            selected,
            members,
            track,
            emptyMap(),
            true,
            tokens,
            fa,
            detail,
        )
    }
}

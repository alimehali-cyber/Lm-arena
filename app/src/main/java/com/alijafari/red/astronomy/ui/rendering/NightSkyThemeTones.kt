package com.alijafari.red.astronomy.ui.rendering

import androidx.compose.ui.graphics.Color
import com.alijafari.red.astronomy.domain.SkyCanvasTheme

/**
 * The night each theme is set in, before the night's own airglow, light dome and moonlight are mixed
 * over it.
 *
 * A theme owns its night: Real Sky is blue-black, Monochrome is neutral grey-black, Observatory is
 * deep red (the dark-adapted eye), Watercolour is indigo, Papercraft is slate. The night model never
 * paints one hard-coded blue over all of them; it mixes each theme's own tone towards the physical
 * airglow, so the themes stay recognisable and the night stays real.
 */
object NightSkyThemeTones {

    /** The theme's night zenith tone. */
    fun skyTone(theme: SkyCanvasTheme): Color = when (theme) {
        SkyCanvasTheme.MONOCHROME_SCIENTIFIC -> Color(0xFF0A0A0C)
        SkyCanvasTheme.OBSERVATORY -> Color(0xFF1A0505)
        SkyCanvasTheme.KIDS_WATERCOLOR -> Color(0xFF1A1B4B)
        SkyCanvasTheme.PAPERCRAFT_DIORAMA -> Color(0xFF1F2432)
        else -> Color(0xFF050A16)
    }

    /** The theme's night horizon tone. Its warmth is what a light dome then builds on. */
    fun horizonTone(theme: SkyCanvasTheme): Color = when (theme) {
        SkyCanvasTheme.MONOCHROME_SCIENTIFIC -> Color(0xFF16161A)
        SkyCanvasTheme.OBSERVATORY -> Color(0xFF2A0808)
        SkyCanvasTheme.KIDS_WATERCOLOR -> Color(0xFF3B1F52)
        SkyCanvasTheme.PAPERCRAFT_DIORAMA -> Color(0xFF3D4A5D)
        else -> Color(0xFF101C33)
    }
}

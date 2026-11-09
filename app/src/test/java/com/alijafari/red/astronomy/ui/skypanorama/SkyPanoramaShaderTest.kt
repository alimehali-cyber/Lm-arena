package com.alijafari.red.astronomy.ui.skypanorama

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Structural checks for the GLSL composite; geometry and texture decoding have separate tests. */
class SkyPanoramaShaderTest {

    private val shader = SkyPanoramaShaders.FRAGMENT_SHADER

    @Test
    fun milkyWayLightIsScreenBlendedOverTheRealNightPalette() {
        assertTrue(shader.contains("vec3 screenOver(vec3 base, vec3 light)"))
        assertTrue(shader.contains("vec3 nightSky = screenOver(day, transmitted) * uVisibility;"))
        assertTrue(shader.contains("vec3 sky = mix(day, nightSky, uNightWeight);"))
        assertFalse(shader.contains("vec3 sky = mix(day, clamp(c, 0.0, 1.0), uNightWeight);"))
    }

    @Test
    fun originalCloudsAndClustersShineThroughWithoutAHardBandEdge() {
        // The same celestial texture remains the source; only its presentation is composited.
        assertTrue(shader.contains("textureLod(uPanorama, uv, uLod).rgb"))
        assertTrue(shader.contains("float cloudPresence = smoothstep(0.018, 0.14, photoLuma);"))
        assertTrue(shader.contains("vec3 coolStarlight = vec3(photoLuma) * vec3(0.72, 0.83, 1.0);"))
        assertTrue(shader.contains("warmCore * 0.65"))
        assertTrue(shader.contains("naturalColour * mix(0.08, 1.0, cloudPresence) * 0.92"))
    }
}

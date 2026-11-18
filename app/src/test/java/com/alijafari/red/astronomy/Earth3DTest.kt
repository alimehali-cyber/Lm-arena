package com.alijafari.red.astronomy

import androidx.compose.ui.geometry.Offset
import com.alijafari.red.astronomy.astro_engine.ISSEngine
import com.alijafari.red.astronomy.astro_engine.SatelliteCatalog
import com.alijafari.red.astronomy.astro_engine.SatelliteCategory
import com.alijafari.red.astronomy.astro_engine.SatelliteEngine
import com.alijafari.red.astronomy.astro_engine.SatelliteItem
import com.alijafari.red.astronomy.astro_engine.SatelliteLiveState
import com.alijafari.red.astronomy.astro_engine.SubSolarPoint
import com.alijafari.red.astronomy.domain.ThemeMode
import com.alijafari.red.astronomy.ui.components.SATELLITE_EARTH_DEFAULT_DISTANCE
import com.alijafari.red.astronomy.ui.components.buildSatelliteEarthStyle
import com.alijafari.red.astronomy.ui.components.findTappedSatellite3D
import com.alijafari.red.astronomy.ui.screens.MapTheme
import java.io.File
import java.security.MessageDigest
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards and mathematical verification for the native GLES 2.0 [Earth3D] renderer and the
 * [com.alijafari.red.astronomy.ui.components.SatelliteEarth3D] Satellites screen integration.
 */
class Earth3DTest {

    private data class ExpectedAsset(
        val name: String,
        val width: Int,
        val height: Int,
        val sha256: String,
    )

    private data class JpegHeader(
        val width: Int,
        val height: Int,
        val components: Int,
    )

    private fun appMainDir(): File {
        val candidates = listOf(
            File("src/main"),
            File("app/src/main"),
        )
        return candidates.firstOrNull { it.isDirectory }
            ?: error("Could not locate app/src/main from ${File(".").absolutePath}")
    }

    private fun readSource(relativePath: String): String {
        val file = File(appMainDir(), "java/$relativePath")
        assertTrue("Expected source file: ${file.path}", file.isFile)
        return file.readText()
    }

    @Test
    fun bundledEarthTextureAssetsArePresentValidRgbPowerOfTwoAndHashVerified() {
        val assetsDir = File(appMainDir(), "assets")
        val expected = listOf(
            ExpectedAsset(
                name = "earth_day_4k.jpg",
                width = 4096,
                height = 2048,
                sha256 = "cd7712d3e891425d9948ea5ac18d3bb1e7e834ea038188587897e689a7b35d0f",
            ),
            ExpectedAsset(
                name = "earth_night_2k.jpg",
                width = 2048,
                height = 1024,
                sha256 = "2912445dd071ef6796980cdce6fbecf0b0d7e911e47343d26c582ad9fdec9ef8",
            ),
            ExpectedAsset(
                name = "earth_clouds.jpg",
                width = 2048,
                height = 1024,
                sha256 = "6743d375184af6ff69bb5f75be1a2d2b969617ead89881260d5bb0b208435fcf",
            ),
            ExpectedAsset(
                name = "earth_bump_2k.jpg",
                width = 2048,
                height = 1024,
                sha256 = "4a40da0e5a992b83bc8d6055fd190f0b16cd9cb560e8adfcfa47dd86dfc32211",
            ),
        )
        assertEquals(expected.map { it.name }, Earth3DMath.REQUIRED_TEXTURE_ASSETS)

        for (spec in expected) {
            val file = File(assetsDir, spec.name)
            assertTrue("Missing Earth texture asset: ${spec.name}", file.isFile)
            val bytes = file.readBytes()
            assertTrue("Empty Earth texture asset: ${spec.name}", bytes.size > 16_384)
            assertEquals("Checksum mismatch for ${spec.name}", spec.sha256, sha256Hex(bytes))

            val header = parseJpegHeader(bytes)
            assertNotNull("Failed to parse JPEG header for ${spec.name}", header)
            val h = header!!
            assertEquals("Unexpected width for ${spec.name}", spec.width, h.width)
            assertEquals("Unexpected height for ${spec.name}", spec.height, h.height)
            assertEquals("Width must be power-of-two for GLES2 GL_REPEAT mipmaps: ${spec.name}", 1, h.width.countOneBits())
            assertEquals("Height must be power-of-two for GLES2 GL_REPEAT mipmaps: ${spec.name}", 1, h.height.countOneBits())
            assertEquals("Expected 3-channel RGB JPEG for ${spec.name}", 3, h.components)
        }
    }

    @Test
    fun earth3DIsHostedInTextureViewWithEgl14AndStrictGles2ShaderCompatibility() {
        val earthSource = readSource("com/alijafari/red/astronomy/Earth3D.kt")

        // In-window TextureView + EGL14 so overlays, popovers, nav bars, and squircle cards composite properly.
        assertTrue(earthSource.contains("TextureView"))
        assertTrue(earthSource.contains("EGL14.eglCreateContext"))
        assertTrue(earthSource.contains("isOpaque = false"))
        assertFalse("Must not use GLSurfaceView in scrolling Compose screens", earthSource.contains("GLSurfaceView"))
        assertFalse("Must not punch above Compose overlays", earthSource.contains("setZOrderOnTop"))
        assertFalse("Must not use MediaOverlay window layer", earthSource.contains("setZOrderMediaOverlay"))

        // Strict GLSL ES 1.00 rule: vertex and fragment shaders must never share uniform names
        // across different default float precisions.
        val vertexUniforms = extractUniformNames(earthSource, "VERTEX_SHADER")
        val earthFragUniforms = extractUniformNames(earthSource, "EARTH_FRAGMENT_SHADER")
        val cloudFragUniforms = extractUniformNames(earthSource, "CLOUD_FRAGMENT_SHADER")
        assertTrue(vertexUniforms.isNotEmpty())
        assertTrue(earthFragUniforms.isNotEmpty())
        assertTrue(cloudFragUniforms.isNotEmpty())
        assertTrue(
            "Shared VS/FS uniforms risk GLSL ES 1.00 precision link failure: ${vertexUniforms.intersect(earthFragUniforms)}",
            vertexUniforms.intersect(earthFragUniforms).isEmpty()
        )
        assertTrue(
            "Shared VS/FS uniforms risk GLSL ES 1.00 precision link failure: ${vertexUniforms.intersect(cloudFragUniforms)}",
            vertexUniforms.intersect(cloudFragUniforms).isEmpty()
        )
    }

    @Test
    fun earthStyleSanitizesRangesAndMapsAllSatelliteMapThemes() {
        val extreme = EarthStyle(
            rotationSpeed = 9f,
            ambientLight = -1f,
            cloudOpacity = 4f,
            exposureEV = 8f,
            saturation = -3f,
            cameraDistance = 0.5f,
        ).sanitized()
        assertEquals(1.2f, extreme.rotationSpeed, 1e-5f)
        assertEquals(0f, extreme.ambientLight, 1e-5f)
        assertEquals(1f, extreme.cloudOpacity, 1e-5f)
        assertEquals(2f, extreme.exposureEV, 1e-5f)
        assertEquals(0f, extreme.saturation, 1e-5f)
        assertEquals(2.25f, extreme.cameraDistance, 1e-5f)

        val subSolar = SubSolarPoint(latDeg = -18.75, lonDeg = 42.5, gmstDeg = 120.0)
        val darkStyle = buildSatelliteEarthStyle(MapTheme.DARK, ThemeMode.LIGHT, subSolar)
        val lightStyle = buildSatelliteEarthStyle(MapTheme.LIGHT, ThemeMode.DARK, subSolar)
        val autoWineStyle = buildSatelliteEarthStyle(MapTheme.AUTO, ThemeMode.WINE, subSolar)

        assertTrue(darkStyle.transparentBackground)
        assertFalse(darkStyle.autoRotate)
        assertTrue(darkStyle.showNightLights)
        assertTrue(darkStyle.showClouds)
        assertEquals(-18.75f, darkStyle.subSolarLatitudeDegrees ?: 0f, 1e-4f)
        assertEquals(42.5f, darkStyle.subSolarLongitudeDegrees ?: 0f, 1e-4f)
        assertEquals(SATELLITE_EARTH_DEFAULT_DISTANCE, darkStyle.cameraDistance, 1e-4f)

        assertTrue("Light map theme should raise ambient over Dark theme", lightStyle.ambientLight > darkStyle.ambientLight)
        assertTrue(autoWineStyle.transparentBackground)
        assertTrue(autoWineStyle.showNightLights)
        assertEquals("HUD theme must not recolor Earth", autoWineStyle,
            buildSatelliteEarthStyle(MapTheme.AUTO, ThemeMode.DARK, subSolar))
        assertEquals(autoWineStyle, buildSatelliteEarthStyle(MapTheme.AUTO, ThemeMode.LIGHT, subSolar))
    }

    @Test
    fun earthCameraAnglesAndProjectionCenterAnyTargetLatLonAccurately() {
        val viewportW = 1080f
        val viewportH = 930f
        val testSites = listOf(
            0.0 to 0.0,           // Gulf of Guinea (Prime Meridian / Equator)
            35.6892 to 51.3890,   // Tehran
            33.7490 to -84.3880,  // Atlanta
            -33.8688 to 151.2093, // Sydney
            35.6762 to 139.6503,  // Tokyo
            51.5072 to -0.1276,   // London
            0.0 to 180.0,         // International Date Line
        )

        for ((lat, lon) in testSites) {
            val (yaw, pitch) = Earth3DMath.cameraAnglesForLatLon(lat, lon)
            val camera = EarthCameraSnapshot(yawRad = yaw, pitchRad = pitch, distance = 3.22f)
            val centerProj = Earth3DMath.projectLatLon(lat, lon, 1f, camera, viewportW, viewportH)
            assertTrue("Expected ($lat, $lon) to be visible when focused", centerProj.visible)
            assertEquals("Expected centered X for ($lat, $lon)", viewportW * 0.5f, centerProj.screenX, 0.05f)
            assertEquals("Expected centered Y for ($lat, $lon)", viewportH * 0.5f, centerProj.screenY, 0.05f)
            assertEquals("Expected worldZ = +1 for centered surface point", 1f, centerProj.worldZ, 1e-4f)
            assertEquals(1f, centerProj.limbFade, 1e-4f)

            // Its antipode must be strictly occluded on Earth's far side.
            val antiLat = -lat
            val antiLon = if (lon >= 0.0) lon - 180.0 else lon + 180.0
            val antiProj = Earth3DMath.projectLatLon(antiLat, antiLon, 1f, camera, viewportW, viewportH)
            assertFalse("Antipode of ($lat, $lon) must be occluded", antiProj.visible)
            assertEquals(0f, antiProj.limbFade, 1e-4f)
        }

        // Heading east from centered Greenwich projects to +X (right), heading north projects to -Y (up).
        val (gYaw, gPitch) = Earth3DMath.cameraAnglesForLatLon(0.0, 0.0)
        val gCam = EarthCameraSnapshot(gYaw, gPitch, 3.22f)
        val east = Earth3DMath.projectLatLon(0.0, 20.0, 1f, gCam, viewportW, viewportH)
        val north = Earth3DMath.projectLatLon(20.0, 0.0, 1f, gCam, viewportW, viewportH)
        assertTrue(east.screenX > viewportW * 0.5f)
        assertTrue(north.screenY < viewportH * 0.5f)
    }

    @Test
    fun rotateWorldToModelInvertsRotateModelToWorldAndLocksAstronomicalSun() {
        val yaw = -1.17f
        val pitch = 0.63f
        val original = Earth3DMath.latLonToModel(23.4, -71.2, 1f)
        val world = Earth3DMath.rotateModelToWorld(original[0], original[1], original[2], yaw, pitch)
        val roundTrip = Earth3DMath.rotateWorldToModel(world[0], world[1], world[2], yaw, pitch)
        assertEquals(original[0], roundTrip[0], 1e-5f)
        assertEquals(original[1], roundTrip[1], 1e-5f)
        assertEquals(original[2], roundTrip[2], 1e-5f)

        // Astronomical sub-solar point: dot(modelSurface, sunModel) == dot(worldSurface, sunWorld) for every yaw/pitch.
        val style = EarthStyle(subSolarLatitudeDegrees = -19.1f, subSolarLongitudeDegrees = 105.4f)
        val sunModel = Earth3DMath.computeSunDirectionModel(style, yaw, pitch)
        val sunWorld = Earth3DMath.computeSunDirectionWorld(style, yaw, pitch)
        val sunMag = sqrt(sunModel[0] * sunModel[0] + sunModel[1] * sunModel[1] + sunModel[2] * sunModel[2])
        assertEquals(1f, sunMag, 1e-5f)

        val dotModel = original[0] * sunModel[0] + original[1] * sunModel[1] + original[2] * sunModel[2]
        val dotWorld = world[0] * sunWorld[0] + world[1] * sunWorld[1] + world[2] * sunWorld[2]
        assertEquals(dotWorld, dotModel, 1e-5f)

        // Sub-solar point itself gets maximum illumination (+1), its antipode gets deep night (-1).
        val subSurface = Earth3DMath.latLonToModel(-19.1, 105.4, 1f)
        val subDot = subSurface[0] * sunModel[0] + subSurface[1] * sunModel[1] + subSurface[2] * sunModel[2]
        assertEquals(1f, subDot, 1e-5f)

        // Standalone EarthStyle (no sub-solar override) also preserves dot products under camera rotation.
        val standalone = EarthStyle(sunAzimuthDegrees = 38f, sunElevationDegrees = 28f)
        val stModel = Earth3DMath.computeSunDirectionModel(standalone, yaw, pitch)
        val stWorld = Earth3DMath.computeSunDirectionWorld(standalone, yaw, pitch)
        val stDotModel = original[0] * stModel[0] + original[1] * stModel[1] + original[2] * stModel[2]
        val stDotWorld = world[0] * stWorld[0] + world[1] * stWorld[1] + world[2] * stWorld[2]
        assertEquals(stDotWorld, stDotModel, 1e-5f)
    }

    @Test
    fun shortestAngleDeltaAndCameraControllerRespectDatelineAndClamps() {
        val delta = Earth3DMath.shortestAngleDeltaRad(
            fromRad = (PI - 0.1).toFloat(),
            toRad = (-PI + 0.1).toFloat(),
        )
        assertEquals(0.2f, delta, 1e-4f)
        assertTrue(abs(delta) < 0.25f)

        val controller = EarthCameraController(
            initialYawRad = -0.5f,
            initialPitchRad = 0.2f,
            initialDistance = 3.22f,
        )
        controller.focusOn(latDeg = 35.6892, lonDeg = 51.3890, targetDistance = 2.8f)
        val (expectedYaw, expectedPitch) = Earth3DMath.cameraAnglesForLatLon(35.6892, 51.3890)
        assertEquals(expectedYaw, controller.snapshot.yawRad, 1e-5f)
        assertEquals(expectedPitch, controller.snapshot.pitchRad, 1e-5f)
        assertEquals(2.8f, controller.snapshot.distance, 1e-5f)

        // Extreme values clamp cleanly.
        controller.setCamera(newYawRad = 0f, newPitchRad = 9f, newDistance = 99f, markUserZoomed = true)
        assertEquals(Earth3DMath.MAX_PITCH_RAD, controller.snapshot.pitchRad, 1e-5f)
        assertEquals(Earth3DMath.MAX_ZOOM_DISTANCE, controller.snapshot.distance, 1e-5f)
        assertTrue(controller.userZoomed)

        controller.resetTo(latDeg = 30.1141, lonDeg = 51.5217, fitDistance = SATELLITE_EARTH_DEFAULT_DISTANCE)
        val (homeYaw, homePitch) = Earth3DMath.cameraAnglesForLatLon(30.1141, 51.5217)
        assertEquals(homeYaw, controller.snapshot.yawRad, 1e-5f)
        assertEquals(homePitch, controller.snapshot.pitchRad, 1e-5f)
        assertEquals(SATELLITE_EARTH_DEFAULT_DISTANCE, controller.snapshot.distance, 1e-5f)
        assertFalse(controller.userZoomed)
    }

    @Test
    fun satelliteTapHitTestingSelectsNearSideAndRejectsFarSideOccludedSatellites() {
        val (yaw, pitch) = Earth3DMath.cameraAnglesForLatLon(0.0, 0.0)
        val camera = EarthCameraSnapshot(yaw, pitch, 3.22f)
        val w = 1000f
        val h = 860f

        // Far-side satellite at (0°, 180°, 420 km) projects straight through screen center in 2D,
        // yet is hidden behind Earth and MUST never steal taps at screen center.
        val farSideSat = fakeLiveState("far_sat", latDeg = 0.0, lonDeg = 180.0, altKm = 420.0)
        assertNull(
            findTappedSatellite3D(
                tapOffset = Offset(w * 0.5f, h * 0.5f),
                candidates = listOf(farSideSat),
                camera = camera,
                viewportWidth = w,
                viewportHeight = h,
                hitRadiusPx = 64f,
            )
        )

        // Near-side satellite at (12°, 18°, 420 km) is cleanly picked when tapped near its projection.
        val nearSideSat = fakeLiveState("iss_zarya", latDeg = 12.0, lonDeg = 18.0, altKm = 420.0)
        val nearProj = Earth3DMath.projectLatLon(
            latDeg = 12.0,
            lonDeg = 18.0,
            radius = Earth3DMath.altitudeKmToRadius(420.0),
            camera = camera,
            viewportWidth = w,
            viewportHeight = h,
        )
        assertTrue(nearProj.visible)
        val picked = findTappedSatellite3D(
            tapOffset = Offset(nearProj.screenX + 8f, nearProj.screenY - 6f),
            candidates = listOf(farSideSat, nearSideSat),
            camera = camera,
            viewportWidth = w,
            viewportHeight = h,
            hitRadiusPx = 48f,
        )
        assertEquals("iss_zarya", picked?.satellite?.id)

        // Tapping empty ocean away from satellites returns null (to dismiss anchored popover).
        assertNull(
            findTappedSatellite3D(
                tapOffset = Offset(w * 0.5f, h * 0.5f),
                candidates = listOf(farSideSat, nearSideSat),
                camera = camera,
                viewportWidth = w,
                viewportHeight = h,
                hitRadiusPx = 48f,
            )
        )
    }

    @Test
    fun calculateOrbitTrack3DProducesPastNowAndFutureLeoPointsWithAltitude() {
        val iss = SatelliteCatalog.satellites.first { it.id == "iss_zarya" }
        // 2026-11-13 12:00:00 UTC (right at bundled fallback TLE epoch 26317.5).
        val nowMs = 1_794_571_200_000L
        val track = SatelliteEngine.calculateOrbitTrack3D(
            satellite = iss,
            currentTimestampMs = nowMs,
            stepSeconds = 60,
            tle = iss.defaultTle,
        )

        assertTrue("Expected full past+future orbital track samples", track.size >= 135)
        assertTrue("Expected past samples before nowMs", track.any { it.timestampMs < nowMs })
        assertTrue("Expected exact current-time sample", track.any { it.timestampMs == nowMs })
        assertTrue("Expected future samples after nowMs", track.any { it.timestampMs > nowMs })
        assertTrue(
            "All ISS orbit track samples must carry valid geographic coords and positive LEO altitude",
            track.all { p ->
                p.latDeg in -90.0..90.0 &&
                    p.lonDeg in -180.0..180.0 &&
                    p.altKm in 250.0..800.0
            }
        )
    }

    @Test
    fun allThreeSatellitesScreensPreserveEveryMapFeatureOnSatelliteEarth3D() {
        val overlay = readSource("com/alijafari/red/astronomy/ui/components/SatelliteEarth3D.kt")
        val orbit = readSource("com/alijafari/red/astronomy/ui/satellites/OrbitOverlay.kt")
        val stage = readSource("com/alijafari/red/astronomy/ui/satellites/SatellitesStage.kt")
        val hud = readSource("com/alijafari/red/astronomy/ui/satellites/SatelliteHud.kt")
        val components = readSource("com/alijafari/red/astronomy/ui/satellites/SatelliteComponents.kt")
        val mapPanel = readSource("com/alijafari/red/astronomy/ui/satellites/MapPanel.kt")
        assertTrue(overlay.contains("Earth3D("))
        assertTrue(overlay.contains("testTag(\"satellite_earth_3d\")"))
        assertTrue(orbit.contains("testTag(\"anchored_sat_label\")"))
        assertTrue(overlay.contains("SatelliteEngine.calculateSubSolarPoint"))
        assertTrue(overlay.contains("SatelliteEngine.majorCityLights"))
        assertTrue(overlay.contains("sampleTrail("))
        assertTrue(overlay.contains("trainMembers.forEach"))
        assertTrue(overlay.contains("priorityFleet("))
        for (layer in listOf("GRID", "EQUATOR", "CITIES", "OBSERVER", "ORBIT", "TRAIN", "TETHER")) {
            assertTrue("native globe uses $layer", overlay.contains("MapLayer.$layer"))
        }
        assertTrue("the stage renders the globe", stage.contains("SatelliteEarth3D("))
        assertTrue("with a coherent live simulation batch", stage.contains("batch.first.takeIf"))
        assertTrue("the map theme toggle", stage.contains("mapTheme = mapTheme"))
        assertTrue("the map theme selector", mapPanel.contains("testTag(\"map_theme_selector\")"))
        assertTrue("Locate is the only reset", hud.contains("testTag=\"map_reset_view\""))
        assertTrue("the time scrubber", components.contains("testTag(\"time_scrubber_slider\")"))
        assertTrue("the status chip", components.contains("testTag(\"satellite_status_chip\")"))
        assertTrue("satellite focus wiring", stage.contains("focusTarget = earthFocusTarget"))
        assertTrue("notification deep links", stage.contains("uiState.selectedSatelliteId"))
        assertTrue("detail opens deliberately", stage.contains("onOpenSatelliteDetail"))

        val screens = mapOf(
            "ISSScreen" to (readSource("com/alijafari/red/astronomy/ui/screens/ISSScreen.kt") to "SatSkin.DEFAULT"),
            "SpatialSatellitesScreen" to (readSource("com/alijafari/red/astronomy/ui/spatial/SpatialSatellitesScreen.kt") to "SatSkin.SPATIAL"),
            "AetherSatellitesScreen" to (readSource("com/alijafari/red/astronomy/ui/aether/AetherSatellitesScreen.kt") to "SatSkin.AETHER"),
        )
        screens.forEach { (name, pair) ->
            val (src, skin) = pair
            assertTrue("$name must render the shared stage", src.contains("SatellitesStage("))
            assertTrue("$name must declare its skin", src.contains(skin))
        }
    }

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }

    private fun parseJpegHeader(bytes: ByteArray): JpegHeader? {
        if (bytes.size < 4) return null
        if ((bytes[0].toInt() and 0xFF) != 0xFF || (bytes[1].toInt() and 0xFF) != 0xD8) return null
        var i = 2
        while (i + 3 < bytes.size) {
            if ((bytes[i].toInt() and 0xFF) != 0xFF) {
                i++
                continue
            }
            while (i < bytes.size && (bytes[i].toInt() and 0xFF) == 0xFF) i++
            if (i >= bytes.size) break
            val marker = bytes[i].toInt() and 0xFF
            i++
            if (marker == 0xD8 || marker == 0xD9 || marker in 0xD0..0xD7 || marker == 0x01) continue
            if (i + 1 >= bytes.size) break
            val segLen = ((bytes[i].toInt() and 0xFF) shl 8) or (bytes[i + 1].toInt() and 0xFF)
            if (segLen < 2 || i + segLen > bytes.size) break
            // Baseline (SOF0 = 0xC0) or progressive (SOF2 = 0xC2) DCT frame header
            if (marker == 0xC0 || marker == 0xC2) {
                if (segLen < 8) return null
                val height = ((bytes[i + 3].toInt() and 0xFF) shl 8) or (bytes[i + 4].toInt() and 0xFF)
                val width = ((bytes[i + 5].toInt() and 0xFF) shl 8) or (bytes[i + 6].toInt() and 0xFF)
                val components = bytes[i + 7].toInt() and 0xFF
                return JpegHeader(width = width, height = height, components = components)
            }
            i += segLen
        }
        return null
    }

    private fun extractUniformNames(source: String, shaderConst: String): Set<String> {
        val start = source.indexOf("private const val $shaderConst")
        assertTrue("Missing shader constant $shaderConst", start >= 0)
        val openQuote = source.indexOf("\"\"\"", start)
        val closeQuote = source.indexOf("\"\"\"", openQuote + 3)
        val shader = source.substring(openQuote + 3, closeQuote)
        return Regex("""uniform\s+\w+\s+(\w+)\s*;""")
            .findAll(shader)
            .map { it.groupValues[1] }
            .toSet()
    }

    private fun fakeLiveState(
        id: String,
        latDeg: Double,
        lonDeg: Double,
        altKm: Double,
    ): SatelliteLiveState {
        val item = SatelliteItem(
            id = id,
            noradId = 25544,
            nameEn = id,
            nameFa = id,
            category = SatelliteCategory.ISS,
            defaultTle = SatelliteCatalog.satellites.first().defaultTle,
        )
        return SatelliteLiveState(
            satellite = item,
            topocentric = ISSEngine.TopocentricPosition(
                elevationDeg = 45.0,
                azimuthDeg = 180.0,
                rangeKm = 600.0,
                subLatDeg = latDeg,
                subLonDeg = lonDeg,
                satAltKm = altKm,
                velocityKmS = 7.66,
                isSunlit = true,
                xEcef = 0.0,
                yEcef = 0.0,
                zEcef = 0.0,
            ),
            apparentMagnitude = -3.2,
            isNakedEyeVisible = true,
            visibilityVerdictEn = "Visible",
            visibilityVerdictFa = "مرئی",
            reasonEn = "",
            reasonFa = "",
        )
    }
}

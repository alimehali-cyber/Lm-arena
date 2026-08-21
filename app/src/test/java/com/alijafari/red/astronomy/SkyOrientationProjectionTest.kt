package com.alijafari.red.astronomy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.*

class SkyOrientationProjectionTest {

    data class ProjectionResult(val x: Float, val y: Float, val isVisible: Boolean)

    /**
     * Exact 3D pinhole camera projection of celestial object onto screen coordinates.
     */
    fun projectCelestialObject(
        azimuthDeg: Double,
        altitudeDeg: Double,
        rotationMatrix: FloatArray,
        canvasWidth: Float,
        canvasHeight: Float,
        fovXDeg: Double
    ): ProjectionResult? {
        val azRad = Math.toRadians(azimuthDeg)
        val altRad = Math.toRadians(altitudeDeg)

        // Celestial unit vector in World frame (East = +X, North = +Y, Up = +Z)
        val ox = cos(altRad) * sin(azRad)
        val oy = cos(altRad) * cos(azRad)
        val oz = sin(altRad)

        // Transform into Device Camera frame:
        // Xc: right on screen, Yc: up on screen, Zc: front of screen
        val xc = ox * rotationMatrix[0] + oy * rotationMatrix[3] + oz * rotationMatrix[6]
        val yc = ox * rotationMatrix[1] + oy * rotationMatrix[4] + oz * rotationMatrix[7]
        val zc = ox * rotationMatrix[2] + oy * rotationMatrix[5] + oz * rotationMatrix[8]

        // Depth in front of camera
        val depth = -zc

        if (depth <= 0.001) {
            return null // Behind the camera
        }

        val fovXRad = Math.toRadians(fovXDeg)
        val focalLength = (canvasWidth / 2.0) / tan(fovXRad / 2.0)

        val px = (canvasWidth / 2.0 + (xc / depth) * focalLength).toFloat()
        val py = (canvasHeight / 2.0 - (yc / depth) * focalLength).toFloat()

        return ProjectionResult(px, py, isVisible = true)
    }

    /**
     * Constructs a rotation matrix for a given camera orientation (Azimuth, Altitude, Roll).
     */
    fun createRotationMatrix(azimuthDeg: Double, altitudeDeg: Double, rollDeg: Double = 0.0): FloatArray {
        val azRad = Math.toRadians(azimuthDeg)
        val altRad = Math.toRadians(altitudeDeg)
        val rollRad = Math.toRadians(rollDeg)

        // Camera pointing vector (out of rear camera)
        val px = cos(altRad) * sin(azRad)
        val py = cos(altRad) * cos(azRad)
        val pz = sin(altRad)

        // Sky Right & Up vectors
        val rx0 = cos(azRad)
        val ry0 = -sin(azRad)
        val rz0 = 0.0

        val ux0 = -sin(altRad) * sin(azRad)
        val uy0 = -sin(altRad) * cos(azRad)
        val uz0 = cos(altRad)

        // Apply roll
        val cosR = cos(rollRad)
        val sinR = sin(rollRad)

        val rx = (rx0 * cosR - ux0 * sinR).toFloat()
        val ry = (ry0 * cosR - uy0 * sinR).toFloat()
        val rz = (rz0 * cosR - uz0 * sinR).toFloat()

        val ux = (rx0 * sinR + ux0 * cosR).toFloat()
        val uy = (ry0 * sinR + uy0 * cosR).toFloat()
        val uz = (rz0 * sinR + uz0 * cosR).toFloat()

        // Device front is -pointing vector
        val fx = (-px).toFloat()
        val fy = (-py).toFloat()
        val fz = (-pz).toFloat()

        return floatArrayOf(
            rx, ux, fx,
            ry, uy, fy,
            rz, uz, fz
        )
    }

    @Test
    fun `test exact center alignment when camera points directly at object`() {
        val width = 1080f
        val height = 2400f
        val fov = 60.0

        val targetAz = 120.0
        val targetAlt = 45.0

        val rotMat = createRotationMatrix(targetAz, targetAlt, 0.0)
        val proj = projectCelestialObject(targetAz, targetAlt, rotMat, width, height, fov)

        assertNotNull(proj)
        assertEquals(width / 2f, proj!!.x, 0.01f)
        assertEquals(height / 2f, proj.y, 0.01f)
    }

    @Test
    fun `test object to the right projects to screen right`() {
        val width = 1080f
        val height = 2400f
        val fov = 60.0

        val cameraAz = 0.0
        val cameraAlt = 30.0

        val targetAz = 5.0 // 5 degrees East of camera
        val targetAlt = 30.0

        val rotMat = createRotationMatrix(cameraAz, cameraAlt, 0.0)
        val proj = projectCelestialObject(targetAz, targetAlt, rotMat, width, height, fov)

        assertNotNull(proj)
        assertTrue("Projected X should be right of center", proj!!.x > width / 2f)
        assertTrue("Projected Y should be near center vertical", abs(proj.y - height / 2f) < 2.0f)
    }

    @Test
    fun `test high altitude object scaling prevents rightward offset distortion`() {
        val width = 1080f
        val height = 2400f
        val fov = 60.0

        // At 60° altitude, 2° azimuth delta is 1° true sky angular separation
        val cameraAz = 180.0
        val cameraAlt = 60.0

        val targetAz = 182.0
        val targetAlt = 60.0

        val rotMat = createRotationMatrix(cameraAz, cameraAlt, 0.0)
        val proj = projectCelestialObject(targetAz, targetAlt, rotMat, width, height, fov)

        assertNotNull(proj)
        // Focal length for 60° FOV: f = (1080 / 2) / tan(30°) = 540 / 0.57735 = 935.3 pixels
        // Angular offset = 2° * cos(60°) = 1° = 0.017453 rad
        // Expected displacement = tan(1°) * 935.3 ≈ 16.32 pixels
        val displacement = proj!!.x - (width / 2f)
        assertEquals(16.32f, displacement, 0.5f)
    }

    @Test
    fun `test object behind camera is not rendered`() {
        val width = 1080f
        val height = 2400f
        val fov = 60.0

        val cameraAz = 0.0
        val cameraAlt = 0.0

        val targetAz = 180.0 // Opposite direction
        val targetAlt = 0.0

        val rotMat = createRotationMatrix(cameraAz, cameraAlt, 0.0)
        val proj = projectCelestialObject(targetAz, targetAlt, rotMat, width, height, fov)

        assertEquals(null, proj)
    }

    @Test
    fun `test ARProjectionEngine cardinal directions with phone 90-degree sensor orientation`() {
        val width = 1080f
        val height = 2400f
        val focalLengthPx = 1000f

        val intrinsics = com.alijafari.red.astronomy.astro_engine.ARProjectionEngine.CameraIntrinsics(
            fx = focalLengthPx.toDouble(),
            fy = focalLengthPx.toDouble(),
            cx = 1080.0 / 2.0, // active array center
            cy = 2400.0 / 2.0,
            skew = 0.0,
            activeArrayWidth = 2400,
            activeArrayHeight = 1080,
            sensorOrientation = 90,
            isLensFacingBack = true,
            source = com.alijafari.red.astronomy.astro_engine.ARProjectionEngine.IntrinsicsSource.ESTIMATED_PHYSICAL_SENSOR
        )

        val cameraAz = 0.0 // facing North
        val cameraAlt = 30.0 // looking 30 deg above horizon
        val rotMat = createRotationMatrix(cameraAz, cameraAlt, 0.0)

        // 1. Center target (directly at crosshair)
        val centerPt = com.alijafari.red.astronomy.astro_engine.ARProjectionEngine.projectAltAz(
            azimuthDeg = cameraAz,
            altitudeDeg = cameraAlt,
            rotationMatrix = rotMat,
            currentAzimuth = cameraAz,
            currentAltitude = cameraAlt,
            currentRoll = 0.0,
            canvasWidth = width,
            canvasHeight = height,
            intrinsics = intrinsics,
            zoomFactor = 1.0f,
            sensorToViewMatrix = null,
            displayRotationDegrees = 0
        )
        assertNotNull(centerPt)
        assertEquals(width / 2f, centerPt!!.x, 1.0f)
        assertEquals(height / 2f, centerPt.y, 1.0f)

        // 2. Object UP (higher altitude: 35 deg > 30 deg)
        val upPt = com.alijafari.red.astronomy.astro_engine.ARProjectionEngine.projectAltAz(
            azimuthDeg = cameraAz,
            altitudeDeg = 35.0,
            rotationMatrix = rotMat,
            currentAzimuth = cameraAz,
            currentAltitude = cameraAlt,
            currentRoll = 0.0,
            canvasWidth = width,
            canvasHeight = height,
            intrinsics = intrinsics,
            zoomFactor = 1.0f,
            sensorToViewMatrix = null,
            displayRotationDegrees = 0
        )
        assertNotNull(upPt)
        assertTrue("Higher altitude object must be above center (py < height/2)", upPt!!.y < height / 2f)
        assertEquals(width / 2f, upPt.x, 1.0f)

        // 3. Object DOWN (lower altitude: 25 deg < 30 deg)
        val downPt = com.alijafari.red.astronomy.astro_engine.ARProjectionEngine.projectAltAz(
            azimuthDeg = cameraAz,
            altitudeDeg = 25.0,
            rotationMatrix = rotMat,
            currentAzimuth = cameraAz,
            currentAltitude = cameraAlt,
            currentRoll = 0.0,
            canvasWidth = width,
            canvasHeight = height,
            intrinsics = intrinsics,
            zoomFactor = 1.0f,
            sensorToViewMatrix = null,
            displayRotationDegrees = 0
        )
        assertNotNull(downPt)
        assertTrue("Lower altitude object must be below center (py > height/2)", downPt!!.y > height / 2f)
        assertEquals(width / 2f, downPt.x, 1.0f)

        // 4. Object RIGHT (East of camera: 5 deg > 0 deg)
        val rightPt = com.alijafari.red.astronomy.astro_engine.ARProjectionEngine.projectAltAz(
            azimuthDeg = 5.0,
            altitudeDeg = cameraAlt,
            rotationMatrix = rotMat,
            currentAzimuth = cameraAz,
            currentAltitude = cameraAlt,
            currentRoll = 0.0,
            canvasWidth = width,
            canvasHeight = height,
            intrinsics = intrinsics,
            zoomFactor = 1.0f,
            sensorToViewMatrix = null,
            displayRotationDegrees = 0
        )
        assertNotNull(rightPt)
        assertTrue("Object to the right must project right of center (px > width/2)", rightPt!!.x > width / 2f)
        assertEquals(height / 2f, rightPt.y, 1.0f)

        // 5. Object LEFT (West of camera: 355 deg < 360/0 deg)
        val leftPt = com.alijafari.red.astronomy.astro_engine.ARProjectionEngine.projectAltAz(
            azimuthDeg = 355.0,
            altitudeDeg = cameraAlt,
            rotationMatrix = rotMat,
            currentAzimuth = cameraAz,
            currentAltitude = cameraAlt,
            currentRoll = 0.0,
            canvasWidth = width,
            canvasHeight = height,
            intrinsics = intrinsics,
            zoomFactor = 1.0f,
            sensorToViewMatrix = null,
            displayRotationDegrees = 0
        )
        assertNotNull(leftPt)
        assertTrue("Object to the left must project left of center (px < width/2)", leftPt!!.x < width / 2f)
        assertEquals(height / 2f, leftPt.y, 1.0f)
    }
}

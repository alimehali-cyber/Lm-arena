package com.alijafari.red.astronomy.sandbox.render.relativistic

import android.opengl.GLES30
import android.opengl.Matrix
import android.util.Log
import com.alijafari.red.astronomy.sandbox.model.SandboxBodyType
import com.alijafari.red.astronomy.sandbox.physics.AstroPhysicsConstants
import com.alijafari.red.astronomy.sandbox.render.camera.SandboxCamera
import com.alijafari.red.astronomy.sandbox.render.geometry.SphereMesh
import com.alijafari.red.astronomy.sandbox.render.gl.QualityLevel
import com.alijafari.red.astronomy.sandbox.render.gl.ShaderProgram
import com.alijafari.red.astronomy.sandbox.render.scale.RenderScaleManager
import com.alijafari.red.astronomy.sandbox.render.shaders.RelativisticShaderSources
import com.alijafari.red.astronomy.sandbox.snapshot.BodyRenderState
import kotlin.math.sqrt

/**
 * High-performance, physically grounded Relativistic Renderer for Black Holes and Wormholes (Phase 5).
 *
 * Implements:
 * - General Relativistic ray-marching in curved spacetimes (Schwarzschild & Kerr).
 * - Black hole shadow with critical impact parameter (b_crit ~ 2.598 r_s).
 * - Multi-order lensed accretion disks with Novikov-Thorne temperature gradient.
 * - Relativistic Doppler beaming and gravitational redshift.
 * - Kerr frame-dragging (Lense-Thirring effect).
 * - Morris-Thorne traversible wormhole portal rendering into alternate celestial universe.
 */
class RelativisticBodyRenderer {

    companion object {
        private const val TAG = "RelativisticRenderer"
    }

    var isEnabled: Boolean = true
    var qualityLevel: QualityLevel = QualityLevel.HIGH

    // Shader Programs
    private var blackHoleShader: ShaderProgram? = null
    private var wormholeShader: ShaderProgram? = null

    // Proxy Bounding Geometry for volumetric raymarching
    private var boundingSphereMesh: SphereMesh? = null

    // Scratch buffers for zero-allocation per frame
    private val modelMatrix = FloatArray(16)
    private val tempVec3 = FloatArray(3)

    fun init() {
        Log.i(TAG, "Compiling Relativistic Black Hole & Wormhole Shader Pipeline...")

        blackHoleShader = ShaderProgram.build(
            RelativisticShaderSources.BLACK_HOLE_VERTEX_SHADER,
            RelativisticShaderSources.BLACK_HOLE_FRAGMENT_SHADER
        )

        wormholeShader = ShaderProgram.build(
            RelativisticShaderSources.WORMHOLE_VERTEX_SHADER,
            RelativisticShaderSources.WORMHOLE_FRAGMENT_SHADER
        )

        boundingSphereMesh = SphereMesh(rings = 24, sectors = 24).apply { init() }
        Log.i(TAG, "Relativistic Shader Pipeline Initialized Successfully.")
    }

    /**
     * Renders all active black holes and theoretical wormholes in the current frame.
     */
    fun draw(
        camera: SandboxCamera,
        scaleManager: RenderScaleManager,
        bodies: List<BodyRenderState>,
        bodyPositions: FloatArray,
        bodyRadii: FloatArray,
        simTimeSeconds: Double
    ): Int {
        if (!isEnabled || bodies.isEmpty()) return 0
        var drawCalls = 0

        val qualityTier = when (qualityLevel) {
            QualityLevel.LOW -> 0.0f
            QualityLevel.MEDIUM -> 1.0f
            QualityLevel.HIGH -> 2.0f
        }

        // Disable backface culling to allow seamless camera fly-through near or inside bounding volume
        GLES30.glDisable(GLES30.GL_CULL_FACE)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        GLES30.glDepthMask(true)

        val activeCount = bodies.size.coerceAtMost(20)

        for (i in 0 until activeCount) {
            val b = bodies[i]
            val type = b.type

            if (type != SandboxBodyType.BLACK_HOLE && type != SandboxBodyType.THEORETICAL_WORMHOLE) {
                continue
            }

            val idx = i * 3
            val cx = bodyPositions[idx]
            val cy = bodyPositions[idx + 1]
            val cz = bodyPositions[idx + 2]
            val renderR = bodyRadii[i]

            // Calculate physical Schwarzschild radius: r_s = 2GM / c^2
            val physicalRs = if (b.massKg > 0.0) {
                (2.0 * AstroPhysicsConstants.G * b.massKg) /
                        (AstroPhysicsConstants.SPEED_OF_LIGHT * AstroPhysicsConstants.SPEED_OF_LIGHT)
            } else {
                2.953e4
            }

            val renderRs = (renderR * 0.45f).coerceAtLeast(0.15f)

            if (type == SandboxBodyType.BLACK_HOLE && blackHoleShader != null && boundingSphereMesh != null) {
                blackHoleShader?.use()

                // Spin parameter: check if Kerr rotating black hole (e.g. if name contains Kerr or custom metadata)
                val isKerr = b.nameEn.contains("Kerr", ignoreCase = true) ||
                        b.nameFa.contains("کِر", ignoreCase = true) ||
                        b.id.contains("kerr", ignoreCase = true)
                val spinA = if (isKerr) 0.94f else 0.0f

                val iscoRadius = if (isKerr) renderRs * 1.5f else renderRs * 3.0f
                val outerDiskRadius = renderRs * 7.5f
                val boundingRadius = outerDiskRadius * 1.85f

                // Setup bounding volume model matrix
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, cx, cy, cz)
                Matrix.scaleM(modelMatrix, 0, boundingRadius, boundingRadius, boundingRadius)

                blackHoleShader?.setUniformMatrix4fv("u_ViewProjectionMatrix", camera.viewProjectionMatrix)
                blackHoleShader?.setUniformMatrix4fv("u_ModelMatrix", modelMatrix)
                blackHoleShader?.setUniform3f("u_CameraPosition", camera.eyeX, camera.eyeY, camera.eyeZ)
                blackHoleShader?.setUniform3f("u_BlackHoleCenter", cx, cy, cz)
                blackHoleShader?.setUniform1f("u_SchwarzschildRadius", renderRs)
                blackHoleShader?.setUniform1f("u_SpinParameter", spinA)
                blackHoleShader?.setUniform3f("u_SpinAxis", 0.0f, 1.0f, 0.0f)
                blackHoleShader?.setUniform1f("u_DiskInnerRadius", iscoRadius)
                blackHoleShader?.setUniform1f("u_DiskOuterRadius", outerDiskRadius)
                blackHoleShader?.setUniform1f("u_DiskBaseTemperature", if (isKerr) 32000.0f else 18000.0f)
                blackHoleShader?.setUniform1f("u_DiskDensityScale", 1.0f)
                blackHoleShader?.setUniform1f("u_SimTime", simTimeSeconds.toFloat())
                blackHoleShader?.setUniform1f("u_QualityTier", qualityTier)

                boundingSphereMesh?.draw()
                drawCalls++
            } else if (type == SandboxBodyType.THEORETICAL_WORMHOLE && wormholeShader != null && boundingSphereMesh != null) {
                wormholeShader?.use()

                val throatRadius = renderRs * 1.25f
                val boundingRadius = throatRadius * 3.5f

                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, cx, cy, cz)
                Matrix.scaleM(modelMatrix, 0, boundingRadius, boundingRadius, boundingRadius)

                wormholeShader?.setUniformMatrix4fv("u_ViewProjectionMatrix", camera.viewProjectionMatrix)
                wormholeShader?.setUniformMatrix4fv("u_ModelMatrix", modelMatrix)
                wormholeShader?.setUniform3f("u_CameraPosition", camera.eyeX, camera.eyeY, camera.eyeZ)
                wormholeShader?.setUniform3f("u_WormholeCenter", cx, cy, cz)
                wormholeShader?.setUniform1f("u_ThroatRadius", throatRadius)
                wormholeShader?.setUniform1f("u_ThroatLength", throatRadius * 2.0f)
                wormholeShader?.setUniform1f("u_SimTime", simTimeSeconds.toFloat())
                wormholeShader?.setUniform1f("u_QualityTier", qualityTier)

                boundingSphereMesh?.draw()
                drawCalls++
            }
        }

        GLES30.glEnable(GLES30.GL_CULL_FACE)
        return drawCalls
    }

    fun destroy() {
        blackHoleShader?.destroy()
        wormholeShader?.destroy()
        boundingSphereMesh?.destroy()
        blackHoleShader = null
        wormholeShader = null
        boundingSphereMesh = null
    }
}

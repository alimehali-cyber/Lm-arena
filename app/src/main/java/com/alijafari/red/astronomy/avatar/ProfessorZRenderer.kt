package com.alijafari.red.astronomy.avatar

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES30
import android.opengl.GLUtils
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer

enum class ProfessorZStatus {
    LOADING,
    READY,
    UNAVAILABLE,
}

data class ProfessorZSceneState(
    val sunAltitudeDeg: Double,
    val moonGlowIntensity: Float,
    val figureOnRight: Boolean,
    val persian: Boolean,
    val tapCounter: Int,
)

/**
 * Dedicated `HandlerThread` OpenGL ES 3.0 renderer for Professor Z (`professor_z.glb` / `professor_z_lod1.glb`).
 *
 * Uses pure `EGL14` + `GLES30` with premultiplied RGBA8 + 16-bit depth (`EGL_ALPHA_SIZE = 8`,
 * `EGL_DEPTH_SIZE = 16`), pure Kotlin only, and degrades seamlessly to `LOD1` or ZIG's
 * 2D `ObserverRenderer` silhouette if GLES 3.0 initialization fails.
 */
class ProfessorZRenderer(
    private val appContext: Context,
    private val listener: Listener,
) {

    interface Listener {
        fun onFrameShown()
        fun onGlFailure(reason: String)
    }

    private class GpuPrimitive(
        val data: MeshPrimitiveData,
        val vao: Int,
        val posVbo: Int,
        val nrmVbo: Int,
        val uvVbo: Int,
        val jntVbo: Int,
        val wgtVbo: Int,
        val ebo: Int,
        val indexCount: Int,
        val workPositions: FloatArray,
        val workPosBuffer: FloatBuffer,
    )

    private val thread = HandlerThread("ProfessorZGl").apply { start() }
    private val handler = Handler(thread.looper)

    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
    private var surfaceTexture: SurfaceTexture? = null

    private var avatarProgram = 0
    private var shadowProgram = 0
    private var shadowVao = 0
    private var shadowVbo = 0
    private var albedoTex = 0
    private var ormTex = 0

    private var uViewProj = -1
    private var uModel = -1
    private var uJoints = -1
    private var uAlbedoTex = -1
    private var uOrmTex = -1
    private var uCameraPos = -1
    private var uKeyLightDir = -1
    private var uKeyLightColor = -1
    private var uSkyZenithColor = -1
    private var uGroundBounceColor = -1
    private var uRimColor = -1

    private var uShadowViewProj = -1
    private var uShadowCenterXZ = -1
    private var uShadowRadiusXZ = -1
    private var uShadowAlpha = -1

    private val gpuPrimitives = ArrayList<GpuPrimitive>()
    private var animator: AvatarAnimator? = null
    private var usingLod1 = false
    private var slowFrameStreak = 0
    private var lastAppliedMorphHash = 0

    private var viewportWidth = 0
    private var viewportHeight = 0
    private var sceneState = ProfessorZSceneState(
        sunAltitudeDeg = -18.0,
        moonGlowIntensity = 0.35f,
        figureOnRight = false, // Professor Z is anchored to the LEFT of the Home Sky canvas.
        persian = false,
        tapCounter = 0,
    )
    private var lastHandledTapCounter = 0
    private var running = false
    private var frameShownReported = false
    private var startElapsedMs = 0L
    private var lastFrameElapsedMs = 0L

    private val viewProjMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)

    private val frameTick = object : Runnable {
        override fun run() {
            if (!running) return
            val t0 = SystemClock.elapsedRealtime()
            drawFrame(t0)
            val frameCostMs = SystemClock.elapsedRealtime() - t0
            if (!usingLod1 && frameCostMs > 28L) {
                slowFrameStreak++
                if (slowFrameStreak >= 12) {
                    slowFrameStreak = 0
                    switchToLod1()
                }
            } else {
                slowFrameStreak = 0
            }
            if (running) {
                handler.postDelayed(this, FRAME_INTERVAL_MS)
            }
        }
    }

    fun attach(surface: SurfaceTexture, width: Int, height: Int) {
        handler.post {
            surfaceTexture = surface
            viewportWidth = width
            viewportHeight = height
            try {
                initEgl(surface)
                initPrograms()
                initShadowQuad()
                loadAvatarGpuAssets(lod1 = false)
                startElapsedMs = SystemClock.elapsedRealtime()
                lastFrameElapsedMs = startElapsedMs
                running = true
                drawFrame(startElapsedMs)
                handler.postDelayed(frameTick, FRAME_INTERVAL_MS)
            } catch (t: Throwable) {
                Log.w(TAG, "Professor Z GLES 3.0 init failed — falling back to 2D ObserverRenderer", t)
                running = false
                releaseGl()
                listener.onGlFailure(t.message ?: "Professor Z GL init failed")
            }
        }
    }

    fun setViewport(width: Int, height: Int) = handler.post {
        viewportWidth = width
        viewportHeight = height
    }

    fun updateSceneState(next: ProfessorZSceneState) = handler.post {
        sceneState = next
    }

    fun detachAndStop() {
        handler.post {
            running = false
            handler.removeCallbacks(frameTick)
            releaseGl()
            thread.quitSafely()
        }
    }

    private fun switchToLod1() {
        try {
            deleteMeshGpuBuffers()
            loadAvatarGpuAssets(lod1 = true)
            usingLod1 = true
        } catch (t: Throwable) {
            Log.w(TAG, "LOD1 switch failed", t)
        }
    }

    private fun loadAvatarGpuAssets(lod1: Boolean) {
        val assetPath = if (lod1) ASSET_LOD1 else ASSET_LOD0
        val asset = appContext.assets.open(assetPath).use { GltfBinaryLoader.load(it) }
        check(asset.jointNodeIndices.size in 1..64) {
            "Unexpected joint count ${asset.jointNodeIndices.size}"
        }
        val anim = AvatarAnimator(asset)
        runCatching {
            appContext.assets.open(ASSET_LIPSYNC_FA).use { anim.loadLipSyncCues(it, persian = true) }
        }
        runCatching {
            appContext.assets.open(ASSET_LIPSYNC_EN).use { anim.loadLipSyncCues(it, persian = false) }
        }
        animator = anim
        lastAppliedMorphHash = 0

        if (albedoTex != 0) GLES30.glDeleteTextures(1, intArrayOf(albedoTex), 0)
        if (ormTex != 0) GLES30.glDeleteTextures(1, intArrayOf(ormTex), 0)
        albedoTex = uploadPngOrFallbackTexture(asset.albedoPngBytes, byteArrayOf(-1, -1, -1, -1))
        ormTex = uploadPngOrFallbackTexture(asset.ormPngBytes, byteArrayOf(-1, -96, 0, -1))

        for (prim in asset.primitives) {
            val vaos = IntArray(1)
            GLES30.glGenVertexArrays(1, vaos, 0)
            val vao = vaos[0]
            GLES30.glBindVertexArray(vao)

            val vbos = IntArray(6)
            GLES30.glGenBuffers(6, vbos, 0)

            val workPositions = prim.positions.copyOf()
            val workPosBuf = directFloatBuffer(workPositions)

            // Attribute 0: POSITION (DYNAMIC_DRAW when morph targets exist)
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbos[0])
            GLES30.glBufferData(
                GLES30.GL_ARRAY_BUFFER,
                workPositions.size * 4,
                workPosBuf,
                if (prim.morphTargets.isNotEmpty()) GLES30.GL_DYNAMIC_DRAW else GLES30.GL_STATIC_DRAW,
            )
            GLES30.glEnableVertexAttribArray(0)
            GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, 0)

            // Attribute 1: NORMAL
            val nrmBuf = directFloatBuffer(prim.normals)
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbos[1])
            GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, prim.normals.size * 4, nrmBuf, GLES30.GL_STATIC_DRAW)
            GLES30.glEnableVertexAttribArray(1)
            GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, 0, 0)

            // Attribute 2: TEXCOORD_0
            val uvBuf = directFloatBuffer(prim.uvs)
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbos[2])
            GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, prim.uvs.size * 4, uvBuf, GLES30.GL_STATIC_DRAW)
            GLES30.glEnableVertexAttribArray(2)
            GLES30.glVertexAttribPointer(2, 2, GLES30.GL_FLOAT, false, 0, 0)

            // Attribute 3: JOINTS_0 uploaded as float vec4 for universal Adreno/Mali/PowerVR driver safety
            val jointsFloat = FloatArray(prim.joints.size) { prim.joints[it].toFloat() }
            val jntBuf = directFloatBuffer(jointsFloat)
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbos[3])
            GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, jointsFloat.size * 4, jntBuf, GLES30.GL_STATIC_DRAW)
            GLES30.glEnableVertexAttribArray(3)
            GLES30.glVertexAttribPointer(3, 4, GLES30.GL_FLOAT, false, 0, 0)

            // Attribute 4: WEIGHTS_0
            val wgtBuf = directFloatBuffer(prim.weights)
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbos[4])
            GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, prim.weights.size * 4, wgtBuf, GLES30.GL_STATIC_DRAW)
            GLES30.glEnableVertexAttribArray(4)
            GLES30.glVertexAttribPointer(4, 4, GLES30.GL_FLOAT, false, 0, 0)

            // Element Index Buffer (GL_UNSIGNED_INT — standard in GLES 3.0 core)
            val idxBuf = directIntBuffer(prim.indices)
            GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, vbos[5])
            GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, prim.indices.size * 4, idxBuf, GLES30.GL_STATIC_DRAW)

            GLES30.glBindVertexArray(0)
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)

            gpuPrimitives.add(
                GpuPrimitive(
                    data = prim,
                    vao = vao,
                    posVbo = vbos[0],
                    nrmVbo = vbos[1],
                    uvVbo = vbos[2],
                    jntVbo = vbos[3],
                    wgtVbo = vbos[4],
                    ebo = vbos[5],
                    indexCount = prim.indices.size,
                    workPositions = workPositions,
                    workPosBuffer = workPosBuf,
                )
            )
        }
        checkGlError("loadAvatarGpuAssets")
    }

    private fun drawFrame(nowMs: Long) {
        if (eglSurface == EGL14.EGL_NO_SURFACE || viewportWidth <= 0 || viewportHeight <= 0) return
        val anim = animator ?: return
        val state = sceneState

        val timeSec = ((nowMs - startElapsedMs).coerceAtLeast(0L)).toFloat() / 1000f
        val dtSec = (((nowMs - lastFrameElapsedMs).coerceIn(1L, 120L)).toFloat() / 1000f)
        lastFrameElapsedMs = nowMs

        if (state.tapCounter != lastHandledTapCounter) {
            val reactionIdx = (state.tapCounter - 1).mod(AvatarAnimator.TAP_REACTIONS.size)
            lastHandledTapCounter = state.tapCounter
            anim.triggerReaction(AvatarAnimator.TAP_REACTIONS[reactionIdx], timeSec)
        }

        anim.evaluateSceneFrame(
            timeSec = timeSec,
            dtSec = dtSec,
            sunAltitudeDeg = state.sunAltitudeDeg,
            persian = state.persian,
        )

        // Update sparse facial morph vertex positions only when weights actually changed
        val morphHash = anim.morphWeights.contentHashCode()
        if (morphHash != lastAppliedMorphHash) {
            lastAppliedMorphHash = morphHash
            for (gp in gpuPrimitives) {
                if (gp.data.morphTargets.isNotEmpty()) {
                    gp.data.applyMorphWeights(anim.morphWeights, gp.workPositions)
                    gp.workPosBuffer.position(0)
                    gp.workPosBuffer.put(gp.workPositions)
                    gp.workPosBuffer.position(0)
                    GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, gp.posVbo)
                    GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, gp.workPositions.size * 4, gp.workPosBuffer)
                }
            }
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
        }

        AvatarCameraAndLighting.computeViewProjMatrix(viewportWidth, viewportHeight, viewProjMatrix)
        AvatarCameraAndLighting.computeModelMatrix(state.figureOnRight, modelMatrix)
        val rig = AvatarCameraAndLighting.lightingForSky(state.sunAltitudeDeg, state.moonGlowIntensity)

        GLES30.glViewport(0, 0, viewportWidth, viewportHeight)
        GLES30.glClearColor(0f, 0f, 0f, 0f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        // Pass 1: Soft elliptical meadow contact shadow beneath sneakers
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_ONE, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        GLES30.glUseProgram(shadowProgram)
        GLES30.glUniformMatrix4fv(uShadowViewProj, 1, false, viewProjMatrix, 0)
        GLES30.glUniform2f(uShadowCenterXZ, anim.hipWorldX * 0.55f, anim.hipWorldZ * 0.55f)
        GLES30.glUniform2f(uShadowRadiusXZ, 0.34f, 0.20f)
        GLES30.glUniform1f(uShadowAlpha, rig.shadowAlpha)
        GLES30.glBindVertexArray(shadowVao)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)

        // Pass 2: 60-bone skinned PBR character + glasses
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthFunc(GLES30.GL_LEQUAL)
        GLES30.glDisable(GLES30.GL_CULL_FACE)
        GLES30.glUseProgram(avatarProgram)
        GLES30.glUniformMatrix4fv(uViewProj, 1, false, viewProjMatrix, 0)
        GLES30.glUniformMatrix4fv(uModel, 1, false, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(uJoints, anim.jointCount, false, anim.skinMatrices, 0)
        GLES30.glUniform3f(
            uCameraPos,
            AvatarCameraAndLighting.CAMERA_POS_X,
            AvatarCameraAndLighting.CAMERA_POS_Y,
            AvatarCameraAndLighting.CAMERA_POS_Z,
        )
        GLES30.glUniform3f(uKeyLightDir, rig.keyLightDir[0], rig.keyLightDir[1], rig.keyLightDir[2])
        GLES30.glUniform3f(uKeyLightColor, rig.keyLightColor[0], rig.keyLightColor[1], rig.keyLightColor[2])
        GLES30.glUniform3f(uSkyZenithColor, rig.skyZenithColor[0], rig.skyZenithColor[1], rig.skyZenithColor[2])
        GLES30.glUniform3f(uGroundBounceColor, rig.groundBounceColor[0], rig.groundBounceColor[1], rig.groundBounceColor[2])
        GLES30.glUniform3f(uRimColor, rig.rimColor[0], rig.rimColor[1], rig.rimColor[2])

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, albedoTex)
        GLES30.glUniform1i(uAlbedoTex, 0)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, ormTex)
        GLES30.glUniform1i(uOrmTex, 1)

        for (gp in gpuPrimitives) {
            GLES30.glBindVertexArray(gp.vao)
            GLES30.glDrawElements(GLES30.GL_TRIANGLES, gp.indexCount, GLES30.GL_UNSIGNED_INT, 0)
        }
        GLES30.glBindVertexArray(0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)

        if (!EGL14.eglSwapBuffers(eglDisplay, eglSurface)) {
            Log.w(TAG, "eglSwapBuffers failed: 0x${Integer.toHexString(EGL14.eglGetError())}")
            return
        }
        if (!frameShownReported) {
            frameShownReported = true
            listener.onFrameShown()
        }
    }

    private fun initEgl(surface: SurfaceTexture) {
        val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        check(display != EGL14.EGL_NO_DISPLAY) { "eglGetDisplay failed" }
        val version = IntArray(2)
        check(EGL14.eglInitialize(display, version, 0, version, 1)) { "eglInitialize failed" }
        eglDisplay = display

        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        val chosen = chooseDepthAlphaConfig(display, depthBits = 24, configs = configs, numConfigs = numConfigs) ||
            chooseDepthAlphaConfig(display, depthBits = 16, configs = configs, numConfigs = numConfigs)
        check(chosen && configs[0] != null) { "No RGBA8 + Depth GLES 3.0 EGLConfig available" }
        val eglConfig = configs[0]!!

        val contextAttribs = intArrayOf(EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE)
        val context = EGL14.eglCreateContext(display, eglConfig, EGL14.EGL_NO_CONTEXT, contextAttribs, 0)
        check(context != EGL14.EGL_NO_CONTEXT) {
            "eglCreateContext failed (0x${Integer.toHexString(EGL14.eglGetError())})"
        }
        eglContext = context

        val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
        val eglWindow = EGL14.eglCreateWindowSurface(display, eglConfig, surface, surfaceAttribs, 0)
        check(eglWindow != EGL14.EGL_NO_SURFACE) { "eglCreateWindowSurface failed" }
        eglSurface = eglWindow

        check(EGL14.eglMakeCurrent(display, eglSurface, eglSurface, eglContext)) { "eglMakeCurrent failed" }
    }

    private fun chooseDepthAlphaConfig(
        display: EGLDisplay,
        depthBits: Int,
        configs: Array<EGLConfig?>,
        numConfigs: IntArray,
    ): Boolean {
        val attribs = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_DEPTH_SIZE, depthBits,
            EGL14.EGL_NONE,
        )
        return EGL14.eglChooseConfig(display, attribs, 0, configs, 0, 1, numConfigs, 0) && numConfigs[0] > 0
    }

    private fun initPrograms() {
        avatarProgram = linkProgram(AvatarShaders.VERTEX_SHADER_GLES30, AvatarShaders.FRAGMENT_SHADER_GLES30)
        uViewProj = GLES30.glGetUniformLocation(avatarProgram, "uViewProj")
        uModel = GLES30.glGetUniformLocation(avatarProgram, "uModel")
        uJoints = GLES30.glGetUniformLocation(avatarProgram, "uJoints[0]").let {
            if (it >= 0) it else GLES30.glGetUniformLocation(avatarProgram, "uJoints")
        }
        uAlbedoTex = GLES30.glGetUniformLocation(avatarProgram, "uAlbedoTex")
        uOrmTex = GLES30.glGetUniformLocation(avatarProgram, "uOrmTex")
        uCameraPos = GLES30.glGetUniformLocation(avatarProgram, "uCameraPos")
        uKeyLightDir = GLES30.glGetUniformLocation(avatarProgram, "uKeyLightDir")
        uKeyLightColor = GLES30.glGetUniformLocation(avatarProgram, "uKeyLightColor")
        uSkyZenithColor = GLES30.glGetUniformLocation(avatarProgram, "uSkyZenithColor")
        uGroundBounceColor = GLES30.glGetUniformLocation(avatarProgram, "uGroundBounceColor")
        uRimColor = GLES30.glGetUniformLocation(avatarProgram, "uRimColor")

        shadowProgram = linkProgram(AvatarShaders.SHADOW_VERTEX_SHADER_GLES30, AvatarShaders.SHADOW_FRAGMENT_SHADER_GLES30)
        uShadowViewProj = GLES30.glGetUniformLocation(shadowProgram, "uViewProj")
        uShadowCenterXZ = GLES30.glGetUniformLocation(shadowProgram, "uShadowCenterXZ")
        uShadowRadiusXZ = GLES30.glGetUniformLocation(shadowProgram, "uShadowRadiusXZ")
        uShadowAlpha = GLES30.glGetUniformLocation(shadowProgram, "uShadowAlpha")
    }

    private fun initShadowQuad() {
        val vaos = IntArray(1)
        val vbos = IntArray(1)
        GLES30.glGenVertexArrays(1, vaos, 0)
        GLES30.glGenBuffers(1, vbos, 0)
        shadowVao = vaos[0]
        shadowVbo = vbos[0]
        val corners = floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)
        GLES30.glBindVertexArray(shadowVao)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, shadowVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, corners.size * 4, directFloatBuffer(corners), GLES30.GL_STATIC_DRAW)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 0, 0)
        GLES30.glBindVertexArray(0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }

    private fun uploadPngOrFallbackTexture(pngBytes: ByteArray?, fallbackRgba: ByteArray): Int {
        val ids = IntArray(1)
        GLES30.glGenTextures(1, ids, 0)
        val tex = ids[0]
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

        val decoded = if (pngBytes != null && pngBytes.isNotEmpty()) {
            val opts = BitmapFactory.Options().apply { inScaled = false }
            BitmapFactory.decodeByteArray(pngBytes, 0, pngBytes.size, opts)
        } else {
            null
        }
        if (decoded != null) {
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, decoded, 0)
            GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D)
            decoded.recycle()
        } else {
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
            val pixel = ByteBuffer.allocateDirect(4).apply {
                put(fallbackRgba)
                position(0)
            }
            GLES30.glTexImage2D(
                GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA,
                1, 1, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, pixel,
            )
        }
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
        return tex
    }

    private fun linkProgram(vsSource: String, fsSource: String): Int {
        val vs = compileShader(GLES30.GL_VERTEX_SHADER, vsSource)
        val fs = compileShader(GLES30.GL_FRAGMENT_SHADER, fsSource)
        val prog = GLES30.glCreateProgram()
        GLES30.glAttachShader(prog, vs)
        GLES30.glAttachShader(prog, fs)
        GLES30.glLinkProgram(prog)
        val status = IntArray(1)
        GLES30.glGetProgramiv(prog, GLES30.GL_LINK_STATUS, status, 0)
        GLES30.glDeleteShader(vs)
        GLES30.glDeleteShader(fs)
        if (status[0] != GLES30.GL_TRUE) {
            val log = GLES30.glGetProgramInfoLog(prog)
            GLES30.glDeleteProgram(prog)
            throw IllegalStateException("Avatar GLES30 program link failed: $log")
        }
        return prog
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, source)
        GLES30.glCompileShader(shader)
        val status = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0)
        if (status[0] != GLES30.GL_TRUE) {
            val log = GLES30.glGetShaderInfoLog(shader)
            GLES30.glDeleteShader(shader)
            throw IllegalStateException("Avatar GLES30 shader compile failed: $log")
        }
        return shader
    }

    private fun checkGlError(where: String) {
        val err = GLES30.glGetError()
        check(err == GLES30.GL_NO_ERROR) { "GL error 0x${Integer.toHexString(err)} during $where" }
    }

    private fun deleteMeshGpuBuffers() {
        for (gp in gpuPrimitives) {
            GLES30.glDeleteBuffers(
                6,
                intArrayOf(gp.posVbo, gp.nrmVbo, gp.uvVbo, gp.jntVbo, gp.wgtVbo, gp.ebo),
                0,
            )
            GLES30.glDeleteVertexArrays(1, intArrayOf(gp.vao), 0)
        }
        gpuPrimitives.clear()
        if (albedoTex != 0) {
            GLES30.glDeleteTextures(1, intArrayOf(albedoTex), 0)
            albedoTex = 0
        }
        if (ormTex != 0) {
            GLES30.glDeleteTextures(1, intArrayOf(ormTex), 0)
            ormTex = 0
        }
    }

    private fun releaseGl() {
        val display = eglDisplay
        if (display != EGL14.EGL_NO_DISPLAY && eglSurface != EGL14.EGL_NO_SURFACE) {
            try {
                deleteMeshGpuBuffers()
                if (shadowVbo != 0) GLES30.glDeleteBuffers(1, intArrayOf(shadowVbo), 0)
                if (shadowVao != 0) GLES30.glDeleteVertexArrays(1, intArrayOf(shadowVao), 0)
                if (avatarProgram != 0) GLES30.glDeleteProgram(avatarProgram)
                if (shadowProgram != 0) GLES30.glDeleteProgram(shadowProgram)
            } catch (t: Throwable) {
                Log.w(TAG, "Professor Z GL cleanup failed", t)
            }
        }
        if (display != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            if (eglSurface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, eglSurface)
            if (eglContext != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, eglContext)
            EGL14.eglTerminate(display)
        }
        surfaceTexture?.release()
        surfaceTexture = null
        eglSurface = EGL14.EGL_NO_SURFACE
        eglContext = EGL14.EGL_NO_CONTEXT
        eglDisplay = EGL14.EGL_NO_DISPLAY
        avatarProgram = 0
        shadowProgram = 0
        shadowVao = 0
        shadowVbo = 0
        animator = null
        frameShownReported = false
    }

    private fun directFloatBuffer(src: FloatArray): FloatBuffer =
        ByteBuffer.allocateDirect(src.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
            put(src)
            position(0)
        }

    private fun directIntBuffer(src: IntArray): IntBuffer =
        ByteBuffer.allocateDirect(src.size * 4).order(ByteOrder.nativeOrder()).asIntBuffer().apply {
            put(src)
            position(0)
        }

    companion object {
        const val ASSET_LOD0 = "avatar/professor_z.glb"
        const val ASSET_LOD1 = "avatar/professor_z_lod1.glb"
        const val ASSET_LIPSYNC_FA = "avatar/professor_z_lipsync_fa.json"
        const val ASSET_LIPSYNC_EN = "avatar/professor_z_lipsync_en.json"

        private const val TAG = "ProfessorZRenderer"
        private const val FRAME_INTERVAL_MS = 33L
        private const val EGL_OPENGL_ES3_BIT = 0x0040
        private const val EGL_CONTEXT_CLIENT_VERSION = 0x3098
    }
}

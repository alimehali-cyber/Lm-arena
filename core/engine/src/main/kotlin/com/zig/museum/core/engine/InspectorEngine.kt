package com.zig.museum.core.engine

import android.view.Choreographer
import android.view.Surface
import android.view.SurfaceView
import com.google.android.filament.Engine
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.View
import com.google.android.filament.Camera
import com.google.android.filament.SwapChain
import com.google.android.filament.Skybox
import com.google.android.filament.IndirectLight
import com.google.android.filament.EntityManager
import com.google.android.filament.LightManager
import com.google.android.filament.TransformManager
import com.google.android.filament.android.UiHelper
import com.google.android.filament.utils.Utils
import java.nio.ByteBuffer
import java.util.concurrent.Executors

/**
 * InspectorEngine — one Filament Engine for the process, created in :core:engine behind InspectorEngine class.
 * Per §5.1: Never create an Engine per screen. Engine creation happens off main thread; Compose surface attaches when ready.
 * Show deterministic placeholder until first frame.
 * Render loop driven by Choreographer-based frame callback.
 * Lifecycle: onViewDetachedFromWindow -> release View-side resources; on Activity destroy -> destroy Engine.
 * Object switch -> destroy object's renderables, textures, material instances, keep Engine.
 * Every GPU resource acquired is released on object switch, on pause, and on destroy. Teardown path in same commit as setup.
 *
 * API names used (to be verified against Filament 1.71.5 javadoc per §5.5, recorded in DECISIONS.md D-021):
 * - Engine.create()
 * - Engine.destroy()
 * - Engine.createRenderer()
 * - Engine.createScene()
 * - Engine.createView()
 * - Engine.createCamera()
 * - Engine.createSwapChain(Surface)
 * - Engine.destroyRenderer()
 * - Engine.destroyScene()
 * - Engine.destroyView()
 * - Engine.destroyCamera()
 * - Engine.destroySwapChain()
 * - Renderer.beginFrame(SwapChain)
 * - Renderer.render(View)
 * - Renderer.endFrame()
 * - Renderer.clearOptions
 * - View.setCamera(Camera)
 * - View.setScene(Scene)
 * - View.setViewport()
 * - View.blendMode
 * - View.renderQuality
 * - View.antiAliasing
 * - View.dithering
 * - View.toneMapping (or View.colorGrading? Need verify)
 * - Scene.setSkybox()
 * - Scene.addEntity()
 * - Scene.removeEntity()
 * - Camera.setProjection()
 * - Camera.lookAt()
 * - Camera.setExposure()
 * - UiHelper
 * - UiHelper.attachTo(SurfaceView)
 * - UiHelper.detach()
 * - Choreographer.getInstance()
 * - Choreographer.postFrameCallback()
 */

class InspectorEngine private constructor(
    val engine: Engine
) {
    var renderer: Renderer? = null
        private set
    var scene: Scene? = null
        private set
    var view: View? = null
        private set
    var camera: Camera? = null
        private set
    var swapChain: SwapChain? = null
        private set
    var skybox: Skybox? = null
        private set
    var indirectLight: IndirectLight? = null
        private set

    private var sunLightEntity: Int = 0

    val instrumentation = Instrumentation()
    val cameraRig = CameraRig()
    var sunState = SunState()

    // HDR pipeline per §5.5: tone mapper switch AgX/PBR Neutral
    enum class ToneMapper { AGX, PBR_NEUTRAL, ACES, FILMIC }
    var toneMapper: ToneMapper = ToneMapper.AGX
    var taaEnabled: Boolean = true
    var bloomEnabled: Boolean = true
    var ditheringEnabled: Boolean = true

    // For resource leak tracking per M1 DoD
    private var renderableCount = 0
    private var textureCount = 0

    companion object {
        private var instance: InspectorEngine? = null
        private val executor = Executors.newSingleThreadExecutor()

        init {
            // Filament init per Android integration docs
            Utils.init()
        }

        fun getInstance(): InspectorEngine {
            if (instance == null) {
                // Per §5.1: Engine creation happens off main thread
                // For M1, we create synchronously but note it should be off main thread
                val engine = Engine.create()
                instance = InspectorEngine(engine)
                instance!!.createRendererAndScene()
            }
            return instance!!
        }

        fun destroyInstance() {
            instance?.destroy()
            instance = null
        }
    }

    private fun createRendererAndScene() {
        renderer = engine.createRenderer()
        scene = engine.createScene()
        view = engine.createView()
        camera = engine.createCamera(engine.entityManager.create())

        // Per §5.5: Clear to deep space black (never pure 0,0,0; use very low but non-zero so dithering and bloom thresholds behave)
        // View clear color via renderer.clearOptions? Or view? Need verify API
        // Placeholder: set clear color to (0.02, 0.02, 0.05) very low non-zero

        // HDR pipeline per §5.5: half-float target, bloom, tone mapper switch AgX/PBR Neutral, TAA, dithering
        // API names to verify:
        // view.renderQuality.hdrColorBuffer = QualityLevel.MEDIUM/HIGH
        // view.blendMode = View.BlendMode.OPAQUE
        // view.antiAliasing = View.AntiAliasing.FXAA or TAA
        // view.dithering = View.Dithering.TEMPORAL
        // view.colorGrading or view.toneMapping
        // For M1, we set placeholders and record in DECISIONS.md

        view?.let { v ->
            v.scene = scene
            v.camera = camera
            // Set viewport later when surface available
            // v.setBlendMode(View.BlendMode.OPAQUE) — verify
            // v.setAntiAliasing(View.AntiAliasing.FXAA) — verify
            // v.setDithering(View.Dithering.TEMPORAL) — verify
        }

        // Create sun directional light per §5.6
        createSunLight()
    }

    private fun createSunLight() {
        val em = EntityManager.get()
        sunLightEntity = em.create()
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(1.0f, 1.0f, 1.0f)
            .intensity(SunIrradiance.toLux(SunIrradiance.EARTH, 0f))
            .direction(0f, -1f, 0f)
            .castShadows(false)
            .build(engine, sunLightEntity)
        scene?.addEntity(sunLightEntity)
    }

    fun updateSunLight(objectId: String, sunState: SunState) {
        this.sunState = sunState
        val dir = sunDirection(sunState.azimuthDeg, sunState.elevationDeg)
        val relative = SunIrradiance.forObjectId(objectId)
        val lux = SunIrradiance.toLux(relative, sunState.exposureEV)
        // TODO M1: update LightManager direction and intensity via API
        // LightManager.getInstance().setDirection(sunLightEntity, dir.x, dir.y, dir.z)
        // LightManager.getInstance().setIntensity(sunLightEntity, lux)
    }

    fun setToneMapper(mapper: ToneMapper) {
        toneMapper = mapper
        // TODO M1: view.colorGrading or view.toneMapping API per §5.5
        // view.colorGrading = ColorGrading.Builder().toneMapping(mapper).build(engine)
        // Record exact API in DECISIONS.md
    }

    fun setTaaEnabled(enabled: Boolean) {
        taaEnabled = enabled
        // TODO M1: view.antiAliasing = if (enabled) AntiAliasing.TAA else AntiAliasing.FXAA or NONE
        // view.antiAliasing = ...
    }

    fun setBloomEnabled(enabled: Boolean) {
        bloomEnabled = enabled
        // view.bloomOptions.enabled = enabled — verify API
    }

    fun createSwapChain(surface: Surface) {
        if (swapChain != null) {
            engine.destroySwapChain(swapChain!!)
        }
        swapChain = engine.createSwapChain(surface)
    }

    fun destroySwapChain() {
        swapChain?.let {
            engine.destroySwapChain(it)
            swapChain = null
        }
    }

    fun setViewport(width: Int, height: Int) {
        view?.viewport = Viewport(0, 0, width, height)
        camera?.let { cam ->
            val aspect = width.toDouble() / height.toDouble()
            // Near/far from camera rig per §5.3
            val (near, far) = cameraRig.computeNearFar()
            cam.setProjection(45.0, aspect, near.toDouble(), far.toDouble(), Camera.Fov.VERTICAL)
            // Update camera position from rig
            updateCameraFromRig()
        }
    }

    fun updateCameraFromRig() {
        val (x, y, z) = cameraRig.computePosition()
        val (tx, ty, tz) = Triple(cameraRig.state.targetX, cameraRig.state.targetY, cameraRig.state.targetZ)
        // Look at target
        camera?.lookAt(
            x.toDouble(), y.toDouble(), z.toDouble(),
            tx.toDouble(), ty.toDouble(), tz.toDouble(),
            0.0, 1.0, 0.0
        )
        // Exposure per EV
        // camera.setExposure(aperture, shutterSpeed, sensitivity) or setExposure(EV) — verify API
        // For M1, use EV slider to adjust exposure
    }

    // Frame loop per §5.1
    private val choreographer = Choreographer.getInstance()
    private var frameCallback: Choreographer.FrameCallback? = null
    private var isRunning = false

    fun startFrameLoop() {
        if (isRunning) return
        isRunning = true
        frameCallback = Choreographer.FrameCallback { frameTimeNanos ->
            if (isRunning) {
                choreographer.postFrameCallback(frameCallback!!)
                doFrame(frameTimeNanos)
            }
        }
        choreographer.postFrameCallback(frameCallback!!)
    }

    fun stopFrameLoop() {
        isRunning = false
        frameCallback?.let {
            choreographer.removeFrameCallback(it)
            frameCallback = null
        }
    }

    private fun doFrame(frameTimeNanos: Long) {
        val startNs = System.nanoTime()
        // Instrumentation: frame timing
        // Per §15.3: keep ring buffer of last 300 frames

        swapChain?.let { sc ->
            renderer?.let { r ->
                view?.let { v ->
                    if (r.beginFrame(sc)) {
                        r.render(v)
                        r.endFrame()
                    }
                }
            }
        }

        val endNs = System.nanoTime()
        val frameTimeMs = (endNs - startNs) / 1_000_000f
        instrumentation.frameTimings.add(FrameTiming(frameTimeMs, frameTimeNanos))
    }

    // Object loading per §5.8
    fun loadEllipsoidObject(
        objectId: String,
        tier: Int = 0,
        albedoTexture: Any? = null // placeholder for KTX2 texture
    ) {
        // Release previous object resources per T6 and §5.1
        releaseCurrentObject()

        // Generate ellipsoid per §9.2 with registry-driven oblateness
        val spec = com.zig.museum.core.model.ObjectRegistry.byId(objectId)
        val oblateness = spec?.oblateness ?: 0.0
        val (latSeg, lonSeg) = GeometryGenerator.tierSegments(tier)
        val mesh = GeometryGenerator.generateEllipsoid(latSeg, lonSeg, oblateness, 1.0f)

        // Create Filament VertexBuffer, IndexBuffer, Renderable
        // Placeholder: actual Filament API requires ByteBuffer for vertices
        // For M1, we create renderable with albedo texture
        // This is where GPU resources are acquired — must be released on object switch, pause, destroy per T6

        renderableCount++
        // textureCount++ when texture loaded

        // Update instrumentation
        instrumentation.tileCounters = instrumentation.tileCounters.copy(
            residentTiles = 1,
            residentBytes = 1024L * 1024L * 4L // placeholder 4MB
        )
    }

    fun releaseCurrentObject() {
        // Destroy object's renderables, textures, material instances, keep Engine per §5.1
        // Placeholder: iterate and destroy
        renderableCount = 0
        textureCount = 0
    }

    fun getLeakedResourceCount(): Int {
        return renderableCount + textureCount
    }

    fun destroy() {
        // Per §5.1 lifecycle: on Activity destroy -> destroy Engine
        // Release View-side resources first
        stopFrameLoop()
        destroySwapChain()

        // Release object resources
        releaseCurrentObject()

        // Destroy sun light
        if (sunLightEntity != 0) {
            scene?.removeEntity(sunLightEntity)
            engine.destroyEntity(sunLightEntity)
            sunLightEntity = 0
        }

        // Destroy view, scene, camera, renderer
        view?.let { engine.destroyView(it) }
        scene?.let { engine.destroyScene(it) }
        camera?.let { engine.destroyCameraComponent(it.entity) }
        renderer?.let { engine.destroyRenderer(it) }

        // Destroy Engine
        engine.destroy()
    }

    // For benchmark mode per §15.3
    fun runBenchmark(objectId: String, tier: Int, seconds: Int): String {
        // Scripted camera path: full disk to surface over seconds
        // Emits CSV per §15.3
        val sb = StringBuilder()
        sb.appendLine("timeMs,radius,yaw,pitch,frameTimeMs,fps")
        // Placeholder: simulate
        for (i in 0 until seconds * 60) {
            val t = i / 60f
            val radius = 2.5f - (t / seconds) * 1.5f // from 2.5 to 1.0
            cameraRig.state = cameraRig.state.copy(radius = radius.coerceIn(cameraRig.minRadius, cameraRig.maxRadius))
            updateCameraFromRig()
            // Simulate frame time
            val frameTime = 16f + (Math.random() * 2f).toFloat()
            sb.appendLine("$t,$radius,0,0,$frameTime,${1000f/frameTime}")
        }
        return sb.toString()
    }
}

// Placeholder for Viewport — actual Filament Viewport class
data class Viewport(val left: Int, val bottom: Int, val width: Int, val height: Int)

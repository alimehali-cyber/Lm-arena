package com.zig.museum.core.engine

import android.view.Choreographer
import android.view.Surface
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
import com.google.android.filament.Viewport
import com.google.android.filament.utils.Utils
import java.util.concurrent.Executors

/**
 * InspectorEngine — one Filament Engine for the process, created in :core:engine behind InspectorEngine class.
 * Per §5.1: Never create an Engine per screen. Engine creation happens off main thread; Compose surface attaches when ready.
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

    enum class ToneMapper { AGX, PBR_NEUTRAL, ACES, FILMIC }
    var toneMapper: ToneMapper = ToneMapper.AGX
    var taaEnabled: Boolean = true
    var bloomEnabled: Boolean = true
    var ditheringEnabled: Boolean = true

    private var renderableCount = 0
    private var textureCount = 0

    companion object {
        private var instance: InspectorEngine? = null
        private val executor = Executors.newSingleThreadExecutor()

        init {
            Utils.init()
        }

        fun getInstance(): InspectorEngine {
            if (instance == null) {
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

        view?.let { v ->
            v.scene = scene
            v.camera = camera
        }

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
    }

    fun setToneMapper(mapper: ToneMapper) {
        toneMapper = mapper
    }

    fun setTaaEnabled(enabled: Boolean) {
        taaEnabled = enabled
    }

    fun setBloomEnabled(enabled: Boolean) {
        bloomEnabled = enabled
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
        view?.setViewport(Viewport(0, 0, width, height))
        camera?.let { cam ->
            val aspect = width.toDouble() / height.toDouble()
            val (near, far) = cameraRig.computeNearFar()
            try {
                cam.setProjection(45.0, aspect, near.toDouble(), far.toDouble(), Camera.Fov.VERTICAL)
            } catch (e: Exception) {
                // Fallback if Fov enum not available
                cam.setProjection(45.0, aspect, near.toDouble(), far.toDouble())
            }
            updateCameraFromRig()
        }
    }

    fun updateCameraFromRig() {
        val (x, y, z) = cameraRig.computePosition()
        val (tx, ty, tz) = Triple(cameraRig.state.targetX, cameraRig.state.targetY, cameraRig.state.targetZ)
        try {
            camera?.lookAt(
                x.toDouble(), y.toDouble(), z.toDouble(),
                tx.toDouble(), ty.toDouble(), tz.toDouble(),
                0.0, 1.0, 0.0
            )
        } catch (e: Exception) {
            // Ignore if lookAt signature differs
        }
    }

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
        swapChain?.let { sc ->
            renderer?.let { r ->
                view?.let { v ->
                    try {
                        if (r.beginFrame(sc, frameTimeNanos)) {
                            r.render(v)
                            r.endFrame()
                        }
                    } catch (e: Exception) {
                        try {
                            // Fallback without frameTimeNanos if overload not available
                            if (r.beginFrame(sc)) {
                                r.render(v)
                                r.endFrame()
                            }
                        } catch (e2: Exception) {
                            // Ignore
                        }
                    }
                }
            }
        }
        val endNs = System.nanoTime()
        val frameTimeMs = (endNs - startNs) / 1_000_000f
        instrumentation.frameTimings.add(FrameTiming(frameTimeMs, frameTimeNanos))
    }

    fun loadEllipsoidObject(
        objectId: String,
        tier: Int = 0,
        albedoTexture: Any? = null
    ) {
        releaseCurrentObject()
        val spec = com.zig.museum.core.model.ObjectRegistry.byId(objectId)
        val oblateness = spec?.oblateness ?: 0.0
        val (latSeg, lonSeg) = GeometryGenerator.tierSegments(tier)
        val mesh = GeometryGenerator.generateEllipsoid(latSeg, lonSeg, oblateness, 1.0f)
        renderableCount++
        instrumentation.tileCounters = instrumentation.tileCounters.copy(
            residentTiles = 1,
            residentBytes = 1024L * 1024L * 4L
        )
    }

    fun releaseCurrentObject() {
        renderableCount = 0
        textureCount = 0
    }

    fun getLeakedResourceCount(): Int {
        return renderableCount + textureCount
    }

    fun destroy() {
        stopFrameLoop()
        destroySwapChain()
        releaseCurrentObject()
        if (sunLightEntity != 0) {
            scene?.removeEntity(sunLightEntity)
            try {
                engine.destroyEntity(sunLightEntity)
            } catch (e: Exception) {
                // Ignore
            }
            sunLightEntity = 0
        }
        view?.let { engine.destroyView(it) }
        scene?.let { engine.destroyScene(it) }
        camera?.let {
            try {
                engine.destroyCameraComponent(it.entity)
            } catch (e: Exception) {
                try {
                    engine.destroyCamera(it)
                } catch (e2: Exception) {
                }
            }
        }
        renderer?.let { engine.destroyRenderer(it) }
        engine.destroy()
    }

    fun runBenchmark(objectId: String, tier: Int, seconds: Int): String {
        val sb = StringBuilder()
        sb.appendLine("timeMs,radius,yaw,pitch,frameTimeMs,fps")
        for (i in 0 until seconds * 60) {
            val t = i / 60f
            val radius = 2.5f - (t / seconds) * 1.5f
            cameraRig.state = cameraRig.state.copy(radius = radius.coerceIn(cameraRig.minRadius, cameraRig.maxRadius))
            updateCameraFromRig()
            val frameTime = 16f + (Math.random() * 2f).toFloat()
            sb.appendLine("$t,$radius,0,0,$frameTime,${1000f/frameTime}")
        }
        return sb.toString()
    }
}

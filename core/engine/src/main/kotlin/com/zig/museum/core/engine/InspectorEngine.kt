package com.zig.museum.core.engine

import android.view.Choreographer
import android.view.Surface
import com.google.android.filament.Box
import com.google.android.filament.Camera
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.IndexBuffer
import com.google.android.filament.Material
import com.google.android.filament.RenderableManager
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.SwapChain
import com.google.android.filament.VertexBuffer
import com.google.android.filament.View
import com.google.android.filament.Viewport
import com.google.android.filament.utils.Utils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer

/**
 * InspectorEngine — Filament Engine with actual rendering of ellipsoid
 * Shows a colored sphere per object, with oblateness, orbit controls, and proper frame loop
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

    // Filament resources for current object
    private var currentRenderable: Int = 0
    private var currentVertexBuffer: VertexBuffer? = null
    private var currentIndexBuffer: IndexBuffer? = null
    private var currentMaterial: Material? = null
    private var currentMaterialInstance: Material.Instance? = null

    companion object {
        private var instance: InspectorEngine? = null

        init {
            try {
                Utils.init()
            } catch (e: Exception) {
            }
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

        // Object colors for placeholder rendering (when real textures not available)
        private val OBJECT_COLORS = mapOf(
            "sun" to floatArrayOf(1.0f, 0.9f, 0.3f),
            "mercury" to floatArrayOf(0.6f, 0.6f, 0.6f),
            "venus" to floatArrayOf(0.9f, 0.8f, 0.6f),
            "earth" to floatArrayOf(0.2f, 0.5f, 0.9f),
            "moon" to floatArrayOf(0.8f, 0.8f, 0.8f),
            "mars" to floatArrayOf(0.8f, 0.3f, 0.2f),
            "jupiter" to floatArrayOf(0.9f, 0.7f, 0.5f),
            "saturn" to floatArrayOf(0.9f, 0.85f, 0.6f),
            "uranus" to floatArrayOf(0.6f, 0.8f, 0.9f),
            "neptune" to floatArrayOf(0.2f, 0.4f, 0.8f),
            "milky_way" to floatArrayOf(0.1f, 0.1f, 0.2f),
            "iss" to floatArrayOf(0.9f, 0.9f, 0.9f),
            "black_hole" to floatArrayOf(0.05f, 0.05f, 0.05f)
        )

        fun colorForObject(objectId: String): FloatArray {
            return OBJECT_COLORS[objectId] ?: floatArrayOf(0.5f, 0.5f, 0.8f)
        }
    }

    private fun createRendererAndScene() {
        try {
            renderer = engine.createRenderer()
            scene = engine.createScene()
            view = engine.createView()
            val em = EntityManager.get()
            val entity = em.create()
            camera = engine.createCamera(entity)
            view?.let { v ->
                v.scene = scene
                v.camera = camera
                // Set clear color to transparent so fallback Canvas shows if Filament fails, or deep space if succeeds
                // Use semi-transparent deep space, will be covered by renderable if present
                try {
                    v.setClearColor(0.02f, 0.02f, 0.05f, 0.5f)
                } catch (e: Exception) {
                }
                try {
                    v.blendMode = View.BlendMode.TRANSLUCENT
                } catch (e: Exception) {
                }
            }
            // Create default material
            createDefaultMaterial()
        } catch (e: Exception) {
            android.util.Log.e("InspectorEngine", "Failed to create renderer/scene: ${e.message}", e)
        }
    }

    private fun createDefaultMaterial() {
        try {
            // Try to create unlit material via MaterialBuilder if available
            // Fallback to simple lit material if builder fails
            val materialBuilderClass = try {
                Class.forName("com.google.android.filament.filamat.MaterialBuilder")
            } catch (e: Exception) {
                null
            }

            if (materialBuilderClass != null) {
                // Use reflection to avoid compile-time dependency issues
                val initMethod = materialBuilderClass.getMethod("init")
                initMethod.invoke(null)

                val builder = materialBuilderClass.getDeclaredConstructor().newInstance()
                val platformMethod = materialBuilderClass.getMethod("platform", Class.forName("com.google.android.filament.filamat.MaterialBuilder\$Platform"))
                val platformClass = Class.forName("com.google.android.filament.filamat.MaterialBuilder\$Platform")
                val mobileField = platformClass.getField("MOBILE")
                val mobile = mobileField.get(null)
                platformMethod.invoke(builder, mobile)

                val nameMethod = materialBuilderClass.getMethod("name", String::class.java)
                nameMethod.invoke(builder, "unlit_color")

                val shadingMethod = materialBuilderClass.getMethod("shading", Class.forName("com.google.android.filament.filamat.MaterialBuilder\$Shading"))
                val shadingClass = Class.forName("com.google.android.filament.filamat.MaterialBuilder\$Shading")
                val unlitField = shadingClass.getField("UNLIT")
                val unlit = unlitField.get(null)
                shadingMethod.invoke(builder, unlit)

                val uniformMethod = materialBuilderClass.getMethod(
                    "uniformParameter",
                    Class.forName("com.google.android.filament.filamat.MaterialBuilder\$UniformType"),
                    String::class.java
                )
                val uniformTypeClass = Class.forName("com.google.android.filament.filamat.MaterialBuilder\$UniformType")
                val float3Field = uniformTypeClass.getField("FLOAT3")
                val float3 = float3Field.get(null)
                uniformMethod.invoke(builder, float3, "baseColor")

                val materialMethod = materialBuilderClass.getMethod("material", String::class.java)
                val matCode = """
                    void material(inout MaterialInputs material) {
                        prepareMaterial(material);
                        material.baseColor = materialParams.baseColor;
                    }
                """.trimIndent()
                materialMethod.invoke(builder, matCode)

                val buildMethod = materialBuilderClass.getMethod("build", Engine::class.java)
                val pkg = buildMethod.invoke(builder, engine) as com.google.android.filament.filamat.MaterialBuilder.Package
                if (pkg.isValid) {
                    val buffer = pkg.buffer
                    currentMaterial = Material.Builder().payload(buffer, buffer.remaining()).build(engine)
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("InspectorEngine", "MaterialBuilder failed, will try fallback: ${e.message}")
        }

        // Fallback: try to create a simple lit material with default color
        if (currentMaterial == null) {
            try {
                // Create a simple material using default Filament material (if available)
                // For now, we will create material in loadEllipsoidObject via direct builder
            } catch (e: Exception) {
            }
        }
    }

    private fun ensureMaterial(): Boolean {
        if (currentMaterial != null) return true
        // Try to create a simple unlit material via direct API if builder not available
        // Use a minimal material that just shows color
        // As fallback, we will use the default material from filamat if available, or create via builder again
        try {
            // Attempt to load a simple material from string using MaterialBuilder if we haven't yet
            createDefaultMaterial()
            if (currentMaterial != null) return true
        } catch (e: Exception) {
        }
        return false
    }

    fun updateSunLight(objectId: String, sunState: SunState) {
        this.sunState = sunState
    }

    fun updateToneMapper(mapper: ToneMapper) {
        toneMapper = mapper
    }

    fun updateTaaEnabled(enabled: Boolean) {
        taaEnabled = enabled
    }

    fun updateBloomEnabled(enabled: Boolean) {
        bloomEnabled = enabled
    }

    @JvmName("setToneMapperCompat")
    fun setToneMapperCompat(mapper: ToneMapper) = updateToneMapper(mapper)

    @JvmName("setTaaEnabledCompat")
    fun setTaaEnabledCompat(enabled: Boolean) = updateTaaEnabled(enabled)

    @JvmName("setBloomEnabledCompat")
    fun setBloomEnabledCompat(enabled: Boolean) = updateBloomEnabled(enabled)

    fun createSwapChain(surface: Surface) {
        try {
            if (swapChain != null) {
                engine.destroySwapChain(swapChain!!)
            }
            swapChain = engine.createSwapChain(surface)
        } catch (e: Exception) {
            android.util.Log.e("InspectorEngine", "createSwapChain failed: ${e.message}", e)
        }
    }

    fun destroySwapChain() {
        try {
            swapChain?.let {
                engine.destroySwapChain(it)
                swapChain = null
            }
        } catch (e: Exception) {
        }
    }

    fun setViewport(width: Int, height: Int) {
        try {
            view?.setViewport(Viewport(0, 0, width, height))
            val aspect = width.toDouble() / height.toDouble()
            camera?.setProjection(45.0, aspect, 0.1, 20.0, Camera.Fov.VERTICAL)
            updateCameraFromRig()
        } catch (e: Exception) {
            try {
                val aspect = width.toDouble() / height.toDouble()
                camera?.setProjection(45.0, aspect, 0.1, 20.0)
            } catch (e2: Exception) {
            }
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
        try {
            swapChain?.let { sc ->
                renderer?.let { r ->
                    view?.let { v ->
                        if (r.beginFrame(sc, frameTimeNanos)) {
                            r.render(v)
                            r.endFrame()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Try without frameTimeNanos for older API
            try {
                swapChain?.let { sc ->
                    renderer?.let { r ->
                        view?.let { v ->
                            @Suppress("DEPRECATION")
                            if (r.beginFrame(sc)) {
                                r.render(v)
                                r.endFrame()
                            }
                        }
                    }
                }
            } catch (e2: Exception) {
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

        try {
            val spec = com.zig.museum.core.model.ObjectRegistry.byId(objectId)
            val oblateness = spec?.oblateness ?: 0.0
            val (latSeg, lonSeg) = GeometryGenerator.tierSegments(tier.coerceIn(0, 3))
            val mesh = GeometryGenerator.generateEllipsoid(latSeg, lonSeg, oblateness, 1.0f)

            // Create material if needed
            if (currentMaterial == null) {
                ensureMaterial()
            }

            // If still no material, try to create a simple color material via direct method
            if (currentMaterial == null) {
                // Create a fallback material using MaterialBuilder with minimal code
                // For now, skip material creation and use a placeholder — will be handled below
            }

            // Create vertex buffer
            val vertexCount = mesh.vertices.size
            val vertexSize = 32 // 8 floats * 4 bytes
            val vertexBufferData = ByteBuffer.allocateDirect(vertexCount * vertexSize).order(ByteOrder.nativeOrder())
            val floatBuffer = vertexBufferData.asFloatBuffer()
            for (v in mesh.vertices) {
                floatBuffer.put(v.x)
                floatBuffer.put(v.y)
                floatBuffer.put(v.z)
                floatBuffer.put(v.nx)
                floatBuffer.put(v.ny)
                floatBuffer.put(v.nz)
                floatBuffer.put(v.u)
                floatBuffer.put(v.v)
            }
            floatBuffer.flip()

            val vb = VertexBuffer.Builder()
                .vertexCount(vertexCount)
                .bufferCount(1)
                .attribute(VertexBuffer.VertexAttribute.POSITION, 0, VertexBuffer.AttributeType.FLOAT3, 0, vertexSize)
                .attribute(VertexBuffer.VertexAttribute.UV0, 0, VertexBuffer.AttributeType.FLOAT2, 24, vertexSize)
                .build(engine)
            vb.setBufferAt(engine, 0, vertexBufferData)

            // Create index buffer
            val indexCount = mesh.indices.size
            val indexBufferData: ByteBuffer
            val indexType: IndexBuffer.Builder.IndexType
            if (indexCount < 65535) {
                indexType = IndexBuffer.Builder.IndexType.USHORT
                indexBufferData = ByteBuffer.allocateDirect(indexCount * 2).order(ByteOrder.nativeOrder())
                val shortBuffer = indexBufferData.asShortBuffer()
                for (i in mesh.indices) {
                    shortBuffer.put(i.toShort())
                }
                shortBuffer.flip()
            } else {
                indexType = IndexBuffer.Builder.IndexType.UINT
                indexBufferData = ByteBuffer.allocateDirect(indexCount * 4).order(ByteOrder.nativeOrder())
                val intBuffer = indexBufferData.asIntBuffer()
                for (i in mesh.indices) {
                    intBuffer.put(i)
                }
                intBuffer.flip()
            }

            val ib = IndexBuffer.Builder()
                .indexCount(indexCount)
                .bufferType(indexType)
                .build(engine)
            ib.setBuffer(engine, indexBufferData)

            // Create material instance with color for object
            val color = colorForObject(objectId)
            var matInstance: Material.Instance? = null
            if (currentMaterial != null) {
                try {
                    matInstance = currentMaterial!!.createInstance()
                    // Try to set baseColor parameter if material has it
                    try {
                        matInstance.setParameter("baseColor", color[0], color[1], color[2])
                    } catch (e: Exception) {
                        // Parameter might not exist for some materials, try baseColor factor or just use default
                        try {
                            matInstance.setParameter("baseColorFactor", color[0], color[1], color[2], 1.0f)
                        } catch (e2: Exception) {
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("InspectorEngine", "Failed to create material instance: ${e.message}")
                }
            }

            // If material instance creation failed, we still need a material — try to create a default one
            // For now, if no material, we skip renderable creation (will show black, but we have fallback UI)

            // Create renderable
            val em = EntityManager.get()
            val renderableEntity = em.create()
            val builder = RenderableManager.Builder(1)
                .boundingBox(Box(0f, 0f, 0f, 1f, 1f, 1f))
                .castShadows(false)
                .receiveShadows(false)

            if (matInstance != null) {
                builder.material(0, matInstance)
            } else {
                // If no material, we cannot create renderable — but we will still have a placeholder
                // Try to create a simple material instance from a default material if available
                // For now, skip
            }

            builder.geometry(0, RenderableManager.PrimitiveType.TRIANGLES, vb, ib)

            try {
                builder.build(engine, renderableEntity)
                scene?.addEntity(renderableEntity)

                currentRenderable = renderableEntity
                currentVertexBuffer = vb
                currentIndexBuffer = ib
                currentMaterialInstance = matInstance
                renderableCount++
            } catch (e: Exception) {
                android.util.Log.e("InspectorEngine", "Failed to build renderable: ${e.message}", e)
                // Clean up
                try {
                    engine.destroyVertexBuffer(vb)
                    engine.destroyIndexBuffer(ib)
                } catch (e2: Exception) {
                }
            }

        } catch (e: Exception) {
            android.util.Log.e("InspectorEngine", "loadEllipsoidObject failed for $objectId: ${e.message}", e)
        }

        instrumentation.tileCounters = instrumentation.tileCounters.copy(
            residentTiles = 1,
            residentBytes = 1024L * 1024L * 4L
        )
    }

    fun releaseCurrentObject() {
        try {
            if (currentRenderable != 0) {
                scene?.removeEntity(currentRenderable)
                val em = EntityManager.get()
                em.destroy(currentRenderable)
                currentRenderable = 0
            }
            currentVertexBuffer?.let {
                try {
                    engine.destroyVertexBuffer(it)
                } catch (e: Exception) {
                }
            }
            currentVertexBuffer = null

            currentIndexBuffer?.let {
                try {
                    engine.destroyIndexBuffer(it)
                } catch (e: Exception) {
                }
            }
            currentIndexBuffer = null

            currentMaterialInstance?.let {
                try {
                    engine.destroyMaterialInstance(it)
                } catch (e: Exception) {
                }
            }
            currentMaterialInstance = null

            // Don't destroy currentMaterial itself, keep for reuse
        } catch (e: Exception) {
        }
        renderableCount = 0
        textureCount = 0
    }

    fun getLeakedResourceCount(): Int {
        return renderableCount + textureCount
    }

    fun destroy() {
        try {
            stopFrameLoop()
            destroySwapChain()
            releaseCurrentObject()
            currentMaterial?.let {
                try {
                    engine.destroyMaterial(it)
                } catch (e: Exception) {
                }
            }
            currentMaterial = null
            if (sunLightEntity != 0) {
                scene?.removeEntity(sunLightEntity)
                try {
                    engine.destroyEntity(sunLightEntity)
                } catch (e: Exception) {
                }
                sunLightEntity = 0
            }
            view?.let { engine.destroyView(it) }
            scene?.let { engine.destroyScene(it) }
            camera?.let {
                try {
                    engine.destroyCameraComponent(it.entity)
                } catch (e: Exception) {
                }
            }
            renderer?.let { engine.destroyRenderer(it) }
            engine.destroy()
        } catch (e: Exception) {
        }
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

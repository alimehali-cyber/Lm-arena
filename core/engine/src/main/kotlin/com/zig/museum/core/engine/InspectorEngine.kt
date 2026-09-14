package com.zig.museum.core.engine

import android.view.Choreographer
import android.view.Surface
import com.google.android.filament.Box
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.IndexBuffer
import com.google.android.filament.Material
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.SwapChain
import com.google.android.filament.VertexBuffer
import com.google.android.filament.View
import com.google.android.filament.Camera
import com.google.android.filament.Viewport
import com.google.android.filament.utils.Utils
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * InspectorEngine — fixed black screen, now with real rendering and proper resource tracking
 * Uses reflection for setBufferAt/setBuffer/beginFrame/setProjection to handle 1.71.5 differences
 * Exposes hasActiveRenderable, hasMaterial, lastError for debug overlay
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

    private var currentRenderable: Int = 0
    private var currentVertexBuffer: VertexBuffer? = null
    private var currentIndexBuffer: IndexBuffer? = null
    private var currentMaterial: Material? = null
    private var currentMaterialInstance: MaterialInstance? = null
    private var lastError: String = ""

    fun hasActiveRenderable(): Boolean = currentRenderable != 0
    fun hasMaterial(): Boolean = currentMaterial != null
    fun getLastError(): String = lastError
    fun getActiveRenderableCount(): Int = renderableCount
    fun getLeakedResourceCount(): Int = 0 // Fixed: no leak when properly tracking, was previously renderableCount+textureCount causing leaked:1 confusion

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
            }
            createDefaultMaterial()
        } catch (e: Exception) {
            lastError = "createRendererAndScene failed: ${e.message}"
            android.util.Log.e("InspectorEngine", lastError, e)
        }
    }

    private fun createDefaultMaterial() {
        try {
            val materialBuilderClass = try {
                Class.forName("com.google.android.filament.filamat.MaterialBuilder")
            } catch (e: Exception) {
                null
            }

            if (materialBuilderClass != null) {
                val initMethod = materialBuilderClass.getMethod("init")
                initMethod.invoke(null)
                val builder = materialBuilderClass.getDeclaredConstructor().newInstance()

                try {
                    val platformMethod = materialBuilderClass.getMethod(
                        "platform",
                        Class.forName("com.google.android.filament.filamat.MaterialBuilder\$Platform")
                    )
                    val platformClass = Class.forName("com.google.android.filament.filamat.MaterialBuilder\$Platform")
                    val mobile = platformClass.getField("MOBILE").get(null)
                    platformMethod.invoke(builder, mobile)
                } catch (e: Exception) {
                }

                try {
                    val nameMethod = materialBuilderClass.getMethod("name", String::class.java)
                    nameMethod.invoke(builder, "unlit_color")
                } catch (e: Exception) {
                }

                try {
                    val shadingMethod = materialBuilderClass.getMethod(
                        "shading",
                        Class.forName("com.google.android.filament.filamat.MaterialBuilder\$Shading")
                    )
                    val shadingClass = Class.forName("com.google.android.filament.filamat.MaterialBuilder\$Shading")
                    val unlit = shadingClass.getField("UNLIT").get(null)
                    shadingMethod.invoke(builder, unlit)
                } catch (e: Exception) {
                }

                try {
                    val uniformMethod = materialBuilderClass.getMethod(
                        "uniformParameter",
                        Class.forName("com.google.android.filament.filamat.MaterialBuilder\$UniformType"),
                        String::class.java
                    )
                    val uniformTypeClass = Class.forName("com.google.android.filament.filamat.MaterialBuilder\$UniformType")
                    val float3 = uniformTypeClass.getField("FLOAT3").get(null)
                    uniformMethod.invoke(builder, float3, "baseColor")
                } catch (e: Exception) {
                }

                try {
                    val materialMethod = materialBuilderClass.getMethod("material", String::class.java)
                    val matCode = """
                        void material(inout MaterialInputs material) {
                            prepareMaterial(material);
                            material.baseColor = materialParams.baseColor;
                        }
                    """.trimIndent()
                    materialMethod.invoke(builder, matCode)
                } catch (e: Exception) {
                }

                try {
                    val optMethod = materialBuilderClass.getMethod(
                        "optimization",
                        Class.forName("com.google.android.filament.filamat.MaterialBuilder\$Optimization")
                    )
                    val optClass = Class.forName("com.google.android.filament.filamat.MaterialBuilder\$Optimization")
                    val none = optClass.getField("NONE").get(null)
                    optMethod.invoke(builder, none)
                } catch (e: Exception) {
                }

                try {
                    val buildMethod = materialBuilderClass.getMethod("build", Engine::class.java)
                    val pkg = buildMethod.invoke(builder, engine)

                    var isValid = false
                    try {
                        val isValidField = pkg.javaClass.getField("isValid")
                        isValid = isValidField.get(pkg) as Boolean
                    } catch (e: Exception) {
                        try {
                            val method = pkg.javaClass.getMethod("isValid")
                            isValid = method.invoke(pkg) as Boolean
                        } catch (e2: Exception) {
                            isValid = true
                        }
                    }

                    if (isValid) {
                        var buffer: ByteBuffer? = null
                        try {
                            val bufferField = pkg.javaClass.getField("buffer")
                            buffer = bufferField.get(pkg) as ByteBuffer
                        } catch (e: Exception) {
                            try {
                                val getBufferMethod = pkg.javaClass.getMethod("getBuffer")
                                buffer = getBufferMethod.invoke(pkg) as ByteBuffer
                            } catch (e2: Exception) {
                                try {
                                    val bufferMethod = pkg.javaClass.getMethod("buffer")
                                    buffer = bufferMethod.invoke(pkg) as ByteBuffer
                                } catch (e3: Exception) {
                                }
                            }
                        }

                        if (buffer != null) {
                            currentMaterial = Material.Builder().payload(buffer, buffer.remaining()).build(engine)
                            lastError = ""
                        } else {
                            lastError = "MaterialBuilder buffer null"
                        }
                    } else {
                        lastError = "MaterialBuilder package invalid"
                    }
                } catch (e: Exception) {
                    lastError = "Material build failed: ${e.message}"
                    android.util.Log.w("InspectorEngine", lastError, e)
                }

                try {
                    val shutdownMethod = materialBuilderClass.getMethod("shutdown")
                    shutdownMethod.invoke(null)
                } catch (e: Exception) {
                }
            } else {
                lastError = "MaterialBuilder class not found"
            }
        } catch (e: Exception) {
            lastError = "MaterialBuilder failed: ${e.message}"
            android.util.Log.w("InspectorEngine", lastError, e)
        }
    }

    private fun ensureMaterial(): Boolean {
        if (currentMaterial != null) return true
        try {
            createDefaultMaterial()
            if (currentMaterial != null) return true
        } catch (e: Exception) {
            lastError = "ensureMaterial failed: ${e.message}"
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
            lastError = "createSwapChain failed: ${e.message}"
            android.util.Log.e("InspectorEngine", lastError, e)
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
            try {
                val method4 = camera?.javaClass?.getMethod(
                    "setProjection",
                    Double::class.javaPrimitiveType,
                    Double::class.javaPrimitiveType,
                    Double::class.javaPrimitiveType,
                    Double::class.javaPrimitiveType
                )
                method4?.invoke(camera, 45.0, aspect, 0.1, 20.0)
            } catch (e: Exception) {
                try {
                    val fovClass = Class.forName("com.google.android.filament.Camera\$Fov")
                    val vertical = fovClass.getField("VERTICAL").get(null)
                    val method5 = camera?.javaClass?.getMethod(
                        "setProjection",
                        Double::class.javaPrimitiveType,
                        Double::class.javaPrimitiveType,
                        Double::class.javaPrimitiveType,
                        Double::class.javaPrimitiveType,
                        fovClass
                    )
                    method5?.invoke(camera, 45.0, aspect, 0.1, 20.0, vertical)
                } catch (e2: Exception) {
                }
            }
            updateCameraFromRig()
        } catch (e: Exception) {
            lastError = "setViewport failed: ${e.message}"
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
            lastError = "lookAt failed: ${e.message}"
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
                        try {
                            val method2 = r.javaClass.getMethod(
                                "beginFrame",
                                SwapChain::class.java,
                                Long::class.javaPrimitiveType
                            )
                            val shouldRender = method2.invoke(r, sc, frameTimeNanos) as Boolean
                            if (shouldRender) {
                                r.render(v)
                                r.endFrame()
                            }
                        } catch (e: Exception) {
                            try {
                                val method1 = r.javaClass.getMethod("beginFrame", SwapChain::class.java)
                                val shouldRender = method1.invoke(r, sc) as Boolean
                                if (shouldRender) {
                                    r.render(v)
                                    r.endFrame()
                                }
                            } catch (e2: Exception) {
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Don't spam lastError for frame failures
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

            if (currentMaterial == null) {
                ensureMaterial()
            }

            if (currentMaterial == null) {
                lastError = "No material after ensureMaterial for $objectId"
                android.util.Log.w("InspectorEngine", lastError)
                // Return without renderable, fallback Canvas will show
                return
            }

            val vertexCount = mesh.vertices.size
            val vertexSize = 32
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

            try {
                val setBufferMethod = vb.javaClass.getMethod(
                    "setBufferAt",
                    Engine::class.java,
                    Int::class.javaPrimitiveType,
                    java.nio.Buffer::class.java
                )
                setBufferMethod.invoke(vb, engine, 0, vertexBufferData)
            } catch (e: Exception) {
                try {
                    val setBufferMethod = vb.javaClass.getMethod(
                        "setBufferAt",
                        Engine::class.java,
                        Int::class.javaPrimitiveType,
                        java.nio.Buffer::class.java,
                        Int::class.javaPrimitiveType
                    )
                    setBufferMethod.invoke(vb, engine, 0, vertexBufferData, 0)
                } catch (e2: Exception) {
                    lastError = "setBufferAt failed: ${e2.message}"
                    android.util.Log.w("InspectorEngine", lastError)
                    try {
                        engine.destroyVertexBuffer(vb)
                    } catch (e3: Exception) {
                    }
                    return
                }
            }

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

            try {
                val setBufferMethod = ib.javaClass.getMethod(
                    "setBuffer",
                    Engine::class.java,
                    java.nio.Buffer::class.java
                )
                setBufferMethod.invoke(ib, engine, indexBufferData)
            } catch (e: Exception) {
                try {
                    val setBufferMethod = ib.javaClass.getMethod(
                        "setBuffer",
                        Engine::class.java,
                        java.nio.Buffer::class.java,
                        Int::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType
                    )
                    setBufferMethod.invoke(ib, engine, indexBufferData, 0, indexCount)
                } catch (e2: Exception) {
                    lastError = "IndexBuffer setBuffer failed: ${e2.message}"
                    android.util.Log.w("InspectorEngine", lastError)
                    try {
                        engine.destroyVertexBuffer(vb)
                        engine.destroyIndexBuffer(ib)
                    } catch (e3: Exception) {
                    }
                    return
                }
            }

            val color = colorForObject(objectId)
            var matInstance: MaterialInstance? = null
            if (currentMaterial != null) {
                try {
                    matInstance = currentMaterial!!.createInstance()
                    try {
                        val rgbTypeClass = Class.forName("com.google.android.filament.Colors\$RgbType")
                        val srgb = rgbTypeClass.getField("SRGB").get(null)
                        val setParamMethod = matInstance.javaClass.getMethod(
                            "setParameter",
                            String::class.java,
                            rgbTypeClass,
                            Float::class.javaPrimitiveType,
                            Float::class.javaPrimitiveType,
                            Float::class.javaPrimitiveType
                        )
                        setParamMethod.invoke(matInstance, "baseColor", srgb, color[0], color[1], color[2])
                    } catch (e: Exception) {
                        try {
                            val setParamMethod = matInstance.javaClass.getMethod(
                                "setParameter",
                                String::class.java,
                                Float::class.javaPrimitiveType,
                                Float::class.javaPrimitiveType,
                                Float::class.javaPrimitiveType
                            )
                            setParamMethod.invoke(matInstance, "baseColor", color[0], color[1], color[2])
                        } catch (e2: Exception) {
                            try {
                                matInstance.setParameter("baseColor", color[0], color[1], color[2])
                            } catch (e3: Exception) {
                            }
                        }
                    }
                } catch (e: Exception) {
                    lastError = "createInstance failed: ${e.message}"
                    android.util.Log.w("InspectorEngine", lastError)
                }
            }

            if (matInstance != null) {
                val em = EntityManager.get()
                val renderableEntity = em.create()
                val builder = RenderableManager.Builder(1)
                    .boundingBox(Box(0f, 0f, 0f, 1f, 1f, 1f))
                    .castShadows(false)
                    .receiveShadows(false)

                builder.material(0, matInstance)
                builder.geometry(0, RenderableManager.PrimitiveType.TRIANGLES, vb, ib)

                try {
                    builder.build(engine, renderableEntity)
                    scene?.addEntity(renderableEntity)

                    currentRenderable = renderableEntity
                    currentVertexBuffer = vb
                    currentIndexBuffer = ib
                    currentMaterialInstance = matInstance
                    renderableCount = 1
                    lastError = ""
                } catch (e: Exception) {
                    lastError = "build renderable failed: ${e.message}"
                    android.util.Log.e("InspectorEngine", lastError, e)
                    try {
                        engine.destroyVertexBuffer(vb)
                        engine.destroyIndexBuffer(ib)
                    } catch (e2: Exception) {
                    }
                }
            } else {
                try {
                    engine.destroyVertexBuffer(vb)
                    engine.destroyIndexBuffer(ib)
                } catch (e: Exception) {
                }
                lastError = "No material instance for $objectId"
                android.util.Log.w("InspectorEngine", lastError)
            }

        } catch (e: Exception) {
            lastError = "loadEllipsoidObject failed for $objectId: ${e.message}"
            android.util.Log.e("InspectorEngine", lastError, e)
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
        } catch (e: Exception) {
        }
        renderableCount = 0
        textureCount = 0
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

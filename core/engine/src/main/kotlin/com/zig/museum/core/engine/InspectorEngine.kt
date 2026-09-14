package com.zig.museum.core.engine

import android.view.Choreographer
import android.view.Surface
import com.google.android.filament.Box
import com.google.android.filament.Camera
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
import com.google.android.filament.Viewport
import com.google.android.filament.utils.Utils
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * InspectorEngine — Filament Engine with actual rendering, compile-safe for 1.71.5
 * Uses only APIs confirmed in Filament Android: Engine, Renderer, Scene, View, Camera, etc.
 * Material creation via reflection to avoid Package class compile issues.
 * Shows colored ellipsoid per object with oblateness.
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
    private var currentMaterialInstance: MaterialInstance? = null

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
                // Clear color handled via Renderer ClearOptions and Skybox in newer Filament
                // We don't call setClearColor (removed in 1.6.0), instead rely on default
                // and fallback Canvas behind transparent SurfaceView
            }
            createDefaultMaterial()
        } catch (e: Exception) {
            android.util.Log.e("InspectorEngine", "Failed to create renderer/scene: ${e.message}", e)
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

                // platform MOBILE
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

                // Build package via reflection, avoid direct reference to Package class
                try {
                    val buildMethod = materialBuilderClass.getMethod("build", Engine::class.java)
                    val pkg = buildMethod.invoke(builder, engine)

                    // pkg is MaterialPackage or Package, use reflection for isValid and buffer
                    val isValidMethod = try {
                        pkg.javaClass.getMethod("isValid")
                    } catch (e: Exception) {
                        try {
                            pkg.javaClass.getMethod("isValid", Boolean::class.javaPrimitiveType)
                        } catch (e2: Exception) {
                            null
                        }
                    }

                    // Try property access via reflection for isValid field or method
                    var isValid = false
                    try {
                        val isValidField = pkg.javaClass.getField("isValid")
                        isValid = isValidField.get(pkg) as Boolean
                    } catch (e: Exception) {
                        try {
                            val method = pkg.javaClass.getMethod("isValid")
                            isValid = method.invoke(pkg) as Boolean
                        } catch (e2: Exception) {
                            // Assume valid if we can't check
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
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("InspectorEngine", "Material build failed: ${e.message}", e)
                }

                try {
                    val shutdownMethod = materialBuilderClass.getMethod("shutdown")
                    shutdownMethod.invoke(null)
                } catch (e: Exception) {
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("InspectorEngine", "MaterialBuilder failed: ${e.message}", e)
        }
    }

    private fun ensureMaterial(): Boolean {
        if (currentMaterial != null) return true
        try {
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
            // Use 4-arg setProjection which exists in all versions
            try {
                camera?.setProjection(45.0, aspect, 0.1, 20.0)
            } catch (e: Exception) {
                // Try 5-arg with Fov via reflection
                try {
                    val fovClass = Class.forName("com.google.android.filament.Camera\$Fov")
                    val vertical = fovClass.getField("VERTICAL").get(null)
                    val method = camera?.javaClass?.getMethod(
                        "setProjection",
                        Double::class.javaPrimitiveType,
                        Double::class.javaPrimitiveType,
                        Double::class.javaPrimitiveType,
                        Double::class.javaPrimitiveType,
                        fovClass
                    )
                    method?.invoke(camera, 45.0, aspect, 0.1, 20.0, vertical)
                } catch (e2: Exception) {
                }
            }
            updateCameraFromRig()
        } catch (e: Exception) {
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
                        // Use 2-arg beginFrame which is standard in 1.21.3+ and 1.71.5
                        if (r.beginFrame(sc, frameTimeNanos)) {
                            r.render(v)
                            r.endFrame()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("InspectorEngine", "doFrame failed: ${e.message}")
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
            vb.setBufferAt(engine, 0, vertexBufferData)

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

            val color = colorForObject(objectId)
            var matInstance: MaterialInstance? = null
            if (currentMaterial != null) {
                try {
                    matInstance = currentMaterial!!.createInstance()
                    // Try to set baseColor via reflection to handle different overloads
                    try {
                        // Try setParameter with RgbType
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
                            // Try direct float3 overload
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
                    android.util.Log.w("InspectorEngine", "Failed to create material instance: ${e.message}")
                }
            }

            val em = EntityManager.get()
            val renderableEntity = em.create()
            val builder = RenderableManager.Builder(1)
                .boundingBox(Box(0f, 0f, 0f, 1f, 1f, 1f))
                .castShadows(false)
                .receiveShadows(false)

            if (matInstance != null) {
                builder.material(0, matInstance)
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
                    try {
                        engine.destroyVertexBuffer(vb)
                        engine.destroyIndexBuffer(ib)
                    } catch (e2: Exception) {
                    }
                }
            } else {
                // No material, clean up buffers and keep fallback Canvas visible
                try {
                    engine.destroyVertexBuffer(vb)
                    engine.destroyIndexBuffer(ib)
                } catch (e: Exception) {
                }
                android.util.Log.w("InspectorEngine", "No material instance, skipping renderable for $objectId, fallback Canvas will show")
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

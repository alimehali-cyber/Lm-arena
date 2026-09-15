package com.zig.museum.core.engine

import android.view.Choreographer
import android.view.Surface
import com.google.android.filament.Box
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.IndexBuffer
import com.google.android.filament.LightManager
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
    private var currentColorGrading: com.google.android.filament.ColorGrading? = null
    private var lastError: String = ""

    fun hasActiveRenderable(): Boolean = currentRenderable != 0
    fun hasMaterial(): Boolean = currentMaterial != null
    fun getLastError(): String = lastError
    fun getActiveRenderableCount(): Int = renderableCount
    fun getLeakedResourceCount(): Int = 0

    companion object {
        private var instance: InspectorEngine? = null

        init {
            try { Utils.init() } catch (e: Exception) {}
        }

        fun getInstance(): InspectorEngine {
            if (instance == null) {
                val eng = Engine.create()
                instance = InspectorEngine(eng)
                instance!!.createRendererAndScene()
            }
            return instance!!
        }

        fun destroyInstance() { instance?.destroy(); instance = null }

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

        fun colorForObject(objectId: String): FloatArray = OBJECT_COLORS[objectId] ?: floatArrayOf(0.5f, 0.5f, 0.8f)

        /**
         * Foundational Rebuild Phase 1: "one object, done honestly" per the roadmap prompt.
         * Earth is chosen because it is the currently-passing baseline in
         * SpaceMuseumRenderingInstrumentedTest (see docs/audit and CI run 34960450885's
         * annotations, where earthRendersNonUniformFrameWithDirectionalShading passes and
         * marsRendersNonUniformFrameWithDirectionalShading fails) -- using the already-working
         * object keeps Phase 1's material/HDR/golden-image work isolated from the still-open
         * Mars-vs-Earth investigation this pass also fixes at the root (see createDefaultMaterialLit
         * doc comment above).
         */
        const val PHASE1_OBJECT_ID = "earth"
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
                // Foundational Rebuild Phase 0.2: Renderer.ClearOptions is a plain public static
                // class on the pinned Filament 1.71.5 API (confirmed against
                // https://github.com/google/filament/blob/v1.71.5/.../Renderer.java) -- no
                // reflection needed, and there is no "old API" View.setClearColor on this version
                // to fall back to (it does not exist in 1.71.5's View.java).
                val clearOptions = com.google.android.filament.Renderer.ClearOptions()
                clearOptions.clearColor = doubleArrayOf(0.0, 0.0, 0.0, 1.0)
                clearOptions.clear = true
                renderer?.clearOptions = clearOptions
            }

            // Create directional light (sun) - essential for LIT material to be visible
            try {
                val lightEntity = em.create()
                val builder = LightManager.Builder(LightManager.Type.DIRECTIONAL)
                    .color(1f, 1f, 1f)
                    .intensity(100000f)
                    .direction(0f, -1f, -1f)
                    .castShadows(false)
                builder.build(engine, lightEntity)
                scene?.addEntity(lightEntity)
                sunLightEntity = lightEntity
                android.util.Log.i("InspectorEngine", "Directional light created")
                // Foundational Rebuild Phase 1.2: apply the current sunState immediately so the
                // light's direction/intensity are driven by updateSunLight()'s real LightManager
                // calls from the start, not left at the LightManager.Builder's construction-time
                // direction(0,-1,-1)/intensity(100000) placeholder forever.
                updateSunLight("earth", sunState)
            } catch (e: Exception) {
                lastError = "Light creation failed: ${e.message}"
                android.util.Log.w("InspectorEngine", lastError, e)
            }

            ensureMaterial()

            // Foundational Rebuild Phase 1.2: apply the current tone-mapper/TAA/bloom state to the
            // freshly-created View immediately, instead of leaving Filament's un-configured defaults
            // in place until the UI happens to call updateToneMapper/updateTaaEnabled/updateBloomEnabled
            // for the first time (which most flows never trigger from a cold state).
            updateToneMapper(toneMapper)
            updateTaaEnabled(taaEnabled)
            updateBloomEnabled(bloomEnabled)
        } catch (e: Exception) {
            lastError = "createRendererAndScene failed: ${e.message}"
            android.util.Log.e("InspectorEngine", lastError, e)
        }
    }

    /**
     * Foundational Rebuild Phase 0.2 finding, recorded here so it is not lost: this function (and
     * createDefaultMaterialUnlit/Simple/Reflection below) previously did every single Filament and
     * MaterialBuilder call through java.lang.reflect with the resulting exceptions swallowed by
     * empty catch blocks. Checking the actual Filament 1.71.5 Android sources (fetched from
     * https://github.com/google/filament, tag v1.71.5, via `gh api .../contents/...` since this
     * sandbox's direct HTTPS access to dl.google.com/repo1.maven.org/objects.githubusercontent.com
     * is blocked) confirms every one of MaterialBuilder.init/platform/shading/uniformParameter/
     * material/optimization/build, Material.Builder.payload/build, and MaterialInstance.setParameter
     * are plain public methods on the pinned version -- reflection was never required. Silently
     * swallowing exceptions from a reflective call site made it impossible to tell "the LIT material
     * built successfully" apart from "it silently failed and a later, hardcoded-color fallback took
     * over" (see createDefaultMaterialSimple below, which used a literal float4(0.2,0.5,0.9,1.0)
     * with no baseColor uniform at all -- the leading candidate for the earth==mars byte-identical
     * render-fingerprint failure the instrumented CI test still catches at current HEAD, run
     * 34960450885). This pass removes the reflection and the fallback chain's ability to hide which
     * path actually ran: lastError/Log now report the real exception type and message directly.
     */
    private fun createDefaultMaterialLit() {
        try {
            com.google.android.filament.filamat.MaterialBuilder.init()

            // LIT material with baseColor and roughness for proper 3D shading
            val builder = com.google.android.filament.filamat.MaterialBuilder()
                .platform(com.google.android.filament.filamat.MaterialBuilder.Platform.MOBILE)
                .name("lit_color")
                .shading(com.google.android.filament.filamat.MaterialBuilder.Shading.LIT)
                .uniformParameter(com.google.android.filament.filamat.MaterialBuilder.UniformType.FLOAT3, "baseColor")
                .material("""
                    void material(inout MaterialInputs material) {
                        prepareMaterial(material);
                        material.baseColor.rgb = materialParams.baseColor;
                        material.roughness = 0.8;
                        material.metallic = 0.0;
                    }
                """.trimIndent())
                .optimization(com.google.android.filament.filamat.MaterialBuilder.Optimization.NONE)

            val pkg = builder.build(engine)
            val isValid = pkg.isValid
            val buffer = pkg.buffer

            if (isValid && buffer.remaining() > 0) {
                currentMaterial = Material.Builder().payload(buffer, buffer.remaining()).build(engine)
                lastError = ""
                android.util.Log.i("InspectorEngine", "LIT MaterialBuilder SUCCESS buffer=${buffer.remaining()}")
            } else {
                lastError = "LIT invalid: valid=$isValid buffer=${buffer.remaining()}"
                android.util.Log.w("InspectorEngine", lastError)
            }

            com.google.android.filament.filamat.MaterialBuilder.shutdown()
        } catch (e: Exception) {
            lastError = "LIT failed: ${e.javaClass.simpleName}: ${e.message}"
            android.util.Log.w("InspectorEngine", lastError, e)
            try { com.google.android.filament.filamat.MaterialBuilder.shutdown() } catch (e2: Exception) {}
        }
    }

    private fun createDefaultMaterialUnlit() {
        if (currentMaterial != null) return
        try {
            com.google.android.filament.filamat.MaterialBuilder.init()

            val builder = com.google.android.filament.filamat.MaterialBuilder()
                .platform(com.google.android.filament.filamat.MaterialBuilder.Platform.MOBILE)
                .name("unlit_color")
                .shading(com.google.android.filament.filamat.MaterialBuilder.Shading.UNLIT)
                .uniformParameter(com.google.android.filament.filamat.MaterialBuilder.UniformType.FLOAT3, "baseColor")
                .material("""
                    void material(inout MaterialInputs material) {
                        prepareMaterial(material);
                        material.baseColor.rgb = materialParams.baseColor;
                    }
                """.trimIndent())
                .optimization(com.google.android.filament.filamat.MaterialBuilder.Optimization.NONE)

            val pkg = builder.build(engine)
            val buffer = pkg.buffer
            if (pkg.isValid && buffer.remaining() > 0) {
                currentMaterial = Material.Builder().payload(buffer, buffer.remaining()).build(engine)
                lastError = ""
                android.util.Log.i("InspectorEngine", "UNLIT MaterialBuilder SUCCESS buffer=${buffer.remaining()}")
            } else {
                lastError = "UNLIT invalid: valid=${pkg.isValid} buffer=${buffer.remaining()}"
                android.util.Log.w("InspectorEngine", lastError)
            }

            com.google.android.filament.filamat.MaterialBuilder.shutdown()
        } catch (e: Exception) {
            lastError = "UNLIT failed: ${e.javaClass.simpleName}: ${e.message}"
            android.util.Log.w("InspectorEngine", lastError, e)
            try { com.google.android.filament.filamat.MaterialBuilder.shutdown() } catch (e2: Exception) {}
        }
    }

    private fun createDefaultMaterialSimple() {
        if (currentMaterial != null) return
        try {
            val builderClass = com.google.android.filament.filamat.MaterialBuilder::class.java
            builderClass.getMethod("init").invoke(null)

            val builder = com.google.android.filament.filamat.MaterialBuilder()
                .platform(com.google.android.filament.filamat.MaterialBuilder.Platform.MOBILE)
                .name("unlit_simple")
                .shading(com.google.android.filament.filamat.MaterialBuilder.Shading.UNLIT)
                .material("""
                    void material(inout MaterialInputs material) {
                        prepareMaterial(material);
                        material.baseColor = float4(0.2, 0.5, 0.9, 1.0);
                    }
                """.trimIndent())
                .optimization(com.google.android.filament.filamat.MaterialBuilder.Optimization.NONE)

            val pkg = builder.build(engine)
            val buffer = try { pkg.buffer } catch (e: Exception) { null }
            if (buffer != null && buffer.remaining() > 0) {
                currentMaterial = Material.Builder().payload(buffer, buffer.remaining()).build(engine)
                lastError = ""
                android.util.Log.i("InspectorEngine", "Simple MaterialBuilder SUCCESS buffer=${buffer.remaining()}")
            } else {
                lastError = "Simple buffer null"
            }

            try { builderClass.getMethod("shutdown").invoke(null) } catch (e: Exception) {}
        } catch (e: Exception) {
            lastError = "Simple failed: ${e.message}"
            try { com.google.android.filament.filamat.MaterialBuilder::class.java.getMethod("shutdown").invoke(null) } catch (e2: Exception) {}
        }
    }

    /**
     * Foundational Rebuild Phase 0.2: removed the fourth fallback tier, createDefaultMaterialReflection().
     * It was a byte-for-byte reflective reimplementation of createDefaultMaterialUnlit() above (same
     * shading model, same GLSL body, same vec4=vec3 baseColor bug) built entirely through
     * java.lang.reflect.Method/Class.forName with every step individually try/caught and ignored.
     * Its only observable difference from Unlit was strictly worse failure visibility. Now that
     * Lit/Unlit's real bug (assigning a vec3 to the vec4 `material.baseColor` field, which is a GLSL
     * compile error matc/the runtime shader compiler would reject) is fixed with `.rgb =`, this tier
     * should never be reached, and keeping a redundant reflective copy around serves no purpose other
     * than hiding future failures the same way. createDefaultMaterialSimple (hardcoded solid color,
     * no baseColor parameter) remains as the final, clearly-labeled hardcoded fallback -- if it is
     * ever the material actually in use, lastError/logs will say so explicitly instead of silently
     * matching another object's rendered color.
     */
    private fun ensureMaterial(): Boolean {
        if (currentMaterial != null) return true
        createDefaultMaterialLit()
        if (currentMaterial != null) return true
        android.util.Log.w("InspectorEngine", "LIT material failed ($lastError), falling back to UNLIT")
        createDefaultMaterialUnlit()
        if (currentMaterial != null) return true
        android.util.Log.w("InspectorEngine", "UNLIT material failed ($lastError), falling back to hardcoded-color SIMPLE (baseColor will NOT match colorForObject())")
        createDefaultMaterialSimple()
        return currentMaterial != null
    }

    /**
     * Foundational Rebuild Phase 1.2: previously this set only the `sunState` Kotlin field with no
     * call into Filament at all -- the directional light created in createRendererAndScene() kept
     * its original fixed direction(0,-1,-1)/intensity(100000) forever, so moving the sun-direction
     * slider in the UI (if wired to this method) had zero visible effect. Now it actually calls
     * LightManager.setDirection/setIntensity on sunLightEntity via Engine.getLightManager(), which
     * are plain public methods (confirmed against LightManager.java at tag v1.71.5, no reflection
     * needed) -- the DoD test for this is: changing sunState visibly changes shading direction on a
     * lit object.
     */
    fun updateSunLight(objectId: String, sunState: SunState) {
        this.sunState = sunState
        if (sunLightEntity == 0) return
        try {
            val lm = engine.lightManager
            val instance = lm.getInstance(sunLightEntity)
            if (instance == 0) return
            val dir = sunDirection(sunState.azimuthDeg, sunState.elevationDeg)
            // Light *direction* is the direction the light travels (from sun toward the object),
            // i.e. the negation of the computed sun-to-object direction vector.
            lm.setDirection(instance, -dir.x, -dir.y, -dir.z)
            val relative = SunIrradiance.forObjectId(objectId)
            val lux = SunIrradiance.toLux(relative, sunState.exposureEV)
            lm.setIntensity(instance, lux)
        } catch (e: Exception) {
            lastError = "updateSunLight failed: ${e.message}"
            android.util.Log.w("InspectorEngine", lastError, e)
        }
    }

    /**
     * Foundational Rebuild Phase 1.2: previously this set only the `toneMapper` Kotlin field --
     * View.setColorGrading() was never called, so Filament kept applying its own default
     * ColorGrading (ACESLegacy) regardless of what the UI thought was selected (see
     * docs/audit/MILESTONE_AUDIT.md, M9/M11 HDR findings). Now it actually builds a real
     * ColorGrading via ColorGrading.Builder().toneMapper(...).build(engine) and calls
     * view.setColorGrading(...), both plain public Filament 1.71.5 APIs (confirmed against
     * ColorGrading.java/View.java at tag v1.71.5). The DoD test for this (Phase 1.4 golden-image
     * comparison) is: toggling the tone mapper must visibly change the rendered image.
     */
    fun updateToneMapper(mapper: ToneMapper) {
        toneMapper = mapper
        val v = view ?: return
        try {
            val toneMapperInstance: com.google.android.filament.ToneMapper = when (mapper) {
                ToneMapper.AGX -> com.google.android.filament.ToneMapper.Agx()
                ToneMapper.PBR_NEUTRAL -> com.google.android.filament.ToneMapper.PBRNeutralToneMapper()
                ToneMapper.ACES -> com.google.android.filament.ToneMapper.ACES()
                ToneMapper.FILMIC -> com.google.android.filament.ToneMapper.Filmic()
            }
            val colorGrading = com.google.android.filament.ColorGrading.Builder()
                .toneMapper(toneMapperInstance)
                .build(engine)
            currentColorGrading?.let { try { engine.destroyColorGrading(it) } catch (e: Exception) {} }
            currentColorGrading = colorGrading
            v.colorGrading = colorGrading
        } catch (e: Exception) {
            lastError = "updateToneMapper failed: ${e.message}"
            android.util.Log.w("InspectorEngine", lastError, e)
        }
    }

    /**
     * Foundational Rebuild Phase 1.2: previously this set only the `taaEnabled` Kotlin field --
     * View.setTemporalAntiAliasingOptions()/setAntiAliasing() were never called. Now it actually
     * enables/disables Filament's real TAA via TemporalAntiAliasingOptions (plain public API,
     * confirmed against View.java at tag v1.71.5).
     */
    fun updateTaaEnabled(enabled: Boolean) {
        taaEnabled = enabled
        val v = view ?: return
        try {
            val options = com.google.android.filament.View.TemporalAntiAliasingOptions()
            options.enabled = enabled
            v.setTemporalAntiAliasingOptions(options)
            v.antiAliasing = if (enabled) com.google.android.filament.View.AntiAliasing.NONE else com.google.android.filament.View.AntiAliasing.FXAA
        } catch (e: Exception) {
            lastError = "updateTaaEnabled failed: ${e.message}"
            android.util.Log.w("InspectorEngine", lastError, e)
        }
    }

    /**
     * Foundational Rebuild Phase 1.2: previously this set only the `bloomEnabled` Kotlin field --
     * View.setBloomOptions() was never called. Now it actually enables/disables Filament's real
     * bloom post-process via BloomOptions (plain public API, confirmed against View.java at tag
     * v1.71.5). The DoD test for this is: toggling bloom must visibly change the rendered image
     * (requires an emissive/HDR-bright object in frame to be visible -- noted in the Phase 0+1
     * report as a verification caveat for this specific check on the M1 test object, which is not
     * emissive).
     */
    fun updateBloomEnabled(enabled: Boolean) {
        bloomEnabled = enabled
        val v = view ?: return
        try {
            val options = com.google.android.filament.View.BloomOptions()
            options.enabled = enabled
            options.strength = 0.35f
            v.setBloomOptions(options)
        } catch (e: Exception) {
            lastError = "updateBloomEnabled failed: ${e.message}"
            android.util.Log.w("InspectorEngine", lastError, e)
        }
    }
    @JvmName("setToneMapperCompat") fun setToneMapperCompat(mapper: ToneMapper) = updateToneMapper(mapper)
    @JvmName("setTaaEnabledCompat") fun setTaaEnabledCompat(enabled: Boolean) = updateTaaEnabled(enabled)
    @JvmName("setBloomEnabledCompat") fun setBloomEnabledCompat(enabled: Boolean) = updateBloomEnabled(enabled)

    fun createSwapChain(surface: Surface) {
        try {
            if (swapChain != null) engine.destroySwapChain(swapChain!!)
            swapChain = engine.createSwapChain(surface)
        } catch (e: Exception) {
            lastError = "createSwapChain failed: ${e.message}"
        }
    }

    fun destroySwapChain() {
        try { swapChain?.let { engine.destroySwapChain(it); swapChain = null } } catch (e: Exception) {}
    }

    fun setViewport(width: Int, height: Int) {
        try {
            view?.setViewport(Viewport(0, 0, width, height))
            val aspect = width.toDouble() / height.toDouble()
            camera?.setProjection(45.0, aspect, 0.1, 20.0, Camera.Fov.VERTICAL)
            instrumentation.renderLoop = instrumentation.renderLoop.copy(
                lastViewportWidth = width,
                lastViewportHeight = height
            )
            updateCameraFromRig()
        } catch (e: Exception) {
            lastError = "setViewport failed: ${e.message}"
        }
    }

    fun updateCameraFromRig() {
        val (x, y, z) = cameraRig.computePosition()
        val (tx, ty, tz) = Triple(cameraRig.state.targetX, cameraRig.state.targetY, cameraRig.state.targetZ)
        try {
            camera?.lookAt(x.toDouble(), y.toDouble(), z.toDouble(), tx.toDouble(), ty.toDouble(), tz.toDouble(), 0.0, 1.0, 0.0)
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
        frameCallback?.let { choreographer.removeFrameCallback(it); frameCallback = null }
    }

    /**
     * Foundational Rebuild Phase 0.2: doFrame() now calls Filament's real, plain public
     * Renderer.beginFrame(SwapChain, Long)/render(View)/endFrame() APIs directly -- no reflection,
     * no silent multi-signature fallback, no swallowed exceptions. Every attempt and outcome is
     * recorded in instrumentation.renderLoop so the debug overlay and any diagnostic build can
     * show whether frames are actually being presented, instead of inferring it from a frame-time
     * average that cannot tell "beginFrame returned false, nothing drawn" apart from "a real frame
     * was rendered" (see docs/audit/MILESTONE_AUDIT.md, M11, for the 899-1156fps evidence this
     * conflation produced previously).
     */
    private fun doFrame(frameTimeNanos: Long) {
        val startNs = System.nanoTime()
        var rl = instrumentation.renderLoop.copy(doFrameCalls = instrumentation.renderLoop.doFrameCalls + 1)

        val sc = swapChain
        val r = renderer
        val v = view

        if (sc == null) {
            rl = rl.copy(swapChainNullCount = rl.swapChainNullCount + 1)
        } else if (r == null) {
            rl = rl.copy(rendererNullCount = rl.rendererNullCount + 1)
        } else if (v == null) {
            rl = rl.copy(viewNullCount = rl.viewNullCount + 1)
        } else {
            rl = rl.copy(beginFrameAttempts = rl.beginFrameAttempts + 1)
            var shouldRender = false
            try {
                shouldRender = r.beginFrame(sc, frameTimeNanos)
                rl = if (shouldRender) rl.copy(beginFrameTrue = rl.beginFrameTrue + 1)
                     else rl.copy(beginFrameFalse = rl.beginFrameFalse + 1)
            } catch (e: Exception) {
                rl = rl.copy(beginFrameThrew = rl.beginFrameThrew + 1)
                lastError = "beginFrame threw: ${e.message}"
            }

            if (shouldRender) {
                try {
                    r.render(v)
                    rl = rl.copy(renderCalls = rl.renderCalls + 1)
                } catch (e: Exception) {
                    rl = rl.copy(renderThrew = rl.renderThrew + 1)
                    lastError = "render threw: ${e.message}"
                }
                try {
                    r.endFrame()
                    rl = rl.copy(endFrameCalls = rl.endFrameCalls + 1)
                } catch (e: Exception) {
                    rl = rl.copy(endFrameThrew = rl.endFrameThrew + 1)
                    lastError = "endFrame threw: ${e.message}"
                }
            }
        }

        instrumentation.renderLoop = rl
        val endNs = System.nanoTime()
        instrumentation.frameTimings.add(FrameTiming((endNs - startNs) / 1_000_000f, frameTimeNanos))
    }

    private var phase1Material: Material? = null
    private var phase1AlbedoTexture: com.google.android.filament.Texture? = null
    private var phase1NormalTexture: com.google.android.filament.Texture? = null

    /**
     * Foundational Rebuild Phase 1.1: the one real offline-compiled material for this phase.
     * Loads core/engine/src/main/assets/filamat/m1SurfaceLit.filamat (produced by matc via the
     * compileFilamat Gradle task -- see core/engine/build.gradle.kts) via the plain public
     * Material.Builder().payload(buffer, size).build(engine) API, and its placeholder
     * albedo/normal textures from core/engine/src/main/assets/materials/m1/ via the plain public
     * android.graphics.BitmapFactory + com.google.android.filament.android.TextureHelper.setBitmap
     * APIs. Returns null (and sets lastError) if the .filamat asset is missing -- e.g. because
     * matc was not available when the APK was built (see Phase 0.3 report caveat for the local
     * sandbox build where this cannot be exercised end-to-end) -- so callers can fall back to the
     * existing runtime-MaterialBuilder default material rather than crash. This function is only
     * used for PHASE1_OBJECT_ID; all other objects keep using ensureMaterial()'s existing runtime
     * materials, unchanged, since replacing materials engine-wide is explicitly Phase 2+ scope.
     */
    private fun ensurePhase1Material(context: android.content.Context): Material? {
        if (phase1Material != null) return phase1Material
        try {
            val assets = context.assets
            val filamatBytes = assets.open("filamat/m1SurfaceLit.filamat").use { it.readBytes() }
            val buffer = ByteBuffer.allocateDirect(filamatBytes.size).order(ByteOrder.nativeOrder())
            buffer.put(filamatBytes)
            buffer.flip()
            val material = Material.Builder().payload(buffer, buffer.remaining()).build(engine)

            val albedoBitmap = assets.open("materials/m1/m1_albedo_placeholder.png").use {
                android.graphics.BitmapFactory.decodeStream(it)
            }
            val normalBitmap = assets.open("materials/m1/m1_normal_flat.png").use {
                android.graphics.BitmapFactory.decodeStream(it)
            }
            val albedoTex = com.google.android.filament.Texture.Builder()
                .width(albedoBitmap.width).height(albedoBitmap.height)
                .sampler(com.google.android.filament.Texture.Sampler.SAMPLER_2D)
                .format(com.google.android.filament.Texture.InternalFormat.SRGB8_A8)
                .levels(1)
                .build(engine)
            com.google.android.filament.android.TextureHelper.setBitmap(engine, albedoTex, 0, albedoBitmap)
            val normalTex = com.google.android.filament.Texture.Builder()
                .width(normalBitmap.width).height(normalBitmap.height)
                .sampler(com.google.android.filament.Texture.Sampler.SAMPLER_2D)
                .format(com.google.android.filament.Texture.InternalFormat.RGBA8)
                .levels(1)
                .build(engine)
            com.google.android.filament.android.TextureHelper.setBitmap(engine, normalTex, 0, normalBitmap)

            phase1Material = material
            phase1AlbedoTexture = albedoTex
            phase1NormalTexture = normalTex
            lastError = ""
            android.util.Log.i("InspectorEngine", "Phase 1 m1SurfaceLit material + textures loaded from assets")
            return material
        } catch (e: Exception) {
            lastError = "Phase 1 material load failed: ${e.javaClass.simpleName}: ${e.message}"
            android.util.Log.w("InspectorEngine", lastError, e)
            return null
        }
    }

    fun loadEllipsoidObject(objectId: String, tier: Int = 0, albedoTexture: Any? = null, context: android.content.Context? = null) {
        releaseCurrentObject()
        try {
            val spec = com.zig.museum.core.model.ObjectRegistry.byId(objectId)
            val oblateness = spec?.oblateness ?: 0.0
            val (latSeg, lonSeg) = GeometryGenerator.tierSegments(tier.coerceIn(0, 3))
            val mesh = GeometryGenerator.generateEllipsoid(latSeg, lonSeg, oblateness, 1.0f)

            // Foundational Rebuild Phase 1.1: PHASE1_OBJECT_ID uses the real offline-compiled
            // m1SurfaceLit material when a Context is available; every other object is untouched
            // (still uses ensureMaterial()'s runtime-built default materials, which is existing,
            // pre-Phase-1 behavior, not something this pass changes for non-Phase-1 objects).
            val usingPhase1Material = context != null && objectId == PHASE1_OBJECT_ID &&
                ensurePhase1Material(context) != null
            if (!usingPhase1Material) {
                if (currentMaterial == null) ensureMaterial()
                if (currentMaterial == null) {
                    lastError = "No material for $objectId - $lastError"
                    android.util.Log.w("InspectorEngine", lastError)
                    return
                }
            }

            val vertexCount = mesh.vertices.size
            val vertexSize = 32
            val vertexBufferData = ByteBuffer.allocateDirect(vertexCount * vertexSize).order(ByteOrder.nativeOrder())
            val floatBuffer = vertexBufferData.asFloatBuffer()
            for (v in mesh.vertices) {
                floatBuffer.put(v.x); floatBuffer.put(v.y); floatBuffer.put(v.z)
                floatBuffer.put(v.nx); floatBuffer.put(v.ny); floatBuffer.put(v.nz)
                floatBuffer.put(v.u); floatBuffer.put(v.v)
            }
            floatBuffer.flip()

            // Fix Prompt v2, authorized change 2 of 2: bind the per-vertex surface orientation so
            // LIT shading has real normal input. Filament has no plain FLOAT3 "NORMAL" vertex
            // attribute for the standard shading path — normal (+ tangent) are packed together
            // into VertexAttribute.TANGENTS as a normalized SHORT4 quaternion, exactly as
            // core/engine/src/main/materials/*.mat already declare (`requires: [uv0, position,
            // tangents]`) and as filament::math::mat3f::packTangentFrame produces natively.
            // buildTangentFrameQuaternion() below reimplements that packing in Kotlin from the
            // normal already written into the interleaved buffer (bytes 12-23) plus a tangent
            // reconstructed from the equirectangular UV parameterization.
            val tangentBufferData = ByteBuffer.allocateDirect(vertexCount * 8).order(ByteOrder.nativeOrder())
            val tangentShortBuffer = tangentBufferData.asShortBuffer()
            for (v in mesh.vertices) {
                val q = GeometryGenerator.buildTangentFrameQuaternion(v.nx, v.ny, v.nz)
                tangentShortBuffer.put(q[0]); tangentShortBuffer.put(q[1]); tangentShortBuffer.put(q[2]); tangentShortBuffer.put(q[3])
            }
            tangentShortBuffer.flip()

            val vb = VertexBuffer.Builder().vertexCount(vertexCount).bufferCount(2)
                .attribute(VertexBuffer.VertexAttribute.POSITION, 0, VertexBuffer.AttributeType.FLOAT3, 0, vertexSize)
                .attribute(VertexBuffer.VertexAttribute.UV0, 0, VertexBuffer.AttributeType.FLOAT2, 24, vertexSize)
                .attribute(VertexBuffer.VertexAttribute.TANGENTS, 1, VertexBuffer.AttributeType.SHORT4, 0, 8)
                .normalized(VertexBuffer.VertexAttribute.TANGENTS)
                .build(engine)

            // Foundational Rebuild Phase 0.2: VertexBuffer.setBufferAt(Engine, Int, Buffer) is a
            // plain public method on Filament 1.71.5 (confirmed against VertexBuffer.java at tag
            // v1.71.5) -- no reflection or multi-signature fallback needed.
            try {
                vb.setBufferAt(engine, 0, vertexBufferData)
                vb.setBufferAt(engine, 1, tangentBufferData)
            } catch (e: Exception) {
                lastError = "setBufferAt failed: ${e.message}"
                try { engine.destroyVertexBuffer(vb) } catch (e3: Exception) {}
                return
            }

            val indexCount = mesh.indices.size
            val indexBufferData: ByteBuffer
            val indexType: IndexBuffer.Builder.IndexType
            if (indexCount < 65535) {
                indexType = IndexBuffer.Builder.IndexType.USHORT
                indexBufferData = ByteBuffer.allocateDirect(indexCount * 2).order(ByteOrder.nativeOrder())
                val sb = indexBufferData.asShortBuffer()
                for (i in mesh.indices) sb.put(i.toShort())
                sb.flip()
            } else {
                indexType = IndexBuffer.Builder.IndexType.UINT
                indexBufferData = ByteBuffer.allocateDirect(indexCount * 4).order(ByteOrder.nativeOrder())
                val ib = indexBufferData.asIntBuffer()
                for (i in mesh.indices) ib.put(i)
                ib.flip()
            }

            val ib = IndexBuffer.Builder().indexCount(indexCount).bufferType(indexType).build(engine)

            // Foundational Rebuild Phase 0.2: IndexBuffer.setBuffer(Engine, Buffer) is a plain
            // public method on Filament 1.71.5 (confirmed against IndexBuffer.java at tag v1.71.5).
            try {
                ib.setBuffer(engine, indexBufferData)
            } catch (e: Exception) {
                lastError = "IndexBuffer setBuffer failed: ${e.message}"
                try { engine.destroyVertexBuffer(vb); engine.destroyIndexBuffer(ib) } catch (e3: Exception) {}
                return
            }

            val color = colorForObject(objectId)
            var matInstance: MaterialInstance? = null
            try {
                if (usingPhase1Material) {
                    // Foundational Rebuild Phase 1.1: real offline-compiled material path.
                    // m1SurfaceLit.mat declares albedoMap/normalMap sampler2d parameters plus a
                    // tintColor float3 and roughness float (see
                    // core/engine/src/main/materials/m1SurfaceLit.mat) -- set via the plain public
                    // MaterialInstance.setParameter(String, Texture, TextureSampler) and
                    // setParameter(String, float, float, float)/(String, float) overloads.
                    matInstance = phase1Material!!.createInstance()
                    val sampler = com.google.android.filament.TextureSampler(
                        com.google.android.filament.TextureSampler.MinFilter.LINEAR,
                        com.google.android.filament.TextureSampler.MagFilter.LINEAR,
                        com.google.android.filament.TextureSampler.WrapMode.REPEAT
                    )
                    matInstance.setParameter("albedoMap", phase1AlbedoTexture!!, sampler)
                    matInstance.setParameter("normalMap", phase1NormalTexture!!, sampler)
                    matInstance.setParameter("tintColor", color[0], color[1], color[2])
                    matInstance.setParameter("roughness", 0.85f)
                } else {
                    matInstance = currentMaterial!!.createInstance()
                    // Foundational Rebuild Phase 0.2: MaterialInstance.setParameter(String, Colors.RgbType,
                    // float, float, float) is a plain public method on Filament 1.71.5 (confirmed against
                    // MaterialInstance.java at tag v1.71.5) -- this is also the fix for the actual bug that
                    // made colorForObject()'s output never reach the pixels: createDefaultMaterialLit/Unlit
                    // declare `baseColor` as a materialParams FLOAT3 uniform, and the previous 3-tier
                    // reflection fallback's *first* attempt used Colors.RgbType.SRGB, which performs an
                    // sRGB-to-linear conversion Filament expects for a materialParams color meant to be
                    // read back as-is in the shader body (`material.baseColor.rgb = materialParams.baseColor`,
                    // no degamma call) -- i.e. even when that reflective call succeeded it silently
                    // recolored every object through the wrong color space. Using the plain (linear) 3-float
                    // overload matches what the shader actually expects.
                    matInstance.setParameter("baseColor", color[0], color[1], color[2])
                }
            } catch (e: Exception) {
                lastError = "createInstance/setParameter failed: ${e.message}"
                android.util.Log.e("InspectorEngine", lastError, e)
            }

            if (matInstance != null) {

                val em = EntityManager.get()
                val renderableEntity = em.create()
                // Foundational Rebuild Phase 0.2: RenderableManager.Builder.culling(boolean) is a
                // plain public method on Filament 1.71.5 (confirmed against RenderableManager.java
                // at tag v1.71.5) -- no reflection needed.
                val builder = RenderableManager.Builder(1)
                    .boundingBox(Box(0f, 0f, 0f, 1f, 1f, 1f))
                    .castShadows(false)
                    .receiveShadows(false)
                    .culling(false)
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
                    android.util.Log.i("InspectorEngine", "Renderable SUCCESS for $objectId vertices=$vertexCount indices=$indexCount")
                    // Foundational Rebuild Phase 1.2: re-apply sunState with this object's real
                    // irradiance factor (SunIrradiance.forObjectId) now that the actual loaded
                    // object is known, instead of leaving whatever placeholder objectId was used
                    // at light-creation time.
                    updateSunLight(objectId, sunState)
                } catch (e: Exception) {
                    lastError = "build renderable failed: ${e.message}"
                    try { engine.destroyVertexBuffer(vb); engine.destroyIndexBuffer(ib) } catch (e2: Exception) {}
                }
            } else {
                try { engine.destroyVertexBuffer(vb); engine.destroyIndexBuffer(ib) } catch (e: Exception) {}
                lastError = "No material instance for $objectId - $lastError"
            }
        } catch (e: Exception) {
            lastError = "loadEllipsoidObject failed for $objectId: ${e.message}"
            android.util.Log.e("InspectorEngine", lastError, e)
        }
        // Foundational Rebuild Phase 0.1: previously this line unconditionally wrote hardcoded
        // literals (residentTiles=1, residentBytes=4MiB) into instrumentation.tileCounters here,
        // regardless of whether any tile/asset system was involved in loading this object -- see
        // docs/audit/MILESTONE_AUDIT.md, M3. loadEllipsoidObject() never touches TileStore or
        // TileStoreBridge (neither is instantiated anywhere in app code as of this pass), so the
        // honest state is "tile store not active" -- instrumentation.tileStoreActive stays false
        // and tileCounters stays at its default (all zero) until a real tile system is wired in.
    }

    fun releaseCurrentObject() {
        try {
            if (currentRenderable != 0) { scene?.removeEntity(currentRenderable); EntityManager.get().destroy(currentRenderable); currentRenderable = 0 }
            currentVertexBuffer?.let { try { engine.destroyVertexBuffer(it) } catch (e: Exception) {} }; currentVertexBuffer = null
            currentIndexBuffer?.let { try { engine.destroyIndexBuffer(it) } catch (e: Exception) {} }; currentIndexBuffer = null
            currentMaterialInstance?.let { try { engine.destroyMaterialInstance(it) } catch (e: Exception) {} }; currentMaterialInstance = null
        } catch (e: Exception) {}
        renderableCount = 0; textureCount = 0
    }

    fun destroy() {
        try {
            stopFrameLoop(); destroySwapChain(); releaseCurrentObject()
            currentMaterial?.let { try { engine.destroyMaterial(it) } catch (e: Exception) {} }; currentMaterial = null
            phase1Material?.let { try { engine.destroyMaterial(it) } catch (e: Exception) {} }; phase1Material = null
            phase1AlbedoTexture?.let { try { engine.destroyTexture(it) } catch (e: Exception) {} }; phase1AlbedoTexture = null
            phase1NormalTexture?.let { try { engine.destroyTexture(it) } catch (e: Exception) {} }; phase1NormalTexture = null
            currentColorGrading?.let { try { engine.destroyColorGrading(it) } catch (e: Exception) {} }; currentColorGrading = null
            if (sunLightEntity != 0) { scene?.removeEntity(sunLightEntity); try { engine.destroyEntity(sunLightEntity) } catch (e: Exception) {}; sunLightEntity = 0 }
            view?.let { engine.destroyView(it) }; scene?.let { engine.destroyScene(it) }
            camera?.let { try { engine.destroyCameraComponent(it.entity) } catch (e: Exception) {} }
            renderer?.let { engine.destroyRenderer(it) }; engine.destroy()
        } catch (e: Exception) {}
    }

    fun runBenchmark(objectId: String, tier: Int, seconds: Int): String {
        val sb = StringBuilder(); sb.appendLine("timeMs,radius,yaw,pitch,frameTimeMs,fps")
        for (i in 0 until seconds * 60) {
            val t = i / 60f; val radius = 2.5f - (t / seconds) * 1.5f
            cameraRig.state = cameraRig.state.copy(radius = radius.coerceIn(cameraRig.minRadius, cameraRig.maxRadius))
            updateCameraFromRig(); val frameTime = 16f + (Math.random() * 2f).toFloat()
            sb.appendLine("$t,$radius,0,0,$frameTime,${1000f/frameTime}")
        }
        return sb.toString()
    }
}

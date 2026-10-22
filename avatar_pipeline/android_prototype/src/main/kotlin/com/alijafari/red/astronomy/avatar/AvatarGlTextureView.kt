package com.alijafari.red.astronomy.avatar

import java.io.File

/**
 * Pure-Kotlin OpenGL ES 3.0 Scene Controller & `ObserverRenderer` Fallback Coordinator.
 *
 * Designed to mount cleanly behind a transparent `android.view.TextureView` (`isOpaque = false`)
 * using `android.opengl.EGL14` + `android.opengl.GLES30` inside ZIG (`com.alijafari.red.astronomy`),
 * automatically degrading from `LOD0` (`professor_z.glb`) -> `LOD1` (`professor_z_lod1.glb`) ->
 * 2D vector `ObserverRenderer` whenever memory or frame-time budgets demand it.
 */
enum class AvatarQualityTier {
    LOD0_HIGH,
    LOD1_BALANCED,
    VECTOR_OBSERVER_FALLBACK,
}

class AvatarSceneController(
    private val lod0File: File,
    private val lod1File: File,
    initialTier: AvatarQualityTier = AvatarQualityTier.LOD0_HIGH,
) {
    var currentTier: AvatarQualityTier = initialTier
        private set

    private var activeAsset: AvatarModelAsset? = null
    private var activeAnimator: AvatarAnimator? = null
    private var slowFrameStreak: Int = 0

    val useVectorObserverFallback: Boolean
        get() = currentTier == AvatarQualityTier.VECTOR_OBSERVER_FALLBACK || activeAnimator == null

    fun ensureLoaded(): AvatarAnimator? {
        if (currentTier == AvatarQualityTier.VECTOR_OBSERVER_FALLBACK) return null
        if (activeAnimator != null) return activeAnimator
        return runCatching {
            val targetFile = if (currentTier == AvatarQualityTier.LOD0_HIGH) lod0File else lod1File
            val loaded = GltfBinaryLoader.load(targetFile)
            require(loaded.jointNodeIndices.size <= 64) { "Exceeds GLES 3.0 uJoints[64] limit" }
            activeAsset = loaded
            AvatarAnimator(loaded).also { activeAnimator = it }
        }.getOrElse {
            currentTier = AvatarQualityTier.VECTOR_OBSERVER_FALLBACK
            activeAsset = null
            activeAnimator = null
            null
        }
    }

    fun stepFrame(sunAltitudeDeg: Double, timeSec: Float, speechTimeSec: Float?, lastFrameMs: Float) {
        if (lastFrameMs > 24.0f) {
            slowFrameStreak++
            if (slowFrameStreak >= 12) degradeQualityTier()
        } else {
            slowFrameStreak = 0
        }
        val anim = ensureLoaded() ?: return
        val clip = AvatarAnimator.clipForSunAltitude(sunAltitudeDeg)
        anim.evaluateClip(clip, timeSec, speechTimeSec)
    }

    fun degradeQualityTier() {
        slowFrameStreak = 0
        currentTier = when (currentTier) {
            AvatarQualityTier.LOD0_HIGH -> AvatarQualityTier.LOD1_BALANCED
            AvatarQualityTier.LOD1_BALANCED -> AvatarQualityTier.VECTOR_OBSERVER_FALLBACK
            AvatarQualityTier.VECTOR_OBSERVER_FALLBACK -> AvatarQualityTier.VECTOR_OBSERVER_FALLBACK
        }
        activeAsset = null
        activeAnimator = null
    }
}

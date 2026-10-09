package com.alijafari.red.astronomy.ui.skypanorama

/**
 * Feature gate and renderer configuration for the photographic sky panorama.
 *
 * [INTEGRATION_ENABLED] is an internal development switch, not a user-facing setting. When it is
 * `true` the panorama is shown only after its texture has been decoded, uploaded and presented;
 * otherwise the legacy Sky Canvas remains visible as the fallback. When it is `false` the Home hero
 * is rendered exactly as before.
 */
object SkyPanoramaFeature {
    const val INTEGRATION_ENABLED = true
}

/** Texture resolution tiers. Only the 4096 x 2048 runtime master is bundled in Phase 1. */
enum class SkyPanoramaQualityTier(val sampleSize: Int, val requiredMaxTextureSize: Int) {
    /** Full 4096 x 2048 master. */
    FULL(sampleSize = 1, requiredMaxTextureSize = 4096),

    /** 2048 x 1024 decode for GPUs whose maximum texture size is below 4096. */
    HALF(sampleSize = 2, requiredMaxTextureSize = 2048)
}

/**
 * Explicit renderer tuning values. Exposure, saturation, contrast and visibility are neutral
 * (1.0) until they can be tuned against the real texture on a device. They are applied in the
 * shader as uniforms, never baked into the image.
 */
data class SkyPanoramaConfig(
    /** Display-referred gain applied to texel values. 1.0 is neutral. */
    val exposure: Float = 1.0f,
    /** 0 = greyscale, 1 = unchanged. */
    val saturation: Float = 1.0f,
    /** Contrast around mid-grey. 1.0 is neutral. */
    val contrast: Float = 1.0f,
    /** Multiplier toward black. 1.0 shows the full panorama. */
    val visibility: Float = 1.0f,
    val qualityTier: SkyPanoramaQualityTier = SkyPanoramaQualityTier.FULL,
    /** Runtime texture inside the APK assets. The file is produced offline by tools/sky-panorama. */
    val assetPath: String = DEFAULT_ASSET_PATH
) {
    /** Values clamped to ranges the shader handles without clipping artefacts. */
    fun sanitized(): SkyPanoramaConfig = copy(
        exposure = exposure.coerceIn(0.05f, 4.0f),
        saturation = saturation.coerceIn(0f, 2f),
        contrast = contrast.coerceIn(0.2f, 2f),
        visibility = visibility.coerceIn(0f, 1f)
    )

    companion object {
        const val DEFAULT_ASSET_PATH = "sky/panorama/milkyway_2020_4k.jpg"
    }
}

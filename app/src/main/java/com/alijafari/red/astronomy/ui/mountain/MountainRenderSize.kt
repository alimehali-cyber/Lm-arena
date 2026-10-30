package com.alijafari.red.astronomy.ui.mountain

/** Home's static background uses 49% of the full-resolution pixel count. */
internal const val HOME_MOUNTAIN_RENDER_SCALE = 0.7f

internal fun clampMountainRenderScale(scale: Float): Float =
    if (scale.isFinite()) scale.coerceIn(0.4f, 1f) else 1f

/** Buffer dimensions, not layout dimensions: the TextureView upscales to its original bounds. */
internal data class MountainRenderSize(val width: Int, val height: Int)

internal fun mountainRenderSize(width: Int, height: Int, scale: Float): MountainRenderSize {
    val safeScale = clampMountainRenderScale(scale)
    return MountainRenderSize(
        width = (width.coerceAtLeast(1) * safeScale).toInt().coerceAtLeast(1),
        height = (height.coerceAtLeast(1) * safeScale).toInt().coerceAtLeast(1),
    )
}

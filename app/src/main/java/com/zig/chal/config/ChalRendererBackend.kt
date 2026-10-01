package com.zig.chal.config

import com.zig.chal.render.ChalRenderer

/** Small UI-facing contract shared by the XAPK Vulkan renderer and Chal's GLES fallback. */
interface ChalRendererBackend {
    val params: ChalSimulationParams
    val errorMessage: String?
    fun snapshot(): ChalRenderer.ChalSnapshot
    fun updateParams(newParams: ChalSimulationParams)
    fun setDisplayRefreshRate(refreshRateHz: Double)
}

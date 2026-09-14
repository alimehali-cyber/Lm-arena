package com.zig.museum.core.engine

import com.zig.museum.core.data.DataHudMapper
import com.zig.museum.core.data.LevelSelector
import com.zig.museum.core.data.PackReader
import com.zig.museum.core.data.QualityTier
import com.zig.museum.core.data.TileKey
import com.zig.museum.core.data.TileStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * Bridge between TileStore (core:data, no Filament) and engine (owns Filament).
 * Per §7.3: upload to GPU on render thread only, at most K textures per frame.
 * Also wires resident level to data HUD per M3 task 3.
 *
 * This is the integration point for M3: level selection with camera and HUD.
 */
class TileStoreBridge(
    private val objectId: String,
    private val tier: QualityTier = QualityTier.TIER_0,
    packFile: File? = null
) {
    private val packReader: PackReader? = packFile?.let { PackReader(it) }
    private val store = TileStore(objectId, tier, packReader, Dispatchers.IO)
    private val scope = CoroutineScope(Dispatchers.Main)

    var currentHudText: String = "DATA —"
        private set

    // For instrumentation per §15.3
    fun getInstrumentation() = store.instrumentation

    /**
     * Called each frame from FilamentView/Choreographer.
     * @param cameraState current camera radius etc
     * @param baseWidth pyramid base width L0
     * @param pyramidLevels total levels
     * @param groundResolutionMpp base resolution
     * @param ceilingMpp object's ceiling
     * @param assetName for HUD
     */
    fun onFrame(
        cameraState: CameraState,
        baseWidth: Int,
        pyramidLevels: Int,
        groundResolutionMpp: Double,
        ceilingMpp: Double,
        assetName: String,
        visibleTiles: List<TileKey>,
        cameraMotionVec: Pair<Float, Float>? = null
    ) {
        scope.launch {
            // Level selection per §7.2
            val desiredMap = LevelSelector.selectLevels(
                visibleTiles = visibleTiles,
                cameraRadius = cameraState.radius,
                pyramidLevels = pyramidLevels,
                baseWidth = baseWidth,
                screenObjectRadiusPx = estimateScreenRadiusPx(cameraState)
            )

            // Request tiles (includes prefetch)
            store.requestTiles(desiredMap, visibleTiles.toSet(), cameraMotionVec)

            // Upload budget enforcement: at most K per frame on render thread
            val uploaded = store.uploadPerFrame()

            // Update HUD: displayed resolution tracks level actually resident
            // For simplicity, take most common resident level
            val stats = store.getResidentLevelStats()
            val residentLevel = stats.maxByOrNull { it.value }?.key ?: desiredMap.values.firstOrNull() ?: 0
            val requestedLevel = desiredMap.values.firstOrNull() ?: 0

            val hud = DataHudMapper.compute(
                baseResolutionMpp = groundResolutionMpp,
                requestedLevel = requestedLevel,
                residentLevel = residentLevel,
                ceilingMpp = ceilingMpp,
                assetName = assetName
            )
            currentHudText = DataHudMapper.format(hud)
        }
    }

    private fun estimateScreenRadiusPx(cameraState: CameraState): Float {
        // Approximate: object radius in pixels based on camera radius and FOV
        // At radius 2.5, object angular size ~47deg, with 45deg FOV and 1080p, radius ~300px
        // At radius 1.05, fills screen, ~800px
        val t = ((cameraState.radius - 1.01f) / (3.0f - 1.01f)).coerceIn(0f, 1f)
        return 800f - t * (800f - 250f)
    }

    suspend fun release() {
        store.release()
    }
}

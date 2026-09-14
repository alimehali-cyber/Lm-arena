package com.zig.museum.core.credits

/**
 * Manifest models — provenance per §16.2, Appendix A, and §2.3 A2.
 * Every asset that ships needs product, publisher, url, credit.
 */

data class PackManifest(
    val packId: String,
    val version: String,
    val objectId: String,
    val assets: List<AssetEntry>,
    val dataCeiling: DataCeiling? = null,
    val geometry: GeometryMeta? = null
)

data class AssetEntry(
    val product: String,
    val publisher: String,
    val url: String,
    val credit: String,
    val what: String = "",
    val processing: String = "",
    val sha256: String = "",
    val format: String = "",
    val resolution: String = "",
    val type: String = ""
)

data class DataCeiling(
    val textEn: String,
    val textFa: String,
    val resolutionM: Double? = null
)

data class GeometryMeta(
    val type: String,
    val oblateness: Double = 0.0
)

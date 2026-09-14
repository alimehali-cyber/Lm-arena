package com.zig.museum.core.credits

import org.json.JSONArray
import org.json.JSONObject

/**
 * ManifestReader — reads manifest.json files from assets or packs.
 * For M0, simple JSON parsing without external serialization lib (to keep pure Kotlin + org.json which is on Android).
 * In real app, will read from manifests/ directory and from assets-built zigpack files (ZIP).
 * For M0, just parses JSON string and validates required fields per §16.3.
 */
object ManifestReader {

    fun parse(json: String): PackManifest {
        val obj = JSONObject(json)
        val packId = obj.getString("packId")
        val version = obj.optString("version", "1")
        val objectId = obj.getString("objectId")
        val assetsArray = obj.getJSONArray("assets")
        val assets = mutableListOf<AssetEntry>()
        for (i in 0 until assetsArray.length()) {
            val a = assetsArray.getJSONObject(i)
            assets.add(
                AssetEntry(
                    product = a.getString("product"),
                    publisher = a.getString("publisher"),
                    url = a.getString("url"),
                    credit = a.getString("credit"),
                    what = a.optString("what", ""),
                    processing = a.optString("processing", ""),
                    sha256 = a.optString("sha256", ""),
                    format = a.optString("format", ""),
                    resolution = a.optString("resolution", ""),
                    type = a.optString("type", "")
                )
            )
        }
        val dataCeiling = if (obj.has("dataCeiling")) {
            val dc = obj.getJSONObject("dataCeiling")
            DataCeiling(
                textEn = dc.optString("textEn", ""),
                textFa = dc.optString("textFa", ""),
                resolutionM = if (dc.has("resolutionM")) dc.getDouble("resolutionM") else null
            )
        } else null

        val geometry = if (obj.has("geometry")) {
            val g = obj.getJSONObject("geometry")
            GeometryMeta(
                type = g.optString("type", "sphere"),
                oblateness = g.optDouble("oblateness", 0.0)
            )
        } else null

        return PackManifest(
            packId = packId,
            version = version,
            objectId = objectId,
            assets = assets,
            dataCeiling = dataCeiling,
            geometry = geometry
        )
    }

    /**
     * Validates provenance per §16.3:
     * - any asset entry missing product, publisher, url or credit => failure
     * - url not plausible (bare domain, search URL, placeholder) => failure
     * - pack SHA mismatch not checked here (done in assetkit verify)
     * Returns list of errors, empty if valid.
     */
    fun validate(manifest: PackManifest): List<String> {
        val errors = mutableListOf<String>()
        if (manifest.assets.isEmpty()) {
            errors.add("ZERO_ASSETS: pack ${manifest.packId} has zero assets")
        }
        manifest.assets.forEachIndexed { idx, asset ->
            if (asset.product.isBlank()) errors.add("Asset $idx missing product")
            if (asset.publisher.isBlank()) errors.add("Asset $idx missing publisher")
            if (asset.url.isBlank()) errors.add("Asset $idx missing url")
            if (asset.credit.isBlank()) errors.add("Asset $idx missing credit")

            val url = asset.url.trim()
            if (url.isNotBlank()) {
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    errors.add("Asset $idx url not plausible (must start http/https): $url")
                }
                if (url.matches(Regex("^https?://[^/]+/?$"))) {
                    errors.add("Asset $idx url is bare domain, not product page: $url")
                }
                val lower = url.lowercase()
                if (lower.contains("google.com/search") || lower.contains("placeholder") || lower.contains("example.com")) {
                    errors.add("Asset $idx url is placeholder/search: $url")
                }
            }
        }
        return errors
    }
}

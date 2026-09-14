package com.zig.museum.core.data

import java.io.File
import java.util.zip.ZipFile

/**
 * Reads pyramids from a pack via ZipFile with no per-file recompression per M3 task 2.
 * Pack format per §6.3: ZIP STORED (no recompression) containing manifest.json, maps KTX2, tiles, luts, meta
 * Must be usable with ZipFile.
 */
class PackReader(
    private val packFile: File
) {
    data class Manifest(
        val objectId: String,
        val objectName: String,
        val baseWidth: Int,
        val baseHeight: Int,
        val tileSize: Int,
        val levels: Int,
        val groundResolutionMpp: Double,
        val assets: List<AssetEntry>
    )

    data class AssetEntry(
        val path: String,
        val product: String,
        val publisher: String,
        val url: String,
        val credit: String,
        val sha256: String
    )

    /**
     * List all tile entries in pack.
     */
    fun listTiles(): List<TileKey> {
        ZipFile(packFile).use { zip ->
            return zip.entries().asSequence()
                .filter { !it.isDirectory && it.name.startsWith("tiles/") && it.name.endsWith(".ktx2") }
                .mapNotNull { entry ->
                    // Extract objectId from manifest or pack filename
                    val objectId = packFile.nameWithoutExtension.removeSuffix(".zigpack")
                    TileKey.fromPath(objectId, entry.name)
                }
                .toList()
        }
    }

    /**
     * Read raw bytes for a tile, for worker decode.
     * Returns null if not found.
     */
    fun readTileBytes(key: TileKey): ByteArray? {
        ZipFile(packFile).use { zip ->
            val entry = zip.getEntry(key.toPath()) ?: return null
            return zip.getInputStream(entry).readBytes()
        }
    }

    /**
     * Read manifest.json bytes.
     */
    fun readManifestBytes(): ByteArray? {
        ZipFile(packFile).use { zip ->
            val entry = zip.getEntry("manifest.json") ?: return null
            return zip.getInputStream(entry).readBytes()
        }
    }

    /**
     * Verify pack can be opened with ZipFile and has expected structure (no recompression assumed STORED).
     * Returns list of issues, empty if OK.
     */
    fun verifyStructure(): List<String> {
        val issues = mutableListOf<String>()
        try {
            ZipFile(packFile).use { zip ->
                val entries = zip.entries().asSequence().toList()
                if (entries.none { it.name == "manifest.json" }) {
                    issues.add("missing manifest.json")
                }
                // Check method STORED = 0 per §6.3
                entries.forEach { e ->
                    if (!e.isDirectory && e.method != ZipFile.STORED && e.name.endsWith(".ktx2")) {
                        // For KTX2, STORED is required for deterministic streaming
                        // But we allow DEFLATED with warning? Per spec must be STORED, so flag.
                        issues.add("entry ${e.name} not STORED method=${e.method}")
                    }
                }
            }
        } catch (e: Exception) {
            issues.add("ZipFile open failed: ${e.message}")
        }
        return issues
    }

    companion object {
        fun isValidPack(file: File): Boolean {
            if (!file.exists() || !file.isFile) return false
            return try {
                ZipFile(file).use { zip ->
                    zip.getEntry("manifest.json") != null
                }
            } catch (e: Exception) {
                false
            }
        }
    }
}

package com.zig.museum.feature.museum

import android.content.Context
import com.zig.museum.core.credits.ManifestReader
import com.zig.museum.core.credits.PackManifest
import java.io.File

object ManifestLoader {

    fun loadFromAssets(context: Context): List<PackManifest> {
        val manifests = mutableListOf<PackManifest>()
        try {
            val assetManager = context.assets
            val files = assetManager.list("manifests") ?: emptyArray()
            for (fileName in files) {
                if (fileName.endsWith(".json")) {
                    try {
                        val json = assetManager.open("manifests/$fileName").bufferedReader().use { it.readText() }
                        val manifest = ManifestReader.parse(json)
                        manifests.add(manifest)
                    } catch (e: Exception) {
                        // Ignore parse errors
                        android.util.Log.w("ManifestLoader", "Failed to parse $fileName: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("ManifestLoader", "Failed to list manifests assets: ${e.message}")
        }

        // Fallback: try to load from file system (for debug, when manifests folder exists at repo root)
        if (manifests.isEmpty()) {
            try {
                val manifestsDir = File("manifests")
                if (manifestsDir.exists() && manifestsDir.isDirectory) {
                    manifestsDir.listFiles { f -> f.extension == "json" }?.forEach { file ->
                        try {
                            val json = file.readText()
                            val manifest = ManifestReader.parse(json)
                            manifests.add(manifest)
                        } catch (e: Exception) {
                            android.util.Log.w("ManifestLoader", "Failed to parse file ${file.name}: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }

        return manifests
    }

    fun loadForObject(context: Context, objectId: String): List<PackManifest> {
        if (objectId.isBlank()) return loadFromAssets(context)
        return loadFromAssets(context).filter { it.objectId == objectId || it.packId.contains(objectId) }
    }
}

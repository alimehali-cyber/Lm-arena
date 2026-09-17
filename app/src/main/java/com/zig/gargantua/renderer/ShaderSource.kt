package com.zig.gargantua.renderer

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Loads canonical shader assets for the Gargantua GPU renderer.
 * The files in assets/shaders/ are the single canonical production source of truth.
 */
object ShaderSource {

    const val VERTEX_SHADER_ASSET_PATH = "shaders/gargantua_test.vert"
    const val FRAGMENT_SHADER_ASSET_PATH = "shaders/gargantua_test.frag"

    fun loadVertexShader(context: Context): String {
        return readAsset(context, VERTEX_SHADER_ASSET_PATH)
    }

    fun loadFragmentShader(context: Context): String {
        return readAsset(context, FRAGMENT_SHADER_ASSET_PATH)
    }

    private fun readAsset(context: Context, path: String): String {
        return try {
            context.assets.open(path).use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            throw IllegalStateException("Failed to load canonical shader from assets/$path", e)
        }
    }
}

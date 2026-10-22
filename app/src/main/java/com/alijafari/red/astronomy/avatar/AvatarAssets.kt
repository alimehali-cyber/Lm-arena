package com.alijafari.red.astronomy.avatar

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * One avatar, decoded: its metadata and its sprite sheets.
 *
 * Loading is deliberately all-or-nothing. If the metadata will not parse, or not one sheet decodes,
 * [AvatarAssets.load] returns null and the scene keeps the vector observer — a missing avatar is a
 * shrug, never a hole in the grass.
 */
class LoadedAvatar(
    val meta: AvatarFramesMeta,
    private val sheets: Map<String, ImageBitmap>
) {
    val clipNames: Set<String> get() = sheets.keys

    /**
     * The sheet for a clip, falling back to the idle clip and then to whatever decoded first. A
     * character shipped without its wave still gets to stand there.
     */
    fun sheetFor(clipName: String): ImageBitmap? =
        sheets[clipName]
            ?: sheets[AvatarPoses.IDLE]
            ?: sheets.values.firstOrNull()

    fun clipFor(clipName: String): AvatarClip? =
        meta.clips.firstOrNull { it.name == clipName }
            ?: meta.clip(AvatarPoses.IDLE)
            ?: meta.clips.firstOrNull()
}

object AvatarAssets {

    private const val TAG = "AvatarAssets"
    private const val FOLDER = "avatar"

    /** The character that ships with the app. */
    const val DEFAULT_ASSET = "professor_z"

    /**
     * Loads `assets/avatar/<asset>/meta.json` and every sheet it names. Call off the main thread.
     *
     * @return null when the asset is absent, unparseable, or has no decodable sheet.
     */
    fun load(context: Context, asset: String = DEFAULT_ASSET): LoadedAvatar? {
        val meta = try {
            val text = context.assets.open("$FOLDER/$asset/meta.json")
                .bufferedReader()
                .use { it.readText() }
            AvatarFramesMeta.parse(text)
        } catch (e: Exception) {
            Log.i(TAG, "No avatar metadata for '$asset'; keeping the vector observer", e)
            null
        } ?: return null

        val sheets = LinkedHashMap<String, ImageBitmap>()
        for (clip in meta.clips) {
            try {
                val bitmap = context.assets.open("$FOLDER/$asset/${clip.sheet}").use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
                if (bitmap != null) sheets[clip.name] = bitmap.asImageBitmap()
            } catch (e: Exception) {
                // One bad sheet must not take the character with it.
                Log.w(TAG, "Avatar sheet '${clip.sheet}' failed to decode", e)
            }
        }
        if (sheets.isEmpty()) {
            Log.w(TAG, "Avatar '$asset' has no usable sheets; keeping the vector observer")
            return null
        }
        return LoadedAvatar(meta, sheets)
    }
}

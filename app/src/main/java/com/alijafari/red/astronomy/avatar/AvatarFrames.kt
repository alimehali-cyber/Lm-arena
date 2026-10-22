package com.alijafari.red.astronomy.avatar

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.alijafari.red.astronomy.domain.SkyCanvasTheme
import com.alijafari.red.astronomy.ui.rendering.LightingState
import com.alijafari.red.astronomy.ui.rendering.SceneComposition
import org.json.JSONObject
import kotlin.math.roundToInt

/**
 * The baked sprite sheets for one avatar: one sheet per clip plus the framing metadata.
 *
 * A generated character ships as two things. The GLB is the portable asset — it opens in Blender, a
 * game engine, or a viewer. These sheets are what the Home scene actually draws, because a phone that
 * has already paid for a full astronomy renderer should not also pay for an EGL surface and a second
 * GL context to animate a figure that is 170 px tall.
 *
 * Everything here is pure: parsing, clip choice, frame choice and placement are functions of their
 * arguments, so the geometry can be unit-tested on the JVM without a device or a bitmap.
 */
data class AvatarClip(
    val name: String,
    /** File name of the atlas inside the avatar's asset folder. */
    val sheet: String,
    val frames: Int,
    val fps: Float,
    val seconds: Float,
    val loop: Boolean
)

data class AvatarFramesMeta(
    val asset: String,
    val heightM: Float,
    val cellWidth: Int,
    val cellHeight: Int,
    val columns: Int,
    val rows: Int,
    /** Where the feet are inside a cell, 0..1 from the top-left corner. */
    val anchorX: Float,
    val anchorY: Float,
    /** How much of the cell's height the standing figure fills. */
    val bodyFraction: Float,
    val clips: List<AvatarClip>
) {
    fun clip(name: String): AvatarClip? = clips.firstOrNull { it.name == name } ?: clips.firstOrNull()

    val cellAspect: Float get() = cellWidth.toFloat() / cellHeight.toFloat()

    companion object {
        /**
         * Parses `meta.json`. Returns null on anything unexpected: a malformed avatar must leave the
         * vector observer on screen, never a hole where a person was.
         */
        fun parse(text: String): AvatarFramesMeta? = runCatching {
            val root = JSONObject(text)
            val cell = root.getJSONObject("cell")
            val anchor = root.optJSONObject("anchor")
            val clipsJson = root.getJSONArray("clips")
            val clips = ArrayList<AvatarClip>(clipsJson.length())
            for (i in 0 until clipsJson.length()) {
                val c = clipsJson.getJSONObject(i)
                clips += AvatarClip(
                    name = c.getString("name"),
                    sheet = c.getString("sheet"),
                    frames = c.getInt("frames").coerceAtLeast(1),
                    fps = c.optDouble("fps", 8.0).toFloat().coerceAtLeast(0.5f),
                    seconds = c.optDouble("seconds", 1.0).toFloat(),
                    loop = c.optBoolean("loop", true)
                )
            }
            if (clips.isEmpty()) return null
            AvatarFramesMeta(
                asset = root.optString("asset", "avatar"),
                heightM = root.optDouble("height_m", 1.8).toFloat(),
                cellWidth = cell.getInt("w").coerceAtLeast(1),
                cellHeight = cell.getInt("h").coerceAtLeast(1),
                columns = cell.getInt("cols").coerceAtLeast(1),
                rows = cell.getInt("rows").coerceAtLeast(1),
                anchorX = anchor?.optDouble("x", 0.5)?.toFloat() ?: 0.5f,
                anchorY = anchor?.optDouble("y", 0.95)?.toFloat() ?: 0.95f,
                bodyFraction = root.optDouble("body_fraction", 0.9).toFloat().coerceIn(0.2f, 1f),
                clips = clips
            )
        }.getOrNull()
    }
}

/**
 * Which performance the scene is showing, from the real Sun altitude.
 *
 * These thresholds are the observer's, not a new invention: `ObserverRenderer.poseFor` shades the eyes
 * above +3°, looks at a phone through twilight and points at the sky below −7°. The avatar keeps the
 * same three beats, so the scene's meaning does not change when the character does.
 */
object AvatarPoses {
    const val IDLE = "idle_breathe"
    const val DAY = "day_shade"
    const val DUSK = "dusk_phone"
    const val NIGHT = "night_point"
    const val WAVE = "wave_hello"

    fun forSunAltitude(sunAltitudeDeg: Double): String = when {
        sunAltitudeDeg > 3.0 -> DAY
        sunAltitudeDeg > -7.0 -> DUSK
        else -> NIGHT
    }
}

/**
 * Where one cell of a sheet lands on the scene canvas.
 *
 * The figure is anchored by its feet and its own body height, never by the cell, so a clip with a
 * raised arm cannot make the character smaller than the clip where the arms are down.
 */
data class AvatarPlacement(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
) {
    val right: Float get() = left + width
    val bottom: Float get() = top + height

    fun contains(point: Offset): Boolean =
        point.x >= left && point.x <= right && point.y >= top && point.y <= bottom

    fun dstOffset(): IntOffset = IntOffset(left.roundToInt(), top.roundToInt())
    fun dstSize(): IntSize = IntSize(width.roundToInt().coerceAtLeast(1), height.roundToInt().coerceAtLeast(1))
}

object AvatarFraming {

    /** Frame index for a running clock. Non-looping clips clamp on their last frame. */
    fun frameIndex(clip: AvatarClip, elapsedMs: Long): Int {
        val frames = clip.frames.coerceAtLeast(1)
        val frameMs = (1000f / clip.fps.coerceAtLeast(0.5f)).coerceAtLeast(1f)
        val index = (elapsedMs.coerceAtLeast(0L) / frameMs).toInt()
        return if (clip.loop) Math.floorMod(index, frames) else index.coerceIn(0, frames - 1)
    }

    /** The cell's pixel rectangle inside the atlas. */
    fun sourceRect(meta: AvatarFramesMeta, clip: AvatarClip, frame: Int): Pair<IntOffset, IntSize> {
        val index = if (clip.loop) Math.floorMod(frame, clip.frames) else frame.coerceIn(0, clip.frames - 1)
        val col = index % meta.columns
        val row = index / meta.columns
        return IntOffset(col * meta.cellWidth, row * meta.cellHeight) to IntSize(meta.cellWidth, meta.cellHeight)
    }

    /**
     * Places the cell on the canvas so the character's feet sit on the grass line at the composition's
     * figure centre, and the character's body is exactly `figureHeightPx` tall.
     */
    fun placement(
        composition: SceneComposition,
        meta: AvatarFramesMeta,
        figureOnRight: Boolean = true
    ): AvatarPlacement {
        val centreX = if (figureOnRight) composition.figureCenterX else composition.widthPx - composition.figureCenterX
        val bodyFraction = meta.bodyFraction.coerceIn(0.2f, 1f)
        val height = composition.figureHeightPx / bodyFraction
        val width = height * meta.cellAspect
        val left = centreX - meta.anchorX.coerceIn(0f, 1f) * width
        val top = composition.figureFeetY - meta.anchorY.coerceIn(0f, 1f) * height
        return AvatarPlacement(left, top, width, height)
    }
}

/**
 * Colour grading for the six themes and the three lighting states, as an alpha plus a 4×5 colour
 * matrix (the shape Compose's [androidx.compose.ui.graphics.ColorFilter] wants).
 *
 * A baked sprite carries the light of the moment it was rendered. Without a grade it would look lit by
 * its own little sun and float in front of the scene; with one it sits inside it — and, just as
 * importantly, a photographic-ish character can still read in Monochrome, Observatory and Papercraft.
 */
object AvatarGrade {
    const val TINT_DAY = "day"
    const val TINT_DUSK = "dusk"
    const val TINT_NIGHT = "night"

    /** Which lighting bucket the scene is in. Mirrors the pose thresholds, widened for the grade. */
    fun lightingBucket(sunAltitudeDeg: Double): String = when {
        sunAltitudeDeg > 6.0 -> TINT_DAY
        sunAltitudeDeg > -6.0 -> TINT_DUSK
        else -> TINT_NIGHT
    }

    /**
     * A 4×5 row-major colour matrix, or null for "unchanged". Only the RGB rows are touched; the alpha
     * row is always the identity so a sprite never loses its transparency.
     */
    fun colorMatrix(theme: SkyCanvasTheme, bucket: String): FloatArray? {
        val (r, g, b) = when (theme) {
            SkyCanvasTheme.MONOCHROME_SCIENTIFIC -> Triple(0.34f, 0.34f, 0.34f)
            SkyCanvasTheme.OBSERVATORY -> Triple(1.05f, 0.42f, 0.38f)
            SkyCanvasTheme.KIDS_WATERCOLOR -> Triple(1.06f, 1.00f, 1.02f)
            SkyCanvasTheme.PAPERCRAFT_DIORAMA -> Triple(1.02f, 0.98f, 0.92f)
            else -> when (bucket) {
                TINT_DAY -> Triple(1.0f, 1.0f, 1.0f)
                TINT_DUSK -> Triple(1.06f, 0.98f, 0.90f)
                else -> Triple(0.72f, 0.80f, 1.00f)
            }
        }
        if (r == 1f && g == 1f && b == 1f) return null
        // Row-major 4x5: R' = r·R, G' = g·G, B' = b·B, A' = A.
        return floatArrayOf(
            r, 0f, 0f, 0f, 0f,
            0f, g, 0f, 0f, 0f,
            0f, 0f, b, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    }

    /** Opacity for the moment: the figure all but disappears into a bright noon sky and stays present at night. */
    fun alpha(lighting: LightingState): Float {
        val day = lighting.ambientBrightness.coerceIn(0f, 1f)
        return (0.96f - 0.06f * day).coerceIn(0.72f, 1f)
    }
}

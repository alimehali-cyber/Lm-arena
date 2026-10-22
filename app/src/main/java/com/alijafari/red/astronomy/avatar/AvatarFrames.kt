package com.alijafari.red.astronomy.avatar

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.alijafari.red.astronomy.domain.SkyCanvasTheme
import com.alijafari.red.astronomy.ui.rendering.LightingState
import com.alijafari.red.astronomy.ui.rendering.SceneComposition
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
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

    /**
     * The on-disk shape of `meta.json`, as written by `tools/avatar/bake_frames.py`. Moshi rather than
     * `org.json`: this runs on the JVM in unit tests, where Android's JSON stub returns nulls and would
     * turn every assertion into a shrug.
     */
    @JsonClass(generateAdapter = false)
    internal class ManifestDto(
        val asset: String? = null,
        @Json(name = "height_m") val heightM: Float? = null,
        val cell: CellDto? = null,
        val anchor: AnchorDto? = null,
        @Json(name = "body_fraction") val bodyFraction: Float? = null,
        val clips: List<ClipDto>? = null
    )

    @JsonClass(generateAdapter = false)
    internal class CellDto(val w: Int = 0, val h: Int = 0, val cols: Int = 1, val rows: Int = 1)

    @JsonClass(generateAdapter = false)
    internal class AnchorDto(val x: Float = 0.5f, val y: Float = 0.95f)

    @JsonClass(generateAdapter = false)
    internal class ClipDto(
        val name: String = "",
        val sheet: String = "",
        val frames: Int = 1,
        val fps: Float = 8f,
        val seconds: Float = 1f,
        val loop: Boolean = true
    )

    companion object {
        private val adapter: JsonAdapter<ManifestDto> by lazy {
            Moshi.Builder().add(KotlinJsonAdapterFactory()).build().adapter(ManifestDto::class.java)
        }

        /**
         * Parses `meta.json`. Returns null on anything unexpected: a malformed avatar must leave the
         * vector observer on screen, never a hole where a person was.
         */
        fun parse(text: String): AvatarFramesMeta? {
            if (text.isBlank()) return null
            val dto = runCatching { adapter.fromJson(text) }.getOrNull() ?: return null
            val cell = dto.cell ?: return null
            if (cell.w < 1 || cell.h < 1 || cell.cols < 1 || cell.rows < 1) return null

            val clips = dto.clips.orEmpty()
                .filter { it.name.isNotBlank() && it.sheet.isNotBlank() }
                .map { c ->
                    AvatarClip(
                        name = c.name,
                        sheet = c.sheet,
                        frames = c.frames.coerceIn(1, cell.cols * cell.rows),
                        fps = c.fps.coerceIn(0.5f, 60f),
                        seconds = c.seconds.coerceAtLeast(0f),
                        loop = c.loop
                    )
                }
            if (clips.isEmpty()) return null

            return AvatarFramesMeta(
                asset = dto.asset ?: "avatar",
                heightM = (dto.heightM ?: 1.8f).coerceIn(0.3f, 3f),
                cellWidth = cell.w,
                cellHeight = cell.h,
                columns = cell.cols,
                rows = cell.rows,
                anchorX = (dto.anchor?.x ?: 0.5f).coerceIn(0f, 1f),
                anchorY = (dto.anchor?.y ?: 0.95f).coerceIn(0f, 1f),
                bodyFraction = (dto.bodyFraction ?: 0.9f).coerceIn(0.2f, 1f),
                clips = clips
            )
        }
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
     *
     * The composition already knows which side the figure stands on — `figureCenterX` is mirrored for
     * Persian — so this reads it rather than deciding again. Mirroring twice puts the character back on
     * the side the caller was trying to move him away from.
     */
    fun placement(composition: SceneComposition, meta: AvatarFramesMeta): AvatarPlacement {
        val centreX = composition.figureCenterX
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
     * A 4x5 row-major colour matrix, or null for "unchanged". Only the RGB rows are touched; the alpha
     * row is always the identity so a sprite never loses its transparency.
     *
     * The themed looks win over the light, because a theme is a deliberate art direction: a red
     * observatory should redden the character standing in it, not just darken him.
     */
    fun colorMatrix(theme: SkyCanvasTheme, bucket: String): FloatArray? = when (theme) {
        // A Swiss plate: the character becomes a value scale like everything else on the sheet.
        SkyCanvasTheme.MONOCHROME_SCIENTIFIC -> luma()
        // Rendered in a dim red room; the figure keeps its shape, loses its colour.
        SkyCanvasTheme.OBSERVATORY -> rowScale(1.02f, 0.40f, 0.36f)
        // Watercolour and paper both lift the highlights a touch, so the figure reads as painted stock.
        SkyCanvasTheme.KIDS_WATERCOLOR -> rowScale(1.04f, 1.00f, 1.02f)
        SkyCanvasTheme.PAPERCRAFT_DIORAMA -> rowScale(1.02f, 0.99f, 0.94f)
        else -> when (bucket) {
            TINT_DAY -> null
            TINT_DUSK -> rowScale(1.06f, 0.98f, 0.90f)
            else -> rowScale(0.74f, 0.82f, 1.0f)
        }
    }

    /** Per-channel gain, alpha untouched. */
    private fun rowScale(r: Float, g: Float, b: Float): FloatArray = floatArrayOf(
        r, 0f, 0f, 0f, 0f,
        0f, g, 0f, 0f, 0f,
        0f, 0f, b, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )

    /** Rec. 709 luma, replicated into all three channels: a true greyscale. */
    private fun luma(): FloatArray {
        val r = 0.2126f
        val g = 0.7152f
        val b = 0.0722f
        return floatArrayOf(
            r, g, b, 0f, 0f,
            r, g, b, 0f, 0f,
            r, g, b, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    }

    /** Opacity for the moment: the figure all but disappears into a bright noon sky and stays present at night. */
    fun alpha(lighting: LightingState): Float {
        val day = lighting.ambientBrightness.coerceIn(0f, 1f)
        return (0.96f - 0.06f * day).coerceIn(0.72f, 1f)
    }
}

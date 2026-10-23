package com.alijafari.red.astronomy.ui.character

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.alijafari.red.astronomy.R
import com.alijafari.red.astronomy.ui.rendering.LightingState
import com.alijafari.red.astronomy.ui.rendering.SceneComposition
import kotlin.math.roundToInt

/**
 * The animated character that stands on the grass line of the Home scene.
 *
 * It is ZIG's first real character: Professor Z, modelled and rigged in Blender, exported as a
 * skinned GLB with 21 clips, and baked here into a sixteen-frame sprite strip of `Idle_Breathing`
 * rendered from that same scene. See avatar_pipeline/blender/build_professor_z.py.
 *
 * ## Why a sprite and not the GLB at runtime
 *
 * `avatar_pipeline/android/` contains a complete GLES 3.0 runtime for the GLB. It is not wired in,
 * for one reason: this environment has no device and no emulator, and a GLSurfaceView composited
 * into a Compose hero inside a scrolling card is precisely the change that fails invisibly —
 * black surface, wrong z-order, EGL context loss on rotation. The sprite is drawn by the same
 * Canvas that draws the sky: one `drawImage` per frame, no EGL, no lifecycle, and it cannot make
 * the app slower than the vector figure it replaces.
 *
 * ## Reverting
 *
 * Set [HeroCharacter.ENABLED] to false. `SkySceneRenderer` then draws the original vector
 * observer again; nothing else in the app changes.
 */
object HeroCharacter {

    /**
     * Master switch for the 3D character. Off = the vector observer, exactly as before this
     * change. Deliberately a compile-time constant: it cannot end up half-on at runtime.
     */
    const val ENABLED = true

    /** Frames per strip: must match the width of the baked PNG divided by one frame. */
    const val FRAME_COUNT = 16

    /**
     * Duration of one breath cycle in the strip, in milliseconds.
     *
     * The bake sampled 60 frames of `Idle_Breathing` at 24 fps = 2.5 s, so the sixteen baked
     * frames must be played over 2.5 s, not over a guessed two seconds.
     */
    const val CYCLE_MS = 2500L
}

/**
 * Loads the strip once and draws it where the vector observer used to stand.
 *
 * The numbers below were measured off the baked frames themselves rather than assumed: with the
 * camera fitted at 1.20x the body height, the figure occupies 86.67 % of the frame vertically and
 * its soles sit at 94.58 % of the frame, horizontally centred. Drawing with those fractions is what
 * makes the character the same height as the observer it replaces.
 */
object HeroCharacterSprite {

    /** Body height as a fraction of the strip frame height (measured, see above). */
    private const val BODY_HEIGHT_FRACTION = 0.8667f

    /** Where the soles sit inside the frame, as a fraction of its height (measured). */
    private const val FEET_FRACTION = 0.9458f

    /** Frame aspect: 160 x 240. */
    private const val ASPECT = 160f / 240f

    @Volatile
    private var strip: ImageBitmap? = null

    /** True once the strip is in memory; false keeps the vector fallback. */
    val isReady: Boolean get() = strip != null

    /**
     * Decodes the strip. Safe to call repeatedly and from any thread; the first successful call
     * wins. A missing drawable leaves the object unloaded, which is a supported state: the scene
     * keeps drawing the vector observer.
     */
    fun ensureLoaded(context: Context) {
        if (strip != null) return
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inScaled = false
        }
        val bitmap = try {
            BitmapFactory.decodeResource(context.resources, R.drawable.hero_profz_idle, options)
        } catch (e: Exception) {
            null
        } ?: return
        strip = bitmap.asImageBitmap()
    }

    /**
     * Draws one frame of the idle. Returns false if the strip is not loaded or the figure would be
     * too small to read, in which case the caller draws the vector observer instead.
     */
    fun draw(
        drawScope: DrawScope,
        composition: SceneComposition,
        lighting: LightingState,
        frameTimeMs: Long
    ): Boolean {
        if (!HeroCharacter.ENABLED) return false
        val image = strip ?: return false
        val s = composition.figureHeightPx
        if (s < 8f) return false

        val frameWidth = image.width / HeroCharacter.FRAME_COUNT
        if (frameWidth <= 0) return false
        val index = ((frameTimeMs / (HeroCharacter.CYCLE_MS / HeroCharacter.FRAME_COUNT)) %
            HeroCharacter.FRAME_COUNT).toInt()

        val h = s / BODY_HEIGHT_FRACTION
        val w = h * ASPECT
        val left = composition.figureCenterX - w / 2f
        val top = composition.figureFeetY - FEET_FRACTION * h

        // Ground contact: the same oval the vector figure drew, so the swap does not change how
        // the feet sit on the grass.
        val shadowStrength = 0.10f + 0.22f * lighting.ambientBrightness.coerceIn(0f, 1f)
        drawScope.drawOval(
            color = Color.Black.copy(alpha = shadowStrength * 0.55f),
            topLeft = Offset(composition.figureCenterX - s * 0.30f, composition.figureFeetY - s * 0.020f),
            size = Size(s * 0.60f, s * 0.055f)
        )

        // Daylight keeps the baked colours; night multiplies them towards a cool dim tone so the
        // character sits in the same light as the rest of the scene. (A baked sprite cannot
        // re-light itself, and tinting is the honest approximation.)
        val ambient = lighting.ambientBrightness.coerceIn(0f, 1f)
        val tint = Color(
            red = 0.42f + 0.58f * ambient,
            green = 0.46f + 0.54f * ambient,
            blue = 0.60f + 0.40f * ambient,
            alpha = 1f
        )

        drawScope.drawImage(
            image = image,
            srcOffset = IntOffset(index * frameWidth, 0),
            srcSize = IntSize(frameWidth, image.height),
            dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
            dstSize = IntSize(w.roundToInt(), h.roundToInt()),
            alpha = 1f,
            colorFilter = ColorFilter.tint(tint, BlendMode.Modulate),
            blendMode = BlendMode.SrcOver,
            filterQuality = FilterQuality.Medium
        )
        return true
    }
}

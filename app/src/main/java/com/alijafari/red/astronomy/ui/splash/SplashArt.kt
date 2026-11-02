package com.alijafari.red.astronomy.ui.splash

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.alijafari.red.astronomy.R
import kotlin.random.Random

/** The splash's bitmaps, decoded once at full resolution. */
@Immutable
class SplashArt(
    val velvet: ImageBitmap,
    val glass: ImageBitmap,
    val rim: ImageBitmap,
    val shadow: ImageBitmap,
    /** Small tile of monochrome noise for the animated film grain. */
    val grain: Bitmap,
)

/** Colours shared by the window theme, the system splash and the first animated frame. */
object SplashPalette {
    /**
     * The darkness the velvet "lights up" from. MUST equal `@color/zig_splash_velvet_dark`, which
     * the window background and the Android 12+ system splash use: frame 0 of the animation is
     * painted in exactly this colour so the OS hand-off is invisible.
     */
    val VelvetDark = Color(0xFF120105)
    val VelvetMean = Color(SplashArtGeometry.VELVET_MEAN_ARGB)
    val Glow = Color(0xFFFF2D55)
    val KeyLight = Color(0xFFFFB3C2)
}

/**
 * Decodes the splash art off the main thread, as early as possible (Activity.onCreate), so the
 * first animated frame never waits on a decode - decoding a ~6 MB bitmap inside composition is a
 * guaranteed dropped frame. The Activity keeps the system splash up (OnPreDrawListener) until
 * [isSettled], with a timeout, so the animation starts on a ready, fully-uploaded texture.
 */
object SplashArtLoader {
    private const val TAG = "SplashArt"

    /** Observable from Compose. Written on the main thread only. */
    var art: SplashArt? by mutableStateOf(null)
        private set

    /** True once decoding finished (successfully or not). Observable from Compose. */
    var isSettled: Boolean by mutableStateOf(false)
        private set

    @Volatile
    private var started = false

    fun preload(context: Context) {
        if (started) return
        started = true
        val app = context.applicationContext
        Thread({
            val result = runCatching { decode(app) }
                .onFailure { Log.w(TAG, "Splash art failed to decode; falling back to a plain splash", it) }
                .getOrNull()
            Handler(Looper.getMainLooper()).post {
                art = result
                isSettled = true
            }
        }, "zig-splash-decode").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    /** Synchronous decode, for tests and previews. */
    fun loadBlocking(context: Context): SplashArt = decode(context.applicationContext)

    /** Drop the bitmaps once the splash is gone; they are only needed for ~4 s per cold start. */
    fun release() {
        art = null
    }

    private fun decode(context: Context): SplashArt {
        val res = context.resources
        fun load(id: Int): Bitmap {
            val opts = BitmapFactory.Options().apply {
                inScaled = false
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bmp = BitmapFactory.decodeResource(res, id, opts)
                ?: error("Could not decode splash resource $id")
            // Kick off the GPU texture upload now, on the RenderThread, instead of on first draw.
            bmp.prepareToDraw()
            return bmp
        }
        return SplashArt(
            velvet = load(R.drawable.splash_velvet).asImageBitmap(),
            glass = load(R.drawable.splash_z).asImageBitmap(),
            rim = load(R.drawable.splash_z_rim).asImageBitmap(),
            shadow = load(R.drawable.splash_z_shadow).asImageBitmap(),
            grain = makeGrainTile(),
        )
    }

    private fun makeGrainTile(size: Int = 128): Bitmap {
        val rnd = Random(0x5A16)
        val px = IntArray(size * size) {
            // Signed grain: half the grains lighten, half darken, so the mean brightness is kept.
            val v = rnd.nextFloat()
            val light = v > 0.5f
            val a = ((kotlin.math.abs(v - 0.5f) * 2f).let { it * it } * 255f).toInt().coerceIn(0, 255)
            if (light) (a shl 24) or 0xFFFFFF else (a shl 24)
        }
        return Bitmap.createBitmap(px, size, size, Bitmap.Config.ARGB_8888).also { it.prepareToDraw() }
    }
}

package com.alijafari.red.astronomy.ui.rendering

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.alijafari.red.astronomy.R
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Professor Z as the scene's observer: one pre-rendered sprite sheet, built in the avatar lab
 * from the uploaded reference photos and shipped as a single `drawable-nodpi` PNG.
 *
 * The sheet's rows follow [ObserverPose] order (SHADING_EYES, PHONE, LOOKING_UP — the pose the
 * app picks from the real Sun altitude), and its 12 columns are one 5.2 s breath cycle the
 * observer steps through on the scene clock, feet planted. Each cell frames 1.87 m of world
 * with the professor's 1.493 m standing height, so the app can place him by feet and height
 * exactly the way the vector figure was placed.
 *
 * Decoding happens off the UI thread; [sheet] stays null until it lands, and
 * [ObserverRenderer] keeps drawing its vector figure in the meantime — the same
 * "appears with its first sprite" hand-off the photographic Moon uses.
 */
class ObserverSprites(context: Context) {

    /** The decoded sheet, or null while the first decode is still running. */
    @Volatile
    var sheet: ImageBitmap? = null
        private set

    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val executor: ExecutorService = Executors.newSingleThreadExecutor { r ->
        Thread(r, "ObserverSheet").apply { priority = Thread.NORM_PRIORITY - 1; isDaemon = true }
    }

    @Volatile
    private var released = false

    init {
        executor.execute {
            val bitmap = runCatching {
                BitmapFactory.decodeResource(appContext.resources, R.drawable.profz_observer_sheet)
            }.getOrNull()
            val image = bitmap?.asImageBitmap()
            main.post {
                if (!released) {
                    sheet = image
                }
                shutdown()
            }
        }
    }

    private fun shutdown() {
        executor.shutdown()
    }

    /** Drops the decoded sheet; the Composable calls this when it leaves the composition. */
    fun release() {
        released = true
        sheet = null
        shutdown()
    }

    companion object {
        /** Breath frames per pose, one full 5.2 s cycle left to right. */
        const val FRAMES = 12

        /** One cell in sheet pixels. */
        const val CELL_W = 192
        const val CELL_H = 384

        /** World metres a cell frames (top of a raised hand) and the professor's standing height. */
        const val FRAME_M = 1.87f
        const val CHAR_M = 1.493f
    }
}

/** Remembers one [ObserverSprites] for the composition and releases it on the way out. */
@Composable
fun rememberObserverSprites(): ObserverSprites {
    val context = LocalContext.current
    val sprites = remember { ObserverSprites(context.applicationContext) }
    DisposableEffect(sprites) { onDispose { sprites.release() } }
    return sprites
}

package com.alijafari.red.astronomy.fieldtrial

import android.app.Activity
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import com.alijafari.red.astronomy.fieldtrial.engine.InverseProjection
import com.alijafari.red.astronomy.fieldtrial.engine.TrackerProjector
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * G-1.8 evidence capture. Primary path: PixelCopy of the window with the guide card
 * hidden (real screenshot incl. camera preview + markers). Fallback (PixelCopy
 * unavailable/failed — and for the offline rehearsal): a deterministic marker-scene
 * renderer via android.graphics (black canvas + thin target ring + tap crosshair +
 * computed offset arrow) — honest evidence of WHAT was measured even without the photo.
 * UNEXECUTED on a device until the trial (compile-verified only).
 */
object Capture {

    /** Marker geometry captured at Confirm (screen px, canvas space). */
    data class MarkerScene(
        val widthPx: Int,
        val heightPx: Int,
        val targetPx: Pair<Float, Float>?,
        val tapPx: Pair<Float, Float>?,
        val drawArrow: Boolean,
        val extraRingPx: Pair<Float, Float>? = null, // L9 green ring
        val label: String = ""
    )

    /** Try a real screenshot of the activity window; null on any failure (PixelCopy is API 26+). */
    fun pixelCopyShot(activity: Activity?, widthPx: Int, heightPx: Int): ByteArray? {
        if (activity == null || widthPx <= 0 || heightPx <= 0) return null
        if (android.os.Build.VERSION.SDK_INT < 26) return null
        return runCatching {
            val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
            val latch = CountDownLatch(1)
            var result = IntArray(1) { Int.MIN_VALUE }
            PixelCopy.request(activity.window, bitmap, { r -> result[0] = r; latch.countDown() },
                Handler(Looper.getMainLooper()))
            if (!latch.await(700, TimeUnit.MILLISECONDS) || result[0] != PixelCopy.SUCCESS) return null
            val out = java.io.ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            bitmap.recycle()
            out.toByteArray()
        }.getOrNull()
    }

    /** Deterministic fallback: render the marker scene (thin rings, crosshair, arrow). */
    fun markerSceneShot(scene: MarkerScene): ByteArray {
        val w = scene.widthPx.coerceAtLeast(8)
        val h = scene.heightPx.coerceAtLeast(8)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(bitmap)
        c.drawColor(android.graphics.Color.BLACK)
        val ring = android.graphics.Paint().apply {
            color = 0xFF6699FF.toInt(); style = android.graphics.Paint.Style.STROKE; strokeWidth = 3f; isAntiAlias = true
        }
        val green = android.graphics.Paint(ring).apply { color = 0xFF66FF99.toInt() }
        val white = android.graphics.Paint(ring).apply { color = 0xFFFFFFFF.toInt(); strokeWidth = 2f }
        val arrow = android.graphics.Paint().apply {
            color = 0xFFFFCC66.toInt(); style = android.graphics.Paint.Style.STROKE; strokeWidth = 4f; isAntiAlias = true
        }
        scene.targetPx?.let { (x, y) -> c.drawCircle(x, y, 26f, ring) }
        scene.extraRingPx?.let { (x, y) -> c.drawCircle(x, y, 26f, green) }
        scene.tapPx?.let { (x, y) ->
            c.drawLine(x - 18f, y, x + 18f, y, white)
            c.drawLine(x, y - 18f, x, y + 18f, white)
            if (scene.drawArrow && scene.targetPx != null) {
                val (tx, ty) = scene.targetPx
                c.drawLine(tx, ty, x, y, arrow)
            }
        }
        if (scene.label.isNotEmpty()) {
            val text = android.graphics.Paint().apply {
                color = 0xFFCCCCCC.toInt(); textSize = 28f
            }
            c.drawText(scene.label, 16f, 40f, text)
        }
        val out = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        bitmap.recycle()
        return out.toByteArray()
    }

    /**
     * Convert a screen-px distance to on-sky degrees (pure math lives in
     * [TrackerProjector.pxDistanceToDeg], pinned by its unit test).
     */
    fun pxToDeg(rCanvasPx: Double, intr: InverseProjection.Intrinsics, canvasW: Double, canvasH: Double, zoom: Double): Double =
        TrackerProjector.pxDistanceToDeg(rCanvasPx, intr, canvasW, canvasH, zoom)
}

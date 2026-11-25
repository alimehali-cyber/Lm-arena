package com.alijafari.red.astronomy.ui.satellites

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.alijafari.red.astronomy.astro_engine.SatelliteCategory
import kotlin.math.min

/**
 * The literal satellite silhouettes, drawn on an Android [Canvas].
 *
 * The Satellites screen draws them with Compose's `Canvas`, the AR screen draws them on the
 * `android.graphics.Canvas` that backs its sky overlay. Keeping the geometry here — in one file,
 * in one coordinate system — is what makes the two screens show the same ISS, Hubble, Tiangong,
 * Envisat and Starlink shapes instead of two approximations that drift apart.
 *
 * All coordinates are normalised to the supplied box: `x` and `y` are 0..1 fractions of
 * [width]/[height]. The caller owns the colour (and therefore its alpha) and an optional reusable
 * paint, so the AR overlay does not allocate a paint per satellite per frame.
 */
object SatelliteGlyph {

    fun drawSilhouette(
        canvas: Canvas,
        category: SatelliteCategory,
        id: String,
        color: Int,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    ) {
        if (width <= 0f || height <= 0f) return
        val unit = min(width, height)
        paint.flags = Paint.ANTI_ALIAS_FLAG
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = color

        fun px(x: Float): Float = left + width * x
        fun py(y: Float): Float = top + height * y
        fun rect(l: Float, t: Float, r: Float, b: Float) =
            RectF(px(l), py(t), px(r), py(b))

        fun line(ax: Float, ay: Float, bx: Float, by: Float, stroke: Float = 0.055f) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = unit * stroke
            paint.color = color
            canvas.drawLine(px(ax), py(ay), px(bx), py(by), paint)
        }

        fun panel(l: Float, t: Float, r: Float, b: Float, rows: Int = 2) {
            val bounds = rect(l, t, r, b)
            paint.style = Paint.Style.FILL
            paint.color = alpha(color, 0.72f)
            canvas.drawRect(bounds, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = unit * 0.028f
            paint.color = alpha(color, 0.92f)
            canvas.drawRect(bounds, paint)
            for (i in 1 until rows) {
                val y = t + (b - t) * i / rows
                line(l, y, r, y, 0.018f)
            }
            val x = (l + r) / 2f
            line(x, t, x, b, 0.018f)
        }

        fun rounded(l: Float, t: Float, r: Float, b: Float, radiusFraction: Float) {
            paint.style = Paint.Style.FILL
            paint.color = color
            canvas.drawRoundRect(rect(l, t, r, b), unit * radiusFraction, unit * radiusFraction, paint)
        }

        val iss = category == SatelliteCategory.ISS || id == "iss_zarya"
        val hubble = category == SatelliteCategory.HUBBLE || id.contains("hubble", true)
        val tiangong = id.contains("tiangong", true)
        when {
            iss -> {
                // ISS: long integrated truss with four broad solar-array wings.
                line(0.12f, 0.50f, 0.88f, 0.50f, 0.065f)
                panel(0.04f, 0.17f, 0.32f, 0.39f, 3)
                panel(0.04f, 0.61f, 0.32f, 0.83f, 3)
                panel(0.68f, 0.17f, 0.96f, 0.39f, 3)
                panel(0.68f, 0.61f, 0.96f, 0.83f, 3)
                rounded(0.40f, 0.35f, 0.60f, 0.65f, 0.07f)
                line(0.50f, 0.30f, 0.50f, 0.70f, 0.025f)
            }
            hubble -> {
                // Hubble: open aperture, long cylindrical telescope bus and two side arrays.
                panel(0.02f, 0.34f, 0.23f, 0.66f)
                panel(0.77f, 0.34f, 0.98f, 0.66f)
                line(0.22f, 0.50f, 0.77f, 0.50f, 0.075f)
                rounded(0.37f, 0.31f, 0.64f, 0.69f, 0.09f)
                paint.style = Paint.Style.FILL
                paint.color = alpha(color, 0.56f)
                canvas.drawCircle(px(0.70f), py(0.50f), unit * 0.10f, paint)
                paint.color = color
                canvas.drawCircle(px(0.70f), py(0.50f), unit * 0.06f, paint)
            }
            tiangong -> {
                // Tiangong's T-shaped module stack and deployed solar wings.
                panel(0.02f, 0.30f, 0.28f, 0.47f)
                panel(0.72f, 0.30f, 0.98f, 0.47f)
                line(0.24f, 0.39f, 0.76f, 0.39f, 0.05f)
                rounded(0.42f, 0.30f, 0.58f, 0.78f, 0.06f)
                rounded(0.28f, 0.47f, 0.72f, 0.61f, 0.06f)
            }
            category == SatelliteCategory.EARTH_OBSERVATION || id.contains("envisat", true) -> {
                // Envisat: compact instrument bus and one long, offset solar-array wing.
                panel(0.57f, 0.20f, 0.98f, 0.80f, 3)
                line(0.48f, 0.50f, 0.62f, 0.50f, 0.045f)
                rounded(0.16f, 0.32f, 0.50f, 0.68f, 0.04f)
                line(0.24f, 0.32f, 0.24f, 0.14f, 0.025f)
            }
            else -> {
                // Starlink: compact flatbus with one broad deployable solar wing.
                panel(0.03f, 0.22f, 0.58f, 0.78f, 3)
                line(0.56f, 0.50f, 0.68f, 0.50f, 0.045f)
                rounded(0.64f, 0.34f, 0.94f, 0.66f, 0.04f)
            }
        }
        paint.color = color
    }

    /** Three pearls on a fine rail — the Satellites screen's Starlink-train marker. */
    fun drawPearl(
        canvas: Canvas,
        color: Int,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        strokeWidthPx: Float,
        paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    ) {
        if (width <= 0f || height <= 0f) return
        val middle = top + height / 2f
        paint.flags = Paint.ANTI_ALIAS_FLAG
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = strokeWidthPx
        paint.color = alpha(color, 0.6f)
        canvas.drawLine(left + width * 0.12f, middle, left + width * 0.88f, middle, paint)
        paint.style = Paint.Style.FILL
        repeat(3) { index ->
            paint.color = color
            val radius = strokeWidthPx * (if (index == 2) 1.25f else 1f)
            canvas.drawCircle(left + width * (0.16f + index * 0.34f), middle, radius, paint)
        }
    }

    private fun alpha(color: Int, factor: Float): Int = android.graphics.Color.argb(
        (android.graphics.Color.alpha(color) * factor).toInt().coerceIn(0, 255),
        android.graphics.Color.red(color),
        android.graphics.Color.green(color),
        android.graphics.Color.blue(color)
    )
}

package com.alijafari.red.astronomy.ui.satellites.core

import kotlin.math.hypot

/** All units are dp. Physical geometry, never start-relative. */
internal data class Point(val x: Float, val y: Float)

internal data class Bounds(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    fun distanceTo(p: Point) = hypot(p.x - p.x.coerceIn(left, right), p.y - p.y.coerceIn(top, bottom))
}

internal data class GlobeFrame(val center: Point, val diameter: Float)

internal data class HudLayout(val top: Float, val bottom: Float, val frame: GlobeFrame, val recenter: Bounds?) {
    /** Apply camera zoom without letting the disc enter the vertical or Recenter keep-outs. */
    fun frameAtDistance(defaultDistance: Float, distance: Float): GlobeFrame {
        val d = distance.coerceAtLeast(1.001f)
        val base = defaultDistance.coerceAtLeast(1.001f)
        val zoom = kotlin.math.sqrt((base * base - 1) / (d * d - 1))
        val diameter = minOf(frame.diameter * zoom, bottom - top - 2,
            recenter?.let { 2 * (it.distanceTo(frame.center) - 1) } ?: Float.MAX_VALUE).coerceAtLeast(0f)
        return frame.copy(diameter = diameter)
    }

    companion object {
        fun measure(
            width: Float,
            height: Float,
            statusInset: Float,
            navHeight: Float,
            cardHeight: Float,
            panelTop: Float? = null,
            recenter: Boolean = false,
            rtl: Boolean = false,
        ): HudLayout {
            val top = (statusInset + 8 + 48).coerceAtMost(height)
            val bottom = (panelTop ?: (height - navHeight - 12 - cardHeight)).coerceIn(top, height)
            val center = Point(width / 2, (top + bottom) / 2)
            val x = if (rtl) 16f else width - 64
            val hit = if (recenter && panelTop == null) Bounds(x, bottom - 60, x + 48, bottom - 12) else null
            val diameter =
                minOf(
                        width * .94f,
                        bottom - top - 16,
                        hit?.let { 2 * (it.distanceTo(center) - 1) } ?: Float.MAX_VALUE,
                    )
                    .coerceAtLeast(0f)
            return HudLayout(top, bottom, GlobeFrame(center, diameter), hit)
        }

        fun sheetHeight(height: Float, full: Boolean, map: Boolean) =
            when {
                full -> .92f * height
                map -> minOf(300f, height * .55f)
                else -> height * if (height < 700) .42f else .46f
            }
    }
}

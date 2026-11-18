package com.alijafari.red.astronomy.ui.satellites

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import kotlin.math.*

internal enum class LayoutRole {
    Mirrors,
    TimeAxis,
    Physical,
    Neutral,
}

internal val LayoutRoleKey = SemanticsPropertyKey<LayoutRole>("LayoutRole")

internal fun Modifier.layoutRole(role: LayoutRole) = semantics { this[LayoutRoleKey] = role }

internal val LocalTimeAxisFollowsLayout = compositionLocalOf { true }

@Composable
internal fun TimeAxis(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalLayoutDirection provides
            if (LocalTimeAxisFollowsLayout.current) LocalLayoutDirection.current else LayoutDirection.Ltr,
        content = content,
    )
}

/**
 * The three tangential Béziers of a continuous corner: transition, circular arc, transition. Cache one immutable
 * outline per physical size (never mutate a path handed to another node).
 */
internal class SquircleShape(
    val cornerRadius: Dp,
    val smoothing: Float = .6f,
    val bottomRadius: Dp = cornerRadius,
    val topEndRadius: Dp = cornerRadius,
    val bottomEndRadius: Dp = bottomRadius,
) : Shape {
    private var sizeKey = Size.Unspecified
    private var densityKey = 0f
    private var directionKey = LayoutDirection.Ltr
    private var last: Outline? = null

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        last?.let { if (size == sizeKey && density.density == densityKey && layoutDirection == directionKey) return it }
        val half = size.minDimension / 2
        val top = with(density) { cornerRadius.toPx() }
        val bottom = with(density) { bottomRadius.toPx() }
        val topEnd = with(density) { topEndRadius.toPx() }
        val bottomEnd = with(density) { bottomEndRadius.toPx() }
        val rtl = layoutDirection == LayoutDirection.Rtl
        val result =
            if (minOf(top, topEnd, bottom, bottomEnd) >= half)
                Outline.Rounded(RoundRect(Rect(Offset.Zero, size), CornerRadius(half)))
            else {
                val w = size.width
                val h = size.height
                val path = Path()
                path.moveTo(w / 2, 0f)
                fun corner(index: Int, radius: Float) {
                    val r = radius.coerceIn(0f, half)
                    val s = if (r == 0f) 0f else smoothing.coerceIn(0f, min(1f, (half / r - 1).coerceAtLeast(0f)))
                    val p = (1 + s) * r
                    val angle = PI / 4 * s
                    val arc = PI / 2 - 2 * angle
                    val reach = r * tan(angle / 2)
                    val c = (reach * cos(angle)).toFloat()
                    val d = (reach * sin(angle)).toFloat()
                    val b = ((p - sqrt(2.0) * r * sin(arc / 2) - c - d) / 3).toFloat()
                    val a = 2 * b
                    fun point(x: Float, y: Float) =
                        when (index) {
                            0 -> Offset(w + x, y)
                            1 -> Offset(w - y, h + x)
                            2 -> Offset(-x, h - y)
                            else -> Offset(y, -x)
                        }
                    fun line(x: Float, y: Float) {
                        val q = point(x, y)
                        path.lineTo(q.x, q.y)
                    }
                    fun curve(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
                        val u = point(x1, y1)
                        val v = point(x2, y2)
                        val q = point(x3, y3)
                        path.cubicTo(u.x, u.y, v.x, v.y, q.x, q.y)
                    }
                    line(-p, 0f)
                    val x = -p + a + b + c
                    if (r > 0) {
                        curve(-p + a, 0f, -p + a + b, 0f, x, d)
                        val k = (r * 4.0 / 3 * tan(arc / 4)).toFloat()
                        val kx = k * cos(angle).toFloat()
                        val ky = k * sin(angle).toFloat()
                        curve(x + kx, d + ky, -d - ky, -x - kx, -d, -x)
                        curve(0f, p - a - b, 0f, p - a, 0f, p)
                    }
                }
                corner(0, if (rtl) top else topEnd)
                corner(1, if (rtl) bottom else bottomEnd)
                corner(2, if (rtl) bottomEnd else bottom)
                corner(3, if (rtl) topEnd else top)
                path.close()
                Outline.Generic(path)
            }
        directionKey = layoutDirection
        sizeKey = size
        densityKey = density.density
        last = result
        return result
    }
}

internal object SatShapes {
    private val radii = mutableMapOf<Dp, SquircleShape>()

    fun of(radius: Dp) = radii.getOrPut(radius) { SquircleShape(radius) }

    val tile = SquircleShape(12.dp)
    val field = SquircleShape(16.dp)
    val card = SquircleShape(24.dp)
    val sheet = SquircleShape(28.dp, bottomRadius = 0.dp)
    val hero = SquircleShape(28.dp)
    val capsule = SquircleShape(1000.dp)
}

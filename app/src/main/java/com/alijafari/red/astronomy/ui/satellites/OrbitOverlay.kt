package com.alijafari.red.astronomy.ui.satellites

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.*
import com.alijafari.red.astronomy.R
import com.alijafari.red.astronomy.astro_engine.*
import com.alijafari.red.astronomy.ui.satellites.core.*
import kotlin.math.*

internal fun ISSEngine.TopocentricPosition.geo() = Geo(subLatDeg, subLonDeg, satAltKm)

internal fun sgp4Ephemeris(s: SatelliteItem): Ephemeris {
    val tle = SatelliteEngine.getEffectiveTle(s)
    return Ephemeris { ms -> ISSEngine.calculateTopocentricPos(ms, 0.0, 0.0, tle = tle).geo() }
}

internal data class Sample(val ms: Long, val geo: Geo)

internal data class Trail(
    val history: List<Sample>,
    val future: List<Sample>,
    val pass: List<Sample>,
    val later: Geo,
    val longer: Geo,
)

internal fun sampleTrail(sat: SatelliteItem, now: Long, pass: ISSEngine.ISSPass?, positions: Ephemeris): Trail {
    val motion = sat.defaultTle.line2.drop(52).take(11).trim().toDoubleOrNull()?.takeIf { it > 0 }
    val period = motion?.let { (86_400_000 / it).toLong() }
    fun samples(start: Long, end: Long, n: Int) =
        List(n + 1) { i ->
            val ms = start + (end - start) * i / n
            Sample(ms, positions.at(ms))
        }
    return Trail(
        samples(now - 90_000, now, 9),
        if (period == null) emptyList() else samples(now, now + period, 192),
        pass?.let { samples(it.startTimeMs, it.endTimeMs, 32) }.orEmpty(),
        positions.at(now + 10_000),
        positions.at(now + 30_000),
    )
}

internal fun priorityFleet(
    states: List<SatelliteLiveState>,
    selected: String?,
    members: Set<String>?,
    favorites: Set<String>,
) =
    states
        .distinctBy { it.satellite.id }
        .sortedWith(
            compareBy<SatelliteLiveState> {
                    val id = it.satellite.id
                    when {
                        id == selected -> 0
                        id in favorites || members?.contains(id) == true -> 1
                        it.satellite.isNakedEyeCandidate && it.topocentric.elevationDeg > 0 -> 2
                        else -> 3
                    }
                }
                .thenBy { it.topocentric.rangeKm },
        )
        .take(60)

private data class Mark(val at: Offset, val width: Float, val shape: Path, val ink: Color, val dot: Boolean)

private data class Line(val path: Path, val alpha: Float)

private data class Arrow(val end: Offset, val previous: Offset, val alpha: Float)

/**
 * The projection and collision pass runs on pose/data changes, outside the Canvas draw loop. All M artwork is shared.
 * Physical offsets are absolute; only the selected name has reading direction.
 */
@Composable
internal fun OrbitOverlay(
    view: GlobeViewport,
    camera: Camera,
    frame: GlobeFrame,
    states: List<SatelliteLiveState>,
    selected: String?,
    members: Set<String>?,
    trail: Trail?,
    tails: Map<String, Geo>,
    showOrbit: Boolean,
    t: ZigSatTokens,
    fa: Boolean,
    onDetail: (SatelliteItem) -> Unit,
    labelModifier: Modifier = Modifier,
    showLabel: Boolean = true,
) {
    val project = remember(frame, camera) { GlobeProjection(frame, camera) }
    val sat = states.firstOrNull { it.satellite.id == selected } ?: return
    val pos = project.project(sat.topocentric.geo())
    val selectedAt = Offset(pos.point.x, pos.point.y)
    val keyline = if (t.isDark) if (t.isWine) Color(0xff0c0307) else Color(0xff05060a) else Color.White
    var lastHeading by remember(selected) { mutableStateOf<Float?>(null) }
    val heading = trail?.let {
        MotionCues.heading(
            pos.point,
            project.project(it.later).point,
            project.project(it.longer).point,
            lastHeading,
            .23f,
        )
    }
    SideEffect { if (heading != null) lastHeading = heading }
    val reduce = LocalSatReduceMotion.current
    Box(Modifier.fillMaxSize()) {
        Spacer(
            Modifier.fillMaxSize().layoutRole(LayoutRole.Physical).testTag("orbit_overlay").drawWithCache {
                val marks = ArrayList<Mark>(states.size)
                val lines = mutableListOf<Line>()
                val arrows = mutableListOf<Arrow>()
                fun line(points: List<Geo>, alpha: Float, flow: Boolean = false, tip: Boolean = false) {
                    val projected = points.map(project::project)
                    var index = 0
                    var flowCount = 0
                    var length = 0f
                    while (index < projected.size - 1) {
                        val far = projected[index].occluded
                        val vertices = mutableListOf<Offset>()
                        val start = index
                        while (index < projected.size && projected[index].occluded == far) {
                            val p = projected[index++].point
                            vertices.add(Offset(p.x, p.y))
                        }
                        if (vertices.size < 2) continue
                        val path = Path()
                        var v = vertices[0]
                        path.moveTo(v.x, v.y)
                        for (i in 1 until vertices.size) {
                            val next = vertices[i]
                            val mid = (v + next) * .5f
                            path.quadraticBezierTo(v.x, v.y, mid.x, mid.y)
                            length += (next - v).getDistance()
                            if (flow && length >= 60 && flowCount < 4) {
                                arrows += Arrow(mid, v, if (far) .10f else .9f - flowCount * .17f)
                                length = 0f
                                flowCount++
                            }
                            v = next
                        }
                        path.lineTo(v.x, v.y)
                        lines += Line(path, if (far) alpha * .16f else alpha)
                        if (tip && start + vertices.size == points.size)
                            arrows += Arrow(v, vertices[vertices.lastIndex - 1], if (far) .18f else .95f)
                    }
                }
                if (trail != null) {
                    line(trail.history.map { it.geo }, .25f)
                    if (showOrbit) line(trail.future.map { it.geo }, .9f, flow = true)
                    line(trail.pass.map { it.geo }, .9f, tip = true)
                }
                for (s in states) {
                    val id = s.satellite.id
                    val chosen = id == selected
                    val p = project.project(s.topocentric.geo())
                    if (p.occluded && !chosen) continue
                    val at = Offset(p.point.x, p.point.y)
                    val rule = shapeOf(s.satellite)
                    val w = if (chosen) 32f else if (rule.id == "starlink") 12f else if (rule.tier == 1) 18f else 14f
                    tails[id]?.let { line(listOf(it, s.topocentric.geo()), .25f) }
                    val collide = marks.any { m ->
                        val x = min(at.x + w / 2, m.at.x + m.width / 2) - max(at.x - w / 2, m.at.x - m.width / 2)
                        val y = min(at.y + w / 2, m.at.y + m.width / 2) - max(at.y - w / 2, m.at.y - m.width / 2)
                        max(0f, x) * max(0f, y) > .30f * min(w * w, m.width * m.width)
                    }
                    val alpha =
                        (if (s.topocentric.isSunlit) 1f else .55f) *
                            (if (!chosen && members != null && id !in members) .2f else 1f) *
                            (if (p.occluded) .18f else 1f)
                    val ink =
                        (if (chosen) t.selected else if (rule.id == "starlink") t.starlink else t.inkPrimary).copy(
                            alpha = alpha,
                        )
                    marks += Mark(at, if (collide) 4f else w, SpacecraftArt.marker(rule.id), ink, collide)
                }
                heading?.let {
                    val rad = Math.toRadians(it.toDouble())
                    val u = Offset(cos(rad).toFloat(), sin(rad).toFloat())
                    arrows += Arrow(selectedAt + u * 26f, selectedAt + u * 19f, if (pos.occluded) .18f else 1f)
                }
                val px = density
                onDrawBehind {
                    scale(px, px, Offset.Zero) {
                        for (i in lines.indices) {
                            val l = lines[i]
                            drawPath(
                                l.path,
                                t.selected.copy(alpha = l.alpha),
                                style = Stroke(1.5f, cap = StrokeCap.Round),
                            )
                        }
                        for (i in arrows.indices) {
                            val a = arrows[i]
                            val d = a.end - a.previous
                            val len = d.getDistance()
                            if (len > .001) {
                                val u = d / len
                                val n = Offset(-u.y, u.x)
                                val b = a.end - u * 5.5f
                                drawLine(t.selected.copy(alpha = a.alpha), b + n * 3f, a.end, 1.5f)
                                drawLine(t.selected.copy(alpha = a.alpha), b - n * 3f, a.end, 1.5f)
                            }
                        }
                        for (i in marks.lastIndex downTo 0) {
                            val m = marks[i]
                            if (m.dot) drawCircle(m.ink, 2f, m.at)
                            else
                                translate(m.at.x - m.width / 2, m.at.y - m.width / 2) {
                                    val k = m.width / 64
                                    scale(k, k, Offset.Zero) {
                                        drawPath(
                                            m.shape,
                                            keyline.copy(alpha = m.ink.alpha),
                                            style = Stroke(2.5f / k, join = StrokeJoin.Round),
                                        )
                                        drawPath(m.shape, m.ink)
                                    }
                                }
                        }
                    }
                }
            },
        )
        val far = pos.occluded
        val label = shortNameOf(sat.satellite, fa)
        val desc = if (far) label + " · " + stringResource(R.string.sat_behind) else label
        if (showLabel && view.layout.bottom - view.layout.top >= 48f) {
            val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
            val width = if (far) 164f else min(176f, max(94f, label.length * 7f + 60f))
            val edge = view.width - 8 - width
            val x =
                if (far) (view.width - width) / 2
                else (selectedAt.x - if (rtl) width * .72f else width * .28f).coerceIn(8f, max(8f, edge))
            val above = selectedAt.y - 65 >= view.layout.top
            val y =
                (if (far) view.layout.bottom - 48 else if (above) selectedAt.y - 65 else selectedAt.y + 17).coerceIn(
                    view.layout.top,
                    max(view.layout.top, view.layout.bottom - 48),
                )
            val tailX = (selectedAt.x - x).coerceIn(14f, width - 14)
            Box(
                Modifier.align(AbsoluteAlignment.TopLeft)
                    .absoluteOffset(x.dp, y.dp)
                    .size(width.dp, 48.dp)
                    .then(labelModifier)
                    .clickable { onDetail(sat.satellite) }
                    .testTag("anchored_sat_label"),
                contentAlignment = Alignment.Center,
            ) {
                if (!far)
                    Canvas(Modifier.matchParentSize()) {
                        val at = if (above) size.height - 8.dp.toPx() else 8.dp.toPx()
                        val h = 6.dp.toPx()
                        val tx = tailX.dp.toPx()
                        val p =
                            Path().apply {
                                moveTo(tx - h, at)
                                lineTo(tx, at + if (above) h else -h)
                                lineTo(tx + h, at)
                                close()
                            }
                        drawPath(p, t.selected, style = Stroke(1.dp.toPx()))
                        drawPath(p, t.card)
                    }
                Row(
                    Modifier.fillMaxWidth()
                        .heightIn(min = 32.dp)
                        .satMaterial(t, SatSlot.Popover, SatShapes.capsule, accent = t.selected)
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (!sat.topocentric.isSunlit) Icon(Icons.Rounded.DarkMode, null, Modifier.size(12.dp), t.umbra)
                    SatText(desc, t.inkPrimary, Modifier.weight(1f), size = 12, strong = true)
                    Disclosure(t)
                }
            }
        }
    }
}

package com.alijafari.red.astronomy.ui.mountain

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.alijafari.red.astronomy.ui.rendering.PersianCalendar
import com.alijafari.red.astronomy.ui.rendering.SkySceneFrame

internal fun mountainSeason(season: PersianCalendar.Season): MountainSeason = when (season) {
    PersianCalendar.Season.SPRING, PersianCalendar.Season.SUMMER -> MountainSeason.GREEN
    PersianCalendar.Season.AUTUMN -> MountainSeason.AUTUMN
    PersianCalendar.Season.WINTER -> MountainSeason.SNOW
}

/** The massif sits behind the ruins; its foot dissolves into the existing valley. */
@Composable
fun HomeMountain(frame: SkySceneFrame, toeFraction: Float, modifier: Modifier = Modifier) {
    var view by remember { mutableStateOf<MountainTextureView?>(null) }
    val season = mountainSeason(PersianCalendar.seasonOf(frame.jd))
    val sunAz = frame.sunHoriz.azimuthDeg.toFloat()
    val sunAlt = frame.sunHoriz.altitudeDeg.toFloat()
    val moonAz = frame.moonData.azimuthDeg.toFloat()
    val moonAlt = frame.moonData.altitudeDeg.toFloat()
    val moonIntensity = (0.22 * frame.moonData.illuminationPercent.coerceIn(0.0, 100.0) / 100.0).toFloat()
    val base = (toeFraction + 0.015f).coerceIn(0f, 1f)

    LaunchedEffect(view, season) { view?.setSeason(season) }
    LaunchedEffect(view, sunAz, sunAlt) { view?.setSun(sunAz, sunAlt) }
    LaunchedEffect(view, moonAz, moonAlt, moonIntensity) { view?.setMoon(moonAz, moonAlt, moonIntensity) }
    LaunchedEffect(view, base) { view?.setFraming(4600f, base) }

    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, view) {
        val current = view
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> current?.onResume()
                Lifecycle.Event.ON_PAUSE -> current?.onPause()
                else -> Unit
            }
        }
        if (current != null) owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    AndroidView(
        factory = { context ->
            MountainTextureView(context).apply {
                setRenderScale(HOME_MOUNTAIN_RENDER_SCALE)
                setSeason(season)
                setSun(sunAz, sunAlt)
                setMoon(moonAz, moonAlt, moonIntensity)
                setFraming(4600f, base)
                view = this
            }
        },
        update = {},
        onRelease = { it.release(); if (view === it) view = null },
        modifier = modifier.drawWithContent {
            drawContext.canvas.saveLayer(Rect(Offset.Zero, size), Paint())
            drawContent()
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.White, 1f to Color.Transparent,
                    startY = size.height * (base - 0.05f), endY = size.height * (base + 0.01f)
                ),
                blendMode = BlendMode.DstIn
            )
            drawContext.canvas.restore()
        }
    )
}

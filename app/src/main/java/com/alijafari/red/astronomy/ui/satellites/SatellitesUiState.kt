package com.alijafari.red.astronomy.ui.satellites

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.listSaver
import com.alijafari.red.astronomy.astro_engine.SatelliteCategory
import com.alijafari.red.astronomy.ui.screens.MapTheme
import kotlin.math.abs
import kotlin.math.roundToInt

internal enum class SatPanel { None, Fleet, Customize, Editor, Time, Passes, Layers, Alerts, Guide, DataStatus;
    val sheet get() = this !in listOf(None, Time, DataStatus)
}
internal enum class MapLayer { GRID, EQUATOR, CITIES, OBSERVER, ORBIT, TRAIN, TETHER }
internal enum class Detent { Half, Full }

@Immutable
internal data class SatAlertsConfig(
    val enabled: Boolean = false,
    val leadMinutes: Int = 10,
    val satelliteIds: Set<String> = setOf("iss_zarya", "tiangong", "starlink_train", "hubble"),
)

/** One panel value makes overlapping HUD destinations unrepresentable. The pass/data snapshots
 * are derived from the live repository, and are deliberately not serialized into the Bundle. */
@Immutable
internal data class SatellitesUiState(
    val focusId: String = "iss_zarya",
    val timeOffsetMin: Int = 0,
    val mapTheme: MapTheme = MapTheme.AUTO,
    val activePanel: SatPanel = SatPanel.None,
    val detent: Detent = Detent.Half,
    val query: String = "",
    val editorId: String? = null,
    val drill: String? = null,
    val visibleOnly: Boolean = true,
    val windowDays: Int = 3,
    val layers: Set<MapLayer> = MapLayer.entries.toSet(),
    val hudHushed: Boolean = false,
)

@Stable
internal class SatellitesState(initial: SatellitesUiState) {
    var ui by mutableStateOf(initial)
        private set

    fun panel(panel: SatPanel) { ui = ui.copy(activePanel = panel, detent = Detent.Half, drill = null, query = "") }
    fun toggle(panel: SatPanel) = panel(if (ui.activePanel == panel) SatPanel.None else panel)
    fun dismiss() = panel(SatPanel.None)
    fun back() { when (ui.activePanel) {
        SatPanel.Editor -> sub(SatPanel.Customize); SatPanel.Customize -> sub(SatPanel.Fleet)
        SatPanel.Alerts -> sub(SatPanel.Passes); SatPanel.Guide -> sub(SatPanel.Layers)
        SatPanel.Fleet -> if (ui.drill != null) drill(null) else dismiss(); else -> dismiss()
    } }
    fun sub(panel: SatPanel, id: String? = null) { ui = ui.copy(activePanel = panel, editorId = id) }
    fun full(value: Boolean) { ui = ui.copy(detent = if(value) Detent.Full else Detent.Half) }
    fun drill(id: String?) { ui = ui.copy(drill = id) }
    fun query(text: String) { ui = ui.copy(query = text, drill = null) }
    fun filterVisible(value: Boolean) { ui = ui.copy(visibleOnly=value) }
    fun days(value: Int) { require(value in listOf(3,7,30)); ui=ui.copy(windowDays=value) }
    fun layer(layer: MapLayer) { ui=ui.copy(layers=if(layer in ui.layers) ui.layers-layer else ui.layers+layer) }
    fun hush(value: Boolean) { ui = ui.copy(hudHushed=value) }
    fun focus(id: String) { ui = ui.copy(focusId = id) }
    fun offset(minutes: Int) { ui = ui.copy(timeOffsetMin = minutes.coerceIn(-720, 720)) }
    fun lighting(theme: MapTheme) { ui = ui.copy(mapTheme = theme) }

    companion object {
        val Saver = listSaver<SatellitesState, String>(
            save = { with(it.ui) { listOf(focusId,timeOffsetMin.toString(),mapTheme.name,activePanel.name,detent.name,query,editorId.orEmpty(),drill.orEmpty(),
                visibleOnly.toString(),windowDays.toString(),layers.joinToString(","){l->l.name}) } },
            restore = { restoreSatelliteState(it) },
        )
    }
}

/** Restore both this version and the old four-field focus/scope/offset/map bundle. */
internal fun restoreSatelliteState(v: List<String>): SatellitesState {
    val defaults = SatellitesUiState()
    val legacy = v.size == 4 && SatelliteCategory.entries.any { it.name == v.getOrNull(1) }
    val offsetIndex = if (legacy) 2 else 1
    val themeIndex = if (legacy) 3 else 2
    val layers = v.getOrNull(10)?.split(',')?.filter(String::isNotBlank)
        ?.mapNotNull { name -> MapLayer.entries.firstOrNull { it.name == name } }?.toSet() ?: defaults.layers
    val days = v.getOrNull(9)?.toIntOrNull()?.takeIf { it in listOf(3, 7, 30) } ?: defaults.windowDays
    return SatellitesState(defaults.copy(
        focusId = v.getOrNull(0)?.takeIf(String::isNotBlank) ?: defaults.focusId,
        timeOffsetMin = (v.getOrNull(offsetIndex)?.toIntOrNull() ?: 0).coerceIn(-720, 720),
        mapTheme = MapTheme.entries.firstOrNull { it.name == v.getOrNull(themeIndex) } ?: defaults.mapTheme,
        activePanel = if (legacy) SatPanel.None else SatPanel.entries.firstOrNull { it.name == v.getOrNull(3) } ?: SatPanel.None,
        detent = Detent.entries.firstOrNull { it.name == v.getOrNull(4) } ?: Detent.Half,
        query = v.getOrNull(5).orEmpty(), editorId = v.getOrNull(6)?.ifEmpty { null },
        drill = v.getOrNull(7)?.ifEmpty { null }, visibleOnly = v.getOrNull(8)?.toBooleanStrictOrNull() ?: true,
        windowDays = days, layers = layers,
    ))
}

@Immutable
internal data class SatTimeMarker(val minute: Int, val quality: SatQuality)

internal fun snapSatelliteTime(minute: Float, markers: List<SatTimeMarker>): Int {
    val raw = minute.coerceIn(-720f, 720f)
    if (abs(raw) <= 4f) return 0
    return markers.minByOrNull { abs(it.minute - raw) }?.takeIf { abs(it.minute - raw) <= 3f }?.minute
        ?: raw.roundToInt()
}

internal fun nextSatelliteTime(markers: List<SatTimeMarker>, offset: Int): Int? =
    markers.asSequence().map { it.minute }.filter { it > offset && it <= 720 }.minOrNull()

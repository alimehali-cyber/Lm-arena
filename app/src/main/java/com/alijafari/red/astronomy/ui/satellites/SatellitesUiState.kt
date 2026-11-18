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

internal enum class SatPanel { None, Fleet, Time, Passes, Layers, Alerts, Guide, DataStatus }

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
    val scope: SatelliteCategory = SatelliteCategory.ALL,
    val timeOffsetMin: Int = 0,
    val mapTheme: MapTheme = MapTheme.AUTO,
    val activePanel: SatPanel = SatPanel.None,
    val legend: Boolean = false,
)

@Stable
internal class SatellitesState(initial: SatellitesUiState) {
    var ui by mutableStateOf(initial)
        private set

    fun panel(panel: SatPanel) { ui = ui.copy(activePanel = panel) }
    fun toggle(panel: SatPanel) = panel(if (ui.activePanel == panel) SatPanel.None else panel)
    fun dismiss() = panel(SatPanel.None)
    fun focus(id: String) { ui = ui.copy(focusId = id, activePanel = SatPanel.None) }
    fun scope(category: SatelliteCategory) { ui = ui.copy(scope = category) }
    fun offset(minutes: Int) { ui = ui.copy(timeOffsetMin = minutes.coerceIn(-720, 720)) }
    fun lighting(theme: MapTheme) { ui = ui.copy(mapTheme = theme, activePanel = SatPanel.None) }
    fun legend(visible: Boolean) { ui = ui.copy(legend = visible) }

    companion object {
        val Saver = listSaver<SatellitesState, String>(
            save = { listOf(it.ui.focusId, it.ui.scope.name, it.ui.timeOffsetMin.toString(), it.ui.mapTheme.name) },
            restore = { SatellitesState(SatellitesUiState(it[0], SatelliteCategory.valueOf(it[1]), it[2].toInt(), MapTheme.valueOf(it[3]))) },
        )
    }
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

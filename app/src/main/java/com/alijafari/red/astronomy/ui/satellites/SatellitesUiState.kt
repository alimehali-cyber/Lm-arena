package com.alijafari.red.astronomy.ui.satellites

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.Saver
import com.alijafari.red.astronomy.astro_engine.ISSEngine
import com.alijafari.red.astronomy.astro_engine.SatelliteItem
import com.alijafari.red.astronomy.domain.UserLocation
import com.alijafari.red.astronomy.astro_engine.SatelliteCategory
import com.alijafari.red.astronomy.ui.screens.MapTheme
import kotlin.math.abs
import kotlin.math.roundToInt

internal enum class SatPanel { None, Fleet, Time, Passes, Layers, Alerts, Guide, DataStatus, Tools, Filters }

/** One family of objects the globe can paint; the Filters popover toggles them independently. */
internal enum class SatGlobeLayer { STARLINK_CONSTELLATION, STARLINK_OVERHEAD, STARLINK_TRAINS, ISS, TIANGONG, HUBBLE, EARTH_OBSERVATION }

/**
 * The globe filter set. Everything is painted by default; [hidden] carries only the layers the user
 * switched off, so an empty set means "show the whole sky". Persisted per device, never in the Bundle
 * saver's positional slots other than the trailing token.
 */
@Immutable
internal data class SatGlobeFilters(val hidden: Set<SatGlobeLayer> = emptySet()) {
    fun shows(layer: SatGlobeLayer): Boolean = layer !in hidden
    val anyStarlink: Boolean get() = shows(SatGlobeLayer.STARLINK_CONSTELLATION) ||
        shows(SatGlobeLayer.STARLINK_OVERHEAD) || shows(SatGlobeLayer.STARLINK_TRAINS)
    fun toggled(layer: SatGlobeLayer): SatGlobeFilters =
        SatGlobeFilters(if (layer in hidden) hidden - layer else hidden + layer)
    fun persisted(): Set<String> = hidden.mapTo(LinkedHashSet<String>()) { it.name }

    companion object {
        val All = SatGlobeFilters()

        /** Rebuilds the set from persisted names, dropping anything this build no longer knows. */
        fun fromPersisted(raw: Set<String>?): SatGlobeFilters {
            val hidden = LinkedHashSet<SatGlobeLayer>()
            for (name in raw.orEmpty()) {
                SatGlobeLayer.entries.firstOrNull { layer -> layer.name == name }?.let { layer -> hidden.add(layer) }
            }
            return SatGlobeFilters(hidden)
        }
    }
}

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
    val globeFilters: SatGlobeFilters = SatGlobeFilters(),
    // The pass card and the passes sheet follow this set of satellite ids; empty means all of them.
    val passScope: Set<String> = emptySet(),
    // A fixed instant is used for an event preview, including events beyond the scrubber range.
    val previewAtMs: Long? = null,
)

@Stable
internal class SatellitesState(initial: SatellitesUiState) {
    var ui by mutableStateOf(initial)
        private set

    fun panel(panel: SatPanel) { ui = ui.copy(activePanel = panel) }
    fun toggle(panel: SatPanel) = panel(if (ui.activePanel == panel) SatPanel.None else panel)
    fun dismiss() = panel(SatPanel.None)
    fun focus(id: String) { ui = ui.copy(focusId = id, activePanel = SatPanel.None) }
    fun select(id: String) { ui = ui.copy(focusId = id) }
    fun scope(category: SatelliteCategory) { ui = ui.copy(scope = category) }
    fun offset(minutes: Int) { ui = ui.copy(timeOffsetMin = minutes.coerceIn(-720, 720), previewAtMs = null) }
    fun previewAt(timestampMs: Long) { ui = ui.copy(previewAtMs = timestampMs, timeOffsetMin = 0) }
    fun lighting(theme: MapTheme) { ui = ui.copy(mapTheme = theme) }
    fun legend(visible: Boolean) { ui = ui.copy(legend = visible) }
    fun filters(value: SatGlobeFilters) { ui = ui.copy(globeFilters = value) }
    fun toggleFilter(layer: SatGlobeLayer) = filters(ui.globeFilters.toggled(layer))

    /** An empty id clears the scope back to "every satellite". */
    fun togglePassScope(id: String) {
        val ids = ui.passScope
        ui = ui.copy(passScope = when {
            id.isEmpty() -> emptySet()
            id in ids -> ids - id
            else -> ids + id
        })
    }

    companion object {
        val Saver = listSaver<SatellitesState, String>(
            save = { listOf(it.ui.focusId, it.ui.scope.name, it.ui.timeOffsetMin.toString(), it.ui.mapTheme.name,
                it.ui.previewAtMs?.toString() ?: "", it.ui.globeFilters.persisted().joinToString(","),
                it.ui.passScope.sorted().joinToString(",")) },
            restore = { SatellitesState(SatellitesUiState(it[0], SatelliteCategory.valueOf(it[1]), it[2].toInt(),
                MapTheme.valueOf(it[3]), previewAtMs = it.getOrNull(4)?.toLongOrNull(),
                globeFilters = SatGlobeFilters.fromPersisted(it.getOrNull(5)?.split(",")?.filter { name -> name.isNotBlank() }?.toSet()),
                passScope = it.getOrNull(6)?.split(",")?.filter { id -> id.isNotBlank() }?.toSet().orEmpty())) },
        )
    }
}


/** The pass snapshot is retained so refreshes cannot substitute another event on return. */
internal data class SatDetailEntry(
    val satellite: SatelliteItem,
    val pass: ISSEngine.ISSPass?,
    val observer: UserLocation,
    val origin: SatPanel,
) {
    val satelliteId: String get() = satellite.id
    val passId: String? get() = pass?.let { "${satellite.id}_${it.startTimeMs}" }
    val timezoneId: String get() = observer.timezoneId
}

/** Transient navigation, owned by MainViewModel, not by the renderer or individual sheets. */
@Stable
internal class SatelliteNavigationState {
    var detail by mutableStateOf<SatDetailEntry?>(null)
    var globeReturn by mutableStateOf<SatDetailEntry?>(null)
    var agendaReferenceMs by mutableStateOf<Long?>(null)
    var agendaSnapshot by mutableStateOf<List<SatPassUi>?>(null)
    var previewObserver by mutableStateOf<UserLocation?>(null)
    var locationAtPreview by mutableStateOf<UserLocation?>(null)
    var returnFromAr by mutableStateOf(false)
    var retainedFocus by mutableStateOf<SatelliteItem?>(null)

    fun beginGlobePreview(entry: SatDetailEntry, agendaTimeMs: Long, currentLocation: UserLocation = entry.observer) {
        require(entry.pass != null)
        globeReturn = entry
        detail = null
        if (entry.origin == SatPanel.Passes) agendaReferenceMs = agendaReferenceMs ?: agendaTimeMs
        else { agendaReferenceMs = null; agendaSnapshot = null }
        previewObserver = entry.observer
        locationAtPreview = currentLocation
    }
    fun returnToPass(): SatDetailEntry? = globeReturn?.also {
        detail = it
        globeReturn = null
    }
}

internal val SatNightOverridesSaver = Saver<Map<Long, Boolean>, ArrayList<String>>(
    save = { map -> ArrayList(map.map { (night, expanded) -> "$night:${if (expanded) 1 else 0}" }) },
    restore = { rows -> rows.associate { row -> row.substringBefore(':').toLong() to (row.substringAfter(':') == "1") } },
)

/**
 * One id that stands for the whole Starlink family inside a pass scope: ticking «استارلینک‌ها» must
 * cover the train and every individually tracked satellite, not just the train's leader.
 */
internal const val STARLINK_SCOPE_ID = "starlink_train"

/** Which satellites the pass card and the passes sheet are allowed to show. Empty means all. */
internal fun passScopeMatches(sat: SatelliteItem, ids: Set<String>): Boolean =
    ids.isEmpty() || sat.id in ids || (STARLINK_SCOPE_ID in ids && sat.category == SatelliteCategory.STARLINK)

/**
 * The pass the dedicated card shows: the soonest one that has not finished yet, whoever flies it.
 * This is deliberately "next", not "best" — the agenda still scores and spotlights passes.
 */
internal fun nextPassInScope(passes: List<SatPassUi>, ids: Set<String>, nowMs: Long): SatPassUi? =
    passes.asSequence()
        .filter { passScopeMatches(it.sat, ids) && it.pass.endTimeMs > nowMs }
        .minByOrNull { it.pass.startTimeMs }

internal fun satelliteDisplayTime(state: SatellitesUiState, nowMs: Long): Long =
    state.previewAtMs ?: (nowMs + state.timeOffsetMin * 60_000L)

internal fun satelliteIsPreview(state: SatellitesUiState): Boolean = state.previewAtMs != null || state.timeOffsetMin != 0

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

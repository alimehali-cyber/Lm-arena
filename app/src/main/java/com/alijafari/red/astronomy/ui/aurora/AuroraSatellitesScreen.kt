package com.alijafari.red.astronomy.ui.aurora

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.alijafari.red.astronomy.astro_engine.ISSEngine
import com.alijafari.red.astronomy.astro_engine.SatelliteCatalog
import com.alijafari.red.astronomy.astro_engine.SatelliteCategory
import com.alijafari.red.astronomy.astro_engine.SatelliteItem
import com.alijafari.red.astronomy.astro_engine.SatelliteLiveState
import com.alijafari.red.astronomy.astro_engine.SatelliteEngine
import com.alijafari.red.astronomy.astro_engine.TimeEngine
import com.alijafari.red.astronomy.domain.AppLanguage
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.MainViewModel
import com.alijafari.red.astronomy.ui.screens.SatelliteDetailScreen
import com.alijafari.red.astronomy.ui.theme.squircleShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

// ============================================================================
// AURORA SATELLITES
// ============================================================================
//
// A live list of what is overhead, and the next pass worth going outside for.
//
// The engine work is SGP4 propagation over every catalogued satellite, which is
// far too heavy for the main thread, so positions are computed on
// [Dispatchers.Default] and published — the list simply keeps showing the last
// complete snapshot until the next one lands. That is system-states DR5 applied
// to data rather than to layout: nothing reflows, nothing blanks, and the
// reading position never jumps.
//
// The screen also refuses to report a single number as a verdict. Every pass and
// every satellite row states *why*: an elevation, a magnitude, a sunlit flag and
// a written reason, so "not visible" is information rather than a red dot.

/** Positions are re-propagated on this cadence. */
private const val AURORA_SAT_REFRESH_MS = 5_000L
/** How far ahead the next-pass search looks. Three days keeps the search cheap. */
private const val AURORA_PASS_SCAN_DAYS = 3

@Composable
fun AuroraSatellitesScreen(
    uiState: MainUiState,
    viewModel: MainViewModel
) {
    val isFa = uiState.language == AppLanguage.PERSIAN
    val location = uiState.userLocation
    val timezone = remember(location.timezoneId) { TimeEngine.resolveTimeZone(location.timezoneId) }

    var category by rememberSaveable { mutableStateOf(SatelliteCategoryFilter.ALL) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }

    if (selectedId != null) {
        val item = remember(selectedId) { SatelliteCatalog.getById(selectedId!!) }
        SatelliteDetailScreen(
            satelliteItem = item,
            userLocation = location,
            language = uiState.language,
            onBack = { selectedId = null }
        )
        return
    }

    // ---- Live positions -----------------------------------------------------
    var states by remember { mutableStateOf<List<SatelliteLiveState>>(emptyList()) }
    var isSyncing by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    DisposableEffect(location, category) {
        val job = scope.launch {
            while (isActive) {
                val snapshot = withContext(Dispatchers.Default) {
                    SatelliteCatalog.getVisibleList()
                        .filter { category.matches(it) }
                        .map { sat ->
                            SatelliteEngine.calculateSatelliteState(
                                satellite = sat,
                                timestampMs = System.currentTimeMillis(),
                                userLatDeg = location.latitude,
                                userLonDeg = location.longitude,
                                userAltMeters = location.elevationMeters
                            )
                        }
                        .sortedWith(
                            compareByDescending<SatelliteLiveState> { it.isNakedEyeVisible }
                                .thenByDescending { it.topocentric.elevationDeg }
                        )
                }
                states = snapshot
                isSyncing = false
                delay(AURORA_SAT_REFRESH_MS)
            }
        }
        onDispose { job.cancel() }
    }

    // ---- The next pass worth watching ---------------------------------------
    var nextPass by remember { mutableStateOf<ISSEngine.ISSPass?>(null) }
    var passSearchDone by remember { mutableStateOf(false) }

    DisposableEffect(location) {
        val job = scope.launch {
            val pass = withContext(Dispatchers.Default) {
                try {
                    ISSEngine.predictPasses(
                        userLatDeg = location.latitude,
                        userLonDeg = location.longitude,
                        startTimestampMs = System.currentTimeMillis(),
                        tle = ISSEngine.cachedTLE,
                        scanDays = AURORA_PASS_SCAN_DAYS,
                        visibleOnly = true
                    ).firstOrNull()
                } catch (t: Throwable) {
                    null
                }
            }
            nextPass = pass
            passSearchDone = true
        }
        onDispose { job.cancel() }
    }

    AuroraSatellitesList(
        isFa = isFa,
        timezone = timezone,
        states = states,
        isSyncing = isSyncing,
        category = category,
        onCategoryChange = { category = it },
        nextPass = nextPass,
        passSearchDone = passSearchDone,
        onOpenSatellite = { selectedId = it },
        onOpenTrack = { viewModel.selectTab(3) }
    )
}

private enum class SatelliteCategoryFilter(val category: SatelliteCategory?) {
    ALL(null),
    ISS(SatelliteCategory.ISS),
    STARLINK(SatelliteCategory.STARLINK),
    HUBBLE(SatelliteCategory.HUBBLE),
    VISIBLE(SatelliteCategory.VISIBLE);

    fun matches(item: SatelliteItem): Boolean =
        category == null || item.category == category
}

@Composable
private fun AuroraSatellitesList(
    isFa: Boolean,
    timezone: java.util.TimeZone,
    states: List<SatelliteLiveState>,
    isSyncing: Boolean,
    category: SatelliteCategoryFilter,
    onCategoryChange: (SatelliteCategoryFilter) -> Unit,
    nextPass: ISSEngine.ISSPass?,
    passSearchDone: Boolean,
    onOpenSatellite: (String) -> Unit,
    onOpenTrack: () -> Unit
) {
    val palette = AuroraTheme.palette

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("aurora_satellites_screen"),
        contentPadding = PaddingValues(
            horizontal = AuroraSpace.screenEdge,
            vertical = AuroraSpace.section
        ),
        verticalArrangement = Arrangement.spacedBy(AuroraSpace.section)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isFa) "ماهواره‌ها" else "Satellites",
                        style = AuroraType.display,
                        color = palette.textPrimary,
                        modifier = Modifier.testTag("satellites_title")
                    )
                    Spacer(modifier = Modifier.height(AuroraSpace.local))
                    Text(
                        text = if (isFa) {
                            "آنچه هم‌اکنون از فراز شما می‌گذرد"
                        } else {
                            "What is passing overhead right now"
                        },
                        style = AuroraType.body,
                        color = palette.textTertiary
                    )
                }
                // The sync state is a written word with a glyph, not a spinning
                // icon on its own: system-states DR2.
                if (isSyncing) {
                    AuroraStatusPill(
                        label = if (isFa) "در حال به‌روزرسانی" else "Tracking",
                        status = AuroraStatus.INFO,
                        icon = Icons.Default.Refresh
                    )
                }
            }
        }

        item {
            AuroraNextPassCard(
                isFa = isFa,
                timezone = timezone,
                pass = nextPass,
                searchDone = passSearchDone,
                onOpenTrack = onOpenTrack
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(AuroraSpace.group)) {
                AuroraSectionHeader(
                    eyebrow = if (isFa) "فیلتر" else "Filter",
                    title = if (isFa) "گروه" else "Group"
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(AuroraSpace.local)) {
                    items(SatelliteCategoryFilter.entries) { filter ->
                        AuroraChip(
                            label = if (isFa) filter.labelFa() else filter.labelEn(),
                            selected = filter == category,
                            onClick = { onCategoryChange(filter) }
                        )
                    }
                }
            }
        }

        item {
            AuroraSectionHeader(
                eyebrow = if (isFa) "زنده" else "Live",
                title = if (isFa) "بالای افق شما" else "Above your horizon",
                supporting = if (isFa) {
                    "هر ${AURORA_SAT_REFRESH_MS / 1000} ثانیه به‌روز می‌شود"
                } else {
                    "Refreshed every ${AURORA_SAT_REFRESH_MS / 1000} seconds"
                }
            )
        }

        if (states.isEmpty() && isSyncing) {
            items(4) {
                AuroraSkeleton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(92.dp)
                )
            }
        } else if (states.isEmpty()) {
            item {
                AuroraEmptyState(
                    title = if (isFa) "ماهواره‌ای در این گروه نیست" else "Nothing in this group",
                    body = if (isFa) {
                        "هیچ ماهواره‌ای در این گروه برای موقعیت شما یافت نشد."
                    } else {
                        "No satellite in this group was found for your location."
                    },
                    icon = Icons.Default.SatelliteAlt
                )
            }
        } else {
            items(states, key = { it.satellite.id }) { state ->
                AuroraSatelliteRow(
                    state = state,
                    isFa = isFa,
                    onClick = { onOpenSatellite(state.satellite.id) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(112.dp)) }
    }
}

private fun SatelliteCategoryFilter.labelEn(): String = when (this) {
    SatelliteCategoryFilter.ALL -> "All"
    SatelliteCategoryFilter.ISS -> "ISS"
    SatelliteCategoryFilter.STARLINK -> "Starlink"
    SatelliteCategoryFilter.HUBBLE -> "Hubble"
    SatelliteCategoryFilter.VISIBLE -> "Visible"
}

private fun SatelliteCategoryFilter.labelFa(): String = when (this) {
    SatelliteCategoryFilter.ALL -> "همه"
    SatelliteCategoryFilter.ISS -> "ایستگاه فضایی"
    SatelliteCategoryFilter.STARLINK -> "استارلینک"
    SatelliteCategoryFilter.HUBBLE -> "هابل"
    SatelliteCategoryFilter.VISIBLE -> "قابل مشاهده"
}

// ----------------------------------------------------------------------------
// Next pass
// ----------------------------------------------------------------------------

/**
 * The one card on this screen that gets the spectral treatment.
 *
 * Colour-emphasis DR3: a group exposes one clear primary emphasis. Here that is
 * "the next pass" — the reason a person opened the screen — and every satellite
 * row below it is the same weight as its peers.
 */
@Composable
private fun AuroraNextPassCard(
    isFa: Boolean,
    timezone: java.util.TimeZone,
    pass: ISSEngine.ISSPass?,
    searchDone: Boolean,
    onOpenTrack: () -> Unit
) {
    val palette = AuroraTheme.palette

    AuroraCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("aurora_next_pass_card"),
        onClick = if (pass != null) onOpenTrack else null,
        enabled = pass != null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (isFa) "گذر بعدی" else "Next pass",
                style = AuroraType.eyebrow,
                color = palette.textTertiary
            )
            if (!searchDone) {
                AuroraStatusPill(
                    label = if (isFa) "در حال جستجو" else "Searching",
                    status = AuroraStatus.INFO
                )
            }
        }

        Spacer(modifier = Modifier.height(AuroraSpace.group))

        when {
            !searchDone -> {
                // The placeholder holds the exact height the headline will take,
                // so the card does not jump when the search lands (DR5).
                AuroraSkeleton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    shape = squircleShape(AuroraRadius.chip)
                )
            }
            pass == null -> {
                Column {
                    Text(
                        text = if (isFa) "گذری در سه روز آینده نیست" else "No pass in the next three days",
                        style = AuroraType.section,
                        color = palette.textPrimary
                    )
                    Spacer(modifier = Modifier.height(AuroraSpace.local))
                    Text(
                        text = if (isFa) {
                            "هیچ گذرِ قابل‌مشاهده‌ای برای این موقعیت یافت نشد. بعداً دوباره بررسی کنید."
                        } else {
                            "No visible pass was found for this location. Check again later."
                        },
                        style = AuroraType.body,
                        color = palette.textTertiary
                    )
                }
            }
            else -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = TimeEngine.formatTime24h(pass.maxTimeMs, isFa, timezone),
                            style = AuroraType.display,
                            color = palette.accent
                        )
                        Spacer(modifier = Modifier.height(AuroraSpace.local))
                        Text(
                            text = if (isFa) {
                                "اوج در ارتفاع ${"%.0f".format(Locale.US, pass.maxElevationDeg)} درجه"
                            } else {
                                "Peak at ${"%.0f".format(Locale.US, pass.maxElevationDeg)}° elevation"
                            },
                            style = AuroraType.label,
                            color = palette.textSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(AuroraSpace.group))
                    AuroraStatusPill(
                        label = if (isFa) pass.classification.labelFa else pass.classification.labelEn,
                        status = auroraStatusForPass(pass.classification),
                        icon = Icons.Default.Visibility
                    )
                }

                Spacer(modifier = Modifier.height(AuroraSpace.group))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AuroraSpace.group)
                ) {
                    AuroraMetricTile(
                        label = if (isFa) "مدت" else "Duration",
                        value = if (isFa) {
                            "${pass.passDurationSec / 60} دقیقه"
                        } else {
                            "${pass.passDurationSec / 60} min"
                        },
                        modifier = Modifier.weight(1f)
                    )
                    AuroraMetricTile(
                        label = if (isFa) "قدر" else "Magnitude",
                        value = "%.1f".format(Locale.US, pass.estimatedMagnitude),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(AuroraSpace.group))
                Text(
                    text = if (isFa) pass.summaryReasonFa else pass.summaryReasonEn,
                    style = AuroraType.body,
                    color = palette.textSecondary
                )
            }
        }
    }
}

private fun auroraStatusForPass(classification: ISSEngine.PassClassification): AuroraStatus = when (classification) {
    ISSEngine.PassClassification.OUTSTANDING,
    ISSEngine.PassClassification.EXCELLENT -> AuroraStatus.GOOD
    ISSEngine.PassClassification.VERY_GOOD,
    ISSEngine.PassClassification.GOOD -> AuroraStatus.INFO
    ISSEngine.PassClassification.MARGINAL,
    ISSEngine.PassClassification.POOR -> AuroraStatus.WARN
    else -> AuroraStatus.BAD
}

// ----------------------------------------------------------------------------
// One satellite
// ----------------------------------------------------------------------------

@Composable
private fun AuroraSatelliteRow(
    state: SatelliteLiveState,
    isFa: Boolean,
    onClick: () -> Unit
) {
    val palette = AuroraTheme.palette
    val above = state.topocentric.elevationDeg >= 0.0
    val status = when {
        state.isNakedEyeVisible -> AuroraStatus.GOOD
        above -> AuroraStatus.INFO
        else -> AuroraStatus.NEUTRAL
    }

    AuroraLiquidSurface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("aurora_satellite_row"),
        shape = squircleShape(AuroraRadius.card),
        tier = AuroraSurfaceTier.CARD,
        onClick = onClick,
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.padding(AuroraSpace.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AuroraSpace.group)
        ) {
            // The mark is a well, matched to the Lab tool marks, so the two
            // screens read as one system.
            AuroraInset(modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.SatelliteAlt,
                        contentDescription = null,
                        tint = if (state.isNakedEyeVisible) palette.accent else palette.textTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isFa) state.satellite.nameFa else state.satellite.nameEn,
                    style = AuroraType.section,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = if (isFa) state.visibilityVerdictFa else state.visibilityVerdictEn,
                    style = AuroraType.caption,
                    color = if (state.isNakedEyeVisible) palette.accent else palette.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(AuroraSpace.local))
                Row(horizontalArrangement = Arrangement.spacedBy(AuroraSpace.group)) {
                    AuroraReadoutChip(
                        label = if (isFa) "ارتفاع" else "Alt",
                        value = "${"%.0f".format(Locale.US, state.topocentric.elevationDeg)}°"
                    )
                    AuroraReadoutChip(
                        label = if (isFa) "سمت" else "Az",
                        value = "${"%.0f".format(Locale.US, state.topocentric.azimuthDeg)}°"
                    )
                    AuroraReadoutChip(
                        label = if (isFa) "فاصله" else "Range",
                        value = "${"%.0f".format(Locale.US, state.topocentric.rangeKm)} km"
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                AuroraStatusDot(status = status)
                Spacer(modifier = Modifier.height(AuroraSpace.local))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = palette.textTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/** A compact label/value pair inside a row, at the local grouping tier. */
@Composable
private fun AuroraReadoutChip(label: String, value: String) {
    val palette = AuroraTheme.palette
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = label, style = AuroraType.eyebrow, color = palette.textTertiary)
        Text(text = value, style = AuroraType.caption, color = palette.textSecondary)
    }
}

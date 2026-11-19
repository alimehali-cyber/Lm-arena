package com.alijafari.red.astronomy.ui.satellites

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import com.alijafari.red.astronomy.R
import com.alijafari.red.astronomy.astro_engine.SatelliteEngine
import com.alijafari.red.astronomy.domain.UserLocation
import com.alijafari.red.astronomy.ui.components.SatelliteEarth3D
import com.alijafari.red.astronomy.ui.screens.MapTheme
import com.alijafari.red.astronomy.ui.theme.squircleShape
import java.util.TimeZone

/** One dated event. The two views share an instant and never mutate the main globe or AR clock. */
@Composable
internal fun SightingGuide(row: SatPassUi, location: UserLocation, isFa: Boolean, tz: TimeZone,
    tokens: ZigSatTokens, mapTheme: MapTheme, realNow: Long, online: Boolean,
    onBack: () -> Unit, actions: SatPassActions, modifier: Modifier = Modifier) {
    val pass = row.pass
    var orbit by rememberSaveable(row.key) { mutableStateOf(false) }
    var phase by rememberSaveable(row.key) { mutableIntStateOf(1) }
    val times = listOf(pass.startTimeMs, pass.maxTimeMs, pass.endTimeMs)
    val bearings = listOf(pass.startAzimuthDeg, pass.maxAzimuthDeg, pass.endAzimuthDeg)
    val labels = listOf(R.string.sat_begins, R.string.sat_highest, R.string.sat_ends)
    val instant = times[phase]
    val city = (if (isFa) location.cityNameFa else location.cityNameEn).ifBlank { tz.id }
    var anchor by remember { mutableStateOf(IntRect.Zero) }
    val canRemind = actions.isReminderEnabled && (pass.startTimeMs - 600_000 > realNow ||
        row.reminderLeadMinutes != null && pass.startTimeMs > realNow)
    Column(modifier.fillMaxSize().background(tokens.canvas).statusBarsPadding().navigationBarsPadding()
        .testTag("sighting_guide")) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ZigSatIconButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.sat_detail_back), tokens, onBack)
            SatText(stringResource(R.string.sat_sighting), tokens.inkPrimary, size = 17, strong = true)
        }
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SatText(stringResource(R.string.sat_observer, city), tokens.accentInk, size = 13, strong = true)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SatAvatar(row.sat, tokens, 44.dp)
                Column(Modifier.weight(1f)) {
                    SatText(row.satName, tokens.inkPrimary, size = 24, strong = true)
                    SatText(SatFmt.dateTime(pass.startTimeMs, isFa, tz) + " · " + stringResource(R.string.sat_local), tokens.inkSecondary)
                }
            }
            if (!online) SatText(stringResource(R.string.sat_passes_cached), tokens.inkSecondary)
            SatText(stringResource(R.string.sat_first_look, SatFmt.compass(pass.startAzimuthDeg, isFa), SatFmt.clock(pass.startTimeMs, isFa, tz)),
                tokens.inkPrimary, size = 18, strong = true)
            Column(Modifier.fillMaxWidth().satMaterial(tokens, SatSlot.Spotlight, squircleShape(20.dp)).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ZigSatChip(stringResource(R.string.sat_local_sky), !orbit, tokens, { orbit = false })
                    ZigSatChip(stringResource(R.string.sat_earth_orbit), orbit, tokens, { orbit = true })
                }
                if (orbit) {
                    val live = remember(row.key, instant, location) { SatelliteEngine.calculateSatelliteState(row.sat, instant,
                        location.latitude, location.longitude, location.elevationMeters) }
                    SatelliteEarth3D(currentSimulationMs = instant, userLocation = location, isFa = isFa, mapTheme = mapTheme,
                        appThemeMode = tokens.themeMode, selectedSatelliteId = row.sat.id, selectedSatItem = row.sat,
                        liveSatelliteStates = listOf(live), mapOverheadStarlinkStates = emptyList(), trainPearlStatesByTrain = emptyList(),
                        allInteractiveMapStates = emptyList(), anchoredSatLabelState = null, overlayStyle = earthOverlayStyle(tokens, tokens.themeMode),
                        onSelectSatellite = {}, onDismissAnchoredLabel = {}, onOpenSatelliteDetail = {}, tokens = tokens,
                        reduceMotion = LocalSatReduceMotion.current, modifier = Modifier.fillMaxWidth().height(210.dp))
                } else SatSkyArc(pass, tokens, isFa, height = 158.dp, nowMs = instant, showCompassMarks = true, train = row.isTrain)
                SatText(stringResource(R.string.sat_globe_time, SatFmt.clock(instant, isFa, tz)), tokens.inkPrimary, strong = true)
                SatText(stringResource(if (orbit) R.string.sat_orbit_key else R.string.sat_path_key), tokens.inkSecondary, size = 11)
                labels.forEachIndexed { i, label ->
                    Row(Modifier.fillMaxWidth().satMaterial(tokens, SatSlot.PassCard, squircleShape(12.dp),
                        backing = if (phase == i) tokens.selectedFill else tokens.cardAlt)
                        .clickable(role = Role.Button) { phase = i }.semantics { selected = phase == i }
                        .heightIn(min = 48.dp).padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SatText(SatFmt.clock(times[i], isFa, tz), tokens.inkPrimary, size = 16, strong = true)
                        SatText(stringResource(label), tokens.inkSecondary, Modifier.weight(1f))
                        SatText(SatFmt.compass(bearings[i], isFa) + if (i == 1) " · " + SatFmt.degrees(pass.maxElevationDeg, isFa) else "",
                            tokens.inkPrimary, strong = true)
                    }
                }
            }
            SatMetaLine(pass, tokens, isFa)
            val reminder = when {
                row.reminderLeadMinutes != null -> stringResource(R.string.sat_scheduled, satLeadLabel(row.reminderLeadMinutes, isFa))
                row.isAutoCovered -> stringResource(R.string.sat_automatic)
                !canRemind -> stringResource(R.string.sat_lead_closed)
                else -> stringResource(R.string.sat_detail_schedule)
            }
            SatDialogButton(reminder, tokens, { actions.onRemind(anchor) }, primary = true, enabled = canRemind,
                modifier = Modifier.fillMaxWidth().onGloballyPositioned { anchor = it.anchorInWindow() })
            SatDialogButton(stringResource(R.string.sat_detail_ar), tokens, actions.onAr, modifier = Modifier.fillMaxWidth())
        }
    }
}

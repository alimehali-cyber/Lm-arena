package com.alijafari.red.astronomy.ui.satellites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.alijafari.red.astronomy.R
import com.alijafari.red.astronomy.ui.screens.MapTheme

@Composable internal fun MapPanel(screen:SatellitesState,t:ZigSatTokens,lighting:(MapTheme)->Unit) {
    SheetBody("map_sheet",stringResource(R.string.sat_map),{SatSheetTitle(stringResource(R.string.sat_map),null,t)}) {
        Row(Modifier.testTag("map_theme_selector"),verticalAlignment=Alignment.CenterVertically){SatText(stringResource(R.string.sat_lighting),t.inkSecondary,Modifier.weight(1f));MapTheme.entries.forEach { m->
            ZigSatChip(stringResource(when(m){MapTheme.AUTO->R.string.sat_auto;MapTheme.DARK->R.string.sat_dark;MapTheme.LIGHT->R.string.sat_light}),m==screen.ui.mapTheme,t,{lighting(m)})}}
        val labels=listOf(R.string.sat_grid,R.string.sat_equator,R.string.sat_cities,R.string.sat_pin,R.string.sat_track,R.string.sat_trains_layer,R.string.sat_tether)
        MapLayer.entries.chunked(2).forEach { pair->Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){pair.forEach{l->Row(Modifier.weight(1f).heightIn(min=48.dp)
            .toggleable(value=l in screen.ui.layers,role=Role.Switch,onValueChange={screen.layer(l)}),verticalAlignment=Alignment.CenterVertically){
                SatText(stringResource(labels[l.ordinal]),t.inkPrimary,Modifier.weight(1f));Switch(l in screen.ui.layers,null)
            }};if(pair.size==1)Spacer(Modifier.weight(1f))} }
        Hairline(t);SatText(stringResource(R.string.sat_legend),t.inkPrimary,strong=true);SatLegendPanel(t,false)
        Row(Modifier.fillMaxWidth().heightIn(min=48.dp).clickable {screen.sub(SatPanel.Guide)},verticalAlignment=Alignment.CenterVertically){SatText(stringResource(R.string.sat_guide),t.inkPrimary,Modifier.weight(1f));Disclosure(t)}
    }
}

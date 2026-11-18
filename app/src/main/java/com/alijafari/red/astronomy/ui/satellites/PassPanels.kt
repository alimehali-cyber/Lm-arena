package com.alijafari.red.astronomy.ui.satellites

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.*
import com.alijafari.red.astronomy.R
import com.alijafari.red.astronomy.astro_engine.ISSEngine.ISSPass
import com.alijafari.red.astronomy.ui.satellites.core.*
import com.alijafari.red.astronomy.ui.theme.RedTypographyTokens
import java.util.TimeZone
import kotlin.math.*

internal fun satBearingBetween(from:Double,to:Double,fraction:Float) = (from+((to-from+540)%360-180)*fraction+360)%360
@Composable private fun DirectionPair(p:ISSPass,t:ZigSatTokens,fa:Boolean) {
    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(2.dp)) {
        SatText("\u2068${SatFmt.compass(p.startAzimuthDeg,fa)}\u2069",t.inkSecondary,size=11)
        Glyph(Icons.AutoMirrored.Rounded.ArrowForward,LayoutRole.Mirrors,t.inkSecondary,12.dp)
        SatText("\u2068${SatFmt.compass(p.endAzimuthDeg,fa)}\u2069",t.inkSecondary,size=11)
    }
}
internal fun DrawScope.arrowTip(at:Offset,previous:Offset,ink:Color,length:Float) {
    val d=at-previous;val norm=d.getDistance();if(norm<.001f)return
    val unit=d/norm; val tangent=Offset(-unit.y,unit.x); val base=at-unit*length
    drawLine(ink,base+tangent*(length*.5f),at,1.5.dp.toPx(),StrokeCap.Round)
    drawLine(ink,base-tangent*(length*.5f),at,1.5.dp.toPx(),StrokeCap.Round)
}
/** Schematic through the actual rise, maximum and set timestamps; no invented SGP4 samples. */
@Composable internal fun PassChart(p:ISSPass,t:ZigSatTokens,fa:Boolean,polar:Boolean,modifier:Modifier) {
    val role=if(polar)LayoutRole.Physical else LayoutRole.TimeAxis
    val rtl=!polar && LocalTimeAxisFollowsLayout.current && LocalLayoutDirection.current==LayoutDirection.Rtl
    val text=rememberTextMeasurer();val style=RedTypographyTokens.badge.copy(color=t.inkSecondary,fontSize=11.sp)
    val labels=listOf(0.0,90.0,180.0,270.0).map { text.measure(SatFmt.compass(it,fa),style) }
    val rise=text.measure(stringResource(R.string.sat_rise),style);val set=text.measure(stringResource(R.string.sat_set),style)
    val speech=stringResource(R.string.sat_sky_description,SatFmt.compass(p.startAzimuthDeg,fa),SatFmt.degrees(p.maxElevationDeg,fa),SatFmt.compass(p.endAzimuthDeg,fa))
    Canvas(modifier.layoutRole(role).testTag(if(polar)"polar_chart" else "pass_curve").semantics {contentDescription=speech}) {
        val span=(p.endTimeMs-p.startTimeMs).coerceAtLeast(1);val peak=((p.maxTimeMs-p.startTimeMs).toFloat()/span).coerceIn(.01f,.99f)
        val radius=min(size.width*.32f,size.height*.37f); val baseline=size.height-8.dp.toPx();val margin=8.dp.toPx()
        fun pt(f:Float):Offset {
            val rising=f<=peak; val phase=if(rising) f/peak else (1-f)/(1-peak)
            val elev=p.maxElevationDeg*sin(phase*PI/2); if(!polar) return Offset(margin+(if(rtl)1-f else f)*(size.width-2*margin),baseline-(baseline-margin)*(elev/90).toFloat())
            val az=Math.toRadians(if(rising)satBearingBetween(p.startAzimuthDeg,p.maxAzimuthDeg,phase) else satBearingBetween(p.endAzimuthDeg,p.maxAzimuthDeg,phase))
            val r=radius*(1-elev/90).toFloat();return center+Offset(-sin(az).toFloat()*r,-cos(az).toFloat()*r)
        }
        if(polar) {
            drawCircle(t.border,radius,style=Stroke(1.dp.toPx()));drawCircle(t.border,radius/2,style=Stroke(.5.dp.toPx()))
            labels.forEachIndexed { i,label ->val a=i*PI/2;val x=center.x-sin(a).toFloat()*(radius+label.size.width*.5f+2.dp.toPx());val y=center.y-cos(a).toFloat()*(radius+label.size.height*.5f)
                drawText(label,topLeft=Offset(x-label.size.width/2,y-label.size.height/2)) }
        } else drawLine(t.border,Offset(margin,baseline),Offset(size.width-margin,baseline),1.dp.toPx())
        var last=pt(0f);for(i in 1..96){val f=i/96f;val cur=pt(f);val time=p.startTimeMs+(span*f).toLong()
            val shadow=(p.shadowEntryMs?.let { time>=it && (p.shadowExitMs==null || p.shadowExitMs<=it || time<p.shadowExitMs) }==true) ||
                (p.shadowExitMs?.let{time<it && (p.shadowEntryMs==null || p.shadowEntryMs>it)}==true)
            if(!shadow || i%6<3)drawLine(if(shadow)t.umbra else t.selected,last,cur,1.5.dp.toPx());last=cur
        };arrowTip(pt(1f),pt(.98f),t.selected,5.dp.toPx());if(polar){listOf(0f to rise,1f to set).forEach{(f,v)->val at=pt(f);drawText(v,topLeft=Offset((at.x+3.dp.toPx()).coerceIn(0f,(size.width-v.size.width).coerceAtLeast(0f)),at.y-v.size.height))}}
    }
}
@Composable internal fun SatSpotlightCard(data:SatSpotlightUi,tokens:ZigSatTokens,isFa:Boolean,actions:SatPassActions,modifier:Modifier=Modifier,
    arcHeight:Dp=96.dp,showActions:Boolean=true,nowMs:Long,realNowMs:Long=nowMs,tz:TimeZone) {
    val p=data.pass.pass;val t=tokens;val fa=isFa;var anchor by remember {mutableStateOf(IntRect.Zero)}
    Column(modifier.fillMaxWidth().testTag("next_pass_spotlight").satMaterial(t,SatSlot.Spotlight,SatShapes.card)
        .then(if(showActions)Modifier.heightIn(max=280.dp).verticalScroll(rememberScrollState()) else Modifier).padding(horizontal=12.dp,vertical=8.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){SatText(data.pass.satName,t.inkSecondary,size=11)
            SatText(if(data.isInProgress) stringResource(R.string.sat_in_progress,SatFmt.countdown(p.endTimeMs-nowMs,fa)) else stringResource(R.string.sat_next_in,SatFmt.countdown(p.startTimeMs-nowMs,fa)),t.inkPrimary,size=24,strong=true)}
            ZigQualityChip(data.pass.quality,p.visibilityScore,t,fa)}
        if(data.pass.sat.isTrain) SatText(stringResource(R.string.sat_train_long,SatFmt.int(data.pass.sat.trainCount,fa)),t.inkSecondary,size=11)
        PassChart(p,t,fa,!showActions,Modifier.fillMaxWidth().height(arcHeight))
        TimeAxis {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){listOf(Triple(R.string.sat_rise,p.startTimeMs,p.startAzimuthDeg),Triple(R.string.sat_max,p.maxTimeMs,p.maxAzimuthDeg),Triple(R.string.sat_set,p.endTimeMs,p.endAzimuthDeg)).forEach{(label,time,az)->
            Column {SatText(stringResource(label)+" · "+SatFmt.compass(az,fa),t.inkSecondary,size=11);SatText(SatFmt.clock(time,fa,tz),t.inkPrimary,size=12,strong=true)}
        }} }
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){SatText(stringResource(R.string.sat_mag_short)+" "+SatFmt.magnitude(p.estimatedMagnitude,fa)+" · "+SatFmt.duration((p.passDurationSec/60).toInt(),fa),t.inkSecondary,size=11);DirectionPair(p,t,fa)}
        p.shadowEntryMs?.let {SatText(stringResource(R.string.sat_shadow_entry,SatFmt.clock(it,fa,tz)),t.inkSecondary,size=11)}
        if(p.isVisible) {val start=p.shadowExitMs?.takeIf{it<=p.maxTimeMs} ?: p.startTimeMs;val end=p.shadowEntryMs?.takeIf{it>=p.maxTimeMs} ?: p.endTimeMs
            SatText(stringResource(R.string.sat_visible_interval,SatFmt.clock(start,fa,tz),SatFmt.clock(end,fa,tz)),t.inkSecondary,size=11)}
        if(showActions)Row(verticalAlignment=Alignment.CenterVertically){SatDialogButton(stringResource(R.string.sat_remind),t,{actions.onRemind(anchor)},enabled=p.startTimeMs-600_000>realNowMs || data.pass.reminderLeadMinutes!=null,
            modifier=Modifier.weight(1f).onGloballyPositioned {anchor=it.anchorInWindow()});SatDialogButton(stringResource(R.string.sat_ar),t,actions.onAr,modifier=Modifier.weight(1f));ZigSatIconButton(Icons.Rounded.Public,stringResource(R.string.sat_show_on_globe),t,actions.onShowOnGlobe)}
    }
}
@Composable private fun PassRow(row:SatPassUi,t:ZigSatTokens,fa:Boolean,actions:SatPassActions,realNow:Long,tz:TimeZone) {
    val p=row.pass;var anchor by remember{mutableStateOf(IntRect.Zero)}
    Row(Modifier.fillMaxWidth().heightIn(min=80.dp).clickable(onClick=actions.onOpenDetail),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){
        TimeAxis { PassChart(p,t,fa,false,Modifier.size(40.dp,24.dp)) };Column(Modifier.weight(1f)) {
            SatText(SatFmt.clock(p.startTimeMs,fa,tz)+"–"+SatFmt.clock(p.endTimeMs,fa,tz),t.inkPrimary,size=18,strong=true)
            Row(horizontalArrangement=Arrangement.spacedBy(4.dp),verticalAlignment=Alignment.CenterVertically){SatText(row.satName,t.inkSecondary,size=11);DirectionPair(p,t,fa);SatText(SatFmt.degrees(p.maxElevationDeg,fa),t.inkSecondary,size=11)}
            SatText(row.quality.label(fa),t.inkSecondary,size=11)
        };ZigBellButton(row.reminderLeadMinutes,row.isAutoCovered,!actions.isReminderEnabled || (row.reminderLeadMinutes==null && p.startTimeMs-600_000<=realNow) || p.startTimeMs<=realNow,t,fa,{actions.onRemind(anchor)},Modifier.onGloballyPositioned{anchor=it.anchorInWindow()})
    };Hairline(t)
}
@Composable internal fun SatNightSection(group:SatNightGroup,expanded:Boolean,shown:Int,cap:Int,tokens:ZigSatTokens,isFa:Boolean,actionsFor:(SatPassUi)->SatPassActions,
    onToggle:()->Unit,onMore:()->Unit,realNow:Long,tz:TimeZone,favorites:Set<String> = emptySet()) {
    var openGroups by remember(group.nightKey){mutableStateOf(emptySet<SatCategory>())}
    Row(Modifier.fillMaxWidth().heightIn(min=56.dp).clickable(onClick=onToggle),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)) { SatText(group.title,tokens.inkPrimary,size=16,strong=true);group.subtitle?.let{SatText(it,tokens.inkSecondary,size=11)} }
        SatText(SatFmt.int(group.passes.size,isFa),tokens.inkSecondary);Glyph(if(expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,LayoutRole.Neutral,tokens.inkTertiary)}
    if(expanded) {val bundles=remember(group,favorites){PassGrouping.rows(group.passes,{it.sat.taxonomy},{it.sat.id in favorites})}
        bundles.take(shown).forEach {list->val first=list.first();val c=first.sat.taxonomy
            if(list.size>1)Row(Modifier.fillMaxWidth().heightIn(min=56.dp).clickable {openGroups=if(c in openGroups)openGroups-c else openGroups+c},verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                SatAvatar(first.sat,tokens);SatText(stringResource(c.label)+" · "+stringResource(R.string.sat_pass_count,SatFmt.int(list.size,isFa)),tokens.inkPrimary,Modifier.weight(1f));Disclosure(tokens)}
            if(list.size==1 || c in openGroups)list.forEach { row->key(row.key){PassRow(row,tokens,isFa,actionsFor(row),realNow,tz)} }
        };if(shown<bundles.size)SatDialogButton(stringResource(R.string.sat_more_passes,SatFmt.int(bundles.size-shown,isFa)),tokens,onMore)
    }
}
@Composable internal fun SatPassesSheet(screen:SatellitesState,lib:Library,t:ZigSatTokens,fa:Boolean,total:Int?,groups:List<SatNightGroup>,spotlight:SatSpotlightUi?,
    now:Long,realNow:Long,tz:TimeZone,actions:(SatPassUi)->SatPassActions,clear:()->Unit,locationPrompt:(()->Unit)?=null) {
    val ui=screen.ui;var menu by remember{mutableStateOf(false)};var overrides by remember(lib.active){mutableStateOf(mapOf<Long,Boolean>())}
    Column(Modifier.fillMaxSize().testTag("passes_sheet")) { SatSheetTitle(stringResource(R.string.sat_passes),null,t){ZigSatIconButton(Icons.Rounded.NotificationsNone,stringResource(R.string.sat_alerts),t,{screen.sub(SatPanel.Alerts)},testTag="sat_alerts")}
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
            ZigSatChip(if(ui.visibleOnly) stringResource(R.string.sat_visible_only) else stringResource(R.string.sat_scope_all,total?.let{SatFmt.int(it,fa)} ?: "—"),ui.visibleOnly,t,{screen.filterVisible(!ui.visibleOnly)})
            Box {ZigSatChip(stringResource(R.string.sat_window_days,SatFmt.int(ui.windowDays,fa)),false,t,{menu=true},trailing={Glyph(Icons.Rounded.ExpandMore,LayoutRole.Neutral,t.inkSecondary)});DropdownMenu(menu,{menu=false}) {listOf(3,7,30).forEach{d->DropdownMenuItem(text={Text(stringResource(R.string.sat_window_days,SatFmt.int(d,fa)))},onClick={screen.days(d);menu=false})}}}
            if(lib.active!=Library.ALL)ZigSatChip(libraryName(lib.active,lib),false,t,clear)
        }
        LazyColumn {if(locationPrompt!=null)item{SatDialogButton(stringResource(R.string.sat_location_prompt),t,locationPrompt)}
            spotlight?.let {item {SatSpotlightCard(it,t,fa,actions(it.pass),nowMs=now,realNowMs=realNow,tz=tz)}}
            if(total==null)item{SatPassSkeletons(t)} else if(groups.isEmpty())item{SatText(stringResource(R.string.sat_no_pass),t.inkSecondary);ZigSatChip(stringResource(R.string.sat_show_all),false,t,{clear();screen.filterVisible(false)})}
            itemsIndexed(groups,key={_,g->g.nightKey}) { i,g->val open=overrides[g.nightKey] ?: (i==0)
                SatNightSection(g,open,Int.MAX_VALUE,Int.MAX_VALUE,t,fa,actions,{overrides=overrides+(g.nightKey to !open)},{},realNow,tz,lib.favorites)}
        }
    }
}

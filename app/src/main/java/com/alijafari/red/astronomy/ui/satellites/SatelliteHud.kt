package com.alijafari.red.astronomy.ui.satellites

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import com.alijafari.red.astronomy.R
import com.alijafari.red.astronomy.astro_engine.SatelliteLiveState
import com.alijafari.red.astronomy.ui.satellites.core.*
import java.util.TimeZone

internal data class GlobeViewport(val width: Float, val height: Float, val layout: HudLayout)
internal fun interface GlobeSurface { @Composable fun Draw(viewport: GlobeViewport) }
internal val LocalSatDebugBounds = compositionLocalOf { false }

@Composable internal fun SatelliteHud(screen: SatellitesState, status: SatStatusData, tokens: ZigSatTokens, config: SatAlertsConfig,
    focused: SatelliteLiveState?, nextFocused: SatPassUi?, now: Long, tz: TimeZone, markers: List<SatTimeMarker>, centered: Boolean,
    firstRun: Boolean, timeActions: SatTimeActions, onLocate: ()->Unit, onCycle: (Int)->Unit, onDetail: ()->Unit,
    onCoachDone: ()->Unit, globe: GlobeSurface, sheet: @Composable (SatPanel)->Unit, avatarModifier: Modifier=Modifier, rising: Boolean?=null) {
    val state=screen.ui; val panel=state.activePanel; val fa=status.isFa
    val reduce=LocalSatReduceMotion.current; val density=LocalDensity.current; val rtl=LocalLayoutDirection.current==LayoutDirection.Rtl
    var coach by rememberSaveable { mutableIntStateOf(0) }
    FrostProvider(tokens) {
    BoxWithConstraints(Modifier.fillMaxSize().clipToBounds().testTag("satellite_screen")) {
        val w=maxWidth.value; val h=maxHeight.value
        val statusInset=WindowInsets.statusBars.asPaddingValues().calculateTopPadding().value
        // The shared floating navigation is 64 dp high with 8 dp padding on both sides.
        val nav=WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().value + 80f
        var measuredCard by remember { mutableFloatStateOf(150f) }; var measuredTray by remember { mutableFloatStateOf(112f) }
        val card=if(panel==SatPanel.Time) measuredTray else measuredCard
        val sheetTop=h-HudLayout.sheetHeight(h,state.detent==Detent.Full,panel==SatPanel.Layers)
        val end = if(panel.sheet) sheetTop else h-nav-12-card
        val bottom by animateFloatAsState(end,tween(if(reduce) 0 else 320),label="free-area")
        val off=!centered && panel==SatPanel.None
        val raw=HudLayout.measure(w,h,statusInset,nav,card,panelTop=if(off) null else bottom,recenter=off,rtl=rtl)
        val viewport=GlobeViewport(w,h,raw)
        val hush by animateFloatAsState(if(state.hudHushed) .35f else 1f,tween(120),label="hush")
        SatelliteStarfield(tokens)
        globe.Draw(viewport)
        if(state.timeOffsetMin!=0) Box(Modifier.matchParentSize().border(2.dp,tokens.sim.copy(alpha=.35f)))
        Box(Modifier.matchParentSize().testTag("sat_primary_hud")) {
        Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().statusBarsPadding().padding(start=16.dp,end=16.dp,top=8.dp).alpha(hush),
            verticalAlignment=Alignment.CenterVertically) {
            SatStatusPill(status,tokens,{screen.toggle(SatPanel.DataStatus)},{screen.panel(SatPanel.Time);timeActions.onLive()})
            Spacer(Modifier.weight(1f));HudToolbar(panel,state.timeOffsetMin!=0,tokens){ screen.toggle(it) }
        }
        if(!panel.sheet) {
            Box(Modifier.align(Alignment.BottomCenter).padding(start=16.dp,end=16.dp,bottom=(nav+12).dp).alpha(hush)) {
                if(panel==SatPanel.Time) Box(Modifier.onSizeChanged { measuredTray=with(density){it.height.toDp().value} }) {
                    TimeAxis { SatTimeTray(tokens,state.timeOffsetMin,now,tz,markers,fa,h<700,timeActions,screen::back) }
                } else if(focused!=null) SatFocusCard(focused,tokens,fa,h<700,w>=412,nextFocused,now+state.timeOffsetMin*60_000L,
                    rising,config.enabled || nextFocused?.reminderLeadMinutes!=null,onDetail,{screen.panel(SatPanel.Passes)},onCycle,
                    Modifier.onSizeChanged { measuredCard=with(density){it.height.toDp().value} },avatarModifier)
            }
            AnimatedVisibility(off,Modifier.align(Alignment.BottomEnd).padding(end=16.dp,bottom=(nav+12+card+12).dp),
                enter=fadeIn(tween(160)),exit=fadeOut(tween(160))) {
                ZigSatIconButton(Icons.Rounded.MyLocation,stringResource(R.string.sat_locate),tokens,onLocate,visualSize=44.dp,testTag="map_reset_view")
            }
        }
        }
        if(panel.sheet) {
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height((h-bottom).coerceAtLeast(0f).dp)
                .satMaterial(tokens,SatSlot.Sheet,SatShapes.sheet,tokens.canvas).pointerInput(Unit) { awaitPointerEventScope { while(true)awaitPointerEvent() } }.navigationBarsPadding().imePadding().padding(horizontal=16.dp)) {
                val full=state.detent==Detent.Full; val action=stringResource(if(full) R.string.sat_collapse else R.string.sat_expand)
                Box(Modifier.fillMaxWidth().height(48.dp).clickable(role=Role.Button,onClickLabel=action){screen.full(!full)}
                    .semantics { contentDescription=action }.pointerInput(full){ var dy=0f
                        detectVerticalDragGestures(onDragStart={dy=0f},onDragEnd={if(dy < -20.dp.toPx()) screen.full(true)
                            if(dy>20.dp.toPx()){if(full) screen.full(false) else screen.back()}}){c,d->c.consume();dy+=d}
                    },contentAlignment=Alignment.Center) { Spacer(Modifier.size(36.dp,4.dp).background(tokens.inkTertiary,SatShapes.capsule)) }
                sheet(panel)
            }
        } else if(panel==SatPanel.DataStatus) SatDataStatusPopover(status,tokens,tz,screen::back,
            Modifier.align(Alignment.TopStart).padding(start=16.dp,end=16.dp,top=raw.top.dp))
        if(firstRun && panel==SatPanel.None) {
            val titles=listOf(R.string.sat_fleet,R.string.sat_time,R.string.sat_map);val bodies=listOf(R.string.sat_coach_fleet,R.string.sat_coach_time,R.string.sat_coach_map)
            Column(Modifier.align(Alignment.TopEnd).padding(end=16.dp,top=raw.top.dp+4.dp).widthIn(max=236.dp)
                .satMaterial(tokens,SatSlot.Popover,SatShapes.field).padding(12.dp)) {
                SatText(stringResource(titles[coach]),tokens.inkPrimary,size=16,strong=true);SatText(stringResource(bodies[coach]),tokens.inkSecondary)
                Row { ZigSatChip(stringResource(R.string.sat_skip),false,tokens,onCoachDone);Spacer(Modifier.weight(1f));ZigSatChip(stringResource(if(coach==2) R.string.sat_got_it else R.string.sat_continue),false,tokens,{if(coach==2)onCoachDone() else coach++}) }
            }
        }
        if(LocalSatDebugBounds.current) Canvas(Modifier.matchParentSize()) {
            fun y(v:Float)=v.dp.toPx()
            drawRect(Color.Red.copy(alpha=.18f),size=Size(size.width,y(raw.top)))
            drawRect(Color.Red.copy(alpha=.18f),Offset(0f,y(raw.bottom)),Size(size.width,size.height-y(raw.bottom)))
            drawCircle(Color.Cyan,y(raw.frame.diameter/2),Offset(y(raw.frame.center.x),y(raw.frame.center.y)),style=Stroke(1.dp.toPx()))
            raw.recenter?.let { drawRect(Color.Yellow,Offset(y(it.left),y(it.top)),Size(y(48f),y(48f)),style=Stroke(1.dp.toPx())) }
        }
    }
    }
}
@Composable private fun SatelliteStarfield(t: ZigSatTokens) {
    val colors=when {t.isWine->listOf(Color(0xff1b070e),Color(0xff0c0307));t.isDark->listOf(Color(0xff0b0d18),Color(0xff05060a));else->listOf(Color.White,Color(0xffece7dc))}
    Spacer(Modifier.fillMaxSize().captureSpace().drawWithCache { val backdrop=Brush.radialGradient(colors,Offset(size.width/2,size.height/2),size.maxDimension*.65f)
        onDrawBehind { drawRect(backdrop);if(t.isDark) repeat(90){i->
            val x=((i*97+53)%229)/229f*size.width;val y=((i*i*31+i*47+23)%307)/307f*size.height
            drawCircle((if(t.isWine) Color(0xffffdec4) else t.stars).copy(alpha=if(t.isWine) .3f else .36f),if(i%11==0) .9.dp.toPx() else .5.dp.toPx(),Offset(x,y))
        } }
    })
}

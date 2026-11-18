package com.alijafari.red.astronomy.ui.satellites

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.tooling.preview.*
import com.alijafari.red.astronomy.astro_engine.*
import com.alijafari.red.astronomy.domain.*
import com.alijafari.red.astronomy.ui.aether.*
import com.alijafari.red.astronomy.ui.components.FloatingBottomBar
import com.alijafari.red.astronomy.ui.screens.SatelliteDetailContent
import com.alijafari.red.astronomy.ui.satellites.core.*
import com.alijafari.red.astronomy.ui.spatial.*
import com.alijafari.red.astronomy.ui.theme.REDTheme
import java.util.TimeZone

@Preview(name="EN",locale="en",widthDp=360,heightDp=800)
@Preview(name="FA",locale="fa",widthDp=360,heightDp=800)
@Preview(name="EN large text",locale="en",widthDp=360,heightDp=800,fontScale=1.3f)
@Preview(name="FA large text",locale="fa",widthDp=360,heightDp=800,fontScale=1.3f)
private annotation class SatLocales
internal enum class ReviewPage { Rest, Recenter, FirstRun, Offline, Catalogue, Category, Search, Customize, Editor, Passes, EmptyPasses, Alerts, Time, Map, Guide, DataStatus, Iss, Tiangong, Hubble, Starlink, Generic, Polar, Shapes }
internal class PreviewPages:PreviewParameterProvider<ReviewPage>{override val values=ReviewPage.entries.asSequence()}
@SatLocales @Composable private fun DefaultDark(@PreviewParameter(PreviewPages::class) page:ReviewPage)=SatPreview(ThemeMode.DARK,skin=SatSkin.DEFAULT,page=page)
@SatLocales @Composable private fun DefaultLight(@PreviewParameter(PreviewPages::class) page:ReviewPage)=SatPreview(ThemeMode.LIGHT,skin=SatSkin.DEFAULT,page=page)
@SatLocales @Composable private fun DefaultWine(@PreviewParameter(PreviewPages::class) page:ReviewPage)=SatPreview(ThemeMode.WINE,skin=SatSkin.DEFAULT,page=page)
@SatLocales @Composable private fun SpatialDark(@PreviewParameter(PreviewPages::class) page:ReviewPage)=SatPreview(ThemeMode.DARK,skin=SatSkin.SPATIAL,page=page)
@SatLocales @Composable private fun SpatialLight(@PreviewParameter(PreviewPages::class) page:ReviewPage)=SatPreview(ThemeMode.LIGHT,skin=SatSkin.SPATIAL,page=page)
@SatLocales @Composable private fun SpatialWine(@PreviewParameter(PreviewPages::class) page:ReviewPage)=SatPreview(ThemeMode.WINE,skin=SatSkin.SPATIAL,page=page)
@SatLocales @Composable private fun AetherDark(@PreviewParameter(PreviewPages::class) page:ReviewPage)=SatPreview(ThemeMode.DARK,skin=SatSkin.AETHER,page=page)
@SatLocales @Composable private fun AetherLight(@PreviewParameter(PreviewPages::class) page:ReviewPage)=SatPreview(ThemeMode.LIGHT,skin=SatSkin.AETHER,page=page)
@SatLocales @Composable private fun AetherWine(@PreviewParameter(PreviewPages::class) page:ReviewPage)=SatPreview(ThemeMode.WINE,skin=SatSkin.AETHER,page=page)

private const val NOW=1_795_026_600_000L
private val tz=TimeZone.getTimeZone("UTC")
private val location=UserLocation(cityNameEn="London",cityNameFa="لندن",latitude=51.5,longitude=0.0,timezoneId="UTC",countryCode="GB")
private fun orbit(index:Int)=Ephemeris { ms->val phase=(ms-NOW)/5_400_000.0*360+index*25
    Geo(35*kotlin.math.sin(Math.toRadians(phase+17)),(phase+20)%360,418.0) }
private val fixtureItems= SatelliteCatalog.satellites+(1..5).map{i->SatelliteCatalog.satellites.last().copy(id="fixture_$i",noradId=0,
    nameEn="Starlink sample $i",nameFa="استارلینک نمونهٔ ${SatFmt.int(i,true)}",tleGroup="starlink",category=SatelliteCategory.STARLINK,designation="")}
private fun live(s:SatelliteItem,i:Int,ms:Long):SatelliteLiveState {val p=orbit(i).at(ms);return SatelliteLiveState(s,
    ISSEngine.TopocentricPosition(if(i%2==0)34.0 else -68.0,225.0,679.0,p.lat,p.lon,p.altKm,7.66,i%3!=1,0.0,0.0,0.0),
    -1.5,true,"","","","",null)}
private fun pass(i:Int)=ISSEngine.ISSPass(NOW+(55L+i*35)*60_000,NOW+(58L+i*35)*60_000,NOW+(61L+i*35)*60_000,68.0,418.0,330.0,225.0,45.0,-1.5,360,-12.0,true,true,
    shadowEntryMs=NOW+(60L+i*35)*60_000,classification=ISSEngine.PassClassification.GOOD,visibilityScore=61,summaryReasonEn="",summaryReasonFa="",detailedReasonsEn=emptyList(),detailedReasonsFa=emptyList(),isVisible=true)

@Composable internal fun SatPreview(theme:ThemeMode, panel:SatPanel=SatPanel.None,detail:Boolean=false,skin:SatSkin=SatSkin.DEFAULT,page:ReviewPage=ReviewPage.Rest,initiallyCentered:Boolean=true) {
    val fa=LocalConfiguration.current.locales[0].language=="fa"
    val t=remember(skin,theme){zigSatTokens(skin,theme)}
    val initial=when(page){ReviewPage.Catalogue,ReviewPage.Category,ReviewPage.Search->SatPanel.Fleet;ReviewPage.Customize->SatPanel.Customize;ReviewPage.Editor->SatPanel.Editor;ReviewPage.Passes,ReviewPage.EmptyPasses->SatPanel.Passes;ReviewPage.Alerts->SatPanel.Alerts;ReviewPage.Time->SatPanel.Time;ReviewPage.Map->SatPanel.Layers;ReviewPage.Guide->SatPanel.Guide;ReviewPage.DataStatus->SatPanel.DataStatus;else->panel}
    val screen=remember(page,panel){SatellitesState(SatellitesUiState(activePanel=initial,query=if(page==ReviewPage.Search)if(fa)"هابل" else "hub" else "",detent=if(page==ReviewPage.Search)Detent.Full else Detent.Half,timeOffsetMin=if(initial==SatPanel.Time)150 else 0))}
    var lib by remember(page){mutableStateOf(Library(active=if(page==ReviewPage.Category)SatCategory.STATION.name else Library.ALL))};val update:LibraryUpdate={lib=it(lib)}
    val repo=remember{CatalogueRepository(fixtureItems)};val offset=screen.ui.timeOffsetMin;val now=NOW+offset*60_000L
    val fleet=remember(now){fixtureItems.mapIndexed{i,s->live(s,i,now)}};val angles=remember(fleet){fleet.associate{it.satellite.id to it.topocentric.elevationDeg}}
    val focus=fleet.firstOrNull{it.satellite.id==screen.ui.focusId} ?: fleet.first()
    val rows by remember(repo,lib,screen.ui.query,fa){repo.rows(screen.ui.query,lib,angles,emptyMap(),fa)}.collectAsState(emptyList())
    val passes=remember(fa,page){if(page==ReviewPage.EmptyPasses) emptyList() else fixtureItems.mapIndexed{i,s->val p=pass(i);SatPassUi(passKey(s,p),s,shortNameOf(s,fa),p,SatNights.nightKey(p.maxTimeMs,tz),null,false)}}
    val markers=remember{passes.map{SatTimeMarker(((it.pass.maxTimeMs-NOW)/60_000).toInt(),it.quality)}}
    val initialDetail=when(page){ReviewPage.Iss->0;ReviewPage.Tiangong->1;ReviewPage.Hubble->2;ReviewPage.Generic->3;ReviewPage.Starlink->4;else->if(detail)0 else -1}
    var opened by remember(page,detail){mutableStateOf(fixtureItems.getOrNull(initialDetail))};val inspection=LocalInspectionMode.current
    if(!inspection)BackHandler(opened==null && screen.ui.activePanel!=SatPanel.None){screen.back()}
    REDTheme(themeMode=theme,isPersian=fa,timestampMs=NOW){ SpatialThemeProvider(enabled=skin==SatSkin.SPATIAL,themeMode=theme){AetherThemeProvider(enabled=skin==SatSkin.AETHER,themeMode=theme){
        CompositionLocalProvider(LocalLayoutDirection provides if(fa)LayoutDirection.Rtl else LayoutDirection.Ltr,LocalZigSatTokens provides t,LocalSatSkin provides skin,LocalSatReduceMotion provides true,LocalFlatSatGlass provides true){
            Box(Modifier.fillMaxSize().background(t.canvas)) {val active=opened
                when {page==ReviewPage.Shapes->Column(Modifier.padding(16.dp)){shapeRules.filter{it.tier==1}.forEach{r->Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                    listOf(Lod.M to 18,Lod.L to 28,Lod.XL to 160).forEach{(lod,size)->Icon(SpacecraftArt.vector(r.id,lod),r.id,Modifier.size(size.dp),t.selected)} }}}
                    page==ReviewPage.Polar->PassChart(passes.first().pass,t,fa,true,Modifier.fillMaxWidth().height(160.dp))
                    active!=null->SatelliteDetailContent(active,fleet.first{it.satellite.id==active.id},location,fa,skin,theme,
                        passes.map{it.pass},null,emptyList(),now,NOW,offset!=0,{opened=null},{_,_,_->},null,{},{},
                        futurePosition=orbit(fixtureItems.indexOf(active)).at(now+10_000))
                    else->{val stub=GlobeStub(fleet,focus.satellite.id,remember(now,focus.satellite.id){sampleTrail(focus.satellite,now,null,orbit(fixtureItems.indexOf(focus.satellite)))},t,fa,detail={opened=it})
                        SatelliteHud(screen,SatStatusData(offset==0,offset,page!=ReviewPage.Offline,if(page==ReviewPage.Offline) 9.0 else .7,NOW-60_000_000,TleDataSource.MEMORY_CACHE,fa),t,SatAlertsConfig(),focus,passes.firstOrNull{it.sat.id==focus.satellite.id},NOW,tz,markers,initiallyCentered && page!=ReviewPage.Recenter,page==ReviewPage.FirstRun,
                            SatTimeActions(screen::offset,{screen.offset(0)},{nextSatelliteTime(markers,offset)?.let(screen::offset)},{screen.offset(offset+it)}),{},{delta->screen.focus(fixtureItems[(fixtureItems.indexOf(focus.satellite)+delta+fixtureItems.size)%fixtureItems.size].id)},{opened=focus.satellite},{},stub,
                            sheet={p->when(p){SatPanel.Fleet->CataloguePanel(screen,repo,rows,lib,update,t,fa,angles.count{it.value>0},{screen.focus(it.id)},{opened=it},{screen.focus(it.id);screen.dismiss()});SatPanel.Customize->CustomizePanel(screen,lib,update,t){id->update{it.delete(id)}}
                                SatPanel.Editor->EditorPanel(screen,repo,lib,update,t,fa);SatPanel.Layers->MapPanel(screen,t,screen::lighting);SatPanel.Guide->SatGuideSheet(t,fa,screen::back);SatPanel.Alerts->SatAlertsSheet(t,fa,SatAlertsConfig(),{_,_->null},screen::back,{screen.back()})
                                SatPanel.Passes->SatPassesSheet(screen,lib,t,fa,passes.size,SatNights.group(passes,now,tz,fa),SatNights.spotlight(passes,now,tz,fa),now,NOW,tz,{r->SatPassActions({opened=r.sat},{},{},{})},{lib=lib.copy(active=Library.ALL)})
                                else->Unit}},rising=true)
                        if(!screen.ui.activePanel.sheet){val mod=Modifier.align(Alignment.BottomCenter);when(skin){SatSkin.DEFAULT->FloatingBottomBar(1,{},mod);SatSkin.SPATIAL->SpatialFloatingNavBar(1,{},mod);SatSkin.AETHER->AetherFloatingNavBar(1,{},mod)}}
                    }
                }
            }
        }
    } } }
}

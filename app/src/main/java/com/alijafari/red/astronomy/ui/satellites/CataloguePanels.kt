package com.alijafari.red.astronomy.ui.satellites

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.*
import com.alijafari.red.astronomy.R
import com.alijafari.red.astronomy.astro_engine.SatelliteItem
import com.alijafari.red.astronomy.ui.satellites.core.*
import java.util.UUID

internal typealias LibraryUpdate = ((Library)->Library)->Unit
private val glyphs = listOf(Icons.Rounded.SatelliteAlt,Icons.Rounded.Public,Icons.Rounded.StarBorder,Icons.Rounded.Visibility,
    Icons.Rounded.Navigation,Icons.Rounded.Science,Icons.Rounded.Layers,Icons.Rounded.BookmarkBorder)
@Composable internal fun libraryName(id:String,lib:Library):String = when(id) {
    Library.ALL -> stringResource(R.string.sat_all);Library.VISIBLE -> stringResource(R.string.sat_visible_now);Library.FAVORITES -> stringResource(R.string.sat_favorites)
    else -> lib.custom.firstOrNull { it.id==id }?.name ?: stringResource(SatCategory.entries.firstOrNull { it.name==id }?.label ?: R.string.sat_other)
}
@Composable private fun Input(text:String,change:(String)->Unit,hint:String,t:ZigSatTokens,modifier:Modifier=Modifier,
    focus:(Boolean)->Unit={},submit:()->Unit={}) {
    BasicTextField(text,change,modifier.fillMaxWidth().heightIn(min=48.dp).background(t.cardAlt,SatShapes.field)
        .onFocusChanged { focus(it.isFocused) }.semantics { contentDescription=hint }.padding(horizontal=12.dp,vertical=10.dp),
        singleLine=true,textStyle=com.alijafari.red.astronomy.ui.theme.RedTypographyTokens.bodySecondary.copy(color=t.inkPrimary),
        cursorBrush=SolidColor(t.inkPrimary),keyboardOptions=KeyboardOptions(imeAction=ImeAction.Done),keyboardActions=KeyboardActions(onDone={submit()}),
        decorationBox={ inner -> Box(contentAlignment=Alignment.CenterStart){if(text.isEmpty()) SatText(hint,t.inkTertiary);inner()} })
}
@Composable private fun ActionIcon(icon:androidx.compose.ui.graphics.vector.ImageVector,label:String,t:ZigSatTokens,click:()->Unit,role:LayoutRole=LayoutRole.Neutral) {
    Box(Modifier.size(48.dp).clickable(role=Role.Button,onClick=click),contentAlignment=Alignment.Center){ Glyph(icon,role,t.inkPrimary,20.dp,label) }
}
private val sortLabels = listOf(R.string.sat_sort_horizon,R.string.sat_sort_name,R.string.sat_angle,R.string.sat_next_pass,R.string.sat_sort_bright)
@OptIn(ExperimentalFoundationApi::class)
@Composable internal fun CataloguePanel(screen:SatellitesState,repo:CatalogueRepository,rows:List<SatRow>,lib:Library,update:LibraryUpdate,
    t:ZigSatTokens,fa:Boolean,aboveCount:Int,focus:(SatelliteItem)->Unit,details:(SatelliteItem)->Unit,show:(SatelliteItem)->Unit) {
    val ui=screen.ui
    var searchFocus by remember {mutableStateOf(false)}; var sortMenu by remember { mutableStateOf(false) };val focusManager=LocalFocusManager.current
    fun record() {val q=ui.query.trim();if(q.isNotEmpty())update { it.copy(recent=(listOf(q)+it.recent.filterNot { v->v==q }).take(5)) }}
    val cats=remember(repo){repo.rows.mapTo(HashSet()) { it.craft.category.name }}
    var add by remember {mutableStateOf<SatRow?>(null)}
    val count={v:Int->SatFmt.int(v,fa)}
    Column(Modifier.fillMaxSize().testTag("fleet_sheet")) {
        SatSheetTitle(ui.drill?.let { libraryName(it,lib) } ?: stringResource(R.string.sat_fleet),stringResource(R.string.sat_catalogue_count,count(aboveCount),count(repo.rows.size)),t) {
            Box {ActionIcon(Icons.Rounded.Sort,stringResource(R.string.sat_sort),t,{sortMenu=true});DropdownMenu(sortMenu,{sortMenu=false}) {
                SatSort.entries.forEach { s -> DropdownMenuItem(text={SatText(stringResource(sortLabels[s.ordinal]),t.inkPrimary)},onClick={update{it.copy(sort=s)};sortMenu=false}) }
            } }
        }
        Input(ui.query,{screen.query(it.take(120))},stringResource(R.string.sat_search),t,Modifier.testTag("sat_catalogue_search"),
            {searchFocus=it;if(it)screen.full(true)},{record();focusManager.clearFocus()})
        if(searchFocus && ui.query.isEmpty() && lib.recent.isNotEmpty()) LazyRow {items(lib.recent){q->ZigSatChip(q,false,t,{screen.query(q);focusManager.clearFocus()})} }
        LazyRow(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
            item { ZigSatChip(stringResource(R.string.sat_customize),false,t,{focusManager.clearFocus();screen.sub(SatPanel.Customize)},leading={Icon(Icons.Rounded.Tune,null,Modifier.size(16.dp),t.inkPrimary)}) }
            items(lib.order.filter { it !in lib.hidden && (it !in SatCategory.entries.map(SatCategory::name) || it in cats) },key={it}) { id ->
                val custom=lib.custom.firstOrNull{it.id==id}
                ZigSatChip(libraryName(id,lib),lib.active==id && ui.query.isBlank(),t,{update{it.copy(active=id)};screen.query("");focusManager.clearFocus()},
                    leading=custom?.let { c -> @Composable { Icon(glyphs[c.glyph.coerceIn(glyphs.indices)],null,Modifier.size(16.dp),if(lib.active==id) ColorWhite else t.inkPrimary) } })
            }
        }
        add?.let { row ->
            SatText(stringResource(R.string.sat_add_collection),t.inkSecondary)
            LazyRow { items(lib.custom.filter { it.rules==null }) { c -> ZigSatChip(c.name,false,t,{update { l->l.copy(custom=l.custom.map { if(it.id==c.id) it.copy(members=it.members+row.item.id) else it }) };add=null}) }
                item {ZigSatChip(stringResource(R.string.sat_new_collection),false,t,{screen.sub(SatPanel.Editor,"+"+row.item.id);add=null})} }
        }
        val groups=remember(rows,ui.drill){rows.filter { ui.drill==null || it.craft.category.name==ui.drill }.groupBy { it.craft.category }}
        if(groups.isEmpty()) {SatText(stringResource(R.string.sat_no_fleet),t.inkSecondary,Modifier.padding(vertical=12.dp));ZigSatChip(stringResource(if(ui.query.isNotBlank()) R.string.sat_clear_search else R.string.sat_show_all),false,t,{screen.query("");update{it.copy(active=Library.ALL)}})}
        LazyColumn(Modifier.fillMaxWidth(),state=key(ui.drill,ui.query,lib.active){rememberLazyListState()}) {
            groups.forEach { (category,list) ->
                stickyHeader { SatText(stringResource(category.label),t.inkSecondary,Modifier.fillMaxWidth().background(t.canvas).padding(vertical=6.dp),size=11,strong=true) }
                val grouped=category==SatCategory.STARLINK && ui.drill==null
                val individual=if(grouped)list.filter { it.item.isTrain } else list
                if(grouped && list.any{!it.item.isTrain}) item("starlink_group") {
                    val members=list.filterNot { it.item.isTrain };Row(Modifier.fillMaxWidth().heightIn(min=56.dp).clickable {screen.drill(category.name)},
                        horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
                        SatAvatar(members.first().item,t);Column(Modifier.weight(1f)){SatText(stringResource(category.label),t.inkPrimary,strong=true);SatText(stringResource(R.string.sat_catalogue_count,count(members.count{(it.elevation ?: -90.0)>0}),count(members.size)),t.inkSecondary,size=11)};Disclosure(t)
                    };Hairline(t)
                }
                val limit=if(ui.drill==null && individual.size>8)5 else individual.size
                items(individual.take(limit),key={it.item.id}) { row ->
                    val selected=row.item.id==ui.focusId
                    CatalogueRow(row,ui.query,selected,row.item.id in lib.favorites,fa,t,
                        tap={record();if(selected)details(row.item)else focus(row.item)},favorite={update{it.toggleFavorite(row.item.id)}},
                        add={add=row},show={show(row.item)},details={details(row.item)})
                }
                if(limit<individual.size)item{ZigSatChip(stringResource(R.string.sat_see_all,count(individual.size)),false,t,{screen.drill(category.name);screen.full(true)})}
            }
        }
    }
}
private val ColorWhite=androidx.compose.ui.graphics.Color.White
@OptIn(ExperimentalFoundationApi::class)
@Composable private fun CatalogueRow(row:SatRow,query:String,selected:Boolean,fav:Boolean,fa:Boolean,t:ZigSatTokens,tap:()->Unit,
    favorite:()->Unit,add:()->Unit,show:()->Unit,details:()->Unit) {
    var menu by remember {mutableStateOf(false)};val rtl=LocalLayoutDirection.current==LayoutDirection.Rtl
    val callbacks=listOf(favorite,add,show,details);val labels=listOf(stringResource(if(fav)R.string.sat_unfavorite else R.string.sat_favorite),stringResource(R.string.sat_add_collection),stringResource(R.string.sat_show_on_globe),stringResource(R.string.sat_details))
    val currentFavorite by rememberUpdatedState(favorite)
    Box {
        Row(Modifier.fillMaxWidth().heightIn(min=56.dp).background(if(selected)t.selectedFill else t.canvas)
            .combinedClickable(onClick=tap,onLongClick={menu=true}).semantics { customActions=labels.mapIndexed { i,s-> CustomAccessibilityAction(s){callbacks[i]();true} } }
            .pointerInput(rtl){var dx=0f;detectHorizontalDragGestures(onDragStart={dx=0f},onDragEnd={if(dx*(if(rtl)-1 else 1)>40.dp.toPx())currentFavorite()}){c,d->c.consume();dx+=d}},
            verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Spacer(Modifier.width(3.dp).height(32.dp).background(if(selected)t.selected else androidx.compose.ui.graphics.Color.Transparent));SatAvatar(row.item,t)
            Column(Modifier.weight(1f)) {
                val name=if(row.item.isTrain) stringResource(R.string.sat_train_name)+" · "+SatFmt.int(row.item.trainCount,fa) else shortNameOf(row.item,fa)
                Row(verticalAlignment=Alignment.CenterVertically){Text(highlight(name,query),Modifier.weight(1f,fill=false),color=t.inkPrimary,style=com.alijafari.red.astronomy.ui.theme.RedTypographyTokens.bodySecondary)
                    if(row.craft.nakedEye)Icon(Icons.Rounded.Visibility,stringResource(R.string.sat_legend_naked),Modifier.padding(start=4.dp).size(14.dp),t.inkSecondary)}
                SatText(stringResource(row.craft.category.label),t.inkSecondary,size=11)
            }
            Column(horizontalAlignment=Alignment.End){SatText(row.elevation?.let{SatFmt.degrees(it,fa)} ?: "—",t.inkPrimary,strong=true)
                SatText(stringResource(when { row.elevation==null -> R.string.sat_unknown;row.elevation>0->R.string.sat_above_horizon;else->R.string.sat_below_horizon }),t.inkSecondary,size=11)}
        }
        DropdownMenu(menu,{menu=false}){labels.forEachIndexed{i,s->DropdownMenuItem(text={Text(s)},onClick={menu=false;callbacks[i]()})}}
    };Hairline(t)
}
private fun highlight(text:String,q:String)= buildAnnotatedString {
    append(text);if(q.isNotBlank())for(match in Regex("[\\p{L}\\p{N}]+").findAll(text)) {
        val term=SearchNormalizer.normalize(q).split(' ').firstOrNull { it.isNotEmpty() && SearchNormalizer.normalize(match.value).startsWith(it) }
        if(term!=null)addStyle(SpanStyle(fontWeight=FontWeight.SemiBold),match.range.first,match.range.first+minOf(match.value.length,term.length))
    }
}
@Composable internal fun CustomizePanel(screen:SatellitesState,lib:Library,update:LibraryUpdate,t:ZigSatTokens,deleted:(String)->Unit) {
    Column {SatSheetTitle(stringResource(R.string.sat_customize),null,t){ActionIcon(Icons.AutoMirrored.Rounded.ArrowBack,stringResource(R.string.sat_detail_back),t,screen::back,LayoutRole.Mirrors)}
        LazyColumn {items(lib.order,key={it}){ id ->val custom=lib.custom.firstOrNull { it.id==id };val name=libraryName(id,lib)
            fun move(step:Int)=update { l->val order=l.order.toMutableList();val from=order.indexOf(id);if(from>=0){order.removeAt(from);order.add((from+step).coerceIn(0,order.size),id)};l.copy(order=order) }
            Row(Modifier.fillMaxWidth().heightIn(min=56.dp),verticalAlignment=Alignment.CenterVertically) {
                Switch(id !in lib.hidden,{visible->update{l->l.copy(hidden=if(visible)l.hidden-id else l.hidden+id,active=if(!visible&&l.active==id)Library.ALL else l.active)}},Modifier.semantics{contentDescription=name})
                SatText(name,t.inkPrimary,Modifier.weight(1f));ActionIcon(Icons.Rounded.KeyboardArrowUp,stringResource(R.string.sat_move_up),t,{move(-1)});ActionIcon(Icons.Rounded.KeyboardArrowDown,stringResource(R.string.sat_move_down),t,{move(1)})
                if(custom!=null){ActionIcon(Icons.Rounded.Edit,stringResource(R.string.sat_edit_collection),t,{screen.sub(SatPanel.Editor,id)});ActionIcon(Icons.Rounded.DeleteOutline,stringResource(R.string.sat_delete),t,{deleted(id)})}
            };Hairline(t)
        };item { SatDialogButton(stringResource(R.string.sat_new_collection),t,{screen.sub(SatPanel.Editor)},modifier=Modifier.fillMaxWidth()) } }
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun EditorPanel(screen:SatellitesState,repo:CatalogueRepository,lib:Library,update:LibraryUpdate,t:ZigSatTokens,fa:Boolean) {
    val id=screen.ui.editorId;val edit=lib.custom.firstOrNull{it.id==id};var name by rememberSaveable(id){mutableStateOf(edit?.name.orEmpty())}
    var glyph by rememberSaveable(id){mutableIntStateOf(edit?.glyph ?: 0)};var rules by remember(id){mutableStateOf(edit?.rules ?: Rules())}
    var auto by remember(id){mutableStateOf(edit?.rules!=null)};var picked by remember(id){mutableStateOf(edit?.members ?: id?.takeIf{it.startsWith('+')}?.let { setOf(it.drop(1)) }.orEmpty())}
    val count=remember(rules,auto,picked,repo){repo.rows.count{if(auto)rules.matches(it.craft) else it.item.id in picked}}
    Column { SatSheetTitle(stringResource(R.string.sat_edit_collection),stringResource(R.string.sat_match_count,SatFmt.int(count,fa)),t){
        SatDialogButton(stringResource(R.string.sat_save),t,{val key=edit?.id ?: UUID.randomUUID().toString();val def=CollectionDef(key,name.trim(),glyph,picked,if(auto)rules else null)
            update {l->l.copy(custom=l.custom.filterNot{it.id==key}+def,order=if(key in l.order)l.order else l.order+key)};screen.sub(SatPanel.Customize)},enabled=name.isNotBlank()&&count>0)
    };LazyColumn {item {Input(name,{name=it.take(24)},stringResource(R.string.sat_collection_name),t);LazyRow{items(8){i->ActionIcon(glyphs[i],stringResource(R.string.sat_glyph,SatFmt.int(i+1,fa)),t,{glyph=i})}}
        Row {ZigSatChip(stringResource(R.string.sat_pick),!auto,t,{auto=false});ZigSatChip(stringResource(R.string.sat_automatic),auto,t,{auto=true})}
    };if(auto)item { FlowRow { SatCategory.entries.forEach { c->ZigSatChip(stringResource(c.label),c in rules.categories,t,{rules=rules.copy(categories=if(c in rules.categories)rules.categories-c else rules.categories+c)}) } }
        Row(verticalAlignment=Alignment.CenterVertically){SatText(stringResource(R.string.sat_naked_only),t.inkPrimary,Modifier.weight(1f));Switch(rules.nakedEyeOnly,{rules=rules.copy(nakedEyeOnly=it)})}
        FlowRow{Orbit.entries.forEach{ o->ZigSatChip(stringResource(listOf(R.string.sat_leo,R.string.sat_meo,R.string.sat_geo)[o.ordinal]),rules.orbit==o,t,{rules=rules.copy(orbit=if(rules.orbit==o)null else o)})}}
        Input(rules.nameContains,{rules=rules.copy(nameContains=it.take(120))},stringResource(R.string.sat_contains),t)
    } else items(repo.rows,key={it.item.id}) {row->Row(Modifier.fillMaxWidth().heightIn(min=56.dp).clickable {val k=row.item.id;picked=if(k in picked)picked-k else picked+k},verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
        SatAvatar(row.item,t);SatText(shortNameOf(row.item,fa),t.inkPrimary,Modifier.weight(1f));Checkbox(row.item.id in picked,null)
    };Hairline(t)} }
    }
}

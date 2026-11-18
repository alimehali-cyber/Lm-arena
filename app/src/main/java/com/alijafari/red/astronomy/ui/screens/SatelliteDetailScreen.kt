package com.alijafari.red.astronomy.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.R
import com.alijafari.red.astronomy.astro_engine.*
import com.alijafari.red.astronomy.data.repository.TleRepository
import com.alijafari.red.astronomy.domain.AppLanguage
import com.alijafari.red.astronomy.domain.ThemeMode
import com.alijafari.red.astronomy.domain.UserLocation
import com.alijafari.red.astronomy.notification.ScheduledNotificationItem
import com.alijafari.red.astronomy.ui.satellites.*
import com.alijafari.red.astronomy.ui.theme.RedShapes
import com.alijafari.red.astronomy.ui.theme.RedTypographyTokens
import com.alijafari.red.astronomy.ui.satellites.core.*
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SatelliteDetailScreen(
    satelliteItem: SatelliteItem, userLocation: UserLocation, language: AppLanguage, onBack: () -> Unit,
    simulationTimestampMs: Long = System.currentTimeMillis(), skin: SatSkin = SatSkin.DEFAULT,
    preselectedPass: ISSEngine.ISSPass? = null, reminderAlarms: List<ScheduledNotificationItem> = emptyList(),
    isSimulated: Boolean = false, onSetReminder: ((SatelliteItem, ISSEngine.ISSPass, Int) -> Unit)? = null,
    onRemoveReminder: ((SatelliteItem, ISSEngine.ISSPass) -> Unit)? = null,
    onShowOnGlobe: ((SatelliteItem) -> Unit)? = null, onSpotInAr: ((SatelliteItem) -> Unit)? = null,
    themeMode: ThemeMode = ThemeMode.DARK, modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val tleRepo = remember { TleRepository.getInstance(context) }
    val starlinkCacheVersion by tleRepo.starlinkVersion.collectAsState()
    val isFa = language == AppLanguage.PERSIAN
    // Preserve the selected dynamic SatelliteItem: an unresolved catalogue ID must not fall
    // through a generic lookup and accidentally display the ISS in another craft's Detail.
    val state = remember(satelliteItem, simulationTimestampMs, userLocation, starlinkCacheVersion) {
        SatelliteEngine.calculateSatelliteState(satelliteItem, simulationTimestampMs, userLocation.latitude,
            userLocation.longitude, userLocation.elevationMeters)
    }
    val futurePosition by produceState<Geo?>(null, satelliteItem, simulationTimestampMs, starlinkCacheVersion) {
        value = withContext(Dispatchers.Default) {
            runCatching { sgp4Ephemeris(satelliteItem).at(simulationTimestampMs + 10_000L) }.getOrNull()
        }
    }
    var predicted by remember(satelliteItem.id, userLocation) { mutableStateOf<List<ISSEngine.ISSPass>?>(null) }
    val days = 7
    val bucket = simulationTimestampMs / 900_000L * 900_000L - 900_000L
    LaunchedEffect(satelliteItem.id, userLocation, bucket, starlinkCacheVersion, state.tleMetadata?.tleData) {
        predicted = withContext(Dispatchers.Default) {
            ISSEngine.predictPasses(userLocation.latitude, userLocation.longitude, bucket,
                tle = SatelliteEngine.getEffectiveTle(satelliteItem), scanDays = days + 1,
                visibleOnly = true, standardMag = satelliteItem.standardMagnitude)
        }
    }
    val start = simulationTimestampMs
    val upcoming = predicted?.filter { it.endTimeMs > start && it.startTimeMs <= start + days * 86_400_000L }
    SatelliteDetailContent(satelliteItem, state, userLocation, isFa, skin, themeMode, upcoming,
        preselectedPass, reminderAlarms, start, System.currentTimeMillis(), isSimulated, onBack, onSetReminder,
        onRemoveReminder, { onShowOnGlobe?.invoke(satelliteItem) }, { onSpotInAr?.invoke(satelliteItem) }, modifier,
        futurePosition = futurePosition)
}

/** Presentation-only body also used by the deterministic preview matrix. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SatelliteDetailContent(
    satelliteItem: SatelliteItem, state: SatelliteLiveState, location: UserLocation, isFa: Boolean, skin: SatSkin,
    themeMode: ThemeMode, predicted: List<ISSEngine.ISSPass>?, preselected: ISSEngine.ISSPass?,
    alarms: List<ScheduledNotificationItem>, simNow: Long, realNow: Long, isSimulated: Boolean, onBack: () -> Unit,
    onSetReminder: ((SatelliteItem, ISSEngine.ISSPass, Int) -> Unit)?, onRemoveReminder: ((SatelliteItem, ISSEngine.ISSPass) -> Unit)?,
    onShow: () -> Unit, onAr: () -> Unit, modifier: Modifier = Modifier, futurePosition: Geo? = null,
) {
    val tokens = remember(skin, themeMode) { zigSatTokens(skin, themeMode) }
    val tz = remember(location.timezoneId) { TimeEngine.resolveTimeZone(location.timezoneId) }
    val reduce = LocalSatReduceMotion.current
    val nights = remember(location.latitude, location.longitude, tz) { SatObserverNights(location.latitude, location.longitude, tz) }
    fun passUi(pass: ISSEngine.ISSPass): SatPassUi {
        val matching = alarms.filter { it.triggerTimeMs > realNow && it.coversSatellitePass(satelliteItem, pass.startTimeMs) }
        return SatPassUi(passKey(satelliteItem, pass), satelliteItem, shortNameOf(satelliteItem, isFa), pass,
            nights.key(pass.maxTimeMs), matching.firstOrNull { it.manualPass }?.leadMinutes, matching.any { !it.manualPass })
    }
    val passes = remember(predicted, alarms, nights, isFa) { predicted?.map(::passUi) }
    val firstNight = remember(nights, simNow / 60_000L) { nights.key(simNow) }
    val groups = remember(passes, firstNight, tz, isFa) {
        SatNights.group(passes.orEmpty(), simNow, tz, isFa, includeSolarHijri = true, referenceNight = firstNight)
    }
    val spotlight = SatNights.spotlight(passes.orEmpty(), simNow, tz, isFa, simulated = isSimulated)
    var selectedPass by remember(preselected) { mutableStateOf(preselected) }
    val preview = selectedPass?.let(::passUi)
    val shownSpotlight = preview?.let {
        val active = simNow in it.pass.startTimeMs until it.pass.endTimeMs
        SatSpotlightUi(it, active, 0, isSimulated && active)
    } ?: spotlight
    val listState = rememberLazyListState()
    val dayState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val thresholdPx = with(LocalDensity.current) { 120.dp.roundToPx() }
    val showsTitle by remember(thresholdPx) { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > thresholdPx } }
    val visibleGroup by remember(groups) { derivedStateOf { groups.getOrNull(listState.firstVisibleItemIndex - 6)?.nightKey } }
    var expanded by remember { mutableStateOf<Map<Long, Boolean>>(emptyMap()) }
    var showMaximum by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var pending by remember { mutableStateOf<SatReminderTarget?>(null) }
    var heroBell by remember { mutableStateOf(IntRect.Zero) }
    if (!LocalInspectionMode.current) BackHandler(enabled = pending == null, onBack = onBack)
    val dayCount = 7
    val days = remember(firstNight, nights, dayCount, groups) {
        (List(dayCount) { nights.plusDays(firstNight, it) } + groups.map { it.nightKey }).distinct().sorted()
    }
    LaunchedEffect(visibleGroup) {
        val day = days.indexOf(visibleGroup)
        if (day >= 0) dayState.scrollToItem((day - 1).coerceAtLeast(0))
    }
    fun scrollTo(index: Int) { scope.launch { if (reduce) listState.scrollToItem(index) else listState.animateScrollToItem(index) } }
    fun actions(pass: SatPassUi) = SatPassActions(
        onOpenDetail = { selectedPass = pass.pass; scrollTo(1) }, onShowOnGlobe = onShow, onAr = onAr,
        onRemind = { pending = SatReminderTarget(pass, it) }, isReminderEnabled = onSetReminder != null,
    )
    CompositionLocalProvider(LocalZigSatTokens provides tokens, LocalSatSkin provides skin) {
        Box(Modifier.fillMaxSize().background(tokens.canvas).testTag("satellite_detail_screen")) {
            LazyColumn(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(), state = listState,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 74.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item("hero") {
                    MediaHero(satelliteItem,tokens,isFa,listState.firstVisibleItemScrollOffset,modifier)
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        SatDialogButton(stringResource(R.string.sat_ar),tokens,onAr,modifier=Modifier.weight(1f))
                        SatDialogButton(stringResource(R.string.sat_show_on_globe),tokens,onShow,modifier=Modifier.weight(1f))
                    }
                }
                item("next_pass") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (preview != null) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                SatText(stringResource(R.string.sat_detail_preview, SatFmt.dateTime(preview.pass.startTimeMs, isFa, tz)), tokens.inkPrimary)
                                SatDialogButton(stringResource(R.string.sat_detail_back_to_next), tokens, { selectedPass = null })
                            }
                            SatText(SatFmt.secondaryDate(preview.pass.startTimeMs, isFa, tz), tokens.inkSecondary, size = 11)
                        }
                        if (shownSpotlight != null) {
                            SatSpotlightCard(shownSpotlight, tokens, isFa, actions(shownSpotlight.pass), arcHeight = 160.dp,
                                showActions = false, nowMs = simNow, realNowMs = realNow, tz = tz)
                            var anchor by remember { mutableStateOf(IntRect.Zero) }
                            SatDialogButton(stringResource(R.string.sat_detail_schedule), tokens,
                                { pending = SatReminderTarget(shownSpotlight.pass, anchor) }, primary = true,
                                enabled = onSetReminder != null && shownSpotlight.pass.pass.startTimeMs > realNow,
                                modifier = Modifier.fillMaxWidth().onGloballyPositioned { anchor = it.anchorInWindow() })
                        } else if (predicted == null) SatPassSkeletons(tokens)
                        else SatText(stringResource(R.string.sat_detail_empty), tokens.inkSecondary)
                    }
                }
                item("telemetry") { DetailTelemetryPanel(state, tokens, isFa, futurePosition) }
                item("visibility") { DetailVisibilityPanel(state, tokens, isFa, location, simNow) }
                item("passes_header") {
                    SatSheetTitle(stringResource(R.string.sat_detail_passes, SatFmt.int(dayCount, isFa)),
                        passes?.size?.let { stringResource(R.string.sat_pass_count, SatFmt.int(it, isFa)) }, tokens)
                    val n = passes?.size ?: 0
                    if (n > 0 && showMaximum.size < groups.size) SatDialogButton(stringResource(R.string.sat_detail_expand, SatFmt.int(n, isFa)), tokens, {
                        expanded = groups.associate { it.nightKey to true }
                        showMaximum = groups.map { it.nightKey }.toSet()
                    })
                }
                item("day_strip") {
                    LazyRow(state = dayState, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        itemsIndexed(days, key = { _, day -> day }) { index, day ->
                            val groupIndex = groups.indexOfFirst { it.nightKey == day }
                            val group = groups.getOrNull(groupIndex)
                            val date = if (isFa) TimeEngine.toSolarHijri(day, tz).day else
                                Calendar.getInstance(tz).apply { timeInMillis = day }.get(Calendar.DAY_OF_MONTH)
                            DayChip(SatFmt.int(date, isFa), group?.passes?.size ?: 0, group?.best?.quality?.graphic(tokens), tokens, isFa,
                                selected = day == (visibleGroup ?: days.first()), enabled = group != null,
                                onClick = { if (groupIndex >= 0) { expanded = expanded + (day to true); scrollTo(6 + groupIndex) } })
                        }
                    }
                }
                itemsIndexed(groups, key = { _, group -> group.nightKey }) { index, g ->
                    val isOpen = expanded[g.nightKey] ?: (index < 3)
                    SatNightSection(g, isOpen, if (g.nightKey in showMaximum) Int.MAX_VALUE else 3,
                        if (g.nightKey in showMaximum) Int.MAX_VALUE else if (satelliteItem.isTrain) 12 else 10,
                        tokens, isFa, ::actions,
                        { expanded = expanded + (g.nightKey to !isOpen) }, { showMaximum = showMaximum + g.nightKey }, realNow, tz = tz)
                }
                item("dossier") { DetailDossier(satelliteItem, tokens, isFa) }
            }
            Row(Modifier.align(Alignment.TopStart).statusBarsPadding().padding(start = 16.dp, top = 16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ZigSatIconButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.sat_detail_back), tokens, onBack,
                    modifier=Modifier.layoutRole(LayoutRole.Mirrors), testTag = "detail_floating_back")
                AnimatedVisibility(showsTitle, enter = fadeIn(tween(120)), exit = fadeOut(tween(120))) {
                    SatText(shortNameOf(satelliteItem, isFa), tokens.inkPrimary, Modifier.satMaterial(tokens, SatSlot.Popover, SatShapes.capsule)
                        .padding(horizontal = 12.dp, vertical = 8.dp), strong = true)
                }
            }
        }
        pending?.let { target ->
            val pass = passUi(target.pass.pass)
            SatReminderPopover(tokens, isFa, target, pass.reminderLeadMinutes, realNow, tz,
                onChoose = { lead -> onSetReminder?.invoke(satelliteItem, pass.pass, lead); pending = null },
                onRemove = if (pass.reminderLeadMinutes == null || onRemoveReminder == null) null else {
                    { onRemoveReminder.invoke(satelliteItem, pass.pass); pending = null }
                }, onClose = { pending = null })
        }
    }
}

@Composable
private fun DetailTelemetryPanel(state:SatelliteLiveState,t:ZigSatTokens,fa:Boolean,next:Geo?) {
    val pos=state.topocentric;val geo=pos.geo()
    val heading=next?.let { MotionCues.bearing(geo,it) };var expanded by remember {mutableStateOf(false)}
    Column {
        SatSheetTitle(stringResource(R.string.sat_telemetry),null,t)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            val direction = if (heading != null && next != null) SatFmt.compass(heading,fa)+" · "+
                stringResource(if(next.lat>=geo.lat)R.string.sat_ascending else R.string.sat_descending)
                else stringResource(R.string.sat_unknown)
            TelemetryTile(stringResource(R.string.sat_heading),direction,t,Modifier.weight(1f),azimuth=heading)
            TelemetryTile(stringResource(R.string.sat_range),SatFmt.kilometers(pos.rangeKm,fa)+" "+stringResource(R.string.sat_kilometers),t,Modifier.weight(1f))
        }
        SatDialogButton(stringResource(if(expanded)R.string.sat_telemetry_reduce else R.string.sat_telemetry_more),t,{expanded=!expanded},modifier=Modifier.fillMaxWidth().testTag("detail_more_telemetry"))
        if(expanded) {
            TelemetryTile(stringResource(R.string.sat_subpoint),SatFmt.degrees(pos.subLatDeg,fa,1)+" · "+SatFmt.degrees(pos.subLonDeg,fa,1),t,Modifier.fillMaxWidth())
            TelemetryTile(stringResource(R.string.sat_azimuth),SatFmt.compass(pos.azimuthDeg,fa)+" · "+SatFmt.degrees(pos.azimuthDeg,fa),t,Modifier.fillMaxWidth())
            SatText(stringResource(if(pos.isSunlit) R.string.sat_sunlit else R.string.sat_shadow),t.inkSecondary)
        }
    }
}

@Composable
private fun TelemetryTile(label: String, value: String, tokens: ZigSatTokens, modifier: Modifier,
    azimuth: Double? = null, muted: Boolean = false) {
    Column(modifier.heightIn(min = 76.dp).satMaterial(tokens, SatSlot.PassRow, SatShapes.of(14.dp), tokens.cardAlt)
        .padding(7.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SatText(label, tokens.inkSecondary, size = 11)
        if (azimuth != null) Canvas(Modifier.layoutRole(LayoutRole.Physical).align(Alignment.CenterHorizontally).size(if (tokens.skin == SatSkin.AETHER) 36.dp else 24.dp)) {
            val r = size.minDimension * .43f
            val color = if (tokens.skin == SatSkin.AETHER) tokens.timeAccent else tokens.inkSecondary
            drawCircle(color.copy(alpha = .66f), r, style = Stroke(1.dp.toPx()))
            val ticks = if (tokens.skin == SatSkin.AETHER) 24 else 4
            for (i in 0 until ticks) {
                val angle = Math.toRadians(i * 360.0 / ticks)
                val vector = Offset(kotlin.math.sin(angle).toFloat(), -kotlin.math.cos(angle).toFloat())
                drawLine(color, center + vector * (.83f * r), center + vector * r, 1.dp.toPx())
            }
            val bearing = Math.toRadians(azimuth)
            val needle = Offset(kotlin.math.sin(bearing).toFloat(), -kotlin.math.cos(bearing).toFloat())
            drawLine(tokens.accentInk, center - needle * (.22f * r), center + needle * (.73f * r), 2.dp.toPx())
            drawCircle(color, 1.7.dp.toPx())
        }
        SatText(value, if (muted) tokens.inkTertiary else tokens.inkPrimary, size = 12, strong = true)
    }
}

@Composable
private fun DetailVisibilityPanel(
    state: SatelliteLiveState,
    tokens: com.alijafari.red.astronomy.ui.satellites.ZigSatTokens,
    isFa: Boolean,
    userLocation: UserLocation,
    tickMs: Long,
) {
    val sunAltitude = remember(tickMs, userLocation) {
        SunEngine.getSunAltitude(tickMs, userLocation.latitude, userLocation.longitude)
    }
    val twilight = remember(sunAltitude) { SunEngine.getTwilightPhase(sunAltitude) }
    val sunlit = state.topocentric.isSunlit
    val darkEnough = sunAltitude <= -6.0
    val aboveHorizon = state.topocentric.elevationDeg > 0.0
    val met = listOf(sunlit, darkEnough, aboveHorizon).count { it }

    ZigSatCard(tokens = tokens, shape = SatShapes.of(SatRadius.card)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = if (isFa) "ارزیابی رؤیت با چشم غیرمسلح" else "Naked-eye visibility assessment",
                style = RedTypographyTokens.sectionTitle.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                color = tokens.inkPrimary,
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SatText(stringResource(if (state.isNakedEyeVisible) R.string.sat_detection_yes else R.string.sat_detection_no),
                    tokens.inkPrimary, Modifier.weight(1f), size = 13, strong = true)
                Text(
                    text = "${stringResource(R.string.sat_mag_short)} ${SatFmt.magnitude(state.apparentMagnitude, isFa)}",
                    style = RedTypographyTokens.sectionHeading.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold).tabular(),
                    color = tokens.inkPrimary,
                )
            }
            Text(
                    text = if (isFa) {
                        "${SatFmt.int(met, true)} از ۳ شرط"
                    } else {
                        "$met of 3 conditions met"
                    },
                    style = RedTypographyTokens.badge.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                    color = tokens.inkSecondary,
                    modifier = Modifier.background(tokens.cardAlt, SatShapes.capsule).padding(horizontal = 8.dp, vertical = 5.dp),
                )
            CheckRow(
                passed = sunlit,
                text = if (isFa) "در نور خورشید" else "Satellite is sunlit",
                tokens = tokens,
            )
            CheckRow(
                passed = darkEnough,
                text = if (isFa) {
                    "آسمان به اندازه کافی تاریک (${twilight.nameFa})"
                } else {
                    "Sky dark enough (${twilight.nameEn})"
                },
                tokens = tokens,
            )
            CheckRow(
                passed = aboveHorizon,
                text = if (isFa) "بالای افق ناظر" else "Above the observer's horizon",
                tokens = tokens,
            )
            // The engine's observing threshold is 10°, not the geometric horizon. Its legacy
            // reason says "below horizon" even for 0–10°; avoid repeating that incorrect wording.
            val reason = if (state.topocentric.elevationDeg >= 0.0 && state.topocentric.elevationDeg < 10.0)
                stringResource(R.string.sat_low_elevation_reason, SatFmt.degrees(state.topocentric.elevationDeg, isFa, 1))
                else SatFmt.digits(if (isFa) state.reasonFa else state.reasonEn, isFa)
            Text(text = reason, style = RedTypographyTokens.bodySecondary, color = tokens.inkSecondary)
        }
    }
}

@Composable
private fun CheckRow(passed: Boolean, text: String, tokens: com.alijafari.red.astronomy.ui.satellites.ZigSatTokens) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (passed) tokens.nakedEye.copy(alpha = 0.18f) else tokens.cardAlt)
                .border(1.dp, if (passed) tokens.nakedEye.copy(alpha = 0.5f) else tokens.border, SatShapes.capsule),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (passed) Icons.Filled.Check else Icons.Filled.Close,
                contentDescription = null,
                tint = if (passed) tokens.nakedEye else tokens.telescopic,
                modifier = Modifier.size(12.dp),
            )
        }
        Text(
            text = text,
            style = RedTypographyTokens.badge.copy(fontSize = 12.sp),
            color = if (passed) tokens.inkPrimary else tokens.inkTertiary,
        )
    }
}

@Composable
private fun DayChip(
    title: String,
    count: Int,
    dotColor: Color?,
    tokens: com.alijafari.red.astronomy.ui.satellites.ZigSatTokens,
    isFa: Boolean,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(56.dp)
            .heightIn(min = 64.dp)
            .clip(SatShapes.of(SatRadius.tile))
            .background(tokens.cardAlt)
            .border(1.dp, if (selected) tokens.accentInk else tokens.border, SatShapes.of(SatRadius.tile))
            .clickable(enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = RedTypographyTokens.badge.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold).tabular(),
            color = tokens.inkPrimary,
        )
        Text(
            text = SatFmt.int(count, isFa),
            style = RedTypographyTokens.badge.copy(fontSize = 10.sp).tabular(),
            color = tokens.inkTertiary,
        )
        if (dotColor != null) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailDossier(
    satelliteItem: SatelliteItem,
    tokens: com.alijafari.red.astronomy.ui.satellites.ZigSatTokens,
    isFa: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }
    var significanceExpanded by remember { mutableStateOf(false) }

    ZigSatCard(tokens = tokens, shape = SatShapes.of(SatRadius.card)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .sizeIn(minWidth = SatGrid.minTouchTarget, minHeight = SatGrid.minTouchTarget)
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.sat_dossier),
                    style = RedTypographyTokens.sectionTitle.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                    color = tokens.inkPrimary,
                    modifier = Modifier.weight(1f),
                )
                Glyph(if(expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,LayoutRole.Neutral,tokens.inkSecondary)
            }

            if (expanded) {
                val unknown = stringResource(R.string.sat_unknown)
                val operator = (if (isFa) satelliteItem.operatorFa else satelliteItem.operatorEn).ifBlank { unknown }
                val launch = SatFmt.launchDate(satelliteItem.launchDate, isFa)?.let {
                    if (isFa && satelliteItem.id == "tiangong") it + " (" + stringResource(R.string.sat_tianhe_module) + ")" else it
                } ?: unknown
                val clipboard = LocalClipboardManager.current
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DossierCell(if (isFa) "اپراتور" else "Operator", SatFmt.digits(operator, isFa), tokens, Modifier.weight(1f), isFa)
                    DossierCell(if (isFa) "تاریخ پرتاب" else "Launch date", SatFmt.digits(launch, isFa), tokens, Modifier.weight(1f), isFa)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DossierCell(stringResource(R.string.sat_norad), isolate(satelliteItem.noradId.toString()), tokens, Modifier.weight(1f), isFa) {
                        clipboard.setText(AnnotatedString(satelliteItem.noradId.toString()))
                    }
                    DossierCell(if (isFa) "کد بین‌المللی" else "Int’l designation", isolate(satelliteItem.designation),
                        tokens, Modifier.weight(1f), isFa) { clipboard.setText(AnnotatedString(satelliteItem.designation)) }
                }

                val overview=if(isFa)satelliteItem.descriptionFa else satelliteItem.descriptionEn
                if(overview.isNotBlank()) SatText(SatFmt.digits(overview,isFa),tokens.inkSecondary)
                val purpose = if (isFa) satelliteItem.missionPurposeFa else satelliteItem.missionPurposeEn
                if (purpose.isNotBlank()) {
                    ZigMicroCaps(text = if (isFa) "هدف ماموریت" else "Mission purpose", tokens = tokens, isFa = isFa)
                    Text(text = SatFmt.digits(purpose, isFa), style = RedTypographyTokens.bodySecondary, color = tokens.inkSecondary)
                }

                val significance = if (isFa) satelliteItem.scientificSignificanceFa else satelliteItem.scientificSignificanceEn
                if (significance.isNotBlank()) {
                    ZigMicroCaps(text = if (isFa) "اهمیت علمی و تاریخی" else "Scientific & historical significance", tokens = tokens, isFa = isFa)
                    Text(
                        text = SatFmt.digits(significance, isFa),
                        style = RedTypographyTokens.bodySecondary,
                        color = tokens.inkSecondary,
                        maxLines = if (significanceExpanded) Int.MAX_VALUE else 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (significance.length > 120) {
                        Text(
                            text = if (significanceExpanded) {
                                if (isFa) "کمتر" else "Read less"
                            } else {
                                if (isFa) "بیشتر" else "Read more"
                            },
                            style = RedTypographyTokens.badge.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                            color = tokens.accentInk,
                            modifier = Modifier
                                .sizeIn(minWidth = SatGrid.minTouchTarget, minHeight = SatGrid.minTouchTarget)
                                .clickable { significanceExpanded = !significanceExpanded }
                                .padding(top = 12.dp),
                        )
                    }
                }

                val facts = if (isFa) satelliteItem.verifiedFactsFa else satelliteItem.verifiedFactsEn
                if (facts.isNotEmpty()) {
                    ZigMicroCaps(text = if (isFa) "حقایق تاییدشده" else "Verified key facts", tokens = tokens, isFa = isFa)
                    facts.take(3).forEach { fact ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(tokens.live),
                            )
                            Text(
                                text = localizedProse(fact, isFa),
                                style = RedTypographyTokens.bodySecondary,
                                color = tokens.inkSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DossierCell(label: String, value: String, tokens: ZigSatTokens, modifier: Modifier, isFa: Boolean, copy: (() -> Unit)? = null) {
    Column(modifier.heightIn(min = 48.dp).satMaterial(tokens, SatSlot.PassRow, SatShapes.of(14.dp))
        .then(if (copy == null) Modifier else Modifier.combinedClickable(onClick = {}, onLongClick = copy)).padding(8.dp)) {
        ZigMicroCaps(label, tokens, isFa = isFa)
        SatText(value, tokens.inkPrimary, size = 13)
    }
}

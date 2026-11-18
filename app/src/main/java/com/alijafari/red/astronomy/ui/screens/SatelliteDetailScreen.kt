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
import com.alijafari.red.astronomy.data.catalog.CanonicalAstroCatalog
import com.alijafari.red.astronomy.data.repository.TleRepository
import com.alijafari.red.astronomy.domain.AppLanguage
import com.alijafari.red.astronomy.domain.ThemeMode
import com.alijafari.red.astronomy.domain.UserLocation
import com.alijafari.red.astronomy.notification.ScheduledNotificationItem
import com.alijafari.red.astronomy.ui.satellites.*
import com.alijafari.red.astronomy.ui.theme.RedShapes
import com.alijafari.red.astronomy.ui.theme.RedTypographyTokens
import com.alijafari.red.astronomy.ui.theme.squircleShape
import com.alijafari.red.astronomy.util.formatTleStalenessBadge
import com.alijafari.red.astronomy.util.rememberIsNetworkOnline
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs
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
    val isOnline by rememberIsNetworkOnline(context)
    val tleRepo = remember { TleRepository.getInstance(context) }
    val starlinkCacheVersion by tleRepo.starlinkVersion.collectAsState()
    val isFa = language == AppLanguage.PERSIAN
    val canonicalSatellite = remember(satelliteItem) {
        CanonicalAstroCatalog.getCanonicalObject("sat_${satelliteItem.noradId}")
            ?: CanonicalAstroCatalog.getCanonicalObject(satelliteItem.id)
    }
    val state = remember(satelliteItem, simulationTimestampMs, userLocation, starlinkCacheVersion) {
        (AstroDispatchEngine.calculateState(canonicalSatellite?.canonicalId ?: "sat_${satelliteItem.noradId}",
            simulationTimestampMs, userLocation.latitude, userLocation.longitude)?.specializedData as? SatelliteLiveState)
            ?: SatelliteEngine.calculateSatelliteState(satelliteItem, simulationTimestampMs, userLocation.latitude, userLocation.longitude)
    }
    val stale = formatTleStalenessBadge(satelliteItem.isTrain, state.tleMetadata,
        if (satelliteItem.isTrain || satelliteItem.category == SatelliteCategory.STARLINK) tleRepo.getStarlinkCacheAgeDays() else null, isFa)
    var predicted by remember(satelliteItem.id, userLocation) { mutableStateOf<List<ISSEngine.ISSPass>?>(null) }
    val days = if (satelliteItem.isTrain) 2 else 7
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
    SatelliteDetailContent(satelliteItem, state, userLocation, isFa, skin, themeMode, isOnline, stale, upcoming,
        preselectedPass, reminderAlarms, start, System.currentTimeMillis(), isSimulated, onBack, onSetReminder,
        onRemoveReminder, { onShowOnGlobe?.invoke(satelliteItem) }, { onSpotInAr?.invoke(satelliteItem) }, modifier)
}

/** Presentation-only body also used by the deterministic preview matrix. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SatelliteDetailContent(
    satelliteItem: SatelliteItem, state: SatelliteLiveState, location: UserLocation, isFa: Boolean, skin: SatSkin,
    themeMode: ThemeMode, isOnline: Boolean, stale: String?, predicted: List<ISSEngine.ISSPass>?, preselected: ISSEngine.ISSPass?,
    alarms: List<ScheduledNotificationItem>, simNow: Long, realNow: Long, isSimulated: Boolean, onBack: () -> Unit,
    onSetReminder: ((SatelliteItem, ISSEngine.ISSPass, Int) -> Unit)?, onRemoveReminder: ((SatelliteItem, ISSEngine.ISSPass) -> Unit)?,
    onShow: () -> Unit, onAr: () -> Unit, modifier: Modifier = Modifier,
) {
    val tokens = remember(skin, themeMode) { zigSatTokens(skin, themeMode) }
    val tz = remember(location.timezoneId) { TimeEngine.resolveTimeZone(location.timezoneId) }
    val reduce = LocalSatReduceMotion.current
    fun passUi(pass: ISSEngine.ISSPass): SatPassUi {
        val matching = alarms.filter { (it.objectId == satelliteItem.id || satelliteItem.isTrain && it.objectId == "starlink_train") &&
            abs(it.eventTimeMs - pass.startTimeMs) <= 30_000L }
        return SatPassUi(passKey(satelliteItem, pass), satelliteItem, shortNameOf(satelliteItem, isFa), pass,
            SatNights.nightKey(pass.startTimeMs, tz), matching.firstOrNull { it.manualPass }?.leadMinutes, matching.any { !it.manualPass })
    }
    val passes = remember(predicted, alarms, tz, isFa) { predicted?.map(::passUi) }
    val groups = remember(passes, simNow / 60_000, tz, isFa) { SatNights.group(passes.orEmpty(), simNow, tz, isFa, includeSolarHijri = true) }
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
    val dayCount = if (satelliteItem.isTrain) 2 else 7
    val firstNight = SatNights.nightKey(simNow, tz)
    val days = remember(firstNight, tz, dayCount) {
        val date = Calendar.getInstance(tz).apply { timeInMillis = firstNight }
        List(dayCount) { date.timeInMillis.also { date.add(Calendar.DATE, 1) } }
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
        Box(modifier.fillMaxSize().background(tokens.canvas).testTag("satellite_detail_screen")) {
            LazyColumn(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(), state = listState,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 74.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item("hero") {
                    val next = spotlight?.pass
                    DetailHero(satelliteItem, state, isFa, isOnline, stale, onAr, onShow,
                        { next?.let { pending = SatReminderTarget(it, heroBell) } },
                        next?.pass?.takeIf { it.startTimeMs > simNow }?.let {
                            val dir = SatFmt.compass(it.startAzimuthDeg, isFa)
                            stringResource(R.string.sat_detail_rises, dir, SatFmt.countdown(it.startTimeMs - simNow, isFa))
                        }, Modifier.onGloballyPositioned { heroBell = it.anchorInWindow() },
                        onSetReminder != null && next != null && next.pass.startTimeMs > realNow)
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
                item("telemetry") { DetailTelemetryPanel(state, tokens, isFa, stale, isOnline) }
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
                            DayChip(SatFmt.int(date, isFa), group?.passes?.size ?: 0, group?.best?.quality?.hue, tokens, isFa,
                                selected = day == (visibleGroup ?: days.first()), enabled = group != null,
                                onClick = { if (groupIndex >= 0) { expanded = expanded + (day to true); scrollTo(6 + groupIndex) } })
                        }
                    }
                }
                itemsIndexed(groups, key = { _, group -> group.nightKey }) { index, g ->
                    val isOpen = expanded[g.nightKey] ?: (index < 3)
                    SatNightSection(g, isOpen, if (g.nightKey in showMaximum) Int.MAX_VALUE else 3,
                        if (satelliteItem.isTrain) 12 else 10, tokens, isFa, ::actions,
                        { expanded = expanded + (g.nightKey to !isOpen) }, { showMaximum = showMaximum + g.nightKey }, realNow, tz = tz)
                }
                item("dossier") { DetailDossier(satelliteItem, tokens, isFa) }
            }
            Row(Modifier.align(Alignment.TopStart).statusBarsPadding().padding(start = 16.dp, top = 16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ZigSatIconButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.sat_detail_back), tokens, onBack,
                    testTag = "detail_floating_back")
                AnimatedVisibility(showsTitle, enter = fadeIn(tween(120)), exit = fadeOut(tween(120))) {
                    SatText(shortNameOf(satelliteItem, isFa), tokens.inkPrimary, Modifier.satMaterial(tokens, SatSlot.Popover, CircleShape)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailHero(
    satelliteItem: SatelliteItem,
    state: SatelliteLiveState,
    isFa: Boolean,
    isOnline: Boolean,
    staleBadgeText: String?,
    onAr: () -> Unit,
    onShowOnGlobe: () -> Unit,
    onRemind: () -> Unit,
    riseSubtitle: String?,
    reminderModifier: Modifier,
    reminderEnabled: Boolean,
) {
    val tokens = LocalZigSatTokens.current
    var overviewExpanded by remember { mutableStateOf(false) }
    ZigSatCard(tokens = tokens, shape = squircleShape(SatRadius.card)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (!isOnline || staleBadgeText != null) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!isOnline) {
                        Row(
                            modifier = Modifier
                                .clip(RedShapes.capsule)
                                .background(tokens.accentInk.copy(alpha = 0.14f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CloudOff,
                                contentDescription = if (isFa) "آفلاین" else "Offline",
                                tint = tokens.accentInk,
                                modifier = Modifier.size(12.dp),
                            )
                            Text(
                                text = if (isFa) "آفلاین" else "Offline",
                                style = RedTypographyTokens.badge.copy(fontSize = 11.sp),
                                color = tokens.accentInk,
                            )
                        }
                    }
                    if (staleBadgeText != null) {
                        Text(
                            text = staleBadgeText,
                            style = RedTypographyTokens.badge.copy(fontSize = 11.sp),
                            color = tokens.sim,
                            modifier = Modifier
                                .clip(RedShapes.capsule)
                                .background(tokens.sim.copy(alpha = 0.14f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(tokens.live.copy(alpha = 0.16f))
                        .border(1.dp, tokens.live.copy(alpha = 0.45f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.SatelliteAlt,
                        contentDescription = null,
                        tint = tokens.accentInk,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = SatFmt.categoryLabel(satelliteItem.category, isFa),
                        style = RedTypographyTokens.badge.copy(fontSize = 11.sp),
                        color = tokens.accentInk,
                    )
                    Text(
                        text = fullNameOf(satelliteItem, isFa),
                        style = RedTypographyTokens.screenTitle.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = tokens.inkPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .clip(RedShapes.capsule)
                    .background(
                        if (state.isNakedEyeVisible) tokens.nakedEye.copy(alpha = 0.16f) else tokens.cardAlt
                    )
                    .border(
                        1.dp,
                        if (state.isNakedEyeVisible) tokens.nakedEye.copy(alpha = 0.5f) else tokens.border,
                        RedShapes.capsule,
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (state.isNakedEyeVisible) tokens.nakedEye else tokens.telescopic),
                )
                Text(
                    text = if (state.isNakedEyeVisible) {
                        if (isFa) "قابل مشاهده با چشم" else "Naked eye visible"
                    } else {
                        if (isFa) "قابل مشاهده نیست" else "Not visible"
                    },
                    style = RedTypographyTokens.badge.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                    color = tokens.inkPrimary,
                )
            }

            val overview = SatFmt.digits(if (isFa) satelliteItem.descriptionFa else satelliteItem.descriptionEn, isFa)
            if (overview.isNotBlank()) {
                Text(
                    text = overview,
                    style = RedTypographyTokens.bodySecondary,
                    color = tokens.inkSecondary,
                    maxLines = if (overviewExpanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (overview.length > 140) {
                    Text(
                        text = if (overviewExpanded) {
                            if (isFa) "کمتر" else "Less"
                        } else {
                            if (isFa) "بیشتر" else "More"
                        },
                        style = RedTypographyTokens.badge.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                        color = tokens.accentInk,
                        modifier = Modifier
                            .height(SatGrid.minTouchTarget)
                            .clickable { overviewExpanded = !overviewExpanded }
                            .padding(top = 12.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(SatGrid.minTouchTarget)
                        .clip(RedShapes.capsule)
                        .background(tokens.live.copy(alpha = 0.16f))
                        .border(1.dp, tokens.live.copy(alpha = 0.55f), RedShapes.capsule)
                        .clickable(onClick = onAr)
                        .testTag("detail_spot_in_ar"),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Visibility,
                            contentDescription = null,
                            tint = tokens.accentInk,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = if (isFa) "در آسمان AR" else "Spot in AR sky",
                            style = RedTypographyTokens.badge.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                            color = tokens.accentInk,
                            maxLines = 1,
                        )
                    }
                }
                ZigSatIconButton(
                    icon = Icons.Filled.CenterFocusWeak,
                    contentDescription = if (isFa) "نمایش روی کره" else "Show on globe",
                    tokens = tokens,
                    onClick = onShowOnGlobe,
                    testTag = "detail_show_on_globe",
                )
                ZigSatIconButton(
                    icon = Icons.Filled.NotificationsActive,
                    contentDescription = if (isFa) "یادآوری گذر بعدی" else "Remind next pass",
                    tokens = tokens,
                    onClick = onRemind,
                    testTag = "detail_remind_next", modifier = reminderModifier, enabled = reminderEnabled,
                )
            }
            if (state.topocentric.elevationDeg <= 0.0 && riseSubtitle != null) {
                SatText(riseSubtitle, tokens.inkSecondary, size = 11)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailTelemetryPanel(
    state: SatelliteLiveState,
    tokens: com.alijafari.red.astronomy.ui.satellites.ZigSatTokens,
    isFa: Boolean,
    staleBadgeText: String?,
    isOnline: Boolean,
) {
    val topo = state.topocentric
    ZigSatCard(tokens = tokens, shape = squircleShape(SatRadius.card)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = if (isFa) "تله‌متری مداری زنده" else "Live orbital telemetry",
                style = RedTypographyTokens.sectionTitle.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                color = tokens.inkPrimary,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TelemetryHeroTile(
                    label = if (isFa) "ارتفاع" else "Altitude",
                    value = SatFmt.kilometers(topo.satAltKm, isFa),
                    unit = if (isFa) "کیلومتر" else "km",
                    tokens = tokens,
                    modifier = Modifier.weight(1f),
                )
                TelemetryHeroTile(
                    label = if (isFa) "سرعت" else "Velocity",
                    value = SatFmt.speed(topo.velocityKmS, isFa),
                    unit = if (isFa) "کیلومتر/ثانیه" else "km/s",
                    sub = SatFmt.kilometers(topo.velocityKmS * 3600.0, isFa) + if (isFa) " کیلومتر/ساعت" else " km/h",
                    tokens = tokens,
                    modifier = Modifier.weight(1f),
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TelemetryTile(
                    label = if (isFa) "سمت" else "Azimuth",
                    value = SatFmt.degrees(topo.azimuthDeg, isFa) + " " + SatFmt.compass(topo.azimuthDeg, isFa),
                    tokens = tokens,
                )
                TelemetryTile(
                    label = if (isFa) "زاویه ارتفاع" else "Elevation",
                    value = SatFmt.signed(topo.elevationDeg, 0, "°", isFa),
                    tokens = tokens,
                    muted = topo.elevationDeg <= 0.0,
                )
                TelemetryTile(
                    label = if (isFa) "فاصله" else "Range",
                    value = SatFmt.kilometers(topo.rangeKm, isFa) + " km",
                    tokens = tokens,
                )
                TelemetryTile(
                    label = if (isFa) "نقطه زیرین" else "Sub-point",
                    value = SatFmt.signed(topo.subLatDeg, 1, "°", isFa) + ", " + SatFmt.signed(topo.subLonDeg, 1, "°", isFa),
                    tokens = tokens,
                )
                TelemetryTile(
                    label = if (isFa) "روشنایی" else "Illumination",
                    value = if (topo.isSunlit) (if (isFa) "☀ آفتاب" else "☀ Sunlit") else (if (isFa) "☾ سایه" else "☾ Umbra"),
                    tokens = tokens,
                )
                val ageDays = state.tleMetadata?.epochAgeDays
                TelemetryTile(
                    label = "TLE",
                    value = when {
                        ageDays == null -> if (isFa) "نامشخص" else "n/a"
                        else -> SatFmt.digits(String.format(java.util.Locale.US, "%.1f", ageDays), isFa) +
                            (if (isFa) " روز" else "d ago")
                    } + if (!isOnline) (if (isFa) " · کَش" else " · cached") else "",
                    tokens = tokens,
                    muted = staleBadgeText != null,
                )
            }
        }
    }
}

@Composable
private fun TelemetryHeroTile(
    label: String,
    value: String,
    unit: String,
    tokens: com.alijafari.red.astronomy.ui.satellites.ZigSatTokens,
    modifier: Modifier = Modifier,
    sub: String? = null,
) {
    Column(
        modifier = modifier
            .clip(squircleShape(SatRadius.tile))
            .background(tokens.cardAlt)
            .border(1.dp, tokens.border, squircleShape(SatRadius.tile))
            .padding(12.dp),
    ) {
        ZigMicroCaps(text = label, tokens = tokens, isFa = false)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = value,
                style = RedTypographyTokens.sectionHeading.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold).tabular(),
                color = tokens.inkPrimary,
                maxLines = 1,
            )
            Text(
                text = unit,
                style = RedTypographyTokens.badge.copy(fontSize = 11.sp),
                color = tokens.inkTertiary,
                maxLines = 1,
            )
        }
        if (sub != null) {
            Text(
                text = sub,
                style = RedTypographyTokens.badge.copy(fontSize = 10.sp).tabular(),
                color = tokens.inkTertiary,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun TelemetryTile(
    label: String,
    value: String,
    tokens: com.alijafari.red.astronomy.ui.satellites.ZigSatTokens,
    modifier: Modifier = Modifier,
    muted: Boolean = false,
) {
    Column(
        modifier = modifier
            .width(104.dp)
            .height(76.dp)
            .clip(squircleShape(SatRadius.tile))
            .background(tokens.cardAlt)
            .border(1.dp, tokens.border, squircleShape(SatRadius.tile))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        ZigMicroCaps(text = label, tokens = tokens, isFa = false)
        Text(
            text = value,
            style = RedTypographyTokens.badge.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold).tabular(),
            color = if (muted) tokens.inkTertiary else tokens.inkPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
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

    ZigSatCard(tokens = tokens, shape = squircleShape(SatRadius.card)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = if (isFa) "ارزیابی رؤیت با چشم غیرمسلح" else "Naked-eye visibility assessment",
                style = RedTypographyTokens.sectionTitle.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                color = tokens.inkPrimary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${stringResource(R.string.sat_mag_short)} ${SatFmt.magnitude(state.apparentMagnitude, isFa)}",
                    style = RedTypographyTokens.sectionHeading.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold).tabular(),
                    color = tokens.inkPrimary,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (isFa) {
                        "${SatFmt.int(met, true)} از ۳ شرط"
                    } else {
                        "$met of 3 conditions met"
                    },
                    style = RedTypographyTokens.badge.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                    color = tokens.inkSecondary,
                )
            }
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
            Text(
                text = SatFmt.digits(if (isFa) state.reasonFa else state.reasonEn, isFa),
                style = RedTypographyTokens.bodySecondary,
                color = tokens.inkSecondary,
            )
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
                .border(1.dp, if (passed) tokens.nakedEye.copy(alpha = 0.5f) else tokens.border, CircleShape),
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
            .clip(squircleShape(SatRadius.tile))
            .background(tokens.cardAlt)
            .border(1.dp, if (selected) tokens.accentInk else tokens.border, squircleShape(SatRadius.tile))
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

    ZigSatCard(tokens = tokens, shape = squircleShape(SatRadius.card)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SatGrid.minTouchTarget)
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (isFa) "شناسنامه ماموریت" else "Mission dossier",
                    style = RedTypographyTokens.sectionTitle.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                    color = tokens.inkPrimary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (expanded) "\u25B4" else "\u25BE",
                    style = RedTypographyTokens.badge.copy(fontSize = 12.sp),
                    color = tokens.inkTertiary,
                )
            }

            if (expanded) {
                val unknown = stringResource(R.string.sat_unknown)
                val operator = (if (isFa) satelliteItem.operatorFa else satelliteItem.operatorEn).ifBlank { unknown }
                val launch = satelliteItem.launchDate.ifBlank { unknown }
                val clipboard = LocalClipboardManager.current
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DossierCell(if (isFa) "اپراتور" else "Operator", SatFmt.digits(operator, isFa), tokens, Modifier.weight(1f), isFa)
                    DossierCell(if (isFa) "تاریخ پرتاب" else "Launch date", SatFmt.digits(launch, isFa), tokens, Modifier.weight(1f), isFa)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DossierCell("NORAD ID", SatFmt.int(satelliteItem.noradId, isFa), tokens, Modifier.weight(1f), isFa) {
                        clipboard.setText(AnnotatedString(satelliteItem.noradId.toString()))
                    }
                    DossierCell(if (isFa) "کد بین‌المللی" else "Int’l designation", SatFmt.digits(satelliteItem.designation, isFa),
                        tokens, Modifier.weight(1f), isFa) { clipboard.setText(AnnotatedString(satelliteItem.designation)) }
                }

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
                                .height(SatGrid.minTouchTarget)
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
                                text = SatFmt.digits(fact, isFa),
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
    Column(modifier.heightIn(min = 48.dp).satMaterial(tokens, SatSlot.PassCard, squircleShape(14.dp))
        .then(if (copy == null) Modifier else Modifier.combinedClickable(onClick = {}, onLongClick = copy)).padding(8.dp)) {
        ZigMicroCaps(label, tokens, isFa = isFa)
        SatText(value, tokens.inkPrimary, size = 13)
    }
}

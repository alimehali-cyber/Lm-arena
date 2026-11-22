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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
    onShowOnGlobe: ((SatelliteItem, ISSEngine.ISSPass?) -> Unit)? = null,
    onSpotInAr: ((SatelliteItem, ISSEngine.ISSPass?) -> Unit)? = null,
    themeMode: ThemeMode = ThemeMode.DARK, modifier: Modifier = Modifier, isActive: Boolean = true,
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
            simulationTimestampMs, userLocation.latitude, userLocation.longitude, userLocation.elevationMeters)?.specializedData as? SatelliteLiveState)
            ?: SatelliteEngine.calculateSatelliteState(satelliteItem, simulationTimestampMs, userLocation.latitude,
                userLocation.longitude, userLocation.elevationMeters)
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
        onRemoveReminder, { pass -> onShowOnGlobe?.invoke(satelliteItem, pass) }, { pass -> onSpotInAr?.invoke(satelliteItem, pass) }, modifier, isActive)
}

/** Presentation-only body also used by the deterministic preview matrix. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SatelliteDetailContent(
    satelliteItem: SatelliteItem, state: SatelliteLiveState, location: UserLocation, isFa: Boolean, skin: SatSkin,
    themeMode: ThemeMode, isOnline: Boolean, stale: String?, predicted: List<ISSEngine.ISSPass>?, preselected: ISSEngine.ISSPass?,
    alarms: List<ScheduledNotificationItem>, simNow: Long, realNow: Long, isSimulated: Boolean, onBack: () -> Unit,
    onSetReminder: ((SatelliteItem, ISSEngine.ISSPass, Int) -> Unit)?, onRemoveReminder: ((SatelliteItem, ISSEngine.ISSPass) -> Unit)?,
    onShow: (ISSEngine.ISSPass?) -> Unit, onAr: (ISSEngine.ISSPass?) -> Unit, modifier: Modifier = Modifier,
    isActive: Boolean = true,
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
    var expanded by rememberSaveable(stateSaver = SatNightOverridesSaver) { mutableStateOf(emptyMap<Long, Boolean>()) }
    var showMaximum by rememberSaveable { mutableStateOf(emptyList<Long>()) }
    var pending by remember { mutableStateOf<SatReminderTarget?>(null) }
    var heroBell by remember { mutableStateOf(IntRect.Zero) }
    if (!LocalInspectionMode.current) BackHandler(enabled = isActive && pending == null, onBack = onBack)
    val dayCount = if (satelliteItem.isTrain) 2 else 7
    val days = remember(firstNight, nights, dayCount, groups) {
        (List(dayCount) { nights.plusDays(firstNight, it) } + groups.map { it.nightKey }).distinct().sorted()
    }
    LaunchedEffect(visibleGroup) {
        val day = days.indexOf(visibleGroup)
        if (day >= 0) dayState.scrollToItem((day - 1).coerceAtLeast(0))
    }
    fun scrollTo(index: Int) { scope.launch { if (reduce) listState.scrollToItem(index) else listState.animateScrollToItem(index) } }
    fun actions(pass: SatPassUi) = SatPassActions(
        onOpenDetail = { selectedPass = pass.pass; scrollTo(1) }, onShowOnGlobe = { onShow(pass.pass) }, onAr = { onAr(pass.pass) },
        onRemind = { pending = SatReminderTarget(pass, it) }, isReminderEnabled = onSetReminder != null,
    )
    CompositionLocalProvider(LocalZigSatTokens provides tokens, LocalSatSkin provides skin) {
        Box(modifier.fillMaxSize().background(tokens.canvas).testTag("satellite_detail_screen")) {
            LazyColumn(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(), state = listState,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 74.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item("hero") {
                    val next = passes?.firstOrNull { it.pass.startTimeMs > simNow } ?: spotlight?.pass
                    if (isSimulated) SatText(stringResource(R.string.sat_preview_at, SatFmt.dateTime(simNow, isFa, tz)), tokens.inkPrimary, strong = true)
                    DetailHero(satelliteItem, state, isFa, isOnline, stale, { onAr(selectedPass) }, { onShow(selectedPass) },
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
                            SatText(stringResource(R.string.sat_ar_live_explanation), tokens.inkSecondary)
                            SatDialogButton(stringResource(R.string.sat_orbit_at_pass), tokens, { onShow(preview.pass) }, modifier = Modifier.fillMaxWidth())
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
                        showMaximum = groups.map { it.nightKey }
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
                        { expanded = expanded + (g.nightKey to !isOpen) }, { showMaximum = (showMaximum + g.nightKey).distinct() }, realNow, tz = tz)
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
    var hasMore by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().satMaterial(tokens, SatSlot.DetailHero, squircleShape(SatRadius.card),
        accent = if (tokens.skin == SatSkin.AETHER) tokens.timeAccent else null)) {
        SatellitePhotoPanel(satelliteItem, tokens, isFa)
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
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
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(squircleShape(16.dp))
                        .background(tokens.live.copy(alpha = 0.12f))
                        .border(1.dp, tokens.live.copy(alpha = 0.35f), squircleShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    SatelliteSilhouette(satelliteItem, tokens.accentInk, Modifier.size(38.dp))
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
                            fontSize = 24.sp, lineHeight = 30.sp,
                            fontWeight = FontWeight.Bold,
                        ).satOptical(),
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
                        if (isFa) "با چشم دیده می‌شود" else "Naked eye visible"
                    } else {
                        if (isFa) "با چشم دیده نمی‌شود" else "Not visible"
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
                    onTextLayout = { if (!overviewExpanded) hasMore = it.hasVisualOverflow },
                )
                if (hasMore || overviewExpanded) {
                    Text(
                        text = if (overviewExpanded) {
                            if (isFa) "کمتر" else "Less"
                        } else {
                            if (isFa) "بیشتر" else "More"
                        },
                        style = RedTypographyTokens.badge.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                        color = tokens.accentInk,
                        modifier = Modifier
                            .sizeIn(minWidth = SatGrid.minTouchTarget, minHeight = SatGrid.minTouchTarget)
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
                        .sizeIn(minWidth = SatGrid.minTouchTarget, minHeight = SatGrid.minTouchTarget)
                        .clip(RedShapes.capsule)
                        .background(tokens.selectedChip)
                        .clickable(onClick = onAr)
                        .testTag("detail_spot_in_ar").padding(horizontal = 9.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Visibility,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = stringResource(R.string.sat_detail_ar),
                            style = RedTypographyTokens.badge.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                            color = Color.White,
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

@Composable
private fun DetailTelemetryPanel(state: SatelliteLiveState, tokens: ZigSatTokens, isFa: Boolean, staleBadgeText: String?, isOnline: Boolean) {
    var expanded by remember { mutableStateOf(false) }
    val topo = state.topocentric
    ZigSatCard(tokens = tokens, shape = squircleShape(SatRadius.card)) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            SatText(stringResource(R.string.sat_telemetry), tokens.inkPrimary, size = 18, strong = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TelemetryHeroTile(stringResource(R.string.sat_altitude), SatFmt.kilometers(topo.satAltKm, isFa),
                    stringResource(R.string.sat_kilometers), tokens, Modifier.weight(1f))
                TelemetryHeroTile(stringResource(R.string.sat_velocity), SatFmt.speed(topo.velocityKmS, isFa),
                    stringResource(R.string.sat_speed_unit), tokens, Modifier.weight(1f),
                    SatFmt.kilometers(topo.velocityKmS * 3600.0, isFa) + " " + stringResource(R.string.sat_speed_hour))
            }
            SatDialogButton(stringResource(if (expanded) R.string.sat_telemetry_reduce else R.string.sat_telemetry_more),
                tokens, { expanded = !expanded }, modifier = Modifier.fillMaxWidth().testTag("detail_more_telemetry"))
            AnimatedVisibility(expanded, enter = fadeIn(tween(120)), exit = fadeOut(tween(120))) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        TelemetryTile(stringResource(R.string.sat_azimuth),
                            SatFmt.degrees(topo.azimuthDeg, isFa) + " · " + SatFmt.compass(topo.azimuthDeg, isFa),
                            tokens, Modifier.weight(1f), azimuth = topo.azimuthDeg)
                        TelemetryTile(stringResource(R.string.sat_elevation), SatFmt.signed(topo.elevationDeg, 0, "°", isFa),
                            tokens, Modifier.weight(1f), muted = topo.elevationDeg <= 0.0)
                        TelemetryTile(stringResource(R.string.sat_range), SatFmt.kilometers(topo.rangeKm, isFa) + " " +
                            stringResource(R.string.sat_kilometers), tokens, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        fun coordinate(deg: Double, positive: String, negative: String): String =
                            SatFmt.degrees(deg, isFa, 1) + " " + if (deg >= 0) positive else negative
                        val lat = coordinate(topo.subLatDeg, SatFmt.compass(0.0, isFa), SatFmt.compass(180.0, isFa))
                        val lon = coordinate(topo.subLonDeg, SatFmt.compass(90.0, isFa), SatFmt.compass(270.0, isFa))
                        TelemetryTile(stringResource(R.string.sat_subpoint), "$lat\n$lon", tokens, Modifier.weight(1f))
                        TelemetryTile(stringResource(R.string.sat_illumination),
                            (if (topo.isSunlit) "☀ " else "☾ ") + stringResource(if (topo.isSunlit) R.string.sat_sunlit else R.string.sat_shadow),
                            tokens, Modifier.weight(1f))
                        val source = stringResource(when (state.tleMetadata?.source) {
                            TleDataSource.LIVE_NETWORK -> R.string.sat_source_live
                            TleDataSource.PERSISTED_CACHE, TleDataSource.MEMORY_CACHE -> R.string.sat_source_cache
                            else -> R.string.sat_source_bundled
                        })
                        val age = state.tleMetadata?.epochAgeDays?.let { stringResource(R.string.sat_age_days, SatFmt.number(it, 1, isFa)) }
                            ?: stringResource(R.string.sat_unknown)
                        TelemetryTile(stringResource(R.string.sat_epoch), "$age · $source", tokens, Modifier.weight(1f),
                            muted = staleBadgeText != null || !isOnline)
                    }
                }
            }
        }
    }
}

private data class SatellitePhoto(val url: String, val credit: String, val creditFa: String, val sourcePage: String, val localResourceId: Int? = null, val isRendering: Boolean = false)

private fun satellitePhoto(item: SatelliteItem): SatellitePhoto? = when {
    item.isTrain || item.category == SatelliteCategory.STARLINK -> SatellitePhoto(
        "", "Official SpaceX Photos · CC0", "تصاویر رسمی اسپیس‌اکس · مالکیت عمومی",
        "https://commons.wikimedia.org/wiki/File:Starlink_Mission_(47926144123).jpg",
        localResourceId = R.drawable.satellite_starlink_stack)
    item.id == "iss_zarya" -> SatellitePhoto(
        "", "NASA/JSC · Public domain", "ناسا / مرکز فضایی جانسون · مالکیت عمومی",
        "https://commons.wikimedia.org/wiki/File:STS-134_International_Space_Station_after_undocking.jpg",
        localResourceId = R.drawable.satellite_iss_sts134)
    item.id == "hubble" -> SatellitePhoto(
        "", "NASA · Hubble Servicing Mission 4", "ناسا · مأموریت تعمیراتی چهارم هابل",
        "https://esahubble.org/images/hubble_large_08_cc/",
        localResourceId = R.drawable.satellite_hubble_sm4)
    item.id.contains("envisat", true) -> SatellitePhoto(
        "", "©ESA/A. Van Der Geest · ESA Standard Licence", "©ESA/A. Van Der Geest · مجوز استاندارد ESA",
        "https://www.esa.int/ESA_Multimedia/Images/2012/03/Envisat_in_the_clean_room_at_ESTEC",
        localResourceId = R.drawable.satellite_envisat_estec)
    item.id == "tiangong" -> SatellitePhoto(
        "", "Shujianyang · CC BY-SA 4.0 · mission rendering", "Shujianyang · مجوز CC BY-SA 4.0 · بازسازی مأموریت",
        "https://commons.wikimedia.org/wiki/File:Tiangong_Space_Station_Rendering_2022.11.01.jpg",
        localResourceId = R.drawable.satellite_tiangong_rendering, isRendering = true)
    else -> null
}

@Composable
private fun SatellitePhotoPanel(item: SatelliteItem, tokens: ZigSatTokens, isFa: Boolean) {
    val photo = remember(item.id, item.isTrain, item.category) { satellitePhoto(item) } ?: return
    val uriHandler = LocalUriHandler.current
    val shape = squircleShape(topStart = SatRadius.card, topEnd = SatRadius.card)
    Box(Modifier.fillMaxWidth().height(190.dp).clip(shape)
        .background(Brush.linearGradient(listOf(tokens.cardAlt, tokens.canvas))), contentAlignment = Alignment.Center) {
        SatelliteSilhouette(item, tokens.accentInk.copy(alpha = .85f), Modifier.size(104.dp))
        AsyncImage(model = photo.localResourceId ?: photo.url, contentDescription = if (isFa) "تصویر مرتبط با ${fullNameOf(item, true)}" else "Satellite image for ${fullNameOf(item, false)}",
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxWidth().height(58.dp).align(Alignment.BottomCenter)
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .78f))))
            .padding(horizontal = 14.dp, vertical = 9.dp), contentAlignment = Alignment.BottomStart) {
            val contextLabel = if (isFa) when {
                item.isTrain || item.category == SatelliteCategory.STARLINK -> "ردیف استارلینک‌ها پیش از پرتاب"
                item.id == "iss_zarya" -> "ایستگاه فضایی بین‌المللی از فضا · مأموریت STS-134"
                item.id == "hubble" -> "تلسکوپ هابل هنگام مأموریت تعمیراتی چهارم"
                item.id.contains("envisat", true) -> "ماهوارهٔ انویسات در اتاق پاک ESA"
                photo.isRendering -> "بازسازی پیکربندی ایستگاه تیانگونگ"
                else -> "تصویر مأموریت"
            } else when {
                item.isTrain || item.category == SatelliteCategory.STARLINK -> "Starlink spacecraft stack before deployment"
                item.id == "iss_zarya" -> "International Space Station in orbit · STS-134"
                item.id == "hubble" -> "Hubble during Servicing Mission 4"
                item.id.contains("envisat", true) -> "Envisat flight model in ESA clean room"
                photo.isRendering -> "Tiangong station configuration rendering"
                else -> "Mission image"
            }
            Text("$contextLabel · ${if (isFa) photo.creditFa else photo.credit}",
                Modifier.clickable { runCatching { uriHandler.openUri(photo.sourcePage) } },
                color = Color.White.copy(alpha = .92f),
                style = RedTypographyTokens.badge.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium), maxLines = 2,
                overflow = TextOverflow.Ellipsis)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TelemetryHeroTile(label: String, value: String, unit: String, tokens: ZigSatTokens, modifier: Modifier, sub: String? = null) {
    Column(modifier.heightIn(min = 88.dp).satMaterial(tokens, SatSlot.PassCard, squircleShape(SatRadius.tile),
        backing = tokens.cardAlt, accent = if (tokens.skin == SatSkin.AETHER) tokens.timeAccent else null).padding(10.dp)) {
        SatText(label, tokens.inkSecondary, size = 11)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (tokens.skin == SatSkin.AETHER && LocalDensity.current.fontScale < 1.3f) {
                SatMetric(value, "", label, tokens, Modifier.padding(top = 3.dp), roll = true)
            } else SatText(value, tokens.inkPrimary, size = 20, strong = true)
            SatText(unit, tokens.inkSecondary, Modifier.padding(top = 3.dp), size = 11)
        }
        if (sub != null) SatText(sub, tokens.inkSecondary, size = 10)
    }
}

@Composable
private fun TelemetryTile(label: String, value: String, tokens: ZigSatTokens, modifier: Modifier,
    azimuth: Double? = null, muted: Boolean = false) {
    Column(modifier.heightIn(min = 76.dp).satMaterial(tokens, SatSlot.PassCard, squircleShape(14.dp), tokens.cardAlt)
        .padding(7.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SatText(label, tokens.inkSecondary, size = 11)
        if (azimuth != null) Canvas(Modifier.align(Alignment.CenterHorizontally).size(if (tokens.skin == SatSkin.AETHER) 36.dp else 24.dp)) {
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

    ZigSatCard(tokens = tokens, shape = squircleShape(SatRadius.card)) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text = if (isFa) "آیا با چشم دیده می‌شود؟" else "Naked-eye visibility assessment",
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
                    modifier = Modifier.background(tokens.cardAlt, CircleShape).padding(horizontal = 8.dp, vertical = 5.dp),
                )
            CheckRow(
                passed = sunlit,
                text = if (isFa) "در نور خورشید" else "Satellite is sunlit",
                tokens = tokens,
            )
            CheckRow(
                passed = darkEnough,
                text = if (isFa) {
                    "آسمان به‌قدر کافی تاریک است (${twilight.nameFa})"
                } else {
                    "Sky dark enough (${twilight.nameEn})"
                },
                tokens = tokens,
            )
            CheckRow(
                passed = aboveHorizon,
                text = if (isFa) "بالای افق تو" else "Above the observer's horizon",
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
    val result = stringResource(if (passed) R.string.sat_condition_met else R.string.sat_condition_not_met)
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) { stateDescription = result },
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
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .sizeIn(minWidth = SatGrid.minTouchTarget, minHeight = SatGrid.minTouchTarget)
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (isFa) "شناسنامهٔ مأموریت" else "Mission dossier",
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
                    ZigMicroCaps(text = if (isFa) "هدف مأموریت" else "Mission purpose", tokens = tokens, isFa = isFa)
                    Text(text = SatFmt.digits(purpose, isFa), style = RedTypographyTokens.bodySecondary, color = tokens.inkSecondary)
                }

                val significance = if (isFa) satelliteItem.scientificSignificanceFa else satelliteItem.scientificSignificanceEn
                if (significance.isNotBlank()) {
                    ZigMicroCaps(text = if (isFa) "جایگاه علمی و تاریخی" else "Scientific & historical significance", tokens = tokens, isFa = isFa)
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
                    ZigMicroCaps(text = if (isFa) "حقایق تأییدشده" else "Verified key facts", tokens = tokens, isFa = isFa)
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

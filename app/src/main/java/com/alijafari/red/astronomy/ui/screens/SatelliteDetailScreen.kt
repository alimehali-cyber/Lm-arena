package com.alijafari.red.astronomy.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.astro_engine.ISSEngine
import com.alijafari.red.astronomy.astro_engine.SatelliteCatalog
import com.alijafari.red.astronomy.astro_engine.SatelliteCategory
import com.alijafari.red.astronomy.astro_engine.SatelliteEngine
import com.alijafari.red.astronomy.astro_engine.SatelliteItem
import com.alijafari.red.astronomy.astro_engine.SatelliteLiveState
import com.alijafari.red.astronomy.astro_engine.SunEngine
import com.alijafari.red.astronomy.astro_engine.TimeEngine
import com.alijafari.red.astronomy.data.catalog.CanonicalAstroCatalog
import com.alijafari.red.astronomy.data.repository.TleRepository
import com.alijafari.red.astronomy.domain.AppLanguage
import com.alijafari.red.astronomy.domain.UserLocation
import com.alijafari.red.astronomy.ui.satellites.LocalZigSatTokens
import com.alijafari.red.astronomy.ui.satellites.SatFmt
import com.alijafari.red.astronomy.ui.satellites.SatNights
import com.alijafari.red.astronomy.ui.satellites.SatPassActions
import com.alijafari.red.astronomy.ui.satellites.SatPassUi
import com.alijafari.red.astronomy.ui.satellites.SatPassesByNight
import com.alijafari.red.astronomy.ui.satellites.SatReminderOptions
import com.alijafari.red.astronomy.ui.satellites.SatSheetScaffold
import com.alijafari.red.astronomy.ui.satellites.SatSheetTitle
import com.alijafari.red.astronomy.ui.satellites.SatSkin
import com.alijafari.red.astronomy.ui.satellites.SatSpotlightCard
import com.alijafari.red.astronomy.ui.satellites.SatSpotlightUi
import com.alijafari.red.astronomy.ui.satellites.SatCompactPassCard
import com.alijafari.red.astronomy.ui.satellites.SatGrid
import com.alijafari.red.astronomy.ui.satellites.SatRadius
import com.alijafari.red.astronomy.ui.satellites.SatBottomClearance
import com.alijafari.red.astronomy.ui.satellites.ZigMicroCaps
import com.alijafari.red.astronomy.ui.satellites.ZigSatCard
import com.alijafari.red.astronomy.ui.satellites.ZigSatIconButton
import com.alijafari.red.astronomy.ui.satellites.passKey
import com.alijafari.red.astronomy.ui.satellites.zigSatTokens
import com.alijafari.red.astronomy.ui.theme.RedShapes
import com.alijafari.red.astronomy.ui.theme.RedTypographyTokens
import com.alijafari.red.astronomy.ui.theme.squircleShape
import com.alijafari.red.astronomy.util.formatTleStalenessBadge
import com.alijafari.red.astronomy.util.rememberIsNetworkOnline
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Satellite detail, redesigned to the "one stage, three skins" anatomy:
 *
 * 1 status chips · 2 hero · 3 next pass (with pass preview) · 4 live orbital telemetry ·
 * 5 naked-eye visibility assessment · 6 passes for the next 7 days (2 for trains) · 7 mission dossier.
 *
 * Chrome is a floating 48 dp back button plus a title capsule that fades in on scroll — there is no
 * app bar, and the hero uses the active skin's surface tokens rather than a hard-coded navy.
 */
@Composable
fun SatelliteDetailScreen(
    satelliteItem: SatelliteItem,
    userLocation: UserLocation,
    language: AppLanguage,
    onBack: () -> Unit,
    simulationTimestampMs: Long = System.currentTimeMillis(),
    skin: SatSkin = SatSkin.DEFAULT,
    preselectedPassStartMs: Long? = null,
    reminders: Map<String, Int> = emptyMap(),
    onSetReminder: ((SatelliteItem, ISSEngine.ISSPass, Int) -> Unit)? = null,
    onRemoveReminder: ((SatelliteItem, ISSEngine.ISSPass) -> Unit)? = null,
    onShowOnGlobe: ((SatelliteItem) -> Unit)? = null,
    onSpotInAr: ((SatelliteItem) -> Unit)? = null,
    themeMode: com.alijafari.red.astronomy.domain.ThemeMode = com.alijafari.red.astronomy.domain.ThemeMode.DARK,
) {
    BackHandler(enabled = true) { onBack() }

    val isFa = language == AppLanguage.PERSIAN
    val context = LocalContext.current
    val isOnline by rememberIsNetworkOnline(context)
    val tleRepo = remember { TleRepository.getInstance(context) }
    val starlinkCacheVersion by tleRepo.starlinkVersion.collectAsState()
    var tleRefreshTick by remember { mutableIntStateOf(0) }
    val tokens = remember(skin, themeMode) { zigSatTokens(skin, themeMode) }
    val tz = remember { TimeZone.getDefault() }

    var wasOnline by remember { mutableStateOf(isOnline) }
    LaunchedEffect(isOnline) {
        if (!wasOnline && isOnline) {
            withContext(Dispatchers.IO) {
                if (tleRepo.refreshTles()) tleRefreshTick += 1
            }
        }
        wasOnline = isOnline
    }

    // 1 Hz telemetry clock: the panel re-propagates once per second while the screen is open.
    var tickMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000L)
            tickMs = System.currentTimeMillis()
        }
    }

    val canonicalSatellite = remember(satelliteItem) {
        CanonicalAstroCatalog.getCanonicalObject("sat_${satelliteItem.noradId}")
            ?: CanonicalAstroCatalog.getCanonicalObject(satelliteItem.id)
    }

    val state: SatelliteLiveState = remember(satelliteItem, simulationTimestampMs, tickMs, userLocation, starlinkCacheVersion, tleRefreshTick) {
        val calculated = com.alijafari.red.astronomy.astro_engine.AstroDispatchEngine.calculateState(
            idOrAlias = canonicalSatellite?.canonicalId ?: "sat_${satelliteItem.noradId}",
            timestampMs = simulationTimestampMs,
            userLatDeg = userLocation.latitude,
            userLonDeg = userLocation.longitude,
        )
        (calculated?.specializedData as? SatelliteLiveState)
            ?: SatelliteEngine.calculateSatelliteState(
                satellite = satelliteItem,
                timestampMs = simulationTimestampMs,
                userLatDeg = userLocation.latitude,
                userLonDeg = userLocation.longitude,
            )
    }

    val staleBadgeText = remember(satelliteItem.isTrain, state.tleMetadata, starlinkCacheVersion, tleRefreshTick, isFa) {
        formatTleStalenessBadge(
            isViewingActiveTrain = satelliteItem.isTrain,
            tleMetadata = state.tleMetadata,
            starlinkCacheAgeDays = if (satelliteItem.isTrain || satelliteItem.category == SatelliteCategory.STARLINK) {
                tleRepo.getStarlinkCacheAgeDays()
            } else {
                null
            },
            isFa = isFa,
        )
    }

    val roundedStartMs = remember(simulationTimestampMs) { (simulationTimestampMs / 60_000L) * 60_000L }
    var passes by remember { mutableStateOf<List<SatPassUi>?>(null) }

    LaunchedEffect(satelliteItem.id, userLocation, roundedStartMs, starlinkCacheVersion, tleRefreshTick) {
        withContext(Dispatchers.Default) {
            val scanDays = if (satelliteItem.isTrain) 2 else 7
            val predicted = ISSEngine.predictPasses(
                userLatDeg = userLocation.latitude,
                userLonDeg = userLocation.longitude,
                startTimestampMs = roundedStartMs,
                tle = SatelliteEngine.getEffectiveTle(satelliteItem),
                scanDays = scanDays,
                visibleOnly = true,
                standardMag = satelliteItem.standardMagnitude,
            )
            val name = if (isFa) satelliteItem.nameFa else satelliteItem.nameEn
            val mapped = predicted.map { pass ->
                SatPassUi(
                    key = passKey(satelliteItem, pass),
                    sat = satelliteItem,
                    satName = name,
                    pass = pass,
                    nightKey = SatNights.nightKey(pass.startTimeMs, tz),
                    reminderLeadMinutes = reminders[passKey(satelliteItem, pass)],
                    isAutoCovered = false,
                )
            }
            withContext(Dispatchers.Main) { passes = mapped }
        }
    }

    var previewedPassStartMs by remember(preselectedPassStartMs) { mutableStateOf(preselectedPassStartMs) }
    var expandedNights by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var shownCounts by remember { mutableStateOf<Map<Long, Int>>(emptyMap()) }
    var reminderTarget by remember { mutableStateOf<SatPassUi?>(null) }
    val listState = rememberLazyListState()
    val showTitleCapsule by remember { derivedStateOf { listState.firstVisibleItemScrollOffset > 120 || listState.firstVisibleItemIndex > 0 } }

    val allNightGroups = remember(passes, tickMs) { SatNights.group(passes.orEmpty(), tickMs, tz, isFa) }
    // A train's passes repeat every few minutes: two nights is all anyone reads before the list
    // stops being a list and starts being a log.
    val nightGroups = remember(allNightGroups, satelliteItem.isTrain) {
        if (satelliteItem.isTrain) allNightGroups.take(2) else allNightGroups
    }
    val nextPass = remember(passes, tickMs) {
        SatNights.spotlight(passes.orEmpty(), simulationTimestampMs, tz, isFa)
    }
    val previewedPass = remember(passes, previewedPassStartMs) {
        passes.orEmpty().firstOrNull { it.pass.startTimeMs == previewedPassStartMs }
    }
    val spotlightToShow: SatSpotlightUi? = when {
        previewedPass != null -> SatSpotlightUi(
            pass = previewedPass,
            isInProgress = simulationTimestampMs in previewedPass.pass.startTimeMs..previewedPass.pass.endTimeMs,
            remainingMinutes = ((previewedPass.pass.startTimeMs - simulationTimestampMs) / 60_000L).toInt().coerceAtLeast(0),
            simulatedInsidePass = simulationTimestampMs in previewedPass.pass.startTimeMs..previewedPass.pass.endTimeMs,
        )
        else -> nextPass
    }

    val passActions: (SatPassUi) -> SatPassActions = { passUi ->
        SatPassActions(
            onOpenDetail = { previewedPassStartMs = passUi.pass.startTimeMs },
            onShowOnGlobe = { onShowOnGlobe?.invoke(satelliteItem) },
            onAr = { onSpotInAr?.invoke(satelliteItem) },
            onRemind = { reminderTarget = passUi },
            onRemoveReminder = { reminderTarget = passUi },
            isReminderEnabled = onSetReminder != null,
        )
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalZigSatTokens provides tokens) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .testTag("satellite_detail_screen"),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 64.dp,
                bottom = SatGrid.bottomClearance,
            ),
            verticalArrangement = Arrangement.spacedBy(SatGrid.gapBetweenGroups),
        ) {
            // 1 + 2. Status chips and hero
            item(key = "hero") {
                DetailHero(
                    satelliteItem = satelliteItem,
                    state = state,
                    isFa = isFa,
                    isOnline = isOnline,
                    staleBadgeText = staleBadgeText,
                    onAr = { onSpotInAr?.invoke(satelliteItem) },
                    onShowOnGlobe = { onShowOnGlobe?.invoke(satelliteItem) },
                    onRemind = { passes?.firstOrNull()?.let { reminderTarget = it } },
                )
            }

            // 3. Next pass (activated component) + pass preview
            item(key = "next_pass") {
                val spotlight = spotlightToShow
                if (spotlight != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (previewedPass != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ZigMicroCaps(
                                    text = if (isFa) {
                                        "پیش‌نمایش گذر " + SatFmt.dateTime(previewedPass.pass.startTimeMs, true, tz)
                                    } else {
                                        "Showing pass of " + SatFmt.dateTime(previewedPass.pass.startTimeMs, false, tz)
                                    },
                                    tokens = tokens,
                                    isFa = isFa,
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Box(
                                    modifier = Modifier
                                        .height(SatGrid.minTouchTarget)
                                        .clickable { previewedPassStartMs = null },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = if (isFa) "بازگشت به بعدی" else "Back to next",
                                        style = RedTypographyTokens.badge.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                                        color = tokens.live,
                                    )
                                }
                            }
                        }
                        SatSpotlightCard(
                            data = spotlight,
                            tokens = tokens,
                            isFa = isFa,
                            actions = SatPassActions(
                                onOpenDetail = { previewedPassStartMs = spotlight.pass.pass.startTimeMs },
                                onShowOnGlobe = { onShowOnGlobe?.invoke(satelliteItem) },
                                onAr = { onSpotInAr?.invoke(satelliteItem) },
                                onRemind = { reminderTarget = spotlight.pass },
                                onRemoveReminder = { reminderTarget = spotlight.pass },
                                isReminderEnabled = onSetReminder != null,
                            ),
                            arcHeight = 160.dp,
                            showActions = false,
                        )
                        if (satelliteItem.isTrain) {
                            Row(
                                modifier = Modifier
                                    .clip(RedShapes.capsule)
                                    .background(tokens.starlink.copy(alpha = 0.16f))
                                    .border(1.dp, tokens.starlink.copy(alpha = 0.5f), RedShapes.capsule)
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = if (isFa) {
                                        "رشته مروارید · " + SatFmt.int(satelliteItem.trainCount, true) + " ماهواره"
                                    } else {
                                        "STRING OF PEARLS · ${satelliteItem.trainCount} SATS"
                                    },
                                    style = RedTypographyTokens.badge.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = tokens.inkPrimary,
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(SatGrid.minTouchTarget)
                                .clip(RedShapes.capsule)
                                .background(tokens.live.copy(alpha = 0.14f))
                                .border(1.dp, tokens.live.copy(alpha = 0.5f), RedShapes.capsule)
                                .clickable { reminderTarget = spotlight.pass }
                                .testTag("detail_schedule_reminder"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(
                                    imageVector = Icons.Filled.NotificationsActive,
                                    contentDescription = null,
                                    tint = tokens.live,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = if (isFa) "تنظیم یادآوری گذر" else "Schedule reminder alert",
                                    style = RedTypographyTokens.badge.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                                    color = tokens.live,
                                )
                            }
                        }
                    }
                }
            }

            // 4. Live orbital telemetry
            item(key = "telemetry") {
                DetailTelemetryPanel(state = state, tokens = tokens, isFa = isFa, staleBadgeText = staleBadgeText, isOnline = isOnline)
            }

            // 5. Naked-eye visibility assessment
            item(key = "visibility") {
                DetailVisibilityPanel(state = state, tokens = tokens, isFa = isFa, userLocation = userLocation, tickMs = tickMs)
            }

            // 6. Passes for the next 7 days (2 for trains)
            item(key = "passes_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = if (isFa) {
                            "گذرها · ۷ روز آینده"
                        } else {
                            "Passes · next ${if (satelliteItem.isTrain) "2" else "7"} days"
                        },
                        style = RedTypographyTokens.sectionTitle.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                        color = tokens.inkPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = SatFmt.intPadded(passes?.size ?: 0, 2, isFa),
                        style = RedTypographyTokens.badge.copy(fontSize = 11.sp).tabular(),
                        color = tokens.inkTertiary,
                    )
                }
            }

            item(key = "day_strip") {
                val days = nightGroups.take(7)
                if (days.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(days, key = { it.nightKey }) { group ->
                            DayChip(
                                title = SatFmt.digits(
                                    java.text.SimpleDateFormat("d", java.util.Locale.US)
                                        .apply { timeZone = tz }
                                        .format(java.util.Date(group.nightKey)),
                                    isFa,
                                ),
                                count = group.passes.size,
                                dotColor = group.best?.quality?.hue,
                                tokens = tokens,
                                isFa = isFa,
                            )
                        }
                    }
                }
            }

            item(key = "passes") {
                val current = passes
                if (current == null) {
                    SatPassSkeletonList(tokens)
                } else {
                    SatPassesByNight(
                        groups = nightGroups,
                        tokens = tokens,
                        isFa = isFa,
                        actionsFor = passActions,
                        onToggleNight = { key ->
                            expandedNights = if (key in expandedNights) expandedNights - key else expandedNights + key
                        },
                        onShowMore = { key ->
                            shownCounts = shownCounts + (key to ((shownCounts[key] ?: 3) + 1))
                        },
                        expandedNights = if (satelliteItem.isTrain) nightGroups.map { it.nightKey }.toSet() else expandedNights,
                        shownCounts = shownCounts,
                        maxCardsPerNight = if (satelliteItem.isTrain) 6 else 10,
                    )
                }
            }

            // 7. Mission dossier
            item(key = "dossier") {
                DetailDossier(satelliteItem = satelliteItem, tokens = tokens, isFa = isFa)
            }
        }

        // Floating back button + title capsule (no app bar).
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 12.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ZigSatIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = if (isFa) "بازگشت" else "Back",
                tokens = tokens,
                onClick = onBack,
                testTag = "detail_floating_back",
            )
            AnimatedVisibility(visible = showTitleCapsule, enter = fadeIn(), exit = fadeOut()) {
                Row(
                    modifier = Modifier
                        .clip(RedShapes.capsule)
                        .background(tokens.cardAlt)
                        .border(1.dp, tokens.border, RedShapes.capsule)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (state.isNakedEyeVisible) tokens.nakedEye else tokens.telescopic),
                    )
                    Text(
                        text = if (isFa) satelliteItem.nameFa else satelliteItem.nameEn,
                        style = RedTypographyTokens.badge.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                        color = tokens.inkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
    }

    val reminder = reminderTarget
    if (reminder != null) {
        SatSheetScaffold(tokens = tokens, onDismiss = { reminderTarget = null }, testTag = "detail_reminder_popover") {
            SatSheetTitle(
                title = if (isFa) "یادآوری گذر" else "Pass reminder",
                subtitle = reminder.satName,
                tokens = tokens,
            )
            SatReminderOptions(
                tokens = tokens,
                isFa = isFa,
                passStartMs = reminder.pass.startTimeMs,
                selectedLeadMinutes = reminders[reminder.key],
                onChoose = { lead ->
                    onSetReminder?.invoke(satelliteItem, reminder.pass, lead)
                    reminderTarget = null
                },
                onRemove = if (reminders.containsKey(reminder.key) && onRemoveReminder != null) {
                    {
                        onRemoveReminder.invoke(satelliteItem, reminder.pass)
                        reminderTarget = null
                    }
                } else {
                    null
                },
            )
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
                                .background(Color(0xFFE53935).copy(alpha = 0.14f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CloudOff,
                                contentDescription = if (isFa) "آفلاین" else "Offline",
                                tint = Color(0xFFE53935),
                                modifier = Modifier.size(12.dp),
                            )
                            Text(
                                text = if (isFa) "آفلاین" else "Offline",
                                style = RedTypographyTokens.badge.copy(fontSize = 11.sp),
                                color = Color(0xFFE53935),
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
                        tint = tokens.live,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = SatFmt.categoryLabel(satelliteItem.category, isFa),
                        style = RedTypographyTokens.badge.copy(fontSize = 11.sp),
                        color = tokens.live,
                    )
                    Text(
                        text = if (isFa) satelliteItem.nameFa else satelliteItem.nameEn,
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

            val overview = if (isFa) satelliteItem.descriptionFa else satelliteItem.descriptionEn
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
                        color = tokens.live,
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
                            tint = tokens.live,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = if (isFa) "در آسمان AR" else "Spot in AR sky",
                            style = RedTypographyTokens.badge.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                            color = tokens.live,
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
                    testTag = "detail_remind_next",
                )
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
                    text = "mag ${SatFmt.magnitude(state.apparentMagnitude, isFa)}",
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
                    color = if (met == 3) tokens.nakedEye else tokens.inkSecondary,
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
                text = if (isFa) state.reasonFa else state.reasonEn,
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
) {
    Column(
        modifier = Modifier
            .width(56.dp)
            .height(64.dp)
            .clip(squircleShape(SatRadius.tile))
            .background(tokens.cardAlt)
            .border(1.dp, tokens.border, squircleShape(SatRadius.tile)),
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

@Composable
private fun SatPassSkeletonList(tokens: com.alijafari.red.astronomy.ui.satellites.ZigSatTokens) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp)
                    .clip(squircleShape(SatRadius.compactCard))
                    .background(tokens.cardAlt)
                    .border(1.dp, tokens.border, squircleShape(SatRadius.compactCard)),
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
    var expanded by remember { mutableStateOf(true) }
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
                val operator = if (isFa) {
                    satelliteItem.operatorFa.ifBlank { "ناسا / بین‌المللی" }
                } else {
                    satelliteItem.operatorEn.ifBlank { "NASA / International" }
                }
                val launch = satelliteItem.launchDate.ifBlank { "1998" }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DossierCell(if (isFa) "اپراتور" else "Operator", operator, tokens)
                    DossierCell(if (isFa) "تاریخ پرتاب" else "Launch date", SatFmt.digits(launch, isFa), tokens)
                    DossierCell("NORAD ID", SatFmt.int(satelliteItem.noradId, isFa), tokens)
                    DossierCell(if (isFa) "کد بین‌المللی" else "Int'l designation", satelliteItem.designation, tokens)
                }

                val purpose = if (isFa) satelliteItem.missionPurposeFa else satelliteItem.missionPurposeEn
                if (purpose.isNotBlank()) {
                    ZigMicroCaps(text = if (isFa) "هدف ماموریت" else "Mission purpose", tokens = tokens, isFa = isFa)
                    Text(text = purpose, style = RedTypographyTokens.bodySecondary, color = tokens.inkSecondary)
                }

                val significance = if (isFa) satelliteItem.scientificSignificanceFa else satelliteItem.scientificSignificanceEn
                if (significance.isNotBlank()) {
                    ZigMicroCaps(text = if (isFa) "اهمیت علمی و تاریخی" else "Scientific & historical significance", tokens = tokens, isFa = isFa)
                    Text(
                        text = significance,
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
                            color = tokens.live,
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

@Composable
private fun DossierCell(
    label: String,
    value: String,
    tokens: com.alijafari.red.astronomy.ui.satellites.ZigSatTokens,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.width(150.dp)) {
        ZigMicroCaps(text = label, tokens = tokens, isFa = false)
        Text(
            text = value,
            style = RedTypographyTokens.badge.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium).tabular(),
            color = tokens.inkPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Kept for call sites that still want the compact pass card by its historical name. It now renders
 * the redesigned card, so every pass surface in the app is the same component.
 */
@Composable
fun DetailedVisiblePassCard(
    satName: String,
    pass: ISSEngine.ISSPass,
    cityName: String,
    isFa: Boolean,
    isTrainPass: Boolean = false,
    trainSatelliteCount: Int = 1,
    onCardClick: (() -> Unit)? = null,
    onSchedulePassReminder: (leadMinutes: Int) -> Unit
) {
    val tokens = LocalZigSatTokens.current
    val sat = remember(isTrainPass, satName) {
        SatelliteCatalog.satellites.firstOrNull { (if (isFa) it.nameFa else it.nameEn) == satName }
            ?: SatelliteCatalog.satellites.first()
    }
    SatCompactPassCard(
        data = SatPassUi(
            key = passKey(sat, pass),
            sat = sat.copy(isTrain = isTrainPass, trainCount = trainSatelliteCount),
            satName = satName,
            pass = pass,
            nightKey = SatNights.nightKey(pass.startTimeMs, TimeZone.getDefault()),
            reminderLeadMinutes = null,
            isAutoCovered = false,
        ),
        tokens = tokens,
        isFa = isFa,
        actions = SatPassActions(
            onOpenDetail = { onCardClick?.invoke() },
            onShowOnGlobe = { onCardClick?.invoke() },
            onAr = { onCardClick?.invoke() },
            onRemind = onSchedulePassReminder,
            onRemoveReminder = {},
        ),
    )
}

package com.zig.gravity.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zig.gravity.physics.EngineConstants
import com.zig.gravity.sim.SimulationViewModel
import com.zig.gravity.ui.theme.LocalGravityColors
import com.zig.gravity.ui.theme.ZigGravityTheme
import kotlin.math.roundToInt

private const val PREFS_NAME = "zig_gravity_sandbox"
private const val PREFS_KEY = "session_v1"

/**
 * The Gravity Sandbox screen.
 *
 * Composition responsibilities only: the frame loop lives in [SimulationViewModel], the drawing
 * lives in [TabletopCanvas], and this file wires gestures, lifecycle, persistence and chrome.
 */
@Composable
fun GravitySandboxRoot(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    startInPersian: Boolean = true,
    startInDarkTheme: Boolean = true
) {
    val vm: SimulationViewModel = viewModel()
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current

    // ---- persistence (process death) + defaults from the host app -------------------------------
    LaunchedEffect(Unit) {
        if (!vm.restoreAttempted) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val restored = vm.restore(prefs.getString(PREFS_KEY, null))
            if (!restored) vm.applyHostDefaults(persian = startInPersian, dark = startInDarkTheme)
            vm.markRestoreAttempted()
        }
    }

    // ---- the host shell hides its bottom navigation while the sandbox owns the screen -----------
    DisposableEffect(Unit) {
        ImmersiveScreenState.enter()
        onDispose {
            ImmersiveScreenState.exit()
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(PREFS_KEY, vm.serialize()).apply()
        }
    }

    // ---- §Locked 9: single-threaded frame loop, paused automatically when not resumed ------------
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            vm.onLifecycleResumed()
            while (true) {
                withFrameNanos { nanos -> vm.onFrame(nanos) }
            }
        }
    }

    BackHandler(enabled = true) { onBack() }

    ZigGravityTheme(dark = vm.darkTheme) {
        val c = LocalGravityColors.current
        val fa = vm.persian

        var canvasWidthPx by remember { mutableStateOf(0f) }
        var canvasHeightPx by remember { mutableStateOf(0f) }
        var showInspector by remember { mutableStateOf(false) }
        var showAdd by remember { mutableStateOf(false) }
        var showChallenges by remember { mutableStateOf(false) }
        var addAtScene by remember { mutableStateOf<Offset?>(null) }
        var menuBodyId by remember { mutableStateOf(0L) }
        var menuPos by remember { mutableStateOf(Offset.Zero) }

        val pxPerMeter: Double
        if (canvasWidthPx > 0f) {
            pxPerMeter = canvasWidthPx / (EngineConstants.SCENE_WIDTH_AU * EngineConstants.AU)
        } else {
            pxPerMeter = 1.0
        }

        fun toSceneX(px: Float): Double = (px - canvasWidthPx / 2f) / pxPerMeter
        fun toSceneY(py: Float): Double = (canvasHeightPx / 2f - py) / pxPerMeter

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(c.tableBottom)
                .testTag("gravity_sandbox_root")
                .onSizeChanged {
                    canvasWidthPx = it.width.toFloat()
                    canvasHeightPx = it.height.toFloat()
                }
        ) {
            TabletopCanvas(vm = vm, modifier = Modifier.fillMaxSize())

            // ---- gestures (§3.11) ------------------------------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("gravity_gesture_layer")
                    .pointerInput(vm, canvasWidthPx, canvasHeightPx) {
                        detectTapGestures(
                            onTap = { pos ->
                                menuBodyId = 0L
                                val hit = vm.hitTest(
                                    toSceneX(pos.x),
                                    toSceneY(pos.y),
                                    12.dp.toPx() / pxPerMeter
                                )
                                if (hit != 0L) {
                                    if (vm.slingshotArmedId != 0L && hit != vm.slingshotArmedId) {
                                        vm.cancelSlingshot()
                                    }
                                    vm.select(hit)
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                } else {
                                    vm.cancelSlingshot()
                                    vm.deselect()
                                }
                            },
                            onLongPress = { pos ->
                                val hit = vm.hitTest(
                                    toSceneX(pos.x),
                                    toSceneY(pos.y),
                                    12.dp.toPx() / pxPerMeter
                                )
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (hit != 0L) {
                                    vm.select(hit)
                                    menuBodyId = hit
                                    menuPos = pos
                                } else {
                                    // Long-press on empty table opens the add catalog at that spot.
                                    addAtScene = Offset(toSceneX(pos.x).toFloat(), toSceneY(pos.y).toFloat())
                                    showAdd = true
                                }
                            }
                        )
                    }
                    .pointerInput(vm, canvasWidthPx, canvasHeightPx) {
                        var mode = 0 // 0 none, 1 kinematic drag, 2 slingshot aim
                        var anchorX = 0.0
                        var anchorY = 0.0
                        detectDragGestures(
                            onDragStart = { pos ->
                                menuBodyId = 0L
                                val sceneX = toSceneX(pos.x)
                                val sceneY = toSceneY(pos.y)
                                val tolerance = 12.dp.toPx() / pxPerMeter
                                val hit = vm.hitTest(sceneX, sceneY, tolerance)
                                if (vm.slingshotArmedId != 0L && (hit == vm.slingshotArmedId || hit == 0L)) {
                                    mode = 2
                                    val slot = vm.snapshot.slotOfId(vm.slingshotArmedId)
                                    if (slot >= 0) {
                                        anchorX = vm.snapshot.x[slot]
                                        anchorY = vm.snapshot.y[slot]
                                    }
                                } else if (hit != 0L) {
                                    mode = 1
                                    vm.beginDrag(hit)
                                } else {
                                    mode = 0
                                }
                            },
                            onDrag = { change, _ ->
                                when (mode) {
                                    1 -> vm.dragTo(
                                        toSceneX(change.position.x),
                                        toSceneY(change.position.y),
                                        System.nanoTime() / 1.0e9
                                    )
                                    2 -> vm.updateSlingshot(
                                        toSceneX(change.position.x) - anchorX,
                                        toSceneY(change.position.y) - anchorY
                                    )
                                }
                                if (mode != 0) change.consume()
                            },
                            onDragEnd = {
                                when (mode) {
                                    1 -> vm.endDrag()
                                    2 -> vm.releaseSlingshot()
                                }
                                mode = 0
                            },
                            onDragCancel = {
                                when (mode) {
                                    1 -> vm.cancelDrag()
                                    2 -> vm.cancelSlingshot()
                                }
                                mode = 0
                            }
                        )
                    }
            )

            // ---- top chrome ------------------------------------------------------------------
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(c.chrome)
                            .border(1.dp, c.chromeBorder, CircleShape)
                            .clickableTag("sandbox_back") { onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (fa) "بازگشت" else "Back",
                            tint = c.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = if (fa) "میز گرانش" else "Gravity table",
                            color = c.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (fa) vm.preset.titleFa else vm.preset.titleEn,
                            color = c.onSurfaceDim,
                            fontSize = 11.sp
                        )
                    }
                    if (vm.teachingEnabled) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(c.chrome)
                                .border(1.dp, c.chromeBorder, RoundedCornerShape(16.dp))
                                .clickableTag("open_challenges") { showChallenges = true }
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Science, contentDescription = null, tint = c.accent, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = if (fa) "حدس بزن" else "Predict",
                                color = c.accent,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                if (vm.teachingEnabled && vm.teachingConcept != null) {
                    TeachingCardView(vm = vm)
                }

                val notice = vm.notice
                if (notice != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(c.chrome)
                            .border(1.dp, c.accent.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                            .clickableTag("notice") { vm.dismissNotice() }
                            .padding(12.dp)
                    ) {
                        Text(notice, color = c.onSurface, fontSize = 12.sp)
                    }
                }
            }

            // ---- long-press action menu -------------------------------------------------------
            if (menuBodyId != 0L) {
                Column(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (menuPos.x - 60.dp.toPx()).roundToInt().coerceAtLeast(12),
                                (menuPos.y + 18.dp.toPx()).roundToInt()
                            )
                        }
                        .width(170.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(c.chrome)
                        .border(1.dp, c.chromeBorder, RoundedCornerShape(14.dp))
                        .testTag("longpress_menu")
                ) {
                    MenuItem(if (fa) "بررسی" else "Inspect", "menu_inspect") {
                        menuBodyId = 0L
                        showInspector = true
                    }
                    MenuItem(if (fa) "تکثیر" else "Duplicate", "menu_duplicate") {
                        vm.duplicateSelected()
                        menuBodyId = 0L
                    }
                    MenuItem(if (fa) "پرتاب با قلاب‌سنگ" else "Slingshot", "menu_slingshot") {
                        vm.armSlingshot(menuBodyId)
                        menuBodyId = 0L
                    }
                    MenuItem(if (fa) "حذف" else "Remove", "menu_remove") {
                        vm.removeSelected()
                        menuBodyId = 0L
                    }
                }
            }

            // ---- slingshot hint ----------------------------------------------------------------
            if (vm.slingshotArmedId != 0L) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(bottom = 120.dp)
                ) {
                    Text(
                        text = if (fa)
                            "به سمت مخالف بکش و رها کن؛ مسیر نقطه‌چین پیش‌نمایش است."
                        else
                            "Drag away and release; the dotted line is the preview.",
                        color = c.accent,
                        fontSize = 12.sp
                    )
                }
            }

            // ---- bottom HUD ----------------------------------------------------------------------
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp, start = 10.dp, end = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (vm.selectedId != 0L && !showInspector) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(c.chrome)
                            .border(1.dp, c.chromeBorder, RoundedCornerShape(14.dp))
                            .clickableTag("open_inspector") { showInspector = true }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (fa) "بازکردن بازرس" else "Open inspector",
                            color = c.accent,
                            fontSize = 12.sp
                        )
                    }
                }
                HudBar(
                    paused = vm.paused,
                    speedIndex = vm.speedIndex,
                    trailsVisible = vm.trailsVisible,
                    teachingEnabled = vm.teachingEnabled,
                    darkTheme = vm.darkTheme,
                    persian = vm.persian,
                    onTogglePlay = vm::togglePlay,
                    onSpeed = vm::setSpeedIndex,
                    onReset = vm::reset,
                    onToggleTrails = vm::toggleTrails,
                    onToggleTeaching = vm::toggleTeaching,
                    onToggleTheme = vm::toggleTheme,
                    onToggleLanguage = vm::toggleLanguage,
                    onAdd = {
                        addAtScene = null
                        showAdd = true
                    }
                )
            }

            if (showInspector && vm.selectedId != 0L) {
                InspectorSheet(vm = vm, onDismiss = { showInspector = false })
            }
            if (showAdd) {
                val at = addAtScene
                AddBodySheet(
                    vm = vm,
                    sceneX = at?.x?.toDouble(),
                    sceneY = at?.y?.toDouble(),
                    onDismiss = {
                        showAdd = false
                        addAtScene = null
                    }
                )
            }
            if (showChallenges) {
                ChallengeSheet(vm = vm, onDismiss = { showChallenges = false })
            }
        }
    }
}

@Composable
private fun MenuItem(text: String, tag: String, onClick: () -> Unit) {
    val c = LocalGravityColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickableTag(tag, onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        Text(text, color = c.onSurface, fontSize = 13.sp)
    }
}

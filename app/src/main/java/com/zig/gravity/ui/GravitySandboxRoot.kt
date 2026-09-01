package com.zig.gravity.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.border
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
import com.zig.gravity.sim.HapticCue
import com.zig.gravity.sim.Preset
import com.zig.gravity.sim.SimulationViewModel
import com.zig.gravity.ui.theme.LocalGravityColors
import com.zig.gravity.ui.theme.ZigGravityTheme
import kotlin.math.roundToInt
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.PI
import androidx.compose.material.icons.filled.Public
import com.zig.gravity.sim.CameraState
import com.zig.gravity.util.SandboxFormat

private const val PREFS_NAME = "zig_gravity_sandbox"
private const val PREFS_KEY = "session_v1"

/**
 * The Gravity Sandbox screen.
 *
 * Composition responsibilities only: the frame loop lives in [SimulationViewModel], the drawing
 * lives in [TabletopCanvas], and this file wires gestures, lifecycle, persistence and chrome.
 */
/** What the single gesture handler decided this touch is. */
private enum class GestureMode { UNDECIDED, OBJECT, SLINGSHOT, CAMERA, HANDLED }

/** Outcome of the first phase of a gesture, before the mode is fixed. */
private enum class Verdict { LIFTED, MOVED, SECOND_FINGER }

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
                withFrameNanos { nanos ->
                    vm.onFrame(nanos)
                    // §14 — the simulation queues at most one cue per collision event and the
                    // queue is drained here, on the frame it happened. Draining inside the frame
                    // callback means haptics never cost a recomposition, and a resting contact
                    // cannot buzz because no new event is ever queued for it.
                    val cues = vm.pendingHaptics
                    if (cues.isNotEmpty()) {
                        var strongest = HapticCue.LIGHT
                        for (i in cues.indices) {
                            if (cues[i].ordinal > strongest.ordinal) strongest = cues[i]
                        }
                        vm.clearHaptics()
                        // The platform honours the user's system haptic setting, and a device
                        // with no vibrator simply performs nothing.
                        haptics.performHapticFeedback(
                            if (strongest == HapticCue.LIGHT) HapticFeedbackType.TextHandleMove
                            else HapticFeedbackType.LongPress
                        )
                    }
                }
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
        var showPresets by remember { mutableStateOf(false) }
        var showCameraPanel by remember { mutableStateOf(false) }
        var addAtScene by remember { mutableStateOf<Offset?>(null) }
        var menuBodyId by remember { mutableStateOf(0L) }
        var menuPos by remember { mutableStateOf(Offset.Zero) }

        // §1 — every screen<->scene conversion goes through the camera, so the gesture layer, the
        // renderer and hit testing can never disagree about where a body is.
        val cam = vm.camera
        fun toSceneX(px: Float, py: Float): Double = cam.toSceneX(px, py, canvasWidthPx, canvasHeightPx)
        fun toSceneY(px: Float, py: Float): Double = cam.toSceneY(px, py, canvasWidthPx, canvasHeightPx)

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(c.tableBottom)
                .testTag("gravity_sandbox_root")
                .onSizeChanged {
                    canvasWidthPx = it.width.toFloat()
                    canvasHeightPx = it.height.toFloat()
                    vm.onViewportSizePx(canvasWidthPx, canvasHeightPx)
                    // §3.2 scene scale still derives from the viewport width in dp; the camera
                    // sits on top of it and never replaces it.
                    vm.onViewportChanged(canvasWidthPx / density.density.toDouble())
                }
        ) {
            TabletopCanvas(vm = vm, modifier = Modifier.fillMaxSize())

            // ---- §1 unified gesture layer -----------------------------------------------------
            //
            // ONE pointer handler owns tap, long press, object drag, slingshot aim and the camera,
            // so no two detectors can fight over the same finger. Transitions are explicit:
            //
            //   1 finger on a body      -> move that body (position only, never a throw)
            //   1 finger on empty table -> nothing (deliberately inert: it must not spin the view)
            //   2 fingers               -> camera pan + pinch zoom + twist orientation
            //   1 -> 2 fingers          -> the drag ends cleanly, the camera takes over, and the
            //                              camera baseline is re-seeded so nothing jumps
            //   2 -> 1 finger           -> stays in camera mode until every finger lifts
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("gravity_gesture_layer")
                    .pointerInput(vm, canvasWidthPx, canvasHeightPx) {
                        val slop = viewConfiguration.touchSlop
                        val longPressMs = viewConfiguration.longPressTimeoutMillis

                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val startPos = down.position
                            val tolerance = 14.dp.toPx() / cam.pxPerMeter(canvasWidthPx)
                            val hitId = vm.hitTest(
                                toSceneX(startPos.x, startPos.y),
                                toSceneY(startPos.x, startPos.y),
                                tolerance
                            )

                            var mode = GestureMode.UNDECIDED
                            var stillDown = true
                            var slingAnchorX = 0.0
                            var slingAnchorY = 0.0

                            // ---- phase 1: what kind of gesture is this? ----------------------
                            val verdict = withTimeoutOrNull(longPressMs) {
                                var outcome = Verdict.LIFTED
                                while (true) {
                                    val ev = awaitPointerEvent()
                                    val pressed = ev.changes.count { it.pressed }
                                    if (pressed >= 2) {
                                        outcome = Verdict.SECOND_FINGER
                                        break
                                    }
                                    val ch = ev.changes.firstOrNull { it.id == down.id }
                                    if (ch == null || !ch.pressed) {
                                        outcome = Verdict.LIFTED
                                        break
                                    }
                                    if ((ch.position - startPos).getDistance() > slop) {
                                        outcome = Verdict.MOVED
                                        break
                                    }
                                }
                                outcome
                            }

                            when (verdict) {
                                null -> {
                                    // Held still past the long-press timeout.
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (hitId != 0L) {
                                        vm.select(hitId)
                                        menuBodyId = hitId
                                        menuPos = startPos
                                    } else {
                                        addAtScene = Offset(
                                            toSceneX(startPos.x, startPos.y).toFloat(),
                                            toSceneY(startPos.x, startPos.y).toFloat()
                                        )
                                        showAdd = true
                                    }
                                    mode = GestureMode.HANDLED
                                }

                                Verdict.LIFTED -> {
                                    // A tap.
                                    menuBodyId = 0L
                                    if (hitId != 0L) {
                                        if (vm.slingshotArmedId != 0L && hitId != vm.slingshotArmedId) {
                                            vm.cancelSlingshot()
                                        }
                                        vm.select(hitId)
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    } else {
                                        vm.cancelSlingshot()
                                        vm.deselect()
                                    }
                                    mode = GestureMode.HANDLED
                                    // The finger is already up: awaitEachGesture drains the rest.
                                    stillDown = false
                                }

                                Verdict.MOVED -> {
                                    menuBodyId = 0L
                                    val armed = vm.slingshotArmedId
                                    if (armed != 0L && (hitId == armed || hitId == 0L)) {
                                        mode = GestureMode.SLINGSHOT
                                        val slot = vm.snapshot.slotOfId(armed)
                                        if (slot >= 0) {
                                            slingAnchorX = vm.snapshot.x[slot]
                                            slingAnchorY = vm.snapshot.y[slot]
                                        }
                                    } else if (hitId != 0L) {
                                        mode = GestureMode.OBJECT
                                        vm.beginDrag(hitId)
                                    } else {
                                        // Empty-space one-finger drag does nothing at all.
                                        mode = GestureMode.HANDLED
                                    }
                                }

                                Verdict.SECOND_FINGER -> mode = GestureMode.CAMERA
                            }

                            // ---- phase 2: run it until the last finger lifts -----------------
                            var cameraSeeded = false
                            while (stillDown) {
                                val ev = awaitPointerEvent()
                                val pressedCount = ev.changes.count { it.pressed }
                                if (pressedCount == 0) break

                                if (pressedCount >= 2 && mode != GestureMode.CAMERA) {
                                    // A second finger always wins. End the object gesture first so
                                    // the transition can never inject a velocity (§1, §2).
                                    when (mode) {
                                        GestureMode.OBJECT -> vm.endDrag()
                                        GestureMode.SLINGSHOT -> vm.cancelSlingshot()
                                        else -> Unit
                                    }
                                    mode = GestureMode.CAMERA
                                    cameraSeeded = false
                                }

                                when (mode) {
                                    GestureMode.CAMERA -> {
                                        if (!cameraSeeded) {
                                            // Skip the first event after the transition: its deltas
                                            // were measured against the previous finger set.
                                            cameraSeeded = true
                                        } else if (pressedCount >= 2) {
                                            val centroid = ev.calculateCentroid(useCurrent = true)
                                            if (centroid != Offset.Unspecified) {
                                                val pan = ev.calculatePan()
                                                cam.applyTransform(
                                                    centroidX = centroid.x,
                                                    centroidY = centroid.y,
                                                    panPxX = pan.x,
                                                    panPxY = pan.y,
                                                    zoomFactor = ev.calculateZoom(),
                                                    rotationRad = (ev.calculateRotation() * PI / 180.0).toFloat(),
                                                    viewportWidthPx = canvasWidthPx,
                                                    viewportHeightPx = canvasHeightPx
                                                )
                                                vm.onCameraMoved()
                                            }
                                        }
                                        ev.changes.forEach { if (it.pressed) it.consume() }
                                    }

                                    GestureMode.OBJECT -> {
                                        val ch = ev.changes.firstOrNull { it.id == down.id }
                                        if (ch != null && ch.pressed) {
                                            vm.dragTo(
                                                toSceneX(ch.position.x, ch.position.y),
                                                toSceneY(ch.position.x, ch.position.y)
                                            )
                                            ch.consume()
                                        }
                                    }

                                    GestureMode.SLINGSHOT -> {
                                        val ch = ev.changes.firstOrNull { it.id == down.id }
                                        if (ch != null && ch.pressed) {
                                            vm.updateSlingshot(
                                                toSceneX(ch.position.x, ch.position.y) - slingAnchorX,
                                                toSceneY(ch.position.x, ch.position.y) - slingAnchorY
                                            )
                                            ch.consume()
                                        }
                                    }

                                    else -> Unit
                                }
                            }

                            when (mode) {
                                GestureMode.OBJECT -> vm.endDrag()
                                GestureMode.SLINGSHOT -> vm.releaseSlingshot()
                                else -> Unit
                            }
                        }
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
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(c.chrome)
                            .border(1.dp, c.chromeBorder, RoundedCornerShape(16.dp))
                            .clickableTag("open_presets") { showPresets = true }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Public, contentDescription = null, tint = c.accent, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(text = if (fa) "صحنه‌ها" else "Scenes", color = c.accent, fontSize = 11.sp)
                    }
                    Spacer(Modifier.width(6.dp))
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
                // §1 camera controls. Elevation lives on an explicit control rather than an
                // ambiguous two-finger vertical drag, so it can never be confused with a pan.
                if (showCameraPanel) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(c.chrome)
                            .border(1.dp, c.chromeBorder, RoundedCornerShape(16.dp))
                            .testTag("camera_panel")
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        SandboxSlider(
                            value = vm.camera.tiltRad.toFloat(),
                            onValueChange = { v -> vm.setCameraTilt(v.toDouble()) },
                            valueRange = 0f..CameraState.MAX_TILT.toFloat(),
                            label = if (fa) "زاویه دید" else "Viewing angle",
                            readout = SandboxFormat.fixed(vm.camera.tiltRad * 180.0 / PI, 0, fa) + "°",
                            tag = "camera_tilt_slider"
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(c.accent.copy(alpha = 0.10f))
                                    .clickableTag("camera_fit") { vm.frameCameraToContent() }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (fa) "جا دادن همه" else "Fit all", color = c.accent, fontSize = 12.sp)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(c.accent.copy(alpha = 0.10f))
                                    .clickableTag("camera_reset") { vm.frameCameraForPreset(vm.preset) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (fa) "بازنشانی دوربین" else "Reset camera", color = c.accent, fontSize = 12.sp)
                            }
                        }
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
                    },
                    cameraPanelOpen = showCameraPanel,
                    onToggleCameraPanel = { showCameraPanel = !showCameraPanel }
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
            if (showPresets) {
                PresetSheet(vm = vm, onDismiss = { showPresets = false })
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

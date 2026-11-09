package com.zig.gravity.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alijafari.red.astronomy.ui.theme.ChromeBanner
import com.alijafari.red.astronomy.ui.theme.ChromeCapsule
import com.alijafari.red.astronomy.ui.theme.ChromeDivider
import com.alijafari.red.astronomy.ui.theme.ChromeIconDisc
import com.alijafari.red.astronomy.ui.theme.ChromePanel
import com.alijafari.red.astronomy.ui.theme.RedElevation
import com.alijafari.red.astronomy.ui.theme.ChromeTextAction
import com.alijafari.red.astronomy.ui.theme.ChromeTone
import com.alijafari.red.astronomy.ui.theme.RedCornerRadius
import com.alijafari.red.astronomy.ui.theme.RedIconSize
import com.alijafari.red.astronomy.ui.theme.RedTypographyTokens
import com.alijafari.red.astronomy.ui.theme.chromeTokens
import com.alijafari.red.astronomy.ui.theme.squircleShape
import com.zig.gravity.edu.TutorialContent
import com.zig.gravity.physics.EngineConstants
import com.zig.gravity.sim.BodyCatalog
import com.zig.gravity.sim.CameraState
import com.zig.gravity.sim.HapticCue
import com.zig.gravity.sim.Preset
import com.zig.gravity.sim.SimulationViewModel
import com.zig.gravity.sim.TutorialGate
import com.zig.gravity.sim.TutorialStore
import com.zig.gravity.ui.theme.LocalGravityColors
import com.zig.gravity.ui.theme.ZigGravityTheme
import com.zig.gravity.util.SandboxFormat
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlinx.coroutines.withTimeoutOrNull

private const val PREFS_NAME = "zig_gravity_sandbox"
private const val PREFS_KEY = "session_v1"

/**
 * [TutorialStore] over the sandbox's existing preferences file (§15).
 *
 * No new persistence mechanism and no new dependency: the same `zig_gravity_sandbox` file that
 * already holds the serialized session gains one boolean. `commit()` rather than `apply()` on the
 * write, because the thing it guards against is the process dying immediately afterwards.
 */
private class SharedPrefsTutorialStore(context: Context) : TutorialStore {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    override fun isTutorialSeen(): Boolean = prefs.getBoolean(TutorialGate.PREF_KEY, false)
    override fun markTutorialSeen() {
        prefs.edit().putBoolean(TutorialGate.PREF_KEY, true).commit()
    }
}

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

    // The sandbox has no language of its own. ZIG's locale is the single source of truth, pushed in
    // on every entry and again whenever the app's language changes underneath us, so a restored
    // session can never resurrect a stale sandbox-local language.
    LaunchedEffect(startInPersian) {
        vm.applyHostLanguage(startInPersian)
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

    // ---- §1/§15/§25 the first-launch tutorial -----------------------------------------------
    //
    // "First launch" means the user has never completed or skipped it, ever — not "first time this
    // composable ran". The flag lives in the sandbox's existing SharedPreferences file, so it
    // survives leaving the screen, process death and reinstall-free app restarts alike.
    val tutorialStore = remember(context) { SharedPrefsTutorialStore(context) }
    var showTutorial by rememberSaveable { mutableStateOf(false) }
    var tutorialChecked by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!tutorialChecked) {
            tutorialChecked = true
            if (TutorialGate.shouldAutoShow(tutorialStore)) showTutorial = true
        }
    }

    /** Skip, finish and Back are the same action: close it, and never auto-show it again (§13/§20). */
    val dismissTutorial: () -> Unit = {
        showTutorial = false
        tutorialStore.markTutorialSeen()
    }

    // §20 — while the tutorial is up, Back closes the tutorial rather than leaving the sandbox.
    BackHandler(enabled = true) {
        if (showTutorial) dismissTutorial() else onBack()
    }

    ZigGravityTheme(surfaceKey = vm.tableSurface) {
        // The tabletop's palette (fills, gradients, trails, cast shadows: the physics' own material)
        // and, over it, the app's chrome — one accent, one hairline, one type scale, one corner
        // geometry, taken from the selected theme and interface style.
        val c = LocalGravityColors.current
        val chrome = chromeTokens()
        val fa = vm.persian

        var canvasWidthPx by remember { mutableStateOf(0f) }
        var canvasHeightPx by remember { mutableStateOf(0f) }
        var showInspector by remember { mutableStateOf(false) }
        var showAdd by remember { mutableStateOf(false) }
        var showChallenges by remember { mutableStateOf(false) }
        var showPresets by remember { mutableStateOf(false) }
        var showCameraPanel by remember { mutableStateOf(false) }
        var showTableSurface by remember { mutableStateOf(false) }
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
                    // No title band and no back chip (Part 6, item 8): the sandbox is a place, not a
                    // page with a header. The system back gesture leaves it, and the controls that
                    // belong to the simulation — help, scenes, predict — keep the row to themselves.
                    Spacer(Modifier.weight(1f))
                    // §16 — the permanent way back into the tutorial. Compact, wordless, and in
                    // the top bar where help belongs; it shares the header's inset handling, so it
                    // can never end up under the status bar or behind the + button.
                    // Help, scenes and predict are the app's capsules now — the same object as an AR
                    // smart pill, drawn by the same component, so the two immersive screens agree.
                    HelpDisc(fa = fa, onClick = { showTutorial = true })
                    Spacer(Modifier.width(6.dp))
                    ChromeCapsule(
                        label = if (fa) "صحنه‌ها" else "Scenes",
                        icon = Icons.Filled.Public,
                        onClick = { showPresets = true },
                        testTag = "open_presets"
                    )
                    Spacer(Modifier.width(6.dp))
                    if (vm.teachingEnabled) {
                        ChromeCapsule(
                            label = if (fa) "حدس بزن" else "Predict",
                            icon = Icons.Filled.Science,
                            onClick = { showChallenges = true },
                            highlighted = true,
                            testTag = "open_challenges"
                        )
                    }
                }

                // §11 — Follow is a mode, and a mode the user cannot see is a mode they will be
                // confused by. One compact chip, naming the body, with its own way out.
                if (vm.isFollowing) {
                    val fSlot = vm.snapshot.slotOfId(vm.followTargetId)
                    val fName = if (fSlot >= 0) {
                        BodyCatalog.nameOf(vm.snapshot.catalogKey[fSlot], vm.snapshot.typeOf(fSlot), fa)
                    } else {
                        ""
                    }
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(c.accent.copy(alpha = if (c.isDark) 0.16f else 0.12f), CircleShape)
                            .border(1.dp, c.accent.copy(alpha = 0.45f), CircleShape)
                            .clickableTag("follow_indicator") { vm.stopFollow() }
                            .padding(start = 12.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.GpsFixed,
                            contentDescription = null,
                            tint = c.accent,
                            modifier = Modifier.size(RedIconSize.xs)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (fa) "در حال دنبال کردن $fName" else "Following $fName",
                            color = c.accent,
                            style = RedTypographyTokens.badge.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = if (fa) "پایان دنبال کردن" else "Stop following",
                            tint = c.accent.copy(alpha = 0.8f),
                            modifier = Modifier.size(RedIconSize.xs)
                        )
                    }
                }

                if (vm.teachingEnabled && vm.teachingConcept != null) {
                    TeachingCardView(vm = vm)
                }

                val notice = vm.notice
                if (notice != null) {
                    ChromeBanner(
                        text = notice,
                        tone = ChromeTone.ACCENT,
                        onDismiss = { vm.dismissNotice() },
                        testTag = "notice"
                    )
                }
            }

            // ---- long-press action menu -------------------------------------------------------
            if (menuBodyId != 0L) {
                ChromePanel(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (menuPos.x - 60.dp.toPx()).roundToInt().coerceAtLeast(12),
                                (menuPos.y + 18.dp.toPx()).roundToInt()
                            )
                        }
                        .width(178.dp)
                        .testTag("longpress_menu"),
                    elevation = RedElevation.popover,
                    shape = chrome.rowShape
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        MenuItem(if (fa) "بررسی" else "Inspect", "menu_inspect") {
                            menuBodyId = 0L
                            showInspector = true
                        }
                        ChromeDivider(modifier = Modifier.padding(horizontal = 12.dp))
                        MenuItem(if (fa) "تکثیر" else "Duplicate", "menu_duplicate") {
                            vm.duplicateSelected()
                            menuBodyId = 0L
                        }
                        ChromeDivider(modifier = Modifier.padding(horizontal = 12.dp))
                        MenuItem(if (fa) "پرتاب با قلاب‌سنگ" else "Slingshot", "menu_slingshot") {
                            vm.armSlingshot(menuBodyId)
                            menuBodyId = 0L
                        }
                        ChromeDivider(modifier = Modifier.padding(horizontal = 12.dp))
                        MenuItem(if (fa) "حذف" else "Remove", "menu_remove") {
                            vm.removeSelected()
                            menuBodyId = 0L
                        }
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
                    ChromeCapsule(
                        label = if (fa) "بازکردن بازرس" else "Open inspector",
                        icon = Icons.Filled.Tune,
                        onClick = { showInspector = true },
                        highlighted = true,
                        testTag = "open_inspector"
                    )
                }
                // §1 camera controls. Elevation lives on an explicit control rather than an
                // ambiguous two-finger vertical drag, so it can never be confused with a pan.
                AnimatedVisibility(
                    visible = showCameraPanel,
                    enter = fadeIn(tween(220)) + expandVertically(tween(220, easing = FastOutSlowInEasing)),
                    exit = fadeOut(tween(160)) + shrinkVertically(tween(160, easing = FastOutSlowInEasing)),
                    label = "CameraPanel"
                ) {
                    ChromePanel(
                        modifier = Modifier.fillMaxWidth().testTag("camera_panel"),
                        elevation = RedElevation.popover
                    ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        // §3 ROOT CAUSE. `vm.camera` is a plain class, not Compose state, so a
                        // composable reading `camera.tiltRad` never recomposed: the slider drove
                        // the camera correctly but then re-synced its thumb and its readout from a
                        // value frozen at the last unrelated recomposition, so the control and the
                        // camera silently drifted apart. Reading `cameraTick` here subscribes this
                        // panel to every camera mutation — slider, gesture, reset and follow alike.
                        @Suppress("UNUSED_VARIABLE") val camTick = vm.cameraTick
                        SandboxSlider(
                            value = vm.cameraTiltFraction.toFloat(),
                            onValueChange = { v -> vm.setCameraTiltFraction(v.toDouble()) },
                            valueRange = 0f..1f,
                            label = if (fa) "زاویه دید" else "Viewing angle",
                            readout = SandboxFormat.fixed(vm.camera.tiltRad * 180.0 / PI, 0, fa) + "°",
                            tag = "camera_tilt_slider"
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ChromeTextAction(
                                text = if (fa) "جا دادن همه" else "Fit all",
                                onClick = { vm.frameCameraToContent() },
                                modifier = Modifier.weight(1f),
                                testTag = "camera_fit"
                            )
                            ChromeTextAction(
                                text = if (fa) "بازنشانی دوربین" else "Reset camera",
                                onClick = { vm.restorePresetCamera() },
                                modifier = Modifier.weight(1f),
                                testTag = "camera_reset"
                            )
                        }
                    }
                    }
                }

                HudBar(
                    paused = vm.paused,
                    speedIndex = vm.speedIndex,
                    trailsVisible = vm.trailsVisible,
                    teachingEnabled = vm.teachingEnabled,
                    persian = vm.persian,
                    onTogglePlay = vm::togglePlay,
                    onSpeed = vm::setSpeedIndex,
                    onReset = vm::reset,
                    onToggleTrails = vm::toggleTrails,
                    onToggleTeaching = vm::toggleTeaching,
                    onOpenTableSurface = { showTableSurface = true },
                    cameraPanelOpen = showCameraPanel,
                    onToggleCameraPanel = { showCameraPanel = !showCameraPanel }
                )
            }

            // §16/§17 — the primary creative action, as a compact wordless disc.
            //
            // A 52 dp visible circle inside a 56 dp touch target: comfortably above the 48 dp
            // minimum without becoming the loudest thing on a screen whose subject is the physics.
            // It carries no label because a plus needs none, and its meaning does not mirror, so
            // only its side of the screen follows the layout direction (§18).
            //
            // It hides whenever any sheet or panel is open (§17). A floating action button that
            // sits on top of the very sheet it just opened is the classic version of this bug;
            // here the button simply is not there while a sheet owns the interaction.
            val anySheetOpen = showAdd || showInspector || showPresets ||
                showChallenges || showTutorial || showCameraPanel || showTableSurface
            if (!anySheetOpen) {
                AddDisc(
                    contentDescription = if (fa) "افزودن جسم" else "Add body",
                    onClick = {
                        addAtScene = null
                        showAdd = true
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(bottom = 78.dp, end = 14.dp)
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
            if (showTableSurface) {
                TableSurfaceSheet(vm = vm, onDismiss = { showTableSurface = false })
            }
            // Rendered last so it sits above every sheet, and given the app's locale rather than
            // any tutorial-local language state (§2/§23).
            if (showTutorial) {
                TutorialOverlay(persian = fa, onDismiss = dismissTutorial)
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
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = text,
            color = c.onSurface,
            style = RedTypographyTokens.bodySecondary
        )
    }
}

/**
 * The primary creative action: one filled disc in the app's accent.
 *
 * A 52 dp circle inside a 56 dp touch target — above the 48 dp minimum without becoming the loudest
 * thing on a screen whose subject is the physics. It replaced a translucent teal ring, which was the
 * only control on the sandbox that did not look like a ZIG control: now it is the app's accent fill,
 * the ink the theme repaired against that fill, and the same 0.94 press spring as every disc around it.
 */
@Composable
private fun AddDisc(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chrome = chromeTokens()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 440f),
        label = "AddDiscPress"
    )
    Box(
        modifier = modifier
            .size(56.dp)
            .clickableTag("hud_add", onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(chrome.accent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = contentDescription,
                tint = chrome.onAccent,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

/**
 * The permanent way into the tutorial, as a wordless disc in the app's chrome.
 *
 * A question mark rather than an icon: it is the same glyph in both languages (؟ / ?), it needs no
 * image asset, and it reads as help at the size of the other controls around it.
 */
@Composable
private fun HelpDisc(fa: Boolean, onClick: () -> Unit) {
    val c = LocalGravityColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 460f),
        label = "HelpDiscPress"
    )
    Box(
        modifier = Modifier
            .size(36.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(c.chrome, CircleShape)
            .border(1.dp, c.chromeBorder, CircleShape)
            .semantics {
                contentDescription = if (fa) TutorialContent.OPEN_HELP_FA else TutorialContent.OPEN_HELP_EN
            }
            .clickableTag("open_tutorial", onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (fa) "؟" else "?",
            color = c.accent,
            style = RedTypographyTokens.badge.copy(fontWeight = FontWeight.Bold)
        )
    }
}

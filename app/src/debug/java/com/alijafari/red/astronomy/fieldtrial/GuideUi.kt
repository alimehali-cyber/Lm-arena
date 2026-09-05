@file:Suppress("PrivateApi")

package com.alijafari.red.astronomy.fieldtrial

import android.app.Activity
import android.content.Intent
import android.location.Location
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.fieldtrial.engine.FieldTrialMachine
import com.alijafari.red.astronomy.fieldtrial.engine.InverseProjection
import com.alijafari.red.astronomy.fieldtrial.engine.Json
import com.alijafari.red.astronomy.fieldtrial.engine.Gating
import com.alijafari.red.astronomy.fieldtrial.engine.LevelOutcome
import com.alijafari.red.astronomy.fieldtrial.engine.LevelStatus
import com.alijafari.red.astronomy.fieldtrial.engine.SkipReason
import com.alijafari.red.astronomy.fieldtrial.engine.SunEvents
import com.alijafari.red.astronomy.fieldtrial.engine.TapMeasurement
import com.alijafari.red.astronomy.fieldtrial.engine.TargetPicker
import com.alijafari.red.astronomy.fieldtrial.engine.TrackerProjector
import com.alijafari.red.astronomy.startracker.debug.FieldTrialHost
import com.alijafari.red.astronomy.startracker.tracking.LockConfidence
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * G-1/G-3: the guided field-trial UI (debug source set only). One guide card at the
 * bottom (height <= 18% so it never touches the centre 60%), collapsible to a one-line
 * pill, tap-to-measure with a draggable crosshair + Confirm, thin marker rings on the
 * AR canvas, dim-red night palette after civil dusk, Part B watchers, Share zip.
 * The tester never types numbers (G-1.6).
 */
object GuideUi {

    // ---------- palettes (G-1.4/G-1.5: plain, >=16 sp, dim red on black at night) ----------

    class Palette(
        val bg: Color, val fg: Color, val dim: Color, val accent: Color,
        val ring: Color, val ringGreen: Color
    )

    fun dayPalette() = Palette(
        bg = Color(0xF0141414), fg = Color.White, dim = Color(0xFF9AA4B2),
        accent = Color(0xFF8AB4F8), ring = Color(0xFF6699FF), ringGreen = Color(0xFF66FF99)
    )

    fun nightPalette() = Palette(
        bg = Color(0xEE000000), fg = Color(0xCCAA6666), dim = Color(0x99885555),
        accent = Color(0xCCBB7777), ring = Color(0xCC995555), ringGreen = Color(0xCC779977)
    )

    // ---------- level copy (title <= 4 words, instruction <= 2 sentences, plain English) ----------

    data class LevelCopy(
        val title: String, val instruction: String,
        val helpTitle: String, val helpBody: String
    )

    fun levelCopy(level: Int, target: TargetPicker.SkyTarget?): LevelCopy = when (level) {
        0 -> LevelCopy(
            "Get ready",
            "Allow camera and location. Then wave the phone in a figure-8 for 5 seconds.",
            "Why the figure-8?",
            "Waving the phone in a figure-8 calibrates the compass. The checklist fills itself in - you never type anything."
        )
        1 -> LevelCopy(
            "Find the Sun",
            "Point the phone toward the Sun. Tap the real Sun where you see it in the picture - don't stare at it.",
            "About the Sun check",
            "Never look straight at the Sun; glance at the PHONE screen only. If the Sun is hidden or near the horizon, use the button below to jump to the Moon level or skip."
        )
        2 -> {
            val n = target?.name ?: "Moon"
            LevelCopy(
                "Find the $n",
                "The $n is marked with a ring. Tap the real $n where you see it in the picture.",
                "How to tap",
                "Tap slowly, drag the crosshair until it sits exactly on the $n, then press Confirm. The app does the measuring."
            )
        }
        3 -> {
            val n = target?.name ?: "the bright star"
            LevelCopy(
                "A bright star",
                "Find $n - it's the brightest star ${directionPhrase(target)} right now. Put the ring in the middle of the screen, then tap the star.",
                "How to find $n",
                (target?.howToFind ?: "Match the brightest steady point of light.") +
                    " The ring on the screen shows exactly which point of light to tap."
            )
        }
        4 -> LevelCopy(
            "Star at the edge",
            "Turn slowly until the ring is near the left or right edge of the screen. Tap the star again.",
            "Why the edge?",
            "The middle of the screen is the easy case; the edges stress the camera math. That is why this level asks you to move the star to the side before tapping."
        )
        5 -> LevelCopy(
            "Seven stars",
            "Tap each star the guide names, one at a time. The ring shows exactly where it should be.",
            "About the seven stars",
            "These stars had position mistakes in an earlier version of the app, so the trial checks each one. Stars below the horizon tonight are listed in Details and skipped."
        )
        6 -> LevelCopy(
            "Credits",
            "Open the app's About or credits screen. Is there a line crediting the HYG star database?",
            "About this check",
            "The star data comes from the free HYG database and must be credited in the app. There is no About screen in this build yet, so 'No' is the expected answer today."
        )
        7 -> LevelCopy(
            "Southern sky",
            "Face north. On the horizon strip, is East on the RIGHT?",
            "About this check",
            "South of the equator the sky picture flips. This one yes/no check catches a flipped horizon, which would put every object on the wrong side."
        )
        8 -> LevelCopy(
            "Lock on",
            "Tap Turn on, then point at the darkest, starriest part of the sky and rest the phone on something. Hold still.",
            "About the star tracker",
            "The star tracker works out where the phone points by recognizing star patterns - the way a spacecraft does. It needs a steady, dark, starry view and can take up to a minute."
        )
        9 -> {
            val n = target?.name ?: "the star"
            LevelCopy(
                "Is it better?",
                "Two rings are on $n: blue is the compass, green is the star tracker. Tap the real star.",
                "Blue ring vs green ring",
                "The blue ring follows the phone's compass; the green ring follows the stars themselves. Tap the REAL star in the sky - whichever ring sits closer wins."
            )
        }
        10 -> LevelCopy(
            "Cover the camera",
            "Three timed steps: hand over the lens, a lit wall, then the ground. Just hold still while the countdown runs.",
            "What is being checked?",
            "With the lens covered the star tracker must never claim it found stars. If it does, that step fails automatically and saves the picture as proof."
        )
        11 -> LevelCopy(
            "Slow sweep",
            "Move the phone slowly across the sky for 30 seconds.",
            "What is being checked?",
            "While moving, the tracker may lose and re-find the stars - that is fine. Claiming a direction while unsure is NOT fine; the trial counts those automatically."
        )
        12 -> LevelCopy(
            "Finish",
            "All done. Share your results, or start a new trial.",
            "You made it",
            "Share sends a small zip with every measurement, the summary, and the saved pictures. Nothing leaves the phone unless you choose to send it."
        )
        else -> LevelCopy("Level $level", "", "", "")
    }

    private fun directionPhrase(target: TargetPicker.SkyTarget?): String {
        val az = ((target?.azDeg ?: return "high in the sky") % 360.0 + 360.0) % 360.0
        val alt = target.altDeg
        val cardinal = when ((az + 22.5).toInt() / 45 % 8) {
            0 -> "north"; 1 -> "northeast"; 2 -> "east"; 3 -> "southeast"
            4 -> "south"; 5 -> "southwest"; 6 -> "west"; else -> "northwest"
        }
        return when {
            alt > 55 -> "high in the $cardinal"
            alt > 25 -> "halfway up the $cardinal sky"
            else -> "low in the $cardinal"
        }
    }

    val tapLevels = setOf(1, 2, 3, 4, 5, 9)

    // ---------- root ----------

    @Composable
    fun Guide(controller: FieldTrialController, access: FieldTrialHost.Access) {
        // poll the live frame (written by the AR screen each projection pass)
        var frame by remember { mutableStateOf<FieldTrialHost.FrameState?>(FieldTrialHost.frame) }
        LaunchedEffect(Unit) {
            while (true) {
                frame = FieldTrialHost.frame
                // keep the tracker's acquisition cross-check + GPS feed fresh (debug only)
                val f = frame
                if (f?.rotationMatrix != null) StarTrackerRuntime.state.sensorSnapshot = f.rotationMatrix
                f?.gps?.let { StarTrackerRuntime.state.gps = it }
                delay(66)
            }
        }
        val rev = controller.revision.value

        // night detection (G-1.5): after civil dusk at the current location
        var night by remember { mutableStateOf(false) }
        LaunchedEffect(frame?.gps?.latitude, frame?.gps?.longitude) {
            while (true) {
                val g = FieldTrialHost.frame?.gps
                if (g != null) {
                    val sunAlt = TargetPicker.sunAltAz(System.currentTimeMillis(), g.latitude, g.longitude).altitudeDeg
                    night = SunEvents.isNightForGuide(sunAlt)
                }
                delay(60_000)
            }
        }
        var unDim by remember { mutableStateOf(false) }
        // real screen dim when the tester turns it on at night
        val context = LocalContext.current
        LaunchedEffect(night, unDim) {
            val act = context as? Activity
            if (act != null) {
                runCatching {
                    val lp = act.window.attributes
                    lp.screenBrightness = if (night && !unDim) 0.05f else -1f
                    act.window.attributes = lp
                }
            }
        }

        val doc = controller.document
        val pending = doc.pending
        val level = pending?.level ?: doc.nextLevel

        var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
        LaunchedEffect(Unit) { while (true) { nowMs = System.currentTimeMillis(); delay(5_000) } }

        var collapsed by remember(doc.trialId) { mutableStateOf(pending == null && doc.nextLevel > 0) }
        var helpVisible by remember { mutableStateOf(false) }
        var detailsOpen by remember { mutableStateOf(false) }
        var skipRowOpen by remember { mutableStateOf(false) }
        var crosshair by remember(doc.trialId, level) { mutableStateOf<Offset?>(null) }

        val gps = frame?.gps
        val lat = gps?.latitude ?: 35.7
        val lon = gps?.longitude ?: 51.4
        val gating = remember(level, nowMs, lat, lon, rev) {
            FieldTrialMachine.gating(level, nowMs, lat, lon, trackerWired = true, l3Done = controller.document.isDone(3))
        }
        val target = remember(level, nowMs, lat, lon, rev, pending?.measurements?.size) {
            targetFor(level, nowMs, lat, lon, controller)
        }
        val pal = if (night && !unDim) nightPalette() else dayPalette()

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val canvasW = maxWidth.value
            val canvasH = maxHeight.value
            val cardMax: Dp = maxHeight * 0.18f // G-1.2: never touches the centre 60%

            // --- tap catcher: the ONLY full-screen input the guide adds (tap levels) ---
            if (level in tapLevels && gating.status == LevelStatus.AVAILABLE &&
                pending != null && !collapsed
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(level) { detectTapGestures(onTap = { off -> crosshair = off }) }
                )
            }

            // --- markers: thin rings only, no full-screen composable ---
            Canvas(Modifier.fillMaxSize()) {
                val f = frame ?: return@Canvas
                val intr = toInverseIntrinsics(f) ?: return@Canvas
                val affine = f.sensorToViewValues?.let {
                    doubleArrayOf(it[0].toDouble(), it[1].toDouble(), it[2].toDouble(), it[3].toDouble(), it[4].toDouble(), it[5].toDouble())
                }
                if (level in tapLevels && target != null) {
                    val px = InverseProjection.forwardProject(
                        target.azDeg, target.altDeg, f.rotationMatrix,
                        f.azimuthDeg, f.altitudeDeg, f.rollDeg,
                        size.width.toDouble(), size.height.toDouble(), intr,
                        f.zoomFactor.toDouble(), affine, f.displayRotationDegrees
                    )
                    px?.let {
                        drawCircle(
                            pal.ring, radius = 26f,
                            center = Offset(it.first.toFloat(), it.second.toFloat()),
                            style = Stroke(width = 2.5f)
                        )
                    }
                }
                if (level == 9) {
                    val q = access.orientationProvider.starTrackerAttitude
                    val radec = if (q != null && target != null) trackerRaDec(controller, target) else null
                    if (q != null && radec != null) {
                        TrackerProjector.project(
                            radec.first, radec.second, q,
                            size.width.toDouble(), size.height.toDouble(), intr,
                            f.zoomFactor.toDouble(), f.displayRotationDegrees
                        )?.let {
                            drawCircle(
                                pal.ringGreen, radius = 26f,
                                center = Offset(it.first.toFloat(), it.second.toFloat()),
                                style = Stroke(width = 2.5f)
                            )
                        }
                    }
                }
                crosshair?.let {
                    drawLine(pal.accent, it.copy(x = it.x - 16f), it.copy(x = it.x + 16f), 2f)
                    drawLine(pal.accent, it.copy(y = it.y - 16f), it.copy(y = it.y + 16f), 2f)
                }
            }

            // --- crosshair drag knob + Back/Confirm row (G-1.6) ---
            crosshair?.let { ch ->
                Box(
                    Modifier
                        .offset { IntOffset((ch.x - 28).roundToInt(), (ch.y - 28).roundToInt()) }
                        .size(56.dp)
                        .pointerInput(level) {
                            detectDragGestures { change, amount ->
                                change.consume()
                                val cur = crosshair ?: return@detectDragGestures
                                crosshair = Offset(
                                    (cur.x + amount.x).coerceIn(0f, canvasW),
                                    (cur.y + amount.y).coerceIn(0f, canvasH)
                                )
                            }
                        }
                )
                Row(
                    Modifier
                        .offset {
                            IntOffset(
                                ((ch.x - 90).roundToInt()).coerceIn(0, (canvasW - 260f).roundToInt()),
                                (ch.y + 26).roundToInt()
                            )
                        }
                        .background(pal.bg, RoundedCornerShape(10.dp))
                ) {
                    TextButton(onClick = { crosshair = null }) { Text("Back", color = pal.dim, fontSize = 16.sp) }
                    TextButton(onClick = {
                        val f = frame
                        if (f != null) {
                            onConfirmTap(controller, level, target, ch, f, canvasW, canvasH)
                        }
                        crosshair = null
                    }) {
                        Text("Confirm", color = pal.accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // --- the guide card (collapsed = one-line pill, G-1.2) ---
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .imePadding()
                    .fillMaxWidth()
            ) {
                Surface(
                    color = pal.bg,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                        .heightIn(max = cardMax)
                ) {
                    if (collapsed) {
                        Row(
                            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Level $level - ${levelCopy(level, target).title}", color = pal.fg, fontSize = 16.sp)
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { collapsed = false }) { Text("v", color = pal.accent, fontSize = 18.sp) }
                        }
                    } else {
                        Column(
                            Modifier
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Level $level - ${levelCopy(level, target).title}",
                                    color = pal.fg, fontSize = 17.sp, fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.weight(1f))
                                TextButton(onClick = { collapsed = true }) { Text("^", color = pal.dim, fontSize = 16.sp) }
                                TextButton(onClick = { helpVisible = true }) { Text("?", color = pal.accent, fontSize = 18.sp) }
                            }
                            Text(levelCopy(level, target).instruction, color = pal.fg, fontSize = 16.sp)

                            if (gating.status != LevelStatus.AVAILABLE) {
                                Text(gatingText(gating), color = pal.dim, fontSize = 15.sp)
                            }
                            if (level == 5) {
                                val below = TargetPicker.sevenStarsNow(nowMs, lat, lon).second
                                if (below.isNotEmpty()) {
                                    Text("Below the horizon tonight: ${below.joinToString()}", color = pal.dim, fontSize = 14.sp)
                                }
                            }

                            PartBWatchers(controller, level, pal)

                            if (detailsOpen) DetailsBody(level, controller, frame, target, pal)

                            if (skipRowOpen) {
                                Text("Why are you skipping?", color = pal.fg, fontSize = 15.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(onClick = { skipRowOpen = false; controller.skip(SkipReason.CLOUDS) }) {
                                        Text(SkipReason.CLOUDS.label, color = pal.fg, fontSize = 14.sp)
                                    }
                                    OutlinedButton(onClick = { skipRowOpen = false; controller.skip(SkipReason.CANT_FIND) }) {
                                        Text(SkipReason.CANT_FIND.label, color = pal.fg, fontSize = 14.sp)
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(onClick = { skipRowOpen = false; controller.skip(SkipReason.NO_TIME) }) {
                                        Text(SkipReason.NO_TIME.label, color = pal.fg, fontSize = 14.sp)
                                    }
                                    OutlinedButton(onClick = { skipRowOpen = false; controller.skip(SkipReason.OTHER) }) {
                                        Text(SkipReason.OTHER.label, color = pal.fg, fontSize = 14.sp)
                                    }
                                }
                            }

                            LevelButtons(
                                controller, level, target, gating, pal, gps,
                                onSkip = { skipRowOpen = !skipRowOpen },
                                onToggleDetails = { detailsOpen = !detailsOpen },
                                detailsOpen = detailsOpen,
                                nightDim = night && !unDim,
                                onToggleDim = { unDim = !unDim },
                                canDim = night
                            )
                        }
                    }
                }
            }

            // --- "?" help sheet (bottom, small; same state on return, G-1.4) ---
            if (helpVisible) {
                Surface(
                    color = pal.bg,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 110.dp)
                        .fillMaxWidth(0.94f)
                ) {
                    Column(
                        Modifier.padding(14.dp).heightIn(max = maxHeight * 0.30f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val copy = levelCopy(level, target)
                        Text(copy.helpTitle, color = pal.accent, fontSize = 18.sp)
                        Text(copy.helpBody, color = pal.fg, fontSize = 16.sp)
                        target?.hint?.let { hint ->
                            Canvas(Modifier.fillMaxWidth().height(90.dp)) {
                                for (line in hint.polylines) {
                                    for (i in 0 until line.size - 1) {
                                        drawLine(
                                            pal.dim,
                                            Offset(line[i].first * size.width, line[i].second * size.height),
                                            Offset(line[i + 1].first * size.width, line[i + 1].second * size.height),
                                            2f
                                        )
                                    }
                                }
                            }
                            Text(hint.title, color = pal.dim, fontSize = 14.sp)
                        }
                        if (level == 1) {
                            Text(
                                "If the Sun is behind clouds or too low, you can jump straight to the Moon level.",
                                color = pal.dim, fontSize = 14.sp
                            )
                            if (TargetPicker.moonTarget(nowMs, lat, lon) != null) {
                                Button(onClick = {
                                    helpVisible = false
                                    controller.skip(SkipReason.CANT_FIND)
                                    controller.open(2)
                                }) { Text("Go to the Moon level", fontSize = 16.sp) }
                            }
                        }
                        TextButton(onClick = { helpVisible = false }) { Text("Close", color = pal.dim, fontSize = 16.sp) }
                    }
                }
            }
        }
    }

    // ---------- Part B live logic (L8 lock, L10 cover, L11 sweep) ----------

    @Composable
    private fun PartBWatchers(controller: FieldTrialController, level: Int, pal: Palette) {
        when (level) {
            0 -> {
                val gps = FieldTrialHost.frame?.gps
                val acc = gps?.accuracy
                Text(
                    if (acc == null) "Waiting for a GPS fix..."
                    else "GPS accuracy: ${"%.0f".format(acc)} m ${if (acc < 20f) "(good)" else "(waiting for under 20 m)"}",
                    color = pal.dim, fontSize = 15.sp
                )
            }
            8 -> {
                var status by remember(controller.document.pending?.startedMs) { mutableStateOf("Tap Turn on to start.") }
                // G-3/L8: the 60 s run starts ONLY behind the "Turn on" button
                var started by remember(controller.document.pending?.startedMs) { mutableStateOf(false) }
                LaunchedEffect(started, controller.document.pending?.startedMs) {
                    if (!started) return@LaunchedEffect
                    if (controller.document.pending?.level != 8) return@LaunchedEffect
                    // fresh run: reset a tracker left on from an earlier level/session
                    if (StarTrackerRuntime.isOn.get()) {
                        controller.trackerTurnOff()
                        delay(600)
                    }
                    controller.trackerTurnOn { status = "Could not start: $it" }
                    val t0 = System.currentTimeMillis()
                    while (true) {
                        delay(400)
                        val probes = StarTrackerRuntime.state.probes
                        val last = probes.lastSample
                        val elapsed = System.currentTimeMillis() - t0
                        when {
                            probes.firstFullLockMs >= 0 -> {
                                val lastFull = probes.samples.lastOrNull { it.lock == LockConfidence.FULL_LOCK }
                                controller.addAuto("firstFullLockMs", Json.JNum(probes.firstFullLockMs.toDouble()))
                                controller.addAuto("fps", Json.JNum(probes.fps()))
                                lastFull?.let {
                                    controller.addAuto("detections", Json.JNum(it.detections.toDouble()))
                                    it.matched?.let { m -> controller.addAuto("matched", Json.JNum(m.toDouble())) }
                                    controller.addAuto("solveMs", Json.JNum(it.solveMs))
                                    it.acquisitionDiscrepancyDeg?.let { d ->
                                        controller.addAuto("acquisitionDiscrepancyDeg", Json.JNum(d))
                                    }
                                }
                                status = "Locked in ${"%.1f".format(probes.firstFullLockMs / 1000.0)} s"
                                controller.complete(LevelOutcome.PASS)
                                return@LaunchedEffect
                            }
                            elapsed > 60_000 -> {
                                val reason = com.alijafari.red.astronomy.fieldtrial.engine.FailureWording.sentence(last?.failure)
                                status = "No lock after 60 s. $reason"
                                controller.addAuto("timeoutReason", Json.JStr(reason))
                                return@LaunchedEffect
                            }
                            else -> status = if (last != null) "Stars seen: ${last.detections} - Locking..." else "Waiting for the camera..."
                        }
                    }
                }
                Text(status, color = pal.accent, fontSize = 16.sp)
                if (!started && controller.document.pending?.auto?.containsKey("timeoutReason") != true) {
                    Button(onClick = { started = true }) { Text("Turn on", fontSize = 16.sp) }
                }
                if (controller.document.pending?.auto?.containsKey("timeoutReason") == true) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            controller.trackerTurnOff()
                            controller.open(8)
                        }) { Text("Retry", fontSize = 16.sp) }
                        Button(onClick = { controller.complete(LevelOutcome.FAIL) }) { Text("Give up", fontSize = 16.sp) }
                    }
                }
            }
            10 -> {
                val subSteps = listOf("Cover the lens with your hand", "Point at a lit wall or streetlight", "Point at the ground")
                var step by remember(controller.document.pending?.startedMs) { mutableStateOf(0) }
                var remaining by remember { mutableStateOf(20) }
                LaunchedEffect(controller.document.pending?.startedMs) {
                    if (controller.document.pending?.level != 10) return@LaunchedEffect
                    controller.trackerTurnOn { }
                    var s = 0
                    while (s < 3) {
                        step = s
                        remaining = 20
                        val t0 = System.currentTimeMillis()
                        var failed = false
                        while (System.currentTimeMillis() - t0 < 20_000) {
                            delay(400)
                            val lock = StarTrackerRuntime.state.probes.lastSample?.lock ?: LockConfidence.NO_LOCK
                            if (lock != LockConfidence.NO_LOCK) {
                                StarTrackerRuntime.requestCapture(1)
                                controller.addAuto("allNoLock", Json.JBool(false))
                                controller.addAuto("brokeAtSubStep", Json.JNum((s + 1).toDouble()))
                                controller.complete(LevelOutcome.FAIL, note = "claimed stars while covered (sub-step ${s + 1})")
                                failed = true
                                break
                            }
                            remaining = 20 - ((System.currentTimeMillis() - t0) / 1000).toInt()
                        }
                        if (failed) return@LaunchedEffect
                        s++
                    }
                    controller.addAuto("allNoLock", Json.JBool(true))
                    controller.complete(LevelOutcome.PASS)
                }
                Text(
                    if (step < 3) "${subSteps[step]} - ${remaining.coerceAtLeast(0)} s" else "Done",
                    color = pal.accent, fontSize = 16.sp
                )
            }
            11 -> {
                var status by remember { mutableStateOf("Sweeping...") }
                LaunchedEffect(controller.document.pending?.startedMs) {
                    if (controller.document.pending?.level != 11) return@LaunchedEffect
                    controller.trackerTurnOn { }
                    val t0 = System.currentTimeMillis()
                    val samples = ArrayList<StarTrackerRuntime.ProbeSample>()
                    while (System.currentTimeMillis() - t0 < 30_000) {
                        delay(200)
                        StarTrackerRuntime.state.probes.lastSample?.let { samples.add(it) }
                    }
                    val fl = PartBAnalysisAdapter.falseLocks(samples)
                    val rl = PartBAnalysisAdapter.relocks(samples)
                    samples.mapNotNull { it.acquisitionDiscrepancyDeg }.maxOrNull()?.let {
                        controller.addAuto("maxJumpDeg", Json.JNum(it))
                    }
                    controller.addAuto("falseLocks", Json.JNum(fl.toDouble()))
                    controller.addAuto("relocks", Json.JNum(rl.toDouble()))
                    controller.complete(if (fl == 0) LevelOutcome.PASS else LevelOutcome.FAIL)
                    status = "Done: $fl false locks, $rl re-locks."
                }
                Text(status, color = pal.accent, fontSize = 16.sp)
            }
            12 -> LaunchedEffect(controller.document.trialId) {
                // G-3/L12: tracker goes OFF automatically at the summary
                if (StarTrackerRuntime.isOn.get()) controller.trackerTurnOff()
            }
        }
    }

    // ---------- primary buttons (<= 3 per level, G-1.4) ----------

    @Composable
    private fun LevelButtons(
        controller: FieldTrialController,
        level: Int,
        target: TargetPicker.SkyTarget?,
        gating: Gating,
        pal: Palette,
        gps: Location?,
        onSkip: () -> Unit,
        onToggleDetails: () -> Unit,
        detailsOpen: Boolean,
        nightDim: Boolean,
        onToggleDim: () -> Unit,
        canDim: Boolean
    ) {
        val context = LocalContext.current
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            when (level) {
                0 -> Button(
                    enabled = gps != null && gps.accuracy < 20f,
                    onClick = { controller.complete(LevelOutcome.PASS) }
                ) { Text("I'm ready", fontSize = 16.sp) }
                1, 2, 3, 4, 5 -> {
                    val frame = FieldTrialHost.frame
                    val needed = if (level == 5) {
                        val g = frame?.gps
                        if (g != null) TargetPicker.sevenStarsNow(System.currentTimeMillis(), g.latitude, g.longitude).first.size else 1
                    } else 1
                    val have = controller.document.pending?.measurements?.size ?: 0
                    if (have >= needed && gating.status == LevelStatus.AVAILABLE) {
                        Button(onClick = { controller.complete(controller.evaluateOpen()) }) {
                            Text(if (level == 5) "Done (all stars)" else "Done", fontSize = 16.sp)
                        }
                    } else {
                        Text(
                            if (level == 5) "Tap ${have + 1} of $needed" else "Tap the ${target?.name ?: "target"}",
                            color = pal.dim, fontSize = 15.sp
                        )
                    }
                }
                6 -> {
                    Button(onClick = { controller.addYesNo(true); controller.complete(LevelOutcome.PASS) }) {
                        Text("Yes, I see it", fontSize = 16.sp)
                    }
                    OutlinedButton(onClick = { controller.addYesNo(false); controller.complete(LevelOutcome.FAIL) }) {
                        Text("No", fontSize = 16.sp)
                    }
                }
                7 -> {
                    Button(onClick = { controller.addYesNo(true); controller.complete(LevelOutcome.PASS) }) {
                        Text("Yes", fontSize = 16.sp)
                    }
                    OutlinedButton(onClick = { controller.addYesNo(false); controller.complete(LevelOutcome.FAIL) }) {
                        Text("No", fontSize = 16.sp)
                    }
                }
                8 -> {
                    // primary "Turn on" lives in the L8 watcher above (starts the 60 s run)
                }
                12 -> {
                    Button(onClick = {
                        val intent = controller.shareIntent()
                        if (intent != null) {
                            context.startActivity(Intent.createChooser(intent, "Share field trial results"))
                        }
                    }) { Text("Share results", fontSize = 16.sp) }
                    Button(onClick = {
                        controller.trackerTurnOff()
                        controller.newDocument()
                        controller.open(0)
                    }) { Text("Start a new trial", fontSize = 16.sp) }
                }
            }
            if (level != 0 && level != 12) {
                OutlinedButton(onClick = onSkip) { Text("Skip", color = pal.dim, fontSize = 16.sp) }
            }
            TextButton(onClick = onToggleDetails) {
                Text(if (detailsOpen) "Details ^" else "Details v", color = pal.dim, fontSize = 15.sp)
            }
            if (canDim) {
                TextButton(onClick = onToggleDim) {
                    Text(if (nightDim) "Brighten" else "Dim screen", color = pal.dim, fontSize = 15.sp)
                }
            }
        }
    }

    // ---------- Details (all numbers live behind the chevron, G-1.4) ----------

    @Composable
    private fun DetailsBody(
        level: Int,
        controller: FieldTrialController,
        frame: FieldTrialHost.FrameState?,
        target: TargetPicker.SkyTarget?,
        pal: Palette
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            target?.let {
                DetailRow("Target", "${it.name}${it.magnitude?.let { m -> " (mag ${"%.1f".format(m)})" } ?: ""}", pal)
                DetailRow("Computed place", "az ${"%.1f".format(it.azDeg)} deg, alt ${"%.1f".format(it.altDeg)} deg", pal)
            }
            frame?.let {
                DetailRow(
                    "Phone attitude",
                    "az ${"%.1f".format(it.azimuthDeg)}, alt ${"%.1f".format(it.altitudeDeg)}, roll ${"%.1f".format(it.rollDeg)}",
                    pal
                )
                DetailRow("Camera tier", it.intrinsics?.source?.name ?: "unknown", pal)
            }
            FieldTrialHost.access()?.let { acc ->
                DetailRow("Compass correction", "${"%.1f".format(acc.orientationProvider.appliedDeclinationDeg)} deg applied", pal)
            }
            val gps = frame?.gps
            DetailRow(
                "GPS",
                if (gps == null) "no fix yet"
                else "${"%.4f".format(gps.latitude)}, ${"%.4f".format(gps.longitude)} (+-${"%.0f".format(gps.accuracy)} m)",
                pal
            )
            DetailRow("Date", SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()), pal)
            if (level in intArrayOf(1, 2, 3, 4, 5, 9)) {
                controller.document.pending?.measurements?.forEachIndexed { i, m ->
                    DetailRow("Tap ${i + 1}", "${m.targetId}: ${"%.2f".format(m.separationDeg)} deg off", pal)
                }
            }
            if (level >= 8) {
                val p = StarTrackerRuntime.state.probes
                DetailRow("Stars seen", p.lastSample?.detections?.toString() ?: "-", pal)
                DetailRow("Frames", p.framesProcessed.toString(), pal)
                DetailRow("Tracker", if (StarTrackerRuntime.isOn.get()) "on" else "off", pal)
            }
            if (level == 12) {
                for ((lvl, outcome) in controller.document.ticks()) {
                    DetailRow("Level $lvl", outcome.name, pal)
                }
            }
        }
    }

    @Composable
    private fun DetailRow(k: String, v: String, pal: Palette) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(k, color = pal.dim, fontSize = 14.sp)
            Spacer(Modifier.weight(1f))
            Text(v, color = pal.fg, fontSize = 14.sp)
        }
    }

    // ---------- helpers ----------

    fun toInverseIntrinsics(f: FieldTrialHost.FrameState): InverseProjection.Intrinsics? {
        val i = f.intrinsics ?: return null
        return InverseProjection.Intrinsics(
            fx = i.fx, fy = i.fy, cx = i.cx, cy = i.cy, skew = i.skew,
            activeArrayWidth = i.activeArrayWidth, activeArrayHeight = i.activeArrayHeight,
            sensorOrientation = i.sensorOrientation
        )
    }

    fun gatingText(g: Gating): String {
        val base = g.reason ?: "Not available right now."
        val whenText = g.whenUtcMs?.let {
            val kind = g.whenKind ?: "Available again"
            " $kind around ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it))}."
        } ?: ""
        return base + whenText
    }

    fun targetFor(
        level: Int, nowMs: Long, lat: Double, lon: Double,
        controller: FieldTrialController
    ): TargetPicker.SkyTarget? {
        val g = FieldTrialHost.frame?.gps
        val la = g?.latitude ?: lat
        val lo = g?.longitude ?: lon
        return when (level) {
            1 -> {
                val h = TargetPicker.sunAltAz(nowMs, la, lo)
                TargetPicker.SkyTarget(
                    id = "sun", name = "Sun", kind = TargetPicker.Kind.SUN,
                    azDeg = h.azimuthDeg, altDeg = h.altitudeDeg, magnitude = null,
                    howToFind = "The bright one - never stare at the Sun itself."
                )
            }
            2 -> TargetPicker.l2Alternative(nowMs, la, lo)
            9 -> {
                val l3 = controller.document.latest(3)?.measurements?.lastOrNull()?.targetId
                val bright = TargetPicker.brightestStarNow(nowMs, la, lo)
                if (l3 != null && bright != null && bright.id != l3) {
                    TargetPicker.sevenStarsNow(nowMs, la, lo).first.firstOrNull { it.id == l3 } ?: bright
                } else bright
            }
            3, 4 -> TargetPicker.brightestStarNow(nowMs, la, lo)
            5 -> TargetPicker.sevenStarsNow(nowMs, la, lo)
                .first.getOrNull(controller.document.pending?.measurements?.size ?: 0)
            else -> null
        }
    }

    private fun trackerRaDec(controller: FieldTrialController, target: TargetPicker.SkyTarget): Pair<Double, Double>? =
        TrackerProjector.catalogRaDecOf(StarTrackerRuntime.state.catalogStars, target.id)

    /** G-1.8: Confirm builds the TapMeasurement (same frame), saves it, captures evidence. */
    fun onConfirmTap(
        controller: FieldTrialController,
        level: Int,
        target: TargetPicker.SkyTarget?,
        tap: Offset,
        f: FieldTrialHost.FrameState,
        canvasW: Float,
        canvasH: Float
    ) {
        val t = target ?: return
        val intr = toInverseIntrinsics(f) ?: return
        val affine = f.sensorToViewValues?.let {
            doubleArrayOf(it[0].toDouble(), it[1].toDouble(), it[2].toDouble(), it[3].toDouble(), it[4].toDouble(), it[5].toDouble())
        }
        val sky = InverseProjection.inverseProject(
            tap.x.toDouble(), tap.y.toDouble(), f.rotationMatrix,
            f.azimuthDeg, f.altitudeDeg, f.rollDeg, canvasW.toDouble(), canvasH.toDouble(),
            intr, f.zoomFactor.toDouble(), affine, f.displayRotationDegrees
        ) ?: return
        val markerPx = InverseProjection.forwardProject(
            t.azDeg, t.altDeg, f.rotationMatrix, f.azimuthDeg, f.altitudeDeg, f.rollDeg,
            canvasW.toDouble(), canvasH.toDouble(), intr, f.zoomFactor.toDouble(), affine, f.displayRotationDegrees
        )
        val access = FieldTrialHost.access()
        val gps = f.gps
        val m = TapMeasurement.of(
            epochMs = System.currentTimeMillis(),
            targetId = t.id,
            computedAzDeg = t.azDeg, computedAltDeg = t.altDeg,
            tappedAzDeg = sky.first, tappedAltDeg = sky.second,
            targetPx = markerPx?.first ?: tap.x.toDouble(), targetPy = markerPx?.second ?: tap.y.toDouble(),
            tapPx = tap.x.toDouble(), tapPy = tap.y.toDouble(),
            sensorAzimuthDeg = f.azimuthDeg, sensorAltitudeDeg = f.altitudeDeg, sensorRollDeg = f.rollDeg,
            sensorRotationMatrix = f.rotationMatrix,
            gpsLat = gps?.latitude, gpsLon = gps?.longitude, gpsAccuracyM = gps?.accuracy?.toDouble(),
            intrinsicsTier = f.intrinsics?.source?.name ?: "?",
            fx = f.intrinsics?.fx, fy = f.intrinsics?.fy, cx = f.intrinsics?.cx, cy = f.intrinsics?.cy,
            distortionTier = "device", k1 = null, k2 = null,
            appliedDeclinationDeg = access?.orientationProvider?.appliedDeclinationDeg?.toDouble() ?: 0.0,
            zoomFactor = f.zoomFactor.toDouble(),
            displayRotationDegrees = f.displayRotationDegrees
        )
        controller.addMeasurement(m)

        // L9: record both ring errors (blue = compass, green = tracker)
        if (level == 9) {
            controller.addAuto("blueSeparationDeg", Json.JNum(m.separationDeg))
            val q = access?.orientationProvider?.starTrackerAttitude
            val radec = q?.let { trackerRaDec(controller, t) }
            if (q != null && radec != null) {
                val gp = TrackerProjector.project(
                    radec.first, radec.second, q, canvasW.toDouble(), canvasH.toDouble(), intr,
                    f.zoomFactor.toDouble(), f.displayRotationDegrees
                )
                if (gp != null && markerPx != null) {
                    val dpx = kotlin.math.hypot(gp.first - markerPx.first, gp.second - markerPx.second)
                    controller.addAuto(
                        "greenSeparationDeg",
                        Json.JNum(Capture.pxToDeg(dpx, intr, canvasW.toDouble(), canvasH.toDouble(), f.zoomFactor.toDouble()))
                    )
                }
            }
        }

        // G-1.8 evidence: real screenshot of the window (card hidden state) else marker scene
        val activity = access?.context as? Activity
        val real = Capture.pixelCopyShot(activity, canvasW.roundToInt(), canvasH.roundToInt())
        val shot = real ?: Capture.markerSceneShot(
            Capture.MarkerScene(
                widthPx = canvasW.roundToInt(), heightPx = canvasH.roundToInt(),
                targetPx = markerPx?.let { Pair(it.first.toFloat(), it.second.toFloat()) },
                tapPx = Pair(tap.x, tap.y),
                drawArrow = true,
                label = "L$level ${t.name} ${"%.2f".format(m.separationDeg)}deg"
            )
        )
        controller.addShot("L$level", shot)
    }
}

package com.zig.gravity.sim

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.zig.gravity.edu.Challenge
import com.zig.gravity.edu.ChallengeRunner
import com.zig.gravity.edu.TeachingCatalog
import com.zig.gravity.edu.TeachingTier
import com.zig.gravity.edu.detectors.SimulationDetectors
import com.zig.gravity.physics.BodyType
import com.zig.gravity.physics.Collision
import com.zig.gravity.physics.EngineConstants
import com.zig.gravity.physics.ImpactTier
import com.zig.gravity.physics.NBodyEngine
import com.zig.gravity.physics.Predictor
import com.zig.gravity.physics.SimArrays
import com.zig.gravity.physics.SimEvent
import com.zig.gravity.physics.Wormhole
import com.zig.gravity.util.SandboxFormat
import kotlin.math.hypot
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * §3.3 `sim/SimulationViewModel` — frame loop, accumulator, intents, snapshot publishing.
 *
 * Threading: everything here runs single-threaded on the main dispatcher inside `withFrameNanos`
 * (locked decision 9). No physics ever touches a background thread.
 *
 * Recomposition budget: the per-frame signal is [frameTick], an Int state that the canvas reads
 * **inside its draw lambda only**. Nothing else changes per frame, so steady-state recomposition
 * per frame is zero (locked decision 8, §3.15).
 */
class SimulationViewModel : ViewModel() {

    // ---- engine state ---------------------------------------------------------------------------
    val arrays = SimArrays()
    private val initialState = SimArrays()
    private val events = ArrayList<SimEvent>(32)
    private val substepBudget = IntArray(1)

    val snapshot = SimSnapshot()

    /** §1 camera. Plain state, mutated by the gesture layer, read in the draw phase. */
    val camera = CameraState()

    /**
     * §1/§26 — the camera pose this preset started in.
     *
     * Reset restores this recorded pose rather than re-deriving a framing, so "reset" genuinely
     * means "put the experiment back the way it started", camera included. It is captured once,
     * when the preset is built, and again whenever the user explicitly re-frames for the preset.
     */
    private var initialCameraPose: CameraPose = camera.snapshot()

    /**
     * Bumped on every camera mutation the UI needs to see. [camera] is deliberately a plain class
     * (it is read in the draw phase and by JVM tests), which means a composable reading
     * `camera.tiltRad` would never recompose. Panels that display camera values read this instead.
     */
    var cameraTick by mutableStateOf(0)
        private set

    // ---- §7-§15 follow --------------------------------------------------------------------------

    /** Id of the body the camera is following, or 0 when Follow is off. */
    var followId: Long = 0L
        private set

    /** Compose-visible mirror of [followId], so the HUD and inspector can react to it. */
    var followTargetId by mutableStateOf(0L)
        private set

    /** True for the first frames after acquiring a target, while the camera glides onto it. */
    private var followAcquiring = false

    /** §13 pooled impact effects. Bounded, elapsed-time driven, never accumulates. */
    val effects = EffectPool()

    /**
     * §14 — one entry per *collision event*, drained by the UI once per frame and then cleared.
     * The physics layer never touches Android, so it publishes intent and the composable vibrates.
     */
    val pendingHaptics = ArrayList<HapticCue>(4)

    /** Viewport in pixels, needed by the camera and by hit testing. */
    var viewportWidthPx: Float = 0f
        private set
    var viewportHeightPx: Float = 0f
        private set

    /** Draw-phase invalidation signal. Read ONLY inside the Canvas draw lambda. */
    var frameTick by mutableIntStateOf(0)
        private set

    /**
     * Bumped only when the *visual set* changes (body added/removed/resized, preset, viewport).
     * Brush, shadow and label caches key off this, so they are rebuilt on mutation and never
     * per frame.
     */
    var visualEpoch by mutableIntStateOf(0)
        private set

    // ---- session state (changes rarely -> recomposition is rare) ----------------------------------
    var preset by mutableStateOf(Preset.DEFAULT)
        private set
    var paused by mutableStateOf(false)
        private set
    private var _speedIndex by mutableIntStateOf(EngineConstants.DEFAULT_SPEED_INDEX)

    /** Index into [EngineConstants.SPEEDS]. Observable read; mutate through [setSpeedIndex]. */
    val speedIndex: Int get() = _speedIndex
    var trailsVisible by mutableStateOf(true)
        private set
    var teachingEnabled by mutableStateOf(true)
        private set
    var darkTheme by mutableStateOf(true)
        private set
    var persian by mutableStateOf(true)
        private set
    private var _marbleBounce by mutableStateOf(false)

    /** §3.7 marble-vs-marble elastic bounce. Observable read; mutate through [setMarbleBounce]. */
    val marbleBounce: Boolean get() = _marbleBounce
    var showBarycenter by mutableStateOf(true)
        private set
    var showVectors by mutableStateOf(true)
        private set
    var selectedId by mutableStateOf(0L)
        private set
    var notice by mutableStateOf<String?>(null)
        private set

    // ---- teaching -----------------------------------------------------------------------------
    private val detectors = SimulationDetectors()
    val challengeRunner = ChallengeRunner()
    var teachingConcept by mutableStateOf<String?>(null)
        private set
    private var _teachingTier by mutableStateOf(TeachingTier.WHAT)

    /** §3.11 teaching depth of the visible card. Observable read; mutate through [setTeachingTier]. */
    val teachingTier: TeachingTier get() = _teachingTier
    var challengeResultOptionId by mutableStateOf<String?>(null)
        private set
    var activeChallenge by mutableStateOf<Challenge?>(null)
        private set
    var challengePrediction by mutableStateOf<String?>(null)
        private set

    // ---- interaction ----------------------------------------------------------------------------
    var draggingId by mutableStateOf(0L)
        private set
    var slingshotArmedId by mutableStateOf(0L)
        private set

    /** Drag vector in scene metres while a slingshot is being aimed. */
    private var slingDx = 0.0
    private var slingDy = 0.0
    var slingshotActive by mutableStateOf(false)
        private set

    /** Launch velocity the aim currently represents. Plain fields: read in the draw phase. */
    var slingshotVx: Double = 0.0
        private set
    var slingshotVy: Double = 0.0
        private set

    /**
     * State held across a drag so the release can restore it exactly (§2). A drag is allowed to
     * change the position and nothing else, so everything else the body owns is stashed here.
     */
    private var dragHeldVx = 0.0
    private var dragHeldVy = 0.0
    private var dragHeldMass = 0.0
    var dragOriginX: Double = 0.0
        private set
    var dragOriginY: Double = 0.0
        private set

    // ---- prediction --------------------------------------------------------------------------
    private val maxPredictionSamples = EngineConstants.PREDICTION_STEPS / Predictor.SAMPLE_STRIDE + 2
    val predictionXY = DoubleArray(maxPredictionSamples * 2)
    var predictionCount: Int = 0
        private set
    var predictionApproximate: Boolean = false
        private set

    /**
     * §13/§14 — true while the displayed prediction is a *preview* of a drag rather than the
     * committed path. The renderer draws it in the ghost style, and the origin marker with it.
     */
    var predictionIsGhost: Boolean = false
        private set

    /** §14 — the previewed path leaves the neighbourhood instead of coming back around. */
    var predictionEscapes: Boolean = false
        private set

    /** §15 — headline/detail for the last meaningful impact, with the real numbers in them. */
    var impactHeadlineFa by mutableStateOf<String?>(null)
        private set
    var impactHeadlineEn by mutableStateOf<String?>(null)
        private set
    var impactDetailFa by mutableStateOf<String?>(null)
        private set
    var impactDetailEn by mutableStateOf<String?>(null)
        private set

    private var predictionDirty = true

    // ---- frame loop bookkeeping -------------------------------------------------------------
    private var lastFrameNanos = 0L
    private var accumulator = 0.0
    private var frameCounter = 0
    private var noticeFramesLeft = 0
    private var lastVisualSignature = 0
    private val scratch2 = DoubleArray(2)

    /** Live diagnostic for the perf overlay: inner steps consumed by the previous frame. */
    var lastFrameSubsteps: Int = 0
        private set

    val speed: Double get() = EngineConstants.SPEEDS[speedIndex]

    /** True once the screen has tried to restore a persisted session (survives rotation). */
    var restoreAttempted: Boolean = false
        private set

    fun markRestoreAttempted() {
        restoreAttempted = true
    }

    /** Seeds language/theme from the host app the first time the sandbox opens. */
    /**
     * Pushes the host application's language into the sandbox. Called on every entry, before and
     * after any restore, so the sandbox can never disagree with the rest of ZIG.
     */
    /**
     * §5 — the single entry point for the sandbox's language.
     *
     * There is no language selector in the sandbox and no second locale system: the host app's
     * locale is the only source of truth, and it is re-applied on every entry and on every host
     * locale change, including for restored sessions.
     */
    fun applyHostLanguage(persianFromApp: Boolean) {
        if (persian != persianFromApp) persian = persianFromApp
    }

    fun applyHostDefaults(persian: Boolean, dark: Boolean) {
        this.persian = persian
        this.darkTheme = dark
    }

    /**
     * Called when the screen becomes RESUMED. Dropping the previous frame timestamp guarantees
     * that time spent in the background is never integrated (§L lifecycle, manual checklist item
     * "backgrounding pauses").
     */
    fun onLifecycleResumed() {
        lastFrameNanos = 0L
    }

    init {
        Presets.build(preset, arrays)
        arrays.copyInto(initialState)
        snapshot.captureFrom(arrays)
        frameCameraForPreset(preset)
    }

    // ==== viewport =================================================================================

    /**
     * §3.2 scene scale: the viewport is always 3 AU wide, so metersPerDp is derived from the live
     * width and never stored as a constant. There is no zoom in v1 (§3.4).
     */
    fun onViewportSizePx(widthPx: Float, heightPx: Float) {
        viewportWidthPx = widthPx
        viewportHeightPx = heightPx
    }

    fun onViewportChanged(widthDp: Double) {
        val mpd = EngineConstants.metersPerDp(widthDp)
        if (mpd == arrays.metersPerDp) return
        val rebuildPreset = arrays.simTime == 0.0
        arrays.setMetersPerDp(mpd)
        initialState.setMetersPerDp(mpd)
        if (rebuildPreset) {
            // Nothing has been simulated yet, so re-lay the preset at the true viewport width.
            Presets.build(preset, arrays)
            arrays.copyInto(initialState)
            // The layout the camera was framed against has just changed underneath it.
            frameCameraForPreset(preset)
        }
        NBodyEngine.computeAccelerations(arrays)
        visualEpoch++
        markDirty()
    }

    // ==== frame loop ================================================================================

    /** §3.6b accumulator. Speed multiplies BASE; DT is never scaled. */
    fun onFrame(frameTimeNanos: Long) {
        val dtReal = if (lastFrameNanos == 0L) 0.0 else (frameTimeNanos - lastFrameNanos) / 1.0e9
        lastFrameNanos = frameTimeNanos

        var advanced = false
        var steps = 0
        if (!paused && dtReal > 0.0) {
            accumulator += min(dtReal, EngineConstants.MAX_FRAME_SECONDS) * EngineConstants.BASE * speed
            substepBudget[0] = EngineConstants.MAX_SUBSTEPS
            NBodyEngine.ensureAccelerations(arrays)
            while (accumulator >= EngineConstants.DT && substepBudget[0] > 0) {
                val used = NBodyEngine.advance(arrays, EngineConstants.DT, events, marbleBounce, substepBudget)
                if (used == 0) break
                accumulator -= EngineConstants.DT
                steps += used
                advanced = true
            }
            // Never spiral: discard the debt once the budget is spent (§3.6b).
            if (substepBudget[0] <= 0) accumulator = 0.0
            if (advanced) arrays.pushTrailSample()
        }
        lastFrameSubsteps = steps
        effects.update(dtReal)
        // §9 — the follow camera is smoothed in wall-clock time, after the physics has advanced
        // and before the snapshot is taken, so the drawn frame is already correctly framed.
        updateFollow(dtReal)

        snapshot.captureFrom(arrays)

        // A merge, a quarantine or a size change during simulation alters the visual set; the
        // brush/label caches must be rebuilt exactly then, and never per frame.
        val sig = snapshot.visualSignature()
        if (sig != lastVisualSignature) {
            lastVisualSignature = sig
            visualEpoch++
        }

        if (events.isNotEmpty()) {
            handleIntegrityEvents()
            spawnEffectsFor(events)
        }
        if (teachingEnabled) {
            val detection = detectors.observe(snapshot, events, System.currentTimeMillis())
            if (detection != null && TeachingCatalog.card(detection.concept) != null) {
                teachingConcept = detection.concept
                _teachingTier = TeachingTier.WHAT
            }
        }
        val resolved = challengeRunner.observe(snapshot, events, detectors)
        if (resolved != null) {
            challengeResultOptionId = resolved
        }
        events.clear()

        frameCounter++
        if (predictionDirty || (!paused && frameCounter % 6 == 0)) {
            recomputePrediction()
        }

        if (noticeFramesLeft > 0) {
            noticeFramesLeft--
            if (noticeFramesLeft == 0) notice = null
        }

        if (advanced || predictionDirty || !paused || dragging()) {
            frameTick++
        }
        predictionDirty = false
    }

    private fun dragging(): Boolean = draggingId != 0L || slingshotActive

    /**
     * §13/§14/§15 — turns physics events into one visual effect and at most one haptic cue each.
     *
     * Exactly one effect is emitted per event, and the event list is cleared every frame, so a
     * pair of bodies resting in contact can never produce a stream of flashes or a buzzing device.
     */
    private fun spawnEffectsFor(list: List<SimEvent>) {
        for (e in list) {
            when (e) {
                is SimEvent.CollisionImpact -> {
                    val severity = ImpactTier.severity(e.relativeSpeed, e.mutualEscapeSpeed)
                    val kind = when {
                        !e.merged -> EffectKind.BOUNCE
                        e.tier == ImpactTier.HIGH -> EffectKind.SHATTER
                        else -> EffectKind.MERGE
                    }
                    val slot = arrays.slotOfId(e.aId).let { if (it >= 0) it else arrays.slotOfId(e.bId) }
                    val tint = if (slot >= 0) {
                        BodyCatalog.colorOf(arrays.catalogKey[slot], arrays.typeOf(slot))
                    } else {
                        0xFFBFC7D5
                    }
                    effects.spawn(kind, e.x, e.y, severity, e.contactRadius, tint)
                    pendingHaptics.add(
                        when (e.tier) {
                            ImpactTier.LOW -> HapticCue.LIGHT
                            ImpactTier.MODERATE -> HapticCue.MEDIUM
                            ImpactTier.HIGH -> HapticCue.HEAVY
                        }
                    )
                    describeImpact(e)
                    if (teachingEnabled) {
                        teachingConcept = SimulationDetectors.IMPACT_ENERGY
                        _teachingTier = TeachingTier.WHAT
                    }
                }

                is SimEvent.BlackHoleCapture -> {
                    // §15: matter is being captured, so the debris spirals in. The capture event
                    // already implies a merge event; the accretion effect replaces its burst.
                    val hole = arrays.slotOfId(e.holeId)
                    val x = if (hole >= 0) arrays.x[hole] else 0.0
                    val y = if (hole >= 0) arrays.y[hole] else 0.0
                    val scale = if (hole >= 0) arrays.radius[hole] else arrays.metersPerDp * 14.0
                    effects.spawn(EffectKind.ACCRETION, x, y, 1.0, scale, 0xFF6E7C99)
                    pendingHaptics.add(HapticCue.HEAVY)
                }

                is SimEvent.WormholeTraversal -> {
                    val slot = arrays.slotOfId(e.toMouthId)
                    if (slot >= 0) {
                        effects.spawn(
                            EffectKind.TRAVERSAL, arrays.x[slot], arrays.y[slot],
                            0.4, arrays.radius[slot], BodyCatalog.WORMHOLE.colorArgb
                        )
                    }
                    pendingHaptics.add(HapticCue.LIGHT)
                }

                else -> Unit
            }
        }
    }

    /**
     * §15 — turns a real collision into a short, honest sentence with the real numbers in it.
     *
     * The headline is the classification the physics actually produced; the detail carries the
     * relative speed and the centre-of-mass impact energy. Nothing here is canned: a gentle touch
     * never reads as a catastrophe.
     */
    private fun describeImpact(e: SimEvent.CollisionImpact) {
        val speedFa = SandboxFormat.speed(e.relativeSpeed, true)
        val speedEn = SandboxFormat.speed(e.relativeSpeed, false)
        val energyFa = SandboxFormat.joules(e.impactEnergyJ, true)
        val energyEn = SandboxFormat.joules(e.impactEnergyJ, false)

        when (e.tier) {
            ImpactTier.LOW -> {
                impactHeadlineFa = "برخورد آرام"
                impactHeadlineEn = "Soft collision"
                impactDetailFa = "سرعت نسبی: $speedFa · انرژی برخورد: $energyFa\n" +
                        "دو جسم نسبت به هم کند حرکت می‌کردند، پس گرانش خودشان توانست آن‌ها را کنار هم نگه دارد."
                impactDetailEn = "Relative speed: $speedEn · Impact energy: $energyEn\n" +
                        "They were moving slowly relative to each other, so their own gravity was able to hold them together."
            }
            ImpactTier.MODERATE -> {
                impactHeadlineFa = "برخورد متوسط"
                impactHeadlineEn = "Moderate collision"
                impactDetailFa = "سرعت نسبی: $speedFa · انرژی برخورد: $energyFa\n" +
                        "سرعت نزدیک‌شدن از سرعت گریز این جفت بیشتر بود، پس برخورد محسوس است اما ویرانگر نیست."
                impactDetailEn = "Relative speed: $speedEn · Impact energy: $energyEn\n" +
                        "The closing speed was above this pair's escape speed, so the impact is noticeable but not destructive."
            }
            ImpactTier.HIGH -> {
                impactHeadlineFa = "برخورد پرانرژی"
                impactHeadlineEn = "High-energy collision"
                impactDetailFa = "سرعت نسبی: $speedFa · انرژی برخورد: $energyFa\n" +
                        "دو جسم خیلی سریع به هم رسیدند. انرژی جنبشی نسبی‌شان چند برابر چیزی بود که گرانش می‌توانست مهار کند."
                impactDetailEn = "Relative speed: $speedEn · Impact energy: $energyEn\n" +
                        "They met very fast. Their relative kinetic energy was several times more than their gravity could contain."
            }
        }
    }

    /** Called by the UI after it has played the queued cues. */
    fun clearHaptics() {
        if (pendingHaptics.isNotEmpty()) pendingHaptics.clear()
    }

    private fun handleIntegrityEvents() {
        for (e in events) {
            // §14 — if the followed body was absorbed, follow the body that absorbed it. The
            // survivor genuinely contains the mass we were watching, so transferring is the
            // honest answer; anything else silently drops the user out of Follow mid-experiment.
            if (e is SimEvent.BodyMerged && followId == e.absorbedId) {
                followId = e.survivorId
                followTargetId = e.survivorId
                followAcquiring = true
            }
            // A body that fell into a black hole or left through a wormhole is simply gone.
            if (e is SimEvent.BlackHoleCapture && followId == e.capturedId) stopFollow()
            if (e is SimEvent.NumericalFailure && e.consecutiveFailures >= NBodyEngine.FAILURE_LIMIT) {
                paused = true
                showNotice(
                    if (persian) "محاسبه ناپایدار شد؛ شبیه‌سازی موقتاً متوقف شد."
                    else "The maths went unstable, so the simulation paused."
                )
            }
        }
    }

    // ==== HUD intents ================================================================================

    fun togglePlay() {
        paused = !paused
        // Resuming must not integrate the whole time spent paused (§3.6b: pause = stop accruing).
        lastFrameNanos = 0L
        accumulator = 0.0
        markDirty()
    }

    fun setSpeedIndex(index: Int) {
        _speedIndex = index.coerceIn(0, EngineConstants.SPEEDS.size - 1)
        markDirty()
    }

    fun toggleTrails() {
        trailsVisible = !trailsVisible
        markDirty()
    }

    fun toggleTeaching() {
        teachingEnabled = !teachingEnabled
        if (!teachingEnabled) teachingConcept = null
        markDirty()
    }

    fun toggleTheme() {
        darkTheme = !darkTheme
    }

    fun setMarbleBounce(enabled: Boolean) {
        _marbleBounce = enabled
    }

    fun toggleBarycenter() {
        showBarycenter = !showBarycenter
        markDirty()
    }

    fun toggleVectors() {
        showVectors = !showVectors
        markDirty()
    }

    /**
     * §18 — switching preset replaces the whole simulation: old bodies, trails, effects, selection,
     * prediction and camera pose all go, and the new table is framed so its content is visible.
     */
    fun loadPreset(p: Preset) {
        preset = p
        paused = true
        Presets.build(p, arrays)
        arrays.copyInto(initialState)
        afterHardReset()
        frameCameraForPreset(p)
        // §23/§25 — a scene that teaches something opens with the card that says what to look for.
        // Only scenes that actually have a card; the others stay silent rather than showing a stub.
        if (teachingEnabled) {
            val concept = TeachingCatalog.presetConcept(p.name)
            if (TeachingCatalog.card(concept) != null) {
                teachingConcept = concept
                _teachingTier = TeachingTier.WHAT
            }
        }
    }

    /**
     * The gesture layer moved the camera; redraw even while paused.
     *
     * A manual camera gesture also ends Follow: the user has taken the wheel, and having the
     * camera fight the finger by sliding back to the followed body is the single most annoying
     * thing a follow camera can do.
     */
    fun onCameraMoved() {
        if (followId != 0L) stopFollow()
        frameTick++
        cameraTick++
    }

    /**
     * §3/§4 camera elevation, as a 0..1 fraction.
     *
     * 0 is straight down on the tabletop; 1 is the shallowest legal side-on angle
     * ([CameraState.MAX_TILT], about 62 degrees off vertical). The camera cannot pass vertical, so
     * it can never flip over, and the projection never approaches the singular edge-on case where
     * the plane would collapse to a line and picking would stop working.
     *
     * This changes the camera and **nothing else**: no body position, velocity, mass, radius or
     * simulated time is touched anywhere on this path.
     */
    fun setCameraTiltFraction(fraction: Double) {
        camera.setTilt(CameraState.tiltFromFraction(fraction))
        frameTick++
        cameraTick++
    }

    /** Elevation as the same 0..1 fraction the slider works in. */
    val cameraTiltFraction: Double get() = CameraState.tiltFraction(camera.tiltRad)

    fun resetCamera() {
        camera.reset()
        frameTick++
        cameraTick++
    }

    /**
     * Frames [p] and records the result as this preset's initial camera pose (§26).
     *
     * Every preset therefore carries a real initial camera state: its declared span if it has one,
     * the default table if it does not, plus the elevation the preset asks for.
     */
    fun frameCameraForPreset(p: Preset) {
        val declared = p.frameHalfSpanM
        if (declared > 0.0) {
            camera.frame(0.0, 0.0, declared)
        } else {
            camera.reset()
        }
        camera.setTilt(CameraState.tiltFromFraction(p.initialTiltFraction))
        initialCameraPose = camera.snapshot()
        cameraTick++
        markDirty()
    }

    /** Restores the recorded initial pose for the current preset without rebuilding the scene. */
    fun restorePresetCamera() {
        camera.restore(initialCameraPose)
        cameraTick++
        markDirty()
    }

    // ==== §7-§15 follow ===============================================================================

    /**
     * Starts following [bodyId]. The camera tracks the body; **the body is not touched**.
     *
     * Nothing on this path writes to position, velocity, mass, radius, acceleration or simulated
     * time. Follow is a camera behaviour and only a camera behaviour.
     */
    fun startFollow(bodyId: Long) {
        if (bodyId == 0L || arrays.slotOfId(bodyId) < 0) return
        followId = bodyId
        followTargetId = bodyId
        followAcquiring = true
        markDirty()
    }

    /** Ends Follow, leaving the camera exactly where it is (§12). */
    fun stopFollow() {
        if (followId == 0L) return
        followId = 0L
        followTargetId = 0L
        followAcquiring = false
        markDirty()
    }

    fun toggleFollow(bodyId: Long) {
        if (followId == bodyId) stopFollow() else startFollow(bodyId)
    }

    val isFollowing: Boolean get() = followTargetId != 0L

    /**
     * §9/§10 — one frame of follow.
     *
     * Critically damped-ish exponential smoothing toward the target, framed in **wall-clock**
     * seconds, not simulated seconds. That is what keeps the feel identical at 1x and at 100x: the
     * camera is a piece of visual furniture and must not inherit the simulation's time dilation.
     *
     * The camera is drawn toward the body but is allowed to keep a little of its existing offset,
     * so a followed planet does not sit welded to the exact centre of the screen with its primary
     * shoved off-frame (§10). It is never teleported.
     */
    private fun updateFollow(dtRealSeconds: Double) {
        if (followId == 0L) return
        val slot = arrays.slotOfId(followId)
        if (slot < 0) {
            // §14 — the target was merged, captured or deleted while we were watching it.
            stopFollow()
            return
        }
        val tx = arrays.x[slot]
        val ty = arrays.y[slot]
        if (!tx.isFinite() || !ty.isFinite()) {
            stopFollow()
            return
        }

        // Acquisition is quicker than steady-state tracking, so the camera arrives promptly and
        // then stops drawing attention to itself.
        val tau = if (followAcquiring) FOLLOW_ACQUIRE_TAU else FOLLOW_TRACK_TAU
        val dt = dtRealSeconds.coerceIn(0.0, 0.1)
        val alpha = if (dt <= 0.0) 0.0 else 1.0 - Math.exp(-dt / tau)

        val nx = camera.panX + (tx - camera.panX) * alpha
        val ny = camera.panY + (ty - camera.panY) * alpha
        camera.setPan(nx, ny)

        if (followAcquiring) {
            val remaining = hypot(tx - nx, ty - ny)
            val scale = EngineConstants.SCENE_WIDTH_AU * EngineConstants.AU / camera.zoom
            if (remaining < scale * 0.02) followAcquiring = false
        }
    }

    /** Frames everything currently on the table (used by the "fit" control). */
    fun frameCameraToContent() {
        if (arrays.n == 0) {
            camera.reset()
            markDirty()
            return
        }
        var minX = Double.MAX_VALUE
        var maxX = -Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var maxY = -Double.MAX_VALUE
        for (i in 0 until arrays.n) {
            if (arrays.x[i] < minX) minX = arrays.x[i]
            if (arrays.x[i] > maxX) maxX = arrays.x[i]
            if (arrays.y[i] < minY) minY = arrays.y[i]
            if (arrays.y[i] > maxY) maxY = arrays.y[i]
        }
        val cx = 0.5 * (minX + maxX)
        val cy = 0.5 * (minY + maxY)
        val half = maxOf((maxX - minX) * 0.5, (maxY - minY) * 0.5, arrays.metersPerDp * 40.0)
        camera.frame(cx, cy, half)
        markDirty()
    }

    /** §19 — restore a deep copy of the initial experiment, leaving no stale visual state. */
    fun reset() {
        initialState.copyInto(arrays)
        arrays.clearTrails()
        NBodyEngine.computeAccelerations(arrays)
        afterHardReset()
        // §1 — and the camera goes back to where this preset started, not merely the physics.
        camera.restore(initialCameraPose)
        cameraTick++
        markDirty()
    }

    /**
     * §1/§2 — everything that "start again" has to mean.
     *
     * Reset used to restore the physics and leave the camera wherever the user had wandered off
     * to, which made it impossible to actually re-run an experiment: the same initial conditions
     * would appear at a different zoom, a different pan and a different angle every time. It now
     * restores the *whole* experiment, camera included, and drops Follow so no stale target and no
     * in-flight interpolation survives into the fresh run.
     */
    private fun afterHardReset() {
        arrays.clearTrails()
        effects.clear()
        pendingHaptics.clear()
        arrays.simTime = 0.0
        accumulator = 0.0
        lastFrameNanos = 0L
        selectedId = 0L
        draggingId = 0L
        slingshotArmedId = 0L
        slingshotActive = false
        predictionCount = 0
        predictionIsGhost = false
        predictionEscapes = false
        teachingConcept = null
        impactHeadlineFa = null
        impactHeadlineEn = null
        impactDetailFa = null
        impactDetailEn = null
        challengeResultOptionId = null
        // §2 — no stale follow target, no continuing interpolation, no camera drift.
        followId = 0L
        followTargetId = 0L
        followAcquiring = false
        detectors.reset()
        NBodyEngine.resetFailureCounter()
        snapshot.captureFrom(arrays)
        visualEpoch++
        markDirty()
    }

    // ==== selection ==================================================================================

    fun select(bodyId: Long) {
        selectedId = bodyId
        markDirty()
    }

    fun deselect() {
        selectedId = 0L
        slingshotArmedId = 0L
        slingshotActive = false
        predictionCount = 0
        markDirty()
    }

    /** Hit test in scene metres; returns the body id or 0. Picks the topmost (smallest) match. */
    fun hitTest(sceneX: Double, sceneY: Double, tolerance: Double): Long {
        var bestId = 0L
        var bestR = Double.MAX_VALUE
        for (i in 0 until arrays.n) {
            val dx = sceneX - arrays.x[i]
            val dy = sceneY - arrays.y[i]
            val r = arrays.radius[i] + tolerance
            if (dx * dx + dy * dy <= r * r && arrays.radius[i] < bestR) {
                bestR = arrays.radius[i]
                bestId = arrays.id[i]
            }
        }
        return bestId
    }

    // ==== add / remove / duplicate ====================================================================

    /**
     * Adds a catalog entry. Wormholes add a linked pair, so they need two free slots.
     * @return true when the body was created.
     */
    fun addFromCatalog(key: String, atSceneX: Double? = null, atSceneY: Double? = null): Boolean {
        val entry = BodyCatalog.byKey(key) ?: return false
        val needed = if (entry.isPair) 2 else 1
        if (arrays.n + needed > EngineConstants.MAX_BODIES) {
            showNotice(capMessage())
            return false
        }
        if (entry.isPair) {
            val a = freeSpot(entry.dp)
            val b = freeSpot(entry.dp, avoidX = a[0], avoidY = a[1])
            val slot = Wormhole.addPair(arrays, a[0], a[1], b[0], b[1], entry.dp)
            if (slot < 0) return false
            events.add(SimEvent.BodyAdded(arrays.simTime, arrays.id[slot], entry.type))
            selectedId = arrays.id[slot]
        } else {
            val pos = if (atSceneX != null && atSceneY != null) {
                doubleArrayOf(atSceneX, atSceneY)
            } else {
                freeSpot(entry.dp)
            }
            val slot = arrays.add(entry.type, entry.massKg, entry.dp, pos[0], pos[1], 0.0, 0.0, entry.key)
            if (slot < 0) return false
            // A new body gets a sensible circular orbit around the dominant attractor so it does
            // something interesting instead of dropping straight in.
            val attractor = NBodyEngine.circularOrbitVelocity(arrays, slot, scratch2)
            if (attractor >= 0 && arrays.mass[attractor] > arrays.mass[slot]) {
                arrays.vx[slot] = scratch2[0]
                arrays.vy[slot] = scratch2[1]
            }
            events.add(SimEvent.BodyAdded(arrays.simTime, arrays.id[slot], entry.type))
            selectedId = arrays.id[slot]
        }
        afterMutation()
        return true
    }

    fun removeSelected() {
        val slot = arrays.slotOfId(selectedId)
        if (slot < 0) return
        val type = arrays.typeOf(slot)
        events.add(SimEvent.BodyRemoved(arrays.simTime, arrays.id[slot], type))
        if (type == BodyType.WORMHOLE_MOUTH) {
            Wormhole.removeWithPartner(arrays, slot)
        } else {
            if (followId == arrays.id[slot]) stopFollow()
            arrays.removeAt(slot)
        }
        selectedId = 0L
        afterMutation()
    }

    fun duplicateSelected() {
        val slot = arrays.slotOfId(selectedId)
        if (slot < 0) return
        if (arrays.typeOf(slot) == BodyType.WORMHOLE_MOUTH) {
            showNotice(
                if (persian) "دهانه کرم‌چاله همیشه جفت است؛ برای افزودن جفت تازه از دکمه + استفاده کن."
                else "Wormhole mouths always come in pairs — use + to add a new pair."
            )
            return
        }
        if (arrays.isFull()) {
            showNotice(capMessage())
            return
        }
        val offset = arrays.radius[slot] * 2.6
        val newSlot = arrays.add(
            type = arrays.typeOf(slot),
            massKg = arrays.mass[slot],
            radiusDpValue = arrays.radiusDp[slot],
            posX = arrays.x[slot] + offset,
            posY = arrays.y[slot],
            velX = arrays.vx[slot],
            velY = arrays.vy[slot],
            catalog = arrays.catalogKey[slot]
        )
        if (newSlot < 0) return
        arrays.setRadiusDpRaw(newSlot, arrays.radiusDp[slot])
        events.add(SimEvent.BodyAdded(arrays.simTime, arrays.id[newSlot], arrays.typeOf(newSlot)))
        selectedId = arrays.id[newSlot]
        afterMutation()
    }

    private fun capMessage(): String = if (persian) {
        "میز آزمایش پر است — حداکثر ۲۰ جسم جا می‌شود."
    } else {
        "The experiment table is full — 20 bodies maximum."
    }

    private fun freeSpot(dp: Double, avoidX: Double = Double.NaN, avoidY: Double = Double.NaN): DoubleArray {
        val mpd = arrays.metersPerDp
        val wanted = dp * mpd
        val ringsDp = doubleArrayOf(70.0, 105.0, 140.0, 45.0, 170.0)
        for (ringDp in ringsDp) {
            var angle = 0
            while (angle < 360) {
                val a = Math.toRadians(angle.toDouble())
                val px = ringDp * mpd * cos(a)
                val py = ringDp * mpd * sin(a)
                if (isFree(px, py, wanted) &&
                    (avoidX.isNaN() || dist(px, py, avoidX, avoidY) > wanted * 6.0)
                ) {
                    return doubleArrayOf(px, py)
                }
                angle += 24
            }
        }
        return doubleArrayOf(ringsDp[0] * mpd, 0.0)
    }

    private fun dist(ax: Double, ay: Double, bx: Double, by: Double): Double {
        val dx = ax - bx
        val dy = ay - by
        return sqrt(dx * dx + dy * dy)
    }

    private fun isFree(px: Double, py: Double, r: Double): Boolean {
        for (i in 0 until arrays.n) {
            val dx = arrays.x[i] - px
            val dy = arrays.y[i] - py
            val need = (arrays.radius[i] + r) * 1.6
            if (dx * dx + dy * dy < need * need) return false
        }
        return true
    }

    // ==== inspector mutations ==========================================================================
    // Every one of these ends in afterMutation(), which recomputes accelerations (§3.5 / test 39)
    // and invalidates the prediction.

    fun setMass(bodyId: Long, massKg: Double) {
        val slot = arrays.slotOfId(bodyId)
        if (slot < 0) return
        val type = arrays.typeOf(slot)
        if (!type.massEditable) return
        val before = arrays.mass[slot]
        val range = BodyType.massRange(type, before)
        arrays.mass[slot] = massKg.coerceIn(range.start, range.endInclusive)
        afterMutation()
        if (teachingEnabled && before > 0.0) {
            val ratio = arrays.mass[slot] / before
            if (ratio >= 1.5 || ratio <= 0.66) {
                teachingConcept = SimulationDetectors.MASS_CHANGED
                _teachingTier = TeachingTier.WHAT
            }
        }
    }

    fun setRadiusDp(bodyId: Long, dp: Double) {
        val slot = arrays.slotOfId(bodyId)
        if (slot < 0) return
        arrays.setRadiusDp(slot, dp)
        afterMutation()
    }

    /**
     * §20 — numeric position editing is exactly equivalent to dragging: position changes, velocity
     * and mass are untouched, and the stale trail is cut because the body never travelled that jump.
     */
    fun setPosition(bodyId: Long, sceneX: Double, sceneY: Double) {
        val slot = arrays.slotOfId(bodyId)
        if (slot < 0) return
        arrays.x[slot] = sceneX
        arrays.y[slot] = sceneY
        arrays.trails[slot].clear()
        afterMutation()
        if (teachingEnabled) {
            teachingConcept = SimulationDetectors.POSITION_MOVED
            _teachingTier = TeachingTier.WHAT
        }
    }

    /**
     * §21 — velocity editing is explicitly *not* dragging: the trajectory changes, the position
     * does not move and the mass is untouched.
     */
    fun setVelocity(bodyId: Long, vx: Double, vy: Double) {
        val slot = arrays.slotOfId(bodyId)
        if (slot < 0) return
        arrays.vx[slot] = vx
        arrays.vy[slot] = vy
        NBodyEngine.clampVelocity(arrays)
        afterMutation()
        if (teachingEnabled) {
            teachingConcept = SimulationDetectors.VELOCITY_CHANGED
            _teachingTier = TeachingTier.WHAT
        }
    }

    /** Keeps direction, changes magnitude. */
    fun setSpeedMagnitude(bodyId: Long, speedMs: Double) {
        val slot = arrays.slotOfId(bodyId)
        if (slot < 0) return
        val current = sqrt(arrays.vx[slot] * arrays.vx[slot] + arrays.vy[slot] * arrays.vy[slot])
        val angle = if (current > 0.0) atan2(arrays.vy[slot], arrays.vx[slot]) else 0.0
        setVelocity(bodyId, speedMs * cos(angle), speedMs * sin(angle))
    }

    /** Keeps magnitude, changes direction (radians). */
    fun setDirection(bodyId: Long, angleRad: Double) {
        val slot = arrays.slotOfId(bodyId)
        if (slot < 0) return
        val current = sqrt(arrays.vx[slot] * arrays.vx[slot] + arrays.vy[slot] * arrays.vy[slot])
        setVelocity(bodyId, current * cos(angleRad), current * sin(angleRad))
    }

    /** §3.11 orbit helper: sqrt(GM/r) perpendicular to the dominant attractor. */
    fun applyOrbitHelper(bodyId: Long): Boolean {
        val slot = arrays.slotOfId(bodyId)
        if (slot < 0) return false
        val attractor = NBodyEngine.circularOrbitVelocity(arrays, slot, scratch2)
        if (attractor < 0) return false
        arrays.vx[slot] = scratch2[0]
        arrays.vy[slot] = scratch2[1]
        afterMutation()
        return true
    }

    /** UI guidance cap for the inspector and slingshot scales (§3.7). */
    fun velocityGuidance(bodyId: Long): Double {
        val slot = arrays.slotOfId(bodyId)
        if (slot < 0) return EngineConstants.V_MAX
        val a = arrays.dominantAttractor(exceptSlot = slot)
        if (a < 0) return EngineConstants.V_MAX
        val r = dist(arrays.x[slot], arrays.y[slot], arrays.x[a], arrays.y[a])
        if (r <= 0.0) return EngineConstants.V_MAX
        return EngineConstants.uiVelocityGuidance(arrays.mass[a], r)
    }

    private fun afterMutation() {
        NBodyEngine.computeAccelerations(arrays)
        predictionDirty = true
        snapshot.captureFrom(arrays)
        visualEpoch++
        markDirty()
    }

    private fun markDirty() {
        predictionDirty = true
        frameTick++
    }

    private fun showNotice(text: String) {
        notice = text
        noticeFramesLeft = 240
    }

    fun dismissNotice() {
        notice = null
        noticeFramesLeft = 0
    }

    // ==== drag: position only (§2) ======================================================================

    /**
     * §2 — the body becomes kinematic while held so the integrator does not fight the finger, and
     * its velocity is stashed so the release can restore it **exactly**.
     */
    fun beginDrag(bodyId: Long) {
        val slot = arrays.slotOfId(bodyId)
        if (slot < 0) return
        draggingId = bodyId
        selectedId = bodyId
        dragOriginX = arrays.x[slot]
        dragOriginY = arrays.y[slot]
        dragHeldVx = arrays.vx[slot]
        dragHeldVy = arrays.vy[slot]
        dragHeldMass = arrays.mass[slot]
        arrays.kinematic[slot] = true
        afterMutation()
    }

    /** Writes the finger position. Nothing else about the body is touched. */
    fun dragTo(sceneX: Double, sceneY: Double) {
        val slot = arrays.slotOfId(draggingId)
        if (slot < 0) return
        arrays.x[slot] = sceneX
        arrays.y[slot] = sceneY
        // §13 — the ghost path is recomputed on every move, including while the table is paused,
        // so the answer to "what happens if I put it HERE?" is always on screen.
        recomputePrediction()
        // Forces are recomputed on every drag update so a release never uses stale acceleration.
        afterMutation()
    }

    /**
     * §2 — release writes **only** the new position.
     *
     * Velocity is restored bit-for-bit from the value the body had when the drag began: no impulse
     * is applied, no velocity is inferred from the finger's travel, and nothing is zeroed. The
     * trajectory changes solely because the body now starts from somewhere else, which is the
     * whole educational point.
     */
    fun endDrag() {
        val slot = arrays.slotOfId(draggingId)
        if (slot < 0) {
            draggingId = 0L
            return
        }
        arrays.kinematic[slot] = false
        arrays.vx[slot] = dragHeldVx
        arrays.vy[slot] = dragHeldVy
        arrays.mass[slot] = dragHeldMass
        // The trail before the jump describes a path the body never took from here.
        arrays.trails[slot].clear()
        draggingId = 0L
        predictionIsGhost = false
        if (teachingEnabled) {
            teachingConcept = SimulationDetectors.POSITION_MOVED
            _teachingTier = TeachingTier.WHAT
        }
        afterMutation()
    }

    /** Aborts a drag and puts the body back exactly where it was, velocity untouched. */
    fun cancelDrag() {
        val slot = arrays.slotOfId(draggingId)
        if (slot >= 0) {
            arrays.kinematic[slot] = false
            arrays.x[slot] = dragOriginX
            arrays.y[slot] = dragOriginY
            arrays.vx[slot] = dragHeldVx
            arrays.vy[slot] = dragHeldVy
        }
        draggingId = 0L
        predictionIsGhost = false
        afterMutation()
    }

    // ==== slingshot ======================================================================================

    fun armSlingshot(bodyId: Long) {
        slingshotArmedId = bodyId
        selectedId = bodyId
        slingshotActive = false
        markDirty()
    }

    fun cancelSlingshot() {
        slingshotArmedId = 0L
        slingshotActive = false
        slingDx = 0.0
        slingDy = 0.0
        slingshotVx = 0.0
        slingshotVy = 0.0
        markDirty()
    }

    /**
     * @param dragSceneX drag vector in scene metres, measured from the body outward.
     *        The launch velocity is the opposite of the drag (§3.11).
     */
    fun updateSlingshot(dragSceneX: Double, dragSceneY: Double) {
        if (slingshotArmedId == 0L) return
        slingDx = dragSceneX
        slingDy = dragSceneY
        slingshotActive = true
        val v = slingshotVelocity()
        slingshotVx = v[0]
        slingshotVy = v[1]
        predictionDirty = true
        frameTick++
    }

    /** Velocity the slingshot would apply right now, already capped by UI guidance. */
    fun slingshotVelocity(): DoubleArray {
        val slot = arrays.slotOfId(slingshotArmedId)
        if (slot < 0) return doubleArrayOf(0.0, 0.0)
        val guidance = velocityGuidance(slingshotArmedId)
        // A drag of 180 dp maps to the full guidance speed: predictable at every viewport width.
        val perMeter = guidance / (180.0 * arrays.metersPerDp)
        var vx = -slingDx * perMeter
        var vy = -slingDy * perMeter
        val v = sqrt(vx * vx + vy * vy)
        if (v > guidance && v > 0.0) {
            vx *= guidance / v
            vy *= guidance / v
        }
        return doubleArrayOf(vx, vy)
    }

    fun releaseSlingshot() {
        val slot = arrays.slotOfId(slingshotArmedId)
        if (slot < 0 || !slingshotActive) {
            cancelSlingshot()
            return
        }
        val v = slingshotVelocity()
        arrays.vx[slot] = v[0]
        arrays.vy[slot] = v[1]
        NBodyEngine.clampVelocity(arrays)
        slingshotActive = false
        slingshotArmedId = 0L
        slingDx = 0.0
        slingDy = 0.0
        slingshotVx = 0.0
        slingshotVy = 0.0
        afterMutation()
    }

    // ==== prediction =======================================================================================

    /**
     * §3.9 test-particle preview. Never mutates the simulation; recomputed on every state change
     * and, while running, at ~10 Hz.
     */
    private fun recomputePrediction() {
        // §13 — a finger on a body outranks everything else: while dragging, the path shown is
        // always the path of the body being dragged, whether or not it happens to be selected.
        val targetId = when {
            draggingId != 0L -> draggingId
            slingshotArmedId != 0L -> slingshotArmedId
            else -> selectedId
        }
        val slot = arrays.slotOfId(targetId)
        if (slot < 0) {
            predictionCount = 0
            predictionApproximate = false
            predictionIsGhost = false
            predictionEscapes = false
            return
        }
        val vx: Double
        val vy: Double
        if (slingshotArmedId != 0L && slingshotActive) {
            val v = slingshotVelocity()
            vx = v[0]
            vy = v[1]
        } else {
            vx = arrays.vx[slot]
            vy = arrays.vy[slot]
        }
        predictionCount = Predictor.predict(
            s = arrays,
            excludeSlot = slot,
            startX = arrays.x[slot],
            startY = arrays.y[slot],
            startVx = vx,
            startVy = vy,
            particleRadius = arrays.radius[slot],
            outXY = predictionXY,
            maxSamples = maxPredictionSamples
        )
        predictionApproximate = Predictor.isApproximate(arrays, arrays.mass[slot])
        // A drag preview is a ghost: same body, same velocity, same mass, different position.
        predictionIsGhost = draggingId != 0L && targetId == draggingId
        predictionEscapes = previewIsUnbound(slot, vx, vy)
    }

    /**
     * §14 — will the previewed body come back, or is it leaving for good?
     *
     * This is the specific orbital energy relative to the system's dominant attractor,
     * `e = v_rel^2 / 2 - G*M/r`. Positive means the two-body orbit is unbound. That is an exact
     * criterion rather than a guess made by eyeballing the end of the sampled path, which is what
     * matters here: the prediction window is only a few hundred steps long, so a body on a clearly
     * hyperbolic path that starts far out would barely move within it and would be misread as
     * bound. The dominant attractor is used because a preview only has to answer "does this stay
     * in the system?", and in every preset one mass overwhelmingly sets that answer.
     */
    private fun previewIsUnbound(slot: Int, vxPreview: Double, vyPreview: Double): Boolean {
        if (predictionCount < 2) return false
        val anchor = arrays.dominantAttractor(exceptSlot = slot)
        if (anchor < 0 || arrays.mass[anchor] <= 0.0) return false
        val r = hypot(arrays.x[slot] - arrays.x[anchor], arrays.y[slot] - arrays.y[anchor])
        if (r <= 0.0) return false
        val rvx = vxPreview - arrays.vx[anchor]
        val rvy = vyPreview - arrays.vy[anchor]
        val energy = 0.5 * (rvx * rvx + rvy * rvy) - EngineConstants.G * arrays.mass[anchor] / r
        return energy > 0.0
    }

    // ==== teaching / challenges ==============================================================================

    fun setTeachingTier(tier: TeachingTier) {
        _teachingTier = tier
    }

    fun dismissTeaching() {
        teachingConcept = null
        _teachingTier = TeachingTier.WHAT
    }

    fun startChallenge(challenge: Challenge) {
        activeChallenge = challenge
        challengePrediction = null
        challengeResultOptionId = null
        loadPreset(challenge.preset)
        challengeRunner.start(challenge, snapshot)
    }

    fun submitPrediction(optionId: String) {
        challengePrediction = optionId
        challengeRunner.predictedOptionId = optionId
        // §18 loads every scene paused, including the one a challenge sets up. Committing to a
        // guess IS the user action that starts it: predict first, then watch it happen.
        if (paused) {
            paused = false
            lastFrameNanos = 0L
        }
    }

    fun closeChallenge() {
        activeChallenge = null
        challengePrediction = null
        challengeResultOptionId = null
        challengeRunner.cancel()
    }

    // ==== persistence =========================================================================================

    fun serialize(): String = SaveState.encode(
        arrays,
        SaveState.Session(
            preset = preset,
            speedIndex = speedIndex,
            paused = paused,
            trailsVisible = trailsVisible,
            teachingEnabled = teachingEnabled,
            darkTheme = darkTheme,
            persian = persian,
            marbleBounce = marbleBounce,
            selectedId = selectedId
        )
    )

    fun restore(text: String?): Boolean {
        val session = SaveState.decode(text, arrays) ?: return false
        preset = session.preset
        _speedIndex = session.speedIndex.coerceIn(0, EngineConstants.SPEEDS.size - 1)
        paused = session.paused
        trailsVisible = session.trailsVisible
        teachingEnabled = session.teachingEnabled
        darkTheme = session.darkTheme
        // §5 — deliberately NOT restoring session.persian. The sandbox has no language of its own;
        // the host app's locale is pushed in by applyHostLanguage on every entry, so a session
        // saved months ago in the other language can never override the app the user is holding.
        _marbleBounce = session.marbleBounce
        selectedId = session.selectedId
        // Reset must still return to the pristine preset, not to the restored mid-experiment state.
        initialState.setMetersPerDp(arrays.metersPerDp)
        Presets.build(session.preset, initialState)
        NBodyEngine.computeAccelerations(arrays)
        snapshot.captureFrom(arrays)
        detectors.reset()
        visualEpoch++
        markDirty()
        return true
    }

    /** Total momentum magnitude, exposed for the collision-momentum challenge readout. */
    fun totalMomentum(): Double {
        val p = Collision.totalMomentum(arrays)
        return sqrt(p[0] * p[0] + p[1] * p[1])
    }

    private companion object {
        /** Wall-clock seconds to close ~63% of the gap while acquiring a new follow target. */
        const val FOLLOW_ACQUIRE_TAU = 0.28

        /** Steady-state tracking constant: tight enough to keep up, loose enough to feel calm. */
        const val FOLLOW_TRACK_TAU = 0.12
    }

}

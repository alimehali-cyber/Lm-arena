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
import com.zig.gravity.physics.NBodyEngine
import com.zig.gravity.physics.Predictor
import com.zig.gravity.physics.SimArrays
import com.zig.gravity.physics.SimEvent
import com.zig.gravity.physics.Wormhole
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

    private val dragHistoryX = DoubleArray(6)
    private val dragHistoryY = DoubleArray(6)
    private val dragHistoryT = DoubleArray(6)
    private var dragHistoryCount = 0

    // ---- prediction --------------------------------------------------------------------------
    private val maxPredictionSamples = EngineConstants.PREDICTION_STEPS / Predictor.SAMPLE_STRIDE + 2
    val predictionXY = DoubleArray(maxPredictionSamples * 2)
    var predictionCount: Int = 0
        private set
    var predictionApproximate: Boolean = false
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
    }

    // ==== viewport =================================================================================

    /**
     * §3.2 scene scale: the viewport is always 3 AU wide, so metersPerDp is derived from the live
     * width and never stored as a constant. There is no zoom in v1 (§3.4).
     */
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

    private fun handleIntegrityEvents() {
        for (e in events) {
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

    fun toggleLanguage() {
        persian = !persian
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

    fun loadPreset(p: Preset) {
        preset = p
        Presets.build(p, arrays)
        arrays.copyInto(initialState)
        afterHardReset()
    }

    /** §19 — restore a deep copy of the initial experiment, leaving no stale visual state. */
    fun reset() {
        initialState.copyInto(arrays)
        arrays.clearTrails()
        NBodyEngine.computeAccelerations(arrays)
        afterHardReset()
    }

    private fun afterHardReset() {
        arrays.clearTrails()
        arrays.simTime = 0.0
        accumulator = 0.0
        lastFrameNanos = 0L
        selectedId = 0L
        draggingId = 0L
        slingshotArmedId = 0L
        slingshotActive = false
        predictionCount = 0
        teachingConcept = null
        challengeResultOptionId = null
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

    fun setPosition(bodyId: Long, sceneX: Double, sceneY: Double) {
        val slot = arrays.slotOfId(bodyId)
        if (slot < 0) return
        arrays.x[slot] = sceneX
        arrays.y[slot] = sceneY
        afterMutation()
    }

    fun setVelocity(bodyId: Long, vx: Double, vy: Double) {
        val slot = arrays.slotOfId(bodyId)
        if (slot < 0) return
        arrays.vx[slot] = vx
        arrays.vy[slot] = vy
        NBodyEngine.clampVelocity(arrays)
        afterMutation()
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

    // ==== drag / throw ==================================================================================

    /** §3.11: the body becomes kinematic while held — physics must not fight the finger. */
    fun beginDrag(bodyId: Long) {
        val slot = arrays.slotOfId(bodyId)
        if (slot < 0) return
        draggingId = bodyId
        selectedId = bodyId
        arrays.kinematic[slot] = true
        dragHistoryCount = 0
        afterMutation()
    }

    fun dragTo(sceneX: Double, sceneY: Double, timeSeconds: Double) {
        val slot = arrays.slotOfId(draggingId)
        if (slot < 0) return
        arrays.x[slot] = sceneX
        arrays.y[slot] = sceneY
        pushDragSample(sceneX, sceneY, timeSeconds)
        // Forces are recomputed on every drag update so a release never uses stale acceleration.
        afterMutation()
    }

    /** Release velocity is derived from pointer history (§3.11), then clamped by the engine. */
    fun endDrag() {
        val slot = arrays.slotOfId(draggingId)
        if (slot < 0) {
            draggingId = 0L
            return
        }
        var vx = 0.0
        var vy = 0.0
        if (dragHistoryCount >= 2) {
            val newest = (dragHistoryCount - 1) % dragHistoryX.size
            val oldestIndex = if (dragHistoryCount > dragHistoryX.size) {
                dragHistoryCount - dragHistoryX.size
            } else {
                0
            }
            val oldest = oldestIndex % dragHistoryX.size
            val dt = dragHistoryT[newest] - dragHistoryT[oldest]
            if (dt > 1.0e-4) {
                vx = (dragHistoryX[newest] - dragHistoryX[oldest]) / dt
                vy = (dragHistoryY[newest] - dragHistoryY[oldest]) / dt
            }
        }
        arrays.kinematic[slot] = false
        arrays.vx[slot] = vx
        arrays.vy[slot] = vy
        NBodyEngine.clampVelocity(arrays)
        draggingId = 0L
        dragHistoryCount = 0
        afterMutation()
    }

    fun cancelDrag() {
        val slot = arrays.slotOfId(draggingId)
        if (slot >= 0) arrays.kinematic[slot] = false
        draggingId = 0L
        afterMutation()
    }

    private fun pushDragSample(x: Double, y: Double, t: Double) {
        val i = dragHistoryCount % dragHistoryX.size
        dragHistoryX[i] = x
        dragHistoryY[i] = y
        dragHistoryT[i] = t
        dragHistoryCount++
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
        val targetId = if (slingshotArmedId != 0L) slingshotArmedId else selectedId
        val slot = arrays.slotOfId(targetId)
        if (slot < 0) {
            predictionCount = 0
            predictionApproximate = false
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
        persian = session.persian
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
}

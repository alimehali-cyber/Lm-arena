package com.zig.gravity.edu

import com.zig.gravity.physics.EngineConstants
import com.zig.gravity.physics.SimEvent
import com.zig.gravity.edu.detectors.SimulationDetectors
import com.zig.gravity.sim.Preset
import com.zig.gravity.sim.SimSnapshot
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * §3.14 — the eight POE (Predict / Observe / Explain) challenges.
 *
 * Every challenge resolves against the **live simulation**, never against a stored answer key
 * alone: [ChallengeRunner] records a baseline when the challenge starts and then watches real
 * state and real engine events to decide which option actually happened.
 */
data class ChallengeOption(
    val id: String,
    val textFa: String,
    val textEn: String,
    val correct: Boolean
)

enum class ChallengeKind {
    MASS_DOUBLING,
    DISTANCE_VS_PERIOD,
    ESCAPE_VELOCITY,
    BINARY_BARYCENTER,
    COLLISION_MOMENTUM,
    BLACK_HOLE_CAPTURE,
    WORMHOLE_TRAVERSAL,
    MOON_FALL
}

data class Challenge(
    val kind: ChallengeKind,
    val titleFa: String,
    val titleEn: String,
    val questionFa: String,
    val questionEn: String,
    val setupFa: String,
    val setupEn: String,
    val preset: Preset,
    val options: List<ChallengeOption>,
    val explainConcept: String
) {
    val correctOptionId: String get() = options.first { it.correct }.id
}

object Challenges {

    val all: List<Challenge> = listOf(
        Challenge(
            kind = ChallengeKind.MASS_DOUBLING,
            titleFa = "اگر جرم زمین دو برابر شود؟",
            titleEn = "What if Earth's mass doubled?",
            questionFa = "اگر جرم زمین را دو برابر کنی، مدار ماه چه می‌شود؟",
            questionEn = "If you double Earth's mass, what happens to the Moon's orbit?",
            setupFa = "روی زمین ضربه بزن، بازرس را باز کن و جرم را دو برابر کن.",
            setupEn = "Tap Earth, open the inspector and double its mass.",
            preset = Preset.EARTH_MOON,
            options = listOf(
                ChallengeOption("tighter", "مدار تنگ‌تر و تندتر می‌شود", "The orbit becomes tighter and faster", true),
                ChallengeOption("escape", "ماه بلافاصله فرار می‌کند", "The Moon escapes immediately", false),
                ChallengeOption("same", "هیچ تغییری نمی‌کند", "Nothing changes", false)
            ),
            explainConcept = SimulationDetectors.MASS_CHANGED
        ),
        Challenge(
            kind = ChallengeKind.DISTANCE_VS_PERIOD,
            titleFa = "فاصله و دوره تناوب",
            titleEn = "Distance and orbital period",
            questionFa = "دو سیاره در فاصله‌های متفاوت از خورشید: کدام دور کامل را زودتر می‌زند؟",
            questionEn = "Two planets at different distances from the Sun: which completes a lap first?",
            setupFa = "یک سیاره دیگر در فاصله‌ای دورتر اضافه کن و صبر کن تا هر دو یک دور کامل بزنند.",
            setupEn = "Add a second planet farther out and wait until both complete a full lap.",
            preset = Preset.SUN_EARTH,
            options = listOf(
                ChallengeOption("near", "نزدیک‌تر — هرچه دورتر، کندتر", "The nearer one — farther means slower", true),
                ChallengeOption("far", "دورتر — چون مسیرش بازتر است", "The farther one — it has more room", false),
                ChallengeOption("same", "هر دو هم‌زمان", "Both at the same time", false)
            ),
            explainConcept = SimulationDetectors.ORBIT_STABILIZED
        ),
        Challenge(
            kind = ChallengeKind.ESCAPE_VELOCITY,
            titleFa = "سرعت گریز",
            titleEn = "Escape velocity",
            questionFa = "چقدر باید سرعت زمین را زیاد کنی تا برای همیشه از خورشید جدا شود؟",
            questionEn = "How much faster must Earth move to leave the Sun for good?",
            setupFa = "زمین را انتخاب کن و در بازرس، اندازه سرعت را کم‌کم زیاد کن.",
            setupEn = "Select Earth and raise its speed in the inspector, step by step.",
            preset = Preset.SUN_EARTH,
            options = listOf(
                ChallengeOption("sqrt2", "حدود ۱٫۴ برابر سرعت مداری (√۲)", "About 1.4x orbital speed (√2)", true),
                ChallengeOption("any", "هر افزایشی کافی است", "Any increase is enough", false),
                ChallengeOption("never", "هرگز نمی‌تواند فرار کند", "It can never escape", false)
            ),
            explainConcept = SimulationDetectors.BODY_ESCAPED
        ),
        Challenge(
            kind = ChallengeKind.BINARY_BARYCENTER,
            titleFa = "مرکز چرخش دو ستاره",
            titleEn = "The centre of a binary",
            questionFa = "دو ستاره هم‌جرم دقیقاً به دور چه چیزی می‌چرخند؟",
            questionEn = "Two equal stars orbit exactly what?",
            setupFa = "نشانگر مرکز جرم را روشن کن و یک دور کامل را تماشا کن.",
            setupEn = "Turn on the barycentre marker and watch one full lap.",
            preset = Preset.BINARY,
            options = listOf(
                ChallengeOption("bary", "یک نقطه خالی بین آن‌ها: مرکز جرم", "An empty point between them: the barycentre", true),
                ChallengeOption("heavier", "به دور ستاره سنگین‌تر", "Around the heavier star", false),
                ChallengeOption("nofix", "هیچ نقطه ثابتی وجود ندارد", "There is no fixed point", false)
            ),
            explainConcept = SimulationDetectors.TWO_BODY_DANCE
        ),
        Challenge(
            kind = ChallengeKind.COLLISION_MOMENTUM,
            titleFa = "تکانه در برخورد",
            titleEn = "Momentum in a collision",
            questionFa = "وقتی دو جسم برخورد می‌کنند و یکی می‌شوند، تکانه کل چه می‌شود؟",
            questionEn = "When two bodies collide and merge, what happens to the total momentum?",
            setupFa = "یک جسم را به سمت جسم دیگر پرتاب کن تا برخورد کنند.",
            setupEn = "Throw one body into another until they collide.",
            preset = Preset.EARTH_MOON,
            options = listOf(
                ChallengeOption("same", "بدون تغییر می‌ماند", "It stays exactly the same", true),
                ChallengeOption("lost", "بخشی از آن از بین می‌رود", "Part of it is lost", false),
                ChallengeOption("double", "دو برابر می‌شود", "It doubles", false)
            ),
            explainConcept = SimulationDetectors.BODY_MERGED
        ),
        Challenge(
            kind = ChallengeKind.BLACK_HOLE_CAPTURE,
            titleFa = "سیاه‌چاله چه چیزی را می‌بلعد؟",
            titleEn = "What does a black hole swallow?",
            questionFa = "کدام اجسام گرفتار سیاه‌چاله می‌شوند؟",
            questionEn = "Which bodies get captured by the black hole?",
            setupFa = "تماشا کن؛ سه جسم آزمایشی با فاصله‌های متفاوت از کنار سیاه‌چاله می‌گذرند.",
            setupEn = "Just watch: three test marbles pass the hole at three different distances.",
            preset = Preset.BLACK_HOLE_LAB,
            options = listOf(
                ChallengeOption("ring", "فقط آن‌هایی که از حلقه رد می‌شوند", "Only the ones that cross the ring", true),
                ChallengeOption("all", "هر چیزی که نزدیک شود", "Anything that comes close", false),
                ChallengeOption("none", "هیچ‌کدام؛ فقط منحرف می‌شوند", "None; they are only deflected", false)
            ),
            explainConcept = SimulationDetectors.BH_CAPTURE
        ),
        Challenge(
            kind = ChallengeKind.WORMHOLE_TRAVERSAL,
            titleFa = "عبور از کرم‌چاله",
            titleEn = "Through the wormhole",
            questionFa = "جسم پس از عبور از دهانه جفت، با چه سرعتی بیرون می‌آید؟",
            questionEn = "At what speed does a body leave the partner mouth?",
            setupFa = "صبر کن تا جسم آزمایشی وارد دهانه شود.",
            setupEn = "Wait for the test marble to reach a mouth.",
            preset = Preset.WORMHOLE_LAB,
            options = listOf(
                ChallengeOption("same", "با همان سرعت قبلی", "With exactly the same speed", true),
                ChallengeOption("stop", "متوقف می‌شود", "It stops", false),
                ChallengeOption("faster", "سریع‌تر می‌شود", "It speeds up", false)
            ),
            explainConcept = SimulationDetectors.WORMHOLE_TRAVERSAL
        ),
        Challenge(
            kind = ChallengeKind.MOON_FALL,
            titleFa = "چرا ماه نمی‌افتد؟",
            titleEn = "Why doesn't the Moon fall?",
            questionFa = "زمین دائم ماه را می‌کشد. پس چرا ماه روی زمین نمی‌افتد؟",
            questionEn = "Earth pulls the Moon constantly. So why doesn't the Moon fall onto Earth?",
            setupFa = "یک دور کامل ماه را تماشا کن. بعد اگر خواستی سرعت ماه را صفر کن و دوباره ببین.",
            setupEn = "Watch one full lap. Then, if you like, set the Moon's speed to zero and look again.",
            preset = Preset.EARTH_MOON,
            options = listOf(
                ChallengeOption("sideways", "چون همزمان با سرعت زیادی به پهلو حرکت می‌کند", "Because it is also moving sideways, fast", true),
                ChallengeOption("noreach", "چون گرانش زمین به آن نمی‌رسد", "Because Earth's gravity does not reach it", false),
                ChallengeOption("light", "چون ماه خیلی سبک است", "Because the Moon is too light", false)
            ),
            explainConcept = SimulationDetectors.MOON_QUESTION
        )
    )

    fun byKind(kind: ChallengeKind): Challenge = all.first { it.kind == kind }
}

/**
 * Runs one challenge against live state.
 *
 * `start` records a baseline; `observe` is called every teaching tick and returns the option id
 * the simulation actually demonstrated, or null while the outcome is still undetermined.
 */
class ChallengeRunner {

    var active: Challenge? = null
        private set
    var predictedOptionId: String? = null
    var resolvedOptionId: String? = null
        private set

    private val baseMass = HashMap<Long, Double>()
    private val baseRadius = HashMap<Long, Double>()
    private val baseSpeed = HashMap<Long, Double>()
    private val sweptPeriods = HashMap<Long, Double>()
    private val baryStart = DoubleArray(2)
    private var survivorFlyby = false

    fun start(challenge: Challenge, snap: SimSnapshot) {
        active = challenge
        predictedOptionId = null
        resolvedOptionId = null
        baseMass.clear(); baseRadius.clear(); baseSpeed.clear(); sweptPeriods.clear()
        survivorFlyby = false
        for (i in 0 until snap.n) {
            baseMass[snap.id[i]] = snap.mass[i]
            baseSpeed[snap.id[i]] = sqrt(snap.vx[i] * snap.vx[i] + snap.vy[i] * snap.vy[i])
            val a = heaviestOther(snap, i)
            if (a >= 0) {
                val dx = snap.x[i] - snap.x[a]
                val dy = snap.y[i] - snap.y[a]
                baseRadius[snap.id[i]] = sqrt(dx * dx + dy * dy)
            }
        }
        baryStart[0] = snap.barycenter[0]
        baryStart[1] = snap.barycenter[1]
    }

    fun cancel() {
        active = null
        predictedOptionId = null
        resolvedOptionId = null
    }

    /** @return the resolved option id the first time the simulation settles the question. */
    fun observe(
        snap: SimSnapshot,
        events: List<SimEvent>,
        detectors: SimulationDetectors
    ): String? {
        val c = active ?: return null
        if (resolvedOptionId != null) return null

        val resolved: String? = when (c.kind) {
            ChallengeKind.MASS_DOUBLING -> resolveMassDoubling(snap)
            ChallengeKind.DISTANCE_VS_PERIOD -> resolveDistanceVsPeriod(snap, detectors)
            ChallengeKind.ESCAPE_VELOCITY -> resolveEscape(snap)
            ChallengeKind.BINARY_BARYCENTER -> resolveBarycenter(snap, detectors)
            ChallengeKind.COLLISION_MOMENTUM -> resolveCollision(events)
            ChallengeKind.BLACK_HOLE_CAPTURE -> resolveCapture(snap, events)
            ChallengeKind.WORMHOLE_TRAVERSAL -> resolveWormhole(snap, events)
            ChallengeKind.MOON_FALL -> resolveMoonFall(snap, detectors)
        }
        if (resolved != null) resolvedOptionId = resolved
        return resolved
    }

    private fun heaviestOther(snap: SimSnapshot, slot: Int): Int {
        var best = -1
        var bestMass = 0.0
        for (j in 0 until snap.n) {
            if (j == slot) continue
            if (snap.mass[j] > bestMass) { bestMass = snap.mass[j]; best = j }
        }
        return best
    }

    private fun resolveMassDoubling(snap: SimSnapshot): String? {
        // Find a body whose mass the user at least (nearly) doubled.
        for (i in 0 until snap.n) {
            val b = baseMass[snap.id[i]] ?: continue
            if (b <= 0.0 || snap.mass[i] < b * 1.8) continue
            // Look at its heaviest companion's orbital radius.
            for (j in 0 until snap.n) {
                if (j == i) continue
                val r0 = baseRadius[snap.id[j]] ?: continue
                val dx = snap.x[j] - snap.x[i]
                val dy = snap.y[j] - snap.y[i]
                val r = sqrt(dx * dx + dy * dy)
                if (r < r0 * 0.94) return "tighter"
                if (r > r0 * 4.0) return "escape"
            }
        }
        return null
    }

    private fun resolveDistanceVsPeriod(snap: SimSnapshot, d: SimulationDetectors): String? {
        val sun = snap.slotOfId(heaviestId(snap)) 
        if (sun < 0) return null
        var nearId = 0L; var nearR = Double.MAX_VALUE
        var farId = 0L; var farR = 0.0
        for (i in 0 until snap.n) {
            if (i == sun || snap.mass[i] <= 0.0) continue
            val dx = snap.x[i] - snap.x[sun]
            val dy = snap.y[i] - snap.y[sun]
            val r = sqrt(dx * dx + dy * dy)
            if (r < nearR) { nearR = r; nearId = snap.id[i] }
            if (r > farR) { farR = r; farId = snap.id[i] }
        }
        if (nearId == 0L || farId == 0L || nearId == farId) return null
        val nearSweep = d.sweptDegreesOf(nearId)
        val farSweep = d.sweptDegreesOf(farId)
        if (nearSweep >= 360.0 && farSweep < 360.0) return "near"
        if (farSweep >= 360.0 && nearSweep < 360.0) return "far"
        return null
    }

    private fun heaviestId(snap: SimSnapshot): Long {
        var best = 0L
        var bestMass = 0.0
        for (i in 0 until snap.n) if (snap.mass[i] > bestMass) { bestMass = snap.mass[i]; best = snap.id[i] }
        return best
    }

    private fun resolveEscape(snap: SimSnapshot): String? {
        val heavy = snap.slotOfId(heaviestId(snap))
        if (heavy < 0) return null
        for (i in 0 until snap.n) {
            if (i == heavy) continue
            val dx = snap.x[i] - snap.x[heavy]
            val dy = snap.y[i] - snap.y[heavy]
            val r = sqrt(dx * dx + dy * dy)
            if (r <= 0.0) continue
            val rvx = snap.vx[i] - snap.vx[heavy]
            val rvy = snap.vy[i] - snap.vy[heavy]
            val v = sqrt(rvx * rvx + rvy * rvy)
            val eps = EngineConstants.specificOrbitalEnergy(v, snap.mass[heavy], r)
            if (eps >= 0.0 && (dx * rvx + dy * rvy) > 0.0) {
                val vCirc = EngineConstants.circularSpeed(snap.mass[heavy], r)
                return if (vCirc > 0.0 && v >= 1.30 * vCirc) "sqrt2" else "any"
            }
        }
        return null
    }

    private fun resolveBarycenter(snap: SimSnapshot, d: SimulationDetectors): String? {
        var sweptEnough = false
        for (i in 0 until snap.n) if (d.sweptDegreesOf(snap.id[i]) >= 350.0) sweptEnough = true
        if (!sweptEnough) return null
        val dx = snap.barycenter[0] - baryStart[0]
        val dy = snap.barycenter[1] - baryStart[1]
        val drift = sqrt(dx * dx + dy * dy)
        // A stationary barycentre while both stars move is the whole point.
        val scale = 5.0 * snap.metersPerDp
        return if (drift < scale) "bary" else "nofix"
    }

    private fun resolveCollision(events: List<SimEvent>): String? {
        for (e in events) {
            if (e is SimEvent.BodyMerged) {
                val before = e.momentumBefore
                val after = e.momentumAfter
                val scale = maxOf(abs(before), 1.0e-9)
                return if (abs(after - before) / scale < 1.0e-6) "same" else "lost"
            }
        }
        return null
    }

    private fun resolveCapture(snap: SimSnapshot, events: List<SimEvent>): String? {
        for (e in events) if (e is SimEvent.BlackHoleCapture) survivorFlyby = true
        if (!survivorFlyby) return null
        // Resolve once at least one other body has clearly survived the pass.
        var survivors = 0
        for (i in 0 until snap.n) if (snap.mass[i] <= 0.0) survivors++
        return if (survivors > 0) "ring" else "all"
    }

    private fun resolveWormhole(snap: SimSnapshot, events: List<SimEvent>): String? {
        for (e in events) {
            if (e is SimEvent.WormholeTraversal) {
                val slot = snap.slotOfId(e.bodyId)
                if (slot < 0) return null
                val v = sqrt(snap.vx[slot] * snap.vx[slot] + snap.vy[slot] * snap.vy[slot])
                val scale = maxOf(e.speed, 1.0e-9)
                return when {
                    abs(v - e.speed) / scale < 1.0e-6 -> "same"
                    v < e.speed * 0.5 -> "stop"
                    else -> "faster"
                }
            }
        }
        return null
    }

    private fun resolveMoonFall(snap: SimSnapshot, d: SimulationDetectors): String? {
        for (i in 0 until snap.n) {
            if (d.sweptDegreesOf(snap.id[i]) >= 350.0) return "sideways"
        }
        return null
    }
}

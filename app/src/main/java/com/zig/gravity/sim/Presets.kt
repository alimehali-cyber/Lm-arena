package com.zig.gravity.sim

import com.zig.gravity.physics.BodyType
import com.zig.gravity.physics.EngineConstants
import com.zig.gravity.physics.NBodyEngine
import com.zig.gravity.physics.SimArrays
import com.zig.gravity.physics.Wormhole
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The six-experiment preset catalog (Phase 6).
 *
 * Layout distances are expressed in **dp** and converted with the live `metersPerDp`, so every
 * preset is readable at any viewport width. Per §3.1 preset distances are deliberately *not to
 * scale* — masses, G and velocities are real, so the emergent periods, escape speeds and energy
 * exchange are genuinely correct for the configuration the user is looking at.
 */
enum class Preset(
    val titleFa: String,
    val titleEn: String,
    val noteFa: String,
    val noteEn: String,
    /**
     * Half-width the camera should frame when this preset loads, in metres. 0 means "the default
     * 3 AU table", which is what every dp-laid-out experiment wants.
     */
    val frameHalfSpanM: Double = 0.0
) {
    FULL_SOLAR_SYSTEM(
        "منظومه شمسی کامل", "Full Solar System",
        "هشت سیاره و ماه، با جرم و فاصله و سرعت واقعی. مدارهای بیرونی خیلی کندند؛ سرعت ۱۰۰× را بزن.",
        "Eight planets and the Moon at real masses, distances and speeds. The outer orbits are very slow, so try 100x.",
        frameHalfSpanM = 4.6e12
    ),
    SUN_EARTH(
        "خورشید و زمین", "Sun and Earth",
        "یک سال زمینی حدود ۳۱ ثانیه طول می‌کشد. مرکز جرم را روشن کن و ببین خورشید هم کمی تکان می‌خورد.",
        "One Earth year takes about 31 seconds. Turn on the barycentre and watch the Sun wobble."
    ),
    EARTH_MOON(
        "زمین و ماه", "Earth and Moon",
        "چرا ماه با وجود کشش زمین، روی زمین نمی‌افتد؟ فاصله بزرگ‌نمایی شده تا مدار دیده شود؛ برای دیدن یک دور کامل سرعت ۱۶× را بزن.",
        "Why doesn't the Moon fall onto Earth? The separation is exaggerated so the orbit is readable; use 16x to watch a full lap."
    ),
    INNER_SYSTEM(
        "منظومه داخلی", "Inner system",
        "چهار سیاره سنگی. هرچه دورتر، کندتر — این همان قانون سوم کپلر است.",
        "Four rocky planets. Farther means slower — that is Kepler's third law."
    ),
    BINARY(
        "ستاره دوتایی", "Binary stars",
        "دو ستاره هم‌جرم به دور یک نقطه خالی می‌چرخند: مرکز جرم مشترک.",
        "Two equal stars orbit an empty point: their shared barycentre."
    ),
    BLACK_HOLE_LAB(
        "آزمایش سیاه‌چاله", "Black hole lab",
        "سیاه‌چاله در این شبیه‌سازی فقط یک جرم نیوتنی است. حلقه، مرز گرفته‌شدن را نشان می‌دهد.",
        "The black hole here is a plain Newtonian mass. The ring marks the capture boundary."
    ),
    WORMHOLE_LAB(
        "آزمایش کرم‌چاله", "Wormhole lab",
        "کرم‌چاله یک مدل فرضی است، نه یک پدیده اثبات‌شده. جسم از یک دهانه وارد و از دهانه جفت خارج می‌شود.",
        "The wormhole is a hypothetical model, not an established phenomenon. A body enters one mouth and leaves its partner."
    ),
    THREE_BODY(
        "مسئله سه‌جسم", "Three-body problem",
        "سه ستاره هم‌جرم روی یک مثلث. این چیدمان راه‌حل بسته ندارد؛ کوچک‌ترین تفاوت، سرنوشت را عوض می‌کند.",
        "Three equal stars on a triangle. This arrangement has no closed solution: the tiniest difference changes its fate."
    ),
    EMPTY_TABLE(
        "میز خالی", "Empty table",
        "هیچ جسمی نیست. با دکمه + شروع کن و ببین گرانش چطور از هیچ، یک سامانه می‌سازد.",
        "Nothing here yet. Start with the + button and watch gravity build a system from scratch."
    );

    companion object {
        /** §9: the sandbox opens on the complete Solar System, never on an empty table. */
        val DEFAULT: Preset = FULL_SOLAR_SYSTEM
    }
}

object Presets {

    /** Builds [preset] into [s]. The array is cleared first; simTime resets to zero. */
    fun build(preset: Preset, s: SimArrays) {
        s.clear()
        val mpd = s.metersPerDp
        fun dp(v: Double) = v * mpd

        when (preset) {
            Preset.FULL_SOLAR_SYSTEM -> buildSolarSystem(s)

            Preset.THREE_BODY -> {
                // Lagrange's equilateral configuration: three equal masses on a circle, each moving
                // perpendicular to its radius at the speed that balances the other two. It is a real
                // (unstable) solution, so it stays coherent for a while and then visibly falls apart —
                // which is the point of the scene.
                val m = EngineConstants.M_SUN
                val r = dp(90.0)
                // For an equilateral triangle of side sqrt(3)*r, the net pull on each star is
                // sqrt(3)*G*m/(sqrt(3)*r)^2 directed inward, giving v = sqrt(G*m/(sqrt(3)*r)).
                val v = sqrt(EngineConstants.G * m / (sqrt(3.0) * r))
                for (i in 0 until 3) {
                    val a = i * 2.0 * PI / 3.0
                    s.add(
                        BodyType.SUN, m, 18.0,
                        r * cos(a), r * sin(a),
                        -v * sin(a), v * cos(a),
                        "sun"
                    )
                }
                zeroTotalMomentum(s)
            }

            Preset.EMPTY_TABLE -> {
                // Deliberately nothing. §9 keeps this off the default path: it is a scene the user
                // has to choose, never what the sandbox opens on.
            }

            Preset.SUN_EARTH -> {
                val r = EngineConstants.AU
                val sun = BodyCatalog.SUN
                val earth = BodyCatalog.EARTH
                val v = EngineConstants.circularSpeed(sun.massKg, r)
                s.add(sun.type, sun.massKg, sun.dp, 0.0, 0.0, 0.0, 0.0, sun.key)
                s.add(earth.type, earth.massKg, earth.dp, r, 0.0, 0.0, v, earth.key)
                zeroTotalMomentum(s)
            }

            Preset.EARTH_MOON -> {
                // 18 dp separation: Earth 8 dp + Moon 5 dp gives a 13 dp contact distance, so the two
                // two bodies are legally and visibly separate at every viewport width.
                val earth = BodyCatalog.EARTH
                val moon = BodyCatalog.MOON
                val sep = dp(18.0)
                val mE = earth.massKg
                val mM = moon.massKg
                val total = mE + mM
                val vRel = sqrt(EngineConstants.G * total / sep)
                val xE = -sep * mM / total
                val xM = sep * mE / total
                val vE = -vRel * mM / total
                val vM = vRel * mE / total
                s.add(earth.type, mE, 8.0, xE, 0.0, 0.0, vE, earth.key)
                s.add(moon.type, mM, 5.0, xM, 0.0, 0.0, vM, moon.key)
            }

            Preset.INNER_SYSTEM -> {
                val sun = BodyCatalog.SUN
                s.add(sun.type, sun.massKg, sun.dp, 0.0, 0.0, 0.0, 0.0, sun.key)
                addOrbiter(s, BodyCatalog.MERCURY, 0.387 * EngineConstants.AU, 200.0)
                addOrbiter(s, BodyCatalog.VENUS, 0.723 * EngineConstants.AU, 300.0)
                addOrbiter(s, BodyCatalog.EARTH, EngineConstants.AU, 0.0)
                addOrbiter(s, BodyCatalog.MARS, 1.524 * EngineConstants.AU, 90.0)
                zeroTotalMomentum(s)
            }

            Preset.BINARY -> {
                val sep = dp(120.0)
                val m = EngineConstants.M_SUN
                val vRel = sqrt(EngineConstants.G * 2.0 * m / sep)
                val half = sep * 0.5
                s.add(BodyType.SUN, m, 24.0, -half, 0.0, 0.0, -vRel * 0.5, "sun")
                s.add(BodyType.SUN, m, 24.0, half, 0.0, 0.0, vRel * 0.5, "sun")
                val rP = dp(160.0)
                val vP = sqrt(EngineConstants.G * 2.0 * m / rP)
                s.add(BodyType.PLANET, EngineConstants.M_EARTH, 10.0, 0.0, rP, -vP, 0.0, "earth")
            }

            Preset.BLACK_HOLE_LAB -> {
                val bh = BodyCatalog.BLACK_HOLE
                s.add(bh.type, bh.massKg, bh.dp, 0.0, 0.0, 0.0, 0.0, bh.key)
                val start = dp(170.0)
                val vIn = 0.7 * EngineConstants.escapeSpeed(bh.massKg, start)
                val marble = BodyCatalog.MARBLE
                s.add(marble.type, 0.0, marble.dp, -start, dp(20.0), vIn, 0.0, marble.key)
                s.add(marble.type, 0.0, marble.dp, -start, dp(55.0), vIn, 0.0, marble.key)
                s.add(marble.type, 0.0, marble.dp, -start, dp(95.0), vIn, 0.0, marble.key)
            }

            Preset.WORMHOLE_LAB -> {
                val earth = BodyCatalog.EARTH
                s.add(earth.type, earth.massKg, earth.dp, 0.0, dp(90.0), 0.0, 0.0, earth.key)
                Wormhole.addPair(s, -dp(110.0), -dp(60.0), dp(110.0), dp(60.0))
                // Aimed straight at the near mouth, fast enough to arrive in ~15 s at 1x.
                val marble = BodyCatalog.MARBLE
                s.add(marble.type, 0.0, marble.dp, -dp(110.0), -dp(140.0), 0.0, 6000.0, marble.key)
            }
        }
        NBodyEngine.computeAccelerations(s)
    }

    /**
     * §8 FULL SOLAR SYSTEM — real masses, real semi-major axes, real circular speeds.
     *
     * Nothing here is decorative. Each planet is placed at its true distance from the Sun and given
     * the circular speed that distance implies, `sqrt(GM/r)`, so the orbital periods that emerge
     * are the real ones (Neptune really does take 165 years). Starting angles are spread out purely
     * so the inner planets do not begin in a line.
     *
     * The Moon is a fully independent body: its own mass, its own radius, its own absolute position
     * and its own absolute velocity (Earth's velocity plus its own circular speed about Earth). It
     * is never parented to Earth — the Earth-Moon relationship is an outcome of the integration.
     */
    private fun buildSolarSystem(s: SimArrays) {
        // §8/§11 — this is the one scene laid out at true astronomical scale, so its bodies carry
        // their real SI collision radii. The dp band system that every hand-laid experiment uses
        // would make Earth 1.1e10 m across, thirty times wider than the Moon's whole orbit, and
        // the Moon would be shoved out of orbit on the first step. Drawn size is unaffected.
        val sun = BodyCatalog.SUN
        s.add(
            sun.type, sun.massKg, sun.dp, 0.0, 0.0, 0.0, 0.0, sun.key,
            physicalRadiusM = BodyCatalog.realRadiusOf(sun.key, sun.type, sun.massKg)
        )

        addOrbiter(s, BodyCatalog.MERCURY, 5.7909e10, 15.0, realRadius = true)
        addOrbiter(s, BodyCatalog.VENUS, 1.0821e11, 95.0, realRadius = true)
        val earthR = 1.4960e11
        addOrbiter(s, BodyCatalog.EARTH, earthR, 180.0, realRadius = true)
        addOrbiter(s, BodyCatalog.MARS, 2.2792e11, 250.0, realRadius = true)
        addOrbiter(s, BodyCatalog.JUPITER, 7.7857e11, 40.0, realRadius = true)
        addOrbiter(s, BodyCatalog.SATURN, 1.43353e12, 140.0, realRadius = true)
        addOrbiter(s, BodyCatalog.URANUS, 2.87246e12, 220.0, realRadius = true)
        addOrbiter(s, BodyCatalog.NEPTUNE, 4.49506e12, 310.0, realRadius = true)

        // ---- the Moon: independent state, not attached to anything ------------------------------
        val earthSlot = s.slotOfCatalog(BodyCatalog.EARTH.key)
        if (earthSlot >= 0) {
            val moon = BodyCatalog.MOON
            val rM = EngineConstants.MOON_ORBIT_RADIUS
            val vM = EngineConstants.circularSpeed(s.mass[earthSlot], rM)
            // Placed perpendicular to the Earth-Sun line and moving perpendicular to that offset,
            // which is a real circular orbit about Earth on top of Earth's own motion.
            val ux = -sin(Math.toRadians(180.0))
            val uy = cos(Math.toRadians(180.0))
            s.add(
                moon.type, moon.massKg, moon.dp,
                s.x[earthSlot] + rM * ux, s.y[earthSlot] + rM * uy,
                s.vx[earthSlot] - vM * uy, s.vy[earthSlot] + vM * ux,
                moon.key,
                physicalRadiusM = BodyCatalog.realRadiusOf(moon.key, moon.type, moon.massKg)
            )
        }
        zeroTotalMomentum(s)
    }

    private fun addOrbiter(
        s: SimArrays,
        e: CatalogEntry,
        r: Double,
        angleDeg: Double,
        realRadius: Boolean = false
    ) {
        val a = Math.toRadians(angleDeg)
        val px = r * cos(a)
        val py = r * sin(a)
        val v = EngineConstants.circularSpeed(s.mass[0], r)
        // Counter-clockwise tangential direction.
        val vx = -sin(a) * v
        val vy = cos(a) * v
        s.add(
            e.type, e.massKg, e.dp, px, py, vx, vy, e.key,
            physicalRadiusM = if (realRadius) BodyCatalog.realRadiusOf(e.key, e.type, e.massKg) else 0.0
        )
    }

    /**
     * Removes the net drift of the whole configuration by pushing the residual momentum onto the
     * most massive body, so the experiment stays centred on the table.
     */
    private fun zeroTotalMomentum(s: SimArrays) {
        var px = 0.0
        var py = 0.0
        for (i in 0 until s.n) {
            px += s.mass[i] * s.vx[i]
            py += s.mass[i] * s.vy[i]
        }
        val heaviest = s.dominantAttractor()
        if (heaviest < 0 || s.mass[heaviest] <= 0.0) return
        s.vx[heaviest] -= px / s.mass[heaviest]
        s.vy[heaviest] -= py / s.mass[heaviest]
    }
}

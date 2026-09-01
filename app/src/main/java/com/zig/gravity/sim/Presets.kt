package com.zig.gravity.sim

import com.zig.gravity.physics.BodyType
import com.zig.gravity.physics.EngineConstants
import com.zig.gravity.physics.NBodyEngine
import com.zig.gravity.physics.SimArrays
import com.zig.gravity.physics.Wormhole
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
enum class Preset(val titleFa: String, val titleEn: String, val noteFa: String, val noteEn: String) {
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
    );

    companion object {
        val DEFAULT: Preset = SUN_EARTH
    }
}

object Presets {

    /** Builds [preset] into [s]. The array is cleared first; simTime resets to zero. */
    fun build(preset: Preset, s: SimArrays) {
        s.clear()
        val mpd = s.metersPerDp
        fun dp(v: Double) = v * mpd

        when (preset) {
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

    private fun addOrbiter(s: SimArrays, e: CatalogEntry, r: Double, angleDeg: Double) {
        val a = Math.toRadians(angleDeg)
        val px = r * cos(a)
        val py = r * sin(a)
        val v = EngineConstants.circularSpeed(s.mass[0], r)
        // Counter-clockwise tangential direction.
        val vx = -sin(a) * v
        val vy = cos(a) * v
        s.add(e.type, e.massKg, e.dp, px, py, vx, vy, e.key)
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

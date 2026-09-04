package probes

import com.alijafari.red.astronomy.startracker.catalog.CatalogIngestor
import com.alijafari.red.astronomy.startracker.catalog.CatalogBuildConfig
import com.alijafari.red.astronomy.startracker.catalog.QuadPatternIndex
import com.alijafari.red.astronomy.startracker.solver.LostInSpaceSolver
import com.alijafari.red.astronomy.startracker.solver.Quaternion
import com.alijafari.red.astronomy.startracker.solver.synthetic.SyntheticSkyObserver
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * D (final pass): SYNTHETIC-SKY end-to-end validation of the REAL star-tracker chain:
 * real HYG catalog -> capped quad index (C defaults) -> synthetic observations
 * (SyntheticSkyObserver: noise + false stars) -> LostInSpaceSolver (NO ground-truth
 * hints — full lost-in-space) -> attitude error vs truth + false-lock detection.
 *
 * Every number printed is SYNTHETIC-SKY (never MEASURED-on-device).
 *
 * Usage: probes.SyntheticE2EProbeKt <csv> [trialCount]
 */
fun main(args: Array<String>) {
    val csvPath = args[0]
    val trials = (args.getOrElse(1) { "20" }).toInt()
    val stars = CatalogIngestor.parse(java.io.File(csvPath).readText(), "HYG_V36_LE6P5")
    println("catalog: ${stars.size} stars")
    val t0 = System.currentTimeMillis()
    val quadIndex = QuadPatternIndex.capped(stars) // C defaults: mag<=5.5, K=6, cap 120k
    println("capped index: ${quadIndex.quads.size} quads (${System.currentTimeMillis() - t0} ms)")
    val solver = LostInSpaceSolver(quadIndex, stars)

    // detection limit: the observer sees the full shipped catalog (sensor sees ~V<=6.5)
    val fovLimitRad = Math.toRadians(31.75) // 63.5 deg diagonal FOV (Phase-1 fallback)
    val pxArcsec = 57.0                    // ~4000 px across 63.5 deg

    data class Cell(val noisePx: Double, val falseStars: Int)
    val cells = listOf(
        Cell(0.0, 0), Cell(0.1, 0), Cell(0.3, 0), Cell(1.0, 0), Cell(2.0, 0),
        Cell(0.3, 5), Cell(0.3, 20), Cell(1.0, 20)
    )

    // deterministic random attitudes/fields
    val rng = Random(20260904)
    val attitudes = (0 until trials).map {
        val u1 = rng.nextDouble(); val u2 = rng.nextDouble() * 2 * PI; val u3 = rng.nextDouble() * 2 * PI
        val s1 = sqrt(1.0 - u1); val s2 = sqrt(u1)
        Quaternion(s1 * sin(u2), s1 * cos(u2), s2 * sin(u3), s2 * cos(u3))
    }

    fun angErrDeg(qTrue: Quaternion, qEst: Quaternion): Double {
        // relative rotation angle between quaternions
        var d = 0.0
        val a = doubleArrayOf(qTrue.w, qTrue.x, qTrue.y, qTrue.z)
        val b = doubleArrayOf(qEst.w, qEst.x, qEst.y, qEst.z)
        for (i in 0 until 4) d += a[i] * b[i]
        d = kotlin.math.abs(d).coerceIn(0.0, 1.0)
        return Math.toDegrees(2.0 * kotlin.math.acos(d))
    }

    val falseLockThresholdDeg = 0.5
    println()
    println("cell (noisePx / falseStars) | trials | solved | falseLocks | medianErr' | p95Err' | notes")
    for (cell in cells) {
        val noiseArcsec = cell.noisePx * pxArcsec
        var solved = 0; var falseLocks = 0
        val errs = mutableListOf<Double>()      // correct solves only
        val allErrs = mutableListOf<Double>()   // every solve incl. false locks (Z-V4)
        val failMsgs = HashMap<String, Int>()
        for ((t, qTrue) in attitudes.withIndex()) {
            val obs = SyntheticSkyObserver().observe(
                catalogStars = stars,
                groundTruthAttitude = qTrue,
                fovLimitRad = fovLimitRad,
                noiseSigmaRad = Math.toRadians(noiseArcsec / 3600.0),
                numFalseStars = cell.falseStars,
                seed = 1000L + t
            )
            val res = solver.solve(obs.observations)
            if (res.success && res.attitude != null) {
                solved++
                val e = angErrDeg(qTrue, res.attitude)
                // Z-V4: percentiles must INCLUDE false locks (they are errors too);
                // a separate correct-solves-only list is reported as its own column.
                allErrs.add(e * 60.0)
                if (e > falseLockThresholdDeg) falseLocks++ else errs.add(e * 60.0)
            } else {
                failMsgs[res.errorMessage ?: "?"] = (failMsgs[res.errorMessage ?: "?"] ?: 0) + 1
            }
        }
        errs.sort(); allErrs.sort()
        fun med(v: List<Double>) = if (v.isEmpty()) Double.NaN else v[v.size / 2]
        fun p95(v: List<Double>) = if (v.isEmpty()) Double.NaN else v[(v.size * 95 / 100).coerceAtMost(v.size - 1)]
        val med = med(errs); val p95v = p95(errs)                 // correct solves only
        val medAll = med(allErrs); val p95All = p95(allErrs)       // incl. false locks
        val topFail = failMsgs.entries.sortedByDescending { it.value }.take(1).joinToString { "${it.value}x ${it.key.take(50)}" }
        println(
            "noise=${cell.noisePx}px false=${cell.falseStars.toString().padStart(2)} | $trials | " +
            "$solved | $falseLocks | " +
            "ALL: ${if (medAll.isNaN()) "n/a" else "%9.2f".format(medAll)} / ${if (p95All.isNaN()) "n/a" else "%10.2f".format(p95All)} | " +
            "CORRECT-ONLY: ${if (med.isNaN()) "n/a" else "%8.3f".format(med)} / ${if (p95v.isNaN()) "n/a" else "%8.3f".format(p95v)} | $topFail"
        )
    }
}

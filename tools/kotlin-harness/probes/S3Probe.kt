package probes

import com.alijafari.red.astronomy.startracker.catalog.CatalogIngestor
import com.alijafari.red.astronomy.startracker.catalog.QuadPatternIndex
import com.alijafari.red.astronomy.startracker.solver.LostInSpaceSolver
import com.alijafari.red.astronomy.startracker.solver.Quaternion
import com.alijafari.red.astronomy.startracker.solver.synthetic.SyntheticSkyObserver
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * S3: statistically proper D re-run (SYNTHETIC-SKY, real solver chain, S2 gate active).
 *
 * Seeds are DISJOINT from all tuning runs (D/S1/S2 used seed 20260904 + observer
 * 1000+t; this probe uses attitude seed 777001+mode and observer seed 500000+trial).
 *
 * Modes (args: <csv> <mode> [n]):
 *   baseline : 0.3 px noise, 0 false stars (default operating point)
 *   noise    : one-factor sweep 0 / 0.1 / 0.3 / 0.5 / 1 / 2 / 3 px (0 false)
 *   false    : one-factor sweep 0 / 5 / 10 / 20 / 40 false stars (0.3 px)
 *   joint    : Monte-Carlo, noise ~ U(0,2) px, false stars ~ {0,5,10,20} uniform
 *
 * False lock (>= MARGINAL by construction: every solver success carries >=4 inliers,
 * which the ladder maps to at least MARGINAL_LOCK): solved AND attitude error > 0.5 deg.
 * FULL_LOCK-level false lock additionally needs confidence >= 0.7 AND fullField >= 20.
 */
val PXRAD = 57.0 / 3600.0 // arcsec per px -> deg

fun angErrDeg(qTrue: Quaternion, qEst: Quaternion): Double {
    var d = 0.0
    val a = doubleArrayOf(qTrue.w, qTrue.x, qTrue.y, qTrue.z)
    val b = doubleArrayOf(qEst.w, qEst.x, qEst.y, qEst.z)
    for (i in 0 until 4) d += a[i] * b[i]
    return Math.toDegrees(2.0 * acos(abs(d).coerceIn(0.0, 1.0)))
}

data class Trial(val solved: Boolean, val errArcmin: Double, val conf: Double,
                 val inliers: Int, val ffMatched: Int, val solveMs: Double, val failMsg: String?)

fun main(args: Array<String>) {
    val csv = args[0]; val mode = args[1]; val nDefault = args.getOrElse(2) { "1000" }.toInt()
    val stars = CatalogIngestor.parse(java.io.File(csv).readText(), "HYG_V36_LE6P5")
    val quadIndex = QuadPatternIndex.capped(stars)
    val solver = LostInSpaceSolver(quadIndex, stars)
    val fov = Math.toRadians(31.75)

    fun attitudes(seed: Long, count: Int): List<Quaternion> {
        val rng = Random(seed)
        return (0 until count).map {
            val u1 = rng.nextDouble(); val u2 = rng.nextDouble() * 2 * PI; val u3 = rng.nextDouble() * 2 * PI
            val s1 = sqrt(1.0 - u1); val s2 = sqrt(u1)
            Quaternion(s1 * sin(u2), s1 * cos(u2), s2 * sin(u3), s2 * cos(u3))
        }
    }

    fun runCell(tag: String, noisePx: Double, falseStars: Int, atts: List<Quaternion>, seedBase: Long): List<Trial> {
        val out = ArrayList<Trial>(atts.size)
        var msSum = 0.0
        for ((t, qTrue) in atts.withIndex()) {
            val obsr = SyntheticSkyObserver().observe(stars, qTrue, fov,
                Math.toRadians(noisePx * PXRAD), falseStars, seedBase + t)
            val t0 = System.nanoTime()
            val res = solver.solve(obsr.observations)
            val ms = (System.nanoTime() - t0) / 1e6
            msSum += ms
            if (res.success && res.attitude != null)
                out.add(Trial(true, angErrDeg(qTrue, res.attitude!!) * 60.0, res.confidence,
                    res.inlierCount, res.fullFieldMatched, ms, null))
            else
                out.add(Trial(false, Double.NaN, 0.0, res.inlierCount, res.fullFieldMatched, ms,
                    res.errorMessage ?: "?"))
        }
        // report
        val solved = out.filter { it.solved }
        val fl = solved.filter { it.errArcmin > 30.0 } // >0.5 deg
        val flFull = fl.filter { it.conf >= 0.7 && it.ffMatched >= 20 } // FULL_LOCK-grade false lock
        val sortedErr = solved.map { it.errArcmin }.sorted()
        fun med(v: List<Double>) = if (v.isEmpty()) Double.NaN else v[v.size / 2]
        fun p95(v: List<Double>) = if (v.isEmpty()) Double.NaN else v[(v.size * 95 / 100).coerceAtMost(v.size - 1)]
        val fails = out.filter { !it.solved }.groupingBy { it.failMsg!!.substringBefore(':') }.eachCount()
        println("cell=$tag n=${out.size} solved=${solved.size} FL=$fl FLfull=$flFull " +
            "medianALL=" + (if (sortedErr.isEmpty()) "n/a" else "%.3f".format(med(sortedErr))) + "'" +
            " p95ALL=" + (if (sortedErr.isEmpty()) "n/a" else "%.2f".format(p95(sortedErr))) + "'" +
            " solveMsMean=%.0f solveMsMax=%.0f".format(msSum / out.size, out.maxOf { it.solveMs }) +
            (if (fails.isEmpty()) "" else " fails=$fails"))
        return out
    }

    when (mode) {
        "baseline" -> runCell("baseline 0.3px/0f", 0.3, 0, attitudes(777001L, nDefault), 500000L)
        "noise" -> {
            val atts = attitudes(777002L, nDefault)
            for (px in doubleArrayOf(0.0, 0.1, 0.3, 0.5, 1.0, 2.0, 3.0))
                runCell("noise ${px}px/0f", px, 0, atts, 510000L)
        }
        "false" -> {
            val atts = attitudes(777003L, nDefault)
            for (f in intArrayOf(0, 5, 10, 20, 40))
                runCell("false 0.3px/${f}f", 0.3, f, atts, 520000L)
        }
        "joint" -> {
            // per-trial randomization: noise ~ U(0,2) px, false ~ uniform{0,5,10,20}
            val rng = Random(777004L)
            val atts = attitudes(777004L, nDefault)
            var solved = 0; var fl = 0; var flFull = 0
            val errs = ArrayList<Double>(); var msSum = 0.0
            val confBins = listOf(0.0 to 0.3, 0.3 to 0.5, 0.5 to 0.7, 0.7 to 1.01)
            val binN = IntArray(4); val binFl = IntArray(4)
            for ((t, qTrue) in atts.withIndex()) {
                val px = rng.nextDouble() * 2.0
                val f = listOf(0, 5, 10, 20)[rng.nextInt(4)]
                val obsr = SyntheticSkyObserver().observe(stars, qTrue, fov,
                    Math.toRadians(px * PXRAD), f, 530000L + t)
                val t0 = System.nanoTime()
                val res = solver.solve(obsr.observations)
                msSum += (System.nanoTime() - t0) / 1e6
                if (res.success && res.attitude != null) {
                    solved++
                    val e = angErrDeg(qTrue, res.attitude!!) * 60.0
                    errs.add(e)
                    val isFl = e > 30.0
                    if (isFl) fl++
                    if (isFl && res.confidence >= 0.7 && res.fullFieldMatched >= 20) flFull++
                    for (b in 0 until 4) if (res.confidence >= confBins[b].first && res.confidence < confBins[b].second) {
                        binN[b]++; if (isFl) binFl[b]++
                    }
                }
            }
            errs.sort()
            println("JOINT n=$nDefault solved=$solved FL=$fl (${"%.3f".format(fl * 100.0 / nDefault)}%) FLfull=$flFull " +
                "medianALL=" + (if (errs.isEmpty()) "n/a" else "%.3f".format(errs[errs.size / 2])) + "' " +
                "p95ALL=" + (if (errs.isEmpty()) "n/a" else "%.2f".format(errs[(errs.size * 95 / 100).coerceAtMost(errs.size - 1)])) + "' " +
                "solveMsMean=%.0f".format(msSum / nDefault))
            println("confidence-bin calibration (bin: n, FL rate): " +
                confBins.withIndex().joinToString(", ") { (b, r) ->
                    "[${r.first}-${if (r.second > 1.0) "1.0" else "%.2f".format(r.second)}): n=${binN[b]} FLrate=${if (binN[b] == 0) "n/a" else "%.4f".format(binFl[b].toDouble() / binN[b])}] " })
        }
        else -> error("unknown mode $mode")
    }
}

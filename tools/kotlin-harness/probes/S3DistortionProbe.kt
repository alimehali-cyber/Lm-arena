package probes

import com.alijafari.red.astronomy.startracker.catalog.CatalogIngestor
import com.alijafari.red.astronomy.startracker.catalog.QuadPatternIndex
import com.alijafari.red.astronomy.startracker.solver.FullFieldVerifier
import com.alijafari.red.astronomy.startracker.solver.LostInSpaceSolver
import com.alijafari.red.astronomy.startracker.solver.Quaternion
import com.alijafari.red.astronomy.startracker.solver.StarObservation
import com.alijafari.red.astronomy.startracker.solver.synthetic.SyntheticSkyObserver
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * T4(b): S3 joint Monte-Carlo re-run with UNMODELLED barrel distortion k1 in
 * {0, -0.03, -0.08} applied to the observations, while the solver keeps the T4(b)
 * radial-tolerance verifier (|k1| <= 0.08 envelope, c = 0.08*tan(31.75 deg) = 0.04951).
 * Same seeds as the S3 joint run (attitudes 777004, observer 530000+t, noise U(0,2) px,
 * false stars uniform {0,5,10,20}) so cells are directly comparable.
 * SYNTHETIC-SKY. Usage: probes.S3DistortionProbeKt <csv> [n=10000]
 */
fun angErrDeg(qTrue: Quaternion, qEst: Quaternion): Double {
    var d = 0.0
    val a = doubleArrayOf(qTrue.w, qTrue.x, qTrue.y, qTrue.z)
    val b = doubleArrayOf(qEst.w, qEst.x, qEst.y, qEst.z)
    for (i in 0 until 4) d += a[i] * b[i]
    return Math.toDegrees(2.0 * acos(abs(d).coerceIn(0.0, 1.0)))
}

fun distortObs(o: StarObservation, k1: Double): StarObservation {
    if (k1 == 0.0) return o
    val v = o.unitVectorCamera
    val x = v.first / v.third; val y = v.second / v.third
    val f = 1.0 + k1 * (x * x + y * y) // Brown-Conrady radial, applied to the OBSERVATION
    val xd = x * f; val yd = y * f
    val n = sqrt(xd * xd + yd * yd + 1.0)
    return StarObservation(Triple(xd / n, yd / n, 1.0 / n), o.flux, o.isSaturated, o.id)
}

fun main(args: Array<String>) {
    val stars = CatalogIngestor.parse(java.io.File(args[0]).readText(), "HYG_V36_LE6P5")
    val quadIndex = QuadPatternIndex.capped(stars)
    val solver = LostInSpaceSolver(
        quadIndex, stars,
        fullFieldVerifier = FullFieldVerifier.withUnmodelledDistortionAllowance()
    )
    val n = args.getOrElse(1) { "10000" }.toInt()
    val fov = Math.toRadians(31.75)
    val pxr = 57.0 / 3600.0

    val rngAtt = Random(777004L)
    val atts = (0 until n).map {
        val u1 = rngAtt.nextDouble(); val u2 = rngAtt.nextDouble() * 2 * PI; val u3 = rngAtt.nextDouble() * 2 * PI
        val s1 = sqrt(1.0 - u1); val s2 = sqrt(u1)
        Quaternion(s1 * sin(u2), s1 * kotlin.math.cos(u2), s2 * sin(u3), s2 * kotlin.math.cos(u3))
    }

    for (k1 in doubleArrayOf(0.0, -0.03, -0.08)) {
        val rng = Random(777004L) // same per-trial randomization stream as the S3 joint run
        var solved = 0; var fl = 0; var flFull = 0
        val errs = ArrayList<Double>(); var msSum = 0.0
        for ((t, qTrue) in atts.withIndex()) {
            val px = rng.nextDouble() * 2.0
            val f = listOf(0, 5, 10, 20)[rng.nextInt(4)]
            val obsr = SyntheticSkyObserver().observe(stars, qTrue, fov, Math.toRadians(px * pxr), f, 530000L + t)
            val distorted = obsr.observations.map { distortObs(it, k1) }
            val t0 = System.nanoTime()
            val res = solver.solve(distorted)
            msSum += (System.nanoTime() - t0) / 1e6
            if (res.success && res.attitude != null) {
                solved++
                val e = angErrDeg(qTrue, res.attitude!!) * 60.0
                errs.add(e)
                if (e > 30.0) {
                    fl++
                    if (res.confidence >= 0.7 && res.fullFieldMatched >= 20) flFull++
                }
            }
        }
        errs.sort()
        val med = if (errs.isEmpty()) Double.NaN else errs[errs.size / 2]
        val p95 = if (errs.isEmpty()) Double.NaN else errs[(errs.size * 95 / 100).coerceAtMost(errs.size - 1)]
        println("k1=$k1 n=$n solved=$solved (${ "%.1f".format(solved * 100.0 / n) }%) FL=$fl (${ "%.3f".format(fl * 100.0 / n) }%) FLfull=$flFull medianALL=" + (if (errs.isEmpty()) "n/a" else "%.3f".format(med)) + "' p95ALL=" + (if (errs.isEmpty()) "n/a" else "%.2f".format(p95)) + "' solveMsMean=%.0f".format(msSum / n))
    }
}

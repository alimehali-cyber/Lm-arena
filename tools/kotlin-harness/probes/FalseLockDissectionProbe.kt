package probes

import com.alijafari.red.astronomy.startracker.catalog.CatalogIngestor
import com.alijafari.red.astronomy.startracker.catalog.QuadPatternIndex
import com.alijafari.red.astronomy.startracker.solver.Correspondence
import com.alijafari.red.astronomy.startracker.solver.LostInSpaceSolver
import com.alijafari.red.astronomy.startracker.solver.Quaternion
import com.alijafari.red.astronomy.startracker.solver.synthetic.SyntheticSkyObserver
import kotlin.math.*

/**
 * S1: dissect the zero-noise false locks of the D ladder (cell 0px/0false).
 * Replays the exact ladder generation (seed 20260904, 20 attitudes, observe seed 1000+t),
 * then instruments quad matching / RANSAC / attitude for every false lock (err > 0.5 deg).
 * SYNTHETIC-SKY. Usage: probes.FalseLockDissectionProbeKt <csv>
 */
fun qToR(q: Quaternion): Array<DoubleArray> {
    val (w, x, y, z) = listOf(q.w, q.x, q.y, q.z)
    return arrayOf(
        doubleArrayOf(1 - 2 * (y * y + z * z), 2 * (x * y - z * w), 2 * (x * z + y * w)),
        doubleArrayOf(2 * (x * y + z * w), 1 - 2 * (x * x + z * z), 2 * (y * z - x * w)),
        doubleArrayOf(2 * (x * z - y * w), 2 * (y * z + x * w), 1 - 2 * (x * x + y * y)))
}
fun rot(R: Array<DoubleArray>, v: Triple<Double, Double, Double>) = Triple(
    R[0][0] * v.first + R[0][1] * v.second + R[0][2] * v.third,
    R[1][0] * v.first + R[1][1] * v.second + R[1][2] * v.third,
    R[2][0] * v.first + R[2][1] * v.second + R[2][2] * v.third)
fun ang(a: Triple<Double, Double, Double>, b: Triple<Double, Double, Double>): Double {
    val d = (a.first * b.first + a.second * b.second + a.third * b.third).coerceIn(-1.0, 1.0)
    return acos(d) * 206264.806 // arcsec
}
fun angErrDeg(qTrue: Quaternion, qEst: Quaternion): Double {
    var d = 0.0
    val a = doubleArrayOf(qTrue.w, qTrue.x, qTrue.y, qTrue.z)
    val b = doubleArrayOf(qEst.w, qEst.x, qEst.y, qEst.z)
    for (i in 0 until 4) d += a[i] * b[i]
    return Math.toDegrees(2.0 * acos(abs(d).coerceIn(0.0, 1.0)))
}
fun main(args: Array<String>) {
    val stars = CatalogIngestor.parse(java.io.File(args[0]).readText(), "HYG_V36_LE6P5")
    val quadIndex = QuadPatternIndex.capped(stars)
    val solver = LostInSpaceSolver(quadIndex, stars)
    val fovLimitRad = Math.toRadians(31.75)
    val rng = kotlin.random.Random(20260904)
    val attitudes = (0 until 20).map {
        val u1 = rng.nextDouble(); val u2 = rng.nextDouble() * 2 * PI; val u3 = rng.nextDouble() * 2 * PI
        val s1 = sqrt(1.0 - u1); val s2 = sqrt(u1)
        Quaternion(s1 * sin(u2), s1 * cos(u2), s2 * sin(u3), s2 * cos(u3))
    }
    for ((t, qTrue) in attitudes.withIndex()) {
        val obsr = SyntheticSkyObserver().observe(stars, qTrue, fovLimitRad, 0.0, 0, 1000L + t)
        val res = solver.solve(obsr.observations)
        if (!(res.success && res.attitude != null)) continue
        val err = angErrDeg(qTrue, res.attitude!!)
        if (err <= 0.5) continue
        println("=" .repeat(100))
        println("FALSE LOCK trial=$t attitudeErr=%.3f deg  solverInliers=${res.inlierCount} confidence=%.3f obsCount=${obsr.observations.size}".format(err, res.confidence))
        // replay pipeline stages
        val cands = solver.quadBuilder.buildLocalCandidates(obsr.observations)
        val matches = solver.matcher.matchQuads(cands, solver.catalogStarsById)
        println("quad candidates=${cands.size} quad matches=${matches.size}")
        for ((mi, m) in matches.withIndex()) {
            val obsDesc = quadIndex.computeDescriptorFromUnitVectors(m.observedQuad.observations.map { it.unitVectorCamera })
            val cd = m.catalogQuad.descriptor
            val dInf = maxOf(abs(obsDesc.ratios[0] - cd.ratios[0]), abs(obsDesc.ratios[1] - cd.ratios[1]),
                abs(obsDesc.ratios[2] - cd.ratios[2]), abs(obsDesc.ratios[3] - cd.ratios[3]), abs(obsDesc.ratios[4] - cd.ratios[4]))
            val corr = m.correspondences.joinToString(", ") { (o, c) ->
                val truth = obsr.trueCorrespondences[o.id]
                "${o.id}->${c.id}${if (truth?.id == c.id) "=TRUE" else "(truth=${truth?.id ?: "none"})"}"
            }
            println("  match[$mi] descLinf=%.5f descCat=%.4f.. obs:[${obsDesc.ratios.joinToString { "%.3f".format(it) }}] corr: $corr".format(dInf, cd.maxSeparationRad))
        }
        val all = matches.flatMap { m -> m.correspondences.map { (o, c) -> Correspondence(o, c, c.toUnitVector()) } }
        val deduped = all.distinctBy { corr -> if (corr.observed.id.isNotBlank()) "id:${corr.observed.id}" else "ref:${System.identityHashCode(corr.observed)}" }
        val rr = solver.ransac.rejectOutliers(deduped, solver.attitudeSolver)
        println("deduped=${deduped.size} ransacInliers=${rr.inlierCount} ransacOutliers=${rr.outliers.size} (threshold=${solver.ransac.inlierThresholdRad} rad = %.1f arcsec)".format(solver.ransac.inlierThresholdRad * 206264.806))
        // residuals under returned attitude + truth attitude; truth-consistency of inliers
        val Rret = qToR(res.attitude!!); val Rtrue = qToR(qTrue)
        var rmsRet = 0.0; var nTrue = 0
        for (c in rr.inliers) {
            val rRet = ang(rot(Rret, c.catalogUnitVector), c.observed.unitVectorCamera)
            rmsRet += rRet * rRet
            val truth = obsr.trueCorrespondences[c.observed.id]
            val rTru = if (truth != null) ang(rot(Rtrue, truth.toUnitVector()), c.observed.unitVectorCamera) else -1.0
            if (truth?.id == c.catalogStar.id) nTrue++
            println("   inlier ${c.observed.id}->${c.catalogStar.id} residualUnderReturned=%9.1f\" residualUnderTruth=%9.1f\" ${if (truth?.id == c.catalogStar.id) "TRUE-CORR" else "WRONG-CORR(truth=" + (truth?.id ?: "none") + ")"}".format(rRet, rTru))
        }
        rmsRet = sqrt(rmsRet / rr.inliers.size.coerceAtLeast(1))
        println("inliers rmsUnderReturned=" + "%.1f".format(rmsRet) + "\" trueCorrespondencesAmongInliers=$nTrue/${rr.inlierCount}")
        // REFLECTION TESTS on the RANSAC inlier set
        val corrPairs = rr.inliers.map { it.catalogUnitVector to it.observed.unitVectorCamera }
        val B = Array(3) { DoubleArray(3) { 0.0 } }
        for ((b, r) in corrPairs) { for (i in 0..2) for (j in 0..2) B[i][j] += b.toList()[i] * r.toList()[j] }
        val detB = B[0][0] * (B[1][1] * B[2][2] - B[1][2] * B[2][1]) - B[0][1] * (B[1][0] * B[2][2] - B[1][2] * B[2][0]) + B[0][2] * (B[1][0] * B[2][1] - B[1][1] * B[2][0])
        println("Wahba det(B) over inliers = %.4f  -> ${if (detB < 0) "IMPROPER (reflection) fit" else "proper rotation fit"}".format(detB))
        fun rmsFor(tf: (Triple<Double, Double, Double>) -> Triple<Double, Double, Double>): Double {
            val pairs = corrPairs.map { (b, r) -> b to tf(r) }
            val q = solver.attitudeSolver.solveDavenportQMethod(pairs, List(pairs.size) { 1.0 })
            val R = qToR(q); var s = 0.0
            for ((b, r) in pairs) { val a = ang(rot(R, b), r); s += a * a }
            return sqrt(s / pairs.size)
        }
        val rId = rmsFor { it }
        val rX = rmsFor { Triple(-it.first, it.second, it.third) }
        val rY = rmsFor { Triple(it.first, -it.second, it.third) }
        val rZ = rmsFor { Triple(it.first, it.second, -it.third) }
        println("refit rms: identity=%9.2f\"  flipX=%9.2f\"  flipY=%9.2f\"  flipZ=%9.2f\"".format(rId, rX, rY, rZ))
    }
    println("=".repeat(100))
}

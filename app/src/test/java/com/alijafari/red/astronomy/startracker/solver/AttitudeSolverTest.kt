package com.alijafari.red.astronomy.startracker.solver

import org.junit.Test
import org.junit.Assert.*
import kotlin.math.*

class AttitudeSolverTest {

    @Test
    fun testDavenportZeroNoise() {
        // Ground truth 30° yaw
        val gtQ = Quaternion.fromAxisAngle(Triple(0.0, 0.0, 1.0), 30.0 * PI / 180.0)

        val catVectors = listOf(
            Triple(1.0, 0.0, 0.0),
            Triple(0.0, 1.0, 0.0),
            Triple(0.0, 0.0, 1.0),
            Triple(0.707, 0.707, 0.0)
        )

        val obsVectors = catVectors.map { gtQ.rotateVector(it) }

        val correspondences = catVectors.zip(obsVectors)
        val weights = List(correspondences.size) { 1.0 }

        val solver = AttitudeSolver()
        val solvedQ = solver.solveDavenportQMethod(correspondences, weights)

        // Angular error should be ~0 for zero noise
        val dot = abs(solvedQ.w * gtQ.w + solvedQ.x * gtQ.x + solvedQ.y * gtQ.y + solvedQ.z * gtQ.z).coerceIn(-1.0, 1.0)
        val angleErr = 2 * acos(dot)
        val angleErrDeg = angleErr * 180 / PI

        println("Davenport zero noise error: $angleErrDeg° = ${angleErrDeg*3600} arcsec")
        assertTrue("Zero noise error should be <0.001°", angleErrDeg < 0.001)
    }

    @Test
    fun testDavenportWithNoise() {
        val gtQ = Quaternion.fromAxisAngle(Triple(0.0, 0.0, 1.0), 30.0 * PI / 180.0)

        val catVectors = listOf(
            Triple(1.0, 0.0, 0.0),
            Triple(0.0, 1.0, 0.0),
            Triple(0.0, 0.0, 1.0),
            Triple(0.707, 0.707, 0.0)
        )

        val obsVectors = catVectors.map { gtQ.rotateVector(it) }

        // Add 0.01° noise
        val noiseSigma = 0.01 * PI / 180.0
        val rnd = java.util.Random(42)
        val noisyObs = obsVectors.map { v ->
            val axis = Triple(rnd.nextDouble()-0.5, rnd.nextDouble()-0.5, rnd.nextDouble()-0.5)
            val cross = Triple(
                v.second * axis.third - v.third * axis.second,
                v.third * axis.first - v.first * axis.third,
                v.first * axis.second - v.second * axis.first
            )
            val norm = sqrt(cross.first*cross.first + cross.second*cross.second + cross.third*cross.third)
            if (norm < 1e-9) return@map v
            val unit = Triple(cross.first/norm, cross.second/norm, cross.third/norm)
            val angle = rnd.nextGaussian() * noiseSigma
            val qNoise = Quaternion.fromAxisAngle(unit, angle)
            qNoise.rotateVector(v)
        }

        val correspondences = catVectors.zip(noisyObs)
        val solver = AttitudeSolver()
        val solvedQ = solver.solveDavenportQMethod(correspondences, List(correspondences.size){1.0})

        val dot = abs(solvedQ.w * gtQ.w + solvedQ.x * gtQ.x + solvedQ.y * gtQ.y + solvedQ.z * gtQ.z).coerceIn(-1.0, 1.0)
        val angleErr = 2 * acos(dot) * 180/PI

        println("Davenport with 0.01° noise error: $angleErr° = ${angleErr*3600} arcsec")
        // With 0.01° noise, expect ~0.007° error (27 arcsec) per Python reference
        assertTrue("Noise 0.01° should give error <0.1°", angleErr < 0.1)
    }

    @Test
    fun testTriad() {
        val gtQ = Quaternion.fromAxisAngle(Triple(0.0, 0.0, 1.0), 30.0 * PI / 180.0)

        val v1Cat = Triple(1.0, 0.0, 0.0)
        val v2Cat = Triple(0.0, 1.0, 0.0)
        val v1Obs = gtQ.rotateVector(v1Cat)
        val v2Obs = gtQ.rotateVector(v2Cat)

        val solver = AttitudeSolver()
        val solvedQ = solver.solveTriad(v1Cat, v2Cat, v1Obs, v2Obs)

        val dot = abs(solvedQ.w * gtQ.w + solvedQ.x * gtQ.x + solvedQ.y * gtQ.y + solvedQ.z * gtQ.z).coerceIn(-1.0, 1.0)
        val angleErr = 2 * acos(dot) * 180/PI

        println("TRIAD error: $angleErr°")
        assertTrue("TRIAD zero noise should be <0.001°", angleErr < 0.001)
    }

    @Test
    fun testJacobiEigenDecomposition() {
        // Worked example from comments: M = [[2,1,0,0],[1,2,0,0],[0,0,3,0],[0,0,0,4]]
        // Eigenvalues: 1,3,3,4
        val M = arrayOf(
            doubleArrayOf(2.0, 1.0, 0.0, 0.0),
            doubleArrayOf(1.0, 2.0, 0.0, 0.0),
            doubleArrayOf(0.0, 0.0, 3.0, 0.0),
            doubleArrayOf(0.0, 0.0, 0.0, 4.0)
        )

        val solver = AttitudeSolver()
        val (eigenvalues, _) = solver.jacobiEigenDecomposition(M)

        val sorted = eigenvalues.sorted()
        println("Jacobi eigenvalues: ${sorted}")
        assertEquals(1.0, sorted[0], 0.001)
        assertEquals(3.0, sorted[1], 0.001)
        assertEquals(3.0, sorted[2], 0.001)
        assertEquals(4.0, sorted[3], 0.001)
    }
}

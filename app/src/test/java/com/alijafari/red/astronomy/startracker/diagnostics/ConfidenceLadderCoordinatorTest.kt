package com.alijafari.red.astronomy.startracker.diagnostics

import com.alijafari.red.astronomy.startracker.tracking.LockConfidence
import org.junit.Test
import org.junit.Assert.*

class ConfidenceLadderCoordinatorTest {

    @Test
    fun testFullLock() {
        val coordinator = ConfidenceLadderCoordinator()
        val input = CoordinatorInput(
            frameQuality = FrameQuality.GOOD,
            failureReason = null,
            ambiguityResult = AmbiguityResult(AmbiguityDecision.CLEAR_WINNER, Hypothesis(0, 0.9, 10, 0.9), Hypothesis(1, 0.3, 3, 0.3), 0.33, null),
            solverDiagnostics = SolverDiagnostics(10, 0.9, 0.2, true)
        )

        val output = coordinator.coordinate(input)
        println("Full lock: ${output.lockConfidence}, guidance=${output.guidanceHint}, msg=${output.message}")
        assertEquals(LockConfidence.FULL_LOCK, output.lockConfidence)
        assertEquals(UserGuidanceHint.NONE, output.guidanceHint)
    }

    @Test
    fun testMarginalLock() {
        val coordinator = ConfidenceLadderCoordinator()
        val input = CoordinatorInput(
            frameQuality = FrameQuality.GOOD,
            failureReason = null,
            ambiguityResult = AmbiguityResult(AmbiguityDecision.CLEAR_WINNER, Hypothesis(0, 0.6, 3, 0.6), null, 0.0, null),
            solverDiagnostics = SolverDiagnostics(3, 0.6, 0.3, true)
        )

        val output = coordinator.coordinate(input)
        println("Marginal lock: ${output.lockConfidence}, guidance=${output.guidanceHint}")
        assertEquals(LockConfidence.MARGINAL_LOCK, output.lockConfidence)
    }

    @Test
    fun testAmbiguousGoesToNoLock() {
        val coordinator = ConfidenceLadderCoordinator()
        val ambiguity = AmbiguityResult(
            decision = AmbiguityDecision.AMBIGUOUS,
            bestHypothesis = Hypothesis(0, 0.8, 5, 0.8),
            secondBest = Hypothesis(1, 0.78, 5, 0.78),
            scoreRatio = 0.975,
            failureReason = FailureReason.AmbiguousSolution(0.8, 0.78, 0.975)
        )

        val input = CoordinatorInput(
            frameQuality = FrameQuality.GOOD,
            failureReason = null,
            ambiguityResult = ambiguity,
            solverDiagnostics = SolverDiagnostics(5, 0.8, 0.2, true)
        )

        val output = coordinator.coordinate(input)
        println("Ambiguous: ${output.lockConfidence}, guidance=${output.guidanceHint}")
        assertEquals(LockConfidence.AMBIGUOUS, output.lockConfidence)
        assertTrue(output.shouldAttemptRelock)
    }

    @Test
    fun testNoStars() {
        val coordinator = ConfidenceLadderCoordinator()
        val input = CoordinatorInput(
            frameQuality = FrameQuality.POOR_LOW_STARS,
            failureReason = FailureReason.NoStarsDetected,
            ambiguityResult = AmbiguityResult(AmbiguityDecision.NO_HYPOTHESIS, null, null, 0.0, FailureReason.NoStarsDetected),
            solverDiagnostics = null
        )

        val output = coordinator.coordinate(input)
        println("No stars: ${output.lockConfidence}, guidance=${output.guidanceHint}")
        assertEquals(LockConfidence.NO_LOCK, output.lockConfidence)
        assertEquals(UserGuidanceHint.POINT_TO_DARK_SKY, output.guidanceHint)
    }

    @Test
    fun testHighResidualDowngrade() {
        val coordinator = ConfidenceLadderCoordinator()
        val input = CoordinatorInput(
            frameQuality = FrameQuality.GOOD,
            failureReason = null,
            ambiguityResult = AmbiguityResult(AmbiguityDecision.CLEAR_WINNER, Hypothesis(0, 0.9, 10, 0.9), null, 0.0, null),
            solverDiagnostics = SolverDiagnostics(10, 0.9, 5.0, true) // high RMS
        )

        val output = coordinator.coordinate(input)
        println("High residual: ${output.lockConfidence}, guidance=${output.guidanceHint}, reason=${output.failureReason}")
        assertEquals(LockConfidence.MARGINAL_LOCK, output.lockConfidence)
        assertTrue(output.failureReason is FailureReason.HighResidualError)
    }

    @Test
    fun testDecisionTableAllCases() {
        val coordinator = ConfidenceLadderCoordinator()
        println("\nDecision table:")
        println("FrameQuality | FailureReason | Inliers | RMS | -> Confidence | Guidance")

        val cases = listOf(
            Triple(FrameQuality.GOOD, null as FailureReason?, SolverDiagnostics(10, 0.9, 0.2, true)),
            Triple(FrameQuality.GOOD, null, SolverDiagnostics(3, 0.6, 0.3, true)),
            Triple(FrameQuality.POOR_LOW_STARS, FailureReason.NoStarsDetected as FailureReason?, null),
            Triple(FrameQuality.POOR_HIGH_NOISE, FailureReason.LowFrameQuality(FrameQuality.POOR_HIGH_NOISE), null),
            Triple(FrameQuality.GOOD, null, SolverDiagnostics(1, 0.3, 0.5, true))
        )

        for ((quality, failure, solver) in cases) {
            val input = CoordinatorInput(
                frameQuality = quality,
                failureReason = failure,
                ambiguityResult = null,
                solverDiagnostics = solver
            )
            val output = coordinator.coordinate(input)
            println("$quality | $failure | ${solver?.inlierCount} | ${solver?.rmsError} | -> ${output.lockConfidence} | ${output.guidanceHint}")
        }
    }
}

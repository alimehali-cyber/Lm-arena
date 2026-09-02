package com.alijafari.red.astronomy.startracker.diagnostics

import org.junit.Test
import org.junit.Assert.*

class AmbiguityDetectorTest {

    @Test
    fun testClearWinner() {
        val detector = AmbiguityDetector()
        val best = Hypothesis(0, 0.9, 10, 0.9)
        val second = Hypothesis(1, 0.3, 3, 0.3)

        val result = detector.detectTwo(best, second, higherScoreIsBetter = true)
        println("Clear winner: ${result.decision}, ratio=${result.scoreRatio}")
        assertEquals(AmbiguityDecision.CLEAR_WINNER, result.decision)
    }

    @Test
    fun testAmbiguousCloseScores() {
        val detector = AmbiguityDetector()
        val best = Hypothesis(0, 0.85, 5, 0.85)
        val second = Hypothesis(1, 0.80, 5, 0.80)

        val result = detector.detectTwo(best, second, higherScoreIsBetter = true)
        println("Ambiguous close: ${result.decision}, ratio=${result.scoreRatio}")
        assertEquals(AmbiguityDecision.AMBIGUOUS, result.decision)
        assertNotNull(result.failureReason)
        assertTrue(result.failureReason is FailureReason.AmbiguousSolution)
    }

    @Test
    fun testNoHypothesis() {
        val detector = AmbiguityDetector()
        val result = detector.detect(emptyList())
        println("No hypothesis: ${result.decision}")
        assertEquals(AmbiguityDecision.NO_HYPOTHESIS, result.decision)
    }

    @Test
    fun testSingleHypothesis() {
        val detector = AmbiguityDetector()
        val best = Hypothesis(0, 0.9, 10, 0.9)
        val result = detector.detect(listOf(best))
        println("Single hypothesis: ${result.decision}")
        assertEquals(AmbiguityDecision.CLEAR_WINNER, result.decision)
    }

    @Test
    fun testTwoHypothesisSameInliersDifferentConfidence() {
        val detector = AmbiguityDetector()
        // Same inliers but close confidence -> ambiguous
        val best = Hypothesis(0, 0.75, 4, 0.75)
        val second = Hypothesis(1, 0.74, 4, 0.74)

        val result = detector.detectTwo(best, second)
        println("Same inliers close conf: ${result.decision}, ratio=${result.scoreRatio}")
        assertEquals(AmbiguityDecision.AMBIGUOUS, result.decision)
    }
}

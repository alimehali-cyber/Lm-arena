package com.alijafari.red.astronomy.startracker.diagnostics

import kotlin.math.abs

/**
 * Detects ambiguous solutions when two hypotheses have close scores.
 * Pure Kotlin, no Android dependency.
 */

data class Hypothesis(
    val attitudeId: Int, // identifier for attitude (could be index)
    val score: Double, // higher is better (e.g., inlier count weighted by confidence) OR lower is better (residual) depending on mode
    val inlierCount: Int,
    val confidence: Double
)

enum class AmbiguityDecision {
    CLEAR_WINNER, // best hypothesis clearly better than second
    AMBIGUOUS, // two hypotheses too close, cannot decide
    NO_HYPOTHESIS // no valid hypothesis
}

data class AmbiguityResult(
    val decision: AmbiguityDecision,
    val bestHypothesis: Hypothesis?,
    val secondBest: Hypothesis?,
    val scoreRatio: Double, // best/second or second/best depending on mode, closer to 1 = more ambiguous
    val failureReason: FailureReason?
)

class AmbiguityDetector(
    val scoreRatioThreshold: Double = 0.8, // if second/best > threshold (close to 1), ambiguous. Conservative default UNVALIDATED
    val minScoreDifference: Double = 0.1, // absolute difference threshold
    val minInlierDifference: Int = 2 // need at least this many more inliers to be clear winner
) {

    /**
     * Two-hypothesis test: given sorted hypotheses (best first), decide if ambiguous.
     * Assumes higher score is better (e.g., confidence). For residual (lower better), invert.
     */
    fun detect(
        hypotheses: List<Hypothesis>,
        higherScoreIsBetter: Boolean = true
    ): AmbiguityResult {
        if (hypotheses.isEmpty()) {
            return AmbiguityResult(
                decision = AmbiguityDecision.NO_HYPOTHESIS,
                bestHypothesis = null,
                secondBest = null,
                scoreRatio = 0.0,
                failureReason = FailureReason.NoStarsDetected
            )
        }

        if (hypotheses.size == 1) {
            return AmbiguityResult(
                decision = AmbiguityDecision.CLEAR_WINNER,
                bestHypothesis = hypotheses[0],
                secondBest = null,
                scoreRatio = 0.0,
                failureReason = null
            )
        }

        // Sort by score descending if higher is better, ascending if lower is better
        val sorted = if (higherScoreIsBetter) {
            hypotheses.sortedByDescending { it.score }
        } else {
            hypotheses.sortedBy { it.score }
        }

        val best = sorted[0]
        val second = sorted[1]

        val ratio = if (higherScoreIsBetter) {
            if (best.score == 0.0) 0.0 else second.score / best.score
        } else {
            if (second.score == 0.0) 0.0 else best.score / second.score
        }

        val scoreDiff = abs(best.score - second.score)
        val inlierDiff = best.inlierCount - second.inlierCount

        // Ambiguous if ratio close to 1 AND score diff small AND inlier diff small
        val isAmbiguous = ratio > scoreRatioThreshold && scoreDiff < minScoreDifference * 10 // relaxed absolute check
            && inlierDiff < minInlierDifference

        // More conservative: also check if confidence difference small
        val confidenceDiff = abs(best.confidence - second.confidence)
        val isConfidenceClose = confidenceDiff < 0.2

        val finalAmbiguous = if (higherScoreIsBetter) {
            ratio > scoreRatioThreshold && isConfidenceClose
        } else {
            ratio > scoreRatioThreshold
        }

        return if (finalAmbiguous) {
            AmbiguityResult(
                decision = AmbiguityDecision.AMBIGUOUS,
                bestHypothesis = best,
                secondBest = second,
                scoreRatio = ratio,
                failureReason = FailureReason.AmbiguousSolution(best.score, second.score, ratio)
            )
        } else {
            AmbiguityResult(
                decision = AmbiguityDecision.CLEAR_WINNER,
                bestHypothesis = best,
                secondBest = second,
                scoreRatio = ratio,
                failureReason = null
            )
        }
    }

    /**
     * Simplified two-hypothesis direct comparison.
     */
    fun detectTwo(
        best: Hypothesis,
        second: Hypothesis,
        higherScoreIsBetter: Boolean = true
    ): AmbiguityResult {
        return detect(listOf(best, second), higherScoreIsBetter)
    }
}

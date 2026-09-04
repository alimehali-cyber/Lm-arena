package com.alijafari.red.astronomy.startracker.diagnostics

import com.alijafari.red.astronomy.startracker.tracking.LockConfidence

/**
 * Confidence Ladder Coordinator — decision table mapping diagnostics to confidence and guidance.
 * Pure Kotlin, no Android dependency.
 *
 * Finalizes Phase 8: takes inputs from FailureReason, FrameQuality, AmbiguityDetector, and solver result
 * and produces final LockConfidence + UserGuidanceHint.
 *
 * Decision table (documented):
 *
 * | FrameQuality | FailureReason | Ambiguity | Solver Inliers | Output Confidence | Guidance |
 * | GOOD | null | CLEAR_WINNER | >=4 high conf >=0.7 | FULL_LOCK | NONE |
 * | GOOD | null | CLEAR_WINNER | 2-3 | MARGINAL_LOCK | HOLD_STEADY |
 * | GOOD | Ambiguous | AMBIGUOUS | any | AMBIGUOUS -> NO_LOCK | HOLD_STEADY |
 * | GOOD | HighResidual | CLEAR | >=2 but high RMS | MARGINAL_LOCK | CALIBRATE_COMPASS |
 * | POOR_LOW_STARS | TooFew | NO_HYPOTHESIS | <2 | NO_LOCK | POINT_TO_DARK_SKY |
 * | POOR_HIGH_NOISE | LowQuality | any | any | NO_LOCK | DARKER_ENVIRONMENT |
 * | POOR_BLUR | LowQuality | any | any | NO_LOCK | HOLD_STEADY |
 * | POOR_OVEREXPOSED | LowQuality | any | any | NO_LOCK | DARKER_ENVIRONMENT |
 * | any | NoStars | NO_HYPOTHESIS | 0 | NO_LOCK | POINT_TO_DARK_SKY |
 * | any | Timeout | any | any | NO_LOCK | HOLD_STEADY |
 * | any | GyroStale | any | any | NO_LOCK (with decay) | MOVE_SLOWLY |
 *
 * This coordinator is the final arbiter before AttitudeBlender.
 */

data class SolverDiagnostics(
    val inlierCount: Int,
    val confidence: Double,
    val rmsError: Double,
    val success: Boolean,
    // S2 full-field verification (defaults preserve legacy behavior for old callers:
    // absent data cannot constrain the ladder).
    val fullFieldMatched: Int = Int.MAX_VALUE,
    val fullFieldFraction: Double = 1.0
)

data class CoordinatorInput(
    val frameQuality: FrameQuality,
    val failureReason: FailureReason?,
    val ambiguityResult: AmbiguityResult?,
    val solverDiagnostics: SolverDiagnostics?
)

data class CoordinatorOutput(
    val lockConfidence: LockConfidence,
    val guidanceHint: UserGuidanceHint,
    val failureReason: FailureReason?,
    val shouldAttemptRelock: Boolean,
    val message: String // debug message, not UI string
)

class ConfidenceLadderCoordinator(
    val fullLockInlierThreshold: Int = 4,
    val marginalInlierThreshold: Int = 2,
    val fullLockConfidenceThreshold: Double = 0.7,
    val rmsErrorThreshold: Double = 1.0, // pixel RMS, UNVALIDATED conservative default
    // S2: FULL_LOCK additionally requires a strongly verified full field
    // (stricter than the solver-level MARGINAL gate of 8 / 0.25).
    val fullLockFullFieldMinCount: Int = 20,
    val fullLockFullFieldMinFraction: Double = 0.5
) {

    fun coordinate(input: CoordinatorInput): CoordinatorOutput {
        val frameQuality = input.frameQuality
        val failureReason = input.failureReason
        val ambiguity = input.ambiguityResult
        val solver = input.solverDiagnostics

        // Rule 1: Ambiguous always -> NO_LOCK (never guess)
        if (ambiguity?.decision == AmbiguityDecision.AMBIGUOUS) {
            return CoordinatorOutput(
                lockConfidence = LockConfidence.AMBIGUOUS,
                guidanceHint = UserGuidanceHint.HOLD_STEADY,
                failureReason = ambiguity.failureReason,
                shouldAttemptRelock = true,
                message = "Ambiguous: best=${ambiguity.bestHypothesis?.score} second=${ambiguity.secondBest?.score} ratio=${ambiguity.scoreRatio}"
            )
        }

        // Rule 2: FailureReason present that indicates no stars
        if (failureReason is FailureReason.NoStarsDetected) {
            return CoordinatorOutput(
                lockConfidence = LockConfidence.NO_LOCK,
                guidanceHint = failureReason.toUserGuidanceHint(),
                failureReason = failureReason,
                shouldAttemptRelock = false, // need better frame first
                message = "No stars detected, frameQuality=$frameQuality"
            )
        }

        if (failureReason is FailureReason.TooFewStars) {
            return CoordinatorOutput(
                lockConfidence = LockConfidence.NO_LOCK,
                guidanceHint = failureReason.toUserGuidanceHint(),
                failureReason = failureReason,
                shouldAttemptRelock = false,
                message = "Too few stars: ${failureReason.detectedCount} < ${failureReason.minimumRequired}"
            )
        }

        if (failureReason is FailureReason.LowFrameQuality) {
            return CoordinatorOutput(
                lockConfidence = LockConfidence.NO_LOCK,
                guidanceHint = failureReason.toUserGuidanceHint(),
                failureReason = failureReason,
                shouldAttemptRelock = false,
                message = "Low frame quality: ${failureReason.quality}"
            )
        }

        // Rule 3: No solver result -> NO_LOCK
        if (solver == null || !solver.success) {
            val reason = failureReason ?: FailureReason.SolverFailed
            return CoordinatorOutput(
                lockConfidence = LockConfidence.NO_LOCK,
                guidanceHint = reason.toUserGuidanceHint(),
                failureReason = reason,
                shouldAttemptRelock = true,
                message = "Solver failed or no result, reason=$reason"
            )
        }

        // Rule 4: High residual error -> downgrade to MARGINAL or NO_LOCK
        if (solver.rmsError > rmsErrorThreshold) {
            return if (solver.inlierCount >= fullLockInlierThreshold) {
                CoordinatorOutput(
                    lockConfidence = LockConfidence.MARGINAL_LOCK,
                    guidanceHint = UserGuidanceHint.CALIBRATE_COMPASS,
                    failureReason = FailureReason.HighResidualError(solver.rmsError, rmsErrorThreshold),
                    shouldAttemptRelock = true,
                    message = "High RMS ${solver.rmsError} > $rmsErrorThreshold, downgrade to MARGINAL"
                )
            } else if (solver.inlierCount >= marginalInlierThreshold) {
                CoordinatorOutput(
                    lockConfidence = LockConfidence.MARGINAL_LOCK,
                    guidanceHint = UserGuidanceHint.HOLD_STEADY,
                    failureReason = FailureReason.HighResidualError(solver.rmsError, rmsErrorThreshold),
                    shouldAttemptRelock = true,
                    message = "High RMS but marginal inliers"
                )
            } else {
                CoordinatorOutput(
                    lockConfidence = LockConfidence.NO_LOCK,
                    guidanceHint = UserGuidanceHint.HOLD_STEADY,
                    failureReason = FailureReason.HighResidualError(solver.rmsError, rmsErrorThreshold),
                    shouldAttemptRelock = true,
                    message = "High RMS and too few inliers"
                )
            }
        }

        // Rule 5: Normal confidence ladder based on inlier count and confidence
        val lockConfidence = when {
            solver.inlierCount >= fullLockInlierThreshold && solver.confidence >= fullLockConfidenceThreshold &&
                solver.fullFieldMatched >= fullLockFullFieldMinCount && solver.fullFieldFraction >= fullLockFullFieldMinFraction -> LockConfidence.FULL_LOCK
            solver.inlierCount >= marginalInlierThreshold -> LockConfidence.MARGINAL_LOCK
            else -> LockConfidence.NO_LOCK
        }

        val guidance = when (lockConfidence) {
            LockConfidence.FULL_LOCK -> UserGuidanceHint.NONE
            LockConfidence.MARGINAL_LOCK -> UserGuidanceHint.HOLD_STEADY
            LockConfidence.NO_LOCK -> failureReason?.toUserGuidanceHint() ?: UserGuidanceHint.POINT_TO_DARK_SKY
            LockConfidence.AMBIGUOUS -> UserGuidanceHint.HOLD_STEADY
        }

        val shouldRelock = lockConfidence != LockConfidence.FULL_LOCK

        return CoordinatorOutput(
            lockConfidence = lockConfidence,
            guidanceHint = guidance,
            failureReason = if (lockConfidence == LockConfidence.NO_LOCK) failureReason else null,
            shouldAttemptRelock = shouldRelock,
            message = "Inliers=${solver.inlierCount} conf=${solver.confidence} rms=${solver.rmsError} -> $lockConfidence"
        )
    }
}

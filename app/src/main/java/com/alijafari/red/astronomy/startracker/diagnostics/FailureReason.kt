package com.alijafari.red.astronomy.startracker.diagnostics

/**
 * Sealed hierarchy for star tracker failure reasons.
 * Pure Kotlin, no Android dependency.
 */
sealed class FailureReason {

    object NoStarsDetected : FailureReason()
    data class TooFewStars(val detectedCount: Int, val minimumRequired: Int = 2) : FailureReason()
    object CatalogMatchFailed : FailureReason()
    data class AmbiguousSolution(val bestScore: Double, val secondBestScore: Double, val ratio: Double) : FailureReason()
    data class LowFrameQuality(val quality: FrameQuality) : FailureReason()
    object SolverFailed : FailureReason()
    object RansacFailed : FailureReason()
    object AttitudeSolverNoConvergence : FailureReason()
    object InsufficientDistribution : FailureReason()
    data class HighResidualError(val rmsError: Double, val threshold: Double) : FailureReason()
    object Timeout : FailureReason()
    object GyroStale : FailureReason()
    object Unknown : FailureReason()

    fun isRecoverable(): Boolean = when (this) {
        is NoStarsDetected -> true
        is TooFewStars -> true
        is LowFrameQuality -> true
        is HighResidualError -> true
        is Timeout -> true
        is GyroStale -> true
        else -> false
    }

    fun toUserGuidanceHint(): UserGuidanceHint = when (this) {
        is NoStarsDetected -> UserGuidanceHint.POINT_TO_DARK_SKY
        is TooFewStars -> UserGuidanceHint.WIDEN_FIELD_OF_VIEW
        is CatalogMatchFailed -> UserGuidanceHint.HOLD_STEADY
        is AmbiguousSolution -> UserGuidanceHint.HOLD_STEADY
        is LowFrameQuality -> when (quality) {
            FrameQuality.POOR_LOW_STARS -> UserGuidanceHint.POINT_TO_DARK_SKY
            FrameQuality.POOR_HIGH_NOISE -> UserGuidanceHint.DARKER_ENVIRONMENT
            FrameQuality.POOR_BLUR -> UserGuidanceHint.HOLD_STEADY
            FrameQuality.POOR_OVEREXPOSED -> UserGuidanceHint.DARKER_ENVIRONMENT
            else -> UserGuidanceHint.HOLD_STEADY
        }
        is SolverFailed -> UserGuidanceHint.HOLD_STEADY
        is RansacFailed -> UserGuidanceHint.HOLD_STEADY
        is AttitudeSolverNoConvergence -> UserGuidanceHint.HOLD_STEADY
        is InsufficientDistribution -> UserGuidanceHint.WIDEN_FIELD_OF_VIEW
        is HighResidualError -> UserGuidanceHint.CALIBRATE_COMPASS
        is Timeout -> UserGuidanceHint.HOLD_STEADY
        is GyroStale -> UserGuidanceHint.MOVE_SLOWLY
        is Unknown -> UserGuidanceHint.HOLD_STEADY
    }
}

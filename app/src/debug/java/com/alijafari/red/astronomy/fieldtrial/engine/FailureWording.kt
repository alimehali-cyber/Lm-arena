package com.alijafari.red.astronomy.fieldtrial.engine

import com.alijafari.red.astronomy.startracker.diagnostics.FailureReason

/**
 * G-2.5: FailureReason -> plain-English sentence for the field-trial guide.
 * Pure Kotlin (harness + CI tested). Never shows enum names or jargon.
 */
object FailureWording {
    fun sentence(reason: FailureReason?): String = when (reason) {
        null -> "No problem reported."
        FailureReason.NoStarsDetected -> "The camera sees no stars at all. Find a darker spot with more stars, and make sure the lens is clean."
        is FailureReason.TooFewStars -> "Only ${reason.detectedCount} star${if (reason.detectedCount == 1) "" else "s"} visible — the tracker needs at least ${reason.minimumRequired}. Find a starrier part of the sky."
        FailureReason.CatalogMatchFailed -> "The stars the camera sees don't match the star map yet. Hold the phone steadier."
        is FailureReason.AmbiguousSolution -> "The pattern of stars matches more than one part of the sky. Hold steady so the picture is clearer."
        is FailureReason.LowFrameQuality -> when (reason.quality) {
            com.alijafari.red.astronomy.startracker.diagnostics.FrameQuality.POOR_LOW_STARS -> "Too few stars in view. Find a darker, starrier patch of sky."
            com.alijafari.red.astronomy.startracker.diagnostics.FrameQuality.POOR_HIGH_NOISE -> "The picture is too noisy. Get away from lights, or give the camera a moment to adjust."
            com.alijafari.red.astronomy.startracker.diagnostics.FrameQuality.POOR_BLUR -> "The picture is blurry. Rest the phone on something solid."
            com.alijafari.red.astronomy.startracker.diagnostics.FrameQuality.POOR_OVEREXPOSED -> "The picture is too bright. Point away from lights and the Moon."
            else -> "The camera picture is not good enough. Try a steadier, darker view of the sky."
        }
        FailureReason.SolverFailed -> "The phone could not work out which way it is pointing from these stars. Try again held steadier."
        FailureReason.RansacFailed -> "Too many stray dots (hot pixels, reflections). Try again; if it repeats, restart the app."
        FailureReason.AttitudeSolverNoConvergence -> "The direction calculation did not settle. Hold still and try again."
        FailureReason.InsufficientDistribution -> "The stars are bunched in one corner. Point at a wider spread of stars."
        is FailureReason.HighResidualError -> "The matched stars don't line up well (error ${"%.1f".format(reason.rmsError)} px). The camera lens shape may need calibrating."
        FailureReason.Timeout -> "This frame took too long. Close other apps and try again."
        FailureReason.GyroStale -> "The motion sensor stopped sending updates. Restart the app."
        FailureReason.Unknown -> "Something went wrong that the app cannot name. Try again."
    }
}

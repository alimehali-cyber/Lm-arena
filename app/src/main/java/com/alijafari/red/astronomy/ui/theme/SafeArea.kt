package com.alijafari.red.astronomy.ui.theme

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Modifier

/**
 * The top inset that respects an open notch. The app hides the phone's status bar across every
 * window, so the status bar inset is zero — but the camera punch hole does not move. [WindowInsets.statusBars]
 * union [WindowInsets.displayCutout] therefore resolves to the cutout height on notched devices and
 * to the real status bar height wherever the bar is shown, which is exactly what top-of-screen
 * chrome (close buttons, titles, the Home search card, …) must clear.
 */
fun Modifier.statusBarsAndCutoutPadding(): Modifier =
    windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout))

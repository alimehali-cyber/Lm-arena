package com.zig.gravity.ui

import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/** Small helper so every interactive control carries a stable test tag. */
fun Modifier.clickableTag(tag: String, onClick: () -> Unit): Modifier =
    this.testTag(tag).clickable(onClick = onClick)

/**
 * Process-wide flag telling the host app that an immersive screen owns the display.
 *
 * The ZIG shell reads this to hide its floating bottom navigation while the sandbox is open and
 * to restore it on exit. Keeping the flag here (rather than in MainUiState) keeps the integration
 * to a single line in MainActivity and touches no unrelated feature state.
 */
object ImmersiveScreenState {

    private var depth = 0

    var active: Boolean by mutableStateOf(false)
        private set

    fun enter() {
        depth++
        active = true
    }

    fun exit() {
        depth = (depth - 1).coerceAtLeast(0)
        active = depth > 0
    }
}

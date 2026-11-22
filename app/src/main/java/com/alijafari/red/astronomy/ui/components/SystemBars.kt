package com.alijafari.red.astronomy.ui.components

import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Keeps the phone's status bar hidden in whatever window this is composed in.
 *
 * The activity window is covered from MainActivity; every dialog is its own window and re-shows
 * the status bar unless it hides the bar too, so each dialog surface calls this once inside its
 * content. Transient bars still appear on a swipe from the top edge and tuck away again.
 */
@Composable
fun HideStatusBar() {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        runCatching {
            val window = (view.parent as? DialogWindowProvider)?.window
                ?: generateSequence(view.context) { (it as? ContextWrapper)?.baseContext }
                    .filterIsInstance<Activity>()
                    .firstOrNull()
                    ?.window
                ?: return@SideEffect
            WindowCompat.getInsetsController(window, window.decorView).apply {
                hide(WindowInsetsCompat.Type.statusBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }
}

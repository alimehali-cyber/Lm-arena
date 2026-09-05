package com.alijafari.red.astronomy.fieldtrial

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.alijafari.red.astronomy.startracker.debug.FieldTrialGuide
import com.alijafari.red.astronomy.startracker.debug.FieldTrialHost

/**
 * G-1: the debug-only guide implementation, loaded reflectively by FieldTrialHost
 * (main source) inside a BuildConfig.DEBUG guard. Lives only in the debug source
 * set — CI dex-inspects the fieldtrial package to prove release absence.
 */
object GuideHost : FieldTrialGuide {

    @Composable
    override fun Content() {
        val access = FieldTrialHost.access() ?: return
        // one controller for the lifetime of the guide (document restored from disk,
        // so an interrupted trial survives death/reboot — G-1.3)
        val controller = remember { FieldTrialController(access.context.applicationContext) }
        GuideUi.Guide(controller, access)
    }
}

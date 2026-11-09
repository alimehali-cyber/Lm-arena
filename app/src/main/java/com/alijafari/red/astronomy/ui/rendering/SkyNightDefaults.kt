package com.alijafari.red.astronomy.ui.rendering

/**
 * The default darkness of the site, used wherever a caller has no observation of its own.
 *
 * Bortle 3 is "rural sky": the natural airglow still dominates, the Milky Way is obvious, and only a
 * low light dome sits on the horizon. It is the value the app has always used for the night sky, so
 * every surface keeps the night it had before unless the observer says otherwise.
 */
object SkyNightDefaults {
    const val BORTLE: Int = 3
}

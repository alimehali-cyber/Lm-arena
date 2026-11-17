package com.alijafari.red.astronomy.ui.screens

/**
 * The persisted globe lighting mode: follow the app theme, or force a dark / light globe.
 *
 * It lives on its own because it is shared by the globe ([com.alijafari.red.astronomy.ui.components.SatelliteEarth3D]),
 * the redesigned Satellites stage and its persisted `satellite_map_theme` preference.
 */
enum class MapTheme {
    AUTO,
    DARK,
    LIGHT
}

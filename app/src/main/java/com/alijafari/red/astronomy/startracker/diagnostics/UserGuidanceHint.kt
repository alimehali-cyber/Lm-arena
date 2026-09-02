package com.alijafari.red.astronomy.startracker.diagnostics

/**
 * Pure enum for user guidance, no UI strings.
 * UI layer will map these to localized strings.
 * This keeps diagnostics pure Kotlin, no Android dependency, no Persian, no Compose.
 */

enum class UserGuidanceHint {
    NONE,
    HOLD_STEADY,
    POINT_TO_DARK_SKY,
    WIDEN_FIELD_OF_VIEW,
    DARKER_ENVIRONMENT,
    CALIBRATE_COMPASS,
    MOVE_SLOWLY,
    TILT_UP,
    TILT_DOWN,
    ROTATE_LEFT,
    ROTATE_RIGHT,
    AVOID_LIGHT_POLLUTION,
    WAIT_FOR_DARKNESS,
    CLEAN_LENS,
    CHECK_FOCUS
}

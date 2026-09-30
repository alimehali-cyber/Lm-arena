package com.zig.chal.config

/**
 * Global Simulation Governance Schema.
 *
 * Centralized source of truth for all physics and visual parameters.
 *
 * NOTE: All ranges are set to physically plausible values for a Kerr Black Hole.
 * Mass is in Solar Masses (M_sun).
 * Spin is the dimensionless spin parameter a* = J / M^2, strictly bounded [-1, 1].
 * Temperatures are in Kelvin.
 *
 * Verbatim port of the reference engine's `src/configs/simulation.config.ts`.
 */
data class ChalParameterConfig(
    val default: Double,
    val min: Double,
    val max: Double,
    val step: Double,
    val unit: String,
    val decimals: Int,
    val label: String
)

object ChalSimulationConfig {

    /** Ray Tracing Step Budgets. */
    object RayTracingSteps {
        const val OFF: Int = 0
        const val LOW: Int = 32
        const val MEDIUM: Int = 64
        const val HIGH: Int = 128
        const val ULTRA: Int = 256 // 500 is often overkill for real-time
    }

    // --- MASTER CONFIGURATION SWITCH ---
    val DEFAULT_PRESET_MODE: ChalPresetName = ChalPresetName.HIGH_QUALITY

    // Singularity Dynamics
    val MASS = ChalParameterConfig(
        default = 1.0,
        min = 0.1, // 0.1 Solar Masses (Micro-BH)
        max = 10.0, // 10 Solar Masses (Stellar BH)
        step = 0.1,
        unit = "M\u2609",
        decimals = 1,
        label = "Black Hole Mass"
    )

    // Angular Momentum (Spin)
    // Strictly bounded to [-1, 1] for Kerr metric stability.
    // Values outside this range represent a naked singularity.
    val SPIN = ChalParameterConfig(
        default = 0.5,
        min = -0.99, // Avoid exactly -1/1 to prevent numerical singularity at horizon
        max = 0.99,
        step = 0.01,
        unit = "a*",
        decimals = 2,
        label = "Spin Parameter"
    )

    // Initial Camera Orientation
    val VERTICAL_ANGLE = ChalParameterConfig(
        default = 97.0, // Mild top-down view for better disk visibility
        min = 0.1, // Near Pole (Top)
        max = 179.9, // Near Pole (Bottom)
        step = 5.0,
        unit = "deg",
        decimals = 1,
        label = "Initial Vertical Axis"
    )

    val ZOOM = ChalParameterConfig(
        default = 30.0,
        min = 1.5, // Close orbit (Deep Dive limit)
        max = 100.0, // Far observer
        step = 0.5,
        unit = "Rs", // Schwarzschild Radii
        decimals = 1,
        label = "Observer Dist"
    )

    // System Kinetics
    val AUTO_SPIN = ChalParameterConfig(
        default = 0.005, // Default to static for accuracy
        min = -0.1,
        max = 0.1,
        step = 0.001,
        unit = "rad/s",
        decimals = 3,
        label = "Cam Auto-Pan"
    )

    val DISK_SIZE = ChalParameterConfig(
        default = 50.0, // Renders at 50.0M
        min = 4.0, // Just outside Event Horizon
        max = 100.0, // Extended observable disk
        step = 0.5,
        unit = "Rs",
        decimals = 1,
        label = "Accretion Max Radius"
    )

    val DISK_SCALE_HEIGHT = ChalParameterConfig(
        default = 0.2, // Standard thin disk approximation
        min = 0.01,
        max = 0.3, // Capped to stay within "Thin Disk" regime (< 0.2)
        step = 0.01,
        unit = "H/R",
        decimals = 2,
        label = "Disk Thickness"
    )

    // Thermodynamics
    val DISK_TEMP = ChalParameterConfig(
        default = 9500.0,
        min = 1000.0, // Cool edge
        max = 1000000.0, // High Energy X-Ray Limit (Scientific Accuracy)
        step = 1000.0,
        unit = "K",
        decimals = 0,
        label = "Disk Temp"
    )

    val DISK_DENSITY = ChalParameterConfig(
        default = 4.0,
        min = 0.0,
        max = 5.0,
        step = 0.1,
        unit = "rel",
        decimals = 1,
        label = "Opt. Density"
    )

    // Relativistic Effects
    val LENSING = ChalParameterConfig(
        default = 0.7, // Standard GR
        min = 0.0,
        max = 2.0, // Exaggerated for education
        step = 0.1,
        unit = "\u03B7", // Eta
        decimals = 1,
        label = "Lensing Str"
    )

    // System Optimization
    val RENDER_SCALE = ChalParameterConfig(
        default = 1.0,
        min = 0.25,
        max = 2.0,
        step = 0.25,
        unit = "x",
        decimals = 2,
        label = "Render Scale"
    )

    /** Performance Features (Core Toggles): `PERFORMANCE_PRESETS[DEFAULT_PRESET_MODE]`. */
    val FEATURES: ChalFeatureToggles = ChalPerformancePresets.byName(DEFAULT_PRESET_MODE)
}

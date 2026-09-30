package com.zig.chal.config

/**
 * Performance Optimization Configuration.
 *
 * Centralized control for GPU scheduling, resolution scaling, and computational budgets.
 * Governs how the simulation utilizes hardware resources. It is designed to maximize FPS on
 * constrained devices (like mobile/MacBooks) while allowing high-fidelity output on dedicated GPUs.
 *
 * Verbatim port of the reference engine's `src/configs/performance.config.ts`.
 */
object ChalPerformanceConfig {

    /** Adaptive Resolution & Scaling. */
    object Resolution {
        /** Default DPI multiplier (1.0 = native). */
        const val BASE_SCALE: Double = 1.0

        /** Minimum allowed downscaling for potato mode. */
        const val MIN_SCALE: Double = 0.5

        /** Maximum supersampling (Retina/4K). */
        const val MAX_SCALE: Double = 2.0

        /** Hard cap for mobile devices to prevent thermal throttling. */
        const val MOBILE_CAP: Double = 1.0

        /** FPS threshold below which resolution drops. */
        const val ADAPTIVE_THRESHOLD: Int = 58

        /** FPS threshold above which resolution recovers. */
        const val RECOVERY_THRESHOLD: Int = 72

        /** Master toggle for DPI scaling. */
        const val ENABLE_DYNAMIC_SCALING: Boolean = true

        /**
         * PID Controller Coefficients -- tuned for STABILITY, not reactivity.
         * Goal: FPS should barely move once converged. Visual smoothness > peak throughput.
         */
        object Pid {
            /** Proportional: halved to dampen oscillation around setpoint. */
            const val KP: Double = 0.025

            /** Integral: reduced to prevent wind-up drift. */
            const val KI: Double = 0.005

            /** Derivative: doubled to resist sudden frame-time spikes. */
            const val KD: Double = 0.04

            /** Fractional: ignore errors < 5% of frame budget (prevents micro-oscillation). */
            const val DEADZONE: Double = 0.05

            /** Minimum time between resolution changes (prevents GPU pipeline thrashing). */
            const val COOLDOWN_MS: Int = 500

            /** Tighter integral wind-up limit (was effectively 20). */
            const val INTEGRAL_CLAMP: Double = 8.0
        }
    }

    /** Stress-Test Calibration (Hardware Awareness). */
    object Calibration {
        /** 3 seconds of stress-testing at startup. */
        const val DURATION_MS: Int = 3000

        /** FPS needed to maintain 'ultra/high' quality (Cinematic standard). */
        const val MIN_STABLE_FPS: Int = 30

        /** Forced max quality for mobile devices. */
        const val MOBILE_HARD_CAP: String = "medium"
    }

    /** Ray Marching Budget (The Engine's "Gas Pedal"). */
    object Compute {
        /** Balanced default for ray steps. */
        const val MAX_STEPS_DEFAULT: Int = 200

        /** Reduced steps for mobile GPUs. */
        const val MAX_STEPS_MOBILE: Int = 80

        /** Distance threshold to switch to larger steps. */
        const val STEP_OPTIMIZATION_THRESHOLD: Double = 0.05

        /** Enable Level-of-Detail scaling based on camera distance. */
        const val DYNAMIC_LOD: Boolean = true
    }

    /** Scheduler & Loop Management. */
    object Scheduler {
        /** The simulation's heartbeat target. */
        const val TARGET_FPS: Int = 60

        /** FPS when tab is inactive/backgrounded. */
        const val IDLE_THROTTLE_FPS: Int = 30

        /** Max milliseconds per frame (1000/60). */
        const val FRAME_BUDGET_MS: Double = 16.67

        /** Time in ms before throttling kicks in. */
        const val IDLE_TIMEOUT_MS: Int = 30000
    }

    /** WebGL Context Attributes (Power Management) -> GLSurfaceView / EGL attributes on Android. */
    object Context {
        /** Depth buffer disabled if 2D quad render (saves bandwidth). */
        const val DEPTH: Boolean = false

        /** Disable MSAA as we use ray-marching (saves GPU). */
        const val ANTIALIAS: Boolean = false

        const val STENCIL: Boolean = false

        const val ALPHA: Boolean = false
    }
}

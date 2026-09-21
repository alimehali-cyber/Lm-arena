package com.zig.gargantua.renderer

/** Pure frame-decision logic for the animated-disk cache and modulation pass. */
class AnimationGate(
    private val settleNanos: Long = GargantuaAnimation.CAMERA_SETTLE_MS * 1_000_000L
) {
    enum class Action { REBUILD, MODULATE, PLAIN }

    enum class State { BUILD, SETTLE, ACTIVE, PLAIN }

    data class Input(
        val animationRequested: Boolean,
        val resourcesReady: Boolean,
        val cacheValid: Boolean,
        val sceneDirty: Boolean,
        val signatureChangedThisFrame: Boolean,
        val nowNanos: Long,
        val lastChangeNanos: Long,
        val amplitudePercent: Int
    )

    data class Decision(
        val action: Action,
        val state: State,
        val cameraStable: Boolean,
        val elapsedSinceChangeNanos: Long
    )

    fun decide(input: Input): Decision {
        val elapsed = (input.nowNanos - input.lastChangeNanos).coerceAtLeast(0L)
        val stable = elapsed >= settleNanos
        return when {
            !input.animationRequested -> Decision(Action.PLAIN, State.PLAIN, stable, elapsed)
            input.amplitudePercent <= 0 -> Decision(Action.PLAIN, State.PLAIN, stable, elapsed)
            !input.resourcesReady -> Decision(Action.PLAIN, State.BUILD, stable, elapsed)
            input.signatureChangedThisFrame || input.sceneDirty || !input.cacheValid ->
                Decision(Action.REBUILD, State.BUILD, stable, elapsed)
            !stable -> Decision(Action.PLAIN, State.SETTLE, false, elapsed)
            else -> Decision(Action.MODULATE, State.ACTIVE, true, elapsed)
        }
    }
}

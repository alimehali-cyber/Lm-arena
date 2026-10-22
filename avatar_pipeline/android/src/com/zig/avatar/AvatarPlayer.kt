package com.zig.avatar

import kotlin.math.max
import kotlin.math.min

/**
 * Plays one clip at a time, with an optional cross-fade, and produces what the renderer
 * needs: skinning matrices and morph weights.
 *
 * Design notes
 *  - Sampling is CPU-side: ~90 channels per clip is microseconds of work and keeps the
 *    code readable. The skinning itself is also CPU-side (see AvatarRenderer), which
 *    avoids relying on a vertex-shader uniform budget we cannot test on a device.
 *  - STEP samplers are honoured. Constant channels are exported that way on purpose.
 *  - Time is advanced with a caller-supplied dt, so reduced-motion, pausing and clip
 *    changes are the host's decisions rather than a hidden loop.
 *  - Every clip is evaluated from the rest pose. A channel the clip does not key must not
 *    inherit the previous clip's pose - that exact bug folded the character in half in
 *    Blender (see reports/FINAL_REPORT.md).
 */
class AvatarPlayer(private val model: AvatarModel) {

    val boneCount: Int = model.jointCount
    private val morphCount: Int = max(1, model.morphNames.size)

    /** 16 floats per joint, column-major: world * inverseBind. */
    val boneMatrices = FloatArray(boneCount * 16)

    /** One weight per morph target, in the model's target order. */
    val morphWeights = FloatArray(morphCount)

    var clipName: String? = null
        private set
    var clipTime: Float = 0f
        private set
    var loop: Boolean = true
        private set
    var speed: Float = 1f
    var isPlaying: Boolean = false
        private set

    /** Set by the host when the system animator duration scale is 0 (reduce motion). */
    var reducedMotion: Boolean = false

    /** Optional lip-sync track, blended on top of the clip's own jaw/mouth motion. */
    var speech: VisemeTimeline? = null

    private var current: AvatarModel.Clip? = null
    private var previous: AvatarModel.Clip? = null
    private var previousTime = 0f
    private var fadeDuration = 0f
    private var fadeElapsed = 0f

    // Pose buffers. Two sets so a cross-fade can blend two evaluated clips.
    private val curT = FloatArray(boneCount * 3)
    private val curR = FloatArray(boneCount * 4)
    private val curS = FloatArray(boneCount * 3)
    private val curM = FloatArray(morphCount)
    private val oldT = FloatArray(boneCount * 3)
    private val oldR = FloatArray(boneCount * 4)
    private val oldS = FloatArray(boneCount * 3)
    private val oldM = FloatArray(morphCount)

    private val world = FloatArray(boneCount * 16)
    private val tmp = FloatArray(16)
    private val q = FloatArray(4)

    /**
     * Joint indices ordered parent-before-child. glTF does not require the node array to
     * be topological, and reading a parent's world matrix before it is written produces
     * a subtly wrong skin that is very hard to spot by eye.
     */
    private val depthOrder: IntArray = run {
        val depth = IntArray(boneCount) { -1 }
        fun d(j: Int): Int {
            if (depth[j] >= 0) return depth[j]
            val p = model.parentOf[j]
            depth[j] = if (p < 0) 0 else d(p) + 1
            return depth[j]
        }
        for (j in 0 until boneCount) d(j)
        (0 until boneCount).sortedBy { depth[it] }.toIntArray()
    }

    init {
        resetToRest()
    }

    fun resetToRest() {
        poseRest(curT, curR, curS, curM)
        java.util.Arrays.fill(morphWeights, 0f)
        computeWorldAndSkin()
    }

    /**
     * Starts a clip. [fadeSeconds] blends from the pose that is on screen now, so the
     * switch does not pop. Returns false if the clip is not in the model.
     */
    fun play(name: String, loop: Boolean = true, fadeSeconds: Float = 0f, fromStart: Boolean = true): Boolean {
        val clip = model.clips[name] ?: return false
        if (current != null && fadeSeconds > 0f && current !== clip) {
            previous = current
            previousTime = clipTime
            fadeDuration = fadeSeconds
            fadeElapsed = 0f
        } else {
            previous = null
            fadeDuration = 0f
        }
        current = clip
        clipName = name
        this.loop = loop
        if (fromStart) clipTime = 0f
        isPlaying = true
        return true
    }

    fun stop() {
        isPlaying = false
        current = null
        previous = null
        clipName = null
        resetToRest()
    }

    /** Advances time and recomputes the pose. [dt] is seconds, clamped by the caller. */
    fun update(dt: Float) {
        val clip = current ?: run { computeWorldAndSkin(); return }
        if (isPlaying) {
            val step = if (reducedMotion) 0f else dt * speed
            clipTime += step
            if (clip.duration > 0f && clipTime > clip.duration) {
                if (loop) clipTime %= clip.duration
                else { clipTime = clip.duration; isPlaying = false }
            }
            if (previous != null) {
                fadeElapsed += dt
                if (fadeElapsed >= fadeDuration) previous = null
            }
        }

        poseClip(clip, clipTime, curT, curR, curS, curM)
        val prev = previous
        if (prev != null && fadeDuration > 0f) {
            poseClip(prev, previousTime + fadeElapsed, oldT, oldR, oldS, oldM)
            val a = Ease.smoothStep(fadeElapsed / fadeDuration)   // 0 = old, 1 = new
            for (j in 0 until boneCount) {
                for (k in 0 until 3) {
                    curT[j * 3 + k] = oldT[j * 3 + k] + (curT[j * 3 + k] - oldT[j * 3 + k]) * a
                    curS[j * 3 + k] = oldS[j * 3 + k] + (curS[j * 3 + k] - oldS[j * 3 + k]) * a
                }
                Quat.nlerp(q, 0, oldR, j * 4, curR, j * 4, a)
                curR[j * 4] = q[0]; curR[j * 4 + 1] = q[1]; curR[j * 4 + 2] = q[2]; curR[j * 4 + 3] = q[3]
            }
            for (k in 0 until morphCount) curM[k] = oldM[k] + (curM[k] - oldM[k]) * a
        }
        System.arraycopy(curM, 0, morphWeights, 0, morphCount)
        speech?.applyTo(clipTime, morphWeights)
        computeWorldAndSkin()
    }

    /** Evaluate a clip at an absolute time without touching playback state (for previews). */
    fun poseAt(name: String, t: Float) {
        val clip = model.clips[name] ?: return
        poseClip(clip, t, curT, curR, curS, curM)
        System.arraycopy(curM, 0, morphWeights, 0, morphCount)
        previous = null
        computeWorldAndSkin()
    }

    // ---- sampling ----------------------------------------------------------

    private fun poseRest(t: FloatArray, r: FloatArray, s: FloatArray, m: FloatArray) {
        for (j in 0 until boneCount) {
            val o = j * 10
            t[j * 3] = model.restLocal[o]
            t[j * 3 + 1] = model.restLocal[o + 1]
            t[j * 3 + 2] = model.restLocal[o + 2]
            r[j * 4] = model.restLocal[o + 3]
            r[j * 4 + 1] = model.restLocal[o + 4]
            r[j * 4 + 2] = model.restLocal[o + 5]
            r[j * 4 + 3] = model.restLocal[o + 6]
            s[j * 3] = model.restLocal[o + 7]
            s[j * 3 + 1] = model.restLocal[o + 8]
            s[j * 3 + 2] = model.restLocal[o + 9]
        }
        java.util.Arrays.fill(m, 0f)
    }

    private fun poseClip(
        clip: AvatarModel.Clip, time: Float,
        t: FloatArray, r: FloatArray, s: FloatArray, m: FloatArray,
    ) {
        poseRest(t, r, s, m)
        for (ch in clip.channels) {
            val smp = ch.sampler
            if (smp.times.isEmpty()) continue
            val i0 = indexAt(smp, time)
            val i1 = if (smp.step || i0 == smp.times.size - 1) i0 else i0 + 1
            val t0 = smp.times[i0]
            val t1 = smp.times[i1]
            val f = if (smp.step || t1 <= t0) 0f else ((time - t0) / (t1 - t0)).coerceIn(0f, 1f)

            when (ch.path) {
                "translation", "scale" -> {
                    val j = jointOfNode(ch.node) ?: continue
                    val dst = if (ch.path == "translation") t else s
                    val o = j * 3
                    for (k in 0 until 3) {
                        val a = smp.values[i0 * smp.stride + k]
                        val b = smp.values[i1 * smp.stride + k]
                        dst[o + k] = a + (b - a) * f
                    }
                }
                "rotation" -> {
                    val j = jointOfNode(ch.node) ?: continue
                    if (f == 0f) {
                        System.arraycopy(smp.values, i0 * 4, r, j * 4, 4)
                    } else {
                        Quat.slerp(r, j * 4, smp.values, i0 * 4, smp.values, i1 * 4, f)
                    }
                }
                "weights" -> {
                    val n = min(m.size, smp.stride)
                    for (k in 0 until n) {
                        val a = smp.values[i0 * smp.stride + k]
                        val b = smp.values[i1 * smp.stride + k]
                        m[k] = (a + (b - a) * f).coerceIn(0f, 1.5f)
                    }
                }
            }
        }
    }

    /** Binary search for the last key at or before [time]. */
    private fun indexAt(s: AvatarModel.Sampler, time: Float): Int {
        val times = s.times
        if (times.size == 1 || time <= times[0]) return 0
        val last = times.size - 1
        if (time >= times[last]) return last
        var lo = 0
        var hi = last
        while (hi - lo > 1) {
            val mid = (lo + hi) ushr 1
            if (times[mid] <= time) lo = mid else hi = mid
        }
        return lo
    }

    private fun jointOfNode(node: Int): Int? {
        for (j in 0 until boneCount) if (model.nodeOfJoint[j] == node) return j
        return null
    }

    // ---- hierarchy ---------------------------------------------------------

    private fun computeWorldAndSkin() {
        for (j in depthOrder) {
            M4.fromTrsF(
                tmp, 0,
                curT[j * 3], curT[j * 3 + 1], curT[j * 3 + 2],
                curR[j * 4], curR[j * 4 + 1], curR[j * 4 + 2], curR[j * 4 + 3],
                curS[j * 3], curS[j * 3 + 1], curS[j * 3 + 2],
            )
            val p = model.parentOf[j]
            if (p >= 0) M4.mulAt(world, j * 16, world, p * 16, tmp, 0)
            else System.arraycopy(tmp, 0, world, j * 16, 16)
        }
        for (j in 0 until boneCount) {
            M4.mulAt(boneMatrices, j * 16, world, j * 16, model.inverseBind, j * 16)
        }
    }
}

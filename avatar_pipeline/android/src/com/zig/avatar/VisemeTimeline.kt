package com.zig.avatar

import org.json.JSONObject
import kotlin.math.max

/**
 * Reads the timeline produced by `tools/lipsync.py` and pushes jaw/mouth morph weights.
 *
 * The JSON contract (frame, t, viseme, weight) is deliberately dumber than a rig:
 * it names a morph target the model already has, so the same file works for any
 * character built by this pipeline and for a re-baked clip. Silence is an empty list
 * with `neutral_fallback: true`, which means "leave the mouth alone" rather than
 * "close it".
 */
class VisemeTimeline private constructor(
    private val frames: FloatArray,      // seconds, ascending
    private val weights: FloatArray,     // per frame, per morph, row-major
    private val morphIndex: IntArray,    // per morph column
    val sourceVisemes: Int,
    val neutralFallback: Boolean,
) {

    /** Blends the lip-sync pose on top of [morphWeights] with a take-the-larger rule. */
    fun applyTo(timeSeconds: Float, morphWeights: FloatArray) {
        if (frames.isEmpty()) return
        val n = morphIndex.size
        if (morphWeights.size < 1) return
        if (timeSeconds <= frames[0]) {
            blend(morphWeights, 0, 1f); return
        }
        val last = frames.size - 1
        if (timeSeconds >= frames[last]) {
            blend(morphWeights, last, 1f); return
        }
        var lo = 0
        var hi = last
        while (hi - lo > 1) {
            val mid = (lo + hi) ushr 1
            if (frames[mid] <= timeSeconds) lo = mid else hi = mid
        }
        val span = frames[hi] - frames[lo]
        val f = if (span <= 1e-5f) 0f else (timeSeconds - frames[lo]) / span
        // Cross-fade the two neighbouring frames, then take the larger of speech and clip.
        for (k in 0 until n) {
            val a = weights[lo * n + k]
            val b = weights[hi * n + k]
            val v = a + (b - a) * f
            val idx = morphIndex[k]
            if (idx >= 0 && idx < morphWeights.size) morphWeights[idx] = max(morphWeights[idx], v)
        }
    }

    private fun blend(morphWeights: FloatArray, frame: Int, scale: Float) {
        val n = morphIndex.size
        for (k in 0 until n) {
            val idx = morphIndex[k]
            if (idx >= 0 && idx < morphWeights.size) {
                morphWeights[idx] = max(morphWeights[idx], weights[frame * n + k] * scale)
            }
        }
    }

    companion object {
        /**
         * Builds a timeline from a visemes JSON file. Keys that name a morph the model
         * does not have are ignored with a count in [rejected], never guessed at.
         */
        fun parse(json: String, model: AvatarModel): Pair<VisemeTimeline, Int> {
            val root = JSONObject(json)
            val keys = root.optJSONArray("keys") ?: return Pair(empty(), 0)
            if (keys.length() == 0) {
                val neutral = root.optBoolean("neutral_fallback", true)
                return Pair(VisemeTimeline(FloatArray(0), FloatArray(0), IntArray(0), 0, neutral), 0)
            }
            val fps = root.optInt("fps", 24).toFloat().let { if (it < 1f) 24f else it }
            val n = model.morphNames.size
            val rows = ArrayList<FloatArray>(keys.length())
            val times = ArrayList<Float>(keys.length())
            var rejected = 0
            for (i in 0 until keys.length()) {
                val k = keys.getJSONObject(i)
                val t = if (k.has("t")) k.getDouble("t").toFloat() else (k.optInt("frame", i + 1) / fps)
                val w = FloatArray(n)
                val name = k.optString("viseme", "")
                if (name.isNotEmpty()) {
                    val idx = model.morphIndex(name)
                    if (idx >= 0) {
                        val amp = k.optDouble("weight", 1.0).toFloat().coerceIn(0f, 1f)
                        // A viseme closes as much as it opens: the shape IS the target pose.
                        w[idx] = amp
                    } else {
                        rejected++
                    }
                }
                times.add(t)
                rows.add(w)
            }
            // ascending time is required by the binary search
            val order = times.indices.sortedBy { times[it] }
            val frames = FloatArray(order.size) { times[order[it]] }
            val flat = FloatArray(order.size * n)
            for ((row, srcIdx) in order.withIndex()) System.arraycopy(rows[srcIdx], 0, flat, row * n, n)
            val map = IntArray(n) { it }
            return Pair(VisemeTimeline(frames, flat, map, keys.length(),
                root.optBoolean("neutral_fallback", false)), rejected)
        }

        private fun empty() = VisemeTimeline(FloatArray(0), FloatArray(0), IntArray(0), 0, true)
    }
}

package com.alijafari.red.astronomy.avatar

import java.io.File
import java.io.InputStream
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.sin
import kotlin.math.sqrt

data class MouthCue(
    val startSec: Float,
    val endSec: Float,
    val shape: String,
    val morphWeights: Map<Int, Float>,
)

data class AvatarReactionPreset(
    val clipName: String,
    val durationSec: Float,
    val lineEn: String,
    val lineFa: String,
    val playLipSync: Boolean,
)

/**
 * Real-time `60`-joint matrix-palette evaluator, smooth dual-clip cross-fader,
 * natural blink generator, and co-articulated Persian (`fa`) / English (`en`) lip-sync blender.
 *
 * Pre-allocates all working buffers so steady-state frame evaluation allocates ZERO heap objects.
 */
class AvatarAnimator(private val model: AvatarModelAsset) {

    val jointCount: Int = model.jointNodeIndices.size
    val morphCount: Int = model.morphTargetCount
    val skinMatrices: FloatArray = FloatArray(maxOf(jointCount, 1) * 16)
    val morphWeights: FloatArray = FloatArray(maxOf(morphCount, 26))

    private val nNodes = model.nodes.size
    private val curT = Array(nNodes) { FloatArray(3) }
    private val curR = Array(nNodes) { FloatArray(4) }
    private val curS = Array(nNodes) { FloatArray(3) }

    private val altT = Array(nNodes) { FloatArray(3) }
    private val altR = Array(nNodes) { FloatArray(4) }
    private val altS = Array(nNodes) { FloatArray(3) }
    private val altMorph = FloatArray(maxOf(morphCount, 26))

    private val localMat = Array(nNodes) { FloatArray(16) }
    private val worldMat = Array(nNodes) { FloatArray(16) }
    private val computed = BooleanArray(nNodes)

    private var mouthCuesFa: List<MouthCue> = emptyList()
    private var mouthCuesEn: List<MouthCue> = emptyList()

    private var currentPrimaryClip: String = "Idle_Breathing"
    private var previousClip: String = "Idle_Breathing"
    private var crossFadeProgress: Float = 1f
    private var activeReactionClip: String? = null
    private var reactionStartSec: Float = -100f
    private var reactionDurationSec: Float = 0f
    private var reactionPlayLipSync: Boolean = false

    val hipWorldX: Float
        get() {
            val hips = model.nodes.indexOfFirst { it.name == "Hips" }
            return if (hips >= 0) worldMat[hips][12] else 0f
        }

    val hipWorldZ: Float
        get() {
            val hips = model.nodes.indexOfFirst { it.name == "Hips" }
            return if (hips >= 0) worldMat[hips][14] else 0f
        }

    companion object {
        val MORPH_INDEX: Map<String, Int> = listOf(
            "eyeBlinkLeft", "eyeBlinkRight", "eyeWideLeft", "eyeWideRight", "eyeSquintLeft", "eyeSquintRight",
            "browInnerUp", "browDownLeft", "browDownRight", "browOuterUpLeft", "browOuterUpRight",
            "mouthSmileLeft", "mouthSmileRight", "mouthFrownLeft", "mouthFrownRight",
            "mouthPucker", "mouthFunnel", "jawOpen", "tongueOut",
            "cheekPuff", "cheekSquintLeft", "cheekSquintRight",
            "viseme_PP", "viseme_FF", "viseme_EE", "viseme_aa",
        ).withIndex().associate { it.value to it.index }

        val TAP_REACTIONS: List<AvatarReactionPreset> = listOf(
            AvatarReactionPreset(
                clipName = "ProfZ_DiscoGroove",
                durationSec = 5.4f,
                lineEn = "Even planets need a groove. ✨",
                lineFa = "حتی سیاره‌ها هم به ریتم نیاز دارن! ✨",
                playLipSync = true,
            ),
            AvatarReactionPreset(
                clipName = "ProfZ_TongueOutWink",
                durationSec = 3.2f,
                lineEn = "Did that star just wink first?",
                lineFa = "اون ستاره اول چشمک زد یا من؟",
                playLipSync = false,
            ),
            AvatarReactionPreset(
                clipName = "ProfZ_HeroGlassesAdjust",
                durationSec = 3.6f,
                lineEn = "Lenses clean — tonight's sky is electric!",
                lineFa = "عینک تمیز شد — آسمون امشب غوغاست!",
                playLipSync = true,
            ),
            AvatarReactionPreset(
                clipName = "Emote_Happy",
                durationSec = 3.2f,
                lineEn = "Drag the sky to travel through time!",
                lineFa = "آسمون رو بکش تا در زمان سفر کنی!",
                playLipSync = true,
            ),
        )

        fun clipForSunAltitude(sunAltitudeDeg: Double): String = when {
            sunAltitudeDeg > 12.0 -> "Shade_Eyes_Day"
            sunAltitudeDeg > -6.0 -> "Check_Phone_Twilight"
            else -> "Point_Sky_Night"
        }

        /** Ambient schedule alternating calm breathing with astronomy context poses & glasses adjusts. */
        fun ambientClipAtTime(sunAltitudeDeg: Double, timeSec: Float): String {
            val cyclePhase = ((timeSec % 16.0f) + 16.0f) % 16.0f
            return when {
                cyclePhase < 7.5f -> "Idle_Breathing"
                cyclePhase < 12.5f -> clipForSunAltitude(sunAltitudeDeg)
                else -> "ProfZ_HeroGlassesAdjust"
            }
        }

        private fun slerpQuat(a: FloatArray, ao: Int, b: FloatArray, bo: Int, t: Float, out: FloatArray) {
            var bx = b[bo]; var by = b[bo + 1]; var bz = b[bo + 2]; var bw = b[bo + 3]
            val ax = a[ao]; val ay = a[ao + 1]; val az = a[ao + 2]; val aw = a[ao + 3]
            var dot = ax * bx + ay * by + az * bz + aw * bw
            if (dot < 0f) {
                dot = -dot; bx = -bx; by = -by; bz = -bz; bw = -bw
            }
            val s0: Float
            val s1: Float
            if (dot > 0.9995f) {
                s0 = 1f - t; s1 = t
            } else {
                val omega = acos(dot.coerceIn(-1f, 1f))
                val invSin = 1f / sin(omega)
                s0 = sin((1f - t) * omega) * invSin
                s1 = sin(t * omega) * invSin
            }
            out[0] = ax * s0 + bx * s1
            out[1] = ay * s0 + by * s1
            out[2] = az * s0 + bz * s1
            out[3] = aw * s0 + bw * s1
            val n = sqrt(out[0] * out[0] + out[1] * out[1] + out[2] * out[2] + out[3] * out[3]).coerceAtLeast(1e-8f)
            out[0] /= n; out[1] /= n; out[2] /= n; out[3] /= n
        }

        private fun composeTRS(t: FloatArray, q: FloatArray, s: FloatArray, m: FloatArray) {
            val x = q[0]; val y = q[1]; val z = q[2]; val w = q[3]
            val x2 = x + x; val y2 = y + y; val z2 = z + z
            val xx = x * x2; val xy = x * y2; val xz = x * z2
            val yy = y * y2; val yz = y * z2; val zz = z * z2
            val wx = w * x2; val wy = w * y2; val wz = w * z2

            m[0] = (1f - (yy + zz)) * s[0]; m[1] = (xy + wz) * s[0];        m[2] = (xz - wy) * s[0];        m[3] = 0f
            m[4] = (xy - wz) * s[1];        m[5] = (1f - (xx + zz)) * s[1]; m[6] = (yz + wx) * s[1];        m[7] = 0f
            m[8] = (xz + wy) * s[2];        m[9] = (yz - wx) * s[2];        m[10] = (1f - (xx + yy)) * s[2]; m[11] = 0f
            m[12] = t[0];                   m[13] = t[1];                   m[14] = t[2];                   m[15] = 1f
        }

        private fun mulMat4(a: FloatArray, b: FloatArray, out: FloatArray, outOff: Int = 0) {
            for (col in 0..3) {
                val b0 = b[col * 4]; val b1 = b[col * 4 + 1]; val b2 = b[col * 4 + 2]; val b3 = b[col * 4 + 3]
                for (row in 0..3) {
                    out[outOff + col * 4 + row] =
                        a[row] * b0 + a[4 + row] * b1 + a[8 + row] * b2 + a[12 + row] * b3
                }
            }
        }
    }

    fun loadLipSyncCues(jsonFile: File, persian: Boolean = true) {
        loadLipSyncCuesFromText(jsonFile.readText(Charsets.UTF_8), persian)
    }

    fun loadLipSyncCues(stream: InputStream, persian: Boolean = true) {
        val text = stream.use { it.readBytes().toString(Charsets.UTF_8) }
        loadLipSyncCuesFromText(text, persian)
    }

    fun loadLipSyncCuesFromText(jsonText: String, persian: Boolean = true) {
        val root = MiniJson.parse(jsonText) as Map<*, *>
        val list = (root["mouthCues"] as? List<*>) ?: emptyList<Any>()
        val parsed = list.map { raw ->
            val m = raw as Map<*, *>
            val wMap = linkedMapOf<Int, Float>()
            val mw = m["morphWeights"] as? Map<*, *>
            mw?.forEach { (k, v) ->
                val idx = MORPH_INDEX[k.toString()]
                if (idx != null && v is Number) wMap[idx] = v.toFloat()
            }
            MouthCue(
                startSec = (m["start"] as Number).toFloat(),
                endSec = (m["end"] as Number).toFloat(),
                shape = m["value"].toString(),
                morphWeights = wMap,
            )
        }
        if (persian) mouthCuesFa = parsed else mouthCuesEn = parsed
    }

    fun triggerReaction(preset: AvatarReactionPreset, nowSec: Float) {
        activeReactionClip = preset.clipName
        reactionStartSec = nowSec
        reactionDurationSec = preset.durationSec
        reactionPlayLipSync = preset.playLipSync
    }

    fun evaluateRestPose() {
        resetPoseBuffers(curT, curR, curS, morphWeights)
        updateWorldAndSkinMatrices()
    }

    fun evaluateClip(clipName: String, timeSec: Float, overlayLipSyncSec: Float? = null, persian: Boolean = true) {
        sampleClipInto(clipName, timeSec, curT, curR, curS, morphWeights)
        if (overlayLipSyncSec != null) {
            applyLipSyncEnvelope(overlayLipSyncSec, persian)
        }
        updateWorldAndSkinMatrices()
    }

    /** Full interactive scene step with smooth 0.32 s skeletal cross-fades, blinks, and speech visemes. */
    fun evaluateSceneFrame(
        timeSec: Float,
        dtSec: Float,
        sunAltitudeDeg: Double,
        persian: Boolean,
    ) {
        val rawElapsed = timeSec - reactionStartSec
        val reactionElapsed = rawElapsed.coerceAtLeast(0f)
        val desiredClip = if (activeReactionClip != null && rawElapsed <= reactionDurationSec) {
            activeReactionClip!!
        } else {
            activeReactionClip = null
            ambientClipAtTime(sunAltitudeDeg, timeSec)
        }

        if (desiredClip != currentPrimaryClip) {
            previousClip = currentPrimaryClip
            currentPrimaryClip = desiredClip
            crossFadeProgress = 0f
        } else if (crossFadeProgress < 1f) {
            crossFadeProgress = (crossFadeProgress + dtSec / 0.32f).coerceIn(0f, 1f)
        }

        val primaryClipTime = if (activeReactionClip != null) reactionElapsed.coerceAtLeast(0f) else timeSec
        sampleClipInto(currentPrimaryClip, primaryClipTime, curT, curR, curS, morphWeights)

        if (crossFadeProgress < 1f && previousClip != currentPrimaryClip) {
            sampleClipInto(previousClip, timeSec, altT, altR, altS, altMorph)
            val smoothAlpha = crossFadeProgress * crossFadeProgress * (3f - 2f * crossFadeProgress)
            val inv = 1f - smoothAlpha
            for (i in 0 until nNodes) {
                for (c in 0..2) {
                    curT[i][c] = altT[i][c] * inv + curT[i][c] * smoothAlpha
                    curS[i][c] = altS[i][c] * inv + curS[i][c] * smoothAlpha
                }
                slerpQuat(altR[i], 0, curR[i], 0, smoothAlpha, curR[i])
            }
            for (m in morphWeights.indices) {
                morphWeights[m] = altMorph[m] * inv + morphWeights[m] * smoothAlpha
            }
        }

        // Natural spontaneous blink pulse every 3.8 s (160 ms smooth eyelid closure)
        val blinkCycle = ((timeSec % 3.8f) + 3.8f) % 3.8f
        if (blinkCycle < 0.16f) {
            val pulse = sin((blinkCycle / 0.16f) * PI.toFloat()).coerceIn(0f, 1f)
            morphWeights[0] = maxOf(morphWeights[0], pulse)
            morphWeights[1] = maxOf(morphWeights[1], pulse)
        }

        // Gentle warm baseline smile when calm
        morphWeights[11] = maxOf(morphWeights[11], 0.16f)
        morphWeights[12] = maxOf(morphWeights[12], 0.16f)

        // Co-articulated Persian / English viseme lip-sync envelope while reacting
        if (activeReactionClip != null && reactionPlayLipSync && reactionElapsed >= 0f) {
            applyLipSyncEnvelope(reactionElapsed, persian)
        }

        updateWorldAndSkinMatrices()
    }

    private fun applyLipSyncEnvelope(speechSec: Float, persian: Boolean) {
        val cues = if (persian && mouthCuesFa.isNotEmpty()) mouthCuesFa
        else if (mouthCuesEn.isNotEmpty()) mouthCuesEn
        else mouthCuesFa
        if (cues.isEmpty()) return
        val totalDur = cues.last().endSec.coerceAtLeast(0.1f)
        val wrapped = speechSec % totalDur
        val idx = cues.indexOfFirst { wrapped >= it.startSec && wrapped <= it.endSec }
        if (idx < 0) return
        val cue = cues[idx]
        val nextCue = cues.getOrNull(idx + 1)
        val span = (cue.endSec - cue.startSec).coerceAtLeast(1e-4f)
        val local = ((wrapped - cue.startSec) / span).coerceIn(0f, 1f)
        val blendNext = if (local > 0.65f && nextCue != null) ((local - 0.65f) / 0.35f) * 0.45f else 0f

        cue.morphWeights.forEach { (mIdx, w) ->
            if (mIdx in morphWeights.indices) {
                morphWeights[mIdx] = (w * (1f - blendNext)).coerceIn(0f, 1f)
            }
        }
        nextCue?.morphWeights?.forEach { (mIdx, w) ->
            if (mIdx in morphWeights.indices && blendNext > 0f) {
                morphWeights[mIdx] = (morphWeights[mIdx] + w * blendNext).coerceIn(0f, 1f)
            }
        }
    }

    private fun resetPoseBuffers(
        outT: Array<FloatArray>,
        outR: Array<FloatArray>,
        outS: Array<FloatArray>,
        outMorph: FloatArray,
    ) {
        for (i in 0 until nNodes) {
            model.nodes[i].baseTranslation.copyInto(outT[i])
            model.nodes[i].baseRotation.copyInto(outR[i])
            model.nodes[i].baseScale.copyInto(outS[i])
            computed[i] = false
        }
        outMorph.fill(0f)
    }

    private fun sampleClipInto(
        clipName: String,
        timeSec: Float,
        outT: Array<FloatArray>,
        outR: Array<FloatArray>,
        outS: Array<FloatArray>,
        outMorph: FloatArray,
    ) {
        resetPoseBuffers(outT, outR, outS, outMorph)
        val clip = model.animations[clipName] ?: model.animations["Idle_Breathing"] ?: return
        if (clip.durationSec <= 0f) return
        val t = ((timeSec % clip.durationSec) + clip.durationSec) % clip.durationSec
        for (ch in clip.channels) {
            val times = ch.sampler.times
            val vals = ch.sampler.values
            val stride = ch.sampler.stride
            if (times.isEmpty()) continue

            var i0 = 0
            while (i0 + 1 < times.size && times[i0 + 1] <= t) i0++
            val i1 = minOf(i0 + 1, times.size - 1)
            val dt = times[i1] - times[i0]
            val alpha = if (dt > 1e-6f) ((t - times[i0]) / dt).coerceIn(0f, 1f) else 0f

            when (ch.path) {
                ChannelPath.TRANSLATION -> {
                    val dst = outT[ch.nodeIndex]
                    for (c in 0..2) dst[c] = vals[i0 * 3 + c] * (1f - alpha) + vals[i1 * 3 + c] * alpha
                }
                ChannelPath.SCALE -> {
                    val dst = outS[ch.nodeIndex]
                    for (c in 0..2) dst[c] = vals[i0 * 3 + c] * (1f - alpha) + vals[i1 * 3 + c] * alpha
                }
                ChannelPath.ROTATION -> {
                    slerpQuat(vals, i0 * 4, vals, i1 * 4, alpha, outR[ch.nodeIndex])
                }
                ChannelPath.WEIGHTS -> {
                    val limit = minOf(stride, outMorph.size)
                    for (c in 0 until limit) {
                        outMorph[c] = vals[i0 * stride + c] * (1f - alpha) + vals[i1 * stride + c] * alpha
                    }
                }
            }
        }
    }

    private fun updateWorldAndSkinMatrices() {
        for (i in 0 until nNodes) {
            composeTRS(curT[i], curR[i], curS[i], localMat[i])
            computed[i] = false
        }

        fun ensureWorld(idx: Int) {
            if (computed[idx]) return
            val p = model.nodes[idx].parent
            if (p < 0) {
                localMat[idx].copyInto(worldMat[idx])
            } else {
                ensureWorld(p)
                mulMat4(worldMat[p], localMat[idx], worldMat[idx])
            }
            computed[idx] = true
        }

        for (i in 0 until nNodes) ensureWorld(i)
        for (j in 0 until jointCount) {
            val nodeIdx = model.jointNodeIndices[j]
            mulMat4(worldMat[nodeIdx], model.inverseBindMatrices[j], skinMatrices, j * 16)
        }
    }
}

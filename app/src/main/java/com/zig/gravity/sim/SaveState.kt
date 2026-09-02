package com.zig.gravity.sim

import com.zig.gravity.physics.BodyType
import com.zig.gravity.physics.SimArrays

/**
 * Save / restore for the whole experiment (Phase 6, test 32).
 *
 * Plain-text, dependency-free encoding. The project does not currently wire kotlinx.serialization
 * or DataStore (both are commented out in app/build.gradle.kts), so this uses a hand-rolled
 * encoder persisted through the app's existing SharedPreferences rather than adding dependencies
 * that could not be verified here. The format is versioned so a later DataStore migration is
 * mechanical.
 */
object SaveState {

    private const val VERSION = 1
    private const val BODY_SEP = ";"
    private const val FIELD_SEP = ","
    private const val HEADER_SEP = "|"

    data class Session(
        val preset: Preset,
        val speedIndex: Int,
        val paused: Boolean,
        val trailsVisible: Boolean,
        val teachingEnabled: Boolean,
        val darkTheme: Boolean,
        val persian: Boolean,
        val marbleBounce: Boolean,
        val selectedId: Long
    )

    fun encode(s: SimArrays, session: Session): String {
        val sb = StringBuilder(512)
        sb.append(VERSION).append(HEADER_SEP)
            .append(session.preset.name).append(HEADER_SEP)
            .append(session.speedIndex).append(HEADER_SEP)
            .append(if (session.paused) 1 else 0).append(HEADER_SEP)
            .append(if (session.trailsVisible) 1 else 0).append(HEADER_SEP)
            .append(if (session.teachingEnabled) 1 else 0).append(HEADER_SEP)
            .append(if (session.darkTheme) 1 else 0).append(HEADER_SEP)
            .append(if (session.persian) 1 else 0).append(HEADER_SEP)
            .append(if (session.marbleBounce) 1 else 0).append(HEADER_SEP)
            .append(session.selectedId).append(HEADER_SEP)
            .append(s.simTime).append(HEADER_SEP)
        for (i in 0 until s.n) {
            if (i > 0) sb.append(BODY_SEP)
            sb.append(s.id[i]).append(FIELD_SEP)
                .append(s.type[i].toInt()).append(FIELD_SEP)
                .append(s.mass[i]).append(FIELD_SEP)
                .append(s.radiusDp[i]).append(FIELD_SEP)
                .append(s.x[i]).append(FIELD_SEP)
                .append(s.y[i]).append(FIELD_SEP)
                .append(s.vx[i]).append(FIELD_SEP)
                .append(s.vy[i]).append(FIELD_SEP)
                .append(s.partnerId[i]).append(FIELD_SEP)
                .append(s.cooldownUntil[i]).append(FIELD_SEP)
                .append(s.gateMouthId[i]).append(FIELD_SEP)
                .append(s.catalogKey[i] ?: "-")
        }
        return sb.toString()
    }

    /** @return the restored session header, or null when [text] is absent or unreadable. */
    fun decode(text: String?, into: SimArrays): Session? {
        if (text.isNullOrBlank()) return null
        return try {
            val head = text.split(HEADER_SEP)
            if (head.size < 12) return null
            if (head[0].toInt() != VERSION) return null
            val session = Session(
                preset = runCatching { Preset.valueOf(head[1]) }.getOrDefault(Preset.DEFAULT),
                speedIndex = head[2].toInt(),
                paused = head[3] == "1",
                trailsVisible = head[4] == "1",
                teachingEnabled = head[5] == "1",
                darkTheme = head[6] == "1",
                persian = head[7] == "1",
                marbleBounce = head[8] == "1",
                selectedId = head[9].toLong()
            )
            val simTime = head[10].toDouble()
            val bodies = head[11]
            into.clear()
            if (bodies.isNotBlank()) {
                for (chunk in bodies.split(BODY_SEP)) {
                    val f = chunk.split(FIELD_SEP)
                    if (f.size < 12) continue
                    val slot = into.add(
                        type = BodyType.fromCode(f[1].toInt().toByte()),
                        massKg = f[2].toDouble(),
                        radiusDpValue = f[3].toDouble(),
                        posX = f[4].toDouble(),
                        posY = f[5].toDouble(),
                        velX = f[6].toDouble(),
                        velY = f[7].toDouble(),
                        catalog = if (f[11] == "-") null else f[11],
                        explicitId = f[0].toLong()
                    )
                    if (slot >= 0) {
                        // Merged bodies can legitimately exceed the type dp band, so restore raw.
                        into.setRadiusDpRaw(slot, f[3].toDouble())
                        into.partnerId[slot] = f[8].toLong()
                        into.cooldownUntil[slot] = f[9].toDouble()
                        into.gateMouthId[slot] = f[10].toLong()
                    }
                }
            }
            into.simTime = simTime
            session
        } catch (t: Throwable) {
            null
        }
    }
}

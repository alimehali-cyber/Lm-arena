package com.example.ui.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.astro_engine.CoordinateEngine
import com.example.domain.CelestialObject
import kotlin.math.sin

object ConstellationRenderer {

    // Define constellation star connections by star IDs in catalog
    private val CONSTELLATION_LINES = listOf(
        // Orion (Betelgeuse, Rigel, Bellatrix, Saiph, Alnitak, Alnilam, Mintaka)
        listOf("star_orion_betelgeuse", "star_orion_bellatrix", "star_orion_mintaka", "star_orion_alnilam", "star_orion_alnitak", "star_orion_saiph", "star_orion_rigel"),
        listOf("star_orion_betelgeuse", "star_orion_alnitak"),
        listOf("star_orion_bellatrix", "star_orion_mintaka"),
        listOf("star_orion_alnilam", "star_orion_rigel"),

        // Ursa Major / Big Dipper (Dubhe, Merak, Phecda, Megrez, Alioth, Mizar, Alkaid)
        listOf("star_uma_dubhe", "star_uma_merak", "star_uma_phecda", "star_uma_megrez", "star_uma_alioth", "star_uma_mizar", "star_uma_alkaid"),
        listOf("star_uma_megrez", "star_uma_dubhe"),

        // Cassiopeia W shape (Schedar, Caph, Gamma Cas, Ruchbah, Segin)
        listOf("star_cas_caph", "star_cas_schedar", "star_cas_gamma", "star_cas_ruchbah", "star_cas_segin"),

        // Cygnus Northern Cross (Deneb, Sadr, Albireo, Gienah, Delta Cyg)
        listOf("star_cyg_deneb", "star_cyg_sadr", "star_cyg_albireo"),
        listOf("star_cyg_gienah", "star_cyg_sadr", "star_cyg_delta"),

        // Scorpius (Antares, Shaula, Lesath, Sargas)
        listOf("star_sco_antares", "star_sco_sargas", "star_sco_shaula"),

        // Canis Major (Sirius, Adhara, Wezen, Mirzam)
        listOf("star_cma_sirius", "star_cma_mirzam", "star_cma_adhara", "star_cma_wezen"),

        // Taurus (Aldebaran, Elnath)
        listOf("star_tau_aldebaran", "star_tau_elnath")
    )

    fun drawConstellationLines(
        drawScope: DrawScope,
        stars: List<Pair<CelestialObject, CoordinateEngine.Horizontal>>,
        starVisibility: Float,
        frameTimeMs: Long
    ) {
        if (starVisibility < 0.2f) return

        val width = drawScope.size.width
        val height = drawScope.size.height

        // Map star ID to horizontal screen offset
        val starMap = stars.associate { (star, horiz) ->
            val sx = (horiz.azimuthDeg / 360.0 * width).toFloat()
            val sy = (height - (horiz.altitudeDeg / 90.0 * height)).toFloat()
            star.id to (Offset(sx, sy) to horiz.altitudeDeg)
        }

        // Pulse line glow gently
        val linePulse = 0.6f + 0.4f * sin(frameTimeMs * 0.0012f).toFloat()
        val lineAlpha = (starVisibility * 0.35f * linePulse).coerceIn(0f, 0.6f)
        val lineColor = Color(0xFF38BDF8).copy(alpha = lineAlpha)

        CONSTELLATION_LINES.forEach { lineGroup ->
            for (i in 0 until lineGroup.size - 1) {
                val id1 = lineGroup[i]
                val id2 = lineGroup[i + 1]

                val p1Info = starMap[id1]
                val p2Info = starMap[id2]

                if (p1Info != null && p2Info != null) {
                    val (p1, alt1) = p1Info
                    val (p2, alt2) = p2Info

                    // Only draw if both stars are above horizon
                    if (alt1 > 0.0 && alt2 > 0.0) {
                        // Soft Outer Glow Line
                        drawScope.drawLine(
                            color = Color(0xFF818CF8).copy(alpha = lineAlpha * 0.4f),
                            start = p1,
                            end = p2,
                            strokeWidth = 3.2f
                        )
                        // Core Delicate Constellation Line
                        drawScope.drawLine(
                            color = lineColor,
                            start = p1,
                            end = p2,
                            strokeWidth = 1.2f
                        )
                    }
                }
            }
        }
    }
}

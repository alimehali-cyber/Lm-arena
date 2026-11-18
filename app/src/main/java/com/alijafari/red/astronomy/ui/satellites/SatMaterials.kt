package com.alijafari.red.astronomy.ui.satellites

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp

/** All slots take the same content and geometry in each skin. Only their paint changes. */
internal enum class SatSlot { StatusPill, RailButton, FocusCard, TimeTray, Sheet, Spotlight, PassCard, Popover, DetailHero }

private interface SatMaterial {
    fun fill(tokens: ZigSatTokens, slot: SatSlot, base: Color, accent: Color?): Brush
    fun rim(tokens: ZigSatTokens, slot: SatSlot, accent: Color?): Brush
}

private fun flat(color: Color): Brush = Brush.linearGradient(listOf(color, color))

private object DefaultSatMaterial : SatMaterial {
    override fun fill(tokens: ZigSatTokens, slot: SatSlot, base: Color, accent: Color?) =
        flat(if (slot == SatSlot.Popover) base else accent?.copy(alpha = 0.08f)?.compositeOver(base) ?: base)
    override fun rim(tokens: ZigSatTokens, slot: SatSlot, accent: Color?) =
        flat(accent?.copy(alpha = if (slot == SatSlot.Popover) 1f else 0.4f) ?: tokens.border)
}

private object SpatialSatMaterial : SatMaterial {
    override fun fill(tokens: ZigSatTokens, slot: SatSlot, base: Color, accent: Color?) = Brush.linearGradient(
        listOf((accent ?: tokens.timeAccent).copy(alpha = 0.10f).compositeOver(base), base.copy(alpha = 0.96f), base),
    )
    override fun rim(tokens: ZigSatTokens, slot: SatSlot, accent: Color?) = Brush.linearGradient(
        listOf(tokens.inkPrimary.copy(alpha = 0.32f), (accent ?: tokens.timeAccent).copy(alpha = 0.33f), tokens.border),
    )
}

private object AetherSatMaterial : SatMaterial {
    override fun fill(tokens: ZigSatTokens, slot: SatSlot, base: Color, accent: Color?): Brush {
        val tint = (accent ?: tokens.timeAccent).copy(alpha = if (slot == SatSlot.DetailHero) 0.22f else 0.12f)
        return if (slot == SatSlot.DetailHero) Brush.linearGradient(listOf(tint.compositeOver(base), base, tint.copy(alpha = .10f).compositeOver(base)))
        else Brush.verticalGradient(listOf(tint.compositeOver(base), base, tint.copy(alpha = .06f).compositeOver(base)))
    }
    override fun rim(tokens: ZigSatTokens, slot: SatSlot, accent: Color?) = Brush.linearGradient(
        listOf((accent ?: tokens.timeAccent).copy(alpha = 0.52f), tokens.border, tokens.inkPrimary.copy(alpha = 0.22f)),
    )
}

internal fun Modifier.satMaterial(
    tokens: ZigSatTokens,
    slot: SatSlot,
    shape: Shape,
    backing: Color = tokens.card,
    accent: Color? = null,
): Modifier {
    val material = when (tokens.skin) {
        SatSkin.DEFAULT -> DefaultSatMaterial
        SatSkin.SPATIAL -> SpatialSatMaterial
        SatSkin.AETHER -> AetherSatMaterial
    }
    val detailMaterial = when {
        tokens.skin == SatSkin.AETHER && slot == SatSlot.DetailHero -> Modifier.drawBehind {
            val glint = Path().apply {
                moveTo(size.width * .63f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height * .52f)
                lineTo(size.width * .57f, size.height * .31f)
                close()
            }
            drawPath(glint, tokens.timeAccent.copy(alpha = .075f))
        }
        tokens.skin == SatSkin.SPATIAL && slot in setOf(SatSlot.FocusCard, SatSlot.TimeTray, SatSlot.Spotlight) -> Modifier.drawBehind {
            // Sparse low-alpha glints give the glass a mesh without putting texture under glyphs.
            val start = size.width * .78f
            for (i in 0..5) {
                val y = (i + 1) * size.height / 8
                drawLine(tokens.inkPrimary.copy(alpha = .033f), Offset(start, y),
                    Offset(size.width, y + size.height * .20f), 1.dp.toPx())
            }
        }
        else -> Modifier
    }
    return clip(shape).background(material.fill(tokens, slot, backing, accent))
        .then(detailMaterial).border(1.dp, material.rim(tokens, slot, accent), shape)
}

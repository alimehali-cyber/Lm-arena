package com.alijafari.red.astronomy.ui.satellites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.unit.*
import com.alijafari.red.astronomy.astro_engine.SatelliteItem
import com.alijafari.red.astronomy.ui.satellites.core.*

internal val SatelliteItem.taxonomy
    get() = SatCategory.fromGroup(tleGroup)

internal fun shapeOf(s: SatelliteItem) = SatShapeRegistry.resolve(s.noradId, s.nameEn, s.taxonomy)

internal enum class Lod {
    M,
    L,
    XL,
}

internal object SpacecraftArt {
    private val vectors = mutableMapOf<Pair<String, Lod>, ImageVector>()
    private val markerPaths = plates.mapValues { (_, plate) ->
        PathParser().parsePathString(plate.marker).toPath().apply { fillType = PathFillType.EvenOdd }
    }

    fun marker(id: String) = markerPaths.getValue(id)

    fun vector(id: String, lod: Lod) =
        vectors.getOrPut(id to lod) {
            val p = plates.getValue(id)
            val d =
                p.marker + (if (lod != Lod.M) " " + p.tileCuts else "") + (if (lod == Lod.XL) " " + p.plateCuts else "")
            ImageVector.Builder(id, 64.dp, 64.dp, 64f, 64f)
                .apply {
                    addPath(
                        pathData = PathParser().parsePathString(d).toNodes(),
                        fill = SolidColor(Color.Black),
                        pathFillType = PathFillType.EvenOdd,
                    )
                }
                .build()
        }
}

@Composable
internal fun SatAvatar(item: SatelliteItem, tokens: ZigSatTokens, size: Dp = 40.dp) {
    val shape = remember(item.noradId, item.nameEn) { shapeOf(item) }
    Box(Modifier.size(size).background(tokens.cardAlt, SatShapes.tile), contentAlignment = Alignment.Center) {
        Icon(
            SpacecraftArt.vector(shape.id, Lod.L),
            null,
            Modifier.size(size * .7f),
            tint = if (shape.id == "starlink") tokens.starlink else tokens.inkPrimary,
        )
    }
}

package com.alijafari.red.astronomy.ui.satellites

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*

internal enum class SatSlot {
    StatusPill,
    Toolbar,
    FocusCard,
    Sheet,
    Tray,
    SatRow,
    PassRow,
    Spotlight,
    Chip,
    Popover,
    DetailHero,
}

internal val LocalFlatSatGlass = compositionLocalOf { false }

internal class Frost(val layer: GraphicsLayer) {
    var origin by mutableStateOf(Offset.Zero)
}

internal val LocalFrost = compositionLocalOf<Frost?> { null }

/**
 * The panels only ever sit on free space. Sample the space backdrop, not the expensive EGL surface, and blur that *one*
 * layer, never the content, glyphs, or the user's whole window.
 */
@Composable
internal fun FrostProvider(tokens: ZigSatTokens, content: @Composable () -> Unit) {
    if (!tokens.glassy || LocalFlatSatGlass.current || Build.VERSION.SDK_INT < 31) {
        content()
        return
    }
    val layer = rememberGraphicsLayer()
    val frost = remember(layer) { Frost(layer) }
    val blur = with(LocalDensity.current) { if (tokens.skin == SatSkin.AETHER) 14.dp.toPx() else 8.dp.toPx() }
    SideEffect { layer.renderEffect = BlurEffect(blur, blur, TileMode.Clamp) }
    CompositionLocalProvider(LocalFrost provides frost, content = content)
}

@Composable
internal fun Modifier.captureSpace(): Modifier {
    val frost = LocalFrost.current ?: return this
    return onGloballyPositioned { frost.origin = it.positionInRoot() }
        .drawWithContent {
            frost.layer.record { this@drawWithContent.drawContent() }
            drawContent()
        }
}

/** Flat fill under every glyph; skin lighting lives on the rim, not in a wash over the content. */
@Composable
internal fun Modifier.satMaterial(
    tokens: ZigSatTokens,
    slot: SatSlot,
    shape: Shape,
    backing: Color = tokens.card,
    accent: Color? = null,
): Modifier {
    val frost = LocalFrost.current
    val base =
        if (frost == null || slot == SatSlot.DetailHero) clip(shape).background(backing)
        else {
            var at by remember { mutableStateOf(Offset.Zero) }
            clip(shape)
                .onGloballyPositioned { at = it.positionInRoot() }
                .drawBehind {
                    val offset = frost.origin - at
                    translate(offset.x, offset.y) { drawLayer(frost.layer) }
                    drawRect(backing.copy(alpha = if (tokens.skin == SatSkin.AETHER) .89f else .82f))
                }
        }
    val flat = tokens.skin == SatSkin.DEFAULT || LocalFlatSatGlass.current
    if (flat) return base.border(Dp.Hairline, accent ?: tokens.border, shape)
    val rim =
        when {
            slot == SatSlot.StatusPill && accent == tokens.sim -> listOf(Color(0xff709df4), Color(0xffb29be1))
            slot == SatSlot.StatusPill && accent == tokens.live -> listOf(Color(0xffe18d9f), Color(0xffe4aa88))
            else -> listOf(tokens.inkPrimary.copy(alpha = .31f), tokens.border, tokens.inkSecondary.copy(alpha = .19f))
        }
    return base.border(Dp.Hairline, Brush.linearGradient(rim), shape)
}

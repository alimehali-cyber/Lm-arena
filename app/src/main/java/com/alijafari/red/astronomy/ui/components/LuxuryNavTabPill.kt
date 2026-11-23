package com.alijafari.red.astronomy.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ------------------------------------------------------------------ */
/*  The bottom navigation — the selected tab is a luxurious burgundy    */
/*  capsule (#660033): layered silk-radial gradient, a diagonal satin   */
/*  sheen, lacquered top light, deep inner shadow, champagne hairline   */
/*  rim and a deterministic micro-grain texture. Pure composition over   */
/*  static brushes — no simulation, no per-frame work.                  */
/* ------------------------------------------------------------------ */

/** The one and only burgundy — the design's #660033. */
val LuxuryBurgundy = Color(0xFF660033)

/** Silk-lit burgundy ramp, darkest at the rim, #660033 at the heart, dusty-rose light at the top. */
private val BurgundySparkLo = Color(0xFF1F000F)   // lacquer edge, near-black wine
private val BurgundyDeep    = Color(0xFF2E0019)   // the deep companion
private val BurgundyDark    = Color(0xFF47001F)   // shadowed wine
private val BurgundyBody    = Color(0xFF660033)   // the design's burgundy
private val BurgundySilk    = Color(0xFF7E1243)   // silk over the body
private val BurgundyShine   = Color(0xFFA63E67)   // dusty-rose silk light

/** Champagne — the whisper of gold in the hairline rim. */
private val Champagne = Color(0xFFF2C9D4)

/** One destination shown inside the navigation bar. */
data class NavTabSpec(
    /** The tab index this cell selects (its stable geometry key). */
    val key: Int,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

/* ------------------------------------------------------------------ */
/*  Deterministic micro-grain: spun-silk tooth under the lacquer.       */
/*  Generated once; the same bite of texture in every capsule.          */
/* ------------------------------------------------------------------ */

private data class Speckle(
    /** Position in unit capsule space (0..1). */
    val x: Float,
    val y: Float,
    val radius: Float,
    val light: Boolean,
    val alpha: Float,
)

private val GrainSpeckles: List<Speckle> = run {
    val rnd = java.util.Random(0x660033.toLong())
    List(260) {
        Speckle(
            x = rnd.nextFloat(),
            y = rnd.nextFloat(),
            radius = 0.004f + rnd.nextFloat() * 0.012f,
            light = rnd.nextBoolean(),
            alpha = 0.05f + rnd.nextFloat() * 0.11f,
        )
    }
}

/** A handful of silk sparks — tiny glints where the weave catches the light. */
private data class Sparkle(val x: Float, val y: Float, val radius: Float, val alpha: Float)

private val SilkSparkles: List<Sparkle> = run {
    val rnd = java.util.Random(0x5EED)
    List(7) {
        Sparkle(
            x = 0.12f + rnd.nextFloat() * 0.76f,
            y = 0.18f + rnd.nextFloat() * 0.40f,
            radius = 0.010f + rnd.nextFloat() * 0.014f,
            alpha = 0.22f + rnd.nextFloat() * 0.30f,
        )
    }
}

/* ------------------------------------------------------------------ */
/*  The luxurious burgundy capsule, painted in five passes              */
/* ------------------------------------------------------------------ */

private fun DrawScope.drawLuxuryBurgundyCapsule() {
    val w = size.width
    val h = size.height
    val pill = Path().apply {
        addRoundRect(
            RoundRect(
                rect = Rect(Offset.Zero, size),
                cornerRadius = CornerRadius(h / 2f, h / 2f),
            )
        )
    }
    clipPath(pill) {
        // 1. Spun-silk radial body: #660033 heart under a dusty-rose glow, deep wine at the rim.
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(BurgundyShine, BurgundySilk, BurgundyBody, BurgundyDark, BurgundyDeep),
                center = Offset(w * 0.5f, h * 0.16f),
                radius = h * 2.1f,
            )
        )
        // 2. Diagonal satin sheen — one soft blade of reflected light, never a stripe.
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.Transparent, Color(0x38FFD9E6), Color.Transparent),
                start = Offset(w * 0.08f, 0f),
                end = Offset(w * 0.62f, h),
            )
        )
        // 3. Lacquered top light — the gloss a well-kept burgundy leather folio carries.
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0x57FFD9E4), Color(0x0FFFD9E4), Color.Transparent),
                startY = 0f,
                endY = h * 0.38f,
            )
        )
        // 4. Deep inner shadow along the bottom — the capsule sits IN the bar, not on it.
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color(0x0F1F000F), Color(0x66000000)),
                startY = h * 0.52f,
                endY = h,
            )
        )
        // 5. Micro-grain tooth + silk sparks — the texture of woven burgundy.
        GrainSpeckles.forEach { s ->
            drawCircle(
                color = (if (s.light) Color.White else Color.Black).copy(alpha = s.alpha),
                radius = s.radius * h,
                center = Offset(s.x * w, s.y * h),
            )
        }
        SilkSparkles.forEach { s ->
            drawCircle(
                color = Color(0xFFFFE9EF).copy(alpha = s.alpha),
                radius = s.radius * h,
                center = Offset(s.x * w, s.y * h),
            )
        }
        // The very last touch: a light caught along the champagne hairline above the fold.
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Champagne.copy(alpha = 0.55f), Color.Transparent),
                startY = 0f,
                endY = h * 0.18f,
            )
        )
    }
    // Champagne hairline rim — thin, lit from above, the last jeweller's touch.
    drawPath(
        path = pill,
        color = Champagne.copy(alpha = 0.28f),
        style = Stroke(width = 1.3f),
    )
}

/* ------------------------------------------------------------------ */
/*  The row of destinations                                             */
/* ------------------------------------------------------------------ */

/** The weighted row of destinations; the selected one wears the luxurious burgundy capsule. */
@Composable
fun LuxuryNavTabItemsRow(
    selectedIndex: Int,
    specs: List<NavTabSpec>,
    selectedContentColor: Color,
    unselectedContentColor: Color,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        specs.forEach { spec ->
            val isSelected = selectedIndex == spec.key
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                LuxuryNavTabCell(
                    label = spec.label,
                    icon = if (isSelected) spec.selectedIcon else spec.unselectedIcon,
                    selected = isSelected,
                    selectedContentColor = selectedContentColor,
                    unselectedContentColor = unselectedContentColor,
                    onClick = { onSelected(spec.key) },
                    modifier = Modifier.testTag(spec.testTag),
                )
            }
        }
    }
}

/** One destination: the burgundy capsule blooms in behind the icon when this tab is selected. */
@Composable
private fun LuxuryNavTabCell(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    selectedContentColor: Color,
    unselectedContentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reveal by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "LuxuryReveal",
    )
    val tint by animateColorAsState(
        targetValue = if (selected) selectedContentColor else unselectedContentColor,
        animationSpec = tween(durationMillis = 220),
        label = "LuxuryTint",
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.9f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow),
        label = "LuxuryScale",
    )
    val capsule = RoundedCornerShape(percent = 50)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (reveal > 0.01f) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 3.dp, vertical = 4.dp)
                    .shadow(
                        elevation = 10.dp,
                        shape = capsule,
                        ambientColor = BurgundySparkLo.copy(alpha = 0.55f),
                        spotColor = BurgundyBody.copy(alpha = 0.55f),
                    )
                    .alpha(reveal)
                    .scale(0.80f + 0.20f * reveal),
            ) {
                drawLuxuryBurgundyCapsule()
            }
        }

        Column(
            modifier = modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                )
                .padding(top = 8.dp, bottom = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier
                    .size(22.dp)
                    .scale(scale)
                    .offset { IntOffset(0, if (selected) -1.dp.roundToPx() else 0) },
            )
            Text(
                text = label,
                color = tint,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

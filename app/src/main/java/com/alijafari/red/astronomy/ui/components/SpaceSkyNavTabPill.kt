package com.alijafari.red.astronomy.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.R

/* ------------------------------------------------------------------ */
/*  The bottom navigation — the selected tab is a jet-black deep space  */
/*  sky: a real nebula photograph composited under the capsule, blue    */
/*  light ribbons, violet nebulous clouds and thousands of star         */
/*  sprinkles, twinkling live. Pure composition over static art plus    */
/*  a soft loop — no simulation, no per-frame physics.                  */
/* ------------------------------------------------------------------ */

/** The starlight hairline that rims the capsule — cool, thin, lit from above. */
private val Starlight = Color(0xFFCFE0FF)

/** A few spectral tints for the sprinkle field. */
private val StarWhite = Color(0xFFF2F6FF)
private val StarBlue = Color(0xFF9CC3FF)
private val StarWarm = Color(0xFFFFD9A8)

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
/*  Deterministic sprinkle fields — generated once, same sky forever.   */
/* ------------------------------------------------------------------ */

private data class Sprinkle(
    /** Position in unit capsule space (0..1). */
    val x: Float,
    val y: Float,
    val radius: Float,
    val alpha: Float,
    val color: Color,
)

private val StarSprinkles: List<Sprinkle> = run {
    val rnd = java.util.Random(0x57A4A5)
    List(170) {
        val pick = rnd.nextFloat()
        Sprinkle(
            x = rnd.nextFloat(),
            y = rnd.nextFloat(),
            radius = 0.006f + rnd.nextFloat() * 0.013f,
            alpha = 0.22f + rnd.nextFloat() * 0.60f,
            color = when {
                pick < 0.62f -> StarWhite
                pick < 0.90f -> StarBlue
                else -> StarWarm
            },
        )
    }
}

/** The bright sprinkles that twinkle — six out-of-phase stars breathing in 0..1. */
private data class TwinklingStar(
    val x: Float,
    val y: Float,
    val radius: Float,
    val alpha: Float,
    val phase: Float,
)

private val TwinklingStars: List<TwinklingStar> = run {
    val rnd = java.util.Random(0xBADC0DE)
    List(6) { index ->
        TwinklingStar(
            x = 0.08f + rnd.nextFloat() * 0.84f,
            y = 0.16f + rnd.nextFloat() * 0.56f,
            radius = 0.013f + rnd.nextFloat() * 0.009f,
            alpha = 0.55f + rnd.nextFloat() * 0.35f,
            phase = index * 0.37f,
        )
    }
}

/* ------------------------------------------------------------------ */
/*  The jet-black space-sky capsule, painted in passes                  */
/* ------------------------------------------------------------------ */

private fun DrawScope.drawSpaceSkyCapsule(nebula: ImageBitmap, twinkle: Float) {
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
        // The nebula itself — cover-cropped to the capsule so the sky never letterboxes.
        val canvasAspect = w / h
        val bitmapAspect = nebula.width.toFloat() / nebula.height.toFloat()
        if (bitmapAspect >= canvasAspect) {
            val srcWidth = (nebula.height * canvasAspect).toInt()
            val srcX = ((nebula.width - srcWidth) / 2f).toInt()
            drawImage(
                image = nebula,
                srcOffset = IntOffset(srcX, 0),
                srcSize = IntSize(srcWidth, nebula.height),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(w.toInt(), h.toInt()),
            )
        } else {
            val srcHeight = (nebula.width / canvasAspect).toInt()
            val srcY = ((nebula.height - srcHeight) / 2f).toInt()
            drawImage(
                image = nebula,
                srcOffset = IntOffset(0, srcY),
                srcSize = IntSize(nebula.width, srcHeight),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(w.toInt(), h.toInt()),
            )
        }
        // The black at the edges — the sky must read as jet black, not lifted gray.
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color(0x40000000), Color(0x80000000)),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = h * 1.35f,
            )
        )
        // One ultra-thin blade of incident light — glass, not paint.
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.Transparent, Color(0x14FFFFFF), Color.Transparent),
                start = Offset(w * 0.10f, 0f),
                end = Offset(w * 0.60f, h),
            )
        )
        // The faintest lacquer along the very top edge.
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0x2EFFFFFF), Color(0x0AFFFFFF), Color.Transparent),
                startY = 0f,
                endY = h * 0.32f,
            )
        )
        // Star sprinkles — the static field.
        StarSprinkles.forEach { s ->
            drawCircle(
                color = s.color.copy(alpha = s.alpha),
                radius = s.radius * h,
                center = Offset(s.x * w, s.y * h),
            )
        }
        // The twinkle field — six bright stars breathing out of phase.
        TwinklingStars.forEach { s ->
            val wave = 0.5f + 0.5f * kotlin.math.sin((twinkle + s.phase) * 2f * Math.PI.toFloat())
            val breathe = 0.22f + 0.78f * wave
            val radius = s.radius * h
            drawCircle(
                color = StarWhite.copy(alpha = s.alpha * breathe),
                radius = radius,
                center = Offset(s.x * w, s.y * h),
            )
            if (wave > 0.62f) {
                val cross = radius * 3.1f
                val lineAlpha = (wave - 0.62f) / 0.38f * s.alpha
                drawLine(
                    color = StarWhite.copy(alpha = lineAlpha),
                    start = Offset(s.x * w - cross, s.y * h),
                    end = Offset(s.x * w + cross, s.y * h),
                    strokeWidth = 1.15f,
                )
                drawLine(
                    color = StarWhite.copy(alpha = lineAlpha),
                    start = Offset(s.x * w, s.y * h - cross),
                    end = Offset(s.x * w, s.y * h + cross),
                    strokeWidth = 1.15f,
                )
            }
        }
        // The faintest starlight kiss along the top edge, caught inside the capsule.
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Starlight.copy(alpha = 0.50f), Color.Transparent),
                startY = 0f,
                endY = h * 0.20f,
            )
        )
    }
    // Starlight hairline rim — thin, cool, lit from above.
    drawPath(
        path = pill,
        color = Starlight.copy(alpha = 0.32f),
        style = Stroke(width = 1.25f),
    )
}

/* ------------------------------------------------------------------ */
/*  The row of destinations                                             */
/* ------------------------------------------------------------------ */

/** The weighted row of destinations; the selected one wears the jet-black space-sky capsule. */
@Composable
fun SpaceSkyNavTabItemsRow(
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
                SpaceSkyNavTabCell(
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

/** One destination: the space-sky capsule springs in behind the icon when this tab is selected. */
@Composable
private fun SpaceSkyNavTabCell(
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
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
        label = "SpaceReveal",
    )
    val tint by animateColorAsState(
        targetValue = if (selected) selectedContentColor else unselectedContentColor,
        animationSpec = tween(durationMillis = 220),
        label = "SpaceTint",
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.9f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow),
        label = "SpaceScale",
    )
    // The twinkle heartbeat — six stars slowly breathing out of phase.
    val infinite = rememberInfiniteTransition(label = "StarTwinkle")
    val twinkle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "StarTwinklePhase",
    )
    val nebula = ImageBitmap.imageResource(R.drawable.tab_nebula)
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
                        ambientColor = Color(0xFF0A0F1E).copy(alpha = 0.60f),
                        spotColor = Color(0xFF16243E).copy(alpha = 0.60f),
                    )
                    .alpha(reveal)
                    .scale(0.78f + 0.22f * reveal),
            ) {
                drawSpaceSkyCapsule(nebula = nebula, twinkle = twinkle)
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

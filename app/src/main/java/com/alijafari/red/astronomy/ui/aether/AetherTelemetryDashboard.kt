package com.alijafari.red.astronomy.ui.aether

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.astro_engine.MoonEngine
import com.alijafari.red.astronomy.domain.AppLanguage
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.cyber.CyberMatrixText
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val Rim = Brush.linearGradient(listOf(Color.White.copy(alpha = .45f), Color.White.copy(alpha = .04f), Color.White.copy(alpha = .20f)))
private val WhiteMuted = Color.White.copy(alpha = .65f)

/** Canvas fallback works on all supported Android versions, including devices without RuntimeShader. */
@Composable
fun AetherAmbientBackground(modifier: Modifier = Modifier) {
    val motion = rememberInfiniteTransition(label = "aether ambient")
    val phase by motion.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "drift")
    Canvas(modifier.background(Color(0xFF08080C))) {
        val w = size.width
        val h = size.height
        drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF59092B).copy(alpha = .5f), Color.Transparent),
            center = Offset(w * (.42f + .16f * sin(phase)), h * (.22f + .06f * sin(phase * 2))), radius = w * .85f), radius = w * .85f,
            center = Offset(w * (.42f + .16f * sin(phase)), h * (.22f + .06f * sin(phase * 2))))
        drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF14385C).copy(alpha = .42f), Color.Transparent),
            center = Offset(w * (.62f - .16f * sin(phase)), h * (.7f + .05f * sin(phase * 2))), radius = w * .8f), radius = w * .8f,
            center = Offset(w * (.62f - .16f * sin(phase)), h * (.7f + .05f * sin(phase * 2))))
    }
}

private fun Modifier.rim(radius: Int) = border(1.dp, Rim, RoundedCornerShape(radius.dp))

@Composable
private fun Modifier.tactile(onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .97f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow), label = "press")
    return scale(scale).clickable(interactionSource = source, indication = null, onClick = onClick)
}

/** Astronomy values are derived from the same live/simulated instant as the rest of Home. */
@Composable
fun AetherTelemetryDashboard(
    uiState: MainUiState,
    moonData: MoonEngine.MoonData,
    sunAltitude: Double,
    onSettings: () -> Unit,
    onLocation: () -> Unit,
    onMoon: () -> Unit,
    onNudgeMoon: (Float) -> Unit,
    onSky: () -> Unit
) {
    val fa = uiState.language == AppLanguage.PERSIAN
    val haptics = LocalHapticFeedback.current
    val illumination = moonData.illuminationPercent.roundToInt().coerceIn(0, 100)
    val progress = remember { Animatable(0f) }
    LaunchedEffect(illumination) { progress.animateTo(illumination / 100f, spring(dampingRatio = .8f, stiffness = 200f)) }
    Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp).padding(top = 16.dp).testTag("aether_dashboard"),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f, false).clip(RoundedCornerShape(100.dp)).background(Color.White.copy(alpha = .08f)).rim(100).clickable(onClick = onLocation).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("✦", color = Color(0xFFFFB7D1), fontSize = 16.sp)
                Spacer(Modifier.width(9.dp))
                Text(if (fa) uiState.userLocation.cityNameFa else uiState.userLocation.cityNameEn, color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(12.dp))
            Box(Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = .08f)).border(1.dp, Rim, CircleShape).clickable(onClick = onSettings), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Settings, contentDescription = if (fa) "تنظیمات" else "Settings", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
        Column(Modifier.fillMaxWidth().tactile(onMoon).clip(RoundedCornerShape(36.dp))
            .background(Brush.radialGradient(listOf(Color(0xFF1C020D), Color(0xFF5E052A), Color(0xFF9E0B45)), radius = 850f))
            .rim(36).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (fa) "تلـمتری ماه" else "Lunar telemetry", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                Text(if (uiState.timeMachineState.mode == com.alijafari.red.astronomy.domain.TimeMachineMode.SIMULATION || uiState.moonDayOffset != 0f) "✦  PREVIEW" else "✦  LIVE", color = WhiteMuted, fontSize = 10.sp, letterSpacing = 1.sp)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    AnimatedContent(targetState = illumination, transitionSpec = { (slideInVertically { it } + fadeIn()).togetherWith(slideOutVertically { -it } + fadeOut()) }, label = "illumination") { value ->
                        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.semantics { contentDescription = "$value percent" }) {
                            CyberMatrixText(value.toString().padStart(2, '0'), dotSize = 4.dp, dotGap = 1.5.dp, charGap = 4.dp, activeColor = Color.White, glowColor = Color.White)
                            Text("%", color = Color.White, fontSize = 22.sp, modifier = Modifier.padding(start = 5.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(if (fa) "روشنایی ماه" else "moon illumination", color = WhiteMuted, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(if (fa) moonData.phaseNameFa else moonData.phaseNameEn, color = Color.White, fontSize = 13.sp)
                    Spacer(Modifier.height(5.dp))
                    Text(if (fa) "فاز کنونی" else "CURRENT PHASE", color = WhiteMuted, fontSize = 10.sp, letterSpacing = .5.sp)
                }
            }
            AetherWave(progress.value, Modifier.fillMaxWidth().height(55.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    AnimatedContent(targetState = moonData.ageDays, transitionSpec = { (slideInVertically { it / 2 } + fadeIn()).togetherWith(slideOutVertically { -it / 2 } + fadeOut()) }, label = "moon age") { age ->
                        Text(String.format(Locale.US, "%.1f", age) + if (fa) " روز" else " days", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Light)
                    }
                    Text(if (fa) "سن ماه" else "LUNAR AGE", color = WhiteMuted, fontSize = 10.sp, letterSpacing = 1.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    listOf(-1f, 1f).forEach { delta ->
                        Box(Modifier.size(44.dp).clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onNudgeMoon(delta)
                        }.semantics { contentDescription = if (delta < 0) { if (fa) "روز قبل" else "Previous day" } else { if (fa) "روز بعد" else "Next day" } }, contentAlignment = Alignment.Center) {
                            Text(if (delta < 0) "←" else "→", color = Color.White, fontSize = 22.sp)
                        }
                    }
                }
            }
            if (uiState.moonDayOffset != 0f) Text(if (fa) "پیش‌نمایش روزهای دیگر" else "DAY OFFSET PREVIEW", color = WhiteMuted, fontSize = 10.sp)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.weight(1f).aspectRatio(1f).tactile(onMoon).clip(RoundedCornerShape(34.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF63504E), Color(0xFF283F53)))).rim(34), contentAlignment = Alignment.Center) {
                AetherGauge(illumination / 100f, Modifier.fillMaxSize().padding(18.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CyberMatrixText(illumination.toString(), dotSize = 2.7.dp, dotGap = 1.dp, activeColor = Color.White, glowColor = Color.White)
                    Spacer(Modifier.height(6.dp))
                    Text(if (fa) "روشنایی ٪" else "% illuminated", color = WhiteMuted, fontSize = 11.sp)
                }
            }
            Column(Modifier.weight(1f).aspectRatio(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                MetricPill(if (fa) "ارتفاع خورشید" else "Sun altitude", String.format(Locale.US, "%.0f°", sunAltitude), if (sunAltitude < 0) (if (fa) "زیر افق" else "BELOW") else (if (fa) "بالای افق" else "ABOVE"), listOf(Color(0xFFAA547C), Color(0xFF32233B)), Modifier.weight(1f), onSky)
                MetricPill(if (fa) "آلودگی نوری" else "Light pollution", uiState.bortleClass.toString(), "BORTLE", listOf(Color(0xFFB86537), Color(0xFF3D1D14)), Modifier.weight(1f), onSettings)
            }
        }
        Text(if (fa) "برای کاوش بیشتر به پایین بروید  ↓" else "SCROLL TO EXPLORE THE SKY  ↓", color = WhiteMuted, fontSize = 10.sp, letterSpacing = 1.2.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 4.dp))
    }
}

@Composable
private fun MetricPill(title: String, value: String, badge: String, colors: List<Color>, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.fillMaxWidth().tactile(onClick).clip(RoundedCornerShape(50)).background(Brush.linearGradient(colors)).border(1.dp, Rim, RoundedCornerShape(50)).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Text(title, color = WhiteMuted, fontSize = 11.sp, maxLines = 1)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(value, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Light, maxLines = 1)
            Text(badge, color = Color.White.copy(alpha = .8f), fontSize = 9.sp, maxLines = 1)
        }
    }
}

@Composable
private fun AetherGauge(progress: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val radius = size.minDimension * .46f
        val c = center
        repeat(60) { i ->
            val angle = (i * 2 * PI / 60 - PI / 2)
            val lit = i < (progress * 60).roundToInt()
            val inner = radius * if (lit && i % 5 == 0) .80f else .91f
            drawLine(Color.White.copy(alpha = if (lit) .95f else .25f), Offset(c.x + inner * cos(angle).toFloat(), c.y + inner * sin(angle).toFloat()), Offset(c.x + radius * cos(angle).toFloat(), c.y + radius * sin(angle).toFloat()), strokeWidth = 1.3.dp.toPx(), cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun AetherWave(progress: Float, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "wave glow")
    val glow by transition.animateFloat(.7f, 1f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "glow")
    Canvas(modifier) {
        val baseline = size.height * .8f
        val path = Path().apply {
            moveTo(0f, baseline)
            val step = size.width / 7f
            repeat(5) { i ->
                val x = i * step
                cubicTo(x, baseline - size.height * .85f, x + step * .55f, baseline - size.height * .85f, x + step * .55f, baseline)
                lineTo(x + step, baseline)
            }
            lineTo(size.width, baseline)
        }
        drawPath(path, Color.White.copy(alpha = .3f), style = Stroke(1.3.dp.toPx(), cap = StrokeCap.Round))
        // Reveal the same path from left to right as the live value changes.
        clipRect(right = size.width * progress.coerceIn(0f, 1f)) {
            drawPath(path, Color.White.copy(alpha = glow * .18f), style = Stroke(7.dp.toPx(), cap = StrokeCap.Round))
            drawPath(path, Color.White.copy(alpha = glow), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}

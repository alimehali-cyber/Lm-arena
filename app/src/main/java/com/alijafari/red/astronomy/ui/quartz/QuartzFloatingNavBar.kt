package com.alijafari.red.astronomy.ui.quartz

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.R

private data class QuartzDockTab(
    val index: Int,
    val labelRes: Int,
    val glyph: QuartzGlyph,
    val tag: String
)

/**
 * Floating quartz dock: a smoked dark capsule with an inset dotted track, a 0.75.dp specular rim, and
 * the active destination marked by a white hairline capsule + a glowing crimson signal dot. Icons are
 * [QuartzGlyph] vectors — the app's navigation carries nothing from the Material icon set here.
 */
@Composable
fun QuartzFloatingNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = QuartzTheme.palette
    val haptics = LocalHapticFeedback.current
    val tabs = remember {
        listOf(
            QuartzDockTab(0, R.string.nav_lab, QuartzGlyph.Flask, "nav_tab_0"),
            QuartzDockTab(3, R.string.nav_arsky, QuartzGlyph.Crosshair, "nav_tab_3"),
            QuartzDockTab(1, R.string.nav_satellites, QuartzGlyph.Satellite, "nav_tab_1"),
            QuartzDockTab(4, R.string.nav_home, QuartzGlyph.Moon, "nav_tab_4")
        )
    }
    val shape = RoundedCornerShape(32.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .testTag("floating_bottom_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF161A1F).copy(alpha = 0.98f),
                            Color(0xFF0C0F13).copy(alpha = 0.98f)
                        )
                    )
                )
                .opticalQuartzSurface(
                    cornerRadius = 32.dp,
                    refractionColor = Color.White,
                    refractionReach = 14.dp,
                    refractionAlpha = 0.16f,
                    rimHighlightAlpha = 0.34f,
                    grainAlpha = palette.grainAlpha * 0.8f,
                    bloom = false
                )
                .quartzInnerDottedTrack(
                    cornerRadius = 32.dp,
                    color = Color.White.copy(alpha = 0.14f),
                    inset = 5.dp
                )
                .padding(horizontal = 7.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isSelected = selectedTab == tab.index
                val itemScale by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0.94f,
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f),
                    label = "quartz_nav_scale"
                )
                Column(
                    modifier = Modifier
                        .weight(if (isSelected) 1.22f else 0.93f)
                        .fillMaxHeight()
                        .scale(itemScale)
                        .clip(RoundedCornerShape(50))
                        .then(
                            if (isSelected) {
                                Modifier
                                    .background(Color.White.copy(alpha = 0.10f))
                                    .drawWithCache {
                                        val stroke = 0.8.dp.toPx()
                                        onDrawBehind {
                                            drawRoundRect(
                                                color = Color.White.copy(alpha = 0.55f),
                                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                                                    size.height / 2f,
                                                    size.height / 2f
                                                ),
                                                style = Stroke(width = stroke)
                                            )
                                        }
                                    }
                            } else {
                                Modifier
                            }
                        )
                        .quartzTapTarget(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTabSelected(tab.index)
                            }
                        )
                        .testTag(tab.tag),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (isSelected) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            QuartzSignalDot(color = palette.accentCrimson, size = 5.dp)
                            QuartzVectorGlyph(
                                glyph = tab.glyph,
                                color = Color.White,
                                modifier = Modifier.size(15.dp),
                                strokeWidth = 1.15.dp
                            )
                            Text(
                                text = stringResource(tab.labelRes),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.2.sp,
                                maxLines = 1
                            )
                        }
                    } else {
                        QuartzVectorGlyph(
                            glyph = tab.glyph,
                            color = Color.White.copy(alpha = 0.46f),
                            modifier = Modifier.size(17.dp),
                            strokeWidth = 1.1.dp
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = stringResource(tab.labelRes),
                            color = Color.White.copy(alpha = 0.34f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Light,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

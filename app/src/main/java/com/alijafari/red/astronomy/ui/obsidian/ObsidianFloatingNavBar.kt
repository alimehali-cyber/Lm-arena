package com.alijafari.red.astronomy.ui.obsidian

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.R
import com.alijafari.red.astronomy.ui.theme.RedTypographyTokens

private data class ObsidianDockTab(
    val index: Int,
    val labelRes: Int,
    val icon: ObsidianVectorIcon,
    val tag: String
)

/**
 * Obsidian Frozen Quartz Dock.
 *
 * A dark smoked-quartz capsule (never a flat bordered bar) wearing the same
 * [opticalQuartzSurface] recipe as every card, with the signature 6.dp inset dashed inner
 * track, a Velvet Crimson active capsule, and Canvas-drawn hairline vector glyphs — no
 * Material icons anywhere.
 */
@Composable
fun ObsidianFloatingNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = ObsidianTheme.palette
    val tabs = remember {
        listOf(
            ObsidianDockTab(0, R.string.nav_lab, ObsidianVectorIcon.LAB, "nav_tab_0"),
            ObsidianDockTab(3, R.string.nav_arsky, ObsidianVectorIcon.AR, "nav_tab_3"),
            ObsidianDockTab(1, R.string.nav_satellites, ObsidianVectorIcon.SATELLITE, "nav_tab_1"),
            ObsidianDockTab(4, R.string.nav_home, ObsidianVectorIcon.HOME, "nav_tab_4")
        )
    }

    val dockShape = RoundedCornerShape(50)
    val dockFill = remember(palette) {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF0E0B0E),
                palette.glassFill,
                Color(0xFF3E6378).copy(alpha = 0.72f)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 8.dp)
            .testTag("floating_bottom_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .clip(dockShape)
                .background(dockFill)
                .opticalQuartzSurface(
                    cornerRadius = 31.dp,
                    innerRefractionColor = Color.White.copy(alpha = 0.28f),
                    rimHighlightAlpha = 0.5f
                )
                .obsidianInnerDottedTrack(cornerRadius = 31.dp)
                .padding(horizontal = 7.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isSelected = selectedTab == tab.index
                val itemScale by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0.95f,
                    animationSpec = spring(
                        dampingRatio = OBSIDIAN_SPRING_DAMPING,
                        stiffness = OBSIDIAN_SPRING_STIFFNESS * 1.42f
                    ),
                    label = "ObsidianNavScale"
                )

                if (isSelected) {
                    Row(
                        modifier = Modifier
                            .weight(1.22f)
                            .fillMaxHeight()
                            .scale(itemScale)
                            .clip(dockShape)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF4B0420), Color(0xFFB81450))
                                )
                            )
                            .opticalQuartzSurface(
                                cornerRadius = 27.dp,
                                innerRefractionColor = Color(0xFFFF7AA8).copy(alpha = 0.25f),
                                rimHighlightAlpha = 0.55f
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onTabSelected(tab.index) }
                            )
                            .testTag(tab.tag),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ObsidianVectorIconGlyph(
                            icon = tab.icon,
                            modifier = Modifier.size(17.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = stringResource(tab.labelRes),
                            style = RedTypographyTokens.caption.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .weight(0.92f)
                            .fillMaxHeight()
                            .scale(itemScale)
                            .clip(dockShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onTabSelected(tab.index) }
                            )
                            .testTag(tab.tag),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        ObsidianVectorIconGlyph(
                            icon = tab.icon,
                            modifier = Modifier.size(18.dp),
                            tint = Color.White.copy(alpha = 0.62f),
                            strokeWidth = 1.1.dp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = stringResource(tab.labelRes),
                            style = RedTypographyTokens.caption.copy(
                                fontWeight = FontWeight.Light,
                                fontSize = 10.sp
                            ),
                            color = Color.White.copy(alpha = 0.55f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

    }
}

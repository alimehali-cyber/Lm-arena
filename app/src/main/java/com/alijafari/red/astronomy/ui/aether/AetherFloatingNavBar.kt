package com.alijafari.red.astronomy.ui.aether

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.SatelliteAlt
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.R
import com.alijafari.red.astronomy.ui.theme.RedSpacing
import com.alijafari.red.astronomy.ui.theme.RedTypographyTokens

private data class AetherDockTab(
    val index: Int,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

/**
 * Floating Aether Telemetry Capsule Dock (`100.dp` corner geometry):
 * Frameless optical capsule (`FrostedGlassFill` `#14FFFFFF`) with razor-thin `1.dp`
 * directional specular rim border, deep crimson jewel active lens, glowing 6.dp LED dot,
 * bouncy spring physics, and `HapticFeedbackType.TextHandleMove` feedback.
 */
@Composable
fun AetherFloatingNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = AetherTheme.palette
    val haptics = LocalHapticFeedback.current
    val tabs = remember {
        listOf(
            AetherDockTab(0, R.string.nav_lab, Icons.Filled.Science, Icons.Outlined.Science, "nav_tab_0"),
            AetherDockTab(3, R.string.nav_arsky, Icons.Filled.Explore, Icons.Outlined.Explore, "nav_tab_3"),
            AetherDockTab(1, R.string.nav_satellites, Icons.Filled.SatelliteAlt, Icons.Outlined.SatelliteAlt, "nav_tab_1"),
            AetherDockTab(4, R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home, "nav_tab_4")
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = RedSpacing.lg, vertical = RedSpacing.sm)
            .testTag("floating_bottom_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .shadow(
                    elevation = 24.dp,
                    shape = RoundedCornerShape(100.dp),
                    ambientColor = palette.crimsonGlow.copy(alpha = 0.28f),
                    spotColor = palette.crimsonGlow.copy(alpha = 0.28f)
                )
                .clip(RoundedCornerShape(100.dp))
                .background(palette.frostedOpticalVeil)
                .background(palette.frostedGlassFill)
                .specularRimBorder(cornerRadius = 100.dp)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isSelected = selectedTab == tab.index
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = when {
                        isPressed -> 0.94f
                        isSelected -> 1.0f
                        else -> 0.97f
                    },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "AetherDockTabScale"
                )

                if (isSelected) {
                    Row(
                        modifier = Modifier
                            .weight(1.22f)
                            .fillMaxHeight()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        AetherCrimsonCoreMid,
                                        palette.crimsonGlow
                                    ),
                                    radius = 220f
                                )
                            )
                            .specularRimBorder(cornerRadius = 50.dp)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTabSelected(tab.index)
                            }
                            .padding(horizontal = 10.dp)
                            .testTag(tab.tag),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(AetherGlowWhite)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = tab.selectedIcon,
                            contentDescription = stringResource(tab.labelRes),
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = stringResource(tab.labelRes),
                            style = RedTypographyTokens.caption.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.5.sp,
                                letterSpacing = (-0.2).sp
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
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .clip(RoundedCornerShape(50))
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTabSelected(tab.index)
                            }
                            .testTag(tab.tag),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = tab.unselectedIcon,
                            contentDescription = stringResource(tab.labelRes),
                            tint = palette.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(tab.labelRes),
                            style = RedTypographyTokens.caption.copy(
                                fontWeight = FontWeight.Light,
                                fontSize = 10.sp
                            ),
                            color = palette.mutedTelemetry,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

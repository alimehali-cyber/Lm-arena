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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.SatelliteAlt
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Settings
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
import com.alijafari.red.astronomy.ui.theme.RedCornerRadius
import com.alijafari.red.astronomy.ui.theme.RedTypographyTokens
import com.alijafari.red.astronomy.ui.theme.squircleShape

/** A little taller than the 46 dp generation, matching the bigger centred chrome cluster. */
private val AETHER_NAV_HEIGHT = 54.dp

private data class AetherDockTab(
    val index: Int,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

/**
 * Floating Aether Telemetry Capsule Dock: a squircle pill with fully rounded ends, a little taller
 * than the previous generation, that travels with the circular settings button as one centred
 * cluster at the bottom of the screen. Frameless optical capsule (`FrostedGlassFill` `#14FFFFFF`)
 * with razor-thin `1.dp` directional specular rim border, deep crimson jewel active lens, bouncy
 * spring physics, and `HapticFeedbackType.TextHandleMove` feedback.
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
            AetherDockTab(4, R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home, "nav_item_home"),
            AetherDockTab(3, R.string.nav_arsky, Icons.Filled.Explore, Icons.Outlined.Explore, "nav_item_arsky"),
            AetherDockTab(1, R.string.nav_satellites, Icons.Filled.SatelliteAlt, Icons.Outlined.SatelliteAlt, "nav_item_satellites"),
            AetherDockTab(0, R.string.nav_lab, Icons.Filled.Science, Icons.Outlined.Science, "nav_item_lab")
        )
    }

    // A pillar: half the bar's height at each end, drawn with the app's squircle rules.
    val shape = squircleShape(RedCornerRadius.pill)
    Box(modifier.testTag("floating_bottom_bar")) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(AETHER_NAV_HEIGHT)
                .shadow(
                    elevation = 24.dp,
                    shape = shape,
                    ambientColor = palette.crimsonGlow.copy(alpha = 0.28f),
                    spotColor = palette.crimsonGlow.copy(alpha = 0.28f)
                )
                .clip(shape)
                .background(palette.frostedOpticalVeil)
                .background(palette.frostedGlassFill)
                .specularRimBorder(shape = shape)
                .padding(horizontal = 8.dp, vertical = 3.dp),
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
                val pill = squircleShape(RedCornerRadius.pill)

                // Icon over label in both states: the dock is a quarter narrower, and a horizontal
                // jewel lens cannot hold a Persian satellite label at this width.
                Column(
                    modifier = Modifier
                        .weight(if (isSelected) 1.08f else 0.97f)
                        .fillMaxHeight()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(pill)
                        .then(
                            if (isSelected) {
                                Modifier
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(AetherCrimsonCoreMid, palette.crimsonGlow),
                                            radius = 220f
                                        )
                                    )
                                    .specularRimBorder(shape = pill)
                            } else {
                                Modifier
                            }
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTabSelected(tab.index)
                        }
                        .padding(vertical = 4.dp, horizontal = 2.dp)
                        .testTag(tab.tag),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = stringResource(tab.labelRes),
                        tint = if (isSelected) Color.White else palette.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = stringResource(tab.labelRes),
                        style = RedTypographyTokens.caption.copy(
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Light,
                            fontSize = 10.5.sp,
                            letterSpacing = (-0.2).sp
                        ),
                        color = if (isSelected) Color.White else palette.mutedTelemetry,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Settings, as a true circle tucked close beside the centred dock: same frosted capsule material and
 * crimson rim, so the dock and it read as one piece of chrome. It appears wherever the dock appears.
 */
@Composable
fun AetherSettingsBubble(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = AetherTheme.palette
    val shape = CircleShape
    Box(
        modifier = modifier
            .size(AETHER_NAV_HEIGHT)
            .testTag("bottom_settings_button")
            .shadow(
                elevation = 24.dp,
                shape = shape,
                ambientColor = palette.crimsonGlow.copy(alpha = 0.28f),
                spotColor = palette.crimsonGlow.copy(alpha = 0.28f)
            )
            .clip(shape)
            .background(palette.frostedOpticalVeil)
            .background(palette.frostedGlassFill)
            .specularRimBorder(shape = shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Settings,
            contentDescription = stringResource(R.string.settings),
            tint = palette.textSecondary,
            modifier = Modifier.size(19.dp)
        )
    }
}

package com.alijafari.red.astronomy.ui.aether

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
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
import com.alijafari.red.astronomy.ui.theme.LocalAppFontFamily
import com.alijafari.red.astronomy.ui.theme.RedShapes
import com.alijafari.red.astronomy.ui.theme.RedTypographyTokens
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

private data class AetherNavTabItem(
    val index: Int,
    val labelResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

/**
 * Frameless Rim-Lit Spatial Navigation Dock (Section 6.4):
 * Uses Haze real-time spatial blur + `FrostedGlassFill` (`8%` white) + `1.dp` directional
 * `specularRimBorder` + glowing `6.dp` active LED telemetry dot and jewel-crimson inner glow.
 */
@Composable
fun AetherFloatingNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = AetherTheme.palette
    val hazeState = AetherTheme.hazeState
    val appFont = LocalAppFontFamily.current
    val haptics = LocalHapticFeedback.current

    val items = listOf(
        AetherNavTabItem(0, R.string.nav_lab, Icons.Filled.Science, Icons.Outlined.Science),
        AetherNavTabItem(3, R.string.nav_arsky, Icons.Filled.Explore, Icons.Outlined.Explore),
        AetherNavTabItem(1, R.string.nav_satellites, Icons.Filled.SatelliteAlt, Icons.Outlined.SatelliteAlt),
        AetherNavTabItem(4, R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home)
    )

    val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val dockShape = RedShapes.capsule
    val hazeStyle = remember(palette) {
        HazeStyle(
            backgroundColor = palette.canvasVoid,
            tint = HazeTint(palette.frostedGlassFill),
            blurRadius = 26.dp,
            noiseFactor = 0.08f
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = 18.dp,
                end = 18.dp,
                top = 6.dp,
                bottom = (navBottomPadding + 10.dp).coerceAtLeast(14.dp)
            )
            .testTag("floating_bottom_bar"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 430.dp)
                .fillMaxWidth()
                .shadow(
                    elevation = if (palette.isDark) 24.dp else 14.dp,
                    shape = dockShape,
                    ambientColor = Color.Black.copy(alpha = if (palette.isDark) 0.55f else 0.14f),
                    spotColor = palette.primaryAccent.copy(alpha = 0.22f)
                )
                .clip(dockShape)
                .then(
                    if (hazeState != null) {
                        Modifier.hazeChild(state = hazeState, style = hazeStyle)
                    } else {
                        Modifier
                    }
                )
                .background(palette.frostedOpticalVeil)
                .background(palette.frostedGlassFill)
                .specularRimBorder(
                    shape = dockShape,
                    highlightStart = palette.specularRimStart,
                    highlightMid = palette.specularRimMid,
                    highlightCenter = palette.specularRimCenter,
                    highlightBounce = palette.specularRimEnd
                )
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = selectedTab == item.index
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = when {
                        isPressed -> 0.93f
                        isSelected -> 1.0f
                        else -> 0.98f
                    },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "aetherNavScale_${item.index}"
                )
                val iconTint by animateColorAsState(
                    targetValue = when {
                        isSelected && palette.isDark -> Color.White
                        isSelected -> palette.primaryAccent
                        else -> palette.textMicro
                    },
                    label = "aetherNavTint_${item.index}"
                )

                val tabLabel = stringResource(item.labelResId)

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(dockShape)
                        .then(
                            if (isSelected) {
                                Modifier
                                    .drawBehind {
                                        drawRect(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    palette.primaryAccent.copy(alpha = if (palette.isDark) 0.42f else 0.16f),
                                                    palette.primaryAccent.copy(alpha = if (palette.isDark) 0.12f else 0.04f)
                                                ),
                                                center = Offset(size.width * 0.5f, size.height * 0.85f),
                                                radius = size.width * 0.85f
                                            )
                                        )
                                    }
                                    .specularRimBorder(
                                        shape = dockShape,
                                        highlightStart = Color.White.copy(alpha = if (palette.isDark) 0.42f else 0.80f),
                                        highlightMid = Color.White.copy(alpha = 0.08f),
                                        highlightCenter = Color.White.copy(alpha = 0.03f),
                                        highlightBounce = palette.primaryAccent.copy(alpha = 0.35f)
                                    )
                            } else {
                                Modifier
                            }
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {
                            if (selectedTab != item.index) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            onTabSelected(item.index)
                        }
                        .testTag("nav_tab_${item.index}")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = tabLabel,
                            tint = iconTint,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tabLabel,
                            style = RedTypographyTokens.caption.copy(
                                fontFamily = appFont,
                                fontSize = 10.sp,
                                lineHeight = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Light
                            ),
                            color = iconTint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 4.5.dp else 0.dp)
                                .clip(CircleShape)
                                .background(
                                    if (palette.isDark) palette.glowWhite else palette.primaryAccent
                                )
                        )
                    }
                }
            }
        }
    }
}

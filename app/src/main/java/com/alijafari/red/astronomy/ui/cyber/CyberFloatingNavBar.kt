package com.alijafari.red.astronomy.ui.cyber

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.R
import com.alijafari.red.astronomy.ui.theme.RedShapes
import com.alijafari.red.astronomy.ui.theme.RedSpacing
import com.alijafari.red.astronomy.ui.theme.RedTypographyTokens

private data class CyberDockTab(
    val index: Int,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

/**
 * Spatial Floating Capsule Dock (Component #4 — Spatial Floating Capsule Bar):
 * Seamless rounded pill dock floating above navigation insets with 4-layer cut glass,
 * sub-frosted Mesh Gradient active pill capsule, ultra-light geometric labels, and
 * Nothing OS / Teenage Engineering glowing signal dot.
 */
@Composable
fun CyberFloatingNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = CyberTheme.palette
    val tabs = remember {
        listOf(
            CyberDockTab(0, R.string.nav_lab, Icons.Filled.Science, Icons.Outlined.Science, "nav_tab_0"),
            CyberDockTab(3, R.string.nav_arsky, Icons.Filled.Explore, Icons.Outlined.Explore, "nav_tab_3"),
            CyberDockTab(1, R.string.nav_satellites, Icons.Filled.SatelliteAlt, Icons.Outlined.SatelliteAlt, "nav_tab_1"),
            CyberDockTab(4, R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home, "nav_tab_4")
        )
    }

    val shape = RedShapes.capsule
    val borderBrush = remember(palette) {
        Brush.verticalGradient(
            0.0f to palette.innerTopHighlight,
            0.40f to palette.glassBorder,
            1.0f to palette.glassBorder.copy(alpha = palette.glassBorder.alpha * 0.7f)
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
                .height(64.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = shape,
                    ambientColor = palette.distanceDropShadow,
                    spotColor = palette.distanceDropShadow
                )
                .clip(shape)
                .background(palette.glassReadabilityTint)
                .background(palette.glassElevatedFill)
                .drawWithContent {
                    drawContent()
                    val topH = 1.6.dp.toPx()
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(palette.innerTopHighlight, Color.Transparent),
                            startY = 0f,
                            endY = topH * 2.2f
                        ),
                        size = Size(size.width, topH * 2.2f)
                    )
                    val bottomH = 2.dp.toPx()
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, palette.innerBottomShadow),
                            startY = size.height - bottomH * 2f,
                            endY = size.height
                        ),
                        topLeft = Offset(0f, size.height - bottomH * 2f),
                        size = Size(size.width, bottomH * 2f)
                    )
                }
                .border(BorderStroke(1.dp, borderBrush), shape)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isSelected = selectedTab == tab.index
                val itemScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.0f else 0.96f,
                    animationSpec = spring(dampingRatio = 0.74f, stiffness = 400f),
                    label = "CyberNavScale"
                )

                if (isSelected) {
                    CyberMeshPillCard(
                        preset = CyberMeshPreset.CYAN_VIOLET,
                        isCapsule = true,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        onClick = { onTabSelected(tab.index) },
                        modifier = Modifier
                            .weight(1.18f)
                            .fillMaxHeight()
                            .scale(itemScale)
                            .testTag(tab.tag)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CyberSignalDot(color = Color.White, size = 5.dp, glowing = false)
                            Spacer(modifier = Modifier.size(5.dp))
                            Icon(
                                imageVector = tab.selectedIcon,
                                contentDescription = stringResource(tab.labelRes),
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(5.dp))
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
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .weight(0.94f)
                            .fillMaxHeight()
                            .scale(itemScale)
                            .clip(RedShapes.capsule)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onTabSelected(tab.index) }
                            )
                            .testTag(tab.tag),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = tab.unselectedIcon,
                            contentDescription = stringResource(tab.labelRes),
                            tint = palette.textSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(tab.labelRes),
                            style = RedTypographyTokens.caption.copy(
                                fontWeight = FontWeight.Light,
                                fontSize = 10.sp
                            ),
                            color = palette.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

package com.alijafari.red.astronomy.ui.spatial

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
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
import com.alijafari.red.astronomy.ui.theme.RedTypographyTokens
import com.alijafari.red.astronomy.ui.theme.squircleShape

private data class SpatialNavTab(
    val index: Int,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

/**
 * VisionOS Floating Spatial Glass Navigation Capsule.
 *
 * Floats above the bottom navigation inset inside a frosted G2 squircle-capsule dock,
 * featuring top-edge specular rim lighting, fluid spring press physics, and a pastel neon
 * radial glow (`#FFB0D0`) behind the active destination.
 */
@Composable
fun SpatialFloatingNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = SpatialTheme.palette
    val tabs = remember {
        listOf(
            SpatialNavTab(4, R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home, "nav_item_home"),
            SpatialNavTab(3, R.string.nav_arsky, Icons.Filled.Explore, Icons.Outlined.Explore, "nav_item_arsky"),
            SpatialNavTab(1, R.string.nav_satellites, Icons.Filled.SatelliteAlt, Icons.Outlined.SatelliteAlt, "nav_item_satellites"),
            SpatialNavTab(0, R.string.nav_lab, Icons.Filled.Science, Icons.Outlined.Science, "nav_item_lab")
        )
    }

    val dockShape = squircleShape(34.dp)
    val rimBrush = remember(palette) {
        Brush.verticalGradient(
            listOf(
                palette.rimTopHighlight,
                palette.rimSideBorder,
                palette.rimBottomReflection
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .testTag("main_bottom_navigation"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("main_bottom_navigation_surface")
                .shadow(
                    elevation = 20.dp,
                    shape = dockShape,
                    ambientColor = palette.ambientDropShadow,
                    spotColor = palette.ambientDropShadow
                )
                .clip(dockShape)
                .background(palette.glassBackingVeil)
                .background(palette.glassElevatedFill)
                .drawWithContent {
                    drawContent()
                    // Inset top white specular highlight
                    val bevel = 2.dp.toPx()
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(palette.innerTopGlow, Color.Transparent),
                            startY = 0f,
                            endY = bevel * 2.2f
                        ),
                        topLeft = Offset.Zero,
                        size = Size(size.width, bevel * 2.2f)
                    )
                }
                .border(BorderStroke(1.dp, rimBrush), dockShape)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    val isSelected = selectedTab == tab.index
                    SpatialNavTabPill(
                        tab = tab,
                        isSelected = isSelected,
                        onClick = { onTabSelected(tab.index) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SpatialNavTabPill(
    tab: SpatialNavTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = SpatialTheme.palette
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.92f
            isSelected -> 1.03f
            else -> 1.0f
        },
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 380f),
        label = "SpatialNavScale"
    )

    val contentTint by animateColorAsState(
        targetValue = if (isSelected) palette.accentInk else palette.textSecondary,
        animationSpec = tween(180),
        label = "SpatialNavTint"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .then(
                if (isSelected) {
                    Modifier
                        .drawBehind {
                            // Pastel neon radial glow (#FFB0D0) beneath active tab pill
                            val r = size.maxDimension * 0.70f
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        SpatialNeonRose.copy(alpha = if (palette.isDark) 0.36f else 0.28f),
                                        Color.Transparent
                                    ),
                                    center = center,
                                    radius = r
                                ),
                                radius = r,
                                center = center
                            )
                        }
                        .clip(RedShapes.capsule)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    SpatialNeonRose.copy(alpha = if (palette.isDark) 0.26f else 0.24f),
                                    SpatialWarmPeach.copy(alpha = if (palette.isDark) 0.16f else 0.18f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.72f),
                                    SpatialNeonRose.copy(alpha = 0.40f)
                                )
                            ),
                            shape = RedShapes.capsule
                        )
                } else {
                    Modifier.clip(RedShapes.capsule)
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 7.dp, horizontal = 4.dp)
            .testTag(tab.testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                contentDescription = stringResource(tab.labelRes),
                tint = contentTint,
                modifier = Modifier.size(21.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = stringResource(tab.labelRes),
                style = RedTypographyTokens.caption.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 11.sp
                ),
                color = contentTint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isSelected) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(palette.accentInk)
                )
            } else {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

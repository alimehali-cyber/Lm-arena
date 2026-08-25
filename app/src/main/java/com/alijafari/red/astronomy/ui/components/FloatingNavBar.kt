package com.alijafari.red.astronomy.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.R
import com.alijafari.red.astronomy.ui.theme.RedCornerRadius
import com.alijafari.red.astronomy.ui.theme.RedElevation
import com.alijafari.red.astronomy.ui.theme.RedIconSize
import com.alijafari.red.astronomy.ui.theme.RedSpacing
import com.alijafari.red.astronomy.ui.theme.RedTheme

data class NavItem(
    val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val targetTabIndex: Int,
    val testTag: String
)

/**
 * Clean, Apple-inspired Floating Navigation Bar
 * Features:
 * - Restrained, clean floating capsule aesthetic
 * - Subdued hairline border with soft ambient elevation
 * - No radial glows or extra decorative dots
 * - Smooth color state transitions and accessible 48dp touch targets
 */
@Composable
fun FloatingBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val navItems = listOf(
        NavItem(R.string.nav_home, Icons.Default.Home, Icons.Outlined.Home, 4, "nav_item_home"),
        NavItem(R.string.nav_arsky, Icons.Default.Explore, Icons.Outlined.Explore, 3, "nav_item_arsky"),
        NavItem(R.string.nav_moon, Icons.Default.NightlightRound, Icons.Outlined.Nightlight, 2, "nav_item_moon"),
        NavItem(R.string.nav_satellites, Icons.Default.SatelliteAlt, Icons.Outlined.SatelliteAlt, 1, "nav_item_satellites"),
        NavItem(R.string.nav_lab, Icons.Default.Science, Icons.Outlined.Science, 0, "nav_item_lab")
    )

    val navShape = RoundedCornerShape(RedCornerRadius.xxl)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = RedSpacing.lg, vertical = RedSpacing.sm)
            .testTag("main_bottom_navigation")
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp),
            shape = navShape,
            color = RedTheme.colors.navSurface,
            shadowElevation = RedElevation.floatingNav,
            border = androidx.compose.foundation.BorderStroke(1.dp, RedTheme.colors.navBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = RedSpacing.xs),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                navItems.forEach { item ->
                    val isSelected = selectedTab == item.targetTabIndex
                    val activeColor by animateColorAsState(
                        targetValue = if (isSelected) RedTheme.colors.accentRed else RedTheme.colors.textSecondary,
                        animationSpec = tween(durationMillis = 200),
                        label = "NavColor"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(RedCornerRadius.md))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onTabSelected(item.targetTabIndex) }
                            .testTag(item.testTag)
                    ) {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = stringResource(item.titleRes),
                            tint = activeColor,
                            modifier = Modifier.size(RedIconSize.md)
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = stringResource(item.titleRes),
                            style = MaterialTheme.typography.labelSmall,
                            color = activeColor,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

package com.alijafari.red.astronomy.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.R
import com.alijafari.red.astronomy.ui.theme.*

data class NavItem(
    val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val targetTabIndex: Int,
    val testTag: String
)

/**
 * RED Liquid Glass Floating Navigation Bar
 *
 * Implements Phase 1 of RED's Liquid Glass System:
 * - Floats physically above actual screen content with high optical transmission (IOR ~ 1.15)
 * - Multi-layer refractive rim & subtle chromatic dispersion highlight
 * - Native backdrop blur on supported devices with graceful fallback
 * - Content-aware optical separation without solid background blocking
 * - Interactive tactile press compression & smooth active item transitions
 * - Accessible 48dp+ touch targets and full RTL/Persian compatibility
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
        LiquidGlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            style = LiquidGlassDefaults.NavigationBar,
            shape = navShape
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = RedSpacing.xs, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                navItems.forEach { item ->
                    val isSelected = selectedTab == item.targetTabIndex
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()

                    val itemScale by animateFloatAsState(
                        targetValue = if (isPressed) 0.94f else 1.0f,
                        animationSpec = spring(dampingRatio = 0.75f, stiffness = 450f),
                        label = "NavItemPressScale"
                    )

                    val activeColor by animateColorAsState(
                        targetValue = if (isSelected) RedTheme.colors.accentRed else RedTheme.colors.textSecondary,
                        animationSpec = tween(durationMillis = 200),
                        label = "NavColor"
                    )

                    val activeBgAlpha by animateFloatAsState(
                        targetValue = if (isSelected) 0.12f else 0.0f,
                        animationSpec = tween(durationMillis = 200),
                        label = "NavActivePillAlpha"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .scale(itemScale)
                            .clip(RoundedCornerShape(RedCornerRadius.pill))
                            .background(
                                color = if (activeBgAlpha > 0f) {
                                    RedTheme.colors.accentRed.copy(alpha = activeBgAlpha)
                                } else {
                                    Color.Transparent
                                }
                            )
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) { onTabSelected(item.targetTabIndex) }
                            .padding(vertical = 4.dp)
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
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = activeColor,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

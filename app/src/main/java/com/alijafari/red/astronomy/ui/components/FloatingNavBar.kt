package com.alijafari.red.astronomy.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.R
import com.alijafari.red.astronomy.ui.theme.*
import com.alijafari.red.astronomy.ui.theme.RedCornerRadius
import com.alijafari.red.astronomy.ui.theme.RedSurface
import com.alijafari.red.astronomy.ui.theme.squircleShape

/**
 * The dock is three quarters of the available width and three quarters of the old height, on the
 * leading side — the right in Persian, the left in English — so the settings bubble has the space it
 * leaves instead of the bar spanning the whole screen.
 */
const val BOTTOM_NAV_WIDTH_FRACTION = 0.75f
val BOTTOM_NAV_HEIGHT: Dp = 64.dp

/** The one silhouette every skin's dock wears: a squircle pill, ends fully rounded. */
internal fun bottomNavShape() = androidx.compose.foundation.shape.CircleShape

data class NavItem(
    val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val targetTabIndex: Int,
    val testTag: String
)

@Composable
fun FloatingBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val navItems = listOf(
        NavItem(R.string.nav_home, Icons.Outlined.Home, Icons.Outlined.Home, 4, "nav_item_home"),
        NavItem(R.string.nav_arsky, Icons.Outlined.Explore, Icons.Outlined.Explore, 3, "nav_item_arsky"),
        NavItem(R.string.nav_satellites, Icons.Outlined.SatelliteAlt, Icons.Outlined.SatelliteAlt, 1, "nav_item_satellites"),
        NavItem(R.string.nav_lab, Icons.Outlined.Science, Icons.Outlined.Science, 0, "nav_item_lab")
    )

    // A pill: the ends are the full half-height of the bar, drawn with the app's squircle rules.
    val navShape = bottomNavShape()

    Box(modifier.testTag("main_bottom_navigation")) {
        RedSurface(
            modifier = Modifier
                .fillMaxWidth()
                .height(BOTTOM_NAV_HEIGHT)
                .testTag("main_bottom_navigation_surface"),
            shape = navShape,
            color = RedTheme.colors.surfaceElevated,
            border = BorderStroke(1.dp, RedTheme.colors.border),
            elevation = RedElevation.floating,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                navItems.forEach { item ->
                    val isSelected = selectedTab == item.targetTabIndex
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()

                    val itemScale by animateFloatAsState(
                        targetValue = if (isPressed) 0.93f else 1.0f,
                        animationSpec = spring(dampingRatio = 0.75f, stiffness = 450f),
                        label = "NavItemPressScale"
                    )

                    val activeColor by animateColorAsState(
                        targetValue = if (isSelected) RedTheme.colors.accentRed else RedTheme.colors.textSecondary,
                        animationSpec = tween(durationMillis = 200),
                        label = "NavColor"
                    )

                    val pillShape = squircleShape(20.dp)

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .scale(itemScale)
                            .clip(pillShape)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) { onTabSelected(item.targetTabIndex) }
                            .padding(vertical = 2.dp)
                            .testTag(item.testTag)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = stringResource(item.titleRes),
                                tint = activeColor,
                                modifier = Modifier.size(RedIconSize.sm)
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
}

/**
 * Settings, in the space the dock leaves on the trailing side. It wears the dock's own material and
 * silhouette so the pair reads as one piece of chrome, and it appears wherever the dock appears.
 */
@Composable
fun FloatingSettingsBubble(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = bottomNavShape()
    RedSurface(
        modifier = modifier
            .size(BOTTOM_NAV_HEIGHT)
            .testTag("bottom_settings_button"),
        shape = shape,
        color = RedTheme.colors.surfaceElevated,
        border = BorderStroke(1.dp, RedTheme.colors.border),
        elevation = RedElevation.floating,
        onClick = onClick,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = stringResource(R.string.settings),
                tint = RedTheme.colors.textPrimary,
                modifier = Modifier.size(RedIconSize.sm)
            )
        }
    }
}

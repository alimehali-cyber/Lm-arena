package com.alijafari.red.astronomy.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.SatelliteAlt
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alijafari.red.astronomy.R
import com.alijafari.red.astronomy.ui.theme.RedElevation
import com.alijafari.red.astronomy.ui.theme.RedIconSize
import com.alijafari.red.astronomy.ui.theme.RedShapes
import com.alijafari.red.astronomy.ui.theme.RedSurface
import com.alijafari.red.astronomy.ui.theme.RedTheme

/**
 * The dock and the circular settings button form one cluster, close together and centred at the
 * bottom of the screen on every skin. The dock wears the design's true capsule, with the selected
 * tab shown as a luxurious burgundy #660033 gradient capsule with a woven-silk grain
 * (see [LuxuryNavTabItemsRow]); the settings circle tucks right beside it.
 */
const val BOTTOM_NAV_WIDTH_FRACTION = 0.72f

/** The capsule, per the design. */
val BOTTOM_NAV_HEIGHT: Dp = 64.dp

/** The air between the capsule and the settings circle in the centred cluster. */
val BOTTOM_NAV_GAP: Dp = 10.dp

/** The settings circle sits a touch smaller than the capsule, as in the design. */
val BOTTOM_SETTINGS_SIZE: Dp = 56.dp

/** The one silhouette every skin's dock wears: a true capsule, ends fully rounded. */
internal fun bottomNavShape() = RedShapes.capsule



@Composable
fun FloatingBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.testTag("main_bottom_navigation")) {
        RedSurface(
            modifier = Modifier
                .fillMaxWidth()
                .height(BOTTOM_NAV_HEIGHT)
                .testTag("main_bottom_navigation_surface"),
            shape = bottomNavShape(),
            color = RedTheme.colors.surfaceElevated,
            border = BorderStroke(1.dp, RedTheme.colors.border),
            elevation = RedElevation.floating,
        ) {
            LuxuryNavTabItemsRow(
                modifier = Modifier.fillMaxSize(),
                selectedIndex = selectedTab,
                specs = listOf(
                    NavTabSpec(4, stringResource(R.string.nav_home), Icons.Outlined.Home, Icons.Outlined.Home, "nav_item_home"),
                    NavTabSpec(3, stringResource(R.string.nav_arsky), Icons.Outlined.Explore, Icons.Outlined.Explore, "nav_item_arsky"),
                    NavTabSpec(1, stringResource(R.string.nav_satellites), Icons.Outlined.SatelliteAlt, Icons.Outlined.SatelliteAlt, "nav_item_satellites"),
                    NavTabSpec(0, stringResource(R.string.nav_lab), Icons.Outlined.Science, Icons.Outlined.Science, "nav_item_lab"),
                ),
                selectedContentColor = Color.White,
                unselectedContentColor = RedTheme.colors.textSecondary,
                onSelected = onTabSelected,
            )
        }
    }
}

/**
 * Settings, as a true circle tucked close beside the centred capsule. It wears the dock's own
 * material so the pair reads as one piece of chrome, and it appears wherever the dock appears.
 */
@Composable
fun FloatingSettingsBubble(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    RedSurface(
        modifier = modifier
            .size(BOTTOM_SETTINGS_SIZE)
            .testTag("bottom_settings_button"),
        shape = CircleShape,
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
                modifier = Modifier.size(RedIconSize.md)
            )
        }
    }
}

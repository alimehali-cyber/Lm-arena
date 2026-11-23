package com.alijafari.red.astronomy.ui.spatial

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.alijafari.red.astronomy.R
import com.alijafari.red.astronomy.ui.components.BOTTOM_NAV_HEIGHT
import com.alijafari.red.astronomy.ui.components.BOTTOM_SETTINGS_SIZE
import com.alijafari.red.astronomy.ui.components.SpaceSkyNavTabItemsRow
import com.alijafari.red.astronomy.ui.components.NavTabSpec
import com.alijafari.red.astronomy.ui.theme.RedShapes



/**
 * VisionOS Floating Spatial Glass Space Navigation Capsule.
 *
 * The design in Spatial chrome: a frosted G2 capsule with top-edge specular rim lighting, and
 * the selected tab shown as a jet-black deep-space capsule — a nebula photograph with blue
 * ribbons, violet clouds, star sprinkles and a live twinkle, travelling with the circular
 * settings button as one centred cluster.
 */
@Composable
fun SpatialFloatingNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = SpatialTheme.palette
    val dockShape = RedShapes.capsule
    val rimBrush = remember(palette) {
        Brush.verticalGradient(
            listOf(
                palette.rimTopHighlight,
                palette.rimSideBorder,
                palette.rimBottomReflection
            )
        )
    }

    Box(modifier.testTag("main_bottom_navigation")) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BOTTOM_NAV_HEIGHT)
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
            SpaceSkyNavTabItemsRow(
                modifier = Modifier.fillMaxSize(),
                selectedIndex = selectedTab,
                specs = listOf(
                    NavTabSpec(4, stringResource(R.string.nav_home), Icons.Filled.Home, Icons.Outlined.Home, "nav_item_home"),
                    NavTabSpec(3, stringResource(R.string.nav_arsky), Icons.Filled.Explore, Icons.Outlined.Explore, "nav_item_arsky"),
                    NavTabSpec(1, stringResource(R.string.nav_satellites), Icons.Filled.SatelliteAlt, Icons.Outlined.SatelliteAlt, "nav_item_satellites"),
                    NavTabSpec(0, stringResource(R.string.nav_lab), Icons.Filled.Science, Icons.Outlined.Science, "nav_item_lab"),
                ),
                selectedContentColor = Color.White,
                unselectedContentColor = palette.textSecondary,
                onSelected = onTabSelected,
            )
        }
    }
}

/**
 * Settings, as a true circle tucked close beside the centred capsule: same frosted glass
 * material and rim, so the dock and it read as one piece of chrome.
 */
@Composable
fun SpatialSettingsBubble(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = SpatialTheme.palette
    val shape = CircleShape
    val rim = remember(palette) {
        Brush.verticalGradient(
            listOf(palette.rimTopHighlight, palette.rimSideBorder, palette.rimBottomReflection)
        )
    }
    Box(
        modifier = modifier
            .size(BOTTOM_SETTINGS_SIZE)
            .testTag("bottom_settings_button")
            .shadow(elevation = 20.dp, shape = shape, ambientColor = palette.ambientDropShadow,
                spotColor = palette.ambientDropShadow)
            .clip(shape)
            .background(palette.glassBackingVeil)
            .background(palette.glassElevatedFill)
            .border(BorderStroke(1.dp, rim), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Settings,
            contentDescription = stringResource(R.string.settings),
            tint = palette.textSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

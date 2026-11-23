package com.alijafari.red.astronomy.ui.aether

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.alijafari.red.astronomy.R
import com.alijafari.red.astronomy.ui.components.BOTTOM_NAV_HEIGHT
import com.alijafari.red.astronomy.ui.components.BOTTOM_SETTINGS_SIZE
import com.alijafari.red.astronomy.ui.components.LuxuryNavTabItemsRow
import com.alijafari.red.astronomy.ui.components.NavTabSpec
import com.alijafari.red.astronomy.ui.theme.RedShapes



/**
 * Floating Aether Telemetry Burgundy Capsule Dock: the selected tab wears a luxurious burgundy
 * (#660033) silk-gradient capsule with a woven-grain texture; the frameless optical dock
 * (`FrostedGlassFill` `#14FFFFFF`) carries a razor-thin `1.dp` directional specular rim border and
 * a crimson glow shadow. Haptic feedback on every tap.
 */
@Composable
fun AetherFloatingNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = AetherTheme.palette
    val haptics = LocalHapticFeedback.current
    val shape = RedShapes.capsule

    Box(modifier.testTag("floating_bottom_bar")) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BOTTOM_NAV_HEIGHT)
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
        ) {
            LuxuryNavTabItemsRow(
                modifier = Modifier.fillMaxSize(),
                selectedIndex = selectedTab,
                specs = listOf(
                    NavTabSpec(4, stringResource(R.string.nav_home), Icons.Filled.Home, Icons.Outlined.Home, "nav_item_home"),
                    NavTabSpec(3, stringResource(R.string.nav_arsky), Icons.Filled.Explore, Icons.Outlined.Explore, "nav_item_arsky"),
                    NavTabSpec(1, stringResource(R.string.nav_satellites), Icons.Filled.SatelliteAlt, Icons.Outlined.SatelliteAlt, "nav_item_satellites"),
                    NavTabSpec(0, stringResource(R.string.nav_lab), Icons.Filled.Science, Icons.Outlined.Science, "nav_item_lab"),
                ),
                selectedContentColor = Color.White,
                unselectedContentColor = palette.mutedTelemetry,
                onSelected = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTabSelected(it)
                },
            )
        }
    }
}

/**
 * Settings, as a true circle tucked close beside the centred capsule: same frosted capsule
 * material and crimson rim, so the dock and it read as one piece of chrome.
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
            .size(BOTTOM_SETTINGS_SIZE)
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

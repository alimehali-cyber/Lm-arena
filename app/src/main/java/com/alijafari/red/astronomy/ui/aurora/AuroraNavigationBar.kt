package com.alijafari.red.astronomy.ui.aurora

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.align
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.alijafari.red.astronomy.R
import com.alijafari.red.astronomy.ui.theme.squircleShape

// ============================================================================
// THE PRISM DOCK
// ============================================================================
//
// Aurora's floating navigation: one raised lens carrying four destinations.
//
// - Shape relationships DR2 — the dock is a container lens (34 dp) and each
//   active thumb is an inner lens at [AuroraRadius.inner] of it, so the two
//   curves read as parallel rather than as a card dropped on a card.
// - Elevation hierarchy DR1/DR3 — the dock is the one surface on screen that is
//   genuinely above the content, so it is the only place a 10 dp shadow appears;
//   it leads with that shadow and keeps its rim quiet.
// - Color emphasis DR3 — one tab is emphasised. The selected thumb takes the
//   spectral underline; the other three are identical in weight.
// - User states DR3 — the selected tab keeps its raised surface through the
//   press. Only an unselected tab dips.

private data class AuroraDockTab(
    val index: Int,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

@Composable
fun AuroraFloatingNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val reduceMotion = rememberAuroraReducedMotion()
    val tabs = remember {
        listOf(
            AuroraDockTab(0, R.string.nav_lab, Icons.Filled.Science, Icons.Outlined.Science, "nav_tab_0"),
            AuroraDockTab(3, R.string.nav_arsky, Icons.Filled.Explore, Icons.Outlined.Explore, "nav_tab_3"),
            AuroraDockTab(1, R.string.nav_satellites, Icons.Filled.SatelliteAlt, Icons.Outlined.SatelliteAlt, "nav_tab_1"),
            AuroraDockTab(4, R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home, "nav_tab_4")
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = AuroraSpace.screenEdge, vertical = AuroraSpace.group)
            .testTag("floating_bottom_bar")
    ) {
        AuroraLiquidSurface(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp),
            shape = squircleShape(AuroraRadius.container),
            tier = AuroraSurfaceTier.RAISED
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    AuroraDockItem(
                        tab = tab,
                        selected = selectedTab == tab.index,
                        reduceMotion = reduceMotion,
                        onClick = { onTabSelected(tab.index) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AuroraDockItem(
    tab: AuroraDockTab,
    selected: Boolean,
    reduceMotion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = AuroraTheme.palette
    val interactionSource = remember { MutableInteractionSource() }
    val label = stringResource(tab.labelRes)

    // The underline grows from the centre on selection, so the eye is told where
    // the emphasis moved rather than only that something changed (motion DR1).
    val underline by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (reduceMotion) 0 else AuroraMotion.LOCAL_MS,
            easing = AuroraMotion.settle
        ),
        label = "AuroraDockUnderline"
    )

    AuroraLiquidSurface(
        modifier = modifier
            .heightIn(min = AURORA_MIN_TARGET)
            .testTag(tab.tag),
        // Inner radius = outer minus the gap: the dock and its thumb stay
        // concentric however the theme retunes the container.
        shape = squircleShape(AuroraRadius.inner(AuroraRadius.container, 10.dp)),
        tier = if (selected) AuroraSurfaceTier.RAISED else AuroraSurfaceTier.CANVAS,
        selected = selected,
        onClick = onClick,
        interactionSource = interactionSource,
        contentAlignment = Alignment.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(horizontal = 2.dp)
            ) {
                Icon(
                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = label,
                    tint = if (selected) palette.accent else palette.textTertiary,
                    modifier = Modifier.size(21.dp)
                )
                Text(
                    text = label,
                    style = AuroraType.caption,
                    color = if (selected) palette.textPrimary else palette.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // The spectral underline: the only place the full spectrum is used as
            // a mark rather than a fill.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 5.dp)
                    .width(26.dp)
                    .height(3.dp),
                contentAlignment = Alignment.Center
            ) {
                AuroraSpectrumBar(progress = underline)
            }
        }
    }
}

/**
 * The spectral underline, painted rather than composed so it can grow from the
 * centre without a second layout pass.
 */
@Composable
internal fun AuroraSpectrumBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val palette = AuroraTheme.palette
    if (progress <= 0.01f) return
    Box(
        modifier = modifier
            .fillMaxWidth(progress)
            .height(3.dp)
            .auroraSpectrumFill(palette)
    )
}

private fun Modifier.auroraSpectrumFill(palette: AuroraPalette): Modifier =
    this.then(
        drawBehind {
            drawRect(
                brush = Brush.linearGradient(
                    colors = palette.spectrum,
                    start = Offset.Zero,
                    end = Offset(size.width, 0f)
                )
            )
        }
    )

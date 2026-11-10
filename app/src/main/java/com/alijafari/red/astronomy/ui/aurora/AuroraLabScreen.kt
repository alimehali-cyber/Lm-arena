package com.alijafari.red.astronomy.ui.aurora

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import com.alijafari.red.astronomy.domain.AppLanguage
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.MainViewModel
import com.alijafari.red.astronomy.ui.screens.LabFeatureType
import com.alijafari.red.astronomy.ui.screens.TimeDilationCalculatorScreen
import com.alijafari.red.astronomy.ui.theme.squircleShape

// ============================================================================
// AURORA LAB
// ============================================================================
//
// Three tools, one list. The content is the default Lab's; the composition is
// Aurora's, and it differs in two deliberate ways.
//
// First, there is no card around the screen title. Template-composition DR2 says
// the page context leads the hierarchy without competing with the primary task,
// and a panel whose only job is to repeat the word "Lab" above a list of Lab
// tools is the opposite of that. The title is ink on the field.
//
// Second, the list is spaced at the section tier (24 dp), not the local tier,
// because three self-contained tools are three separate concerns — spatial
// hierarchy DR2 — and the 6 dp inside each card is what binds a tool's own
// title, subtitle and description into one object.

@Composable
fun AuroraLabScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val isFa = uiState.language == AppLanguage.PERSIAN
    var selectedFeature by rememberSaveable { mutableStateOf<LabFeatureType?>(null) }

    DisposableEffect(Unit) {
        onDispose { selectedFeature = null }
    }

    when (selectedFeature) {
        LabFeatureType.TIME_DILATION -> TimeDilationCalculatorScreen(
            uiState = uiState,
            onBackToLab = { selectedFeature = null },
            modifier = modifier
        )
        LabFeatureType.GRAVITY_SANDBOX -> com.zig.gravity.ui.GravitySandboxRoot(
            onBack = { selectedFeature = null },
            modifier = modifier,
            startInPersian = isFa,
            startInDarkTheme = uiState.themeMode != com.alijafari.red.astronomy.domain.ThemeMode.LIGHT
        )
        LabFeatureType.ZIG_BLACKHOLE -> com.zig.antar.ui.AntarRoot(
            onBack = { selectedFeature = null },
            language = uiState.language,
            uiStyle = uiState.uiStyle,
            themeMode = uiState.themeMode,
            modifier = modifier
        )
        null -> AuroraLabList(
            isFa = isFa,
            onOpenFeature = { selectedFeature = it },
            modifier = modifier
        )
    }
}

@Composable
private fun AuroraLabList(
    isFa: Boolean,
    onOpenFeature: (LabFeatureType) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = AuroraTheme.palette

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("lab_screen"),
        contentPadding = PaddingValues(
            horizontal = AuroraSpace.screenEdge,
            vertical = AuroraSpace.section
        ),
        verticalArrangement = Arrangement.spacedBy(AuroraSpace.section)
    ) {
        item {
            Column {
                Text(
                    text = if (isFa) "آزمایشگاه اخترفیزیک" else "Astrophysics Lab",
                    style = AuroraType.display,
                    color = palette.textPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AuroraSpace.local)
                        .testTag("lab_title")
                )
                Spacer(modifier = Modifier.height(AuroraSpace.local))
                Text(
                    text = if (isFa) {
                        "شبیه‌سازها و ماشین‌حساب‌های اخترفیزیک"
                    } else {
                        "Simulators and calculators for the sky above you"
                    },
                    style = AuroraType.body,
                    color = palette.textTertiary,
                    modifier = Modifier.padding(horizontal = AuroraSpace.local)
                )
            }
        }

        items(LabFeatureType.entries) { feature ->
            AuroraLabFeatureCard(
                feature = feature,
                isFa = isFa,
                onClick = { onOpenFeature(feature) }
            )
        }

        // Clearance for the Prism Dock, which floats over the list.
        item { Spacer(modifier = Modifier.height(112.dp)) }
    }
}

@Composable
private fun AuroraLabFeatureCard(
    feature: LabFeatureType,
    isFa: Boolean,
    onClick: () -> Unit
) {
    val palette = AuroraTheme.palette
    val available = feature.isAvailable

    AuroraLiquidSurface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("lab_feature_card_${feature.name.lowercase()}"),
        shape = squircleShape(AuroraRadius.card),
        tier = AuroraSurfaceTier.CARD,
        enabled = available,
        onClick = if (available) onClick else null
    ) {
        Row(
            modifier = Modifier.padding(AuroraSpace.cardPadding),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(AuroraSpace.group)
        ) {
            // The tool's mark. A recessed well, not a flat disc: the icon is
            // something you look *into*.
            AuroraInset(modifier = Modifier.size(46.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = feature.icon,
                        contentDescription = null,
                        tint = if (available) palette.accent else palette.textDisabled,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AuroraSpace.local)
                ) {
                    Text(
                        text = if (isFa) feature.titleFa else feature.titleEn,
                        style = AuroraType.section,
                        color = if (available) palette.textPrimary else palette.textDisabled,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (!available) {
                        // Two cues for one state: the word and the muted ink.
                        AuroraStatusPill(
                            label = if (isFa) "به زودی" else "Soon",
                            status = AuroraStatus.NEUTRAL
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = if (isFa) feature.subtitleFa else feature.subtitleEn,
                    style = AuroraType.label,
                    color = if (available) palette.accent else palette.textDisabled
                )
                Spacer(modifier = Modifier.height(AuroraSpace.local))
                Text(
                    text = if (isFa) feature.descriptionFa else feature.descriptionEn,
                    style = AuroraType.body,
                    color = palette.textSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (available) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = palette.textTertiary,
                    modifier = Modifier
                        .size(18.dp)
                        .padding(top = 4.dp)
                )
            }
        }
    }
}

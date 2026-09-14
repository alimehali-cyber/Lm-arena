package com.zig.museum.feature.museum

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zig.museum.core.model.ObjectRegistry
import com.zig.museum.core.model.ObjectSpec
import com.zig.museum.feature.viewer.SpaceMuseumViewerScreen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable

/**
 * SpaceMuseumRoot — hosts grid + viewer navigation internally.
 * For M0, simple state: selectedObjectId null = grid, non-null = viewer.
 * This avoids needing Navigation Compose and matches real repo's selectedFeature pattern.
 */
@Composable
fun SpaceMuseumRoot(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    isFa: Boolean = false,
    isDark: Boolean = true
) {
    var selectedObjectId by remember { mutableStateOf<String?>(null) }
    var showCredits by remember { mutableStateOf(false) }
    var creditsObjectId by remember { mutableStateOf<String?>(null) }

    // Load all manifests from assets for credits
    val context = androidx.compose.ui.platform.LocalContext.current
    val allManifests = remember {
        ManifestLoader.loadFromAssets(context)
    }

    // Set immersive for entire museum to hide bottom nav bar
    androidx.compose.runtime.DisposableEffect(Unit) {
        try {
            val clazz = Class.forName("com.zig.gravity.ui.ImmersiveScreenState")
            val instance = clazz.getField("INSTANCE").get(null)
            val activeField = instance.javaClass.getDeclaredField("active")
            activeField.isAccessible = true
            activeField.set(instance, true)
        } catch (e: Exception) {
        }
        onDispose {
            try {
                val clazz = Class.forName("com.zig.gravity.ui.ImmersiveScreenState")
                val instance = clazz.getField("INSTANCE").get(null)
                val activeField = instance.javaClass.getDeclaredField("active")
                activeField.isAccessible = true
                activeField.set(instance, false)
            } catch (e: Exception) {
            }
        }
    }

    if (showCredits) {
        val manifestsForCredits = if (creditsObjectId != null) {
            ManifestLoader.loadForObject(context, creditsObjectId!!).ifEmpty { allManifests.filter { it.objectId == creditsObjectId } }
        } else {
            allManifests
        }
        com.zig.museum.core.credits.CreditsScreen(
            manifests = manifestsForCredits.ifEmpty { allManifests },
            onBack = {
                showCredits = false
                creditsObjectId = null
            },
            isFa = isFa,
            modifier = modifier
        )
    } else if (selectedObjectId != null) {
        SpaceMuseumViewerScreen(
            objectId = selectedObjectId!!,
            onBack = { selectedObjectId = null },
            onCredits = { objectId ->
                creditsObjectId = objectId
                showCredits = true
            },
            modifier = modifier,
            isFa = isFa
        )
    } else {
        SpaceMuseumGridScreen(
            onObjectClick = { id -> selectedObjectId = id },
            onBack = onBack,
            onCredits = { objectId ->
                creditsObjectId = objectId
                showCredits = true
            },
            modifier = modifier,
            isFa = isFa
        )
    }
}

@Composable
fun SpaceMuseumGridScreen(
    onObjectClick: (String) -> Unit,
    onBack: () -> Unit,
    onCredits: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    isFa: Boolean = false
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("space_museum_grid_screen")
            .padding(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            )
    ) {
        // Top bar — aware of status bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("museum_back_button")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = if (isFa) "موزه فضا" else "Space Museum",
                style = MaterialTheme.typography.titleLarge
            )
            // Info for all packs
            if (onCredits != null) {
                IconButton(onClick = { onCredits("") }, modifier = Modifier.testTag("museum_all_credits_button")) {
                    Icon(Icons.Filled.RocketLaunch, contentDescription = "All Credits")
                }
            } else {
                Spacer(Modifier.size(48.dp))
            }
        }

        // Header card — reuse RedElevatedCard pattern if available, else simple Surface
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.RocketLaunch,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = if (isFa) "سیزده جهان، یک به یک" else "Thirteen Worlds, One at a Time",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (isFa) "آفلاین، با کیفیت بالا، یک شی در هر لحظه" else "Offline, high-fidelity, one object at a time",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Grid of 13 tiles — aware of bottom nav bar and system insets
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 148.dp),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 16.dp + 80.dp // extra for bottom nav + system nav
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
        ) {
            items(ObjectRegistry.all) { spec ->
                MuseumTile(
                    spec = spec,
                    isFa = isFa,
                    onClick = { onObjectClick(spec.id) },
                    onCreditsClick = { onCredits?.invoke(spec.id) }
                )
            }
            // Extra bottom spacing so last row not obstructed by bottom nav bar
            item {
                Spacer(modifier = Modifier.height(120.dp))
            }
        }
    }
}

@Composable
private fun MuseumTile(
    spec: ObjectSpec,
    isFa: Boolean,
    onClick: () -> Unit,
    onCreditsClick: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("space_museum_tile_${spec.id}"),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = spec.displayNameEn.take(1),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (onCreditsClick != null) {
                    androidx.compose.material3.IconButton(
                        onClick = onCreditsClick,
                        modifier = Modifier.size(28.dp).testTag("museum_tile_info_${spec.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = "Info",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Text(
                text = if (isFa) spec.displayNameFa else spec.displayNameEn,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (isFa) spec.dataCeilingTextFa else spec.dataCeilingTextEn,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

package com.zig.museum.core.credits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zig.museum.core.model.ObjectRegistry

/**
 * Sources & Credits surface — shows real manifests from assets/manifests/
 * Per §5.9: one row per shipped asset, generated from installed packs' provenance manifests
 */
@Composable
fun CreditsScreen(
    manifests: List<PackManifest> = emptyList(),
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    isFa: Boolean = false
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(WindowInsets.statusBars.asPaddingValues())
    ) {
        // Top bar with status bar padding
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = if (isFa) "منابع و اعتبار" else "Sources & Credits",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.padding(24.dp))
        }

        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 16.dp + 100.dp // extra for bottom nav + system nav
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (isFa) "دارایی‌ها: ${manifests.sumOf { it.assets.size }}" else "Assets: ${manifests.sumOf { it.assets.size }}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (isFa) "بسته‌ها: ${manifests.size}" else "Packs: ${manifests.size}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = if (isFa) "اشیاء: ${ObjectRegistry.all.size}" else "Objects: ${ObjectRegistry.all.size}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (manifests.isNotEmpty()) {
                            Text(
                                text = if (isFa) "همه منابع آفلاین و قابل ردیابی هستند" else "All sources offline and traceable",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(manifests) { manifest ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "${manifest.objectId} — ${manifest.packId} v${manifest.version}",
                            style = MaterialTheme.typography.titleSmall
                        )
                        manifest.dataCeiling?.let { ceiling ->
                            Text(
                                text = if (isFa) ceiling.textFa else ceiling.textEn,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        manifest.assets.forEach { asset ->
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                Text(
                                    text = "${asset.product} — ${asset.publisher}",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = asset.url,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = asset.credit,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${asset.type} ${asset.resolution} ${asset.format}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (manifests.isEmpty()) {
                item {
                    Text(
                        text = if (isFa) "هنوز بسته‌ای نصب نشده — این صفحه در M5 کامل می‌شود" else "No packs installed yet — this screen completes in M5",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Bottom spacing for nav bar
            item {
                Spacer(modifier = Modifier.height(120.dp))
            }
        }
    }
}

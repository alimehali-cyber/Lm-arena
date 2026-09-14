package com.zig.museum.core.credits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zig.museum.core.model.ObjectRegistry

/**
 * Sources & Credits surface — M0 empty implementation.
 * Per §5.9: one row per shipped asset, generated from installed packs' provenance manifests,
 * never hand-written. This is a product feature and also what lets owner trace every asset.
 *
 * For M0, shows placeholder with ObjectRegistry count and empty manifest list.
 * Real implementation in M5 will read manifests/ and display rows.
 */
@Composable
fun CreditsScreen(
    manifests: List<PackManifest> = emptyList(),
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    isFa: Boolean = false
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Top bar placeholder
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(text = if (isFa) "منابع و اعتبار" else "Sources & Credits")
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(text = if (isFa) "دارایی‌ها: ${manifests.sumOf { it.assets.size }}" else "Assets: ${manifests.sumOf { it.assets.size }}")
                Text(text = if (isFa) "بسته‌ها: ${manifests.size}" else "Packs: ${manifests.size}")
                Text(text = if (isFa) "اشیاء: ${ObjectRegistry.all.size}" else "Objects: ${ObjectRegistry.all.size}")
            }
            items(manifests.flatMap { it.assets }) { asset ->
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Text(text = "${asset.product} — ${asset.publisher}")
                    Text(text = asset.url)
                    Text(text = asset.credit)
                }
            }
            if (manifests.isEmpty()) {
                item {
                    Text(text = if (isFa) "هنوز بسته‌ای نصب نشده — این صفحه در M5 کامل می‌شود" else "No packs installed yet — this screen completes in M5")
                }
            }
        }
    }
}

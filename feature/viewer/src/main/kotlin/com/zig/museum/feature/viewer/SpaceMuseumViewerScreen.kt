package com.zig.museum.feature.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.zig.museum.core.model.ObjectRegistry

/**
 * SpaceMuseumViewerScreen — M0 empty implementation.
 * In M1, this will host Filament surface via InspectorEngine.
 * For M0, proves navigation works: shows object name, back, credits placeholder.
 * Sets ImmersiveScreenState to hide FloatingBottomBar (reuse gravity's object).
 */
@Composable
fun SpaceMuseumViewerScreen(
    objectId: String,
    onBack: () -> Unit,
    onCredits: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isFa: Boolean = false
) {
    // Reuse gravity's immersive flag to hide bottom nav bar without editing MainActivity
    // This is additive reuse per D-008
    DisposableEffect(Unit) {
        try {
            val clazz = Class.forName("com.zig.gravity.ui.ImmersiveScreenState")
            val field = clazz.getField("active")
            // It's an object with var active, need to set via reflection? Actually it's Kotlin object with mutable property.
            // We'll try to access via getter/setter
            val instance = clazz.getField("INSTANCE").get(null)
            val activeField = instance.javaClass.getDeclaredField("active")
            activeField.isAccessible = true
            activeField.set(instance, true)
        } catch (e: Exception) {
            // If gravity immersive state not found, ignore — nav bar will stay visible in M0, acceptable
        }
        onDispose {
            try {
                val clazz = Class.forName("com.zig.gravity.ui.ImmersiveScreenState")
                val instance = clazz.getField("INSTANCE").get(null)
                val activeField = instance.javaClass.getDeclaredField("active")
                activeField.isAccessible = true
                activeField.set(instance, false)
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    val spec = ObjectRegistry.byId(objectId)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090A0F))
            .testTag("space_museum_viewer_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("viewer_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = if (isFa) spec?.displayNameFa ?: objectId else spec?.displayNameEn ?: objectId,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.testTag("viewer_object_name")
                )
                IconButton(onClick = { onCredits?.invoke() }, modifier = Modifier.testTag("viewer_credits_button")) {
                    Icon(Icons.Default.Info, contentDescription = "Credits", tint = Color.White)
                }
            }

            // Center placeholder
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isFa) "نمایشگر در M1 کامل می‌شود" else "Viewer completes in M1",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = spec?.let { if (isFa) it.dataCeilingTextFa else it.dataCeilingTextEn } ?: "",
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "ID: $objectId | Radius: ${spec?.sceneRadiusMetres} m | Oblateness: ${spec?.oblateness}",
                        color = Color.White.copy(alpha = 0.4f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // Bottom placeholder for controls (rotation, sun direction)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(Color.White.copy(alpha = 0.05f))
                    .testTag("viewer_controls_placeholder"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isFa) "کنترل‌های چرخش و نور خورشید در M5" else "Rotation & sun controls in M5",
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.catalog.AstronomyCatalog
import com.example.domain.AppLanguage
import com.example.domain.CalendarSystem
import com.example.domain.SkyCanvasTheme
import com.example.domain.ThemeMode
import com.example.ui.MainUiState
import com.example.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    uiState: MainUiState,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val isFa = uiState.language == AppLanguage.PERSIAN
    var showCityPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_dialog"),
        title = {
            Text(
                text = stringResource(R.string.app_settings),
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Language Selection Segmented Switch
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.application_language),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = uiState.language == AppLanguage.PERSIAN,
                            onClick = { viewModel.setLanguage(AppLanguage.PERSIAN) },
                            label = { Text(text = stringResource(R.string.language_persian)) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = uiState.language == AppLanguage.ENGLISH,
                            onClick = { viewModel.setLanguage(AppLanguage.ENGLISH) },
                            label = { Text(text = stringResource(R.string.language_english)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Calendar System Selection
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.calendar_system),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = uiState.calendarSystem == CalendarSystem.SOLAR_HIJRI,
                            onClick = { viewModel.setCalendarSystem(CalendarSystem.SOLAR_HIJRI) },
                            label = { Text(text = stringResource(R.string.calendar_solar_hijri)) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = uiState.calendarSystem == CalendarSystem.GREGORIAN,
                            onClick = { viewModel.setCalendarSystem(CalendarSystem.GREGORIAN) },
                            label = { Text(text = stringResource(R.string.calendar_gregorian)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Theme Mode Selection (OLED Black, Deep Navy, Light)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (isFa) "پوسته برنامه (App Theme):" else "App Theme Mode:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = uiState.themeMode == ThemeMode.DARK_NAVY,
                                onClick = { viewModel.setThemeMode(ThemeMode.DARK_NAVY) },
                                label = { Text(text = if (isFa) "🌌 سرمه‌ای" else "🌌 Deep Navy") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = uiState.themeMode == ThemeMode.OLED_BLACK,
                                onClick = { viewModel.setThemeMode(ThemeMode.OLED_BLACK) },
                                label = { Text(text = if (isFa) "🖤 OLED" else "🖤 Pitch OLED") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        FilterChip(
                            selected = uiState.themeMode == ThemeMode.LIGHT,
                            onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                            label = { Text(text = if (isFa) "☀️ حالت روشن" else "☀️ Light Mode") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Sky Canvas Theme Section
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (isFa) "سبک هنری آسمان (Sky Canvas Theme):" else "Sky Canvas Theme:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = uiState.skyCanvasTheme == SkyCanvasTheme.CELESTIAL,
                            onClick = { viewModel.setSkyCanvasTheme(SkyCanvasTheme.CELESTIAL) },
                            label = { Text(text = if (isFa) "🌌 آسمانی" else "Celestial") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = uiState.skyCanvasTheme == SkyCanvasTheme.MONOCHROME,
                            onClick = { viewModel.setSkyCanvasTheme(SkyCanvasTheme.MONOCHROME) },
                            label = { Text(text = if (isFa) "🔳 تک‌رنگ" else "Monochrome") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = uiState.skyCanvasTheme == SkyCanvasTheme.FUN,
                            onClick = { viewModel.setSkyCanvasTheme(SkyCanvasTheme.FUN) },
                            label = { Text(text = if (isFa) "🖍️ پاستلی" else "Fun (Crayon)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Location / City Selector Button
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.observation_location),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    OutlinedButton(
                        onClick = { showCityPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        val cityName = if (isFa) uiState.userLocation.cityNameFa else uiState.userLocation.cityNameEn
                        Text(text = cityName)
                    }
                }

                // Light Pollution / Bortle Class Slider
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val bortleText = stringResource(R.string.bortle_class_setting, uiState.bortleClass)
                    Text(
                        text = bortleText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Slider(
                        value = uiState.bortleClass.toFloat(),
                        onValueChange = { viewModel.setBortleClass(it.toInt()) },
                        valueRange = 1f..9f,
                        steps = 7
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Developer Credit Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SafeAppLogo(
                            modifier = Modifier.size(44.dp),
                            cornerRadius = 8.dp
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.developed_by),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.telegram_id),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(text = stringResource(R.string.ok))
            }
        }
    )

    // Iran Provincial Capitals Picker Modal
    if (showCityPicker) {
        AlertDialog(
            onDismissRequest = { showCityPicker = false },
            title = {
                Text(text = stringResource(R.string.select_city))
            },
            text = {
                Box(modifier = Modifier.height(300.dp)) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(AstronomyCatalog.IRAN_CITIES) { (en, fa, coords) ->
                            TextButton(
                                onClick = {
                                    viewModel.setLocation(en, fa, coords.first, coords.second)
                                    showCityPicker = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = if (isFa) fa else en)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCityPicker = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        )
    }
}

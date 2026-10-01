package com.zig.chal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.ui.theme.RedCornerRadius
import com.alijafari.red.astronomy.ui.theme.RedSpacing
import com.alijafari.red.astronomy.ui.theme.RedTheme
import com.zig.chal.config.ChalFeatureToggles
import com.zig.chal.config.ChalParameterConfig
import com.zig.chal.config.ChalPresetName
import com.zig.chal.config.ChalRayTracingQuality
import com.zig.chal.config.ChalSimulationConfig
import com.zig.chal.config.ChalSimulationParams
import com.zig.chal.config.ChalXapkScenario
import kotlin.math.exp
import kotlin.math.ln
import java.util.Locale

/**
 * Scientific real-time interface.
 *
 * Provides the XAPK physical scenarios, numeric controls, feature mask, and quality choices when
 * the native backend is active. Chal-only benchmark/cinematic controls remain available only in the
 * GLES compatibility mode.
 */
@Composable
fun ChalControlPanel(
    params: ChalSimulationParams,
    isPersian: Boolean,
    isCinematic: Boolean,
    isXapkRenderer: Boolean,
    onScenarioSelected: (ChalXapkScenario) -> Unit,
    onParamsChange: (ChalSimulationParams) -> Unit,
    onPresetSelected: (ChalPresetName) -> Unit,
    onQualitySelected: (ChalRayTracingQuality) -> Unit,
    onStartCinematic: (ChalCinematicTool) -> Unit,
    onStartBenchmark: () -> Unit,
    onCancelBenchmark: () -> Unit,
    isBenchmarkRunning: Boolean,
    benchmarkPreset: String?,
    benchmarkProgress: Double,
    benchmarkResults: List<com.zig.chal.render.ChalBenchmark.BenchmarkResult>,
    benchmarkRecommendation: com.zig.chal.config.ChalPresetName?,
    modifier: Modifier = Modifier
) {
    var tab by remember { mutableStateOf(ChalPanelTab.PHYSICS) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("chal_control_panel")
            .clip(RoundedCornerShape(RedCornerRadius.xxl))
            .background(Color.Black.copy(alpha = 0.72f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(RedCornerRadius.xxl))
            .padding(RedSpacing.md),
        verticalArrangement = Arrangement.spacedBy(RedSpacing.md)
    ) {
        // ---- Tab strip -------------------------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RedSpacing.sm)
        ) {
            ChalPanelTab.entries.forEach { entry ->
                ChalTabButton(
                    label = entry.label(isPersian),
                    selected = tab == entry,
                    onClick = { tab = entry },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (tab == ChalPanelTab.PHYSICS) 268.dp else 232.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(RedSpacing.md)
        ) {
            when (tab) {
                ChalPanelTab.PHYSICS -> {
                    ChalPanelSection(if (isPersian) "سناریوها" else "Scenarios") {
                        val selectedScenario = ChalXapkScenario.matching(params)
                        ChalXapkScenario.entries.chunked(2).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(RedSpacing.sm)) {
                                row.forEach { scenario ->
                                    ChalChoiceButton(
                                        label = if (isPersian) scenario.persianLabel else scenario.label,
                                        selected = selectedScenario == scenario,
                                        onClick = { onScenarioSelected(scenario) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    ChalPanelSection(if (isPersian) "پارامترهای سیاه‌چاله" else "Black Hole Parameters") {
                        ChalSlider(
                            config = ChalSimulationConfig.MASS,
                            value = params.mass,
                            isPersian = isPersian,
                            onChange = { onParamsChange(params.copy(mass = it)) }
                        )
                        if (!isXapkRenderer) {
                            ChalSlider(
                                config = ChalSimulationConfig.ZOOM,
                                value = params.zoom,
                                isPersian = isPersian,
                                onChange = { onParamsChange(params.copy(zoom = it)) }
                            )
                        }
                        ChalSlider(
                            config = ChalSimulationConfig.SPIN,
                            value = params.spin,
                            isPersian = isPersian,
                            onChange = { onParamsChange(params.copy(spin = it)) }
                        )
                        ChalSlider(
                            config = ChalSimulationConfig.LENSING,
                            value = params.lensing,
                            isPersian = isPersian,
                            onChange = { onParamsChange(params.copy(lensing = it)) }
                        )
                        ChalSlider(
                            config = ChalSimulationConfig.FRAME_DRAGGING,
                            value = params.frameDraggingStrength,
                            isPersian = isPersian,
                            onChange = { onParamsChange(params.copy(frameDraggingStrength = it)) }
                        )
                    }

                    ChalPanelSection(if (isPersian) "دینامیک قرص برافزایشی" else "Accretion Dynamics") {
                        ChalSlider(
                            config = ChalSimulationConfig.AUTO_SPIN,
                            value = params.autoSpin,
                            isPersian = isPersian,
                            onChange = { onParamsChange(params.copy(autoSpin = it)) }
                        )
                        ChalSlider(
                            config = ChalSimulationConfig.DISK_SIZE,
                            value = params.diskSize,
                            isPersian = isPersian,
                            onChange = { onParamsChange(params.copy(diskSize = it)) }
                        )
                        ChalSlider(
                            config = ChalSimulationConfig.DISK_SCALE_HEIGHT,
                            value = params.diskScaleHeight,
                            isPersian = isPersian,
                            onChange = { onParamsChange(params.copy(diskScaleHeight = it)) }
                        )
                        ChalSlider(
                            config = ChalSimulationConfig.DISK_TEMP,
                            value = params.diskTemp,
                            isPersian = isPersian,
                            logarithmic = true,
                            enabled = params.features.accretionDisk,
                            onChange = { onParamsChange(params.copy(diskTemp = it)) }
                        )
                        ChalSlider(
                            config = ChalSimulationConfig.DISK_DENSITY,
                            value = params.diskDensity,
                            isPersian = isPersian,
                            enabled = params.features.accretionDisk,
                            onChange = { onParamsChange(params.copy(diskDensity = it)) }
                        )
                    }

                    if (isXapkRenderer) {
                        ChalPanelSection(if (isPersian) "درخشش" else "Bloom") {
                            ChalSlider(
                                config = ChalSimulationConfig.BLOOM_THRESHOLD,
                                value = params.bloomThreshold,
                                isPersian = isPersian,
                                onChange = { onParamsChange(params.copy(bloomThreshold = it)) }
                            )
                            ChalSlider(
                                config = ChalSimulationConfig.BLOOM_INTENSITY,
                                value = params.bloomIntensity,
                                isPersian = isPersian,
                                onChange = { onParamsChange(params.copy(bloomIntensity = it)) }
                            )
                        }
                    }
                }

                ChalPanelTab.SYSTEM -> {
                    if (!isXapkRenderer) ChalPanelSection(if (isPersian) "پیش‌تنظیم‌های کارایی" else "Performance Presets") {
                        Row(horizontalArrangement = Arrangement.spacedBy(RedSpacing.sm)) {
                            ChalPresetName.entries.filter { it != ChalPresetName.CUSTOM }.forEach { preset ->
                                ChalChoiceButton(
                                    label = preset.label,
                                    selected = params.performancePreset == preset,
                                    onClick = { onPresetSelected(preset) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        ChalSlider(
                            config = ChalSimulationConfig.RENDER_SCALE,
                            value = params.renderScale,
                            isPersian = isPersian,
                            onChange = { onParamsChange(params.copy(renderScale = it)) }
                        )
                    }

                    ChalPanelSection(if (isPersian) "دقت ردیابی پرتو" else "Ray Tracing Fidelity") {
                        Row(horizontalArrangement = Arrangement.spacedBy(RedSpacing.sm)) {
                            ChalRayTracingQuality.entries
                                .filter { !isXapkRenderer || it != ChalRayTracingQuality.OFF }
                                .forEach { quality ->
                                    ChalChoiceButton(
                                        label = quality.label,
                                        selected = params.features.rayTracingQuality == quality,
                                        onClick = { onQualitySelected(quality) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                        }
                    }

                    if (!isXapkRenderer) {
                        ChalPanelSection(if (isPersian) "اعتبارسنجی سامانه" else "System Validation") {
                            ChalChoiceButton(
                                label = if (isBenchmarkRunning) {
                                    if (isPersian) "توقف سنجش" else "Abort Benchmark"
                                } else {
                                    if (isPersian) "اجرای سنجش کارایی" else "Run Performance Suite"
                                },
                                selected = isBenchmarkRunning,
                                onClick = { if (isBenchmarkRunning) onCancelBenchmark() else onStartBenchmark() },
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (isBenchmarkRunning) {
                                Text(
                                    text = "${benchmarkPreset ?: ""} ${Math.round(benchmarkProgress * 100.0)}%",
                                    color = Color.White.copy(alpha = 0.70f),
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(RedCornerRadius.full))
                                        .background(Color.White.copy(alpha = 0.08f))
                                ) {
                                    Spacer(
                                        modifier = Modifier
                                            .fillMaxWidth(benchmarkProgress.coerceIn(0.0, 1.0).toFloat())
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(RedCornerRadius.full))
                                            .background(RedTheme.colors.accentRed.copy(alpha = 0.8f))
                                    )
                                }
                            }
                            benchmarkResults.forEach { result ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = result.presetName.label.uppercase(Locale.US),
                                        color = Color.White.copy(alpha = 0.65f),
                                        fontSize = 8.sp
                                    )
                                    Text(
                                        text = String.format(Locale.US, "%.1f fps", result.averageFPS),
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            benchmarkRecommendation?.let { preset ->
                                Text(
                                    text = if (isPersian) "پیشنهاد: ${preset.label}" else "Recommended: ${preset.label}",
                                    color = RedTheme.colors.accentRed.copy(alpha = 0.9f),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        ChalPanelSection(if (isPersian) "دقت نمایش" else "Display Precision") {
                            Text(
                                text = if (isPersian) {
                                    "دقت طیفی و حد چگالی پراکندگی حجمی."
                                } else {
                                    "Spectral precision and volumetric scattering density limit."
                                },
                                color = Color.White.copy(alpha = 0.30f),
                                fontSize = 8.sp
                            )
                        }
                    }
                }

                ChalPanelTab.MODULES -> {
                    ChalPanelSection(if (isPersian) "ماژول‌های فیزیک" else "Physics Modules") {
                        ChalFeatureToggle.entries
                            .filter {
                                !isXapkRenderer || it !in setOf(
                                    ChalFeatureToggle.VOLUMETRIC_BLOOM,
                                    ChalFeatureToggle.KERR_SHADOW_GUIDE,
                                    ChalFeatureToggle.SPACETIME_VISUALIZATION
                                )
                            }
                            .forEach { toggle ->
                                ChalToggleRow(
                                    label = toggle.label(isPersian),
                                    checked = toggle.read(params.features),
                                    onToggle = { onParamsChange(params.copy(features = toggle.write(params.features, it))) }
                                )
                            }
                    }

                    if (!isXapkRenderer) ChalPanelSection(if (isPersian) "ابزارهای سینمایی" else "Cinematic Tools") {
                        Row(horizontalArrangement = Arrangement.spacedBy(RedSpacing.sm)) {
                            ChalChoiceButton(
                                label = if (isPersian) "تور مداری" else "Orbit Tour",
                                selected = false,
                                onClick = { onStartCinematic(ChalCinematicTool.ORBIT) },
                                modifier = Modifier.weight(1f)
                            )
                            ChalChoiceButton(
                                label = if (isPersian) "سقوط آزاد" else "Infall Dive",
                                selected = false,
                                onClick = { onStartCinematic(ChalCinematicTool.DIVE) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Text(
                            text = if (isCinematic) {
                                if (isPersian) "دوربین در حالت کارگردانی است — برای توقف «ریست» را بزنید."
                                else "Director mode is running — press Reset to abort."
                            } else {
                                if (isPersian) "دوربین خودکار برای گشت‌وگذار سینمایی در اطراف افق رویداد."
                                else "Automatic camera choreography around the event horizon."
                            },
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Chal panel tabs retain the pre-existing Chal UI order and captions ("Physics", "Modules", "System").
 */
enum class ChalPanelTab {
    PHYSICS, MODULES, SYSTEM;

    fun label(isPersian: Boolean): String = when (this) {
        PHYSICS -> if (isPersian) "فیزیک" else "Physics"
        MODULES -> if (isPersian) "ماژول‌ها" else "Modules"
        SYSTEM -> if (isPersian) "سامانه" else "System"
    }
}

/** Cinematic director modes exposed by the panel. */
enum class ChalCinematicTool { ORBIT, DIVE }

/**
 * The ten physics modules of `FeatureToggles`, in the reference panel's order.
 *
 * `spacetimeVisualization` is carried for schema and control-surface parity: in the reference it
 * drives a separate analytics canvas rather than the ray-marcher, so toggling it here records the
 * operator's intent without changing the geodesic pass.
 */
enum class ChalFeatureToggle {
    GRAVITATIONAL_LENSING,
    ACCRETION_DISK,
    DOPPLER_BEAMING,
    PHOTON_SPHERE,
    BACKGROUND_STARS,
    VOLUMETRIC_BLOOM,
    RELATIVISTIC_JETS,
    GRAVITATIONAL_REDSHIFT,
    KERR_SHADOW_GUIDE,
    SPACETIME_VISUALIZATION;

    fun label(isPersian: Boolean): String = when (this) {
        GRAVITATIONAL_LENSING -> if (isPersian) "همگرایی گرانشی" else "Gravitational Lensing"
        ACCRETION_DISK -> if (isPersian) "قرص برافزایشی" else "Accretion Disk"
        DOPPLER_BEAMING -> if (isPersian) "درخشش دوپلری" else "Doppler Beaming"
        PHOTON_SPHERE -> if (isPersian) "کره فوتونی" else "Photon Sphere"
        BACKGROUND_STARS -> if (isPersian) "ستارگان پس‌زمینه" else "Background Stars"
        VOLUMETRIC_BLOOM -> if (isPersian) "درخشش حجمی" else "Volumetric Bloom"
        RELATIVISTIC_JETS -> if (isPersian) "فواره‌های نسبیتی" else "Relativistic Jets"
        GRAVITATIONAL_REDSHIFT -> if (isPersian) "انتقال به سرخ گرانشی" else "Gravitational Redshift"
        KERR_SHADOW_GUIDE -> if (isPersian) "راهنمای سایه کر" else "Kerr Shadow Guide"
        SPACETIME_VISUALIZATION -> if (isPersian) "تجسم فضازمان" else "Spacetime Visualization"
    }

    fun read(features: ChalFeatureToggles): Boolean = when (this) {
        GRAVITATIONAL_LENSING -> features.gravitationalLensing
        ACCRETION_DISK -> features.accretionDisk
        DOPPLER_BEAMING -> features.dopplerBeaming
        PHOTON_SPHERE -> features.photonSphereGlow
        BACKGROUND_STARS -> features.backgroundStars
        VOLUMETRIC_BLOOM -> features.bloom
        RELATIVISTIC_JETS -> features.relativisticJets
        GRAVITATIONAL_REDSHIFT -> features.gravitationalRedshift
        KERR_SHADOW_GUIDE -> features.kerrShadow
        SPACETIME_VISUALIZATION -> features.spacetimeVisualization
    }

    fun write(features: ChalFeatureToggles, value: Boolean): ChalFeatureToggles = when (this) {
        GRAVITATIONAL_LENSING -> features.copy(gravitationalLensing = value)
        ACCRETION_DISK -> features.copy(accretionDisk = value)
        DOPPLER_BEAMING -> features.copy(dopplerBeaming = value)
        PHOTON_SPHERE -> features.copy(photonSphereGlow = value)
        BACKGROUND_STARS -> features.copy(backgroundStars = value)
        VOLUMETRIC_BLOOM -> features.copy(bloom = value)
        RELATIVISTIC_JETS -> features.copy(relativisticJets = value)
        GRAVITATIONAL_REDSHIFT -> features.copy(gravitationalRedshift = value)
        KERR_SHADOW_GUIDE -> features.copy(kerrShadow = value)
        SPACETIME_VISUALIZATION -> features.copy(spacetimeVisualization = value)
    }
}

// ---------------------------------------------------------------------------------------------
// Building blocks (`ControlSlider`, `SectionHeader`, preset/quality buttons, toggle rows)
// ---------------------------------------------------------------------------------------------

@Composable
private fun ChalPanelSection(label: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RedCornerRadius.lg))
            .background(Color.White.copy(alpha = 0.03f))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(RedCornerRadius.lg))
            .padding(RedSpacing.md),
        verticalArrangement = Arrangement.spacedBy(RedSpacing.sm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(RedSpacing.sm)) {
            Spacer(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(RedTheme.colors.statusSuccess)
            )
            Text(
                text = label.uppercase(Locale.US),
                color = Color.White,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black
            )
        }
        content()
    }
}

/**
 * Logarithmic-aware slider matching `ControlSlider` from the reference (log-scale for the wide
 * disk-temperature range, value readout with unit and fixed decimals).
 */
@Composable
private fun ChalSlider(
    config: ChalParameterConfig,
    value: Double,
    isPersian: Boolean,
    onChange: (Double) -> Unit,
    enabled: Boolean = true,
    logarithmic: Boolean = false
) {
    val safeMin = if (logarithmic) maxOf(config.min, 1e-4) else config.min
    val safeValue = if (logarithmic) maxOf(value, 1e-4) else value

    val minPos = if (logarithmic) ln(safeMin) else config.min
    val maxPos = if (logarithmic) ln(config.max) else config.max
    val valuePos = if (logarithmic) ln(safeValue) else safeValue

    val label = if (isPersian) persianLabel(config.label) else config.label

    Column(modifier = Modifier.fillMaxWidth().testTag("chal_slider_${config.label.replace(" ", "_").lowercase(Locale.US)}")) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label.uppercase(Locale.US),
                color = Color.White.copy(alpha = if (enabled) 0.70f else 0.30f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Black
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = String.format(Locale.US, "%.${config.decimals}f", value),
                    color = Color.White.copy(alpha = if (enabled) 1.0f else 0.35f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = " ${config.unit}",
                    color = Color.White.copy(alpha = 0.40f),
                    fontSize = 8.sp
                )
            }
        }

        Slider(
            value = valuePos.toFloat(),
            onValueChange = { raw ->
                val converted = if (logarithmic) exp(raw.toDouble()) else raw.toDouble()
                onChange(converted.coerceIn(config.min, config.max))
            },
            valueRange = minPos.toFloat()..maxPos.toFloat(),
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White.copy(alpha = 0.90f),
                inactiveTrackColor = Color.White.copy(alpha = 0.10f)
            ),
            modifier = Modifier.fillMaxWidth().height(20.dp)
        )
    }
}

@Composable
private fun ChalChoiceButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(RedCornerRadius.sm))
            .background(if (selected) RedTheme.colors.accentRed.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f))
            .border(
                1.dp,
                if (selected) RedTheme.colors.accentRed.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.10f),
                RoundedCornerShape(RedCornerRadius.sm)
            )
            .clickable(onClick = onClick)
            .padding(vertical = RedSpacing.sm, horizontal = RedSpacing.xs),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label.uppercase(Locale.US),
            color = if (selected) Color.White else Color.White.copy(alpha = 0.70f),
            fontSize = 8.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun ChalTabButton(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(RedCornerRadius.sm))
            .background(if (selected) Color.White.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = RedSpacing.sm),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label.uppercase(Locale.US),
            color = if (selected) Color.White else Color.White.copy(alpha = 0.55f),
            fontSize = 8.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun ChalToggleRow(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RedCornerRadius.sm))
            .background(Color.White.copy(alpha = 0.02f))
            .clickable { onToggle(!checked) }
            .padding(horizontal = RedSpacing.sm, vertical = RedSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
        Box(
            modifier = Modifier
                .size(width = 34.dp, height = 18.dp)
                .clip(RoundedCornerShape(RedCornerRadius.full))
                .background(if (checked) RedTheme.colors.accentRed.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.12f))
                .padding(2.dp)
        ) {
            Spacer(
                modifier = Modifier
                    .size(14.dp)
                    .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

/** Persian labels for the sliders, mirroring the bilingual Lab card convention. */
private fun persianLabel(label: String): String = when (label) {
    ChalSimulationConfig.MASS.label -> "جرم سیاه‌چاله"
    ChalSimulationConfig.ZOOM.label -> "فاصله ناظر"
    ChalSimulationConfig.SPIN.label -> "پارامتر اسپین"
    ChalSimulationConfig.LENSING.label -> "شدت همگرایی"
    ChalSimulationConfig.AUTO_SPIN.label -> "حرکت خودکار دوربین"
    ChalSimulationConfig.DISK_SIZE.label -> "شعاع بیشینه قرص"
    ChalSimulationConfig.DISK_SCALE_HEIGHT.label -> "ضخامت قرص"
    ChalSimulationConfig.DISK_TEMP.label -> "دمای قرص"
    ChalSimulationConfig.DISK_DENSITY.label -> "چگالی نوری"
    ChalSimulationConfig.FRAME_DRAGGING.label -> "کشش چارچوب"
    ChalSimulationConfig.BLOOM_THRESHOLD.label -> "آستانهٔ درخشش"
    ChalSimulationConfig.BLOOM_INTENSITY.label -> "شدت درخشش"
    ChalSimulationConfig.RENDER_SCALE.label -> "مقیاس رندر"
    else -> label
}

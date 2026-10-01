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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.ui.theme.RedCornerRadius
import com.alijafari.red.astronomy.ui.theme.RedControlHeight
import com.alijafari.red.astronomy.ui.theme.RedSpacing
import com.alijafari.red.astronomy.ui.theme.RedTheme
import com.zig.chal.config.ChalFeatureToggles
import com.zig.chal.config.ChalFormatting
import com.zig.chal.config.ChalParameterConfig
import com.zig.chal.config.ChalPerformanceConfig
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
 * Provides the XAPK physical scenarios, numeric controls, feature mask, and quality choices for both
 * renderer backends. Everything shown here changes something the active backend honours: controls
 * that a backend cannot implement (the benchmark suite and the cinematic director, which exist only
 * in the GLES fallback) are not rendered at all rather than left inert.
 *
 * @param onCommit invoked when an edit gesture finishes, so the session is persisted once per
 *   interaction instead of once per slider frame.
 * @param maxContentHeight height of the scrolling body; the host shrinks it in landscape so the
 *   panel never pushes the top chrome off screen.
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
    onCommit: () -> Unit,
    isBenchmarkRunning: Boolean,
    benchmarkPreset: String?,
    benchmarkProgress: Double,
    benchmarkResults: List<com.zig.chal.render.ChalBenchmark.BenchmarkResult>,
    benchmarkRecommendation: com.zig.chal.config.ChalPresetName?,
    maxContentHeight: Dp = 268.dp,
    modifier: Modifier = Modifier
) {
    var tab by remember { mutableStateOf(ChalPanelTab.PHYSICS) }
    // Controls that would fight the running measurement are disabled, not hidden, so the panel keeps
    // its shape while the benchmark owns the renderer.
    val editable = !isBenchmarkRunning

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
                .height(maxContentHeight)
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
                                        enabled = editable,
                                        onClick = {
                                            onScenarioSelected(scenario)
                                            onCommit()
                                        },
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
                            enabled = editable,
                            onCommit = onCommit,
                            onChange = { onParamsChange(params.copy(mass = it)) }
                        )
                        ChalSlider(
                            config = ChalSimulationConfig.SPIN,
                            value = params.spin,
                            isPersian = isPersian,
                            enabled = editable,
                            onCommit = onCommit,
                            onChange = { onParamsChange(params.copy(spin = it)) }
                        )
                        ChalSlider(
                            config = ChalSimulationConfig.LENSING,
                            value = params.lensing,
                            isPersian = isPersian,
                            enabled = editable,
                            onCommit = onCommit,
                            onChange = { onParamsChange(params.copy(lensing = it)) }
                        )
                        ChalSlider(
                            config = ChalSimulationConfig.FRAME_DRAGGING,
                            value = params.frameDraggingStrength,
                            isPersian = isPersian,
                            enabled = editable,
                            onCommit = onCommit,
                            onChange = { onParamsChange(params.copy(frameDraggingStrength = it)) }
                        )
                        if (!isXapkRenderer) {
                            ChalSlider(
                                config = ChalSimulationConfig.ZOOM,
                                value = params.zoom,
                                isPersian = isPersian,
                                enabled = editable,
                                onCommit = onCommit,
                                onChange = { onParamsChange(params.copy(zoom = it)) }
                            )
                        }
                    }

                    val diskEnabled = editable && params.features.accretionDisk
                    ChalPanelSection(if (isPersian) "دینامیک قرص برافزایشی" else "Accretion Dynamics") {
                        ChalSlider(
                            config = ChalSimulationConfig.AUTO_SPIN,
                            value = params.autoSpin,
                            isPersian = isPersian,
                            enabled = editable,
                            onCommit = onCommit,
                            onChange = { onParamsChange(params.copy(autoSpin = it)) }
                        )
                        ChalSlider(
                            config = ChalSimulationConfig.DISK_SIZE,
                            value = params.diskSize,
                            isPersian = isPersian,
                            enabled = diskEnabled,
                            onCommit = onCommit,
                            onChange = { onParamsChange(params.copy(diskSize = it)) }
                        )
                        ChalSlider(
                            config = ChalSimulationConfig.DISK_SCALE_HEIGHT,
                            value = params.diskScaleHeight,
                            isPersian = isPersian,
                            enabled = diskEnabled,
                            onCommit = onCommit,
                            onChange = { onParamsChange(params.copy(diskScaleHeight = it)) }
                        )
                        ChalSlider(
                            config = ChalSimulationConfig.DISK_TEMP,
                            value = params.diskTemp,
                            isPersian = isPersian,
                            logarithmic = true,
                            enabled = diskEnabled,
                            onCommit = onCommit,
                            onChange = { onParamsChange(params.copy(diskTemp = it)) }
                        )
                        ChalSlider(
                            config = ChalSimulationConfig.DISK_DENSITY,
                            value = params.diskDensity,
                            isPersian = isPersian,
                            enabled = diskEnabled,
                            onCommit = onCommit,
                            onChange = { onParamsChange(params.copy(diskDensity = it)) }
                        )
                        if (!params.features.accretionDisk) {
                            ChalPanelHint(if (isPersian) "برای فعال شدن این کنترل‌ها ماژول «قرص برافزایشی» را روشن کنید." else "Enable the Accretion Disk module to unlock these controls.")
                        }
                    }

                    ChalPanelSection(if (isPersian) "درخشش" else "Bloom") {
                        // The native backend has no bloom feature bit: its post stack always runs and
                        // only these two floats shape it, so the module gate does not apply there.
                        val bloomEnabled = editable && (isXapkRenderer || params.features.bloom)
                        ChalSlider(
                            config = ChalSimulationConfig.BLOOM_THRESHOLD,
                            value = params.bloomThreshold,
                            isPersian = isPersian,
                            enabled = bloomEnabled,
                            onCommit = onCommit,
                            onChange = { onParamsChange(params.copy(bloomThreshold = it)) }
                        )
                        ChalSlider(
                            config = ChalSimulationConfig.BLOOM_INTENSITY,
                            value = params.bloomIntensity,
                            isPersian = isPersian,
                            enabled = bloomEnabled,
                            onCommit = onCommit,
                            onChange = { onParamsChange(params.copy(bloomIntensity = it)) }
                        )
                        if (!isXapkRenderer && !params.features.bloom) {
                            ChalPanelHint(if (isPersian) "برای فعال شدن این کنترل‌ها ماژول «درخشش حجمی» را روشن کنید." else "Enable the Volumetric Bloom module to unlock these controls.")
                        }
                    }
                }

                ChalPanelTab.SYSTEM -> {
                    ChalPanelSection(if (isPersian) "پیش‌تنظیم‌های کارایی" else "Performance presets") {
                        Row(horizontalArrangement = Arrangement.spacedBy(RedSpacing.sm)) {
                            ChalPresetName.entries.filter { it != ChalPresetName.CUSTOM }.forEach { preset ->
                                ChalChoiceButton(
                                    label = preset.label(isPersian),
                                    selected = params.performancePreset == preset,
                                    enabled = editable,
                                    onClick = {
                                        onPresetSelected(preset)
                                        onCommit()
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        if (!isXapkRenderer) {
                            ChalSlider(
                                config = ChalSimulationConfig.RENDER_SCALE,
                                value = params.renderScale,
                                isPersian = isPersian,
                                enabled = editable,
                                onCommit = onCommit,
                                onChange = { onParamsChange(params.copy(renderScale = it)) }
                            )
                        } else {
                            ChalPanelHint(if (isPersian) "مقیاس رندر را موتور بومی خودش تنظیم می‌کند." else "Render scale is owned by the native engine.")
                        }
                    }

                    ChalPanelSection(if (isPersian) "دقت ردیابی پرتو" else "Ray tracing fidelity") {
                        val visibleTiers = ChalRayTracingQuality.entries
                            .filter { !isXapkRenderer || it != ChalRayTracingQuality.OFF }
                        Row(horizontalArrangement = Arrangement.spacedBy(RedSpacing.sm)) {
                            visibleTiers.forEach { quality ->
                                ChalChoiceButton(
                                    label = quality.label(isPersian),
                                    selected = params.features.rayTracingQuality == quality,
                                    enabled = editable,
                                    onClick = {
                                        onQualitySelected(quality)
                                        onCommit()
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Text(
                            text = ChalFormatting.rayBudget(
                                quality = params.features.rayTracingQuality,
                                isPersian = isPersian,
                                isMobile = ChalPerformanceConfig.Mobile.IS_MOBILE_HARDWARE
                            ),
                            color = Color.White.copy(alpha = 0.70f),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        val tierNote = ChalFormatting.duplicateTierNote(visibleTiers, isPersian)
                        if (tierNote != null) ChalPanelHint(tierNote)
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
                                    text = "${benchmarkPreset ?: ""} ${ChalFormatting.percent(benchmarkProgress, isPersian)}",
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
                                        text = result.presetName.label(isPersian).uppercase(Locale.US),
                                        color = Color.White.copy(alpha = 0.65f),
                                        fontSize = 8.sp
                                    )
                                    Text(
                                        text = "${ChalFormatting.fixed(result.averageFPS, 1, isPersian)} ${if (isPersian) "فریم/ثانیه" else "fps"}",
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            benchmarkRecommendation?.let { preset ->
                                Text(
                                    text = if (isPersian) {
                                        "پیشنهاد: ${preset.label(true)}"
                                    } else {
                                        "Recommended: ${preset.label(false)}"
                                    },
                                    color = RedTheme.colors.accentRed.copy(alpha = 0.9f),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }

                ChalPanelTab.MODULES -> {
                    ChalPanelSection(if (isPersian) "ماژول‌های فیزیک" else "Physics Modules") {
                        ChalFeatureToggle.entries
                            .filter { !isXapkRenderer || it !in NATIVE_UNSUPPORTED_MODULES }
                            .forEach { toggle ->
                                ChalToggleRow(
                                    label = toggle.label(isPersian),
                                    checked = toggle.read(params.features),
                                    enabled = editable,
                                    onToggle = {
                                        onParamsChange(params.copy(features = toggle.write(params.features, it)))
                                        onCommit()
                                    }
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
                                if (isPersian) "دوربین در حالت کارگردانی است — برای توقف «بازنشانی نما» را بزنید."
                                else "Director mode is running — press Reset view to abort."
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

/** Cinematic director modes exposed by the panel (GLES backend only). */
enum class ChalCinematicTool { ORBIT, DIVE }

/**
 * Modules the native parameter block has no input for.
 *
 * The 16-float/4-int contract carries no bloom *bit* (bloom is always on there, shaped by its
 * threshold and intensity floats), nor any Kerr-shadow-guide or spacetime-visualisation input, so
 * those switches are hidden on the native backend instead of being left to do nothing.
 */
private val NATIVE_UNSUPPORTED_MODULES = setOf(
    ChalFeatureToggle.VOLUMETRIC_BLOOM,
    ChalFeatureToggle.KERR_SHADOW_GUIDE,
    ChalFeatureToggle.SPACETIME_VISUALIZATION
)

/**
 * The ten physics modules of `FeatureToggles`, in the reference panel's order.
 *
 * The native backend does not branch on the Kerr-shadow guide or the spacetime visualisation, so
 * those two rows are hidden while it is active; every other module maps onto a native feature flag
 * or shader define and therefore changes the image.
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
// Building blocks (section header, sliders, choice/toggle rows)
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
        Text(
            text = label.uppercase(Locale.US),
            color = Color.White,
            fontSize = 8.sp,
            fontWeight = FontWeight.Black
        )
        content()
    }
}

/** Secondary explanatory line. Only ever used for text that states a real constraint. */
@Composable
private fun ChalPanelHint(text: String) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.45f),
        fontSize = 8.sp
    )
}

/**
 * Logarithmic-aware slider matching `ControlSlider` from the reference (log-scale for the wide
 * disk-temperature range, value readout with unit and fixed decimals).
 *
 * The readout is tappable so an exact value can be typed — a slider alone cannot reach a precise
 * number, and the reference's numeric fields allow it. The value leaves this control through
 * [ChalParameterConfig.snap], so the number on screen is the number the renderer receives.
 */
@Composable
private fun ChalSlider(
    config: ChalParameterConfig,
    value: Double,
    isPersian: Boolean,
    onChange: (Double) -> Unit,
    onCommit: () -> Unit,
    enabled: Boolean = true,
    logarithmic: Boolean = false
) {
    val safeMin = if (logarithmic) maxOf(config.min, 1e-4) else config.min
    val safeValue = if (logarithmic) maxOf(value, 1e-4) else value

    val minPos = if (logarithmic) ln(safeMin) else config.min
    val maxPos = if (logarithmic) ln(config.max) else config.max
    val valuePos = if (logarithmic) ln(safeValue) else safeValue

    val label = if (isPersian) persianLabel(config.label) else config.label
    var editing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chal_slider_${config.label.replace(" ", "_").lowercase(Locale.US)}")
    ) {
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
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.clickable(enabled = enabled) { editing = true }
            ) {
                Text(
                    text = ChalFormatting.digits(config.format(value), isPersian),
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
                onChange(config.snap(converted))
            },
            valueRange = minPos.toFloat()..maxPos.toFloat(),
            enabled = enabled,
            onValueChangeFinished = onCommit,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White.copy(alpha = 0.90f),
                inactiveTrackColor = Color.White.copy(alpha = 0.10f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("chal_slider_track_${config.label.replace(" ", "_").lowercase(Locale.US)}")
        )
    }

    if (editing) {
        ChalNumberEntryDialog(
            config = config,
            value = value,
            label = label,
            isPersian = isPersian,
            onDismiss = { editing = false },
            onConfirm = { typed ->
                onChange(config.snap(typed))
                editing = false
                onCommit()
            }
        )
    }
}

/** Exact numeric entry for a control, mirroring the reference's numeric fields. */
@Composable
private fun ChalNumberEntryDialog(
    config: ChalParameterConfig,
    value: Double,
    label: String,
    isPersian: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var text by remember { mutableStateOf(String.format(Locale.US, "%.${config.decimals}f", config.snap(value))) }
    val parsed = text.replace(',', '.').toDoubleOrNull()
    val valid = parsed != null && parsed.isFinite() && parsed in config.min..config.max

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(RedSpacing.sm)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true
                )
                Text(
                    text = "${ChalFormatting.fixed(config.min, config.decimals, isPersian)} – " +
                        "${ChalFormatting.fixed(config.max, config.decimals, isPersian)} ${config.unit}",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 9.sp
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let(onConfirm) },
                enabled = valid
            ) {
                Text(text = if (isPersian) "تأیید" else "Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = if (isPersian) "لغو" else "Cancel")
            }
        }
    )
}

@Composable
private fun ChalChoiceButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .heightIn(min = RedControlHeight.regular)
            .clip(RoundedCornerShape(RedCornerRadius.sm))
            .background(
                when {
                    !enabled -> Color.White.copy(alpha = 0.02f)
                    selected -> RedTheme.colors.accentRed.copy(alpha = 0.22f)
                    else -> Color.White.copy(alpha = 0.05f)
                }
            )
            .border(
                1.dp,
                when {
                    !enabled -> Color.White.copy(alpha = 0.06f)
                    selected -> RedTheme.colors.accentRed.copy(alpha = 0.55f)
                    else -> Color.White.copy(alpha = 0.10f)
                },
                RoundedCornerShape(RedCornerRadius.sm)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = RedSpacing.sm, horizontal = RedSpacing.xs),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label.uppercase(Locale.US),
            color = if (enabled) {
                if (selected) Color.White else Color.White.copy(alpha = 0.70f)
            } else {
                Color.White.copy(alpha = 0.35f)
            },
            fontSize = 8.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun ChalTabButton(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .heightIn(min = RedControlHeight.regular)
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
private fun ChalToggleRow(
    label: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = RedControlHeight.regular)
            .clip(RoundedCornerShape(RedCornerRadius.sm))
            .background(Color.White.copy(alpha = 0.02f))
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onToggle
            )
            .padding(horizontal = RedSpacing.sm, vertical = RedSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = if (enabled) 0.85f else 0.40f),
            fontSize = 11.sp
        )
        Box(
            modifier = Modifier
                .size(width = 34.dp, height = 18.dp)
                .clip(RoundedCornerShape(RedCornerRadius.full))
                .background(
                    if (checked) RedTheme.colors.accentRed.copy(alpha = if (enabled) 0.65f else 0.30f)
                    else Color.White.copy(alpha = 0.12f)
                )
                .padding(2.dp)
        ) {
            Spacer(
                modifier = Modifier
                    .size(14.dp)
                    .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = if (enabled) 1.0f else 0.5f))
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

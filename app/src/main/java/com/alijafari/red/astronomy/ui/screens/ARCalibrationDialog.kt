package com.alijafari.red.astronomy.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.astro_engine.*
import com.alijafari.red.astronomy.data.catalog.AstronomyCatalog
import com.alijafari.red.astronomy.domain.CelestialObject
import com.alijafari.red.astronomy.domain.ObjectType
import com.alijafari.red.astronomy.domain.UserLocation
import com.alijafari.red.astronomy.ui.theme.*

/**
 * Data structure for dynamic calibration reference celestial targets.
 * Computed entirely via astronomical engines without hardcoded positions.
 */
data class CalibrationReferenceTarget(
    val id: String,
    val nameEn: String,
    val nameFa: String,
    val type: ObjectType,
    val celestialObject: CelestialObject,
    val azimuthDeg: Double,
    val altitudeDeg: Double,
    val magnitude: Double,
    val isVisibleNow: Boolean
)

private enum class ReferenceCategoryTab {
    VISIBLE,
    MOON_PLANETS,
    BRIGHT_STARS,
    ALL
}

/**
 * RED AR Manual Pointing Calibration Overlay.
 *
 * Provides live, mathematically isolated correction for device optical misalignment:
 * - Yaw offset (ΔAzimuth: -25.0° to +25.0°)
 * - Pitch offset (ΔElevation: -25.0° to +25.0°)
 * - Roll offset (ΔRoll: -25.0° to +25.0°)
 *
 * Applied in SO(3) to the True-North rotation matrix:
 *   R_calibrated = R_true * R_calib(yaw, pitch, roll)
 *
 * When dragging any calibration slider, the large dialog window temporarily disappears,
 * leaving an ultra-compact floating HUD at the bottom so the camera feed and sky remain
 * completely unobstructed for accurate visual reticle alignment.
 */
@Composable
fun ARCalibrationDialog(
    isFa: Boolean,
    userLocation: UserLocation = UserLocation(),
    currentJd: Double = TimeEngine.getJulianDate(),
    currentAzimuth: Double = 0.0,
    currentAltitude: Double = 0.0,
    onDismiss: () -> Unit,
    onSelectReferenceTarget: (CelestialObject) -> Unit = {},
    onAdjustingStateChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    LaunchedEffect(context) {
        ARCalibrationManager.init(context)
    }

    val calibOffsets by ARCalibrationManager.calibrationFlow.collectAsState()
    val autoPromptEnabled by ARCalibrationManager.autoPromptEnabledFlow.collectAsState()

    var yaw by remember(calibOffsets) { mutableFloatStateOf(calibOffsets.yawOffsetDeg) }
    var pitch by remember(calibOffsets) { mutableFloatStateOf(calibOffsets.pitchOffsetDeg) }
    var roll by remember(calibOffsets) { mutableFloatStateOf(calibOffsets.rollOffsetDeg) }
    var selectedTargetName by remember(calibOffsets) { mutableStateOf(calibOffsets.referenceStarName) }

    var activeDragAxis by remember { mutableStateOf<String?>(null) }
    val isAdjusting = activeDragAxis != null

    LaunchedEffect(isAdjusting) {
        onAdjustingStateChanged(isAdjusting)
    }

    var activeCategoryTab by remember { mutableStateOf(ReferenceCategoryTab.VISIBLE) }

    // Dynamically calculate candidate calibration targets using real astronomy engines
    val candidateTargets = remember(currentJd, userLocation) {
        val lastDeg = TimeEngine.getLAST(currentJd, userLocation.longitude)
        val list = mutableListOf<CalibrationReferenceTarget>()

        // 1. Moon (Prominent visual reference)
        try {
            val mData = MoonEngine.calculateMoon(
                jd = currentJd,
                latitude = userLocation.latitude,
                longitude = userLocation.longitude,
                elevationM = userLocation.elevationMeters
            )
            val moonObj = AstronomyCatalog.MOON
            list.add(
                CalibrationReferenceTarget(
                    id = "moon",
                    nameEn = "Moon",
                    nameFa = "ماه",
                    type = ObjectType.MOON,
                    celestialObject = moonObj,
                    azimuthDeg = mData.azimuthDeg,
                    altitudeDeg = mData.altitudeDeg,
                    magnitude = -12.0,
                    isVisibleNow = mData.altitudeDeg >= 0.0
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Bright Planets (Venus, Jupiter, Saturn, Mars, Mercury)
        val planetIds = listOf(
            "planet_venus" to ("زهره (Venus)" to "Venus"),
            "planet_jupiter" to ("مشتری (Jupiter)" to "Jupiter"),
            "planet_saturn" to ("زحل (Saturn)" to "Saturn"),
            "planet_mars" to ("مریخ (Mars)" to "Mars"),
            "planet_mercury" to ("عطارد (Mercury)" to "Mercury")
        )
        for ((pId, names) in planetIds) {
            try {
                val pObj = AstronomyCatalog.getById(pId, currentJd)
                if (pObj != null) {
                    val horiz = CoordinateEngine.equatorialToHorizontal(
                        CoordinateEngine.Equatorial(pObj.raDeg, pObj.decDeg),
                        lastDeg,
                        userLocation.latitude,
                        userLocation.elevationMeters
                    )
                    list.add(
                        CalibrationReferenceTarget(
                            id = pId,
                            nameEn = names.second,
                            nameFa = names.first,
                            type = ObjectType.PLANET,
                            celestialObject = pObj,
                            azimuthDeg = horiz.azimuthDeg,
                            altitudeDeg = horiz.altitudeDeg,
                            magnitude = pObj.magnitude,
                            isVisibleNow = horiz.altitudeDeg >= 0.0
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 3. Sun
        try {
            val sHoriz = SunEngine.getSunAltAz(currentJd, userLocation.latitude, userLocation.longitude)
            val sunObj = AstronomyCatalog.SUN
            list.add(
                CalibrationReferenceTarget(
                    id = "sun",
                    nameEn = "Sun (Sol)",
                    nameFa = "خورشید",
                    type = ObjectType.SUN,
                    celestialObject = sunObj,
                    azimuthDeg = sHoriz.azimuthDeg,
                    altitudeDeg = sHoriz.altitudeDeg,
                    magnitude = -26.7,
                    isVisibleNow = sHoriz.altitudeDeg >= 0.0
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 4. Bright Prominent Stars
        val brightStarIds = listOf(
            "star_cma_sirius" to ("شباهنگ (Sirius)" to "Sirius (α CMa)"),
            "star_car_canopus" to ("سهیل (Canopus)" to "Canopus (α Car)"),
            "star_boo_arcturus" to ("نگهبان شمال (Arcturus)" to "Arcturus (α Boo)"),
            "star_lyr_vega" to ("نسر واقع (Vega)" to "Vega (α Lyr)"),
            "star_aur_capella" to ("بزبان (Capella)" to "Capella (α Aur)"),
            "star_ori_rigel" to ("پای شکارچی (Rigel)" to "Rigel (β Ori)"),
            "star_cmi_procyon" to ("شعرای شامی (Procyon)" to "Procyon (α CMi)"),
            "star_ori_betelgeuse" to ("ابط‌الجوزا (Betelgeuse)" to "Betelgeuse (α Ori)"),
            "star_aql_altair" to ("نسر طایر (Altair)" to "Altair (α Aql)"),
            "star_tau_aldebaran" to ("دبران (Aldebaran)" to "Aldebaran (α Tau)"),
            "star_sco_antares" to ("قلب‌العقرب (Antares)" to "Antares (α Sco)"),
            "star_vir_spica" to ("بی‌سلاح (Spica)" to "Spica (α Vir)"),
            "star_gem_pollux" to ("راس التوام (Pollux)" to "Pollux (β Gem)"),
            "star_cyg_deneb" to ("دم قو (Deneb)" to "Deneb (α Cyg)"),
            "star_leo_regulus" to ("قلب‌الاسد (Regulus)" to "Regulus (α Leo)"),
            "star_umi_polaris" to ("ستاره قطبی (Polaris)" to "Polaris (α UMi)")
        )
        for ((sId, names) in brightStarIds) {
            try {
                val sObj = AstronomyCatalog.getById(sId, currentJd)
                if (sObj != null) {
                    val horiz = CoordinateEngine.equatorialToHorizontal(
                        CoordinateEngine.Equatorial(sObj.raDeg, sObj.decDeg),
                        lastDeg,
                        userLocation.latitude,
                        userLocation.elevationMeters
                    )
                    list.add(
                        CalibrationReferenceTarget(
                            id = sId,
                            nameEn = names.second,
                            nameFa = names.first,
                            type = ObjectType.STAR,
                            celestialObject = sObj,
                            azimuthDeg = horiz.azimuthDeg,
                            altitudeDeg = horiz.altitudeDeg,
                            magnitude = sObj.magnitude,
                            isVisibleNow = horiz.altitudeDeg >= 0.0
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        list
    }

    // Filtered targets according to selected category tab
    val displayedTargets = remember(candidateTargets, activeCategoryTab) {
        when (activeCategoryTab) {
            ReferenceCategoryTab.VISIBLE -> candidateTargets.filter { it.isVisibleNow }
            ReferenceCategoryTab.MOON_PLANETS -> candidateTargets.filter { it.type == ObjectType.MOON || it.type == ObjectType.PLANET || it.type == ObjectType.SUN }
            ReferenceCategoryTab.BRIGHT_STARS -> candidateTargets.filter { it.type == ObjectType.STAR }
            ReferenceCategoryTab.ALL -> candidateTargets
        }
    }

    // Identify currently selected target data
    val selectedTargetData = remember(candidateTargets, selectedTargetName) {
        candidateTargets.firstOrNull {
            it.nameEn == selectedTargetName || it.nameFa == selectedTargetName || it.id == selectedTargetName
        }
    }

    // Apply live adjustments immediately to orientation pipeline
    fun updateLive(newYaw: Float, newPitch: Float, newRoll: Float) {
        yaw = newYaw
        pitch = newPitch
        roll = newRoll
        ARCalibrationManager.updateOffsets(newYaw, newPitch, newRoll, selectedTargetName)
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // --- VIEW MODE 1: Full Calibration Dialog (Visible when not actively dragging a slider) ---
        AnimatedVisibility(
            visible = !isAdjusting,
            enter = fadeIn(tween(100)) + scaleIn(tween(100), initialScale = 0.96f),
            exit = fadeOut(tween(80)) + scaleOut(tween(80), targetScale = 0.96f)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .testTag("ar_calibration_dialog"),
                shape = RoundedCornerShape(24.dp),
                color = BackgroundCard.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, CardBorder),
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Header: Title and Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = AccentPrimary.copy(alpha = 0.15f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = AccentPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = if (isFa) "کالیبراسیون دستی AR" else "Manual AR Calibration",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isFa) "همترازی جهت دوربین با اجرام شاخص" else "Align optical pointing with bright reference object",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("calib_dismiss_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // 2. Alignment & Calibration Status Banner
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (calibOffsets.isCalibrated) StatusGood.copy(alpha = 0.12f) else Color(0x2238BDF8),
                        border = BorderStroke(1.dp, if (calibOffsets.isCalibrated) StatusGood.copy(alpha = 0.4f) else Color(0x4438BDF8))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (calibOffsets.isCalibrated) Icons.Default.CheckCircle else Icons.Default.Info,
                                    contentDescription = null,
                                    tint = if (calibOffsets.isCalibrated) StatusGood else Color(0xFF38BDF8),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (calibOffsets.isCalibrated) {
                                        if (isFa) "کالیبراسیون فعال (دارای آفست)" else "Calibrated (Active Offsets)"
                                    } else {
                                        if (isFa) "پیش‌فرض سنسور (بدون آفست)" else "Default (Zero Offsets)"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (calibOffsets.isCalibrated) StatusGood else Color(0xFF38BDF8)
                                )
                            }

                            if (selectedTargetName.isNotEmpty()) {
                                Text(
                                    text = "★ $selectedTargetName",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AccentPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // 3. Reference Celestial Object Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isFa) "انتخاب جرم شاخص جهت همترازی:" else "Select Calibration Reference Target:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        // Category Filter Tabs
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                FilterChip(
                                    selected = activeCategoryTab == ReferenceCategoryTab.VISIBLE,
                                    onClick = { activeCategoryTab = ReferenceCategoryTab.VISIBLE },
                                    label = { Text(if (isFa) "قابل رویت (بالای افق)" else "Visible Now", fontSize = 11.sp) },
                                    leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(12.dp)) }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = activeCategoryTab == ReferenceCategoryTab.MOON_PLANETS,
                                    onClick = { activeCategoryTab = ReferenceCategoryTab.MOON_PLANETS },
                                    label = { Text(if (isFa) "ماه و سیارات" else "Moon & Planets", fontSize = 11.sp) }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = activeCategoryTab == ReferenceCategoryTab.BRIGHT_STARS,
                                    onClick = { activeCategoryTab = ReferenceCategoryTab.BRIGHT_STARS },
                                    label = { Text(if (isFa) "ستارگان درخشان" else "Bright Stars", fontSize = 11.sp) }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = activeCategoryTab == ReferenceCategoryTab.ALL,
                                    onClick = { activeCategoryTab = ReferenceCategoryTab.ALL },
                                    label = { Text(if (isFa) "همه اجرام" else "All Objects", fontSize = 11.sp) }
                                )
                            }
                        }

                        // Target Items Chips
                        if (displayedTargets.isEmpty()) {
                            Text(
                                text = if (isFa) "هیچ جرمی در این دسته‌بندی یافت نشد." else "No targets in this category.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(displayedTargets) { target ->
                                    val label = if (isFa) target.nameFa else target.nameEn
                                    val isSelected = selectedTargetName == (if (isFa) target.nameFa else target.nameEn)
                                    val altFmt = String.format("%+.0f°", target.altitudeDeg)

                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedTargetName = if (isSelected) "" else (if (isFa) target.nameFa else target.nameEn)
                                            onSelectReferenceTarget(target.celestialObject)
                                        },
                                        label = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                                Text(
                                                    text = if (isFa) TimeEngine.formatPersianNumbers(altFmt) else altFmt,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (target.isVisibleNow) Color(0xFF4ADE80) else Color(0xFFF87171),
                                                    fontSize = 9.sp
                                                )
                                            }
                                        },
                                        leadingIcon = {
                                            val iconText = when (target.type) {
                                                ObjectType.MOON -> "🌕"
                                                ObjectType.SUN -> "☀️"
                                                ObjectType.PLANET -> "🪐"
                                                else -> "⭐"
                                            }
                                            Text(iconText, fontSize = 12.sp)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 4. Selected Target Real-Time Ephemeris & Horizon Graceful Handling
                    if (selectedTargetData != null) {
                        val azFmt = String.format("%.1f°", selectedTargetData.azimuthDeg)
                        val altFmt = String.format("%+.1f°", selectedTargetData.altitudeDeg)
                        val magFmt = String.format("%+.1f", selectedTargetData.magnitude)

                        if (selectedTargetData.isVisibleNow) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0x1E4ADE80),
                                border = BorderStroke(1.dp, Color(0x444ADE80)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.MyLocation, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(16.dp))
                                        Text(
                                            text = if (isFa) "موقعیت واقعی: سمت $azFmt | ارتفاع $altFmt" else "Real Target: Az $azFmt | Alt $altFmt",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF4ADE80)
                                        )
                                    }
                                    Text(
                                        text = if (isFa) "قدر: $magFmt" else "Mag: $magFmt",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        } else {
                            // Quality-of-Life: Graceful warning when reference object is below horizon
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0x22EF4444),
                                border = BorderStroke(1.dp, Color(0x55EF4444)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                                    Column {
                                        Text(
                                            text = if (isFa) "⚠️ جرم انتخابی در زیر خط افق قرار دارد (ارتفاع: $altFmt)"
                                            else "⚠️ Target is below the horizon (Alt: $altFmt)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFEF4444)
                                        )
                                        Text(
                                            text = if (isFa) "این جرم از زمین پنهان است؛ لطفاً یک جرم قابل رویت در بالای افق انتخاب کنید."
                                            else "This object is occluded by Earth. Please select a visible target above the horizon.",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 10.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = CardBorder, thickness = 0.5.dp)

                    // 5. Three-Axis Manual Calibration Sliders (Yaw, Pitch, Roll)
                    // Slider 1: Horizontal / Yaw (Azimuth Offset)
                    CalibrationSliderRow(
                        title = if (isFa) "انحراف افقی (یاو / سمت)" else "Horizontal Offset (Yaw / ΔAzimuth)",
                        subtitle = if (isFa) "چرخش چپ و راست دوربین در امتداد افق" else "Left / Right horizontal boresight alignment",
                        value = yaw,
                        range = -25f..25f,
                        isFa = isFa,
                        tag = "yaw",
                        onDragStateChange = { dragging ->
                            activeDragAxis = if (dragging) "yaw" else null
                        },
                        onValueChange = { updateLive(it, pitch, roll) },
                        onNudge = { delta -> updateLive((yaw + delta).coerceIn(-25f, 25f), pitch, roll) }
                    )

                    // Slider 2: Vertical / Pitch (Elevation Offset)
                    CalibrationSliderRow(
                        title = if (isFa) "انحراف عمودی (پیچ / ارتفاع)" else "Vertical Offset (Pitch / ΔElevation)",
                        subtitle = if (isFa) "چرخش بالا و پایین دوربین در راستای عمودی" else "Up / Down vertical elevation alignment",
                        value = pitch,
                        range = -25f..25f,
                        isFa = isFa,
                        tag = "pitch",
                        onDragStateChange = { dragging ->
                            activeDragAxis = if (dragging) "pitch" else null
                        },
                        onValueChange = { updateLive(yaw, it, roll) },
                        onNudge = { delta -> updateLive(yaw, (pitch + delta).coerceIn(-25f, 25f), roll) }
                    )

                    // Slider 3: Axial Tilt / Roll (Roll Offset)
                    CalibrationSliderRow(
                        title = if (isFa) "انحراف دورانی (رول / چرخش محوری)" else "Axial Tilt (Roll / ΔRoll)",
                        subtitle = if (isFa) "چرخش زاویه‌ای ساعت‌گرد و پادساعت‌گرد صفحه" else "Clockwise / Counter-clockwise axial tilt",
                        value = roll,
                        range = -25f..25f,
                        isFa = isFa,
                        tag = "roll",
                        onDragStateChange = { dragging ->
                            activeDragAxis = if (dragging) "roll" else null
                        },
                        onValueChange = { updateLive(yaw, pitch, it) },
                        onNudge = { delta -> updateLive(yaw, pitch, (roll + delta).coerceIn(-25f, 25f)) }
                    )

                    HorizontalDivider(color = CardBorder, thickness = 0.5.dp)

                    // 6. Auto-Prompt Sensor Calibration Toggle
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = RedTheme.colors.surfaceGrouped.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, RedTheme.colors.border.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Explore,
                                    contentDescription = null,
                                    tint = AccentPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = if (isFa) "اعلان خودکار کالیبراسیون سنسور" else "Auto-Prompt Sensor Calibration",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (isFa) "نمایش راهنمای شکل ۸ هنگام افت دقت قطب‌نما" else "Show figure-8 guide when compass accuracy drops",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Switch(
                                checked = autoPromptEnabled,
                                onCheckedChange = { enabled ->
                                    ARCalibrationManager.setAutoPromptEnabled(enabled, context)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = AccentPrimary,
                                    uncheckedThumbColor = TextSecondary,
                                    uncheckedTrackColor = Color.White.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.testTag("calib_auto_prompt_switch")
                            )
                        }
                    }

                    // 7. Action Buttons (Reset & Save)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                yaw = 0f
                                pitch = 0f
                                roll = 0f
                                selectedTargetName = ""
                                ARCalibrationManager.resetCalibration(context)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("calib_reset_button"),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isFa) "بازنشانی" else "Reset",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                ARCalibrationManager.saveCalibration(context, selectedTargetName)
                                onDismiss()
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("calib_save_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isFa) "ذخیره کالیبراسیون" else "Save Calibration",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // --- VIEW MODE 2: Minimal Floating HUD Bar (Temporarily replaces dialog while dragging slider) ---
        AnimatedVisibility(
            visible = isAdjusting,
            enter = fadeIn(tween(100)) + slideInVertically(tween(100), initialOffsetY = { it / 2 }),
            exit = fadeOut(tween(80)) + slideOutVertically(tween(80), targetOffsetY = { it / 2 }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 20.dp)
                    .testTag("ar_calibration_floating_hud"),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xDD0D1B2A),
                border = BorderStroke(1.5.dp, AccentPrimary),
                shadowElevation = 24.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // HUD Header: Axis Title & Live Value Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val activeTitle = when (activeDragAxis) {
                            "yaw" -> if (isFa) "⟷ انحراف افقی (یاو / سمت)" else "⟷ Horizontal (Yaw / ΔAzimuth)"
                            "pitch" -> if (isFa) "↕ انحراف عمودی (پیچ / ارتفاع)" else "↕ Vertical (Pitch / ΔElevation)"
                            "roll" -> if (isFa) "🔄 چرخش محوری (رول)" else "🔄 Axial Tilt (Roll / ΔRoll)"
                            else -> ""
                        }
                        val activeVal = when (activeDragAxis) {
                            "yaw" -> yaw
                            "pitch" -> pitch
                            "roll" -> roll
                            else -> 0f
                        }
                        val valStr = String.format("%+.1f°", activeVal)

                        Text(
                            text = activeTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AccentPrimary,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = if (isFa) TimeEngine.formatPersianNumbers(valStr) else valStr,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Active Live Slider
                    val currentValue = when (activeDragAxis) {
                        "yaw" -> yaw
                        "pitch" -> pitch
                        "roll" -> roll
                        else -> 0f
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = {
                                when (activeDragAxis) {
                                    "yaw" -> updateLive((yaw - 0.1f).coerceIn(-25f, 25f), pitch, roll)
                                    "pitch" -> updateLive(yaw, (pitch - 0.1f).coerceIn(-25f, 25f), roll)
                                    "roll" -> updateLive(yaw, pitch, (roll - 0.1f).coerceIn(-25f, 25f))
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "-0.1°", tint = Color.White)
                        }

                        RedSlider(
                            value = currentValue,
                            onValueChange = { newVal ->
                                when (activeDragAxis) {
                                    "yaw" -> updateLive(newVal, pitch, roll)
                                    "pitch" -> updateLive(yaw, newVal, roll)
                                    "roll" -> updateLive(yaw, pitch, newVal)
                                }
                            },
                            onValueChangeFinished = {
                                activeDragAxis = null
                            },
                            valueRange = -25f..25f,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = {
                                when (activeDragAxis) {
                                    "yaw" -> updateLive((yaw + 0.1f).coerceIn(-25f, 25f), pitch, roll)
                                    "pitch" -> updateLive(yaw, (pitch + 0.1f).coerceIn(-25f, 25f), roll)
                                    "roll" -> updateLive(yaw, pitch, (roll + 0.1f).coerceIn(-25f, 25f))
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "+0.1°", tint = Color.White)
                        }
                    }

                    // Guidance Footer Tip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isFa) "🎯 هدف‌گیر مرکزی را با جرم آسمانی واقعی هم‌تراز کنید" else "🎯 Align central reticle with the physical celestial object",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            color = Color(0xFF38BDF8)
                        )

                        Text(
                            text = if (isFa) "رها کردن: بازگشت پنجره" else "Release to restore window",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalibrationSliderRow(
    title: String,
    subtitle: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    isFa: Boolean,
    tag: String,
    onDragStateChange: (Boolean) -> Unit,
    onValueChange: (Float) -> Unit,
    onNudge: (Float) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDragged by interactionSource.collectIsDraggedAsState()

    LaunchedEffect(isPressed, isDragged) {
        if (isPressed || isDragged) {
            onDragStateChange(true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("calib_row_$tag"),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            // Offset Value Badge
            val valStr = String.format("%+.1f°", value)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AccentPrimary.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.4f))
            ) {
                Text(
                    text = if (isFa) TimeEngine.formatPersianNumbers(valStr) else valStr,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = AccentPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        // Slider and Fine-tune Steppers
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = { onNudge(-0.1f) },
                modifier = Modifier
                    .size(32.dp)
                    .testTag("calib_dec_$tag")
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "-0.1°",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            RedSlider(
                value = value,
                onValueChange = {
                    onDragStateChange(true)
                    onValueChange(it)
                },
                onValueChangeFinished = {
                    onDragStateChange(false)
                },
                interactionSource = interactionSource,
                valueRange = range,
                modifier = Modifier
                    .weight(1f)
                    .testTag("calib_slider_$tag")
            )

            IconButton(
                onClick = { onNudge(+0.1f) },
                modifier = Modifier
                    .size(32.dp)
                    .testTag("calib_inc_$tag")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "+0.1°",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

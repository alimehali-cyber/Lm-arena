package com.alijafari.red.astronomy.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.astro_engine.ARCalibrationManager
import com.alijafari.red.astronomy.astro_engine.ARCalibrationOffsets
import com.alijafari.red.astronomy.astro_engine.TimeEngine
import com.alijafari.red.astronomy.data.catalog.AstronomyCatalog
import com.alijafari.red.astronomy.domain.AppLanguage
import com.alijafari.red.astronomy.domain.CelestialObject
import com.alijafari.red.astronomy.domain.ObjectType
import com.alijafari.red.astronomy.ui.theme.*

/**
 * RED AR Manual Pointing Calibration Overlay Screen.
 *
 * Allows real-time interactive adjustment of three explicit physical offset parameters:
 * - Yaw offset (ΔAzimuth: -30.0° to +30.0°)
 * - Pitch offset (ΔElevation: -30.0° to +30.0°)
 * - Roll offset (ΔRoll: -30.0° to +30.0°)
 *
 * Applied directly as an isolated rotation matrix R_calib in the AR orientation pipeline:
 *   R_calibrated = R_true * R_calib(yaw, pitch, roll)
 */
@Composable
fun ARCalibrationDialog(
    isFa: Boolean,
    onDismiss: () -> Unit,
    onSelectReferenceStar: (CelestialObject) -> Unit = {}
) {
    val context = LocalContext.current
    LaunchedEffect(context) {
        ARCalibrationManager.init(context)
    }

    val calibOffsets by ARCalibrationManager.calibrationFlow.collectAsState()

    var yaw by remember(calibOffsets) { mutableFloatStateOf(calibOffsets.yawOffsetDeg) }
    var pitch by remember(calibOffsets) { mutableFloatStateOf(calibOffsets.pitchOffsetDeg) }
    var roll by remember(calibOffsets) { mutableFloatStateOf(calibOffsets.rollOffsetDeg) }
    var selectedStarName by remember(calibOffsets) { mutableStateOf(calibOffsets.referenceStarName) }

    // Popular prominent reference stars for calibration
    val referenceStars = remember {
        listOf(
            "star_cma_sirius" to ("شباهنگ (Sirius)" to "Sirius (α CMa)"),
            "star_ori_betelgeuse" to ("ابط‌الجوزا (Betelgeuse)" to "Betelgeuse (α Ori)"),
            "star_ori_rigel" to ("پای شکارچی (Rigel)" to "Rigel (β Ori)"),
            "star_lyr_vega" to ("نسر واقع (Vega)" to "Vega (α Lyr)"),
            "star_boo_arcturus" to ("نگهبان شمال (Arcturus)" to "Arcturus (α Boo)"),
            "star_aur_capella" to ("بزبان (Capella)" to "Capella (α Aur)"),
            "star_cyg_deneb" to ("دم قو (Deneb)" to "Deneb (α Cyg)"),
            "star_umi_polaris" to ("ستاره قطبی (Polaris)" to "Polaris (α UMi)")
        )
    }

    // Apply live adjustments immediately to orientation pipeline
    fun updateLive(newYaw: Float, newPitch: Float, newRoll: Float) {
        yaw = newYaw
        pitch = newPitch
        roll = newRoll
        ARCalibrationManager.updateOffsets(newYaw, newPitch, newRoll, selectedStarName)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Title and Close Button
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
                            text = if (isFa) "کالیبراسیون دستی AR" else "AR Manual Calibration",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isFa) "تنظیم دقیق خط دید با ستاره شاخص" else "Align device optical pointing with a bright star",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp).testTag("calib_dismiss_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Current Status Banner
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
                                if (isFa) "کالیبره شده برای دوربین پشت" else "Calibrated (Rear Camera)"
                            } else {
                                if (isFa) "پیش‌فرض سنسور (بدون آفست)" else "Default (Zero Offsets)"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (calibOffsets.isCalibrated) StatusGood else Color(0xFF38BDF8)
                        )
                    }

                    if (selectedStarName.isNotEmpty()) {
                        Text(
                            text = "★ $selectedStarName",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentPrimary
                        )
                    }
                }
            }

            // Reference Star Selection Row
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (isFa) "انتخاب ستاره شاخص جهت همترازی:" else "Select Bright Reference Star:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(referenceStars) { (starId, names) ->
                        val (faName, enName) = names
                        val label = if (isFa) faName else enName
                        val isSelected = selectedStarName == (if (isFa) faName else enName)

                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedStarName = if (isSelected) "" else (if (isFa) faName else enName)
                                val starObj = AstronomyCatalog.getById(starId)
                                if (starObj != null) {
                                    onSelectReferenceStar(starObj)
                                }
                            },
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (isSelected) AccentPrimary else Color(0xFFFDE047)
                                )
                            }
                        )
                    }
                }
            }

            HorizontalDivider(color = CardBorder, thickness = 0.5.dp)

            // Slider 1: Yaw (Azimuth Offset)
            CalibrationSliderRow(
                title = if (isFa) "انحراف افقی (یاو / سمت)" else "Yaw Offset (ΔAzimuth)",
                subtitle = if (isFa) "چرخش چپ / راست در امتداد افق" else "Left / Right horizontal correction",
                value = yaw,
                range = -25f..25f,
                step = 0.2f,
                isFa = isFa,
                tag = "yaw",
                onValueChange = { updateLive(it, pitch, roll) },
                onNudge = { delta -> updateLive((yaw + delta).coerceIn(-25f, 25f), pitch, roll) }
            )

            // Slider 2: Pitch (Elevation Offset)
            CalibrationSliderRow(
                title = if (isFa) "انحراف عمودی (پیچ / ارتفاع)" else "Pitch Offset (ΔElevation)",
                subtitle = if (isFa) "چرخش بالا / پایین در امتداد آسمان" else "Up / Down vertical correction",
                value = pitch,
                range = -25f..25f,
                step = 0.2f,
                isFa = isFa,
                tag = "pitch",
                onValueChange = { updateLive(yaw, it, roll) },
                onNudge = { delta -> updateLive(yaw, (pitch + delta).coerceIn(-25f, 25f), roll) }
            )

            // Slider 3: Roll (Axial Roll Offset)
            CalibrationSliderRow(
                title = if (isFa) "انحراف دورانی (رول / چرخش محوری)" else "Roll Offset (ΔRoll)",
                subtitle = if (isFa) "چرخش زاویه‌ای ساعت‌گرد / پادساعت‌گرد" else "Clockwise / Counter-clockwise rotation",
                value = roll,
                range = -25f..25f,
                step = 0.2f,
                isFa = isFa,
                tag = "roll",
                onValueChange = { updateLive(yaw, pitch, it) },
                onNudge = { delta -> updateLive(yaw, pitch, (roll + delta).coerceIn(-25f, 25f)) }
            )

            // Disclaimers / Technical transparency notes
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.White.copy(alpha = 0.04f)
            ) {
                Text(
                    text = if (isFa)
                        "توجه: این کالیبراسیون انحراف فیزیکی شاسی دستگاه را تصحیح می‌کند. در اثر نوسان و دریفت سنسورهای مغناطیسی، دقت زیر یک درجه تضمین نمی‌شود."
                    else
                        "Notice: This calibration compensates for device chassis misalignment. Due to environmental sensor drift, sub-degree pointing accuracy is not guaranteed.",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(10.dp),
                    textAlign = TextAlign.Start
                )
            }

            // Action Buttons (Reset & Save)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        yaw = 0f
                        pitch = 0f
                        roll = 0f
                        selectedStarName = ""
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
                        ARCalibrationManager.saveCalibration(context, selectedStarName)
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

@Composable
private fun CalibrationSliderRow(
    title: String,
    subtitle: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    step: Float,
    isFa: Boolean,
    tag: String,
    onValueChange: (Float) -> Unit,
    onNudge: (Float) -> Unit
) {
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
                onClick = { onNudge(-0.2f) },
                modifier = Modifier.size(32.dp).testTag("calib_dec_$tag")
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "-0.2°",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            RedSlider(
                value = value,
                onValueChange = onValueChange,
                valueRange = range,
                modifier = Modifier
                    .weight(1f)
                    .testTag("calib_slider_$tag")
            )

            IconButton(
                onClick = { onNudge(+0.2f) },
                modifier = Modifier.size(32.dp).testTag("calib_inc_$tag")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "+0.2°",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

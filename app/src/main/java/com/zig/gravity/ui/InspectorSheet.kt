package com.zig.gravity.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zig.gravity.physics.BodyType
import com.zig.gravity.physics.EngineConstants
import com.zig.gravity.sim.BodyCatalog
import com.zig.gravity.sim.SimulationViewModel
import com.zig.gravity.ui.theme.LocalGravityColors
import com.zig.gravity.util.SandboxFormat
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * §3.11 Inspector — a contextual bottom sheet, so the tabletop stays visible behind it.
 *
 * Every control here mutates real simulation state through the ViewModel, which recomputes
 * accelerations and invalidates the prediction on the way out (§3.5 synchronisation rule).
 * Nothing in this sheet changes a displayed value only.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectorSheet(vm: SimulationViewModel, onDismiss: () -> Unit) {
    val c = LocalGravityColors.current
    val fa = vm.persian
    val snap = vm.snapshot
    val slot = snap.slotOfId(vm.selectedId)
    if (slot < 0) {
        // The body was merged or removed while the sheet was open: close on the next frame rather
        // than writing state during composition.
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    val type = snap.typeOf(slot)
    val key = snap.catalogKey[slot]
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = c.chrome,
        contentColor = c.onSurface,
        modifier = Modifier.testTag("inspector_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ---- header ---------------------------------------------------------------------
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(c.bodyTone(BodyCatalog.colorOf(key, type)))
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = BodyCatalog.nameOf(key, type, fa),
                        color = c.onSurface,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = BodyCatalog.let { if (fa) it.typeNameFa(type) else it.typeNameEn(type) },
                        color = c.onSurfaceDim,
                        fontSize = 12.sp
                    )
                }
                IconAction(Icons.Filled.ContentCopy, if (fa) "تکثیر" else "Duplicate", "inspector_duplicate") {
                    vm.duplicateSelected()
                }
                IconAction(Icons.Filled.Delete, if (fa) "حذف" else "Remove", "inspector_remove") {
                    vm.removeSelected()
                    onDismiss()
                }
            }

            Divider()

            // ---- mass (log slider) ------------------------------------------------------------
            if (type.massEditable) {
                val range = BodyType.massRange(type, snap.mass[slot])
                val lo = log10(maxOf(range.start, 1.0e12))
                val hi = log10(maxOf(range.endInclusive, 1.0e13))
                val current = log10(maxOf(snap.mass[slot], 1.0e12)).coerceIn(lo, hi)
                LabelRow(
                    title = if (fa) "جرم" else "Mass",
                    value = SandboxFormat.mass(snap.mass[slot], fa),
                    hint = SandboxFormat.kilograms(snap.mass[slot], fa)
                )
                Slider(
                    value = current.toFloat(),
                    onValueChange = { v -> vm.setMass(vm.selectedId, 10.0.pow(v.toDouble())) },
                    valueRange = lo.toFloat()..hi.toFloat(),
                    colors = sliderColors(),
                    modifier = Modifier.testTag("inspector_mass_slider")
                )
            } else {
                LabelRow(
                    title = if (fa) "جرم" else "Mass",
                    value = if (fa) "بی‌جرم" else "Massless",
                    hint = if (fa) "گرانش را حس می‌کند اما خودش گرانش ندارد." else "Feels gravity, exerts none."
                )
            }

            // ---- visual size ------------------------------------------------------------------
            LabelRow(
                title = if (fa) "اندازه نمایشی" else "Visual size",
                value = SandboxFormat.fixed(snap.radiusDp[slot], 1, fa) + " dp",
                hint = (if (fa) "شعاع فیزیکی واقعی: " else "Real physical radius: ") +
                        SandboxFormat.distance(BodyCatalog.realRadiusOf(key, type, snap.mass[slot]), fa)
            )
            Slider(
                value = snap.radiusDp[slot].toFloat().coerceIn(type.minDp.toFloat(), type.maxDp.toFloat()),
                onValueChange = { v -> vm.setRadiusDp(vm.selectedId, v.toDouble()) },
                valueRange = type.minDp.toFloat()..type.maxDp.toFloat(),
                colors = sliderColors(),
                modifier = Modifier.testTag("inspector_size_slider")
            )

            // ---- velocity ------------------------------------------------------------------------
            val vxNow = snap.vx[slot]
            val vyNow = snap.vy[slot]
            val speedNow = sqrt(vxNow * vxNow + vyNow * vyNow)
            val guidance = vm.velocityGuidance(vm.selectedId)
            LabelRow(
                title = if (fa) "سرعت" else "Velocity",
                value = SandboxFormat.speed(speedNow, fa),
                hint = (if (fa) "سقف پیشنهادی: " else "Suggested cap: ") + SandboxFormat.speed(guidance, fa)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = speedNow.toFloat().coerceIn(0f, guidance.toFloat()),
                    onValueChange = { v -> vm.setSpeedMagnitude(vm.selectedId, v.toDouble()) },
                    valueRange = 0f..guidance.toFloat(),
                    colors = sliderColors(),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("inspector_speed_slider")
                )
                Spacer(Modifier.width(12.dp))
                DirectionDial(
                    angleRad = if (speedNow > 0.0) atan2(vyNow, vxNow) else 0.0,
                    onAngle = { a -> vm.setDirection(vm.selectedId, a) }
                )
            }

            // ---- orbit helper ----------------------------------------------------------------------
            TextAction(
                text = if (fa) "مدار دایره‌ای کن" else "Make orbit circular",
                tag = "inspector_orbit_helper"
            ) { vm.applyOrbitHelper(vm.selectedId) }

            TextAction(
                text = if (fa) "آماده پرتاب (قلاب‌سنگ)" else "Arm slingshot",
                tag = "inspector_slingshot",
                icon = Icons.AutoMirrored.Filled.Send
            ) {
                vm.armSlingshot(vm.selectedId)
                onDismiss()
            }

            // ---- type-specific honesty notes ---------------------------------------------------------
            when (type) {
                BodyType.BLACK_HOLE -> {
                    Divider()
                    val rs = EngineConstants.schwarzschildRadius(snap.mass[slot])
                    LabelRow(
                        title = if (fa) "افق رویداد واقعی" else "Real event horizon",
                        value = SandboxFormat.distance(rs, fa),
                        hint = if (fa)
                            "این عدد از r_s = 2GM/c² می‌آید. حلقه‌ای که می‌بینی یک قرارداد نمایشی است و همان مرز گرفته‌شدن در این شبیه‌سازی است؛ محاسبه‌های ما کاملاً نیوتنی است."
                        else
                            "From r_s = 2GM/c². The drawn ring is a display convention and is this sandbox's capture boundary; our maths is purely Newtonian."
                    )
                }
                BodyType.WORMHOLE_MOUTH -> {
                    Divider()
                    LabelRow(
                        title = if (fa) "کرم‌چاله (فرضی)" else "Wormhole (hypothetical)",
                        value = if (fa) "بدون جرم، بدون گرانش" else "Massless, no gravity",
                        hint = if (fa)
                            "یک مدل آموزشی است، نه پدیده‌ای اثبات‌شده. جسم از مرکز یک دهانه وارد و با همان سرعت از دهانه جفت خارج می‌شود."
                        else
                            "A teaching model, not an established phenomenon. A body entering one mouth's centre leaves the partner with the same velocity."
                    )
                }
                BodyType.TEST_MARBLE -> {
                    Divider()
                    ToggleRow(
                        title = if (fa) "برخورد کشسان بین جسم‌های آزمایشی" else "Bounce between test marbles",
                        checked = vm.marbleBounce,
                        tag = "inspector_bounce"
                    ) { vm.setMarbleBounce(it) }
                }
                else -> Unit
            }

            Divider()
            ToggleRow(
                title = if (fa) "نمایش بردارها" else "Show vectors",
                checked = vm.showVectors,
                tag = "inspector_vectors"
            ) { vm.toggleVectors() }
            ToggleRow(
                title = if (fa) "نمایش مرکز جرم" else "Show barycentre",
                checked = vm.showBarycenter,
                tag = "inspector_barycenter"
            ) { vm.toggleBarycenter() }

            if (vm.predictionApproximate) {
                Text(
                    text = if (fa)
                        "پیش‌نمایش تقریبی: این جسم سهم زیادی از جرم سامانه دارد، پس مسیر نقطه‌چین فقط یک تخمین است."
                    else
                        "Approximate preview: this body carries a large share of the system mass, so the dotted path is an estimate.",
                    color = c.onSurfaceDim,
                    fontSize = 11.sp
                )
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun DirectionDial(angleRad: Double, onAngle: (Double) -> Unit) {
    val c = LocalGravityColors.current
    Canvas(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(c.onSurface.copy(alpha = 0.06f))
            .border(1.dp, c.chromeBorder, CircleShape)
            .testTag("inspector_direction_dial")
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val dx = (change.position.x - cx).toDouble()
                    val dy = (cy - change.position.y).toDouble()
                    if (dx != 0.0 || dy != 0.0) onAngle(atan2(dy, dx))
                }
            }
    ) {
        val r = size.minDimension / 2f - 6f
        val cx = size.width / 2f
        val cy = size.height / 2f
        drawCircle(c.onSurfaceDim.copy(alpha = 0.35f), r, Offset(cx, cy), style = Stroke(width = 1f))
        val ex = cx + (cos(angleRad) * r).toFloat()
        val ey = cy - (sin(angleRad) * r).toFloat()
        drawLine(c.accent, Offset(cx, cy), Offset(ex, ey), strokeWidth = 2.5f)
        drawCircle(c.accent, 3.5f, Offset(ex, ey))
    }
}

@Composable
private fun LabelRow(title: String, value: String, hint: String? = null) {
    val c = LocalGravityColors.current
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, color = c.onSurfaceDim, fontSize = 12.sp)
            Text(value, color = c.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        if (hint != null) {
            Text(hint, color = c.onSurfaceDim.copy(alpha = 0.75f), fontSize = 10.sp)
        }
    }
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    val c = LocalGravityColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = c.onSurface, fontSize = 13.sp)
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = c.accent,
                checkedTrackColor = c.accent.copy(alpha = 0.35f)
            ),
            modifier = Modifier.testTag(tag)
        )
    }
}

@Composable
private fun IconAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    tag: String,
    onClick: () -> Unit
) {
    val c = LocalGravityColors.current
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickableTag(tag, onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = c.onSurfaceDim, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun TextAction(
    text: String,
    tag: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Filled.RadioButtonChecked,
    onClick: () -> Unit
) {
    val c = LocalGravityColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.accent.copy(alpha = 0.10f))
            .clickableTag(tag, onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = c.accent, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = c.accent, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun Divider() {
    val c = LocalGravityColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(c.chromeBorder)
    )
}

@Composable
private fun sliderColors() = SliderDefaults.colors(
    thumbColor = LocalGravityColors.current.accent,
    activeTrackColor = LocalGravityColors.current.accent,
    inactiveTrackColor = LocalGravityColors.current.onSurfaceDim.copy(alpha = 0.25f)
)

package com.zig.gravity.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zig.gravity.ui.theme.LocalGravityColors

/**
 * The one slider the sandbox uses (§4).
 *
 * The previous inspector drove Material's `Slider` from values recomputed out of `vm.snapshot` —
 * plain `DoubleArray`s that are **not** Compose state. The control was therefore "controlled" by a
 * value that never changed during recomposition, which is exactly why the thumb stuck, snapped
 * back, or ignored the track. This fixes the root cause instead of papering over it:
 *
 *  * **Owns its drag state.** While a finger is down the thumb follows the finger from a local
 *    `Float`, so it can never fight a stale value handed back by the caller.
 *  * **Re-syncs when idle.** Values changing from elsewhere (a new selection, a preset load, a
 *    mass edit) are adopted the moment the finger is up.
 *  * **The whole track is live, including tap-to-seek.** A press anywhere jumps there and starts
 *    dragging from that point — no dead zone and no finger/thumb offset.
 *  * **It claims the gesture.** Every change is consumed inside the control, so no ancestor
 *    detector can steal the drag, and nothing is layered on top of it.
 *  * **RTL-aware.** In Persian the track runs right to left and the fraction is mirrored.
 *  * **48 dp touch target** over a thin drawn track (§24).
 */
@Composable
fun SandboxSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    label: String? = null,
    readout: String? = null,
    tag: String = "",
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null
) {
    val colors = LocalGravityColors.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    val span = (valueRange.endInclusive - valueRange.start).let { if (it == 0f) 1f else it }
    val startValue = valueRange.start

    var dragging by remember { mutableStateOf(false) }
    var localFraction by remember { mutableFloatStateOf(0f) }

    val externalFraction = ((value - startValue) / span).coerceIn(0f, 1f)
    // Not dragging => the simulation is the source of truth. Dragging => the finger is.
    val fraction = if (dragging) localFraction else externalFraction

    Column(modifier = modifier.fillMaxWidth()) {
        if (label != null || readout != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (label != null) {
                    Text(
                        text = label,
                        color = colors.onSurfaceDim,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (readout != null) {
                    Text(
                        text = readout,
                        color = colors.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = if (tag.isEmpty()) Modifier else Modifier.testTag("${tag}_readout")
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .then(if (tag.isEmpty()) Modifier else Modifier.testTag(tag))
                .semantics {
                    progressBarRangeInfo =
                        ProgressBarRangeInfo(current = startValue + fraction * span, range = valueRange)
                    if (label != null) contentDescription = label
                }
                .pointerInput(enabled, startValue, span, rtl) {
                    if (!enabled) return@pointerInput
                    awaitEachGesture {
                        val width = size.width.toFloat()
                        val down = awaitFirstDown(requireUnconsumed = false)
                        // Claim it straight away: an ancestor pan/zoom detector must never be able
                        // to take this drag mid-stroke (§4, §24).
                        down.consume()
                        if (width <= 0f) return@awaitEachGesture

                        fun fractionAt(x: Float): Float {
                            val raw = (x / width).coerceIn(0f, 1f)
                            return if (rtl) 1f - raw else raw
                        }

                        dragging = true
                        // Tap-to-seek: the press alone already commits a value.
                        localFraction = fractionAt(down.position.x)
                        onValueChange(startValue + localFraction * span)

                        horizontalDrag(down.id) { change ->
                            localFraction = fractionAt(change.position.x)
                            onValueChange(startValue + localFraction * span)
                            change.consume()
                        }

                        dragging = false
                        onValueChangeFinished?.invoke()
                    }
                }
        ) {
            val track = colors.onSurfaceDim.copy(alpha = if (enabled) 0.25f else 0.12f)
            val fill = if (enabled) colors.accent else colors.accent.copy(alpha = 0.35f)

            Canvas(modifier = Modifier.fillMaxSize()) {
                val cy = size.height * 0.5f
                val trackH = 4.dp.toPx()
                val thumbR = 9.dp.toPx()
                val left = thumbR
                val right = (size.width - thumbR).coerceAtLeast(left)
                val usable = right - left

                drawRoundRect(
                    color = track,
                    topLeft = Offset(0f, cy - trackH * 0.5f),
                    size = Size(size.width, trackH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackH * 0.5f)
                )

                val visual = if (rtl) 1f - fraction else fraction
                val thumbX = left + usable * visual

                if (rtl) {
                    drawRoundRect(
                        color = fill,
                        topLeft = Offset(thumbX, cy - trackH * 0.5f),
                        size = Size((size.width - thumbX).coerceAtLeast(0f), trackH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackH * 0.5f)
                    )
                } else {
                    drawRoundRect(
                        color = fill,
                        topLeft = Offset(0f, cy - trackH * 0.5f),
                        size = Size(thumbX.coerceAtLeast(0f), trackH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackH * 0.5f)
                    )
                }

                drawCircle(color = fill, radius = thumbR, center = Offset(thumbX, cy), style = Fill)
            }
        }
    }
}

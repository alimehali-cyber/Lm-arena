package com.zig.gargantua.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.zig.gargantua.renderer.GargantuaSurfaceView
import com.zig.gargantua.renderer.GargantuaTelemetry
import com.zig.gargantua.util.GargantuaCapability
import com.zig.gravity.ui.ImmersiveScreenState
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Root Composable for the Gargantua laboratory screen.
 * Validates GLES 3.x capability before creating the surface. On supported devices,
 * hosts the native OpenGL ES 3.x surface view via AndroidView with lifecycle observation.
 * On unsupported devices, renders a clean informative fallback card.
 */
@Composable
fun GargantuaRoot(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    startInPersian: Boolean = false,
    startInDarkTheme: Boolean = true
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val isFa = startInPersian
    val isGles3Supported = remember(context) {
        GargantuaCapability.isGles3Supported(context)
    }

    // Intercept Android hardware/gesture back to return cleanly to Lab screen
    BackHandler(enabled = true) {
        onBack()
    }

    // Host shell hides bottom floating navigation while Gargantua owns the display
    DisposableEffect(Unit) {
        ImmersiveScreenState.enter()
        onDispose {
            ImmersiveScreenState.exit()
        }
    }

    if (!isGles3Supported) {
        // Fallback screen for devices lacking OpenGL ES 3.x capability
        GargantuaUnsupportedScreen(
            onBack = onBack,
            isFa = isFa,
            modifier = modifier
        )
    } else {
        // Active OpenGL ES 3.x Renderer
        GargantuaRendererScreen(
            onBack = onBack,
            isFa = isFa,
            startInDarkTheme = startInDarkTheme,
            lifecycleOwner = lifecycleOwner,
            modifier = modifier
        )
    }
}

@Composable
private fun GargantuaUnsupportedScreen(
    onBack: () -> Unit,
    isFa: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0C14))
            .testTag("gargantua_unsupported_root")
    ) {
        // Top navigation bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x99181B26))
                    .border(1.dp, Color(0x338899AA), CircleShape)
                    .clickable { onBack() }
                    .testTag("gargantua_back_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = if (isFa) "بازگشت" else "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = if (isFa) "گارگانتوا" else "Gargantua",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isFa) "عدم پشتیبانی سخت‌افزاری" else "Hardware Not Supported",
                    color = Color(0xFFEF9A9A),
                    fontSize = 11.sp
                )
            }
        }

        // Informative center card
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xEE161A26))
                .border(1.dp, Color(0x33FFA726), RoundedCornerShape(20.dp))
                .padding(24.dp)
                .testTag("gargantua_unsupported_fallback"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFA726)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFFA726),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = if (isFa) "نیازمند OpenGL ES 3.x" else "OpenGL ES 3.x Required",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (isFa)
                        "آزمایشگاه نسبیتی گارگانتوا به خط لوله پردازش گرافیکی OpenGL ES 3.0 نیازمند است که توسط پردازنده گرافیکی این دستگاه پشتیبانی نمی‌شود.\n\nسایر بخش‌های برنامه زیگ (رصد ستاره‌ها، ردگیری ماهواره‌ها، واقعیت افزوده و میز گرانش) بدون هیچ مشکلی در دسترس شما هستند."
                    else
                        "Gargantua's laboratory requires hardware OpenGL ES 3.0 or higher for its native shader pipeline. This device's GPU does not support OpenGL ES 3.0.\n\nThe rest of ZIG (sky observation, satellite tracking, AR compass, and Gravity Sandbox) remains fully functional.",
                    color = Color(0xFFB0BEC5),
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                ElevatedButton(
                    onClick = onBack,
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = Color(0xFF263238),
                        contentColor = Color(0xFF90CAF9)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("gargantua_fallback_back_button")
                ) {
                    Text(
                        text = if (isFa) "بازگشت به آزمایشگاه" else "Return to Lab",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun GargantuaRendererScreen(
    onBack: () -> Unit,
    isFa: Boolean,
    startInDarkTheme: Boolean,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    modifier: Modifier = Modifier
) {
    var surfaceViewRef by remember { mutableStateOf<GargantuaSurfaceView?>(null) }
    var telemetry by remember { mutableStateOf(GargantuaTelemetry()) }

    // Lifecycle observation to pause/resume the GL thread cleanly
    DisposableEffect(lifecycleOwner, surfaceViewRef) {
        val sv = surfaceViewRef
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> sv?.onPause()
                Lifecycle.Event.ON_RESUME -> sv?.onResume()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            sv?.onPause()
        }
    }

    // Periodic telemetry polling loop (every 250ms) to update UI HUD
    LaunchedEffect(surfaceViewRef) {
        val sv = surfaceViewRef ?: return@LaunchedEffect
        while (true) {
            telemetry = sv.renderer.stateHolder.getTelemetry()
            delay(250)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0C14))
            .testTag("gargantua_root")
    ) {
        // Native GPU surface view embedded via AndroidView
        AndroidView(
            factory = { ctx ->
                GargantuaSurfaceView(ctx).also { sv ->
                    sv.renderer.stateHolder.updateState {
                        it.copy(
                            isDarkTheme = startInDarkTheme,
                            isPersian = isFa
                        )
                    }
                    surfaceViewRef = sv
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("gargantua_gl_surface")
        )

        // Top Chrome: Back button, title, reset button, and telemetry chip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x99181B26))
                        .border(1.dp, Color(0x338899AA), CircleShape)
                        .clickable { onBack() }
                        .testTag("gargantua_back_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = if (isFa) "بازگشت" else "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = if (isFa) "گارگانتوا" else "Gargantua",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isFa) "دوربین نسبیتی و رندر سینمایی (M6)" else "Relativistic Camera & Cinematic HDR (M6)",
                        color = Color(0xFF88A0C0),
                        fontSize = 11.sp
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Reset Camera Action Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x99181B26))
                        .border(1.dp, Color(0x338899AA), CircleShape)
                        .clickable { surfaceViewRef?.resetCamera() }
                        .testTag("gargantua_reset_camera_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = if (isFa) "بازنشانی زاویه دید دوربین" else "Reset Observer Camera",
                        tint = Color(0xFF90CAF9),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // GPU telemetry badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x99181B26))
                        .border(1.dp, Color(0x33446688), RoundedCornerShape(16.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("gargantua_fps_badge")
                ) {
                    val fpsText = if (telemetry.fps > 0f) {
                        String.format(Locale.US, "%.0f FPS", telemetry.fps)
                    } else {
                        "-- FPS"
                    }
                    Text(
                        text = fpsText,
                        color = Color(0xFF64B5F6),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Bottom-corner compact information HUD card
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 16.dp, bottom = 16.dp, end = 16.dp)
                .testTag("gargantua_status_card")
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 240.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xD9121520))
                    .border(1.dp, Color(0x33446688), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Row 1: Status indicator, Title, and HDR Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (telemetry.isInitialized) Color(0xFF4CAF50) else Color(0xFFFF9800))
                        )
                        Text(
                            text = if (isFa) "فضازمان کر (نسبیتی)" else "Kerr GR (M6)",
                            color = Color(0xFFD0D8E8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (telemetry.isHdrActive) Color(0x3381C784) else Color(0x33FFB74D))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = if (telemetry.isHdrActive) "HDR" else "LDR",
                                color = if (telemetry.isHdrActive) Color(0xFF81C784) else Color(0xFFFFB74D),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Row 2: Physical & Camera parameters
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = String.format(Locale.US, "a*=%.2f", telemetry.spin),
                            color = Color(0xFF64B5F6),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = String.format(Locale.US, "ISCO=%.2fM", telemetry.iscoRadius),
                            color = Color(0xFFFFB74D),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = String.format(Locale.US, "d=%.0fM", telemetry.camDist),
                            color = Color(0xFFCE93D8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = String.format(Locale.US, "φ=%.0f°", telemetry.camAzimuthDeg),
                            color = Color(0xFF81D4FA),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Row 3: Internal resolution, scale factor, and frame time
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (telemetry.renderResolution.isNotEmpty()) {
                            Text(
                                text = String.format(Locale.US, "%s@%.1fx", telemetry.renderResolution, telemetry.renderScale),
                                color = Color(0xFF80CBC4),
                                fontSize = 10.sp
                            )
                        }
                        if (telemetry.frameTimeMs > 0f) {
                            Text(
                                text = String.format(Locale.US, "• %.1f ms", telemetry.frameTimeMs),
                                color = Color(0xFF88A0C0),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // Error message if any
            if (telemetry.errorMessage != null) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xCCB71C1C))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = telemetry.errorMessage ?: "",
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

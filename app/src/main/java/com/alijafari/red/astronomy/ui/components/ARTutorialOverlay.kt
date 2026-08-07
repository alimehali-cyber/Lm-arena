package com.alijafari.red.astronomy.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.ui.theme.*

enum class ARTutorialTopic(
    val titleEn: String,
    val titleFa: String,
    val icon: ImageVector
) {
    PILLS_OVERVIEW("Top Control System", "سیستم کنترل ۵ گانه", Icons.Default.ViewCarousel),
    CAM_GPS("Camera & Sensors", "دوربین و حسگرها", Icons.Default.Sensors),
    TIME_MACHINE("Time Machine", "ماشین زمان رصد", Icons.Default.Schedule),
    SSA("Solar System AR", "واقعیت افزوده منظومه شمسی", Icons.Default.Public),
    MODES("AR Modes", "حالت‌های رصد و گرافیک", Icons.Default.Layers),
    SEARCH("Celestial Search & Finder", "جستجو و هدف‌یابی", Icons.Default.Search)
}

data class TutorialStep(
    val stepIndex: Int,
    val totalSteps: Int,
    val titleEn: String,
    val titleFa: String,
    val textEn: String,
    val textFa: String,
    val icon: ImageVector
)

object ARTutorialData {
    fun getSteps(topic: ARTutorialTopic): List<TutorialStep> {
        return when (topic) {
            ARTutorialTopic.PILLS_OVERVIEW -> listOf(
                TutorialStep(
                    1, 3,
                    "Top 5-Pill Control Bar", "پنل کنترل ۵ نوار بالای صفحه",
                    "The top control bar gives instant access to Cam/GPS, Time Machine, Solar System AR, Visual Modes, and Search.",
                    "پنل بالای صفحه دسترسی سریع به دوربین/موقعیت‌یاب، ماشین زمان، منظومه شمسی، حالت‌های رصد و جستجو را فراهم می‌کند.",
                    Icons.Default.ViewCarousel
                ),
                TutorialStep(
                    2, 3,
                    "Single Expanded Pill", "کنترل هوشمند و خلوت",
                    "To keep your sky view clutter-free, only one pill expands at a time. Tap anywhere on the sky to collapse it.",
                    "برای حفظ خلوتی صفحه، فقط یک پنل در هر لحظه باز است. با لمس آسمان پنل باز بسته می‌شود.",
                    Icons.Default.Tune
                ),
                TutorialStep(
                    3, 3,
                    "Gestures & Back Button", "ژست‌های لمسی و کلید بازگشت",
                    "Pressing the back button or tapping the sky automatically closes active panels before navigating away.",
                    "فشردن دکمه بازگشت یا لمس صفحه ابتدا پنل‌های باز را می‌بندد.",
                    Icons.Default.TouchApp
                )
            )

            ARTutorialTopic.CAM_GPS -> listOf(
                TutorialStep(
                    1, 3,
                    "Live Camera & GPS", "دوربین زنده و GPS دقیق",
                    "Toggle camera feed overlay and live location updates to automatically adjust magnetic declination.",
                    "تصویر زنده دوربین و موقعیت‌یاب ماهواره‌ای را برای انطباق دقیق قطب مغناطیسی فعال کنید.",
                    Icons.Default.GpsFixed
                ),
                TutorialStep(
                    2, 3,
                    "Sensor Fusion Gyroscope", "ترکیب حسگرهای ژیروسکوپ",
                    "Combines accelerometer and magnetometer data for fluid 60 FPS movement tracking across the night sky.",
                    "ترکیب داده‌های شتاب‌سنج و مغناطیس‌سنج جهت ردگیری روان ۶۰ فریم بر ثانیه آسمان.",
                    Icons.Default.Sensors
                ),
                TutorialStep(
                    3, 3,
                    "Figure-8 Calibration", "کالیبراسیون قطب‌نما (حرکت ۸)",
                    "If directional precision drops, move your phone in a 3D figure-8 loop to calibrate the sensors.",
                    "در صورت کاهش دقت قطب‌نما، گوشی را به شکل عدد ۸ انگلیسی در فضا حرکت دهید.",
                    Icons.Default.CompassCalibration
                )
            )

            ARTutorialTopic.TIME_MACHINE -> listOf(
                TutorialStep(
                    1, 3,
                    "Time Travel Simulation", "سفر در زمان نجومی",
                    "Scrub through past and future centuries to preview planet alignments, eclipses, and constellation visibility.",
                    "در قرون گذشته و آینده سفر کنید تا موقعیت سیارات و کسوف‌ها را پیش‌بینی نمایید.",
                    Icons.Default.Schedule
                ),
                TutorialStep(
                    2, 3,
                    "Fast Forward Controls", "کنترل سرعت بازپخش",
                    "Accelerate time up to 10,000x or 1 day per second to observe diurnal sky motion and orbit paths.",
                    "سرعت زمان را تا ۱۰,۰۰۰ برابر افزایش دهید تا چرخش شبانه‌روزی و مدارهای آسمانی را ببینید.",
                    Icons.Default.FastForward
                ),
                TutorialStep(
                    3, 3,
                    "Smooth Return to Live", "بازگشت نرم به زمان واقعی",
                    "Tap 'LIVE' for a smooth 1-second animated return to current real-time sky coordinates.",
                    "با لمس دکمه LIVE با انیمیشن یک ثانیه‌ای نرم به زمان حال واقعی بازگردید.",
                    Icons.Default.History
                )
            )

            ARTutorialTopic.SSA -> listOf(
                TutorialStep(
                    1, 3,
                    "Solar System AR (SSA)", "واقعیت افزوده منظومه شمسی",
                    "Projects 3D Keplerian planetary orbits and real-time celestial alignments directly into your camera view.",
                    "مدارهای سه‌بعدی کپلری و موقعیت واقعی سیارات منظومه شمسی را در دوربین نمایش می‌دهد.",
                    Icons.Default.Public
                ),
                TutorialStep(
                    2, 3,
                    "3D Orbital Planes", "صفحات مداری سه‌بعدی",
                    "Visualize ecliptic tilt, planetary distances, and Galilean moon positions with physical accuracy.",
                    "انحراف صفحه دایره‌البروج و مدارهای سه‌بعدی را با دقت فیزیکی مشاهده کنید.",
                    Icons.Default.Public
                ),
                TutorialStep(
                    3, 3,
                    "Scale & Distance Toggle", "مقیاس فواصل سیارات",
                    "Switch between realistic scaled distance mode and enhanced visibility mode for dark skies.",
                    "بین حالت مقیاس واقعی فواصل و حالت دید تقویت‌شده تغییر وضعیت دهید.",
                    Icons.Default.Straighten
                )
            )

            ARTutorialTopic.MODES -> listOf(
                TutorialStep(
                    1, 4,
                    "Sky View (AR)", "حالت دید آسمان (AR)",
                    "Full camera feed with interactive celestial object overlays, magnitude glow, and trajectory arcs.",
                    "نمای کامل دوربین همراه با لایه‌های تعاملی صورت‌های فلکی و نوار راه شیری.",
                    Icons.Default.CameraAlt
                ),
                TutorialStep(
                    2, 4,
                    "360° Compass View", "حالت قطب‌نمای ۳۶۰ درجه",
                    "High-contrast cardinal compass ring featuring precision azimuth, altitude telemetry, and pitch angle.",
                    "حلقه‌ای با کنتراست بالای قطب‌نما با نمایش دقیق زاویه سمت و ارتفاع و جهات اصلی.",
                    Icons.Default.Explore
                ),
                TutorialStep(
                    3, 4,
                    "Constellation Lines Overlay", "خطوط و خط‌چین صورت‌های فلکی",
                    "Renders delicate anti-aliased constellation connection lines using the scientific star catalog.",
                    "رسم خطوط اتصال ستارگان صورت‌های فلکی با قابلیت تنظیم شفافیت و درخشش.",
                    Icons.Default.Polyline
                ),
                TutorialStep(
                    4, 4,
                    "Eclipse Preview AR", "پیش‌نمایش خورشیدگرفتگی در AR",
                    "Preview solar and lunar eclipse geometry in AR with contact times, coverage %, and direction guide.",
                    "پیش‌نمایش خورشیدگرفتگی و ماه گرفتگی درواقعیت افزوده به همراه زمان‌بندی و درصد پوشش.",
                    Icons.Default.WbSunny
                )
            )

            ARTutorialTopic.SEARCH -> listOf(
                TutorialStep(
                    1, 3,
                    "Instant Celestial Search", "جستجوی آنی اجرام",
                    "Search stars, planets, constellations, Messier objects, and the ISS by Persian or English name.",
                    "جستجوی خورشید، سیارات، ستارگان، سحابی‌ها و ایستگاه فضایی به فارسی و انگلیسی.",
                    Icons.Default.Search
                ),
                TutorialStep(
                    2, 3,
                    "Target Locking & Finder", "قفل هدف و فلش راهنما",
                    "Lock onto any object to display a 360° direction arrow guiding your phone right to the target.",
                    "روی هر جرم قفل کنید تا فلش راهنما گوشی شما را دقیقا به سمت آن هدایت کند.",
                    Icons.Default.GpsFixed
                ),
                TutorialStep(
                    3, 3,
                    "Haptic Target Acquired", "بازخورد لمسی در ردیابی",
                    "Vibrates gently and opens celebration details when your reticle locks onto the target.",
                    "هنگام قرارگیری نشانه‌رو روی هدف، ویبره هپتیک فعال شده و شناسنامه جرم باز می‌شود.",
                    Icons.Default.Vibration
                )
            )
        }
    }
}

@Composable
fun ARTutorialModal(
    topic: ARTutorialTopic,
    isFa: Boolean,
    onDismiss: () -> Unit
) {
    val steps = remember(topic) { ARTutorialData.getSteps(topic) }
    var currentStepIndex by remember { mutableIntStateOf(0) }
    val step = steps[currentStepIndex]

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(
                shape = CircleShape,
                color = AccentPrimary.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, AccentPrimary)
            ) {
                Icon(
                    imageVector = step.icon,
                    contentDescription = null,
                    tint = AccentPrimary,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(32.dp)
                )
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isFa) topic.titleFa else topic.titleEn,
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isFa) step.titleFa else step.titleEn,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isFa) step.textFa else step.textEn,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = TextSecondary,
                    lineHeight = 22.sp
                )

                // Step Progress Indicator Pill
                Surface(
                    shape = CircleShape,
                    color = NavyBackground,
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Text(
                        text = if (isFa) "گام ${step.stepIndex} از ${step.totalSteps}" else "Step ${step.stepIndex} of ${step.totalSteps}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AccentSecondary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (currentStepIndex < steps.size - 1) {
                        currentStepIndex++
                    } else {
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (currentStepIndex < steps.size - 1) (if (isFa) "گام بعدی ➔" else "Next ➔")
                    else (if (isFa) "متوجه شدم" else "Got It!"),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            if (currentStepIndex > 0) {
                TextButton(onClick = { currentStepIndex-- }) {
                    Text(if (isFa) "قبلی" else "Previous", color = TextSecondary)
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text(if (isFa) "بستن" else "Close", color = TextSecondary)
                }
            }
        },
        containerColor = BackgroundCard,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.testTag("ar_tutorial_dialog")
    )
}

package com.alijafari.red.astronomy.ui.components

import androidx.compose.foundation.BorderStroke
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

enum class SSATutorialCategory {
    SCALE_WALK,
    GRAVITY_SANDBOX
}

data class SSAStep(
    val stepIndex: Int,
    val totalSteps: Int,
    val titleEn: String,
    val titleFa: String,
    val textEn: String,
    val textFa: String,
    val icon: ImageVector
)

object SSATutorialData {
    fun getSteps(category: SSATutorialCategory): List<SSAStep> {
        return when (category) {
            SSATutorialCategory.SCALE_WALK -> listOf(
                SSAStep(
                    1, 4,
                    "Solar System Scale Walk", "پیمایش مقیاس‌دار منظومه شمسی",
                    "Step into a 1:1 mathematically accurate 3D model of the Solar System anchored right in your physical environment.",
                    "قدم به مدل سه‌بعدی و دقیق منظومه شمسی بگذارید که مستقیماً در محیط اطراف شما تثبیت شده است.",
                    Icons.Default.DirectionsWalk
                ),
                SSAStep(
                    2, 4,
                    "Walking Scale Selection", "انتخاب مقیاس گام رصدگر",
                    "Choose walking scale from 1M km/step up to 1 AU/step. As you walk forward in physical space, your position advances through interplanetary space.",
                    "مقیاس هر گام را از ۱ میلیون کیلومتر تا ۱ واحد نجومی تنظیم کنید. با قدم زدن به جلو، در فضای میان‌سیاره‌ای حرکت می‌کنید.",
                    Icons.Default.Straighten
                ),
                SSAStep(
                    3, 4,
                    "Radius Exaggeration vs Real Scale", "بزرگ‌نمایی شعاع در برابر مقیاس واقعی",
                    "Distance between planets always matches strict Keplerian orbital math. Toggle radius exaggeration to enlarge distant planets for easy viewing.",
                    "فاصله‌های بین سیارات همواره مطابق با محاسبات مداری کپلری است. بزرگ‌نمایی شعاع برای دیدن بهتر سیارات دوردست به کار می‌رود.",
                    Icons.Default.AspectRatio
                ),
                SSAStep(
                    4, 4,
                    "Origin Reset & AR Stability", "بازنشانی مبدأ و پایداری AR",
                    "If position drifts or you wish to re-center the Sun at your feet, tap 'Reset Origin'. Ensures stable 60 FPS placement.",
                    "در صورت جابه‌جایی یا تمایل به قرار دادن خورشید زیر پای خود، دکمه 'بازنشانی مبدأ' را لمس کنید.",
                    Icons.Default.CenterFocusStrong
                )
            )

            SSATutorialCategory.GRAVITY_SANDBOX -> listOf(
                SSAStep(
                    1, 4,
                    "Newtonian Gravity Simulator", "شبیه‌ساز گرانش نیوتونی",
                    "Experiment with real N-body gravitational physics. Place stars, planets, comets, or black holes and observe orbital dynamics.",
                    "با فیزیک گرانش چندجرمی نیوتونی آزمایش کنید. ستاره‌ها، سیارات و سیاه‌چاله را قرار دهید و مدارهای آن‌ها را بررسی کنید.",
                    Icons.Default.Public
                ),
                SSAStep(
                    2, 4,
                    "Interactive Swipe Launch", "پرتاب تعاملی با لمس و کشیدن",
                    "Tap to spawn objects, then drag and release to set initial velocity vectors. Observe orbital capture or escape trajectories.",
                    "با لمس، جرم جدید بسازید و با کشیدن انگشت، بردار سرعت اولیه بدهید تا به مدار یا مسیر فرار برود.",
                    Icons.Default.TouchApp
                ),
                SSAStep(
                    3, 4,
                    "Collision & Black Hole Physics", "برخورد اجرام و فیزیک سیاه‌چاله",
                    "Select collision modes (Merge, Elastic, Destroy, Ignore). Black holes warp space-time and capture surrounding matter.",
                    "حالت‌های برخورد (ادغام، کشسان، نابودی، عبور) را انتخاب کنید. سیاه‌چاله‌ها دارای افق رویداد و همگرایی گرانشی هستند.",
                    Icons.Default.BlurCircular
                ),
                SSAStep(
                    4, 4,
                    "Presets & Lagrange Points", "پیش‌فرض‌ها و نقاط لاگرانژی",
                    "Explore famous orbital presets including 3-Body Chaos, Lagrange Points (L1-L5), and Gravitational Slingshot maneuvers.",
                    "سامانه‌های معروف از جمله آشوب سه جسمی، نقاط لاگرانژی (L1-L5) و مانور قلاب‌سنگی گرانشی را بررسی نمایید.",
                    Icons.Default.AutoAwesome
                )
            )
        }
    }
}

@Composable
fun SSATutorialModal(
    category: SSATutorialCategory,
    isFa: Boolean,
    onDismiss: () -> Unit
) {
    val steps = remember(category) { SSATutorialData.getSteps(category) }
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
                    text = if (category == SSATutorialCategory.SCALE_WALK) (if (isFa) "راهنمای پیمایش منظومه شمسی" else "Solar System Walk Guide") else (if (isFa) "راهنمای آزمایشگاه گرانش" else "Gravity Sandbox Guide"),
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
        modifier = Modifier.testTag("ssa_tutorial_dialog")
    )
}

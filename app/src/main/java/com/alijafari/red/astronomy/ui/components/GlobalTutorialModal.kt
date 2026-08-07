package com.alijafari.red.astronomy.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.ui.theme.*

enum class RedTutorialTopic(
    val titleEn: String,
    val titleFa: String,
    val icon: ImageVector
) {
    SATELLITES("Satellites & Earth Engine", "کنترل‌سنتر ماهواره‌ها و زمین زنده", Icons.Default.SatelliteAlt),
    TIDES("Ocean Tidal Simulator", "شبیه‌ساز زنده مَدّ و جزر اقیانوسی", Icons.Default.Waves),
    SCALE_WALK("Scale Walk (SSA)", "پیمایش مقیاس نجومی", Icons.Default.DirectionsWalk),
    SPACE_TIME("Space-Time Explorer (SSA)", "کاوشگر زمان-فضا و انحنای گرانش", Icons.Default.Timelapse),
    GRAVITY_SANDBOX("Gravity Sandbox (SSA)", "آزمایشگاه چندجرمی گرانش", Icons.Default.Public),
    AR_CONSTELLATIONS("AR Constellation Lines", "صورت‌های فلکی در واقعیت افزوده", Icons.Default.Stars),
    AR_ECLIPSE("AR Eclipse Preview", "پیش‌نمایش واقعیت افزوده گرفتگی‌ها", Icons.Default.Brightness3),
    MOON_EXPLORER("Moon 3D & Librations", "رصدخانه و فازهای ۳بعدی ماه", Icons.Default.NightlightRound)
}

data class UnifiedTutorialStep(
    val stepIndex: Int,
    val totalSteps: Int,
    val titleEn: String,
    val titleFa: String,
    val purposeEn: String,
    val purposeFa: String,
    val scientificBackgroundEn: String,
    val scientificBackgroundFa: String,
    val usageStepsEn: List<String>,
    val usageStepsFa: List<String>,
    val offlineAndPerformanceEn: String,
    val offlineAndPerformanceFa: String,
    val icon: ImageVector
)

object RedTutorialData {
    fun getSteps(topic: RedTutorialTopic): List<UnifiedTutorialStep> {
        return when (topic) {
            RedTutorialTopic.SATELLITES -> listOf(
                UnifiedTutorialStep(
                    1, 2,
                    "Live Satellite Propagation", "ردیابی مداری پیشرفته ماهواره‌ها",
                    "Track artificial satellites including ISS, Starlink trains, Hubble, JWST, and weather satellites in real-time.",
                    "ردیابی زنده ایستگاه فضایی بین‌المللی، قطارهای ماهواره‌ای استارلینک، تلسکوپ‌های هابل و جیمز وب و ماهواره‌های هواشناسی.",
                    "Calculates precise orbital coordinates using the SGP4/J2 gravitational perturbation propagator and cached NORAD TLE orbital elements.",
                    "محاسبه موقعیت دقیق مداری با استفاده از مدل اختلال گرانشی SGP4/J2 و پارامترهای دوخطی TLE.",
                    listOf(
                        "1. Select any satellite from the top selector bar.",
                        "2. Inspect live speed, altitude, and range telemetry.",
                        "3. Tap on 'Information' for 5 verified facts and mission details."
                    ),
                    listOf(
                        "۱. ماهواره موردنظر را از نوار بالای صفحه انتخاب کنید.",
                        "۲. پارامترهای زنده سرعت، ارتفاع و فاصله مستقیم را بررسی کنید.",
                        "۳. دکمه اطلاعات را لمس کنید تا ۵ حقیقت علمی تاییدشده را ببینید."
                    ),
                    "Fully operational offline using cached orbital elements. TLE data updates automatically when internet is available.",
                    "کاملاً آفلاین با المان‌های مداری ذخیره‌شده کار می‌کند. در صورت اتصال به اینترنت TLE به‌روزرسانی می‌شود.",
                    Icons.Default.SatelliteAlt
                ),
                UnifiedTutorialStep(
                    2, 2,
                    "Procedural Earth Engine & Pass Alerts", "موتور هندسه زمین و پیش‌بینی گذر",
                    "Renders a procedural 2D/3D Earth with real solar geometry, day/night terminator, and illuminated Iranian cities.",
                    "رندر هندسی کره زمین با زاویه واقعی نور خورشید، سایه شب و روز و شهرهای روشن ایران در شب.",
                    "Calculates subsolar position, solar zenith angles, and observer horizon passes to predict naked-eye visibility windows.",
                    "محاسبه موقعیت نقطه خورشیدزیرین، زاویه سمت‌الرأس خورشید و پیش‌بینی گذرهای قابل مشاهده با چشم غیرمسلح.",
                    listOf(
                        "1. Pinch to zoom and drag to rotate the Earth view.",
                        "2. Use the Time Machine slider to preview orbital paths +-24 hours.",
                        "3. Enable notification alerts to get notified prior to visible passes."
                    ),
                    listOf(
                        "۱. با دو انگشت زوم کنید و نقشه را بچرخانید.",
                        "۲. با اسلایدر ماشین زمان، مسیر مداری ۲۴ ساعت آینده و گذشته را ببینید.",
                        "۳. هشدار خودکار را فعال کنید تا قبل از گذرهای پرنور پیام دریافت کنید."
                    ),
                    "Runs smoothly at 60 FPS using GPU-accelerated Compose Canvas rendering.",
                    "اجرای بسیار روان با رندر گرافیکی شتاب‌یافته Canvas با سرعت ۶۰ فریم بر ثانیه.",
                    Icons.Default.Public
                )
            )

            RedTutorialTopic.TIDES -> listOf(
                UnifiedTutorialStep(
                    1, 1,
                    "Ocean Tidal Mechanics", "مکانیک مَدّ و جزر اقیانوسی",
                    "Interactive simulation explaining how lunar and solar gravitational gradients shape ocean bulges around Earth.",
                    "شبیه‌ساز تعاملی برای درک ملموس چگونگی شکل‌گیری برجستگی‌های مَدی اقیانوس‌ها توسط گرانش ماه و خورشید.",
                    "Tides arise from the differential gravitational force across Earth's diameter (proportional to G*M/r^3).",
                    "مَدّ و جزر از تفاوت کشش گرانشی ماه و خورشید در عرض قطر زمین (متناسب با معکوس مکعب فاصله) ناشی می‌شود.",
                    listOf(
                        "1. Drag the Moon around its orbit to change the Moon-Sun alignment angle.",
                        "2. Observe Spring Tides when Moon and Sun align (0° and 180°).",
                        "3. Observe Neap Tides when Moon and Sun pull at 90° angles.",
                        "4. Toggle vectors to view individual gravitational forces."
                    ),
                    listOf(
                        "۱. ماه را روی مدار بکشید تا زاویه آن با خورشید تغییر کند.",
                        "۲. پدیده مَدّ اکبر (Spring) را در زاویه ۰ و ۱۸۰ درجه رصد کنید.",
                        "۳. پدیده مَدّ کهین (Neap) را در زاویه ۹۰ درجه رصد کنید.",
                        "۴. بردارها را فعال کنید تا بردار نیروهای گرانشی را مجزا ببینید."
                    ),
                    "Works 100% offline with zero external network dependencies.",
                    "۱۰۰٪ آفلاین و بدون هیچ نیازی به اینترنت اجرا می‌شود.",
                    Icons.Default.Waves
                )
            )

            RedTutorialTopic.SCALE_WALK -> listOf(
                UnifiedTutorialStep(
                    1, 1,
                    "Solar System Scale Walk", "پیمایش مقیاس حقیقی منظومه شمسی",
                    "Explore true astronomical distances from 0.01 AU out to 100 AU in an interactive 3D perspective.",
                    "کاوش فواصل واقعی نجومی از ۰.۰۱ واحد نجومی تا ۱۰۰ واحد نجومی با نمای ۳بعدی.",
                    "Uses logarithmic distance scaling to compress vast interplanetary voids while preserving relative proportional sizes.",
                    "استفاده از مقیاس لگاریتمی فواصل برای نمایش همزمان اجرام نزدیک و سیارات دوردست.",
                    listOf(
                        "1. Drag the scale slider to walk outward into the Kuiper belt.",
                        "2. Tap any planet to view orbital velocity and orbital period."
                    ),
                    listOf(
                        "۱. اسلایدر مقیاس را بکشید تا به کمربند کایپر سفر کنید.",
                        "۲. روی هر سیاره ضربه بزنید تا سرعت مداری و دوره تناوب آن را ببینید."
                    ),
                    "Zero external network calls required.",
                    "بدون نیاز به شبکه و اینترنت.",
                    Icons.Default.DirectionsWalk
                )
            )

            RedTutorialTopic.SPACE_TIME -> listOf(
                UnifiedTutorialStep(
                    1, 1,
                    "Space-Time Grid Warping", "انحنای شبکه زمان-فضا و گرانش",
                    "Visualize General Relativity where massive planetary objects bend the 4D spacetime fabric.",
                    "تجسم نسبیت عام آلبرت اینشتین که در آن جرم سیارات شبکه زمان-فضا را خمیده می‌کند.",
                    "Simulates relativistic potential wells where grid depression depth is proportional to object mass divided by distance.",
                    "شبیه‌سازی چاه پتانسیل نسبیتی که در آن عمق خمیدگی متناسب با جرم سیاره است.",
                    listOf(
                        "1. Select different planets to see how mass deforms the local spacetime grid.",
                        "2. Rotate camera pitch and angle to view grid depression from any perspective."
                    ),
                    listOf(
                        "۱. سیارات مختلف را انتخاب کنید تا انحنای شبکه زمان-فضا را ببینید.",
                        "۲. زاویه دوربین را بچرخانید تا چاه پتانسیل گرانشی را بررسی کنید."
                    ),
                    "100% offline calculation using custom vector matrix transformation.",
                    "محاسبه ۱۰۰٪ آفلاین با ماتریس‌های تبدیل برداری.",
                    Icons.Default.Timelapse
                )
            )

            RedTutorialTopic.GRAVITY_SANDBOX -> listOf(
                UnifiedTutorialStep(
                    1, 1,
                    "N-Body Gravitational Physics", "آزمایشگاه فیزیک چندجرمی گرانشی",
                    "Spawn planetary masses in deep space and simulate real-time orbital mechanics and gravitational collisions.",
                    "افزودن اجرام گرانشی در فضا و شبیه‌سازی زنده مدارهای پیچیده و برخوردهای کیهانی.",
                    "Uses Runge-Kutta 4th order numerical integration to solve the N-body gravitational force differential equations.",
                    "استفاده از روش عددی رانگ-کوتا برای حل معادلات دیفرانسیل گرانشی چندجرمی.",
                    listOf(
                        "1. Tap 'Add Object' to spawn custom masses or preset solar systems.",
                        "2. Drag vectors to set initial velocity directions.",
                        "3. Adjust simulation speed multipliers up to 100,000x."
                    ),
                    listOf(
                        "۱. دکمه افزودن جرم را لمس کنید تا جرم جدید یا منظومه پیش‌فرض بسازید.",
                        "۲. فلش سرعت اولیه را بکشید تا سمت حرکت را تعیین کنید.",
                        "۳. سرعت شبیه‌سازی را تا ۱۰۰,۰۰۰ برابر افزایش دهید."
                    ),
                    "Runs smoothly with up to 50 active gravitational bodies.",
                    "اجرای بسیار روان تا ۵۰ جرم گرانشی همزمان.",
                    Icons.Default.Public
                )
            )

            RedTutorialTopic.AR_CONSTELLATIONS -> listOf(
                UnifiedTutorialStep(
                    1, 1,
                    "Official IAU Constellations", "صورت‌های فلکی رسمی IAU در AR",
                    "Overlay 88 official constellation boundaries and star connection figures onto your live camera sky.",
                    "نمایش ۸۸ صورت فلکی رسمی اتحادیه بین‌المللی اخترشناسی روی تصویر زنده دوربین.",
                    "Transforms equatorial Right Ascension and Declination to topocentric Azimuth and Altitude using Local Apparent Sidereal Time.",
                    "تبدیل مختصات استوایی (بعد و میل) به مختصات افقی (سمت و ارتفاع) با زمان نجومی محلی.",
                    listOf(
                        "1. Point your phone towards the sky.",
                        "2. Adjust line opacity using the slider in the Modes panel.",
                        "3. Enable horizon filtering to view only stars above your current horizon."
                    ),
                    listOf(
                        "۱. گوشی خود را به سمت آسمان بگیرید.",
                        "۲. میزان شفافیت خطوط را از پنل حالت‌ها تنظیم کنید.",
                        "۳. فیلتر افق را فعال کنید تا فقط صور فلکی بالای افق دیده شوند."
                    ),
                    "Uses sensor fusion for zero-latency camera tracking.",
                    "استفاده از ترکیب حسگرها جهت ردیابی بدون تاخیر دوربین.",
                    Icons.Default.Stars
                )
            )

            RedTutorialTopic.AR_ECLIPSE -> listOf(
                UnifiedTutorialStep(
                    1, 1,
                    "Solar & Lunar Eclipse AR", "پیش‌نمایش واقعیت افزوده گرفتگی‌ها",
                    "Preview upcoming and historical solar/lunar eclipse shadow progressions directly in the sky.",
                    "پیش‌نمایش زنده خورشیدگرفتگی‌ها و ماه گرفتگی‌های تاریخ در موقعیت دقیق آن‌ها در آسمان.",
                    "Computes Besselian elements and exact angular separations of the Sun and Moon disks to determine obscuration percentage.",
                    "محاسبه المان‌های بسل و جدایی زاویه‌ای قرص خورشید و ماه جهت تعیین درصد پوشش.",
                    listOf(
                        "1. Select an eclipse event from the preset list or set a custom time.",
                        "2. Follow the AR compass arrow to locate the Sun/Moon in the sky.",
                        "3. Observe calculated coverage percentage and shadow phase transition."
                    ),
                    listOf(
                        "۱. یک رویداد گرفتگی را از لیست پیش‌فرض انتخاب کرده یا زمان دلخواه وارد کنید.",
                        "۲. فلش راهنمای AR را دنبال کنید تا خورشید یا ماه را بیابید.",
                        "۳. درصد پوشش محاسبه‌شده و فاز سایه را رصد کنید."
                    ),
                    "Fully operational offline with astronomical polynomial approximations.",
                    "کاملاً آفلاین با محاسبات تقریبی دقیق چندجمله‌ای‌های نجومی.",
                    Icons.Default.Brightness3
                )
            )

            RedTutorialTopic.MOON_EXPLORER -> listOf(
                UnifiedTutorialStep(
                    1, 1,
                    "3D Moon & Libration", "ماه ۳بعدی و حرکات رخ‌گردی",
                    "Inspect Moon phases, crater surface maps, perigee/apogee distance variations, and optical librations.",
                    "بررسی فازهای ماه، نقشه گودال‌های دهانه، تغییرات فاصله اوج و حضیض و حرکات رخ‌گردی optical.",
                    "Calculates lunar phase angle, illuminated fraction, and optical libration in longitude and latitude.",
                    "محاسبه زاویه فاز ماه، درصد بخش درخشان و میزان رخ‌گردی طولی و عرضی.",
                    listOf(
                        "1. Rotate the 3D Moon globe using touch gestures.",
                        "2. Tap any named crater to view origin facts and coordinates.",
                        "3. Use the Time Machine to observe monthly phase shifts."
                    ),
                    listOf(
                        "۱. کره ۳بعدی ماه را با لمس بچرخانید.",
                        "۲. روی هر دهانه معروف ضربه بزنید تا اطلاعات و مختصات آن را ببینید.",
                        "۳. با ماشین زمان تغییرات ماهانه فازها را رصد کنید."
                    ),
                    "100% offline data and rendering.",
                    "رندر و داده‌های ۱۰۰٪ آفلاین.",
                    Icons.Default.NightlightRound
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalTutorialModal(
    topic: RedTutorialTopic,
    isFa: Boolean,
    onDismiss: () -> Unit
) {
    val steps = remember(topic) { RedTutorialData.getSteps(topic) }
    var currentStepIndex by remember { mutableIntStateOf(0) }
    val step = steps.getOrElse(currentStepIndex) { steps[0] }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(step.icon, contentDescription = null, tint = AccentPrimary)
                    Text(
                        text = if (isFa) step.titleFa else step.titleEn,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = AccentPrimary.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "${step.stepIndex}/${step.totalSteps}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Purpose Section
                Text(
                    text = if (isFa) "هدف کاربردی:" else "Purpose:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = AccentPrimary
                )
                Text(
                    text = if (isFa) step.purposeFa else step.purposeEn,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                // Scientific Background Section
                Text(
                    text = if (isFa) "مبنای علمی و ریاضی:" else "Scientific Background:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = AccentPrimary
                )
                Text(
                    text = if (isFa) step.scientificBackgroundFa else step.scientificBackgroundEn,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                // Usage Steps Section
                Text(
                    text = if (isFa) "مراحل استفاده:" else "Step-by-Step Usage:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = AccentPrimary
                )
                val usageList = if (isFa) step.usageStepsFa else step.usageStepsEn
                usageList.forEach { itemText ->
                    Text(
                        text = itemText,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )
                }

                // Offline & Performance
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = if (isFa) "قابلیت آفلاین و کارایی:" else "Offline Capabilities & Performance:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AccentSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isFa) step.offlineAndPerformanceFa else step.offlineAndPerformanceEn,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStepIndex > 0) {
                    TextButton(onClick = { currentStepIndex-- }) {
                        Text(if (isFa) "قبلی" else "Previous", color = TextSecondary)
                    }
                }
                if (currentStepIndex < steps.size - 1) {
                    Button(
                        onClick = { currentStepIndex++ },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                    ) {
                        Text(if (isFa) "بعدی" else "Next", color = Color.Black)
                    }
                } else {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                    ) {
                        Text(if (isFa) "فهمیدم" else "Got it", color = Color.Black)
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        modifier = Modifier.testTag("global_tutorial_modal")
    )
}

package com.zig.antar.ui

import com.zig.antar.config.AntarCameraMode
import com.zig.antar.config.AntarMassPreset
import com.zig.antar.config.AntarObjectMode
import com.zig.antar.config.AntarPreset
import com.zig.antar.config.AntarQualityTier

enum class AntarControlTab {
    SCENE,
    SPACETIME,
    DISK_SKY,
    VIEW_TOUCH
}

/**
 * Bilingual English & Persian (`فارسی`) copy for ZIG's Blackhole (`سیاه‌چاله زیگ`).
 */
object AntarStrings {

    fun title(fa: Boolean): String = if (fa) "سیاه‌چاله زیگ" else "ZIG's Blackhole"
    fun subtitle(fa: Boolean): String =
        if (fa) "ردیابی پرتو نسبیتی کر–شیلد" else "Kerr–Schild Null-Geodesic Ray Tracer"

    fun loadingShader(fa: Boolean): String =
        if (fa) "در حال کامپایل شیدرهای نسبیت عام…" else "Compiling general-relativistic shaders…"

    fun webglErrorTitle(fa: Boolean): String =
        if (fa) "شتاب‌دهنده گرافیکی WebGL2 در دسترس نیست" else "WebGL2 GPU Engine Unavailable"

    fun insideHorizonBadge(fa: Boolean): String =
        if (fa) "درون افق رویداد" else "INSIDE EVENT HORIZON"

    fun restartInspiral(fa: Boolean): String =
        if (fa) "شروع دوباره مارپیچ ادغام" else "Restart Inspiral"

    fun exitPlunge(fa: Boolean): String =
        if (fa) "بازگشت به مدار" else "Return to Orbit"

    fun stopTour(fa: Boolean): String =
        if (fa) "پایان تور" else "Stop Tour"

    fun resetView(fa: Boolean): String =
        if (fa) "بازنشانی نما" else "Reset View"

    fun resetAll(fa: Boolean): String =
        if (fa) "تنظیمات پیش‌فرض" else "Reset All"

    fun play(fa: Boolean): String = if (fa) "پخش" else "Play"
    fun pause(fa: Boolean): String = if (fa) "مکث" else "Pause"
    fun controls(fa: Boolean): String = if (fa) "تنظیمات" else "Controls"
    fun closeControls(fa: Boolean): String = if (fa) "بستن پنل" else "Close Panel"
    fun hideHud(fa: Boolean): String = if (fa) "پنهان کردن رابط" else "Hide HUD"
    fun showHud(fa: Boolean): String = if (fa) "نمایش رابط" else "Show HUD"

    // Tabs
    fun tabLabel(tab: AntarControlTab, fa: Boolean): String = when (tab) {
        AntarControlTab.SCENE -> if (fa) "صحنه" else "Scene"
        AntarControlTab.SPACETIME -> if (fa) "فضازمان" else "Physics"
        AntarControlTab.DISK_SKY -> if (fa) "قرص و آسمان" else "Disk & Sky"
        AntarControlTab.VIEW_TOUCH -> if (fa) "نما و لمس" else "View & Touch"
    }

    // Enum-backed localized labels
    fun objectModeLabel(mode: AntarObjectMode, fa: Boolean): String =
        if (fa) mode.labelFa else mode.labelEn

    fun cameraModeLabel(mode: AntarCameraMode, fa: Boolean): String =
        if (fa) mode.labelFa else mode.labelEn

    fun qualityLabel(q: AntarQualityTier, fa: Boolean): String =
        if (fa) q.labelFa else q.labelEn

    fun massLabel(mass: AntarMassPreset, fa: Boolean): String =
        if (fa) mass.labelFa else mass.labelEn

    fun presetTitle(preset: AntarPreset, fa: Boolean): String =
        if (fa) preset.titleFa else preset.titleEn

    fun presetSubtitle(preset: AntarPreset, fa: Boolean): String =
        if (fa) preset.subtitleFa else preset.subtitleEn

    // Section headers
    fun sectionObjectType(fa: Boolean): String = if (fa) "ساختار جرم کیهانی" else "Astrophysical System"
    fun sectionCameraMode(fa: Boolean): String = if (fa) "حالت دوربین و حرکت" else "Camera & Trajectory"
    fun sectionPresets(fa: Boolean): String = if (fa) "صحنه‌های آماده اخترفیزیکی" else "Astrophysical Presets"
    fun sectionObservables(fa: Boolean): String = if (fa) "کمیت‌های تحلیلی نسبیت عام" else "Exact Relativistic Observables"
    fun sectionKerrMetric(fa: Boolean): String = if (fa) "پارامترهای متریک کر" else "Kerr Metric Parameters"
    fun sectionBinary(fa: Boolean): String = if (fa) "پارامترهای دوتایی و امواج گرانشی" else "Binary Inspiral & GW"
    fun sectionQuasar(fa: Boolean): String = if (fa) "موتور اختروش ابرپرجرم" else "Quasar Central Engine"
    fun sectionDisk(fa: Boolean): String = if (fa) "قرص برافزایشی نوویکوف–تورن" else "Novikov–Thorne Accretion Disk"
    fun sectionSky(fa: Boolean): String = if (fa) "پس‌زمینه کیهانی و فواره نسبیتی" else "Celestial Sky & Relativistic Jet"
    fun sectionOptics(fa: Boolean): String = if (fa) "اپتیک دوربین و کیفیت رندر" else "Camera Optics & Render Quality"
    fun sectionTouch(fa: Boolean): String = if (fa) "کنترل لمسی طبیعی" else "Natural Touch Controls"

    // Observables & sliders
    fun obsHorizon(fa: Boolean): String = if (fa) "افق رویداد r+" else "Horizon r+"
    fun obsIsco(fa: Boolean): String = if (fa) "مدار پایدار ISCO" else "ISCO Radius"
    fun obsPhotonRing(fa: Boolean): String = if (fa) "مدار فوتون r_ph" else "Photon Orbit"
    fun obsTimeDilation(fa: Boolean): String = if (fa) "اتساع زمان dτ/dt" else "Time Dilation"
    fun obsBinarySep(fa: Boolean): String = if (fa) "جدایی مداری" else "Separation"
    fun obsOrbitalSpeed(fa: Boolean): String = if (fa) "تندی مداری v/c" else "Orbital v/c"
    fun obsGwFreq(fa: Boolean): String = if (fa) "بسامد موج گرانشی" else "GW Frequency"

    fun paramSpin(fa: Boolean): String = if (fa) "چرخش بی‌بعد (a/M)" else "Dimensionless Spin (a/M)"
    fun paramMass(fa: Boolean): String = if (fa) "مقیاس جرم سیاه‌چاله" else "Mass Scale"
    fun paramTimeScale(fa: Boolean): String = if (fa) "سرعت زمان شبیه‌سازی" else "Simulation Time Rate"
    fun paramPlungeSpeed(fa: Boolean): String = if (fa) "شتاب زمان سقوط آزاد" else "Free-Fall Rate"
    fun paramBeaming(fa: Boolean): String = if (fa) "تقویت دوپلر نسبیتی" else "Relativistic Doppler Beaming"
    fun paramBolometric(fa: Boolean): String = if (fa) "قفل درخشندگی بولومتریک" else "Lock Bolometric Luminosity"

    fun paramBinaryQ(fa: Boolean): String = if (fa) "نسبت جرم (q = M₂/M₁)" else "Mass Ratio (q = M₂/M₁)"
    fun paramBinarySpin1(fa: Boolean): String = if (fa) "چرخش سیاه‌چاله اول (χ₁)" else "Primary Spin (χ₁)"
    fun paramBinarySpin2(fa: Boolean): String = if (fa) "چرخش سیاه‌چاله دوم (χ₂)" else "Secondary Spin (χ₂)"
    fun paramBinarySep(fa: Boolean): String = if (fa) "جدایی آغازین (M)" else "Initial Separation (M)"
    fun paramBinaryRate(fa: Boolean): String = if (fa) "سرعت تکامل ادغام" else "Inspiral Speed"

    fun paramDiskEnabled(fa: Boolean): String = if (fa) "قرص برافزایشی فعال" else "Accretion Disk"
    fun paramThickDisk(fa: Boolean): String = if (fa) "هندسه قرص ضخیم" else "Thick Torus Geometry"
    fun paramPhysicalTemp(fa: Boolean): String = if (fa) "دمای فیزیکی از نسبت ادینگتون" else "Self-Consistent Eddington T(r)"
    fun paramEddington(fa: Boolean): String = if (fa) "نسبت برافزایش ادینگتون" else "Eddington Ratio (ṁ)"
    fun paramPeakTemp(fa: Boolean): String = if (fa) "دمای اوج قرص (کلوین)" else "Peak Temperature (K)"
    fun paramOuterRadius(fa: Boolean): String = if (fa) "شعاع بیرونی قرص (M)" else "Outer Disk Radius (M)"
    fun paramDensity(fa: Boolean): String = if (fa) "چگالی نوری پلاسما" else "Optical Density"
    fun paramTurbulence(fa: Boolean): String = if (fa) "تلاطم مگنتوهیدرودینامیکی" else "MRI Turbulence"
    fun paramHaze(fa: Boolean): String = if (fa) "غبار پراکندگی تاج" else "Coronal Haze"
    fun paramDiskBright(fa: Boolean): String = if (fa) "بهره تابش قرص" else "Disk Emission Gain"
    fun paramCorona(fa: Boolean): String = if (fa) "تاج پرتو ایکس چراغ‌پستی" else "Lamppost X-Ray Corona"
    fun paramWind(fa: Boolean): String = if (fa) "باد خط‌پهن فرابنفش" else "UV Disk Wind"
    fun paramBlr(fa: Boolean): String = if (fa) "ناحیه خطوط پهن (BLR)" else "Broad-Line Region"

    fun paramStars(fa: Boolean): String = if (fa) "میدان ستارگان و راه شیری" else "Starfield & Milky Way"
    fun paramStarBright(fa: Boolean): String = if (fa) "درخشش ستارگان همگراشده" else "Lensed Star Brightness"
    fun paramSkyBright(fa: Boolean): String = if (fa) "سحابی و غبار کهکشانی" else "Galactic Dust & Nebula"
    fun paramGrid(fa: Boolean): String = if (fa) "شبکه مختصات سماوی" else "Celestial Lensing Grid"
    fun paramJet(fa: Boolean): String = if (fa) "فواره نسبیتی بلندفورد–زنایک" else "Relativistic Polar Jet"
    fun paramEht(fa: Boolean): String = if (fa) "بازسازی تداخل‌سنجی EHT ۲۳۰GHz" else "EHT 230 GHz VLBI Filter"

    fun paramDistance(fa: Boolean): String = if (fa) "فاصله شعاعی دوربین (r/M)" else "Radial Distance (r/M)"
    fun paramInclination(fa: Boolean): String = if (fa) "زاویه میل ناظر" else "Inclination Angle"
    fun paramRoll(fa: Boolean): String = if (fa) "چرخش افق دوربین" else "Horizon Roll"
    fun paramFov(fa: Boolean): String = if (fa) "میدان دید عمودی (FOV)" else "Vertical Field of View"
    fun paramExposure(fa: Boolean): String = if (fa) "نوردهی تون‌مپینگ ACES" else "ACES Exposure"
    fun paramBloom(fa: Boolean): String = if (fa) "هاله درخشش فوتونی" else "Photon Ring Bloom"
    fun paramQuality(fa: Boolean): String = if (fa) "دقت گام انتگرال‌گیری پرتو" else "Geodesic Step Quality"
    fun paramAutoRes(fa: Boolean): String = if (fa) "تنظیم هوشمند تفکیک‌پذیری (۶۰ فریم)" else "Adaptive 60 FPS Scale"

    fun paramSensitivity(fa: Boolean): String = if (fa) "حساسیت چرخش لمسی" else "Orbit Drag Sensitivity"
    fun paramInertia(fa: Boolean): String = if (fa) "لغزش لختی پس از رهاسازی" else "Inertial Glide on Release"
    fun paramTwoFingerRoll(fa: Boolean): String = if (fa) "چرخش افق با دو انگشت" else "Two-Finger Horizon Twist"
    fun paramShowHudBadge(fa: Boolean): String = if (fa) "نمایش نوار زنده کمیت‌های فیزیکی" else "Live Telemetry Pill"

    fun gestureSummary(fa: Boolean): String = if (fa) {
        "• یک انگشت: چرخش نرم سه‌بعدی به دور سیاه‌چاله (یا نگاه به اطراف هنگام سقوط آزاد)\n" +
            "• دو انگشت: بزرگ‌نمایی، جابه‌جایی صفحه و چرخاندن زاویه افق\n" +
            "• دو ضربه پیاپی: بازگرداندن فوری سیاه‌چاله به مرکز تصویر"
    } else {
        "• 1 finger: smooth 3D arcball orbit & tilt (or head-look during free-fall)\n" +
            "• 2 fingers: pinch to zoom, drag to pan, twist to roll the horizon\n" +
            "• Double-tap: instantly re-center and level the viewport"
    }
}

package com.zig.antar.config

import android.content.Context
import com.zig.antar.physics.AntarKerrMetrics
import java.util.Locale

enum class AntarObjectMode(val jsKey: String, val labelEn: String, val labelFa: String) {
    KERR("kerr", "Kerr Black Hole", "سیاه‌چاله کر"),
    TON_618("ton618", "TON 618 Quasar", "اختروش TON 618"),
    BINARY_MERGER("merger", "Binary Merger", "ادغام دوتایی");

    companion object {
        fun fromJsKey(key: String?): AntarObjectMode =
            entries.firstOrNull { it.jsKey == key } ?: KERR
    }
}

enum class AntarCameraMode(val jsKey: String, val labelEn: String, val labelFa: String) {
    ORBIT("orbit", "Orbit", "مدار دستی"),
    CINEMATIC("cinematic", "Cinematic", "سینمایی"),
    PLUNGE("plunge", "Plunge", "سقوط آزاد"),
    TOUR("tour", "Tour", "گشت خودکار");

    companion object {
        fun fromJsKey(key: String?): AntarCameraMode =
            entries.firstOrNull { it.jsKey == key } ?: ORBIT
    }
}

enum class AntarMassPreset(
    val jsKey: String,
    val solarMasses: Double,
    val labelEn: String,
    val labelFa: String
) {
    SGR_A("sgra", 4.3e6, "Sgr A* (4.3×10⁶ M☉)", "کمان A* (۴٫۳×۱۰⁶ M☉)"),
    M87("m87", 6.5e9, "M87* (6.5×10⁹ M☉)", "M87* (۶٫۵×۱۰⁹ M☉)"),
    SUPERMASSIVE_1E8("smbh", 1.0e8, "Supermassive (10⁸ M☉)", "کلان‌جرم (۱۰⁸ M☉)"),
    STELLAR_10("stellar", 10.0, "Stellar (10 M☉)", "ستاره‌ای (۱۰ M☉)"),
    TON_618("ton618", 4.07e10, "TON 618 (4.07×10¹⁰ M☉)", "TON 618 (۴٫۰۷×۱۰¹⁰ M☉)");

    companion object {
        fun fromJsKey(key: String?): AntarMassPreset =
            entries.firstOrNull { it.jsKey == key } ?: SGR_A
    }
}

enum class AntarQualityTier(
    val jsKey: String,
    val maxSteps: Int,
    val maxScale: Double,
    val labelEn: String,
    val labelFa: String
) {
    LOW("dusuk", 220, 0.50, "Low", "پایین"),
    MEDIUM("orta", 320, 0.70, "Balanced", "متعادل"),
    HIGH("yuksek", 460, 0.85, "High", "بالا"),
    ULTRA("ultra", 800, 1.00, "Ultra", "فوق‌العاده");

    companion object {
        fun fromJsKey(key: String?): AntarQualityTier =
            entries.firstOrNull { it.jsKey == key } ?: HIGH
    }
}

enum class AntarBackdropStyle(val labelEn: String, val labelFa: String) {
    DEEP_VOID("Deep Space Void", "خلأ تاریک کیهانی"),
    THEME_AMBIENT("App Theme Glow", "همگام با پوسته برنامه")
}

enum class AntarPanelTab(val labelEn: String, val labelFa: String) {
    SCENE("Scene", "صحنه"),
    PHYSICS("Spacetime", "فضازمان"),
    DISK_SKY("Disk & Sky", "قرص و آسمان"),
    VIEW_TOUCH("View & Touch", "تصویر و لمس")
}

enum class AntarPreset(
    val jsKey: String,
    val titleEn: String,
    val titleFa: String,
    val subtitleEn: String,
    val subtitleFa: String
) {
    PHYSICAL(
        jsKey = "fiziksel",
        titleEn = "Physical Kerr",
        titleFa = "کر فیزیکی",
        subtitleEn = "Novikov-Thorne disk & Doppler beaming",
        subtitleFa = "قرص نوویکوف-تورن و درخشش دوپلری"
    ),
    THICK_CINEMA(
        jsKey = "thickdisk",
        titleEn = "Thick Disk",
        titleFa = "قرص ضخیم",
        subtitleEn = "Symmetric cinematic accretion halo",
        subtitleFa = "هاله برافزایشی متقارن سینمایی"
    ),
    POLAR(
        jsKey = "kutup",
        titleEn = "Polar View",
        titleFa = "نمای قطبی",
        subtitleEn = "Face-on spiral plasma & photon ring",
        subtitleFa = "نمای عمودی حلقه فوتونی و پلاسما"
    ),
    EDGE_ON(
        jsKey = "kenar",
        titleEn = "Razor Edge",
        titleFa = "لبه‌به‌لبه",
        subtitleEn = "Equatorial lensed upper & lower arches",
        subtitleFa = "قوس‌های همگراشده در استوای سیاه‌چاله"
    ),
    NEAR_HORIZON(
        jsKey = "yakin",
        titleEn = "Near Horizon",
        titleFa = "نزدیک افق",
        subtitleEn = "7.5 M wide-angle relativistic immersion",
        subtitleFa = "فاصله ۷٫۵M با میدان دید باز نسبیتی"
    ),
    EINSTEIN_LENS(
        jsKey = "lens",
        titleEn = "Einstein Lens",
        titleFa = "عدسی گرانشی",
        subtitleEn = "Coordinate sky grid & pure photon sphere",
        subtitleFa = "شبکه مختصات آسمان و کره فوتونی خالص"
    ),
    EHT_M87(
        jsKey = "m87",
        titleEn = "M87* VLBI",
        titleFa = "تصویر M87*",
        subtitleEn = "1.3 mm Event Horizon Telescope beam",
        subtitleFa = "شبیه‌سازی پرتو تلسکوپ افق رویداد"
    ),
    SYNCHROTRON_JET(
        jsKey = "jet",
        titleEn = "Relativistic Jet",
        titleFa = "فوران نسبیتی",
        subtitleEn = "Blandford-Znajek helical polar outflow",
        subtitleFa = "فوران مارپیچی سنکروترونی قطب‌ها"
    ),
    TON_618_QUASAR(
        jsKey = "ton618",
        titleEn = "TON 618 Quasar",
        titleFa = "اختروش TON 618",
        subtitleEn = "Corona, UV disk wind & broad-line clouds",
        subtitleFa = "تاج پرتو ایکس، باد قرص و ابرهای BLR"
    ),
    BINARY_INSPIRAL(
        jsKey = "carpisma",
        titleEn = "Binary Merger",
        titleFa = "ادغام دوتایی",
        subtitleEn = "Inspiral, horizon coalescence & ringdown",
        subtitleFa = "مارپیچ درونی، ادغام افق‌ها و فرونشست"
    );

    fun applyTo(current: AntarSimulationSettings = AntarSimulationSettings.DEFAULT): AntarSimulationSettings {
        val base = when (this) {
            PHYSICAL -> current.copy(
                objectMode = AntarObjectMode.KERR,
                massPreset = AntarMassPreset.SGR_A,
                spin = 0.94,
                inclinationDeg = 79.0,
                distanceM = 18.5,
                fovDeg = 48.0,
                rollDeg = 0.0,
                diskEnabled = true,
                peakTemperatureK = 4500.0,
                outerRadiusM = 25.0,
                opticalDensity = 1.65,
                turbulence = 0.55,
                coronalHaze = 0.35,
                diskBrightness = 1.25,
                beamingStrength = 1.0,
                bolometricLocked = true,
                thickDiskMode = false,
                lensGridEnabled = false,
                ehtFilterEnabled = false,
                relativisticJetEnabled = false,
                starsEnabled = true,
                starBrightness = 0.95,
                skyBrightness = 0.35,
                exposure = 1.38,
                bloomStrength = 1.15,
                physicalDiskTemp = false,
                xrayCorona = 0.0,
                uvDiskWind = 0.0,
                broadLineRegion = 0.0
            )
            THICK_CINEMA -> current.copy(
                objectMode = AntarObjectMode.KERR,
                massPreset = AntarMassPreset.SUPERMASSIVE_1E8,
                spin = 0.99,
                inclinationDeg = 86.5,
                distanceM = 22.0,
                fovDeg = 46.0,
                rollDeg = 0.0,
                diskEnabled = true,
                peakTemperatureK = 4200.0,
                outerRadiusM = 18.0,
                opticalDensity = 1.80,
                turbulence = 0.65,
                coronalHaze = 0.60,
                diskBrightness = 1.20,
                beamingStrength = 0.0,
                bolometricLocked = true,
                thickDiskMode = true,
                lensGridEnabled = false,
                ehtFilterEnabled = false,
                relativisticJetEnabled = false,
                exposure = 1.22,
                bloomStrength = 1.35,
                physicalDiskTemp = false,
                xrayCorona = 0.0,
                uvDiskWind = 0.0,
                broadLineRegion = 0.0
            )
            POLAR -> current.copy(
                rollDeg = 0.0,
                inclinationDeg = 6.0,
                distanceM = 30.0,
                fovDeg = 46.0,
                diskEnabled = true,
                lensGridEnabled = false,
                ehtFilterEnabled = false
            )
            EDGE_ON -> current.copy(
                rollDeg = 0.0,
                inclinationDeg = 89.5,
                distanceM = 17.0,
                fovDeg = 55.0,
                diskEnabled = true,
                lensGridEnabled = false,
                ehtFilterEnabled = false
            )
            NEAR_HORIZON -> current.copy(
                rollDeg = 0.0,
                inclinationDeg = 84.0,
                distanceM = 7.5,
                fovDeg = 75.0,
                diskEnabled = true,
                lensGridEnabled = false,
                ehtFilterEnabled = false
            )
            EINSTEIN_LENS -> current.copy(
                objectMode = AntarObjectMode.KERR,
                rollDeg = 0.0,
                diskEnabled = false,
                lensGridEnabled = true,
                starsEnabled = true,
                starBrightness = 1.35,
                skyBrightness = 0.75,
                inclinationDeg = 90.0,
                distanceM = 16.0,
                fovDeg = 70.0,
                ehtFilterEnabled = false,
                relativisticJetEnabled = false,
                xrayCorona = 0.0,
                uvDiskWind = 0.0,
                broadLineRegion = 0.0
            )
            EHT_M87 -> current.copy(
                objectMode = AntarObjectMode.KERR,
                massPreset = AntarMassPreset.M87,
                spin = 0.90,
                inclinationDeg = 17.0,
                distanceM = 70.0,
                fovDeg = 22.0,
                rollDeg = 90.0,
                outerRadiusM = 10.0,
                ehtFilterEnabled = true,
                diskEnabled = true,
                lensGridEnabled = false,
                beamingStrength = 1.0,
                bolometricLocked = true,
                thickDiskMode = false,
                peakTemperatureK = 6000.0,
                relativisticJetEnabled = false,
                xrayCorona = 0.0,
                uvDiskWind = 0.0,
                broadLineRegion = 0.0
            )
            SYNCHROTRON_JET -> current.copy(
                objectMode = AntarObjectMode.KERR,
                rollDeg = 0.0,
                relativisticJetEnabled = true,
                inclinationDeg = 64.0,
                distanceM = 48.0,
                fovDeg = 56.0,
                diskEnabled = true,
                lensGridEnabled = false,
                ehtFilterEnabled = false
            )
            TON_618_QUASAR -> current.copy(
                objectMode = AntarObjectMode.TON_618,
                massPreset = AntarMassPreset.TON_618,
                spin = 0.90,
                physicalDiskTemp = true,
                eddingtonRatio = 0.078,
                xrayCorona = 1.0,
                uvDiskWind = 1.0,
                broadLineRegion = 1.0,
                relativisticJetEnabled = true,
                diskEnabled = true,
                outerRadiusM = 40.0,
                opticalDensity = 1.10,
                turbulence = 0.85,
                coronalHaze = 0.60,
                diskBrightness = 1.0,
                beamingStrength = 1.0,
                bolometricLocked = true,
                thickDiskMode = false,
                lensGridEnabled = false,
                ehtFilterEnabled = false,
                starsEnabled = true,
                starBrightness = 0.70,
                skyBrightness = 0.55,
                exposure = 1.30,
                bloomStrength = 1.40,
                inclinationDeg = 68.0,
                distanceM = 42.0,
                fovDeg = 56.0,
                rollDeg = 0.0
            )
            BINARY_INSPIRAL -> current.copy(
                objectMode = AntarObjectMode.BINARY_MERGER,
                cameraMode = if (current.cameraMode == AntarCameraMode.PLUNGE) AntarCameraMode.ORBIT else current.cameraMode,
                massPreset = AntarMassPreset.TON_618,
                spin = 0.90,
                physicalDiskTemp = false,
                xrayCorona = 0.0,
                uvDiskWind = 0.0,
                broadLineRegion = 0.0,
                relativisticJetEnabled = false,
                diskEnabled = true,
                peakTemperatureK = 4500.0,
                outerRadiusM = 70.0,
                opticalDensity = 1.0,
                turbulence = 0.85,
                coronalHaze = 0.0,
                diskBrightness = 1.0,
                beamingStrength = 1.0,
                bolometricLocked = true,
                thickDiskMode = false,
                lensGridEnabled = false,
                ehtFilterEnabled = false,
                starsEnabled = true,
                starBrightness = 0.75,
                skyBrightness = 0.55,
                exposure = 1.40,
                bloomStrength = 1.30,
                inclinationDeg = 72.0,
                distanceM = 75.0,
                fovDeg = 50.0,
                rollDeg = 0.0,
                binaryMassRatio = 0.50,
                binarySpin1 = 0.90,
                binarySpin2 = 0.70,
                binarySeparationM = 12.0,
                binaryPlaybackSpeed = 1.0,
                paused = false
            )
        }
        return base.copy(
            activePreset = this,
            cameraMode = if (base.objectMode == AntarObjectMode.BINARY_MERGER && base.cameraMode == AntarCameraMode.PLUNGE) {
                AntarCameraMode.ORBIT
            } else {
                base.cameraMode
            }
        ).normalize()
    }
}

data class AntarSimulationSettings(
    val activePreset: AntarPreset? = AntarPreset.PHYSICAL,
    val objectMode: AntarObjectMode = AntarObjectMode.KERR,
    val cameraMode: AntarCameraMode = AntarCameraMode.ORBIT,
    val spin: Double = 0.94,
    val massPreset: AntarMassPreset = AntarMassPreset.SGR_A,
    val azimuthDeg: Double = 38.0,
    val inclinationDeg: Double = 79.0,
    val distanceM: Double = 18.5,
    val fovDeg: Double = 48.0,
    val rollDeg: Double = 0.0,
    val plungeSpeed: Double = 1.0,
    val diskEnabled: Boolean = true,
    val peakTemperatureK: Double = 4500.0,
    val outerRadiusM: Double = 25.0,
    val opticalDensity: Double = 1.65,
    val turbulence: Double = 0.55,
    val coronalHaze: Double = 0.35,
    val diskBrightness: Double = 1.25,
    val timeScale: Double = 9.0,
    val paused: Boolean = false,
    val beamingStrength: Double = 1.0,
    val bolometricLocked: Boolean = true,
    val thickDiskMode: Boolean = false,
    val eddingtonRatio: Double = 0.078,
    val physicalDiskTemp: Boolean = false,
    val xrayCorona: Double = 0.0,
    val uvDiskWind: Double = 0.0,
    val broadLineRegion: Double = 0.0,
    val binaryMassRatio: Double = 0.50,
    val binarySpin1: Double = 0.90,
    val binarySpin2: Double = 0.70,
    val binarySeparationM: Double = 14.0,
    val binaryPlaybackSpeed: Double = 1.0,
    val starsEnabled: Boolean = true,
    val starBrightness: Double = 0.95,
    val skyBrightness: Double = 0.35,
    val lensGridEnabled: Boolean = false,
    val relativisticJetEnabled: Boolean = false,
    val backdropStyle: AntarBackdropStyle = AntarBackdropStyle.DEEP_VOID,
    val exposure: Double = 1.38,
    val bloomStrength: Double = 1.15,
    val quality: AntarQualityTier = AntarQualityTier.HIGH,
    val adaptiveResolution: Boolean = true,
    val ehtFilterEnabled: Boolean = false,
    val showTelemetryHud: Boolean = true,
    val gestureSensitivity: Double = 1.0,
    val inertialGlide: Boolean = true,
    val twoFingerRollEnabled: Boolean = true
) {
    companion object {
        val DEFAULT: AntarSimulationSettings = AntarSimulationSettings()
    }

    /** Switches object mode (`Kerr` / `TON 618` / `Binary Merger`) with sensible mode defaults. */
    fun withObjectMode(target: AntarObjectMode): AntarSimulationSettings {
        if (target == objectMode) return this
        return when (target) {
            AntarObjectMode.KERR -> copy(
                objectMode = AntarObjectMode.KERR,
                physicalDiskTemp = false,
                xrayCorona = 0.0,
                uvDiskWind = 0.0,
                broadLineRegion = 0.0,
                outerRadiusM = outerRadiusM.coerceIn(6.0, 45.0),
                distanceM = distanceM.coerceIn(3.6, 80.0)
            ).normalize()
            AntarObjectMode.TON_618 -> AntarPreset.TON_618_QUASAR.applyTo(this)
            AntarObjectMode.BINARY_MERGER -> AntarPreset.BINARY_INSPIRAL.applyTo(this)
        }
    }

    /** Resets camera view framing without altering spacetime, disk, sky, or gesture preferences. */
    fun resetCameraView(): AntarSimulationSettings {
        val defaultDist = when (objectMode) {
            AntarObjectMode.KERR -> 18.5
            AntarObjectMode.TON_618 -> 42.0
            AntarObjectMode.BINARY_MERGER -> 75.0
        }
        val defaultIncl = when (objectMode) {
            AntarObjectMode.KERR -> 79.0
            AntarObjectMode.TON_618 -> 68.0
            AntarObjectMode.BINARY_MERGER -> 72.0
        }
        val defaultFov = when (objectMode) {
            AntarObjectMode.KERR -> 48.0
            AntarObjectMode.TON_618 -> 56.0
            AntarObjectMode.BINARY_MERGER -> 50.0
        }
        return copy(
            cameraMode = AntarCameraMode.ORBIT,
            azimuthDeg = 38.0,
            inclinationDeg = defaultIncl,
            distanceM = defaultDist,
            fovDeg = defaultFov,
            rollDeg = 0.0,
            paused = false
        ).normalize()
    }

    fun normalize(): AntarSimulationSettings {
        val clampedSpin = spin.coerceIn(0.0, 0.998)
        val clampedSep = binarySeparationM.coerceIn(8.0, 30.0)
        val isMerger = objectMode == AntarObjectMode.BINARY_MERGER
        val minDist = AntarKerrMetrics.minOrbitDistanceM(clampedSpin, isMerger, clampedSep)
        val safeCameraMode = if (isMerger && cameraMode == AntarCameraMode.PLUNGE) {
            AntarCameraMode.ORBIT
        } else {
            cameraMode
        }
        var wrappedAz = azimuthDeg % 360.0
        if (wrappedAz > 180.0) wrappedAz -= 360.0
        if (wrappedAz < -180.0) wrappedAz += 360.0
        var wrappedRoll = rollDeg % 360.0
        if (wrappedRoll > 180.0) wrappedRoll -= 360.0
        if (wrappedRoll < -180.0) wrappedRoll += 360.0

        return copy(
            cameraMode = safeCameraMode,
            spin = clampedSpin,
            azimuthDeg = if (wrappedAz.isFinite()) wrappedAz else 38.0,
            inclinationDeg = inclinationDeg.coerceIn(1.5, 178.5),
            distanceM = distanceM.coerceIn(minDist, 220.0),
            fovDeg = fovDeg.coerceIn(15.0, 110.0),
            rollDeg = if (wrappedRoll.isFinite()) wrappedRoll else 0.0,
            plungeSpeed = plungeSpeed.coerceIn(0.25, 3.0),
            peakTemperatureK = peakTemperatureK.coerceIn(2_500.0, 100_000.0),
            outerRadiusM = outerRadiusM.coerceIn(6.0, 80.0),
            opticalDensity = opticalDensity.coerceIn(0.10, 5.0),
            turbulence = turbulence.coerceIn(0.0, 1.0),
            coronalHaze = coronalHaze.coerceIn(0.0, 2.0),
            diskBrightness = diskBrightness.coerceIn(0.10, 6.0),
            timeScale = timeScale.coerceIn(0.0, 60.0),
            beamingStrength = beamingStrength.coerceIn(0.0, 1.0),
            eddingtonRatio = eddingtonRatio.coerceIn(0.001, 1.0),
            xrayCorona = xrayCorona.coerceIn(0.0, 3.0),
            uvDiskWind = uvDiskWind.coerceIn(0.0, 3.0),
            broadLineRegion = broadLineRegion.coerceIn(0.0, 3.0),
            binaryMassRatio = binaryMassRatio.coerceIn(0.10, 1.0),
            binarySpin1 = binarySpin1.coerceIn(0.0, 0.99),
            binarySpin2 = binarySpin2.coerceIn(0.0, 0.99),
            binarySeparationM = clampedSep,
            binaryPlaybackSpeed = binaryPlaybackSpeed.coerceIn(0.10, 5.0),
            starBrightness = starBrightness.coerceIn(0.0, 4.0),
            skyBrightness = skyBrightness.coerceIn(0.0, 4.0),
            exposure = exposure.coerceIn(0.10, 6.0),
            bloomStrength = bloomStrength.coerceIn(0.0, 3.0),
            gestureSensitivity = gestureSensitivity.coerceIn(0.50, 2.0)
        )
    }

    /** Encodes initial query string parameters for the WebGL2 host URL. */
    fun toInitialQueryString(): String {
        val s = normalize()
        val f = { v: Double, d: Int -> String.format(Locale.US, "%.${d}f", v) }
        val b = { v: Boolean -> if (v) "1" else "0" }
        val bootMode = when (s.cameraMode) {
            AntarCameraMode.PLUNGE, AntarCameraMode.TOUR -> AntarCameraMode.ORBIT.jsKey
            else -> s.cameraMode.jsKey
        }
        return buildList {
            add("transparent=1")
            add("ui=0")
            add("panel=0")
            add("diagram=0")
            add("lang=en")
            add("object=${s.objectMode.jsKey}")
            add("mode=$bootMode")
            add("spin=${f(s.spin, 4)}")
            add("mass=${s.massPreset.jsKey}")
            add("az=${f(s.azimuthDeg, 2)}")
            add("incl=${f(s.inclinationDeg, 2)}")
            add("dist=${f(s.distanceM, 2)}")
            add("fov=${f(s.fovDeg, 1)}")
            add("roll=${f(s.rollDeg, 1)}")
            add("plungeSpeed=${f(s.plungeSpeed, 2)}")
            add("disk=${b(s.diskEnabled)}")
            add("temp=${f(s.peakTemperatureK, 0)}")
            add("rout=${f(s.outerRadiusM, 1)}")
            add("density=${f(s.opticalDensity, 2)}")
            add("turb=${f(s.turbulence, 2)}")
            add("haze=${f(s.coronalHaze, 2)}")
            add("bright=${f(s.diskBrightness, 2)}")
            add("timeScale=${f(s.timeScale, 1)}")
            add("paused=${b(s.paused)}")
            add("beaming=${f(s.beamingStrength, 2)}")
            add("bolo=${b(s.bolometricLocked)}")
            add("interstellar=${b(s.thickDiskMode)}")
            add("edd=${f(s.eddingtonRatio, 4)}")
            add("physT=${b(s.physicalDiskTemp)}")
            add("corona=${f(s.xrayCorona, 2)}")
            add("wind=${f(s.uvDiskWind, 2)}")
            add("blr=${f(s.broadLineRegion, 2)}")
            add("bq=${f(s.binaryMassRatio, 2)}")
            add("bchi1=${f(s.binarySpin1, 3)}")
            add("bchi2=${f(s.binarySpin2, 3)}")
            add("bsep=${f(s.binarySeparationM, 1)}")
            add("bspeed=${f(s.binaryPlaybackSpeed, 2)}")
            add("stars=${b(s.starsEnabled)}")
            add("starBright=${f(s.starBrightness, 2)}")
            add("skyBright=${f(s.skyBrightness, 2)}")
            add("grid=${b(s.lensGridEnabled)}")
            add("jet=${b(s.relativisticJetEnabled)}")
            add("exposure=${f(s.exposure, 2)}")
            add("bloom=${f(s.bloomStrength, 2)}")
            add("quality=${s.quality.jsKey}")
            add("autoRes=${b(s.adaptiveResolution)}")
            add("eht=${b(s.ehtFilterEnabled)}")
        }.joinToString("&")
    }
}

/**
 * Live telemetry reported back from the WebGL2 Kerr geodesic engine.
 */
data class AntarTelemetry(
    val engineReady: Boolean = false,
    val errorMessage: String? = null,
    val fps: Int = 60,
    val renderScale: Double = 0.70,
    val traceWidth: Int = 0,
    val traceHeight: Int = 0,
    val cameraDistanceM: Double = 18.5,
    val cameraInclinationDeg: Double = 79.0,
    val cameraAzimuthDeg: Double = 38.0,
    val plungeActive: Boolean = false,
    val plungeRadiusM: Double = 18.5,
    val plungeProperTimeM: Double = 0.0,
    val insideHorizon: Boolean = false,
    val tourActive: Boolean = false,
    val binarySeparationM: Double = 14.0,
    val binaryVelocityC: Double = 0.0,
    val binaryGwFreqHz: Double = 0.0,
    val binaryTimeToMergerSec: Double = 0.0,
    val binaryMerged: Boolean = false
)

object AntarSettingsStore {
    private const val PREFS_NAME = "antar_blackhole_prefs_v1"

    fun encodeToMap(settings: AntarSimulationSettings): Map<String, String> {
        val s = settings.normalize()
        return mapOf(
            "activePreset" to (s.activePreset?.name ?: ""),
            "objectMode" to s.objectMode.name,
            "cameraMode" to if (s.cameraMode == AntarCameraMode.PLUNGE) AntarCameraMode.ORBIT.name else s.cameraMode.name,
            "spin" to s.spin.toString(),
            "massPreset" to s.massPreset.name,
            "azimuthDeg" to s.azimuthDeg.toString(),
            "inclinationDeg" to s.inclinationDeg.toString(),
            "distanceM" to s.distanceM.toString(),
            "fovDeg" to s.fovDeg.toString(),
            "rollDeg" to s.rollDeg.toString(),
            "plungeSpeed" to s.plungeSpeed.toString(),
            "diskEnabled" to s.diskEnabled.toString(),
            "peakTemperatureK" to s.peakTemperatureK.toString(),
            "outerRadiusM" to s.outerRadiusM.toString(),
            "opticalDensity" to s.opticalDensity.toString(),
            "turbulence" to s.turbulence.toString(),
            "coronalHaze" to s.coronalHaze.toString(),
            "diskBrightness" to s.diskBrightness.toString(),
            "timeScale" to s.timeScale.toString(),
            "paused" to s.paused.toString(),
            "beamingStrength" to s.beamingStrength.toString(),
            "bolometricLocked" to s.bolometricLocked.toString(),
            "thickDiskMode" to s.thickDiskMode.toString(),
            "eddingtonRatio" to s.eddingtonRatio.toString(),
            "physicalDiskTemp" to s.physicalDiskTemp.toString(),
            "xrayCorona" to s.xrayCorona.toString(),
            "uvDiskWind" to s.uvDiskWind.toString(),
            "broadLineRegion" to s.broadLineRegion.toString(),
            "binaryMassRatio" to s.binaryMassRatio.toString(),
            "binarySpin1" to s.binarySpin1.toString(),
            "binarySpin2" to s.binarySpin2.toString(),
            "binarySeparationM" to s.binarySeparationM.toString(),
            "binaryPlaybackSpeed" to s.binaryPlaybackSpeed.toString(),
            "starsEnabled" to s.starsEnabled.toString(),
            "starBrightness" to s.starBrightness.toString(),
            "skyBrightness" to s.skyBrightness.toString(),
            "lensGridEnabled" to s.lensGridEnabled.toString(),
            "relativisticJetEnabled" to s.relativisticJetEnabled.toString(),
            "backdropStyle" to s.backdropStyle.name,
            "exposure" to s.exposure.toString(),
            "bloomStrength" to s.bloomStrength.toString(),
            "quality" to s.quality.name,
            "adaptiveResolution" to s.adaptiveResolution.toString(),
            "ehtFilterEnabled" to s.ehtFilterEnabled.toString(),
            "showTelemetryHud" to s.showTelemetryHud.toString(),
            "gestureSensitivity" to s.gestureSensitivity.toString(),
            "inertialGlide" to s.inertialGlide.toString(),
            "twoFingerRollEnabled" to s.twoFingerRollEnabled.toString()
        )
    }

    fun decodeFromMap(map: Map<String, String>): AntarSimulationSettings {
        val d = AntarSimulationSettings()
        fun dbl(key: String, fallback: Double): Double =
            map[key]?.toDoubleOrNull()?.takeIf { it.isFinite() } ?: fallback
        fun bool(key: String, fallback: Boolean): Boolean =
            map[key]?.toBooleanStrictOrNull() ?: fallback

        return AntarSimulationSettings(
            activePreset = map["activePreset"]?.takeIf { it.isNotBlank() }?.let { runCatching { AntarPreset.valueOf(it) }.getOrNull() },
            objectMode = map["objectMode"]?.let { runCatching { AntarObjectMode.valueOf(it) }.getOrNull() } ?: d.objectMode,
            cameraMode = map["cameraMode"]?.let { runCatching { AntarCameraMode.valueOf(it) }.getOrNull() } ?: d.cameraMode,
            spin = dbl("spin", d.spin),
            massPreset = map["massPreset"]?.let { runCatching { AntarMassPreset.valueOf(it) }.getOrNull() } ?: d.massPreset,
            azimuthDeg = dbl("azimuthDeg", d.azimuthDeg),
            inclinationDeg = dbl("inclinationDeg", d.inclinationDeg),
            distanceM = dbl("distanceM", d.distanceM),
            fovDeg = dbl("fovDeg", d.fovDeg),
            rollDeg = dbl("rollDeg", d.rollDeg),
            plungeSpeed = dbl("plungeSpeed", d.plungeSpeed),
            diskEnabled = bool("diskEnabled", d.diskEnabled),
            peakTemperatureK = dbl("peakTemperatureK", d.peakTemperatureK),
            outerRadiusM = dbl("outerRadiusM", d.outerRadiusM),
            opticalDensity = dbl("opticalDensity", d.opticalDensity),
            turbulence = dbl("turbulence", d.turbulence),
            coronalHaze = dbl("coronalHaze", d.coronalHaze),
            diskBrightness = dbl("diskBrightness", d.diskBrightness),
            timeScale = dbl("timeScale", d.timeScale),
            paused = bool("paused", d.paused),
            beamingStrength = dbl("beamingStrength", d.beamingStrength),
            bolometricLocked = bool("bolometricLocked", d.bolometricLocked),
            thickDiskMode = bool("thickDiskMode", d.thickDiskMode),
            eddingtonRatio = dbl("eddingtonRatio", d.eddingtonRatio),
            physicalDiskTemp = bool("physicalDiskTemp", d.physicalDiskTemp),
            xrayCorona = dbl("xrayCorona", d.xrayCorona),
            uvDiskWind = dbl("uvDiskWind", d.uvDiskWind),
            broadLineRegion = dbl("broadLineRegion", d.broadLineRegion),
            binaryMassRatio = dbl("binaryMassRatio", d.binaryMassRatio),
            binarySpin1 = dbl("binarySpin1", d.binarySpin1),
            binarySpin2 = dbl("binarySpin2", d.binarySpin2),
            binarySeparationM = dbl("binarySeparationM", d.binarySeparationM),
            binaryPlaybackSpeed = dbl("binaryPlaybackSpeed", d.binaryPlaybackSpeed),
            starsEnabled = bool("starsEnabled", d.starsEnabled),
            starBrightness = dbl("starBrightness", d.starBrightness),
            skyBrightness = dbl("skyBrightness", d.skyBrightness),
            lensGridEnabled = bool("lensGridEnabled", d.lensGridEnabled),
            relativisticJetEnabled = bool("relativisticJetEnabled", d.relativisticJetEnabled),
            backdropStyle = map["backdropStyle"]?.let { runCatching { AntarBackdropStyle.valueOf(it) }.getOrNull() } ?: d.backdropStyle,
            exposure = dbl("exposure", d.exposure),
            bloomStrength = dbl("bloomStrength", d.bloomStrength),
            quality = map["quality"]?.let { runCatching { AntarQualityTier.valueOf(it) }.getOrNull() } ?: d.quality,
            adaptiveResolution = bool("adaptiveResolution", d.adaptiveResolution),
            ehtFilterEnabled = bool("ehtFilterEnabled", d.ehtFilterEnabled),
            showTelemetryHud = bool("showTelemetryHud", d.showTelemetryHud),
            gestureSensitivity = dbl("gestureSensitivity", d.gestureSensitivity),
            inertialGlide = bool("inertialGlide", d.inertialGlide),
            twoFingerRollEnabled = bool("twoFingerRollEnabled", d.twoFingerRollEnabled)
        ).normalize()
    }

    fun load(context: Context): AntarSimulationSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.all.mapNotNull { (k, v) -> (v as? String)?.let { k to it } }.toMap()
        return if (raw.isEmpty()) AntarSimulationSettings() else decodeFromMap(raw)
    }

    fun save(context: Context, settings: AntarSimulationSettings) {
        val editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        for ((k, v) in encodeToMap(settings)) {
            editor.putString(k, v)
        }
        editor.apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }
}

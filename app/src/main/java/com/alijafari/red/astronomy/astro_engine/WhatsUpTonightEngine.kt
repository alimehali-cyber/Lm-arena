package com.alijafari.red.astronomy.astro_engine

import com.alijafari.red.astronomy.data.catalog.MeteorShowerCatalog
import com.alijafari.red.astronomy.domain.CelestialObject
import com.alijafari.red.astronomy.domain.ObjectType
import com.alijafari.red.astronomy.util.toPersianDigits
import java.util.Calendar

object WhatsUpTonightEngine {

    enum class EventVisibilityStatus {
        OPTIMAL,     // Bright / Highly Visible locally
        GOOD,        // Visible under decent conditions
        MARGINAL,    // Low altitude / Requires equipment
        NOT_VISIBLE  // Not visible locally
    }

    data class TonightEvent(
        val id: String,
        val icon: String,
        val titleEn: String,
        val titleFa: String,
        val explanationEn: String,
        val explanationFa: String,
        val timeOrDateStrEn: String,
        val timeOrDateStrFa: String,
        val visibilityStatus: EventVisibilityStatus,
        val visibilityTextEn: String,
        val visibilityTextFa: String,
        val importanceScore: Int, // Higher = ranked first
        val targetObject: CelestialObject? = null
    )

    fun calculateTonightEvents(
        jd: Double = TimeEngine.getJulianDate(),
        userLatDeg: Double,
        userLonDeg: Double,
        isFa: Boolean
    ): List<TonightEvent> {
        val events = mutableListOf<TonightEvent>()
        val nowMs = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        val gmstDeg = TimeEngine.getGMST(jd) * 15.0

        // 1. Check Upcoming Solar/Lunar Eclipses
        val (solarEclipse, lunarEclipse) = EclipseEngine.getNextEclipses(nowMs, userLatDeg, userLonDeg)
        val solarDaysDiff = (solarEclipse.event.dateUtcMs - nowMs) / 86400000L
        if (solarDaysDiff in 0..30) {
            val status = if (solarEclipse.isLocallyVisible) EventVisibilityStatus.OPTIMAL else EventVisibilityStatus.NOT_VISIBLE
            val score = if (solarEclipse.isLocallyVisible) 1000 else 600
            events.add(
                TonightEvent(
                    id = solarEclipse.event.id,
                    icon = "☀️",
                    titleEn = solarEclipse.event.nameEn,
                    titleFa = solarEclipse.event.nameFa,
                    explanationEn = solarEclipse.event.descriptionEn,
                    explanationFa = solarEclipse.event.descriptionFa,
                    timeOrDateStrEn = solarEclipse.formattedDateEn,
                    timeOrDateStrFa = solarEclipse.formattedDateFa,
                    visibilityStatus = status,
                    visibilityTextEn = solarEclipse.localVisibilityTextEn,
                    visibilityTextFa = solarEclipse.localVisibilityTextFa,
                    importanceScore = score
                )
            )
        }
        val lunarDaysDiff = (lunarEclipse.event.dateUtcMs - nowMs) / 86400000L
        if (lunarDaysDiff in 0..30) {
            val status = if (lunarEclipse.isLocallyVisible) EventVisibilityStatus.OPTIMAL else EventVisibilityStatus.NOT_VISIBLE
            val score = if (lunarEclipse.isLocallyVisible) 980 else 580
            events.add(
                TonightEvent(
                    id = lunarEclipse.event.id,
                    icon = "🌕",
                    titleEn = lunarEclipse.event.nameEn,
                    titleFa = lunarEclipse.event.nameFa,
                    explanationEn = lunarEclipse.event.descriptionEn,
                    explanationFa = lunarEclipse.event.descriptionFa,
                    timeOrDateStrEn = lunarEclipse.formattedDateEn,
                    timeOrDateStrFa = lunarEclipse.formattedDateFa,
                    visibilityStatus = status,
                    visibilityTextEn = lunarEclipse.localVisibilityTextEn,
                    visibilityTextFa = lunarEclipse.localVisibilityTextFa,
                    importanceScore = score
                )
            )
        }

        // 2. Active / Peak Meteor Showers
        val showers = MeteorShowerCatalog.getMeteorShowers()
        for (shower in showers) {
            val (peakDoy, nameEn, nameFa) = when (shower.id) {
                "shower_perseids" -> Triple(225, "Perseids Meteor Shower Peak", "اوج بارش شهابی برساوشی")
                "shower_geminids" -> Triple(348, "Geminids Meteor Shower Peak", "اوج بارش شهابی دوپیکری")
                "shower_quadrantids" -> Triple(4, "Quadrantids Meteor Shower Peak", "اوج بارش شهابی ربعی")
                "shower_lyrids" -> Triple(113, "Lyrids Meteor Shower Peak", "اوج بارش شهابی شلیاقی")
                "shower_eta_aquariids" -> Triple(126, "Eta Aquariids Peak", "اوج بارش شهابی اتا دلو")
                "shower_orionids" -> Triple(294, "Orionids Meteor Shower Peak", "اوج بارش شهابی جباری")
                "shower_leonids" -> Triple(322, "Leonids Meteor Shower Peak", "اوج بارش شهابی اسدی")
                else -> Triple(-1, shower.nameEn, shower.nameFa)
            }
            if (peakDoy > 0) {
                val diff = Math.abs(dayOfYear - peakDoy)
                if (diff <= 15) {
                    val isPeak = diff <= 2
                    val score = if (isPeak) 900 else 650
                    events.add(
                        TonightEvent(
                            id = shower.id,
                            icon = "☄️",
                            titleEn = nameEn,
                            titleFa = nameFa,
                            explanationEn = if (isPeak) "Peak activity tonight with high ZHR rate (~${shower.zhr} meteors/hr)." else "Active annual shower visible in late night hours.",
                            explanationFa = if (isPeak) "اوج فعالیت امشب با نرخ بارش تا ${shower.zhr} شهاب در ساعت.".toPersianDigits() else "بارش شهابی فعال در ساعات پایانی شب.",
                            timeOrDateStrEn = shower.activePeakDateWindowEn,
                            timeOrDateStrFa = shower.activePeakDateWindowFa,
                            visibilityStatus = EventVisibilityStatus.OPTIMAL,
                            visibilityTextEn = "Best seen after midnight away from city lights",
                            visibilityTextFa = "بهترین زمان رصد پس از نیمه‌شب دور از آلودگی نوری",
                            importanceScore = score,
                            targetObject = shower
                        )
                    )
                }
            }
        }

        // 3. Bright ISS Passes Tonight
        try {
            val passes = ISSEngine.predictPasses(userLatDeg, userLonDeg, startTimestampMs = nowMs, scanDays = 1)
            val bestPass = passes.filter { it.maxElevationDeg >= 20.0 }.maxByOrNull { it.maxElevationDeg }
            if (bestPass != null) {
                val timeStr = TimeEngine.formatTime24h(bestPass.maxTimeMs, isFa)
                val elevInt = bestPass.maxElevationDeg.toInt()
                events.add(
                    TonightEvent(
                        id = "iss_pass_tonight",
                        icon = "🛰️",
                        titleEn = "International Space Station (ISS) Pass",
                        titleFa = "گذر پرنور ایستگاه فضایی بین‌المللی (ISS)",
                        explanationEn = "Bright naked-eye pass reaching $elevInt° elevation above horizon.",
                        explanationFa = "گذر درخشان با چشم غیرمسلح با اوج ارتفاع $elevInt درجه بالای افق.".toPersianDigits(),
                        timeOrDateStrEn = "Peak Time: $timeStr",
                        timeOrDateStrFa = "زمان اوج گذر: $timeStr",
                        visibilityStatus = EventVisibilityStatus.OPTIMAL,
                        visibilityTextEn = "Mag ${String.format("%.1f", bestPass.estimatedMagnitude)} — Highly Visible",
                        visibilityTextFa = "قدر ${String.format("%.1f", bestPass.estimatedMagnitude)} — کاملاً شفاف".toPersianDigits(),
                        importanceScore = 850
                    )
                )
            }
        } catch (_: Exception) {}

        // 4. Moon Phase & Nightly Elevation
        val moonData = MoonEngine.calculateMoon(jd)
        val moonHoriz = CoordinateEngine.equatorialToHorizontal(
            CoordinateEngine.Equatorial(moonData.raDeg, moonData.decDeg),
            gmstDeg,
            userLatDeg
        )
        val moonIllumInt = moonData.illuminationPercent.toInt()
        val phaseNameEn = moonData.phaseNameEn
        val phaseNameFa = moonData.phaseNameFa

        if (moonHoriz.altitudeDeg > 0.0 || moonIllumInt > 85) {
            val moonScore = if (moonIllumInt >= 90) 800 else 700
            events.add(
                TonightEvent(
                    id = "moon_nightly_status",
                    icon = "🌙",
                    titleEn = "Moon: $phaseNameEn ($moonIllumInt% Illuminated)",
                    titleFa = "وضعیت ماه: $phaseNameFa ($moonIllumInt٪ درخشندگی)".toPersianDigits(),
                    explanationEn = "Current distance: ${moonData.distanceKm.toInt()} km. Age: ${String.format("%.1f", moonData.ageDays)} days.",
                    explanationFa = "فاصله تا زمین: ${moonData.distanceKm.toInt()} کیلومتر. سن ماه: ${String.format("%.1f", moonData.ageDays)} روز.".toPersianDigits(),
                    timeOrDateStrEn = "Tonight's Sky",
                    timeOrDateStrFa = "آسمان امشب",
                    visibilityStatus = if (moonHoriz.altitudeDeg > 0) EventVisibilityStatus.OPTIMAL else EventVisibilityStatus.GOOD,
                    visibilityTextEn = if (moonHoriz.altitudeDeg > 0) "Visible now at ${moonHoriz.altitudeDeg.toInt()}° altitude" else "Rises later tonight",
                    visibilityTextFa = if (moonHoriz.altitudeDeg > 0) "هم‌اکنون در ارتفاع ${moonHoriz.altitudeDeg.toInt()} درجه".toPersianDigits() else "طلوع در ادامه امشب",
                    importanceScore = moonScore
                )
            )
        }

        // 5. Bright Planets (Venus, Jupiter, Saturn, Mars) - DO NOT include Jupiter's moons!
        val sunPos = SunEngine.calculatePosition(jd)
        val sunHoriz = CoordinateEngine.equatorialToHorizontal(
            CoordinateEngine.Equatorial(sunPos.raDeg, sunPos.decDeg),
            gmstDeg,
            userLatDeg
        )

        val planetList = listOf(
            Triple("planet_venus", PlanetEngine.PlanetType.VENUS, "Venus" to "ناهید (زهره)"),
            Triple("planet_jupiter", PlanetEngine.PlanetType.JUPITER, "Jupiter" to "مشتری (برجیس)"),
            Triple("planet_saturn", PlanetEngine.PlanetType.SATURN, "Saturn" to "کیوان (زحل)"),
            Triple("planet_mars", PlanetEngine.PlanetType.MARS, "Mars" to "مریخ (بهرام)")
        )

        for ((id, type, names) in planetList) {
            val pos = PlanetEngine.calculatePlanet(type, jd)
            val horiz = CoordinateEngine.equatorialToHorizontal(
                CoordinateEngine.Equatorial(pos.raDeg, pos.decDeg),
                gmstDeg,
                userLatDeg
            )
            val obs = ObservabilityEngine.calculateObservability(
                altitudeDeg = horiz.altitudeDeg,
                sunAltitudeDeg = sunHoriz.altitudeDeg,
                moonIlluminationPercent = moonData.illuminationPercent,
                objectMagnitude = pos.magnitude,
                objectType = ObjectType.PLANET,
                objectId = id
            )

            if (horiz.altitudeDeg > 5.0 || obs.scorePercent > 40) {
                val pScore = 750 + (pos.magnitude * -50).toInt() + (horiz.altitudeDeg * 2).toInt()
                events.add(
                    TonightEvent(
                        id = "event_$id",
                        icon = if (id == "planet_saturn") "🪐" else "🌟",
                        titleEn = "Planet ${names.first} Visible",
                        titleFa = "سیاره ${names.second} در آسمان امشب",
                        explanationEn = "Shining brightly at magnitude ${String.format("%.1f", pos.magnitude)} in ${horiz.azimuthCompassNameEn}.",
                        explanationFa = "درخشش با قدر ${String.format("%.1f", pos.magnitude)} در سمت ${horiz.azimuthCompassNameFa}.".toPersianDigits(),
                        timeOrDateStrEn = "Evening / Night Sky",
                        timeOrDateStrFa = "آسمان شبانگاهی",
                        visibilityStatus = if (horiz.altitudeDeg > 15.0) EventVisibilityStatus.OPTIMAL else EventVisibilityStatus.GOOD,
                        visibilityTextEn = "Altitude: ${horiz.altitudeDeg.toInt()}° — ${obs.level.nameEn}",
                        visibilityTextFa = "ارتفاع: ${horiz.altitudeDeg.toInt()} درجه — ${obs.level.nameFa}".toPersianDigits(),
                        importanceScore = pScore,
                        targetObject = CelestialObject(
                            id = id,
                            type = ObjectType.PLANET,
                            nameEn = names.first,
                            nameFa = names.second,
                            raDeg = pos.raDeg,
                            decDeg = pos.decDeg,
                            magnitude = pos.magnitude,
                            constellationEn = "",
                            constellationFa = "",
                            distanceLightYears = 0.0,
                            category = "Solar System Planet",
                            descriptionEn = type.descriptionEn,
                            descriptionFa = type.descriptionFa,
                            observationTipEn = "",
                            observationTipFa = ""
                        )
                    )
                )
            }
        }

        // 6. Planetary & Lunar Conjunction Check
        for ((id, type, names) in planetList) {
            val pos = PlanetEngine.calculatePlanet(type, jd)
            val sepDeg = CoordinateEngine.calculateAngularSeparationDeg(
                moonData.raDeg, moonData.decDeg,
                pos.raDeg, pos.decDeg
            )
            if (sepDeg < 5.0) {
                val sepStr = String.format("%.1f", sepDeg)
                events.add(
                    TonightEvent(
                        id = "conj_moon_$id",
                        icon = "✨",
                        titleEn = "Close Conjunction: Moon & ${names.first}",
                        titleFa = "مقارنه نزدیک ماه و ${names.second}",
                        explanationEn = "The Moon and ${names.first} appear just $sepStr° apart in tonight's sky.",
                        explanationFa = "ماه و ${names.second} تنها با فاصله $sepStr درجه در کنار یکدیگر می‌درخشند.".toPersianDigits(),
                        timeOrDateStrEn = "Tonight",
                        timeOrDateStrFa = "امشب",
                        visibilityStatus = EventVisibilityStatus.OPTIMAL,
                        visibilityTextEn = "Separation: $sepStr° — Beautiful Naked Eye View",
                        visibilityTextFa = "فاصله زاویه‌ای: $sepStr درجه — رصد جذاب با چشم غیرمسلح".toPersianDigits(),
                        importanceScore = 920
                    )
                )
            }
        }

        return events.sortedByDescending { it.importanceScore }
    }
}

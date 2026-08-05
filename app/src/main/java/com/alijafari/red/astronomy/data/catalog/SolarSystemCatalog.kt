package com.alijafari.red.astronomy.data.catalog

import com.alijafari.red.astronomy.astro_engine.JupiterMoonsEngine
import com.alijafari.red.astronomy.astro_engine.MoonEngine
import com.alijafari.red.astronomy.astro_engine.PlanetEngine
import com.alijafari.red.astronomy.astro_engine.SunEngine
import com.alijafari.red.astronomy.domain.CelestialObject
import com.alijafari.red.astronomy.domain.ObjectType

object SolarSystemCatalog {

    fun getSun(jd: Double): CelestialObject {
        val pos = SunEngine.calculatePosition(jd)
        val ra = pos.raDeg
        val constEn = when {
            ra in 45.0..75.0 -> "Taurus"
            ra in 75.0..105.0 -> "Gemini"
            ra in 105.0..135.0 -> "Cancer"
            ra in 135.0..165.0 -> "Leo"
            ra in 165.0..195.0 -> "Virgo"
            ra in 195.0..225.0 -> "Libra"
            ra in 225.0..255.0 -> "Scorpius"
            ra in 255.0..285.0 -> "Sagittarius"
            ra in 285.0..315.0 -> "Capricornus"
            ra in 315.0..345.0 -> "Aquarius"
            else -> "Pisces"
        }
        val constFa = when {
            ra in 45.0..75.0 -> "گاو (ثور)"
            ra in 75.0..105.0 -> "دوپیکر (جوزا)"
            ra in 105.0..135.0 -> "خرچنگ (سرطان)"
            ra in 135.0..165.0 -> "شیر (اسد)"
            ra in 165.0..195.0 -> "دوشیزه (سنبله)"
            ra in 195.0..225.0 -> "ترازو (میزان)"
            ra in 225.0..255.0 -> "عقرب (کژدم)"
            ra in 255.0..285.0 -> "کمان (قوس)"
            ra in 285.0..315.0 -> "بزغاله (جدی)"
            ra in 315.0..345.0 -> "دلو (آب‌کش)"
            else -> "ماهی (حوت)"
        }

        return CelestialObject(
            id = "sun_main",
            type = ObjectType.SUN,
            nameEn = "The Sun (Sol)",
            nameFa = "خورشید (مهر)",
            raDeg = pos.raDeg,
            decDeg = pos.decDeg,
            magnitude = -26.74,
            constellationEn = constEn,
            constellationFa = constFa,
            distanceLightYears = (pos.distanceAU * 149597870.7) / (9.461e12), // AU to LY
            category = "Yellow Dwarf Star (G2V)",
            descriptionEn = "The star at the center of the Solar System powering life on Earth.",
            descriptionFa = "ستاره مرکز منظومه شمسی و سرچشمه حیات، گرما و نور زمین.",
            observationTipEn = "WARNING: Never view Sun directly without certified solar filter!",
            observationTipFa = "هشدار: هرگز بدون فیلتر استاندارد خورشیدی مستقیم به خورشید نگاه نکنید!",
            spectralType = "G2V",
            temperatureK = 5778
        )
    }

    fun getMoon(jd: Double): CelestialObject {
        val pos = MoonEngine.calculateMoon(jd)
        val mag = -12.7 + 2.5 * kotlin.math.log10(100.0 / (pos.illuminationPercent.coerceAtLeast(1.0)))

        val ra = pos.raDeg
        val constEn = when {
            ra in 45.0..75.0 -> "Taurus"
            ra in 75.0..105.0 -> "Gemini"
            ra in 105.0..135.0 -> "Cancer"
            ra in 135.0..165.0 -> "Leo"
            ra in 165.0..195.0 -> "Virgo"
            ra in 195.0..225.0 -> "Libra"
            ra in 225.0..255.0 -> "Scorpius"
            ra in 255.0..285.0 -> "Sagittarius"
            ra in 285.0..315.0 -> "Capricornus"
            ra in 315.0..345.0 -> "Aquarius"
            else -> "Pisces"
        }
        val constFa = when {
            ra in 45.0..75.0 -> "گاو (ثور)"
            ra in 75.0..105.0 -> "دوپیکر (جوزا)"
            ra in 105.0..135.0 -> "خرچنگ (سرطان)"
            ra in 135.0..165.0 -> "شیر (اسد)"
            ra in 165.0..195.0 -> "دوشیزه (سنبله)"
            ra in 195.0..225.0 -> "ترازو (میزان)"
            ra in 225.0..255.0 -> "عقرب (کژدم)"
            ra in 255.0..285.0 -> "کمان (قوس)"
            ra in 285.0..315.0 -> "بزغاله (جدی)"
            ra in 315.0..345.0 -> "دلو (آب‌کش)"
            else -> "ماهی (حوت)"
        }

        return CelestialObject(
            id = "moon_main",
            type = ObjectType.MOON,
            nameEn = "The Moon (Luna)",
            nameFa = "ماه (قمر زمین)",
            raDeg = pos.raDeg,
            decDeg = pos.decDeg,
            magnitude = mag,
            constellationEn = constEn,
            constellationFa = constFa,
            distanceLightYears = (pos.distanceKm) / (9.461e12),
            category = "Natural Satellite",
            descriptionEn = "Earth's only natural satellite, showing phases and surface maria.",
            descriptionFa = "تنها قمر طبیعی زمین با فازهای متغیر ماه نو تا ماه کامل.",
            observationTipEn = "Best viewed along the terminator line during Quarter phases.",
            observationTipFa = "بهترین عوارض و دهانه‌های رصدی در مرز سایه و روشنایی (ترمیناتور) قرار دارند.",
            activePeakDateWindowEn = "Illuminated: ${pos.illuminationPercent.toInt()}% (${pos.phaseNameEn})",
            activePeakDateWindowFa = "درخشندگی: ${pos.illuminationPercent.toInt()}٪ (${pos.phaseNameFa})"
        )
    }

    fun getPlanets(jd: Double): List<CelestialObject> {
        val nakedEyePlanets = listOf(
            PlanetEngine.PlanetType.MERCURY,
            PlanetEngine.PlanetType.VENUS,
            PlanetEngine.PlanetType.MARS,
            PlanetEngine.PlanetType.JUPITER,
            PlanetEngine.PlanetType.SATURN,
            PlanetEngine.PlanetType.URANUS
        )

        return nakedEyePlanets.map { pType ->
            val pos = PlanetEngine.calculatePlanet(pType, jd)
            val constEn = when {
                pos.decDeg > 20 -> "Taurus / Gemini"
                pos.decDeg > 0 -> "Virgo / Leo"
                pos.decDeg > -20 -> "Ophiuchus / Sagittarius"
                else -> "Scorpius / Pisces"
            }
            val constFa = when {
                pos.decDeg > 20 -> "ثور / دوپیکر"
                pos.decDeg > 0 -> "سنبله / اسد"
                pos.decDeg > -20 -> "حوا / قوس"
                else -> "عقرب / حوت"
            }

            CelestialObject(
                id = "planet_${pType.name.lowercase()}",
                type = ObjectType.PLANET,
                nameEn = pType.nameEn,
                nameFa = pType.nameFa,
                raDeg = pos.raDeg,
                decDeg = pos.decDeg,
                magnitude = pos.magnitude,
                constellationEn = constEn,
                constellationFa = constFa,
                distanceLightYears = (pos.distanceAU * 149597870.7) / (9.461e12),
                category = if (pType == PlanetEngine.PlanetType.MERCURY || pType == PlanetEngine.PlanetType.VENUS || pType == PlanetEngine.PlanetType.MARS) "Terrestrial Planet" else "Gas/Ice Giant",
                descriptionEn = pType.descriptionEn,
                descriptionFa = pType.descriptionFa,
                observationTipEn = "Distance: ${String.format("%.2f", pos.distanceAU)} AU | Illuminated: ${(pos.illuminatedFraction * 100).toInt()}%",
                observationTipFa = "فاصله: ${String.format("%.2f", pos.distanceAU)} واحد نجومی | درخشندگی: ${(pos.illuminatedFraction * 100).toInt()}٪"
            )
        }
    }

    fun getGalileanMoons(jd: Double): List<CelestialObject> {
        val jupSystem = JupiterMoonsEngine.calculateJupiterMoons(jd)
        val jupPos = jupSystem.jupiterPos

        return jupSystem.moons.map { moonPos ->
            val raDeg = jupPos.raDeg + (moonPos.offsetRaArcsec / 3600.0)
            val decDeg = jupPos.decDeg + (moonPos.offsetDecArcsec / 3600.0)

            val statusEn = moonPos.phenomenon.name
            val statusFa = when (moonPos.phenomenon) {
                JupiterMoonsEngine.MoonPhenomenon.VISIBLE -> "قابل رصد"
                JupiterMoonsEngine.MoonPhenomenon.IN_TRANSIT -> "در حال ترانزیت از روی مشتری"
                JupiterMoonsEngine.MoonPhenomenon.OCCULTED -> "در پشت مشتری (اختفا)"
                JupiterMoonsEngine.MoonPhenomenon.IN_ECLIPSE -> "در سایه مشتری (گرفت)"
                JupiterMoonsEngine.MoonPhenomenon.SHADOW_TRANSIT -> "عبور سایه قمر روی مشتری"
            }

            CelestialObject(
                id = "galilean_moon_${moonPos.moon.name.lowercase()}",
                type = ObjectType.MOON,
                nameEn = "${moonPos.moon.nameEn} (Jupiter Moon)",
                nameFa = "قمر گالیله‌ای ${moonPos.moon.nameFa} (مشتری)",
                raDeg = raDeg,
                decDeg = decDeg,
                magnitude = 5.0, // Naked eye visible under ideal conditions or small optics
                constellationEn = "Jupiter Orbit",
                constellationFa = "مدار مشتری",
                distanceLightYears = (jupPos.distanceAU * 149597870.7) / (9.461e12),
                category = "Galilean Satellite",
                descriptionEn = "One of the 4 major moons of Jupiter discovered by Galileo Galilei in 1610.",
                descriptionFa = "یکی از ۴ قمر بزرگ گالیله‌ای مشتری که در سال ۱۶۱۰ کشف شد.",
                observationTipEn = "Offset from Jupiter: ${String.format("%.1f", moonPos.xRJ)} RJ ($statusEn)",
                observationTipFa = "فاصله از مشتری: ${String.format("%.1f", moonPos.xRJ)} شعاع مشتری ($statusFa)"
            )
        }
    }
}

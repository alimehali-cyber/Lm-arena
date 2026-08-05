package com.alijafari.red.astronomy.data.catalog

import com.alijafari.red.astronomy.astro_engine.TimeEngine
import com.alijafari.red.astronomy.domain.CelestialObject
import com.alijafari.red.astronomy.domain.ConstellationData
import com.alijafari.red.astronomy.domain.ObjectType

object AstronomyCatalog {

    val SUN = SolarSystemCatalog.getSun(TimeEngine.getJulianDate())
    val MOON = SolarSystemCatalog.getMoon(TimeEngine.getJulianDate())

    val ISS = CelestialObject(
        id = "sat_iss",
        type = ObjectType.SATELLITE,
        nameEn = "International Space Station (ISS)",
        nameFa = "ایستگاه فضایی بین‌المللی (ISS)",
        raDeg = 180.0,
        decDeg = 35.0,
        magnitude = -3.2,
        constellationEn = "Low Earth Orbit",
        constellationFa = "مدار زمین",
        distanceLightYears = 0.00000004,
        category = "Habitable Space Station",
        descriptionEn = "Habitable artificial satellite orbiting Earth every 90 minutes at 28,000 km/h.",
        descriptionFa = "ایستگاه فضایی سرنشین‌دار در حال چرخش به دور زمین هر ۹۰ دقیقه با سرعت ۲۸,۰۰۰ کیلومتر بر ساعت.",
        observationTipEn = "Appears as a fast-moving unblinking bright point across twilight skies.",
        observationTipFa = "مانند یک نقطه بسیار پرنور بدون چشمک‌زدن با سرعت بالای افق حرکت می‌کند."
    )

    val MILKY_WAY = CelestialObject(
        id = "galaxy_milky_way",
        type = ObjectType.GALAXY,
        nameEn = "Milky Way Galaxy Center",
        nameFa = "مرکز کهکشان راه شیری (Sagittarius A*)",
        raDeg = 266.4,
        decDeg = -29.0,
        magnitude = -5.0,
        constellationEn = "Sagittarius",
        constellationFa = "کمان (قوس)",
        distanceLightYears = 26000.0,
        category = "Barred Spiral Galaxy",
        descriptionEn = "Our home spiral galaxy containing 100-400 billion stars and Sagittarius A*.",
        descriptionFa = "کهکشان خانگی ما شامل ۱۰۰ تا ۴۰۰ میلیارد ستاره و سیاهچاله کلان‌جرم مرکزی.",
        observationTipEn = "Visible as a luminous glowing arch across dark unpolluted summer skies.",
        observationTipFa = "نوار پهن و درخشان نورانی در شب‌های تابستان و مناطق کویری تاریک."
    )

    val ANDROMEDA = DeepSkyCatalog.getDeepSkyObjects().first { it.id == "dso_m31_andromeda" }
    val ORION_NEBULA = DeepSkyCatalog.getDeepSkyObjects().first { it.id == "dso_m42_orion_nebula" }
    val PLEIADES = DeepSkyCatalog.getDeepSkyObjects().first { it.id == "dso_m45_pleiades" }

    val SIRIUS = StarCatalog.getStars().first { it.id == "star_cma_sirius" }
    val VEGA = StarCatalog.getStars().first { it.id == "star_lyr_vega" }
    val BETELGEUSE = StarCatalog.getStars().first { it.id == "star_ori_betelgeuse" }
    val POLARIS = StarCatalog.getStars().first { it.id == "star_umi_polaris" }

    val IRAN_CITIES = listOf(
        Triple("Safashahr (Fars)", "صفاشهر (فارس)", 30.6158 to 53.1956),
        Triple("Nurabad City (NC)", "نورآباد ممسنی (NC)", 30.1141 to 51.5217),
        Triple("Tehran", "تهران", 35.6892 to 51.3890),
        Triple("Shiraz", "شیراز", 29.5918 to 52.5837),
        Triple("Isfahan", "اصفهان", 32.6546 to 51.6680),
        Triple("Tabriz", "تبریز", 38.0962 to 46.2694),
        Triple("Mashhad", "مشهد", 36.2972 to 59.6067),
        Triple("Kerman", "کرمان", 30.2839 to 57.0834),
        Triple("Ahvaz", "اهواز", 31.3183 to 48.6706),
        Triple("Rasht", "رشت", 37.2808 to 49.5832),
        Triple("Yazd", "یزد", 31.8974 to 54.3675),
        Triple("Kermanshah", "کرمانشاه", 34.3142 to 47.0650),
        Triple("Hamadan", "همدان", 34.7982 to 48.5146),
        Triple("Zahedan", "زاهدان", 29.4963 to 60.8629),
        Triple("Bandar Abbas", "بندرعباس", 27.1832 to 56.2666),
        Triple("Sanandaj", "سنندج", 35.3144 to 46.9923),
        Triple("Bushehr", "بوشهر", 28.9234 to 50.8382)
    )

    val DEFAULT_CONSTELLATIONS = ConstellationCatalog.getConstellations()

    /**
     * Master catalog query: aggregates Sun, Moon, Planets, Galilean Moons, Stars,
     * Deep Sky Objects, Meteor Showers, Asterisms, Constellations, and Reference Points
     * into a single master catalog.
     */
    fun getAllObjects(jd: Double = TimeEngine.getJulianDate()): List<CelestialObject> {
        val sun = SolarSystemCatalog.getSun(jd)
        val moon = SolarSystemCatalog.getMoon(jd)
        val planets = SolarSystemCatalog.getPlanets(jd)
        val galileanMoons = SolarSystemCatalog.getGalileanMoons(jd)

        val stars = StarCatalog.getStars()
        val deepSky = DeepSkyCatalog.getDeepSkyObjects()
        val meteorShowers = MeteorShowerCatalog.getMeteorShowers()
        val asterisms = AsterismCatalog.getAsterisms()

        val constellationsAsObjects = ConstellationCatalog.getConstellations().map { c ->
            val avgRa = if (c.mainStars.isNotEmpty()) c.mainStars.map { it.first }.average() else 0.0
            val avgDec = if (c.mainStars.isNotEmpty()) c.mainStars.map { it.second }.average() else 0.0
            CelestialObject(
                id = "const_${c.code.lowercase()}",
                type = com.alijafari.red.astronomy.domain.ObjectType.CONSTELLATION,
                nameEn = "${c.nameEn} Constellation",
                nameFa = "صورت فلکی ${c.nameFa}",
                raDeg = avgRa,
                decDeg = avgDec,
                magnitude = 2.0,
                constellationEn = c.nameEn,
                constellationFa = c.nameFa,
                distanceLightYears = 0.0,
                category = "Constellation (${c.seasonEn})",
                descriptionEn = "${c.historicalInfoEn} Area: ${c.areaSqDeg} sq deg. Best Month: ${c.bestViewingMonthEn}.",
                descriptionFa = "${c.historicalInfoFa} مساحت: ${c.areaSqDeg} درجه مربع. بهترین ماه رصد: ${c.bestViewingMonthFa}.",
                observationTipEn = "Look for it in the ${c.seasonEn} sky.",
                observationTipFa = "در آسمان ${c.seasonFa} قابل مشاهده است."
            )
        }

        val referencePoints = listOf(
            CelestialObject(
                id = "ref_ncp",
                type = com.alijafari.red.astronomy.domain.ObjectType.REFERENCE_POINT,
                nameEn = "North Celestial Pole",
                nameFa = "قطب شمال آسمانی (NCP)",
                raDeg = 0.0,
                decDeg = 90.0,
                magnitude = 2.0,
                constellationEn = "Ursa Minor",
                constellationFa = "خرس کوچک (دب اصغر)",
                distanceLightYears = 0.0,
                category = "Celestial Coordinate Reference",
                descriptionEn = "Point in the sky around which all northern celestial objects appear to rotate.",
                descriptionFa = "نقطه‌ای محوری در قطب شمال کره آسمان که تمام اجرام به دور آن می‌چرخند.",
                observationTipEn = "Located right next to Polaris in the northern sky.",
                observationTipFa = "در مجاورت ستاره قطبی در آسمان شمالی قرار دارد."
            ),
            CelestialObject(
                id = "ref_galactic_center",
                type = com.alijafari.red.astronomy.domain.ObjectType.REFERENCE_POINT,
                nameEn = "Galactic Center (Sagittarius A*)",
                nameFa = "مرکز کهکشان راه شیری (Sagittarius A*)",
                raDeg = 266.417,
                decDeg = -29.008,
                magnitude = 0.0,
                constellationEn = "Sagittarius",
                constellationFa = "کمان (قوس)",
                distanceLightYears = 26000.0,
                category = "Supermassive Black Hole",
                descriptionEn = "Rotational center of the Milky Way galaxy housing a 4-million solar mass supermassive black hole.",
                descriptionFa = "مرکز چرخش کهکشان راه شیری حاوی سیاهچاله غول‌پیکر ۴ میلیون برابر جرم خورشید.",
                observationTipEn = "Look towards the richest part of the Milky Way in Sagittarius during summer.",
                observationTipFa = "در تابستان به متراکم‌ترین بخش نوار راه شیری در صورت فلکی قوس نگاه کنید."
            )
        )

        return listOf(sun, moon, ISS, MILKY_WAY) + planets + galileanMoons + stars + deepSky + meteorShowers + asterisms + constellationsAsObjects + referencePoints
    }

    fun getConstellations(): List<ConstellationData> {
        return ConstellationCatalog.getConstellations()
    }

    fun getAsterisms(): List<CelestialObject> {
        return AsterismCatalog.getAsterisms()
    }

    fun getMeteorShowers(): List<CelestialObject> {
        return MeteorShowerCatalog.getMeteorShowers()
    }

    fun getDeepSkyObjects(): List<CelestialObject> {
        return DeepSkyCatalog.getDeepSkyObjects()
    }

    fun getStars(): List<CelestialObject> {
        return StarCatalog.getStars()
    }
}

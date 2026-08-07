package com.alijafari.red.astronomy.astro_engine

import kotlin.math.*

object EarthEngine {

    const val AXIAL_TILT_DEG = 23.439281
    const val EQUATORIAL_RADIUS_KM = 6378.137
    const val POLAR_RADIUS_KM = 6356.752
    const val EQUATORIAL_CIRCUMFERENCE_KM = 40075.017
    const val ROTATION_SPEED_EQUATOR_KMH = 1670.0
    const val ORBITAL_SPEED_KMS = 29.78
    const val SURFACE_AREA_MILLION_KM2 = 510.072
    const val WATER_PERCENT = 70.8
    const val LAND_PERCENT = 29.2

    data class SubsolarPoint(
        val latDeg: Double,
        val lonDeg: Double,
        val declinationDeg: Double,
        val ghaDeg: Double
    )

    data class CityLightNode(
        val nameEn: String,
        val nameFa: String,
        val latDeg: Float,
        val lonDeg: Float,
        val brightness: Float // 0.4f to 1.0f
    )

    enum class TerrainType {
        LAND_GREEN,
        LAND_DESERT,
        ICE_CAP,
        TUNDRA
    }

    data class PolygonRing(
        val terrainType: TerrainType,
        val points: List<Pair<Float, Float>> // List of (Lat, Lon) in degrees
    )

    /**
     * Calculates the subsolar point on Earth for a given Julian Date.
     */
    fun calculateSubsolarPoint(jd: Double): SubsolarPoint {
        val sunPos = SunEngine.calculatePosition(jd)
        val gmstDeg = TimeEngine.getGMST(jd)

        // Greenwich Hour Angle (GHA) of Sun = GMST - RightAscension
        var ghaDeg = (gmstDeg - sunPos.raDeg) % 360.0
        if (ghaDeg < 0) ghaDeg += 360.0

        // Subsolar Longitude = -GHA mapped to [-180, 180]
        var lonDeg = -ghaDeg
        while (lonDeg < -180.0) lonDeg += 360.0
        while (lonDeg > 180.0) lonDeg -= 360.0

        return SubsolarPoint(
            latDeg = sunPos.decDeg,
            lonDeg = lonDeg,
            declinationDeg = sunPos.decDeg,
            ghaDeg = ghaDeg
        )
    }

    /**
     * Calculates solar elevation angle at given lat/lon for a subsolar point.
     */
    fun calculateSolarElevation(
        latDeg: Double,
        lonDeg: Double,
        subsolar: SubsolarPoint
    ): Double {
        val latRad = Math.toRadians(latDeg)
        val lonRad = Math.toRadians(lonDeg)
        val subLatRad = Math.toRadians(subsolar.latDeg)
        val subLonRad = Math.toRadians(subsolar.lonDeg)

        val sinAlt = sin(latRad) * sin(subLatRad) +
                cos(latRad) * cos(subLatRad) * cos(lonRad - subLonRad)

        val sinAltClamped = sinAlt.coerceIn(-1.0, 1.0)
        return Math.toDegrees(asin(sinAltClamped))
    }

    /**
     * Comprehensive list of global urban night light clusters for realistic night rendering.
     */
    fun getCityNightLights(): List<CityLightNode> {
        return listOf(
            CityLightNode("Tehran", "تهران", 35.6892f, 51.3890f, 1.0f),
            CityLightNode("Mashhad", "مشهد", 36.2972f, 59.6067f, 0.85f),
            CityLightNode("Isfahan", "اصفهان", 32.6546f, 51.6680f, 0.85f),
            CityLightNode("Shiraz", "شیراز", 29.5918f, 52.5837f, 0.80f),
            CityLightNode("Tabriz", "تبریز", 38.0800f, 46.2919f, 0.80f),
            CityLightNode("Dubai", "دبی", 25.2048f, 55.2708f, 1.0f),
            CityLightNode("Riyadh", "ریاض", 24.7136f, 46.6753f, 0.90f),
            CityLightNode("Cairo", "قاهره", 30.0444f, 31.2357f, 0.95f),
            CityLightNode("Istanbul", "استانبول", 41.0082f, 28.9784f, 0.95f),
            CityLightNode("London", "لندن", 51.5074f, -0.1278f, 1.0f),
            CityLightNode("Paris", "پاریس", 48.8566f, 2.3522f, 1.0f),
            CityLightNode("Madrid", "مادرید", 40.4168f, -3.7038f, 0.85f),
            CityLightNode("Rome", "رم", 41.9028f, 12.4964f, 0.85f),
            CityLightNode("Berlin", "برلین", 52.5200f, 13.4050f, 0.90f),
            CityLightNode("Moscow", "مسکو", 55.7558f, 37.6173f, 0.95f),
            CityLightNode("Tokyo", "توکیو", 35.6762f, 139.6503f, 1.0f),
            CityLightNode("Osaka", "اوساکا", 34.6937f, 135.5023f, 0.95f),
            CityLightNode("Seoul", "سئول", 37.5665f, 126.9780f, 1.0f),
            CityLightNode("Beijing", "پکن", 39.9042f, 116.4074f, 1.0f),
            CityLightNode("Shanghai", "شانگهای", 31.2304f, 121.4737f, 1.0f),
            CityLightNode("Hong Kong", "هنگ کنگ", 22.3193f, 114.1694f, 0.95f),
            CityLightNode("Guangzhou", "گوانگژو", 23.1291f, 113.2644f, 0.95f),
            CityLightNode("New Delhi", "دهلی نو", 28.6139f, 77.2090f, 0.95f),
            CityLightNode("Mumbai", "ممبئی", 19.0760f, 72.8777f, 1.0f),
            CityLightNode("Bangkok", "بانکوک", 13.7563f, 100.5018f, 0.90f),
            CityLightNode("Singapore", "سنگاپور", 1.3521f, 103.8198f, 0.95f),
            CityLightNode("Jakarta", "جاکرتا", -6.2088f, 106.8456f, 0.90f),
            CityLightNode("Sydney", "سیدنی", -33.8688f, 151.2093f, 0.90f),
            CityLightNode("Melbourne", "ملبورن", -37.8136f, 144.9631f, 0.85f),
            CityLightNode("New York", "نیویورک", 40.7128f, -74.0060f, 1.0f),
            CityLightNode("Los Angeles", "لوس آنجلس", 34.0522f, -118.2437f, 1.0f),
            CityLightNode("Chicago", "شیکاگو", 41.8781f, -87.6298f, 0.95f),
            CityLightNode("Houston", "هوستون", 29.7604f, -95.3698f, 0.85f),
            CityLightNode("Miami", "میامی", 25.7617f, -80.1918f, 0.90f),
            CityLightNode("Toronto", "تورنتو", 43.6532f, -79.3832f, 0.90f),
            CityLightNode("Mexico City", "مکزیکوسیتی", 19.4326f, -99.1332f, 0.95f),
            CityLightNode("Sao Paulo", "سائوپائولو", -23.5505f, -46.6333f, 0.95f),
            CityLightNode("Rio de Janeiro", "ریودوژانیرو", -22.9068f, -43.1729f, 0.90f),
            CityLightNode("Buenos Aires", "بوئنوس آیرس", -34.6037f, -58.3816f, 0.90f),
            CityLightNode("Santiago", "سانتیاگو", -33.4489f, -70.6693f, 0.85f),
            CityLightNode("Johannesburg", "ژوهانسبورگ", -26.2041f, 28.0473f, 0.85f),
            CityLightNode("Lagos", "لاگوس", 6.5244f, 3.3792f, 0.85f)
        )
    }

    /**
     * Detailed mathematical spherical polygon contours representing Earth's landmasses.
     */
    fun getContinentPolygons(): List<PolygonRing> {
        val list = mutableListOf<PolygonRing>()

        // 1. AFRICA (Sahara Desert north, Savannah/Jungle south)
        list.add(
            PolygonRing(
                terrainType = TerrainType.LAND_DESERT,
                points = listOf(
                    37f to 10f, 36f to -6f, 32f to -9f, 28f to -13f, 21f to -17f,
                    15f to -17f, 10f to -14f, 5f to -3f, 4f to 9f, 2f to 10f,
                    9f to 14f, 12f to 43f, 30f to 32f, 32f to 34f, 37f to 10f
                )
            )
        )
        list.add(
            PolygonRing(
                terrainType = TerrainType.LAND_GREEN,
                points = listOf(
                    12f to 43f, 11f to 51f, 0f to 42f, -12f to 40f, -25f to 35f,
                    -34f to 26f, -34f to 18f, -28f to 16f, -18f to 12f, -12f to 13f,
                    -5f to 12f, 2f to 10f, 4f to 9f, 5f to -3f, 10f to -14f,
                    15f to -17f, 12f to 43f
                )
            )
        )

        // 2. EURASIA (Europe, Middle East, Asia)
        // Europe (Green)
        list.add(
            PolygonRing(
                terrainType = TerrainType.LAND_GREEN,
                points = listOf(
                    36f to -6f, 43f to -9f, 48f to -4f, 48f to -1f, 51f to 1f,
                    54f to 5f, 57f to 8f, 58f to 11f, 56f to 14f, 54f to 18f,
                    55f to 21f, 59f to 23f, 65f to 23f, 70f to 28f, 70f to 60f,
                    60f to 60f, 50f to 50f, 45f to 35f, 40f to 30f, 41f to 28f,
                    38f to 24f, 37f to 22f, 36f to 15f, 38f to 12f, 43f to 16f,
                    43f to 10f, 41f to 9f, 38f to 0f, 36f to -6f
                )
            )
        )
        // Scandinavia (Tundra / Green)
        list.add(
            PolygonRing(
                terrainType = TerrainType.TUNDRA,
                points = listOf(
                    55f to 12f, 58f to 11f, 62f to 5f, 68f to 14f, 71f to 26f,
                    70f to 30f, 65f to 25f, 60f to 18f, 56f to 13f, 55f to 12f
                )
            )
        )
        // Middle East & North Africa Desert Belt
        list.add(
            PolygonRing(
                terrainType = TerrainType.LAND_DESERT,
                points = listOf(
                    30f to 32f, 34f to 35f, 37f to 36f, 38f to 44f, 30f to 48f,
                    25f to 55f, 22f to 59f, 12f to 43f, 15f to 53f, 24f to 57f,
                    30f to 60f, 35f to 62f, 40f to 50f, 30f to 32f
                )
            )
        )
        // Asia Main Body (Siberia, China, India, Southeast Asia)
        list.add(
            PolygonRing(
                terrainType = TerrainType.LAND_GREEN,
                points = listOf(
                    70f to 60f, 73f to 80f, 72f to 110f, 70f to 140f, 65f to 170f,
                    60f to 165f, 55f to 135f, 40f to 120f, 30f to 122f, 22f to 114f,
                    20f to 108f, 10f to 104f, 1f to 103f, 8f to 98f, 20f to 92f,
                    22f to 88f, 15f to 80f, 8f to 77f, 20f to 73f, 25f to 67f,
                    30f to 60f, 40f to 60f, 50f to 60f, 60f to 60f, 70f to 60f
                )
            )
        )
        // India Desert (Thar)
        list.add(
            PolygonRing(
                terrainType = TerrainType.LAND_DESERT,
                points = listOf(
                    25f to 67f, 30f to 70f, 28f to 76f, 23f to 72f, 25f to 67f
                )
            )
        )

        // 3. NORTH AMERICA
        list.add(
            PolygonRing(
                terrainType = TerrainType.LAND_GREEN,
                points = listOf(
                    70f to -165f, 71f to -130f, 68f to -90f, 62f to -80f, 58f to -63f,
                    47f to -53f, 44f to -66f, 41f to -70f, 35f to -75f, 25f to -80f,
                    30f to -85f, 29f to -95f, 26f to -97f, 20f to -97f, 16f to -93f,
                    15f to -88f, 9f to -79f, 8f to -83f, 14f to -92f, 20f to -105f,
                    32f to -117f, 37f to -122f, 48f to -124f, 60f to -140f,
                    60f to -165f, 65f to -168f, 70f to -165f
                )
            )
        )
        // North America Southwest Desert
        list.add(
            PolygonRing(
                terrainType = TerrainType.LAND_DESERT,
                points = listOf(
                    32f to -117f, 36f to -115f, 35f to -105f, 29f to -100f,
                    26f to -97f, 20f to -105f, 32f to -117f
                )
            )
        )
        // Greenland (Ice Cap)
        list.add(
            PolygonRing(
                terrainType = TerrainType.ICE_CAP,
                points = listOf(
                    60f to -44f, 65f to -52f, 72f to -55f, 78f to -68f, 82f to -30f,
                    80f to -20f, 70f to -22f, 65f to -38f, 60f to -44f
                )
            )
        )

        // 4. SOUTH AMERICA
        list.add(
            PolygonRing(
                terrainType = TerrainType.LAND_GREEN,
                points = listOf(
                    11f to -73f, 10f to -62f, 8f to -59f, 5f to -52f, -5f to -35f,
                    -10f to -36f, -22f to -41f, -33f to -52f, -40f to -62f, -52f to -68f,
                    -55f to -66f, -50f to -74f, -40f to -73f, -30f to -71f,
                    -18f to -70f, -5f to -81f, 1f to -79f, 9f to -79f, 11f to -73f
                )
            )
        )

        // 5. AUSTRALIA
        list.add(
            PolygonRing(
                terrainType = TerrainType.LAND_DESERT,
                points = listOf(
                    -12f to 131f, -15f to 136f, -25f to 153f, -38f to 145f,
                    -38f to 140f, -35f to 135f, -32f to 116f, -22f to 114f,
                    -15f to 124f, -12f to 131f
                )
            )
        )

        // 6. ANTARCTICA (Ice Cap)
        list.add(
            PolygonRing(
                terrainType = TerrainType.ICE_CAP,
                points = listOf(
                    -65f to -180f, -70f to -120f, -75f to -60f, -65f to 0f,
                    -67f to 60f, -66f to 120f, -68f to 160f, -65f to 180f
                )
            )
        )

        // 7. MAJOR ISLANDS
        // Japan
        list.add(
            PolygonRing(
                terrainType = TerrainType.LAND_GREEN,
                points = listOf(
                    31f to 130f, 35f to 135f, 44f to 144f, 45f to 142f, 38f to 138f, 31f to 130f
                )
            )
        )
        // Great Britain & Ireland
        list.add(
            PolygonRing(
                terrainType = TerrainType.LAND_GREEN,
                points = listOf(
                    50f to -5f, 54f to -3f, 58f to -5f, 57f to -2f, 51f to 1f, 50f to -5f
                )
            )
        )
        // Madagascar
        list.add(
            PolygonRing(
                terrainType = TerrainType.LAND_GREEN,
                points = listOf(
                    -12f to 49f, -16f to 44f, -25f to 44f, -25f to 47f, -16f to 50f, -12f to 49f
                )
            )
        )
        // New Zealand
        list.add(
            PolygonRing(
                terrainType = TerrainType.LAND_GREEN,
                points = listOf(
                    -35f to 174f, -39f to 177f, -46f to 167f, -42f to 172f, -35f to 174f
                )
            )
        )

        return list
    }
}

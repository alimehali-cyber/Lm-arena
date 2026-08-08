package com.alijafari.red.astronomy.astro_engine

import androidx.compose.ui.graphics.Path
import kotlin.math.abs

/**
 * Accurate offline geographic dataset of Earth landmasses and major islands
 * represented in standard Equirectangular / Plate Carrée coordinates:
 * Longitude: [-180.0 .. 180.0] degrees
 * Latitude:  [-90.0 .. 90.0] degrees
 */
object WorldGeographyData {

    // (Latitude, Longitude) polygons for main world continents & islands
    val landmasses: List<List<Pair<Double, Double>>> = listOf(
        // North America (detailed outline)
        listOf(
            71.3 to -156.5, 70.0 to -141.0, 69.0 to -135.0, 68.0 to -120.0,
            60.0 to -114.0, 63.0 to -90.0, 60.0 to -82.0, 62.0 to -78.0,
            55.0 to -78.0, 52.0 to -82.0, 51.0 to -64.0, 47.0 to -53.0,
            44.0 to -63.0, 41.0 to -70.0, 35.0 to -75.0, 25.0 to -80.0,
            25.0 to -81.0, 30.0 to -85.0, 29.0 to -89.0, 26.0 to -97.0,
            20.0 to -97.0, 18.0 to -91.0, 21.0 to -87.0, 15.0 to -88.0,
            13.0 to -87.0, 9.0 to -79.0, 8.0 to -83.0, 14.0 to -92.0,
            16.0 to -98.0, 23.0 to -110.0, 32.0 to -117.0, 38.0 to -123.0,
            48.0 to -125.0, 55.0 to -133.0, 60.0 to -149.0, 60.0 to -165.0,
            66.0 to -168.0, 71.0 to -157.0
        ),
        // Greenland
        listOf(
            76.0 to -68.0, 81.0 to -60.0, 83.0 to -30.0, 81.0 to -12.0,
            76.0 to -20.0, 70.0 to -22.0, 65.0 to -37.0, 60.0 to -43.0,
            64.0 to -52.0, 70.0 to -54.0, 76.0 to -68.0
        ),
        // South America
        listOf(
            11.5 to -72.8, 10.5 to -61.5, 8.0 to -59.5, 4.0 to -51.5,
            -2.5 to -44.0, -5.0 to -35.0, -13.0 to -38.5, -23.0 to -42.0,
            -34.5 to -53.5, -39.0 to -62.0, -46.0 to -66.0, -52.0 to -68.0,
            -55.0 to -67.0, -53.0 to -71.0, -45.0 to -74.0, -33.0 to -72.0,
            -18.0 to -70.0, -12.0 to -77.0, -5.0 to -81.0, 1.0 to -79.0,
            8.0 to -77.5, 11.5 to -72.8
        ),
        // Eurasia (Europe + Asia mainland detailed outline)
        listOf(
            36.0 to -5.3, 43.5 to -9.3, 48.0 to -4.5, 50.0 to 1.5,
            53.0 to 5.0, 55.0 to 8.5, 58.0 to 11.0, 62.0 to 5.0,
            70.0 to 18.0, 71.0 to 28.0, 68.0 to 40.0, 67.0 to 50.0,
            73.0 to 70.0, 76.0 to 100.0, 77.5 to 104.0, 72.0 to 125.0,
            70.0 to 150.0, 66.0 to 170.0, 66.0 to 180.0, 60.0 to 165.0,
            55.0 to 160.0, 50.0 to 142.0, 43.0 to 132.0, 37.0 to 122.0,
            30.0 to 122.0, 22.0 to 113.0, 21.0 to 108.0, 10.0 to 104.0,
            1.3 to 103.8, 6.0 to 100.0, 16.0 to 96.0, 21.0 to 89.0,
            16.0 to 82.0, 8.0 to 77.5, 16.0 to 73.0, 23.0 to 68.0,
            25.0 to 62.0, 26.0 to 56.0, 12.8 to 45.0, 12.0 to 43.0,
            28.0 to 34.0, 31.0 to 32.0, 36.0 to 36.0, 41.0 to 28.5,
            40.0 to 26.0, 38.0 to 23.0, 36.5 to 22.5, 40.0 to 19.5,
            45.0 to 13.5, 44.0 to 9.5, 43.0 to 6.5, 36.0 to -5.3
        ),
        // United Kingdom & Ireland
        listOf(
            50.0 to -5.5, 51.0 to 1.3, 54.0 to -0.2, 58.0 to -3.0,
            58.5 to -5.0, 55.0 to -5.5, 53.0 to -4.0, 50.0 to -5.5
        ),
        listOf(
            51.5 to -10.0, 54.5 to -10.0, 55.0 to -6.0, 52.0 to -6.0, 51.5 to -10.0
        ),
        // Japan (Honshu & Hokkaido)
        listOf(
            31.0 to 130.5, 34.0 to 132.0, 36.0 to 140.0, 40.0 to 140.0,
            41.5 to 141.5, 45.0 to 142.0, 43.0 to 145.0, 38.0 to 141.0,
            35.0 to 136.0, 33.0 to 130.0, 31.0 to 130.5
        ),
        // Africa
        listOf(
            37.0 to 10.0, 37.0 to 11.0, 32.0 to 24.0, 31.5 to 32.5,
            28.0 to 34.5, 22.0 to 37.0, 11.5 to 43.0, 12.0 to 51.0,
            5.0 to 48.0, -10.0 to 40.0, -25.0 to 33.0, -34.8 to 20.0,
            -34.0 to 18.0, -22.0 to 14.0, -12.0 to 13.0, -5.0 to 12.0,
            4.0 to 9.0, 5.0 to -3.0, 4.5 to -7.5, 11.0 to -15.0,
            14.7 to -17.5, 21.0 to -17.0, 28.0 to -13.0, 35.8 to -5.8,
            37.0 to 10.0
        ),
        // Madagascar
        listOf(
            -12.0 to 49.3, -16.0 to 44.4, -25.0 to 44.0, -25.5 to 45.2,
            -20.0 to 48.5, -15.0 to 50.5, -12.0 to 49.3
        ),
        // Australia
        listOf(
            -12.0 to 131.0, -11.0 to 136.0, -15.0 to 136.0, -12.0 to 142.0,
            -25.0 to 153.5, -37.5 to 150.0, -38.5 to 146.0, -38.0 to 140.0,
            -35.0 to 135.0, -32.0 to 125.0, -35.0 to 117.0, -31.0 to 115.0,
            -22.0 to 114.0, -19.0 to 121.0, -14.0 to 126.0, -12.0 to 131.0
        ),
        // New Zealand (North & South Islands)
        listOf(
            -34.5 to 172.5, -37.0 to 175.0, -39.0 to 177.0, -41.5 to 175.0,
            -41.0 to 172.0, -38.0 to 174.5, -34.5 to 172.5
        ),
        listOf(
            -41.0 to 174.0, -43.0 to 173.0, -46.5 to 169.0, -46.5 to 166.5,
            -44.0 to 168.0, -41.5 to 171.5, -41.0 to 174.0
        ),
        // Indonesia & Philippines (Southeast Asian archipelagos)
        listOf( // Sumatra
            5.5 to 95.3, 3.0 to 98.0, -3.0 to 103.0, -5.8 to 105.8,
            -3.0 to 101.5, 2.0 to 97.0, 5.5 to 95.3
        ),
        listOf( // Borneo / Kalimantan
            7.0 to 117.0, 5.0 to 119.0, 1.0 to 119.0, -3.5 to 116.0,
            -3.0 to 110.0, 1.5 to 109.0, 4.0 to 114.0, 7.0 to 117.0
        ),
        listOf( // Java
            -6.0 to 106.0, -6.5 to 108.5, -7.5 to 112.5, -8.7 to 114.5,
            -8.0 to 110.0, -7.0 to 106.0, -6.0 to 106.0
        ),
        listOf( // New Guinea
            -2.5 to 140.0, -3.0 to 143.0, -8.0 to 147.0, -10.5 to 150.0,
            -8.0 to 143.0, -5.0 to 138.0, -1.0 to 132.0, -2.5 to 140.0
        ),
        // Antarctica
        listOf(
            -65.0 to -180.0, -65.0 to -120.0, -70.0 to -70.0, -63.0 to -60.0,
            -72.0 to 0.0, -66.0 to 60.0, -66.0 to 120.0, -68.0 to 160.0,
            -65.0 to 180.0, -90.0 to 180.0, -90.0 to -180.0, -65.0 to -180.0
        )
    )

    /**
     * Builds unit paths (normalized coordinates in [0..1] x [0..1]) for fast GPU/Canvas drawing.
     * Normalized X = (longitude + 180) / 360
     * Normalized Y = (90 - latitude) / 180
     */
    val normalizedPaths: List<Path> by lazy {
        landmasses.map { polygon ->
            val path = Path()
            polygon.forEachIndexed { index, (lat, lon) ->
                val nx = ((lon + 180.0) / 360.0).toFloat()
                val ny = ((90.0 - lat) / 180.0).toFloat()
                if (index == 0) {
                    path.moveTo(nx, ny)
                } else {
                    path.lineTo(nx, ny)
                }
            }
            path.close()
            path
        }
    }
}

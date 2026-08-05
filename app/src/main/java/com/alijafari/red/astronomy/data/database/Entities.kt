package com.alijafari.red.astronomy.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val objectId: String,
    val objectType: String,
    val savedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(tableName = "cached_cities")
data class CityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nameEn: String,
    val nameFa: String,
    val lat: Double,
    val lon: Double,
    val provinceEn: String,
    val provinceFa: String
)

@Entity(tableName = "cached_tle")
data class TleEntity(
    @PrimaryKey val noradId: Int,
    val line1: String,
    val line2: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "observation_log")
data class ObservationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val objectId: String,
    val objectName: String,
    val observedAt: Long = System.currentTimeMillis(),
    val notes: String,
    val rating: Int // 1 to 5 stars
)

@Entity(tableName = "cached_celestial_objects")
data class CelestialObjectEntity(
    @PrimaryKey val id: String,
    val typeName: String,
    val nameEn: String,
    val nameFa: String,
    val raDeg: Double,
    val decDeg: Double,
    val magnitude: Double,
    val constellationEn: String,
    val constellationFa: String,
    val distanceLightYears: Double,
    val category: String,
    val descriptionEn: String,
    val descriptionFa: String,
    val observationTipEn: String,
    val observationTipFa: String,
    val spectralType: String = "",
    val hipId: Int? = null,
    val hdId: Int? = null,
    val bayerDesignation: String = "",
    val flamsteedNumber: String = "",
    val temperatureK: Int = 0,
    val activePeakDateWindowEn: String = "",
    val activePeakDateWindowFa: String = "",
    val zhr: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_constellations")
data class ConstellationEntity(
    @PrimaryKey val code: String,
    val nameEn: String,
    val nameFa: String,
    val latinName: String,
    val mainStarsSerialized: String,
    val areaSqDeg: Double = 0.0,
    val seasonEn: String = "",
    val seasonFa: String = "",
    val hemisphereEn: String = "",
    val hemisphereFa: String = "",
    val bestViewingMonthEn: String = "",
    val bestViewingMonthFa: String = "",
    val historicalInfoEn: String = "",
    val historicalInfoFa: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

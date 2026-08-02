package com.example.data.database

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

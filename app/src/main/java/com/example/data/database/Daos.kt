package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY savedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE objectId = :objectId LIMIT 1)")
    fun isFavorite(objectId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE objectId = :objectId")
    suspend fun deleteByObjectId(objectId: String)
}

@Dao
interface SettingDao {
    @Query("SELECT * FROM settings")
    fun getAllSettings(): Flow<List<SettingEntity>>

    @Query("SELECT value FROM settings WHERE key = :key LIMIT 1")
    suspend fun getValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: SettingEntity)
}

@Dao
interface CityDao {
    @Query("SELECT * FROM cached_cities ORDER BY nameEn ASC")
    fun getAllCities(): Flow<List<CityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cities: List<CityEntity>)
}

@Dao
interface ObservationLogDao {
    @Query("SELECT * FROM observation_log ORDER BY observedAt DESC")
    fun getAllLogs(): Flow<List<ObservationLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: ObservationLogEntity)

    @Delete
    suspend fun delete(log: ObservationLogEntity)
}

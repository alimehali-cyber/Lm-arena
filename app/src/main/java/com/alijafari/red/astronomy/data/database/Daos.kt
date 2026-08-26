package com.alijafari.red.astronomy.data.database

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
interface FavoriteLocationDao {
    @Query("SELECT * FROM favorite_locations ORDER BY savedAt DESC LIMIT 5")
    fun getAllFavoriteLocationsFlow(): Flow<List<FavoriteLocationEntity>>

    @Query("SELECT * FROM favorite_locations ORDER BY savedAt DESC LIMIT 5")
    suspend fun getAllFavoriteLocationsDirect(): List<FavoriteLocationEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_locations WHERE id = :id LIMIT 1)")
    fun isFavoriteLocationFlow(id: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(location: FavoriteLocationEntity)

    @Query("DELETE FROM favorite_locations WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM favorite_locations")
    suspend fun getCount(): Int
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

@Dao
interface CelestialObjectDao {
    @Query("SELECT * FROM cached_celestial_objects ORDER BY magnitude ASC")
    fun getAllObjectsFlow(): Flow<List<CelestialObjectEntity>>

    @Query("SELECT * FROM cached_celestial_objects ORDER BY magnitude ASC")
    suspend fun getAllObjectsDirect(): List<CelestialObjectEntity>

    @Query("SELECT * FROM cached_celestial_objects WHERE id = :id LIMIT 1")
    suspend fun getObjectById(id: String): CelestialObjectEntity?

    @Query("SELECT COUNT(*) FROM cached_celestial_objects")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(objects: List<CelestialObjectEntity>)

    @Query("DELETE FROM cached_celestial_objects")
    suspend fun deleteAll()
}

@Dao
interface ConstellationDao {
    @Query("SELECT * FROM cached_constellations ORDER BY nameEn ASC")
    fun getAllConstellationsFlow(): Flow<List<ConstellationEntity>>

    @Query("SELECT * FROM cached_constellations ORDER BY nameEn ASC")
    suspend fun getAllConstellationsDirect(): List<ConstellationEntity>

    @Query("SELECT COUNT(*) FROM cached_constellations")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(constellations: List<ConstellationEntity>)
}

@Dao
interface TleDao {
    @Query("SELECT * FROM cached_tle")
    suspend fun getAllTles(): List<TleEntity>

    @Query("SELECT * FROM cached_tle WHERE noradId = :noradId LIMIT 1")
    suspend fun getTleById(noradId: Int): TleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(tle: TleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tles: List<TleEntity>)
}

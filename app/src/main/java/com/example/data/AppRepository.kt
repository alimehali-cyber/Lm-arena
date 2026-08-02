package com.example.data

import com.example.data.database.*
import com.example.domain.AppLanguage
import com.example.domain.CalendarSystem
import com.example.domain.UserLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class AppRepository(private val db: AppDatabase) {

    val favoritesFlow: Flow<List<FavoriteEntity>> = db.favoriteDao().getAllFavorites()
    val observationLogsFlow: Flow<List<ObservationLogEntity>> = db.observationLogDao().getAllLogs()

    fun isFavorite(objectId: String): Flow<Boolean> = db.favoriteDao().isFavorite(objectId)

    suspend fun toggleFavorite(objectId: String, objectType: String) {
        val favDao = db.favoriteDao()
        val exists = favDao.isFavorite(objectId).firstOrNull() ?: false
        if (exists) {
            favDao.deleteByObjectId(objectId)
        } else {
            favDao.insert(FavoriteEntity(objectId = objectId, objectType = objectType))
        }
    }

    suspend fun addObservationLog(objectId: String, objectName: String, notes: String, rating: Int) {
        db.observationLogDao().insert(
            ObservationLogEntity(
                objectId = objectId,
                objectName = objectName,
                notes = notes,
                rating = rating
            )
        )
    }

    suspend fun deleteObservationLog(log: ObservationLogEntity) {
        db.observationLogDao().delete(log)
    }

    suspend fun saveSetting(key: String, value: String) {
        db.settingDao().setSetting(SettingEntity(key, value))
    }

    suspend fun getSetting(key: String, defaultValue: String): String {
        return db.settingDao().getValue(key) ?: defaultValue
    }
}

package com.alijafari.red.astronomy.data

import com.alijafari.red.astronomy.astro_engine.TimeEngine
import com.alijafari.red.astronomy.data.catalog.AstronomyCatalog
import com.alijafari.red.astronomy.data.catalog.GeoCity
import com.alijafari.red.astronomy.data.catalog.GeoLocationCatalog
import com.alijafari.red.astronomy.data.database.*
import com.alijafari.red.astronomy.domain.CelestialObject
import com.alijafari.red.astronomy.domain.ConstellationData
import com.alijafari.red.astronomy.domain.ObjectType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class AppRepository(private val db: AppDatabase) {

    val favoritesFlow: Flow<List<FavoriteEntity>> = db.favoriteDao().getAllFavorites()
    val favoriteLocationsFlow: Flow<List<FavoriteLocationEntity>> = db.favoriteLocationDao().getAllFavoriteLocationsFlow()
    val observationLogsFlow: Flow<List<ObservationLogEntity>> = db.observationLogDao().getAllLogs()
    val userOccasionsFlow: Flow<List<UserOccasionEntity>> = db.userOccasionDao().getAllOccasionsFlow()

    suspend fun getUserOccasionsCount(): Int = db.userOccasionDao().getCount()

    suspend fun saveUserOccasion(id: String?, title: String, timestampMs: Long): Boolean {
        val dao = db.userOccasionDao()
        if (id == null) {
            val count = dao.getCount()
            if (count >= 20) {
                return false
            }
            val newId = "occ_${System.currentTimeMillis()}_${(1000..9999).random()}"
            dao.insert(UserOccasionEntity(id = newId, title = title.trim(), timestampMs = timestampMs))
            return true
        } else {
            dao.update(UserOccasionEntity(id = id, title = title.trim(), timestampMs = timestampMs))
            return true
        }
    }

    suspend fun deleteUserOccasion(id: String) {
        db.userOccasionDao().deleteById(id)
    }

    fun isFavorite(objectId: String): Flow<Boolean> = db.favoriteDao().isFavorite(objectId)

    suspend fun toggleFavoriteLocation(city: GeoCity): Boolean {
        val favLocDao = db.favoriteLocationDao()
        val exists = favLocDao.isFavoriteLocationFlow(city.id).firstOrNull() ?: false
        if (exists) {
            favLocDao.deleteById(city.id)
            return false
        } else {
            val count = favLocDao.getCount()
            if (count < 5) {
                favLocDao.insert(
                    FavoriteLocationEntity(
                        id = city.id,
                        nameEn = city.nameEn,
                        nameFa = city.nameFa,
                        lat = city.latitude,
                        lon = city.longitude,
                        elevationMeters = city.elevationMeters,
                        timezoneId = city.timezoneId,
                        countryEn = city.countryEn,
                        countryFa = city.countryFa,
                        provinceEn = city.provinceEn,
                        provinceFa = city.provinceFa,
                        isIran = city.isIran,
                        isCapital = city.isCapital
                    )
                )
                return true
            }
            return false
        }
    }

    suspend fun removeFavoriteLocation(id: String) {
        db.favoriteLocationDao().deleteById(id)
    }

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

    /**
     * Seeds or refreshes Room local database cache for celestial objects and constellations.
     */
    suspend fun syncCelestialCatalogIfNeeded() {
        val objDao = db.celestialObjectDao()
        val constDao = db.constellationDao()

        if (objDao.getCount() == 0) {
            val allObjects = AstronomyCatalog.getAllObjects()
            val entities = allObjects.map { obj ->
                CelestialObjectEntity(
                    id = obj.id,
                    typeName = obj.type.name,
                    nameEn = obj.nameEn,
                    nameFa = obj.nameFa,
                    raDeg = obj.raDeg,
                    decDeg = obj.decDeg,
                    magnitude = obj.magnitude,
                    constellationEn = obj.constellationEn,
                    constellationFa = obj.constellationFa,
                    distanceLightYears = obj.distanceLightYears,
                    category = obj.category,
                    descriptionEn = obj.descriptionEn,
                    descriptionFa = obj.descriptionFa,
                    observationTipEn = obj.observationTipEn,
                    observationTipFa = obj.observationTipFa,
                    spectralType = obj.spectralType,
                    hipId = obj.hipId,
                    hdId = obj.hdId,
                    bayerDesignation = obj.bayerDesignation,
                    flamsteedNumber = obj.flamsteedNumber,
                    temperatureK = obj.temperatureK,
                    activePeakDateWindowEn = obj.activePeakDateWindowEn,
                    activePeakDateWindowFa = obj.activePeakDateWindowFa,
                    zhr = obj.zhr
                )
            }
            objDao.insertAll(entities)
        }

        if (constDao.getCount() == 0) {
            val constellations = AstronomyCatalog.DEFAULT_CONSTELLATIONS
            val entities = constellations.map { c ->
                ConstellationEntity(
                    code = c.code,
                    nameEn = c.nameEn,
                    nameFa = c.nameFa,
                    latinName = c.latinName,
                    mainStarsSerialized = c.mainStars.joinToString(";") { "${it.first},${it.second}" },
                    areaSqDeg = c.areaSqDeg,
                    seasonEn = c.seasonEn,
                    seasonFa = c.seasonFa,
                    hemisphereEn = c.hemisphereEn,
                    hemisphereFa = c.hemisphereFa,
                    bestViewingMonthEn = c.bestViewingMonthEn,
                    bestViewingMonthFa = c.bestViewingMonthFa,
                    historicalInfoEn = c.historicalInfoEn,
                    historicalInfoFa = c.historicalInfoFa
                )
            }
            constDao.insertAll(entities)
        }
    }

    /**
     * Returns celestial objects from Room local cache first, recalculated for dynamic positions if needed.
     */
    suspend fun getCachedCelestialObjects(jd: Double = TimeEngine.getJulianDate()): List<CelestialObject> {
        syncCelestialCatalogIfNeeded()
        val cachedEntities = db.celestialObjectDao().getAllObjectsDirect()
        
        if (cachedEntities.isEmpty()) {
            return AstronomyCatalog.getAllObjects(jd)
        }

        val baseObjects = cachedEntities.map { entity ->
            val type = try {
                ObjectType.valueOf(entity.typeName)
            } catch (e: Exception) {
                ObjectType.STAR
            }
            CelestialObject(
                id = entity.id,
                type = type,
                nameEn = entity.nameEn,
                nameFa = entity.nameFa,
                raDeg = entity.raDeg,
                decDeg = entity.decDeg,
                magnitude = entity.magnitude,
                constellationEn = entity.constellationEn,
                constellationFa = entity.constellationFa,
                distanceLightYears = entity.distanceLightYears,
                category = entity.category,
                descriptionEn = entity.descriptionEn,
                descriptionFa = entity.descriptionFa,
                observationTipEn = entity.observationTipEn,
                observationTipFa = entity.observationTipFa,
                spectralType = entity.spectralType,
                hipId = entity.hipId,
                hdId = entity.hdId,
                bayerDesignation = entity.bayerDesignation,
                flamsteedNumber = entity.flamsteedNumber,
                temperatureK = entity.temperatureK,
                activePeakDateWindowEn = entity.activePeakDateWindowEn,
                activePeakDateWindowFa = entity.activePeakDateWindowFa,
                zhr = entity.zhr
            )
        }

        // Apply dynamic ephemeris updates to Sun, Moon, and Planets
        val catalogMap = AstronomyCatalog.getAllObjects(jd).associateBy { it.id }
        return baseObjects.map { obj ->
            catalogMap[obj.id]?.let { dynamicObj ->
                obj.copy(raDeg = dynamicObj.raDeg, decDeg = dynamicObj.decDeg, magnitude = dynamicObj.magnitude)
            } ?: obj
        }
    }

    /**
     * Returns constellations from Room local cache.
     */
    suspend fun getCachedConstellations(): List<ConstellationData> {
        syncCelestialCatalogIfNeeded()
        val cachedEntities = db.constellationDao().getAllConstellationsDirect()

        if (cachedEntities.isEmpty()) {
            return AstronomyCatalog.DEFAULT_CONSTELLATIONS
        }

        return cachedEntities.map { entity ->
            val stars = entity.mainStarsSerialized.split(";").mapNotNull { pairStr ->
                val parts = pairStr.split(",")
                if (parts.size == 2) {
                    val ra = parts[0].toDoubleOrNull()
                    val dec = parts[1].toDoubleOrNull()
                    if (ra != null && dec != null) ra to dec else null
                } else null
            }
            ConstellationData(
                code = entity.code,
                nameEn = entity.nameEn,
                nameFa = entity.nameFa,
                latinName = entity.latinName,
                mainStars = stars,
                areaSqDeg = entity.areaSqDeg,
                seasonEn = entity.seasonEn,
                seasonFa = entity.seasonFa,
                hemisphereEn = entity.hemisphereEn,
                hemisphereFa = entity.hemisphereFa,
                bestViewingMonthEn = entity.bestViewingMonthEn,
                bestViewingMonthFa = entity.bestViewingMonthFa,
                historicalInfoEn = entity.historicalInfoEn,
                historicalInfoFa = entity.historicalInfoFa
            )
        }
    }
}


package com.alijafari.red.astronomy.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alijafari.red.astronomy.astro_engine.AstroDispatchEngine
import com.alijafari.red.astronomy.data.AppRepository
import com.alijafari.red.astronomy.data.catalog.AstronomyCatalog
import com.alijafari.red.astronomy.data.catalog.CanonicalAstroCatalog
import com.alijafari.red.astronomy.data.database.AppDatabase
import com.alijafari.red.astronomy.data.database.ObservationLogEntity
import com.alijafari.red.astronomy.domain.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MainUiState(
    val language: AppLanguage = AppLanguage.PERSIAN,
    val calendarSystem: CalendarSystem = CalendarSystem.SOLAR_HIJRI,
    val themeMode: ThemeMode = ThemeMode.DARK_NAVY,
    val skyCanvasTheme: SkyCanvasTheme = SkyCanvasTheme.CELESTIAL,
    val userLocation: UserLocation = UserLocation(),
    val selectedTab: Int = 4,
    val searchQuery: String = "",
    val selectedObjectForDetail: CelestialObject? = null,
    val isDetailFavorite: Boolean = false,
    val showFavoritesDialog: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val showObservationLogDialog: Boolean = false,
    val bortleClass: Int = 3,
    val favoritesList: List<String> = emptyList(),
    val observationLogs: List<ObservationLogEntity> = emptyList(),
    val timeMachineState: TimeMachineState = TimeMachineState(),
    val selectedTargetObject: CelestialObject? = null,
    val selectedSatelliteId: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(AppDatabase.getDatabase(application))
    private val prefs = application.getSharedPreferences("astro_app_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        MainUiState(
            language = try {
                AppLanguage.valueOf(prefs.getString("app_language", AppLanguage.PERSIAN.name) ?: AppLanguage.PERSIAN.name)
            } catch (e: Exception) {
                AppLanguage.PERSIAN
            },
            calendarSystem = try {
                CalendarSystem.valueOf(prefs.getString("calendar_system", CalendarSystem.SOLAR_HIJRI.name) ?: CalendarSystem.SOLAR_HIJRI.name)
            } catch (e: Exception) {
                CalendarSystem.SOLAR_HIJRI
            },
            skyCanvasTheme = try {
                SkyCanvasTheme.valueOf(prefs.getString("sky_canvas_theme", SkyCanvasTheme.CELESTIAL.name) ?: SkyCanvasTheme.CELESTIAL.name)
            } catch (e: Exception) {
                SkyCanvasTheme.CELESTIAL
            },
            themeMode = try {
                ThemeMode.valueOf(prefs.getString("theme_mode", ThemeMode.DARK_NAVY.name) ?: ThemeMode.DARK_NAVY.name)
            } catch (e: Exception) {
                ThemeMode.DARK_NAVY
            },
            bortleClass = prefs.getInt("bortle_class", 3),
            userLocation = run {
                val savedCityEn = prefs.getString("user_city_name_en", null)
                val savedCityFa = prefs.getString("user_city_name_fa", null)
                val savedLat = if (prefs.contains("user_latitude")) {
                    prefs.getFloat("user_latitude", 30.1141f).toDouble()
                } else if (prefs.contains("user_lat")) {
                    prefs.getFloat("user_lat", 30.1141f).toDouble()
                } else {
                    null
                }
                val savedLon = if (prefs.contains("user_longitude")) {
                    prefs.getFloat("user_longitude", 51.5217f).toDouble()
                } else if (prefs.contains("user_lon")) {
                    prefs.getFloat("user_lon", 51.5217f).toDouble()
                } else {
                    null
                }
                val bortle = prefs.getInt("bortle_class", 3)

                if (savedCityEn != null && savedCityFa != null && savedLat != null && savedLon != null) {
                    UserLocation(
                        cityNameEn = savedCityEn,
                        cityNameFa = savedCityFa,
                        latitude = savedLat,
                        longitude = savedLon,
                        bortleClass = bortle
                    )
                } else {
                    UserLocation(bortleClass = bortle)
                }
            }
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        // Sync local database cache for offline accessibility
        viewModelScope.launch {
            repository.syncCelestialCatalogIfNeeded()
        }

        // Observe DB favorites and observation logs
        viewModelScope.launch {
            repository.favoritesFlow.collect { favs ->
                val ids = favs.map { it.objectId }
                _uiState.update { it.copy(favoritesList = ids) }
                // Update detail favorite state if object is open
                _uiState.value.selectedObjectForDetail?.let { currentObj ->
                    _uiState.update { it.copy(isDetailFavorite = ids.contains(currentObj.id)) }
                }
            }
        }

        viewModelScope.launch {
            repository.observationLogsFlow.collect { logs ->
                _uiState.update { it.copy(observationLogs = logs) }
            }
        }
    }

    fun selectTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    fun setLanguage(language: AppLanguage) {
        prefs.edit().putString("app_language", language.name).apply()
        _uiState.update { it.copy(language = language) }
    }

    fun setCalendarSystem(calendarSystem: CalendarSystem) {
        prefs.edit().putString("calendar_system", calendarSystem.name).apply()
        _uiState.update { it.copy(calendarSystem = calendarSystem) }
    }

    fun setThemeMode(themeMode: ThemeMode) {
        prefs.edit().putString("theme_mode", themeMode.name).apply()
        _uiState.update { it.copy(themeMode = themeMode) }
    }

    fun setSkyCanvasTheme(theme: SkyCanvasTheme) {
        prefs.edit().putString("sky_canvas_theme", theme.name).apply()
        _uiState.update { it.copy(skyCanvasTheme = theme) }
    }

    fun setLocation(cityEn: String, cityFa: String, lat: Double, lon: Double) {
        val newLoc = UserLocation(
            cityNameEn = cityEn,
            cityNameFa = cityFa,
            latitude = lat,
            longitude = lon,
            bortleClass = _uiState.value.bortleClass
        )
        // Persist to SharedPreferences so location remains across app restarts
        prefs.edit()
            .putString("user_city_name_en", cityEn)
            .putString("user_city_name_fa", cityFa)
            .putFloat("user_latitude", lat.toFloat())
            .putFloat("user_longitude", lon.toFloat())
            .putFloat("user_lat", lat.toFloat())
            .putFloat("user_lon", lon.toFloat())
            .apply()

        // Also persist to astro_prefs for notification and background workers
        val astroPrefs = getApplication<Application>().getSharedPreferences("astro_prefs", Context.MODE_PRIVATE)
        astroPrefs.edit()
            .putString("user_city_name_en", cityEn)
            .putString("user_city_name_fa", cityFa)
            .putFloat("user_lat", lat.toFloat())
            .putFloat("user_lon", lon.toFloat())
            .apply()

        _uiState.update { it.copy(userLocation = newLoc) }
        com.alijafari.red.astronomy.notification.AstroNotificationManager.handleLocationChanged(getApplication(), newLoc)
    }

    fun selectSatelliteById(satId: String?) {
        _uiState.update { it.copy(selectedSatelliteId = satId) }
    }

    fun setBortleClass(bortle: Int) {
        prefs.edit().putInt("bortle_class", bortle).apply()
        _uiState.update {
            it.copy(
                bortleClass = bortle,
                userLocation = it.userLocation.copy(bortleClass = bortle)
            )
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun openObjectDetail(obj: CelestialObject) {
        val canonicalId = CanonicalAstroCatalog.resolveCanonicalId(obj.id)
        val canonicalObj = CanonicalAstroCatalog.getCanonicalObject(canonicalId)
        val finalObj = if (canonicalObj != null) {
            val calc = AstroDispatchEngine.calculateState(
                idOrAlias = canonicalObj.canonicalId,
                timestampMs = System.currentTimeMillis(),
                userLatDeg = _uiState.value.userLocation.latitude,
                userLonDeg = _uiState.value.userLocation.longitude
            )
            CanonicalAstroCatalog.toCelestialObject(
                canonicalObj = canonicalObj,
                dynamicRa = calc?.raDeg ?: obj.raDeg,
                dynamicDec = calc?.decDeg ?: obj.decDeg,
                dynamicMag = calc?.magnitude ?: obj.magnitude
            )
        } else {
            obj
        }
        val isFav = _uiState.value.favoritesList.contains(finalObj.id) || _uiState.value.favoritesList.contains(obj.id)
        _uiState.update {
            it.copy(
                selectedObjectForDetail = finalObj,
                isDetailFavorite = isFav
            )
        }
    }

    fun openObjectDetailById(idOrAlias: String) {
        val canonicalId = CanonicalAstroCatalog.resolveCanonicalId(idOrAlias)
        val canonicalObj = CanonicalAstroCatalog.getCanonicalObject(canonicalId)
        if (canonicalObj != null) {
            val calc = AstroDispatchEngine.calculateState(
                idOrAlias = canonicalObj.canonicalId,
                timestampMs = System.currentTimeMillis(),
                userLatDeg = _uiState.value.userLocation.latitude,
                userLonDeg = _uiState.value.userLocation.longitude
            )
            val celObj = CanonicalAstroCatalog.toCelestialObject(
                canonicalObj = canonicalObj,
                dynamicRa = calc?.raDeg ?: 0.0,
                dynamicDec = calc?.decDeg ?: 0.0,
                dynamicMag = calc?.magnitude ?: canonicalObj.physicalProperties.magnitude
            )
            openObjectDetail(celObj)
        } else {
            AstronomyCatalog.getById(idOrAlias)?.let { openObjectDetail(it) }
        }
    }

    fun closeObjectDetail() {
        _uiState.update { it.copy(selectedObjectForDetail = null) }
    }

    fun toggleCurrentDetailFavorite() {
        val obj = _uiState.value.selectedObjectForDetail ?: return
        viewModelScope.launch {
            repository.toggleFavorite(obj.id, obj.type.name)
        }
    }

    fun toggleFavoriteById(objectId: String, objectType: String) {
        viewModelScope.launch {
            repository.toggleFavorite(objectId, objectType)
        }
    }

    fun setShowFavoritesDialog(show: Boolean) {
        _uiState.update { it.copy(showFavoritesDialog = show) }
    }

    fun setShowSettingsDialog(show: Boolean) {
        _uiState.update { it.copy(showSettingsDialog = show) }
    }

    fun setShowObservationLogDialog(show: Boolean) {
        _uiState.update { it.copy(showObservationLogDialog = show) }
    }

    fun addObservationLog(notes: String, rating: Int) {
        val obj = _uiState.value.selectedObjectForDetail ?: return
        val name = if (_uiState.value.language == AppLanguage.PERSIAN) obj.nameFa else obj.nameEn
        viewModelScope.launch {
            repository.addObservationLog(obj.id, name, notes, rating)
        }
    }

    fun deleteObservationLog(log: ObservationLogEntity) {
        viewModelScope.launch {
            repository.deleteObservationLog(log)
        }
    }

    // Time Machine Functions
    fun setSimulatedTime(timeMs: Long, eventName: String? = null, isBirthday: Boolean = false) {
        val clamped = timeMs.coerceIn(TimeMachineState.MIN_TIMESTAMP_MS, TimeMachineState.MAX_TIMESTAMP_MS)
        _uiState.update {
            it.copy(
                timeMachineState = it.timeMachineState.copy(
                    mode = TimeMachineMode.SIMULATION,
                    simulationTimeMs = clamped,
                    eventName = eventName,
                    isBirthdayMode = isBirthday
                )
            )
        }
    }

    fun setTimeMachineMode(mode: TimeMachineMode) {
        _uiState.update {
            val currentSimTime = if (mode == TimeMachineMode.SIMULATION && it.timeMachineState.mode == TimeMachineMode.LIVE) {
                System.currentTimeMillis()
            } else {
                it.timeMachineState.simulationTimeMs
            }
            it.copy(
                timeMachineState = it.timeMachineState.copy(
                    mode = mode,
                    simulationTimeMs = currentSimTime
                )
            )
        }
    }

    fun toggleTimeMachinePlaying() {
        _uiState.update {
            it.copy(
                timeMachineState = it.timeMachineState.copy(
                    isPlaying = !it.timeMachineState.isPlaying
                )
            )
        }
    }

    fun setTimeMachinePlaying(playing: Boolean) {
        _uiState.update {
            it.copy(
                timeMachineState = it.timeMachineState.copy(isPlaying = playing)
            )
        }
    }

    fun setTimeMachineSpeed(speed: TimeSimulationSpeed) {
        _uiState.update {
            it.copy(
                timeMachineState = it.timeMachineState.copy(speed = speed)
            )
        }
    }

    fun toggleTimeMachineReverse() {
        _uiState.update {
            it.copy(
                timeMachineState = it.timeMachineState.copy(isReverse = !it.timeMachineState.isReverse)
            )
        }
    }

    fun toggleTimeMachineExpanded() {
        _uiState.update {
            it.copy(
                timeMachineState = it.timeMachineState.copy(isExpanded = !it.timeMachineState.isExpanded)
            )
        }
    }

    fun setTimeMachineExpanded(expanded: Boolean) {
        _uiState.update {
            it.copy(
                timeMachineState = it.timeMachineState.copy(isExpanded = expanded)
            )
        }
    }

    fun returnToLiveTime() {
        _uiState.update {
            it.copy(
                timeMachineState = it.timeMachineState.copy(
                    mode = TimeMachineMode.LIVE,
                    isPlaying = false,
                    eventName = null,
                    isBirthdayMode = false
                )
            )
        }
    }

    fun locateObjectInAR(obj: CelestialObject) {
        _uiState.update {
            it.copy(
                selectedTargetObject = obj,
                selectedObjectForDetail = null,
                selectedTab = 3
            )
        }
    }

    fun clearTargetObject() {
        _uiState.update {
            it.copy(selectedTargetObject = null)
        }
    }
}

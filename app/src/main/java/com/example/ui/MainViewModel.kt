package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppRepository
import com.example.data.catalog.AstronomyCatalog
import com.example.data.database.AppDatabase
import com.example.data.database.ObservationLogEntity
import com.example.domain.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MainUiState(
    val language: AppLanguage = AppLanguage.PERSIAN,
    val calendarSystem: CalendarSystem = CalendarSystem.SOLAR_HIJRI,
    val themeMode: ThemeMode = ThemeMode.DARK_NAVY,
    val userLocation: UserLocation = UserLocation(),
    val selectedTab: Int = 0,
    val searchQuery: String = "",
    val selectedObjectForDetail: CelestialObject? = null,
    val isDetailFavorite: Boolean = false,
    val showFavoritesDialog: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val showObservationLogDialog: Boolean = false,
    val bortleClass: Int = 3,
    val favoritesList: List<String> = emptyList(),
    val observationLogs: List<ObservationLogEntity> = emptyList()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(AppDatabase.getDatabase(application))

    private val _uiState = MutableStateFlow(MainUiState())
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
        _uiState.update { it.copy(language = language) }
    }

    fun setCalendarSystem(calendarSystem: CalendarSystem) {
        _uiState.update { it.copy(calendarSystem = calendarSystem) }
    }

    fun setThemeMode(themeMode: ThemeMode) {
        _uiState.update { it.copy(themeMode = themeMode) }
    }

    fun setLocation(cityEn: String, cityFa: String, lat: Double, lon: Double) {
        _uiState.update {
            it.copy(
                userLocation = UserLocation(
                    cityNameEn = cityEn,
                    cityNameFa = cityFa,
                    latitude = lat,
                    longitude = lon,
                    bortleClass = it.bortleClass
                )
            )
        }
    }

    fun setBortleClass(bortle: Int) {
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
        val isFav = _uiState.value.favoritesList.contains(obj.id)
        _uiState.update {
            it.copy(
                selectedObjectForDetail = obj,
                isDetailFavorite = isFav
            )
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
}

package com.danylo.seriesdiary.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.danylo.seriesdiary.data.AppDatabase
import com.danylo.seriesdiary.data.SeriesRepository
import com.danylo.seriesdiary.data.SettingsDataStore
import com.danylo.seriesdiary.data.TvSeriesEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch



sealed interface ListUiState {
    object Loading : ListUiState
    data class Success(val series: List<TvSeriesEntity>) : ListUiState
    data class Error(val message: String) : ListUiState
}

class ListViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = SeriesRepository(db.tvSeriesDao())
    private val settings = SettingsDataStore(application)

    private val _showOnlyFavorites = MutableStateFlow(false)
    val showOnlyFavorites: StateFlow<Boolean> = _showOnlyFavorites.asStateFlow()

    val uiState: StateFlow<ListUiState> = combine(
        repository.getAllSeries(),
        _showOnlyFavorites,
        settings.defaultSortByRating,
        settings.showEndedSeries
    ) { allSeries, favOnly, sortByRating, showEnded ->
        var list = if (favOnly) allSeries.filter { it.isFavorite } else allSeries
        if (!showEnded) list = list.filter { it.status != "ENDED" }
        list = if (sortByRating) list.sortedByDescending { it.rating }
        else list.sortedBy { it.title }
        ListUiState.Success(list)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListUiState.Loading)

    init {
        viewModelScope.launch { repository.seedIfEmpty() }
    }

    fun toggleFavorites(show: Boolean) {
        _showOnlyFavorites.value = show
    }

    fun toggleFavorite(series: TvSeriesEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(series.id, !series.isFavorite)
        }
    }
}



sealed interface DetailsUiState {
    object Loading : DetailsUiState
    data class Success(val series: TvSeriesEntity, val extraInfo: String) : DetailsUiState
    data class Error(val message: String) : DetailsUiState
}

class DetailsViewModel(
    application: Application,
    private val seriesTitle: String
) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = SeriesRepository(db.tvSeriesDao())

    private val _uiState = MutableStateFlow<DetailsUiState>(DetailsUiState.Loading)
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    init {
        loadDetails()
    }

    private fun loadDetails() {
        viewModelScope.launch {
            val series = repository.getByTitle(seriesTitle)
            if (series != null) {
                val extraInfo = if (series.rating > 8.5) "Справжній хіт!" else "Гарний вибір."
                _uiState.value = DetailsUiState.Success(series, extraInfo)
            } else {
                _uiState.value = DetailsUiState.Error("Серіал не знайдено")
            }
        }
    }

    class Factory(
        private val application: Application,
        private val seriesTitle: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DetailsViewModel(application, seriesTitle) as T
        }
    }
}


class GridViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = SeriesRepository(db.tvSeriesDao())

    private val _sortByRating = MutableStateFlow(false)
    val sortByRating: StateFlow<Boolean> = _sortByRating.asStateFlow()

    val series: StateFlow<List<TvSeriesEntity>> = combine(
        repository.getAllSeries(),
        _sortByRating
    ) { allSeries, byRating ->
        if (byRating) allSeries.sortedByDescending { it.rating }
        else allSeries.sortedBy { it.title }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch { repository.seedIfEmpty() }
    }

    fun setSortByRating(byRating: Boolean) {
        _sortByRating.value = byRating
    }
}


class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = SettingsDataStore(application)

    val userName: StateFlow<String> = settings.userName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val defaultSortByRating: StateFlow<Boolean> = settings.defaultSortByRating
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val showEndedSeries: StateFlow<Boolean> = settings.showEndedSeries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun updateName(name: String) {
        viewModelScope.launch { settings.saveUserName(name) }
    }

    fun setDefaultSortByRating(value: Boolean) {
        viewModelScope.launch { settings.saveDefaultSortByRating(value) }
    }

    fun setShowEndedSeries(value: Boolean) {
        viewModelScope.launch { settings.saveShowEndedSeries(value) }
    }
}


class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = SettingsDataStore(application)

    val savedName: StateFlow<String> = settings.userName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            settings.userName.collect {
                _isLoading.value = false
            }
        }
    }

    fun saveName(name: String) {
        viewModelScope.launch { settings.saveUserName(name) }
    }
}

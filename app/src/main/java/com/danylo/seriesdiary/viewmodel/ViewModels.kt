package com.danylo.seriesdiary.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.danylo.seriesdiary.data.AppDatabase
import com.danylo.seriesdiary.data.FetchResult
import com.danylo.seriesdiary.data.SeriesRepository
import com.danylo.seriesdiary.data.SettingsDataStore
import com.danylo.seriesdiary.data.TvSeriesEntity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


sealed interface ListUiState {
    object Loading : ListUiState
    data class Success(
        val series: List<TvSeriesEntity>,
        val isFromCache: Boolean
    ) : ListUiState
    data class Error(val message: String) : ListUiState
}

private sealed interface LoadStatus {
    object Loading : LoadStatus
    object Loaded : LoadStatus
    object Offline : LoadStatus
    data class Error(val message: String) : LoadStatus
}

class ListViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = SeriesRepository(db.tvSeriesDao())
    private val settings = SettingsDataStore(application)

    private val _showOnlyFavorites = MutableStateFlow(false)
    val showOnlyFavorites: StateFlow<Boolean> = _showOnlyFavorites.asStateFlow()

    private val _loadStatus = MutableStateFlow<LoadStatus>(LoadStatus.Loading)

    private val _isMutating = MutableStateFlow(false)
    val isMutating: StateFlow<Boolean> = _isMutating.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _errorEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val errorEvents = _errorEvents.asSharedFlow()

    val uiState: StateFlow<ListUiState> = combine(
        repository.observeCached(),
        _loadStatus,
        _showOnlyFavorites,
        settings.defaultSortByRating,
        settings.showEndedSeries
    ) { cached, status, favOnly, sortByRating, showEnded ->
        val filtered = applyFilters(cached, favOnly, sortByRating, showEnded)
        when (status) {
            LoadStatus.Loading ->
                if (cached.isEmpty()) ListUiState.Loading
                else ListUiState.Success(filtered, isFromCache = true)
            LoadStatus.Loaded -> ListUiState.Success(filtered, isFromCache = false)
            LoadStatus.Offline -> ListUiState.Success(filtered, isFromCache = true)
            is LoadStatus.Error ->
                if (cached.isNotEmpty()) ListUiState.Success(filtered, isFromCache = true)
                else ListUiState.Error(status.message)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListUiState.Loading)

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _loadStatus.value = LoadStatus.Loading
            when (val result = repository.refreshSeries()) {
                is FetchResult.Success -> _loadStatus.value = LoadStatus.Loaded
                is FetchResult.Offline -> _loadStatus.value = LoadStatus.Offline
                is FetchResult.Error -> _loadStatus.value = LoadStatus.Error(result.message)
            }
        }
    }

    fun pullToRefresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(900)
            when (val result = repository.refreshSeries()) {
                is FetchResult.Success -> _loadStatus.value = LoadStatus.Loaded
                is FetchResult.Offline -> _loadStatus.value = LoadStatus.Offline
                is FetchResult.Error -> {
                    _loadStatus.value = LoadStatus.Error(result.message)
                    _errorEvents.tryEmit(result.message)
                }
            }
            _isRefreshing.value = false
        }
    }

    private fun applyFilters(
        list: List<TvSeriesEntity>,
        favOnly: Boolean,
        sortByRating: Boolean,
        showEnded: Boolean
    ): List<TvSeriesEntity> {
        var result = list
        if (favOnly) result = result.filter { it.isFavorite }
        if (!showEnded) result = result.filter { it.status != "ENDED" }
        return if (sortByRating) result.sortedByDescending { it.rating }
        else result.sortedBy { it.title }
    }

    fun toggleFavorites(show: Boolean) {
        _showOnlyFavorites.value = show
    }

    fun toggleFavorite(series: TvSeriesEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(series.id, !series.isFavorite)
        }
    }

    fun deleteSeries(id: String) {
        if (_isMutating.value) return
        viewModelScope.launch {
            _isMutating.value = true
            when (val result = repository.deleteSeries(id)) {
                is FetchResult.Success -> { /* cached Flow emits -> UI updates */ }
                is FetchResult.Offline -> _errorEvents.tryEmit("Офлайн-режим: видалення недоступне")
                is FetchResult.Error -> _errorEvents.tryEmit(result.message)
            }
            _isMutating.value = false
        }
    }
}


sealed interface DetailsUiState {
    object Loading : DetailsUiState
    data class Success(
        val series: TvSeriesEntity,
        val extraInfo: String,
        val isFromCache: Boolean
    ) : DetailsUiState
    data class Error(val message: String) : DetailsUiState
}

class DetailsViewModel(
    application: Application,
    private val seriesId: String
) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = SeriesRepository(db.tvSeriesDao())

    private val _uiState = MutableStateFlow<DetailsUiState>(DetailsUiState.Loading)
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    init {
        loadDetails()
        observeLocalChanges()
    }

    fun loadDetails() {
        viewModelScope.launch {
            _uiState.value = DetailsUiState.Loading
            when (val result = repository.fetchSeriesById(seriesId)) {
                is FetchResult.Success -> {
                    val extraInfo = extraInfoFor(result.data.rating)
                    _uiState.value = DetailsUiState.Success(result.data, extraInfo, false)
                }
                is FetchResult.Offline -> {
                    val extraInfo = extraInfoFor(result.cached.rating)
                    _uiState.value = DetailsUiState.Success(result.cached, extraInfo, true)
                }
                is FetchResult.Error -> _uiState.value = DetailsUiState.Error(result.message)
            }
        }
    }

    /**
     * Реактивно стежить за змінами цього серіалу у Room — щойно photoPath оновився,
     * UI отримує новий state без явного reload.
     */
    private fun observeLocalChanges() {
        viewModelScope.launch {
            repository.observeCached().collect { list ->
                val current = list.firstOrNull { it.id == seriesId } ?: return@collect
                val state = _uiState.value
                if (state is DetailsUiState.Success && state.series != current) {
                    _uiState.value = state.copy(series = current)
                }
            }
        }
    }

    fun setPhoto(path: String?) {
        viewModelScope.launch {
            repository.updatePhoto(seriesId, path)
        }
    }

    private fun extraInfoFor(rating: Double): String =
        if (rating > 8.5) "Справжній хіт!" else "Гарний вибір."

    class Factory(
        private val application: Application,
        private val seriesId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DetailsViewModel(application, seriesId) as T
        }
    }
}


sealed interface GridUiState {
    object Loading : GridUiState
    data class Success(
        val series: List<TvSeriesEntity>,
        val isFromCache: Boolean
    ) : GridUiState
    data class Error(val message: String) : GridUiState
}

class GridViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = SeriesRepository(db.tvSeriesDao())

    private val _sortByRating = MutableStateFlow(false)
    val sortByRating: StateFlow<Boolean> = _sortByRating.asStateFlow()

    private val _loadStatus = MutableStateFlow<LoadStatus>(LoadStatus.Loading)

    val uiState: StateFlow<GridUiState> = combine(
        repository.observeCached(),
        _loadStatus,
        _sortByRating
    ) { cached, status, byRating ->
        val sorted = if (byRating) cached.sortedByDescending { it.rating }
        else cached.sortedBy { it.title }
        when (status) {
            LoadStatus.Loading ->
                if (cached.isEmpty()) GridUiState.Loading
                else GridUiState.Success(sorted, isFromCache = true)
            LoadStatus.Loaded -> GridUiState.Success(sorted, isFromCache = false)
            LoadStatus.Offline -> GridUiState.Success(sorted, isFromCache = true)
            is LoadStatus.Error ->
                if (cached.isNotEmpty()) GridUiState.Success(sorted, isFromCache = true)
                else GridUiState.Error(status.message)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GridUiState.Loading)

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _loadStatus.value = LoadStatus.Loading
            when (val result = repository.refreshSeries()) {
                is FetchResult.Success -> _loadStatus.value = LoadStatus.Loaded
                is FetchResult.Offline -> _loadStatus.value = LoadStatus.Offline
                is FetchResult.Error -> _loadStatus.value = LoadStatus.Error(result.message)
            }
        }
    }

    fun setSortByRating(byRating: Boolean) {
        _sortByRating.value = byRating
    }
}


sealed interface AddSeriesUiState {
    object Idle : AddSeriesUiState
    object Saving : AddSeriesUiState
    object Saved : AddSeriesUiState
    data class Error(val message: String) : AddSeriesUiState
}

class AddSeriesViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = SeriesRepository(db.tvSeriesDao())

    private val _uiState = MutableStateFlow<AddSeriesUiState>(AddSeriesUiState.Idle)
    val uiState: StateFlow<AddSeriesUiState> = _uiState.asStateFlow()

    fun save(
        title: String,
        releaseYear: Int,
        status: String,
        rating: Double,
        isFavorite: Boolean = false,
        numberOfSeasons: Int = 1,
        imdbUrl: String = "",
        comment: String = ""
    ) {
        if (_uiState.value is AddSeriesUiState.Saving) return
        viewModelScope.launch {
            _uiState.value = AddSeriesUiState.Saving
            when (val result = repository.createSeries(
                title, releaseYear, status, rating, isFavorite, numberOfSeasons, imdbUrl, comment
            )) {
                is FetchResult.Success -> _uiState.value = AddSeriesUiState.Saved
                is FetchResult.Offline -> _uiState.value =
                    AddSeriesUiState.Error("Офлайн-режим: збереження недоступне")
                is FetchResult.Error -> _uiState.value = AddSeriesUiState.Error(result.message)
            }
        }
    }

    fun reset() {
        _uiState.value = AddSeriesUiState.Idle
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

    private val _savedName = MutableStateFlow("")
    val savedName: StateFlow<String> = _savedName.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            settings.userName.collect { name ->
                _savedName.value = name
                _isLoading.value = false
            }
        }
    }

    fun saveName(name: String) {
        viewModelScope.launch { settings.saveUserName(name) }
    }
}
